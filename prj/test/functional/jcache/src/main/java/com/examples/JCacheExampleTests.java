/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.examples;

import com.tangosol.net.CacheFactory;
import com.oracle.coherence.testing.SystemPropertyIsolation;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;

import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;

import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;

import javax.cache.processor.EntryProcessorResult;

/**
 * JCache Documentation examples
 *
 * Extracted from documentation to ensure regression test for these usages.
 *
 * @since  12.1.3
 * @author jr 2014.5.1
 * @author jf 2014.5.8
 */
public class JCacheExampleTests
    {
    /**
     * get the CacheManager
     *
     * @return {@link CacheManager}
     */
    protected CacheManager getCacheManager() {
        return Caching.getCachingProvider().getCacheManager();
        }

    @AfterClass
    public static void cleanup()
        {
        Caching.getCachingProvider().close();
        CacheFactory.shutdown();
        }

    /**
     * Section 33.5.4 JCache Cache Configuration
     */
    @Test
    public void jcacheConfigurationSample()
        {
        m_cacheManager = getCacheManager();

        MutableConfiguration<String, Integer> config = new MutableConfiguration();

        config.setStoreByValue(true).setTypes(String.class, Integer.class).setReadThrough(true).setWriteThrough(true)
            .setManagementEnabled(true).setStatisticsEnabled(true)
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.FIVE_MINUTES));

        Cache<String, Integer> cache    = m_cacheManager.createCache("MyCache", config);

        Cache<String, Integer> getcache = m_cacheManager.getCache("MyCache", String.class, Integer.class);

        assertEquals(cache, getcache);
        }

    /**
     * Section 34.2.1: Create the Sample JCache Application
     */
    @Test
    public void testJCacheExample()
        {
        m_cacheManager = getCacheManager();

        MutableConfiguration<String, Object> config = new MutableConfiguration<String, Object>();

        config.setStoreByValue(true).setTypes(String.class, Object.class);

        Cache<String, Object> cache = m_cacheManager.createCache("MyCache1", config);

        Person                p     = new Person("John", "Doe", 24);

        String                key   = "k";
        Person                value = p;

        cache.put(key, value);
        System.out.println("\n Cache: " + cache.getName() + " contains: " + cache.get(key) + "\n");
        }

    /**
     *  Section 36.4.1 Registering Event Listeners and Filters During Cache Configuration
     */
    @Test
    public void configurationTimeCacheEntryListenerRegistration()
        {
        m_cacheManager = getCacheManager();

        MutableConfiguration<String, String> config = new MutableConfiguration<String, String>();

        config.setTypes(String.class, String.class)
            .addCacheEntryListenerConfiguration(new MutableCacheEntryListenerConfiguration(FactoryBuilder
                .factoryOf(MyCacheEntryListener.class), FactoryBuilder.factoryOf(MyCacheEntryEventFilter.class), true,
                    true));

        Cache<String, String> cache = m_cacheManager.createCache("MyCache2", config);

        cache.put("firstKey", "firstValue");
        cache.put("filterOutKey", "filterOutValue");    // no listener for this one
        }

    /**
     * Section 36.4.1 Registering Event Listeners and Filters at Runtime
     */
    @Test
    public void dynamicListenerRegistration()
        {
        m_cacheManager = getCacheManager();

        MutableConfiguration<String, String> config = new MutableConfiguration<String, String>();

        config.setTypes(String.class, String.class);

        Cache<String, String> cache = m_cacheManager.createCache("MyCache3", config);

        MutableCacheEntryListenerConfiguration listenerConfig =
            new MutableCacheEntryListenerConfiguration(FactoryBuilder.factoryOf(MyCacheEntryListener.class),
                FactoryBuilder.factoryOf(MyCacheEntryEventFilter.class), true, true);

        cache.registerCacheEntryListener(listenerConfig);
        cache.put("firstKey", "firstValue");
        cache.put("filterOutKey", "filterOutValue");    // no listener for this one
        }

    /**
     * Section 37.3.1: Invoking Entry Processors for a Single Key
     */
    @Test
    public void invokeEntryProcessorDocumentation()
        {
        m_cacheManager = getCacheManager();

        MutableConfiguration<String, Integer> config = new MutableConfiguration<String, Integer>();

        config.setTypes(String.class, Integer.class);

        Cache<String, Integer> cache = m_cacheManager.createCache("MyCache4", config);

        String                 key   = "k";
        Integer                value = 1;

        cache.put(key, value);

        System.out.println("The value is " + cache.get(key) + "\n");

        Integer result = cache.invoke(key, new MyEntryProcessor());

        assertEquals(value, result);

        System.out.println("The value is now " + cache.get(key) + "\n");
        }

    /**
     * Section 37.3.2: Invoking Entry Processors for Multiple Keys
     */
    @Test
    public void multipleKeyInvokeEntryProcessor()
        {
        m_cacheManager = getCacheManager();

        MutableConfiguration<String, Integer> config = new MutableConfiguration<String, Integer>();

        config.setTypes(String.class, Integer.class);

        Cache<String, Integer> cache  = m_cacheManager.createCache("MyCache5", config);

        String                 key    = "k";
        Integer                value  = 1;
        String                 key1   = "k1";
        Integer                value1 = 1;

        cache.put(key, value);
        cache.put(key1, value1);

        HashSet<String> hs = new HashSet<String>();

        hs.add(key);
        hs.add(key1);

        Map<String, EntryProcessorResult<Integer>> map = cache.invokeAll(hs, new MyEntryProcessor());

        System.out.println("The value of k is now " + cache.get(key)
                           + " the result of invokeAll for k is previous value of " + map.get(key).get() + "\n");
        System.out.println("The value of k1 is now " + cache.get(key1)
                           + " the result of invokeAll for k1 is previous value of " + map.get(key1).get() + "\n");

        // assert EntryProcessor process result returned previous value for key.
        assertEquals(map.get(key).get(), value);
        assertEquals(map.get(key1).get(), value1);
        }

    // ----- data members ---------------------------------------------------
    private CacheManager m_cacheManager;

    /**
     * A {@link org.junit.ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }
