/*
 * Created on 2 feb. 2004
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;

public class TextViewerWindow {
  public TextViewerWindow(String sTitleID, String sMessageID, String sText) {
    final Display display = SWTThread.getInstance().getDisplay();
    final Shell shell = org.gudy.azureus2.ui.swt.components.shell.ShellFactory.createShell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

    if (sTitleID != null) shell.setText(MessageText.keyExists(sTitleID)?MessageText.getString(sTitleID):sTitleID);
    
    if(! Constants.isOSX) {
      shell.setImage(ImageRepository.getImage("azureus"));
    }
    
    GridLayout layout = new GridLayout();
    shell.setLayout(layout);

    Label label = new Label(shell, SWT.NONE);
    if (sMessageID != null) label.setText(MessageText.keyExists(sMessageID)?MessageText.getString(sMessageID):sMessageID);
    GridData gridData = new GridData();
    gridData.widthHint = 200;
    label.setLayoutData(gridData);

    final Text txtInfo = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
    gridData = new GridData();
    gridData.widthHint = 300;
    txtInfo.setLayoutData(gridData);
    txtInfo.setText(sText);

    Button ok = new Button(shell, SWT.PUSH);
    ok.setText(MessageText.getString("Button.ok"));
    gridData = new GridData();
    gridData.widthHint = 70;
    ok.setLayoutData(gridData);
    shell.setDefaultButton(ok);
    ok.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        try {
        	shell.dispose();
        }
        catch (Exception e) {
        	Debug.printStackTrace( e );
        }
      }
    });

    shell.pack();
    shell.open();
    while (!shell.isDisposed())
      if (!display.readAndDispatch()) display.sleep();
  }
}
