/*
 * File    : UtilitiesImpl.java
 * Created : 24-Mar-2004
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

package org.gudy.azureus2.pluginsimpl.local.utils;

/**
 * @author parg
 *
 */

import java.io.InputStream;
import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.utils.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.plugins.utils.security.SESecurityManager;
import org.gudy.azureus2.plugins.utils.xml.rss.RSSFeed;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentException;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentFactory;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.*;
import org.gudy.azureus2.pluginsimpl.local.utils.security.*;
import org.gudy.azureus2.pluginsimpl.local.utils.xml.rss.RSSFeedImpl;
import org.gudy.azureus2.pluginsimpl.local.utils.xml.simpleparser.*;

import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPChecker;
import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPCheckerFactory;
import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPCheckerService;
import org.gudy.azureus2.core3.ipchecker.extipchecker.ExternalIPCheckerServiceListener;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.IPToHostNameResolver;
import org.gudy.azureus2.core3.util.IPToHostNameResolverListener;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.Timer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.ui.common.UIImageRepository;

import com.aelitis.azureus.core.versioncheck.VersionCheckClient;

public class 
UtilitiesImpl
	implements Utilities
{
	protected PluginInterface			pi;
	
	public
	UtilitiesImpl(
		PluginInterface		_pi )
	{
		pi		= _pi;
	}
	
	public String
	getAzureusUserDir()
	{
		String	res = SystemProperties.getUserPath();
		
		if ( res.endsWith(File.separator )){
			
			res = res.substring(0,res.length()-1);
		}
		
		return( res );
	}
	
	public String
	getAzureusProgramDir()
	{
		String	res = SystemProperties.getApplicationPath();
		
		if ( res.endsWith(File.separator )){
			
			res = res.substring(0,res.length()-1);
		}
		
		return( res );	
	}
	
	public boolean
	isWindows()
	{
		return( Constants.isWindows );
	}
	
	public boolean
	isLinux()
	{
		return( Constants.isLinux );
	}
	
	public boolean
	isOSX()
	{
		return( Constants.isOSX );
	}
	
	public boolean
	isCVSVersion()
	{
		return( Constants.isCVSVersion());
	}
	
	public InputStream
	getImageAsStream(
		String	image_name )
	{
		return( UIImageRepository.getImageAsStream( image_name ));
	}
	
	public Semaphore
	getSemaphore()
	{
		return( new SemaphoreImpl());
	}
	
	public ByteBuffer
	allocateDirectByteBuffer(
		int		size )
	{
		return( DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_EXTERNAL,size ).getBuffer(DirectByteBuffer.SS_EXTERNAL));
	}
	
	public void
	freeDirectByteBuffer(
		ByteBuffer	buffer )
	{
    
		//DirectByteBufferPool.freeBuffer( buffer );
	}
	
	public Formatters
	getFormatters()
	{
		return( new FormattersImpl());
	}
	
	public LocaleUtilities
	getLocaleUtilities()
	{
		return( new LocaleUtilitiesImpl( pi ));
	}
	
	public UTTimer
	createTimer(
		String		name )
	{
		return( new UTTimerImpl( pi, name ));
	}
	
	public void
	createThread(
		String			name,
		final Runnable	target )
	{
		Thread t = 
			new AEThread( pi.getPluginName() + "::" + name )
			{
				public void
				runSupport()
				{
					target.run();
				}
			};
	
		t.setDaemon(true);
		
		t.start();
	}
	
	public void
	createProcess(
		String		command_line )
	
		throws PluginException
	{
	    try{
	    		// we need to spawn without inheriting handles
	    	
	    	PlatformManager pm = PlatformManagerFactory.getPlatformManager();
	    	
	    	pm.createProcess( command_line, false );
	    	    	
	    }catch(Throwable e) {
	    	
	        Debug.printStackTrace(e);
	        
	        try{
	        	Runtime.getRuntime().exec( command_line );
	        	
	        }catch( Throwable f ){
	        	
	        	throw( new PluginException("Failed to create process", f ));
	        }
	    }
	}
	
	public ResourceDownloaderFactory
	getResourceDownloaderFactory()
	{
		return( ResourceDownloaderFactoryImpl.getSingleton());
	}
	
	public SESecurityManager
	getSecurityManager()
	{
		return( new SESecurityManagerImpl());
	}
	
	public SimpleXMLParserDocumentFactory
	getSimpleXMLParserDocumentFactory()
	{
		return( new SimpleXMLParserDocumentFactoryImpl());
	}
	
	public RSSFeed
	getRSSFeed(
		URL		feed_location )
	
		throws ResourceDownloaderException, SimpleXMLParserDocumentException
	{
		return( getRSSFeed( getResourceDownloaderFactory().create( feed_location )));
	}
	
	public RSSFeed
	getRSSFeed(
		ResourceDownloader	feed_location )
	
		throws ResourceDownloaderException, SimpleXMLParserDocumentException
	{
		return( new RSSFeedImpl( this, feed_location ));
	}
	
	public InetAddress
	getPublicAddress()
	{
		try{
			
			String	vc_ip = VersionCheckClient.getSingleton().getExternalIpAddress();
			
			if ( vc_ip != null && vc_ip.length() > 0 ){
								
				return( InetAddress.getByName( vc_ip ));
			}
			
			ExternalIPChecker	checker = ExternalIPCheckerFactory.create();
			
			ExternalIPCheckerService[]	services = checker.getServices();
			
			final String[]	ip = new String[]{ null };
			
			for (int i=0;i<services.length && ip[0] == null;i++){
				
				final ExternalIPCheckerService	service = services[i];
				
				if ( service.supportsCheck()){

					final AESemaphore	sem = new AESemaphore("Utilities:getExtIP");
					
					ExternalIPCheckerServiceListener	listener = 
						new ExternalIPCheckerServiceListener()
						{
							public void
							checkComplete(
								ExternalIPCheckerService	_service,
								String						_ip )
							{
								ip[0]	= _ip;
								
								sem.release();
							}
								
							public void
							checkFailed(
								ExternalIPCheckerService	_service,
								String						_reason )
							{
								sem.release();
							}
								
							public void
							reportProgress(
								ExternalIPCheckerService	_service,
								String						_message )
							{
							}
						};
						
					services[i].addListener( listener );
					
					try{
						
						services[i].initiateCheck( 60000 );
						
						sem.reserve( 60000 );
						
					}finally{
						
						services[i].removeListener( listener );
					}
				}
				
					
				if ( ip[0] != null ){
						
					return( InetAddress.getByName( ip[0] ));
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
		
		return( null );
	}
	
	public String
	reverseDNSLookup(
		InetAddress		address )
	{
		final AESemaphore	sem = new AESemaphore("Utilities:reverseDNS");
		
		final String[]	res = { null };
		
		IPToHostNameResolver.addResolverRequest(
					address.getHostAddress(),
					new IPToHostNameResolverListener()
					{
						public void 
						IPResolutionComplete(
							String 		result,
							boolean 	succeeded )
						{
							if ( succeeded ){
								
								res[0] = result;
							}

							sem.release();
						}
					});
		
		sem.reserve( 60000 );
		
		return( res[0] );
	}
  
  
  public long getCurrentSystemTime() {
    return SystemTime.getCurrentTime();
  }
  
	public ByteArrayWrapper
	createWrapper(
		byte[]		data )
	{
		return( new HashWrapper( data ));
	}
	
	public AggregatedDispatcher
	createAggregatedDispatcher(
		final long	idle_dispatch_time,
		final long	max_queue_size )
	{
		return( 
			new AggregatedDispatcher()
			{
				private AggregatedList	list = 
					createAggregatedList(
						new AggregatedListAcceptor()
						{
							public void
							accept(
								List		l )
							{
								for (int i=0;i<l.size();i++){
									
									try{
										((Runnable)l.get(i)).run();
										
									}catch( Throwable e ){
										
										Debug.printStackTrace(e);
									}
								}
							}
						},
						idle_dispatch_time,
						max_queue_size );
				
				public void
				add(
					Runnable	runnable )
				{
					list.add( runnable );
				}
				
				public void
				destroy()
				{
					list.destroy();
				}
			});
	}
	
	public AggregatedList
	createAggregatedList(
		final AggregatedListAcceptor	acceptor,
		final long						idle_dispatch_time,
		final long						max_queue_size )
	{
		return( 
			new AggregatedList()
			{
				Timer		timer = new Timer( "AggregatedList" );
				TimerEvent	event;
				
				List		list	= new ArrayList();
				
				public void
				add(
					Object	obj )
				{
					
					List	dispatch_now = null;
					
					synchronized( timer ){
						
							// if the list is full kick off a dispatch and reset the list
						
						if (	max_queue_size > 0 &&
								max_queue_size	== list.size()){
							
							dispatch_now = list;
							
							list	= new ArrayList();
							
						}
							
						list.add( obj );
						
							// set up a timer to wakeup in required time period 
						
						long	now = SystemTime.getCurrentTime();
					
						if ( event != null ){
							
							event.cancel();
						}
						
						event = 
							timer.addEvent( 
									now + idle_dispatch_time,
									new TimerEventPerformer()
									{
										public void
										perform(
											TimerEvent	event )
										{
											dispatch();
										}
									});
					}
					
					if ( dispatch_now != null ){
						
						dispatch( dispatch_now );
					}
				}

				protected void
				dispatch()
				{
					List	dispatch_list;
					
					synchronized( timer ){
					
						dispatch_list	= list;
						
						list	= new ArrayList();
						
					}
					
					dispatch( dispatch_list );
				}

				protected void
				dispatch(
					List		l )
				{
					try{
						acceptor.accept( l );
						
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}
				}
				
				public void
				destroy()
				{
					dispatch();
					
					timer.destroy();
				}
			});
	}
}
