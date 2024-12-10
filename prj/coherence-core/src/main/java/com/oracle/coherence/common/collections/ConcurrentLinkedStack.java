/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;


/**
 * ConcurrentLinkedStack is a low-contention unbounded Stack implementation.
 * <p>
 * Unlike the java.util.concurrent collections, the ConcurrentLinkedStack supports <tt>null</tt> elements, and also
 * has a constant time {@link #size} implementation.
 * </p>
 *
 * @param <V>  the type of the values that will be stored in the Stack
 *
 * @author mf/cp 2013.03.22
 */
public class ConcurrentLinkedStack<V>
        implements Stack<V>, Iterable<V>
    {
    // ----- Stack interface ------------------------------------------------

    @Override
    public void push(V value)
        {
        Element<V> next;
        Element<V> element = new Element<V>(value);
        do
            {
            element.m_next    = next = f_refHead.get();
            element.m_cHeight = next.m_cHeight + 1;
            }
        while (!f_refHead.compareAndSet(next, element));
        }

    @Override
    public V pop()
        {
        Element<V> head;
        do
            {
            head = f_refHead.get();
            }
        while (head != TAIL && !f_refHead.compareAndSet(head, head.m_next));

        head.m_next = null; // avoid infecting stack via tenured garbage

        return head.f_value;
        }

    @Override
    public V peek()
        {
        return f_refHead.get().f_value;
        }

    @Override
    public boolean isEmpty()
        {
        return f_refHead.get() == TAIL;
        }

    @Override
    public int size()
        {
        return f_refHead.get().m_cHeight;
        }

    // ----- Iterable interface ---------------------------------------------

    @Override
    /**
     * Return a read-only iterator over the Stack. The iterator creates a
     * "free" (no cost) snapshot of the stack.
     * <p>
     * This iterator does not support {@link Iterator#remove}.
     *
     * @return an iterator over the stack
     */
    public Iterator<V> iterator()
        {
        return new Iterator<V>()
            {
            @Override
            public boolean hasNext()
                {
                return m_next != TAIL;
                }

            @Override
            public V next()
                {
                Element<V> current = m_next;
                if (current == TAIL)
                    {
                    throw new NoSuchElementException();
                    }

                m_next = current.m_next;
                return current.f_value;
                }

            @Override
            public void remove()
                {
                // despite being technically re-linkable, this would mean that Node.m_next needed to be CAS able and
                // would open up a number of issues with the overall implementation
                throw new UnsupportedOperationException();
                }

            private Element<V> m_next = f_refHead.get();
            };
        }

    // ----- inner class: Node ----------------------------------------------

    /**
     * Node is a holder for an element within the stack.
     *
     * @param <V>  the type of the value
     */
    static final class Element<V>
        {
        /**
         * Construct a Element to hold a value.
         *
         * @param value  the value
         */
        Element(V value)
            {
            f_value = value;
            }

        /**
         * Construct the "tail" Element, which is always the last Element in the stack.
         */
        Element()
            {
            f_value   = null;
            m_next    = this;
            m_cHeight = 0;
            }

        /**
         * The value stored in this Element.
         */
        protected final V f_value;

        /**
         * The height of the stack from this Element down.
         * <p>
         * This field is effectively final after being pushed into the stack.
         */
        private int m_cHeight;

        /**
         * The next Element down in the stack
         * <p>
         * This field is effectively final after being pushed into the stack.
         */
        private Element<V> m_next;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The bottom of all stacks. ("It's <tt>TAIL</tt>s the whole way down.")
     */
    protected static final Element TAIL = new Element();

    /**
     * Reference to the top of the stack.
     */
    protected final AtomicReference<Element<V>> f_refHead = new AtomicReference<Element<V>>(TAIL);
    }
