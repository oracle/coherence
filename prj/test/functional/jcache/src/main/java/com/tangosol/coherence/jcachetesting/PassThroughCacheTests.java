/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.tangosol.coherence.jcache.Constants;

import com.tangosol.coherence.jcache.passthroughcache.PassThroughCacheConfiguration;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.oracle.coherence.testing.SystemPropertyIsolation;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.net.URISyntaxException;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;

import javax.cache.configuration.MutableConfiguration;

/**
 * PassThroughCache functional test
 *
 * TODO: COH-11101: complete this test.  Only testing creation at this time.
 *
 * @version  Coherence 12.1.3
 * @author  jf 2014.2.25
 */
public class PassThroughCacheTests
    {
    @BeforeClass
    public static void beforeClassSetup()
            throws URISyntaxException
        {
        CacheFactory.shutdown();
        Caching.getCachingProvider().close();

        System.setProperty(Constants.DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY,
                PassThroughCacheConfiguration.class.getCanonicalName());

        // NOTE: must not use JCache namespace with passthrough.
        cacheManager = Caching.getCachingProvider().getCacheManager(new URI("coherence-cache-config.xml"), null);
        assertNotNull(cacheManager);
        }

    @AfterClass
    public static void afterClassCleanup()
        {
        cacheManager.close();
        CacheFactory.shutdown();
        }

    @Test
    public void createTest()
            throws URISyntaxException
        {
        String                                      jcacheName     = "myjcache";
        String                                      namedCacheName = "mynamecache";
        PassThroughCacheConfiguration<Long, String> config         = new PassThroughCacheConfiguration<Long, String>();

        config.setNamedCacheName(namedCacheName);
        config.setTypes(Long.class, String.class);

        Cache<Long, String> myJcache = cacheManager.createCache(jcacheName, config);

        assertNotNull(myJcache);

        NamedCache myNamedCache = myJcache.unwrap(NamedCache.class);

        assertNotNull(myNamedCache);
        assertEquals(namedCacheName, myNamedCache.getCacheName());

        PassThroughCacheConfiguration<Long, String> aConfig =
            myJcache.getConfiguration(PassThroughCacheConfiguration.class);

        myJcache.close();
        assertNotNull(aConfig);
        }

    @Test
    public void createTestWithMutableConfiguration()
            throws URISyntaxException
        {
        String                             jcacheName = "myjcache";
        MutableConfiguration<Long, String> config     = new MutableConfiguration<Long, String>();

        config.setTypes(Long.class, String.class);

        Cache<Long, String> myJcache = cacheManager.createCache(jcacheName, config);

        assertNotNull(myJcache);

        NamedCache myNamedCache = myJcache.unwrap(NamedCache.class);

        assertNotNull(myNamedCache);
        assertEquals(jcacheName, myNamedCache.getCacheName());

        PassThroughCacheConfiguration<Long, String> actualConfig =
            myJcache.getConfiguration(PassThroughCacheConfiguration.class);

        assertNotNull(actualConfig);
        assertEquals(config.getKeyType(), actualConfig.getKeyType());
        assertEquals(config.getValueType(), actualConfig.getValueType());
        myJcache.close();
        }

    @Test
    public void createTestWithMutableConfigurationAndPassthroughAlias()
        {
        String                             jcacheName = "myjcache";
        MutableConfiguration<Long, String> config     = new MutableConfiguration<Long, String>();

        config.setTypes(Long.class, String.class);
        System.setProperty(Constants.DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY, "passthrough");

        Cache<Long, String> myJcache = cacheManager.createCache(jcacheName, config);

        assertNotNull(myJcache);

        NamedCache myNamedCache = myJcache.unwrap(NamedCache.class);

        assertNotNull(myNamedCache);
        assertEquals(jcacheName, myNamedCache.getCacheName());

        PassThroughCacheConfiguration<Long, String> actualConfig =
            myJcache.getConfiguration(PassThroughCacheConfiguration.class);

        assertNotNull(actualConfig);
        assertEquals(config.getKeyType(), actualConfig.getKeyType());
        assertEquals(config.getValueType(), actualConfig.getValueType());
        myJcache.close();
        }

    private static CacheManager cacheManager;

    /**
     * A {@link org.junit.ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();


    }
