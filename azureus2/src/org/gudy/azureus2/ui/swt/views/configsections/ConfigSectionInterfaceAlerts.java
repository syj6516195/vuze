/*
 * File    : ConfigSectionInterfaceAlerts.java
 * Created : Dec 4, 2006
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

import java.applet.Applet;
import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.*;
import org.gudy.azureus2.ui.swt.plugins.UISWTConfigSection;

import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;

public class ConfigSectionInterfaceAlerts
	implements UISWTConfigSection
{
	private final static String INTERFACE_PREFIX = "ConfigView.section.interface.";

	private final static String LBLKEY_PREFIX = "ConfigView.label.";

	private final static String STYLE_PREFIX = "ConfigView.section.style.";

	private final static int REQUIRED_MODE = 0;

	public String configSectionGetParentSection() {
		return ConfigSection.SECTION_INTERFACE;
	}

	/* Name of section will be pulled from 
	 * ConfigView.section.<i>configSectionGetName()</i>
	 */
	public String configSectionGetName() {
		return "interface.alerts";
	}

	public void configSectionSave() {
	}

	public void configSectionDelete() {
		ImageLoader imageLoader = ImageLoader.getInstance();
		imageLoader.releaseImage("openFolderButton");
	}

	public int maxUserMode() {
		return REQUIRED_MODE;
	}

	public Composite configSectionCreate(final Composite parent) {
		Image imgOpenFolder = null;
		ImageLoader imageLoader = ImageLoader.getInstance();
		imgOpenFolder = imageLoader.getImage("openFolderButton");

		GridData gridData;
		GridLayout layout;

		Composite cSection = new Composite(parent, SWT.NULL);
		gridData = new GridData(GridData.VERTICAL_ALIGN_FILL
				| GridData.HORIZONTAL_ALIGN_FILL);
		cSection.setLayoutData(gridData);
		layout = new GridLayout();
		layout.marginWidth = 0;
		//layout.numColumns = 2;
		cSection.setLayout(layout);

		Composite cArea = new Composite(cSection, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 4;
		cArea.setLayout(layout);
		cArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// OS X counterpart for alerts (see below for what is disabled)
		if (Constants.isOSX) {
			// download info 

			new BooleanParameter(
					cArea, "Play Download Finished Announcement", LBLKEY_PREFIX
							+ "playdownloadspeech");

			final StringParameter d_speechParameter = new StringParameter(cArea,
					"Play Download Finished Announcement Text");
			gridData = new GridData();
			gridData.horizontalSpan = 3;
			gridData.widthHint = 150;
			d_speechParameter.setLayoutData(gridData);
			((Text) d_speechParameter.getControl()).setTextLimit(40);

			/* we support per-download speech now so leave sound selection always available
			d_speechEnabledParameter.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
					d_speechParameter.getControls()));
			*/
		}

		new BooleanParameter(cArea,
				"Play Download Finished", LBLKEY_PREFIX + "playdownloadfinished");

		// download info

		gridData = new GridData(GridData.FILL_HORIZONTAL);

		final StringParameter d_pathParameter = new StringParameter(cArea,
				"Play Download Finished File", "");

		if (d_pathParameter.getValue().length() == 0) {

			d_pathParameter.setValue("<default>");
		}

		d_pathParameter.setLayoutData(gridData);

		Button d_browse = new Button(cArea, SWT.PUSH);

		d_browse.setImage(imgOpenFolder);

		imgOpenFolder.setBackground(d_browse.getBackground());

		d_browse.setToolTipText(MessageText.getString("ConfigView.button.browse"));

		d_browse.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				FileDialog dialog = new FileDialog(parent.getShell(),
						SWT.APPLICATION_MODAL);
				dialog.setFilterExtensions(new String[] {
					"*.wav"
				});
				dialog.setFilterNames(new String[] {
					"*.wav"
				});

				dialog.setText(MessageText.getString(INTERFACE_PREFIX + "wavlocation"));

				final String path = dialog.open();

				if (path != null) {

					d_pathParameter.setValue(path);

					new AEThread2("SoundTest") {
						public void run() {
							try {
								Applet.newAudioClip(new File(path).toURI().toURL()).play();

								Thread.sleep(2500);

							} catch (Throwable e) {

							}
						}
					}.start();
				}
			}
		});

		Label d_sound_info = new Label(cArea, SWT.WRAP);
		Messages.setLanguageText(d_sound_info, INTERFACE_PREFIX
				+ "wavlocation.info");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.widthHint = 100;
		d_sound_info.setLayoutData(gridData);

		/* we support per-download alerts now so leave sound selection always available
		d_play_sound.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
				d_pathParameter.getControls()));
		d_play_sound.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
				new Control[] {
					d_browse,
					d_sound_info
				}));
		*/
		// 

		// OS X counterpart for alerts
		if (Constants.isOSX) {

			// per-file info

			final BooleanParameter f_speechEnabledParameter = new BooleanParameter(
					cArea, "Play File Finished Announcement", LBLKEY_PREFIX
							+ "playfilespeech");

			final StringParameter f_speechParameter = new StringParameter(cArea,
					"Play File Finished Announcement Text");
			gridData = new GridData();
			gridData.horizontalSpan = 3;
			gridData.widthHint = 150;
			f_speechParameter.setLayoutData(gridData);
			((Text) f_speechParameter.getControl()).setTextLimit(40);

			/* we support per-file alerts now so leave speech selection always available
			f_speechEnabledParameter.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
					f_speechParameter.getControls()));
			*/
		}

		BooleanParameter f_play_sound = new BooleanParameter(cArea,
				"Play File Finished", LBLKEY_PREFIX + "playfilefinished");

		// file info

		gridData = new GridData(GridData.FILL_HORIZONTAL);

		final StringParameter f_pathParameter = new StringParameter(cArea,
				"Play File Finished File", "");

		if (f_pathParameter.getValue().length() == 0) {

			f_pathParameter.setValue("<default>");
		}

		f_pathParameter.setLayoutData(gridData);

		Button f_browse = new Button(cArea, SWT.PUSH);

		f_browse.setImage(imgOpenFolder);

		imgOpenFolder.setBackground(f_browse.getBackground());

		f_browse.setToolTipText(MessageText.getString("ConfigView.button.browse"));

		f_browse.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				FileDialog dialog = new FileDialog(parent.getShell(),
						SWT.APPLICATION_MODAL);
				dialog.setFilterExtensions(new String[] {
					"*.wav"
				});
				dialog.setFilterNames(new String[] {
					"*.wav"
				});

				dialog.setText(MessageText.getString(INTERFACE_PREFIX + "wavlocation"));

				final String path = dialog.open();

				if (path != null) {

					f_pathParameter.setValue(path);

					new AEThread2("SoundTest") {
						public void run() {
							try {
								Applet.newAudioClip(new File(path).toURI().toURL()).play();

								Thread.sleep(2500);

							} catch (Throwable e) {

							}
						}
					}.start();
				}
			}
		});

		Label f_sound_info = new Label(cArea, SWT.WRAP);
		Messages.setLanguageText(f_sound_info, INTERFACE_PREFIX
				+ "wavlocation.info");
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.widthHint = 100;
		f_sound_info.setLayoutData(gridData);

		/* we support per-file alerts now so leave sound selection always available
		f_play_sound.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
				f_pathParameter.getControls()));
		f_play_sound.setAdditionalActionPerformer(new ChangeSelectionActionPerformer(
				new Control[] {
					f_browse,
					f_sound_info
				}));
		*/
		
		boolean isAZ3 = COConfigurationManager.getStringParameter("ui").equals("az3");
		
		if ( isAZ3 ){
			
			BooleanParameter p = new BooleanParameter(cArea,
					"Request Attention On New Download", LBLKEY_PREFIX + "dl.add.req.attention");
			gridData = new GridData();
			gridData.horizontalSpan = 3;
			p.setLayoutData(gridData);
		}
		
		BooleanParameter activate_win = new BooleanParameter(cArea,
				"Activate Window On External Download", LBLKEY_PREFIX + "show.win.on.add");
		gridData = new GridData();
		gridData.horizontalSpan = 3;
		activate_win.setLayoutData(gridData);
		
			// popups group
		
		Group gPopup = new Group(cSection, SWT.NULL);
		Messages.setLanguageText( gPopup, "label.popups" );
		layout = new GridLayout();
		layout.numColumns = 2;
		gPopup.setLayout(layout);
		gPopup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		BooleanParameter popup_dl_added = new BooleanParameter(gPopup,
				"Popup Download Added", LBLKEY_PREFIX + "popupdownloadadded");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		popup_dl_added.setLayoutData(gridData);

		BooleanParameter popup_dl_completed = new BooleanParameter(gPopup,
				"Popup Download Finished", LBLKEY_PREFIX + "popupdownloadfinished");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		popup_dl_completed.setLayoutData(gridData);

		BooleanParameter popup_file_completed = new BooleanParameter(gPopup,
				"Popup File Finished", LBLKEY_PREFIX + "popupfilefinished");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		popup_file_completed.setLayoutData(gridData);

		BooleanParameter disable_sliding = new BooleanParameter(gPopup,
				"GUI_SWT_DisableAlertSliding", STYLE_PREFIX + "disableAlertSliding");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		disable_sliding.setLayoutData(gridData);

		// Timestamps for popup alerts.
		BooleanParameter show_alert_timestamps = new BooleanParameter(gPopup,
				"Show Timestamp For Alerts", LBLKEY_PREFIX + "popup.timestamp");
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		show_alert_timestamps.setLayoutData(gridData);

		// Auto-hide popup setting.
		Label label = new Label(gPopup, SWT.WRAP);
		Messages.setLanguageText(label, LBLKEY_PREFIX + "popup.autohide");
		label.setLayoutData(new GridData());
		IntParameter auto_hide_alert = new IntParameter(gPopup,
				"Message Popup Autoclose in Seconds", 0, 86400);
		gridData = new GridData();
		gridData.horizontalSpan = 1;
		auto_hide_alert.setLayoutData(gridData);

		return cSection;
	}
}
