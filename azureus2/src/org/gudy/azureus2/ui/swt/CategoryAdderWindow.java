/*
 * Created on 2 feb. 2004
 *
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.components.ControlUtils;

/**
 * @author Olivier
 * 
 */
public class CategoryAdderWindow {
  private Category newCategory = null;
  public CategoryAdderWindow(final Display display) {
    final Shell shell = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

    shell.setText(MessageText.getString("CategoryAddWindow.title"));
    if(! Constants.isOSX) {
      shell.setImage(ImageRepository.getImage("azureus"));
    }
    GridLayout layout = new GridLayout();
    shell.setLayout(layout);

    Label label = new Label(shell, SWT.NONE);
    label.setText(MessageText.getString("CategoryAddWindow.message"));    
    GridData gridData = new GridData();
    gridData.widthHint = 200;
    label.setLayoutData(gridData);

    final Text category = new Text(shell, SWT.BORDER);
    gridData = new GridData();
    gridData.widthHint = 300;
    category.setLayoutData(gridData);

    Composite panel = new Composite(shell, SWT.NULL);
    final RowLayout rLayout = new RowLayout();
    rLayout.marginTop = 0;
    rLayout.marginLeft = 0;
    rLayout.marginBottom = 0;
    rLayout.marginRight = 0;
    rLayout.fill = true;
    rLayout.spacing = ControlUtils.getButtonMargin();
    panel.setLayout(rLayout);
    gridData = new GridData();
    gridData.horizontalAlignment = (Constants.isOSX) ? SWT.END : SWT.CENTER;
    panel.setLayoutData(gridData);

    Button ok;
    Button cancel;
    if(Constants.isOSX) {
        cancel = createAlertButton(panel, "Button.cancel");
        ok = createAlertButton(panel, "Button.ok");
    }
    else {
        ok = createAlertButton(panel, "Button.ok");
        cancel = createAlertButton(panel, "Button.cancel");
    }

    ok.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        try {
          if (category.getText() != "") {
           newCategory = CategoryManager.createCategory(category.getText());
          }
        	
        	shell.dispose();
        }
        catch (Exception e) {
        	Debug.printStackTrace( e );
        }
      }
    });
    cancel.addListener(SWT.Selection, new Listener() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
       */
      public void handleEvent(Event event) {
        shell.dispose();
      }
    });

    shell.setDefaultButton(ok);

    shell.pack();
    Utils.createURLDropTarget(shell, category);
    Utils.centreWindow(shell);
    shell.open();
    while (!shell.isDisposed())
      if (!display.readAndDispatch()) display.sleep();
  }

  private static Button createAlertButton(final Composite panel, String localizationKey)
  {
      final Button button = new Button(panel, SWT.PUSH);
      button.setText(MessageText.getString(localizationKey));
      final RowData rData = new RowData();
      rData.width = Math.max(
              ControlUtils.getDialogButtonMinWidth(),
              button.computeSize(SWT.DEFAULT,  SWT.DEFAULT).x
        );
      button.setLayoutData(rData);
      return button;
  }

  public Category getNewCategory() {
    return newCategory;
  }
}
