/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor.util;

import com.tangosol.util.function.Remote.Predicate;

import java.util.Iterator;

import java.util.NoSuchElementException;

/**
 * A {@link FilteringIterator} that filters elements in another {@link Iterator} based on a specified
 * {@link Predicate}.
 *
 * @param <T>  the type being iterated
 *
 * @author bo
 * @since 21.12
 */
public class FilteringIterator<T>
        implements Iterator<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link FilteringIterator}.
     *
     * @param iterator   the {@link Iterator} of elements to filter
     * @param predicate  the {@link Predicate} that must be satisfied
     */
    public FilteringIterator(Iterator<T> iterator,
                             Predicate<? super T> predicate)
        {
        f_iterator  = iterator;
        f_predicate = predicate;
        m_element   = null;
        }

    // ----- Iterator interface ---------------------------------------------

    /**
     * Obtains the next element from the {@link Iterator} that satisfies the {@link Predicate}.
     *
     * @return the next element satisfying the {@link Predicate} or
     *         <code>null</code> if no such element exists
     */
    T findNext()
        {
        T element = null;

        while (f_iterator != null && f_iterator.hasNext() && element == null)
            {
            element = f_iterator.next();

            if (!f_predicate.test(element))
                {
                element = null;
                }
            }

        return element;
        }

    @Override
    public boolean hasNext()
        {
        if (m_element == null)
            {
            m_element = findNext();
            }

        return m_element != null;
        }

    @Override
    public T next()
        {
        if (m_element == null)
            {
            m_element = findNext();
            }

        if (m_element == null)
            {
            throw new NoSuchElementException("No further elements satisfy " + f_predicate);
            }
        else
            {
            T next = m_element;

            m_element = findNext();

            return next;
            }
        }

    @Override
    public void remove()
        {
        throw new UnsupportedOperationException("FilteringIterator doesn't support removal");
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying {@link Iterator} of elements to be filtered.
     */
    protected final Iterator<T> f_iterator;

    /**
     * The {@link Predicate} that must be satisfied for an element to be returned by the {@link Iterator}.
     */
    protected final Predicate<? super T> f_predicate;

    /**
     * The next element to return from the {@link Iterator} that matched the {@link Predicate}.
     */
    protected T m_element;
    }
