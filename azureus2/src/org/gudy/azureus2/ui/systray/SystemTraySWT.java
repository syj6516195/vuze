/*
 * File    : SystemTraySWT.java
 * Created : 2 avr. 2004
 * By      : Olivier
 * 
 * Azureus - a Java Bittorrent client
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
 */
package org.gudy.azureus2.ui.systray;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.PasswordWindow;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.SelectableSpeedMenu;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import java.util.List;

/**
 * @author Olivier Chalouhi
 *
 */
public class SystemTraySWT {
  
  MainWindow mainWindow;
  Display display;
  
  Tray tray;
  TrayItem trayItem;
  
  Menu menu;

  public SystemTraySWT(MainWindow _mainWindow) {
    this.mainWindow = _mainWindow;
    this.display = mainWindow.getDisplay();
    
    tray = display.getSystemTray();
    trayItem = new TrayItem(tray,SWT.NULL);
    
    if( ! Constants.isOSX) {
      trayItem.setImage(ImageRepository.getImage("azureus"));  
    }    
    trayItem.setVisible(true);
    
    menu = new Menu(mainWindow.getShell(),SWT.POP_UP);
    
    final MenuItem itemShow = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemShow,"SystemTray.menu.show");
    
    
    new MenuItem(menu,SWT.SEPARATOR);
    
    final MenuItem itemCloseAll = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemCloseAll,"SystemTray.menu.closealldownloadbars");
    
    new MenuItem(menu,SWT.SEPARATOR);

    createUploadLimitMenu(menu);
    createDownloadLimitMenu(menu);

    new MenuItem(menu, SWT.SEPARATOR);

    final MenuItem itemStartAll = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemStartAll,"SystemTray.menu.startalltransfers");
    
    final MenuItem itemStopAll = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemStopAll,"SystemTray.menu.stopalltransfers");
    
    final MenuItem itemPause = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemPause,"SystemTray.menu.pausetransfers");
    
    final MenuItem itemResume = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemResume,"SystemTray.menu.resumetransfers");

    new MenuItem(menu,SWT.SEPARATOR);
    
    final MenuItem itemExit = new MenuItem(menu,SWT.NULL);
    Messages.setLanguageText(itemExit,"SystemTray.menu.exit");
    
    itemShow.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        show();
      }
    });
    
    itemStartAll.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        SystemTraySWT.this.mainWindow.getGlobalManager().startAllDownloads();
      }
    });
    
    itemStopAll.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
      	ManagerUtils.asyncStopAll();
      }
    });
    
    
    itemPause.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event arg0) {
        	ManagerUtils.asyncPause();
        }
      });
      
    itemResume.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event arg0) {
          SystemTraySWT.this.mainWindow.getGlobalManager().resumeDownloads();
        }
      });
      
    
    menu.addMenuListener(
          	new MenuListener()
    		{
          		public void
    			menuShown(
    				MenuEvent	_menu )
          		{
          		  itemPause.setEnabled( SystemTraySWT.this.mainWindow.getGlobalManager().canPauseDownloads() );
          			
          			itemResume.setEnabled( SystemTraySWT.this.mainWindow.getGlobalManager().canResumeDownloads() );
          		}
          		
        		public void
    			menuHidden(
    				MenuEvent	_menu )
          		{
          			
          		}
    		});
    
    itemCloseAll.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        SystemTraySWT.this.mainWindow.closeDownloadBars();
      }
    });
    
    itemExit.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event arg0) {
        SystemTraySWT.this.mainWindow.dispose(false,false);
      }
    });
    
    
    
    trayItem.addListener(SWT.DefaultSelection,new Listener() {
      public void handleEvent(Event arg0) {
        show();
      }
    });
    
    trayItem.addListener(SWT.MenuDetect,new Listener() {
      public void handleEvent(Event arg0) {
        menu.setVisible(true);
      }
    });               
  }

    /**
     * Creates the global upload limit context menu item
     * @param parent The system tray contextual menu
     */
    private final void createUploadLimitMenu(final Menu parent)
    {
        final MenuItem uploadSpeedItem = new MenuItem(parent, SWT.CASCADE);
        uploadSpeedItem.setText(MessageText.getString("GeneralView.label.maxuploadspeed"));

        final Menu uploadSpeedMenu = new Menu(mainWindow.getShell(), SWT.DROP_DOWN);

        uploadSpeedMenu.addListener(SWT.Show,  new Listener()
        {
            public void handleEvent(Event event)
            {
            	SelectableSpeedMenu.generateMenuItems(uploadSpeedMenu, mainWindow, true);
            }
        });


        uploadSpeedItem.setMenu(uploadSpeedMenu);
    }

    /**
     * Creates the global download limit context menu item
     * @param parent The system tray contextual menu
     */
    private final void createDownloadLimitMenu(final Menu parent)
    {
        final MenuItem downloadSpeedItem = new MenuItem(parent, SWT.CASCADE);
        downloadSpeedItem.setText(MessageText.getString("GeneralView.label.maxdownloadspeed"));

        final Menu downloadSpeedMenu = new Menu(mainWindow.getShell(), SWT.DROP_DOWN);

        downloadSpeedMenu.addListener(SWT.Show, new Listener()
        {
            public void handleEvent(Event event)
            {
            	SelectableSpeedMenu.generateMenuItems(downloadSpeedMenu, mainWindow, false);
            }
        });

        downloadSpeedItem.setMenu(downloadSpeedMenu);
    }


    /**
     * Gets the selection listener of a upload or download limit menu item (including unlimited)
     * @param parent The parent menu
     * @param configKey The configuration key
     * @return The selection listener
     */
   private final Listener getLimitMenuItemListener(final boolean up_menu, final Menu parent, final String configKey)
   {
       return new Listener() {
           public void handleEvent(Event event) {
               final MenuItem[] items = parent.getItems();
               for(int i = 0; i < items.length; i++) {
                    if(items[i] == event.widget)
                    {
                        items[i].setSelection(true);
                        final int cValue = ((Integer)new TransferSpeedValidator(configKey, items[i].getData("maxkb")).getValue()).intValue();
                        COConfigurationManager.setParameter(configKey, cValue);
                        
                        if ( up_menu ){
                            
                        	String configAutoKey = 
                        		TransferSpeedValidator.getActiveAutoUploadParameter( mainWindow.getGlobalManager());
             
                        	COConfigurationManager.setParameter( configAutoKey, false );
                        }
                        
                        COConfigurationManager.save();
                    }
                    else {
                        items[i].setSelection(false);
                    }
                }
           }
       };
   }

    public void dispose() {
    if(trayItem != null && !trayItem.isDisposed()) {
      trayItem.dispose();
    }
  }
  
  public void update() {    
    if(trayItem.isDisposed()) 
      return;
    List managers = mainWindow.getGlobalManager().getDownloadManagers();
    //StringBuffer toolTip = new StringBuffer("Azureus - ");//$NON-NLS-1$
    StringBuffer toolTip = new StringBuffer();
    int seeding = 0;
    int downloading = 0;
    
      for (int i = 0; i < managers.size(); i++) {
        DownloadManager manager = (DownloadManager) managers.get(i);
        int state = manager.getState();
        if (state == DownloadManager.STATE_DOWNLOADING)
          downloading++;
        if (state == DownloadManager.STATE_SEEDING)
          seeding++;
      }
 
	  	// something went funny here across Java versions, leading " " got lost
	  
    String	seeding_text 		= MessageText.getString("SystemTray.tooltip.seeding").replaceAll("%1", ""+seeding);
	String	downloading_text 	= MessageText.getString("SystemTray.tooltip.downloading").replaceAll("%1", ""+downloading);
	
/*	if ( !seeding_text.startsWith(" " )){
		seeding_text = " " + seeding_text;
	}*/
	if ( !downloading_text.startsWith(" " )){
		downloading_text = " " + downloading_text;
	}
	
    
    toolTip.append(seeding_text)
    	.append(downloading_text)
    	.append("\n");
    toolTip.append(MessageText.getString("ConfigView.download.abbreviated"))
    	.append(" "); 
    toolTip.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(mainWindow.getGlobalManager().getStats().getDataReceiveRate() + mainWindow.getGlobalManager().getStats().getProtocolReceiveRate() ));
    toolTip.append(", ")
    	.append(MessageText.getString("ConfigView.upload.abbreviated"))
    	.append(" ");
    toolTip.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(mainWindow.getGlobalManager().getStats().getDataSendRate() + mainWindow.getGlobalManager().getStats().getProtocolSendRate()));
    
    
    trayItem.setToolTipText(toolTip.toString());    
    
    //Why should we refresh the image? it never changes ...
    //and is a memory bottleneck for some non-obvious reasons.
    //trayItem.setImage(ImageRepository.getImage("azureus"));   
    trayItem.setVisible(true);    
  }
  
  private void show() {
    if (!COConfigurationManager.getBooleanParameter("Password enabled",false))          
      SystemTraySWT.this.mainWindow.setVisible(true);
    else
      PasswordWindow.showPasswordWindow(MainWindow.getWindow().getDisplay());
  }
  
  public Menu getMenu() {
    return menu;
  }
  
}
