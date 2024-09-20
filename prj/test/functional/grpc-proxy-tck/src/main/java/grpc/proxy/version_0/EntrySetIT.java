/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.version_0;

import com.google.protobuf.ByteString;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.v0.Requests;

import com.oracle.coherence.grpc.messages.cache.v0.EntryResult;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import grpc.proxy.TestNamedCacheServiceProvider;
import grpc.proxy.TestStreamObserver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * An integration test for the entry set methods of {@link NamedCacheService}.
 *
 * @author Jonathan Knight  2019.11.08
 * @since 20.06
 */
@SuppressWarnings("resource")
class EntrySetIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.ttl",        "0");
        System.setProperty("coherence.cluster",    "EntrySetIT");
        DefaultCacheServer.startServerDaemon().waitForServiceStart();

        s_ccf     = CacheFactory.getCacheFactoryBuilder()
                                .getConfigurableCacheFactory("coherence-config.xml", null);

        GrpcAcceptorController                controller = GrpcAcceptorController.discoverController();
        NamedCacheService.DefaultDependencies deps       = new NamedCacheService.DefaultDependencies(controller.getServerType());
        deps.setConfigurableCacheFactorySupplier(ConfigurableCacheFactorySuppliers.fixed(s_ccf));
        // set the transfer threshold small so that all the cache data does not fit into one page
        deps.setTransferThreshold(100L);

        Optional<TestNamedCacheServiceProvider> optional = TestNamedCacheServiceProvider.getProvider();
        assertThat(optional.isPresent(), is(true));
        s_service = optional.get().getService(deps);
        }
    
    // ----- test methods ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnEntrySetOfEmptyCache(String serializerName, Serializer serializer) throws Exception
        {
        String           sCacheName = "test-cache";
        NamedCache<?, ?> cache      = s_ccf.ensureCache(sCacheName, null);
        cache.clear();

        Requests.page(GrpcDependencies.DEFAULT_SCOPE, sCacheName, serializerName, ByteString.EMPTY);

        TestStreamObserver<EntryResult> observer = new TestStreamObserver<>();
        s_service.nextEntrySetPage(Requests.page(GrpcDependencies.DEFAULT_SCOPE, sCacheName, serializerName, ByteString.EMPTY), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));

        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        EntryResult result = observer.values().get(0);
        assertThat(result,             is(notNullValue()));
        assertThat(result.getKey(),    is(ByteString.EMPTY));
        assertThat(result.getValue(),  is(ByteString.EMPTY));
        assertThat(result.getCookie(), is(ByteString.EMPTY));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnEntrySetOfCacheWithOneEntry(String serializerName, Serializer serializer) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = s_ccf.ensureCache(sCacheName, null);

        cache.clear();
        cache.put("key-1", "value-1");

        Requests.page(GrpcDependencies.DEFAULT_SCOPE, sCacheName, serializerName, ByteString.EMPTY);

        TestStreamObserver<EntryResult> observer = new TestStreamObserver<>();
        s_service.nextEntrySetPage(Requests.page(GrpcDependencies.DEFAULT_SCOPE, sCacheName, serializerName, ByteString.EMPTY), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));

        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(2);

        List<EntryResult> list = observer.values();

        EntryResult cookieEntry = list.get(0);
        assertThat(cookieEntry,             is(notNullValue()));
        assertThat(cookieEntry.getKey(),    is(ByteString.EMPTY));
        assertThat(cookieEntry.getValue(),  is(ByteString.EMPTY));
        assertThat(cookieEntry.getCookie(), is(ByteString.EMPTY));

        EntryResult cacheEntry = list.get(1);
        assertThat(cacheEntry,             is(notNullValue()));
        assertThat(cacheEntry.getKey(),    is(not(ByteString.EMPTY)));
        assertThat(cacheEntry.getValue(),  is(not(ByteString.EMPTY)));
        assertThat(cacheEntry.getCookie(), is(ByteString.EMPTY));

        Binary binKey = BinaryHelper.toBinary(cacheEntry.getKey());
        String key    = ExternalizableHelper.fromBinary(binKey, serializer);
        assertThat(key, is("key-1"));

        Binary binValue = BinaryHelper.toBinary(cacheEntry.getValue());
        String value    = ExternalizableHelper.fromBinary(binValue, serializer);
        assertThat(value, is("value-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldReturnEntrySetOfCacheWithLotsOfEntries(String serializerName, Serializer serializer) throws Exception
        {
        String                     sCacheName = "test-cache";
        NamedCache<String, String> cache      = s_ccf.ensureCache(sCacheName, null);

        cache.clear();
        for (int i = 0; i < 2000; i++)
            {
            cache.put("key-" + i, "value-" + i);
            }

        HashMap<String, String> entries = new HashMap<>();
        ByteString              cookie  = null;
        Requests.page(GrpcDependencies.DEFAULT_SCOPE, sCacheName, serializerName, ByteString.EMPTY);

        TestStreamObserver<EntryResult> observer = new TestStreamObserver<>();
        s_service.nextEntrySetPage(Requests.page(GrpcDependencies.DEFAULT_SCOPE, sCacheName, serializerName, ByteString.EMPTY), observer);

        while (cookie == null || !cookie.isEmpty())
            {
            assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
            observer.assertComplete()
                    .assertNoErrors();

            LinkedList<EntryResult> list = new LinkedList<>(observer.values());
            assertThat(list.isEmpty(), is(false));
            EntryResult cookieResult = list.poll();
            assertThat(cookieResult, is(notNullValue()));
            cookie = cookieResult.getCookie();
            assertThat(cookie, is(notNullValue()));

            for (EntryResult entry : list)
                {
                String sKey   = ExternalizableHelper.fromBinary(BinaryHelper.toBinary(entry.getKey()), serializer);
                String sValue = ExternalizableHelper.fromBinary(BinaryHelper.toBinary(entry.getValue()), serializer);
                entries.put(sKey, sValue);
                }

            observer = new TestStreamObserver<>();
            s_service.nextEntrySetPage(Requests.page(GrpcDependencies.DEFAULT_SCOPE, sCacheName, serializerName, cookie), observer);
            }

        assertThat(entries.size(),     is(cache.size()));
        assertThat(entries.entrySet(), is(cache.entrySet()));
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
        List<Arguments> args   = new ArrayList<>();
        ClassLoader     loader = Base.getContextClassLoader();

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
