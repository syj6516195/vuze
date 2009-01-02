/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.torrent;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URL;
import java.util.*;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLSet;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.pluginsimpl.local.torrent.TorrentImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.cnetwork.ContentNetwork;
import com.aelitis.azureus.core.messenger.PlatformMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformRatingMessenger;
import com.aelitis.azureus.core.messenger.config.PlatformTorrentMessenger;

import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.torrent.Torrent;

/**
 * @author TuxPaper
 * @created Sep 27, 2006
 *
 */
public class PlatformTorrentUtils
{
	private static final long RETRY_METADATA = 10 * 60 * 1000;

	private static final long MIN_SPEED_DEFAULT = 100 * 1024;

	private static final long MIN_MD_REFRESH_MS = 1000 * 60;

	private static final long MAX_MD_REFRESH_MS = 1000L * 60 * 60 * 24 * 30;

	public static final String AELITIS_HOST_CORE	= "aelitis.com";			// needs to be lowercase

	public static final boolean DEBUG_CACHING = System.getProperty(
			"az3.debug.caching", "0").equals("1");

	private static final String TOR_AZ_PROP_MAP = "Content";

	private static final String TOR_AZ_PROP_HASH = "Content Hash";

	private static final String TOR_AZ_PROP_TITLE = "Title";

	private static final String TOR_AZ_PROP_DESCRIPTION = "Description";

	private static final String TOR_AZ_PROP_CONTENT_TYPE = "Content Type";

	private static final String TOR_AZ_PROP_AUTHOR = "Author";

	private static final String TOR_AZ_PROP_PUBLISHER = "Publisher";

	private static final String TOR_AZ_PROP_URL = "URL";

	private static final String TOR_AZ_PROP_THUMBNAIL = "Thumbnail";

	private static final String TOR_AZ_PROP_THUMBNAIL_URL = "Thumbnail.url";

	private static final String TOR_AZ_PROP_QUALITY = "Quality";

	private static final String TOR_AZ_PROP_USER_RATING = "UserRating";

	private static final String TOR_AZ_PROP_LASTUPDATED = "Revision Date";

	private static final String TOR_AZ_PROP_CREATIONDATE = "Creation Date";

	private static final String TOR_AZ_PROP_METADATA_REFRESHON = "Refresh On";

	private static final String TOR_AZ_PROP_PROGRESSIVE = "Progressive";

	private static final String TOR_AZ_PROP_SPEED = "Speed Bps";

	private static final String TOR_AZ_PROP_MIN_SPEED = "Min Speed Bps";

	private static final String TOR_AZ_PROP_DRM = "DRM";

	private static final String TOR_AZ_PROP_PURCHASED = "Purchased";

	private static final String TOR_AZ_PROP_QOS_CLASS = "QOS Class";

	private static final String TOR_AZ_PROP_CONTENT_NETWORK = "Content Network";
		
	private static final String TOR_AZ_PROP_AD_ID = "Ad ID";

	private static final String TOR_AZ_PROP_AD_ENABLED = "Ad Enabled";

	private static final String TOR_AZ_PROP_EXPIRESON = "Expires On";
	
	private static final String TOR_AZ_PROP_PRIMARY_FILE = "Primary File Index";

	private static final ArrayList metaDataListeners = new ArrayList(1);

	private static final ArrayList hasBeenOpenedListeners = new ArrayList(1);

	private static final String TOR_AZ_PROP_USE_EMP = "useEMP";
	
	private static final String TOR_AZ_PROP_FILE_METADATA = "File MetaData";
	
	private static final String TOR_AZ_PROP_WEB_AD_ENABLED = "Web Ad Enabled";

	private static final String TOR_AZ_PROP_VIDEO_WIDTH = "Video Width";

	private static final String TOR_AZ_PROP_VIDEO_HEIGHT = "Video Height";

	private static final String TOR_AZ_PROP_VIDEO_RUNNINGTIME = "Running Time";

	private static final String TOR_AZ_PROP_OPENED = "Opened";

	private static ArrayList listPlatformHosts = null;

	private static final Map mapPlatformTrackerTorrents = new WeakHashMap();

	private static boolean embeddedPlayerAvail = false;

	public static Map getContentMap(TOTorrent torrent) {
		if (torrent == null) {
			return Collections.EMPTY_MAP;
		}

		Map mapAZProps = torrent.getAdditionalMapProperty(TOTorrent.AZUREUS_PROPERTIES);

		if (mapAZProps == null) {
			mapAZProps = new HashMap();
			torrent.setAdditionalMapProperty(TOTorrent.AZUREUS_PROPERTIES, mapAZProps);
		}

		Object objExistingContentMap = mapAZProps.get(TOR_AZ_PROP_MAP);

		Map mapContent;
		if (objExistingContentMap instanceof Map) {
			mapContent = (Map) objExistingContentMap;
		} else {
			mapContent = new HashMap();
			mapAZProps.put(TOR_AZ_PROP_MAP, mapContent);
		}

		return mapContent;
	}

	static Map getTempContentMap(TOTorrent torrent) {
		if (torrent == null) {
			return new HashMap();
		}

		Map mapAZProps = torrent.getAdditionalMapProperty("attributes");

		if (mapAZProps == null) {
			mapAZProps = new HashMap();
			torrent.setAdditionalMapProperty("attributes", mapAZProps);
		}

		Object objExistingContentMap = mapAZProps.get(TOR_AZ_PROP_MAP);

		Map mapContent;
		if (objExistingContentMap instanceof Map) {
			mapContent = (Map) objExistingContentMap;
		} else {
			mapContent = new HashMap();
			mapAZProps.put(TOR_AZ_PROP_MAP, mapContent);
		}

		return mapContent;
	}

	private static String getContentMapString(TOTorrent torrent, String key) {
		if (torrent == null) {
			return null;
		}

		Map mapContent = getContentMap(torrent);
		Object obj = mapContent.get(key);

		if (obj instanceof String) {
			return (String) obj;
		} else if (obj instanceof byte[]) {
			try {
				return new String((byte[]) obj, Constants.DEFAULT_ENCODING);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return null;
	}

	private static void setContentMapString(TOTorrent torrent, String key,
			String value) {
		if (torrent == null) {
			return;
		}

		Map mapContent = getContentMap(torrent);
		mapContent.put(key, value);
	}

	private static long getContentMapLong(TOTorrent torrent, String key, long def) {
		if (torrent == null) {
			return def;
		}

		Map mapContent = getContentMap(torrent);
		Object obj = mapContent.get(key);

		try {
			if (obj instanceof Long) {
				return ((Long) obj).longValue();
			} else if (obj instanceof Integer) {
				return ((Integer) obj).longValue();
			} else if (obj instanceof String) {
				return Long.parseLong((String) obj);
			} else if (obj instanceof byte[]) {
				return Long.parseLong(new String((byte[]) obj));
			}
		} catch (Exception e) {
		}

		return def;
	}

	public static Map getContentMapMap(TOTorrent torrent, String key ){
		if ( torrent == null ){
			return( null );
		}
		
		Map mapContent = getContentMap(torrent);
		Object obj = mapContent.get(key);
		
		if ( obj instanceof Map ){
			return((Map)obj);
		}
		
		return( null );
	}
	
	private static void setContentMapLong(TOTorrent torrent, String key,
			long value) {
		if (torrent == null) {
			return;
		}

		Map mapContent = getContentMap(torrent);
		mapContent.put(key, new Long(value));
	}

	private static void setContentMapMap(TOTorrent torrent, String key,
			Map value) {
		if (torrent == null) {
			return;
		}

		Map mapContent = getContentMap(torrent);
		mapContent.put(key, value);
	}
	
	public static String getContentHash(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_HASH);
	}

	public static String getContentTitle(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_TITLE);
	}

	public static void setContentTitle(TOTorrent torrent, String title) {
		setContentMapString(torrent, TOR_AZ_PROP_TITLE, title);
	}

	public static String getContentDescription(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_DESCRIPTION);
	}

	public static String getContentType(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_CONTENT_TYPE);
	}

	public static String getContentAuthor(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_AUTHOR);
	}

	public static String getContentPublisher(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_PUBLISHER);
	}

	public static String getContentURL(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_URL);
	}

	public static String getContentQuality(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_QUALITY);
	}

	public static boolean isContentDRM(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_DRM, -1) >= 0;
	}

	public static boolean isContentPurchased(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_PURCHASED, 0) == 1;
	}


	public static long getQOSClass(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_QOS_CLASS, 0);
	}

	public static void setQOSClass(TOTorrent torrent, long cla) {
		setContentMapLong(torrent, TOR_AZ_PROP_QOS_CLASS, cla);
	}

	public static long getContentNetworkID(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_CONTENT_NETWORK, ContentNetwork.CONTENT_NETWORK_VUZE );
	}

	public static void setContentNetworkID(TOTorrent torrent, long cnet) {
		setContentMapLong(torrent, TOR_AZ_PROP_CONTENT_NETWORK, cnet);
	}
	
	private static void putOrRemove(Map map, String key, Object obj) {
		if (obj == null || obj.equals(null)) {
			map.remove(key);
		} else {
			map.put(key, obj);
		}
	}

	public static void setContentQuality(TOTorrent torrent, String sQualityID) {
		Map mapContent = getContentMap(torrent);
		putOrRemove(mapContent, TOR_AZ_PROP_QUALITY, sQualityID);

		writeTorrentIfExists(torrent);
	}

	private static void writeTorrentIfExists(TOTorrent torrent) {
		AzureusCore core = AzureusCoreFactory.getSingleton();
		if (core == null || !core.isStarted()) {
			return;
		}

		GlobalManager gm = core.getGlobalManager();
		if (gm == null || gm.getDownloadManager(torrent) == null) {
			return;
		}

		try {
			TorrentUtils.writeToFile(torrent);
		} catch (TOTorrentException e) {
			Debug.out(e);
		}
	}

	public static byte[] getContentThumbnail(TOTorrent torrent) {
		Map mapContent = getContentMap(torrent);
		Object obj = mapContent.get(TOR_AZ_PROP_THUMBNAIL);

		if (obj instanceof byte[]) {
			return (byte[]) obj;
		}

		return null;
	}

	public static String getContentThumbnailUrl(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_THUMBNAIL_URL);
	}

	public static void setContentThumbnail(TOTorrent torrent, byte[] thumbnail) {
		Map mapContent = getContentMap(torrent);
		putOrRemove(mapContent, TOR_AZ_PROP_THUMBNAIL, thumbnail);

		writeTorrentIfExists(torrent);
	}

	public static void setUserRating(TOTorrent torrent, int rating) {
		if (torrent == null || getUserRating(torrent) == rating) {
			return;
		}
		Map mapContent = getTempContentMap(torrent);
		mapContent.put(TOR_AZ_PROP_USER_RATING, new Long(rating));
		writeTorrentIfExists(torrent);
		RatingInfoList ratingReply = new SingleUserRatingInfo(torrent);
		PlatformRatingMessenger.invokeUpdateListeners(ratingReply);
	}

	public static void removeUserRating(TOTorrent torrent) {
		Map mapContent = getTempContentMap(torrent);
		if (mapContent.remove(TOR_AZ_PROP_USER_RATING) != null) {
			writeTorrentIfExists(torrent);
			RatingInfoList ratingReply = new SingleUserRatingInfo(torrent);
			PlatformRatingMessenger.invokeUpdateListeners(ratingReply);
		}
	}

	/**
	 * 
	 * @param torrent
	 * @return -1: No rating
	 */
	public static int getUserRating(TOTorrent torrent) {
		Map mapContent = getTempContentMap(torrent);
		Long l = (Long) mapContent.get(TOR_AZ_PROP_USER_RATING);
		if (l == null) {
			return -1;
		}
		return l.intValue();
	}

	public static long getMetaDataRefreshOn(TOTorrent torrent) {
		Map mapContent = getTempContentMap(torrent);
		Long l = (Long) mapContent.get(TOR_AZ_PROP_METADATA_REFRESHON);
		if (l == null) {
			return 0;
		}
		return l.longValue();
	}

	public static void setMetaDataRefreshOn(TOTorrent torrent, long refreshOn) {
		Map mapContent = getTempContentMap(torrent);
		mapContent.put(TOR_AZ_PROP_METADATA_REFRESHON, new Long(refreshOn));
		writeTorrentIfExists(torrent);
	}

	public static boolean isContent(TOTorrent torrent,
			boolean requirePlatformTracker) {
		if (torrent == null) {
			return false;
		}
		boolean bContent = PlatformTorrentUtils.getContentHash(torrent) != null;
		if (!bContent || (bContent && !requirePlatformTracker)) {
			return bContent;
		}

		return isPlatformTracker(torrent);
	}

	public static boolean isContent(Torrent torrent,
			boolean requirePlatformTracker) {
		if (torrent instanceof TorrentImpl) {
			return isContent(((TorrentImpl) torrent).getTorrent(),
					requirePlatformTracker);
		}
		return false;
	}

	public static List getPlatformHosts() {
		if (listPlatformHosts == null) {
			listPlatformHosts = new ArrayList();
			for (int i = 0; i < Constants.AZUREUS_DOMAINS.length; i++) {
				listPlatformHosts.add(Constants.AZUREUS_DOMAINS[i].toLowerCase());
			}
		}
		return listPlatformHosts;
	}

	public static void addPlatformHost(String host) {
		List platformHosts = getPlatformHosts();
		host = host.toLowerCase();

		if (!platformHosts.contains(host)) {
			platformHosts.add(host);
			mapPlatformTrackerTorrents.clear();
		}
	}

	public static boolean isPlatformHost(String host) {
		Object[] domains = getPlatformHosts().toArray();

		host = host.toLowerCase();

		for (int i = 0; i < domains.length; i++) {

			String domain = (String) domains[i];

			if (domain.equals(host)) {

				return (true);
			}

			if (host.endsWith("." + domain)) {

				return (true);
			}
		}

		if ( Constants.isCVSVersion()){
		
				// allow local addresses for testing
			
			try{
				InetAddress ia = InetAddress.getByName( host );
			
				return( ia.isLoopbackAddress() || ia.isLinkLocalAddress() || ia.isSiteLocalAddress());
				
			}catch( Throwable e ){
			}
		}
		
		return (false);
	}

	public static boolean isPlatformTracker(TOTorrent torrent) {
		try {
			if (torrent == null) {

				return false;
			}

			Object oCache = mapPlatformTrackerTorrents.get(torrent);
			if (oCache instanceof Boolean) {
				return ((Boolean) oCache).booleanValue();
			}

			// check them all incase someone includes one of our trackers in a multi-tracker
			// torrent

			URL announceURL = torrent.getAnnounceURL();

			if (announceURL != null) {

				if (!isPlatformHost(announceURL.getHost())) {

					mapPlatformTrackerTorrents.put(torrent, new Boolean(false));
					return (false);
				}
			}

			TOTorrentAnnounceURLSet[] sets = torrent.getAnnounceURLGroup().getAnnounceURLSets();

			for (int i = 0; i < sets.length; i++) {

				URL[] urls = sets[i].getAnnounceURLs();

				for (int j = 0; j < urls.length; j++) {

					if (!isPlatformHost(urls[j].getHost())) {

						mapPlatformTrackerTorrents.put(torrent, new Boolean(false));
						return (false);
					}
				}
			}

			boolean b = announceURL != null;
			mapPlatformTrackerTorrents.put(torrent, new Boolean(b));
			return b;

		} catch (Throwable e) {

			Debug.printStackTrace(e);

			mapPlatformTrackerTorrents.put(torrent, new Boolean(false));
			return (false);
		}
	}

	public static boolean isPlatformTracker(Torrent torrent) {
		if (torrent instanceof TorrentImpl) {
			return isPlatformTracker(((TorrentImpl) torrent).getTorrent());
		}
		return false;
	}
	
	public static boolean isUpdateDM(DownloadManager dm) {
		Boolean oisUpdate = (Boolean) dm.getUserData("isUpdate");
		if (oisUpdate != null) {
			return oisUpdate.booleanValue();
		}

		boolean isUpdate = true;
		TOTorrent torrent = dm.getTorrent();
		if (torrent == null) {
			isUpdate = false;
		} else {
			URL announceURL = torrent.getAnnounceURL();

			if (announceURL != null) {
				if (announceURL.getHost().indexOf(AELITIS_HOST_CORE) == -1) {
					isUpdate = false;
				}
			}
			
			if (isUpdate) {
				TOTorrentAnnounceURLSet[] sets = torrent.getAnnounceURLGroup().getAnnounceURLSets();

				for (int i = 0; i < sets.length; i++) {

					URL[] urls = sets[i].getAnnounceURLs();

					for (int j = 0; j < urls.length; j++) {

						if (urls[j].getHost().indexOf(AELITIS_HOST_CORE) == -1) {
							isUpdate = false;
							break;
						}
					}
				}
			}
		}

		dm.setUserData("isUpdate", new Boolean(isUpdate));
		return isUpdate;
	}

	public static String getAdId(TOTorrent torrent) {
		return getContentMapString(torrent, TOR_AZ_PROP_AD_ID);
	}

	public static void setAdId(TOTorrent torrent, String sID) {
		Map mapContent = getContentMap(torrent);
		putOrRemove(mapContent, TOR_AZ_PROP_AD_ID, sID);

		writeTorrentIfExists(torrent);
	}


	/**
	 * @param torrent
	 * @param maxDelayMS TODO
	 *
	public static void updateMetaData(final TOTorrent[] torrents, long maxDelayMS) {
		if (torrents == null) {
			log("no torrents to update MD on");
			return;
		}

		log("update " + torrents.length + " MD");

		PlatformTorrentMessenger.getMetaData(torrents, maxDelayMS,
				new PlatformTorrentMessenger.GetMetaDataReplyListener() {

			public void messageSent() {
			}

			public void replyReceived(String replyType, Map mapHashes) {
				for (Iterator iter = mapHashes.keySet().iterator(); iter.hasNext();) {
					String hash = (String) iter.next();
					updateMetaData_handleReply(null, hash, replyType, mapHashes);
				}
			}
		});
	}
*/
	/**
	 * @param torrent
	 * @param maxDelayMS TODO
	 */
	public static void updateMetaData(final TOTorrent torrent, long maxDelayMS) {
		if (!isContent(torrent, true)) {
			log(torrent, "torrent " + new String(torrent.getName())
					+ " not az content");
			return;
		}

		log(torrent, "updateMD");

		PlatformTorrentMessenger.getMetaData(getContentNetworkID(torrent),
				new TOTorrent[] {
					torrent
				}, maxDelayMS, new PlatformTorrentMessenger.GetMetaDataReplyListener() {

					public void messageSent() {
					}

					public void replyReceived(String replyType, Map mapHashes) {
						updateMetaData_handleReply(torrent, null, replyType, mapHashes);
					}
				});
	}

	private static void updateMetaData_handleReply(TOTorrent torrent,
			String hash, String replyType, Map mapHashes) {
		if (hash == null && torrent != null) {
			try {
				hash = torrent.getHashWrapper().toBase32String();
			} catch (Exception e) {
			}
		}

		GlobalManager gm = AzureusCoreFactory.getSingleton().getGlobalManager();
		DownloadManager dm = gm.getDownloadManager(new HashWrapper(Base32.decode(hash)));

		if (torrent == null && dm != null) {
			torrent = dm.getTorrent();
		}
		Map contentMap = PlatformTorrentUtils.getContentMap(torrent);

		final TOTorrent torrentFinal = torrent;
		
		if (replyType.equals(PlatformMessenger.REPLY_EXCEPTION)) {
			if (torrent != null) {
  			// try again in a bit
  			log(torrent, "Exception, retrying later (~" + RETRY_METADATA + "ms)");
  			SimpleTimer.addEvent("Update MD Retry", SystemTime.getCurrentTime()
  					+ RETRY_METADATA, new TimerEventPerformer() {
  				public void perform(TimerEvent event) {
  					log(torrentFinal, "retry time");
  					PlatformTorrentUtils.updateMetaData(torrentFinal, 15000);
  				}
  			});
			}
		} else {
			Map jsonMapMetaData = hash == null ? null : (Map) mapHashes.get(hash);
			if (jsonMapMetaData != null && !jsonMapMetaData.isEmpty()) {
				long oldLastUpdated = getContentLastUpdated(torrent);
				long expireyMins = 0;

				for (Iterator iter = jsonMapMetaData.keySet().iterator(); iter.hasNext();) {
					String key = (String) iter.next();
					Object value = jsonMapMetaData.get(key);

					if (value == null || value.equals(null)) {
						contentMap.remove(key);
					} else if ((key.equals("Thumbnail") || key.endsWith(".B64"))
							&& value instanceof String) {
						contentMap.put(key, Base64.decode((String) value));
					} else if (key.equals("expires-in-mins") && value instanceof Long) {
						expireyMins = ((Long) value).longValue();
					} else {
						contentMap.put(key, value);
					}
					writeTorrentIfExists(torrent);
				}

				// crappy way of updating the display name
				if (dm != null) {
					String title = PlatformTorrentUtils.getContentTitle(torrent);
					if (title != null && title.length() > 0
							&& dm.getDownloadState().getDisplayName() == null) {
						dm.getDownloadState().setDisplayName(title);
					}
				}
				triggerMetaDataUpdateListeners(torrent);

				if (torrent != null) {
					// setup next refresh

  				long refreshOn;
  				if (expireyMins > 0) {
  					refreshOn = SystemTime.getCurrentTime() + (expireyMins * 60 * 1000L);
  				} else {
  					long newLastUpdated = getContentLastUpdated(torrent);
  
  					long diff = newLastUpdated - oldLastUpdated;
  					log(torrent, "Last Updated: new " + new Date(newLastUpdated)
  							+ ";old " + new Date(oldLastUpdated) + ";diff=" + diff);
  					if (diff > 0 && oldLastUpdated != 0) {
  						diff *= 2;
  						if (diff < MIN_MD_REFRESH_MS) {
  							diff = MIN_MD_REFRESH_MS;
  						} else if (diff > MAX_MD_REFRESH_MS) {
  							diff = MAX_MD_REFRESH_MS;
  						}
  						refreshOn = SystemTime.getOffsetTime(diff);
  					} else {
  						refreshOn = SystemTime.getCurrentTime()
  								+ (7 * 24 * 60 * 60 * 1000L);
  					}
  				}
  
  				log(torrent, "got MD. Next refresh in "
  						+ (refreshOn - SystemTime.getCurrentTime()));
  				setMetaDataRefreshOn(torrent, refreshOn);
  				SimpleTimer.addEvent("Update MD", refreshOn, new TimerEventPerformer() {
  					public void perform(TimerEvent event) {
  						PlatformTorrentUtils.updateMetaData(torrentFinal, 15000);
  					}
  				});
				}
			} else if (torrent != null) {
				long refreshOn = SystemTime.getCurrentTime()
						+ (30 * 24 * 60 * 60 * 1000L);
				setMetaDataRefreshOn(torrent, refreshOn);
				log(torrent, "no hash in reply. Next refresh on " + new Date(refreshOn));
			}
		}
	}

	public static void addListener(MetaDataUpdateListener l) {
		if (metaDataListeners.indexOf(l) < 0) {
			metaDataListeners.add(l);
		}
	}

	public static void removeListener(MetaDataUpdateListener l) {
		metaDataListeners.remove(l);
	}

	public static void triggerMetaDataUpdateListeners(TOTorrent torrent) {
		if (torrent == null) {
			return;
		}
		MetaDataUpdateListener[] listeners = (MetaDataUpdateListener[]) metaDataListeners.toArray(new MetaDataUpdateListener[0]);
		for (int i = 0; i < listeners.length; i++) {
			MetaDataUpdateListener listener = listeners[i];
			try {
				listener.metaDataUpdated(torrent);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	public static long getContentLastUpdated(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_LASTUPDATED, 0);
	}

	public static void setContentLastUpdated(TOTorrent torrent, long lastUpdate) {
		setContentMapLong(torrent, TOR_AZ_PROP_LASTUPDATED, lastUpdate);
	}

	public static boolean isContentProgressive(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_PROGRESSIVE, 0) == 1;
	}

	public static long getContentStreamSpeedBps(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_SPEED, 0);
	}

	public static long getContentMinimumSpeedBps(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_MIN_SPEED, MIN_SPEED_DEFAULT);
	}

	/**
	 * If either Ad Enabled or Web Ad Enabled is set return true.
	 * @param torrent
	 * @return - boolean - true if EITHER "Ad Enabled"==1  OR  "Web Ad Enabled"==1
	 */
	public static boolean isContentAdEnabled(TOTorrent torrent) {
		return (getContentMapLong(torrent, TOR_AZ_PROP_AD_ENABLED, 0) == 1 ||
				getContentMapLong(torrent, TOR_AZ_PROP_WEB_AD_ENABLED, 0) == 1);
	}

	public static boolean isContentUnitAdEnabled(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_WEB_AD_ENABLED, 0) == 1;
	}

	public static boolean isContentWebAdEnabled(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_WEB_AD_ENABLED, 0) == 1;
	}

	public static boolean useEMP(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_USE_EMP, 0) == 1;
	}
	
	public static void setUseEMP(TOTorrent torrent, boolean useEMP) {
		setContentMapLong(torrent, TOR_AZ_PROP_USE_EMP, useEMP ? 1 : 0);
	}
	
	public static void setFileMetaData(TOTorrent torrent, Map map) {
		setContentMapMap(torrent, TOR_AZ_PROP_FILE_METADATA, map );
	}
	
	public static Map getFileMetaData(TOTorrent torrent ){
		return getContentMapMap( torrent, TOR_AZ_PROP_FILE_METADATA );
	}
	
	public static long getExpiresOn(TOTorrent torrent) {
		Map mapContent = getContentMap(torrent);
		Long l = (Long) mapContent.get(TOR_AZ_PROP_EXPIRESON);
		if (l == null) {
			return 0;
		}
		return l.longValue();
	}

	public static void setExpiresOn(TOTorrent torrent, long expiresOn) {
		Map mapContent = getContentMap(torrent);
		mapContent.put(TOR_AZ_PROP_EXPIRESON, new Long(expiresOn));
		writeTorrentIfExists(torrent);
	}

	public static int getContentPrimaryFileIndex(TOTorrent torrent ){
		return (int)getContentMapLong(torrent, TOR_AZ_PROP_PRIMARY_FILE, -1 );
	}
	
	
	private static long getContentVideoWidth(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_VIDEO_WIDTH, -1);
	}

	private static long getContentVideoHeight(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_VIDEO_HEIGHT, -1);
	}

	public static long getContentVideoRunningTime(TOTorrent torrent) {
		return getContentMapLong(torrent, TOR_AZ_PROP_VIDEO_RUNNINGTIME, -1);
	}
	
	public static int[] getContentVideoResolution(TOTorrent torrent) {
		long width = getContentVideoWidth(torrent);
		if (width <= 0) {
			return null;
		}
		long height = getContentVideoWidth(torrent);
		if (height <= 0) {
			return null;
		}
		return new int[] { (int) width, (int) height };
	}

	
	public static void log(String str) {
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("v3.MD");
		diag_logger.log(str);
		if (DEBUG_CACHING) {
			System.out.println(Thread.currentThread().getName() + "|"
					+ System.currentTimeMillis() + "] " + str);
		}
	}

	/**
	 * @param torrent
	 * @param string
	 *
	 * @since 3.0.1.5
	 */
	public static void log(TOTorrent torrent, String string) {
		String hash = "";
		try {
			hash = torrent.getHashWrapper().toBase32String();
		} catch (Exception e) {
		}
		log(hash + "] " + string);
	}

	public static boolean embeddedPlayerAvail() {
		// cache true, always recheck false in case plugin installs.
		if (embeddedPlayerAvail) {
			return true;
		}

		try {
			PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
					"azemp", true);
			if (pi != null) {
				embeddedPlayerAvail = true;
			}
		} catch (Throwable e1) {
		}

		return embeddedPlayerAvail;
	}

	/**
	 * @param torrent
	 *
	 * @since 3.0.4.3
	 */
	public static String getContentTitle2(DownloadManager dm) {
		if (dm == null) {
			return null;
		}
		// DM state's display name can be set by user, so show that if we have it
		String name = dm.getDownloadState().getDisplayName();
		if (name == null || name.length() == 0) {
			name = PlatformTorrentUtils.getContentTitle(dm.getTorrent());
			if (name == null) {
				name = dm.getDisplayName();
			}
		}
		return name;
	}
	
	public static void setHasBeenOpened(DownloadManager dm, boolean opened) {
		TOTorrent torrent = dm.getTorrent();
		if (torrent == null) {
			return;
		}
		if (opened == getHasBeenOpened(dm)) {
			return;
		}
		setContentMapLong(torrent, TOR_AZ_PROP_OPENED, opened ? 1 : 0);
		writeTorrentIfExists(torrent);
		Object[] array = hasBeenOpenedListeners.toArray();
		for (int i = 0; i < array.length; i++) {
			try {
				HasBeenOpenedListener l = (HasBeenOpenedListener) array[i];
				l.hasBeenOpenedChanged(dm, opened);
			} catch (Exception e) {
				Debug.out(e);
			}
		}
	}

	public static boolean getHasBeenOpened(DownloadManager dm) {
		TOTorrent torrent = dm.getTorrent();
		if (torrent == null) {
			return true;
		}
		boolean opened = getContentMapLong(torrent, TOR_AZ_PROP_OPENED, -1) > 0;
		if (opened || getAdId(torrent) != null || isUpdateDM(dm)) {
			return true;
		}
		
		return false;
	}

	public static void addHasBeenOpenedListener(HasBeenOpenedListener l) {
		hasBeenOpenedListeners.add(l);
	}
}
