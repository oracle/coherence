/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.internal.util.DistributedAsyncNamedCache;
import com.tangosol.internal.util.VersionHelper;
import com.tangosol.internal.util.processor.CacheProcessors;

import com.tangosol.net.cache.CacheMap;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

    /**
     * Copies all the mappings from the specified map to this map with the specified expiry delay.
     * <p/>
     * NOTE: This method was introduced in a patch to Coherence. All the cluster members must be
     * running with a compatible version (the patch or higher) for this method to work.
     * See {@link DistributedAsyncNamedCache#IS_BINARY_PROCESSOR_COMPATIBLE}
     *
     * @param map      mappings to be added to this map
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
    default CompletableFuture<Void> putAll(Map<? extends K, ? extends V> map, long cMillis)
        {
        if (getNamedCache().getCacheService().isVersionCompatible(DistributedAsyncNamedCache.IS_BINARY_PROCESSOR_COMPATIBLE))
            {
            return invokeAll(map.keySet(), CacheProcessors.putAll(map, cMillis)).thenAccept(nil -> {});
            }
        CacheService service  = getNamedCache().getService();
        int          nVersion = service.getMinimumServiceVersion();
        throw new UnsupportedOperationException("the whole cluster is not running a compatible version to execute " +
                "this method (version=\"" + VersionHelper.toVersionString(nVersion, true) +
                "\" encoded=" + nVersion + ")");
        }
    }
