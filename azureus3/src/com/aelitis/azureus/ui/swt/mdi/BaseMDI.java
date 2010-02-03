package com.aelitis.azureus.ui.swt.mdi;

import java.util.*;

import org.eclipse.swt.SWT;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.UIPluginView;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.IViewInfo;
import org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.PluginAddedViewListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.IView;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.mdi.*;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkinObject;
import com.aelitis.azureus.ui.swt.views.skin.SkinView;
import com.aelitis.azureus.util.ConstantsVuze;
import com.aelitis.azureus.util.ContentNetworkUtils;
import com.aelitis.azureus.util.MapUtils;

public abstract class BaseMDI
	extends SkinView
	implements MultipleDocumentInterfaceSWT, UIUpdatable
{
	public static String SIDEBAR_SECTION_BROWSE = "ContentNetwork.1";

	static {
		SIDEBAR_SECTION_BROWSE = ContentNetworkUtils.getTarget(ConstantsVuze.getDefaultContentNetwork());
	}

	protected MdiEntrySWT currentEntry;

	protected Map<String, MdiEntryCreationListener> mapIdToCreationListener = new LightHashMap<String, MdiEntryCreationListener>();

	// Sync changes to entry maps on mapIdEntry
	protected Map<String, MdiEntrySWT> mapIdToEntry = new LightHashMap<String, MdiEntrySWT>();

	private List<MdiListener> listeners = new ArrayList<MdiListener>();

	private static Map<String, Object> mapAutoOpen = new LightHashMap<String, Object>();

	public void addListener(MdiListener l) {
		synchronized (listeners) {
			if (listeners.contains(l)) {
				return;
			}
			listeners.add(l);
		}
	}

	public void removeListener(MdiListener l) {
		synchronized (listeners) {
			listeners.remove(l);
		}
	}

	protected void triggerSelectionListener(MdiEntry newEntry, MdiEntry oldEntry) {
		MdiListener[] array = listeners.toArray(new MdiListener[0]);
		for (MdiListener l : array) {
			l.mdiEntrySelected(newEntry, oldEntry);
		}
	}

	public void closeEntry(final String id) {
		MdiEntry entry = getEntry(id);
		if (entry != null) {
			entry.close(false);
		}
	}

	public abstract MdiEntry createEntryFromEventListener(String parentID,
			UISWTViewEventListener l, String id, boolean closeable, Object datasource);

	public abstract MdiEntry createEntryFromIView(String parentID, IView iview,
			String id, Object datasource, boolean closeable, boolean show,
			boolean expand);

	public abstract MdiEntry createEntryFromIViewClass(String parent, String id,
			String title, Class<?> iviewClass, Class<?>[] iviewClassArgs,
			Object[] iviewClassVals, Object datasource, ViewTitleInfo titleInfo,
			boolean closeable);

	public abstract MdiEntry createEntryFromSkinRef(String parentID, String id,
			String configID, String title, ViewTitleInfo titleInfo, Object params,
			boolean closeable, int index);

	public MdiEntry getCurrentEntry() {
		return currentEntry;
	}

	public MdiEntrySWT getCurrentEntrySWT() {
		return currentEntry;
	}

	public MdiEntry[] getEntries() {
		return mapIdToEntry.values().toArray(new MdiEntry[0]);
	}

	public MdiEntrySWT[] getEntriesSWT() {
		return mapIdToEntry.values().toArray(new MdiEntrySWT[0]);
	}

	public MdiEntry getEntry(String id) {
		if ("Browse".equalsIgnoreCase(id)) {
			id = SIDEBAR_SECTION_BROWSE;
		}
		MdiEntry entry = mapIdToEntry.get(id);
		return entry;
	}

	public MdiEntrySWT getEntrySWT(String id) {
		if ("Browse".equalsIgnoreCase(id)) {
			id = SIDEBAR_SECTION_BROWSE;
		}
		MdiEntrySWT entry = mapIdToEntry.get(id);
		return entry;
	}

	/**
	 * @param skinView
	 * @return 
	 *
	 * @since 3.1.1.1
	 */
	public MdiEntry getEntryBySkinView(Object skinView) {
		SWTSkinObject so = ((SkinView)skinView).getMainSkinObject();
		Object[] sideBarEntries = mapIdToEntry.values().toArray();
		for (int i = 0; i < sideBarEntries.length; i++) {
			//MdiEntrySWT entry = (MdiEntrySWT) sideBarEntries[i];
			BaseMdiEntry entry = (BaseMdiEntry) sideBarEntries[i];
			SWTSkinObject entrySO = entry.getSkinObject();
			SWTSkinObject entrySOParent = entrySO == null ? entrySO
					: entrySO.getParent();
			if (entrySO == so || entrySO == so.getParent() || entrySOParent == so) {
				return entry;
			}
		}
		return null;
	}

	public IView getIViewFromID(String id) {
		if (id == null) {
			return null;
		}
		MdiEntrySWT entry = getEntrySWT(id);
		if (entry == null) {
			return null;
		}
		return entry.getIView();
	}

	public String getUpdateUIName() {
		if (currentEntry == null || currentEntry.getIView() == null) {
			return "MDI";
		}
		if (currentEntry.getIView() instanceof UIPluginView) {
			UIPluginView uiPluginView = (UIPluginView) currentEntry.getIView();
			return uiPluginView.getViewID();
		}

		return currentEntry.getIView().getFullTitle();
	}

	public void registerEntry(String id, MdiEntryCreationListener l) {
		mapIdToCreationListener.put(id, l);

		Object o = mapAutoOpen.get(id);
		if (o instanceof Map<?, ?>) {
			MdiEntryCreationListener mdiEntryCreationListener = mapIdToCreationListener.get(id);
			if (mdiEntryCreationListener != null) {
				try {
					mdiEntryCreationListener.createMDiEntry(id);
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		}
	}

	public abstract boolean showEntryByID(String id);

	@Override
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		return null;
	}

	public void updateUI() {
		MdiEntry currentEntry = getCurrentEntry();
		if (currentEntry != null) {
			currentEntry.updateUI();
		}
	}

	public boolean entryExists(String id) {
		if ("Browse".equalsIgnoreCase(id)) {
			id = SIDEBAR_SECTION_BROWSE;
		}
		MdiEntry entry = mapIdToEntry.get(id);
		if (entry == null) {
			return false;
		}
		return entry.isAdded();
	}

	protected MdiEntry createWelcomeSection() {
		MdiEntry entry = createEntryFromSkinRef(null, SIDEBAR_SECTION_WELCOME,
				"main.area.welcome", MessageText.getString(
						"v3.MainWindow.menu.getting_started").replaceAll("&", ""), null,
				null, true, 0);
		entry.setImageLeftID("image.sidebar.welcome");
		addDropTest(entry);
		return entry;
	}

	protected void addDropTest(MdiEntry entry) {
		if (!Constants.isCVSVersion()) {
			return;
		}
		entry.addListener(new MdiEntryDropListener() {
			public boolean mdiEntryDrop(MdiEntry entry, Object droppedObject) {
				String s = "You just dropped " + droppedObject.getClass() + "\n"
						+ droppedObject + "\n\n";
				if (droppedObject.getClass().isArray()) {
					Object[] o = (Object[]) droppedObject;
					for (int i = 0; i < o.length; i++) {
						s += "" + i + ":  ";
						Object object = o[i];
						if (object == null) {
							s += "null";
						} else {
							s += object.getClass() + ";" + object;
						}
						s += "\n";
					}
				}
				new MessageBoxShell(SWT.OK, "test", s).open(null);
				return true;
			}
		});
	}

	public void setEntryAutoOpen(String id, boolean autoOpen) {
		if (!autoOpen) {
			mapAutoOpen.remove(id);
		} else {
			mapAutoOpen.put(id, new LightHashMap(0));
		}
	}

	protected void setupPluginViews() {
		UISWTInstanceImpl uiSWTInstance = (UISWTInstanceImpl) UIFunctionsManagerSWT.getUIFunctionsSWT().getUISWTInstance();
		if (uiSWTInstance != null) {
			Map<String, Map<String, UISWTViewEventListener>> allViews = uiSWTInstance.getAllViews();
			Object[] parentIDs = allViews.keySet().toArray();
			for (int i = 0; i < parentIDs.length; i++) {
				String parentID = (String) parentIDs[i];
				Map<String, UISWTViewEventListener> mapSubViews = allViews.get(parentID);
				if (mapSubViews != null) {
					Object[] viewIDs = mapSubViews.keySet().toArray();
					for (int j = 0; j < viewIDs.length; j++) {
						String viewID = (String) viewIDs[j];
						UISWTViewEventListener l = (UISWTViewEventListener) mapSubViews.get(viewID);
						if (l != null) {
							// TODO: Datasource
							// TODO: Multiple open

							boolean open = COConfigurationManager.getBooleanParameter(
									"SideBar.AutoOpen." + viewID, false);
							if (open) {
								createEntryFromEventListener(parentID, l, viewID, true, null);
							}
						}
					}
				}
			}
		}

		// When a new Plugin View is added, check out auto-open list to see if
		// the user had it open
		PluginsMenuHelper.getInstance().addPluginAddedViewListener(
				new PluginAddedViewListener() {
					// @see org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.PluginAddedViewListener#pluginViewAdded(org.gudy.azureus2.ui.swt.mainwindow.PluginsMenuHelper.IViewInfo)
					public void pluginViewAdded(IViewInfo viewInfo) {
						//System.out.println("PluginView Added: " + viewInfo.viewID);
						Object o = mapAutoOpen.get(viewInfo.viewID);
						if (o instanceof Map<?, ?>) {
							processAutoOpenMap(viewInfo.viewID, (Map<?, ?>) o, viewInfo);
						}
					}
				});
	}

	public void informAutoOpenSet(MdiEntry entry, Map<String, Object> autoOpenInfo) {
		mapAutoOpen.put(entry.getId(), autoOpenInfo);
	}

	public void loadCloseables() {
		Map<?,?> loadedMap = FileUtil.readResilientConfigFile("sidebarauto.config", true);
		if (loadedMap.isEmpty()) {
			return;
		}
		BDecoder.decodeStrings(loadedMap);
		for (Iterator<?> iter = loadedMap.keySet().iterator(); iter.hasNext();) {
			String id = (String) iter.next();
			Object o = loadedMap.get(id);

			if (o instanceof Map<?, ?>) {
				if (!processAutoOpenMap(id, (Map<?, ?>) o, null)) {
					mapAutoOpen.put(id, o);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void saveCloseables() {
		// update title
		for (Iterator<?> iter = mapAutoOpen.keySet().iterator(); iter.hasNext();) {
			String id = (String) iter.next();
			Object o = mapAutoOpen.get(id);

			MdiEntry entry = getEntry(id);
			if (entry != null && entry.isAdded() && (o instanceof Map)) {
				Map autoOpenInfo = (Map) o;

				String s = entry.getTitle();
				if (s != null) {
					autoOpenInfo.put("title", s);
				}
			}
		}

		FileUtil.writeResilientConfigFile("sidebarauto.config", mapAutoOpen);
	}

	private boolean processAutoOpenMap(String id, Map<?, ?> autoOpenInfo,
			IViewInfo viewInfo) {
		try {
			MdiEntry entry = getEntry(id);
			if (entry != null) {
				return true;
			}

			if (id.equals(SIDEBAR_SECTION_WELCOME)) {
				createWelcomeSection();
			}

			String title = MapUtils.getMapString(autoOpenInfo, "title", id);
			String parentID = (String) autoOpenInfo.get("parentID");
			Object datasource = autoOpenInfo.get("datasource");

			if (viewInfo != null) {
				if (viewInfo.view != null) {
					entry = createEntryFromIView(parentID, viewInfo.view, id, datasource,
							true, false, true);
				} else if (viewInfo.event_listener != null) {
					entry = createEntryFromEventListener(parentID,
							viewInfo.event_listener, id, true, datasource);
				}
			}

			Class<?> cla = Class.forName(MapUtils.getMapString(autoOpenInfo,
					"iviewClass", ""));
			if (cla != null) {
				String dmHash = MapUtils.getMapString(autoOpenInfo, "dm", null);
				if (dmHash != null) {
					HashWrapper hw = new HashWrapper(Base32.decode(dmHash));
					GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
					DownloadManager dm = gm.getDownloadManager(hw);
					if (dm != null) {
						datasource = dm;
					}
					// XXX Skip auto open DM for now
					return false;
				}
				entry = createEntryFromIViewClass(parentID, id, title, cla, null, null,
						datasource, null, true);
			}
		} catch (ClassNotFoundException ce) {
			// ignore
		} catch (Throwable e) {
			Debug.out(e);
		}
		return true;
	}

	public void removeItem(MdiEntry entry) {
		String id = entry.getId();
		synchronized (mapIdToEntry) {
			mapIdToEntry.remove(id);
		}
	}

	public Object updateLanguage(SWTSkinObject skinObject, Object params) {
  	MdiEntrySWT[] entries = getEntriesSWT();
  	for (MdiEntrySWT entry : entries) {
			if (entry == null) {
				continue;
			}
			IView view = entry.getIView();
			if (view != null) {
			  try {
          view.updateLanguage();
          view.refresh();
        }
        catch (Exception e) {
        	Debug.printStackTrace(e);
        }
			}
		}
    
		return null;
	}
}
