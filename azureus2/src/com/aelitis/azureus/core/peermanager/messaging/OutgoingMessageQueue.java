/*
 * Created on May 8, 2004
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


package com.aelitis.azureus.core.peermanager.messaging;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.TCPTransport;


/**
 * Priority-based outbound peer message queue.
 */
public class OutgoingMessageQueue {
  private final LinkedList 		queue		= new LinkedList();
  private final AEMonitor	queue_mon	= new AEMonitor( "OutgoingMessageQueue:queue" );

  private final ArrayList delayed_notifications = new ArrayList();
  private final AEMonitor delayed_notifications_mon = new AEMonitor( "OutgoingMessageQueue:DN" );

  private volatile ArrayList listeners 		= new ArrayList();  //copied-on-write
  private final AEMonitor listeners_mon		= new AEMonitor( "OutgoingMessageQueue:L");
  
  private int total_size = 0;
  private RawMessage urgent_message = null;
  private boolean destroyed = false;
  
  private MessageStreamEncoder stream_encoder;
  private TCPTransport tcp_transport;
  
  
  /**
   * Create a new outgoing message queue.
   * @param stream_encoder default message encoder
   */
  public OutgoingMessageQueue( MessageStreamEncoder stream_encoder, TCPTransport transport ) {
    this.stream_encoder = stream_encoder;
    this.tcp_transport = transport;
  }
  
  
  /**
   * Set the message stream encoder that will be used to encode outgoing messages.
   * @param stream_encoder to use
   */
  public void setEncoder( MessageStreamEncoder stream_encoder ) {
    this.stream_encoder = stream_encoder;
  }
  
  

  /**
   * Destroy this queue; i.e. perform cleanup actions.
   */
  public void destroy() {
    destroyed = true;
    try{
      queue_mon.enter();
    
      while( !queue.isEmpty() ) {
      	((RawMessage)queue.remove( 0 )).destroy();
      }
    }finally{
      queue_mon.exit();
    }
    total_size = 0;
  }
  
  
  /**
   * Get the total number of bytes ready to be transported.
   * @return total bytes remaining
   */
  public int getTotalSize() {  return total_size;  }
  
  
  /**
   * Whether or not an urgent message (one that needs an immediate send, i.e. a no-delay message) is queued.
   * @return true if there's a message tagged for immediate write
   */
  public boolean hasUrgentMessage() {  return urgent_message == null ? false : true;  }
  
  
  /**
   * Add a message to the message queue.
   * NOTE: Allows for manual listener notification at some later time,
   * using doListenerNotifications(), instead of notifying immediately
   * from within this method.  This is useful if you want to invoke
   * listeners outside of some greater synchronized block to avoid
   * deadlock.
   * @param message message to add
   * @param manual_listener_notify true for manual notification, false for automatic
   */
  public void addMessage( Message message, boolean manual_listener_notify ) {
    
    RawMessage rmesg = stream_encoder.encodeMessage( message );
    
    if( destroyed ) {  //queue is shutdown, drop any added messages
      rmesg.destroy();
      return;
    }
    
    removeMessagesOfType( rmesg.messagesToRemove(), manual_listener_notify );
    try{
      queue_mon.enter();
    
      int pos = 0;
      for( Iterator i = queue.iterator(); i.hasNext(); ) {
        RawMessage msg = (RawMessage)i.next();
        if( rmesg.getPriority() > msg.getPriority() 
          && msg.getRawPayload()[0].position(DirectByteBuffer.SS_NET) == 0 ) {  //but don't insert in front of a half-sent message
          break;
        }
        pos++;
      }
      if( rmesg.isNoDelay() ) {
        urgent_message = rmesg;
      }
      queue.add( pos, rmesg );
      
      DirectByteBuffer[] payload = rmesg.getRawPayload();
      for( int i=0; i < payload.length; i++ ) {
        total_size += payload[i].remaining(DirectByteBuffer.SS_NET);
      }
    }finally{
      queue_mon.exit();
    }
    
    if( manual_listener_notify ) {  //register listener event for later, manual notification
      NotificationItem item = new NotificationItem( NotificationItem.MESSAGE_ADDED );
      item.message = rmesg;
      try {
        delayed_notifications_mon.enter();
        
        delayed_notifications.add( item );
      }
      finally {
        delayed_notifications_mon.exit();
      }
    }
    else { //do listener notification now
      ArrayList listeners_ref = listeners;
    
      for( int i=0; i < listeners_ref.size(); i++ ) {
        MessageQueueListener listener = (MessageQueueListener)listeners_ref.get( i );
        listener.messageAdded( rmesg );
      }
    }
  }
  

  
  /**
   * Remove all messages of the given types from the queue.
   * NOTE: Allows for manual listener notification at some later time,
   * using doListenerNotifications(), instead of notifying immediately
   * from within this method.  This is useful if you want to invoke
   * listeners outside of some greater synchronized block to avoid
   * deadlock.
   * @param message_types type to remove
   * @param manual_listener_notify true for manual notification, false for automatic
   */
  public void removeMessagesOfType( Message[] message_types, boolean manual_listener_notify ) {
    if( message_types == null ) return;
    
    ArrayList messages_removed = null;
    
    try{
      queue_mon.enter();
    
      for( Iterator i = queue.iterator(); i.hasNext(); ) {
        RawMessage msg = (RawMessage)i.next();
        for( int t=0; t < message_types.length; t++ ) {
          boolean same_type = message_types[t].getID().equals( msg.getID() ) && message_types[t].getVersion() == msg.getVersion();
        	if( same_type && msg.getRawPayload()[0].position(DirectByteBuffer.SS_NET) == 0 ) {   //dont remove a half-sent message
            if( msg == urgent_message ) urgent_message = null;
            
            DirectByteBuffer[] payload = msg.getRawPayload();
            for( int x=0; x < payload.length; x++ ) {
              total_size -= payload[x].remaining(DirectByteBuffer.SS_NET);
            }
            
            if( manual_listener_notify ) {
              NotificationItem item = new NotificationItem( NotificationItem.MESSAGE_REMOVED );
              item.message = msg;
              try {
                delayed_notifications_mon.enter();
                
                delayed_notifications.add( item );
              }
              finally {
                delayed_notifications_mon.exit();
              }
            }
            else {
              if ( messages_removed == null ){
              	messages_removed = new ArrayList();
              }
              messages_removed.add( msg );
            }
        		i.remove();
            break;
        	}
        }
      }
    }finally{
      queue_mon.exit();
    }

    if( !manual_listener_notify && messages_removed != null ) {
      //do listener notifications now
      ArrayList listeners_ref = listeners;
        
      for( int x=0; x < messages_removed.size(); x++ ) {
        RawMessage msg = (RawMessage)messages_removed.get( x );
        
        for( int i=0; i < listeners_ref.size(); i++ ) {
          MessageQueueListener listener = (MessageQueueListener)listeners_ref.get( i );
          listener.messageRemoved( msg );
        }
        msg.destroy();
      }
    }
  }
  
  
  /**
   * Remove a particular message from the queue.
   * NOTE: Only the original message found in the queue will be destroyed upon removal,
   * which may not necessarily be the one passed as the method parameter,
   * as some messages override equals() (i.e. BTRequest messages) instead of using reference
   * equality, and could be a completely different object, and would need to be destroyed
   * manually.  If the message does not override equals, then any such method will likely
   * *not* be found and removed, as internal queued object was a new allocation on insertion.
   * NOTE: Allows for manual listener notification at some later time,
   * using doListenerNotifications(), instead of notifying immediately
   * from within this method.  This is useful if you want to invoke
   * listeners outside of some greater synchronized block to avoid
   * deadlock.
   * @param message to remove
   * @param manual_listener_notify true for manual notification, false for automatic
   * @return true if the message was removed, false otherwise
   */
  public boolean removeMessage( Message message, boolean manual_listener_notify ) {
    RawMessage msg_removed = null;
    
    try{
      queue_mon.enter();
    
      int index = queue.indexOf( message );
      if( index != -1 ) {
        RawMessage msg = (RawMessage)queue.get( index );
        if( msg.getRawPayload()[0].position(DirectByteBuffer.SS_NET) == 0 ) {  //dont remove a half-sent message
          if( msg == urgent_message ) urgent_message = null;  
          
          DirectByteBuffer[] payload = msg.getRawPayload();
          for( int x=0; x < payload.length; x++ ) {
            total_size -= payload[x].remaining(DirectByteBuffer.SS_NET);
          }

          queue.remove( index );
          msg_removed = msg;
        }
      }
    }finally{
      queue_mon.exit();
    }
    
    
    if( msg_removed != null ) {
      if( manual_listener_notify ) { //delayed manual notification
        NotificationItem item = new NotificationItem( NotificationItem.MESSAGE_REMOVED );
        item.message = msg_removed;
        try {
          delayed_notifications_mon.enter();
          
          delayed_notifications.add( item );
        }
        finally {
          delayed_notifications_mon.exit();
        }
      }
      else {   //do listener notification now
        ArrayList listeners_ref = listeners;
      
        for( int i=0; i < listeners_ref.size(); i++ ) {
          MessageQueueListener listener = (MessageQueueListener)listeners_ref.get( i );
          listener.messageRemoved( msg_removed );
        }
        msg_removed.destroy();
      }
      return true;
    }
    
    return false;
  }
  
  
  /**
   * Deliver (write) message(s) data to the underlying transport.
   * 
   * NOTE: Allows for manual listener notification at some later time,
   * using doListenerNotifications(), instead of notifying immediately
   * from within this method.  This is useful if you want to invoke
   * listeners outside of some greater synchronized block to avoid
   * deadlock.
   * @param max_bytes maximum number of bytes to deliver
   * @param manual_listener_notify true for manual notification, false for automatic
   * @return number of bytes delivered
   * @throws IOException on delivery error
   */
  public int deliverToTransport( int max_bytes, boolean manual_listener_notify ) throws IOException {    
    if( max_bytes < 1 ) {
      Debug.out( "max_bytes < 1: " +max_bytes );
      return 0;
    }
    
    int data_written = 0;
    int protocol_written = 0;
    
    ArrayList messages_sent = null;
    
    try{
      queue_mon.enter();

    	if( !queue.isEmpty() ) {
        ArrayList raw_buffers = new ArrayList();
        ArrayList orig_positions = new ArrayList();
        int total_sofar = 0;
        
        for( Iterator i = queue.iterator(); i.hasNext(); ) {
          DirectByteBuffer[] payloads = ((RawMessage)i.next()).getRawPayload();
          boolean stop = false;
          
          for( int x=0; x < payloads.length; x++ ) {
            ByteBuffer buff = payloads[x].getBuffer( DirectByteBuffer.SS_NET );
            raw_buffers.add( buff );
            orig_positions.add( new Integer( buff.position() ) );
            total_sofar += buff.remaining();
            
            if( total_sofar >= max_bytes ) {
              stop = true;
              break;
            }
          }
          
          if( stop )  break;
        }
                
        int num_raw = raw_buffers.size();
        
        ByteBuffer last_buff = (ByteBuffer)raw_buffers.get( num_raw - 1 );
        int orig_last_limit = last_buff.limit();
    		if( total_sofar > max_bytes ) {
          last_buff.limit( orig_last_limit - (total_sofar - max_bytes) );
    		}
        
        ByteBuffer[] buffs = new ByteBuffer[ num_raw ];
        raw_buffers.toArray( buffs );
        
        tcp_transport.write( buffs, 0, num_raw );
        
        last_buff.limit( orig_last_limit );
        
        int pos = 0;
        boolean stop = false;
        
        while( !queue.isEmpty() && !stop ) {
          RawMessage msg = (RawMessage)queue.get( 0 );
          DirectByteBuffer[] payloads = msg.getRawPayload();
                    
          for( int x=0; x < payloads.length; x++ ) {
            ByteBuffer bb = payloads[x].getBuffer( DirectByteBuffer.SS_NET );
            
            int bytes_written = (bb.limit() - bb.remaining()) - ((Integer)orig_positions.get( pos )).intValue();
            total_size -= bytes_written;
            
            if( msg.getType() == Message.TYPE_DATA_PAYLOAD ) {
              data_written += bytes_written;
            }
            else {
              protocol_written += bytes_written;
            }
            
            if( bb.hasRemaining() ) {  //still data left to send in this message
              stop = true;  //so don't bother checking later messages for completion
              break;
            }
            else if( x == payloads.length - 1 ) {  //last payload buffer of message is empty
              if( msg == urgent_message ) urgent_message = null;
            
              queue.remove( 0 );
              
              if( manual_listener_notify ) {
                NotificationItem item = new NotificationItem( NotificationItem.MESSAGE_SENT );
                item.message = msg;
                item.transport = tcp_transport;
                try {  delayed_notifications_mon.enter();
                  delayed_notifications.add( item );
                } finally {  delayed_notifications_mon.exit();  }
              }
              else {
                if( messages_sent == null ) {
                  messages_sent = new ArrayList();
                }
                messages_sent.add( msg );
              }
            }
            
            pos++;
            if( pos >= num_raw ) {
              stop = true;
              break;
            }
          }
        }
    	}
    }finally{
      queue_mon.exit();
    }
    
    if( data_written + protocol_written > 0 ) {
      if( manual_listener_notify ) {
        
        if( data_written > 0 ) {  //data bytes notify
          NotificationItem item = new NotificationItem( NotificationItem.DATA_BYTES_SENT );
          item.byte_count = data_written;
          try {
            delayed_notifications_mon.enter();
            
            delayed_notifications.add( item );
          }
          finally {
            delayed_notifications_mon.exit();
          }
        }

        if( protocol_written > 0 ) {  //protocol bytes notify
          NotificationItem item = new NotificationItem( NotificationItem.PROTOCOL_BYTES_SENT );
          item.byte_count = protocol_written;
          try {
            delayed_notifications_mon.enter();
            
            delayed_notifications.add( item );
          }
          finally {
            delayed_notifications_mon.exit();
          }
        }
      }
      else {  //do listener notification now
        ArrayList listeners_ref = listeners;
        
        int num_listeners = listeners_ref.size();
        for( int i=0; i < num_listeners; i++ ) {
          MessageQueueListener listener = (MessageQueueListener)listeners_ref.get( i );

          if( data_written > 0 )  listener.dataBytesSent( data_written );
          if( protocol_written > 0 )  listener.protocolBytesSent( protocol_written );
          
          if ( messages_sent != null ){
          	
	          for( int x=0; x < messages_sent.size(); x++ ) {
	            RawMessage msg = (RawMessage)messages_sent.get( x );
	
	            listener.messageSent( msg );
	            
	            if( i == num_listeners - 1 ) {  //the last listener notification, so destroy
	              LGLogger.log( LGLogger.CORE_NETWORK, "Sent " +msg.getDescription()+ " message to " + tcp_transport.getDescription() );
	              msg.destroy();
	            }
	          }
          }
        }
      }
    }
    
    return data_written + protocol_written;
  }
  
  
  /**
   * Manually send any unsent listener notifications.
   */
  public void doListenerNotifications() {
    ArrayList notifications_copy;
    try {
      delayed_notifications_mon.enter();
      
      if( delayed_notifications.size() == 0 )  return;
      notifications_copy = new ArrayList( delayed_notifications );
      delayed_notifications.clear();
    }
    finally {
      delayed_notifications_mon.exit();
    }
    
    ArrayList listeners_ref = listeners;
    
    for( int j=0; j < notifications_copy.size(); j++ ) {  //for each notification
      NotificationItem item = (NotificationItem)notifications_copy.get( j );

      switch( item.type ) {
        case NotificationItem.MESSAGE_ADDED:
          for( int i=0; i < listeners_ref.size(); i++ ) {  //for each listener
            MessageQueueListener listener = (MessageQueueListener)listeners_ref.get( i );
            listener.messageAdded( item.message );
          }
          break;
          
        case NotificationItem.MESSAGE_REMOVED:
          for( int i=0; i < listeners_ref.size(); i++ ) {  //for each listener
            MessageQueueListener listener = (MessageQueueListener)listeners_ref.get( i );
            listener.messageRemoved( item.message );
          }
          item.message.destroy();
          break;
          
        case NotificationItem.MESSAGE_SENT:
          for( int i=0; i < listeners_ref.size(); i++ ) {  //for each listener
            MessageQueueListener listener = (MessageQueueListener)listeners_ref.get( i );
            listener.messageSent( item.message );
          }
          LGLogger.log( LGLogger.CORE_NETWORK, "Sent " +item.message.getDescription()+ " message to " + item.transport.getDescription() );
          item.message.destroy();
          break;
          
        case NotificationItem.PROTOCOL_BYTES_SENT:
          for( int i=0; i < listeners_ref.size(); i++ ) {  //for each listener
            MessageQueueListener listener = (MessageQueueListener)listeners_ref.get( i );
            listener.protocolBytesSent( item.byte_count );
          }
          break;
          
        case NotificationItem.DATA_BYTES_SENT:
          for( int i=0; i < listeners_ref.size(); i++ ) {  //for each listener
            MessageQueueListener listener = (MessageQueueListener)listeners_ref.get( i );
            listener.dataBytesSent( item.byte_count );
          }
          break;
          
        default:
          Debug.out( "NotificationItem.type unknown :" + item.type );
      }
    }
  }
  

  /////////////////////////////////////////////////////////////////
  
  /**
   * Receive notification of queue events.
   */
  public interface MessageQueueListener {
    /**
     * The given message has just been queued for sending out the transport.
     * @param message queued
     */
    public void messageAdded( Message message );
    
    /**
     * The given message has just been forcibly removed from the queue,
     * i.e. it was *not* sent out the transport.
     * @param message removed
     */
    public void messageRemoved( Message message );
    
    /**
     * The given message has been completely sent out through the transport.
     * @param message sent
     */
    public void messageSent( Message message );
    
    /**
     * The given number of protocol (overhead) bytes has been written to the transport.
     * @param byte_count number of protocol bytes
     */
    public void protocolBytesSent( int byte_count );
    
    
    /**
     * The given number of (piece) data bytes has been written to the transport.
     * @param byte_count number of data bytes
     */
    public void dataBytesSent( int byte_count );
  }
  

  
  /**
   * Add a listener to be notified of queue events.
   * @param listener
   */
  public void registerQueueListener( MessageQueueListener listener ) {
    try{  listeners_mon.enter();
      //copy-on-write
      ArrayList new_list = new ArrayList( listeners.size() + 1 );
      new_list.addAll( listeners );
      new_list.add( listener );
      listeners = new_list;
    }
    finally{  listeners_mon.exit();  }
  }
  
  
  /**
   * Cancel queue event notification listener.
   * @param listener
   */
  public void cancelQueueListener( MessageQueueListener listener ) {
    try{  listeners_mon.enter();
      //copy-on-write
      ArrayList new_list = new ArrayList( listeners );
      new_list.remove( listener );
      listeners = new_list;
    }
    finally{  listeners_mon.exit();  }
  }

  
  private static class NotificationItem {
    private static final int MESSAGE_ADDED        = 0;
    private static final int MESSAGE_REMOVED      = 1;
    private static final int MESSAGE_SENT         = 2;
    private static final int DATA_BYTES_SENT      = 3;
    private static final int PROTOCOL_BYTES_SENT  = 4;
    private final int type;
    private RawMessage message;
    private TCPTransport transport;
    private int byte_count = 0;
    private NotificationItem( int notification_type ) {
      type = notification_type;
    }
  }
  
}
