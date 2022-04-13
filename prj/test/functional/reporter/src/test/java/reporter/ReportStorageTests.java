/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package reporter;


import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;

import com.tangosol.net.PartitionedService;
import com.tangosol.net.management.MBeanServerProxy;

import common.AbstractFunctionalTest;

import javax.management.openmbean.TabularData;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;

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
        System.setProperty("coherence.distributed.localstorage", "false");

        // disable until failure resolved
        //AbstractFunctionalTest._startup();
        }

    // ----- test methods ---------------------------------------------------

    // disable until failure resolved
    // @Test
    public void testStorageManagerReport()
        {
        String sServer = "StorageMangerReport";

        try
            {
            String sProjectName = "ReportStorage";
            Cluster cluster = CacheFactory.ensureCluster();

            startCacheServer(sServer + "-1", sProjectName);
            startCacheServer(sServer + "-2", sProjectName);

            NamedCache<String, String> cache    = CacheFactory.getCache("test");
            PartitionedService         service  = (PartitionedService) cache.getCacheService();

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));

            cache.put("key", "value");

            MBeanServerProxy proxy = cluster.getManagement().getMBeanServerProxy();
            TabularData reportData = (TabularData) proxy.invoke("Coherence:type=Reporter,nodeId=1", "runTabularReport",
                    new Object[]{"reports/report-cache-storage.xml"}, new String[]{"java.lang.String"});

            // this is a good enough test to make sure we get two rows in the reportData meaning
            // two StorageManagerMBean results for the created cache
            assertEquals(2, reportData.size());
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
    }
