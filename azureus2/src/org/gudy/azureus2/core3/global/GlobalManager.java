/*
 * File    : GlobalManager.java
 * Created : 21-Oct-2003
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

package org.gudy.azureus2.core3.global;

import java.util.List;

import com.aelitis.azureus.core.AzureusCoreComponent;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.download.*;

public interface
GlobalManager
	extends AzureusCoreComponent
{
	public DownloadManager
	addDownloadManager(
		String			file_name,
		String			save_path );
	
	public DownloadManager
	addDownloadManager(
		String			file_name,
		String			save_path,
		int        		initialState );
		
	public DownloadManager
	addDownloadManager(
	    String 		fileName,
	    String 		savePath,
	    int         initialState,
		boolean		persistent );
  
	public DownloadManager
	addDownloadManager(
	    String 		fileName,
	    String 		savePath,
	    int         initialState,
		boolean		persistent,
		boolean		for_seeding );
  
	public void
	removeDownloadManager(
		DownloadManager	dm )
	
		throws GlobalManagerDownloadRemovalVetoException;
	
	public void
	canDownloadManagerBeRemoved(
			DownloadManager	dm )
	
		throws GlobalManagerDownloadRemovalVetoException;
	
		/**
		 * returns a COPY of the current set of download managers so iteration is safe
		 * @return
		 */
	
	public List
	getDownloadManagers();
	
	public DownloadManager 
	getDownloadManager(TOTorrent torrent);

	public void
	stopAll();

	/**
	 * Stops all downloads without removing them
	 *	
	 *	 @author Rene Leonhardt
	 */
	
	public void
	stopAllDownloads();
  
	/**
	 * Starts all downloads
	 */
	
    public void
    startAllDownloads();
    
    /**
     * pauses (stops) all running downloads
     * @return an object to supply when resuming the downloads - defines the paused state
     */
    
    
    
    /**
     * Pauses (stops) all running downloads/seedings.
     */
    public void pauseDownloads();
    

    /**
     * Indicates whether or not there are any downloads that can be paused.
     * @return true if there is at least one download to pause, false if none
     */
    public boolean canPauseDownloads();

 	
    /**
     * Resumes (starts) all downloads paused by the previous pauseDownloads call.
     */
    public void resumeDownloads();

    
    /**
     * Indicates whether or not there are any paused downloads to resume.
     * @return true if there is at least one download to resume, false if none.
     */
    public boolean canResumeDownloads();
    
    
    
    
	public TRTrackerScraper
	getTrackerScraper();
	
	public GlobalManagerStats
	getStats();

	public int
	getIndexOf(
		DownloadManager	dm );
	
	public boolean
	isMoveableDown(
		DownloadManager	dm );
	
	public boolean
	isMoveableUp(
		DownloadManager	dm );
	
	public void
	moveDown(
		DownloadManager	dm );
	
	public void
	moveUp(
		DownloadManager	dm );
		
  public void
  moveEnd(
      DownloadManager[] dm );
  
  public void
  moveTop(
      DownloadManager[] dm );
  
  public void 
  moveTo(
  		DownloadManager manager, int newPosition );

  /** Verifies the positions of the DownloadManagers, 
   *  filling in gaps and shifting duplicate IDs down if necessary.
   *
   *  This does not need to be called after MoveXXX functions.
   */
	public void
	fixUpDownloadManagerPositions();

  public void
	addListener(
		GlobalManagerListener	l );
		
	public void
	removeListener(
		GlobalManagerListener	l );

	public void
	addDownloadWillBeRemovedListener(
		GlobalManagerDownloadWillBeRemovedListener	l );
	
	public void
	removeDownloadWillBeRemovedListener(
		GlobalManagerDownloadWillBeRemovedListener	l );
}