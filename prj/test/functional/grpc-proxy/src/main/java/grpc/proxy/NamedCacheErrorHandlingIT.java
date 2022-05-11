/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy;

import com.google.protobuf.ByteString;
import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.grpc.ErrorsHelper;
import com.oracle.coherence.grpc.GetRequest;
import com.oracle.coherence.grpc.NamedCacheServiceGrpc;
import com.oracle.coherence.grpc.OptionalValue;
import com.oracle.coherence.grpc.Requests;
import com.oracle.coherence.grpc.proxy.GrpcServerController;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Base;
import com.tangosol.util.Filters;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration tests to verify {@link com.oracle.coherence.grpc.proxy.NamedCacheServiceImpl}.
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
    public void shouldHandleSerializationError() throws Exception
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = s_ccf.ensureCache(cacheName, Base.getContextClassLoader());
        cache.put("key-1", "value-1");

        NamedCacheServiceGrpc.NamedCacheServiceStub client   = NamedCacheServiceGrpc.newStub(s_channel);
        TestStreamObserver<OptionalValue>           observer = new TestStreamObserver<>();
        ByteString                                  badData  = ByteString.copyFrom("bad json", Charset.defaultCharset());
        GetRequest                                  request  = Requests.get(Requests.DEFAULT_SCOPE, cacheName, "json", badData);

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
