/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryEventFilter;

/**
 * An {@link java.util.Iterator}s to allow filtering of {@link javax.cache.event.CacheEntryEvent}s.
 *
 * @param <K> the type of keys
 * @param <V> the type of value
 *
 * @author jf  2013.10.24
 * @since Coherence 12.1.3
 */
public class CoherenceCacheEntryEventFilteringIterator<K, V>
        implements Iterator<CacheEntryEvent<K, V>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an {@link CoherenceCacheEntryEventFilteringIterator}.
     *
     * @param iterator the underlying m_iterator to m_filter
     * @param filter   the m_filter to apply to entries in the m_iterator
     */
    public CoherenceCacheEntryEventFilteringIterator(Iterator<CacheEntryEvent<K, V>> iterator,
        CacheEntryEventFilter<? super K, ? super V> filter)
        {
        m_iterator  = iterator;
        m_filter    = filter;
        m_entryNext = null;
        }

    // ----- CoherenceCacheEntryEventFilteringIterator methods --------------

    /**
     * Fetches the next available, entry that satisfies the m_filter from
     * the underlying m_iterator
     */
    private void fetch()
        {
        while (m_entryNext == null && m_iterator.hasNext())
            {
            CacheEntryEvent<K, V> entry = m_iterator.next();

            if (m_filter.evaluate(entry))
                {
                m_entryNext = entry;
                }
            }
        }

    // ----- Iterator interface ---------------------------------------------

    @Override
    public boolean hasNext()
        {
        if (m_entryNext == null)
            {
            fetch();
            }

        return m_entryNext != null;
        }

    @Override
    public CacheEntryEvent<K, V> next()
        {
        if (hasNext())
            {
            CacheEntryEvent<K, V> entry = m_entryNext;

            // reset m_entryNext to force fetching the next available entry
            m_entryNext = null;

            return entry;
            }
        else
            {
            throw new NoSuchElementException();
            }
        }

    @Override
    public void remove()
        {
        m_iterator.remove();
        m_entryNext = null;
        }

    // ------ data members --------------------------------------------------

    /**
     * The underlying m_iterator to m_filter.
     */
    private Iterator<CacheEntryEvent<K, V>> m_iterator;

    /**
     * The m_filter to apply to Cache Entry Events in the {@link java.util.Iterator}.
     */
    private CacheEntryEventFilter<? super K, ? super V> m_filter;

    /**
     * The next available Cache Entry Event that satisfies the m_filter.
     * (when null we must seek to find the next event)
     */
    private CacheEntryEvent<K, V> m_entryNext;
    }
