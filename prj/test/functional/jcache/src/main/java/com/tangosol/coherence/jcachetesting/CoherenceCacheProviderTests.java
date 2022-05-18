/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.tangosol.coherence.jcache.partitionedcache.PartitionedCacheConfiguration;
import com.tangosol.coherence.jcache.CoherenceBasedCachingProvider;
;
import com.oracle.coherence.testing.SystemPropertyIsolation;

import org.junit.ClassRule;
import org.junit.Test;

import static com.tangosol.coherence.jcache.Constants.DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;

import javax.cache.configuration.OptionalFeature;

import javax.cache.spi.CachingProvider;
import java.util.Properties;

/**
 * Class description
 *
 * @version        Enter version here..., 13/03/28
 * @author         Enter your name here...
 */
public class CoherenceCacheProviderTests
    {
    /**
     * Method description
     *
     * @throws Exception
     */
    @Test
    public void testReflectionConstructor()
            throws Exception
        {
        CachingProvider provider = Caching.getCachingProvider();

        assertNotNull(provider);
        }

    /**
     * Method description
     */
    @Test
    public void testIsSupported()
        {
        CachingProvider cacheProvider = getCachingProvider();
        CoherenceBasedCachingProvider provider = cacheProvider instanceof CoherenceBasedCachingProvider
                                                 ? (CoherenceBasedCachingProvider) cacheProvider : null;

        for (OptionalFeature feature : OptionalFeature.values())
            {
            // depending on the configuration for the default Coherence JCache Adapter implementation,
            // this can return true or false depending if default is for local or partitioned.
            if (feature.equals(OptionalFeature.STORE_BY_REFERENCE) && provider != null)
                {
                if (provider.getDefaultCoherenceBasedConfigurationClassName().contains("Local"))
                    {
                    System.out.println("Assert " + feature + " is supported for "
                                       + provider.getDefaultCoherenceBasedConfigurationClassName());
                    assertTrue(cacheProvider.isSupported(feature));
                    }
                else if (provider.getDefaultCoherenceBasedConfigurationClassName().contains("Partitioned"))
                    {
                    System.out.println("Assert " + feature + " not supported for "
                                       + provider.getDefaultCoherenceBasedConfigurationClassName());
                    assertFalse(cacheProvider.isSupported(feature));
                    }
                }
            }
        }

    @Test
    public void testDefaultCoherenceBasedConfigurationResetAfterClose()
        {
        try
            {
            getCachingProvider().close();
            System.setProperty(DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY, "partitioned");
            CoherenceBasedCachingProvider provider = getCachingProvider();
            assertTrue("expected contains Partitioned; got " + provider.getDefaultCoherenceBasedConfigurationClassName(),
                    provider.getDefaultCoherenceBasedConfigurationClassName().contains("Partitioned"));

            getCachingProvider().close();
            System.setProperty(DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY, PartitionedCacheConfiguration.class.getCanonicalName());
            assertTrue(getCachingProvider().getDefaultCoherenceBasedConfigurationClassName().contains("Partitioned"));

            getCachingProvider().close();
            System.setProperty(DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY, "local");
            assertTrue(getCachingProvider().getDefaultCoherenceBasedConfigurationClassName().contains("Local"));

            getCachingProvider().close();
            System.setProperty(DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY, "extend");
            assertTrue(getCachingProvider().getDefaultCoherenceBasedConfigurationClassName().contains("Remote"));

            getCachingProvider().close();
            System.setProperty(DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY, "remote");
            assertTrue(getCachingProvider().getDefaultCoherenceBasedConfigurationClassName().contains("Remote"));

            getCachingProvider().close();
            System.setProperty(DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY, "passthrough");
            assertTrue(getCachingProvider().getDefaultCoherenceBasedConfigurationClassName().contains("PassThrough"));
            }
        finally
            {
            System.clearProperty(DEFAULT_COHERENCE_JCACHE_CONFIGURATION_CLASS_NAME_SYSTEM_PROPERTY);
            getCachingProvider().close();
            }

        }

    @Test
    public void getCacheManager_nonNullProperties()
        {
        // make sure existing cache managers are closed and the non empty properties get picked up
        try
            {
            Caching.getCachingProvider().close();
            }
        catch (CacheException ignore)
            {
            // ignore exception which may happen if the provider is not active
            }

        CachingProvider provider = Caching.getCachingProvider();
        Properties properties = new Properties();
        properties.put("dummy.com", "goofy");

        CacheManager mgr1 = provider.getCacheManager(provider.getDefaultURI(), provider.getDefaultClassLoader(), properties);
        CacheManager manager = provider.getCacheManager();
        assertEquals(mgr1, manager);
        assertEquals(mgr1.getProperties(), manager.getProperties());
        assertEquals("goofy", manager.getProperties().getProperty("dummy.com"));
        assertEquals(properties, manager.getProperties());
        }

    // Utilities --------------------------------------------------

    static CoherenceBasedCachingProvider getCachingProvider()
        {
        return (CoherenceBasedCachingProvider) Caching.getCachingProvider();
        }

    /**
     * A {@link org.junit.ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }
