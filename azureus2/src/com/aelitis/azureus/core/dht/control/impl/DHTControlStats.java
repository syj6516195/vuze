/*
 * Created on 31-Jan-2005
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

package com.aelitis.azureus.core.dht.control.impl;

import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.azureus.core.dht.router.*;
import com.aelitis.azureus.core.dht.transport.*;

/**
 * @author parg
 *
 */

public class 
DHTControlStats 
	implements DHTTransportFullStats
{
	private static final int	UPDATE_INTERVAL	= 10*1000;
	
	private DHTControlImpl		control;
	
	
	private Average	packets_in_average 		= Average.getInstance(UPDATE_INTERVAL, 12 );
	private Average	packets_out_average 	= Average.getInstance(UPDATE_INTERVAL, 12 );
	private Average	bytes_in_average 		= Average.getInstance(UPDATE_INTERVAL, 12 );
	private Average	bytes_out_average 		= Average.getInstance(UPDATE_INTERVAL, 12 );

	private DHTTransportStats	transport_snapshot;
	private long[]				router_snapshot;
	
	protected
	DHTControlStats(
		DHTControlImpl		_control )
	{
		control	= _control;
		
		transport_snapshot	= control.getTransport().getStats().snapshot();
		
		router_snapshot		= control.getRouter().getStats().getStats();
		
		Timer	timer = new Timer("DHTControl:stats");
		
		timer.addPeriodicEvent(
			UPDATE_INTERVAL,
			new TimerEventPerformer()
			{
				public void
				perform(
					TimerEvent	event )
				{
					update();
				}
			});
	}
	
	protected void
	update()
	{
		DHTTransport	transport 	= control.getTransport();
		
		DHTTransportStats	t_stats = transport.getStats().snapshot();
					
		packets_in_average.addValue( 
				t_stats.getPacketsReceived() - transport_snapshot.getPacketsReceived());
			
		packets_out_average.addValue( 
				t_stats.getPacketsSent() - transport_snapshot.getPacketsSent());
			
		bytes_in_average.addValue( 
				t_stats.getBytesReceived() - transport_snapshot.getBytesReceived());
			
		bytes_out_average.addValue( 
				t_stats.getBytesSent() - transport_snapshot.getBytesSent());
		
		transport_snapshot	= t_stats;
		
		router_snapshot	= control.getRouter().getStats().getStats();
	}
	
	public long
	getTotalBytesReceived()
	{
		return( transport_snapshot.getBytesReceived());
	}
	
	public long
	getTotalBytesSent()
	{
		return( transport_snapshot.getBytesSent());
	}
	
	public long
	getTotalPacketsReceived()
	{
		return( transport_snapshot.getPacketsReceived());
	}
	
	public long
	getTotalPacketsSent()
	{
		return( transport_snapshot.getPacketsSent());
		
	}
	
	public long
	getTotalPingsReceived()
	{
		return( transport_snapshot.getPings()[DHTTransportStats.STAT_RECEIVED]);
	}
	public long
	getTotalFindNodesReceived()
	{
		return( transport_snapshot.getFindNodes()[DHTTransportStats.STAT_RECEIVED]);
	}
	public long
	getTotalFindValuesReceived()
	{
		return( transport_snapshot.getFindValues()[DHTTransportStats.STAT_RECEIVED]);
	}
	public long
	getTotalStoresReceived()
	{
		return( transport_snapshot.getStores()[DHTTransportStats.STAT_RECEIVED]);
	}
	
		// averages
	
	public long
	getAverageBytesReceived()
	{
		return( bytes_in_average.getAverage());
	}
	
	public long
	getAverageBytesSent()
	{
		return( bytes_out_average.getAverage());	
	}
	
	public long
	getAveragePacketsReceived()
	{
		return( packets_in_average.getAverage());
	}
	
	public long
	getAveragePacketsSent()
	{
		return( packets_out_average.getAverage());
	}
	
	public long
	getDBValuesStored()
	{
		return( control.getDataBase().getSize());
	}
	
		// Router
	
	public long
	getRouterNodes()
	{
		return( router_snapshot[DHTRouterStats.ST_NODES]);
	}
	
	public long
	getRouterLeaves()
	{
		return( router_snapshot[DHTRouterStats.ST_LEAVES]);
	}
	
	public long
	getRouterContacts()
	{
		return( router_snapshot[DHTRouterStats.ST_CONTACTS]);
	}
	
	public String
	getString()
	{
		return(	"transport:" + 
				getTotalBytesReceived() + "," +
				getTotalBytesSent() + "," +
				getTotalPacketsReceived() + "," +
				getTotalPacketsSent() + "," +
				getTotalPingsReceived() + "," +
				getTotalFindNodesReceived() + "," +
				getTotalFindValuesReceived() + "," +
				getTotalStoresReceived() + "," +
				getAverageBytesReceived() + "," +
				getAverageBytesSent() + "," +
				getAveragePacketsReceived() + "," +
				getAveragePacketsSent() + 
				", router:" +
				getRouterNodes() + "," +
				getRouterLeaves() + "," +
				getRouterContacts() + 
				",database:" +
				getDBValuesStored());
	}
}
