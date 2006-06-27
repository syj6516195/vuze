/*
 * Created on 22 Jun 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.networkmanager.impl.udp;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.RandomUtils;

import com.aelitis.net.udp.uc.PRUDPPacket;

public class 
UDPNetworkManager 
{
	private static final int MIN_MSS = 128;
	private static final int MAX_MSS = PRUDPPacket.MAX_PACKET_SIZE;
	
	private static int udp_mss_size;
	
	public static int getUdpMssSize() {  return udp_mss_size;  }

	public static void
	refreshRates(
		int		min_rate )
	{
			// TODO: adjust this!!!!
		
		udp_mss_size = COConfigurationManager.getIntParameter( "network.udp.mtu.size" ) - 40; 	        

	    if( udp_mss_size > min_rate )  udp_mss_size = min_rate - 1;
	    
	    if( udp_mss_size < MIN_MSS )  udp_mss_size = MIN_MSS; 
	    
	    if ( udp_mss_size > MAX_MSS ) udp_mss_size = MAX_MSS;
	}
	
	private static UDPNetworkManager	singleton = new UDPNetworkManager();
	

	public static UDPNetworkManager
	getSingleton()
	{
		return( singleton );
	}
	
	private int udp_listen_port	= -1;

	private UDPConnectionManager	connection_manager;
	
	protected
	UDPNetworkManager()
	{
		COConfigurationManager.addAndFireParameterListener( 
			   "UDP.Listen.Port", 
			   new ParameterListener() 
			   {
				   public void 
				   parameterChanged(String name) 
				   {
					   int port = COConfigurationManager.getIntParameter( name );
					   
					   if ( port == udp_listen_port ){
						   
						   return;
					   }
					   
					   if ( port < 0 || port > 65535 || port == 6880 ) {
						   
					        String msg = "Invalid incoming UDP listen port configured, " +port+ ". The port has been reset. Please check your config!";
					        
					        Debug.out( msg );
					        
					        Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_ERROR, msg));
					        
					        udp_listen_port = RandomUtils.generateRandomNetworkListenPort();
					        
					        COConfigurationManager.setParameter( name, udp_listen_port );
					        
					    }else{
					
					    	udp_listen_port	= port;
					    }
				   }
			   });
	   
		connection_manager = new UDPConnectionManager( udp_listen_port );
	}
	
  
	public int 
	getUDPListeningPortNumber()
	{
		return( udp_listen_port );
	}
	
	public UDPConnectionManager
	getConnectionManager()
	{
		return( connection_manager );
	}
}
