/*
 * Created on Apr 16, 2004
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

package org.gudy.azureus2.platform.win32.access.impl;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.platform.win32.access.*;
import org.gudy.azureus2.platform.win32.*;

public class 
AEWin32AccessInterface 
{
	public static final int	HKEY_CLASSES_ROOT		= AEWin32Access.HKEY_CLASSES_ROOT;
	public static final int	HKEY_CURRENT_CONFIG		= AEWin32Access.HKEY_CURRENT_CONFIG;
	public static final int	HKEY_LOCAL_MACHINE		= AEWin32Access.HKEY_LOCAL_MACHINE;
	public static final int	HKEY_CURRENT_USER		= AEWin32Access.HKEY_CURRENT_USER;

	public static final int	WM_QUERYENDSESSION		=       0x0011;
	public static final int	WM_ENDSESSION           =       0x0016;
	
	private static AEWin32AccessCallback		cb;
	
	static{
		System.loadLibrary( PlatformManagerImpl.DLL_NAME );
		
		try{
			initialise();
			
		}catch( Throwable e ){
			
			System.out.println( "Old aereg version, please update!" );
		}
	}
	
	protected static void
	load(
		AEWin32AccessCallback	_callback )
	{	
		cb = _callback;
	}
	
	public static long
	callback(
		int		msg,
		int		param1,
		long	param2 )
	{
		if ( cb == null ){
			
			System.out.println( "callback: " + msg + "/" + param1 + "/" + param2 );
			
			return( -1 );
		}else{
			
			return( cb.windowsMessage( msg, param1, param2 ));
		}
	}
	
	protected static native void
	initialise()
	
		throws AEWin32AccessExceptionImpl;
	
	protected static native void
	destroy()
	
		throws AEWin32AccessExceptionImpl;
	
	protected static native String
	getVersion();
	
	protected static native String
	readStringValue(
		int		type,		// HKEY type from above
		String	subkey,
		String	value_name )
	
		throws AEWin32AccessExceptionImpl;
	
	protected static native void
	writeStringValue(
		int		type,		// HKEY type from above
		String	subkey,
		String	value_name,
		String	value_value )
	
		throws AEWin32AccessExceptionImpl;
	
	protected static native int
	readWordValue(
		int		type,		// HKEY type from above
		String	subkey,
		String	value_name )
	
		throws AEWin32AccessExceptionImpl;
	
	protected static native void
	writeWordValue(
		int		type,		// HKEY type from above
		String	subkey,
		String	value_name,
		int		value_value )
	
		throws AEWin32AccessExceptionImpl;
	

	protected static native void
	deleteKey(
		int		type,
		String	subkey,
		boolean	recursive )
	
		throws AEWin32AccessExceptionImpl;
	
	protected static native void
	deleteValue(
		int		type,
		String	subkey,
		String 	value_namae )
	
		throws AEWin32AccessExceptionImpl;
	
	public static native void
	createProcess(
		String		command_line,
		boolean		inherit_handles )
	
		throws AEWin32AccessException;
	
	public static native void
	moveToRecycleBin(
		String		file_name )
	
		throws AEWin32AccessException;
}
