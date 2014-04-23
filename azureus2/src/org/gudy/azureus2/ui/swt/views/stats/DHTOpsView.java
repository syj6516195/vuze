/*
 * Created on 22 juin 2005
 * Created by Olivier Chalouhi
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
package org.gudy.azureus2.ui.swt.views.stats;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.plugins.dht.DHTPlugin;

public class DHTOpsView 
	implements UISWTViewEventListener
{
	public static final int DHT_TYPE_MAIN   = DHT.NW_MAIN;

	public static final String MSGID_PREFIX = "DHTOpsView";

	DHT dht;
	Composite panel;
	DHTOpsPanel drawPanel;
	private final boolean autoAlpha;
	private final boolean autoDHT;
	
	private int dht_type;
	private AzureusCore core;
	private UISWTView swtView;

	public DHTOpsView() {
		this(false);
	}

	public DHTOpsView(boolean autoAlpha) {
		this( autoAlpha, true );
	}

	public DHTOpsView(boolean autoAlpha, boolean autoDHT ) {
		this.autoAlpha = autoAlpha;
		this.autoDHT	= autoDHT;
	}

	private void init(AzureusCore core) {
		try {
			PluginInterface dht_pi = core.getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );

			if ( dht_pi == null ){

				if ( drawPanel != null ){
					
					drawPanel.setUnavailable();
				}
				
				return;
			}

			DHTPlugin dht_plugin = (DHTPlugin)dht_pi.getPlugin();
			
			DHT[] dhts = dht_plugin.getDHTs();

			for (int i=0;i<dhts.length;i++){
				if ( dhts[i].getTransport().getNetwork() == dht_type ){
					dht = dhts[i];
					break;
				}
			}
			
			if ( drawPanel != null ){
				
				if ( 	dht == null &&
						!dht_plugin.isInitialising()){
					
					drawPanel.setUnavailable();
				}
			}

			if ( dht == null ){
				return;
			}

		} catch(Exception e) {
			Debug.printStackTrace( e );
		}
	}

	public void
	setDHT(
		DHT		_dht )
	{
		dht	= _dht;
	}
	
	public void initialize(Composite composite) {
		if ( autoDHT ){
			AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
	
				public void azureusCoreRunning(AzureusCore core) {
					DHTOpsView.this.core = core;
					init(core);
				}
			});
		}
		
		panel = new Composite(composite,SWT.NULL);
		panel.setLayout(new FillLayout());    
		drawPanel = new DHTOpsPanel(panel);    
		drawPanel.setAutoAlpha(autoAlpha);
	}

	private Composite getComposite() {
		return panel;
	}

	private void refresh() {
		if (dht == null) {
			if (core != null) {
				// keep trying until dht is avail
				init(core);
			} else {
				return;
			}
		}

		if (dht != null) {
			drawPanel.refreshView( dht );
		}
	}

	private String 
	getTitleID() 
	{
		return( MSGID_PREFIX + ".title.full" );
	}

	private 
	void delete() 
	{	
		if (drawPanel != null) {
			drawPanel.delete();
		}
	}

	public boolean eventOccurred(UISWTViewEvent event) {
		switch (event.getType()) {
		case UISWTViewEvent.TYPE_CREATE:
			swtView = (UISWTView)event.getData();
			swtView.setTitle(MessageText.getString(getTitleID()));
			break;

		case UISWTViewEvent.TYPE_DESTROY:
			delete();
			break;

		case UISWTViewEvent.TYPE_INITIALIZE:
			initialize((Composite)event.getData());
			break;

		case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
			Messages.updateLanguageForControl(getComposite());
			if (swtView != null) {
				swtView.setTitle(MessageText.getString(getTitleID()));
			}
			break;

		case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
			if (event.getData() instanceof Number) {
				dht_type = ((Number) event.getData()).intValue();
				if (swtView != null) {
					swtView.setTitle(MessageText.getString(getTitleID()));
				}
			}
			break;

		case UISWTViewEvent.TYPE_FOCUSGAINED:
			break;

		case UISWTViewEvent.TYPE_REFRESH:
			refresh();
			break;
		}

		return true;
	}
}
