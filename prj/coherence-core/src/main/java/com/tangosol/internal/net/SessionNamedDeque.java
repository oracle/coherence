/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import com.tangosol.net.NamedDeque;
import com.tangosol.net.QueueService;
import com.tangosol.net.Session;
import com.tangosol.net.ValueTypeAssertion;

import com.tangosol.net.queue.QueueStatistics;
import com.tangosol.util.Base;
import com.tangosol.util.CollectionListener;
import com.tangosol.util.Filter;
import com.tangosol.util.HashHelper;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

/**
 * A {@link Session}-based implementation of {@link NamedDeque}, that delegates
 * requests onto an internal {@link NamedDeque} and isolates developer provided
 * resources so that when a {@link Session} is closed the resources are released.
 *
 * @see Session
 */
@SuppressWarnings("PatternVariableCanBeUsed")
public class SessionNamedDeque<E, Q extends NamedDeque<E>>
        implements NamedDeque<E>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link SessionNamedDeque}.
     *
     * @param session  the {@link ConfigurableCacheFactorySession} that produced this {@link SessionNamedDeque}
     * @param deque          the {@link NamedDeque} to which requests will be delegated
     * @param loader         the {@link ClassLoader} associated with the deque
     * @param typeAssertion  the {@link ValueTypeAssertion} for the NamedDeque
     */
    public SessionNamedDeque(Session session, Q deque,
            ClassLoader loader, ValueTypeAssertion<E> typeAssertion)
        {
        f_deque         = Objects.requireNonNull(deque);
        f_session       = Objects.requireNonNull(session);
        f_typeAssertion = typeAssertion;
        f_loader        = Objects.requireNonNull(loader);
        m_fActive       = f_deque.isActive();
        }

    // ----- SessionNamedDeque methods --------------------------------------

    /**
     * Obtain the wrapped {@link NamedDeque}.
     *
     * @return  the wrapped {@link NamedDeque}
     */
    public NamedDeque<E> getInternalNamedDeque()
        {
        return f_deque;
        }

    /**
     * Obtain the {@link ValueTypeAssertion} to use to
     * assert the type of deque values.
     *
     * @return the {@link ValueTypeAssertion} to use to
     * assert the type of deque values
     */
    @SuppressWarnings("unchecked")
    ValueTypeAssertion<E> getTypeAssertion()
        {
        return f_typeAssertion;
        }

    /**
     * Perform any pre-close actions.
     */
    void onClosing()
        {
        m_fActive = false;
        }

    /**
     * Perform any post-close actions.
     */
    void onClosed()
        {
        }

    /**
     * Perform any pre-destroy actions.
     */
    void onDestroying()
        {
        m_fActive = false;
        }

    /**
     * Perform any post-destroy actions.
     */
    void onDestroyed()
        {
        }

    // ----- NamedDeque methods ---------------------------------------------

    @Override
    public void release()
        {
        if (m_fActive)
            {
            // closing a NamedDeque is always delegated to the Session to manage
            f_session.close(this);
            }
        }

    @Override
    public void destroy()
        {
        if (m_fActive)
            {
            // destroying a NamedQueue is always delegated to the Session to manage
            f_session.destroy(this);
            }
        }

    @Override
    public QueueStatistics getQueueStatistics()
        {
        return f_deque.getQueueStatistics();
        }

    @Override
    public void addFirst(E e)
        {
        f_deque.addFirst(e);
        }

    @Override
    public void addLast(E e)
        {
        f_deque.addLast(e);
        }

    @Override
    public boolean offerFirst(E e)
        {
        return f_deque.offerFirst(e);
        }

    @Override
    public boolean offerLast(E e)
        {
        return f_deque.offerLast(e);
        }

    @Override
    public E removeFirst()
        {
        return f_deque.removeFirst();
        }

    @Override
    public E removeLast()
        {
        return f_deque.removeLast();
        }

    @Override
    public E pollFirst()
        {
        return f_deque.pollFirst();
        }

    @Override
    public E pollLast()
        {
        return f_deque.pollLast();
        }

    @Override
    public E getFirst()
        {
        return f_deque.getFirst();
        }

    @Override
    public E getLast()
        {
        return f_deque.getLast();
        }

    @Override
    public E peekFirst()
        {
        return f_deque.peekFirst();
        }

    @Override
    public E peekLast()
        {
        return f_deque.peekLast();
        }

    @Override
    public boolean removeFirstOccurrence(Object o)
        {
        return f_deque.removeFirstOccurrence(o);
        }

    @Override
    public boolean removeLastOccurrence(Object o)
        {
        return f_deque.removeLastOccurrence(o);
        }

    @Override
    public void push(E e)
        {
        f_deque.push(e);
        }

    @Override
    public E pop()
        {
        return f_deque.pop();
        }

    @Override
    public Iterator<E> descendingIterator()
        {
        return f_deque.descendingIterator();
        }

    @Override
    public boolean add(E e)
        {
        return f_deque.add(e);
        }

    @Override
    public boolean offer(E e)
        {
        return f_deque.offer(e);
        }

    @Override
    public E remove()
        {
        return f_deque.remove();
        }

    @Override
    public E poll()
        {
        return f_deque.poll();
        }

    @Override
    public E element()
        {
        return f_deque.element();
        }

    @Override
    public E peek()
        {
        return f_deque.peek();
        }

    @Override
    public int size()
        {
        return f_deque.size();
        }

    @Override
    public boolean isEmpty()
        {
        return f_deque.isEmpty();
        }

    @Override
    public boolean contains(Object o)
        {
        return f_deque.contains(o);
        }

    @Override
    public Iterator<E> iterator()
        {
        return f_deque.iterator();
        }

    @Override
    public Object[] toArray()
        {
        return f_deque.toArray();
        }

    @Override
    public <T> T[] toArray(T[] a)
        {
        return f_deque.toArray(a);
        }

    @Override
    public boolean remove(Object o)
        {
        return f_deque.remove(o);
        }

    @Override
    public boolean containsAll(Collection<?> c)
        {
        return f_deque.containsAll(c);
        }

    @Override
    public boolean addAll(Collection<? extends E> c)
        {
        return f_deque.addAll(c);
        }

    @Override
    public boolean removeAll(Collection<?> c)
        {
        return f_deque.removeAll(c);
        }

    @Override
    public boolean retainAll(Collection<?> c)
        {
        return f_deque.retainAll(c);
        }

    @Override
    public void clear()
        {
        f_deque.clear();
        }

    @Override
    public String getName()
        {
        return f_deque.getName();
        }

    @Override
    public QueueService getService()
        {
        return f_deque.getService();
        }

    @Override
    public boolean isActive()
        {
        return m_fActive && f_deque.isActive();
        }

    @Override
    public boolean isDestroyed()
        {
        return f_deque.isDestroyed();
        }

    @Override
    public boolean isReleased()
        {
        return f_deque.isReleased();
        }

    @Override
    public int getQueueNameHash()
        {
        return f_deque.getQueueNameHash();
        }

    /**
     * Return the {@link ClassLoader} used by the wrapped deque.
     *
     * @return the {@link ClassLoader} used by the wrapped deque
     */
    ClassLoader getContextClassLoader()
        {
        return f_loader;
        }

    @Override
    public void addListener(CollectionListener<? super E> listener)
        {
        f_deque.addListener(listener);
        }

    @Override
    public void removeListener(CollectionListener<? super E> listener)
        {
        f_deque.removeListener(listener);
        }

    @Override
    public void addListener(CollectionListener<? super E> listener, Filter<E> filter, boolean fLite)
        {
        f_deque.addListener(listener, filter, fLite);
        }

    @Override
    public void removeListener(CollectionListener<? super E> listener, Filter<E> filter)
        {
        f_deque.removeListener(listener, filter);
        }

    @Override
    public long prepend(E e)
        {
        return f_deque.prepend(e);
        }

    @Override
    public long append(E e)
        {
        return f_deque.append(e);
        }

    // ----- object methods -------------------------------------------------

    @Override
    @SuppressWarnings("rawtypes")
    public boolean equals(Object obj)
        {
        if (obj instanceof SessionNamedDeque)
            {
            SessionNamedDeque sessionOther = (SessionNamedDeque) obj;

            return Base.equals(f_session, sessionOther.f_session)
                    && Base.equals(f_deque, sessionOther.f_deque)
                    && f_loader.equals(sessionOther.f_loader);
            }

        return false;
        }

    @Override
    public int hashCode()
        {
        int hash = HashHelper.hash(f_session, 31);

        return HashHelper.hash(f_deque, hash) + f_loader.hashCode();
        }

    @Override
    public String toString()
        {
        return f_deque.toString();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The wrapped {@link NamedDeque}.
     */
    protected final Q f_deque;

    /**
     * The {@link ClassLoader} associated with this session's deque.
     */
    private final ClassLoader f_loader;

    /**
     * The {@link ConfigurableCacheFactorySession} used to create the deque.
     */
    private final Session f_session;

    /**
     * The {@link ValueTypeAssertion} used to assert the topic value types.
     */
    @SuppressWarnings("rawtypes")
    private final ValueTypeAssertion f_typeAssertion;

    /**
     * A flag indicating whether the deque is active.
     */
    private volatile boolean m_fActive;
    }
