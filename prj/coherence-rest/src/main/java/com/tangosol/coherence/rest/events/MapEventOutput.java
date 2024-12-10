/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.events;


import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.WrapperCollections;

import com.tangosol.util.filter.MapEventFilter;

import java.io.IOException;

import java.util.HashSet;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;


/**
 * {@link MapListener} implementation that converts Coherence {@code MapEvents}
 * into Jersey Server Sent Events (SSE).
 *
 * @author as  2015.06.25
 */
public class MapEventOutput<K, V>
        extends EventOutput
        implements MapListener<K, V>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct MapEventOutput instance.
     *
     * @param cache   the cache to register listener with
     * @param fLite   the flag specifying whether to use lite events
     */
    public MapEventOutput(NamedCache<K, V> cache, boolean fLite)
        {
        m_cache = cache;
        m_fLite = fLite;
        }

    // ---- fluent API ------------------------------------------------------

    /**
     * Set the filter to listen on.
     *
     * @param filter  the filter to listen on
     *
     * @return this MapEventOutput
     */
    public MapEventOutput setFilter(Filter filter)
        {
        if (m_type == Type.KEY)
            {
            throw new IllegalStateException("Only key or filter can be set, but not both");
            }

        m_filter = filter instanceof MapEventFilter
                   ? (MapEventFilter) filter
                   : new MapEventFilter(MapEventFilter.E_ALL, filter);
        m_type   = Type.FILTER;
        return this;
        }

    /**
     * Set the key to listen on.
     *
     * @param key  the key to listen on
     *
     * @return this MapEventOutput
     */
    public MapEventOutput setKey(K key)
        {
        if (m_type == Type.FILTER)
            {
            throw new IllegalStateException("Only key or filter can be set, but not both");
            }

        m_key  = key;
        m_type = Type.KEY;
        return this;
        }

    // ---- EventOutput class -----------------------------------------------

    @Override
    public void close() throws IOException
        {
        unregister();
        super.close();
        }

    // ---- MapListener interface -------------------------------------------

    @Override
    public void entryInserted(MapEvent<K, V> evt)
        {
        writeEvent("insert", evt);
        }

    @Override
    public void entryUpdated(MapEvent<K, V> evt)
        {
        String sName = "update";
        if (m_type == Type.FILTER)
            {
            Filter  filter = m_filter.getFilter();
            boolean fOld   = InvocableMapHelper.evaluateEntry(filter, evt.getOldEntry());
            boolean fNew   = InvocableMapHelper.evaluateEntry(filter, evt.getNewEntry());
            if (!fOld && fNew)
                {
                sName = "insert";
                }
            else if (fOld && !fNew)
                {
                sName = "delete";
                }
            }
        writeEvent(sName, evt);
        }

    @Override
    public void entryDeleted(MapEvent<K, V> evt)
        {
        writeEvent("delete", evt);
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Register this listener.
     */
    public void register()
        {
        switch (m_type)
            {
            case CACHE:
                m_cache.addMapListener(this);
                break;
            case FILTER:
                m_cache.addMapListener(this, m_filter, m_fLite);
                break;
            case KEY:
                m_cache.addMapListener(this, m_key, m_fLite);
                break;
            }

        REGISTRY.add(this);
        if (Logger.isEnabled(Logger.FINEST))
            {
            Logger.finest("Registered listener: " + this);
            }
        }

    /**
     * Unregister this listener.
     */
    protected void unregister()
        {
        switch (m_type)
            {
            case CACHE:
                m_cache.removeMapListener(this);
                break;
            case FILTER:
                m_cache.removeMapListener(this, m_filter);
                break;
            case KEY:
                m_cache.removeMapListener(this, m_key);
                break;
            }

        REGISTRY.remove(this);
        if (Logger.isEnabled(Logger.FINEST))
            {
            Logger.finest("Unregistered listener: " + this);
            }
        }

    /**
     * Write single event to this EventOutput.
     *
     * @param sName  the event name
     * @param evt    the event to write
     */
    protected void writeEvent(String sName, MapEvent<? extends K, ? extends V> evt)
        {
        try
            {
            write(createEvent(sName, evt));
            }
        catch (IOException e)
            {
            if (isClosed())
                {
                unregister();
                }
            else
                {
                throw new RuntimeException(e);
                }
            }
        }

    /**
     * Convert MapEvent to JSON-based OutboundEvent that can be sent to the client.
     *
     * @param sName  the event name
     * @param evt    the MapEvent to convert
     *
     * @return an OutboundEvent with a given name and MapEvent data in JSON format
     */
    protected OutboundEvent createEvent(String sName, MapEvent<? extends K, ? extends V> evt)
        {
        return new OutboundEvent.Builder()
                .name(sName)
                .data(SimpleMapEvent.class, new SimpleMapEvent<>(evt))
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .build();
        }

    // ---- Object methods --------------------------------------------------

    @Override
    public String toString()
        {
        return "MapEventOutput{" +
               "cache=" + m_cache.getCacheName() +
               ", filter=" + m_filter +
               ", key=" + m_key +
               ", fLite=" + m_fLite +
               ", type=" + m_type +
               '}';
        }

    // ---- enum: Type ------------------------------------------------------

    /**
     * The type of the MapListener to register.
     */
    private enum Type
        {
        CACHE,
        FILTER,
        KEY
        }

    // ---- static members --------------------------------------------------

    /**
     * Registry of all active MapEventOutput instances.
     */
    private static final WrapperCollections.ConcurrentWrapperSet<MapEventOutput> REGISTRY =
            new WrapperCollections.ConcurrentWrapperSet<>(new HashSet<>());

    // ---- data members ----------------------------------------------------

    /**
     * The cache to register listener for.
     */
    private NamedCache<K, V> m_cache;

    /**
     * The filter to register listener for (optional).
     */
    private MapEventFilter m_filter;

    /**
     * The key to register listener for (optional).
     */
    private K m_key;

    /**
     * The flag specifying whether to use lite events.
     */
    private boolean m_fLite;

    /**
     * The type of the listener to register.
     */
    private Type m_type = Type.CACHE;
    }
