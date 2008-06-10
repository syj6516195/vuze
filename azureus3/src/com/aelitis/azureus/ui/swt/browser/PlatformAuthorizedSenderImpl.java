/**
 * Created on May 15, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.browser;

import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.messenger.PlatformAuthorizedSender;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.ui.swt.utils.SWTLoginUtils;
import com.aelitis.azureus.util.Constants;

/**
 * @author TuxPaper
 * @created May 15, 2008
 *
 */
public class PlatformAuthorizedSenderImpl
	implements PlatformAuthorizedSender
{
	String s = null;

	private final Shell shell;

	/**
	 * 
	 */
	public PlatformAuthorizedSenderImpl(Shell shell) {
		this.shell = shell;
	}

	// @see com.aelitis.azureus.core.messenger.PlatformAuthorizedSender#startDownload(java.net.URL, java.lang.String, org.gudy.azureus2.core3.util.AESemaphore, boolean)
	public void startDownload(final URL url, final String data,
			final AESemaphore sem_waitDL, final boolean loginAndRetry) {
		Utils.execSWTThread(new AERunnable() {
			boolean isRetry = false;

			public void runSupport() {
				try {
					final Browser browser = new Browser(shell, SWT.NONE);
					browser.setVisible(false);

					// Safari doesn't return getText() when results aren't text/html
					// IE removes /n when in text/html mode
					String responseType = Constants.isOSX ? "text/html" : "text/plain";
					final String url = Constants.URL_AUTHORIZED_RPC + "?" + data
							+ "&responseType=" + responseType;
					PlatformMessenger.debug("Open Auth URL: "
							+ Constants.URL_AUTHORIZED_RPC + " in " + responseType);
					browser.setUrl(url);

					browser.addProgressListener(new ProgressListener() {
						public void completed(ProgressEvent event) {
							parseAuthorizedListenerResult(browser, sem_waitDL, isRetry,
									loginAndRetry);
						}

						public void changed(ProgressEvent event) {
						}
					});
					
				} catch (Throwable e) {

					Debug.printStackTrace(e);

					sem_waitDL.release();
				}
			}
		});
	}

	private void parseAuthorizedListenerResult(final Browser browser,
			final AESemaphore sem_waitDL, boolean isRetry, boolean loginAndRetry) {
		if (browser.isDisposed()) {
			return;
		}
		try {
			s = browser.getText();

			// authFail message is "authentication required"
			// catch a little bit more, just in case 
			boolean authFail = s.indexOf(";exception;") > 0
					&& s.indexOf("authenticat") > 0 && s.indexOf("required") > 0;

			int i = s.indexOf("0;");

			if (i < 0) {
				String partial = s.length() == 0 ? "" : s.substring(0, Math.min(200,
						s.length()));
				PlatformMessenger.debug("Got BAD Auth Reply ( " + s.length() + "): "
						+ partial);
			}

			if (authFail && loginAndRetry && !isRetry) {
				s = null;

				// add a reserve because finally will release and
				// we still need to wait for login
				sem_waitDL.reserve();
				isRetry = true;

				SWTLoginUtils.waitForLogin(new SWTLoginUtils.loginWaitListener() {
					public void loginComplete() {
						browser.refresh();
					}
					
					public void loginCanceled() {
						browser.dispose();
						sem_waitDL.release();
					}
				});
				// TODO: fail and dispose?
			} else {
				if (i > 0) {
					s = s.substring(i);
				}
				browser.dispose();
			}
		} finally {
			sem_waitDL.release();
		}
	}

	public String getResults() {
		return s;
	}
}
