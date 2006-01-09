/*
 * File    : EndGameModeChunk.java
 * Created : 4 d�c. 2003}
 * By      : Olivier
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
package org.gudy.azureus2.core3.peer.impl.control;

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.peer.PEPiece;

import com.aelitis.azureus.core.util.PieceBlock;

/**
 * @author Olivier
 * @author MjrTom
 * 			2006/Jan/06 Refactoring, change to use PieceBlock
 */
public class EndGameModeChunk
{
	private PieceBlock	chunk;

	private int	offset;
	private int	length;

	public EndGameModeChunk(PEPiece pePiece, int blockNumber)
	{
		//this.piece = piece;
		chunk =new PieceBlock(pePiece.getPieceNumber(), blockNumber);
		length =pePiece.getBlockSize(blockNumber);
		offset =blockNumber *DiskManager.BLOCK_SIZE;
	}

	public boolean compare(int pieceNumber, int os)
	{
		return ((chunk.getPieceNumber() ==pieceNumber) &&(this.offset ==os));
	}

	/**
	 * @return int Returns the pieceNumber.
	 */
	public int getPieceNumber()
	{
		return chunk.getPieceNumber();
	}

	/**
	 * @return int Returns the blockNumber.
	 */
	public int getBlockNumber()
	{
		return chunk.getBlockNumber();
	}

	public int getOffset()
	{
		return offset;
	}

	public int getLength()
	{
		return length;
	}
}
