/*
 * File    : PluginEvent.java
 * Created : 06-Feb-2004
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

package org.gudy.azureus2.plugins;

/** allow config wizard progress to be determined
 *
 * @author parg
 * @since 2.0.8.0
 */
public interface 
PluginEvent 
{
	public static final int	PEV_CONFIGURATION_WIZARD_STARTS			= 1;
	public static final int	PEV_CONFIGURATION_WIZARD_COMPLETES		= 2;
	public static final int	PEV_INITIALISATION_PROGRESS_TASK		= 3;
	public static final int	PEV_INITIALISATION_PROGRESS_PERCENT		= 4;
		/**
		 * @since 2403
		 */
	public static final int	PEV_INITIAL_SHARING_COMPLETE			= 5;
	
		/**
		 * Plugin specific events can be raised by a plugin to communicate with
		 * other components. The event type must start from the number below
		 */
	
	public static final int	PEV_FIRST_USER_EVENT					= 1024;

	public int
	getType();
	
	public Object
	getValue();
}
