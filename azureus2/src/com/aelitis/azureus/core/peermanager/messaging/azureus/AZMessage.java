/*
 * Created on Feb 20, 2005
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

package com.aelitis.azureus.core.peermanager.messaging.azureus;

import com.aelitis.azureus.core.peermanager.messaging.Message;

/**
 * A core AZ type peer message.
 */
public interface AZMessage extends Message {

  public static final String ID_AZ_HANDSHAKE     = "AZ_HANDSHAKE";
  public static final String ID_AZ_PEER_EXCHANGE = "AZ_PEER_EXCHANGE";
  
  
  public static final byte AZ_DEFAULT_VERSION = (byte)1;
}
