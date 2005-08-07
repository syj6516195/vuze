/*
 * File    : PEPieceImpl.java
 * Created : 15-Oct-2003
 * By      : Olivier
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

package org.gudy.azureus2.core3.peer.impl;

/**
 * @author parg
 *
 */

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.util.AEMonitor;

public class 
PEPieceImpl
	implements PEPiece
{  
  private DiskManagerPiece	dm_piece;
	  
  private boolean[] downloaded;
  private boolean[] requested;
  
  private PEPeer[] 	writers;
  private List 		writes;

  private PEPeer reservedBy;
    
   public boolean isBeingChecked = false;

  //A Flag to indicate that this piece is for slow peers
  //Slow peers can only continue/create slow pieces
  //Fast peers can't continue/create slow pieces
  //In end game mode, this limitation isn't used
   
  private boolean slowPiece;
  
  public PEPeerManager manager;

  	// experimental class level lock
  
  protected static AEMonitor 	class_mon	= new AEMonitor( "PEPiece:class");

  public 
  PEPieceImpl(
  	PEPeerManager 		_manager, 
	DiskManagerPiece	_dm_piece,
	boolean				_slow_piece,
	boolean				_recovered )
  {  
	manager 	= _manager;
	dm_piece	= _dm_piece;
	slowPiece	= _slow_piece;
		
	int	nbBlocs 	= dm_piece.getBlockCount();
	
	downloaded 	= new boolean[nbBlocs];
	requested 	= new boolean[nbBlocs];
	writers 	= new PEPeer[nbBlocs];
	writes 		= new ArrayList(0);
	
	if ( !_recovered ){
		
		dm_piece.setInitialWriteTime();
	}
  }

  
  public int 
  getAvailability()
  {
  	if ( manager == null ){
  		
  		return( 0 );
  	}
  	
  	return( manager.getAvailability( dm_piece.getPieceNumber()));
  }



  public boolean[] getRequested(){
  	return( requested );
  }
  
  public boolean[] getDownloaded(){
    return( downloaded );
  }
  
  
  public void setBlockWritten(int blocNumber) {
	downloaded[blocNumber] = true;    
  }

  // This method is used to clear the requested information
  public void clearRequested(int blocNumber) {
	requested[blocNumber] = false;
  }

  // This method will return the first non requested bloc and
  // will mark it as requested
  
  	/**
  	 * Assumption - single threaded access to this
  	 */
  public int 
  getAndMarkBlock() 
  {
		for (int i = 0; i < requested.length; i++) {
			
		  if (!requested[i] && !dm_piece.getWritten(i)) {
			
			requested[i] = true;
	
			return( i );
		  }
		}
		
		return( -1 );
  }

  	/**
  	 * This method is safe in a multi-threaded situation as the worst that it can
  	 * do is mark a block as not requested even though its downloaded which may lead
  	 * to it being downloaded again
  	 */
  
  public void 
  unmarkBlock(
  	int blocNumber) 
  { 	
  	if (!downloaded[blocNumber]){
  		
  		requested[blocNumber] = false;
  	}
  }
  
  	/**
  	 * Assumption - single threaded with getAndMarkBlock
  	 */
  
  public void 
  markBlock(int blocNumber) 
  { 	
  	if (!downloaded[blocNumber]){
  		
  		requested[blocNumber] = true;
  	}
  }

  public int 
  getBlockSize(
  	int blocNumber) 
  {
	if ( blocNumber == (downloaded.length - 1)){
	
		int	length = dm_piece.getLength();
		
		if ((length % DiskManager.BLOCK_SIZE) != 0){
		
			return( length % DiskManager.BLOCK_SIZE );
		}
	}
	
	return DiskManager.BLOCK_SIZE;
  }

  public int 
  getPieceNumber()
  {
  	return( dm_piece.getPieceNumber() );
  }
  public int getLength(){
  	return( dm_piece.getLength() );
  }
  public int getNbBlocs(){
  	return( downloaded.length );  
  }
  
  public void setBeingChecked() {
	this.isBeingChecked = true;
  }
  
  public void setBeingChecked(boolean checking) {
  this.isBeingChecked = checking;
  }

  public boolean isBeingChecked() {
	return this.isBeingChecked;
  }

  /**
   * @param manager
   */
  public void setManager(PEPeerManager _manager) {
	manager = _manager;
  }

  public void setWritten(PEPeer peer,int blocNumber) {
    writers[blocNumber] = peer;
  
    dm_piece.setWritten( blocNumber );
  }
  
  public List getPieceWrites() {
    List result;
    try{
    	class_mon.enter();
    
    	result = new ArrayList(writes);
    }finally{
    	
    	class_mon.exit();
    }
    return result;
  }


  public List getPieceWrites(int blockNumber) {
    List result;
    try{
    	class_mon.enter();
    
    	result = new ArrayList(writes);
      
    }finally{
    	
    	class_mon.exit();
    }
    Iterator iter = result.iterator();
    while(iter.hasNext()) {
      PEPieceWriteImpl write = (PEPieceWriteImpl) iter.next();
      if(write.getBlockNumber() != blockNumber)
        iter.remove();
    }
    return result;
  }


  public List getPieceWrites(PEPeer peer) {
    List result;
     try{
     	class_mon.enter();
     
     	result = new ArrayList(writes);
     }finally{
     	class_mon.exit();
     }
    Iterator iter = result.iterator();
    while(iter.hasNext()) {
      PEPieceWriteImpl write = (PEPieceWriteImpl) iter.next();
      if(peer == null || ! peer.equals(write.getSender()))
        iter.remove();
    }
     return result;
  }
  
  public void reset() {
  	dm_piece.reset();
  	
  	int	nbBlocs = downloaded.length;
  	
    downloaded 	= new boolean[nbBlocs];
    requested 	= new boolean[nbBlocs];
    writers 	= new PEPeer[nbBlocs];
    
    isBeingChecked = false;
    reservedBy = null;
 
  }
  
  protected void addWrite(PEPieceWriteImpl write) {
  	try{
  		class_mon.enter();
  
  		writes.add(write);
  		
  	}finally{
  		
  		class_mon.exit();
  	}
  }
  
  public void 
  addWrite(
  		int blockNumber,
		PEPeer sender, 
		byte[] hash,
		boolean correct	)
  {
  	addWrite( new PEPieceWriteImpl( blockNumber, sender, hash, correct ));
  }
  public PEPeer[] getWriters() {
    return writers;
  }
  
  public boolean isSlowPiece() {
    return slowPiece;
  }
  
  public void setSlowPiece(boolean _slowPiece) {
    slowPiece = _slowPiece;
  }

  	// written can be null, in which case if the piece is complete, all blocks are complete
  	// otherwise no blocks are complete
  
  public boolean[] 
  getWritten()
  {
  	return( dm_piece.getWritten());
  }
  
  public boolean
  isComplete()
  {
  	return( dm_piece.getCompleted());
  }
  
  public int
  getCompleted()
  {
  	return( dm_piece.getCompleteCount());
  }
  
  public boolean
  isWritten(
  	int		bn )
  {
  	return( dm_piece.getWritten( bn ));
  }

  public long
  getLastWriteTime()
  {
  	return( dm_piece.getLastWriteTime());
  }
  
  /**
   * @return Returns the manager.
   */
  public PEPeerManager getManager() {
    return manager;
  }
  
  public void setReservedBy(PEPeer peer) {
   this.reservedBy = peer;   
  }
  
  public PEPeer getReservedBy() {
    return this.reservedBy;
  }
  
  public void reDownloadBlock(int blockNumber) {
    downloaded[blockNumber] = false;
    requested[blockNumber] = false;
    dm_piece.reDownloadBlock(blockNumber);
  }
  
}