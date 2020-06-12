/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import io.helidon.microprofile.grpc.client.GrpcProxy;

import io.helidon.microprofile.grpc.core.InProcessGrpcChannel;

import io.helidon.microprofile.server.Server;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Instance;

import javax.enterprise.inject.spi.CDI;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * A test of {@link NamedCacheService} that uses CDI to discover and deploy
 * the service into a Helidon gRPC server and then create a client proxy
 * with which to communicate with the service.
 *
 * @author Jonathan Knight  2019.11.01
 * @since 14.1.2
 */
class NamedCacheServiceCdiIT
        extends BaseNamedCacheServiceTests
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    protected static void setup()
        {
        System.setProperty("coherence.ttl",        "0");
        System.setProperty("coherence.cluster",    "NamedCacheServiceCdiIT");
        System.setProperty("coherence.pof.config", "test-pof-config.xml");
        System.setProperty("coherence.override",   "test-coherence-override.xml");
        System.setProperty("mp.initializer.allow", "true");

        s_server = Server.create().start();
        }

    @AfterAll
    static void cleanupBaseTest()
        {
        s_server.stop();
        }

    // ----- NamedCacheServiceTest ------------------------------------------

    @Override
    protected NamedCacheClient createService()
        {
        Instance<NamedCacheClient> instance = CDI.current()
                .select(NamedCacheClient.class, GrpcProxy.Literal.INSTANCE, InProcessGrpcChannel.Literal.INSTANCE);

        assertThat(instance.isResolvable(), is(true));
        return instance.get();
        }

    @Override
    protected <K, V> NamedCache<K, V> ensureCache(String name, ClassLoader loader)
        {
        return CacheFactory.getCache(name, loader);
        }

    @Override
    protected void destroyCache(NamedCache<?, ?> cache)
        {
        CacheFactory.destroyCache(cache);
        }

    // ----- inner class: ClientBean ----------------------------------------

    /**
     * A CDI bean that will have a {@link NamedCacheService} client
     * injected into it.
     */
    @ApplicationScoped
    public static class ClientBean
        {
        // ----- data members -----------------------------------------------
        @Inject
        @GrpcProxy
        @InProcessGrpcChannel
        private NamedCacheClient service;
        }

    // ----- data members ---------------------------------------------------

    private static Server s_server;
    }
