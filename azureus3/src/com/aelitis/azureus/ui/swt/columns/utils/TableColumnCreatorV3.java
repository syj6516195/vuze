package com.aelitis.azureus.ui.swt.columns.utils;

import java.lang.reflect.Constructor;
import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.LightHashMap;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCoreCreationListener;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.*;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.swt.columns.torrent.*;
import com.aelitis.azureus.ui.swt.columns.vuzeactivity.*;

import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;

/**
 * A utility class for creating some common column sets; this is a virtual clone of <code>TableColumnCreator</code>
 * with slight modifications
 * @author khai
 *
 */
public class TableColumnCreatorV3
{

	public static int DATE_COLUMN_WIDTH = 90;

	public static TableColumnCore[] createIncompleteDM(String tableID, boolean big) {
		final String[] defaultVisibleOrder = {
			ColumnThumbnail.COLUMN_ID,
			NameItem.COLUMN_ID,
			"azsubs.ui.column.subs",
			SizeItem.COLUMN_ID,
			ColumnQuality.COLUMN_ID,
			ColumnInfo.COLUMN_ID,
			ColumnProgressETA.COLUMN_ID,
			CompletionItem.COLUMN_ID,
			StatusItem.COLUMN_ID,
			DownSpeedItem.COLUMN_ID,
			SeedsItem.COLUMN_ID,
			PeersItem.COLUMN_ID,
			ETAItem.COLUMN_ID,
		};

		TableColumnManager tcManager = TableColumnManager.getInstance();
		Map mapTCs = tcManager.getTableColumnsAsMap(DownloadTypeIncomplete.class,
				tableID);

		if (!tcManager.loadTableColumnSettings(DownloadTypeIncomplete.class,
				tableID)) {
			setVisibility(mapTCs, defaultVisibleOrder);
			NameItem tc = (NameItem) mapTCs.get(NameItem.COLUMN_ID);
			if (tc != null) {
				tcManager.setDefaultSortColumnName(tableID, NameItem.COLUMN_ID);
				tc.setSortAscending(true);
			}
		}

		// special changes
		StatusItem tcStatusItem = (StatusItem) mapTCs.get(StatusItem.COLUMN_ID);
		if (tcStatusItem != null) {
			tcStatusItem.setChangeRowFG(false);
		}
		NameItem tcNameItem = (NameItem) mapTCs.get(NameItem.COLUMN_ID);
		if (tcNameItem != null) {
			tcNameItem.setShowIcon(false);
		}

		return (TableColumnCore[]) mapTCs.values().toArray(new TableColumnCore[0]);
	}

	/**
	 * @param mapTCs
	 * @param defaultVisibleOrder
	 */
	private static void setVisibility(Map mapTCs, String[] defaultVisibleOrder) {
		for (Iterator iter = mapTCs.values().iterator(); iter.hasNext();) {
			TableColumnCore tc = (TableColumnCore) iter.next();
			tc.setVisible(false);
		}

		for (int i = 0; i < defaultVisibleOrder.length; i++) {
			String id = defaultVisibleOrder[i];
			TableColumnCore tc = (TableColumnCore) mapTCs.get(id);
			if (tc != null) {
				tc.setVisible(true);
				tc.setPositionNoShift(i);
			}
		}
	}

	public static TableColumnCore[] createCompleteDM(String tableID, boolean big) {
		final String[] defaultVisibleOrder = {
			ColumnUnopened.COLUMN_ID,
			ColumnThumbnail.COLUMN_ID,
			NameItem.COLUMN_ID,
			"azsubs.ui.column.subs",
			SizeItem.COLUMN_ID,
			ColumnQuality.COLUMN_ID,
			ColumnInfo.COLUMN_ID,
			ColumnRateUpDown.COLUMN_ID,
			StatusItem.COLUMN_ID,
			ShareRatioItem.COLUMN_ID,
			ColumnDateCompleted2Liner.COLUMN_ID,
		};

		TableColumnManager tcManager = TableColumnManager.getInstance();
		Map mapTCs = tcManager.getTableColumnsAsMap(DownloadTypeComplete.class,
				tableID);

		if (!tcManager.loadTableColumnSettings(DownloadTypeComplete.class, tableID)) {
			setVisibility(mapTCs, defaultVisibleOrder);
			ColumnDateCompleted2Liner tc = (ColumnDateCompleted2Liner) mapTCs.get(ColumnDateCompleted2Liner.COLUMN_ID);
			if (tc != null) {
				tcManager.setDefaultSortColumnName(tableID,
						ColumnDateCompleted2Liner.COLUMN_ID);
				tc.setSortAscending(false);
			}
		}

		// special changes
		StatusItem tcStatusItem = (StatusItem) mapTCs.get(StatusItem.COLUMN_ID);
		if (tcStatusItem != null) {
			tcStatusItem.setChangeRowFG(false);
		}
		NameItem tcNameItem = (NameItem) mapTCs.get(NameItem.COLUMN_ID);
		if (tcNameItem != null) {
			tcNameItem.setShowIcon(false);
		}

		return (TableColumnCore[]) mapTCs.values().toArray(new TableColumnCore[0]);
	}

	public static TableColumnCore[] createUnopenedDM(String tableID, boolean big) {
		final String[] defaultVisibleOrder = {
			ColumnUnopened.COLUMN_ID,
			ColumnThumbnail.COLUMN_ID,
			NameItem.COLUMN_ID,
			"azsubs.ui.column.subs",
			SizeItem.COLUMN_ID,
			ColumnQuality.COLUMN_ID,
			ColumnInfo.COLUMN_ID,
			StatusItem.COLUMN_ID,
			ColumnDateCompleted2Liner.COLUMN_ID,
		};

		TableColumnManager tcManager = TableColumnManager.getInstance();
		Map mapTCs = tcManager.getTableColumnsAsMap(DownloadTypeIncomplete.class,
				tableID);

		if (!tcManager.loadTableColumnSettings(DownloadTypeIncomplete.class,
				tableID)) {
			setVisibility(mapTCs, defaultVisibleOrder);
			ColumnDateCompleted2Liner tc = (ColumnDateCompleted2Liner) mapTCs.get(ColumnDateCompleted2Liner.COLUMN_ID);
			if (tc != null) {
				tcManager.setDefaultSortColumnName(tableID,
						ColumnDateCompleted2Liner.COLUMN_ID);
				tc.setSortAscending(false);
			}
		}

		// special changes
		StatusItem tcStatusItem = (StatusItem) mapTCs.get(StatusItem.COLUMN_ID);
		if (tcStatusItem != null) {
			tcStatusItem.setChangeRowFG(false);
		}
		NameItem tcNameItem = (NameItem) mapTCs.get(NameItem.COLUMN_ID);
		if (tcNameItem != null) {
			tcNameItem.setShowIcon(false);
		}

		return (TableColumnCore[]) mapTCs.values().toArray(new TableColumnCore[0]);
	}

	public static TableColumnCore[] createActivitySmall(String tableID) {
		final String[] defaultVisibleOrder = {
			ColumnActivityNew.COLUMN_ID,
			ColumnActivityType.COLUMN_ID,
			ColumnActivityText.COLUMN_ID,
			ColumnActivityActions.COLUMN_ID,
			ColumnActivityDate.COLUMN_ID,
		};
		TableColumnManager tcManager = TableColumnManager.getInstance();
		Map mapTCs = tcManager.getTableColumnsAsMap(VuzeActivitiesEntry.class,
				tableID);

		if (!tcManager.loadTableColumnSettings(VuzeActivitiesEntry.class, tableID)) {
			setVisibility(mapTCs, defaultVisibleOrder);
			ColumnActivityDate tc = (ColumnActivityDate) mapTCs.get(ColumnActivityDate.COLUMN_ID);
			if (tc != null) {
				tcManager.setDefaultSortColumnName(tableID,
						ColumnActivityDate.COLUMN_ID);
				tc.setSortAscending(false);
			}
		}

		return (TableColumnCore[]) mapTCs.values().toArray(new TableColumnCore[0]);
	}

	public static TableColumnCore[] createActivityBig(String tableID) {
		final String[] defaultVisibleOrder = {
			ColumnActivityNew.COLUMN_ID,
			ColumnActivityType.COLUMN_ID,
			ColumnActivityAvatar.COLUMN_ID,
			ColumnActivityText.COLUMN_ID,
			ColumnThumbnail.COLUMN_ID,
			ColumnActivityActions.COLUMN_ID,
			ColumnActivityDate.COLUMN_ID,
		};
		TableColumnManager tcManager = TableColumnManager.getInstance();
		Map mapTCs = tcManager.getTableColumnsAsMap(VuzeActivitiesEntry.class,
				tableID);

		if (!tcManager.loadTableColumnSettings(VuzeActivitiesEntry.class, tableID)) {
			setVisibility(mapTCs, defaultVisibleOrder);

			ColumnActivityText tcText = (ColumnActivityText) mapTCs.get(ColumnActivityText.COLUMN_ID);
			if (tcText != null) {
				tcText.setWidth(350);
			}
			ColumnActivityDate tc = (ColumnActivityDate) mapTCs.get(ColumnActivityDate.COLUMN_ID);
			if (tc != null) {
				tcManager.setDefaultSortColumnName(tableID,
						ColumnActivityDate.COLUMN_ID);
				tc.setSortAscending(false);
			}
		}

		return (TableColumnCore[]) mapTCs.values().toArray(new TableColumnCore[0]);
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	public static void initCoreColumns() {
		TableColumnCreator.initCoreColumns();

		// short variable names to reduce wrapping
		final Map c = new LightHashMap(7);
		final Class all = Download.class;
		final Class dl = DownloadTypeIncomplete.class;
		//final Class cd = DownloadTypeComplete.class;

		c.put(ColumnUnopened.COLUMN_ID, new cInfo(ColumnUnopened.class, all));
		//c.put(ColumnThumbnail.COLUMN_ID, new cInfo(ColumnThumbnail.class, all));
		c.put(ColumnQuality.COLUMN_ID, new cInfo(ColumnQuality.class, all));
		c.put(ColumnInfo.COLUMN_ID, new cInfo(ColumnInfo.class, all));
		c.put(ColumnRateUpDown.COLUMN_ID, new cInfo(ColumnRateUpDown.class, all));
		c.put(ColumnRatingGlobal.COLUMN_ID,
				new cInfo(ColumnRatingGlobal.class, all));
		c.put(ColumnVideoLength.COLUMN_ID, new cInfo(ColumnVideoLength.class, all));
		c.put(ColumnDateAdded2Liner.COLUMN_ID, new cInfo(
				ColumnDateAdded2Liner.class, all));
		c.put(ColumnDateCompleted2Liner.COLUMN_ID, new cInfo(
				ColumnDateCompleted2Liner.class, all));
		c.put(ColumnProgressETA.COLUMN_ID, new cInfo(ColumnProgressETA.class, dl));

		/////////

		final Class ac = VuzeActivitiesEntry.class;

		c.put(ColumnActivityNew.COLUMN_ID, new cInfo(ColumnActivityNew.class, ac));
		c.put(ColumnActivityAvatar.COLUMN_ID, new cInfo(ColumnActivityAvatar.class,
				ac));
		c.put(ColumnActivityType.COLUMN_ID, new cInfo(ColumnActivityType.class, ac));
		c.put(ColumnActivityText.COLUMN_ID, new cInfo(ColumnActivityText.class, ac));
		c.put(ColumnActivityActions.COLUMN_ID, new cInfo(
				ColumnActivityActions.class, ac));
		c.put(ColumnActivityDate.COLUMN_ID, new cInfo(ColumnActivityDate.class, ac));

		c.put(ColumnThumbnail.COLUMN_ID, new cInfo(ColumnThumbnail.class,
				new Class[] {
					ac,
					all
				}));

		// Core columns are implementors of TableColumn to save one class creation
		// Otherwise, we'd have to create a generic TableColumnImpl class, pass it 
		// to another class so that it could manipulate it and act upon changes.

		TableColumnManager tcManager = TableColumnManager.getInstance();

		TableColumnCoreCreationListener tcCreator = new TableColumnCoreCreationListener() {
			// @see org.gudy.azureus2.ui.swt.views.table.TableColumnCoreCreationListener#createTableColumnCore()
			public TableColumnCore createTableColumnCore(String tableID,
					String columnID) {
				cInfo info = (cInfo) c.get(columnID);

				try {
					Constructor constructor = info.cla.getDeclaredConstructor(new Class[] {
						String.class
					});
					TableColumnCore column = (TableColumnCore) constructor.newInstance(new Object[] {
						tableID
					});
					return column;
				} catch (Exception e) {
					Debug.out(e);
				}

				return null;
			}

			public void tableColumnCreated(TableColumn column) {
			}
		};

		for (Iterator iter = c.keySet().iterator(); iter.hasNext();) {
			String id = (String) iter.next();
			cInfo info = (cInfo) c.get(id);

			for (int i = 0; i < info.forDataSourceTypes.length; i++) {
				Class cla = info.forDataSourceTypes[i];

				tcManager.registerColumn(cla, id, tcCreator);
			}
		}

	}

	private static class cInfo
	{
		public Class cla;

		public Class[] forDataSourceTypes;

		public cInfo(Class cla, Class forDataSourceType) {
			this.cla = cla;
			this.forDataSourceTypes = new Class[] {
				forDataSourceType
			};
		}

		public cInfo(Class cla, Class[] forDataSourceTypes) {
			this.cla = cla;
			this.forDataSourceTypes = forDataSourceTypes;
		}
	}
}
