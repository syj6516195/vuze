/*
 * Created on 12-Jan-2005
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

package com.aelitis.azureus.core.dht.control;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;

import com.aelitis.azureus.core.dht.DHTOperationListener;
import com.aelitis.azureus.core.dht.router.DHTRouter;
import com.aelitis.azureus.core.dht.transport.*;

/**
 * @author parg
 *
 */

public interface 
DHTControl 
{
	public static final int		K_DEFAULT								= 20;
	public static final int		B_DEFAULT								= 4;
	public static final int		MAX_REP_PER_NODE_DEFAULT				= 5;
	public static final int		SEARCH_CONCURRENCY_DEFAULT				= 5;
	public static final int		LOOKUP_CONCURRENCY_DEFAULT				= 10;
	public static final int		CACHE_AT_CLOSEST_N_DEFAULT				= 1;
	public static final int		ORIGINAL_REPUBLISH_INTERVAL_DEFAULT		= 8*60*60*1000;
	public static final int		CACHE_REPUBLISH_INTERVAL_DEFAULT		=   30*60*1000; 
	public static final int		MAX_VALUES_STORED_DEFAULT				= 10000;
	
	public void
	seed();
	
	public void
	put(
		byte[]		key,
		byte[]		value );
	
	public void
	put(
		byte[]			key,
		byte[]			value,
		DHTOperationListener	listener );
	
	public byte[]
	get(
		byte[]		key,
		long		timeout );
	
	public void
	get(
		byte[]			key,
		int				max_values,
		long			timeout,
		DHTOperationListener	listener );
	
	public byte[]
	remove(
		byte[]		key );
	
	public byte[]
	remove(
		byte[]					key,
		DHTOperationListener	listener );
	
	public DHTTransport
	getTransport();
	
	public DHTRouter
	getRouter();
	
	public void
	exportState(
		DataOutputStream	os,
		int				max )
		
		throws IOException;
		
	public void
	importState(
		DataInputStream		is )
		
		throws IOException;
	
		// support methods for DB
	
	public List
	getClosestKContactsList(
		byte[]	id );
	
	public void
	put(
		byte[]				key,
		DHTTransportValue	value,
		long				timeout );
	
	public void
	put(
		byte[]				key,
		DHTTransportValue	value,
		List				closest );
	
	public void
	print();
}
