/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.passthroughcache;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;

import java.util.Collections;

import javax.cache.Cache;

import javax.cache.event.*;

/**
 * An internal {@link MapListener} implementation that delegates {@link MapEvent}s
 * onto a {@link CacheEntryListener}.
 *
 * @param <K>  the type of the {@link Cache} keys
 * @param <V>  the type of the {@link Cache} values
 *
 * @author bo  2013.11.01
 * @since Coherence 12.1.3
 */
public class PassThroughMapListenerAdapter<K, V>
        implements MapListener
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link PassThroughMapListenerAdapter}.
     *
     * @param cache               the {@link Cache} on which the {@link MapListener} is operating
     * @param cacheEntryListener  the {@link CacheEntryListener} to which to delegate
     *                            and adapt {@link MapEvent}s
     */
    public PassThroughMapListenerAdapter(Cache<K, V> cache, CacheEntryListener<? super K, ? super V> cacheEntryListener)
        {
        m_cache              = cache;
        m_cacheEntryListener = cacheEntryListener;
        }

    // ----- MapListener methods --------------------------------------------

    @Override
    public void entryInserted(MapEvent event)
        {
        PassThroughCacheEntryEvent<K, V> entryEvent = new PassThroughCacheEntryEvent<K, V>(m_cache, EventType.CREATED,
                                                          event);

        if (m_cacheEntryListener instanceof CacheEntryCreatedListener)
            {
            ((CacheEntryCreatedListener) m_cacheEntryListener).onCreated(Collections.singleton(entryEvent));
            }
        }

    @Override
    public void entryUpdated(MapEvent event)
        {
        PassThroughCacheEntryEvent<K, V> entryEvent = new PassThroughCacheEntryEvent<K, V>(m_cache, EventType.UPDATED,
                                                          event);

        if (m_cacheEntryListener instanceof CacheEntryUpdatedListener)
            {
            ((CacheEntryUpdatedListener) m_cacheEntryListener).onUpdated(Collections.singleton(entryEvent));
            }
        }

    @Override
    public void entryDeleted(MapEvent event)
        {
        PassThroughCacheEntryEvent<K, V> entryEvent = new PassThroughCacheEntryEvent<K, V>(m_cache, EventType.REMOVED,
                                                          event);

        if (m_cacheEntryListener instanceof CacheEntryRemovedListener)
            {
            ((CacheEntryRemovedListener) m_cacheEntryListener).onRemoved(Collections.singleton(entryEvent));
            }
        }

    // ----- PassThroughMapListenerAdapter methods --------------------------

    /**
     * Obtains the {@link CacheEntryListener} to which
     * {@link MapEvent}s will be delegated.
     *
     * @return  the {@link CacheEntryListener}
     */
    public CacheEntryListener<? super K, ? super V> getCacheEntryListener()
        {
        return m_cacheEntryListener;
        }

    // ------ data members --------------------------------------------------

    /**
     * The {@link Cache} on which the {@link CacheEntryEvent}s will occur.
     */
    private Cache<K, V> m_cache;

    /**
     * The {@link CacheEntryListener} to which {@link MapListener}
     * {@link MapEvent}s will be delegated.
     */
    private CacheEntryListener<? super K, ? super V> m_cacheEntryListener;
    }
