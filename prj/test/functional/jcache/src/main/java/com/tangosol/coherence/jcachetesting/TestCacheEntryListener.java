/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import org.junit.Assert;

import java.util.ArrayList;

import java.util.concurrent.atomic.AtomicInteger;

import javax.cache.event.*;

/**
 * Class description
 *
 * @param <K>
 * @param <V>
 *
 * @version        Enter version here..., 13/04/29
 * @author         Enter your name here...
 */
public class TestCacheEntryListener<K, V>
        implements CacheEntryCreatedListener<K, V>, CacheEntryUpdatedListener<K, V>, CacheEntryExpiredListener<K, V>,
                   CacheEntryRemovedListener<K, V>
    {
    /**
     * Constructs ...
     *
     */
    public TestCacheEntryListener()
        {
        this(false);
        }

    /**
     * Constructs ...
     *
     *
     * @param fVerbose
     */
    public TestCacheEntryListener(boolean fVerbose)
        {
        this.fVerbose = fVerbose;
        }

    /**
     * Number of created events
     *
     * @return number of created events
     */
    public int getCreated()
        {
        return created.get();
        }

    /**
     * Number of updated events
     *
     * @return number of updated events
     */
    public int getUpdated()
        {
        return updated.get();
        }

    /**
     * Number of remove events
     *
     * @return number of remove events
     */
    public int getRemoved()
        {
        return removed.get();
        }

    /**
     * number of expired events
     *
     * @return number of expired events
     */
    public int getExpired()
        {
        return expired.get();
        }

    /**
     * Get entries
     *
     * @return entries the entries
     */
    public ArrayList<CacheEntryEvent<K, V>> getEntries()
        {
        return entries;
        }

    /**
     * Method description
     *
     * @param events created events
     * @throws javax.cache.event.CacheEntryListenerException
     *
     */
    @Override
    public void onCreated(Iterable<CacheEntryEvent<? extends K, ? extends V>> events)
            throws CacheEntryListenerException
        {
        for (CacheEntryEvent<? extends K, ? extends V> event : events)
            {
            created.incrementAndGet();
            verboseOut("onCreated", event);
            Assert.assertFalse(event.isOldValueAvailable());
            }
        }

    /**
     * expired event dispatcher
     *
     * @param events events to dispatch
     * @throws javax.cache.event.CacheEntryListenerException
     *
     */
    @Override
    public void onExpired(Iterable<CacheEntryEvent<? extends K, ? extends V>> events)
            throws CacheEntryListenerException
        {
        for (CacheEntryEvent<? extends K, ? extends V> event : events)
            {
            Assert.assertNotNull(event.getKey());
            verboseOut("onExpired", event);

            expired.incrementAndGet();
            }
        }

    /**
     * removed event dispatcher
     *
     * @param events removed events
     * @throws javax.cache.event.CacheEntryListenerException
     *
     */
    @Override
    public void onRemoved(Iterable<CacheEntryEvent<? extends K, ? extends V>> events)
            throws CacheEntryListenerException
        {
        for (CacheEntryEvent<? extends K, ? extends V> event : events)
            {
            Assert.assertNotNull(event.getKey());
            verboseOut("onRemoved", event);

            removed.incrementAndGet();
            }
        }

    /**
     * updated event dispatcher
     *
     * @param events updated events to dispatch listeners to.
     * @throws javax.cache.event.CacheEntryListenerException
     *
     */
    @Override
    public void onUpdated(Iterable<CacheEntryEvent<? extends K, ? extends V>> events)
            throws CacheEntryListenerException
        {
        for (CacheEntryEvent<? extends K, ? extends V> event : events)
            {
            Assert.assertTrue(event.isOldValueAvailable());
            verboseOut("onUpdated", event);
            updated.incrementAndGet();
            }
        }

    // ----- helpers --------------------------------------------------------

    private void verboseOut(String eventType, CacheEntryEvent event)
    {
        if (fVerbose) {
            System.out.println("EventType: " + eventType + "  "  + event);
        }
    }


    // ----- data members -----------------------------------------------
    final AtomicInteger                    created = new AtomicInteger();
    final AtomicInteger                    updated = new AtomicInteger();
    final AtomicInteger                    removed = new AtomicInteger();
    final AtomicInteger                    expired = new AtomicInteger();
    final ArrayList<CacheEntryEvent<K, V>> entries = new ArrayList<CacheEntryEvent<K, V>>();
    final boolean                          fVerbose;
    }
