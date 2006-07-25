/*
 * Created on Jul 28, 2004
 * Created by Alon Rohter
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
package com.aelitis.azureus.core.networkmanager.impl.tcp;


import java.nio.channels.*;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.*;

import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.networkmanager.VirtualChannelSelector;



/**
 * Provides a simplified and safe (selectable-channel) socket single-op selector.
 */
public class VirtualChannelSelectorImpl {
	
	/*
	static boolean	rm_trace 	= false;
	static boolean	rm_test_fix = false;
	
	static{
	
		COConfigurationManager.addAndFireParameterListeners(
				new String[]{ "user.rm.trace", "user.rm.testfix" },
				new ParameterListener()
				{
					public void 
					parameterChanged(
						String parameterName)
					{
						rm_trace 	= COConfigurationManager.getBooleanParameter( "user.rm.trace", false );
						rm_test_fix = COConfigurationManager.getBooleanParameter( "user.rm.testfix", false );
					}
				});
	}
	
	private long rm_flag_last_log;
	private Map	rm_listener_map = new HashMap();
	*/
	
    protected Selector selector;
    private final SelectorGuard selector_guard;
    
    private final LinkedList 	register_cancel_list 		= new LinkedList();
    private final AEMonitor 	register_cancel_list_mon	= new AEMonitor( "VirtualChannelSelector:RCL");

    private final HashMap paused_states = new HashMap();
    
    private final int 		INTEREST_OP;
    private final boolean	pause_after_select;

    protected final VirtualChannelSelector parent;
    
    
    //private int[] select_counts = new int[ 50 ];
    //private int round = 0;
    
    private volatile boolean	destroyed;
    
    
    public VirtualChannelSelectorImpl( VirtualChannelSelector _parent, int _interest_op, boolean _pause_after_select ) {	
      this.parent = _parent;
      INTEREST_OP = _interest_op;
      
      pause_after_select	= _pause_after_select;
      
      String type;
      switch( INTEREST_OP ) {
        case VirtualChannelSelector.OP_CONNECT:
          type = "OP_CONNECT";  break;
        case VirtualChannelSelector.OP_READ:
          type = "OP_READ";  break;
        default:
          type = "OP_WRITE";  break;
      }
      
      
      selector_guard = new SelectorGuard( type, new SelectorGuard.GuardListener() {
        public boolean safeModeSelectEnabled() {
          return parent.isSafeSelectionModeEnabled();
        }
        
        public void spinDetected() {
          closeExistingSelector();
          try {  Thread.sleep( 1000 );  }catch( Throwable x ) {x.printStackTrace();}
          parent.enableSafeSelectionMode();
        }
        
        public void failureDetected() {
          try {  Thread.sleep( 10000 );  }catch( Throwable x ) {x.printStackTrace();}
          closeExistingSelector();
          try {  Thread.sleep( 1000 );  }catch( Throwable x ) {x.printStackTrace();}
          selector = openNewSelector();
        }
      });
      
      selector = openNewSelector();
    }
    
  
    
    protected Selector openNewSelector() {
      Selector sel = null;
      
      try {
        sel = Selector.open();
      }
      catch (Throwable t) {
        Debug.out( "ERROR: caught exception on Selector.open()", t );
        
        try {  Thread.sleep( 3000 );  }catch( Throwable x ) {x.printStackTrace();}
        
        int fail_count = 1;
        
        while( fail_count < 10 ) {
          try {
            sel = Selector.open();
            break;
          }
          catch( Throwable f ) {
            Debug.out( f );
            fail_count++;
            try {  Thread.sleep( 3000 );  }catch( Throwable x ) {x.printStackTrace();}
          }
        }
        
        if( fail_count < 10 ) { //success ! 
          Debug.out( "NOTICE: socket Selector successfully opened after " +fail_count+ " failures." );
        }
        else {  //failure
        	Logger.log(new LogAlert(LogAlert.REPEATABLE, LogAlert.AT_ERROR,
						"ERROR: socket Selector.open() failed 10 times in a row, aborting."
								+ "\nAzureus / Java is likely being firewalled!"));
        }
      }
      
      return sel;
    }
    
    
    
    
   
     
    public void pauseSelects( AbstractSelectableChannel channel ) {
      
      //System.out.println( "pauseSelects: " + channel + " - " + Debug.getCompressedStackTrace() );
      
      if( channel == null ) {
        return;
      }
      
      SelectionKey key = channel.keyFor( selector );
      
      if( key != null && key.isValid() ) {
        key.interestOps( key.interestOps() & ~INTEREST_OP );
      }
      else {  //channel not (yet?) registered
        if( channel.isOpen() ) {  //only bother if channel has not already been closed
          try{  register_cancel_list_mon.enter();
          
            paused_states.put( channel, new Boolean( true ) );  //ensure the op is paused upon reg select-time reg

          }
          finally{  register_cancel_list_mon.exit();  }
        }
      }
    }
    


    
    public void resumeSelects( AbstractSelectableChannel channel ) {
      //System.out.println( "resumeSelects: " + channel + " - " + Debug.getCompressedStackTrace() );
      if( channel == null ) {
        Debug.printStackTrace( new Exception( "resumeSelects():: channel == null" ) );
        return;
      }
      
      SelectionKey key = channel.keyFor( selector );
      
      if( key != null && key.isValid() ) {
        key.interestOps( key.interestOps() | INTEREST_OP );
      }
      else {  //channel not (yet?) registered
        try{  register_cancel_list_mon.enter();
          paused_states.remove( channel );  //check if the channel's op has been already paused before select-time reg
        }
        finally{  register_cancel_list_mon.exit();  }
      }
      
      //try{
      //  selector.wakeup();
      //}
      //catch( Throwable t ) {  Debug.out( "selector.wakeup():: caught exception: ", t );   }
    }



    
    public void 
	cancel( 
		AbstractSelectableChannel channel ) 
    {
      //System.out.println( "cancel: " + channel + " - " + Debug.getCompressedStackTrace() );
    				 
    	if ( destroyed ){
    		    		
    		// don't worry too much about cancels
    	}
    	
    	try{
    		register_cancel_list_mon.enter();
      	   		
    			// ensure that there's only one operation outstanding for a given channel
    			// at any one time (the latest operation requested )
    		
    		for (Iterator it = register_cancel_list.iterator();it.hasNext();){
    			
    			Object	obj = it.next();
    			  		   				
    			if ( 	channel == obj ||
    					(	obj instanceof RegistrationData &&
    								((RegistrationData)obj).channel == channel )){
    					
    						// remove existing cancel or register
    					   				
    				it.remove();
    				
    				break;
    			}
    		}
    		   	
			pauseSelects((AbstractSelectableChannel)channel );
			
  			register_cancel_list.add( channel );
    		
    	}finally{
    		
    		register_cancel_list_mon.exit();
    	}
    }
    
    
    
    public void 
	register( 
		AbstractSelectableChannel 								channel, 
		VirtualChannelSelector.VirtualAbstractSelectorListener 	listener, 
		Object 													attachment ) 
    {
    	if ( destroyed ){
     			
   			Debug.out( "register called after selector destroyed" );
    	}
    	
    	try{
    		register_cancel_list_mon.enter();
      	   		
    			// ensure that there's only one operation outstanding for a given channel
    			// at any one time (the latest operation requested )
    		
    		for (Iterator it = register_cancel_list.iterator();it.hasNext();){
    			
    			Object	obj = it.next();
    			
				if ( channel == obj ||
   						(	obj instanceof RegistrationData &&
								((RegistrationData)obj).channel == channel )){
				
					it.remove();
				
					break;
    			}
    		}
				
			paused_states.remove( channel );
			
  			register_cancel_list.add( new RegistrationData( channel, listener, attachment ));
    		
    	}finally{
    		
    		register_cancel_list_mon.exit();
    	}
    }
    
    
    
    public int select( long timeout ) {
    	
      long select_start_time = SystemTime.getCurrentTime();
      
      if( selector == null ) {
        Debug.out( "VirtualChannelSelector.select() op called with null selector" );
        try {  Thread.sleep( 3000 );  }catch( Throwable x ) {x.printStackTrace();}
        return 0;
      } 
      
      if( !selector.isOpen()) {
          Debug.out( "VirtualChannelSelector.select() op called with closed selector" );
          try {  Thread.sleep( 3000 );  }catch( Throwable x ) {x.printStackTrace();}
          return 0;
      }  
      
      	// store these when they occur so they can be raised *outside* of the monitor to avoid
      	// potential deadlocks
      
      RegistrationData	select_fail_data	= null;
      Throwable 		select_fail_excep	= null;
      
      //process cancellations
      try {
      	register_cancel_list_mon.enter();
        
      		// don't use an iterator here as it is possible that error notifications to listeners
      		// can result in the addition of a cancel request.
      		// Note that this can only happen for registrations, and this *should* only result in
      		// possibly a cancel being added (i.e. not a further registration), hence this can't
      		// loop. Also note the approach of removing the entry before processing. This is so
      		// that the logic used when adding a cancel (the removal of any matching entries) does
      		// not cause the entry we're processing to be removed
      	
        while( register_cancel_list.size() > 0 ){
        	
          Object	obj = register_cancel_list.remove(0);
         
          if ( obj instanceof AbstractSelectableChannel ){
           
         		// process cancellation
         	
        	  AbstractSelectableChannel	canceled_channel = (AbstractSelectableChannel)obj;
  
            try{
              SelectionKey key = canceled_channel.keyFor( selector );
	            
              if( key != null ){
	            	
                key.cancel();  //cancel the key, since already registered
              }
	            
            }catch( Throwable e ){
         		
              Debug.printStackTrace(e);
            }
          }else{
            //process new registrations  
 
            RegistrationData data = (RegistrationData)obj;
            	
            if( data == null ) {
              Debug.out( "data == null" );
            }
            
            if( data.channel == null ) {
              Debug.out( "data.channel == null" );
            }
            
            try {
              if( data.channel.isOpen() ){
                	
                // see if already registered
                SelectionKey key = data.channel.keyFor( selector );
                  
                if ( key != null && key.isValid() ) {  //already registered
                  key.attach( data );
                  key.interestOps( key.interestOps() | INTEREST_OP );  //ensure op is enabled
                }
                else{
                  data.channel.register( selector, INTEREST_OP, data );
                }
                  
                //check if op has been paused before registration moment
                Object paused = paused_states.get( data.channel );
                  
                if( paused != null ) {
                  pauseSelects( data.channel );  //pause it
                }
              }
              else{
            	
              	select_fail_data	= data;
              	select_fail_excep	= new Throwable( "select registration: channel is closed" );
              	
              }
            }catch (Throwable t){
              
            	Debug.printStackTrace(t);
           	    
           		select_fail_data	= data;
           		select_fail_excep	= t;
            } 	
          }
        }
        
        paused_states.clear();  //reset after every registration round
               
      }finally { 
      	
      	register_cancel_list_mon.exit();
      }
      
      if ( select_fail_data != null ){
      	
      	try{
	      	parent.selectFailure( 
	      			select_fail_data.listener,
					select_fail_data.channel, 
					select_fail_data.attachment, 
					select_fail_excep );
	      	
      	}catch( Throwable e ){
      		
      		Debug.printStackTrace( e );
      	}
      }
      
  
      //do the actual select
      int count = 0;
      selector_guard.markPreSelectTime();
      try {
        count = selector.select( timeout );
      }
      catch (Throwable t) {
        Debug.out( "Caught exception on selector.select() op: " +t.getMessage(), t );
        try {  Thread.sleep( timeout );  }catch(Throwable e) { e.printStackTrace(); }
      }
      
      	// do this after the select so that any pending cancels (prior to destroy) are processed
      	// by the selector before we kill it
      
 	  if ( destroyed ){
  		
 	    closeExistingSelector();
 	    	
 	    return( 0 );
 	  }
 	   	  
      /*
      if( INTEREST_OP == VirtualChannelSelector.OP_READ ) {  //TODO
      	select_counts[ round ] = count;
      	round++;
      	if( round == select_counts.length ) {
      		StringBuffer buf = new StringBuffer( select_counts.length * 3 );
      		
      		buf.append( "select_counts=" );
      		for( int i=0; i < select_counts.length; i++ ) {
      			buf.append( select_counts[i] );
      			buf.append( ' ' );
      		}
      		
      		//System.out.println( buf.toString() );
      		round = 0;
      	}
      }
      */
      
      selector_guard.verifySelectorIntegrity( count, SystemTime.TIME_GRANULARITY_MILLIS /2 );
      
      if( !selector.isOpen() )  return count;
      
      int	progress_made_key_count	= 0;
      int	total_key_count			= 0;
      
      //notification of ready keys via listener callback
      for( Iterator i = selector.selectedKeys().iterator(); i.hasNext(); ) {
    	  
    	total_key_count++;
    	
        SelectionKey key = (SelectionKey)i.next();
        i.remove();
        RegistrationData data = (RegistrationData)key.attachment();

        // int	rm_type;
        
        if( key.isValid() ) {
          if( (key.interestOps() & INTEREST_OP) == 0 ) {  //it must have been paused between select and notification
        	// rm_type = 2;
          }else{            
            
	          if( pause_after_select ) { 
	            key.interestOps( key.interestOps() & ~INTEREST_OP );
	          }
	                        
	          boolean	progress_indicator = parent.selectSuccess( data.listener, data.channel, data.attachment );
	          
	          if ( progress_indicator ){
	            
	        	// rm_type = 0;
	        	
	        	progress_made_key_count++;
	        	  
	            data.non_progress_count = 0;
	            
	          }else{
	            
	        	// rm_type = 1;
	        	  
	            data.non_progress_count++;
	            	
	            if ( 	data.non_progress_count == 10 ||
	            		data.non_progress_count %100 == 0 && data.non_progress_count > 0 ){
	            		
	              Debug.out( 
	                  "VirtualChannelSelector: No progress for op " + INTEREST_OP + 
	                  	": listener = " + data.listener.getClass() + 
	                  	", count = " + data.non_progress_count +
	                  	", socket: open = " + data.channel.isOpen() + 
	                  		(INTEREST_OP==VirtualChannelSelector.OP_ACCEPT?"":
	                  			(", connected = " + ((SocketChannel)data.channel).isConnected())));
	                			  
	            		
	              if ( data.non_progress_count == 1000 ){
	                
	                Debug.out( "No progress for " + data.non_progress_count + ", closing connection" );
	            			
	                try{
	                  data.channel.close();
	            				
	                }catch( Throwable e ){
	            				e.printStackTrace();
	                }
	              }
	            }
	          }
	        }
        }else{
          // rm_type = 3;
          key.cancel();
          parent.selectFailure( data.listener, data.channel, data.attachment, new Throwable( "key is invalid" ) );
          // can get this if socket has been closed between select and here
        }
        
        /*
        if ( rm_trace ){
        	
          	Object	rm_key = data.listener.getClass();
          	
          	int[]	rm_count = (int[])rm_listener_map.get( rm_key );
          	
          	if ( rm_count == null ){
          		
          		rm_count = new int[]{0,0,0,0};
          		
          		rm_listener_map.put( rm_key, rm_count );
          	}
          	
          	rm_count[rm_type]++;
          }
          */
      }
        
      	// if any of the ready keys hasn't made any progress then enforce minimum sleep period to avoid
      	// spinning
      
      if ( total_key_count == 0 || progress_made_key_count != total_key_count ){
    	  
	      long time_diff = SystemTime.getCurrentTime() - select_start_time;
	      
	      if( time_diff < timeout && time_diff >= 0 ) {  //ensure that it always takes at least 'timeout' time to complete the select op
	      	try {  Thread.sleep( timeout - time_diff );  }catch(Throwable e) { e.printStackTrace(); }      
	      }
      }else{
    	  /*
    	  if ( rm_test_fix ){
    		 
    	      long time_diff = SystemTime.getCurrentTime() - select_start_time;
    	      
    	      if( time_diff < 10 && time_diff >= 0 ) { 
    	      	try {  Thread.sleep( 10 - time_diff );  }catch(Throwable e) { e.printStackTrace(); }      
    	      } 
    	  }
    	  */
      }
      
      /*
      if ( rm_trace ){
    	  
    	  if ( select_start_time - rm_flag_last_log > 10000 ){
    		  
    		  rm_flag_last_log	= select_start_time;
    		  
    		  Iterator it = rm_listener_map.entrySet().iterator();
    		
    		  String	str = "";
    		  
    		  while( it.hasNext()){
    			  
    			  Map.Entry	entry = (Map.Entry)it.next();
    			  
    			  Class	cla = (Class)entry.getKey();
    			  
    			  String	name = cla.getName();
    			  int		pos = name.lastIndexOf('.');
    			  name = name.substring( pos+1 );
    			  
    			  int[]	counts = (int[])entry.getValue();
    			  
    			  str += (str.length()==0?"":",")+ name + ":" + counts[0]+"/"+counts[1]+"/"+counts[2]+"/"+counts[3];
    		  }
    		  
       		  Debug.outNoStack( "RM trace: " + hashCode() + ": op=" + INTEREST_OP + "-" + str ); 
    	  }
      }
      */
      
      return count;
    }
    
    	/**
    	 * Note that you have to ensure that a select operation is performed on the normal select
    	 * loop *after* destroying the selector to actually cause the destroy to occur
    	 */
    
    public void
    destroy()
    {
    	destroyed	= true;
    }
    
    protected void closeExistingSelector() {
      for( Iterator i = selector.keys().iterator(); i.hasNext(); ) {
        SelectionKey key = (SelectionKey)i.next();
        RegistrationData data = (RegistrationData)key.attachment();
        parent.selectFailure(data.listener, data.channel, data.attachment, new Throwable( "selector destroyed" ) );
      }
      
      try{
        selector.close();
      }
      catch( Throwable t ) { t.printStackTrace(); }
    }
    
    
    
    
    private static class RegistrationData {
        protected final AbstractSelectableChannel channel;
        protected final VirtualChannelSelector.VirtualAbstractSelectorListener listener;
        protected final Object attachment;
        
        protected int non_progress_count;
        
      	private RegistrationData( AbstractSelectableChannel _channel, VirtualChannelSelector.VirtualAbstractSelectorListener _listener, Object _attachment ) {
      		channel 		= _channel;
      		listener		= _listener;
      		attachment 		= _attachment;
      	}
      }
          
}
