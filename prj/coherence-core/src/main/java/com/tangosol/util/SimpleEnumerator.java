/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.lang.reflect.Array;

import java.util.Collection;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.ArrayList;


/**
* Provide a generic implementation of an array enumerator.
*
* @author Cameron Purdy
* @version 0.50, 09/05/97
* @version 1.00, 08/07/98
*/
@SuppressWarnings("unchecked")
public class SimpleEnumerator<E>
        extends Base
        implements Enumeration<E>, Iterator<E>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the simple enumerator based on an array of objects.
    *
    * @param aoItem  array of objects to enumerate
    */
    public SimpleEnumerator(E[] aoItem)
        {
        // iterate forwards, don't copy the array
        this(aoItem, 0, aoItem.length, true, false);
        }

    /**
    * Construct the simple enumerator based on an array of objects.
    *
    * @param aoItem   array of objects to enumerate
    * @param ofStart  the first object position
    * @param cItems   the number of objects to enumerate
    */
    public SimpleEnumerator(E[] aoItem, int ofStart, int cItems)
        {
        // iterate forwards, don't copy the array
        this(aoItem, ofStart, cItems, true, false);
        }

    /**
    * Construct the simple enumerator based on an array of objects, making
    * a copy of the array if specified.
    *
    * @param aoItem    array of objects to enumerate
    * @param ofStart   the first object position
    * @param cItems    the number of objects to enumerate
    * @param fForward  true to iterate forwards, false to iterate from the
    *                  end backwards to the beginning
    * @param fCopy     pass true to make a copy of the array or false if the
    *                  array's contents will not change
    */
    public SimpleEnumerator(E[] aoItem, int ofStart, int cItems,
                            boolean fForward, boolean fCopy)
        {
        if (cItems < 0)
            {
            throw new IllegalArgumentException("Negative count");
            }
        if (fForward
                ? ofStart < 0 || ofStart + cItems > aoItem.length
                : ofStart >= aoItem.length || ofStart - cItems < 0)
            {
            throw new IllegalArgumentException("Off limits");
            }

        // only copy if there are at least two items in the iterator
        if (fCopy && cItems > 1)
            {
            aoItem = aoItem.clone();
            }

        m_aoItem   = aoItem;
        m_fForward = fForward;
        m_iItem    = ofStart;
        m_ofLimit  = fForward ? ofStart + cItems : ofStart - cItems;
        }

    /**
    * Construct a simple enumerator based on another Enumeration.  This forces
    * an initial pass through the elements of the passed enumerator, making
    * a copy of that data, thus capturing a "point in time" view of an
    * Enumeration which may be backed by mutable data.  In other words, this
    * constructs a "safe enumerator" based on a "not-safe enumerator".
    *
    * @param enmr  the java.util.Enumeration to enumerate
    */
    public SimpleEnumerator(Enumeration<E> enmr)
        {
        this(toArray(enmr));
        }

    /**
    * Construct a simple enumerator based on another Iterator.  This forces
    * an initial pass through the elements of the passed enumerator, making
    * a copy of that data, thus capturing a "point in time" view of an
    * Iterator which may be backed by mutable data.  In other words, this
    * constructs a "safe enumerator" based on a "not-safe enumerator".
    *
    * @param iter  the java.util.Iterator to enumerate
    */
    public SimpleEnumerator(Iterator<E> iter)
        {
        this(toArray(iter));
        }

    /**
    * Construct a simple enumerator based on a collection.
    *
    * @param col  the java.util.Collection to enumerate
    */
    public SimpleEnumerator(Collection<E> col)
        {
        this((E[]) col.toArray());
        }


    // ----- Enumeration interface ------------------------------------------

    /**
    * Tests if this enumeration contains more elements.
    *
    * @return true if the enumeration contains more elements, false otherwise
    */
    public boolean hasMoreElements()
        {
        return hasNext();
        }

    /**
    * Returns the next element of this enumeration.
    *
    * @return the next element in the enumeration
    */
    public E nextElement()
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
        return m_fForward ? m_iItem < m_ofLimit : m_iItem > m_ofLimit;
        }

    /**
    * Returns the next element of this Iterator.
    *
    * @return the next element in the Iterator
    */
    public E next()
        {
        if (!hasNext())
            {
            throw new NoSuchElementException();
            }

        try
            {
            E o = m_aoItem[m_iItem];

            if (m_fForward)
                {
                m_iItem++;
                }
            else
                {
                m_iItem--;
                }
            return o;
            }
        catch (ArrayIndexOutOfBoundsException e)
            {
            throw new NoSuchElementException();
            }
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


    // ----- Enumeration helpers --------------------------------------------

    /**
    * Turns an enumeration into an array.
    *
    * @param enmr  the enumerator
    *
    * @return an array of the objects from the enumerator
    */
    public static <T> T[] toArray(Enumeration<T> enmr)
        {
        if (!enmr.hasMoreElements())
            {
            return (T[]) NO_OBJECTS;
            }

        if (enmr instanceof SimpleEnumerator)
            {
            return (T[]) ((SimpleEnumerator) enmr).toArray();
            }

        ArrayList<T> list = new ArrayList<>();
        do
            {
            list.add(enmr.nextElement());
            }
        while (enmr.hasMoreElements());
        return (T[]) list.toArray();
        }

    /**
    * Turns an enumeration into an array. The behavior of this method differs
    * from the simple {@link #toArray(Enumeration)} method in the same way
    * that the {@link Collection#toArray(Object[])} method differs from the
    * {@link Collection#toArray()} method.
    *
    * @param enmr  the enumerator
    * @param ao    the array into which the elements of the Enumeration are
    *              to be stored, if it is big enough; otherwise, a new array
    *              of the same runtime type is allocated for this purpose
    *
    * @return an array of the objects from the enumerator
    */
    public static <T> T[] toArray(Enumeration<T> enmr, T[] ao)
        {
        if (!enmr.hasMoreElements())
            {
            if (ao.length > 0)
                {
                ao[0] = null;
                }
            return ao;
            }

        if (enmr instanceof SimpleEnumerator)
            {
            return (T[]) ((SimpleEnumerator) enmr).toArray(ao);
            }

        ArrayList<T> list = new ArrayList<>();
        do
            {
            list.add(enmr.nextElement());
            }
        while (enmr.hasMoreElements());
        return ao == null ? (T[]) list.toArray() : list.toArray(ao);
        }

    /**
    * Turns an Iterator into an array.
    *
    * @param iter  the Iterator
    *
    * @return an array of the objects
    */
    public static <T> T[] toArray(Iterator<T> iter)
        {
        if (!iter.hasNext())
            {
            return (T[]) NO_OBJECTS;
            }

        if (iter instanceof SimpleEnumerator)
            {
            return (T[]) ((SimpleEnumerator) iter).toArray();
            }

        ArrayList list = new ArrayList();
        do
            {
            list.add(iter.next());
            }
        while (iter.hasNext());
        return (T[]) list.toArray();
        }

    /**
    * Turns an Iterator into an array. The behavior of this method differs
    * from the simple {@link #toArray(Iterator)} method in the same way that
    * the {@link Collection#toArray(Object[])} method differs from the
    * {@link Collection#toArray()} method.
    *
    * @param iter  the Iterator
    * @param ao    the array into which the elements of the Iterator are
    *              to be stored, if it is big enough; otherwise, a new array
    *              of the same runtime type is allocated for this purpose
    *
    * @return an array of the objects from the Iterator
    */
    public static <T> T[] toArray(Iterator<T> iter, T[] ao)
        {
        if (!iter.hasNext())
            {
            if (ao == null)
                {
                ao = (T[]) NO_OBJECTS;
                }
            else if (ao.length > 0)
                {
                ao[0] = null;
                }
            return ao;
            }

        if (iter instanceof SimpleEnumerator)
            {
            return (T[]) ((SimpleEnumerator) iter).toArray(ao);
            }

        ArrayList<T> list = new ArrayList<>();
        do
            {
            list.add(iter.next());
            }
        while (iter.hasNext());
        return ao == null ? (T[]) list.toArray() : list.toArray(ao);
        }


    // ----- Collection-like helpers ----------------------------------------

    /**
    * Return the remaining contents of this SimpleEnumerator as an array.
    *
    * @return an array containing all of the elements in this enumerator
    */
    public Object[] toArray()
        {
        return toArray((Object[]) null);
        }

    /**
    * Return the remaining contents of this SimpleEnumerator as an array of
    * the type specified by the passed array, and use that passed array to
    * store the values if it is big enough, putting a null in the first
    * unused element if the size of the passed array is bigger than the
    * number of remaining elements in this enumerator, as per the contract
    * of the {@link Collection#toArray(Object[])} method.
    *
    * @param ao    the array into which the elements of the Enumeration are
    *              to be stored, if it is big enough; otherwise, a new array
    *              of the same runtime type is allocated for this purpose
    *
    * @return an array containing the elements of the collection
    * 
    * @throws ArrayStoreException if the runtime type of the specified array
    *         is not a supertype of the runtime type of every element in this
    *         collection
    */
    public <T> T[] toArray(T[] ao)
        {
        E[]      aoItem   = m_aoItem;
        int      iNext    = m_iItem;
        int      iLast    = m_ofLimit;
        boolean  fForward = m_fForward;

        // create the array to store the map contents
        int co = fForward ? iLast - iNext : iNext - iLast;
        if (ao == null)
            {
            // implied Object[] type, see toArray()
            ao = (T[]) new Object[co];
            }
        else if (ao.length < co)
            {
            // if it is not big enough, a new array of the same runtime
            // type is allocated
            ao = (T[]) Array.newInstance(ao.getClass().getComponentType(), co);
            }
        else if (ao.length > co)
            {
            // if the collection fits in the specified array with room to
            // spare, the element in the array immediately following the
            // end of the collection is set to null
            ao[co] = null;
            }

        if (fForward)
            {
            System.arraycopy(aoItem, iNext, ao, 0, co);
            }
        else
            {
            int i = 0;
            while (iNext > iLast)
                {
                ao[i++] = (T) aoItem[iNext--];
                }
            }

        // set the enumerator to "finished"
        m_iItem = iLast;

        return ao;
        }


    // ----- constants ------------------------------------------------------

    /**
    * An array of no items.
    */
    private static final Object[] NO_OBJECTS = new Object[0];


    // ----- data members ---------------------------------------------------

    /**
    * Array of items to enumerate.
    */
    protected E[] m_aoItem;

    /**
    * Iterator position:  next item to return.
    */
    protected int m_iItem;

    /**
    * Iterator end position (beyond last).
    */
    protected int m_ofLimit;

    /**
    * Iterator direction.
    */
    protected boolean m_fForward;
    }
