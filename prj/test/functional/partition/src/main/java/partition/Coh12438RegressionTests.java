/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package partition;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.partition.PartitionListener;
import com.tangosol.net.partition.PartitionEvent;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.Listeners;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.EventListener;

/**
 * Regression test for COH-12438
 *
 * @author jf 2014.12.03
 */
public class Coh12438RegressionTests
        extends AbstractFunctionalTest
    {
    /**
     * Constructs regression test with a cache config that statically configures a
     * PartitionListener for cache scheme used by this test.
     */
    public Coh12438RegressionTests()
        {
        super("regression-cache-config.xml");
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.distributed.localstorage", "false");
        AbstractFunctionalTest._startup();
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Test Partition Listener not instantiated on a storage disabled node
     */
    @Test
    public void testCacheConfiguredPartitionListenerNotInstantiatedWithStorageDisabledNode()
        {
        try
            {
            NamedCache   cache    = getNamedCache("COH12438");
            CacheService service  = cache.getCacheService();
            Object[]     aoParams =
                {
                };

            PartitionedService svc = (PartitionedService) ClassHelper.invoke(service.getClass(), service, "getService",
                                         aoParams);

            Listeners listeners = (Listeners) ClassHelper.invoke(svc.getClass(), svc, "getPartitionListeners",
                                      aoParams);
            EventListener[] aoListeners = listeners.listeners();

            assertTrue("verify no instantiated PartitionedListener on a storage-disabled server",
                       aoListeners.length == 0);
            }
        catch (Exception e)
            {
            fail("Failed to confirm that configured PartitionListener was not instantiated on a Storage disabled node");
            }
        }

    // ----- inner class TestPartitionListener -----------------------------

    static public class TestPartitionListener
            implements PartitionListener
        {

        /**
         * Should never be called for this test.
         */
        public TestPartitionListener()
            {
            System.out.println("########################## new");
            Thread.dumpStack();
            fail("should not be instantiated on a local storage disabled");
            }

        @Override
        public void onPartitionEvent(PartitionEvent evt)
            {
            System.out.println("########################## onPartitionEvent: " + evt);
            }
        }
    }
