/*
 * File    : NatCheckerServer.java
 * Created : 12 oct. 2003 19:05:09
 * By      : Olivier 
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
 
package org.gudy.azureus2.core3.ipchecker.natchecker;

import java.net.*;
import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.*;
import com.aelitis.azureus.core.peermanager.messaging.*;
import com.aelitis.azureus.core.peermanager.messaging.azureus.*;



/**
 * 
 *
 */
public class NatCheckerServer extends AEThread {
		private static final LogIDs LOGID = LogIDs.NET;
    private static final String incoming_handshake = "NATCHECK_HANDSHAKE";
  
    private final String check;
    private ServerSocket server;
    private boolean valid = false;    
    private boolean bContinue = true;
    private final boolean use_incoming_router;
    private NetworkManager.ByteMatcher matcher;
    
    
    public NatCheckerServer(int _port, final String _check) {     
      super("Nat Checker Server");
      
      this.check = _check;
      use_incoming_router = _port == COConfigurationManager.getIntParameter("TCP.Listen.Port");
      
      if( use_incoming_router ) {
        //test port and currently-configured listening port are the same,
        //so register for incoming connection routing
        
        matcher = new NetworkManager.ByteMatcher() {
          public int size() {  return incoming_handshake.getBytes().length;  }
          public int minSize(){ return size(); }
        
          public Object matches( InetSocketAddress address, ByteBuffer to_compare, int port ) {             
            int old_limit = to_compare.limit();
            to_compare.limit( to_compare.position() + size() );
            boolean matches = to_compare.equals( ByteBuffer.wrap( incoming_handshake.getBytes() ) );
            to_compare.limit( old_limit );  //restore buffer structure
            return matches?"":null;
          }
          public Object minMatches( InetSocketAddress address, ByteBuffer to_compare, int port ) { return( matches( address, to_compare, port )); } 
          public byte[] getSharedSecret(){ return( null ); }
  	   	  public int getSpecificPort(){return( -1 );
		}
        };
        
        NetworkManager.getSingleton().requestIncomingConnectionRouting(
            matcher,
            new NetworkManager.RoutingListener() {
              public void connectionRouted( NetworkConnection connection, Object routing_data ) {
  							if (Logger.isEnabled())
  								Logger.log(new LogEvent(LOGID, "Incoming connection from ["
  										+ connection + "] successfully routed to NAT CHECKER"));
                
                try{
                  ByteBuffer msg = ByteBuffer.wrap( check.getBytes() );
                  while( msg.hasRemaining() ) {
                    connection.getTransport().write( new ByteBuffer[]{ msg }, 0, 1 );
                    Thread.sleep( 20 );
                  }
                }
                catch( Throwable t ) {
                  Debug.out( "Nat check write failed", t );
                }
                
                connection.close();
              }
              public boolean
          	  autoCryptoFallback()
              {
            	  return( true );
              }
            },
            new MessageStreamFactory() {
              public MessageStreamEncoder createEncoder() {  return new AZMessageEncoder();  /* unused */}
              public MessageStreamDecoder createDecoder() {  return new AZMessageDecoder();  /* unused */}
            });
        
        valid = true;
  			if (Logger.isEnabled())
  				Logger.log(new LogEvent(LOGID, "NAT tester using central routing for "
  						+ "server socket"));
      }
      else {  //different port than already listening on, start new listen server     	
        try {
          String bind_ip  = COConfigurationManager.getStringParameter("Bind IP", "");

          server = new ServerSocket();  //unbound          
          server.setReuseAddress( true );  //set SO_REUSEADDR 
          
          InetSocketAddress address;
  	      try{
  	        if( bind_ip.length() > 6 ) {
  	          address = new InetSocketAddress( InetAddress.getByName( bind_ip ), _port );
  	        }
  	        else {
  	          address = new InetSocketAddress( _port );
  	        }
  	      }
  	      catch( UnknownHostException e ) {
  	        Debug.out( e );
  	        address = new InetSocketAddress( _port );
  	      }
            	      
  	      server.bind( address );
  	      
  	      if (Logger.isEnabled())	Logger.log(new LogEvent(LOGID, "NAT tester server socket bound to " +address ));
          
          valid = true;
        }
        catch(Exception e) {  valid = false;  }      
      }
    }
    
    
    
    public void runSupport() {
      while(bContinue) {
        try {
          if (use_incoming_router) {
            //just NOOP loop sleep while waiting for routing
            Thread.sleep(20);
          }
          else {
            //listen for accept
          	Socket sck = server.accept();
          	sck.getOutputStream().write( check.getBytes() );
            sck.close();
          }
        } catch(Exception e) {
        	//Debug.printStackTrace(e);
        	bContinue = false;
        }
      }
    }
      
    
    
    public boolean isValid() {
      return this.valid;
    }
    
    
    public void stopIt() {
      bContinue = false;
      
      if( use_incoming_router ) {
        NetworkManager.getSingleton().cancelIncomingConnectionRouting( matcher );
      }
      else if( server != null ) {
        try {
          server.close();
        }
        catch(Throwable t) { t.printStackTrace(); }
      }
    }
    
    
  }
