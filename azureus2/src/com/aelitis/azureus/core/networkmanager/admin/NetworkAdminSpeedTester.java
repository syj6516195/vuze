/**
* Created on Apr 17, 2007
* Created by Alan Snyder
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


package com.aelitis.azureus.core.networkmanager.admin;


public interface NetworkAdminSpeedTester 
{
	public static final int TEST_TYPE_UPLOAD_AND_DOWNLOAD 	= 0;
    public static final int TEST_TYPE_UPLOAD_ONLY 			= 1;
    public static final int TEST_TYPE_DOWNLOAD_ONLY 		= 2;

    public static final int[] TEST_TYPES = { TEST_TYPE_UPLOAD_AND_DOWNLOAD, TEST_TYPE_UPLOAD_ONLY, TEST_TYPE_DOWNLOAD_ONLY };
    
	public int getTestType();

    public void setMode( int mode );

    public int getMode();
    
    public void addListener( NetworkAdminSpeedTesterListener listener);
    
    public void removeListener( NetworkAdminSpeedTesterListener listener);
}
