/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import java.util.Deque;

/**
 * A {@link Deque} based data-structure that manages values across one or
 * more processes. Values are typically managed in memory.
 *
 * @param <E> the type of values in the deque
 */
public interface NamedDeque<E>
        extends NamedQueue<E>, Deque<E>
    {
    @Override
    QueueService getService();

    /**
     * Insert an element to head of this {@link NamedDeque}.
     *
     * @param e  the element to insert
     *
     * @return the identifier for the inserted element, or {@link Long#MIN_VALUE} if the element could not be inserted
     */
    default long prepend(E e)
        {
        return prepend(e, EXPIRY_DEFAULT);
        }

    /**
     * Insert an element to head of this {@link NamedDeque}.
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
    long prepend(E e, long cMillis);

    /**
     * Inserts the specified element at the front of this deque if it is
     * possible to do so immediately without violating capacity restrictions,
     * throwing an {@code IllegalStateException} if no space is currently
     * available.  When using a capacity-restricted deque, it is generally
     * preferable to use method {@link #offerFirst}.
     *
     * @param e        the element to add
     * @param cMillis  the number of milliseconds until the queue entry will
     *                 expire, also referred to as the entry's "time to live";
     *                 pass {@link #EXPIRY_DEFAULT} to use the queue's default
     *                 time-to-live setting; pass {@link #EXPIRY_NEVER} to
     *                 indicate that the queue entry should never expire; this
     *                 milliseconds value is <b>not</b> a date/time value, such
     *                 as is returned from System.currentTimeMillis()
     *
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    void addFirst(E e, long cMillis);

    /**
     * Inserts the specified element at the end of this deque if it is
     * possible to do so immediately without violating capacity restrictions,
     * throwing an {@code IllegalStateException} if no space is currently
     * available.  When using a capacity-restricted deque, it is generally
     * preferable to use method {@link #offerLast}.
     *
     * <p>This method is equivalent to {@link #add}.
     *
     * @param e        the element to add
     * @param cMillis  the number of milliseconds until the queue entry will
     *                 expire, also referred to as the entry's "time to live";
     *                 pass {@link #EXPIRY_DEFAULT} to use the queue's default
     *                 time-to-live setting; pass {@link #EXPIRY_NEVER} to
     *                 indicate that the queue entry should never expire; this
     *                 milliseconds value is <b>not</b> a date/time value, such
     *                 as is returned from System.currentTimeMillis()
     *
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    void addLast(E e, long cMillis);

    /**
     * Inserts the specified element at the front of this deque unless it would
     * violate capacity restrictions.  When using a capacity-restricted deque,
     * this method is generally preferable to the {@link #addFirst} method,
     * which can fail to insert an element only by throwing an exception.
     *
     * @param e        the element to add
     * @param cMillis  the number of milliseconds until the queue entry will
     *                 expire, also referred to as the entry's "time to live";
     *                 pass {@link #EXPIRY_DEFAULT} to use the queue's default
     *                 time-to-live setting; pass {@link #EXPIRY_NEVER} to
     *                 indicate that the queue entry should never expire; this
     *                 milliseconds value is <b>not</b> a date/time value, such
     *                 as is returned from System.currentTimeMillis()
     *
     * @return {@code true} if the element was added to this deque, else
     *         {@code false}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    boolean offerFirst(E e, long cMillis);

    /**
     * Inserts the specified element at the end of this deque unless it would
     * violate capacity restrictions.  When using a capacity-restricted deque,
     * this method is generally preferable to the {@link #addLast} method,
     * which can fail to insert an element only by throwing an exception.
     *
     * @param e        the element to add
     * @param cMillis  the number of milliseconds until the queue entry will
     *                 expire, also referred to as the entry's "time to live";
     *                 pass {@link #EXPIRY_DEFAULT} to use the queue's default
     *                 time-to-live setting; pass {@link #EXPIRY_NEVER} to
     *                 indicate that the queue entry should never expire; this
     *                 milliseconds value is <b>not</b> a date/time value, such
     *                 as is returned from System.currentTimeMillis()
     *
     * @return {@code true} if the element was added to this deque, else
     *         {@code false}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    boolean offerLast(E e, long cMillis);

    /**
     * Pushes an element onto the stack represented by this deque (in other
     * words, at the head of this deque) if it is possible to do so
     * immediately without violating capacity restrictions, throwing an
     * {@code IllegalStateException} if no space is currently available.
     *
     * <p>This method is equivalent to {@link #addFirst}.
     *
     * @param e        the element to push
     * @param cMillis  the number of milliseconds until the queue entry will
     *                 expire, also referred to as the entry's "time to live";
     *                 pass {@link #EXPIRY_DEFAULT} to use the queue's default
     *                 time-to-live setting; pass {@link #EXPIRY_NEVER} to
     *                 indicate that the queue entry should never expire; this
     *                 milliseconds value is <b>not</b> a date/time value, such
     *                 as is returned from System.currentTimeMillis()
     *
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    void push(E e, long cMillis);
    }
