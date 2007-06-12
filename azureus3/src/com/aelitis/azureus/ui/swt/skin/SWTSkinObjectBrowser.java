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

package com.aelitis.azureus.ui.swt.skin;

import java.io.File;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.swt.browser.BrowserContext;
import com.aelitis.azureus.ui.swt.browser.listener.ConfigListener;
import com.aelitis.azureus.ui.swt.browser.listener.DisplayListener;
import com.aelitis.azureus.ui.swt.browser.listener.TorrentListener;
import com.aelitis.azureus.ui.swt.browser.listener.publish.LocalHoster;
import com.aelitis.azureus.ui.swt.browser.listener.publish.PublishListener;
import com.aelitis.azureus.util.LocalResourceHTTPServer;

import org.gudy.azureus2.plugins.PluginInterface;

/**
 * @author TuxPaper
 * @created Oct 9, 2006
 *
 */
public class SWTSkinObjectBrowser
	extends SWTSkinObjectBasic
	implements LocalHoster
{

	private Browser browser;

	private Composite cArea;

	private String sStartURL;

	private LocalResourceHTTPServer local_publisher;

	private BrowserContext context;

	/**
	 * @param skin
	 * @param properties
	 * @param sID
	 * @param sConfigID
	 * @param type
	 * @param parent
	 */
	public SWTSkinObjectBrowser(SWTSkin skin, SWTSkinProperties properties,
			String sID, String sConfigID, SWTSkinObject parent) {
		super(skin, properties, sID, sConfigID, "browser", parent);

		AzureusCore core = AzureusCoreFactory.getSingleton();

		cArea = (Composite) parent.getControl();

		browser = new Browser(cArea, SWT.NONE);

		Control widgetIndicator = null;
		String sIndicatorWidgetID = properties.getStringValue(sConfigID
				+ ".indicator");
		if (sIndicatorWidgetID != null) {
			SWTSkinObject skinObjectIndicator = skin.getSkinObjectByID(sIndicatorWidgetID);
			if (skinObjectIndicator != null) {
				widgetIndicator = skinObjectIndicator.getControl();
			}
		}

		context = new BrowserContext(sID, browser,
				widgetIndicator);
		context.addMessageListener(new TorrentListener(core));
		context.addMessageListener(new DisplayListener(browser));
		context.addMessageListener(new ConfigListener(browser));
		PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getDefaultPluginInterface();
		context.addMessageListener(new PublishListener(skin.getShell(), pi, this));

		setControl(browser);
	}

	public Browser getBrowser() {
		return browser;
	}

	public void setURL(String url) {
		if (url == null) {
			browser.setText("");
		} else {
			browser.setUrl(url);
		}
		if (sStartURL == null) {
			sStartURL = url;
			browser.setData("StartURL", url);
		}
		System.out.println(SystemTime.getCurrentTime() + "] Set URL: " + url);
	}

	public void restart() {
		// TODO: Replace the existing rand
		setURL(sStartURL + (sStartURL.indexOf('?') > 0 ? "&" : "?") + "rand=" + SystemTime.getCurrentTime());
	}

	/**
	 * 
	 */
	public void layout() {
		cArea.layout();
	}

	// @see com.aelitis.azureus.ui.swt.browser.listener.publish.LocalHoster#hostFile(java.io.File)
	public URL hostFile(File f) {
		if (local_publisher == null) {
			try {
				PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getDefaultPluginInterface();
				local_publisher = new LocalResourceHTTPServer(pi, null);
			} catch (Throwable e) {
				Debug.out("Failed to create local resource publisher", e);
				return null;
			}
		}
		try {
			return local_publisher.publishResource(f);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public BrowserContext getContext() {
		return context;
	}
}
