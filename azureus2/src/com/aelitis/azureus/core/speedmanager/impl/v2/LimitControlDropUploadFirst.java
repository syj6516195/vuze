package com.aelitis.azureus.core.speedmanager.impl.v2;

/**
 * Created on Jul 9, 2007
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

public class LimitControlDropUploadFirst implements LimitControl
{

    private float valueUp=0.5f;//number between 0.0 - 1.0
    int upMax;
    int upCurr;
    int upMin;
    SaturatedMode upUsage;

    private float valueDown=1.0f;
    int downMax;
    int downCurr;
    int downMin;
    SaturatedMode downUsage;


    TransferMode mode;

    float usedUpMaxDownloadMode=0.6f;

    public void updateStatus(int currUpLimit, SaturatedMode uploadUsage,
                             int currDownLimit, SaturatedMode downloadUsage,
                             TransferMode transferMode){
        upCurr = currUpLimit;
        upUsage = uploadUsage;
        downCurr = currDownLimit;
        downUsage = downloadUsage;

        mode=transferMode;
    }


    public void updateLimits(int _upMax, int _upMin, int _downMax, int _downMin){
        
        //verify the limits.
        upMax = _upMax;
        upMin = _upMin;
        downMax = _downMax;
        downMin = _downMin;
    }


    private int usedUploadCapacity(){

        float usedUpMax = upMax;
        if( mode.getMode() == TransferMode.State.SEEDING ){
            usedUpMax = upMax;
        }else if( mode.getMode()==TransferMode.State.DOWNLOADING ){
            usedUpMax = upMax*usedUpMaxDownloadMode;
        }else if( mode.getMode()==TransferMode.State.DOWNLOAD_LIMIT_SEARCH ){
            usedUpMax = upMax*usedUpMaxDownloadMode;
        }else if( mode.getMode()==TransferMode.State.UPLOAD_LIMIT_SEARCH ){    
            usedUpMax = upMax;
        }else{

            SpeedManagerLogger.trace("LimitControlDropUploadFirst -> unrecognized transfer mode. ");
        }

        return Math.round( usedUpMax );
    }

    public void updateSeedSettings(float downloadModeUsed)
    {
        if( downloadModeUsed < 1.0f && downloadModeUsed > 0.1f){
            usedUpMaxDownloadMode = downloadModeUsed;
            SpeedManagerLogger.trace("LimitControlDropUploadFirst %used upload used while downloading: "+downloadModeUsed);
        }
    }

    public SMUpdate adjust( float amount ){

        boolean increase = true;
        if( amount<0.0f ){
            increase = false;
        }

        float factor = amount/10.0f;
        int usedUpMax = usedUploadCapacity();
        float gamma = (float) usedUpMax/downMax;

        if( increase ){
            //increase download first
            if( valueDown<0.99f ){
                valueDown = calculateNewValue(valueDown,factor);
            }else{
                //only increase upload if used.
                if( upUsage==SaturatedMode.AT_LIMIT ){
                    valueUp = calculateNewValue(valueUp,gamma*factor);
                }else{
                    SpeedManagerLogger.trace("LmitControlDropUploadFirst not increasing limit, since not AT_LIMIT.");
                }
            }
        }else{
            //decrease upload first
            if( valueUp > 0.01f){
                valueUp = calculateNewValue(valueUp,gamma*factor);
            }else{
                valueDown = calculateNewValue(valueDown,factor);
            }
        }

        return update();
    }//adjust

    private SMUpdate update(){
        int upLimit;
        int downLimit;

        int usedUpMax = usedUploadCapacity();

        upLimit = Math.round( ((usedUpMax-upMin)*valueUp)+upMin );
        downLimit = Math.round( ((downMax-downMin)*valueDown)+downMin );

        //log this change.
        String msg = " create-update: valueUp="+valueUp+",upLimit="+upLimit+",valueDown="+valueDown
                +",downLimit="+downLimit+",upMax="+upMax+",usedUpMax="+usedUpMax+",upMin="+upMin+",downMax="+downMax
                +",downMin="+downMin+",transferMode="+mode.getString();
        SpeedManagerLogger.log( msg );

        return new SMUpdate(upLimit,true,downLimit,true);
    }

    private float calculateNewValue(float curr, float amount){
        curr += amount;
        if( curr > 1.0f){
            curr = 1.0f;
        }
        if( curr < 0.0f ){
            curr = 0.0f;
        }
        return curr;
    }

}
