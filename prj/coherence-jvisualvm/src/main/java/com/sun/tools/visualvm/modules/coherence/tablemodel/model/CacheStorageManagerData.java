/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

import com.fasterxml.jackson.databind.JsonNode;

import com.sun.tools.visualvm.modules.coherence.VisualVMModel;

import com.sun.tools.visualvm.modules.coherence.helper.HttpRequestSender;
import com.sun.tools.visualvm.modules.coherence.helper.RequestSender;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeList;
import javax.management.ObjectName;

import static com.sun.tools.visualvm.modules.coherence.helper.JMXUtils.getAttributeValueAsString;

/**
 * A class to hold detailed cache storage data.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public class CacheStorageManagerData
        extends AbstractData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create CacheStorageManagerData passing in the number of columns.
     */
    public CacheStorageManagerData()
        {
        super(INDEX_TOTAL_UNITS + 1);
        }

    // ----- DataRetriever methods ------------------------------------------

    /**
     * {@inheritDoc}
     */
    public List<Map.Entry<Object, Data>> getJMXData(RequestSender requestSender, VisualVMModel model)
        {
        SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();
        Data                    data;
        Pair<String, String> selectedCache = model.getSelectedCache();

        if (selectedCache != null)
            {
            try
                {
                // see if we have domainPartition key
                String[] asServiceDetails = getDomainAndService(selectedCache.getX());
                String   sDomainPartition = asServiceDetails[0];
                String   sServiceName     = asServiceDetails[1];

                Set<ObjectName> resultSet = requestSender.getCacheStorageMembers(sServiceName, selectedCache.getY(),
                        sDomainPartition);

                for (Iterator<ObjectName> iter = resultSet.iterator(); iter.hasNext(); )
                    {
                    ObjectName objName = iter.next();
                    String     sNodeId = objName.getKeyProperty("nodeId");

                    data = new CacheStorageManagerData();

                    AttributeList listAttr = requestSender.getAttributes(objName,
                        new String[]{ ATTR_LOCKS_GRANTED, ATTR_LOCKS_PENDING, ATTR_LISTENER_REG });

                    data.setColumn(CacheStorageManagerData.NODE_ID, new Integer(sNodeId));
                    data.setColumn(CacheStorageManagerData.LOCKS_GRANTED,
                            Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_LOCKS_GRANTED)));
                    data.setColumn(CacheStorageManagerData.LOCKS_PENDING,
                            Integer.parseInt(getAttributeValueAsString(listAttr, ATTR_LOCKS_PENDING)));
                    data.setColumn(CacheStorageManagerData.LISTENER_REGISTRATIONS,
                            Long.parseLong(getAttributeValueAsString(listAttr, ATTR_LISTENER_REG)));
                    try {
                        data.setColumn(CacheStorageManagerData.MAX_QUERY_DURATION,
                                   Long.parseLong(requestSender.getAttribute(objName, "MaxQueryDurationMillis")));
                        data.setColumn(CacheStorageManagerData.MAX_QUERY_DESCRIPTION,
                                   (String) requestSender.getAttribute(objName, "MaxQueryDescription"));
                        data.setColumn(CacheStorageManagerData.NON_OPTIMIZED_QUERY_AVG,
                                   Long.parseLong(requestSender.getAttribute(objName, "NonOptimizedQueryAverageMillis")));
                        data.setColumn(CacheStorageManagerData.OPTIMIZED_QUERY_AVG,
                                Long.parseLong(requestSender.getAttribute(objName, "OptimizedQueryAverageMillis")));
                        data.setColumn(CacheStorageManagerData.INDEX_TOTAL_UNITS,
                                Long.parseLong(requestSender.getAttribute(objName, "IndexTotalUnits")));
                        }
                    catch (Exception eIgnore)
                       {
                       // ignore errors as these attributes are not available until 3.7.
                       // Refer: COH-11034
                       }

                    mapData.put(data.getColumn(0), data);
                    }

                return new ArrayList<Map.Entry<Object, Data>>(mapData.entrySet());
                }
            catch (Exception e)
                {
                LOGGER.log(Level.WARNING, "Error getting cache storage managed statistics", e);

                return null;
                }
            }
        else
            {
            // no selected service, so don't query the storage manager data
            return null;
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String preProcessReporterXML(VisualVMModel model, String sReporterXML)
        {
        // the report XML contains the following tokens that require substitution:
        // %SERVICE_NAME%
        // %CACHE_NAME%

        Pair<String, String> selectedCache = model.getSelectedCache();

        // see if we have domainPartition key
        String sServiceName     = null;
        String sDomainPartition = null;

        if (selectedCache != null)
            {
            String[] asServiceDetails = getDomainAndService(selectedCache.getX());
            sServiceName              = asServiceDetails[1];
            sDomainPartition          = asServiceDetails[0];
            }

        return sServiceName == null ? sReporterXML :
                     sReporterXML.replaceAll("%SERVICE_NAME%", sServiceName +
                                             (sDomainPartition != null ? ",domainPartition=" + sDomainPartition : "") )
                                 .replaceAll("%CACHE_NAME%",   selectedCache.getY());
        }

    /**
     * {@inheritDoc}
     */
    public String getReporterReport()
        {
        return REPORT_STORAGE_MANAGER;
        }

    /**
     * {@inheritDoc}
     */
    public Data processReporterData(Object[] aoColumns, VisualVMModel model)
        {
        Data data = new CacheStorageManagerData();

        data.setColumn(CacheStorageManagerData.NODE_ID,
                       new Integer(getNumberValue(aoColumns[2].toString())));
        data.setColumn(CacheStorageManagerData.LOCKS_GRANTED,
                       new Integer(getNumberValue(aoColumns[3].toString())));
        data.setColumn(CacheStorageManagerData.LOCKS_PENDING,
                       new Integer(getNumberValue(aoColumns[4].toString())));
        data.setColumn(CacheStorageManagerData.LISTENER_REGISTRATIONS,
                       new Long(getNumberValue(aoColumns[5].toString())));
        data.setColumn(CacheStorageManagerData.MAX_QUERY_DURATION,
                       new Long(getNumberValue(aoColumns[6].toString())));
        data.setColumn(CacheStorageManagerData.MAX_QUERY_DESCRIPTION,
                       new String(aoColumns[7] == null ? "" : aoColumns[7].toString()));
        data.setColumn(CacheStorageManagerData.NON_OPTIMIZED_QUERY_AVG,
                       new Long(getNumberValue(aoColumns[8].toString())));
        data.setColumn(CacheStorageManagerData.OPTIMIZED_QUERY_AVG,
                       new Long(getNumberValue(aoColumns[9].toString())));

        try
            {
            data.setColumn(CacheStorageManagerData.INDEX_TOTAL_UNITS,
                       new Long(getNumberValue(aoColumns[10].toString())));
            }
        catch (Exception e)
            {
            // if we connect to a coherence version 12.1.3.X then this
            // attribute was not included in MBean and reports so we must
            // protect against NPE
            }

        return data;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel     model,
                                                                     HttpRequestSender requestSender)
            throws Exception
        {
        Pair<String, String> selectedCache = model.getSelectedCache();

        if (selectedCache == null)
            {
            return null;
            }

        // see if we have domainPartition key
        String sServiceName     = null;
        String sDomainPartition = null;

        if (selectedCache != null)
            {
            String[] asServiceDetails = getDomainAndService(selectedCache.getX());
            sServiceName              = asServiceDetails[1];
            sDomainPartition          = asServiceDetails[0];
            }

        JsonNode rootNode = requestSender.getDataForStorageManagerMembers(sServiceName, sDomainPartition, selectedCache.getY());

        SortedMap<Object, Data> mapData        = new TreeMap<Object, Data>();
        JsonNode                nodeCacheItems = rootNode.get("items");

        if (nodeCacheItems != null && nodeCacheItems.isArray())
            {
            for (int i = 0; i < nodeCacheItems.size(); i++)
                {
                JsonNode nodeCacheStorage = nodeCacheItems.get(i);
                Data     data             = new CacheStorageManagerData();

                data.setColumn(CacheStorageManagerData.NODE_ID,
                        Integer.valueOf(nodeCacheStorage.get("nodeId").asText()));
                data.setColumn(CacheStorageManagerData.LOCKS_GRANTED,
                        Integer.valueOf(nodeCacheStorage.get("locksGranted").asText()));
                data.setColumn(CacheStorageManagerData.LOCKS_PENDING,
                        Integer.valueOf(nodeCacheStorage.get("locksPending").asText()));
                data.setColumn(CacheStorageManagerData.LISTENER_REGISTRATIONS,
                        Long.valueOf(nodeCacheStorage.get("listenerRegistrations").asText()));
                data.setColumn(CacheStorageManagerData.MAX_QUERY_DURATION,
                        Long.valueOf(nodeCacheStorage.get("maxQueryDurationMillis").asText()));
                data.setColumn(CacheStorageManagerData.MAX_QUERY_DESCRIPTION,
                        nodeCacheStorage.get("maxQueryDescription").asText());
                data.setColumn(CacheStorageManagerData.NON_OPTIMIZED_QUERY_AVG,
                        Long.valueOf(nodeCacheStorage.get("nonOptimizedQueryAverageMillis").asText()));
                data.setColumn(CacheStorageManagerData.OPTIMIZED_QUERY_AVG,
                        Long.valueOf(nodeCacheStorage.get("optimizedQueryAverageMillis").asText()));
                data.setColumn(CacheStorageManagerData.INDEX_TOTAL_UNITS,
                        Long.valueOf(nodeCacheStorage.get("indexTotalUnits").asText()));

                mapData.put(data.getColumn(0), data);
                }
            }
        return mapData;
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = -7989560126715725202L;

    /**
     * Array index for node id.
     */
    public static final int NODE_ID = 0;

    /**
     * Array index for locks granted.
     */
    public static final int LOCKS_GRANTED = 1;

    /**
     * Array index for locks pending.
     */
    public static final int LOCKS_PENDING = 2;

    /**
     * Array index for listener registrations.
     */
    public static final int LISTENER_REGISTRATIONS = 3;

    /**
     * Array index for max query duration.
     */
    public static final int MAX_QUERY_DURATION = 4;

    /**
     * Array index for max query descriptions.
     */
    public static final int MAX_QUERY_DESCRIPTION = 5;

    /**
     * Array index for non-optimized query avg.
     */
    public static final int NON_OPTIMIZED_QUERY_AVG = 6;

    /**
     * Array index for optimized query avg.
     */
    public static final int OPTIMIZED_QUERY_AVG = 7;

    /**
     * Array index for index total units avg.
     */
    public static final int INDEX_TOTAL_UNITS = 8;

    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(CacheStorageManagerData.class.getName());

    /**
     * Report for storage manager data.
     */
    public static final String REPORT_STORAGE_MANAGER = "reports/visualvm/cache-storage-manager-stats.xml";

    /**
     * JMX attribute name for Locks Granted.
     */
    protected static final String ATTR_LOCKS_GRANTED = "LocksGranted";

    /**
     * JMX attribute name for Locks Pending
     */
    protected static final String ATTR_LOCKS_PENDING = "LocksPending";

    /**
     * JMX attribute name for Listener Registrations.
     */
    protected static final String ATTR_LISTENER_REG = "ListenerRegistrations";
    }
