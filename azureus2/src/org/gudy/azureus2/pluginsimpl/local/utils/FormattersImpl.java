/*
 * File    : FormattersImpl.java
 * Created : 30-Mar-2004
 * By      : parg
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

package org.gudy.azureus2.pluginsimpl.local.utils;

/**
 * @author parg
 *
 */

import java.io.IOException;
import java.util.Map;

import org.gudy.azureus2.core3.util.*;

import org.gudy.azureus2.plugins.utils.*;

public class 
FormattersImpl
	implements Formatters
{
	public String
	formatByteCountToKiBEtc(
		long		bytes )
	{
		return( DisplayFormatters.formatByteCountToKiBEtc( bytes ));
	}
	
	public String
	formatByteCountToKiBEtcPerSec(
		long		bytes )
	{
		return( DisplayFormatters.formatByteCountToKiBEtcPerSec( bytes ));
	}

	public String
	formatPercentFromThousands(
		long		thousands )
	{
		return( DisplayFormatters.formatPercentFromThousands( (int)thousands ));
	}
	
	public String
	formatByteArray(
		byte[]		data,
		boolean		no_spaces )
	{
		return( ByteFormatter.nicePrint( data, no_spaces ));
	}
	
	public String
	encodeBytesToString(
		byte[]		bytes )
	{
		return( ByteFormatter.encodeString( bytes ));
	}
	
	public byte[]
	decodeBytesFromString(
		String		str )
	{
		return( ByteFormatter.decodeString( str ));
	}
	
	public String
	formatDate(
		long		millis )
	{
		return( DisplayFormatters.formatDate( millis ));
	}
	
	public String
	formatTimeFromSeconds(
		long		seconds )
	{
		return( DisplayFormatters.formatTime( seconds ));
	}
	
	public byte[]
	bEncode(
		Map	map )
	
		throws IOException
	{
		return( BEncoder.encode( map ));
	}
	
	public Map
	bDecode(
		byte[]	data )
	
		throws IOException
	{
		return( BDecoder.decode( data ));
	}	
}
