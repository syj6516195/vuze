/*
 * Created on 10 juil. 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package org.gudy.azureus2.ui.swt.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.config.*;

/**
 * @author Olivier
 * 
 */
public class IntListParameter extends Parameter {

  Combo list;

  public IntListParameter(
                          Composite composite,
                          final String name,
                          final String labels[],
                          final int values[]) {
    this(composite, name, COConfigurationManager.getIntParameter(name), labels, values);
  }

  public IntListParameter(Composite composite, final String name,
			int defaultValue, final String labels[], final int values[]) {
		super(name);
      if(labels.length != values.length)
        return;
      int value = COConfigurationManager.getIntParameter(name,defaultValue);
      int index = findIndex(value,values);
      list = new Combo(composite,SWT.SINGLE | SWT.READ_ONLY);
      for(int i = 0 ; i < labels.length  ;i++) {
        list.add(labels[i]);
      }
      
      list.select(index);
      
      list.addListener(SWT.Selection, new Listener() {
           public void handleEvent(Event e) {
        	int	selected_value = values[list.getSelectionIndex()];
			COConfigurationManager.setParameter(name, selected_value);
           }
         });
      
    }
    
  private int findIndex(int value,int values[]) {
    for(int i = 0 ; i < values.length ;i++) {
      if(values[i] == value)
        return i;
    }
    return 0;
  }
  
  
  public void setLayoutData(Object layoutData) {
    list.setLayoutData(layoutData);
   }
   
  public Control getControl() {
    return list;
  }
}
