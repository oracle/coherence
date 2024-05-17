/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.version_0;

import com.google.protobuf.Int32Value;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.grpc.v0.Requests;
import com.oracle.coherence.grpc.services.cache.v0.NamedCacheServiceGrpc;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;
import com.tangosol.net.Session;
import com.tangosol.net.grpc.GrpcDependencies;
import grpc.proxy.FindGrpcProxyPort;
import grpc.proxy.IsGrpcProxyRunning;
import grpc.proxy.TestStreamObserver;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ManagedChannelProvider;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class GrpcProxyRestartIT
    {
    @BeforeAll
    static void setup() throws Exception
        {
        String sCluster = "GrpcProxyRestartIT";

        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.cluster", sCluster);
        System.setProperty("coherence.cacheconfig", "coherence-config.xml");
        System.setProperty("coherence.pof.config", "test-pof-config.xml");
        System.setProperty("coherence.override", "test-coherence-override.xml");
        System.setProperty(GrpcDependencies.PROP_ENABLED, "true");

        int nPort = LocalPlatform.get().getAvailablePorts().next();
        System.setProperty("coherence.grpc.server.port", String.valueOf(nPort));

        s_coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);

        s_session = s_coherence.getSession();
        }

    @AfterAll
    static void cleanup()
        {
        Coherence.closeAll();
        }

    @Test
    public void shouldRestartProxy() throws Exception
        {
        try
            {
            String sCacheName = "test-cache";

            MatcherAssert.assertThat(IsGrpcProxyRunning.locally(), is(true));

            ManagedChannel channel = createChannel();
            NamedCacheServiceGrpc.NamedCacheServiceStub client = NamedCacheServiceGrpc.newStub(channel);
            assertThat(getCacheSize(client, sCacheName), is(0));

            NamedCache<String, String> cache = s_session.getCache(sCacheName);
            cache.put("key-1", "value-1");
            cache.put("key-2", "value-2");
            cache.put("key-3", "value-3");

            assertThat(getCacheSize(client, sCacheName), is(cache.size()));

            Service service = s_coherence.getCluster().getService(GrpcDependencies.SCOPED_PROXY_SERVICE_NAME);
            assertThat(service.isRunning(), is(true));

            service.stop();

            Eventually.assertDeferred(service::isRunning, is(true));
            assertThat(getCacheSize(client, sCacheName), is(cache.size()));
            }
        catch (ManagedChannelProvider.ProviderNotFoundException e)
            {
            throw new TestAbortedException("Test aborted, cannot load gRPC provider", e);
            }
        }

    private ManagedChannel createChannel()
        {
        int nPort = FindGrpcProxyPort.local();
        return ManagedChannelBuilder
                .forAddress("127.0.0.1", nPort)
                .usePlaintext()
                .build();
        }

    private int getCacheSize(NamedCacheServiceGrpc.NamedCacheServiceStub client, String sCacheName) throws Exception
        {
        TestStreamObserver<Int32Value> observer = new TestStreamObserver<>();

        client.size(Requests.size(GrpcDependencies.DEFAULT_SCOPE, sCacheName), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));

        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        Int32Value int32Value = observer.valueAt(0);
        assertThat(int32Value, is(notNullValue()));
        return int32Value.getValue();
        }

    // ----- data members ---------------------------------------------------

    private static Coherence s_coherence;

    private static Session   s_session;
    }
