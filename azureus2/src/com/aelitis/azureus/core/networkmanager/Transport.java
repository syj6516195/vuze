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

package com.aelitis.azureus.core.networkmanager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;


/**
 * Represents a peer transport connection (eg. a network socket).
 */
public class Transport {
  
  private static boolean enable_efficient_write = true;
  
  private final SocketChannel socket_channel;
  
  
  protected Transport( SocketChannel channel ) {
    this.socket_channel = channel;
  }
  
  /**
   * Get the socket channel used by the transport.
   * @return the socket channel
   */
  protected SocketChannel getSocketChannel() {  return socket_channel;  }
  
  
  /**
   * Get a textual description for this transport.
   * @return description
   */
  protected String getDescription() {
    return socket_channel.socket().getInetAddress().getHostAddress() + ":" + socket_channel.socket().getPort();
  }
  
    
  /**
   * Write data to the transport from the given buffers.
   * NOTE: Works like GatheringByteChannel.
   * @param buffers th buffers from which bytes are to be retrieved
   * @param array_offset offset within the buffer array of the first buffer from which bytes are to be retrieved
   * @param array_length maximum number of buffers to be accessed
   * @return number of bytes written
   * @throws IOException
   */
  protected long write( ByteBuffer[] buffers, int array_offset, int array_length ) throws IOException {
    if( enable_efficient_write ) {
      try {
        return socket_channel.write( buffers, array_offset, array_length );
      }
      catch( IOException e ) {
        //a bug only fixed in Tiger (1.5 series):
        //http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4854354
        if( e.getMessage().equals( "A non-blocking socket operation could not be completed immediately" ) ) {
          enable_efficient_write = false;
          System.out.println( "ERROR: Multi-buffer socket write failed; switching to single-buffer mode. Upgrade to JRE 1.5 series to fix." );
        }
        throw e;
      }
    }
    
    //single-buffer mode
    long written_sofar = 0;
    for( int i=array_offset; i < array_length; i++ ) {
      int data_length = buffers[ i ].remaining();
      int written = socket_channel.write( buffers[ i ] );
      written_sofar += written;
      if( written < data_length )  break;
    }
    return written_sofar;
  }
  
  
  protected void destroy() {
    //TODO
  }
  
}
