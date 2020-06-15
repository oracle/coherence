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

import java.util.logging.Logger;

import javax.management.MBeanServerConnection;

/**
 * A class to hold JCache configuration information.
 *
 * @author tam  2014.09.22
 * @since   12.1.3
 */
public class JCacheConfigurationData
        extends AbstractData
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create JCacheConfigurationData passing in the number of columns.
     */
    public JCacheConfigurationData()
        {
        super(STORE_BY_VALUE + 1);
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
        Data data = new JCacheConfigurationData();
        // the identifier for this row is the configuration name and cache name
        Pair<String, String> key = new Pair<String, String>(aoColumns[2].toString(), aoColumns[3].toString());

        data.setColumn(JCacheConfigurationData.CACHE_MANAGER, key);
        data.setColumn(JCacheConfigurationData.KEY_TYPE, aoColumns[4]);
        data.setColumn(JCacheConfigurationData.VALUE_TYPE, aoColumns[5]);
        data.setColumn(JCacheConfigurationData.STATISTICS_ENABLED, aoColumns[6]);
        data.setColumn(JCacheConfigurationData.READ_THROUGH, aoColumns[7]);
        data.setColumn(JCacheConfigurationData.WRITE_THROUGH, aoColumns[8]);
        data.setColumn(JCacheConfigurationData.STORE_BY_VALUE, aoColumns[9]);

        return data;
        }

    @Override
    public SortedMap<Object, Data> getAggregatedDataFromHttpQuerying(VisualVMModel     model,
                                                                     HttpRequestSender requestSender)
            throws Exception
        {
        return null;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Array index for cache manager.
     */
    public static final int CACHE_MANAGER = 0;

    /**
     * Array index for key type.
     */
    public static final int KEY_TYPE = 1;

    /**
     * Array index for value type.
     */
    public static final int VALUE_TYPE = 2;

    /**
     * Array index for statistics enabled.
     */
    public static final int STATISTICS_ENABLED = 3;

    /**
     * Array index for read through.
     */
    public static final int READ_THROUGH = 4;

    /**
     * Array index for write through.
     */
    public static final int WRITE_THROUGH = 5;

    /**
     * Array index for store by value.
     */
    public static final int STORE_BY_VALUE = 6;

    /**
     * Report for cluster data.
     */
    public static final String REPORT_JCACHE_CONFIGURATION = "reports/visualvm/jcache-configuration.xml";
    }
