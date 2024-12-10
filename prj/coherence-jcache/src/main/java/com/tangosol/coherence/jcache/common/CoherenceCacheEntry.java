/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import java.util.Map;

import javax.cache.Cache;

/**
 * A {@link javax.cache.Cache.Entry} implementation.
 *
 * @param <K>  the type of keys
 * @param <V>  the type of values
 *
 * @author jf 2014.11.06
 * @since Coherence 12.1.3
 */
public class CoherenceCacheEntry<K, V>
        implements Cache.Entry<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link CoherenceCacheEntry} given a {@link Map.Entry}.
     *
     * @param entry  the entry
     */
    public CoherenceCacheEntry(Map.Entry<K, V> entry)
        {
        m_key      = entry.getKey();
        m_value    = entry.getValue();
        m_valueOld = null;
        m_entry    = entry;
        }

    /**
     * Constructs a {@link CoherenceCacheEntry} given a key and value
     *
     * @param key    the key of the entry
     * @param value  the value of the entry
     */
    public CoherenceCacheEntry(K key, V value)
        {
        if (key == null)
            {
            throw new NullPointerException("Keys cannot be null");
            }

        if (value == null)
            {
            throw new NullPointerException("Values cannot be null");
            }

        m_key      = key;
        m_value    = value;
        m_valueOld = null;
        m_entry    = null;
        }

    /**
     * Constructs a {@link CoherenceCacheEntry} given a key, value and
     * previous value.
     *
     * @param key       the key of the entry
     * @param value     the value of the entry
     * @param oldValue  the previous value of the entry
     */
    public CoherenceCacheEntry(K key, V value, V oldValue)
        {
        if (key == null)
            {
            throw new NullPointerException("Keys cannot be null");
            }

        if (value == null)
            {
            throw new NullPointerException("Values cannot be null");
            }

        if (oldValue == null)
            {
            throw new NullPointerException("Previous values cannot be null");
            }

        m_key      = key;
        m_value    = value;
        m_valueOld = oldValue;
        m_entry    = null;
        }

    // ----- Cache.Entry interface ------------------------------------------

    @Override
    public K getKey()
        {
        return m_key;
        }

    @Override
    public V getValue()
        {
        return m_value;
        }

    @Override
    public <T> T unwrap(Class<T> clazz)
        {
        if (clazz != null && clazz.isInstance(this))
            {
            return (T) this;
            }
        else if (m_entry != null && clazz.isAssignableFrom(m_entry.getClass()))
            {
            return (T) m_entry;
            }
        else
            {
            throw new IllegalArgumentException("Class " + clazz + " is unknown to this implementation");
            }
        }

    // ----- CoherenceCacheEntry methods ------------------------------------

    /**
     * Obtains the old value for the entry.
     *
     * @return the old value of the entry or <code>null</code> if one is
     *         not defined
     */
    public V getOldValue()
        {
        return m_valueOld;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object object)
        {
        if (this == object)
            {
            return true;
            }

        if (object == null || getClass() != object.getClass())
            {
            return false;
            }

        CoherenceCacheEntry<?, ?> other = (CoherenceCacheEntry<?, ?>) object;

        return getKey().equals(other.getKey()) && getValue().equals(other.getValue())
               && (getOldValue() == null && other.getOldValue() == null || getOldValue().equals(other.getOldValue()));
        }

    @Override
    public int hashCode()
        {
        return getKey().hashCode();
        }

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
     * The Coherence {@link Map.Entry} that may have been provided to the {@link CoherenceCacheEntry}.
     */
    private final Map.Entry<K, V> m_entry;
    }
