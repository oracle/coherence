/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.net.NamedDeque;

import java.util.Iterator;

public class WrapperNamedCacheDeque<E>
        extends AbstractWrapperNamedCacheQueue<E, QueueKey, NamedCacheDeque<E>>
        implements NamedDeque<E>
    {
    public WrapperNamedCacheDeque(NamedCacheDeque<E> delegate)
        {
        super(null, delegate);
        }

    public WrapperNamedCacheDeque(String sName, NamedCacheDeque<E> delegate)
        {
        super(sName, delegate);
        }

    public NamedCacheDeque<E> getDelegate()
        {
        return f_delegate;
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
    }
