/*
 * Created on 29-Jun-2004
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

package org.gudy.azureus2.core3.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author parg
 *
 */

public class 
HostNameToIPResolver 
{
	public static final String	HT_NORMAL		= "N";
	public static final String	HT_I2P			= "I";
	public static final String	HT_TOR			= "T";
	
	static protected Thread			resolver_thread;
	
	static protected List			request_queue		= new ArrayList();
	
	static protected AEMonitor		request_queue_mon	= new AEMonitor( "HostNameToIPResolver" );

	static protected AESemaphore	request_semaphore	= new AESemaphore("HostNameToIPResolver");
	
	public static String
	categoriseAddress(
		String	str )
	{
		int	last_dot = str.lastIndexOf('.');
		
		if ( last_dot == -1 ){
			
			return( HT_NORMAL );	// no idea really, treat as normal
		}
		
		String	dom = str.substring(last_dot+1).toLowerCase();
		
		if ( dom.equals( "i2p" )){
			
			return( HT_I2P );
			
		}else if ( dom.equals( "onion" )){
			
			return( HT_TOR );
		}
		
		return( HT_NORMAL );
	}
	
	public static boolean
	isNonDNSName(
		String	host )
	{
		return( categoriseAddress( host ) != HT_NORMAL );
	}
	
	public static InetAddress
	syncResolve(
		String	host )
	
		throws UnknownHostException
	{
		if ( isNonDNSName( host )){
			
			throw( new HostNameToIPResolverException( "non-DNS name '" + host + "'", true ));
		}
		
		return( InetAddress.getByName( host));	
	}
	
	public static void
	addResolverRequest(
		String							host,
		HostNameToIPResolverListener	l )
	{
		byte[]	bytes = textToNumericFormat( host );
		
		if ( bytes != null ){
		
			try{
				l.hostNameResolutionComplete( InetAddress.getByAddress( host, bytes ));
			
				return;
				
			}catch( UnknownHostException e ){
			}
		}
		
		try{
			request_queue_mon.enter();
			
			request_queue.add( new request( host, l ));
			
			request_semaphore.release();
			
			if ( resolver_thread == null ){
				
				resolver_thread = 
					new AEThread("HostNameToIPResolver")
					{
						public void
						runSupport()
						{
							while(true){
								
								try{
									request_semaphore.reserve();
									
									request	req;
									
									try{
										request_queue_mon.enter();
										
										req	= (request)request_queue.remove(0);
										
									}finally{
										
										request_queue_mon.exit();
									}
									
									try{
										InetAddress addr = syncResolve( req.getHost());
										
										req.getListener().hostNameResolutionComplete( addr );
											
									}catch( Throwable e ){
										
										req.getListener().hostNameResolutionComplete( null );
										
									}
								}catch( Throwable e ){
									
									Debug.printStackTrace( e );
								}
							}
						}
					};
					
				resolver_thread.setDaemon( true );	
					
				resolver_thread.start();
			}
		}finally{
			
			request_queue_mon.exit();
		}
	}
	
		// this has been copied from Inet4Address - need to change for IPv6
	
	final static int INADDRSZ	= 4;
	
	static byte[] textToNumericFormat(String src)
	    {
		if (src.length() == 0) {
		    return null;
		}
		
		int octets;
		char ch;
		byte[] dst = new byte[INADDRSZ];
	        char[] srcb = src.toCharArray();
		boolean saw_digit = false;

		octets = 0;
		int i = 0;
		int cur = 0;
		while (i < srcb.length) {
		    ch = srcb[i++];
		    if (Character.isDigit(ch)) {
			// note that Java byte is signed, so need to convert to int
			int sum = (dst[cur] & 0xff)*10
			    + (Character.digit(ch, 10) & 0xff);
			
			if (sum > 255)
			    return null;

			dst[cur] = (byte)(sum & 0xff);
			if (! saw_digit) {
			    if (++octets > INADDRSZ)
				return null;
			    saw_digit = true;
			}
		    } else if (ch == '.' && saw_digit) {
			if (octets == INADDRSZ)
			    return null;
			cur++;
			dst[cur] = 0;
			saw_digit = false;
		    } else
			return null;
		}
		if (octets < INADDRSZ)
		    return null;
		return dst;
	}
	
	
	
	protected static class
	request
	{
		protected String						host;
		protected HostNameToIPResolverListener	listener;
		
		protected
		request(
			String							_host,
			HostNameToIPResolverListener	_listener )
		{
			host			= _host;
			listener		= _listener;
		}
		
		protected String
		getHost()
		{
			return( host );
		}
		
		protected HostNameToIPResolverListener
		getListener()
		{
			return( listener );
		}
	}
}
