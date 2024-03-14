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
import com.tangosol.net.NamedMap;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An {@link Iterator} that returns the ordered contents of a
 * {@link NamedCacheQueue} page by page.
 *
 * @param <E>  the type of elements in the queue
 */
public class QueuePageIterator<E>
        implements Iterator<E>
    {
    /**
     * Create a {@link QueuePageIterator}.
     *
     * @param nQueueHash    the queue name hash
     * @param fHead         {@code true} if the iteration starts at the head
     * @param fPoll         {@code true} to remove iterated entries
     * @param cache         the {@link NamedMap} containing the queue data
     * @param nPageSize     the size of the page to retrieve
     * @param cMaxElements  the maximum number of elements to return
     */
    private QueuePageIterator(int nQueueHash, boolean fHead, boolean fPoll,
                              NamedMap<QueueKey, E> cache, int nPageSize, int cMaxElements)
        {
        m_nQueueHash         = nQueueHash;
        m_fHead              = fHead;
        m_fPoll              = fPoll;
        m_cache              = cache;
        m_nLastId            = fHead ? Long.MIN_VALUE : Long.MAX_VALUE;
        m_fHasNext           = true;
        m_iterator           = Collections.emptyIterator();
        m_nPageSize  = nPageSize;
        m_cRemaining = cMaxElements;
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
    public E next()
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
                int                nPage  = Math.min(m_nPageSize, m_cRemaining);
                QueueKey           key    = new QueueKey(m_nQueueHash, Long.MAX_VALUE - 1);
                QueuePageResult<E> result = m_cache.invoke(key, new QueuePage<>(m_fHead, nPage, m_nLastId, m_fPoll));
                List<E>            list   = result.getList();
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
     * Return a {@link QueuePageIterator} that iterates from head to tail.
     *
     * @param nQueueHash  the queue name hash
     * @param cache       the {@link NamedMap} containing the queue contents
     * @param <E>         the type of element in the queue
     *
     * @return a {@link QueuePageIterator} that iterates from head to tail
     */
    public static <E> QueuePageIterator<E> head(int nQueueHash, NamedMap<QueueKey, E> cache)
        {
        return new QueuePageIterator<>(nQueueHash, true, false, cache, DEFAULT_PAGE_SIZE, Integer.MAX_VALUE);
        }

    /**
     * Return a {@link QueuePageIterator} that iterates from tail to head.
     *
     * @param nQueueHash  the queue name hash
     * @param cache       the {@link NamedMap} containing the queue contents
     * @param <E>         the type of element in the queue
     *
     * @return a {@link QueuePageIterator} that iterates from head to tail
     */
    public static <E> QueuePageIterator<E> tail(int nQueueHash, NamedMap<QueueKey, E> cache)
        {
        return new QueuePageIterator<>(nQueueHash, false, false, cache, DEFAULT_PAGE_SIZE, Integer.MAX_VALUE);
        }

    /**
     * Return a {@link QueuePageIterator} that iterates from head to tail and removes
     * the values from the queue as each page is returned.
     *
     * @param nQueueHash  the queue name hash
     * @param cache       the {@link NamedMap} containing the queue contents
     * @param <E>         the type of element in the queue
     *
     * @return a {@link QueuePageIterator} that iterates from head to tail
     */
    public static <E> QueuePageIterator<E> headPolling(int nQueueHash, NamedMap<QueueKey, E> cache)
        {
        return headPolling(nQueueHash, cache, Integer.MAX_VALUE);
        }

    /**
     * Return a {@link QueuePageIterator} that iterates from head to tail and removes
     * the values from the queue as each page is returned.
     *
     * @param nQueueHash  the queue name hash
     * @param cache       the {@link NamedMap} containing the queue contents
     * @param cMax        the maximum number of entries to retrieve
     * @param <E>         the type of element in the queue
     *
     * @return a {@link QueuePageIterator} that iterates from head to tail
     */
    public static <E> QueuePageIterator<E> headPolling(int nQueueHash, NamedMap<QueueKey, E> cache, int cMax)
        {
        return new QueuePageIterator<>(nQueueHash, true, true, cache, DEFAULT_PAGE_SIZE, cMax);
        }

    /**
     * Return a {@link QueuePageIterator} that iterates from tail to head and removes
     * the values from the queue as each page is returned.
     *
     * @param nQueueHash  the queue name hash
     * @param cache       the {@link NamedMap} containing the queue contents
     * @param <E>         the type of element in the queue
     *
     * @return a {@link QueuePageIterator} that iterates from head to tail
     */
    public static <E> QueuePageIterator<E> tailPolling(int nQueueHash, NamedMap<QueueKey, E> cache)
        {
        return new QueuePageIterator<>(nQueueHash, false, true, cache, DEFAULT_PAGE_SIZE, Integer.MAX_VALUE);
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
    private final NamedMap<QueueKey, E> m_cache;

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
    private Iterator<E> m_iterator;

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

    /**
     * The remaining number of elements to return.
     */
    private int m_cRemaining;
    }
