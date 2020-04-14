/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import java.util.Iterator;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryEventFilter;

/**
 * An adapter to provide {@link Iterable}s over Cache Entries, those of which
 * are filtered using a {@link javax.cache.event.CacheEntryEventFilter}.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 *
 * @author jf  2013.10.24
 * @since Coherence 12.1.3
 */
public class CoherenceCacheEntryEventFilteringIterable<K, V>
        implements Iterable<CacheEntryEvent<K, V>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an {@link CoherenceCacheEntryEventFilteringIterable}.
     *
     * @param iterable  the underlying iterable to filter
     * @param filter    the filter to apply to entries in the iterable
     */
    public CoherenceCacheEntryEventFilteringIterable(Iterable<CacheEntryEvent<K, V>> iterable,
        CacheEntryEventFilter<? super K, ? super V> filter)
        {
        m_iterable = iterable;
        m_filter   = filter;
        }

    // ------ CoherenceCacheEntryEventFilteringIterable methods -------------

    @Override
    public Iterator<CacheEntryEvent<K, V>> iterator()
        {
        return new CoherenceCacheEntryEventFilteringIterator<K, V>(m_iterable.iterator(), m_filter);
        }

    // ------ data members --------------------------------------------------

    /**
     * The underlying {@link Iterable} to m_filter.
     */
    private final Iterable<CacheEntryEvent<K, V>> m_iterable;

    /**
     * The m_filter to apply to entries in the produced {@link java.util.Iterator}s.
     */
    private final CacheEntryEventFilter<? super K, ? super V> m_filter;
    }
