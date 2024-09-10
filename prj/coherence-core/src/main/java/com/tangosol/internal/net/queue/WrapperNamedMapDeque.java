/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.net.NamedDeque;
import com.tangosol.net.NamedMap;

import java.util.Iterator;

/**
 * A {@link NamedDeque} implementation that wraps a {@link NamedMapDeque}.
 *
 * @param <K>  the type of the underlying cache key
 * @param <E>  the type of the elements stored in the queue
 */
public class WrapperNamedMapDeque<K extends QueueKey, E>
        extends AbstractWrapperNamedMapQueue<QueueKey, E, NamedMapDeque<K, E>>
        implements NamedDeque<E>
    {
    /**
     * Create a {@link WrapperNamedMapDeque}.
     *
     * @param delegate  the {@link NamedMapDeque} to delegate to
     */
    public WrapperNamedMapDeque(NamedMapDeque<K, E> delegate)
        {
        super(null, delegate);
        }

    /**
     * Create a {@link WrapperNamedMapDeque}.
     *
     * @param sName     the name of this queue
     * @param delegate  the {@link NamedMapDeque} to delegate to
     */
    public WrapperNamedMapDeque(String sName, NamedMapDeque<K, E> delegate)
        {
        super(sName, delegate);
        }

    @Override
    public long prepend(E e)
        {
        return f_delegate.prepend(e);
        }

    @Override
    public void addFirst(E e)
        {
        f_delegate.addFirst(e);
        }

    @Override
    public void addLast(E e)
        {
        f_delegate.addLast(e);
        }

    @Override
    public boolean offerFirst(E e)
        {
        return f_delegate.offerFirst(e);
        }

    @Override
    public boolean offerLast(E e)
        {
        return f_delegate.offerLast(e);
        }

    @Override
    public E removeFirst()
        {
        return f_delegate.removeFirst();
        }

    @Override
    public E removeLast()
        {
        return f_delegate.removeLast();
        }

    @Override
    public E pollFirst()
        {
        return f_delegate.pollFirst();
        }

    @Override
    public E pollLast()
        {
        return f_delegate.pollLast();
        }

    @Override
    public E getFirst()
        {
        return f_delegate.getFirst();
        }

    @Override
    public E getLast()
        {
        return f_delegate.getLast();
        }

    @Override
    public E peekFirst()
        {
        return f_delegate.peekFirst();
        }

    @Override
    public E peekLast()
        {
        return f_delegate.peekLast();
        }

    @Override
    public boolean removeFirstOccurrence(Object o)
        {
        return f_delegate.removeFirstOccurrence(o);
        }

    @Override
    public boolean removeLastOccurrence(Object o)
        {
        return f_delegate.removeLastOccurrence(o);
        }

    @Override
    public void push(E e)
        {
        f_delegate.push(e);
        }

    @Override
    public E pop()
        {
        return f_delegate.pop();
        }

    @Override
    public Iterator<E> descendingIterator()
        {
        return f_delegate.descendingIterator();
        }

    /**
     * Obtain the underlying {@link NamedMap}.
     *
     * @return the underlying {@link NamedMap}
     */
    public NamedMap<K, E> getNamedMap()
        {
        return f_delegate.getNamedMap();
        }
    }
