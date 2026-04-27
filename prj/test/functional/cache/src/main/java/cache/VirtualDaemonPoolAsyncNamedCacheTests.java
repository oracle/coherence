/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.net.NamedCache;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Reuses the async named-cache functional matrix against a VT-backed
 * distributed cache service.
 *
 * @author Aleks Seovic  2026.04.24
 * @since 26.04
 */
public class VirtualDaemonPoolAsyncNamedCacheTests
        extends BaseAsyncNamedCacheTests
    {
    public VirtualDaemonPoolAsyncNamedCacheTests()
        {
        super(CACHE_NAME);
        }

    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.test.daemonpool", "virtual");
        System.setProperty("coherence.distributed.localstorage", "true");

        AbstractFunctionalTest._startup();
        }

    @AfterClass
    public static void _shutdown()
        {
        try
            {
            AbstractFunctionalTest._shutdown();
            }
        finally
            {
            System.clearProperty("coherence.test.daemonpool");
            System.clearProperty("coherence.distributed.localstorage");
            }
        }

    @After
    public void cleanupCache()
        {
        NamedCache<Object, Object> cache = s_support.ensureCache(CACHE_NAME);

        if (cache.isActive())
            {
            cache.clear();
            }
        }

    @Override
    protected <K, V> NamedCache<K, V> getNamedCache(String sCacheName)
        {
        NamedCache<K, V> cache = s_support.ensureCache(sCacheName);
        cache.clear();
        return cache;
        }

    protected static class FunctionalSupport
            extends AbstractFunctionalTest
        {
        protected FunctionalSupport()
            {
            super(FILE_CFG_CACHE);
            }

        protected <K, V> NamedCache<K, V> ensureCache(String sCacheName)
            {
            return super.getNamedCache(sCacheName);
            }
        }

    protected static final String FILE_CFG_CACHE = "coherence-cache-config.xml";

    protected static final String CACHE_NAME = "dist-vt-async-test";

    protected static final FunctionalSupport s_support = new FunctionalSupport();
    }
