/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.program.Program;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.debug.ObfusticateCellText;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;

/**
 * @author TuxPaper
 * @created Oct 10, 2006
 *
 */
public class ColumnTitle
	extends CoreTableColumn
	implements TableCellRefreshListener, ObfusticateCellText
{
	public static String COLUMN_ID = "name";
	
	public static boolean SHOW_EXT_INFO = false;

	static public String s = "";

	/** Default Constructor */
	public ColumnTitle(String sTableID) {
		super(COLUMN_ID, POSITION_LAST, 250, sTableID);
		setMinWidth(70);
		setObfustication(true);
		setType(TableColumn.TYPE_TEXT);
	}

	public void refresh(TableCell cell) {
		String name = null;
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		if (dm != null) {
			name = PlatformTorrentUtils.getContentTitle2(dm);
		}
		if (name == null) {
			name = "";
		}

		if (!cell.setSortValue(name) && cell.isValid()) {
			return;
		}

		if (!cell.isShown()) {
			return;
		}

		if (SHOW_EXT_INFO && name.length() > 0) {
			String path = dm.getDownloadState().getPrimaryFile();
			if (path != null) {
				int pos = path.lastIndexOf('.');
				if (pos >= 0) {
					String ext = path.substring(pos);
					Program program = Program.findProgram(ext);
					if (program != null) {
						ext += " (" + program.getName() + ")";
					}
					name += "\n"
							+ MessageText.getString("TableColumn.header.name.ext",
									new String[] {
										ext
									});
				}
			}
		}
		
		if (ColumnProgressETA.TRY_NAME_COLUMN_EXPANDER) {
  		if (dm.getAssumedComplete()) {
  			long size = dm.getSize() - dm.getStats().getRemaining();
  			name += "\nCompleted. " + DisplayFormatters.formatByteCountToKiBEtc(size);
  		}
		}
		
		cell.setText(name);
	}

	public String getObfusticatedText(TableCell cell) {
		String name = null;
		DownloadManager dm = (DownloadManager) cell.getDataSource();
		if (dm != null) {
			name = dm.toString();
			int i = name.indexOf('#');
			if (i > 0) {
				name = name.substring(i + 1);
			}
		}

		if (name == null) {
			name = "";
		}
		return name;
	}
}
