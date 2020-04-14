/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;


/**
* Provide an enumerator which enumerates the contents of multiple
* enumerators.
*
* @author cp  1998.08.07
*/
public class ChainedEnumerator
        extends Base
        implements Enumeration, Iterator
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an enumerator that will first enumerate multiple enumerators.
    */
    public ChainedEnumerator()
        {
        }

    /**
    * Construct an enumerator that will first enumerate the passed
    * Enumeration.
    *
    * @param enmrFirst  the first Enumeration
    */
    public ChainedEnumerator(Enumeration enmrFirst)
        {
        addEnumeration(enmrFirst);
        }

    /**
    * Construct an enumerator that will first enumerate the passed
    * Iterator.
    *
    * @param iterator  the first Iterator
    */
    public ChainedEnumerator(Iterator iterator)
        {
        addIterator(iterator);
        }

    /**
    * Construct an enumerator that will first enumerate the first Enumeration
    * and then will enumerate the second Enumeration as if they were together
    * a single Enumeration.
    *
    * @param enmrFirst   the first Enumeration
    * @param enmrSecond  the second Enumeration
    */
    public ChainedEnumerator(Enumeration enmrFirst, Enumeration enmrSecond)
        {
        addEnumeration(enmrFirst);
        addEnumeration(enmrSecond);
        }

    /**
    * Construct an enumerator that will first enumerate the first Iterator
    * and then will enumerate the second Iterator as if they were together
    * a single Iterator.
    *
    * @param iterFirst   the first Iterator
    * @param iterSecond  the second Iterator
    */
    public ChainedEnumerator(Iterator iterFirst, Iterator iterSecond)
        {
        addIterator(iterFirst);
        addIterator(iterSecond);
        }

    /**
    * Construct an enumerator that will first enumerate the Enumerations
    * passed in the array as if they were together a single enumerator.
    *
    * @param aEnum  an array of Enumerations
    */
    public ChainedEnumerator(Enumeration[] aEnum)
        {
        for (int i = 0, c = aEnum.length; i < c; ++i)
            {
            addEnumeration(aEnum[i]);
            }
        }

    /**
    * Construct an enumerator that will first enumerate the Iterators
    * passed in the array as if they were together a single enumerator.
    *
    * @param aIterator  an array of Iterators
    */
    public ChainedEnumerator(Iterator[] aIterator)
        {
        for (int i = 0, c = aIterator.length; i < c; ++i)
            {
            addIterator(aIterator[i]);
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Add the Enumeration to the end of the chain.
    *
    * @param enmr  an Enumeration
    */
    public void addEnumeration(Enumeration enmr)
        {
        if (enmr != null)
            {
            m_listIter.add(enmr instanceof Iterator ? (Iterator) enmr
                                                    : new EnumerationIterator(enmr));
            }
        }

    /**
    * Add the Iterator to the end of the chain.
    *
    * @param iterator  an Iterator
    */
    public void addIterator(Iterator iterator)
        {
        if (iterator != null)
            {
            m_listIter.add(iterator);
            }
        }


    // ----- Enumeration interface ------------------------------------------

    /**
    * Tests if this enumeration contains more elements.
    *
    * @return true if the enumeration contains more elements, false otherwise
    */
    public boolean hasMoreElements()
        {
        return getIterator().hasNext();
        }

    /**
    * Returns the next element of this enumeration.
    *
    * @return the next element in the enumeration
    */
    public Object nextElement()
        {
        return getIterator().next();
        }


    // ----- Iterator interface ---------------------------------------------

    /**
    * Tests if this Iterator contains more elements.
    *
    * @return true if the Iterator contains more elements, false otherwise
    */
    public boolean hasNext()
        {
        return getIterator().hasNext();
        }

    /**
    * Returns the next element of this Iterator.
    *
    * @return the next element in the Iterator
    */
    public Object next()
        {
        return getIterator().next();
        }

    /**
    * Remove the last-returned element that was returned by the Iterator.
    * This method always throws UnsupportedOperationException because the
    * Iterator is immutable.
    */
    public void remove()
        {
        Iterator iter = getRecentIterator();
        if (iter == null)
            {
            throw new IllegalStateException();
            }
        iter.remove();
        }


    // ----- internal -------------------------------------------------------

    /**
    * Get the current or next enumeration in the list
    *
    * @return the current enumeration.
    */
    protected Iterator getIterator()
        {
        Iterator iter = m_iterCurrent;

        if (iter == null || !iter.hasNext())
            {
            int iIter = m_iNextIter;
            int cIter = m_listIter.size();
            do
                {
                if (iIter >= cIter)
                    {
                    iter = NullImplementation.getIterator();
                    break;
                    }

                iter = (Iterator) m_listIter.get(iIter++);
                }
            while (!iter.hasNext());

            m_iNextIter   = iIter;
            m_iterCurrent = iter;
            }

        return iter;
        }

    /**
    * Get the recently used iterator, if any.
    *
    * @return the iterator that was most recently used
    */
    protected Iterator getRecentIterator()
        {
        return m_iterCurrent;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The Iterators.
    */
    protected List m_listIter = new ArrayList();

    /**
    * The next Iterator (index into the m_listIter) to iterate.
    */
    protected int m_iNextIter;

    /**
    * The current Iterator.
    */
    protected Iterator m_iterCurrent;
    }
