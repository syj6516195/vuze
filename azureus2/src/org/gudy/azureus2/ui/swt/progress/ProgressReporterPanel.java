package org.gudy.azureus2.ui.swt.progress;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

/**
 * 
 * @author knguyen
 *
 */
public class ProgressReporterPanel
	extends Composite
	implements IProgressReportConstants, IProgressReporterListener
{

	private Color normalColor = null;

	private Color errorColor = null;

	public IProgressReporter pReporter = null;

	private Label imageLabel = null;

	private Label nameLabel = null;

	private Label statusLabel = null;

	private StyledText detailListWidget = null;

	private GridData detailSectionData = null;

	private AZProgressBar pBar = null;

	private Composite progressPanel = null;

	private TwistieSection detailSection = null;

	private int style;

	private Label actionLabel_cancel = null;

	private Label actionLabel_remove = null;

	private Label actionLabel_retry = null;

	/**
	 * The height of the detail panel when the window first appears.
	 * This only takes effect when there is some detail messages to display and when that
	 * list of messages is too long; this value limits the height of the panel so that the window
	 * does not grow to take up too much of the screen in such instances.
	 */
	private int maxPreferredDetailPanelHeight = 200;

	/**
	 * The preferred maximum height for the detail section on initialization
	 */
	private int maxPreferredDetailPanelHeight_Standalone = 600;

	/**
	 * The preferred maximum width for the panel.  When the longest line of the detail messages
	 * of the width of the title is too wide we use this limit to prevent the panel from taking
	 * up too much width; the user is still free to manually make the panel wider of narrower as desired
	 */
	private int maxPreferredWidth = 900;

	/**
	 * Create a panel for the given reporter.
	 * <code>style</code> could be one or more of these:
	 * <ul>
	 * <li><code>IProgressReportConstants.NONE</code> -- the default</li>
	 * <li><code>IProgressReportConstants.AUTO_CLOSE</code> -- automatically disposes this panel when the given reporter is done</li>
	 * <li><code>IProgressReportConstants.STANDALONE</code> -- this panel will be hosted by itself in a window; the detail section of this panel will be given more height</li>
	 * <li><code>IProgressReportConstants.BORDER</code> -- this panel will be hosted by itself in a window; the detail section of this panel will be given more height</li>
	 * </ul>
	 * @param parent the <code>Composite</code> hosting the panel
	 * @param reporter the <code>IProgressReporter</code> to host
	 * @param style one of the style bits listed above
	 */
	public ProgressReporterPanel(Composite parent, IProgressReporter reporter,
			int style) {
		super(parent, ((style & BORDER) != 0 ? SWT.BORDER : SWT.NONE));

		if (null == reporter) {
			throw new NullPointerException("IProgressReporter can not be null");//KN: should use resource?
		}

		this.pReporter = reporter;
		this.style = style;

		normalColor = Colors.blue;
		errorColor = Colors.colorError;

		/*
		 * Give this Composite some margin and spacing
		 */
		setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		GridLayout gLayout = new GridLayout(2, false);
		gLayout.marginWidth = 25;
		gLayout.marginTop =15;
		gLayout.marginBottom=10;
		setLayout(gLayout);

		/*
		 * Creates the rest of the controls
		 */
		createControls(pReporter.getProgressReport());

		/*
		 * Resize content when this panel changes size
		 */
		addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				resizeContent();
			}
		});

		/*
		 * Update the UI when ever the reporter send a report
		 */
		pReporter.addListener(this);

	}

	/**
	 * Call-back method from <code>IProgressReporterListener</code>; this method is called when ever the reporter
	 * dispatches an event
	 */
	public int report(IProgressReport pReport) {
		return handleEvents(pReport);
	}

	/**
	 * Creates all the controls for the panel
	 * @param pReport
	 */
	private void createControls(IProgressReport pReport) {

		imageLabel = new Label(this, SWT.NONE);
		imageLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, false, false,1,3));

		
		/* 
		 * Creates the main panel
		 */
		progressPanel = new Composite(this, SWT.NONE);
		progressPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout rightLayout = new GridLayout(4, false);
		rightLayout.marginHeight = 0;
		rightLayout.marginWidth = 0;
		progressPanel.setLayout(rightLayout);

		/*
		 * Creates all the controls
		 */

		nameLabel = new Label(progressPanel, SWT.WRAP);
		nameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false, 4, 1));

		pBar = new AZProgressBar(progressPanel, pReport.isIndeterminate());
		pBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		actionLabel_cancel = new Label(progressPanel, SWT.NONE);
		actionLabel_cancel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false,
				false));

		actionLabel_remove = new Label(progressPanel, SWT.NONE);
		actionLabel_remove.setLayoutData(new GridData(SWT.END, SWT.CENTER, false,
				false));

		actionLabel_retry = new Label(progressPanel, SWT.NONE);
		actionLabel_retry.setLayoutData(new GridData(SWT.END, SWT.CENTER, false,
				false));

		statusLabel = new Label(progressPanel, SWT.NONE);
		statusLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false,
				2, 1));

		/*
		 * Creates the detail section
		 */
		createDetailSection(pReport);

		/*
		 * Initialize controls from information in the given report
		 */
		initControls(pReport);

		/*
		 * Listener to 'cancel' label
		 */
		actionLabel_cancel.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				pReporter.cancel();
			}
		});

		/*
		 * Listener to 'retry' label
		 */
		actionLabel_retry.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				pReporter.retry();
			}
		});

		/*
		 * Listener to 'remove' label
		 */
		actionLabel_remove.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				/*
				 * Removes the current reporter from the history stack of the reporting manager
				 */
				ProgressReportingManager.getInstance().remove(pReporter);

				/*
				 * Then perform general clean-ups
				 */
				dispose();
			}
		});
	}

	/**
	 * Initialize the controls with information from the given <code>IProgressReport</code>
	 * @param pReport
	 */
	private void initControls(IProgressReport pReport) {

		/*
		 * Image
		 */
		if (null != pReport.getImage()) {
			imageLabel.setImage(pReport.getImage());
		} else {
			imageLabel.setImage(getDisplay().getSystemImage(SWT.ICON_INFORMATION));
		}

		/*
		 * Name label
		 */
		nameLabel.setText(formatForDisplay(pReport.getName()));

		/*
		 * Action labels
		 */

		/* replaced with existing images to integrate with Az look&feel
		actionLabel_cancel.setImage(ImageRepository.getImage("progress_cancel"));
		actionLabel_remove.setImage(ImageRepository.getImage("progress_remove"));
		actionLabel_retry.setImage(ImageRepository.getImage("progress_retry"));
		*/
		
		actionLabel_cancel.setImage(ImageRepository.getImage("stop"));
		actionLabel_remove.setImage(ImageRepository.getImage("delete"));
		actionLabel_retry.setImage(ImageRepository.getImage("recheck"));

		actionLabel_cancel.setToolTipText(MessageText.getString("Progress.reporting.action.label.cancel.tooltip"));
		actionLabel_remove.setToolTipText(MessageText.getString("Progress.reporting.action.label.remove.tooltip"));
		actionLabel_retry.setToolTipText(MessageText.getString("Progress.reporting.action.label.retry.tooltip"));

		/* ========================================
		 * Catch up on any messages we might have missed
		 */
		{

			if (true == pReport.isDone()) {
				updateStatusLabel(
						MessageText.getString("Progress.reporting.status.finished"), false);
			} else if (true == pReport.isInErrorState()) {
				updateStatusLabel(
						MessageText.getString("Progress.reporting.default.error"), true);
			} else if (true == pReport.isCanceled()) {
				updateStatusLabel(
						MessageText.getString("Progress.reporting.status.canceled"), false);
			} else if (true == pReport.isIndeterminate()) {
				updateStatusLabel(Constants.INFINITY_STRING, false);
			} else {
				updateStatusLabel(pReport.getPercentage() + "%", false);
			}

		}

		/*
		 * Synch the progress bar
		 */
		synchProgressBar(pReport);

		/*
		 * Synch the action label according to existing properties of the ProgressReport
		 */
		synchActionLabels(pReport);
	}

	/**
	 * Below the panel, taking up the entire width of the window, is the detail section
	 */
	private void createDetailSection(IProgressReport pReport) {
		Label separator = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		detailSection = new TwistieSection(this, TwistieLabel.NONE);
		detailSection.setTitle(MessageText.getString("Progress.reporting.action.label.detail"));
		Composite sectionContent = detailSection.getContent();

		detailSectionData = new GridData(SWT.FILL, SWT.FILL, true, true);
		detailSection.setLayoutData(detailSectionData);

		GridLayout sectionLayout = new GridLayout();
		sectionLayout.marginHeight = 0;
		sectionLayout.marginWidth = 0;
		sectionContent.setLayout(sectionLayout);
		detailSection.setEnabled(false);

		detailListWidget = new StyledText(sectionContent, SWT.BORDER | SWT.V_SCROLL
				| SWT.WRAP);
		detailListWidget.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		/*
		 * Add a default message instead of an empty box if there is no history;
		 * remove this later when a real detail message arrive
		 */

		IMessage[] messages = pReporter.getMessageHistory();
		/*
		 * Show error messages in red; otherwise use default color
		 */
		for (int i = 0; i < messages.length; i++) {
			if (messages[i].getType() == MSG_TYPE_ERROR) {
				appendToDetail(formatForDisplay(messages[i].getValue()), true);
			} else {
				appendToDetail(formatForDisplay(messages[i].getValue()), false);
			}
		}

		resizeDetailSection();

		/*
		 * Force a layout when ever the section is collapsed or expanded
		 */
		detailSection.addTwistieListener(new ITwistieListener() {
			public void isCollapsed(boolean value) {
				resizeDetailSection();
				layout(true, true);
			}

		});

	}

	/**
	 * Ensure that the detail does not take up too much vertical space
	 */
	private void resizeDetailSection() {
		/*
		 * Since detail panel automatically grows to show the entire list of detail messages
		 * we want to at least place a limit on its height just in case the list is too long.
		 * A vertical scrollbar will appear so the user can scroll through the rest of the list 
		 */

		Point computedSize = detailSection.computeSize(SWT.DEFAULT, SWT.DEFAULT);

		detailSectionData.heightHint = computedSize.y;

		if ((STANDALONE & style) != 0) {
			if (computedSize.y > maxPreferredDetailPanelHeight_Standalone) {
				detailSectionData.heightHint = maxPreferredDetailPanelHeight_Standalone;
			}
		} else if (computedSize.y > maxPreferredDetailPanelHeight) {
			detailSectionData.heightHint = maxPreferredDetailPanelHeight;
		}

		if (computedSize.x > maxPreferredWidth) {
			detailSectionData.widthHint = maxPreferredWidth;
		}

	}

	public Point computeSize(int hint, int hint2, boolean changed) {
		Point newSize = super.computeSize(hint, hint2, changed);

		if (newSize.x > maxPreferredWidth) {
			newSize.x = maxPreferredWidth;
		}
		return newSize;
	}

	/**
	 * Process the event from the given <code>ProgressReport</code>
	 * 
	 * @param pReport
	 */
	private int handleEvents(final IProgressReport pReport) {
		if (null == pReport || true == isDisposed() || null == getDisplay()) {
			return RETVAL_OK;
		}

		/* Note each 'case' statement of the 'switch' block performs its UI update encapsulated in an .asyncExec()
		 * so that the UI does not freeze or flicker.  It may be tempting to encapsulate the whole 'switch' block
		 * within one .asyncExec() but then a separate loop would be required to wait for the Runnable to finish
		 * before attaining the correct return code.  The alternative would be to encapsulate the 'switch' block
		 * in a .syncExec() but that would cause freezing and flickering
		 */

		switch (pReport.getReportType()) {
			case REPORT_TYPE_PROPERTY_CHANGED:

				getDisplay().asyncExec(new Runnable() {
					public void run() {
						if (null != nameLabel && false == nameLabel.isDisposed()) {
							nameLabel.setText(pReport.getName());
						}
						if (true == pReport.isIndeterminate()) {
							updateStatusLabel(Constants.INFINITY_STRING, false);
						} else {
							updateStatusLabel(pReport.getPercentage() + "%", false);
						}
						appendToDetail(pReport.getMessage(), false);
						appendToDetail(pReport.getDetailMessage(), false);
						synchProgressBar(pReport);
						synchActionLabels(pReport);
						resizeContent();
					}
				});
				break;
			case REPORT_TYPE_CANCEL:
				getDisplay().asyncExec(new Runnable() {
					public void run() {
						synchProgressBar(pReport);
						updateStatusLabel(
								MessageText.getString("Progress.reporting.status.canceled"),
								false);
						appendToDetail(pReport.getMessage(), false);
						synchActionLabels(pReport);
						resizeContent();
					}
				});
				break;
			case REPORT_TYPE_DONE:
				getDisplay().asyncExec(new Runnable() {
					public void run() {

						if (((style & AUTO_CLOSE) != 0)) {
							dispose();
						} else {

							synchProgressBar(pReport);
							updateStatusLabel(
									MessageText.getString("Progress.reporting.status.finished"),
									false);
							appendToDetail(
									MessageText.getString("Progress.reporting.status.finished"),
									false);
							synchActionLabels(pReport);
							resizeContent();
						}
					}
				});

				/*
				 * Since the reporter is done we don't need this listener anymore
				 */
				return RETVAL_OK_TO_DISPOSE;
			case REPORT_TYPE_MODE_CHANGE:
				getDisplay().asyncExec(new Runnable() {
					public void run() {
						if (null != pBar && false == pBar.isDisposed()) {
							pBar.setIndeterminate(pReport.isIndeterminate());
						}
					}
				});
				break;
			case REPORT_TYPE_ERROR:
				getDisplay().asyncExec(new Runnable() {
					public void run() {
						updateStatusLabel(
								MessageText.getString("Progress.reporting.default.error"), true);
						appendToDetail(pReport.getErrorMessage(), true);
						synchActionLabels(pReport);
						synchProgressBar(pReport);
						resizeContent();
					}
				});
				break;

			case REPORT_TYPE_RETRY:
				getDisplay().asyncExec(new Runnable() {
					public void run() {
						updateStatusLabel(pReport.getMessage(), false);
						appendToDetail(
								MessageText.getString("Progress.reporting.status.retrying"),
								false);
						synchActionLabels(pReport);
						synchProgressBar(pReport);
						resizeContent();
					}
				});

				break;
			default:
				break;
		}

		return RETVAL_OK;
	}

	/**
	 * Synchronize the progress bar with the given <code>IProgressReport</code>
	 * @param pReport
	 */
	private void synchProgressBar(IProgressReport pReport) {
		if (null == pBar || pBar.isDisposed() || null == pReport) {
			return;
		}

		if (true == pReport.isInErrorState()) {
			pBar.setIndeterminate(false);
			pBar.setSelection(pReport.getMinimum());
		} else {
			pBar.setIndeterminate(pReport.isIndeterminate());
			if (false == pReport.isIndeterminate()) {
				pBar.setMinimum(pReport.getMinimum());
				pBar.setMaximum(pReport.getMaximum());
			}
			pBar.setSelection(pReport.getSelection());
		}
	}

	/**
	 * Sets the defined color to the given <code>label</code>
	 * @param label
	 * @param text
	 * @param showAsError <code>true</code> to show as error; <code>false</code> otherwise
	 */
	private void updateStatusLabel(String text, boolean showAsError) {
		if (null == statusLabel || statusLabel.isDisposed()) {
			return;
		}
		statusLabel.setText(formatForDisplay(text));
		if (false == showAsError) {
			statusLabel.setForeground(normalColor);
		} else {
			statusLabel.setForeground(errorColor);
		}
		statusLabel.update();

	}

	/**
	 * Display the appropriate text for the action labels
	 * based on what action can be taken
	 */
	private void synchActionLabels(IProgressReport pReport) {
		if (null == actionLabel_remove || null == actionLabel_cancel
				|| null == actionLabel_retry || true == actionLabel_remove.isDisposed()
				|| true == actionLabel_cancel.isDisposed()
				|| true == actionLabel_retry.isDisposed()) {
			return;
		}

		/*
		 * There are 3 labels that can be clicked on; base on the state of the reporter itself.
		 * The basic rules are these:
		 * 
		 * 	If it's done
		 * 		then show just "remove"
		 * 
		 * 	else if it's in error
		 * 		and retry is allowed
		 * 		then show "retry" and "remove"
		 * 		else just show "remove"
		 * 
		 * 	else if it's been canceled
		 * 		and retry is allowed
		 * 		then show "retry" and "remove"
		 * 		else just show "remove"
		 * 
		 * 	else if it's none of the above
		 * 		then show just the "cancel" label
		 * 			enable the label if cancel is allowed
		 * 			else disable the label
		 * 
		 */

		showActionLabel(actionLabel_cancel, false);
		showActionLabel(actionLabel_remove, false);
		showActionLabel(actionLabel_retry, false);

		if (true == pReport.isDone()) {
			showActionLabel(actionLabel_remove, true);
		} else if (true == pReport.isInErrorState()) {
			if (true == pReport.isRetryAllowed()) {
				showActionLabel(actionLabel_retry, true);
				showActionLabel(actionLabel_remove, true);
			} else {
				showActionLabel(actionLabel_remove, true);
			}

		} else if (true == pReport.isCanceled()) {
			if (true == pReport.isRetryAllowed()) {
				showActionLabel(actionLabel_retry, true);
				showActionLabel(actionLabel_remove, true);
			} else {
				showActionLabel(actionLabel_remove, true);
			}

		} else {
			showActionLabel(actionLabel_cancel, true);
			actionLabel_cancel.setEnabled(pReport.isCancelAllowed());
		}

	}

	/**
	 * Convenience method for showing or hiding a label by setting its <code>GridData.widthHint</code>
	 * @param label
	 * @param showIt
	 */
	private void showActionLabel(Label label, boolean showIt) {
		((GridData) label.getLayoutData()).widthHint = (true == showIt) ? 16 : 0;
	}

	/**
	 * Resizes the content of this panel to fit within the shell and to layout children control appropriately
	 */
	public void resizeContent() {
		if (false == isDisposed()) {
			layout(true, true);
		}
	}

	/**
	 * Formats the string so it displays properly in an SWT text control
	 * @param string
	 * @return
	 */
	private String formatForDisplay(String string) {
		/*
		 * SWT text controls do not allow a null argument as a value and will throw an exception if a <code>null</code> is encountered</p>
		 */
		string = null == string ? "" : string;

		/*
		 * Escaping the '&' character so it won't be shown as an underscore
		 */
		return string.replaceAll("&", "&&");
	}

	public void dispose() {
		imageLabel.dispose();
		nameLabel.dispose();
		pBar.dispose();
		actionLabel_cancel.dispose();
		actionLabel_remove.dispose();
		actionLabel_retry.dispose();
		statusLabel.dispose();
		super.dispose();
	}

	public void addTwistieListener(ITwistieListener listener) {
		detailSection.addTwistieListener(listener);
	}

	public void removeTwistieListener(ITwistieListener listener) {
		detailSection.removeTwistieListener(listener);
	}

	/**
	 * Appends the given message to the detail panel; render the message in error color if specified
	 * @param value
	 * @param isError if <code>true</code> then render the message in the system error color; otherwise render in default color
	 */
	private void appendToDetail(String value, boolean isError) {

		if (null == value || value.length() < 1) {
			return;
		}

		if (null == detailListWidget || detailListWidget.isDisposed()) {
			return;
		}

		int charCount = detailListWidget.getCharCount();
		detailListWidget.append(value + "\n");
		if (true == isError) {
			StyleRange style2 = new StyleRange();
			style2.start = charCount;
			style2.length = value.length();
			style2.foreground = errorColor;
			detailListWidget.setStyleRange(style2);
		}
		detailSection.setEnabled(true);
	}
}