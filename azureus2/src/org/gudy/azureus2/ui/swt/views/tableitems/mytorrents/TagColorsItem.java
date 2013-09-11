/*
 * File    : CategoryItem.java
 * Created : 01 feb. 2004
 * By      : TuxPaper
 *
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

package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumnInfo;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;

import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagManager;
import com.aelitis.azureus.core.tag.TagManagerFactory;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

/** Display Category torrent belongs to.
 *
 * @author TuxPaper
 */
public class TagColorsItem
       extends CoreTableColumnSWT 
       implements TableCellRefreshListener, TableCellSWTPaintListener
{
	private static TagManager tag_manager = TagManagerFactory.getTagManager();
	
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "tag_colors";

	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] { CAT_CONTENT });
	}

	/** Default Constructor */
	public TagColorsItem(String sTableID) {
		super(DATASOURCE_TYPE, COLUMN_ID, ALIGN_LEAD, 70, sTableID);
		setRefreshInterval(INTERVAL_LIVE);
	}

	public void refresh(TableCell cell) {
		String sTags = null;
		DownloadManager dm = (DownloadManager)cell.getDataSource();
		if (dm != null) {
			List<Tag> tags = tag_manager.getTagsForTaggable( TagType.TT_DOWNLOAD_MANUAL, dm );
			
			if ( tags.size() > 0 ){
				
				for ( Tag t: tags ){
											
					String str = t.getTagName( true );
						
					if ( sTags == null ){
						sTags = str;
					}else{
						sTags += ", " + str;
					}
				}
			}
		}
		cell.setSortValue( sTags );
		cell.setToolTip((sTags == null) ? "" : sTags );
	}
	
	public void cellPaint(GC gc, TableCellSWT cell) {

		DownloadManager dm = (DownloadManager)cell.getDataSource();
		
		List<Color> colors = new ArrayList<Color>();
		
		
		if (dm != null) {
			
			List<Tag> tags = tag_manager.getTagsForTaggable( TagType.TT_DOWNLOAD_MANUAL, dm );
			
			for ( Tag tag: tags ){
				
				int[] rgb = tag.getColor();
				
				if ( rgb != null && rgb.length == 3 ){
				
					colors.add( ColorCache.getColor( gc.getDevice(), rgb ));
				}
			}
		}
		
		int	num_colors = colors.size();
		
		if ( num_colors > 0 ){
			
			Rectangle bounds = cell.getBounds();

			bounds.x+=1;
			bounds.y+=1;
			bounds.width-=1;
			bounds.height-=1;
			
			if ( num_colors == 1 ){
				
				gc.setBackground( colors.get(0));
		
				gc.fillRectangle( bounds );
				
			}else{
				
				int	width = bounds.width;
				int	chunk = width/num_colors;
				
				if ( chunk == 0 ){
					chunk = 1;
				}
				
				bounds.width = chunk;
				
				for ( int i=0;i<num_colors;i++){
					
					if ( i == num_colors-1 ){
						
						int	rem = width - ( chunk * (num_colors-1 ));
						
						if ( rem > 0 ){
							
							bounds.width = rem;
						}
					}
					
					gc.setBackground( colors.get(i));
					
					gc.fillRectangle( bounds );
					
					bounds.x += chunk;
				}
			}
		}
	}
}
