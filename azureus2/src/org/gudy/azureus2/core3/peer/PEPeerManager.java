/*
 * File    : PEPeerManager
 * Created : 5 Oct. 2003
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

package org.gudy.azureus2.core3.peer;

/**
 * @author parg
 *
 */

import java.util.List;

import org.gudy.azureus2.core3.disk.DiskManagerReadRequest;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.peer.util.*;
import org.gudy.azureus2.core3.tracker.client.*;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.download.DownloadManager;

import com.aelitis.azureus.core.peermanager.LimitedRateGroup;


public interface 
PEPeerManager 
{
	public static final int PS_INITIALISED	= 1;
	public static final int PS_DOWNLOADING	= 2;
	public static final int PS_SEEDING		= 3;
	public static final int PS_STOPPED		= 4;
	
	/**
	 * get the state from above PS_ set
	 * @return
	 */
	
	public int
	getState();
	
 	public DownloadManager
	getDownloadManager();
 
	public void
	start();
		
	public void
	stopAll();
		
	public byte[]
	getHash();
	
	public PeerIdentityDataID
	getPeerIdentityDataID();
	
	public byte[]
	getPeerId();

	public int[] getAvailability();

	public int getAvailability(int pieceNumber);
	
    public float getMinAvailability();

	public PEPiece[] getPieces();

	public PEPeerManagerStats
	getStats();

	public void
	processTrackerResponse(
		TRTrackerAnnouncerResponse	response );
		
	public int getNbPeers();

	public int getNbSeeds();
	
	public int getNbHashFails();
  
	public void setNbHashFails(int fails);

	public int getPiecesNumber();

  public int getPieceLength(int pieceNumber);
		
	public long getRemaining();
	
	public long getETA();

	public String getElapsedTime();
	
	// Time Started in ms
	public long getTimeStarted();

	public long getTimeStartedSeeding();
	
	public void
	addListener(
		PEPeerManagerListener	l );
		
	public void
	removeListener(
		PEPeerManagerListener	l );
  
  public void pieceAdded(PEPiece piece);
  
  public boolean needsMD5CheckOnCompletion(int pieceNumber);
  
  public boolean
  isSuperSeedMode();
  
  public int getNbRemoteConnections();
  
  
  /**
   * Data bytes received.
   * @param l
   */
	public void	received(	int	l );	
	
  /**
   * Data bytes sent.
   * @param l
   */
	public void	sent( int	l );
	
  /**
   * Protocol bytes sent.
   * @param length
   */
  public void protocol_sent( int length );
  
  /**
   * Protocol bytes received.
   * @param length
   */
  public void protocol_received( int length );
  
  
  
	public void
	discarded(
		int		l );		
	
	public PEPeerStats
	createPeerStats();
	
	
	public List
	getPeers();
	
	public void
	addPeer(
		PEPeer	peer );
  
  
  /**
   * Add a new peer, using the default internal PEPeer implementation
   * (like for peers given in announce reply), using the given address
   * and port.
   * @param ip_address of peer to inject
   * @param port of peer to inject
   */
  public void addPeer( String ip_address, int port );
  
	
	public void
	removePeer(
		PEPeer	peer );
	
	public void 
	peerAdded(PEPeer pc);

	public void 
	peerRemoved(PEPeer pc);
	
	public DiskManagerReadRequest
	createDiskManagerRequest(
	   int pieceNumber,
	   int offset,
	   int length );
	
	public void
	requestCanceled(
		DiskManagerReadRequest	item );
		
	public boolean 
	checkBlock(
		int 		pieceNumber, 
		int 		offset, 
		DirectByteBuffer 	data );
	
	public void 
	writeBlock(
		int 		pieceNumber, 
		int 		offset, 
		DirectByteBuffer 	data,
		PEPeer 		sender);		
  
  public void writeBlockAndCancelOutstanding(int pieceNumber, int offset, DirectByteBuffer data,PEPeer sender);
  
  public boolean isBlockAlreadyWritten( int piece_number, int offset );

  /**
   * Are we in end-game mode?
   * @return true if in end game mode, false if not
   */
  public boolean isInEndGameMode();
  
  /**
   * Notify the manager that the given peer connection has been closed.
   * @param peer closed
   * @param reconnect if true, attempt to reestablish this peer connection
   */
  public void peerConnectionClosed( PEPeerTransport peer, boolean reconnect );
  
  
  /**
   * Get the limited rate group used for upload limiting.
   * @return upload limit group
   */
  public LimitedRateGroup getUploadLimitedRateGroup();
  
  /** To retreive arbitrary objects against this object. */
  public Object getData (String key);
  /** To store arbitrary objects against this object. */
  public void setData (String key, Object value);
  
}
