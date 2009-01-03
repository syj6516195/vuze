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

package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter.URLInfo;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.util.ConstantsV3;
import com.aelitis.azureus.util.DataSourceUtils;
import com.aelitis.azureus.util.UrlFilter;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Oct 10, 2006
 *
 */
public class ColumnTitle
	extends CoreTableColumn
	implements TableCellRefreshListener, ObfusticateCellText,
	TableCellMouseMoveListener, TableCellDisposeListener, TableCellVisibilityListener
{
	public static String COLUMN_ID = "name";

	public static boolean SHOW_EXT_INFO = false;
	
	public static boolean LINKIFY = false;

	static public String s = "";

	private Color colorLinkNormal;

	private Color colorLinkHover;

	/** Default Constructor */
	public ColumnTitle(String sTableID) {
		super(COLUMN_ID, POSITION_INVISIBLE, 250, sTableID);
		setMinWidth(70);
		setObfustication(true);
		if (LINKIFY) {
			setType(TableColumn.TYPE_GRAPHIC);

			SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
			colorLinkNormal = skinProperties.getColor("color.links.normal");
			colorLinkHover = skinProperties.getColor("color.links.hover");
		} else {
			// Mouse movement, cell disposal and visibility monitoring only needed
			// if we are making a graphic.  These get added automatically, so remove
			// them
			removeCellMouseMoveListener(this);
			removeCellDisposeListener(this);
			removeCellVisibilityListener(this);
		}
	}
	
	// @see org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener#dispose(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void dispose(TableCell cell) {
		disposeExisting(cell);
	}

	public void refresh(TableCell cell) {
		String name = null;
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		if (dm != null) {
			name = PlatformTorrentUtils.getContentTitle2(dm);
		}
		if (name == null) {
			name = "";
		}

		if (!cell.setSortValue(name) && cell.isValid()) {
			return;
		}

		if (!cell.isShown()) {
			return;
		}
		
		if (!LINKIFY) {
			cell.setText(name);
			return;
		}

		if (SHOW_EXT_INFO && name.length() > 0) {
			String path = dm.getDownloadState().getPrimaryFile();
			if (path != null) {
				int pos = path.lastIndexOf('.');
				if (pos >= 0) {
					String ext = path.substring(pos);
					Program program = Program.findProgram(ext);
					if (program != null) {
						ext += " (" + program.getName() + ")";
					}
					name += "\n"
							+ MessageText.getString("TableColumn.header.name.ext",
									new String[] {
										ext
									});
				}
			}
		}

		Graphic graphic = cell.getBackgroundGraphic();
		if (!(graphic instanceof UISWTGraphic)) {
			cell.setText(name);
		}

		Image img = ((UISWTGraphic) graphic).getImage();
		if (img == null) {
			return;
		}

		String sText = name;
		if (DataSourceUtils.isPlatformContent(dm)) {
			try {
				sText = "<A HREF=\""
						+ DataSourceUtils.getContentNetwork(dm).getContentDetailsService(
								dm.getTorrent().getHashWrapper().toBase32String(), null)
						+ "\">" + name + "</A>";
			} catch (Exception e) {
				Debug.out(e);
			}
		}

		GC gc = new GC(img);
		try {
			gc.setForeground(ColorCache.getColor(gc.getDevice(), cell.getForeground()));
			GCStringPrinter stringPrinter = new GCStringPrinter(gc, sText,
					img.getBounds(), true, true, SWT.LEFT | SWT.WRAP);
			stringPrinter.calculateMetrics();

			if (stringPrinter.hasHitUrl()) {
				TableRow row = cell.getTableRow();
				if (row instanceof TableRowCore) {
					((TableRowCore) row).setData("titleStringPrinter", stringPrinter);
				}
				int[] mouseOfs = cell.getMouseOffset();
				if (mouseOfs != null
						&& stringPrinter.getHitUrl(mouseOfs[0], mouseOfs[1]) != null) {
					stringPrinter.setUrlColor(colorLinkHover);
				} else {
					stringPrinter.setUrlColor(colorLinkNormal);
				}
			}

			stringPrinter.printString();
		} finally {
			gc.dispose();
		}

		disposeExisting(cell);
		cell.setGraphic(new UISWTGraphicImpl(img));
	}

	private void disposeExisting(TableCell cell) {
		Graphic oldGraphic = cell.getGraphic();
		//log(cell, oldGraphic);
		if (oldGraphic instanceof UISWTGraphic) {
			Image oldImage = ((UISWTGraphic) oldGraphic).getImage();
			if (oldImage != null && !oldImage.isDisposed()) {
				//log(cell, "dispose");
				cell.setGraphic(null);
				oldImage.dispose();
			}
		}
	}

	public String getObfusticatedText(TableCell cell) {
		String name = null;
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		if (dm != null) {
			name = dm.toString();
			int i = name.indexOf('#');
			if (i > 0) {
				name = name.substring(i + 1);
			}
		}

		if (name == null) {
			name = "";
		}
		return name;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener#cellMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent)
	public void cellMouseTrigger(TableCellMouseEvent event) {
		if (!(event.cell instanceof TableCellSWT)) {
			return;
		}
		TableRow row = event.cell.getTableRow();
		if (row instanceof TableRowCore) {
			GCStringPrinter stringPrinter = (GCStringPrinter) ((TableRowCore) row).getData("titleStringPrinter");
			if (stringPrinter != null) {
				URLInfo hitUrl = stringPrinter.getHitUrl(event.x, event.y);

				int oldCursorID = ((TableCellSWT) event.cell).getCursorID();
				int newCursorID = oldCursorID;
				if (hitUrl != null) {
					if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP
							&& event.button == 1) {
						if (UrlFilter.getInstance().urlIsBlocked(hitUrl.url)) {
							Utils.launch(hitUrl.url);
						} else {
							UIFunctionsSWT uif = UIFunctionsManagerSWT.getUIFunctionsSWT();
							if (uif != null) {
								uif.viewURL(hitUrl.url, SkinConstants.VIEWID_BROWSER_BROWSE, 0,
										0, false, false);
								return;
							}
						}
					}

					newCursorID = SWT.CURSOR_HAND;
				} else {
					newCursorID = SWT.CURSOR_ARROW;
				}
				if (oldCursorID != newCursorID) {
					((TableCellSWT) event.cell).setCursorID(newCursorID);
					event.cell.invalidate();
					((TableCellSWT) event.cell).refreshAsync();
				}
			}
		}
	}
	
	// @see org.gudy.azureus2.plugins.ui.tables.TableCellVisibilityListener#cellVisibilityChanged(org.gudy.azureus2.plugins.ui.tables.TableCell, int)
	public void cellVisibilityChanged(TableCell cell, int visibility) {
		if (visibility == TableCellVisibilityListener.VISIBILITY_HIDDEN) {
			disposeExisting(cell);
		}
	}
}
