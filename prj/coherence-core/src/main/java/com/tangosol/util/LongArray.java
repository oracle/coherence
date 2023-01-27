/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.Serializable;

import java.util.Spliterators;

import java.util.stream.StreamSupport;


/**
* An interface, similar in its methods to List, and similar in its purpose
* to a Java array, designed for sparse storage and indexed by long values.
* <p>
* Unlike the List interface, the LongArray interface assumes that every
* index can be accessed and has storage available.
*
* @author cp
* @version 1.00 2002.04.24  based on WindowedArray component and SparseArray
*                           prototype
*/
public interface LongArray<V>
        extends Cloneable, Serializable, Iterable<V>
    {
    /**
    * Return the value stored at the specified index.
    *
    * @param lIndex  a long index value
    *
    * @return the object stored at the specified index, or null
    */
    public V get(long lIndex);

    /**
    * Return the "first" index which is less than or equal to the specified index.
    *
    * @param lIndex  the index
    *
    * @return the index or NOT_FOUND
    */
    public long floorIndex(long lIndex);

    /**
    * Return the "first" value with an index which is less than or equal to the specified index.
    *
    * @param lIndex  the index
    *
    * @return the value or null
    */
    public V floor(long lIndex);

    /**
    * Return the "first" index which is greater than or equal to the specified index.
    *
    * @param lIndex  the index
    *
    * @return the index or NOT_FOUND
    */
    public long ceilingIndex(long lIndex);

    /**
    * Return the "first" value with an index which is greater than or equal to the specified index.
    *
    * @param lIndex  the index
    *
    * @return the value or null
    */
    public V ceiling(long lIndex);

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
    public V set(long lIndex, V oValue);

    /**
    * Add the passed element value to the LongArray and return the index at
    * which the element value was stored.
    *
    * @param oValue  the object to add to the LongArray
    *
    * @return  the long index value at which the element value was stored
    */
    public long add(V oValue);

    /**
    * Determine if the specified index is in use.
    *
    * @param lIndex  a long index value
    *
    * @return true if a value (including null) is stored at the specified
    *         index, otherwise false
    */
    public boolean exists(long lIndex);

    /**
    * Remove the specified index from the LongArray, returning its associated
    * value.
    *
    * @param lIndex  the index into the LongArray
    *
    * @return the associated value (which can be null) or null if the
    *         specified index is not in the LongArray
    */
    public V remove(long lIndex);

    /**
     * Remove all nodes in the specified range.
     *
     * @param lIndexFrom  the floor index
     * @param lIndexTo    the ceiling index (exclusive)
     */
    public void remove(long lIndexFrom, long lIndexTo);

    /**
    * Determine if the LongArray contains the specified element.
    * <p>
    * More formally, returns <tt>true</tt> if and only if this LongArray
    * contains at least one element <tt>e</tt> such that
    * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
    *
    * @param oValue  element whose presence in this list is to be tested
    *
    * @return <tt>true</tt> if this list contains the specified element
    */
    public boolean contains(V oValue);

    /**
    * Remove all nodes from the LongArray.
    */
    public void clear();

    /**
    * Test for empty LongArray.
    *
    * @return true if LongArray has no nodes
    */
    public boolean isEmpty();

    /**
    * Determine the size of the LongArray.
    *
    * @return the number of nodes in the LongArray
    */
    public int getSize();

    /**
    * Obtain a LongArray.Iterator of the contents of the LongArray in
    * order of increasing indices.
    *
    * @return an instance of LongArray.Iterator
    */
    public Iterator<V> iterator();

    /**
    * Obtain a LongArray.Iterator of the contents of the LongArray in
    * order of increasing indices, starting at a particular index such
    * that the first call to <tt>next</tt> will set the location of
    * the iterator at the first existent index that is greater than or
    * equal to the specified index, or will throw a
    * NoSuchElementException if there is no such existent index.
    *
    * @param lIndex  the LongArray index to iterate from
    *
    * @return an instance of LongArray.Iterator
    */
    public Iterator<V> iterator(long lIndex);

    /**
    * Obtain a LongArray.Iterator of the contents of the LongArray in
    * reverse order (decreasing indices).
    *
    * @return an instance of LongArray.Iterator
    */
    public Iterator<V> reverseIterator();

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
    public Iterator<V> reverseIterator(long lIndex);

    /**
    * Determine the first index that exists in the LongArray.
    *
    * @return the lowest long value that exists in this LongArray, or
    *         NOT_FOUND if the LongArray is empty
    */
    public long getFirstIndex();

    /**
    * Determine the last index that exists in the LongArray.
    *
    * @return the highest long value that exists in this LongArray,
    *         or NOT_FOUND if the LongArray is empty
    */
    public long getLastIndex();

    /**
     * Return the index in this LongArray of the first occurrence of
     * the specified element, or NOT_FOUND if this LongArray does not
     * contain the specified element.
     *
     * @param oValue  the object to find index for
     *
     * @return the index of the specified object in the LongArray
     */
    public long indexOf(V oValue);

    /**
     * Return the index in this LongArray of the first occurrence of
     * the specified element such that <tt>(index greater or equal to lIndex)</tt>, or
     * NOT_FOUND if this LongArray does not contain the specified element.
     *
     * @param oValue  the object to find the index for
     * @param lIndex  the index to compare to
     *
     * @return the index of the specified object in the LongArray that is greater
     *         or equal to the specified index
     */
    public long indexOf(V oValue, long lIndex);

    /**
     * Return the index in this LongArray of the last occurrence of the
     * specified element, or NOT_FOUND if this LongArray does not
     * contain the specified element.
     *
     * @param oValue  the object to find the index for
     *
     * @return the index of the last occurrence of the specified object in
     *         the LongArray
     */
    public long lastIndexOf(V oValue);

    /**
     * Return the index in this LongArray of the last occurrence of
     * the specified element such that <tt>(index less then or equal to lIndex)</tt>, or
     * NOT_FOUND if this LongArray does not contain the specified element.
     *
     * @param oValue  the object to find the index for
     * @param lIndex  the index to compare to
     *
     * @return the index of the specified object in this LongArray that is
     *         less or equal to the specified index
     */
    public long lastIndexOf(V oValue, long lIndex);

    /**
     * Return an array of the indices into this LongArray.
     *
     * @return an array of the indices into this LongArray
     *
     * @since 12.2.1.4
     */
    public default long[] keys()
        {
        Iterator<V> iterator = iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false)
                .mapToLong(o -> iterator.getIndex())
                .toArray();
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Provide a string representation of the LongArray.
    *
    * @return a human-readable String value describing the LongArray instance
    */
    public String toString();

    /**
    * Test for LongArray equality.
    *
    * @param o  an Object to compare to this LongArray for equality
    *
    * @return true if the passed Object is a LongArray containing the same
    *         indexes and whose elements at those indexes are equal
    */
    public boolean equals(Object o);


    // ----- cloneable interface --------------------------------------------

    /**
    * Make a clone of the LongArray. The element values are not deep-cloned.
    *
    * @return a clone of this LongArray object
    */
    public LongArray<V> clone();

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings("unchecked")
    static <V> LongArray<V> singleton(V oValue)
        {
        return oValue == null
                ? NullImplementation.getLongArray()
                : new SimpleLongArray(oValue);
        }

    // ----- LongArray.Iterator interface -----------------------------------

    /**
    * An Iterator that adds a "current element" concept, similar to the
    * {@link java.util.Map.Entry} interface.
    */
    public interface Iterator<V>
            extends java.util.Iterator<V>
        {
        /**
        * Returns <tt>true</tt> if the iteration has more elements. (In other
        * words, returns <tt>true</tt> if <tt>next</tt> would return an
        * element rather than throwing an exception.)
        *
        * @return <tt>true</tt> if the iterator has more elements
        */
        public boolean hasNext();

        /**
        * Returns the next element in the iteration.
        *
        * @return the next element in the iteration
        *
        * @exception java.util.NoSuchElementException iteration has no
        *           more elements
        */
        public V next();

        /**
         * Returns the index of the current value, which is the value returned
         * by the most recent call to the <tt>next</tt> method.
         *
         * @exception IllegalStateException if the <tt>next</tt> method has
         *            not yet been called, or the <tt>remove</tt> method has
         *            already been called after the last call to the
         *            <tt>next</tt> method.
         *
         * @return the index of the current value
         */
        public long getIndex();

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
        public V getValue();

        /**
         * Stores a new value at the current value index, returning the value
         * that was replaced. The index of the current value is obtainable by
         * calling the <tt>getIndex</tt> method.
         *
         * @param oValue  the new value to store
         *
         * @return the replaced value
         *
         * @exception IllegalStateException if the <tt>next</tt> method has
         *            not yet been called, or the <tt>remove</tt> method has
         *            already been called after the last call to the
         *            <tt>next</tt> method.
         */
        public V setValue(V oValue);

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
        public void remove();
        }

    // ----- constants ------------------------------------------------------

    /**
    * This index is used to indicate that an element was not found.
    */
    public static final long NOT_FOUND = -1;
    }
