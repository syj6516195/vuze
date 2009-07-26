/*
 * Created on Jul 24, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.devices.impl;

import java.io.IOException;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;

public class 
DeviceTivo
	extends DeviceMediaRendererImpl
{
	protected
	DeviceTivo(
		DeviceManagerImpl	_manager,
		String				_uid,
		String				_classification )
	{
		super( _manager, _uid, _classification, false );
		
		setName( "TiVo" );
	}

	protected
	DeviceTivo(
		DeviceManagerImpl	_manager,
		Map					_map )
	
		throws IOException
	{
		super( _manager, _map );
	}
	
	protected boolean
	updateFrom(
		DeviceImpl		_other )
	{
		if ( !super.updateFrom( _other )){
			
			return( false );
		}
		
		if ( !( _other instanceof DeviceTivo )){
			
			Debug.out( "Inconsistent" );
			
			return( false );
		}
		
		DeviceTivo other = (DeviceTivo)_other;
		
		return( true );
	}
	
	protected void
	initialise()
	{
		super.initialise();
	}
	
	public boolean 
	canAssociate()
	{
		return( true );
	}
}