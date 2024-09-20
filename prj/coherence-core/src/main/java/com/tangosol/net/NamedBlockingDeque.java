/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * A {@link BlockingDeque} based data-structure that manages values across one or
 * more processes. Values are typically managed in memory.
 *
 * @param <E> the type of values in the queue
 */
public interface NamedBlockingDeque<E>
        extends NamedBlockingQueue<E>, NamedDeque<E>, BlockingDeque<E>
    {
    /**
     * Inserts the specified element at the front of this {@link NamedBlockingDeque},
     * waiting up to the specified wait time if necessary for space to become available.
     *
     * @param e        the element to insert
     * @param timeout  how long to wait before giving up, in units of {@code unit}
     * @param unit     a {@code TimeUnit} determining how to interpret the
     *                 {@code timeout} parameter
     *
     * @return the identifier for the inserted element, or {@code -1L} if the element could not be inserted
     * @throws InterruptedException if interrupted while waiting
     */
    long prepend(E e, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Inserts the specified element at the front of this {@link NamedBlockingDeque},
     * waiting if necessary for space to become available.
     *
     * @param e  the element to insert
     *
     * @return the identifier for the inserted element, or {@code -1L} if the element could not be inserted
     * @throws InterruptedException if interrupted while waiting
     */
    long prependFirst(E e) throws InterruptedException;
    }
