/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package processor;


import com.oracle.coherence.common.base.Converter;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.io.ExternalizableLite;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.partition.KeyAssociator;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.InvocableMap.EntryProcessor;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.WrapperException;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.processor.AbstractProcessor;

import common.AbstractRollingRestartTest;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;


/**
* Test for "lite-transactional" functionality of EntryProcessors.
*/
public class LiteTxnProcessorTests
        extends AbstractRollingRestartTest
    {
    // ----- constructors --------------------------------------------------

    /**
    * Default constructor.
    */
    public LiteTxnProcessorTests()
        {
        super(s_sCacheConfig);
        }


    // ----- lifecycle -----------------------------------------------------

    @After
    public void cleanup()
        {
        stopAllApplications();
        CacheFactory.shutdown();
        }

    // ----- accessors -----------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String getCacheConfigPath()
        {
        return s_sCacheConfig;
        }

    /**
    * {@inheritDoc}
    */
    public String getBuildPath()
        {
        return s_sBuild;
        }

    /**
    * {@inheritDoc}
    */
    public String getProjectName()
        {
        return s_sProject;
        }

    /**
     * Return the prefix to use for the server names.
     *
     * @return the server name prefix
     */
    public String getServerPrefix()
        {
        return "LiteTxn";
        }


    // ----- test methods -------------------------------------------------

    /**
    * Test that EntryProcessor changes are made atomically.
    */
    @Test
    public void testAtomicEntryProcessor()
            throws InterruptedException
        {
        final NamedCache   cache1   = getNamedCache("test-cache1");
        final NamedCache   cache2   = getNamedCache("test-cache2");
        final NamedCache   cache3   = getNamedCache("test-cache3");
        final int          cKeys    = 200;
        final int          cServers = 4;

        MemberHandler memberHandler = new MemberHandler(
        CacheFactory.ensureCluster(), getServerPrefix() + "-Atomic",
                /*fExternalKill*/false, /*fGraceful*/false);

        try
            {
            // setup, start the initial cache servers
            for (int i = 0; i < cServers; i++)
                {
                memberHandler.addServer();
                }

            final DistributedCacheService service = (DistributedCacheService) cache1.getCacheService();
            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(cServers));

            // prime the cache
            for (int i = 0; i < cKeys; i++)
                {
                String sKey = getKey(i);
                cache1.put(sKey, Integer.valueOf(0));
                cache2.put(sKey, Integer.valueOf(0));
                cache3.put(sKey, Integer.valueOf(0));
                }

            // start the client (load) thread
            final boolean[] afExiting   = new boolean[1];
            final Object[]  aoException = new Object[1];
            Thread thdLoad = new Thread()
                {
                public void run()
                    {
                    EntryProcessor processor = new AtomicUpdateProcessor();
                    try
                        {
                        while (!afExiting[0])
                            {
                            for (int i = 0; i < cKeys; i++)
                                {
                                cache1.invoke(getKey(i), processor);
                                }
                            Base.sleep(10);
                            }
                        }
                    catch (Exception e)
                        {
                        aoException[0] = e;
                        throw Base.ensureRuntimeException(e);
                        }
                    }
                };
            thdLoad.start();

            // perform the rolling restart
            doRollingRestart(memberHandler, 25,
                new Runnable()
                    {
                    public void run()
                        {
                        validateCaches(cache1, cKeys);
                        waitForNodeSafe(service);
                        validateCaches(cache1, cKeys);
                        }
                    });

            afExiting[0] = true;
            thdLoad.join();

            validateCaches(cache1, cKeys);
            assertNull(aoException[0]);
            }
        finally
            {
            memberHandler.dispose();
            Cluster cluster = CacheFactory.getCluster();
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
            }
        }

    /**
    * Test that deadlock on re-entrant lock acquisition is detected properly.
    */
    @Test
    public void testDeadlockDetectionSame()
        {
        doDeadlockDetectionTest("same", "test-associator1", "test-associator1", Integer.valueOf(2), Integer.valueOf(102));
        }

    @Test
    public void testDeadlockDetectionDiff1()
        {
        doDeadlockDetectionTest("diff1", "test-associator1", "test-associator2", Integer.valueOf(2), Integer.valueOf(102));
        }

    @Test
    public void testDeadlockDetectionDiff2()
        {
        doDeadlockDetectionTest("diff2", "test-associator1", "test-associator2", Integer.valueOf(2), Integer.valueOf(2));
        }

    @Test
    public void testDeadlockDetectionDuringInvokeAll()
        {
        doDeadlockDetectionTestDuringInvokeAll("invokeAll", "test-cache1", "test-cache2", "key");
        }
    /**
    * Helper method for deadlock detection tests.
    */
    protected void doDeadlockDetectionTest(String sTest, final String sCache1,
            final String sCache2, final Object oKey1, final Object oKey2)
        {
        String sServer = getServerPrefix() + "-Deadlock-" + sTest;

        // make sure we start the invocation service
        Properties props = new Properties();
        props.setProperty("tangosol.coherence.invocation.autostart", "true");
        props.setProperty("tangosol.coherence.log.level", "9");

        CoherenceClusterMember clusterMember = startCacheServer(sServer, s_sProject, s_sCacheConfig, props);

        Cluster cluster = CacheFactory.getCluster();
        Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(2));

        Eventually.assertThat(invoking(clusterMember).isServiceRunning("PartitionedCacheAssoc"), is(true));
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("InvocationService"), is(true));

        final InvocationService service  = (InvocationService) getFactory().ensureService("InvocationService");
        final Member            member   = findCacheServer(sServer);
        final Object[]          aoResult = new Object[2];

        Thread t1;
        Thread t2;

        // test same-cache deadlock
        t1 = new Thread()
            {
            public void run()
                {
                aoResult[0] = service.query(
                        new DeadlockInvocable(
                                sCache1, oKey1,
                                new DeadlockProcessor(sCache2, oKey2), 2),
                        Collections.singleton(member)).get(member);
                }
            };
        t2 = new Thread()
            {
            public void run()
                {
                aoResult[1] = service.query(
                        new DeadlockInvocable(
                                 sCache2, oKey2,
                                 new DeadlockProcessor(sCache1, oKey1), 1),
                        Collections.singleton(member)).get(member);
                }
            };

        t1.start();
        t2.start();

        NamedCache cache1 = null;
        NamedCache cache2 = null;
        try
            {
            t1.join();
            t2.join();

            cache1 = getFactory().ensureCache(sCache1, null);
            cache2 = getFactory().ensureCache(sCache2, null);

            Integer NResult  = (Integer) aoResult[0];
            Integer NResult1 = (Integer) aoResult[1];
            assertFalse(String.format("Response from invocable was null {0:%s, 1:%s}", NResult, NResult1),
                        NResult == null || NResult1 == null);

            assertThat(NResult.intValue() + NResult1.intValue(), is(3));

            assertThat((Integer) cache1.get(oKey1), anyOf(is(1), is(2)));
            assertThat((Integer) cache2.get(oKey2), anyOf(is(1), is(2)));
            }
        catch (InterruptedException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        finally
            {
            if (cache1 != null)
                {
                cache1.remove(oKey1);
                }
            if (cache2 != null)
                {
                cache2.remove(oKey2);
                }
            stopCacheServer(sServer);
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
            }
        }

    /**
     * Helper method for deadlock detection tests during invokeAll with Filter.
     */
    public void doDeadlockDetectionTestDuringInvokeAll(String sTest, final String sCache1, final String sCache2,
                                    final Object oKey)
        {
        String sServer = getServerPrefix() + "-Deadlock-" + sTest;

        startCacheServer(sServer, s_sProject, s_sCacheConfig, null);

        final NamedCache cache          = getFactory().ensureCache(sCache1, null);
        final NamedCache cacheSecondary = getFactory().ensureCache(sCache2, null);

        cache.put(oKey, new Integer(0));
        cacheSecondary.put(oKey, new Integer(0));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Runnable        runner1  = () -> cacheSecondary.invokeAll(AlwaysFilter.INSTANCE,
                                             new DeadlockProcessor(sCache1, oKey));
        Runnable        runner2  = () -> cache.invokeAll(AlwaysFilter.INSTANCE,
                                             new DeadlockProcessor(sCache2, oKey));

        executor.submit(runner1);
        executor.submit(runner2);
        executor.shutdown();
        try
            {
            executor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (Exception e)
                {
                e.printStackTrace();
                }
        assertThat((Integer) cache.get(oKey), anyOf(is(1), is(2)));
        assertThat((Integer) cacheSecondary.get(oKey), anyOf(is(1), is(2)));
        }

    /**
    * Test an EntryProcessor that increments the value of a key in the
    * "parent" cache ("test-associator1"), and also the value of the
    * associated key (shared) in the "child" cache ("test-associator2").
    */
    @Test
    public void testChildCacheMutate()
        {
        doTestChildCacheMutate("testChild", 3, 0);
        }

    @Test
    public void testChildCacheMutateRolling()
        {
        doTestChildCacheMutate("testChildRolling", 3, 10);
        }

    /**
    * Helper for testChildCacheMutate tests.
    */
    protected void doTestChildCacheMutate(String sTest, int cServers, int cRollingRestart)
        {
        boolean       fRollingRestart = cRollingRestart > 0;
        Cluster       cluster         = CacheFactory.getCluster();
        MemberHandler memberHandler   = new MemberHandler(
                CacheFactory.ensureCluster(), getServerPrefix() + "-" + sTest,
                /*fExternalKill*/true, /*fGraceful*/false);

        try
            {
            for (int i = 0; i < cServers; i++)
                {
                memberHandler.addServer();
                }

            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(cServers + 1));

            int              cThreads = 4;
            final int        cKeys    = 10000;
            Thread[]         aThread  = new Thread[cThreads];
            final NamedCache cache1   = getNamedCache("test-associator1");
            final NamedCache cache2   = getNamedCache("test-associator2");

            for (int i = 0; i < cThreads; i++)
                {
                aThread[i] = new Thread()
                    {
                    public void run()
                        {
                        int[] aiKeys = new int[cKeys];
                        for (int i = 0; i < 10000; i++)
                            {
                            aiKeys[i] = i;
                            }

                        aiKeys = Base.randomize(aiKeys);

                        EntryProcessor processor = new TestChildCacheMutateProcessor();
                        for (int i = 0; i < cKeys; i++)
                            {
                            cache1.invoke(aiKeys[i], processor);
                            }
                        }
                    };
                aThread[i].start();
                }

            if (fRollingRestart)
                {
                doRollingRestart(memberHandler, cRollingRestart, new WaitForNodeSafeRunnable(cache1.getCacheService()));
                }

            try
                {
                for (int i = 0; i < cThreads; i++)
                    {
                    aThread[i].join();
                    }
                }
            catch (InterruptedException e)
                {
                fail("test was interrupted: " + e);
                }

            // verify contents...
            for (int i = 0; i < cKeys; i++)
                {
                assertEquals("Key " + i, cThreads, cache1.get(i));
                assertEquals("Key " + i, (cKeys / 100) * cThreads, cache2.get(i % 100));
                }
            }
        finally
            {
            memberHandler.dispose();
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
            }
        }

    /**
    * Regression test for COH-4090
    */
    @Test
    public void testCoh4090()
        {
        String  sServer = getServerPrefix() + "-Coh4090";
        Cluster cluster = CacheFactory.getCluster();

        CoherenceClusterMember member = startCacheServer(sServer, s_sProject, s_sCacheConfig);
        Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(2));
        Eventually.assertThat(invoking(member).isServiceRunning("PartitionedCache"), is(true));

        NamedCache cache1 = getNamedCache("test-cache1");
        NamedCache cache2 = getNamedCache("test-cache2");

        cache1.clear();
        cache2.clear();
        assertEquals(NullImplementation.getMap(), cache1);
        assertEquals(NullImplementation.getMap(), cache2);

        // run the processor; should return null if the test passes,
        // or an exception if the test fails.
        try
            {
            Object oResult = cache1.invoke("foo", new Coh4090Processor());
            if (oResult instanceof Throwable)
                {
                fail(Base.getStackTrace((Throwable) oResult));
                }
            }
        finally
            {
            stopCacheServer(sServer);
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
            }
        }

    /**
    * Regression test for COH-4435
    */
    @Test
    public void testCoh4435()
        {
        String  sServer = getServerPrefix() + "-Coh4435";
        Cluster cluster = CacheFactory.getCluster();

        CoherenceClusterMember member = startCacheServer(sServer, s_sProject, s_sCacheConfig);
        Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(2));
        Eventually.assertThat(invoking(member).isServiceRunning("PartitionedCache"), is(true));

        NamedCache cache1 = getNamedCache("test-cache1");
        NamedCache cache2 = getNamedCache("test-cache2");

        cache1.clear();
        cache2.clear();
        assertEquals(NullImplementation.getMap(), cache1);
        assertEquals(NullImplementation.getMap(), cache2);

        HashSet setKeys = new HashSet();
        for (int i = 0; i < 10; i++)
            {
            setKeys.add(i);
            }

        try
            {
            // run the processor; should return null if the test passes,
            // or an exception if the test fails.
            try
                {
                // single invoke
                Object oResult = cache1.invoke(1, new Coh4435Processor(false));
                fail("Expected failure, but entry processor returned " + oResult);
                }
            catch (Exception e)
                {
                // all changes should have rolled-back
                assertEquals(NullImplementation.getMap(), cache1);
                assertEquals(NullImplementation.getMap(), cache2);
                }

            try
                {
                // bulk-invoke, no partial commit
                Object oResult = cache1.invokeAll(setKeys, new Coh4435Processor(false));
                fail("Expected failure, but entry processor returned " + oResult);
                }
            catch (Exception e)
                {
                // all changes should have rolled-back
                assertEquals(NullImplementation.getMap(), cache1);
                assertEquals(NullImplementation.getMap(), cache2);
                }

            try
                {
                // bulk-invoke, partial commit of keys (0,1,2,3,4) to cache1
                Object oResult = cache1.invokeAll(setKeys, new Coh4435Processor(true));
                fail("Expected failure, but entry processor returned ");
                }
            catch (Exception e)
                {
                HashMap mapExpected = new HashMap();
                for (int i = 0; i < 5; i++)
                    {
                    mapExpected.put(i, i);
                    }

                // changes to keys [0,4] on cache1 should be committed;
                // all other changes should have rolled-back
                assertEquals(mapExpected, cache1);
                assertEquals(NullImplementation.getMap(), cache2);
                }
            }
        finally
            {
            stopCacheServer(sServer);
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
            }
        }

    /**
    * Tests for COH-9931
    */
    @Test
    public void testCoh9931()
        {
        String sServer = getServerPrefix() + "-Coh9931";
        Cluster cluster = CacheFactory.getCluster();

        CoherenceClusterMember member = startCacheServer(sServer, s_sProject, s_sCacheConfig);
        Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(2));
        Eventually.assertThat(invoking(member).isServiceRunning("PartitionedCache"), is(true));
        Eventually.assertThat(invoking(member).isServiceRunning("PartitionedCacheAssoc"), is(true));

        NamedCache cache1 = getNamedCache("test-associator1");
        NamedCache cache2 = getNamedCache("test-associator2");

        cache1.clear();
        cache2.clear();

        HashSet setKeys = new HashSet();
        HashMap map     = new HashMap();

        for (int i = 0; i < 3; i++)
            {
            map.put(Integer.valueOf(i), Integer.valueOf(i));
            map.put(Integer.valueOf(i + 100), Integer.valueOf(i + 100));
            setKeys.add(Integer.valueOf(i));
            }

        cache1.putAll(map);
        cache2.putAll(map);

        try
            {
            cache1.invoke(Integer.valueOf(1), new Coh9931Processor());
            cache1.invokeAll(setKeys, new Coh9931Processor());
            }
        catch (Exception e)
            {
            fail(Base.getStackTrace(e));
            }
        finally
            {
            stopCacheServer(sServer);
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
            }
        }

    /**
     * Regression test for COH-14323
     */
    @Test
    public void testCoh14323()
        {
        String sServer = getServerPrefix() + "-Coh14323";
        Cluster cluster = CacheFactory.getCluster();

        CoherenceClusterMember member = startCacheServer(sServer, s_sProject, s_sCacheConfig);
        Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(2));
        Eventually.assertThat(invoking(member).isServiceRunning("PartitionedCache"), is(true));

        NamedCache cache = getNamedCache("test-cache1");
        cache.clear();
        try
            {
            cache.invoke(1, new Coh14323Processor());

            fail("Execution of Coh14323Processor should have failed");
            }
        catch (Throwable t)
            {
            // check that the cache service is not terminated
            assertTrue(member.isServiceRunning("PartitionedCache"));
            }
        finally
            {
            stopCacheServer(sServer);
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
            }
        }

    /**
     *  Test for COH-14323
     */
    @Test
    public void testCoh15249()
        {
        String sServer = getServerPrefix() + "-Coh15249";
        Cluster cluster = CacheFactory.getCluster();

        CoherenceClusterMember member = startCacheServer(sServer, s_sProject, s_sCacheConfig);
        Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(2));
        Eventually.assertThat(invoking(member).isServiceRunning("PartitionedCache"), is(true));
        NamedCache cache1 = getNamedCache("test-associator1");
        NamedCache cache2 = getNamedCache("test-associator2");
        try
            {
            cache1.invoke(101, new TestGetReadonlyEntryProcessor());
            }
        finally
            {
            stopCacheServer(sServer);
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
            }
        }


    // ----- inner class --------------------------------

    /**
     * An entry processor that throw non-serializable RuntimeException.
     */
    public static class Coh14323Processor
            extends AbstractProcessor
        {
        @Override
        public Object process(Entry entry)
            {
            throw new TestException();
            }
        }

    public static class TestException
            extends RuntimeException
        {
        // make this exception not serializable by adding
        // a non-serializable non-transient field
        private Object m_object = new Object();
        }

    /**
    * EntryProcessor for testCoh4090
    */
    public static class Coh4090Processor
            extends    AbstractProcessor
            implements ExternalizableLite
        {
        // ----- EntryProcessor methods -----------------------------------
        /**
        * {@inheritDoc}
        */
        public Object process(Entry entry)
            {
            BinaryEntry              binEntry = (BinaryEntry) entry;
            Binary                   binKey   = binEntry.getBinaryKey();
            BackingMapManagerContext ctx      = binEntry.getContext();

            Map         mapCache2 = ctx.getBackingMap("test-cache2");
            BinaryEntry binEntry2 = (BinaryEntry) ctx.getBackingMapContext("test-cache2").getBackingMapEntry(
             binKey);
            Thread      thd;
            try
                {
                // destroy test-cache1
                thd = new Thread()
                    {
                    public void run()
                        {
                        CacheFactory.getCache("test-cache1").destroy();
                        }
                    };
                thd.start();
                thd.join();

                // check that my backing-map is not null
                Eventually.assertThat(invoking(ctx).getBackingMap("test-cache1"), is(notNullValue()));
                Eventually.assertThat(invoking(ctx).getBackingMap("test-cache1"), is((Map) binEntry.getBackingMap()));

                // destroy test-cache2
                thd = new Thread()
                    {
                    public void run()
                        {
                        CacheFactory.getCache("test-cache2").destroy();
                        }
                    };
                thd.start();
                thd.join();

                // check that cache2 backing-map is still the same
                Eventually.assertThat(invoking(ctx).getBackingMap("test-cache2"), is(mapCache2));
                Eventually.assertThat(invoking(ctx).getBackingMapContext("test-cache2").getBackingMapEntry(binKey), is((InvocableMap.Entry) binEntry2));
                assertEquals(mapCache2, binEntry2.getBackingMap());

                return null;
                }
            catch (Throwable t)
                {
                return t;
                }
            }

        // ----- ExternalizableLite methods -------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(DataInput in) throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(DataOutput out) throws IOException
            {
            }
        }

    /**
     * EntryProcessor used by the testCoh15249
     */
    public static class TestGetReadonlyEntryProcessor
            extends AbstractProcessor
        {
        /**
         * {@inheritDoc}
         */
        public Object process(Entry entry)
            {
            BinaryEntry              binEntry = (BinaryEntry) entry;
            BackingMapManagerContext ctx      = binEntry.getContext();
            Integer                  IKey     = (Integer) binEntry.getKey();
            Integer                  IValue   = (Integer) binEntry.getValue();

            binEntry.setValue((IValue == null ? 0 : IValue.intValue()) + 1);

            PartitionedService service    = (PartitionedService) ctx.getCacheService();
            KeyAssociator      associator = service.getKeyAssociator();
            Integer            IKeyAssoc  = (Integer) associator.getAssociatedKey(IKey);

            // make sure IKeyAssoc is different from Ikey, so it is not yet enlisted
            assertTrue(IKeyAssoc != IKey);

            BackingMapContext ctxBackingMap = ctx.getBackingMapContext("test-associator2");
            Binary            binKeyAssoc   = (Binary) ctx.getKeyToInternalConverter().convert(IKeyAssoc);

            // should be null because binKeyAssoc is not enlisted and is not in the backing map
            assertNull(ctxBackingMap.getReadOnlyEntry(binKeyAssoc));

            // enlist binKeyAssoc in the transaction sandbox, but not in backingMap yet
            BinaryEntry binEntryAssoc = (BinaryEntry) ctxBackingMap.getBackingMapEntry(binKeyAssoc);
            assertTrue(binEntryAssoc != null);

            // should return the writable binEntryAssoc from sandbox
            assertTrue(ctxBackingMap.getReadOnlyEntry(binKeyAssoc) == binEntryAssoc);

            return null;
            }
        }

    // ----- inner class: Coh4435Processor --------------------------------
    /**
    * EntryProcessor for testCoh4435
    */
    public static class Coh4435Processor
            extends    AbstractProcessor
            implements ExternalizableLite
        {
        /**
        * Default constructor.
        */
        public Coh4435Processor()
            {
            }

        /**
        * Construct a Coh4435Processor.
        *
        * @param fPartialCommit  true iff the processor should partially-commit
        *                        the processAll() operation
        */
        public Coh4435Processor(boolean fPartialCommit)
            {
            m_fPartialCommit = fPartialCommit;
            }

        // ----- EntryProcessor methods -----------------------------------

        /**
        * {@inheritDoc}
        */
        public Map processAll(Set setEntries)
            {
            boolean fPartialCommit = m_fPartialCommit;
            for (Iterator iter = setEntries.iterator(); iter.hasNext(); )
                {
                BinaryEntry              binEntry1 = (BinaryEntry) iter.next();
                Binary                   binKey    = binEntry1.getBinaryKey();
                BackingMapManagerContext ctx       = binEntry1.getContext();
                BinaryEntry              binEntry2 = (BinaryEntry)
                    ctx.getBackingMapContext("test-cache2").getBackingMapEntry(binKey);

                binEntry1.updateBinaryValue(binKey);
                binEntry2.updateBinaryValue(binKey);

                int iKey = ((Integer) binEntry1.getKey()).intValue();
                if (iKey < 5 && fPartialCommit)
                    {
                    iter.remove();
                    }
                }

            throw new RuntimeException();
            }

        /**
        * {@inheritDoc}
        */
        public Object process(Entry entry)
            {
            BinaryEntry              binEntry1 = (BinaryEntry) entry;
            Binary                   binKey    = binEntry1.getBinaryKey();
            BackingMapManagerContext ctx       = binEntry1.getContext();
            BinaryEntry              binEntry2 = (BinaryEntry)
                ctx.getBackingMapContext("test-cache2").getBackingMapEntry(binKey);

            binEntry1.setValue(binKey);
            binEntry2.setValue(binKey);

            throw new RuntimeException();
            }

        // ----- ExternalizableLite methods -------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(DataInput in) throws IOException
            {
            m_fPartialCommit = in.readBoolean();
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(DataOutput out) throws IOException
            {
            out.writeBoolean(m_fPartialCommit);
            }

        // ----- data members ---------------------------------------------

        /**
        * Should this entry processor perform a partial-commit (for processAll)?
        */
        protected boolean m_fPartialCommit;
        }


    // ----- inner class: TestChildCacheMutateProcessor -------------------

    /**
    * EntryProcessor used by the testChildCacheMutate tests.
    */
    public static class TestChildCacheMutateProcessor
            extends AbstractProcessor
        {
        /**
        * {@inheritDoc}
        */
        public Object process(Entry entry)
            {
            BinaryEntry              binEntry = (BinaryEntry) entry;
            BackingMapManagerContext ctx      = binEntry.getContext();
            Integer                  IKey     = (Integer) binEntry.getKey();
            Integer                  IValue   = (Integer) binEntry.getValue();

            // increment the value
            binEntry.setValue((IValue == null ? 0 : IValue.intValue()) + 1);

            PartitionedService service       = (PartitionedService) ctx.getCacheService();
            KeyAssociator      associator    = service.getKeyAssociator();
            Integer            IKeyAssoc     = (Integer) associator.getAssociatedKey(IKey);
            Converter          converter     = ctx.getKeyToInternalConverter();
            Binary             binKeyAssoc   = (Binary) converter.convert(IKeyAssoc);
            BinaryEntry        binEntryAssoc = (BinaryEntry)
                ctx.getBackingMapContext("test-associator2").getBackingMapEntry(
                    binKeyAssoc);

            // increment the associated value
            Integer IValueAssoc = (Integer) binEntryAssoc.getValue();
            binEntryAssoc.setValue((IValueAssoc == null ? 0 : IValueAssoc.intValue()) + 1);

            return null;
            }
        }


    // ----- inner class: Associator --------------------------------------

    /**
    * KeyAssociator implementation used by the PartitionedCacheAssoc service.
    * (See lite-txn-cache-config.xml).
    */
    public static class Associator
            implements KeyAssociator
        {
        /**
        * {@inheritDoc}
        */
        public void init(PartitionedService service)
            {
            }

        /**
        * {@inheritDoc}
        */
        public Object getAssociatedKey(Object oKey)
            {
            if (oKey instanceof Integer)
                {
                int nKey = ((Integer) oKey).intValue();
                return Integer.valueOf(nKey % 100);
                }

            return oKey;
            }
        }

    /**
    * Increment the value of the specified integer, encoded in Binary format
    * according to the specified converters.
    *
    * @param binValue  the integer value to increment
    * @param convUp    the Binary->Integer converter
    * @param convDown  the Integer->Binary converter
    *
    * @return a Binary representing the incremented value
    */
    protected static Binary incrementBinaryInteger(
            Binary binValue, Converter convUp, Converter convDown)
        {
        Integer IValue = (Integer) convUp.convert(binValue);
        return (Binary) convDown.convert(Integer.valueOf(IValue.intValue() + 1));
        }

    /**
    * Run the validation on the specified cache.
    *
    * @param cache  the cache to run validation on
    */
    protected static void validateCaches(NamedCache cache, int cKeys)
        {
        EntryProcessor processor = new ValidateProcessor();

        for (int i = 0; i < cKeys; i++)
            {
            assertEquals(Boolean.TRUE, cache.invoke(getKey(i), processor));
            }
        }


    // ----- inner class: DeadlockProcessor -------------------------------

    /**
    * EntryProcessor used for deadlock detection tests.
    */
    public static class DeadlockProcessor
            extends    AbstractProcessor
            implements ExternalizableLite
        {
        public DeadlockProcessor()
            {
            }

        public DeadlockProcessor(String sCache, Object oKey)
            {
            m_sCache    = sCache;
            m_oKey      = oKey;
            }

        // ----- EntryProcessor methods -----------------------------------

        /**
        * {@inheritDoc}
        */
        public Object process(Entry entry)
            {
            f_cInvocations.decrementAndGet();

            Eventually.assertThat(invoking(this).dereference(f_cInvocations), lessThanOrEqualTo(0));

            BinaryEntry              binEntry  = (BinaryEntry) entry;
            BackingMapManagerContext context   = binEntry.getContext();
            Converter                converter = context.getKeyToInternalConverter();

            BinaryEntry binEntryOther = (BinaryEntry)
                    context.getBackingMapContext(m_sCache).getBackingMapEntry(converter.convert(m_oKey));

            Integer NValue      = (Integer) binEntry     .getValue();
            Integer NValueOther = (Integer) binEntryOther.getValue();

            NValue      = NValue      == null ? Integer.valueOf(1) : Integer.valueOf(NValue.intValue() + 1);
            NValueOther = NValueOther == null ? Integer.valueOf(1) : Integer.valueOf(NValueOther.intValue() + 1);

            binEntry     .setValue(NValue);
            binEntryOther.setValue(NValueOther);

            return NValue;
            }

        public int dereference(AtomicInteger ref)
            {
            return ref.get();
            }

        // ----- ExternalizableLite methods -------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(DataInput in) throws IOException
            {
            m_sCache = in.readUTF();
            m_oKey   = ExternalizableHelper.readObject(in);
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(DataOutput out) throws IOException
            {
            out.writeUTF(m_sCache);
            ExternalizableHelper.writeObject(out, m_oKey);
            }

        // ----- constants ------------------------------------------------

        protected static final AtomicInteger f_cInvocations = new AtomicInteger(2);

        // ----- data members ---------------------------------------------

        protected String m_sCache;
        protected Object m_oKey;
        }


    // ----- inner class: DeadlockInvocable -------------------------------

    /**
    * Invocable used for deadlock detection tests.
    */
    public static class DeadlockInvocable
            extends AbstractInvocable
            implements ExternalizableLite
        {
        public DeadlockInvocable()
            {
            }

        public DeadlockInvocable(
                String sCache, Object oKey, EntryProcessor processor, int nStep)
            {
            m_sCache    = sCache;
            m_oKey      = oKey;
            m_nStep     = nStep;
            m_processor = processor;
            }

        // ----- Invocable methods ----------------------------------------

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            Eventually.assertThat(invoking(this).dereference(DeadlockProcessor.f_cInvocations), is(m_nStep));

            try
                {
                Object oResult = CacheFactory.getCache(m_sCache).invoke(m_oKey, m_processor);
                setResult(oResult);
                }
            catch (WrapperException e)
                {
                Base.log("CAUGHT: " + e +":"+ Base.getStackTrace(e));
                setResult(Base.getOriginalException(e));
                }
            }

        public int dereference(AtomicInteger ref)
            {
            return ref.get();
            }

        // ----- ExternalizableLite methods -------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(DataInput in) throws IOException
            {
            m_sCache    = in.readUTF();
            m_oKey      = ExternalizableHelper.readObject(in);
            m_nStep     = in.readInt();
            m_processor = (EntryProcessor) ExternalizableHelper.readObject(in);
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(DataOutput out) throws IOException
            {
            out.writeUTF(m_sCache);
            ExternalizableHelper.writeObject(out, m_oKey);
            out.writeInt(m_nStep);
            ExternalizableHelper.writeObject(out, m_processor);
            }

        // ----- data members ---------------------------------------------

        protected String         m_sCache;
        protected Object         m_oKey;
        protected int            m_nStep;
        protected EntryProcessor m_processor;
        }


    // ----- inner class: AtomicUpdateProcessor ---------------------------

    /**
    * The updating processor to test the atomicity of.  This processor is
    * sensitive to the remote-kill signal and may choose to terminate abruptly.
    *
    * Used by testAtomicEntryProcessor tests.
    */
    protected static class AtomicUpdateProcessor
            extends AbstractProcessor
        {
        /**
        * {@inheritDoc}
        */
        public Object process(Entry entry)
            {
            BinaryEntry              binEntry = (BinaryEntry) entry;
            BackingMapManagerContext context  = binEntry.getContext();
            Map                      map2     = context.getBackingMap("test-cache2");
            Map                      map3     = context.getBackingMap("test-cache3");
            Binary                   binKey   = binEntry.getBinaryKey();
            Binary                   binValue = binEntry.getBinaryValue();
            Converter                convUp   = context.getValueFromInternalConverter();
            Converter                convDown = context.getValueToInternalConverter();
            DistributedCacheService  service   = (DistributedCacheService) context.getCacheService();

            // for debugging
            if (binValue == null)
                {
                Base.out("Running AtomicUpdateProcessor on key " + convUp.convert(binKey) + " at partition "
                        + context.getKeyPartition(binKey) + ", value " + binValue);

                Map map1 = binEntry.getBackingMap();
                Base.out("BinaryValue is null!  OriginalBinaryValue: " + binEntry.getOriginalBinaryValue() +
                        " value from backing map1:  " + map1.get(binKey) + " @ " + map1.getClass().getSimpleName() +
                        " size = " + map1.size() +
                        " map2: " + map2.get(binKey) + " size=" + map2.size() +
                        " map3: " + map3.get(binKey) + " size=" + map3.size());
                }
            binEntry.updateBinaryValue(incrementBinaryInteger(binValue, convUp, convDown));
            map2.put(binKey, incrementBinaryInteger((Binary) map2.get(binKey), convUp, convDown));
            map3.put(binKey, incrementBinaryInteger((Binary) map3.get(binKey), convUp, convDown));
            if (s_remoteDefault.isRemoteKill())
                {
                Base.err("Remote kill detected; halting the JVM");
                Runtime.getRuntime().halt(-1);
                }

            return null;
            }
        }


    // ----- inner class: ValidateProcessor -------------------------------

    /**
    * ValidateProcessor is used to (remotely) validate the contents of the
    * caches' backing-maps.
    *
    * Used by testAtomicEntryProcessor tests.
    */
    protected static class ValidateProcessor
            extends AbstractProcessor
        {
        /**
        * {@inheritDoc}
        */
        public Object process(Entry entry)
            {
            BinaryEntry              binEntry  = (BinaryEntry) entry;
            BackingMapManagerContext context   = binEntry.getContext();
            Map                      map2      = context.getBackingMap("test-cache2");
            Map                      map3      = context.getBackingMap("test-cache3");
            Binary                   binKey    = binEntry.getBinaryKey();
            Binary                   binValue  = undecorateExpiry(binEntry.getBinaryValue());
            Binary                   binValue2 = undecorateExpiry((Binary) map2.get(binKey));
            Binary                   binValue3 = undecorateExpiry((Binary) map3.get(binKey));

            if (!Base.equals(binValue, binValue2) ||
                !Base.equals(binValue, binValue3))
                {
                Converter convUp = context.getValueFromInternalConverter();

                Base.err("Non-atomic change found on key: " + binKey);
                Base.err("  test-cache1: " + convUp.convert(binValue));
                Base.err("  test-cache2: " + convUp.convert(binValue2));
                Base.err("  test-cache3: " + convUp.convert(binValue3));
                return Boolean.FALSE;
                }
            return Boolean.TRUE;
            }
        }


    // ----- inner class: Coh9931Processor --------------------------------
    /**
    * EntryProcessor for testCoh9931
    */
    public static class Coh9931Processor
            extends    AbstractProcessor
            implements ExternalizableLite
        {
        /**
        * Default constructor.
        */
        public Coh9931Processor()
            {
            }

        // ----- EntryProcessor methods -----------------------------------

        /**
        * {@inheritDoc}
        */
        public Map processAll(Set setEntries)
            {
            for (Iterator iter = setEntries.iterator(); iter.hasNext(); )
                {
                process((BinaryEntry) iter.next());
                }
            return null;
            }

        /**
        * {@inheritDoc}
        */
        public Object process(Entry entry)
            {
            BinaryEntry binEntry = (BinaryEntry) entry;
            //test getReadOnlyEntry from same cache
            testReadOnlyEntry(binEntry, null);

            //test getReadOnlyEntry from different cache
            testReadOnlyEntry(binEntry, "test-associator2");
            return null;
            }

        protected void testReadOnlyEntry(BinaryEntry binEntry, String sCache)
            {
            Binary                   binKey      = binEntry.getBinaryKey();
            BackingMapManagerContext ctx         = binEntry.getContext();
            BackingMapContext        ctxThis     = sCache == null ? binEntry.getBackingMapContext()
                                                       : ctx.getBackingMapContext(sCache);
            Converter                converterFr = ctx.getKeyFromInternalConverter();
            Converter                converterTo = ctx.getKeyToInternalConverter();

            //test none-exist entry, should return null
            Integer     oKey         = (Integer) converterFr.convert(binKey) + 200;
            BinaryEntry binTestEntry = (BinaryEntry) ctxThis.getReadOnlyEntry(converterTo.convert(oKey));
            assertNull(binTestEntry);

            //test existing entry from enlisted partition
            oKey         = (Integer) converterFr.convert(binKey) + 100;
            binTestEntry = (BinaryEntry) ctxThis.getReadOnlyEntry(converterTo.convert(oKey));
            assertEquals(binTestEntry.getValue(), oKey);

            //test mutating readOnly entry
            try
                {
                binTestEntry.updateBinaryValue(binKey);
                fail("Expected failure, but mutating read-only entry is success!");
                }
            catch (UnsupportedOperationException e)
                {
                Base.log("CAUGHT expected exception: " + e);
                }

            //test entry from partition that is not enlisted
            oKey = (Integer) converterFr.convert(binKey) + 101;
            try
                {
                binTestEntry = (BinaryEntry) ctxThis.getReadOnlyEntry(converterTo.convert(oKey));
                fail("Expected failure, but get read-only entry from none-enlisted partition return!");
                }
            catch (IllegalStateException e)
                {
                Base.log("CAUGHT expected exception: " + e);
                }
            }

        // ----- ExternalizableLite methods -------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(DataInput in) throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(DataOutput out) throws IOException
            {
            }
        }

    /**
    * Return a key for the specified index.
    *
    * @param i  the index of the key
    *
    * @return a key
    */
    public static String getKey(int i)
        {
        return "key-" + i;
        }

    /**
    * Remove the expiry time from the passed Binary. The Binary transferred
    * to primary from backup can be encoded with the expiry time.
    */
    private static Binary undecorateExpiry(Binary binValue)
        {
        return ExternalizableHelper.asBinary(
                   ExternalizableHelper.encodeExpiry(binValue, 0L));
        }


    // ----- data members and constants -----------------------------------

    /**
    * The path to the cache configuration.
    */
    public final static String s_sCacheConfig = "lite-txn-cache-config.xml";

    /**
    * The path to the Ant build script.
    */
    public final static String s_sBuild       = "build.xml";

    /**
    * The project name.
    */
    public final static String s_sProject     = "processor";
    }