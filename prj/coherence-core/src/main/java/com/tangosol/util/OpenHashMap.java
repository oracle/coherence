/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import com.oracle.coherence.common.collections.AbstractStableIterator;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * An implementation of {@link java.util.Map} that is optimized for memory
 * footprint. Specifically, instead of instantiating an "Entry" object for each
 * element (as the {@link java.util.HashMap} does, which references those
 * entries in a linked list fashion with the head of the linked list stored in
 * the hash bucket array, this implementation stores its elements using an open
 * hashing algorithm, i.e. the keys are stored directly in the hash bucket
 * array and the values are stored in a corresponding values array.
 * <p>
 * This implementation is explicitly <b>NOT</b> thread-safe.
 *
 * @author cp 2013.01.30
 * @since 12.2.1
 */
public class OpenHashMap<K, V>
        extends AbstractKeyBasedMap<K, V>
    {
    // ----- constructors ------------------------------------------------------

    /**
     * Default constructor.
     */
    public OpenHashMap()
        {
        clear();
        }

    /**
     * Create a OpenHashMap pre-sized to hold the specified number of entries.
     *
     * @param initialCapacity  the initial capacity requirement for the hash map
     */
    public OpenHashMap(int initialCapacity)
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

            // pretend we actually have that many entries & grow the storage
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
     * Create a OpenHashMap that will initially contain the contents of the
     * passed Map. In other words, this is a "copy constructor".
     *
     * @param map  the Collection whose contents this
     */
    public OpenHashMap(Map map)
        {
        // pre-size the storage accordingly
        this(map == null ? 0 : map.size());

        if (map != null)
            {
            Object[] aElement  = m_aEntry;
            int      cElements = 0;
            for (Map.Entry entry : (Set<Map.Entry>) map.entrySet())
                {
                Object oKey = toInternal(entry.getKey());
                int    i    = find(oKey, true);
                if (i < 0)
                    {
                    i = -1 - i;
                    aElement[i]   = oKey;
                    aElement[i+1] = toInternal(entry.getValue());
                    ++cElements;
                    }
                else
                    {
                    throw new IllegalArgumentException("duplicate entry for key: " + entry.getKey());
                    }
                }
            m_cEntries = cElements;
            }
        }


    // ----- Map interface -----------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
        {
        return m_cEntries;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(Object oKey)
        {
        return find(toInternal(oKey), false) >= 0;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public V get(Object oKey)
        {
        int i = find(toInternal(oKey), false);
        return i < 0 ? null : toExternal(m_aEntry[i+1]);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object put(Object oKey, Object oValue)
        {
        oKey = toInternal(oKey);
        int i = find(oKey, true);

        Object[] aEntry = m_aEntry;
        Object oOrig;
        if (i < 0)
            {
            // adding an entry; make sure there's enough storage
            int cEntries = m_cEntries + 1;
            if (checkCapacity(cEntries))
                {
                // hash changed; find new index to insert at
                aEntry = m_aEntry;
                i = find(oKey, true);
                assert i < 0;
                }
            m_cEntries = cEntries;

            // no previous value for the key
            oOrig = null;

            // convert an "insertion point" to an index
            i = -1 - i;

            if (aEntry[i] == REMOVED)
                {
                --m_cRemoved;
                }
            }
        else
            {
            oOrig = toExternal(aEntry[i+1]);
            }

        aEntry[i]   = oKey;
        aEntry[i+1] = toInternal(oValue);

        return oOrig;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public V remove(Object oKey)
        {
        int i = find(toInternal(oKey), false);
        if (i < 0)
            {
            return null;
            }

        Object[] aEntry = m_aEntry;
        Object   oValue = aEntry[i+1];

        aEntry[i]   = REMOVED;
        aEntry[i+1] = null;
        --m_cEntries;
        ++m_cRemoved;

        return toExternal(oValue);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear()
        {
        m_aEntry           = EMPTY;
        m_cEntries         = 0;
        m_cRemoved         = 0;
        m_cMaxDepth        = 0;
        m_cGrowThreshold   = -1;
        m_cShrinkThreshold = -1;
        m_cPurgeThreshold  = Integer.MAX_VALUE;
        }


    // ----- inner class: KeySet -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected Set instantiateKeySet()
        {
        return new KeySet();
        }

    /**
     * A set of keys backed by this map.
     */
    protected class KeySet
            extends AbstractKeyBasedMap.KeySet
        {
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

            Object[] aSrc  = m_aEntry;
            Object[] aDest = new Object[c];
            int      iDest = 0;
            for (int iSrc = 0, cSrc = aSrc.length; iSrc < cSrc; iSrc += 2)
                {
                Object o = aSrc[iSrc];
                if (o != null && o != REMOVED)
                    {
                    aDest[iDest++] = toExternal(o);
                    }
                }

            assert iDest == c;
            return aDest;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object[] toArray(Object a[])
            {
            // create the array to store the key set contents
            int c = size();
            if (a == null)
                {
                // implied Object[] type
                a = (c == 0 ? EMPTY : new Object[c]);
                }
            else if (a.length < c)
                {
                // if it is not big enough, a new array of the same runtime
                // type is allocated
                a = (Object[]) Array.newInstance(a.getClass().getComponentType(), c);
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

            Object[] aSrc  = m_aEntry;
            int      iDest = 0;
            for (int iSrc = 0, cSrc = aSrc.length; iSrc < cSrc; iSrc += 2)
                {
                Object o = aSrc[iSrc];
                if (o != null && o != REMOVED)
                    {
                    a[iDest++] = toExternal(o);
                    }
                }

            assert iDest == c;
            return a;
            }
        }


    // ----- inner class: EntrySet ---------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected Set instantiateEntrySet()
        {
        return new EntrySet();
        }

    /**
     * A set of entries backed by this map.
     */
    public class EntrySet
            extends AbstractKeyBasedMap.EntrySet
        {
        /**
         * {@inheritDoc}
         */
        public Object[] toArray()
            {
            int c = size();
            if (c == 0)
                {
                return EMPTY;
                }

            Object[] aSrc  = m_aEntry;
            Object[] aDest = new Object[c];
            int      iDest = 0;
            for (int iSrc = 0, cSrc = aSrc.length; iSrc < cSrc; iSrc += 2)
                {
                Object o = aSrc[iSrc];
                if (o != null && o != REMOVED)
                    {
                    aDest[iDest++] = instantiateEntry(toExternal(o), toExternal(aSrc[iSrc+1]));
                    }
                }

            assert iDest == c;
            return aDest;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object[] toArray(Object a[])
            {
            // create the array to store the entry set contents
            int c = size();
            if (a == null)
                {
                // implied Object[] type
                a = (c == 0 ? EMPTY : new Object[c]);
                }
            else if (a.length < c)
                {
                // if it is not big enough, a new array of the same runtime
                // type is allocated
                a = (Object[]) Array.newInstance(a.getClass().getComponentType(), c);
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

            Object[] aSrc  = m_aEntry;
            int      iDest = 0;
            for (int iSrc = 0, cSrc = aSrc.length; iSrc < cSrc; iSrc += 2)
                {
                Object o = aSrc[iSrc];
                if (o != null && o != REMOVED)
                    {
                    a[iDest++] = instantiateEntry(toExternal(o), toExternal(aSrc[iSrc+1]));
                    }
                }

            assert iDest == c;
            return a;
            }

        // ----- inner class: Entry -------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        protected Map.Entry instantiateEntry(Object oKey, Object oValue)
            {
            return new Entry(oKey, oValue);
            }

        /**
         * An Entry implementation that is augmented to allow an Entry instance
         * to be re-used, which means that the same Entry instance can represent
         * different map entries over time.
         */
        protected class Entry
                extends AbstractKeyBasedMap.EntrySet.Entry
            {
            /**
             * Construct an Entry.
             *
             * @param oKey    the Entry key
             * @param oValue  the Entry value (optional)
             */
            protected Entry(Object oKey, Object oValue)
                {
                super(oKey, oValue);
                }

            /**
             * Re-use the Entry instance for a different key and value.
             *
             * @param oKey    the new key
             * @param oValue  the new value
             */
            protected void reuse(Object oKey, Object oValue)
                {
                m_oKey   = oKey;
                m_oValue = oValue;
                }
            }

        // ----- inner class: Entry Set Iterator ------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        protected Iterator instantiateIterator()
            {
            return new EntrySetIterator();
            }

        /**
         * An Iterator over the EntrySet that is backed by the Map.
         */
        protected class EntrySetIterator
                extends AbstractKeyBasedMap.EntrySet.EntrySetIterator
            {
            Entry m_entry;

            /**
             * {@inheritDoc}
             */
            @Override
            public Map.Entry<K, V> next()
                {
                // this implementation assumes that the Entry objects that are
                // iterated will not be held onto; since there are no actual
                // "Entry" objects managed by this map, creating one for each
                // key would be unduly expensive from a memory management POV
                Object oKey  = m_iterKeys.next();
                Entry  entry = m_entry;
                if (entry == null)
                    {
                    m_entry = entry = (Entry) instantiateEntry(oKey, null);
                    }
                else
                    {
                    entry.reuse(oKey, null);
                    }
                return entry;
                }
            }

        }


    // ----- internal methods --------------------------------------------------

    /**
     * Convert a key or value to the internal representation in a form that can
     * be managed in this Map.
     *
     * @param oValue  a value that can a consumer of this Map has provided
     *
     * @return a value that this Map can manage internally; never null
     */
    private Object toInternal(Object oValue)
        {
        return oValue == null ? NULL_SUBSTITUTE : oValue;
        }

    /**
     * Convert a value that is managed in this Map to the external value in the
     * form known to a consumer of this Map.
     * <p>
     * Note: Not private because it is used by inner classes.
     *
     * @param o  the value as it is managed internally by this Map; never null
     *
     * @return the value as it was provided by a consumer of this Map
     */
    V toExternal(Object o)
        {
        return o == NULL_SUBSTITUTE ? null : (V) o;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator iterateKeys()
        {
        // keep a reference to the current array that holds the Map contents
        final Object[] aEntry = OpenHashMap.this.m_aEntry;

        // keep track of whether there are any live iterators (to attempt to
        // prevent re-ordering while iteration is occurring)
        ++m_cLiveIterators;

        return new AbstractStableIterator()
            {
            private int iNext;  // next index in the array to iterate

            @Override
            protected void advance()
                {
                final int cLimit = aEntry.length;
                while (iNext < cLimit)
                    {
                    Object o = aEntry[iNext];
                    iNext += 2;
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
            protected void remove(Object oPrev)
                {
                OpenHashMap.this.remove(oPrev);
                }
            };
        }

    /**
     * Given a proposed capacity, validate the current capacity and adjust it
     * as necessary.
     *
     * @param c  the proposed capacity, in terms of the number of entries to
     *           store in the map
     *
     * @return true iff the storage was modified
     */
    private boolean checkCapacity(int c)
        {
        if (c >= m_cShrinkThreshold && c <= m_cGrowThreshold)
            {
            // even though the size of the storage is not changing, the storage
            // may have reached a point at which a large number of REMOVED
            // indicators are causing the performance of the map to degrade
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

        // if it's growing, make sure it's growing; note that the entry array is
        // twice the length of the modulo
        assert !fGrowing || m_aEntry.length == 0 ||
                (Arrays.binarySearch(PRIME_MODULO, m_aEntry.length >>> 1) >= 0 &&
                iModulo > Arrays.binarySearch(PRIME_MODULO, m_aEntry.length >>> 1));

        // if it's shrinking, make sure it's shrinking; note that the entry
        // array is twice the length of the modulo
        assert fGrowing ||
                (Arrays.binarySearch(PRIME_MODULO, m_aEntry.length >>> 1) >= 0 &&
                iModulo < Arrays.binarySearch(PRIME_MODULO, m_aEntry.length >>> 1));

        // save off the old storage (its contents will later need to be
        // rehashed)
        Object[] aOld = m_aEntry;

        // create the new storage; the array stores keys AND values, so it is
        // *double* the modulo
        int cNew = PRIME_MODULO[iModulo];
        if (cNew > Integer.MAX_VALUE >>> 1)
            {
            throw new IllegalStateException("maximum array size exceeded (modulo="
                    + cNew + ")");
            }
        Object[] aNew = new Object[cNew << 1];

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
        m_aEntry    = aNew;
        m_cMaxDepth = 0;
        m_cRemoved  = 0;

        // rehash
        for (int iOld = 0, cOld = aOld.length; iOld < cOld; iOld += 2)
            {
            Object oKey = aOld[iOld];
            if (oKey != null && oKey != REMOVED)
                {
                int iNew = find(oKey, true);
                if (iNew < 0)
                    {
                    iNew = -1 - iNew;
                    aNew[iNew  ] = oKey;
                    aNew[iNew+1] = aOld[iOld+1];
                    }
                else
                    {
                    throw new IllegalStateException("duplicate found during rehash: "
                            + oKey + " and " + aNew[iNew]);
                    }
                }
            }

        return true;
        }

    /**
     * Determine the location of the specified key in the storage array.
     *
     * @param o     the key (in the "internal format") to find
     * @param fAdd  true if the intention is to add the specified object; false
     *              if just looking for (or planning to remove) the specified
     *              object
     *
     * @return the index of the specified key in the storage array, or a
     *         value less than zero if the specified object is not found in the
     *         storage array; if <tt>fAdd</tt> is <tt>true</tt>, and the return
     *         value is less than zero, then the return value is the insertion
     *         point encoded as <tt>(-1 - index)</tt> where the object should be
     *         placed
     */
    private int find(Object o, boolean fAdd)
        {
        Object[]  aEntry    = m_aEntry;
        int       cElements = aEntry.length;
        if (cElements == 0)
            {
            return -1;
            }

        int       nHash     = o.hashCode();
        long      nNextHash = nHash & 0xFFFFFFFFL;
        int       cModulo   = cElements >>> 1;
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

            int    iTest = ((int) (nNextHash % cModulo)) << 1;
            Object oTest = aEntry[iTest];
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
                            aEntry[iInsert]   = oTest;
                            aEntry[iInsert+1] = aEntry[iTest+1];
                            aEntry[iTest]     = REMOVED;
                            aEntry[iTest+1]   = null;
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
        Object[] aEntry    = m_aEntry;
        int      cElements = aEntry.length;
        for (int i = 0; i < cElements; i += 2)
            {
            if (aEntry[i] == REMOVED)
                {
                aEntry[i] = null;
                }
            }

        // for each entry, see if it can be moved "forward" in its open hash
        // chain
        int    cRemoved   = 0;
        int    cMaxDepth  = m_cEntries == 0 ? 0 : 1;
        int    cModulo    = cElements >>> 1;
        Object oRemoved   = null;
        for (int i = 0; i < cElements; i += 2)
            {
            Object oKey = aEntry[i];
            if (oKey != null && oKey != REMOVED)
                {
                int  nHash     = oKey.hashCode();
                long nNextHash = nHash & 0xFFFFFFFFL;
                int  iTarget   = ((int) (nNextHash % cModulo)) << 1;
                if (i != iTarget)
                    {
                    // the object doesn't hash to this location; see if it can
                    // be "moved up" in its open hash chain
                    int cDepth   = 1;
                    int nHashInc = 0;
                    do
                        {
                        Object oTarget = aEntry[iTarget];
                        if (oTarget == null || oTarget == REMOVED)
                            {
                            // move the object to the target location,
                            // potentially replacing a REMOVED indicator, and
                            // place a REMOVED indicator (or null if the longest
                            // chain is still not more than length 1) in the
                            // location that the object was moved from
                            aEntry[iTarget]   = oKey;
                            aEntry[iTarget+1] = aEntry[i+1];
                            aEntry[i]         = oRemoved;
                            aEntry[i+1]       = null;
                            if (oRemoved == REMOVED && oTarget != REMOVED)
                                {
                                ++cRemoved;
                                }
                            break;
                            }

                        // calculate the next location in the open hash chain
                        nNextHash += nHashInc == 0 ? nHashInc = calculateHashIncrement(nHash) : nHashInc;
                        iTarget    = ((int) (nNextHash % cModulo)) << 1;
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
    private static int calculateHashIncrement(int nHash)
        {
        return OpenHashSet.calculateHashIncrement(nHash);
        }


    // ----- data members ------------------------------------------------------

    /**
     * An object that represents a key that has been removed.
     */
    static final Object REMOVED = OpenHashSet.REMOVED;

    /**
     * An object that represents a key or value whose value is the Java
     * <tt>null</tt> reference.
     */
    private static final Object NULL_SUBSTITUTE = OpenHashSet.NULL_SUBSTITUTE;

    /**
     * An empty array.
     */
    static final Object[] EMPTY = OpenHashSet.EMPTY;

    /**
     * This is the "percentage of full" (also known as the "load factor") that
     * the OpenHashMap targets to not exceed.
     */
    private static final double LOAD_FACTOR = OpenHashSet.LOAD_FACTOR;

    /**
     * This is the "percentage of full" -- including {@link #REMOVED} indicators
     * -- at which point the OpenHashMap will attempt to purge the REMOVED
     * indicators.
     */
    private static final double PURGE_FACTOR = OpenHashSet.PURGE_FACTOR;

    /**
     * A list of possible modulos to use.
     */
    private static final int[] PRIME_MODULO = OpenHashSet.PRIME_MODULO;

    /**
     * Stores the entries of the map using an "open hashing" algorithm, with
     * the layout being key/value/key/value/key/value ad-nauseam.
     */
    Object[] m_aEntry;

    /**
     * Tracks the number of entries currently stored in the map.
     */
    private int m_cEntries;

    /**
     * The number of {@link #REMOVED} indicators in the {@link #m_aEntry}
     * array.
     */
    private int m_cRemoved;

    /**
     * The maximum number of look-ups (the initial hash plus any number of
     * re-hashes) necessary to find any entry.
     */
    private int m_cMaxDepth;

    /**
     * The number of entries above which the map's storage must grow.
     */
    private int m_cGrowThreshold;

    /**
     * The number of entries below which the map's storage must shrink.
     */
    private int m_cShrinkThreshold;

    /**
     * The number of entries (including REMOVED indicators) above which the
     * map's storage must be purged of REMOVED indicators.
     */
    private int m_cPurgeThreshold;

    /**
     * A count of iterators that have been created but not exhausted. While an
     * iterator is "live", the map will suspend the optimization that moves
     * entries of the map towards the front of their open hash chains.
     */
    int m_cLiveIterators;
    }
