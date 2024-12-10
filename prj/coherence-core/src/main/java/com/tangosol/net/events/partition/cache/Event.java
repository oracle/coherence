/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.partition.cache;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;

/**
 * An event dispatched by a {@link PartitionedCacheDispatcher}.
 *
 * @param <T>  the type of event
 *
 * @author rhl/hr/gg  2012.09.21
 * @since Coherence 12.1.2
 */
public interface Event<T extends Enum<T>>
            extends com.tangosol.net.events.Event<T>
    {
    /**
     * Return the {@link PartitionedCacheDispatcher} this event was
     * raised by.
     *
     * @return the {@code PartitionedCacheDispatcher} this event was raised by
     */
    @Override
    public PartitionedCacheDispatcher getDispatcher();

    /**
     * Return the name of the cache this event was raised from.
     *
     * @return the name of the cache this event was raised from
     */
    public default String getCacheName()
        {
        return getBackingMapContext().getCacheName();
        }

    /**
     * Return the {@link CacheService} this event was raised from.
     *
     * @return the {@code CacheService} this event was raised from
     */
    public default CacheService getService()
        {
        return getManagerContext().getCacheService();
        }

    /**
     * Return the {@link BackingMapManagerContext} this event was raised from.
     *
     * @return the {@code BackingMapManagerContext} this event was raised from
     */
    public default BackingMapManagerContext getManagerContext()
        {
        return getBackingMapContext().getManagerContext();
        }

    /**
     * Return the {@link BackingMapContext} this event was raised from.
     *
     * @return the {@code BackingMapContext} this event was raised from
     */
    public default BackingMapContext getBackingMapContext()
        {
        return getDispatcher().getBackingMapContext();
        }
    }
