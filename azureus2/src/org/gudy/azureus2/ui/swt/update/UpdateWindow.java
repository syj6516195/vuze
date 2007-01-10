/*
 * Created on 7 mai 2004
 * Created by Olivier Chalouhi
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
package org.gudy.azureus2.ui.swt.update;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;

import org.gudy.azureus2.plugins.update.Update;
import org.gudy.azureus2.plugins.update.UpdateCheckInstance;
import org.gudy.azureus2.plugins.update.UpdateManagerDecisionListener;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderListener;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.StringListChooser;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

import java.io.InputStream;
import java.util.*;
import java.util.List;

/**
 * @author Olivier Chalouhi
 *
 */
public class 
UpdateWindow
	extends 	AERunnable
	implements 	ResourceDownloaderListener{
  
  private UpdateCheckInstance	check_instance;
  private int					check_type;
  
  Display display;  
  
  Shell updateWindow;
  Table table;
  StyledText stDescription;
  ProgressBar progress;
  Label status;
  
  Button btnOk;
  Listener lOk;
  
  Button btnCancel;
  Listener lCancel;
  
  // list of linkInfo for tracking where the links are
  // could have just used stDecription.getStyleRanges() since we underline
  // the links, but I didn't want to risk a chance of any other styles
  // being in there that I don't know about (plus managing the URL)
  ArrayList links = new ArrayList();
  
  
  
  boolean askingForShow;
  
  boolean restartRequired;
  
  private long totalDownloadSize;
  private List downloaders;
  private Iterator iterDownloaders;
  
  private static final int COL_NAME = 0;
  private static final int COL_VERSION = 1;
  private static final int COL_SIZE = 2;
  
  
  public 
  UpdateWindow(
  		AzureusCore			_azureus_core,
  		UpdateCheckInstance	_check_instance )
  {
  	check_instance 	= _check_instance;
  	
  	check_type = check_instance.getType();
  	
  	check_instance.addDecisionListener(
	  		new UpdateManagerDecisionListener()
	  		{
	  			public Object
	  			decide(
	  				Update		update,
	  				int			decision_type,
	  				String		decision_name,
	  				String		decision_description,
	  				Object		decision_data )
	  			{
	  				if ( decision_type == UpdateManagerDecisionListener.DT_STRING_ARRAY_TO_STRING ){
	  					
	  					String[]	options = (String[])decision_data;
  					
	  					Shell	shell = updateWindow;
	  					
	  					if ( shell == null ){
	  						
	  						Debug.out( "Shell doesn't exist" );
	  						
	  						return( null );
	  					}
	  					
	  					StringListChooser chooser = new StringListChooser( shell );
	  					
	  					chooser.setTitle( decision_name );
	  					chooser.setText( decision_description );
	  					
	  					for (int i=0;i<options.length;i++){
	  						
	  						chooser.addOption( options[i] );
	  					}
	  					
	  					String	result = chooser.open();
	  					
	  					return( result );
	  				}
	  				
	  				return( null );
	  			}
	  		});
  	
    this.display = SWTThread.getInstance().getDisplay();
    this.updateWindow = null;
    this.askingForShow =false;
    if(display != null && !display.isDisposed())
      display.asyncExec(this);
  }
  
  //The Shell creation process
  public void runSupport() {
    if(display == null || display.isDisposed())
      return;
    
    //Do not use ~SWT.CLOSE cause on some linux/GTK platform it
    //forces the window to be only 200x200
    //catch close event instead, and never do it
  	UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
  	if (uiFunctions != null) {
			Shell mainShell = uiFunctions.getMainShell();
			updateWindow = ShellFactory.createShell(mainShell, SWT.DIALOG_TRIM
					| SWT.RESIZE);
  	}
    
    updateWindow.addListener(SWT.Close,new Listener() {
      public void handleEvent(Event e) {
        dispose();
      }
    });
    
    Utils.setShellIcon(updateWindow);
    
    String	res_prefix = "swt.";
    
    if ( check_type == UpdateCheckInstance.UCI_INSTALL ){
    	
    	res_prefix += "install.window";
    	
    }else if (check_type == UpdateCheckInstance.UCI_UNINSTALL ){
    	
    	res_prefix += "uninstall.window";
    	
    }else{
    	
    	res_prefix += "update.window";
    }
    
    Messages.setLanguageText(updateWindow, res_prefix + ".title");
    
    FormLayout layout = new FormLayout();
    try {
      layout.spacing = 5;
    } catch (NoSuchFieldError e) { /* Pre SWT 3.0 */ }
    layout.marginHeight = 10;
    layout.marginWidth = 10;
    FormData formData;
    updateWindow.setLayout(layout);
    
    Label lHeaderText = new Label(updateWindow,SWT.WRAP);
    Messages.setLanguageText(lHeaderText,res_prefix + ".header");
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    formData.top = new FormAttachment(0,0);
    lHeaderText.setLayoutData(formData);
    
    SashForm sash = new SashForm(updateWindow,SWT.VERTICAL);
       
    table = new Table(sash,SWT.CHECK | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
    String[] names = {"name" , "version" , "size"};
    int[] sizes = {220,80,80};
    for(int i = 0 ; i < names.length ; i++) {
      TableColumn column = new TableColumn(table,SWT.LEFT);
      Messages.setLanguageText(column,"swt.update.window.columns." + names[i]);
      column.setWidth(sizes[i]);
    }
    table.setHeaderVisible(true);    

    table.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event e) {
      	rowSelected();
      }
    });
    
    stDescription = new StyledText(sash,SWT.BORDER | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);    
    stDescription.setWordWrap(true);
    
    stDescription.addListener(SWT.MouseUp, new Listener() {
    	public void handleEvent(Event event) {
    		if (links.size() == 0) {
    			return;
    		}
    		try {
    			int ofs = stDescription.getOffsetAtLocation(new Point(event.x, event.y));
    			for (int i = 0; i < links.size(); i++) {
						linkInfo linkInfo = (linkInfo)links.get(i);
						if (ofs >= linkInfo.ofsStart && ofs <= linkInfo.ofsEnd) {
							Utils.launch(linkInfo.url);
							break;
						}
					}
    		} catch (Exception e) {
    			
    		}
    	}
    });

    final Cursor handCursor = new Cursor(display, SWT.CURSOR_HAND);
    stDescription.addListener(SWT.MouseMove, new Listener() {
    	public void handleEvent(Event event) {
    		if (links.size() == 0) {
    			return;
    		}
  			boolean onLink = false;
    		try {
    			int ofs = stDescription.getOffsetAtLocation(new Point(event.x, event.y));
    			for (int i = 0; i < links.size(); i++) {
						linkInfo linkInfo = (linkInfo)links.get(i);
						if (ofs >= linkInfo.ofsStart && ofs <= linkInfo.ofsEnd) {
							onLink = true;
							break;
						}
					}
    		} catch (Exception e) {
    			
    		}
  			Cursor cursor = onLink ? handCursor : null;
  			if (stDescription.getCursor() != cursor) {
  				stDescription.setCursor(cursor);
  			}
    	}
    });
    
    stDescription.addListener(SWT.Dispose, new Listener() {
    	public void handleEvent(Event event) {
				stDescription.setCursor(null);
				handCursor.dispose();
    	}
    });

    progress = new ProgressBar(updateWindow,SWT.NULL);
    progress.setMinimum(0);
    progress.setMaximum(100);
    progress.setSelection(0);
    
    status = new Label(updateWindow,SWT.NULL);
    
 
    
    btnOk = new Button(updateWindow,SWT.PUSH);
    Messages.setLanguageText(btnOk,res_prefix + ".ok" );
    
    updateWindow.setDefaultButton( btnOk );
    lOk = new Listener() {
      public void handleEvent(Event e) {
        update();
      }
    };
    
    btnOk.addListener(SWT.Selection, lOk);
    btnOk.setEnabled( false );
    
    btnCancel = new Button(updateWindow,SWT.PUSH);
    
    Messages.setLanguageText(btnCancel,"swt.update.window.cancel");
    
    lCancel = new Listener() {
	      public void handleEvent(Event e) {
	        dispose();
	       	check_instance.cancel();
	      }
	   };
    btnCancel.addListener(SWT.Selection,lCancel);
    
    updateWindow.addListener(SWT.Traverse, new Listener() {	
		public void handleEvent(Event e) {
			if ( e.character == SWT.ESC){
			      dispose();
			      check_instance.cancel();			
			 }
		}
    });
    
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    formData.top = new FormAttachment(lHeaderText);
    formData.bottom = new FormAttachment(progress);
    sash.setLayoutData(formData);
    
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    formData.bottom = new FormAttachment(status);
    progress.setLayoutData(formData);
    
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.right = new FormAttachment(100,0);
    formData.bottom = new FormAttachment(btnCancel);
    status.setLayoutData(formData);
    
    formData = new FormData();
    formData.width = 100;
    formData.right = new FormAttachment(100,0);
    formData.bottom = new FormAttachment(100,0);
    btnCancel.setLayoutData(formData);
    
    formData = new FormData();
    formData.width = 100;
    formData.right = new FormAttachment(btnCancel);
    formData.bottom = new FormAttachment(100,0);
    btnOk.setLayoutData(formData);
    
    updateWindow.setSize(400,400);
    //updateWindow.open();
  }
  
  protected void
  rowSelected()
  {
    checkMandatory();
    checkRestartNeeded();
    TableItem[] items = table.getSelection();
    if(items.length == 0) return;
    Update update = (Update) items[0].getData();        
    String[] descriptions = update.getDescription();
    stDescription.setText("");
    int ofs = 0;
    for(int i = 0 ; i < descriptions.length ; i++) {
    	String s = descriptions[i].replaceAll("<.*a\\s++.*href=\"?([^\\'\"\\s>]++).*", "$1");
      stDescription.append(s + "\n");
      
      try {
	      int iURLStart = s.indexOf("http");
	      if (iURLStart >= 0) {
	      	int iURLEnd = s.indexOf(' ', iURLStart);
	      	String url;
	      	if (iURLEnd >= 0) {
	      		url = s.substring(iURLStart, iURLEnd);
	      	} else {
	      		url = s.substring(iURLStart);
	      	}
	      	linkInfo info = new linkInfo(iURLStart + ofs, iURLStart + ofs
							+ url.length(), url);
	      	links.add(info);
	      	
	      	StyleRange sr = new StyleRange();
	      	sr.start = info.ofsStart;
	      	sr.length = url.length();
	      	sr.underline = true;
	      	
	      	stDescription.setStyleRange(sr);
	      }
	      ofs += s.length() + 1;
      } catch (Exception e) {
      	Debug.out(e);
      }
    }
  }
  
  public Shell
  getShell()
  {
	  return( updateWindow );
  }
  
  public void dispose() {
    updateWindow.dispose();
    MainWindow window = MainWindow.getWindow();
    if (window != null) {
    	MainWindow.getWindow().setUpdateNeeded(null);
    }
  }
  
  public void addUpdate(final Update update) {
    if(display == null || display.isDisposed())
      return;
  
    display.asyncExec(new AERunnable() {
      public void runSupport() {
        if(table == null || table.isDisposed())
          return;
        
        final TableItem item = new TableItem(table,SWT.NULL);
        item.setData(update);
        item.setText(COL_NAME,update.getName()==null?"Unknown":update.getName());  
        item.setText(COL_VERSION,update.getNewVersion()==null?"Unknown":update.getNewVersion());
        ResourceDownloader[] rds = update.getDownloaders();
        long totalLength = 0;
        for(int i = 0 ; i < rds.length ; i++) {
          try {
            totalLength += rds[i].getSize();
          } catch(Exception e) {
          }
        }                
        
        item.setText(COL_SIZE,DisplayFormatters.formatByteCountToBase10KBEtc(totalLength));                
        
        item.setChecked(true);
        
        	// select first entry
        
        if ( table.getItemCount() == 1 ){
        	
        	table.select(0);
        	
        	rowSelected();	// don't seem to be getting the selection event, do it explicitly
        }
        
        checkRestartNeeded();
        
        if( 	COConfigurationManager.getBooleanParameter("update.opendialog")|| 
        		check_instance.getType() != UpdateCheckInstance.UCI_UPDATE ) {
        	
        	show();
        	
        }else{
        	
        	if (MainWindow.getWindow() != null) {
        		MainWindow.getWindow().setUpdateNeeded(UpdateWindow.this);
        	}
        }
      }
    });
  }
  
  protected void
  updateAdditionComplete()
  {
    if(display == null || display.isDisposed())
        return;
    
      display.asyncExec(new AERunnable() {
        public void runSupport() {
          if(btnOk == null || btnOk.isDisposed())
            return;
          
          btnOk.setEnabled(true);
        }
      });
  }
  
  public void show() {
    if(updateWindow == null || updateWindow.isDisposed())
      return;
    Utils.centreWindow( updateWindow );
    updateWindow.open();
    updateWindow.forceActive();       
  }
  
  
  private void checkMandatory() {
    TableItem[] items = table.getItems();
    for(int i = 0 ; i < items.length ; i++) {
      Update update = (Update) items[i].getData();
      if(update.isMandatory()) items[i].setChecked(true);
    }
  }
  
  private void checkRestartNeeded() {  
    restartRequired = false;
    boolean	restartMaybeRequired = false;
    TableItem[] items = table.getItems();
    for(int i = 0 ; i < items.length ; i++) {
      if(! items[i].getChecked()) continue;
      Update update = (Update) items[i].getData();
      int required = update.getRestartRequired();
      if((required == Update.RESTART_REQUIRED_MAYBE)){
      	restartMaybeRequired = true;
      }else if ( required == Update.RESTART_REQUIRED_YES ){
      	restartRequired = true;
      }
    }
    if(restartRequired) {
        status.setText(MessageText.getString("swt.update.window.status.restartNeeded"));
    }else if(restartMaybeRequired) {
        status.setText(MessageText.getString("swt.update.window.status.restartMaybeNeeded"));
    }else{
      status.setText("");
    }
  }
  
  private void update() {
    btnOk.setEnabled(false);    
    Messages.setLanguageText(btnCancel,"swt.update.window.cancel");
    table.setEnabled(false);
    stDescription.setText("");
    TableItem[] items = table.getItems();
    
    totalDownloadSize = 0;   
    downloaders = new ArrayList();
    
    for(int i = 0 ; i < items.length ; i++) {
      if(! items[i].getChecked()) continue;
      
      Update update = (Update) items[i].getData();
      ResourceDownloader[] rds = update.getDownloaders();
      for(int j = 0 ; j < rds.length ; j++) {
        downloaders.add(rds[j]);        
        try {
          totalDownloadSize += rds[j].getSize();
        } catch (Exception e) {
          stDescription.append(MessageText.getString("swt.update.window.no_size") + rds[j].getName() +"\n");
        }        
      }
    }
    downloadersToData = new HashMap();
    iterDownloaders = downloaders.iterator();
    nextUpdate();
  }
  
  private void nextUpdate() {
    if(iterDownloaders.hasNext()) {
      ResourceDownloader downloader = (ResourceDownloader) iterDownloaders.next();
      downloader.addListener(this);
      downloader.asyncDownload();
    } else {
      switchToRestart();      
    }
  }
  
  private void switchToRestart() {
    if(display == null || display.isDisposed())
      return;
    
    display.asyncExec(new AERunnable(){
      public void runSupport() {
      	checkRestartNeeded();	// gotta recheck coz a maybe might have got to yes
        progress.setSelection(100);
        status.setText(MessageText.getString("swt.update.window.status.done"));
        btnOk.removeListener(SWT.Selection,lOk);
        btnOk.setEnabled(true);
        btnOk.addListener(SWT.Selection,new Listener() {
          public void handleEvent(Event e) {
            finishUpdate(true);
          }
        });
        if(restartRequired) {
          ((FormData)btnOk.getLayoutData()).width = 150;
          ((FormData)btnCancel.getLayoutData()).width = 150;
          updateWindow.layout();
          Messages.setLanguageText(btnOk,"swt.update.window.restart");
          btnCancel.removeListener(SWT.Selection,lCancel);
          Messages.setLanguageText(btnCancel,"swt.update.window.restartLater");
          btnCancel.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
              finishUpdate(false);
            }
          });
        } else {
          Messages.setLanguageText(btnOk,"swt.update.window.close");
          btnCancel.setEnabled(false);
        }
      }
    });
  }
  
  public void reportPercentComplete(ResourceDownloader downloader,
      int percentage) {
    setProgressSelection(percentage);   
  }
  
  private void setProgressSelection(final int percent) {
    if(display == null || display.isDisposed())
      return;
    
    display.asyncExec(new AERunnable() {
      public void runSupport() {
        if(progress != null && !progress.isDisposed())
        progress.setSelection(percent);
      }
    });
  }
  
  private Map downloadersToData;
  
  public boolean completed(ResourceDownloader downloader, InputStream data) {
    downloadersToData.put(downloader,data);
    downloader.removeListener(this);
    setProgressSelection(0);
    nextUpdate();
    return true;
  }
  
  public void failed(ResourceDownloader downloader,
      ResourceDownloaderException e) {
    downloader.removeListener(this);
    setStatusText(MessageText.getString("swt.update.window.status.failed"));
    
    String	msg = downloader.getName() + " : " + e;
    
    if ( e.getCause() != null ){
    	
    	msg += " [" + e.getCause() + "]";
    }
    
    appendDetails(msg);
  }
  
  public void reportActivity(ResourceDownloader downloader, final String activity) {
    setStatusText(activity.trim());
    appendDetails(activity);
  }
  
  private void setStatusText(final String text) {
    if(display == null || display.isDisposed())
      return;
    
    display.asyncExec(new AERunnable(){
      public void runSupport() {
        if(status != null && !status.isDisposed())
          status.setText(text);
      }
    });  
  }
  
  private void appendDetails(final String text) {
    if(display == null || display.isDisposed())
      return;
    
    display.asyncExec(new AERunnable(){
      public void runSupport() {
        if(stDescription != null && !stDescription.isDisposed())
          stDescription.append(text + "\n");
      }
    });  
  }
  
  
  private void finishUpdate(boolean restartNow) {
    //When completing, remove the link in mainWindow :
  	if (MainWindow.getWindow() != null) {
  		MainWindow.getWindow().setUpdateNeeded(null);
  	}
    
    //If restart is required, then restart
    if( restartRequired && restartNow) {
    	// this HAS to be done this way around else the restart inherits
    	// the 6880 port listen. However, this is a general problem....
    	UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
    	if (uiFunctions != null) {
    		if (!uiFunctions.dispose(true, false)) {
        	updateWindow.dispose(); 
    		}
    	} else {
      	updateWindow.dispose(); 
    	}
    }else{
    	
      updateWindow.dispose();      
    }
  }
  
  protected boolean
  isDisposed()
  {
  	return( display == null || display.isDisposed() || updateWindow == null || updateWindow.isDisposed());
  }
  
  public static class linkInfo {
  	int ofsStart;
  	int ofsEnd;
  	String url;
  	
  	linkInfo(int s, int e, String url) {
  		ofsStart = s;
  		ofsEnd = e;
  		this.url = url;
  	}
  }
}
