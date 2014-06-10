/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.core3.util;

/**
 * @author TuxPaper
 * @created Mar 21, 2007
 *
 */
public abstract class AERunnableBoolean
	implements Runnable
{
	private Boolean[] returnValueObject;

	private AESemaphore sem;

	private String id = "AEReturningRunnable";

	public void run() {
		try {
			//System.out.println(this + "]" + id + " run start");
			boolean b = runSupport();
			//System.out.println(this + "]" + id + " runSupport Done: ret=" + b);
			if (returnValueObject != null && returnValueObject.length > 0) {
				returnValueObject[0] = b;
			}
		} catch (Throwable e) {
			Debug.out(id, e);
		} finally {
			//System.out.println(this + "]" + id + " sem=" + sem);
			if (sem != null) {
				//System.out.println(this + "]" + id + " sem Release");
				sem.releaseForever();
			}
		}
	}

	public void setupReturn(String ID, Boolean[] returnValueObject,
			AESemaphore sem) {
		id = ID;
		this.returnValueObject = returnValueObject;
		this.sem = sem;
	}

	public abstract boolean runSupport();
}
