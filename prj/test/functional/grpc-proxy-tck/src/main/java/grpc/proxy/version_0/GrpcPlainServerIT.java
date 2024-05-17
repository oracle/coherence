/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.version_0;

import com.google.protobuf.Int32Value;

import com.oracle.bedrock.junit.BootstrapCoherence;

import com.oracle.coherence.grpc.v0.Requests;

import com.oracle.coherence.grpc.services.cache.v0.NamedCacheServiceGrpc;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;

import com.tangosol.net.Session;

import com.tangosol.net.grpc.GrpcDependencies;

import grpc.proxy.FindGrpcProxyPort;
import grpc.proxy.TestStreamObserver;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ManagedChannelProvider;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.opentest4j.TestAbortedException;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test that the plain gRPC Java named cache service
 * deployed into a standard gRPC server.
 *
 * @author Jonathan Knight  2019.11.29
 * @since 20.06
 */
@BootstrapCoherence
class GrpcPlainServerIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setup() throws Exception
        {
        String sCluster = "GrpcPlainServerIT";

        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.cluster", sCluster);
        System.setProperty("coherence.cacheconfig", "coherence-config.xml");
        System.setProperty("coherence.pof.config", "test-pof-config.xml");
        System.setProperty("coherence.override", "test-coherence-override.xml");
        System.setProperty(GrpcDependencies.PROP_ENABLED, "true");

        Coherence coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);

        s_session = coherence.getSession();

        int nPort = FindGrpcProxyPort.local();
        try
            {
            s_channel = ManagedChannelBuilder
                    .forAddress("127.0.0.1", nPort)
                    .usePlaintext()
                    .build();
            }
        catch (ManagedChannelProvider.ProviderNotFoundException e)
            {
            throw new TestAbortedException("Test aborted, cannot load client", e);
            }
        }

    @AfterAll
    static void cleanup()
        {
        if (s_channel != null)
            {
            s_channel.shutdownNow();
            }
        Coherence.closeAll();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldAccessCaches() throws Exception
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = s_session.getCache(cacheName);
        cache.put("key-1", "value-1");
        cache.put("key-2", "value-2");
        cache.put("key-3", "value-3");

        NamedCacheServiceGrpc.NamedCacheServiceStub client   = NamedCacheServiceGrpc.newStub(s_channel);
        TestStreamObserver<Int32Value>              observer = new TestStreamObserver<>();

        client.size(Requests.size(GrpcDependencies.DEFAULT_SCOPE, cacheName), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        Int32Value int32Value = observer.valueAt(0);
        assertThat(int32Value, is(notNullValue()));
        assertThat(int32Value.getValue(), is(cache.size()));
        }

    // ----- data members ---------------------------------------------------

    private static Session s_session;

    private static ManagedChannel s_channel;
    }
