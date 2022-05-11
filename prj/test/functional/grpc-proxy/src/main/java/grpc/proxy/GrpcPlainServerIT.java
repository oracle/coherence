/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy;

import com.google.protobuf.Int32Value;

import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.grpc.NamedCacheServiceGrpc;
import com.oracle.coherence.grpc.Requests;

import com.oracle.coherence.grpc.proxy.GrpcServerController;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test that the plain gRPC Java {@link com.oracle.coherence.grpc.proxy.NamedCacheServiceGrpcImpl} service
 * deployed into a standard gRPC server.
 *
 * @author Jonathan Knight  2019.11.29
 * @since 20.06
 */
class GrpcPlainServerIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setup() throws IOException
        {
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.cluster", "NamedCacheServiceIT");
        System.setProperty("coherence.cacheconfig", "coherence-config.xml");
        System.setProperty("coherence.pof.config", "test-pof-config.xml");
        System.setProperty("coherence.override", "test-coherence-override.xml");
        System.setProperty(GrpcServerController.PROP_ENABLED, "true");

        DefaultCacheServer.startServerDaemon()
                .waitForServiceStart();

        s_ccf = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory(Classes.getContextClassLoader());

        s_channel = ManagedChannelBuilder
                .forAddress("127.0.0.1", GrpcServerController.INSTANCE.getPort())
                .usePlaintext()
                .build();
        }

    @AfterAll
    static void cleanup()
        {
        s_channel.shutdownNow();
        DefaultCacheServer.shutdown();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldAccessCaches() throws Exception
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = s_ccf.ensureCache(cacheName, Base.getContextClassLoader());
        cache.put("key-1", "value-1");
        cache.put("key-2", "value-2");
        cache.put("key-3", "value-3");

        NamedCacheServiceGrpc.NamedCacheServiceStub client   = NamedCacheServiceGrpc.newStub(s_channel);
        TestStreamObserver<Int32Value>              observer = new TestStreamObserver<>();

        client.size(Requests.size(Requests.DEFAULT_SCOPE, cacheName), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        Int32Value int32Value = observer.valueAt(0);
        assertThat(int32Value, is(notNullValue()));
        assertThat(int32Value.getValue(), is(cache.size()));
        }

    // ----- data members ---------------------------------------------------

    private static ConfigurableCacheFactory s_ccf;

    private static ManagedChannel s_channel;
    }
