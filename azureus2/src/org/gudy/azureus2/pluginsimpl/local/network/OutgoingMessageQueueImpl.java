/*
 * Created on Feb 9, 2005
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

package org.gudy.azureus2.pluginsimpl.local.network;

import java.util.HashMap;

import org.gudy.azureus2.plugins.messaging.*;
import org.gudy.azureus2.plugins.network.*;
import org.gudy.azureus2.pluginsimpl.local.messaging.AdapterMessageImpl;




/**
 *
 */
public class OutgoingMessageQueueImpl implements OutgoingMessageQueue {
  private final com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue core_queue;
  private final HashMap registrations = new HashMap();
  
  
  protected OutgoingMessageQueueImpl( com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue core_queue ) {
    this.core_queue = core_queue;
  }
  
  
  public void setEncoder( final MessageStreamEncoder encoder ) {
    core_queue.setEncoder( new com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder() {
      public com.aelitis.azureus.core.networkmanager.RawMessage encodeMessage( com.aelitis.azureus.core.peermanager.messaging.Message message ) {
        RawMessage raw_plug = encoder.encodeMessage( new AdapterMessageImpl( message ) );
        return new AdapterRawMessageImpl( raw_plug );
      }
    });
  }
  

  public void sendMessage( Message message ) {
    core_queue.addMessage( new AdapterMessageImpl( message ), false );
  }
  

  public void registerListener( final OutgoingMessageQueueListener listener ) {
    com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue.MessageQueueListener core_listener =
      new com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue.MessageQueueListener() {
      
        public boolean messageAdded( com.aelitis.azureus.core.peermanager.messaging.Message message ) {
          return listener.messageAdded( new AdapterMessageImpl( message ) );
        }

        public void messageQueued( com.aelitis.azureus.core.peermanager.messaging.Message message ) {  /*nothing*/  }
        public void messageRemoved( com.aelitis.azureus.core.peermanager.messaging.Message message ) {  /*nothing*/  }

        public void messageSent( com.aelitis.azureus.core.peermanager.messaging.Message message ) {
          listener.messageSent( new AdapterMessageImpl( message ) );
        }

        public void protocolBytesSent( int byte_count ) {  listener.bytesSent( byte_count );  }

        public void dataBytesSent( int byte_count ) {  listener.bytesSent( byte_count );  }
    };
    
    registrations.put( listener, core_listener );  //save this mapping for later
    
    core_queue.registerQueueListener( core_listener );
  }
  

  public void deregisterListener( OutgoingMessageQueueListener listener ) {
    //retrieve saved mapping
    com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue.MessageQueueListener core_listener =
      (com.aelitis.azureus.core.networkmanager.OutgoingMessageQueue.MessageQueueListener)registrations.remove( listener );
    
    if( core_listener != null ) {
      core_queue.cancelQueueListener( core_listener );
    }
  }
  
}
