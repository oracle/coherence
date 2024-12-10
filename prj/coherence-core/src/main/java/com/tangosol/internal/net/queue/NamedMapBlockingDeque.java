/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.internal.net.NamedMapCollection;
import com.tangosol.internal.net.queue.model.QueueKey;

import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedBlockingDeque;
import com.tangosol.net.NamedMap;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.filter.AlwaysFilter;

import java.util.Collection;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link NamedBlockingDeque} implementation that wraps a {@link NamedMapDeque}.
 *
 * @param <E> the type of elements held in this queue
 */
public class NamedMapBlockingDeque<K extends QueueKey, E>
        extends WrapperNamedMapDeque<K, E>
        implements NamedBlockingDeque<E>, MapListener<K, E>
    {
    /**
     * Create a {@link NamedMapBlockingDeque} that delegates to
     * the specified {@link NamedMapDeque}.
     *
     * @param sName     the name of the deque
     * @param delegate  the {@link NamedMapDeque} to delegate to
     */
    public NamedMapBlockingDeque(String sName, NamedMapDeque<K, E> delegate)
        {
        super(sName, delegate);
        delegate.getNamedMap().addMapListener(this, AlwaysFilter.INSTANCE(), true);
        m_nHash = QueueKey.calculateQueueHash(delegate.getName());
        }

    // ----- BlockingDeque methods ------------------------------------------

    @Override
    public void release()
        {
        f_delegate.getNamedMap().removeMapListener(this);
        super.release();
        }

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

        QueuePageIterator<K, E> iterator = QueuePageIterator.headPolling(f_delegate::createKey, getNamedMap());
        int                     cPolled  = 0;
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

        QueuePageIterator<K, E> iterator = QueuePageIterator.headPolling(f_delegate::createKey, getNamedMap(), maxElements);
        int                     cPolled  = 0;
        while (iterator.hasNext())
            {
            cPolled++;
            c.add(iterator.next());
            }
        return cPolled;
        }

    // ----- MapListener methods --------------------------------------------

    @Override
    public void entryInserted(MapEvent<K, E> evt)
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
    public void entryUpdated(MapEvent<K, E> evt)
        {
        }

    @Override
    public void entryDeleted(MapEvent<K, E> evt)
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

    // ----- helper methods -------------------------------------------------

    /**
     * Obtain the underlying cache.
     *
     * @return the underlying cache
     */
    public NamedMap<K, E> getNamedMap()
        {
        return f_delegate.getNamedMap();
        }

    private void assertNotSameCollection(Collection<?> c, String sMsg)
        {
        if (c == null)
            {
            throw new NullPointerException("target collection cannot be null");
            }
        if (this == c)
            {
            throw new IllegalArgumentException(sMsg);
            }
        if (c instanceof NamedMapCollection)
            {
            NamedMap<K, E> mapThis  = f_delegate.getNamedMap();
            NamedMap<?, ?> mapOther = ((NamedMapCollection<?, ?, ?>) c).getNamedMap();
            if (mapThis.getName().equals(mapOther.getName()))
                {
                CacheService serviceThis  = mapThis.getService();
                String       sNameThis    = serviceThis.getInfo().getServiceName();
                CacheService serviceOther = mapOther.getService();
                String       sNameOther   = serviceOther.getInfo().getServiceName();
                if (sNameThis.equals(sNameOther))
                    {
                    ConfigurableCacheFactory ccfThis      = serviceThis.getBackingMapManager().getCacheFactory();
                    ConfigurableCacheFactory ccfOther     = serviceOther.getBackingMapManager().getCacheFactory();
                    if (ccfThis.equals(ccfOther))
                        {
                        throw new IllegalArgumentException(sMsg);
                        }
                    }
                }
            }
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

    /**
     * The hash of the queue.
     */
    private final int m_nHash;
    }
