/*
 * Created on Jun 29, 2006 10:16:26 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.ui.swt.browser.msg;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.browser.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.core.messenger.browser.BrowserMessageDispatcher;
import com.aelitis.azureus.core.messenger.browser.listeners.BrowserMessageListener;
import com.aelitis.azureus.util.UrlFilter;

/**
 * Dispatches messages to listeners registered with unique IDs. Each message sent
 * from the browser must be given a message sequence number that is different
 * from that of the previous message to detect duplicate events.
 *   <p/>
 * Messages are in the form
 * <pre>  PREFIX DELIM <em>&lt;seq-no&gt;</em> DELIM <em>&lt;listener-id&gt;</em> DELIM <em>&lt;operation-id&gt;</em> DELIM <em>&lt;params&gt;</em> . . .</pre>
 * 
 * For example,
 * <pre>  "AZMSG;37;publish;choose-files"</pre>
 * 
 * Sequence numbers are unique to each {@link Browser} instance in the UI,
 * and they start at 1, being reset each time an HTML page loads.
 * 
 * @author dharkness
 * @created Jul 18, 2006
 */
public class MessageDispatcherSWT
	implements StatusTextListener, TitleListener, BrowserMessageDispatcher
{
	public static final String LISTENER_ID = "dispatcher";

	public static final String OP_RESET_SEQUENCE = "reset-sequence";

	public static final String CONTEXT_LISTENER_ID = "context";

	private static final int INITIAL_LAST_SEQUENCE = -1;

	private ClientMessageContext context;

	private Map listeners = new HashMap();

	private int lastSequence = INITIAL_LAST_SEQUENCE;

	private String sLastEventText;

	private AEMonitor class_mon = new AEMonitor("MessageDispatcher");

	private Browser browser;

	/**
	 * Registers itself as a listener to receive sequence number reset message.
	 */
	public MessageDispatcherSWT(ClientMessageContext context) {
		this.context = context;
	}

	public void registerBrowser(Browser browser) {
		this.browser = browser;
		browser.addStatusTextListener(this);
		browser.addTitleListener(this);
	}

	/**
	 * Detaches this dispatcher from the given {@link Browser}.
	 * This dispatcher listens for dispose events from the browser
	 * and calls this method in response.
	 * 
	 * @param browser {@link Browser} which will no longer send messages
	 */
	public void deregisterBrowser(Browser browser) {
		browser.removeStatusTextListener(this);
		browser.removeTitleListener(this);
	}

	/**
	 * Registers the given listener for the given ID.
	 * 
	 * @param id unique identifier used when dispatching messages
	 * @param listener receives messages targetted at the given ID
	 * 
	 * @throws IllegalStateException
	 *              if another listener is already registered under the same ID
	 */
	public synchronized void addListener(BrowserMessageListener listener) {
		String id = listener.getId();
		BrowserMessageListener registered = (BrowserMessageListener) listeners.get(id);
		if (registered != null) {
			if (registered != listener) {
				throw new IllegalStateException("Listener "
						+ registered.getClass().getName() + " already registered for ID "
						+ id);
			}
		} else {
			listener.setContext(context);
			listeners.put(id, listener);
		}
	}

	/**
	 * Deregisters the listener with the given ID.
	 * 
	 * @param id unique identifier of the listener to be removed
	 */
	public synchronized void removeListener(BrowserMessageListener listener) {
		removeListener(listener.getId());
	}

	/**
	 * Deregisters the listener with the given ID.
	 * 
	 * @param id unique identifier of the listener to be removed
	 */
	public synchronized void removeListener(String id) {
		BrowserMessageListener removed = (BrowserMessageListener) listeners.remove(id);
		if (removed == null) {
			//            throw new IllegalStateException("No listener is registered for ID " + id);
		} else {
			removed.setContext(null);
		}
	}

	// @see com.aelitis.azureus.ui.swt.browser.msg.MessageDispatcher#getListener(java.lang.String)
	public BrowserMessageListener getListener(String id) {
		return (BrowserMessageListener) listeners.get(id);
	}

	/**
	 * Parses the event to see if it's a valid message and dispatches it.
	 * 
	 * @param event contains the message
	 * 
	 * @see org.eclipse.swt.browser.StatusTextListener#changed(org.eclipse.swt.browser.StatusTextEvent)
	 */
	public void changed(StatusTextEvent event) {
		processIncomingMessage(event.text, ((Browser) event.widget).getUrl());
	}

	// @see org.eclipse.swt.browser.TitleListener#changed(org.eclipse.swt.browser.TitleEvent)
	public void changed(TitleEvent event) {
		processIncomingMessage(event.title, ((Browser) event.widget).getUrl());
	}

	private void processIncomingMessage(String msg, String referer) {
		if (msg == null) {
			return;
		}

		try {
			class_mon.enter();
			if (sLastEventText != null && msg.equals(sLastEventText)) {
				return;
			}

			sLastEventText = msg;
		} finally {
			class_mon.exit();
		}

		if (msg.startsWith(BrowserMessage.MESSAGE_PREFIX)) {
			try {
				BrowserMessage browserMessage = new BrowserMessage(msg);
				browserMessage.setReferer(referer);
				dispatch(browserMessage);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	// @see com.aelitis.azureus.ui.swt.browser.msg.MessageDispatcher#dispatch(com.aelitis.azureus.core.messenger.browser.BrowserMessage)
	public void dispatch(final BrowserMessage message) {
		if (message == null) {
			return;
		}
		String referer = message.getReferer();
		if (referer != null && !UrlFilter.getInstance().urlCanRPC(referer)) {
			context.debug("blocked " + message + "\n  " + referer);
			return;
		}


		context.debug("Received " + message);
		if (browser != null && !browser.isDisposed() && Utils.isThisThreadSWT()) {
			context.debug("   browser url: " + browser.getUrl());
		}

		// handle messages for dispatcher and context regardless of sequence number
		String listenerId = message.getListenerId();
		if ("lightbox-browser".equals(listenerId)) {
			listenerId = "display";
		}
		if (LISTENER_ID.equals(listenerId)) {
			handleMessage(message);
		} else {
			if (!isValidSequence(message)) {
				context.debug("Ignoring duplicate: " + message);
			} else {
				final BrowserMessageListener listener = getListener(listenerId);
				if (listener == null) {
					context.debug("No listener registered with ID " + listenerId);
				} else {
					new AEThread2("dispatch for " + listenerId, true) {
						public void run() {
							listener.handleMessage(message);
							message.complete(true, true, null);
						}
					}.start();
				}
			}
		}
	}

	// @see com.aelitis.azureus.ui.swt.browser.msg.MessageDispatcher#handleMessage(com.aelitis.azureus.core.messenger.browser.BrowserMessage)
	public void handleMessage(BrowserMessage message) {
		String operationId = message.getOperationId();
		if (OP_RESET_SEQUENCE.equals(operationId)) {
			resetSequence();
		} else {
			throw new IllegalArgumentException("Unknown operation: " + operationId);
		}
	}

	// @see com.aelitis.azureus.ui.swt.browser.msg.MessageDispatcher#isValidSequence(com.aelitis.azureus.core.messenger.browser.BrowserMessage)
	public boolean isValidSequence(BrowserMessage message) {
		int sequence = message.getSequence();
		if (sequence < 0) {
			Debug.outNoStack("Invalid sequence number: " + sequence);
			return false;
		}

		if (sequence <= lastSequence) {
			context.debug("Duplicate sequence number: " + sequence + ", last: "
					+ lastSequence);
			return false;
		}

		lastSequence = sequence;
		return true;
	}

	/**
	 * Resets the sequence number for the given {@link Browser} to 0.
	 * 
	 * @param browser {@link Browser} to reset
	 */
	public void resetSequence() {
		context.debug("Reseting sequence number");
		lastSequence = INITIAL_LAST_SEQUENCE;
	}
}
