/*
 * File    : TableCellCore.java
 * Created : 2004/May/14
 *
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
 
package org.gudy.azureus2.ui.swt.views.table;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import org.gudy.azureus2.plugins.ui.tables.TableCell;


/** Core Table Cell functions are those available to plugins plus
 * some core-only functions.  The core-only functions are listed here.
 *
 * @see org.gudy.azureus2.ui.swt.views.table.impl.TableCellImpl
 *
 * @future split out SWT functions to TableCellSWTCore and move TableCellCore
 *         out of swt package. An abstract adapter for TableCell may have to 
 *         be created which implents any SWT functions (overriden by SWT 
 *         implementation)
 */
public interface TableCellCore
       extends TableCell, Comparable
{
  static final int TOOLTIPLISTENER_HOVER = 0;
  static final int TOOLTIPLISTENER_HOVERCOMPLETE = 1;
  
  public void invalidate(boolean bMustRefresh);

  /** Change the cell's foreground color.
   *
   * NOTE: favor (R, G, B)
   *
   * @param color SWT Color object.
   * @return True - Color changed. <br>
   *         False - Color was already set.
   */
  boolean setForeground(Color color);

  /** 
   * Refresh the cell
   * 
   * @param bDoGraphics Whether to update graphic cells 
   */
  public void refresh(boolean bDoGraphics);

  /** 
   * Refresh the cell, including graphic types 
   */
  public void refresh();
  
  /**
   * Refresh the cell.  This method overide takes a bRowVisible paramater in
   * order to reduce the number of calls to TableRow.isVisible() in cases where
   * multiple cells on the same row are being refreshed.
   * 
   * @param bDoGraphics Whether to update graphic cells
   * @param bRowVisible Visibility state of row
   */
  public void refresh(boolean bDoGraphics, boolean bRowVisible);
  
  /** dispose of the cell */
  public void dispose();
  
  /** Set the cell's image
   *
   * @param img Cell's new image
   */
  public void setImage(Image img);

  /** Retrieve whether the cell need any paint calls (graphic)
   *
   * @return whether the cell needs painting
   */
  public boolean needsPainting();
  
  /** Paint the cell (for graphics)
   *
   * @param gc GC object to be used for painting
   */
  public void doPaint(GC gc);

  /** Location of the cell has changed */
  public void locationChanged();

  /** Retrieve the row that this cell belongs to
   *
   * @return the row that this cell belongs to
   */
  public TableRowCore getTableRowCore();
  
  public Point getSize();
  
  public boolean setGraphic(Image img);
  public Image getGraphicSWT();

  public void invokeToolTipListeners(int type);
  public void setUpToDate(boolean upToDate);
}
