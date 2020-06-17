/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.oracle.coherence.cdi.Scope;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.Base;
import org.junit.jupiter.api.BeforeAll;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        System.setProperty("coherence.cacheconfig", "coherence-config.xml");
        System.setProperty("coherence.pof.config", "test-pof-config.xml");
        System.setProperty("coherence.override",   "test-coherence-override.xml");

        s_ccfDefault = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(null);
        DefaultCacheServer.startServerDaemon(s_ccfDefault).waitForServiceStart();

        s_service = NamedCacheService.builder()
                        .configurableCacheFactorySupplier(NamedCacheServiceIT::ensureCCF)
                        .build();

        }

    // ----- NamedCacheServiceTest methods ----------------------------------

    @Override
    protected <K, V> NamedCache<K, V> ensureCache(String sScope, String name, ClassLoader loader)
        {
        ConfigurableCacheFactory ccf = NamedCacheServiceIT.ensureCCF(sScope);
        return ccf.ensureCache(name, loader);
        }

    @Override
    protected void destroyCache(String sScope, NamedCache<?, ?> cache)
        {
        ConfigurableCacheFactory ccf = NamedCacheServiceIT.ensureCCF(sScope);
        ccf.destroyCache(cache);
        }

    @Override
    protected NamedCacheClient createService()
        {
        return s_service;
        }

    protected static ConfigurableCacheFactory ensureCCF(String sScope)
        {
        if (Scope.DEFAULT.equals(sScope))
            {
            return s_ccfDefault;
            }
        return s_mapCCF.computeIfAbsent(sScope, NamedCacheServiceIT::createCCF);
        }

    protected static ConfigurableCacheFactory createCCF(String sScope)
        {
        ClassLoader loader = Base.getContextClassLoader();
        XmlElement xmlConfig = XmlHelper.loadFileOrResource("coherence-config.xml", "Cache Configuration", loader);

        ExtensibleConfigurableCacheFactory.Dependencies deps = ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance(xmlConfig, loader, "test-pof-config.xml", sScope);
        ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(deps);
        eccf.activate();
        return eccf;
        }

    // ----- data members ---------------------------------------------------

    private static ConfigurableCacheFactory s_ccfDefault;

    private static NamedCacheService s_service;

    private final static Map<String, ConfigurableCacheFactory> s_mapCCF = new ConcurrentHashMap<>();
    }
