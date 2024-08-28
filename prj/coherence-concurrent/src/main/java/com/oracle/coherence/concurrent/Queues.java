/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent;

import com.oracle.coherence.common.base.Classes;

import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;

import com.tangosol.coherence.config.builder.MapBuilder;

import com.tangosol.coherence.config.scheme.NamedQueueScheme;
import com.tangosol.coherence.config.scheme.PagedQueueScheme;
import com.tangosol.coherence.config.scheme.SimpleDequeScheme;

import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;

import com.tangosol.internal.net.queue.NamedCacheBlockingDeque;
import com.tangosol.internal.net.queue.NamedCacheBlockingQueue;
import com.tangosol.internal.net.queue.NamedCacheDeque;

import com.tangosol.internal.net.queue.paged.PagedNamedQueue;
import com.tangosol.io.ClassLoaderAware;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedBlockingDeque;
import com.tangosol.net.NamedBlockingQueue;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.Session;
import com.tangosol.net.ValueTypeAssertion;

import com.tangosol.net.internal.ScopedReferenceStore;

/**
 * A factory to obtain various types of {@link NamedBlockingQueue}
 * and {@link NamedCacheBlockingDeque}.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Queues
    {
    /**
     * Return a {@link NamedBlockingDeque}.
     * <p/>
     * The queue implementation returned by this method has a
     * maximum size of 2GB. If the queue reaches capacity it
     * will not accept further elements until some have been
     * removed by one of the poll methods.
     *
     * @param sName  the name of the queue to return
     * @param <E>    the type of element in the queue
     *
     * @return a {@link NamedBlockingDeque} with the specified name
     */
    public static <E> NamedBlockingDeque<E> blockingDeque(String sName)
        {
        return blockingDeque(sName, session());
        }

    /**
     * Return a {@link NamedBlockingDeque}.
     * <p/>
     * The queue implementation returned by this method has a
     * maximum size of 2GB. If the queue reaches capacity it
     * will not accept further elements until some have been
     * removed by one of the poll methods.
     *
     * @param sName    the name of the queue to return
     * @param session  the {@link Session} to use to obtain the underlying cache
     * @param <E>      the type of element in the queue
     *
     * @return a {@link NamedBlockingDeque} with the specified name
     */
    public static <E> NamedBlockingDeque<E> blockingDeque(String sName, Session session)
        {
        SimpleDequeScheme  scheme     = new SimpleDequeScheme();
        String             sCacheName = QUEUE_CACHE_PREFIX + sName;
        NamedCacheDeque<E> deque      = ensureCollectionInternal(sCacheName, NamedCacheDeque.class, scheme, session);
        return new NamedCacheBlockingDeque<>(sName, deque);
        }

    /**
     * Return a {@link NamedBlockingQueue}.
     * <p/>
     * The queue implementation returned by this method has a
     * maximum size of 2GB. If the queue reaches capacity it
     * will not accept further elements until some have been
     * removed by one of the poll methods.
     *
     * @param sName  the name of the queue to return
     * @param <E>    the type of element in the queue
     *
     * @return a {@link NamedBlockingQueue} with the specified name
     */
    public static <E> NamedBlockingQueue<E> blocking(String sName)
        {
        return blocking(sName, session());
        }

    /**
     * Return a {@link NamedBlockingQueue}.
     * <p/>
     * The queue implementation returned by this method has a
     * maximum size of 2GB. If the queue reaches capacity it
     * will not accept further elements until some have been
     * removed by one of the poll methods.
     *
     * @param sName    the name of the queue to return
     * @param session  the {@link Session} to use to obtain the underlying cache
     * @param <E>      the type of element in the queue
     *
     * @return a {@link NamedBlockingQueue} with the specified name
     */
    public static <E> NamedBlockingQueue<E> blocking(String sName, Session session)
        {
        return blockingDeque(sName, session);
        }

    /**
     * Return a paged {@link NamedBlockingQueue}.
     * <p/>
     * The queue implementation returned by this method has a
     * maximum size of 2GB. If the queue reaches capacity it
     * will not accept further elements until some have been
     * removed by one of the poll methods.
     *
     * @param sName  the name of the queue to return
     * @param <E>    the type of element in the queue
     *
     * @return a {@link NamedBlockingQueue} with the specified name
     */
    public static <E> NamedBlockingQueue<E> pagedBlockingQueue(String sName)
        {
        return pagedBlockingQueue(sName, session());
        }

    /**
     * Return a paged {@link NamedBlockingQueue}.
     * <p/>
     * The implementation of the queue returned distributes the queue elements over
     * the cluster in pages. The size of the queue is limited to the amount of data
     * that can be stored in the underlying caches.
     * <p/>
     * As a guide to the maximum size, this would be the partition count multiplied by
     * the recommended maximum size of a cache partition. A cache partition should
     * never exceed 2GB in total, but in practice should be a lot less than this to
     * all for fast fail-over. If the recommended max partition size is around a 100MB,
     * then for the default partition count of 257, this gives a queue size of around
     * 25GB and for a partition count of 1087, this gives a queue size of around 108GB.
     *
     * @param sName    the name of the queue to return
     * @param session  the {@link Session} to use to obtain the underlying cache
     * @param <E>      the type of element in the queue
     *
     * @return a {@link NamedBlockingQueue} with the specified name
     */
    public static <E> NamedBlockingQueue<E> pagedBlockingQueue(String sName, Session session)
        {
        PagedQueueScheme   scheme     = new PagedQueueScheme();
        String             sCacheName = PAGED_QUEUE_CACHE_PREFIX + sName;
        PagedNamedQueue<E> queue      = ensureCollectionInternal(sCacheName, PagedNamedQueue.class, scheme, session);
        return new NamedCacheBlockingQueue<>(sName, queue);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return Coherence {@link Session} for the Semaphore module.
     *
     * @return Coherence {@link Session} for the Semaphore module
     */
    protected static Session session()
        {
        return Coherence.findSession(SESSION_NAME).orElseThrow(() ->
                new IllegalStateException(String.format("The session '%s' has not been initialized", SESSION_NAME)));
        }

    // ----- helper methods -------------------------------------------------

    private static <Q extends NamedQueue> Q ensureCollectionInternal(String sName, Class<Q> clzQueue,
            NamedQueueScheme<Q> scheme, Session session)
        {
        Q collection;

        ConfigurableCacheFactorySession ccfSession = (ConfigurableCacheFactorySession) session;

        if (sName == null || sName.isEmpty())
            {
            sName = "Default";
            }

        ClassLoader loader = Classes.getContextClassLoader();

        do
            {
            NamedQueue col = f_storeQueues.get(sName, loader);

            if (col != null && col.isActive())
                {
                if (clzQueue.isAssignableFrom(col.getClass()))
                    {
                    return (Q) col;
                    }
                else
                    {
                    String sMsg = String.format(
                            "A Collection already exist for name '%s' but is of type %s when requested type is %s",
                            sName, col.getClass(), clzQueue);

                    throw new IllegalStateException(sMsg);
                    }
                }

            // there are instances of sibling caches for different
            // class loaders; check for and clear invalid references
            f_storeQueues.clearInactiveRefs(sName);

            if (scheme.realizes(clzQueue))
                {
                ParameterResolver resolver = new NullParameterResolver();
                MapBuilder.Dependencies dependencies = new MapBuilder.Dependencies(ccfSession.getConfigurableCacheFactory(), null,
                                                  loader, sName,null);

                collection = (Q) scheme.realize(ValueTypeAssertion.WITHOUT_TYPE_CHECKING, resolver, dependencies);
                }
            else
                {
                throw new IllegalArgumentException("The specified builder cannot build a queue of type " + clzQueue);
                }

            } while (f_storeQueues.putIfAbsent(collection, loader) != null);

        return collection;
        }

    protected void releaseQueue(NamedQueue<?> queue)
        {
        releaseCollection(queue, false);
        }

    protected void destroyQueue(NamedQueue<?> queue)
        {
        releaseCollection(queue, true);
        }

    /**
     * Release a {@link NamedCollection} managed by this factory, optionally destroying it.
     *
     * @param collection  the collection to release
     * @param fDestroy     true to destroy the collection as well
     */
    private static void releaseCollection(NamedQueue<?> collection, boolean fDestroy)
        {
        String      sName   = collection.getName();
        ClassLoader loader  = collection instanceof ClassLoaderAware
                ? ((ClassLoaderAware) collection).getContextClassLoader()
                : Classes.getContextClassLoader();

        if (f_storeQueues.release(collection, loader)) // free the resources
            {
            // allow collection to release/destroy internal resources
            if (fDestroy)
                {
                collection.destroy();
                }
            else
                {
                collection.release();
                }
            }
        else if (collection.isActive())
            {
            // active, but not managed by this factory
            throw new IllegalArgumentException("The collection " + sName
                + " was created using a different factory; that same"
                + " factory should be used to release the collection.");
            }
        }

    // ----- inner class QueueReferenceStore --------------------------------

    @SuppressWarnings("rawtypes")
    protected static class QueueReferenceStore
            extends ScopedReferenceStore<NamedQueue>
        {
        public QueueReferenceStore()
            {
            super(NamedQueue.class, NamedQueue::isActive, NamedQueue::getName, NamedQueue::getService);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The prefix use for simple queue cache names.
     */
    public static final String QUEUE_CACHE_PREFIX = "Queue$";

    /**
     * The prefix use for paged queue cache names.
     */
    public static final String PAGED_QUEUE_CACHE_PREFIX = "PagedQueue$";

    /**
     * The session name.
     */
    public static final String SESSION_NAME = ConcurrentServicesSessionConfiguration.SESSION_NAME;

    // ----- data members ---------------------------------------------------

    /**
     * Store of queue references with subject scoping if configured.
     */
    private static final QueueReferenceStore f_storeQueues = new QueueReferenceStore();
    }
