/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
* PagedIterator is an Iterator implementation based on a concept of a <i>page
* Advancer</i> - a pluggable component that knows how to supply a next page of
* objects to iterate through. As common to iterators, this implementation is
* not thread safe.
*
* @author gg 2008.01.25
* @since Coherence 3.4
*/
public class PagedIterator
        extends    Base
        implements Iterator
    {
    /**
    * Construct a PagedIterator based on the specified Advancer.
    *
    * @param advancer  the underlying Advancer
    */
    public PagedIterator(Advancer advancer)
        {
        azzert(advancer != null);
        m_advancer = advancer;
        }


    // ----- Iterator interface --------------------------------------------

    /**
    * Removes from the underlying collection the last element returned by the
    * iterator.
    *
    * {@inheritDoc}
    */
    public void remove()
        {
        Object oCurr = m_oCurr;
        if (oCurr == null)
            {
            throw new IllegalStateException();
            }

        try
            {
            m_advancer.remove(oCurr);
            }
        finally
            {
            m_oCurr = null;
            }
        }

    /**
    * Check whether or not the iterator has more elements.
    *
    * {@inheritDoc}
    */
    public boolean hasNext()
        {
        Iterator iter = m_iterPage;
        while (iter == null || !iter.hasNext())
            {
            Collection colPage = m_advancer.nextPage();
            if (colPage == null)
                {
                return false;
                }
            iter = m_iterPage = colPage.iterator();
            }
        return true;
        }

    /**
    * Return the next element in the iteration.
    *
    * {@inheritDoc}
    */
    public Object next()
        {
        Iterator iter = m_iterPage;
        while (iter == null || !iter.hasNext())
            {
            Collection colPage = m_advancer.nextPage();
            if (colPage == null)
                {
                throw new NoSuchElementException();
                }
            iter = m_iterPage = colPage.iterator();
            }
        return m_oCurr = iter.next();
        }


    // ----- Inner interface -----------------------------------------------

    /**
    * Advancer is a pluggable component that knows how to load a new page
    * (Collection) of objects to be used by the enclosing PagedIterator.
    */
    public interface Advancer
        {
        /**
        * Obtain a new page of objects to be used by the enclosing
        * PagedIterator.
        *
        * @return a Collection of objects or null if the Advancer is exhausted
        */
        public Collection nextPage();

        /**
        * Remove the specified object from the underlying collection. Naturally,
        * only an object from the very last non-empty page could be removed.
        *
        * @param oCurr  currently "active" item to be removed from an
        *               underlying collection
        */
        public void remove(Object oCurr);
        }


    // ----- data fields --------------------------------------------------

    /**
    * The underlying Advancer.
    */
    protected Advancer m_advancer;

    /**
    * An Iterator for the current page.
    */
    protected Iterator m_iterPage;

    /**
    * Currently "Active" object.
    */
    protected Object m_oCurr;
    }