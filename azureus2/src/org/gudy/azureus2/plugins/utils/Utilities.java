/*
 * File    : Utilities.java
 * Created : 24-Mar-2004
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

package org.gudy.azureus2.plugins.utils;

/**
 * @author parg
 *
 */

import java.io.InputStream;
import java.nio.ByteBuffer;

import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;
import org.gudy.azureus2.plugins.utils.security.*;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.*;

public interface 
Utilities 
{
	public String
	getAzureusUserDir();
	
	public boolean
	isCVSVersion();
	
	public InputStream
	getImageAsStream(
		String	image_name );
	
	public Semaphore
	getSemaphore();
	
	public ByteBuffer
	allocateDirectByteBuffer(
		int		size );
	
	public void
	freeDirectByteBuffer(
		ByteBuffer	buffer );
	
	public Formatters
	getFormatters();
	
	public LocaleUtilities
	getLocaleUtilities();
	
	public UTTimer
	createTimer(
		String		name );

		/**
		 * create and run a thread for the target. This will be a daemon thread so that
		 * its existence doesn't interfere with Azureus closedown
		 * @param name
		 * @param target
		 */
	
	public void
	createThread(
		String		name,
		Runnable	target );
	
		/**
		 * create a child process and executes the supplied command line. The child process
		 * will not inherit any open handles on Windows, which does happen if Runtime is
		 * used directly. This relies on the Platform plugin, if this is not installed then
		 * this will fall back to using Runtime.exec 
		 * @param command_line
		 */
	
	public void
	createProcess(
		String		command_line )
	
		throws PluginException;
	
	public ResourceDownloaderFactory
	getResourceDownloaderFactory();
	
	public SESecurityManager
	getSecurityManager();
	
	public SimpleXMLParserDocumentFactory
	getSimpleXMLParserDocumentFactory();
}
