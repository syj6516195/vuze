/*
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
 
package org.gudy.azureus2.ui.swt.views.table;

import java.util.Comparator;
import java.util.List;

import org.gudy.azureus2.core3.util.IndentWriter;

import org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;

/** Core Table Column functions are those available to plugins plus
 * some core-only functions.  The core-only functions are listed here.
 *
 * @see TableColumnManager

 * @future split out SWT functions to TableColumnSWTCore and move TableColumnCore
 *         out of swt package.
 */
public interface TableColumnCore extends TableColumn, Comparator {
  /** Set the internal flag specifying whether the column has been added to the
   * TableColumnManager.  Some functions can not be run after a column has been
   * added.
   *
   * @param bAdded true - Column has been added<br>
   *               false - Column has not been added
   */
  public void setColumnAdded(boolean bAdded);
  /** Retrieve  whether the column has been added to the TableColumnManager
   * 
   * @return true - Column has been added<br>
   *         false - Column has not been added
   */
  public boolean getColumnAdded();
  
  /** Changes what {@link TableCellCore.getDataSource()} and 
   * {@link TableRowCore.getDataSource()} return.
   *
   * @param bCoreDataSource true - returns a core object<br>
   *                        false - returns a plugin object (if available)
   */
  public void setUseCoreDataSource(boolean bCoreDataSource);
  /** Retrieve whether a core or plugin object is sent via getDataSource()
   *
   * @return true - returns a core object<br>
   *         false - returns a plugin object (if available)
   */
  public boolean getUseCoreDataSource();
  
  /** Send a refresh trigger to all listeners stored in TableColumn
   *
   * @param cell the cell is being refreshed
   */
  public void invokeCellRefreshListeners(TableCellCore cell);

  public List getCellRefreshListeners();
  
  /** Send a cellAdded trigger to all listeners stored in TableColumn
   *
   * @param cell the cell is being added
   */
  public void invokeCellAddedListeners(TableCellCore cell);

  public List getCellAddedListeners();
  
  /** Send a dispose trigger to all listeners stored in TableColumn
   *
   * @param cell the cell is being disposed
   */
  public void invokeCellDisposeListeners(TableCellCore cell);

  public void invokeCellToolTipListeners(TableCellCore cell, int type);

  public void invokeCellMouseListeners(TableCellMouseEvent event);


  /** Sets the position of the column without adjusting the other columns.
   * This will cause duplicate columns, and is only usefull if you are
   * adjusting the positions of multiple columns at once.
   *
   * @param position new position (0 based)
   *
   * @see TableColumnManager.ensureIntegrity()
   */
  public void setPositionNoShift(int position);

  /** Load width and position settings from config.
   */
  public void loadSettings();
  /** Save width and position settings to config.
   */
  public void saveSettings();

  /** Convert the getAlignment() constant to a SWT constant
   *
   * @return SWT alignment constant
   */
  public int getSWTAlign();
  
  /** Returns the key in the properties bundle that has the title of the
   * column.
   *
   * @return Title's language key
   */
  public String getTitleLanguageKey();
  
  public int getConsecutiveErrCount();
  public void setConsecutiveErrCount(int iCount);
  
  public TableContextMenuItem addContextMenuItem(String key);
  public void removeContextMenuItem(TableContextMenuItem menuItem);
  public TableContextMenuItem[] getContextMenuItems();
	/**
	 * @return
	 */
	boolean hasCellRefreshListener();
	
	/**
	 * @return
	 */
	long getLastSortValueChange();
	
	/**
	 * @param lastSortValueChange
	 */
	void setLastSortValueChange(long lastSortValueChange);

	/**
	 * @param live
	 */
	public void setSortValueLive(boolean live);
	
	public boolean isSortValueLive();
	/**
	 * @param ms
	 */
	public void addRefreshTime(long ms);
	/**
	 * @param writer
	 */
	void generateDiagnostics(IndentWriter writer);

  void setTableID(String tableID);
	/**
	 * @return
	 */
	boolean isSortAscending();
	/**
	 * @param bAscending
	 */
	void setSortAscending(boolean bAscending);
}