/*
 * Created on 07-May-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.update;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;
	
import org.gudy.azureus2.plugins.*;

	// Note this is unloadable because it shouldn't be loaded in the first place
	// Hence, on upgrade, a restart isn't required

public class 
UpdaterUpdateChecker
	implements UnloadablePlugin
{
	public static void
	checkPlugin()
	{
		try{
				// this is a bootstrap to ensure that the updater plugin exists
			
		    File user_plugin_dir = FileUtil.getUserFile("plugins");

		    File user_updater_plugin = new File( user_plugin_dir, "azupdater" );
	
		    File user_updater_props = new File( user_updater_plugin, "plugin.properties" );
		    
		    if ( user_updater_props.exists()){
		    	
		    	return;
		    }
		    
		    File shared_plugin_dir = FileUtil.getApplicationFile("plugins");

		    File shared_updater_plugin = new File( shared_plugin_dir, "azupdater" );
		    
		    shared_updater_plugin.mkdirs();
		    
		    File	props = new File( shared_updater_plugin, "plugin.properties" );
		    
		    if ( props.exists()){
		    		    	
		    	return;
		    }
		    
		    PrintWriter	pw = null;
		    
		    try{
		    	pw = new PrintWriter( new FileWriter( props ));
		    	
		    	pw.println( "plugin.class=org.gudy.azureus2.update.UpdaterUpdateChecker;org.gudy.azureus2.update.UpdaterPatcher" );
		    	pw.println( "plugin.name=Azureus Update Support;Azureus Updater Support Patcher" );
		    	
		    }finally{
		    	
		    	if ( pw != null ){
		    		
		    		pw.close();
		    	}
		    }
		    
		    if ( !props.exists()){
		    	
		    	throw( new Exception( "Failed to write '" + props.toString() + "'"));
		    }
		    
		}catch( Throwable e ){

			LGLogger.logUnrepeatableAlert( "azupdater plugin: initialisation error", e );
		}
	}
	
	public void
	initialize(
		PluginInterface		pi )
	{
		Properties	props = pi.getPluginProperties();
		
		props.setProperty( "plugin.mandatory", "true" );
		
		if ( pi.getPluginVersion() == null ){
			
			props.setProperty( "plugin.version", "1.0" );
		}
		
		props.setProperty( "plugin.id", "azupdater" );
	}
	
	public void
	unload()
	{
	}
}