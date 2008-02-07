/*
 * Created on Jan 30, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.plugins.net.netstatus;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.plugins.net.netstatus.swt.NetStatusPluginView;

public class 
NetStatusPlugin
	implements Plugin
{
	public static final String VIEW_ID = "aznetstatus";
	
	private LoggerChannel	logger;
	
	private StringParameter ping_target;
	
	public void
	initialize(
		final PluginInterface		plugin_interface )
	{
		String name_res = "Views.plugins." + VIEW_ID + ".title";
		
		String name = 
			plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText( name_res );
		
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		name );

		logger = plugin_interface.getLogger().getChannel( "NetStatus" );
		
		logger.setDiagnostic();
		
		BasicPluginConfigModel config = plugin_interface.getUIManager().createBasicPluginConfigModel( name_res );
		
		ping_target = config.addStringParameter2( "plugin.aznetstatus.pingtarget", "plugin.aznetstatus.pingtarget", "www.google.com" );
		
		plugin_interface.getUIManager().addUIListener(
			new UIManagerListener()
			{
				public void
				UIAttached(
					UIInstance		instance )
				{
					if ( instance instanceof UISWTInstance ){
						
						UISWTInstance swt_ui = (UISWTInstance)instance;
						
						NetStatusPluginView view = new NetStatusPluginView( NetStatusPlugin.this );

						swt_ui.addView(	UISWTInstance.VIEW_MAIN, VIEW_ID, view );
						
						//swt_ui.openMainView( VIEW_ID, view, null );
					}
				}

				public void
				UIDetached(
					UIInstance		instance )
				{
				}
			});
	}
	
	public String
	getPingTarget()
	{
		return( ping_target.getValue());
	}
	
	public void
	log(
		String		str )
	{
		logger.log( str );
	}
}
