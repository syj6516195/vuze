/*
 * Created on 06-Aug-2004
 * Created by Paul Gardner
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

package com.aelitis.azureus.core.diskmanager.cache;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.core3.torrent.TOTorrent;

public interface 
CacheFileManagerStats 
{
	public long
	getSize();
	
	public long
	getUsedSize();
	
	public long
	getBytesWrittenToCache();
	
	public long
	getBytesWrittenToFile();
	
	public long
	getBytesReadFromCache();
	
	public long
	getBytesReadFromFile();
	
	public long
	getAverageBytesWrittenToCache();
	
	public long
	getAverageBytesWrittenToFile();
	
	public long
	getAverageBytesReadFromCache();
	
	public long
	getAverageBytesReadFromFile();
	
		// returns the number of bytes in the requested range that are in cache
	
	public long
	getBytesInCache(
		TOTorrent		torrent,
		int				piece_number,
		int				offset,
		long			length );
}
