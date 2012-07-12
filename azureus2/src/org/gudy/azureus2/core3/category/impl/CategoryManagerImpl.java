/*
 * File    : CategoryManagerImpl.java
 * Created : 09 feb. 2004
 * By      : TuxPaper
 *
 * Azureus - a Java Bittorrent client
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
 */

package org.gudy.azureus2.core3.category.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.gudy.azureus2.core3.category.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.xml.util.XMLEscapeWriter;
import org.gudy.azureus2.core3.xml.util.XUXmlWriter;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadScrapeResult;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageRequest;
import org.gudy.azureus2.plugins.tracker.web.TrackerWebPageResponse;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.rssgen.RSSGeneratorPlugin;

public class 
CategoryManagerImpl 
	implements RSSGeneratorPlugin.Provider 
{
  private static final String PROVIDER = "categories";

  private static final String UNCAT_NAME 	= "__uncategorised__";
  private static final String ALL_NAME 		= "__all__";
  
  private static CategoryManagerImpl catMan;
  private static CategoryImpl catAll = null;
  private static CategoryImpl catUncategorized = null;
  private static boolean doneLoading = false;
  private static AEMonitor	class_mon	= new AEMonitor( "CategoryManager:class" );
  
  private Map<String,CategoryImpl> categories 			= new HashMap<String,CategoryImpl>();
  private AEMonitor	categories_mon	= new AEMonitor( "Categories" );
  
  private static final int LDT_CATEGORY_ADDED     = 1;
  private static final int LDT_CATEGORY_REMOVED   = 2;
  private static final int LDT_CATEGORY_CHANGED   = 3;
  private ListenerManager category_listeners = ListenerManager.createManager(
    "CatListenDispatcher",
    new ListenerManagerDispatcher()
    {
      public void
      dispatch(Object   _listener,
               int      type,
               Object   value )
      {
        CategoryManagerListener target = (CategoryManagerListener)_listener;

        if ( type == LDT_CATEGORY_ADDED )
          target.categoryAdded((Category)value);
        else if ( type == LDT_CATEGORY_REMOVED )
            target.categoryRemoved((Category)value);
        else if ( type == LDT_CATEGORY_CHANGED )
            target.categoryChanged((Category)value);
        }
    });


  protected
  CategoryManagerImpl()
  {
  	loadCategories();
  }
  
  public void addCategoryManagerListener(CategoryManagerListener l) {
    category_listeners.addListener( l );
  }

  public void removeCategoryManagerListener(CategoryManagerListener l) {
    category_listeners.removeListener( l );
  }

  public static CategoryManagerImpl getInstance() {
  	try{
  		class_mon.enter();
	    if (catMan == null)
	      catMan = new CategoryManagerImpl();
	    return catMan;
  	}finally{
  		
  		class_mon.exit();
  	}
  }

  protected void loadCategories() {
    if (doneLoading)
      return;
    doneLoading = true;

    FileInputStream fin = null;
    BufferedInputStream bin = null;
 
    makeSpecialCategories();

   
    try {
      //open the file
      File configFile = FileUtil.getUserFile("categories.config");
      fin = new FileInputStream(configFile);
      bin = new BufferedInputStream(fin, 8192);
     
      Map map = BDecoder.decode(bin);

      List catList = (List) map.get("categories");
      for (int i = 0; i < catList.size(); i++) {
        Map mCategory = (Map) catList.get(i);
        try {
          String catName = new String((byte[]) mCategory.get("name"), Constants.DEFAULT_ENCODING);
          
          Long l_maxup 		= (Long)mCategory.get( "maxup" );
          Long l_maxdown 	= (Long)mCategory.get( "maxdown" );
          Map<String,String>	attributes = BDecoder.decodeStrings((Map)mCategory.get( "attr" ));
          
          if ( attributes == null ){
        	  
        	  attributes = new HashMap<String, String>();
          }
          
          if ( catName.equals( UNCAT_NAME )){
        	  
        	  catUncategorized.setUploadSpeed(l_maxup==null?0:l_maxup.intValue());
        	  catUncategorized.setDownloadSpeed(l_maxdown==null?0:l_maxdown.intValue());
        	  catUncategorized.setAttributes( attributes );
        	  
          }else if ( catName.equals( ALL_NAME )){
            	  
              catAll.setAttributes( attributes );

          }else{
	          categories.put( 
	        	catName,
	        	  new CategoryImpl( 
	        		  catName, 
	        		  l_maxup==null?0:l_maxup.intValue(),
	        		  l_maxdown==null?0:l_maxdown.intValue(),
	        			attributes ));
          }
        }
        catch (UnsupportedEncodingException e1) {
          //Do nothing and process next.
        }
      }
    }
    catch (FileNotFoundException e) {
      //Do nothing
    }
    catch (Exception e) {
    	Debug.printStackTrace( e );
    }
    finally {
      try {
        if (bin != null)
          bin.close();
      }
      catch (Exception e) {}
      try {
        if (fin != null)
          fin.close();
      }
      catch (Exception e) {}
      
      checkConfig();
    }
  }

  protected void saveCategories(Category category ){
	  saveCategories();
	  
      category_listeners.dispatch( LDT_CATEGORY_CHANGED, category );
  }
  protected void saveCategories() {
    try{
    	categories_mon.enter();
    
      Map map = new HashMap();
      List list = new ArrayList(categories.size());

      Iterator<CategoryImpl> iter = categories.values().iterator();
      while (iter.hasNext()) {
        CategoryImpl cat = iter.next();

        if (cat.getType() == Category.TYPE_USER) {
          Map catMap = new HashMap();
          catMap.put( "name", cat.getName());
          catMap.put( "maxup", new Long(cat.getUploadSpeed()));
          catMap.put( "maxdown", new Long(cat.getDownloadSpeed()));
          catMap.put( "attr", cat.getAttributes());
          list.add(catMap);
        }
      }
      
      Map uncat = new HashMap();
      uncat.put( "name", UNCAT_NAME );
      uncat.put( "maxup", new Long(catUncategorized.getUploadSpeed()));
      uncat.put( "maxdown", new Long(catUncategorized.getDownloadSpeed()));
      uncat.put( "attr", catUncategorized.getAttributes());
      list.add( uncat );
      
      Map allcat = new HashMap();
      allcat.put( "name", ALL_NAME );
      allcat.put( "attr", catAll.getAttributes());
      list.add( allcat );
      
      map.put("categories", list);


      FileOutputStream fos = null;

      try {
        //encode the data
        byte[] torrentData = BEncoder.encode(map);

         File oldFile = FileUtil.getUserFile("categories.config");
         File newFile = FileUtil.getUserFile("categories.config.new");

         //write the data out
        fos = new FileOutputStream(newFile);
        fos.write(torrentData);
         fos.flush();
         fos.getFD().sync();

          //close the output stream
         fos.close();
         fos = null;

         //delete the old file
         if ( !oldFile.exists() || oldFile.delete() ) {
            //rename the new one
            newFile.renameTo(oldFile);
         }

      }
      catch (Exception e) {
      	Debug.printStackTrace( e );
      }
      finally {
        try {
          if (fos != null)
            fos.close();
        }
        catch (Exception e) {}
      }
    }finally{
    	
    	checkConfig();
    	 
    	categories_mon.exit();
    }
  }

  public Category createCategory(String name) {
    makeSpecialCategories();
    CategoryImpl newCategory = getCategory(name);
    if (newCategory == null) {
      newCategory = new CategoryImpl(name, 0, 0, new HashMap<String,String>());
      categories.put(name, newCategory);
      saveCategories();

      category_listeners.dispatch( LDT_CATEGORY_ADDED, newCategory );
      return (Category)categories.get(name);
    }
    return newCategory;
  }

  public void removeCategory(Category category) {
    if (categories.containsKey(category.getName())) {
      categories.remove(category.getName());
      saveCategories();
      category_listeners.dispatch( LDT_CATEGORY_REMOVED, category );
    }
  }

  public Category[] getCategories() {
    if (categories.size() > 0)
      return (Category[])categories.values().toArray(new Category[categories.size()]);
    return (new Category[0]);
  }

  public CategoryImpl getCategory(String name) {
    return categories.get(name);
  }

  public Category getCategory(int type) {
    if (type == Category.TYPE_ALL)
      return catAll;
    if (type == Category.TYPE_UNCATEGORIZED)
      return catUncategorized;
    return null;
  }

  private void makeSpecialCategories() {
    if (catAll == null) {
      catAll = new CategoryImpl("Categories.all", Category.TYPE_ALL, new HashMap<String,String>());
      categories.put("Categories.all", catAll);
    }
    
    if (catUncategorized == null) {
      catUncategorized = new CategoryImpl("Categories.uncategorized", Category.TYPE_UNCATEGORIZED, new HashMap<String,String>());
      categories.put("Categories.uncategorized", catUncategorized);
    }
  }
  
  
  
  private void
  checkConfig()
  {
	  boolean	gen_enabled = false;
	  
	  for ( CategoryImpl cat: categories.values()){
		  
		  if ( cat.getBooleanAttribute( Category.AT_RSS_GEN )){
			  
			  gen_enabled = true;
			  
			  break;
		  }
	  }
	  
	  if ( gen_enabled ){
		  
		  RSSGeneratorPlugin.registerProvider( PROVIDER, this  );
		  
	  }else{
		  
		  RSSGeneratorPlugin.unregisterProvider( PROVIDER );
	  }
  }
  
	public boolean
	isEnabled()
	{
		return( true );
	}

	public boolean
	generate(
		TrackerWebPageRequest		request,
		TrackerWebPageResponse		response )
	
		throws IOException
	{
		URL	url	= request.getAbsoluteURL();
		
		String path = url.getPath();
		
		int	pos = path.indexOf( '?' );
		
		if ( pos != -1 ){
			
			path = path.substring(0,pos);
		}
		
		path = path.substring( PROVIDER.length()+1);

		XMLEscapeWriter pw = new XMLEscapeWriter( new PrintWriter(new OutputStreamWriter( response.getOutputStream(), "UTF-8" )));

		pw.setEnabled( false );
		
		if ( path.length() <= 1 ){
			
			response.setContentType( "text/html; charset=UTF-8" );
			
			pw.println( "<HTML><HEAD><TITLE>Vuze Category Feeds</TITLE></HEAD><BODY>" );
			
			Map<String,String>	lines = new TreeMap<String, String>();
			
			List<CategoryImpl>	cats;
			
			try{
				categories_mon.enter();

				cats = new ArrayList<CategoryImpl>( categories.values());

			}finally{
			
				categories_mon.exit();
			}
			
			for ( CategoryImpl c: cats ){
			
				if ( c.getBooleanAttribute( Category.AT_RSS_GEN )){
							
					String	name = getDisplayName( c );
					
					String	cat_url = PROVIDER + "/" + URLEncoder.encode( c.getName(), "UTF-8" );
				
					lines.put( name, "<LI><A href=\"" + cat_url + "\">" + name + "</A></LI>" );
				}
			}
			
			for ( String line: lines.values() ){
				
				pw.println( line );
			}
			
			pw.println( "</BODY></HTML>" );
			
		}else{
			
			String	cat_name = URLDecoder.decode( path.substring( 1 ), "UTF-8" );
			
			CategoryImpl	cat;
			
			try{
				categories_mon.enter();

				cat = categories.get( cat_name );

			}finally{
			
				categories_mon.exit();
			}
			
			if ( cat == null ){
				
				response.setReplyStatus( 404 );
				
				return( true );
			}
			
			List<DownloadManager> dms = cat.getDownloadManagers( AzureusCoreFactory.getSingleton().getGlobalManager().getDownloadManagers());
			
			List<Download> downloads = new ArrayList<Download>( dms.size());
			
			long	dl_marker = 0;
			
			for ( DownloadManager dm: dms ){
				
				TOTorrent torrent = dm.getTorrent();
				
				if ( torrent == null ){
					
					continue;
				}
				
				if ( !TorrentUtils.isReallyPrivate( torrent )){
				
					dl_marker += dm.getDownloadState().getLongParameter( DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME );
				
					downloads.add( PluginCoreUtils.wrap(dm));
				}
			}
			
			String	config_key = "cat.rss.config." + Base32.encode( cat.getName().getBytes( "UTF-8" ));
			
			long	old_marker = COConfigurationManager.getLongParameter( config_key + ".marker", 0 );
			
			long	last_modified = COConfigurationManager.getLongParameter( config_key + ".last_mod", 0 );
			
			long now = SystemTime.getCurrentTime();
			
			if ( old_marker == dl_marker ){
				
				if ( last_modified == 0 ){
					
					last_modified = now;
				}
			}else{
				
				COConfigurationManager.setParameter( config_key + ".marker", dl_marker );
				
				last_modified = now; 
			}
			
			if ( last_modified == now ){
				
				COConfigurationManager.setParameter( config_key + ".last_mod", last_modified );
			}
			
			pw.println( "<?xml version=\"1.0\" encoding=\"utf-8\"?>" );
			
			pw.println( "<rss version=\"2.0\" xmlns:vuze=\"http://www.vuze.com\">" );
			
			pw.println( "<channel>" );
			
			pw.println( "<title>" + escape( getDisplayName( cat )) + "</title>" );
			
			Collections.sort(
					downloads,
				new Comparator<Download>()
				{
					public int 
					compare(
						Download d1, 
						Download d2) 
					{
						long	added1 = getAddedTime( d1 )/1000;
						long	added2 = getAddedTime( d2 )/1000;
		
						return((int)(added2 - added1 ));
					}
				});
								
							
			pw.println(	"<pubDate>" + TimeFormatter.getHTTPDate( last_modified ) + "</pubDate>" );
		
			for (int i=0;i<downloads.size();i++){
				
				Download download = downloads.get( i );
				
				DownloadManager	core_download = PluginCoreUtils.unwrap( download );
				
				Torrent torrent = download.getTorrent();
				
				byte[] hash = torrent.getHash();
				
				String	hash_str = Base32.encode( hash );
				
				pw.println( "<item>" );
				
				pw.println( "<title>" + escape( download.getName()) + "</title>" );
				
				pw.println( "<guid>" + hash_str + "</guid>" );
				
				String magnet_url = escape( UrlUtils.getMagnetURI( download.getName(), torrent ));

				pw.println( "<link>" + magnet_url + "</link>" );
				
				long added = core_download.getDownloadState().getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME);
				
				pw.println(	"<pubDate>" + TimeFormatter.getHTTPDate( added ) + "</pubDate>" );
				
				pw.println(	"<vuze:size>" + torrent.getSize()+ "</vuze:size>" );
				pw.println(	"<vuze:assethash>" + hash_str + "</vuze:assethash>" );
												
				pw.println( "<vuze:downloadurl>" + magnet_url + "</vuze:downloadurl>" );
		
				DownloadScrapeResult scrape = download.getLastScrapeResult();
				
				if ( scrape != null && scrape.getResponseType() == DownloadScrapeResult.RT_SUCCESS ){
					
					pw.println(	"<vuze:seeds>" + scrape.getSeedCount() + "</vuze:seeds>" );
					pw.println(	"<vuze:peers>" + scrape.getNonSeedCount() + "</vuze:peers>" );
				}
				
				pw.println( "</item>" );
			}
			
			pw.println( "</channel>" );
			
			pw.println( "</rss>" );
		}
		 
		pw.flush();
		
		return( true );
	}
		
	private String
	getDisplayName(
		CategoryImpl	c )
	{
		if ( c == catAll ){
			
			return( MessageText.getString( "Categories.all" ));
			
		}else if ( c == catUncategorized ){
			
			return( MessageText.getString( "Categories.uncategorized" ));
			
		}else{
			
			return( c.getName());
		}
	}
	
	protected long
	getAddedTime(
		Download	download )
	{
		DownloadManager	core_download = PluginCoreUtils.unwrap( download );
		
		return( core_download.getDownloadState().getLongParameter(DownloadManagerState.PARAM_DOWNLOAD_ADDED_TIME));
	}
	
	protected String
	escape(
		String	str )
	{
		return( XUXmlWriter.escapeXML(str));
	}
}
