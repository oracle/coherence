/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.queue;

import com.tangosol.net.NamedDeque;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.QueueService;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

public class WrapperNamedDeque<E>
        extends WrapperNamedQueue<E>
        implements NamedDeque<E>
    {
    /**
     * Construct a {@link WrapperNamedDeque} that wraps an empty {@link LinkedList}.
     *
     * @param sName  the deque name
     */
    public WrapperNamedDeque(String sName)
        {
        this(new LinkedList<>(), sName);
        }

    /**
     * Construct a {@link WrapperNamedDeque} wrapper based on the specified deque.
     *
     * @param deque  the {@link Deque} that will be wrapped by this {@link WrapperNamedDeque}
     * @param sName  the deque name
     */
    public WrapperNamedDeque(Deque<E> deque, String sName)
        {
        this(deque, sName, null);
        }

    /**
     * Construct a {@link WrapperNamedDeque} wrapper based on the specified deque.
     *
     * @param deque    the {@link Deque} that will be wrapped by this {@link WrapperNamedDeque}
     * @param sName    the deque name (ignored if the {@code deque} is a {@link NamedDeque}
     * @param service  the {@link QueueService} this deque is part of (ignored if this deque
     *                 is a {@link NamedDeque}
     */
    public WrapperNamedDeque(Deque<E> deque, String sName, QueueService service)
        {
        super(deque, sName, service);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Obtain the {@link Deque} this {@link WrapperNamedDeque} wraps.
     *
     * @return the {@link Deque} this {@link WrapperNamedDeque} wraps
     */
    public Deque<E> getDeque()
        {
        return (Deque<E>) getQueue();
        }

    // ----- Deque methods --------------------------------------------------

    @Override
    public void addFirst(E e)
        {
        getDeque().addFirst(e);
        }

    @Override
    public void addLast(E e)
        {
        getDeque().addLast(e);
        }

    @Override
    public boolean offerFirst(E e)
        {
        return getDeque().offerFirst(e);
        }

    @Override
    public boolean offerLast(E e)
        {
        return getDeque().offerLast(e);
        }

    @Override
    public E removeFirst()
        {
        return getDeque().removeFirst();
        }

    @Override
    public E removeLast()
        {
        return getDeque().removeLast();
        }

    @Override
    public E pollFirst()
        {
        return getDeque().pollFirst();
        }

    @Override
    public E pollLast()
        {
        return getDeque().pollLast();
        }

    @Override
    public E getFirst()
        {
        return getDeque().getFirst();
        }

    @Override
    public E getLast()
        {
        return getDeque().getLast();
        }

    @Override
    public E peekFirst()
        {
        return getDeque().peekFirst();
        }

    @Override
    public E peekLast()
        {
        return getDeque().peekLast();
        }

    @Override
    public boolean removeFirstOccurrence(Object o)
        {
        return getDeque().removeFirstOccurrence(o);
        }

    @Override
    public boolean removeLastOccurrence(Object o)
        {
        return getDeque().removeLastOccurrence(o);
        }

    @Override
    public void push(E e)
        {
        getDeque().push(e);
        }

    @Override
    public E pop()
        {
        return getDeque().pop();
        }

    @Override
    public Iterator<E> descendingIterator()
        {
        return getDeque().descendingIterator();
        }

    @Override
    public long prepend(E e, long cMillis)
        {
        Deque<E> deque = getDeque();
        if (cMillis == NamedQueue.EXPIRY_DEFAULT)
            {
            return deque.offerFirst(e) ?  0L : Long.MIN_VALUE;
            }
        else if (deque instanceof NamedDeque<E>)
            {
            return ((NamedDeque<E>) deque).prepend(e, cMillis);
            }
        throw new UnsupportedOperationException("prepend with expiry is not supported in deque type "
                + deque.getClass().getName());
        }

    @Override
    public void addFirst(E e, long cMillis)
        {
        Deque<E> deque = getDeque();
        if (cMillis == NamedQueue.EXPIRY_DEFAULT)
            {
            deque.addFirst(e);
            }
        else if (deque instanceof NamedDeque<E>)
            {
            ((NamedDeque<E>) deque).addFirst(e, cMillis);
            }
        throw new UnsupportedOperationException("addFirst with expiry is not supported in deque type "
                + deque.getClass().getName());
        }

    @Override
    public void addLast(E e, long cMillis)
        {
        Deque<E> deque = getDeque();
        if (cMillis == NamedQueue.EXPIRY_DEFAULT)
            {
            deque.addLast(e);
            }
        else if (deque instanceof NamedDeque<E>)
            {
            ((NamedDeque<E>) deque).addLast(e, cMillis);
            }
        throw new UnsupportedOperationException("addLast with expiry is not supported in deque type "
                + deque.getClass().getName());
        }

    @Override
    public boolean offerFirst(E e, long cMillis)
        {
        Deque<E> deque = getDeque();
        if (cMillis == NamedQueue.EXPIRY_DEFAULT)
            {
            return deque.offerFirst(e);
            }
        else if (deque instanceof NamedDeque<E>)
            {
            return ((NamedDeque<E>) deque).offerFirst(e, cMillis);
            }
        throw new UnsupportedOperationException("offerFirst with expiry is not supported in deque type "
                + deque.getClass().getName());
        }

    @Override
    public boolean offerLast(E e, long cMillis)
        {
        Deque<E> deque = getDeque();
        if (cMillis == NamedQueue.EXPIRY_DEFAULT)
            {
            return deque.offerLast(e);
            }
        else if (deque instanceof NamedDeque<E>)
            {
            return ((NamedDeque<E>) deque).offerLast(e, cMillis);
            }
        throw new UnsupportedOperationException("offerLast with expiry is not supported in deque type "
                + deque.getClass().getName());
        }

    @Override
    public void push(E e, long cMillis)
        {
        Deque<E> deque = getDeque();
        if (cMillis == NamedQueue.EXPIRY_DEFAULT)
            {
            deque.push(e);
            }
        else if (deque instanceof NamedDeque<E>)
            {
            ((NamedDeque<E>) deque).push(e, cMillis);
            }
        throw new UnsupportedOperationException("push with expiry is not supported in deque type "
                + deque.getClass().getName());
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "WrapperNamedDeque {" + getDescription() + "}";
        }
    }
