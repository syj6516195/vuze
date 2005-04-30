/*
 * Created on 10 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.config.*;

/**
 * @author Olivier
 * 
 */
public class 
IntParameter 
	extends Parameter
{
  Text inputField;
  int iMinValue = 0;
  int iMaxValue = -1;
  int iDefaultValue;
  String sParamName;
  boolean allowZero = false;
  boolean generateIntermediateEvents = true;

  boolean value_is_changing_internally;
    
  public IntParameter(Composite composite, final String name) {
    iDefaultValue = COConfigurationManager.getIntParameter(name);
    initialize(composite,name);
  }

  public IntParameter(Composite composite, final String name, int defaultValue) {
    iDefaultValue = defaultValue;
    initialize(composite, name);
  }
  
  
  public IntParameter(Composite composite, final String name, int defaultValue,boolean generateIntermediateEvents) {
    iDefaultValue = defaultValue;
    this.generateIntermediateEvents = generateIntermediateEvents;
    initialize(composite, name);
  }
  
  
  public IntParameter(Composite composite,
                      final String name,
                      int minValue,
                      int maxValue,
                      boolean allowZero,
                      boolean generateIntermediateEvents ) {
    iDefaultValue = COConfigurationManager.getIntParameter(name);
    iMinValue = minValue;
    iMaxValue = maxValue;
    this.allowZero = allowZero;
    this.generateIntermediateEvents = generateIntermediateEvents;
    initialize(composite,name);
  }
  
    
  public void initialize(Composite composite, String name) {
    sParamName = name;

    inputField = new Text(composite, SWT.BORDER);
    int value = COConfigurationManager.getIntParameter(name, iDefaultValue);
    inputField.setText(String.valueOf(value));
    inputField.addListener(SWT.Verify, new Listener() {
      public void handleEvent(Event e) {
        String text = e.text;
        char[] chars = new char[text.length()];
        text.getChars(0, chars.length, chars, 0);
        for (int i = 0; i < chars.length; i++) {
          if (!('0' <= chars[i] && chars[i] <= '9')) {
            e.doit = false;
            return;
          }
        }
      }
    });

    if(generateIntermediateEvents) {
      inputField.addListener(SWT.Modify, new Listener() {
        public void handleEvent(Event event) {
        	checkValue();
        }
      });
    }

    
    inputField.addListener(SWT.FocusOut, new Listener() {
      public void handleEvent(Event event) {
      	checkValue();
      }
    });
  }
  
  public void
  setAllowZero(
  	boolean		allow )
  {
  	allowZero	= allow;
  }
  
  public void
  setMinimumValue(
  	int		value )
  {
  	iMinValue	= value;
  }
  public void
  setMaximumValue(
  	int		value )
  {
  	iMaxValue	= value;
  }
  
  protected void
  checkValue()
 {
    try{
    	int	old_val = COConfigurationManager.getIntParameter( sParamName, -1 );
    	
        int new_val = Integer.parseInt(inputField.getText());
        
        int	original_new_val	= new_val;
               
        if (new_val < iMinValue) {
          if (!(allowZero && new_val == 0)) {
          	new_val = iMinValue;
          }
        }
        
        if (new_val > iMaxValue) {
          if (iMaxValue > -1) {
            new_val = iMaxValue;
          }
        }
        
        if ( new_val == old_val ){
        	
        	if ( new_val != original_new_val ){
        		
        		inputField.setText(String.valueOf(new_val));
        	}
        	
        }else{
        	COConfigurationManager.setParameter(sParamName, new_val);
        	
        	if ( new_val != original_new_val ){
        		
        		inputField.setText(String.valueOf(new_val));
        	}
        	
        	if( change_listeners != null ) {
            for (int i=0;i<change_listeners.size();i++){
              ((ParameterChangeListener)change_listeners.get(i)).parameterChanged(this,value_is_changing_internally);
            }
          }
        }
      }
      catch (Exception e) {
        inputField.setText( String.valueOf( iMinValue ) );
        COConfigurationManager.setParameter( sParamName, iMinValue );
      }
  }

  public void
  setValue(
  	int		value )
  {
  	if ( getValue() != value ){
  		
	  	try{
	  		value_is_changing_internally	= true;
	  		
	  		inputField.setText(String.valueOf(value));
	  		
	  	}finally{
	  		
	 		value_is_changing_internally	= false;
	  	}
  	}
  }
  
  public int
  getValue()
  {
  	return(COConfigurationManager.getIntParameter(sParamName, iDefaultValue));
  }
  
  public void setLayoutData(Object layoutData) {
    inputField.setLayoutData(layoutData);
  }
  
  public Control
  getControl()
  {
  	return( inputField );
  }
}