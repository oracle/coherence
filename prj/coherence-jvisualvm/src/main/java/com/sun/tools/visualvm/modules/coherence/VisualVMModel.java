/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence;

import com.sun.tools.visualvm.modules.coherence.helper.HttpRequestSender;
import com.sun.tools.visualvm.modules.coherence.helper.RequestSender;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.CacheData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.CacheDetailData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.CacheFrontDetailData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.CacheStorageManagerData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ClusterData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Data;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.DataRetriever;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.FederationDestinationData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.FederationDestinationDetailsData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.FederationOriginData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.FederationOriginDetailsData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.FlashJournalData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.HttpProxyData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.HttpProxyMemberData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.HttpSessionData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.JCacheConfigurationData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.JCacheStatisticsData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.MachineData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.MemberData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.NodeStorageData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Pair;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.PersistenceData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.PersistenceNotificationsData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ProxyData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.RamJournalData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ServiceData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.ServiceMemberData;
import com.sun.tools.visualvm.modules.coherence.tablemodel.model.Tuple;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.Map.Entry;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServerConnection;

/**
 * A class that is used to store and update Coherence cluster
 * JMX statistics. This is used to avoid placing too much stress on
 * the Management service of a cluster.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public class VisualVMModel
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Create a new VisualVModel for monitoring Coherence clusters in JMX.
     */
    public VisualVMModel()
        {
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Initialize anything for this instance of the model.
     */
    private void init()
        {
        nRefreshTime = DEFAULT_REFRESH_TIME;

        String sRefreshTime      = getSystemProperty("coherence.jvisualvm.refreshtime");
        String sReporterDisabled = getSystemProperty("coherence.jvisualvm.reporter.disabled");
        fLogJMXQueryTimes        = getSystemProperty("coherence.jvisualvm.log.query.times") != null;

        if (sRefreshTime != null)
            {
            nRefreshTime = Long.parseLong(sRefreshTime) * 1000L;
            }

        // if this option is set we are specifically disabling the reporter even if Coherence
        // version >= 12.1.3
        if (sReporterDisabled != null && "true".equalsIgnoreCase(sReporterDisabled))
            {
            setReporterAvailable(false);
            }

        // force update on first time
        ldtLastUpdate = System.currentTimeMillis() - nRefreshTime - 1L;

        // populate mapCollectedData which contains an etry for each type
        mapCollectedData = new HashMap<DataType, List<Entry<Object, Data>>>();

        for (DataType type : DataType.values())
            {
            mapCollectedData.put(type, null);
            }

        // intialize the data retrievers map
        mapDataRetrievers.put(CacheData.class, new CacheData());
        mapDataRetrievers.put(ClusterData.class, new ClusterData());
        mapDataRetrievers.put(MemberData.class, new MemberData());
        mapDataRetrievers.put(ServiceData.class, new ServiceData());
        mapDataRetrievers.put(ServiceMemberData.class, new ServiceMemberData());
        mapDataRetrievers.put(ProxyData.class, new ProxyData());
        mapDataRetrievers.put(MachineData.class, new MachineData());
        mapDataRetrievers.put(CacheDetailData.class, new CacheDetailData());
        mapDataRetrievers.put(CacheFrontDetailData.class, new CacheFrontDetailData());
        mapDataRetrievers.put(PersistenceData.class, new PersistenceData());
        mapDataRetrievers.put(PersistenceNotificationsData.class, new PersistenceNotificationsData());
        mapDataRetrievers.put(CacheStorageManagerData.class, new CacheStorageManagerData());
        mapDataRetrievers.put(HttpSessionData.class, new HttpSessionData());
        mapDataRetrievers.put(FederationDestinationData.class, new FederationDestinationData());
        mapDataRetrievers.put(FederationDestinationDetailsData.class, new FederationDestinationDetailsData());
        mapDataRetrievers.put(FederationOriginData.class, new FederationOriginData());
        mapDataRetrievers.put(FederationOriginDetailsData.class, new FederationOriginDetailsData());
        mapDataRetrievers.put(RamJournalData.class, new RamJournalData());
        mapDataRetrievers.put(FlashJournalData.class, new FlashJournalData());
        mapDataRetrievers.put(JCacheConfigurationData.class, new JCacheConfigurationData());
        mapDataRetrievers.put(JCacheStatisticsData.class, new JCacheStatisticsData());
        mapDataRetrievers.put(HttpProxyData.class, new HttpProxyData());
        mapDataRetrievers.put(HttpProxyMemberData.class, new HttpProxyMemberData());
        mapDataRetrievers.put(NodeStorageData.class, new NodeStorageData());

        // Loop through each data retriever and initialize the map of
        // report XML. Doing it this way we load it only once
        mapReportXML = new HashMap<>();

        Iterator<Map.Entry<Class, DataRetriever>> iter = mapDataRetrievers.entrySet().iterator();
        while (iter.hasNext())
            {
            Map.Entry<Class, DataRetriever> entry = iter.next();
            String sReport = entry.getValue().getReporterReport();
            if (sReport != null)
                {
                String sReportXML = getReportXML(sReport);
                if (sReportXML != null)
                    {
                    mapReportXML.put(entry.getKey(), sReportXML);
                    }
                }
            }
        }

    /**
     * Return a system property value and ensure if the value is not found, we try
     * using the old property value with "com.oracle." prefix. <br>
     * This is for backwards compatibility only and support for properties with com.oracle
     * prefix will be deprecated.
     *
     * @param   sKey  the property key
     * @return  the property value
     */
    public static String getSystemProperty(String sKey)
        {
        // attempt to get the property with the "proper" prefix.
        String sValue = System.getProperty(sKey);

        if (sValue == null)
            {
            // attempt to get value by prefix  "com.oracle."
            return System.getProperty("com.oracle." + sKey);
            }

        return sValue;
        }

    /**
     * Refresh the statistics from the given {@link MBeanServerConnection}
     * connection. This method will only refresh data if > REFRESH_TIME
     * has passed since last refresh.
     *
     * @param requestSender  the RequestSender to use
     */
    public void refreshStatistics(RequestSender requestSender)
        {
        if (System.currentTimeMillis() - ldtLastUpdate >= nRefreshTime)
            {
            long ldtStart = System.currentTimeMillis();

            // its important that the CACHE data is refreshed first and
            // as such we are relying on the order of types in the enum.
            for (DataType type : DataType.values())
                {
                long ldtCollectionStart = System.currentTimeMillis();
                mapCollectedData.put(type, getData(requestSender, type.getClassName()));
                long ldtCollectionTime  = System.currentTimeMillis() - ldtCollectionStart;

                if (fLogJMXQueryTimes)
                    {
                    LOGGER.info("Time to query statistics for " + type.toString() + " was " +
                                ldtCollectionTime + " ms");
                    }
                }

            long ldtTotalDuration = System.currentTimeMillis() - ldtStart;

            if (fLogJMXQueryTimes)
               {
               LOGGER.info("Time to query all statistics was " + ldtTotalDuration + " ms");
               }

            ldtLastUpdate = System.currentTimeMillis();
            }
        }

    /**
     * This is a wrapper method which will call the underlying implementation
     * to get statistics. If statistics directly from the reporter are available
     * then run the particular report, otherwise do a JMX query.
     *
     * @param requestSender  the {@link RequestSender} to use to query the report
     * @param clazz          the implementation of {@link DataRetriever} to get data for
     *
     * @return the {@link List} of data obtainer by either method
     */
    public List<Entry<Object, Data>> getData(RequestSender requestSender, Class clazz)
        {
        boolean fFallBack = false;

        // Re Bug 22132359 - When we are connecting to pre 12.2.1.1.0 cluster from 12.2.1.1.0 or above and we
        // are collecting ProxyStats, we need to force to use JMX rather than report
        if (isReporterAvailable() != null && isReporterAvailable() &&
            !(clazz.equals(ProxyData.class) && getClusterVersionAsInt() < 122110))
            {
            // retrieve the report XML for this class
            String sReportXML = mapReportXML.get(clazz);

            if (sReportXML == null)
                {
                // this means there is no report for this class
                fFallBack = true;
                }
            if (!fFallBack)
                {
                try
                    {
                    DataRetriever           retriever        = getDataRetrieverInstance(clazz);
                    SortedMap<Object, Data> mapCollectedData = null;
                    if (requestSender instanceof HttpRequestSender)
                        {
                        mapCollectedData = retriever.
                                getAggregatedDataFromHttpQuerying(this, ((HttpRequestSender) requestSender));
                        }
                    else
                        {
                        mapCollectedData = retriever.getAggregatedDataUsingReport(this, requestSender, sReportXML);
                        }

                    if (mapCollectedData != null)
                        {
                        return new ArrayList<Map.Entry<Object, Data>>(mapCollectedData.entrySet());
                        }
                    else
                        {
                        return null;
                        }
                    }
                catch (Exception e)
                    {
                    // we received an error running the report, so mark as
                    // a fall back so it will be immediately run
                    LOGGER.warning(Localization.getLocalText("ERR_Failed_to_run_report", new String[] { clazz.toString(), e.toString() }));
                    e.printStackTrace();
                    fFallBack = true;
                    }
                }
            }

        // this code path is for the following scenarios:
        // 1. If we need to fall-back as the reporter is being used but no report yet available
        // 2. The reporter is not available
        // 3. We have not yet decided is the reporter is available
        // 4. Bug 22132359 - We are connecting to pre 12.2.1.1.0 cluster from 12.2.1.1.0 or above and we
        //                   are collecting ProxyStats,  we need to force to use JMX rather than report
        if (fFallBack || isReporterAvailable() == null || !isReporterAvailable() ||
            (clazz.equals(ProxyData.class) && getClusterVersionAsInt() < 122110))
            {
            try
                {
                // get data the old fashioned way via JMX queries
                // ClusterData is a a special case as its used to determine if
                // we may be able to use the reporter
                if (clazz.equals(ClusterData.class))
                    {
                    List<Entry<Object, Data>> clusterData = getDataRetrieverInstance(clazz).getJMXData(requestSender, this);

                    // if we have not yet evaluated if the reporter is available, e.g. value of null,
                    // then do it. Also check for the version as well.
                    if (isReporterAvailable() == null || is1213AndAbove() == null)
                        {
                        // get the Coherence version. Easier to do if we are connected to a cluster,
                        // but we are have JMX connection as we have to look in data we collected.

                        if (clusterData != null)
                            {
                            for (Entry<Object, Data> entry : clusterData)
                                {
                                // there will only be one cluster entry

                                String sCoherenceVersion =
                                    entry.getValue().getColumn(ClusterData.VERSION).toString().replaceFirst(" .*$", "")
                                                    .replaceFirst("[\\.-]SNAPSHOT.*$","").replaceAll("-",".");
                                m_sClusterVersion = sCoherenceVersion;

                                int nVersion = 0;

                                if (sCoherenceVersion.startsWith("3.5"))
                                    {
                                    // manual check as version numbering changed after 35
                                    nVersion = 353;
                                    }
                                else if (sCoherenceVersion.startsWith("2"))
                                    {
                                    // check for versions such as 20.06 or 20.06.01 and convert them to an ever increasing number
                                    // 20.06    -> 2006000
                                    // 20.06.1  -> 2006100
                                    // 20.06.10 -> 2006100
                                    String sStrippedVersion = sCoherenceVersion.replaceAll("\\.", "");
                                    nVersion = Integer.parseInt(sStrippedVersion) * (int) Math.pow(10, 7 - sStrippedVersion.length());
                                    }
                                else
                                    {
                                    nVersion = Integer.parseInt(sCoherenceVersion.replaceAll("\\.", ""));
                                    }

                                if (nVersion >= 121300)
                                    {
                                    // only set if the reporter available is it is not already set as we may have
                                    // got to this code path because is1213AndAbove() is still null
                                    setReporterAvailable(isReporterAvailable() == null ? true : isReporterAvailable());
                                    fis1213AndAbove = true;
                                    }
                                else
                                    {
                                    setReporterAvailable(isReporterAvailable() == null ? false : isReporterAvailable());
                                    fis1213AndAbove = false;
                                    }
                                m_nClusterVersion = nVersion;
                                }
                            }
                        }

                    return clusterData;
                    }
                else
                    {
                    return getDataRetrieverInstance(clazz).getJMXData(requestSender, this);
                    }
                }
            catch (Exception e)
                {
                LOGGER.log(Level.WARNING, "Unable to get data", e);
                }
            }

        return null;
        }

    /**
     * Retrieve the XML for the report by loading it from the resource.
     *
     * @param sReport  the report to load from - this will be available as part
     *                 of the JVisualVM plugin
     *
     * @return  a String containing the report XML
     */
    private String getReportXML(String sReport) {
        StringBuffer sb = new StringBuffer();
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(sReport);

        if (in == null)
            {
            throw new RuntimeException("Unable to load report " + sReport);
            }

        InputStreamReader is = new InputStreamReader(in);
        BufferedReader br = new BufferedReader(is);
        try
            {
            String sLine;

            while ((sLine  = br.readLine()) != null)
                {
                sb.append(sLine);

                }
            }
        catch (IOException ioe)
            {
            throw  new RuntimeException("Unable to read from report " + sReport + " : " +
                                        ioe.getMessage());
            }
        finally
            {
            closeAndIgnore(br);
            closeAndIgnore(is);
            closeAndIgnore(is);
            }

        return sb.toString();
    }

    /**
     * Close a {@link Closeable} or {@link Reader} object.
     *
     * @param obj  the {@link Closeable} or {@link Reader} object to close
     */
    private void closeAndIgnore(Object obj)
        {
        try
            {
            if (obj instanceof Closeable)
                {
                ((Closeable)obj).close();
                }
            else if (obj instanceof Reader)
                {
                ((Reader)obj).close();
                }
            }
        catch (Exception e)
            {
            // ignore
            }
        }

    /**
     * Returns a unique list of addresses for the member data as we only want to
     * get information for each machine. We also store the machine and nodeId as
     * a key for querying individual nodes but will strip this later.
     *
     * @return the {@link SortedMap} of machines
     */
    public SortedMap<Pair<String, Integer>, Data> getInitialMachineMap()
        {
        SortedMap<Pair<String, Integer>, Data> initialMachineMap = new TreeMap<Pair<String, Integer>, Data>();

        // get a unique list of addresses for the member data as we only want to
        // get information for each machine. We also store the machine and nodeId as
        // a key but will strip this later

        if (mapCollectedData.get(DataType.MEMBER) != null)
            {
            for (Entry<Object, Data> entry : mapCollectedData.get(DataType.MEMBER))
                {
                Pair<String, Integer> key = new Pair<String,
                                                Integer>((String) entry.getValue().getColumn(MemberData.ADDRESS),
                                                    (Integer) entry.getValue().getColumn(MemberData.NODE_ID));

                if (initialMachineMap.get(key) == null)
                    {
                    initialMachineMap.put(key, (Data) null);
                    }
                }
            }

        return initialMachineMap;
        }

    /**
     * Erase the current service member data as we have changed the
     * selected service.
     */
    public void eraseServiceMemberData()
        {
        mapCollectedData.put(DataType.SERVICE_DETAIL, null);
        }

    /**
     * Erase the current destination details data and origin details data
     * as we have changed the service / participant pair in federation tab.
     */
    public void eraseFederationDetailsData()
        {
        mapCollectedData.put(DataType.FEDERATION_DESTINATION_DETAILS, null);
        mapCollectedData.put(DataType.FEDERATION_ORIGIN_DETAILS, null);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the currently selected service.
     *
     * @return the currently selected service
     */
    public String getSelectedService()
        {
        return sSelectedService;
        }

    /**
     * Sets the currently selected service.
     *
     * @param sService the currently selected service
     */
    public void setSelectedService(String sService)
        {
        sSelectedService = sService;
        mapCollectedData.remove(DataType.SERVICE_DETAIL);
        }

    /**
     * Returns the currently selected service in http proxy tab.
     *
     * @return  the currently selected service in http proxy tab
     */
    public String getSelectedHttpProxyService()
        {
        return m_sSelectedHttpProxyService;
        }

    /**
     * Sets the current selected service in http proxy tab.
     *
     * @param sService the currently selected service
     */
    public void setSelectedHttpProxyService(String sService)
        {
        m_sSelectedHttpProxyService = sService;
        mapCollectedData.remove(DataType.HTTP_PROXY_DETAIL);
        }

    /**
     * Returns the currently selected service in federation tab.
     *
     * @return the currently selected service
     */
    public String getSelectedServiceInFed()
        {
        return m_sSelectedServiceInFed;
        }

    /**
     * Sets the currently selected service in federation tab.
     *
     * @param sService the currently selected service
     */
    public void setSelectedServiceInFed(String sService)
        {
        m_sSelectedServiceInFed = sService;
        }

    /**
     * Returns the selected node Id in outbound details in federation tab.
     *
     * @return the currently selected node Id in outbound.
     */
    public String getSelectedNodeOutbound()
        {
        return m_sOutboundNodeId;
        }

    /**
     * Sets the selected node Id in outbound details in federation tab.
     *
     * @param sNodeId  the node Id
     */
    public void setSelectedNodeOutbound(String sNodeId)
        {
        m_sOutboundNodeId = sNodeId;
        }

    /**
     * Returns the selected node Id in inbound details in federation tab.
     *
     * @return the currently selected node Id in inbound.
     */
    public String getSelectedNodeInbound()
        {
        return m_sInboundNodeId;
        }

    /**
     * Sets the selected node Id in inbound details in federation tab.
     *
     * @param sNodeId  the node Id
     */
    public void setSelectedNodeInbound(String sNodeId)
        {
        m_sInboundNodeId = sNodeId;
        }

    /**
     * Returns if the reporter is available for use.
     *
     * @return null if not yet evaluated or true/false if evaluated
     */
    public Boolean isReporterAvailable()
        {
        return fReporterAvailable;
        }

    /**
     * Return if we are running Coherence 12.1.3 or above.
     *
     * @return if we are running Coherence 12.1.3 or above
     */
    public Boolean is1213AndAbove()
        {
        return fis1213AndAbove;
        }

    /**
     * Sets if the reporter is available.
     *
     * @param value  if the reporter is available
     */
    public void setReporterAvailable(Boolean value)
        {
        fReporterAvailable = value;
        }

    /**
     * Sets if we want to include the NameService in the list of
     * proxy servers.
     *
     * @param fInclude if we want to include the NameService
     *                 in the list of proxy servers
     */
    public void setIncludeNameService(boolean fInclude)
        {
        fIncludeNameService = fInclude;
        }

    /**
     * Returns if we want to include the NameService in the list of
     * proxy servers.
     *
     * @return if we want to include the NameService
     */
    public boolean isIncludeNameService()
        {
        return fIncludeNameService;
        }

    /**
     * Sets the currently selected cache.
     *
     * @param selectedCache  the currently selected cache (service/cache name {@link Tuple}
     */
    public void setSelectedCache(Pair<String, String> selectedCache)
        {
        this.selectedCache = selectedCache;
        mapCollectedData.remove(DataType.CACHE_DETAIL);
        mapCollectedData.remove(DataType.CACHE_FRONT_DETAIL);
        mapCollectedData.remove(DataType.CACHE_STORAGE_MANAGER);
        }

    /**
     * Returns the currently selected cache.
     *
     * @return the currently selected cache
     */
    public Pair<String, String> getSelectedCache()
        {
        return this.selectedCache;
        }

    /**
     * Sets the current selected JCache.
     */
    public void setSelectedJCache(Pair<String, String> selectedJCache)
        {
        this.selectedJCache = selectedJCache;
        }

    /**
     * Returns the currently selected JCache cache.
     *
     * @return the currently selected JCache cache
     */
    public Pair<String, String> getSelectedJCache()
        {
        return this.selectedJCache;
        }

    /**
     * Sets the currently selected service name and participant name in federation tab.
     *
     * @param selectedServiceParticipant  the currently selected service name and participant name {@link Tuple}
     */
    public void setSelectedServiceParticipant(Pair<String, String> selectedServiceParticipant)
        {
        this.selectedServiceParticipant = selectedServiceParticipant;
        }

    /**
     * Returns the currently selected service name and participant name in federation tab.
     *
     * @return the currently selected service name and participant name
     */
    public Pair<String, String> getSelectedServiceParticipant()
        {
        return this.selectedServiceParticipant;
        }

    /**
     * Sets the flag to indicate whether the federation service is available.
     *
     * @param isAvailable  true if the federation is available
     */
    public void setFederationAvailable(boolean isAvailable)
        {
        fIsFederationAvailable = isAvailable;
        }

    /**
     * Returns if proxy servers are configured.
     *
     * @return true if proxy servers are configured.
     */
    public boolean isCoherenceExtendConfigured()
        {
        // if we have never set this flag, do it once only so that
        // the tab will always display and be updated
        if (fIsCoherenceExtendConfigured == null)
            {
            fIsCoherenceExtendConfigured = mapCollectedData.get(DataType.PROXY) != null
                                           && mapCollectedData.get(DataType.PROXY).size() != 0;
            }

        return fIsCoherenceExtendConfigured;
        }

    /**
     * Returns if Coherence*Web is configured.
     *
     * @return true if Coherence*Web is configured.
     */
    public boolean isCoherenceWebConfigured()
        {
        return mapCollectedData.get(DataType.HTTP_SESSION) != null
               && mapCollectedData.get(DataType.HTTP_SESSION).size() != 0;
        }

    /**
     * Returns if Persistence is configured.
     *
     * @return true if Persistence is configured.
     */
    public boolean isPersistenceConfigured()
        {
        return mapCollectedData.get(DataType.PERSISTENCE) != null
                && mapCollectedData.get(DataType.PERSISTENCE).size() != 0;
        }

    /**
     * Return if Federation is configured.
     *
     * @return true if Federation is configured.
     */
    public boolean isFederationCongfigured()
        {
        return fIsFederationAvailable;
        }

    /**
     * Returns if Elastic Data is configured.
     *
     * @return true if Elastic Data is configured.
     */
    public boolean isElasticDataConfigured()
        {
        return (mapCollectedData.get(DataType.RAMJOURNAL) != null
                  && mapCollectedData.get(DataType.RAMJOURNAL).size() != 0) ||
               (mapCollectedData.get(DataType.FLASHJOURNAL) != null
                  && mapCollectedData.get(DataType.FLASHJOURNAL).size() != 0);
        }

    /**
     * Returns if JCache is configured.
     *
     * @return true if JCache is configured.
     */
    public boolean isJCacheConfigured()
        {
        return (mapCollectedData.get(DataType.JCACHE_CONFIG) != null
                && mapCollectedData.get(DataType.JCACHE_CONFIG).size() != 0) ||
               (mapCollectedData.get(DataType.JCACHE_STATS) != null
                && mapCollectedData.get(DataType.JCACHE_STATS).size() != 0);
        }

    /**
     * Return if http proxy servers are configured.
     *
     * @return true if http proxy servers are configured
     */
    public boolean isHttpProxyConfigured()
        {
        return (mapCollectedData.get(DataType.HTTP_PROXY) != null
           && mapCollectedData.get(DataType.HTTP_PROXY).size() != 0);
        }

    /**
     * Returns the data for a given {@link DataType} enum.
     *
     * @param dataType the type of data to return
     */
    public List<Entry<Object, Data>> getData(DataType dataType)
        {
        return mapCollectedData.get(dataType);
        }

    /**
     * Returns if load average is available.
     *
     * @return true if load average is available
     */
    public boolean isLoadAverageAvailable()
        {
        return m_fIsLoadAverageAvailable;
        }

    /**
     * Set an indicator to show if load average should be used.
     *
     * @param fLoadAverageAvailable indicates if load average is available
     */
    public void setLoadAverageAvailable(boolean fLoadAverageAvailable)
        {
        m_fIsLoadAverageAvailable = fLoadAverageAvailable;
        }

    /**
     * Returns an instance of the data retriever class for executing JMX calls on.
     *
     * @param clazz the {@link Class} to get the instance for
     *
     * @return an instance of the data retriever class for executing JMX calls on
     */
    public DataRetriever getDataRetrieverInstance(Class clazz)
        {
        DataRetriever retriever = mapDataRetrievers.get(clazz);

        if (retriever == null)
            {
            throw new IllegalArgumentException(Localization.getLocalText("ERR_instance",
                new String[] {clazz.getCanonicalName()}));
            }

        return retriever;
        }

    /**
      * Returns the cluster version as a String.
      */
    public String getClusterVersion()
        {
        return m_sClusterVersion;
        }

    /**
     * Returns the cluster version as an integer for comparison reasons.
     */
    public int getClusterVersionAsInt()
        {
        return m_nClusterVersion;
        }

    /**
     * Set the known distributed caches.
     *
     * @param  setCaches the {@link Set} of distributed caches.
     */
    public void setDistributedCaches(Set<String> setCaches)
        {
        this.setKnownDistributedCaches = setCaches;
        }

    /**
     * Returns the {@link Set} of distributed caches.
     *
     * @return the {@link Set} of distributed caches
     */
    public Set<String> getDistributedCaches()
        {
        return setKnownDistributedCaches;
        }

    /**
     * Returns the {@link Set} of domain partitions that have been discovered.
     *
     * @return the {@link Set} of domain partitions that have been discovered
     */
    public Set<String> getDomainPartitions()
        {
        return setDomainPartitions;
        }

    /**
     * Return the last time the statistics were updated.
     *
     * @return the last time the statistics were updated
     */
    public long getLastUpdate()
        {
        return ldtLastUpdate;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Returns an instance of the VisualVMModel.
     *
     * @return an instance of the initialized VisualVMModel
     */
    public static VisualVMModel getInstance()
        {
        VisualVMModel model = new VisualVMModel();

        model.init();

        return model;
        }

    /**
     * Returns the report XML for a given class.
     *
     * @return the report XML for a given class
     */
    public Map<Class, String> getReportXMLMap()
        {
        return this.mapReportXML;
        }

    /**
     * Defines the type of data we can collect.
     * Note: The order of these is important. Please do not change. e.g. cluster
     * need to go first so we can determine the version. Also service needs
     * to go before cache so we could setup the list of distributed caches.
     */
    public enum DataType
        {
        CLUSTER(ClusterData.class, CLUSTER_LABELS),
        SERVICE(ServiceData.class, SERVICE_LABELS),
        SERVICE_DETAIL(ServiceMemberData.class, SERVICE_DETAIL_LABELS),
        CACHE(CacheData.class, CACHE_LABELS),
        CACHE_DETAIL(CacheDetailData.class, CACHE_DETAIL_LABELS),
        CACHE_FRONT_DETAIL(CacheFrontDetailData.class, CACHE_FRONT_DETAIL_LABELS),
        CACHE_STORAGE_MANAGER(CacheStorageManagerData.class, CACHE_STORAGE_MANAGER_LABELS),
        MEMBER(MemberData.class, MEMBER_LABELS),
        NODE_STORAGE(NodeStorageData.class, new String[] {}),
        MACHINE(MachineData.class, MACHINE_LABELS),
        PROXY(ProxyData.class, PROXY_LABELS),
        PERSISTENCE(PersistenceData.class, PERSISTENCE_LABELS),
        PERSISTENCE_NOTIFICATIONS(PersistenceNotificationsData.class, PERSISTENCE_NOTIFICATIONS_LABELS),
        HTTP_SESSION(HttpSessionData.class, HTTP_SESSION_LABELS),
        FEDERATION_DESTINATION(FederationDestinationData.class, FEDERATION_OVERALL_LABELS),
        FEDERATION_ORIGIN(FederationOriginData.class, null),
        FEDERATION_DESTINATION_DETAILS(FederationDestinationDetailsData.class, FEDERATION_DESTINATION_DETAILS_LABELS),
        FEDERATION_ORIGIN_DETAILS(FederationOriginDetailsData.class, FEDERATION_ORIGIN_DETAILS_LABELS),
        RAMJOURNAL(RamJournalData.class, ELASTIC_DATA_LABELS),
        FLASHJOURNAL(FlashJournalData.class, ELASTIC_DATA_LABELS),
        JCACHE_CONFIG(JCacheConfigurationData.class, JCACHE_CONFIG_LABELS),
        JCACHE_STATS(JCacheStatisticsData.class, JCACHE_STATS_LABELS),
        HTTP_PROXY(HttpProxyData.class, HTTP_PROXY_LABELS),
        HTTP_PROXY_DETAIL(HttpProxyMemberData.class, HTTP_PROXY_DETAIL_LABELS);

        private DataType(Class clz, String[] asMeta)
            {
            clazz      = clz;
            asMetadata = asMeta;
            }

        /**
         * Returns the class for this enum.
         *
         * @return the class for this enum
         */
        public Class getClassName()
            {
            return clazz;
            }

        /**
         * Returns the column metadata for this enum.
         *
         * @return the column metadata for this enum
         */
        public String[] getMetadata()
            {
            return asMetadata;
            }

        /**
         * The {@link Class} associated with this enum.
         */
        private Class clazz;

        /**
         * The column name associated with this enum.
         */
        private String[] asMetadata;
        }

    /**
     * Labels for cluster table. Note: No localization is done for these labels
     * as currently they are not displayed.
     */
    private static final String[] CLUSTER_LABELS = new String[] {"Cluster Name", "License Mode", "Version",
        "Departure Count", "Cluster Size"};

    /**
     * Labels for service table.
     */
    private static final String[] SERVICE_LABELS = new String[]
        {
        Localization.getLocalText("LBL_service_name"), Localization.getLocalText("LBL_status_ha"),
        Localization.getLocalText("LBL_members"), Localization.getLocalText("LBL_storage_enabled"),
        Localization.getLocalText("LBL_partitions"), Localization.getLocalText("LBL_endangered"),
        Localization.getLocalText("LBL_vulnerable"), Localization.getLocalText("LBL_unbalanced"),
        Localization.getLocalText("LBL_pending")
        };

    /**
     * Labels for service detail table.
     */
    private static final String[] SERVICE_DETAIL_LABELS = new String[]
        {
        Localization.getLocalText("LBL_node_id"), Localization.getLocalText("LBL_threads"),
        Localization.getLocalText("LBL_idle_threads"), Localization.getLocalText("LBL_thread_util"),
        Localization.getLocalText("LBL_task_average"), Localization.getLocalText("LBL_task_backlog"),
        Localization.getLocalText("LBL_request_average")
        };

    /**
     * Labels for cache table.
     */
    private static final String[] CACHE_LABELS = new String[] {Localization.getLocalText("LBL_service_cache_name"),
        Localization.getLocalText("LBL_size"), Localization.getLocalText("LBL_memory_bytes"),
        Localization.getLocalText("LBL_memory_mb"), Localization.getLocalText("LBL_average_object_size"),
        Localization.getLocalText("LBL_unit_calculator")};

    /**
     * Labels for cache detail table.
     */
    private static final String[] CACHE_DETAIL_LABELS = new String[]
        {
        Localization.getLocalText("LBL_node_id"), Localization.getLocalText("LBL_size"),
        Localization.getLocalText("LBL_memory_bytes"), Localization.getLocalText("LBL_total_gets"),
        Localization.getLocalText("LBL_total_puts"), Localization.getLocalText("LBL_cache_hits"),
        Localization.getLocalText("LBL_cache_misses"), Localization.getLocalText("LBL_hit_probability")
        };

    /**
     * Labels for front cache detail table.
     */
    private static final String[] CACHE_FRONT_DETAIL_LABELS = new String[]
        {
        Localization.getLocalText("LBL_node_id"), Localization.getLocalText("LBL_size"),
        Localization.getLocalText("LBL_total_gets"), Localization.getLocalText("LBL_total_puts"),
        Localization.getLocalText("LBL_cache_hits"), Localization.getLocalText("LBL_cache_misses"),
        Localization.getLocalText("LBL_hit_probability")
        };

    /**
     * Labels for storage manager table.
     */
    private static final String[] CACHE_STORAGE_MANAGER_LABELS = new String[]
        {
        Localization.getLocalText("LBL_node_id"), Localization.getLocalText("LBL_locks_granted"),
        Localization.getLocalText("LBL_locks_pending"), Localization.getLocalText("LBL_listener_reg"),
        Localization.getLocalText("LBL_max_query_millis"), Localization.getLocalText("LBL_max_query_desc"),
        Localization.getLocalText("LBL_non_opt_avge"), Localization.getLocalText("LBL_opt_avge"),
        Localization.getLocalText("LBL_index_units")
        };

    /**
     * Labels for member table.
     */
    private static final String[] MEMBER_LABELS = new String[]
        {
        Localization.getLocalText("LBL_node_id"), Localization.getLocalText("LBL_unicast_address"),
        Localization.getLocalText("LBL_port"), Localization.getLocalText("LBL_role"),
        Localization.getLocalText("LBL_publisher_rate"), Localization.getLocalText("LBL_receiver_rate"),
        Localization.getLocalText("LBL_send_q"), Localization.getLocalText("LBL_max_memory"),
        Localization.getLocalText("LBL_used_memory"), Localization.getLocalText("LBL_free_memory"),
        Localization.getLocalText("LBL_storage_enabled")
        };

    /**
     * Labels for machine table.
     */
    private static final String[] MACHINE_LABELS = new String[]
        {
        Localization.getLocalText("LBL_machine_name"), Localization.getLocalText("LBL_core_count"),
        Localization.getLocalText("LBL_load_average"), Localization.getLocalText("LBL_total_physical_mem"),
        Localization.getLocalText("LBL_free_physical_mem"), Localization.getLocalText("LBL_percent_free_mem")
        };

    /**
     * Labels for proxy table.
     */
    private static final String[] PROXY_LABELS = new String[]
        {
        Localization.getLocalText("LBL_ip_port"), Localization.getLocalText("LBL_service_name"),
        Localization.getLocalText("LBL_node_id"), Localization.getLocalText("LBL_connection_count"),
        Localization.getLocalText("LBL_outgoing_msg_backlog"), Localization.getLocalText("LBL_total_bytes_rcv"),
        Localization.getLocalText("LBL_total_bytes_sent"), Localization.getLocalText("LBL_total_msg_rcv"),
        Localization.getLocalText("LBL_total_msg_sent")
        };

    /**
     * Labels for persistence table.
     */
    private static final String[] PERSISTENCE_LABELS = new String[]
        {
        Localization.getLocalText("LBL_service_name"), Localization.getLocalText("LBL_persistence_mode"),
        Localization.getLocalText("LBL_active_space_bytes"), Localization.getLocalText("LBL_active_space_mb"),
        Localization.getLocalText("LBL_avge_persistence"), Localization.getLocalText("LBL_max_persistence"),
        Localization.getLocalText("LBL_snapshot_count"), Localization.getLocalText("LBL_status")
        };

    /**
     * Labels for persistence notifications table.
     */
    private static final String[] PERSISTENCE_NOTIFICATIONS_LABELS = new String[]
        {
        Localization.getLocalText("LBL_sequence"), Localization.getLocalText("LBL_service_name"),
        Localization.getLocalText("LBL_operation"), Localization.getLocalText("LBL_start_time"),
        Localization.getLocalText("LBL_end_time"), Localization.getLocalText("LBL_duration"),
        Localization.getLocalText("LBL_message")
        };

    /**
     * Labels for persistence table.
     */
    private static final String[] HTTP_SESSION_LABELS = new String[]
        {
        Localization.getLocalText("LBL_application_id"), Localization.getLocalText("LBL_platform"),
        Localization.getLocalText("LBL_session_timeout"), Localization.getLocalText("LBL_session_cache_name"),
        Localization.getLocalText("LBL_overflow_cache_name"), Localization.getLocalText("LBL_avge_session_size"),
        Localization.getLocalText("LBL_total_reaped_sessions"), Localization.getLocalText("LBL_avge_reaped_sessions"),
        Localization.getLocalText("LBL_avge_reap_duration"), Localization.getLocalText("LBL_last_reap_max"),
        Localization.getLocalText("LBL_session_updates")
        };

    /**
     * Labels for federation table.
     */
    private static final String[] FEDERATION_OVERALL_LABELS = new String[]
        {
        Localization.getLocalText("LBL_service_name"), Localization.getLocalText("LBL_participant"),
        Localization.getLocalText("LBL_status"), Localization.getLocalText("LBL_total_bytes_sent_sec"),
        Localization.getLocalText("LBL_total_msgs_sent_sec"), Localization.getLocalText("LBL_total_bytes_received_sec"),
        Localization.getLocalText("LBL_total_msgs_received_sec")
        };

    /**
     * Labels for federation destination details table.
     */
    private static final String[] FEDERATION_DESTINATION_DETAILS_LABELS = new String[]
        {
        Localization.getLocalText("LBL_node_id"), Localization.getLocalText("LBL_state"),
        Localization.getLocalText("LBL_current_bandwidth"), Localization.getLocalText("LBL_total_bytes_sent"),
        Localization.getLocalText("LBL_total_entries_sent"), Localization.getLocalText("LBL_total_records_sent"),
        Localization.getLocalText("LBL_total_msg_sent"), Localization.getLocalText("LBL_total_msg_unacked")
        };

    /**
     * Labels for federation origin details table.
     */
    private static final String[] FEDERATION_ORIGIN_DETAILS_LABELS = new String[]
        {
        Localization.getLocalText("LBL_node_id"), Localization.getLocalText("LBL_total_bytes_received"),
        Localization.getLocalText("LBL_total_records_received"),Localization.getLocalText("LBL_total_entries_received"),
        Localization.getLocalText("LBL_total_msg_received"), Localization.getLocalText("LBL_total_msg_unacked"),
        };

    /**
     * Labels for ramjournal/ flashjournal table.
     */
    private static final String[] ELASTIC_DATA_LABELS = new String[]
        {
        Localization.getLocalText("LBL_node_id"), Localization.getLocalText("LBL_file_count"),
        Localization.getLocalText("LBL_max_journal_files"), Localization.getLocalText("LBL_max_file_size"),
        Localization.getLocalText("LBL_total_committed_bytes"), Localization.getLocalText("LBL_max_committed_bytes"),
        Localization.getLocalText("LBL_total_data_size"), Localization.getLocalText("LBL_compaction_count"),
        Localization.getLocalText("LBL_exhaustive_compaction_count"), Localization.getLocalText("LBL_current_collector_load_factor")
        };

    /**
     * Labels for JCache Configuration table.
     */
    private static final String[] JCACHE_CONFIG_LABELS = new String[]
        {
        Localization.getLocalText("LBL_config_cache"), Localization.getLocalText("LBL_key_type"),
        Localization.getLocalText("LBL_value_type"),  Localization.getLocalText("LBL_statistics_enabled"),
        Localization.getLocalText("LBL_read_through"), Localization.getLocalText("LBL_write_through"),
        Localization.getLocalText("LBL_store_by_value")
        };

    /**
     * Labels for JCache Statistics table.
     */
    private static final String[] JCACHE_STATS_LABELS = new String[]
        {
        Localization.getLocalText("LBL_config_cache"), Localization.getLocalText("LBL_total_puts"),
        Localization.getLocalText("LBL_total_gets"), Localization.getLocalText("LBL_removals"),
        Localization.getLocalText("LBL_cache_hits"), Localization.getLocalText("LBL_cache_misses"),
        Localization.getLocalText("LBL_evictions"), Localization.getLocalText("GRPH_average_get_time"),
        Localization.getLocalText("GRPH_average_put_time"), Localization.getLocalText("GRPH_average_remove_time"),
        Localization.getLocalText("LBL_hit_percentage"), Localization.getLocalText("LBL_miss_percentage")
        };

    /**
     * Labels for Http Proxy table.
     */
    private static final String[] HTTP_PROXY_LABELS = new String[]
        {
        Localization.getLocalText("LBL_service_name"), Localization.getLocalText("LBL_http_server_type"),
        Localization.getLocalText("LBL_members"),
        Localization.getLocalText("LBL_total_request_count"), Localization.getLocalText("LBL_total_error_count"),
        Localization.getLocalText("LBL_avg_request_per_second"), Localization.getLocalText("LBL_avg_request_time")
        };

    /**
     * Labels for Http Proxy Member table.
     */
    private static final String[] HTTP_PROXY_DETAIL_LABELS = new String[]
        {
        Localization.getLocalText("LBL_node_id"), Localization.getLocalText("LBL_ip_port"),
        Localization.getLocalText("LBL_avg_request_time"), Localization.getLocalText("LBL_avg_request_per_second"),
        Localization.getLocalText("LBL_total_request_count"), Localization.getLocalText("LBL_total_error_count")
        };

    /**
     * Default refresh time of 30 seconds.
     */
    private static final long DEFAULT_REFRESH_TIME = 30 * 1000L;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(VisualVMModel.class.getName());

    // ----- data members ---------------------------------------------------

    /**
     * The time between refresh of JMX data. Defaults to DEFAULT_REFRESH_TIME.
     */
    private long nRefreshTime;

    /**
     * Last time statistics were updated.
     */
    private long ldtLastUpdate = -1L;

    /**
     * Indicates if we should log detailed JMX query times for troubleshooting.
     */
    private boolean fLogJMXQueryTimes = false;

    /**
     * A {@link Map} of {@link List}s to store the retrieved data
     */
    private Map<DataType, List<Entry<Object, Data>>> mapCollectedData;

    /**
     * a {@link Map} of report Class and their loaded XML.
     */
    private Map<Class, String> mapReportXML;

    /**
     * The selected service for detailed service data.
     */
    private String sSelectedService = null;

    /**
     * The selected service for detailed service data in federation tab.
     */
    private String m_sSelectedServiceInFed = null;

    /**
     * The selected service for detail in HTTP proxy tab.
     */
    private String m_sSelectedHttpProxyService = null;

    /**
     * The selected node Id in outbound details in federation tab.
     */
    private String m_sOutboundNodeId = null;

    /**
     * The selected node Id in inbound details in federation tab.
     */
    private String m_sInboundNodeId = null;

    /**
     * The selected cache for detailed cache data.
     */
    private Pair<String, String> selectedCache = null;

    /**
     * The selected JCache cache for detailed JCache information.
     */
    private Pair<String, String> selectedJCache = null;


    /**
     * The selected service / participants pair in federation tab.
     */
    private Pair<String, String> selectedServiceParticipant = null;

    /**
     * Defines if the federation service is used.
     */
    private boolean fIsFederationAvailable = false;

    /**
     * Defines if we can get statistics directly from reporter. This is only valid for
     * a coherence version >= 12.1.3. An initial null value indicates that we have
     * not yet determined if we can use the reporter.
     */
    private Boolean fReporterAvailable = null;

    /**
     * Defines if we are running Coherence 12.1.3 or above
     */
    private Boolean fis1213AndAbove = null;

    /**
     * Defines if we want to include the NameService in the list of proxy servers.
     */
    private boolean fIncludeNameService = false;

    /**
     * Defines is proxy servers were present when we first collected stats.
     */
    private Boolean fIsCoherenceExtendConfigured = null;

    /**
     * Map of instances of data retrievers for execution of actual JMX queries.
     */
    private Map<Class, DataRetriever> mapDataRetrievers = new HashMap<Class, DataRetriever>();

    /**
     * The set of distributed caches so that we don't double count replicated
     * or optimistic caches.
     */
    private Set<String> setKnownDistributedCaches;

    /**
     * The set of domainPartition key values to check for connection
     * to WebLogicServer MT environment.
     */
    private Set<String> setDomainPartitions = new HashSet<>();

    /**
     * The cluster version as a String.
     */
    private String m_sClusterVersion;

    /**
     * The cluster version as an integer for comparison.
     */
    private int m_nClusterVersion;

    /**
     * Indicates if "Load Average" is available for the cluster being sampled.
     * If "SystemLoadAverage" attribute returns -1, then this means we are on
     * Windows (tm) platform and we should use the "SystemCPULoad" instead.
     */
    private boolean m_fIsLoadAverageAvailable = true;
    }
