/*
 * Created on 27-Apr-2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package org.gudy.azureus2.pluginsimpl.update.sf.impl;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.pluginsimpl.update.sf.*;
import org.gudy.azureus2.core3.resourcedownloader.*;
import org.gudy.azureus2.core3.html.*;

public class 
SFPluginDetailsLoaderImpl 
	implements SFPluginDetailsLoader
{

	public static final String	site_prefix = "http://azureus.sourceforge.net/";
	
	public static final String	page_url 	= site_prefix + "plugin_list.php";

	protected static SFPluginDetailsLoaderImpl		singleton;
	
	
	public static synchronized SFPluginDetailsLoader
	getSingleton()
	{
		if ( singleton == null ){
			
			singleton	= new SFPluginDetailsLoaderImpl();
		}
		
		return( singleton );
	}
	
	protected boolean	plugin_names_loaded	= false;
	
	protected List		plugin_names		= new ArrayList();
	protected Map		plugin_map			= new HashMap();
	
	protected
	SFPluginDetailsLoaderImpl()
	{
	}
	
	protected void
	loadPluginList()
	
		throws SFPluginDetailsException
	{
		ResourceDownloader dl = ResourceDownloaderFactory.create( page_url );
		
		dl = ResourceDownloaderFactory.getRetryDownloader( dl, 5 );
		
		try{
			HTMLPage	page = HTMLPageFactory.loadPage( dl.download());
			
			String[]	links = page.getLinks();
			
			List	details = new ArrayList();
			
			for (int i=0;i<links.length;i++){
				
				String	link = links[i];
				
				if ( link.startsWith("plugin_details.php?plugin=" )){
	
					String	plugin_name = link.substring( 26 );

					plugin_names.add( plugin_name );
				}					
			}
			
			plugin_names_loaded	= true;
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			throw( new SFPluginDetailsException( "Plugin list load failed", e ));
		}
	}
	
	protected SFPluginDetailsImpl
	loadPluginDetails(
		String		plugin_name )
	
		throws SFPluginDetailsException
	{
		try{
			ResourceDownloader p_dl = ResourceDownloaderFactory.create( site_prefix + "plugin_details.php?plugin=" + plugin_name );
		
			p_dl = ResourceDownloaderFactory.getRetryDownloader( p_dl, 5 );
		
			HTMLPage	plugin_page = HTMLPageFactory.loadPage( p_dl.download());
			
			SFPluginDetailsImpl res = processPluginPage( plugin_name, plugin_page );
			
			if ( res == null ){
				
				throw( new SFPluginDetailsException( "Plugin details load fails: data not found" ));
			}
			
			return( res );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
			
			throw( new SFPluginDetailsException( "Plugin details load fails", e ));
		}
	}
	
	protected SFPluginDetailsImpl
	processPluginPage(
		String			name,
		HTMLPage		page )
	
		throws SFPluginDetailsException
	{
		return( processPluginPage( name, page.getTables()));
	}
	
	protected SFPluginDetailsImpl
	processPluginPage(
		String			name,
		HTMLTable[]		tables )
	
		throws SFPluginDetailsException
	{
		for (int i=0;i<tables.length;i++){
			
			HTMLTable	table = tables[i];
			
			HTMLTableRow[]	rows = table.getRows();
		
			if ( rows.length == 9 ){
				
				HTMLTableCell[]	cells = rows[0].getCells();
				
				if ( cells.length == 6 &&
						cells[0].getContent().trim().equals("Name") &&
						cells[5].getContent().trim().equals("Contact")){
				
					
					// got the plugin details table
				
					HTMLTableCell[]	detail_cells = rows[2].getCells();
					
					String	plugin_name			= detail_cells[0].getContent();
					String	plugin_version		= detail_cells[1].getContent();
					String	plugin_auth			= detail_cells[4].getContent();
					
					String[]	dl_links = detail_cells[2].getLinks();
					
					String	plugin_download;
					
					if ( dl_links.length == 0 ){
						
						plugin_download	= "<unknown>";
						
					}else{
						
						plugin_download = site_prefix + dl_links[0];
					}
					
					System.out.println( "got plugin:" + plugin_name + "/" + plugin_version + "/" + plugin_download + "/" + plugin_auth );
					
					return(	new SFPluginDetailsImpl(
									plugin_name,
									plugin_version,
									plugin_download,
									plugin_auth ));							
				}
			}
			
			HTMLTable[]	sub_tables = table.getTables();
			
			SFPluginDetailsImpl	res = processPluginPage( name, sub_tables );
			
			if( res != null ){
				
				return( res );
			}
		}
		
		return( null );
	}
	
	protected void
	dumpTables(
		String			indent,
		HTMLTable[]		tables )
	{
		for (int i=0;i<tables.length;i++){
			
			HTMLTable	tab = tables[i];
			
			System.out.println( indent + "tab:" + tab.getContent());
			
			HTMLTableRow[] rows = tab.getRows();
			
			for (int j=0;j<rows.length;j++){
				
				HTMLTableRow	row = rows[j];
				
				System.out.println( indent + "  row[" + j + "]: " + rows[j].getContent());
				
				HTMLTableCell[]	cells = row.getCells();
				
				for (int k=0;k<cells.length;k++){
					
					System.out.println( indent + "    cell[" + k + "]: " + cells[k].getContent());
					
				}
			}
			
			dumpTables( indent + "  ", tab.getTables());
		}
	}
	
	public String[]
	getPluginNames()
		
		throws SFPluginDetailsException
	{
		if ( !plugin_names_loaded ){
			
			loadPluginList();
		}
		
		String[]	res = new String[plugin_names.size()];
		
		plugin_names.toArray( res );
		
		return( res );
	}
	
	public SFPluginDetails
	getPluginDetails(
		String		name )
	
		throws SFPluginDetailsException
	{
		SFPluginDetails details = (SFPluginDetails)plugin_map.get(name); 
		
		if ( details == null ){
			
			details = loadPluginDetails( name );
			
			plugin_map.put( name, details );
		}
		
		return( details );
	}
	
	public SFPluginDetails[]
	getPluginDetails()
	
		throws SFPluginDetailsException	
	{
		SFPluginDetails[]	res = new SFPluginDetails[plugin_names.size()];
	
		for (int i=0;i<plugin_names.size();i++){
			
			res[i] = getPluginDetails((String)plugin_names.get(i));
		}
		
		return( res );
	}
}
