/*
 * Created on 24-Jan-2005
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

package com.aelitis.azureus.plugins.dht;

import java.net.InetSocketAddress;
import java.util.Properties;

import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.HostNameToIPResolver;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.model.*;

import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTFactory;
import com.aelitis.azureus.core.dht.transport.DHTTransport;
import com.aelitis.azureus.core.dht.transport.DHTTransportFactory;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;

/**
 * @author parg
 *
 */

public class 
DHTPlugin
	implements Plugin
{
	private PluginInterface		plugin_interface;
	
	private LoggerChannel		log;
	
	public void
	initialize(
		PluginInterface 	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
				
		plugin_interface.getPluginProperties().setProperty( "plugin.name", "DHT" );

		log = plugin_interface.getLogger().getChannel("DHT");

		UIManager	ui_manager = plugin_interface.getUIManager();

		final BasicPluginViewModel model = 
			ui_manager.createBasicPluginViewModel( "DHT");
		
		BasicPluginConfigModel	config = ui_manager.createBasicPluginConfigModel( "Plugins", "DHT" );
			
		//config.addLabelParameter2( "download.removerules.unauthorised.info" );
		
		log.addListener(
				new LoggerChannelListener()
				{
					public void
					messageLogged(
						int		type,
						String	message )
					{
						model.getLogArea().appendText( message+"\n");
					}
					
					public void
					messageLogged(
						String		str,
						Throwable	error )
					{
						model.getLogArea().appendText( error.toString()+"\n");
					}
				});
		
		
		Thread t = 
			new AEThread( "DTDPlugin.init" )
			{
				public void
				runSupport()
				{
					try{
						int	port = plugin_interface.getPluginconfig().getIntParameter( "TCP.Listen.Port" );
						
						DHTTransportUDP transport = DHTTransportFactory.createUDP( port, 5, 30000,log );
						
						DHT	dht = DHTFactory.create( transport, new Properties(), log );
						
						transport.importContact(new InetSocketAddress( "213.186.46.164", 6881 ));
						
						dht.integrate();
						
						log.log( "DHT integration complete" );
						
						dht.print();
						
					}catch( Throwable e ){
						
						log.log( "DHT integrtion fails", e );
					}
				}
			};
			
		t.setDaemon(true);
		
		t.start();
	}
}
