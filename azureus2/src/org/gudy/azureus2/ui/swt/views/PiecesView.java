/*
 * Created on 2 juil. 2003
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
package org.gudy.azureus2.ui.swt.views;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.download.DownloadManagerPieceListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.components.Legend;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewEventImpl;
import org.gudy.azureus2.ui.swt.views.piece.MyPieceDistributionView;
import org.gudy.azureus2.ui.swt.views.piece.PieceInfoView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.tableitems.pieces.*;

import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableDataSourceChangedListener;
import com.aelitis.azureus.ui.common.table.TableLifeCycleListener;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.selectedcontent.SelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2004/Apr/21: extends TableView instead of IAbstractView
 * @author MjrTom
 *			2005/Oct/08: Add PriorityItem, SpeedItem
 */

public class PiecesView 
	extends TableViewTab<PEPiece>
	implements DownloadManagerPeerListener, 
	DownloadManagerPieceListener,
	TableDataSourceChangedListener,
	TableLifeCycleListener,
	UISWTViewCoreEventListener
{
	private static boolean registeredCoreSubViews = false;

	private final static TableColumnCore[] basicItems = {
		new PieceNumberItem(),
		new SizeItem(),
		new BlockCountItem(),
		new BlocksItem(),
		new CompletedItem(),
		new AvailabilityItem(),
		new TypeItem(),
		new ReservedByItem(),
		new WritersItem(),
		new PriorityItem(),
		new SpeedItem(),
		new RequestedItem()
	};

	static{
		TableColumnManager tcManager = TableColumnManager.getInstance();

		tcManager.setDefaultColumnNames( TableManager.TABLE_TORRENT_PIECES, basicItems );
	}
	
	public static final String MSGID_PREFIX = "PiecesView";

	private DownloadManager 		manager;
	private boolean					enable_tabs = true;
	private TableViewSWT<PEPiece> 	tv;

	private Composite legendComposite;

  
	/**
	 * Initialize
	 *
	 */
	public PiecesView() {
		super(MSGID_PREFIX);
	}

	// @see org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab#initYourTableView()
	public TableViewSWT<PEPiece> initYourTableView() {
		tv = TableViewFactory.createTableViewSWT(PEPiece.class,
				TableManager.TABLE_TORRENT_PIECES, getPropertiesPrefix(), basicItems,
				basicItems[0].getName(), SWT.SINGLE | SWT.FULL_SELECTION | SWT.VIRTUAL);
		tv.setEnableTabViews(enable_tabs,true,null);

		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (uiFunctions != null) {
			UISWTInstance pluginUI = uiFunctions.getUISWTInstance();
			
			if (pluginUI != null && !registeredCoreSubViews) {
				
				pluginUI.addView(TableManager.TABLE_TORRENT_PIECES,
						"PieceInfoView", PieceInfoView.class, manager);

				pluginUI.addView(TableManager.TABLE_TORRENT_PIECES,
						"MyPieceDistributionView", MyPieceDistributionView.class, manager);

				registeredCoreSubViews = true;
			}
		}

		tv.addTableDataSourceChangedListener(this, true);
		tv.addLifeCycleListener(this);

		return tv;
	}

	private boolean comp_focused;
	private Object focus_pending_ds;

	private void
	setFocused( boolean foc )
	{
		if ( foc ){

			comp_focused = true;

			dataSourceChanged( focus_pending_ds );

		}else{

			focus_pending_ds = manager;

			dataSourceChanged( null );

			comp_focused = false;
		}
	}
	  
	// @see com.aelitis.azureus.ui.common.table.TableDataSourceChangedListener#tableDataSourceChanged(java.lang.Object)
	public void tableDataSourceChanged(Object newDataSource) {
		if ( !comp_focused ){
			focus_pending_ds = newDataSource;
			return;
		}
	  	DownloadManager old_manager = manager;
		if (newDataSource == null){
			manager = null;
		}else if (newDataSource instanceof Object[]){
			Object temp = ((Object[])newDataSource)[0];
			if ( temp instanceof DownloadManager ){
				manager = (DownloadManager)temp;
			}else if ( temp instanceof DiskManagerFileInfo){
				manager = ((DiskManagerFileInfo)temp).getDownloadManager();
			}else{
				return;
			}
		}else{
			if ( newDataSource instanceof DownloadManager ){
				manager = (DownloadManager)newDataSource;
			}else if ( newDataSource instanceof DiskManagerFileInfo){
				manager = ((DiskManagerFileInfo)newDataSource).getDownloadManager();
			}else{
				return;
			}
		}
		
		if ( old_manager == manager ){
			return;
		}
		
		if (old_manager != null){
			old_manager.removePeerListener(this);
			old_manager.removePieceListener(this);
		}

		if ( !tv.isDisposed()){
			tv.removeAllTableRows();
			if (manager != null) {
				manager.addPeerListener(this, false);
				manager.addPieceListener(this, false);
				addExistingDatasources();
			}
		}
	}

	// @see com.aelitis.azureus.ui.common.table.TableLifeCycleListener#tableViewInitialized()
	public void tableViewInitialized() {
		if (legendComposite != null && tv != null) {
			Composite composite = ((TableViewSWT<PEPiece>) tv).getTableComposite();

			legendComposite = Legend.createLegendComposite(composite,
					BlocksItem.colors, new String[] {
					"PiecesView.legend.requested",
					"PiecesView.legend.written",        			
					"PiecesView.legend.downloaded",
						"PiecesView.legend.incache"
					});
	}

		if (manager != null) {
			manager.removePeerListener(this);
			manager.removePieceListener(this);
			manager.addPeerListener(this, false);
			manager.addPieceListener(this, false);
			addExistingDatasources();
    }
    }

	// @see com.aelitis.azureus.ui.common.table.TableLifeCycleListener#tableViewDestroyed()
	public void tableViewDestroyed() {
		if (legendComposite != null && legendComposite.isDisposed()) {
			legendComposite.dispose();
		}

		if (manager != null) {
			manager.removePeerListener(this);
			manager.removePieceListener(this);
		}
	}

	/* DownloadManagerPeerListener implementation */
	public void pieceAdded(PEPiece created) {
    tv.addDataSource(created);
	}

	public void pieceRemoved(PEPiece removed) {    
    tv.removeDataSource(removed);
	}

	public void peerAdded(PEPeer peer) {  }
	public void peerRemoved(PEPeer peer) {  }
  public void peerManagerWillBeAdded( PEPeerManager	peer_manager ){}
	public void peerManagerAdded(PEPeerManager manager) {	}
	public void peerManagerRemoved(PEPeerManager	manager) {
		tv.removeAllTableRows();
	}

	/**
	 * Add datasources already in existance before we called addListener.
	 * Faster than allowing addListener to call us one datasource at a time. 
	 */
	private void addExistingDatasources() {
		if (manager == null || tv.isDisposed()) {
			return;
		}

		PEPiece[] dataSources = manager.getCurrentPieces();
		if (dataSources == null || dataSources.length == 0)
			return;

		tv.addDataSources(dataSources);
  	tv.processDataSourceQueue();
	}

	/**
	 * @return the manager
	 */
	public DownloadManager getManager() {
		return manager;
	}
	
	public boolean eventOccurred(UISWTViewEvent event) {
	    switch (event.getType()) {
	     
	      case UISWTViewEvent.TYPE_CREATE:{
	    	  if ( event instanceof UISWTViewEventImpl ){
	    		  
	    		  String parent = ((UISWTViewEventImpl)event).getParentID();
	    		  
	    		  enable_tabs = parent != null && parent.equals( UISWTInstance.VIEW_TORRENT_DETAILS );
	    	  }
	    	  break;
	      }
	      case UISWTViewEvent.TYPE_FOCUSGAINED:
	      	String id = "DMDetails_Pieces";
	      	
	      	setFocused( true );	// do this here to pick up corrent manager before rest of code
	      	
	      	if (manager != null) {
	      		if (manager.getTorrent() != null) {
	  					id += "." + manager.getInternalName();
	      		} else {
	      			id += ":" + manager.getSize();
	      		}
	      	}
	  
	      	SelectedContentManager.changeCurrentlySelectedContent(id, new SelectedContent[] {
	      		new SelectedContent(manager)
	      	});
		 
		    break;
	      case UISWTViewEvent.TYPE_FOCUSLOST:
	    	  setFocused( false );
	    	  break;	
	    }
	    
	    return( super.eventOccurred(event));
	}
	
	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		if ( ViewUtils.toolBarItemActivated(manager, item, activationType, datasource)){
			return( true );
		}
		return( super.toolBarItemActivated(item, activationType, datasource));
	}

	public void refreshToolBarItems(Map<String, Long> list) {
		ViewUtils.refreshToolBarItems(manager, list);
		super.refreshToolBarItems(list);
	}
}
