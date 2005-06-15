/* Written and copyright 2001-2003 Tobias Minich.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 *
 *
 * Main.java
 *
 * Created on 22. August 2003, 00:04
 */

package org.gudy.azureus2.ui.console;

import org.apache.log4j.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.ui.common.IUserInterface;
import org.gudy.azureus2.ui.common.UIConst;

/**
 *
 * @author  Tobias Minich
 */
public class UI extends org.gudy.azureus2.ui.common.UITemplateHeadless implements IUserInterface {
  
  private ConsoleInput console = null;
  
  /** Creates a new instance of Main */
  /*public UI() {
  }*/
  
  public void init(boolean first, boolean others) {
    super.init(first,others);
    System.setProperty("java.awt.headless", "true");
  }
  
  public String[] processArgs(String[] args) {
    return args;
  }
  
  public void startUI() {
    super.startUI();
    TorrentDownloaderFactory.initManager(UIConst.getGlobalManager(), true, true, COConfigurationManager.getStringParameter("Default save path") );
    if ((!isStarted()) || (console == null) || ((console!=null) && (!console.isAlive()))) {
//      ConsoleInput.printconsolehelp(System.out);
      System.out.println();
      console = new ConsoleInput("Main", UIConst.getAzureusCore(), System.in, System.out, Boolean.TRUE);
      console.printconsolehelp();
    }
  }
  
  public void openTorrent(String fileName) {
    if( fileName.toUpperCase().startsWith( "HTTP://" ) ) {
      System.out.println( "Downloading torrent from url: " + fileName );
      TorrentDownloaderFactory.downloadManaged( fileName );
      return;
    }
    
    try {
      if (!FileUtil.isTorrentFile(fileName)) {//$NON-NLS-1$
        Logger.getLogger("azureus2.ui.console").error(fileName+" doesn't seem to be a torrent file. Not added.");
        return;
      }
    } catch (Exception e) {
      Logger.getLogger("azureus2.ui.console").error("Something is wrong with "+fileName+". Not added. (Reason: "+e.getMessage()+")");
      return;
    }
    if (UIConst.getGlobalManager()!=null) {
      try {
        UIConst.getGlobalManager().addDownloadManager(fileName, COConfigurationManager.getDirectoryParameter("Default save path"));
      } catch (Exception e) {
        Logger.getLogger("azureus2.ui.console").error("The torrent "+fileName+" could not be added.", e);
      }
    }
  }
  
}