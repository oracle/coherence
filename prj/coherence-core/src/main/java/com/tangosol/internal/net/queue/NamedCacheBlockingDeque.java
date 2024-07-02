/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.internal.net.SessionNamedBlockingDeque;
import com.tangosol.internal.net.SessionNamedDeque;

import com.tangosol.internal.net.queue.model.QueueKey;

import com.tangosol.net.NamedBlockingDeque;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedDeque;
import com.tangosol.net.Session;
import com.tangosol.net.ValueTypeAssertion;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.filter.AlwaysFilter;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link NamedBlockingDeque} implementation that wraps a {@link NamedCache}.
 *
 * @param <E> the type of elements held in this queue
 */
public class NamedCacheBlockingDeque<E>
        extends NamedCacheDeque<E>
        implements NamedBlockingDeque<E>, MapListener<QueueKey, E>
    {
    /**
     * Create a {@link NamedCacheBlockingDeque} that wrap s a {@link NamedCache}.
     *
     * @param sName  the name of this queue
     * @param cache  the {@link NamedCache} to wrap
     */
    public NamedCacheBlockingDeque(String sName, NamedCache<QueueKey, E> cache)
        {
        super(sName, cache);
        cache.addMapListener(this, AlwaysFilter.INSTANCE(), true);
        }

    // ----- factory methods ------------------------------------------------

    /**
     * Create a {@link Builder} option.
     * <p>
     * The {@link Builder} can be passed as an option to the
     * {@link Session#getDeque(String, Option...)} method to return
     * a {@link NamedBlockingDeque} instead of a {@link NamedDeque}.
     */
    public Builder builder()
        {
        return builder("");
        }

    /**
     * Create a {@link Builder} option.
     * <p>
     * The {@link Builder} can be passed as an option to the
     * {@link Session#getDeque(String, Option...)} method to return
     * a {@link NamedBlockingDeque} instead of a {@link NamedDeque}.
     *
     * @param sNamePrefix  the prefix to add to queue cache names
     */
    public Builder builder(String sNamePrefix)
        {
        return new Builder(sNamePrefix);
        }

    // ----- BlockingDeque methods ------------------------------------------

    @Override
    public long prepend(E e, long timeout, TimeUnit unit) throws InterruptedException
        {
        long          nanos = unit.toNanos(timeout);
        ReentrantLock lock  = m_lock;
        lock.lockInterruptibly();
        try
            {
            long nId = prepend(e);
            while (nId < 0L)
                {
                if (nanos <= 0L)
                    {
                    return -1L;
                    }
                nanos = m_notFull.awaitNanos(nanos);
                nId = prepend(e);
              }
            return nId;
            }
        finally
            {
            lock.unlock();
            }
        }

    @Override
    public long prependFirst(E e) throws InterruptedException
        {
        final ReentrantLock lock = m_lock;
        lock.lock();
        try
            {
            long nId = prepend(e);
            while (nId < 0L)
                {
                m_notFull.await();
                nId = prepend(e);
                }
            return nId;
            }
        finally
            {
            lock.unlock();
            }
        }

    @Override
    public long append(E e, long timeout, TimeUnit unit) throws InterruptedException
        {
        long          nanos = unit.toNanos(timeout);
        ReentrantLock lock  = m_lock;
        lock.lockInterruptibly();
        try
            {
            long nId = append(e);
            while (nId < 0L)
                {
                if (nanos <= 0L)
                    {
                    return -1L;
                    }
                nanos = m_notFull.awaitNanos(nanos);
                nId = append(e);
              }
            return nId;
            }
        finally
            {
            lock.unlock();
            }
        }

    @Override
    public long appendLast(E e) throws InterruptedException
        {
        final ReentrantLock lock = m_lock;
        lock.lock();
        try
            {
            long nId = append(e);
            while (nId < 0L)
                {
                m_notFull.await();
                nId = append(e);
                }
            return nId;
            }
        finally
            {
            lock.unlock();
            }
        }

    @Override
    public void putFirst(E e) throws InterruptedException
        {
        final ReentrantLock lock = m_lock;
        lock.lock();
        try
            {
            while (!offerFirst(e))
                {
                m_notFull.await();
                }
            }
        finally
            {
            lock.unlock();
            }
        }

    @Override
    public void putLast(E e) throws InterruptedException
        {
        final ReentrantLock lock = m_lock;
        lock.lock();
        try
            {
            while (!offerLast(e))
                {
                m_notFull.await();
                }
            }
        finally
            {
            lock.unlock();
            }
        }

    @Override
    public boolean offerFirst(E e, long timeout, TimeUnit unit) throws InterruptedException
        {
        long          nanos = unit.toNanos(timeout);
        ReentrantLock lock  = m_lock;
        lock.lockInterruptibly();
        try
            {
            while (!offerFirst(e))
                {
                if (nanos <= 0L)
                    {
                    return false;
                    }
                nanos = m_notFull.awaitNanos(nanos);
              }
            return true;
            }
        finally
            {
            lock.unlock();
            }
        }

    @Override
    public boolean offerLast(E e, long timeout, TimeUnit unit) throws InterruptedException
        {
        long          nanos = unit.toNanos(timeout);
        ReentrantLock lock  = m_lock;
        lock.lockInterruptibly();
        try
            {
            while (!offerLast(e))
                {
                if (nanos <= 0L)
                    {
                    return false;
                    }
                nanos = m_notFull.awaitNanos(nanos);
              }
            return true;
            }
        finally
            {
            lock.unlock();
            }
        }

    @Override
    public E takeFirst() throws InterruptedException
        {
        final ReentrantLock lock = m_lock;
        lock.lock();
        try
            {
            E x;
            while ( (x = poll()) == null)
                {
                m_notEmpty.await();
                }
            return x;
            }
        finally
            {
            lock.unlock();
            }
        }

    @Override
    public E takeLast() throws InterruptedException
        {
        final ReentrantLock lock = m_lock;
        lock.lock();
        try
            {
            E x;
            while ( (x = pollLast()) == null)
                {
                m_notEmpty.await();
                }
            return x;
            }
        finally
            {
            lock.unlock();
            }
        }

    @Override
    public E pollFirst(long timeout, TimeUnit unit) throws InterruptedException
        {
        long          nanos = unit.toNanos(timeout);
        ReentrantLock lock  = m_lock;
        lock.lockInterruptibly();
        try
            {
            E x;
            while ( (x = poll()) == null) {
                if (nanos <= 0L)
                    {
                    return null;
                    }
                nanos = m_notEmpty.awaitNanos(nanos);
                }
            return x;
            }
        finally
            {
            lock.unlock();
            }
        }

    @Override
    public E pollLast(long timeout, TimeUnit unit) throws InterruptedException
        {
        long          nanos = unit.toNanos(timeout);
        ReentrantLock lock  = m_lock;
        lock.lockInterruptibly();
        try
            {
            E x;
            while ( (x = pollLast()) == null) {
                if (nanos <= 0L)
                    {
                    return null;
                    }
                nanos = m_notEmpty.awaitNanos(nanos);
                }
            return x;
            }
        finally
            {
            lock.unlock();
            }
        }

    @Override
    public void put(E e) throws InterruptedException
        {
        putLast(e);
        }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException
        {
        return offerLast(e, timeout, unit);
        }

    @Override
    public E take() throws InterruptedException
        {
        return takeFirst();
        }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException
        {
        return pollFirst(timeout, unit);
        }

    @Override
    public int remainingCapacity()
        {
        // ToDo are we size limited, if so return something meaningful
        return Integer.MAX_VALUE;
        }

    @Override
    public int drainTo(Collection<? super E> c)
        {
        assertNotSameCollection(c, "Queue cannot be drained to the same underlying cache");

        QueuePageIterator<E> iterator = QueuePageIterator.headPolling(m_keyHead.getHash(), m_cache);
        int                  cPolled  = 0;
        while (iterator.hasNext())
            {
            cPolled++;
            c.add(iterator.next());
            }
        return cPolled;
        }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements)
        {
        assertNotSameCollection(c, "Queue cannot be drained to the same underlying cache");

        QueuePageIterator<E> iterator = QueuePageIterator.headPolling(m_keyHead.getHash(), m_cache, maxElements);
        int                  cPolled  = 0;
        while (iterator.hasNext())
            {
            cPolled++;
            c.add(iterator.next());
            }
        return cPolled;
        }

    // ----- MapListener methods --------------------------------------------

    @Override
    public void entryInserted(MapEvent<QueueKey, E> evt)
        {
        m_lock.lock();
        try
            {
            m_notEmpty.signal();
            }
        finally
            {
            m_lock.unlock();
            }
        }

    @Override
    public void entryUpdated(MapEvent<QueueKey, E> evt)
        {
        }

    @Override
    public void entryDeleted(MapEvent<QueueKey, E> evt)
        {
        m_lock.lock();
        try
            {
            m_notFull.signal();
            }
        finally
            {
            m_lock.unlock();
            }
        }

    // ----- inner class Builder --------------------------------------------

    /**
     * A {@link NamedCacheDequeBuilder} to build a blocking deque.
     */
    public static class Builder
            implements NamedCacheDequeBuilder
        {
        /**
         * Create a {@link Builder}.
         *
         * @param sNamePrefix  the prefix to add to queue cache names
         */
        public Builder(String sNamePrefix)
            {
            f_sNamePrefix = sNamePrefix == null ? "" : sNamePrefix;
            }

        @Override
        public String getCacheName(String sName)
            {
            return getCacheName(f_sNamePrefix, sName);
            }

        /**
         * Return the cache name for a queue.
         *
         * @param sPrefix  the cache name prefix
         * @param sName    the queue name
         *
         * @return  the cache name for a queue
         */
        public static String getCacheName(String sPrefix, String sName)
            {
            return sPrefix + sName;
            }

        @Override
        public String getCollectionName(String sCacheName)
            {
            if (sCacheName.startsWith(f_sNamePrefix))
                {
                return sCacheName.substring(f_sNamePrefix.length());
                }
            return sCacheName;
            }

        @Override
        public <E> NamedCacheDeque<E> build(String sName, NamedCache<QueueKey, E> cache)
            {
            return new NamedCacheBlockingDeque<>(sName, cache);
            }

        @Override
        public <E> SessionNamedDeque<E, ?> wrapForSession(Session session, NamedDeque<E> deque,
                ClassLoader loader, ValueTypeAssertion<E> typeAssertion)
            {
            NamedBlockingDeque<E> blockingDeque = (NamedBlockingDeque<E>) deque;
            return new SessionNamedBlockingDeque<>(session, blockingDeque, loader, typeAssertion);
            }

        // -----constants -------------------------------------------------------

        /**
         * The singleton instance of the default {@link Builder}.
         */
        public static Builder DEFAULT = new Builder("");

        // ----- data members ---------------------------------------------------

        /**
         * The prefix to use for cache names.
         */
        private final String f_sNamePrefix;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The main lock guarding all access.
     */
    final ReentrantLock m_lock = new ReentrantLock();

    /**
     * The Condition for waiting takes.
     */
    private final Condition m_notEmpty = m_lock.newCondition();

    /**
     * The Condition for waiting puts.
     */
    private final Condition m_notFull = m_lock.newCondition();
    }
