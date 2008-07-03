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

package com.aelitis.azureus.ui.swt.views.skin;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionAdapter;
import com.aelitis.azureus.ui.swt.skin.*;
import com.aelitis.azureus.ui.swt.skin.SWTSkinButtonUtility.ButtonListenerAdapter;
import com.aelitis.azureus.ui.swt.views.TorrentListView;
import com.aelitis.azureus.ui.swt.views.TorrentListViewListener;
import com.aelitis.azureus.util.PublishUtils;

/**
 * @author TuxPaper
 * @created Sep 30, 2006
 *
 * TODO Code similaries between MiniRecentList, MiniDownloadList, ManageCdList, 
 *     and ManageDlList.  Need to combine
 */
public class ManageCdList
	extends SkinView
{
	private SWTSkinObjectText lblCountAreaNotOurs;

	private SWTSkinObjectText lblCountAreaOurs;

	private TorrentListView view;

	private String PREFIX = "manage-cd-";

	private SWTSkinObjectText statusObject;

	private AzureusCore core;

	private boolean bShowMyPublished = false;

	// @see com.aelitis.azureus.ui.swt.views.skin.SkinView#showSupport(com.aelitis.azureus.ui.swt.skin.SWTSkinObject, java.lang.Object)
	public Object skinObjectInitialShow(SWTSkinObject skinObject, Object params) {
		final SWTSkin skin = skinObject.getSkin();
		core = AzureusCoreFactory.getSingleton();
		

		skinObject = skin.getSkinObject(PREFIX + "titlextra");
		if (skinObject != null) {
			lblCountAreaNotOurs = (SWTSkinObjectText) skinObject;
			SWTSkinButtonUtility btnCountAreaNotOurs = new SWTSkinButtonUtility(
					skinObject);
			btnCountAreaNotOurs.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					if (bShowMyPublished) {
						bShowMyPublished = false;

						lblCountAreaOurs.switchSuffix("", 1, true);
						lblCountAreaNotOurs.switchSuffix("-selected", 1, true);
						view.regetDownloads();
					}
				}
			});
		}
		lblCountAreaNotOurs.switchSuffix("-selected", 1, true);

		skinObject = skin.getSkinObject(PREFIX + "titlextra-2");
		if (skinObject != null) {
			lblCountAreaOurs = (SWTSkinObjectText) skinObject;

			SWTSkinButtonUtility btnCountAreaOurs = new SWTSkinButtonUtility(
					skinObject);
			btnCountAreaOurs.addSelectionListener(new ButtonListenerAdapter() {
				public void pressed(SWTSkinButtonUtility buttonUtility) {
					if (!bShowMyPublished) {
						bShowMyPublished = true;

						lblCountAreaNotOurs.switchSuffix("", 1, true);
						lblCountAreaOurs.switchSuffix("-selected", 1, true);
						view.regetDownloads();
					}
				}
			});
		}

		view = new TorrentListView(this, PREFIX,
				TorrentListView.VIEW_RECENT_DOWNLOADED, false, true) {
			public boolean isOurDownload(DownloadManager dm) {
				if (dm == null) {
					Debug.out("DM == null");
					return false;
				}
				boolean bDownloadComplete = dm.isDownloadComplete(false);
				boolean isPublished = PublishUtils.isPublished(dm);
				try {
					return bDownloadComplete && (isPublished == bShowMyPublished);
				} catch (Exception e) {
					Debug.out("STUPID ERROR", e);
					return false;
				}
			}
		};


		view.addListener(new TorrentListViewListener() {
			boolean countChanging = false;

			public void countChanged() {
				if (countChanging) {
					return;
				}

				countChanging = true;
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						countChanging = false;

						long totalOurs = 0;
						long totalNotOurs = 0;

						GlobalManager globalManager = core.getGlobalManager();
						Object[] dms = globalManager.getDownloadManagers().toArray();

						for (int i = 0; i < dms.length; i++) {
							DownloadManager dm = (DownloadManager) dms[i];
							if (dm.isDownloadComplete(false)) {
								if (PublishUtils.isPublished(dm)) {
									totalOurs++;
								} else {
									totalNotOurs++;
								}
							}
						}

						if (lblCountAreaOurs != null) {
							lblCountAreaOurs.setText(MessageText.getString("v3.MainWindow."
									+ PREFIX + "ours.count", new String[] {
								"" + totalOurs
							}));
						}
						if (lblCountAreaNotOurs != null) {
							lblCountAreaNotOurs.setText(MessageText.getString(
									"v3.MainWindow." + PREFIX + "notours.count", new String[] {
										"" + totalNotOurs
									}));
							lblCountAreaNotOurs.getControl().getParent().layout(true, true);
						}

					}
				});
			}
		});

		skinObject = skin.getSkinObject(PREFIX + "status");
		if (skinObject instanceof SWTSkinObjectText) {
			statusObject = (SWTSkinObjectText) skinObject;

			view.addListener(new TorrentListViewListener() {
				public void stateChanged(DownloadManager manager) {
					TableRowCore[] selectedRows = view.getSelectedRows();
					updateStatusText(selectedRows.length == 1 ? selectedRows[0] : null);
				}
			});
		}

		view.addSelectionListener(new TableSelectionAdapter() {
			public void deselected(TableRowCore[] row) {
				update();
			}

			public void selected(TableRowCore[] row) {
				update();
			}

			public void focusChanged(TableRowCore focusedRow) {
				update();
			}

			private void update() {
				TableRowCore[] rows = view.getSelectedRows();
				if (rows.length == 0 || rows.length > 1) {
					updateStatusText(null);
				} else {
					updateStatusText(rows[0]);
				}
			}
		}, true);
		return null;
	}

	/**
	 * @param row
	 */
	protected void updateStatusText(TableRowCore row) {
		if (row != null) {
			DownloadManager dm = (DownloadManager) row.getDataSource(true);
			statusObject.setText(DisplayFormatters.formatDownloadStatus(dm));
		} else {
			statusObject.setTextID("v3.MainWindow." + PREFIX + "status.noselection");
		}
	}
}
