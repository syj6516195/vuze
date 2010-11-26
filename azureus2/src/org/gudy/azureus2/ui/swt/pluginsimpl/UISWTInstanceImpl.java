/*
 * Created on 05-Sep-2005
 * Created by Paul Gardner
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.ui.swt.pluginsimpl;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AETemporaryFileHandler;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.ui.model.BasicPluginViewModel;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadImpl;
import org.gudy.azureus2.pluginsimpl.local.ui.UIManagerImpl;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.ui.swt.minibar.AllTransfersBar;
import org.gudy.azureus2.ui.swt.minibar.DownloadBar;
import org.gudy.azureus2.ui.swt.plugins.*;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.table.utils.TableContextMenuManager;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.ui.IUIIntializer;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableStructureEventDispatcher;
import com.aelitis.azureus.ui.common.table.impl.TableColumnImpl;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

public class 
UISWTInstanceImpl
	implements UIInstanceFactory, UISWTInstance, UIManagerEventListener
{
	private AzureusCore		core;
	
	private Map<UISWTAWTPluginView,UISWTPluginView> 			awt_view_map 	= new WeakHashMap<UISWTAWTPluginView,UISWTPluginView>();
	private Map<BasicPluginConfigModel,BasicPluginConfigImpl> 	config_view_map = new WeakHashMap<BasicPluginConfigModel,BasicPluginConfigImpl>();
	
	private Map<String,Map<String,UISWTViewEventListenerHolder>> views = new HashMap<String,Map<String,UISWTViewEventListenerHolder>>();
	
	private Map<PluginInterface,UIInstance>	plugin_map = new WeakHashMap<PluginInterface,UIInstance>();
	
	private boolean bUIAttaching;

	private final UIFunctionsSWT uiFunctions;
	
	
	public UISWTInstanceImpl(AzureusCore _core) {
		core		= _core;

		// Since this is a UI **SWT** Instance Implementor, it's assumed
		// that the UI Functions are of UIFunctionsSWT 
		uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
	}
	
	public void init(IUIIntializer init) {
		UIManager ui_manager = PluginInitializer.getDefaultInterface().getUIManager();
		ui_manager.addUIEventListener(this);
		
		bUIAttaching = true;
		
		((UIManagerImpl) ui_manager).attachUI(this, init);
		
		bUIAttaching = false;
	}
  
	public UIInstance
	getInstance(
		PluginInterface		plugin_interface )
	{
		UIInstance	instance = (UIInstance)plugin_map.get( plugin_interface );
		
		if ( instance == null ){
			
			instance = new instanceWrapper( plugin_interface, this );
			
			plugin_map.put( plugin_interface, instance );
		}
		
		return( instance );
	}
	
	public boolean
	eventOccurred(
		final UIManagerEvent	event )
	{
		boolean	done = true;
		
		final Object	data = event.getData();
		
		switch( event.getType()){
		
			case UIManagerEvent.ET_SHOW_TEXT_MESSAGE:
			{
				Utils.execSWTThread(
					new Runnable()
					{
						public void 
						run()
						{
							String[]	params = (String[])data;
							
							new TextViewerWindow( params[0], params[1], params[2] );
						}
					});
				
				break;
			}
			case UIManagerEvent.ET_SHOW_MSG_BOX:
			{
				final int[] result = { UIManagerEvent.MT_NONE };
				
					Utils.execSWTThread(
					new Runnable()
					{
						public void 
						run()
						{
							UIFunctionsManagerSWT.getUIFunctionsSWT().bringToFront();
							
							Object[]	params = (Object[])data;
							
							long	_styles = ((Long)(params[2])).longValue();
							
							int		styles	= 0;
							
							if (( _styles & UIManagerEvent.MT_YES ) != 0 ){
								
								styles |= SWT.YES;
							}
							if (( _styles & UIManagerEvent.MT_NO ) != 0 ){
								
								styles |= SWT.NO;
							}
							if (( _styles & UIManagerEvent.MT_OK ) != 0 ){
								
								styles |= SWT.OK;
							}
							if (( _styles & UIManagerEvent.MT_CANCEL ) != 0 ){
								
								styles |= SWT.CANCEL;
							}
							

							MessageBoxShell mb = new MessageBoxShell(styles, 
									MessageText.getString((String)params[0]), 
									MessageText.getString((String)params[1]));
							mb.open(null);
							int _r = mb.waitUntilClosed();
							
							int	r = 0;
							
							if (( _r & SWT.YES ) != 0 ){
								
								r |= UIManagerEvent.MT_YES;
							}
							if (( _r & SWT.NO ) != 0 ){
								
								r |= UIManagerEvent.MT_NO;
							}
							if (( _r & SWT.OK ) != 0 ){
								
								r |= UIManagerEvent.MT_OK;
							}
							if (( _r & SWT.CANCEL ) != 0 ){
								
								r |= UIManagerEvent.MT_CANCEL;
							}
							
							result[0] = r;
						}
					}, false );
				
				event.setResult( new Long( result[0] ));
				
				break;
			}
			case UIManagerEvent.ET_OPEN_TORRENT_VIA_FILE:
			{	
				TorrentOpener.openTorrent(((File)data).toString());

				break;
			}
			case UIManagerEvent.ET_OPEN_TORRENT_VIA_TORRENT:
			{	
				Torrent t = (Torrent)data;
				
				try{
					File f = AETemporaryFileHandler.createTempFile();
					
					t.writeToFile( f );
					
					TorrentOpener.openTorrent( f.toString());

				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
				}
				
				break;
			}
			case UIManagerEvent.ET_OPEN_TORRENT_VIA_URL:
			{
				Display display = SWTThread.getInstance().getDisplay();

				display.syncExec(new AERunnable() {
					public void runSupport() {
						Object[] params = (Object[]) data;

						URL 		target 				= (URL) params[0];
						URL 		referrer 			= (URL) params[1];
						boolean 	auto_download 		= ((Boolean) params[2]).booleanValue();
						Map			request_properties	= (Map)params[3];
						
						// programmatic request to add a torrent, make sure az is visible

						if (auto_download) {
							Shell shell = uiFunctions.getMainShell();
							if (shell != null) {
								new FileDownloadWindow(shell, target.toString(),
										referrer == null ? null : referrer.toString(),
										request_properties );
							}
						} else {

							// TODO: handle referrer?

							TorrentOpener.openTorrent(target.toString());
						}
					}
				});

				break;
			}
			case UIManagerEvent.ET_PLUGIN_VIEW_MODEL_CREATED:
			{
				if ( data instanceof BasicPluginViewModel ){
					BasicPluginViewModel model = (BasicPluginViewModel)data;
					
					// property bundles can't handle spaces in keys
					//
					// If this behaviour changes, change the openView(model)
					// method lower down.
					String sViewID = model.getName().replaceAll(" ", ".");
					BasicPluginViewImpl view = new BasicPluginViewImpl(model);
					addView(UISWTInstance.VIEW_MAIN, sViewID, view);
				}
				
				break;
			}
			case UIManagerEvent.ET_PLUGIN_VIEW_MODEL_DESTROYED:
			{
				if ( data instanceof BasicPluginViewModel ){
					BasicPluginViewModel model = (BasicPluginViewModel)data;
					// property bundles can't handle spaces in keys
					//
					// If this behaviour changes, change the openView(model)
					// method lower down.
					String sViewID = model.getName().replaceAll(" ", ".");
					removeViews(UISWTInstance.VIEW_MAIN, sViewID);
				}
				
				break;
			}
			case UIManagerEvent.ET_PLUGIN_CONFIG_MODEL_CREATED:
			{
				if ( data instanceof BasicPluginConfigModel ){
					
					BasicPluginConfigModel	model = (BasicPluginConfigModel)data;
					
					BasicPluginConfigImpl view = new BasicPluginConfigImpl( new WeakReference<BasicPluginConfigModel>( model ));
					   
					config_view_map.put( model, view );
					
					model.getPluginInterface().addConfigSection( view );
				}
				
				break;
			}
			case UIManagerEvent.ET_PLUGIN_CONFIG_MODEL_DESTROYED:
			{
				if ( data instanceof BasicPluginConfigModel ){
					
					BasicPluginConfigModel	model = (BasicPluginConfigModel)data;
					
					BasicPluginConfigImpl view = (BasicPluginConfigImpl)config_view_map.get( model );
					   
					if ( view != null ){
						
						model.getPluginInterface().removeConfigSection( view );
					}
				}
				
				break;
			}
			case UIManagerEvent.ET_COPY_TO_CLIPBOARD:
			{
				ClipboardCopy.copyToClipBoard((String)data);
				
				break;
			}
			case UIManagerEvent.ET_OPEN_URL:
			{
				Utils.launch(((URL)data).toExternalForm());
				
				break;
			}
			case UIManagerEvent.ET_CREATE_TABLE_COLUMN:{
				
				if (data instanceof TableColumn) {
					event.setResult((TableColumn) data);
				} else {
					String[] args = (String[]) data;

					event.setResult(new TableColumnImpl(args[0], args[1]));
				}
	 			
	 			break;
			} 
			case UIManagerEvent.ET_ADD_TABLE_COLUMN:{
				
				TableColumn	_col = (TableColumn)data;
				
				if ( _col instanceof TableColumnImpl ){
					
					TableColumnManager.getInstance().addColumns(new TableColumnCore[] {	(TableColumnCore) _col });
					
					TableStructureEventDispatcher tsed = TableStructureEventDispatcher.getInstance(_col.getTableID());
					
					tsed.tableStructureChanged(true, _col.getForDataSourceType());
					
				}else{
					
					throw(new UIRuntimeException("TableManager.addColumn(..) can only add columns created by createColumn(..)"));
				}
				
				break;
			} 
			case UIManagerEvent.ET_REGISTER_COLUMN:{
				
				Object[] params = (Object[])data;
				
				TableColumnManager tcManager = TableColumnManager.getInstance();
				
				Class 	dataSource 	= (Class)params[0];
				String	columnName	= (String)params[1];
				
				tcManager.registerColumn(dataSource, columnName, (TableColumnCreationListener)params[2]);
				
				String[] tables = tcManager.getTableIDs();
				
				for ( String tid: tables ){
					
						// we don't know which tables are affected at this point to refresh all. 
						// if this proves to be a performance issue then we would have to use the
						// datasource to derive affected tables somehow
					
					TableStructureEventDispatcher tsed = TableStructureEventDispatcher.getInstance( tid );
							
					tsed.tableStructureChanged(true, dataSource);
				}
				
				break;
			} 
			case UIManagerEvent.ET_UNREGISTER_COLUMN:{
				
				Object[] params = (Object[])data;
				
				TableColumnManager tcManager = TableColumnManager.getInstance();
				
				Class 	dataSource 	= (Class)params[0];
				String	columnName	= (String)params[1];

				tcManager.unregisterColumn(dataSource, columnName, (TableColumnCreationListener)params[2]);
				
				String[] tables = tcManager.getTableIDs();
				
				for ( String tid: tables ){
					
					TableColumnCore col = tcManager.getTableColumnCore( tid, columnName );
					
					if ( col != null ){
					
						col.remove();
					}
				}
				
				break;
			} 
			case UIManagerEvent.ET_ADD_TABLE_CONTEXT_MENU_ITEM:{
				TableContextMenuItem	item = (TableContextMenuItem)data;
				TableContextMenuManager.getInstance().addContextMenuItem(item);
				break;
			}
			case UIManagerEvent.ET_ADD_MENU_ITEM: {
				MenuItem item = (MenuItem)data;
				MenuItemManager.getInstance().addMenuItem(item);
				break;
			}
			case UIManagerEvent.ET_REMOVE_TABLE_CONTEXT_MENU_ITEM:{
				TableContextMenuItem item = (TableContextMenuItem)data;
				TableContextMenuManager.getInstance().removeContextMenuItem(item);
				break;
			}
			case UIManagerEvent.ET_REMOVE_MENU_ITEM: {
				MenuItem item = (MenuItem)data;
				MenuItemManager.getInstance().removeMenuItem(item);
				break;
			}			
			case UIManagerEvent.ET_SHOW_CONFIG_SECTION: {
				event.setResult(new Boolean(false));

				if (!(data instanceof String))
					break;

	    	event.setResult(Boolean.TRUE);
	    	
	    	uiFunctions.openView(UIFunctions.VIEW_CONFIG, data);

				break;
			}
			case UIManagerEvent.ET_FILE_OPEN: {
				File file_to_use = (File)data;
				Utils.launch(file_to_use.getAbsolutePath());
				break;
			}
			case UIManagerEvent.ET_FILE_SHOW: {
				File file_to_use = (File)data;
				final boolean use_open_containing_folder = COConfigurationManager.getBooleanParameter("MyTorrentsView.menu.show_parent_folder_enabled");
				ManagerUtils.open(file_to_use, use_open_containing_folder);
				break;
			}			
			
			default:
			{
				done	= false;
				
				break;
			}
		}
		
		return( done );
	}
	
	public Display 
	getDisplay() 
	{
		return SWTThread.getInstance().getDisplay();
	}
  
	public Image
	loadImage(
		String	resource )
	{
		throw( new RuntimeException( "plugin specific instance required" ));
	}
	
	protected Image  
	loadImage(
		PluginInterface	pi,
		String 			res ) 
	{
		InputStream is = pi.getPluginClassLoader().getResourceAsStream( res);
		
		if ( is != null ){
		        
			ImageData imageData = new ImageData(is);
		    
			return new Image(getDisplay(), imageData);
		}
		
		return null;
	}
	
	public UISWTGraphic 
	createGraphic(
		Image img) 
	{
		return new UISWTGraphicImpl(img);
	}
	
	public Shell createShell(int style) {
		Shell shell = ShellFactory.createMainShell(style);
		Utils.setShellIcon(shell);
		return shell;
	}
  

	/** @deprecated */
	public void addView(final UISWTPluginView view, boolean bAutoOpen) {
		// Currently used by firefrog.
		try {
			uiFunctions.addPluginView(view);
			if (bAutoOpen) {
				uiFunctions.openPluginView(view);
			}
		} catch (Throwable e) {
			// SWT not available prolly
		}
	} 
  

	public void removeView(UISWTPluginView view) {
		try {
			uiFunctions.removePluginView(view);
		} catch (Throwable e) {
			// SWT not available prolly
		}
	}

	/** @deprecated */
	public void
	addView(
		UISWTAWTPluginView			view,
		boolean						auto_open )
	{
		final WeakReference<UISWTAWTPluginView> view_ref = new WeakReference<UISWTAWTPluginView>( view );
		
		UISWTPluginView	v = 
			new UISWTPluginView()
			{
				Composite		composite;
				Component		component;
				
				boolean	first_paint = true;
				
				public String
				getPluginViewName()
				{
					return( view_ref.get().getPluginViewName());
				}
				
				public String 
				getFullTitle() 
				{
					return( view_ref.get().getPluginViewName());
				}
				 
				public void 
				initialize(
					Composite _composite )
				{
					first_paint	= true;
					
					composite	= _composite;
					
					Composite frame_composite = new Composite(composite, SWT.EMBEDDED);
	
					GridData data = new GridData(GridData.FILL_BOTH);
					
					frame_composite.setLayoutData(data);
	
					Frame	f = SWT_AWT.new_Frame(frame_composite);
	
					BorderLayout	layout = 
						new BorderLayout()
						{
							public void 
							layoutContainer(Container parent)
							{
								try{
									super.layoutContainer( parent );
								
								}finally{
									if ( first_paint ){
										
										first_paint	= false;
											
										view_ref.get().open( component );
									}
								}
							}
						};
					
					Panel	pan = new Panel( layout );
	
					f.add( pan );
							
					component	= view_ref.get().create();
					
					pan.add( component, BorderLayout.CENTER );
				}
				
				public Composite 
				getComposite()
				{
					return( composite );
				}
				
				public void 
				delete() 
				{
					super.delete();
					
					view_ref.get().delete( component );
				}
			};
			
		awt_view_map.put( view, v );
		
		addView( v, auto_open );
	}
	
	public void
	removeView(
		UISWTAWTPluginView		view )
	{
		UISWTPluginView	v = (UISWTPluginView)awt_view_map.remove(view );
		
		if ( v != null ){
			
			removeView( v );
		}
	}
	
	public void
	detach()
	
		throws UIException
	{
		throw( new UIException( "not supported" ));
	}


	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTInstance#addView(java.lang.String, java.lang.String, org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener)
	 */
	
	public void addView(String sParentID, String sViewID,
			final UISWTViewEventListener l) {
		addView( null, sParentID, sViewID, l );
	}
	
	public void addView( PluginInterface pi, String sParentID, final String sViewID,
			UISWTViewEventListener _l) 
	{
		final UISWTViewEventListenerHolder l = new UISWTViewEventListenerHolder( _l, pi );
		if (sParentID == null) {
			sParentID = UISWTInstance.VIEW_MAIN;
		}
		Map<String,UISWTViewEventListenerHolder> subViews = views.get(sParentID);
		if (subViews == null) {
			subViews = new HashMap<String,UISWTViewEventListenerHolder>();
			views.put(sParentID, subViews);
		}

		subViews.put(sViewID, l );

		if (sParentID.equals(UISWTInstance.VIEW_MAIN)) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					try {
						uiFunctions.addPluginView(sViewID, l );
					} catch (Throwable e) {
						// SWT not available prolly
					}
				}
			});
		}
	}
	
	// TODO: Remove views from PeersView, etc
	public void removeViews(String sParentID, final String sViewID) {
		Map<String,UISWTViewEventListenerHolder> subViews = views.get(sParentID);
		if (subViews == null)
			return;

		if (sParentID.equals(UISWTInstance.VIEW_MAIN)) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					try {
						if (uiFunctions != null) {
							uiFunctions.removePluginView(sViewID);
						}
					} catch (Throwable e) {
						// SWT not available prolly
					}
				}
			});
		}
		subViews.remove(sViewID);
	}

	public boolean openView(final String sParentID, final String sViewID,
			final Object dataSource) {
		return openView(sParentID, sViewID, dataSource, true);
	}
	
	public boolean openView(final String sParentID, final String sViewID,
			final Object dataSource, final boolean setfocus) {
		Map<String,UISWTViewEventListenerHolder> subViews = views.get(sParentID);
		if (subViews == null) {
			return false;
		}

		final UISWTViewEventListenerHolder l = subViews.get(sViewID);
		if (l == null) {
			return false;
		}

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (uiFunctions != null) {
					uiFunctions.openPluginView(
							sParentID, sViewID, l, dataSource,
							setfocus && !bUIAttaching);
				}
			}
		});

		return true;
	}

	public void openMainView(final String sViewID,
			UISWTViewEventListener l, Object dataSource) {
		openMainView(null,sViewID, l, dataSource, true);
	}
	
	public void openMainView(PluginInterface pi, String sViewID,
			UISWTViewEventListener l, Object dataSource) {
		openMainView( pi, sViewID, l, dataSource, true);
	}
	
	public void openMainView(final String sViewID,
			final UISWTViewEventListener l, final Object dataSource,
			final boolean setfocus) {
		openMainView( null, sViewID, l, dataSource, setfocus );
	}
	public void openMainView(final PluginInterface pi, final String sViewID,
			final UISWTViewEventListener _l, final Object dataSource,
			final boolean setfocus) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (uiFunctions != null) {
					UISWTViewEventListenerHolder l = new UISWTViewEventListenerHolder( _l, pi );
					
					uiFunctions.openPluginView(UISWTInstance.VIEW_MAIN, sViewID, l, dataSource, setfocus && !bUIAttaching);
				}
			}
		});
	}

	public UISWTView[] getOpenViews(String sParentID) {
		if (sParentID.equals(UISWTInstance.VIEW_MAIN)) {
			try {
				if (uiFunctions != null) {
					return uiFunctions.getPluginViews();
				}
			} catch (Throwable e) {
				// SWT not available prolly
			}
		}
		return new UISWTView[0];
	}
	
	// @see org.gudy.azureus2.plugins.ui.UIInstance#promptUser(java.lang.String, java.lang.String, java.lang.String[], int)
	public int promptUser(String title, String text, String[] options,
			int defaultOption) {
		
		MessageBoxShell mb = new MessageBoxShell(title, text, options,
				defaultOption);
		mb.open(null);
		// bah, no way to change this to use the UserPrompterResultListener trigger
		return mb.waitUntilClosed();
	}
	
	public void showDownloadBar(Download download, final boolean display) {
		if (!(download instanceof DownloadImpl)) {return;}
		final DownloadManager dm = ((DownloadImpl)download).getDownload();
		if (dm == null) {return;} // Not expecting this, but just in case...
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (display) {
					DownloadBar.open(dm, getDisplay().getActiveShell());
				}
				else {
					DownloadBar.close(dm);
				}
			}
		}, false);
	}
	
	public void showTransfersBar(final boolean display) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				if (display) {
					AllTransfersBar.open(core.getGlobalManager(), getDisplay().getActiveShell());
				}
				else {
					AllTransfersBar.close(core.getGlobalManager());
				}
			}
		}, false);
	}

	// Core Functions
	// ==============
	
	public Map<String,UISWTViewEventListenerHolder> getViewListeners(String sParentID) {
		return views.get(sParentID);
	}
	
	/**
	 * @return  Map(key=parentid, value=Map(key=id, value=UISWTViewEventListener))
	 *
	 * @since 3.1.1.1
	 */
	public Map<String,Map<String,UISWTViewEventListenerHolder>> getAllViews() {
		return views;
	}
	
	public UIInputReceiver getInputReceiver() {
		return new SimpleTextEntryWindow();
	}
	
	public UIMessage createMessage() {
		return new UIMessageImpl();
	}
	
	public UISWTStatusEntry createStatusEntry() {
		final UISWTStatusEntryImpl entry = new UISWTStatusEntryImpl();
		UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (functionsSWT == null) {
			Debug.outNoStack("No UIFunctionsSWT on createStatusEntry");
			return null;
		}
		
		MainStatusBar mainStatusBar = functionsSWT.getMainStatusBar();
		if (mainStatusBar == null) {
			Debug.outNoStack("No MainStatusBar on createStatusEntry");
			return null;
		}
		mainStatusBar.createStatusEntry(entry);
		
		return entry;
	}
	
	public boolean openView(BasicPluginViewModel model) {
		return openView(VIEW_MAIN, model.getName().replaceAll(" ", "."), null);
	}
	
	public void openConfig(final BasicPluginConfigModel model) {
		Utils.execSWTThread(new Runnable() {
			public void run() {
	    	uiFunctions.openView(UIFunctions.VIEW_CONFIG, model.getSection());
			}
		});
	}
	
	protected static class
	instanceWrapper
		implements UISWTInstance
	{
		private WeakReference<PluginInterface>		pi_ref;
		private UISWTInstanceImpl					delegate;
		
		protected
		instanceWrapper(
			PluginInterface		_pi,
			UISWTInstanceImpl	_delegate )
		{
			pi_ref		= new WeakReference<PluginInterface>(_pi );
			delegate	= _delegate;
		}
		
		public void
		detach()
		
			throws UIException
		{
			delegate.detach();
		}
	
		public Display
		getDisplay()
		{
			return( delegate.getDisplay());
		}
		
		public Image
		loadImage(
			String	resource )
		{
			PluginInterface pi = pi_ref.get();
			
			if ( pi == null ){
				
				return( null );
			}
			
			return( delegate.loadImage( pi, resource ));
		}
		
		public UISWTGraphic 
		createGraphic(
			Image img )
		{
			return( delegate.createGraphic( img ));
		}
		
		public void 
		addView(String sParentID, String sViewID, UISWTViewEventListener l)
		{
			PluginInterface pi = pi_ref.get();
			
			delegate.addView( pi, sParentID, sViewID, l );
		}

		public void 
		openMainView(String sViewID, UISWTViewEventListener l,Object dataSource)
		{
			PluginInterface pi = pi_ref.get();
			
			delegate.openMainView( pi, sViewID, l, dataSource );
		}

		public void 
		openMainView(String sViewID, UISWTViewEventListener l,Object dataSource, boolean setfocus)
		{
			PluginInterface pi = pi_ref.get();
			
			delegate.openMainView( pi, sViewID, l, dataSource, setfocus );
		}

		
		public void 
		removeViews(String sParentID, String sViewID)
		{
			delegate.removeViews(sParentID, sViewID );
		}


		public UISWTView[] 
		getOpenViews(String sParentID)
		{
			return( delegate.getOpenViews(sParentID));
		}


		public void 
		addView(UISWTPluginView view, boolean autoOpen)
		{
			delegate.addView( view, autoOpen );
		}


		public void 
		removeView(UISWTPluginView view)
		{
			delegate.removeView( view );
		}

		public void 
		addView(UISWTAWTPluginView view, boolean auto_open)
		{
			delegate.addView( view, auto_open );
		}


		public void 
		removeView(UISWTAWTPluginView view)
		{
			delegate.removeView( view );
		}
		
		public int promptUser(String title, String text, String[] options,
				int defaultOption) {
			return delegate.promptUser(title, text, options, defaultOption);
		}

		public boolean openView(String sParentID, String sViewID, Object dataSource) {
			return delegate.openView(sParentID, sViewID, dataSource);
		}
		
		public boolean openView(String sParentID, String sViewID, Object dataSource, boolean setfocus) {
			return delegate.openView(sParentID, sViewID, dataSource, setfocus);
		}
		
		public UIInputReceiver getInputReceiver() {
			return delegate.getInputReceiver();
		}
		
		public UIMessage createMessage() {
			return delegate.createMessage();
		}
		
		public void showDownloadBar(Download download, boolean display) {
			delegate.showDownloadBar(download, display);
		}
		
		public void showTransfersBar(boolean display) {
			delegate.showTransfersBar(display);
		}
		
		public UISWTStatusEntry createStatusEntry() {
			return delegate.createStatusEntry();
		}
		
		public boolean openView(BasicPluginViewModel model) {
			return delegate.openView(model);
		}

		public void openConfig(BasicPluginConfigModel model) {
			delegate.openConfig(model);
		}

		public Shell createShell(int style) {
			return delegate.createShell(style);
		}
		
	}
}
