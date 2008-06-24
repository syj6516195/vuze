/*
 * Created on May 29, 2006 2:07:38 PM
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadWillBeRemovedListener;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.LogRelationUtils;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AERunnableBoolean;
import org.gudy.azureus2.core3.util.AERunnableObject;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.ui.swt.Alerts;
import org.gudy.azureus2.ui.swt.IconBar;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.PasswordWindow;
import org.gudy.azureus2.ui.swt.UIExitUtilsSWT;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.associations.AssociationChecker;
import org.gudy.azureus2.ui.swt.components.BufferedToolItem;
import org.gudy.azureus2.ui.swt.debug.ObfusticateShell;
import org.gudy.azureus2.ui.swt.mainwindow.IMainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.IMenuConstants;
import org.gudy.azureus2.ui.swt.mainwindow.MainStatusBar;
import org.gudy.azureus2.ui.swt.mainwindow.MenuFactory;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.minibar.AllTransfersBar;
import org.gudy.azureus2.ui.swt.minibar.MiniBarManager;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.shells.MessageSlideShell;
import org.gudy.azureus2.ui.swt.views.IView;
import org.gudy.azureus2.ui.swt.views.stats.VivaldiView;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils.RunDownloadManager;
import org.gudy.azureus2.ui.systray.SystemTraySWT;

import com.aelitis.azureus.activities.VuzeActivitiesManager;
import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.buddy.VuzeBuddyCreator;
import com.aelitis.azureus.buddy.impl.VuzeBuddyManager;
import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.messenger.ClientMessageContext;
import com.aelitis.azureus.core.messenger.PlatformMessage;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.PlatformMessengerListener;
import com.aelitis.azureus.core.messenger.browser.BrowserMessage;
import com.aelitis.azureus.core.messenger.browser.BrowserMessageDispatcher;
import com.aelitis.azureus.core.messenger.config.PlatformBuddyMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformRelayMessenger;
import com.aelitis.azureus.core.torrent.GlobalRatingUtils;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.launcher.Launcher;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.plugins.startstoprules.defaultplugin.StartStopRulesDefaultPlugin;
import com.aelitis.azureus.plugins.startstoprules.defaultplugin.StartStopRulesFPListener;
import com.aelitis.azureus.ui.IUIIntializer;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.Initializer;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.UISkinnableManagerSWT;
import com.aelitis.azureus.ui.swt.UISkinnableSWTListener;
import com.aelitis.azureus.ui.swt.browser.PlatformAuthorizedSenderImpl;
import com.aelitis.azureus.ui.swt.buddy.impl.VuzeBuddyFakeSWTImpl;
import com.aelitis.azureus.ui.swt.buddy.impl.VuzeBuddySWTImpl;
import com.aelitis.azureus.ui.swt.extlistener.StimulusRPC;
import com.aelitis.azureus.ui.swt.skin.SWTSkin;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectBrowser;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectListener;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObjectTab;
import com.aelitis.azureus.ui.swt.skin.SWTSkinTabSet;
import com.aelitis.azureus.ui.swt.skin.SWTSkinTabSetListener;
import com.aelitis.azureus.ui.swt.skin.SWTSkinUtils;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.utils.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;
import com.aelitis.azureus.ui.swt.utils.PlayNowList;
import com.aelitis.azureus.ui.swt.utils.UIUpdatable;
import com.aelitis.azureus.ui.swt.utils.UIUpdater;
import com.aelitis.azureus.ui.swt.utils.UIUpdaterFactory;
import com.aelitis.azureus.ui.swt.views.ViewDownSpeedGraph;
import com.aelitis.azureus.ui.swt.views.ViewUpSpeedGraph;
import com.aelitis.azureus.ui.swt.views.skin.Browse;
import com.aelitis.azureus.ui.swt.views.skin.BuddiesViewer;
import com.aelitis.azureus.ui.swt.views.skin.ButtonBar;
import com.aelitis.azureus.ui.swt.views.skin.DetailPanel;
import com.aelitis.azureus.ui.swt.views.skin.Footer;
import com.aelitis.azureus.ui.swt.views.skin.ManageCdList;
import com.aelitis.azureus.ui.swt.views.skin.ManageDlList;
import com.aelitis.azureus.ui.swt.views.skin.MediaList;
import com.aelitis.azureus.ui.swt.views.skin.MiniBrowse;
import com.aelitis.azureus.ui.swt.views.skin.MiniDownloadList;
import com.aelitis.azureus.ui.swt.views.skin.MiniLibraryList;
import com.aelitis.azureus.ui.swt.views.skin.MiniRecentList;
import com.aelitis.azureus.ui.swt.views.skin.Publish;
import com.aelitis.azureus.ui.swt.views.skin.SearchResultsTabArea;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.ui.swt.views.skin.SkinViewManager;
import com.aelitis.azureus.ui.swt.views.skin.TorrentListViewsUtils;
import com.aelitis.azureus.ui.swt.views.skin.UserAreaUtils;
import com.aelitis.azureus.ui.swt.views.skin.VuzeActivitiesView;
import com.aelitis.azureus.ui.swt.views.skin.VuzeShareUtils;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.DCAdManager;
import com.aelitis.azureus.util.NavigationHelper;
import com.aelitis.azureus.util.PublishUtils;

/**
 * @author TuxPaper
 * @created May 29, 2006
 *
 */
public class MainWindow
	implements SWTSkinTabSetListener, IMainWindow, ObfusticateShell
{

	private static final LogIDs LOGID = LogIDs.GUI;

	protected Shell shell;

	private final Display display;

	private final AzureusCore core;

	private final IUIIntializer uiInitializer;

	private SWTSkin skin;

	private org.gudy.azureus2.ui.swt.mainwindow.MainWindow oldMainWindow;

	private MainMenu menu;

	private UISWTInstanceImpl uiSWTInstanceImpl;

	private UIFunctionsImpl uiFunctions;

	private SystemTraySWT systemTraySWT;

	private Map mapTrackUsage = null;

	private AEMonitor mapTrackUsage_mon = new AEMonitor("mapTrackUsage");

	private long lCurrentTrackTime = 0;

	private long lCurrentTrackTimeIdle = 0;

	private boolean disposedOrDisposing;

	private Object[] dms_Startup;

	protected boolean isReady = false;

	private List topbarViews = new ArrayList();

	private IView activeTopBar;

	private MainStatusBar statusBar;

	private String lastShellStatus = null;

	public static void main(String args[]) {
		if (Launcher.checkAndLaunch(MainWindow.class, args))
			return;
		Initializer.main(new String[0]);
		//org.gudy.azureus2.ui.swt.Main.main(args);
	}

	/**
	 * 
	 */
	public MainWindow(AzureusCore core, Display display,
			final IUIIntializer uiInitializer) {
		this.core = core;
		this.display = display;
		this.uiInitializer = uiInitializer;

		disposedOrDisposing = false;

		VuzeBuddyManager.init(new VuzeBuddyCreator() {
			public VuzeBuddy createBuddy(String publicKey) {
				VuzeBuddyManager.log("created buddy: " + publicKey);
				return new VuzeBuddySWTImpl(publicKey);
			}

			public VuzeBuddy createBuddy() {
				VuzeBuddyManager.log("created buddy");
				return new VuzeBuddySWTImpl();
			}

			// @see com.aelitis.azureus.buddy.VuzeBuddyCreator#createPotentialBuddy(Map)
			public VuzeBuddy createPotentialBuddy(Map map) {
				return new VuzeBuddyFakeSWTImpl(map);
			}
		});

		// Hack for 3014 -> 3016 upgrades on Vista who become an Administrator
		// user after restart.
		if (Constants.isWindows
				&& System.getProperty("os.name").indexOf("Vista") > 0
				&& !COConfigurationManager.getBooleanParameter("vista.adminquit")) {
			File fileFromInstall = FileUtil.getApplicationFile("license.txt");
			if (fileFromInstall.exists()
					&& fileFromInstall.lastModified() < new GregorianCalendar(2007, 06,
							13).getTimeInMillis()) {
				// install older than 3016
				GlobalManager gm = core.getGlobalManager();
				if (gm != null
						&& gm.getDownloadManagers().size() == 0
						&& gm.getStats().getTotalProtocolBytesReceived() < 1024 * 1024 * 100) {
					File fileTestWrite = FileUtil.getApplicationFile("testwrite.dll");
					fileTestWrite.deleteOnExit();
					try {
						FileOutputStream fos = new FileOutputStream(fileTestWrite);
						fos.write(23);
						fos.close();

						COConfigurationManager.setParameter("vista.adminquit", true);
						MessageBoxShell.open(shell,
								MessageText.getString("mb.azmustclose.title"),
								MessageText.getString("mb.azmustclose.text"), new String[] {
									MessageText.getString("Button.ok")
								}, 0);
						if (uiInitializer != null) {
							uiInitializer.abortProgress();
						}
						dispose(false, false);
						return;
					} catch (Exception e) {
					}
				}
			}
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				try {
					createWindow(uiInitializer);
				} catch (Throwable e) {
					Logger.log(new LogAlert(false, "Error Initialize MainWindow", e));
				}
				if (uiInitializer != null) {
					uiInitializer.abortProgress();
				}
			}
		});

		// When a download is added, check for new meta data and
		// un-"wait state" the rating
		// TODO: smart refreshing of meta data ("Refresh On" attribute)
		GlobalManager gm = core.getGlobalManager();
		dms_Startup = gm.getDownloadManagers().toArray();
		gm.addListener(new GlobalManagerListener() {

			public void seedingStatusChanged(boolean seeding_only_mode, boolean b) {
			}

			public void downloadManagerRemoved(DownloadManager dm) {
			}

			public void downloadManagerAdded(final DownloadManager dm) {
				downloadAdded(dm);
			}

			public void destroyed() {
			}

			public void destroyInitiated() {
			}

		}, false);

		gm.addDownloadWillBeRemovedListener(new GlobalManagerDownloadWillBeRemovedListener() {
			public void downloadWillBeRemoved(DownloadManager dm,
					boolean remove_torrent, boolean remove_data)

			throws GlobalManagerDownloadRemovalVetoException {
				TOTorrent torrent = dm.getTorrent();
				if (PublishUtils.isPublished(dm)) {
					String title = MessageText.getString("v3.mb.delPublished.title");
					String text = MessageText.getString("v3.mb.delPublished.text",
							new String[] {
								dm.getDisplayName(),
								Constants.URL_PREFIX,
								Constants.DEFAULT_ADDRESS,
								Constants.URL_PUBLISH_INFO
							});

					MessageBoxShell mb = new MessageBoxShell(shell, title, text,
							new String[] {
								MessageText.getString("v3.mb.delPublished.delete"),
								MessageText.getString("v3.mb.delPublished.cancel")
							}, 1);
					mb.setRelatedObject(dm);

					int result = mb.open();
					if (result != 0) {
						throw new GlobalManagerDownloadRemovalVetoException("", true);
					}
				} else if (PlatformTorrentUtils.isContentDRM(torrent) && remove_data) {

					String prefix = "v3.mb.deletePurchased.";
					String title = MessageText.getString(prefix + "title");
					String text = MessageText.getString(prefix + "text", new String[] {
						dm.getDisplayName()
					});

					MessageBoxShell mb = new MessageBoxShell(shell, title, text,
							new String[] {
								MessageText.getString(prefix + "button.delete"),
								MessageText.getString(prefix + "button.cancel")
							}, 1);
					mb.setRelatedObject(dm);

					int result = mb.open();
					if (result != 0) {
						throw new GlobalManagerDownloadRemovalVetoException("", true);
					}
				}
			}
		});

		Alerts.addListener(new Alerts.AlertListener() {

			public boolean allowPopup(Object[] relatedObjects, int configID) {
				DownloadManager dm = (DownloadManager) LogRelationUtils.queryForClass(
						relatedObjects, DownloadManager.class);

				if (dm == null) {
					return true;
				}
				if (dm.getDownloadState().getFlag(DownloadManagerState.FLAG_LOW_NOISE)) {
					return false;
				}

				HashWrapper hw;
				try {
					hw = dm.getTorrent().getHashWrapper();
					if (PlayNowList.contains(hw)) {
						return false;
					}
				} catch (TOTorrentException e) {
				}
				return true;
			}

		});
	}

	private void processStartupDMS() {
		// must be in a new thread because we don't want to block
		// initilization or any other add listeners
		AEThread2 thread = new AEThread2("v3.mw.dmAdded", true) {
			public void run() {
				long startTime = SystemTime.getCurrentTime();
				if (dms_Startup == null || dms_Startup.length == 0) {
					return;
				}

				for (int i = 0; i < dms_Startup.length; i++) {
					DownloadManager dm = (DownloadManager) dms_Startup[i];
					downloadAdded(dm);
				}

				dms_Startup = null;

				System.out.println("psDMS " + (SystemTime.getCurrentTime() - startTime)
						+ "ms");
			}
		};
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

	private void downloadAdded(final DownloadManager dm) {
		final TOTorrent torrent = dm.getTorrent();
		if (torrent == null) {
			return;
		}

		String hash = null;
		try {
			hash = torrent.getHashWrapper().toBase32String();
		} catch (TOTorrentException e) {
			Debug.out(e);
		}

		String title = PlatformTorrentUtils.getContentTitle(torrent);
		if (title != null && title.length() > 0
				&& dm.getDownloadState().getDisplayName() == null) {
			dm.getDownloadState().setDisplayName(title);
		}

		boolean isContent = PlatformTorrentUtils.isContent(torrent, true);

		final String fHash = hash;

		if (isContent) {
			if (PlatformTorrentUtils.getUserRating(torrent) == -2) {
				PlatformTorrentUtils.setUserRating(torrent, -1);
				PlatformRatingMessenger.getUserRating(new String[] {
					PlatformRatingMessenger.RATE_TYPE_CONTENT
				}, new String[] {
					hash
				}, 5000);
			}

			long now = SystemTime.getCurrentTime();
			long mdRefreshOn = PlatformTorrentUtils.getMetaDataRefreshOn(torrent);
			if (mdRefreshOn < now) {
				PlatformTorrentUtils.log(torrent, "addDM, update MD NOW");
				PlatformTorrentUtils.updateMetaData(torrent, 5000);
			} else {
				PlatformTorrentUtils.log(torrent, "addDM, update MD on "
						+ new Date(mdRefreshOn));
				SimpleTimer.addEvent("Update MD", mdRefreshOn,
						new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								PlatformTorrentUtils.updateMetaData(torrent, 15000);
							}
						});
			}

			long grRefreshOn = GlobalRatingUtils.getRefreshOn(torrent);
			if (grRefreshOn <= now) {
				PlatformRatingMessenger.updateGlobalRating(torrent, 5000);
			} else {
				SimpleTimer.addEvent("Update G.Rating", grRefreshOn,
						new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								PlatformRatingMessenger.updateGlobalRating(torrent, 15000);
							}
						});
			}

			long expiresOn = PlatformTorrentUtils.getExpiresOn(torrent);
			if (expiresOn > now) {
				SimpleTimer.addEvent("dm Expirey", expiresOn,
						new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								dm.getDownloadState().setFlag(
										DownloadManagerState.FLAG_LOW_NOISE, true);
								ManagerUtils.remove(dm, null, true, true);
							}
						});
			}

			if (PublishUtils.isPublished(dm) && dm.getStats().getShareRatio() < 1000
					&& !dm.isForceStart()) {
				dm.setForceStart(true);
			}
		} // isContent
	}

	/**
	 * @param uiInitializer 
	 * 
	 */
	protected void createWindow(IUIIntializer uiInitializer) {
		long startTime = SystemTime.getCurrentTime();

		uiFunctions = new UIFunctionsImpl(this);
		UIFunctionsManager.setUIFunctions(uiFunctions);

		Utils.disposeComposite(shell);

		increaseProgress(uiInitializer, "splash.initializeGui");

		// XXX Temporary.  We'll use our own images
		ImageRepository.loadImagesForSplashWindow(display);

		ImageRepository.loadImages(display);

		shell = new Shell(display, SWT.SHELL_TRIM);

		PlatformMessenger.setAuthorizedTransferListener(new PlatformAuthorizedSenderImpl(
				shell));

		try {
			shell.setData("class", this);
			shell.setText("Vuze");
			Utils.setShellIcon(shell);
			Utils.linkShellMetricsToConfig(shell, "window");

			setupUsageTracker();

			increaseProgress(uiInitializer, "v3.splash.initSkin");

			skin = SWTSkinFactory.getInstance();

			/*
			 * KN: passing the skin to the uifunctions so it can be used by UIFunctionsSWT.createMenu()
			 */
			uiFunctions.setSkin(skin);

			System.out.println("new shell took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			initSkinListeners();

			increaseProgress(uiInitializer, "v3.splash.initSkin");

			skin.initialize(shell, "main.shell", uiInitializer);

			increaseProgress(uiInitializer, "v3.splash.initSkin");
			System.out.println("skin init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			menu = new MainMenu(skin, shell);

			System.out.println("MainMenu init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			if (org.gudy.azureus2.core3.util.Constants.isOSX) {
				try {

					Class ehancerClass = Class.forName("org.gudy.azureus2.ui.swt.osx.CarbonUIEnhancer");

					Method method = ehancerClass.getMethod("registerToolbarToggle",
							new Class[] {
								Shell.class
							});
					method.invoke(null, new Object[] {
						shell
					});

				} catch (Exception e) {
					Debug.printStackTrace(e);
				}

				Listener toggleListener = new Listener() {
					public void handleEvent(Event event) {
						boolean bVisible = (event.type == SWT.Expand);
						SWTSkinUtils.setVisibility(skin, SkinConstants.VIEWID_PLUGINBAR
								+ ".visible", SkinConstants.VIEWID_PLUGINBAR, bVisible, true,
								true);
					}
				};
				shell.addListener(SWT.Expand, toggleListener);
				shell.addListener(SWT.Collapse, toggleListener);
			}

			System.out.println("createWindow init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			increaseProgress(uiInitializer, "v3.splash.initSkin");

			skin.layout();

			System.out.println("skin layout took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			try {
				Utils.createTorrentDropTarget(shell, false);
			} catch (Throwable e) {
				Logger.log(new LogEvent(LOGID, "Drag and Drop not available", e));
			}

			shell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					dispose(false, false);
				}
			});

			shell.addShellListener(new ShellAdapter() {
				public void shellClosed(ShellEvent event) {
					if (disposedOrDisposing) {
						return;
					}
					if (systemTraySWT != null
							&& COConfigurationManager.getBooleanParameter("Enable System Tray")
							&& COConfigurationManager.getBooleanParameter("Close To Tray")) {

						minimizeToTray(event);
					} else {
						event.doit = dispose(false, false);
					}
				}

				public void shellIconified(ShellEvent event) {
					if (disposedOrDisposing) {
						return;
					}
					if (systemTraySWT != null
							&& COConfigurationManager.getBooleanParameter("Enable System Tray")
							&& COConfigurationManager.getBooleanParameter("Minimize To Tray")) {

						minimizeToTray(event);
					}
				}

				public void shellDeiconified(ShellEvent e) {
					if (Constants.isOSX
							&& COConfigurationManager.getBooleanParameter("Password enabled")) {
						shell.setVisible(false);
						if (PasswordWindow.showPasswordWindow(display)) {
							shell.setVisible(true);
						}
					}
				}
			});

			increaseProgress(uiInitializer, "v3.splash.initSkin");
			System.out.println("pre skin widgets init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			try {
				DCAdManager.getInstance().initialize(core);
			} catch (Throwable e) {
			}

			StimulusRPC.hookListeners(core, this);

			increaseProgress(uiInitializer, "v3.splash.initSkin");
			System.out.println("hooks init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			initWidgets();

			increaseProgress(uiInitializer, "v3.splash.initSkin");
			System.out.println("skin widgets init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			SWTSkinTabSet tabSet = skin.getTabSet(SkinConstants.TABSET_MAIN);
			if (tabSet != null) {
				tabSet.addListener(this);
			}
			SWTSkinTabSet subtabSet = skin.getTabSet(SkinConstants.TABSET_DASHBOARD_LEFT);
			if (subtabSet != null) {
				subtabSet.addListener(this);
			}

			increaseProgress(uiInitializer, "v3.splash.hookPluginUI");
			System.out.println("pre SWTInstance init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			// attach the UI to plugins
			// Must be done before initializing views, since plugins may register
			// table columns and other objects
			uiSWTInstanceImpl = new UISWTInstanceImpl(core);
			uiSWTInstanceImpl.init(uiInitializer);

			increaseProgress(uiInitializer, "splash.initializeGui");
			System.out.println("SWTInstance init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			if (tabSet != null) {

				String startTab;

				COConfigurationManager.setBooleanDefault("v3.Start Advanced", false);
				if (COConfigurationManager.getBooleanParameter("v3.Start Advanced")) {
					startTab = SkinConstants.VIEWID_ADVANCED_TAB;
				} else {
					startTab = SkinConstants.VIEWID_HOME_TAB;
				}
				tabSet.setActiveTab(startTab);

				System.out.println("Activate tab " + startTab + " took "
						+ (SystemTime.getCurrentTime() - startTime) + "ms");
				startTime = SystemTime.getCurrentTime();
			}

			increaseProgress(uiInitializer, "splash.initializeGui");

			buildTopBarViews();

			PluginInterface pi = core.getPluginManager().getPluginInterfaceByID(
					"azbpstartstoprules");
			if (pi != null) {
				// plugin is built in, so instead of using IPC, just cast it
				StartStopRulesDefaultPlugin plugin = (StartStopRulesDefaultPlugin) pi.getPlugin();
				plugin.addListener(new StartStopRulesFPListener() {
					public boolean isFirstPriority(Download dl, int numSeeds,
							int numPeers, StringBuffer debug) {
						// FP while our content doesn't have another seed
						boolean b = dl.getState() == Download.ST_SEEDING
								&& PublishUtils.isPublished(dl)
								&& dl.getStats().getAvailability() < 2 && numSeeds == 0;
						return b;
					}
				});
			}

			IconBar.addListener(new IconBar.IconBarListener() {
				// @see org.gudy.azureus2.ui.swt.IconBar.IconBarListener#iconBarInitialized(org.eclipse.swt.widgets.CoolBar, org.gudy.azureus2.ui.swt.IconBar)
				public void iconBarInitialized(CoolBar cb, IconBar ib) {
					final CoolItem coolItem = new CoolItem(cb, SWT.NULL);
					final ToolBar tb = new ToolBar(cb, SWT.FLAT);

					ImageLoader imageLoader = ImageLoaderFactory.getInstance();
					final BufferedToolItem tiShare = new BufferedToolItem(tb, SWT.PUSH);
					final String id = "share";
					tiShare.setData(id, "share");
					((ToolItem) tiShare.getWidget()).setToolTipText(MessageText.getString("v3.MainWindow.button.sendtofriend"));
					boolean newTag = COConfigurationManager.getBooleanParameter(
							"ui.adv.share.newtag", true);

					final Image shareImage = imageLoader.getImage("image.button.sharewhite");

					if (newTag) {
						Image newTagImage = imageLoader.getImage("image.newtag");
						final Rectangle shareBounds = shareImage.getBounds();
						int width = shareBounds.width + newTagImage.getBounds().width;
						Image bg = Utils.createAlphaImage(display, width,
								shareImage.getBounds().height, (byte) 0);

						final Image dstImage1 = Utils.renderTransparency(display, bg,
								shareImage, new Point(0, 0), 255);

						Image dstImage = Utils.renderTransparency(display, dstImage1,
								newTagImage, new Point(shareBounds.width, 0), 255);

						bg.dispose();
						tiShare.setImage(dstImage);

						tiShare.addListener(SWT.Selection, new Listener() {
							public void handleEvent(Event e) {
								COConfigurationManager.setParameter("ui.adv.share.newtag",
										false);
								tiShare.setImage(dstImage1);
								ISelectedContent[] contents = SelectedContentManager.getCurrentlySelectedContent();
								if (contents.length > 0) {
									VuzeShareUtils.getInstance().shareTorrent(contents[0],
											"advanced");
								}
							}
						});
					} else {
						tiShare.setImage(shareImage);

						tiShare.addListener(SWT.Selection, new Listener() {
							public void handleEvent(Event e) {
								ISelectedContent[] contents = SelectedContentManager.getCurrentlySelectedContent();
								if (contents.length > 0) {
									VuzeShareUtils.getInstance().shareTorrent(contents[0],
											"advanced");
								}
							}
						});
					}

					ib.addITemKeyToControl(id, tiShare);
					tb.pack();
					Point p = tb.getSize();
					coolItem.setControl(tb);
					coolItem.setSize(p.x, p.y);
					coolItem.setMinimumSize(p.x, p.y);
				}
			});

			ManagerUtils.setRunRunnable(new RunDownloadManager() {
				public void run(DownloadManager dm) {
					if (PlatformTorrentUtils.isContent(dm.getTorrent(), true)) {
						TorrentListViewsUtils.playOrStream(dm);
					} else {
						Utils.launch(dm.getSaveLocation().toString());
					}
				}
			});
		} finally {

			shell.layout(true, true);

			System.out.println("sb="
					+ COConfigurationManager.getBooleanParameter(SkinConstants.VIEWID_PLUGINBAR
							+ ".visible"));

			String configID = SkinConstants.VIEWID_PLUGINBAR + ".visible";
			if (false == ConfigurationDefaults.getInstance().doesParameterDefaultExist(
					configID)) {
				COConfigurationManager.setBooleanDefault(configID, true);
			}
			setVisible(WINDOW_ELEMENT_SEARCHBAR,
					COConfigurationManager.getBooleanParameter(configID));

			configID = "Footer.visible";
			if (false == ConfigurationDefaults.getInstance().doesParameterDefaultExist(
					configID)) {
				COConfigurationManager.setBooleanDefault(configID, true);
			}
			setVisible(WINDOW_ELEMENT_FOOTER,
					COConfigurationManager.getBooleanParameter(configID));

			showMainWindow();

			//================

			increaseProgress(uiInitializer, "splash.initializeGui");

			System.out.println("shell.open took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			processStartupDMS();

			System.out.println("processStartupDMS took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			VuzeActivitiesManager.initialize(core);

			System.out.println("vuzeactivities init took "
					+ (SystemTime.getCurrentTime() - startTime) + "ms");
			startTime = SystemTime.getCurrentTime();

			NavigationHelper.addListener(new NavigationHelper.navigationListener() {
				public void processCommand(final int type, final String[] args) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {

							UIFunctions uif = UIFunctionsManager.getUIFunctions();

							if (type == NavigationHelper.COMMAND_SWITCH_TO_TAB) {

								SWTSkin skin = SWTSkinFactory.getInstance();
								SWTSkinObject skinObject = skin.getSkinObject(args[0]);
								if (skinObject != null) {
									skin.activateTab(skinObject);
								}

								if (uif != null) {

									uif.bringToFront();
								}
							} else if (type == NavigationHelper.COMMAND_BUDDY_SYNC) {

								try {
									PlatformRelayMessenger.fetch(0);
									PlatformBuddyMessenger.sync(null);
									PlatformBuddyMessenger.getInvites();
								} catch (NotLoggedInException e1) {
								}

							} else if (type == NavigationHelper.COMMAND_CONDITION_CHECK) {

								if (args[0].equals(NavigationHelper.COMMAND_CHECK_BUDDY_MANAGER)) {

									if (args[1].equals(NavigationHelper.COMMAND_CHECK_BUDDY_MANAGER_ENABLED)) {

										if (!VuzeBuddyManager.isEnabled()) {

											VuzeBuddyManager.showDisabledDialog();

											if (uif != null) {

												uif.bringToFront();
											}
										}
									}
								}
							}
						}
					});
				}
			});
		}
	}

	/**
	 * @param uiInitializer
	 * @param taskKey TODO
	 *
	 * @since 3.0.4.3
	 */
	private void increaseProgress(IUIIntializer uiInitializer, String taskKey) {
		if (uiInitializer != null) {
			uiInitializer.increaseProgress();
			if (taskKey != null) {
				uiInitializer.reportCurrentTask(MessageText.getString(taskKey));
			}
		}
		// XXX Disabled because plugin update window will pop up and take control
		// 		 of the dispatch loop..
		if (false && Utils.isThisThreadSWT()) {
			// clean the dispatch loop so the splash screen gets updated
			int i = 1000;
			while (display.readAndDispatch() && i > 0) {
				i--;
			}
			//if (i < 999) {
			//	System.out.println("dispatched " + (1000 - i));
			//}
		}
	}

	public boolean dispose(final boolean for_restart,
			final boolean close_already_in_progress) {
		if (disposedOrDisposing) {
			return true;
		}
		return Utils.execSWTThreadWithBool("v3.MainWindow.dispose",
				new AERunnableBoolean() {
					public boolean runSupport() {
						return _dispose(for_restart, close_already_in_progress);
					}
				});
	}

	public boolean _dispose(boolean bForRestart, boolean bCloseAlreadyInProgress) {
		if (disposedOrDisposing) {
			return true;
		}

		isReady = false;

		disposedOrDisposing = true;
		if (oldMainWindow != null) {
			boolean res = oldMainWindow.dispose(bForRestart, bCloseAlreadyInProgress);
			oldMainWindow = null;

			if (res == false) {
				disposedOrDisposing = false;
				return false;
			}
		} else {
			if (!UIExitUtilsSWT.canClose(core.getGlobalManager(), bForRestart)) {
				disposedOrDisposing = false;
				return false;
			}

			UIExitUtilsSWT.uiShutdown();
		}

		if (systemTraySWT != null) {
			systemTraySWT.dispose();
		}

		/**
		 * Explicitly force the transfer bar location to be saved (if appropriate and open).
		 * 
		 * We can't rely that the normal mechanism for doing this won't fail (which it usually does)
		 * when the GUI is being disposed of.
		 */
		AllTransfersBar transfer_bar = AllTransfersBar.getBarIfOpen(AzureusCoreFactory.getSingleton().getGlobalManager());
		if (transfer_bar != null) {
			transfer_bar.forceSaveLocation();
		}

		if (!SWTThread.getInstance().isTerminated()) {
			SWTThread.getInstance().getInitializer().stopIt(bForRestart, false);
		}

		mapTrackUsage_mon.enter();
		try {
			if (mapTrackUsage != null) {
				String id = getUsageActiveTabID();
				if (id != null) {
					if (lastShellStatus == null) {
						lastShellStatus = id;
					}
					updateMapTrackUsage(lastShellStatus);
				}

				FileUtil.writeResilientFile(new File(SystemProperties.getUserPath(),
						"timingstats.dat"), mapTrackUsage);
			}
		} finally {
			mapTrackUsage_mon.exit();
		}

		return true;
	}

	private String getUsageActiveTabID() {
		try {
			SWTSkinTabSet tabSetMain = skin.getTabSet(SkinConstants.TABSET_MAIN);
			if (tabSetMain != null && tabSetMain.getActiveTab() != null) {
				String id = tabSetMain.getActiveTab().getSkinObjectID();
				if (id.equals("maintabs.home")) {
					SWTSkinTabSet tabSetLeft = skin.getTabSet(SkinConstants.TABSET_DASHBOARD_LEFT);
					if (tabSetLeft != null && tabSetLeft.getActiveTab() != null) {
						id += "-"
								+ tabSetLeft.getActiveTab().getSkinObjectID().substring(8);
					}
				}
				if (id.length() > 9) {
					id = id.substring(9);
				}
				return id;
			}
		} catch (Exception e) {
		}
		return "unknown";
	}

	/**
	 * 
	 */
	private void setupUsageTracker() {
		mapTrackUsage_mon.enter();
		try {
			if (COConfigurationManager.getBooleanParameter("Send Version Info")
					&& PlatformConfigMessenger.allowSendStats()) {

				mapTrackUsage = new HashMap();

				File f = new File(SystemProperties.getUserPath(), "timingstats.dat");
				if (f.exists()) {
					final Map oldMapTrackUsage = FileUtil.readResilientFile(f);
					PlatformConfigMessenger.sendUsageStats(oldMapTrackUsage,
							f.lastModified(), new PlatformMessengerListener() {

								public void messageSent(PlatformMessage message) {
								}

								public void replyReceived(PlatformMessage message,
										String replyType, Map reply) {
									if (mapTrackUsage == null) {
										return;
									}
									mapTrackUsage_mon.enter();
									try {
										if (replyType.equals(PlatformMessenger.REPLY_EXCEPTION)) {
											for (Iterator iterator = oldMapTrackUsage.keySet().iterator(); iterator.hasNext();) {
												String key = (String) iterator.next();
												Long value = (Long) oldMapTrackUsage.get(key);

												Long oldValue = (Long) mapTrackUsage.get(key);
												if (oldValue != null) {
													value = new Long(value.longValue()
															+ oldValue.longValue());
												}
												mapTrackUsage.put(key, value);
											}
										}
									} finally {
										mapTrackUsage_mon.exit();
									}
								}

							});
				}

				SimpleTimer.addPeriodicEvent("UsageTracker", 1000,
						new TimerEventPerformer() {
							long lLastMouseMove = SystemTime.getCurrentTime();

							Point ptLastMousePos = new Point(0, 0);

							public void perform(TimerEvent event) {
								Utils.execSWTThread(new AERunnable() {
									public void runSupport() {
										if (shell == null || shell.isDisposed()
												|| shell.getDisplay().getActiveShell() == null) {
											// so when we become active again, we count a few
											// seconds (if the mouse moves)
											if (ptLastMousePos.x > 0) {
												ptLastMousePos.x = 0;
												ptLastMousePos.y = 0;
												lLastMouseMove = 0;
											}
											return;
										}

										Point pt = shell.getDisplay().getCursorLocation();
										if (pt.equals(ptLastMousePos)) {
											return;
										}
										ptLastMousePos = pt;

										long now = SystemTime.getCurrentTime();
										if (lLastMouseMove > 0) {
											long diff = now - lLastMouseMove;
											if (diff < 10000) {
												lCurrentTrackTime += diff;
											} else {
												lCurrentTrackTimeIdle += diff;
											}
										}

										lLastMouseMove = now;
									}
								});
							}
						});

				Listener lActivateDeactivate = new Listener() {
					long start;

					public void handleEvent(Event event) {
						if (event.type == SWT.Activate) {
							lCurrentTrackTimeIdle = 0;
							if (start > 0 && lastShellStatus != null) {
								lCurrentTrackTime = SystemTime.getCurrentTime() - start;
								updateMapTrackUsage(lastShellStatus);
							}
							lastShellStatus = null;
						} else {
							updateMapTrackUsage(getUsageActiveTabID());
							lastShellStatus = shell.getMinimized() || !shell.isVisible()
									? "idle-minimized" : "idle-nofocus";
							start = SystemTime.getCurrentTime();
						}
					}
				};
				shell.addListener(SWT.Activate, lActivateDeactivate);
				shell.addListener(SWT.Deactivate, lActivateDeactivate);

			} else {
				mapTrackUsage = null;
			}
		} catch (Exception e) {
			Debug.out(e);
		} finally {
			mapTrackUsage_mon.exit();
		}
	}

	private void showMainWindow() {
		if (oldMainWindow != null) {
			oldMainWindow.postPluginSetup(-1, 0);
		}

		boolean isOSX = org.gudy.azureus2.core3.util.Constants.isOSX;
		// No tray access on OSX yet
		boolean bEnableTray = COConfigurationManager.getBooleanParameter("Enable System Tray")
				&& (!isOSX || SWT.getVersion() > 3300);
		boolean bPassworded = COConfigurationManager.getBooleanParameter("Password enabled");
		boolean bStartMinimize = bEnableTray
				&& (bPassworded || COConfigurationManager.getBooleanParameter("Start Minimized"));

		if (!bStartMinimize) {
			shell.open();
			if (!isOSX) {
				shell.forceActive();
			}
		} else if (isOSX) {
			shell.setMinimized(true);
			shell.setVisible(true);
		}

		if (bEnableTray) {

			try {
				systemTraySWT = new SystemTraySWT();

			} catch (Throwable e) {

				Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
						"Upgrade to SWT3.0M8 or later for system tray support."));
			}

			if (bStartMinimize) {
				minimizeToTray(null);
			}
			//Only show the password if not started minimized
			//Correct bug #878227
			else {
				if (bPassworded) {
					minimizeToTray(null);
					setVisible(true); // invokes password
				}
			}
		}

		// do this before other checks as these are blocking dialogs to force order

		if (uiInitializer != null) {

			uiInitializer.initializationComplete();
		}

		AssociationChecker.checkAssociations();

		core.triggerLifeCycleComponentCreated(uiFunctions);

		isReady = true;
	}

	public void setVisible(final boolean visible) {
		setVisible(visible, true);
	}

	public void setVisible(final boolean visible, final boolean tryTricks) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				boolean currentlyVisible = shell.getVisible() && !shell.getMinimized();
				if (visible && !currentlyVisible) {
					if (COConfigurationManager.getBooleanParameter("Password enabled")) {
						if (!PasswordWindow.showPasswordWindow(display)) {
							shell.setVisible(false);
							return;
						}
					}
				}

				ArrayList wasVisibleList = null;
				boolean bHideAndShow = false;
				// temp disabled
				//tryTricks && visible && Constants.isWindows && display.getActiveShell() != shell;
				if (bHideAndShow) {
					wasVisibleList = new ArrayList();
					// We don't want the window to just flash and not open, so:
					// -Minimize main shell
					// -Set all shells invisible
					try {
						shell.setMinimized(true);
						Shell[] shells = shell.getDisplay().getShells();
						for (int i = 0; i < shells.length; i++) {
							if (shells[i].isVisible()) {
								wasVisibleList.add(shells[i]);
								shells[i].setVisible(false);
							}
						}
					} catch (Exception e) {
					}
				}

				if (visible) {
					shell.setMinimized(false);
					if (!currentlyVisible
							&& COConfigurationManager.getBooleanParameter("window.maximized")) {
						shell.setMaximized(true);
					}
				} else {
					// XXX hack for release.. should not access param outside Utils.linkShellMetrics
					COConfigurationManager.setParameter("window.maximized",
							shell.getMaximized());
				}

				shell.setVisible(visible);
				if (visible) {
					shell.forceActive();

					if (bHideAndShow) {
						try {
							Shell[] shells = shell.getDisplay().getShells();
							for (int i = 0; i < shells.length; i++) {
								if (shells[i] != shell) {
									if (wasVisibleList.contains(shells[i])) {
										shells[i].setVisible(visible);
									}
									shells[i].setFocus();
								}
							}
						} catch (Exception e) {
						}
					}
				}

			}
		});
	}

	private void minimizeToTray(ShellEvent event) {
		//Added this test so that we can call this method with null parameter.
		if (event != null) {
			event.doit = false;
		}

		// XXX hack for release.. should not access param outside Utils.linkShellMetrics
		COConfigurationManager.setParameter("window.maximized",
				shell.getMaximized());
		shell.setVisible(false);
		MiniBarManager.getManager().setAllVisible(true);
	}

	/**
	 * Associates every view ID that we use to a class, and creates the class
	 * on first EVENT_SHOW.
	 */
	private void initSkinListeners() {
		final Map views = new HashMap();

		// List of all views ids we use
		views.put("minibrowse-area", MiniBrowse.class);
		views.put("searchresults-area", SearchResultsTabArea.class);
		views.put("minidownload-list", MiniDownloadList.class);
		views.put("minirecent-list", MiniRecentList.class);

		views.put("browse-area", Browse.class);

		views.put("manage-dl-list", ManageDlList.class);
		views.put("manage-cd-list", ManageCdList.class);

		views.put("my-media-list", MediaList.class);

		views.put("publish-area", Publish.class);

		views.put("minilibrary-list", MiniLibraryList.class);

		views.put("vuzeevents-list", VuzeActivitiesView.class);

		views.put(SkinConstants.VIEWID_FOOTER, Footer.class);
		views.put(SkinConstants.VIEWID_DETAIL_PANEL, DetailPanel.class);

		SWTSkinObjectListener l = new SWTSkinObjectListener() {
			public Object eventOccured(SWTSkinObject skinObject, int eventType,
					Object params) {
				if (eventType == EVENT_SHOW) {
					Object key = skinObject.getViewID();
					Class cla = (Class) views.get(key);
					if (cla != null) {
						try {
							SkinView skinView = (SkinView) cla.newInstance();
							SkinViewManager.add(skinView);
							skinObject.addListener(skinView);
							skinView.eventOccured(skinObject, eventType, params);
							views.remove(key);
						} catch (InstantiationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} else if (eventType == EVENT_SELECT) {
					//System.out.println("select " + skinObject);
				} else {
				}
				return null;
			}
		};

		for (Iterator iterator = views.keySet().iterator(); iterator.hasNext();) {
			String viewID = (String) iterator.next();
			skin.addListener(viewID, l);
		}

		UISkinnableManagerSWT skinnableManagerSWT = UISkinnableManagerSWT.getInstance();
		skinnableManagerSWT.addSkinnableListener(MessageBoxShell.class.toString(),
				new UISkinnableSWTListener() {
					public void skinBeforeComponents(Composite composite,
							Object skinnableObject, Object[] relatedObjects) {
						Color colorBG = skin.getSkinProperties().getColor("color.mainshell");
						Color colorLink = skin.getSkinProperties().getColor(
								"color.links.normal");
						Color colorText = skin.getSkinProperties().getColor("color.text.fg");

						//composite.setBackground(colorBG);
						//composite.setForeground(colorText);

						MessageBoxShell shell = (MessageBoxShell) skinnableObject;
						//shell.setUrlColor(colorLink);

						TOTorrent torrent = null;
						DownloadManager dm = (DownloadManager) LogRelationUtils.queryForClass(
								relatedObjects, DownloadManager.class);
						if (dm != null) {
							torrent = dm.getTorrent();
						} else {
							torrent = (TOTorrent) LogRelationUtils.queryForClass(
									relatedObjects, TOTorrent.class);
						}

						if (torrent != null) {
							byte[] contentThumbnail = PlatformTorrentUtils.getContentThumbnail(torrent);
							if (contentThumbnail != null) {
								try {
									ByteArrayInputStream bis = new ByteArrayInputStream(
											contentThumbnail);
									final Image img = new Image(Display.getDefault(), bis);

									shell.setLeftImage(img);

									composite.addDisposeListener(new DisposeListener() {
										public void widgetDisposed(DisposeEvent e) {
											if (!img.isDisposed()) {
												img.dispose();
											}
										}
									});
								} catch (Exception e) {

								}
							}
						}
					}

					public void skinAfterComponents(Composite composite,
							Object skinnableObject, Object[] relatedObjects) {
					}
				});

		skinnableManagerSWT.addSkinnableListener(
				MessageSlideShell.class.toString(), new UISkinnableSWTListener() {

					public void skinBeforeComponents(Composite composite,
							Object skinnableObject, Object[] relatedObjects) {
						if (skinnableObject instanceof MessageSlideShell) {
							Color colorBG = skin.getSkinProperties().getColor(
									"color.mainshell");
							Color colorLink = skin.getSkinProperties().getColor(
									"color.links.normal");
							Color colorText = skin.getSkinProperties().getColor(
									"color.text.fg");

							composite.setBackground(colorBG);
							composite.setForeground(colorText);
							//bg = composite.getBackground(); // temp disable

							final Image image = new Image(composite.getDisplay(), 250, 300);

							TOTorrent torrent = null;
							DownloadManager dm = (DownloadManager) LogRelationUtils.queryForClass(
									relatedObjects, DownloadManager.class);
							if (dm != null) {
								torrent = dm.getTorrent();
							} else {
								torrent = (TOTorrent) LogRelationUtils.queryForClass(
										relatedObjects, TOTorrent.class);
							}

							MessageSlideShell shell = (MessageSlideShell) skinnableObject;
							shell.setUrlColor(colorLink);
							shell.setColorFG(colorText);

							byte[] contentThumbnail = PlatformTorrentUtils.getContentThumbnail(torrent);
							GC gc = new GC(image);
							try {
								if (colorBG != null) {
									gc.setBackground(colorBG);
									gc.fillRectangle(image.getBounds());
								}
								if (contentThumbnail != null) {

									try {
										ByteArrayInputStream bis = new ByteArrayInputStream(
												contentThumbnail);
										final Image img = new Image(Display.getDefault(), bis);
										Rectangle imgBounds = img.getBounds();
										double pct = 35.0 / imgBounds.height;
										int w = (int) (imgBounds.width * pct);

										try {
											gc.setAdvanced(true);
											gc.setInterpolation(SWT.HIGH);
										} catch (Exception e) {
											// not important if we can't set advanced
										}

										gc.drawImage(img, 0, 0, imgBounds.width, imgBounds.height,
												0, 265, w, 35);
										img.dispose();
									} catch (Exception e) {

									}

								}
							} finally {
								gc.dispose();
							}
							shell.setImgPopup(image);

							composite.addListener(SWT.Dispose, new Listener() {
								public void handleEvent(Event event) {
									if (!image.isDisposed()) {
										image.dispose();
									}
								}
							});
						}
					}

					public void skinAfterComponents(Composite composite,
							Object skinnableObject, Object[] relatedObjects) {
						if (true) {
							return; // temp disable
						}
						Color bg = skin.getSkinProperties().getColor("color.mainshell");
						if (bg != null) {
							composite.setBackground(bg);
						}
						Color fg = skin.getSkinProperties().getColor("color.section.header");
						if (fg != null) {
							setChildrenFG(composite, fg);
						}
						composite.setBackgroundMode(SWT.INHERIT_DEFAULT);
					}
				});
	}

	private void setChildrenFG(Control parent, Color color) {
		parent.setForeground(color);
		if (parent instanceof Composite) {
			Control[] children = ((Composite) parent).getChildren();
			for (int i = 0; i < children.length; i++) {
				Control control = children[i];
				if (!(control instanceof Button)
						|| (((Button) control).getStyle() & SWT.CHECK) > 0) {
					setChildrenFG(control, color);
				}
			}
		}
	}

	/**
	 * 
	 */
	private void initWidgets() {
		SWTSkinObject skinObject;

		/*
		 * Directly loading the buddies viewer since we need to access it
		 * before it's even shown for the first time
		 */
		skinObject = skin.getSkinObject(SkinConstants.VIEWID_BUDDIES_VIEWER);
		if (null != skinObject) {
			BuddiesViewer skinView = new BuddiesViewer();
			SkinViewManager.add(skinView);
			skinObject.addListener(skinView);
		}

		/*
		 * Directly loading the Button Bar since we need to access it
		 * before it's even shown for the first time
		 */
		skinObject = skin.getSkinObject(SkinConstants.VIEWID_BUTTON_BAR);
		if (null != skinObject) {
			ButtonBar skinView = new ButtonBar();
			SkinViewManager.add(skinView);
			skinObject.addListener(skinView);
		}

		skinObject = skin.getSkinObject("statusbar");
		if (skinObject != null) {
			final Composite cArea = (Composite) skinObject.getControl();

			statusBar = new MainStatusBar();
			Composite composite = statusBar.initStatusBar(core,
					core.getGlobalManager(), display, cArea);

			composite.setLayoutData(Utils.getFilledFormData());

			UIUpdater uiUpdater = UIUpdaterFactory.getInstance();
			// XXX Could just make MainStatusBar implement UIUpdatable
			uiUpdater.addUpdater(new UIUpdatable() {
				public String getUpdateUIName() {
					return "StatusBar";
				}

				public void updateUI() {
					statusBar.refreshStatusBar();
					if (systemTraySWT != null) {
						systemTraySWT.update();
					}
				}
			});
		}

		skinObject = skin.getSkinObject("search-text");
		if (skinObject != null) {
			attachSearchBox(skinObject);
		}

		skinObject = skin.getSkinObject(SkinConstants.VIEWID_PLUGINBAR);
		if (skinObject != null) {
			Menu topbarMenu = new Menu(shell, SWT.POP_UP);

			MainMenu.createViewMenuItem(skin, topbarMenu, "v3.MainWindow.menu.view."
					+ SkinConstants.VIEWID_PLUGINBAR, SkinConstants.VIEWID_PLUGINBAR
					+ ".visible", SkinConstants.VIEWID_PLUGINBAR, true);

			addMenuAndNonTextChildren((Composite) skinObject.getControl(), topbarMenu);

			skinObject = skin.getSkinObject("tabbar");
			if (skinObject != null) {
				addMenuAndNonTextChildren((Composite) skinObject.getControl(),
						topbarMenu);
			}
		}

		/*
		 * Init the user area for login/logout info
		 */
		new UserAreaUtils(skin, uiFunctions);

		/*
		 * Hides the buddy bar unless a command line parameter is specified
		 * WARNING: TODO -- This is temporary and must be removed once the buddies features are complete
		 */
		if (Constants.DISABLE_BUDDIES_BAR) {
			COConfigurationManager.setParameter("Footer.visible", false);
		}

		shell.layout(true, true);
	}

	private void addMenuAndNonTextChildren(Composite parent, Menu menu) {
		parent.setMenu(menu);

		Control[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control control = children[i];
			if (control instanceof Composite) {
				Composite c = (Composite) control;
				addMenuAndNonTextChildren(c, menu);
			} else if (!(control instanceof Text)) {
				control.setMenu(menu);
			}
		}
	}

	/**
	 * @param skinObject
	 *
	 * @since 3.0.1.1
	 */
	private void buildTopBarViews() {
		// TODO actually use plugins..
		SWTSkinObject skinObject = skin.getSkinObject("topbar-plugins");
		if (skinObject == null) {
			return;
		}

		try {
			IView[] coreTopBarViews = {
				new ViewDownSpeedGraph(),
				new ViewUpSpeedGraph(),
				new VivaldiView(false)
			};

			final Composite composite = (Composite) skinObject.getControl();

			for (int i = 0; i < coreTopBarViews.length; i++) {
				IView view = coreTopBarViews[i];
				addTopBarView(view, composite);
			}

			Map pluginViews = null;
			pluginViews = uiSWTInstanceImpl.getViewListeners(UISWTInstance.VIEW_TOPBAR);
			if (pluginViews != null) {
				String[] sNames = (String[]) pluginViews.keySet().toArray(new String[0]);
				for (int i = 0; i < sNames.length; i++) {
					UISWTViewEventListener l = (UISWTViewEventListener) pluginViews.get(sNames[i]);
					if (l != null) {
						try {
							UISWTViewImpl view = new UISWTViewImpl(UISWTInstance.VIEW_TOPBAR,
									sNames[i], l);
							addTopBarView(view, composite);
						} catch (Exception e) {
							// skip, plugin probably specifically asked to not be added
						}
					}
				}
			}

			if (topbarViews.size() > 0) {
				activeTopBar = (IView) topbarViews.get(0);
				activeTopBar.getComposite().setVisible(true);
			}

			UIUpdaterFactory.getInstance().addUpdater(new UIUpdatable() {
				public void updateUI() {
					Object[] views = topbarViews.toArray();
					for (int i = 0; i < views.length; i++) {
						try {
							IView view = (IView) views[i];
							if (view.getComposite().isVisible()) {
								view.refresh();
							}
						} catch (Exception e) {
							Debug.out(e);
						}
					}
				}

				public String getUpdateUIName() {
					return "TopBar";
				}
			});

			skinObject.getControl().addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					Object[] views = topbarViews.toArray();
					topbarViews.clear();
					for (int i = 0; i < views.length; i++) {
						IView view = (IView) views[i];
						view.delete();
					}
				}
			});

			SWTSkinObject soPrev = skin.getSkinObject("topbar-plugin-prev");
			if (soPrev != null) {
				SWTSkinButtonUtility btnPrev = new SWTSkinButtonUtility(soPrev);
				btnPrev.addSelectionListener(new ButtonListenerAdapter() {
					public void pressed(SWTSkinButtonUtility buttonUtility) {
						//System.out.println("prev click " + activeTopBar + " ; "
						//		+ topbarViews.size());
						if (activeTopBar == null || topbarViews.size() <= 1) {
							return;
						}
						int i = topbarViews.indexOf(activeTopBar) - 1;
						if (i < 0) {
							i = topbarViews.size() - 1;
						}
						activateTopBar((IView) topbarViews.get(i));
					}
				});
			}

			SWTSkinObject soNext = skin.getSkinObject("topbar-plugin-next");
			if (soNext != null) {
				SWTSkinButtonUtility btnNext = new SWTSkinButtonUtility(soNext);
				btnNext.addSelectionListener(new ButtonListenerAdapter() {
					public void pressed(SWTSkinButtonUtility buttonUtility) {
						//System.out.println("next click");
						if (activeTopBar == null || topbarViews.size() <= 1) {
							return;
						}
						int i = topbarViews.indexOf(activeTopBar) + 1;
						if (i >= topbarViews.size()) {
							i = 0;
						}
						activateTopBar((IView) topbarViews.get(i));
					}
				});
			}

			SWTSkinObject soTitle = skin.getSkinObject("topbar-plugin-title");
			if (soTitle != null) {
				final Composite cTitle = (Composite) soTitle.getControl();
				cTitle.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent e) {
						e.gc.setAdvanced(true);
						//Font font = new Font(e.gc.getDevice(), "Sans", 8, SWT.NORMAL);
						//e.gc.setFont(font);
						if (e.gc.getAdvanced() && activeTopBar != null) {
							try {
								e.gc.setTextAntialias(SWT.ON);
							} catch (Exception ex) {
								// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
							}

							try {
								Transform transform = new Transform(e.gc.getDevice());
								transform.rotate(270);
								e.gc.setTransform(transform);

								String s = activeTopBar.getShortTitle();
								Point size = e.gc.textExtent(s);
								e.gc.drawText(s, -size.x, 0, true);
								//e.gc.drawText(s, 0,0, true);
								transform.dispose();
							} catch (Exception ex) {
								// setTransform can trhow a ERROR_NO_GRAPHICS_LIBRARY error
								// no use trying to draw.. it would look weird
							}
							//font.dispose();
						}
					}
				});
			}

			skinObject = skin.getSkinObject(SkinConstants.VIEWID_PLUGINBAR);
			if (skinObject != null) {
				Listener l = new Listener() {
					private int mouseDownAt = 0;

					public void handleEvent(Event event) {
						Composite c = (Composite) event.widget;
						if (event.type == SWT.MouseDown) {
							Rectangle clientArea = c.getClientArea();
							if (event.y > clientArea.height - 10) {
								mouseDownAt = event.y;
							}
						} else if (event.type == SWT.MouseUp && mouseDownAt > 0) {
							int diff = event.y - mouseDownAt;
							mouseDownAt = 0;
							FormData formData = (FormData) c.getLayoutData();
							formData.height += diff;
							if (formData.height < 50) {
								formData.height = 50;
							} else {
								Rectangle clientArea = c.getShell().getClientArea();
								int max = clientArea.height - 350;
								if (formData.height > max) {
									formData.height = max;
								}
							}
							COConfigurationManager.setParameter("v3.topbar.height",
									formData.height);
							Utils.relayout(c);
						} else if (event.type == SWT.MouseMove) {
							Rectangle clientArea = c.getClientArea();
							boolean draggable = (event.y > clientArea.height - 10);
							c.setCursor(draggable ? c.getDisplay().getSystemCursor(
									SWT.CURSOR_SIZENS) : null);
						} else if (event.type == SWT.MouseExit) {
							c.setCursor(null);
						}
					}
				};
				Control control = skinObject.getControl();
				control.addListener(SWT.MouseDown, l);
				control.addListener(SWT.MouseUp, l);
				control.addListener(SWT.MouseMove, l);
				control.addListener(SWT.MouseExit, l);

				skinObject.addListener(new SWTSkinObjectListener() {
					public Object eventOccured(SWTSkinObject skinObject, int eventType,
							Object params) {
						if (eventType == EVENT_SHOW) {
							int h = COConfigurationManager.getIntParameter("v3.topbar.height");
							Control control = skinObject.getControl();
							FormData formData = (FormData) control.getLayoutData();
							formData.height = h;
							control.setLayoutData(formData);
							Utils.relayout(control);
						}
						return null;
					}
				});
			}
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	/**
	 * @param view
	 *
	 * @since 3.0.1.1
	 */
	protected void activateTopBar(IView view) {
		activeTopBar.getComposite().setVisible(false);
		activeTopBar = view;
		activeTopBar.getComposite().setVisible(true);

		SWTSkinObject soTitle = skin.getSkinObject("topbar-plugin-title");
		//System.out.println("Hello" + soTitle);
		if (soTitle != null) {
			soTitle.getControl().redraw();
		}
	}

	/**
	 * @param view
	 *
	 * @since 3.0.1.1
	 */
	private void addTopBarView(IView view, Composite c) {
		view.initialize(c);
		Composite composite = view.getComposite();
		composite.setVisible(false);
		composite.setLayoutData(Utils.getFilledFormData());
		topbarViews.add(view);
	}

	/**
	 * @param skinObject
	 */
	private void attachSearchBox(SWTSkinObject skinObject) {
		Composite cArea = (Composite) skinObject.getControl();

		Text text = null;

		text = new Text(cArea, SWT.NONE);
		FormData filledFormData = Utils.getFilledFormData();
		text.setLayoutData(filledFormData);

		text.addListener(SWT.Resize, new Listener() {
			// @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
			public void handleEvent(Event event) {
				Text text = (Text) event.widget;

				int h = text.getClientArea().height - 2;
				Font font = Utils.getFontWithHeight(text.getFont(), null, h);
				if (font != null) {
					text.setFont(font);
					final Font fFont = font;

					text.addDisposeListener(new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							Text text = (Text) e.widget;
							if (!fFont.isDisposed()) {
								text.setFont(null);
								fFont.dispose();
							}
						}
					});
				}
			}
		});
		text.setTextLimit(254);

		final String sDefault = MessageText.getString("v3.MainWindow.search.defaultText");

		text.setForeground(ColorCache.getColor(text.getDisplay(), 127, 127, 127));
		text.setBackground(ColorCache.getColor(text.getDisplay(), 255, 255, 255));
		text.addMouseListener(new MouseListener() {

			public void mouseUp(MouseEvent e) {
			}

			public void mouseDown(MouseEvent e) {
				Text text = (Text) e.widget;
				if (text.getText().equals(sDefault)) {
					text.setForeground(ColorCache.getColor(text.getDisplay(), 0, 0, 0));
					text.setText("");
				}
			}

			public void mouseDoubleClick(MouseEvent e) {
			}
		});

		text.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}

			public void widgetDefaultSelected(SelectionEvent e) {
				Text text = (Text) e.widget;

				if (e.detail == SWT.CANCEL) {
					text.setText("");
					return;
				}
				// Open a new search result view

				// Search Results are placed in a Search Results tab in the 
				// "Browse Content" tab view. 

				String sSearchText = text.getText();

				doSearch(sSearchText);

			}

		});

		// must be done after layout
		text.setText(sDefault);
		text.selectAll();

		final Text fText = text;
		SWTSkinObject searchGo = skin.getSkinObject("search-go");
		if (searchGo != null) {
			SWTSkinButtonUtility btnGo = new SWTSkinButtonUtility(searchGo);
			btnGo.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					String sSearchText = fText.getText().trim();
					doSearch(sSearchText);
				}
			});
		}
	}

	/**
	 * @param searchText
	 */
	protected void doSearch(String sSearchText) {
		String sDefault = MessageText.getString("v3.MainWindow.search.defaultText");
		if (sSearchText.equals(sDefault) || sSearchText.length() == 0) {
			return;
		}

		// Switch to browse tab
		SearchResultsTabArea.closeSearchResults(Collections.EMPTY_MAP);

		String sURL = Constants.URL_PREFIX + Constants.URL_ADD_SEARCH
				+ UrlUtils.encode(sSearchText) + "&" + Constants.URL_SUFFIX + "&rand="
				+ SystemTime.getCurrentTime();

		if (System.getProperty("metasearch", "1").equals("1")) {
			sURL = Constants.URL_PREFIX + "xsearch?q=" + UrlUtils.encode(sSearchText)
					+ "&" + Constants.URL_SUFFIX + "&rand=" + SystemTime.getCurrentTime();
		}

		System.out.println(sURL);

		UIFunctions functions = UIFunctionsManager.getUIFunctions();
		if (functions != null) {
			functions.viewURL(sURL, SkinConstants.VIEWID_BROWSER_SEARCHRESULTS, 0, 0,
					false, false);
			return;
		}

		// below is the old impementation of search, which creates a new tab
		// and browser for each search.
		/*
		// Get Search Results tab (which contains a tabset of searched terms),
		// create if needed
		SWTSkinObject skinObject = skin.getSkinObject("browse-tabs");
		if (skinObject == null) {
			System.err.println("no browse-tabs");
			return;
		}

		SWTSkinTabSet tabSetSearch = skin.getTabSet(SWTSkinTabSet.getTabSetID(skin,
				skinObject, "search-tab"));

		SWTSkinObjectTab searchTab = null;
		if (tabSetSearch != null) {
			SWTSkinObjectTab[] tabs = tabSetSearch.getTabs();
			if (tabs.length > 0) {
				searchTab = tabs[tabs.length - 1];
			}
		}

		String sTabID = "internal.tab.searchresults";
		if (searchTab == null || !searchTab.getSkinObjectID().equals(sTabID)) {
			// Create search results tab
			searchTab = skin.createTab(sTabID, "search-tab", skinObject);

			// Attach the new tab to the previous one
			Control currentControl = searchTab.getControl();
			FormData formData = (FormData) currentControl.getLayoutData();
			if (formData == null) {
				formData = new FormData();
			}
			formData.right = new FormAttachment(100, 0);
			currentControl.setLayoutData(formData);

			currentControl.getParent().layout(true);

			tabSetSearch = searchTab.getTabset();
		}

		if (tabSetSearch != null) {
			tabSetSearch.setActiveTabByID(sTabID);
		}
		SWTSkinObject searchResultsTabsView = skin.getSkinObject("search-results-tabs");
		if (searchResultsTabsView == null) {
			System.err.println("searchResultsTabs null");
			return;
		}

		sTabID = "internal.tab.searchresults."
				+ Integer.toHexString(sSearchText.hashCode());
		SWTSkinObjectTab tabSearchResult = null;

		String sTabSetID = SWTSkinTabSet.getTabSetID(skin, searchResultsTabsView,
				"tab");
		SWTSkinTabSet tabSetSearchResults = skin.getTabSet(sTabSetID);
		SWTSkinObject lastTab = null;
		if (tabSetSearchResults != null) {
			SWTSkinObject[] tabs = tabSetSearchResults.getTabs();
			if (tabs.length > 0) {
				lastTab = tabs[tabs.length - 1];
			}

			tabSearchResult = tabSetSearchResults.getTab(sTabID);
		}

		if (tabSearchResult == null) {
			// Create tab specifically for this search

			tabSearchResult = skin.createTab(sTabID, "tab", searchResultsTabsView);

			if (tabSetSearchResults == null) {
				tabSetSearchResults = skin.getTabSet(sTabSetID);
			}

			Control currentControl = tabSearchResult.getControl();

			// Attach the new tab to the previous one
			if (lastTab != null) {
				FormData formData = (FormData) currentControl.getLayoutData();
				if (formData == null) {
					formData = new FormData();
				}
				formData.left = new FormAttachment(lastTab.getControl(), 2);
				currentControl.setLayoutData(formData);
			}

			// Set the new tab's text
			SWTSkinObject tabText = skin.getSkinObject("search-result-tab-text",
					tabSearchResult);
			if (tabText instanceof SWTSkinObjectText) {
				((SWTSkinObjectText) tabText).setText(sSearchText);
			}

			searchResultsTabsView.getControl().getParent().layout(true, true);

			String[] activeWidgetIDs = tabSearchResult.getActiveWidgetIDs();
			SWTSkinObject searchResultsView = skin.getSkinObject("search-results-view");
			if (activeWidgetIDs.length == 1 && searchResultsView != null) {
				String sContentConfigID = activeWidgetIDs[0];

				String sContentID = "internal.tab.searchresults.content."
						+ Integer.toHexString(sSearchText.hashCode());
				SWTSkinObject searchResultsContent = skin.createSkinObject(sContentID,
						sContentConfigID, searchResultsView);

				tabSearchResult.setActiveWidgets(new SWTSkinObject[] {
					searchResultsContent
				});

				SWTSkinObject searchResultsContentG = skin.getSkinObject(
						"search-results-google", searchResultsContent);
				if (searchResultsContentG != null) {
					Composite cArea = (Composite) searchResultsContentG.getControl();

					final Browser browser = new Browser(cArea, SWT.NONE);
					final ClientMessageContext context = new BrowserContext("search",
							browser, null, true);
					context.addMessageListener(new TorrentListener(core));
					browser.setLayoutData(Utils.getFilledFormData());
					//					browser.setUrl("http://www.google.com/search?num=5&q="
					//		+ UrlUtils.encode(sSearchText + " torrent"));
					browser.setUrl(sURL);
					cArea.layout(true, true);
				}

				SWTSkinObject searchResultsContentN = skin.getSkinObject(
						"search-results-network", searchResultsContent);
				if (searchResultsContentN != null) {
					Composite cArea = (Composite) searchResultsContentN.getControl();

					final Browser browser = new Browser(cArea, SWT.NONE);
					new BrowserContext("search", browser, null, true);

					browser.setLayoutData(Utils.getFilledFormData());

					NetworkSearch.search(core, sSearchText, browser);

					cArea.layout(true, true);
				}
			}
		}

		// activate!
		if (tabSetSearchResults != null) {
			tabSetSearchResults.setActiveTabByID(sTabID);
		}
		*/
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinTabSetListener#tabChanged(com.aelitis.azureus.ui.swt.skin.SWTSkinTabSet, java.lang.String, java.lang.String)
	public void tabChanged(SWTSkinTabSet tabSet, String oldTabID, String newTabID) {

		MenuFactory.isAZ3_ADV = newTabID.equals("maintabs.advanced");

		boolean isDashboardTab = tabSet.getID().equals(
				SkinConstants.TABSET_DASHBOARD_LEFT);
		boolean isMainTab = tabSet.getID().equals(SkinConstants.TABSET_MAIN);
		if (mapTrackUsage != null) {
			String id = "";
			if (isMainTab) {
				id = oldTabID;
			} else {
				SWTSkinTabSet tabMain = skin.getTabSet(SkinConstants.TABSET_MAIN);
				SWTSkinObjectTab tab = tabMain.getActiveTab();
				if (tab != null) {
					id = tab.getSkinObjectID();
				}
			}
			if (id.length() > 9) {
				id = id.substring(9);
			}
			String id2 = "";
			if (isDashboardTab) {
				id2 = oldTabID;
			} else if (oldTabID.equals("maintabs.home")) {
				SWTSkinTabSet tabSetLeft = skin.getTabSet(SkinConstants.TABSET_DASHBOARD_LEFT);
				if (tabSetLeft != null && tabSetLeft.getActiveTab() != null) {
					id2 = tabSetLeft.getActiveTab().getSkinObjectID();
				}
			}
			if (id2.length() > 8) {
				id2 = "-" + id2.substring(8);
			}

			updateMapTrackUsage(id + id2);
		}

		if (isMainTab) {
			// TODO: Don't use internal skin IDs.  Skin needs to provide an ViewID
			//        we can query (or is passed in)
			if (newTabID.equals("maintabs.advanced")) {
				createOldMainWindow();
			} else if (newTabID.equals("maintabs.home")
					&& oldTabID.equals("maintabs.home")) {

				SkinView view = SkinViewManager.get(MiniBrowse.class);
				if (view instanceof MiniBrowse) {
					((MiniBrowse) view).restart();
				}
			} else if (newTabID.equals("maintabs.browse")
					&& oldTabID.equals("maintabs.browse")) {

				SkinView view = SkinViewManager.get(Browse.class);
				if (view instanceof Browse) {
					((Browse) view).restart();
				}
			} else if (newTabID.equals("maintabs.publish")
					&& oldTabID.equals("maintabs.publish")) {

				SkinView view = SkinViewManager.get(Publish.class);
				if (view instanceof Publish) {
					((Publish) view).restart();
				}
			}

			if (newTabID.equals("maintabs.home")) {
				SWTSkinTabSet tabSetLeft = skin.getTabSet(SkinConstants.TABSET_DASHBOARD_LEFT);
				if (tabSetLeft != null && tabSetLeft.getActiveTab() == null) {
					String startTab = COConfigurationManager.getStringParameter("v3.home-tab.starttab");

					if (!tabSetLeft.setActiveTab(startTab)) {
						tabSetLeft.setActiveTab(SkinConstants.VIEWID_ACTIVITY_TAB);
					}
				}
			}

			/*
			 * Updates the enablement states when ever a tab is selected
			 */
			MenuFactory.updateEnabledStates(menu.getMenu(IMenuConstants.MENU_ID_MENU_BAR));
		} else if (isDashboardTab) {
			String newTabViewID = tabSet.getActiveTab().getViewID();
			if (newTabViewID.equals("tab-activities")) {
				skin.getSkinObjectByID("main.area.events").setVisible(true);
			} else {
				skin.getSkinObjectByID("main.area.minilibrary").setVisible(true);
			}
			COConfigurationManager.setParameter("v3.home-tab.starttab", newTabViewID);
		}
	}

	/**
	 * 
	 */
	private void updateMapTrackUsage(String sTabID) {
		if (mapTrackUsage != null) {
			mapTrackUsage_mon.enter();
			try {
				if (lCurrentTrackTime > 0) {
					Long currentLength = (Long) mapTrackUsage.get(sTabID);
					long newLength;
					if (currentLength == null) {
						newLength = lCurrentTrackTime;
					} else {
						newLength = currentLength.longValue() + lCurrentTrackTime;
					}
					if (newLength > 1000) {
						mapTrackUsage.put(sTabID, new Long(newLength / 1000));
					}
				}

				if (lCurrentTrackTimeIdle > 0) {
					String id = "idle-" + sTabID;
					Long currentLengthIdle = (Long) mapTrackUsage.get(id);
					long newLengthIdle = currentLengthIdle == null
							? lCurrentTrackTimeIdle : currentLengthIdle.longValue()
									+ lCurrentTrackTimeIdle;
					if (newLengthIdle > 1000) {
						mapTrackUsage.put(id, new Long(newLengthIdle / 1000));
					}
				}
			} finally {
				mapTrackUsage_mon.exit();
			}
		}

		lCurrentTrackTime = 0;
		lCurrentTrackTimeIdle = 0;
	}

	/**
	 * 
	 */
	private org.gudy.azureus2.ui.swt.mainwindow.MainWindow createOldMainWindow() {
		if (oldMainWindow != null || disposedOrDisposing) {
			return oldMainWindow;
		}

		if (uiSWTInstanceImpl == null) {
			System.out.println("This will end only in disaster! "
					+ Debug.getCompressedStackTrace());
		}

		return (org.gudy.azureus2.ui.swt.mainwindow.MainWindow) Utils.execSWTThreadWithObject(
				"createOldMainWindow", new AERunnableObject() {

					public Object runSupport() {

						SWTSkinObject skinObject = skin.getSkinObject("advanced");
						if (skinObject != null) {
							Composite cArea = (Composite) skinObject.getControl();

							Label lblWait = new Label(cArea, SWT.CENTER);
							FormData formData = new FormData();
							formData.left = new FormAttachment(0, 0);
							formData.right = new FormAttachment(100, 0);
							formData.top = new FormAttachment(0, 0);
							formData.bottom = new FormAttachment(100, 0);
							lblWait.setLayoutData(formData);
							lblWait.setForeground(skinObject.getProperties().getColor(
									"color.row.fg"));
							Messages.setLanguageText(lblWait, "v3.MainWindow.view.wait");
							cArea.layout(true);
							lblWait.update();

							Color c = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
							if (Constants.isUnix) {
								// Hack: For some reason, if we set the color of a Composite
								// to the widget background color, it will use the color
								// of the parent composite, even when backgroundmode is
								// INHERIT_NONE
								// The hack fix is to not use the exact color :(
								if (c.getRed() > 0) {
									c = ColorCache.getColor(display, c.getRed() - 1,
											c.getGreen(), c.getBlue());
								} else {
									c = ColorCache.getColor(display, c.getRed() + 1,
											c.getGreen(), c.getBlue());
								}
							}
							cArea.setBackground(c);

							oldMainWindow = new org.gudy.azureus2.ui.swt.mainwindow.MainWindow(
									core, null, cArea.getShell(), cArea, uiSWTInstanceImpl);
							oldMainWindow.setShowMainWindow(false);
							oldMainWindow.runSupport();
							if (isReady) {
								oldMainWindow.postPluginSetup(-1, 0);
							}

							/*
							 * KN: A hack to pass the old main menu to the old main window;
							 * this whole old/new main window/menu must be redesigned to be more flexible :-(
							 */
							//							IMainMenu menu = uiFunctions.createMainMenu(cArea.getShell());
							oldMainWindow.setMainMenu(menu);

							uiFunctions.oldMainWindowInitialized(oldMainWindow);

							lblWait.dispose();
							cArea.layout(true);
						}
						return oldMainWindow;
					}

				}, 0);
	}

	public org.gudy.azureus2.ui.swt.mainwindow.MainWindow getOldMainWindow(
			boolean bForceCreate) {
		if (oldMainWindow == null && bForceCreate) {
			createOldMainWindow();
		}
		return oldMainWindow;
	}

	public UIFunctionsSWT getOldUIFunctions(boolean bCreateOld) {
		if (oldMainWindow == null && bCreateOld) {
			createOldMainWindow();
		}
		if (oldMainWindow != null) {
			return oldMainWindow.getUIFunctions();
		}
		return null;
	}

	private String getActiveTab() {
		SWTSkinTabSet tabSetMain = skin.getTabSet(SkinConstants.TABSET_MAIN);
		if (tabSetMain == null) {
			return "";
		}
		SWTSkinObjectTab activeTab = tabSetMain.getActiveTab();
		if (activeTab == null) {
			return "";
		}
		return activeTab.getViewID();
	}

	public void switchToAdvancedTab() {
		SWTSkinTabSet tabSetMain = skin.getTabSet(SkinConstants.TABSET_MAIN);
		if (tabSetMain == null) {
			System.err.println(SkinConstants.TABSET_MAIN);
			return;
		}

		tabSetMain.setActiveTab(SkinConstants.VIEWID_ADVANCED_TAB);
	}

	public UISWTInstance getUISWTInstanceImpl() {
		return uiSWTInstanceImpl;
	}

	/**
	 * @param url
	 * @param target
	 */
	public void showURL(String url, String target) {

		if (url.startsWith("AZMSG%3B") && false) {
			try {
				BrowserMessage browserMsg;
				browserMsg = new BrowserMessage(URLDecoder.decode(url, "utf-8"));
				ClientMessageContext context = PlatformMessenger.getClientMessageContext();
				BrowserMessageDispatcher dispatcher = context.getDispatcher();
				if (dispatcher != null) {
					dispatcher.dispatch(browserMsg);
					dispatcher.resetSequence();
				} else {
					browserMsg.debug("no dispatcher for showURL action");
				}
			} catch (UnsupportedEncodingException e) {
			}
			return;
		}

		SWTSkinObject skinObject = skin.getSkinObject("tab-" + target);

		if (skinObject == null) {
			skinObject = skin.getSkinObject(target);
		}

		if (skinObject == null) {
			return;
		}

		setVisible(true);
		skin.activateTab(skinObject);

		skinObject = skin.getSkinObject(target);

		if (skinObject == null && target.startsWith("tab-")) {
			skinObject = skin.getSkinObject(target.substring(4));
		}

		if (skinObject instanceof SWTSkinObjectBrowser) {
			((SWTSkinObjectBrowser) skinObject).getBrowser().setVisible(false);
			if (url == null || url.length() == 0) {
				((SWTSkinObjectBrowser) skinObject).restart();
			} else {
				((SWTSkinObjectBrowser) skinObject).setURL(url);
			}
		}
	}

	protected MainStatusBar getMainStatusBar() {
		return statusBar;
	}

	public boolean isVisible(int windowElement) {
		if (windowElement == IMainWindow.WINDOW_ELEMENT_TOOLBAR) {
			/*
			 * Only the (embedded) old main window has a toolbar which is available only in Vuze Advanced
			 */
			if (null != oldMainWindow) {
				return oldMainWindow.isVisible(windowElement);
			}
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_SEARCHBAR) {
			SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_PLUGINBAR);
			if (skinObject != null) {
				return skinObject.isVisible();
			}
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_TABBAR) {
			SWTSkinObject skinObject = skin.getSkinObject("tabbar");
			if (skinObject != null) {
				return skinObject.isVisible();
			}
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_STATUSBAR) {
			//TODO:
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_MENU) {
			//TODO:
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_FOOTER) {
			SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_FOOTER);
			if (skinObject != null) {
				return skinObject.isVisible();
			}
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_BUTTON_BAR) {
			SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_BUTTON_BAR);
			if (skinObject != null) {
				return skinObject.isVisible();
			}
		}

		return false;
	}

	public void setVisible(int windowElement, boolean value) {
		if (windowElement == IMainWindow.WINDOW_ELEMENT_TOOLBAR) {
			if (null != oldMainWindow) {
				/*
				 * Only the (embedded) old main window has a toolbar which is available only in Vuze Advanced
				 */
				oldMainWindow.setVisible(windowElement, value);
			}
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_SEARCHBAR) {

			SWTSkinUtils.setVisibility(skin, SkinConstants.VIEWID_PLUGINBAR
					+ ".visible", SkinConstants.VIEWID_PLUGINBAR, value, true, true);

		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_STATUSBAR) {
			//TODO:
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_MENU) {
			//TODO:
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_FOOTER) {

			SWTSkinUtils.setVisibility(skin, "Footer.visible",
					SkinConstants.VIEWID_FOOTER, value, true, true);

		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_BUTTON_BAR) {
			// We don't allow the button bar to ever be hidden
		}

	}

	public Rectangle getMetrics(int windowElement) {
		if (windowElement == IMainWindow.WINDOW_ELEMENT_TOOLBAR) {
			if (null != oldMainWindow) {
				/*
				 * Only the (embedded) old main window has a toolbar which is available only in Vuze Advanced
				 */
				return oldMainWindow.getMetrics(windowElement);
			}
		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_SEARCHBAR) {

			SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_PLUGINBAR);
			if (skinObject != null) {
				return skinObject.getControl().getBounds();
			}

		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_TABBAR) {

			SWTSkinObject skinObject = skin.getSkinObject("tabbar");
			if (skinObject != null) {
				return skinObject.getControl().getBounds();
			}

		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_STATUSBAR) {

			return statusBar.getBounds();

		} else if (windowElement == IMainWindow.WINDOW_CLIENT_AREA) {

			return shell.getClientArea();

		} else if (windowElement == IMainWindow.WINDOW_CONTENT_DISPLAY_AREA) {

			Rectangle r = getMetrics(IMainWindow.WINDOW_CLIENT_AREA);
			r.height -= getMetrics(IMainWindow.WINDOW_ELEMENT_SEARCHBAR).height;
			r.height -= getMetrics(IMainWindow.WINDOW_ELEMENT_TABBAR).height;
			r.height -= getMetrics(IMainWindow.WINDOW_ELEMENT_STATUSBAR).height;
			r.height -= getMetrics(IMainWindow.WINDOW_ELEMENT_FOOTER).height;
			r.height -= getMetrics(IMainWindow.WINDOW_ELEMENT_BUTTON_BAR).height;
			return r;

		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_BUTTON_BAR) {

			SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_BUTTON_BAR);
			if (skinObject != null) {
				return skinObject.getControl().getBounds();
			}

		} else if (windowElement == IMainWindow.WINDOW_ELEMENT_FOOTER) {

			SWTSkinObject skinObject = skin.getSkinObject(SkinConstants.VIEWID_FOOTER);
			if (skinObject != null) {
				return skinObject.getControl().getBounds();
			}

		}

		return new Rectangle(0, 0, 0, 0);
	}

	public SWTSkin getSkin() {
		return skin;
	}

	public boolean isReady() {
		return isReady;
	}

	public Image generateObfusticatedImage() {
		if (getActiveTab().equals(SkinConstants.VIEWID_ADVANCED_TAB)
				&& oldMainWindow != null) {
			return oldMainWindow.generateObfusticatedImage();
		}

		Image image;
		Rectangle clientArea = shell.getClientArea();
		image = new Image(display, clientArea.width, clientArea.height);

		GC gc = new GC(shell);
		try {
			gc.copyArea(image, clientArea.x, clientArea.y);
		} finally {
			gc.dispose();
		}
		return image;
	}
}