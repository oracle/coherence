/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.net.NamedMap;
import com.tangosol.net.NamedQueue;

/**
 * A {@link NamedQueue} implementation that wraps a {@link NamedMap}.
 *
 * @param <E> the type of elements held in this queue
 */
public interface NamedMapQueue<K, E>
        extends NamedQueue<E>
    {
    /**
     * Return the {@link NamedMap} containing the queue content.
     *
     * @return the {@link NamedMap} containing the queue content
     */
    public NamedMap<K, E> getNamedMap();

    public K createKey(long id);

    // ----- constants ------------------------------------------------------

    /**
     * The maximum size for a queue.
     * <p>
     * A queue is stored in a single partition and partition size is limited to 2GB per cache
     * (or max int value of 2147483647 bytes).
     * This max size is about 147MB under 2GB.
     */
    public static final long MAX_QUEUE_SIZE = 2000000000;
    }
