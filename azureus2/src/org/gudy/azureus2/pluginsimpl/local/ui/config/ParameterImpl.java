/*
 * File    : GenericParameter.java
 * Created : Nov 21, 2003
 * By      : epall
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
 
package org.gudy.azureus2.pluginsimpl.local.ui.config;

import java.util.*;

import org.gudy.azureus2.plugins.ui.config.EnablerParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.PluginConfig;

/**
 * @author epall
 *
 */
public class 
ParameterImpl 
	implements EnablerParameter
{
	protected 	PluginConfig	config;
	private 	String 			key;
	private 	String 			label;
	
	private List toDisable	= new ArrayList();
	private List toEnable	= new ArrayList();
	  
	public 
	ParameterImpl(
		PluginConfig	_config,
		String 			_key, 
		String 			_label )
	{
		config	= _config;
		key		= _key;
		label 	= _label;
	}
	/**
	 * @return Returns the key.
	 */
	public String getKey()
	{
		return key;
	}

	/**
	 * @return Returns the label.
	 */
	public String getLabel()
	{
		return label;
	}
	 public void addDisabledOnSelection(Parameter parameter) {
	    toDisable.add(parameter);
	  }
	  
	  public void addEnabledOnSelection(Parameter parameter) {    
	    toEnable.add(parameter);
	  }
	  
	  public List getDisabledOnSelectionParameters() {
	    return toDisable;
	  }
	  
	  public List getEnabledOnSelectionParameters() {
	    return toEnable;
	  }
}
