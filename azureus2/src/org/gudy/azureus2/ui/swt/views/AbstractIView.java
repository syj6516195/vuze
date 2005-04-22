/*
 * Created on 29 juin 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Tab;

/**
 * @author Ren�
 * 
 */
public abstract class AbstractIView implements IView {

  protected AEMonitor this_mon 	= new AEMonitor( "AbstractIView" );

  public void initialize(Composite composite){    
  }
  
  public final void setTabListener() {
    Tab.addTabKeyListenerToComposite(getComposite());
  }
  
  public Composite getComposite(){ return null; }
  public void refresh(){}
  
  /**
   * A basic implementation that disposes the composite
   * Should be called with super.delete() from any extending class.
   * Images, Colors and such SWT handles must be disposed by the class itself.
   */
  public void delete(){
    Composite comp = getComposite();
    if (!comp.isDisposed())
      comp.dispose();
  }

  public String getData(){ return null; }

  public String getFullTitle(){
    return MessageText.getString(getData());
  }

  public final String getShortTitle() {
    String shortTitle = getFullTitle();
    if(shortTitle != null && shortTitle.length() > 30) {
      shortTitle = shortTitle.substring(0,30) + "...";
    }
    return shortTitle;
	}
  
  public void updateLanguage() {
    Messages.updateLanguageForControl(getComposite());
  }
  
  
  public boolean isEnabled(String itemKey) {
    return false;
  }
  
  public boolean isSelected(String itemKey) {
    return false;
  }

  public void itemActivated(String itemKey) {   
  }

  public void
  generateDiagnostics(
	IndentWriter	writer )
  {
	  writer.println( "  Diagnostics for " + this + " (" + getFullTitle()+ ")");
  }
}
