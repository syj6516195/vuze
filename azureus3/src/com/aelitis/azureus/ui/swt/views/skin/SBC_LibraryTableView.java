/**
 * Created on Jul 3, 2008
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

package com.aelitis.azureus.ui.swt.views.skin;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import org.gudy.azureus2.plugins.ui.tables.TableRowRefreshListener;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.*;
import org.gudy.azureus2.ui.swt.views.table.TableRowSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.ToolBarEnabler;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionAdapter;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.selectedcontent.*;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.columns.utils.TableColumnCreatorV3;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.utils.TorrentUIUtilsV3;
import com.aelitis.azureus.util.DLReferals;
import com.aelitis.azureus.util.DataSourceUtils;
import com.aelitis.azureus.util.PlayUtils;

/**
 * Classic My Torrents view wrapped in a SkinView
 * 
 * @author TuxPaper
 * @created Jul 3, 2008
 *
 */
public class SBC_LibraryTableView
	extends SkinView
	implements UIUpdatable, ToolBarEnabler, ObfusticateImage
{
	private final static String ID = "SBC_LibraryTableView";

	private IView view;

	private Composite viewComposite;
	
	private TableViewSWT tv;

	protected int torrentFilterMode = SBC_LibraryView.TORRENTS_ALL;

	private SWTSkinObject soParent;
	
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		soParent = skinObject.getParent();
		
  	AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(final AzureusCore core) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						if (soParent == null || soParent.isDisposed()) {
							return;
						}
						initShow(core);
					}
				});
			}
  	});

		return null;
	}

	public void initShow(AzureusCore core) {
		Object data = soParent.getControl().getData("TorrentFilterMode");
		if (data instanceof Long) {
			torrentFilterMode = (int) ((Long) data).longValue();
		}
		
		data = soParent.getControl().getData("DataSource");
		
		boolean useBigTable = useBigTable();
		
		SWTSkinObjectTextbox soFilter = (SWTSkinObjectTextbox) skin.getSkinObject(
				"library-filter", soParent.getParent());
		Text txtFilter = soFilter == null ? null : soFilter.getTextControl();
		
		SWTSkinObjectContainer soCats = (SWTSkinObjectContainer) skin.getSkinObject(
				"library-categories", soParent.getParent());
		Composite cCats = soCats == null ? null : soCats.getComposite();

		// columns not needed for small mode, all torrents
		TableColumnCore[] columns = useBigTable
				|| torrentFilterMode != SBC_LibraryView.TORRENTS_ALL ? getColumns()
				: null;

		if (null != columns) {
			TableColumnManager tcManager = TableColumnManager.getInstance();
			tcManager.addColumns(columns);
		}

		if (useBigTable) {
			if (torrentFilterMode == SBC_LibraryView.TORRENTS_COMPLETE
					|| torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE
					|| torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {

				view = new MyTorrentsView_Big(core, torrentFilterMode, columns,
						txtFilter, cCats);

			} else {
				//view = new MyTorrentsSuperView_Big();
				view = new MyTorrentsView_Big(core, torrentFilterMode, columns,
						txtFilter, cCats);
			}

		} else {
			String tableID = SB_Transfers.getTableIdFromFilterMode(
					torrentFilterMode, false);
			if (torrentFilterMode == SBC_LibraryView.TORRENTS_COMPLETE) {
				view = new MyTorrentsView(core, tableID, true, columns, txtFilter,
						cCats);

			} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE) {
				view = new MyTorrentsView(core, tableID, false, columns, txtFilter,
						cCats);

			} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {
				view = new MyTorrentsView(core, tableID, true, columns, txtFilter,
						cCats) {
					public boolean isOurDownloadManager(DownloadManager dm) {
						if (PlatformTorrentUtils.getHasBeenOpened(dm)) {
							return false;
						}
						return super.isOurDownloadManager(dm);
					}
				};
			} else {
				view = new MyTorrentsSuperView(txtFilter, cCats) {
					public void initializeDone() {
						MyTorrentsView seedingview = getSeedingview();
						if (seedingview != null) {
							seedingview.overrideDefaultSelected(new TableSelectionAdapter() {
								public void defaultSelected(TableRowCore[] rows, int stateMask) {
									doDefaultClick(rows, stateMask, false);
								}
							});
							MyTorrentsView torrentview = getTorrentview();
							if (torrentview != null) {
								torrentview.overrideDefaultSelected(new TableSelectionAdapter() {
									public void defaultSelected(TableRowCore[] rows, int stateMask) {
										doDefaultClick(rows, stateMask, false);
									}
								});
							}
						}
					}
				};
			}
			
			if (view instanceof MyTorrentsView) {
				((MyTorrentsView) view).overrideDefaultSelected(new TableSelectionAdapter() {
					public void defaultSelected(TableRowCore[] rows, int stateMask) {
						doDefaultClick(rows, stateMask, false);
					}
				});
			}
		}

		if (data != null) {
			view.dataSourceChanged(data);
		}

		SWTSkinObjectContainer soContents = new SWTSkinObjectContainer(skin,
				skin.getSkinProperties(), getUpdateUIName(), "", soMain);

		skin.layout();

		viewComposite = soContents.getComposite();
//		viewComposite.setBackground(viewComposite.getDisplay().getSystemColor(
//				SWT.COLOR_WIDGET_BACKGROUND));
//		viewComposite.setForeground(viewComposite.getDisplay().getSystemColor(
//				SWT.COLOR_WIDGET_FOREGROUND));
		viewComposite.setLayoutData(Utils.getFilledFormData());
		GridLayout gridLayout = new GridLayout();
		gridLayout.horizontalSpacing = gridLayout.verticalSpacing = gridLayout.marginHeight = gridLayout.marginWidth = 0;
		viewComposite.setLayout(gridLayout);

		view.initialize(viewComposite);


		if (tv == null) {
			if (view instanceof TableViewTab) {
				TableViewTab tvt = (TableViewTab) view;
				tv = tvt.getTableView();
			} else if (view instanceof TableViewSWT) {
				tv = (TableViewSWT) view;
			}
		}
		
		SWTSkinObject soSizeSlider = skin.getSkinObject("table-size-slider", soParent.getParent());
		if (soSizeSlider instanceof SWTSkinObjectContainer) {
			SWTSkinObjectContainer so = (SWTSkinObjectContainer) soSizeSlider;
			if (tv != null && !tv.enableSizeSlider(so.getComposite(), 16, 100)) {
				so.setVisible(false);
			}
		}

		
		if (torrentFilterMode == SBC_LibraryView.TORRENTS_ALL
				&& tv != null) {
			tv.addRefreshListener(new TableRowRefreshListener() {
				public void rowRefresh(TableRow row) {
					TableRowSWT rowCore = (TableRowSWT)row;
					Object ds = rowCore.getDataSource(true);
					if (!(ds instanceof DownloadManager)) {
						return;
					}
					DownloadManager dm = (DownloadManager) ds;
					boolean changed = false;
					boolean assumedComplete = dm.getAssumedComplete();
					if (!assumedComplete) {
						changed |= rowCore.setAlpha(160);
					} else if (!PlatformTorrentUtils.getHasBeenOpened(dm)) {
						changed |= rowCore.setAlpha(255);
					} else {
						changed |= rowCore.setAlpha(255);
					}
				}
			});
		}
		
		if (tv != null) {
			tv.addKeyListener(new KeyListener() {

				public void keyReleased(KeyEvent e) {
				}

				public void keyPressed(KeyEvent e) {
					if (e.character == 15 && e.stateMask == (SWT.SHIFT | SWT.CONTROL)) {
						Object[] selectedDataSources = tv.getSelectedDataSources().toArray();
						for (int i = 0; i < selectedDataSources.length; i++) {
							DownloadManager dm = (DownloadManager) selectedDataSources[i];
							if (dm != null) {
								TOTorrent torrent = dm.getTorrent();
								String contentHash = PlatformTorrentUtils.getContentHash(torrent);
								if (contentHash != null && contentHash.length() > 0) {
									ContentNetwork cn = DataSourceUtils.getContentNetwork(torrent);
									if (cn == null) {
										new MessageBoxShell(SWT.OK, "coq",
												"Not in Content Network List").open(null);
										return;
									}
									String url = cn.getTorrentDownloadService(contentHash, "coq");
									DownloadUrlInfo dlInfo = new DownloadUrlInfoContentNetwork(
											url, cn);
									TorrentUIUtilsV3.loadTorrent(dlInfo, false, false, true);
								}

							}
						}
					}
				}
			});
		}

		if (torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {
			SWTSkinObject so = skin.getSkinObject("library-list-button-right",
					soParent.getParent());
			if (so != null) {
				so.setVisible(true);
				SWTSkinButtonUtility btn = new SWTSkinButtonUtility(so);
				btn.setTextID("Mark All UnNew");
				btn.addSelectionListener(new SWTSkinButtonUtility.ButtonListenerAdapter() {
					public void pressed(SWTSkinButtonUtility buttonUtility,
							SWTSkinObject skinObject, int stateMask) {
						TableViewSWT tv = ((MyTorrentsView) view).getTableView();
						Object[] dataSources = tv.getDataSources().toArray();
						for (int i = 0; i < dataSources.length; i++) {
							Object ds = dataSources[i];
							if (ds instanceof DownloadManager) {
								PlatformTorrentUtils.setHasBeenOpened((DownloadManager) ds,
										true);
								// give user visual indication right away 
								tv.removeDataSource(ds);
							}
						}
					}
				});
			}
		}
		viewComposite.getParent().layout(true);
	}

	public static void 
	doDefaultClick(
		final TableRowCore[] 	rows, 
		final int 				stateMask,
		final boolean 			neverPlay) 
	{
		if (rows == null || rows.length != 1) {
			return;
		}
		
		final Object ds = rows[0].getDataSource(true);

		String mode = COConfigurationManager.getStringParameter("list.dm.dblclick");
		if (mode.equals("1")) {
			// OMG! Show Details! I <3 you!
			DownloadManager dm = DataSourceUtils.getDM(ds);
			if (dm != null) {
				UIFunctionsManager.getUIFunctions().openView(UIFunctions.VIEW_DM_DETAILS, dm);
				return;
			}
		}else if (mode.equals("2")) {
			// Show in explorer
			DownloadManager dm = DataSourceUtils.getDM(ds);
			if (dm != null) {
				boolean openMode = COConfigurationManager.getBooleanParameter("MyTorrentsView.menu.show_parent_folder_enabled");
				ManagerUtils.open(dm, openMode);
				return;
			}
		}
		
		if (neverPlay) {
			return;
		}
		
			// fallback
		
		if (PlayUtils.canPlayDS(ds, -1) || (stateMask & SWT.CONTROL) > 0) {
			TorrentListViewsUtils.playOrStreamDataSource(ds,
					DLReferals.DL_REFERAL_DBLCLICK, false, true );
		}

		if (PlayUtils.canStreamDS(ds, -1)) {
			TorrentListViewsUtils.playOrStreamDataSource(ds,
					DLReferals.DL_REFERAL_DBLCLICK, true, false );
		}
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#getUpdateUIName()
	public String getUpdateUIName() {
		return ID;
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#updateUI()
	public void updateUI() {
		if (viewComposite == null || viewComposite.isDisposed()
				|| !viewComposite.isVisible() || view == null) {
			return;
		}
		view.refresh();
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectShown(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectShown(SWTSkinObject skinObject, Object params) {
		super.skinObjectShown(skinObject, params);

		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi != null) {
			MdiEntry entry = mdi.getEntryFromSkinObject(skinObject);
			if (entry != null) {
				entry.addToolbarEnabler(this);
			}
		}
		
		if (view instanceof IViewExtension) {
			((IViewExtension) view).viewActivated();
		}

		if (torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED
				&& AzureusCoreFactory.isCoreRunning()) {
			if (view instanceof MyTorrentsView) {
				MyTorrentsView torrentsView = (MyTorrentsView) view;
				TableViewSWT tv = torrentsView.getTableView();
				List dms = AzureusCoreFactory.getSingleton().getGlobalManager().getDownloadManagers();
				for (Iterator iter = dms.iterator(); iter.hasNext();) {
					DownloadManager dm = (DownloadManager) iter.next();

					if (!torrentsView.isOurDownloadManager(dm)) {
						tv.removeDataSource(dm);
					} else {
						tv.addDataSource(dm);
					}
				}
			}
		}

		if (view instanceof MyTorrentsView) {
			((MyTorrentsView)view).updateSelectedContent( true );
		}
		
		Utils.execSWTThreadLater(0, new AERunnable() {
			
			public void runSupport() {
				updateUI();
			}
		});

		return null;
	}
	
	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#skinObjectHidden(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectHidden(SWTSkinObject skinObject, Object params) {
		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi != null) {
			MdiEntry entry = mdi.getEntryFromSkinObject(skinObject);
			if (entry != null) {
				entry.removeToolbarEnabler( this );
			}
		}

		if (view instanceof IViewExtension) {
			((IViewExtension) view).viewActivated();
		}

		return super.skinObjectHidden(skinObject, params);
	}

	public void refreshToolBar(Map<String, Boolean> list) {
		if (view instanceof ToolBarEnabler) {
			((ToolBarEnabler) view).refreshToolBar(list);
		}
		if (tv == null) {
			return;
		}
		ISelectedContent[] currentContent = SelectedContentManager.getCurrentlySelectedContent();
		boolean has1Selection = currentContent.length == 1;
		list.put(
				"play",
				has1Selection
						&& (!(currentContent[0] instanceof ISelectedVuzeFileContent))
						&& PlayUtils.canPlayDS(currentContent[0],
								currentContent[0].getFileIndex()));
		list.put(
				"stream",
				has1Selection
						&& (!(currentContent[0] instanceof ISelectedVuzeFileContent))
						&& PlayUtils.canStreamDS(currentContent[0],
								currentContent[0].getFileIndex()));
	}

	public boolean toolBarItemActivated(String itemKey) {
		if (view instanceof ToolBarEnabler) {
			if (((ToolBarEnabler) view).toolBarItemActivated(itemKey)) {
				return true;
			}
		}
		// currently stream and play are handled by ToolbarView..
		return false;
	}

	/**
	 * Return either MODE_SMALLTABLE or MODE_BIGTABLE
	 * Subclasses may override
	 * @return
	 */
	protected int getTableMode() {
		return SBC_LibraryView.MODE_SMALLTABLE;
	}

	/**
	 * Returns whether the big version of the tables should be used
	 * Subclasses may override
	 * @return
	 */
	protected boolean useBigTable() {
		return false;
	}

	/**
	 * Returns the appropriate set of columns for the completed or incomplete torrents views
	 * Subclasses may override to return different sets of columns
	 * @return
	 */
	protected TableColumnCore[] getColumns() {
		if (torrentFilterMode == SBC_LibraryView.TORRENTS_COMPLETE) {
			return TableColumnCreator.createCompleteDM(TableManager.TABLE_MYTORRENTS_COMPLETE);
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_INCOMPLETE) {
			return TableColumnCreator.createIncompleteDM(TableManager.TABLE_MYTORRENTS_INCOMPLETE);
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_UNOPENED) {
			return TableColumnCreatorV3.createUnopenedDM(
					TableManager.TABLE_MYTORRENTS_UNOPENED, false);
		} else if (torrentFilterMode == SBC_LibraryView.TORRENTS_ALL) {
			return TableColumnCreator.createCompleteDM(TableManager.TABLE_MYTORRENTS_ALL_BIG);
		}

		return null;
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#skinObjectDestroyed(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectDestroyed(SWTSkinObject skinObject, Object params) {
		if (view != null) {
			view.delete();
		}
		return super.skinObjectDestroyed(skinObject, params);
	}
	
	// @see org.gudy.azureus2.ui.swt.debug.ObfusticateImage#obfusticatedImage(org.eclipse.swt.graphics.Image, org.eclipse.swt.graphics.Point)
	public Image obfusticatedImage(Image image) {
		if (view instanceof ObfusticateImage) {
			ObfusticateImage oi = (ObfusticateImage) view;
			return oi.obfusticatedImage(image);
		}
		return image;
	}
}
