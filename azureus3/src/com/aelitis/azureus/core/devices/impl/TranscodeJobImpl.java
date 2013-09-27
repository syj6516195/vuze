/*
 * Created on Feb 6, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.devices.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadRemovalVetoException;
import org.gudy.azureus2.plugins.download.DownloadWillBeRemovedListener;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.peers.PeerManagerStats;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.devices.TranscodeAnalysisListener;
import com.aelitis.azureus.core.devices.TranscodeJob;
import com.aelitis.azureus.core.devices.TranscodeProfile;
import com.aelitis.azureus.core.devices.TranscodeException;
import com.aelitis.azureus.core.devices.TranscodeActionVetoException;
import com.aelitis.azureus.core.devices.TranscodeTarget;
import com.aelitis.azureus.core.download.DiskManagerFileInfoFile;
import com.aelitis.azureus.core.download.DiskManagerFileInfoURL;
import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.util.ImportExportUtils;

public class 
TranscodeJobImpl 
	implements TranscodeJob, DownloadWillBeRemovedListener
{
	private static final int TRANSCODE_OK_DL_PERCENT	= 90;
	
	private TranscodeQueueImpl		queue;
	private TranscodeTarget			target;
	private TranscodeProfile		profile;
	private DiskManagerFileInfo		file;
	private TranscodeFileImpl		transcode_file;
	
	private boolean					is_stream;
	private volatile InputStream	stream;
	private AESemaphore				stream_sem = new AESemaphore( "TJ:s" );
	
	private int						transcode_requirement;
	
	private int						state 				= ST_QUEUED;
	private int						percent_complete	= 0;
	private int						eta					= Integer.MAX_VALUE;
	private String					error;
	private long					started_on;
	private long					paused_on;
	private long					process_time;
	
	private boolean					use_direct_input;
	private boolean					prefer_direct_input;
	
	private boolean					auto_retry_enabled	= true;
	private boolean					auto_retry;
	private int						auto_retry_count;
	
	private Download				download;
	private boolean					download_ok;
	
	protected
	TranscodeJobImpl(
		TranscodeQueueImpl		_queue,
		TranscodeTarget			_target,
		TranscodeProfile		_profile,
		DiskManagerFileInfo		_file,
		boolean					_add_stopped,
		int						_transcode_requirement,
		boolean					_is_stream )
	
		throws TranscodeException
	{
		queue					= _queue;
		target					= _target;
		profile					= _profile;
		file					= _file;
		transcode_requirement	= _transcode_requirement;
		is_stream				= _is_stream;
		
		if ( _add_stopped ){
			
			state = ST_STOPPED;
		}
		
		init();
	}
	
	protected
	TranscodeJobImpl(
		TranscodeQueueImpl		_queue,
		Map<String,Object>		map )
	
		throws IOException, TranscodeException
	{
		queue	= _queue;
		
		state = ImportExportUtils.importInt( map, "state" );
		
		if ( state == ST_RUNNING ){
			
			state = ST_QUEUED;
		}
		
		error = ImportExportUtils.importString( map, "error", null );
		
		String	target_id = ImportExportUtils.importString( map, "target" );
		
		target = queue.lookupTarget( target_id );
		
		String	profile_id = ImportExportUtils.importString( map, "profile" );
		
		profile = queue.lookupProfile( profile_id );
		
		String file_str = ImportExportUtils.importString( map, "file" );
		
		if ( file_str == null ){
			
			byte[] dl_hash = ByteFormatter.decodeString( ImportExportUtils.importString( map, "dl_hash" ));
			
			int file_index = ImportExportUtils.importInt( map, "file_index" );
			
			file = queue.lookupFile( dl_hash, file_index );
		}else{
		
			file = new DiskManagerFileInfoFile( new File( file_str ));
		}
		
		transcode_requirement	= ImportExportUtils.importInt( map, "trans_req", TranscodeTarget.TRANSCODE_UNKNOWN );
		
		auto_retry_enabled = ImportExportUtils.importBoolean( map, "ar_enable", true );
		
		prefer_direct_input = ImportExportUtils.importBoolean( map, "pdi", false );
		
		init();
	}

	protected Map<String,Object>
	toMap()
	
		throws IOException
	{
		try{
			Map<String,Object> map = new HashMap<String, Object>();
			
			synchronized( this ){
				
				ImportExportUtils.exportInt( map, "state", state );
				ImportExportUtils.exportString( map, "error", error );
				
				ImportExportUtils.exportString( map, "target", target.getID());
				
				ImportExportUtils.exportString( map, "profile", profile.getUID());
				
				try{
					Download download = file.getDownload();
					
					ImportExportUtils.exportString( map, "dl_hash", ByteFormatter.encodeString( download.getTorrent().getHash()));
	
					ImportExportUtils.exportInt( map, "file_index", file.getIndex());
	
				}catch( DownloadException e ){
					
						// external file
					
					ImportExportUtils.exportString( map, "file", file.getFile().getAbsolutePath());
				}
			
				ImportExportUtils.exportInt( map, "trans_req", transcode_requirement );
					
				ImportExportUtils.exportBoolean( map, "ar_enable", auto_retry_enabled );
				
				ImportExportUtils.exportBoolean( map, "pdi", prefer_direct_input );
			}
			
			return( map );
			
		}catch( Throwable e ){
			
			throw( new IOException( "Export failed: " + Debug.getNestedExceptionMessage(e)));
		}
	}
	
	protected void
	init()
	
		throws TranscodeException
	{
		transcode_file = ((DeviceImpl)target.getDevice()).allocateFile( profile, getTranscodeRequirement() == TranscodeTarget.TRANSCODE_NEVER, file, true );
		
		try{
			download = file.getDownload();
			
			if ( download != null ){
				
				download.addDownloadWillBeRemovedListener( this );
			}
		}catch( Throwable e ){
		}
		
		updateStatus( false );
	}
	
	protected void
	updateStatus()
	{
		updateStatus( true );
	}
	
	protected void
	updateStatus(
		boolean	report_change )
	{
		synchronized( this ){
			
			if ( download_ok ){
				
				return;
			}
			
			long	downloaded 	= file.getDownloaded();
			long	length		= file.getLength();
			
			if ( download == null || downloaded == length ){
				
				download_ok = true;
				
			}else{

				Torrent torrent = download.getTorrent();
						
				if ( 	PlatformTorrentUtils.isContent( torrent, false ) || 
						PlatformTorrentUtils.getContentNetworkID( PluginCoreUtils.unwrap( torrent )) == ContentNetwork.CONTENT_NETWORK_VHDNL ){
					
					download_ok = true;
					
				}else{
					
					int	percent_done = (int)( 100*downloaded/length );
					
					if ( percent_done >= TRANSCODE_OK_DL_PERCENT ){
						
						download_ok = true;
						
					}else{
						
						PeerManager pm = download.getPeerManager();
						
						if ( pm != null ){
							
							PeerManagerStats stats = pm.getStats();
							
							int connected_seeds 	= stats.getConnectedSeeds();
							int connected_leechers	= stats.getConnectedLeechers();
							
							
							if ( connected_seeds > 10 && connected_seeds > connected_leechers ){
								
								download_ok = true;
							}
						}else{
							
							int state = download.getState();
							
							if ( state == Download.ST_STOPPED ){
								
								try{
									download.restart();
									
								}catch( Throwable e ){
									
									Debug.out( e );
								}
							}
						}
					}
				}
			}
		}
		
		if ( download_ok && report_change ){
			
			queue.jobChanged( this, true, false );
		}
	}
	
	public long
	getDownloadETA()
	{
		if ( download_ok ){
			
			return( 0 );
		}
		
		if ( file.getDownloaded() == file.getLength()){
			
			return( 0 );
		}

		if ( file.isSkipped() || file.isDeleted()){
				
			return( Long.MAX_VALUE );
		}
			
		try{
			long	eta = PluginCoreUtils.unwrap( download ).getStats().getETA();
			
			if ( eta < 0 ){
				
				return( Long.MAX_VALUE );
			}
			
			long adjusted = eta*100/TRANSCODE_OK_DL_PERCENT;
			
			if ( adjusted == 0 ){
				
				adjusted = 1;
			}
			
			return( adjusted );
			
		}catch( Throwable e ){
			
			Debug.out( e );
			
			return( Long.MAX_VALUE );
		}
	}
	
	protected boolean
	canUseDirectInput()
	{
		if ( file instanceof DiskManagerFileInfoURL ){
			
			return( true );
		}
		
		long	length = file.getLength();

		return( file.getDownloaded() == length &&
				file.getFile().length() == length );
	}
	
	protected boolean
	useDirectInput()
	{
		synchronized( this ){

			return( use_direct_input || 
					( getPreferDirectInput() && canUseDirectInput()));
		}
	}
	
	protected void
	setUseDirectInput()
	{
		synchronized( this ){

			use_direct_input = true;
		}
	}
	
	public void
	setPreferDirectInput(
		boolean		prefer )
	{
		synchronized( this ){
		
			prefer_direct_input = prefer;
		}
	}
	
	public boolean
	getPreferDirectInput()
	{
		synchronized( this ){
		
			return( prefer_direct_input );
		}
	}
	
	protected void
	setAutoRetry(
		boolean		_auto_retry )
	{
		synchronized( this ){

			if ( _auto_retry ){
				
				auto_retry 	= true;
				
				auto_retry_count++;
				
			}else{
				
				auto_retry = false;
			}
		}
	}
	
	protected boolean
	isAutoRetry()
	{
		synchronized( this ){
			
			return( auto_retry );
		}
	}
	
	protected int
	getAutoRetryCount()
	{
		synchronized( this ){

			return( auto_retry_count );
		}
	}
	
	public void
	setEnableAutoRetry(
		boolean		enabled )
	{
		auto_retry_enabled = enabled;
	}
	
	public boolean 
	getEnableAutoRetry() 
	{
		return( auto_retry_enabled );
	}
	
	protected boolean
	isStream()
	{
		return( is_stream );
	}
	
	protected void
	setStream(
		InputStream		_stream )
	{
		stream		= _stream;
		
		stream_sem.releaseForever();
	}
	
	protected InputStream
	getStream(
		int		wait_for_millis )
	
		throws IOException
	{
		if ( state == ST_FAILED ){
			
			throw( new IOException( "Transcode job failed: " + error ));
			
		}else if ( state == ST_CANCELLED ){
			
			throw( new IOException( "Transcode job cancelled" ));

		}else if ( state == ST_REMOVED ){
			
			throw( new IOException( "Transcode job removed" ));
		}
		
		stream_sem.reserve( wait_for_millis );
		
		return( stream );
	}
	
	public void 
	downloadWillBeRemoved(
		Download 	download )

		throws DownloadRemovalVetoException
	{
		if ( queue.getIndex( this ) == 0 || state == ST_COMPLETE ){
			
			download.removeDownloadWillBeRemovedListener( this );
			
		}else{
			
			throw( new DownloadRemovalVetoException( 
					MessageText.getString( "devices.xcode.remove.vetoed",
						new String[]{ download.getName()})));
		}
	}
	
	public String
	getName()
	{
		if ( download != null ){
		
			if ( download.getDiskManagerFileInfo().length == 1 ){
				
				return( download.getName());
			}
			
			return( download.getName() + ": " + file.getFile().getName());
			
		}else{
			
			return( file.getFile().getName());
		}
	}
	
	protected void
	reset()
	{
		state 				= ST_QUEUED;
		error 				= null;
		percent_complete	= 0;
		eta					= Integer.MAX_VALUE;
	}
	
	protected void
	starts()
	{
		synchronized( this ){
		
			started_on 	= SystemTime.getMonotonousTime();
			paused_on	= 0;

				// this is for an Azureus restart with a paused job - we don't want to change the
				// state as we want it to re-pause...
			
			if ( state != ST_PAUSED ){
			
				state = ST_RUNNING;				
			}
		}
		
		queue.jobChanged( this, false, true );
	}
	
	protected void
	failed(
		Throwable	e )
	{
		queue.log( "Transcode failed", e );
		
		synchronized( this ){
			
			if ( state != ST_STOPPED ){
			
				state = ST_FAILED;
			
				error = Debug.getNestedExceptionMessage( e );
				
					// process_time filled with negative pause time, so add to it
				
				process_time += SystemTime.getMonotonousTime() - started_on;
				
				started_on = paused_on = 0;
			}
		}
		
		queue.jobChanged( this, false, true );
	}
	
	protected void
	complete()
	{
		synchronized( this ){
		
			state = ST_COMPLETE;
			
				// process_time filled with negative pause time, so add to it
			
			process_time += SystemTime.getMonotonousTime() - started_on;
			
			started_on = paused_on = 0;
		}
		
		if ( download != null ){
			
			download.removeDownloadWillBeRemovedListener( this );
		}
		
		transcode_file.setComplete( true );
		
		queue.jobChanged( this, false, false );
	}
	
	protected void
	updateProgress(
		int		_done,
		int		_eta )
	{
		if ( percent_complete != _done || eta != _eta){
		
			percent_complete	= _done;
			eta					= _eta;
			
			queue.jobChanged( this, false, false );
		}
	}
	
	public TranscodeTarget
	getTarget()
	{
		return( target );
	}
	
	public int
	getTranscodeRequirement()
	{
		if ( transcode_requirement >= 0 ){
			
			return( transcode_requirement );
		}
		
		return( getDevice().getTranscodeRequirement());
	}
	
	public void
	analyseNow(
		TranscodeAnalysisListener	listener )
	
		throws TranscodeException
	{
		queue.analyse( this, listener );
	}
	
	protected DeviceImpl
	getDevice()
	{
		return((DeviceImpl)target );
	}
	
	public TranscodeProfile
	getProfile()
	{
		return( profile );
	}
	
	public DiskManagerFileInfo
	getFile()
	{
		return( file );
	}
	
	public TranscodeFileImpl 
	getTranscodeFile() 
	{
		return( transcode_file );
	}
	
	public int
	getIndex()
	{
		return( queue.getIndex( this ));
	}
	
	public int
	getState()
	{
		return( state );
	}
	
	public int
	getPercentComplete()
	{
		return( percent_complete );
	}
	
	public long
	getETASecs()
	{
		if ( eta <= 0 ){
			
			return( 0 );
			
		}else if ( eta == Integer.MAX_VALUE ){
			
			return( Long.MAX_VALUE );
			
		}else{
			
			return( eta );
		}
	}
	
	public String
	getETA()
	{
		if ( eta < 0 ){
			
			return( null );
			
		}else if ( eta == Integer.MAX_VALUE ){
			
			return( Constants.INFINITY_STRING );
			
		}else{
			
			return( TimeFormatter.format( eta ));
		}
	}
	
	public String
	getError()
	{
		return( error );
	}
	
	public boolean
	canPause()
	{
		synchronized( this ){

			return( !use_direct_input );
		}
	}
	
	public void
	pause()
	{
		synchronized( this ){
			
			if ( use_direct_input ){
				
				return;
			}
			
			if ( state == ST_RUNNING ){
		
				state = ST_PAUSED;
				
				paused_on = SystemTime.getMonotonousTime();
				
			}else{
				
				return;
			}
		}
		
		queue.jobChanged( this, false, true );
	}
	
	public void
	resume()
	{
		synchronized( this ){

			if ( state == ST_PAUSED ){
				
				state = ST_RUNNING;

				if ( paused_on > 0 && started_on > 0 ){
					
					process_time -= SystemTime.getMonotonousTime() - paused_on;
				}
			}else{
				
				return;
			}
		}
		
		queue.jobChanged( this, false, true );
	}
	
	public void
	queue()
	{
		boolean	do_resume;
	
		synchronized( this ){

			do_resume = state == ST_PAUSED;
		}
		
		if ( do_resume ){
			
			resume();
			
			return;
		}

		synchronized( this ){
			
			if ( state != ST_QUEUED ){
		
				if ( 	state == ST_RUNNING ||
						state == ST_PAUSED ){
					
					stop();
				}
								
				reset();
				
					// manual start, scrub error details
				
				use_direct_input 	= false;
				auto_retry			= false;
				auto_retry_count	= 0;
				is_stream			= false;

			}else{
				
				return;
			}
		}
		
		queue.jobChanged( this, true, true);
	}
	
	public void
	stop()
	{
		synchronized( this ){
			
			if ( state != ST_STOPPED ){
		
				state = ST_STOPPED;
				
				process_time = 0;

				started_on = 0;
				
			}else{
				
				return;
			}
		}
		
		queue.jobChanged( this, true, true );
	}
	
	public void
	remove()
	
		throws TranscodeActionVetoException
	{
		queue.remove( this, false );
	}
	
	public void
	removeForce()
	{
		try{
			queue.remove( this, true );
			
		}catch( TranscodeActionVetoException e ){
			
			Debug.out( e );
		}
	}
	
	protected void
	destroy()
	{
		boolean	delete_file;
		
		synchronized( this ){
			
			delete_file = state != ST_COMPLETE;
			
			state = ST_REMOVED;
		}
		
		if ( delete_file && !isStream()){
			
			try{
				transcode_file.delete( true );
				
			}catch( Throwable e ){
				
				queue.log( "Faile to destroy job", e );
			}
		}
	}
	
	public void 
	moveUp() 
	{
		queue.moveUp( this );
	}
	
	public void 
	moveDown() 
	{
		queue.moveDown( this );
	}
	
	public long
	getProcessTime()
	{
		if ( state == ST_COMPLETE ){
		
			return process_time;
		}
		
		if ( started_on == 0 ){
			
			if (  process_time > 0 ){
				
				return process_time;
			}
			
			return 0;
		}
			// process_time filled with pause
		
		return SystemTime.getMonotonousTime() - started_on + process_time;
	}
		
	public void
	generate(
		IndentWriter		writer )
	{
		writer.println( "target=" + target.getID() + ", profile=" + profile.getName() + ", file=" + file );
		writer.println( "tfile=" + transcode_file.getString());
		writer.println( "stream=" + is_stream + ", state=" + state + ", treq=" + transcode_requirement + ", %=" + percent_complete + ", error=" + error );
	}
}
