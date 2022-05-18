/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.tangosol.coherence.jcache.AbstractCoherenceBasedCache;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.lang.management.ManagementFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheManager;

import javax.cache.configuration.MutableConfiguration;

import javax.cache.expiry.AccessedExpiryPolicy;

import javax.cache.management.CacheStatisticsMXBean;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import static javax.cache.expiry.Duration.ONE_HOUR;

/**
 * Class description
 *
 * @version        Enter version here..., 13/10/04
 * @author         Enter your name here...
 */
public class CacheStatisticsTests
        extends TestSupport

    {
    /**
     * Method description
     */
    @BeforeClass
    static public void setup()
        {
        System.setProperty("com.sun.management.jmxremote", "true");

        // System.setProperty("coherence.management", "all");
        // System.setProperty("coherence.management.remote", "true");
        }

    /**
     * Method description
     *
     * @throws AttributeNotFoundException
     * @throws InstanceNotFoundException
     * @throws MBeanException
     * @throws MalformedObjectNameException
     * @throws ReflectionException
     */
    @Test
    public void accessStatistics()
            throws MalformedObjectNameException, AttributeNotFoundException, MBeanException, ReflectionException,
                   InstanceNotFoundException
        {
        CacheManager                          cacheManager = getJcacheTestContext().getCacheManager(null, null, null);

        MutableConfiguration<String, Integer> config       = new MutableConfiguration<String, Integer>();

        config.setStoreByValue(true).setTypes(String.class,
                               Integer.class).setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(
                                   ONE_HOUR)).setStatisticsEnabled(true);

        Cache<String, Integer> cache = getJcacheTestContext().configureCache(cacheManager, getTestCacheName(), config);
        CacheStatisticsMXBean  bean  = ((AbstractCoherenceBasedCache) cache).getStatistics();

        System.out.println("Beginning Cache Statistics = " + bean.toString());

        cache.containsKey("missing");
        assertEquals(0, bean.getCacheMisses());
        cache.put("hello", 1);
        assertEquals(1, bean.getCachePuts());

        cache.get("missing");
        assertEquals(1, bean.getCacheMisses());

        Map<String, Integer> map = new HashMap<String, Integer>();

        map.put("hello", 1);
        map.put("world", 2);
        map.put("good", 3);
        map.put("bye", 4);
        map.put("seed", 5);
        cache.putAll(map);
        assertEquals(6, bean.getCachePuts());

        System.out.println("1) hits=" + bean.getCacheHits() + " misses=" + bean.getCacheMisses() + " gets="
                           + bean.getCacheGets() + " puts=" + bean.getCachePuts() + " removes="
                           + bean.getCacheRemovals());

        // cache.get("missingAgain");
        // cache.put("world", 2);
        // cache.get("hello");

        Set<ObjectName> registeredObjectNames = null;
        MBeanServer     mBeanServer           = ManagementFactory.getPlatformMBeanServer();

        ObjectName objectName = new ObjectName("javax.cache:type=CacheStatistics" + ",CacheManager="
                                    + (cache.getCacheManager().getURI().toString()) + ",Cache=" + cache.getName());

        System.out.println("JMXBean: " + objectName + " CachePuts="
                           + mBeanServer.getAttribute(objectName, "CachePuts"));

        int hitCount = 0;

        cache.remove("hello", 1);
        hitCount++;
        assertEquals(hitCount, bean.getCacheHits());
        assertEquals(1, bean.getCacheRemovals());

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

        cache.get("seed");
        hitCount++;
        assertEquals(hitCount, bean.getCacheHits());
        cache.get("missing");
        assertEquals(2, bean.getCacheMisses());

        // assertEquals(50.0f, bean.getCacheHitPercentage(), 0.9);
        // assertEquals(50.0f, bean.getCacheMissPercentage(), 0.9);
        System.out.println("Final Cache Statistics = " + bean.toString());
        }
    }
