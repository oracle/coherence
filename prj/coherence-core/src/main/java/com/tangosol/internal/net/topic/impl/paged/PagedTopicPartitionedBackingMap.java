/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.net.BackingMapManager;

import com.tangosol.net.partition.ObservableSplittingBackingCache;

/**
 * A base class for partitioned backing map implementations
 * used by a paged topic caches.
 *
 * @author Jonathan Knight 2022.08.11
 */
public abstract class PagedTopicPartitionedBackingMap
        extends ObservableSplittingBackingCache
    {
    /**
     * Create a {@link PagedTopicPartitionedBackingMap}
     *
     * @param bmm    the {@link BackingMapManager} for the cache
     * @param sName  the name of the cache
     */
    protected PagedTopicPartitionedBackingMap(BackingMapManager bmm, String sName)
        {
        super(new PagedTopicCapacityAwareMap(bmm, sName));
        }

    @Override
    public PagedTopicCapacityAwareMap getMap()
        {
        return (PagedTopicCapacityAwareMap) super.getMap();
        }

    // ----- inner class: PagedTopicCapacityAwareMap ------------------------

    /**
     * A subclass of {@link com.tangosol.net.partition.ObservableSplittingBackingCache.CapacityAwareMap}
     * used by paged topic caches.
     */
    public static class PagedTopicCapacityAwareMap
            extends CapacityAwareMap
        {
        /**
         * Create a {@link PagedTopicCapacityAwareMap}.
         *
         * @param bmm    the {@link BackingMapManager} for the cache
         * @param sName  the name of the cache
         */
        protected PagedTopicCapacityAwareMap(BackingMapManager bmm, String sName)
            {
            super(bmm, sName);
            }

        @Override
        protected MapArray getMapArray()
            {
            return super.getMapArray();
            }
        }
    }
