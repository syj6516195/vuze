/*
 * File    : PeersTableExtensions.java
 * Created : 29 nov. 2003
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
 
package org.gudy.azureus2.plugins.ui.tables.impl;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.plugins.ui.tables.PluginPeerItemFactory;

/**
 * @author Olivier
 *
 */
public class PeersTableExtensions {

  private static PeersTableExtensions instance;
  private Map items;
  
  private PeersTableExtensions() {
   items = new HashMap();
  }
  
  public static synchronized PeersTableExtensions getInstance() {
    if(instance == null)
      instance = new PeersTableExtensions();
    return instance;
  }
  
  public void addExtension(String name,PluginPeerItemFactory item) {
    synchronized(items) {
      items.put(name,item);
    }
  }
  
  public Map getExtensions() {
    synchronized(items) {
      return new HashMap(items);
    }
  }

}
