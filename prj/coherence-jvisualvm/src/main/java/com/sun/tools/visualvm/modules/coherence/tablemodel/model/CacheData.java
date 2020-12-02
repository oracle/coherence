/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

import static com.sun.tools.visualvm.modules.coherence.helper.JMXUtils.getAttributeValue;
import static com.sun.tools.visualvm.modules.coherence.helper.JMXUtils.getAttributeValueAsString;

/**
 * A class to hold basic cache size data.
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public class CacheData
        extends AbstractData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create CacheData passing in the number of columns.
     *
     */
    public CacheData()
        {
        super(UNIT_CALCULATOR + 1);
        }

    // ----- DataRetriever methods ------------------------------------------

    /**
     * {@inheritDoc}
     */
    public List<Map.Entry<Object, Data>> getJMXData(RequestSender sender, VisualVMModel model)
        {
        SortedMap<Object, Data> mapData = new TreeMap<Object, Data>();
        Data                    data;

        try
            {
            if (sender instanceof HttpRequestSender)
                {
                // we can more effectively retrieve the data using REST aggregations
                SortedMap<Object, Data> mapAggregatedData = getAggregatedDataFromHttpQuerying(model, (HttpRequestSender) sender);
                if (mapAggregatedData != null)
                    {
                    return new ArrayList<>(mapAggregatedData.entrySet());
                    }
                // else fall through and use less efficient way
                }

            // get the list of caches
            Set<ObjectName> cacheNamesSet = sender.getAllCacheMembers();

            for (Iterator<ObjectName> cacheNameIter = cacheNamesSet.iterator(); cacheNameIter.hasNext(); )
                {
                ObjectName cacheNameObjName = cacheNameIter.next();

                String     sCacheName       = cacheNameObjName.getKeyProperty("name");
                String     sServiceName     = cacheNameObjName.getKeyProperty("service");
                String     sDomainPartition = cacheNameObjName.getKeyProperty("domainPartition");

                if (sDomainPartition != null)
                    {
                    sServiceName = getFullServiceName(sDomainPartition, sServiceName);
                    }

                Pair<String, String> key = new Pair<String, String>(sServiceName, sCacheName);

                data = new CacheData();

                data.setColumn(CacheData.SIZE, 0);
                data.setColumn(CacheData.MEMORY_USAGE_BYTES, 0L);
                data.setColumn(CacheData.MEMORY_USAGE_MB, 0);
                data.setColumn(CacheData.CACHE_NAME, key);

                mapData.put(key, data);
                }

            // loop through each cache and find all the different node entries for the caches
            // and aggregate the information.

            for (Iterator cacheNameIter = mapData.keySet().iterator(); cacheNameIter.hasNext(); )
                {
                Pair<String, String> key                 = (Pair<String, String>) cacheNameIter.next();
                String               sCacheName          = key.getY();
                String               sRawServiceName     = key.getX();
                Set<String>          setDistributedCache = model.getDistributedCaches();

                if (setDistributedCache == null)
                    {
                    throw new RuntimeException("setDistributedCache must not be null. Make sure SERVICE is before CACHE in enum.");
                    }

                boolean fIsDistributedCache = setDistributedCache.contains(sRawServiceName);

                String[] asServiceDetails = getDomainAndService(sRawServiceName);
                String   sDomainPartition = asServiceDetails[0];
                String   sServiceName     = asServiceDetails[1];

                Set resultSet = sender.getCacheMembers(sServiceName, sCacheName, sDomainPartition);

                boolean fisSizeCounted = false;    // indicates if non dist cache size has been counted

                for (Iterator iter = resultSet.iterator(); iter.hasNext(); )
                    {
                    ObjectName objectName = (ObjectName) iter.next();

                    if (objectName.getKeyProperty("tier").equals("back"))
                        {
                        data = (CacheData) mapData.get(key);

                        if (fIsDistributedCache || !fisSizeCounted)
                            {
                            data.setColumn(CacheData.SIZE,
                                           (Integer) data.getColumn(CacheData.SIZE)
                                           + Integer.parseInt(sender.getAttribute(objectName, "Size")));

                            if (!fisSizeCounted)
                                {
                                fisSizeCounted = true;
                                }
                            }

                        AttributeList listAttr = sender.getAttributes(objectName,
                          new String[]{ CacheDetailData.ATTR_UNITS, CacheDetailData.ATTR_UNIT_FACTOR, MEMORY_UNITS});

                        data.setColumn(CacheData.MEMORY_USAGE_BYTES,
                                       (Long) data.getColumn(CacheData.MEMORY_USAGE_BYTES)
                                       + (Integer.parseInt(getAttributeValueAsString(listAttr, CacheDetailData.ATTR_UNITS)) * 1L *
                                          Integer.parseInt(getAttributeValueAsString(listAttr, CacheDetailData.ATTR_UNIT_FACTOR))));

                        // set unit calculator if its not already set
                        if (data.getColumn(UNIT_CALCULATOR) == null)
                            {
                            boolean fMemoryUnits = Boolean.valueOf(getAttributeValue(listAttr, MEMORY_UNITS).toString());
                            data.setColumn(CacheData.UNIT_CALCULATOR, fMemoryUnits ? "BINARY" : "FIXED");
                            }

                        mapData.put(key, data);
                        }
                    }

                // update the cache entry averages
                data = (CacheData) mapData.get(key);

                // for FIXED unit calculator make the memory bytes and MB and avg object size null
                if (data.getColumn(CacheData.UNIT_CALCULATOR).equals("FIXED"))
                    {
                    data.setColumn(CacheData.AVG_OBJECT_SIZE, 0);
                    data.setColumn(CacheData.MEMORY_USAGE_BYTES, 0);
                    data.setColumn(CacheData.MEMORY_USAGE_MB, 0);
                    }
                else {
                    if ((Integer) data.getColumn(CacheData.SIZE) != 0)
                        {
                        data.setColumn(CacheData.AVG_OBJECT_SIZE,
                                       (Long) data.getColumn(CacheData.MEMORY_USAGE_BYTES)
                                       / (Integer) data.getColumn(CacheData.SIZE));
                        }

                    Long nMemoryUsageMB = ((Long) data.getColumn(CacheData.MEMORY_USAGE_BYTES)) / 1024 / 1024;

                    data.setColumn(CacheData.MEMORY_USAGE_MB, nMemoryUsageMB.intValue());
                }

                mapData.put(key, data);
                }

            return new ArrayList<>(mapData.entrySet());

            }
        catch (Exception e)
            {
            LOGGER.log(Level.WARNING, "Error getting cache statistics", e);

            return null;
            }
        }

    /**
     * {@inheritDoc}
     */
    public String getReporterReport()
        {
        return null;    // until the following JIRA is fixed we cannot implement this

        // COH-10175:  Reporter does not Correctly Display Size of Replicated Or Optimistic Cache
        // return REPORT_CACHE_SIZE;
        }

    /**
     * {@inheritDoc}
     */
    public Data processReporterData(Object[] aoColumns, VisualVMModel model)
        {
        Data data = new CacheData();

        // the identifier for this row is the service name and cache name
        Pair<String, String> key = new Pair<>(aoColumns[2].toString(), aoColumns[3].toString());

        data.setColumn(CacheData.CACHE_NAME, key);
        data.setColumn(CacheData.SIZE, Integer.valueOf(getNumberValue(aoColumns[4].toString())));
        data.setColumn(CacheData.MEMORY_USAGE_BYTES, Long.valueOf(getNumberValue(aoColumns[5].toString())));
        data.setColumn(CacheData.MEMORY_USAGE_MB, Integer.valueOf(getNumberValue(aoColumns[5].toString())) / 1024 / 1024);

        if (aoColumns[7] != null)
            {
            data.setColumn(CacheData.AVG_OBJECT_SIZE, Integer.valueOf(getNumberValue(aoColumns[7].toString())));
            }
        else
            {
            data.setColumn(CacheData.AVG_OBJECT_SIZE, 0);
            }

        return data;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel model, HttpRequestSender requestSender)
            throws Exception
        {
        // minimize the number of round-trips by querying each of the services and getting the cache details
        // this will be one per service
        List<Map.Entry<Object, Data>> serviceData = model.getData(VisualVMModel.DataType.SERVICE);
        if (serviceData != null && serviceData.size() > 0)
            {
            final SortedMap<Object, Data> mapData = new TreeMap<>();
            
            for (Map.Entry<Object, Data> service : serviceData)
                {
                String   sService            = (String) service.getKey();
                String[] asServiceDetails    = getDomainAndService(sService);
                String   sDomainPartition    = asServiceDetails[0];
                String   sServiceName        = asServiceDetails[1];
                
                JsonNode listOfServiceCaches = requestSender.getListOfServiceCaches(sServiceName, sDomainPartition);
                JsonNode itemsNode           = listOfServiceCaches.get("items");
                boolean  fisDistributed      = model.getDistributedCaches().contains(sServiceName);

                if (itemsNode != null && itemsNode.isArray())
                    {
                    for (int i = 0; i < ((ArrayNode) itemsNode).size(); i++)
                        {
                        Data     data         = new CacheData();
                        JsonNode cacheDetails = itemsNode.get(i);
                        String   sCacheName   = cacheDetails.get("name").asText();

                        Pair<String, String> key = new Pair<>(sServiceName, sCacheName);

                        JsonNode nodeSize = cacheDetails.get("size");
                        if (nodeSize == null)
                            {
                            // Connecting to version without Bug 32134281 fix so force less efficient way
                            return null;
                            }
                        int nCacheSize = nodeSize.asInt();

                        String sUnitCalculator = getChildValue("true", "memoryUnits", cacheDetails) != null
                                ? "BINARY" : "FIXED";
                        int nMembers = Integer.parseInt(getChildValue("count", "averageMissMillis", cacheDetails));
                        
                        data.setColumn(CACHE_NAME, key);

                        // if is replicated so the actual size has been aggregated so we must divide by the member count
                        data.setColumn(SIZE, fisDistributed ? nCacheSize : nCacheSize / nMembers);

                        data.setColumn(UNIT_CALCULATOR, sUnitCalculator);
                        long cMemoryUsageBytes = Long.parseLong(getFirstMemberOfArray(cacheDetails, "unitFactor"))
                                            * cacheDetails.get("units").asLong();
                        data.setColumn(MEMORY_USAGE_BYTES, cMemoryUsageBytes);
                        data.setColumn(MEMORY_USAGE_MB, (int) (cMemoryUsageBytes / 1024 / 1024));

                        if (data.getColumn(CacheData.UNIT_CALCULATOR).equals("FIXED"))
                            {
                            data.setColumn(CacheData.AVG_OBJECT_SIZE, 0);
                            data.setColumn(CacheData.MEMORY_USAGE_BYTES, 0);
                            data.setColumn(CacheData.MEMORY_USAGE_MB, 0);
                            }
                        else
                            {
                            if (nCacheSize != 0)
                                {
                                data.setColumn(CacheData.AVG_OBJECT_SIZE, (int) (cMemoryUsageBytes / nCacheSize));
                                }
                            }

                        mapData.put(key, data);
                        }
                    }
                }
                return mapData;
            }

        return null;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SortedMap<Object, Data> postProcessReporterData(SortedMap<Object, Data> mapData, VisualVMModel model)
        {
        Set<String> setDistributedCache = model.getDistributedCaches();

        if (setDistributedCache == null)
            {
            throw new RuntimeException("setDistributedCache must not be null. Make sure SERVICE is before CACHE in enum.");
            }

        // we need to check to see if this cache is not a distributed cache and adjust the
        // size accordingly - The problem is that currently we have no way of identifying the number of storage
        // enabled members - so this is currently going to report incorrect sizes for Replicated or Optomistic
        // caches

        return mapData;
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = 6427775621469258645L;

    /**
     * Array index for cache name.
     */
    public static final int CACHE_NAME = 0;

    /**
     * Array index for cache size.
     */
    public static final int SIZE = 1;

    /**
     * Array index for memory usage in bytes.
     */
    public static final int MEMORY_USAGE_BYTES = 2;

    /**
     * Array index for memory usage in MB.
     */
    public static final int MEMORY_USAGE_MB = 3;

    /**
     * Array index for average object size.
     */
    public static final int AVG_OBJECT_SIZE = 4;

    /**
     * Array index for unit calculator.
     */
    public static final int UNIT_CALCULATOR = 5;
    
    /**
     * The logger object to use.
     */
    private static final Logger LOGGER = Logger.getLogger(CacheData.class.getName());

    /**
     * Attribute for "MemoryUnits".
     */
    private static final String MEMORY_UNITS = "MemoryUnits";
    }
