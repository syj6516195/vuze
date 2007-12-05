package org.gudy.azureus2.ui.swt.progress;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.ITwistieListener;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

public class ProgressReporterWindow
	implements IProgressReportConstants, ITwistieListener, DisposeListener
{
	private Shell shell;

	private ScrolledComposite scrollable;

	private Composite scrollChild;

	private IProgressReporter[] pReporters;

	/**
	 * A registry to keep track of all reporters that are being displayed in all instances
	 * of this window.
	 * @see #isOpened(IProgressReporter)
	 */
	private static final ArrayList reportersRegistry = new ArrayList();

	/**
	 * A special boolean to track whether this window is opened and is showing the empty panel;
	 * mainly used to prevent opening more than one of these window when there are no reporters to work with
	 */
	private static boolean isShowingEmpty = false;

	/**
	 * The default width for the shell upon first opening
	 */
	private int defaultShellWidth = 500;

	/**
	 * The maximum number of panels to show when the window first open
	 */
	private int initialMaxNumberOfPanels = 3;

	/**
	 * The style bits to use for this panel
	 */
	private int style;

	/**
	 * Construct a <code>ProgressReporterWindow</code> for a single <code>ProgressReporter</code> 
	 * @param pReporter
	 */
	private ProgressReporterWindow(IProgressReporter pReporter, int style) {
		this.style = style;
		if (null != pReporter) {
			pReporters = new IProgressReporter[] {
				pReporter
			};

		} else {
			pReporters = new IProgressReporter[0];
		}

		createControls();
	}

	/**
	 * Construct a single <code>ProgressReporterWindow</code> showing all <code>ProgressReporter</code>'s in the given array
	 * @param pReporters
	 */
	private ProgressReporterWindow(IProgressReporter[] pReporters, int style) {
		this.style = style;
		if (null != pReporters) {
			this.pReporters = pReporters;

		} else {
			pReporters = new IProgressReporter[0];
		}

		createControls();
	}

	/**
	 * Opens the window and display the given <code>IProgressReporter</code>
	 * 
	 * @param pReporter
	 * @param closeOnFinished <code>true</code> to automatically close this window when the reporter is finished; otherwise leave it opened
	 */
	public static void open(IProgressReporter pReporter, int style) {
		new ProgressReporterWindow(pReporter, style).openWindow();
	}

	/**
	 * Opens the window and display the given array of <code>IProgressReporter</code>'s
	 * @param pReporters
	 */
	public static void open(IProgressReporter[] pReporters, int style) {
		new ProgressReporterWindow(pReporters, style).openWindow();
	}

	/**
	 * Returns whether this window is already opened and is showing the empty panel
	 * @return
	 */
	public static boolean isShowingEmpty() {
		return isShowingEmpty;
	}

	/**
	 * Returns whether the given <code>IProgressReporter</code> is opened in any instance of this window;
	 * processes can query this method before opening another window to prevent opening multiple
	 * windows for the same reporter.  This is implemented explicitly instead of having the window automatically
	 * recycle instances because there are times when it is desirable to open a reporter in more than one
	 * instances of this window.
	 * @param pReporter
	 * @return
	 */
	public static boolean isOpened(IProgressReporter pReporter) {
		return reportersRegistry.contains(pReporter);
	}

	private void createControls() {
		/*
		 * Sets up the shell
		 */

		int shellStyle = SWT.DIALOG_TRIM | SWT.RESIZE;
		if ((style & MODAL) != 0) {
			shellStyle |= SWT.APPLICATION_MODAL;
		}

		shell = ShellFactory.createMainShell(shellStyle);
		shell.setText(MessageText.getString("progress.window.title"));

		if (!Constants.isOSX) {
			shell.setImage(ImageRepository.getImage("azureus"));
		}

		GridLayout gLayout = new GridLayout();
		gLayout.marginHeight = 0;
		gLayout.marginWidth = 0;
		shell.setLayout(gLayout);

		/*
		 * Using ScrolledComposite with only vertical scroll
		 */
		scrollable = new ScrolledComposite(shell, SWT.V_SCROLL);
		scrollable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		/*
		 * Main content composite where panels will be created
		 */
		scrollChild = new Composite(scrollable, SWT.NONE);

		GridLayout gLayoutChild = new GridLayout();
		gLayoutChild.marginHeight = 0;
		gLayoutChild.marginWidth = 0;
		gLayoutChild.verticalSpacing = 0;
		scrollChild.setLayout(gLayoutChild);
		scrollable.setContent(scrollChild);
		scrollable.setExpandVertical(true);
		scrollable.setExpandHorizontal(true);

		/*
		 * Re-adjust scrollbar setting when the window resizes
		 */
		scrollable.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle r = scrollable.getClientArea();
				scrollable.setMinSize(scrollChild.computeSize(r.width, SWT.DEFAULT));
			}
		});

		/*
		 * On closing remove all reporters that was handled by this instance of the window from the registry 
		 */
		shell.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {

				/*
				 * Remove this class as a listener to the disposal event for the panels or else
				 * as the shell is closing the panels would be disposed one-by-one and each one would
				 * force a re-layouting of the shell.
				 */
				Control[] controls = scrollChild.getChildren();
				for (int i = 0; i < controls.length; i++) {
					if (controls[i] instanceof ProgressReporterPanel) {
						((ProgressReporterPanel) controls[i]).removeDisposeListener(ProgressReporterWindow.this);
					}
				}

				/*
				 * Removes all the reporters that is still handled by this window
				 */
				for (int i = 0; i < pReporters.length; i++) {
					reportersRegistry.remove(pReporters[i]);
				}

				isShowingEmpty = false;
			}
		});

		if (pReporters.length == 0) {
			createEmptyPanel();
		} else {
			createPanels();
		}
	}

	/**
	 * Creates just an empty panel with a message indicating there are no reports to display 
	 */
	private void createEmptyPanel() {
		Label nothingToDisplay = new Label(scrollChild, SWT.NONE);
		GridData gData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gData.heightHint = 100;
		nothingToDisplay.setLayoutData(gData);
		nothingToDisplay.setText(MessageText.getString("Progress.reporting.no.reports.to.display"));

		/*
		 * Mark this as being opened and is showing the empty panel
		 */
		isShowingEmpty = true;

	}

	/**
	 * Set initial size and layout for the window then open it
	 */
	private void openWindow() {

		/*
		 * Using initialMaxNumberOfPanels as a lower limit we exclude all other panels from the layout,
		 * compute the window size, then finally we include all panels back into the layout
		 * 
		 *  This ensures that the window will fit exactly the desired number of panels
		 */
		Control[] controls = scrollChild.getChildren();
		for (int i = (initialMaxNumberOfPanels); i < controls.length; i++) {
			((GridData) controls[i].getLayoutData()).exclude = true;
		}

		Point p = shell.computeSize(defaultShellWidth, SWT.DEFAULT);

		for (int i = 0; i < controls.length; i++) {
			((GridData) controls[i].getLayoutData()).exclude = false;
		}
		formatLastPanel(null);
		scrollChild.layout();

		/*
		 * Set the shell size if it's different that the computed size
		 */
		if (false == shell.getSize().equals(p)) {
			shell.setSize(p);
			shell.layout(false);
		}

		/*
		 * Centers the window
		 */

		Utils.centreWindow(shell);

		shell.open();
	}

	private void createPanels() {

		int size = pReporters.length;

		/*
		 * Add the style bit for standalone if there is zero or 1 reporters
		 */
		if (size < 2) {
			style |= STANDALONE;
		}

		for (int i = 0; i < size; i++) {
			if (null != pReporters[i]) {

				/*
				 * Add this reporter to the registry
				 */
				reportersRegistry.add(pReporters[i]);

				/*
				 * Create the reporter panel; adding the style bit for BORDER
				 */
				final ProgressReporterPanel panel = new ProgressReporterPanel(
						scrollChild, pReporters[i], style | BORDER);

				panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

				panel.addTwistieListener(this);
				panel.addDisposeListener(this);
			}
		}

		formatLastPanel(null);
	}

	/**
	 * Formats the last <code>ProgressReporterPanel</code> in the window to extend to the bottom of the window.
	 * This method will iterate from the last panel backward to the first, skipping over the given panel.
	 * @param panelToIgnore 
	 */
	private void formatLastPanel(ProgressReporterPanel panelToIgnore) {
		Control[] controls = scrollChild.getChildren();

		for (int i = controls.length - 1; i >= 0; i--) {
			if (true != controls[i].equals(panelToIgnore)) {
				((GridData) controls[i].getLayoutData()).grabExcessVerticalSpace = true;
				break;
			}
		}
	}

	/**
	 * Remove the given <code>IProgressReporter</code> from the <code>pReporters</code> array; resize the array if required
	 * @param reporter
	 */
	private void removeReporter(IProgressReporter reporter) {

		/*
		 * Removes it from the registry
		 */
		reportersRegistry.remove(reporter);

		/*
		 * The array is typically small so this is good enough for now
		 */

		int IDX = Arrays.binarySearch(pReporters, reporter);
		if (IDX >= 0) {
			IProgressReporter[] rps = new IProgressReporter[pReporters.length - 1];
			for (int i = 0; i < rps.length; i++) {
				rps[i] = pReporters[(i >= IDX ? i + 1 : i)];
			}
			pReporters = rps;
		}
	}

	/**
	 * When any <code>ProgressReporterPanel</code> in this window is expanded or collapsed 
	 * re-layout the controls and window appropriately 
	 */
	public void isCollapsed(boolean value) {
		if (null != shell && false == shell.isDisposed()) {
			scrollable.setRedraw(false);
			Rectangle r = scrollable.getClientArea();
			scrollable.setMinSize(scrollChild.computeSize(r.width, SWT.DEFAULT));

			/*
			 * Resizing to fit the panel if there is only one
			 */
			if (pReporters.length == 1) {
				Point p = shell.computeSize(defaultShellWidth, SWT.DEFAULT);
				if (shell.getSize().y != p.y) {
					p.x = shell.getSize().x;
					shell.setSize(p);
				}
			}

			scrollable.layout();
			scrollable.setRedraw(true);
		}
	}

	/**
	 * When any <code>ProgressReporterPanel</code> in this window is disposed 
	 * re-layout the controls and window appropriately 
	 */
	public void widgetDisposed(DisposeEvent e) {

		if (e.widget instanceof ProgressReporterPanel) {
			ProgressReporterPanel panel = (ProgressReporterPanel) e.widget;
			removeReporter(panel.pReporter);

			panel.removeTwistieListener(this);

			/*
			 * Must let the GridLayout manager know that this control should be ignored
			 */
			((GridData) panel.getLayoutData()).exclude = true;
			panel.setVisible(false);

			/*
			 * If it's the last reporter then close the shell itself since it will be just empty
			 */
			if (pReporters.length == 0) {
				if ((style & AUTO_CLOSE) != 0) {
					if (null != shell && false == shell.isDisposed()) {
						shell.close();
					}
				} else {
					createEmptyPanel();
				}
			} else {

				/*
				 * Formats the last panel; specifying this panel as the panelToIgnore
				 * because at this point in the code this panel has not been removed
				 * from the window yet
				 */
				formatLastPanel(panel);
			}

			if (null != shell && false == shell.isDisposed()) {
				shell.layout(true, true);
			}
		}
	}
}
