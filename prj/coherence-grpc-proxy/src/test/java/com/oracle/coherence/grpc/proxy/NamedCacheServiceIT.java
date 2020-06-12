/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.NamedCache;

import org.junit.jupiter.api.BeforeAll;

/**
 * Integration tests for {@link NamedCacheService}.
 *
 * @author Jonathan Knight  2019.11.01
 * @since 20.06
 */
class NamedCacheServiceIT
        extends BaseNamedCacheServiceTests
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    protected static void setup()
        {
        System.setProperty("coherence.ttl",        "0");
        System.setProperty("coherence.cluster",    "NamedCacheServiceIT");
        System.setProperty("coherence.pof.config", "test-pof-config.xml");
        System.setProperty("coherence.override",   "test-coherence-override.xml");
        DefaultCacheServer.startServerDaemon().waitForServiceStart();

        s_ccf = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("coherence-cache-config.xml", null);

        s_service = NamedCacheService.builder().configurableCacheFactory(s_ccf).build();
        }

    // ----- NamedCacheServiceTest methods ----------------------------------

    @Override
    protected <K, V> NamedCache<K, V> ensureCache(String name, ClassLoader loader)
        {
        return s_ccf.ensureCache(name, loader);
        }

    @Override
    protected void destroyCache(NamedCache<?, ?> cache)
        {
        s_ccf.destroyCache(cache);
        }

    @Override
    protected NamedCacheClient createService()
        {
        return s_service;
        }

    // ----- data members ---------------------------------------------------

    protected static ConfigurableCacheFactory s_ccf;

    protected static NamedCacheService s_service;
    }
