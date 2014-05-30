/*
 * Created on May 29, 2014
 * Created by Paul Gardner
 * 
 * Copyright 2014 Azureus Software, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.dht.transport;

import java.util.Map;

public interface 
DHTTransportAlternativeContact 
{
	public int
	getNetworkType();
	
	public int
	getVersion();
	
		/** 
		 * A good-enough ID to spot duplicates - must be equal to Arrays.hashCode( BEncode( getProperties()));
		 * @return
		 */
	
	public int
	getID();
	
		/**
		 * A value that can be compared to others to get an ordering, but not related to current real/mono time
		 * and can be negative
		 * @return
		 */
	
	public int
	getLastAlive();
	
		/**
		 * Gets the contact's age since last known to be alive in seconds
		 * @return
		 */
	
	public int
	getAge();
	
	public Map<String,Object>
	getProperties();
}
