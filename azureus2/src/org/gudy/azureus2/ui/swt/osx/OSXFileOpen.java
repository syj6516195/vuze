package org.gudy.azureus2.ui.swt.osx;

import java.lang.reflect.Field;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.platform.macosx.access.jnilib.OSXAccess;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;

public class OSXFileOpen
{

	public static void fileOpen(String[] files) {
		for (String file : files) {
			fileOpen(file);
		}
	}

	public static void fileOpen(final String file) {
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				UIFunctionsManagerSWT.getUIFunctionsSWT().openTorrentOpenOptions(
						Utils.findAnyShell(), null, new String[] { file }, false, false);
			}
		});
	}

	/**
	 * Called by OSXAccess by reflection to do some SWT hooks
	 * 
	 * @since 5.0.0.1
	 */
	public static void initLight() {
		try {
  		Display display = new Display();
  		
  		// hook opendoc
  		try {
  			Field fldOpenDoc = SWT.class.getDeclaredField("OpenDocument");
  			int SWT_OpenDocument = fldOpenDoc.getInt(null);
  
  			display.addListener(SWT_OpenDocument, new Listener() {
  				public void handleEvent(final Event event) {
  					try {
  						OSXAccess.passParameter(event.text);
  					} catch (Throwable e) {
  					}
  				}
  			});
  		} catch (Throwable t) {
  		}

  		// OpenDoc will trigger almost immediately (in first loop).  However,
  		// do it for at least 300ms just in case 
  		for (int i = 0; i < 10; i ++) {
  			while (display.readAndDispatch()) {
  			}
  			Thread.sleep(30);
  		}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
