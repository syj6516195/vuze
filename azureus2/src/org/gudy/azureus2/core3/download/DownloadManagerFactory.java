/*
 * File    : DownloadManagerFactory.java
 * Created : 19-Oct-2003
 * By      : stuff
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

package org.gudy.azureus2.core3.download;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.download.impl.*;
import org.gudy.azureus2.core3.global.*;

public class 
DownloadManagerFactory 
{
 	public static DownloadManager
 	create(
 		GlobalManager 	gm, 
		byte[]			torrent_hash,
 		String 			torrentFileName, 
 		String 			savePath, 
		boolean 		stopped,
 		boolean			persistent,
		boolean			recovered )
 	{
 		int state = (stopped) ? DownloadManager.STATE_STOPPED : DownloadManager.STATE_WAITING;
 		
		return( new DownloadManagerImpl( gm, torrent_hash, torrentFileName, savePath, null, state, persistent, recovered, false ));
 	}	

	public static DownloadManager
 	create(
 		GlobalManager 	gm, 
		byte[]			torrent_hash,
 		String 			torrentFileName, 
 		String 			torrent_save_dir,
		String			torrent_save_file, 
		boolean 		stopped,
 		boolean			persistent,
		boolean			recovered )
 	{
 		int state = (stopped) ? DownloadManager.STATE_STOPPED : DownloadManager.STATE_WAITING;
 		
		return( new DownloadManagerImpl( gm, torrent_hash, torrentFileName, torrent_save_dir, torrent_save_file, state, persistent, recovered, false ));
 	}
	
	public static DownloadManager
	create(
		GlobalManager 	gm, 
		byte[]			torrent_hash,
		String 			torrentFileName, 
		String 			savePath, 
		int      		initialState,
		boolean			persistent,
		boolean			recovered,
		boolean			for_seeding )
	{
		return( new DownloadManagerImpl( gm, torrent_hash, torrentFileName, savePath, null, initialState, persistent, recovered, for_seeding ));
	}
	
	public static DownloadManager
	create(
		GlobalManager 	gm, 
		byte[]			torrent_hash,
		String 			torrentFileName, 
		String 			torrent_save_dir,
		String			torrent_save_file, 
		int      		initialState,
		boolean			persistent,
		boolean			recovered )
	{
		return( new DownloadManagerImpl( gm, torrent_hash, torrentFileName, torrent_save_dir, torrent_save_file, initialState, persistent, recovered, false ));
	}
}
