/*
 * Created on 13-May-2005
 * Created by Paul Gardner
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.util.bloom.impl;

public class 
BloomFilterAddOnly
	extends BloomFilterImpl
{
	private byte[]		map;

	public
	BloomFilterAddOnly(
		int		_max_entries )
	{
		super( _max_entries );
			
		map	= new byte[(getMaxEntries()+7)/8];
	}
	
	protected byte
	getValue(
		int		index )
	{
		byte	b = map[index/8];
			
		return((byte)((b>>(index%8))&0x01));

	}
	
	protected void
	setValue(
		int		index,
		byte	value )
	{
		byte	b = map[index/8];
				
		if ( value == 0 ){
			
			
			// b = (byte)(b&~(0x01<<(index%8)));
			
			throw( new RuntimeException( "remove not supported" ));

		}else{
			
			b = (byte)(b|(0x01<<(index%8)));
		}
		
		// System.out.println( "setValue[" + index + "]:" + Integer.toHexString( map[index/2]&0xff) + "->" + Integer.toHexString( b&0xff ));
		
		map[index/8] = b;
	}
}
