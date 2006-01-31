/*
 * Created on 21.07.2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gudy.azureus2.ui.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.gudy.azureus2.ui.swt.components.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.config.IParameter;
import org.gudy.azureus2.ui.swt.views.TableView;

import java.util.regex.Pattern;


/**
 * @author Arbeiten
 */
public class Messages {

  private static final Pattern HIG_ELLIP_EXP = Pattern.compile("([\\.]{3})"); // rec. hig style on some platforms

  /**
   * 
   */
  private Messages() {

    // TODO Auto-generated constructor stub
  }
  public static void updateLanguageForControl(Widget widget) {
    if (widget == null || widget.isDisposed())
      return;

    updateLanguageFromData(widget,null);	// OK, so we loose parameters on language change...
    updateToolTipFromData(widget);

    if (widget instanceof CTabFolder) {
      CTabFolder folder = (CTabFolder) widget;
      CTabItem[] items = folder.getItems();
      for (int i = 0; i < items.length; i++) {
        updateLanguageForControl(items[i]);
        updateLanguageForControl(items[i].getControl());
      }
    } else if (widget instanceof TabFolder) {
      TabFolder folder = (TabFolder) widget;
      TabItem[] items = folder.getItems();
      for (int i = 0; i < items.length; i++) {
        updateLanguageForControl(items[i]);
        updateLanguageForControl(items[i].getControl());
      }
    }
    else if(widget instanceof CoolBar) {
      CoolItem[] items = ((CoolBar)widget).getItems();
      for(int i = 0 ; i < items.length ; i++) {
        Control control = items[i].getControl();
        updateLanguageForControl(control);
      }
    }
    else if(widget instanceof ToolBar) {
      ToolItem[] items = ((ToolBar)widget).getItems();
      for(int i = 0 ; i < items.length ; i++) {
        updateLanguageForControl(items[i]);
      }
    }
    else if (widget instanceof Composite) {
      Composite group = (Composite) widget;
      Control[] controls = group.getChildren();
      for (int i = 0; i < controls.length; i++) {
        updateLanguageForControl(controls[i]);
      }
      if (widget instanceof Table) {
        Table table = (Table) widget;
        TableColumn[] columns = table.getColumns();
        for (int i = 0; i < columns.length; i++) {
          updateLanguageFromData(columns[i], null);
        }
        updateLanguageForControl(table.getMenu());

        TableView tv = (TableView)widget.getData("TableView");
        if (tv != null)
          tv.tableInvalidate();
      }
      else if (widget instanceof Tree) {
        Tree tree = (Tree) widget;
        TreeItem[] treeitems = tree.getItems();
        for (int i = 0; i < treeitems.length; i++) {
          updateLanguageForControl(treeitems[i]);
        }
      }
      else if (widget instanceof TreeItem) {
        TreeItem treeItem = (TreeItem) widget;
        TreeItem[] treeitems = treeItem.getItems();
        for (int i = 0; i < treeitems.length; i++) {
          updateLanguageForControl(treeitems[i]);
        }
      }
        
      group.layout();
    }
    else if (widget instanceof MenuItem) {
      MenuItem menuItem = (MenuItem) widget;
      updateLanguageForControl(menuItem.getMenu());
    }
    else if (widget instanceof Menu) {
      Menu menu = (Menu) widget;
      if (menu.getStyle() == SWT.POP_UP)
        System.out.println("POP_UP");

      MenuItem[] items = menu.getItems();
      for (int i = 0; i < items.length; i++) {
        updateLanguageForControl(items[i]);
      }
    }
    else if (widget instanceof TreeItem) {
      TreeItem treeitem = (TreeItem) widget;
      TreeItem[] treeitems = treeitem.getItems();
      for (int i = 0; i < treeitems.length; i++) {
        updateLanguageFromData(treeitems[i], null);
      }
    }
    
  }

  public static void setLanguageText(IParameter paramObject, String key) {
    setLanguageText(paramObject.getControl(), key, false);
  }

  public static void setLanguageText(IParameter paramObject, String key, String[] params) {
    setLanguageText(paramObject.getControl(), key, params, false );
  }

  public static void setLanguageText(Widget widget, String key) {
    setLanguageText(widget, key, false);
  }
  
  public static void setLanguageText(Widget widget, String key, String[]params) {
    setLanguageText(widget, key, params, false);
  }

  public static void setLanguageText(Widget widget, String key, boolean setTooltipOnly) {
  	setLanguageText( widget, key, null, setTooltipOnly );
  }
  
  private static void 
  setLanguageText(Widget widget, String key, String[] params, boolean setTooltipOnly) {
  	widget.setData(key);
  	if(!setTooltipOnly)
      updateLanguageFromData(widget, params);
  	updateToolTipFromData(widget);
  }
  
  public static void setLanguageText(BufferedWidget buffered_widget, String key) {
    setLanguageText(buffered_widget.getWidget(), key, false);
  }

  public static void setLanguageText(BufferedWidget buffered_widget, String key, boolean setTooltipOnly) {
    setLanguageText(buffered_widget.getWidget(), key, setTooltipOnly);
  }
  
  private static void updateToolTipFromData(Widget widget) {
    if(widget instanceof Control) {
      String key = (String) widget.getData();
      if(key != null) {
        if(!key.endsWith(".tooltip"))
          key += ".tooltip";
        String toolTip = MessageText.getString(key);
        if(!toolTip.equals('!' + key + '!')) {
          ((Control)widget).setToolTipText(toolTip);
        }
      }
    } else if(widget instanceof ToolItem) {
      String key = (String) widget.getData();
      if(key != null) {
        if(!key.endsWith(".tooltip"))
          key += ".tooltip";
        String toolTip = MessageText.getString(key);
        if(!toolTip.equals('!' + key + '!')) {
          ((ToolItem)widget).setToolTipText(toolTip);
        }
      }
    } else if (widget instanceof TableColumn) {
      String key = (String) widget.getData();
			if (key != null) {
				if (!key.endsWith(".info"))
					key += ".info";
				String toolTip = MessageText.getString(key, (String) null);
				if (toolTip == null)
					toolTip = MessageText.getString(key.substring(0, key.length() - 5),
							(String) null);
				if (toolTip != null) {
					try {
						((TableColumn) widget).setToolTipText(toolTip);
					} catch (NoSuchMethodError e) {
						// Pre SWT 3.2
					}
				}
			}
    }
  }
  

  private static void updateLanguageFromData(Widget widget,String[] params) {
      if (widget.getData() != null) {
        String key = null;
        try {
          key = (String) widget.getData();
        } catch(ClassCastException e) {
        }
        
        if(key == null) return;        
        if(key.endsWith(".tooltip")) return;
        
        String	message;
        
        if ( params == null ){
        	
        	message = MessageText.getString((String) widget.getData());
        }else{
        	
           	message = MessageText.getString((String) widget.getData(), params);         	
        }        
        
        if (widget instanceof MenuItem) {
            final MenuItem menuItem = ((MenuItem) widget);
            boolean indent = (menuItem.getData("IndentItem") != null);

            if(Constants.isOSX)
                message = HIG_ELLIP_EXP.matcher(message).replaceAll("\u2026"); // hig style - ellipsis

            menuItem.setText(indent ? "  " + message : message);

            if(menuItem.getAccelerator() != 0) // opt-in only for now; remove this conditional check to allow accelerators for arbitrary MenuItem objects
                KeyBindings.setAccelerator(menuItem, (String)menuItem.getData()); // update keybinding
        }
        else if (widget instanceof TableColumn)
           ((TableColumn) widget).setText(message);
        else if (widget instanceof Label)
        	// Disable Mnemonic when & is before a space.  Otherwise, it's most
        	// likely meant to be a Mnemonic
          ((Label) widget).setText(message.replaceAll("& ", "&& "));
        else if (widget instanceof CLabel)
          ((CLabel) widget).setText(message.replaceAll("& ", "&& "));
        else if (widget instanceof Group)
           ((Group) widget).setText(message);
        else if (widget instanceof Button)
           ((Button) widget).setText(message);
        else if (widget instanceof CTabItem)
           ((CTabItem) widget).setText(message);
        else if (widget instanceof TabItem)
           ((TabItem) widget).setText(message);
        else if (widget instanceof TreeItem)
          ((TreeItem) widget).setText(message);
        else if(widget instanceof Shell) 
          ((Shell) widget).setText(message);
        else if(widget instanceof ToolItem) 
          ((ToolItem) widget).setText(message);
        else
          System.out.println("No cast for " + widget.getClass().getName());
      } 
  }

  public static void setLanguageTooltip(Widget widget, String key) {
    widget.setData(key);
    updateTooltipLanguageFromData(widget);
  }

  public static void updateTooltipLanguageFromData(Widget widget) {
    if (widget.getData() != null) {
      if (widget instanceof CLabel)
        ((CLabel) widget).setToolTipText(MessageText.getString((String) widget.getData()));
      else
        System.out.println("No cast for " + widget.getClass().getName());
    }
  }
}
