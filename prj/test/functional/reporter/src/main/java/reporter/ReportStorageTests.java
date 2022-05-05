/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package reporter;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.reporter.ReportBatch;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.hamcrest.Matchers;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;

/**
 * Tests to confirm storage manager report can be run.
 *
 * @author tam 2022-04-07
 */
public class ReportStorageTests
        extends AbstractFunctionalTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.management", "all");

        AbstractFunctionalTest._startup();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testStorageManagerReport()
        {
        String sServer = "StorageMangerReport";

        try
            {
            String      sProjectName = "ReportStorage";
            ReportBatch batch        = new ReportBatch();

            startCacheServer(sServer + "-1", sProjectName);
            startCacheServer(sServer + "-2", sProjectName);

            NamedCache<String, String> cache    = CacheFactory.getCache("test");
            PartitionedService         service  = (PartitionedService) cache.getCacheService();

            cache.put("key", "value");

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));

            Eventually.assertDeferred(batch.runTabularReport(REPORT).toString(), () -> batch.runTabularReport(REPORT).keySet().size(), Matchers.is(2));
            }
        catch (Exception e)
            {
            e.printStackTrace();
            throw new RuntimeException("Error from test: " + e.getMessage());
            }
        finally
            {
            stopCacheServer(sServer + "-1");
            stopCacheServer(sServer + "-2");
            }
        }

    private static final String REPORT = "reports/report-cache-storage.xml";
    }
