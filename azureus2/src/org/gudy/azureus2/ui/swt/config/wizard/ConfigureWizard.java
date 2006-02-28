/*
 * File    : ConfigureWizard.java
 * Created : 12 oct. 2003 16:06:44
 * By      : Olivier 
 * 
 * Azureus - a Java Bittorrent client
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
 */
 
package org.gudy.azureus2.ui.swt.config.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;

import com.aelitis.azureus.core.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.Wizard;

/**
 * @author Olivier
 * 
 */
public class ConfigureWizard extends Wizard {

  //Transfer settings
  int upSpeed = 4;
  int maxUpSpeed = 40;
  int maxActiveTorrents = 7;
  int maxDownloads = 5;
  int nbUploadsPerTorrent = 4;
  
  //Server / NAT Settings
  //int	  serverMinPort = 6881;
  //int 	  serverMaxPort = 6889;
  int serverTCPListenPort = COConfigurationManager.getIntParameter( "TCP.Listen.Port" );
  //boolean serverSharePort = true;
  //Files / Torrents
  String torrentPath;
  boolean fastResume = true;
  
  boolean completed = false;
 

  public 
  ConfigureWizard(
  	AzureusCore		azureus_core,
	Display 		display) 
  {
    super(azureus_core,display,"configureWizard.title");
    IWizardPanel panel = new LanguagePanel(this,null);
    this.setFirstPanel(panel);
    try  {
      torrentPath = COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory");
    } catch(Exception e) {
      torrentPath = ""; 
    }
  }
  
  public void onClose() {
  	
    if(!completed && !COConfigurationManager.getBooleanParameter("Wizard Completed",false)) {
      MessageBox mb = new MessageBox(this.getWizardWindow(),SWT.ICON_QUESTION | SWT.YES | SWT.NO);
      mb.setText(MessageText.getString("wizard.close.confirmation"));
      mb.setMessage(MessageText.getString("wizard.close.message"));
      int result = mb.open();
      if(result == SWT.NO) {
        COConfigurationManager.setParameter("Wizard Completed",true);
        COConfigurationManager.save();
      }         
    }
    
    super.onClose();
  }
}
