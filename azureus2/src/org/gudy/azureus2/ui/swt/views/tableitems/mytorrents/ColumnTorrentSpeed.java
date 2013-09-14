/**
 * 
 */
package org.gudy.azureus2.ui.swt.views.tableitems.mytorrents;

import org.eclipse.swt.graphics.Image;

import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.CoreTableColumnSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

/**
 * @author TuxPaper
 * @created Jul 11, 2010
 *
 */
public class ColumnTorrentSpeed
	extends CoreTableColumnSWT
	implements TableCellRefreshListener
{
	public static final Class DATASOURCE_TYPE = Download.class;

	public static final String COLUMN_ID = "torrentspeed";

	private Image imgUp;

	private Image imgDown;

	public ColumnTorrentSpeed(String tableID) {
		super(COLUMN_ID, 80, tableID);
		setAlignment(ALIGN_TRAIL);
		setType(TableColumn.TYPE_TEXT);
    setRefreshInterval(INTERVAL_LIVE);
    setUseCoreDataSource(false);
    
    ImageLoader imageLoader = ImageLoader.getInstance();
    imgUp = imageLoader.getImage("image.torrentspeed.up");
    imgDown = imageLoader.getImage("image.torrentspeed.down");
	}
	
	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
			CAT_ESSENTIAL,
			CAT_BYTES,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}

  public void refresh(TableCell cell) {
	Object ds = cell.getDataSource();
  	if (!(ds instanceof Download)) {
  		return;
  	}
    Download dm = (Download)ds;
    long value;
    long sortValue;
    String prefix = "";

    int iState;
    iState = dm.getState();
    if (iState == Download.ST_DOWNLOADING) {
    	value = dm.getStats().getDownloadAverage();
    	((TableCellSWT)cell).setIcon(imgDown);
    } else if (iState == Download.ST_SEEDING) {
    	value = dm.getStats().getUploadAverage();
    	((TableCellSWT)cell).setIcon(imgUp);
    } else {
    	((TableCellSWT)cell).setIcon(null);
    	value = 0;
    }
    sortValue = (value << 4) | iState;
  
    
    if (cell.setSortValue(sortValue) || !cell.isValid()) {
    	cell.setText(value > 0 ? prefix + DisplayFormatters.formatByteCountToKiBEtcPerSec(value) : "");
    }
  }

}
