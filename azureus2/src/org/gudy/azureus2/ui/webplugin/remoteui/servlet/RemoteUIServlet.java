/*
 * File    : RemoteUIServlet.java
 * Created : 27-Jan-2004
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

package org.gudy.azureus2.ui.webplugin.remoteui.servlet;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.ui.webplugin.*;

import java.io.*;
import java.util.zip.*;
import java.util.jar.*;

import org.gudy.azureus2.plugins.tracker.web.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.torrent.*;

import org.gudy.azureus2.pluginsremote.*;
import org.gudy.azureus2.ui.webplugin.util.*;

public class 
RemoteUIServlet
	extends WebPlugin
{
	static String[] resource_icon_names = {
			//"ui/icons/start2.png",
			"ui/icons/openFolder16x12.gif",
			"ui/icons/start.gif",
			"ui/icons/stop.gif",
			"ui/icons/delete.gif",
			"ui/icons/recheck.gif",		
	};
	
	static String[] resource_names = {
			
		"ui/common/UIImageRepository.class",
		
		"ui/swing/UISwingImageRepository.class",	
		
		"ui/swt/views/AbstractIView.class",
		"ui/swt/views/IView.class",
		"ui/swt/IconBarEnabler.class",
		
		"ui/webplugin/remoteui/applet/RemoteUIApplet.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanelAdaptor.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel.class",
		"ui/webplugin/remoteui/applet/model/MDDownloadModel.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView.class",
		"ui/webplugin/remoteui/applet/view/TableSorter.class",
		"ui/webplugin/remoteui/applet/view/TableMap.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView$1.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView$2.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView$3.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView$4.class",
		"ui/webplugin/remoteui/applet/view/VWDownloadView$5.class",
		"ui/webplugin/remoteui/applet/view/TableSorter$1.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$1.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$2.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$3.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$4.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$5.class",
		"ui/webplugin/remoteui/applet/RemoteUIApplet$1.class",
		"ui/webplugin/remoteui/applet/model/MDConfigModelListener.class",
		"ui/webplugin/remoteui/applet/model/MDConfigModel.class",
		"ui/webplugin/remoteui/applet/view/VWConfigView.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$6.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$7.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$8.class",
		"ui/webplugin/remoteui/applet/RemoteUIMainPanel$9.class",
		"ui/webplugin/remoteui/applet/view/VWConfigView$1.class",
		"ui/webplugin/remoteui/applet/view/VWGridBagConstraints.class",
		"ui/webplugin/remoteui/applet/RemoteUIApplet$3.class",
		"ui/webplugin/remoteui/applet/RemoteUIApplet$2.class",
		"ui/webplugin/remoteui/applet/view/VWStatusAreaView.class",
		"ui/webplugin/remoteui/applet/view/VWLabel.class",
		"ui/webplugin/remoteui/applet/model/MDStatusAreaModel.class",
		"ui/webplugin/remoteui/applet/view/VWStatusEntryBorder.class",
		"ui/webplugin/remoteui/applet/model/MDConfigModelPropertyChangeEvent.class",
		"ui/webplugin/util/WUJarReader.class",
		
		"core3/config/COConfigurationManager.class",
		"core3/config/impl/ConfigurationManager.class",
		"core3/config/impl/ConfigurationParameterNotFoundException.class",
		"core3/util/DisplayFormatters.class",
		"core3/config/ParameterListener.class",
		"core3/util/DisplayFormatters$1.class",
		"core3/util/Semaphore.class",
		"core3/torrentdownloader/TorrentDownloaderException.class",
		
		"pluginsremote/RPRequestDispatcher.class",
		"pluginsremote/RPException.class",
		"pluginsremote/RPFactory.class",
		"pluginsremote/RPRequest.class",
		"pluginsremote/RPRequestHandler.class",		
		"pluginsremote/RPObject.class",
		"pluginsremote/RPReply.class",
		"pluginsremote/RPPluginInterface.class",
		"pluginsremote/download/RPDownloadManager.class",
		"pluginsremote/download/RPDownload.class",
		"pluginsremote/torrent/RPTorrent.class",
		"pluginsremote/download/RPDownloadStats.class",
		"pluginsremote/download/RPDownloadAnnounceResult.class",
		"pluginsremote/download/RPDownloadScrapeResult.class",
		"pluginsremote/RPPluginConfig.class",
		"pluginsremote/torrent/RPTorrentManager.class",
		"pluginsremote/torrent/RPTorrentDownloader.class",

		"plugins/download/DownloadManager.class",
		"plugins/PluginInterface.class",
		"plugins/PluginListener.class",
		"plugins/PluginView.class",
		"plugins/ui/config/Parameter.class",
		"plugins/ui/tables/peers/PluginPeerItemFactory.class",
		"plugins/ui/config/ConfigSection.class",
		"plugins/tracker/Tracker.class",
		"plugins/tracker/web/TrackerWebContext.class",
		"plugins/logging/Logger.class",
		"plugins/peers/protocol/PeerProtocolManager.class",
		"plugins/sharing/ShareManager.class",
		"plugins/sharing/ShareException.class",
		"plugins/PluginConfig.class",
		"plugins/ui/config/PluginConfigUIFactory.class",
		"plugins/PluginEventListener.class",
		"plugins/download/DownloadException.class",
		"plugins/download/Download.class",
		"plugins/download/DownloadManagerListener.class",
		"plugins/torrent/Torrent.class",
		"plugins/download/DownloadStats.class",
		"plugins/download/DownloadScrapeResult.class",
		"plugins/download/DownloadAnnounceResult.class",
		"plugins/download/DownloadRemovalVetoException.class",
		"plugins/download/DownloadListener.class",
		"plugins/download/DownloadTrackerListener.class",
		"plugins/download/DownloadWillBeRemovedListener.class",
		"plugins/torrent/TorrentFile.class",
		"plugins/torrent/TorrentException.class",
		"plugins/ui/tables/mytorrents/PluginMyTorrentsItemFactory.class",
		"plugins/PluginException.class",
		"plugins/torrent/TorrentManager.class",
		"plugins/torrent/TorrentDownloader.class",
		"plugins/ipfilter/IPFilter.class",
		"plugins/torrent/TorrentAnnounceURLList.class",
		"plugins/utils/Utilities.class",
	};
	
	protected RPRequestHandler		request_handler;
	
	public
	RemoteUIServlet()
	{
		super();
	}
	
	public void 
	initialize(
		PluginInterface _plugin_interface )
	
		throws PluginException
	{	
		super.initialize( _plugin_interface );
		
		request_handler = new RPRequestHandler( _plugin_interface );
	}
	
	public boolean
	generateSupport(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		String	url = request.getURL();
		
		if ( url.equals( "/remui.jar" ) || url.equals( "/remuiicons.jar" )){
			
			JarOutputStream	jos = null;
			
			try{
				jos = new JarOutputStream( response.getOutputStream());
			
				WUJarBuilder.buildFromResources( 
						jos, 
						plugin_interface.getPluginClassLoader(), 
						"org/gudy/azureus2", 
						url.equals( "/remui.jar")?resource_names:resource_icon_names );
				
				response.setContentType("application/java-archive");
				
				return( true );
				
			}finally{
				
				if ( jos != null ){

					jos.close();
				}
			}
		}else if ( url.equals( "/process.cgi")){
	
			ObjectInputStream	dis = null;
			
			try{
				dis = new ObjectInputStream( new GZIPInputStream(request.getInputStream()));
								
				RPRequest	rp_request = (RPRequest)dis.readObject();
				
				// System.out.println( "RemoteUIServlet:got request: " + rp_request.getString());
				
				RPReply	reply = request_handler.processRequest( rp_request );
				
				if ( reply == null ){
					
					reply = new RPReply( null );
				}
				
				response.setContentType( "application/octet-stream" );
				
				ObjectOutputStream	oos = new ObjectOutputStream(new GZIPOutputStream(response.getOutputStream()));
				
				try{
					oos.writeObject( reply );
				
				}finally{
					
					oos.close();
				}
				
				return( true );
				
			}catch( ClassNotFoundException e ){
				
				e.printStackTrace();
				
			}finally{
				
				if ( dis != null ){
					
					dis.close();
				}
			}
		}else if ( url.equals( "/upload.cgi")){

			// -----------------------------7d4f2a310bca
			//Content-Disposition: form-data; name="upfile"; filename="C:\Temp\(HH)-Demon Beast Resurrection 1-2.torrent"
			//Content-Type: application/octet-stream
			//
			// <data>
			// -----------------------------7d4f2a310bca
			
		
			InputStream	is = request.getInputStream();
			
			try{
				
				String	content = "";
				
				while( true ){
					
					byte[]	buffer = new byte[1024];
					
					int	len = is.read(buffer);
					
					if ( len <= 0 ){
						
						break;
					}
					
					content += new String(buffer, 0,len, "ISO-8859-1" );
				}
				
				int	sep1 = content.indexOf( "\r\n" );
				
				String	tag = content.substring(0,sep1);
				
				int	sep2 = content.indexOf( "\r\n\r\n");
				
				int	data_start 	= sep2 + 4;
				int data_end	= content.indexOf(tag, data_start );
				
				byte[]	data = content.substring(data_start, data_end).getBytes("ISO-8859-1");
				
				PrintWriter pw = new PrintWriter( new OutputStreamWriter( response.getOutputStream()));
		
				try{
					Torrent torrent = plugin_interface.getTorrentManager().createFromBEncodedData( data );
				
					plugin_interface.getDownloadManager().addDownload( torrent );
				
					pw.println("<HTML><BODY><P><FONT COLOR=#00CC44>Upload OK</FONT></P></BODY></HTML>");
				
				}catch( Throwable e ){
					
					String	message_chain = "";
					
					Throwable	temp = e;
					
					while( temp != null ){
						
						String	this_message = temp.getMessage();
						
						if ( this_message != null ){
							
							message_chain += (message_chain.length()==0?"":"\n") + this_message;
						}
						
						temp = temp.getCause();
					}
								
					String	message = message_chain.length()==0?e.toString():message_chain;
						

					pw.println("<HTML><BODY><P><FONT COLOR=#FF0000>Upload Failed: " + message + "</FONT></P></BODY></HTML>");
					
				}finally{
					
					pw.close();
				}
				
				return( true );
			}finally{
				
				if ( is != null ){
					
					is.close();
				}
			}
			
		}
		
		return( false );
	}
}

