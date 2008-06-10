/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.messenger;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.util.Timer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.messenger.browser.*;
import com.aelitis.azureus.core.messenger.browser.listeners.MessageCompletionListener;
import com.aelitis.azureus.core.messenger.config.PlatformRelayMessenger;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.JSONUtils;
import com.aelitis.azureus.util.MapUtils;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;

/**
 * @author TuxPaper
 * @created Sep 25, 2006
 *
 */
public class PlatformMessenger
{
	private static final boolean DEBUG_URL = System.getProperty("platform.messenger.debug.url", "0").equals("1");

	private static boolean USE_HTTP_POST = true;

	public static String REPLY_EXCEPTION = "exception";

	public static String REPLY_ACTION = "action";

	public static String REPLY_RESULT = "response";

	static private Map mapQueueAuthorized = new HashMap();

	static private Map mapQueue = new HashMap();

	static private AEMonitor queue_mon = new AEMonitor(
			"v3.PlatformMessenger.queue");

	static private Timer timerProcess = new Timer("v3.PlatformMessenger.queue");

	static private TimerEvent timerEvent = null;

	private static boolean initialized;

	private static fakeContext context;

	private static PlatformAuthorizedSender authorizedSender;

	private static boolean authorizedDelayed;

	public static synchronized void init() {
		if (initialized) {
			return;
		}
		initialized = true;

		// The UI will initialize this
		context = new fakeContext();
	}

	public static void setAuthorizedTransferListener(
			PlatformAuthorizedSender authorizedSender) {
		PlatformMessenger.authorizedSender = authorizedSender;
	}

	public static ClientMessageContext getClientMessageContext() {
		if (!initialized) {
			init();
		}
		return context;
	}

	public static void queueMessage(PlatformMessage message,
			PlatformMessengerListener listener) {
		queueMessage(message, listener, true);
	}

	public static void queueMessage(PlatformMessage message,
			PlatformMessengerListener listener, boolean addToBottom) {
		if (!initialized) {
			init();
		}

		if (message != null) {
			debug("q " + message.toShortString() + ": " + message + " for "
					+ new Date(message.getFireBefore()));
			if (message.requiresAuthorization() && authorizedDelayed) {
				debug("   authorized msg is delayed");
			}
		} else {
			debug("fire timerevent");
		}
		queue_mon.enter();
		try {
			long fireBefore;
			if (message != null) {
  			if (message.requiresAuthorization()) {
  				mapQueueAuthorized.put(message, listener);
  			} else {
  				mapQueue.put(message, listener);
  			}
  			fireBefore = message.getFireBefore();
			} else {
				fireBefore = SystemTime.getCurrentTime();
			}

			if (timerEvent == null || timerEvent.hasRun()) {
				timerEvent = timerProcess.addEvent(fireBefore,
						new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								timerEvent = null;
								while (mapQueue.size() > 0) {
									processQueue(mapQueue, false);
								}
								if (!authorizedDelayed) {
  								while (mapQueueAuthorized.size() > 0) {
  									processQueue(mapQueueAuthorized, true);
  								}
								}
							}
						});
			} else {
				// Move the time up if we have to
				if (fireBefore < timerEvent.getWhen()) {
					timerProcess.adjustAllBy(fireBefore - timerEvent.getWhen());
				}
			}
		} finally {
			queue_mon.exit();
		}
	}

	/**
	 * @param string
	 */
	public static void debug(String string) {
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.PMsgr");
		diag_logger.log(string);
		if (Constants.DIAG_TO_STDOUT) {
			System.out.println(Thread.currentThread().getName() + "|"
					+ System.currentTimeMillis() + "] " + string);
		}
	}
	
	/**
	 * Sends the message almost immediately, skipping delayauthorization check 
	 * @param message
	 * @param listener
	 *
	 * @since 3.0.5.3
	 */
	public static void pushMessageNow(PlatformMessage message, PlatformMessengerListener listener) {
		Map map = new HashMap(1);
		map.put(message, listener);
		processQueue(map, message.requiresAuthorization());
	}

	/**
	 * @param requiresAuthorization 
	 * 
	 */
	protected static void processQueue(Map mapQueue,
			final boolean requiresAuthorization) {
		if (!initialized) {
			init();
		}

		final Map mapProcessing = new HashMap();

		boolean loginAndRetry = false;
		queue_mon.enter();
		try {
			// add one at a time, ensure relay server messages are seperate
			for (Iterator iter = mapQueue.keySet().iterator(); iter.hasNext();) {
				PlatformMessage message = (PlatformMessage) iter.next();
				Object value = mapQueue.get(message);

				boolean isRelayServer = PlatformRelayMessenger.LISTENER_ID.equals(message.getListenerID());

				if (isRelayServer && mapProcessing.size() > 0) {
					break;
				}

				mapProcessing.put(message, value);

				iter.remove();

				// split up ones that requre login and retry and ones that don't
				if (mapProcessing.size() == 1) {
					loginAndRetry = message.getLoginAndRetry();
				} else {
					if (loginAndRetry != message.getLoginAndRetry()) {
						break;
					}
				}
				
				if (isRelayServer) {
					break;
				}
			}
		} finally {
			queue_mon.exit();
		}
		//debug("about to process " + mapProcessing.size());

		if (mapProcessing.size() == 0) {
			return;
		}

		// Create urlStem (or post data)
		// determine which server to use
		String server = null;
		String urlStem = "";
		long sequenceNo = 0;
		for (Iterator iter = mapProcessing.keySet().iterator(); iter.hasNext();) {
			PlatformMessage message = (PlatformMessage) iter.next();
			message.setSequenceNo(sequenceNo);

			if (sequenceNo > 0) {
				urlStem += "&";
			}

			String listenerID = message.getListenerID();
			try {
				urlStem += "cmd="
						+ URLEncoder.encode(message.getMessageID()
								+ BrowserMessage.MESSAGE_DELIM + sequenceNo
								+ BrowserMessage.MESSAGE_DELIM + listenerID
								+ BrowserMessage.MESSAGE_DELIM + message.getOperationID()
								+ BrowserMessage.MESSAGE_DELIM
								+ message.getParameters().toString(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
			}
			if (server == null) {
				server = message.getMessageID() + "-" + listenerID;
			} else if (!server.equals(listenerID)) {
				server = "multi";
			}

			PlatformMessengerListener listener = (PlatformMessengerListener) mapProcessing.get(message);
			if (listener != null) {
				listener.messageSent(message);
			}
			sequenceNo++;
		}

		if (server == null) {
			server = "default";
		}

		// Build base RPC url based on listener and server
		String sURL_RPC;
		boolean isRelayServer = (PlatformRelayMessenger.MSG_ID + "-" + PlatformRelayMessenger.LISTENER_ID).equals(server);
		if (isRelayServer) {
			sURL_RPC = Constants.URL_RELAY_RPC;
		} else {
			sURL_RPC = Constants.URL_PREFIX + Constants.URL_RPC + server;
		}

		// Build full url and data to send
		String sURL;
		String sPostData = null;
		if (USE_HTTP_POST) {
			sURL = sURL_RPC;
			sPostData = Constants.URL_POST_PLATFORM_DATA + "&" + urlStem + "&"
					+ Constants.URL_SUFFIX;
			if (!requiresAuthorization) {
				if (DEBUG_URL) {
					debug("POST: " + sURL + "?" + sPostData);
				} else {
					debug("POST: " + sURL);
				}
			}
		} else {
			sURL = sURL_RPC + Constants.URL_PLATFORM_MESSAGE + "&" + urlStem + "&"
					+ Constants.URL_SUFFIX;
			if (DEBUG_URL) {
				debug("GET: " + sURL);
			} else {
				debug("GET: " + sURL_RPC + Constants.URL_PLATFORM_MESSAGE);
			}
		}

		final String fURL = sURL;
		final String fPostData = sPostData;
		final boolean fLoginAndRetry = loginAndRetry; 

		// proccess queue on a new thread
		AEThread2 thread = new AEThread2("v3.PlatformMessenger", true) {
			public void run() {
				try {
					processQueueAsync(fURL, fPostData, mapProcessing,
							requiresAuthorization, fLoginAndRetry);
				} catch (Throwable e) {
					if (e instanceof ResourceDownloaderException) {
						Debug.out("Error while sending message(s) to Platform: "
								+ e.toString());
					} else {
						Debug.out("Error while sending message(s) to Platform", e);
					}
					for (Iterator iter = mapProcessing.keySet().iterator(); iter.hasNext();) {
						PlatformMessage message = (PlatformMessage) iter.next();
						PlatformMessengerListener l = (PlatformMessengerListener) mapProcessing.get(message);
						if (l != null) {
							try {
								HashMap map = new HashMap();
								map.put("text", e.toString());
								map.put("Throwable", e);
								l.replyReceived(message, REPLY_EXCEPTION, map);
							} catch (Throwable e2) {
								Debug.out("Error while sending replyReceived", e2);
							}
						}
					}
				}
			}
		};
		thread.start();
	}

	/**
	 * @param mapProcessing 
	 * @param surl
	 * @throws Exception 
	 */
	protected static void processQueueAsync(String sURL, String sData,
			Map mapProcessing, boolean requiresAuthorization,
			boolean loginAndRetry) throws Exception {
		URL url;
		url = new URL(sURL);
		AzureusCore core = AzureusCoreFactory.getSingleton();
		final PluginInterface pi = core.getPluginManager().getDefaultPluginInterface();

		String s;
		if (requiresAuthorization && authorizedSender != null) {
			AESemaphore sem_waitDL = new AESemaphore("Waiting for DL");
			authorizedSender.startDownload(url, sData, sem_waitDL, loginAndRetry);
			sem_waitDL.reserve();
			s = authorizedSender.getResults();
		} else {
			byte[] bytes = downloadURL(pi, url, sData);
			s = new String(bytes, "UTF8");
		}

		// Format: <sequence no> ; <classification> [; <results>] [ \n ]

		if (s == null || s.length() == 0 || !Character.isDigit(s.charAt(0))) {
			Debug.out("Error while sending message(s) to Platform: reply: " + s
					+ "\nurl: " + sURL + "\nPostData: " + sData);
			for (Iterator iter = mapProcessing.keySet().iterator(); iter.hasNext();) {
				PlatformMessage message = (PlatformMessage) iter.next();
				PlatformMessengerListener l = (PlatformMessengerListener) mapProcessing.get(message);
				if (l != null) {
					try {
						HashMap map = new HashMap();
						map.put("text", "result was " + s);
						l.replyReceived(message, REPLY_EXCEPTION, map);
					} catch (Throwable e2) {
						Debug.out("Error while sending replyReceived" + "\nurl: " + sURL
								+ "\nPostData: " + sData, e2);
					}
				}
			}
			return;
		}

		Map mapSeqToBrowserMsg = new HashMap();

		String[] replies = s.split("\\n");
		for (int i = 0; i < replies.length; i++) {
			String reply = replies[i];

			final String[] replySections = reply.split(BrowserMessage.MESSAGE_DELIM,
					3);
			if (replySections.length < 2) {
				continue;
			}
			long sequenceNo = NumberFormat.getInstance().parse(replySections[0]).longValue();

			Map actionResults = null;

			if (replySections.length == 3) {
				try {
					actionResults = JSONUtils.decodeJSON(replySections[2]);
				} catch (Throwable e) {
					Debug.out("Error while sending message(s) to Platform: reply: " + s
							+ "\nurl: " + sURL + "\nPostData: " + sData, e);
				}
			}

			// Find PlatformMessage associated with sequence
			// TODO: There's a better way to do this
			PlatformMessage message = null;
			PlatformMessengerListener listener = null;
			for (Iterator iter = mapProcessing.keySet().iterator(); iter.hasNext();) {
				PlatformMessage potentialMessage = (PlatformMessage) iter.next();
				if (potentialMessage.getSequenceNo() == sequenceNo) {
					message = potentialMessage;
					listener = (PlatformMessengerListener) mapProcessing.get(message);
				}
			}

			if (message == null) {
				Debug.out("No message with sequence number " + sequenceNo);
				continue;
			}

			debug("Got a reply for " + message.toShortString() + "\n\t\t" + reply);

			final PlatformMessage fMessage = message;
			final PlatformMessengerListener fListener = listener;
			final Map fActionResults = actionResults;

			// test
			if (i == 0 && false) {
				replySections[1] = "action";
				actionResults = new JSONObject();
				actionResults.put("retry-client-message", new Boolean(true));
				JSONArray a = new JSONArray();
				a.add("[AZMSG;1;display;open-url;{\"url\":\"http://yahoo.com\",\"width\":500,\"height\":200}]");
				actionResults.put("messages", a);
			}

			// Todo check array [1] for reply type

			if (replySections[1].equals("action")) {
				final BrowserMessageDispatcher dispatcher = context.getDispatcher();
				if (dispatcher == null) {
					debug("action requested.. no dispatcher");
				} else if (actionResults instanceof Map) {
					final boolean bRetry = MapUtils.getMapBoolean(actionResults,
							"retry-client-message", false);

					List array = (List) MapUtils.getMapObject(actionResults, "messages",
							null, List.class);
					if (actionResults.containsKey("messages")) {
						for (int j = 0; j < array.size(); j++) {
							final String sMsg = (String) array.get(j);
							debug("handling (" + ((bRetry) ? " with retry" : " no retry")
									+ "): " + sMsg);

							final BrowserMessage browserMsg = new BrowserMessage(sMsg);
							int seq = browserMsg.getSequence();
							BrowserMessage existingBrowserMsg = (BrowserMessage) mapSeqToBrowserMsg.get(new Long(
									seq));
							if (existingBrowserMsg != null) {
								existingBrowserMsg.addCompletionListener(new MessageCompletionListener() {
									public void completed(boolean success, Object data) {
										debug("got complete for " + sMsg);
										if (success) {
											queueMessage(fMessage, fListener);
										} else {
											if (fListener != null) {
												try {
													fListener.replyReceived(fMessage, replySections[1],
															fActionResults);
												} catch (Throwable e2) {
													Debug.out("Error while sending replyReceived", e2);
												}
											}
										}
									}
								});
								continue;
							}

							if (bRetry) {
								mapSeqToBrowserMsg.put(new Long(seq), browserMsg);

								browserMsg.addCompletionListener(new MessageCompletionListener() {
									public void completed(boolean success, Object data) {
										debug("got complete for " + sMsg + ";" + success);
										if (success) {
											queueMessage(fMessage, fListener);
										} else {
											if (fListener != null) {
												try {
													fListener.replyReceived(fMessage, replySections[1],
															fActionResults);
												} catch (Throwable e2) {
													Debug.out("Error while sending replyReceived", e2);
												}
											}
										}
									}
								});
							}

							new AEThread2("v3.Msg.Dispatch", true) {
								public void run() {
									dispatcher.dispatch(browserMsg);
								}
							}.start();
						}
					}
					if (bRetry) {
						continue;
					}
				}
			}

			if (listener != null) {
				try {
					listener.replyReceived(message, replySections[1], actionResults);
				} catch (Exception e2) {
					Debug.out("Error while sending replyReceived", e2);
				}
			}
		}
		BrowserMessageDispatcher dispatcher = context.getDispatcher();
		if (dispatcher != null) {
			dispatcher.resetSequence();
		}
	}

	private static byte[] downloadURL(PluginInterface pi, URL url, String postData)
			throws Exception {
		ResourceDownloaderFactory rdf = pi.getUtilities().getResourceDownloaderFactory();

		ResourceDownloader rd = rdf.create(url, postData);

		rd = rdf.getRetryDownloader(rd, 3);
		// We could report percentage to listeners, but there's no need to atm
		//		rd.addListener(new ResourceDownloaderListener() {
		//		
		//			public void reportPercentComplete(ResourceDownloader downloader,
		//					int percentage) {
		//			}
		//		
		//			public void reportActivity(ResourceDownloader downloader, String activity) {
		//			}
		//		
		//			public void failed(ResourceDownloader downloader,
		//					ResourceDownloaderException e) {
		//			}
		//		
		//			public boolean completed(ResourceDownloader downloader, InputStream data) {
		//				return true;
		//			}
		//		});

		InputStream is = rd.download();

		byte data[];

		try {
			int length = is.available();

			data = new byte[length];

			is.read(data);

		} finally {

			is.close();
		}

		return (data);
	}

	private static class fakeContext
		extends ClientMessageContextImpl
	{
		private void
		log(
			String str )
		{
			if ( System.getProperty( "browser.route.all.external.stimuli.for.testing", "false" ).equalsIgnoreCase( "true" )){
				
				System.err.println( str );
			}
			debug( str );
		}

		public fakeContext() {
			super("fakeContext", null);
		}

		public void deregisterBrowser() {
			log("deregisterBrowser");
		}

		public void displayBrowserMessage(String message) {
			log("displayBrowserMessage - " + message );
		}

		public boolean executeInBrowser(String javascript) {
			log("executeInBrowser - " + javascript );
			return false;
		}

		public Object getBrowserData(String key) {
			log("getBrowserData - " + key );
			return null;
		}

		public boolean sendBrowserMessage(String key, String op) {
			log("sendBrowserMessage - " + key + "/" + op );
			return false;
		}

		public boolean sendBrowserMessage(String key, String op, Map params) {
			log("sendBrowserMessage - " + key + "/" + op + "/" + params );
			return false;
		}

		public void setBrowserData(String key, Object value) {
			log("setBrowserData - " + key + "/" + value );
		}

		public boolean sendBrowserMessage(String key, String op, Collection params) {
			log("sendBrowserMessage - " + key + "/" + op + "/" + params );
			return false;
		}
	}

	/**
	 * @param b
	 *
	 * @since 3.0.5.3
	 */
	public static void setAuthorizedDelayed(boolean authorizedDelayed) {
		debug("setDelayAuthorized " + authorizedDelayed);
		PlatformMessenger.authorizedDelayed = authorizedDelayed;
		if (!authorizedDelayed && mapQueueAuthorized.size() > 0) {
			queueMessage(null, null);
		}
	}

	public static boolean isAuthorizedDelayed() {
		return authorizedDelayed;
	}
}
