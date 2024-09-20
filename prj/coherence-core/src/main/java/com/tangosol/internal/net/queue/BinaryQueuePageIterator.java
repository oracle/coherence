/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.internal.net.queue.model.QueuePageResult;
import com.tangosol.internal.net.queue.processor.QueuePage;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.NamedMap;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An {@link Iterator} that returns the ordered contents of a
 * {@link NamedMapQueue} page by page.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class BinaryQueuePageIterator
        implements Iterator<Binary>
    {
    /**
     * Create a {@link BinaryQueuePageIterator}.
     *
     * @param nQueueHash    the queue name hash
     * @param fHead         {@code true} if the iteration starts at the head
     * @param fPoll         {@code true} to remove iterated entries
     * @param cache         the {@link NamedMap} containing the queue data
     * @param nPageSize     the size of the page to retrieve
     * @param cMaxElements  the maximum number of elements to return
     */
    private BinaryQueuePageIterator(int nQueueHash, boolean fHead, boolean fPoll,
                                    NamedMap<Binary, Binary> cache, int nPageSize, int cMaxElements)
        {
        m_nQueueHash         = nQueueHash;
        m_fHead              = fHead;
        m_fPoll              = fPoll;
        m_cache              = cache;
        m_nLastId      = fHead ? Long.MIN_VALUE : Long.MAX_VALUE;
        m_fHasNext     = true;
        m_iterator     = Collections.emptyIterator();
        m_nPageSize    = nPageSize;
        m_cRemaining   = cMaxElements;
        BackingMapManagerContext context = cache.getService().getBackingMapManager().getContext();
        m_converterKey   = context.getKeyToInternalConverter();
        m_converterValue = context.getValueFromInternalConverter();
        }

    // ----- Iterator methods -----------------------------------------------

    @Override
    public boolean hasNext()
        {
        if (m_fHasNext)
            {
            if (!m_iterator.hasNext())
                {
                nextPage();
                }
            }
        return m_fHasNext;
        }

    @Override
    public Binary next()
        {
        if (m_fHasNext)
            {
            if (!m_iterator.hasNext())
                {
                nextPage();
                }
            }
        return m_iterator.next();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtain the next page of value to iterator over.
     */
    protected void nextPage()
        {
        if (m_fHasNext)
            {
            m_lock.lock();
            try
                {
                int                     nPage  = Math.min(m_nPageSize, m_cRemaining);
                QueueKey                key    = new QueueKey(m_nQueueHash, Long.MAX_VALUE - 1);
                Binary                  binKey = m_converterKey.convert(key);
                Binary          binary = (Binary) m_cache.invoke(binKey, new QueuePage(m_fHead, nPage, m_nLastId, m_fPoll));
                QueuePageResult result = (QueuePageResult) m_converterValue.convert(binary);
                List<Binary>    list   = result.getBinaryList();
                if (list.isEmpty())
                    {
                    m_fHasNext = false;
                    m_iterator = Collections.emptyIterator();
                    }
                else
                    {
                    m_cRemaining -= list.size();
                    m_iterator   = list.iterator();
                    m_fHasNext   = true;
                    m_nLastId    = result.getKey();
                    }
                }
            finally
                {
                m_lock.unlock();
                }
            }
        }

    /**
     * Return a {@link BinaryQueuePageIterator} that iterates from head to tail.
     *
     * @param nQueueHash  the queue name hash
     * @param cache       the {@link NamedMap} containing the queue contents
     *
     * @return a {@link BinaryQueuePageIterator} that iterates from head to tail
     */
    public static BinaryQueuePageIterator head(int nQueueHash, NamedMap<Binary, Binary> cache)
        {
        return new BinaryQueuePageIterator(nQueueHash, true, false, cache, DEFAULT_PAGE_SIZE, Integer.MAX_VALUE);
        }

    /**
     * Return a {@link BinaryQueuePageIterator} that iterates from tail to head.
     *
     * @param nQueueHash  the queue name hash
     * @param cache       the {@link NamedMap} containing the queue contents
     *
     * @return a {@link BinaryQueuePageIterator} that iterates from head to tail
     */
    public static BinaryQueuePageIterator tail(int nQueueHash, NamedMap<Binary, Binary> cache)
        {
        return new BinaryQueuePageIterator(nQueueHash, false, false, cache, DEFAULT_PAGE_SIZE, Integer.MAX_VALUE);
        }

    /**
     * Return a {@link BinaryQueuePageIterator} that iterates from head to tail and removes
     * the values from the queue as each page is returned.
     *
     * @param nQueueHash  the queue name hash
     * @param cache       the {@link NamedMap} containing the queue contents
     *
     * @return a {@link BinaryQueuePageIterator} that iterates from head to tail
     */
    public static BinaryQueuePageIterator headPolling(int nQueueHash, NamedMap<Binary, Binary> cache)
        {
        return headPolling(nQueueHash, cache, Integer.MAX_VALUE);
        }

    /**
     * Return a {@link BinaryQueuePageIterator} that iterates from head to tail and removes
     * the values from the queue as each page is returned.
     *
     * @param nQueueHash  the queue name hash
     * @param cache       the {@link NamedMap} containing the queue contents
     * @param cMax        the maximum number of entries to retrieve
     *
     * @return a {@link BinaryQueuePageIterator} that iterates from head to tail
     */
    public static BinaryQueuePageIterator headPolling(int nQueueHash, NamedMap<Binary, Binary> cache, int cMax)
        {
        return new BinaryQueuePageIterator(nQueueHash, true, true, cache, DEFAULT_PAGE_SIZE, cMax);
        }

    /**
     * Return a {@link BinaryQueuePageIterator} that iterates from tail to head and removes
     * the values from the queue as each page is returned.
     *
     * @param nQueueHash  the queue name hash
     * @param cache       the {@link NamedMap} containing the queue contents
     *
     * @return a {@link BinaryQueuePageIterator} that iterates from head to tail
     */
    public static BinaryQueuePageIterator tailPolling(int nQueueHash, NamedMap<Binary, Binary> cache)
        {
        return new BinaryQueuePageIterator(nQueueHash, false, true, cache, DEFAULT_PAGE_SIZE, Integer.MAX_VALUE);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The default size of a page to return
     */
    public static final int DEFAULT_PAGE_SIZE = 100;

    /**
     * The queue name hash.
     */
    private final int m_nQueueHash;

    /**
     * The {@link NamedMap} containing the queue data.
     */
    private final NamedMap<Binary, Binary> m_cache;

    /**
     * {@code true} to iterate from head to tail, or {@link false}
     * to iterate tail to head.
     */
    private final boolean m_fHead;

    /**
     * {@code true} to remove the values in each page as they are returned.
     */
    private final boolean m_fPoll;

    /**
     * The last queue key id returned.
     */
    private long m_nLastId;

    /**
     * The inner iterator of data.
     */
    private Iterator<Binary> m_iterator;

    /**
     * {@code true} if there are still pages of data to fetch.
     */
    private boolean m_fHasNext;

    /**
     * The lock to control fetching pages.
     */
    private final Lock m_lock = new ReentrantLock();

    /**
     * The size of the page to retrieve.
     */
    private final int m_nPageSize;

    private final Converter<QueueKey, Binary> m_converterKey;

    private final Converter<Binary, Object> m_converterValue;

    /**
     * The remaining number of elements to return.
     */
    private int m_cRemaining;
    }
