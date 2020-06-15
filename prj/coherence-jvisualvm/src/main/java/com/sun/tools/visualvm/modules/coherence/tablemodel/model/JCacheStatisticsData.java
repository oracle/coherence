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

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.management.MBeanServerConnection;

/**
 * A class to hold JCache statistics information.
 *
 * @author tam  2014.09.22
 * @since   12.1.3
 */
public class JCacheStatisticsData
        extends AbstractData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create JCacheStatisticsData passing in the number of columns.
     */
    public JCacheStatisticsData()
        {
        super(CACHE_MISS_PERCENTAGE + 1);
        }

    // ----- DataRetriever methods ------------------------------------------

    /**
     * {@inheritDoc}
     */
    public List<Map.Entry<Object, Data>> getJMXData(RequestSender requestSender, VisualVMModel model)
        {
        // only available via report
        return null;
        }

    /**
     * {@inheritDoc}
     */
    public String getReporterReport()
        {
        return REPORT_JCACHE_CONFIGURATION;
        }

    /**
     * {@inheritDoc}
     */
    public Data processReporterData(Object[] aoColumns, VisualVMModel model)
        {
        Data data = new JCacheStatisticsData();

        // the identifier for this row is the configuration name and cache name
        Pair<String, String> key = new Pair<String, String>(aoColumns[2].toString(), aoColumns[3].toString());

        data.setColumn(JCacheStatisticsData.CACHE_MANAGER, key);
        data.setColumn(JCacheStatisticsData.CACHE_GETS, new Long(getNumberValue(aoColumns[4].toString())));
        data.setColumn(JCacheStatisticsData.CACHE_PUTS, new Long(getNumberValue(aoColumns[5].toString())));
        data.setColumn(JCacheStatisticsData.CACHE_REMOVALS, new Long(getNumberValue(aoColumns[6].toString())));
        data.setColumn(JCacheStatisticsData.CACHE_HITS, new Long(getNumberValue(aoColumns[7].toString())));
        data.setColumn(JCacheStatisticsData.CACHE_MISSES, new Long(getNumberValue(aoColumns[8].toString())));
        data.setColumn(JCacheStatisticsData.CACHE_EVICTIONS, new Long(getNumberValue(aoColumns[9].toString())));
        data.setColumn(JCacheStatisticsData.AVERAGE_GET_TIME, new Float(aoColumns[10].toString()));
        data.setColumn(JCacheStatisticsData.AVERAGE_PUT_TIME, new Float(aoColumns[11].toString()));
        data.setColumn(JCacheStatisticsData.AVERAGE_REMOVE_TIME, new Float(aoColumns[12].toString()));
        data.setColumn(JCacheStatisticsData.CACHE_HIT_PERCENTAGE, new Float(aoColumns[13].toString()));
        data.setColumn(JCacheStatisticsData.CACHE_MISS_PERCENTAGE, new Float(aoColumns[14].toString()));

        return data;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel model, HttpRequestSender requestSender) throws Exception
        {
        return null;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Array index for cache manager.
     */
    public static final int CACHE_MANAGER = 0;

    /**
     * Array index for cache gets.
     */
    public static final int CACHE_GETS = 1;

    /**
     * Array index for cache puts.
     */
    public static final int CACHE_PUTS = 2;

    /**
     * Array index for cache removes.
     */
    public static final int CACHE_REMOVALS = 3;

    /**
     * Array index for cache hits.
     */
    public static final int CACHE_HITS = 4;

    /**
     * Array index for cache misses.
     */
    public static final int CACHE_MISSES = 5;

    /**
     * Array index for cache evictions.
     */
    public static final int CACHE_EVICTIONS = 6;

    /**
     * Array index for average get time.
     */
    public static final int AVERAGE_GET_TIME = 7;

    /**
     * Array index for average put time.
     */
    public static final int AVERAGE_PUT_TIME = 8;

    /**
     * Array index for average remove time.
     */
    public static final int AVERAGE_REMOVE_TIME = 9;

    /**
     * Array index for cache hit percentage.
     */
    public static final int CACHE_HIT_PERCENTAGE = 10;

    /**
     * Array index for cache miss percentage.
     */
    public static final int CACHE_MISS_PERCENTAGE = 11;

    /**
     * Report for cluster data.
     */
    public static final String REPORT_JCACHE_CONFIGURATION = "reports/visualvm/jcache-statistics.xml";
    }
