/*
 * File    : SeedingRankColumnListener.java
 * Created : Sep 27, 2005
 * By      : TuxPaper
 *
 * Copyright (C) 2005, 2006 Aelitis SAS, All rights Reserved
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

package com.aelitis.azureus.plugins.startstoprules.defaultplugin;

import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;

/** A "My Torrents" column for displaying Seeding Rank.
 */
public class 
DownloadingRankColumnListener 
	implements TableCellRefreshListener
{
	private StartStopRulesDefaultPlugin		plugin;
	
	public 
	DownloadingRankColumnListener(
		StartStopRulesDefaultPlugin		_plugin ) 
	{
		plugin	= _plugin;
	}

	public void refresh(TableCell cell) {
		Download dl = (Download) cell.getDataSource();
		if (dl == null)
			return;

		DefaultRankCalculator dlData = null;
		Object o = cell.getSortValue();
		if (o instanceof DefaultRankCalculator)
			dlData = (DefaultRankCalculator) o;
		else {
			dlData = StartStopRulesDefaultPlugin.getRankCalculator( dl );
		}
		if (dlData == null)
			return;
			
		int position = dlData.dl.getPosition();
		
		cell.setSortValue( position );

		cell.setText( "" + position );
		if (plugin.bDebugLog) {
			String dlr = dlData.getDLRTrace();
			if ( dlr.length() > 0 ){
				dlr = "AR: " + dlr + "\n";
			}
			cell.setToolTip(
				dlr + 
				"TRACE:\n" + dlData.sTrace );
		} else {
			cell.setToolTip(null);
		}
	}
}
