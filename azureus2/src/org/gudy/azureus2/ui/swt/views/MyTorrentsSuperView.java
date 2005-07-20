package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Event;

import com.aelitis.azureus.core.*;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.views.MyTorrentsView;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.*;
import org.gudy.azureus2.plugins.ui.tables.TableManager;


public class MyTorrentsSuperView extends AbstractIView  {
  private AzureusCore	azureus_core;
  
  private MyTorrentsView torrentview;
  private MyTorrentsView seedingview;
  private SashForm form;

  final static TableColumnCore[] tableIncompleteItems = {
    new HealthItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new RankItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new NameItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new SizeItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new DownItem(),
    new DoneItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new StatusItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new SeedsItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new PeersItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new DownSpeedItem(),
    new UpSpeedItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new ETAItem(),    
    new ShareRatioItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new UpItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),    
    new UpSpeedLimitItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new TrackerStatusItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    
    // Initially Invisible    
    new RemainingItem(),
    new PiecesItem(),
    new CompletionItem(),
    new MaxUploadsItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new TotalSpeedItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new SavePathItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new TorrentPathItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new CategoryItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new NetworksItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new PeerSourcesItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new AvailabilityItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new SecondsSeedingItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new SecondsDownloadingItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new OnlyCDing4Item(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new TrackerNextAccessItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new TrackerNameItem( TableManager.TABLE_MYTORRENTS_INCOMPLETE ),
    new SeedToPeerRatioItem( TableManager.TABLE_MYTORRENTS_INCOMPLETE ),
    new DownSpeedLimitItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new SwarmAverageSpeed( TableManager.TABLE_MYTORRENTS_INCOMPLETE )
  };

  final static TableColumnCore[] tableCompleteItems = {
    new HealthItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new RankItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new NameItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new SizeItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new DoneItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new StatusItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new SeedsItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new PeersItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new UpSpeedItem(TableManager.TABLE_MYTORRENTS_COMPLETE),    
    new ShareRatioItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new UpItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new UpSpeedLimitItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    
    // Initially Invisible
    new MaxUploadsItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new TotalSpeedItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new SavePathItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new TorrentPathItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new CategoryItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new NetworksItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new PeerSourcesItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new AvailabilityItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new SecondsSeedingItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new SecondsDownloadingItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new OnlyCDing4Item(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new TrackerStatusItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new TrackerNextAccessItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new TrackerNameItem( TableManager.TABLE_MYTORRENTS_COMPLETE ),
    new SeedToPeerRatioItem( TableManager.TABLE_MYTORRENTS_COMPLETE ),
    new SwarmAverageSpeed( TableManager.TABLE_MYTORRENTS_COMPLETE )
  };

  public MyTorrentsSuperView(AzureusCore	_azureus_core) {
  	azureus_core		= _azureus_core;

    TableColumnManager tcExtensions = TableColumnManager.getInstance();
    for (int i = 0; i < tableCompleteItems.length; i++) {
      tcExtensions.addColumn(tableCompleteItems[i]);
    }
    for (int i = 0; i < tableIncompleteItems.length; i++) {
      tcExtensions.addColumn(tableIncompleteItems[i]);
    }
  }

  public Composite getComposite() {
    return form;
  }
  
  public void delete() {
    MainWindow.getWindow().setMytorrents(null);
    if (torrentview != null)
      torrentview.delete();
    if (seedingview != null)
      seedingview.delete();
    super.delete();
  }

  public void initialize(Composite composite0) {
    if (form != null) {      
      return;
    }

    GridData gridData;
    form = new SashForm(composite0,SWT.VERTICAL);
    gridData = new GridData(GridData.FILL_BOTH); 
    form.setLayoutData(gridData);
    
    Composite child1 = new Composite(form,SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    child1.setLayout(layout);
    torrentview = new MyTorrentsView(azureus_core, false, tableIncompleteItems);
    torrentview.initialize(child1);
    child1.addListener(SWT.Resize, new Listener() {
      public void handleEvent(Event e) {
        int[] weights = form.getWeights();
        int iSashValue = weights[0] * 100 / (weights[0] + weights[1]);
        COConfigurationManager.setParameter("MyTorrents.SplitAt", iSashValue);
      }
    });

    Composite child2 = new Composite(form,SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 1;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    child2.setLayout(layout);
    seedingview = new MyTorrentsView(azureus_core, true, tableCompleteItems);
    seedingview.initialize(child2);
    int weight = COConfigurationManager.getIntParameter("MyTorrents.SplitAt", 30);
    if (weight > 100)
      weight = 100;
    form.setWeights(new int[] {weight,100 - weight});
  }

  public void refresh() {
    if (getComposite() == null || getComposite().isDisposed())
      return;

    seedingview.refresh();
    torrentview.refresh();
  }

  public String getFullTitle() {
    return MessageText.getString("MyTorrentsView.mytorrents");
  }
  
  // XXX: Is there an easier way to find out what has the focus?
  private IView getCurrentView() {
    // wrap in a try, since the controls may be disposed
    try {
      if (torrentview.getTable().isFocusControl())
        return torrentview;
      else if (seedingview.getTable().isFocusControl())
        return seedingview;
    } catch (Exception ignore) {/*ignore*/}

    return null;
  }

  public boolean isEnabled(String itemKey) {
    IView currentView = getCurrentView();
    if (currentView != null)
      return currentView.isEnabled(itemKey);
    else
      return false;
  }
  
  public void itemActivated(String itemKey) {
    IView currentView = getCurrentView();
    if (currentView != null)
      currentView.itemActivated(itemKey);    
  }
  
  public void removeDownloadBar(DownloadManager manager) {
   torrentview.removeDownloadBar(manager);
   seedingview.removeDownloadBar(manager);
  }

  public void
  generateDiagnostics(
	IndentWriter	writer )
  {
	  super.generateDiagnostics( writer );

	  try{
		  writer.indent();
	  
		  writer.println( "Downloading" );
		  
		  writer.indent();

		  torrentview.generateDiagnostics( writer );
	  
	  }finally{
		  
		  writer.exdent();
		  
		  writer.exdent();
	  }
	  
	  try{
		  writer.indent();
	  
		  writer.println( "Seeding" );
		  
		  writer.indent();

		  seedingview.generateDiagnostics( writer );
	  
	  }finally{
		  
		  writer.exdent();

		  writer.exdent();
	  }
  }
}