/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.Serializable;

import java.util.Iterator;
import java.util.Enumeration;
import java.util.NoSuchElementException;


/**
* Represents a double-ended queue (dequeue) of objects.  A dequeue allows
* items which have been removed from the front of the queue to be put back
* on the front of the queue.
*
* @version 1.00, 12/05/96
* @author 	Cameron Purdy
*/
public class Dequeue
        extends Base
        implements Enumeration, Iterator, Cloneable, Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Constructs a dequeue.
    */
    public Dequeue()
        {
        removeAllElements();
        }


    // ----- Enumeration interface ------------------------------------------

    /**
    * Determines if the Dequeue contains more elements.
    *
    * @return true if Dequeue contains more elements, false otherwise
    */
    public boolean hasMoreElements()
        {
        return !isEmpty();
        }

    /**
    * Returns the next element from the Dequeue.  Calls to this method will
    * enumerate successive elements.
    *
    * @return the next element
    *
    * @exception NoSuchElementException If no more elements exist.
    */
    public Object nextElement() throws NoSuchElementException
        {
        if (isEmpty())
            {
            throw new NoSuchElementException();
            }

        // get the next element
        m_iFront = next(m_iFront);
        Object o = m_aoItem[m_iFront];

        // release the reference to the next element (allows it to be gc'd)
        m_aoItem[m_iFront] = null;

        return o;
        }


    // ----- Iterator interface ---------------------------------------------

    /**
    * Tests if this Iterator contains more elements.
    *
    * @return true if the Iterator contains more elements, false otherwise
    */
    public boolean hasNext()
        {
        return !isEmpty();
        }

    /**
    * Returns the next element of this Iterator.
    *
    * @return the next element in the Iterator
    */
    public Object next()
        {
        return nextElement();
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


    // ----- Dequeue interface ----------------------------------------------

    /**
    * Adds an element to the dequeue.  The passed object is placed at the end
    * of the dequeue.
    *
    * @param o object to add to the Dequeue
    */
    public void addElement(Object o)
        {
        if (isFull())
            {
            grow();
            }

        m_iBack = next(m_iBack);
        m_aoItem[m_iBack] = o;
        }

    /**
    * Returns the most recently returned element to the Dequeue.  After calling
    * this method, a call to nextElement will return the element that was
    * returned by the most recent call to nextElement.  This method can be
    * called multiple times to put back multiple elements.
    *
    * @param o  the object
    *
    * @exception NoSuchElementException If the element that is being put back
    *            is no longer in the Dequeue.
    */
    public void putBackElement(Object o)
        {
        if (isFull())
            {
            grow();
            }

        m_aoItem[m_iFront] = o;
        m_iFront = prev(m_iFront);
        }

    /**
    * Removes all items from the dequeue.
    */
    public void removeAllElements()
        {
        m_aoItem = new Object [DEFAULT_MAX_ELEMENTS];
        m_iFront = 0;
        m_iBack  = 0;
        }

    /**
    * Determine the current capacity of this dequeue.
    *
    * @return the current capacity of this dequeue
    */
    public int capacity()
        {
        return m_aoItem.length - 1;
        }

    /**
    * Determine the current number of objects in this dequeue.
    *
    * @return the current number of objects in this dequeue
    */
    public int size()
        {
        if (m_iFront <= m_iBack)
            {
            return m_iBack - m_iFront;
            }
        else
            {
            return m_aoItem.length - (m_iFront - m_iBack);
            }
        }

    /**
    * Determines if the dequeue is empty.
    *
    * @return true if the dequeue is empty, false otherwise
    */
    public boolean isEmpty()
        {
        return (m_iFront == m_iBack);
        }

    /**
    * Determines if the dequeue is full.  This method is mainly of use
    * internally, since the dequeue auto-resizes when additional items are
    * added.
    *
    * @return true if the dequeue is full, false otherwise
    */
    public boolean isFull()
        {
        return (m_iFront == next(m_iBack));
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Creates a clone of the dequeue.
    *
    * @return the clone
    */
    public Object clone()
        {
        try
            {
            Dequeue that = (Dequeue) super.clone();
            that.m_aoItem = (Object[]) that.m_aoItem.clone();
            return that;
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }


    // ----- internal methods -----------------------------------------------

    /**
    * Determines the index following the passed index for storing or
    * accessing an item from the m_ao storage.
    *
    * @param i  the index
    *
    * @return the next index
    */
    protected int next(int i)
        {
        return (++i == m_aoItem.length ? 0 : i);
        }

    /**
    * Determines the index preceding the passed index for storing or
    * accessing an item from the m_ao storage.
    *
    * @param i  the index
    *
    * @return the previous index
    */
    protected int prev(int i)
        {
        return (i == 0 ? m_aoItem.length - 1 : --i);
        }

    /**
    * Increase the capacity of the Dequeue.
    */
    protected void grow()
        {
        // grow by 50%, but by 32 minimum or 256 maximum
        int cOld = m_aoItem.length;
        int cNew = cOld + Math.max(Math.min(cOld >> 1, 256), 32);

        // create new array and copy existing elements to it
        Object [] aoNew = new Object [cNew];
        if (m_iFront < m_iBack)
            {
            System.arraycopy(m_aoItem, m_iFront, aoNew, m_iFront, m_iBack - m_iFront + 1);
            }
        else if (m_iFront > m_iBack)
            {
            System.arraycopy(m_aoItem, 0, aoNew, 0, m_iBack + 1);
            System.arraycopy(m_aoItem, m_iFront, aoNew, cNew - (cOld - m_iFront), cOld - m_iFront);
            m_iFront += cNew - cOld;
            }

        m_aoItem = aoNew;
        }


    // ----- constants ------------------------------------------------------

    /**
    * Default dequeue size.
    */
    private static final int DEFAULT_MAX_ELEMENTS   = 32;


    // ----- data members ---------------------------------------------------

    /**
    * The storage for the objects in the dequeue.
    */
    Object[] m_aoItem;

    /**
    * The front of the queue, which is the next element to return.
    */
    int m_iFront;

    /**
    * The back of the queue, which is the last element that can be returned.
    */
    int m_iBack;
    }
