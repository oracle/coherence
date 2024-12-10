/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.version_0;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.v0.Requests;

import com.oracle.coherence.grpc.proxy.common.ConfigurableCacheFactorySuppliers;
import com.oracle.coherence.grpc.proxy.common.v0.NamedCacheService;

import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.NamedCache;
import com.tangosol.net.OperationalContext;

import com.tangosol.net.grpc.GrpcAcceptorController;
import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import java.util.concurrent.TimeUnit;

import java.util.stream.Stream;

import grpc.proxy.TestNamedCacheServiceProvider;
import grpc.proxy.TestStreamObserver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * An integration test for the key set methods of {@link NamedCacheService}.
 *
 * @author Jonathan Knight  2019.11.08
 * @since 20.06
 */
@SuppressWarnings({"resource", "unused"})
class KeySetIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.ttl",        "0");
        System.setProperty("coherence.cluster",    "KeySetIT");
        DefaultCacheServer.startServerDaemon().waitForServiceStart();

        String sConfigURI = "coherence-config.xml";

        s_ccf = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(sConfigURI, null);

        GrpcAcceptorController                controller = GrpcAcceptorController.discoverController();
        NamedCacheService.DefaultDependencies deps       = new NamedCacheService.DefaultDependencies(controller.getServerType());

        deps.setConfigurableCacheFactorySupplier(ConfigurableCacheFactorySuppliers.fixed(s_ccf));
        // set the transfer threshold small so that all of the cache data does not fit into one page
        deps.setTransferThreshold(100L);

        Optional<TestNamedCacheServiceProvider> optional = TestNamedCacheServiceProvider.getProvider();
        assertThat(optional.isPresent(), is(true));
        s_service = optional.get().getService(deps);
        }

    // ----- test methods ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnKeySetOfEmptyCache(String serializerName, Serializer serializer) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = s_ccf.ensureCache(sCacheName, null);
        cache.clear();

        Requests.page(GrpcDependencies.DEFAULT_SCOPE, sCacheName, "java", ByteString.EMPTY);
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        s_service.nextKeySetPage(Requests.page(GrpcDependencies.DEFAULT_SCOPE, sCacheName, serializerName, ByteString.EMPTY), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));

        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        List<BytesValue> list = observer.values();
        assertThat(list.get(0).getValue(), is(ByteString.EMPTY));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnKeySetOfCacheWithOneEntry(String serializerName, Serializer serializer) throws Exception
        {
        String cacheName = "test-cache";
        NamedCache<String, String> cache = s_ccf.ensureCache(cacheName, null);
        cache.clear();
        cache.put("key-1", "value-1");

        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        s_service.nextKeySetPage(Requests.page(GrpcDependencies.DEFAULT_SCOPE, cacheName, serializerName, ByteString.EMPTY), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));

        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(2);

        List<BytesValue> list = observer.values();
        assertThat(list.get(0), is(BytesValue.of(ByteString.EMPTY)));

        Binary bin = BinaryHelper.toBinary(list.get(1));
        Object result = ExternalizableHelper.fromBinary(bin, serializer);
        assertThat(result, is("key-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnKeySetOfCacheWithLotsOfEntries(String serializerName, Serializer serializer) throws Exception
        {
        String cacheName = "test-cache";
        NamedCache<String, String> cache = s_ccf.ensureCache(cacheName, null);
        cache.clear();
        for (int i = 0; i < 2000; i++)
            {
            cache.put("key-" + i, "value-" + i);
            }

        Set<String> keys = new HashSet<>();
        ByteString cookie = null;
        TestStreamObserver<BytesValue> observer = new TestStreamObserver<>();
        s_service.nextKeySetPage(Requests.page(GrpcDependencies.DEFAULT_SCOPE, cacheName, serializerName, ByteString.EMPTY), observer);

        while (cookie == null || !cookie.isEmpty())
            {
            assertThat(observer.await(1, TimeUnit.MINUTES), is(true));

            observer.assertComplete()
                    .assertNoErrors();

            LinkedList<BytesValue> list = new LinkedList<>(observer.values());
            BytesValue val = list.poll();
            assertThat(val, is(notNullValue()));
            cookie = val.getValue();
            assertThat(cookie, is(notNullValue()));

            list.stream()
                    .map(s -> (String) ExternalizableHelper.fromBinary(BinaryHelper.toBinary(s), serializer))
                    .forEach(keys::add);

            observer = new TestStreamObserver<>();
            s_service.nextKeySetPage(Requests.page(GrpcDependencies.DEFAULT_SCOPE, cacheName, "java", cookie), observer);
            }

        assertThat(keys.size(), is(cache.size()));
        assertThat(keys, is(cache.keySet()));
        }

    /**
     * Obtain the {@link com.tangosol.io.Serializer} instances to use for parameterized
     * test {@link org.junit.jupiter.params.provider.Arguments}.
     *
     * @return the {@link com.tangosol.io.Serializer} instances to use for test
     * {@link org.junit.jupiter.params.provider.Arguments}
     */
    static Stream<Arguments> serializers()
        {
        List<Arguments> args = new ArrayList<>();
        ClassLoader loader = Base.getContextClassLoader();

        args.add(Arguments.of("", new ConfigurablePofContext()));

        OperationalContext ctx = (OperationalContext) CacheFactory.getCluster();
        for (Map.Entry<String, SerializerFactory> entry : ctx.getSerializerMap().entrySet())
            {
            args.add(Arguments.of(entry.getKey(), entry.getValue().createSerializer(loader)));
            }

        return args.stream();
        }

    // ----- data members ---------------------------------------------------

    private static ConfigurableCacheFactory s_ccf;

    private static NamedCacheService s_service;
    }
