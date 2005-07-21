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

package com.aelitis.net.upnp.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.*;
import java.io.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.logging.*;

import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderAdapter;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocument;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentException;

import com.aelitis.net.upnp.*;
import com.aelitis.net.upnp.impl.device.*;
import com.aelitis.net.upnp.services.UPnPWANConnectionPortMapping;
import com.aelitis.net.upnp.services.UPnPWANIPConnection;

public class 
UPnPImpl
	extends 	ResourceDownloaderAdapter
	implements 	UPnP, SSDPListener
{	
	public static final String	NL	= "\r\n";
	
	protected static UPnPImpl	singleton;
	protected static AEMonitor	class_mon 	= new AEMonitor( "UPnP:class" );
	
	public static UPnP
	getSingleton(
		PluginInterface		plugin_interface )
	
		throws UPnPException
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
				
				singleton = new UPnPImpl( plugin_interface );
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	protected PluginInterface		plugin_interface;
	protected LoggerChannel			log;
	protected SSDP					ssdp;
	
	protected Map		root_locations	= new HashMap();
	
	protected List		log_listeners	= new ArrayList();
	protected List		log_history		= new ArrayList();
	
	protected List		rd_listeners		= new ArrayList();
	protected AEMonitor	rd_listeners_mon 	= new AEMonitor( "UPnP:L" );

	protected int		http_calls_ok	= 0;
	protected int		direct_calls_ok	= 0;
	
	protected int		trace_index		= 0;
	
	protected AEMonitor	this_mon 	= new AEMonitor( "UPnP" );

	protected
	UPnPImpl(
		PluginInterface		_plugin_interface )
	
		throws UPnPException
	{
		plugin_interface	= _plugin_interface;
		
		log		= plugin_interface.getLogger().getChannel("UPnP Core");
		
		ssdp = SSDPFactory.create( this );
		
		ssdp.addListener(this);
		
		ssdp.start();
	}
	
	public void
	rootDiscovered(
		NetworkInterface	network_interface,
		InetAddress			local_address,
		URL					location )

	{
		UPnPRootDeviceImpl root_device = (UPnPRootDeviceImpl)root_locations.get( location.getHost());
			
		if ( root_device != null ){
	
				// device is still there with same IP, however 
			
			if ( root_device.getLocation().equals( location )){
				
				
					// 1) port of UPnP device might have changed (it does on mine when enabling/disabling UPnP)
					// 2) our local IP might have changed (DHCP reassignment)

				if ( root_device.getLocalAddress().equals( local_address )){
				
					// everythings the same, nothing to do
				
					return;
				}
			
					// an alternative situation is where the same device is discovered by two network interfaces
					// (see https://sourceforge.net/forum/message.php?msg_id=2912370 )
					// if this is the case we just use the first NI through which it was discovered and
					// map to that NI's local address
			
				if ( !root_device.getNetworkInterface().equals( network_interface )){
				
					log( "UPnP: secondary route to = " + location + ", local = " + local_address.toString() + " - using initial network interface (" + 
							root_device.getNetworkInterface());
	
					return;
				}
			}
			
				// something changed, resetablish everything
			
			root_locations.remove( location.getHost());
			
			root_device.destroy( true );
		}
		
		log( "UPnP: root discovered = " + location + ", local = " + local_address.toString() );
		
		try{
			root_device = new UPnPRootDeviceImpl( this, network_interface, local_address, location );
		
			List	listeners;
			
			try{
				rd_listeners_mon.enter();
				
				root_locations.put( location.getHost(), root_device );

				listeners = new ArrayList( rd_listeners );
				
			}finally{
				
				rd_listeners_mon.exit();
			}
			
			for (int i=0;i<listeners.size();i++){
				
				((UPnPListener)listeners.get(i)).rootDeviceFound( root_device );
				
			}
		
		}catch( UPnPException e ){
			
			log( e.toString());
		}
	}
	
	public void
	rootAlive(
		URL			location )

	{
		UPnPRootDeviceImpl root_device = (UPnPRootDeviceImpl)root_locations.get( location.getHost());
			
		if ( root_device == null ){
			
			ssdp.searchNow();
		}
	}
	
	public void
	rootLost(
		InetAddress	local_address,
		URL			location )
	{
		UPnPRootDeviceImpl	root_device;
	
		try{
			rd_listeners_mon.enter();

			root_device = (UPnPRootDeviceImpl)root_locations.remove( location.getHost());
			
		}finally{
			
			rd_listeners_mon.exit();
		}
		
		if ( root_device == null ){
			
			return;
		}
		
		log( "UPnP: root lost = " + location +", local = " + local_address.toString() );
	
		root_device.destroy( false );
	}
	
	public void
	reset()
	{
		log( "UPnP: reset" );

		List	roots;
		
		try{
			rd_listeners_mon.enter();

			roots = new ArrayList(root_locations.values());
			
			root_locations.clear();
			
		}finally{
			
			rd_listeners_mon.exit();
		}
		
		for (int i=0;i<roots.size();i++){
			
			((UPnPRootDeviceImpl)roots.get(i)).destroy( true );
		}
		
		ssdp.searchNow();
	}
	
	public SimpleXMLParserDocument
	parseXML(
		InputStream		_is )
	
		throws SimpleXMLParserDocumentException, IOException
	{
			// ASSUME UTF-8
		
		ByteArrayOutputStream		baos = null;
		
		try{
			baos = new ByteArrayOutputStream(1024);
			
			byte[]	buffer = new byte[8192];
			
			while(true){
				
				int	len = _is.read( buffer );
				
				if ( len <= 0 ){
					
					break;
				}
				
				baos.write( buffer, 0, len );
			}
		}finally{
			
			baos.close();
		}
		
		byte[]	bytes_in = baos.toByteArray();
		
		InputStream	is = new ByteArrayInputStream( bytes_in );
		
			// Gudy's router was returning trailing nulls which then stuffed up the
			// XML parser. Hence this code to try and strip them
		
		try{
			StringBuffer	data = new StringBuffer(1024);
			
			LineNumberReader	lnr = new LineNumberReader( new InputStreamReader( is, "UTF-8" ));
			
			while( true ){
				
				String	line = lnr.readLine();
				
				if ( line == null ){
					
					break;
				}
				
				data.append( line.trim() + "\n" );	
			}
				
			String	data_str = data.toString();
			
			log.log( "UPnP:Response:" + data_str );
			
			return( plugin_interface.getUtilities().getSimpleXMLParserDocumentFactory().create( data_str ));
			
		}catch( Throwable e ){
			
			try{
				FileOutputStream	trace = new FileOutputStream( getTraceFile());
				
				trace.write( bytes_in );
				
				trace.close();
				
			}catch( Throwable f ){
				
				Debug.printStackTrace( f );
			}
			
			if ( e instanceof SimpleXMLParserDocumentException ){
				
				throw((SimpleXMLParserDocumentException)e);
			}
			
			throw( new SimpleXMLParserDocumentException(e ));
		}
	}
	
	public SimpleXMLParserDocument
	downloadXML(
		URL		url )
	
		throws UPnPException
	{
		ResourceDownloaderFactory rdf = plugin_interface.getUtilities().getResourceDownloaderFactory();
		
		ResourceDownloader rd = rdf.getRetryDownloader( rdf.create( url ), 3 );
		
		rd.addListener( this );
		
		try{
			InputStream	data = rd.download();
			
			return( parseXML( data ));
			
		}catch( Throwable e ){
			
			log( e );

			if (e instanceof UPnPException ){
				
				throw((UPnPException)e);
			}
			
			throw( new UPnPException( "Root device location '" + url + "' - data read failed", e ));
		}	
	}
	
	public SimpleXMLParserDocument
	performSOAPRequest(
		UPnPService		service,
		String			soap_action,
		String			request )
	
		throws SimpleXMLParserDocumentException, UPnPException, IOException
	{
		SimpleXMLParserDocument	res;
		
		try{
			res =  performSOAPRequest( service, soap_action, request, true );
				
			http_calls_ok++;
			
		}catch( IOException e ){
			
			res = performSOAPRequest( service, soap_action, request, false );
			
			direct_calls_ok++;
			
			if ( direct_calls_ok == 1 ){
				
				log.log( "Invocation via http connection failed (" + e.getMessage() + ") but socket connection succeeded" );
			}
		}
		
		return( res );
	}
	
	public SimpleXMLParserDocument
	performSOAPRequest(
		UPnPService		service,
		String			soap_action,
		String			request,
		boolean			use_http_connection )
	
		throws SimpleXMLParserDocumentException, UPnPException, IOException
	{
		log.log( "UPnP:Request:" + request );

		URL	control = service.getControlURL();
		
		if ( use_http_connection ){
			
			HttpURLConnection	con = (HttpURLConnection)control.openConnection();
			
			con.setRequestProperty( "SOAPAction", "\""+ soap_action + "\"");
			
			con.setRequestProperty( "Content-Type", "text/xml; charset=\"utf-8\"" );
			
			con.setRequestProperty( "User-Agent", "Azureus (UPnP/1.0)" );
			
			con.setRequestMethod( "POST" );
			
			con.setDoInput( true );
			con.setDoOutput( true );
			
			OutputStream	os = con.getOutputStream();
			
			PrintWriter	pw = new PrintWriter( new OutputStreamWriter(os, "UTF-8" ));
						
			pw.println( request );
			
			pw.flush();
	
			con.connect();
			
			if ( con.getResponseCode() == 405 ){
				
					// gotta retry with M-POST method
								
				con = (HttpURLConnection)control.openConnection();
				
				con.setRequestProperty( "Content-Type", "text/xml; charset=\"utf-8\"" );
				
				con.setRequestMethod( "M-POST" );
				
				con.setRequestProperty( "MAN", "\"http://schemas.xmlsoap.org/soap/envelope/\"; ns=01" );
	
				con.setRequestProperty( "01-SOAPACTION", "\""+ soap_action + "\"");
				
				con.setDoInput( true );
				con.setDoOutput( true );
				
				os = con.getOutputStream();
				
				pw = new PrintWriter( new OutputStreamWriter(os, "UTF-8" ));
							
				pw.println( request );
				
				pw.flush();
	
				con.connect();
			
				return( parseXML(con.getInputStream()));	
				
			}else{
				
				return( parseXML(con.getInputStream()));
			}
		}else{
	
			Socket	socket = new Socket(control.getHost(), control.getPort());
			
			PrintWriter	pw = new PrintWriter(new OutputStreamWriter( socket.getOutputStream(), "UTF8" ));
		
			String	url_target = control.toString();
			
			int	p1 	= url_target.indexOf( "://" ) + 3;
			p1		= url_target.indexOf( "/", p1 );
			
			url_target = url_target.substring( p1 );
			
			pw.print( "POST " + url_target + " HTTP/1.1" + NL );
			pw.print( "Content-Type: text/xml; charset=\"utf-8\"" + NL );
			pw.print( "SOAPAction: \"" + soap_action + "\"" + NL );
			pw.print( "User-Agent: Azureus (UPnP/1.0)" + NL );
			pw.print( "Host: " + control.getHost() + NL );
			pw.print( "Content-Length: " + request.getBytes( "UTF8" ).length + NL );
			pw.print( "Connection: Keep-Alive" + NL );
			pw.print( "Pragma: no-cache" + NL + NL );

			pw.print( request );
			
			pw.flush();
			
			InputStream	is = socket.getInputStream();
			
			String	reply_header = "";
			
			while(true){
				
				byte[]	buffer = new byte[1];
				
				if ( is.read( buffer ) <= 0 ){
					
					throw( new IOException( "Premature end of input stream" ));
				}
				
				reply_header += (char)buffer[0];
				
				if ( reply_header.endsWith( NL+NL )){
					
					break;
				}
			}
			
			p1 = reply_header.indexOf( NL );
			
			String	first_line = reply_header.substring( 0, p1 ).trim();
			
			if ( first_line.indexOf( "200" ) == -1 ){
				
				throw( new IOException( "HTTP request failed:" + first_line ));
			}
			
			return( parseXML( is ));
		}
	}
	
	protected File
	getTraceFile()
	{
		try{
			this_mon.enter();
		
			trace_index++;
			
			if ( trace_index == 6 ){
				
				trace_index = 1;
			}
			
			return( new File( plugin_interface.getUtilities().getAzureusUserDir(), "upnp_trace" + trace_index + ".log" ));
		}finally{
			
			this_mon.exit();
		}
	}
	
	public PluginInterface
	getPluginInterface()
	{
		return( plugin_interface );
	}
	
	public void
	reportActivity(
		ResourceDownloader	downloader,
		String				activity )
	{
		log( activity );
	}
		
	public void
	failed(
		ResourceDownloader			downloader,
		ResourceDownloaderException e )
	{
		log( e );
	}
	
	public void
	log(
		Throwable e )
	{
		log( e.toString());
	}
	
	public void
	log(
		String	str )
	{
		List	old_listeners;
		
		try{
			this_mon.enter();

			old_listeners = new ArrayList(log_listeners);

			log_history.add( str );
			
			if ( log_history.size() > 32 ){
				
				log_history.remove(0);
			}
		}finally{
			
			this_mon.exit();
		}
		
		for (int i=0;i<old_listeners.size();i++){
	
			((UPnPLogListener)old_listeners.get(i)).log( str );
		}
	}
	
	public void
	addLogListener(
		UPnPLogListener	l )
	{
		List	old_logs;
		
		try{
			this_mon.enter();

			old_logs = new ArrayList(log_history);

			log_listeners.add( l );
		}finally{
			
			this_mon.exit();
		}
		
		for (int i=0;i<old_logs.size();i++){
			
			l.log((String)old_logs.get(i));
		}
	}
		
	public void
	removeLogListener(
		UPnPLogListener	l )
	{
		log_listeners.remove( l );
	}
	
	public void
	addRootDeviceListener(
		UPnPListener	l )
	{
		List	old_locations;
		
		try{
			this_mon.enter();

			old_locations = new ArrayList(root_locations.values());

			rd_listeners.add( l );
			
		}finally{
			
			this_mon.exit();
		}
		
		for (int i=0;i<old_locations.size();i++){
			
			l.rootDeviceFound(((UPnPRootDevice)old_locations.get(i)));
		}
	}
		
	public void
	removeRootDeviceListener(
		UPnPListener	l )
	{
		rd_listeners.remove( l );
	}
	public static void
	main(
		String[]		args )
	{
		try{
			UPnP	upnp = UPnPFactory.getSingleton(null);	// won't work with null ....
				
			upnp.addRootDeviceListener(
					new UPnPListener()
					{
						public void
						rootDeviceFound(
							UPnPRootDevice		device )
						{
							try{
								processDevice( device.getDevice() );
								
							}catch( Throwable e ){
								
								Debug.printStackTrace( e );
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
						System.out.println( str );
					}
				});
			
			Thread.sleep(20000);
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	protected static void
	processDevice(
		UPnPDevice	device )
	
		throws UPnPException
	{
		if ( device.getDeviceType().equalsIgnoreCase("urn:schemas-upnp-org:device:WANConnectionDevice:1")){
			
			System.out.println( "got device");
			
			UPnPService[] services = device.getServices();
			
			for (int i=0;i<services.length;i++){
				
				UPnPService	s = services[i];
				
				if ( s.getServiceType().equalsIgnoreCase( "urn:schemas-upnp-org:service:WANIPConnection:1")){
					
					System.out.println( "got service" );
					
					UPnPAction[]	actions = s.getActions();
					
					for (int j=0;j<actions.length;j++){
						
						System.out.println( actions[j].getName());
					}
					
					UPnPStateVariable[]	vars = s.getStateVariables();
					
					for (int j=0;j<vars.length;j++){
						
						System.out.println( vars[j].getName());
					}
					
					UPnPStateVariable noe = s.getStateVariable("PortMappingNumberOfEntries");
					
					System.out.println( "noe = " + noe.getValue());
					
					UPnPWANIPConnection wan_ip = (UPnPWANIPConnection)s.getSpecificService();
					
					UPnPWANConnectionPortMapping[] ports = wan_ip.getPortMappings();
					
					wan_ip.addPortMapping( true, 7007, "Moo!" );
	
					UPnPAction act	= s.getAction( "GetGenericPortMappingEntry" );
					
					UPnPActionInvocation inv = act.getInvocation();

					inv.addArgument( "NewPortMappingIndex", "0" );
					
					UPnPActionArgument[] outs = inv.invoke();
					
					for (int j=0;j<outs.length;j++){
						
						System.out.println( outs[j].getName() + " = " + outs[j].getValue());
					}
				}
			}
		}else{
			
			UPnPDevice[]	kids = device.getSubDevices();
			
			for (int i=0;i<kids.length;i++){
				
				processDevice( kids[i] );
			}
		}
	}
}
