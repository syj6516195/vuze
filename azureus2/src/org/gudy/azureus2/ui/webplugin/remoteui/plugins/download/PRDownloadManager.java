/*
 * File    : PRDownloadManager.java
 * Created : 28-Jan-2004
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

package org.gudy.azureus2.ui.webplugin.remoteui.plugins.download;

/**
 * @author parg
 *
 */

import java.io.*;
import java.net.*;

import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.torrent.*;

import org.gudy.azureus2.ui.webplugin.remoteui.plugins.*;

public class 
PRDownloadManager
	extends		RPObject
	implements 	DownloadManager
{
	protected transient DownloadManager		delegate;
	
	public static PRDownloadManager
	create(
		DownloadManager		_delegate )
	{
		PRDownloadManager	res =(PRDownloadManager)_lookupLocal( _delegate );
		
		if ( res == null ){
			
			res = new PRDownloadManager( _delegate );
		}
		
		return( res );
	}
	
	protected
	PRDownloadManager(
		DownloadManager		_delegate )
	{
		super( _delegate );
		
		delegate	= _delegate;
	}
	
	public void
	_setLocal()
	
		throws RPException
	{
		delegate = (DownloadManager)_fixupLocal();
	}
	
	
	public RPReply
	_process(
			RPRequest	request	)
	{
		String	method = request.getMethod();
		
		if ( method.equals( "getDownloads")){
			
			Download[]	downloads = delegate.getDownloads();
			
			PRDownload[]	res = new PRDownload[downloads.length];
			
			for (int i=0;i<res.length;i++){
				
				res[i] = PRDownload.create( downloads[i]);
			}
			
			return( new RPReply( res ));
		}
		
		throw( new RPException( "Unknown method: " + method ));
	}
	
	
	public void 
	addDownload(
		File 	torrent_file )

		throws DownloadException
	{
		notSupported();
	}
	
	public void 
	addDownload(
		URL		url )
	
		throws DownloadException
	{
		notSupported();
	}
	
	
	public Download
	addDownload(
		Torrent		torrent,
		File		torrent_location,
		File		data_location )
	
		throws DownloadException
	{
		notSupported();
		
		return( null );
	}
	
	
	public Download
	addNonPersistentDownload(
		Torrent		torrent,
		File		torrent_location,
		File		data_location )
	
		throws DownloadException
	{
		notSupported();
		
		return( null );
	}
	
	
	public Download
	getDownload(
		Torrent		torrent )
	{
		notSupported();
		
		return( null );
	}
	
	
	public Download[]
	getDownloads()
	{
		PRDownload[]	res = (PRDownload[])dispatcher.dispatch( new RPRequest( this, "getDownloads", null )).getResponse();
		
		for (int i=0;i<res.length;i++){
			
			res[i]._setRemote( dispatcher );
		}
		
		return( res );
	}
	
	
	public void
	addListener(
		DownloadManagerListener	l )
	{
		notSupported();
	}
	
	
	public void
	removeListener(
		DownloadManagerListener	l )
	{
		notSupported();
	}	
}