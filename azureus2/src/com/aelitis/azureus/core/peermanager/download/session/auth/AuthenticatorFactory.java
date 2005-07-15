/*
 * Created on Jul 12, 2005
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

package com.aelitis.azureus.core.peermanager.download.session.auth;

import com.aelitis.azureus.core.peermanager.download.session.TorrentSessionAuthenticator;

public class AuthenticatorFactory {

  /**
   * Create a torrent session authenticator of the given type.
   * @param type_id of authenticator
   * @param infohash session infohash
   * @return authenticator or null if unknown auth type
   */
  public static TorrentSessionAuthenticator createAuthenticator( String type_id, byte[] infohash ) {
    
    if( type_id.equals( TorrentSessionAuthenticator.AUTH_TYPE_STANDARD ) ) {
      return new StandardAuthenticator( infohash );
    }
    
    if( type_id.equals( TorrentSessionAuthenticator.AUTH_TYPE_SECURE ) ) {
      return new SecureAuthenticator( infohash );
    }
    
    return null;  //auth type not found
  }
  
}
