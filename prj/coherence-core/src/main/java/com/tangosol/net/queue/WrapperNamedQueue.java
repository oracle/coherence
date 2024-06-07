/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.queue;

import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.QueueService;
import com.tangosol.util.CollectionListener;
import com.tangosol.util.Filter;
import com.tangosol.util.ObservableCollection;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A simple implementation of the {@link NamedQueue} interface built
 * as a wrapper around any {@link Queue} implementation.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class WrapperNamedQueue<E>
        implements NamedQueue<E>
    {
    /**
     * Construct a {@link WrapperNamedQueue} that wraps an empty {@link LinkedList}.
     *
     * @param sName  the queue name
     */
    public WrapperNamedQueue(String sName)
        {
        this(new LinkedList<>(), sName, null);
        }

    /**
     * Construct a {@link WrapperNamedQueue} wrapper based on the specified queue.
     *
     * @param queue  the {@link Queue} that will be wrapped by this {@link WrapperNamedQueue}
     * @param sName  the queue name (ignored if the {@code queue} is a {@link NamedQueue}
     */
    public WrapperNamedQueue(Queue<E> queue, String sName)
        {
        this(queue, sName, null);
        }

    /**
     * Construct a {@link WrapperNamedQueue} wrapper based on the specified queue.
     *
     * @param queue    the {@link Queue} that will be wrapped by this {@link WrapperNamedQueue}
     * @param sName    the queue name (ignored if the {@code queue} is a {@link NamedQueue}
     * @param service  the {@link QueueService} this queue is part of (ignored if this queue
     *                 is a {@link NamedQueue}
     */
    public WrapperNamedQueue(Queue<E> queue, String sName, QueueService service)
        {
        m_queue   = queue;
        m_sName   = queue instanceof NamedCache && sName == null ?
                    ((NamedQueue<?>) queue).getName()    : sName;
        m_service = queue instanceof NamedCache ?
                    ((NamedQueue<?>) queue).getService() : service;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Obtain the {@link Queue} this {@link WrapperNamedQueue} wraps.
     *
     * @return the {@link Queue} this {@link WrapperNamedQueue} wraps
     */
    public Queue<E> getQueue()
        {
        return m_queue;
        }

    // ----- NamedQueue methods ---------------------------------------------

    @Override
    public String getName()
        {
        return m_sName;
        }

    @Override
    public void destroy()
        {
        Queue<E> queue = getQueue();
        if (queue instanceof NamedQueue<?>)
            {
            ((NamedQueue<?>) queue).destroy();
            }
        }

    @Override
    public QueueService getService()
        {
        QueueService service = m_service;
        if (service == null)
            {
            Queue<?> queue = getQueue();
            if (queue instanceof NamedQueue<?>)
                {
                service = ((NamedQueue<?>) queue).getService();
                }
            }
        return service;
        }

    @Override
    public boolean isActive()
        {
        Queue<E> queue = getQueue();
        if (queue instanceof NamedQueue<?>)
            {
            return ((NamedQueue<?>) queue).isActive();
            }
        return true;
        }

    @Override
    public void release()
        {
        Queue<E> queue = getQueue();
        if (queue instanceof NamedQueue<?>)
            {
            ((NamedQueue<?>) queue).release();
            }
        }

    @Override
    public boolean isReleased()
        {
        Queue<E> queue = getQueue();
        if (queue instanceof NamedQueue<?>)
            {
            return ((NamedQueue<?>) queue).isReleased();
            }
        return true;
        }

    @Override
    public boolean isDestroyed()
        {
        Queue<E> queue = getQueue();
        if (queue instanceof NamedQueue<?>)
            {
            return ((NamedQueue<?>) queue).isDestroyed();
            }
        return true;
        }

    @Override
    public boolean add(E e)
        {
        return getQueue().add(e);
        }

    @Override
    public boolean offer(E e)
        {
        return getQueue().offer(e);
        }

    @Override
    public long append(E e)
        {
        return getQueue().offer(e) ? getQueue().size() : Long.MIN_VALUE;
        }

    @Override
    public E remove()
        {
        return getQueue().remove();
        }

    @Override
    public E poll()
        {
        return getQueue().poll();
        }

    @Override
    public E element()
        {
        return getQueue().element();
        }

    @Override
    public E peek()
        {
        return getQueue().peek();
        }

    @Override
    public int size()
        {
        return getQueue().size();
        }

    @Override
    public boolean isEmpty()
        {
        return getQueue().isEmpty();
        }

    @Override
    public boolean contains(Object o)
        {
        return getQueue().contains(o);
        }

    @Override
    public Iterator<E> iterator()
        {
        return getQueue().iterator();
        }

    @Override
    public Object[] toArray()
        {
        return getQueue().toArray();
        }

    @Override
    public <T> T[] toArray(T[] a)
        {
        return getQueue().toArray(a);
        }

    @Override
    public boolean remove(Object o)
        {
        return getQueue().remove(o);
        }

    @Override
    public boolean containsAll(Collection<?> c)
        {
        return getQueue().containsAll(c);
        }

    @Override
    public boolean addAll(Collection<? extends E> c)
        {
        return getQueue().addAll(c);
        }

    @Override
    public boolean removeAll(Collection<?> c)
        {
        return getQueue().removeAll(c);
        }

    @Override
    public boolean retainAll(Collection<?> c)
        {
        return getQueue().retainAll(c);
        }

    @Override
    public void clear()
        {
        getQueue().clear();
        }

    @Override
    public QueueStatistics getQueueStatistics()
        {
        Queue<E> queue = getQueue();
        if (queue instanceof NamedQueue<?>)
            {
            return ((NamedQueue<?>) queue).getQueueStatistics();
            }
        return null;
        }

    @Override
    public int getQueueNameHash()
        {
        Queue<E> queue = getQueue();
        if (queue instanceof NamedQueue<?>)
            {
            return ((NamedQueue<?>) queue).getQueueNameHash();
            }
        return QueueKey.calculateQueueHash(getName());
        }

    @Override
    public void addListener(CollectionListener<? super E> listener)
        {
        Queue<E> queue = getQueue();
        if (queue instanceof ObservableCollection)
            {
            ((ObservableCollection) queue).addListener(listener);
            }
        }

    @Override
    public void removeListener(CollectionListener<? super E> listener)
        {
        Queue<E> queue = getQueue();
        if (queue instanceof ObservableCollection)
            {
            ((ObservableCollection) queue).removeListener(listener);
            }
        }

    @Override
    public void addListener(CollectionListener<? super E> listener, Filter<E> filter, boolean fLite)
        {
        Queue<E> queue = getQueue();
        if (queue instanceof ObservableCollection)
            {
            ((ObservableCollection) queue).addListener(listener, filter, fLite);
            }
        }

    @Override
    public void removeListener(CollectionListener<? super E> listener, Filter<E> filter)
        {
        Queue<E> queue = getQueue();
        if (queue instanceof ObservableCollection)
            {
            ((ObservableCollection) queue).removeListener(listener, filter);
            }
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public int hashCode()
        {
        return getQueue().hashCode();
        }

    @Override
    public boolean equals(Object o)
        {
        return getQueue().equals(o);
        }

    @Override
    public String toString()
        {
        return "WrapperNamedQueue {" + getDescription() + "}";
        }

    // ----- helper methods -------------------------------------------------

    /**
    * Assemble a human-readable description.
    *
    * @return a description of this queue
    */
    protected String getDescription()
        {
        Queue<?> queue = getQueue();
        return "Queue {class=" + queue.getClass().getName()
                + ", size=" + queue.size();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Queue} this {@link WrapperNamedQueue} wraps.
     */
    private final Queue<E> m_queue;

    /**
     * The name of this queue.
     */
    private final String m_sName;

    /**
     * The {@link QueueService} this queue is part of.
     */
    private final QueueService m_service;
    }
