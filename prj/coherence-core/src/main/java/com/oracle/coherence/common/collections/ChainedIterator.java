/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.collections;


import java.util.Iterator;


/**
 * Provide an Iterator which iterates over the contents of multiple Iterators.
 *
 * @author cp  1998.08.07, 2010.12.08
 */
public class ChainedIterator<T>
        extends AbstractStableIterator<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an enumerator that will first enumerate the first Iterator
    * and then will enumerate the second Iterator as if they were together
    * a single Iterator.
    *
    * @param iterFirst   the first Iterator
    * @param iterSecond  the second Iterator
    */
    public ChainedIterator(Iterator<T> iterFirst, Iterator<T> iterSecond)
        {
        this(new Iterator[] {iterFirst, iterSecond});
        }

    /**
    * Construct an enumerator that will first enumerate the Iterators
    * passed in the array as if they were together a single enumerator.
    *
    * @param aIter  an array of Iterators
    */
    public ChainedIterator(Iterator<T>[] aIter)
        {
        m_aIter = aIter;
        }


    // ----- Iterator interface ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public T next()
        {
        T oNext = super.next();
        m_iterPrevious = m_iterCurrent;
        return oNext;
        }


    // ----- internal -------------------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    protected void advance()
        {
        Iterator<T> iter = m_iterCurrent;

        if (iter == null || !iter.hasNext())
            {
            Iterator<T>[] aIter  = m_aIter;
            int           cIters = aIter.length;
            int           iNext  = m_iNextIter;
            do
                {
                if (iNext >= cIters)
                    {
                    return;
                    }

                iter = aIter[iNext++];
                }
            while (!iter.hasNext());

            m_iNextIter   = iNext;
            m_iterCurrent = iter;
            }

        setNext(iter.next());
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected void remove(T oPrev)
        {
        Iterator<T> iter = m_iterPrevious;
        if (iter == null)
            {
            throw new IllegalStateException();
            }
        iter.remove();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The Iterators.
    */
    private final Iterator<T>[] m_aIter;

    /**
    * The next Iterator (index into m_aIter) to iterate.
    */
    private int m_iNextIter;

    /**
    * The current Iterator.
    */
    private Iterator<T> m_iterCurrent;

    /**
    * The previous Iterator.
    */
    private Iterator<T> m_iterPrevious;
    }

