/*
 * Created on 09.11.2003
 * 
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gudy.azureus2.ui.web2.stages.hdapi.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import seda.sandStorm.core.BufferElement;

import HTML.Template;

import org.apache.log4j.spi.LoggingEvent;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPeerManagerStats;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.DisplayFormatters;
import org.gudy.azureus2.ui.common.ExternalUIConst;
import org.gudy.azureus2.ui.common.UIConst;
import org.gudy.azureus2.ui.common.util.SLevel;
import org.gudy.azureus2.ui.web2.UI;
import org.gudy.azureus2.ui.web2.util.TemplateCache;
import org.gudy.azureus2.ui.web2.http.request.httpRequest;
import org.gudy.azureus2.ui.web2.http.response.httpNotFoundResponse;
import org.gudy.azureus2.ui.web2.http.response.httpOKResponse;
import org.gudy.azureus2.ui.web2.http.response.httpResponse;
import org.gudy.azureus2.ui.web2.stages.hdapi.httpRequestHandlerIF;

/**
 * @author Tobias Minich
 * 
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class TemplateHandler implements httpRequestHandlerIF {

  /*
   * (non-Javadoc)
   * 
   * @see seda.apps.Haboob.hdapi.httpRequestHandlerIF#handleRequest(seda.sandStorm.lib.http.httpRequest)
   */
  public httpResponse handleRequest(httpRequest arg0) {
    try {
      Template tmpl = TemplateCache.getInstance().get(arg0.getURL());
      tmpl.setParam("Global_ServerName", COConfigurationManager.getStringParameter("Server_sName"));
      if (COConfigurationManager.getIntParameter("Server_iRefresh") != 0)
        tmpl.setParam("Global_Refresh", COConfigurationManager.getIntParameter("Server_iRefresh"));
      //            if ((in.useragent.toUpperCase().indexOf("LYNX")!=-1) ||
	  // (in.useragent.toUpperCase().indexOf("LINKS")!=-1) ||
	  // COConfigurationManager.getBooleanParameter("Server_bNoJavaScript"))
      //              tmpl.setParam("Global_NoJavaScript", Boolean.TRUE);
      TemplateCache tc = TemplateCache.getInstance();
      if (tc.needs(arg0.getURL(), "Options"))
        this.handleConfig(tmpl);
      if (tc.needs(arg0.getURL(), "Torrents"))
        this.handleTorrents(tmpl);
      if (tc.needs(arg0.getURL(), "TorrentInfo"))
        this.handleTorrentInfo(tmpl);
      if (tc.needs(arg0.getURL(), "Log"))
        this.handleLog(tmpl);
      httpOKResponse resp = new httpOKResponse("text/html", new BufferElement(tmpl.output().getBytes()));
      return resp;
    } catch (Exception e) {
      return new httpNotFoundResponse(arg0, e.getMessage());
    }
  }

  private void handleConfigFloat(Template tmpl, String name) {
    String po = MessageText.getString("ConfigView.label." + UI.messagetextmap.get(name.substring(name.indexOf('_') + 2).toLowerCase()));
    if (!po.startsWith("!"))
      tmpl.setParam("Options_" + name + "_D", po);
    tmpl.setParam("Options_" + name, String.valueOf(COConfigurationManager.getFloatParameter(parameterlegacy.get(name).toString())));
  }

  private void handleConfigInt(Template tmpl, String name) {
    String po = MessageText.getString("ConfigView.label." + UI.messagetextmap.get(name.substring(name.indexOf('_') + 2).toLowerCase()));
    if (!po.startsWith("!"))
      tmpl.setParam("Options_" + name + "_D", po);
    tmpl.setParam("Options_" + name, COConfigurationManager.getIntParameter(ExternalUIConst.parameterlegacy.get(name).toString()));
  }

  private void handleConfigBool(Template tmpl, String name) {
    String po = MessageText.getString("ConfigView.label." + UI.messagetextmap.get(name.substring(name.indexOf('_') + 2).toLowerCase()));
    if (!po.startsWith("!"))
      tmpl.setParam("Options_" + name + "_D", po);
    if (COConfigurationManager.getBooleanParameter(ExternalUIConst.parameterlegacy.get(name).toString()))
      tmpl.setParam("Options_" + name, 1);
  }

  private void handleConfigStr(Template tmpl, String name) {
    String po = MessageText.getString("ConfigView.label." + UI.messagetextmap.get(name.substring(name.indexOf('_') + 2).toLowerCase()));
    if (!po.startsWith("!"))
      tmpl.setParam("Options_" + name + "_D", po);
    tmpl.setParam("Options_" + name, COConfigurationManager.getStringParameter(ExternalUIConst.parameterlegacy.get(name).toString()));
  }

  private void handleConfig(Template tmpl) {
    handleConfigStr(tmpl, "General_sDefaultSave_Directory");
    handleConfigStr(tmpl, "General_sDefaultTorrent_Directory");
    handleConfigStr(tmpl, "Core_sOverrideIP");
    handleConfigBool(tmpl, "Core_bAllocateNew");
    handleConfigInt(tmpl, "Core_iLowPort");
    handleConfigInt(tmpl, "Core_iHighPort");
    handleConfigInt(tmpl, "Core_iMaxActiveTorrents");
    handleConfigInt(tmpl, "Core_iMaxDownloads");
    handleConfigInt(tmpl, "Core_iMaxClients");
    handleConfigInt(tmpl, "Core_iMaxUploads");
    handleConfigInt(tmpl, "Core_iMaxUploadSpeed");
    handleConfigBool(tmpl, "Core_bUseResume");
    handleConfigInt(tmpl, "Core_iSaveResumeInterval");
    handleConfigBool(tmpl, "Core_bIncrementalAllocate");
    handleConfigBool(tmpl, "Core_bCheckPiecesOnCompletion");
    handleConfigFloat(tmpl, "Core_fSeedingShareStop");
    handleConfigInt(tmpl, "Core_iSeedingRatioStop");
    handleConfigBool(tmpl, "Core_bDisconnectSeed");
    handleConfigBool(tmpl, "Core_bSwitchPriority");
    handleConfigStr(tmpl, "Core_sPriorityExtensions");
    handleConfigStr(tmpl, "Server_sName");
    handleConfigStr(tmpl, "Server_sBindIP");
    handleConfigInt(tmpl, "Server_iPort");
    handleConfigInt(tmpl, "Server_iTimeout");
    handleConfigStr(tmpl, "Server_sTemplate_Directory");
    handleConfigInt(tmpl, "Server_iMaxHTTPConnections");
    handleConfigInt(tmpl, "Server_iRefresh");
    handleConfigBool(tmpl, "Server_bNoJavaScript");
    handleConfigStr(tmpl, "Server_sAllowStatic");
    handleConfigStr(tmpl, "Server_sAllowDynamic");
    handleConfigInt(tmpl, "Server_iRecheckDynamic");
    handleConfigStr(tmpl, "Server_sAccessHost");
    handleConfigBool(tmpl, "Server_bProxyEnableCookies");
    handleConfigBool(tmpl, "Server_bProxyBlockURLs");
    handleConfigBool(tmpl, "Server_bProxyFilterHTTP");
    handleConfigStr(tmpl, "Server_sProxyUserAgent");
    handleConfigBool(tmpl, "Server_bProxyGrabTorrents");
    handleConfigBool(tmpl, "Server_bUseDownstreamProxy");
    handleConfigStr(tmpl, "Server_sDownstreamProxyHost");
    handleConfigInt(tmpl, "Server_iDownstreamProxyPort");

    handleConfigInt(tmpl, "Server_iLogCount");
    //    if (config.getIntParameter("Server_iVerbosity") !=
	// this.server.verbosity())
    //      tmpl.setParam("Override_Verbosity", "Verbosity overridden via command
	// line to "+this.server.verbosity());
    handleConfigInt(tmpl, "Server_iLogLevelWebinterface");
    handleConfigInt(tmpl, "Server_iLogLevelCore");
    handleConfigBool(tmpl, "Server_bLogFile");
    handleConfigStr(tmpl, "Server_sLogFile");
  }

  private void handleTorrents(Template tmpl) {
    List torrents = UIConst.GM.getDownloadManagers();
    DownloadManager dm;
    int dmstate;
    Hashtable h;
    if (!torrents.isEmpty()) {
      Vector v = new Vector();
      Iterator torrent = torrents.iterator();
      long totalReceived = 0;
      long totalSent = 0;
      long totalDiscarded = 0;
      int connectedSeeds = 0;
      int connectedPeers = 0;
      PEPeerManagerStats ps;
      while (torrent.hasNext()) {
        dm = (DownloadManager) torrent.next();
        TRTrackerScraperResponse hd = dm.getTrackerScrapeResponse();
        dmstate = dm.getState();
        try {
          ps = dm.getPeerManager().getStats();
        } catch (Exception e) {
          ps = null;
        }
        if (ps != null) {
          totalReceived += dm.getStats().getDownloaded();
          totalSent += dm.getStats().getUploaded();
          totalDiscarded += ps.getTotalDiscarded();
          connectedSeeds += dm.getNbSeeds();
          connectedPeers += dm.getNbPeers();
        }
        h = new Hashtable();
        if (dmstate == DownloadManager.STATE_STOPPED) {
          h.put("Torrents_Torrent_Command", "Resume");
          h.put("Torrents_Torrent_Stopped", Boolean.TRUE);
        } else
          h.put("Torrents_Torrent_Command", "Stop");
        if ((hd == null) || (hd.getSeeds() == 0))
          h.put("Torrents_Torrent_Seedless", Boolean.TRUE);
        if (dmstate == DownloadManager.STATE_INITIALIZING)
          h.put("Torrents_Torrent_Initializing", Boolean.TRUE);
        else if (dmstate == DownloadManager.STATE_ALLOCATING)
          h.put("Torrents_Torrent_Allocating", Boolean.TRUE);
        else if (dmstate == DownloadManager.STATE_CHECKING)
          h.put("Torrents_Torrent_Checking", Boolean.TRUE);

        DownloadManagerStats stats = dm.getStats();

        try {
          h.put("Torrents_Torrent_PercentDone", Integer.toString(stats.getCompleted() / 10));
          h.put("Torrents_Torrent_PercentLeft", Integer.toString((1000 - stats.getCompleted()) / 10));
          h.put("Torrents_Torrent_PercentDonePrec", Float.toString(((float) stats.getCompleted()) / 10));
          h.put("Torrents_Torrent_PercentLeftPrec", Float.toString((1000 - (float) stats.getCompleted()) / 10));
        } catch (ArithmeticException e) {
        }
        h.put("Torrents_Torrent_SpeedDown", DisplayFormatters.formatByteCountToKiBEtcPerSec(stats.getDownloadAverage()));
        h.put("Torrents_Torrent_SpeedUp", DisplayFormatters.formatByteCountToKiBEtcPerSec(stats.getUploadAverage()));
        h.put("Torrents_Torrent_FileSize", DisplayFormatters.formatByteCountToKiBEtc(dm.getSize()));
        try {
          h.put("Torrents_Torrent_FileSizeDone", DisplayFormatters.formatByteCountToKiBEtc((((long) stats.getCompleted()) * ((long) dm.getSize())) / 1000));
        } catch (ArithmeticException e) {
        }
        if (dm.getName() == null)
          h.put("Torrents_Torrent_FileName", "?");
        else
          h.put("Torrents_Torrent_FileName", dm.getName());
        if (dmstate == DownloadManager.STATE_ERROR)
          h.put("Torrents_Torrent_Error", dm.getErrorDetails());
        h.put("Torrents_Torrent_Status", UI.status.get(new Integer(dmstate)));
        h.put("Torrents_Torrent_StatusInt", Integer.toString(dmstate));
        if (hd == null || !hd.isValid()) {
          h.put("Torrents_Torrent_Seeds", "?");
          h.put("Torrents_Torrent_Peers", "?");
        } else {
          h.put("Torrents_Torrent_Seeds", Integer.toString(hd.getSeeds()));
          h.put("Torrents_Torrent_Peers", Integer.toString(hd.getPeers()));
        }
        h.put("Torrents_Torrent_SeedsConnected", Integer.toString(dm.getNbSeeds()));
        h.put("Torrents_Torrent_PeersConnected", Integer.toString(dm.getNbPeers()));
        h.put("Torrents_Torrent_ETA", (DisplayFormatters.formatETA(stats.getETA()) == "") ? "&nbsp;" : DisplayFormatters.formatETA(stats.getETA()));
        h.put("Torrents_Torrent_SizeDown", DisplayFormatters.formatDownloaded(stats));
        h.put("Torrents_Torrent_SizeUp", DisplayFormatters.formatByteCountToKiBEtc(stats.getUploaded()));
        h.put("Torrents_Torrent_Hash", ByteFormatter.nicePrintTorrentHash(dm.getTorrent(), true));
        //				if ((in.useragent.toUpperCase().indexOf("LYNX") != -1)
        //					|| (in.useragent.toUpperCase().indexOf("LINKS") != -1)
        //					||
		// COConfigurationManager.getBooleanParameter("Server_bNoJavaScript"))
        //					h.put("Global_NoJavaScript", Boolean.TRUE);
        v.addElement(h);
      }
      tmpl.setParam("Torrents_Torrents", v);
      tmpl.setParam("Torrents_TotalSpeedDown", DisplayFormatters.formatByteCountToKiBEtcPerSec(UIConst.GM.getStats().getDownloadAverage()));
      tmpl.setParam("Torrents_TotalSpeedUp", DisplayFormatters.formatByteCountToKiBEtcPerSec(UIConst.GM.getStats().getUploadAverage()));
      tmpl.setParam("Torrents_TotalSizeDown", DisplayFormatters.formatByteCountToKiBEtc(totalReceived));
      tmpl.setParam("Torrents_TotalSizeUp", DisplayFormatters.formatByteCountToKiBEtc(totalSent));
      tmpl.setParam("Torrents_TotalSizeDiscarded", DisplayFormatters.formatByteCountToKiBEtc(totalDiscarded));
      tmpl.setParam("Torrents_TotalSeedsConnected", Integer.toString(connectedSeeds));
      tmpl.setParam("Torrents_TotalPeersConnected", Integer.toString(connectedPeers));
    }
  }

  private void handleTorrentInfo(Template tmpl) {
    //		if ((!in.vars.isEmpty()) && in.vars.containsKey("hash")) {
    //			tmpl.setParam("TorrentInfo_Hash", in.vars.get("hash").toString());
    //		}
  }

  private void handleLog(Template tmpl) {
    SimpleDateFormat temp = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
    tmpl.setParam("Log_Logtime", temp.format(new Date()));
    tmpl.setParam("Log_Starttime", temp.format(UIConst.startTime));
    tmpl.setParam("Log_Count", UI.logList.size());
    if (UI.logList.size() > 0) {
      ListIterator it = UI.logList.listIterator(UI.logList.size() - 1);
      Vector v = new Vector();
      Hashtable h;
      while (it.hasPrevious()) {
        LoggingEvent rec = (LoggingEvent) it.previous();
        h = new Hashtable();
        h.put("Log_Logs_TimeCode", temp.format(new Date(rec.timeStamp)));
        h.put("Log_Logs_Message", rec.getLevel().toString() + " -- " + rec.getMessage());
        if (rec.getLevel().equals(SLevel.FATAL))
          h.put("Log_Logs_LevelError", Boolean.TRUE);
        v.addElement(h);
      }
      tmpl.setParam("Log_Logs", v);
    }
  }
}
