/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import com.tangosol.net.cache.CacheMap;
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
    default long append(E e)
        {
        return append(e, EXPIRY_DEFAULT);
        }

    /**
     * Insert an element to the tail this {@link NamedQueue}.
     *
     * @param e        the element to insert
     * @param cMillis  the number of milliseconds until the queue entry will
     *                 expire, also referred to as the entry's "time to live";
     *                 pass {@link #EXPIRY_DEFAULT} to use the queue's default
     *                 time-to-live setting; pass {@link #EXPIRY_NEVER} to
     *                 indicate that the queue entry should never expire; this
     *                 milliseconds value is <b>not</b> a date/time value, such
     *                 as is returned from System.currentTimeMillis()
     *
     * @return the identifier for the inserted element, or {@link Long#MIN_VALUE} if the element could not be inserted
     */
    boolean offer(E e, long cMillis);

    /**
     * Insert an element to the tail this {@link NamedQueue}.
     *
     * @param e        the element to insert
     * @param cMillis  the number of milliseconds until the queue entry will
     *                 expire, also referred to as the entry's "time to live";
     *                 pass {@link #EXPIRY_DEFAULT} to use the queue's default
     *                 time-to-live setting; pass {@link #EXPIRY_NEVER} to
     *                 indicate that the queue entry should never expire; this
     *                 milliseconds value is <b>not</b> a date/time value, such
     *                 as is returned from System.currentTimeMillis()
     *
     * @return the identifier for the inserted element, or {@link Long#MIN_VALUE} if the element could not be inserted
     */
    boolean add(E e, long cMillis);

    /**
     * Insert an element to the tail this {@link NamedQueue}.
     *
     * @param e        the element to insert
     * @param cMillis  the number of milliseconds until the queue entry will
     *                 expire, also referred to as the entry's "time to live";
     *                 pass {@link #EXPIRY_DEFAULT} to use the queue's default
     *                 time-to-live setting; pass {@link #EXPIRY_NEVER} to
     *                 indicate that the queue entry should never expire; this
     *                 milliseconds value is <b>not</b> a date/time value, such
     *                 as is returned from System.currentTimeMillis()
     *
     * @return the identifier for the inserted element, or {@link Long#MIN_VALUE} if the element could not be inserted
     */
    long append(E e, long cMillis);

    /**
     * A special time-to-live value that can be passed to indicate that the
     * queue default expiry should be used.
     */
    long EXPIRY_DEFAULT = CacheMap.EXPIRY_DEFAULT;

    /**
     * A special time-to-live value that can be passed to indicate that the
     * queue entry should never expire.
     */
    long EXPIRY_NEVER   = CacheMap.EXPIRY_NEVER;
    }
