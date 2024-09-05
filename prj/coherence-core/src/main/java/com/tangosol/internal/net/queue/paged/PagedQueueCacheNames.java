/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedMap;
import com.tangosol.net.cache.NearCache;
import com.tangosol.util.NullImplementation;

/**
 * An enum representing the different caches used for a page queue.
 */
public enum PagedQueueCacheNames
    {
    /**
     * The buckets cache name.
     */
    Buckets("$buckets", true),
    /**
     * The elements cache name.
     */
    Elements("", true),
    /**
     * The versions cache name.
     */
    Version("$versions", false),
    /**
     * The info cache name.
     */
    Info("$info", false),
    ;

    /**
     * Create a {@link PagedQueueCacheNames} enum.
     *
     * @param sSuffix  the cache name suffix
     */
    PagedQueueCacheNames(String sSuffix, boolean fPassThru)
        {
        m_sSuffix   = sSuffix;
        m_fPassThru = fPassThru;
        }

    /**
     * Return the cache name suffix for this cache.
     *
     * @return the cache name suffix for this cache
     */
    public String suffix()
        {
        return m_sSuffix;
        }

    /**
     * Return this cache name for the queue represented by the specified {@link NamedMap}.
     *
     * @param map  one of the {@link NamedMap} instances used by a paged queue
     *
     * @return this cache name for the queue represented by the specified {@link NamedMap}
     */
    public String getCacheName(NamedMap<?, ?> map)
        {
        return getCacheName(map.getName());
        }

    /**
     * Return this cache name for the queue represented by the specified {@link BackingMapContext}.
     *
     * @param ctx  one of the {@link BackingMapContext} instances used by a paged queue
     *
     * @return this cache name for the queue represented by the specified {@link BackingMapContext}
     */
    public String getCacheName(BackingMapContext ctx)
        {
        return getCacheName(ctx.getCacheName());
        }

    /**
     * Return this cache name for the queue represented by the specified cache name.
     *
     * @param sCacheName  one of the cache names used by a paged queue
     *
     * @return this cache name for the queue represented by the specified cache name
     */
    public String getCacheName(String sCacheName)
        {
        if (sCacheName.endsWith(Buckets.m_sSuffix))
            {
            return sCacheName.substring(0, sCacheName.length() - Buckets.m_sSuffix.length()) + m_sSuffix;
            }
        if (sCacheName.endsWith(Version.m_sSuffix))
            {
            return sCacheName.substring(0, sCacheName.length() - Version.m_sSuffix.length()) + m_sSuffix;
            }
        if (sCacheName.endsWith(Info.m_sSuffix))
            {
            return sCacheName.substring(0, sCacheName.length() - Info.m_sSuffix.length()) + m_sSuffix;
            }
        return sCacheName + m_sSuffix;
        }

    public boolean isPassThru()
        {
        return m_fPassThru;
        }

    @SuppressWarnings("unchecked")
    public <K, V> NamedMap<K, V> ensureBinaryMap(String sQueueName, CacheService cacheService)
        {
        NamedMap<K, V> map;
        if (m_fPassThru)
            {
            map = cacheService.ensureCache(sQueueName + m_sSuffix, NullImplementation.getClassLoader());
            }
        else
            {
            ClassLoader  loaderService = cacheService.getContextClassLoader();
            map = cacheService.ensureCache(sQueueName + m_sSuffix, loaderService);
            }
        if (map instanceof NearCache)
            {
            map = ((NearCache<K, V>) map).getBackCache();
            }
        return map;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The cache name suffix.
     */
    private final String m_sSuffix;

    /**
     * A flag that is {@code true} if this cache should be a pass-thru
     * cache for a binary paged queue.
     */
    private final boolean m_fPassThru;
    }
