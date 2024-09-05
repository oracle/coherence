/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.internal.net.queue.model.QueueOfferResult;
import com.tangosol.internal.net.queue.model.QueuePollResult;
import com.tangosol.internal.net.queue.processor.QueueOffer;
import com.tangosol.internal.net.queue.processor.QueuePeek;
import com.tangosol.internal.net.queue.processor.QueuePoll;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedMap;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.Session;
import com.tangosol.net.options.WithClassLoader;
import com.tangosol.util.Binary;
import com.tangosol.util.NullImplementation;

import java.util.Iterator;

/**
 * A {@link NamedQueue} implementation that wraps a {@link NamedMap}.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class BinaryNamedMapQueue
        extends BaseBinaryNamedMapQueue
    {
    /**
     * Create a {@link BinaryNamedMapQueue} that wrap s a {@link NamedMap}.
     *
     * @param sName    the name of the cache to wrap
     * @param session  the {@link Session} to obtain the underlying cache
     *
     * @throws NullPointerException      if either of the {@code name} or {@code cache} parameters is {@code null}
     * @throws IllegalArgumentException  if the name is blank
     */
    public BinaryNamedMapQueue(String sName, Session session)
        {
        this(sName, session.getCache(sName, WithClassLoader.nullImplementation()));
        }

    /**
     * Create a {@link BinaryNamedMapQueue} that wrap s a {@link NamedMap}.
     *
     * @param sName  the name of the cache to wrap
     * @param eccf   the {@link ExtensibleConfigurableCacheFactory} to obtain the underlying cache
     *
     * @throws NullPointerException      if either of the {@code name} or {@code cache} parameters is {@code null}
     * @throws IllegalArgumentException  if the name is blank
     */
    public BinaryNamedMapQueue(String sName, ExtensibleConfigurableCacheFactory eccf)
        {
        this(sName, eccf.ensureCache(sName, NullImplementation.getClassLoader()));
        }

    /**
     * Create a {@link BinaryNamedMapQueue} that wrap s a {@link NamedMap}.
     *
     * @param sName    the name of the cache to wrap
     * @param cache    the {@link NamedMap} to wrap
     *
     * @throws NullPointerException      if either of the {@code name} or {@code cache} parameters is {@code null}
     * @throws IllegalArgumentException  if the name is blank
     */
    public BinaryNamedMapQueue(String sName, NamedMap<Binary, Binary> cache)
        {
        super(sName, cache);
        }

    // ----- BaseNamedCacheQueue methods ------------------------------------

    @Override
    public Iterator<Binary> iterator()
        {
        return BinaryQueuePageIterator.head(m_nHash, m_cache);
        }

    @Override
    protected QueueOfferResult offerToTailInternal(Binary e)
        {
        long               lStart    = System.nanoTime();
        QueueOffer         processor = new QueueOffer<>(e);
        Binary             binKey    = m_converterKeyToInternal.convert(m_keyTail.randomTail());
        Binary             binResult = (Binary) m_cache.invoke(binKey, processor);
        QueueOfferResult   result    = (QueueOfferResult) m_converterValueFromInternal.convert(binResult);
        long               lEnd      = System.nanoTime();
        m_statistics.offered(lEnd - lStart);
        return result;
        }

    @Override
    protected QueuePollResult pollFromHeadInternal()
        {
        long            lStart    = System.nanoTime();
        QueuePoll       poll      = QueuePoll.instance();
        Binary          binResult = (Binary) m_cache.invoke(m_binKeyHead, poll);
        QueuePollResult result    = (QueuePollResult) m_converterValueFromInternal.convert(binResult);
        long            lEnd      = System.nanoTime();
        m_statistics.polled(lEnd - lStart);
        return result;
        }

    @Override
    protected QueuePollResult peekAtHeadInternal()
        {
        long            lStart    = System.nanoTime();
        QueuePeek       peek      = QueuePeek.instance();
        Binary          binResult = (Binary) m_cache.invoke(m_binKeyHead, peek);
        QueuePollResult result    = (QueuePollResult) m_converterValueFromInternal.convert(binResult);
        long            lEnd      = System.nanoTime();
        m_statistics.polled(lEnd - lStart);
        return result;
        }
    }
