/*
 * Created on Jan 11, 2005
 * Created by Alon Rohter
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
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

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.messaging.MessageException;


/**
 * Basic raw message implementation used internally for
 * Message-->RawMessage conversions.
 */
public class RawMessageImpl implements RawMessage {
  private final Message message;
  private final DirectByteBuffer[] payload;
  private final int priority;
  private final boolean is_no_delay;
  private final Message[] to_remove;
  
  
  /**
   * Create a new raw message using the given parameters.
   * @param source original message
   * @param raw_payload headers + original message data
   * @param priority in queue
   * @param is_no_delay is an urgent message
   * @param to_remove message types to auto-remove upon queue
   */  
  public RawMessageImpl( Message source,
                            DirectByteBuffer[] raw_payload,
                            int priority,
                            boolean is_no_delay,
                            Message[] to_remove ) {
    this.message = source;
    this.payload = raw_payload;
    this.priority = priority;
    this.is_no_delay = is_no_delay;
    this.to_remove = to_remove;
  }
  
  //message impl
  public String getID() {  return message.getID();  }
  
  public byte getVersion() {  return message.getVersion();  }
  
  public int getType() {  return message.getType();  }
  
  public String getDescription() {  return message.getDescription();  }
  
  public DirectByteBuffer[] getData() {  return message.getData();  }
  
  public Message deserialize( String id, byte version, DirectByteBuffer data ) throws MessageException {
    return message.deserialize( id, version, data );
  }
  
  
  //rawmessage impl
  public DirectByteBuffer[] getRawPayload() {  return payload;  }
  
  public int getPriority() {  return priority;  }
  
  public boolean isNoDelay() {  return is_no_delay;  }
  
  public Message[] messagesToRemove() {  return to_remove;  }
  
  public void destroy() {
    //NOTE: Assumes that the raw payload is made up of the original
    //      message data buffers plus some header data, so returning
    //      the raw buffers will therefore also take care of the data
    //      buffers return.
    for( int i=0; i < payload.length; i++ ) {
      payload[i].returnToPool();
    }
  }
  
}
