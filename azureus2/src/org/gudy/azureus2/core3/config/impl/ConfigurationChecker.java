/*
 * File    : ConfigurationChecker.java
 * Created : 8 oct. 2003 23:04:14
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
 
package org.gudy.azureus2.core3.config.impl;


import java.util.HashMap;
import java.io.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.security.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.LGLogger;

import com.aelitis.azureus.core.proxy.AEProxy;
import com.aelitis.azureus.core.proxy.AEProxyFactory;

/**
 * 
 * The purpose of this class is to provide a way of checking that the config file
 * contains valid values when azureus is started.
 * 
 * 
 * @author Olivier
 * 
 */
public class 
ConfigurationChecker 
{
  //private static boolean migrated				= false;
	 
  private static boolean system_properties_set	= false;
  
  private static boolean checked 				= false;
   
  private static AEMonitor	class_mon	= new AEMonitor( "ConfigChecker");
  
  
  /*
  protected static void
  migrateConfig()
  {
  	try{
  		class_mon.enter();
  	
	  	if ( migrated ){
	  		
	  		return;
	  	}
	  	
	  	migrated	= true;
	    
	    migrateOldConfigFiles();
	    
  	}finally{
  		
  		class_mon.exit();
  	}
  }
  */
  
  protected static void
  setSystemProperties()
  {
  	try{
  		class_mon.enter();
  	
	  	if ( system_properties_set ){
	  		
	  		return;
	  	}
	  	
	  	system_properties_set	= true;
	    	  	
	  	String	handlers = System.getProperty( "java.protocol.handler.pkgs" );
	  	
	  	if ( handlers == null ){
	  		
	  		handlers = "org.gudy.azureus2.core3.util.protocol";
	  	}else{
	  		
	  		handlers += "|org.gudy.azureus2.core3.util.protocol";
	  	}
	  	
	  	System.setProperty( "java.protocol.handler.pkgs", handlers );
	  	 	
	  		// DNS cache timeouts
	  	
	  	System.setProperty("sun.net.inetaddr.ttl", "60");
	  	System.setProperty("networkaddress.cache.ttl", "60");
      
      
      //see http://developer.apple.com/releasenotes/Java/Java142RN/ResolvedIssues/chapter_3_section_7.html
      //fixes the osx kernel panic bug caused by Apple's faulty kqueue implementation (as of 10.3.6)
      if( Constants.isOSX ) {
        System.setProperty( "java.nio.preferSelect", "true" );
      }
      
      
      // socket connect/read timeouts
	  	
	  	int	connect_timeout = COConfigurationManager.getIntParameter( "Tracker Client Connect Timeout");
	  	int	read_timeout 	= COConfigurationManager.getIntParameter( "Tracker Client Read Timeout");
	  	
	  	LGLogger.log( "TrackerClient: connect timeout = " + connect_timeout + ", read timeout = " + read_timeout );
	  	
	  	System.setProperty(
	  			"sun.net.client.defaultConnectTimeout", 
				String.valueOf( connect_timeout*1000 ));
	  			
	  	System.setProperty(
	  			"sun.net.client.defaultReadTimeout", 
	  			String.valueOf( read_timeout*1000 ));
	    
	    // proxy
	    if ( COConfigurationManager.getBooleanParameter("Enable.Proxy", false) ) {
	      String host = COConfigurationManager.getStringParameter("Proxy.Host");
	      String port = COConfigurationManager.getStringParameter("Proxy.Port");
	      String user = COConfigurationManager.getStringParameter("Proxy.Username");
	      String pass = COConfigurationManager.getStringParameter("Proxy.Password");
		      
	      if ( COConfigurationManager.getBooleanParameter("Enable.SOCKS", false) ) {
	        System.setProperty("socksProxyHost", host);
	        System.setProperty("socksProxyPort", port);
	        
	        if (user.length() > 0) {
	          System.setProperty("java.net.socks.username", user);
	          System.setProperty("java.net.socks.password", pass);
	        }
	      }
	      else {
	        System.setProperty("http.proxyHost", host);
	        System.setProperty("http.proxyPort", port);
	        System.setProperty("https.proxyHost", host);
	        System.setProperty("https.proxyPort", port);
	        
	        if (user.length() > 0) {
	          System.setProperty("http.proxyUser", user);
	          System.setProperty("http.proxyPassword", pass);
	        }
	      }
	    }else{
	    		// no explicit proxy, install our own baby for testing purposes at the moment
	    	
	    	try{
	    		AEProxy	proxy = 
	    			AEProxyFactory.create( 0, 0 );
	    		
		        System.setProperty("socksProxyHost", "127.0.0.1");
		        System.setProperty("socksProxyPort", "" + proxy.getPort());

	    		
	    	}catch( Throwable e ){
	    		
	    		Debug.printStackTrace(e);
	    	}
	    	
	    }
	  
	  	SESecurityManager.initialise();
  	}finally{
  		
  		class_mon.exit();
  	}
  }
  
  public static void 
  checkConfiguration() 
  { 
  	try{
  		class_mon.enter();

	    if(checked)
	      return;
	    checked = true;
	    
	    boolean	changed	= false;
	    
	    String	last_version = COConfigurationManager.getStringParameter( "azureus.version", "" );
	    
	    String	this_version	= Constants.AZUREUS_VERSION;
	    
	    if ( !last_version.equals( this_version )){
	    
	    	COConfigurationManager.setParameter( "azureus.version", this_version );
	    	
	    	changed	= true;
	    }
	    
	    	// migration from default-save-dir enable = true to false
	    	// if the user hadn't explicitly set a value then we want to stick with true
	    
	    if ( last_version.length() == 0 ){
	    	
	    		// "last version" introduced at same time as the default save dir problem
	    		// which was the release after 2.2.0.0
	    	
	    		// only do this on an already existing configuration. Easiest way to test
	    		// for this is the "diagnostics.tidy_close" flag
	    	
	    	if ( 	COConfigurationManager.doesParameterNonDefaultExist( "diagnostics.tidy_close" ) && 
	    			!COConfigurationManager.doesParameterNonDefaultExist( "Use default data dir" )){
	    		
	    		COConfigurationManager.setParameter( "Use default data dir", true );
	    		
	    		changed	= true;
	    	}
	    	
	    		// also, if we now have a default data dir enabled (either explicitly or by
	    		// above migration fix), and there's no value defined for the dir, then
	    		// set it to what it would have been before the default was changed to blank
	    	
	    	if ( 	COConfigurationManager.getBooleanParameter( "Use default data dir" ) &&
	    			!COConfigurationManager.doesParameterNonDefaultExist( "Default save path" )){	
	    		
	    		COConfigurationManager.setParameter( "Default save path", SystemProperties.getUserPath()+"downloads" );
	    		
	    		changed	= true;
	    	}
	    }
	    
      
      /*
       * Old migration code... 
       * 
	    int nbMinSeeds = COConfigurationManager.getIntParameter("StartStopManager_iIgnoreSeedCount", -1);
	    if (nbMinSeeds == -1) {
	    COConfigurationManager.setParameter("StartStopManager_iIgnoreSeedCount", 0);
	      // not set yet.. import from "Start Num Peers"
	    int nbOldMinSeeds = COConfigurationManager.getIntParameter("Start Num Peers", -1);
	    if (nbOldMinSeeds != -1)
	      COConfigurationManager.setParameter("StartStopManager_iIgnoreSeedCount", nbOldMinSeeds);
	    changed = true;
	    }
	
	    //migrate from older BPs setting to newer KBs setting
	    int speed = COConfigurationManager.getIntParameter("Max Upload Speed", -1);
	    if ( speed > -1 ) {      
	      COConfigurationManager.setParameter("Max Upload Speed KBs", speed / 1024);
	      COConfigurationManager.setParameter("Max Upload Speed", -1);
	      changed = true;
	    }
	    
	    //migrate to new dual connection limit option
	    int maxclients = COConfigurationManager.getIntParameter("Max Clients", -1);
	    if ( maxclients > -1 ) {      
	      COConfigurationManager.setParameter("Max.Peer.Connections.Per.Torrent", maxclients);
	      COConfigurationManager.setParameter("Max Clients", -1);
	      changed = true;
	    }
      
      //if previous config did not use shared port, grab the port
      if (!COConfigurationManager.getBooleanParameter("Server.shared.port", true)) {
        int lp = COConfigurationManager.getIntParameter("Low Port", 6881);
        COConfigurationManager.setParameter("TCP.Listen.Port", lp);
        COConfigurationManager.setParameter("Server.shared.port", true);
        changed = true;
      }
      */
      
	    
	    // migrate to split tracker client/server key config
	    
	    if ( !COConfigurationManager.doesParameterDefaultExist( "Tracker Key Enable Client")){
	    	
	    	boolean	old_value = COConfigurationManager.getBooleanParameter("Tracker Key Enable");
	    	
	    	COConfigurationManager.setParameter("Tracker Key Enable Client", old_value);
	    	
	    	COConfigurationManager.setParameter("Tracker Key Enable Server", old_value);
	    	
	    	changed = true;
	    }
	    
	    int maxUpSpeed 		= COConfigurationManager.getIntParameter("Max Upload Speed KBs",0);
	    int maxDownSpeed 	= COConfigurationManager.getIntParameter("Max Download Speed KBs",0);
	    
	    if(	maxUpSpeed > 0 && 
	    	maxUpSpeed < COConfigurationManager.CONFIG_DEFAULT_MIN_MAX_UPLOAD_SPEED &&
			(	maxDownSpeed == 0 || maxDownSpeed > (2*maxUpSpeed ))){
	    	
	      changed = true;
	      COConfigurationManager.setParameter("Max Upload Speed KBs", COConfigurationManager.CONFIG_DEFAULT_MIN_MAX_UPLOAD_SPEED);
	    }
	    
	
	    int peersRatio = COConfigurationManager.getIntParameter("Stop Peers Ratio",0);
	    if(peersRatio > 14) {
	      COConfigurationManager.setParameter("Stop Peers Ratio", 14);
	      changed = true;
	    }
	    
	    int minQueueingShareRatio = COConfigurationManager.getIntParameter("StartStopManager_iFirstPriority_ShareRatio");
	    if (minQueueingShareRatio < 500) {
	      COConfigurationManager.setParameter("StartStopManager_iFirstPriority_ShareRatio", 500);
	      changed = true;
	    }
	    
	    int iSeedingMin = COConfigurationManager.getIntParameter("StartStopManager_iFirstPriority_SeedingMinutes");
	    if (iSeedingMin < 90 && iSeedingMin != 0) {
	      COConfigurationManager.setParameter("StartStopManager_iFirstPriority_SeedingMinutes", 90);
	      changed = true;
	    }
	
	    int iDLMin = COConfigurationManager.getIntParameter("StartStopManager_iFirstPriority_DLMinutes");
	    if (iDLMin < 60*3 && iDLMin != 0) {
	      COConfigurationManager.setParameter("StartStopManager_iFirstPriority_DLMinutes", 60*3);
	      changed = true;
	    }
	
	    String uniqueId = COConfigurationManager.getStringParameter("ID",null);
	    if(uniqueId == null || uniqueId.length() != 20) {
	      uniqueId = generatePeerId();      
	      COConfigurationManager.setParameter("ID", uniqueId);
	      changed = true;
	    }
	    
	    
	    /**
	     * Special Patch for OSX users
	     */
	    if (Constants.isOSX) {
	      boolean sound = COConfigurationManager.getBooleanParameter("Play Download Finished",true);
	      boolean close = COConfigurationManager.getBooleanParameter("Close To Tray",true);
	      boolean min = COConfigurationManager.getBooleanParameter("Minimize To Tray",true);
	      boolean confirmExit = COConfigurationManager.getBooleanParameter("confirmationOnExit",false);
	      
	      if ( sound || close || min || confirmExit ) {
	        COConfigurationManager.setParameter("Play Download Finished",false);
	        COConfigurationManager.setParameter("Close To Tray",false);
	        COConfigurationManager.setParameter("Minimize To Tray",false);
	        COConfigurationManager.setParameter("confirmationOnExit",false);
	        changed = true;
	      }
	    }
	    
	    
	    
	    //remove a trailing slash, due to user manually entering the path in config
	    String[] path_params = { "Default save path",
	                             "General_sDefaultTorrent_Directory",
	                             "Watch Torrent Folder Path",
	                             "Completed Files Directory" };
	    for( int i=0; i < path_params.length; i++ ) {
	      if( path_params[i].endsWith( SystemProperties.SEP ) ) {
	        String new_path = path_params[i].substring( 0, path_params[i].length() - 1 );
	        COConfigurationManager.setParameter( path_params[i], new_path );
	        changed = true;
	      }
	    }
      
      
      //2105 removed the language file web-update functionality,
      //but old left-over MessagesBundle.properties files in the user dir
      //cause display text problems, so let's delete them.
	    if( ConfigurationManager.getInstance().doesParameterNonDefaultExist( "General_bEnableLanguageUpdate" ) ) {        
        File user_dir = new File( SystemProperties.getUserPath() );
        File[] files = user_dir.listFiles( new FilenameFilter() {
          public boolean accept(File dir, String name) {
            if( name.startsWith( "MessagesBundle" ) && name.endsWith( ".properties" ) ) {
              return true;
            }
            return false;
          }
        });
        
        for( int i=0; i < files.length; i++ ) {
          File file = files[ i ];
          if( file.exists() ) {
            LGLogger.log( "ConfigurationChecker:: removing old language file: " + file.getAbsolutePath() );
            file.renameTo( new File( file.getParentFile(), "delme" + file.getName() ) );
          }
        }

        ConfigurationManager.getInstance().removeParameter( "General_bEnableLanguageUpdate" );
        changed = true;
      }
      
      
	
	    if(changed) {
	      COConfigurationManager.save();
	    } 
  	}finally{
  		
  		class_mon.exit();
  	}
  }
  
  public static String generatePeerId() {
    String uniqueId = "";
    long currentTime = SystemTime.getCurrentTime();
    for(int i = 0 ; i < currentTime % 1000 ; i++)
      Math.random();            
    //Allocate a 10 random chars ID
    String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    for(int i = 0 ; i < 20 ; i++) {
      int pos = (int) ( Math.random() * chars.length());
      uniqueId += chars.charAt(pos);
    }
    return uniqueId;
  }
  
  public static void main(String args[]) {
    Integer obj = new Integer(1);
    HashMap test = new HashMap();
    int collisions = 0;
    for(int i = 0 ; i < 1000000 ; i++) {
      String id = generatePeerId();
      if(test.containsKey(id)) {
        collisions++;
      } else {
        test.put(id,obj);
      }
      if(i%1000 == 0) {
        System.out.println(i + " : " + id + " : " + collisions);
      }
    }
    System.out.println("\n" + collisions);
  }
  
  
  
  /*
   * Migrates old user files/dirs from application dir to user dir
   *
  private static void migrateOldConfigFiles() {
    if ( COConfigurationManager.getBooleanParameter("Already_Migrated", false)) {
      return;
    }
    
    String results = "";
    
    //migrate from old buggy APPDATA dir to new registry-culled dir
    if( Constants.isWindows ) {
      String old_dir = SystemProperties.getEnvironmentalVariable( "APPDATA" );
      if( old_dir != null && old_dir.length() > 0 ) {
        old_dir = old_dir + SystemProperties.SEP + "Azureus" + SystemProperties.SEP;
        results += migrateAllFiles( old_dir, SystemProperties.getUserPath() );
      }
    }
    
    //migrate from old ~/Library/Azureus/ to ~/Library/Application Support/Azureus/
    if( Constants.isOSX ) {
      String old_dir = System.getProperty("user.home") + "/Library/Azureus/";
      results += migrateAllFiles( old_dir, SystemProperties.getUserPath() );
    }
    
    //migrate from old ~/Azureus/ to ~/.Azureus/
    if( Constants.isLinux ) {
      String old_dir = System.getProperty("user.home") + "/Azureus/";
      results += migrateAllFiles( old_dir, SystemProperties.getUserPath() );
    }

    ConfigurationManager.getInstance().load();
    COConfigurationManager.setParameter("Already_Migrated", true);
    
    if( results.length() > 0 ) {
    	String[] params = { results };
    	LGLogger.logAlertUsingResource(LGLogger.INFORMATION, "AutoMigration.useralert", params);
    }
  }
    
  private static String migrateAllFiles( String source_path, String dest_path ) {
    String result = "";
    File source_dir = new File( source_path );
    File dest_dir = new File( dest_path );
    if( source_dir.exists() && !source_path.equals( dest_path ) ) {
      if( !dest_dir.exists() ) dest_dir.mkdirs();
      File[] files = source_dir.listFiles();
      for( int i=0; i < files.length; i++ ) {
        File source_file = files[ i ];
        File dest_file = new File( dest_dir, source_file.getName() );
        boolean ok = FileUtil.renameFile( source_file, dest_file );
        if( ok ) result += source_file.toURI().getPath() + "\n---> " + dest_file.toURI().getPath() + " : OK\n";
        else result += source_file.toURI().getPath() + "\n---> " + dest_file.toURI().getPath() + " : FAILED\n";
      }
      source_dir.delete();
    }
    return result;
  }
  */
  
  
  
}
