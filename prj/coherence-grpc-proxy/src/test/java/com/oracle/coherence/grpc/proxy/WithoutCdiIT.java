/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.google.protobuf.Int32Value;

import com.oracle.coherence.grpc.Requests;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import io.helidon.grpc.server.GrpcRouting;
import io.helidon.grpc.server.GrpcServer;
import io.helidon.grpc.server.ServiceDescriptor;

import io.helidon.microprofile.grpc.client.GrpcProxyBuilder;

import io.helidon.microprofile.grpc.server.GrpcServiceBuilder;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test that the {@link NamedCacheService} can be deployed into a
 * Helidon gRPC server that is running without CDI.
 *
 * @author Jonathan Knight  2019.11.29
 * @since 14.1.2
 */
@Disabled("Tests disabled until Helidon changes to support this are committed")
class WithoutCdiIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.ttl",        "0");
        System.setProperty("coherence.cluster",    "NamedCacheServiceIT");
        System.setProperty("coherence.pof.config", "test-pof-config.xml");
        System.setProperty("coherence.override",   "test-coherence-override.xml");
        DefaultCacheServer.startServerDaemon().waitForServiceStart();

        s_ccf = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("coherence-cache-config.xml", null);

        NamedCacheService service = NamedCacheService.create();
        ServiceDescriptor descriptor = GrpcServiceBuilder.create(NamedCacheService.class, () -> service, null).build();
        GrpcRouting routing = GrpcRouting.builder().register(descriptor).build();
        s_server = GrpcServer.builder(routing).build();

        s_server.start().toCompletableFuture().join();

        s_channel = ManagedChannelBuilder
                .forAddress("localhost", 1408)
                .usePlaintext()
                .build();
        }

    @AfterAll
    static void cleanup()
        {
        s_server.shutdown().toCompletableFuture().join();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldDeployServiceWithoutCDI() throws Exception
        {
        String cacheName = "test-cache";
        NamedCache<String, String> cache = s_ccf.ensureCache(cacheName, Base.getContextClassLoader());
        cache.put("key-1", "value-1");
        cache.put("key-2", "value-2");
        cache.put("key-3", "value-3");

        NamedCacheClient client = GrpcProxyBuilder.create(s_channel, NamedCacheClient.class).build();
        Int32Value int32Value = client.size(Requests.size(cacheName)).toCompletableFuture().get();
        assertThat(int32Value, is(notNullValue()));
        assertThat(int32Value.getValue(), is(cache.size()));
        }

    // ----- data members ---------------------------------------------------

    private static ConfigurableCacheFactory s_ccf;

    private static GrpcServer s_server;

    private static ManagedChannel s_channel;
    }
