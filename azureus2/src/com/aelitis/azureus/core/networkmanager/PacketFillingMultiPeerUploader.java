/*
 * Created on Sep 28, 2004
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

package com.aelitis.azureus.core.networkmanager;

import java.io.IOException;
import java.util.*;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.messaging.*;



/**
 * A rate-controlled write entity backed by multiple peer connections, with an
 * emphasis on transmitting packets with full payloads, i.e. it writes to the
 * transport in mss-sized chunks if at all possible. It also employs fair,
 * round-robin write scheduling, where connections each take turns writing a
 * single full packet per round.
 */
public class PacketFillingMultiPeerUploader implements RateControlledWriteEntity {
  private static final int FLUSH_CHECK_LOOP_TIME = 250;  //250ms
  private static final int FLUSH_WAIT_TIME = 3*1000;  //3sec no-new-data wait before forcing write flush
  
  private final RateHandler rate_handler;
  private boolean destroyed = false;
  
  private final HashMap waiting_connections = new HashMap();
  private final LinkedList ready_connections = new LinkedList();
  private final AEMonitor lists_lock = new AEMonitor( "PacketFillingMultiPeerUploader:lists_lock" );
  

  /**
   * Create a new packet-filling multi-peer upload entity,
   * rate-controlled by the given handler.
   * @param rate_handler listener to handle upload rate limits
   */
  public PacketFillingMultiPeerUploader( RateHandler rate_handler ) {
    this.rate_handler = rate_handler;
    
    Thread flush_checker = new AEThread( "PacketFillingMultiPeerUploader:FlushChecker" ) {
      public void runSupport() {
        flushCheckLoop();
      }
    };
    flush_checker.setDaemon( true );
    flush_checker.start();
  }
  
  
  /**
   * Checks the connections in the waiting list to see if it's time to be force-flushed.
   */
  private void flushCheckLoop() {
    while( !destroyed ) {
      long start_time = SystemTime.getCurrentTime();
      
      try {  lists_lock.enter();
        long current_time = SystemTime.getCurrentTime();
        
        for( Iterator i = waiting_connections.entrySet().iterator(); i.hasNext(); ) {
          Map.Entry entry = (Map.Entry)i.next();
          PeerData peer_data = (PeerData)entry.getValue();
          
          long wait_time = current_time - peer_data.last_message_added_time;
          
          if( wait_time > FLUSH_WAIT_TIME || wait_time < 0 ) {  //time to force flush
            Connection conn = (Connection)entry.getKey();
            
            if( conn.getOutgoingMessageQueue().getTotalSize() > 0 ) { //has data to flush
              conn.getOutgoingMessageQueue().cancelQueueListener( peer_data.queue_listener ); //cancel the listener
              i.remove();  //remove from the waiting list
              addToReadyList( conn );
            }
            else { //no data, so reset flush wait time
              peer_data.last_message_added_time = current_time;
            }
          }
        }
      }
      finally {  lists_lock.exit();  }
      
      long total_time = SystemTime.getCurrentTime() - start_time;
      
      if( total_time < FLUSH_CHECK_LOOP_TIME && total_time >= 0 ) {
        try {  Thread.sleep( FLUSH_CHECK_LOOP_TIME - total_time );  }catch( Exception e ) { Debug.printStackTrace( e ); }
      }
    }
  }
  
  
  
  /**
   * Destroy this upload entity.
   * Note: Removes all peer connections in the process.
   */
  public void destroy() {
    destroyed = true;
    
    try {
      lists_lock.enter();
      
      //remove and cancel all connections in waiting list    
      for( Iterator i = waiting_connections.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry entry = (Map.Entry)i.next();
        Connection conn = (Connection)entry.getKey();
        PeerData data = (PeerData)entry.getValue();
        conn.getOutgoingMessageQueue().cancelQueueListener( data.queue_listener );
      }
      waiting_connections.clear();
      
      //remove from ready list
      ready_connections.clear();
    }
    finally {
      lists_lock.exit();
    }
  }
  

  
  
  /**
   * Add the given connection to be managed by this upload entity.
   * @param peer_connection to be write managed
   */
  public void addPeerConnection( Connection peer_connection ) {
    int mss_size = NetworkManager.getSingleton().getTcpMssSize();
    boolean has_urgent_data = peer_connection.getOutgoingMessageQueue().hasUrgentMessage();
    int num_bytes_ready = peer_connection.getOutgoingMessageQueue().getTotalSize();
    
    if( num_bytes_ready >= mss_size || has_urgent_data ) {  //has a full packet's worth, or has urgent data
      addToReadyList( peer_connection );
    }
    else {   //has data to send, but not enough for a full packet
      addToWaitingList( peer_connection );
    }
  }
  
  
  /**
   * Remove the given connection from this upload entity.
   * @param peer_connection to be removed
   * @return true if the connection was found and removed, false if not removed
   */
  public boolean removePeerConnection( Connection peer_connection ) {
    try {
      lists_lock.enter();
      
      //look for the connection in the waiting list and cancel listener if found
      PeerData peer_data = (PeerData)waiting_connections.remove( peer_connection );
      if( peer_data != null ) {
        peer_connection.getOutgoingMessageQueue().cancelQueueListener( peer_data.queue_listener );
        return true;
      }
      
      //look for the connection in the ready list
      if( ready_connections.remove( peer_connection ) ) {
        return true;
      }
      
      return false;
    }
    finally {
      lists_lock.exit();
    }
  }
  
  
  

  //connections with less than a packet's worth of data
  private void addToWaitingList( final Connection conn ) {
    final PeerData peer_data = new PeerData();
    
    OutgoingMessageQueue.MessageQueueListener listener = new OutgoingMessageQueue.MessageQueueListener() {
      public boolean messageAdded( Message message ) {  return true;  }
      
      public void messageQueued( Message message ) {  //connection now has more data to send
        try {
          lists_lock.enter();
          
          if( waiting_connections.get( conn ) == null ) {  //connection has already been removed from the waiting list
            return;  //stop further processing
          }
          
          int mss_size = NetworkManager.getSingleton().getTcpMssSize();
          boolean has_urgent_data = conn.getOutgoingMessageQueue().hasUrgentMessage();
          int num_bytes_ready = conn.getOutgoingMessageQueue().getTotalSize();
        
          if( num_bytes_ready >= mss_size || has_urgent_data ) {  //has a full packet's worth, or has urgent data
            waiting_connections.remove( conn );  //remove from waiting list
            conn.getOutgoingMessageQueue().cancelQueueListener( this ); //cancel this listener
            addToReadyList( conn );
          }
          else {  //still not enough data for a full packet
            peer_data.last_message_added_time = SystemTime.getCurrentTime();  //update last message added time
          }
        }
        finally {
          lists_lock.exit();
        }
      }

      public void messageRemoved( Message message ) {/*ignore*/}
      public void messageSent( Message message ) {/*ignore*/}
      public void protocolBytesSent( int byte_count ) {/*ignore*/}
      public void dataBytesSent( int byte_count ) {/*ignore*/}
    };
    
    peer_data.queue_listener = listener;  //attach listener
    peer_data.last_message_added_time = SystemTime.getCurrentTime(); //start flush wait time
    
    try {
      lists_lock.enter();
      
      waiting_connections.put( conn, peer_data ); //add to waiting list
      conn.getOutgoingMessageQueue().registerQueueListener( listener );  //listen for added data
    }
    finally {
      lists_lock.exit();
    }
  }
  
  
  
  //connections ready to write
  private void addToReadyList( final Connection conn ) {
    
    if( conn.getOutgoingMessageQueue().getTotalSize() < 1 ) {
      Debug.out( "~~~ added conn size < 1 " );
    }

    try {
      lists_lock.enter();
      
      ready_connections.addLast( conn );  //add to ready list
    }
    finally {
      lists_lock.exit();
    }
  }
  
  
  
  private int write( int num_bytes_to_write ) {
    if( num_bytes_to_write < 1 ) {
      Debug.out( "num_bytes_to_write < 1" );
      return 0;  //not allowed to write
    }
        
    HashMap connections_to_notify_of_exception = new HashMap();
    ArrayList manual_notifications = new ArrayList();
    
    int num_bytes_remaining = num_bytes_to_write;    
    
    try {
      lists_lock.enter();
      
      int num_unusable_connections = 0;
      
      while( num_bytes_remaining > 0 && num_unusable_connections < ready_connections.size() ) {
        Connection conn = (Connection)ready_connections.removeFirst();
        
        if( !conn.getTCPTransport().isReadyForWrite() ) {  //not yet ready for writing
          ready_connections.addLast( conn );  //re-add to end as currently unusable
          num_unusable_connections++;
          continue;  //move on to the next connection
        }
        
        int total_size = conn.getOutgoingMessageQueue().getTotalSize();
        
        if( total_size < 1 ) {  //oops, all messages have been removed
          addToWaitingList( conn );
          continue;  //move on to the next connection
        }
        
        int mss_size = NetworkManager.getSingleton().getTcpMssSize();
        int num_bytes_allowed = num_bytes_remaining > mss_size ? mss_size : num_bytes_remaining;  //allow a single full packet at most
        int num_bytes_available = total_size > mss_size ? mss_size : total_size;  //allow a single full packet at most
        
        if( num_bytes_allowed >= num_bytes_available ) { //we're allowed enough (for either a full packet or to drain any remaining data)
          int written = 0;
          try {
            written = conn.getOutgoingMessageQueue().deliverToTransport( num_bytes_available, true );
                   
            if( written > 0 ) {  
              manual_notifications.add( conn );  //register it for manual listener notification
            }
            
            boolean has_urgent_data = conn.getOutgoingMessageQueue().hasUrgentMessage();
            int remaining = conn.getOutgoingMessageQueue().getTotalSize();
            
            if( remaining >= mss_size || has_urgent_data ) {  //still has a full packet's worth, or has urgent data
              ready_connections.addLast( conn );  //re-add to end for further writing
              num_unusable_connections = 0;  //reset the unusable count so that it has a chance to try this connection again in the loop
            }
            else {  //connection does not have enough for a full packet, so remove and place into waiting list
              addToWaitingList( conn );
            }
          }
          catch( IOException e ) {  //write exception, so move to waiting list while it waits for removal
            connections_to_notify_of_exception.put( conn, e );  //do exception notification outside of sync'd block
            addToWaitingList( conn );
          }
          
          num_bytes_remaining -= written;
        }
        else {  //we're not allowed enough to maximize the packet payload
          ready_connections.addLast( conn );  //re-add to end as currently unusable
          num_unusable_connections++;
          continue;  //move on to the next connection
        }
      }
    }
    finally {
      lists_lock.exit();
    }
    
    //manual queue listener notifications
    for( int i=0; i < manual_notifications.size(); i++ ) {
      Connection conn = (Connection)manual_notifications.get( i );
      conn.getOutgoingMessageQueue().doListenerNotifications();
    }
    
    //exception notifications
    for( Iterator i = connections_to_notify_of_exception.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry entry = (Map.Entry)i.next();
      Connection conn = (Connection)entry.getKey();
      IOException exception = (IOException)entry.getValue();
      conn.notifyOfException( exception );
    }
    
    int num_bytes_written = num_bytes_to_write - num_bytes_remaining;
    if( num_bytes_written > 0 ) {
      rate_handler.bytesWritten( num_bytes_written );
    }
    
    return num_bytes_written;
  }
  
  
  /**
   * Does this entity have data ready for writing.
   * @return true if it has data to send, false if empty
   */
  public boolean hasWriteDataAvailable() {
    if( ready_connections.isEmpty() )  return false;
    return true;
  }
  
  
  
  private static class PeerData {
    private OutgoingMessageQueue.MessageQueueListener queue_listener;
    private long last_message_added_time;
  }
  
  
  //////////////// RateControlledWriteEntity implementation ////////////////////
  
  public boolean canWrite() {
    if( ready_connections.isEmpty() )  return false;  //no data to send
    if( rate_handler.getCurrentNumBytesAllowed() < 1 )  return false;  //not allowed to send
    return true;
  }
  
  public boolean doWrite() {
    return write( rate_handler.getCurrentNumBytesAllowed() ) > 0 ? true : false;
  }

  public int getPriority() {
    return RateControlledWriteEntity.PRIORITY_HIGH;
  }
  
 ///////////////////////////////////////////////////////////////////////////////
  
}
