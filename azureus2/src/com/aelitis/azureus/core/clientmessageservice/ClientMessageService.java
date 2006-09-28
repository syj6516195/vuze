/*
 * Created on Oct 31, 2005
 * Created by Alon Rohter
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */
package com.aelitis.azureus.core.clientmessageservice;

import java.io.IOException;
import java.util.Map;

/**
 * 
 */
public interface ClientMessageService {
	
	/**
	 * Send the given message to the server service.
	 * NOTE: blocking op
	 * @param message (bencode-able) to send
	 * @throws IOException on error
	 */
	public void sendMessage( Map message ) throws IOException;
	
	
	/**
	 * Receive the next message from the server.
	 * NOTE: blocking op
	 * @return message received
	 * @throws IOException on error
	 */
	public Map receiveMessage() throws IOException;
	
	
	/**
	 * Drop and closedown the connection with the server.
	 */
	public void close();
	
	/**
	 * Override the default max message size
	 * @param max_bytes
	 */
	public void
	setMaximumMessageSize( int max_bytes );
}
