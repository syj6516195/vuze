/*
 * File    : MultiTrackerEditor.java
 * Created : 3 déc. 2003}
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
package org.gudy.azureus2.ui.swt.maketorrent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.core3.util.TrackersUtil;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.TextViewerWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.ui.UserPrompterResultListener;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * @author Olivier
 *
 */
public class MultiTrackerEditor {   
  
  TrackerEditorListener listener;
  String oldName;
  String currentName;
  boolean	anonymous;
  boolean	showTemplates;
  
  List<List<String>> trackers;
  
  Shell shell;    
  Text textName;
  Tree treeGroups;
  TreeEditor editor;
  TreeItem itemEdited;
  Button btnSave;
  Button btnCancel;
  
  Menu menu;
  
  public MultiTrackerEditor(Shell parent_shell, String name, List<List<String>> trackers,TrackerEditorListener listener) {
  	this( parent_shell, name, trackers, listener, false );
  }
  
  public 
  MultiTrackerEditor(
		Shell 					parent_shell,
  		String 					name,
		List<List<String>> 		trackers,
		TrackerEditorListener 	listener,
		boolean					anonymous ) 
  {
	  this( parent_shell, name, trackers, listener, anonymous, false );
  }
  
  public 
  MultiTrackerEditor(
		Shell 					parent_shell,
  		String 					name,
		List<List<String>> 		trackers,
		TrackerEditorListener 	listener,
		boolean					_anonymous,
		boolean					_showTemplates )
  {
	  this.oldName = name;
	  if(name != null){
		  this.currentName = name;
	  }else{
		  this.currentName = "";
	  }
	  this.listener = listener;
	  anonymous = _anonymous;
	  showTemplates	= _showTemplates;
	  this.trackers = new ArrayList<List<String>>(trackers);
	  createWindow( parent_shell );

  }
  
  private void createWindow( Shell parent_shell) {
	if ( parent_shell == null ){
		this.shell = ShellFactory.createMainShell(SWT.DIALOG_TRIM | SWT.RESIZE);
	}else{
		this.shell = ShellFactory.createShell(parent_shell,SWT.DIALOG_TRIM | SWT.RESIZE);
	}
	
    Messages.setLanguageText(this.shell, anonymous?"wizard.multitracker.edit.title":"wizard.multitracker.template.title");
    Utils.setShellIcon(shell);
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    shell.setLayout(layout);
    
    GridData gridData;
    
    if ( !anonymous ){
    	
	    Label labelName = new Label(shell,SWT.NULL);
	    Messages.setLanguageText(labelName,"wizard.multitracker.edit.name");
	    
	    textName = new Text(shell,SWT.BORDER);
	    textName.setText(currentName);
	    gridData = new GridData(GridData.FILL_HORIZONTAL);
	    gridData.horizontalSpan = 2;
	    textName.setLayoutData(gridData);
	    textName.addModifyListener(new ModifyListener() {
		    public void modifyText(ModifyEvent arg0) {
		      currentName = textName.getText();
		      computeSaveEnable();
		    }
	    });   
    }
        
    treeGroups = new Tree(shell,SWT.BORDER);
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.horizontalSpan = 3;
    gridData.heightHint = 150;
    treeGroups.setLayoutData(gridData);
    
    treeGroups.addMouseListener(
    		new MouseAdapter() 
    		{
    			public void 
    			mouseDoubleClick(
    				MouseEvent arg0 )
    			{
    				if(treeGroups.getSelectionCount() == 1) {
    					TreeItem treeItem = treeGroups.getSelection()[0];
    					String type = (String) treeItem.getData("type");
    					if(type.equals("tracker")) {
    						editTreeItem(treeItem);
    					}
    				}
    			}
    		});
    	
    if ( showTemplates ){
    	
		// template operations
	    
		Composite cTemplate = new Composite(shell, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		cTemplate.setLayoutData(gridData);
		GridLayout layoutTemplate = new GridLayout();
		layoutTemplate.numColumns = 5;
		cTemplate.setLayout(layoutTemplate);

		final Label labelTitle = new Label(cTemplate,SWT.NULL);
		Messages.setLanguageText(labelTitle, "Search.menu.engines");


		final Combo configList = new Combo(cTemplate,SWT.READ_ONLY);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		configList.setLayoutData(gridData);


		final List<Button>	buttons = new ArrayList<Button>();
	
		String sel_str = COConfigurationManager.getStringParameter( "multitrackereditor.last.selection", null );
		
		final String[]	currentTemplate = { sel_str==null||sel_str.length()==0?null:sel_str };

		final Runnable updateSelection =
			new Runnable()
			{
				public void
				run()
				{
					int selection = configList.getSelectionIndex();
					
				    boolean enabled = selection != -1;
				    
				    String sel_str = currentTemplate[0] = enabled?configList.getItem( selection ):null;
				    
				    COConfigurationManager.setParameter( "multitrackereditor.last.selection", sel_str==null?"":sel_str );
				    
				    Iterator<Button> it = buttons.iterator();
				   
				    it.next();	// skip the new button
				   
				    while( it.hasNext()){
					   
					   it.next().setEnabled( enabled );
				    }
				}
			};
				
		final Runnable updateTemplates =
			new Runnable()
			{
				public void
				run()
				{
					Map<String,List<List<String>>> multiTrackers = TrackersUtil.getInstance().getMultiTrackers();
					
					configList.removeAll();
					
					for ( String str: multiTrackers.keySet()){
						
						configList.add( str );
					}
				
				    String toBeSelected = currentTemplate[0];
				    
				    if  ( toBeSelected != null ){
				    	
					    int selection = configList.indexOf(toBeSelected);
					    
					    if (selection != -1 ){
					      
					    	configList.select( selection );
					    	
					    }else if (configList.getItemCount() > 0) {
					    	
					    	currentTemplate[0] = configList.getItem( 0 ); 
					      
					    	configList.select(0);
					    }
				    }
				    
				    updateSelection.run();
				}
			};
			
		final TrackerEditorListener	templateTEL = 
			new TrackerEditorListener()
			{
				public void 
				trackersChanged(
					String 				oldName,
					String 				newName,
					List<List<String>> 	trackers)
				{
				    TrackersUtil util = TrackersUtil.getInstance();
				    
				    if  ( oldName != null && !oldName.equals( newName )){
				    	
				    	util.removeMultiTracker( oldName );
				    }
				    
				    util.addMultiTracker( newName, trackers );
				    
				    currentTemplate[0] = newName;
				    
				    updateTemplates.run();
				}
			};
			
	
		final Button btnNew = new Button(cTemplate, SWT.PUSH);
		buttons.add( btnNew );
		Messages.setLanguageText(btnNew, "wizard.multitracker.new");
		btnNew.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				List group = new ArrayList();
				List tracker = new ArrayList();
				group.add(tracker);
				new MultiTrackerEditor(btnNew.getShell(), null,group, templateTEL);
			}
		});
				
		configList.addListener(SWT.Selection,new Listener() {
			public void handleEvent(Event e) {                
				updateSelection.run();
			}
		});
		
		
		final Button btnEdit = new Button(cTemplate, SWT.PUSH);   
		buttons.add( btnEdit );
		Messages.setLanguageText(btnEdit, "wizard.multitracker.edit");
		btnEdit.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {			
				Map multiTrackers = TrackersUtil.getInstance().getMultiTrackers();
				String	selected = currentTemplate[0];								
				new MultiTrackerEditor( btnEdit.getShell(),selected,(List)multiTrackers.get(selected), templateTEL );
			}
		});

		final Button btnDelete = new Button(cTemplate, SWT.PUSH);
		buttons.add( btnDelete );
		Messages.setLanguageText(btnDelete, "wizard.multitracker.delete");
		btnDelete.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				final String	selected = currentTemplate[0];
				
				MessageBoxShell mb = 
						new MessageBoxShell(
							MessageText.getString("message.confirm.delete.title"),
							MessageText.getString("message.confirm.delete.text",
									new String[] { selected	}), 
							new String[] {
								MessageText.getString("Button.yes"),
								MessageText.getString("Button.no")
							},
							1 );
					
					mb.open(new UserPrompterResultListener() {
						public void prompterClosed(int result) {
							if (result == 0) {
								TrackersUtil.getInstance().removeMultiTracker(selected);
								updateTemplates.run();
							}
						}
					});
				
			}
		});

		Label labelApply = new Label(cTemplate,SWT.NULL);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;
		labelApply.setLayoutData(gridData);
		Messages.setLanguageText(labelApply, "apply.selected.template");

		final Button btnReplace = new Button(cTemplate, SWT.PUSH);
		buttons.add( btnReplace );
		Messages.setLanguageText(btnReplace, "label.replace");
		btnReplace.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				Map<String,List<List<String>>> multiTrackers = TrackersUtil.getInstance().getMultiTrackers();
				String	selected = currentTemplate[0];	
				trackers = TorrentUtils.getClone(multiTrackers.get( selected ));
				refresh();
				computeSaveEnable();
			}
		});
		
		final Button btnMerge = new Button(cTemplate, SWT.PUSH);
		buttons.add( btnMerge );
		Messages.setLanguageText(btnMerge, "label.merge");
		btnMerge.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				Map<String,List<List<String>>> multiTrackers = TrackersUtil.getInstance().getMultiTrackers();
				String	selected = currentTemplate[0];
				trackers = TorrentUtils.mergeAnnounceURLs(trackers, multiTrackers.get( selected ));
				refresh();
				computeSaveEnable();
			}
		});
		
		final Button btnRemove = new Button(cTemplate, SWT.PUSH);
		buttons.add( btnRemove );
		Messages.setLanguageText(btnRemove, "Button.remove");
		btnRemove.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				Map<String,List<List<String>>> multiTrackers = TrackersUtil.getInstance().getMultiTrackers();
				String	selected = currentTemplate[0];
				trackers = TorrentUtils.removeAnnounceURLs(trackers, multiTrackers.get( selected ));
				refresh();
				computeSaveEnable();
			}
		});
		
		updateTemplates.run();
		
		Utils.makeButtonsEqualWidth( buttons );
		
    }


    Label labelSeparator = new Label(shell,SWT.SEPARATOR | SWT.HORIZONTAL);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    labelSeparator.setLayoutData(gridData);    
    
    	// button row 
    
    Composite cButtons = new Composite(shell, SWT.NONE);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    gridData.horizontalSpan = 3;
    cButtons.setLayoutData(gridData);
    GridLayout layoutButtons = new GridLayout();
    layoutButtons.numColumns = 4;
    cButtons.setLayout(layoutButtons);
    
    List<Button> buttons = new ArrayList<Button>();
    
    final Button btnedittext = new Button(cButtons,SWT.PUSH);
    buttons.add( btnedittext );
    gridData = new GridData();
    gridData.horizontalAlignment = GridData.END;
    btnedittext.setLayoutData(gridData);
    Messages.setLanguageText(btnedittext,"wizard.multitracker.edit.text");
    btnedittext.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
    	  
    	  btnSave.setEnabled( false );
    	  btnedittext.setEnabled( false );
    	  
    	  trackers = new ArrayList();
    	  TreeItem[] groupItems = treeGroups.getItems();

    	  for(int i = 0 ; i < groupItems.length ; i++) {
    		  TreeItem group = groupItems[i];      
    		  TreeItem[] trackerItems = group.getItems();
    		  List groupList = new ArrayList(group.getItemCount());
    		  for(int j = 0 ; j < trackerItems.length ; j++) {
    			  groupList.add(trackerItems[j].getText());
    		  }
    		  trackers.add(groupList);
    	  }
    	    
    	  final String	old_text = TorrentUtils.announceGroupsToText( trackers );
    	  
    	  final TextViewerWindow viewer =
    		  new TextViewerWindow(
    				  shell,
    				  "wizard.multitracker.edit.text.title", 
    				  "wizard.multitracker.edit.text.msg", 
    				  old_text, false, false );

    	  viewer.setEditable( true );
			
    	  viewer.addListener(
				new TextViewerWindow.TextViewerWindowListener()
				{
					public void 
					closed() 
					{
						try{
					    	  String new_text = viewer.getText();
					    	  
					    	  if ( !old_text.equals( new_text )){
					    		  
					    		  String[] lines = new_text.split( "\n" );
					    		  
					    		  StringBuffer valid_text = new StringBuffer( new_text.length()+1 );
					    		  
					    		  for ( String line: lines ){
					    			  
					    			  line = line.trim();
					    			  
					    			  if ( line.length() > 0 ){
					    				  
					    				  if ( !validURL( line )){
					    					  
					    					  continue;
					    				  }
					    			  }
					    			  
					    			  valid_text.append( line );
					    			  valid_text.append( "\n" );
					    		  }
					    		  
					    		  trackers = TorrentUtils.announceTextToGroups( valid_text.toString());
					    		  
					    		  refresh();
					    	  }
						}finally{
							
							computeSaveEnable();
							
							btnedittext.setEnabled( true );
						}
					}
				});
      }
    });
    
    Label label = new Label(cButtons,SWT.NULL);
    gridData = new GridData(GridData.FILL_HORIZONTAL );
    label.setLayoutData(gridData);
    
    btnSave = new Button(cButtons,SWT.PUSH);
    buttons.add( btnSave );
    gridData = new GridData();
    gridData.horizontalAlignment = GridData.END;
    btnSave.setLayoutData(gridData);
    Messages.setLanguageText(btnSave,"wizard.multitracker.edit.save");
    btnSave.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        update();
        shell.dispose();
      }
    });
    
    btnCancel = new Button(cButtons,SWT.PUSH);
    buttons.add( btnCancel );
    gridData = new GridData();
    gridData.horizontalAlignment = GridData.END;
    btnCancel.setLayoutData(gridData);
    Messages.setLanguageText(btnCancel,"Button.cancel");
    btnCancel.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        shell.dispose();
      }
    });
    
	Utils.makeButtonsEqualWidth( buttons );
	
    shell.setDefaultButton( btnSave );
    
    shell.addListener(SWT.Traverse, new Listener() {	
    	public void handleEvent(Event e) {
    		if ( e.character == SWT.ESC){
    			shell.dispose();
    		}
    	}
    });

    
    computeSaveEnable();
    refresh();
    constructMenu();
    
    editor = new TreeEditor (treeGroups);
    treeGroups.addSelectionListener(new SelectionAdapter() {
	    public void widgetSelected(SelectionEvent arg0) {	      
	      if(itemEdited != null && !itemEdited.isDisposed() && !editor.getEditor().isDisposed()){
	        itemEdited.setText(((Text)editor.getEditor()).getText());
	      }
	      
	      removeEditor();
	    }
    });
    
    Point size = shell.computeSize(500,SWT.DEFAULT);
    shell.setSize(size);
    
    Utils.centreWindow( shell );
    
    shell.open();
  }  
  
  private void update() {
    trackers = new ArrayList();
    TreeItem[] groupItems = treeGroups.getItems();
    
    for(int i = 0 ; i < groupItems.length ; i++) {
      TreeItem group = groupItems[i];      
      TreeItem[] trackerItems = group.getItems();
      List groupList = new ArrayList(group.getItemCount());
      for(int j = 0 ; j < trackerItems.length ; j++) {
        groupList.add(trackerItems[j].getText());
      }
      trackers.add(groupList);
    }
    
    listener.trackersChanged(oldName,currentName,trackers);
    oldName = currentName;
  }

  private void computeSaveEnable() 
  {
  	boolean	enabled = anonymous || !("".equals(currentName));
  	
  	if ( enabled ){
  		
 	  	TreeItem[] groupItems = treeGroups.getItems();
		 
 	  	outer:
	  	for(int i = 0 ; i < groupItems.length ; i++) {
	  		TreeItem group = groupItems[i];      
	  		TreeItem[] trackerItems = group.getItems();
	  		for(int j = 0 ; j < trackerItems.length ; j++) {
		  			
	  			if ( ! validURL(trackerItems[j].getText())){
	  				
	  				enabled = false;
	  				
	  				break outer;
		  		}
		  	}
	  	}
  	}
  	
  	if ( enabled != btnSave.getEnabled()){
  		
  		btnSave.setEnabled( enabled );
  	}
  }
  
  private void refresh() {
    treeGroups.removeAll();    
    Iterator iter = trackers.iterator();
    while(iter.hasNext()) {
      List trackerGroup = (List) iter.next();
      TreeItem itemRoot = newGroup();
      Iterator iter2 = trackerGroup.iterator();
      while(iter2.hasNext()) {
        String url =  (String) iter2.next();
        newTracker(itemRoot,url);
      }      
    }      
  }
  
  private void constructMenu() {
    menu = new Menu(shell,SWT.NULL);
    menu.addListener(SWT.Show, new Listener() {
      public void handleEvent(Event e) {
        //1. Empty the menu
        MenuItem[] items = menu.getItems();
        for(int i = 0 ; i < items.length ; i++) {
          items[i].dispose();
        }
        
        //2. Test for the number of element selected
        // should be 1
        if(treeGroups.getSelectionCount() != 1) {
          MenuItem item = new MenuItem(menu,SWT.NULL);
          Messages.setLanguageText(item,"wizard.multitracker.edit.newgroup");
          item.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event arg0) {
              TreeItem group = newGroup();
              TreeItem itemTracker = newTracker(group,"http://");
              editTreeItem(itemTracker);
            }
          });
          return;
        }
        
        //3. Grab the element
        final TreeItem treeItem = treeGroups.getSelection()[0];
        String type = (String) treeItem.getData("type");
        if(type.equals("tracker")) {
          //The Tracker menu
          MenuItem item = new MenuItem(menu,SWT.NULL);
          Messages.setLanguageText(item,"wizard.multitracker.edit.deletetracker");
          item.addListener(SWT.Selection,new Listener(){
            public void handleEvent(Event arg0) {
              treeItem.dispose();
            }
          });
          
          item = new MenuItem(menu,SWT.NULL);
          Messages.setLanguageText(item,"wizard.multitracker.edit.edit");
          item.addListener(SWT.Selection,new Listener(){
	          public void handleEvent(Event arg0) {
	            editTreeItem(treeItem);
	          }
          });
        } else
        if(type.equals("group")) {
          //The Group menu
          MenuItem item = new MenuItem(menu,SWT.NULL);
          Messages.setLanguageText(item,"wizard.multitracker.edit.newgroup");
          item.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event arg0) {
              TreeItem group = newGroup();
              TreeItem itemTracker = newTracker(group,"http://");
              editTreeItem(itemTracker);
            }
          });
          
          item = new MenuItem(menu,SWT.NULL);
          Messages.setLanguageText(item,"wizard.multitracker.edit.deletegroup");
          item.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event arg0) {
              TreeItem[] subItems = treeItem.getItems();
              for(int i = 0 ; i < subItems.length ; i++) {
                subItems[i].dispose();
              }
              treeItem.dispose();
            }
          });
                              
          new MenuItem(menu,SWT.SEPARATOR);
          
          item = new MenuItem(menu,SWT.NULL);
          Messages.setLanguageText(item,"wizard.multitracker.edit.newtracker");
          item.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event arg0) {
              TreeItem itemTracker = newTracker(treeItem,"http://");
              editTreeItem(itemTracker);
            }
          });
          /*
          new MenuItem(menu,SWT.SEPARATOR);
          
          item = new MenuItem(menu,SWT.NULL);
          Messages.setLanguageText(item,"wizard.multitracker.edit.tracker.moveUp");
          
          item = new MenuItem(menu,SWT.NULL);
          Messages.setLanguageText(item,"wizard.multitracker.edit.tracker.moveDown");
          */    
        }
      }
    });
    treeGroups.setMenu(menu);
  }
  
  private void editTreeItem(final TreeItem item) {
    // Clean up any previous editor control
    Control oldEditor = editor.getEditor();
    if (oldEditor != null)
      oldEditor.dispose();	
 
    itemEdited = item;
    // The control that will be the editor must be a child of the Tree
    final Text text = new Text(treeGroups, SWT.BORDER);
    text.setText(item.getText());
    text.setForeground(item.getForeground());
    text.setSelection(item.getText().length());
    text.addListener (SWT.DefaultSelection, new Listener () {
      public void handleEvent (Event e) {
      	String url = text.getText();
      	if ( validURL(url)){
      		text.setForeground( null );
      		item.setForeground( null );
      	}else{
      		text.setForeground( Colors.colorError );
      		item.setForeground( Colors.colorError );
      	}
      	item.setText(url);
        computeSaveEnable();
        removeEditor();
      }
    });
    
    text.addListener(SWT.Modify, new Listener() {
    	public void handleEvent (Event e) {
    		String url = text.getText();
    		if ( validURL(url)){
    			text.setForeground( null );
    			item.setForeground( null );
    		}else{
    			text.setForeground( Colors.colorError );
    			item.setForeground( Colors.colorError );
    		}
    		item.setText(url);
     		computeSaveEnable();
    	}
    });
    
    text.addKeyListener(new KeyAdapter() {
	    public void keyReleased(KeyEvent keyEvent) {
	     if(keyEvent.character == SWT.ESC) {	       
	       removeEditor();
	     }
	    }
    });
    

    //The text editor must have the same size as the cell and must
    //not be any smaller than 50 pixels.
    editor.horizontalAlignment = SWT.LEFT;
    editor.grabHorizontal = true;
    editor.minimumWidth = 50;

    Rectangle r = text.computeTrim(0, 0, 100, text.getLineHeight());
    editor.minimumHeight = r.height;


    // Open the text editor on the selected row.
    editor.setEditor (text, item);

    // Assign focus to the text control
    text.setFocus ();
  }
  
  private boolean
  validURL(
  	String	str )
  {
  	try{
  		URL url = new URL(str);
  		
  		String prot = url.getProtocol().toLowerCase();
  		
  		if ( prot.equals( "http") || prot.equals( "https" ) || prot.equals( "udp") || prot.equals( "dht")){
  			
  			return( true );
  		}
  		
  		return( false );
  		
  	}catch( Throwable e ){
  		
  		return( false );
  	}
  }
  
  private void removeEditor() {
    Control oldEditor = editor.getEditor();
    if (oldEditor != null && !oldEditor.isDisposed()){
      oldEditor.dispose();
    }
  }
  
  private TreeItem newGroup() {
    TreeItem item = new TreeItem(treeGroups,SWT.NULL);
    item.setData("type","group");
    Messages.setLanguageText(item, "wizard.multitracker.group");
    return item;
  }
  
  private TreeItem newTracker(TreeItem root,String url) {
    TreeItem item = new TreeItem(root,SWT.NULL);
    item.setText(url);
    item.setData("type","tracker");
    root.setExpanded(true);
    return item;
  }
}
