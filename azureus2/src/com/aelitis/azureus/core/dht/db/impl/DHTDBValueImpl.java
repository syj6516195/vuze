/*
 * Created on 18-Jan-2005
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

package com.aelitis.azureus.core.dht.db.impl;

import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.dht.impl.DHTLog;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;

/**
 * @author parg
 *
 */

public class 
DHTDBValueImpl
	implements DHTDBValue
{
	private long				creation_time;
	private byte[]				value;
	private DHTTransportContact	originator;
	private DHTTransportContact	sender;
	private int					distance;
	private int					flags;
	
	private long				store_time;
	
		/**
		 * constructor for the originator of values only
		 * @param _creation_time
		 * @param _value
		 * @param _originator
		 * @param _sender
		 * @param _distance
		 * @param _flags
		 */
	
	protected
	DHTDBValueImpl(
		long				_creation_time,
		byte[]				_value,
		DHTTransportContact	_originator,
		DHTTransportContact	_sender,
		int					_distance,
		int					_flags )
	{
		creation_time	= _creation_time;
		value			= _value;
		originator		= _originator;
		sender			= _sender;
		distance		= _distance<0?0:_distance;
		flags			= _flags;
		
		reset();
	}

		/**
		 * Constructor used to generate values for relaying to other contacts
		 * or receiving a value from another contact - adjusts the cache offset 
		 * and sender as required
		 * Originator, creation time, flags and value are fixed.
		 * @param _sender
		 * @param _other
		 * @param _cache_offset
		 */
	
	protected 
	DHTDBValueImpl(
		DHTTransportContact	_sender,
		DHTTransportValue	_other,
		int					_cache_offset )
	{
		this( 	_other.getCreationTime(), 
				_other.getValue(),
				_other.getOriginator(),
				_sender,
				_other.getCacheDistance() + _cache_offset,
				_other.getFlags());
	}
	
	protected void
	reset()
	{
		store_time	= SystemTime.getCurrentTime();
		
			// make sure someone hasn't sent us a stupid creation time
		
		if ( creation_time > store_time ){
			
			creation_time	= store_time;
		}	
	}
	
	public long
	getCreationTime()
	{
		return( creation_time );
	}
	
	protected void
	setCreationTime()
	{
		creation_time = SystemTime.getCurrentTime();
	}
	
	protected void
	setStoreTime(
		long	l )
	{
		store_time	= l;
	}
	
	protected long
	getStoreTime()
	{
		return( store_time );
	}
	
	public int 
	getCacheDistance() 
	{
		return( distance );
	}
		
	public byte[]
	getValue()
	{
		return( value );
	}
	
	public DHTTransportContact
	getOriginator()
	{
		return( originator);
	}
	
	public DHTTransportContact
	getSender()
	{
		return( sender );
	}
	public int
	getFlags()
	{
		return( flags );
	}
	
	public void
	setOriginator(
		DHTTransportContact	_originator )
	{
		originator	= _originator;
	}
	
	public DHTDBValue
	getValueForRelay(
		DHTTransportContact	_sender )
	{
		return( new DHTDBValueImpl( _sender, this, -1 ));
	}
	
	public String
	getString()
	{
		long	now = SystemTime.getCurrentTime();
		
		return( DHTLog.getString( value ) + " - " + new String(value) + "{ca=" + (now - creation_time ) + ",sa=" + (now-store_time)+"}" );
	}
}
