/*
 * File    : TypeItem.java
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
 
package org.gudy.azureus2.ui.swt.views.tableitems.peers;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

/**
 *
 * @author Olivier
 * @author TuxPaper (2004/Apr/19: modified to TableCellAdapter)
 */
public class TypeItem
       extends CoreTableColumn 
       implements TableCellRefreshListener, TableCellToolTipListener
{
  /** Default Constructor */
  public TypeItem() {
    super("T", ALIGN_CENTER, POSITION_LAST, 20, TableManager.TABLE_TORRENT_PEERS);
    setRefreshInterval(INTERVAL_LIVE);
  }

  public void refresh(TableCell cell) {
    PEPeer peer = (PEPeer)cell.getDataSource();
    long value = (peer == null) ? 0 : (peer.isIncoming() ? 1 : 0);

    if (!cell.setSortValue(value) && cell.isValid())
      return;

    cell.setText((value == 1) ? "R" : "L");
  }

	public void cellHover(TableCell cell) {
		String ID = "PeersView.T." + cell.getText() + ".tooltip";
		String sTooltip = MessageText.getString(ID, "");
		if (sTooltip.length() > 0)
			cell.setToolTip(sTooltip);
	}

	public void cellHoverComplete(TableCell cell) {
		cell.setToolTip(null);
	}
}
