/*
 * Created on 21-Jun-2004
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

package org.gudy.azureus2.core3.internat;

import java.io.File;
import java.io.UnsupportedEncodingException;
import org.gudy.azureus2.core3.util.*;

/**
 * @author parg
 *
 */

public class 
LocaleUtilDecoderFallback 
	implements LocaleUtilDecoder
{
	public static String	NAME	= "Fallback";

	private static volatile int		max_ok_name_length	= 64;
	private static volatile boolean max_ok_name_length_determined;
	
		// don't change these, it'll stuff up people with torrents that are using the 
		// fallback encoding
	
	private static final String VALID_CHARS = "abcdefghijklmnopqrstuvwxyz1234567890_-.";
	
	private int		index;
	

	protected
	LocaleUtilDecoderFallback(
		int		_index )
	{
		index	= _index;
	}
	
	public String
	getName()
	{
		return( NAME );
	}

	public int
	getIndex()
	{
		return( index );
	}
	
	public String
	tryDecode(
		byte[]		bytes,
		boolean		lax )
	{
		return( decode( bytes ));
	}
	
	public String
	decodeString(
		byte[]		bytes )
		
		throws UnsupportedEncodingException
	{
		return( decode( bytes ));
	}
	
	protected String
	decode(
		byte[]	data )
	{
		if ( data == null ){
			
			return( null );
		}
		
		String	res = "";
		
		for (int i=0;i<data.length;i++){
			
			byte	c = data[i];
			
			if ( VALID_CHARS.indexOf( Character.toLowerCase((char)c)) != -1 ){
				
				res += (char)c;
				
			}else{
				
				res += "_" + ByteFormatter.nicePrint(c);
			}
		}
		
			// more often that not these decoded values are used for filenames. Windows has a limit
			// of 250 (ish) chars, so we do something sensible with longer values
		
		int	len = res.length();
		
		if ( len > max_ok_name_length ){
		
				// could be a file system out there that supports arbitarily long names, so
				// we can't pre-calculate the max
			
			if ( 	( !max_ok_name_length_determined )&&
					fileLengthOK( len )){
			
					// this length is ok, bump up the known limit
				
				max_ok_name_length = len;
				
			}else{
				
					// won't fit
				
				if ( !max_ok_name_length_determined ){
					
					for (int i=max_ok_name_length+16;i<len;i+=16 ){
						
						if ( fileLengthOK( i )){
							
							max_ok_name_length	= i;
						}
					}
					
					max_ok_name_length_determined	= true;
				}
				
					// replace the end of the string with a hash value to ensure uniqueness
				
				byte[] hash = new SHA1Hasher().calculateHash( data );
				
				String	hash_str = ByteFormatter.nicePrint( hash, true );
				
				res = res.substring( 0, max_ok_name_length-hash_str.length()) + hash_str;
			}
		}
		
		return( res );
	}
	
	protected boolean
	fileLengthOK(
		int		len )
	{
		String n = "";
		
		for (int i=0;i<len;i++){
			
			n += "A";
		}
		
		try{
			File f = File.createTempFile( n, "" );
						
			f.delete();
			
			return( true );
			
		}catch( Throwable e ){
			
			return( false );
		}
	}
}
