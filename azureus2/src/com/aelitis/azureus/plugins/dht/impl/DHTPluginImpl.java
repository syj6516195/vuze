/*
 * Created on 24-Jan-2005
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

package com.aelitis.azureus.plugins.dht.impl;


import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Properties;


import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTFactory;
import com.aelitis.azureus.core.dht.DHTOperationListener;

import com.aelitis.azureus.core.dht.control.DHTControlStats;
import com.aelitis.azureus.core.dht.db.DHTDBStats;
import com.aelitis.azureus.core.dht.router.DHTRouterStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportException;
import com.aelitis.azureus.core.dht.transport.DHTTransportFactory;

import com.aelitis.azureus.core.dht.transport.DHTTransportListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportProgressListener;
import com.aelitis.azureus.core.dht.transport.DHTTransportStats;
import com.aelitis.azureus.core.dht.transport.DHTTransportTransferHandler;
import com.aelitis.azureus.core.dht.transport.DHTTransportValue;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;

import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginOperationListener;
import com.aelitis.azureus.plugins.dht.DHTPluginProgressListener;
import com.aelitis.azureus.plugins.dht.DHTPluginTransferHandler;
import com.aelitis.azureus.plugins.dht.DHTPluginValue;
import com.aelitis.azureus.plugins.dht.impl.DHTPluginStorageManager;


/**
 * @author parg
 *
 */

public class 
DHTPluginImpl
{
	private static final String	SEED_ADDRESS	= "aelitis.com";
	private static final int	SEED_PORT		= 6881;
		
	private static final long	MIN_ROOT_SEED_IMPORT_PERIOD	= 8*60*60*1000;
	
		
	private PluginInterface		plugin_interface;
	
	private int					status;
	private String				status_text;
	
	private ActionParameter		reseed;
	
	private DHT					dht;
	private byte				protocol_version;
	private DHTTransportUDP		transport;
	private long				integrated_time;
		
	private DHTPluginStorageManager storage_manager;

	private long				last_root_seed_import_time;
			
	private LoggerChannel		log;
	

	public
	DHTPluginImpl(
		PluginInterface			_plugin_interface,
		byte					_protocol_version,
		int						_network,
		String					_ip,
		int						_port,
		ActionParameter			_reseed,
		boolean					_logging,
		LoggerChannel			_log )
	{
		plugin_interface	= _plugin_interface;
		protocol_version	= _protocol_version;
		reseed				= _reseed;
		log					= _log;
		
		try{
			storage_manager = new DHTPluginStorageManager( log, getDataDir( _network ));
			
			PluginConfig conf = plugin_interface.getPluginconfig();
			
			int	send_delay = conf.getPluginIntParameter( "dht.senddelay", 50 );
			int	recv_delay	= conf.getPluginIntParameter( "dht.recvdelay", 25 );
			
			boolean	bootstrap	= conf.getPluginBooleanParameter( "dht.bootstrapnode", false );
			
			final int f_port	= _port;

			transport = 
				DHTTransportFactory.createUDP( 
						_protocol_version,
						_network,
						_ip,
						storage_manager.getMostRecentAddress(),
						_port, 
						4,
						2,
						20000, 	// udp timeout - tried less but a significant number of 
								// premature timeouts occurred
						send_delay, recv_delay, 
						bootstrap,
						log );
			
			transport.addListener(
				new DHTTransportListener()
				{
					public void
					localContactChanged(
						DHTTransportContact	local_contact )
					{
					}
					
					public void
					currentAddress(
						String		address )
					{
						storage_manager.recordCurrentAddress( address );
					}
				});
				
			final int sample_frequency		= 60*1000;
			final int sample_duration		= 10*60;
			final int sample_stats_ticks	= 15;	// every 15 mins

			plugin_interface.getUtilities().createTimer("DHTStats").addPeriodicEvent(
					sample_frequency,
					new UTTimerEventPerformer()
					{
						Average	incoming_packet_average = Average.getInstance(sample_frequency,sample_duration);
						
						long	last_incoming;
						
						int	ticks = 0;
						
						public void
						perform(
							UTTimerEvent		event )
						{
							ticks++;
							
							if ( dht != null ){
								
								DHTTransportStats t_stats = transport.getStats();
													
								long	current_incoming = t_stats.getIncomingRequests();
								
								incoming_packet_average.addValue( (current_incoming-last_incoming)*sample_frequency/1000);
								
								last_incoming	= current_incoming;
								
								long	incoming_average = incoming_packet_average.getAverage();
								
								// System.out.println( "incoming average = " + incoming_average );
								
								long	now = SystemTime.getCurrentTime();
								
									// give some time for thing to generate reasonable stats
								
								if ( 	integrated_time > 0 &&
										now - integrated_time >= 5*60*1000 ){
								
										// 1 every 30 seconds indicates problems
									
									if ( incoming_average <= 2 ){
										
										String msg = "If you have a router/firewall, please check that you have port " + f_port + 
														" UDP open.\nDecentralised tracking requires this." ;

										int	warned_port = plugin_interface.getPluginconfig().getPluginIntParameter( "udp_warned_port", 0 );
										
										if ( warned_port == f_port  ){
											
											log.log( msg );
											
										}else{
											
											plugin_interface.getPluginconfig().setPluginParameter( "udp_warned_port", f_port );
											
											log.logAlert( LoggerChannel.LT_WARNING, msg );
										}
									}
								}
								
								if ( ticks % sample_stats_ticks == 0 ){
									
									DHTDBStats		d_stats	= dht.getDataBase().getStats();
									DHTControlStats	c_stats = dht.getControl().getStats();
									DHTRouterStats	r_stats = dht.getRouter().getStats();
									
									long[]	rs = r_stats.getStats();

									log.log( "DHT:ip=" + transport.getLocalContact().getAddress() + 
												",net=" + transport.getNetwork() +
												",prot=V" + transport.getProtocolVersion());

									log.log( 	"Router" +
												":nodes=" + rs[DHTRouterStats.ST_NODES] +
												",leaves=" + rs[DHTRouterStats.ST_LEAVES] +
												",contacts=" + rs[DHTRouterStats.ST_CONTACTS] +
												",replacement=" + rs[DHTRouterStats.ST_REPLACEMENTS] +
												",live=" + rs[DHTRouterStats.ST_CONTACTS_LIVE] +
												",unknown=" + rs[DHTRouterStats.ST_CONTACTS_UNKNOWN] +
												",failing=" + rs[DHTRouterStats.ST_CONTACTS_DEAD]);
						
									log.log( 	"Transport" + 
												":" + t_stats.getString()); 
											
									int[]	dbv_details = d_stats.getValueDetails();
									
									log.log(    "Control:dht=" + c_stats.getEstimatedDHTSize() + 
											   	", Database:keys=" + d_stats.getKeyCount() +
											   	",vals=" + dbv_details[DHTDBStats.VD_VALUE_COUNT]+
											   	",loc=" + dbv_details[DHTDBStats.VD_LOCAL_SIZE]+
											   	",dir=" + dbv_details[DHTDBStats.VD_DIRECT_SIZE]+
											   	",ind=" + dbv_details[DHTDBStats.VD_INDIRECT_SIZE]+
											   	",div_f=" + dbv_details[DHTDBStats.VD_DIV_FREQ]+
											   	",div_s=" + dbv_details[DHTDBStats.VD_DIV_SIZE] );
								}
							}
						}
					});
			
			Properties	props = new Properties();
			
			/*
			System.out.println( "FRIGGED REFRESH PERIOD" );
			
			props.put( DHT.PR_CACHE_REPUBLISH_INTERVAL, new Integer( 5*60*1000 ));
			*/
												
			dht = DHTFactory.create( 
						transport, 
						props,
						storage_manager,
						log );
			
			dht.setLogging( _logging );
			
			DHTTransportContact root_seed = importRootSeed();
			
			storage_manager.importContacts( dht );
			
			plugin_interface.getUtilities().createTimer( "DHTExport" ).addPeriodicEvent(
					10*60*1000,
					new UTTimerEventPerformer()
					{
						public void
						perform(
							UTTimerEvent		event )
						{
							checkForReSeed(false);
							
							storage_manager.exportContacts( dht );
						}
					});

			integrateDHT( true, root_seed );
			
			status = DHTPlugin.STATUS_RUNNING;
			
			status_text = "Running";
												
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			log.log( "DHT integrtion fails", e );
			
			status_text = "DHT Integration fails: " + Debug.getNestedExceptionMessage( e );
			
			status	= DHTPlugin.STATUS_FAILED;
		}
	}
	
	public int
	getStatus()
	{
		return( status );
	}
	
	public String
	getStatusText()
	{
		return( status_text );
	}
	
	public void
	setLogging(
		boolean		l )
	{
		dht.setLogging( l );
	}
	protected File
	getDataDir(
		int		network )
	{
		String	term = network==0?"":"." + network;
		
		File	dir = new File( plugin_interface.getUtilities().getAzureusUserDir(), "dht" + term );
		
		dir.mkdirs();
		
		return( dir );
	}
	
	public void
	integrateDHT(
		boolean				first,
		DHTTransportContact	remove_afterwards )
	{
		try{
			reseed.setEnabled( false );						

			log.log( "DHT " + (first?"":"re-") + "integration starts" );
		
			long	start = SystemTime.getCurrentTime();
			
			dht.integrate( false );
			
			if ( remove_afterwards != null ){
				
				log.log( "Removing seed " + remove_afterwards.getString());
				
				remove_afterwards.remove();
			}
			
			long	end = SystemTime.getCurrentTime();
	
			integrated_time	= end;
			
			log.log( "DHT " + (first?"":"re-") + "integration complete: elapsed = " + (end-start));
			
			dht.print();
			
		}finally{
			
			reseed.setEnabled( true );						
		}
	}
	
	public void
	checkForReSeed(
		boolean	force )
	{
		int	seed_limit = 32;
		
		try{
			
			long[]	router_stats = dht.getRouter().getStats().getStats();
		
			if ( router_stats[ DHTRouterStats.ST_CONTACTS_LIVE] < seed_limit || force ){
				
				if ( force ){
					
					log.log( "Reseeding" );
					
				}else{
					
					log.log( "Less than 32 live contacts, reseeding" );
				}
				
					// first look for peers to directly import
				
				Download[]	downloads = plugin_interface.getDownloadManager().getDownloads();
				
				int	peers_imported	= 0;
				
				for (int i=0;i<downloads.length;i++){
					
					Download	download = downloads[i];
					
					PeerManager pm = download.getPeerManager();
					
					if ( pm == null ){
						
						continue;
					}
					
					Peer[] 	peers = pm.getPeers();
					
outer:
					for (int j=0;j<peers.length;j++){
						
						Peer	p = peers[j];
						
						int	peer_udp_port = p.getUDPListenPort();
						
						if ( peer_udp_port != 0 ){
													
							if ( importSeed( p.getIp(), peer_udp_port ) != null ){
								
								peers_imported++;
															
								if ( peers_imported > seed_limit ){
									
									break outer;
								}
							}
						}	
					}
				}
				
				DHTTransportContact	root_to_remove = null;
				
				if ( peers_imported == 0 ){
				
					root_to_remove = importRootSeed();
					
					if ( root_to_remove != null ){
						
						peers_imported++;
					}
				}
				
				if ( peers_imported > 0 ){
					
					integrateDHT( false, root_to_remove );
				}
			}
			
		}catch( Throwable e ){
			
			log.log(e);
		}
	}
		
	protected DHTTransportContact
	importRootSeed()
	{
		try{
			long	 now = SystemTime.getCurrentTime();
			
			if ( now - last_root_seed_import_time > MIN_ROOT_SEED_IMPORT_PERIOD ){
		
				last_root_seed_import_time	= now;
				
				return( importSeed( getSeedAddress(), SEED_PORT ));
			
			}else{
				
				log.log( "    root seed imported too recently, ignoring" );
			}
		}catch( Throwable e ){
			
			log.log(e);
		}
		
		return( null );
	}
	
	public DHTTransportContact
	importSeed(
		String		ip,
		int			port )
	{
		try{
			
			return( importSeed( InetAddress.getByName( ip ), port ));
			
		}catch( Throwable e ){
			
			log.log(e);
			
			return( null );
		}
	}
	
	protected DHTTransportContact
	importSeed(
		InetAddress		ia,
		int				port )
	
	{
		try{
			return(
				transport.importContact( new InetSocketAddress(ia, port ), protocol_version ));
		
		}catch( Throwable e ){
			
			log.log(e);
			
			return( null );
		}
	}
	
	protected InetAddress
	getSeedAddress()
	{
		try{
			return( InetAddress.getByName( SEED_ADDRESS ));
			
		}catch( Throwable e ){
			
			try{
				return( InetAddress.getByName("213.186.46.164"));
				
			}catch( Throwable f ){
				
				log.log(f);
				
				return( null );
			}
		}
	}
	

	

	public void
	put(
		final byte[]						key,
		final String						description,
		final byte[]						value,
		final byte							flags,
		final DHTPluginOperationListener	listener)
	{		
		dht.put( 	key, 
					description,
					value,
					flags,
					new DHTOperationListener()
					{
						public void
						searching(
							DHTTransportContact	contact,
							int					level,
							int					active_searches )
						{
							String	indent = "";
							
							for (int i=0;i<level;i++){
								
								indent += "  ";
							}
							
							// log.log( indent + "Put: level = " + level + ", active = " + active_searches + ", contact = " + contact.getString());
						}
						
						public void
						found(
							DHTTransportContact	contact )
						{
						}

						public void
						read(
							DHTTransportContact	_contact,
							DHTTransportValue	_value )
						{
							Debug.out( "read operation not supported for puts" );
						}
						
						public void
						wrote(
							DHTTransportContact	_contact,
							DHTTransportValue	_value )
						{
							// log.log( "Put: wrote " + _value.getString() + " to " + _contact.getString());
							
							if ( listener != null ){
								
								listener.valueWritten( new DHTPluginContactImpl(DHTPluginImpl.this, _contact ), mapValue( _value ));
							}

						}
						
						public void
						complete(
							boolean				timeout )
						{
							// log.log( "Put: complete, timeout = " + timeout );
						
							if ( listener != null ){
								
								listener.complete( timeout );
							}
						}
					});
	}
	
	public DHTPluginValue
	getLocalValue(
		byte[]		key )
	{
		final DHTTransportValue	val = dht.getLocalValue( key );
		
		if ( val == null ){
			
			return( null );
		}
		
		return( mapValue( val ));
	}
	
	public void
	get(
		final byte[]								key,
		final String								description,
		final byte									flags,
		final int									max_values,
		final long									timeout,
		final boolean								exhaustive,
		final DHTPluginOperationListener			listener )
	{
		dht.get( 	key, description, flags, max_values, timeout, exhaustive, 
					new DHTOperationListener()
					{
						public void
						searching(
							DHTTransportContact	contact,
							int					level,
							int					active_searches )
						{
							String	indent = "";
							
							for (int i=0;i<level;i++){
								
								indent += "  ";
							}
							
							// log.log( indent + "Get: level = " + level + ", active = " + active_searches + ", contact = " + contact.getString());
						}
						
						public void
						found(
							DHTTransportContact	contact )
						{
						}

						public void
						read(
							final DHTTransportContact	contact,
							final DHTTransportValue		value )
						{
							// log.log( "Get: read " + value.getString() + " from " + contact.getString() + ", originator = " + value.getOriginator().getString());
							
							if ( listener != null ){
								
								listener.valueRead( new DHTPluginContactImpl( DHTPluginImpl.this, value.getOriginator()), mapValue( value ));
							}
						}
						
						public void
						wrote(
							final DHTTransportContact	contact,
							final DHTTransportValue		value )
						{
							// log.log( "Get: wrote " + value.getString() + " to " + contact.getString());
						}
						
						public void
						complete(
							boolean				_timeout )
						{
							// log.log( "Get: complete, timeout = " + _timeout );
							
							if ( listener != null ){
								
								listener.complete( _timeout );
							}
						}
					});
	}
	
	public void
	remove(
		final byte[]						key,
		final String						description,
		final DHTPluginOperationListener	listener )
	{
		dht.remove( 	key,
						description,
						new DHTOperationListener()
						{
							public void
							searching(
								DHTTransportContact	contact,
								int					level,
								int					active_searches )
							{
								String	indent = "";
								
								for (int i=0;i<level;i++){
									
									indent += "  ";
								}
								
								// log.log( indent + "Remove: level = " + level + ", active = " + active_searches + ", contact = " + contact.getString());
							}
							
							public void
							found(
								DHTTransportContact	contact )
							{
							}

							public void
							read(
								DHTTransportContact	contact,
								DHTTransportValue	value )
							{
								// log.log( "Remove: read " + value.getString() + " from " + contact.getString());
							}
							
							public void
							wrote(
								DHTTransportContact	contact,
								DHTTransportValue	value )
							{
								// log.log( "Remove: wrote " + value.getString() + " to " + contact.getString());
								if ( listener != null ){
									
									listener.valueWritten( new DHTPluginContactImpl( DHTPluginImpl.this, contact ), mapValue( value ));
								}
							}
							
							public void
							complete(
								boolean				timeout )
							{
								// log.log( "Remove: complete, timeout = " + timeout );
							
								if ( listener != null ){
								
									listener.complete( timeout );
								}
							}			
						});
	}
	
	public DHTPluginContact
	getLocalAddress()
	{
		return( new DHTPluginContactImpl( this, dht.getTransport().getLocalContact()));
	}
	
		// direct read/write support
	
	public void
	registerHandler(
		byte[]							handler_key,
		final DHTPluginTransferHandler	handler )
	{
		dht.getTransport().registerTransferHandler( 
				handler_key,
				new DHTTransportTransferHandler()
				{
					public byte[]
					handleRead(
						DHTTransportContact	originator,
						byte[]				key )
					{
						return( handler.handleRead( new DHTPluginContactImpl( DHTPluginImpl.this, originator ), key ));
					}
					
					public void
					handleWrite(
							DHTTransportContact	originator,
						byte[]				key,
						byte[]				value )
					{
						handler.handleWrite( new DHTPluginContactImpl( DHTPluginImpl.this, originator ), key, value );
					}
				});
	}
	
	public byte[]
	read(
		final DHTPluginProgressListener	listener,
		DHTPluginContact				target,
		byte[]							handler_key,
		byte[]							key,
		long							timeout )
	{
		try{
			return( dht.getTransport().readTransfer(
						new DHTTransportProgressListener()
						{
							public void
							reportSize(
								long	size )
							{
								listener.reportSize( size );
							}
							
							public void
							reportActivity(
								String	str )
							{
								listener.reportActivity( str );
							}
							
							public void
							reportCompleteness(
								int		percent )
							{
								listener.reportCompleteness( percent );
							}
						},
						((DHTPluginContactImpl)target).getContact(), 
						handler_key, 
						key, 
						timeout ));
			
		}catch( DHTTransportException e ){
			
			throw( new RuntimeException( e ));
		}
	}

	public DHT
	getDHT()
	{
		return( dht );
	}
	
	public void
	closedownInitiated()
	{
		storage_manager.exportContacts( dht );
	}

	public boolean
	isRecentAddress(
		String		address )
	{
		return( storage_manager.isRecentAddress( address ));
	}
	
	protected DHTPluginValue
	mapValue(
		final DHTTransportValue	value )
	{
		if ( value == null ){
			
			return( null );
		}
		
		return( new DHTPluginValueImpl(value));
	}
}
