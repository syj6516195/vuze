/*
 * Created on 13-Mar-2004
 * Created by James Yeh
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

package org.gudy.azureus2.platform;

/**
 * Enum for a PlatformManager's reported capabilities
 * @version 1.0 Initial Version
 * @since 1.4
 * @author James Yeh
 */
public final class PlatformManagerCapabilities
{
    public static final PlatformManagerCapabilities CreateCommandLineProcess = new PlatformManagerCapabilities("CreateCommandLineProcess");
    public static final PlatformManagerCapabilities NativeNotification = new PlatformManagerCapabilities("NativeNotification");
    public static final PlatformManagerCapabilities NativeScripting = new PlatformManagerCapabilities("NativeScripting");

    public static final PlatformManagerCapabilities PlaySystemAlert = new PlatformManagerCapabilities("PlaySystemAlert");

    public static final PlatformManagerCapabilities GetUserDataDirectory = new PlatformManagerCapabilities("GetUserDataDirectory");

    public static final PlatformManagerCapabilities RecoverableFileDelete = new PlatformManagerCapabilities("RecoverableFileDelete");
    public static final PlatformManagerCapabilities RegisterFileAssociations = new PlatformManagerCapabilities("RegisterFileAssociations");
    public static final PlatformManagerCapabilities ShowFileInBrowser = new PlatformManagerCapabilities("ShowFileInBrowser");
    public static final PlatformManagerCapabilities ShowPathInCommandLine = new PlatformManagerCapabilities("ShowPathInCommandLine");

    private final String myName; // for debug only

    private PlatformManagerCapabilities(String name)
    {
        myName = name;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return myName;
    }
}
