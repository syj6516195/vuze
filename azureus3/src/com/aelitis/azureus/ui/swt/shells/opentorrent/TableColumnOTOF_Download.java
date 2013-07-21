/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */

package com.aelitis.azureus.ui.swt.shells.opentorrent;

import org.gudy.azureus2.core3.torrent.impl.TorrentOpenFileOptions;
import org.gudy.azureus2.plugins.ui.tables.*;

import com.aelitis.azureus.ui.swt.columns.ColumnCheckBox;

public class TableColumnOTOF_Download
	extends ColumnCheckBox
{
	public static final String COLUMN_ID = "download";

	/** Default Constructor */
	public TableColumnOTOF_Download(TableColumn column) {
		super(column, 60);
		column.setPosition(TableColumn.POSITION_LAST);
	}

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			TableColumn.CAT_ESSENTIAL,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	@Override
	protected Boolean getCheckBoxState(Object ds) {
		if (!(ds instanceof TorrentOpenFileOptions)) {
			return false;
		}
		return ((TorrentOpenFileOptions) ds).isToDownload();
	}

	@Override
	protected void setCheckBoxState(Object ds, boolean set) {
		if (!(ds instanceof TorrentOpenFileOptions)) {
			return;
		}
		((TorrentOpenFileOptions) ds).setToDownload(set);
	}
}
