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

package org.gudy.azureus2.plugins.network;

import org.gudy.azureus2.plugins.messaging.MessageStreamDecoder;


/**
 * Inbound message queue.
 */
public interface IncomingMessageQueue {

  /**
   * Set the message stream decoder that will be used to decode incoming messages.
   * @param stream_decoder to use
   */
  public void setDecoder( MessageStreamDecoder stream_decoder );
  
  
  /**
   * Register queue listener.
   * @param listener to register
   */
  public void registerListener( IncomingMessageQueueListener listener );
  
  
  /**
   * Remove registration of queue listener.
   * @param listener to remove
   */
  public void deregisterListener( IncomingMessageQueueListener listener );
  
}
