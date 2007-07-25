package com.aelitis.azureus.core.speedmanager.impl.v2;

import com.aelitis.azureus.core.speedmanager.SpeedManagerListener;

/**
 * Created on Jul 24, 2007
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 * <p/>
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
 * <p/>
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

public class SpeedLimitListener implements SpeedManagerListener {

    SpeedLimitMonitor mon;

    public SpeedLimitListener(SpeedLimitMonitor limitMonitor){

        mon = limitMonitor;

    }

    public void propertyChanged(int property) {

        String type="unknown";
        if( property == SpeedManagerListener.PR_ASN ){
            type = "ASN change";
        }else if( property == SpeedManagerListener.PR_DOWN_CAPACITY ){
            type = "download capacity";
        }else if( property == SpeedManagerListener.PR_UP_CAPACITY ){
            type = "upload capacity";
        }

        SpeedManagerLogger.log("Update from SpeedManagerPingMapper property="+type);

        mon.readFromPersistentMap();
        mon.updateFromCOConfigManager();

    }

}//class
