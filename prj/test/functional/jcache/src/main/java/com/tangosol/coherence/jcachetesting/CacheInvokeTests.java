/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.cache.Cache;
import javax.cache.CacheManager;

import javax.cache.configuration.MutableConfiguration;

import javax.cache.processor.EntryProcessorException;

/**
 * Regression test added after trying to run coherence jcache adapter impl against coherence 12.1.3.
 * See workaround in adapter impl: PartitionedCache.invoke and invokeAll.
 *
 * @version        1.0, 13/09/13
 * @author         jfialli
 */
public class CacheInvokeTests
        extends TestSupport
    {
    /**
     * Method description
     */
    @Before
    public void setup()
        {
        MutableConfiguration<Integer, String> config = new MutableConfiguration<Integer, String>();

        m_mgr = getJcacheTestContext().getCacheManager(null, null, null);
        }

    /**
     * Method description
     */
    @After
    public void cleanup()
        {
        m_mgr = getJcacheTestContext().getCacheManager(null, null, null);

        for (String cacheName : m_mgr.getCacheNames())
            {
            m_mgr.destroyCache(cacheName);
            }
        }

    /**
     * regression test for issue discovered in JCACHE-129 when upgrading from coherence 3.7.x to 12.1.3.x.
     */
    @Test
    public void COH10453_regresssionTest()
        {
        final String           TEST_CACHE_NAME = getTestCacheName();
        final Integer          key             = 123;
        Cache<Integer, String> cache           = null;
        MutableConfiguration<Integer, String> config = new MutableConfiguration<Integer,
                                                           String>().setTypes(Integer.class, String.class);

        try
            {
            cache = getJcacheTestContext().configureCache(m_mgr, TEST_CACHE_NAME, config);
            assertNotNull(cache);
            assertNotNull(m_mgr.getCache(TEST_CACHE_NAME, Integer.class, String.class));
            assertEquals(TEST_CACHE_NAME, cache.getName());
            cache.invoke(key, new FailingEntryProcessor<Integer, String, Void>(UnsupportedOperationException.class));
            fail();
            }
        catch (EntryProcessorException e)
            {
            assertTrue(e.getCause() instanceof RuntimeException);
            m_mgr.destroyCache(TEST_CACHE_NAME);

            // CacheInvokeTest.existingReplace()/existingRemove/existingException fragment to recreate issue.
            config = new MutableConfiguration<Integer, String>().setTypes(Integer.class, String.class);
            cache  = getJcacheTestContext().configureCache(m_mgr, TEST_CACHE_NAME, config);
            assertNotNull(cache);
            assertNotNull(m_mgr.getCache(TEST_CACHE_NAME, Integer.class, String.class));
            assertEquals(TEST_CACHE_NAME, cache.getName());

            // without the containsKey() being added when the only reference to a cache is an invoke processor throwing
            // an exception, next cache with same name is broken.  a simple put will not work.
            // this was causing 3 CacheInvokeTest that happen to run after
            // jsr 107 tck test CacheInvokeTest.TestProcessorExceptionIsWrapped
            cache.put(key, "456");

            // the next assertion fails, confirming that put will not work.
            // adding simple call to containsKey after the invoke entry processor that threw an exception works around
            // this issue.  need assistance in verifying if this is issue in coherence 12.1.3 since never saw
            // this issue when running on top of coherence 3.7.1.
            assertTrue(cache.containsKey(key));

            // expected
            }
        finally
            {
            if (cache != null)
                {
                m_mgr.destroyCache(TEST_CACHE_NAME);
                }
            }
        }

    // ----- data members -----------------------------------------------------
    private CacheManager m_mgr;
    }
