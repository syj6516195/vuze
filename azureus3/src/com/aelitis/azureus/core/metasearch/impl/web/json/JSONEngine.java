package com.aelitis.azureus.core.metasearch.impl.web.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.aelitis.azureus.core.metasearch.Engine;
import com.aelitis.azureus.core.metasearch.Result;
import com.aelitis.azureus.core.metasearch.ResultListener;
import com.aelitis.azureus.core.metasearch.SearchException;
import com.aelitis.azureus.core.metasearch.SearchParameter;
import com.aelitis.azureus.core.metasearch.impl.EngineImpl;
import com.aelitis.azureus.core.metasearch.impl.MetaSearchImpl;
import com.aelitis.azureus.core.metasearch.impl.web.FieldMapping;
import com.aelitis.azureus.core.metasearch.impl.web.WebEngine;
import com.aelitis.azureus.core.metasearch.impl.web.WebResult;

public class 
JSONEngine 
	extends WebEngine 
{
	public static EngineImpl
	importFromBEncodedMap(
		MetaSearchImpl	meta_search,
		Map				map )
	
		throws IOException
	{
		return( new JSONEngine( meta_search, map ));
	}
	
	public static Engine
	importFromJSONString(
		MetaSearchImpl	meta_search,
		long			id,
		long			last_updated,
		String			name,
		JSONObject		map )
	
		throws IOException
	{
		return( new JSONEngine( meta_search, id, last_updated, name, map ));
	}
	
	private String resultsEntryPath;

	
		// explicit test constructor

	public 
	JSONEngine(
		MetaSearchImpl		meta_search,
		long 				id,
		long 				last_updated,
		String 				name,
		String 				searchURLFormat,
		String 				timeZone,
		boolean 			automaticDateFormat,
		String 				userDateFormat,
		String 				resultsEntryPath,
		FieldMapping[] 		mappings) 
	{
		super( meta_search, Engine.ENGINE_TYPE_JSON, id,last_updated,name,searchURLFormat,timeZone,automaticDateFormat,userDateFormat,mappings);
		
		this.resultsEntryPath = resultsEntryPath;
		
		setSource( Engine.ENGINE_SOURCE_LOCAL );
		
		setSelectionState( SEL_STATE_MANUAL_SELECTED );
	}
	
		// bencoded constructor
	
	protected 
	JSONEngine(
		MetaSearchImpl	meta_search,
		Map				map )
	
		throws IOException
	{
		super( meta_search, map );
		
		resultsEntryPath = importString( map, "json.path" );
	}
	
		// json constructor
	
	protected 
	JSONEngine(
		MetaSearchImpl	meta_search,
		long			id,
		long			last_updated,
		String			name,
		JSONObject		map )
	
		throws IOException
	{
		super( meta_search, Engine.ENGINE_TYPE_JSON, id, last_updated, name, map );
				
		resultsEntryPath = importString( map, "json_result_key" );
	}
	
	public Map 
	exportToBencodedMap() 
	
		throws IOException
	{
		Map	res = new HashMap();
		
		exportString( res, "json.path", resultsEntryPath );
		
		super.exportToBencodedMap( res );
		
		return( res );
	}
	
	protected void
	exportToJSONObject(
		JSONObject		res )
	
		throws IOException
	{
		res.put( "json_result_key", resultsEntryPath );

		super.exportToJSONObject( res );
	}
	
	protected Result[]
	searchSupport(
		SearchParameter[] 	searchParameters,
		int					max_matches,
		String				headers, 
		ResultListener		listener )
	
		throws SearchException
	{	
		String page = super.getWebPageContent( searchParameters, headers );
		
		if ( listener != null ){
			listener.contentReceived( this, page );
		}
		
		
		String searchQuery = null;
		
		for(int i = 0 ; i < searchParameters.length ; i++) {
			if(searchParameters[i].getMatchPattern().equals("s")) {
				searchQuery = searchParameters[i].getValue();
			}
		}
		
		FieldMapping[] mappings = getMappings();

		if(page != null) {
			try {
				Object jsonObject = JSONValue.parse(page);
				
				JSONArray resultArray = null;
				
				if(resultsEntryPath != null) {
					StringTokenizer st = new StringTokenizer(resultsEntryPath,".");
					if(jsonObject instanceof JSONArray && st.countTokens() > 0) {
						JSONArray array = (JSONArray) jsonObject;
						if(array.size() == 1) {
							jsonObject = array.get(0);
						}
					}
					while(st.hasMoreTokens()) {
						try {
							jsonObject = ((JSONObject)jsonObject).get(st.nextToken());
						} catch(Throwable t) {
							throw new SearchException("Invalid entry path : " + resultsEntryPath,t);
						}
					}
				}
				
				try {
					resultArray = (JSONArray) jsonObject;
				} catch (Throwable t) {
					throw new SearchException("Object is not a result array. Check the JSON service and/or the entry path");
				}
					
					
				if(resultArray != null) {
					
					List results = new ArrayList();
					
					for(int i = 0 ; i < resultArray.size() ; i++) {
						
						Object obj = resultArray.get(i);
						
						if(obj instanceof JSONObject) {
							JSONObject jsonEntry = (JSONObject) obj;
							
							if ( max_matches >= 0 ){
								if ( --max_matches < 0 ){
									break;
								}
							}
							
							WebResult result = new WebResult(getRootPage(),getBasePage(),getDateParser(),searchQuery);
							
							List bits = listener==null?null:new ArrayList();
							
							for(int j = 0 ; j < mappings.length ; j++) {
								String fieldFrom = mappings[j].getName();
								if(fieldFrom != null) {
									int fieldTo = mappings[j].getField();
									Object fieldContentObj = ((Object)jsonEntry.get(fieldFrom));
									if(fieldContentObj != null) {
										String fieldContent = fieldContentObj.toString();

										if ( bits != null ){
											bits.add( fieldContent );
										}
										
										switch(fieldTo) {
										case FIELD_NAME :
											result.setNameFromHTML(fieldContent);
											break;
										case FIELD_SIZE :
											result.setSizeFromHTML(fieldContent);
											break;
										case FIELD_PEERS :
											result.setNbPeersFromHTML(fieldContent);
											break;
										case FIELD_SEEDS :
											result.setNbSeedsFromHTML(fieldContent);
											break;
										case FIELD_CATEGORY :
											result.setCategoryFromHTML(fieldContent);
											break;
										case FIELD_DATE :
											result.setPublishedDateFromHTML(fieldContent);
											break;
										case FIELD_COMMENTS :
											result.setCommentsFromHTML(fieldContent);
											break;
										case FIELD_CDPLINK :
											result.setCDPLink(fieldContent);
											break;
										case FIELD_TORRENTLINK :
											result.setTorrentLink(fieldContent);
											break;
										default:
											break;
										}
									}
								}
							}
							
							if ( bits != null ){
								
								listener.matchFound( this,(String[])bits.toArray(new String[bits.size()]));
							}
							
							results.add(result);
							
						}
					}
					
					return (Result[]) results.toArray(new Result[results.size()]);
					
				}

			} catch (Exception e) {
				e.printStackTrace();
				throw new SearchException(e);
			}
		} else {
			throw new SearchException("Web Page is empty");
		}
		
		return new Result[0];
	}
	
	public String getIcon() {
		
		String rootPage = getRootPage();
		
		if(rootPage != null) {
			return rootPage + "/favicon.ico";
		}
		return null;
	}
	

}
