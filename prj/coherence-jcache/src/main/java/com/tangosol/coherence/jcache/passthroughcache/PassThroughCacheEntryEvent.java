/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.passthroughcache;

import com.tangosol.util.MapEvent;

import javax.cache.Cache;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.EventType;

/**
 * An implementation of a {@link CacheEntryEvent} based on an underlying
 * Coherence {@link MapEvent}.
 *
 * @param <K>  the type of the {@link Cache} keys
 * @param <V>  the type of the {@link Cache} values
 *
 * @author bo  2013.11.01
 * @since Coherence 12.1.3
 */
public class PassThroughCacheEntryEvent<K, V>
        extends CacheEntryEvent<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link PassThroughCacheEntryEvent}.
     *
     * @param cache  the {@link Cache} on which the {@link CacheEntryEvent} occurred
     * @param type   the {@link EventType}
     * @param event  the underlying {@link MapEvent}
     */
    public PassThroughCacheEntryEvent(Cache<K, V> cache, EventType type, MapEvent event)
        {
        super(cache, type);

        m_event = event;
        }

    // ----- CacheEntry methods ---------------------------------------------

    @Override
    public K getKey()
        {
        return (K) m_event.getKey();
        }

    @Override
    public V getValue()
        {
        return (V) m_event.getNewValue();
        }

    @Override
    public <T> T unwrap(Class<T> clz)
        {
        if (clz != null && clz.isInstance(m_event))
            {
            return (T) m_event;
            }
        else
            {
            throw new IllegalArgumentException("Unsupported unwrap(" + clz + ")");
            }
        }

    // ----- CacheEntryEvent methods ----------------------------------------

    @Override
    public boolean isOldValueAvailable()
        {
        return m_event.getOldValue() != null;
        }

    @Override
    public V getOldValue()
        {
        return (V) m_event.getOldValue();
        }

    // ----- PassThroughCacheEntryEvent methods -----------------------------

    /**
     * Creates a {@link PassThroughCacheEntryEvent} based on a {@link MapEvent}
     *
     * @param event  the {@link MapEvent}
     *
     * @param <K> the key type
     * @param <V> the value type
     *
     * @return a new {@link PassThroughCacheEntryEvent}
     */
    public static <K, V> PassThroughCacheEntryEvent<K, V> from(MapEvent event)
        {
        // determine the type of the event
        EventType type;

        switch (event.getId())
            {
            case MapEvent.ENTRY_INSERTED:
                type = EventType.CREATED;
                break;

            case MapEvent.ENTRY_UPDATED:
                type = EventType.UPDATED;
                break;

            case MapEvent.ENTRY_DELETED:
                type = EventType.REMOVED;
                break;

            default:
                type = null;
            }

        // TODO: create a null implementation of the Cache based on the MapEvent

        return new PassThroughCacheEntryEvent<K, V>(null, type, event);
        }

    // ------ data members --------------------------------------------------

    /**
     * The underlying {@link MapEvent} to be adapted into a {@link CacheEntryEvent}.
     */
    private MapEvent m_event;
    }
