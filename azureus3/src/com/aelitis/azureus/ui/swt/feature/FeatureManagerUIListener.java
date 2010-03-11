package com.aelitis.azureus.ui.swt.feature;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.utils.FeatureManager;
import org.gudy.azureus2.plugins.utils.FeatureManager.FeatureManagerListener;
import org.gudy.azureus2.plugins.utils.FeatureManager.Licence;
import org.gudy.azureus2.plugins.utils.FeatureManager.Licence.LicenceInstallationListener;

import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.mdi.MdiEntrySWT;
import com.aelitis.azureus.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.aelitis.azureus.ui.swt.views.skin.*;

public class FeatureManagerUIListener
	implements FeatureManagerListener
{
	private final static boolean DEBUG = true;

	private final FeatureManager featman;

	private boolean hasPendingAuth;

	public FeatureManagerUIListener(FeatureManager featman) {
		System.out.println("FEAT:");
		this.featman = featman;
	}

	public void licenceAdded(Licence licence) {
		updateSidebar();

		if (DEBUG) {
			System.out.println("FEAT: Licence Added with state " + licence.getState());
		}

		if (licence.getState() == Licence.LS_PENDING_AUTHENTICATION) {
			hasPendingAuth = true;
			FeatureManagerUI.openLicenceValidatingWindow();
		}

		if (licence.isFullyInstalled()) {
			return;
		}

		licence.addInstallationListener(new LicenceInstallationListener() {

			public void start(String licence_key) {
				if (DEBUG) {
					System.out.println("FEATINST: START!");
				}
				try {
					Licence licence = featman.addLicence(licence_key);
					new FeatureManagerInstallWindow(licence).open();
				} catch (PluginException e) {
					Debug.out(e);
				}
			}

			public void reportProgress(String licenceKey, String install, int percent) {
				if (DEBUG) {
					System.out.println("FEATINST: " + install + ": " + percent);
				}
			}

			public void reportActivity(String licenceKey, String install,
					String activity) {
				if (DEBUG) {
					System.out.println("FEAT: ACTIVITY: " + install + ": " + activity);
				}
			}

			public void failed(String licenceKey, PluginException error) {
			}

			public void complete(String licenceKey) {
				if (hasPendingAuth) {
					hasPendingAuth = false;
					FeatureManagerUI.openLicenceSuccessWindow();
				}
			}

		});
	}

	public void licenceChanged(Licence licence) {
		updateSidebar();
		int state = licence.getState();
		if (DEBUG) {
			System.out.println("FEAT: License State Changed: " + state);
		}
		if (state == Licence.LS_PENDING_AUTHENTICATION) {
			hasPendingAuth = true;
			FeatureManagerUI.openLicenceValidatingWindow();
		} else if (state == Licence.LS_INVAID_KEY) {
			FeatureManagerUI.openLicenceFailedWindow(state);
		} else {
			FeatureManagerUI.closeLicenceValidatingWindow();
			if (state == Licence.LS_AUTHENTICATED) {
				if (hasPendingAuth) {
					hasPendingAuth = false;
					if (licence.isFullyInstalled()) {
						FeatureManagerUI.openLicenceSuccessWindow();
					}
				}
			}
		}
	}

	/**
	 * 
	 */
	private void updateSidebar() {
		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		if (mdi != null) {
			MdiEntrySWT entry = mdi.getEntrySWT(MultipleDocumentInterface.SIDEBAR_SECTION_PLUS);
			if (entry != null) {
				boolean hasFullLicence = FeatureManagerUI.hasFullLicence();
				String title = MessageText.getString(hasFullLicence
						? "mdi.entry.plus.full" : "mdi.entry.plus.free");
				entry.setTitle(title);
				SBC_PlusFTUX view = (SBC_PlusFTUX) SkinViewManager.getByClass(SBC_PlusFTUX.class);
				if (view != null) {
					view.updateLicenceInfo();
				}
				SkinView[] views = SkinViewManager.getMultiByClass(SBC_BurnFTUX.class);
				if (views != null) {
					for (SkinView bview : views) {
						((SBC_BurnFTUX) bview).updateLicenceInfo();
					}
				}
			}
		}
	}

	public void licenceRemoved(Licence licence) {
		updateSidebar();
	}

}