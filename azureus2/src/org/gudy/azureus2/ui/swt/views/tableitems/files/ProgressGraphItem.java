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

package org.gudy.azureus2.ui.swt.views.tableitems.files;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.TableCellCore;

/** Torrent Completion Level Graphic Cell for My Torrents.
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/17: modified to TableCellAdapter)
 */
public class ProgressGraphItem
       extends CoreTableColumn 
       implements TableCellAddedListener
{
  private static final int borderWidth = 1;

  /** Default Constructor */
  public ProgressGraphItem() {
    super("pieces", TableManager.TABLE_TORRENT_FILES);
    initializeAsGraphic(POSITION_LAST, 200);
  }

  public void cellAdded(TableCell cell) {
    new Cell(cell);
  }

  private class Cell
          implements TableCellRefreshListener, TableCellDisposeListener
  {
    int lastPercentDone = 0;
    private long last_draw_time	= SystemTime.getCurrentTime();
    private boolean bNoRed = false;
    private boolean	was_running = false;

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
      DiskManagerFileInfo fileInfo = (DiskManagerFileInfo)cell.getDataSource();
      int percentDone = 0;
      if (fileInfo != null && fileInfo.getLength() != 0)
        percentDone = (int)((1000 * fileInfo.getDownloaded()) / fileInfo.getLength());
      
      cell.setSortValue(percentDone);

      //Compute bounds ...
      int newWidth = cell.getWidth();
      if (newWidth <= 0) {
        return;
      }
      int newHeight = cell.getHeight();

      int x1 = newWidth - borderWidth - 1;
      int y1 = newHeight - borderWidth - 1;
      if (x1 < 10 || y1 < 3) {
        return;
      }

	  DiskManager			manager			= fileInfo.getDiskManager();

      	// we want to run through the image part once one the transition from with a disk manager (running)
      	// to without a disk manager (stopped) in order to clear the pieces view
      
		 
	  boolean	running	= manager != null;
	  
      boolean bImageBufferValid = (lastPercentDone == percentDone) &&
                                  cell.isValid() && bNoRed && running == was_running;
      
      if (bImageBufferValid) {
        return;
      }

      was_running	= running;
      
      lastPercentDone = percentDone;

      Image piecesImage = ((TableCellCore)cell).getGraphicSWT();

      if (piecesImage != null && !piecesImage.isDisposed())
        piecesImage.dispose();
      piecesImage = new Image(SWTThread.getInstance().getDisplay(),
                              newWidth, newHeight);

      GC gcImage = new GC(piecesImage);

	  	// dm may be null if this is a skeleton file view
	  
      if (fileInfo != null && manager != null ) {
    	   	  
        if (percentDone == 1000) {
          gcImage.setForeground(Colors.blues[Colors.BLUES_DARKEST]);
          gcImage.setBackground(Colors.blues[Colors.BLUES_DARKEST]);
          gcImage.fillRectangle(1, 1, newWidth - 2, newHeight - 2);
        } else {
          int firstPiece = fileInfo.getFirstPieceNumber();
          int nbPieces = fileInfo.getNbPieces();
       
          DiskManagerPiece[] dm_pieces = manager.getPieces();
       
          bNoRed = true;
          for (int i = 0; i < newWidth; i++) {
            int a0 = (i * nbPieces) / newWidth;
            int a1 = ((i + 1) * nbPieces) / newWidth;
            if (a1 == a0)
              a1++;
            if (a1 > nbPieces && nbPieces != 0)
              a1 = nbPieces;
            int nbAvailable = 0;
            boolean written   = false;
            boolean partially_written = false;
            if (firstPiece >= 0) {
              for (int j = a0; j < a1; j++){
                int this_index = j+firstPiece;
                
               	DiskManagerPiece	dm_piece = dm_pieces[this_index];
                
                if (dm_piece.getDone()) {
                  nbAvailable++;
                }
                
                if (written){
                  continue;
                }
                
                written = written || (dm_piece.getLastWriteTime() + 500) > last_draw_time;
    
                if ((!written) && (!partially_written)) {
                  boolean[] blocks = dm_piece.getWritten();
    
                  if ( blocks != null ) {
                    for (int k = 0; k < blocks.length; k++){
                      if (blocks[k]){
                    	  partially_written = true;
                        break;
                      }
                    }
                  }
                }
    
              } // for j
            } else {
              nbAvailable = 1;
            }
    
            gcImage.setBackground(written ? Colors.red
                                          : partially_written ? Colors.grey 
                                                      : Colors.blues[(nbAvailable * Colors.BLUES_DARKEST) / (a1 - a0)]);
            gcImage.fillRectangle(i, 1, 1, newHeight - 2);
            if (written)
              bNoRed = false;
          }
          gcImage.setForeground(Colors.grey);
        }
      } else {
        gcImage.setForeground(Colors.grey);
      }
	  
	  if ( manager != null ){
		  
		  gcImage.drawRectangle(0, 0, newWidth - 1, newHeight - 1);
	  }

      gcImage.dispose();
      last_draw_time = SystemTime.getCurrentTime();

      ((TableCellCore)cell).setGraphic(piecesImage);
    }
  }
}
