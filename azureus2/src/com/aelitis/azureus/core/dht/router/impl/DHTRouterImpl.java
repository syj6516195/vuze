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

package com.aelitis.azureus.core.dht.router.impl;

import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.logging.LoggerChannel;

import com.aelitis.azureus.core.dht.impl.DHTLog;
import com.aelitis.azureus.core.dht.router.*;

/**
 * @author parg
 *
 */

public class 
DHTRouterImpl
	implements DHTRouter
{
	private static final int	SMALLEST_SUBTREE_MAX_EXCESS	= 1024;
	
	private int		K;
	private int		B;
	private int		max_rep_per_node;
	
	private LoggerChannel	logger;
	
	private int		smallest_subtree_max;
	
	private DHTRouterAdapter		adapter;
	
	private byte[]					router_node_id;
	
	private DHTRouterNodeImpl		root;
	private DHTRouterNodeImpl		smallest_subtree;
	
	private static long				random_seed	= SystemTime.getCurrentTime();
	private Random					random;
	
	private List					outstanding_pings	= new ArrayList();
	private List					outstanding_adds	= new ArrayList();
	
	private DHTRouterStatsImpl		stats	= new DHTRouterStatsImpl( this );
	
	
	public
	DHTRouterImpl(
		int										_K,
		int										_B,
		int										_max_rep_per_node,
		byte[]									_router_node_id,
		DHTRouterContactAttachment				_attachment,
		LoggerChannel							_logger )
	{
		synchronized( DHTRouterImpl.class ){
			
			random = new Random( random_seed++);
		}
		
		K					= _K;
		B					= _B;
		max_rep_per_node	= _max_rep_per_node;
		logger				= _logger;
		
		
		smallest_subtree_max	= 1;
		
		for (int i=0;i<B;i++){
			
			smallest_subtree_max	*= 2;
		}
		
		smallest_subtree_max	+= SMALLEST_SUBTREE_MAX_EXCESS;
		
		router_node_id	= _router_node_id;
		
		List	buckets = new ArrayList();
		
		DHTRouterContactImpl local_contact = new DHTRouterContactImpl( router_node_id, _attachment, true );
		
		buckets.add( local_contact );
		
		root	= new DHTRouterNodeImpl( this, 0, true, buckets );
	}
	
	public DHTRouterStats
	getStats()
	{
		return( stats );
	}
	
	public int
	getK()
	{
		return( K );
	}
	
	
	public byte[]
	getID()
	{
		return( router_node_id );
	}
	
	public boolean
	isID(
		byte[]	id )
	{
		return( Arrays.equals( id, router_node_id ));
	}
	
	public void
	setAdapter(
		DHTRouterAdapter	_adapter )
	{
		adapter	= _adapter;
	}
	
	public DHTRouterContact
	contactKnown(
		byte[]						node_id,
		DHTRouterContactAttachment	attachment )
	{
		return( addContact( node_id, attachment, false ));
	}
	
	public DHTRouterContact
	contactAlive(
		byte[]						node_id,
		DHTRouterContactAttachment	attachment )
	{
		return( addContact( node_id, attachment, true ));
	}
	
	// all incoming node actions come through either contactDead or addContact
	// A side effect of processing
	// the node is that either a ping can be requested (if a replacement node
	// is available and the router wants to check the liveness of an existing node)
	// or a new node can be added (either directly to a node or indirectly via 
	// a replacement becoming "live"
	// To avoid requesting these actions while synchronized these are recorded
	// in lists and then kicked off separately here


	public DHTRouterContact
	contactDead(
		byte[]						node_id,
		DHTRouterContactAttachment	attachment )
	{
		try{
			synchronized( this ){

				Object[]	res = findContactSupport( node_id );
				
				DHTRouterNodeImpl		node	= (DHTRouterNodeImpl)res[0];
				DHTRouterContactImpl	contact = (DHTRouterContactImpl)res[1];
				
				if ( contact != null ){
				
					node.dead( contact );
				}
				
				return( contact );
			}
		}finally{

			dispatchPings();
			
			dispatchNodeAdds();
		}
	}
	
	public DHTRouterContact
	addContact(
		byte[]						node_id,
		DHTRouterContactAttachment	attachment,
		boolean						known_to_be_alive )
	{	
		try{
			synchronized( this ){
			
				return( addContactSupport( node_id, attachment, known_to_be_alive ));
			}
		}finally{
			
			dispatchPings();
			
			dispatchNodeAdds();
		}
	}
	
	protected DHTRouterContact
	addContactSupport(
		byte[]						node_id,
		DHTRouterContactAttachment	attachment,
		boolean						known_to_be_alive )
	{		
		DHTRouterNodeImpl	current_node = root;
			
		boolean	part_of_smallest_subtree	= false;
		
		for (int i=0;i<node_id.length;i++){
			
			byte	b = node_id[i];
			
			int	j = 7;
			
			while( j >= 0 ){
					
				if ( current_node == smallest_subtree ){
					
					part_of_smallest_subtree	= true;
				}
				
				boolean	bit = ((b>>j)&0x01)==1?true:false;
								
				DHTRouterNodeImpl	next_node;
				
				if ( bit ){
					
					next_node = current_node.getLeft();
					
				}else{
					
					next_node = current_node.getRight();
				}
				
				if ( next_node == null ){
		
					DHTRouterContact	existing_contact = current_node.updateExistingNode( node_id, attachment, known_to_be_alive );
					
					if ( existing_contact != null ){
						
						return( existing_contact );
					}

					List	buckets = current_node.getBuckets();

					if ( buckets.size() == K ){
						
							// split if either
							// 1) this list contains router_node_id or
							// 2) depth % B is not 0
							// 3) this is part of the smallest subtree
						
						boolean	contains_router_node_id = current_node.containsRouterNodeID();
						int		depth					= current_node.getDepth();
						
						boolean	too_deep_to_split = depth % B == 0;	// note this will be true for 0 but other
																	// conditions will allow the split
						
						if ( 	contains_router_node_id ||
								(!too_deep_to_split)	||
								part_of_smallest_subtree ){
							
								// the smallest-subtree bit is to ensure that we remember all of
								// our closest neighbours as ultimately they are the ones responsible
								// for returning our identity to queries (due to binary choppery in
								// general the query will home in on our neighbours before
								// hitting us. It is therefore important that we keep ourselves live
								// in their tree by refreshing. If we blindly chopped at K entries
								// (down to B levels) then a highly unbalanced tree would result in
								// us dropping some of them and therefore not refreshing them and
								// therefore dropping out of their trees. There are also other benefits
								// of maintaining this tree regarding stored value refresh
							
								// Note that it is rare for such an unbalanced tree. 
								// However, a possible DOS here would be for a rogue node to 
								// deliberately try and create such a tree with a large number
								// of entries.
								
							if ( 	part_of_smallest_subtree &&
									too_deep_to_split &&
									( !contains_router_node_id ) &&
									getContactCount( smallest_subtree ) > smallest_subtree_max ){
								
								Debug.out( "DHTRouter: smallest subtree max size violation" );
								
								return( null );	
							}
							
								// split!!!!
							
							List	left_buckets 	= new ArrayList();
							List	right_buckets 	= new ArrayList();
							
							for (int k=0;k<buckets.size();k++){
								
								DHTRouterContactImpl	contact = (DHTRouterContactImpl)buckets.get(k);
								
								byte[]	bucket_id = contact.getID();
								
								if (((bucket_id[depth/8]>>(7-(depth%8)))&0x01 ) == 0 ){
									
									right_buckets.add( contact );
									
								}else{
									
									left_buckets.add( contact );
								}
							}
				
							boolean	right_contains_rid = false;
							boolean left_contains_rid = false;
							
							if ( contains_router_node_id ){
								
								right_contains_rid = 
										((router_node_id[depth/8]>>(7-(depth%8)))&0x01 ) == 0;
							
								left_contains_rid	= !right_contains_rid;
							}
							
							DHTRouterNodeImpl	new_left 	= new DHTRouterNodeImpl( this, depth+1, left_contains_rid, left_buckets );
							DHTRouterNodeImpl	new_right 	= new DHTRouterNodeImpl( this, depth+1, right_contains_rid, right_buckets );
							
							current_node.split( new_left, new_right );
							
							if ( right_contains_rid ){
							
									// we've created a new smallest subtree
									// TODO: tidy up old smallest subtree - remember to factor in B...
								
								smallest_subtree = new_left;
								
							}else if ( left_contains_rid ){
								
									// TODO: tidy up old smallest subtree - remember to factor in B...
								
								smallest_subtree = new_right;
							}
							
								// not complete, retry addition 
							
						}else{
								
								// split not appropriate, add as a replacemnet
							
							DHTRouterContactImpl new_contact = new DHTRouterContactImpl( node_id, attachment, known_to_be_alive );
							
							return( current_node.addReplacement( new_contact, max_rep_per_node ));					
						}
					}else{
						
							// bucket space free, just add it
		
						DHTRouterContactImpl new_contact = new DHTRouterContactImpl( node_id, attachment, known_to_be_alive );
							
						current_node.addNode( new_contact );	// complete - added to bucket
						
						return( new_contact );
					}						
				}else{
						
					current_node = next_node;
				
					j--;				
				}
			}
		}
		
		Debug.out( "DHTRouter inconsistency" );
		
		return( null );
	}
	
	public synchronized List
	findClosestContacts(
		byte[]	node_id )
	{
			// find the K-ish closest nodes - consider all buckets, not just the closest

		List res = new ArrayList();
				
		findClosestContacts( node_id, 0, root, res );
		
		return( res );
	}
		
	protected void
	findClosestContacts(
		byte[]					node_id,
		int						depth,
		DHTRouterNodeImpl		current_node,
		List					res )
	{
		List	buckets = current_node.getBuckets();
		
		if ( buckets != null ){
			
			for (int i=0;i<buckets.size();i++){
								
				res.add( buckets.get(i));
			}			
		}else{
		
			boolean bit = ((node_id[depth/8]>>(7-(depth%8)))&0x01 ) == 1;
					
			DHTRouterNodeImpl	best_node;
			DHTRouterNodeImpl	worse_node;
					
			if ( bit ){
						
				best_node = current_node.getLeft();
				
				worse_node = current_node.getRight();
			}else{
						
				best_node = current_node.getRight();
				
				worse_node = current_node.getLeft();
			}
	
			findClosestContacts( node_id, depth+1, best_node, res  );
			
			if ( res.size() < K ){
				
				findClosestContacts( node_id, depth+1, worse_node, res );
			}
		}
	}
	
	public synchronized DHTRouterContact
	findContact(
		byte[]		node_id )
	{
		Object[]	res = findContactSupport( node_id );
				
		return((DHTRouterContact)res[1]);
	}
	
	protected synchronized DHTRouterNodeImpl
	findNode(
		byte[]	node_id )
	{
		Object[]	res = findContactSupport( node_id );
				
		return((DHTRouterNodeImpl)res[0]);	
	}
	
	protected synchronized Object[]
	findContactSupport(
		byte[]		node_id )
	{
		DHTRouterNodeImpl	current_node	= root;
		
		for (int i=0;i<node_id.length;i++){
			
			if ( current_node.getBuckets() != null ){
			
				break;
			}

			byte	b = node_id[i];
			
			int	j = 7;
			
			while( j >= 0 ){
					
				boolean	bit = ((b>>j)&0x01)==1?true:false;
				
				if ( current_node.getBuckets() != null ){
					
					break;
				}
								
				if ( bit ){
					
					current_node = current_node.getLeft();
					
				}else{
					
					current_node = current_node.getRight();
				}
				
				j--;
			}
		}
		
		List	buckets = current_node.getBuckets();
		
		for (int k=0;k<buckets.size();k++){
			
			DHTRouterContactImpl	contact = (DHTRouterContactImpl)buckets.get(k);
			
			if ( Arrays.equals(node_id, contact.getID())){

				return( new Object[]{ current_node, contact });
			}
		}
		
		return( new Object[]{ current_node, null });
	}
	
	public long
	getNodeCount()
	{
		return( getNodeCount( root ));
	}
	
	protected long
	getNodeCount(
		DHTRouterNodeImpl	node )
	{
		if ( node.getBuckets() != null ){
			
			return( 1 );
			
		}else{
			
			return( 1 + getNodeCount( node.getLeft())) + getNodeCount( node.getRight());
		}
	}
	
	protected long
	getContactCount()
	{
		return( getContactCount( root ));
	}
	
	protected long
	getContactCount(
		DHTRouterNodeImpl	node )
	{
		if ( node.getBuckets() != null ){
			
			return( node.getBuckets().size());
			
		}else{
			
			return( getContactCount( node.getLeft())) + getContactCount( node.getRight());
		}
	}
	
	public List
	findBestContacts(
		int		max )
	{
		Set	set = 
			new TreeSet(
					new Comparator()
					{
						public int
						compare(
							Object	o1,
							Object	o2 )
						{
							DHTRouterContactImpl	c1 = (DHTRouterContactImpl)o1;
							DHTRouterContactImpl	c2 = (DHTRouterContactImpl)o2;
							
							return((int)( c2.getTimeAlive() - c1.getTimeAlive()));
						}
					});
		
		
		findBestContacts( set, root );
		
		List	result = new ArrayList( max );
	
		Iterator	it = set.iterator();
		
		while( it.hasNext() && (max <= 0 || result.size() < max )){
			
			result.add( it.next());
		}
		
		return( result );
	}
	
	protected void
	findBestContacts(
		Set					set,
		DHTRouterNodeImpl	node )
	{
		List	buckets = node.getBuckets();
		
		if ( buckets == null ){
			
			findBestContacts( set, node.getLeft());
			
			findBestContacts( set, node.getRight());
		}else{
			
			for (int i=0;i<buckets.size();i++){
				
				DHTRouterContactImpl	contact = (DHTRouterContactImpl)buckets.get(i);
								
				set.add( contact );
			}
		}
	}
	
	public void
	seed()
	{
			// refresh all buckets apart from closest neighbour
		
		byte[]	path = new byte[router_node_id.length];
		
		List	ids = new ArrayList();
		
		synchronized( this ){
			
			refreshNodes( ids, root, path, true, 0 );
		}
		
		for (int i=0;i<ids.size();i++){
			
			requestLookup((byte[])ids.get(i));
		}
	}
	
	protected void
	refreshNodes(
		List				nodes_to_refresh,
		DHTRouterNodeImpl	node,
		byte[]				path,
		boolean				seeding,
		long				max_permitted_idle )	// 0 -> don't check
	{
			// when seeding we don't do the smallest subtree
		
		if ( seeding && node == smallest_subtree ){
			
			return;
		}
		
		if ( max_permitted_idle != 0 ){
			
			if ( node.getTimeSinceLastLookup() <= max_permitted_idle ){
				
				return;
			}
		}
		
		if ( node.getBuckets() != null ){
		
				// and we also don't refresh the bucket containing the router id when seeding
			
			if ( seeding && node.containsRouterNodeID()){
				
				return;
			}
			
			refreshNode( nodes_to_refresh, node, path );		
		}
		
			// synchronous refresh may result in this bucket being split
			// so we retest here to refresh sub-buckets as required
		
		if ( node.getBuckets() == null ){
			
			int	depth = node.getDepth();
			
			byte	mask = (byte)( 0x01<<(7-(depth%8)));
			
			path[depth/8] = (byte)( path[depth/8] | mask );
			
			refreshNodes( nodes_to_refresh, node.getLeft(), path,seeding, max_permitted_idle  );
			
			path[depth/8] = (byte)( path[depth/8] & ~mask );
		
			refreshNodes( nodes_to_refresh, node.getRight(), path,seeding, max_permitted_idle  );
		}
	}
	
	protected void
	refreshNode(
		List				nodes_to_refresh,
		DHTRouterNodeImpl	node,
		byte[]				path )
	{
			// pick a random id in the node's range.
		
		byte[]	id = new byte[router_node_id.length];
		
		random.nextBytes( id );
		
		int	depth = node.getDepth();
		
		for (int i=0;i<depth;i++){
			
			byte	mask = (byte)( 0x01<<(7-(i%8)));
			
			boolean bit = ((path[i/8]>>(7-(i%8)))&0x01 ) == 1;

			if ( bit ){
				
				id[i/8] = (byte)( id[i/8] | mask );

			}else{
				
				id[i/8] = (byte)( id[i/8] & ~mask );	
			}
		}
		
		nodes_to_refresh.add( id );
	}
	
	protected DHTRouterNodeImpl
	getSmallestSubtree()
	{
		return( smallest_subtree );
	}
	
	public void
	recordLookup(
		byte[]	node_id )
	{
		findNode( node_id ).setLastLookupTime();
	}
	
	public void
	refreshIdleLeaves(
		long	idle_max)
	{
		// while we are synchronously refreshing the smallest subtree the tree can mutate underneath us
		// as new contacts are discovered. We NEVER merge things back together
		
		byte[]	path = new byte[router_node_id.length];
		
		List	ids = new ArrayList();
		
		synchronized( this ){
			
			refreshNodes( ids, root, path, false, idle_max );
		}
		
		for (int i=0;i<ids.size();i++){
			
			requestLookup((byte[])ids.get(i) );
		}
	}
	
	protected void
	requestPing(
		DHTRouterContactImpl	contact )
	{
			// make sure we don't do the ping when synchronized
		
		DHTLog.log( "DHTRouter: requestPing:" + DHTLog.getString( contact.getID()));
		
		synchronized( this ){
			
			if ( !outstanding_pings.contains( contact )){
			
				outstanding_pings.add( contact );
			}
		}
	}
	
	protected void
	dispatchPings()
	{
		List	pings;
		
		synchronized( this ){
		
			pings	= outstanding_pings;
			
			outstanding_pings = new ArrayList();
		}
		
		for (int i=0;i<pings.size();i++){
			
			adapter.requestPing((DHTRouterContactImpl)pings.get(i));
		}
	}
	
	protected void
	requestNodeAdd(
		DHTRouterContactImpl	contact )
	{
			// make sure we don't do the ping when synchronized
		
		DHTLog.log( "DHTRouter: requestNodeAdd:" + DHTLog.getString( contact.getID()));
		
		synchronized( this ){
			
			if ( !outstanding_adds.contains( contact )){
			
				outstanding_adds.add( contact );
			}
		}
	}
	
	protected void
	dispatchNodeAdds()
	{
		List	adds;
		
		synchronized( this ){
		
			adds	= outstanding_adds;
			
			outstanding_adds = new ArrayList();
		}
		
		for (int i=0;i<adds.size();i++){
			
			adapter.requestAdd((DHTRouterContactImpl)adds.get(i));
		}
	}

	
	protected void
	requestLookup(
		byte[]		id )
	{
		DHTLog.log( "DHTRouter: requestLookup:" + DHTLog.getString( id ));
		
		adapter.requestLookup( id );
	}
	
	protected void
	getStatsSupport(
		long[]				stats_array,
		DHTRouterNodeImpl	node )
	{
		stats_array[DHTRouterStats.ST_NODES]++;
		
		List	buckets = node.getBuckets();
		
		if ( buckets == null ){
			
			getStatsSupport( stats_array, node.getLeft());
			
			getStatsSupport( stats_array, node.getRight());
			
		}else{
			
			stats_array[DHTRouterStats.ST_LEAVES]++;
			
			stats_array[DHTRouterStats.ST_CONTACTS] += buckets.size();
			
			for (int i=0;i<buckets.size();i++){
				
				DHTRouterContactImpl	contact = (DHTRouterContactImpl)buckets.get(i);
				
				if ( contact.getFirstFailTime() > 0 ){
					
					stats_array[DHTRouterStats.ST_CONTACTS_DEAD]++;
					
				}else if ( contact.hasBeenAlive()){
					
					stats_array[DHTRouterStats.ST_CONTACTS_LIVE]++;
					
				}else{
					
					stats_array[DHTRouterStats.ST_CONTACTS_UNKNOWN]++;
				}
			}
			
			List	rep = node.getReplacements();
			
			if ( rep != null ){
				
				stats_array[DHTRouterStats.ST_REPLACEMENTS] += rep.size();
			}
		}
	}
	
	protected synchronized long[]
	getStatsSupport()
	{
		 /* number of nodes
		 * number of leaves
		 * number of contacts
		 * number of replacements
		 * number of live contacts
		 * number of unknown contacts
		 * number of dying contacts
		 */
		
		long[]	res = new long[7];
		
		getStatsSupport( res, root );
		
		return( res );
	}
	
	protected void
	log(
		String	str )
	{
		logger.log( str );
	}
	
	public synchronized void
	print()
	{
		DHTLog.log( "DHT: " + DHTLog.getString(router_node_id) + ", node count = " + getNodeCount()+ ", contacts =" + getContactCount());
		
		root.print( "", "" );
	}
}
