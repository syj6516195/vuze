/**
 * Created on Apr 23, 2008
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
 
package com.aelitis.azureus.ui.swt.tests;

import java.util.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.messenger.*;
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger;
import com.aelitis.azureus.util.ConstantsV3;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;

/**
 * @author TuxPaper
 * @created Apr 23, 2008
 *
 */
public class TestPlatformMessenger
{
	public void initialize(PluginInterface pi) throws PluginException {

		PlatformMessenger.init();
		Map parameters = new HashMap();
		parameters.put("section-type", "browse");
		parameters.put("locale", Locale.getDefault().toString());
		System.out.println(SystemTime.getCurrentTime() + ": queueMessage 0");
		PlatformMessenger.queueMessage(new PlatformMessage("AZMSG", "config",
				"get-browse-sections", parameters, 150),
				new PlatformMessengerListener() {

					public void replyReceived(PlatformMessage message, String replyType,
							Map reply) {
						System.out.println(SystemTime.getCurrentTime() + ": replyRecieved "
								+ message + ";" + replyType + ";" + reply);
					}

					public void messageSent(PlatformMessage message) {
						System.out.println(SystemTime.getCurrentTime() + ": messageSent"
								+ message);
					}

				});

		parameters = new HashMap();
		parameters.put("section-type", "minibrowse");
		parameters.put("locale", Locale.getDefault().toString());
		System.out.println(SystemTime.getCurrentTime() + ": queueMessage 1");
		PlatformMessenger.queueMessage(new PlatformMessage("AZMSG", "config",
				"get-browse-sections", parameters, 550),
				new PlatformMessengerListener() {

					public void replyReceived(PlatformMessage message, String replyType,
							Map reply) {
						System.out.println(SystemTime.getCurrentTime() + ": replyRecieved "
								+ message + ";" + replyType + ";" + reply);
					}

					public void messageSent(PlatformMessage message) {
						System.out.println(SystemTime.getCurrentTime() + ": messageSent"
								+ message);
					}

				});

		System.out.println(SystemTime.getCurrentTime() + ": queueMessage gr");
		PlatformRatingMessenger.getUserRating(1l,
				new String[] { PlatformRatingMessenger.RATE_TYPE_CONTENT
				}, new String[] { "11"
				}, 500);

		System.out.println(SystemTime.getCurrentTime() + ": queueMessage 3");
		//PlatformRatingMessenger.setUserRating("11", 1, false, 500, null);
	}

	public static void dumpMap(Map map, String indent) {
		for (Iterator iterator = map.keySet().iterator(); iterator.hasNext();) {
			Object key = (Object) iterator.next();
			Object value = map.get(key);
			if (value instanceof Map) {
				System.out.println(key + " - " + ((Map) value).size());
				dumpMap((Map) value, indent + "  ");
			}
			System.out.println(indent + key + ": " + value);
		}
	}

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display, SWT.DIALOG_TRIM);
		shell.open();

		int count = 0;
		try {
			AzureusCore core = AzureusCoreFactory.create();
			TestPlatformMessenger test = new TestPlatformMessenger();
			test.initialize(core.getPluginManager().getDefaultPluginInterface());

			while (!shell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}

		} catch (Throwable e) {

			Debug.printStackTrace(e);
		}
	}
}
