/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.util.Options;
import com.tangosol.internal.util.processor.CacheProcessors;

import com.tangosol.net.cache.CacheMap;

import com.tangosol.util.AsynchronousAgent;
import com.tangosol.util.Filter;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.aggregator.Count;
import com.tangosol.util.comparator.EntryComparator;
import com.tangosol.util.comparator.SafeComparator;

import com.tangosol.util.filter.AlwaysFilter;

import com.tangosol.util.function.Remote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import java.util.concurrent.CompletableFuture;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Asynchronous {@link NamedCache}.
 *
 * @param <K>  the type of the cache entry keys
 * @param <V>  the type of the cache entry values
 *
 * @author as  2015.01.17
 * @since 12.2.1
 */
public interface AsyncNamedCache<K, V>
        extends AsyncNamedMap<K, V>
    {
    /**
     * Return the {@link NamedCache} instance this {@code AsyncNamedCache} is
     * based on.
     *
     * @return the {@link NamedCache} instance this {@code AsyncNamedCache} is
     *         based on
     */
    public NamedCache<K, V> getNamedCache();

    // ---- Asynchronous CacheMap methods -----------------------------------

    /**
     * Associates the specified value with the specified key in this cache. If
     * the cache previously contained a mapping for this key, the old value is
     * replaced. This variation of the {@link #put(Object oKey, Object oValue)}
     * method allows the caller to specify an expiry (or "time to live") for the
     * cache entry.
     *
     * @param key      key with which the specified value is to be associated
     * @param value    value to be associated with the specified key
     * @param cMillis  the number of milliseconds until the cache entry will
     *                 expire, also referred to as the entry's "time to live";
     *                 pass {@link CacheMap#EXPIRY_DEFAULT} to use the cache's
     *                 default time-to-live setting; pass {@link
     *                 CacheMap#EXPIRY_NEVER} to indicate that the cache entry
     *                 should never expire; this milliseconds value is <b>not</b>
     *                 a date/time value, such as is returned from
     *                 System.currentTimeMillis()
     *
     * @return a {@link CompletableFuture}
     */
    public default CompletableFuture<Void> put(K key, V value, long cMillis)
        {
        return invoke(key, CacheProcessors.put(value, cMillis));
        }
    }
