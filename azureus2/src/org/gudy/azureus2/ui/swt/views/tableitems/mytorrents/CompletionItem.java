/*
 * File    : CompletionItem.java
 * Created : 24 nov. 2003
 * By      : Olivier
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;

/** Torrent Completion Level Graphic Cell for My Torrents.
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class CompletionItem
       extends CoreTableColumn 
       implements TableCellAddedListener
{
  private static final int borderWidth = 1;

  /** Default Constructor */
  public CompletionItem() {
    super("completion", TableManager.TABLE_MYTORRENTS_INCOMPLETE);
    initializeAsGraphic(POSITION_INVISIBLE, 250);
  }

  public void cellAdded(TableCell cell) {
    cell.setMarginHeight(2);
    new Cell(cell);
  }

  private class Cell
          implements TableCellRefreshListener, TableCellDisposeListener
  {
    int lastPercentDone = 0;
    
    public Cell(TableCell cell) {
      cell.setFillCell(true);
			cell.addListeners(this);
    }

    public void dispose(TableCell cell) {
      Image img = ((TableCellCore)cell).getGraphicSWT();
      if (img != null && !img.isDisposed())
        img.dispose();
    }
  
      
    
    public void refresh(TableCell cell) {
      int percentDone = getPercentDone(cell);
      
      if( !cell.setSortValue(percentDone) && cell.isValid() && lastPercentDone == percentDone ) {
        return;
      }
      
      //Compute bounds ...
      int newWidth = cell.getWidth();
      if (newWidth <= 0)
        return;
      int newHeight = cell.getHeight();
      
      int x1 = newWidth - borderWidth - 1;
      int y1 = newHeight - borderWidth - 1;
      if (x1 < 10 || y1 < 3) {
        return;
      }

      lastPercentDone = percentDone;

      Image image = ((TableCellCore)cell).getGraphicSWT();
      GC gcImage;
      boolean bImageSizeChanged;
      Rectangle imageBounds;
      if (image == null) {
        bImageSizeChanged = true;
      } else {
        imageBounds = image.getBounds();
        bImageSizeChanged = imageBounds.width != newWidth ||
                            imageBounds.height != newHeight;
      }
      
      if (bImageSizeChanged) {
        image = new Image(SWTThread.getInstance().getDisplay(),
                          newWidth, newHeight);
        imageBounds = image.getBounds();
  
        // draw border
        gcImage = new GC(image);
        gcImage.setForeground(Colors.grey);
        gcImage.drawRectangle(0, 0, newWidth - 1, newHeight - 1);
      } else {
        gcImage = new GC(image);
      }
  
      int limit = (x1 * percentDone) / 1000;
      gcImage.setBackground(Colors.blues[Colors.BLUES_DARKEST]);
      gcImage.fillRectangle(1,1,limit,y1);
      if (limit < x1) {
        gcImage.setBackground(Colors.blues[Colors.BLUES_LIGHTEST]);
        gcImage.fillRectangle(limit+1, 1, x1-limit, y1);
      }
  
      gcImage.dispose();
        
      ((TableCellCore)cell).setGraphic(image);
    }
  
    public int getPercentDone(TableCell cell) {
      DownloadManager dm = (DownloadManager)cell.getDataSource();
      if (dm == null) {
        return 0;
      }
      return dm.getStats().getDownloadCompleted(true);
    }
  }
}
