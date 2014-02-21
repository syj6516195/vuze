/*
 * Created on 06-Dec-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.proxy;

import java.net.Proxy;
import java.net.URL;
import java.util.List;

import org.gudy.azureus2.plugins.PluginInterface;

import com.aelitis.azureus.core.proxy.impl.*;

/**
 * @author parg
 *
 */

public class 
AEProxyFactory 
{
		/**
		 * @param port				0 = free port
		 * @param connect_timeout	0 = no timeout
		 * @param read_timeout		0 = no timeout
		 * @return
		 * @throws AEProxyException
		 */
	
	public static AEProxy
	create(
		int					port,
		long				connect_timeout,
		long				read_timeout,
		AEProxyHandler		state_factory )	
	
		throws AEProxyException
	{
		return( new AEProxyImpl(port,connect_timeout,read_timeout,state_factory));
	}
	
	public static AEProxyAddressMapper
	getAddressMapper()
	{
		return( AEProxyAddressMapperImpl.getSingleton());
	}
	
	public static PluginProxy
	getPluginProxy(
		String		reason,
		URL			target )
	{
		return( getPluginProxy( reason, target, false));
	}
	
	public static PluginProxy
	getPluginProxy(
		String		reason,
		URL			target,
		boolean		can_wait )
	{
		return( AEPluginProxyHandler.getPluginProxy( reason, target, can_wait ));
	}
	
	public static PluginProxy
	getPluginProxy(
		String		reason,
		String		host,
		int			port )
	{
		return( AEPluginProxyHandler.getPluginProxy( reason, host, port ));
	}
		
	public static PluginProxy
	getPluginProxy(
		Proxy		proxy )
	{
		return( AEPluginProxyHandler.getPluginProxy( proxy ));
	}
	
	public static Boolean
	testPluginHTTPProxy(
		URL				target,
		boolean			can_wait )
	{
		return( AEPluginProxyHandler.testPluginHTTPProxy( target, can_wait ));

	}
	
	public static PluginHTTPProxy
	getPluginHTTPProxy(
		String			reason,
		URL				target,
		boolean			can_wait )
	{
		return( AEPluginProxyHandler.getPluginHTTPProxy( reason, target, can_wait ));
	}
	
	public static List<PluginInterface>
	getPluginHTTPProxyProviders(
		boolean	can_wait )
	{
		return( AEPluginProxyHandler.getPluginHTTPProxyProviders( can_wait ));
	}
	
	public static boolean
	hasPluginProxy()
	{
		return( AEPluginProxyHandler.hasPluginProxy());
	}
	
	public interface
	PluginProxy
	{
		public PluginProxy
		getChildProxy(
			String		reason,
			URL			url );
		
		public Proxy
		getProxy();
		
		public URL
		getURL();
		
		public String
		getURLHostRewrite();
		
		public String
		getHost();
		
		public int
		getPort();
		
		public void
		setOK(
			boolean	good );
	}
	
	public interface
	PluginHTTPProxy
	{
		public Proxy
		getProxy();
		
		public String
		proxifyURL(
			String		url );
		
		public void
		destroy();
	}
}
