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

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

import org.gudy.azureus2.core3.ipfilter.IpFilter;
import org.gudy.azureus2.core3.ipfilter.IpFilterManagerFactory;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.logging.LoggerChannel;

import com.aelitis.azureus.core.dht.impl.DHTLog;
import com.aelitis.azureus.core.dht.transport.*;
import com.aelitis.azureus.core.dht.transport.udp.*;
import com.aelitis.azureus.core.dht.transport.util.DHTTransportRequestCounter;
import com.aelitis.net.udp.*;

/**
 * @author parg
 *
 */

public class 
DHTTransportUDPImpl 
	implements DHTTransportUDP, PRUDPRequestHandler
{
	public static boolean TEST_EXTERNAL_IP	= false;
		
	private static String	external_address;
	
	private String				ip_override;
	private int					port;
	private int					max_fails_for_live;
	private int					max_fails_for_unknown;
	private long				request_timeout;
	private long				store_timeout;
	private LoggerChannel		logger;
	
	private PRUDPPacketHandler			packet_handler;
	
	private DHTTransportRequestHandler	request_handler;
	
	private DHTTransportUDPContactImpl		local_contact;
	
	private Map transfer_handlers 	= new HashMap();
	private Map	transfers			= new HashMap();
	
	private long last_address_change;
	
	private List listeners	= new ArrayList();
	
	private IpFilter	ip_filter	= IpFilterManagerFactory.getSingleton().getIPFilter();

	
	private DHTTransportUDPStatsImpl	stats;

	private boolean		bootstrap_node	= false;
	
	
	private static final int CONTACT_HISTORY_MAX = 32;
	
	private Map	contact_history = 
		new LinkedHashMap(CONTACT_HISTORY_MAX,0.75f,true)
		{
			protected boolean 
			removeEldestEntry(
		   		Map.Entry eldest) 
			{
				return size() > CONTACT_HISTORY_MAX;
			}
		};
		
	private static final int RECENT_REPORTS_HISTORY_MAX = 32;

	private Map	recent_reports = 
			new LinkedHashMap(RECENT_REPORTS_HISTORY_MAX,0.75f,true)
			{
				protected boolean 
				removeEldestEntry(
			   		Map.Entry eldest) 
				{
					return size() > RECENT_REPORTS_HISTORY_MAX;
				}
			};
		
		// TODO: secure enough?
	
	private	Random		random = new Random( SystemTime.getCurrentTime());
	
	
	private static AEMonitor	class_mon	= new AEMonitor( "DHTTransportUDP:class" );
	
	private AEMonitor	this_mon	= new AEMonitor( "DHTTransportUDP" );

	public
	DHTTransportUDPImpl(
		String			_ip,
		int				_port,
		int				_max_fails_for_live,
		int				_max_fails_for_unknown,
		long			_timeout,
		int				_dht_send_delay,
		int				_dht_receive_delay,
		boolean			_bootstrap_node,
		LoggerChannel	_logger )
	
		throws DHTTransportException
	{
		ip_override				= _ip;
		port					= _port;
		max_fails_for_live		= _max_fails_for_live;
		max_fails_for_unknown	= _max_fails_for_unknown;
		request_timeout			= _timeout;
		bootstrap_node			= _bootstrap_node;
		logger					= _logger;
				
		store_timeout			= request_timeout * 2;
		
		DHTUDPPacket.registerCodecs( logger );

			// DHTPRUDPPacket relies on the request-handler being an instanceof THIS so watch out
			// if you change it :)
		
		packet_handler = PRUDPPacketHandlerFactory.getHandler( _port, this );		

			// limit send and receive rates. Receive rate is lower as we want a stricter limit
			// on the max speed we generate packets than those we're willing to process.
		
		// logger.log( "send delay = " + _dht_send_delay + ", recv = " + _dht_receive_delay );
		
		packet_handler.setDelays( _dht_send_delay, _dht_receive_delay, (int)request_timeout );
		
		stats =  new DHTTransportUDPStatsImpl( packet_handler.getStats());
		
		getExternalAddress( "127.0.0.1", logger );
		
		InetSocketAddress	address = new InetSocketAddress( external_address, port );

		logger.log( "Initial external address: " + address );
		
		local_contact = new DHTTransportUDPContactImpl( this, address, address, DHTUDPPacket.VERSION, random.nextInt(), 0);
	}
	
	public void
	testInstanceIDChange()
	
		throws DHTTransportException
	{
		local_contact = new DHTTransportUDPContactImpl( this, local_contact.getTransportAddress(), local_contact.getExternalAddress(), DHTUDPPacket.VERSION, random.nextInt(), 0);		
	}
	
	public void
	testTransportIDChange()
	
		throws DHTTransportException
	{
		if ( external_address.equals("127.0.0.1")){
			
			external_address = "192.168.0.2";
		}else{
			
			external_address = "127.0.0.1";
		}
		
		InetSocketAddress	address = new InetSocketAddress( external_address, port );
		
		local_contact = new DHTTransportUDPContactImpl( this, address, address, DHTUDPPacket.VERSION, local_contact.getInstanceID(), 0 );		

		for (int i=0;i<listeners.size();i++){
			
			try{
				((DHTTransportListener)listeners.get(i)).localContactChanged( local_contact );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	public void
	testExternalAddressChange()
	{
		try{
			Iterator	it = contact_history.values().iterator();
			
			DHTTransportUDPContactImpl c1 = (DHTTransportUDPContactImpl)it.next();
			DHTTransportUDPContactImpl c2 = (DHTTransportUDPContactImpl)it.next();
			
			externalAddressChange( c1, c2.getExternalAddress());
			//externalAddressChange( c, new InetSocketAddress( "192.168.0.7", 6881 ));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected void
	getExternalAddress(
		String				default_address,
		final LoggerChannel	log )
	{
			// class level synchronisation is for testing purposes when running multiple UDP instances
			// in the same VM
		
		try{
			class_mon.enter();
			
			String new_external_address = null;
			
			try{				
				log.log( "Obtaining external address" );
				
				if ( TEST_EXTERNAL_IP ){
					
					new_external_address	= "192.168.0.2";
					
					log.log( "    External IP address obtained from test data: " + new_external_address );
				}
					
				if ( ip_override != null ){
					
					new_external_address	= ip_override;
					
					log.log( "    External IP address explicitly overridden: " + new_external_address );
				}
				
				if ( new_external_address == null ){

						// First attempt is via other contacts we know about. Select three
					
					List	contacts;
					
					try{
						this_mon.enter();
						
						contacts = new ArrayList( contact_history.values());
						
					}finally{
						
						this_mon.exit();
					}
					
						// randomly select up to 10 entries to ping until we 
						// get three replies
					
					String	returned_address 	= null;
					int		returned_matches	= 0;
					
					int		search_lim = Math.min(10, contacts.size());
					
					log.log( "    Contacts to search = " + search_lim );
					
					for (int i=0;i<search_lim;i++){
						
						DHTTransportUDPContactImpl	contact = (DHTTransportUDPContactImpl)contacts.remove((int)(contacts.size()*Math.random()));
													
						InetSocketAddress a = askContactForExternalAddress( contact );
						
						if ( a != null && a.getAddress() != null ){
							
							String	ip = a.getAddress().getHostAddress();
							
							if ( returned_address == null ){
								
								returned_address = ip;
																	
								log.log( "    : contact " + contact.getString() + " reported external address as '" + ip + "'" );
								
								returned_matches++;
								
							}else if ( returned_address.equals( ip )){
								
								returned_matches++;
								
								log.log( "    : contact " + contact.getString() + " also reported external address as '" + ip + "'" );
								
								if ( returned_matches == 3 ){
									
									new_external_address	= returned_address;
									
									log.log( "    External IP address obtained from contacts: "  + returned_address );
									
									break;
								}
							}else{
								
								log.log( "    : contact " + contact.getString() + " reported external address as '" + ip + "', abandoning due to mismatch" );
								
									// mismatch - give up
								
								break;
							}
						}else{
							
							log.log( "    : contact " + contact.getString() + " didn't reply" );
						}
					}

				}
				
				if ( new_external_address == null ){
			
					new_external_address = logger.getLogger().getPluginInterface().getUtilities().getPublicAddress().getHostAddress();
								
					if ( new_external_address != null ){
							
						log.log( "    External IP address obtained: " + new_external_address );
					}
				}
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		
			if ( new_external_address == null ){
			
				new_external_address =	default_address;
			
				log.log( "    External IP address defaulted:  " + new_external_address );
			}
			
			if ( external_address == null || !external_address.equals( new_external_address )){
				
				informLocalAddress( new_external_address );
			}
			
			external_address = new_external_address;
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	protected void
	informLocalAddress(
		String	address )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((DHTTransportListener)listeners.get(i)).currentAddress( address );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}		
	}
		
	protected void
	externalAddressChange(
		DHTTransportUDPContactImpl	reporter,
		InetSocketAddress			new_address )
	
		throws DHTTransportException
	{
			/*
			 * A node has reported that our external address and the one he's seen a 
			 * message coming from differ. Natural explanations are along the lines of
			 * 1) my address is dynamically allocated by my ISP and it has changed
			 * 2) I have multiple network interfaces 
			 * 3) there's some kind of proxy going on
			 * 4) this is a DOS attempting to stuff me up
			 * 
			 * We assume that our address won't change more frequently than once every
			 * 5 minutes
			 * We assume that if we can successfully obtain an external address by
			 * using the above explicit check then this is accurate
			 * Only in the case where the above check fails do we believe the address
			 * that we've been told about
			 */
			
		InetAddress	ia = new_address.getAddress();
		
		if ( ia == null ){
			
			Debug.out( "reported new external address '" + new_address + "' is unresolved" );
			
			throw( new DHTTransportException( "Address '" + new_address + "' is unresolved" ));
		}
		
		final String	new_ip = ia.getHostAddress();
		
		if ( new_ip.equals( external_address )){
			
				// probably just be a second notification of an address change, return
				// "ok to retry" as it should now work
							
			return;
		}
		
		try{
			this_mon.enter();
	
			long	now = SystemTime.getCurrentTime();
	
			if ( now - last_address_change < 5*60*1000 ){
				
				return;
			}
			
			logger.log( "Node " + reporter.getString() + " has reported that the external IP address is '" + new_address + "'" );
	
				// check for dodgy addresses that shouldn't appear as an external address!
			
			if ( invalidExternalAddress( ia )){
				
				logger.log( "     This is invalid as it is a private address." );
	
				return;
			}
	
				// another situation to ignore is where the reported address is the same as
				// the reporter (they must be seeing it via, say, socks connection on a local
				// interface
			
			if ( reporter.getExternalAddress().getAddress().getHostAddress().equals( new_ip )){
				
				logger.log( "     This is invalid as it is the same as the reporter's address." );
	
				return;		
			}
			
			last_address_change	= now;
			
		}finally{
			
			this_mon.exit();
		}
		
		final String	old_external_address = external_address;
		
			// we need to perform this test on a separate thread otherwise we'll block in the UDP handling
			// code because we're already running on the "process" callback from the UDP handler
			// (the test attempts to ping contacts)
			
		
		new AEThread( "DHTTransportUDP:getAddress", true )
		{
			public void
			runSupport()
			{
				getExternalAddress( new_ip, logger );
				
				if ( old_external_address.equals( external_address )){
					
						// address hasn't changed, notifier must be perceiving different address
						// due to proxy or something
									
					return;
				}
				
				InetSocketAddress	s_address = new InetSocketAddress( external_address, port );
		
				try{
					local_contact = new DHTTransportUDPContactImpl( DHTTransportUDPImpl.this, s_address, s_address, DHTUDPPacket.VERSION, random.nextInt(), 0);
			
					logger.log( "External address changed: " + s_address );
					
					for (int i=0;i<listeners.size();i++){
						
						try{
							((DHTTransportListener)listeners.get(i)).localContactChanged( local_contact );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
					}
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}.start();
	}
	
	protected void
	contactAlive(
		DHTTransportUDPContactImpl	contact )
	{
		try{
			this_mon.enter();
			
			contact_history.put( contact.getTransportAddress(), contact );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected boolean
	invalidExternalAddress(
		InetAddress	ia )
	{
		return(	ia.isLinkLocalAddress() ||
				ia.isLoopbackAddress() ||
				ia.isSiteLocalAddress()); 
	}
	
	protected int
	getMaxFailForLiveCount()
	{
		return( max_fails_for_live );
	}
	
	protected int
	getMaxFailForUnknownCount()
	{
		return( max_fails_for_unknown );
	}
	
	public DHTTransportContact
	getLocalContact()
	{
		return( local_contact );
	}
	
	public DHTTransportContact
	importContact(
		DataInputStream		is )
	
		throws IOException, DHTTransportException
	{
		DHTTransportContact	contact = DHTUDPUtils.deserialiseContact( this, is );
		
		request_handler.contactImported( contact );
				
		logger.log( "Imported contact " + contact.getString());
		
		return( contact );
	}
	
	public DHTTransportContact
	importContact(
		InetSocketAddress	address,
		byte				protocol_version )
	
		throws DHTTransportException
	{
			// instance id of 0 means "unknown"
		
		DHTTransportContact	contact = new DHTTransportUDPContactImpl( this, address, address, protocol_version, 0, 0 );
		
		request_handler.contactImported( contact );
		
		logger.log( "Imported contact " + contact.getString());

		return( contact );
	}
	
	public void
	exportContact(
		DHTTransportContact	contact,
		DataOutputStream	os )
	
		throws IOException, DHTTransportException
	{
		DHTUDPUtils.serialiseContact( os, contact );
	}
	
	public void
	removeContact(
		DHTTransportContact	contact )
	{
		request_handler.contactRemoved( contact );
	}
	
	public void
	setRequestHandler(
		DHTTransportRequestHandler	_request_handler )
	{
		request_handler = new DHTTransportRequestCounter( _request_handler, stats );
	}
	
	public DHTTransportStats
	getStats()
	{
		return( stats );
	}
	
	//protected HashMap	port_map = new HashMap();
	//protected long		last_portmap_dump	= SystemTime.getCurrentTime();
	
	protected void
	checkAddress(
		DHTTransportUDPContactImpl		contact )
	
		throws PRUDPPacketHandlerException
	{
		/*
		int	port = contact.getExternalAddress().getPort();
		
		try{
			this_mon.enter();
		
			int	count;
			
			Integer i = (Integer)port_map.get(new Integer(port));
			
			if ( i != null ){
				
				count 	= i.intValue() + 1;
				
			}else{
				
				count	= 1;
			}
			
			port_map.put( new Integer(port), new Integer(count));
			
			long	now = SystemTime.getCurrentTime();
			
			if ( now - last_portmap_dump > 60000 ){
				
				last_portmap_dump	= now;
				
				Iterator	it = port_map.keySet().iterator();
				
				Map	rev = new TreeMap();
				
				while( it.hasNext()){
					
					Integer	key = (Integer)it.next();
					
					Integer	val = (Integer)port_map.get(key);
					
					rev.put( val, key );
				}
				
				it = rev.keySet().iterator();
				
				while( it.hasNext()){
					
					Integer	val = (Integer)it.next();
					
					Integer	key = (Integer)rev.get(val);
					
					System.out.println( "port:" + key + "->" + val );
				}
			}
			
		}finally{
			
			this_mon.exit();
		}
		*/
		
		if ( ip_filter.isInRange( contact.getTransportAddress().getAddress().getHostAddress(), "DHT" )){
			
			throw( new PRUDPPacketHandlerException( "IPFilter check fails" ));
		}
	}
	
	protected void
	sendPing(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler )
	{
		stats.pingSent();

		final long	connection_id = getConnectionID();
		
		final DHTUDPPacketRequestPing	request = 
			new DHTUDPPacketRequestPing( connection_id, local_contact, contact );
			
		try{
			checkAddress( contact );
			
			packet_handler.sendAndReceive(
				request,
				contact.getTransportAddress(),
				new PRUDPPacketReceiver()
				{
					public void
					packetReceived(
						PRUDPPacket			_packet,
						InetSocketAddress	from_address )
					{
						try{
							DHTUDPPacketReply	packet = (DHTUDPPacketReply)_packet;
							
							if ( packet.getConnectionId() != connection_id ){
								
								throw( new Exception( "connection id mismatch" ));
							}
							
							contact.setInstanceID( packet.getTargetInstanceID());
							
							handleErrorReply( contact, packet );							
								
							stats.pingOK();
							
							handler.pingReply( contact );
						
						}catch( PRUDPPacketHandlerException e ){
							
							error( e );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							error( new PRUDPPacketHandlerException( "ping failed", e ));
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						stats.pingFailed();
						
						handler.failed( contact,e );
					}
				},
				request_timeout, false );
			
		}catch( Throwable e ){
			
			stats.pingFailed();
			
			handler.failed( contact,e );
		}
	}
	
		// stats
	
	protected void
	sendStats(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler )
	{
		stats.statsSent();

		final long	connection_id = getConnectionID();
		
		final DHTUDPPacketRequestStats	request = 
			new DHTUDPPacketRequestStats( connection_id, local_contact, contact );
			
		try{
			checkAddress( contact );
			
			packet_handler.sendAndReceive(
				request,
				contact.getTransportAddress(),
				new PRUDPPacketReceiver()
				{					
					public void
					packetReceived(
						PRUDPPacket			_packet,
						InetSocketAddress	from_address )
					{
						try{
							DHTUDPPacketReply	packet = (DHTUDPPacketReply)_packet;
							
							if ( packet.getConnectionId() != connection_id ){
								
								throw( new Exception( "connection id mismatch" ));
							}
							
							contact.setInstanceID( packet.getTargetInstanceID());
							
							handleErrorReply( contact, packet );
										
							DHTUDPPacketReplyStats	reply = (DHTUDPPacketReplyStats)packet;

							stats.statsOK();
							
							handler.statsReply( contact, reply.getStats());
						
						}catch( PRUDPPacketHandlerException e ){
							
							error( e );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							error( new PRUDPPacketHandlerException( "stats failed", e ));
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						stats.statsFailed();
						
						handler.failed( contact, e );
					}
				},
				request_timeout, true );
			
		}catch( Throwable e ){
			
			stats.statsFailed();
			
			handler.failed( contact, e );
		}
	}
	
		// PING for deducing external IP address
	
	protected InetSocketAddress
	askContactForExternalAddress(
		DHTTransportUDPContactImpl	contact )
	{
		stats.pingSent();

		final long	connection_id = getConnectionID();
	
		final DHTUDPPacketRequestPing	request = 
			new DHTUDPPacketRequestPing( connection_id, local_contact, contact );
		
		try{
			checkAddress( contact );
		
			final AESemaphore	sem = new AESemaphore( "DHTTransUDP:extping" );

			final InetSocketAddress[]	result = new InetSocketAddress[1];
			
			packet_handler.sendAndReceive(
				request,
				contact.getTransportAddress(),
				new PRUDPPacketReceiver()
				{
					public void
					packetReceived(
						PRUDPPacket			_packet,
						InetSocketAddress	from_address )
					{
						try{
							
							if ( _packet instanceof DHTUDPPacketReplyPing ){
								
								// ping was OK so current address is OK
								
								result[0] = local_contact.getExternalAddress();
								
							}else if ( _packet instanceof DHTUDPPacketReplyError ){
								
								DHTUDPPacketReplyError	packet = (DHTUDPPacketReplyError)_packet;
								
								if ( packet.getErrorType() == DHTUDPPacketReplyError.ET_ORIGINATOR_ADDRESS_WRONG ){
									
									result[0] = packet.getOriginatingAddress();
								}
							}
						}finally{
							
							sem.release();
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						try{
							stats.pingFailed();
							
						}finally{
						
							sem.release();
						}
					}
				},
				5000, false );
			
			sem.reserve( 5000 );
			
			return( result[0] );
			
		}catch( Throwable e ){
			
			stats.pingFailed();

			return( null );
		}
	}
	
		// STORE
	
	public void
	sendStore(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		byte[][]							keys,
		DHTTransportValue[][]				value_sets )
	{
		stats.storeSent();

		final long	connection_id = getConnectionID();
		
		if ( false ){
			int	total_values = 0;
			for (int i=0;i<keys.length;i++){
				total_values += value_sets[i].length;
			}
			System.out.println( "store: keys = " + keys.length +", values = " + total_values );
		}
		
			// only report to caller the outcome of the first packet
		
		int	packet_count	= 0;
		
		try{
			checkAddress( contact );
			
			int		current_key_index	= 0;
			int		current_value_index	= 0;
			
			while( current_key_index < keys.length ){
			
				packet_count++;
								
				int	space = DHTUDPPacket.PACKET_MAX_BYTES - DHTUDPPacketRequest.DHT_HEADER_SIZE;
								
				List	key_list	= new ArrayList();
				List	values_list	= new ArrayList();
								
				key_list.add( keys[current_key_index]);
				
				space -= ( keys[current_key_index].length + 1 );	// 1 for length marker
				
				values_list.add( new ArrayList());
				
				while( 	space > 0 &&  
						current_key_index < keys.length ){
					
					if ( current_value_index == value_sets[current_key_index].length ){
						
							// all values from the current key have been processed
						
						current_key_index++;
						
						current_value_index	= 0;
						
						if ( key_list.size() == DHTUDPPacketRequestStore.MAX_KEYS_PER_PACKET ){
							
								// no more keys allowed in this packet
								
							break;
						}
							
						if ( current_key_index == keys.length ){
						
								// no more keys left, job done
							
							break;
						}
						
						key_list.add( keys[current_key_index]);
						
						space -= ( keys[current_key_index].length + 1 );	// 1 for length marker
						
						values_list.add( new ArrayList());
					}
					
					DHTTransportValue	value = value_sets[current_key_index][current_value_index];
					
					int	entry_size = DHTUDPUtils.DHTTRANSPORTVALUE_SIZE_WITHOUT_VALUE + value.getValue().length + 1;
					
					List	values = (List)values_list.get(values_list.size()-1);
					
					if ( 	space < entry_size || 
							values.size() == DHTUDPPacketRequestStore.MAX_VALUES_PER_KEY ){
						
							// no space left or we've used up our limit on the
							// number of values permitted per key
						
						break;
					}
					
					values.add( value );
					
					space -= entry_size;
					
					current_value_index++;
				}
				
				int	packet_entries = key_list.size();

				if ( packet_entries > 0 ){
				
						// if last entry has no values then ignore it
					
					if ( ((List)values_list.get( packet_entries-1)).size() == 0 ){
						
						packet_entries--;
					}
				}
				
				if ( packet_entries == 0 ){
					
					break;
				}
	
				byte[][]				packet_keys 		= new byte[packet_entries][];
				DHTTransportValue[][]	packet_value_sets 	= new DHTTransportValue[packet_entries][];
				
				//int	packet_value_count = 0;
				
				for (int i=0;i<packet_entries;i++){
					
					packet_keys[i] = (byte[])key_list.get(i);
					
					List	values = (List)values_list.get(i);
					
					packet_value_sets[i] = new DHTTransportValue[values.size()];
					
					for (int j=0;j<values.size();j++){
						
						packet_value_sets[i][j] = (DHTTransportValue)values.get(j);
						
						//packet_value_count++;
					}
				}
				
				// System.out.println( "    packet " + packet_count + ": keys = " + packet_entries + ", values = " + packet_value_count );

					// first packet sending recorded on entry
				
				if ( packet_count > 1 ){
				
					stats.storeSent();
				}
				
				final DHTUDPPacketRequestStore	request = 
					new DHTUDPPacketRequestStore( connection_id, local_contact, contact );
			
				
				request.setRandomID( contact.getRandomID());
				
				request.setKeys( packet_keys );
				
				request.setValueSets( packet_value_sets );
				
				final int f_packet_count	= packet_count;
				
				packet_handler.sendAndReceive(
					request,
					contact.getTransportAddress(),
					new PRUDPPacketReceiver()
					{
						public void
						packetReceived(
							PRUDPPacket			_packet,
							InetSocketAddress	from_address )
						{
							try{
								DHTUDPPacketReply	packet = (DHTUDPPacketReply)_packet;
								
								if ( packet.getConnectionId() != connection_id ){
									
									throw( new Exception( "connection id mismatch" ));
								}
								
								contact.setInstanceID( packet.getTargetInstanceID());
								
								handleErrorReply( contact, packet );

								DHTUDPPacketReplyStore	reply = (DHTUDPPacketReplyStore)packet;
									
								stats.storeOK();
									
								if ( f_packet_count == 1 ){
										
									handler.storeReply( contact, reply.getDiversificationTypes());
								}
								
							}catch( PRUDPPacketHandlerException e ){
								
								error( e );
								
							}catch( Throwable e ){
								
								Debug.printStackTrace(e);
								
								error( new PRUDPPacketHandlerException( "store failed", e ));
							}
						}
						
						public void
						error(
							PRUDPPacketHandlerException	e )
						{
							stats.storeFailed();
							
							if ( f_packet_count == 1 ){
								
								handler.failed( contact, e );
							}
						}
					},
					store_timeout,
					true );	// low priority

			}
		}catch( Throwable e ){
										
			stats.storeFailed();
			
			if ( packet_count <= 1 ){
								
				handler.failed( contact, e );
			}
		}
	}
	
		// FIND NODE
	
	public void
	sendFindNode(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		byte[]								nid )
	{
		stats.findNodeSent();
		
		final long	connection_id = getConnectionID();
		
		try{
			checkAddress( contact );
			
			final DHTUDPPacketRequestFindNode	request = 
				new DHTUDPPacketRequestFindNode( connection_id, local_contact, contact );
			
			request.setID( nid );
			
			packet_handler.sendAndReceive(
				request,
				contact.getTransportAddress(),
				new PRUDPPacketReceiver()
				{					
					public void
					packetReceived(
						PRUDPPacket			_packet,
						InetSocketAddress	from_address )
					{
						try{
							DHTUDPPacketReply	packet = (DHTUDPPacketReply)_packet;
														
							if ( packet.getConnectionId() != connection_id ){
								
								throw( new Exception( "connection id mismatch" ));
							}

							contact.setInstanceID( packet.getTargetInstanceID());
							
							handleErrorReply( contact, packet );
								
							DHTUDPPacketReplyFindNode	reply = (DHTUDPPacketReplyFindNode)packet;
							
								// copy out the random id in preparation for a possible subsequent
								// store operation
							
							contact.setRandomID( reply.getRandomID());
							
							stats.findNodeOK();
								
							handler.findNodeReply( contact, reply.getContacts());
							
						}catch( PRUDPPacketHandlerException e ){
							
							error( e );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							error( new PRUDPPacketHandlerException( "findNode failed", e ));
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						stats.findNodeFailed();
						
						handler.failed( contact, e );
					}
				},
				request_timeout, false );
			
		}catch( Throwable e ){
			
			stats.findNodeFailed();
			
			handler.failed( contact, e );
		}
	}
	
		// FIND VALUE
	
	public void
	sendFindValue(
		final DHTTransportUDPContactImpl	contact,
		final DHTTransportReplyHandler		handler,
		byte[]								key,
		int									max_values,
		byte								flags )
	{
		stats.findValueSent();

		final long	connection_id = getConnectionID();
		
		try{
			checkAddress( contact );
			
			final DHTUDPPacketRequestFindValue	request = 
				new DHTUDPPacketRequestFindValue( connection_id, local_contact, contact );
			
			request.setID( key );
			
			request.setMaximumValues( max_values );
			
			request.setFlags( flags );
			
			packet_handler.sendAndReceive(
				request,
				contact.getTransportAddress(),
				new PRUDPPacketReceiver()
				{
					public void
					packetReceived(
						PRUDPPacket			_packet,
						InetSocketAddress	from_address )
					{
						try{
							DHTUDPPacketReply	packet = (DHTUDPPacketReply)_packet;
							
							if ( packet.getConnectionId() != connection_id ){
								
								throw( new Exception( "connection id mismatch" ));
							}
							
							contact.setInstanceID( packet.getTargetInstanceID());
							
							handleErrorReply( contact, packet );
								
							DHTUDPPacketReplyFindValue	reply = (DHTUDPPacketReplyFindValue)packet;
								
							stats.findValueOK();
								
							DHTTransportValue[]	res = reply.getValues();
								
							if ( res != null ){
									
								boolean	continuation = reply.hasContinuation();
																
								handler.findValueReply( contact, res, reply.getDiversificationType(), continuation);
									
							}else{
									
								handler.findValueReply( contact, reply.getContacts());
							}
						}catch( PRUDPPacketHandlerException e ){
							
							error( e );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
							
							error( new PRUDPPacketHandlerException( "findValue failed", e ));
						}
					}
					
					public void
					error(
						PRUDPPacketHandlerException	e )
					{
						stats.findValueFailed();
						
						handler.failed( contact, e );
					}
				},
				request_timeout, false );
			
		}catch( Throwable e ){
			
			if ( !(e instanceof PRUDPPacketHandlerException )){
				
				stats.findValueFailed();
			
				handler.failed( contact, e );
			}
		}
	}
	
	protected DHTTransportFullStats
	getFullStats(
		DHTTransportUDPContactImpl	contact )
	{
		if ( contact == local_contact ){
			
			return( request_handler.statsRequest( contact ));
		}
		
		final DHTTransportFullStats[] res = { null };
		
		final AESemaphore	sem = new AESemaphore( "DHTTransportUDP:getFullStats");
		
		sendStats(	contact,
					new DHTTransportReplyHandlerAdapter()
					{
						public void
						statsReply(
							DHTTransportContact 	_contact,
							DHTTransportFullStats	_stats )
						{
							res[0]	= _stats;
							
							sem.release();
						}
						
						public void
						failed(
							DHTTransportContact 	_contact,
							Throwable				_error )
						{
							sem.release();
						}
				
					});
		
		sem.reserve();

		return( res[0] );
	}
	
		// read request
	
	protected void
	sendReadRequest(
		long						connection_id,	
		DHTTransportUDPContactImpl	contact,
		byte[]						transfer_key,
		byte[]						key )
	{
		sendReadRequest( connection_id, contact, transfer_key, key, 0, 0 );
	}
	
	protected void
	sendReadRequest(
		long						connection_id,	
		DHTTransportUDPContactImpl	contact,
		byte[]						transfer_key,
		byte[]						key,
		int							start_pos,
		int							len )
	{
		final DHTUDPPacketData	request = 
			new DHTUDPPacketData( connection_id, local_contact, contact );
			
		request.setDetails( DHTUDPPacketData.PT_READ_REQUEST, transfer_key, key, new byte[0], start_pos, len, 0 );
				
		try{
			checkAddress( contact );
			
			logger.log( "Transfer read request: key = " + DHTLog.getFullString( key ) + ", contact = " + contact.getString());
			
			packet_handler.send(
				request,
				contact.getTransportAddress());
			
		}catch( Throwable e ){
			
		}
	}
	
	protected void
	sendReadReply(
		long						connection_id,	
		DHTTransportUDPContactImpl	contact,
		byte[]						transfer_key,
		byte[]						key,
		byte[]						data,
		int							start_position,
		int							length,
		int							total_length )
	{
		final DHTUDPPacketData	request = 
			new DHTUDPPacketData( connection_id, local_contact, contact );
			
		request.setDetails( DHTUDPPacketData.PT_READ_REPLY, transfer_key, key, data, start_position, length, total_length );
		
		try{
			checkAddress( contact );
			
			logger.log( "Transfer read reply: key = " + DHTLog.getFullString( key ));

			packet_handler.send(
				request,
				contact.getTransportAddress());
			
		}catch( Throwable e ){
			
		}
	}
	public void
	registerTransferHandler(
		byte[]						handler_key,
		DHTTransportTransferHandler	handler )
	{
		transfer_handlers.put( new HashWrapper( handler_key ), handler );
	}
	
	protected void
	dataRequest(
		DHTTransportUDPContactImpl	originator,
		DHTUDPPacketData			req )
	{
			// both requests and replies come through here. Currently we only support read
			// requests so we can safely use the data.length == 0 test to discriminate between
			// a request and a reply to an existing transfer
		
		byte	packet_type = req.getPacketType();
		
		if (	packet_type == DHTUDPPacketData.PT_READ_REPLY ||
				packet_type == DHTUDPPacketData.PT_WRITE_REPLY ){
			
			transferQueue	queue = lookupTransferQueue( req.getConnectionId());
			
				// unmatched -> drop it
			
			if ( queue != null ){
			
				queue.add( req );
			}
				
		}else{
			
			byte[]	transfer_key = req.getTransferKey();
			
			DHTTransportTransferHandler	handler = (DHTTransportTransferHandler)transfer_handlers.get(new HashWrapper( transfer_key ));
			
			if ( handler == null ){
				
				logger.log( "No transfer handler for '" + req.getString() + "'" );
				
			}else{
						
				if ( packet_type == DHTUDPPacketData.PT_READ_REQUEST ){
					
					byte[] data = handler.handleRead( originator, req.getRequestKey());
					
					if ( data != null ){
							
							// special case 0 length data
						
						if ( data.length == 0 ){
							sendReadReply( 
									req.getConnectionId(),
									originator,
									transfer_key,
									req.getRequestKey(),
									data,
									0,
									0,
									0 );							
						}else{
							int	start = req.getStartPosition();
							
							if ( start < 0 ){
								
								start	= 0;
								
							}else if ( start >= data.length ){
								
								logger.log( "dataRequest: invalid start position" );
								
								return;
							}
							
							int len = req.getLength();
							
							if ( len <= 0 ){
								
								len = data.length;
								
							}else if ( start + len > data.length ){
								
								logger.log( "dataRequest: invalid length" );
								
								return;
							}
							
							int	end = start+len;
							
							while( start < end ){
								
								int	chunk = end - start;
								
								if ( chunk > DHTUDPPacketData.MAX_DATA_SIZE ){
									
									chunk = DHTUDPPacketData.MAX_DATA_SIZE;								
								}
								
								sendReadReply( 
										req.getConnectionId(),
										originator,
										transfer_key,
										req.getRequestKey(),
										data,
										start,
										chunk,
										data.length );
								
								start += chunk;
							}
						}
					}
				}else{
					
					// write request not supported
					
				}
			}
		}
	}
		
	public byte[]
	readTransfer(
		DHTTransportProgressListener	listener,
		DHTTransportContact				target,
		byte[]							handler_key,
		byte[]							key,
		long							timeout )
	
		throws DHTTransportException
	{
		long	connection_id 	= getConnectionID();
		
		transferQueue	transfer_queue = new transferQueue( connection_id );
		
		SortedSet	packets = 
			new TreeSet(
				new Comparator()
				{
					public int
					compare(
						Object	o1,
						Object	o2 )
					{
						DHTUDPPacketData	p1 = (DHTUDPPacketData)o1;
						DHTUDPPacketData	p2 = (DHTUDPPacketData)o2;
						
						return( p1.getStartPosition() - p2.getStartPosition());
					}
				});
		
		int	entire_request_count = 0;
		
		int transfer_size 	= -1;
		int	transferred		= 0;
		
		String	target_name = DHTLog.getString2(target.getID());
		
		try{
			long	start = SystemTime.getCurrentTime();
			
			listener.reportActivity( "Requesting entire transfer from " + target_name );

			entire_request_count++;
			
			sendReadRequest( connection_id, (DHTTransportUDPContactImpl)target, handler_key, key );

			while( SystemTime.getCurrentTime() - start <= timeout ){					
				
				DHTUDPPacketData	reply = transfer_queue.receive( 10000 );
				
				if ( reply != null ){
	
					if ( transfer_size == -1 ){
						
						transfer_size = reply.getTotalLength();
						
						listener.reportSize( transfer_size );
					}
					
					Iterator	it = packets.iterator();
					
					boolean	duplicate = false;
					
					while( it.hasNext()){
						
						DHTUDPPacketData	p = (DHTUDPPacketData)it.next();
						
							// ignore overlaps
						
						if (	p.getStartPosition() < reply.getStartPosition() + reply.getLength() &&
								p.getStartPosition() + p.getLength() > reply.getStartPosition()){
							
							duplicate	= true;
							
							break;
						}
					}
					
					if ( !duplicate ){
						
						listener.reportActivity( "Received " + reply.getStartPosition() + " to " + (reply.getStartPosition() + reply.getLength()) + " from " + target_name );

						transferred += reply.getLength();
						
						listener.reportCompleteness( transfer_size==0?100: ( 100 * transferred / transfer_size ));
						
						packets.add( reply );
						
							// see if we're done				
					
						it = packets.iterator();
						
						int	pos			= 0;
						int	actual_end	= -1;
						
						while( it.hasNext()){
							
							DHTUDPPacketData	p = (DHTUDPPacketData)it.next();
						
							if ( actual_end == -1 ){
								
								actual_end = p.getTotalLength();
							}
							
							if ( p.getStartPosition() != pos ){
								
									// missing data, give up
								
								break;
							}
							
							pos += p.getLength();
							
							if ( pos == actual_end ){
							
									// huzzah, we got the lot
							
								listener.reportActivity( "Complete" );
								
								byte[]	result = new byte[actual_end];
								
								it =  packets.iterator();
								
								pos	= 0;
								
								while( it.hasNext()){
									
									p = (DHTUDPPacketData)it.next();

									System.arraycopy( p.getData(), 0, result, pos, p.getLength());
									
									pos	+= p.getLength();
								}
								
								return( result );
							}
						}
					}
				}else{
					
						// timeout, look for missing bits
					
					if ( packets.size() == 0 ){
						
						if ( entire_request_count == 2 ){
						
							listener.reportActivity( "Timeout, no replies received" );
							
							return( null );
						}
						
						entire_request_count++;
						
						listener.reportActivity( "Re-requesting entire transfer from " + target_name );
						
						sendReadRequest( connection_id, (DHTTransportUDPContactImpl)target, handler_key, key );
						
					}else{
						
						Iterator it = packets.iterator();
					
						int	pos			= 0;
						int	actual_end	= -1;
						
						while( it.hasNext()){
							
							DHTUDPPacketData	p = (DHTUDPPacketData)it.next();
						
							if ( actual_end == -1 ){
								
								actual_end = p.getTotalLength();
							}
							
							if ( p.getStartPosition() != pos ){
								
								listener.reportActivity( "Re-requesting " + pos + " to " + p.getStartPosition() +  " from " + target_name );
								
								sendReadRequest( 
										connection_id, 
										(DHTTransportUDPContactImpl)target, 
										handler_key, 
										key,
										pos,
										p.getStartPosition()-pos );
							
							}
							
							pos = p.getStartPosition() + p.getLength();
						}
						
						if ( pos != actual_end ){
							
							listener.reportActivity( "Re-requesting " + pos + " to " + actual_end + " from " + target_name );

							sendReadRequest( 
									connection_id, 
									(DHTTransportUDPContactImpl)target, 
									handler_key, 
									key,
									pos,
									actual_end - pos );						
						}
					}
				}
			}
			
			listener.reportActivity( 
					"Timeout, " + 
						(packets.size()==0?
							" no replies received":
							("" + packets.size() + " packets received but incomplete" )));
			
			return( null );
			
		}finally{
			
			transfer_queue.destroy();
		}
	}
	
	public void
	writeTransfer(
		DHTTransportContact		target,
		byte[]					handler_key,
		byte[]					key,
		byte[]					data,
		long					timeout )
	
		throws DHTTransportException
	{
		throw( new DHTTransportException( "not imp" ));
	}
	
	
	
	
	public void
	process(
		PRUDPPacketRequest	_request )
	{
		if ( request_handler == null ){
			
			logger.log( "Ignoring packet as not yet ready to process" );
			
			return;
		}
		
		try{
			stats.incomingRequestReceived();
			
			DHTUDPPacketRequest	request = (DHTUDPPacketRequest)_request;
			
			InetSocketAddress	transport_address = request.getAddress();
			
			DHTTransportUDPContactImpl	originating_contact = 
				new DHTTransportUDPContactImpl( 
						this, 
						transport_address, 
						request.getOriginatorAddress(), 
						request.getVersion(),
						request.getOriginatorInstanceID(),
						request.getClockSkew());
			
			try{
				checkAddress( originating_contact );
					
			}catch( PRUDPPacketHandlerException e ){
				
				return;
			}

			if ( !originating_contact.addressMatchesID()){
				
				String	contact_string = originating_contact.getString();

				if ( recent_reports.get(contact_string) == null ){
					
					recent_reports.put( contact_string, "" );
					
					logger.log( "Node " + contact_string + " has incorrect ID, reporting it to them" );
				}
				
				DHTUDPPacketReplyError	reply = 
					new DHTUDPPacketReplyError(
							request.getTransactionId(),
							request.getConnectionId(),
							local_contact,
							originating_contact );
				
				reply.setErrorType( DHTUDPPacketReplyError.ET_ORIGINATOR_ADDRESS_WRONG );
				
				reply.setOriginatingAddress( originating_contact.getTransportAddress());
				
				packet_handler.send( reply, request.getAddress());

			}else{
				
				contactAlive( originating_contact );
				
				if ( request instanceof DHTUDPPacketRequestPing ){
					
					if ( !bootstrap_node ){
						
						request_handler.pingRequest( originating_contact );
						
						DHTUDPPacketReplyPing	reply = 
							new DHTUDPPacketReplyPing(
									request.getTransactionId(),
									request.getConnectionId(),
									local_contact,
									originating_contact );
						
						packet_handler.send( reply, request.getAddress());
					}
				}else if ( request instanceof DHTUDPPacketRequestStats ){
					
					DHTTransportFullStats	full_stats = request_handler.statsRequest( originating_contact );
					
					DHTUDPPacketReplyStats	reply = 
						new DHTUDPPacketReplyStats(
								request.getTransactionId(),
								request.getConnectionId(),
								local_contact,
								originating_contact );
					
					reply.setStats( full_stats );
					
					packet_handler.send( reply, request.getAddress());

				}else if ( request instanceof DHTUDPPacketRequestStore ){
					
					if ( !bootstrap_node ){

						DHTUDPPacketRequestStore	store_request = (DHTUDPPacketRequestStore)request;
						
						originating_contact.setRandomID( store_request.getRandomID());
						
						byte[] diversify = 
							request_handler.storeRequest(
								originating_contact, 
								store_request.getKeys(), 
								store_request.getValueSets());
						
						DHTUDPPacketReplyStore	reply = 
							new DHTUDPPacketReplyStore(
									request.getTransactionId(),
									request.getConnectionId(),
									local_contact,
									originating_contact );
						
						reply.setDiversificationTypes( diversify );
						
						packet_handler.send( reply, request.getAddress());
					}
					
				}else if ( request instanceof DHTUDPPacketRequestFindNode ){
					
					DHTUDPPacketRequestFindNode	find_request = (DHTUDPPacketRequestFindNode)request;
					
					boolean	acceptable;
					
						// as a bootstrap node we only accept find-node requests for the originator's
						// ID
					
					if ( bootstrap_node ){
						
						acceptable = Arrays.equals( find_request.getID(), originating_contact.getID());
												
					}else{
						
						acceptable	= true;
					}
					
					if ( acceptable ){
						
						DHTTransportContact[]	res = 
							request_handler.findNodeRequest(
										originating_contact,
										find_request.getID());
						
						DHTUDPPacketReplyFindNode	reply = 
							new DHTUDPPacketReplyFindNode(
									request.getTransactionId(),
									request.getConnectionId(),
									local_contact,
									originating_contact );
								
						reply.setRandomID( originating_contact.getRandomID());
						
						reply.setContacts( res );
						
						packet_handler.send( reply, request.getAddress());
					}
					
				}else if ( request instanceof DHTUDPPacketRequestFindValue ){
					
					if ( !bootstrap_node ){

						DHTUDPPacketRequestFindValue	find_request = (DHTUDPPacketRequestFindValue)request;
					
						DHTTransportFindValueReply res = 
							request_handler.findValueRequest(
										originating_contact,
										find_request.getID(),
										find_request.getMaximumValues(),
										find_request.getFlags());
						
						DHTUDPPacketReplyFindValue	reply = 
							new DHTUDPPacketReplyFindValue(
								request.getTransactionId(),
								request.getConnectionId(),
								local_contact,
								originating_contact );
						
						if ( res.hit()){
							
							DHTTransportValue[]	res_values = res.getValues();
							
							int		max_size = DHTUDPPacket.PACKET_MAX_BYTES - DHTUDPPacketReplyFindValue.DHT_FIND_VALUE_HEADER_SIZE;
														
							List	values 		= new ArrayList();
							int		values_size	= 0;
							
							int	pos = 0;
							
							while( pos < res_values.length ){
						
								DHTTransportValue	v = res_values[pos];
								
								int	v_len = v.getValue().length + DHTUDPPacketReplyFindValue.DHT_FIND_VALUE_TV_HEADER_SIZE;
								
								if ( 	values_size > 0 && // if value too big, cram it in anyway 
										values_size + v_len > max_size ){
									
										// won't fit, send what we've got
									
									DHTTransportValue[]	x = new DHTTransportValue[values.size()];
									
									values.toArray( x );
																		
									reply.setValues( x, res.getDiversificationType(), true );	// continuation = true
																	
									packet_handler.send( reply, request.getAddress());
									
									values_size	= 0;
									
									values		= new ArrayList();
									
								}else{
									
									values.add(v);
									
									values_size	+= v_len;
									
									pos++;
								}
							}
							
								// send the remaining (possible zero length) non-continuation values
								
							DHTTransportValue[]	x = new DHTTransportValue[values.size()];
								
							values.toArray( x );
								
							reply.setValues( x, res.getDiversificationType(), false );
															
							packet_handler.send( reply, request.getAddress());
						
						}else{
							
							reply.setContacts(res.getContacts());
							
							packet_handler.send( reply, request.getAddress());
						}
					}
				}else if ( request instanceof DHTUDPPacketData ){
					
					if ( !bootstrap_node ){
						
						dataRequest(originating_contact, (DHTUDPPacketData)request );
					}
				}else{
					
					Debug.out( "Unexpected packet:" + request.toString());
				}
			}
		}catch( PRUDPPacketHandlerException e ){
			
			// not interesting, send packet fail or something
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
		/**
		 * Returns false if this isn't an error reply, true if it is and a retry can be
		 * performed, throws an exception otherwise
		 * @param reply
		 * @return
		 * @throws PRUDPPacketHandlerException
		 */
	
	protected void
	handleErrorReply(
		DHTTransportUDPContactImpl	contact,
		DHTUDPPacketReply			reply )
	
		throws PRUDPPacketHandlerException
	{
		if ( reply.getAction() == DHTUDPPacket.ACT_REPLY_ERROR ){
			
			DHTUDPPacketReplyError	error = (DHTUDPPacketReplyError)reply;
						
			switch( error.getErrorType()){
			
				case DHTUDPPacketReplyError.ET_ORIGINATOR_ADDRESS_WRONG:
				{
					try{
						externalAddressChange( contact, error.getOriginatingAddress());
						
					}catch( DHTTransportException e ){
						
						Debug.printStackTrace(e);
					}
											
					break;
				}
				default:
				{
					Debug.out( "Unknown error type received" );
					
					break;
				}
			}
				
			throw( new PRUDPPacketHandlerException( "retry not permitted" ));
			
		}else{
			
			contactAlive( contact );
		}
	}
	
	protected long
	getConnectionID()
	{
			// unfortunately, to reuse the UDP port with the tracker protocol we 
			// have to distinguish our connection ids by setting the MSB. This allows
			// the decode to work as there is no common header format for the request
			// and reply packets
		
			// note that tracker usage of UDP via this handler is only for outbound
			// messages, hence for that use a request will never be received by the
			// handler
		
		return( 0x8000000000000000L | random.nextLong());
	}
	
	public boolean
	supportsStorage()
	{
		return( !bootstrap_node );
	}
	
	public void
	addListener(
		DHTTransportListener	l )
	{
		listeners.add(l);
		
		if ( external_address != null ){
			
			l.currentAddress( external_address );
		}
	}
	
	public void
	removeListener(
		DHTTransportListener	l )
	{
		listeners.remove(l);
	}

	protected transferQueue
	lookupTransferQueue(
		long		id )
	{
		try{
			this_mon.enter();

			return((transferQueue)transfers.get(new Long(id)));
			
		}finally{
			
			this_mon.exit();
		}	
	}
	
	protected class
	transferQueue
	{
		long		id;
		
		List		packets	= new ArrayList();
		
		AESemaphore	packets_sem	= new AESemaphore("DHTUDPTransport:transferQueue");
		
		protected
		transferQueue(
			long		_id )
		{
			id		= _id;
			
			try{
				this_mon.enter();

				transfers.put( new Long( id ), this );
				
			}finally{
				
				this_mon.exit();
			}			
		}
		
		protected void
		add(
			DHTUDPPacketData	packet )
		{
			try{
				this_mon.enter();
	
				packets.add( packet );
				
			}finally{
				
				this_mon.exit();
			}
			
			packets_sem.release();
		}
		
		protected DHTUDPPacketData
		receive(
			long	timeout )
		{
			if ( packets_sem.reserve( timeout )){
				
				try{
					this_mon.enter();
								
					return((DHTUDPPacketData)packets.remove(0));
					
				}finally{
					
					this_mon.exit();
				}				
			}else{
				
				return( null );
			}
		}
		
		protected void
		destroy()
		{
			try{
				this_mon.enter();
							
				transfers.remove( new Long( id ));
				
			}finally{
				
				this_mon.exit();
			}
		}
	}
}
