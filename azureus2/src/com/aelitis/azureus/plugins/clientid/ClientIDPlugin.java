/*
 * Created on 29-Dec-2004
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

package com.aelitis.azureus.plugins.clientid;

import java.util.Properties;

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.clientid.ClientIDGenerator;
import org.gudy.azureus2.plugins.torrent.Torrent;

/**
 * @author parg
 *
 */

public class 
ClientIDPlugin 
	implements Plugin
{
	private PluginInterface		plugin_interface;
	
	public static void
	load(
		final PluginInterface	plugin_interface )
	{
		plugin_interface.getClientIDManager().setGenerator( 
			new ClientIDGenerator()
			{
				public byte[]
				generatePeerID(
					Torrent		torrent,
					boolean		for_tracker )
				{
					return( createPeerID());
				}
							
				public void
				generateHTTPProperties(
					Properties	properties )
				{
					doHTTPProperties( plugin_interface, properties );
				}
				
				public String[]
				filterHTTP(
					String[]	lines_in )
				{
					return( lines_in );
				}
			},
			false );
	}
	
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		"Client ID" );		
	}
	

	protected static void
	doHTTPProperties(
		PluginInterface		plugin_interface,
		Properties			properties )
	{
		String	agent = Constants.AZUREUS_NAME + " " + Constants.AZUREUS_VERSION;
		
		if ( plugin_interface.getPluginconfig().getBooleanParameter("Tracker Client Send OS and Java Version")){
							
			agent += ";" + Constants.OSName;
		
			agent += ";Java " + Constants.JAVA_VERSION;
		}
		
		properties.put( ClientIDGenerator.PR_USER_AGENT, agent );
	}
	
	static final String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	public static byte[]
	createPeerID()
	{
		byte[] peerId = new byte[20];
	
		byte[] version = Constants.VERSION_ID;
    
		for (int i = 0; i < 8; i++) {
			peerId[i] = version[i];
		}
    
	 	for (int i = 8; i < 20; i++) {
		  int pos = (int) ( Math.random() * chars.length());
		  peerId[i] = (byte)chars.charAt(pos);
		}
	 	
		// System.out.println( "generated new peer id:" + ByteFormatter.nicePrint(peerId));

	 	return( peerId );
	}
}
