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
 * BitTorrent cancel message.
 */
public class BTCancel implements BTMessage {
  private final DirectByteBuffer buffer;
  private final String description;
  
  private final int piece_number;
  private final int piece_offset;
  private final int length;
  
  
  public BTCancel( int piece_number, int piece_offset, int length ) {
    buffer = new DirectByteBuffer( ByteBuffer.allocate( 12 ) );
    buffer.putInt( DirectByteBuffer.SS_BT, piece_number );
    buffer.putInt( DirectByteBuffer.SS_BT, piece_offset );
    buffer.putInt( DirectByteBuffer.SS_BT, length );
    buffer.flip( DirectByteBuffer.SS_BT );
        
    this.piece_number = piece_number;
    this.piece_offset = piece_offset;
    this.length = length;
    description = BTMessage.ID_BT_CANCEL + " piece #" + piece_number + ": " + piece_offset + "->" + (piece_offset + length -1);
  }
  
  
  
  public int getPieceNumber() {  return piece_number;  }
  
  public int getPieceOffset() {  return piece_offset;  }
  
  public int getLength() {  return length;  }
  
  
  
  public String getID() {  return BTMessage.ID_BT_CANCEL;  }
  
  public byte getVersion() {  return BTMessage.BT_DEFAULT_VERSION;  }
  
  public int getType() {  return Message.TYPE_PROTOCOL_PAYLOAD;  }
    
  public String getDescription() {  return description;  }
  
  public DirectByteBuffer[] getData() {  return new DirectByteBuffer[]{ buffer };  }
  
  public Message deserialize( String id, byte version, DirectByteBuffer data ) throws MessageException {
    if( !id.equals( getID() ) ) {
      throw new MessageException( "decode error: invalid id" );
    }
    
    if( version != getVersion()  ) {
      throw new MessageException( "decode error: invalid version" );
    }
    
    if( data == null ) {
      throw new MessageException( "decode error: data == null" );
    }
    
    if( data.remaining( DirectByteBuffer.SS_MSG ) < 12 ) {
      throw new MessageException( "decode error: payload.remaining() < 12" );
    }
    
    int num = data.getInt( DirectByteBuffer.SS_MSG );
    if( num < 0 ) {
      throw new MessageException( "decode error: num < 0" );
    }
    
    int offset = data.getInt( DirectByteBuffer.SS_MSG );
    if( offset < 0 ) {
      throw new MessageException( "decode error: offset < 0" );
    }
    
    int lngth = data.getInt( DirectByteBuffer.SS_MSG );
    if( lngth < 0 ) {
      throw new MessageException( "decode error: lngth < 0" );
    }
    
    data.returnToPool();
    
    return new BTCancel( num, offset, lngth );
  }

  
  public void destroy() {  /*nothing*/  }
}
