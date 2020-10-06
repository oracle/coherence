/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.helidon.client;

import com.oracle.coherence.cdi.Remote;
import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.client.GrpcRemoteSession;
import com.oracle.coherence.client.GrpcSessions;
import com.oracle.coherence.grpc.proxy.GrpcServerController;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.NamedCache;
import com.tangosol.net.SessionProvider;
import io.helidon.microprofile.server.Server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight  2020.09.30
 */
public class CacheProducerIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setupBaseTest() throws Exception
        {
        System.setProperty("coherence.ttl",              "0");
        System.setProperty("coherence.clustername",      "MapEventsIT");
        System.setProperty("coherence.cacheconfig",      "coherence-config.xml");
        System.setProperty("coherence.cacheconfig.test", "coherence-config-two.xml");
        System.setProperty("coherence.pof.config",       "test-pof-config.xml");
        System.setProperty("coherence.pof.enabled",      "true");
        System.setProperty("coherence.log.level",        "9");

        // The CDI server will start DCS which will in turn cause the gRPC server to start
        s_server  = Server.create().start();
        s_ccf     = CacheFactory.getCacheFactoryBuilder()
                                .getConfigurableCacheFactory("coherence-config.xml", null);
        s_ccfTest = CacheFactory.getCacheFactoryBuilder()
                                .getConfigurableCacheFactory("coherence-config-two.xml", null);

        // wait at most 1 minute for the gRPC Server
        GrpcServerController.INSTANCE.whenStarted()
                .toCompletableFuture()
                .get(1, TimeUnit.MINUTES);
        }

    @AfterAll
    static void cleanupBaseTest()
        {
        SessionProvider.get().close();
        if (s_server != null)
            {
            s_server.stop();
            }
        }

    // ----- tests ----------------------------------------------------------

    @Test
    public void shouldInjectCaches()
        {
        Instance<CacheHolder> instance = CDI.current().select(CacheHolder.class);
        assertThat(instance.isResolvable(), is(true));

        CacheHolder holder = instance.get();
        NamedCache<String, String> cacheOne = holder.getCacheOne();
        NamedCache<String, String> cacheTwo = holder.getCacheTwo();

        assertThat(cacheOne, is(notNullValue()));
        assertThat(cacheTwo, is(notNullValue()));

        cacheOne.put("key-1", "value-one");
        cacheTwo.put("key-1", "value-two");

        assertThat(cacheOne.get("key-1"), is("value-one"));
        assertThat(cacheTwo.get("key-1"), is("value-two"));
        }


    // ----- inner class: CacheHolder ---------------------------------------

    @ApplicationScoped
    public static class CacheHolder
        {
        @Inject
        @Remote
        private NamedCache<String, String> cacheOne;

        @Inject
        @Remote
        @Scope("test")
        private NamedCache<String, String> cacheTwo;

        public NamedCache<String, String> getCacheOne()
            {
            return cacheOne;
            }

        public NamedCache<String, String> getCacheTwo()
            {
            return cacheTwo;
            }
        }

    // ----- data members ---------------------------------------------------

    private static ConfigurableCacheFactory s_ccf;

    private static ConfigurableCacheFactory s_ccfTest;

    private static Server s_server;
    }
