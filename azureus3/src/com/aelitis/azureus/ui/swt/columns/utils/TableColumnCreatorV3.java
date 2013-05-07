package com.aelitis.azureus.ui.swt.columns.utils;

import java.lang.reflect.Constructor;
import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.LightHashMap;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadTypeComplete;
import org.gudy.azureus2.plugins.download.DownloadTypeIncomplete;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.*;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableColumnCoreCreationListener;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.swt.columns.torrent.*;
import com.aelitis.azureus.ui.swt.columns.vuzeactivity.*;

/**
 * A utility class for creating some common column sets; this is a virtual clone of <code>TableColumnCreator</code>
 * with slight modifications
 * @author khai
 *
 */
public class TableColumnCreatorV3
{
	/**
	 * @param tableMytorrentsAllBig
	 * @param b
	 * @return
	 *
	 * @since 4.0.0.1
	 */
	public static TableColumnCore[] createAllDM(String tableID, boolean big) {
		final String[] oldVisibleOrder = {
			ColumnUnopened.COLUMN_ID,
			ColumnThumbAndName.COLUMN_ID,
			ColumnStream.COLUMN_ID,
			SizeItem.COLUMN_ID,
			ColumnProgressETA.COLUMN_ID,
			"azsubs.ui.column.subs",
			StatusItem.COLUMN_ID,
			ColumnTorrentSpeed.COLUMN_ID,
			SeedsItem.COLUMN_ID,
			PeersItem.COLUMN_ID,
			ShareRatioItem.COLUMN_ID
		};
		final String[] defaultVisibleOrder = {
			RankItem.COLUMN_ID,
			ColumnThumbAndName.COLUMN_ID,
			ColumnStream.COLUMN_ID,
			ColumnProgressETA.COLUMN_ID,
			SizeItem.COLUMN_ID,
			ColumnTorrentSpeed.COLUMN_ID,
			ETAItem.COLUMN_ID,
			//DateCompletedItem.COLUMN_ID,
			"RatingColumn",
			"azsubs.ui.column.subs",
			DateAddedItem.COLUMN_ID
		};

		TableColumnManager tcManager = TableColumnManager.getInstance();
		Map<String, TableColumnCore> mapTCs = tcManager.getTableColumnsAsMap(
				Download.class, tableID);

		tcManager.setDefaultColumnNames(tableID, defaultVisibleOrder);

		if (!tcManager.loadTableColumnSettings(Download.class, tableID)
				|| areNoneVisible(mapTCs)) {
			setVisibility(mapTCs, defaultVisibleOrder, true);
			RankItem tc = (RankItem) mapTCs.get(RankItem.COLUMN_ID);
			if (tc != null) {
				tcManager.setDefaultSortColumnName(tableID, RankItem.COLUMN_ID);
				tc.setSortAscending(true);
			}
		} else {
			upgradeColumns(oldVisibleOrder, defaultVisibleOrder, mapTCs);
		}

		// special changes
		StatusItem tcStatusItem = (StatusItem) mapTCs.get(StatusItem.COLUMN_ID);
		if (tcStatusItem != null) {
			tcStatusItem.setChangeRowFG(false);
			if (big) {
				tcStatusItem.setChangeCellFG(false);
				tcStatusItem.setShowTrackerErrors(true);
			}
		}
		if (big) {
			ShareRatioItem tcShareRatioItem = (ShareRatioItem) mapTCs.get(ShareRatioItem.COLUMN_ID);
			if (tcShareRatioItem != null) {
				tcShareRatioItem.setChangeFG(false);
				tcShareRatioItem.setWidth(80);
			}
		}

		return mapTCs.values().toArray(new TableColumnCore[0]);
	}

	private static void upgradeColumns(String[] oldOrder, String[] newOrder,
			Map<String, TableColumnCore> mapTCs) {
		List<String> listCurrentOrder = new ArrayList<String>();

		for (TableColumnCore tc : mapTCs.values()) {
			if (tc.isVisible()) {
				listCurrentOrder.add(tc.getName());
			}
		}

		System.out.println("upgradeColumns; old=" + oldOrder.length + ";cur=" + listCurrentOrder.size() + ";" + Debug.getCompressedStackTrace());
		if (oldOrder.length == listCurrentOrder.size()) {
			List<String> listOldOrder = Arrays.asList(oldOrder);
			if (listOldOrder.containsAll(listCurrentOrder)) {
				// exactly the same items (perhaps in different order) -- upgrade to new list
				System.out.println("upgradeColumns: SAME -> UPGRADING!");
				setVisibility(mapTCs, newOrder, true);
			}
		} else if (listCurrentOrder.size() > oldOrder.length) {
			List<String> listNewOrder = Arrays.asList(newOrder);
			if (listCurrentOrder.containsAll(listNewOrder)) {
				System.out.println("upgradeColumns: has all old plus -> UPGRADING!");
				// Current visible has all of old order, plus some added ones
				// make all new columns visible (keep old ones visible too)
				for (String id : newOrder) {
					TableColumnCore tc = mapTCs.get(id);
					if (tc != null) {
						tc.setVisible(true);
					}
				}
			}
		}
	}

	public static TableColumnCore[] createIncompleteDM(String tableID, boolean big) {
		final String[] oldVisibleOrder = {
			ColumnThumbAndName.COLUMN_ID,
			ColumnStream.COLUMN_ID,
			SizeItem.COLUMN_ID,
			ColumnFileCount.COLUMN_ID,
			ColumnProgressETA.COLUMN_ID,
			SeedsItem.COLUMN_ID,
			PeersItem.COLUMN_ID,
			"azsubs.ui.column.subs",
		};
		final String[] defaultVisibleOrder = {
			RankItem.COLUMN_ID,
			ColumnThumbAndName.COLUMN_ID,
			ColumnStream.COLUMN_ID,
			ColumnProgressETA.COLUMN_ID,
			SizeItem.COLUMN_ID,
			ColumnTorrentSpeed.COLUMN_ID,
			ETAItem.COLUMN_ID,
			"RatingColumn",
			"azsubs.ui.column.subs",
			DateAddedItem.COLUMN_ID
		};

		TableColumnManager tcManager = TableColumnManager.getInstance();
		Map<String, TableColumnCore> mapTCs = tcManager.getTableColumnsAsMap(
				DownloadTypeIncomplete.class, tableID);

		tcManager.setDefaultColumnNames(tableID, defaultVisibleOrder);

		if (!tcManager.loadTableColumnSettings(DownloadTypeIncomplete.class,
				tableID) || areNoneVisible(mapTCs)) {
			setVisibility(mapTCs, defaultVisibleOrder, true);
			RankItem tc = (RankItem) mapTCs.get(RankItem.COLUMN_ID);
			if (tc != null) {
				tcManager.setDefaultSortColumnName(tableID, RankItem.COLUMN_ID);
				tc.setSortAscending(true);
			}
		} else {
			upgradeColumns(oldVisibleOrder, defaultVisibleOrder, mapTCs);
		}

		// special changes
		StatusItem tcStatusItem = (StatusItem) mapTCs.get(StatusItem.COLUMN_ID);
		if (tcStatusItem != null) {
			tcStatusItem.setChangeRowFG(false);
			if (big) {
				tcStatusItem.setChangeCellFG(false);
			}
		}

		if (big) {
			ShareRatioItem tcShareRatioItem = (ShareRatioItem) mapTCs.get(ShareRatioItem.COLUMN_ID);
			if (tcShareRatioItem != null) {
				tcShareRatioItem.setChangeFG(false);
				tcShareRatioItem.setWidth(80);
			}
		}

		return mapTCs.values().toArray(new TableColumnCore[0]);
	}

	/**
	 * @param mapTCs
	 * @param defaultVisibleOrder
	 */
	private static void setVisibility(Map mapTCs, String[] defaultVisibleOrder,
			boolean reorder) {
		for (Iterator iter = mapTCs.values().iterator(); iter.hasNext();) {
			TableColumnCore tc = (TableColumnCore) iter.next();
			Long force_visible = (Long) tc.getUserData(TableColumn.UD_FORCE_VISIBLE);
			if (force_visible == null || force_visible == 0) {

				tc.setVisible(false);
			}
		}

		for (int i = 0; i < defaultVisibleOrder.length; i++) {
			String id = defaultVisibleOrder[i];
			TableColumnCore tc = (TableColumnCore) mapTCs.get(id);
			if (tc != null) {
				tc.setVisible(true);
				if (reorder) {
					tc.setPositionNoShift(i);
				}
			}
		}
	}

	public static TableColumnCore[] createCompleteDM(String tableID, boolean big) {
		final String[] oldVisibleOrder = {
			ColumnUnopened.COLUMN_ID,
			ColumnThumbAndName.COLUMN_ID,
			"RatingColumn",
			"azsubs.ui.column.subs",
			SizeItem.COLUMN_ID,
			StatusItem.COLUMN_ID,
			ShareRatioItem.COLUMN_ID,
			DateCompletedItem.COLUMN_ID,
		};
		final String[] defaultVisibleOrder = {
			RankItem.COLUMN_ID,
			ColumnThumbAndName.COLUMN_ID,
			ColumnStream.COLUMN_ID,
			StatusItem.COLUMN_ID,
			SizeItem.COLUMN_ID,
			ColumnTorrentSpeed.COLUMN_ID,
			"RatingColumn",
			"azsubs.ui.column.subs",
			DateCompletedItem.COLUMN_ID
		};

		TableColumnManager tcManager = TableColumnManager.getInstance();
		Map mapTCs = tcManager.getTableColumnsAsMap(DownloadTypeComplete.class,
				tableID);

		tcManager.setDefaultColumnNames(tableID, defaultVisibleOrder);

		if (!tcManager.loadTableColumnSettings(DownloadTypeComplete.class, tableID)
				|| areNoneVisible(mapTCs)) {
			setVisibility(mapTCs, defaultVisibleOrder, true);
			DateCompletedItem tc = (DateCompletedItem) mapTCs.get(DateCompletedItem.COLUMN_ID);
			if (tc != null) {
				tcManager.setDefaultSortColumnName(tableID, DateCompletedItem.COLUMN_ID);
				tc.setSortAscending(false);
			}
		} else {
			upgradeColumns(oldVisibleOrder, defaultVisibleOrder, mapTCs);
		}

		// special changes
		StatusItem tcStatusItem = (StatusItem) mapTCs.get(StatusItem.COLUMN_ID);
		if (tcStatusItem != null) {
			tcStatusItem.setChangeRowFG(false);
			if (big) {
				tcStatusItem.setChangeCellFG(false);
			}
		}
		if (big) {
			ShareRatioItem tcShareRatioItem = (ShareRatioItem) mapTCs.get(ShareRatioItem.COLUMN_ID);
			if (tcShareRatioItem != null) {
				tcShareRatioItem.setChangeFG(false);
				tcShareRatioItem.setWidth(80);
			}
		}

		return (TableColumnCore[]) mapTCs.values().toArray(new TableColumnCore[0]);
	}

	public static TableColumnCore[] createUnopenedDM(String tableID, boolean big) {
		final String[] oldVisibleOrder = {
			ColumnUnopened.COLUMN_ID,
			ColumnThumbAndName.COLUMN_ID,
			"azsubs.ui.column.subs",
			SizeItem.COLUMN_ID,
			ColumnProgressETA.COLUMN_ID,
			StatusItem.COLUMN_ID,
			DateCompletedItem.COLUMN_ID,
		};
		final String[] defaultVisibleOrder = {
			ColumnUnopened.COLUMN_ID,
			ColumnThumbAndName.COLUMN_ID,
			ColumnStream.COLUMN_ID,
			SizeItem.COLUMN_ID,
			"RatingColumn",
			"azsubs.ui.column.subs",
			DateCompletedItem.COLUMN_ID,
		};

		TableColumnManager tcManager = TableColumnManager.getInstance();
		Map mapTCs = tcManager.getTableColumnsAsMap(DownloadTypeComplete.class,
				tableID);

		tcManager.setDefaultColumnNames(tableID, defaultVisibleOrder);

		if (!tcManager.loadTableColumnSettings(DownloadTypeComplete.class, tableID)
				|| areNoneVisible(mapTCs)) {
			setVisibility(mapTCs, defaultVisibleOrder, true);
			DateCompletedItem tc = (DateCompletedItem) mapTCs.get(DateCompletedItem.COLUMN_ID);
			if (tc != null) {
				tcManager.setDefaultSortColumnName(tableID, DateCompletedItem.COLUMN_ID);
				tc.setSortAscending(false);
			}
		} else {
			upgradeColumns(oldVisibleOrder, defaultVisibleOrder, mapTCs);
		}

		// special changes
		StatusItem tcStatusItem = (StatusItem) mapTCs.get(StatusItem.COLUMN_ID);
		if (tcStatusItem != null) {
			tcStatusItem.setChangeRowFG(false);
			if (big) {
				tcStatusItem.setChangeCellFG(false);
			}
		}
		if (big) {
			ShareRatioItem tcShareRatioItem = (ShareRatioItem) mapTCs.get(ShareRatioItem.COLUMN_ID);
			if (tcShareRatioItem != null) {
				tcShareRatioItem.setChangeFG(false);
				tcShareRatioItem.setWidth(80);
			}
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

		tcManager.setDefaultColumnNames(tableID, defaultVisibleOrder);

		if (!tcManager.loadTableColumnSettings(VuzeActivitiesEntry.class, tableID)
				|| areNoneVisible(mapTCs)) {
			setVisibility(mapTCs, defaultVisibleOrder, true);
			ColumnActivityDate tc = (ColumnActivityDate) mapTCs.get(ColumnActivityDate.COLUMN_ID);
			if (tc != null) {
				tcManager.setDefaultSortColumnName(tableID,
						ColumnActivityDate.COLUMN_ID);
				tc.setSortAscending(false);
			}
			ColumnActivityText tcText = (ColumnActivityText) mapTCs.get(ColumnActivityText.COLUMN_ID);
			if (tcText != null) {
				tcText.setWidth(445);
			}
		}

		return (TableColumnCore[]) mapTCs.values().toArray(new TableColumnCore[0]);
	}

	public static TableColumnCore[] createActivityBig(String tableID) {
		final String[] defaultVisibleOrder = {
			ColumnActivityNew.COLUMN_ID,
			ColumnActivityType.COLUMN_ID,
			ColumnActivityText.COLUMN_ID,
			ColumnThumbnail.COLUMN_ID,
			ColumnActivityActions.COLUMN_ID,
			ColumnActivityDate.COLUMN_ID,
		};
		TableColumnManager tcManager = TableColumnManager.getInstance();
		Map mapTCs = tcManager.getTableColumnsAsMap(VuzeActivitiesEntry.class,
				tableID);

		tcManager.setDefaultColumnNames(tableID, defaultVisibleOrder);

		if (!tcManager.loadTableColumnSettings(VuzeActivitiesEntry.class, tableID)
				|| areNoneVisible(mapTCs)) {
			setVisibility(mapTCs, defaultVisibleOrder, true);

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
	private static boolean areNoneVisible(Map mapTCs) {
		boolean noneVisible = true;
		for (Iterator iter = mapTCs.values().iterator(); iter.hasNext();) {
			TableColumn tc = (TableColumn) iter.next();
			if (tc.isVisible()) {
				noneVisible = false;
				break;
			}
		}
		return noneVisible;
	}

	/**
	 * 
	 *
	 * @since 3.1.1.1
	 */
	public static void initCoreColumns() {
		TableColumnCreator.initCoreColumns();

		// short variable names to reduce wrapping
		final Map<String, cInfo> c = new LightHashMap<String, cInfo>(7);

		c.put(ColumnUnopened.COLUMN_ID, new cInfo(ColumnUnopened.class,
				ColumnUnopened.DATASOURCE_TYPE));
		c.put(ColumnThumbAndName.COLUMN_ID, new cInfo(ColumnThumbAndName.class,
				ColumnThumbAndName.DATASOURCE_TYPES));
		c.put(ColumnStream.COLUMN_ID, new cInfo(ColumnStream.class,
				ColumnStream.DATASOURCE_TYPES));
		c.put(DateAddedItem.COLUMN_ID, new cInfo(DateAddedItem.class,
				DateAddedItem.DATASOURCE_TYPE));
		c.put(DateCompletedItem.COLUMN_ID, new cInfo(DateCompletedItem.class,
				DateCompletedItem.DATASOURCE_TYPE));
		c.put(ColumnProgressETA.COLUMN_ID, new cInfo(ColumnProgressETA.class,
				ColumnProgressETA.DATASOURCE_TYPE));

		/////////

		final Class ac = VuzeActivitiesEntry.class;

		c.put(ColumnActivityNew.COLUMN_ID, new cInfo(ColumnActivityNew.class, ac));
		c.put(ColumnActivityType.COLUMN_ID, new cInfo(ColumnActivityType.class, ac));
		c.put(ColumnActivityText.COLUMN_ID, new cInfo(ColumnActivityText.class, ac));
		c.put(ColumnActivityActions.COLUMN_ID, new cInfo(
				ColumnActivityActions.class, ac));
		c.put(ColumnActivityDate.COLUMN_ID, new cInfo(ColumnActivityDate.class, ac));

		c.put(ColumnThumbnail.COLUMN_ID, new cInfo(ColumnThumbnail.class,
				new Class[] {
					ac,
				}));

		// Core columns are implementors of TableColumn to save one class creation
		// Otherwise, we'd have to create a generic TableColumnImpl class, pass it 
		// to another class so that it could manipulate it and act upon changes.

		TableColumnManager tcManager = TableColumnManager.getInstance();

		TableColumnCoreCreationListener tcCreator = new TableColumnCoreCreationListener() {
			// @see org.gudy.azureus2.ui.swt.views.table.TableColumnCoreCreationListener#createTableColumnCore(java.lang.Class, java.lang.String, java.lang.String)
			public TableColumnCore createTableColumnCore(Class forDataSourceType,
					String tableID, String columnID) {
				cInfo info = c.get(columnID);

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

		tcManager.unregisterColumn(NameItem.DATASOURCE_TYPE, NameItem.COLUMN_ID,
				null);

		for (Iterator<String> iter = c.keySet().iterator(); iter.hasNext();) {
			String id = iter.next();
			cInfo info = c.get(id);

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
