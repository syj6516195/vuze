/*
 * File    : MySharesView.java
 * Created : 18-Jan-2004
 * By      : parg
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */


package org.gudy.azureus2.ui.swt.views;

import com.aelitis.azureus.core.AzureusCore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.category.CategoryManagerListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.plugins.torrent.TorrentAttribute;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentManagerImpl;
import org.gudy.azureus2.ui.swt.Alerts;
import org.gudy.azureus2.ui.swt.CategoryAdderWindow;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;
import org.gudy.azureus2.ui.swt.views.tableitems.myshares.CategoryItem;
import org.gudy.azureus2.ui.swt.views.tableitems.myshares.NameItem;
import org.gudy.azureus2.ui.swt.views.tableitems.myshares.TypeItem;

import java.util.Arrays;
import java.util.List;

/**
 * @author parg
 * @author TuxPaper
 *         2004/Apr/20: Remove need for tableItemToObject
 *         2004/Apr/21: extends TableView instead of IAbstractView
 */
public class 
MySharesView 
	extends TableView
	implements ShareManagerListener, CategoryManagerListener
{
  private static final TableColumnCore[] basicItems = {
    new NameItem(),
    new TypeItem(),
	new CategoryItem(),
  };
  
	protected static final TorrentAttribute	category_attribute = 
		TorrentManagerImpl.getSingleton().getAttribute( TorrentAttribute.TA_CATEGORY );

  	private AzureusCore		azureus_core;
  	
	private GlobalManager	global_manager;
	
	private Menu			menuCategory;
	
	public 
	MySharesView(
		AzureusCore	_azureus_core )
	{	
    super(TableManager.TABLE_MYSHARES, "MySharesView", basicItems, "name", 
          SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.VIRTUAL);
    
    	azureus_core	= _azureus_core;
		global_manager = azureus_core.getGlobalManager();
	}
	 
	public void 
	initialize(
			Composite composite) 
	{
		super.initialize(composite);

		getTable().addMouseListener(new MouseAdapter() {
		   public void mouseDoubleClick(MouseEvent mEvent) {
			 TableItem[] tis = getTable().getSelection();
			 if (tis.length == 0) {
			   return;
			 }
			 ShareResource share = (ShareResource)getFirstSelectedDataSource();
			 
			 if (share != null){
			 	
			 	List dms = global_manager.getDownloadManagers();
			 	
			 	for (int i=0;i<dms.size();i++){
			 		
			 		DownloadManager	dm = (DownloadManager)dms.get(i);
			 		
			 		try{
				 		byte[]	share_hash = null;
				 		
				 		if ( share.getType() == ShareResource.ST_DIR ){
				 			
				 			share_hash = ((ShareResourceDir)share).getItem().getTorrent().getHash();
				 			
				 		}else if ( share.getType() == ShareResource.ST_FILE ){
				 			
				 			share_hash = ((ShareResourceFile)share).getItem().getTorrent().getHash();
				 		}
				 		
				 		if ( Arrays.equals( share_hash, dm.getTorrent().getHash())){
				 		
						 	MainWindow.getWindow().openManagerView(dm);
						 	
						 	break;
				 		}
			 		}catch( Throwable e ){
			 			
			 			Debug.printStackTrace( e );
			 		}
			 	}
			 }
		   }
		 });	
		 
		createRows();
		 
	    CategoryManager.addCategoryManagerListener(this);
	}

  private void createRows() {
		try{

			ShareManager	sm = azureus_core.getPluginManager().getDefaultPluginInterface().getShareManager();
			
			ShareResource[]	shares = sm.getShares();
			
			for (int i=0;i<shares.length;i++){
				
				resourceAdded(shares[i]);
			}
			
			sm.addListener(this);
			
		}catch( ShareException e ){
			
			Debug.printStackTrace( e );
		}
	}

  public void tableStructureChanged() {
    super.tableStructureChanged();
    createRows();
  }

  public void 
  fillMenu(
  	final Menu menu) 
  {
		/*
	   final MenuItem itemStart = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemStart, "MySharesView.menu.start"); //$NON-NLS-1$
	   itemStart.setImage(ImageRepository.getImage("start"));

	   final MenuItem itemStop = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemStop, "MySharesView.menu.stop"); //$NON-NLS-1$
	   itemStop.setImage(ImageRepository.getImage("stop"));
	   */
		
	    menuCategory = new Menu(getComposite().getShell(), SWT.DROP_DOWN);
	    final MenuItem itemCategory = new MenuItem(menu, SWT.CASCADE);
	    Messages.setLanguageText(itemCategory, "MyTorrentsView.menu.setCategory"); //$NON-NLS-1$
	    //itemCategory.setImage(ImageRepository.getImage("speed"));
	    itemCategory.setMenu(menuCategory);

	    addCategorySubMenu();
	    
	    new MenuItem(menu, SWT.SEPARATOR);

	   final MenuItem itemRemove = new MenuItem(menu, SWT.PUSH);
	   Messages.setLanguageText(itemRemove, "MySharesView.menu.remove"); //$NON-NLS-1$
	   Utils.setMenuItemImage(itemRemove, "delete");


	   Object[] shares = getSelectedDataSources();

	   itemRemove.setEnabled(shares.length > 0);

	   itemRemove.addListener(SWT.Selection, new Listener() {
		 public void handleEvent(Event e) {
		   removeSelectedShares();
		 }   
	   });

    new MenuItem(menu, SWT.SEPARATOR);

    super.fillMenu(menu);
	}
	
	public void resourceAdded(ShareResource resource) {		
	  addDataSource(resource);
	}
	
	public void resourceModified(ShareResource resource) { }
	
	public void resourceDeleted(ShareResource resource) {
	  removeDataSource(resource);
	}
	
	public void reportProgress(final int percent_complete) {	}
	
	public void	reportCurrentTask(final String task_description) { }
 
	public void refresh(boolean bForceSort) {
		if (getComposite() == null || getComposite().isDisposed()) {
      return;
	  }
		
		computePossibleActions();
		MainWindow.getWindow().refreshIconBar();
		
		super.refresh(bForceSort);
	}	 

	 private void addCategorySubMenu() {
	    MenuItem[] items = menuCategory.getItems();
	    int i;
	    for (i = 0; i < items.length; i++) {
	      items[i].dispose();
	    }

	    Category[] categories = CategoryManager.getCategories();
	    Arrays.sort(categories);

	    if (categories.length > 0) {
	      Category catUncat = CategoryManager.getCategory(Category.TYPE_UNCATEGORIZED);
	      if (catUncat != null) {
	        final MenuItem itemCategory = new MenuItem(menuCategory, SWT.PUSH);
	        Messages.setLanguageText(itemCategory, catUncat.getName());
	        itemCategory.setData("Category", catUncat);
	        itemCategory.addListener(SWT.Selection, new Listener() {
	          public void handleEvent(Event event) {
	            MenuItem item = (MenuItem)event.widget;
	            assignSelectedToCategory((Category)item.getData("Category"));
	          }
	        });

	        new MenuItem(menuCategory, SWT.SEPARATOR);
	      }

	      for (i = 0; i < categories.length; i++) {
	        if (categories[i].getType() == Category.TYPE_USER) {
	          final MenuItem itemCategory = new MenuItem(menuCategory, SWT.PUSH);
	          itemCategory.setText(categories[i].getName());
	          itemCategory.setData("Category", categories[i]);

	          itemCategory.addListener(SWT.Selection, new Listener() {
	            public void handleEvent(Event event) {
	              MenuItem item = (MenuItem)event.widget;
	              assignSelectedToCategory((Category)item.getData("Category"));
	            }
	          });
	        }
	      }

	      new MenuItem(menuCategory, SWT.SEPARATOR);
	    }

	    final MenuItem itemAddCategory = new MenuItem(menuCategory, SWT.PUSH);
	    Messages.setLanguageText(itemAddCategory,
	                             "MyTorrentsView.menu.setCategory.add");

	    itemAddCategory.addListener(SWT.Selection, new Listener() {
	      public void handleEvent(Event event) {
	        addCategory();
	      }
	    });

	  }
	
	  public void 
	  categoryAdded(Category category) 
	  {
	  	MainWindow.getWindow().getDisplay().asyncExec(
		  		new AERunnable() 
				{
		  			public void 
					runSupport() 
		  			{
		  				addCategorySubMenu();
		  			}
				});
	  }

	  public void 
	  categoryRemoved(
	  	Category category) 
	  {
	  	MainWindow.getWindow().getDisplay().asyncExec(
	  		new AERunnable() 
			{
	  			public void 
				runSupport() 
	  			{
	  				addCategorySubMenu();
	  			}
			});
	  }
	
	  
	  private void addCategory() {
	    CategoryAdderWindow adderWindow = new CategoryAdderWindow(MainWindow.getWindow().getDisplay());
	    Category newCategory = adderWindow.getNewCategory();
	    if (newCategory != null)
	      assignSelectedToCategory(newCategory);
	  }
	  
	  private void assignSelectedToCategory(final Category category) {
	    runForSelectedRows(new GroupTableRowRunner() {
	      public void run(TableRowCore row) {
	      	String value;
	      	
	      	if ( category == null ){
	      		
	      		value = null;
	      		
	      	}else if ( category == CategoryManager.getCategory(Category.TYPE_UNCATEGORIZED)){
	      		
	      		value = null;
	      		
	      	}else{
	      		
	      		value = category.getName();
	      	}
	      	
	        ((ShareResource)row.getDataSource(true)).setAttribute( category_attribute, value );
	      }
	    });
	  }
	  
  public void delete() {
    super.delete();

	 	try {
	 		azureus_core.getPluginManager().getDefaultPluginInterface().getShareManager().removeListener(this);
	 	}catch( ShareException e ){
	 		Debug.printStackTrace( e );
	 	}
	 	
    MainWindow.getWindow().setMyShares(null);
  }

  private boolean start,stop,remove;
  
  private void computePossibleActions() {
    start = stop = remove = false;
    Object[] shares = getSelectedDataSources();
    if (shares.length > 0) {
      remove = true;
      for (int i=0; i < shares.length; i++){        
        /*
        ShareResource	share = (ShareResource)shares[i];
        
        int	status = host_torrent.getStatus();
        
        if ( status == TRHostTorrent.TS_STOPPED ){          
          start	= true;          
        }
        
        if ( status == TRHostTorrent.TS_STARTED ){          
          stop = true;
        }
        */
      }
    }
  }
  
  public boolean isEnabled(String itemKey) {
    if(itemKey.equals("start"))
      return start;
    if(itemKey.equals("stop"))
      return stop;
    if(itemKey.equals("remove"))
      return remove;
    return false;
  }
  

  public void itemActivated(String itemKey) {
    if(itemKey.equals("remove")){
      removeSelectedShares();
      return;
    }
  }
  
  private void 
  removeSelectedShares()
  {
    Object[] shares = getSelectedDataSources();
    for (int i = 0; i < shares.length; i++) {
    	try{
    		((ShareResource)shares[i]).delete();
    		
    	}catch( Throwable e ){
    		
    	  Alerts.showErrorMessageBoxUsingResourceString( "globalmanager.download.remove.veto", e );
    	}
    }
  }
}
