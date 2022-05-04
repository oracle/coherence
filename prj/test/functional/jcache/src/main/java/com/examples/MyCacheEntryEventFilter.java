/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.examples;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.EventType;

/**
 * Example 36-2 An Example Event Filter Implementation
 * @param <K>
 * @param <V>
 */
public class MyCacheEntryEventFilter<K, V>
        implements CacheEntryEventFilter<K, V>
{

    public boolean evaluate(CacheEntryEvent<? extends K, ? extends V> event)
            throws CacheEntryListenerException
    {
        boolean result = true;

        if (event.getEventType() == EventType.CREATED)
        {
            if (((String)event.getKey()).contains("filterOut"))
            {
                result = false;
            }
            System.out.println("filter event=" + event + " filter result=" +
                    result);

        }

        return result;
    }
}