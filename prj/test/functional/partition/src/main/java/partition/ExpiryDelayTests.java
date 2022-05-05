/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package partition;


import com.oracle.coherence.common.base.Blocking;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.cache.ConfigurableCacheMap;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap.Entry;

import com.tangosol.util.processor.AbstractProcessor;

import com.oracle.coherence.testing.AbstractRollingRestartTest;

import java.io.Serializable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


/**
 * Tests for the cache configured for automatic expiry.
 */
public class ExpiryDelayTests
        extends AbstractRollingRestartTest
    {
    // ----- constructors -------------------------------------------------

    /**
     * Default constructor.
     */
    public ExpiryDelayTests()
        {
        super(s_sCacheConfig);
        }


    // ----- AbstractRollingRestartTest methods ---------------------------

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


    // ----- test methods -------------------------------------------------

    /**
     * Test that expiry values are kept after restarting servers.
     */
    @Test
    public void testExpiryDelay()
        {
        final int     cKeys   = 20;
        final HashMap mapData = new HashMap(cKeys);

        for (int i = 0; i < cKeys; i++)
            {
            mapData.put(Integer.valueOf(i), Integer.valueOf(0));
            }

        doTest("expiry-delay", "expiry-delay", mapData, /*fExpiry*/true);
        }

    /**
     * Test that expiry values are kept after restarting servers.
     */
    @Test
    public void testExpiryDelayWithLargeValues()
        {
        final int     cKeys      = 10;
        final int     cValueSize = 256;
        final byte[]  ab         = new byte[cValueSize];
        final HashMap mapData    = new HashMap(cKeys);

        for (int i = 0; i < cValueSize; i++)
            {
            ab[i] = 'a';
            }

        for (int i = 0; i < cKeys; i++)
            {
            mapData.put(Integer.valueOf(i), new Binary(ab));
            }

        doTest("expiry-delay", "expiry-with-large-val", mapData, /*fExpiry*/true);
        }

    /**
     * Test that expiry values are zero if the expriry delay is set
     * to zero to the backing map.
     */
    @Test
    public void testNoExpiryDelay()
        {
        final int     cKeys   = 20;
        final HashMap mapData = new HashMap(cKeys);

        for (int i = 0; i < cKeys; i++)
            {
            mapData.put(Integer.valueOf(i), Integer.valueOf(0));
            }

        doTest("no-expiry-delay", "no-expiry-delay", mapData, /*fExpiry*/false);
        }

    @Test
    public void testConcurrentExpiryAndEP() throws InterruptedException
        {
        startCacheServer("ExpriyStorage1", getProjectName(), getCacheConfigPath());

        final NamedCache   cache   = getNamedCache("dist-bme-test");
        PartitionedService service = (PartitionedService) cache.getCacheService();

        Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(1));
        try
            {
            cache.put(1, 1, 1000L);

            Thread t = Base.makeThread(null, new Runnable()
                {
                @Override public void run()
                    {
                    cache.invoke(1, new SleepProcessor());
                    }
                }, "EPInvoker");
            t.start();

            // wait for entry to be eligible for eviction
            Blocking.sleep(1100);

            // cause eviction concurrent to a front door request
            cache.size();

            // allow the EP to complete
            Eventually.assertThat(invoking(cache).get(1), is((Object) Integer.valueOf(2)));
            }
        finally
            {
            stopCacheServer("ExpriyStorage1");
            Cluster cluster = CacheFactory.getCluster();
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
            }
        }

    @Test
    public void testExpiryDelayWithMixedDelay()
        {
        int     cKeys   = 10;
        HashMap mapData = new HashMap(cKeys);

        for (int i = 0; i < cKeys; i++)
            {
            mapData.put(Integer.valueOf(i), Integer.valueOf(i));
            }

        doTest("expiry-delay", "expiry-multi-delay", mapData, /*fExpiry*/true);
        }

    private void doTest(String sCacheName, String sTestName, Map mapData, boolean fExpiry)
        {
        final NamedCache        cache         = getNamedCache(sCacheName);
        final int               cServers      = 2;
        DistributedCacheService service       = (DistributedCacheService) cache.getCacheService();
        Cluster                 cluster       = CacheFactory.ensureCluster();
        MemberHandler           memberHandler = new MemberHandler(cluster, sTestName, true, false);
        WaitForNodeSafeRunnable nodeSafeRunnable
                = new WaitForNodeSafeRunnable(cache.getCacheService());

        try
            {
            // setup, start the initial cache servers
            for (int i = 0; i < cServers; i++)
                {
                memberHandler.addServer();
                }

            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(cServers + 1));
            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(cServers));

            for (Map.Entry entry : (Set<Map.Entry>) mapData.entrySet())
                {
                cache.put(entry.getKey(), entry.getValue());
                }

            if (sTestName.equals("expiry-multi-delay"))
                {
                for (int i=0; i<10; i++)
                    {
                    if (i%2 == 0)
                        {
                        cache.put("key" + i, "value", -1L);
                        }
                    else
                        {
                        cache.put("key" + i, "value", 120000);
                        }
                    mapData.put("key" + i, "value");
                    }
                }

            Map mapCanons = getExpiryValues(cache, mapData.keySet());

            Base.sleep(500L);

            // perform the rolling restart
            doRollingRestart(memberHandler, cServers + 1, nodeSafeRunnable);

            Map mapResults = getExpiryValues(cache, mapData.keySet());

            for (Object key : mapData.keySet())
                {
                Object oCanon  = mapCanons.get(key);
                Object oResult = mapResults.get(key);

                assertNotNull(oCanon);
                assertNotNull(oResult);

                if (fExpiry)
                    {
                    // The expiry time should not be changed while restarting servers.
                    // However, an error between the expiry value ConfigurableMapCache.Entry
                    // has and the expiry value encoded in a backup may be observed.
                    // So we cannot assert that lCanon and lResult are equal.
                    long lCanon  = ((Long) oCanon).longValue();
                    long lResult = ((Long) oResult).longValue();
                    long lDelta  = Math.abs(lResult - lCanon);
                    // We sleep 500 ms before restarting servers. So the difference will
                    // be more than 500 ms if the expiry time is changed.
                    assertTrue("Expiry time " + lCanon + " was changed to " + lResult,
                               lDelta <= 500L);
                    }
                else
                    {
                    assertEquals(0L, ((Long) oResult).longValue());
                    }
                }
            }
        finally
            {
            memberHandler.dispose();
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
            }
        }


    // ----- helpers ------------------------------------------------------

    /**
     * Return expiry values for the specified keys
     */
    private static Map getExpiryValues(NamedCache cache, Set keys)
        {
        class ExpiryValueProcessor
                extends AbstractProcessor
            {
            public Object process(Entry entry)
                {
                BinaryEntry                binEntry   = (BinaryEntry) entry;
                ConfigurableCacheMap       backingMap = (ConfigurableCacheMap) binEntry.getBackingMap();
                ConfigurableCacheMap.Entry cacheEntry = backingMap.getCacheEntry(binEntry.getBinaryKey());
                return cacheEntry != null ? Long.valueOf(cacheEntry.getExpiryMillis()) : -1;
                }
            }

        Map map = new HashMap();
        for (Object key : keys)
            {
            map.put(key, cache.invoke(key, new ExpiryValueProcessor()));
            }

        return map;
        }

    public static class SleepProcessor
            extends AbstractProcessor
            implements Serializable
        {

        @Override
        public Object process(Entry entry)
            {
            try
                {
                Blocking.sleep(1500);
                entry.setValue(2);
                }
            catch (InterruptedException e) { }
            return null;
            }
        }

    // ----- constants and data members -----------------------------------

    /**
     * The path to the cache configuration.
     */
    public final static String s_sCacheConfig = "coherence-cache-config.xml";

    /**
     * The path to the Ant build script.
     */
    public final static String s_sBuild       = "build.xml";

    /**
     * The project name.
     */
    public final static String s_sProject     = "partition";
    }