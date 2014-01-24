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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.proxy.AEProxyFactory;


public class ConfigSectionInterfaceDisplay implements UISWTConfigSection {
	private final static String MSG_PREFIX = "ConfigView.section.style.";

	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_INTERFACE;
	}

	public String configSectionGetName() {
		return "display";
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
	}
	
	public int maxUserMode() {
		return 2;
	}


	public Composite configSectionCreate(final Composite parent) {
    int userMode = COConfigurationManager.getIntParameter("User Mode");
		boolean isAZ3 = COConfigurationManager.getStringParameter("ui").equals("az3");

		Label label;
		GridLayout layout;
		GridData gridData;
		Composite cSection = new Composite(parent, SWT.NULL);
		cSection.setLayoutData(new GridData(GridData.FILL_BOTH));
		layout = new GridLayout();
		layout.numColumns = 1;
		cSection.setLayout(layout);

			// various stuff
		
		Group gVarious = new Group(cSection, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 1;
		gVarious.setLayout(layout);
		gVarious.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		gVarious.setText( MessageText.getString( "label.various" ));
		
		
		new BooleanParameter(gVarious, "Show Download Basket", MSG_PREFIX
				+ "showdownloadbasket");

		if (!isAZ3) {
			new BooleanParameter(gVarious, "IconBar.enabled", MSG_PREFIX + "showiconbar");
		}

		new BooleanParameter(gVarious, "Add URL Silently", MSG_PREFIX	+ "addurlsilently");

		//new BooleanParameter(gVarious, "suppress_file_download_dialog", "ConfigView.section.interface.display.suppress.file.download.dialog");

		new BooleanParameter(gVarious, "show_torrents_menu", "Menu.show.torrent.menu");

		if (Constants.isWindowsXP) {
			final Button enableXPStyle = new Button(gVarious, SWT.CHECK);
			Messages.setLanguageText(enableXPStyle, MSG_PREFIX + "enableXPStyle");

			boolean enabled = false;
			boolean valid = false;
			try {
				File f = new File(System.getProperty("java.home")
						+ "\\bin\\javaw.exe.manifest");
				if (f.exists()) {
					enabled = true;
				}
				f = FileUtil.getApplicationFile("javaw.exe.manifest");
				if (f.exists()) {
					valid = true;
				}
			} catch (Exception e) {
				Debug.printStackTrace(e);
				valid = false;
			}
			enableXPStyle.setEnabled(valid);
			enableXPStyle.setSelection(enabled);
			enableXPStyle.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event arg0) {
					//In case we enable the XP Style
					if (enableXPStyle.getSelection()) {
						try {
							File fDest = new File(System.getProperty("java.home")
									+ "\\bin\\javaw.exe.manifest");
							File fOrigin = new File("javaw.exe.manifest");
							if (!fDest.exists() && fOrigin.exists()) {
								FileUtil.copyFile(fOrigin, fDest);
							}
						} catch (Exception e) {
							Debug.printStackTrace(e);
						}
					} else {
						try {
							File fDest = new File(System.getProperty("java.home")
									+ "\\bin\\javaw.exe.manifest");
							fDest.delete();
						} catch (Exception e) {
							Debug.printStackTrace(e);
						}
					}
				}
			});
		}

		if (Constants.isOSX) {
			new BooleanParameter(gVarious, "enable_small_osx_fonts", MSG_PREFIX	+ "osx_small_fonts");
		}
		
		// Reuse the labels of the other menu actions.
		if (PlatformManagerFactory.getPlatformManager().hasCapability(PlatformManagerCapabilities.ShowFileInBrowser)) {
			BooleanParameter bp = new BooleanParameter(gVarious, "MyTorrentsView.menu.show_parent_folder_enabled", MSG_PREFIX
					+ "use_show_parent_folder");
			Messages.setLanguageText(bp.getControl(), "ConfigView.section.style.use_show_parent_folder", new String[] {
				MessageText.getString("MyTorrentsView.menu.open_parent_folder"),
				MessageText.getString("MyTorrentsView.menu.explore"),
			});
			
			if (Constants.isOSX) {
				new BooleanParameter(gVarious, "FileBrowse.usePathFinder", 
						MSG_PREFIX + "usePathFinder");
			}
		}
		
		if ( Constants.isOSX_10_5_OrHigher ){
			
			Composite cSWT = new Composite(gVarious, SWT.NULL);
			layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			layout.numColumns = 2;
			cSWT.setLayout(layout);
			cSWT.setLayoutData(new GridData());
			
			label = new Label(cSWT, SWT.NULL);
			label.setText( "SWT Library" );
			String[] swtLibraries = { "carbon", "cocoa" };
					
			new StringListParameter(cSWT, MSG_PREFIX + "swt.library.selection", swtLibraries, swtLibraries);
		}
		
			// sidebar
		
		if ( isAZ3 ){
		
			Group gSideBar = new Group(cSection, SWT.NULL);
			Messages.setLanguageText(gSideBar, "v3.MainWindow.menu.view.sidebar" );
			layout = new GridLayout();
			layout.numColumns = 2;
			gSideBar.setLayout(layout);
			gSideBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
			new BooleanParameter(gSideBar, "Show Side Bar", "sidebar.show");
			label = new Label(gSideBar, SWT.NULL);
			
			label = new Label(gSideBar, SWT.NULL);
			Messages.setLanguageText(label, "sidebar.top.level.gap" );
			
			new IntParameter(gSideBar, "Side Bar Top Level Gap", 0, 5 );
			
			new BooleanParameter(gSideBar, "Show Options In Side Bar", "sidebar.show.options");
			label = new Label(gSideBar, SWT.NULL);

		}
		
			// status bar
		
		Group cStatusBar = new Group(cSection, SWT.NULL);
		Messages.setLanguageText(cStatusBar, MSG_PREFIX + "status");
		layout = new GridLayout();
		layout.numColumns = 1;
		cStatusBar.setLayout(layout);
		cStatusBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		new BooleanParameter(cStatusBar, "Status Area Show SR", MSG_PREFIX	+ "status.show_sr");
		new BooleanParameter(cStatusBar, "Status Area Show NAT",  MSG_PREFIX + "status.show_nat");
		new BooleanParameter(cStatusBar, "Status Area Show DDB", MSG_PREFIX + "status.show_ddb");
		new BooleanParameter(cStatusBar, "Status Area Show IPF", MSG_PREFIX + "status.show_ipf");
		new BooleanParameter(cStatusBar, "status.rategraphs", MSG_PREFIX + "status.show_rategraphs");
	
			// display units

		if (userMode > 0) {
			Group cUnits = new Group(cSection, SWT.NULL);
			Messages.setLanguageText(cUnits, MSG_PREFIX + "units");
			layout = new GridLayout();
			layout.numColumns = 1;
			cUnits.setLayout(layout);
			cUnits.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			new BooleanParameter(cUnits, "config.style.useSIUnits", MSG_PREFIX
					+ "useSIUnits");

			new BooleanParameter(cUnits, "config.style.forceSIValues", MSG_PREFIX
					+ "forceSIValues");

			new BooleanParameter(cUnits, "config.style.useUnitsRateBits", MSG_PREFIX
					+ "useUnitsRateBits");

			new BooleanParameter(cUnits, "config.style.doNotUseGB", MSG_PREFIX
					+ "doNotUseGB");

			new BooleanParameter(cUnits, "config.style.dataStatsOnly", MSG_PREFIX
					+ "dataStatsOnly");

			new BooleanParameter(cUnits, "config.style.separateProtDataStats",
					MSG_PREFIX + "separateProtDataStats");
		}
		
			// external browser
			
		if( userMode > 0 ) {
			Group gExternalBrowser = new Group(cSection, SWT.NULL);
			layout = new GridLayout();
			layout.numColumns = 1;
			gExternalBrowser.setLayout(layout);
			gExternalBrowser.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
			gExternalBrowser.setText( MessageText.getString( "config.external.browser" ));
			
			label = new Label(gExternalBrowser, SWT.NULL);
			Messages.setLanguageText(label, "config.external.browser.info1");
			label = new Label(gExternalBrowser, SWT.NULL);
			Messages.setLanguageText(label, "config.external.browser.info2");
			
				// browser selection

			final java.util.List<String[]> browser_choices = new ArrayList<String[]>(); 
				
			browser_choices.add( 
					new String[]{ "system",  MessageText.getString( "external.browser.system" ) });
			browser_choices.add( 
					new String[]{ "manual",  MessageText.getString( "external.browser.manual" ) });
			
			java.util.List<PluginInterface> pis = 
					AzureusCoreFactory.getSingleton().getPluginManager().getPluginsWithMethod(
						"launchURL", 
						new Class[]{ URL.class, boolean.class, Runnable.class });
			
			for ( PluginInterface pi: pis ){
				
				browser_choices.add( 
						new String[]{ "plugin:" + pi.getPluginID(),  pi.getPluginName() });
				
			}
			final Composite cEBArea = new Composite(gExternalBrowser, SWT.WRAP);
			gridData = new GridData( GridData.FILL_HORIZONTAL);
			cEBArea.setLayoutData(gridData);
			layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginHeight = 0;
			cEBArea.setLayout(layout);
			
			label = new Label(cEBArea, SWT.NULL);
			Messages.setLanguageText(label, "config.external.browser.select");

			final Composite cEB = new Group(cEBArea, SWT.WRAP);
			gridData = new GridData( GridData.FILL_HORIZONTAL);
			cEB.setLayoutData(gridData);
			layout = new GridLayout();
			layout.numColumns = browser_choices.size();
			layout.marginHeight = 0;
			cEB.setLayout(layout);

			java.util.List<Button> buttons = new ArrayList<Button>();
			
			for ( int i=0;i< browser_choices.size(); i++ ){
				Button button = new Button ( cEB, SWT.RADIO );
				button.setText( browser_choices.get(i)[1] );
				button.setData("index", String.valueOf(i));
			
				buttons.add( button );
			}
					
			String existing = COConfigurationManager.getStringParameter( "browser.external.id", browser_choices.get(0)[0] );
			
			int existing_index = -1;
			
			for ( int i=0; i<browser_choices.size();i++){
				
				if ( browser_choices.get(i)[0].equals( existing )){
					
					existing_index = i;
							
					break;
				}
			}
			
			if ( existing_index == -1 ){
				
				existing_index = 0;
				
				COConfigurationManager.setParameter( "browser.external.id", browser_choices.get(0)[0] );
			}
			
			buttons.get(existing_index).setSelection( true );
			
			Messages.setLanguageText(new Label(cEBArea,SWT.NONE), "config.external.browser.prog" );
			
			Composite manualArea = new Composite(cEBArea,SWT.NULL);
			layout = new GridLayout(2,false);
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			manualArea.setLayout( layout);
			manualArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			final Parameter manualProg = new FileParameter(manualArea, "browser.external.prog","", new String[]{});

			manualProg.setEnabled( existing_index == 1 );
			
		    Listener radioListener = 
			    	new Listener () 
			    	{
			    		public void 
			    		handleEvent(
			    			Event event ) 
			    		{	
						    Button button = (Button)event.widget;

						    if ( button.getSelection()){
					    		Control [] children = cEB.getChildren ();
					    		
					    		for (int j=0; j<children.length; j++) {
					    			 Control child = children [j];
					    			 if ( child != button && child instanceof Button) {
					    				 Button b = (Button) child;
					    				
					    				 b.setSelection (false);
					    			 }
					    		}
								    
							    int index = Integer.parseInt((String)button.getData("index"));
						    
							    COConfigurationManager.setParameter( "browser.external.id", browser_choices.get(index)[0] );
							    
							    manualProg.setEnabled( index == 1 );
						    }
					    }
			    	};
			
			for ( Button b: buttons ){
				
				b.addListener( SWT.Selection, radioListener );
			}
			
				// test launch
			
			Composite testArea = new Composite(gExternalBrowser,SWT.NULL);
			layout = new GridLayout(3,false);
			layout.marginHeight = 0;
			testArea.setLayout(layout);
			testArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			label = new Label(testArea, SWT.NULL);
			Messages.setLanguageText(label, "config.external.browser.test");

		    final Button test_button = new Button(testArea, SWT.PUSH);
		    
		    Messages.setLanguageText(test_button, "configureWizard.nat.test");

		    test_button.addListener(SWT.Selection, 
		    		new Listener() 
					{
				        public void 
						handleEvent(Event event) 
				        {
				        	test_button.setEnabled( false );
				        	
				        	new AEThread2( "async" )
				        	{
				        		public void
				        		run()
				        		{
				        			try{
				        				Utils.launch( "http://www.vuze.com/", true );
				        				
				        			}finally{
				        				
				        				Utils.execSWTThread(
				        					new Runnable()
				        					{
				        						public void
				        						run()
				        						{
				        							if (! test_button.isDisposed()){
				        				
				        								test_button.setEnabled( true );
				        							}
				        						}
				        					});
				        			}
				        		}
				        	}.start();
				        }
				    });
			
			label = new Label(testArea, SWT.NULL);
			label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

				// switch internal->external
			
			label = new Label(gExternalBrowser, SWT.NULL);
			Messages.setLanguageText(label, "config.external.browser.switch.info");

			Group switchArea = new Group(gExternalBrowser,SWT.NULL);
			layout = new GridLayout(3,false);
			//layout.marginHeight = 0;
			//layout.marginWidth = 0;
			switchArea.setLayout(layout);
			switchArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

				// header
			
			label = new Label(switchArea, SWT.NULL);
			Messages.setLanguageText(label, "config.external.browser.switch.feature");
			label = new Label(switchArea, SWT.NULL);
			Messages.setLanguageText(label, "config.external.browser.switch.external");
			label = new Label(switchArea, SWT.NULL);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalIndent = 10;
			label.setLayoutData(gridData);
			Messages.setLanguageText(label, "config.external.browser.switch.implic");

				// search 
			
			label = new Label(switchArea, SWT.NULL);
			gridData = new GridData();
			gridData.verticalIndent = 10;
			label.setLayoutData(gridData);
			Messages.setLanguageText(label, "config.external.browser.switch.search");
			
			BooleanParameter switchSearch = new BooleanParameter(switchArea, "browser.external.search" );
			gridData = new GridData();
			gridData.verticalIndent = 10;
			gridData.horizontalAlignment = SWT.CENTER;
			switchSearch.setLayoutData(gridData);
			
			label = new Label(switchArea, SWT.NULL);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.verticalIndent = 10;
			gridData.horizontalIndent = 10;
			label.setLayoutData(gridData);
			Messages.setLanguageText(label, "config.external.browser.switch.search.inf");

				// subscriptions
			
			label = new Label(switchArea, SWT.NULL);
			gridData = new GridData();
			label.setLayoutData(gridData);
			Messages.setLanguageText(label, "config.external.browser.switch.subs");
			
			BooleanParameter switchSubs = new BooleanParameter(switchArea, "browser.external.subs" );
			gridData = new GridData();
			gridData.horizontalAlignment = SWT.CENTER;
			switchSubs.setLayoutData(gridData);
			
			label = new Label(switchArea, SWT.NULL);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			gridData.horizontalIndent = 10;
			label.setLayoutData(gridData);
			Messages.setLanguageText(label, "config.external.browser.switch.subs.inf");
		}
		
			// internal browser
		
		if( userMode > 1 ) {
			Group gInternalBrowser = new Group(cSection, SWT.NULL);
			layout = new GridLayout();
			layout.numColumns = 1;
			gInternalBrowser.setLayout(layout);
			gInternalBrowser.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
			gInternalBrowser.setText( MessageText.getString( "config.internal.browser" ));
			
			label = new Label(gInternalBrowser, SWT.NULL);
			Messages.setLanguageText(label, "config.internal.browser.info1");

			label = new Label(gInternalBrowser, SWT.NULL);
			Messages.setLanguageText(label, "config.internal.browser.info3");
			
			java.util.List<PluginInterface> pis = AEProxyFactory.getPluginHTTPProxyProviders( true ); 
					
			final java.util.List<String[]> proxy_choices = new ArrayList<String[]>(); 

			proxy_choices.add( 
					new String[]{ "none",  MessageText.getString( "PeersView.uniquepiece.none" ) });

			for ( PluginInterface pi: pis ){
				
				proxy_choices.add( 
						new String[]{ "plugin:" + pi.getPluginID(),  pi.getPluginName() });
				
			}
			
			final Composite cIPArea = new Composite(gInternalBrowser, SWT.WRAP);
			gridData = new GridData( GridData.FILL_HORIZONTAL);
			cIPArea.setLayoutData(gridData);
			layout = new GridLayout();
			layout.numColumns = 2;
			layout.marginHeight = 0;
			cIPArea.setLayout(layout);
			
			label = new Label(cIPArea, SWT.NULL);
			Messages.setLanguageText(label, "config.internal.browser.proxy.select");

			final Composite cIP = new Group(cIPArea, SWT.WRAP);
			gridData = new GridData( GridData.FILL_HORIZONTAL);
			cIP.setLayoutData(gridData);
			layout = new GridLayout();
			layout.numColumns = proxy_choices.size();
			layout.marginHeight = 0;
			cIP.setLayout(layout);

			java.util.List<Button> buttons = new ArrayList<Button>();
			
			for ( int i=0;i< proxy_choices.size(); i++ ){
				Button button = new Button ( cIP, SWT.RADIO );
				button.setText( proxy_choices.get(i)[1] );
				button.setData("index", String.valueOf(i));
			
				buttons.add( button );
			}
					
			String existing = COConfigurationManager.getStringParameter( "browser.internal.proxy.id", proxy_choices.get(0)[0] );
			
			int existing_index = -1;
			
			for ( int i=0; i<proxy_choices.size();i++){
				
				if ( proxy_choices.get(i)[0].equals( existing )){
					
					existing_index = i;
							
					break;
				}
			}
			
			if ( existing_index == -1 ){
				
				existing_index = 0;
				
				COConfigurationManager.setParameter( "browser.internal.proxy.id", proxy_choices.get(0)[0] );
			}
			
			buttons.get(existing_index).setSelection( true );
			
						
		    Listener radioListener = 
			    	new Listener () 
			    	{
			    		public void 
			    		handleEvent(
			    			Event event ) 
			    		{	
						    Button button = (Button)event.widget;

						    if ( button.getSelection()){
					    		Control [] children = cIP.getChildren ();
					    		
					    		for (int j=0; j<children.length; j++) {
					    			 Control child = children [j];
					    			 if ( child != button && child instanceof Button) {
					    				 Button b = (Button) child;
					    				
					    				 b.setSelection (false);
					    			 }
					    		}
								    
							    int index = Integer.parseInt((String)button.getData("index"));
						    
							    COConfigurationManager.setParameter( "browser.internal.proxy.id", proxy_choices.get(index)[0] );
						    }
					    }
			    	};
			
			for ( Button b: buttons ){
				
				b.addListener( SWT.Selection, radioListener );
			}			
			
				// force firefox
			
			label = new Label(gInternalBrowser, SWT.NULL);
			Messages.setLanguageText(label, "config.internal.browser.info2");

			
			final BooleanParameter fMoz = new BooleanParameter(gInternalBrowser, "swt.forceMozilla",MSG_PREFIX + "forceMozilla");
			Composite pArea = new Composite(gInternalBrowser,SWT.NULL);
			pArea.setLayout(new GridLayout(3,false));
			pArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			Messages.setLanguageText(new Label(pArea,SWT.NONE), MSG_PREFIX+"xulRunnerPath");
			final Parameter xulDir = new DirectoryParameter(pArea, "swt.xulRunner.path","");
			fMoz.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(xulDir.getControls(), false));
		}
		
			// refresh
		
		Group gRefresh = new Group(cSection, SWT.NULL);
		gRefresh.setText( MessageText.getString( "upnp.refresh.button" ));
		
		layout = new GridLayout();
		layout.numColumns = 2;
		gRefresh.setLayout(layout);
		gRefresh.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		label = new Label(gRefresh, SWT.NULL);
		Messages.setLanguageText(label, MSG_PREFIX + "guiUpdate");
		int[] values = { 100, 250, 500, 1000, 2000, 5000, 10000, 15000 };
		String[] labels = { "100 ms", "250 ms", "500 ms", "1 s", "2 s", "5 s", "10 s", "15 s" };
		new IntListParameter(gRefresh, "GUI Refresh", 1000, labels, values);
		
		label = new Label(gRefresh, SWT.NULL);
		Messages.setLanguageText(label, MSG_PREFIX + "inactiveUpdate");
		gridData = new GridData();
		IntParameter inactiveUpdate = new IntParameter(gRefresh, "Refresh When Inactive", 1,	-1);
		inactiveUpdate.setLayoutData(gridData);

		label = new Label(gRefresh, SWT.NULL);
		Messages.setLanguageText(label, MSG_PREFIX + "graphicsUpdate");
		gridData = new GridData();
		IntParameter graphicUpdate = new IntParameter(gRefresh, "Graphics Update", 1,	-1);
		graphicUpdate.setLayoutData(gridData);
		
		return cSection;
	}
}
