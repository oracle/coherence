/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package near;

import com.oracle.bedrock.options.Timeout;

import com.oracle.bedrock.runtime.java.options.JvmOptions;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.testing.junit.ThreadDumpOnTimeoutRule;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.InvocationService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheStatistics;
import com.tangosol.net.cache.CachingMap;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.NearCache;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.EntryEvents;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.partition.cache.EntryEvent;

import com.tangosol.net.management.MBeanHelper;

import com.tangosol.util.Base;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.SegmentedConcurrentMap;

import com.tangosol.util.processor.NumberIncrementor;
import com.tangosol.util.processor.PropertyManipulator;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.TestCoh15021;

import data.Person;

import java.io.Serializable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.hamcrest.Matchers;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.testsupport.deferred.Eventually.assertDeferred;
import static com.oracle.bedrock.testsupport.deferred.Eventually.within;
import static java.util.Arrays.stream;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
* NearCache tests
*
* @author mf 2006.07.26
*/
public class NearCacheTests
        extends AbstractFunctionalTest implements Serializable
    {
    // ----- constructors ---------------------------------------------------
    /**
    * Default constructor.
    */
    public NearCacheTests()
        {
        super(FILE_CFG_CACHE);
        System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE);
        Logger.config("coherence.cacheconfig=" + FILE_CFG_CACHE);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServer("NearCacheTests-1", "near", FILE_CFG_CACHE,
                         null, true, null,
                         JvmOptions.include("-XX:+ExitOnOutOfMemoryError"));
        startCacheServer("NearCacheTests-2", "near", FILE_CFG_CACHE, null, true, null,
                         JvmOptions.include("-XX:+ExitOnOutOfMemoryError"));
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("NearCacheTests-1");
        stopCacheServer("NearCacheTests-2");
        }


    // ----- test methods ---------------------------------------------------

    /**
    * Test concurrent puts
    */
    @Test
    public void testConcurrentPut()
        {
        test(new PutAgent());
        }

    /**
    * Test concurrent puts
    */
    @Test
    public void testConcurrentPutAll()
        {
        test(new PutAllAgent());
        }

    /**
    * Test contending concurrent puts
    */
    @Test
    public void testContentionPutAll()
        {
        test(new ContentionPutAllAgent());
        }

    /**
    * COH-4211
    *
    * Test near cache contains key after put with expiry
    */
    @Test
    public void testCoh4211()
        {
        NearCache cache = (NearCache) getNamedCache("near-coh4211");
        try
            {
            cache.put("1", "2");
            cache.get("1");
            cache.put("1", "2", 1000l);
            assertEquals(1, cache.getFrontMap().size());
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
    * COH-4447
    *
    * Test NearCache.getAll() using invalidation strategy "none"
    */
    @Test
    public void testCoh4447()
        {
        NamedCache cache = getNamedCache("coh-4447-near");
        try
            {
            cache.put("TEST1", "one");
            ((NearCache) cache).getFrontMap().clear();
            Set setKeys = new HashSet(cache.keySet());

            Map map1 = cache.getAll(setKeys);
            Map map2 = cache.getAll(setKeys);

            assertSame(map1.get("TEST1"), map2.get("TEST1"));
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
     * COH-8796
     *
     * Test the NearCache Invalidation Strategy "logical"
     */
    @Test
    public void testCoh8796()
        {
        NamedCache cache = getNamedCache("coh-8796-near");
        try
            {
            int        cPuts    = 1000;
            Map        frontMap = ((NearCache) cache).getFrontMap();
            NamedCache backMap  = (NamedCache) ((NearCache) cache).getBackMap();

            for (int i = 0; i < cPuts; i++)
                {
                backMap.put("TEST" + i, i, 5000l);
                if (i % 2 == 0)
                    {
                    cache.get("TEST" + i);
                    }
                }

            assertEquals(cPuts / 2, frontMap.size());
            assertEquals(cPuts, backMap.size());

            // expire the back map
            // calling size() expires the entries in the back and sends out synthetic deletes
            Eventually.assertThat(invoking(backMap).size(), is(0));

            // ensure that synthetic deletes were filtered out: front map values for evens are still there
            for (int i = 0; i < cPuts; i++)
                {
                if (i % 2 == 0)
                    {
                    assertEquals(i, ((Integer) frontMap.get("TEST" + i)).intValue());
                    }
                else
                    {
                    assertNull(frontMap.get("TEST" + i));
                    }
                }

            assertEquals(cPuts / 2, frontMap.size());       // 2, 4, 6...
            assertEquals(0, backMap.size());

            // ensure that put works, and that a value update is properly distributed to both the front and back map
            for (int i = 0; i < cPuts / 4; i++)
                {
                String sKey = "TEST" + (i * 4 + 2);
                int    nVal = i * 4;

                cache.put(sKey, nVal);

                assertEquals(nVal, frontMap.get(sKey));
                assertEquals(nVal, backMap.get(sKey));
                }

            assertEquals(cPuts / 2, frontMap.size());       // 0, 2, 4...
            assertEquals(cPuts / 4, backMap.size());        // 2, 6, 10...

            // ensure that a synthetic update event causes eviction from the front
            for (int i = 0; i < cPuts / 4; i++)
                {
                String sKey = "TEST" + (i * 4 + 2);
                int    nVal = i * 4 + 2;                    // different nVal from for loop above

                backMap.put(sKey, nVal);

                assertEquals(nVal, backMap.get(sKey));
                assertNull(frontMap.get(sKey));
                }

            assertEquals(cPuts / 4, frontMap.size());       // 0, 4, 8...
            assertEquals(cPuts / 4, backMap.size());        // 2, 6, 10...

            // ensure that a synthetic insert event caused by a rwbm going to its
            // cache store results in front eviction
            for (int i = 0; i < cPuts / 8; i++)
                {
                String sKey = "TEST" + i * 4;

                assertEquals((i*4), frontMap.get(sKey));

                // force a read to cache store because we know it doesn't exist in the back
                assertEquals(sKey, backMap.get(sKey));
                assertNull(frontMap.get(sKey));
                }

            assertEquals(cPuts / 8, frontMap.size());       // 500, 504, 508...
            assertEquals((3 * cPuts) / 8, backMap.size());  // 2, 4, 6, 8, 10...502, 506, 510...

            // ensure that a remove called directly on the back results in a removal from both maps
            for (int i = cPuts / 8; i < cPuts / 4; i++)
                {
                String sKey = "TEST" + i * 4;

                cache.put(sKey, sKey);

                assertEquals(sKey, frontMap.get(sKey));
                assertEquals(sKey, backMap.get(sKey));

                backMap.remove(sKey);

                assertNull(frontMap.get(sKey));
                assertFalse(backMap.containsKey(sKey)); // get on the NullCacheStore would have returned the key back
                }

            assertEquals(0, frontMap.size());
            assertEquals((3 * cPuts) / 8, backMap.size());   // 2, 4, 6, 8, 10...502, 506, 510...
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
     * Regression test for COH-15021 (courtesy of Tim Middleton).
     */
    @Test
    public void testCoh15021()
        {
        TestCoh15021.testCoh15021(getNamedCache("simple-dist"));
        }

    /**
     * COH-15130, test that front map is correctly updated if
     * entry value are mutated by interceptors.  In the TestInterceptor,
     * any update to the value is reverted back to the original value.
     */
    @Test
    public void testCoh15130()
        {
        NamedCache cache    = getNamedCache("coh-15130-near");
        Map        frontMap = ((NearCache) cache).getFrontMap();

        for (int i = 0; i< 5; i++)
            {
            cache.put(i, i);
            cache.get(i);
            assertEquals(i, frontMap.get(i));

            cache.put(i, i + 1);  // this will be reverted by the interceptor
            assertEquals(i, frontMap.get(i));
            }

        Map map = new HashMap();
        for (int i = 0; i < 5; i++)
            {
            map.put(i, i + 5);
            }

        cache.putAll(map);  // this will be reverted by the interceptor

        for (int i = 0; i< 5; i++)
            {
            assertEquals(i, frontMap.get(i));
            }
        }

    /**
     * Test near caches invalidations.
     */
    @Test
    public void testInvalidations()
        {
        NearCache nc1 = new NearCache(new LocalCache(), getNamedCache("simple-dist"));
        nc1.clear();
        nc1.put("100", "Ballav");
        assertNull(nc1.getFrontMap().get("100"));
        assertEquals("Ballav", nc1.get("100"));
        assertEquals("Ballav", nc1.getFrontMap().get("100"));
        long lInvHits = nc1.getInvalidationHits();
        long lInvMisses = nc1.getInvalidationMisses();

         // invalidation is called for existing entry
        NearCache nc2 = new NearCache(new LocalCache(), getNamedCache("simple-dist"));
        nc2.put("100", "unknown");
        assertEquals("unknown", nc1.get("100"));

        // adding an listener when getting an entry for the first time
        assertEquals("unknown", nc2.get("100"));
        assertEquals(1, nc2.getTotalRegisterListener());

        assertEquals(lInvHits + 1, nc1.getInvalidationHits());
        assertEquals(lInvMisses, nc1.getInvalidationMisses());

        lInvHits = nc1.getInvalidationHits();
        lInvMisses = nc1.getInvalidationMisses();
        nc2.put("101", "Sam");
        assertEquals("Sam", nc1.get("101"));
        assertEquals(lInvHits, nc1.getInvalidationHits());
        assertEquals(lInvMisses, nc1.getInvalidationMisses());
        }

    /**
     * Regression test for COH-18336
     * @throws Exception
     */
    @Test
    public void testTruncate()
            throws Exception
        {
        NearCache cache = (NearCache) getNamedCache("near-TruncateTest");
        try
            {
            MBeanServer server          = MBeanHelper.findMBeanServer();
            ObjectName  objNameCache    = new ObjectName("Coherence:type=Cache,tier=front,name=near-TruncateTest,*");
            ObjectName  objNameStorage1 = new ObjectName("Coherence:type=StorageManager,service=DistributedCache,cache=near-TruncateTest,nodeId=2");
            ObjectName  objNameStorage2 = new ObjectName("Coherence:type=StorageManager,service=DistributedCache,cache=near-TruncateTest,nodeId=3");

            MBeanServerHandler mbeanServerHandler = new MBeanServerHandler(server);

            // make sure that the near cache mbean has been registered
            Eventually.assertThat(invoking(mbeanServerHandler).getMBeanCount(objNameCache), is(1));
            assertDeferred(() -> getMBeanAttribute(server, objNameStorage1, "ListenerKeyCount"), is(0));
            assertDeferred(() -> getMBeanAttribute(server, objNameStorage2, "ListenerKeyCount"), is(0));

            cache.put("foo", "bar");
            assertEquals(cache.get("foo"), "bar");
            Eventually.assertThat(invoking(mbeanServerHandler).getMBeanCount(objNameStorage1), is(1));
            Eventually.assertThat(invoking(mbeanServerHandler).getMBeanCount(objNameStorage2), is(1));
            assertDeferred(() -> getMBeanAttribute(server, objNameStorage1, "ListenerKeyCount") + getMBeanAttribute(server, objNameStorage2, "ListenerKeyCount"), is(1));

            for (int i = 1; i <= 20; i++)
                {
                cache.put("key_" + i, "value_" + i);
                cache.get("key_" + i);
                }
            assertDeferred("assert equal number of keys in front map and key listeners on back map",
                           () -> getMBeanAttribute(server, objNameStorage1, "ListenerKeyCount") + getMBeanAttribute(server, objNameStorage2, "ListenerKeyCount"),
                           is(cache.getFrontMap().size()));

            cache.truncate();
            assertDeferred(()->cache.getFrontMap().size(), is(0));
            assertDeferred(()->cache.getControlMap().size(), is(0));
            Logger.finer("dumping held locks after truncate front map size=" + cache.getFrontMap().size() + " controlMap size=" + cache.getControlMap().size());
            ((SegmentedConcurrentMap)cache.getControlMap()).dumpHeldLocks();
            Eventually.assertThat(invoking(cache).get("foo"), Matchers.nullValue());
            assertDeferred(() -> getMBeanAttribute(server, objNameStorage1, "ListenerKeyCount"), is(0));
            assertDeferred(() -> getMBeanAttribute(server, objNameStorage2, "ListenerKeyCount"), is(0));

            cache.put("bar", "foo");
            assertEquals(cache.get("bar"), "foo");
            for (int i = 1; i <= 20; i++)
                {
                cache.put("key_" + i, "value_" + i);
                cache.get("key_" + i);
                }
            assertDeferred("assert equal number of keys in front map and key listeners on back map",
                           () -> getMBeanAttribute(server, objNameStorage1, "ListenerKeyCount") + getMBeanAttribute(server, objNameStorage2, "ListenerKeyCount"),
                           is(cache.getFrontMap().size()));

            Eventually.assertThat(invoking(mbeanServerHandler).getMBeanCount(objNameCache), is(1), Timeout.after("2000"));
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
     * Regression test for COH-28721.
     * Simulate concurrent gets occurring when a cache truncate occurs.
     * Validate NearCache truncate post-conditions that no map control key locks should exist after the truncate.
     * Validate none of the concurrent threads hang.
     */
    @Test
    public void testTruncateReleaseOutstandingKeyLock()
        {
        Thread        arThread[]  = new Thread[3];
        NearCache     cache       = (NearCache)getNamedCache("near-TruncateReleaseOutstandingKeyLockTest");
        AtomicBoolean fKeyLocked  = new AtomicBoolean(false);
        AtomicInteger cBlockedGet = new AtomicInteger(0);

        try
            {
            cache.put("key", "value");
            cache.get("key");
            assertThat("ensure invalidation strategy is PRESENT", cache.getInvalidationStrategy(), is(CachingMap.LISTEN_PRESENT));
            cache.put("foo", "bar");

            // simulate 3 concurrent gets just before a truncate
            arThread[0] = new Thread(()->
                                     {
                                     // simulate a get that is not completed and waiting for result from registering listener and getting priming event
                                     String                 sKey       = "foo";
                                     SegmentedConcurrentMap mapControl = (SegmentedConcurrentMap) cache.getControlMap();

                                     if (mapControl.lock(sKey, -1))
                                         {
                                         // allow other two concurrent get threads to proceed now that key lock has been acquired.
                                         fKeyLocked.compareAndSet(false, true);

                                         List listEvents = new LinkedList();
                                         mapControl.put("foo", listEvents);
                                         try
                                             {
                                             Blocking.sleep(120000);
                                             }
                                         catch (InterruptedException e)
                                             {
                                             e.printStackTrace();
                                             Thread.currentThread().interrupt();
                                             throw Base.ensureRuntimeException(e, "interrupted simulated hung thread");
                                             }
                                         }
                                     },
                                     "simulateUncompletedGetHoldingKeyLockForFoo");

            arThread[1] = new Thread(()->
                                     {
                                     // this thread will block in NearCache get() due to thread simulateUncompletedGetHoldingKeyLockForFoo
                                     while (! fKeyLocked.get())
                                         {
                                         try
                                             {
                                             Blocking.sleep(100);
                                             }
                                         catch (InterruptedException e)
                                             {
                                             throw new RuntimeException(e);
                                             }
                                         }
                                     try
                                         {
                                         cBlockedGet.getAndIncrement();
                                         Logger.info("thread " + this + " get(\"foo\") returned value of " + cache.get("foo"));
                                         }
                                     catch (Throwable t)
                                         {
                                         Logger.info("thread " + this + " terminated with an exception. \nStack trace:\n" + Base.printStackTrace(t));
                                         }
                                     },
                                     "BlockedGetForFoo1");

            arThread[2] = new Thread(()->
                                     {
                                     // this thread will block in NearCache get() due to thread simulateUncompletedGetHoldingKeyLockForFoo
                                     while (! fKeyLocked.get())
                                         {
                                         try
                                             {
                                             Blocking.sleep(100);
                                             }
                                         catch (InterruptedException e)
                                             {
                                             throw new RuntimeException(e);
                                             }
                                         }
                                     try
                                         {
                                         cBlockedGet.getAndIncrement();
                                         Logger.info("thread " + this + " get(\"foo\") returned value of " + cache.get("foo"));
                                         }
                                     catch (Throwable t)
                                         {
                                         Logger.info("thread " + this + " terminated with an exception. \nStack trace:\n" + Base.printStackTrace(t));
                                         }
                                     },
                                     "BlockedGetForFoo2");

            for (Thread t : arThread)
                {
                t.start();
                }

            // wait until 3 threads have progressed to desire state
            while (!fKeyLocked.get() && cBlockedGet.get() == 2)
                {
                Blocking.sleep(10);
                }

            assertDeferred("ensure simulating concurrent get() by ensuring there is a current lock", () -> cache.getControlMap().size(), is(1));
            Logger.info("Dump held locks before truncate");
            ((SegmentedConcurrentMap)cache.getControlMap()).dumpHeldLocks();

            // assert all 3 concurrent gets are where we think they should be
            assertThreadStackTrace(arThread[0], Blocking.class, "sleep");
            assertThreadStackTrace(arThread[1], SegmentedConcurrentMap.class, "lock");
            assertThreadStackTrace(arThread[1], SegmentedConcurrentMap.LockableEntry.class, "waitForNotify");
            assertThreadStackTrace(arThread[2], SegmentedConcurrentMap.class, "lock");
            assertThreadStackTrace(arThread[2], SegmentedConcurrentMap.LockableEntry.class, "waitForNotify");

            // truncate will interrupt all threads holding locks
            cache.truncate();

            try
                {
                // verify truncate post conditions
                Eventually.assertDeferred("validate truncate post condition condition that near cache control map is zero",
                                          () -> cache.getControlMap().size(), is(0),
                                          within(30, TimeUnit.SECONDS));
                }
            catch (Throwable t)
                {
                Logger.info("Dump held locks after truncate and handling assertion exception:  " + t);
                ((SegmentedConcurrentMap)cache.getControlMap()).dumpHeldLocks();

                throw t;
                }

            // ensure all threads completed and none are blocked
            for (Thread t : arThread)
                {
                t.join(30000);
                assertThat("verify thread " + t + " not hung \n" + Base.getStackTrace(t), t.isAlive(), is(false));
                }
            }
        catch (InterruptedException e)
            {
            throw new RuntimeException(e);
            }
        finally
            {
            // cleanup
            cache.destroy();
            for (Thread t: arThread)
                {
                if (t != null && t.isAlive())
                    {
                    t.interrupt();
                    }
                }
            }
        }

    /**
     * Regression test for Bug 26725338
     * @throws Exception
     */
    @Test
    public void testMBeanUnregistration()
            throws Exception
        {
        NamedCache cache = getNamedCache("coh-mbean-near");
        try
            {
            cache.put("key", "value");

            MBeanServer        server             = MBeanHelper.findMBeanServer();
            ObjectName         objName            = new ObjectName("Coherence:type=Cache,tier=front,name=coh-mbean-near,*");
            MBeanServerHandler mbeanServerHandler = new MBeanServerHandler(server);

            // make sure that the near cache mbean has been registered
            Eventually.assertThat(invoking(mbeanServerHandler).getMBeanCount(objName), is(1));

            // shutdown the cache service and make sure that the registered mbean will be unregistered
            cache.getCacheService().shutdown();

            Eventually.assertThat(invoking(mbeanServerHandler).getMBeanCount(objName), is(0));
            }
        finally
            {
            cache.destroy();
            }
        }

    @Test
    public void testSyntheticUpdate()
        {
        NamedCache cache = getNamedCache("near-4ever-foo");
        try
            {
            // prime the front
                {
                cache.put(1, "foo");

                assertThat(cache.get(1), is("foo"));
                }

            // perform a logical (non-synthetic update) and verify the front is updated
                {
                cache.invoke(1, entry ->
                    {
                    entry.setValue("foo2", false);
                    return null;
                    });
                assertThat(cache.get(1), is("foo2"));
                }

            // perform a synthetic update and verify the front is updated
                {
                cache.invoke(1, entry ->
                    {
                    entry.setValue("foo3", true);
                    return null;
                    });
                assertThat(cache.get(1), is("foo3"));
                }
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
     * Test for COH-23095
     */
    @Test
    public void testCoh23095()
        {
        NearCache<Integer, Person> cache = (NearCache) getNamedCache("near-coh-23095");

        CacheStatistics cacheStats     = getFrontMapCacheStatistics(cache);
        long            cInitialMisses = cacheStats.getCacheMisses();

        cache.computeIfAbsent(2,
            person -> new Person("4321", "frist", "lats", 2000, null, new String[0]));
        assertEquals(cacheStats.getCacheMisses(), cInitialMisses + 1);

        cache.getOrDefault(3,
            new Person("5678", "a", "b", 1980, null, new String[0]));
        // 2 misses in getOrDefault(), frontMap is checked twice
        assertEquals(cacheStats.getCacheMisses(), cInitialMisses + 3);
        }

    @Test
    public void testCoh24641()
        {
        NamedCache<Integer, Person> cache = getNamedCache("near-coh-24641");

        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int x = 0; x < 10; x++)
            {
            executor.submit(() ->
                {
                for (int j = 0; j < 10000; j++)
                    {
                    cache.put(j, new Person("4321" + j, "first" + j, "last", 2000, null, new String[0]), 300L);
                    cache.get(j);
                    }

                // force eviction
                Base.sleep(300);

                return null;
                });
            }

        executor.shutdown();

        try
            {
            executor.awaitTermination(60, TimeUnit.SECONDS);
            }
        catch (InterruptedException ie)
            {
            Assert.fail("Test timed out");
            }

        // Caffeine front cache background eviction intermittently does not completely clear the cache, it does not have low units
        // computing size does not ensure eviction processing of expired entries is completed for CaffeineCache
        for (int j = 0; j < 10000; j++)
            {
            cache.get(j);
            }

        // eventually front cache should be empty
        Eventually.assertThat(invoking(((NearCache) cache).getFrontMap()).size(), is(0));
        }

    /**
    * Test runner
    */
    public void test(AbstractTestAgent agent)
        {
        // run put agent on all servers
        InvocationService svc = (InvocationService)
            CacheFactory.getService("InvocationService");

        NamedCache cache = getNamedCache("near-test");
        agent.setCache(cache);
        try
            {
            Set setMembers = svc.getInfo().getServiceMembers();
            cache.put(RUNNING, Integer.valueOf(setMembers.size()));
            String sAgent = agent.getClass().getName();
            sAgent = sAgent.substring(sAgent.indexOf("$") + 1);
            out("running " + setMembers.size() + " " + sAgent +
                " test agents on " + cache.getCacheName());
            Map mapResult = svc.query(agent, setMembers);

            // validate results
            for (Iterator iter = mapResult.values().iterator(); iter.hasNext(); )
                {
                Object oResult = iter.next();
                assertEquals(Boolean.TRUE, oResult);
                }
            }
        finally
            {
            // TODO: would prefer synchronous destroy()
            getNamedCache(cache.getCacheName()).clear();
            }
        }

    /**
    * Abstract base class for test agents.
    */
    public abstract class AbstractTestAgent
           extends AbstractInvocable
        {
        public void run()
            {
            ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                        .getConfigurableCacheFactory(m_sCacheConfig, null);
            setFactory(factory);

            try
                {
                setResult(Boolean.valueOf(test()));
                }
            catch (RuntimeException e)
                {
                out("local exception " + e);
                e.printStackTrace();
                setResult(e);
                throw e;
                }
            finally
                {
                finish();
                }
            }

        public void setCache(NamedCache cache)
            {
            m_sCacheName = cache.getCacheName();
            }

        public NamedCache getCache()
            {
            NamedCache cache = getNamedCache(m_sCacheName);
            if (!cache.isActive())
                {
                // re-create destroyed cache
                cache.release();
                cache = getNamedCache(m_sCacheName);
                }
            return cache;
            }

        /**
         * Mark myself as finished.
         */
        protected void finish()
            {
            if (!m_fFinished)
                {
                out("local node finished");
                m_fFinished = true;
                getCache().invoke(RUNNING, new NumberIncrementor(
                        (PropertyManipulator) null, Integer.valueOf(-1), false));
                }
            }

        /**
         * Helper method to wait for peers to finish.
         */
        protected void waitForPeers()
            {
            finish();

            out("waiting for others...");
            NamedCache cache = getCache();
            assertDeferred(() -> cache.get(RUNNING), is((Object) Integer.valueOf(0)));
            }

        /**
         * Helper method for validating that the front is a subset of the back.
         *
         * @return true if the front is valid
         */
        protected boolean validateFront()
            {
            NearCache cache    = (NearCache) getCache();
            Map       mapFront = cache.getFrontMap();
            Map       mapBack  = cache.getBackMap();
            out("validating front(" + mapFront.size() +
                    ") against back(" + mapBack.size() + ")");
            boolean fValid = mapBack.keySet().containsAll(mapFront.keySet());
            if (!fValid)
                {
                Logger.err("Back map doesn't contain all the keys present in the front map!");
                }

            Set setDiff = new HashSet();
            for (Object oKey : mapFront.keySet())
                {
                if (!mapBack.get(oKey).equals(mapFront.get(oKey)))
                    {
                    setDiff.add(oKey);
                    }
                }

            if (!setDiff.isEmpty())
                {
                fValid = false;
                Logger.err("The following keys in the front and back map have different values: " + setDiff);
                }

            return fValid;
            }

        /**
         * Test method to be implemented.
         * @return true if test passed false otherwise
         */
        abstract public boolean test();

        protected transient boolean m_fFinished;
        protected           String  m_sCacheName;
        protected           String  m_sCacheConfig = System.getProperty("coherence.cacheconfig");
        }

    /**
    * Put test agent.
    */
    public class PutAgent
           extends AbstractTestAgent
        {
        public boolean test()
            {
            out("Running PutAgent");
            Random     rand    = new Random();
            int        iKeyMax = m_nKeys;
            NamedCache cache   = getCache();
            cache.put(Integer.valueOf(0), null); // include a null value
            for (int i = 0, c = m_nIters; i < c; ++i)
                {
                Object oKey = Integer.valueOf(rand.nextInt(iKeyMax));
                Object oVal = Integer.valueOf(rand.nextInt());
                cache.put(oKey, oVal);

                if (i == c >>> 1)
                    {
                    // fill the front
                    cache.getAll(cache.keySet());
                    }
                else
                    {
                    // pull a random item into the front
                    cache.get(Integer.valueOf(rand.nextInt(iKeyMax)));
                    // overwrite last with null
                    cache.put(oKey, null);
                    }
                }

            // wait everyone to finish
            waitForPeers();

            // validate that front is a subset of the back
            return validateFront();
            }

        protected int m_nKeys  = 10000;
        protected int m_nIters = 100;
        }

    /**
    * Put test agent.
    */
    public class PutAllAgent
           extends AbstractTestAgent
        {
        public boolean test()
            {
            out("Running PutAllAgent");
            Random     rand     = new Random();
            int        iKeyMax  = m_nKeys;
            NamedCache cache    = getCache();
            Map        mapBatch = new HashMap();
            Object     oKey     = null;
            for (int i = 0, c = m_nIters; i < c; ++i)
                {
                for (int j = 0; j < m_nBatch; ++j)
                    {
                    oKey = Integer.valueOf(rand.nextInt(iKeyMax));
                    Object oVal = Integer.valueOf(rand.nextInt());

                    mapBatch.put(oKey, oVal);
                    }
                mapBatch.put(Integer.valueOf(0), null);
                cache.putAll(mapBatch);


                if (i == c >>> 1)
                    {
                    // fill the front
                    cache.getAll(mapBatch.keySet());
                    }
                else
                    {
                    // pull a random item into the front
                    cache.get(Integer.valueOf(rand.nextInt(iKeyMax)));
                    }

                mapBatch.clear();
                // include a null overwrite
                mapBatch.put(oKey, null);
                }

            // wait everyone to finish
            waitForPeers();

            // validate that front is a subset of the back
            return validateFront();
            }

        protected int m_nKeys  = 10000;
        protected int m_nBatch = 100;
        protected int m_nIters = 100;
        }

    /**
    * Put test agent.
    */
    public class ContentionPutAllAgent
           extends AbstractTestAgent
        {
        public boolean test()
            {
            out("Running ContentionPutAllAgent");
            Random     rand     = new Random();
            int        iKeyMax  = m_nKeys;
            NamedCache cache    = getCache();
            Map        mapBatch = new HashMap();
            Object     oKey     = null;
            for (int i = 0, c = m_nIters; i < c; ++i)
                {
                for (int j = 0; j < m_nBatch; ++j)
                    {
                    oKey = Integer.valueOf(j);
                    Object oVal = Integer.valueOf(rand.nextInt());

                    mapBatch.put(oKey, oVal);
                    }
                mapBatch.put(Integer.valueOf(0), null);
                cache.putAll(mapBatch);

                if (i == c >>> 1)
                    {
                    // fill the front
                    cache.getAll(mapBatch.keySet());
                    }
                else
                    {
                    // pull a random item into the front
                    cache.get(Integer.valueOf(rand.nextInt(iKeyMax)));
                    }

                mapBatch.clear();
                // include a null overwrite
                mapBatch.put(oKey, null);
                }

            // wait everyone to finish
            waitForPeers();

            // validate that front is a subset of the back
            return validateFront();
            }

        protected int m_nKeys  = 10000;
        protected int m_nBatch = 100;
        protected int m_nIters = 100;
        }

    /**
     * Static inner class to be used with Eventually.assertThat.
     * Eventually.assertThat cannot take MBeanServer as an argument in the invoking method
     * as the implementation is a final class. Hence this handler class, to be used
     * as a sort of wrapper object.
     */
    public static class MBeanServerHandler
        {
        public MBeanServerHandler(MBeanServer server)
            {
            this.m_mbeanServer = server;
            }

        public int getMBeanCount(ObjectName query)
            {
            return m_mbeanServer.queryMBeans(query, null).size();
            }

        private final MBeanServer m_mbeanServer;
        }

    /**
     * Test interceptor for COH-15130
     */
    @Interceptor(identifier = "Mutator")
    @EntryEvents({EntryEvent.Type.UPDATING})
    public static class TestInterceptor
            implements EventInterceptor<EntryEvent<?, ?>>, Serializable
        {
        @Override
        public void onEvent(EntryEvent<?, ?> evt)
            {
            assertTrue(evt.getType() == EntryEvent.Type.UPDATING);

            for (Object oEntry : ((EntryEvent) evt).getEntrySet())
                {
                BinaryEntry binEntry = (BinaryEntry) oEntry;
                binEntry.setValue(binEntry.getOriginalValue());
                }
            }
        }

    /**
     * Regression test for COH-26003 failed to detect a lock is being held by a terminated thread
     */
    @Test
    public void testCoh26003() throws InterruptedException
        {
        NamedCache<String, String>    cacheBack = getNamedCache("simple-dist");
        NearCache<String, String>     cache     = new NearCache(new LocalCache(1000), cacheBack);
        String                        key       = "theKey";

        cache.put(key, "someValue");

        HoldMapControlKeyLock holdLockAction = new HoldMapControlKeyLock(cache, key);
        Thread                thread         = new Thread(holdLockAction, "TroubleThreadHoldingKeyLock");

        thread.start();
        thread.join();

        assertDeferred("wait for thread to terminate so auto drop of lock from a terminated thread can work", () -> thread.isAlive(), is(false));

        // remove this assertion if fix NearCache.getMapControl.lock(key, 0) to check if thread is terminated
        assertDeferred("confirm can not get lock since terminated thread has it", () -> cache.getControlMap().lock(key, 0), is(false));

        // update backing map so NearCache.validate(MapEvent) called.
        // before fix got  "Detected a state corruption on the key" in CachingMap.validate(MapEvent)
        // since thread   thread still holding mapControl lock for key.
        int MAX = 4;
        for (int i=0; i<=4; i++)
            {
            // this used to cause "Detected a state corruption on the key key990481 ..." in CachingMap.validate(MapEvent) on locked key held by a terminated thread.
            cacheBack.put(key, "someValue_updated_" + i);
            }

        assertThat(cache.get(key), is("someValue_updated_" + MAX));

        try
            {
            assertDeferred("confirm can get lock since terminated thread no longer has it", () -> cache.getControlMap().lock(key, 0), is(true));
            }
        finally
            {
            assertThat(cache.getControlMap().unlock(key), is(true));
            }
        }

    // ----- inner class: HoldMapControlKeyLock ------------------------------
    /**
     * Simulate failure case that lock was acquired by a thread and thraad terminated before
     * releasing lock.
     */
    private static class HoldMapControlKeyLock
            implements Runnable
        {
        // ----- constructor --------------------------------------------------

        public HoldMapControlKeyLock(NearCache cache, String key)
            {
            m_cache = cache;
            m_sKey = key;
            }

        // ----- Runnable methods --------------------------------------------

        @Override
        public void run()
            {
            // intentionally get lock and don't unlock it for test scenario
            boolean fResult = m_cache.getControlMap().lock(m_sKey);

            assertThat(fResult, is(true));
            }

        private NearCache m_cache;
        private String    m_sKey;
        }

    // ----- helpers ---------------------------------------------------------

    /**
     * Return {@link CacheStatistics} for front map.
     *
     * @param cache  the near cache
     *
     * @return {@link CacheStatistics} for front map
     */
    protected CacheStatistics getFrontMapCacheStatistics(NearCache cache)
        {
        return ((LocalCache) cache.getFrontMap()).getCacheStatistics();
        }

    /**
     * Assert thread stack trace contains a caller with {@code clz} name and {@code sMethodName method name}.
     *
     * @param t            the thread
     * @param clz          the class to verify in thread stack
     * @param sMethodName  the method name to verify in thread stack
     */
    protected void assertThreadStackTrace(Thread t, Class clz, String sMethodName)
        {
        Eventually.assertDeferred("validate that thread " + t + " contains call to " + clz.getName() + "." + sMethodName,
                                  () -> stream(t.getStackTrace()).anyMatch((e)-> e.getClassName().equals(clz.getName()) && e.getMethodName().equals(sMethodName)), is(true));
        }

    /**
     * Get attribute value for MBean {@code objectName}.
     *
     * @param server      MBean server
     * @param objectName  MBean object name
     * @param sAttribute  MBean attribute that should be an integer
     *
     * @return MBean attribute integer value or throw runtime exception if not found.
     */
    protected int getMBeanAttribute(MBeanServer server, ObjectName objectName, String sAttribute)
        {
        try
            {
            return (Integer) server.getAttribute(objectName, sAttribute);
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    // ---- constants -------------------------------------------------------

    protected static final String RUNNING  = "running-agents";
    protected static String FILE_CFG_CACHE = "near-cache-config.xml";

    @ClassRule
    public static final ThreadDumpOnTimeoutRule timeout = ThreadDumpOnTimeoutRule.after(15, TimeUnit.MINUTES);
    }
