/*
 * File    : ConfigPanel*.java
 * Created : 11 mar. 2004
 * By      : TuxPaper
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.views.configsections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;

import org.gudy.azureus2.plugins.ui.config.ConfigSection;

import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

public class ConfigSectionInterfaceStart implements UISWTConfigSection {
  public String configSectionGetParentSection() {
    return ConfigSection.SECTION_INTERFACE;
  }

	public String configSectionGetName() {
		return "start";
	}

  public void configSectionSave() {
  }

  public void configSectionDelete() {
  }
  

  public Composite configSectionCreate(final Composite parent) {
    // "Start" Sub-Section
    // -------------------
    GridLayout layout;
    Composite cStart = new Composite(parent, SWT.NULL);

    cStart.setLayoutData(new GridData(GridData.FILL_BOTH));
    layout = new GridLayout();
    layout.numColumns = 1;
    cStart.setLayout(layout);

    new BooleanParameter(cStart, "Show Splash", true, "ConfigView.label.showsplash");
    //new BooleanParameter(cStart, "Auto Update", true, "ConfigView.label.autoupdate");
    new BooleanParameter(cStart, "update.start", true, "ConfigView.label.checkonstart");
    new BooleanParameter(cStart, "update.periodic", true, "ConfigView.label.periodiccheck");
    new BooleanParameter(cStart, "update.opendialog", true, "ConfigView.label.opendialog");
    new Label(cStart,SWT.NULL);
    new BooleanParameter(cStart, "Open MyTorrents", "ConfigView.label.openmytorrents");
    new BooleanParameter(cStart, "Open Console", false, "ConfigView.label.openconsole");
    new BooleanParameter(cStart, "Open Stats On Start", false, "ConfigView.label.openstatsonstart");
    new BooleanParameter(cStart, "Open Config", false, "ConfigView.label.openconfig");
    new BooleanParameter(cStart, "Start Minimized", false, "ConfigView.label.startminimized");
    
    if (Constants.compareVersions(Constants.AZUREUS_VERSION, "3.0.0.0") >= 0) {
			new BooleanParameter(cStart, "v3.Start Advanced",
					"ConfigView.interface.start.advanced");
		}
    
    return cStart;
  }
}
