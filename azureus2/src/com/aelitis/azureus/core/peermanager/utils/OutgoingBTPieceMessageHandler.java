/*
 * Created on Jul 19, 2004
 * Created by Alon Rohter
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

package com.aelitis.azureus.core.peermanager.utils;

import java.util.*;

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue;
import com.aelitis.azureus.core.peermanager.messaging.*;
import com.aelitis.azureus.core.peermanager.messaging.bittorrent.*;


/**
 * Front-end manager for handling requested outgoing bittorrent Piece messages.
 * Peers often make piece requests in batch, with multiple requests always
 * outstanding, all of which won't necessarily be honored (i.e. we choke them),
 * so we don't want to waste time reading in the piece data from disk ahead
 * of time for all the requests. Thus, we only want to perform read-aheads for a
 * small subset of the requested data at any given time, which is what this handler
 * does, before passing the messages onto the outgoing message queue for transmission.
 */
public class OutgoingBTPieceMessageHandler {
  private final OutgoingMessageQueue outgoing_message_queue;
  private final DiskManager disk_manager;
  private final LinkedList requests = new LinkedList();
  private final List		loading_messages 		= new ArrayList();
  private final Map queued_messages = new HashMap();
  private int num_messages_loading = 0;
  private int num_messages_in_queue = 0;
  private final AEMonitor	lock_mon	= new AEMonitor( "OutgoingBTPieceMessageHandler:lock");
  private boolean destroyed = false;
  private int request_read_ahead = 16;
  
  
  private final DiskManagerReadRequestListener read_req_listener = new DiskManagerReadRequestListener() {
    public void readCompleted( DiskManagerReadRequest request, DirectByteBuffer data ) {
      BTPiece msg;
      try{
      	lock_mon.enter();

      	if( !loading_messages.contains( request ) || destroyed ) { //was canceled
      	  data.returnToPool();
      	  return;
      	}
      	loading_messages.remove( request );

        num_messages_loading--; 
        msg = new BTPiece( request.getPieceNumber(), request.getOffset(), data );
        queued_messages.put( msg, request );
        num_messages_in_queue++;
        
        outgoing_message_queue.addMessage( msg, true );
      }
      finally{
      	lock_mon.exit();
      }

      outgoing_message_queue.doListenerNotifications();
    }
  };
  
  
  private final OutgoingMessageQueue.MessageQueueListener sent_message_listener = new OutgoingMessageQueue.MessageQueueListener() {
    public boolean messageAdded( Message message ) {  return true;  }
    
    public void messageSent( Message message ) {
      if( message.getID().equals( BTMessage.ID_BT_PIECE ) ) {
        try{
          lock_mon.enter();
        
          queued_messages.remove( message );
          num_messages_in_queue--;
          doReadAheadLoads();
        }finally{
          lock_mon.exit();
        }
      }
    }
    public void messageQueued( Message message ) {/*nothing*/}
    public void messageRemoved( Message message ) {/*nothing*/}
    public void protocolBytesSent( int byte_count ) {/*ignore*/}
    public void dataBytesSent( int byte_count ) {/*ignore*/}
  };
  
  
  
  /**
   * Create a new handler for outbound piece messages,
   * reading piece data from the given disk manager
   * and transmitting the messages out the given message queue.
   * @param disk_manager
   * @param outgoing_message_q
   */
  public OutgoingBTPieceMessageHandler( DiskManager disk_manager, OutgoingMessageQueue outgoing_message_q ) {
    this.disk_manager = disk_manager;
    this.outgoing_message_queue = outgoing_message_q;
    outgoing_message_queue.registerQueueListener( sent_message_listener );
  }
  
  
  /**
   * Register a new piece data request.
   * @param piece_number
   * @param piece_offset
   * @param length
   */
  public void addPieceRequest( int piece_number, int piece_offset, int length ) {
    if( destroyed )  return;
    
    DiskManagerReadRequest dmr = disk_manager.createReadRequest( piece_number, piece_offset, length );

    try{
      lock_mon.enter();
    
      requests.addLast( dmr );
      doReadAheadLoads();
    }finally{
      lock_mon.exit();
    }
  }
  
  
  /**
   * Remove an outstanding piece data request.
   * @param piece_number
   * @param piece_offset
   * @param length
   */
  public void removePieceRequest( int piece_number, int piece_offset, int length ) {
    DiskManagerReadRequest dmr = disk_manager.createReadRequest( piece_number, piece_offset, length );
    
    try{
      lock_mon.enter();
    
      if( requests.contains( dmr ) ) {
        requests.remove( dmr );
        return;
      }
      
      if( loading_messages.contains( dmr ) ) {
        loading_messages.remove( dmr );
        num_messages_loading--;
        return;
      }
      
      
      for( Iterator i = queued_messages.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry entry = (Map.Entry)i.next();
        if( entry.getValue().equals( dmr ) ) {  //it's already been queued
          BTPiece msg = (BTPiece)entry.getKey();
          if( outgoing_message_queue.removeMessage( msg, true ) ) {
            i.remove();
            num_messages_in_queue--;
          }
          break;  //do manual listener notify
        }
      }
    }
    finally{
      lock_mon.exit();
    }
    
    outgoing_message_queue.doListenerNotifications();
  }
  
  
  /**
   * Remove all outstanding piece data requests.
   */
  public void removeAllPieceRequests() {
    try{
      lock_mon.enter();
  
      requests.clear();
      loading_messages.clear();
      num_messages_loading = 0;
      
      for( Iterator i = queued_messages.keySet().iterator(); i.hasNext(); ) {
        BTPiece msg = (BTPiece)i.next();
        if( outgoing_message_queue.removeMessage( msg, true ) ) {
          i.remove();
          num_messages_in_queue--;
        }
      }
    }
    finally{
      lock_mon.exit();
    }
    
    outgoing_message_queue.doListenerNotifications();
  }
      

  
  public void setRequestReadAhead( int num_to_read_ahead ) {
    request_read_ahead = num_to_read_ahead;
  }
  
  
  
  public void destroy() {
    try{
      lock_mon.enter();
  
      requests.clear();
      loading_messages.clear();
      num_messages_loading = 0;
      destroyed = true;
    }
    finally{
      lock_mon.exit();
    }
  }
  
  
  private void doReadAheadLoads() {
    while( num_messages_loading + num_messages_in_queue < request_read_ahead && !requests.isEmpty() && !destroyed ) {
      DiskManagerReadRequest dmr = (DiskManagerReadRequest)requests.removeFirst();
      loading_messages.add( dmr );
      disk_manager.enqueueReadRequest( dmr, read_req_listener );
      num_messages_loading++;
    }
  }

  
}
