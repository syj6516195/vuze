/*
 * Created on May 16, 2004
 * Created by Olivier Chalouhi
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

package org.gudy.azureus2.update;

import java.io.*;
import java.util.*;
import java.text.*;

public class Updater {

  public static final String VERSION  = "1.1";
  
  public static final char NOT_FOUND  = '0';
  public static final char READY      = '1';
  public static final char FOUND      = '2';
    
  public static final String		LOG_FILE_NAME	= "update.log";
  
  		// change these and you'll need to change the UpdateInstaller !!!!
  
	protected static final String	UPDATE_DIR 	= "updates";
	protected static final String	ACTIONS		= "install.act";

  public static void main(String[] args) {   
    if (args.length < 3) {
      System.out.println("Usage: Updater full_classpath full_librarypath full_userpath");
      System.exit(-1);
    }
    
    if ( args[0].equals( "restart" )){
    	
    	newRestart( args );
    }else{
    	
    	oldRestart( args );
    }
  }
  
  protected static void
  oldRestart(
  	String[]	args )
  {
    PrintWriter log = null;
    
    String classPath    = args[0];
    String libraryPath  = args[1];
    String userPath     = args[2];
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
      
    String relativePath = "";
    if(System.getProperty("os.name").equalsIgnoreCase("Mac OS X")) {
      relativePath = "Azureus.app/Contents/Resources/Java/";
    }
    
    File oldFile = new File(userPath, relativePath + "Azureus2.jar");
    File updateFile = new File(oldFile.getParentFile(), "Azureus2-new.jar");
    File logFile = new File( userPath, LOG_FILE_NAME );
    
    try {
      log = new PrintWriter( new FileWriter(logFile, true ));
      
      log.write("Updater:: classPath=" + classPath
                         + " libraryPath=" + libraryPath
                         + " userPath=" + userPath + "\n");
      
      log.write("Updater:: oldFile=" + oldFile.getAbsolutePath()
                    + " updateFile=" + updateFile.getAbsolutePath() + "\n");
    
      log.write("Updater:: testing for " + oldFile.getAbsolutePath() + " .....");
      if(oldFile.isFile()) {
        log.write("exists\n");
        
        log.write("Updater:: testing for " + updateFile.getAbsolutePath() + " .....");
        if(updateFile.isFile()) {
          log.write("exists\n");
          
          log.write("Updater:: attempting to delete " + oldFile.getAbsolutePath() + " ...");
          while(!oldFile.delete()) {
            log.write(" x ");
            Thread.sleep(1000);
          }
          log.write("deleted\n");

          
          log.write("Updater:: attempting to rename " + updateFile.getAbsolutePath() + " ...");
          while(!updateFile.renameTo(oldFile)) {
            log.write(" x ");
            Thread.sleep(1000);
          }
          log.write("renamed\n");
        
          log.write("Updater:: is restarting ");
          restartAzureus(log,"org.gudy.azureus2.ui.swt.Main",new String[0]);
        }
        else log.write("not found\n");
      }
      else log.write("not found\n");
    }
    catch (Exception e) {
        log.write("\nUpdater:: exception:\n" + e.toString());
    }
    finally {
        if (log != null) log.close();
     }
  }
  
  	protected static void
	newRestart(
		String[]	args)
	{
  		String command	    = args[0];
  		String app_path		= args[1];
  		String user_path    = args[2];
 
	    File logFile = new File( user_path, LOG_FILE_NAME );
  		    
	    PrintWriter	log = null;
	    
  		try{
  				// TODO:!!! socket stuff for close sync
  			
  			Thread.sleep(5000);
  			
  		    log = new PrintWriter(new FileWriter(logFile, true));

  		    Calendar now = GregorianCalendar.getInstance();

  		    log.println( "Update Starts: " + now.get(Calendar.HOUR_OF_DAY) + ":" + format(now.get(Calendar.MINUTE)) + ":" + format(now.get(Calendar.SECOND)));        

  		    log.println( "app  dir = " + app_path );
  		    log.println( "user dir = " + user_path );
  		    
  		    File	update_dir = new File( user_path, UPDATE_DIR );
  		    
  		    File[]	inst_dirs = update_dir.listFiles();
  		    
  		    for (int i=0;i<inst_dirs.length;i++){
  		    	
  		    	File	inst_dir = inst_dirs[i];
  		    	
  		    	if ( inst_dir.isDirectory()){
  		    		
  		    		processUpdate( log, inst_dir );
  		    	}
  		    }
  		    
  		}catch( Throwable e ){
  			
  			if ( log != null ){
  				
  				log.println( "Update Fails" );
  				
  				e.printStackTrace( log );
  			}
  		}finally{
  			
  			if ( log != null ){
  			
  				log.println( "Restarting Azureus" );
  			}
  			
            restartAzureus( log, "org.gudy.azureus2.ui.swt.Main", new String[0] );
            
 			if ( log != null ){
 	  			
 	  			log.println( "Restart initiated" );
 	  			
 	  			log.close();
 	  		}
  		}
	}
  	
  	protected static void
	processUpdate(
		PrintWriter	log,
		File		inst_dir )
	{
		log.println( "processing " + inst_dir.toString());
	
		try{
			
			File	commands = new File( inst_dir, ACTIONS );
			
			if ( !commands.exists()){
				
				log.println( "    command file '" + ACTIONS + "' not found, aborting");
				
				return;
			}
			
			LineNumberReader	lnr = new LineNumberReader( new FileReader( commands ));
				
			boolean	failed = false;
			
			while(true){
					
				String	line = lnr.readLine();
					
				if ( line == null ){
						
					break;
				}
					
				log.println( "    command:" + line );
				
				StringTokenizer tok = new StringTokenizer(line, ",");
				
				String	command = tok.nextToken();
				
				if ( command.equals( "move" )){
					
					File	from_file 	= new File(tok.nextToken());
					File	to_file		= new File(tok.nextToken());
					
					if ( to_file.exists()){
						
						if ( !to_file.delete()){
							
							log.println( "failed to delete '" + to_file.toString() + "'");
						}
					}
					
					if ( !from_file.renameTo( to_file )){
						
						log.println( "failed to delete '" + to_file.toString() + "'");
						
						failed	= true;
					}
				}else{
					
					log.println( "unrecognised command '" + command + "'" );
					
					failed	= true;
				}
			}
			
			lnr.close();
			
			if ( !failed ){
				
				deleteDirOrFile( inst_dir );
			}
		}catch( Throwable e ){
  			 				
  			log.println( "processing fails" );
  				
  			e.printStackTrace( log );
		}
	}

  	private static void 
  	deleteDirOrFile(File f) 
	{
  		try{
  	  
  	      if (f.isDirectory()){
  	      	
  	        File[] files = f.listFiles();
  	        
  	        for (int i = 0; i < files.length; i++){
  	        	
  	        	deleteDirOrFile(files[i]);
  	        }
  	      } 
  	      
  	      f.delete();
  
  	    }catch( Exception ignore ){
  	    	
  	    }
  	}
  	
	private static String 
	format(
		int n) 
	{
	   if(n < 10) return "0".concat(String.valueOf(n));
	   return String.valueOf(n);
	}  
  
  /*
   * Follows a complete copy of RestartUtil, with logging options disabled. 
   *
   */
  private static final String restartScriptName = "restartScript";
  
  public static void restartAzureus(PrintWriter log, String mainClass,String[] parameters) {
    String osName = System.getProperty("os.name");
    if(osName.equalsIgnoreCase("Mac OS X")) {
      restartAzureus_OSX(log,mainClass,parameters);
    } else if(osName.equalsIgnoreCase("Linux")) {
      restartAzureus_Linux(log,mainClass,parameters);
    } else {
      restartAzureus_win32(log,mainClass,parameters);
    }
  }
  
  private static void restartAzureus_win32(PrintWriter log,String mainClass,String[] parameters) {
    //Classic restart way using Runtime.exec directly on java(w)
    String classPath = System.getProperty("java.class.path");
    
    String libraryPath = System.getProperty("java.library.path");
    
    if ( libraryPath == null ){
    	libraryPath	= "";
    }else if ( libraryPath.length() > 0 ){
    	libraryPath = "-Djava.library.path=\"" + libraryPath + "\" ";
    }
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = "\"" + javaPath + "javaw\" -classpath \"" + classPath
    + "\" " + libraryPath + mainClass;
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    
    if ( log != null ){
    	log.println( "  " + exec );
    }
    try {                
      Runtime.getRuntime().exec(exec);
    } catch(Exception e) {
        e.printStackTrace(log);
   }
  }
  
  private static void restartAzureus_OSX(PrintWriter log,String mainClass,String[] parameters) {
    String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
    String libraryPath = System.getProperty("java.library.path"); //$NON-NLS-1$
    String userPath = System.getProperty("user.dir"); //$NON-NLS-1$
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = "#!/bin/bash\n\"" + userPath + "/Azureus.app/Contents/MacOS/java_swt\" -classpath \"" + classPath
    + "\" -Duser.dir=\"" + userPath + "\" -Djava.library.path=\"" + libraryPath + "\" " + mainClass ;
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    
    if ( log != null ){
    	log.println( "  " + exec );
    }
    String fileName = userPath + "/" + restartScriptName;
    
    File fUpdate = new File(fileName);
    try {
	    FileOutputStream fosUpdate = new FileOutputStream(fUpdate,false);
	    fosUpdate.write(exec.getBytes());
	    fosUpdate.close();
	    Process pChMod = Runtime.getRuntime().exec("chmod 755 " + fileName);
	    pChMod.waitFor();
	    Process p = Runtime.getRuntime().exec("./" + restartScriptName);
    } catch(Exception e) {
      e.printStackTrace(log);
    }
  }
  
  private static void restartAzureus_Linux(PrintWriter log,String mainClass,String[] parameters) {
    String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
    String libraryPath = System.getProperty("java.library.path"); //$NON-NLS-1$
    String userPath = System.getProperty("user.dir"); //$NON-NLS-1$
    String javaPath = System.getProperty("java.home")
                    + System.getProperty("file.separator")
                    + "bin"
                    + System.getProperty("file.separator");
    
    String exec = "#!/bin/bash\n\"" + javaPath + "java\" -classpath \"" + classPath
    + "\" -Duser.dir=\"" + userPath + "\" -Djava.library.path=\"" + libraryPath + "\" " + mainClass ;
    for(int i = 0 ; i < parameters.length ; i++) {
      exec += " \"" + parameters[i] + "\"";
    }
    
    if ( log != null ){
    	log.println( "  " + exec );
    }
    
    String fileName = userPath + "/" + restartScriptName;
    
    File fUpdate = new File(fileName);
    try {
	    FileOutputStream fosUpdate = new FileOutputStream(fUpdate,false);
	    fosUpdate.write(exec.getBytes());
	    fosUpdate.close();
	    Process pChMod = Runtime.getRuntime().exec("chmod 755 " + fileName);
	    pChMod.waitFor();
	    Process p = Runtime.getRuntime().exec("./" + restartScriptName);
    } catch(Exception e) {
        e.printStackTrace(log);
    }
  }
  
}
      

