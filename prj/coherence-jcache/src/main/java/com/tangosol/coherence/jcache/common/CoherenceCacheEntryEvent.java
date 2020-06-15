/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import com.tangosol.util.MapEvent;

import javax.cache.Cache;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.EventType;

/**
 * A minimal implementation of the {@link CacheEntryEvent}.
 *
 * @param <K>  the type of keys
 * @param <V>  the type of values
 *
 * @author jf  2013.10.24
 * @since Coherence 12.1.3
 */
public class CoherenceCacheEntryEvent<K, V>
        extends CacheEntryEvent<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link CoherenceCacheEntryEvent} based on a specific key and value.
     *
     * @param cache      the {@link Cache} on which the event occurred
     * @param eventType  the {@link EventType} for the event
     * @param key        the key of the event
     * @param value      the value of the event
     */
    public CoherenceCacheEntryEvent(Cache<K, V> cache, EventType eventType, K key, V value)
        {
        super(cache, eventType);
        m_key                = key;
        m_value              = value;
        m_valueOld           = null;
        m_fOldValueAvailable = false;
        }

    /**
     * Constructs a {@link CoherenceCacheEntryEvent} based on a specific key, value
     * and old value.
     *
     * @param cache      the {@link Cache} on which the event occurred
     * @param eventType  the {@link EventType} for the event
     * @param key        the key of the event
     * @param value      the value of the event
     * @param oldValue   the old value of the event
     */
    public CoherenceCacheEntryEvent(Cache<K, V> cache, EventType eventType, K key, V value, V oldValue)
        {
        super(cache, eventType);
        m_key                = key;
        m_value              = value;
        m_valueOld           = oldValue;
        m_fOldValueAvailable = true;
        }

    // ----- CacheEntryEvent interface --------------------------------------

    @Override
    public K getKey()
        {
        return m_key;
        }

    /**
     * Return current value.
     *
     * @see #JCACHE_1_0_COMPATIBILITY_MODE
     *
     * @return current value of entry in cache
     */
    @Override
    public V getValue()
        {
        switch (getEventType())
            {
            case CREATED:
            case UPDATED:
                return m_value;

            case EXPIRED:
            case REMOVED:
                return JCACHE_1_0_COMPATIBILITY_MODE ? (V) getOldValue() : null;

            default:
                return null;
            }
        }

    @Override
    public <T> T unwrap(Class<T> clz)
        {
        if (clz != null && clz.isInstance(this))
            {
            return (T) this;
            }
        else
            {
            throw new IllegalArgumentException("Class " + clz + " is unknown to this implementation");
            }
        }

    @Override
    public V getOldValue()
        {
        return m_valueOld;
        }

    @Override
    public boolean isOldValueAvailable()
        {
        return m_fOldValueAvailable;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        StringBuffer buf = new StringBuffer(100);

        buf.append("CacheEntryEvent[");

        buf.append(" key=");
        buf.append(getKey());
        buf.append(", value=");

        try
            {
            buf.append(getValue());
            }
        catch (Throwable e)
            {
            }

        try
            {
            if (isOldValueAvailable())
                {
                buf.append(", oldValue=");
                buf.append(getOldValue());
                }
            }
        catch (Throwable e)
            {
            }

        return buf.toString();
        }

    // ----- constants ------------------------------------------------------

    /**
     * When enabled, method {@link #getValue()}returns {@link #getOldValue() oldValue} for {@link EventType#EXPIRED} and {@link EventType#REMOVED}.
     */
    static final private boolean JCACHE_1_0_COMPATIBILITY_MODE = true;

    // ----- data members ---------------------------------------------------

    /**
     * The key of the {@link CoherenceCacheEntry}.
     */
    private final K m_key;

    /**
     * The value of the {@link CoherenceCacheEntry}.
     */
    private final V m_value;

    /**
     * The old value of the {@link CoherenceCacheEntry}.
     */
    private final V m_valueOld;

    /**
     * A flag indicating if the old value has been provided and is available.
     */
    private final boolean m_fOldValueAvailable;
    }
