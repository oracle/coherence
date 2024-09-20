/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.internal.net.queue.model.QueueOfferResult;
import com.tangosol.internal.net.queue.model.QueuePollResult;

import com.tangosol.internal.net.queue.processor.QueueOffer;
import com.tangosol.internal.net.queue.processor.QueuePeek;
import com.tangosol.internal.net.queue.processor.QueuePoll;

import com.tangosol.internal.net.queue.processor.QueueRemove;
import com.tangosol.net.NamedMap;
import com.tangosol.net.NamedQueue;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Extractors;
import com.tangosol.util.Filters;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A {@link NamedQueue} implementation that wraps a {@link NamedMap}.
 *
 * @param <E> the type of elements held in this queue
 */
public class SimpleNamedMapDeque<E>
        extends SimpleNamedMapQueue<E>
        implements NamedMapDeque<QueueKey, E>
    {
    /**
     * Create a {@link NamedMapDeque} that wrap s a {@link NamedMap}.
     *
     * @param sName  the name of the cache to wrap
     * @param cache  the {@link NamedMap} to wrap
     *
     * @throws NullPointerException      if either of the {@code name} or {@code service} parameters is {@code null}
     * @throws IllegalArgumentException  if the name is blank
     */
    public SimpleNamedMapDeque(String sName, NamedMap<QueueKey, E> cache)
        {
        super(Objects.requireNonNull(sName), Objects.requireNonNull(cache));
        }

    // ----- NamedDeque methods ---------------------------------------------

    @Override
    public long prepend(E e)
        {
        QueueOfferResult result = offerToHeadInternal(e);
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
    public void addFirst(E e)
        {
        QueueOfferResult result  = offerToHeadInternal(e);
        int              nStatus = result.getResult();
        if (nStatus == QueueOfferResult.RESULT_FAILED_CAPACITY)
            {
            m_statistics.registerRejected();
            throw new IllegalStateException("Queue " + m_sName + " is full");
            }
        m_statistics.registerAccepted();
        }

    @Override
    public void addLast(E e)
        {
        QueueOfferResult result  = offerToTailInternal(e);
        int              nStatus = result.getResult();
        if (nStatus == QueueOfferResult.RESULT_FAILED_CAPACITY)
            {
            m_statistics.registerRejected();
            throw new IllegalStateException("Queue " + m_sName + " is full");
            }
        m_statistics.registerAccepted();
        }

    @Override
    public boolean offerFirst(E e)
        {
        QueueOfferResult result = offerToHeadInternal(e);
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
    public boolean offerLast(E e)
        {
        return offer(e);
        }

    @Override
    public E removeFirst()
        {
        E e = poll();
        if (e == null)
            {
            throw new NoSuchElementException();
            }
        return e;
        }

    @Override
    public E removeLast()
        {
        E e = pollLast();
        if (e == null)
            {
            throw new NoSuchElementException();
            }
        return e;
        }

    @Override
    public E pollFirst()
        {
        return poll();
        }

    @Override
    public E pollLast()
        {
        QueuePollResult result = pollFromTailInternal();
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
    public E getFirst()
        {
        E e = peek();
        if (e == null)
            {
            throw new NoSuchElementException();
            }
        return e;
        }

    @Override
    public E getLast()
        {
        E e = peekLast();
        if (e == null)
            {
            throw new NoSuchElementException();
            }
        return e;
        }

    @Override
    public E peekFirst()
        {
        return peek();
        }

    @Override
    public E peekLast()
        {
        QueuePollResult result = peekAtTailInternal();
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
    public boolean removeFirstOccurrence(Object o)
        {
        Map<QueueKey, Boolean> map = m_cache.invokeAll(Filters.equal(Extractors.identity(), o),
                QueueRemove.removeFirst());
        if (map.isEmpty())
            {
            return false;
            }
        return map.values().iterator().next();
        }

    @Override
    public boolean removeLastOccurrence(Object o)
        {
        Map<QueueKey, Boolean> map = m_cache.invokeAll(Filters.equal(Extractors.identity(), o),
                QueueRemove.removeLast());
        if (map.isEmpty())
            {
            return false;
            }
        return map.values().iterator().next();
        }

    @Override
    public void push(E e)
        {
        addFirst(e);
        }

    @Override
    public E pop()
        {
        return removeFirst();
        }

    @Override
    public Iterator<E> descendingIterator()
        {
        return QueuePageIterator.tail(this::createKey, m_cache);
        }

    // ----- helper methods -------------------------------------------------

    protected QueueOfferResult offerToHeadInternal(E e)
        {
        long             lStart    = System.nanoTime();
        QueueOffer<E>    processor = new QueueOffer<>(e);
        QueueOfferResult result    = m_cache.invoke(m_keyHead.randomHead(), processor);
        long             lEnd      = System.nanoTime();
        m_statistics.offered(lEnd - lStart);
        return result;
        }


    protected QueuePollResult pollFromTailInternal()
        {
        long            lStart = System.nanoTime();
        QueuePollResult result = m_cache.invoke(m_keyTail, QueuePoll.instance());
        long            lEnd   = System.nanoTime();
        m_statistics.polled(lEnd - lStart);
        return result;
        }

    protected QueuePollResult peekAtTailInternal()
        {
        long            lStart = System.nanoTime();
        QueuePollResult result = m_cache.invoke(m_keyTail, QueuePeek.instance());
        long            lEnd   = System.nanoTime();
        m_statistics.polled(lEnd - lStart);
        return result;
        }
    }
