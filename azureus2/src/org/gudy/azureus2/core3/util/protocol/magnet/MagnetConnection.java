/*
 * Created on 06-Mar-2005
 * Created by Paul Gardner
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

package org.gudy.azureus2.core3.util.protocol.magnet;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.net.magneturi.MagnetURIHandler;

/**
 * @author parg
 *
 */

public class 
MagnetConnection
	extends HttpURLConnection
{
	protected Socket	socket;
	
	protected
	MagnetConnection(
		URL		_url )
	{
		super( _url );
	}
	
	public void
	connect()
		throws IOException
		
	{
		socket = new Socket( "127.0.0.1", MagnetURIHandler.getSingleton().getPort());
						
		String	get = "GET " + "/download/" + getURL().toString().substring( 7 ) + " HTTP/1.0\r\n";
		
		socket.getOutputStream().write( get.getBytes());
		
		socket.getOutputStream().flush();
	}
	
	public InputStream
	getInputStream()
	
		throws IOException
	{
		return( socket.getInputStream());
	}
	
	public int
	getResponseCode()
	{
		return( HTTP_OK );
	}
	
	public boolean
	usingProxy()
	{
		return( false );
	}
	
	public void
	disconnect()
	{
		try{
			socket.close();
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
}
