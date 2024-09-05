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

import com.tangosol.io.Serializer;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedMap;
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
 * A base class for {@link NamedQueue} implementations that wraps a {@link NamedMap}.
 *
 * @param <E> the type of elements held in this queue
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class BaseNamedMapQueue<K extends QueueKey, E>
        extends NamedMapValuesCollection<K, E>
        implements NamedMapQueue<K, E>
    {
    /**
     * Create a {@link BaseNamedMapQueue} that wrap s a {@link NamedMap}.
     *
     * @param sName    the name of the cache to wrap
     * @param cache    the {@link NamedMap} to wrap
     *
     * @throws NullPointerException      if either of the {@code name} or {@code cache} parameters is {@code null}
     * @throws IllegalArgumentException  if the name is blank
     */
    public BaseNamedMapQueue(String sName, NamedMap<K, E> cache)
        {
        super(Objects.requireNonNull(sName), Objects.requireNonNull(cache));
        m_service    = ensureQueueService(cache.getService());
        m_serializer = m_service.getSerializer();
        m_statistics = new SimpleQueueStatistics();
        m_keyHead    = QueueKey.head(m_sName);
        m_keyTail    = QueueKey.tail(m_sName);

        m_cache.addIndex(QueueKeyExtractor.instance(), true, null);
        }

    // ----- NamedQueue methods ---------------------------------------------

    @Override
    public K createKey(long id)
        {
        return (K) new QueueKey(m_keyHead.getHash(), id);
        }

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
        MapListenerCollectionListener<K, E> wrapper = new MapListenerCollectionListener(listener);
        m_cache.addMapListener(wrapper);
        }

    @Override
    public void removeListener(CollectionListener<? super E> listener)
        {
        MapListenerCollectionListener<K, E> wrapper = new MapListenerCollectionListener(listener);
        m_cache.removeMapListener(wrapper);
        }

    @Override
    public void addListener(CollectionListener<? super E> listener, Filter<E> filter, boolean fLite)
        {
        MapListenerCollectionListener<K, E> wrapper = new MapListenerCollectionListener(listener);
        m_cache.addMapListener(wrapper, filter, fLite);
        }

    @Override
    public void removeListener(CollectionListener<? super E> listener, Filter<E> filter)
        {
        MapListenerCollectionListener<K, E> wrapper = new MapListenerCollectionListener(listener);
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
        QueuePollResult result  = peekAtHeadInternal();
        Binary          binary  = result.getBinaryElement();
        E               element = binary == null ? null : ExternalizableHelper.fromBinary(binary, m_serializer);
        if (element != null)
            {
            m_statistics.registerHit();
            return element;
            }
        m_statistics.registerMiss();
        throw new NoSuchElementException();
        }

    @Override
    public long append(E e)
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
        return fSuccess ? result.getId() : Long.MIN_VALUE;
        }

    @Override
    public boolean offer(E e)
        {
        long id = append(e);
        return id >= 0L;
        }

    @Override
    public E peek()
        {
        QueuePollResult result = peekAtHeadInternal();
        Binary          binary = result.getBinaryElement();
        E               oValue = binary == null ? null : ExternalizableHelper.fromBinary(binary, m_serializer);
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
        Binary          binary = result.getBinaryElement();
        E               oValue = binary == null ? null : ExternalizableHelper.fromBinary(binary, m_serializer);
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
        Binary          binary  = result.getBinaryElement();
        E               element = binary == null ? null : ExternalizableHelper.fromBinary(binary, m_serializer);
        if (element != null)
            {
            m_statistics.registerHit();
            return element;
            }
        m_statistics.registerMiss();
        throw new NoSuchElementException();
        }

    @Override
    public abstract Iterator<E> iterator();

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

    protected abstract QueueOfferResult offerToTailInternal(E e);

    protected abstract QueuePollResult pollFromHeadInternal();

    protected abstract QueuePollResult peekAtHeadInternal();

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
                throw new ConcurrentModificationException("element " + i + ": " + e.getMessage());
                }
            }

        return ao;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link QueueService}.
     */
    protected final QueueService m_service;

    /**
     * The service serializer.
     */
    protected final Serializer m_serializer;

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
