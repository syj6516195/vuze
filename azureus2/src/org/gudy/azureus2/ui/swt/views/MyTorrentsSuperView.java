/*
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.views.table.TableColumnCore;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;
import org.gudy.azureus2.ui.swt.views.tableitems.mytorrents.*;

import com.aelitis.azureus.core.AzureusCore;

import org.gudy.azureus2.plugins.ui.tables.TableManager;

/**
 * @author MjrTom
 *			2005/Dec/08: Avg Avail Item
 */

public class MyTorrentsSuperView extends AbstractIView implements
		ObfusticateImage, IViewExtension
{
  private AzureusCore	azureus_core;
  
  private MyTorrentsView torrentview;
  private MyTorrentsView seedingview;
  private SashForm form;

  final static TableColumnCore[] tableIncompleteItems = {
    new HealthItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new RankItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new SendToItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
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
    new CommentItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new MaxUploadsItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new TotalSpeedItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new FilesDoneItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new SavePathItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new TorrentPathItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new CategoryItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new NetworksItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new PeerSourcesItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new AvailabilityItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new AvgAvailItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new SecondsSeedingItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new SecondsDownloadingItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new TimeSinceDownloadItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new TimeSinceUploadItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new OnlyCDing4Item(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new TrackerNextAccessItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new TrackerNameItem( TableManager.TABLE_MYTORRENTS_INCOMPLETE ),
    new SeedToPeerRatioItem( TableManager.TABLE_MYTORRENTS_INCOMPLETE ),
    new DownSpeedLimitItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new SwarmAverageSpeed( TableManager.TABLE_MYTORRENTS_INCOMPLETE ),
    new SwarmAverageCompletion( TableManager.TABLE_MYTORRENTS_INCOMPLETE ),
    new DateAddedItem( TableManager.TABLE_MYTORRENTS_INCOMPLETE ),
  };

  final static TableColumnCore[] tableCompleteItems = {
    new HealthItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new RankItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new SendToItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
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
    new CommentItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE),
    new MaxUploadsItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new TotalSpeedItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new FilesDoneItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new SavePathItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new TorrentPathItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new CategoryItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new NetworksItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new PeerSourcesItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new AvailabilityItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new AvgAvailItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new SecondsSeedingItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new SecondsDownloadingItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new TimeSinceUploadItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new OnlyCDing4Item(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new TrackerStatusItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new TrackerNextAccessItem(TableManager.TABLE_MYTORRENTS_COMPLETE),
    new TrackerNameItem( TableManager.TABLE_MYTORRENTS_COMPLETE ),
    new SeedToPeerRatioItem( TableManager.TABLE_MYTORRENTS_COMPLETE ),
    new SwarmAverageSpeed( TableManager.TABLE_MYTORRENTS_COMPLETE ),
    new SwarmAverageCompletion( TableManager.TABLE_MYTORRENTS_COMPLETE ),
    new DateAddedItem( TableManager.TABLE_MYTORRENTS_COMPLETE ),
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
    form.SASH_WIDTH = 5;
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
        int iSashValue = weights[0] * 10000 / (weights[0] + weights[1]);
        if (iSashValue < 100) {
        	iSashValue = 100;
        }
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
    // More precision, times by 100
    int weight = (int) (COConfigurationManager.getFloatParameter("MyTorrents.SplitAt"));
		if (weight > 10000) {
			weight = 10000;
		} else if (weight < 100) {
			weight *= 100;
		}
		
		// Min/max of 5%/95%
		if (weight < 500) {
			weight = 500;
		} else if (weight > 9000) {
			weight = 9000;
		}
    form.setWeights(new int[] {weight,10000 - weight});
  }

  public void refresh() {
    if (getComposite() == null || getComposite().isDisposed())
      return;

    seedingview.refresh();
    torrentview.refresh();
  }

  public void updateLanguage() {
  	// no super call, the views will do their own
  	
    if (getComposite() == null || getComposite().isDisposed())
      return;

    seedingview.updateLanguage();
    torrentview.updateLanguage();
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

  // IconBarEnabler
  public boolean isEnabled(String itemKey) {
    IView currentView = getCurrentView();
    if (currentView != null)
      return currentView.isEnabled(itemKey);
    else
      return false;
  }
  
  // IconBarEnabler
  public void itemActivated(String itemKey) {
    IView currentView = getCurrentView();
    if (currentView != null)
      currentView.itemActivated(itemKey);    
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

	public Image obfusticatedImage(Image image, Point shellOffset) {
		torrentview.obfusticatedImage(image, shellOffset);
		seedingview.obfusticatedImage(image, shellOffset);
		return image;
	}

	public Menu getPrivateMenu() {
		return null;
	}

	public void viewActivated() {
    IView currentView = getCurrentView();
    if (currentView instanceof IViewExtension) {
    	((IViewExtension)currentView).viewActivated();
    }
	}

	public void viewDeactivated() {
    IView currentView = getCurrentView();
    if (currentView instanceof IViewExtension) {
    	((IViewExtension)currentView).viewDeactivated();
    }
	}
}