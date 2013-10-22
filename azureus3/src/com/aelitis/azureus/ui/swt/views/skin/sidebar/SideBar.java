/**
 * Created on Jun 23, 2008
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

package com.aelitis.azureus.ui.swt.views.skin.sidebar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.ui.config.ConfigSectionHolder;
import org.gudy.azureus2.pluginsimpl.local.ui.config.ConfigSectionRepository;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.MenuBuildUtils;
import org.gudy.azureus2.ui.swt.URLTransfer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.plugins.*;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore;
import org.gudy.azureus2.ui.swt.views.IViewAlwaysInitialize;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.updater.UIUpdater;
import com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo;
import com.aelitis.azureus.ui.mdi.MdiEntry;
import com.aelitis.azureus.ui.mdi.MdiEntryVitalityImage;
import com.aelitis.azureus.ui.swt.mdi.*;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.utils.FontUtils;

/**
 * @author TuxPaper
 * @created Jun 23, 2008
 *
 */
public class SideBar
	extends BaseMDI
	implements ObfusticateImage, AEDiagnosticsEvidenceGenerator
{
	protected static final boolean END_INDENT = Constants.isLinux
			|| Constants.isWindows2000 || Constants.isWindows9598ME;

	private static final boolean USE_PAINTITEM = !Utils.isCarbon;

	// Need to use paint even on Cocoa, because there's cases where an area
	// will become invalidated and we don't get a paintitem :(
	private static final boolean USE_PAINT = !Constants.isWindows && !Utils.isGTK;

	protected static final boolean USE_NATIVE_EXPANDER = Utils.isGTK;

	private static final int GAP_BETWEEN_LEVEL_1;
	
	
	static{
		GAP_BETWEEN_LEVEL_1 = Math.min( 5, Math.max( 0, COConfigurationManager.getIntParameter( "Side Bar Top Level Gap", 1 )));
	}

	protected static final int SIDEBAR_ATTENTION_PERIOD	 	= 500;
	protected static final int SIDEBAR_ATTENTION_DURATION 	= 5000;
	
	private SWTSkin skin;

	private SWTSkinObjectContainer soSideBarContents;

	private SWTSkinObject soSideBarList;

	private Tree tree;

	private Font fontHeader;

	private Font font;

	private SWTSkinObject soSideBarPopout;

	private SelectionListener dropDownSelectionListener;

	private DropTarget dropTarget;

	protected SideBarEntrySWT draggingOver;

	private Color fg;

	private Color bg;

	private List<MdiSWTMenuHackListener> listMenuHackListners;

	private List<SideBarEntrySWT> 	attention_seekers = new ArrayList<SideBarEntrySWT>();
	private TimerEventPeriodic		attention_event;

	public static SideBar instance = null;

	
	public SideBar() {
		super();
		if (instance == null) {
			instance = this;
		}
		AEDiagnostics.addEvidenceGenerator(this);
	}

	// @see com.aelitis.azureus.ui.swt.skin.SWTSkinObjectAdapter#skinObjectCreated(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectCreated(SWTSkinObject skinObject, Object params) {
		super.skinObjectCreated(skinObject, params);

		skin = skinObject.getSkin();

		soSideBarContents = (SWTSkinObjectContainer) skin.getSkinObject("sidebar-contents");
		soSideBarList = skin.getSkinObject("sidebar-list");
		soSideBarPopout = skin.getSkinObject("sidebar-pop");

		// addTestMenus();

		createSideBar();

		try {
				// don't think this is required as the SideBar constructor (well SkinView) registers it
			
			UIUpdater updater = UIFunctionsManager.getUIFunctions().getUIUpdater();
			
			if ( !updater.isAdded( this )){
				
				updater.addUpdater(this);
			}
		} catch ( Throwable  e) {
			Debug.out(e);
		}

		Display.getDefault().addFilter(SWT.KeyDown, new Listener() {
			public void handleEvent(Event event) {
				// F9 is standard Seamonkey, but doesn't work on OSX
				// Command Option T is standard on OSX
				// F7 works on both
				if (event.keyCode == SWT.F9
						|| event.keyCode == SWT.F7
						|| (event.keyCode == 116 && event.stateMask == (SWT.COMMAND | SWT.ALT))) {
					event.doit = false;
					event.keyCode = 0;
					event.character = '\0';
					flipSideBarVisibility();
				}
			}
		});

		return null;
	}

	/**
	 * 
	 *
	 * @since 3.1.0.1
	 */
	/*
	private void addTestMenus() {
		// Add some test menus
		PluginManager pm = AzureusCoreFactory.getSingleton().getPluginManager();
		PluginInterface pi = pm.getDefaultPluginInterface();
		UIManager uim = pi.getUIManager();
		MenuManager menuManager = uim.getMenuManager();
		MenuItem menuItem = menuManager.addMenuItem("sidebar", "test menu");
		menuItem.addListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				ToolBarView tb = (ToolBarView) SkinViewManager.getByClass(ToolBarView.class);
				if (tb != null) {
					System.out.println("Found download Toolbar");
					ToolBarItem dlItem = tb.getToolBarItem("download");
					System.out.println("Download ToolBar Item is " + dlItem);
					if (dlItem != null) {
						System.out.println(dlItem.getSkinButton().getSkinObject());
					}
					dlItem.setEnabled(!dlItem.isEnabled());
				}

				if (target instanceof SideBarEntry) {
					SideBarEntry info = (SideBarEntry) target;
					System.err.println(info.getId() + " of " + info.getParentID()
							+ ";ds=" + info.getDatasource());
				}
			}
		});

		menuItem = menuManager.addMenuItem("sidebar." + SIDEBAR_SECTION_ACTIVITIES,
				"Activity Only Menu");
		menuItem.addListener(new MenuItemListener() {
			public void selected(MenuItem menu, Object target) {
				if (target instanceof SideBarEntry) {
					SideBarEntry info = (SideBarEntry) target;
					System.err.println(info.getId() + " of " + info.getParentID()
							+ ";ds=" + info.getDatasource());
				}
			}
		});
	}
	*/

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	public void flipSideBarVisibility() {
		final SWTSkinObjectSash soSash = (SWTSkinObjectSash) skin.getSkinObject("sidebar-sash");
		if (soSash == null) {
			return;
		}
		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				boolean visible = !soSash.isAboveVisible();
				
				soSash.setAboveVisible( visible );
				updateSidebarVisibility();
				
				COConfigurationManager.setParameter( "Show Side Bar", visible );
			}
		});
	}

	private void updateSidebarVisibility() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				final SWTSkinObjectSash soSash = (SWTSkinObjectSash) skin.getSkinObject("sidebar-sash");
				if (soSash == null) {
					return;
				}
				if (soSash.isAboveVisible()) {
					if (soSideBarPopout != null) {
						Object ld = soSideBarPopout.getControl().getLayoutData();
						if (ld instanceof FormData) {
							FormData fd = (FormData) ld;
							fd.width = 0;
						}
						soSideBarPopout.setVisible(false);

						Utils.relayout(soSideBarPopout.getControl());
					}
				} else {
					if (soSideBarPopout != null) {
						Object ld = soSideBarPopout.getControl().getLayoutData();
						if (ld instanceof FormData) {
							FormData fd = (FormData) ld;
							fd.width = 24;
						}
						soSideBarPopout.setVisible(true);
						soSideBarPopout.getControl().moveAbove(null);
						Utils.relayout(soSideBarPopout.getControl());
					}
				}
			}
		});
	}

	public boolean isVisible() {
		SWTSkinObjectSash soSash = (SWTSkinObjectSash) skin.getSkinObject("sidebar-sash");
		if (soSash == null) {
			return false;
		}
		return soSash.isAboveVisible();
	}

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {

		UIManager ui_manager = PluginInitializer.getDefaultInterface().getUIManager();
		ui_manager.addUIListener(new UIManagerListener() {
			public void UIDetached(UIInstance instance) {
			}
			
			public void UIAttached(UIInstance instance) {
				if (instance instanceof UISWTInstance) {
					final AESemaphore wait_sem = new AESemaphore( "SideBar:wait" );
					
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							try{
								try {
									loadCloseables();
								} catch (Throwable t) {
									Debug.out(t);
								}
	
								setupPluginViews();
							}finally{
								
								wait_sem.release();
							}
						}
					});
					
						// we need to wait for the loadCloseables to complete as there is code in MainMDISetup that runs on the 'UIAttachedComplete'
						// callback that needs the closables to be loaded (when setting 'start tab') otherwise the order gets broken
					
					if ( !wait_sem.reserve(10*1000)){
						
						Debug.out( "eh?");
					}
				}
			}
		});

		COConfigurationManager.addParameterListener(
			"Show Side Bar",
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String name )
				{
					boolean visible = COConfigurationManager.getBooleanParameter( name );
					
					if ( visible != isVisible()){
						
						flipSideBarVisibility();
					}
				}
			});
		
		updateSidebarVisibility();
		
		return null;
	}

	public Object skinObjectDestroyed(SWTSkinObject skinObject, Object params) {
		try {
			UIFunctionsManager.getUIFunctions().getUIUpdater().removeUpdater(this);
		} catch (Exception e) {
			Debug.out(e);
		}

		return super.skinObjectDestroyed(skinObject, params);
	}

	private void createSideBar() {
		if (soSideBarList == null) {
			return;
		}
		Composite parent = (Composite) soSideBarList.getControl();

		tree = new Tree(parent, SWT.FULL_SELECTION | SWT.V_SCROLL
				| SWT.DOUBLE_BUFFERED | SWT.NO_SCROLL);
		tree.setHeaderVisible(false);

		new SideBarToolTips(this, tree);

		tree.setLayoutData(Utils.getFilledFormData());

		SWTSkinProperties skinProperties = skin.getSkinProperties();
		bg = skinProperties.getColor("color.sidebar.bg");
		fg = skinProperties.getColor("color.sidebar.fg");

		tree.setBackground(bg);
		tree.setForeground(fg);
		FontData[] fontData = tree.getFont().getFontData();

		int fontHeight = (Constants.isOSX ? 11 : 12)
				+ (tree.getItemHeight() > 18 ? tree.getItemHeight() - 18 : 0);

		fontData[0].setStyle(SWT.BOLD);
		FontUtils.getFontHeightFromPX(tree.getDisplay(), fontData, null, fontHeight);
		fontHeader = new Font(tree.getDisplay(), fontData);
		font = FontUtils.getFontWithHeight(tree.getFont(), null, fontHeight);

		tree.setFont(font);
		
			// after a scroll we need to recalculate the hit areas as they will have moved!
		
		tree.getVerticalBar().addSelectionListener( 
			new SelectionAdapter()
			{
				public void widgetSelected(SelectionEvent e) {
					if ( e.detail == SWT.None ){
						SideBarEntrySWT[] sideBarEntries = mapIdToEntry.values().toArray(
								new SideBarEntrySWT[0]);
						swt_updateSideBarHitAreasY(sideBarEntries);
					}
				}
			});
		
		Listener treeListener = new Listener() {
			TreeItem lastTopItem = null;

			boolean mouseDowned = false;

			private boolean wasExpanded;

			public void handleEvent(final Event event) {
				TreeItem treeItem = (TreeItem) event.item;
				Tree tree = getTree();

				try {
					switch (event.type) {
						case SWT.MeasureItem: {
							int clientWidth = tree.getClientArea().width;
							String text = treeItem.getText(event.index);
							Point size = event.gc.textExtent(text);
							if (event.x + event.width < clientWidth) {
								event.width = size.x + event.x; // tree.getClientArea().width;
								event.x = 0;
							}

							if (Constants.isWindows) {
								event.width = clientWidth - event.x;
							}

							event.height = 20;

							break;
						}
						case SWT.PaintItem: {
							if (USE_PAINTITEM) {
								SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");
								//System.out.println("PaintItem: " + event.item + ";" + event.index + ";" + event.detail + ";" + id + ";" + event.getBounds() + ";" + event.gc.getClipping());
								if (entry != null) {
									boolean selected = currentEntry == entry
											&& entry.isSelectable();

									if (!selected) {
										event.detail &= ~SWT.SELECTED;
									} else {
										event.detail |= SWT.SELECTED;
									}
									entry.swt_paintSideBar(event);
								}
							}
							break;
						}

						case SWT.Paint: {
							//System.out.println("Paint: " + event.getBounds() + ";" + event.detail + ";" + event.index + ";" + event.gc.getClipping() + "  " + Debug.getCompressedStackTrace());
							if (!USE_PAINT) {
								return;
							}
							Rectangle bounds = event.getBounds();
							int indent = END_INDENT ? tree.getClientArea().width - 1 : 0;
							int y = event.y + 1;
							treeItem = tree.getItem(new Point(indent, y));

							while (treeItem != null) {
								SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");
								Rectangle itemBounds = entry == null ? null
										: entry.swt_getBounds();

								// null itemBounds is weird, the entry must be disposed. it 
								// happened once, so let's check..
								if (itemBounds != null && entry != null) {
									event.item = treeItem;

									boolean selected = currentEntry == entry
											&& entry.isSelectable();
									event.detail = selected ? SWT.SELECTED : SWT.NONE;

									Rectangle newClip = bounds.intersection(itemBounds);
									//System.out.println("Paint " + id + " @ " + newClip);
									event.setBounds(newClip);
									event.gc.setClipping(newClip);

									entry.swt_paintSideBar(event);

									y = itemBounds.y + itemBounds.height + 1;
								} else {
									y += tree.getItemHeight();
								}

								if (y > bounds.y + bounds.height) {
									break;
								}
								TreeItem oldTreeItem = treeItem;
								treeItem = tree.getItem(new Point(indent, y));
								if (oldTreeItem == treeItem) {
									break;
								}
							}

							if (tree.getTopItem() != lastTopItem) {
								lastTopItem = tree.getTopItem();
								SideBarEntrySWT[] sideBarEntries = mapIdToEntry.values().toArray(
										new SideBarEntrySWT[0]);
								swt_updateSideBarHitAreasY(sideBarEntries);
							}

							break;
						}

						case SWT.EraseItem: {
							SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");
							if (entry == null) {
								event.detail = 0;
							}
							//event.detail &= ~SWT.FOREGROUND;
							//event.detail &= ~(SWT.FOREGROUND | SWT.BACKGROUND);
							event.doit = true;
							break;
						}

						case SWT.Resize: {
							tree.redraw();
							break;
						}

						case SWT.Selection: {
							if (treeItem == null) {
								return;
							}
							SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");
							if (entry != null && entry.isSelectable()) {
								showEntry(entry);
							} else if (currentEntry != null) {
								TreeItem topItem = tree.getTopItem();

								// prevent "jumping" in the case where selection is off screen
								// setSelection would jump the item on screen, and then
								// showItem would jump back to where the user was.
								tree.setRedraw(false);
								TreeItem ti = ((SideBarEntrySWT) currentEntry).getTreeItem();
								if (ti != null) {
									tree.setSelection(ti);
								}

								tree.setTopItem(topItem);
								tree.setRedraw(true);

								event.doit = false;
							}
							break;
						}

						case SWT.MouseMove: {
							int indent = END_INDENT ? tree.getClientArea().width - 1 : 0;
							treeItem = tree.getItem(new Point(indent, event.y));
							SideBarEntrySWT entry = (SideBarEntrySWT) ((treeItem == null || treeItem.isDisposed())
									? null : treeItem.getData("MdiEntry"));

							int cursorNo = SWT.CURSOR_ARROW;
							if (treeItem != null) {
								Rectangle closeArea = (Rectangle) treeItem.getData("closeArea");
								if (closeArea != null && closeArea.contains(event.x, event.y)) {
									cursorNo = SWT.CURSOR_HAND;
								} else if (entry != null && !entry.isCollapseDisabled()
										&& treeItem.getItemCount() > 0) {
									cursorNo = SWT.CURSOR_HAND;
								}
							}

							Cursor cursor = event.display.getSystemCursor(cursorNo);
							if (tree.getCursor() != cursor) {
								tree.setCursor(cursor);
							}

							if (treeItem != null) {
								wasExpanded = entry != null && entry.isExpanded();
							} else {
								wasExpanded = false;
							}
							break;
						}

						case SWT.MouseDown: {
							mouseDowned = true;
							break;
						}

						case SWT.MouseUp: {
							if (!mouseDowned) {
								return;
							}
							mouseDowned = false;
							if (tree.getItemCount() == 0 || event.button != 1) {
								return;
							}
							int indent = END_INDENT ? tree.getClientArea().width - 1 : 0;
							treeItem = tree.getItem(new Point(indent, event.y));
							if (treeItem == null) {
								return;
							}
							SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");

							Rectangle closeArea = (Rectangle) treeItem.getData("closeArea");
							if (closeArea != null && closeArea.contains(event.x, event.y)) {
								treeItem.dispose();
								return;
							} else if (currentEntry != entry && Constants.isOSX) {
								showEntry(entry);
							}

							if (entry != null) {
								MdiEntryVitalityImage[] vitalityImages = entry.getVitalityImages();
								for (int i = 0; i < vitalityImages.length; i++) {
									SideBarVitalityImageSWT vitalityImage = (SideBarVitalityImageSWT) vitalityImages[i];
									if (vitalityImage == null || !vitalityImage.isVisible()) {
										continue;
									}
									Rectangle hitArea = vitalityImage.getHitArea();
									if (hitArea == null) {
										continue;
									}
									// setHitArea needs it relative to entry
									Rectangle itemBounds = entry.swt_getBounds();
									int relY = event.y - (itemBounds == null ? 0 : itemBounds.y);

									if (hitArea.contains(event.x, relY)) {
										vitalityImage.triggerClickedListeners(event.x, relY);
										return;
									}
								}

								if (!entry.isCollapseDisabled() && treeItem.getItemCount() > 0) {
									if (!entry.isSelectable() || event.x < 20) {
										// Note: On Windows, user can expand row by clicking the invisible area where the OS twisty would be
  									MdiEntry currentEntry = getCurrentEntry();
  									if (currentEntry != null
  											&& entry.getId().equals(currentEntry.getParentID())) {
  										showEntryByID(SIDEBAR_SECTION_LIBRARY);
  									}
  									entry.setExpanded(!wasExpanded);
  									wasExpanded = !wasExpanded;
									}
								}
							}

							break;
						}

						case SWT.Dispose: {
							fontHeader.dispose();
							font.dispose();
							if (dropTarget != null && !dropTarget.isDisposed()) {
								dropTarget.dispose();
							}
							saveCloseables();

							break;
						}

						case SWT.Collapse: {
							SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");

							if (entry.isCollapseDisabled()) {
								tree.setRedraw(false);
								Display.getDefault().asyncExec(new Runnable() {
									public void run() {
										((TreeItem) event.item).setExpanded(true);
										getTree().setRedraw(true);
									}
								});
							} else {
								MdiEntry currentEntry = getCurrentEntry();
								if (currentEntry != null
										&& entry.getId().equals(currentEntry.getParentID())) {
									showEntryByID(SIDEBAR_SECTION_LIBRARY);
								}
							}
							break;
						}

					}
				} catch (Exception e) {
					Debug.out(e);
				}
			}
		};
		tree.addListener(SWT.MeasureItem, treeListener);
		tree.addListener(SWT.Resize, treeListener);
		tree.addListener(SWT.Paint, treeListener);
		if (USE_PAINTITEM) {
			tree.addListener(SWT.PaintItem, treeListener);
			tree.addListener(SWT.EraseItem, treeListener);
		}

		tree.addListener(SWT.Selection, treeListener);
		tree.addListener(SWT.Dispose, treeListener);

		// For icons
		tree.addListener(SWT.MouseUp, treeListener);
		tree.addListener(SWT.MouseDown, treeListener);

		// For cursor
		tree.addListener(SWT.MouseMove, treeListener);

		// to disable collapsing
		tree.addListener(SWT.Collapse, treeListener);

		dropTarget = new DropTarget(tree, DND.DROP_COPY);
		dropTarget.setTransfer(new Transfer[] {
			URLTransfer.getInstance(),
			FileTransfer.getInstance(),
			TextTransfer.getInstance(),
		});

		dropTarget.addDropListener(new DropTargetAdapter() {
			public void dropAccept(DropTargetEvent event) {
				event.currentDataType = URLTransfer.pickBestType(event.dataTypes,
						event.currentDataType);
			}

			public void dragEnter(DropTargetEvent event) {
			}

			public void dragOperationChanged(DropTargetEvent event) {
			}

			// @see org.eclipse.swt.dnd.DropTargetAdapter#dragOver(org.eclipse.swt.dnd.DropTargetEvent)
			public void dragOver(DropTargetEvent event) {
				TreeItem treeItem = (event.item instanceof TreeItem)
						? (TreeItem) event.item : null;

				if (treeItem != null) {
					SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");

					draggingOver = entry;
				} else {
					draggingOver = null;
				}
				if (draggingOver == null || !draggingOver.hasDropListeners()) {

					boolean isTorrent = TorrentOpener.doesDropHaveTorrents(event);

					if (isTorrent) {
						event.detail = DND.DROP_COPY;
					} else {
						event.detail = DND.DROP_NONE;
					}
					draggingOver = null;
				} else if ((event.operations & DND.DROP_LINK) > 0)
					event.detail = DND.DROP_LINK;
				else if ((event.operations & DND.DROP_COPY) > 0)
					event.detail = DND.DROP_COPY;
				else if ((event.operations & DND.DROP_DEFAULT) > 0)
					event.detail = DND.DROP_COPY;

				if (Constants.isOSX) {
					tree.redraw();
				}
				
				event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL | DND.FEEDBACK_EXPAND;
			}

			// @see org.eclipse.swt.dnd.DropTargetAdapter#dragLeave(org.eclipse.swt.dnd.DropTargetEvent)
			public void dragLeave(DropTargetEvent event) {
				draggingOver = null;
				tree.redraw();
			}

			public void drop(DropTargetEvent event) {
				draggingOver = null;
				tree.redraw();
				if (!(event.item instanceof TreeItem)) {
					defaultDrop(event);
					return;
				}
				TreeItem treeItem = (TreeItem) event.item;

				SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");

				boolean handled = entry.triggerDropListeners(event.data);
				if (!handled) {
					defaultDrop(event);
				}
			}
		});

		final Menu menuTree = new Menu(tree);
		tree.setMenu(menuTree);

		menuTree.addMenuListener(new MenuListener() {
			boolean bShown = false;

			public void menuHidden(MenuEvent e) {
				bShown = false;

				if (Constants.isOSX) {
					return;
				}

				// Must dispose in an asyncExec, otherwise SWT.Selection doesn't
				// get fired (async workaround provided by Eclipse Bug #87678)
				Utils.execSWTThreadLater(0, new AERunnable() {
					public void runSupport() {
						if (bShown || menuTree.isDisposed()) {
							return;
						}
						Utils.disposeSWTObjects(menuTree.getItems());
					}
				});
			}

			public void menuShown(MenuEvent e) {
				Utils.disposeSWTObjects(menuTree.getItems());

				bShown = true;

				Point ptMouse = tree.toControl(e.display.getCursorLocation());

				int indent = END_INDENT ? tree.getClientArea().width - 1 : 0;
				TreeItem treeItem = tree.getItem(new Point(indent, ptMouse.y));
				if (treeItem == null) {
					return;
				}
				SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");

				fillMenu(menuTree, entry);

				if (menuTree.getItemCount() == 0) {
					Utils.execSWTThreadLater(0, new AERunnable() {
						public void runSupport() {
							menuTree.setVisible(false);
						}
					});
				}
			}
		});

		if (soSideBarPopout != null) {
			SWTSkinObject soDropDown = skin.getSkinObject("sidebar-dropdown");
			if (soDropDown != null) {

				final Menu menuDropDown = new Menu(soDropDown.getControl());

				menuDropDown.addMenuListener(new MenuListener() {
					boolean bShown = false;

					public void menuHidden(MenuEvent e) {
						bShown = false;

						if (Constants.isOSX) {
							return;
						}

						// Must dispose in an asyncExec, otherwise SWT.Selection doesn't
						// get fired (async workaround provided by Eclipse Bug #87678)
						Utils.execSWTThreadLater(0, new AERunnable() {
							public void runSupport() {
								if (bShown || menuDropDown.isDisposed()) {
									return;
								}
								Utils.disposeSWTObjects(menuDropDown.getItems());
							}
						});
					}

					public void menuShown(MenuEvent e) {
						Utils.disposeSWTObjects(menuDropDown.getItems());

						bShown = true;

						fillDropDownMenu(menuDropDown, tree.getItems(), 0);
					}
				});

				dropDownSelectionListener = new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						String id = (String) e.widget.getData("Plugin.viewID");
						showEntryByID(id);
					}

					public void widgetDefaultSelected(SelectionEvent e) {
					}
				};

				SWTSkinButtonUtility btnDropDown = new SWTSkinButtonUtility(soDropDown);
				btnDropDown.addSelectionListener(new ButtonListenerAdapter() {
					public void pressed(SWTSkinButtonUtility buttonUtility,
							SWTSkinObject skinObject, int stateMask) {
						Control c = buttonUtility.getSkinObject().getControl();
						menuDropDown.setLocation(c.getDisplay().getCursorLocation());
						menuDropDown.setVisible(!menuDropDown.getVisible());
					}
				});
			}

			SWTSkinObject soExpand = skin.getSkinObject("sidebar-expand");
			if (soExpand != null) {
				SWTSkinButtonUtility btnExpand = new SWTSkinButtonUtility(soExpand);
				btnExpand.addSelectionListener(new ButtonListenerAdapter() {
					public void pressed(SWTSkinButtonUtility buttonUtility,
							SWTSkinObject skinObject, int stateMask) {
						flipSideBarVisibility();
					}
				});
			}

		}
	}

	/**
	 * @param event
	 */
	protected void defaultDrop(DropTargetEvent event) {
		TorrentOpener.openDroppedTorrents(event, false);
	}

	/**
	 * @param menuDropDown
	 *
	 * @since 3.1.1.1
	 */
	protected void fillDropDownMenu(Menu menuDropDown, TreeItem[] items,
			int indent) {
		String s = "";
		for (int i = 0; i < indent; i++) {
			s += "   ";
		}
		for (int i = 0; i < items.length; i++) {
			TreeItem treeItem = items[i];
			
			SideBarEntrySWT entry = (SideBarEntrySWT) treeItem.getData("MdiEntry");
			if (entry == null) {
				continue;
			}
			org.eclipse.swt.widgets.MenuItem menuItem = new org.eclipse.swt.widgets.MenuItem(
					menuDropDown, entry.isSelectable() ? SWT.RADIO : SWT.CASCADE);
			
			String id = entry.getId();
			menuItem.setData("Plugin.viewID", id);
			ViewTitleInfo titleInfo = entry.getViewTitleInfo();
			String ind = "";
			if (titleInfo != null) {
				String o = (String) titleInfo.getTitleInfoProperty(ViewTitleInfo.TITLE_INDICATOR_TEXT);
				if (o != null) {
					ind = "  (" + o + ")";
					//ind = "\t" + o;
				}
			}
			menuItem.setText(s + entry.getTitle() + ind);
			menuItem.addSelectionListener(dropDownSelectionListener);
			if (currentEntry != null && currentEntry.getId().equals(id)) {
				menuItem.setSelection(true);
			}

			TreeItem[] subItems = treeItem.getItems();
			if (subItems.length > 0) {
				Menu parent = menuDropDown;
				if (!entry.isSelectable()) {
					parent = new Menu(menuDropDown.getParent().getShell(), SWT.DROP_DOWN);
					menuItem.setMenu(parent);
				}
				

				fillDropDownMenu(parent, subItems, indent + 1);
			}
		}
	}

	/**
	 * @param menuTree
	 *
	 * @since 3.1.0.1
	 */
	protected void fillMenu(Menu menuTree, final MdiEntry entry) {
		org.gudy.azureus2.plugins.ui.menus.MenuItem[] menu_items;

		menu_items = MenuItemManager.getInstance().getAllAsArray("sidebar");

		MenuBuildUtils.addPluginMenuItems((Composite) soMain.getControl(),
				menu_items, menuTree, false, true,
				new MenuBuildUtils.MenuItemPluginMenuControllerImpl(new Object[] {
					entry
				}));

		if (entry != null) {

			menu_items = MenuItemManager.getInstance().getAllAsArray(
					"sidebar." + entry.getId());

			if (menu_items.length == 0) {

				UIPluginView view = entry.getView();

				if (view instanceof UISWTView) {

					PluginInterface pi = ((UISWTView) view).getPluginInterface();

					if (pi != null) {

						final List<String> relevant_sections = new ArrayList<String>();

						List<ConfigSectionHolder> sections = ConfigSectionRepository.getInstance().getHolderList();

						for (ConfigSectionHolder cs : sections) {

							if (pi == cs.getPluginInterface()) {

								relevant_sections.add(cs.configSectionGetName());
							}
						}

						if (relevant_sections.size() > 0) {

							MenuItem mi = pi.getUIManager().getMenuManager().addMenuItem(
									"sidebar." + entry.getId(),
									"MainWindow.menu.view.configuration");

							mi.addListener(new MenuItemListener() {
								public void selected(MenuItem menu, Object target) {
									UIFunctions uif = UIFunctionsManager.getUIFunctions();

									if (uif != null) {

										for (String s : relevant_sections) {

											uif.openView(UIFunctions.VIEW_CONFIG, s);
										}
									}
								}
							});

							menu_items = MenuItemManager.getInstance().getAllAsArray(
									"sidebar." + entry.getId());
						}
					}
				}
			}

			MenuBuildUtils.addPluginMenuItems((Composite) soMain.getControl(),
					menu_items, menuTree, false, true,
					new MenuBuildUtils.MenuItemPluginMenuControllerImpl(new Object[] {
						entry
					}));

			MdiSWTMenuHackListener[] menuHackListeners = getMenuHackListeners();
			for (MdiSWTMenuHackListener l : menuHackListeners) {
				try {
					l.menuWillBeShown(entry, menuTree);
				} catch (Exception e) {
					Debug.out(e);
				}
			}
			if (currentEntry instanceof SideBarEntrySWT) {
				menuHackListeners = ((SideBarEntrySWT) entry).getMenuHackListeners();
				for (MdiSWTMenuHackListener l : menuHackListeners) {
					try {
						l.menuWillBeShown(entry, menuTree);
					} catch (Exception e) {
						Debug.out(e);
					}
				}
			}
		}
	}

	public void addListener(MdiSWTMenuHackListener l) {
		synchronized (this) {
			if (listMenuHackListners == null) {
				listMenuHackListners = new ArrayList<MdiSWTMenuHackListener>(1);
			}
			if (!listMenuHackListners.contains(l)) {
				listMenuHackListners.add(l);
			}
		}
	}

	public void removeListener(MdiSWTMenuHackListener l) {
		synchronized (this) {
			if (listMenuHackListners == null) {
				listMenuHackListners = new ArrayList<MdiSWTMenuHackListener>(1);
			}
			listMenuHackListners.remove(l);
		}
	}

	public MdiSWTMenuHackListener[] getMenuHackListeners() {
		synchronized (this) {
			if (listMenuHackListners == null) {
				return new MdiSWTMenuHackListener[0];
			}
			return listMenuHackListners.toArray(new MdiSWTMenuHackListener[0]);
		}
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	private void swt_updateSideBarHitAreasY(SideBarEntrySWT[] entries) {
		for (int x = 0; x < entries.length; x++) {
			SideBarEntrySWT entry = entries[x];
			TreeItem treeItem = entry.getTreeItem();
			if (treeItem == null || treeItem.isDisposed()) {
				continue;
			}
			Rectangle itemBounds = entry.swt_getBounds();

			if ( itemBounds != null ){
				if (entry.isCloseable()) {
					Rectangle closeArea = (Rectangle) treeItem.getData("closeArea");
					if (closeArea != null) {
						closeArea.y = itemBounds.y + (itemBounds.height - closeArea.height)
								/ 2;
					}
				}
	
				MdiEntryVitalityImage[] vitalityImages = entry.getVitalityImages();
				for (int i = 0; i < vitalityImages.length; i++) {
					SideBarVitalityImageSWT vitalityImage = (SideBarVitalityImageSWT) vitalityImages[i];
					if (!vitalityImage.isVisible()) {
						continue;
					}
					Image image = vitalityImage.getImage();
					if (image != null) {
						Rectangle bounds = vitalityImage.getHitArea();
						if (bounds == null) {
							continue;
						}
						bounds.y = (itemBounds.height - bounds.height) / 2;
					}
				}
			}
		}
	}

	protected int indexOf(final MdiEntry entryLibrary) {
		Object o = Utils.execSWTThreadWithObject("indexOf", new AERunnableObject() {
			public Object runSupport() {
				TreeItem treeItem = ((SideBarEntrySWT) entryLibrary).getTreeItem();
				if (treeItem == null) {
					return -1;
				}
				TreeItem parentItem = treeItem.getParentItem();
				if (parentItem != null) {
					return parentItem.indexOf(treeItem);
				}
				return tree.indexOf(treeItem);
			}
		}, 500);
		if (o instanceof Number) {
			return ((Number) o).intValue();
		}
		return -1;
	}

	public MdiEntry createHeader(String id, String titleID, String preferredAfterID) {
		MdiEntry oldEntry = getEntry(id);
		if (oldEntry != null) {
			return oldEntry;
		}

		SideBarEntrySWT entry = new SideBarEntrySWT(this, skin, id);
		entry.setSelectable(false);
		entry.setPreferredAfterID(preferredAfterID);
		entry.setTitleID(titleID);

		setupNewEntry(entry, id, true, false);

		return entry;
	}

	public MdiEntry createEntryFromView(String parentID, UISWTViewCore iview, String id,
			Object datasource, boolean closeable, boolean show, boolean expand) {
		if (id == null) {
			id = iview.getClass().getName();
			int i = id.lastIndexOf('.');
			if (i > 0) {
				id = id.substring(i + 1);
			}
		}

		MdiEntry oldEntry = getEntry(id);
		if (oldEntry != null) {
			if (show) {
				showEntry(oldEntry);
			}
			return oldEntry;
		}

		SideBarEntrySWT entry = new SideBarEntrySWT(this, skin, id);

		entry.setCoreView(iview);
		entry.setDatasource(datasource);
		entry.setParentID(parentID);

		setupNewEntry(entry, id, expand, closeable);

		if (iview instanceof IViewAlwaysInitialize) {
			entry.build();
		}

		if (show) {
			showEntry(entry);
		}

		return entry;
	}

	private void setupNewEntry(final SideBarEntrySWT entry, final String id,
			final boolean expandParent, final boolean closeable) {
		//System.out.println("createItem " + id + ";" + Debug.getCompressedStackTrace());
		synchronized (mapIdToEntry) {
			mapIdToEntry.put(id, entry);
		}

		entry.setCloseable(closeable);
		entry.setParentSkinObject(soSideBarContents);

		if (SIDEBAR_HEADER_PLUGINS.equals(entry.getParentID())
				&& entry.getImageLeftID() == null) {
			entry.setImageLeftID("image.sidebar.plugin");
		}

		Utils.execSWTThreadLater(0, new AERunnable() {
			public void runSupport() {
				_setupNewEntry(entry, id, expandParent, closeable);
			}
		});
	}

	protected void _setupNewEntry(SideBarEntrySWT entry, String id,
			boolean expandParent, boolean closeable) {
		String parentID = entry.getParentID();
		MdiEntry parent = getEntry(parentID);
		TreeItem parentTreeItem = null;
		if (parent instanceof SideBarEntrySWT) {
			SideBarEntrySWT parentSWT = (SideBarEntrySWT) parent;
			parentTreeItem = parentSWT.getTreeItem();
			if (expandParent) {
				parentTreeItem.setExpanded(true);
			}
		}
		int index = -1;
		String preferredAfterID = entry.getPreferredAfterID();
		if (preferredAfterID != null) {
			if (preferredAfterID.length() == 0) {
				index = 0;
			} else {
				boolean hack_it = preferredAfterID.startsWith( "~" );
				
				if ( hack_it ){
					
						//hack - this means preferred BEFORE ID...
					
					preferredAfterID = preferredAfterID.substring(1);
				}
				
				MdiEntry entryAbove = getEntry(preferredAfterID);
				if (entryAbove != null) {
					index = indexOf(entryAbove);
					if ( hack_it ){
				
					}else{
						if (index >= 0) {
							index++;
						}
					}
					//System.out.println("ENTRY " + id + " is going to go below " + entryAbove.getId() + " at " + index);
				}
			}
		}

		if (index == -1 && parent == null) {
			index = 0;
			String[] order = getPreferredOrder();
			for (int i = 0; i < order.length; i++) {
				String orderID = order[i];
				if (orderID.equals(id)) {
					break;
				}
				MdiEntry entry2 = getEntry(orderID);
				if (entry2 != null) {
					int i2 = indexOf(entry2);
					if (i2 >= 0) {
						index = i2 + 1;
					}
				}
			}
		}

		if (GAP_BETWEEN_LEVEL_1 > 0 && parentTreeItem == null
				&& tree.getItemCount() > 0 && index != 0) {
			for (int i=0;i<GAP_BETWEEN_LEVEL_1;i++){
				createTreeItem(null, index);
				if (index >= 0) {
					index++;
				}
			}
		}
		TreeItem treeItem = createTreeItem(parentTreeItem, index);
		if (treeItem != null) {
			treeItem.setData("MdiEntry", entry);
			entry.setTreeItem(treeItem);

			triggerEntryLoadedListeners(entry);
		}
		if (GAP_BETWEEN_LEVEL_1 > 0 && parentTreeItem == null
				&& tree.getItemCount() > 1 && index == 0) {
			for (int i=0;i<GAP_BETWEEN_LEVEL_1;i++){
				createTreeItem(null, ++index);
			}
		}
	}

	private TreeItem createTreeItem(Object parentSwtItem, int index) {
		TreeItem treeItem;

		if (parentSwtItem == null) {
			parentSwtItem = tree;
		}

		if (parentSwtItem instanceof Tree) {
			Tree tree = (Tree) parentSwtItem;
			if (tree.isDisposed()) {
				return null;
			}
			if (index >= 0 && index < tree.getItemCount()) {
				treeItem = new TreeItem(tree, SWT.NONE, index);
			} else {
				treeItem = new TreeItem(tree, SWT.NONE);
			}
		} else {
			if (((TreeItem) parentSwtItem).isDisposed()) {
				return null;
			}
			if (index >= 0 && index < ((TreeItem) parentSwtItem).getItemCount()) {
				treeItem = new TreeItem((TreeItem) parentSwtItem, SWT.NONE, index);
			} else {
				treeItem = new TreeItem((TreeItem) parentSwtItem, SWT.NONE);
			}
		}

		return treeItem;
	}

	public void showEntry(MdiEntry newEntry) {
		if (tree.isDisposed()) {
			return;
		}

		if (newEntry == null || !newEntry.isSelectable()) {
			return;
		}

		final SideBarEntrySWT oldEntry = (SideBarEntrySWT) currentEntry;

		//System.out.println("showEntry " + newEntry.getId() + "; was " + (oldEntry == null ? "null" : oldEntry.getId()) + " via " + Debug.getCompressedStackTrace());
		if (currentEntry == newEntry) {
			triggerSelectionListener(newEntry, newEntry);
			return;
		}

		// show new
		currentEntry = (MdiEntrySWT) newEntry;

		if (oldEntry != null && oldEntry != newEntry) {
			oldEntry.redraw();
		}

		if (currentEntry != null) {
			((BaseMdiEntry) currentEntry).show();
		}

		// hide old
		if (oldEntry != null && oldEntry != newEntry) {
			oldEntry.hide();
			oldEntry.redraw();
		}

		newEntry.redraw();

		triggerSelectionListener(newEntry, oldEntry);
	}

	/**
	 *  @see com.aelitis.azureus.ui.swt.mdi.BaseMDI#createEntryFromEventListener(java.lang.String, org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener, java.lang.String, boolean, java.lang.Object)
	 */
	public MdiEntry createEntryFromEventListener(String parentID,
			UISWTViewEventListener l, String id, boolean closeable, Object datasource, String preferredAfterID) {

		MdiEntry oldEntry = getEntry(id);
		if (oldEntry != null) {
			return oldEntry;
		}

		SideBarEntrySWT entry = new SideBarEntrySWT(this, skin, id);
		try {
			// hack: setEventListner will create the UISWTView.
			// We need to have the entry available for the view to use
			// if it wants
			synchronized (mapIdToEntry) {
				mapIdToEntry.put(id, entry);
			}

			entry.setParentID(parentID);
			entry.setDatasource(datasource);
			entry.setPreferredAfterID(preferredAfterID);
			setupNewEntry(entry, id, false, closeable);

			entry.setEventListener(l);

			if (l instanceof IViewAlwaysInitialize) {
				entry.build();
			}
		} catch (Exception e) {
			Debug.out(e);
			entry.close(true);
		}

		return entry;
	}

	// @see com.aelitis.azureus.ui.swt.mdi.BaseMDI#createEntryFromSkinRef(java.lang.String, java.lang.String, java.lang.String, java.lang.String, com.aelitis.azureus.ui.common.viewtitleinfo.ViewTitleInfo, java.lang.Object, boolean, java.lang.String)
	public MdiEntry createEntryFromSkinRef(String parentID, String id,
			String configID, String title, ViewTitleInfo titleInfo, Object params,
			boolean closeable, String preferredAfterID) {

		MdiEntry oldEntry = getEntry(id);
		if (oldEntry != null) {
			return oldEntry;
		}

		SideBarEntrySWT entry = new SideBarEntrySWT(this, skin, id);

		entry.setTitle(title);
		entry.setSkinRef(configID, params);
		entry.setParentID(parentID);
		entry.setViewTitleInfo(titleInfo);
		entry.setPreferredAfterID(preferredAfterID);

		setupNewEntry(entry, id, false, closeable);

		return entry;
	}

	// @see com.aelitis.azureus.ui.swt.utils.UIUpdatable#updateUI()
	public void updateUI() {
		if (currentEntry == null || currentEntry.getView() == null
				|| tree.getSelectionCount() == 0) {
			return;
		}
		currentEntry.updateUI();
	}

	protected boolean wasEntryLoadedOnce(String id) {
		@SuppressWarnings("deprecation")
		boolean loadedOnce = COConfigurationManager.getBooleanParameter("sb.once."
				+ id, false);
		return loadedOnce;
	}
	
	protected void setEntryLoadedOnce(String id) {
		COConfigurationManager.setParameter("sb.once." + id, true);
	}

	public Font getHeaderFont() {
		return fontHeader;
	}

	protected Tree getTree() {
		return tree;
	}

	// @see com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT#getEntryFromSkinObject(org.gudy.azureus2.ui.swt.plugins.PluginUISWTSkinObject)
	public MdiEntrySWT getEntryFromSkinObject(
			PluginUISWTSkinObject pluginSkinObject) {
		if (pluginSkinObject instanceof SWTSkinObject) {
			Control control = ((SWTSkinObject) pluginSkinObject).getControl();
			while (control != null && !control.isDisposed()) {
				Object entry = control.getData("BaseMDIEntry");
				if (entry instanceof BaseMdiEntry) {
					BaseMdiEntry mdiEntry = (BaseMdiEntry) entry;
					return mdiEntry;
				}
				control = control.getParent();
			}
		}
		return null;
	}
	
	protected void
	requestAttention(
		SideBarEntrySWT	entry )
	{
		synchronized( attention_seekers ){
	
			if ( !attention_seekers.contains( entry )){
				
				attention_seekers.add( entry );
			}
			
			if ( attention_event == null ){
				
				attention_event = 
					SimpleTimer.addPeriodicEvent(
						"SideBar:attention",
						SIDEBAR_ATTENTION_PERIOD,
						new TimerEventPerformer()
						{
							int	tick_count = 0;
							
							public void 
							perform(
								TimerEvent event ) 
							{
								tick_count++;
								
								final List<SideBarEntrySWT>	repaints = new ArrayList<SideBarEntrySWT>();
								
								synchronized( attention_seekers ){
									
									Iterator<SideBarEntrySWT> it = attention_seekers.iterator();
									
									while ( it.hasNext()){
										
										SideBarEntrySWT entry = it.next();
										
										if ( entry.isDisposed()){
											
											it.remove();
											
										}else{
										
											if ( !entry.attentionUpdate( tick_count )){
																																				
												it.remove();
											}
											
											repaints.add( entry );
										}
									}
									
									if ( attention_seekers.size() == 0 ){
										
										TimerEventPeriodic ev = attention_event;
										
										if ( ev != null ){
											
											ev.cancel();
											
											attention_event = null;
										}
									}
								}
								
								if ( repaints.size() > 0 ){
									
									Utils.execSWTThread(
										new AERunnable() 
										{
											@Override
											public void 
											runSupport() 
											{
												for ( SideBarEntrySWT entry: repaints ){
													
													entry.redraw();
												}
											}
										});
								}
							}
						});
						
			}
		}
	}
	
	public void generate(IndentWriter writer) {
		MdiEntrySWT[] entries = getEntriesSWT();
		for (MdiEntrySWT entry : entries) {
			if (entry == null) {
				continue;
			}

			UISWTViewCore view = entry.getCoreView();

			if (!(view instanceof AEDiagnosticsEvidenceGenerator)) {
				writer.println("Sidebar View (No Generator): " + entry.getId());
				try {
					writer.indent();

					writer.println("Parent: " + entry.getParentID());
					writer.println("Title: " + entry.getTitle());
				} catch (Exception e) {

				} finally {

					writer.exdent();
				}
			}
			
		}
	}

	// @see org.gudy.azureus2.ui.swt.debug.ObfusticateImage#obfusticatedImage(org.eclipse.swt.graphics.Image)
	public Image obfusticatedImage(Image image) {

		Rectangle treeBounds = tree.getBounds();
		SideBarEntrySWT[] sideBarEntries = mapIdToEntry.values().toArray(
				new SideBarEntrySWT[0]);
		for (SideBarEntrySWT entry : sideBarEntries) {
			Rectangle entryBounds = entry.swt_getBounds();
			if (entryBounds != null && treeBounds.intersects(entryBounds)) {
				entry.obfusticatedImage(image);
			}
		}
		return image;
	}
}
