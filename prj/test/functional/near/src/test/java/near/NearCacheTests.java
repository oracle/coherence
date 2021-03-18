/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package near;

import com.oracle.bedrock.options.Timeout;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.InvocationService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheStatistics;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.NearCache;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.EntryEvents;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.partition.cache.EntryEvent;

import com.tangosol.net.management.MBeanHelper;

import com.tangosol.util.BinaryEntry;

import com.tangosol.util.processor.NumberIncrementor;
import com.tangosol.util.processor.PropertyManipulator;

import common.AbstractFunctionalTest;
import common.TestCoh15021;

import data.Person;

import java.io.Serializable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.hamcrest.Matchers;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

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
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServer("NearCacheTests-1", "near", FILE_CFG_CACHE);
        startCacheServer("NearCacheTests-2", "near", FILE_CFG_CACHE);
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
        NamedCache cache = getNamedCache("near-TruncateTest");
        try
            {
            cache.put("key", "value");

            MBeanServer        server             = MBeanHelper.findMBeanServer();
            ObjectName         objName            = new ObjectName("Coherence:type=Cache,tier=front,name=near-TruncateTest,*");
            MBeanServerHandler mbeanServerHandler = new MBeanServerHandler(server);

            // make sure that the near cache mbean has been registered
            Eventually.assertThat(invoking(mbeanServerHandler).getMBeanCount(objName), is(1));

            cache.put("foo", "bar");
            assertEquals(cache.get("foo"), "bar");
            cache.truncate();
            Eventually.assertThat(invoking(cache).get("foo"), Matchers.nullValue());

            cache.put("bar", "foo");
            assertEquals(cache.get("bar"), "foo");
            Eventually.assertThat(invoking(mbeanServerHandler).getMBeanCount(objName), is(1), Timeout.after("2000"));
            }
        finally
            {
            cache.destroy();
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

        CacheStatistics cacheStats     = ((LocalCache) cache.getFrontMap()).getCacheStatistics();
        long            cInitialMisses = cacheStats.getCacheMisses();

        cache.computeIfAbsent(2,
            person -> new Person("4321", "frist", "lats", 2000, null, new String[0]));
        assertEquals(cacheStats.getCacheMisses(), cInitialMisses + 1);

        cache.getOrDefault(3,
            new Person("5678", "a", "b", 1980, null, new String[0]));
        // 2 misses in getOrDefault(), frontMap is checked twice
        assertEquals(cacheStats.getCacheMisses(), cInitialMisses + 3);
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
            Eventually.assertThat(invoking(cache).get(RUNNING), is((Object) Integer.valueOf(0)));
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
            boolean fResult = mapBack.entrySet().containsAll(mapFront.entrySet());

            if (!fResult)
                {
                // find delta
                Map mapCopy = new HashMap(mapFront);
                mapCopy.entrySet().remove(mapBack.entrySet());
                String sMsg = "front contains errors:";
                boolean fError = false;
                for (Iterator iter = mapCopy.entrySet().iterator(); iter.hasNext(); )
                    {
                    Map.Entry entry = (Map.Entry) iter.next();
                    Object oKey = entry.getKey();
                    Object oVal = entry.getValue();
                    Object oBackVal = mapBack.get(oKey);

                    if (!equals(oVal, oBackVal) || (oVal == null && mapFront.containsKey(oKey)))
                        {
                        sMsg += " key: " + oKey + " value: " + oVal;
                        if (oVal == null)
                            {
                            sMsg += " in front " + mapFront.containsKey(oKey) + " " + mapFront.get(oKey);
                            }

                        sMsg += " back: " + oBackVal;
                        fError |= true;
                        }
                    }

                if (!fError)
                    {
                    err("validation is wrong");
                    }
                throw ensureRuntimeException(null, sMsg);
                }
            return fResult;
            }

        /**
         * Test method to be implemented.
         * @return true if test passed false otherwise
         */
        abstract public boolean test();

        protected transient boolean m_fFinished;
        protected           String  m_sCacheName;
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

    // ---- constants -------------------------------------------------------

    protected static final String RUNNING  = "running-agents";
    protected static final String FILE_CFG_CACHE = "near-cache-config.xml";
    }
