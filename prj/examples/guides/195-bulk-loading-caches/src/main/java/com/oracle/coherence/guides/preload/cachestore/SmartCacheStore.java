/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload.cachestore;

import com.tangosol.net.cache.BinaryEntryStore;
import com.tangosol.net.cache.CacheStore;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.stream.Collectors;

/**
 * A {@link SmartCacheStore} delegates to a {@link BinaryEntryStore} or
 * {@link CacheStore} where store operations are controlled by the presence
 * or absence of a decoration on the entry's {@link com.tangosol.util.Binary} value.
 * <p>
 * If a decoration with a specified id is present on the {@link BinaryEntry#getBinaryValue() binary value}
 * passed to the {@link #store(BinaryEntry)} or {@link #storeAll(Set)} methods, these entries will not
 * be passed to the {@link CacheStore#store(Object, Object)} or {@link CacheStore#storeAll(Map)} methods.
 * <p>
 * The {@link CacheStore#load(Object)}, {@link CacheStore#loadAll(Collection)}, {@link CacheStore#erase(Object)}
 * and {@link CacheStore#eraseAll(Collection)} methods will always be called and are not controlled by decorations.
 *
 * @param <K>  the type of the cache keys
 * @param <V>  the type of the cache values
 */
public class SmartCacheStore<K, V>
        implements BinaryEntryStore<K, V>
    {
    /**
     * Create a {@link SmartCacheStore} that delegates
     * to a {@link CacheStore}.
     *
     * @param cacheStore  the {@link CacheStore} to delegate to
     *
     * @throws NullPointerException if the {@code cacheStore} parameter is {@code null}
     */
    public SmartCacheStore(CacheStore<K, V> cacheStore)
        {
        this(new WrapperBinaryEntryStore<>(Objects.requireNonNull(cacheStore)));
        }

    /**
     * Create a {@link SmartCacheStore} that delegates
     * to a {@link CacheStore}.
     *
     * @param cacheStore    the {@link CacheStore} to delegate to
     * @param decorationId  the id of the binary decoration to use to control the store operations
     *
     * @throws NullPointerException if the {@code cacheStore} parameter is {@code null}
     */
    public SmartCacheStore(CacheStore<K, V> cacheStore, int decorationId)
        {
        this(new WrapperBinaryEntryStore<>(Objects.requireNonNull(cacheStore)), decorationId);
        }

    /**
     * Create a {@link SmartCacheStore} that delegates
     * to a {@link BinaryEntryStore}.
     *
     * @param delegate  the {@link BinaryEntryStore} to delegate to
     *
     * @throws NullPointerException if the {@code cacheStore} parameter is {@code null}
     */
    public SmartCacheStore(BinaryEntryStore<K, V> delegate)
        {
        this(Objects.requireNonNull(delegate), DEFAULT_DECORATION_ID);
        }

    /**
     * Create a {@link SmartCacheStore} that delegates
     * to a {@link BinaryEntryStore}.
     *
     * @param delegate    the {@link BinaryEntryStore} to delegate to
     * @param decorationId  the id of the binary decoration to use to control the store operations
     *
     * @throws NullPointerException if the {@code cacheStore} parameter is {@code null}
     */
    public SmartCacheStore(BinaryEntryStore<K, V> delegate, int decorationId)
        {
        this.delegate = Objects.requireNonNull(delegate);
        this.decorationId = decorationId;
        }

    @Override
    public void load(BinaryEntry<K, V> entry)
        {
        delegate.load(entry);
        }

    @Override
    public void loadAll(Set<? extends BinaryEntry<K, V>> entries)
        {
        delegate.loadAll(entries);
        }

    @Override
    public void store(BinaryEntry<K, V> entry)
        {
        if (shouldStore(entry))
            {
            delegate.store(entry);
            }
        }

    @Override
    public void storeAll(Set<? extends BinaryEntry<K, V>> entries)
        {
        Set<? extends BinaryEntry<K, V>> entriesToStore = entries.stream()
                .filter(this::shouldStore)
                .collect(Collectors.toSet());

        if (entriesToStore.size() > 0)
            {
            delegate.storeAll(entriesToStore);
            }
        }

    @Override
    public void erase(BinaryEntry<K, V> entry)
        {
        delegate.erase(entry);
        }

    @Override
    public void eraseAll(Set<? extends BinaryEntry<K, V>> entries)
        {
        delegate.eraseAll(entries);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Returns {@code true} if the {@link com.tangosol.util.Binary} returned by calling
     * {@link BinaryEntry#getBinaryValue()} does not have a decoration with the
     * {@link #decorationId} identifier.
     *
     * @param entry  the {@link BinaryEntry} to check
     *
     * @return {@code true} if the {@link BinaryEntry} should be passed to the store operations
     */
    private boolean shouldStore(BinaryEntry<K, V> entry)
        {
        return !ExternalizableHelper.isDecorated(entry.getBinaryValue(), decorationId);
        }

    // ----- inner class: WrapperBinaryEntryStore ---------------------------

    /**
     * A wrapper that implements {@link BinaryEntryStore} and wraps a
     * normal {@link CacheStore} implementation.
     *
     * @param <K>  the type of the cache keys
     * @param <V>  the type of the cache values
     */
    protected static class WrapperBinaryEntryStore<K, V>
            implements BinaryEntryStore<K, V>
        {
        /**
         * Create a {@link WrapperBinaryEntryStore}.
         *
         * @param cacheStore  the wrapped {@link CacheStore}
         *
         * @throws NullPointerException if the {@code cacheStore} parameter is {@code null}
         */
        public WrapperBinaryEntryStore(CacheStore<K, V> cacheStore)
            {
            this.cacheStore = Objects.requireNonNull(cacheStore);
            }

        @Override
        public void load(BinaryEntry<K, V> entry)
            {
            V value = cacheStore.load(entry.getKey());
            if (value != null)
                {
                entry.setValue(value);
                }
            }

        @Override
        public void loadAll(Set<? extends BinaryEntry<K, V>> entries)
            {
            Map<K, ? extends BinaryEntry<K, V>> entryMap = entries.stream()
                    .collect(Collectors.toMap(InvocableMap.Entry::getKey, e -> e));

            Map<K, V> loadedMap = cacheStore.loadAll(entryMap.keySet());
            for (Map.Entry<K, V> entry : loadedMap.entrySet())
                {
                V value = entry.getValue();
                if (value != null)
                    {
                    BinaryEntry<K, V> binaryEntry = entryMap.get(entry.getKey());
                    if (binaryEntry != null)
                        {
                        binaryEntry.setValue(value);
                        }
                    }
                }
            }

        @Override
        public void store(BinaryEntry<K, V> entry)
            {
            cacheStore.store(entry.getKey(), entry.getValue());
            }

        @Override
        public void storeAll(Set<? extends BinaryEntry<K, V>> entries)
            {
            Map<K, V> map = entries.stream()
                    .collect(Collectors.toMap(InvocableMap.Entry::getKey, InvocableMap.Entry::getValue));

            cacheStore.storeAll(map);
            }

        @Override
        public void erase(BinaryEntry<K, V> entry)
            {
            cacheStore.erase(entry.getKey());
            }

        @Override
        public void eraseAll(Set<? extends BinaryEntry<K, V>> entries)
            {
            Set<K> keys = entries.stream()
                    .map(InvocableMap.Entry::getKey)
                    .collect(Collectors.toSet());

            cacheStore.eraseAll(keys);
            }

        // ----- data members -----------------------------------------------

        /**
         * The wrapped {@link CacheStore}.
         */
        private final CacheStore<K, V> cacheStore;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The default decoration identifier.
     */
    public static final int DEFAULT_DECORATION_ID = ExternalizableHelper.DECO_APP_1;

    // ----- data members ---------------------------------------------------

    /**
     * The {@link BinaryEntryStore} this store delegates to.
     */
    private final BinaryEntryStore<K, V> delegate;

    /**
     * The binary decoration identifier used to indicate an entry should not be stored.
     */
    private final int decorationId;
    }
