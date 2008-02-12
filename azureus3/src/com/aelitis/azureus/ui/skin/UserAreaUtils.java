/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.ui.skin;

import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.shells.LightBoxBrowserWindow;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.ILoginInfoListener;
import com.aelitis.azureus.util.LoginInfoManager;
import com.aelitis.azureus.util.LoginInfoManager.LoginInfo;

public class UserAreaUtils
{
	private SWTSkin skin;

	private UIFunctionsSWT uiFunctions = null;

	public UserAreaUtils(SWTSkin skin, UIFunctionsSWT uiFunctions) {
		this.skin = skin;
		this.uiFunctions = uiFunctions;
		hookListeners();

	}

	private void hookListeners() {

		/*
		 * Listens to log in 
		 */
		SWTSkinObject skinObject = skin.getSkinObject("text-log-in");
		if (null != skinObject) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(skinObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {

					String url = Constants.URL_PREFIX_NO_PORT + Constants.URL_LOGIN + "?" + Constants.URL_SUFFIX;
					new LightBoxBrowserWindow(url);

				}
			});
		}

		/*
		 * Listens to log out 
		 */
		skinObject = skin.getSkinObject("text-log-out");
		if (skinObject != null) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(skinObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					String url = Constants.URL_PREFIX_NO_PORT + Constants.URL_LOGOUT + "?" + Constants.URL_SUFFIX;
					if (null != uiFunctions) {
						uiFunctions.viewURL(url, SkinConstants.VIEWID_BROWSE_TAB, 0, 0,
								false, false);
					}
					synchLoginStates(null, null, true);
				}
			});
		}

		/*
		 * Listens to "Get Started"
		 */
		skinObject = skin.getSkinObject("text-get-started");
		if (skinObject != null) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(skinObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					if (null != uiFunctions) {
						String url = Constants.URL_PREFIX_NO_PORT + Constants.URL_REGISTRATION + "?" + Constants.URL_SUFFIX;
						new LightBoxBrowserWindow(url);
					}

				}
			});
		}

		/*
		 * Listens to user name
		 */
		skinObject = skin.getSkinObject("text-user-name");
		if (skinObject != null) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(skinObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					if (null != uiFunctions) {
						String url = Constants.URL_PREFIX_NO_PORT + Constants.URL_PROFILE + "?" + Constants.URL_SUFFIX;
						uiFunctions.viewURL(url, SkinConstants.VIEWID_BROWSE_TAB, 0, 0,
								true, true);
					}

				}
			});
		}

		/*
		 * Listens to "MyAccount"
		 */
		skinObject = skin.getSkinObject("text-my-account");
		if (skinObject != null) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(skinObject);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					if (null != uiFunctions) {
						String url = Constants.URL_PREFIX_NO_PORT + Constants.URL_ACCOUNT + "?" + Constants.URL_SUFFIX;
						uiFunctions.viewURL(url, SkinConstants.VIEWID_BROWSE_TAB, 0, 0,
								true, true);
					}

				}
			});
		}

		/*
		 * Hide the user area on start-up until we get an update from the RuntimeInfoManager;
		 * at which point we will determine which panel to show
		 * 
		 */
		//		skinObject = skin.getSkinObject("user-area-logged-in");
		//		skinObject.setVisible(false);
		//		skinObject = skin.getSkinObject("user-area-logged-out");
		//		skinObject.setVisible(false);
		LoginInfoManager.getInstance().addListener(new ILoginInfoListener() {
			public void loginUpdate(LoginInfo info) {
				synchLoginStates(info.userName, info.userID, info.isNewOrUpdated);
			}
		});

	}

	private void synchLoginStates(String userName, String userID,
			boolean isNewOrUpdated) {

		SWTSkinObject skinObject = null;

		if (null != userName) {
			skinObject = skin.getSkinObject("user-area-logged-in");
			skinObject.setVisible(true);
			skinObject = skin.getSkinObject("user-area-logged-out");
			skinObject.setVisible(false);

			skinObject = skin.getSkinObject("text-user-name");
			if (skinObject instanceof SWTSkinObjectText) {
				((SWTSkinObjectText) skinObject).setText(userName + " ");
			}

			/*
			 * Restart the main browse tab if the login state has changed
			 */
			if (true == isNewOrUpdated) {
				SWTSkinObject browser = skin.getSkinObject("browse");
				if (browser instanceof SWTSkinObjectBrowser) {
					((SWTSkinObjectBrowser) browser).restart();
				}
			}

		} else {
			skinObject = skin.getSkinObject("user-area-logged-out");
			skinObject.setVisible(true);
			skinObject = skin.getSkinObject("user-area-logged-in");
			skinObject.setVisible(false);
			skinObject = skin.getSkinObject("text-user-name");
			if (skinObject instanceof SWTSkinObjectText) {
				((SWTSkinObjectText) skinObject).setText("");
			}
		}

		Utils.execSWTThread(new Runnable() {
			public void run() {
				SWTSkinObject skinObject = skin.getSkinObject("user-area");
				if (null != skinObject) {
					Utils.relayout(skinObject.getControl());
				}
			}
		});

	}

}
