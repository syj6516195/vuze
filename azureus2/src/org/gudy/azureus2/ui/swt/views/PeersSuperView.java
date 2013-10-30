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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.views.peer.PeerInfoView;
import org.gudy.azureus2.ui.swt.views.peer.RemotePieceDistributionView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;
import org.gudy.azureus2.ui.swt.views.tableitems.peers.DownloadNameItem;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableLifeCycleListener;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * AllPeersView
 * 
 * @author Olivier
 * @author TuxPaper
 *         2004/Apr/20: Use TableRowImpl instead of PeerRow
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2004/Apr/21: extends TableView instead of IAbstractView
 * @author MjrTom
 *			2005/Oct/08: Add PieceItem
 */

public class PeersSuperView
	extends TableViewTab<PEPeer>
	implements GlobalManagerListener, DownloadManagerPeerListener,
	TableLifeCycleListener, TableViewSWTMenuFillListener
{	
	private TableViewSWT<PEPeer> tv;
	private Shell shell;
	private boolean active_listener = true;
	
	protected static boolean registeredCoreSubViews = false;


  /**
   * Initialize
   *
   */
  public PeersSuperView() {
  	super("AllPeersView");
	}	

  // @see org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab#initYourTableView()
  public TableViewSWT<PEPeer> initYourTableView() {
  	TableColumnCore[] items = PeersView.getBasicColumnItems(TableManager.TABLE_ALL_PEERS);
  	TableColumnCore[] basicItems = new TableColumnCore[items.length + 1];
  	System.arraycopy(items, 0, basicItems, 0, items.length);
  	basicItems[items.length] = new DownloadNameItem(TableManager.TABLE_ALL_PEERS);

  	tv = TableViewFactory.createTableViewSWT(Peer.class, TableManager.TABLE_ALL_PEERS,
				getPropertiesPrefix(), basicItems, "connected_time", SWT.MULTI
						| SWT.FULL_SELECTION | SWT.VIRTUAL);
		tv.setRowDefaultHeight(16);
		tv.setEnableTabViews(true,true,null);

		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (uiFunctions != null) {
			UISWTInstance pluginUI = uiFunctions.getUISWTInstance();
			
			if (pluginUI != null && !registeredCoreSubViews) {

				pluginUI.addView(TableManager.TABLE_ALL_PEERS, "PeerInfoView",
						PeerInfoView.class, null);
				pluginUI.addView(TableManager.TABLE_ALL_PEERS,
						"RemotePieceDistributionView", RemotePieceDistributionView.class,
						null);
				pluginUI.addView(TableManager.TABLE_ALL_PEERS, "LoggerView",
						LoggerView.class, true);

				registeredCoreSubViews = true;
			}
		}

		tv.addLifeCycleListener(this);
		tv.addMenuFillListener(this);
		
  	return tv;
  }

	// @see com.aelitis.azureus.ui.common.table.TableLifeCycleListener#tableViewInitialized()
	public void tableViewInitialized() {
		shell = tv.getComposite().getShell();
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
		
			public void azureusCoreRunning(AzureusCore core) {
				registerGlobalManagerListener(core);
			}
		});
	}
		
	public void tableViewDestroyed() {
		unregisterListeners();
	}

	public void fillMenu(String sColumnName, final Menu menu) {
		PeersView.fillMenu(menu, tv, shell, null );
	}


  /* DownloadManagerPeerListener implementation */
  public void peerAdded(PEPeer created) {
    tv.addDataSource(created);
  }

  public void peerRemoved(PEPeer removed) {
    tv.removeDataSource(removed);
  }

	/**
	 * Add datasources already in existance before we called addListener.
	 * Faster than allowing addListener to call us one datasource at a time. 
	 * @param core 
	 */
	private void addExistingDatasources(AzureusCore core) {
		if (tv.isDisposed()) {
			return;
		}

		ArrayList<PEPeer> sources = new ArrayList<PEPeer>();
		Iterator<?> itr = core.getGlobalManager().getDownloadManagers().iterator();
		while (itr.hasNext()) {
			PEPeer[] peers = ((DownloadManager)itr.next()).getCurrentPeers();
			if (peers != null) {
				sources.addAll(Arrays.asList(peers));
			}
		}
		if (sources.isEmpty()) {
			return;
		}
		
		tv.addDataSources(sources.toArray(new PEPeer[sources.size()]));
		tv.processDataSourceQueue();
	}

	private void registerGlobalManagerListener(AzureusCore core) {
		this.active_listener = false;
		try {
			core.getGlobalManager().addListener(this);
		} finally {
			this.active_listener = true;
		}
		addExistingDatasources(core);
	}
	
	private void unregisterListeners() {
		try {
			GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
			gm.removeListener(this);
			Iterator<?> itr = gm.getDownloadManagers().iterator();
			while(itr.hasNext()) {
				DownloadManager dm = (DownloadManager)itr.next();
				downloadManagerRemoved(dm);
			}
		} catch (Exception e) {
		}
	}
	
	public void	downloadManagerAdded(DownloadManager dm) {
		dm.addPeerListener(this, !this.active_listener);
	}
	public void	downloadManagerRemoved(DownloadManager dm) {
		dm.removePeerListener(this);
	}
	
	// Methods I have to implement but have no need for...
	public void	destroyInitiated() {}		
	public void destroyed() {}
    public void seedingStatusChanged(boolean seeding_only_mode, boolean b) {}
	public void addThisColumnSubMenu(String columnName, Menu menuThisColumn) {}
	public void peerManagerAdded(PEPeerManager manager){}
	public void peerManagerRemoved(PEPeerManager manager) {}
	public void peerManagerWillBeAdded(PEPeerManager manager) {}
}
