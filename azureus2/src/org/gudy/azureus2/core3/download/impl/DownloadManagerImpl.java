/*
 * File    : DownloadManagerImpl.java
 * Created : 19-Oct-2003
 * By      : parg
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

package org.gudy.azureus2.core3.download.impl;
/*
 * Created on 30 juin 2003
 *
 */
 
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.net.*;


import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.download.*;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.plugins.download.DownloadAnnounceResult;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;

/**
 * @author Olivier
 * 
 */

public class 
DownloadManagerImpl 
	implements DownloadManager
{
		// DownloadManager listeners
	
	private static final int LDT_STATECHANGED		= 1;
	private static final int LDT_DOWNLOADCOMPLETE	= 2;
	private static final int LDT_COMPLETIONCHANGED = 3;
	private static final int LDT_POSITIONCHANGED = 4;
	
	private ListenerManager	listeners 	= ListenerManager.createManager(
			"DMM:ListenDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		_listener,
					int			type,
					Object		value )
				{
					DownloadManagerListener	listener = (DownloadManagerListener)_listener;
					
					if ( type == LDT_STATECHANGED ){
						
						listener.stateChanged(DownloadManagerImpl.this, ((Integer)value).intValue());
						
					}else if ( type == LDT_DOWNLOADCOMPLETE ){
						
						listener.downloadComplete(DownloadManagerImpl.this);

					}else if ( type == LDT_COMPLETIONCHANGED ){
						listener.completionChanged(DownloadManagerImpl.this, ((Boolean)value).booleanValue());

					}else if ( type == LDT_POSITIONCHANGED ){
						listener.positionChanged(DownloadManagerImpl.this,
						                         ((Integer)value).intValue(), position);
					}
				}
			});		
	
		// TrackerListeners
	
	private static final int LDT_TL_ANNOUNCERESULT		= 1;
	private static final int LDT_TL_SCRAPERESULT		= 2;
	
	private ListenerManager	tracker_listeners 	= ListenerManager.createManager(
			"DMM:TrackerListenDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		_listener,
					int			type,
					Object		value )
				{
					DownloadManagerTrackerListener	listener = (DownloadManagerTrackerListener)_listener;
					
					if ( type == LDT_TL_ANNOUNCERESULT ){
						
						listener.announceResult((TRTrackerAnnouncerResponse)value);
						
					}else if ( type == LDT_TL_SCRAPERESULT ){
						
						listener.scrapeResult((TRTrackerScraperResponse)value);
					}
				}
			});	

	// PeerListeners
	
	private static final int LDT_PE_PEER_ADDED		= 1;
	private static final int LDT_PE_PEER_REMOVED	= 2;
	private static final int LDT_PE_PIECE_ADDED		= 3;
	private static final int LDT_PE_PIECE_REMOVED	= 4;
	private static final int LDT_PE_PM_ADDED		= 5;
	private static final int LDT_PE_PM_REMOVED		= 6;
	
	private ListenerManager	peer_listeners 	= ListenerManager.createAsyncManager(
			"DMM:PeerListenDispatcher",
			new ListenerManagerDispatcher()
			{
				public void
				dispatch(
					Object		_listener,
					int			type,
					Object		value )
				{
					DownloadManagerPeerListener	listener = (DownloadManagerPeerListener)_listener;
					
					if ( type == LDT_PE_PEER_ADDED ){
						
						listener.peerAdded((PEPeer)value);
						
					}else if ( type == LDT_PE_PEER_REMOVED ){
						
						listener.peerRemoved((PEPeer)value);
						
					}else if ( type == LDT_PE_PIECE_ADDED ){
						
						listener.pieceAdded((PEPiece)value);
						
					}else if ( type == LDT_PE_PIECE_REMOVED ){
						
						listener.pieceRemoved((PEPiece)value);
						
					}else if ( type == LDT_PE_PM_ADDED ){
						
						listener.peerManagerAdded((PEPeerManager)value);
						
					}else if ( type == LDT_PE_PM_REMOVED ){
						
						listener.peerManagerRemoved((PEPeerManager)value);
					}
				}
			});	
	
	private AEMonitor	peer_listeners_mon	= new AEMonitor( "DownloadManager:PL" );
	
	private List	current_peers 	= new ArrayList();
	private List	current_pieces	= new ArrayList();
  
	private DownloadManagerStatsImpl	stats;
	
	private boolean		persistent;
	/**
	 * forceStarted torrents can't/shouldn't be automatically stopped
	 */
	private boolean 	forceStarted;
	/**
	 * Only seed this torrent. Never download or allocate<P>
	 * Current Implementation:
	 * - implies that the user completed the download at one point
	 * - Checks if there's Data Missing when torrent is done (or torrent load)
	 *
	 * Perhaps a better name would be "bCompleted"
	 */
	protected boolean onlySeeding;
	
	private int 		state = -1;
  
	private int prevState = -1;

	private String errorDetail;

	private GlobalManager globalManager;

	private String torrentFileName;

	private int nbPieces;
	
	private String	display_name;
	
	
		// torrent_save_dir is always the directory within which torrent data is being saved. That is, it
		// never includes the torrent data itself. In particular it DOESN'T include the dir name of a
		// non-simple torrent
	
	private String	torrent_save_dir;
	
		// torrent_save_file is the top level file corresponding to the torrent save data location. This
		// will be the file name for simple torrents and the folder name for non-simple ones
	
	private String	torrent_save_file;
	
  
	// Position in Queue
	private int position = -1;
	
  
	//Used when trackerConnection is not yet created.
	// private String trackerUrl;

	
	private	DownloadManagerState		download_manager_state;
	
	private TOTorrent		torrent;
	private String 			torrent_comment;
	private String 			torrent_created_by;
	
	private TRTrackerAnnouncer 			tracker_client;
	private TRTrackerAnnouncerListener		tracker_client_listener;
	
	private long						scrape_random_seed	= SystemTime.getCurrentTime();
	
	private DiskManager 			diskManager;
	private DiskManagerListener		disk_manager_listener;
  
	private PEPeerManager 			peerManager;
	private PEPeerManagerListener	peer_manager_listener;

	private HashMap data;
  
	private boolean data_already_allocated = false;
  
	private long	creation_time	= SystemTime.getCurrentTime();
   
	// Only call this with STATE_QUEUED, STATE_WAITING, or STATE_STOPPED unless you know what you are doing
	
	
	public 
	DownloadManagerImpl(
		GlobalManager 	_gm,
		byte[]			_torrent_hash,
		String 			_torrentFileName, 
		String 			_torrent_save_dir,
		String			_torrent_save_file,
		int   			_initialState,
		boolean			_persistent,
		boolean			_recovered,
		boolean			_open_for_seeding ) 
	{
		persistent	= _persistent;
  	
		stats = new DownloadManagerStatsImpl( this );
  	
		globalManager = _gm;
	
		stats.setMaxUploads( COConfigurationManager.getIntParameter("Max Uploads") );
	 
		forceStarted = false;
	
		torrentFileName = _torrentFileName;
	
		torrent_save_dir	= _torrent_save_dir;	
		torrent_save_file	= _torrent_save_file;
	
			// readTorrent adjusts the save dir and file to be sensible values
			
		readTorrent( _torrent_hash, persistent && !_recovered, _open_for_seeding );
		
			// must be after readTorrent, so that any listeners have a TOTorrent
		
		if (state == -1){
			
			setState( _initialState );
		}
	}

  public void 
  initialize() 
  {
    setState( STATE_INITIALIZING );
         	
    // If we only want to seed, do a quick check first (before we create the diskManager, which allocates diskspace)
    if (onlySeeding && !filesExist()) {
      // If the user wants to re-download the missing files, they must
      // do a re-check, which will reset the onlySeeding flag.
      return;
    }

    if ( torrent == null ) {
    	
      setFailed();
      
      return;
    }

    errorDetail = "";
    
    if ( state == STATE_WAITING || state == STATE_ERROR ){
    	
      return;
    }
    
    try{
      if ( tracker_client != null ){

        tracker_client.destroy();
      }

      tracker_client = TRTrackerAnnouncerFactory.create( torrent, download_manager_state.getNetworks());
    
      tracker_client.setTrackerResponseCache( download_manager_state.getTrackerResponseCache());

      tracker_client_listener = new TRTrackerAnnouncerListener() {
        public void receivedTrackerResponse(TRTrackerAnnouncerResponse	response) {
          PEPeerManager pm = peerManager;
          if ( pm != null ) {
            pm.processTrackerResponse( response );
          }

          tracker_listeners.dispatch( LDT_TL_ANNOUNCERESULT, response );
        }

        public void urlChanged(String url, boolean explicit) {
          if ( explicit ){
            checkTracker( true );
          }
        }

        public void urlRefresh() {
          checkTracker( true );
        }
      };

      tracker_client.addListener( tracker_client_listener );

      if ( getState() != STATE_ERROR ){
      	
      		// we need to set the state to "initialized" before kicking off the disk manager
      		// initialisation as it should only report its status while in the "initialized"
      		// state (see getState for how this works...)
      	
      	setState( STATE_INITIALIZED );
          
      	initializeDiskManager();
      }


    }catch( TRTrackerAnnouncerException e ){
 		
    	setFailed( e );
			 
    }
  }

  public void 
  startDownload() 
  {
	setState( STATE_DOWNLOADING );
	
	PEPeerManager temp = PEPeerManagerFactory.create(this, tracker_client, diskManager);

	peer_manager_listener = 	
		new PEPeerManagerListener()
		{
			public void	stateChanged(	int	new_state ){}
      public void peerAdded( PEPeerManager manager, PEPeer peer ) {}
      public void peerRemoved( PEPeerManager manager, PEPeer peer ) {}
		};
		
	temp.addListener( peer_manager_listener );
		
	temp.start();
	
	try{
		peer_listeners_mon.enter();
		
		peerManager = temp;		// delay this so peerManager var not available to other threads until it is started
	
		peer_listeners.dispatch( LDT_PE_PM_ADDED, temp );
	}finally{
		
		peer_listeners_mon.exit();
	}
	
	tracker_client.update( true );
  }

	private void 
	readTorrent(
		byte[]		torrent_hash,		// can be null for initial torrents
		boolean		new_torrent,		// probably equivalend to (torrent_hash == null)????
		boolean		open_for_seeding )
	{
		display_name				= torrentFileName;	// default if things go wrong decoding it
		//trackerUrl				= "";
		torrent_comment				= "";
		torrent_created_by			= "";
		nbPieces					= 0;
		
		try {

			 download_manager_state	= 
				 	DownloadManagerStateImpl.getDownloadState(
				 			this, torrentFileName, torrent_hash );
			 
			 torrent	= download_manager_state.getTorrent();
			 
			 LocaleUtilDecoder	locale_decoder = LocaleUtil.getSingleton().getTorrentEncoding( torrent );
					 
			 	// if its a simple torrent and an explicit save file wasn't supplied, use
			 	// the torrent name itself
			 
			 display_name = locale_decoder.decodeString( torrent.getName());
             
			 display_name = FileUtil.convertOSSpecificChars( display_name );
		
			 	// now we know if its a simple torrent or not we can make some choices about
			 	// the save dir and file. On initial entry the save_dir will have the user-selected
			 	// save location and the save_file will be null
			 
			 File	save_dir_file	= new File( torrent_save_dir );
			 
			 // System.out.println( "before: " + torrent_save_dir + "/" + torrent_save_file );
			 
			 	// if save file is non-null then things have already been sorted out
			 
			 if ( torrent_save_file == null ){
			 		 	
			 	if ( torrent.isSimpleTorrent()){
			 		
			 			// if target save location is a directory then we use that as the save
			 			// dir and use the torrent display name as the target. Otherwise we
			 			// use the file name
			 		
			 		if ( save_dir_file.exists()){
			 			
			 			if ( save_dir_file.isDirectory()){
			 				
			 				torrent_save_file	= display_name;
			 				
			 			}else{
			 				
			 				torrent_save_dir	= save_dir_file.getParent().toString();
			 				
			 				torrent_save_file	= save_dir_file.getName();
			 			}
			 		}else{
			 			
			 				// doesn't exist, assume it refers directly to the file
			 			
		 				torrent_save_dir	= save_dir_file.getParent().toString();
		 				
		 				torrent_save_file	= save_dir_file.getName(); 			
			 		}
			 		
			 	}else{
			 	
			 			// torrent is a folder. It is possible that the natural location
			 			// for the folder is X/Y and that in fact 'Y' already exists and
			 			// has been selected. If ths is the case the select X as the dir and Y
			 			// as the file name
			 		
			 		if ( save_dir_file.exists()){
			 			
			 			if ( !save_dir_file.isDirectory()){
			 				
			 				throw( new Exception( "'" + torrent_save_dir + "' is not a directory" ));
			 			}
			 			
			 			if ( save_dir_file.getName().equals( display_name )){
			 				
			 				torrent_save_dir	= save_dir_file.getParent().toString();
			 			}
			 		}
			 		
			 		torrent_save_file	= display_name;		
			 	}
			 }
			 
			 // System.out.println( "after: " + torrent_save_dir + "/" + torrent_save_file );

			 save_dir_file	= torrent.isSimpleTorrent()?new File( torrent_save_dir ):new File( torrent_save_dir, torrent_save_file );

			 if ( !save_dir_file.exists()){
			 	
			 		// if this isn't a new torrent then we treat the absence of the enclosing folder
			 		// as a fatal error. This is in particular to solve a problem with the use of
			 		// externally mounted torrent data on OSX, whereby a re-start with the drive unmounted
			 		// results in the creation of a local diretory in /Volumes that subsequently stuffs
			 		// up recovery when the volume is mounted
			 	
			 		// changed this to only report the error on non-windows platforms 
			 	
			 	if ( !(new_torrent || Constants.isWindows )){
			 		
			 		throw( new Exception( MessageText.getString("DownloadManager.error.datamissing") + " " + save_dir_file.toString()));
			 	}
			 }	
			 
			 	// if this is a newly introduced torrent trash the tracker cache. We do this to
			 	// prevent, say, someone publishing a torrent with a load of invalid cache entries
			 	// in it and a bad tracker URL. This could be used as a DOS attack

			 if ( new_torrent ){
			 	
			 	download_manager_state.setTrackerResponseCache( new HashMap());
			 	
			 		// also remove resume data incase someone's published a torrent with resume
			 		// data in it
			 	
			 	if ( open_for_seeding ){
			 		
			 		DiskManagerFactory.setTorrentResumeDataNearlyComplete(download_manager_state, torrent_save_dir, torrent_save_file );

			 	}else{
			 		
			 		download_manager_state.clearResumeData();
			 	}
			 }
			 
	         
			 //trackerUrl = torrent.getAnnounceURL().toString();
         
			 torrent_comment = locale_decoder.decodeString(torrent.getComment());
         
			if ( torrent_comment == null ){
			   torrent_comment	= "";
			}
			
			torrent_created_by = locale_decoder.decodeString(torrent.getCreatedBy());
         
			if ( torrent_created_by == null ){
				torrent_created_by	= "";
			}
			 
			 nbPieces = torrent.getNumberOfPieces();
			 
			 	// only restore the tracker response cache for non-seeds
	   
			 if ( DiskManagerFactory.isTorrentResumeDataComplete(download_manager_state, torrent_save_dir, torrent_save_file )) {
			 	
				  download_manager_state.clearTrackerResponseCache();
					
				  stats.setDownloadCompleted(1000);
			  
				  setOnlySeeding(true);
			  
			 }else{
			 					 
				 setOnlySeeding(false);
			}
		}catch( TOTorrentException e ){
		
			Debug.printStackTrace( e );
			
			nbPieces = 0;
        		 			
			setFailed( TorrentUtils.exceptionToText( e ));
 			
		}catch( UnsupportedEncodingException e ){
		
			Debug.printStackTrace( e );
			
			nbPieces = 0;
        					
			setFailed( MessageText.getString("DownloadManager.error.unsupportedencoding"));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			nbPieces = 0;
    					
			setFailed( e );		
		}
		
		if ( download_manager_state == null ){
		
				// torrent's stuffed - create a dummy "null object" to simplify use
				// by other code
			
			download_manager_state	= DownloadManagerStateImpl.getDownloadState( this );
			
		}else{
			
				// make sure we know what networks to use for this download
			
			if ( download_manager_state.getNetworks().length == 0 ){
				
				String[] networks = AENetworkClassifier.getNetworks( torrent, display_name );
				
				download_manager_state.setNetworks( networks );
			}
			
			if ( download_manager_state.getPeerSources().length == 0 ){
				
				String[] ps = PEPeerSource.getPeerSources();
				
				download_manager_state.setPeerSources( ps );
			}
		}
	}




  /**
   * @return
   */
  public int getState() {
	if (state != STATE_INITIALIZED)
	  return state;
	if (diskManager == null)
	  return STATE_INITIALIZED;
	int diskManagerState = diskManager.getState();
	if (diskManagerState == DiskManager.INITIALIZING)
	  return STATE_INITIALIZED;
	if (diskManagerState == DiskManager.ALLOCATING)
	  return STATE_ALLOCATING;
	if (diskManagerState == DiskManager.CHECKING)
	  return STATE_CHECKING;
	if (diskManagerState == DiskManager.READY)
	  return STATE_READY;
	if (diskManagerState == DiskManager.FAULTY)
	  return STATE_ERROR;
	return STATE_ERROR;
  }
  
	public boolean getOnlySeeding() {
		return onlySeeding;
	}
	
  public void 
  setOnlySeeding(
  	boolean _onlySeeding) 
  {
     //LGLogger.log(getName()+"] setOnlySeeding("+onlySeeding+") was " + onlySeeding);
    if (onlySeeding != _onlySeeding) {
      onlySeeding = _onlySeeding;

      if (_onlySeeding && filesExist()) {
        // make sure stats always knows we are completed
			  stats.setDownloadCompleted(1000);
      }

  	  // we are in a new list, move to the top of the list so that we continue seeding
  	  // -1 position means it hasn't been added to the global list.  We shouldn't
  	  // touch it, since it'll get a position once it's adding is complete
      if (globalManager != null && position != -1) {
  		  DownloadManager[] dms = { DownloadManagerImpl.this };
  		  // pretend we are at the bottom of the new list
  		  // so that move top will shift everything down one
  		  position = globalManager.getDownloadManagers().size() + 1;
  		  
  		  if ( COConfigurationManager.getBooleanParameter("Newly Seeding Torrents Get First Priority" )){
  		  	globalManager.moveTop(dms);
  		  }else{
  		  	globalManager.moveEnd(dms);	
  		  }
  		  // we left a gap in incomplete list, fixup
        globalManager.fixUpDownloadManagerPositions();
      }
      listeners.dispatch( LDT_COMPLETIONCHANGED, new Boolean( _onlySeeding ));
    }
  }
	
	public boolean filesExist() {
	  return (filesExistErrorMessage() == "");
	}

	private String filesExistErrorMessage() {
		String strErrMessage = "";
		// currently can only seed if whole torrent exists
		if (diskManager == null) {
  		DiskManager dm = DiskManagerFactory.createNoStart( torrent, this);
  		if (dm.getState() == DiskManager.FAULTY)
  		  strErrMessage = dm.getErrorMessage();
  		else if (!dm.filesExist()) 
  		  strErrMessage = dm.getErrorMessage();
  		dm = null;
  	} else {
  		if (!diskManager.filesExist()) 
  		  strErrMessage = diskManager.getErrorMessage();
  	}
  	
  	if (!strErrMessage.equals("")) {
     
      setFailed( MessageText.getString("DownloadManager.error.datamissing") + " " + strErrMessage );
  	}

    return strErrMessage;
	}
	
	
  public boolean
  isPersistent()
  {
  	return( persistent );
  }
  
  /**
   * Returns the 'previous' state.
   */
  public int getPrevState() {
    return prevState;
  }
  
  /**
   * Sets the 'previous' state.
   */
  public void setPrevState(int prev_state) {
    prevState = prev_state;
  }
  

  public String 
  getDisplayName() 
  {
  	return( display_name );
  }	

  public String getErrorDetails() {
	return errorDetail;
  }

  public long getSize() {
	if (diskManager != null)
	  return diskManager.getTotalLength();
  if(torrent != null)
    return torrent.getSize();
  return 0;
  }

  protected void
  setFailed()
  {
  	setFailed((String)null );
  }
  
  protected void
  setFailed(
  	Throwable 	e )
  {
  	setFailed( Debug.getNestedExceptionMessage(e));
  }
  
  protected void
  setFailed(
  	String		reason )
  {
  	if ( reason != null ){
  		
  		errorDetail = reason;
  	}
  	
  	stopIt( DownloadManager.STATE_ERROR, false, false );
  }

  public void 
  stopIt(
  	final int 			_stateAfterStopping, 
	final boolean 		remove_torrent, 
	final boolean 		remove_data )
  {
    if( state == DownloadManager.STATE_STOPPED ||
        state == DownloadManager.STATE_ERROR ) {
    
    		//already in stopped state, just do removals if necessary
    	
      if( remove_data )  deleteDataFiles();
      
      if( remove_torrent )  deleteTorrentFile();
      
      setState( _stateAfterStopping );
      
      return;
    }
    
    
    if (state == DownloadManager.STATE_STOPPING){
    
    	return;
    }
    
  	setState( DownloadManager.STATE_STOPPING );

  		// this will run synchronously but on a non-daemon thread so that it will under
  		// normal circumstances complete, even if we're closing
  	
  	try{
	  	NonDaemonTaskRunner.run(
			new NonDaemonTask()
			{
				public Object 
				run()
				{
					int	stateAfterStopping = _stateAfterStopping;
					
					try{
			  								
						if (peerManager != null){
						  stats.setSavedDownloadedUploaded( 
								  stats.getSavedDownloaded() + peerManager.getStats().getTotalReceived(),
							 	  stats.getSavedUploaded() + peerManager.getStats().getTotalSent());
				      
						  stats.saveDiscarded(stats.getDiscarded());
						  stats.saveHashFails(stats.getHashFails());
						  stats.setSecondsDownloading(stats.getSecondsDownloading());
						  stats.setSecondsOnlySeeding(stats.getSecondsOnlySeeding());
							 	  
						  peerManager.removeListener( peer_manager_listener );
						  
						  peerManager.stopAll(); 
						  
						  try{
						  	peer_listeners_mon.enter();
						  
						  	peer_listeners.dispatch( LDT_PE_PM_REMOVED, peerManager );
						  	
						  }finally{
						  	
						  	peer_listeners_mon.exit();
						  }
			
						  peerManager = null; 
						}      
						
							// kill the tracker client after the peer manager so that the
							// peer manager's "stopped" event has a chance to get through
						
						if ( tracker_client != null ){
							
							tracker_client.removeListener( tracker_client_listener );
						
							download_manager_state.setTrackerResponseCache(
										tracker_client.getTrackerResponseCache());
								
							tracker_client.destroy();
								
							tracker_client = null;
						}							

						if (diskManager != null){
							stats.setCompleted(stats.getCompleted());
							stats.setDownloadCompleted(stats.getDownloadCompleted(true));
				      
						  if (diskManager.getState() == DiskManager.READY){
						  	
						  	try{
						  		diskManager.dumpResumeDataToDisk(true, false);
						  		
						  	}catch( Exception e ){
						  		
								errorDetail = "Resume data save fails: " + Debug.getNestedExceptionMessage(e);
								
								stateAfterStopping	= STATE_ERROR;
						  	}
						  }
				      
						  	// we don't want to update the torrent if we're seeding
						  
						  if ( !onlySeeding ){
						  	
						  	download_manager_state.save();
						  }
						  					  
						  diskManager.storeFilePriorities();
						  
						  diskManager.stop();
						  	
						  diskManager.removeListener( disk_manager_listener );
						  
						  diskManager = null;
						}
					
					 }finally{
								  
					   forceStarted = false;
             
					   if( remove_data ){
					   
					   		deleteDataFiles();
					   }
					   
					   if( remove_torrent ){
					   	
					   	deleteTorrentFile();
					   }
             
					   setState( stateAfterStopping );
             
					 }
				  	
					 return( null );
				}
			  });	
  	}catch( Throwable e ){
  		
  		Debug.printStackTrace( e );
  	}
  }

  public void
  saveResumeData()
  {
    if ( getState() == STATE_DOWNLOADING) {

    	try{
    		getDiskManager().dumpResumeDataToDisk(false, false);
    		
    	}catch( Exception e ){
    		
			setFailed( errorDetail = "Resume data save fails: " + Debug.getNestedExceptionMessage(e));
    	}
    }
    
  	// we don't want to update the torrent if we're seeding
	  
	  if ( !onlySeeding  ){
	  	
	  	download_manager_state.save();
	  }
  }
  
  public void
  saveDownload()
  {
    DiskManager disk_manager = diskManager;
    
    if ( disk_manager != null ){
    	
    	disk_manager.storeFilePriorities();
    }
    
    download_manager_state.save();
  }
  
  public void setState(int _state){
    // note: there is a DIFFERENCE between the state held on the DownloadManager and
    // that reported via getState as getState incorporated DiskManager states when
    // the DownloadManager is INITIALIZED
  	//System.out.println( "DM:setState - " + _state );
    if ( state != _state ) {
      state = _state;
      // sometimes, downloadEnded() doesn't get called, so we must check here too
      if (state == STATE_SEEDING) {
        setOnlySeeding(true);
      } else if (state == STATE_QUEUED) {
        if (onlySeeding && !filesExist())
          return;
      }else if ( state == STATE_ERROR ){
      
      		// the process of attempting to start the torrent may have left some empty
      		// directories created, some users take exception to this.
      		// the most straight forward way of remedying this is to delete such empty
      		// folders here
      	
      	if ( torrent != null && !torrent.isSimpleTorrent()){

      		File	save_dir_file	= new File( torrent_save_dir, torrent_save_file );

	      	if ( save_dir_file.exists() && save_dir_file.isDirectory()){
	      		
	      		FileUtil.recursiveEmptyDirDelete( save_dir_file );
	      	}
      	}
      }
      
      informStateChanged( state );
    }
  }

  public int getNbSeeds() {
	if (peerManager != null)
	  return peerManager.getNbSeeds();
	return 0;
  }

  public int getNbPeers() {
	if (peerManager != null)
	  return peerManager.getNbPeers();
	return 0;
  }

  

  public String getTrackerStatus() {
    if (tracker_client != null)
      return tracker_client.getStatusString();
    // to tracker, return scrape
    if (torrent != null && globalManager != null) {
      TRTrackerScraperResponse response = getTrackerScrapeResponse();
      if (response != null) {
        return response.getStatusString();
      }
    }

    return "";
  }

  	// this is called asynchronously when a response is received
  
  public void
  setTrackerScrapeResponse(
  	TRTrackerScraperResponse	response )
  {
  	tracker_listeners.dispatch( LDT_TL_SCRAPERESULT, response );
  }
  
  public TRTrackerAnnouncer 
  getTrackerClient() 
  {
	return( tracker_client );
  }
 
	public void
	setAnnounceResult(
		DownloadAnnounceResult	result )
	{
		TRTrackerAnnouncer	cl = getTrackerClient();
		
		if ( cl == null ){
			
			Debug.out( "setAnnounceResult called when download not running" );
			
			return;
		}
		
		cl.setAnnounceResult( result );
	}
	
	public void
	setScrapeResult(
		DownloadScrapeResult	result )
	{
		if ( torrent != null ){
			
			TRTrackerScraper	scraper = globalManager.getTrackerScraper();
		
			TRTrackerScraperResponse current_resp = getTrackerScrapeResponse();
			
			URL	target_url;
			
			if ( current_resp != null ){
				
				target_url = current_resp.getURL();
				
			}else{
				
				target_url = torrent.getAnnounceURL();
			}
			
			scraper.setScrape( torrent, target_url, result );
		}
	}
	
  /**
   * @return
   */
  public int getNbPieces() {
	return nbPieces;
  }


  public int getTrackerTime() {
    if (tracker_client != null)
      return tracker_client.getTimeUntilNextUpdate();

    // no tracker, return scrape
    if (torrent != null && globalManager != null) {
      TRTrackerScraperResponse response = getTrackerScrapeResponse();
      if (response != null) {
        if (response.getStatus() == TRTrackerScraperResponse.ST_SCRAPING)
          return -1;
        return (int)((response.getNextScrapeStartTime() - SystemTime.getCurrentTime()) / 1000);
      }
    }
	
    return TRTrackerAnnouncer.REFRESH_MINIMUM_SECS;
  }

  /**
   * @return
   */
  public TOTorrent
  getTorrent() 
  {
	return( torrent );
  }


  	public String 
	getTorrentSaveDirAndFile() 
  	{	  
  		return( torrent_save_dir + ( torrent_save_file==null?"":(File.separator + torrent_save_file )));
  	}

  	public String
	getTorrentSaveDir()
  	{		
  		return( torrent_save_dir );
  	}
	
	public String
	getTorrentSaveFile()
	{
		return( torrent_save_file );
	}
	
  public void 
  setTorrentSaveDir(
  	String sPath) 
  {
  		// assumption here is that the caller really knows what they are doing. You can't
  		// just change this willy nilly, it must be synchronised with reality. For example,
  		// the disk-manager calls it after moving files on completing
  		// The UI can call it as long as the torrent is stopped.
  		// Calling it while a download is active will in general result in unpredictable behaviour!
  
  	torrent_save_dir	= sPath;
  }

  public String getPieceLength(){
  	if ( torrent != null ){
	  return( DisplayFormatters.formatByteCountToKiBEtc(torrent.getPieceLength()));
  	}
  	return( "" );
  }

  /**
   * @return
   */
  public String getTorrentFileName() {
	return torrentFileName;
  }

  /**
   * @param string
   */
  public void setTorrentFileName(String string) {
	torrentFileName = string;
  }

  public TRTrackerScraperResponse 
  getTrackerScrapeResponse() 
  {
    TRTrackerScraperResponse r = null;
    
    if (globalManager != null) {
    	
    	TRTrackerScraper	scraper = globalManager.getTrackerScraper();
    	
    	if ( tracker_client != null ){
      	
    		r = scraper.scrape(tracker_client);
    	}
      
	    if ( r == null && torrent != null){
	      	
	    		// torrent not running. For multi-tracker torrents we need to behave sensibly
	      		// here
	      	
	    	TRTrackerScraperResponse	non_null_response = null;
	    	
	    	TOTorrentAnnounceURLSet[]	sets = torrent.getAnnounceURLGroup().getAnnounceURLSets();
	    	
	    	if ( sets.length == 0 ){
	    	
	    		r = scraper.scrape(torrent);
	    		
	    	}else{
	    			    			
	    			// we use a fixed seed so that subsequent scrapes will randomise
	    			// in the same order, as required by the spec. Note that if the
	    			// torrent's announce sets are edited this all works fine (if we
	    			// cached the randomised URL set this wouldn't work)
	    		
	    		Random	scrape_random = new Random(scrape_random_seed);
	    		
	    		for (int i=0;r==null && i<sets.length;i++){
	    			
	    			TOTorrentAnnounceURLSet	set = sets[i];
	    			
	    			URL[]	urls = set.getAnnounceURLs();
	    			
	    			List	rand_urls = new ArrayList();
	    							 	
				 	for (int j=0;j<urls.length;j++ ){
				  		
						URL url = urls[j];
						            									
						int pos = (int)(scrape_random.nextDouble() *  (rand_urls.size()+1));
						
						rand_urls.add(pos,url);
				  	}
				 	
				 	for (int j=0;r==null && j<rand_urls.size();j++){
				 		
				 		r = scraper.scrape(torrent, (URL)rand_urls.get(j));
				 		
				 		if ( r!= null ){
				 			
				 				// treat bad scrapes as missing so we go on to 
				 				// the next tracker
				 			
				 			if ( (!r.isValid()) || r.getStatus() == TRTrackerScraperResponse.ST_ERROR ){
				 				
				 				if ( non_null_response == null ){
				 					
				 					non_null_response	= r;
				 				}
				 				
				 				r	= null;
				 			}
				 		}
				 	}
	    		}
	    		
	    		if ( r == null ){
	    			
	    			r = non_null_response;
	    		}
	    	}
	    }
    }
    return r;
  }

  /**
   * @param string
   */
  public void setErrorDetail(String string) {
	errorDetail = string;
  }

  
  /**
   * Stops the current download, then restarts it again.
   */
  public void 
  restartDownload(
  	boolean use_fast_resume) 
  {
  	try{
	    if (!use_fast_resume) {
	      
	    		//invalidate resume info
	    	
	      diskManager.dumpResumeDataToDisk(false, true);
	      
	      readTorrent( torrent==null?null:torrent.getHash(),false, false );
	    }
	    
	    stopIt( DownloadManager.STATE_STOPPED, false, false );
	    
	    try {
	      while (state != DownloadManager.STATE_STOPPED) Thread.sleep(50);
	    } catch (Exception ignore) {/*ignore*/}
	    
	    initialize();
	    
  	}catch( Exception e ){
  		
		setFailed( "Resume data save fails: " + Debug.getNestedExceptionMessage(e));
  	}
  }
    
  
  public void startDownloadInitialized(boolean initStoppedDownloads) {
	if (getState() == DownloadManager.STATE_WAITING || initStoppedDownloads && getState() == DownloadManager.STATE_STOPPED) {
	  initialize();
	}
	if (getState() == DownloadManager.STATE_READY) {
	  startDownload();
	}
  }

  /** @retun true, if the other DownloadManager has the same size and hash 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj)
  {
		// check for object equivalence first!
  		
	if ( this == obj ){
  		
		return( true );
	}
  	
	if(null != obj && obj instanceof DownloadManager) {
    	
	  DownloadManager other = (DownloadManager) obj;
          
	  TOTorrent t1 = getTorrent();
	  TOTorrent t2 = other.getTorrent();
      
	  if ( t1 == null || t2 == null ){
      	
		return( false );	// broken torrents - treat as different so shown
							// as broken
	  }
      
	  try{
      	
		return( Arrays.equals(t1.getHash(), t2.getHash()));
     
	  }catch( TOTorrentException e ){
      	
			// only get here is serious problem with hashing process
      		
	  	Debug.printStackTrace( e );
	  }
	}
    
	return false;
  }
  
  public void 
  checkTracker() 
  {
  	checkTracker(false);
  }
  
  protected void
  checkTracker(
  	boolean	force )
  {
	if( tracker_client != null)
	tracker_client.update( force );
  }

  /**
   * @return
   */
  public String 
  getTorrentComment() {
	return torrent_comment;
  }
  
  public String 
  getTorrentCreatedBy() {
  	return torrent_created_by;
  }
  
  public long 
  getTorrentCreationDate() {
  	if (torrent==null){
  		return(0);
  	}
  	
  	return( torrent.getCreationDate());
  }
  
  /**
   * @return
   */
  public int getIndex() {
	if(globalManager != null)
	  return globalManager.getIndexOf(this);
	return -1;
  }
  
  public boolean isMoveableUp() {
	if(globalManager != null)
	  return globalManager.isMoveableUp(this);
	return false;
  }
  
  public boolean isMoveableDown() {
	if(globalManager != null)
	  return globalManager.isMoveableDown(this);
	return false;
  }
  
  public void moveUp() {
	if(globalManager != null)
	  globalManager.moveUp(this);
  }
  
  public void moveDown() {
	if(globalManager != null)
	  globalManager.moveDown(this);
  }      
  

	public GlobalManager
	getGlobalManager()
	{
		return( globalManager );
	}
	
  public DiskManager
  getDiskManager()
  {
  	return( diskManager );
  }
  
  public PEPeerManager
  getPeerManager()
  {
  	return( peerManager );
  }

  	public boolean
	isDownloadComplete()
  	{
  		return( onlySeeding );
  	}
  	
	public void
	addListener(
		DownloadManagerListener	listener )
	{
		listeners.addListener(listener);
		
			// pick up the current state
		
		listener.stateChanged( this, state );

			// we DON'T dispatch a downloadComplete event here as this event is used to mark the
			// transition between downloading and seeding, NOT purely to inform of seeding status
	}
	
	public void
	removeListener(
		DownloadManagerListener	listener )
	{
		listeners.removeListener(listener);			
	}
	
	protected void
	informStateChanged(
		int		new_state )
	{
		listeners.dispatch( LDT_STATECHANGED, new Integer( new_state ));
	}
	
	protected void
	informDownloadEnded()
	{
		listeners.dispatch( LDT_DOWNLOADCOMPLETE, null );
	}
	
	protected void
	informPositionChanged(int iOldPosition)
	{
		listeners.dispatch( LDT_POSITIONCHANGED, new Integer(iOldPosition));
	}

  public void
  addPeerListener(
	  DownloadManagerPeerListener	listener )
  {
  	try{
  		peer_listeners_mon.enter();
  		
  		peer_listeners.addListener( listener );
  		
		for (int i=0;i<current_peers.size();i++){
  			
			peer_listeners.dispatch( listener, LDT_PE_PEER_ADDED, current_peers.get(i));
		}
		
		for (int i=0;i<current_pieces.size();i++){
  			
			peer_listeners.dispatch( listener, LDT_PE_PIECE_ADDED, current_pieces.get(i));
		}
		
		PEPeerManager	temp = peerManager;
		
		if ( temp != null ){
	
			peer_listeners.dispatch( listener, LDT_PE_PM_ADDED, temp );
		}
  	}finally{
  		
  		peer_listeners_mon.exit();
  	}
  }
		
  public void
  removePeerListener(
	  DownloadManagerPeerListener	listener )
  {
  	peer_listeners.removeListener( listener );
  }
 

  public void
  addPeer(
	  PEPeer 		peer )
  {
  	try{
  		peer_listeners_mon.enter();
 	
  		current_peers.add( peer );
  		
  		peer_listeners.dispatch( LDT_PE_PEER_ADDED, peer );
  		
	}finally{
		
		peer_listeners_mon.exit();
	}
  }
		
  public void
  removePeer(
	  PEPeer		peer )
  {
    try{
    	peer_listeners_mon.enter();
    	
    	current_peers.remove( peer );
    	
    	peer_listeners.dispatch( LDT_PE_PEER_REMOVED, peer );
    	
    }finally{
    	
    	peer_listeners_mon.exit();
    }
 }
		
  public void
  addPiece(
	  PEPiece 	piece )
  {
  	try{
  		peer_listeners_mon.enter();
  		
  		current_pieces.add( piece );
  		
  		peer_listeners.dispatch( LDT_PE_PIECE_ADDED, piece );
  		
  	}finally{
  		
  		peer_listeners_mon.exit();
  	}
 }
		
  public void
  removePiece(
	  PEPiece		piece )
  {
  	try{
  		peer_listeners_mon.enter();
  		
  		current_pieces.remove( piece );
  		
  		peer_listeners.dispatch( LDT_PE_PIECE_REMOVED, piece );
  		
  	}finally{
  		
  		peer_listeners_mon.exit();
  	}
 }

	public DownloadManagerStats
	getStats()
	{
		return( stats );
	}

  public boolean isForceStart() {
    return forceStarted;
  }

  public void setForceStart(boolean forceStart) {
    if (forceStarted != forceStart) {
      forceStarted = forceStart;
      if (forceStarted && 
          (getState() == STATE_STOPPED || getState() == STATE_QUEUED)) {
        // Start it!  (Which will cause a stateChanged to trigger)
        setState(STATE_WAITING);
      } else {
        informStateChanged(getState());
      }
    }
  }

  /**
   * Is called when a download is finished.
   * Activates alerts for the user.
   *
   * @author Rene Leonhardt
   */
  public void 
  downloadEnded()
  {
    if (isForceStart()){
    	
      setForceStart(false);
    }

    setOnlySeeding(true);
	
    informDownloadEnded();
  }

  public DiskManager 
  initializeDiskManager() 
  {
  	DiskManager	res = diskManager;
  	
  	if( res == null) {

  		res = diskManager = DiskManagerFactory.create( torrent, this);
      
  		disk_manager_listener = 
  			new DiskManagerListener()
  			{
  				public void
  				stateChanged(
  					int 	oldDMState,
  					int		newDMState )
  				{
  					if ( newDMState == DiskManager.FAULTY ){
  						
  						setFailed( diskManager.getErrorMessage());						
   					}
  					
  					if (oldDMState == DiskManager.CHECKING) {
  						
  						stats.setDownloadCompleted(stats.getDownloadCompleted(true));
  						
  						DownloadManagerImpl.this.setOnlySeeding(diskManager.getRemaining() == 0);
  					}
  					  
  					if ( newDMState == DiskManager.READY ){
  						
  							// make up some sensible "downloaded" figure for torrents that have been re-added to Azureus
  							// and resumed 
  					
  						if ( 	stats.getDownloaded() == 0 &&
  								stats.getUploaded() == 0 &&
  								stats.getSecondsDownloading() == 0 ){
  						
  							int	completed = stats.getDownloadCompleted(false);
  							
  								// for seeds leave things as they are as they may never have been downloaded in the
  								// first place...
  							
  							if ( completed < 1000 ){
 
  									// assume downloaded = uploaded, optimistic but at least results in
  									// future share ratios relevant to amount up/down from now on
  									// see bug 1077060 
  								
  								long	amount_downloaded = (completed*diskManager.getTotalLength())/1000;
  								
 								stats.setSavedDownloadedUploaded( amount_downloaded,amount_downloaded );
   							}
  						}
  					}
  					
  					int	dl_state = getState();
  					
  					if ( dl_state != state ){
  						
  						informStateChanged( dl_state );
  					}
  				}
  			};
  		
  		diskManager.addListener( disk_manager_listener );
  	}
  	
  	return( res );
  }
  
  public boolean 
  canForceRecheck() 
  {
  	if ( torrent == null ){
  			// broken torrent, can't force recheck
  		
  		return( false );
  	}
  	
    return (state == STATE_STOPPED) ||
           (state == STATE_QUEUED) ||
           (state == STATE_ERROR && diskManager == null);
  }

  public void 
  forceRecheck() 
  {
  	if ( diskManager != null ) {
  		LGLogger.log(0, 0, LGLogger.ERROR, "Trying to force recheck while diskmanager active");
  		return;
  	}
  
  	if ( torrent == null ){
 		LGLogger.log(0, 0, LGLogger.ERROR, "Trying to force recheck with broken torrent");
  		return;
 		
  	}
    Thread recheck = 
    	new AEThread("forceRecheck") 
		{
			public void runSupport() {
				int start_state = DownloadManagerImpl.this.getState();
				
				setState(STATE_CHECKING);
				
					// remove resume data
				
				download_manager_state.clearResumeData();
				
				// For extra protection from a plugin stopping a checking torrent,
				// fake a forced start. 
				
				boolean wasForceStarted = forceStarted;
				
				forceStarted = true;
				
					// if a file has been deleted we want this recheck to recreate the file and mark
					// it as 0%, not fail the recheck. Otherwise the only way of recovering is to remove and
					// re-add the torrent
				
				setDataAlreadyAllocated( false );
				
				DiskManager recheck_disk_manager = initializeDiskManager();
				
				while ( recheck_disk_manager.getState() != DiskManager.FAULTY &&
						recheck_disk_manager.getState() != DiskManager.READY){
					
					try {
						
						Thread.sleep(100);
						
					} catch (Exception e) {
						
						Debug.printStackTrace( e );
					}
				}
				
				forceStarted = wasForceStarted;
				
				stats.setDownloadCompleted(stats.getDownloadCompleted(true));
				
				if ( recheck_disk_manager.getState() == DiskManager.READY ){
					
				  	try{
				  		recheck_disk_manager.dumpResumeDataToDisk(true, false);
				  		
						recheck_disk_manager.stop();
						
						setOnlySeeding(recheck_disk_manager.getRemaining() == 0);
						
						diskManager = null;
						
						if (start_state == STATE_ERROR){
							
							setState( STATE_STOPPED );
							
						}else{
							
							setState(start_state);
						}
				  	}catch( Exception e ){
				  		
						setFailed( errorDetail = "Resume data save fails: " + Debug.getNestedExceptionMessage(e));
				  	}
				  	
				  }else{ // Faulty
				  						
				  	recheck_disk_manager.stop();
					
				  	setOnlySeeding(false);
					
				  	diskManager = null;
					
					setFailed( recheck_disk_manager.getErrorMessage());	 
				  }
				}
			};
		
		recheck.setDaemon( true );
		recheck.setPriority(Thread.MIN_PRIORITY);
		recheck.start();
  }
  
  
  public int getHealthStatus() {
    if(peerManager != null && (state == STATE_DOWNLOADING || state == STATE_SEEDING)) {
      int nbSeeds = getNbSeeds();
      int nbPeers = getNbPeers();
      int nbRemotes = peerManager.getNbRemoteConnections();
      int trackerStatus = tracker_client.getLastResponse().getStatus();
      boolean isSeed = (state == STATE_SEEDING);
      
      if( (nbSeeds + nbPeers) == 0) {
        if(isSeed)
          return WEALTH_NO_TRACKER;        
        return WEALTH_KO;        
      }
      if( trackerStatus == TRTrackerAnnouncerResponse.ST_OFFLINE || trackerStatus == TRTrackerAnnouncerResponse.ST_REPORTED_ERROR)
        return WEALTH_NO_TRACKER;
      if( nbRemotes == 0 )
        return WEALTH_NO_REMOTE;
      return WEALTH_OK;
    } else {
      return WEALTH_STOPPED;
    }
  }
  
  public int getPosition() {
  	return position;
  }

  public void setPosition(int newPosition) {
    if (newPosition != position) {
//  	  LGLogger.log(getName() + "] setPosition from "+position+" to "+newPosition);
//    	Debug.outStackTrace();
      int oldPosition = position;
    	position = newPosition;
    	informPositionChanged(oldPosition);
    }
  }

  public void
  addTrackerListener(
  	DownloadManagerTrackerListener	listener )
  {  		
  	tracker_listeners.addListener( listener );
  }
  
  public void
  removeTrackerListener(
  	DownloadManagerTrackerListener	listener )
  {
  		tracker_listeners.removeListener( listener );
  }
  
  private void 
  deleteDataFiles() 
  {
  	DiskManagerFactory.deleteDataFiles(torrent, torrent_save_dir, torrent_save_file );
  }
  
  private void 
  deleteTorrentFile() 
  {
  	if ( torrentFileName != null ){
  		
        TorrentUtils.delete( new File(torrentFileName));
    }
  }
  
  public DownloadManagerState 
  getDownloadState()
  {
  	return( download_manager_state );
  }
  
  
  /** To retreive arbitrary objects against a download. */
  public Object getData (String key) {
  	if (data == null) return null;
    return data.get(key);
  }

  /** To store arbitrary objects against a download. */
  public void setData (String key, Object value) {
  	try{
  		peer_listeners_mon.enter();
  	
	  	if (data == null) {
	  	  data = new HashMap();
	  	}
	    if (value == null) {
	      if (data.containsKey(key))
	        data.remove(key);
	    } else {
	      data.put(key, value);
	    }
  	}finally{
  		
  		peer_listeners_mon.exit();
  	}
  }
  
  
  public boolean 
  isDataAlreadyAllocated() 
  {  
  	return data_already_allocated;  
  }
  
  public void 
  setDataAlreadyAllocated( 
  	boolean already_allocated ) 
  {
    data_already_allocated = already_allocated;
  }
    
  public long
  getCreationTime()
  {
  	return( creation_time );
  }

  public void
  setCreationTime(
  	long		t )
  {
  	creation_time	= t;
  }
}
