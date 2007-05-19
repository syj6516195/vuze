package org.gudy.azureus2.ui.swt.views.configsections;

import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;
import org.gudy.azureus2.ui.swt.config.IntParameter;
import org.gudy.azureus2.ui.swt.config.BooleanParameter;
import org.gudy.azureus2.ui.swt.config.StringListParameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.SWT;
import com.aelitis.azureus.core.speedmanager.impl.SpeedManagerAlgorithmProviderV2;

/**
 * Created on May 15, 2007
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

public class ConfigSectionTransferAutoSpeedBeta
        implements UISWTConfigSection
{

    BooleanParameter enableV2AutoSpeedBeta;

    StringListParameter strategyList;

    //upload/download limits
    IntParameter downMaxLim;
    IntParameter downMinLim;
    IntParameter uploadMaxLim;
    IntParameter uploadMinLim;

    //vivaldi set-points
    IntParameter vGood;
    IntParameter vGoodTol;
    IntParameter vBad;
    IntParameter vBadTol;

    //DHT ping set-points
    IntParameter dGood;
    IntParameter dGoodTol;
    IntParameter dBad;
    IntParameter dBadTol;
    //general ping set-points.
    IntParameter adjustmentInterval;
    BooleanParameter skipAfterAdjustment;



    /**
     * Create your own configuration panel here.  It can be anything that inherits
     * from SWT's Composite class.
     * Please be mindfull of small screen resolutions
     *
     * @param parent The parent of your configuration panel
     * @return your configuration panel
     */

    /**
     * Returns section you want your configuration panel to be under.
     * See SECTION_* constants.  To add a subsection to your own ConfigSection,
     * return the configSectionGetName result of your parent.<br>
     */
    public String configSectionGetParentSection() {
        return ConfigSection.SECTION_TRANSFER;
    }

    /**
     * In order for the plugin to display its section correctly, a key in the
     * Plugin language file will need to contain
     * <TT>ConfigView.section.<i>&lt;configSectionGetName() result&gt;</i>=The Section name.</TT><br>
     *
     * @return The name of the configuration section
     */
    public String configSectionGetName() {
        return "transfer.autospeedbeta";
    }

    /**
     * User selected Save.
     * All saving of non-plugin tabs have been completed, as well as
     * saving of plugins that implement org.gudy.azureus2.plugins.ui.config
     * parameters.
     */
    public void configSectionSave() {
    }

    /**
     * Config view is closing
     */
    public void configSectionDelete() {
    }


    public Composite configSectionCreate(final Composite parent) {

        //ToDo: for new we are NOT going to internationalize this panel. Wait until the panel is in its final format.

        GridData gridData;

        Composite cSection = new Composite(parent, SWT.NULL);

        gridData = new GridData(GridData.VERTICAL_ALIGN_FILL|GridData.HORIZONTAL_ALIGN_FILL);
        cSection.setLayoutData(gridData);
        GridLayout subPanel = new GridLayout();
        subPanel.numColumns = 3;
        cSection.setLayout(subPanel);

        ///////////////////////////////////
        // AutoSpeed Beta mode group
        ///////////////////////////////////
        //Beta-mode grouping.
        Group modeGroup = new Group(cSection, SWT.NULL);
        //Messages.setLanguageText
        modeGroup.setText("AutoSpeed-Beta mode");
        GridLayout modeLayout = new GridLayout();
        modeLayout.numColumns = 3;
        modeGroup.setLayout(modeLayout);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        modeGroup.setLayoutData(gridData);



        //To enable the beta.
        gridData = new GridData();
        gridData.widthHint = 50;
        gridData.horizontalAlignment = GridData.END;
        enableV2AutoSpeedBeta = new BooleanParameter(modeGroup,SpeedManagerAlgorithmProviderV2.SETTING_V2_BETA_ENABLED);
        enableV2AutoSpeedBeta.setLayoutData(gridData);

        Label enableLabel = new Label(modeGroup, SWT.NULL);
        enableLabel.setText("Enable AutoSpeed Beta");
        gridData = new GridData();
        gridData.widthHint = 40;
        cSection.setLayoutData(gridData);


        //spacer
        Label enableSpacer = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=3;
        enableSpacer.setLayoutData(gridData);


        //Need a drop down to select which method will be used.
        Label label = new Label(modeGroup, SWT.NULL);
        label.setText("algorithm: ");
        gridData = new GridData();
        gridData.widthHint = 40;
        label.setLayoutData(gridData);

        //Set DHT as the default 
        String[] modeNames = {
                "SpeedSense - Vivaldi",
                "SpeedSense - DHT"
        };
        String[] modes = {
                SpeedManagerAlgorithmProviderV2.VALUE_SOURCE_VIVALDI,
                SpeedManagerAlgorithmProviderV2.VALUE_SOURCE_DHT
        };
        strategyList = new StringListParameter(modeGroup,
                SpeedManagerAlgorithmProviderV2.SETTING_DATA_SOURCE_INPUT,
                SpeedManagerAlgorithmProviderV2.VALUE_SOURCE_DHT,
                modeNames,modes,true);


        //ToDo: for now we put in just the Vivaldi settings, but this WILL change.

        //spacer
        Label spacer = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=3;
        spacer.setLayoutData(gridData);

        //label column for speed test results
        Label limits = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint=80;
        limits.setText("Speed Test Limits: ");
        //Messages.setLanguageText //ToDo: internationalize

        Label limMax = new Label(modeGroup,SWT.NULL);
        gridData = new GridData();
        limMax.setLayoutData(gridData);
        limMax.setText("max");
        //Messages.setLanguageText //ToDo: internationalize

        Label limMin = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        limMin.setLayoutData(gridData);
        limMin.setText("min");
        //Messages.setLanguageText //ToDo: internationalize


        //download settings
        Label setDown = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        setDown.setLayoutData(gridData);
        setDown.setText("Download: ");
        //Messages.setLanguageText //ToDo: internationalize

        gridData = new GridData();
        gridData.widthHint = 50;
        downMaxLim = new IntParameter(modeGroup,SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MAX_LIMIT);
        downMaxLim.setLayoutData( gridData );


        gridData = new GridData();
        gridData.widthHint = 50;
        downMinLim = new IntParameter(modeGroup,SpeedManagerAlgorithmProviderV2.SETTING_DOWNLOAD_MIN_LIMIT);
        downMinLim.setLayoutData( gridData );

        //upload settings
        Label setUp = new Label(modeGroup, SWT.NULL);
        gridData = new GridData();
        setUp.setLayoutData(gridData);
        setUp.setText("Upload: ");
        //Messages.setLanguageText //ToDo: internationalize


        gridData = new GridData();
        gridData.widthHint = 50;
        uploadMaxLim = new IntParameter(modeGroup, SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MAX_LIMIT);
        uploadMaxLim.setLayoutData( gridData );


        gridData = new GridData();
        gridData.widthHint = 50;
        uploadMinLim = new IntParameter(modeGroup, SpeedManagerAlgorithmProviderV2.SETTING_UPLOAD_MIN_LIMIT, 800, 5000);
        uploadMinLim.setLayoutData( gridData );

        //spacer
        spacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=3;
        spacer.setLayoutData(gridData);

        //////////////////////////
        //Vivaldi Median Distance Group
        //////////////////////////

        //Vivaldi grouping.
        Group vivaldiGroup = new Group(cSection, SWT.NULL);
        //Messages.setLanguageText
        vivaldiGroup.setText("Data: Vivaldi");
        GridLayout vivaldiLayout = new GridLayout();
        vivaldiLayout.numColumns = 3;
        vivaldiGroup.setLayout(subPanel);

        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 3;
        vivaldiGroup.setLayoutData(gridData);

        //label column for Vivaldi limits
        Label vivaldiSetting = new Label(vivaldiGroup, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint=80;
        vivaldiSetting.setText("Vivaldi Settings: ");
        //Messages.setLanguageText //ToDo: internationalize

        Label vSet = new Label(vivaldiGroup,SWT.NULL);
        gridData = new GridData();
        vSet.setLayoutData(gridData);
        vSet.setText("set point");
        //Messages.setLanguageText //ToDo: internationalize

        Label vTol = new Label(vivaldiGroup, SWT.NULL);
        gridData = new GridData();
        vTol.setLayoutData(gridData);
        vTol.setText("tolerance");
        //Messages.setLanguageText //ToDo: internationalize

        //good
        Label vGoodLbl = new Label(vivaldiGroup, SWT.NULL);
        gridData = new GridData();
        vGoodLbl.setLayoutData(gridData);
        vGoodLbl.setText("Good: ");
        //Messages.setLanguageText //ToDo: internationalize

        
        gridData = new GridData();
        gridData.widthHint = 50;
        vGood = new IntParameter(vivaldiGroup, SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_GOOD_SET_POINT);
        vGood.setLayoutData( gridData );


        //ToDo: calculate this limit as 10% of upper limit, or 5 kb/s which ever is greater.
        gridData = new GridData();
        gridData.widthHint = 50;
        vGoodTol = new IntParameter(vivaldiGroup, SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_GOOD_TOLERANCE);
        vGoodTol.setLayoutData( gridData );

        //bad
        Label vBadLbl = new Label(vivaldiGroup, SWT.NULL);
        gridData = new GridData();
        vBadLbl.setLayoutData(gridData);
        vBadLbl.setText("Bad: ");
        //Messages.setLanguageText //ToDo: internationalize


        gridData = new GridData();
        gridData.widthHint = 50;
        vBad = new IntParameter(vivaldiGroup, SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_BAD_SET_POINT);
        vBad.setLayoutData( gridData );

        
        //ToDo: calculate this limit as 10% of upper limit, or 5 kb/s which ever is greater.
        gridData = new GridData();
        gridData.widthHint = 50;
        vBadTol = new IntParameter(vivaldiGroup, SpeedManagerAlgorithmProviderV2.SETTING_VIVALDI_BAD_TOLERANCE);
        vBadTol.setLayoutData( gridData );

        //spacer
        spacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=3;
        spacer.setLayoutData(gridData);

        //////////////////////////
        //DHT Ping Group
        //////////////////////////

        Group dhtGroup = new Group(cSection, SWT.NULL);
        //Messages.setLanguageText
        dhtGroup.setText("Data: DHT Pings");
        GridLayout dhtLayout = new GridLayout();
        dhtLayout.numColumns = 3;
        //dhtGroup.setLayout(dhtLayout);
        dhtGroup.setLayout(subPanel);

        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 3;
        dhtGroup.setLayoutData(gridData);

        //label column for Vivaldi limits
        Label dhtSetting = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        gridData.widthHint=80;
        dhtSetting.setText("DHT Ping Settings: ");
        //Messages.setLanguageText //ToDo: internationalize

        Label dSet = new Label(dhtGroup,SWT.NULL);
        gridData = new GridData();
        dSet.setLayoutData(gridData);
        dSet.setText("set point");
        //Messages.setLanguageText //ToDo: internationalize

        Label dTol = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        dTol.setLayoutData(gridData);
        dTol.setText("tolerance");
        //Messages.setLanguageText //ToDo: internationalize

        //good
        Label dGoodLbl = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        dGoodLbl.setLayoutData(gridData);
        dGoodLbl.setText("Good: ");
        //Messages.setLanguageText //ToDo: internationalize


        gridData = new GridData();
        gridData.widthHint = 50;
        dGood = new IntParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_DHT_GOOD_SET_POINT);
        dGood.setLayoutData( gridData );


        //ToDo: calculate this limit as 10% of upper limit, or 5 kb/s which ever is greater.
        gridData = new GridData();
        gridData.widthHint = 50;
        dGoodTol = new IntParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_DHT_GOOD_TOLERANCE);
        dGoodTol.setLayoutData( gridData );

        //bad
        Label dBadLbl = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        dBadLbl.setLayoutData(gridData);
        dBadLbl.setText("Bad: ");
        //Messages.setLanguageText //ToDo: internationalize


        gridData = new GridData();
        gridData.widthHint = 50;
        dBad = new IntParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_DHT_BAD_SET_POINT);
        dBad.setLayoutData( gridData );


        //ToDo: calculate this limit as 10% of upper limit, or 5 kb/s which ever is greater.
        gridData = new GridData();
        gridData.widthHint = 50;
        dBadTol = new IntParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_DHT_BAD_TOLERANCE);
        dBadTol.setLayoutData( gridData );

        //spacer
        spacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=1;
        spacer.setLayoutData(gridData);

        //how much data to accumulate before making an adjustment.
        Label iCount = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=2;
        gridData.horizontalAlignment=GridData.BEGINNING;
        iCount.setLayoutData(gridData);
        iCount.setText("adjustment interval: ");

        adjustmentInterval = new IntParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_INTERVALS_BETWEEN_ADJUST);
        gridData = new GridData();
        gridData.widthHint = 50;
        adjustmentInterval.setLayoutData(gridData);

        //spacer
        spacer = new Label(cSection, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=1;
        spacer.setLayoutData(gridData);

        //how much data to accumulate before making an adjustment.
        Label skip = new Label(dhtGroup, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan=2;
        gridData.horizontalAlignment=GridData.BEGINNING;
        skip.setLayoutData(gridData);
        skip.setText("skip after adjustment: ");

        skipAfterAdjustment = new BooleanParameter(dhtGroup, SpeedManagerAlgorithmProviderV2.SETTING_WAIT_AFTER_ADJUST);
        gridData = new GridData();
        gridData.widthHint = 50;
        skipAfterAdjustment.setLayoutData(gridData);

        return cSection;
    }

}
