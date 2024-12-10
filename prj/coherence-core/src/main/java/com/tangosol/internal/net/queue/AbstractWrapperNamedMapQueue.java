/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.QueueService;
import com.tangosol.net.queue.QueueStatistics;
import com.tangosol.util.CollectionListener;
import com.tangosol.util.Filter;

import java.util.Collection;
import java.util.Iterator;

/**
 * A base class for wrappers around a {@link NamedMapQueue}.
 *
 * @param <E>  the type of element stored in the queue
 * @param <K>  the type of the underlying cache key
 * @param <D>  the type of the wrapped {@link NamedMapQueue}
 */
public abstract class AbstractWrapperNamedMapQueue<K extends QueueKey, E, D extends NamedMapQueue<? extends K, E>>
        implements NamedQueue<E>
    {
    /**
     * Create a {@link AbstractWrapperNamedMapQueue}.
     *
     * @param delegate  the {@link NamedMapQueue} to wrap
     */
    protected AbstractWrapperNamedMapQueue(D delegate)
        {
        this(null, delegate);
        }

    /**
     * Create a {@link AbstractWrapperNamedMapQueue}.
     *
     * @param sName     the name of this queue
     * @param delegate  the {@link NamedMapQueue} to wrap
     */
    protected AbstractWrapperNamedMapQueue(String sName, D delegate)
        {
        f_sName    = sName == null || sName.isBlank() ? null : sName;
        f_delegate = delegate;
        }

    /**
     * Return the wrapped {@link NamedMapQueue}.
     *
     * @return the wrapped {@link NamedMapQueue}
     */
    public D getDelegate()
        {
        return f_delegate;
        }

    @Override
    public String getName()
        {
        return f_sName;
        }

    @Override
    public QueueService getService()
        {
        return f_delegate.getService();
        }

    @Override
    public void destroy()
        {
        f_delegate.destroy();
        }

    @Override
    public boolean isActive()
        {
        return f_delegate.isActive();
        }

    @Override
    public boolean isReleased()
        {
        return f_delegate.isReleased();
        }

    @Override
    public boolean isDestroyed()
        {
        return f_delegate.isDestroyed();
        }

    @Override
    public boolean isReady()
        {
        return f_delegate.isReady();
        }

    @Override
    public void release()
        {
        f_delegate.release();
        }

    @Override
    public QueueStatistics getQueueStatistics()
        {
        return f_delegate.getQueueStatistics();
        }

    @Override
    public int getQueueNameHash()
        {
        return f_delegate.getQueueNameHash();
        }

    @Override
    public long append(E e)
        {
        return f_delegate.append(e);
        }

    @Override
    public void addListener(CollectionListener<? super E> listener)
        {
        f_delegate.addListener(listener);
        }

    @Override
    public void removeListener(CollectionListener<? super E> listener)
        {
        f_delegate.removeListener(listener);
        }

    @Override
    public void addListener(CollectionListener<? super E> listener, Filter<E> filter, boolean fLite)
        {
        f_delegate.addListener(listener, filter, fLite);
        }

    @Override
    public void removeListener(CollectionListener<? super E> listener, Filter<E> filter)
        {
        f_delegate.removeListener(listener, filter);
        }

    @Override
    public boolean offer(E e)
        {
        return f_delegate.offer(e);
        }

    @Override
    public E remove()
        {
        return f_delegate.remove();
        }

    @Override
    public E poll()
        {
        return f_delegate.poll();
        }

    @Override
    public E element()
        {
        return f_delegate.element();
        }

    @Override
    public E peek()
        {
        return f_delegate.peek();
        }

    @Override
    public int size()
        {
        return f_delegate.size();
        }

    @Override
    public boolean isEmpty()
        {
        return f_delegate.isEmpty();
        }

    @Override
    public boolean contains(Object o)
        {
        return f_delegate.contains(o);
        }

    @Override
    public Iterator<E> iterator()
        {
        return f_delegate.iterator();
        }

    @Override
    public Object[] toArray()
        {
        return f_delegate.toArray();
        }

    @Override
    public <T> T[] toArray(T[] a)
        {
        return f_delegate.toArray(a);
        }

    @Override
    public boolean add(E e)
        {
        return f_delegate.add(e);
        }

    @Override
    public boolean remove(Object o)
        {
        return f_delegate.remove(o);
        }

    @Override
    public boolean containsAll(Collection<?> c)
        {
        return f_delegate.containsAll(c);
        }

    @Override
    public boolean addAll(Collection<? extends E> c)
        {
        return f_delegate.addAll(c);
        }

    @Override
    public boolean removeAll(Collection<?> c)
        {
        return f_delegate.removeAll(c);
        }

    @Override
    public boolean retainAll(Collection<?> c)
        {
        return f_delegate.retainAll(c);
        }

    @Override
    public void clear()
        {
        f_delegate.clear();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The wrapped {@link NamedMapQueue}.
     */
    protected final D f_delegate;

    /**
     * The name of this queue.
     */
    protected final String f_sName;
    }
