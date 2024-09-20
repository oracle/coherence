/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import com.tangosol.net.queue.QueueStatistics;
import com.tangosol.util.ObservableCollection;

import java.util.Queue;

/**
 * A {@link Queue} based data-structure that manages values across one or more processes.
 * Values are typically managed in memory.
 *
 * @param <E> the type of values in the queue
 */
public interface NamedQueue<E>
        extends NamedCollection, ObservableCollection<E>, Queue<E>
    {
    @Override
    QueueService getService();

    /**
     * Returns whether this {@code NamedMap} is ready to be used.
     * </p>
     * An example of when this method would return {@code false} would
     * be where a partitioned cache service that owns this cache has no
     * storage-enabled members.
     *
     * @return return {@code true} if the {@code NamedMap} may be used
     *         otherwise returns {@code false}.
     *
     * @since 14.1.1.2206.5
     */
    default boolean isReady()
        {
        return isActive();
        }

    /**
     * Returns {@code true} if this map is not released or destroyed.
     * In other words, calling {@code isActive()} is equivalent to calling
     * {@code !cache.isReleased() && !cache.isDestroyed()}.
     *
     * @return {@code true} if the cache is active, otherwise {@code false}
     */
    boolean isActive();

    /**
     * Returns the current statistics for this queue.
     *
     * @return the current statistics for this queue
     */
    QueueStatistics getQueueStatistics();

    /**
     * Return the consistent hash of the queue name.
     *
     * @return the consistent hash of the queue name
     */
    int getQueueNameHash();

    /**
     * Insert an element to the tail this {@link NamedQueue}.
     *
     * @param e  the element to insert
     *
     * @return the identifier for the inserted element, or {@link Long#MIN_VALUE} if the element could not be inserted
     */
    long append(E e);
    }
