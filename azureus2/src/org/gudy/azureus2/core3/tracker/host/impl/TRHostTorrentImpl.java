/*
 * File    : TRHostTorrentImpl.java
 * Created : 26-Oct-2003
 * By      : stuff
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.tracker.host.impl;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.tracker.server.*;
import org.gudy.azureus2.core3.torrent.*;

public class 
TRHostTorrentImpl
	implements TRHostTorrent 
{
	protected TRHostImpl		host;
	protected TRTrackerServer	server;
	protected TOTorrent			torrent;
	protected int				port;
	
	protected int				status	= TS_STOPPED;
	
	protected long				total_uploaded;
	protected long				total_downloaded;
	
	protected long				last_uploaded;
	protected long				last_downloaded;

		//average over 10 periods, update every period.

	protected Average			average_uploaded		= Average.getInstance(TRHostImpl.STATS_PERIOD_SECS*1000,TRHostImpl.STATS_PERIOD_SECS*10);
	protected Average			average_downloaded		= Average.getInstance(TRHostImpl.STATS_PERIOD_SECS*1000,TRHostImpl.STATS_PERIOD_SECS*10);
	
	protected
	TRHostTorrentImpl(
		TRHostImpl		_host,
		TRTrackerServer	_server,
		TOTorrent		_torrent,
		int				_port )
	{
		host		= _host;
		server		= _server;
		torrent		= _torrent;
		port		= _port;
	}
	
	protected int
	getPort()
	{
		return( port );
	}
	
	public synchronized void
	start()
	{
		try{
		
			server.permit( torrent.getHash());
		
			status = TS_STARTED;
			
			host.hostTorrentStateChange( this );
			
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}
	}
	
	public synchronized void
	stop()
	{
		try{
			
			server.deny( torrent.getHash());
		
			status = TS_STOPPED;
	
			host.hostTorrentStateChange( this );
			
		}catch( TOTorrentException e ){
			
				e.printStackTrace();
		}
	}
	
	public synchronized void
	remove()
	{
		stop();
		
		host.remove( this );
	}
	
	public int
	getStatus()
	{
		return( status );
	}
	
	public TOTorrent
	getTorrent()
	{
		return( torrent );
	}

	public TRHostPeer[]
	getPeers()
	{
		try{
		
			TRTrackerServerPeer[]	peers = server.getPeers( torrent.getHash());
		
			if ( peers != null ){
			
				TRHostPeer[]	res = new TRHostPeer[peers.length];
				
				for (int i=0;i<peers.length;i++){
					
					res[i] = new TRHostPeerImpl(peers[i]);
				}
				
				return( res );
			}
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}
		
		return( new TRHostPeer[0] );
	}	
	
	public int
	getAnnounceCount()
	{
		try{
		
			TRTrackerServerTorrentStats	stats = server.getStats( torrent.getHash());
		
			if ( stats != null ){
			
				return( stats.getAnnounceCount());
			}
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}
		
		return( 0 );
	}
	
	protected void
	updateStats()
	{
		try{
		
			// System.out.println( "stats update ");
			
			TRTrackerServerTorrentStats	stats = server.getStats( torrent.getHash());
		
			if ( stats != null ){
			
				long	current_uploaded 	= stats.getUploaded();
				long	current_downloaded 	= stats.getDownloaded();
				
					// bit crap this as the maintained values are only for *active*
					// peers - stats are lost when they disconnect
					
				long ul_diff = current_uploaded - last_uploaded;
					
				if ( ul_diff > 0 ){
			
					total_uploaded += ul_diff;
				
				}else{
				
					ul_diff = 0;
				}
				
				average_uploaded.addValue((int)ul_diff);
				
				last_uploaded = current_uploaded;
				
				long dl_diff = current_downloaded - last_downloaded;
					
				if ( dl_diff > 0 ){
				
					total_downloaded += dl_diff;
					
				}else{
					
					dl_diff = 0;
				}
				
				average_downloaded.addValue((int)dl_diff);
				
				last_downloaded = current_downloaded;
				
				// System.out.println( "tot_up = " + total_uploaded + ", tot_down = " + total_downloaded);
			}
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}	
	}
	
	public long
	getTotalUploaded()
	{
		return( total_uploaded );
	}
	
	public long
	getTotalDownloaded()
	{
		return( total_downloaded );
	}	
	
	public long
	getAverageUploaded()
	{
		return( average_uploaded.getAverage() );
	}
	
	public long
	getAverageDownloaded()
	{
		return( average_downloaded.getAverage() );
	}
}
