/*
 * TorrentDownloaderFactory.java
 *
 * Created on 2. November 2003, 03:52
 */

package org.gudy.azureus2.core3.torrentdownloader;

import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.torrentdownloader.impl.TorrentDownloaderImpl;
import org.gudy.azureus2.core3.torrentdownloader.impl.TorrentDownloaderManager;
import org.gudy.azureus2.core3.util.Debug;

/**
 *
 * @author  Tobias Minich
 */
public class TorrentDownloaderFactory {
  
  private static TorrentDownloaderImpl getClass(boolean logged) {
    try {
      return (TorrentDownloaderImpl) Class.forName("org.gudy.azureus2.core3.torrentdownloader.impl.TorrentDownloader"+(logged?"Logged":"")+"Impl").newInstance();
    } catch (Exception e) {
    	Debug.printStackTrace( e );
      return null;
    }
  }
  
  public static TorrentDownloader 
  download(
  	TorrentDownloaderCallBackInterface 	callback, 
	String 								url,
	String								referrer,
	String 								fileordir, 
	boolean 							logged) 
  {
    TorrentDownloaderImpl dl = getClass(logged);
    if (dl!=null)
      dl.init(callback, url, referrer, fileordir);
    return dl;
  }
  
  public static TorrentDownloader 
  download(
  		TorrentDownloaderCallBackInterface 	callback, 
		String 								url,
		String								referrer,
		String 								fileordir) 
  {
    return download(callback, url, referrer, fileordir, false);
  }
  
  public static TorrentDownloader download(TorrentDownloaderCallBackInterface callback, String url, boolean logged) {
    return download(callback, url, null, null, logged);
  }
  
  public static TorrentDownloader download(TorrentDownloaderCallBackInterface callback, String url) {
      return download(callback, url, null, null, false);
  }
  
  public static TorrentDownloader download(String url, String fileordir, boolean logged) {
    return download(null, url, null, fileordir, logged);
  }
  
  public static TorrentDownloader download(String url, String fileordir) {
    return download(null, url, null, fileordir, false);
  }
  
  public static TorrentDownloader download(String url, boolean logged) {
    return download(null, url, null, null, logged);
  }
  
  public static TorrentDownloader download(String url) {
    return download(null, url, null, null, false);
  }
  
  public static void initManager(GlobalManager gm, boolean logged, boolean autostart, String downloaddir) {
    TorrentDownloaderManager.getInstance().init(gm, logged, autostart, downloaddir);
  }
    
  public static TorrentDownloader downloadManaged(String url, String fileordir, boolean logged) {
    return TorrentDownloaderManager.getInstance().download(url, fileordir, logged);
  }
  
  public static TorrentDownloader downloadManaged(String url, String fileordir) {
    return TorrentDownloaderManager.getInstance().download(url, fileordir);
  }
  
  public static TorrentDownloader downloadManaged(String url, boolean logged) {
    return TorrentDownloaderManager.getInstance().download(url, logged);
  }
  
  public static TorrentDownloader downloadManaged(String url) {
    return TorrentDownloaderManager.getInstance().download(url);
  }
}
