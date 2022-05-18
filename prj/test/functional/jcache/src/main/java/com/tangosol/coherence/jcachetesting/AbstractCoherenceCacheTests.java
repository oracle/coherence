/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.jcache.common.MBeanServerRegistrationUtility;

import com.tangosol.coherence.jcache.CoherenceBasedCompleteConfiguration;
import com.tangosol.coherence.jcache.AbstractCoherenceBasedCache;


import com.tangosol.coherence.jcache.localcache.LocalCache;

import com.oracle.coherence.testing.SystemPropertyIsolation;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.jcache.common.CoherenceCacheEntryListenerRegistration;

import com.tangosol.net.CacheFactory;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.io.Serializable;

import java.lang.management.ManagementFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;

import javax.cache.Caching;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;

import javax.cache.event.CacheEntryEventFilter;

import javax.cache.integration.CacheLoader;
import javax.cache.integration.CompletionListenerFuture;

import javax.cache.management.CacheStatisticsMXBean;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * Common Cache Tests.
 * Extend this class to configure setup routines to run in different modes.
 *
 * @author         jfialli
 *
 * @see CoherenceBasedCacheTests
 * @see ExtendClientTests
 * @see PartitionedCacheTests
 * @see PartitionedCacheMultipleServersTests
 */
public abstract class AbstractCoherenceCacheTests
        extends TestSupport
    {
    static public void beforeClassSetup()
        {
        s_sPofEnabled = System.getProperty("tangosol.pof.enabled", "true");
        s_sPofConfigUri = System.getProperty("tangosol.pof.config", "coherence-jcache-junit-pof-config.xml");

        System.setProperty("tangosol.pof.enabled", s_sPofEnabled);
        System.setProperty("tangosol.pof.config", s_sPofConfigUri);
        System.setProperty("coherence.jcache.statistics.refreshtime", "0s");

        cacheMgr = getJcacheTestContext().getCacheManager(null, null, null);
        }

    @AfterClass
    static public void afterClassSetup()
        {
        Caching.getCachingProvider().close();
        CacheFactory.shutdown();
        }

    abstract protected <K, V> CompleteConfiguration<K, V> getConfiguration();

    /**
     * add a test CacheEntryListener
     *
     * @param config current test configuration
     *
     * @return test configuration with test CacheEntryListener added
     */
    public CompleteConfiguration extraSetup(CompleteConfiguration config)
        {
        listener = new CacheListenerTests.MyCacheEntryListener();

        if (config.getCacheEntryListenerConfigurations() != null)
            {
            // remove all prior registered listeners.
            Iterator<CoherenceCacheEntryListenerRegistration> iter =
                config.getCacheEntryListenerConfigurations().iterator();

            while (iter.hasNext())
                {
                iter.next();
                iter.remove();
                }
            }

        if (config instanceof CoherenceBasedCompleteConfiguration)
            {
            ((CoherenceBasedCompleteConfiguration) config)
                .addCacheEntryListenerConfiguration(new MutableCacheEntryListenerConfiguration(FactoryBuilder
                    .factoryOf(listener), null, false, true));
            }
        else if (config instanceof MutableConfiguration)
            {
            ((MutableConfiguration) config)
                .addCacheEntryListenerConfiguration(new MutableCacheEntryListenerConfiguration(FactoryBuilder
                    .factoryOf(listener), null, false, true));
            }

        return config;
        }

    /**
     * test create cache and put and get simple values
     */
    @Test
    public void createAndAccessCacheWithSimpleValues()
        {
        Logger.info("start createAndAccessCacheWithSimpleValues");
        ssConfiguration = extraSetup(ssConfiguration);
        ssCache         = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), ssConfiguration);
        assertNotNull(ssCache);
        assertNotNull(cacheMgr.getCache(getTestCacheName(), String.class, String.class));
        assertEquals(getTestCacheName(), ssCache.getName());

        ssCache = cacheMgr.getCache(getTestCacheName(), String.class, String.class);
        System.out.println("Testing Coherence JCache Adapter implementation: " + ssCache.getClass().getCanonicalName());

        Assert.assertEquals(0, listener.getCreated());
        Assert.assertEquals(0, listener.getUpdated());
        Assert.assertEquals(0, listener.getExpired());
        Assert.assertEquals(0, listener.getRemoved());
        Assert.assertNotNull(ssCache);

        ssCache.put("firstKey", "firstValue");
        Assert.assertEquals(1, listener.getCreated());

        String value = ssCache.get("firstKey");

        for (int i = 0; i < 4; i++)
            {
            ssCache.get("firstKey");
            }

        Assert.assertEquals("firstValue", value);
        Assert.assertTrue(ssCache.containsKey("firstKey"));
        Assert.assertFalse(ssCache.containsKey("unknownKey"));

        ssCache.put("firstKey", "updatedValue");

        Assert.assertEquals(1, listener.getUpdated());

        value = ssCache.get("firstKey");
        Assert.assertEquals("updatedValue", value);

        ssCache.put("firstKey", "updatedValueD");

        Assert.assertEquals(2, listener.getUpdated());

        Assert.assertTrue(ssCache.remove("firstKey"));
        Assert.assertEquals(1, listener.getRemoved());
        Assert.assertFalse(ssCache.remove("firstKey"));
        Assert.assertEquals(1, listener.getRemoved());

        Assert.assertFalse(ssCache.containsKey("firstKey"));

        ssCache.put("firstKey", "thirdValueForFirstKey");
        Assert.assertEquals(2, listener.getCreated());

        Assert.assertEquals(2, listener.getUpdated());
        Assert.assertTrue(ssCache.containsKey("firstKey"));

        Map<String, String> entries = new HashMap<String, String>();

        ssCache.put("secondKey", "value2");
        ssCache.put("thirdKey", "value3");
        ssCache.put("fourthKey", "value4");
        ssCache.put("fifthKey", "value5");

        // ssCache.putAll(entries);

        for (Map.Entry<String, String> entry : entries.entrySet())
            {
            Assert.assertTrue(ssCache.containsKey(entry.getKey()));
            Assert.assertEquals(entry.getValue(), ssCache.get(entry.getKey()));
            }

        Assert.assertEquals(6, listener.getCreated());

        Assert.assertEquals(2, listener.getUpdated());
        Assert.assertEquals(1, listener.getRemoved());

        Iterator<Cache.Entry<String, String>> iter = ssCache.iterator();
        int                                   i    = 0;

        while (iter.hasNext())
            {
            i++;

            Cache.Entry<String, String> entry = iter.next();

            Assert.assertTrue(entry.getKey(), ssCache.containsKey(entry.getKey()));
            Assert.assertEquals(entry.getValue(), ssCache.get(entry.getKey()));
            }

        Assert.assertEquals(5, i);
        iter = ssCache.iterator();

        while (iter.hasNext())
            {
            iter.next();
            iter.remove();
            }

        i    = 0;
        iter = ssCache.iterator();

        while (iter.hasNext())
            {
            i++;
            iter.next();
            }

        Assert.assertEquals(0, i);
        Assert.assertEquals(6, listener.getRemoved());
        ssCache.clear();

        Assert.assertFalse(ssCache.containsKey("firstKey"));

        cacheMgr.destroyCache(getTestCacheName());
        assertNull(cacheMgr.getCache(getTestCacheName()));
        ssCache = null;
        }

    /**
     * test PutAll with Complex (not simple) values.
     */
    @Test
    public void putAllWithComplexValues()
        {
        Logger.info("start putAllWithComplexValues");

        spConfiguration = extraSetup(spConfiguration);
        spCache         = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), spConfiguration);

        Assert.assertEquals(0, listener.getCreated());

        assertNotNull(spCache);
        assertNotNull(cacheMgr.getCache(getTestCacheName(), spConfiguration.getKeyType(),
                                        spConfiguration.getValueType()));
        assertEquals(getTestCacheName(), spCache.getName());

        Map<String, Point> entries = new HashMap<String, Point>();

        entries.put("one", new Point(1, 1));
        entries.put("two", new Point(2, 2));
        entries.put("three", new Point(3, 3));

        spCache.putAll(entries);
        Assert.assertEquals(3, listener.getCreated());

        for (Map.Entry<String, Point> entry : entries.entrySet())
            {
            Assert.assertTrue(spCache.containsKey(entry.getKey()));
            Assert.assertEquals(entry.getValue(), spCache.get(entry.getKey()));
            }

        cacheMgr.destroyCache(getTestCacheName());
        }

    /**
     * JCache cache content lifecycle test.
     *
     * Validate access to previously created and closed Coherence JCache cache when the cache is a distributed or remote cache.
     */
    @Test
    public void createPutCloseCreateCache()
        {
        Logger.info("start createPutCloseGetCache");

        spCache         = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), spConfiguration);

        assertNotNull(spCache);
        assertNotNull(cacheMgr.getCache(getTestCacheName(), spConfiguration.getKeyType(),
                spConfiguration.getValueType()));
        assertEquals(getTestCacheName(), spCache.getName());

        Map<String, Point> entries = new HashMap<String, Point>();

        entries.put("one", new Point(1, 1));
        entries.put("two", new Point(2, 2));
        entries.put("three", new Point(3, 3));

        spCache.putAll(entries);

        for (Map.Entry<String, Point> entry : entries.entrySet())
            {
            Assert.assertTrue(spCache.containsKey(entry.getKey()));
            Assert.assertEquals(entry.getValue(), spCache.get(entry.getKey()));
            }

        spCache.close();
        assertTrue(spCache.isClosed());

        spCache = cacheMgr.createCache(getTestCacheName(), spConfiguration);
        assertFalse(spCache.isClosed());

        for (Map.Entry<String, Point> entry : entries.entrySet())
            {
            Assert.assertTrue(spCache.containsKey(entry.getKey()));
            Assert.assertEquals(entry.getValue(), spCache.get(entry.getKey()));
            }

        spCache.close();
        assertTrue(spCache.isClosed());

        // non-destroyed cache content is even preserved by a create
        spCache = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), spConfiguration);

        for (Map.Entry<String, Point> entry : entries.entrySet())
            {
            Assert.assertTrue(spCache.containsKey(entry.getKey()));
            Assert.assertEquals(entry.getValue(), spCache.get(entry.getKey()));
            }

        cacheMgr.destroyCache(getTestCacheName());

        spCache = cacheMgr.getCache(getTestCacheName(), spConfiguration.getKeyType(), spConfiguration.getValueType());
        Assert.assertNull("cache " + getTestCacheName() + " should be null after destroyCache", spCache);

        spCache = cacheMgr.createCache(getTestCacheName(), spConfiguration);
        Assert.assertFalse("createPutCloseGet: assert no remaining content after destroyCache of " + getTestCacheName(), spCache.iterator().hasNext());

        cacheMgr.destroyCache(getTestCacheName());
        }


    /**
     * JCache cache content lifecycle test.
     *
     * Validate access to previously created and closed Coherence JCache cache when the cache is a distributed or remote cache.
     */
    @Test
    public void createPutCloseGetCache()
        {
        Logger.info("start createPutCloseGetCache");

        spCache         = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), spConfiguration);

        if (spCache instanceof LocalCache)
            {
            // disable this test for LocalCache since its lifecycle does not allow for a getCache after a cache is closed.
            return;
            }

        assertNotNull(spCache);
        assertNotNull(cacheMgr.getCache(getTestCacheName(), spConfiguration.getKeyType(),
                spConfiguration.getValueType()));
        assertEquals(getTestCacheName(), spCache.getName());

        Map<String, Point> entries = new HashMap<String, Point>();

        entries.put("one", new Point(1, 1));
        entries.put("two", new Point(2, 2));
        entries.put("three", new Point(3, 3));

        spCache.putAll(entries);

        for (Map.Entry<String, Point> entry : entries.entrySet())
            {
            Assert.assertTrue(spCache.containsKey(entry.getKey()));
            Assert.assertEquals(entry.getValue(), spCache.get(entry.getKey()));
            }

        spCache.close();
        assertTrue(spCache.isClosed());

        spCache = cacheMgr.getCache(getTestCacheName(), spConfiguration.getKeyType(), spConfiguration.getValueType());
        assertFalse(spCache.isClosed());

        for (Map.Entry<String, Point> entry : entries.entrySet())
            {
            Assert.assertTrue(spCache.containsKey(entry.getKey()));
            Assert.assertEquals(entry.getValue(), spCache.get(entry.getKey()));
            }

        spCache.close();
        assertTrue(spCache.isClosed());

        // non-destroyed cache content is even preserved by a create
        spCache = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), spConfiguration);

        for (Map.Entry<String, Point> entry : entries.entrySet())
            {
            Assert.assertTrue(spCache.containsKey(entry.getKey()));
            Assert.assertEquals(entry.getValue(), spCache.get(entry.getKey()));
            }

        cacheMgr.destroyCache(getTestCacheName());

        spCache = cacheMgr.getCache(getTestCacheName(), spConfiguration.getKeyType(), spConfiguration.getValueType());
        Assert.assertNull("cache " + getTestCacheName() + " should be null after destroyCache", spCache);

        spCache = cacheMgr.createCache(getTestCacheName(), spConfiguration);
        Assert.assertFalse("createPutCloseGet: assert no remaining content after destroyCache of " + getTestCacheName(), spCache.iterator().hasNext());

        cacheMgr.destroyCache(getTestCacheName());
        }


    /**
     * Regression test for COH-12332
     */
    @Test
    public void accessStatistics_regressionCOH12332()
            throws MalformedObjectNameException, AttributeNotFoundException, MBeanException, ReflectionException,
            InstanceNotFoundException, InterruptedException
        {
        Logger.info("start accessStatistics_regressionCOH12332");

        if (ssConfiguration instanceof CoherenceBasedCompleteConfiguration)
            {
            ((CoherenceBasedCompleteConfiguration) ssConfiguration).setStatisticsEnabled(true);
            ((CoherenceBasedCompleteConfiguration) ssConfiguration).setManagementEnabled(true);

            }

        else if (ssConfiguration instanceof MutableConfiguration)
            {
            ((MutableConfiguration) ssConfiguration).setStatisticsEnabled(true);
            ((MutableConfiguration) ssConfiguration).setManagementEnabled(true);
            }

        Cache<String, String> cache = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(),
                ssConfiguration);

        if (cache instanceof LocalCache)
            {
            return;
            }

        assertTrue(((AbstractCoherenceBasedCache) cache).isStatisticsEnabled());

        CacheStatisticsMXBean bean = cache.unwrap(AbstractCoherenceBasedCache.class).getStatistics();

        System.out.println("Beginning Cache Statistics = " + bean.toString());

        System.out.println("CacheMisses expect 0, observe " + bean.getCacheMisses());

        cache.containsKey("missing");
        assertEquals(0, bean.getCacheMisses());
        cache.put("hello", "one");
        assertEquals(1, bean.getCachePuts());

        cache.get("missing");
        assertEquals(1, bean.getCacheMisses());
        assertEquals(0, bean.getCacheHits());

        Map<String, String> map = new HashMap<String, String>();

        map.put("hello", "one");
        map.put("world", "two");
        map.put("good", "three");
        map.put("bye", "four");
        map.put("seed", "five");
        cache.putAll(map);

        /******* Begin regression testing: test that statistics mbean stays around after close and get of the cache. */
        cache.close();
        assertTrue(cache.isClosed());

        /** Access MBean while cache is closed. */
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName  sNameJCacheStatisticsMBean = MBeanServerRegistrationUtility.calculateObjectName(cache, MBeanServerRegistrationUtility.ObjectNameType.Statistics);

        System.out.println("JMXBean: " + sNameJCacheStatisticsMBean + " CacheHits="
                + mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheHits"));

        assertEquals("Verify CacheStatisticsMXBean attribute CacheHits", 0L,
                mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheHits"));
        assertEquals("Validate CacheStatisticsMXBean attribute CachePuts", 6L,
                mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CachePuts"));
        assertEquals("Validate CacheStatisticsMXBean attribute CacheMisses", 1L,
                mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheMisses"));
        assertEquals("Validate CacheStatisticsMXBean attribute CacheRemovals", 0L,
                mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheRemovals"));

        /** Assert that when one gets the cache again, that all JCache CacheStatistics are not reset. */
        cache = cacheMgr.getCache(getTestCacheName(), String.class, String.class);
        assertNotNull(cache);
        bean = cache.unwrap(AbstractCoherenceBasedCache.class).getStatistics();
        System.out.println("2) hits=" + bean.getCacheHits() + " misses=" + bean.getCacheMisses() + " gets="
                + bean.getCacheGets() + " puts=" + bean.getCachePuts() + " removes="
                + bean.getCacheRemovals());

        assertEquals("Verify CacheStatisticsMXBean attribute CacheHits", 0L, bean.getCacheHits());
        assertEquals("Validate CacheStatisticsMXBean attribute CachePuts", 6L,
                bean.getCachePuts());
        assertEquals("Validate CacheStatisticsMXBean attribute CacheMisses", 1L,
                bean.getCacheMisses());
        assertEquals("Validate CacheStatisticsMXBean attribute CacheRemovals", 0L,
                bean.getCacheRemovals());

        assertEquals("Verify CacheStatisticsMXBean attribute CacheHits", 0L,
                mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheHits"));
        assertEquals("Validate CacheStatisticsMXBean attribute CachePuts", 6L,
                mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CachePuts"));
        assertEquals("Validate CacheStatisticsMXBean attribute CacheMisses", 1L,
                mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheMisses"));
        assertEquals("Validate CacheStatisticsMXBean attribute CacheRemovals", 0L,
                mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheRemovals"));

        cache.get("missingAgain");
        assertEquals(2L, mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheMisses"));
        assertEquals(0L, mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheHits"));

        cache.put("world", "two");
        cache.get("hello");
        assertEquals(2L, mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheMisses"));
        assertEquals(1L, mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheHits"));

        System.out.println("JMXBean: " + sNameJCacheStatisticsMBean + " CacheHits="
                + mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheHits"));

        assertEquals("Verify CacheStatisticsMXBean attribute CacheHits", 1L,
                mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheHits"));
        assertEquals("Validate CacheStatisticsMXBean attribute CachePuts", 7L,
                mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CachePuts"));
        assertEquals("Validate CacheStatisticsMXBean attribute CacheMisses", 2L,
                mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheMisses"));
        assertEquals("Validate CacheStatisticsMXBean attribute CacheRemovals", 0L,
                mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheRemovals"));

        cache.remove("hello");

        assertEquals("Validate CacheStatisticsMXBean attribute CacheRemovals", 1L,
                mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheRemovals"));
        assertEquals("Validate CacheStatisticsMXBean attribute CacheRemovals", 1L,
                bean.getCacheRemovals());

        Set keys = new HashSet();

        keys.add("hello");
        keys.add("world");
        keys.add("bye");
        keys.add("good");
        keys.add("non-existent");
        keys.add("missing");
        cache.removeAll(keys);
        assertEquals(4L, mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheRemovals"));
        assertEquals(4L, bean.getCacheRemovals());

        cache.close();
        assertEquals("JCache Statistics MBean still available after close", 4L, mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CacheRemovals"));


        System.out.println("Final Cache Statistics = " + bean.toString());
        assertTrue(mBeanServer.isRegistered(sNameJCacheStatisticsMBean));

        cacheMgr.destroyCache(getTestCacheName());
        assertFalse(mBeanServer.isRegistered(sNameJCacheStatisticsMBean));
        }

    /** Minimal regression test rather than a revision of existing accessStatistics() test.
     */
    @Test
    public void testCOH12332() throws AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException
        {
        // setup cache with JCache Statistics MBEAN enabled
        Logger.info("start testCOH12332");

        if (ssConfiguration instanceof CoherenceBasedCompleteConfiguration)
            {
            ((CoherenceBasedCompleteConfiguration) ssConfiguration).setStatisticsEnabled(true);
            ((CoherenceBasedCompleteConfiguration) ssConfiguration).setManagementEnabled(true);

            }

        else if (ssConfiguration instanceof MutableConfiguration)
            {
            ((MutableConfiguration) ssConfiguration).setStatisticsEnabled(true);
            ((MutableConfiguration) ssConfiguration).setManagementEnabled(true);
            }

        Cache<String, String>   cache = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(),
                ssConfiguration);

        if (cache instanceof LocalCache)
            {
            return;
            }

        MBeanServer             mBeanServer                   = ManagementFactory.getPlatformMBeanServer();
        ObjectName              sNameJCacheStatisticsMBean    = MBeanServerRegistrationUtility.calculateObjectName(cache, MBeanServerRegistrationUtility.ObjectNameType.Statistics);
        ObjectName              sNameJCacheConfigurationMBean = MBeanServerRegistrationUtility.calculateObjectName(cache, MBeanServerRegistrationUtility.ObjectNameType.Configuration);


        assertTrue(mBeanServer.isRegistered(sNameJCacheStatisticsMBean));
        assertTrue(mBeanServer.isRegistered(sNameJCacheConfigurationMBean));

        assertTrue(cache.unwrap(AbstractCoherenceBasedCache.class).isStatisticsEnabled());

        CacheStatisticsMXBean bean = cache.unwrap(AbstractCoherenceBasedCache.class).getStatistics();

        System.out.println("Beginning Cache Statistics = " + bean.toString());

        cache.put("hello", "one");
        assertEquals(1, bean.getCachePuts());


        /******* Begin regression testing: test that statistics mbean stays around after close and recreate of the cache. */
        assertFalse(cache.isClosed());
        cache.close();
        assertTrue(cache.isClosed());
        assertTrue(mBeanServer.isRegistered(sNameJCacheStatisticsMBean));
        assertTrue(mBeanServer.isRegistered(sNameJCacheConfigurationMBean));

        // get attribute from JCache MBean while cache is closed.
        assertEquals(1L, mBeanServer.getAttribute(sNameJCacheStatisticsMBean, "CachePuts"));

        // ensure getting the cache back still maintains previous attribute value.
        cache = cacheMgr.getCache(getTestCacheName(), ssConfiguration.getKeyType(), ssConfiguration.getValueType());
        bean = cache.unwrap(AbstractCoherenceBasedCache.class).getStatistics();
        assertEquals(1, bean.getCachePuts());

        cacheMgr.destroyCache(getTestCacheName());
        assertFalse(mBeanServer.isRegistered(sNameJCacheStatisticsMBean));
        assertFalse(mBeanServer.isRegistered(sNameJCacheConfigurationMBean));
        }

    /**
     * Method description
     */

    @Test
    public void createAndClearCacheWithSimpleValues()
        {
        Logger.info("start createAndClearCacheWithSimpleValues");

        ssConfiguration = extraSetup(ssConfiguration);
        ssCache         = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), ssConfiguration);
        assertNotNull(ssCache);
        assertNotNull(cacheMgr.getCache(getTestCacheName(), String.class, String.class));
        assertEquals(getTestCacheName(), ssCache.getName());

        Assert.assertEquals(0, listener.getCreated());
        Assert.assertEquals(0, listener.getUpdated());
        Assert.assertEquals(0, listener.getExpired());
        Assert.assertEquals(0, listener.getRemoved());
        Assert.assertNotNull(ssCache);

        ssCache.put("firstKey", "firstValue");
        Assert.assertEquals(1, listener.getCreated());
        ssCache.clear();
        Assert.assertEquals(0, listener.getRemoved());

        ssCache.put("firstKey", "anotherFirstValue");
        Assert.assertEquals(2, listener.getCreated());
        ssCache.remove("firstKey");
        Eventually.assertThat(invoking(listener).getRemoved(), is(1));

        Map<String, String> entries = new HashMap<String, String>();

        entries.put("secondKey", "value2");
        entries.put("thirdKey", "value3");
        entries.put("fourthKey", "value4");
        entries.put("fifthKey", "value5");
        ssCache.putAll(entries);

        for (Map.Entry<String, String> entry : entries.entrySet())
            {
            Assert.assertTrue(ssCache.containsKey(entry.getKey()));
            Assert.assertEquals(entry.getValue(), ssCache.get(entry.getKey()));
            }

        Assert.assertEquals(6, listener.getCreated());

        Assert.assertEquals(1, listener.getRemoved());
        ssCache.clear();
        Assert.assertEquals(1, listener.getRemoved());

        Assert.assertFalse(ssCache.containsKey("firstKey"));
        cacheMgr.destroyCache(getTestCacheName());
        assertNull(cacheMgr.getCache(getTestCacheName()));
        ssCache = null;
        }

    /**
     * test JCache cache with long key and string value.
     */
    @Test
    public void createAndAccessCacheWithLongKeyStringValues()
        {
        Logger.info("start createAndAccessCacheWithLongKeyStringValues");

        long now = System.currentTimeMillis();

        lsConfiguration = extraSetup(lsConfiguration);
        lsCache         = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), lsConfiguration);
        assertNotNull(lsCache);
        assertNotNull(cacheMgr.getCache(getTestCacheName(), lsConfiguration.getKeyType(),
                                        lsConfiguration.getValueType()));
        assertEquals(getTestCacheName(), lsCache.getName());

        Assert.assertEquals(0, listener.getCreated());
        Assert.assertEquals(0, listener.getUpdated());
        Assert.assertEquals(0, listener.getExpired());
        Assert.assertEquals(0, listener.getRemoved());

        long key = now + 1L;

        lsCache.put(key, "value" + key);
        Assert.assertEquals(1, listener.getCreated());

        String value = lsCache.get(key);

        Assert.assertEquals("value" + key, value);
        Assert.assertTrue(lsCache.containsKey(key));
        Assert.assertFalse(lsCache.containsKey(now));

        lsCache.put(key, "updatedValue" + key);

        Assert.assertEquals(1, listener.getUpdated());
        value = lsCache.get(key);
        Assert.assertEquals("updatedValue" + key, value);

        lsCache.put(key, "updatedValueDif" + key);

        Assert.assertEquals(2, listener.getUpdated());
        Assert.assertTrue(lsCache.remove(key));
        Assert.assertEquals(1, listener.getRemoved());
        Assert.assertFalse(lsCache.remove(key));
        Assert.assertEquals(1, listener.getRemoved());
        Assert.assertFalse(lsCache.containsKey(key));

        lsCache.put(key, "updatedValueAgain" + key);
        Assert.assertEquals(2, listener.getCreated());
        Assert.assertEquals(2, listener.getUpdated());
        Assert.assertTrue(lsCache.containsKey(key));
        Assert.assertEquals(1, listener.getRemoved());

        lsCache.clear();
        Assert.assertEquals(1, listener.getRemoved());
        Assert.assertFalse(lsCache.containsKey(key));
        Assert.assertEquals(2, listener.getUpdated());

        Map<Long, String> entries = new HashMap<Long, String>();

        for (Long ii = 1L; ii < 5L; ii++)
            {
            key = now + ii;
            entries.put(key, "value" + key);
            }

        lsCache.putAll(entries);
        Assert.assertEquals(6, listener.getCreated());

        Assert.assertEquals(2, listener.getUpdated());

        for (int i = 1; i < 5; i++)
            {
            key = now + i;
            Assert.assertEquals("value" + key, lsCache.get(key));
            }

        for (Map.Entry<Long, String> entry : entries.entrySet())
            {
            Assert.assertEquals(entry.getValue(), lsCache.get(entry.getKey()));
            }

        lsCache.clear();
        Assert.assertEquals(1, listener.getRemoved());
        Assert.assertFalse(lsCache.containsKey(now + 1));

        cacheMgr.destroyCache(getTestCacheName());
        assertNull(cacheMgr.getCache(getTestCacheName()));
        lsCache = null;
        }

    /**
     * test JCache cache with complex value.
     */
    @Test
    public void createAndAccessCacheWithComplexValues()
        {
        Logger.info("start createAndAccessCacheWithComplexValues");

        snpConfiguration = extraSetup(snpConfiguration);
        snpCache         = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), snpConfiguration);
        assertNotNull(snpCache);
        assertNotNull(cacheMgr.getCache(getTestCacheName(), snpConfiguration.getKeyType(),
                                        snpConfiguration.getValueType()));
        assertEquals(getTestCacheName(), snpCache.getName());

        snpCache.put("startPoint", new NonPofPoint(1, 2));

        NonPofPoint value = snpCache.get("startPoint");

        System.out.println("ComplexValue =" + value);
        Assert.assertEquals(value.x, 1);
        Assert.assertEquals(value.y, 2);
        Assert.assertTrue(snpCache.containsKey("startPoint"));
        Assert.assertFalse(snpCache.containsKey("unknownKey"));
        Assert.assertTrue(snpCache.remove("startPoint"));
        Assert.assertFalse(snpCache.remove("startPoint"));
        Assert.assertFalse(snpCache.containsKey("startPoint"));
        snpCache.put("startPoint", new NonPofPoint(3, 4));
        value = snpCache.get("startPoint");
        System.out.println("ComplexValue =" + value);
        Assert.assertEquals(value.x, 3);
        Assert.assertEquals(value.y, 4);
        snpCache.clear();
        Assert.assertFalse(snpCache.containsKey("startPoint"));
        cacheMgr.destroyCache(getTestCacheName());
        assertNull(cacheMgr.getCache(getTestCacheName()));
        snpCache = null;

        Configuration<String, Set<NonPofPoint>> config2 = getConfiguration();

        cacheMgr.createCache(getTestCacheName(), config2);

        Cache<String, Set<NonPofPoint>> cache2 = cacheMgr.getCache(getTestCacheName(), config2.getKeyType(),
                                                     config2.getValueType());

        Set<NonPofPoint> line = new TreeSet<NonPofPoint>();

        line.add(new NonPofPoint(5, 6));
        line.add(new NonPofPoint(7, 8));
        cache2.put("line", line);

        // TODO: code review question:  this code used to be next line before Pof conversion, had to change to next line.
        // just want to understand what is going on better.
        // Set<NonPofPoint> result = cache2.get("line");
        Set<NonPofPoint> result = new TreeSet<NonPofPoint>(cache2.get("line"));

        Assert.assertTrue(result.size() == 2);
        Assert.assertTrue(result.contains(new NonPofPoint(7, 8)));
        Assert.assertTrue(result.contains(new NonPofPoint(5, 6)));
        cacheMgr.destroyCache(cache2.getName());
        }

    /**
     * Test JCache with value with PoF serializer.
     */
    @Test
    public void createAndAccessCacheWithPofComplexValues()
        {
        Logger.info("start createAndAccessCacheWithPofComplexValues");

        spConfiguration = extraSetup(spConfiguration);
        spCache         = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), spConfiguration);
        assertNotNull(spCache);
        assertNotNull(cacheMgr.getCache(getTestCacheName(), spConfiguration.getKeyType(),
                                        spConfiguration.getValueType()));
        assertEquals(getTestCacheName(), spCache.getName());

        try
            {
            spCache.put("startPoint", new Point(1, 2));
            }
        catch (CacheException e)
            {
            if (!getJcacheTestContext().supportsPof() && e.getCause() != null && e.getCause() instanceof IOException)
                {
                // pof is only serialization supported for Point. So this test is not valid so just return.
                return;
                }
            else
                {
                throw e;
                }
            }

        Point value = spCache.get("startPoint");

        System.out.println("ComplexValue =" + value);
        Assert.assertEquals(value.x, 1);
        Assert.assertEquals(value.y, 2);
        Assert.assertTrue(spCache.containsKey("startPoint"));
        Assert.assertFalse(spCache.containsKey("unknownKey"));
        Assert.assertTrue(spCache.remove("startPoint"));
        Assert.assertFalse(spCache.remove("startPoint"));
        Assert.assertFalse(spCache.containsKey("startPoint"));
        spCache.put("startPoint", new Point(3, 4));
        value = spCache.get("startPoint");
        Assert.assertNotNull(value);
        System.out.println("ComplexValue =" + value);
        Assert.assertEquals(value.x, 3);
        Assert.assertEquals(value.y, 4);
        spCache.clear();
        Assert.assertFalse(spCache.containsKey("startPoint"));
        cacheMgr.destroyCache(getTestCacheName());
        spCache = null;

        Configuration<String, Set<Point>> config2 = getConfiguration();
        Cache<String, Set<Point>> cache2 = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), config2);

        Set<Point>                        line    = new TreeSet<Point>();

        line.add(new Point(5, 6));
        line.add(new Point(7, 8));
        cache2.put("line", line);

        Set<Point> result = new TreeSet<Point>(cache2.get("line"));

        Assert.assertTrue(result.size() == 2);
        Assert.assertTrue(result.contains(new Point(7, 8)));
        Assert.assertTrue(result.contains(new Point(5, 6)));
        cacheMgr.destroyCache(cache2.getName());
        }

    /**
     * Regression unit test for JCACHE-83.
     */
    @Test
    public void customConfigurationWithNullExpiryFactory()
        {
        Logger.info("start customConfigurationWithNullExpiryFactory");

        CompleteConfiguration<String, String> config = getConfiguration();

        // due to JCache MutableConfiguration not having an interface that CoherenceBasedCompleteConfiguration implements.
        if (config instanceof CoherenceBasedCompleteConfiguration)
            {
            ((CoherenceBasedCompleteConfiguration) config).setExpiryPolicyFactory(null);
            }
        else
            {
            ((MutableConfiguration) config).setExpiryPolicyFactory(null);
            }

        Cache cache = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), config);

        assertNotNull(cache != null);
        assertNotNull(cacheMgr.getCache(getTestCacheName(), config.getKeyType(), config.getValueType()));
        cacheMgr.destroyCache(getTestCacheName());

        }

    /**
     * Test getAndReplace does not require server side class.
     */
    @Test
    public void jcache96Regression()
        {
        Logger.info("start jcache96Regression");

        spCache = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), spConfiguration);

        assertNotNull(spCache);
        assertNotNull(cacheMgr.getCache(getTestCacheName(), spConfiguration.getKeyType(),
                                        spConfiguration.getValueType()));
        assertEquals(getTestCacheName(), spCache.getName());

        Point toBeReplaced = new Point(1, 2);

        spCache.put("startPoint", toBeReplaced);

        Point replacement   = new Point(2, 3);

        Point previousPoint = spCache.getAndReplace("startPoint", replacement);

        assertTrue(previousPoint.equals(toBeReplaced));
        assertTrue(spCache.get("startPoint" + "").equals(replacement));
        cacheMgr.destroyCache(getTestCacheName());
        }

    /**
     * Test getAndReplace does not require server side class.
     */
    @Test
    public void jcache103Regression()
        {
        Logger.info("start jcache103Regression");

        spCache = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), spConfiguration);

        assertNotNull(spCache);
        assertNotNull(cacheMgr.getCache(getTestCacheName(), spConfiguration.getKeyType(),
                                        spConfiguration.getValueType()));
        assertEquals(getTestCacheName(), spCache.getName());

        Point toBeReplaced = new Point(1, 2);

        spCache.put("startPoint", toBeReplaced);

        Point   replacement = new Point(2, 3);

        boolean fReplaced   = spCache.replace("startPoint", replacement);

        assertTrue(fReplaced);
        assertTrue(spCache.get("startPoint" + "").equals(replacement));
        cacheMgr.destroyCache(getTestCacheName());
        }

    /**
     * Test putIfAbsent does not require server side class.
     */
    @Test
    public void jcache102Regression()
        {
        Logger.info("start jcache102Regression");

        spCache = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), spConfiguration);

        assertNotNull(spCache);
        assertNotNull(cacheMgr.getCache(getTestCacheName(), spConfiguration.getKeyType(),
                                        spConfiguration.getValueType()));
        assertEquals(getTestCacheName(), spCache.getName());

        Point   point   = new Point(1, 2);

        boolean fResult = spCache.putIfAbsent("startPoint", point);

        assertTrue(fResult);
        assertTrue(point.equals(spCache.get("startPoint")));
        cacheMgr.destroyCache(getTestCacheName());
        }

    /**
     * JCache TCK works around this issue by always having at least one Listener specified in Configuration
     * before dynamically registering a second Listener.
     *
     * Test explicitly use of dynamic listener registration.
     */
    @Test
    public void dynamicCacheEntryListenerRegistration_regressionCOH11488()
        {
        Logger.info("start dynamicCacheEntryListenerRegistration_regressionCOH11488");

        listener = new CacheListenerTests.MyCacheEntryListener();

        Cache<String, String> cache = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(),
                                          ssConfiguration);
        CacheEntryListenerConfiguration synchronousListenerConfiguration =
            new MutableCacheEntryListenerConfiguration(FactoryBuilder.factoryOf(listener), null, false, true);

        cache.registerCacheEntryListener(synchronousListenerConfiguration);

        assertEquals(0, listener.getCreated());
        assertEquals(0, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(0, listener.getRemoved());

        cache.put("one", "Sooty");
        assertEquals(1, listener.getCreated());
        assertEquals(0, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(0, listener.getRemoved());

        cache.put("one", "Booty");
        assertEquals(1, listener.getCreated());
        assertEquals(1, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(0, listener.getRemoved());

        cache.remove("one");
        assertEquals(1, listener.getCreated());
        assertEquals(1, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(1, listener.getRemoved());

        cache.put("one", "Sooty");
        assertEquals(2, listener.getCreated());
        assertEquals(1, listener.getUpdated());
        assertEquals(0, listener.getExpired());
        assertEquals(1, listener.getRemoved());

        cache.deregisterCacheEntryListener(synchronousListenerConfiguration);
        cache.put("three", "Sooty");
        assertEquals(2, listener.getCreated());

        CacheEntryListenerConfiguration asynchronousListenerConfiguration =
            new MutableCacheEntryListenerConfiguration(FactoryBuilder.factoryOf(listener), null, false, false);

        cache.registerCacheEntryListener(asynchronousListenerConfiguration);
        cache.put("four", "sooty");
        Eventually.assertThat(invoking(listener).getCreated(), is(3));

        cache.deregisterCacheEntryListener(asynchronousListenerConfiguration);
        cache.put("five", "sooty");
        assertEquals(3, listener.getCreated());

        cacheMgr.destroyCache(getTestCacheName());
        }

    @Test
    public void accessStatistics()
            throws MalformedObjectNameException, AttributeNotFoundException, MBeanException, ReflectionException,
                   InstanceNotFoundException, InterruptedException
        {
        Logger.info("start accessStatistics");

        if (ssConfiguration instanceof CoherenceBasedCompleteConfiguration)
            {
            ((CoherenceBasedCompleteConfiguration) ssConfiguration).setStatisticsEnabled(true);
            ((CoherenceBasedCompleteConfiguration) ssConfiguration).setManagementEnabled(true);

            }

        else if (ssConfiguration instanceof MutableConfiguration)
            {
            ((MutableConfiguration) ssConfiguration).setStatisticsEnabled(true);
            ((MutableConfiguration) ssConfiguration).setManagementEnabled(true);
            }

        Cache<String, String> cache = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(),
                                          ssConfiguration);

        assertTrue(((AbstractCoherenceBasedCache) cache).isStatisticsEnabled());

        CacheStatisticsMXBean bean = ((AbstractCoherenceBasedCache) cache).getStatistics();

        System.out.println("Beginning Cache Statistics = " + bean.toString());

        System.out.println("CacheMisses expect 0, observe " + bean.getCacheMisses());

        cache.containsKey("missing");
        assertEquals(0, bean.getCacheMisses());
        cache.put("hello", "one");
        assertEquals(1, bean.getCachePuts());

        cache.get("missing");
        assertEquals(1, bean.getCacheMisses());
        assertEquals(0, bean.getCacheHits());

        Map<String, String> map = new HashMap<String, String>();

        map.put("hello", "one");
        map.put("world", "two");
        map.put("good", "three");
        map.put("bye", "four");
        map.put("seed", "five");
        cache.putAll(map);
        assertEquals(6, bean.getCachePuts());

        System.out.println("1) hits=" + bean.getCacheHits() + " misses=" + bean.getCacheMisses() + " gets="
                           + bean.getCacheGets() + " puts=" + bean.getCachePuts() + " removes="
                           + bean.getCacheRemovals());

        cache.get("missingAgain");
        assertEquals(2, bean.getCacheMisses());
        assertEquals(0, bean.getCacheHits());

        cache.put("world", "two");
        cache.get("hello");
        assertEquals(2, bean.getCacheMisses());
        assertEquals(1, bean.getCacheHits());

        Set<ObjectName> registeredObjectNames = null;
        MBeanServer     mBeanServer           = ManagementFactory.getPlatformMBeanServer();

        ObjectName objectName = new ObjectName("javax.cache:type=CacheStatistics" + ",CacheManager="
                                    + (cache.getCacheManager().getURI().toString()) + ",Cache=" + cache.getName());

        System.out.println("JMXBean: " + objectName + " CacheHits="
                           + mBeanServer.getAttribute(objectName, "CacheHits"));

        assertEquals("Verify CacheStatisticsMXBean attribute CacheHits", 1L,
                     mBeanServer.getAttribute(objectName, "CacheHits"));
        assertEquals("Validate CacheStatisticsMXBean attribute CachePuts", 7L,
                     mBeanServer.getAttribute(objectName, "CachePuts"));
        assertEquals("Validate CacheStatisticsMXBean attribute CacheMisses", 2L,
                     mBeanServer.getAttribute(objectName, "CacheMisses"));
        assertEquals("Validate CacheStatisticsMXBean attribute CacheRemovals", 0L,
                     mBeanServer.getAttribute(objectName, "CacheRemovals"));

        cache.remove("hello");

        assertEquals("Validate CacheStatisticsMXBean attribute CacheRemovals", 1L,
        mBeanServer.getAttribute(objectName, "CacheRemovals"));

        bean = ((AbstractCoherenceBasedCache) cache).getStatistics();

        System.out.println("2)hits=" + bean.getCacheHits() + " misses=" + bean.getCacheMisses() + " gets="
                           + bean.getCacheGets() + " puts=" + bean.getCachePuts() + " removes="
                           + bean.getCacheRemovals());

        // cache.removeAll();
        Set keys = new HashSet();

        keys.add("hello");
        keys.add("world");
        keys.add("bye");
        keys.add("good");
        keys.add("non-existent");
        keys.add("missing");
        cache.removeAll(keys);
        assertEquals(4, bean.getCacheRemovals());
        System.out.println("Final Cache Statistics = " + bean.toString());
        cacheMgr.destroyCache(getTestCacheName());
        }

    /**
     * Method description
     */
    protected void setupTest()
        {
        }

    /**
     * Method description
     */
    protected void cleanupAfterTest()
        {
        if (cacheMgr != null)
            {
            for (String name : cacheMgr.getCacheNames())
                {
                cacheMgr.destroyCache(name);
                }
            }
        }

    /**
     * loadAll test
     *
     * @throws Exception
     */
    @Test
    public void loadAll_1Found1Not()
            throws Exception
        {
        Logger.info("start loadAll_1Found1Not");

        loader = new SimpleCacheLoader<Integer>();

        if (iiConfiguration instanceof CoherenceBasedCompleteConfiguration)
            {
            ((CoherenceBasedCompleteConfiguration) iiConfiguration).setCacheLoaderFactory(FactoryBuilder.factoryOf(
                loader));
            ((CoherenceBasedCompleteConfiguration) iiConfiguration).setReadThrough(true);
            }
        else if (iiConfiguration instanceof MutableConfiguration)
            {
            ((MutableConfiguration) iiConfiguration).setCacheLoaderFactory(FactoryBuilder.factoryOf(loader));
            ((MutableConfiguration) iiConfiguration).setReadThrough(true);
            }

        iiCache = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), iiConfiguration);
        assertNotNull(iiCache);
        assertNotNull(cacheMgr.getCache(getTestCacheName(), iiConfiguration.getKeyType(),
                                        iiConfiguration.getValueType()));
        assertEquals(getTestCacheName(), iiCache.getName());

        Integer keyThere = 1;

        iiCache.put(keyThere, keyThere);

        Integer          keyNotThere = keyThere + 1;
        HashSet<Integer> keys        = new HashSet<Integer>();

        keys.add(keyThere);
        keys.add(keyNotThere);

        CompletionListenerFuture future = new CompletionListenerFuture();

        iiCache.loadAll(keys, false, future);
        future.get(30, TimeUnit.SECONDS);
        assertTrue(future.isDone());

        assertEquals(1, loader.getLoadCount());
        assertTrue(loader.hasLoaded(keyNotThere));
        assertEquals(keyThere, iiCache.get(keyThere));
        assertEquals(keyNotThere, iiCache.get(keyNotThere));
        cacheMgr.destroyCache(getTestCacheName());
        iiCache = null;
        }

    /**
     * Method description
     */
    @Test
    public void getTypedCacheWithoutTypedGetCache()
        {
        Logger.info("start getTypedCacheWithoutTypedGetCache");

        iiCache = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), iiConfiguration);

        try
            {
            cacheMgr.getCache(getTestCacheName());
            }
        finally
            {
            cacheMgr.destroyCache(getTestCacheName());
            iiCache = null;
            }
        }

    @SuppressWarnings({"unchecked"})
    @Test
    public void createAndAccessCollectionsTest()
        {
        Logger.info("start createAndAccessCollectionsTest");

        slCache = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), slConfiguration);

        // Create a sample collection
        List list = new ArrayList();

        for (int i = 0; i < 5; i++)
            {
            list.add(String.valueOf(i));
            }

        slCache.put("list", list);

        List listFromCache = slCache.get("list");
        List correctType   = new ArrayList(listFromCache);

        System.out.println("Type of list put in cache: " + list.getClass());
        System.out.println("Type of list in cache: " + listFromCache.getClass());

        try
            {
            assertEquals(list.getClass(), correctType.getClass());

            boolean pofEnabled = Boolean.getBoolean("tangosol.pof.enabled");

            if (!pofEnabled)
                {
                assertEquals(list.getClass(), listFromCache.getClass());
                }
            }
        finally
            {
            cacheMgr.destroyCache(getTestCacheName());
            }
        }

    @SuppressWarnings({"unchecked"})
    @Test
    public void createAndAccessMapTest()
        {
        Logger.info("start createAndAccessMapTest");

        smCache = getJcacheTestContext().configureCache(cacheMgr, getTestCacheName(), smConfiguration);

        Map map = new TreeMap();

        for (int i = 0; i < 5; i++)
            {
            String value = String.valueOf(i);

            map.put(value, value);
            }

        smCache.put("map", map);

        Map mapFromCache = (Map) smCache.get("map");

        System.out.println("Type of map put in cache: " + map.getClass());
        System.out.println("Type of map in cache: " + mapFromCache.getClass());

        boolean pofEnabled = Boolean.getBoolean("tangosol.pof.enabled");

        if (!pofEnabled)
            {
            assertEquals(map.getClass(), mapFromCache.getClass());
            }

        cacheMgr.destroyCache(getTestCacheName());

        }

    /**
     * A simple example of a Cache Loader which simply adds the key as the value.
     *
     * @param <K>
     */
    public static class SimpleCacheLoader<K>
            implements CacheLoader<K, K>, Serializable
        {
        /**
         * Constructs ...
         *
         */
        public SimpleCacheLoader()
            {
            }

        /**
         * For Pof Serializer
         * @param count
         * @param map
         */
        SimpleCacheLoader(int count, Map map)
            {
            this.loadCount.set(count);
            this.loaded.putAll(map);
            }

        /**
         * Method description
         *
         * @param key
         *
         * @return
         */
        @Override
        public K load(final K key)
            {
            loaded.put(key, key);

            return key;
            }

        /**
         * Method description
         *
         * @param keys
         *
         * @return
         */
        @Override
        public Map<K, K> loadAll(Iterable<? extends K> keys)
            {
            Map<K, K> map = new HashMap<K, K>();

            for (K key : keys)
                {
                map.put(key, key);
                }

            loaded.putAll(map);
            loadCount.addAndGet(map.size());

            return map;
            }

        /**
         * Obtain the number of entries that have been loaded.
         *
         * @return the number of entries loaded thus far.
         */
        public int getLoadCount()
            {
            return loadCount.get();
            }

        /**
         * Method description
         *
         * @return
         */
        public ConcurrentHashMap<K, K> getLoaded()
            {
            return loaded;
            }

        /**
         * Determines if the specified key has been loaded by this loader.
         *
         * @param key  the key
         *
         * @return true if the key has been loaded, false otherwise
         */
        public boolean hasLoaded(K key)
            {
            return loaded.containsKey(key);
            }

        private static final long serialVersionUID = -1L;

        /**
         * The keys that have been loaded by this loader.
         */
        private ConcurrentHashMap<K, K> loaded = new ConcurrentHashMap<K, K>();

        /**
         * The number of loads that have occurred.
         */
        private AtomicInteger loadCount = new AtomicInteger(0);
        }

    // ----- constants ------------------------------------------------------

    static final CacheEntryEventFilter<String, String> ALL                = null;
    static final CacheEntryEventFilter<Long, String>   ALL_LS             = null;

    /**
     * A {@link org.junit.ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation               s_systemPropertyIsolation = new SystemPropertyIsolation();

    // ----- data members ---------------------------------------------------

    static protected CacheManager                          cacheMgr;

    protected CompleteConfiguration<String, String>        ssConfiguration;
    protected CompleteConfiguration<Long, String>          lsConfiguration;
    protected CompleteConfiguration<String, Point>         spConfiguration;
    protected CompleteConfiguration<String, NonPofPoint>   snpConfiguration;
    protected CompleteConfiguration<Integer, Integer>      iiConfiguration;
    protected CompleteConfiguration<String, List>          slConfiguration;
    protected CompleteConfiguration<String, Map>           smConfiguration;
    protected Cache<String, String>                        ssCache;
    protected Cache<Long, String>                          lsCache;
    protected Cache<String, NonPofPoint>                   snpCache;
    protected Cache<String, Point>                         spCache;
    protected Cache<Integer, Integer>                      iiCache;
    protected Cache<String, List>                          slCache;
    protected Cache<String, Map>                           smCache;
    protected SimpleCacheLoader<Integer>                   loader;
    protected CacheListenerTests.MyCacheEntryListener<?, ?> listener;

    public static String           s_sPofEnabled;
    public static String           s_sPofConfigUri;
    }
