/*
 * Created on Oct 29, 2005
 * Created by Alon Rohter
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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
package com.aelitis.azureus.core.clientmessageservice.impl;

import java.util.Map;

import com.aelitis.azureus.core.clientmessageservice.impl.ClientConnection;



/**
 * 
 */
public class ClientMessage {
	private final String message_id;
	private final ClientConnection client;
	private final Map payload;
	private final ClientMessageHandler handler;
	
	
	public ClientMessage( String msg_id, ClientConnection _client, Map msg_payload, ClientMessageHandler _handler ) {
		this.message_id = msg_id;
		this.client = _client;
		this.payload = msg_payload;
		this.handler = _handler;
	}
	
	
	public String getMessageID(){  return message_id;  }
	
	public ClientConnection getClient(){  return client;  }
	
	public Map getPayload(){  return payload;  }
	
	public ClientMessageHandler getHandler(){  return handler;  }

}
