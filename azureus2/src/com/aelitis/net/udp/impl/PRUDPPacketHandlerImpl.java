/*
 * File    : PRUDPPacketReceiverImpl.java
 * Created : 20-Jan-2004
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.aelitis.net.udp.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;
import java.net.*;

import sun.misc.BASE64Decoder;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.net.udp.PRUDPPacket;
import com.aelitis.net.udp.PRUDPPacketHandler;
import com.aelitis.net.udp.PRUDPPacketHandlerException;
import com.aelitis.net.udp.PRUDPPacketReply;

public class 
PRUDPPacketHandlerImpl
	implements PRUDPPacketHandler
{	
	protected int				port;
	protected DatagramSocket	socket;
	
	protected long		last_timeout_check;
	
	protected Map			requests = new HashMap();
	protected AEMonitor		requests_mon	= new AEMonitor( "PRUDPPH:req" );

	protected
	PRUDPPacketHandlerImpl(
		int		_port )
	{
		port		= _port;
		
		final AESemaphore init_sem = new AESemaphore("PRUDPPacketHandler");
		
		Thread t = new AEThread( "PRUDPPacketReciever:".concat(String.valueOf(port)))
			{
				public void
				runSupport()
				{
					receiveLoop(init_sem);
				}
			};
		
		t.setDaemon(true);
		
		t.start();
		
		init_sem.reserve();
	}
	
	protected void
	receiveLoop(
		AESemaphore	init_sem )
	{
		try{
			String bind_ip = COConfigurationManager.getStringParameter("Bind IP", "");
			
			InetSocketAddress	address;
			
			if ( bind_ip.length() == 0 ){
				
				address = new InetSocketAddress("127.0.0.1",port);
				
				socket = new DatagramSocket( port );
				
			}else{
				
				address = new InetSocketAddress(InetAddress.getByName(bind_ip), port);
				
				socket = new DatagramSocket( address );		
			}
					
			socket.setReuseAddress(true);
			
			socket.setSoTimeout( PRUDPPacket.DEFAULT_UDP_TIMEOUT );
			
			init_sem.release();
			
			LGLogger.log( "PRUDPPacketReceiver: receiver established on port ".concat(String.valueOf(port))); 
	
			byte[] buffer = new byte[PRUDPPacket.MAX_PACKET_SIZE];
			
			long	successful_accepts 	= 0;
			long	failed_accepts		= 0;
			
			while(true){
				
				try{
						
					DatagramPacket packet = new DatagramPacket( buffer, buffer.length, address );
					
					socket.receive( packet );
					
					successful_accepts++;
					
					process( packet );
				
				}catch( SocketTimeoutException e ){
										
				}catch( Throwable e ){
						
					failed_accepts++;
					
					LGLogger.log( "PRUDPPacketReceiver: receive failed on port " + port, e ); 

					if ( failed_accepts > 100 && successful_accepts == 0 ){
						
		
						LGLogger.logUnrepeatableAlertUsingResource( 
								LGLogger.AT_ERROR,
								"Network.alert.acceptfail",
								new String[]{ ""+port, "UDP" } );
										
							// break, sometimes get a screaming loop. e.g.
						/*
						[2:01:55]  DEBUG::Tue Dec 07 02:01:55 EST 2004
						[2:01:55]    java.net.SocketException: Socket operation on nonsocket: timeout in datagram socket peek
						[2:01:55]  	at java.net.PlainDatagramSocketImpl.peekData(Native Method)
						[2:01:55]  	at java.net.DatagramSocket.receive(Unknown Source)
						[2:01:55]  	at org.gudy.azureus2.core3.tracker.server.impl.udp.TRTrackerServerUDP.recvLoop(TRTrackerServerUDP.java:118)
						[2:01:55]  	at org.gudy.azureus2.core3.tracker.server.impl.udp.TRTrackerServerUDP$1.runSupport(TRTrackerServerUDP.java:90)
						[2:01:55]  	at org.gudy.azureus2.core3.util.AEThread.run(AEThread.java:45)
						*/
						
						break;
					}
					
				}finally{
					
					checkTimeouts();
				}
			}
		}catch( Throwable e ){
			
			LGLogger.logUnrepeatableAlertUsingResource( 
					LGLogger.AT_ERROR,
					"Tracker.alert.listenfail",
					new String[]{ "UDP:"+port });
			
			LGLogger.log( "PRUDPPacketReceiver: DatagramSocket bind failed on port ".concat(String.valueOf(port)), e ); 
		}
	}
	
	protected void
	checkTimeouts()
	{
		long	now = SystemTime.getCurrentTime();

		if ( SystemTime.isErrorLast30sec() || now - last_timeout_check >= PRUDPPacket.DEFAULT_UDP_TIMEOUT ){
			
			last_timeout_check	= now;
			
			try{
				requests_mon.enter();
				
				Iterator it = requests.values().iterator();
				
				while( it.hasNext()){
					
					PRUDPPacketHandlerRequest	request = (PRUDPPacketHandlerRequest)it.next();
					
					if ( now - request.getCreateTime() >= PRUDPPacket.DEFAULT_UDP_TIMEOUT ){
					
						LGLogger.log( LGLogger.ERROR, "PRUDPPacketHandler: request timeout" ); 
						
							// don't change the text of this message, it's used elsewhere
						
						request.setException(new PRUDPPacketHandlerException("timed out"));
					}
				}
			}finally{
				
				requests_mon.exit();
			}
		}
		
	}
	protected void
	process(
		DatagramPacket	packet )
	
		throws IOException
	{
		byte[]	packet_data = packet.getData();
	
		
		PRUDPPacket reply = 
			PRUDPPacketReply.deserialiseReply( 
				new DataInputStream(new ByteArrayInputStream( packet_data, 0, packet.getLength())));

		LGLogger.log( "PRUDPPacketHandler: reply packet received: ".concat(reply.getString())); 
				
		try{
			requests_mon.enter();
			
			PRUDPPacketHandlerRequest	request = (PRUDPPacketHandlerRequest)requests.get(new Integer(reply.getTransactionId()));
		
			if ( request == null ){
			
				LGLogger.log( LGLogger.ERROR, "PRUDPPacketReceiver: unmatched reply received, discarding:".concat(reply.getString()));
			
			}else{
			
				request.setReply( reply );
			}
		}finally{
			
			requests_mon.exit();
		}
	}
	
	public PRUDPPacket
	sendAndReceive(
		PasswordAuthentication	auth,
		PRUDPPacket				request_packet,
		InetSocketAddress		destination_address )
	
		throws PRUDPPacketHandlerException
	{
		try{
			ByteArrayOutputStream	baos = new ByteArrayOutputStream();
			
			DataOutputStream os = new DataOutputStream( baos );
					
			request_packet.serialise(os);
			
			byte[]	buffer = baos.toByteArray();
			
			if ( auth != null ){
				
				//<parg_home> so <new_packet> = <old_packet> + <user_padded_to_8_bytes> + <hash>
				//<parg_home> where <hash> = first 8 bytes of sha1(<old_packet> + <user_padded_to_8> + sha1(pass))
				//<XTF> Yes
				
				SHA1Hasher hasher = new SHA1Hasher();

				String	user_name 	= auth.getUserName();
				String	password	= new String(auth.getPassword());
				
				byte[]	sha1_password;
				
				if ( user_name.equals( "<internal>")){
					
					sha1_password = new BASE64Decoder().decodeBuffer(password);

				}else{
					
					sha1_password = hasher.calculateHash(password.getBytes());			
				}
				
				byte[]	user_bytes = new byte[8];
				
				Arrays.fill( user_bytes, (byte)0);
				
				for (int i=0;i<user_bytes.length&&i<user_name.length();i++){
					
					user_bytes[i] = (byte)user_name.charAt(i);
				}
				
				hasher = new SHA1Hasher();
				
				hasher.update( buffer );
				hasher.update( user_bytes );
				hasher.update( sha1_password );
				
				byte[]	overall_hash = hasher.getDigest();
				
				//System.out.println("PRUDPHandler - auth = " + auth.getUserName() + "/" + new String(auth.getPassword()));
								
				baos.write( user_bytes );
				baos.write( overall_hash, 0, 8 );
				
				buffer = baos.toByteArray();
			}
			
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, destination_address );
			
			PRUDPPacketHandlerRequest	request = new PRUDPPacketHandlerRequest();
		
			try{
				requests_mon.enter();
					
				requests.put( new Integer( request_packet.getTransactionId()), request );
				
			}finally{
				
				requests_mon.exit();
			}
			
			LGLogger.log( "PRUDPPacketHandler: request packet sent: ".concat(request_packet.getString())); 
			
			try{
				socket.send( packet );
			
				return( request.getReply());
				
			}finally{
				
				try{
					requests_mon.enter();
					
					requests.remove( new Integer( request_packet.getTransactionId()));
					
				}finally{
					
					requests_mon.exit();
				}
			}
		}catch( PRUDPPacketHandlerException e ){
			
			throw( e );
			
		}catch( Throwable e ){
			
			LGLogger.log( "PRUDPPacketHandler: sendAndReceive failed", e ); 
			
			throw( new PRUDPPacketHandlerException( "PRUDPPacketHandler:sendAndReceive failed", e ));
		}
	}
}
