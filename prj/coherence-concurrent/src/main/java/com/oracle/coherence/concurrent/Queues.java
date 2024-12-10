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

import com.tangosol.internal.net.NamedCacheDeactivationListener;
import com.tangosol.internal.net.queue.NamedMapBlockingDeque;
import com.tangosol.internal.net.queue.NamedMapBlockingQueue;
import com.tangosol.internal.net.queue.NamedMapDeque;

import com.tangosol.internal.net.queue.NamedMapQueue;
import com.tangosol.internal.net.queue.PagedQueue;
import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.internal.net.queue.paged.PagedNamedQueue;
import com.tangosol.io.ClassLoaderAware;

import com.tangosol.net.Coherence;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedBlockingDeque;
import com.tangosol.net.NamedBlockingQueue;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.NamedMap;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.Session;
import com.tangosol.net.ValueTypeAssertion;

import com.tangosol.net.internal.ScopedReferenceStore;
import com.tangosol.util.MapEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A factory to obtain various types of {@link NamedBlockingQueue}
 * and {@link NamedMapBlockingDeque}.
 */
@SuppressWarnings({"rawtypes", "unchecked", "resource"})
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
    public static <E> NamedBlockingDeque<E> deque(String sName)
        {
        return deque(sName, session());
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
    public static <E> NamedBlockingDeque<E> deque(String sName, Session session)
        {
        if (session == null)
            {
            session = session();
            }
        NamedQueueScheme           scheme     = SimpleDequeScheme.INSTANCE;
        String                     sCacheName = isConcurrent(session) ? cacheNameForDeque(sName) : sName;
        NamedMapDeque<QueueKey, E> deque      = ensureCollectionInternal(sCacheName, NamedMapDeque.class, scheme, session);
        return new NamedMapBlockingDeque<>(sName, deque);
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
    public static <E> NamedBlockingQueue<E> queue(String sName)
        {
        return queue(sName, session());
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
    public static <E> NamedBlockingQueue<E> queue(String sName, Session session)
        {
        return deque(sName, session);
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
    public static <E> NamedBlockingQueue<E> pagedQueue(String sName)
        {
        return pagedQueue(sName, session());
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
    public static <E> NamedBlockingQueue<E> pagedQueue(String sName, Session session)
        {
        if (session == null)
            {
            session = session();
            }
        PagedQueueScheme   scheme     = PagedQueueScheme.INSTANCE;
        String             sCacheName = isConcurrent(session) ? cacheNameForPagedQueue(sName) : sName;
        PagedNamedQueue<E> queue      = (PagedNamedQueue<E>) ensureCollectionInternal(sCacheName, NamedMapQueue.class, scheme, session);
        return new NamedMapBlockingQueue<>(sName, queue);
        }

    /**
     * Return the name of the cache used to hold queue content fpr a given queue name.
     *
     * @param sName  the name of the queue
     *
     * @return the name of the cache used to hold queue content fpr a given queue name
     */
    public static String cacheNameForQueue(String sName)
        {
        return QUEUE_CACHE_PREFIX + sName;
        }

    /**
     * Return the name of the cache used to hold queue content fpr a given deque name.
     *
     * @param sName  the name of the queue
     *
     * @return the name of the cache used to hold queue content fpr a given deque name
     */
    public static String cacheNameForDeque(String sName)
        {
        return QUEUE_CACHE_PREFIX + sName;
        }

    /**
     * Return the name of the cache used to hold queue content fpr a given paged queue name.
     *
     * @param sName  the name of the queue
     *
     * @return the name of the cache used to hold queue content fpr a given paged queue name
     */
    public static String cacheNameForPagedQueue(String sName)
        {
        return PAGED_QUEUE_CACHE_PREFIX + sName;
        }
    
    // ----- helper methods -------------------------------------------------

    /**
     * Return Coherence {@link Session} for the concurrent module.
     *
     * @return Coherence {@link Session} for the concurrent module
     */
    protected static Session session()
        {
        return Coherence.findSession(SESSION_NAME).orElseThrow(() ->
                new IllegalStateException(String.format("The session '%s' has not been initialized", SESSION_NAME)));
        }

    // ----- helper methods -------------------------------------------------

    private static boolean isConcurrent(Session session)
        {
        return SESSION_NAME.equals(session.getName());
        }

    private static <Q extends NamedMapQueue> Q ensureCollectionInternal(String sName, Class<Q> clzQueue,
                NamedQueueScheme<Q> scheme, Session session)
        {
        Q collection;

        ConfigurableCacheFactorySession ccfSession = (ConfigurableCacheFactorySession) session;

        if (sName == null || sName.isEmpty())
            {
            sName = "Default";
            }

        ExtensibleConfigurableCacheFactory eccf     = (ExtensibleConfigurableCacheFactory) ccfSession.getConfigurableCacheFactory();
        ClassLoader                        loader   = eccf.getConfigClassLoader();
        String                             sSession = session.getName();
        QueueReferenceStore                store    = f_mapStores.computeIfAbsent(sSession, k -> new QueueReferenceStore());

        do
            {
            NamedQueue col = store.get(sName, loader);

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
            store.clearInactiveRefs(sName);

            if (scheme.realizes(clzQueue))
                {
                ParameterResolver resolver = new NullParameterResolver();
                MapBuilder.Dependencies dependencies = new MapBuilder.Dependencies(ccfSession.getConfigurableCacheFactory(), null,
                                                  loader, sName,null);

                collection = (Q) scheme.realize(ValueTypeAssertion.WITHOUT_TYPE_CHECKING, resolver, dependencies);
                NamedMap map = collection.getNamedMap();
                map.addMapListener(new DeactivationListener(collection, store));
                }
            else
                {
                throw new IllegalArgumentException("The specified builder cannot build a queue of type " + clzQueue);
                }

            } while (store.putIfAbsent(collection, loader) != null);

        return collection;
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

    // ----- DeactivationListener -------------------------------------------

    protected static class DeactivationListener
            implements NamedCacheDeactivationListener
        {
        public DeactivationListener(NamedQueue<?> queue, QueueReferenceStore store)
            {
            m_queue = queue;
            m_store = store;
            }

        @Override
        public void entryInserted(MapEvent evt)
            {
            }

        @Override
        public void entryUpdated(MapEvent evt)
            {
            }

        @Override
        public void entryDeleted(MapEvent evt)
            {
            m_store.release(m_queue);
            }

        private final NamedQueue<?> m_queue;

        private final QueueReferenceStore m_store;
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
     * The map of queue reference stores keyed by session name.
     */
    private static final Map<String, QueueReferenceStore> f_mapStores = new ConcurrentHashMap<>();
    }
