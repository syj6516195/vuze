/*
 * Created on Apr 30, 2004
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

package com.aelitis.azureus.core.peermanager.messaging.bittorrent;

import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;



/**
 * BitTorrent request message.
 * NOTE: Overrides equals()
 */
public class BTRequest implements BTMessage {
  private final DirectByteBuffer buffer;
  private final int piece_number;
  private final int piece_offset;
  private final int length;
  private final int hashcode;
  
  
  public BTRequest( int piece_number, int piece_offset, int length ) {
    this.piece_number = piece_number;
    this.piece_offset = piece_offset;
    this.length = length;
    this.hashcode = piece_number + piece_offset + length;
    
    buffer = new DirectByteBuffer( ByteBuffer.allocate( 12 ) );  //MUST BE NON-DIRECT!
    buffer.putInt( DirectByteBuffer.SS_BT, piece_number );
    buffer.putInt( DirectByteBuffer.SS_BT, piece_offset );
    buffer.putInt( DirectByteBuffer.SS_BT, length );
    buffer.flip( DirectByteBuffer.SS_BT );
  }
  
  
  /**
   * Used for creating a lightweight message-type comparison message.
   */
  public BTRequest() {
    buffer = null;
    piece_number = -1;
    piece_offset = -1;
    length = -1;
    hashcode = -1;
  }
  
  
  public int getPieceNumber() {  return piece_number;  }
  
  public int getPieceOffset() {  return piece_offset;  }
  
  public int getLength() {  return length;  }
  
  
    
  public String getID() {  return BTMessage.ID_BT_REQUEST;  }
  
  public byte getVersion() {  return BTMessage.BT_DEFAULT_VERSION;  }
  
  public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }
    
  public String getDescription() {
    return BTMessage.ID_BT_REQUEST + " piece #" + piece_number + ": " + piece_offset + "->" + (piece_offset + length -1);
  }
  
  public DirectByteBuffer[] getData() {  return new DirectByteBuffer[]{ buffer };  }
  
  public Message deserialize( DirectByteBuffer data ) throws MessageException {   
    if( data == null ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: data == null" );
    }
    
    if( data.remaining( DirectByteBuffer.SS_MSG ) < 12 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: payload.remaining[" +data.remaining( DirectByteBuffer.SS_MSG )+ "] < 12" );
    }
    
    int num = data.getInt( DirectByteBuffer.SS_MSG );
    if( num < 0 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: num < 0" );
    }
    
    int offset = data.getInt( DirectByteBuffer.SS_MSG );
    if( offset < 0 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: offset < 0" );
    }
    
    int lngth = data.getInt( DirectByteBuffer.SS_MSG );
    if( lngth < 0 ) {
      throw new MessageException( "[" +getID() + ":" +getVersion()+ "] decode error: lngth < 0" );
    }
    
    data.returnToPool();
    
    return new BTRequest( num, offset, lngth );
  }
  
  
  public void destroy() { /*nothing*/ } 
  
  
  //used for removing individual requests from the message queue
  public boolean equals( Object obj ) {
    if( this == obj )  return true;
    if( obj != null && obj instanceof BTRequest ) {
      BTRequest other = (BTRequest)obj;
      if( other.piece_number == this.piece_number &&
          other.piece_offset == this.piece_offset &&
          other.length == this.length )  return true;
    }
    return false;
  }

  public int hashCode() {  return hashcode;  }
}
