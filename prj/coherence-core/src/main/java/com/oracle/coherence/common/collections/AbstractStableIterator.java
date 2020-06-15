/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.collections;


import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * An abstract Iterator implementation that is stable between the
 * {@link #hasNext} and {@link #next} methods, and between the {@link #next}
 * and {@link #remove()} methods.
 *
 * @author cp  2003.05.24, 2010.12.09
 */
public abstract class AbstractStableIterator<T>
        implements Iterator<T>, Enumeration<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public AbstractStableIterator()
        {
        }


    // ----- Iterator interface ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean hasNext()
        {
        if (m_fNextReady)
            {
            return true;
            }

        advance();
        return m_fNextReady;
        }

    /**
    * {@inheritDoc}
    */
    public T next()
        {
        if (!m_fNextReady)
            {
            advance();
            if (!m_fNextReady)
                {
                throw new NoSuchElementException();
                }
            }

        T oNext = m_oNext;

        m_fNextReady = false;
        m_fCanDelete = true;
        m_oPrev      = oNext;

        return oNext;
        }

    /**
    * {@inheritDoc}
    */
    public void remove()
        {
        if (m_fCanDelete)
            {
            m_fCanDelete = false;
            remove(m_oPrev);
            }
        else
            {
            throw new IllegalStateException();
            }
        }


    // ----- Enumeration interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean hasMoreElements()
        {
        return hasNext();
        }

    /**
    * {@inheritDoc}
    */
    public T nextElement()
        {
        return next();
        }


    // ----- internal -------------------------------------------------------

    /**
    * Obtain the previous object provided by the Iterator.
    *
    * @return the object previously returned from a call to {@link #next}
    */
    protected T getPrevious()
        {
        return m_oPrev;
        }

    /**
    * Specify the next object to provide from the Iterator.
    *
    * @param oNext  the next object to provide from the Iterator
    */
    protected void setNext(T oNext)
        {
        m_oNext      = oNext;
        m_fNextReady = true;
        }

    /**
    * Advance to the next object.
    * <p>
    * This method must be implemented by the concrete sub-class by calling
    * {@link #setNext} if there is a next object.
    */
    protected abstract void advance();

    /**
    * Remove the specified item.
    * <p>
    * This is an optional operation. If the Iterator supports element
    * removal, then it should implement this method, which is delegated to by
    * the {@link #remove()} method.
    *
    * @param oPrev  the previously iterated object that should be removed
    *
    * @throws UnsupportedOperationException if removal is not supported
    */
    protected void remove(T oPrev)
        {
        throw new UnsupportedOperationException();
        }


    // ----- data members ---------------------------------------------------

    /**
    * Set to true when <tt>m_oNext</tt> is the next object to return
    * from the iterator. If there is no next object, or if the next object
    * is not determined yet, then this will be false. Set up by
    * {@link #setNext} and reset by {@link #next}.
    */
    private boolean m_fNextReady;

    /**
    * The next object to return from this iterator.  Set up by
    * {@link #setNext} and reset by {@link #next}.
    */
    private T m_oNext;

    /**
    * Set to true when the <tt>m_oPrev</tt> object has been returned
    * but not yet removed. Set up by {@link #next} and reset by
    * {@link #remove()}.
    */
    private boolean m_fCanDelete;

    /**
    * The object that can be deleted, if any. Set up by {@link #next}.
    */
    private T m_oPrev;
    }
