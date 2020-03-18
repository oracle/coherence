/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.collections;


import com.oracle.coherence.common.base.Predicate;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * A generic implementation of an Iterator which iterates items based on an
 * inclusion test.
 *
 * @param <T> the type of iterated items
 *
 * @author cp  1997.09.05
 */
public class PredicateIterator<T>
        implements Iterator<T>
    {
    /**
     * Construct a {@link PredicateIterator} based on the specified Iterator
     * and the Predicate.
     *
     * @param iter  the Iterator of objects to filter
     * @param test  the inclusion test
     */
    public PredicateIterator(Iterator<T> iter, Predicate<T> test)
        {
        m_iter  = iter;
        m_test  = test;
        m_fNext = false;
        }


    // ----- Iterator interface ---------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean hasNext()
        {
        // check if we've already check for the "next one"
        boolean fNext = m_fNext;
        if (fNext)
            {
            return true;
            }

        // find if there is a "next one"
        Iterator<T>  iter = m_iter;
        Predicate<T> test = m_test;

        while (iter.hasNext())
            {
            T next = iter.next();
            if (test.evaluate(next))
                {
                m_next = next;
                fNext  = true;
                break;
                }
            }

        // can't call remove now (because we'd end up potentially
        // removing the wrong one
        m_fPrev = false;
        m_fNext = fNext;

        return fNext;
        }

    /**
     * {@inheritDoc}
     */
    public T next()
        {
        if (hasNext())
            {
            m_fNext = false;
            m_fPrev = true;
            return m_next;
            }
        else
            {
            throw new NoSuchElementException();
            }
        }

    /**
     * {@inheritDoc}
     */
    public void remove()
        {
        if (m_fPrev)
            {
            m_fPrev = false;
            m_iter.remove();
            }
        else
            {
            throw new IllegalStateException();
            }
        }


    // ----- data members ---------------------------------------------------

    /**
     * Iterator to filter.
     */
    protected Iterator<T> m_iter;

    /**
     * Test to perform on each item.
     */
    protected Predicate<T> m_test;

    /**
     * Is there a next item which passed the test?
     */
    protected boolean m_fNext;

    /**
     * Is there a previous item which passed the test and can be removed?
     */
    protected boolean m_fPrev;

    /**
     * The next item which passed the test.
     */
    protected T m_next;
    }
