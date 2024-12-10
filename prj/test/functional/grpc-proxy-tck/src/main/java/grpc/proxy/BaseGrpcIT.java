/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy;

import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;

import com.oracle.coherence.common.base.Classes;

import com.oracle.coherence.grpc.BinaryHelper;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;

import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.Session;

import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.cache.CacheEvent;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapIndex;
import com.tangosol.util.MapListener;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ValueExtractor;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.provider.Arguments;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 * A base class for gRPC proxy integration tests.
 */
public abstract class BaseGrpcIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    protected static void setup(TestInfo info) throws Exception
        {
        System.setProperty("coherence.ttl",         "0");
        System.setProperty("coherence.wka",         "127.0.0.1");
        System.setProperty("coherence.cluster",     "BaseGrpcIT");
        System.setProperty("coherence.cacheconfig", "coherence-config.xml");
        System.setProperty("coherence.pof.config",  "test-pof-config.xml");
        System.setProperty("coherence.override",    "test-coherence-override.xml");
        System.setProperty("coherence.serializer",  "pof");

        s_scopeNames = null;
        info.getTestClass().ifPresent(clz ->
            {
            s_scopeNames = new ArrayList<>();
            if (clz.isAnnotationPresent(WithNullScopeName.class))
                {
                s_scopeNames.add(null);
                }
            WithScopeNames withScopeNames = clz.getAnnotation(WithScopeNames.class);
            if (withScopeNames != null)
                {
                s_scopeNames.addAll(List.of(withScopeNames.scopes()));
                }
            else
                {
                s_scopeNames.add(Coherence.DEFAULT_SCOPE);
                s_scopeNames.add("one");
                }
            });

        CoherenceConfiguration.Builder builder = CoherenceConfiguration.builder()
                .discoverSessions();

        Arrays.stream(getTestScopeNames())
                .filter(Objects::nonNull)
                .forEach(sScope ->
                    {
                    builder.withSession(SessionConfiguration.builder()
                            .named(sScope)
                            .withScopeName(sScope)
                            .withConfigUri("coherence-config.xml")
                            .build());
                    });

        Coherence.clusterMember(builder.build()).start().get(5, TimeUnit.MINUTES);
        s_ccfDefault = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(null);
        }

    @AfterAll
    public static void cleanup()
        {
        Coherence.closeAll();
        }

    // ----- helper methods -------------------------------------------------


    protected static Stream<Arguments> getTestScopes()
        {
        if (s_scopeNames == null || s_scopeNames.isEmpty())
            {
            return Stream.of(Arguments.of(Coherence.DEFAULT_SCOPE), Arguments.of("one"));
            }
        return s_scopeNames.stream()
                .map(s -> new Object[]{s})
                .map(Arguments::of);
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
        ClassLoader                 loader = Classes.getContextClassLoader();

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
     * Remove all the indexes from the cache and return its index map.
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

    /**
     * Obtain the specified {@link NamedCache}.
     *
     * @param scope   the scope name for the cache
     * @param name    the cache name
     * @param loader  the {@link ClassLoader} to use to obtain the cache
     * @param <K>     the type of the cache keys
     * @param <V>     the type of the cache values
     *
     * @return the specified {@link NamedCache}
     */
    protected <K, V> NamedCache<K, V> ensureCache(String scope, String name, ClassLoader loader)
        {
        ConfigurableCacheFactory ccf = ensureCCF(scope);
        return ccf.ensureCache(name, loader);
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
     * Obtain the specified {@link NamedCache} and ensure it is empty.
     *
     * @param scope   the scope name for the cache
     * @param name    the cache name
     * @param loader  the {@link ClassLoader} to use to obtain the cache
     * @param <K>     the type of the cache keys
     * @param <V>     the type of the cache values
     *
     * @return the specified {@link NamedCache}
     */
    protected <K, V> NamedCache<K, V> ensureEmptyCache(String scope, String name, ClassLoader loader)
        {
        NamedCache<K, V> cache = ensureCache(scope, name, loader);
        cache.truncate();
        return cache;
        }


    /**
     * Obtain the specified pass-thru {@link NamedCache}.
     *
     * @param scope  the scope name for the cache
     * @param name   the cache name
     *
     * @return the specified {@link NamedCache}
     */
    protected NamedCache<Binary, Binary> ensurePassThruCache(String scope, String name)
        {
        ConfigurableCacheFactory ccf = ensureCCF(scope);
        return ccf.ensureCache(name, NullImplementation.getClassLoader());
        }

    /**
     * Obtain the specified pass-thru {@link NamedCache} and ensure it is empty.
     *
     * @param sScope  the scope name for the cache
     * @param name    the cache name
     *
     * @return the specified {@link NamedCache}
     */
    protected NamedCache<Binary, Binary> ensureEmptyPassThruCache(String sScope, String name)
        {
        NamedCache<Binary, Binary> cache = ensurePassThruCache(sScope, name);
        cache.truncate();
        return cache;
        }

    protected String ensureScopeName(String sName)
        {
        return sName == null ? Coherence.DEFAULT_SCOPE : sName;
        }

    /**
     * Destroy the specified {@link NamedCache}.
     *
     * @param sScope  the scope name for the cache
     * @param cache   the cache to destroy
     */
    protected void destroyCache(String sScope, NamedCache<?, ?> cache)
        {
        ConfigurableCacheFactory ccf = ensureCCF(sScope);
        ccf.destroyCache(cache);
        }

    protected ConfigurableCacheFactory ensureCCF(String sScopeOrNull)
        {
        String sScope = ensureScopeName(sScopeOrNull);
        Optional<Session> optional = Coherence.findSession(sScope);
        if (optional.isPresent())
            {
            return ((ConfigurableCacheFactorySession) optional.get()).getConfigurableCacheFactory();
            }

        if (Coherence.DEFAULT_SCOPE.equals(sScope))
            {
            return s_ccfDefault;
            }
        return s_mapCCF.computeIfAbsent(sScope, BaseGrpcIT::createCCF);
        }

    protected static ConfigurableCacheFactory createCCF(String sScope)
        {
        ClassLoader loader    = Classes.getContextClassLoader();
        XmlElement  xmlConfig = XmlHelper.loadFileOrResource("coherence-config.xml", "Cache Configuration", loader);

        ExtensibleConfigurableCacheFactory.Dependencies deps = ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance(xmlConfig, loader, "test-pof-config.xml", sScope, null);
        ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(deps);
        eccf.activate();
        return eccf;
        }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WithScopeNames
        {
        String[] scopes() default {};
        }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WithNullScopeName
        {
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

        /**
         * Return the number of values received.
         *
         * @return the number of values received
         */
        public int count()
            {
            return values.size();
            }

        public List<MapEvent<K, V>> safeValues()
            {
            lock.lock();
            try
                {
                return new ArrayList<>(values);
                }
            finally
                {
                lock.unlock();
                }
            }

        @Override
        public void onNext(@NonNull MapEvent<K, V> kvMapEvent)
            {
            lock.lock();
            try
                {
                super.onNext(kvMapEvent);
                }
            finally
                {
                lock.unlock();
                }
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

        // ----- data members -----------------------------------------------

        private final Lock lock = new ReentrantLock();
        }

    // ----- data members ---------------------------------------------------

    protected static ConfigurableCacheFactory s_ccfDefault;

    protected final static Map<String, ConfigurableCacheFactory> s_mapCCF = new ConcurrentHashMap<>();

    private static List<String> s_scopeNames;
    }
