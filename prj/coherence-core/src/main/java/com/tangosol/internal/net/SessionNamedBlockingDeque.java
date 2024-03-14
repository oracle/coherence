/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net;

import com.tangosol.net.NamedBlockingDeque;
import com.tangosol.net.Session;
import com.tangosol.net.ValueTypeAssertion;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * A wrapper around a {@link Session} and a {@link NamedBlockingDeque}.
 *
 * @param <E>  the type of elements stored in the queue
 */
public class SessionNamedBlockingDeque<E>
        extends SessionNamedDeque<E, NamedBlockingDeque<E>>
        implements NamedBlockingDeque<E>
    {
    /**
     * Create a {@link SessionNamedBlockingDeque}.
     *
     * @param session        the owning {@link Session}
     * @param deque          the {@link NamedBlockingDeque} to wrap
     * @param loader         the owning class loader
     * @param typeAssertion  the type assertion
     */
    public SessionNamedBlockingDeque(Session session, NamedBlockingDeque<E> deque,
            ClassLoader loader, ValueTypeAssertion<E> typeAssertion)
        {
        super(session, deque, loader, typeAssertion);
        }

    @Override
    public void putFirst(E e) throws InterruptedException
        {
        f_deque.putFirst(e);
        }

    @Override
    public void putLast(E e) throws InterruptedException
        {
        f_deque.putLast(e);
        }

    @Override
    public boolean offerFirst(E e, long timeout, TimeUnit unit) throws InterruptedException
        {
        return f_deque.offerFirst(e, timeout, unit);
        }

    @Override
    public boolean offerLast(E e, long timeout, TimeUnit unit) throws InterruptedException
        {
        return f_deque.offerLast(e, timeout, unit);
        }

    @Override
    public E takeFirst() throws InterruptedException
        {
        return f_deque.takeFirst();
        }

    @Override
    public E takeLast() throws InterruptedException
        {
        return f_deque.takeLast();
        }

    @Override
    public E pollFirst(long timeout, TimeUnit unit) throws InterruptedException
        {
        return f_deque.pollFirst(timeout, unit);
        }

    @Override
    public E pollLast(long timeout, TimeUnit unit) throws InterruptedException
        {
        return f_deque.pollLast(timeout, unit);
        }

    @Override
    public void put(E e) throws InterruptedException
        {
        f_deque.put(e);
        }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException
        {
        return f_deque.offer(e, timeout, unit);
        }

    @Override
    public E take() throws InterruptedException
        {
        return f_deque.take();
        }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException
        {
        return f_deque.poll(timeout, unit);
        }

    @Override
    public int remainingCapacity()
        {
        return f_deque.remainingCapacity();
        }

    @Override
    public int drainTo(Collection<? super E> c)
        {
        return f_deque.drainTo(c);
        }

    @Override
    public int drainTo(Collection<? super E> c, int maxElements)
        {
        return f_deque.drainTo(c, maxElements);
        }
    }
