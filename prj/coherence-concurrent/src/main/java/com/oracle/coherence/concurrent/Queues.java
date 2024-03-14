/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent;

import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;

import com.tangosol.internal.net.queue.NamedCacheBlockingDeque;
import com.tangosol.internal.net.queue.NamedCacheDequeBuilder;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedBlockingDeque;
import com.tangosol.net.NamedBlockingQueue;
import com.tangosol.net.NamedDeque;
import com.tangosol.net.Session;

/**
 * A factory to obtain named blocking queues.
 */
@SuppressWarnings("resource")
public class Queues
    {
    /**
     * Return a {@link NamedBlockingDeque}.
     *
     * @param sName  the name of the queue to return
     * @param <E>    the type of element in the queue
     *
     * @return a {@link NamedBlockingDeque} with the specified name
     */
    public static <E> NamedBlockingDeque<E> blockingDeque(String sName)
        {
        return ensureQueue(sName);
        }

    /**
     * Return a {@link NamedBlockingQueue}.
     *
     * @param sName  the name of the queue to return
     * @param <E>    the type of element in the queue
     *
     * @return a {@link NamedBlockingQueue} with the specified name
     */
    public static <E> NamedBlockingQueue<E> blocking(String sName)
        {
        return blockingDeque(sName);
        }

    // ----- helper methods -------------------------------------------------


    /**
     * Return Coherence {@link Session} for the Semaphore module.
     *
     * @return Coherence {@link Session} for the Semaphore module
     */
    protected static Session session()
        {
        return Coherence.findSession(SESSION_NAME)
                        .orElseThrow(() ->
                                new IllegalStateException(String.format("The session '%s' has not been initialized", SESSION_NAME)));
        }

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings("unchecked")
    protected static <E> NamedBlockingDeque<E> ensureQueue(String sName)
        {
        if (sName == null || sName.isEmpty())
            {
            sName = "Default";
            }

        Session session = session();
        NamedDeque<Object> deque = session.getDeque(sName, BUILDER);
        return (NamedBlockingDeque<E>) deque;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The prefix use for queue cache names.
     */
    public static final String QUEUE_CACHE_PREFIX = "Queue$";

    /**
     * The session name.
     */
    public static final String SESSION_NAME = ConcurrentServicesSessionConfiguration.SESSION_NAME;

    /**
     * The {@link NamedCacheDequeBuilder} used to ensure a blocking queue is created from
     * the concurrent session with the correct name suffix.
     */
    public static final NamedCacheDequeBuilder BUILDER = new NamedCacheBlockingDeque.Builder(QUEUE_CACHE_PREFIX);
    }
