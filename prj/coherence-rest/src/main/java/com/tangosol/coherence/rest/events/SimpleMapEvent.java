/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.events;


import com.fasterxml.jackson.annotation.JsonProperty;

import com.tangosol.net.NamedCache;

import com.tangosol.util.MapEvent;


/**
 * Simple representation of {@link MapEvent} that can be marshalled to JSON.
 *
 * @author as  2015.06.26
 */
public class SimpleMapEvent<K, V>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public SimpleMapEvent()
        {
        }

    /**
     * Construct SimpleMapEvent instance.
     *
     * @param evt  the MapEvent to construct this instance from
     */
    public SimpleMapEvent(MapEvent<? extends K, ? extends V> evt)
        {
        m_sCache   = evt.getMap() instanceof NamedCache
                     ? ((NamedCache) evt.getMap()).getCacheName()
                     : null;
        m_nType    = evt.getId();
        m_key      = evt.getKey();
        m_oldValue = evt.getOldValue();
        m_newValue = evt.getNewValue();
        }

    // ---- accessors -------------------------------------------------------

    /**
     * Return the cache name.
     *
     * @return the cache name
     */
    public String getCache()
        {
        return m_sCache;
        }

    /**
     * Set the cache name.
     *
     * @param sCache the cache name
     */
    public void setCache(String sCache)
        {
        m_sCache = sCache;
        }

    /**
     * Return the event type.
     * <p>
     * The value is one of:
     * <ol>
     *     <li>INSERTED</li>
     *     <li>UPDATED</li>
     *     <li>DELETED</li>
     * </ol>
     * <p>
     * Unlike SSE event name, which is calculated for update operations when
     * filter-based listeners are used, this attribute always represents the
     * actual operation that occured on the underlying cache.
     *
     * @return the event type
     */
    public int getType()
        {
        return m_nType;
        }

    /**
     * Set the event type.
     *
     * @param nType the event type
     */
    public void setType(int nType)
        {
        m_nType = nType;
        }

    /**
     * Return the key associated with this event.
     *
     * @return the key
     */
    public K getKey()
        {
        return m_key;
        }

    /**
     * Set the key for this event.
     *
     * @param key  the key
     */
    public void setKey(K key)
        {
        m_key = key;
        }

    /**
     * Return the old value associated with this event.
     * <p>
     * The old value represents a value deleted from or updated in a map.
     * It is always null for "insert" notifications.
     *
     * @return the old value
     */
    public V getOldValue()
        {
        return m_oldValue;
        }

    /**
     * Set the old value.
     *
     * @param oldValue the old value
     */
    public void setOldValue(V oldValue)
        {
        m_oldValue = oldValue;
        }

    /**
     * Return the new value associated with this event.
     * <p>
     * The new value represents a new value inserted into or updated in
     * a map. It is always null for "delete" notifications.
     *
     * @return the new value
     */
    public V getNewValue()
        {
        return m_newValue;
        }

    /**
     * Set the new value.
     *
     * @param newValue  the new value
     */
    public void setNewValue(V newValue)
        {
        m_newValue = newValue;
        }

    // ---- object methods --------------------------------------------------

    @Override
    public String toString()
        {
        return "SimpleMapEvent{" +
               "cache='" + m_sCache + '\'' +
               ", type=" + m_nType +
               ", key=" + m_key +
               ", oldValue=" + m_oldValue +
               ", newValue=" + m_newValue +
               '}';
        }

    // ---- data members ----------------------------------------------------

    /**
     * The name of the cache event occured in.
     */
    @JsonProperty("cache")
    private String m_sCache;

    /**
     * The type of the event.
     */
    @JsonProperty("type")
    private int m_nType;

    /**
     * The key of an entry that triggered the event.
     */
    @JsonProperty("key")
    private K m_key;

    /**
     * The old value of an entry that triggered the event.
     */
    @JsonProperty("oldValue")
    private V m_oldValue;

    /**
     * The new value of an entry that triggered the event.
     */
    @JsonProperty("newValue")
    private V m_newValue;
    }
