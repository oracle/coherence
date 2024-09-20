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

import com.tangosol.net.NamedMap;
import com.tangosol.net.NamedQueue;

import java.util.Iterator;

/**
 * A {@link NamedQueue} implementation that wraps a {@link NamedMap}.
 *
 * @param <E> the type of elements held in this queue
 */
public class SimpleNamedMapQueue<E>
        extends BaseNamedMapQueue<QueueKey, E>
        implements NamedMapQueue<QueueKey, E>
    {
    /**
     * Create a {@link NamedMapQueue} that wrap s a {@link NamedMap}.
     *
     * @param sName    the name of the cache to wrap
     * @param cache    the {@link NamedMap} to wrap
     *
     * @throws NullPointerException      if either of the {@code name} or {@code cache} parameters is {@code null}
     * @throws IllegalArgumentException  if the name is blank
     */
    public SimpleNamedMapQueue(String sName, NamedMap<QueueKey, E> cache)
        {
        super(sName, cache);
        }

    // ----- BaseNamedCacheQueue methods ------------------------------------

    @Override
    public Iterator<E> iterator()
        {
        return QueuePageIterator.head(this::createKey, m_cache);
        }

    @Override
    protected QueueOfferResult offerToTailInternal(E e)
        {
        long             lStart    = System.nanoTime();
        QueueOffer<E>    processor = new QueueOffer<>(e);
        QueueOfferResult result    = m_cache.invoke(m_keyTail.randomTail(), processor);
        long             lEnd      = System.nanoTime();
        m_statistics.offered(lEnd - lStart);
        return result;
        }

    @Override
    protected QueuePollResult pollFromHeadInternal()
        {
        long            lStart  = System.nanoTime();
        QueuePollResult result = m_cache.invoke(m_keyHead, QueuePoll.instance());
        long            lEnd   = System.nanoTime();
        m_statistics.polled(lEnd - lStart);
        return result;
        }

    @Override
    protected QueuePollResult peekAtHeadInternal()
        {
        long            lStart = System.nanoTime();
        QueuePollResult result = m_cache.invoke(m_keyHead, QueuePeek.instance());
        long            lEnd   = System.nanoTime();
        m_statistics.polled(lEnd - lStart);
        return result;
        }
    }
