/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache;

import com.tangosol.coherence.jcache.common.ContainerHelper;
import com.tangosol.coherence.jcache.localcache.LocalCache;
import com.tangosol.coherence.jcache.localcache.LocalCacheConfiguration;
import com.tangosol.coherence.jcache.partitionedcache.PartitionedCache;
import com.tangosol.coherence.jcache.partitionedcache.PartitionedCacheConfiguration;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;

import com.oracle.coherence.testing.SystemPropertyIsolation;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.*;

import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Iterator;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;

import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.MutableConfiguration;

/**
 * Junit test for CoherenceBasedCacheManager
 *
 * @author         jfialli
 */
public class CoherenceBasedCacheManagerTests
        extends TestSupport
    {
    /**
     * Method description
     */
    @Test
    public void testValidConfigUri()
        {
        CacheManager cmgr = getJcacheTestContext().getCacheManager(null, null, null);

        assertNotNull(cmgr);
        cmgr.close();
        }

    @Test
    public void testClose()
        {
        CacheManager cmgr = getJcacheTestContext().getCacheManager(null, null, null);

        assertNotNull(cmgr);
        assertFalse(cmgr.isClosed());
        cmgr.close();
        assertTrue(cmgr.isClosed());
        }

    /**
     * For Container Support, it is important that when container undeploys coherence application and it disposes of the
     * CCF, that the associated CacheManager is also closed.  This test verifies that registering CacheManager with
     * CCF resource registry that it is disposed when CCF is disposed.
     */
    @Test
    public void testCCFDispose()
        {
        CoherenceBasedCacheManager cmgr1 = (CoherenceBasedCacheManager) getJcacheTestContext().getCacheManager(null,
                                               null, null);

        assertNotNull(cmgr1);
        assertFalse(cmgr1.isClosed());

        ConfigurableCacheFactory ccf1 = cmgr1.getConfigurableCacheFactory();

        ccf1.dispose();
        assertTrue(cmgr1.isClosed());

        CoherenceBasedCacheManager cmgr2 = (CoherenceBasedCacheManager) getJcacheTestContext().getCacheManager(null,
                                               null, null);

        assertNotEquals(cmgr1, cmgr2);

        CoherenceBasedCacheManager cmgr2Ref = (CoherenceBasedCacheManager) getJcacheTestContext().getCacheManager(null,
                                                  null, null);

        assertEquals(cmgr2, cmgr2Ref);

        ExtensibleConfigurableCacheFactory ccf2 =
            (ExtensibleConfigurableCacheFactory) cmgr2.getConfigurableCacheFactory();

        assertNotEquals(ccf1, ccf2);
        assertEquals(cmgr2.getConfigurableCacheFactory(), cmgr2Ref.getConfigurableCacheFactory());
        cmgr2.close();
        assertTrue(cmgr2.isClosed());
        assertTrue(cmgr2Ref.isClosed());

        try
            {
            ccf2.getResourceRegistry();
            assertTrue("ConfigurableCacheFactory should not be usable after its associated CacheManager closed", false);
            }
        catch (Throwable t)
            {
            assertTrue(true);
            }

        CoherenceBasedCacheManager cmgr3 = (CoherenceBasedCacheManager) getJcacheTestContext().getCacheManager(null,
                                               null, null);

        assertNotEquals(ccf2, cmgr3.getConfigurableCacheFactory());
        cmgr3.close();
        }

    /**
     * Method description
     */
    @Test
    public void testInvalidConfigUri()
        {
        CacheManager cmgr = null;

        try
            {
            getJcacheTestContext().getCacheManager(getJcacheTestContext().getInvalidCacheConfigURI(), null, null);
            assertTrue("getCacheManager(junit-client-invalid-cache-config.xml) should have thrown a RuntimeException",
                       false);
            }
        catch (CacheException ce)
            {
            assertTrue("handled expected CacheException when specifying an invalid configuration file as "
                       + " uri parameter to getCacheManager", true);

            //ce.printStackTrace();
            }

        assertNull(cmgr);
        }

    /**
     * Method description
     */
    @Test
    public void storeByReferenceTest()
        {
        Configuration<String, String> config = new MutableConfiguration<String, String>().setStoreByValue(false);
        CacheManager                  cmgr   = getJcacheTestContext().getCacheManager(null, null, null);

        Cache<String, String>         cache  = null;

        try
            {
            cache = cmgr.createCache(getTestCacheName(), config);
            assertTrue(cmgr.unwrap(CoherenceBasedCacheManager.class).validate());

            if (cache instanceof PartitionedCache)
                {
                assertTrue("expected to throw UnsupportedOperationexception for unsupported optional feature storeByReference",
                           false);
                }
            else if (cache instanceof LocalCache)
                {
                assertTrue("storeByReference is allowed for Coherence JCache Adapter LocalCache implementation", true);
                }
            }
        catch (UnsupportedOperationException e)
            {
            }
        finally
            {
            if (cache != null)
                {
                cmgr.destroyCache(cache.getName());
                }
            cmgr.close();
            }

        }

    /**
     * Method description
     */
    @Test
    public void testDefaultCacheCreation()
        {
        MutableConfiguration<String, String> config = new MutableConfiguration<String, String>();
        CacheManager                         cmgr   = getJcacheTestContext().getCacheManager(null, null, null);
        Cache<String, String>                cache  = cmgr.createCache(getTestCacheName(), config);

        try
            {
            if (cache instanceof PartitionedCache)
                {
                PartitionedCache pcache = cache.unwrap(PartitionedCache.class);

                assertTrue(pcache != null);
                System.out.println("Created default cache of type " + pcache.getClass().getName());
                }
            else
                {
                LocalCache lcache = cache.unwrap(LocalCache.class);

                assertTrue(lcache != null);
                System.out.println("Created default cache of type " + lcache.getClass().getName());
                }

            assertTrue(cmgr.unwrap(CoherenceBasedCacheManager.class).validate());
            }
        finally
            {
            cmgr.destroyCache(cache.getName());
            cmgr.close();
            }
        }

    /**
     * Method description
     */
    @Test
    public void testLocalCacheCreation()
        {
        CompleteConfiguration<String, String> config = new MutableConfiguration<String, String>();
        CacheManager                          cmgr   = getJcacheTestContext().getCacheManager(null, null, null);
        Cache<String, String> cache = cmgr.createCache(getTestCacheName(),
                                          new LocalCacheConfiguration<String, String>(config));

        try
            {
            LocalCache lcache = cache.unwrap(LocalCache.class);

            assertTrue(lcache != null);
            System.out.println("Created cache of type " + lcache.getClass().getName());
            cmgr.unwrap(CoherenceBasedCacheManager.class).validate();
            }
        finally
            {
            cmgr.destroyCache(cache.getName());
            cmgr.close();
            }
        }

    /**
     * Method description
     */
    @Test
    public void testPartitionedCacheCreation()
            throws IOException, InterruptedException
        {
        CompleteConfiguration<String, String> config = new MutableConfiguration<String,
                                                           String>().setTypes(String.class, String.class);
        CacheManager cmgr = getJcacheTestContext().getCacheManager(null, null, null);
        Cache<String, String> cache = cmgr.createCache(getTestCacheName(),
                                          new PartitionedCacheConfiguration<String, String>(config));

        try
            {
            PartitionedCache pcache = cache.unwrap(PartitionedCache.class);

            assertTrue(pcache != null);
            System.out.println("Created cache of type " + pcache.getClass().getName());
            cmgr.unwrap(CoherenceBasedCacheManager.class).validate();
            }
        finally
            {
            cmgr.destroyCache(cache.getName());
            cmgr.close();
            }
        }

    @Test
    public void testPartitionedCacheCreationInteger()
            throws IOException
        {
        CompleteConfiguration<Integer, Integer> config = new MutableConfiguration<Integer,
                                                             Integer>().setTypes(Integer.class, Integer.class);
        CacheManager cmgr = getJcacheTestContext().getCacheManager(null, null, null);
        Cache<Integer, Integer> cache = cmgr.createCache(getTestCacheName(),
                                            new PartitionedCacheConfiguration<Integer, Integer>(config));

        try
            {
            PartitionedCache pcache = cache.unwrap(PartitionedCache.class);

            assertTrue(pcache != null);
            System.out.println("Created cache of type " + pcache.getClass().getName());
            cmgr.unwrap(CoherenceBasedCacheManager.class).validate();
            }
        finally
            {
            cmgr.destroyCache(cache.getName());
            cmgr.close();
            }
        }

    @Test(expected = CacheException.class)
    public void testPartitionedCacheInvalidSameCacheNameDifferentConfiguration()
        {
        CompleteConfiguration<String, String> config = new MutableConfiguration<String,
                                                           String>().setTypes(String.class, String.class);
        CacheManager cmgr = getJcacheTestContext().getCacheManager(null, null, null);
        Cache<String, String> cache = cmgr.createCache(getTestCacheName(),
                                          new PartitionedCacheConfiguration<String, String>(config));
        Cache<Integer, Integer> cache1 = null;

        try
            {
            CompleteConfiguration<Integer, Integer> config1 = new MutableConfiguration<Integer,
                                                                  Integer>().setTypes(Integer.class, Integer.class);

            cache1 = cmgr.createCache(getTestCacheName(), new PartitionedCacheConfiguration<Integer, Integer>(config1));
            assertNull("should fail to create a cache with same name but a different configuration", cache1);
            }
        finally
            {
            cmgr.destroyCache(cache.getName());

            if (cache1 != null)
                {
                cmgr.destroyCache(cache1.getName());
                }
            cmgr.close();
            }
        }

    @Test(expected = CacheException.class)
    public void testPartitionedCacheCreateSameCacheNameSameConfiguration()
        {
        CompleteConfiguration<String, String> config = new MutableConfiguration<String,
                                                           String>().setTypes(String.class, String.class);
        CacheManager cmgr = getJcacheTestContext().getCacheManager(null, null, null);
        Cache<String, String> cache = cmgr.createCache(getTestCacheName(),
                                          new PartitionedCacheConfiguration<String, String>(config));
        Cache<String, String> cache1 = null;

        try
            {
            CompleteConfiguration<String, String> config1 = new MutableConfiguration<String,
                                                                String>().setTypes(String.class, String.class);

            cache1 = cmgr.createCache(getTestCacheName(), new PartitionedCacheConfiguration<String, String>(config1));
            assertNull("should fail to create a cache when one already exists", cache1);
            }
        finally
            {
            assertTrue(cmgr.unwrap(CoherenceBasedCacheManager.class).validate());

            cmgr.destroyCache(cache.getName());

            if (cache1 != null)
                {
                cmgr.destroyCache(cache1.getName());
                }
            cmgr.close();
            }
        }

    /**
     * Method description
     */

    //@Test
    public void testMultipleCacheManagerCacheCreation_JCACHE125()
            throws Exception
        {
        // TODO: this test no longer using same classloader.  Still issue with starting multiple coherence jcache
        // local or partitioned service without setting CCF scope.
        // workaround was to configure one cache to be local and other to be partitioned.
        // Avoids following failure:
        // java.lang.IllegalStateException: Service "jcache-local-service" has been started by a different configurable cache factory.
        // at com.tangosol.net.ExtensibleConfigurableCacheFactory.validateBackingMapManager(ExtensibleConfigurableCacheFactory.java:848)
        // at com.tangosol.net.ExtensibleConfigurableCacheFactory.ensureService(ExtensibleConfigurableCacheFactory.java:604)
        // at com.tangosol.coherence.config.scheme.AbstractCachingScheme.realizeCache(AbstractCachingScheme.java:54)
        // at com.tangosol.net.ExtensibleConfigurableCacheFactory.ensureCache(ExtensibleConfigurableCacheFactory.java:255)
        // at com.tangosol.coherence.jcache.localcache.LocalCache.<init>(LocalCache.java:119)
        // at com.tangosol.coherence.jcache.localcache.LocalCacheConfiguration.createCache(LocalCacheConfiguration.java:51)
        // at com.tangosol.coherence.jcache.CoherenceBasedCacheManager.createCache(CoherenceBasedCacheManager.java:126)
        // at com.tangosol.coherence.jcache.CoherenceBasedCacheManager.createCache(CoherenceBasedCacheManager.java:142)
        // at com.tangosol.coherence.jcache.CoherenceBasedCacheManagerTest.testMultipleCacheManagerCacheCreation_JCACHE125(CoherenceBasedCacheManagerTest.java:246)
        //
        // Can run this test on its own as it is configured.  It interfers with other junit tests getting above error.
        // CoherenceBasedCacheManager.close() does not appear to be closing services started by instantiating caches.
        CoherenceBasedCompleteConfiguration<String, String> config1 = new LocalCacheConfiguration<String, String>();

        config1.setTypes(String.class, String.class);

        // WORKAROUND: fails when this configuration is same as coherence jcache adapter impl as config1.
        CoherenceBasedCompleteConfiguration<Integer, Integer> config2 = new PartitionedCacheConfiguration<Integer,
                                                                            Integer>();

        config2.setTypes(Integer.class, Integer.class);

        CacheManager cmgr1                  = getJcacheTestContext().getCacheManager(null, null, null);

        ClassLoader  alternativeClassLoader = new MyClassLoader(Thread.currentThread().getContextClassLoader());
        CacheManager cmgr2 = Caching.getCachingProvider(alternativeClassLoader).getCacheManager(    // null,
            new URI("jcache-coherence-adapter-cache-config-alternative-cache-mgr-context.xml"), alternativeClassLoader,
            null);

        assertNotSame("validate that different URI result in different CacheManagers", cmgr1, cmgr2);

        Cache<String, String>   cache1  = null;
        Cache<Integer, Integer> cache2  = null;

        NamedCache              ncache1 = null;
        NamedCache              ncache2 = null;

        try
            {
            cache1  = cmgr1.createCache(getTestCacheName(), config1);
            cache2  = cmgr2.createCache(getTestCacheName(), config2);
            ncache1 = cache1.unwrap(NamedCache.class);
            ncache2 = cache2.unwrap(NamedCache.class);

            System.out.println("NamedCache1 = " + ncache1.getCacheName());
            System.out.println("NamedCache2 = " + ncache2.getCacheName());

            assertNotSame("these named caches should be different since CacheManagers have different scope", ncache1,
                          ncache2);

            cache1.put("hello", "goodbye");
            assertTrue(cache1.containsKey("hello"));
            cache2.put(1, 2);
            assertTrue(cache2.containsKey(1));

            Iterator                    iter  = cache1.iterator();
            Cache.Entry<String, String> entry = (Cache.Entry<String, String>) iter.next();

            assertEquals("hello", entry.getKey());
            assertEquals("goodbye", entry.getValue());
            assertFalse(iter.hasNext());

            Iterator                      iter2  = cache2.iterator();
            Cache.Entry<Integer, Integer> entry2 = (Cache.Entry<Integer, Integer>) iter2.next();

            assertEquals((Integer) 1, entry2.getKey());
            assertEquals((Integer) 2, entry2.getValue());
            assertFalse(iter2.hasNext());
            }
        finally
            {
            cmgr1.destroyCache(cache1.getName());
            cmgr1.close();

            if (ncache1 != ncache2)
                {
                try
                    {
                    cmgr2.destroyCache(cache2.getName());
                    cmgr2.close();
                    }
                catch (IllegalStateException e)
                    {
                    System.out.println("failed to destroyCache(" + cache2.getName() + ") due to handled exception "
                                       + e.getLocalizedMessage());
                    }
                }
            }
        }

    /**
     * Ensure setting "coherence.cacheconfig" system property is honored by
     * JCache CacheManager creation.
     *
     * This test case ensures that passthrough can be easily configured by setting that
     * system property. The Coherence JCache default URI is coherence-jcache-cache-config.xml.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testCacheManagerCreateViaCoherenceCacheConfigSystemProperty()
            throws URISyntaxException
        {
        String       preserve = System.getProperty(Constants.DEFAULT_COHERENCE_CONFIGURATION_URI_SYSTEM_PROPERTY);
        CacheManager cmgr     = null;

        try
            {
            System.setProperty(Constants.DEFAULT_COHERENCE_CONFIGURATION_URI_SYSTEM_PROPERTY,
                               "coherence-cache-config.xml");

            cmgr = getJcacheTestContext().getCacheManager(null, null, null);

            assertEquals(cmgr.getURI(), new URI("coherence-cache-config.xml"));
            }
        finally
            {
            CoherenceBasedCacheManager mgr = (CoherenceBasedCacheManager) cmgr;

            mgr.getConfigurableCacheFactory().dispose();
            Caching.getCachingProvider().close();

            if (preserve == null)
                {
                System.clearProperty(Constants.DEFAULT_COHERENCE_CONFIGURATION_URI_SYSTEM_PROPERTY);
                }
            else
                {
                System.setProperty(Constants.DEFAULT_COHERENCE_CONFIGURATION_URI_SYSTEM_PROPERTY, preserve);
                }
            }
        }

    @Test
    public void testJCacheNotActivatedByCacheServerActivated()
        {
        ExtensibleConfigurableCacheFactory eccf =
            (ExtensibleConfigurableCacheFactory) CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory(Constants.DEFAULT_COHERENCE_JCACHE_CONFIGURATION_URI,
                    Base.getContextClassLoader());

        try
            {
            ContainerHelper.JCacheLifecycleInterceptor interceptor =
                (ContainerHelper
                    .JCacheLifecycleInterceptor) eccf.getInterceptorRegistry()
                        .getEventInterceptor("jcache-lifecycle-interceptor");

            assertNotNull("JCacheNamespace did not register LifecycleInterceptor, interceptor unexpectedly null",
                          interceptor);
            assertEquals(0, interceptor.getActivatedCount());
            assertNull(eccf.getResourceRegistry().getResource(CacheManager.class));

            // simulate DefaultCacheServer calling eccf.activate().
            eccf.activate();

            CoherenceBasedCachingProvider provider = (CoherenceBasedCachingProvider) Caching.getCachingProvider();

            assertEquals("validate that LifeCycleEvent.ACTIVATED occurred due to call to ECCF.activate()", 1,
                         interceptor.getActivatedCount());
            }
        finally
            {
            if (eccf != null)
                {
                CacheFactory.getCacheFactoryBuilder().release(eccf);
                eccf.dispose();
                }
            }
        }

    private static class MyClassLoader
            extends ClassLoader
        {
        /**
         * Constructs {@link MyClassLoader}
         *
         *
         * @param parent classloader
         */
        public MyClassLoader(ClassLoader parent)
            {
            super(parent);
            }
        }

    /**
     * A {@link org.junit.ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();

    }
