/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestIncompleteException;

import com.tangosol.net.cache.CacheMap;

import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.util.Filter;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of a {@link NamedCache}
 * that wraps an {@link AsyncNamedCacheClient}.
 *
 * @param <K> the type of the cache keys
 * @param <V> the type of the cache values
 *
 * @author Jonathan Knight  2019.11.07
 * @since 20.06
 */
@SuppressWarnings("rawtypes")
public class NamedCacheClient<K, V>
        implements NamedCache<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a {@link NamedCacheClient} wrapping the specified {@link AsyncNamedCacheClient}.
     *
     * @param asyncClient  the asynchronous client
     */
    protected NamedCacheClient(AsyncNamedCacheClient<K, V> asyncClient)
        {
        f_asyncClient = asyncClient;

        AsyncNamedCacheClient.Dependencies dependencies = asyncClient.getDependencies();
        f_deadline    = dependencies == null
                ? GrpcDependencies.DEFAULT_DEADLINE_MILLIS
                : Math.max(asyncClient.getDependencies().getDeadline(), 0L);
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Return an asynchronous version of the client.
     *
     * @return an asynchronous version of the client
     */
    public AsyncNamedCacheClient<K, V> getAsyncClient()
        {
        return f_asyncClient;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "NamedCacheClient{"
               + "scope: \"" + f_asyncClient.getScopeName() + '"'
               + ", name: \"" + f_asyncClient.getCacheName() + '"'
               + ", format: \"" + f_asyncClient.getFormat() + '"'
               + '}';
        }

    // ----- NamedCache interface -------------------------------------------

    @Override
    public <T, E> void addIndex(ValueExtractor<? super T, ? extends E> valueExtractor,
                                boolean sorted, Comparator<? super E> comparator)
        {
        f_asyncClient.assertActive();
        handleCompletableFuture(f_asyncClient.addIndex(valueExtractor, sorted, comparator));
        }

    @Override
    public void addMapListener(MapListener<? super K, ? super V> mapListener)
        {
        f_asyncClient.assertActive();
        handleCompletableFuture(f_asyncClient.addMapListener(mapListener));
        }

    @Override
    public void addMapListener(MapListener<? super K, ? super V> mapListener, K key, boolean lite)
        {
        f_asyncClient.assertActive();
        handleCompletableFuture(f_asyncClient.addMapListener(mapListener, key, lite));
        }

    @Override
    public void addMapListener(MapListener<? super K, ? super V> mapListener, Filter filter, boolean lite)
        {
        f_asyncClient.assertActive();
        CompletableFuture<Void> future = f_asyncClient.addMapListener(mapListener, filter, lite);
        handleCompletableFuture(future);
        }

    @Override
    public <R> R aggregate(Collection<? extends K> keys, EntryAggregator<? super K, ? super V, R> aggregator)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.aggregate(keys, aggregator));
        }

    @Override
    public <R> R aggregate(Filter filter, EntryAggregator<? super K, ? super V, R> aggregator)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.aggregate(filter, aggregator));
        }

    @Override
    public void clear()
        {
        f_asyncClient.assertActive();
        handleCompletableFuture(f_asyncClient.clear());
        }

    @Override
    public boolean containsKey(Object key)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.containsKeyInternal(key));
        }

    @Override
    public boolean containsValue(Object value)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.containsValue(value));
        }

    @Override
    public void destroy()
        {
        f_asyncClient.assertActive();
        handleCompletableFuture(f_asyncClient.destroy());
        }

    @Override
    public Set<Map.Entry<K, V>> entrySet(Filter filter)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.entrySet(filter));
        }

    @Override
    public Set<Map.Entry<K, V>> entrySet(Filter filter, Comparator comparator)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.entrySet(filter, comparator));
        }

    @Override
    public Set<Map.Entry<K, V>> entrySet()
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.entrySet());
        }

    @Override
    public V get(Object key)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.getInternal(key, null));
        }

    @Override
    public Map<K, V> getAll(Collection<? extends K> keys)
        {
        f_asyncClient.assertActive();
        return f_asyncClient.getAllInternalAsMap(keys);
        }

    @Override
    public String getCacheName()
        {
        return f_asyncClient.getCacheName();
        }

    @Override
    public CacheService getCacheService()
        {
        return f_asyncClient.getCacheService();
        }

    @Override
    public <R> R invoke(K k, EntryProcessor<K, V, R> entryProcessor)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.invoke(k, entryProcessor));
        }

    @Override
    public <R> Map<K, R> invokeAll(Collection<? extends K> keys, EntryProcessor<K, V, R> entryProcessor)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.invokeAll(keys, entryProcessor));
        }

    @Override
    public <R> Map<K, R> invokeAll(Filter filter, EntryProcessor<K, V, R> entryProcessor)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.invokeAll(filter, entryProcessor));
        }

    @Override
    public boolean isActive()
        {
        return f_asyncClient.isActiveInternal();
        }

    @Override
    public boolean isReady()
        {
        return handleCompletableFuture(f_asyncClient.isReady());
        }

    @Override
    public boolean isEmpty()
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.isEmpty());
        }

    @Override
    public Set<K> keySet(Filter filter)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.keySet(filter));
        }

    @Override
    public Set<K> keySet()
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.keySet());
        }

    @Override
    public boolean lock(Object o, long l)
        {
        throw new UnsupportedOperationException("not supported");
        }

    @Override
    public boolean lock(Object o)
        {
        throw new UnsupportedOperationException("not supported");
        }

    @Override
    public V put(K key, V value)
        {
        return put(key, value, CacheMap.EXPIRY_DEFAULT);
        }

    @Override
    public V put(K key, V value, long ttl)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.putInternal(key, value, ttl));
        }

    @Override
    @SuppressWarnings("NullableProblems")
    public void putAll(Map<? extends K, ? extends V> m)
        {
        f_asyncClient.assertActive();
        handleCompletableFuture(f_asyncClient.putAll(m));
        }

    @Override
    public void release()
        {
        f_asyncClient.assertActive();
        handleCompletableFuture(f_asyncClient.release());
        }

    @Override
    public V remove(Object key)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.removeInternal(key));
        }

    @Override
    public <T, E> void removeIndex(ValueExtractor<? super T, ? extends E> valueExtractor)
        {
        f_asyncClient.assertActive();
        handleCompletableFuture(f_asyncClient.removeIndex(valueExtractor));
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V> mapListener)
        {
        f_asyncClient.assertActive();
        handleCompletableFuture(f_asyncClient.removeMapListener(mapListener));
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V> mapListener, K key)
        {
        f_asyncClient.assertActive();
        handleCompletableFuture(f_asyncClient.removeMapListener(mapListener, key));
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V> mapListener, Filter filter)
        {
        f_asyncClient.assertActive();
        handleCompletableFuture(f_asyncClient.removeMapListener(mapListener, filter));
        }

    @Override
    public int size()
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.size());
        }

    @Override
    public boolean unlock(Object o)
        {
        throw new UnsupportedOperationException("Not supported");
        }

    @Override
    public Collection<V> values()
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.values());
        }

    // ----- NamedCache methods ---------------------------------------------

    @Override
    public <R> R aggregate(EntryAggregator<? super K, ? super V, R> aggregator)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.aggregate(aggregator));
        }

    @Override
    public AsyncNamedCache<K, V> async()
        {
        return f_asyncClient;
        }

    @Override
    public AsyncNamedCache<K, V> async(AsyncNamedCache.Option... options)
        {
        return f_asyncClient;
        }

    @Override
    public V getOrDefault(Object key, V defaultValue)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.getInternal(key, defaultValue));
        }

    @Override
    public boolean isDestroyed()
        {
        return f_asyncClient.isDestroyed();
        }

    @Override
    public boolean isReleased()
        {
        return f_asyncClient.isReleased();
        }

    @Override
    public V putIfAbsent(K key, V value)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.putIfAbsent(key, value));
        }

    @Override
    public boolean remove(Object key, Object value)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.removeInternal(key, value));
        }

    @Override
    public boolean replace(K key, V oldValue, V newValue)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.replace(key, oldValue, newValue));
        }

    @Override
    public void truncate()
        {
        f_asyncClient.assertActive();
        handleCompletableFuture(f_asyncClient.truncate());
        }

    @Override
    public Collection<V> values(Filter filter)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.values(filter));
        }

    @Override
    public Collection<V> values(Filter filter, Comparator comparator)
        {
        f_asyncClient.assertActive();
        return handleCompletableFuture(f_asyncClient.valuesInternal(filter, comparator));
        }

    // ----- helper methods -------------------------------------------------

    protected <T> T handleCompletableFuture(CompletableFuture<T> future)
        {
        return handleCompletableFuture(future, f_deadline);
        }

    protected <T> T handleCompletableFuture(CompletableFuture<T> future, long cTimeoutMillis)
        {
        try
            {
            if (cTimeoutMillis > 0)
                {
                return future.get(cTimeoutMillis, TimeUnit.MILLISECONDS);
                }
            return future.get();
            }
        catch (ExecutionException e)
            {
            Throwable cause = e.getCause();
            if (cause instanceof UnsupportedOperationException)
                {
                throw (UnsupportedOperationException) cause;
                }
            if (cause instanceof RequestIncompleteException)
                {
                throw (RequestIncompleteException) cause;
                }
            if (cause == null)
                {
                cause = e;
                }
            throw new RequestIncompleteException(cause);
            }
        catch (TimeoutException | InterruptedException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The asynchronous client.
     */
    protected final AsyncNamedCacheClient<K, V> f_asyncClient;

    /**
     * The request timeout.
     */
    protected final long f_deadline;
    }
