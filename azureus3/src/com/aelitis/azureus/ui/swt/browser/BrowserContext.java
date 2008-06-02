/*
 * Created on Jul 19, 2006 10:16:26 PM
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
package com.aelitis.azureus.ui.swt.browser;

import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.core.messenger.ClientMessageContextImpl;
import com.aelitis.azureus.core.messenger.browser.listeners.BrowserMessageListener;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.ui.swt.browser.msg.MessageDispatcherSWT;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.JSONUtils;

/**
 * Manages the context for a single SWT {@link Browser} component,
 * including transactions, listeners and messages.
 * 
 * @author dharkness
 * @created Jul 19, 2006
 */
public class BrowserContext
	extends ClientMessageContextImpl
	implements DisposeListener
{
	private static final String CONTEXT_KEY = "BrowserContext";

	private static final String KEY_ENABLE_MENU = "browser.menu.enable";

	private Browser browser;

	private Display display;

	private boolean pageLoading = false;

	private String lastValidURL = null;

	private final boolean forceVisibleAfterLoad;

	private TimerEventPeriodic checkURLEvent;

	private boolean checkBlocked = true;

	private Control widgetWaitIndicator;

	private MessageDispatcherSWT messageDispatcherSWT;

	protected boolean wiggleBrowser = org.gudy.azureus2.core3.util.Constants.isOSX;

	/**
	 * Creates a context and registers the given browser.
	 * 
	 * @param id unique identifier of this context
	 * @param browser the browser to be registered
	 */
	public BrowserContext(String id, Browser browser,
			Control widgetWaitingIndicator, boolean forceVisibleAfterLoad) {
		this(id, forceVisibleAfterLoad);
		registerBrowser(browser, widgetWaitingIndicator);
	}

	/**
	 * Creates a context without a registered browser.
	 * This method should rarely be used.
	 * 
	 * @param id unique identifier of this context
	 */
	public BrowserContext(String id, boolean forceVisibleAfterLoad) {
		super(id, null);
		messageDispatcherSWT = new MessageDispatcherSWT(this);
		setMessageDispatcher(messageDispatcherSWT);
		this.forceVisibleAfterLoad = forceVisibleAfterLoad;
	}

	public void registerBrowser(Object oBrowser,
			Object oWidgetWaitIndicator) {
		if (this.browser != null) {
			throw new IllegalStateException("Context " + getID()
					+ " already has a registered browser");
		}

		if (oBrowser instanceof Browser) {
			this.browser = (Browser) oBrowser;
		} else {
			throw new IllegalStateException("Context " + getID()
					+ ": brwoser isn't a Browser");
		}

		if (oWidgetWaitIndicator instanceof Control) {
			this.widgetWaitIndicator = (Control) oWidgetWaitIndicator;
		}
		
		final TimerEventPerformer showBrowersPerformer = new TimerEventPerformer() {
			public void perform(TimerEvent event) {
				if (browser != null && !browser.isDisposed()) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							if (forceVisibleAfterLoad && browser != null
									&& !browser.isDisposed() && !browser.isVisible()) {
								browser.setVisible(true);
							}
						}
					});
				}
			}
		};

		final TimerEventPerformer hideIndicatorPerformer = new TimerEventPerformer() {
			public void perform(TimerEvent event) {
				if (widgetWaitIndicator != null && !widgetWaitIndicator.isDisposed()) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							if (widgetWaitIndicator != null
									&& !widgetWaitIndicator.isDisposed()) {
								widgetWaitIndicator.setVisible(false);
							}
						}
					});
				}
			}
		};

		final TimerEventPerformer checkURLEventPerformer = new TimerEventPerformer() {
			public void perform(TimerEvent event) {
				if (!browser.isDisposed()) {
					Utils.execSWTThreadLater(0, new AERunnable() {
						public void runSupport() {
							if (!browser.isDisposed()) {
								browser.execute("try { "
										+ "tuxLocString = document.location.toString();"
										+ "if (tuxLocString.indexOf('res://') == 0) {"
										+ "  document.title = 'err: ' + tuxLocString;"
										+ "} else {"
										+ "  tuxTitleString = document.title.toString();"
										+ "  if (tuxTitleString.indexOf('408 ') == 0 || tuxTitleString.indexOf('503 ') == 0 || tuxTitleString.indexOf('500 ') == 0) "
										+ "  { document.title = 'err: ' + tuxTitleString; } " + "}"
										+ "} catch (e) { }");
							}
						}
					});
				}
			}
		};

		if (forceVisibleAfterLoad) {
			browser.setVisible(false);
		}
		if (widgetWaitIndicator != null && !widgetWaitIndicator.isDisposed()) {
			widgetWaitIndicator.setVisible(false);
		}
		browser.addTitleListener(new TitleListener() {
			public void changed(TitleEvent event) {
				/*
				 * The browser might have been disposed already by the time this method is called 
				 */
				if(true == browser.isDisposed()){
					return;
				}
				
				if (!browser.isVisible()) {
					SimpleTimer.addEvent("Show Browser",
							System.currentTimeMillis() + 700, showBrowersPerformer);
				}
				if (event.title.startsWith("err: ")) {
					fillWithRetry(event.title);
				}
			}
		});

		browser.addProgressListener(new ProgressListener() {
			public void changed(ProgressEvent event) {
				//int pct = event.total == 0 ? 0 : 100 * event.current / event.total;
				//System.out.println(pct + "%/" + event.current + "/" + event.total);
			}

			public void completed(ProgressEvent event) {
				/*
				 * The browser might have been disposed already by the time this method is called 
				 */
				if(true == browser.isDisposed()){
					return;
				}
				
				checkURLEventPerformer.perform(null);
				if (forceVisibleAfterLoad && !browser.isVisible()) {
					browser.setVisible(true);
				}

				browser.execute("try { if (azureusClientWelcome) { azureusClientWelcome('"
						+ Constants.AZID
						+ "',"
						+ "{ 'azv':'"
						+ org.gudy.azureus2.core3.util.Constants.AZUREUS_VERSION
						+ "', 'browser-id':'" + getID() + "' }" + ");} } catch (e) { }");

				if (org.gudy.azureus2.core3.util.Constants.isCVSVersion()
						|| System.getProperty("debug.https", null) != null) {
					if (browser.getUrl().indexOf("https") == 0) {
						browser.execute("try { o = document.getElementsByTagName('body'); if (o) o[0].style.borderTop = '2px dotted #3b3b3b'; } catch (e) {}");
					}
				}

				if (wiggleBrowser ) {
					Shell shell = browser.getShell();
					Point size = shell.getSize();
					size.x -= 1;
					size.y -= 1;
					shell.setSize(size);
					size.x += 1;
					size.y += 1;
					shell.setSize(size);
				}
			}
		});

		checkURLEvent = SimpleTimer.addPeriodicEvent("checkURL", 10000,
				checkURLEventPerformer);

		
		browser.addOpenWindowListener(new OpenWindowListener() {
			public void open(WindowEvent event) {
				if(! event.required) return;
				if(checkBlocked) return;
				final Browser subBrowser = new Browser(browser,SWT.NONE);
				subBrowser.addLocationListener(new LocationListener() {
					public void changed(LocationEvent arg0) {
						// TODO Auto-generated method stub
						
					}
					public void changing(LocationEvent event) {
						event.doit = false;
						System.out.println("SubBrowser URL : " + event.location);
						if(event.location.startsWith("http://") || event.location.startsWith("https://")) {
							Program.launch(event.location);
						}
						subBrowser.dispose();
					}
				});
				event.browser = subBrowser;
			}
		});
		
		browser.addLocationListener(new LocationListener() {
			private TimerEvent timerevent;

			public void changed(LocationEvent event) {
				//System.out.println("cd" + event.location);
				if (timerevent != null) {
					timerevent.cancel();
				}
				checkURLEventPerformer.perform(null);
				if (widgetWaitIndicator != null && !widgetWaitIndicator.isDisposed()) {
					widgetWaitIndicator.setVisible(false);
				}
			}

			public void changing(LocationEvent event) {
				//System.out.println("cing " + event.location); 
				
				/*
				 * The browser might have been disposed already by the time this method is called 
				 */
				if(true == browser.isDisposed()){
					return;
				}
				
				if (event.location.startsWith("javascript")
						&& event.location.indexOf("back()") > 0) {
					if (browser.isBackEnabled()) {
						browser.back();
					} else if (lastValidURL != null) {
						fillWithRetry(event.location);
					}
					return;
				}
				boolean isWebURL = event.location.startsWith("http://")
						|| event.location.startsWith("https://");
				if (!isWebURL) {
					if (event.location.startsWith("res://") && lastValidURL != null) {
						fillWithRetry(event.location);
					}
					// we don't get a changed state on non URLs (mailto, javascript, etc)
					return;
				}

				// Regex Test for https?://moo\.com:?[0-9]*/dr
				// http://moo.com/dr
				// httpd://moo.com:80/dr
				// https://moo.com:a0/dr
				// http://moo.com:80/dr
				// http://moo.com:8080/dr
				// https://moo.com/dr
				// https://moo.com:80/dr
				
				

				boolean blocked = checkBlocked && PlatformConfigMessenger.isURLBlocked(event.location);

				if (blocked) {
					event.doit = false;
					browser.back();
				} else {
					if(event.top || checkBlocked) {
						lastValidURL = event.location;
						if (widgetWaitIndicator != null && !widgetWaitIndicator.isDisposed()) {
							widgetWaitIndicator.setVisible(true);
						}
	
						// Backup in case changed(..) is never called
						timerevent = SimpleTimer.addEvent("Hide Indicator",
								System.currentTimeMillis() + 20000, hideIndicatorPerformer);
					} else {
						boolean isTorrent = false;
						//Try to catch .torrent files
						if(event.location.endsWith(".torrent")) {
							isTorrent = true;
						} else {
							//If it's not obviously a web page
							if(event.location.indexOf(".htm") == -1) {
								try {
									//See what the content type is
									URL url = new URL(event.location);
									URLConnection conn = url.openConnection();
									String contentType = conn.getContentType();
									if(contentType != null && contentType.indexOf("torrent") != -1) {
										isTorrent = true;
									}
									String contentDisposition = conn.getHeaderField("Content-Disposition");
									if(contentDisposition != null && contentDisposition.indexOf(".torrent") != -1) {
										isTorrent = true;
									}
									
								} catch(Exception e) {
									e.printStackTrace();
								}
							}
						}
						
						if(isTorrent) {
							event.doit = false;
							try {
								AzureusCoreImpl.getSingleton().getPluginManager().getDefaultPluginInterface().getDownloadManager().addDownload(new URL(event.location),true);
							} catch(Exception e) {
								e.printStackTrace();
							}
						}
						
						if(event.location.indexOf("utorrent.com") != -1) {
							event.doit = false;
						}
					}
				}
			}
		});

		browser.setData(CONTEXT_KEY, this);
		browser.addDisposeListener(this);

		// enable right-click context menu only if system property is set
		final boolean enableMenu = System.getProperty(KEY_ENABLE_MENU, "0").equals(
				"1");
		browser.addListener(SWT.MenuDetect, new Listener() {
			// @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
			public void handleEvent(Event event) {
				event.doit = enableMenu;
			}
		});

		// check if blocked only if we aren't already blocking
		messageDispatcherSWT.registerBrowser(browser, !checkBlocked);
		this.display = browser.getDisplay();
	}

	public void fillWithRetry(String s) {
		browser.setText("<html><body style='overflow:auto; font-family: verdana; font-size: 10pt' bgcolor=#1b1b1b text=#a0a0a0>"
				+ "<br>Sorry, there was a problem loading this page.<br> "
				+ "Please check if your internet connection is working and click <a href='"
				+ lastValidURL
				+ "' style=\"color: rgb(100, 155, 255); \">retry</a> to continue."
				+ "<div style='word-wrap: break-word'><font size=1 color=#2e2e2e>"
				+ s
				+ "</font></div>" + "</body></html>");
	}

	public void deregisterBrowser() {
		if (browser == null) {
			throw new IllegalStateException("Context " + getID()
					+ " doesn't have a registered browser");
		}

		browser.setData(CONTEXT_KEY, null);
		browser.removeDisposeListener(this);
		messageDispatcherSWT.deregisterBrowser(browser);
		browser = null;

		if (checkURLEvent != null && !checkURLEvent.isCancelled()) {
			checkURLEvent.cancel();
			checkURLEvent = null;
		}
	}

	/**
	 * Accesses the context associated with the given browser.
	 * 
	 * @param browser holds the context in its application data map
	 * @return the browser's context or <code>null</code> if there is none
	 */
	public static BrowserContext getContext(Browser browser) {
		Object data = browser.getData(CONTEXT_KEY);
		if (data != null && !(data instanceof BrowserContext)) {
			Debug.out("Data in Browser with key " + CONTEXT_KEY
					+ " is not a BrowserContext");
			return null;
		}

		return (BrowserContext) data;
	}

	public void addMessageListener(BrowserMessageListener listener) {
		messageDispatcherSWT.addListener(listener);
	}

	public Object getBrowserData(String key) {
		return browser.getData(key);
	}

	public void setBrowserData(String key, Object value) {
		browser.setData(key, value);
	}

	public boolean sendBrowserMessage(String key, String op) {
		return sendBrowserMessage(key, op, (Map) null);
	}

	public boolean sendBrowserMessage(String key, String op, Map params) {
		StringBuffer msg = new StringBuffer();
		msg.append("az.msg.dispatch('").append(key).append("', '").append(op).append(
				"'");
		if (params != null) {
			msg.append(", ").append(JSONUtils.encodeToJSON(params));
		}
		msg.append(")");

		return executeInBrowser(msg.toString());
	}

	public boolean sendBrowserMessage(String key, String op, Collection params) {
		StringBuffer msg = new StringBuffer();
		msg.append("az.msg.dispatch('").append(key).append("', '").append(op).append(
				"'");
		if (params != null) {
			msg.append(", ").append(JSONUtils.encodeToJSON(params));
		}
		msg.append(")");

		return executeInBrowser(msg.toString());
	}

	protected boolean maySend(String key, String op, Map params) {
		return !pageLoading;
	}

	public boolean executeInBrowser(final String javascript) {
		if (!mayExecute(javascript)) {
			debug("BLOCKED: browser.execute( " + getShortJavascript(javascript)
					+ " )");
			return false;
		}
		if (display == null || display.isDisposed()) {
			debug("CANNOT: browser.execute( " + getShortJavascript(javascript) + " )");
			return false;
		}

		// swallow errors silently
		final String reallyExecute = "try { " + javascript + " } catch ( e ) { }";
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (browser == null || browser.isDisposed()) {
					debug("CANNOT: browser.execute( " + getShortJavascript(javascript)
							+ " )");
				} else if (!browser.execute(reallyExecute)) {
					debug("FAILED: browser.execute( " + getShortJavascript(javascript)
							+ " )");
				} else {
					debug("SUCCESS: browser.execute( " + getShortJavascript(javascript)
							+ " )");
				}
			}
		});

		return true;
	}

	protected boolean mayExecute(String javascript) {
		return !pageLoading;
	}

	public void widgetDisposed(DisposeEvent event) {
		if (event.widget == browser) {
			deregisterBrowser();
		}
	}

	private String getShortJavascript(String javascript) {
		if (javascript.length() < (256 + 3 + 256)) {
			return javascript;
		}
		StringBuffer result = new StringBuffer();
		result.append(javascript.substring(0, 256));
		result.append("...");
		result.append(javascript.substring(javascript.length() - 256));
		return result.toString();
	}

	public boolean getCheckBlocked() {
		return checkBlocked;
	}

	public void setCheckBlocked(boolean checkBlocked) {
		this.checkBlocked = checkBlocked;
	}

	public void setWiggleBrowser(boolean wiggleBrowser) {
		this.wiggleBrowser = wiggleBrowser;
	}
}