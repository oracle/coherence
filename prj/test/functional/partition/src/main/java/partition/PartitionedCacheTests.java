/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package partition;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.coherence.component.util.SafeService;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;

import com.tangosol.net.Action;
import com.tangosol.net.ActionPolicy;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.GuardSupport;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestPolicyException;
import com.tangosol.net.Service;

import com.tangosol.net.cache.ConfigurableCacheMap;

import com.tangosol.net.partition.ObservableSplittingBackingMap;
import com.tangosol.net.partition.PartitionEvent;
import com.tangosol.net.partition.PartitionListener;
import com.tangosol.net.partition.PartitionSplittingBackingMap;
import com.tangosol.net.partition.SimplePartitionKey;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ClassHelper;

import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.Listeners;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.SafeHashMap;

import com.tangosol.util.processor.AbstractProcessor;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.TestSynchronousMapListener;

import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static com.oracle.bedrock.deferred.DeferredHelper.delayedBy;

/**
* A collection of functional tests for partitioned NamedCache.
*
* @author gg  2009.09.10
*/
public class PartitionedCacheTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public PartitionedCacheTests()
        {
        super();
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");
        System.setProperty("coherence.distribution.2server", "false");

        AbstractFunctionalTest._startup();
        }


    // ----- test methods ---------------------------------------------------

    /**
    * Test the entrySet() during eviction (COH-2591)
    */
    @Test
    public void testEntrySetWithEviction()
        {
        final NamedCache cache = getNamedCache("dist-test");

        for (int i = 0; i < 10; i++)
            {
            cache.put(i, (long) i, 500L);
            }

        sleep(2000);

        final Set setResult = new HashSet();
        Runnable task = new Runnable()
            {
            public void run()
                {
                Set setEntry = cache.entrySet(); // COH-2591 - infinite loop
                setResult.addAll(setEntry);
                }
            };

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        try
            {
            thread.join(1000);
            }
        catch (InterruptedException e) {}

        if (thread.isAlive())
            {
            GuardSupport.logStackTraces();
            thread.interrupt();
            fail("Detected infinite loop in entrySet()");
            }

        assertTrue("not empty", setResult.isEmpty());
        }

    /**
    * Regression test for COH-3974.
    *
    * Install a quorum policy that blocks the put() thread long enough for a
    * concurrent destroyCache() to occur, triggering the COH-3974 condition.
    */
    @Test
    public void testCoh3974()
        {
        final NamedCache  cache      = getNamedCache("Coh3974");
        final Throwable[] aexcHolder = new Throwable[1];
        Thread thd1  = new Thread()
            {
            @Override
            public void run()
                {
                try
                    {
                    // this put() should not complete, due to concurrent destroyCache()
                    cache.put("foo", "bar");
                    fail("Failed to throw expected exception");
                    }
                catch (RequestPolicyException e)
                    {
                    // expected exception
                    }
                catch (Throwable t)
                    {
                    // unexpected exceptions (including assert failure)
                    aexcHolder[0] = t;
                    }
                }
            };
        Thread thd2  = new Thread()
            {
            @Override
            public void run()
                {
                cache.destroy();
                }
            };

        try
            {
            Coh3974QuorumPolicy.setThreads(thd1, thd2);
            thd1.start();
            // Note: delay starting thd2, until thd1 is blocked within
            //       the client-side put() call; this ensures that we will not
            //       "correctly" catch the destroyed cache at the SafeNamedCache

            try
                {
                thd1.join(3000L);
                assertFalse("Put() thread failed to complete", thd1.isAlive());
                }
            catch (InterruptedException e)
                {
                fail("Unexpected thread interruption: " + e);
                }

            Throwable t = aexcHolder[0];
            if (t != null)
                {
                Base.err(Base.getStackTrace(t));
                fail("Unexpected exception: " + t);
                }
            }
        finally
            {
            Coh3974QuorumPolicy.setThreads(null, null);
            }
        }

    /**
    * Regression test for COH-4211.
    *
    * Ensure that updates with an expiry send proper events
    */
    @Test
    public void testCoh4211()
        {
        NamedCache                 cache    = getNamedCache("dist-Coh4211");
        TestSynchronousMapListener listener = new TestSynchronousMapListener();

        try
            {
            cache.addMapListener(listener);
            cache.put("1", "2");
            cache.put("1", "2", 1000l);
            assertEquals(2, listener.getCount());
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
    * Test for COH-8795.
    *
    * Test that Binary references are reused during key listener registrations.
    */
    @Test
    public void testCoh8795()
        {
        NamedCache cache = getNamedCache("dist-Coh8795");
        Integer    oKey  = 10;
        try
            {
            cache.addMapListener(new TestSynchronousMapListener(), oKey, false);
            cache.put(oKey, "SomeValue");
            assertEquals(cache.invoke(oKey, new KeyReferenceCheckProcessor()), Boolean.TRUE);
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
    * Test that a single partition listener gets executed when we add second cache server. Part of COH-4081
    */
    @Test
    public void testSingleConfigPartitionListener()
        {
        NamedCache cache = getNamedCache("single-listener");

        SafeService      serviceSafe = (SafeService) cache.getCacheService();
        PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();
        try
            {
            Listeners listeners = serviceReal.getPartitionListeners();

            EventListener[] aoListeners = listeners.listeners();
            assertTrue(aoListeners.length == 1);

            startCacheServer("secondary", "partition", null, PROPS_SEONE);

            Eventually.assertThat(invoking(serviceReal).getOwnershipEnabledMembers().size(), is(2));

            Member      memberThis  = serviceReal.getCluster().getLocalMember();
            int         cPartitions = serviceReal.getPartitionCount();
            Set<Member> setMembers  = serviceReal.getOwnershipEnabledMembers();

            setMembers.remove(memberThis);

            Member memberThat = setMembers.iterator().next();
            Eventually.assertThat(invoking(this).veryThisThatCardinality(serviceReal, cPartitions, memberThis, memberThat), is(true));
            Eventually.assertThat(invoking(this).isDistributionOrTransferInProgress(serviceReal), is(false));

            int iCount = 0;
            for (int i = 0; i < aoListeners.length; i++)
                {
                iCount++;
                assertTrue(((TestPartitionListener) aoListeners[i]).executed);
                assertTrue(((TestPartitionListener) aoListeners[i]).partitionTransmitted);
                }

            assertTrue(iCount == 1);
            }
        catch (Exception e)
            {
            e.printStackTrace();
            fail("Unexpected failure..");
            }
        finally
            {
            stopCacheServer("secondary");
            stopCacheServer("third");
            }
        }

    /**
     * Test cache config file partitionListener with init-params
     */
    @Test
    public void testPartitionListenerWithInitParams()
        {
        NamedCache cache = getNamedCache("single-init-params-listener");

        SafeService      serviceSafe = (SafeService) cache.getCacheService();
        PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();
        try
            {
            Listeners listeners = serviceReal.getPartitionListeners();

            EventListener[] aoListeners = listeners.listeners();
            assertTrue(aoListeners.length == 1);

            startCacheServer("secondary", "partition", null, PROPS_SEONE);

            Eventually.assertThat(invoking(serviceReal).getOwnershipEnabledMembers().size(), is(2));

            Member      memberThis  = serviceReal.getCluster().getLocalMember();
            int         cPartitions = serviceReal.getPartitionCount();
            Set<Member> setMembers  = serviceReal.getOwnershipEnabledMembers();

            setMembers.remove(memberThis);

            Member memberThat = setMembers.iterator().next();
            Eventually.assertThat(invoking(this).veryThisThatCardinality(serviceReal, cPartitions, memberThis, memberThat), is(true));
            Eventually.assertThat(invoking(this).isDistributionOrTransferInProgress(serviceReal), is(false));

            int iCount = 0;
            for (int i = 0; i < aoListeners.length; i++)
                {
                TestPartitionListenerWithInitParams listener = (TestPartitionListenerWithInitParams)aoListeners[i];
                iCount++;
                assertTrue(listener.m_fExecuted);
                assertTrue(listener.m_fPartitionTransmitted);
                assertEquals("service-name parameter resolution", serviceSafe.getServiceName(), listener.m_sServiceName);
                assertTrue("classloader parameter resolution failed: listener=" + listener,
                        listener.m_classLoader instanceof ClassLoader && listener.m_classLoader != null);
                }

            assertTrue(iCount == 1);
            }
        catch (Exception e)
            {
            e.printStackTrace();
            fail("Unexpected failure..");
            }
        finally
            {
            stopCacheServer("secondary");
            }
        }

    public boolean veryThisThatCardinality(PartitionedCache serviceReal, int cPartitions, Member memberThis, Member memberThat)
        {
        int cPartsThis = serviceReal.getOwnedPartitions(memberThis).cardinality();
        int cPartsThat = serviceReal.getOwnedPartitions(memberThat).cardinality();

        return cPartsThis + cPartsThat == cPartitions && Math.abs(cPartsThis - cPartsThat) == 1;
        }

    public boolean isDistributionOrTransferInProgress(PartitionedCache serviceReal)
        {
        return serviceReal.isTransferInProgress() || serviceReal.isDistributionInProgress();
        }

    /**
    * Test that we can programmatically register listeners. Part of COH-4081
    */
    @Test
    public void testDoublePartitionListener()
        {
        NamedCache cache = getNamedCache("program-listener");

        CacheService     service     = cache.getCacheService();
        SafeService      serviceSafe = (SafeService) service;
        PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();
        try
            {
            //Programmatically register the PartitionListeners
            serviceReal.addPartitionListener(new TestPartitionListener());
            serviceReal.addPartitionListener(new TestPartitionListener());

            Listeners       listeners   = serviceReal.getPartitionListeners();
            EventListener[] aoListeners = listeners.listeners();
            assertTrue(aoListeners.length == 2);

            startCacheServer("secondary", "partition", null, PROPS_SEONE);

            Eventually.assertThat(invoking(serviceReal).getOwnershipEnabledMembers().size(), is(2));

            Member      memberThis  = serviceReal.getCluster().getLocalMember();
            int         cPartitions = serviceReal.getPartitionCount();
            Set<Member> setMembers  = serviceReal.getOwnershipEnabledMembers();

            setMembers.remove(memberThis);

            Member memberThat = setMembers.iterator().next();
            Eventually.assertThat(invoking(this).veryThisThatCardinality(serviceReal, cPartitions, memberThis, memberThat), is(true));
            Eventually.assertThat(invoking(this).isDistributionOrTransferInProgress(serviceReal), is(false));

            int iCount = 0;
            for (int i = 0; i < aoListeners.length; i++)
                {
                iCount++;
                assertTrue(((TestPartitionListener) aoListeners[i]).executed);
                assertTrue(((TestPartitionListener) aoListeners[i]).partitionTransmitted);
                }

            assertTrue(iCount == 2);
            }
        catch (Exception e)
            {
            e.printStackTrace();
            fail("Unexpected failure..");
            }
        finally
            {
            stopCacheServer("secondary");
            }
        }

    /**
    * Test to add Partition Listener to a storage disabled node
    */
    @Test
    public void testRegisterPartitionListenerWithStorageDisabledNode()
        {
        System.setProperty("coherence.distributed.localstorage", "false");
        try
            {
            getNamedCache("COH10397");
            }
        catch (Exception e)
            {
            fail("Failed to register Partition Listener on a Storage disabled node");
            }
        finally
            {
            System.clearProperty("coherence.distributed.localstorage");
            }
        }

    /**
    * Tests for PartitionAwareKey functionality
    */
    @Test
    public void testPartitionAwareKey()
        {
        doPartitionAwareKeyTest("dist-foo");
        }


    /**
    * Test helper for PartitionAwareKey.
    *
    * @param sCacheName  the cache name to test
    */
    private void doPartitionAwareKeyTest(String sCacheName)
        {
        NamedCache     cache     = getNamedCache(sCacheName);
        EntryProcessor processor = new TestPAProcessor();

        try
            {
            for (int i = 0; i < 257; i++)
                {
                int nPartition = (Integer) cache.invoke(
                            SimplePartitionKey.getPartitionKey(i), processor);

                assertEquals("SimplePartitionKey was not in the correct partition",
                             i, nPartition);
                }
            }
        finally
            {
            cache.destroy();
            }
        }

    public static class TestPAProcessor
        extends AbstractProcessor
        {
        public TestPAProcessor()
            {
            }

        public Object process(InvocableMap.Entry entry)
            {
            BinaryEntry binEntry = (BinaryEntry) entry;
            return binEntry.getContext().getKeyPartition(binEntry.getBinaryKey());
            }
        }

    /**
    * Regression test for COH-5108.
    * Test that custom backing-maps that do not implement ObservableMap
    * will not cause missing/corrupted backups and events
    */
    @Test
    public void testNonObservableBackingMap()
        {
        final int CACHE_SIZE = 200000;
        NamedCache cache = getNamedCache("custom-backing-map");
        try
            {
            startCacheServer("secondary", "partition");
            startCacheServer("tertiary", "partition");

            SafeService      serviceSafe = (SafeService) cache.getCacheService();
            PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

            // prevent orphaned partitions by waiting for partition assignment to complete
            // across all servers.
            // (this client has local storage enabled along with two servers started by test)
            Eventually.assertDeferred(() -> serviceReal.getOwnershipEnabledMembers().size(), is(3));
            waitForBalanced(cache.getCacheService());

            Map mapTemp = new HashMap();
            for (int i = 1; i <= CACHE_SIZE; i++)
                {
                mapTemp.put(i, i);
                }
            cache.putAll(mapTemp);

            assertEquals(CACHE_SIZE, cache.size());

            stopCacheServer("secondary");
            assertEquals(CACHE_SIZE, cache.size());

            // prevent orphan partitions by waiting until not endangered
            Eventually.assertDeferred(() -> serviceReal.getOwnershipEnabledMembers().size(), is(2));
            waitForBalanced(cache.getCacheService());

            stopCacheServer("tertiary");
            assertEquals(CACHE_SIZE, cache.size());
            }
       finally
            {
            stopCacheServer("secondary");
            stopCacheServer("tertiary");
            }
        }

    /**
    * Regression test for COH-10328.
    * Test that custom backing-maps that are partition-aware
    * are wrapped into a ObservableSplittingBackingMap
    */
    @Test
    public void testCoh10328()
        {
        NamedCache cache      = getNamedCache("COH10328");
        Map        backingMap = cache.getCacheService().getBackingMapManager().getContext()
                                      .getBackingMap(cache.getCacheName());
        String     message    = "Expected : " + ObservableSplittingBackingMap.class + " Got :" + backingMap.getClass();

        assertTrue(message, backingMap instanceof ObservableSplittingBackingMap);
        }

    // ----- inner class: CustomPABM  --------------------------------------

    public static class CustomPABM extends PartitionSplittingBackingMap
        {
        public CustomPABM(String sCacheName, BackingMapManagerContext ctx)
            {
            super(ctx.getManager(), sCacheName);
            }
        }

    // ----- inner class: Coh3974QuorumPolicy -------------------------------

    /**
    * Quorum policy for COH-3974 regression test that has the effect of delaying
    * a client thread.
    */
    public static class Coh3974QuorumPolicy
            implements ActionPolicy
        {
        /**
        * Set the threads.
        */
        protected static void setThreads(Thread thd1, Thread thd2)
            {
            s_thd1 = thd1;
            s_thd2 = thd2;
            }

        /**
        * {@inheritDoc}
        */
        public void init(Service service)
            {
            }

        /**
        * Return true iff the specified cache service contains a cache with the
        * specified name.
        *
        * @param service     the cache service
        * @param sCacheName  the cache name
        *
        * @return true iff the service contains the specified cache name
        */
        public boolean existsCache(CacheService service, String sCacheName)
            {
            for (Enumeration enumCache = service.getCacheNames();
                 enumCache.hasMoreElements(); )
                {
                if (Base.equals(sCacheName, enumCache.nextElement()))
                    {
                    return true;
                    }
                }
            return false;
            }

        /**
        * {@inheritDoc}
        */
        public boolean isAllowed(Service service, Action action)
            {
            Thread thdCurrent = Thread.currentThread();
            if (thdCurrent == s_thd1)
                {
                try
                    {
                    s_thd2.start();
                    s_thd2.join();

                    Eventually.assertThat(invoking(this).existsCache((CacheService) service, "Coh3974"), is(false));
                    }

                catch (InterruptedException e)
                    {
                    thdCurrent.interrupt();
                    throw Base.ensureRuntimeException(e);
                    }
                }
            return true;
            }

        // ----- data members ---------------------------------------------

        private static Thread s_thd1;
        private static Thread s_thd2;
        }


    // ----- inner class TestPartitionListener ------------------------------

    public static class TestPartitionListener
            implements PartitionListener
        {

        public TestPartitionListener()
            {
            executed = false;
            partitionTransmitted = false;
            }

        public void onPartitionEvent(PartitionEvent evt)
            {
            executed = true;

            if (evt.getId() == PartitionEvent.PARTITION_TRANSMIT_COMMIT)
                {
                partitionTransmitted = true;
                }
            }

        // ----- data members -----------------------------------------------

        public volatile boolean executed;
        public volatile boolean partitionTransmitted;
        }

    // ----- inner class TestPartitionListenerWithInitParams ----------------

    public static class TestPartitionListenerWithInitParams
            implements PartitionListener
        {

        public TestPartitionListenerWithInitParams(String serviceName, ClassLoader classloader)
            {
            m_fExecuted = false;
            m_fPartitionTransmitted = false;
            m_classLoader = classloader;
            m_sServiceName = serviceName;
            }

        public void onPartitionEvent(PartitionEvent evt)
            {
            m_fExecuted = true;

            if (evt.getId() == PartitionEvent.PARTITION_TRANSMIT_COMMIT)
                {
                m_fPartitionTransmitted = true;
                }
            }

        // ----- Object methods ---------------------------------------------

        public String toString()
            {
            return this.getClass().getSimpleName() + " service-name=" + m_sServiceName +
                    " classloader=" + m_classLoader;
            }

        // ----- data members -----------------------------------------------

        public volatile boolean     m_fExecuted;
        public volatile boolean     m_fPartitionTransmitted;
        public volatile String      m_sServiceName;
        public volatile ClassLoader m_classLoader;
        }

    // ----- inner class KeyReferenceCheckProcessor -------------------------

    public static class KeyReferenceCheckProcessor
            extends AbstractProcessor
        {
        @Override
        public Object process(InvocableMap.Entry entry)
            {
            BinaryEntry          binEntry    = (BinaryEntry) entry;
            ConfigurableCacheMap mapResource = (ConfigurableCacheMap) binEntry.getBackingMap();
            Binary               binKeyThis  = (Binary) mapResource.getCacheEntry(binEntry.getBinaryKey()).getKey();
            Binary               binKeyThat;

            try
                {
                Object      oStorage     = ClassHelper.invoke(binEntry, "getStorage", null);
                SafeHashMap mapListeners = (SafeHashMap) ClassHelper.invoke(oStorage, "getKeyListenerMap", null);

                binKeyThat = (Binary) ((Map.Entry) mapListeners.getEntry(binKeyThis)).getKey();
                }
            catch (Throwable t)
                {
                throw Base.ensureRuntimeException(t);
                }

            if (binKeyThis != binKeyThat)
                {
                throw new AssertionError("Expected keys to be interned");
                }

            return Boolean.TRUE;
            }
        }
    }