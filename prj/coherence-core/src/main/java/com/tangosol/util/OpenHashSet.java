/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import com.oracle.coherence.common.collections.AbstractStableIterator;

import java.lang.reflect.Array;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;


/**
 * An implementation of {@link java.util.Set} that is optimized for memory
 * footprint. Specifically, instead of delegating to a {@link java.util.HashMap}
 * which instantiates an "Entry" object for each element (as the {@link
 * java.util.HashSet} does, which references those entries in a linked list
 * fashion with the head of the linked list stored in the hash bucket array,
 * this implementation stores its elements using an open hashing algorithm, i.e.
 * the elements are stored directly in the hash bucket array.
 * <p>
 * This implementation is explicitly <b>NOT</b> thread-safe.
 *
 * @author cp 2013.01.15
 * @since 12.2.1
 *
 * @param <E> the type of elements that can be managed by this set
 */
public class OpenHashSet<E>
        extends AbstractSet<E>
    {
    // ----- constructors ------------------------------------------------------

    /**
     * Default constructor.
     */
    public OpenHashSet()
        {
        clear();
        }

    /**
     * Create a OpenHashSet pre-sized to hold the specified number of
     * elements.
     *
     * @param initialCapacity  the initial capacity requirement for the hash set
     */
    public OpenHashSet(int initialCapacity)
        {
        this();

        if (initialCapacity > 0)
            {
            long cElements = (long) (initialCapacity * (1.0 / LOAD_FACTOR));
            if (cElements >= Integer.MAX_VALUE)
                {
                throw new IllegalArgumentException("initialCapacity too large: "
                        + initialCapacity);
                }

            // pretend we actually have that many elements & grow the storage
            // accordingly
            checkCapacity(initialCapacity);
            }
        else if (initialCapacity < 0)
            {
            throw new IllegalArgumentException("negative initial capacity: "
                    + initialCapacity);
            }
        }

    /**
     * Create a OpenHashSet that will initially contain the contents of the
     * passed Collection. In other words, this is a "copy constructor".
     *
     * @param coll  the Collection whose contents this
     */
    public OpenHashSet(Collection<? extends E> coll)
        {
        // pre-size the storage accordingly
        this(coll == null ? 0 : coll.size());

        if (coll != null)
            {
            Object[] aElement  = m_aElement;
            int      cElements = 0;
            for (E oExternal : coll)
                {
                Object o = toInternal(oExternal);
                int    i = find(o, true);
                if (i < 0)
                    {
                    aElement[-1 - i] = o;
                    ++cElements;
                    }
                }
            m_cElements = cElements;

            // resize if necessary (e.g. shrink if there were lots of
            // duplicates)
            checkCapacity(cElements);
            }
        }


    // ----- Set methods -------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
        {
        return m_cElements;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Object o)
        {
        return find(toInternal((E) o), false) >= 0;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(E e)
        {
        Object o = toInternal(e);
        int    i = find(o, true);
        if (i >= 0)
            {
            return false;
            }

        int cElements = m_cElements + 1;
        if (checkCapacity(cElements))
            {
            // hash changed; find new index to insert at
            i = find(o, true);
            assert i < 0;
            }

        // convert an "insertion point" to an index
        i = -1 - i;

        if (m_aElement[i] == REMOVED)
            {
            --m_cRemoved;
            }

        m_aElement[i] = o;
        m_cElements   = cElements;
        return true;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Object o)
        {
        int i = find(toInternal((E) o), false);
        if (i < 0)
            {
            return false;
            }

        m_aElement[i] = REMOVED;
        --m_cElements;
        ++m_cRemoved;
        return true;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear()
        {
        m_aElement         = EMPTY;
        m_cElements        = 0;
        m_cRemoved         = 0;
        m_cMaxDepth        = 0;
        m_cGrowThreshold   = -1;
        m_cShrinkThreshold = -1;
        m_cPurgeThreshold  = Integer.MAX_VALUE;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<E> iterator()
        {
        // take a snapshot of the array that holds the set contents
        final Object[] aElement = OpenHashSet.this.m_aElement;

        // keep track of whether there are any live iterators (to attempt to
        // prevent re-ordering while iteration is occurring)
        ++m_cLiveIterators;

        return new AbstractStableIterator<E>()
            {
            private int iNext;  // next index in the array to iterate

            @Override
            protected void advance()
                {
                final int cLimit = aElement.length;
                while (iNext < cLimit)
                    {
                    Object o = aElement[iNext++];
                    if (o != null && o != REMOVED)
                        {
                        setNext(toExternal(o));
                        return;
                        }
                    }

                // iterator is exhausted
                --m_cLiveIterators;
                }

            @Override
            protected void remove(E oPrev)
                {
                OpenHashSet.this.remove(oPrev);
                }
            };
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] toArray()
        {
        int c = size();
        if (c == 0)
            {
            return EMPTY;
            }

        Object[] a = new Object[c];
        int i = 0;

        for (Object o : m_aElement)
            {
            if (o != null && o != REMOVED)
                {
                a[i++] = toExternal(o);
                }
            }

        assert i == c;
        return a;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T[] toArray(T[] a)
        {
        // create the array to store the set contents
        int c = size();
        if (a == null)
            {
            // implied Object[] type
            a = (T[]) (c == 0 ? EMPTY : new Object[c]);
            }
        else if (a.length < c)
            {
            // if it is not big enough, a new array of the same runtime
            // type is allocated
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), c);
            }
        else if (a.length > c)
            {
            // if the collection fits in the specified array with room to
            // spare, the element in the array immediately following the
            // end of the collection is set to null
            a[c] = null;
            }

        if (c == 0)
            {
            return a;
            }

        int i = 0;
        for (Object o : m_aElement)
            {
            if (o != null && o != REMOVED)
                {
                a[i++] = (T) toExternal(o);
                }
            }

        assert i == c;
        return a;
        }


    // ----- internal ----------------------------------------------------------

    /**
     * Convert a value to the internal representation of that value in a form
     * that can be managed in this Set.
     *
     * @param oValue  a value that can a consumer of this Set has provided
     *
     * @return a value that this Set can manage internally; never null
     */
    private Object toInternal(E oValue)
        {
        return oValue == null ? NULL_SUBSTITUTE : oValue;
        }

    /**
     * Convert a value that is managed in this Set to the external value in the
     * form known to a consumer of this Set.
     * <p>
     * Note: Not private because it is used by the iterator inner class.
     *
     * @param o  the value as it is managed internally by this Set; never null
     *
     * @return the value as it was provided by a consumer of this Set
     */
    E toExternal(Object o)
        {
        return (E) (o == NULL_SUBSTITUTE ? null : o);
        }

    /**
     * Given a proposed capacity, validate the current capacity and adjust it
     * as necessary.
     *
     * @param c  the proposed capacity, in terms of the number of elements to
     *           store in the set
     *
     * @return true iff the storage was modified
     */
    private boolean checkCapacity(int c)
        {
        if (c >= m_cShrinkThreshold && c <= m_cGrowThreshold)
            {
            // even though the size of the storage is not changing, the storage
            // may have reached a point at which a large number of REMOVED
            // indicators are causing the performance of the set to degrade
            // (by lengthening a significant portion of open hash chains)
            if (c + m_cRemoved > m_cPurgeThreshold)
                {
                purgeRemoves();
                return true;
                }
            else
                {
                return false;
                }
            }

        // calculate the new modulo to manage the desired capacity, planning
        // for minimum 50% headroom (or half that minimum headroom if shrinking)
        final boolean fGrowing       = c > m_cGrowThreshold;
        final long    cTarget        = (long) (c * (fGrowing ? 1.5 : 1.25) * (1.0 / LOAD_FACTOR));
        final int     BIGGEST_MODULO = PRIME_MODULO[PRIME_MODULO.length - 1];
        if (cTarget > BIGGEST_MODULO)
            {
            throw new IllegalStateException("maximum size (" + BIGGEST_MODULO
                    + ") exceeded (" + cTarget + ")");
            }

        int iModulo = Arrays.binarySearch(PRIME_MODULO, (int) cTarget);
        if (iModulo < 0)
            {
            iModulo = -1 - iModulo;
            }

        // if it's growing, make sure it's growing
        assert !fGrowing || m_aElement.length == 0 ||
                (Arrays.binarySearch(PRIME_MODULO, m_aElement.length) >= 0 &&
                iModulo > Arrays.binarySearch(PRIME_MODULO, m_aElement.length));

        // if it's shrinking, make sure it's shrinking
        assert fGrowing ||
                (Arrays.binarySearch(PRIME_MODULO, m_aElement.length) >= 0 &&
                iModulo < Arrays.binarySearch(PRIME_MODULO, m_aElement.length));

        // save off the old storage (its contents will later need to be
        // rehashed)
        Object[] aOld = m_aElement;

        // create the new storage
        int      cNew = PRIME_MODULO[iModulo];
        Object[] aNew = new Object[cNew];

        // calculate the new growth threshold based on the load factor against
        // the new modulo
        m_cGrowThreshold   = (int) (cNew * LOAD_FACTOR);

        // calculate the new shrinkage threshold based on where the next
        // smallest modulo would have sufficient room to grow
        m_cShrinkThreshold = iModulo == 0 ? -1
                : (int) (PRIME_MODULO[iModulo-1] * LOAD_FACTOR * LOAD_FACTOR);

        // calculate the threshold at which the total number of elements *and*
        // REMOVED indicators will trigger a purging of the REMOVED indicators
        m_cPurgeThreshold  = (int) (cNew * PURGE_FACTOR);

        assert m_cGrowThreshold   > c;
        assert m_cShrinkThreshold < c;
        assert m_cPurgeThreshold > m_cGrowThreshold;

        // save off the new storage BEFORE rehashing (since the find method
        // relies on the storage)
        m_aElement  = aNew;
        m_cMaxDepth = 0;
        m_cRemoved  = 0;

        // rehash
        for (Object o : aOld)
            {
            if (o != null && o != REMOVED)
                {
                int i = find(o, true);
                if (i < 0)
                    {
                    aNew[-1 - i] = o;
                    }
                else
                    {
                    throw new IllegalStateException("duplicate found during rehash: "
                            + o + " and " + aNew[i]);
                    }
                }
            }

        return true;
        }

    /**
     * Determine the location of the specified object in the storage array.
     *
     * @param o     the object (in the "internal format") to find
     * @param fAdd  true if the intention is to add the specified object; false
     *              if just looking for (or planning to remove) the specified
     *              object
     *
     * @return the index of the specified object in the storage array, or a
     *         value less than zero if the specified object is not found in the
     *         storage array; if <tt>fAdd</tt> is <tt>true</tt>, and the return
     *         value is less than zero, then the return value is the insertion
     *         point encoded as <tt>(-1 - index)</tt> where the object should be
     *         placed
     */
    private int find(Object o, boolean fAdd)
        {
        Object[]  aElement  = m_aElement;
        int       cModulo   = aElement.length;
        if (cModulo == 0)
            {
            return -1;
            }

        int       nHash     = o.hashCode();
        long      nNextHash = nHash & 0xFFFFFFFFL;
        long      nHashInc  = 0L;
        int       iInsert   = -1;
        int       cDepth    = 0;
        final int MAX_DEPTH = m_cMaxDepth;
        while (true)
            {
            if (fAdd)
                {
                // as long as an insert point isn't found, the depth (in
                // terms of the number of iterations of open hashing) at
                // which the insert will occur keeps increasing
                if (iInsert < 0)
                    {
                    ++cDepth;
                    }
                }
            else if (++cDepth > MAX_DEPTH)
                {
                // in searching for the specified object, the open hash chain
                // was scanned past the max depth, therefore the object is not
                // present
                return -1;
                }

            int    iTest = (int) (nNextHash % cModulo);
            Object oTest = aElement[iTest];
            if (oTest == null)
                {
                // not found
                if (iInsert < 0)
                    {
                    iInsert = iTest;
                    }
                if (fAdd && cDepth > MAX_DEPTH)
                    {
                    m_cMaxDepth = cDepth;
                    }
                return -1 - iInsert;
                }
            else if (oTest == REMOVED)
                {
                // the REMOVED indicator can be replaced with an object to
                // insert, but the REMOVED indicator does not terminate the open
                // hash chain, so continue to search until the object is found
                // or the max depth is exceeded
                if (iInsert < 0)
                    {
                    iInsert = iTest;
                    }
                }
            else
                {
                try
                    {
                    if (o == oTest || nHash == oTest.hashCode() && o.equals(oTest))
                        {
                        if (iInsert >= 0 && m_cLiveIterators == 0)
                            {
                            // if a "REMOVED" was encountered while searching,
                            // then swap the found value "forward" in the open
                            // hash chain to where the first "REMOVED" was
                            // encountered
                            aElement[iInsert] = oTest;
                            aElement[iTest]   = REMOVED;
                            return iInsert;
                            }
                        else
                            {
                            return iTest;
                            }
                        }
                    }
                catch (ClassCastException e) {}
                }

            // rehash by adding a prime number, which will eventually cause this
            // loop to cover every index in the elements array
            nNextHash += nHashInc == 0 ? nHashInc = calculateHashIncrement(nHash) : nHashInc;
            }
        }

    /**
     * Remove the unnecessary {@link #REMOVED} place-holders from the element
     * storage. There is no way to provably remove all of the REMOVED indicators
     * in a single pass without allocating a new element storage. Instead, all
     * of the REMOVED indicators are replaced with <tt>null</tt>, and then a
     * single pass is made of the existent (non-null, non-REMOVED) elements. For
     * each element that does not hash to its current location, its proper hash
     * location is checked for availability, and if it is available, then the
     * element is moved there, with the position that it is being moved from
     * being replaced (in the general case) with a REMOVED indicator. If the
     * proper hash location is unavailable, then the open hash chain (a virtual
     * linked list) is searched for an available (null or REMOVED) spot until
     * the list returns to the point where the element currently sits, at which
     * point, it has been proven that the element cannot be moved up in its open
     * hash chain. As part of this process, the maximum depth ({@link
     * #m_cMaxDepth}) is recalculated.
     */
    private void purgeRemoves()
        {
        // clear all REMOVED flags
        Object[] aElement   = m_aElement;
        int      cModulo    = aElement.length;
        for (int i = 0; i < cModulo; ++i)
            {
            Object o = aElement[i];
            if (o == REMOVED)
                {
                aElement[i] = null;
                }
            }

        // for each element, see if it can be moved "forward" in its open hash
        // chain
        int    cRemoved   = 0;
        int    cMaxDepth  = m_cElements == 0 ? 0 : 1;
        Object oRemoved   = null;
        for (int i = 0; i < cModulo; ++i)
            {
            Object o = aElement[i];
            if (o != null && o != REMOVED)
                {
                int  nHash     = o.hashCode();
                long nNextHash = nHash & 0xFFFFFFFFL;
                int  iTarget   = (int) (nNextHash % cModulo);
                if (i != iTarget)
                    {
                    // the object doesn't hash to this location; see if it can
                    // be "moved up" in its open hash chain
                    int cDepth   = 1;
                    int nHashInc = 0;
                    do
                        {
                        Object oTarget = aElement[iTarget];
                        if (oTarget == null || oTarget == REMOVED)
                            {
                            // move the object to the target location,
                            // potentially replacing a REMOVED indicator, and
                            // place a REMOVED indicator (or null if the longest
                            // chain is still not more than length 1) in the
                            // location that the object was moved from
                            aElement[iTarget] = o;
                            aElement[i]       = oRemoved;
                            if (oRemoved == REMOVED && oTarget != REMOVED)
                                {
                                ++cRemoved;
                                }
                            break;
                            }

                        // calculate the next location in the open hash chain
                        nNextHash += nHashInc == 0 ? nHashInc = calculateHashIncrement(nHash) : nHashInc;
                        iTarget    = (int) (nNextHash % cModulo);
                        ++cDepth;
                        }
                    while (i != iTarget);

                    if (cDepth > cMaxDepth)
                        {
                        cMaxDepth = cDepth;

                        // as long as the max-depth is 1, there are no chains
                        // (i.e. nothing longer than 1) and thus there cannot be
                        // a REMOVED indicator in a chain, but once the max
                        // depth moves past 1, the REMOVED indicator must be
                        // used (instead of null) because the item being moved
                        // may exist in a chain that has already been evaluated
                        oRemoved  = REMOVED;
                        }
                    }
                }
            }

        m_cMaxDepth = cMaxDepth;
        m_cRemoved  = cRemoved;
        }

    /**
     * Expose the indexed element storage to sub-classes in a range-unsafe
     * manner.
     *
     * @param n  an element index
     *
     * @return the value stored at the specified index; note that a null value
     *         is not differentiable from an empty element, since both are
     *         returned as <tt>null</tt>
     */
    protected E getElement(int n)
        {
        final Object o = m_aElement[n];
        return o == REMOVED ? null : toExternal(m_aElement[n]);
        }

    /**
     * Provide an arbitrary prime increment to add to a hash code in order to
     * determine the next index in a hash chain in an open hash data structure.
     * <p>
     * The purpose of this method is to try to provide different hash increments
     * for different hash codes, to avoid harmonics in the open hash system.
     *
     * @param nHash  the base hash code that is being incremented
     *
     * @return a hash increment
     */
    static int calculateHashIncrement(int nHash)
        {
        return PRIME_NON_MODULO[0x7F & (nHash ^ (nHash >>> 7) ^ (nHash >>> 14) ^ (nHash >>> 21) ^ (nHash >>> 28))];
        }


    // ----- data members ------------------------------------------------------

    /**
     * An object that represents an element that has been removed.
     */
    static final Object REMOVED = new Object();

    /**
     * An object that represents an element whose value is the Java
     * <tt>null</tt> reference.
     */
    static final Object NULL_SUBSTITUTE = new Object()
        {
        @Override
        public int hashCode()
            {
            // why not? the 50,000,000th prime ..
            return 982451653;
            }

        @Override
        public boolean equals(Object that)
            {
            return this == that;
            }
        };

    /**
     * An empty array.
     */
    static final Object[] EMPTY = new Object[0];

    /**
     * This is the "percentage of full" (also known as the "load factor") that
     * the OpenHashSet targets to not exceed.
     */
    static final double LOAD_FACTOR = 0.65;

    /**
     * This is the "percentage of full" -- including {@link #REMOVED} indicators
     * -- at which point the OpenHashSet will attempt to purge the REMOVED
     * indicators.
     */
    static final double PURGE_FACTOR = 0.75;

    /**
     * A list of possible modulos to use.
     */
    static final int[] PRIME_MODULO =
        {
        17,31,47,61,79,103,127,149,173,197,229,277,347,397,457,509,587,641,
        701,761,827,883,953,1019,1129,1279,1427,1543,1733,1951,2143,2371,
        2671,2927,3253,3539,3907,4211,4591,4973,5393,5743,6143,6619,6997,
        7529,8009,8423,8819,9311,9929,10069,11087,12203,13003,14051,15017,
        16007,17027,18061,19013,20063,23011,27011,30011,35023,40009,45007,
        50021,60013,70001,80021,90001,100003,120011,140009,160001,180001,
        200003,233021,266003,300007,350003,400009,450001,500009,550007,
        600011,650011,700001,800011,850009,900001,950009,1000003,1100009,
        1200007,1300021,1400017,1500007,1600033,1700021,1800017,1900009,
        2000003,2500009,3000017,3500017,4000037,4500007,5000011,6000011,
        7000003,8000009,9000011,10000019,12000017,14000029,16000057,
        18000041,20000003,25000009,30000001,35000011,40000003,45000017,
        50000017,60000011,70000027,80000023,90000049,100000007,150000001,
        200000033,300000007,400000009,500000003,600000001,700000001,
        800000011,900000011,1000000007,1100000009,1200000041,1300000003,
        1400000023,1500000001,1600000009,1700000009,1800000011,1900000043,
        2147455043 // close to Integer.MAX_VALUE
        };

    /**
     * A set of 128 relatively small primes that are not found in the list
     * {@link #PRIME_MODULO}.
     */
    static final int[] PRIME_NON_MODULO =
        {
        37,  41,  43,  53,  59,  67,  71,  73,  83,  89,  97, 101, 107, 109, 113, 131,
        137, 139, 151, 157, 163, 167, 179, 181, 191, 193, 199, 211, 223, 227, 233, 239,
        241, 251, 257, 263, 269, 271, 281, 283, 293, 307, 311, 313, 317, 331, 337, 349,
        353, 359, 367, 373, 379, 383, 389, 401, 409, 419, 421, 431, 433, 439, 443, 449,
        461, 463, 467, 479, 487, 491, 499, 503, 521, 523, 541, 547, 557, 563, 569, 571,
        577, 593, 599, 601, 607, 613, 617, 619, 631, 643, 647, 653, 659, 661, 673, 677,
        683, 691, 709, 719, 727, 733, 739, 743, 751, 757, 769, 773, 787, 797, 809, 811,
        821, 823, 829, 839, 853, 857, 859, 863, 877, 881, 887, 907, 911, 919, 929, 937,
        };

    /**
     * Stores the elements of the Set using an "open hashing" algorithm.
     */
    private Object[] m_aElement;

    /**
     * Tracks the number of elements currently stored in the set.
     */
    private int m_cElements;

    /**
     * The number of {@link #REMOVED} indicators in the {@link #m_aElement}
     * array.
     */
    private int m_cRemoved;

    /**
     * The maximum number of look-ups (the initial hash plus any number of
     * re-hashes) necessary to find any object.
     */
    private int m_cMaxDepth;

    /**
     * The number of elements above which the set's storage must grow.
     */
    private int m_cGrowThreshold;

    /**
     * The number of elements below which the set's storage must shrink.
     */
    private int m_cShrinkThreshold;

    /**
     * The number of elements (including REMOVED indicators) above which the
     * set's storage must be purged of REMOVED indicators.
     */
    private int m_cPurgeThreshold;

    /**
     * A count of iterators that have been created but not exhausted. While an
     * iterator is "live", the set will suspend the optimization that moves
     * elements of the set towards the front of their open hash chains; see
     * SetTest#assertIteratorMatchesCollection() for an example failure mode
     * that would occur if this optimization were not suspended.
     */
    int m_cLiveIterators;
    }
