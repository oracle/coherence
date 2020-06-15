/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import com.tangosol.util.comparator.SafeComparator;

import java.util.AbstractCollection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import java.util.concurrent.atomic.AtomicLong;


/**
 * SortedBag is a <a href="http://en.wikipedia.org/wiki/Multiset">
 * <i>multiset</i> or <i>bag</i></a> implementation that supports sorted traversal
 * of the contained elements and is optimized for insertions and removals.
 * <p>
 * This implementation is not thread-safe and does not support null elements.
 *
 * @author rhl 2013.04.24
 */
public class SortedBag<E>
        extends AbstractCollection<E>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    protected SortedBag()
        {
        this(SafeComparator.INSTANCE);
        }

    /**
     * Construct a SortedBag whose elements are to be ordered by the specified
     * comparator.
     *
     * @param comparator  the comparator to use to order the elements
     */
    public SortedBag(Comparator<? super E> comparator)
        {
        m_atomicNonce = new AtomicLong(0L);
        m_comparator  = comparator == null ? SafeComparator.INSTANCE : comparator;
        m_map         = instantiateInternalMap(m_comparator);
        }


    // ----- accessors ------------------------------------------------------

    /**
     * Return the internal sorted-map holding the bag contents.
     *
     * @return the internal sorted map holding the bag contents
     */
    protected NavigableMap getInternalMap()
        {
        return m_map;
        }

    /**
     * Return the Comparator used to order the bag contents.
     *
     * @return the Comparator used to order the bag contents
     */
    protected Comparator<? super E> getComparator()
        {
        return m_comparator;
        }

    /**
     * Return the nonce counter used to assign unique element ids.
     *
     * @return the nonce counter
     */
    protected AtomicLong getNonceCounter()
        {
        return m_atomicNonce;
        }

    // ----- Collection interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    public int size()
        {
        return getInternalMap().size();
        }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty()
        {
        return getInternalMap().isEmpty();
        }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Object o)
        {
        Object oCeiling = getInternalMap().ceilingKey(o);
        return oCeiling != null && Base.equals(o, unwrap(oCeiling));
        }

    /**
     * {@inheritDoc}
     */
    public Iterator<E> iterator()
        {
        final Iterator iter = getInternalMap().keySet().iterator();
        return new Iterator<E>()
            {
            public boolean hasNext()
                {
                return iter.hasNext();
                }

            public E next()
                {
                return unwrap(iter.next());
                }

            public void remove()
                {
                iter.remove();
                }
            };
        }

    /**
     * {@inheritDoc}
     */
    public boolean add(E o)
        {
        // loop around a "conditional" put here to allow for a "safe" extension
        NavigableMap map = getInternalMap();
        do
            {
            if (Base.equals(o, unwrap(map.ceilingKey(o))))
                {
                // does not need to be "conditional", as all wrapped values are unique
                map.put(wrap(o), NO_VALUE);
                return true;
                }
            else if (map.put(o, NO_VALUE) == null)
                {
                return true;
                }
            }
        while (true);
        }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object o)
        {
        // loop around a "conditional" remove here to allow for a "safe" extension
        NavigableMap map = getInternalMap();
        do
            {
            Object oCeiling = map.ceilingKey(o);
            if (Base.equals(o, unwrap(oCeiling)))
                {
                if (map.remove(oCeiling) != null)
                    {
                    return true;
                    }
                }
            else
                {
                return false;
                }
            }
        while (true);
        }


    // ----- SortedBag methods ----------------------------------------------

    /**
     * Returns a view of the portion of this bag whose elements range
     * from <tt>fromElement</tt>, inclusive, to <tt>toElement</tt>,
     * exclusive.  (If <tt>fromElement</tt> and <tt>toElement</tt> are
     * equal, the returned bag is empty.)  The returned bag is backed
     * by this bag, so changes in the returned bag are reflected in
     * this bag, and vice-versa.  The returned bag supports all
     * optional bag operations that this bag supports.
     *
     * <p>The returned bag will throw an <tt>IllegalArgumentException</tt>
     * on an attempt to insert an element outside its range.
     *
     * @param fromElement  low endpoint (inclusive) of the returned bag
     * @param toElement    high endpoint (exclusive) of the returned bag
     *
     * @return a view of the portion of this bag whose elements range from
     *         <tt>fromElement</tt>, inclusive, to <tt>toElement</tt>, exclusive
     *
     * @throws ClassCastException if <tt>fromElement</tt> and
     *         <tt>toElement</tt> cannot be compared to one another using this
     *         bag's comparator (or, if the bag has no comparator, using
     *         natural ordering).  Implementations may, but are not required
     *         to, throw this exception if <tt>fromElement</tt> or
     *         <tt>toElement</tt> cannot be compared to elements currently in
     *         the bag.
     * @throws NullPointerException if <tt>fromElement</tt> or
     *         <tt>toElement</tt> is null and this bag does not permit null
     *         elements
     * @throws IllegalArgumentException if <tt>fromElement</tt> is
     *         greater than <tt>toElement</tt>; or if this bag itself
     *         has a restricted range, and <tt>fromElement</tt> or
     *         <tt>toElement</tt> lies outside the bounds of the range
     */
    public SortedBag<E> subBag(E fromElement, E toElement)
        {
        return new ViewBag(fromElement, toElement);
        }

    /**
     * Returns a view of the portion of this bag whose elements are
     * strictly less than <tt>toElement</tt>.  The returned bag is
     * backed by this bag, so changes in the returned bag are
     * reflected in this bag, and vice-versa.  The returned bag
     * supports all optional bag operations that this bag supports.
     *
     * <p>The returned bag will throw an <tt>IllegalArgumentException</tt>
     * on an attempt to insert an element outside its range.
     *
     * @param toElement  high endpoint (exclusive) of the returned bag
     *
     * @return a view of the portion of this bag whose elements are strictly
     *         less than <tt>toElement</tt>
     *
     * @throws ClassCastException if <tt>toElement</tt> is not compatible
     *         with this bag's comparator (or, if the bag has no comparator,
     *         if <tt>toElement</tt> does not implement {@link Comparable}).
     *         Implementations may, but are not required to, throw this
     *         exception if <tt>toElement</tt> cannot be compared to elements
     *         currently in the bag.
     * @throws NullPointerException if <tt>toElement</tt> is null and
     *         this bag does not permit null elements
     * @throws IllegalArgumentException if this bag itself has a
     *         restricted range, and <tt>toElement</tt> lies outside the
     *         bounds of the range
     */
    public SortedBag<E> headBag(E toElement)
        {
        return new ViewBag(null, toElement);
        }

    /**
     * Returns a view of the portion of this bag whose elements are
     * greater than or equal to <tt>fromElement</tt>.  The returned
     * bag is backed by this bag, so changes in the returned bag are
     * reflected in this bag, and vice-versa.  The returned bag
     * supports all optional bag operations that this bag supports.
     *
     * <p>The returned bag will throw an <tt>IllegalArgumentException</tt>
     * on an attempt to insert an element outside its range.
     *
     * @param fromElement  low endpoint (inclusive) of the returned bag
     *
     * @return a view of the portion of this bag whose elements are greater
     *         than or equal to <tt>fromElement</tt>
     *
     * @throws ClassCastException if <tt>fromElement</tt> is not compatible
     *         with this bag's comparator (or, if the bag has no comparator,
     *         if <tt>fromElement</tt> does not implement {@link Comparable}).
     *         Implementations may, but are not required to, throw this
     *         exception if <tt>fromElement</tt> cannot be compared to elements
     *         currently in the bag.
     * @throws NullPointerException if <tt>fromElement</tt> is null
     *         and this bag does not permit null elements
     * @throws IllegalArgumentException if this bag itself has a
     *         restricted range, and <tt>fromElement</tt> lies outside the
     *         bounds of the range
     */
    public SortedBag<E> tailBag(E fromElement)
        {
        return new ViewBag(fromElement, null);
        }

    /**
     * Returns the first (lowest) element currently in this bag.
     *
     * @return the first (lowest) element currently in this bag
     *
     * @throws NoSuchElementException if this bag is empty
     */
    public E first()
        {
        return unwrap(getInternalMap().firstKey());
        }

    /**
     * Returns the last (highest) element currently in this bag.
     *
     * @return the last (highest) element currently in this bag
     *
     * @throws NoSuchElementException if this bag is empty
     */
    public E last()
        {
        return unwrap(getInternalMap().lastKey());
        }

    /**
     * Remove and return the least element in this bag, or null if empty.
     *
     * @return the removed first element of this bag, or null if empty
     */
    public E removeFirst()
        {
        Map.Entry entryRemoved = getInternalMap().pollFirstEntry();
        return entryRemoved == null ? null : unwrap(entryRemoved.getKey());
        }

    /**
     * Remove and return the greatest element in this bag, or null if empty.
     *
     * @return the removed last element of this bag, or null if empty
     */
    public E removeLast()
        {
        Map.Entry entryRemoved = getInternalMap().pollLastEntry();
        return entryRemoved == null ? null : unwrap(entryRemoved.getKey());
        }


    // ----- internal helpers -----------------------------------------------

    /**
     * Factory for the sorted internal map to use to hold the bag elements.
     *
     * @param comparator  the comparator to use to sort the bag elements
     *
     * @return a sorted map
     */
    protected NavigableMap instantiateInternalMap(Comparator comparator)
        {
        return new TreeMap(new WrapperComparator(comparator));
        }

    /**
     * Wrap the specified element to ensure uniqueness.
     *
     * @param o  the element to wrap
     *
     * @return a unique element representing the specified element
     */
    protected UniqueElement<E> wrap(E o)
        {
        return new UniqueElement<>(o);
        }

    /**
     * Unwrap the specified element (which could be a {@link UniqueElement
     * wrapped} or an "actual") element.
     *
     * @param o  the element to unwrap
     *
     * @return the unwrapped element
     */
    protected E unwrap(Object o)
        {
        return (E) (o instanceof UniqueElement ? ((UniqueElement) o).f_elem : o);
        }


    // ----- inner class: WrapperComparator ---------------------------------

    /**
     * WrapperComparator is a Comparator implementation that is aware of
     * {@link UniqueElement} wrappers and will delegate the comparison of the
     * elements in both forms to the wrapped comparator.
     */
    protected class WrapperComparator
            implements Comparator
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a WrapperComparator around the specified comparator.
         *
         * @param comparator  the underlying comparator
         */
        public WrapperComparator(Comparator comparator)
            {
            f_comparator = comparator;
            }

        // ----- Comparator methods -----------------------------------------

        /**
         * {@inheritDoc}
         */
        public int compare(Object o1, Object o2)
            {
            if (o1 instanceof UniqueElement)
                {
                return ((UniqueElement) o1).compareTo(o2);
                }
            else if (o2 instanceof UniqueElement)
                {
                return -((UniqueElement) o2).compareTo(o1);
                }
            else // o1 and o2 are both "real" elements
                {
                return f_comparator.compare(o1, o2);
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The underlying "logical" comparator.
         */
        protected final Comparator f_comparator;
        }


    // ----- inner class: ViewBag -------------------------------------------

    /**
     * A range-limited view of the SortedBag.  This view is backed by the
     * SortedBag, so any modifications made to it are visible to the underlying
     * bag, and vice-versa.
     */
    protected class ViewBag
            extends SortedBag<E>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a view of the SortedBag, constrained to the range [from, to).
         *
         * @param from  the "from" element (inclusive), or null
         * @param to    the "to" element (exclusive), or null
         */
        public ViewBag(E from, E to)
            {
            super();

            NavigableMap mapInternal = SortedBag.this.getInternalMap();
            m_map = from == null
                    ? mapInternal.headMap(to, false)
                    : to == null
                        ? mapInternal.tailMap(from, true)
                        : mapInternal.subMap(from, true, to, false);
            m_atomicNonce = SortedBag.this.getNonceCounter();
            m_comparator  = SortedBag.this.getComparator();

            f_oFrom = from;
            f_oTo   = to;
            }

        // ----- SortedBag methods ------------------------------------------

        /**
         * {@inheritDoc}
         */
        public boolean add(E o)
            {
            checkRange(o);

            return super.add(o);
            }

        /**
         * {@inheritDoc}
         */
        public SortedBag<E> subBag(E fromElement, E toElement)
            {
            checkRange(fromElement);
            checkRange(toElement);

            return super.subBag(fromElement, toElement);
            }

        /**
         * {@inheritDoc}
         */
        public SortedBag<E> headBag(E toElement)
            {
            checkRange(toElement);

            return super.headBag(toElement);
            }

        /**
         * {@inheritDoc}
         */
        public SortedBag<E> tailBag(E fromElement)
            {
            checkRange(fromElement);

            return super.tailBag(fromElement);
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Check that the specified object is within the range of this view.
         *
         * @param o  the object to check
         */
        protected void checkRange(Object o)
            {
            Comparator comparator = getComparator();
            if ((f_oFrom != null && comparator.compare(o, f_oFrom) < 0) ||
                (f_oTo   != null && comparator.compare(o, f_oTo) >= 0))
                {
                throw new IllegalArgumentException();
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The (inclusive) lower bound of this view.
         */
        protected final Object f_oFrom;

        /**
         * The (exclusive) upper bound of this view.
         */
        protected final Object f_oTo;
        }


    // ----- inner class: UniqueElement -------------------------------------

    /**
     * UniqueElement represents a unique instance of a logical element in the bag.
     */
    protected class UniqueElement<E>
            implements Comparable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a UniqueElement to represent the specified element.
         *
         * @param elem  the element
         */
        public UniqueElement(E elem)
            {
            f_elem      = elem;
            f_nUniqueId = getNonceCounter().incrementAndGet();
            }

        // ----- Comparable interface ---------------------------------------

        /**
         * {@inheritDoc}
         */
        public int compareTo(Object o)
            {
            Comparator comparator = getComparator();
            if (o instanceof UniqueElement)
                {
                UniqueElement<E> that     = (UniqueElement<E>) o;
                int           nCompare = comparator.compare(this.f_elem, that.f_elem);
                long          nIdThis  = this.f_nUniqueId;
                long          nIdThat  = that.f_nUniqueId;
                return nCompare == 0
                        ? nIdThis == nIdThat
                            ? 0
                            : nIdThis < nIdThat
                                ? -1 : 1
                        : nCompare;
                }
            else
                {
                int nCompare = comparator.compare(this.f_elem, o);

                // this must be comparing to a "plain" or "actual" element;
                // order the actual elements first if they are equivalent
                return nCompare < 0 ? -1 : 1;
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The unique "id" for this element.
         */
        protected final long f_nUniqueId;

        /**
         * The "actual" element.
         */
        protected final E f_elem;
        }

    // ----- constants and data members -------------------------------------

    /**
     * Marker value object.
     */
    protected static final Object NO_VALUE = new Object();

    /**
     * The nonce used to increment the unique element ids.
     */
    protected AtomicLong m_atomicNonce;

    /**
     * The internal sorted map holding the bag contents.
     */
    protected NavigableMap m_map;

    /**
     * The comparator used to compare logical elements.
     */
    protected Comparator<? super E> m_comparator;
    }
