/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.jcache.localcache.processors;

import com.tangosol.coherence.jcache.localcache.LocalCache;
import com.tangosol.coherence.jcache.localcache.LocalCacheValue;

import com.tangosol.util.InvocableMap;

import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;

import javax.cache.processor.MutableEntry;

/**
 * A {@link javax.cache.processor.MutableEntry} that is used by {@link javax.cache.processor.EntryProcessor}s.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author jf 2013.12.19
 * @since Coherence 12.1.3
 */
public class EntryProcessorEntry<K, V>
        implements MutableEntry<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link EntryProcessorEntry}
     *
     * @param cache       LocalCache to get context info
     * @param entry internal Coherence NamedCache entry with key & value pair of internalKey and LocalCacheValue.
     * @param ldtNow         the current time
     */
    public EntryProcessorEntry(LocalCache cache, InvocableMap.Entry entry, long ldtNow)
        {
        this.m_cache       = cache;
        this.m_key         = (K) cache.getKeyConverter().fromInternal(entry.getKey());
        this.m_cachedValue = (LocalCacheValue) entry.getValue();
        this.m_operation   = MutableEntryOperation.NONE;
        this.m_value       = null;
        this.m_ldtNow      = ldtNow;
        this.m_cacheLoader = cache.getContext().isReadThrough() ? cache.getContext().getCacheLoader() : null;
        }

    // ----- MutableEntry interface -----------------------------------------

    @Override
    public K getKey()
        {
        return m_key;
        }

    @Override
    public V getValue()
        {
        if (m_operation == MutableEntryOperation.NONE)
            {
            if (m_cachedValue == null || m_cachedValue.isExpiredAt(m_ldtNow))
                {
                m_value = null;
                }
            else if (m_value == null)
                {
                Object internalValue = m_cachedValue.getInternalValue(m_ldtNow);

                m_value = internalValue == null ? null : m_cache.getValueConverter().fromInternal(internalValue);
                }
            }

        if (m_value != null)
            {
            // mark as Accessed so AccessedExpiry will be computed upon return from entry processor.
            if (m_operation == MutableEntryOperation.NONE)
                {
                m_operation = MutableEntryOperation.ACCESS;
                }
            }
        else
            {
            // check for read-through
            if (m_cacheLoader != null)
                {
                try
                    {
                    m_value = m_cacheLoader.load(m_key);

                    if (m_value != null)
                        {
                        m_operation = MutableEntryOperation.LOAD;

                        Object internalValue = m_cache.getValueConverter().toInternal(m_value);

                        m_cachedValue = LocalCacheValue.createLoadedLocalCacheValue(internalValue, m_ldtNow,
                            m_cache.getContext().getExpiryPolicy());
                        }
                    }
                catch (Exception e)
                    {
                    if (!(e instanceof CacheLoaderException))
                        {
                        throw new CacheLoaderException("Exception in CacheLoader", e);
                        }
                    else
                        {
                        throw(RuntimeException) e;
                        }
                    }
                }
            }

        return m_value;
        }

    @Override
    public boolean exists()
        {
        return (m_operation == MutableEntryOperation.NONE && m_cachedValue != null
                && !m_cachedValue.isExpiredAt(m_ldtNow)) || m_value != null;
        }

    @Override
    public void remove()
        {
        m_operation = (m_operation == MutableEntryOperation.CREATE || m_operation == MutableEntryOperation.LOAD)
                      ? MutableEntryOperation.NONE : MutableEntryOperation.REMOVE;
        m_value       = null;
        m_cachedValue = null;
        }

    @Override
    public void setValue(V value)
        {
        if (value == null)
            {
            throw new NullPointerException();
            }

        m_operation = m_cachedValue == null || m_cachedValue.isExpiredAt(m_ldtNow)
                      ? MutableEntryOperation.CREATE : MutableEntryOperation.UPDATE;
        this.m_value = value;

        if (m_cachedValue == null && m_operation == MutableEntryOperation.CREATE)
            {
            // creating LocalCacheValue but defer till end of processing to finalize this setValue
            // deferring write-through and updating internal entry in cache.
            Object internalValue = m_cache.getValueConverter().toInternal(value);

            m_cachedValue = LocalCacheValue.createLocalCacheValue(internalValue, m_ldtNow,
                m_cache.getContext().getExpiryPolicy());
            }
        }

    @Override
    public <T> T unwrap(Class<T> clazz)
        {
        throw new IllegalArgumentException("Can't unwrap an EntryProcessor Entry");
        }

    // ----- EntryProcessorEntry methods ------------------------------------

    /**
     * Get operation
     * @return Return the m_operation
     */
    public MutableEntryOperation getOperation()
        {
        return m_operation;
        }

    /**
     * Get value
     *
     * @return {@link LocalCacheValue}
     */
    public LocalCacheValue getCacheValue()
        {
        return m_cachedValue;
        }

    // ----- data members ---------------------------------------------------

    /**
     * context for processing the entry
     */
    private final LocalCache<K, V> m_cache;

    /**
     * The m_key of the {@link javax.cache.processor.MutableEntry}.
     */
    private final K m_key;

    /**
     * The {@link LocalCacheValue} for the {@link javax.cache.processor.MutableEntry}.
     */
    private LocalCacheValue m_cachedValue;

    /**
     * The new m_value for the {@link javax.cache.processor.MutableEntry}.
     */
    private V m_value;

    /**
     * The {@link MutableEntryOperation} to be performed on the {@link javax.cache.processor.MutableEntry}.
     */
    private MutableEntryOperation m_operation;

    /**
     * The time (since the Epoc) when the MutableEntry was created.
     */
    private long m_ldtNow;

    /**
     * CacheLoader to call if getValue() would return null.
     */
    private CacheLoader<K, V> m_cacheLoader;
    }
