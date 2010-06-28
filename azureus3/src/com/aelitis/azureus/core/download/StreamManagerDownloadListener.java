/*
 * Created on Jun 21, 2010
 * Created by Paul Gardner
 * 
 * Copyright 2010 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.download;

public interface 
StreamManagerDownloadListener 
{
	public void
	updateActivity(
		String		str );
	
	public void
	updateStats(
		int			secs_until_playable,
		int			buffer_secs,
		long		buffer_bytes,
		int			target_buffer_secs );
	
	public void
	ready();
	
	public void
	failed(
		Throwable 	error );
}
