/**
 * Copyright (C) 2013 Azureus Software, Inc. All Rights Reserved.
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
 */

package com.aelitis.azureus.ui.swt.columns.tag;

import java.io.File;

import org.gudy.azureus2.plugins.ui.tables.*;

import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagFeatureFileLocation;

public class ColumnTagCopyOnComp
	implements TableCellRefreshListener, TableColumnExtraInfoListener
{
	public static String COLUMN_ID = "tag.copyoncomp";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			TableColumn.CAT_ESSENTIAL,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

	/** Default Constructor */
	public ColumnTagCopyOnComp(TableColumn column) {
		column.setWidth(200);
		column.addListeners(this);
	}

	public void refresh(TableCell cell) {
		Tag tag = (Tag) cell.getDataSource();
		if ( tag instanceof TagFeatureFileLocation ){
			TagFeatureFileLocation fl = (TagFeatureFileLocation)tag;
			
			if ( fl.supportsTagCopyOnComplete()){
	
				File target_file = fl.getTagCopyOnCompleteFolder();
				
				String target;
				
				if ( target_file == null ){
					target = "";
				}else{
					target = target_file.getAbsolutePath();
				}
				
				if (!cell.setSortValue(target) && cell.isValid()) {
					return;
				}
		
				if (!cell.isShown()) {
					return;
				}
				
				cell.setText(target);
			}
		}
	}
}
