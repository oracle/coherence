/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.Entry;
import com.oracle.coherence.grpc.MapEventResponse;
import com.oracle.coherence.grpc.MapListenerResponse;

import com.oracle.coherence.grpc.proxy.common.NamedCacheService;

import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.OperationalContext;

import com.tangosol.net.cache.CacheEvent;

import com.tangosol.net.grpc.GrpcDependencies;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapIndex;
import com.tangosol.util.MapListener;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.ValueExtractor;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.TestObserver;

import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.params.provider.Arguments;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import java.util.concurrent.ConcurrentHashMap;

import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.jupiter.api.Assertions.fail;

import static org.mockito.Mockito.mock;

public abstract class BaseNamedCacheServiceImplIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    protected static void setup()
        {
        System.setProperty("coherence.ttl",         "0");
        System.setProperty("coherence.wka",         "127.0.0.1");
        System.setProperty("coherence.cluster",     "NamedCacheServiceImplIT");
        System.setProperty("coherence.cacheconfig", "coherence-config.xml");
        System.setProperty("coherence.pof.config",  "test-pof-config.xml");
        System.setProperty("coherence.override",    "test-coherence-override.xml");

        s_ccfDefault = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(null);
        DefaultCacheServer.startServerDaemon(s_ccfDefault).waitForServiceStart();

        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies();
        deps.setConfigurableCacheFactorySupplier(BaseNamedCacheServiceImplIT::ensureCCF);
        Optional<TestNamedCacheServiceProvider> optional = TestNamedCacheServiceProvider.getProvider();
        assertThat(optional.isPresent(), is(true));
        s_service = optional.get().getService(deps);
        }

    // ----- inner class: CollectingMapListener -----------------------------

    @SuppressWarnings("unchecked")
    protected static class CollectingMapListener<K, V>
            extends TestObserver<MapEvent<K, V>>
            implements MapListener<K, V>
        {

        public CollectingMapListener()
            {
            onSubscribe(mock(Disposable.class));
            }

        @Override
        public void entryInserted(MapEvent<K, V> mapEvent)
            {
            onNext(toSimpleEvent(mapEvent));
            }

        @Override
        public void entryUpdated(MapEvent<K, V> mapEvent)
            {
            onNext(toSimpleEvent(mapEvent));
            }

        @Override
        public void entryDeleted(MapEvent<K, V> mapEvent)
            {
            onNext(toSimpleEvent(mapEvent));
            }

        @SuppressWarnings("rawtypes")
        protected MapEvent<K, V> toSimpleEvent(MapEvent<K, V> event)
            {
            if (event instanceof CacheEvent)
                {
                CacheEvent<K, V> ce = (CacheEvent) event;
                return new CacheEvent<>(ce.getMap(), ce.getId(), ce.getKey(), ce.getOldValue(), ce.getNewValue(), ce.isSynthetic(), ce.getTransformationState(), ce.isPriming());
                }
            return new MapEvent<>(event.getMap(), event.getId(), event.getKey(), event.getOldValue(), event.getNewValue());
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtain the specified {@link NamedCache}.
     *
     * @param <K>     the type of the cache keys
     * @param <V>     the type of the cache values
     *
     * @param sScope  the scope name for the cache
     * @param name    the cache name
     * @param loader  the {@link ClassLoader} to use to obtain the cache
     * @return the specified {@link NamedCache}
     */
    protected <K, V> NamedCache<K, V> ensureCache(String sScope, String name, ClassLoader loader)
        {
        ConfigurableCacheFactory ccf = BaseNamedCacheServiceImplIT.ensureCCF(sScope);
        return ccf.ensureCache(name, loader);
        }

    /**
     * Destroy the specified {@link NamedCache}.
     *
     * @param sScope  the scope name for the cache
     * @param cache   the cache to destroy
     */
    protected void destroyCache(String sScope, NamedCache<?, ?> cache)
        {
        ConfigurableCacheFactory ccf = BaseNamedCacheServiceImplIT.ensureCCF(sScope);
        ccf.destroyCache(cache);
        }

    /**
     * Create an instance of the {@link NamedCacheService} to use for testing.
     *
     * @return an instance of the {@link NamedCacheService} to use for testing
     */
    protected NamedCacheService createService()
        {
        return s_service;
        }

    protected static ConfigurableCacheFactory ensureCCF(String sScope)
        {
        if (GrpcDependencies.DEFAULT_SCOPE.equals(sScope))
            {
            return s_ccfDefault;
            }
        return s_mapCCF.computeIfAbsent(sScope, BaseNamedCacheServiceImplIT::createCCF);
        }

    protected static ConfigurableCacheFactory createCCF(String sScope)
        {
        ClassLoader loader = Base.getContextClassLoader();
        XmlElement xmlConfig = XmlHelper.loadFileOrResource("coherence-config.xml", "Cache Configuration", loader);

        ExtensibleConfigurableCacheFactory.Dependencies deps = ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance(xmlConfig, loader, "test-pof-config.xml", sScope, null);
        ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(deps);
        eccf.activate();
        return eccf;
        }

    protected static Stream<Arguments> getTestScopes()
        {
        return Stream.of(Arguments.of(GrpcDependencies.DEFAULT_SCOPE), Arguments.of("one"));
        }

    protected static String[] getTestScopeNames()
        {
        return getTestScopes()
                .map(arg -> (String) arg.get()[0])
                .toArray(String[]::new);
        }

    /**
     * Obtain the {@link Serializer} and {@code Scope} instances to use for
     * parameterized test {@link Arguments}.
     *
     * @return the {@link Serializer} and {@code Scope} instances to use
     * for test {@link Arguments}
     */
    protected static Stream<Arguments> serializers()
        {
        OperationalContext          ctx    = (OperationalContext) CacheFactory.getCluster();
        TreeMap<String, Serializer> map    = new TreeMap<>();
        ClassLoader                 loader = Base.getContextClassLoader();

        map.put("", new ConfigurablePofContext());

        for (Map.Entry<String, SerializerFactory> entry : ctx.getSerializerMap().entrySet())
            {
            map.put(entry.getKey(), entry.getValue().createSerializer(loader));
            }

        List<Arguments> list = new ArrayList<>();
        for (String sScope : getTestScopeNames())
            {
            for (Map.Entry<String, Serializer> entry : map.entrySet())
                {
                list.add(Arguments.of(entry.getKey(), entry.getValue(), sScope));
                }
            }

        return list.stream();
        }



    protected <K, V> List<MapEvent<K, V>> toMapEvents(ObservableMap<K, V> map, List<MapListenerResponse> responses,
                                                      Serializer serializer)
        {
        return toMapEventsForFilterId(map, responses, serializer, -1);
        }

    protected <K, V> List<MapEvent<K, V>> toMapEventsForFilterId(ObservableMap<K, V> map,
                                                                 List<MapListenerResponse> responses,
                                                                 Serializer serializer, long filterId)
        {
        List<MapEvent<K, V>> events = new ArrayList<>();
        for (MapListenerResponse response : responses)
            {
            if (response.getResponseTypeCase() == MapListenerResponse.ResponseTypeCase.EVENT)
                {
                MapEventResponse event = response.getEvent();
                assertThat(event, is(notNullValue()));

                if (filterId <= 0 || event.getFilterIdsList().contains(filterId))
                    {
                    events.add(new MapEvent<>(map,
                                              event.getId(),
                                              BinaryHelper.fromByteString(event.getKey(), serializer),
                                              BinaryHelper.fromByteString(event.getOldValue(), serializer),
                                              BinaryHelper.fromByteString(event.getNewValue(), serializer)));
                    }
                }
            }
        return events;
        }

    /**
     * Assert at two lists of {@link MapEvent}s are equal.
     * <p>
     * This is required because {@link MapEvent} does not implement
     * an equals() method.
     *
     * @param actual   the actual list of events
     * @param expected the expected list of events
     * @param <K>      the type of the event key
     * @param <V>      the type of the event value
     */
    protected <K, V> void assertEqual(List<MapEvent<K, V>> actual, List<MapEvent<K, V>> expected)
        {
        if (actual.size() != expected.size())
            {
            fail("Event lists do not match:\nexpected: " + expected + "\nactual: " + actual);
            }

        for (int i = 0; i < actual.size(); i++)
            {
            assertEqual(actual.get(i), expected.get(i));
            }
        }

    /**
     * Assert at two {@link MapEvent}s are equal.
     * <p>
     * This is required because {@link MapEvent} does not implement
     * an equals() method.
     *
     * @param actual   the actual event
     * @param expected the expected event
     * @param <K>      the type of the event key
     * @param <V>      the type of the event value
     */
    protected <K, V> void assertEqual(MapEvent<K, V> actual, MapEvent<K, V> expected)
        {
        assertThat(actual.getId(), is(expected.getId()));
        assertThat(actual.getKey(), is(expected.getKey()));
        assertThat(actual.getOldValue(), is(expected.getOldValue()));
        assertThat(actual.getNewValue(), is(expected.getNewValue()));
        }

    /**
     * Obtain the specified {@link NamedCache}.
     *
     * @param <K>  the type of the cache keys
     * @param <V>  the type of the cache values
     *
     * @param sScope  the scope name of the cache
     * @param name    the cache name
     * @return the specified {@link NamedCache}
     */
    protected <K, V> NamedCache<K, V> ensureEmptyCache(String sScope, String name)
        {
        NamedCache<K, V> cache = ensureCache(sScope, name, Base.getContextClassLoader());
        cache.clear();
        return cache;
        }

    /**
     * Clear the specified cache and populate it with entries.
     *
     * @param cache the cache to clear and populate
     * @param count the number of entries to add to the cache
     */
    protected void clearAndPopulate(NamedCache<String, String> cache, int count)
        {
        cache.clear();
        for (int i = 1; i <= count; i++)
            {
            cache.put("key-" + i, "value-" + i);
            }
        }

    /**
     * Serialize the specified value using the {@link Serializer}
     * and convert the serialized data to a {@link ByteString}.
     *
     * @param value      the value to serialize
     * @param serializer the {@link Serializer} to use
     *
     * @return the serialized value as a {@link ByteString}
     */
    protected ByteString toByteString(Object value, Serializer serializer)
        {
        return BinaryHelper.toByteString(ExternalizableHelper.toBinary(value, serializer));
        }

    /**
     * Deserialize the bytes in the {@link BytesValue}.
     *
     * @param bytesValue the {@link BytesValue} containing the serialized value
     * @param serializer the {@link Serializer} to use to deserialize the value
     * @param type       the type of the deserialized value
     * @param <T>        the type of the deserialized value
     *
     * @return the value deserialized from the {@link BytesValue}
     */
    @SuppressWarnings("SameParameterValue")
    protected <T> T fromBytesValue(BytesValue bytesValue, Serializer serializer, Class<T> type)
        {
        return fromByteString(bytesValue.getValue(), serializer, type);
        }

    /**
     * Deserialize the bytes in the {@link ByteString}.
     *
     * @param byteString the {@link ByteString} containing the serialized value
     * @param serializer the {@link Serializer} to use to deserialize the value
     * @param type       the type of the deserialized value
     * @param <T>        the type of the deserialized value
     *
     * @return the value deserialized from the {@link ByteString}
     */
    protected <T> T fromByteString(ByteString byteString, Serializer serializer, Class<T> type)
        {
        if (byteString == null || byteString.isEmpty())
            {
            return null;
            }
        return ExternalizableHelper.fromBinary(BinaryHelper.toBinary(byteString), serializer, type);
        }

    /**
     * Convert a list of {@link Entry} instances into a {@link Map}.
     *
     * @param list       the list of {@link Entry} instances
     * @param serializer the serializer to use to deserialize the keys and values
     * @param <K>        the type of the entry key
     * @param <V>        the type of te entry value
     *
     * @return the {@link Map} of entries
     */
    protected <K, V> LinkedHashMap<K, V> toMap(List<Entry> list, Serializer serializer)
        {
        LinkedHashMap<K, V> map = new LinkedHashMap<>();
        for (Entry entry : list)
            {
            map.put(BinaryHelper.fromByteString(entry.getKey(), serializer),
                    BinaryHelper.fromByteString(entry.getValue(), serializer));
            }
        return map;
        }

    /**
     * Convert a list of {@link Entry} instances into a {@link List} of {@link Map.Entry}
     * instances with deserialized keys and values.
     *
     * @param list       the list of {@link Entry} instances
     * @param serializer the serializer to use to deserialize the keys and values
     * @param <K>        the type of the entry key
     * @param <V>        the type of te entry value
     *
     * @return the {@link Map} of entries
     */
    protected <K, V> List<Map.Entry<K, V>> toList(List<Entry> list, Serializer serializer)
        {
        LinkedHashMap<K, V> map = new LinkedHashMap<>();
        for (Entry entry : list)
            {
            map.put(BinaryHelper.fromByteString(entry.getKey(), serializer),
                    BinaryHelper.fromByteString(entry.getValue(), serializer));
            }
        return new ArrayList<>(map.entrySet());
        }

    /**
     * Remove all of the indexes from the cache and return its index map.
     *
     * @param cache the cache to remove indexes from
     *
     * @return the cache's index map
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Map<ValueExtractor, MapIndex> removeIndexes(NamedCache cache)
        {
        cache.clear();

        BackingMapContext ctx = cache.getCacheService()
                .getBackingMapManager()
                .getContext()
                .getBackingMapContext(cache.getCacheName());

        Map<ValueExtractor, MapIndex> indexMap = ctx.getIndexMap();
        for (Map.Entry<ValueExtractor, MapIndex> entry : indexMap.entrySet())
            {
            cache.removeIndex(entry.getKey());
            }
        return indexMap;
        }

    // ----- data members ---------------------------------------------------

    private static ConfigurableCacheFactory s_ccfDefault;

    private static NamedCacheService s_service;

    private final static Map<String, ConfigurableCacheFactory> s_mapCCF = new ConcurrentHashMap<>();
    }
