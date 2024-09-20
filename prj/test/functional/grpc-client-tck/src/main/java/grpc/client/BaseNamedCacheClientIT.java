/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import com.tangosol.io.Serializer;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.NamedCache;

import com.tangosol.net.grpc.GrpcDependencies;

import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.comparator.SafeComparator;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.UniversalExtractor;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Comparator;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.collection.IsMapContaining.hasKey;

/**
 * An abstract class of integration tests to verify the gRPC client.
 * <p>
 * These tests are run by subclasses that can configure the gRPC client
 * in different ways.
 *
 * @author Jonathan Knight  2019.11.25
 * @since 20.06
 */
@SuppressWarnings({"rawtypes", "resource"})
abstract class BaseNamedCacheClientIT
        extends AbstractGrpcClientIT
    {
    protected BaseNamedCacheClientIT(ServerHelper serverHelper)
        {
        this(serverHelper, GrpcDependencies.DEFAULT_SCOPE);
        }

    public BaseNamedCacheClientIT(ServerHelper serverHelper, String sScopeName)
        {
        super(sScopeName);
        f_serverHelper = serverHelper;
        }

    // ----- test methods ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldAddIndex(String sSerializerName, Serializer serializer)
        {
        String                          cacheName = "add-index-cache";
        NamedCache                      cache     = ensureCache(cacheName);
        Map<ValueExtractor, MapIndex>   indexMap  = removeIndexes(cache);
        ValueExtractor<String, Integer> extractor = new UniversalExtractor<>("length()");

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        grpcClient.addIndex(extractor, false, null);

        assertThat(indexMap, hasKey(extractor));
        assertThat(indexMap.get(extractor).isOrdered(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldAddSortedIndex(String sSerializerName, Serializer serializer)
        {
        String                          cacheName = "add-sorted-index-cache";
        NamedCache                      cache     = ensureCache(cacheName);
        Map<ValueExtractor, MapIndex>   indexMap  = removeIndexes(cache);
        ValueExtractor<String, Integer> extractor = new UniversalExtractor<>("length()");

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        grpcClient.addIndex(extractor, true, null);

        assertThat(indexMap, hasKey(extractor));
        assertThat(indexMap.get(extractor).isOrdered(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @SuppressWarnings("unchecked")
    void shouldAddSortedIndexWithComparator(String sSerializerName, Serializer serializer)
        {
        String                          cacheName  = "add-comparator-index-cache";
        NamedCache<?, ?>                cache      = ensureCache(cacheName);
        Map<ValueExtractor, MapIndex>   indexMap   = removeIndexes(cache);
        ValueExtractor<String, Integer> extractor  = new UniversalExtractor<>("length()");
        Comparator<Integer>             comparator = SafeComparator.INSTANCE();

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        grpcClient.addIndex(extractor, true, comparator);

        assertThat(indexMap, hasKey(extractor));
        assertThat(indexMap.get(extractor).isOrdered(), is(true));
        assertThat(indexMap.get(extractor).getComparator(), is(comparator));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldRemoveIndexWhenIndexExists(String sSerializerName, Serializer serializer)
        {
        String           cacheName = "remove-index-cache";
        NamedCache<?, ?> cache     = ensureCache(cacheName);
        cache.clear();

        Map<ValueExtractor, MapIndex> indexMap = removeIndexes(cache);

        ValueExtractor<String, Integer> extractor = new UniversalExtractor<>("length()");

        // Add the index using the normal cache
        cache.addIndex(extractor, false, null);

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        grpcClient.removeIndex(extractor);

        assertThat(indexMap.isEmpty(), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    void shouldRemoveIndexWhenIndexDoesNotExist(String sSerializerName, Serializer serializer)
        {
        String     cacheName = "remove-index-cache";
        NamedCache cache     = ensureCache(cacheName);
        cache.clear();

        Map<ValueExtractor, MapIndex> indexMap = removeIndexes(cache);

        ValueExtractor<String, Integer> extractor = new UniversalExtractor<>("length()");

        NamedCache<String, String> grpcClient = createClient(cacheName, sSerializerName, serializer);
        grpcClient.removeIndex(extractor);

        assertThat(indexMap.isEmpty(), is(true));
        }

    // ----- helper methods -------------------------------------------------

    protected <K, V> NamedCache<K, V> createClient(String sCacheName, String sSerializerName, Serializer serializer)
        {
        return f_serverHelper.createClient(f_sScopeName, sCacheName, sSerializerName, serializer);
        }

    protected <K, V> NamedCache<K, V> ensureCache(String sName, ClassLoader loader)
        {
        return f_serverHelper.getSession().getCache(sName);
        }

    /**
     * Remove all the indexes from the cache and return its index map.
     *
     * @param cache  the cache to remove indexes from
     *
     * @return the cache's index map
     */
    @SuppressWarnings("unchecked")
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

    protected final ServerHelper f_serverHelper;
    }
