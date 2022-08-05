/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload.cachestore;

import com.tangosol.net.cache.CacheStore;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link ControllableCacheStore} delegates to another {@link CacheStore} and is
 * controlled by a {@link Controller}.
 * <p>
 * When the {@link Controller#isEnabled()} method returns {@code true} the {@link CacheStore}
 * operations are delegated to the wrapped {@link CacheStore}. If the {@link Controller#isEnabled()}
 * method returns {@code false} the {@link CacheStore} are a no-op.
 *
 * @param <K>  the type of the cache keys
 * @param <V>  the type of the cache values
 */
public class ControllableCacheStore<K, V>
        implements CacheStore<K, V>
    {
    /**
     * Create a {@link ControllableCacheStore}.
     *
     * @param controller  the {@link Controller} that enables or disabled cache store operations
     * @param cacheStore  the {@link CacheStore} to delegate operations to
     */
    public ControllableCacheStore(Controller controller, CacheStore<K, V> cacheStore)
        {
        this.controller = controller;
        this.cacheStore = cacheStore;
        }

    @Override
    public V load(K key)
        {
        if (controller.isEnabled())
            {
            return cacheStore.load(key);
            }
        return null;
        }

    @Override
    public Map<K, V> loadAll(Collection<? extends K> colKeys)
        {
        if (controller.isEnabled())
            {
            return cacheStore.loadAll(colKeys);
            }
        return new HashMap<>();
        }

    @Override
    public void store(K key, V value)
        {
        if (controller.isEnabled())
            {
            cacheStore.store(key, value);
            }
        }

    @Override
    public void storeAll(Map<? extends K, ? extends V> mapEntries)
        {
        if (controller.isEnabled())
            {
            cacheStore.storeAll(mapEntries);
            }
        }

    @Override
    public void erase(K key)
        {
        if (controller.isEnabled())
            {
            cacheStore.erase(key);
            }
        }

    @Override
    public void eraseAll(Collection<? extends K> colKeys)
        {
        if (controller.isEnabled())
            {
            cacheStore.eraseAll(colKeys);
            }
        }

    /**
     * Returns {@code true} if this cache store is enabled.
     *
     * @return {@code true} if this cache store is enabled
     */
    public boolean isEnabled()
        {
        return controller.isEnabled();
        }

    /**
     * Return the {@link Controller} used by this {@link ControllableCacheStore}.
     *
     * @return the {@link Controller} used by this {@link ControllableCacheStore}
     */
    public Controller getController()
        {
        return controller;
        }

    /**
     * Implementations of {@link Controller} can control a {@link ControllableCacheStore}.
     */
    public interface Controller
        {
        /**
         * Returns {@code true} if the {@link ControllableCacheStore} should delegate operations
         * to the wrapped {@link CacheStore} or false if the {@link CacheStore} operations are
         * a no-op.
         *
         * @return {@code true} if the {@link ControllableCacheStore} should delegate operations
         *         to the wrapped {@link CacheStore} or false if the {@link CacheStore} operations are
         *         a no-op
         */
        boolean isEnabled();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@code Controller} that indicates whether this {@link CacheStore}  is enabled.
     */
    private final Controller controller;

    /**
     * The {@link CacheStore} this {@link ControllableCacheStore} wraps.
     */
    private final CacheStore<K, V> cacheStore;
    }
