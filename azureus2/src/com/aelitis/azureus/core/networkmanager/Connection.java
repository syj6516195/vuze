/*
 * Created on Jul 29, 2004
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


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


import com.aelitis.azureus.core.peermanager.messaging.bittorrent.*;


/**
 * Represents a managed peer connection, over which messages can be sent and received.
 */
public class Connection {
  private final InetSocketAddress remote_address;
  private final TCPTransport tcp_transport;
  private ConnectionListener connection_listener;
  private boolean is_connected;

  private final OutgoingMessageQueue outgoing_message_queue;
  private final IncomingMessageQueue incoming_message_queue;
  
  
  
  
  
  /**
   * Constructor for new OUTbound connection.
   * The connection is not yet established upon instantiation; use connect() to do so.
   * @param _remote_address to connect to
   */
  protected Connection( InetSocketAddress _remote_address ) {
    remote_address = _remote_address;
    tcp_transport = new TCPTransport();
    is_connected = false;
    outgoing_message_queue = new OutgoingMessageQueue( new BTMessageEncoder(), tcp_transport );  //TODO create proper default encoder
    incoming_message_queue = new IncomingMessageQueue( this, new BTMessageDecoder() );  //TODO create proper default decoder
  }
  
  
  /**
   * Constructor for new INbound connection.
   * The connection is assumed to be already established, by the given already-connected channel.
   * @param _remote_channel connected by
   * @param data_already_read bytestream already read during routing
   */
  protected Connection( SocketChannel _remote_channel, ByteBuffer data_already_read ) {
    remote_address = new InetSocketAddress( _remote_channel.socket().getInetAddress(), _remote_channel.socket().getPort() );
    tcp_transport = new TCPTransport( _remote_channel, data_already_read );
    is_connected = true;
    outgoing_message_queue = new OutgoingMessageQueue( new BTMessageEncoder(), tcp_transport );  //TODO create proper default encoder
    incoming_message_queue = new IncomingMessageQueue( this, new BTMessageDecoder() );  //TODO create proper default decoder
  }
  
  
  /**
   * Connect this connection's transport, i.e. establish the peer connection.
   * If this connection is already established (from an incoming connection for example),
   * then this provides a mechanism to register the connection listener, in which case
   * connectSuccess() will be called immediately.
   * @param listener notified on connect success or failure
   */
  public void connect( ConnectionListener listener ) {
    this.connection_listener = listener;
    
    if( is_connected ) {
      connection_listener.connectSuccess();
      return;
    }
    
    tcp_transport.establishOutboundConnection( remote_address, new TCPTransport.ConnectListener() {
      public void connectAttemptStarted() {
        connection_listener.connectStarted();
      }
      
      public void connectSuccess() {
        is_connected = true;
        connection_listener.connectSuccess();
      }
      
      public void connectFailure( Throwable failure_msg ) {
        is_connected = false;
        connection_listener.connectFailure( failure_msg );
      }
    });
  }
  

  
  
  /**
   * Close and shutdown this connection.
   */
  public void close() {
    incoming_message_queue.destroy();
    outgoing_message_queue.destroy();
    tcp_transport.close();
    is_connected = false;
  }
  
  
  /**
   * Inform connection of a thrown exception.
   * @param error exception
   */
  public void notifyOfException( Throwable error ) {
    if( connection_listener != null ) {
      connection_listener.exceptionThrown( error );
    }
    else System.out.println( "connection_listener == null" );
  }
  
  

  
  /**
   * Get the connection's outgoing message queue.
   * @return outbound message queue
   */
  public OutgoingMessageQueue getOutgoingMessageQueue() {  return outgoing_message_queue;  }
  
  
  /**
   * Get the connection's incoming message queue.
   * @return inbound message queue
   */
  public IncomingMessageQueue getIncomingMessageQueue() {  return incoming_message_queue;  }
  
  
  
  
  
  /**
   * Get the connection's data tcp transport interface.
   * @return the transport
   */
  public TCPTransport getTCPTransport() {  return tcp_transport;  }
  
  
  
  /**
   * TEMP METHOD UNTIL SOCKET READING IS HANDLED INTERNALLY  //TODO
   */
  public SocketChannel getSocketChannel() {
    return tcp_transport.getSocketChannel();
  }
  

  
  
  public String toString() {
    return tcp_transport.getDescription();
  }
  
  
  
  
  
  /**
   * Listener for notification of connection events.
   */
  public interface ConnectionListener {
    /**
     * The connection establishment process has started,
     * i.e. the connection is actively being attempted.
     */
    public void connectStarted();    
    
    /**
     * The connection attempt succeeded.
     * The connection is now established.
     * NOTE: Called only during initial connect attempt.
     */
    public void connectSuccess();
    
    /**
     * The connection attempt failed.
     * NOTE: Called only during initial connect attempt.
     * @param failure_msg failure reason
     */
    public void connectFailure( Throwable failure_msg );
    
    /**
     * Handle exception thrown by this connection.
     * NOTE: Can be called at any time during connection lifetime.
     * @param error exception
     */
    public void exceptionThrown( Throwable error );
  }

  
}
