/*
 * Created : 11 nov. 2004
 * By      : Alon Rohter
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

import java.util.List;

import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;



public class SwarmAverageCompletion
       extends CoreTableColumn 
       implements TableCellRefreshListener
{

  public SwarmAverageCompletion(String sTableID) {
    super("swarm_average_completion", ALIGN_TRAIL, POSITION_INVISIBLE, 70, sTableID);
    setRefreshInterval(INTERVAL_GRAPHIC);  //TODO
  }

  protected void finalize() throws Throwable {
    super.finalize();
  }

  public void refresh(TableCell cell) {
    int average = -1;

    DownloadManager dm = (DownloadManager)cell.getDataSource();
    
    if( dm != null && dm.getPeerManager() != null ) {	
    	List peers = dm.getPeerManager().getPeers();
    	
    	if( peers != null && peers.size() > 0 ) {
    		int sum = 0;
    	
    		for( int i=0; i < peers.size(); i++ ) {
    			PEPeer peer = (PEPeer)peers.get( i );	
    			sum += peer.getPercentDoneInThousandNotation();
    		}
    	
    		average = sum / peers.size();
    	}
    }

    if( !cell.setSortValue( average ) && cell.isValid() ) {
      return;
    }
    
    if( average < 0 ) {
      cell.setText( "" );
    }
    else {
      cell.setText( DisplayFormatters.formatPercentFromThousands( average ) );
    }
  }

}
