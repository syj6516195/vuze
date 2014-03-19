/**
 * Created on Nov 15, 2010
 *
 * Copyright 2010 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package org.gudy.azureus2.ui.swt.views.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.core3.util.TrackersUtil;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.menus.MenuManager;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.utils.FormattersImpl;
import org.gudy.azureus2.ui.swt.MenuBuildUtils;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.maketorrent.MultiTrackerEditor;
import org.gudy.azureus2.ui.swt.maketorrent.TrackerEditorListener;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.ViewUtils;
import org.gudy.azureus2.ui.swt.views.ViewUtils.SpeedAdapter;
import org.gudy.azureus2.ui.swt.views.stats.StatsView;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.tag.TagDownload;
import com.aelitis.azureus.core.tag.TagException;
import com.aelitis.azureus.core.tag.TagFeature;
import com.aelitis.azureus.core.tag.TagFeatureFileLocation;
import com.aelitis.azureus.core.tag.TagFeatureProperties;
import com.aelitis.azureus.core.tag.TagFeatureProperties.TagProperty;
import com.aelitis.azureus.core.tag.TagFeatureRSSFeed;
import com.aelitis.azureus.core.tag.TagFeatureRateLimit;
import com.aelitis.azureus.core.tag.TagFeatureRunState;
import com.aelitis.azureus.core.tag.TagFeatureTranscode;
import com.aelitis.azureus.core.tag.TagManager;
import com.aelitis.azureus.core.tag.TagManagerFactory;
import com.aelitis.azureus.core.tag.TagType;
import com.aelitis.azureus.core.tag.Taggable;
import com.aelitis.azureus.core.util.AZ3Functions;
import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddy;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UIFunctionsUserPrompter;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;

/**
 * @author TuxPaper
 * @created Nov 15, 2010
 *
 */
public class TagUIUtils
{
	public static final int MAX_TOP_LEVEL_TAGS_IN_MENU	= 20;
	
	public static void
	setupSideBarMenus(
		final MenuManager	menuManager )
	{
		org.gudy.azureus2.plugins.ui.menus.MenuItem menuItem = menuManager.addMenuItem("sidebar."
				+ MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS,
				"ConfigView.section.style.TagInSidebar");
		
		menuItem.setStyle(org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_CHECK);
		
		menuItem.addListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemListener() {
			public void selected(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object target) {
				boolean b = COConfigurationManager.getBooleanParameter("Library.TagInSideBar");
				COConfigurationManager.setParameter("Library.TagInSideBar", !b);
			}
		});
		
		menuItem.addFillListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener() {
			public void menuWillBeShown(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object data) {
				menu.setData(Boolean.valueOf(COConfigurationManager.getBooleanParameter("Library.TagInSideBar")));
			}
		});
		
			// tag options
		
		menuItem = menuManager.addMenuItem("sidebar."
				+ MultipleDocumentInterface.SIDEBAR_HEADER_TRANSFERS,
				"label.tags");
		
		menuItem.setStyle(org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_MENU);
		
		menuItem.addFillListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener() {
			public void menuWillBeShown(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object data) {
				menu.removeAllChildItems();
				
				
					// manual		
				
				final TagType manual_tt = TagManagerFactory.getTagManager().getTagType( TagType.TT_DOWNLOAD_MANUAL );
				
				org.gudy.azureus2.plugins.ui.menus.MenuItem menuItem = menuManager.addMenuItem( menu, manual_tt.getTagTypeName( false ));
				
				menuItem.setStyle( org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_MENU );
				
				menuItem.addFillListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener() {
					public void menuWillBeShown(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object data) {
						menu.removeAllChildItems();
						
						final List<Tag> all_tags = manual_tt.getTags();

						List<String>	menu_names 		= new ArrayList<String>();
						Map<String,Tag>	menu_name_map 	= new IdentityHashMap<String, Tag>();

						boolean	all_visible 	= true;
						boolean all_invisible 	= true;

						boolean	has_ut	= false;
						
						for ( Tag t: all_tags ){
								
							String name = t.getTagName( true );
							
							menu_names.add( name );
							menu_name_map.put( name, t );
							
							if ( t.isVisible()){
								all_invisible = false;
							}else{
								all_visible = false;
							}
							
							TagFeatureProperties props = (TagFeatureProperties)t;
							
							TagProperty prop = props.getProperty( TagFeatureProperties.PR_UNTAGGED );
									
							if ( prop != null ){
								
								Boolean b = prop.getBoolean();
								
								if ( b != null && b ){
									
									has_ut = true;
								}
							}
						}
						
						org.gudy.azureus2.plugins.ui.menus.MenuItem showAllItem = menuManager.addMenuItem( menu, "label.show.all" );
						showAllItem.setStyle(org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_PUSH );
						
						showAllItem.addListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemListener() {
							public void selected(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object target) {
								for ( Tag t: all_tags ){
									t.setVisible( true );
								}
							}
						});
						
						org.gudy.azureus2.plugins.ui.menus.MenuItem hideAllItem = menuManager.addMenuItem( menu, "popup.error.hideall" );
						hideAllItem.setStyle(org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_PUSH );
						
						hideAllItem.addListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemListener() {
							public void selected(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object target) {
								for ( Tag t: all_tags ){
									t.setVisible( false );
								}
							}
						});
						
						org.gudy.azureus2.plugins.ui.menus.MenuItem sepItem = menuManager.addMenuItem( menu, "sepm" );
						
						sepItem.setStyle(org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_SEPARATOR );
						
						showAllItem.setEnabled( !all_visible );
						hideAllItem.setEnabled( !all_invisible );					
							
						List<Object>	menu_structure = MenuBuildUtils.splitLongMenuListIntoHierarchy( menu_names, TagUIUtils.MAX_TOP_LEVEL_TAGS_IN_MENU );
						
						for ( Object obj: menu_structure ){

							List<Tag>	bucket_tags = new ArrayList<Tag>();
							
							org.gudy.azureus2.plugins.ui.menus.MenuItem parent_menu;
							
							if ( obj instanceof String ){
								
								parent_menu = menu;
								
								bucket_tags.add( menu_name_map.get((String)obj));
								
							}else{
								
								Object[]	entry = (Object[])obj;
								
								List<String>	tag_names = (List<String>)entry[1];
								
								boolean	sub_all_visible 	= true;
								boolean sub_some_visible	= false;
								
								for ( String name: tag_names ){
									
									Tag tag = menu_name_map.get( name );
									
									if ( tag.isVisible()){
										
										sub_some_visible = true;
										
									}else{
										
										sub_all_visible = false;
									}
									
									bucket_tags.add( tag );
								}
								
								String mod;
								
								if ( sub_all_visible ){
									
									mod = " (*)";
									
								}else if ( sub_some_visible ){
									
									mod = " (+)";
									
								}else{
									
									mod = "";
								}
								
								parent_menu = menuManager.addMenuItem (menu, "!" + (String)entry[0] + mod + "!" );
								
								parent_menu.setStyle( org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_MENU );


							}
							
							for ( final Tag tag: bucket_tags ){
							
								org.gudy.azureus2.plugins.ui.menus.MenuItem m = menuManager.addMenuItem( parent_menu, tag.getTagName( false ));
										
								m.setStyle( org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_CHECK );
															
								m.setData( new Boolean( tag.isVisible()));
										
								m.addListener(
									new MenuItemListener() 
									{
										public void
										selected(
											org.gudy.azureus2.plugins.ui.menus.MenuItem			menu,
											Object 												target )
										{
											tag.setVisible( !tag.isVisible());
										}
									});
							}
						}
						
						if ( !has_ut ){
							
							sepItem = menuManager.addMenuItem( menu, "sepu" );

							sepItem.setStyle(org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_SEPARATOR );
							
							
							org.gudy.azureus2.plugins.ui.menus.MenuItem m = menuManager.addMenuItem( menu, "label.untagged" );
							
							m.setStyle( org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_PUSH );
																							
							m.addListener(
								new MenuItemListener() 
								{
									public void
									selected(
										org.gudy.azureus2.plugins.ui.menus.MenuItem			menu,
										Object 												target )
									{						
										try{
											String tag_name = MessageText.getString( "label.untagged" );
											
											Tag ut_tag = manual_tt.getTag( tag_name, true );
											
											if ( ut_tag == null ){
												
											
												ut_tag = manual_tt.createTag( tag_name, true );
											}
											
											TagFeatureProperties tp = (TagFeatureProperties)ut_tag;
											
											tp.getProperty( TagFeatureProperties.PR_UNTAGGED ).setBoolean( true );
											
										}catch( TagException e ){
												
											Debug.out( e );
										}
									}
								});
						}
					}
				});
				
				menuItem = menuManager.addMenuItem( menu, "label.add.tag");
				
				menuItem.addListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemListener() {
					public void selected(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object target) {
						createManualTag();
					}
				});
				
				org.gudy.azureus2.plugins.ui.menus.MenuItem sepItem = menuManager.addMenuItem( menu, "sep1" );
				
				sepItem.setStyle(org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_SEPARATOR );

								
					// auto
				
				menuItem = menuManager.addMenuItem( menu, "wizard.maketorrent.auto" );
				
				menuItem.setStyle( org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_MENU );
				
				menuItem.addFillListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener() {
					public void menuWillBeShown(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object data) {
						menu.removeAllChildItems();
						
							// content 
						
						org.gudy.azureus2.plugins.ui.menus.MenuItem menuItem = menuManager.addMenuItem( menu, "label.content" );

						menuItem.setStyle(org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_MENU);
						
						menuItem.addFillListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener() {
							public void menuWillBeShown(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object data) {
								menu.removeAllChildItems();

								String[]	tag_ids = { "tag.type.man.vhdn", "tag.type.man.featcon" };

								for ( String id: tag_ids ){
									
									final String c_id = id + ".enabled";
									
									org.gudy.azureus2.plugins.ui.menus.MenuItem menuItem = menuManager.addMenuItem( menu, id);

									menuItem.setStyle(org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_CHECK );
									
									menuItem.addListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemListener() {
										public void selected(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object target) {
											COConfigurationManager.setParameter( c_id, menu.isSelected());
										}
									});
									menuItem.addFillListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener() {
										public void menuWillBeShown(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object data) {
											menu.setData( COConfigurationManager.getBooleanParameter( c_id, true ));
										}
									});
								}
							}});
								
						
							// autos
								
						
						List<TagType> tag_types = TagManagerFactory.getTagManager().getTagTypes();
						
						for ( final TagType tag_type: tag_types ){
							
							if ( tag_type.getTagType() == TagType.TT_DOWNLOAD_CATEGORY ){
								
								continue;
							}
							
							if ( !tag_type.isTagTypeAuto()){
								
								continue;
							}
							
							if ( tag_type.getTags().size() == 0 ){
								
								continue;
							}
							
							menuItem = menuManager.addMenuItem( menu, tag_type.getTagTypeName( false ));

							menuItem.setStyle(org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_MENU);
							
							menuItem.addFillListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener() {
								public void menuWillBeShown(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object data) {
									menu.removeAllChildItems();

									final List<Tag> tags = tag_type.getTags();

									org.gudy.azureus2.plugins.ui.menus.MenuItem showAllItem = menuManager.addMenuItem( menu, "label.show.all" );
									showAllItem.setStyle(org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_PUSH );
									
									showAllItem.addListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemListener() {
										public void selected(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object target) {
											for ( Tag t: tags ){
												t.setVisible( true );
											}
										}
									});
									
									org.gudy.azureus2.plugins.ui.menus.MenuItem hideAllItem = menuManager.addMenuItem( menu, "popup.error.hideall" );
									hideAllItem.setStyle(org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_PUSH );
									
									hideAllItem.addListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemListener() {
										public void selected(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object target) {
											for ( Tag t: tags ){
												t.setVisible( false );
											}
										}
									});
									
									boolean	all_visible 	= true;
									boolean all_invisible 	= true;
									
									for ( Tag t: tags ){
										if ( t.isVisible()){
											all_invisible = false;
										}else{
											all_visible = false;
										}
									}
									
									showAllItem.setEnabled( !all_visible );
									hideAllItem.setEnabled( !all_invisible );
									
									org.gudy.azureus2.plugins.ui.menus.MenuItem sepItem = menuManager.addMenuItem( menu, "sep2" );
									
									sepItem.setStyle(org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_SEPARATOR );
																		
									for ( final Tag t: tags ){
										
										org.gudy.azureus2.plugins.ui.menus.MenuItem  menuItem = menuManager.addMenuItem( menu, t.getTagName( false ));
										
										menuItem.setStyle(org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_CHECK );
										
										menuItem.addListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemListener() {
											public void selected(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object target) {
												t.setVisible( menu.isSelected());
											}
										});
										menuItem.addFillListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemFillListener() {
											public void menuWillBeShown(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object data) {
												menu.setData( t.isVisible());
											}
										});
									}
								}
							});
						}
					}
				});
				
				sepItem = menuManager.addMenuItem( menu, "sep3" );
				
				sepItem.setStyle(org.gudy.azureus2.plugins.ui.menus.MenuItem.STYLE_SEPARATOR );

				
				menuItem = menuManager.addMenuItem( menu, "tag.show.stats");
				
				menuItem.addListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemListener() {
					public void selected(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object target) {
						UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
						uiFunctions.getMDI().loadEntryByID(StatsView.VIEW_ID, true, false, "TagStatsView");

					}
				});
				
				menuItem = menuManager.addMenuItem( menu, "tag.show.overview");
				
				menuItem.addListener(new org.gudy.azureus2.plugins.ui.menus.MenuItemListener() {
					public void selected(org.gudy.azureus2.plugins.ui.menus.MenuItem menu, Object target) {
						UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
						uiFunctions.openView( UIFunctions.VIEW_TAGS_OVERVIEW, null);

					}
				});
			}
		});
		
		AzureusCoreFactory.addCoreRunningListener(
			new AzureusCoreRunningListener()
			{
				public void 
				azureusCoreRunning(
					AzureusCore core) 
				{
					checkTagSharing( true );
				}
			});

	}
	
	public static void
	checkTagSharing(
		boolean		start_of_day )
	{
		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		
		if ( uiFunctions != null ){
			
			TagManager tm = TagManagerFactory.getTagManager();

			if ( start_of_day ){
				
				if ( COConfigurationManager.getBooleanParameter( "tag.sharing.default.checked", false )){
					
					return;
				}
				
				COConfigurationManager.setParameter( "tag.sharing.default.checked", true );
				
				List<TagType> tag_types = tm.getTagTypes();
				
				boolean	prompt_required = false;
				
				for ( TagType tag_type: tag_types ){
					
					List<Tag> tags = tag_type.getTags();
					
					for ( Tag tag: tags ){
						
						if ( tag.isPublic()){
							
							prompt_required = true;
						}
					}
				}
				
				if ( !prompt_required ){
					
					return;
				}
			}
		
			String title = MessageText.getString("tag.sharing.enable.title");
			
			String text = MessageText.getString("tag.sharing.enable.text" );
			
			UIFunctionsUserPrompter prompter = uiFunctions.getUserPrompter(title, text, new String[] {
				MessageText.getString("Button.yes"),
				MessageText.getString("Button.no")
			}, 0);
			
			prompter.setRemember( "tag.share.default", true,
					MessageText.getString("MessageBoxWindow.nomoreprompting"));
			
			prompter.setAutoCloseInMS(0);
			
			prompter.open(null);
			
			boolean	share = prompter.waitUntilClosed() == 0;
			
			tm.setTagPublicDefault( share );
		}
	}
	
	public static Tag
	createManualTag()
	{
		SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
				"TagAddWindow.title", "TagAddWindow.message");
		
		entryWindow.prompt();
		
		if (entryWindow.hasSubmittedInput()) {
			String tag_name = entryWindow.getSubmittedInput().trim();
			TagType tt = TagManagerFactory.getTagManager().getTagType( TagType.TT_DOWNLOAD_MANUAL );
			
			Tag existing = tt.getTag( tag_name, true );
			
			if ( existing == null ){
				
				try{
					checkTagSharing( false );
					
					return( tt.createTag( tag_name, true ));
					
				}catch( TagException e ){
					
					Debug.out( e );
				}
			}
		}
		
		return( null );
	}
	
	public static void 
	createSideBarMenuItems(
		final Menu menu, final Tag tag ) 
	{
	    int userMode = COConfigurationManager.getIntParameter("User Mode");

		final TagType	tag_type = tag.getTagType();
		
		boolean	needs_separator_next = false;
		
		if ( tag_type.hasTagTypeFeature( TagFeature.TF_RATE_LIMIT )) {

			final TagFeatureRateLimit	tf_rate_limit = (TagFeatureRateLimit)tag;
			
			boolean	has_up 		= tf_rate_limit.supportsTagUploadLimit();
			boolean	has_down 	= tf_rate_limit.supportsTagDownloadLimit();
			
			if ( has_up || has_down ){
				
				needs_separator_next = true;
				
				long maxDownload = COConfigurationManager.getIntParameter(
						"Max Download Speed KBs", 0) * 1024L;
				long maxUpload = COConfigurationManager.getIntParameter(
						"Max Upload Speed KBs", 0) * 1024L;
	
				int down_speed 	= tf_rate_limit.getTagDownloadLimit();
				int up_speed 	= tf_rate_limit.getTagUploadLimit();
	
				ViewUtils.addSpeedMenu(menu.getShell(), menu, has_up, has_down, true, true, false,
						down_speed == 0, down_speed, down_speed, maxDownload, false,
						up_speed == 0, up_speed, up_speed, maxUpload, 1, new SpeedAdapter() {
							public void setDownSpeed(int val) {
								tf_rate_limit.setTagDownloadLimit(val);
							}
	
							public void setUpSpeed(int val) {
								tf_rate_limit.setTagUploadLimit(val);
							}
						});
			}
			
		    if ( userMode > 0 ){
		    	
				if ( tf_rate_limit.getTagUploadPriority() >= 0 ){
					
					needs_separator_next = true;
					
					final MenuItem upPriority = new MenuItem(menu, SWT.CHECK );
				
					upPriority.setSelection( tf_rate_limit.getTagUploadPriority() > 0 );
						
					Messages.setLanguageText(upPriority, "cat.upload.priority");
					upPriority.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							boolean set = upPriority.getSelection();
							tf_rate_limit.setTagUploadPriority( set?1:0 );
						}
					});
				}
				
				if ( tf_rate_limit.getTagMinShareRatio() >= 0 ){
					
					needs_separator_next = true;
		
					MenuItem itemSR = new MenuItem(menu, SWT.PUSH);
					
					final String existing = String.valueOf( tf_rate_limit.getTagMinShareRatio()/1000.0f);
	
					Messages.setLanguageText(itemSR, "menu.min.share.ratio", new String[]{existing} );
					
					itemSR.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
									"min.sr.window.title", "min.sr.window.message");
													
							entryWindow.setPreenteredText( existing, false );
							entryWindow.selectPreenteredText( true );
							
							entryWindow.prompt();
							
							if ( entryWindow.hasSubmittedInput()){
								
								try{
									String text = entryWindow.getSubmittedInput().trim();
									
									int	sr = 0;
									
									if ( text.length() > 0 ){
									
										try{
											float f = Float.parseFloat( text );
										
											sr = (int)(f * 1000 );
										
											if ( sr < 0 ){
												
												sr = 0;
												
											}else if ( sr == 0 && f > 0 ){
												
												sr = 1;
											}
										
										}catch( Throwable e ){
											
											Debug.out( e );
										}
										
										tf_rate_limit.setTagMinShareRatio( sr );
										
									}			
								}catch( Throwable e ){
									
									Debug.out( e );
								}
							}
						}
					});
				}
				
				if ( tf_rate_limit.getTagMaxShareRatio() >= 0 ){
					
					needs_separator_next = true;
		
					MenuItem itemSR = new MenuItem(menu, SWT.PUSH);
					
					final String existing = String.valueOf( tf_rate_limit.getTagMaxShareRatio()/1000.0f);
	
					Messages.setLanguageText(itemSR, "menu.max.share.ratio", new String[]{existing} );
					
					itemSR.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
									"max.sr.window.title", "max.sr.window.message");
													
							entryWindow.setPreenteredText( existing, false );
							entryWindow.selectPreenteredText( true );
							
							entryWindow.prompt();
							
							if ( entryWindow.hasSubmittedInput()){
								
								try{
									String text = entryWindow.getSubmittedInput().trim();
									
									int	sr = 0;
									
									if ( text.length() > 0 ){
									
										try{
											float f = Float.parseFloat( text );
										
											sr = (int)(f * 1000 );
										
											if ( sr < 0 ){
												
												sr = 0;
												
											}else if ( sr == 0 && f > 0 ){
												
												sr = 1;
											}
										
										}catch( Throwable e ){
											
											Debug.out( e );
										}
										
										tf_rate_limit.setTagMaxShareRatio( sr );
										
									}			
								}catch( Throwable e ){
									
									Debug.out( e );
								}
							}
						}
					});
				}
			}
		}
		
		if ( tag_type.hasTagTypeFeature( TagFeature.TF_RUN_STATE )) {

			final TagFeatureRunState	tf_run_state = (TagFeatureRunState)tag;

			int caps = tf_run_state.getRunStateCapabilities();
			
			int[] op_set = { 
					TagFeatureRunState.RSC_START, TagFeatureRunState.RSC_STOP,
					TagFeatureRunState.RSC_PAUSE, TagFeatureRunState.RSC_RESUME };
			
			boolean[] can_ops_set = tf_run_state.getPerformableOperations( op_set );
			
			if ((caps & TagFeatureRunState.RSC_START ) != 0 ){
				
				needs_separator_next = true;
				
				final MenuItem itemOp = new MenuItem(menu, SWT.PUSH);
				Messages.setLanguageText(itemOp, "MyTorrentsView.menu.queue");
				Utils.setMenuItemImage(itemOp, "start");
				itemOp.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						tf_run_state.performOperation( TagFeatureRunState.RSC_START );
					}
				});
				itemOp.setEnabled(can_ops_set[0]);
			}
			
			if ((caps & TagFeatureRunState.RSC_STOP ) != 0 ){
				
				needs_separator_next = true;
				
				final MenuItem itemOp = new MenuItem(menu, SWT.PUSH);
				Messages.setLanguageText(itemOp, "MyTorrentsView.menu.stop");
				Utils.setMenuItemImage(itemOp, "stop");
				itemOp.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						tf_run_state.performOperation( TagFeatureRunState.RSC_STOP );
					}
				});
				itemOp.setEnabled(can_ops_set[1]);
			}
			
			if ((caps & TagFeatureRunState.RSC_PAUSE ) != 0 ){
				
				needs_separator_next = true;
				
				final MenuItem itemOp = new MenuItem(menu, SWT.PUSH);
				Messages.setLanguageText(itemOp, "v3.MainWindow.button.pause");
				Utils.setMenuItemImage(itemOp, "pause");
				itemOp.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						tf_run_state.performOperation( TagFeatureRunState.RSC_PAUSE );
					}
				});
				itemOp.setEnabled(can_ops_set[2]);
			}
			
			if ((caps & TagFeatureRunState.RSC_RESUME ) != 0 ){
				
				needs_separator_next = true;
				
				final MenuItem itemOp = new MenuItem(menu, SWT.PUSH);
				Messages.setLanguageText(itemOp, "v3.MainWindow.button.resume");
				Utils.setMenuItemImage(itemOp, "start");
				itemOp.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						tf_run_state.performOperation( TagFeatureRunState.RSC_RESUME );
					}
				});
				itemOp.setEnabled(can_ops_set[3]);
			}
		}
		
		if ( tag_type.hasTagTypeFeature( TagFeature.TF_FILE_LOCATION )) {
		
			final TagFeatureFileLocation fl = (TagFeatureFileLocation)tag;
			
			if ( fl.supportsTagInitialSaveFolder() || fl.supportsTagMoveOnComplete() || fl.supportsTagCopyOnComplete()){
				
				needs_separator_next = true;
				
				Menu files_menu = new Menu( menu.getShell(), SWT.DROP_DOWN);
				
				MenuItem files_item = new MenuItem( menu, SWT.CASCADE);
				
				Messages.setLanguageText( files_item, "ConfigView.section.files" );
				
				files_item.setMenu( files_menu );

				if ( fl.supportsTagInitialSaveFolder()){

					final Menu moc_menu = new Menu( files_menu.getShell(), SWT.DROP_DOWN);
					
					MenuItem isl_item = new MenuItem( files_menu, SWT.CASCADE);
					
					Messages.setLanguageText( isl_item, "label.init.save.loc" );
					
					isl_item.setMenu( moc_menu );
	
					MenuItem clear_item = new MenuItem( moc_menu, SWT.CASCADE);
					
					Messages.setLanguageText( clear_item, "Button.clear" );
	
					clear_item.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							fl.setTagInitialSaveFolder( null );
						}});
					
					new MenuItem( moc_menu, SWT.SEPARATOR);
	
					File existing = fl.getTagInitialSaveFolder();
					
					if ( existing != null ){
						
						MenuItem current_item = new MenuItem( moc_menu, SWT.RADIO );
						current_item.setSelection( true );
						
						current_item.setText( existing.getAbsolutePath());
						
						new MenuItem( moc_menu, SWT.SEPARATOR);
						
					}else{
						
						clear_item.setEnabled( false );
					}
					
					MenuItem set_item = new MenuItem( moc_menu, SWT.CASCADE);
					
					Messages.setLanguageText( set_item, "label.set" );
	
					set_item.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event){
							DirectoryDialog dd = new DirectoryDialog(moc_menu.getShell());
	
							dd.setFilterPath( TorrentOpener.getFilterPathData());
	
							dd.setText(MessageText.getString("MyTorrentsView.menu.movedata.dialog"));
	
							String path = dd.open();
	
							if ( path != null ){
								
								TorrentOpener.setFilterPathData( path );
								
								fl.setTagInitialSaveFolder( new File( path ));
							}
						}});
				}
			
				if ( fl.supportsTagMoveOnComplete()){
					
					final Menu moc_menu = new Menu( files_menu.getShell(), SWT.DROP_DOWN);
					
					MenuItem moc_item = new MenuItem( files_menu, SWT.CASCADE);
					
					Messages.setLanguageText( moc_item, "label.move.on.comp" );
					
					moc_item.setMenu( moc_menu );
	
					MenuItem clear_item = new MenuItem( moc_menu, SWT.CASCADE);
					
					Messages.setLanguageText( clear_item, "Button.clear" );
	
					clear_item.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							fl.setTagMoveOnCompleteFolder( null );
						}});
					
					new MenuItem( moc_menu, SWT.SEPARATOR);
	
					File existing = fl.getTagMoveOnCompleteFolder();
					
					if ( existing != null ){
						
						MenuItem current_item = new MenuItem( moc_menu, SWT.RADIO );
						current_item.setSelection( true );
						
						current_item.setText( existing.getAbsolutePath());
						
						new MenuItem( moc_menu, SWT.SEPARATOR);
						
					}else{
						
						clear_item.setEnabled( false );
					}
					
					MenuItem set_item = new MenuItem( moc_menu, SWT.CASCADE);
					
					Messages.setLanguageText( set_item, "label.set" );
	
					set_item.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event){
							DirectoryDialog dd = new DirectoryDialog(moc_menu.getShell());
	
							dd.setFilterPath( TorrentOpener.getFilterPathData());
	
							dd.setText(MessageText.getString("MyTorrentsView.menu.movedata.dialog"));
	
							String path = dd.open();
	
							if ( path != null ){
								
								TorrentOpener.setFilterPathData( path );
								
								fl.setTagMoveOnCompleteFolder( new File( path ));
							}
						}});
				}
				
				if ( fl.supportsTagCopyOnComplete()){
					
					final Menu moc_menu = new Menu( files_menu.getShell(), SWT.DROP_DOWN);
					
					MenuItem moc_item = new MenuItem( files_menu, SWT.CASCADE);
					
					Messages.setLanguageText( moc_item, "label.copy.on.comp" );
					
					moc_item.setMenu( moc_menu );
	
					MenuItem clear_item = new MenuItem( moc_menu, SWT.CASCADE);
					
					Messages.setLanguageText( clear_item, "Button.clear" );
	
					clear_item.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							fl.setTagCopyOnCompleteFolder( null );
						}});
					
					new MenuItem( moc_menu, SWT.SEPARATOR);
	
					File existing = fl.getTagCopyOnCompleteFolder();
					
					if ( existing != null ){
						
						MenuItem current_item = new MenuItem( moc_menu, SWT.RADIO );
						current_item.setSelection( true );
						
						current_item.setText( existing.getAbsolutePath());
						
						new MenuItem( moc_menu, SWT.SEPARATOR);
						
					}else{
						
						clear_item.setEnabled( false );
					}
					
					MenuItem set_item = new MenuItem( moc_menu, SWT.CASCADE);
					
					Messages.setLanguageText( set_item, "label.set" );
	
					set_item.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event){
							DirectoryDialog dd = new DirectoryDialog(moc_menu.getShell());
	
							dd.setFilterPath( TorrentOpener.getFilterPathData());
	
							dd.setText(MessageText.getString("MyTorrentsView.menu.movedata.dialog"));
	
							String path = dd.open();
	
							if ( path != null ){
								
								TorrentOpener.setFilterPathData( path );
								
								fl.setTagCopyOnCompleteFolder( new File( path ));
							}
						}});
				}
			}
		}
		
		// options

		if ( tag instanceof TagDownload ){
			
			needs_separator_next = true;
			
			MenuItem itemOptions = new MenuItem(menu, SWT.PUSH);
	
			final Set<DownloadManager> dms = ((TagDownload)tag).getTaggedDownloads();

			Messages.setLanguageText(itemOptions, "cat.options");
			itemOptions.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
	
					uiFunctions.openView(UIFunctions.VIEW_DM_MULTI_OPTIONS, dms.toArray( new DownloadManager[dms.size()]));
				}
			});
	
			if (dms.size() == 0) {
	
				itemOptions.setEnabled(false);
			}
		}

	    if ( userMode > 0 ){

			if ( tag_type.hasTagTypeFeature( TagFeature.TF_PROPERTIES )){
				
				TagFeatureProperties props = (TagFeatureProperties)tag;
				
				TagProperty[] tps = props.getSupportedProperties();
				
				if ( tps.length > 0 ){
					
					needs_separator_next = true;
					
					final Menu props_menu = new Menu( menu.getShell(), SWT.DROP_DOWN);
					
					MenuItem props_item = new MenuItem( menu, SWT.CASCADE);
					
					Messages.setLanguageText( props_item, "label.properties" );
					
					props_item.setMenu( props_menu );
	
					for ( final TagProperty tp: tps ){
											
						if ( tp.getType() == TagFeatureProperties.PT_STRING_LIST ){
							
							String tp_name = tp.getName( false );
							
							if ( tp_name.equals( TagFeatureProperties.PR_CONSTRAINT )){
								
								MenuItem const_item = new MenuItem( props_menu, SWT.PUSH);
								
								Messages.setLanguageText( const_item, "label.contraints" );
								
								const_item.addListener(SWT.Selection, new Listener() {
									public void 
									handleEvent(Event event)
									{
										String[] val = tp.getStringList();
										
										String def_val;
										
										if ( val != null && val.length > 0 ){
											
											def_val = val[0];
											
										}else{
											
											def_val = "";
										}
										
										String msg = MessageText.getString( "UpdateConstraint.message" );
										
										SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow( "UpdateConstraint.title", "!" + msg + "!" );
										
										entryWindow.setPreenteredText( def_val, false );
										entryWindow.selectPreenteredText( true );
										
										entryWindow.prompt();
										
										if ( entryWindow.hasSubmittedInput()){
											
											try{
												String text = entryWindow.getSubmittedInput().trim();
												
												if ( text.length() ==  0 ){
													
													tp.setStringList( null );
													
												}else{
													
													tp.setStringList( new String[]{ text });
												}
											}catch( Throwable e ){
												
												Debug.out( e );
											}
										}
									}});
								
							}else if ( tp_name.equals( TagFeatureProperties.PR_TRACKER_TEMPLATES )){
								
								final TrackersUtil tut = TrackersUtil.getInstance();
								
								List<String> templates = new ArrayList<String>( tut.getMultiTrackers().keySet());

								String str_merge 	= MessageText.getString("label.merge" );
								String str_replace 	= MessageText.getString("label.replace" );
								String str_remove 	= MessageText.getString("Button.remove" );

								String[] val = tp.getStringList();
								
								String def_str;
								
								final List<String> selected = new ArrayList<String>();
								
								if ( val == null || val.length == 0 ){
									
									def_str = "";
									
								}else{
									
									def_str = "";
									
									for ( String v: val ){
										
										String[] bits = v.split( ":" );
										
										if ( bits.length == 2 ){
										
											String tn = bits[1];
											
											if ( templates.contains( tn )){
											
												String type = bits[0];
												
												if ( type.equals( "m" )){
											
													tn += ": " + str_merge;
													
												}else if ( type.equals( "r" )){
												
													tn += ": " + str_replace;
													
												}else{
													tn += ": " + str_remove;
												}
										
												selected.add( v );
												
												def_str += (def_str.length()==0?"":", ") + tn;
											}
										}
									}
								}
																
								Collections.sort( templates );
								
									// deliberately hanging this off the main menu, not properties...
								
								Menu ttemp_menu = new Menu( menu.getShell(), SWT.DROP_DOWN);
								
								MenuItem ttemp_item = new MenuItem( menu, SWT.CASCADE);
								
								ttemp_item.setText( MessageText.getString( "label.tracker.templates" ) + (def_str.length()==0?"":(" (" + def_str + ")  ")));
								
								ttemp_item.setMenu( ttemp_menu );
								
								MenuItem new_item = new MenuItem( ttemp_menu, SWT.PUSH);
								
								Messages.setLanguageText( new_item, "wizard.multitracker.new" );
								
								new_item.addListener(SWT.Selection, new Listener() {
									public void 
									handleEvent(Event event)
									{
										List<List<String>> group = new ArrayList<List<String>>();
										List<String> tracker = new ArrayList<String>();
										group.add(tracker);
										
										new MultiTrackerEditor(
											props_menu.getShell(), 
											null, 
											group, 
											new TrackerEditorListener() {
												
												public void 
												trackersChanged(
													String 				oldName, 
													String 				newName,
													List<List<String>> 	trackers) 
												{
													if ( trackers != null ){
														
														tut.addMultiTracker(newName , trackers );
													}
												}
											});
									}});
									
								MenuItem reapply_item = new MenuItem( ttemp_menu, SWT.PUSH);
								
								Messages.setLanguageText( reapply_item, "label.reapply" );
								
								reapply_item.addListener(SWT.Selection, new Listener() {
									public void 
									handleEvent(Event event)
									{
										tp.syncListeners();
									}});
								
								reapply_item.setEnabled( def_str.length() > 0 );
								
								if ( templates.size() > 0 ){
								
									new MenuItem( ttemp_menu, SWT.SEPARATOR);
																		
									for ( final String template_name: templates ){
										
										Menu t_menu = new Menu( ttemp_menu.getShell(), SWT.DROP_DOWN);
										
										MenuItem t_item = new MenuItem( ttemp_menu, SWT.CASCADE);
										
										t_item.setText( template_name );
										
										t_item.setMenu( t_menu );

										boolean	r_selected = false;

										for ( int i=0;i<3;i++){
											
											final MenuItem sel_item = new MenuItem( t_menu, SWT.CHECK);
											
											final String key = (i==0?"m":(i==1?"r":"x")) + ":" + template_name;
																						
											sel_item.setText( i==0?str_merge:(i==1?str_replace:str_remove));
											
											boolean is_sel = selected.contains( key );
											
											r_selected |= is_sel;
											
											sel_item.setSelection( is_sel );
											
											sel_item.addListener(SWT.Selection, new Listener() {
												public void 
												handleEvent(Event event)
												{
													if ( sel_item.getSelection()){
														
														selected.add( key );
														
													}else{
														
														selected.remove( key );
													}
													
													tp.setStringList( selected.toArray( new String[ selected.size()]));

												}});
										}
										
										if ( r_selected ){
											
											Utils.setMenuItemImage( t_item, "graytick" );
										}
										
										new MenuItem( t_menu, SWT.SEPARATOR);
										
										MenuItem edit_item = new MenuItem( t_menu, SWT.PUSH);
										
										Messages.setLanguageText( edit_item, "wizard.multitracker.edit" );
										
										edit_item.addListener(SWT.Selection, new Listener() {
											public void 
											handleEvent(Event event)
											{
												new MultiTrackerEditor(
													props_menu.getShell(), 
													template_name, 
													tut.getMultiTrackers().get( template_name ), 
													new TrackerEditorListener() {
														
														public void 
														trackersChanged(
															String 				oldName, 
															String 				newName,
															List<List<String>> 	trackers) 
														{
															if  ( oldName != null && !oldName.equals( newName )){
														    	
																tut.removeMultiTracker( oldName );
														    }
																
															tut.addMultiTracker(newName , trackers );
														}
													});
											}});
										
										MenuItem del_item = new MenuItem( t_menu, SWT.PUSH);
										
										Messages.setLanguageText( del_item, "FileItem.delete" );
										
										Utils.setMenuItemImage( del_item, "delete" );

										del_item.addListener(SWT.Selection, new Listener() {
											public void 
											handleEvent(Event event)
											{
												MessageBoxShell mb = 
														new MessageBoxShell(
															MessageText.getString("message.confirm.delete.title"),
															MessageText.getString("message.confirm.delete.text",
																	new String[] { template_name	}), 
															new String[] {
																MessageText.getString("Button.yes"),
																MessageText.getString("Button.no")
															},
															1 );
													
													mb.open(new UserPrompterResultListener() {
														public void prompterClosed(int result) {
															if (result == 0) {
																tut.removeMultiTracker( template_name );
															}
														}
													});
											}});
									}
								}
							}else{
								String[] val = tp.getStringList();
								
								String def_str;
								
								if ( val == null || val.length == 0 ){
									
									def_str = "";
									
								}else{
									
									def_str = "";
									
									for ( String v: val ){
										
										def_str += (def_str.length()==0?"":", ") + v;
									}
								}
								
								MenuItem set_item = new MenuItem( props_menu, SWT.PUSH);
		
								set_item.setText( tp.getName( true ) + (def_str.length()==0?"":(" (" + def_str + ") ")) + "..." );
				
								final String f_def_str = def_str;
								
								set_item.addListener(SWT.Selection, new Listener() {
									public void handleEvent(Event event){
										
										String msg = MessageText.getString( "UpdateProperty.list.message", new String[]{ tp.getName( true ) } );
										
										SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow( "UpdateProperty.title", "!" + msg + "!" );
										
										entryWindow.setPreenteredText( f_def_str, false );
										entryWindow.selectPreenteredText( true );
										
										entryWindow.prompt();
										
										if ( entryWindow.hasSubmittedInput()){
											
											try{
												String text = entryWindow.getSubmittedInput().trim();
												
												if ( text.length() ==  0 ){
													
													tp.setStringList( null );
													
												}else{
													text = text.replace( ';', ',');
													text = text.replace( ' ', ',');
													text = text.replaceAll( "[,]+", "," );
													
													String[] bits = text.split( "," );
													
													List<String> vals = new ArrayList<String>();
													
													for ( String bit: bits ){
														
														bit = bit.trim();
														
														if ( bit.length() > 0 ){
															
															vals.add( bit );
														}
													}
													
													if ( vals.size() == 0 ){
														
														tp.setStringList( null );
													}else{
														
														tp.setStringList( vals.toArray( new String[ vals.size()]));
													}
												}
											}catch( Throwable e ){
												
												Debug.out( e );
											}
										}
									}});
							}
						}else if ( tp.getType() == TagFeatureProperties.PT_BOOLEAN ){
							
							final MenuItem set_item = new MenuItem( props_menu, SWT.CHECK);
	
							set_item.setText( tp.getName( true ));
							
							Boolean val = tp.getBoolean();
							
							set_item.setSelection( val != null && val );
	
							set_item.addListener(
								SWT.Selection, 
								new Listener() 
								{
									public void 
									handleEvent(
										Event event) 
									{
										tp.setBoolean( set_item.getSelection());
									}
								});
							
						}else{
							
							Debug.out( "Unknown property" );
						}
					}
				}		
			}
	    }
	    
		if ( needs_separator_next ){
			
			new MenuItem( menu, SWT.SEPARATOR);
			
			needs_separator_next = false;
		}

			// sharing
		
		if ( tag.canBePublic()){
			
			needs_separator_next = true;
			
			final MenuItem itemPublic = new MenuItem(menu, SWT.CHECK );
			
			itemPublic.setSelection( tag.isPublic());
			
			Messages.setLanguageText(itemPublic, "tag.share");

			itemPublic.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					
					tag.setPublic( itemPublic.getSelection());
				}});
		}
		
		// share with friends

		PluginInterface bpi = PluginInitializer.getDefaultInterface().getPluginManager().getPluginInterfaceByClass(
				BuddyPlugin.class);

		if ( tag_type.getTagType() == TagType.TT_DOWNLOAD_MANUAL && bpi != null ){
				
			TagFeatureProperties props = (TagFeatureProperties)tag;
							
			TagProperty tp = props.getProperty( TagFeatureProperties.PR_UNTAGGED );
			
			Boolean is_ut = tp==null?null:tp.getBoolean();
			
			if ( is_ut == null || !is_ut ){
	
				final BuddyPlugin buddy_plugin = (BuddyPlugin) bpi.getPlugin();
	
				if (buddy_plugin.isEnabled()) {
	
					final Menu share_menu = new Menu(menu.getShell(), SWT.DROP_DOWN);
					final MenuItem share_item = new MenuItem(menu, SWT.CASCADE);
					Messages.setLanguageText(share_item, "azbuddy.ui.menu.cat.share");
					share_item.setText( share_item.getText() + "  " );	// nasty hack to fix nastyness on windows
					share_item.setMenu(share_menu);
	
					List<BuddyPluginBuddy> buddies = buddy_plugin.getBuddies();
	
					if (buddies.size() == 0) {
	
						final MenuItem item = new MenuItem(share_menu, SWT.CHECK);
	
						item.setText(MessageText.getString("general.add.friends"));
	
						item.setEnabled(false);
	
					} else {
						final String tag_name = tag.getTagName( true );
	
						final boolean is_public = buddy_plugin.isPublicTagOrCategory( tag_name );
	
						final MenuItem itemPubCat = new MenuItem(share_menu, SWT.CHECK);
	
						Messages.setLanguageText(itemPubCat, "general.all.friends");
	
						itemPubCat.setSelection(is_public);
	
						itemPubCat.addListener(SWT.Selection, new Listener() {
							public void handleEvent(Event event) {
								if (is_public) {
	
									buddy_plugin.removePublicTagOrCategory( tag_name );
	
								} else {
	
									buddy_plugin.addPublicTagOrCategory( tag_name );
								}
							}
						});
	
						new MenuItem(share_menu, SWT.SEPARATOR);
	
						for (final BuddyPluginBuddy buddy : buddies) {
	
							if (buddy.getNickName() == null) {
	
								continue;
							}
	
							final boolean auth = buddy.isLocalRSSTagOrCategoryAuthorised(tag_name);
	
							final MenuItem itemShare = new MenuItem(share_menu, SWT.CHECK);
	
							itemShare.setText(buddy.getName());
	
							itemShare.setSelection(auth || is_public);
	
							if (is_public) {
	
								itemShare.setEnabled(false);
							}
	
							itemShare.addListener(SWT.Selection, new Listener() {
								public void handleEvent(Event event) {
									if (auth) {
	
										buddy.removeLocalAuthorisedRSSTagOrCategory(tag_name);
	
									} else {
	
										buddy.addLocalAuthorisedRSSTagOrCategory(tag_name);
									}
								}
							});
	
						}
					}
				}
			}
		}
		
		
			// rss feed
		
		if ( tag_type.hasTagTypeFeature( TagFeature.TF_RSS_FEED )) {

			final TagFeatureRSSFeed tfrss = (TagFeatureRSSFeed)tag;
			
			// rss feed
			
			final MenuItem rssOption = new MenuItem(menu, SWT.CHECK );
	
			rssOption.setSelection( tfrss.isTagRSSFeedEnabled());
			
			Messages.setLanguageText(rssOption, "cat.rss.gen");
			rssOption.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					boolean set = rssOption.getSelection();
					tfrss.setTagRSSFeedEnabled( set );
				}
			});
		}
		
		if ( tag_type.hasTagTypeFeature( TagFeature.TF_XCODE )) {

			final TagFeatureTranscode tf_xcode = (TagFeatureTranscode)tag;
			
			if ( tf_xcode.supportsTagTranscode()){
				
				AZ3Functions.provider provider = AZ3Functions.getProvider();
		
				if ( provider != null ){
		
					AZ3Functions.provider.TranscodeTarget[] tts = provider.getTranscodeTargets();
		
					if (tts.length > 0) {
		
						final Menu t_menu = new Menu(menu.getShell(), SWT.DROP_DOWN);
						
						final MenuItem t_item = new MenuItem(menu, SWT.CASCADE);
						
						Messages.setLanguageText( t_item, "cat.autoxcode" );
						
						t_item.setMenu(t_menu);
		
						String[] existing = tf_xcode.getTagTranscodeTarget();
		
						for ( final AZ3Functions.provider.TranscodeTarget tt : tts ){
		
							AZ3Functions.provider.TranscodeProfile[] profiles = tt.getProfiles();
		
							if ( profiles.length > 0 ){
		
								final Menu tt_menu = new Menu(t_menu.getShell(), SWT.DROP_DOWN);
								
								final MenuItem tt_item = new MenuItem(t_menu, SWT.CASCADE);
								
								tt_item.setText(tt.getName());
								
								tt_item.setMenu(tt_menu);
		
								for (final AZ3Functions.provider.TranscodeProfile tp : profiles) {
		
									final MenuItem p_item = new MenuItem(tt_menu, SWT.CHECK);
		
									p_item.setText(tp.getName());
		
									boolean	selected = existing != null	&& existing[0].equals(tp.getUID());
									
									if ( selected ){
										
										Utils.setMenuItemImage(tt_item, "graytick");
									}
									
									p_item.setSelection(selected );
		
									p_item.addListener(SWT.Selection, new Listener(){
										public void handleEvent(Event event) {
											
											String name = tt.getName() + " - " + tp.getName();
											
											if ( p_item.getSelection()){
												
												tf_xcode.setTagTranscodeTarget( tp.getUID(), name );
												
											}else{
												
												tf_xcode.setTagTranscodeTarget( null, null );
											}
										}
									});
								}
								
								new MenuItem(tt_menu, SWT.SEPARATOR );
								
								final MenuItem no_xcode_item = new MenuItem(tt_menu, SWT.CHECK);
								
								final String never_str = MessageText.getString( "v3.menu.device.defaultprofile.never" );
								
								no_xcode_item.setText( never_str );
	
								final String never_uid = tt.getID() + "/blank";
								
								boolean	selected = existing != null	&& existing[0].equals(never_uid);
								
								if ( selected ){
									
									Utils.setMenuItemImage(tt_item, "graytick");
								}
								
								no_xcode_item.setSelection(selected );
								
								no_xcode_item.addListener(SWT.Selection, new Listener(){
									
									public void handleEvent(Event event) {
										
										String name = tt.getName() + " - " + never_str;
										
										if ( no_xcode_item.getSelection()){
											
											tf_xcode.setTagTranscodeTarget( never_uid, name );
											
										}else{
											
											tf_xcode.setTagTranscodeTarget( null, null );
										}
									}
								});
							}
						}
					}
				}
			}
		}
		
		needs_separator_next = true;

		MenuItem itemShowStats = new MenuItem(menu, SWT.PUSH);
		
		Messages.setLanguageText(itemShowStats, "tag.show.stats");
		itemShowStats.addListener(SWT.Selection, new Listener() {
			public void handleEvent( Event event ){
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				uiFunctions.getMDI().loadEntryByID(StatsView.VIEW_ID, true, false, "TagStatsView");
			}
		});
		
		if ( needs_separator_next ){
		
			new MenuItem( menu, SWT.SEPARATOR);
			
			needs_separator_next = false;
		}

		boolean	auto = tag_type.isTagTypeAuto();
		
		boolean	closable = auto;
		
		if ( tag.getTaggableTypes() == Taggable.TT_DOWNLOAD ){
			
			closable = true;	// extended closable tags to include manual ones due to user request
		}
		
		Menu menuShowHide = null;
		
		if ( closable ){
			
			final List<Tag>	tags = tag_type.getTags();
			
			int	visible_count 	= 0;
			int	invisible_count = 0;
			
			for ( Tag t: tags ){
				
				if ( t.isVisible()){
					visible_count++;
				}else{
					invisible_count++;
				}
			}
						
			menuShowHide = new Menu(menu.getShell(), SWT.DROP_DOWN);
			
			final MenuItem showhideitem = new MenuItem(menu, SWT.CASCADE);
			showhideitem.setText( MessageText.getString( "label.showhide.tag" ));
			showhideitem.setMenu(menuShowHide);			
			
			MenuItem title = new MenuItem(menuShowHide, SWT.PUSH);
			title.setText( "[" + tag_type.getTagTypeName( true ) + "]" );
			title.setEnabled( false );
			new MenuItem( menuShowHide, SWT.SEPARATOR);
									
			if ( invisible_count > 0 ){
				MenuItem showAll = new MenuItem(menuShowHide, SWT.PUSH);
				Messages.setLanguageText(showAll, "label.show.all");
				showAll.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event){
						for ( Tag t: tags ){
							
							if ( !t.isVisible()){
								t.setVisible( true );
							}
						}
					}});
				
				needs_separator_next = true;
			}
			
			if ( visible_count > 0 ){
				MenuItem hideAll = new MenuItem(menuShowHide, SWT.PUSH);
				Messages.setLanguageText(hideAll, "popup.error.hideall");
				hideAll.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event){
						for ( Tag t: tags ){
							
							if ( t.isVisible()){
								t.setVisible( false );
							}
						}
					}});
				
				needs_separator_next = true;
			}
			
			if ( tags.size() > 0 ){
				
				if ( needs_separator_next ){
						
					new MenuItem( menuShowHide, SWT.SEPARATOR);
						
					needs_separator_next = false;
				}
				
				List<String>	menu_names 		= new ArrayList<String>();
				Map<String,Tag>	menu_name_map 	= new IdentityHashMap<String, Tag>();

				for ( Tag t: tags ){
											
					String name = t.getTagName( true );
						
					menu_names.add( name );
					menu_name_map.put( name, t );
				}
				
				List<Object>	menu_structure = MenuBuildUtils.splitLongMenuListIntoHierarchy( menu_names, MAX_TOP_LEVEL_TAGS_IN_MENU );
				
				for ( Object obj: menu_structure ){
				
					List<Tag>	bucket_tags = new ArrayList<Tag>();
					
					Menu parent_menu;
					
					if ( obj instanceof String ){
						
						parent_menu = menuShowHide;
						
						bucket_tags.add( menu_name_map.get((String)obj));
						
					}else{
						
						Object[]	entry = (Object[])obj;
												
						List<String>	tag_names = (List<String>)entry[1];
						
						boolean	sub_all_visible 	= true;
						boolean sub_some_visible	= false;
						
						for ( String name: tag_names ){
							
							Tag sub_tag = menu_name_map.get( name );
							
							if ( sub_tag.isVisible()){
								
								sub_some_visible = true;
								
							}else{
								
								sub_all_visible = false;
							}
							
							bucket_tags.add( sub_tag );
						}
						
						String mod;
						
						if ( sub_all_visible ){
							
							mod = " (*)";
							
						}else if ( sub_some_visible ){
							
							mod = " (+)";
							
						}else{
							
							mod = "";
						}
						
						Menu menu_bucket = new Menu( menuShowHide.getShell(), SWT.DROP_DOWN );
						
						MenuItem bucket_item = new MenuItem( menuShowHide, SWT.CASCADE );
						
						bucket_item.setText((String)entry[0] + mod);
						
						bucket_item.setMenu( menu_bucket );		
						
						parent_menu = menu_bucket;
					}
				
					for ( final Tag t: bucket_tags ){
									
						MenuItem showTag = new MenuItem( parent_menu, SWT.CHECK );
						
						showTag.setSelection( t.isVisible());
						
						Messages.setLanguageText(showTag, t.getTagName( false ));
						
						showTag.addListener(SWT.Selection, new Listener() {
							public void handleEvent(Event event){
								t.setVisible( !t.isVisible());
							}});
					}
				}	
			}
			
			showhideitem.setEnabled( true );
		}
		
		if ( !auto ){
			
			if ( tag_type.hasTagTypeFeature( TagFeature.TF_PROPERTIES )){			
				
				TagFeatureProperties props = (TagFeatureProperties)tag;
							
				boolean has_ut = props.getProperty( TagFeatureProperties.PR_UNTAGGED ) != null;
			
				if ( has_ut ){
					
					has_ut = false;
					
					for ( Tag t: tag_type.getTags()){
						
						props = (TagFeatureProperties)t;
						
						TagProperty prop = props.getProperty( TagFeatureProperties.PR_UNTAGGED );
								
						if ( prop != null ){
							
							Boolean b = prop.getBoolean();
							
							if ( b != null && b ){
								
								has_ut = true;
									
								break;
							}
						}
					}
					
					if  ( !has_ut ){
						
						if ( menuShowHide == null ){
							
							menuShowHide = new Menu(menu.getShell(), SWT.DROP_DOWN);
						
							MenuItem showhideitem = new MenuItem(menu, SWT.CASCADE);
							showhideitem.setText( MessageText.getString( "label.showhide.tag" ));
							showhideitem.setMenu(menuShowHide);		
							
						}else{
							
							new MenuItem( menuShowHide, SWT.SEPARATOR );
						}
						
						MenuItem showAll = new MenuItem(menuShowHide, SWT.PUSH);
						Messages.setLanguageText(showAll, "label.untagged");
						showAll.addListener(SWT.Selection, new Listener() {
							public void handleEvent(Event event){
								try{
									String tag_name = MessageText.getString( "label.untagged" );
									
									Tag ut_tag = tag_type.getTag( tag_name, true );
									
									if ( ut_tag == null ){
										
									
										ut_tag = tag_type.createTag( tag_name, true );
									}
									
									TagFeatureProperties tp = (TagFeatureProperties)ut_tag;
									
									tp.getProperty( TagFeatureProperties.PR_UNTAGGED ).setBoolean( true );
									
								}catch( TagException e ){
										
									Debug.out( e );
								}
							}});
					}
				}
			}
			
			MenuItem item_create = new MenuItem( menu, SWT.PUSH);
			
			Messages.setLanguageText(item_create, "label.add.tag");
			item_create.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					
					createManualTag();
				}
			});
			
			MenuItem itemSetColor = new MenuItem(menu, SWT.PUSH);
			
			itemSetColor.setText( MessageText.getString( "label.color") + "..." );
			
			itemSetColor.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					
					int[] existing = tag.getColor();
					
					RGB e_rg = existing==null?null:new RGB(existing[0],existing[1],existing[2]);
					
					RGB rgb = Utils.showColorDialog( menu.getShell(), e_rg );
					
					if ( rgb != null ){
						
						tag.setColor( new int[]{ rgb.red, rgb.green, rgb.blue });
					}
				}
			});
			
			MenuItem itemRename = new MenuItem(menu, SWT.PUSH);
						
			Messages.setLanguageText(itemRename, "MyTorrentsView.menu.rename");
			itemRename.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow(
							"TagRenameWindow.title", "TagRenameWindow.message");
					
					entryWindow.setPreenteredText( tag.getTagName( true ), false );
					entryWindow.selectPreenteredText( true );
					
					entryWindow.prompt();
					
					if ( entryWindow.hasSubmittedInput()){
						
						try{
							tag.setTagName( entryWindow.getSubmittedInput().trim());
							
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				}
			});
			
			MenuItem itemDelete = new MenuItem(menu, SWT.PUSH);
			
			Utils.setMenuItemImage(itemDelete, "delete");
			
			Messages.setLanguageText(itemDelete, "FileItem.delete");
			itemDelete.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					tag.removeTag();
				}
			});
		}
	}
	
	public static void 
	addLibraryViewTagsSubMenu(
		final DownloadManager[] 	dms, 
		Menu 						menu_tags, 
		final Composite 			composite) 
	{
		MenuItem[] items = menu_tags.getItems();
		
		for ( MenuItem item: items ){
			
			item.dispose();
		}
		
		final TagManager tm = TagManagerFactory.getTagManager();
		
		Map<TagType,List<Tag>>	auto_map = new HashMap<TagType, List<Tag>>();
		
		TagType manual_tt = tm.getTagType( TagType.TT_DOWNLOAD_MANUAL );

		Map<Tag,Integer>	manual_map = new HashMap<Tag, Integer>();
		
		for ( DownloadManager dm: dms ){
			
			List<Tag> tags = tm.getTagsForTaggable( dm );
			
			for ( Tag t: tags ){
				
				TagType tt = t.getTagType();
				
				if ( tt == manual_tt ){
					
					Integer i = manual_map.get( t );
					
					manual_map.put( t, i==null?1:i+1 );
					
				}else if ( tt.isTagTypeAuto()){
					
					List<Tag> x = auto_map.get( tt );
					
					if ( x == null ){
						
						x = new ArrayList<Tag>();
						
						auto_map.put( tt, x );
					}
					
					x.add( t );
				}
			}
		}
		
		if ( auto_map.size() > 0 ){
			
			final Menu menuAuto = new Menu(menu_tags.getShell(), SWT.DROP_DOWN);
			final MenuItem autoItem = new MenuItem(menu_tags, SWT.CASCADE);
			Messages.setLanguageText(autoItem, "wizard.maketorrent.auto" );
			autoItem.setMenu(menuAuto);			

			List<TagType>	auto_tags = sortTagTypes( auto_map.keySet());
	
			for ( TagType tt: auto_tags ){
				
				MenuItem tt_i = new MenuItem(menuAuto, Constants.isOSX?SWT.CHECK:SWT.PUSH);
				
				String tt_str = tt.getTagTypeName( true ) + ": ";
				
				List<Tag> tags = auto_map.get( tt );
				
				Map<Tag,Integer>	tag_counts = new HashMap<Tag, Integer>();
				
				for ( Tag t: tags ){
					
					Integer i = tag_counts.get( t );
					
					tag_counts.put( t, i==null?1:i+1 );
				}
				
				tags = sortTags( tag_counts.keySet());
				
				int	 num = 0;
				
				for ( Tag t: tags ){
				
					tt_str += (num==0?"":", " ) + t.getTagName( true );
					
					num++;
					
					if ( dms.length > 1 ){
						
						tt_str += " (" + tag_counts.get( t ) + ")";
					}
				}
				
				tt_i.setText( tt_str );
				if ( Constants.isOSX ){
					tt_i.setSelection(true);
				}else{
					Utils.setMenuItemImage( tt_i, "graytick" );
				}
				
				//tt_i.setEnabled(false);
			}
		}
		
		List<Tag>	manual_t = manual_tt.getTags();
		
		if ( manual_t.size() > 0 ){
			
			if ( auto_map.size() > 0 ){
				
				new MenuItem( menu_tags, SWT.SEPARATOR );
			}
			
			List<String>	menu_names 		= new ArrayList<String>();
			Map<String,Tag>	menu_name_map 	= new IdentityHashMap<String, Tag>();

			for ( Tag t: manual_t ){
				
					// don't allow manual adding of taggables to auto-tags
				
				if ( !t.isTagAuto()){
					
					String name = t.getTagName( true );
					
					menu_names.add( name );
					menu_name_map.put( name, t );
				}
			}
				
			List<Object>	menu_structure = MenuBuildUtils.splitLongMenuListIntoHierarchy( menu_names, MAX_TOP_LEVEL_TAGS_IN_MENU );
			
			for ( Object obj: menu_structure ){
			
				List<Tag>	bucket_tags = new ArrayList<Tag>();
				
				Menu	 parent_menu;
				
				if ( obj instanceof String ){
					
					parent_menu = menu_tags;
					
					bucket_tags.add( menu_name_map.get((String)obj));
					
				}else{
					
					Object[]	entry = (Object[])obj;
					
					List<String>	tag_names = (List<String>)entry[1];
					
					boolean	sub_all_selected 	= true;
					boolean sub_some_selected	= false;
					
					for ( String name: tag_names ){
						
						Tag sub_tag = menu_name_map.get( name );
												
						Integer c = manual_map.get( sub_tag );
					
						if ( c != null && c == dms.length ){
							
							sub_some_selected = true;
							
						}else{
							
							sub_all_selected = false;
						}
						
						bucket_tags.add( sub_tag );
					}
					
					String mod;
					
					if ( sub_all_selected ){
						
						mod = " (*)";
						
					}else if ( sub_some_selected ){
						
						mod = " (+)";
						
					}else{
						
						mod = "";
					}
					
					Menu menu_bucket = new Menu( menu_tags.getShell(), SWT.DROP_DOWN );
					
					MenuItem bucket_item = new MenuItem( menu_tags, SWT.CASCADE );
					
					bucket_item.setText((String)entry[0] + mod);
					
					bucket_item.setMenu( menu_bucket );		
					
					parent_menu = menu_bucket;
				}
								
				for ( final Tag t: bucket_tags ){
				
					final MenuItem t_i = new MenuItem( parent_menu, SWT.CHECK );
					
					String tag_name = t.getTagName( true );
					
					Integer c = manual_map.get( t );
					
					if ( c != null ){
						
						if ( c == dms.length ){
							
							t_i.setSelection( true );
							
							t_i.setText( tag_name );
														
						}else{
							
							t_i.setText( tag_name + " (" + c + ")" );
						}
					}else{
						
						t_i.setText( tag_name );
					}
					
					t_i.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							
							boolean	selected = t_i.getSelection();
							
							for ( DownloadManager dm: dms ){
								
								if ( selected ){
									
									t.addTaggable( dm );
									
								}else{
									
									t.removeTaggable( dm );
								}
							}
						}
					});
				}
			}
		}
		
		new MenuItem( menu_tags, SWT.SEPARATOR );
		 
		MenuItem item_create = new MenuItem( menu_tags, SWT.PUSH);
		
		Messages.setLanguageText(item_create, "label.add.tag");
		item_create.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				
				Tag new_tag = createManualTag();
				
				if ( new_tag != null ){
					
					for ( DownloadManager dm: dms ){
						
						new_tag.addTaggable( dm );
					}
					
					COConfigurationManager.setParameter( "Library.TagInSideBar", true );
				}
			}
		});
	}
	
	public static List<TagType>
	sortTagTypes(
		Collection<TagType>	_tag_types )
	{
		List<TagType>	tag_types = new ArrayList<TagType>( _tag_types );
		
		Collections.sort(
			tag_types,
			new Comparator<TagType>()
			{
				final Comparator<String> comp = new FormattersImpl().getAlphanumericComparator( true );
				
				public int 
				compare(
					TagType o1, TagType o2) 
				{
					return( comp.compare( o1.getTagTypeName(true), o2.getTagTypeName(true)));
				}
			});
		
		return( tag_types );
	}
	
	public static List<Tag>
	sortTags(
		Collection<Tag>	_tags )
	{
		List<Tag>	tags = new ArrayList<Tag>( _tags );
		
		Collections.sort(
			tags,
			new Comparator<Tag>()
			{
				final Comparator<String> comp = new FormattersImpl().getAlphanumericComparator( true );

				public int 
				compare(
					Tag o1, Tag o2) 
				{
					return( comp.compare( o1.getTagName(true), o2.getTagName(true)));
				}
			});
		
		return( tags );
	}
	
	public static String
	getTagTooltip(
		Tag		tag )
	{
		TagType tag_type = tag.getTagType();
		
		String 	str = tag_type.getTagTypeName( true ) + ": " + tag.getTagName( true );
		
		if ( tag_type.hasTagTypeFeature( TagFeature.TF_RATE_LIMIT )){
			
			TagFeatureRateLimit rl = (TagFeatureRateLimit)tag;
			
			String 	up_str 		= "";
			String	down_str 	= "";
			
			int	limit_up = rl.getTagUploadLimit();
				
			if ( limit_up > 0 ){
				
				up_str += MessageText.getString( "label.limit" ) + "=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( limit_up );
			}
			
			int current_up 		= rl.getTagCurrentUploadRate();
			
			if ( current_up >= 0 ){
				
				up_str += (up_str.length()==0?"":", " ) + MessageText.getString( "label.current" ) + "=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( current_up);
			}
			
			int	limit_down = rl.getTagDownloadLimit();
			
			if ( limit_down > 0 ){
				
				down_str += MessageText.getString( "label.limit" ) + "=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( limit_down );
			}
			
			int current_down 		= rl.getTagCurrentDownloadRate();
			
			if ( current_down >= 0 ){
				
				down_str += (down_str.length()==0?"":", " ) + MessageText.getString( "label.current" ) + "=" + DisplayFormatters.formatByteCountToKiBEtcPerSec( current_down);
			}
			
			
			if ( up_str.length() > 0 ){
				
				str += "\r\n    " + MessageText.getString("iconBar.up") + ": " + up_str;
			}
			
			if ( down_str.length() > 0 ){
				
				str += "\r\n    " + MessageText.getString("iconBar.down") + ": " + down_str;
			}
			
			
			int up_pri = rl.getTagUploadPriority();
			
			if ( up_pri > 0 ){
				
				str += "\r\n    " + MessageText.getString("cat.upload.priority");
			}
		}
			
		if ( tag_type.hasTagTypeFeature( TagFeature.TF_FILE_LOCATION )) {
			
			TagFeatureFileLocation fl = (TagFeatureFileLocation)tag;

			if ( fl.supportsTagInitialSaveFolder()){
				
				File init_loc = fl.getTagInitialSaveFolder();
				
				if ( init_loc != null ){
					
					str += "\r\n    " + MessageText.getString("label.init.save.loc") + "=" + init_loc.getAbsolutePath();
				}
			}
			
			if ( fl.supportsTagMoveOnComplete()){
				
				File move_on_comp = fl.getTagMoveOnCompleteFolder();
				
				if ( move_on_comp != null ){
					
					str += "\r\n    " + MessageText.getString("label.move.on.comp") + "=" + move_on_comp.getAbsolutePath();
				}
			}
			if ( fl.supportsTagCopyOnComplete()){
				
				File copy_on_comp = fl.getTagCopyOnCompleteFolder();
				
				if ( copy_on_comp != null ){
					
					str += "\r\n    " + MessageText.getString("label.copy.on.comp") + "=" + copy_on_comp.getAbsolutePath();
				}
			}
		}
		
		return( str );
	}
	

}
