/*
 * File    : ErrorPopupShell.java
 * Created : 15 mars 2004
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
package org.gudy.azureus2.ui.swt.shells;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.animations.Animator;
import org.gudy.azureus2.ui.swt.animations.shell.AnimableShell;
import org.gudy.azureus2.ui.swt.animations.shell.LinearAnimator;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;


/**
 * @author Olivier Chalouhi
 *
 */
public class MessagePopupShell implements AnimableShell {
  
  private Shell shell;
  private Shell detailsShell;  
  Image shellImg;
  private Display display;

  public static final String ICON_ERROR 	= "error";
  public static final String ICON_WARNING 	= "warning";
  public static final String ICON_INFO	 	= "info";

   private static LinkedList viewStack;
   private Timer closeTimer;

    private String icon;

  static {
      viewStack = new LinkedList();
  }

  public MessagePopupShell(Display display,String icon,String title,String errorMessage,String details) {
    closeTimer = new Timer(true);

    this.display = display;
    this.icon = icon;
    detailsShell = new Shell(display,SWT.BORDER | SWT.ON_TOP);
    if(! Constants.isOSX) {
      detailsShell.setImage(ImageRepository.getImage("azureus"));
    }
    
    detailsShell.setLayout(new FillLayout());
    StyledText textDetails = new StyledText(detailsShell, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);  
    textDetails.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
    textDetails.setWordWrap( true );
    detailsShell.layout();    
    detailsShell.setSize(500,300);    
    
    
    shell = new Shell(display,SWT.ON_TOP);
    shell.setSize(250,150);
    if(! Constants.isOSX) {
      shell.setImage(ImageRepository.getImage("azureus"));
    }
    FormLayout layout = new FormLayout();
    layout.marginHeight = 0; layout.marginWidth = 0; 
    try {
      layout.spacing = 0;
    } catch (NoSuchFieldError e) {
      /* Ignore for Pre 3.0 SWT.. */
    }
    shell.setLayout(layout);
    
    
    shellImg = new Image(display,ImageRepository.getImage("popup"),SWT.IMAGE_COPY);
    GC gcImage = new GC(shellImg);
    
    Image imgIcon = ImageRepository.getImage(icon);
    imgIcon.setBackground(shell.getBackground());
    
    gcImage.drawImage(imgIcon,5,5);
    
    Font tempFont = shell.getFont();
    FontData[] fontDataMain = tempFont.getFontData();
    for(int i=0 ; i < fontDataMain.length ; i++) {             
      fontDataMain[i].setStyle(SWT.BOLD);
      fontDataMain[i].setHeight((int) (fontDataMain[i].getHeight() * 1.2));
    }
    
    Font fontTitle = new Font(display,fontDataMain);
    gcImage.setFont(fontTitle);
    
    GCStringPrinter.printString(gcImage,title,new Rectangle(59,11,182,43));
    
    gcImage.setFont(tempFont);
    fontTitle.dispose();
    
    
    boolean bItFit = GCStringPrinter.printString(gcImage,errorMessage, 
                                                 new Rectangle(5,40,240,60));
    
    gcImage.dispose();            
    if (!bItFit && details == null)
      details = errorMessage;
    
    if(details != null)
      textDetails.setText(details);

    final Button btnDetails = new Button(shell,SWT.TOGGLE);
    Messages.setLanguageText(btnDetails,"popup.error.details");    
    btnDetails.setEnabled(details != null);
    
    final Button btnHide = new Button(shell,SWT.PUSH);
    Messages.setLanguageText(btnHide,"popup.error.hide");    
    
    Label lblImage = new Label(shell,SWT.NULL);
    lblImage.setImage(shellImg);
    
    FormData formData;
    
    formData = new FormData();    
    formData.right = new FormAttachment(btnHide,-5);
    formData.bottom = new FormAttachment(100,-5);
    btnDetails.setLayoutData(formData);
    
    formData = new FormData();
    formData.right = new FormAttachment(100,-5);
    formData.bottom = new FormAttachment(100,-5);
    btnHide.setLayoutData(formData);
    
    formData = new FormData();
    formData.left = new FormAttachment(0,0);
    formData.top = new FormAttachment(0,0);
    lblImage.setLayoutData(formData);
    
    shell.layout();
    shell.setTabList(new Control[] {btnDetails, btnHide});

    btnHide.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event arg0) {
          btnHide.setEnabled(false);
          btnDetails.setEnabled(false);
          hideShell();
      }
    });
    
    btnDetails.addListener(SWT.Selection,new Listener() {
      public void handleEvent(Event arg0) {
       detailsShell.setVisible(btnDetails.getSelection());
      }
    });
    
    Rectangle bounds = display.getClientArea();
    x0 = bounds.x + bounds.width - 255;
    x1 = bounds.x + bounds.width;

    y0 = bounds.y + bounds.height;
    y1 = bounds.y + bounds.height - 155;
    
    shell.setLocation(x0,y0);
    viewStack.addFirst(new WeakReference(this));
    detailsShell.setLocation(x1-detailsShell.getSize().x,y1-detailsShell.getSize().y);
    currentAnimator = new LinearAnimator(this,new Point(x0,y0),new Point(x0,y1),20,30);
    currentAnimator.start();
    shell.open();
    }

    private void hideShell()
    {
        if(currentAnimator == null) {
          closeTimer.cancel();
          detailsShell.setVisible(false);
          detailsShell.forceActive();
          if(!Constants.isOSX){detailsShell.forceFocus();}
          currentAnimator = new LinearAnimator(this,new Point(x0,y1),new Point(x1,y1),20,30);
          currentAnimator.start();
          closeAfterAnimation = true;
        }
    }


  private Animator currentAnimator;
  private boolean closeAfterAnimation;
  int x0,y0,x1,y1;
  
  public void animationEnded(Animator source) {
    if(source == currentAnimator) {
      currentAnimator = null;
    }
    if(closeAfterAnimation) {   
      if(display == null || display.isDisposed())
        return;
      display.asyncExec(new AERunnable(){
        public void runSupport() {
          viewStack.removeFirst();
          shell.dispose();
          detailsShell.dispose();
          shellImg.dispose();          
        }
      });     
    }
    else {
        scheduleAutocloseTask();
    }
  }

   private void scheduleAutocloseTask() {
       final int delay = COConfigurationManager.getIntParameter("Message Popup Autoclose in Seconds") * 1000;
        if(delay < 1000)
            return;

       closeTimer.scheduleAtFixedRate(new TimerTask() {
           public void run() {
               display.syncExec(new AERunnable() {
                    public void runSupport() {
                        if(shell.isDisposed()) {
                            closeTimer.cancel();
                            return;
                        }

                        final boolean notInfoType = MessagePopupShell.this.icon != ICON_INFO;
                        if(notInfoType) {
                            closeTimer.cancel();
                            return;
                        }

                        final boolean notTopWindow = ((WeakReference)viewStack.getFirst()).get() != MessagePopupShell.this;
                        final boolean animationInProgress = currentAnimator != null;
                        final boolean detailsOpen = (!detailsShell.isDisposed() && detailsShell.isVisible());

                        final Control cc = display.getCursorControl();
                        boolean mouseOver = (cc == shell);
                        if(!mouseOver) {
                            final Control[] childControls = shell.getChildren();
                            for(int i = 0; i < childControls.length; i++) {
                                if(childControls[i] == cc) {
                                    mouseOver = true;
                                    break;
                                }
                            }
                        }

                        if(notTopWindow || mouseOver || animationInProgress || detailsOpen)
                            return;

                        hideShell();
                    }
                });
           }
       }, delay, delay);
   }

  public void animationStarted(Animator source) {
  }

  
  public Shell getShell() {
    return shell;
  }
  
  public void reportPercent(int percent) {    
  }
}
