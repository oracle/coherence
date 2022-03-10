/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package reporter;

import com.oracle.bedrock.runtime.coherence.options.CacheConfig;

import com.tangosol.coherence.reporter.ReportBatch;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.Base;
import com.tangosol.util.Service;

import common.AbstractFunctionalTest;

import javax.management.openmbean.TabularData;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A set of tests to verify the fix for COH-24823 where the number of rows in the
 * Proxy service report does not change as nodes or proxy services are added to or
 * removed from the running cluster.
 *
 * @author phf  2022.02.23
 */
public class Coh24823Tests
        extends AbstractFunctionalTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty(CacheConfig.PROPERTY, CACHE_CONFIG);
        System.setProperty("tangosol.coherence.management", "local-only");

        AbstractFunctionalTest._startup();
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Verify that the number of rows in the proxy report corresponds to the number of running proxy services.
     */
    @Test
    public void testProxyReportRowCount()
        {
        ReportBatch batch = new ReportBatch();

        // start Proxy1; the proxy report should have one row
        CacheFactory.getService(PROXY1_SERVICE);
        TabularData data = batch.runTabularReport(REPORT_PROXY);
        Assert.assertEquals(data.toString(), 1, data.keySet().size());

        // start Proxy2; the proxy report should have two rows
        Service proxy2 = CacheFactory.getService(PROXY2_SERVICE);
        data = batch.runTabularReport(REPORT_PROXY);
        Assert.assertEquals(data.toString(), 2, data.keySet().size());

        // stop Proxy2; the proxy report should be back to just one row
        proxy2.shutdown();
        int i = 0;
        while (proxy2.isRunning() && i < 10)
            {
            Base.sleep(1000);
            i++;
            }
        data = batch.runTabularReport(REPORT_PROXY);
        Assert.assertEquals(data.toString(), 1, data.keySet().size());
        }

    // ----- data members ---------------------------------------------------

    /**
     * Proxy service names.
     */
    private static final String PROXY1_SERVICE = "Proxy1";
    private static final String PROXY2_SERVICE = "Proxy2";

    /**
     * Proxy report file.
     */
    private static final String REPORT_PROXY = "reports/report-proxy.xml";

    /**
     * Cache configuration.
     */
    private static final String CACHE_CONFIG = "reporter-proxy-cache-config.xml";
    }
