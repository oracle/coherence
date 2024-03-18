/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.internal.net.NamedMapValuesCollection;
import com.tangosol.internal.net.queue.extractor.QueueKeyExtractor;
import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.internal.net.queue.model.QueueOfferResult;
import com.tangosol.internal.net.queue.model.QueuePollResult;
import com.tangosol.internal.net.queue.processor.QueueOffer;
import com.tangosol.internal.net.queue.processor.QueuePeek;
import com.tangosol.internal.net.queue.processor.QueuePoll;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.QueueService;
import com.tangosol.net.queue.MutableQueueStatistics;
import com.tangosol.net.queue.QueueStatistics;
import com.tangosol.util.Binary;
import com.tangosol.util.CollectionListener;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.transformer.MapListenerCollectionListener;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A {@link NamedQueue} implementation that wraps a {@link NamedCache}.
 *
 * @param <E> the type of elements held in this queue
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class NamedCacheQueue<E>
        extends NamedMapValuesCollection<QueueKey, E>
        implements NamedQueue<E>
    {
    /**
     * Create a {@link NamedCacheQueue} that wrap s a {@link NamedCache}.
     *
     * @param sName    the name of the cache to wrap
     * @param cache    the {@link NamedCache} to wrap
     *
     * @throws NullPointerException      if either of the {@code name} or {@code cache} parameters is {@code null}
     * @throws IllegalArgumentException  if the name is blank
     */
    public NamedCacheQueue(String sName, NamedCache<QueueKey, E> cache)
        {
        super(Objects.requireNonNull(sName), Objects.requireNonNull(cache));
        m_service    = ensureQueueService(cache.getCacheService());
        m_statistics = new SimpleQueueStatistics();
        m_keyHead    = QueueKey.head(m_sName);
        m_keyTail    = QueueKey.tail(m_sName);
        m_cache.addIndex(QueueKeyExtractor.instance(), true, null);
        }

    // ----- NamedQueue methods ---------------------------------------------

    @Override
    public QueueService getService()
        {
        return m_service;
        }

    @Override
    public QueueStatistics getQueueStatistics()
        {
        return m_statistics;
        }

    @Override
    public int getQueueNameHash()
        {
        return m_keyHead.getHash();
        }

    @Override
    public void addListener(CollectionListener<? super E> listener)
        {
        MapListenerCollectionListener<QueueKey, E> wrapper = new MapListenerCollectionListener(listener);
        m_cache.addMapListener(wrapper);
        }

    @Override
    public void removeListener(CollectionListener<? super E> listener)
        {
        MapListenerCollectionListener<QueueKey, E> wrapper = new MapListenerCollectionListener(listener);
        m_cache.removeMapListener(wrapper);
        }

    @Override
    public void addListener(CollectionListener<? super E> listener, Filter<E> filter, boolean fLite)
        {
        MapListenerCollectionListener<QueueKey, E> wrapper = new MapListenerCollectionListener(listener);
        m_cache.addMapListener(wrapper, filter, fLite);
        }

    @Override
    public void removeListener(CollectionListener<? super E> listener, Filter<E> filter)
        {
        MapListenerCollectionListener<QueueKey, E> wrapper = new MapListenerCollectionListener(listener);
        m_cache.removeMapListener(wrapper, filter);
        }

    // ----- AbstractQueue methods ------------------------------------------

    @Override
    public boolean add(E e)
        {
        return offer(e);
        }

    @Override
    public boolean addAll(Collection<? extends E> c)
        {
        assertNotSameCollection(c, "This collection cannot be added to itself or the same underlying cache");
        for (E e : c)
            {
            if (!offer(e))
                {
                return false;
                }
            }
        return true;
        }

    @Override
    public E element()
        {
        QueuePollResult result = peekAtHeadInternal();
        E element = result.getElement();
        if (element != null)
            {
            m_statistics.registerHit();
            return element;
            }
        m_statistics.registerMiss();
        throw new NoSuchElementException();
        }

    @Override
    public boolean offer(E e)
        {
        assertNotNull(e);
        QueueOfferResult result = offerToTailInternal(e);
        boolean fSuccess = result.getResult() == QueueOfferResult.RESULT_SUCCESS;
        if (fSuccess)
            {
            m_statistics.registerAccepted();
            }
        else
            {
            m_statistics.registerRejected();
            }
        return fSuccess;
        }

    @Override
    public E peek()
        {
        QueuePollResult result = peekAtHeadInternal();
        E               oValue = result.getElement();
        if (oValue == null)
            {
            m_statistics.registerMiss();
            }
        else
            {
            m_statistics.registerHit();
            }
        return oValue;
        }

    @Override
    public E poll()
        {
        QueuePollResult result = pollFromHeadInternal();
        E               oValue = result.getElement();
        if (oValue == null)
            {
            m_statistics.registerMiss();
            }
        else
            {
            m_statistics.registerHit();
            }
        return oValue;
        }

    @Override
    public E remove()
        {
        QueuePollResult result  = pollFromHeadInternal();
        E               element = result.getElement();
        if (element != null)
            {
            m_statistics.registerHit();
            return element;
            }
        m_statistics.registerMiss();
        throw new NoSuchElementException();
        }

    @Override
    public Iterator<E> iterator()
        {
        return QueuePageIterator.head(m_keyHead.getHash(), m_cache);
        }

    @Override
    public Object[] toArray()
        {
        return toArrayInternal(null);
        }

    @Override
    public <T> T[] toArray(T[] ao)
        {
        return (T[]) toArrayInternal(ao);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder s = new StringBuilder()
                .append(getClass().getSimpleName())
                .append("{name=\"")
                .append(m_sName)
                .append("\"")
                .append(", metrics={");
        getQueueStatistics().logTo(s);
        s.append("}");
        return s.toString();
        }

    // ----- helper methods -------------------------------------------------

    protected static QueueService ensureQueueService(CacheService cacheService)
        {
        if (cacheService instanceof QueueService)
            {
            return (QueueService) cacheService;
            }
        return new CacheQueueService(cacheService);
        }

    protected QueueOfferResult offerToTailInternal(E e)
        {
        long             lStart    = System.nanoTime();
        QueueOffer<E>    processor = new QueueOffer<>(e);
        QueueOfferResult result    = m_cache.invoke(m_keyTail.randomTail(), processor);
        long             lEnd      = System.nanoTime();
        m_statistics.offered(lEnd - lStart);
        return result;
        }

    protected QueuePollResult pollFromHeadInternal()
        {
        long            lStart  = System.nanoTime();
        QueuePollResult result = m_cache.invoke(m_keyHead, QueuePoll.instance());
        long            lEnd   = System.nanoTime();
        m_statistics.polled(lEnd - lStart);
        return result;
        }

    protected QueuePollResult peekAtHeadInternal()
        {
        long            lStart = System.nanoTime();
        QueuePollResult result = m_cache.invoke(m_keyHead, QueuePeek.instance());
        long            lEnd   = System.nanoTime();
        m_statistics.polled(lEnd - lStart);
        return result;
        }

    protected Object[] toArrayInternal(Object[] ao)
        {
        int c = size();
        if (ao == null)
            {
            ao = new Object[c];
            }
        else if (ao.length < c)
            {
            ao = (Object[])java.lang.reflect.Array.newInstance(
                ao.getClass().getComponentType(), c);
            }
        else if (ao.length > c)
            {
            ao[c] = null;
            }

        Iterator iter = iterator();
        for (int i = 0; i < c; i++)
            {
            try
                {
                ao[i] = iter.next();
                }
            catch (ArrayStoreException e) // NoSuchElement; IndexOutOfBounds
                {
                throw e;
                }
            catch (RuntimeException e) // NoSuchElement; IndexOutOfBounds
                {
                throw new ConcurrentModificationException(e.toString());
                }
            }

        return ao;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The maximum size for a queue.
     * <p>
     * A queue is stored in a single partition and partition size is limited to 2GB per cache
     * (or max int value of 2147483647 bytes).
     * This max size is about 147MB under 2GB.
     */
    public static final long MAX_QUEUE_SIZE = 2000000000;

    // ----- data members ---------------------------------------------------

    /**
     * The {@link QueueService}.
     */
    protected final QueueService m_service;

    /**
     * The {@link MutableQueueStatistics} to use.
     */
    protected final MutableQueueStatistics m_statistics;

    /**
     * The key to use to invoke operations against the queue head
     */
    protected final QueueKey m_keyHead;

    /**
     * The key to use to invoke operations against the queue tail
     */
    protected final QueueKey m_keyTail;
    }
