/*
 * Created on 22 juil. 2003
 *
 */
package org.gudy.azureus2.core3.tracker.client.classic;

/**
 * 
 * @author Olivier
 * @author TuxPaper
 */

import java.net.URL;

import org.gudy.azureus2.core3.tracker.client.*;

public class TRTrackerScraperResponseImpl 
  implements TRTrackerScraperResponse
{
  protected byte[]  hash;
  protected int     seeds;
  protected int     peers;
  
  private long scrapeStartTime;
  private long nextScrapeStartTime;
  private TrackerStatus ts;
  private String sStatus = "";
  private String sLastStatus = "";
  private int status;
  private int last_status;

  protected TRTrackerScraperResponseImpl(TrackerStatus _ts,
                                         byte[] _hash) {
    this(_ts, _hash, -1, -1, -1);
  }

  protected TRTrackerScraperResponseImpl(TrackerStatus _ts,
                                         byte[] _hash,
                                         int  _seeds, 
                                         int  _peers,
                                         long _scrapeStartTime)  {
    hash = _hash;
    seeds = _seeds;
    peers = _peers;
    ts = _ts;
    scrapeStartTime = _scrapeStartTime;
    
    status = (isValid()) ? TRTrackerScraperResponse.ST_INITIALIZING : TRTrackerScraperResponse.ST_ONLINE;
    nextScrapeStartTime = -1;
  }

  public byte[] getHash() {
    return hash;
  }
    
  public TrackerStatus getTrackerStatus() {
    return ts;
  }

  public int getSeeds() {
    return seeds ;
  }
  
  public int getPeers() {
    return peers;
  }
  
	public void setSeedsPeers(int iSeeds, int iPeers) {
	  seeds = iSeeds;
	  peers = iPeers;
	  status = (isValid()) ? TRTrackerScraperResponse.ST_INITIALIZING : TRTrackerScraperResponse.ST_ONLINE;
    
    // XXX Is this a good idea?
    ts.scrapeReceived(this);
	}

  public int getStatus() {
    return status;
  }
  
  protected void setStatus(int iNewStatus, String sNewStatus) {
    if (last_status != status && iNewStatus != status)
      last_status = status;
    if (iNewStatus == TRTrackerScraperResponse.ST_ONLINE) {
      status = (isValid()) ? TRTrackerScraperResponse.ST_INITIALIZING : TRTrackerScraperResponse.ST_ONLINE;
    } else {
      status = iNewStatus;
    }
    
    if (sNewStatus == null)
      return;

    if (!sLastStatus.equals(sStatus)) {
      sLastStatus = sStatus;
    }
    sStatus = sNewStatus;
  }
  
  protected void revertStatus() {
    status = last_status;
    sStatus = sLastStatus;
  }
  
  public void 
  setScrapeStartTime(long time) 
  {
    scrapeStartTime = time;
  }
    
  
  public long 
  getScrapeStartTime() 
  {
    return scrapeStartTime;
  }

  public long getNextScrapeStartTime() {
    return nextScrapeStartTime;
  }
 
  public void setNextScrapeStartTime(long _nextScrapeStartTime) {
    nextScrapeStartTime = _nextScrapeStartTime;
  }
   
  public String getStatusString() {
    return sStatus;
  }
  
  public boolean isValid() {
    return !(seeds == -1 && peers == -1);
  }
  
  public URL
  getURL()
  {
  	return( ts.getTrackerURL());
  }
}
