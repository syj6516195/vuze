/*
 * File    : ProgressWindow.java
 * Created : 15-Jan-2004
 * By      : parg
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

package org.gudy.azureus2.ui.swt.sharing.progress;

/**
 * @author parg
 *
 */
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import org.gudy.azureus2.ui.swt.animations.Animator;
import org.gudy.azureus2.ui.swt.animations.shell.AnimableShell;
import org.gudy.azureus2.ui.swt.animations.shell.LinearAnimator;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.ui.swt.shells.PopupShell;

import org.gudy.azureus2.core3.internat.MessageText;

import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.pluginsimpl.local.*;

public class 
ProgressWindow
	implements ShareManagerListener
{
	protected progressDialog	dialog;
	
	protected Display			display;
	
	protected StyledText		tasks;
	protected ProgressBar		progress;
	
	protected boolean			shell_opened;
	
	public
	ProgressWindow()
	{
		display = MainWindow.getWindow().getDisplay();
		
		if ( display.isDisposed()){
			
			return;
		}
		
		try{
			dialog = new progressDialog( display );
			
			PluginInitializer.getDefaultInterface().getShareManager().addListener(this);
			
		}catch( ShareException e ){
			
			e.printStackTrace();
		}
		
	}
	
	protected class
	progressDialog extends PopupShell implements AnimableShell
	{
		//protected Shell			shell;
		
		protected
		progressDialog(
			Display				display )
		{
      super(display);
			if ( display.isDisposed()){
								
				return;
			}
			
			//shell = new Shell( display, SWT.DIALOG_TRIM );
			
			//shell.setImage(ImageRepository.getImage("azureus"));
			shell.setText(MessageText.getString("sharing.progress.title"));
			
			//GridLayout layout = new GridLayout();
			//layout.numColumns = 3;
			
			//shell.setLayout (layout);
			
			//Composite panel = new Composite(shell, SWT.NULL);
			//GridData gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER | GridData.FILL_HORIZONTAL);
			//panel.setLayoutData(gridData);
			//layout = new GridLayout();
			//layout.numColumns = 1;
			//panel.setLayout(layout);

			tasks = new StyledText(shell, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);  
			tasks.setBackground(display.getSystemColor(SWT.COLOR_WHITE));      
      
      
			//gridData = new GridData(GridData.FILL_BOTH);
			//gridData.heightHint = 200;
			//tasks.setLayoutData(gridData);

			progress = new ProgressBar(shell, SWT.NULL);
			progress.setMinimum(0);
			progress.setMaximum(100);            
      
			//gridData = new GridData(GridData.FILL_HORIZONTAL);
			//progress.setLayoutData(gridData);		
			
			// buttons
						
			//Composite comp = new Composite(panel,SWT.NULL);
			//gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
			//gridData.grabExcessHorizontalSpace = true;
			//gridData.horizontalSpan = 1;
			//comp.setLayoutData(gridData);
			//GridLayout layoutButtons = new GridLayout();
			//layoutButtons.numColumns = 2;
			//comp.setLayout(layoutButtons);
			
			
			//new Label(comp,SWT.NULL);
			
			Button hide_button = new Button(shell,SWT.PUSH);
			hide_button.setText(MessageText.getString("sharing.progress.hide"));
			//gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END | GridData.HORIZONTAL_ALIGN_FILL);
			//gridData.grabExcessHorizontalSpace = true;
			//gridData.widthHint = 70;
			//hide_button.setLayoutData(gridData);
      
      
      //Layout :
      
      //Progress Bar on bottom, with Hide button next to it.
      FormData formData;
      formData = new FormData();
      formData.right = new FormAttachment(100,-5);
      formData.bottom = new FormAttachment(100,-10);
      
      hide_button.setLayoutData(formData);
      
      formData = new FormData();
      formData.right = new FormAttachment(hide_button,-5);
      formData.left = new FormAttachment(0,50);
      formData.bottom = new FormAttachment(100,-10);
      
      progress.setLayoutData(formData);
      
      formData = new FormData();
      formData.right = new FormAttachment(100,-5);
      formData.bottom = new FormAttachment(100,-50);
      formData.top = new FormAttachment(0,5);
      formData.left = new FormAttachment(0,5);
      
      tasks.setLayoutData(formData);
      
      
      layout();
      
			hide_button.addListener(SWT.Selection,new Listener() {
				public void handleEvent(Event e) {
					hidePanel();
				}
			});
			
			
			shell.setDefaultButton( hide_button );
			
			shell.addListener(SWT.Traverse, new Listener() {	
				public void handleEvent(Event e) {
					if ( e.character == SWT.ESC){
						hidePanel();
					}
				}
			});

			
      Rectangle bounds = display.getClientArea();    
      x0 = bounds.width - 255;
      x1 = bounds.width;
      
      y0 = bounds.height;
      y1 = bounds.height - 155;
					
			shell.setLocation(x0,y0);
		}
		
		protected void
		hidePanel()
		{			
      currentAnimator = new LinearAnimator(this,new Point(x0,y1),new Point(x1,y1),15,30);
      currentAnimator.start();
      hideAfter = true;
		}
		
		protected void
		showPanel()
		{
			if ( !shell_opened ){
			
				shell_opened = true;
				
				shell.open();        
			}
      
      if(currentAnimator == null) {
        currentAnimator = new LinearAnimator(this,new Point(x0,y0),new Point(x0,y1),15,30);
        currentAnimator.start();
      }
            
			shell.moveAbove( MainWindow.getWindow().getShell());
			
			if ( !shell.isVisible()){
				
				shell.setVisible(true);
			}
		}
    
    
    
    //Animation properties
    Animator currentAnimator;
    int x0,y0,x1,y1;
    
    boolean isAnimated;
    boolean hideAfter;
    
    public void animationEnded(Animator source) {
      if(source != currentAnimator) 
        return;
      isAnimated = false;
      currentAnimator = null;
      if(hideAfter) {
        hideAfter = false;
        if(display == null || display.isDisposed())
          return;
        display.asyncExec(new Runnable() {          
          public void run() {
            shell.setVisible(false);
          }
        });
      }
    }

    public void animationStarted(Animator source) {
    }

    public Shell getShell() {
      return shell;
    }

    public void reportPercent(int percent) {
    }
	}
	
	public void
	resourceAdded(
		ShareResource		resource )
	{		
		reportCurrentTask( "Resource added: " + resource.getName());
	}
	
	public void
	resourceModified(
		ShareResource		resource )
	{
		reportCurrentTask( "Resource modified: " + resource.getName());
	}
	
	public void
	resourceDeleted(
		ShareResource		resource )
	{
		reportCurrentTask( "Resource deleted: " + resource.getName());	
	}
	
	public void
	reportProgress(
		final int		percent_complete )
	{
		if (display != null && !display.isDisposed()){
			
			display.asyncExec(new Runnable()
				{
					public void 
					run()
					{
						if (progress != null && !progress.isDisposed()){
							
							//dialog.showPanel();
							
							progress.setSelection(percent_complete);
						}

					}
				});
		}
	}
	
	public void
	reportCurrentTask(
		final String	task_description )
	{
		if (display != null && !display.isDisposed()){
			
			display.asyncExec(new Runnable() 
				{
					public void run()
					{
						if (tasks != null && !tasks.isDisposed()){
								
							dialog.showPanel();
							
							tasks.append(task_description + Text.DELIMITER);
							
							int lines = tasks.getLineCount();
							
							// tasks(nbLines - 2, 1, colors[_color]);
							
							tasks.setTopIndex(lines-1);						
						}
					}
				});
		}	
	}
}