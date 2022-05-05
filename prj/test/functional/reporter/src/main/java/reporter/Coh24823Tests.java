/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package reporter;

import com.oracle.bedrock.runtime.coherence.options.CacheConfig;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.reporter.ReportBatch;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.Service;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.is;

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
        Eventually.assertDeferred(batch.runTabularReport(REPORT_PROXY).toString(), () -> batch.runTabularReport(REPORT_PROXY).keySet().size(), is(1));

        // start Proxy2; the proxy report should have two rows
        Service proxy2 = CacheFactory.getService(PROXY2_SERVICE);
        Eventually.assertDeferred(batch.runTabularReport(REPORT_PROXY).toString(), () -> batch.runTabularReport(REPORT_PROXY).keySet().size(), is(2));

        // stop Proxy2; the proxy report should be back to just one row
        proxy2.shutdown();
        Eventually.assertDeferred(batch.runTabularReport(REPORT_PROXY).toString(), () -> batch.runTabularReport(REPORT_PROXY).keySet().size(), is(1));
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
