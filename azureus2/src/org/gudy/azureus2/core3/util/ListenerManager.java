/*
 * File    : ListenerManager.java
 * Created : 15-Jan-2004
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

package org.gudy.azureus2.core3.util;

/**
 * @author parg
 *
 */

/**
 * This class exists to support the invocation of listeners while *not* synchronized.
 * This is important as in general it is a bad idea to invoke an "external" component
 * whilst holding a lock on something as unexpected deadlocks can result.
 * It has been introduced to reduce the likelyhood of such deadlocks
 */

import java.util.*;

public class 
ListenerManager
{
	public static ListenerManager
	createManager(
		String							name,
		ListenerManagerDispatcher		target )
	{
		return( new ListenerManager( name, target, false ));
	}
	
	public static ListenerManager
	createAsyncManager(
		String							name,
		ListenerManagerDispatcher		target )
	{
		return( new ListenerManager( name, target, true ));
	}
	
	
	protected String	name;
	
	protected ListenerManagerDispatcher					target;
	protected ListenerManagerDispatcherWithException	target_with_exception;
	
	protected boolean	async;
	protected boolean	with_exception;
	
	protected List		listeners		= new ArrayList();
	protected List		dispatch_queue	= new LinkedList();
	
	protected Semaphore	dispatch_sem = new Semaphore();
	
	protected
	ListenerManager(
		String							_name,
		ListenerManagerDispatcher		_target,
		boolean							_async )
	{
		name	= _name;
		target	= _target;
		async	= _async;
		
		if ( target instanceof ListenerManagerDispatcherWithException ){
			
			target_with_exception = (ListenerManagerDispatcherWithException)target;
		}
		
		if ( async ){
			
			if ( target_with_exception != null ){
				
				throw( new RuntimeException( "Can't have an async manager with exceptions!"));
			}
			
			Thread	t = new Thread( name )
			{
				public void
				run()
				{
					dispatchLoop();
				}
			};
			
			t.setDaemon( true );
			
			t.start();
		}
	}
	
	public void
	addListener(
		Object		listener )
	{
		synchronized( listeners ){
			
			listeners.add( listener );
		}
	}
	
	public void
	removeListener(
		Object		listener )
	{
		synchronized( listeners ){
			
			listeners.remove( listener );
		}
	}
	
	public void
	dispatch(
		int		type,
		Object	value )
	{
		if ( async ){
			
			synchronized( dispatch_queue ){
				
				dispatch_queue.add(new Object[]{new Integer(type), value});
				
				dispatch_sem.release();
			}
		}else{
			
			if ( target_with_exception != null ){
				
				throw( new RuntimeException( "call dispatchWithException, not dispatch"));
			}
			
			try{
				dispatchInternal( type, value );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
	}	
	
	public void
	dispatchWithException(
		int		type,
		Object	value )
	
		throws Throwable
	{
		dispatchInternal( type, value );
	}
	
	public void
	dispatch(
		Object	listener,
		int		type,
		Object	value )
	{
		if ( async ){
			
			synchronized( dispatch_queue ){
				
				dispatch_queue.add(new Object[]{ listener, new Integer(type), value});
				
				dispatch_sem.release();
			}
		}else{
			
			if ( target_with_exception != null ){
				
				throw( new RuntimeException( "call dispatchWithException, not dispatch"));
			}
			
			target.dispatch( listener, type, value );
		}
	}

	protected void
	dispatchInternal(
		int		type,
		Object	value )
	
		throws Throwable
	{
			// take a copy of the listeners as concurrent modification is possible as
			// we're not synchronized on them
		
		Object[]	listeners_copy;
		
		synchronized( listeners ){
		
			listeners_copy = new Object[ listeners.size() ];
			
			listeners.toArray( listeners_copy );
		}
		
		for (int i=0;i<listeners_copy.length;i++){
			
			if ( target_with_exception != null ){
				
				// System.out.println( name + ":dispatchWithException" );
				
				target_with_exception.dispatchWithException( listeners_copy[i], type, value );
				
			}else{
				
				try{
					// System.out.println( name + ":dispatch" );
					
					target.dispatch( listeners_copy[i], type, value );
					
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
		}
	}
	
	public void
	dispatchLoop()
	{
		while(true){
			
			dispatch_sem.reserve();
			
			Object[] data;
			
			synchronized( dispatch_queue ){
				
				data = (Object[])dispatch_queue.remove(0);
			}
			
			try{
				if ( data.length == 3 ){
					
					target.dispatch(data[0], ((Integer)data[1]).intValue(), data[2] );
					
				}else{
					
					dispatchInternal(((Integer)data[0]).intValue(), data[1] );
				}
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
	}
}

