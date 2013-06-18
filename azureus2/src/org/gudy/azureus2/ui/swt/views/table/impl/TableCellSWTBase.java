package org.gudy.azureus2.ui.swt.views.table.impl;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.views.table.*;

import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

public abstract class TableCellSWTBase
	implements TableCellSWT
{
	private static final LogIDs LOGID = LogIDs.GUI;

	private static AEMonitor this_mon = new AEMonitor("TableCell");

	/**
	 * Plugins read this to see if their datasource has changed.
	 * {@link #invalidate()} will clear this flag, causing the cell to set its ui again
	 */
	protected static final int FLAG_VALID = 1;

	/**
	 * Indicates if the sort value is also the text displayed.  We can optimize.
	 */
	protected static final int FLAG_SORTVALUEISTEXT = 2;

	/**
	 * Indicates if the tooltip is autogenerated
	 */
	protected static final int FLAG_TOOLTIPISAUTO = 4;

	/**
	 * For refreshing, this flag manages whether the row is actually up to date.
	 * 
	 * We don't update any visuals while the row isn't visible.  But, validility
	 * does get set to true so that the cell isn't forced to refresh every
	 * cycle when not visible.  (We can't just never call refresh when the row
	 * is not visible, as refresh also sets the sort value)
	 *  
	 * When the row does become visible, we have to invalidate the row so
	 * that the row will set its visuals again (this time, actually
	 * updating a viewable object).
	 */
	protected static final int FLAG_UPTODATE = 8;

	protected static final int FLAG_DISPOSED = 16;

	/**
	 * Cell has been invalidated, it must refresh on next cycle
	 */
	protected static final int FLAG_MUSTREFRESH = 32;

	/**
	 * If any visuals change between 2 refreshes, this flag gets set
	 */
	public static final int FLAG_VISUALLY_CHANGED_SINCE_REFRESH = 64;

	private static final boolean DEBUGONLYZERO = false;

	private static final boolean DEBUG_FLAGS = false;

	private int flags;

	protected TableRowCore tableRow;

	protected TableColumnCore tableColumn;

	private byte tooltipErrLoopCount;

	public boolean bDebug = false;

	protected ArrayList<TableCellRefreshListener> refreshListeners;

	private ArrayList<TableCellDisposeListener> disposeListeners;

	private ArrayList<TableCellToolTipListener> tooltipListeners;

	private ArrayList<TableCellMouseListener> cellMouseListeners;

	private ArrayList<TableCellMouseMoveListener> cellMouseMoveListeners;

	private ArrayList<TableCellVisibilityListener> cellVisibilityListeners;

	protected ArrayList<TableCellSWTPaintListener> cellSWTPaintListeners;

	private ArrayList<TableCellClipboardListener> cellClipboardListeners;

	protected Comparable sortValue;

	private byte restartRefresh = 0;

	private boolean bInRefreshAsync = false;

	private byte refreshErrLoopCount;

	private byte loopFactor;

	// TODO: private
	protected static int MAX_REFRESHES = 10;

	private static int MAX_REFRESHES_WITHIN_MS = 100;

	private boolean bInRefresh = false;

	private long lastRefresh;

	// TODO: Private
	protected int numFastRefreshes;

	private Object oToolTip;

	private Object defaultToolTip;

	private int textAlpha = 255;

	private boolean doFillCell = false;

	private int iCursorID = SWT.CURSOR_ARROW;

	private boolean mouseOver;

	private Image icon;
	
	private Graphic graphic = null;

	public TableCellSWTBase(TableRowCore row, TableColumnCore column) {
		flags = FLAG_SORTVALUEISTEXT;
		tableRow = row;
		tableColumn = column;
		tooltipErrLoopCount = 0;
		refreshErrLoopCount = 0;
		loopFactor = 0;
		if (column != null && column.getType() == TableColumnCore.TYPE_GRAPHIC) {
			setMarginHeight(1);
			setMarginWidth(1);
		}
	}

	public void addRefreshListener(TableCellRefreshListener listener) {
		try {
			this_mon.enter();

			if (refreshListeners == null)
				refreshListeners = new ArrayList(1);

			if (bDebug) {
				debug("addRefreshListener; count=" + refreshListeners.size());
			}
			refreshListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeRefreshListener(TableCellRefreshListener listener) {
		try {
			this_mon.enter();

			if (refreshListeners == null)
				return;

			refreshListeners.remove(listener);
		} finally {

			this_mon.exit();
		}
	}

	public void addDisposeListener(TableCellDisposeListener listener) {
		try {
			this_mon.enter();

			if (disposeListeners == null) {
				disposeListeners = new ArrayList(1);
			}
			disposeListeners.add(listener);
		} finally {

			this_mon.exit();
		}
	}

	public void removeDisposeListener(TableCellDisposeListener listener) {
		try {
			this_mon.enter();

			if (disposeListeners == null)
				return;

			disposeListeners.remove(listener);

		} finally {

			this_mon.exit();
		}
	}

	public void addToolTipListener(TableCellToolTipListener listener) {
		try {
			this_mon.enter();

			if (tooltipListeners == null) {
				tooltipListeners = new ArrayList(1);
			}
			tooltipListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeToolTipListener(TableCellToolTipListener listener) {
		try {
			this_mon.enter();

			if (tooltipListeners == null)
				return;

			tooltipListeners.remove(listener);
		} finally {

			this_mon.exit();
		}
	}

	public void addMouseListener(TableCellMouseListener listener) {
		try {
			this_mon.enter();

			if (cellMouseListeners == null)
				cellMouseListeners = new ArrayList(1);

			cellMouseListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeMouseListener(TableCellMouseListener listener) {
		try {
			this_mon.enter();

			if (cellMouseListeners == null)
				return;

			cellMouseListeners.remove(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void addMouseMoveListener(TableCellMouseMoveListener listener) {
		try {
			this_mon.enter();

			if (cellMouseMoveListeners == null)
				cellMouseMoveListeners = new ArrayList(1);

			cellMouseMoveListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeMouseMoveListener(TableCellMouseMoveListener listener) {
		try {
			this_mon.enter();

			if (cellMouseMoveListeners == null)
				return;

			cellMouseMoveListeners.remove(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void addVisibilityListener(TableCellVisibilityListener listener) {
		try {
			this_mon.enter();

			if (cellVisibilityListeners == null)
				cellVisibilityListeners = new ArrayList(1);

			cellVisibilityListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void removeVisibilityListener(TableCellVisibilityListener listener) {
		try {
			this_mon.enter();

			if (cellVisibilityListeners == null)
				return;

			cellVisibilityListeners.remove(listener);

		} finally {
			this_mon.exit();
		}
	}

	/**
	 * @param listenerObject
	 *
	 * @since 3.1.1.1
	 */
	private void addSWTPaintListener(TableCellSWTPaintListener listener) {
		try {
			this_mon.enter();

			if (cellSWTPaintListeners == null)
				cellSWTPaintListeners = new ArrayList(1);

			cellSWTPaintListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public void invokeSWTPaintListeners(GC gc) {
		if (tableColumn != null) {
			Object[] swtPaintListeners = tableColumn.getCellOtherListeners("SWTPaint");
			if (swtPaintListeners != null) {
				for (int i = 0; i < swtPaintListeners.length; i++) {
					try {
						TableCellSWTPaintListener l = (TableCellSWTPaintListener) swtPaintListeners[i];

						l.cellPaint(gc, this);

					} catch (Throwable e) {
						Debug.printStackTrace(e);
					}
				}
			}
		}

		if (cellSWTPaintListeners == null) {
			return;
		}

		for (int i = 0; i < cellSWTPaintListeners.size(); i++) {
			try {
				TableCellSWTPaintListener l = (TableCellSWTPaintListener) (cellSWTPaintListeners.get(i));

				l.cellPaint(gc, this);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}

	private void addCellClipboardListener(TableCellClipboardListener listener) {
		try {
			this_mon.enter();

			if (cellClipboardListeners == null)
				cellClipboardListeners = new ArrayList<TableCellClipboardListener>(1);

			cellClipboardListeners.add(listener);

		} finally {
			this_mon.exit();
		}
	}

	public String getClipboardText() {
		if (isDisposed()) {
			return "";
		}
		String text = tableColumn.getClipboardText(this);
		if (text != null) {
			return text;
		}

		try {
			this_mon.enter();

			if (cellClipboardListeners != null) {
				for (TableCellClipboardListener l : cellClipboardListeners) {
					try {
						text = l.getClipboardText(this);
					} catch (Exception e) {
						Debug.out(e);
					}
					if (text != null) {
						break;
					}
				}
			}
		} finally {
			this_mon.exit();
		}
		if (text == null) {
			text = this.getText();
		}
		return text;
	}

	public void addListeners(Object listenerObject) {
		if (listenerObject instanceof TableCellDisposeListener) {
			addDisposeListener((TableCellDisposeListener) listenerObject);
		}

		if (listenerObject instanceof TableCellRefreshListener)
			addRefreshListener((TableCellRefreshListener) listenerObject);

		if (listenerObject instanceof TableCellToolTipListener)
			addToolTipListener((TableCellToolTipListener) listenerObject);

		if (listenerObject instanceof TableCellMouseMoveListener) {
			addMouseMoveListener((TableCellMouseMoveListener) listenerObject);
		}

		if (listenerObject instanceof TableCellMouseListener) {
			addMouseListener((TableCellMouseListener) listenerObject);
		}

		if (listenerObject instanceof TableCellVisibilityListener)
			addVisibilityListener((TableCellVisibilityListener) listenerObject);

		if (listenerObject instanceof TableCellSWTPaintListener)
			addSWTPaintListener((TableCellSWTPaintListener) listenerObject);

		if (listenerObject instanceof TableCellClipboardListener)
			addCellClipboardListener((TableCellClipboardListener) listenerObject);
	}

	public void invokeToolTipListeners(int type) {
		if (tableColumn == null)
			return;

		tableColumn.invokeCellToolTipListeners(this, type);

		if (tooltipListeners == null || tooltipErrLoopCount > 2)
			return;

		int iErrCount = tableColumn.getConsecutiveErrCount();
		if (iErrCount > 10)
			return;

		try {
			if (type == TOOLTIPLISTENER_HOVER) {
				for (int i = 0; i < tooltipListeners.size(); i++)
					((TableCellToolTipListener) (tooltipListeners.get(i))).cellHover(this);
			} else {
				for (int i = 0; i < tooltipListeners.size(); i++)
					((TableCellToolTipListener) (tooltipListeners.get(i))).cellHoverComplete(this);
			}
			tooltipErrLoopCount = 0;
		} catch (Throwable e) {
			tooltipErrLoopCount++;
			tableColumn.setConsecutiveErrCount(++iErrCount);
			pluginError(e);
			if (tooltipErrLoopCount > 2)
				Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
						"TableCell's tooltip will not be refreshed anymore this session."));
		}
	}

	public void invokeMouseListeners(TableCellMouseEvent event) {
		ArrayList listeners = event.eventType == TableCellMouseEvent.EVENT_MOUSEMOVE
				? cellMouseMoveListeners : cellMouseListeners;
		if (listeners == null)
			return;

		if (event.cell != null && event.row == null) {
			event.row = event.cell.getTableRow();
		}

		for (int i = 0; i < listeners.size(); i++) {
			try {
				TableCellMouseListener l = (TableCellMouseListener) (listeners.get(i));

				l.cellMouseTrigger(event);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}

	public void invokeVisibilityListeners(int visibility,
			boolean invokeColumnListeners) {
		if (invokeColumnListeners && tableColumn != null) {
			tableColumn.invokeCellVisibilityListeners(this, visibility);
		}

		if (cellVisibilityListeners == null)
			return;

		for (int i = 0; i < cellVisibilityListeners.size(); i++) {
			try {
				TableCellVisibilityListener l = cellVisibilityListeners.get(i);

				l.cellVisibilityChanged(this, visibility);

			} catch (Throwable e) {
				Debug.printStackTrace(e);
			}
		}
	}

	public void dispose() {
		if ( isDisposed()){
			// parg added this check at same time as removing the isDisposed check in getDataSource
			// in case there is some recursive disposal occuring on a dispose-listener
			Debug.out( "Double disposal!" );
			return;
		}
		setFlag(FLAG_DISPOSED);

		if (tableColumn != null) {
			tableColumn.invokeCellDisposeListeners(this);
		}

		if (disposeListeners != null) {
			try {
				for (Iterator iter = disposeListeners.iterator(); iter.hasNext();) {
					TableCellDisposeListener listener = (TableCellDisposeListener) iter.next();
					listener.dispose(this);
				}
				disposeListeners = null;
			} catch (Throwable e) {
				pluginError(e);
			}
		}

		refreshListeners = null;
		tableColumn = null;
		tableRow = null;
		sortValue = null;
	}

	public void debug(final String s) {
		if (DEBUGONLYZERO && tableColumn.getPosition() != 0) {
			return;
		}
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (tableRow == null) {
					System.out.println(SystemTime.getCurrentTime() + ": c"
							+ (tableColumn == null ? null : tableColumn.getPosition()) + ";F=" + flagToText(flags, false)
							+ ";" + s);

				} else {
					System.out.println(SystemTime.getCurrentTime() + ": r"
							+ tableRow.getIndex() + "c"
							+ (tableColumn == null ? null : tableColumn.getPosition())
							+ ";r.v?" + ((tableRow.isVisible() ? "Y" : "N")) + "F="
							+ flagToText(flags, false) + ";" + s);
				}
			}
		}, true);
	}

	protected void pluginError(Throwable e) {
		if (tableColumn != null) {
  		String sTitleLanguageKey = tableColumn.getTitleLanguageKey();
  
  		String sPosition = tableColumn.getPosition() + " ("
  				+ MessageText.getString(sTitleLanguageKey) + ")";
  		Logger.log(new LogEvent(LOGID, "Table Cell Plugin for Column #" + sPosition
  				+ " generated an exception ", e));
		} else {
  		Logger.log(new LogEvent(LOGID, "Table Cell Plugin generated an exception ", e));
		}
	}

	protected void pluginError(String s) {
		String sPosition = "r" + tableRow.getIndex() + "c"
				+ tableColumn.getPosition();
		Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
				"Table Cell Plugin for Column #" + sPosition + ":" + s + "\n  "
						+ Debug.getStackTrace(true, true)));
	}

	public boolean refresh() {
		return refresh(true);
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#refresh(boolean)
	public boolean refresh(boolean bDoGraphics) {
		boolean isRowShown;
		if (tableRow != null) {
			TableView view = tableRow.getView();
			isRowShown = view.isRowVisible(tableRow);
		} else {
			isRowShown = true;
		}
		boolean isCellShown = isRowShown && isShown();
		return refresh(bDoGraphics, isRowShown, isCellShown);
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#refresh(boolean, boolean)
	public boolean refresh(boolean bDoGraphics, boolean bRowVisible) {
		boolean isCellShown = bRowVisible && isShown();
		return refresh(bDoGraphics, bRowVisible, isCellShown);
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#refresh(boolean, boolean, boolean)
	public boolean refresh(boolean bDoGraphics, boolean bRowVisible,
			boolean bCellVisible) {
		//  	if (Utils.isThisThreadSWT()) {
		//  		System.out.println("ONSWT: " + Debug.getCompressedStackTrace());
		//  	}
		TableColumnCore	tc = tableColumn;

		if (tc == null) {
			return false;
		}
		boolean ret = getVisuallyChangedSinceRefresh();
		// don't clear flag -- since anyone can call refresh(), only the code that
		// actually refreshes the display should clear the flag.
		//clearFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);

		int iErrCount = 0;
		if (refreshErrLoopCount > 2) {
			return ret;
		}

		iErrCount = tc.getConsecutiveErrCount();
		if (iErrCount > 10) {
			refreshErrLoopCount = 3;
			return ret;
		}

		if (bInRefresh) {
			// Skip a Refresh call when being called from within refresh.
			// This could happen on virtual tables where SetData calls us again, or
			// if we ever introduce plugins to refresh.
			if (bDebug)
				debug("Calling Refresh from Refresh :) Skipping.");
			return ret;
		}
		try {
			bInRefresh = true;
			if (ret) {
				long now = SystemTime.getCurrentTime();
				if (now - lastRefresh < MAX_REFRESHES_WITHIN_MS) {
					numFastRefreshes++;
					if (numFastRefreshes >= MAX_REFRESHES) {
						if ((numFastRefreshes % MAX_REFRESHES) == 0) {
							pluginError("this plugin is crazy. tried to refresh "
									+ numFastRefreshes + " times in " + (now - lastRefresh)
									+ "ms");
						}
						return ret;
					}
				} else {
					numFastRefreshes = 0;
					lastRefresh = now;
				}
			}

			// See bIsUpToDate variable comments
			if (bCellVisible && !isUpToDate()) {
				if (bDebug)
					debug("Setting Invalid because visible & not up to date");
				clearFlag(FLAG_VALID);
				setFlag(FLAG_UPTODATE);
			} else if (!bCellVisible && isUpToDate()) {
				if (bDebug)
					debug("Setting not up to date because cell not visible "
							+ Debug.getCompressedStackTrace());
				clearFlag(FLAG_UPTODATE);
			}

			if (bDebug) {
				debug("Cell Valid?" + hasFlag(FLAG_VALID) + "; Visible?"
						+ tableRow.isVisible() + "/" + isShown());
			}
			int iInterval = tc.getRefreshInterval();
			if (iInterval == TableColumnCore.INTERVAL_INVALID_ONLY
					&& !hasFlag(FLAG_MUSTREFRESH | FLAG_VALID)
					&& hasFlag(FLAG_SORTVALUEISTEXT) && sortValue != null
					&& tc.getType() == TableColumnCore.TYPE_TEXT_ONLY) {
				if (bCellVisible) {
					if (bDebug)
						debug("fast refresh: setText");
					ret = setText((String) sortValue);
					setFlag(FLAG_VALID);
				}
			} else if ((iInterval == TableColumnCore.INTERVAL_LIVE
					|| (iInterval == TableColumnCore.INTERVAL_GRAPHIC && bDoGraphics)
					|| (iInterval > 0 && (loopFactor % iInterval) == 0)
					|| !hasFlag(FLAG_VALID) || hasFlag(FLAG_MUSTREFRESH))) {
				boolean bWasValid = isValid();

				ret = hasFlag(FLAG_MUSTREFRESH);
				if (ret) {
					clearFlag(FLAG_MUSTREFRESH);
				}

				if (bDebug) {
					debug("invoke refresh; wasValid? " + bWasValid);
				}

				long lTimeStart = Constants.isCVSVersion()
						? SystemTime.getMonotonousTime() : 0;
				tc.invokeCellRefreshListeners(this, !bCellVisible);
				if (refreshListeners != null) {
					for (TableCellRefreshListener l : refreshListeners) {
						if (l instanceof TableCellLightRefreshListener) {
							((TableCellLightRefreshListener) l).refresh(this, !bCellVisible);
						} else {
							l.refresh(this);
						}
					}
				}
				if (Constants.isCVSVersion()) {
					long lTimeEnd = SystemTime.getMonotonousTime();
					tc.addRefreshTime(lTimeEnd - lTimeStart);
				}

				// Change to valid only if we weren't valid before the listener calls
				// This is in case the listeners set valid to false when it was true
				if (!bWasValid && !hasFlag(FLAG_MUSTREFRESH)) {
					setFlag(FLAG_VALID);
				}
			}
			loopFactor++;
			refreshErrLoopCount = 0;
			if (iErrCount > 0)
				tc.setConsecutiveErrCount(0);

			// has changed if visually changed or "must refresh"
			ret |= getVisuallyChangedSinceRefresh();
			if (bDebug)
				debug("refresh done; visual change? " + ret + ";"
						+ Debug.getCompressedStackTrace());
		} catch (Throwable e) {
			refreshErrLoopCount++;
			if (tc != null) {
				tc.setConsecutiveErrCount(++iErrCount);
			}
			pluginError(e);
			if (refreshErrLoopCount > 2)
				Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
						"TableCell will not be refreshed anymore this session."));
		} finally {
			bInRefresh = false;
		}

		return ret;
	}

	public boolean setSortValue(Comparable valueToSort) {
		if ( tableColumn == null ){
			return( false );
		}
		if (!tableColumn.isSortValueLive()) {
			// objects that can't change aren't live
			if (!(valueToSort instanceof Number) && !(valueToSort instanceof String)
					&& !(valueToSort instanceof TableColumnSortObject)) {
				tableColumn.setSortValueLive(true);
			}
		}
		return _setSortValue(valueToSort);
	}

	private boolean _setSortValue(Comparable valueToSort) {
		if (isDisposed()) {
			return false;
		}

		if (sortValue == valueToSort)
			return false;

		if (hasFlag(FLAG_SORTVALUEISTEXT)) {
			clearFlag(FLAG_SORTVALUEISTEXT);
			if (sortValue instanceof String)
				// Make sure text is actually in the cell (it may not have been if
				// cell wasn't created at the time of setting)
				setText((String) sortValue);
		}

		if ((valueToSort instanceof String) && (sortValue instanceof String)
				&& sortValue.equals(valueToSort)) {
			return false;
		}

		if ((valueToSort instanceof Number) && (sortValue instanceof Number)
				&& sortValue.equals(valueToSort)) {
			return false;
		}

		if (bDebug)
			debug("Setting SortValue to "
					+ ((valueToSort == null) ? "null" : valueToSort.getClass().getName()));

		tableColumn.setLastSortValueChange(SystemTime.getCurrentTime());
		sortValue = valueToSort;

		// Columns with SWT Paint Listeners usually rely on a repaint whenever the
		// sort value changes
		if (cellSWTPaintListeners != null
				|| tableColumn.hasCellOtherListeners("SWTPaint")) {
    	setFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
			//redraw();
		}

		return true;
	}

	public boolean setSortValue(long valueToSort) {
		if (isDisposed()) {
			return false;
		}

		if ((sortValue instanceof Long)
				&& ((Long) sortValue).longValue() == valueToSort)
			return false;

		return _setSortValue(Long.valueOf(valueToSort));
	}

	public boolean setSortValue(float valueToSort) {
		if (isDisposed()) {
			return false;
		}

		if (sortValue instanceof Float
				&& ((Float) sortValue).floatValue() == valueToSort)
			return false;

		return _setSortValue(new Float(valueToSort));
	}

	public Comparable getSortValue() {
		return sortValue;
	}

	public boolean isValid() {
		// Called often.. inline faster
		return (flags & FLAG_VALID) != 0;
		//return hasFlag(FLAG_VALID);
	}

	public boolean isDisposed() {
		return (flags & FLAG_DISPOSED) != 0;
	}

	public boolean hasFlag(int flag) {
		return (flags & flag) != 0;
	}

	public void setFlag(int flag) {
		if (DEBUG_FLAGS && (flags & flag) != flag) {
			debug("SET FLAG " + flagToText((~flags) & flag, true) + " via "
					+ Debug.getStackTrace(true, true, 1, 7));
		}
		flags |= flag;
	}

	public void clearFlag(int flag) {
		if (DEBUG_FLAGS && (flags & flag) != 0) {
			debug("CLEAR FLAG " + flagToText(flags & flag, true) + " via "
					+ Debug.getStackTrace(true, true, 1, 7));
		}
		flags &= ~flag;
	}

	/**
	 * If a plugin in trying to invalidate a cell, then clear the sort value
	 * too.
	 */
	public void invalidate() {
		if (isDisposed()) {
			return;
		}

		invalidate(true);
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.common.table.TableCellCore#invalidate(boolean)
	 */
	public void invalidate(boolean bMustRefresh) {
		//if (bInRefresh && Utils.isThisThreadSWT()) {
		//	System.out.println("Invalidating when in refresh via " + Debug.getCompressedStackTrace());
		//}
		if ((flags & (FLAG_VALID | FLAG_VISUALLY_CHANGED_SINCE_REFRESH)) == FLAG_VISUALLY_CHANGED_SINCE_REFRESH) { //!hasFlag(FLAG_VALID)
			if (bMustRefresh) {
				if ((flags & FLAG_MUSTREFRESH) != 0) {
					return;
				}
			} else {
				if (DEBUG_FLAGS) {
					debug("ALREADY FLAGGED for invalidate via " + Debug.getCompressedStackTrace(7));
				}
				return;
			}
		}
		clearFlag(FLAG_VALID);

		if (bDebug)
			debug("Invalidate Cell;" + bMustRefresh);

		if (bMustRefresh) {
			setFlag(FLAG_MUSTREFRESH | FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
		} else {
			setFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableCellCore#refreshAsync()
	public void refreshAsync() {
		if (bInRefreshAsync) {
			//System.out.println(System.currentTimeMillis() + "] SKIP " + restartRefresh);
			if (restartRefresh < Byte.MAX_VALUE) {
				restartRefresh++;
			}
			return;
		}
		bInRefreshAsync = true;

		AERunnable runnable = new AERunnable() {
			public void runSupport() {
				//System.out.println(System.currentTimeMillis() + "] REFRESH!");
				restartRefresh = 0;
				refresh(true);
				bInRefreshAsync = false;
				//System.out.println(System.currentTimeMillis() + "] REFRESH OUT!");
				if (restartRefresh > 0) {
					refreshAsync();
				}
			}
		};
		Utils.execSWTThreadLater(25, runnable);
	}

	public void setUpToDate(boolean upToDate) {
		if (bDebug)
			debug("set up to date to " + upToDate);
		if (upToDate) {
			setFlag(FLAG_UPTODATE);
		} else {
			clearFlag(FLAG_UPTODATE);
		}
	}

	public boolean isUpToDate() {
		return hasFlag(FLAG_UPTODATE);
	}

	public boolean getVisuallyChangedSinceRefresh() {
		return hasFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
	}

	public void clearVisuallyChangedSinceRefresh() {
		clearFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
	}

	/** Compare our sortValue to the specified object.  Assumes the object 
	 * is TableCellSWTBase (safe assumption)
	 */
	public int compareTo(Object o) {
		try {
			Comparable ourSortValue = getSortValue();
			Comparable otherSortValue = ((TableCellSWTBase) o).getSortValue();
			if (ourSortValue instanceof String && otherSortValue instanceof String) {
				// Collator.getInstance cache's Collator object, so this is relatively
				// fast.  However, storing it as static somewhere might give a small
				// performance boost.  If such an approach is take, ensure that the static
				// variable is updated the user chooses an different language.
				Collator collator = Collator.getInstance(Locale.getDefault());
				return collator.compare(ourSortValue, otherSortValue);
			}
			try {
				return ourSortValue.compareTo(otherSortValue);
			} catch (ClassCastException e) {
				// It's possible that a row was created, but not refreshed yet.
				// In that case, one sortValue will be String, and the other will be
				// a comparable object that the plugin defined.  Those two sortValues 
				// may not be compatable (for good reason!), so just skip it.
			}
		} catch (Exception e) {
			System.out.println("Could not compare cells");
			Debug.printStackTrace(e);
		}
		return 0;
	}

	public boolean needsPainting() {
		if (isDisposed()) {
			return false;
		}

		if (cellSWTPaintListeners != null
				|| tableColumn.hasCellOtherListeners("SWTPaint")) {
			return true;
		}

		return getGraphic() != null;
	}

	public boolean setText(String text) {
		if (isDisposed()) {
			return false;
		}

		if (text == null)
			text = "";
		boolean bChanged = false;

		if (hasFlag(FLAG_SORTVALUEISTEXT) && !text.equals(sortValue)) {
			bChanged = true;
			sortValue = text;
			tableColumn.setLastSortValueChange(SystemTime.getCurrentTime());
			if (bDebug)
				debug("Setting SortValue to text;");
		}

		// Slower than setText(..)!
		//  	if (isInvisibleAndCanRefresh()) {
		//  		if (bDebug) {
		//  			debug("setText ignored: invisible");
		//  		}
		//  		return false;
		//  	}

		if (uiSetText(text) && !hasFlag(FLAG_SORTVALUEISTEXT))
			bChanged = true;

		if (bDebug) {
			debug("setText (" + bChanged + ") : " + text);
		}

		if (bChanged) {
			setFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
		}

		boolean do_auto = tableColumn == null ? false : tableColumn.doesAutoTooltip();

		// If we were using auto tooltips (and we aren't any more), then
		// clear up previously set tooltips.
		if (!do_auto) {
			if (hasFlag(FLAG_TOOLTIPISAUTO)) {
				this.oToolTip = null;
				clearFlag(FLAG_TOOLTIPISAUTO);
			}
		}

		else {
			this.oToolTip = text;
			setFlag(FLAG_TOOLTIPISAUTO);
		}

		return bChanged;
	}

	public void setToolTip(Object tooltip) {
		oToolTip = tooltip;

		if (tooltip == null) {
			setFlag(FLAG_TOOLTIPISAUTO);
		} else {
			clearFlag(FLAG_TOOLTIPISAUTO);
		}
	}

	public Object getToolTip() {
		return oToolTip;
	}

	public Object getDefaultToolTip() {
		return defaultToolTip;
	}

	public void setDefaultToolTip(Object tt) {
		defaultToolTip = tt;
	}

	public abstract boolean uiSetText(String text);

  public void doPaint(GC gc) {
  	//This sometimes causes a infinite loop if the listener invalidates
  	//the drawing area
  	//if ((!hasFlag(FLAG_UPTODATE) || !hasFlag(FLAG_VALID)) && !bInRefresh && !bInRefreshAsync
		//		&& (refreshListeners != null || tableColumn.hasCellRefreshListener())) {
  	//	if (bDebug) {
  	//		debug("doPaint: invoke refresh");
  	//	}
  	//	refresh(true);
  	//}

		if (bDebug) {
			debug("doPaint up2date:" + hasFlag(FLAG_UPTODATE) + ";v:" + hasFlag(FLAG_VALID) + ";rl=" + refreshListeners);
		}
		
		invokeSWTPaintListeners(gc);
  }

  public int getTextAlpha() {
		return textAlpha;
	}

	public void setTextAlpha(int textOpacity) {
		this.textAlpha = textOpacity;
	}


	// @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#getTableRowSWT()
	public TableRowSWT getTableRowSWT() {
		if (tableRow instanceof TableRowSWT) {
			return (TableRowSWT)tableRow;
		}
		return null;
	}

  public TableRowCore getTableRowCore() {
    return tableRow;
  }
  
  private String flagToText(int flag, boolean onlySet) {
  	StringBuilder sb = new StringBuilder();
  	sb.append((flag & FLAG_DISPOSED) > 0 ? 'D' : onlySet ? ' ' : 'd');
  	sb.append((flag & FLAG_MUSTREFRESH) > 0 ? 'M' : onlySet ? ' ' : 'm');
  	sb.append((flag & FLAG_SORTVALUEISTEXT) > 0 ? 'S' : onlySet ? ' ' : 's');
  	sb.append((flag & FLAG_TOOLTIPISAUTO) > 0 ? 'T' : onlySet ? ' ' : 't');
  	sb.append((flag & FLAG_UPTODATE) > 0 ? 'U' : onlySet ? ' ' : 'u');
  	sb.append((flag & FLAG_VALID) > 0 ? 'V' : onlySet ? ' ' : 'v');
  	sb.append((flag & FLAG_VISUALLY_CHANGED_SINCE_REFRESH) > 0 ? "VC" : onlySet ? ' ' : "vc");
  	return sb.toString();
  }

	public abstract int getWidthRaw();


	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.tables.TableCell#setFillCell(boolean)
	 */
	public void setFillCell(boolean doFillCell) {
		this.doFillCell = doFillCell;
	}
	
	public boolean getFillCell() {
		return doFillCell;
	}


	public TableColumnCore getTableColumnCore() {
		return tableColumn;
	}
	
	public boolean setCursorID(int cursorID) {
		if (iCursorID == cursorID) {
			return false;
		}
		iCursorID = cursorID;
		return true;
	}


	public int getCursorID() {
		return iCursorID;
	}

	public void setMouseOver(boolean b) {
		mouseOver = b;
	}

	public boolean isMouseOver() {
		if (tableRow != null && !tableRow.isVisible()) {
			// XXX: Should trigger event and set mouseOver false?
			return false;
		}
		return mouseOver;
	}

  public boolean setIcon(Image img) {
  	if (isInvisibleAndCanRefresh())
  		return false;

  	icon = img;
  	
    graphic = null;
    setFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
    return true;
  }
  
  public Image getIcon() {
  	return icon;
  }

  // @see org.gudy.azureus2.ui.swt.views.table.TableCellSWT#setGraphic(org.eclipse.swt.graphics.Image)
  public boolean setGraphic(Image img) {
  	return setGraphic(new UISWTGraphicImpl(img));
  }

  // @see org.gudy.azureus2.plugins.ui.tables.TableCell#setGraphic(org.gudy.azureus2.plugins.ui.Graphic)
  public boolean setGraphic(Graphic img) {
  	if (img != null && isDisposed()) {
			return false;
  	}

		if (tableColumn == null
				|| tableColumn.getType() != TableColumnCore.TYPE_GRAPHIC) {
      return false;
    }

    if (img == graphic && numFastRefreshes >= MAX_REFRESHES) {
    	pluginError("TableCellImpl::setGraphic to same Graphic object. "
					+ "Forcing refresh.");
    }
    
    boolean changed = (img == graphic || (img != null && !img.equals(graphic)) || (graphic != null && !graphic.equals(img)));
    
    graphic = img;
    
    if (changed) {
    	setFlag(FLAG_VISUALLY_CHANGED_SINCE_REFRESH);
    	redraw();
    }
    
    return changed;
  }

  public Graphic getGraphic() {
  	return graphic;
  }
  
  public Image getGraphicSWT() {
		return (graphic instanceof UISWTGraphic)
				? ((UISWTGraphic) graphic).getImage() : null;
  }

	public boolean isInvisibleAndCanRefresh() {
  	return !isDisposed() && !isShown()
				&& (refreshListeners != null || tableColumn.hasCellRefreshListener());
	}

  public int[] getBackground() {
		Color color = getBackgroundSWT();

		if (color == null) {
			return null;
		}

		return new int[] {
			color.getRed(),
			color.getGreen(),
			color.getBlue()
		};
	}

  // @see org.gudy.azureus2.plugins.ui.tables.TableCell#getForeground()
  public int[] getForeground() {
		Color color = getForegroundSWT();

		if (color == null) {
			return new int[3];
		}

		return new int[] { color.getRed(), color.getGreen(), color.getBlue()
		};
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.tables.TableCell#setForeground(int, int, int)
	 */
	public boolean setForeground(int red, int green, int blue) {
		// Don't need to set when not visible
		if (isInvisibleAndCanRefresh()) {
			return false;
		}

		if (red < 0 || green < 0 || blue < 0) {
			return setForeground((Color) null);
		}
		return setForeground(new RGB(red, green, blue));
	}

	private boolean setForeground(final RGB rgb) {
		Color colorFG = getForegroundSWT();
		boolean changed = colorFG == null || colorFG.isDisposed()
				|| !colorFG.getRGB().equals(rgb);
		if (changed) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					setForeground(ColorCache.getColor(Display.getCurrent(), rgb));
				}
			});
		}
		return changed;
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCell#setForeground(int[])
	public boolean setForeground(int[] rgb) {
		if (rgb == null || rgb.length < 3) {
			return setForeground((Color) null);
		}
		return setForeground(rgb[0], rgb[1], rgb[2]);
	}

  public boolean setForegroundToErrorColor() {
	  return setForeground(Colors.colorError);
  }

	public int[] getMouseOffset() {
		Point ofs = ((TableViewSWT) tableRow.getView()).getTableCellMouseOffset(this);
		return ofs == null ? null : new int[] { ofs.x, ofs.y };
	}

	public String getObfusticatedText() {
		if (isDisposed()) {
			return null;
		}
		if (tableColumn.isObfusticated()) {
			if (tableColumn instanceof ObfusticateCellText) {
				return ((ObfusticateCellText)tableColumn).getObfusticatedText(this);
			}
			
			return "";
		}
		return null;
	}

}
