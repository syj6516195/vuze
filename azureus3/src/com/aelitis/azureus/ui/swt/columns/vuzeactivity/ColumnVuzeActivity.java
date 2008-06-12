/**
 * Created on Feb 2, 2008 
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

package com.aelitis.azureus.ui.swt.columns.vuzeactivity;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter.URLInfo;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableCellImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.activities.VuzeActivitiesConstants;
import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.skin.SkinConstants;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.columns.torrent.ColumnMediaThumb;
import com.aelitis.azureus.ui.swt.columns.torrent.ColumnRate;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.utils.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;
import com.aelitis.azureus.ui.swt.views.list.*;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.DataSourceUtils;
import com.aelitis.azureus.util.StringCompareUtils;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Feb 2, 2008
 *
 */
public class ColumnVuzeActivity
	extends CoreTableColumn
	implements TableCellRefreshListener, TableCellDisposeListener,
	TableCellAddedListener, TableCellMouseMoveListener,
	TableCellVisibilityListener, TableCellToolTipListener
{
	private static final int MARGIN_WIDTH = 8 - ListView.COLUMN_MARGIN_WIDTH;

	private static final int EVENT_INDENT = 8 + 16 + 5 - MARGIN_WIDTH;

	private static final int MARGIN_HEIGHT = 7 - 1; // we set row margin height to 1

	public static String COLUMN_ID = "name";

	private static Font headerFont = null;

	private static Font vuzeNewsFont = null;

	private static SimpleDateFormat timeFormat = new SimpleDateFormat(
			"h:mm:ss a, EEEE, MMMM d, yyyy");

	private Color colorLinkNormal;

	private Color colorLinkHover;

	private Color colorHeaderFG;

	private Color colorNewsFG;

	private Color colorNewsBG;

	private static Image imgDelete;

	/** Default Constructor */
	public ColumnVuzeActivity(String sTableID) {
		super(COLUMN_ID, POSITION_LAST, 250, sTableID);
		setObfustication(true);
		setType(TableColumn.TYPE_GRAPHIC);
		setRefreshInterval(INTERVAL_LIVE);

		SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
		colorLinkNormal = skinProperties.getColor("color.links.normal");
		colorLinkHover = skinProperties.getColor("color.links.hover");
		colorHeaderFG = skinProperties.getColor("color.activity.row.header.fg");
		colorNewsBG = skinProperties.getColor("color.vuze-entry.news.bg");
		colorNewsFG = skinProperties.getColor("color.vuze-entry.news.fg");

		//imgDelete = ImageRepository.getImage("progress_remove"); 
	}

	public void cellAdded(TableCell cell) {
		cell.setMarginWidth(MARGIN_WIDTH);
		cell.setMarginHeight(0);
		cell.setFillCell(true);

		Object ds = cell.getDataSource();
		if (!(ds instanceof VuzeActivitiesEntry)) {
			return;
		}
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) ds;
		entry.tableColumn = (TableColumnCore) cell.getTableColumn();
		//((TableCellCore) cell).getTableRowCore().setHeight(
		//		(int) (Math.random() * 30) + 20);
	}

	public void refresh(TableCell cell) {
		TableCellImpl thumbCell = getThumbCell(cell);
		TableCellImpl ratingCell = getRatingCell(cell);
		boolean force = !cell.isValid();
		if (thumbCell != null
				&& (!thumbCell.isValid() || thumbCell.getVisuallyChangedSinceRefresh())) {
			force = true;
		}
		if (ratingCell != null
				&& (!ratingCell.isValid() || ratingCell.getVisuallyChangedSinceRefresh())) {
			force = true;
		}

		Object ds = cell.getDataSource();
		if (!(ds instanceof VuzeActivitiesEntry)) {
			return;
		}
		VuzeActivitiesEntry entry = (VuzeActivitiesEntry) ds;

		int width = cell.getWidth();
		int height = cell.getHeight();
		if (width <= 0 || height <= 0) {
			cell.setSortValue(entry.getTimestamp());
			return;
		}

		boolean canShowThumb = canShowThumb(entry);

		Image image = null;

		Graphic graphic = cell.getGraphic();
		if (graphic instanceof UISWTGraphic) {
			image = ((UISWTGraphic) graphic).getImage();
		}

		Rectangle imgBounds = image == null ? null : image.getBounds();
		force |= imgBounds == null
				|| !imgBounds.equals(new Rectangle(0, 0, width, height));

		if (!cell.setSortValue(entry) && cell.isValid() && !force) {
			return;
		}
		((TableColumnCore) cell.getTableColumn()).setSortValueLive(false);

		if (force) {
			graphic = cell.getBackgroundGraphic();
			if (graphic instanceof UISWTGraphic) {
				image = ((UISWTGraphic) graphic).getImage();
				if (image == null) {
					return;
				}
			}
			imgBounds = image.getBounds();
		}

		Image imgIcon;
		if (entry.getIconID() == null) {
			imgIcon = null;
		} else {
			ImageLoader imageLoader = ImageLoaderFactory.getInstance();
			imgIcon = imageLoader.getImage(entry.getIconID());
			if (!ImageLoader.isRealImage(imgIcon)) {
				imgIcon = null;
			}
		}

		boolean isHeader = VuzeActivitiesConstants.TYPEID_HEADER.equals(entry.getTypeID());
		int x = isHeader ? 0 : EVENT_INDENT;
		int y = 0;

		boolean isVuzeNewsEntry = !isHeader
				&& VuzeActivitiesConstants.TYPEID_VUZENEWS.equalsIgnoreCase(entry.getTypeID());

		int style = SWT.WRAP;
		Device device = Display.getDefault();
		GCStringPrinter stringPrinter;
		GC gcQuery = new GC(device);
		Rectangle drawRect;
		try {
			try {
				gcQuery.setAdvanced(true);
				gcQuery.setTextAntialias(SWT.ON);
			} catch (Exception e) {
				// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
			}
			if (isHeader) {
				if (headerFont == null) {
					// no sync required, SWT is on single thread
					FontData[] fontData = gcQuery.getFont().getFontData();
					fontData[0].setStyle(SWT.BOLD);
					// we can do a few more pixels because we have no text hanging below baseline
					if (!Constants.isUnix) {
						fontData[0].setName("Arial");
					}
					Utils.getFontHeightFromPX(device, fontData, gcQuery, 17);
					headerFont = new Font(device, fontData);
				}
				gcQuery.setFont(headerFont);
			} else if (isVuzeNewsEntry) {
				if (vuzeNewsFont == null) {
					FontData[] fontData = gcQuery.getFont().getFontData();
					fontData[0].setStyle(SWT.BOLD);
					vuzeNewsFont = new Font(device, fontData);
				}
				gcQuery.setFont(vuzeNewsFont);
			}
			Rectangle potentialArea = new Rectangle(x, 2, width - x - 4, 10000);
			stringPrinter = new GCStringPrinter(gcQuery, entry.getText(),
					potentialArea, 0, SWT.WRAP | SWT.TOP);
			stringPrinter.calculateMetrics();
			Point size = stringPrinter.getCalculatedSize();
			//System.out.println(size + ";" + entry.text);

			//boolean focused = ((ListRow) cell.getTableRow()).isFocused();
			//if (focused) size.y += 40;
			if (isHeader) {
				height = 35 - 2;
				y = 8;
			} else {
				height = size.y;
				height += (MARGIN_HEIGHT * 2);
				y = MARGIN_HEIGHT + 1;
			}
			style |= SWT.TOP;

			if (canShowThumb) {
				height += 60;
			}

			if (height < 30) {
				height = 30;
			}
			boolean heightChanged = ((TableCellCore) cell).getTableRowCore().setDrawableHeight(
					height);

			if (heightChanged) {
				disposeExisting(cell, null);
				graphic = cell.getBackgroundGraphic();
				if (graphic instanceof UISWTGraphic) {
					image = ((UISWTGraphic) graphic).getImage();
					if (image == null) {
						return;
					}
				}
				imgBounds = image.getBounds();
			}

			drawRect = new Rectangle(x, y, width - x - 4, height - y + MARGIN_HEIGHT);
			stringPrinter = new GCStringPrinter(gcQuery, entry.getText(), drawRect,
					0, SWT.WRAP | SWT.TOP);
			stringPrinter.calculateMetrics();

			if (stringPrinter.hasHitUrl()) {
				URLInfo[] hitUrlInfo = stringPrinter.getHitUrlInfo();
				for (int i = 0; i < hitUrlInfo.length; i++) {
					URLInfo info = hitUrlInfo[i];
					info.urlColor = colorLinkNormal;
				}
				int[] mouseOfs = cell.getMouseOffset();
				if (mouseOfs != null) {
					URLInfo hitUrl = stringPrinter.getHitUrl(mouseOfs[0] - MARGIN_WIDTH,
							mouseOfs[1]);
					if (hitUrl != null) {
						hitUrl.urlColor = colorLinkHover;
					}
				}
			}

			//System.out.println("height=" + height + ";b=" + imgBounds);
		} finally {
			gcQuery.dispose();
		}

		GC gc = new GC(image);
		try {
			try {
				gc.setAdvanced(true);
				gc.setTextAntialias(SWT.ON);
			} catch (Exception e) {
				// Ignore ERROR_NO_GRAPHICS_LIBRARY error or any others
			}
			gc.setBackground(ColorCache.getColor(device, cell.getBackground()));
			gc.setForeground(ColorCache.getColor(device, cell.getForeground()));

			if (isHeader) {
				gc.setFont(headerFont);
				gc.setForeground(colorHeaderFG);

				gc.fillRectangle(imgBounds);
				height = height - 5;
			} else {
				if (isVuzeNewsEntry) {
					if (colorNewsBG != null) {
						gc.setBackground(colorNewsBG);
						ListRow row = (ListRow) cell.getTableRow();
						row.setBackgroundColor(colorNewsBG);
					}
					if (colorNewsFG != null) {
						gc.setForeground(colorNewsFG);
					}
					gc.setFont(vuzeNewsFont);
				} else {
					TableRow row = cell.getTableRow();
					gc.setBackground(((ListRow) row).getBackground());
				}

				gc.fillRectangle(imgBounds);
				if (imgIcon != null) {
					Rectangle iconBounds = imgIcon.getBounds();
					gc.drawImage(imgIcon, iconBounds.x, iconBounds.y, iconBounds.width,
							iconBounds.height, 0, MARGIN_HEIGHT, 16, 16);
				}
			}

			stringPrinter.printString(gc, drawRect, style);
			entry.urlInfo = stringPrinter;

			if (canShowThumb) {
				Rectangle dmThumbRect = getDMImageRect(height);
				if (thumbCell == null) {
					ListCell listCell = new ListCellGraphic((ListRow) cell.getTableRow(),
							SWT.LEFT, dmThumbRect);

					thumbCell = new TableCellImpl((TableRowCore) cell.getTableRow(),
							new ColumnMediaThumb(cell.getTableID(), -1), 0, listCell);
					listCell.setTableCell(thumbCell);

					((TableCellImpl) cell).addChildCell(thumbCell);
					listCell.setParentCell((TableCellSWT) cell);

					setThumbCell(cell, thumbCell);
				}

				((ListCell) thumbCell.getBufferedTableItem()).setBackground(gc.getBackground());
				((ListCell) thumbCell.getBufferedTableItem()).setBounds(dmThumbRect);
				invalidateAndRefresh(thumbCell);

				if (VuzeActivitiesConstants.TYPEID_RATING_REMINDER.equals(entry.getTypeID())) {
					if (canShowThumb && DataSourceUtils.isPlatformContent(ds)) {
						Rectangle dmRatingRect = getDMRatingRect(width, height);
						if (ratingCell == null) {
							ListCell listCell = new ListCellGraphic(
									(ListRow) cell.getTableRow(), SWT.RIGHT, dmRatingRect);

							ratingCell = new TableCellImpl((TableRowCore) cell.getTableRow(),
									new ColumnRate(cell.getTableID(), true), 0, listCell);
							listCell.setTableCell(ratingCell);

							((TableCellImpl) cell).addChildCell(ratingCell);
							listCell.setParentCell((TableCellSWT) cell);

							setRatingCell(cell, ratingCell);
						}

						((ListCell) ratingCell.getBufferedTableItem()).setBackground(gc.getBackground());
						((ListCell) ratingCell.getBufferedTableItem()).setBounds(dmRatingRect);
						invalidateAndRefresh(ratingCell);
					}
				}
			}

			if (!isHeader && ((TableCellCore) cell).isMouseOver()
					&& imgDelete != null) {
				gc.setAlpha(50);
				gc.drawImage(imgDelete, width - imgDelete.getBounds().width - 5, 2);
			}
		} finally {
			gc.dispose();
		}

		disposeExisting(cell, image);

		cell.setGraphic(new UISWTGraphicImpl(image));
	}

	/**
	 * @param entry
	 * @return
	 *
	 * @since 3.0.5.3
	 */
	private boolean canShowThumb(VuzeActivitiesEntry entry) {
		if (!entry.getShowThumb()) {
			return false;
		}

		return entry.getDownloadManger() != null || entry.getTorrent() != null
				|| entry.getImageBytes() != null;
	}

	private void disposeExisting(TableCell cell, Image exceptIfThisImage) {
		Graphic oldGraphic = cell.getGraphic();
		//log(cell, oldGraphic);
		if (oldGraphic instanceof UISWTGraphic) {
			Image oldImage = ((UISWTGraphic) oldGraphic).getImage();
			if (oldImage != null && !oldImage.isDisposed()
					&& oldImage != exceptIfThisImage) {
				//log(cell, "dispose");
				cell.setGraphic(null);
				oldImage.dispose();
			}
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener#dispose(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void dispose(TableCell cell) {
		disposeExisting(cell, null);
		try {
			TableCellImpl thumbCell = getThumbCell(cell);
			if (thumbCell != null) {
				thumbCell.dispose();
				setThumbCell(cell, null);
			}
		} catch (Exception e) {
			Debug.out(e);
		}
		try {
			TableCellImpl ratingCell = getRatingCell(cell);
			if (ratingCell != null) {
				ratingCell.dispose();
				setRatingCell(cell, null);
			}
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	private void setThumbCell(TableCell cell, TableCellImpl thumbCell) {
		TableRow tableRow = cell.getTableRow();
		if (tableRow instanceof TableRowCore) {
			TableRowCore tableRowCore = (TableRowCore) tableRow;
			tableRowCore.setData("ThumbCell", thumbCell);
		}
	}

	private TableCellImpl getThumbCell(TableCell cell) {
		TableRow tableRow = cell.getTableRow();
		if (tableRow instanceof TableRowCore) {
			TableRowCore tableRowCore = (TableRowCore) tableRow;
			return (TableCellImpl) tableRowCore.getData("ThumbCell");
		}
		return null;
	}

	private boolean getIsMouseOverCell(String id, TableCell cell) {
		TableRow tableRow = cell.getTableRow();
		if (tableRow instanceof TableRowCore) {
			TableRowCore tableRowCore = (TableRowCore) tableRow;
			Object data = tableRowCore.getData("IsMouseOver" + id + "Cell");
			if (data instanceof Boolean) {
				return ((Boolean) data).booleanValue();
			}
		}
		return false;
	}

	private boolean setIsMouseOverCell(String id, TableCell cell, boolean b) {
		TableRow tableRow = cell.getTableRow();
		if (tableRow instanceof TableRowCore) {
			TableRowCore tableRowCore = (TableRowCore) tableRow;
			tableRowCore.setData("IsMouseOver" + id + "Cell", new Boolean(b));
		}
		return false;
	}

	private void setRatingCell(TableCell cell, TableCellImpl ratingCell) {
		TableRow tableRow = cell.getTableRow();
		if (tableRow instanceof TableRowCore) {
			TableRowCore tableRowCore = (TableRowCore) tableRow;
			tableRowCore.setData("RatingCell", ratingCell);
		}
	}

	private TableCellImpl getRatingCell(TableCell cell) {
		TableRow tableRow = cell.getTableRow();
		if (tableRow instanceof TableRowCore) {
			TableRowCore tableRowCore = (TableRowCore) tableRow;
			return (TableCellImpl) tableRowCore.getData("RatingCell");
		}
		return null;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener#cellHover(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellHover(TableCell cell) {
		TableCellImpl thumbCell = getThumbCell(cell);
		if (thumbCell != null) {
			if (getIsMouseOverCell("Thumb", cell)) {
				Object toolTip = thumbCell.getToolTip();
				if (toolTip != null) {
					cell.setToolTip(toolTip);
				}
			}
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellToolTipListener#cellHoverComplete(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void cellHoverComplete(TableCell cell) {
		TableCellImpl thumbCell = getThumbCell(cell);
		if (thumbCell != null) {
			if (getIsMouseOverCell("Thumb", cell)) {
				Object toolTip = thumbCell.getToolTip();
				if (toolTip != null) {
					cell.setToolTip(null);
				}
			}
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener#cellMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent)
	public void cellMouseTrigger(TableCellMouseEvent event) {
		String tooltip = null;

		boolean invalidateAndRefresh = false;

		TableCellImpl thumbCell = getThumbCell(event.cell);
		TableCellImpl ratingCell = getRatingCell(event.cell);
		if (thumbCell != null || ratingCell != null) {
			Rectangle dmThumbRect = getDMImageRect(event.cell.getHeight());
			Rectangle dmRatingRect = getDMRatingRect(event.cell.getWidth(),
					event.cell.getHeight());

			TableCellMouseEvent subCellEvent = new TableCellMouseEvent();
			subCellEvent.button = event.button;
			subCellEvent.data = event.data;
			subCellEvent.eventType = event.eventType;
			subCellEvent.row = event.row;

			boolean isMouseOverThumbCell = false;
			if (thumbCell != null) {
				isMouseOverThumbCell = dmThumbRect.contains(event.x, event.y);
				boolean wasMouseOverThumbCell = getIsMouseOverCell("Thumb", event.cell);
				//System.out.println("was=" + wasMouseOverThumbCell + ";is=" + isMouseOverThumbCell);

				if (wasMouseOverThumbCell != isMouseOverThumbCell) {
					invalidateAndRefresh = true;
					setIsMouseOverCell("Thumb", event.cell, isMouseOverThumbCell);
					subCellEvent.eventType = isMouseOverThumbCell
							? TableCellMouseEvent.EVENT_MOUSEENTER
							: TableCellMouseEvent.EVENT_MOUSEEXIT;
					subCellEvent.x = event.x - dmThumbRect.x - MARGIN_WIDTH;
					subCellEvent.y = event.y - dmThumbRect.y - MARGIN_WIDTH;
					subCellEvent.cell = thumbCell;

					TableColumn tc = thumbCell.getTableColumn();
					if (tc instanceof TableColumnCore) {
						((TableColumnCore) tc).invokeCellMouseListeners(subCellEvent);
					}
					if (thumbCell instanceof TableCellCore) {
						((TableCellCore) thumbCell).invokeMouseListeners(subCellEvent);
					}
					event.skipCoreFunctionality |= subCellEvent.skipCoreFunctionality;

					subCellEvent.eventType = TableCellMouseEvent.EVENT_MOUSEMOVE;
				}
			}

			boolean isMouseOverRatingCell = false;
			if (ratingCell != null) {
				isMouseOverRatingCell = dmRatingRect.contains(event.x, event.y);
				boolean wasMouseOverRatingCell = getIsMouseOverCell("Rating",
						event.cell);
				if (wasMouseOverRatingCell != isMouseOverRatingCell) {
					invalidateAndRefresh = true;
					setIsMouseOverCell("Rating", event.cell, isMouseOverRatingCell);
					subCellEvent.eventType = isMouseOverRatingCell
							? TableCellMouseEvent.EVENT_MOUSEENTER
							: TableCellMouseEvent.EVENT_MOUSEEXIT;
					subCellEvent.cell = ratingCell;
					subCellEvent.x = event.x - dmRatingRect.x - MARGIN_WIDTH;
					subCellEvent.y = event.y - dmRatingRect.y - MARGIN_WIDTH;

					TableColumn tc = ratingCell.getTableColumn();
					if (tc instanceof TableColumnCore) {
						((TableColumnCore) tc).invokeCellMouseListeners(subCellEvent);
					}
					if (ratingCell instanceof TableCellCore) {
						((TableCellCore) ratingCell).invokeMouseListeners(subCellEvent);
					}
					event.skipCoreFunctionality |= subCellEvent.skipCoreFunctionality;
				}
			}

			if (thumbCell != null && isMouseOverThumbCell) {
				subCellEvent.cell = thumbCell;
				subCellEvent.x = event.x - dmThumbRect.x - MARGIN_WIDTH;
				subCellEvent.y = event.y - dmThumbRect.y - MARGIN_WIDTH;

				if (thumbCell instanceof TableCellCore) {
					TableRowCore row = ((TableCellCore) thumbCell).getTableRowCore();
					if (row != null) {
						row.invokeMouseListeners(subCellEvent);
					}
					((TableCellCore) thumbCell).invokeMouseListeners(subCellEvent);
					TableColumn tc = thumbCell.getTableColumn();
					if (tc instanceof TableColumnCore) {
						((TableColumnCore) tc).invokeCellMouseListeners(subCellEvent);
					}
				}
				event.skipCoreFunctionality |= subCellEvent.skipCoreFunctionality;
			}
			if (ratingCell != null && isMouseOverRatingCell) {
				subCellEvent.cell = ratingCell;
				subCellEvent.x = event.x - dmRatingRect.x - MARGIN_WIDTH;
				subCellEvent.y = event.y - dmRatingRect.y - MARGIN_WIDTH;

				if (ratingCell instanceof TableCellCore) {
					((TableCellCore) ratingCell).getTableRowCore().invokeMouseListeners(
							subCellEvent);
					((TableCellCore) ratingCell).invokeMouseListeners(subCellEvent);
				}
				TableColumn tc = ratingCell.getTableColumn();
				if (tc instanceof TableColumnCore) {
					((TableColumnCore) tc).invokeCellMouseListeners(subCellEvent);
				}
				event.skipCoreFunctionality |= subCellEvent.skipCoreFunctionality;
			}
		}

		Comparable sortValue = event.cell.getSortValue();
		if (sortValue instanceof VuzeActivitiesEntry) {
			VuzeActivitiesEntry entry = (VuzeActivitiesEntry) sortValue;
			if (entry.urlInfo != null) {
				//((ListView)((ListRow)event.cell.getTableRow()).getView()).getTableCellCursorOffset()
				//System.out.println(entry.urlHitArea);
				URLInfo hitUrl = ((GCStringPrinter) entry.urlInfo).getHitUrl(event.x
						- MARGIN_WIDTH, event.y);
				int newCursor;
				if (hitUrl != null) {
					if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP) {
						if (!PlatformConfigMessenger.urlCanRPC(hitUrl.url)) {
							Utils.launch(hitUrl.url);
						} else {
							UIFunctionsSWT uif = UIFunctionsManagerSWT.getUIFunctionsSWT();
							if (uif != null) {
								String target = hitUrl.target == null
										? SkinConstants.VIEWID_BROWSER_BROWSE : hitUrl.target;
								uif.viewURL(hitUrl.url, target, 0, 0, false, false);
								return;
							}
						}
					}

					newCursor = SWT.CURSOR_HAND;
					if (PlatformConfigMessenger.urlCanRPC(hitUrl.url)) {
						tooltip = hitUrl.title;
					} else {
						tooltip = hitUrl.url;
					}
					//tooltip = hitUrl.url;
				} else {
					newCursor = SWT.CURSOR_ARROW;
				}

				int oldCursor = ((TableCellSWT) event.cell).getCursorID();
				if (oldCursor != newCursor) {
					invalidateAndRefresh = true;
					((TableCellSWT) event.cell).setCursorID(newCursor);
				}
			}
		}

		Object ds = event.cell.getDataSource();
		if (ds instanceof VuzeActivitiesEntry) {
			VuzeActivitiesEntry entry = (VuzeActivitiesEntry) ds;
			boolean inHitArea = new Rectangle(0, 0, EVENT_INDENT, EVENT_INDENT).contains(
					event.x, event.y);

			boolean isHeader = VuzeActivitiesConstants.TYPEID_HEADER.equals(entry.getTypeID());
			if (!isHeader && inHitArea) {
				String ts = timeFormat.format(new Date(entry.getTimestamp()));
				tooltip = "Activity occurred on " + ts;
			}
		}

		Object o = event.cell.getToolTip();
		if ((o == null) | (o instanceof String)) {
			String oldTooltip = (String) o;
			if (!StringCompareUtils.equals(oldTooltip, tooltip)) {
				invalidateAndRefresh = true;
				event.cell.setToolTip(tooltip);
			}
		}

		if (invalidateAndRefresh) {
			invalidateAndRefresh(event.cell);
		}
	}

	private void invalidateAndRefresh(TableCell cell) {
		cell.invalidate();
		if (cell instanceof TableCellCore) {
			TableCellCore cellCore = (TableCellCore) cell;
			cellCore.refreshAsync();
		}
	}

	private Rectangle getDMImageRect(int cellHeight) {
		//return new Rectangle(0, cellHeight - 50 - MARGIN_HEIGHT, 16, 50);
		return new Rectangle(EVENT_INDENT, cellHeight - 50 - MARGIN_HEIGHT, 88, 50);
	}

	private Rectangle getDMRatingRect(int cellWidth, int cellHeight) {
		return new Rectangle(cellWidth - 80 - 10, cellHeight - 42 - MARGIN_HEIGHT,
				80, 38);
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellVisibilityListener#cellVisibilityChanged(org.gudy.azureus2.plugins.ui.tables.TableCell, int)
	public void cellVisibilityChanged(TableCell cell, int visibility) {
		TableCellImpl thumbCell = getThumbCell(cell);
		if (thumbCell != null) {
			thumbCell.invokeVisibilityListeners(visibility, true);
		}
		TableCellImpl ratingCell = getRatingCell(cell);
		if (ratingCell != null) {
			ratingCell.invokeVisibilityListeners(visibility, true);
		}
	}
}
