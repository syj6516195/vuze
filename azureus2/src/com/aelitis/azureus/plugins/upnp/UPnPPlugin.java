/*
 * Created on 14-Jun-2004
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

package com.aelitis.azureus.plugins.upnp;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.URL;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.ui.config.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEMonitor;

import com.aelitis.net.upnp.*;
import com.aelitis.net.upnp.services.*;

public class 
UPnPPlugin
	implements Plugin, UPnPMappingListener
{
	protected PluginInterface		plugin_interface;
	protected LoggerChannel 		log;
	
	protected UPnPMappingManager	mapping_manager	= UPnPMappingManager.getSingleton();
	
	protected UPnP	upnp;
	
	protected BooleanParameter	alert_success_param;
	protected BooleanParameter	grab_ports_param;
	protected BooleanParameter	alert_other_port_param;
	protected BooleanParameter	alert_device_probs_param;
	protected BooleanParameter	release_mappings_param;
	
	protected List	mappings	= new ArrayList();
	protected List	services	= new ArrayList();
	
	protected Map	root_info_map	= new HashMap();
	
	protected AEMonitor	this_mon 	= new AEMonitor( "UPnPPlugin" );
	   
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		"Universal Plug and Play (UPnP)" );
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{	
				}
				
				public void
				closedownInitiated()
				{
					if ( services.size() == 0 ){
						
						plugin_interface.getPluginconfig().setPluginParameter( "plugin.info", "" );
					}
				}
				
				public void
				closedownComplete()
				{
					closeDown( true );
				}
			});
		
		log = plugin_interface.getLogger().getChannel("UPnP");

		UIManager	ui_manager = plugin_interface.getUIManager();
		
		final BasicPluginViewModel model = 
			ui_manager.createBasicPluginViewModel( 
					"UPnP");
		
		BasicPluginConfigModel	config = ui_manager.createBasicPluginConfigModel( "Plugins", "UPnP" );
		
		config.addLabelParameter2( "upnp.info" );
		
		ActionParameter	wiki = config.addActionParameter2( "Utils.link.visit", "MainWindow.about.internet.wiki" );
		
		wiki.setStyle( ActionParameter.STYLE_LINK );
		
		wiki.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					try{
						plugin_interface.getUIManager().openURL( new URL( "http://azureus.aelitis.com/wiki/index.php/UPnP" ));
						
					}catch( Throwable e ){
						
						e.printStackTrace();
					}
				}
			});
		
		final BooleanParameter enable_param = 
			config.addBooleanParameter2( "upnp.enable", "upnp.enable", true );
		
		
		grab_ports_param = config.addBooleanParameter2( "upnp.grabports", "upnp.grabports", false );
		
		release_mappings_param	 = config.addBooleanParameter2( "upnp.releasemappings", "upnp.releasemappings", true );

		ActionParameter refresh_param = config.addActionParameter2( "upnp.refresh.label", "upnp.refresh.button" );
		
		refresh_param.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					upnp.reset();
				}
			});

		
		config.addLabelParameter2( "blank.resource" );
		
		alert_success_param = config.addBooleanParameter2( "upnp.alertsuccess", "upnp.alertsuccess", false );
		
		alert_other_port_param = config.addBooleanParameter2( "upnp.alertothermappings", "upnp.alertothermappings", true );
		
		alert_device_probs_param = config.addBooleanParameter2( "upnp.alertdeviceproblems", "upnp.alertdeviceproblems", true );
		
		

		enable_param.addEnabledOnSelection( alert_success_param );
		enable_param.addEnabledOnSelection( grab_ports_param );
		enable_param.addEnabledOnSelection( refresh_param );
		enable_param.addEnabledOnSelection( alert_other_port_param );
		enable_param.addEnabledOnSelection( alert_device_probs_param );
		enable_param.addEnabledOnSelection( release_mappings_param );
		
		boolean	enabled = enable_param.getValue();
		
		model.getStatus().setText( enabled?"Running":"Disabled" );
		
		enable_param.addListener(
				new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	p )
					{
						boolean	e = enable_param.getValue();
						
						model.getStatus().setText( e?"Running":"Disabled" );
						
						if ( e ){
							
							startUp();
							
						}else{
							
							closeDown( true );
						}
					}
				});
		
		model.getActivity().setVisible( false );
		model.getProgress().setVisible( false );
		
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
		
		if ( enabled ){
			
			startUp();			
		}
	}
	
	protected void
	startUp()
	{
		if ( upnp != null ){
			
				// already started up, must have been re-enabled
			
			upnp.reset();
			
			return;
		}
		
		try{
			upnp = UPnPFactory.getSingleton( plugin_interface );
				
			upnp.addRootDeviceListener(
				new UPnPListener()
				{
					public void
					rootDeviceFound(
						UPnPRootDevice		device )
					{
						try{
							processDevice( device.getDevice() );
							
							try{
								this_mon.enter();
							
								root_info_map.put( device.getLocation(), device.getInfo());
							
								Iterator	it = root_info_map.values().iterator();
								
								String	all_info = "";
									
								while( it.hasNext()){
									
									String	info = (String)it.next();
									
									if ( info != null ){
										
										all_info += (all_info.length()==0?"":",") + info;
									}
								}
								
								if ( all_info.length() > 0 ){
									
									plugin_interface.getPluginconfig().setPluginParameter( "plugin.info", all_info );
								}
								
							}finally{
								
								this_mon.exit();
							}
							
						}catch( Throwable e ){
							
							log.log( "Root device processing fails", e );
						}
					}
				});
			
			upnp.addLogListener(
				new UPnPLogListener()
				{
					public void
					log(
						String	str )
					{
						log.log( str );
					}
					
					public void
					logAlert(
						String	str,
						boolean	error,
						int		type )
					{
						boolean	logged = false;
						
						if ( alert_device_probs_param.getValue()){
							
							if ( type == UPnPLogListener.TYPE_ALWAYS ){
								
								log.logAlertRepeatable(						
										error?LoggerChannel.LT_ERROR:LoggerChannel.LT_WARNING,
										str );
								
								logged	= true;
								
							}else{
								
								boolean	do_it	= false;
								
								if ( type == UPnPLogListener.TYPE_ONCE_EVER ){
									
									byte[] fp = 
										plugin_interface.getUtilities().getSecurityManager().calculateSHA1(
											str.getBytes());
									
									String	key = "upnp.alert.fp." + plugin_interface.getUtilities().getFormatters().encodeBytesToString( fp );
									
									PluginConfig pc = plugin_interface.getPluginconfig();
									
									if ( !pc.getPluginBooleanParameter( key, false )){
										
										pc.setPluginParameter( key, true );
										
										do_it	= true;
									}
								}else{
									
									do_it	= true;
								}
							
								if ( do_it ){						
									
									log.logAlert(						
										error?LoggerChannel.LT_ERROR:LoggerChannel.LT_WARNING,
										str );	
									
									logged	= true;
								}
							}		
						}
						
						if ( !logged ){
							
							log.log( str );
						}
					}
				});
			
			mapping_manager.addListener(
				new UPnPMappingManagerListener()
				{
					public void
					mappingAdded(
						UPnPMapping		mapping )
					{
						addMapping( mapping );
					}
				});
			
			UPnPMapping[]	upnp_mappings = mapping_manager.getMappings();
			
			for (int i=0;i<upnp_mappings.length;i++){
				
				addMapping( upnp_mappings[i] );
			}
			
		}catch( Throwable e ){
			
			log.log( e );
		}
	}
	
	protected void
	closeDown(
		boolean	end_of_day )
	{
		for (int i=0;i<mappings.size();i++){
			
			UPnPMapping	mapping = (UPnPMapping)mappings.get(i);
			
			if ( !mapping.isEnabled()){
				
				continue;
			}
			
			for (int j=0;j<services.size();j++){
				
				UPnPPluginService	service = (UPnPPluginService)services.get(j);
				
				service.removeMapping( log, mapping, end_of_day );
			}
		}		
	}
	
	protected void
	processDevice(
		UPnPDevice		device )
	
		throws UPnPException
	{			
		processServices( device, device.getServices());
			
		UPnPDevice[]	kids = device.getSubDevices();
		
		for (int i=0;i<kids.length;i++){
			
			processDevice( kids[i] );
		}
	}
	
	protected void
	processServices(
		UPnPDevice		device,
		UPnPService[] 	device_services )
	
		throws UPnPException
	{
		for (int i=0;i<device_services.length;i++){
			
			UPnPService	s = device_services[i];
			
			String	service_type = s.getServiceType();
			
			if ( 	service_type.equalsIgnoreCase( "urn:schemas-upnp-org:service:WANIPConnection:1") || 
					service_type.equalsIgnoreCase( "urn:schemas-upnp-org:service:WANPPPConnection:1")){
				
				final UPnPWANConnection	wan_service = (UPnPWANConnection)s.getSpecificService();
				
				device.getRootDevice().addListener(
					new UPnPRootDeviceListener()
					{
						public void
						lost(
							UPnPRootDevice	root,
							boolean			replaced )
						{
							removeService( wan_service, replaced );
						}
					});
				
				addService( wan_service );
				
			}else if ( 	service_type.equalsIgnoreCase( "urn:schemas-upnp-org:service:WANCommonInterfaceConfig:1")){ 
				
				try{
					UPnPWANCommonInterfaceConfig	config = (UPnPWANCommonInterfaceConfig)s.getSpecificService();
				
					long[]	speeds = config.getCommonLinkProperties();
					
					if ( speeds[0] > 0 && speeds[1] > 0 ){
						
						log.log( "Device speed: down=" + 
									plugin_interface.getUtilities().getFormatters().formatByteCountToKiBEtcPerSec(speeds[0]/8) + ", up=" + 
									plugin_interface.getUtilities().getFormatters().formatByteCountToKiBEtcPerSec(speeds[1]/8));
					}
				}catch( Throwable e ){
					
					log.log(e);
				}
			}
		}
	}
	
	protected void
	addService(
		UPnPWANConnection	wan_service )
	
		throws UPnPException
	{
		try{
			this_mon.enter();
		
			log.log( "    Found " + ( wan_service.getGenericService().getServiceType().indexOf("PPP") == -1? "WANIPConnection":"WANPPPConnection" ));
			
			UPnPWANConnectionPortMapping[] ports = wan_service.getPortMappings();
			
			for (int j=0;j<ports.length;j++){
				
				log.log( "      mapping [" + j  + "] " + ports[j].getExternalPort() + "/" + 
								(ports[j].isTCP()?"TCP":"UDP" ) + " [" + ports[j].getDescription() + "] -> " + ports[j].getInternalHost());
			}
			
			services.add(new UPnPPluginService( wan_service, ports, alert_success_param, grab_ports_param, alert_other_port_param, release_mappings_param ));
			
			checkState();
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	removeService(
		UPnPWANConnection	wan_service,
		boolean				replaced )
	{
		try{
			this_mon.enter();
			
			String	name = wan_service.getGenericService().getServiceType().indexOf("PPP") == -1? "WANIPConnection":"WANPPPConnection";
			
			String	text = 
				MessageText.getString( 
						"upnp.alert.lostdevice", 
						new String[]{ name, wan_service.getGenericService().getDevice().getRootDevice().getLocation().getHost()});
			
			log.log( text );
			
			if ( (!replaced) && alert_device_probs_param.getValue()){
				
				log.logAlertRepeatable( LoggerChannel.LT_WARNING, text );
			}
					
			for (int i=0;i<services.size();i++){
				
				UPnPPluginService	ps = (UPnPPluginService)services.get(i);
				
				if ( ps.getService() == wan_service ){
					
					services.remove(i);
					
					break;
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	addMapping(
		UPnPMapping		mapping )
	{
		try{
			this_mon.enter();
		
			mappings.add( mapping );
			
			log.log( "Mapping request: " + mapping.getString() + ", enabled = " + mapping.isEnabled());
			
			mapping.addListener( this );
			
			checkState();
			
		}finally{
			
			this_mon.exit();
		}
	}	
	
	public void
	mappingChanged(
		UPnPMapping	mapping )
	{
		checkState();
	
	}
	
	public void
	mappingDestroyed(
		UPnPMapping	mapping )
	{
		try{
			this_mon.enter();
		
			mappings.remove( mapping );
			
			for (int j=0;j<services.size();j++){
				
				UPnPPluginService	service = (UPnPPluginService)services.get(j);
				
				service.removeMapping( log, mapping, false );
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	checkState()
	{		
		try{
			this_mon.enter();
		
			for (int i=0;i<mappings.size();i++){
				
				UPnPMapping	mapping = (UPnPMapping)mappings.get(i);
	
				for (int j=0;j<services.size();j++){
					
					UPnPPluginService	service = (UPnPPluginService)services.get(j);
					
					service.checkMapping( log, mapping );
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
		// for external use, e.g. webui
	
	public UPnPMapping
	addMapping(
		String		desc_resource,
		boolean		tcp,
		int			port,
		boolean		enabled )
	{
		return( UPnPMappingManager.getSingleton().addMapping( desc_resource, tcp, port, enabled ));
	}
	
	public UPnPMapping
	getMapping(
		boolean	tcp,
		int		port )
	{
		return( UPnPMappingManager.getSingleton().getMapping( tcp, port ));
	}
}
