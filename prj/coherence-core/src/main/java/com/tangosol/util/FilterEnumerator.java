/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
* Provide a generic implementation of an enumerator which can enumerate
* items based on an inclusion test.
*
* @author cp  1997.09.05
* @author cp  1998.08.07
*/
public class FilterEnumerator
        extends Base
        implements Enumeration, Iterator
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the Filter enumerator based on an Enumeration.
    *
    * @param enmr  Enumeration of objects to filter
    * @param test  an inclusion test
    */
    public FilterEnumerator(Enumeration enmr, Filter test)
        {
        this(new EnumerationIterator(enmr), test);
        }

    /**
    * Construct the Filter enumerator based on an Iterator.
    *
    * @param iter  Iterator of objects to filter
    * @param test  an inclusion test
    */
    public FilterEnumerator(Iterator iter, Filter test)
        {
        m_iter  = iter;
        m_test  = test;
        m_fNext = false;
        }

    /**
    * Construct the Filter enumerator based on an array of objects.
    *
    * @param aoItem  array of objects to enumerate
    * @param test    an inclusion test
    */
    public FilterEnumerator(Object[] aoItem, Filter test)
        {
        this((Iterator) new SimpleEnumerator(aoItem), test);
        }


    // ----- Enumeration interface ------------------------------------------

    /**
    * Tests if this enumeration contains more elements.
    *
    * @return false if the enumeration has been exhausted
    */
    public boolean hasMoreElements()
        {
        return hasNext();
        }

    /**
    * Get the next element in the enumeration.
    *
    * @return the next element of this enumeration
    */
    public Object nextElement()
        {
        return next();
        }


    // ----- Iterator interface ---------------------------------------------

    /**
    * Tests if this Iterator contains more elements.
    *
    * @return true if the Iterator contains more elements, false otherwise
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
        Iterator iter = m_iter;
        Filter   test = m_test;
        while (iter.hasNext())
            {
            Object oNext = iter.next();
            if (test.evaluate(oNext))
                {
                m_oNext = oNext;
                fNext   = true;
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
    * Returns the next element of this Iterator.
    *
    * @return the next element in the Iterator
    */
    public Object next()
        {
        if (hasNext())
            {
            m_fNext = false;
            m_fPrev = true;
            return m_oNext;
            }
        else
            {
            throw new NoSuchElementException();
            }
        }

    /**
    * Remove the last-returned element that was returned by the Iterator.
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
    * Objects to filter/enumerate.
    */
    protected Iterator m_iter;

    /**
    * Test to perform on each item.
    */
    protected Filter m_test;

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
    protected Object m_oNext;
    }
