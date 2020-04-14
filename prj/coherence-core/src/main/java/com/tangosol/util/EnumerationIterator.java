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
* Provide a implementation of an Iterator based on data from an Enumeration.
*
* @author Cameron Purdy
* @version 1.00, 2002.02.07
*/
public class EnumerationIterator<E>
        extends Base
        implements Iterator<E>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the Iterator based on an Enumeration.
    *
    * @param enmr  an Enumeration
    */
    public EnumerationIterator(Enumeration<E> enmr)
        {
        m_enmr = enmr;
        }


    // ----- Iterator interface ---------------------------------------------

    /**
    * Tests if this Iterator contains more elements.
    *
    * @return true if the Iterator contains more elements, false otherwise
    */
    public boolean hasNext()
        {
        return m_enmr.hasMoreElements();
        }

    /**
    * Returns the next element of this Iterator.
    *
    * @return the next element in the Iterator
    */
    public E next()
        {
        return m_enmr.nextElement();
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
    * Enumeration to pipe through the Iterator.
    */
    private Enumeration<E> m_enmr;
    }
