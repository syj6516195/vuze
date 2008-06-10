/**
 * Created on Apr 14, 2008
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.buddy.impl;

import java.io.UnsupportedEncodingException;
import java.util.*;

import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.UrlUtils;

import com.aelitis.azureus.activities.VuzeActivitiesEntry;
import com.aelitis.azureus.activities.VuzeActivitiesEntryContentShare;
import com.aelitis.azureus.buddy.VuzeBuddy;
import com.aelitis.azureus.login.NotLoggedInException;
import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddy;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentV3;
import com.aelitis.azureus.util.*;

/**
 * BuddyPluginBuddy plus some vuze specific stuff
 * 
 * @author TuxPaper
 * @created Apr 14, 2008
 *
 */
/**
 * @author TuxPaper
 * @created May 11, 2008
 *
 */
public class VuzeBuddyImpl
	implements VuzeBuddy
{
	protected String displayName;

	private String loginID;

	private String code;

	private long lastUpdated;

	private long createdOn;

	private byte[] avatar;

	private String avatarURL;

	private List pluginBuddies = new ArrayList();

	private AEMonitor mon_pluginBuddies = new AEMonitor("pluginBuddies");

	protected VuzeBuddyImpl(String publicKey) {
		addPublicKey(publicKey);
	}

	protected VuzeBuddyImpl() {
	}

	public void loadFromMap(Map mapNewBuddy) {
		setDisplayName(MapUtils.getMapString(mapNewBuddy, "display-name", ""
				+ mapNewBuddy.hashCode()));
		setLoginID(MapUtils.getMapString(mapNewBuddy, "login-id", ""
				+ mapNewBuddy.hashCode()));

		List pkList = MapUtils.getMapList(mapNewBuddy, "pks",
				Collections.EMPTY_LIST);
		for (Iterator iter = pkList.iterator(); iter.hasNext();) {
			Object o = iter.next();
			String pk = null;
			if (o instanceof byte[]) {
				try {
					pk = new String((byte[]) o, "utf-8");
				} catch (UnsupportedEncodingException e) {
				}
			} else if (o instanceof String) {
				pk = (String) o;
			}
			if (pk != null) {
				addPublicKey(pk);
			}
		}

		// first try to get the avatar via raw bytes
		byte[] newAvatar = MapUtils.getMapByteArray(mapNewBuddy, "avatar", null);
		if (newAvatar != null) {
			setAvatar(newAvatar);
		}
		
		String newAvatarURL = MapUtils.getMapString(mapNewBuddy, "avatar.url", null);
		if (!StringCompareUtils.equals(newAvatarURL, avatarURL) || avatar == null) {
			avatarURL = newAvatarURL;
			if (avatarURL != null) {
				ImageDownloader.loadImage(avatarURL,
						new ImageDownloader.ImageDownloaderListener() {
							public void imageDownloaded(byte[] image) {
								VuzeBuddyManager.log("Got new avatar! " + toDebugString());
								setAvatar(image);
							}
						});
			}
		}

		
		setCode(MapUtils.getMapString(mapNewBuddy, "code", null));
		setCreatedOn(MapUtils.getMapLong(mapNewBuddy, "created-on", 0));
	}

	public Map toMap() {
		Map map = new HashMap();
		map.put("display-name", displayName);
		map.put("login-id", loginID);
		map.put("code", code);
		map.put("created-on", new Long(createdOn));

		List pks = Arrays.asList(getPublicKeys());
		map.put("pks", pks);

		map.put("avatar.url", avatarURL);
		map.put("avatar", avatar);

		return map;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		if (displayName == null) {
			displayName = "";
		}
		if (displayName.equals(this.displayName)) {
			return;
		}
		this.displayName = displayName;
		VuzeBuddyManager.triggerOrderChangedListener();
	}

	public String getLoginID() {
		return loginID;
	}

	public void setLoginID(String loginID) {
		this.loginID = loginID;
	}

	public long getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(long lastUpdated) {
		boolean trigger = (this.lastUpdated > 0);
		this.lastUpdated = lastUpdated;
		if (trigger) {
			VuzeBuddyManager.triggerChangeListener(this);
		}
	}

	public byte[] getAvatar() {
		return avatar;
	}

	public void setAvatar(byte[] avatar) {
		this.avatar = avatar;
		VuzeBuddyManager.triggerChangeListener(this);
	}

	public boolean isOnline() {
		mon_pluginBuddies.enter();
		try {
			for (Iterator iter = pluginBuddies.iterator(); iter.hasNext();) {
				BuddyPluginBuddy pluginBuddy = (BuddyPluginBuddy) iter.next();
				if (pluginBuddy.isOnline()) {
					return true;
				}
			}
		} finally {
			mon_pluginBuddies.exit();
		}
		return false;
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#addPublicKey()
	public void addPublicKey(String pk) {
		// We add public keys by adding BuddyPluginBuddy
		mon_pluginBuddies.enter();
		try {
			BuddyPluginBuddy pluginBuddy = VuzeBuddyManager.getBuddyPluginBuddyForVuze(pk);

			if (pluginBuddy != null && !pluginBuddies.contains(pluginBuddy)) {
				pluginBuddies.add(pluginBuddy);
			}

		} finally {
			mon_pluginBuddies.exit();
		}

		VuzeBuddyManager.linkPKtoBuddy(pk, this);
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#removePublicKey(java.lang.String)
	public void removePublicKey(String pk) {
		// our public key list is actually a BuddyPluginBuddy list, so find
		// it in our list and remove it
		mon_pluginBuddies.enter();
		try {
			for (Iterator iter = pluginBuddies.iterator(); iter.hasNext();) {
				BuddyPluginBuddy pluginBuddy = (BuddyPluginBuddy) iter.next();
				if (pluginBuddy.getPublicKey().equals(pk)) {
					iter.remove();
					if (pluginBuddy.getSubsystem() == BuddyPlugin.SUBSYSTEM_AZ3) {
						VuzeBuddyManager.log("Remove pk " + pk);
						pluginBuddy.remove();
					} else {
						VuzeBuddyManager.log("Can't remove pk as it's not az3: " + pk);
					}
				}
			}
		} finally {
			mon_pluginBuddies.exit();
		}
	}

	public String[] getPublicKeys() {
		mon_pluginBuddies.enter();
		try {
			String[] ret = new String[pluginBuddies.size()];
			int x = 0;

			for (Iterator iter = pluginBuddies.iterator(); iter.hasNext();) {
				BuddyPluginBuddy pluginBuddy = (BuddyPluginBuddy) iter.next();
				if (pluginBuddy != null) {
					ret[x++] = pluginBuddy.getPublicKey();
				}
			}
			return ret;
		} finally {
			mon_pluginBuddies.exit();
		}
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#sendActivity(com.aelitis.azureus.util.VuzeActivitiesEntry)
	public void sendActivity(VuzeActivitiesEntry entry)
			throws NotLoggedInException {
		BuddyPluginBuddy[] buddies = (BuddyPluginBuddy[]) pluginBuddies.toArray(new BuddyPluginBuddy[0]);
		VuzeBuddyManager.sendActivity(entry, buddies);
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#sendPayloadMap(java.util.Map)
	public void sendPayloadMap(Map map)
			throws NotLoggedInException {
		BuddyPluginBuddy[] buddies = (BuddyPluginBuddy[]) pluginBuddies.toArray(new BuddyPluginBuddy[0]);
		VuzeBuddyManager.sendPayloadMap(map, buddies);
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#shareDownload(com.aelitis.azureus.ui.swt.currentlyselectedcontent.CurrentContent, java.lang.String)
	public void shareDownload(SelectedContentV3 content, String message)
			throws NotLoggedInException {
		if (content == null) {
			return;
		}

		VuzeActivitiesEntryContentShare entry;
		entry = new VuzeActivitiesEntryContentShare(content, message);
		entry.setBuddy(this);

		sendActivity(entry);
	}

	public void tellBuddyToSyncUp()
			throws NotLoggedInException {
		Map map = new HashMap();
		map.put(VuzeBuddyManager.VUZE_MESSAGE_TYPE, VuzeBuddyManager.VMT_BUDDYSYNC);

		sendPayloadMap(map);
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getProfileAHREF(String referer) {
		StringBuffer buf = new StringBuffer();

		buf.append("<A HREF=\"");
		buf.append(Constants.URL_PREFIX);
		buf.append(Constants.URL_PROFILE);
		buf.append(UrlUtils.encode(loginID));
		buf.append("?");
		buf.append(Constants.URL_SUFFIX);
		buf.append("&client_ref=");
		buf.append(UrlUtils.encode(referer));
		buf.append("\" TITLE=\"");
		buf.append(displayName);
		if (!loginID.equals(displayName)) {
			buf.append(" (");
			buf.append(loginID);
			buf.append(")");
		}
		buf.append("\">");
		buf.append(displayName);
		buf.append("</A>");
		return buf.toString();
	}

	public String getProfileUrl(String referer) {
		return Constants.URL_PREFIX + Constants.URL_PROFILE + getLoginID() + "?"
				+ Constants.URL_SUFFIX + "&client_ref=" + referer;
	}

	// @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	public int compare(Object arg0, Object arg1) {
		if (!(arg0 instanceof VuzeBuddy) || !(arg1 instanceof VuzeBuddy)) {
			return 0;
		}

		String c0 = ((VuzeBuddy) arg0).getDisplayName();
		String c1 = ((VuzeBuddy) arg1).getDisplayName();

		if (c0 == null) {
			c0 = "";
		}
		if (c1 == null) {
			c1 = "";
		}
		return c0.compareToIgnoreCase(c1);
	}

	// @see java.lang.Comparable#compareTo(java.lang.Object)
	public int compareTo(Object arg0) {
		return compare(this, arg0);
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#setCreatedOn(long)
	public void setCreatedOn(long createdOn) {
		this.createdOn = createdOn;
	}

	// @see com.aelitis.azureus.buddy.VuzeBuddy#getCreatedOn()
	public long getCreatedOn() {
		return createdOn;
	}
	
	// @see com.aelitis.azureus.buddy.VuzeBuddy#toDebugString()
	public String toDebugString() {
		return "Buddy {" + loginID + "}";
	}
}
