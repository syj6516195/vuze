/*
 * Created on 27-Aug-2004
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

package org.gudy.azureus2.core3.tracker.client;

/**
 * @author parg
 *
 */

public interface 
TRTrackerScraperClientResolver 
{
	public static final int	ST_NOT_FOUND		= 1;
	public static final int	ST_RUNNING			= 2;	// downloading, seeding or queued
	public static final int	ST_OTHER			= 3;

		/**
		 * Gives access to a restricted set of states for this torrent from ST_ set
		 * @param torrent_hash
		 * @return
		 */
	
	public int
	getStatus(
		byte[]	torrent_hash );
	
		/**
		 * returns the current tracker client for the torrent, if it exists
		 * @param torrent_hash
		 * @return
		 */
	
	public TRTrackerClient
	getClient(
		byte[]	torrent_hash );
}
