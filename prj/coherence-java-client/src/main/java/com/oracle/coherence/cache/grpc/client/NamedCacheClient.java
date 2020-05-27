/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cache.grpc.client;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestIncompleteException;

import com.tangosol.net.cache.CacheMap;

import com.tangosol.util.Filter;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ExecutionException;

/**
 * Implementation of a {@link NamedCache}
 * that wraps an {@link AsyncNamedCacheClient}.
 *
 * @param <K> the type of the cache keys
 * @param <V> the type of the cache values
 *
 * @author Jonathan Knight  2019.11.07
 * @since 14.1.2
 */
class NamedCacheClient<K, V>
        implements NamedCache<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a {@link NamedCacheClient} with the specified cache name
     * and {@link NamedCacheService}.
     *
     * @param asyncClient  the asynchronous client
     */
    protected NamedCacheClient(AsyncNamedCacheClient<K, V> asyncClient)
        {
        this.f_asyncClient = asyncClient;
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Return an asynchronous version of the client.
     *
     * @return an asynchronous version of the client
     */
    protected AsyncNamedCacheClient<K, V> getAsyncClient()
        {
        return f_asyncClient;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "NamedCacheClient{"
               + "name: \"" + f_asyncClient.getCacheName() + '"'
               + " format: \"" + f_asyncClient.getFormat() + '"'
               + '}';
        }

    // ----- NamedCache interface -------------------------------------------

    @Override
    public <T, E> void addIndex(ValueExtractor<? super T, ? extends E> valueExtractor,
                                boolean sorted, Comparator<? super E> comparator)
        {
        f_asyncClient.assertActive();
        try
            {
            f_asyncClient.addIndex(valueExtractor, sorted, comparator).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public void addMapListener(MapListener<? super K, ? super V> mapListener)
        {
        f_asyncClient.assertActive();
        try
            {
            f_asyncClient.addMapListener(mapListener).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public void addMapListener(MapListener<? super K, ? super V> mapListener, K key, boolean lite)
        {
        f_asyncClient.assertActive();
        try
            {
            f_asyncClient.addMapListener(mapListener, key, lite).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public void addMapListener(MapListener<? super K, ? super V> mapListener, Filter filter, boolean lite)
        {
        f_asyncClient.assertActive();
        try
            {
            f_asyncClient.addMapListener(mapListener, filter, lite).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public <R> R aggregate(Collection<? extends K> keys, EntryAggregator<? super K, ? super V, R> aggregator)
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.aggregate(keys, aggregator).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public <R> R aggregate(Filter filter, EntryAggregator<? super K, ? super V, R> aggregator)
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.aggregate(filter, aggregator).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public void clear()
        {
        f_asyncClient.assertActive();
        try
            {
            f_asyncClient.clear().get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public boolean containsKey(Object key)
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.containsKeyInternal(key).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public boolean containsValue(Object value)
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.containsValue(value).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public void destroy()
        {
        f_asyncClient.assertActive();
        try
            {
            f_asyncClient.destroy().get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public Set<Map.Entry<K, V>> entrySet(Filter filter)
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.entrySet(filter).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public Set<Map.Entry<K, V>> entrySet(Filter filter, Comparator comparator)
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.entrySet(filter, comparator).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public Set<Map.Entry<K, V>> entrySet()
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.entrySet().get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public V get(Object key)
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.getInternal(key, null).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
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
        try
            {
            return f_asyncClient.invoke(k, entryProcessor).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public <R> Map<K, R> invokeAll(Collection<? extends K> keys, EntryProcessor<K, V, R> entryProcessor)
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.invokeAll(keys, entryProcessor).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public <R> Map<K, R> invokeAll(Filter filter, EntryProcessor<K, V, R> entryProcessor)
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.invokeAll(filter, entryProcessor).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public boolean isActive()
        {
        try
            {
            return f_asyncClient.isActive().get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public boolean isEmpty()
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.isEmpty().get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public Set<K> keySet(Filter filter)
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.keySet(filter).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public Set<K> keySet()
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.keySet().get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
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
        try
            {
            return f_asyncClient.putInternal(key, value, ttl).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public void putAll(Map<? extends K, ? extends V> m)
        {
        f_asyncClient.assertActive();
        try
            {
            f_asyncClient.putAll(m).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public void release()
        {
        f_asyncClient.assertActive();
        try
            {
            f_asyncClient.release().get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public V remove(Object key)
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.removeInternal(key).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public <T, E> void removeIndex(ValueExtractor<? super T, ? extends E> valueExtractor)
        {
        f_asyncClient.assertActive();
        try
            {
            f_asyncClient.removeIndex(valueExtractor).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V> mapListener)
        {
        f_asyncClient.assertActive();
        try
            {
            f_asyncClient.removeMapListener(mapListener).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V> mapListener, K key)
        {
        f_asyncClient.assertActive();
        try
            {
            f_asyncClient.removeMapListener(mapListener, key).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V> mapListener, Filter filter)
        {
        f_asyncClient.assertActive();
        try
            {
            f_asyncClient.removeMapListener(mapListener, filter).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public int size()
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.size().get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
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
        try
            {
            return f_asyncClient.values().get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    // ----- NamedCache methods ---------------------------------------------

    @Override
    public <R> R aggregate(EntryAggregator<? super K, ? super V, R> aggregator)
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.aggregate(aggregator).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
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
        try
            {
            return f_asyncClient.getInternal(key, defaultValue).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
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
        try
            {
            return f_asyncClient.putIfAbsent(key, value).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public boolean remove(Object key, Object value)
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.removeInternal(key, value).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public boolean replace(K key, V oldValue, V newValue)
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.replace(key, oldValue, newValue).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public void truncate()
        {
        f_asyncClient.assertActive();
        try
            {
            f_asyncClient.truncate().get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public Collection<V> values(Filter filter)
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.values(filter).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    @Override
    public Collection<V> values(Filter filter, Comparator comparator)
        {
        f_asyncClient.assertActive();
        try
            {
            return f_asyncClient.valuesInternal(filter, comparator).get();
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw new RequestIncompleteException(e);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The asynchronous client.
     */
    protected final AsyncNamedCacheClient<K, V> f_asyncClient;
    }
