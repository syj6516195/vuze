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
import org.gudy.azureus2.pluginsimpl.local.messaging.MessageAdapter;
import org.gudy.azureus2.pluginsimpl.local.messaging.MessageStreamDecoderAdapter;



/**
 *
 */
public class IncomingMessageQueueImpl implements IncomingMessageQueue {
  private final com.aelitis.azureus.core.networkmanager.IncomingMessageQueue core_queue;
  private final HashMap registrations = new HashMap();
  
  
  protected IncomingMessageQueueImpl( com.aelitis.azureus.core.networkmanager.IncomingMessageQueue core_queue ) {
    this.core_queue = core_queue;
  }
  
  
  
  public void setDecoder( MessageStreamDecoder stream_decoder ) {
    core_queue.setDecoder( new MessageStreamDecoderAdapter( stream_decoder ) );
  }
  

  public void registerListener( final IncomingMessageQueueListener listener ) {
    com.aelitis.azureus.core.networkmanager.IncomingMessageQueue.MessageQueueListener core_listener = 
      new com.aelitis.azureus.core.networkmanager.IncomingMessageQueue.MessageQueueListener() {
        public boolean messageReceived( com.aelitis.azureus.core.peermanager.messaging.Message message ) {
          if( message instanceof MessageAdapter ) {
            //the message must have been originally decoded by plugin decoder
            //so just use original plugin message...i.e. unwrap out of MessageAdapter
            return listener.messageReceived( ((MessageAdapter)message).getPluginMessage() );
          }
          
          //message originally decoded by core
          return listener.messageReceived( new MessageAdapter( message ) );
        }
      
        public void protocolBytesReceived( int byte_count ) {  listener.bytesReceived( byte_count );  }

        public void dataBytesReceived( int byte_count ) {  listener.bytesReceived( byte_count );  }
    };
    
    registrations.put( listener, core_listener );  //save this mapping for later
    
    core_queue.registerQueueListener( core_listener );
    
    if( registrations.size() == 1 ) {
      core_queue.startQueueProcessing();
    }
  }
    

  public void deregisterListener( IncomingMessageQueueListener listener ) {
    //retrieve saved mapping
    com.aelitis.azureus.core.networkmanager.IncomingMessageQueue.MessageQueueListener core_listener =
      (com.aelitis.azureus.core.networkmanager.IncomingMessageQueue.MessageQueueListener)registrations.remove( listener );
    
    if( core_listener != null ) {
      core_queue.cancelQueueListener( core_listener );
    }
    
    if( registrations.size() < 1 ) {
      core_queue.stopQueueProcessing();
    }
  }
  
  
  public void notifyOfExternalReceive( Message message ) {
    core_queue.notifyOfExternallyReceivedMessage( new MessageAdapter( message ) );
  }
  
}
