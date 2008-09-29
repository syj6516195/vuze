/**
 * 
 */
package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.download.DownloadManagerEnhancer;
import com.aelitis.azureus.core.download.EnhancedDownloadManager;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.views.list.ListCell;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jun 13, 2006
 *
 */
public class ColumnProgressETA
	extends CoreTableColumn
	implements TableCellAddedListener
{
	public static final boolean TRY_NAME_COLUMN_EXPANDER = false;

	public static String COLUMN_ID = "ProgressETA";

	private static final int borderWidth = 1;

	private static final int COLUMN_WIDTH = 110;

	private static Font fontText = null;

	Display display;

	/**
	 * 
	 */
	public ColumnProgressETA(String sTableID) {
		super(COLUMN_ID, sTableID);
		initializeAsGraphic(COLUMN_WIDTH);
		setAlignment(ALIGN_LEAD);
		setMinWidth(COLUMN_WIDTH);

		display = SWTThread.getInstance().getDisplay();
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	private class Cell
		implements TableCellRefreshListener, TableCellDisposeListener,
		TableCellVisibilityListener
	{
		int lastPercentDone = 0;

		long lastETA;

		private boolean bMouseDowned = false;

		public Cell(TableCell cell) {
			cell.addListeners(this);
			cell.setMarginHeight(0);
			//cell.setFillCell(true);
		}

		public void dispose(TableCell cell) {
			disposeExisting(cell);
		}

		public void refresh(TableCell cell) {
			refresh(cell, false);
		}

		private void refresh(final TableCell cell, final boolean bForce) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null) {
				return;
			}

			if (!Utils.isThisThreadSWT()) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						refresh(cell, bForce);
					}
				});
				return;
			}

			TOTorrent torrent = dm.getTorrent();

			int percentDone = getPercentDone(cell);
			long eta = getETA(cell);

			long sortValue = 0;

			long completedTime = dm.getDownloadState().getLongParameter(
					DownloadManagerState.PARAM_DOWNLOAD_COMPLETED_TIME);
			if (completedTime <= 0) {
				sortValue = dm.getDownloadState().getLongParameter(
						DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME) * 10000;
				sortValue += 1000 - percentDone;
			} else {
				sortValue = completedTime;
			}

			if (!cell.setSortValue(sortValue) && !bForce && cell.isValid()
					&& lastPercentDone == percentDone && lastETA == eta) {
				return;
			}

			if (TRY_NAME_COLUMN_EXPANDER) {
				if (dm.getAssumedComplete()) {
					//System.out.println(percentDone + ";" + lastPercentDone + ";" + Debug.getCompressedStackTrace());
					try {
						TableCellSWT cellTitle = (TableCellSWT) cell.getTableRow().getTableCell(
								"name");
						ListCell itemTitle = (ListCell) cellTitle.getBufferedTableItem();
						ListCell listCell = (ListCell) ((TableCellSWT) cell).getBufferedTableItem();
						Rectangle bounds = listCell.getBounds();
						if (bounds != null) {
							//itemTitle.setSecretWidth(cellTitle.getTableColumn().getWidth() + bounds.width);
							disposeExisting(cell);
							lastPercentDone = percentDone;
							listCell.setBounds(null);
						}
						return;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			if (!bForce && !cell.isShown()) {
				return;
			}

			//Compute bounds ...
			int newWidth = cell.getWidth();
			if (newWidth <= 0) {
				return;
			}
			int newHeight = cell.getHeight();

			int x1 = borderWidth;
			int y1 = borderWidth;
			int x2 = newWidth - x1 - borderWidth;
			int progressX2 = x2;
			int progressY2 = newHeight - y1 - borderWidth - 12;
			if (progressY2 > 18) {
				progressY2 = 18;
			}
			boolean showSecondLine = progressY2 > 0;
			if (!showSecondLine) {
				progressY2 = newHeight;
			}
			
			if (x2 < 10 || progressX2 < 10) {
				return;
			}

			SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
			Color cBG = skinProperties.getColor("color.progress.bg");
			if (cBG == null) {
				cBG = Colors.blues[Colors.BLUES_LIGHTEST];
			}
			Color cFG = skinProperties.getColor("color.progress.fg");
			if (cFG == null) {
				cFG = Colors.blues[Colors.BLUES_MIDLIGHT];
			}
			Color cBorder = skinProperties.getColor("color.progress.border");
			if (cBorder == null) {
				cBorder = Colors.grey;
			}
			Color cText = skinProperties.getColor("color.progress.text");
			if (cText == null) {
				cText = Colors.black;
			}

			lastPercentDone = percentDone;
			lastETA = eta;

			boolean bDrawProgressBar = true;
			Graphic graphic = cell.getBackgroundGraphic();
			Image image = null;
			if (graphic instanceof UISWTGraphic) {
				image = ((UISWTGraphic) graphic).getImage();
			}
			GC gcImage;
			boolean bImageSizeChanged;
			Rectangle imageBounds;
			if (image == null) {
				bImageSizeChanged = true;
			} else {
				imageBounds = image.getBounds();
				bImageSizeChanged = imageBounds.width != newWidth
						|| imageBounds.height != newHeight;
			}

			if (false) {
				log(cell, "building: " + cell.isValid() + ";"
						+ (lastPercentDone == percentDone) + ";" + (lastETA == eta)
						+ "; oldimg=" + (image != null));
			}

			boolean isNewImage = newHeight > 36 || image == null;
			if (isNewImage) {
				if (image != null) {
					image.dispose();
				}
				image = new Image(display, newWidth, Math.min(36, newHeight));
			}
			imageBounds = image.getBounds();

			gcImage = new GC(image);
			if (isNewImage) {
				Color background = ColorCache.getColor(display, cell.getBackground());
				if (background != null) {
					gcImage.setBackground(background);
					gcImage.fillRectangle(imageBounds);
				}
			}

			String sETALine = null;
			long lSpeed = getSpeed(dm);
			String sSpeed = lSpeed <= 0 ? "" : "("
					+ DisplayFormatters.formatByteCountToKiBEtcPerSec(lSpeed, true) + ")";

			if (bDrawProgressBar && percentDone < 1000) {
				if (bImageSizeChanged || true) {
					// draw border
					gcImage.setForeground(cBorder);
					gcImage.drawRectangle(0, 0, progressX2 - x1 + 1, progressY2 - y1 + 1);
				} else {
					gcImage = new GC(image);
				}

				int limit = ((progressX2 - x1) * percentDone) / 1000;

				gcImage.setBackground(cBG);
				gcImage.fillRectangle(x1, y1, limit, progressY2 - y1);
				if (limit < progressX2) {
					gcImage.setBackground(cFG);
					gcImage.fillRectangle(limit + 1, y1, progressX2 - limit - 1,
							progressY2 - y1);
				}

			}

			if (sETALine == null) {
				//if (isStopped(cell)) {
				//sETALine = DisplayFormatters.formatDownloadStatus((DownloadManager) cell.getDataSource());
				//} else
				if (dm.isDownloadComplete(true)) {
					sETALine = DisplayFormatters.formatByteCountToKiBEtc(dm.getSize());
				} else if (eta > 0) {
					String sETA = TimeFormatter.format(eta);
					sETALine = MessageText.getString(
							"MyTorrents.column.ColumnProgressETA.2ndLine", new String[] {
								sETA
							});
				} else {
					sETALine = DisplayFormatters.formatDownloadStatus(dm);
					//sETALine = "";
				}
			}

			if (fontText == null) {
				fontText = Utils.getFontWithHeight(gcImage.getFont(), gcImage, 12);
			}

			if (showSecondLine) {
  			gcImage.setFont(fontText);
  			int[] fg = cell.getForeground();
  			gcImage.setForeground(ColorCache.getColor(display, fg[0], fg[1], fg[2]));
  			gcImage.drawText(sETALine, 2, progressY2, true);
  			Point textExtent = gcImage.textExtent(sETALine);
  			cell.setToolTip(textExtent.x > newWidth ? sETALine : null);
			}
			int middleY = (progressY2 - 12) / 2;
			if (percentDone == 1000) {
				gcImage.setForeground(cText);
				gcImage.drawText("Complete", 2, middleY, true);
			} else if (bDrawProgressBar) {
				gcImage.setForeground(cText);
				String sPercent = DisplayFormatters.formatPercentFromThousands(percentDone);
				gcImage.drawText(sSpeed, 50, middleY, true);
				gcImage.drawText(sPercent, 2, middleY, true);
			}
  
			gcImage.setFont(null);

			gcImage.dispose();

			disposeExisting(cell);

			if (cell instanceof TableCellSWT) {
				((TableCellSWT) cell).setGraphic(image);
			} else {
				cell.setGraphic(new UISWTGraphicImpl(image));
			}
		}

		private int getPercentDone(TableCell cell) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null) {
				return 0;
			}
			return dm.getStats().getDownloadCompleted(true);
		}

		private long getETA(TableCell cell) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null) {
				return Constants.INFINITY_AS_INT;
			}
			return dm.getStats().getETA();
		}

		private int getState(TableCell cell) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null) {
				return DownloadManager.STATE_ERROR;
			}
			return dm.getState();
		}

		private boolean isStopped(TableCell cell) {
			int state = getState(cell);
			return state == DownloadManager.STATE_QUEUED
					|| state == DownloadManager.STATE_STOPPED
					|| state == DownloadManager.STATE_STOPPING
					|| state == DownloadManager.STATE_ERROR;
		}

		private long getSpeed(DownloadManager dm) {
			if (dm == null) {
				return 0;
			}

			return dm.getStats().getDataReceiveRate();
		}

		public EnhancedDownloadManager getEDM(DownloadManager dm) {
			DownloadManagerEnhancer dmEnhancer = DownloadManagerEnhancer.getSingleton();
			if (dmEnhancer == null) {
				return null;
			}
			return dmEnhancer.getEnhancedDownload(dm);
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

		public void cellVisibilityChanged(TableCell cell, int visibility) {
			if (visibility == TableCellVisibilityListener.VISIBILITY_HIDDEN) {
				//log(cell, "whoo, save");
				disposeExisting(cell);
			} else if (visibility == TableCellVisibilityListener.VISIBILITY_SHOWN) {
				//log(cell, "whoo, draw");
				refresh(cell, true);
			}
		}

		private void log(TableCell cell, String s) {
			System.out.println(((TableRowCore) cell.getTableRow()).getIndex() + ":"
					+ System.currentTimeMillis() + ": " + s);
		}
	}
}
