/*
 * Created on 11-Jan-2005
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

package com.aelitis.azureus.core.dht.router;

/**
 * @author parg
 *
 */

import java.util.*;

public interface 
DHTRouter 
{
	public int
	getK();
	
	public byte[]
	getID();
	
	public boolean
	isID(
		byte[]	node_id );
	
	public void
	setAdapter(
		DHTRouterAdapter	_adapter );
	
		/**
		 * Tells the router to perform its "start of day" functions required to integrate
		 * it into the DHT (search for itself, refresh buckets)
		 */
	
	public void
	seed();
	
		/**
		 * Adds a contact to the router. The contact is not known to be alive (e.g.
		 * we've been returned the contact by someone but we've not either got a reply
		 * from it, nor has it invoked us.
		 * @param node_id
		 * @param attachment
		 * @return
		 */
	
	public DHTRouterContact
	contactKnown(
		byte[]	node_id,
		Object	attachment );
	
		/**
		 * Adds a contact to the router and marks it as "known to be alive"
		 * @param node_id
		 * @param attachment
		 * @return
		 */
	
	public DHTRouterContact
	contactAlive(
		byte[]	node_id,
		Object	attachment );

		/**
		 * Informs the router that an attempt to interact with the contact failed 
		 * @param node_id
		 * @param attachment
		 * @return
		 */
	
	public DHTRouterContact
	contactDead(
		byte[]	node_id,
		Object	attachment );
	
	public DHTRouterContact
	findContact(
		byte[]	node_id );	

		/**
		 * Returns K or a few more closest contacts, unordered
		 */
	
	public List
	findClosestContacts(
		byte[]	node_id );
		
	public void
	recordLookup(
		byte[]	node_id );
	
	public void
	refreshIdleLeaves(
		long	idle_max );
	
	public DHTRouterStats
	getStats();
	
	public void
	print();
}
