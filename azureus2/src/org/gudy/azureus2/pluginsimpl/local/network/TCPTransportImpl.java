/*
 * Created on Feb 11, 2005
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

import java.io.IOException;
import java.nio.ByteBuffer;

import org.gudy.azureus2.plugins.network.Transport;

import com.aelitis.azureus.core.networkmanager.TCPTransport;

/**
 *
 */
public class TCPTransportImpl implements Transport {
  private final TCPTransport core_transport;
  
  protected TCPTransportImpl( TCPTransport core_transport ) {
    this.core_transport = core_transport;
  }
  
  
  
  public long read( ByteBuffer[] buffers, int array_offset, int length ) throws IOException {
    return core_transport.read( buffers, array_offset, length );
  }
  
 
  public long write( ByteBuffer[] buffers, int array_offset, int length ) throws IOException {
    return core_transport.write( buffers, array_offset, length );
  }
 
}
