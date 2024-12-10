/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.Serializable;

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;


/**
* An implementation of LongArray that stores values in an array, thus is
* actually an "IntArray". Optimized for 0-based arrays. Null values are
* not considered to be entries, thus will not affect "first" and "last"
* and will not show up in the iterator. Note: not designed to be thread
* safe.
*
* @author cp
* @version 1.00 2003.04.09
*/
public class SimpleLongArray
        extends AbstractLongArray
        implements Serializable, LongArray
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an empty SimpleLongArray.
    */
    public SimpleLongArray()
        {
        clear();
        }

    /**
     * Create a {@link SimpleLongArray} with a single entry
     * at index zero.
     *
     * @param oValue  the value to add at index zero
     * @throws NullPointerException if the value is {@code null}
     */
    public SimpleLongArray(Object oValue)
        {
        if (oValue == null)
            {
            throw new NullPointerException("value cannot be null");
            }
        m_ao     = new Object[]{oValue};
        m_iFirst = 0;
        m_iLast  = 0;
        m_cItems = 1;
        }

    // ----- LongArray interface --------------------------------------------

    /**
    * Return the value stored at the specified index.
    *
    * @param lIndex  a long index value
    *
    * @return the object stored at the specified index, or null
    */
    public Object get(long lIndex)
        {
        if (lIndex < 0L || lIndex > MAX)
            {
            throw new IndexOutOfBoundsException("illegal index: " + lIndex);
            }

        int i = (int) lIndex;
        if (i < m_iFirst || i > m_iLast)
            {
            return null;
            }

        return m_ao[i];
        }

    @Override
    public long floorIndex(long lIndex)
        {
        int i = (int) lIndex;
        if (i < m_iFirst)
            {
            return NOT_FOUND;
            }
        else if (i > m_iLast)
            {
            return m_iLast;
            }
        else
            {
            return i;
            }
        }

    @Override
    public Object floor(long lIndex)
        {
        return get(floorIndex(lIndex));
        }

    @Override
    public long ceilingIndex(long lIndex)
        {
        int i = (int) lIndex;
        if (i < m_iFirst)
            {
            return m_iFirst;
            }
        else if (i > m_iLast)
            {
            return NOT_FOUND;
            }
        else
            {
            return i;
            }
        }

    @Override
    public Object ceiling(long lIndex)
        {
        return get(ceilingIndex(lIndex));
        }

    /**
    * Add the passed item to the LongArray at the specified index.
    * <p>
    * If the index is already used, the passed value will replace the current
    * value stored with the key, and the replaced value will be returned.
    * <p>
    * It is expected that LongArray implementations will "grow" as necessary
    * to support the specified index.
    *
    * @param lIndex  a long index value
    * @param oValue  the object to store at the specified index
    *
    * @return the object that was stored at the specified index, or null
    */
    public Object set(long lIndex, Object oValue)
        {
        // this implementation does not "manage" nulls
        if (oValue == null)
            {
            return remove(lIndex);
            }

        if (lIndex < 0L || lIndex > MAX)
            {
            throw new IndexOutOfBoundsException("illegal index: " + lIndex);
            }

        int      iFirst = m_iFirst;
        int      iLast  = m_iLast;
        int      cItems = m_cItems;
        Object[] ao     = m_ao;
        int      co     = ao.length;
        int      i      = (int) lIndex;

        // ensure enough storage
        if (i >= co)
            {
            // grow at least to that 64 element block, and at least 25%
            int      coNew = Math.max(((i >>> 5) + 1) << 5, co + (co >>> 2));
            Object[] aoNew = new Object[coNew];
            if (cItems > 0)
                {
                System.arraycopy(ao, iFirst, aoNew, iFirst, iLast - iFirst + 1);
                }
            m_ao = ao = aoNew;
            }

        // get the previous value, store the new value
        Object oOrig = ao[i];
        ao[i] = oValue;

        // if it was previously null, the array size will change and the
        // first and/or last index could change
        if (oOrig == null)
            {
            if (cItems == 0)
                {
                m_iFirst = i;
                m_iLast  = i;
                m_cItems = 1;
                }
            else
                {
                if (i < iFirst)
                    {
                    m_iFirst = i;
                    }
                else if (i > iLast)
                    {
                    m_iLast = i;
                    }
                m_cItems = cItems + 1;
                }
            }

        return oOrig;
        }

    /**
    * Add the passed element value to the LongArray and return the index at
    * which the element value was stored.
    *
    * @param oValue  the object to add to the LongArray
    *
    * @return  the long index value at which the element value was stored
    */
    public long add(Object oValue)
        {
        int i = m_iLast + 1;
        set(i, oValue);
        return i;
        }

    /**
    * Determine if the specified index is in use.
    *
    * @param lIndex  a long index value
    *
    * @return true if a value (including null) is stored at the specified
    *         index, otherwise false
    */
    public boolean exists(long lIndex)
        {
        if (lIndex >= 0L && lIndex < MAX)
            {
            int i = (int) lIndex;
            if (i >= m_iFirst && i <= m_iLast)
                {
                return m_ao[i] != null;
                }
            }

        return false;
        }

    /**
    * Remove the specified index from the LongArray, returning its associated
    * value.
    *
    * @param lIndex  the index into the LongArray
    *
    * @return the associated value (which can be null) or null if the
    *         specified index is not in the LongArray
    */
    public Object remove(long lIndex)
        {
        if (lIndex < 0L || lIndex > MAX)
            {
            throw new IndexOutOfBoundsException("illegal index: " + lIndex);
            }

        // if it is out of the array bounds, then nothing to remove
        int iFirst = m_iFirst;
        int iLast  = m_iLast;
        int i      = (int) lIndex;
        if (i < iFirst || i > iLast)
            {
            return null;
            }

        // if it is null, then nothing to remove
        Object[] ao    = m_ao;
        Object   oOrig = ao[i];
        if (oOrig == null)
            {
            return null;
            }

        int cItems = m_cItems;
        if (cItems == 1)
            {
            // last item
            clear();
            }
        else
            {
            // remove the item
            ao[i]    = null;
            m_cItems = cItems - 1;

            // check to see if it affects the bounds of the array
            if (i == iFirst)
                {
                while (ao[++iFirst] == null)
                    {
                    }
                m_iFirst = iFirst;
                }
            else if (i == iLast)
                {
                while (ao[--iLast] == null)
                    {
                    }
                m_iLast = iLast;
                }
            }

        return oOrig;
        }

    /**
    * Determine if the LongArray contains the specified element.
    * <p>
    * More formally, returns <tt>true</tt> if and only if this LongArray
    * contains at least one element <tt>e</tt> such that
    * <tt>(oValue==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;oValue.equals(e))</tt>.
    *
    * @param oValue  element whose presence in this list is to be tested
    *
    * @return <tt>true</tt> if this list contains the specified element
    */
    public boolean contains(Object oValue)
        {
        // SimpleLongArray doesn't manage negative indices
        return indexOf(oValue, 0) != NOT_FOUND;
        }

    /**
    * Remove all nodes from the LongArray.
    */
    public void clear()
        {
        m_ao     = EMPTY;
        m_iFirst = -1;
        m_iLast  = -1;
        m_cItems = 0;
        }

    /**
    * Test for empty LongArray.
    *
    * @return true if LongArray has no nodes
    */
    public boolean isEmpty()
        {
        return m_cItems == 0;
        }

    /**
    * Determine the size of the LongArray.
    *
    * @return the number of nodes in the LongArray
    */
    public int getSize()
        {
        return m_cItems;
        }

    /**
    * Obtain a LongArray.Iterator of the contents of the LongArray.
    *
    * @return an instance of LongArray.Iterator
    */
    public LongArray.Iterator iterator()
        {
        return new Iterator(Math.max(0, m_iFirst), true);
        }

    /**
    * Obtain a LongArray.Iterator of the contents of the LongArray, starting
    * at a particular index such that the first call to <tt>next</tt> will
    * set the location of the iterator at the first existent index that is
    * greater than or equal to the specified index, or will throw a
    * NoSuchElementException if there is no such existent index.
    *
    * @param lIndex  the LongArray index to iterate from
    *
    * @return an instance of LongArray.Iterator
    */
    public LongArray.Iterator iterator(long lIndex)
        {
        if (lIndex < 0L || lIndex > MAX)
            {
            throw new IndexOutOfBoundsException("illegal index: " + lIndex);
            }

        return new Iterator((int) lIndex, true);
        }

    /**
    * Obtain a LongArray.Iterator of the contents of the LongArray in
    * reverse order (decreasing indices).
    *
    * @return an instance of LongArray.Iterator
    */
    public LongArray.Iterator reverseIterator()
        {
        return new Iterator(Math.max(0, m_iLast), false);
        }

    /**
    * Obtain a LongArray.Iterator of the contents of the LongArray in
    * reverse order (decreasing indices), starting at a particular
    * index such that the first call to <tt>next</tt> will set the
    * location of the iterator at the first existent index that is
    * less than or equal to the specified index, or will throw a
    * NoSuchElementException if there is no such existent index.
    *
    * @param lIndex  the LongArray index to iterate from
    *
    * @return an instance of LongArray.Iterator
    */
    public LongArray.Iterator reverseIterator(long lIndex)
        {
        if (lIndex < 0L || lIndex > MAX)
            {
            throw new IndexOutOfBoundsException("illegal index: " + lIndex);
            }

        return new Iterator((int) lIndex, false);
        }

    /**
    * Determine the first index that exists in the LongArray.
    *
    * @return the lowest long value that exists in this LongArray,
    *         or NOT_FOUND if the LongArray is empty
    */
    public long getFirstIndex()
        {
        return m_iFirst == -1 ? NOT_FOUND : (long) m_iFirst;
        }

    /**
    * Determine the last index that exists in the LongArray.
    *
    * @return the highest long value that exists in this LongArray,
    *         or NOT_FOUND if the LongArray is empty
    */
    public long getLastIndex()
        {
        return m_iLast == -1 ? NOT_FOUND : (long) m_iLast;
        }

    /**
    * Return the index in this LongArray of the first occurrence of
    * the specified element such that <tt>(index &gt;= lIndex)</tt>, or
    * NOT_FOUND if this LongArray does not contain the specified element.
    */
    public long indexOf(Object oValue, long lIndex)
        {
        if (oValue == null || m_iFirst == -1)
            {
            // this implementation does not "manage" nulls
            return NOT_FOUND;
            }

        // valid search range is [0, MAX]
        int iFirst = (int)Math.min(Math.max(0, lIndex), MAX);
        int iLast  = m_iLast;

        // search through everything
        Object[] ao = m_ao;
        for (int i = iFirst; i <= iLast; ++i)
            {
            Object oElement = ao[i];
            if (oElement != null && oValue.equals(oElement))
                {
                return i;
                }
            }
        return NOT_FOUND;
        }

    /**
    * Return the index in this LongArray of the last occurrence of
    * the specified element such that <tt>(index &lt;= lIndex)</tt>, or
    * NOT_FOUND if this LongArray does not contain the specified element.
    */
    public long lastIndexOf(Object oValue, long lIndex)
        {
        if (oValue == null || m_iFirst == -1)
            {
            // this implementation does not "manage" nulls
            return NOT_FOUND;
            }

        // valid search range is [0, MAX]
        int iFirst = m_iFirst;
        int iLast  = (int)Math.min(lIndex, MAX);

        // search through everything
        Object[] ao = m_ao;
        for (int i = iLast; i >= iFirst; --i)
            {
            Object oElement = ao[i];
            if (oElement != null && oValue.equals(oElement))
                {
                return i;
                }
            }
        return NOT_FOUND;
        }

    // ----- cloneable interface --------------------------------------------

    /**
    * Make a clone of the LongArray. The element values are not deep-cloned.
    *
    * @return a clone of this LongArray object
    */
    public SimpleLongArray clone()
        {
        SimpleLongArray that = (SimpleLongArray) super.clone();
        Object[]        ao   = that.m_ao;
        if (ao != null && ao.length != 0)
            {
            that.m_ao = (Object[]) ao.clone();
            }
        return that;
        }


    // ----- inner class: Iterator ------------------------------------------

    /**
    * An Iterator that adds a "current element" concept, similar to the
    * {@link java.util.Map.Entry} interface.
    */
    public class Iterator
            implements LongArray.Iterator
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct an iterator that will iterate over the SimpleLongArray
        * starting with the specified index.
        *
        * @param iNext  the index to start iterating from
        */
        public Iterator(int iNext, boolean fForward)
            {
            m_iNext = (fForward ?
                       Math.max(m_iFirst, iNext) :
                       Math.min(m_iLast, iNext));
            m_iPrev = -1;
            m_fForward = fForward;

            if (!exists(m_iNext))
                {
                scanNext();
                }
            }


        // ----- LongArray.Iterator interface ---------------------------

        /**
        * Returns <tt>true</tt> if the iteration has more elements. (In other
        * words, returns <tt>true</tt> if <tt>next</tt> would return an
        * element rather than throwing an exception.)
        *
        * @return <tt>true</tt> if the iterator has more elements
        */
        public boolean hasNext()
            {
            return m_iNext >= m_iFirst && m_iNext <= m_iLast && m_iNext >= 0;
            }

        /**
        * Returns the next element in the iteration.
        *
        * @return the next element in the iteration
        *
        * @exception NoSuchElementException iteration has no more elements
        */
        public Object next()
            {
            int i = m_iNext;
            if ((i > m_iLast) || (i < m_iFirst))
                {
                throw new NoSuchElementException();
                }

            Object o = get(i);
            if (o == null)
                {
                throw new ConcurrentModificationException();
                }

            m_iPrev = i;
            scanNext();
            return o;
            }

        /**
        * Returns the index of the current value, which is the value returned
        * by the most recent call to the <tt>next</tt> method.
        *
        * @exception IllegalStateException if the <tt>next</tt> method has
        *            not yet been called, or the <tt>remove</tt> method has
        *            already been called after the last call to the
        *            <tt>next</tt> method.
        */
        public long getIndex()
            {
            int i = m_iPrev;
            if (i < 0)
                {
                throw new IllegalStateException();
                }
            return i;
            }

        /**
        * Returns the current value, which is the same value returned by the
        * most recent call to the <tt>next</tt> method, or the most recent
        * value passed to <tt>setValue</tt> if <tt>setValue</tt> were called
        * after the <tt>next</tt> method.
        *
        * @return  the current value
        *
        * @exception IllegalStateException if the <tt>next</tt> method has
        *            not yet been called, or the <tt>remove</tt> method has
        *            already been called after the last call to the
        *            <tt>next</tt> method.
        */
        public Object getValue()
            {
            return get(getIndex());
            }

        /**
        * Stores a new value at the current value index, returning the value
        * that was replaced. The index of the current value is obtainable by
        * calling the <tt>getIndex</tt> method.
        *
        * @return  the replaced value
        *
        * @exception IllegalStateException if the <tt>next</tt> method has
        *            not yet been called, or the <tt>remove</tt> method has
        *            already been called after the last call to the
        *            <tt>next</tt> method.
        */
        public Object setValue(Object oValue)
            {
            Object oOrig = set(getIndex(), oValue);
            if (oValue == null)
                {
                m_iPrev = -1;
                }
            return oOrig;
            }

        /**
        * Removes from the underlying collection the last element returned by
        * the iterator (optional operation).  This method can be called only
        * once per call to <tt>next</tt>.  The behavior of an iterator is
        * unspecified if the underlying collection is modified while the
        * iteration is in progress in any way other than by calling this
        * method.
        *
        * @exception UnsupportedOperationException if the <tt>remove</tt>
        *            operation is not supported by this Iterator
        * @exception IllegalStateException if the <tt>next</tt> method has
        *            not yet been called, or the <tt>remove</tt> method has
        *            already been called after the last call to the
        *            <tt>next</tt> method.
        */
        public void remove()
            {
            SimpleLongArray.this.remove(getIndex());
            m_iPrev = -1;
            }


        // ----- internal -----------------------------------------------

        /**
        * Find the next non-null element in the array.
        */
        private void scanNext()
            {
            Object[] ao     = m_ao;
            int      iDelta = (m_fForward ? 1 : -1);
            int      i      = m_iNext;
            int      iFirst = m_iFirst;
            int      iLast  = m_iLast;

            do
                {
                i += iDelta;
                }
            while (i >= iFirst && i <= iLast && i >= 0 && ao[i] == null);

            m_iNext = i;
            }


        // ----- data members -------------------------------------------

        /**
        * Is the iteratation direction forward or backward.
        */
        private boolean m_fForward;

        /**
        * Next index to return or greater than m_iLast if no more.
        */
        private int m_iNext;

        /**
        * Previous returned object's index or -1 if n/a.
        */
        private int m_iPrev;
        }


    // ----- constants ------------------------------------------------------

    /**
    * Maximum index value.
    */
    public static final long MAX = (long) Integer.MAX_VALUE;

    /**
    * Empty array of objects.
    */
    public static final Object[] EMPTY = new Object[0];


    // ----- data members ---------------------------------------------------

    /**
    * Contents of the array.
    */
    Object[] m_ao;

    /**
    * First valid index or -1 if empty.
    */
    int m_iFirst;

    /**
    * Last valid index of -1 if empty.
    */
    int m_iLast;

    /**
    * Number of valid indexes (non-null values) in the array.
    */
    int m_cItems;
    }
