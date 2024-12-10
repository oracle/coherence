/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.Listeners;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.function.Remote;

import com.tangosol.util.stream.RemoteStream;

import java.util.Collection;
import java.util.Comparator;
import java.util.EventListener;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A {@link ConfigurableCacheFactorySession}-based implementation of
 * {@link NamedCache}, that delegates requests onto an internal
 * {@link NamedCache} and isolates developer provided instances like
 * {@link MapListener}s, so that when a {@link Session} is closed, the
 * resources are released.
 *
 * @see Session
 *
 * @author bo 2015.10.14
 */
public class SessionNamedCache<K, V>
        implements NamedCache<K, V>, AutoCloseable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link SessionNamedCache}.
     *
     * @param session       the {@link ConfigurableCacheFactorySession} that produced this {@link SessionNamedCache}
     * @param cache         the {@link NamedCache} to which requests will be delegated
     * @param typeAssertion the {@link TypeAssertion} for the {@link NamedCache}
     */
    public SessionNamedCache(ConfigurableCacheFactorySession session,
                             NamedCache<K, V> cache,
                             TypeAssertion<K, V> typeAssertion)
        {
        this(session, cache, null, typeAssertion);
        }

    /**
     * Constructs a {@link SessionNamedCache}.
     *
     * @param session       the {@link ConfigurableCacheFactorySession} that produced this {@link SessionNamedCache}
     * @param cache         the {@link NamedCache} to which requests will be delegated
     * @param loader        the {@Link ClassLoader} associated with the cache
     * @param typeAssertion the {@link TypeAssertion} for the {@link NamedCache}
     */
    public SessionNamedCache(ConfigurableCacheFactorySession session,
                             NamedCache<K, V> cache,
                             ClassLoader loader,
                             TypeAssertion<K, V> typeAssertion)
        {
        m_session       = session;
        m_cache         = cache;
        f_loader        = loader;
        m_typeAssertion = typeAssertion;
        m_fActive       = cache.isActive();
        m_listeners     = new MapListenerSupport();
        }

    // ----- NamedCache interface -------------------------------------------

    @Override
    public String getCacheName()
        {
        return m_cache.getCacheName();
        }

    @Override
    public CacheService getCacheService()
        {
        return m_cache.getCacheService();
        }

    @Override
    public boolean isActive()
        {
        return m_fActive && m_cache.isActive();
        }

    @Override
    public boolean isReady()
        {
        return m_cache.isReady();
        }

    @Override
    public boolean isDestroyed()
        {
        return m_cache.isDestroyed();
        }

    @Override
    public boolean isReleased()
        {
        return m_cache.isReleased();
        }

    @Override
    public void release()
        {
        // release is the same as a close
        close();
        }

    @Override
    public void destroy()
        {
        if (isActive())
            {
            // destroying a NamedCache is always delegated to the Session to manage
            m_session.onDestroy(this);
            }
        }

    @Override
    public V put(K key, V value, long cMillis)
        {
        return m_cache.put(key, value, cMillis);
        }

    @Override
    public void clear()
        {
        m_cache.clear();
        }

    @Override
    public void truncate()
        {
        m_cache.truncate();
        }

    @Override
    public AsyncNamedCache<K, V> async(AsyncNamedCache.Option... options)
        {
        return m_cache.async(options);
        }

    @Override
    public void addMapListener(MapListener<? super K, ? super V> listener)
        {
        m_listeners.addListener(listener, (Filter) null, true);

        m_cache.addMapListener(listener);
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V> listener)
        {
        m_listeners.removeListener(listener, (Filter) null);

        m_cache.removeMapListener(listener);
        }

    @Override
    public void addMapListener(MapListener<? super K, ? super V> listener, K key, boolean fLite)
        {
        m_listeners.addListener(listener, key, fLite);

        m_cache.addMapListener(listener, key, fLite);
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V> listener, K key)
        {
        m_listeners.removeListener(listener, key);

        m_cache.removeMapListener(listener, key);
        }

    @Override
    public void addMapListener(MapListener<? super K, ? super V> listener, Filter filter, boolean fLite)
        {
        m_listeners.addListener(listener, filter, fLite);

        m_cache.addMapListener(listener, filter, fLite);
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V> listener, Filter filter)
        {
        m_listeners.removeListener(listener, filter);

        m_cache.removeMapListener(listener, filter);
        }

    @Override
    public int size()
        {
        return m_cache.size();
        }

    @Override
    public boolean isEmpty()
        {
        return m_cache.isEmpty();
        }

    @Override
    public boolean containsKey(Object key)
        {
        return m_cache.containsKey(key);
        }

    @Override
    public boolean containsValue(Object value)
        {
        return m_cache.containsValue(value);
        }

    @Override
    public V get(Object key)
        {
        return m_cache.get(key);
        }

    @Override
    public V put(K key, V value)
        {
        return m_cache.put(key, value);
        }

    @Override
    public V remove(Object key)
        {
        return m_cache.remove(key);
        }

    @Override
    public void putAll(Map<? extends K, ? extends V> m)
        {
        m_cache.putAll(m);
        }

    @Override
    public Set<K> keySet()
        {
        return m_cache.keySet();
        }

    @Override
    public Collection<V> values()
        {
        return m_cache.values();
        }

    @Override
    public Set<Map.Entry<K, V>> entrySet()
        {
        return m_cache.entrySet();
        }

    @Override
    public V getOrDefault(Object key, V defaultValue)
        {
        return m_cache.getOrDefault(key, defaultValue);
        }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action)
        {
        m_cache.forEach(action);
        }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function)
        {
        m_cache.replaceAll(function);
        }

    @Override
    public V putIfAbsent(K key, V value)
        {
        return m_cache.putIfAbsent(key, value);
        }

    @Override
    public boolean remove(Object key, Object value)
        {
        return m_cache.remove(key, value);
        }

    @Override
    public boolean replace(K key, V oldValue, V newValue)
        {
        return m_cache.replace(key, oldValue, newValue);
        }

    @Override
    public V replace(K key, V value)
        {
        return m_cache.replace(key, value);
        }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
        {
        return m_cache.computeIfAbsent(key, mappingFunction);
        }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        return m_cache.computeIfPresent(key, remappingFunction);
        }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        return m_cache.compute(key, remappingFunction);
        }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)
        {
        return m_cache.merge(key, value, remappingFunction);
        }

    @Override
    public Map<K, V> getAll(Collection<? extends K> colKeys)
        {
        return m_cache.getAll(colKeys);
        }

    @Override
    public void forEach(Collection<? extends K> collKeys, BiConsumer<? super K, ? super V> action)
        {
        m_cache.forEach(collKeys, action);
        }

    @Override
    public boolean lock(Object oKey, long cWait)
        {
        return m_cache.lock(oKey, cWait);
        }

    @Override
    public boolean lock(Object oKey)
        {
        return m_cache.lock(oKey);
        }

    @Override
    public boolean unlock(Object oKey)
        {
        return m_cache.unlock(oKey);
        }

    @Override
    public Set<K> keySet(Filter filter)
        {
        return m_cache.keySet(filter);
        }

    @Override
    public Set<Map.Entry<K, V>> entrySet(Filter filter)
        {
        return m_cache.entrySet(filter);
        }

    @Override
    public Set<Map.Entry<K, V>> entrySet(Filter filter, Comparator comparator)
        {
        return m_cache.entrySet(filter, comparator);
        }

    @Override
    public Collection<V> values(Filter filter)
        {
        return m_cache.values(filter);
        }

    @Override
    public Collection<V> values(Filter filter, Comparator comparator)
        {
        return m_cache.values(filter, comparator);
        }

    @Override
    public <T, E> void addIndex(ValueExtractor<? super T, ? extends E> extractor, boolean fOrdered, Comparator<? super E> comparator)
        {
        m_cache.addIndex(extractor, fOrdered, comparator);
        }

    @Override
    public <T, E> void removeIndex(ValueExtractor<? super T, ? extends E> extractor)
        {
        m_cache.removeIndex(extractor);
        }

    @Override
    public void forEach(Filter filter, BiConsumer<? super K, ? super V> action)
        {
        m_cache.forEach(filter, action);
        }

    @Override
    public <R> R invoke(K key, EntryProcessor<K, V, R> processor)
        {
        return m_cache.invoke(key, processor);
        }

    @Override
    public <R> Map<K, R> invokeAll(EntryProcessor<K, V, R> processor)
        {
        return m_cache.invokeAll(processor);
        }

    @Override
    public <R> Map<K, R> invokeAll(Collection<? extends K> collKeys, EntryProcessor<K, V, R> processor)
        {
        return m_cache.invokeAll(collKeys, processor);
        }

    @Override
    public <R> Map<K, R> invokeAll(Filter filter, EntryProcessor<K, V, R> processor)
        {
        return m_cache.invokeAll(filter, processor);
        }

    @Override
    public <R> R aggregate(EntryAggregator<? super K, ? super V, R> aggregator)
        {
        return m_cache.aggregate(aggregator);
        }

    @Override
    public <R> R aggregate(Collection<? extends K> collKeys, EntryAggregator<? super K, ? super V, R> aggregator)
        {
        return m_cache.aggregate(collKeys, aggregator);
        }

    @Override
    public <R> R aggregate(Filter filter, EntryAggregator<? super K, ? super V, R> aggregator)
        {
        return m_cache.aggregate(filter, aggregator);
        }

    @Override
    public V computeIfAbsent(K key, Remote.Function<? super K, ? extends V> mappingFunction)
        {
        return m_cache.computeIfAbsent(key, mappingFunction);
        }

    @Override
    public V computeIfPresent(K key, Remote.BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        return m_cache.computeIfPresent(key, remappingFunction);
        }

    @Override
    public V compute(K key, Remote.BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        return m_cache.compute(key, remappingFunction);
        }

    @Override
    public V merge(K key, V value, Remote.BiFunction<? super V, ? super V, ? extends V> remappingFunction)
        {
        return m_cache.merge(key, value, remappingFunction);
        }

    @Override
    public void replaceAll(Remote.BiFunction<? super K, ? super V, ? extends V> function)
        {
        m_cache.replaceAll(function);
        }

    @Override
    public void replaceAll(Collection<? extends K> collKeys, Remote.BiFunction<? super K, ? super V, ? extends V> function)
        {
        m_cache.replaceAll(collKeys, function);
        }

    @Override
    public void replaceAll(Filter filter, Remote.BiFunction<? super K, ? super V, ? extends V> function)
        {
        m_cache.replaceAll(filter, function);
        }

    @Override
    public RemoteStream<InvocableMap.Entry<K, V>> stream()
        {
        return m_cache.stream();
        }

    @Override
    public RemoteStream<InvocableMap.Entry<K, V>> stream(Collection<? extends K> collKeys)
        {
        return m_cache.stream(collKeys);
        }

    @Override
    public RemoteStream<InvocableMap.Entry<K, V>> stream(Filter filter)
        {
        return m_cache.stream(filter);
        }

    @Override
    public <T, E> RemoteStream<E> stream(ValueExtractor<T, ? extends E> extractor)
        {
        return m_cache.stream(extractor);
        }

    @Override
    public <T, E> RemoteStream<E> stream(Collection<? extends K> collKeys, ValueExtractor<T, ? extends E> extractor)
        {
        return m_cache.stream(collKeys, extractor);
        }

    @Override
    public <T, E> RemoteStream<E> stream(Filter filter, ValueExtractor<T, ? extends E> extractor)
        {
        return m_cache.stream(filter, extractor);
        }

    @Override
    public <C extends NamedCache<K, V>> C as(Class<C> clzNamedCache)
        {
        Base.azzert(clzNamedCache != null, "The specified Class can't be null");

        return clzNamedCache.isInstance(this) ? (C) this : m_cache.as(
                clzNamedCache);
        }

    // ----- AutoCloseable interface ----------------------------------------

    @Override
    public synchronized void close()
        {
        if (isActive())
            {
            // closing a NamedCache is always delegated to the Session to manage
            m_session.onClose(this);
            }
        }

    // ----- SessionNamedCache methods --------------------------------------

    /**
     * Return the {@link NamedCache} this class wraps.
     *
     * @return the {@link NamedCache} this class wraps
     */
    public NamedCache<K, V> getInternalNamedCache()
        {
        return m_cache;
        }

    /**
     * Return the {@link TypeAssertion} associated with the wrapped cache.
     *
     * @return the {@link TypeAssertion} associated with the wrapped cache
     */
    TypeAssertion<K, V> getTypeAssertion()
        {
        return m_typeAssertion;
        }

    /**
     * Return the {@link ClassLoader} used by the wrapped cache.
     *
     * @return the {@link ClassLoader} used by the wrapped cache
     */
    ClassLoader getContextClassLoader()
        {
        return f_loader;
        }

    /**
     * Perform actions prior to this cache closing.
     */
    void onClosing()
        {
        m_fActive = false;

        dropListeners();
        }

    /**
     * Perform actions after this cache has closed.
     */
    void onClosed()
        {
        }

    /**
     * Perform actions prior to this cache being destroyed.
     */
    void onDestroying()
        {
        m_fActive = false;

        dropListeners();
        }

    /**
     * Perform actions after this cache has been destroyed.
     */
    void onDestroyed()
        {
        }

    /**
     * Drops all registered key and filter-based listeners.
     */
    void dropListeners()
        {
        // remove the key-based listeners from the underlying cache
        m_listeners.getKeySet().stream().forEach(key ->
                                                 {
                                                 Listeners listeners = m_listeners.getListeners(
                                                         key);

                                                 if (listeners != null)
                                                     {
                                                     for (EventListener listener : listeners.listeners())
                                                         {
                                                         m_cache.removeMapListener(
                                                                 (MapListener) listener,
                                                                 (K) key);
                                                         }
                                                     }
                                                 });

        // remove the filter-based listeners from the underlying cache
        m_listeners.getFilterSet().stream().forEach(filter ->
                                                    {
                                                    Listeners listeners = m_listeners.getListeners(
                                                            filter);

                                                    if (listeners != null)
                                                        {
                                                        for (EventListener listener : listeners.listeners())
                                                            {
                                                            m_cache.removeMapListener(
                                                                    (MapListener) listener,
                                                                    (Filter) filter);
                                                            }
                                                        }
                                                    });

        // we're no longer holding onto listeners
        m_listeners.clear();
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        SessionNamedCache<?, ?> that = (SessionNamedCache<?, ?>) o;
        return Objects.equals(m_session, that.m_session)
                && Objects.equals(m_cache, that.m_cache)
                && Objects.equals(f_loader, that.f_loader);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_session, m_cache, f_loader);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link ConfigurableCacheFactorySession} that produced this {@link SessionNamedCache}.
     */
    private ConfigurableCacheFactorySession m_session;

    /**
     * The underlying {@link NamedCache} on which requests will be delegated.
     */
    private NamedCache<K, V> m_cache;

    /**
     * The {@link ClassLoader} associated with this session's cache.
     */
    private final ClassLoader f_loader;

    /**
     * The {@link TypeAssertion} to be used for this {@link SessionNamedCache}.
     */
    private TypeAssertion<K, V> m_typeAssertion;

    /**
     * The {@link MapListenerSupport} for tracking {@link MapListener}s
     * registered against this {@link NamedCache} implementation.
     */
    private MapListenerSupport m_listeners;

    /**
     * A flag indicating if this instance of a {@link NamedCache} is active,
     * regardless of the "active" status of the underlying {@link NamedCache}.
     */
    private volatile boolean m_fActive;
    }
