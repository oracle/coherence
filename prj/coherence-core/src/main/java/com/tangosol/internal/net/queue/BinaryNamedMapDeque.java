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
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedMap;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.Session;
import com.tangosol.net.options.WithClassLoader;
import com.tangosol.util.Binary;
import com.tangosol.util.Extractors;
import com.tangosol.util.Filters;
import com.tangosol.util.NullImplementation;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A {@link NamedQueue} implementation that wraps a {@link NamedMap}.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class BinaryNamedMapDeque
        extends BinaryNamedMapQueue
        implements NamedMapDeque<Binary, Binary>
    {
    /**
     * Create a {@link BinaryNamedMapDeque} that wrap s a {@link NamedMap}.
     *
     * @param sName    the name of the cache to wrap
     * @param session  the {@link Session} to obtain the underlying cache
     *
     * @throws NullPointerException      if either of the {@code name} or {@code cache} parameters is {@code null}
     * @throws IllegalArgumentException  if the name is blank
     */
    public BinaryNamedMapDeque(String sName, Session session)
        {
        this(sName, session.getCache(sName, WithClassLoader.nullImplementation()));
        }

    /**
     * Create a {@link BinaryNamedMapDeque} that wrap s a {@link NamedMap}.
     *
     * @param sName  the name of the cache to wrap
     * @param eccf   the {@link ExtensibleConfigurableCacheFactory} to obtain the underlying cache
     *
     * @throws NullPointerException      if either of the {@code name} or {@code cache} parameters is {@code null}
     * @throws IllegalArgumentException  if the name is blank
     */
    public BinaryNamedMapDeque(String sName, ExtensibleConfigurableCacheFactory eccf)
        {
        this(sName, eccf.ensureCache(sName, NullImplementation.getClassLoader()));
        }

    /**
     * Create a {@link BinaryNamedMapDeque} that wrap s a {@link NamedMap}.
     *
     * @param sName  the name of the cache to wrap
     * @param cache  the {@link NamedMap} to wrap
     *
     * @throws NullPointerException      if either of the {@code name} or {@code service} parameters is {@code null}
     * @throws IllegalArgumentException  if the name is blank
     */
    public BinaryNamedMapDeque(String sName, NamedMap<Binary, Binary> cache)
        {
        super(Objects.requireNonNull(sName), Objects.requireNonNull(cache));
        }

    // ----- NamedDeque methods ---------------------------------------------

    @Override
    public long prepend(Binary e)
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
    public void addFirst(Binary e)
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
    public void addLast(Binary e)
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
    public boolean offerFirst(Binary e)
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
    public boolean offerLast(Binary e)
        {
        return offer(e);
        }

    @Override
    public Binary removeFirst()
        {
        Binary e = poll();
        if (e == null)
            {
            throw new NoSuchElementException();
            }
        return e;
        }

    @Override
    public Binary removeLast()
        {
        Binary e = pollLast();
        if (e == null)
            {
            throw new NoSuchElementException();
            }
        return e;
        }

    @Override
    public Binary pollFirst()
        {
        return poll();
        }

    @Override
    public Binary pollLast()
        {
        QueuePollResult result = pollFromTailInternal();
        Binary          binary = result.getBinaryElement();
        if (binary == null)
            {
            m_statistics.registerMiss();
            }
        else
            {
            m_statistics.registerHit();
            }
        return binary;
        }

    @Override
    public Binary getFirst()
        {
        Binary e = peek();
        if (e == null)
            {
            throw new NoSuchElementException();
            }
        return e;
        }

    @Override
    public Binary getLast()
        {
        Binary e = peekLast();
        if (e == null)
            {
            throw new NoSuchElementException();
            }
        return e;
        }

    @Override
    public Binary peekFirst()
        {
        return peek();
        }

    @Override
    public Binary peekLast()
        {
        QueuePollResult result = peekAtTailInternal();
        Binary          binary = result.getBinaryElement();
        if (binary == null)
            {
            m_statistics.registerMiss();
            }
        else
            {
            m_statistics.registerHit();
            }
        return binary;
        }

    @Override
    public boolean removeFirstOccurrence(Object o)
        {
        QueueRemove           remove = QueueRemove.removeFirst();
        Map<QueueKey, Binary> map    = m_cache.invokeAll(Filters.equal(Extractors.identity(), o),
                remove);
        if (map.isEmpty())
            {
            return false;
            }
        return (Boolean) m_converterValueFromInternal.convert(map.values().iterator().next());
        }

    @Override
    public boolean removeLastOccurrence(Object o)
        {
        QueueRemove           remove = QueueRemove.removeLast();
        Map<QueueKey, Binary> map    = m_cache.invokeAll(Filters.equal(Extractors.identity(), o), remove);
        if (map.isEmpty())
            {
            return false;
            }
        return (Boolean) m_converterValueFromInternal.convert(map.values().iterator().next());
        }

    @Override
    public void push(Binary e)
        {
        addFirst(e);
        }

    @Override
    public Binary pop()
        {
        return removeFirst();
        }

    @Override
    public Iterator<Binary> descendingIterator()
        {
        return BinaryQueuePageIterator.tail(m_keyHead.getHash(), m_cache);
        }

    // ----- helper methods -------------------------------------------------

    protected QueueOfferResult offerToHeadInternal(Binary e)
        {
        long             lStart    = System.nanoTime();
        QueueOffer       processor = new QueueOffer<>(e);
        Binary           binKey    = m_converterKeyToInternal.convert(m_keyHead.randomHead());
        Binary           binary    = (Binary) m_cache.invoke(binKey, processor);
        QueueOfferResult result    = (QueueOfferResult) m_converterValueFromInternal.convert(binary);
        long             lEnd      = System.nanoTime();
        m_statistics.offered(lEnd - lStart);
        return result;
        }


    protected QueuePollResult pollFromTailInternal()
        {
        long              lStart   = System.nanoTime();
        QueuePoll       poll = QueuePoll.instance();
        Binary binary   = (Binary) m_cache.invoke(m_binKeyTail, poll);
        QueuePollResult   result    = (QueuePollResult) m_converterValueFromInternal.convert(binary);
        long              lEnd     = System.nanoTime();
        m_statistics.polled(lEnd - lStart);
        return result;
        }

    protected QueuePollResult peekAtTailInternal()
        {
        long             lStart = System.nanoTime();
        QueuePeek        peek   = QueuePeek.instance();
        Binary           binary = (Binary) m_cache.invoke(m_binKeyTail, peek);
        QueuePollResult  result = (QueuePollResult) m_converterValueFromInternal.convert(binary);
        long             lEnd   = System.nanoTime();
        m_statistics.polled(lEnd - lStart);
        return result;
        }
    }
