/*
 * Created on 21-Jan-2005
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

package com.aelitis.azureus.core.dht.transport.udp.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;


/**
 * @author parg
 *
 */

public class 
DHTUDPPacketRequestStore 
	extends DHTUDPPacketRequest
{
	public static final int	MAX_KEYS_PER_PACKET		= 255; // 1 byte DHTUDPPacket.PACKET_MAX_BYTES / 20;
	public static final int	MAX_VALUES_PER_KEY		= 255; // 1 byte DHTUDPPacket.PACKET_MAX_BYTES / DHTUDPUtils.DHTTRANSPORTVALUE_SIZE_WITHOUT_VALUE;
	
	private byte[][]				keys;
	private	DHTTransportValue[][]	value_sets;
	
	public
	DHTUDPPacketRequestStore(
		long						_connection_id,
		DHTTransportUDPContactImpl	_contact )
	{
		super( DHTUDPPacket.ACT_REQUEST_STORE, _connection_id, _contact );
	}

	protected
	DHTUDPPacketRequestStore(
		DHTTransportUDPImpl		transport,
		DataInputStream			is,
		long					con_id,
		int						trans_id )
	
		throws IOException
	{
		super( is,  DHTUDPPacket.ACT_REQUEST_STORE, con_id, trans_id );
		
		keys		= DHTUDPUtils.deserialiseByteArrayArray( is, MAX_KEYS_PER_PACKET );
		
			// times receieved are adjusted by + skew
				
		value_sets 	= DHTUDPUtils.deserialiseTransportValuesArray( transport, is, getClockSkew(), MAX_VALUES_PER_KEY );
	}
	
	public void
	serialise(
		DataOutputStream	os )
	
		throws IOException
	{
		super.serialise(os);
		
		DHTUDPUtils.serialiseByteArrayArray( os, keys, MAX_KEYS_PER_PACKET );
		
		try{
			DHTUDPUtils.serialiseTransportValuesArray( os, value_sets, 0, MAX_VALUES_PER_KEY );
			
		}catch( DHTTransportException e ){
			
			throw( new IOException( e.getMessage()));
		}
	}

	protected void
	setValueSets(
		DHTTransportValue[][]	_values )
	{
		value_sets	= _values;
	}
	
	protected DHTTransportValue[][]
	getValueSets()
	{
		return( value_sets );
	}
	
	protected void
	setKeys(
		byte[][]		_key )
	{
		keys	= _key;
	}
	
	protected byte[][]
	getKeys()
	{
		return( keys );
	}
	
	public String
	getString()
	{
		return( super.getString());
	}
}