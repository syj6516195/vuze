/*
 * Created on Apr 30, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package org.gudy.azureus2.ui.swt.speedtest;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.WizardListener;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestScheduledTest;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestScheduledTestListener;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTesterListener;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTestScheduler;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTester;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSpeedTesterResult;
import com.aelitis.azureus.core.networkmanager.admin.impl.NetworkAdminSpeedTestSchedulerImpl;

public class 
SpeedTestPanel
	extends AbstractWizardPanel 
	implements NetworkAdminSpeedTestScheduledTestListener, NetworkAdminSpeedTesterListener
{

    private NetworkAdminSpeedTestScheduler nasts;
	private NetworkAdminSpeedTestScheduledTest	scheduled_test;

    private Combo testCombo;

    private Button      test;
    private Button      abort;
    private Label       testCountDown1;
    private Label       testCountDown2;

    private Text textMessages;
	private ProgressBar progress;
	private Display 	display;

	private boolean		test_running;
	private boolean		switched_to_close;

    //measured upload and download results.
    int uploadTest, downloadTest;

    WizardListener clListener;

    private static final String START_VALUES = "   -         ";

    public
	SpeedTestPanel(
		SpeedTestWizard _wizard, 
		IWizardPanel 	_previousPanel) 
	{
	    super( _wizard, _previousPanel );
	    wizard	= _wizard;
		nasts = NetworkAdminSpeedTestSchedulerImpl.getInstance();
    }
	
	public void 
	show() 
	{
		display = wizard.getDisplay();
		wizard.setTitle(MessageText.getString( SpeedTestWizard.CFG_PREFIX + "run" ));
		wizard.setCurrentInfo("BitTorrent bandwidth testing.");
		wizard.setPreviousEnabled(false);
        wizard.setFinishEnabled(false);

        Composite rootPanel = wizard.getPanel();
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		rootPanel.setLayout(layout);

        Composite panel = new Composite(rootPanel, SWT.NULL);
        GridData gridData = new GridData(GridData.FILL_BOTH);
		panel.setLayoutData(gridData);

        //label explain section.
        layout = new GridLayout();
        layout.numColumns = 4;
        panel.setLayout(layout);

        Label explain = new Label(panel, SWT.WRAP);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 4;
        explain.setLayoutData(gridData);
        StringBuffer sb = new StringBuffer("This test will measure the speed at which data can be simultaneously uploaded");
        sb.append("and downloaded in your network. The test will first request a testing slot from our service. If a ");
        sb.append("testing slot is available it will then get a virtual torrent, half for uploading and half for downloading.");
        sb.append("Once the test start it has 2 minutes to complete. It will first pause all the downloads, then set a ");
        sb.append("very high global limit, so it doesn't consume too much server bandwidth. It lets the download rate ");
        explain.setText( sb.toString() );

        //space line
        Label spacer = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 4;
        spacer.setLayoutData(gridData);

        //label type and button section.
        Label ul = new Label(panel, SWT.NULL );
        gridData = new GridData();
        ul.setLayoutData(gridData);
        ul.setText("Azureus speed test: ");

        //Label ulType = new Label(panel, SWT.NULL);
        //gridData = new GridData(GridData.FILL_HORIZONTAL);
        //ulType.setLayoutData(gridData);
        //ulType.setText("BT upload/download");

        testCombo = new Combo(panel, SWT.READ_ONLY);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        testCombo.setLayoutData(gridData);
           
        int[]	test_types  	= NetworkAdminSpeedTester.TEST_TYPES;
        int		up_down_index 	= 0;
        
        for (int i=0;i<test_types.length;i++){
        	
        	int	test_type = test_types[i];
        	
        	String	resource = null;
        	
        	if ( test_type == NetworkAdminSpeedTester.TEST_TYPE_UPLOAD_AND_DOWNLOAD ){
        	
        		resource = "updown";
        		
        		up_down_index = i;
        		
        	}else if ( test_type == NetworkAdminSpeedTester.TEST_TYPE_UPLOAD_ONLY ){
        		
        		resource = "up";
        		
        	}else if ( test_type == NetworkAdminSpeedTester.TEST_TYPE_DOWNLOAD_ONLY ){
        		
        		resource = "down";
        	}else{
        		
        		Debug.out( "Unknown test type" );
        	}
        	
        	testCombo.add( "BT " + MessageText.getString( "speedtest.wizard.test.mode." + resource ), i);
        }
        
        testCombo.select( up_down_index );

        test = new Button(panel, SWT.PUSH);
        test.setText("Run");
        gridData = new GridData();
        gridData.widthHint = 70;
        test.setLayoutData(gridData);
        test.addListener(SWT.Selection, new RunButtonListener() );

        abort = new Button(panel, SWT.PUSH);
        abort.setText("Abort");
        gridData = new GridData();
        gridData.widthHint = 70;
        abort.setLayoutData(gridData);
        abort.setEnabled(false);
        abort.addListener(SWT.Selection, new AbortButtonListener() );

        //space line
        spacer = new Label(panel, SWT.NULL);
        gridData = new GridData();
        gridData.horizontalSpan = 4;
        spacer.setLayoutData(gridData);

        //test count down section.
        Label abortCountDown = new Label(panel, SWT.NULL);
        gridData = new GridData();
        abortCountDown.setLayoutData(gridData);
        abortCountDown.setText("abort test in: ");

        testCountDown1 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        testCountDown1.setLayoutData(gridData);
        testCountDown1.setText(START_VALUES);

        Label testFinishCountDown = new Label(panel, SWT.NULL);
        gridData = new GridData();
        testFinishCountDown.setLayoutData(gridData);
        testFinishCountDown.setText("test finish in: ");

        testCountDown2 = new Label(panel, SWT.NULL);
        gridData = new GridData();
        testCountDown2.setLayoutData(gridData);
        testCountDown2.setText(START_VALUES);

        
        //progress bar section.
        progress = new ProgressBar(panel, SWT.SMOOTH);
		progress.setMinimum(0);
		progress.setMaximum(100);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 4;
        progress.setLayoutData(gridData);

        //message text section.
        textMessages = new Text(panel, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL );
		textMessages.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 4;
        textMessages.setLayoutData(gridData);

	}

	public void 
	finish() 
	{
		test_running	= true;

        clListener = new WizardListener()
			{
				public void
				closed()
				{
					cancel();
				}
			};

        wizard.addListener(clListener);

        wizard.setFinishEnabled( false );

        	// convert to mode
        
        final int test_mode = NetworkAdminSpeedTester.TEST_TYPES[testCombo.getSelectionIndex()];
    
        	
        Thread t =
			new AEThread("SpeedTest Performer") 
			{
				public void 
				runSupport() 
				{

                    runTest(test_mode);
				}
			};
		
		t.setPriority(Thread.MIN_PRIORITY);
		t.setDaemon(true);
		t.start();
	}
	  
	public void
	cancel()
	{
		if ( scheduled_test != null ){
		
			scheduled_test.abort();

			if ( !test.isDisposed()){
         
                test.setEnabled(true);   
                abort.setEnabled(false);         
                wizard.setNextEnabled(false);
                wizard.setFinishEnabled(false);
  			}
        }
	}
	
	protected void
	runTest( int test_mode )
	{
		test_running	= true;
		
		if ( nasts.getCurrentTest() !=  null ){
	
			reportStage( "Test already running!" );

		}else{
				// what's the contract here in terms of listener removal?
			
			try{
                scheduled_test = nasts.scheduleTest( NetworkAdminSpeedTestScheduler.TEST_TYPE_BT );
                
                scheduled_test.getTester().setMode( test_mode );
                
 				scheduled_test.addListener( this );
				scheduled_test.getTester().addListener( this );
				scheduled_test.start();
				
			}catch( Throwable e ){
				
				reportStage( "Test request not accepted: " + Debug.getNestedExceptionMessage(e));
								
			    if (!test.isDisposed()) {
				      display.asyncExec(new AERunnable(){
				        public void runSupport() {

			                test.setEnabled(true);
			                abort.setEnabled(false);
				        }
				      });
			    }

			}
		}//else
	}//runTest
	
	public void 
	stage(
		NetworkAdminSpeedTestScheduledTest 	test, 
		String 								step )
	{
		reportStage( step );
	}

	public void 
	complete(
		NetworkAdminSpeedTestScheduledTest test )
	{
	}
	
	public void 
	stage(
		NetworkAdminSpeedTester 	tester, 
		String 						step )
	{
		reportStage( step );
	}
	
	public void 
	complete(
		NetworkAdminSpeedTester			tester,
		NetworkAdminSpeedTesterResult 	result )
	{
		reportComplete( result );
	}
	
	protected void 
	reportComplete(
		final NetworkAdminSpeedTesterResult 	result )
	{
	    if ( !textMessages.isDisposed()) {
		      display.asyncExec(new AERunnable(){
		        public void runSupport() {
		        	if ( !textMessages.isDisposed()){
		        	  if ( result.hadError()){
		        		  
		        		  textMessages.append( "Test failed: " + result.getLastError());
		        		  test.setEnabled( true );
		        		  abort.setEnabled(false);
		                  wizard.setErrorMessage("Test failed");
		                  
		        	  }else{
                        uploadTest = result.getUploadSpeed();
                        downloadTest = result.getDownloadSpeed();
                        textMessages.append("Upload speed = " + DisplayFormatters.formatByteCountToKiBEtcPerSec(result.getUploadSpeed()) + Text.DELIMITER);
			            textMessages.append("Download speed = " + DisplayFormatters.formatByteCountToKiBEtcPerSec(result.getDownloadSpeed()) + Text.DELIMITER);

			            
                        if( result.getTest().getMode() == NetworkAdminSpeedTester.TEST_TYPE_DOWNLOAD_ONLY ){
                            //only the combined test will allow the next step.
                        }else{
                            wizard.setNextEnabled(true);
                        }
                        abort.setEnabled(false);
                        test.setEnabled(true);
		        	  }

	                  if( !result.hadError() ){
	                    switchToClose();
	                  }
		        	}
                }
		      });
        }
        wizard.removeListener(clListener);
        clListener=null;
    }

	protected void 
	reportStage(
		final String 						step )
	{
	    if ( !textMessages.isDisposed()) {
		      display.asyncExec(new AERunnable(){
		        public void runSupport() {

		        	if ( !textMessages.isDisposed()){
	                  if(step==null)
	                    return;
	
	                  //intercept progress indications.
	                  if( step.startsWith("progress:")){
	                      //expect format of string to be "progress: # : ..." where # is 0-100
	                      int progressAmount = getProgressBarValueFromString(step);
	                      progress.setSelection(progressAmount);
	
	                      int[] timeLeft = getTimeLeftFromString(step);
	                      if(timeLeft!=null){
	                          testCountDown1.setText( ""+timeLeft[0]+" sec " );
	                          testCountDown2.setText( ""+timeLeft[1]+" sec " );
	                      }else{
	                          testCountDown1.setText(START_VALUES);
	                          testCountDown2.setText(START_VALUES);
	                      }
	                  }
	
	                  //print everything including progress indications.
        
			           textMessages.append( step + Text.DELIMITER);
		          }
		        }
		      });
		    }	
	}

    /**
     * If you find the time left values then use them. On any error return null and the calling
     * function should handle that condition.
     * @param step - String in format "progress: #: text: text: #: #"     The last two items are
     *               the seconds till abort and seconds till complete respectively.
     * @return - int array of size 2 with time left in test, or null on any error.
     */
    private static int[] getTimeLeftFromString(String step){
        if(step==null)
            return null;
        if( !step.startsWith("progress:") )
            return null;

        String[] values = step.split(":");
            if(values.length<5)
                return null;

        int[] times = new int[2];
        try{
            times[0] = Integer.parseInt( values[4].trim() );
            times[1] = Integer.parseInt( values[5].trim() );
        }catch(Exception e){
            return null;
        }
        return times;
    }//getTimeLeftFromString

    /**
     *
     * @param step - String with the expected format.  "progress: #" where # is 0 - 100.
     * @return The number as an integer, if the result is not known return 0.
     */
    private static int getProgressBarValueFromString(String step){
        if(step==null)
            return 0;
        
        if( !step.startsWith("progress:") )
            return 0;

        String[] value = step.split(":");
        if(value.length<2)
            return 0;

        int progress;
        try{
            progress = Integer.parseInt(value[1].trim());
        }catch(Exception e){
            return 0;
        }

        if( progress<0 || progress>100 )
            return 0;

        return progress;
    }//getProgressValueFromString

    protected void
	switchToClose()
	{
		switched_to_close	= true;
		
		wizard.switchToClose();
	}
	
	public boolean
	isFinishEnabled()
	{
		return( !( switched_to_close || test_running ));
	}
	
	public boolean
	isFinishSelectionOK()
	{
		return( !( switched_to_close || test_running ));
	}
	
	public IWizardPanel 
	getFinishPanel()
	{
		return( this );
	}


    public boolean isNextEnabled(){
        //only enable after the test completes correctly.
        return uploadTest>0;
    }//isNextEnabled

    public IWizardPanel getNextPanel() {
        return new SetUploadLimitPanel( wizard, this, uploadTest, downloadTest);
    }

    /**
     * An abort button listener
     */
    class AbortButtonListener implements Listener{

        public void handleEvent(Event event) {
            //same action as "cancel" button.
            cancel();
            test.setEnabled(true);
            abort.setEnabled(false);
            wizard.setNextEnabled(false);
            uploadTest=0;
            downloadTest=0;

            wizard.setErrorMessage("test aborted manually.");
            reportStage("\ntest aborted manually.");

        }//handleEvent
    }


    /**
     * A run button listener
     */
    class RunButtonListener implements Listener{

        public void handleEvent(Event event) {
            abort.setEnabled(true);
            test.setEnabled(false);
            wizard.setErrorMessage("");
            finish();
        }//handleEvent
    }

}
