/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.Iterator;
import java.util.Enumeration;


/**
* Provide a implementation of an enumerator based on data from an Iterator.
* <p>
* This has two main uses:
* <ol>
* <li>  Turn Iterator interface into an Enumeration interface
* <li>  Make an Iterator immutable
* </ol>
*
* @author Cameron Purdy
* @version 1.00, 03/23/99
*/
public class IteratorEnumerator
        extends Base
        implements Enumeration, Iterator
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the enumerator based on an Iterator.
    *
    * @param iter  an Iterator
    */
    public IteratorEnumerator(Iterator iter)
        {
        m_iter = iter;
        }


    // ----- Enumeration interface ------------------------------------------

    /**
    * Tests if this enumeration contains more elements.
    *
    * @return true if the enumeration contains more elements, false otherwise
    */
    public boolean hasMoreElements()
        {
        return m_iter.hasNext();
        }

    /**
    * Returns the next element of this enumeration.
    *
    * @return the next element in the enumeration
    */
    public Object nextElement()
        {
        return m_iter.next();
        }


    // ----- Iterator interface ---------------------------------------------

    /**
    * Tests if this Iterator contains more elements.
    *
    * @return true if the Iterator contains more elements, false otherwise
    */
    public boolean hasNext()
        {
        return m_iter.hasNext();
        }

    /**
    * Returns the next element of this Iterator.
    *
    * @return the next element in the Iterator
    */
    public Object next()
        {
        return m_iter.next();
        }

    /**
    * Remove the last-returned element that was returned by the Iterator.
    * This method always throws UnsupportedOperationException because the
    * Iterator is immutable.
    */
    public void remove()
        {
        throw new UnsupportedOperationException("iterator is immutable");
        }


    // ----- data members ---------------------------------------------------

    /**
    * Iterator to pipe through the enumerator.
    */
    private Iterator m_iter;
    }
