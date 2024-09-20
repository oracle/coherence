/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.version_0;

import com.google.protobuf.ByteString;
import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.grpc.ErrorsHelper;

import com.oracle.coherence.grpc.v0.Requests;
import com.oracle.coherence.grpc.messages.cache.v0.GetRequest;
import com.oracle.coherence.grpc.messages.cache.v0.OptionalValue;
import com.oracle.coherence.grpc.proxy.common.v0.NamedCacheService;
import com.oracle.coherence.grpc.services.cache.v0.NamedCacheServiceGrpc;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.NamedCache;
import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.util.Base;
import grpc.proxy.FindGrpcProxyPort;
import grpc.proxy.TestStreamObserver;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ManagedChannelProvider;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration tests to verify {@link NamedCacheService}.
 *
 * @author Jonathan Knight  2019.11.01
 * @since 20.06
 */
public class NamedCacheErrorHandlingIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.ttl",         "0");
        System.setProperty("coherence.wka",         "127.0.0.1");
        System.setProperty("coherence.localhost",   "127.0.0.1");
        System.setProperty("coherence.cluster",     "NamedCacheServiceIT");
        System.setProperty("coherence.cacheconfig", "coherence-config.xml");
        System.setProperty("coherence.pof.config",  "test-pof-config.xml");
        System.setProperty("coherence.override",    "test-coherence-override.xml");
        System.setProperty(GrpcDependencies.PROP_ENABLED, "true");

        DefaultCacheServer.startServerDaemon()
                .waitForServiceStart();

        s_ccf = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory(Classes.getContextClassLoader());

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
            throw new TestAbortedException("Test aborted, cannot load gRPC provider", e);
            }
        }

    @AfterAll
    static void cleanup()
        {
        if (s_channel != null)
            {
            s_channel.shutdownNow();
            }
        DefaultCacheServer.shutdown();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldHandleSerializationError() throws Exception
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = s_ccf.ensureCache(cacheName, Base.getContextClassLoader());
        cache.put("key-1", "value-1");

        NamedCacheServiceGrpc.NamedCacheServiceStub client   = NamedCacheServiceGrpc.newStub(s_channel);
        TestStreamObserver<OptionalValue>           observer = new TestStreamObserver<>();
        ByteString                                  badData  = ByteString.copyFrom("bad json", Charset.defaultCharset());
        GetRequest                                  request  = Requests.get(GrpcDependencies.DEFAULT_SCOPE, cacheName, "json", badData);

        client.get(request, observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));

        observer.assertError(t -> t instanceof StatusRuntimeException);

        StatusRuntimeException ex = (StatusRuntimeException) observer.getError();
        Optional<String> sStack = ErrorsHelper.getRemoteStack(ex);
        System.err.println("Remote exception stack trace:");
        System.err.println(sStack);
        assertThat(sStack, is(notNullValue()));
        }

    // ----- data members ---------------------------------------------------

    private static ConfigurableCacheFactory s_ccf;

    private static ManagedChannel s_channel;
    }
