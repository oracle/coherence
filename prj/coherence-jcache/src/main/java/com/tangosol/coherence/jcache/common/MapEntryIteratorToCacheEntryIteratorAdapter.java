/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import java.util.Iterator;
import java.util.Map;

import javax.cache.Cache;

/**
 * An adapter that converts an {@link Iterator} over {@link Map} entries
 * into an {@link Iterator} over {@link Cache} entries.
 *
 * @param <K>  the type of the keys
 * @param <V>  the type of the values
 *
 * @author bo  2013.10.24
 * @since Coherence 12.1.3
 */
public class MapEntryIteratorToCacheEntryIteratorAdapter<K, V>
        implements Iterator<Cache.Entry<K, V>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link MapEntryIteratorToCacheEntryIteratorAdapter}
     * that lazily adapts an {@link Iterator} over {@link Map} entries in
     * to an {@link Iterator} over {@link Cache} entries.
     *
     * @param iterMap the {@link Map} entry {@link Iterator}
     */
    public MapEntryIteratorToCacheEntryIteratorAdapter(Iterator<Map.Entry<K, V>> iterMap)
        {
        m_iterMap = iterMap;
        }

    // ----- Iterator interface ------------------------------------------

    @Override
    public boolean hasNext()
        {
        return m_iterMap.hasNext();
        }

    @Override
    public Cache.Entry<K, V> next()
        {
        return new CoherenceCacheEntry<K, V>(m_iterMap.next());
        }

    @Override
    public void remove()
        {
        throw new UnsupportedOperationException("Can't remove from a " + this.getClass().getName());
        }

    // ------ data members --------------------------------------------------

    /**
     * The underlying {@link Map} entry {@link Iterator}.
     */
    private Iterator<Map.Entry<K, V>> m_iterMap;
    }
