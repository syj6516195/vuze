/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import java.util.ArrayList;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import com.aelitis.azureus.core.*;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.ui.swt.Alerts;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTInstanceImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

/**
 * Torrent download view, consisting of several information tabs
 * 
 * @author Olivier
 * 
 */
public class ManagerView extends AbstractIView implements
		DownloadManagerListener {

  private AzureusCore		azureus_core;
  private DownloadManager 	manager;
  private TabFolder folder;
  private ArrayList tabViews = new ArrayList();
  
  public 
  ManagerView(
  	AzureusCore		_azureus_core,
	DownloadManager manager) 
  {
  	azureus_core	= _azureus_core;
    this.manager 	= manager;
    
    dataSourceChanged(manager);

    manager.addListener(this);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    MainWindow.getWindow().removeManagerView(manager);
    manager.removeListener(this);
    
    if ( !folder.isDisposed()){
    	
    	folder.setSelection(0);
    }
    
    //Don't ask me why, but without this an exception is thrown further
    // (in folder.dispose() )
    //TODO : Investigate to see if it's a platform (OSX-Carbon) BUG, and report to SWT team.
    if(Constants.isOSX) {
      if(folder != null && !folder.isDisposed()) {
        TabItem[] items = folder.getItems();
        for(int i=0 ; i < items.length ; i++) {
          if (!items[i].isDisposed())
            items[i].dispose();
        }
      }
    }

    for (int i = 0; i < tabViews.size(); i++) {
    	IView view = (IView) tabViews.get(i);
    	if (view != null)
    		view.delete();
    }
    tabViews.clear();

    if (folder != null && !folder.isDisposed()) {
      folder.dispose();
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getComposite()
   */
  public Composite getComposite() {
    return folder;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    int completed = manager.getStats().getCompleted();
    return DisplayFormatters.formatPercentFromThousands(completed) + " : " + manager.getDisplayName();
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
    
	  	if (folder == null) {
	    folder = new TabFolder(composite, SWT.LEFT);
	    folder.setBackground(Colors.background);
	  	} else {
	  	  System.out.println("ManagerView::initialize : folder isn't null !!!");
	  	}

	  IView views[] = { new GeneralView(), new PeersView(),
				new PeersGraphicView(), new PiecesView(), new FilesView(),
				new LoggerView() };

		for (int i = 0; i < views.length; i++)
			addSection(views[i], manager);

    // Call plugin listeners
		UISWTInstanceImpl pluginUI = MainWindow.getWindow().getUISWTInstanceImpl();
		Map pluginViews = pluginUI.getViewListeners(UISWTInstance.VIEW_MYTORRENTS);
		if (pluginViews != null) {
			String[] sNames = (String[])pluginViews.keySet().toArray(new String[0]);
			for (int i = 0; i < sNames.length; i++) {
				UISWTViewEventListener l = (UISWTViewEventListener)pluginViews.get(sNames[i]);
				if (l != null) {
					try {
						UISWTViewImpl view = new UISWTViewImpl(
								UISWTInstance.VIEW_MYTORRENTS, sNames[i], l);
						addSection(view);
					} catch (Exception e) {
						// skip
					}
				}
			}
		}
		
    
    // Initialize view when user selects it
    folder.addSelectionListener(new SelectionListener() {
      public void widgetSelected(SelectionEvent e) {
        TabItem item = (TabItem)e.item;
        if (item != null && item.getControl() == null) {
        	IView view = (IView)item.getData("IView");
        	
        	view.initialize(folder);
        	item.setControl(view.getComposite());
        }
        refresh();
      }
      public void widgetDefaultSelected(SelectionEvent e) {
      }
    });
    
    
    views[0].initialize(folder);
    folder.getItem(0).setControl(views[0].getComposite());
    views[0].refresh();
    views[0].getComposite().layout(true);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {
		if (getComposite() == null || getComposite().isDisposed())
			return;

		try {
			int index = folder.getSelectionIndex();
			if (index == -1)
				return;
			TabItem ti = folder.getItem(index);
			if (ti.isDisposed())
				return;

			IView view = (IView) ti.getData("IView");
			if (view != null)
				view.refresh();

		} catch (Exception e) {
			Debug.printStackTrace(e);
		}
	}
  
  public boolean isEnabled(String itemKey) {
    if(itemKey.equals("run"))
      return true;
    if(itemKey.equals("start"))
      return ManagerUtils.isStartable(manager);
    if(itemKey.equals("stop"))
      return ManagerUtils.isStopable(manager);
    if(itemKey.equals("host"))
      return true;
    if(itemKey.equals("publish"))
      return true;
    if(itemKey.equals("remove"))
      return true;
    return false;
  }
  
  public void itemActivated(String itemKey) {
	  if(itemKey.equals("run")) {
	    ManagerUtils.run(manager);
	    return;
	  }
	  if(itemKey.equals("start")) {
	    ManagerUtils.queue(manager,folder);
	    return;
	  }
	  if(itemKey.equals("stop")) {
	    ManagerUtils.stop(manager,folder);
	    return;
	  }
	  if(itemKey.equals("host")) {
	    ManagerUtils.host(azureus_core, manager,folder);
	    MainWindow.getWindow().showMyTracker();
	    return;
	  }
	  if(itemKey.equals("publish")) {
	    ManagerUtils.publish(azureus_core, manager,folder);
	    MainWindow.getWindow().showMyTracker();
	    return;
	  }
	  if(itemKey.equals("remove")) {
	  
        
        if( COConfigurationManager.getBooleanParameter( "confirm_torrent_removal" ) ) {
          MessageBox mb = new MessageBox(folder.getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
          mb.setText(MessageText.getString("deletedata.title"));
          mb.setMessage(MessageText.getString("MyTorrentsView.confirm_torrent_removal") + manager.getDisplayName() );
          if( mb.open() == SWT.NO ) {
            return;
          }
        }
        
       	new AEThread( "asyncStop", true )
			{
        		public void
				runSupport()
        		{
        			try{
        		        
				        manager.stopIt( DownloadManager.STATE_STOPPED, false, false );
				        
				        manager.getGlobalManager().removeDownloadManager( manager );
					  		
        			}catch( GlobalManagerDownloadRemovalVetoException e ){
					  		
        				Alerts.showErrorMessageBoxUsingResourceString( "globalmanager.download.remove.veto", e );
					}
        		}
			}.start();
	  }
  }
  
  
  public void downloadComplete(DownloadManager manager) {   
  }

  public void completionChanged(DownloadManager manager, boolean bCompleted) {
  }

  public void stateChanged(DownloadManager manager, int state) {
    if(folder == null || folder.isDisposed())
      return;    
    Display display = folder.getDisplay();
    if(display == null || display.isDisposed())
      return;
    Utils.execSWTThread(new AERunnable() {
	    public void runSupport() {
	      MainWindow.getWindow().refreshIconBar();  
	    }
    });    
  }

  public void positionChanged(DownloadManager download, int oldPosition, int newPosition) {
  }

	public void addSection(UISWTViewImpl view) {
		Object pluginDataSource = null;
		try {
			pluginDataSource = DownloadManagerImpl.getDownloadStatic(manager);
		} catch (DownloadException e) { 
			/* Ignore */
		}
		addSection(view, pluginDataSource);
	}
	
	private void addSection(IView view, Object dataSource) {
		if (view == null)
			return;

		view.dataSourceChanged(dataSource);

		TabItem item = new TabItem(folder, SWT.NULL);
		Messages.setLanguageText(item, view.getData());
		item.setData("IView", view);
		tabViews.add(view);
	}
}
