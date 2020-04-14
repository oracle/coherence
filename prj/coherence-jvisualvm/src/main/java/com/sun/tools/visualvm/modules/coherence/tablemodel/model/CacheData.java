/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

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

                data.setColumn(CacheData.SIZE, Integer.valueOf(0));
                data.setColumn(CacheData.MEMORY_USAGE_BYTES, Long.valueOf(0));
                data.setColumn(CacheData.MEMORY_USAGE_MB, Integer.valueOf(0));
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

                        if (fIsDistributedCache || (!fIsDistributedCache && !fisSizeCounted))
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
                    data.setColumn(CacheData.AVG_OBJECT_SIZE, Integer.valueOf(0));
                    data.setColumn(CacheData.MEMORY_USAGE_BYTES, Integer.valueOf(0));
                    data.setColumn(CacheData.MEMORY_USAGE_MB, Integer.valueOf(0));
                    }
                else {
                    if ((Integer) data.getColumn(CacheData.SIZE) != 0)
                        {
                        data.setColumn(CacheData.AVG_OBJECT_SIZE,
                                       (Long) data.getColumn(CacheData.MEMORY_USAGE_BYTES)
                                       / (Integer) data.getColumn(CacheData.SIZE));
                        }

                    Long nMemoryUsageMB = ((Long) data.getColumn(CacheData.MEMORY_USAGE_BYTES)) / 1024 / 1024;

                    data.setColumn(CacheData.MEMORY_USAGE_MB, Integer.valueOf(nMemoryUsageMB.intValue()));
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
        Pair<String, String> key = new Pair<String, String>(aoColumns[2].toString(), aoColumns[3].toString());

        data.setColumn(CacheData.CACHE_NAME, key);
        data.setColumn(CacheData.SIZE, new Integer(getNumberValue(aoColumns[4].toString())));
        data.setColumn(CacheData.MEMORY_USAGE_BYTES, new Long(getNumberValue(aoColumns[5].toString())));
        data.setColumn(CacheData.MEMORY_USAGE_MB, new Integer(getNumberValue(aoColumns[5].toString())) / 1024 / 1024);

        if (aoColumns[7] != null)
            {
            data.setColumn(CacheData.AVG_OBJECT_SIZE, new Integer(getNumberValue(aoColumns[7].toString())));
            }
        else
            {
            data.setColumn(CacheData.AVG_OBJECT_SIZE, Integer.valueOf(0));
            }

        return data;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel model, HttpRequestSender requestSender)
            throws Exception
        {
        // no reports being used, hence using default functionality provided in getJMXData
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
