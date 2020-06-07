/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.cdi.events.Cache;
import com.oracle.coherence.cdi.events.Deleted;
import com.oracle.coherence.cdi.events.Inserted;
import com.oracle.coherence.cdi.events.Service;
import com.oracle.coherence.cdi.events.Updated;

import com.tangosol.net.NamedCache;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.ObservableMap;

import java.lang.annotation.Annotation;

import javax.enterprise.event.Event;

import javax.inject.Inject;

/**
 * {@link MapListener} implementation that dispatches Coherence {@code MapEvent}s
 * to registered CDI observers.
 *
 * @author Aleks Seovic  2020.04.14
 */
public abstract class CdiMapListener<K, V>
        implements MapListener<K, V>
    {
    // ---- MapListener interface -------------------------------------------

    @Override
    public void entryInserted(MapEvent<K, V> event)
        {
        fireEvent(event, Inserted.Literal.INSTANCE);
        }

    @Override
    public void entryUpdated(MapEvent<K, V> event)
        {
        fireEvent(event, Updated.Literal.INSTANCE);
        }

    @Override
    public void entryDeleted(MapEvent<K, V> event)
        {
        fireEvent(event, Deleted.Literal.INSTANCE);
        }

    // ---- data members ----------------------------------------------------

    /**
     * Fires the event to CDI observers after adding {@link Cache @Cache} and
     * {@link Service} qualifiers (if possible).
     *
     * @param event       {@link MapEvent} to fire
     * @param qualifiers  the additional event qualifiers
     */
    @SuppressWarnings("unchecked")
    private void fireEvent(MapEvent<K, V> event, Annotation... qualifiers)
        {
        Event<MapEvent<K, V>> e   = m_mapEventEvent.select(qualifiers);
        ObservableMap<K, V>   map = event.getMap();

        if (map instanceof NamedCache)
            {
            NamedCache<K, V>    cache   = (NamedCache<K, V>) map;
            Cache.Literal   cacheName   = Cache.Literal.of(cache.getCacheName());
            Service.Literal serviceName = Service.Literal.of(cache.getCacheService().getInfo().getServiceName());

            e = e.select(cacheName, serviceName);
            }

        e.fireAsync(event);
        e.fire(event);
        }

    // ---- data members ----------------------------------------------------

    /**
     * CDI event dispatcher for map events.
     */
    @Inject
    private Event<MapEvent<K, V>> m_mapEventEvent;
    }
