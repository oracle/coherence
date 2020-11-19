/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.helidon.client;

import com.oracle.coherence.cdi.SessionName;
import com.oracle.coherence.client.NamedCacheClient;
import com.oracle.coherence.grpc.proxy.GrpcServerController;

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

import static org.hamcrest.CoreMatchers.instanceOf;
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
        System.setProperty("coherence.clustername",      "CacheProducerIT");
        System.setProperty("coherence.cacheconfig",      "coherence-config.xml");
        System.setProperty("coherence.pof.config",       "test-pof-config.xml");
        System.setProperty("coherence.pof.enabled",      "true");
        System.setProperty("coherence.log.level",        "9");

        // The CDI server will start DCS which will in turn cause the gRPC server to start
        s_server = Server.create().start();

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

        assertThat(cacheOne, is(instanceOf(NamedCacheClient.class)));
        assertThat(cacheTwo, is(instanceOf(NamedCacheClient.class)));

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
        @SessionName(SessionConfigurations.CLIENT_DEFAULT)
        private NamedCache<String, String> cacheOne;

        @Inject
        @SessionName(SessionConfigurations.CLIENT_TEST)
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

    private static Server s_server;
    }
