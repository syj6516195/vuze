/*
 * Created on 11-May-2004
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

package org.gudy.azureus2.plugins.update;

/**
 * @author parg
 *
 */

public interface 
UpdateCheckInstance 
{
	public void
	start();
	
	public void
	cancel();
	
	public UpdateChecker[]
	getCheckers();
	
	public Update[]
	getUpdates();
	
	public UpdateInstaller
	createInstaller()
		
		throws UpdateException;
	
		/**
		 * Add a further updatable component to this instance. Must be called before
		 * the check process is started
		 * @param component
		 * @param mandatory
		 */
	
	public void
	addUpdatableComponent(
		UpdatableComponent		component,
		boolean					mandatory );
	
	public void
	addListener(
		UpdateCheckInstanceListener	l );
	
	public void
	removeListener(
		UpdateCheckInstanceListener	l );
}
