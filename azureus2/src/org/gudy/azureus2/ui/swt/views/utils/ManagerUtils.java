/*
 * File    : ManagerUtils.java
 * Created : 7 d�c. 2003}
 * By      : Olivier
 *
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 *
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views.utils;

import com.aelitis.azureus.core.AzureusCore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.host.TRHostException;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerException;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;

/**
 * @author Olivier
 *
 */
public class ManagerUtils {
  
  public static void run(DownloadManager dm) {
    if(dm != null) {
      Program.launch(dm.getTorrentSaveDirAndFile());
    }
  }

 /**
  * Opens the parent folder of dm's path
  * @param dm DownloadManager instance
  */
  public static void open(DownloadManager dm) {
    if(dm != null) {
        PlatformManager mgr = PlatformManagerFactory.getPlatformManager();

        if(mgr.hasCapability(PlatformManagerCapabilities.ShowFileInBrowser)) {
            try
            {
                PlatformManagerFactory.getPlatformManager().showFile(dm.getTorrentSaveDirAndFile());
                return;
            }
            catch (PlatformManagerException e)
            {
                Debug.printStackTrace(e);
            }
        }

        Program.launch(dm.getTorrentSaveDir()); // default launcher
    }
  }
  
  public static boolean isStartable(DownloadManager dm) {
    if(dm == null)
      return false;
    int state = dm.getState();
    if (state != DownloadManager.STATE_STOPPED) {
      return false;
    }
    return true;
  }
  
  public static boolean isStopable(DownloadManager dm) {
    if(dm == null)
      return false;
    int state = dm.getState();
    if (	state == DownloadManager.STATE_STOPPED ||
    		state == DownloadManager.STATE_STOPPING	) {
      return false;
    }
    return true;
  }
  
  public static boolean
  isForceStartable(
  	DownloadManager	dm )
  {
    if(dm == null){
        return false;
  	}
    
    int state = dm.getState();
    
    if (	state != DownloadManager.STATE_STOPPED && state != DownloadManager.STATE_QUEUED &&
            state != DownloadManager.STATE_SEEDING && state != DownloadManager.STATE_DOWNLOADING){

    	return( false );
    }
    
    return( true );
  }
  
  public static void 
  host(
  	AzureusCore		azureus_core,
	DownloadManager dm,
	Composite 		panel) 
  {
    if(dm == null)
      return;
    TOTorrent torrent = dm.getTorrent();
    if (torrent != null) {
      try {
      	azureus_core.getTrackerHost().hostTorrent(torrent);
      } catch (TRHostException e) {
        MessageBox mb = new MessageBox(panel.getShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText(MessageText.getString("MyTorrentsView.menu.host.error.title"));
        mb.setMessage(MessageText.getString("MyTorrentsView.menu.host.error.message").concat("\n").concat(e.toString()));
        mb.open();
      }
    }
  }
  
  public static void 
  publish(
  		AzureusCore		azureus_core,
		DownloadManager dm,
		Composite		 panel) 
  {
    if(dm == null)
     return;
    TOTorrent torrent = dm.getTorrent();
    if (torrent != null) {
      try {
      	azureus_core.getTrackerHost().publishTorrent(torrent);
      } catch (TRHostException e) {
        MessageBox mb = new MessageBox(panel.getShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText(MessageText.getString("MyTorrentsView.menu.host.error.title"));
        mb.setMessage(MessageText.getString("MyTorrentsView.menu.host.error.message").concat("\n").concat(e.toString()));
        mb.open();
      }
    }
  }
  
  
  public static void 
  start(
  		DownloadManager dm) 
  {
    if (dm != null && dm.getState() == DownloadManager.STATE_STOPPED) {
    	
      dm.setState(DownloadManager.STATE_WAITING);
    }
  }

  public static void 
  queue(
  		DownloadManager dm,
		Composite panel) 
  {
    if (dm != null) {
    	if (dm.getState() == DownloadManager.STATE_STOPPED){
    		
    		dm.setState(DownloadManager.STATE_QUEUED);
    		
    		/* parg - removed this - why would we want to effectively stop + restart
    		 * torrents that are running? This is what happens if the code is left in.
    		 * e.g. select two torrents, one stopped and one downloading, then hit "queue"
    		 
    		 }else if (	dm.getState() == DownloadManager.STATE_DOWNLOADING || 
    				dm.getState() == DownloadManager.STATE_SEEDING) {
    		
    			stop(dm,panel,DownloadManager.STATE_QUEUED);
    		*/
      }
    }
  }
  
  public static void stop(DownloadManager dm,Composite panel) {
  	stop(dm, panel, DownloadManager.STATE_STOPPED);
  }
  
  public static void 
  stop(
  		DownloadManager dm,
		Composite panel,
		int stateAfterStopped ) 
  {
  	
    if (	dm != null && 
    		dm.getState() != DownloadManager.STATE_STOPPED &&
			dm.getState() != DownloadManager.STATE_STOPPING && 
			dm.getState() != stateAfterStopped) {
    	
      if (dm.getState() == DownloadManager.STATE_SEEDING
          && dm.getStats().getShareRatio() >= 0
          && dm.getStats().getShareRatio() < 1000
          && COConfigurationManager.getBooleanParameter("Alert on close", true)) {
        MessageBox mb = new MessageBox(panel.getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
        mb.setText(MessageText.getString("seedmore.title"));
        mb.setMessage(
            MessageText.getString("seedmore.shareratio")
            + (dm.getStats().getShareRatio() / 10)
            + "%.\n"
            + MessageText.getString("seedmore.uploadmore"));
        int action = mb.open();
        if (action == SWT.YES){
        	asyncStop( dm, stateAfterStopped );
        }
      }else {
  
      	asyncStop( dm, stateAfterStopped );
      }
    }
  }
  
  	public static void
	asyncStop(
		final DownloadManager	dm,
		final int 				stateAfterStopped )
  	{
    	new AEThread( "asyncStop", true )
		{
    		public void
			runSupport()
    		{
    			dm.stopIt( stateAfterStopped, false, false );
    		}
		}.start();
  	}
  	
  	public static void
	asyncStopAll()
  	{
		new AEThread( "asyncStopAll", true )
		{
			public void
			runSupport()
			{
       			MainWindow.getWindow().getGlobalManager().stopAllDownloads();
			}
			
		}.start();
  	}
  	
  	public static void
	asyncPause()
  	{
     	new AEThread( "asyncPause", true )
		{
    		public void
			runSupport()
    		{
    			MainWindow.getWindow().getGlobalManager().pauseDownloads();
    		}
		}.start();
  	}
}
