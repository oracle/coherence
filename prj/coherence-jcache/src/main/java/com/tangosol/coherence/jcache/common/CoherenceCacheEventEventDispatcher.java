/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.jcache.common;

import com.oracle.coherence.common.base.Logger;

import java.util.ArrayList;

import java.util.concurrent.ConcurrentHashMap;

import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;

/**
 * Collects and appropriately dispatches {@link javax.cache.event.CacheEntryEvent}s to
 * {@link javax.cache.event.CacheEntryListener}s.
 *
 * @param <K> the type of keys
 * @param <V> the type of values
 *
 * @author jf  2013.10.24
 * @since Coherence 12.1.3
 */
public class CoherenceCacheEventEventDispatcher<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an {@link CoherenceCacheEventEventDispatcher}.
     */
    public CoherenceCacheEventEventDispatcher()
        {
        m_mapEvt = new ConcurrentHashMap<Class<? extends CacheEntryListener>, ArrayList<CacheEntryEvent<K, V>>>();
        }

    // ----- CoherenceCacheEventEventDispatcher methods ---------------------

    /**
     * Requests that the specified event be prepared for dispatching to the
     * specified type of listeners.
     *
     * @param listenerClass the class of {@link javax.cache.event.CacheEntryListener} that should
     *                         receive the event
     * @param event         the event to be dispatched
     */
    public void addEvent(Class<? extends CacheEntryListener> listenerClass, CacheEntryEvent<K, V> event)
        {
        if (listenerClass == null)
            {
            throw new NullPointerException("listenerClass can't be null");
            }

        if (event == null)
            {
            throw new NullPointerException("event can't be null");
            }

        if (!listenerClass.isInterface() || !CacheEntryListener.class.isAssignableFrom(listenerClass))
            {
            throw new IllegalArgumentException("listenerClass must be an CacheEntryListener interface");
            }

        // for safety
        ArrayList<CacheEntryEvent<K, V>> listEvt;

        synchronized (this)
            {
            listEvt = m_mapEvt.get(listenerClass);

            if (listEvt == null)
                {
                listEvt = new ArrayList<CacheEntryEvent<K, V>>();

                ArrayList<CacheEntryEvent<K, V>> previous = m_mapEvt.putIfAbsent(listenerClass, listEvt);

                if (previous != null)
                    {
                    listEvt = previous;
                    }
                }
            }

        listEvt.add(event);
        }

    /**
     * Dispatches the added events to the listeners defined by the specified
     * {@link javax.cache.configuration.CacheEntryListenerConfiguration}s.
     *
     * @see #addEvent(Class, javax.cache.event.CacheEntryEvent)
     *
     * @param registrations the {@link javax.cache.configuration.CacheEntryListenerConfiguration} defining
     *                         {@link javax.cache.event.CacheEntryListener}s to which to dispatch events
     */
    public void dispatch(Iterable<CoherenceCacheEntryListenerRegistration<K, V>> registrations)
        {
        // TODO: we could really optimize this implementation
        Iterable<CacheEntryEvent<K, V>> events;
        // notify expiry listeners
        events = m_mapEvt.get(CacheEntryExpiredListener.class);

        if (events != null)
            {
            for (CoherenceCacheEntryListenerRegistration<? super K, ? super V> registration : registrations)
                {
                CacheEntryEventFilter<? super K, ? super V> filter = registration.getCacheEntryFilter();
                Iterable<CacheEntryEvent<K, V>> iterable = filter == null
                                                           ? events
                                                           : new CoherenceCacheEntryEventFilteringIterable<K,
                                                               V>(events, filter);

                CacheEntryListener<? super K, ? super V> listener = registration.getCacheEntryListener();

                if (listener instanceof CacheEntryExpiredListener)
                    {
                    try
                        {
                        ((CacheEntryExpiredListener) listener).onExpired(iterable);
                        }
                    catch (Throwable e)
                        {
                        logListenerException(listener, e, "CacheEntryExpiredListener");
                        }
                    }
                }
            }

        // notify create listeners
        events = m_mapEvt.get(CacheEntryCreatedListener.class);

        if (events != null)
            {
            for (CoherenceCacheEntryListenerRegistration<? super K, ? super V> registration : registrations)
                {
                CacheEntryEventFilter<? super K, ? super V> filter = registration.getCacheEntryFilter();
                Iterable<CacheEntryEvent<K, V>> iterable = filter == null
                                                           ? events
                                                           : new CoherenceCacheEntryEventFilteringIterable<K,
                                                               V>(events, filter);

                CacheEntryListener<? super K, ? super V> listener = registration.getCacheEntryListener();

                if (listener instanceof CacheEntryCreatedListener)
                    {
                    try
                        {
                        ((CacheEntryCreatedListener) listener).onCreated(iterable);
                        }
                    catch (Throwable e)
                        {
                        logListenerException(listener, e, "CacheEntryCreatedListener");
                        }
                    }
                }
            }

        // notify update listeners
        events = m_mapEvt.get(CacheEntryUpdatedListener.class);

        if (events != null)
            {
            for (CoherenceCacheEntryListenerRegistration<? super K, ? super V> registration : registrations)
                {
                CacheEntryEventFilter<? super K, ? super V> filter = registration.getCacheEntryFilter();
                Iterable<CacheEntryEvent<K, V>> iterable = filter == null
                                                           ? events
                                                           : new CoherenceCacheEntryEventFilteringIterable<K,
                                                               V>(events, filter);

                CacheEntryListener<? super K, ? super V> listener = registration.getCacheEntryListener();

                if (listener instanceof CacheEntryUpdatedListener)
                    {
                    try
                        {
                        ((CacheEntryUpdatedListener) listener).onUpdated(iterable);
                        }
                    catch (Throwable e)
                        {
                        logListenerException(listener, e, "CacheEntryUpdatedListener");
                        }
                    }
                }
            }

        // notify remove listeners
        events = m_mapEvt.get(CacheEntryRemovedListener.class);

        if (events != null)
            {
            for (CoherenceCacheEntryListenerRegistration<? super K, ? super V> registration : registrations)
                {
                CacheEntryEventFilter<? super K, ? super V> filter = registration.getCacheEntryFilter();
                Iterable<CacheEntryEvent<K, V>> iterable = filter == null
                                                           ? events
                                                           : new CoherenceCacheEntryEventFilteringIterable<K,
                                                               V>(events, filter);

                CacheEntryListener<? super K, ? super V> listener = registration.getCacheEntryListener();

                if (listener instanceof CacheEntryRemovedListener)
                    {
                    try
                        {
                        ((CacheEntryRemovedListener) listener).onRemoved(iterable);
                        }
                    catch (Throwable e)
                        {
                        logListenerException(listener, e, "CacheEntryRemovedListener");
                        }
                    }
                }
            }
        }

    // ----- helper ---------------------------------------------------------

    /**
     * Log handled exception running registered CacheEntryListener for CacheEntryEvent
     * @param listener  throwing unexpected exception
     * @param e handled exception
     * @param sDescription what CacheEntryEvent listener type was being dispatched when exception was handled.
     */
    private void logListenerException(CacheEntryListener listener, Throwable e, String sDescription)
        {
        Logger.warn("handled unexpected exception in registered " + sDescription + ": "
                    + listener.getClass().getCanonicalName(), e);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The map of {@link javax.cache.event.CacheEntryEvent}s to deliver, keyed by the class of
     * {@link javax.cache.event.CacheEntryListener} to which they should be dispatched.
     */
    private ConcurrentHashMap<Class<? extends CacheEntryListener>, ArrayList<CacheEntryEvent<K, V>>> m_mapEvt;
    }
