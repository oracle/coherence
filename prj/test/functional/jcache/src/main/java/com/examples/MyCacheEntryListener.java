/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.examples;

import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;

public class MyCacheEntryListener<K, V>
        implements CacheEntryCreatedListener<K, V>
{

    public void onCreated(Iterable<CacheEntryEvent<? extends K, ? extends V>>
                                  events)
            throws CacheEntryListenerException
    {
        for (CacheEntryEvent<? extends K, ? extends V> event : events)
        {
            System.out.println("Received a " + event);
        }
    }
}