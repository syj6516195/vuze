/*
 * Created on Jul 13, 2006 6:15:55 PM
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
package com.aelitis.azureus.ui.swt.shells.main;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerEvent;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.impl.TorrentOpenOptions;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInputReceiverListener;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarManager;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.FileDownloadWindow;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.ui.swt.minibar.AllTransfersBar;
import org.gudy.azureus2.ui.swt.minibar.MiniBarManager;
import org.gudy.azureus2.ui.swt.plugins.*;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.shells.MessageSlideShell;
import org.gudy.azureus2.ui.swt.update.FullUpdateWindow;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.swt.views.clientstats.ClientStatsView;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.ui.*;
import com.aelitis.azureus.ui.common.table.TableView;
import com.aelitis.azureus.ui.common.updater.UIUpdater;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MdiEntryOpenListener;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.Initializer;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.mdi.BaseMdiEntry;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.plugininstall.SimplePluginInstaller;
import com.aelitis.azureus.ui.swt.shells.BrowserWindow;
import com.aelitis.azureus.ui.swt.shells.RemotePairingWindow;
import com.aelitis.azureus.ui.swt.shells.opentorrent.OpenTorrentOptionsWindow;
import com.aelitis.azureus.ui.swt.shells.opentorrent.OpenTorrentWindow;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.uiupdater.UIUpdaterSWT;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.views.skin.*;
import com.aelitis.azureus.ui.swt.views.skin.SkinnedDialog.SkinnedDialogClosedListener;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBar;
import com.aelitis.azureus.ui.swt.views.skin.sidebar.SideBarEntrySWT;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.ContentNetworkUtils;
import com.aelitis.azureus.util.UrlFilter;

/**
 * @author TuxPaper
 * @created Jul 13, 2006
 *
 */
public class UIFunctionsImpl
	implements UIFunctionsSWT
{
	private final static String MSG_ALREADY_EXISTS = "OpenTorrentWindow.mb.alreadyExists";

	private final static String MSG_ALREADY_EXISTS_NAME = MSG_ALREADY_EXISTS
			+ ".default.name";
	
	private final static LogIDs LOGID = LogIDs.GUI;

	private final com.aelitis.azureus.ui.swt.shells.main.MainWindow mainWindow;

	/**
	 * Stores the current <code>SWTSkin</code> so it can be used by {@link #createMenu(Shell)}
	 */
	private SWTSkin skin = null;

	protected boolean isTorrentMenuVisible;

	/**
	 * @param window
	 */
	public UIFunctionsImpl(
			com.aelitis.azureus.ui.swt.shells.main.MainWindow window) {
		this.mainWindow = window;
		
		COConfigurationManager.addAndFireParameterListener(
				"show_torrents_menu", new ParameterListener() {
					public void parameterChanged(String parameterName) {
						isTorrentMenuVisible = COConfigurationManager.getBooleanParameter("show_torrents_menu");
					}
				});
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#addPluginView(java.lang.String, org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener)
	public void addPluginView(final String viewID, final UISWTViewEventListener l) {
		try {

			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					PluginsMenuHelper.getInstance().addPluginView(viewID, l);
				}
			});

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "addPluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.UIFunctions#bringToFront()
	public void bringToFront() {
		bringToFront(true);
	}

	// @see com.aelitis.azureus.ui.UIFunctions#bringToFront(boolean)
	public void bringToFront(final boolean tryTricks) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				try {
					// this will force active and set !minimized after PW test
					mainWindow.setVisible(true, tryTricks);

				} catch (Exception e) {
					Logger.log(new LogEvent(LOGID, "bringToFront", e));
				}

			}
		});
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#closeDownloadBars()
	public void closeDownloadBars() {
		try {
			Utils.execSWTThreadLater(0, new AERunnable() {
				public void runSupport() {
					MiniBarManager.getManager().closeAll();
				}
			});

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "closeDownloadBars", e));
		}

	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#closePluginView(org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore)
	 */
	public void closePluginView(UISWTViewCore view) {
		try {
			MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
			if (mdi == null) {
				return;
			}
			String id;
			if (view instanceof UISWTViewImpl) {
				id = ((UISWTViewImpl)view).getViewID();
			} else {
				id = view.getClass().getName();
				int i = id.lastIndexOf('.');
				if (i > 0) {
					id = id.substring(i + 1);
				}
			}
			mdi.closeEntry(id);

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "closePluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#closePluginViews(java.lang.String)
	public void closePluginViews(String sViewID) {
		try {
			MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
			if (mdi == null) {
				return;
			}
			mdi.closeEntry(sViewID);
			
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "closePluginViews", e));
		}

	}

	// @see com.aelitis.azureus.ui.UIFunctions#dispose(boolean, boolean)
	public boolean dispose(boolean for_restart, boolean close_already_in_progress) {
		try {
			return mainWindow.dispose(for_restart, close_already_in_progress);
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "Disposing MainWindow", e));
		}
		return false;
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#getMainShell()
	public Shell getMainShell() {
		return mainWindow == null ? null : mainWindow.getShell();
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#getPluginViews()
	public UISWTView[] getPluginViews() {
		try {
			return new UISWTView[0];
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "getPluginViews", e));
		}

		return new UISWTView[0];
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#getSWTPluginInstanceImpl()
	public UISWTInstanceImpl getSWTPluginInstanceImpl() {
		try {
			return mainWindow.getUISWTInstanceImpl();
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "getSWTPluginInstanceImpl", e));
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#openPluginView(org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore, java.lang.String)
	 */
	public void openPluginView(UISWTViewCore view, String name) {
		try {
			MultipleDocumentInterfaceSWT mdi = getMDISWT();
			if (mdi == null) {
				return;
			}
			if (mdi.createEntryFromView(
					MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS, view, name, null,
					true, true, true) != null) {
				return;
			}
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "openPluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#openPluginView(java.lang.String, java.lang.String, org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener, java.lang.Object, boolean)
	public void openPluginView(String sParentID, String sViewID,
			UISWTViewEventListener l, Object dataSource, boolean bSetFocus) {
		try {
			MultipleDocumentInterfaceSWT mdi = getMDISWT();

			if (mdi != null) {
				
				String sidebarParentID = null;
				
				if (UISWTInstance.VIEW_MYTORRENTS.equals(sParentID)) {
					sidebarParentID = SideBar.SIDEBAR_HEADER_TRANSFERS;
				} else if (UISWTInstance.VIEW_MAIN.equals(sParentID)) {
					sidebarParentID = MultipleDocumentInterface.SIDEBAR_HEADER_PLUGINS;
				} else {
					System.err.println("Can't find parent " + sParentID + " for " + sViewID);
				}
				
				MdiEntry entry = mdi.createEntryFromEventListener(sidebarParentID, l, sViewID,
						true, dataSource, null);
				if (bSetFocus) {
					mdi.showEntryByID(sViewID);
				} else if (entry instanceof BaseMdiEntry) {
					// Some plugins (CVS Updater) want their view's composite initialized
					// on OpenPluginView, otherwise they won't do logic users expect
					// (like check for new snapshots).  So, enforce loading entry.
					((BaseMdiEntry) entry).build();
				}
			}
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "openPluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.UIFunctions#refreshIconBar()
	public void refreshIconBar() {
		try {
			SkinView[] tbSkinViews = SkinViewManager.getMultiByClass(ToolBarView.class);
			if (tbSkinViews != null) {
				for (SkinView skinview : tbSkinViews) {
					if (skinview instanceof ToolBarView) {
						ToolBarView tb = (ToolBarView) skinview;
  					if (tb.isVisible()) {
  						tb.refreshCoreToolBarItems();
  					}
					}
				}
			}

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "refreshIconBar", e));
		}

	}

	// @see com.aelitis.azureus.ui.UIFunctions#refreshLanguage()
	public void refreshLanguage() {
		try {
			mainWindow.setSelectedLanguageItem();
			
		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "refreshLanguage", e));
		}

	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#removePluginView(java.lang.String)
	public void removePluginView(String viewID) {
		try {

			PluginsMenuHelper.getInstance().removePluginViews(viewID);

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "removePluginView", e));
		}

	}

	// @see com.aelitis.azureus.ui.UIFunctions#setStatusText(java.lang.String)
	public void setStatusText(final String string) {
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				IMainStatusBar sb = getMainStatusBar();
				if ( sb != null ){
					sb.setStatusText(string);
				}
			}
		});
	}

	// @see com.aelitis.azureus.ui.UIFunctions#setStatusText(int, java.lang.String, com.aelitis.azureus.ui.UIStatusTextClickListener)
	public void setStatusText(final int statustype, final String string,
			final UIStatusTextClickListener l) {
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				IMainStatusBar sb = getMainStatusBar();
				if ( sb != null ){
					sb.setStatusText(statustype, string, l);
				}
			}
		});
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#getMainStatusBar()
	public IMainStatusBar getMainStatusBar() {
		if (mainWindow == null) {
			return null;
		}
		return mainWindow.getMainStatusBar();
	}
	
	// @see com.aelitis.azureus.ui.UIFunctions#showConfig(java.lang.String)
	public boolean showConfig(String section) {
		try {
			boolean uiClassic = COConfigurationManager.getStringParameter("ui").equals("az2");
			if (uiClassic || COConfigurationManager.getBooleanParameter( "Show Options In Side Bar" )) {
				openView(SideBar.SIDEBAR_HEADER_PLUGINS, ConfigView.class, null, section, true);
			} else {
				ConfigShell.getInstance().open(section);
			}
			return true;

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "showConfig", e));
		}

		return false;
	}

	public void openView(final int viewID, final Object data) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				_openView(viewID, data);
			}
		});
	}
		
	private void _openView(int viewID, Object data) {
		switch (viewID) {
			case VIEW_CONSOLE:
				openView(SideBar.SIDEBAR_HEADER_PLUGINS, LoggerView.class,
						null, data, true);
				break;

			case VIEW_ALLPEERS:
				openView(SideBar.SIDEBAR_HEADER_TRANSFERS, PeersSuperView.class,
						null, data, true);
				break;

			case VIEW_PEERS_STATS:
				openView(SideBar.SIDEBAR_HEADER_PLUGINS, ClientStatsView.class,
						null, data, true);
				break;

			case VIEW_CONFIG:
				showConfig((data instanceof String) ? (String) data : null);
				break;

			case VIEW_DM_DETAILS: {
				String id = SideBar.SIDEBAR_TORRENT_DETAILS_PREFIX;
				if (data instanceof DownloadManager) {
					DownloadManager dm = (DownloadManager) data;
					TOTorrent torrent = dm.getTorrent();
					if (torrent != null) {
						try {
							id += torrent.getHashWrapper().toBase32String();
						} catch (TOTorrentException e) {
							e.printStackTrace();
						}
					}
				}
				
				MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
				if (mdi != null) {
					mdi.loadEntryByID(id, true, false, data);
				}
			}
				break;

			case VIEW_DM_MULTI_OPTIONS:
				openView(SideBar.SIDEBAR_HEADER_TRANSFERS,
						TorrentOptionsView.class, null, data, true);
				break;

			case VIEW_MYSHARES:
				openView(SideBar.SIDEBAR_HEADER_TRANSFERS,
						MySharesView.class, null, data, true);
				break;

			case VIEW_MYTORRENTS: {
				MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
				if (mdi != null) {
					mdi.showEntryByID(SideBar.SIDEBAR_SECTION_LIBRARY);
				}
			}
				break;

			case VIEW_MYTRACKER:
				openView(SideBar.SIDEBAR_HEADER_TRANSFERS, MyTrackerView.class,
						null, data, true);
				break;

			case VIEW_TAGS_OVERVIEW:{
				
				MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
				
				if ( mdi != null ){

					mdi.showEntryByID( MultipleDocumentInterface.SIDEBAR_SECTION_TAGS);
				}
				
				break;
			}
			case VIEW_TAG: {
				
				if ( data instanceof Tag ){
					
					Tag tag = (Tag)data;
					
					String id = "Tag." + tag.getTagType().getTagType() + "." + tag.getTagID();
					
					MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
					
					if ( mdi != null ){
						
						mdi.loadEntryByID(id, true, false, data);
					}
				}
				break;
			}
			default:
				break;
		}
	}

	private void openView(final String parentID,
			final Class<? extends UISWTViewEventListener> cla, String id,
			final Object data, final boolean closeable) {
		final MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi == null) {
			return;
		}

		if (id == null) {
			id = cla.getName();
			int i = id.lastIndexOf('.');
			if (i > 0) {
				id = id.substring(i + 1);
			}
		}

		UISWTViewCore viewFromID = mdi.getCoreViewFromID(id);
		if (viewFromID != null) {
			viewFromID.triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, data);
			mdi.showEntryByID(id);
		}

		final String _id = id;
		Utils.execSWTThreadLater(0, new AERunnable() {

			public void runSupport() {
				if (mdi.showEntryByID(_id)) {
					return;
				}
				UISWTViewEventListener l = null;
				try {
					Constructor<?> constructor = cla.getConstructor(new Class[] {
						data.getClass()
					});
					l = (UISWTViewEventListener) constructor.newInstance(new Object[] {
						data
					});
				} catch (Exception e) {
				}

				try {
					if (l == null) {
						l = cla.newInstance();
					}
					mdi.createEntryFromEventListener(parentID, l, _id, closeable,
							data, null );
				} catch (Exception e) {
					Debug.out(e);
				}
				mdi.showEntryByID(_id);
			}
		});

	}
	public UISWTInstance getUISWTInstance() {
		UISWTInstanceImpl impl = mainWindow.getUISWTInstanceImpl();
		if (impl == null) {
			Debug.out("No uiswtinstanceimpl");
		}
		return impl;
	}
	
	// @see com.aelitis.azureus.ui.UIFunctions#viewURL(java.lang.String, java.lang.String, java.lang.String)
	public void viewURL(String url, String target, String sourceRef) {
		viewURL(url, target, 0, 0, true, false);
	}

	public boolean viewURL(final String url, final String target, final int w,
			final int h, final boolean allowResize, final boolean isModal) {

		 mainWindow.getShell().getDisplay().syncExec(new AERunnable() {
			public void runSupport() {
				String realURL = url;
				ContentNetwork cn = ContentNetworkUtils.getContentNetworkFromTarget(target);
				if ( !realURL.startsWith( "http" )
					&& !realURL.startsWith("#")) {
					if ("_blank".equals(target)) {
						realURL = cn.getExternalSiteRelativeURL(realURL, false );
					} else {
						realURL = cn.getSiteRelativeURL(realURL, false );
					}
				}
				if (target == null) {
					if (UrlFilter.getInstance().urlCanRPC(realURL)) {
						realURL = cn.appendURLSuffix(realURL, false, true);
					}
					BrowserWindow window = new BrowserWindow( mainWindow.getShell(), realURL,
							w, h, allowResize, isModal);
					window.waitUntilClosed();
				} else {
					showURL(realURL, target);
				}
			}
		});
		return true;
	}

	public boolean viewURL(final String url, final String target, final double w,
			final double h, final boolean allowResize, final boolean isModal) {

		 mainWindow.getShell().getDisplay().syncExec(new AERunnable() {
			public void runSupport() {
				String realURL = url;
				ContentNetwork cn = ContentNetworkUtils.getContentNetworkFromTarget(target);
				if ( !realURL.startsWith( "http" )){
					if ("_blank".equals(target)) {
						realURL = cn.getExternalSiteRelativeURL(realURL, false );
					} else {
						realURL = cn.getSiteRelativeURL(realURL, false );
					}
				}
				if (target == null) {
					if (UrlFilter.getInstance().urlCanRPC(realURL)) {
						realURL = cn.appendURLSuffix(realURL, false, true);
					}
					BrowserWindow window = new BrowserWindow( mainWindow.getShell(), realURL,
							w, h, allowResize, isModal);
					window.waitUntilClosed();
				} else {
					showURL(realURL, target);
				}
			}
		});
		return true;
	}

	/**
	 * @param url
	 * @param target
	 */
	
	private void showURL(final String url, String target) {

		if ("_blank".equalsIgnoreCase(target)) {
			Utils.launch(url);
			return;
		}

		if (target.startsWith("tab-")) {
			target = target.substring(4);
		}

		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();

		if (MultipleDocumentInterface.SIDEBAR_SECTION_PLUS.equals(target)) {
			SBC_PlusFTUX.setSourceRef(url.substring(1));
			mdi.showEntryByID(target);
			return;
		}
		
		// Note; We don't setSourceRef on ContentNetwork here like we do
		// everywhere else because the source ref should already be set
		// by the caller
		if (mdi == null || !mdi.showEntryByID(target)) {
			Utils.launch(url);
			return;
		}

		MdiEntry entry = mdi.getEntry(target);
		entry.addListener(new MdiEntryOpenListener() {

			public void mdiEntryOpen(MdiEntry entry) {
				entry.removeListener(this);

				mainWindow.setVisible( true, true );

				if (!(entry instanceof SideBarEntrySWT)) {
					return;
				}
				SideBarEntrySWT entrySWT = (SideBarEntrySWT) entry;

				SWTSkinObjectBrowser soBrowser = SWTSkinUtils.findBrowserSO(entrySWT.getSkinObject());

				if (soBrowser != null) {
					//((SWTSkinObjectBrowser) skinObject).getBrowser().setVisible(false);
					if (url == null || url.length() == 0) {
						soBrowser.restart();
					} else {
						String fullURL = url;
						if (UrlFilter.getInstance().urlCanRPC(url)) {
							// 4010 Tux: This shouldn't be.. either determine ContentNetwork from
							//           url or target, or do something..
							fullURL = ConstantsVuze.getDefaultContentNetwork().appendURLSuffix(
									url, false, true);
						}

						soBrowser.setURL(fullURL);
					}
				}
			}
		});
	}
	
	// @see com.aelitis.azureus.ui.UIFunctions#promptUser(java.lang.String, java.lang.String, java.lang.String[], int, java.lang.String, java.lang.String, boolean, int)
	public void promptUser(String title, String text, String[] buttons,
			int defaultOption, String rememberID, String rememberText,
			boolean rememberByDefault, int autoCloseInMS, UserPrompterResultListener l) {
		MessageBoxShell.open(getMainShell(), title, text, buttons,
				defaultOption, rememberID, rememberText, rememberByDefault,
				autoCloseInMS, l);
	}

	// @see com.aelitis.azureus.ui.UIFunctions#getUserPrompter(java.lang.String, java.lang.String, java.lang.String[], int)
	public UIFunctionsUserPrompter getUserPrompter(String title, String text,
			String[] buttons, int defaultOption) {

		MessageBoxShell mb = new MessageBoxShell(title, text, buttons,
				defaultOption);
		return mb;
	}

	public boolean isGlobalTransferBarShown() {
		if (!AzureusCoreFactory.isCoreRunning()) {
			return false;
		}
		return AllTransfersBar.getManager().isOpen(
				AzureusCoreFactory.getSingleton().getGlobalManager());
	}

	public void showGlobalTransferBar() {
		AllTransfersBar.open(getMainShell());
	}

	public void closeGlobalTransferBar() {
		AllTransfersBar.closeAllTransfersBar();
	}

	public void refreshTorrentMenu() {
		if (!isTorrentMenuVisible) {
			return;
		}
		try {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					final MenuItem torrentItem = MenuFactory.findMenuItem(
							mainWindow.getMainMenu().getMenu(IMenuConstants.MENU_ID_MENU_BAR),
							MenuFactory.MENU_ID_TORRENT, false);

					if (null != torrentItem) {

						DownloadManager[] dms = SelectedContentManager.getDMSFromSelectedContent();

						final DownloadManager[] dm_final = dms;
						final boolean detailed_view_final = false;
						if (null == dm_final) {
							torrentItem.setEnabled(false);
						} else {
							TableView<?> tv = SelectedContentManager.getCurrentlySelectedTableView();

							torrentItem.getMenu().setData("TableView", tv);
							torrentItem.getMenu().setData("downloads", dm_final);
							torrentItem.getMenu().setData("is_detailed_view",
									Boolean.valueOf(detailed_view_final));
							torrentItem.setEnabled(true);
						}
					}
				}
			});

		} catch (Exception e) {
			Logger.log(new LogEvent(LOGID, "refreshTorrentMenu", e));
		}
	}

	public IMainMenu createMainMenu(Shell shell) {
		IMainMenu menu;
		boolean uiClassic = COConfigurationManager.getStringParameter("ui").equals("az2");
		if (uiClassic) {
			menu = new org.gudy.azureus2.ui.swt.mainwindow.MainMenu(shell);
		} else {
			menu = new MainMenu(skin, shell);
		}
		return menu;
	}

	public SWTSkin getSkin() {
		return skin;
	}

	public void setSkin(SWTSkin skin) {
		this.skin = skin;
	}

	public IMainWindow getMainWindow() {
		return mainWindow;
	}

	// @see com.aelitis.azureus.ui.UIFunctions#getUIUpdater()
	public UIUpdater getUIUpdater() {
		return UIUpdaterSWT.getInstance();
	}
	
	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#closeAllDetails()
	public void closeAllDetails() {
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		if (mdi == null) {
			return;
		}
		MdiEntry[] sideBarEntries = mdi.getEntries();
		for (int i = 0; i < sideBarEntries.length; i++) {
			MdiEntry entry = sideBarEntries[i];
			String id = entry.getId();
			if (id != null && id.startsWith("DMDetails_")) {
				mdi.closeEntry(id);
			}
		}

	}
	
	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#hasDetailViews()
	public boolean hasDetailViews() {
		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		if (mdi == null) {
			return false;
		}

		MdiEntry[] sideBarEntries = mdi.getEntries();
		for (int i = 0; i < sideBarEntries.length; i++) {
			MdiEntry entry = sideBarEntries[i];
			String id = entry.getId();
			if (id != null && id.startsWith("DMDetails_")) {
				return true;
			}
		}

		return false;
	}
	
	public void 
	performAction(
		int 					action_id, 
		Object 					args, 
		final actionListener 	listener )
	{
		if ( action_id == ACTION_FULL_UPDATE ){
			
			FullUpdateWindow.handleUpdate((String)args, listener );
			
		}else if ( action_id == ACTION_UPDATE_RESTART_REQUEST ){
			
			String MSG_PREFIX = "UpdateMonitor.messagebox.";
			
			String title = MessageText.getString(MSG_PREFIX + "restart.title" );
			
			String text = MessageText.getString(MSG_PREFIX + "restart.text" );
			
			bringToFront();
			
			boolean no_timeout = args instanceof Boolean && ((Boolean)args).booleanValue();
			
			int timeout = 180000;
			
			if ( no_timeout || !PluginInitializer.getDefaultInterface().getPluginManager().isSilentRestartEnabled()){
				
				timeout = -1;
			}
			
			promptUser(
				title, 
				text, 
				new String[] {
					MessageText.getString("UpdateWindow.restart"),
					MessageText.getString("UpdateWindow.restartLater")
				}, 
				0, 
				null, 
				null, 
				false, 
				timeout, 
				new UserPrompterResultListener() 
				{
					public void 
					prompterClosed(
						int result ) 
					{
						listener.actionComplete( result == 0 );
					}
				});
		}else{
			
			Debug.out( "Unknown action " + action_id );
		}
	}

	// @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#showCoreWaitDlg()
	public Shell showCoreWaitDlg() {
		final SkinnedDialog closeDialog = new SkinnedDialog(
				"skin3_dlg_coreloading", "coreloading.body", SWT.TITLE | SWT.BORDER
				| SWT.APPLICATION_MODAL);
		
		closeDialog.setTitle(MessageText.getString("dlg.corewait.title"));
		SWTSkin skin = closeDialog.getSkin();
		SWTSkinObjectButton soButton = (SWTSkinObjectButton) skin.getSkinObject("close");

		final SWTSkinObjectText soWaitTask = (SWTSkinObjectText) skin.getSkinObject("task");

		final SWTSkinObject soWaitProgress = skin.getSkinObject("progress");
		if (soWaitProgress != null) {
			soWaitProgress.getControl().addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent e) {
					Control c = (Control) e.widget;
					Point size = c.getSize();
					e.gc.setBackground(ColorCache.getColor(e.display, "#23a7df"));
					Object data = soWaitProgress.getData("progress");
					if (data instanceof Long) {
						int waitProgress = ((Long) data).intValue();
						int breakX = size.x * waitProgress / 100;
						e.gc.fillRectangle(0, 0, breakX, size.y);
						e.gc.setBackground(ColorCache.getColor(e.display, "#cccccc"));
						e.gc.fillRectangle(breakX, 0, size.x - breakX, size.y);
					}
				}
			});
		}
		
		if (!AzureusCoreFactory.isCoreRunning()) {
			final Initializer initializer = Initializer.getLastInitializer();
			if (initializer != null) {
				initializer.addListener(new InitializerListener() {
					public void reportPercent(final int percent) {
						Utils.execSWTThread(new AERunnable() {
							public void runSupport() {
								if (soWaitProgress != null && !soWaitProgress.isDisposed()) {
									soWaitProgress.setData("progress", new Long(percent));
									soWaitProgress.getControl().redraw();
									soWaitProgress.getControl().update();
								}
							}
						});
						if (percent > 100) {
							initializer.removeListener(this);
						}
					}
				
					public void reportCurrentTask(String currentTask) {
						if (soWaitTask != null && !soWaitTask.isDisposed()) {
							soWaitTask.setText(currentTask);
						}
					}
				});
			}
		}

		if (soButton != null) {
			soButton.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility,
						SWTSkinObject skinObject, int stateMask) {
					closeDialog.close();
				}
			});
		}

		closeDialog.addCloseListener(new SkinnedDialogClosedListener() {
			public void skinDialogClosed(SkinnedDialog dialog) {
			}
		});

		closeDialog.open();
		return closeDialog.getShell();
	}
	
	/**
	 * @param searchText
	 */
	//TODO : Tux Move to utils? Could you also add a "mode" or something that would be added to the url
	// eg: &subscribe_mode=true
	public void doSearch(final String sSearchText) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				doSearch(sSearchText, false);
			}
		});
	}

	public void doSearch(String sSearchText, boolean toSubscribe) {
		if (sSearchText.length() == 0) {
			return;
		}

		if ( checkForSpecialSearchTerm( sSearchText )){
			
			return;
		}
		
		SearchResultsTabArea.SearchQuery sq = new SearchResultsTabArea.SearchQuery(
				sSearchText, toSubscribe);

		MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
		String id = MultipleDocumentInterface.SIDEBAR_SECTION_SEARCH;
		MdiEntry existingEntry = mdi.getEntry(id);
		if (existingEntry != null && existingEntry.isAdded()) {
			SearchResultsTabArea searchClass = (SearchResultsTabArea) SkinViewManager.getByClass(SearchResultsTabArea.class);
			if (searchClass != null) {
				searchClass.anotherSearch(sSearchText, toSubscribe);
			}
			mdi.showEntry(existingEntry);
			return;
		}

		final MdiEntry entry = mdi.createEntryFromSkinRef(
				MultipleDocumentInterface.SIDEBAR_HEADER_DISCOVERY, id,
				"main.area.searchresultstab", sSearchText, null, sq, true, MultipleDocumentInterface.SIDEBAR_POS_FIRST );
		if (entry != null) {
			entry.setImageLeftID("image.sidebar.search");
			entry.setDatasource(sq);
			entry.setViewTitleInfo(new ViewTitleInfo() {
				public Object getTitleInfoProperty(int propertyID) {
					if (propertyID == TITLE_TEXT) {
						SearchResultsTabArea searchClass = (SearchResultsTabArea) SkinViewManager.getByClass(SearchResultsTabArea.class);
						if (searchClass != null && searchClass.sq != null) {
							return searchClass.sq.term;
						}
					}
					return null;
				}
			});
		}
		mdi.showEntryByID(id);
	}
	
	private static boolean
	checkForSpecialSearchTerm(
		String		str )
	{
		str = str.trim();
		
		String hit = UrlUtils.parseTextForURL( str, true, true );
		
		if ( hit == null ){
			
			return( false );
		}
		
		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		
		new FileDownloadWindow( uiFunctions.getMainShell(), str, null, null, true );
			
		return( true );
	}

	
	public void promptForSearch() {
		SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow("Button.search", "search.dialog.text");
		entryWindow.prompt(new UIInputReceiverListener() {
			public void UIInputReceiverClosed(UIInputReceiver receiver) {
				if (receiver.hasSubmittedInput()) {
					doSearch(receiver.getSubmittedInput());
				}
			}
		});
	}

	public MultipleDocumentInterface getMDI() {
		return (MultipleDocumentInterface) SkinViewManager.getByViewID(SkinConstants.VIEWID_MDI);
	}

	public MultipleDocumentInterfaceSWT getMDISWT() {
		return (MultipleDocumentInterfaceSWT) SkinViewManager.getByViewID(SkinConstants.VIEWID_MDI);
	}

	/**
	 * 
	 * @param keyPrefix
	 * @param details may not get displayed
	 * @param textParams
	 */
	public void showErrorMessage(final String keyPrefix,
			final String details, final String[] textParams) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				Shell mainShell = getMainShell();
				if (mainShell.getDisplay().getActiveShell() != null
						|| mainShell.isFocusControl()) {
					new MessageSlideShell(Display.getCurrent(), SWT.ICON_ERROR,
							keyPrefix, details, textParams, -1);
				} else {
					MessageBoxShell mb = new MessageBoxShell(SWT.OK, keyPrefix,
							textParams);
					mb.open(null);
				}
			}
		});
	}
	
	public void forceNotify(final int iconID, final String title, final String text,
			final String details, final Object[] relatedObjects, final int timeoutSecs) {
		
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				int swtIconID = SWT.ICON_INFORMATION;
				switch (iconID) {
					case STATUSICON_WARNING:
						swtIconID = SWT.ICON_WARNING;
						break;
						
					case STATUSICON_ERROR:
						swtIconID = SWT.ICON_ERROR;
						break;
				}
				
				new MessageSlideShell(SWTThread.getInstance().getDisplay(), swtIconID,
						title, text, details, relatedObjects, timeoutSecs);
				
			}
		});
	}
	
	public void 
	installPlugin(
		String 				plugin_id,
		String				resource_prefix,
		actionListener		listener )
	{
		new SimplePluginInstaller( plugin_id, resource_prefix, listener );
	}

	public UIToolBarManager getToolBarManager() {
		Object tb = SkinViewManager.getByClass(ToolBarView.class);
		if (tb instanceof UIToolBarManager) {
			return (UIToolBarManager) tb;
		}
		return null;
	}
	
	public void
	runOnUIThread(
		final int			ui_type,
		final Runnable		runnable )
	{
		if ( ui_type == UIInstance.UIT_SWT ){
		
			Utils.execSWTThread( runnable );
			
		}else{
			
			runnable.run();
		}
	}
	
	public boolean 
	isProgramInstalled(
		String extension, 
		String name ) 
	{
		if ( !extension.startsWith( "." )){
			
			extension = "." + extension;
		}
		
		Program program = Program.findProgram( extension );
		
		return( program == null ? false:(program.getName().toLowerCase(Locale.US).indexOf( name.toLowerCase(Locale.US)) != -1));
	}
	
	public void 
	openRemotePairingWindow() 
	{
		RemotePairingWindow.open();
	}
	
	public void
	playOrStreamDataSource(
		Object 		ds, 
		String 		referal,
		boolean 	launch_already_checked, 
		boolean 	complete_only )
	{
		TorrentListViewsUtils.playOrStreamDataSource( ds, referal, launch_already_checked, complete_only );	
	}
	
	public void 
	setHideAll( 
		boolean hidden ) 
	{
		mainWindow.setHideAll( hidden );
	}
	
	public boolean addTorrentWithOptions(boolean force, 
			final TorrentOpenOptions torrentOptions) {

		if (AzureusCoreFactory.isCoreRunning()) {
			GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
			// Check if torrent already exists in gm, and add if not
			DownloadManager existingDownload = gm.getDownloadManager(torrentOptions.getTorrent());

			if (existingDownload != null) {

				final String fExistingName = existingDownload.getDisplayName();
				final DownloadManager fExistingDownload = existingDownload;

				fExistingDownload.fireGlobalManagerEvent(GlobalManagerEvent.ET_REQUEST_ATTENTION);

				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						boolean can_merge = TorrentUtils.canMergeAnnounceURLs(
								torrentOptions.getTorrent(), fExistingDownload.getTorrent());

						Shell mainShell = UIFunctionsManagerSWT.getUIFunctionsSWT().getMainShell();

						if ((Display.getDefault().getActiveShell() == null
								|| !mainShell.isVisible() || mainShell.getMinimized())
								&& (!can_merge)) {

							new MessageSlideShell(Display.getCurrent(), SWT.ICON_INFORMATION,
									MSG_ALREADY_EXISTS, null, new String[] {
										":" + torrentOptions.sOriginatingLocation,
										fExistingName,
										MessageText.getString(MSG_ALREADY_EXISTS_NAME),
									}, new Object[] {
										fExistingDownload
									}, -1);
						} else {

							if (can_merge) {

								String text = MessageText.getString(MSG_ALREADY_EXISTS
										+ ".text", new String[] {
									":" + torrentOptions.sOriginatingLocation,
									fExistingName,
									MessageText.getString(MSG_ALREADY_EXISTS_NAME),
								});

								text += "\n\n"
										+ MessageText.getString("openTorrentWindow.mb.alreadyExists.merge");

								MessageBoxShell mb = new MessageBoxShell(SWT.YES | SWT.NO,
										MessageText.getString(MSG_ALREADY_EXISTS + ".title"), text);

								mb.open(new UserPrompterResultListener() {
									public void prompterClosed(int result) {
										if (result == SWT.YES) {

											TorrentUtils.mergeAnnounceURLs(
													torrentOptions.getTorrent(),
													fExistingDownload.getTorrent());
										}
									}
								});
							} else {
								MessageBoxShell mb = new MessageBoxShell(SWT.OK,
										MSG_ALREADY_EXISTS, new String[] {
											":" + torrentOptions.sOriginatingLocation,
											fExistingName,
											MessageText.getString(MSG_ALREADY_EXISTS_NAME),
										});
								mb.open(null);
							}
						}
					}
				});

				if (torrentOptions.bDeleteFileOnCancel) {
					File torrentFile = new File(torrentOptions.sFileName);
					torrentFile.delete();
				}
				return true;
			}
		}
		

		if (!force) {
			String showAgainMode = COConfigurationManager.getStringParameter(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS);
			if (showAgainMode != null
					&& ((showAgainMode.equals(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_NEVER)) || (showAgainMode.equals(ConfigurationDefaults.CFG_TORRENTADD_OPENOPTIONS_MANY)
							&& torrentOptions.getFiles() != null && torrentOptions.getFiles().length == 1))) {
				
					// we're about to silently add the download - ensure that it is going to be saved somewhere vaguely sensible
					// as the current save location is simply taken from the 'default download' config which can be blank (for example)
				
				boolean	looks_good = false;
				
				String save_loc = torrentOptions.getParentDir().trim();
				
				if ( save_loc.length() == 0 ){
					
						// blank :(
					
				}else if ( save_loc.startsWith( "." )){
					
						// relative to who knows where
				}else{
					
					File f = new File( save_loc );
					
					if ( !f.exists()){
						
						f.mkdirs();
					}
					
					if ( f.isDirectory() && FileUtil.canWriteToDirectory( f )){
						
						if ( !f.equals(AETemporaryFileHandler.getTempDirectory())){
							
							looks_good = true;
						}
					}
				}
				
				if ( looks_good ){
				
					return TorrentOpener.addTorrent(torrentOptions);
					
				}else{
					
					torrentOptions.setParentDir( "" );
					
					MessageBoxShell mb = 
						new MessageBoxShell(
							SWT.OK | SWT.ICON_ERROR,
							"OpenTorrentWindow.mb.invaliddefsave", 
							new String[]{ save_loc });
					
					mb.open(
						new UserPrompterResultListener() 
						{
							public void 
							prompterClosed(
								int result) 
							{
								OpenTorrentOptionsWindow.addTorrent( torrentOptions );
							}
						});
					
					return( true );
				}
			}
		}
		
		OpenTorrentOptionsWindow.addTorrent( torrentOptions );
		
		return true;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#openTorrentOpenOptions(org.eclipse.swt.widgets.Shell, java.lang.String, java.lang.String[], boolean, boolean, boolean)
	 */
	public void openTorrentOpenOptions(Shell shell, String sPathOfFilesToOpen,
			String[] sFilesToOpen, boolean defaultToStopped, boolean forceOpen) {

		TorrentOpenOptions torrentOptions = new TorrentOpenOptions();
		if (defaultToStopped) {
			torrentOptions.iStartID = TorrentOpenOptions.STARTMODE_STOPPED;
		}
		if (sFilesToOpen == null) {
			new OpenTorrentWindow(shell);
		} else {
			// with no listener, Downloader will open options window if user configured
			TorrentOpener.openTorrentsFromStrings(torrentOptions, shell,
					sPathOfFilesToOpen, sFilesToOpen, null, null, forceOpen);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.swt.UIFunctionsSWT#openTorrentWindow()
	 */
	public void openTorrentWindow() {
		new OpenTorrentWindow(Utils.findAnyShell());
	}
}
