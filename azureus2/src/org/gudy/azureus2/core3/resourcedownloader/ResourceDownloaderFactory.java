/*
 * File    : TorrentDownloader2Factory.java
 * Created : 27-Feb-2004
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

package org.gudy.azureus2.core3.resourcedownloader;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.resourcedownloader.impl.*;

public class 
ResourceDownloaderFactory 
{
	public static ResourceDownloader
	create(
		String	url )
	{
		return( new ResourceDownloaderImpl( url ));
	}
	
	public static ResourceDownloader
	getRetryDownloader(
		ResourceDownloader		downloader,
		int						retry_count )
	{
		return( new ResourceDownloaderRetryImpl( downloader, retry_count ));
	}
	
	public static ResourceDownloader
	getTimeoutDownloader(
		ResourceDownloader		downloader,
		int						timeout_millis )
	{
		return( new ResourceDownloaderTimeoutImpl( downloader, timeout_millis ));
	}
	
	public static ResourceDownloader
	getAlternateDownloader(
		ResourceDownloader[]		downloaders )
	{
		return( new ResourceDownloaderAlternateImpl( downloaders ));
	}
	
}
