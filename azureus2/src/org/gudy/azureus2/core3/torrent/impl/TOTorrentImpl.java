/*
 * Created on 03-Oct-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.gudy.azureus2.core3.torrent.impl;

/**
 * @author gardnerpar
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import java.util.*;
import java.io.*;
import java.net.*;

import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;

public class 
TOTorrentImpl
	implements TOTorrent
{
	protected static final String TK_ANNOUNCE			= "announce";
	protected static final String TK_ANNOUNCE_LIST		= "announce-list";
	protected static final String TK_COMMENT			= "comment";
	protected static final String TK_INFO				= "info";
	protected static final String TK_NAME				= "name";
	protected static final String TK_LENGTH				= "length";
	protected static final String TK_PATH				= "path";
	protected static final String TK_FILES				= "files";
	protected static final String TK_PIECE_LENGTH		= "piece length";
	protected static final String TK_PIECES				= "pieces";
	
	private String							torrent_name;
	private String							comment;
	private URL								announce_url;
	private TOTorrentAnnounceURLGroupImpl	announce_group = new TOTorrentAnnounceURLGroupImpl();
	
	private long		piece_length;
	private byte[][]	pieces;
	
	private byte[]		torrent_hash;
	
	private boolean				simple_torrent;
	private TOTorrentFile[]		files;

	private Map					additional_properties = new HashMap();
	
	/** 
	 * Constructor for deserialisation
	 */
	
	protected
	TOTorrentImpl()
	{
	}

	/** 
	 * Constructor for creation
	 */
	
	protected
	TOTorrentImpl(
		String		_torrent_name,
		URL			_announce_url,
		long		_piece_length,
		boolean		_simple_torrent )
	{
		torrent_name		= _torrent_name;
		announce_url		= _announce_url;
		piece_length		= _piece_length;
		simple_torrent		= _simple_torrent;
	}
	
	public void
	serialiseToFile(
		File		output_file )
	
		throws TOTorrentException
	{				
		byte[]	res = serialiseToByteArray();
		
		try{
			FileOutputStream os = new FileOutputStream( output_file );
			
			os.write( res );
		
			os.close();
			
		}catch( Throwable e){
			
			throw( new TOTorrentException( "TOTorrent::serialise: fails '" + e.toString() + "'" ));
		}
	}
	
	protected byte[]
	serialiseToByteArray()
	
		throws TOTorrentException
	{
		Map	root = serialiseToMap();
				
		return( BEncoder.encode( root ));
	}		

	protected Map
	serialiseToMap()
	
		throws TOTorrentException
	{		
		Map	root = new HashMap();
		
		writeStringToMetaData( root, TK_ANNOUNCE, announce_url.toString());
		
		TOTorrentAnnounceURLSet[] sets = announce_group.getAnnounceURLSets();
		
		if (sets.length > 0 ){
			
			List	announce_list = new ArrayList();
			
			for (int i=0;i<sets.length;i++){
				
				TOTorrentAnnounceURLSet	set = sets[i];
				
				URL[]	urls = set.getAnnounceURLs();
				
				if ( urls.length == 0 ){
					
					continue;
				}
				
				List sub_list = new ArrayList();
				
				announce_list.add( sub_list );
				
				for (int j=0;j<urls.length;j++){
					
					sub_list.add( writeStringToMetaData( urls[j].toString())); 
				}
			}
			
			if ( announce_list.size() > 0 ){
				
				root.put( TK_ANNOUNCE_LIST, announce_list );
			}
		}
		
		if ( comment != null ){
			
			writeStringToMetaData( root, TK_COMMENT, comment );			
		}
		
		Map info = new HashMap();
		
		root.put( TK_INFO, info );
		
		info.put( TK_PIECE_LENGTH, new Long( piece_length ));
		
		byte[]	flat_pieces = new byte[pieces.length*20];
		
		for (int i=0;i<pieces.length;i++){
			
			System.arraycopy( pieces[i], 0, flat_pieces, i*20, 20 );
		}
		
		info.put( TK_PIECES, flat_pieces );
		
		writeStringToMetaData( info, TK_NAME, torrent_name );
		
		if ( simple_torrent ){
		
			TOTorrentFile	file = files[0];
			
			info.put( TK_LENGTH, new Long( file.getLength()));
			
		}else{
	
			List	meta_files = new ArrayList();
		
			info.put( TK_FILES, meta_files );
		
			for (int i=0;i<files.length;i++){
				
				Map	file = new HashMap();
		
				meta_files.add( file );
				
				file.put( TK_LENGTH, new Long( files[i].getLength()));
				
				List path = new ArrayList();
				
				file.put( TK_PATH, path );
				
				String	str_path = files[i].getPath();
				
				int	pos = 0;
				
				while(true){
					
					int	p1 = str_path.indexOf( File.separator, pos );
					
					if ( p1 == -1 ){
						
						path.add( writeStringToMetaData( str_path.substring(pos)));
						
						break;
						
					}else{
						
						path.add( writeStringToMetaData( str_path.substring(pos,p1)));
						
						pos	= p1+1;
					}
				}
			}
		}
		
		Iterator it = additional_properties.keySet().iterator();
		
		while( it.hasNext()){
			
			String	key = (String)it.next();
			
			Object	value = additional_properties.get( key );
			
			if ( value != null ){
				
				root.put( key, value );
			}
		}
		
		return( root );
	}
	
	public String
	getName()
	{
		return( torrent_name );
	}
	
	public String
	getComment()
	{
		return( comment );
	}
	
	protected void
	setName(
		String	_name )
	{
		torrent_name		= _name;
	}
	
	public URL
	getAnnounceURL()
	{
		return( announce_url );
	}
	
	public byte[]
	getHash()
	
		throws TOTorrentException
	{
		if ( torrent_hash == null ){
			
			Map	root = serialiseToMap();
				
			Map info = (Map)root.get( TK_INFO );
				
			setHashFromInfo( info );		
		}
		
		return( torrent_hash );
	}
	
	protected void
	setHashFromInfo(
		Map		info )
		
		throws TOTorrentException
	{	
		try{
			SHA1Hasher s = new SHA1Hasher();
				
			torrent_hash = s.calculateHash(BEncoder.encode(info));
	
		}catch( Throwable e ){
				
			throw( new TOTorrentException( "TOTorrent::setHashFromInfo: fails '" + e.toString() + "'"));
		}
	}
	
	protected void
	setAnnounceURL(
		URL		_url )
	{
		announce_url		= _url;
	}
	
	public TOTorrentAnnounceURLGroup
	getAnnounceURLGroup()
	{
		return( announce_group );
	}

	protected void
	addTorrentAnnounceURLSet(
		URL[]		urls )
	{
		announce_group.addSet( new TOTorrentAnnounceURLSetImpl( urls ));
	}
	
	public long
	getPieceLength()
	{
		return( piece_length );
	}
	
	protected void
	setPieceLength(
		long	_length )
	{
		piece_length	= _length;
	}
	
	public byte[][]
	getPieces()
	{
		return( pieces );
	}
	
	protected void
	setPieces(
		byte[][]	_pieces )
	{
		pieces = _pieces;
	}
	
	public TOTorrentFile[]
	getFiles()
	{
		return( files );
	}
	
	protected void
	setFiles(
		TOTorrentFile[]		_files )
	{
		files	= _files;
	}
	
	protected boolean
	getSimpleTorrent()
	{
		return( simple_torrent );
	}
	
	protected void
	setSimpleTorrent(
		boolean	_simple_torrent )
	{
		simple_torrent	= _simple_torrent;
	}
	
	public void
	setAdditionalStringProperty(
		String		name,
		String		value )
		
		throws TOTorrentException
	{
		setAdditionalByteArrayProperty( name, writeStringToMetaData( value ));
	}
		
	public String
	getAdditionalStringProperty(
		String		name )
		
		throws TOTorrentException
	{				
		return( readStringFromMetaData( getAdditionalByteArrayProperty(name)));
	}
	
	public void
	setAdditionalByteArrayProperty(
		String		name,
		byte[]		value )
	{
		additional_properties.put( name, value );
	}
		
	public byte[]
	getAdditionalByteArrayProperty(
		String		name )
	{
		return((byte[])additional_properties.get( name ));
	}
	
	protected String
	readStringFromMetaData(
		Map		meta_data,
		String	name )
		
		throws TOTorrentException
	{
		return(readStringFromMetaData((byte[])meta_data.get(name)));			
	}
	
	protected String
	readStringFromMetaData(
		byte[]		value )
		
		throws TOTorrentException
	{
		try{
			if ( value == null ){
				
				return( null );
			}
			
			return(	new String(value, Constants.DEFAULT_ENCODING ));
			
		}catch( UnsupportedEncodingException e ){
			
			throw( new TOTorrentException( "TOTorrentDeserialise: unsupported encoding for '" + new String(value) + "'"));
		}
	}
	
	protected void
	writeStringToMetaData(
		Map		meta_data,
		String	name,
		String	value )
		
		throws TOTorrentException
	{
		meta_data.put( name, writeStringToMetaData( value ));	
	}
	
	protected byte[]
	writeStringToMetaData(
		String		value )
		
		throws TOTorrentException
	{
		try{
			
			return(	value.getBytes( Constants.DEFAULT_ENCODING ));
			
		}catch( UnsupportedEncodingException e ){
			
			throw( new TOTorrentException( "TOTorrent::writeStringToMetaData: unsupported encoding for '" + new String(value) + "'"));
		}
	}
	
	public void
	print()
	{
		try{
			byte[]	hash = getHash();
			
			System.out.println( "name = " + torrent_name );
			System.out.println( "announce url = " + announce_url );
			System.out.println( "announce group = " + announce_group.getAnnounceURLSets().length );
			System.out.println( "hash = " + ByteFormatter.nicePrint( hash ));
			System.out.println( "piece length = " + getPieceLength() );
			System.out.println( "pieces = " + getPieces().length );
			
			for (int i=0;i<pieces.length;i++){
				
				System.out.println( "\t" + ByteFormatter.nicePrint(pieces[i]));
			}
											 
			for (int i=0;i<files.length;i++){
				
				System.out.println( "\t" + files[i].getPath() + " (" + files[i].getLength() + ")" );
			}
		}catch( TOTorrentException e ){
			
			e.printStackTrace();
		}
	}
}