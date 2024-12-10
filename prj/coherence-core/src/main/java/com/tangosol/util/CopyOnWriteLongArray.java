/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* A thread-safe variant of {@link LongArray} in which all mutating operations
* (e.g. <tt>add</tt>, <tt>set</tt>) are implemented by making a fresh copy of
* the underlying array.
* <p>
* Iterators over this LongArray are guaranteed to produce a safe-iteration and
* not to throw <tt>ConcurrentModificationException</tt>. The iterator will not
* reflect concurrent additions, removals, or changes to this array.  Mutating
* operations on iterators themselves (e.g. <tt>remove</tt>, <tt>setValue</tt>)
* are not supported (and will throw <tt>UnsupportedOperationException</tt>).
* <p>
* Note: mutations on this LongArray are costly, but may be <em>more</em>
* efficient than alternatives when traversal operations vastly outnumber
* mutations.  All mutating operations are synchronized, so concurrent mutation
* can be prevented by holding synchronization on this object.
* <p>
* This data-structure is not suitable to frequent updates, or updates on large
* data-sets see {@link ReadHeavyLongArray} for a more suitable alternative for
* such cases.
*
* @since  Coherence 3.7
* @author rhl 2010.09.09
*/
public class CopyOnWriteLongArray<V>
        extends WrapperCollections.AbstractWrapperLongArray<V>
    {
    // ----- Constructors -------------------------------------------------

    /**
    * Default constructor.
    */
    public CopyOnWriteLongArray()
        {
        setDelegate(EMPTY_ARRAY);
        }

    /**
    * Construct a CopyOnWriteLongArray with an underlying array of the specified
    * type.
    *
    * @param clazz  the type of the internal array; must implement LongArray
    *               and have a public no-arg constructor
    *
    * @throws ClassCastException      if the specified type does not implement
    *                                 LongArray
    * @throws IllegalAccessException  if the class or its no-arg constructor is
    *                                 not accessible
    * @throws InstantiationException  if the specified Class represents an
    *                                 abstract class or interface; or if the
    *                                 class does not have a no-arg constructor;
    *                                 or if the instantiation fails for some
    *                                 other reason.
    */
    public CopyOnWriteLongArray(Class<? extends LongArray<V>> clazz)
            throws IllegalAccessException, InstantiationException
        {
        if (!LongArray.class.isAssignableFrom(clazz))
            {
            throw new ClassCastException(clazz.getName());
            }

        setDelegate(clazz.newInstance());
        }

    /**
    * Construct a CopyOnWriteLongArray, initialized with the contents of the
    * specified LongArray.
    *
    * @param array  the initial LongArray
    */
    public CopyOnWriteLongArray(LongArray<V> array)
        {
        // copy the passed array to insure that it cannot be modified directly
        setDelegate(copyArray(array));
        }


    // ----- accessors ----------------------------------------------------

    /**
    * Return the internal LongArray.
    *
    * @return the internal LongArray
    */
    protected LongArray<V> delegate()
        {
        return m_arrayInternal;
        }

    /**
    * Set the internal LongArray.
    *
    * @param array  the new internal LongArray
    */
    protected void setDelegate(LongArray<V> array)
        {
        m_arrayInternal = array;
        }


    // ----- helpers ------------------------------------------------------

    /**
    * Return a shallow copy of the specified LongArray.
    *
    * @param array  the array to be copied
    *
    * @return a shallow copy of the specified LongArray
    */
    public LongArray<V> copyArray(LongArray<V> array)
        {
        return array.clone();
        }


    // ----- LongArray methods --------------------------------------------

    /**
    * {@inheritDoc}
    */
    public synchronized V set(long lIndex, V oValue)
        {
        LongArray<V> arrayNew  = copyArray(delegate());
        V            oValueOld = arrayNew.set(lIndex, oValue);
        setDelegate(arrayNew);

        return oValueOld;
        }

    /**
    * {@inheritDoc}
    */
    public synchronized long add(V oValue)
        {
        LongArray<V> arrayNew = copyArray(delegate());
        long         lIndex   = arrayNew.add(oValue);
        setDelegate(arrayNew);

        return lIndex;
        }

    /**
    * {@inheritDoc}
    */
    public synchronized V remove(long lIndex)
        {
        LongArray<V> arrayNew  = copyArray(delegate());
        V            oValueOld = arrayNew.remove(lIndex);
        setDelegate(arrayNew);

        return oValueOld;
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void remove(long lIndexFrom, long lIndexTo)
        {
        LongArray<V> arrayNew = copyArray(delegate());

        arrayNew.remove(lIndexFrom, lIndexTo);
        setDelegate(arrayNew);
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void clear()
        {
        LongArray<V> arrayNew = copyArray(delegate());
        arrayNew.clear();
        setDelegate(arrayNew);
        }

    /**
    * {@inheritDoc}
    */
    public Iterator<V> iterator()
        {
        return instantiateUnmodifiableIterator(delegate().iterator());
        }

    /**
    * {@inheritDoc}
    */
    public Iterator<V> iterator(long lIndex)
        {
        return instantiateUnmodifiableIterator(delegate().iterator(lIndex));
        }

    /**
    * {@inheritDoc}
    */
    public Iterator<V> reverseIterator()
        {
        return instantiateUnmodifiableIterator(delegate().reverseIterator());
        }

    /**
    * {@inheritDoc}
    */
    public Iterator<V> reverseIterator(long lIndex)
        {
        return instantiateUnmodifiableIterator(delegate().reverseIterator(lIndex));
        }

    /**
    * {@inheritDoc}
    */
    public CopyOnWriteLongArray<V> clone()
        {
        // the clone can share the same (immutable) underlying array
        CopyOnWriteLongArray<V> arrayNew = new CopyOnWriteLongArray<>();
        arrayNew.setDelegate(delegate());

        return arrayNew;
        }


    // ----- inner class: UnmodifiableIterator ----------------------------

    /**
    * Factory pattern: instantiate an UnmodifiableIterator.
    *
    * @param iterator  the underlying iterator
    *
    * @return an UnmodifiableIterator
    */
    public LongArray.Iterator<V> instantiateUnmodifiableIterator(
                LongArray.Iterator<V> iterator)
        {
        return new UnmodifiableIterator<>(iterator);
        }

    /**
    * Unmodifiable view of a LongArray.Iterator.
    */
    public static class UnmodifiableIterator<V>
            implements LongArray.Iterator<V>
        {
        // ----- constructors ---------------------------------------------

        /**
         * Construct a wrapper for the specified Iterator.
         *
         * @param iterator  the iterator to create a wrapper for
         */
        public UnmodifiableIterator(LongArray.Iterator<V> iterator)
            {
            m_iteratorInternal = iterator;
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the internal Iterator.
        *
        * @return the internal Iterator
        */
        public LongArray.Iterator<V> getInternalIterator()
            {
            return m_iteratorInternal;
            }

        // ----- LongArray.Iterator methods -------------------------------

        /**
        * {@inheritDoc}
        */
        public boolean hasNext()
            {
            return getInternalIterator().hasNext();
            }

        /**
        * {@inheritDoc}
        */
        public V next()
            {
            return getInternalIterator().next();
            }

        /**
        * {@inheritDoc}
        */
        public long getIndex()
            {
            return getInternalIterator().getIndex();
            }

        /**
        * {@inheritDoc}
        */
        public V getValue()
            {
            return getInternalIterator().getValue();
            }

        /**
        * {@inheritDoc}
        */
        public V setValue(V oValue)
            {
            throw new UnsupportedOperationException();
            }

        /**
        * {@inheritDoc}
        */
        public void remove()
            {
            throw new UnsupportedOperationException();
            }

        // ----- data members ---------------------------------------------

        /**
        * The internal Iterator
        */
        protected LongArray.Iterator<V> m_iteratorInternal;
        }


    // ----- constants and data members -----------------------------------

    /**
    * An empty placeholder array.  This array should not be mutated or exposed.
    */
    protected static final LongArray EMPTY_ARRAY = new SparseArray();

    /**
    * The internal LongArray.
    */
    private volatile LongArray<V> m_arrayInternal;
    }