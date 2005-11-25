/*
 * Created on 22 juil. 2003
 *
 */
package org.gudy.azureus2.core3.tracker.client.impl;

/**
 * 
 * @author Olivier
 * @author TuxPaper
 */


import org.gudy.azureus2.core3.tracker.client.*;

public abstract class 
TRTrackerScraperResponseImpl 
  implements TRTrackerScraperResponse
{
	private byte[]  hash;
	private int     seeds;
	private int     peers;
  
  private long scrapeStartTime;
  private long nextScrapeStartTime;
  private String sStatus = "";
  private String sLastStatus = "";
  private int status;
  private int last_status;

  protected 
  TRTrackerScraperResponseImpl(                                     
  		byte[] _hash ) 
  {
    this( _hash, -1, -1, -1);
  }

  protected 
  TRTrackerScraperResponseImpl(
     byte[] _hash,
     int  _seeds, 
     int  _peers,
     long _scrapeStartTime)  
  {
    hash = _hash;
    seeds = _seeds;
    peers = _peers;

    scrapeStartTime = _scrapeStartTime;
    
    status = (!isValid()) ? TRTrackerScraperResponse.ST_INITIALIZING : TRTrackerScraperResponse.ST_ONLINE;
    nextScrapeStartTime = -1;
  }

  public byte[] getHash() {
    return hash;
  }
    

  public int getSeeds() {
    return seeds ;
  }
  
  public void
  setSeeds(
  	int		s )
  {
  	seeds	= s;
  }
  
  public int getPeers() {
    return peers;
  }
  
  public void
  setPeers(
  	int	p )
  {
  	peers	= p;
  }


  public int getStatus() {
    return status;
  }
  
  public void
  setStatus(
  	int	s )
  {
  	status	= s;
  }
  
  protected void
  setStatus(
	String	str )
  {
	  sStatus	= str;
  }
  
  public void setStatus(int iNewStatus, String sNewStatus) {
    if (last_status != status && iNewStatus != status)
      last_status = status;
    if (iNewStatus == TRTrackerScraperResponse.ST_ONLINE) {
      status = (!isValid()) ? TRTrackerScraperResponse.ST_INITIALIZING : TRTrackerScraperResponse.ST_ONLINE;
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
  
  public void revertStatus() {
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

  /**
   * Calculate Scrape interval, applying internal min/max limits and default
   * calculations.
   * 
   * @param iRecIntervalSecs Recommended Interval in Seconds, or 0 for no
   *                          recommendation
   * @param iNumSeeds        # of seeds torrent has, used to calculate scrape
   *                          interval
   * @return Calculated interval in Seconds
   */
	public static int calcScrapeIntervalSecs(int iRecIntervalSecs, int iNumSeeds) {
		final int MIN = 15 * 60;
		final int MAX = 3 * 60 * 60;

		// Min 15 min, plus 10 seconds for every seed
		// ex. 10 Seeds = 15m + 100s = ~16.66m
		// 60 seeds = 15m + 600s = ~25m
		// 1000 seeds = 15m + 10000s = ~2h 52m
		int scrapeInterval = MIN + (iNumSeeds * 10);
		if (iRecIntervalSecs > scrapeInterval)
			scrapeInterval = iRecIntervalSecs;

		if (scrapeInterval > MAX)
			scrapeInterval = MAX;

		return scrapeInterval;
	}
}
