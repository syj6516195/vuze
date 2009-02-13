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
package com.aelitis.azureus.core.messenger.browser;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEDiagnosticsLogger;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.messenger.browser.listeners.BrowserMessageListener;
import com.aelitis.azureus.core.messenger.browser.listeners.MessageCompletionListener;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.JSFunctionParametersParser;
import com.aelitis.azureus.util.JSONUtils;

/**
 * Holds a message being dispatched to a {@link BrowserMessageListener}.
 * 
 * @author dharkness
 * @created Jul 18, 2006
 */
public class BrowserMessage 
{
	/** All messages must start with this prefix. */
	public static final String MESSAGE_PREFIX = "AZMSG";

	/** Separates prefix and listener ID from rest of message. */
	public static final String MESSAGE_DELIM = ";";

	public static String MESSAGE_DELIM_ENCODED;

	/** There were no parameters passed with the message. */
	public static final int NO_PARAM = 0;

	/** Parameters were an encoded JSONObject. */
	public static final int OBJECT_PARAM = 1;

	/** Parameters were an encoded JSONArray. */
	public static final int ARRAY_PARAM = 2;

	/** Parameters were an encoded list. */
	public static final int LIST_PARAM = 3;
	
	static {
		try {
			MESSAGE_DELIM_ENCODED = URLEncoder.encode(";", "UTF-8");
		} catch (UnsupportedEncodingException e) {
			MESSAGE_DELIM_ENCODED = MESSAGE_DELIM;
		}
	}

	static int seqFake = 1;

	private int sequence;

	private String listenerId;

	private String operationId;

	private String params;

	private int paramType;

	private Object decodedParams;

	private String sFullMessage;

	private ArrayList completionListeners = new ArrayList();

	private boolean completed;

	private boolean completeDelayed;

	private String referer;

	public BrowserMessage(String sMsg) {
		if (sMsg == null) {
			throw new IllegalArgumentException("event must be non-null");
		}

		this.sFullMessage = sMsg;
		parse();
	}

	public void addCompletionListener(MessageCompletionListener l) {
		completionListeners.add(l);
	}

	/**
	 * Sets the message complete and fires of the listeners who are waiting
	 * for a response.
	 * 
	 * @param bOnlyNonDelayed Only mark complete if this message does not have a delayed reponse
	 * @param success Success level of the message
	 * @param data Any data the message results wants to send
	 */
	public void complete(boolean bOnlyNonDelayed, boolean success, Object data) {
		//System.out.println("complete called with " + bOnlyNonDelayed);
		if (completed || (bOnlyNonDelayed && completeDelayed)) {
			//System.out.println("exit early" + completed);
			return;
		}
		triggerCompletionListeners(success, data);
		completed = true;
	}

	public void debug(String message) {
		debug(message, null);
	}

	public void debug(String message, Throwable t) {
		try {
			AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.CMsgr");
			String out = "[" + getListenerId() + ":" + getOperationId() + "] "
					+ message;
			diag_logger.log(out);
			if (t != null) {
				diag_logger.log(t);
			}
			if (ConstantsVuze.DIAG_TO_STDOUT) {
				System.out.println(out);
				if (t != null) {
					t.printStackTrace();
				}
			}
		} catch (Throwable t2) {
			Debug.out(t2);
		}
	}

	public List getDecodedArray() {
		if (!isParamArray()) {
			throw new IllegalStateException("Decoded parameter is not a List");
		}
		return (List) decodedParams;
	}

	public List getDecodedList() {
		if (!isParamList()) {
			throw new IllegalStateException("Decoded parameter is not a List");
		}
		return (List) decodedParams;
	}

	public Map getDecodedMap() {
		if (!isParamObject()) {
			throw new IllegalStateException("Decoded parameter is not a Map");
		}
		return (Map) decodedParams;
	}

	public String getFullMessage() {
		return sFullMessage;
	}

	public String getListenerId() {
		return listenerId;
	}

	public String getOperationId() {
		return operationId;
	}

	public String getParams() {
		return params;
	}

	public String getReferer() {
		return referer;
	}

	/*
	    public StatusTextEvent getMessage( ) {
	        return event;
	    }


	    public Browser getBrowser ( ) {
	        return (Browser) event.widget;
	    }

	    public Object getBrowserData ( String key ) {
	        return getBrowser().getData(key);
	    }

	    public void setBrowserData ( String key , Object value ) {
	        getBrowser().setData(key, value);
	    }

	    public void executeInBrowser ( String javascript ) {
	        getBrowser().execute(javascript);
	    }
	*/
	public int getSequence() {
		return sequence;
	}

	public boolean isParamArray() {
		return paramType == ARRAY_PARAM;
	}

	public boolean isParamList() {
		return paramType == LIST_PARAM;
	}

	public boolean isParamObject() {
		return paramType == OBJECT_PARAM;
	}

	/**
	 * Parses the full message into its component parts.
	 * 
	 * @throws IllegalArgumentException 
	 *              if the message cannot be parsed
	 */
	protected void parse() {
		String text = sFullMessage;

		// DJH: StringTokenizer was not used so that listeners
		//      could define their message format
		int delimSeqNum = text.indexOf(MESSAGE_DELIM);
		if (delimSeqNum == -1) {
			throw new IllegalArgumentException("Message has no delimeters: " + text);
		}
		if (delimSeqNum == text.length() - 1) {
			throw new IllegalArgumentException("Message has no sequence number: "
					+ text);
		}

		int delimListener = text.indexOf(MESSAGE_DELIM, delimSeqNum + 1);
		if (delimListener == -1 || delimListener == text.length() - 1) {
			throw new IllegalArgumentException("Message has no listener ID: " + text);
		}
		try {
			sequence = Integer.parseInt(text.substring(delimSeqNum + 1, delimListener));
		} catch (NumberFormatException e) {
			System.err.println("Plese put the throw back in once souk fixes the seq # bug");
			sequence = seqFake++;
			//throw new IllegalArgumentException("Message has no sequence number: " + text);
		}

		int delimOperation = text.indexOf(MESSAGE_DELIM, delimListener + 1);
		if (delimOperation == -1 || delimOperation == text.length() - 1) {
			// listener ID without operation
			throw new IllegalArgumentException("Message has no operation ID: " + text);
		}

		listenerId = text.substring(delimListener + 1, delimOperation);

		int delimParams = text.indexOf(MESSAGE_DELIM, delimOperation + 1);
		if (delimParams == -1) {
			// operation without parameters
			operationId = text.substring(delimOperation + 1);
		} else if (delimParams == text.length() - 1) {
			// operation without parameters
			operationId = text.substring(delimOperation + 1, delimParams);
			params = null;
			paramType = NO_PARAM;
			decodedParams = null;
		} else {
			// operation with parameters
			operationId = text.substring(delimOperation + 1, delimParams);
			params = text.substring(delimParams + 1);
			char leading = params.charAt(0);
			try {
				switch (leading) {
					case '{':
						paramType = OBJECT_PARAM;
						decodedParams = JSONUtils.decodeJSON(params);
						break;

					case '[':
						paramType = ARRAY_PARAM;
						Map decodeJSON = JSONUtils.decodeJSON(params);
						if (decodeJSON != null) {
							decodedParams = decodeJSON.get("value");
						} else {
							decodedParams = null;
						}
						break;

					default:
						paramType = LIST_PARAM;
						decodedParams = JSFunctionParametersParser.parse(params);
						break;
				}
			} catch (Exception e) {
				decodedParams = null;
			}
			if (decodedParams == null) {
				paramType = NO_PARAM;
			}
		}
	}

	public void removeCompletionListener(MessageCompletionListener l) {
		completionListeners.remove(l);
	}

	public void setCompleteDelayed(boolean bCompleteDelayed) {
		completeDelayed = bCompleteDelayed;
	}

	public void setReferer(String referer) {
		this.referer = referer;
	}

	public String toString() {
		return "[" + sequence + "] " + listenerId + "." + operationId + "("
				+ params + ")";
	}

	private void triggerCompletionListeners(boolean success, Object data) {
		for (Iterator iterator = completionListeners.iterator(); iterator.hasNext();) {
			MessageCompletionListener l = (MessageCompletionListener) iterator.next();
			try {
				l.completed(success, data);
			} catch (Throwable e) {
				Debug.out(e);
			}
		}
	}
}
