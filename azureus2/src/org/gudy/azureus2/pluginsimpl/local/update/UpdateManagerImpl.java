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

package org.gudy.azureus2.pluginsimpl.local.update;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.update.*;

import com.aelitis.azureus.core.AzureusCore;

public class 
UpdateManagerImpl
	implements UpdateManager
{
	protected static UpdateManagerImpl		singleton;
	
	public static UpdateManager
	getSingleton(
		AzureusCore		core )
	{
		if ( singleton == null ){
			
			singleton = new UpdateManagerImpl( core );
		}
		
		return( singleton );
	}

	protected AzureusCore	azureus_core;
		
	protected List	components 	= new ArrayList();
	protected List	listeners	= new ArrayList();
	
	protected AEMonitor	this_mon 	= new AEMonitor( "UpdateManager" );

	protected
	UpdateManagerImpl(
		AzureusCore		_azureus_core )
	{
		azureus_core	= _azureus_core;
		
		UpdateInstallerImpl.checkForFailedInstalls();
		
			// cause the platform manager to register any updateable components
		
		try{
			PlatformManagerFactory.getPlatformManager();
			
		}catch( Throwable e ){
		
		}
	}
	
	public void
	registerUpdatableComponent(
		UpdatableComponent		component,
		boolean					mandatory )
	{
		try{
			this_mon.enter();
			
			components.add( new UpdatableComponentImpl( component, mandatory ));
		}finally{
			
			this_mon.exit();
		}
	}
	
	
	public UpdateCheckInstance
	createUpdateCheckInstance()
	{
		try{
			this_mon.enter();
	
			UpdatableComponentImpl[]	comps = new UpdatableComponentImpl[components.size()];
			
			components.toArray( comps );
			
			UpdateCheckInstance	res = new UpdateCheckInstanceImpl( comps );
			
			for (int i=0;i<listeners.size();i++){
				
				((UpdateManagerListener)listeners.get(i)).checkInstanceCreated( res );
			}
			
			return( res );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public UpdateCheckInstance
	createEmptyUpdateCheckInstance(
		int			type )
	{
		try{
			this_mon.enter();
	
			UpdatableComponentImpl[]	comps = new UpdatableComponentImpl[0];
			
			UpdateCheckInstance	res = new UpdateCheckInstanceImpl( type, comps );
			
			for (int i=0;i<listeners.size();i++){
				
				((UpdateManagerListener)listeners.get(i)).checkInstanceCreated( res );
			}
			
			return( res );
			
		}finally{
			
			this_mon.exit();
		}		
	}

	public UpdateInstaller
	createInstaller()
		
		throws UpdateException
	{
		return( new UpdateInstallerImpl());
	}
	
	
	public void
	restart()
	
		throws UpdateException
	{
		applyUpdates( true );
	}
	
	public void
	applyUpdates(
		boolean	restart_after )
	
		throws UpdateException
	{
		try{
			azureus_core.requestRestart( !restart_after );
			
		}catch( Throwable e ){
			
			throw( new UpdateException( "UpdateManager:applyUpdates fails", e ));
		}
	}
	
	public void
	addListener(
		UpdateManagerListener	l )
	{
		listeners.add(l);
	}
	
	public void
	removeListener(
		UpdateManagerListener	l )
	{
		listeners.remove(l);
	}
}
