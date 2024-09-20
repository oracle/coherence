/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.collections.AbstractStableIterator;

import java.lang.ref.WeakReference;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
* An implementation of java.util.Map that is optimized for heavy concurrent use.
* <p>
* Retrieval and update operations to the map (e.g. <tt>get</tt>, <tt>put</tt>)
* are non-blocking and uncontended and will reflect some state of the map.
* Insert and remove operations to the map (e.g. <tt>put</tt>, <tt>remove</tt>)
* do require internal locking.
* <p>
* The entries in the map are internally segmented so as to permit a high level
* of concurrent "locked" operations without contention.
* <p>
* Retrievals and updates that run concurrently with bulk operations
* (e.g. <tt>putAll</tt>, <tt>clear</tt> may reflect insertion or removal of
* only some entries.  Iterators on the Map may also reflect concurrent updates
* made since the Iterator was created.  However, Iterators will not throw
* <tt>ConcurrentModificationException</tt>.
*
* @since Coherence 3.5
* @author rhl 2008.12.01
*/
public class SegmentedHashMap
        extends Base
        implements Map
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public SegmentedHashMap()
        {
        this(DEFAULT_INITIALSIZE, DEFAULT_LOADFACTOR, DEFAULT_GROWTHRATE);
        }

    /**
    * Construct a thread-safe hash map using the specified settings.
    *
    * @param cInitialBuckets  the initial number of hash buckets, 0 &lt; n
    * @param flLoadFactor     the acceptable load factor before resizing
    *                         occurs, 0 &lt; n, such that a load factor of
    *                         1.0 causes resizing when the number of entries
    *                         exceeds the number of buckets
    * @param flGrowthRate     the rate of bucket growth when a resize occurs,
    *                         0 &lt; n, such that a growth rate of 1.0 will
    *                         double the number of buckets:
    *                         bucketcount = bucketcount * (1 + growthrate)
    */
    public SegmentedHashMap(int cInitialBuckets, float flLoadFactor,
                            float flGrowthRate)
        {
        if (cInitialBuckets <= 0)
            {
            throw new IllegalArgumentException("SegmentedHashMap:  "
                + "Initial number of buckets must be greater than zero.");
            }
        if (flLoadFactor <= 0)
            {
            throw new IllegalArgumentException("SegmentedHashMap:  "
                + "Load factor must be greater than zero.");
            }
        if (flGrowthRate <= 0)
            {
            throw new IllegalArgumentException("SegmentedHashMap:  "
                + "Growth rate must be greater than zero.");
            }

        // initialize the hash map data structure
        m_aeBucket         = new Entry[cInitialBuckets];
        m_cSegmentCapacity =
            Math.max((int) (cInitialBuckets * flLoadFactor) / SEGMENT_COUNT,
                     MIN_SEGMENT_CAPACITY);
        m_flLoadFactor     = flLoadFactor;
        m_flGrowthRate     = flGrowthRate;

        // Initialize the segment control structures
        m_aSegment = new Segment[LOCK_COUNT];
        for (int i = 0; i < LOCK_COUNT; i++)
            {
            m_aSegment[i] = new Segment();
            }

        initializeActions();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the registered action for insert.
    *
    * @return the registered action for insert
    */
    protected InsertAction getInsertAction()
        {
        return m_actionInsert;
        }

    /**
    * Specify the action for insert.
    *
    * @param action  the action for insert
    */
    protected void setInsertAction(InsertAction action)
        {
        m_actionInsert = action;
        }

    /**
    * Return the registered action for getEntryInternal.
    *
    * @return the registered action for getEntryInternal
    */
    protected GetEntryAction getGetEntryAction()
        {
        return m_actionGetEntry;
        }

    /**
    * Specify the action for getEntryInternal.
    *
    * @param action  the action for getEntryInternal
    */
    protected void setGetEntryAction(GetEntryAction action)
        {
        m_actionGetEntry = action;
        }

    /**
    * Return the registered action for remove().
    *
    * @return the registered action for remove()
    */
    protected RemoveAction getRemoveAction()
        {
        return m_actionRemove;
        }

    /**
    * Specify the action for remove().
    *
    * @param action  the action for remove()
    */
    protected void setRemoveAction(RemoveAction action)
        {
        m_actionRemove = action;
        }

    /**
    * Return the registered action for containsValue().
    *
    * @return the registered action for containsValue()
    */
    protected ContainsValueAction getContainsValueAction()
        {
        return m_actionContainsValue;
        }

    /**
    * Specify the action for containsValue().
    *
    * @param action  the action for containsValue()
    */
    protected void setContainsValueAction(ContainsValueAction action)
        {
        m_actionContainsValue = action;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compares the specified object with this map for equality.  Returns
    * <tt>true</tt> if the given object is also a map and the two maps
    * represent the same mappings.  More formally, two maps <tt>t1</tt> and
    * <tt>t2</tt> represent the same mappings if
    * <tt>t1.keySet().equals(t2.keySet())</tt> and for every key <tt>k</tt>
    * in <tt>t1.keySet()</tt>, <tt> (t1.get(k)==null ? t2.get(k)==null :
    * t1.get(k).equals(t2.get(k))) </tt>.  This ensures that the
    * <tt>equals</tt> method works properly across different implementations
    * of the map interface.
    *
    * @param oThat  object to be compared for equality with this Map
    *
    * @return <tt>true</tt> if the specified object is equal to this Map
    */
    public boolean equals(Object oThat)
        {
        if (oThat == this)
            {
            return true;
            }

        if (!(oThat instanceof Map))
            {
            return false;
            }

        Map mapThat = (Map) oThat;
        if (mapThat.size() != this.size())
            {
            return false;
            }

        for (Iterator iter = mapThat.entrySet().iterator(); iter.hasNext(); )
            {
            Map.Entry entryThat = (Map.Entry) iter.next();
            Entry     entryThis = getEntryInternal(entryThat.getKey());

            if (!Base.equals(entryThis, entryThat))
                {
                return false;
                }
            }
        return true;
        }

    /**
    * Returns the hash code value for this Map. The hash code of a Map is
    * defined to be the sum of the hash codes of each entry in the Map's
    * <tt>entrySet()</tt> view.  This ensures that <tt>t1.equals(t2)</tt>
    * implies that <tt>t1.hashCode()==t2.hashCode()</tt> for any two maps
    * <tt>t1</tt> and <tt>t2</tt>, as required by the general contract of
    * Object.hashCode.
    *
    * @return the hash code value for this Map
    */
    public int hashCode()
        {
        int nHash = 0;
        for (Iterator iter = entrySet().iterator(); iter.hasNext(); )
            {
            nHash += iter.next().hashCode();
            }
        return nHash;
        }

    /**
    * Returns a String representation of this map.
    *
    * @return a String representation of this map
    */
    public String toString()
        {
        StringBuffer buf = new StringBuffer();
        buf.append("{");

        Iterator iter     = entrySet().iterator();
        boolean  fHasNext = iter.hasNext();
        while (fHasNext)
            {
            Entry  entry  = (Entry) iter.next();
            Object oKey   = entry.getKey();
            Object oValue = entry.getValue();

            buf.append(oKey == this ? "(this Map)" : oKey);
            buf.append("=");
            buf.append(oValue == this ? "(this Map)" : oValue);

            if (fHasNext = iter.hasNext())
                {
                buf.append(", ");
                }
            }
        buf.append("}");
        return buf.toString();
        }


    // ----- Map interface --------------------------------------------------

    /**
    * Returns the number of key-value mappings in this map.
    * <p>
    * Note: Unlike some Map implementations, the <tt>size()</tt> operation on
    * this map may be relatively expensive.
    *
    * @return the number of key-value mappings in this map
    */
    public int size()
        {
        // On a JSR-133 compliant JVM, doing a read from the atomic counter
        // will guarantee up-to-date values for the cEntries field on all
        // segments.  On non-compliant JVMs, we accept that the value returned
        // may be stale.
        m_atomicLocks.get();

        // If there are no synthetic entries, just sum the Entry counters.
        // Subclasses that may introduce synthetic entries must override
        // size() to provide a suitable implementation to account for
        // synthetics.
        int       cEntries = 0;
        Segment[] aSegment = m_aSegment;
        for (int i = 0; i < SEGMENT_COUNT; i++)
            {
            cEntries += aSegment[i].cEntries;
            }
        return cEntries;
        }

    /**
    * Returns <tt>true</tt> if this map contains no key-value mappings.
    *
    * @return <tt>true</tt> if this map contains no key-value mappings
    */
    public boolean isEmpty()
        {
        return size() == 0;
        }

    /**
    * Returns <tt>true</tt> iff this map contains a mapping for the specified
    * key.
    *
    * @param oKey  key whose presence in this map is to be tested
    *
    * @return <tt>true</tt> iff this map contains a mapping for the specified
    *         key
    */
    public boolean containsKey(Object oKey)
        {
        return getEntryInternal(oKey) != null;
        }

    /**
    * Returns <tt>true</tt> if this map maps one or more keys to the
    * specified value.
    *
    * @param oValue  value whose presence in this map is to be tested
    *
    * @return <tt>true</tt> if this map maps one or more keys to the
    *         specified value
    */
    public boolean containsValue(Object oValue)
        {
        ContainsValueAction actionEntry = getContainsValueAction();

        // if there was an intervening resize and the value was not found,
        // apply the action again as the Entry may have been missed.
        Entry[] aeBucket;
        do
            {
            Object oContext = actionEntry.instantiateContext(oValue);
            aeBucket = getStableBucketArray();

            invokeOnAllKeys(oContext, /*fLock*/ false, actionEntry);
            if (actionEntry.isFound(oContext))
                {
                return true;
                }
            }
        while (aeBucket != getStableBucketArray());
        return false;
        }

    /**
    * Returns the value to which this map maps the specified key.  Returns
    * <tt>null</tt> if the map contains no mapping for this key.  A return
    * value of <tt>null</tt> does not <i>necessarily</i> indicate that the map
    * contains no mapping for the key; it's also possible that the map
    * explicitly maps the key to <tt>null</tt>.  The <tt>containsKey</tt>
    * operation may be used to distinguish these two cases.
    *
    * @param oKey  key whose associated value is to be returned
    *
    * @return the value to which this map maps the specified key, or
    *         <tt>null</tt> if the map contains no mapping for this key
    */
    public Object get(Object oKey)
        {
        Entry entry = getEntryInternal(oKey);
        return (entry == null ? null : entry.getValue());
        }

    /**
    * Locate an Entry in the this map based on its key.
    *
    * @param key  the key object to search for
    *
    * @return the Entry or null if the entry does not exist
    */
    public Map.Entry getEntry(Object key)
        {
        return getEntryInternal(key);
        }

    /**
    * Associates the specified value with the specified key in this map.  If
    * the map previously contained a mapping for this key, the old value is
    * replaced.
    *
    * @param oKey    key with which the specified value is to be associated
    * @param oValue  value to be associated with the specified key
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key.  A <tt>null</tt> return can
    *         also indicate that the map previously associated <tt>null</tt>
    *         with the specified key
    */
    public Object put(Object oKey, Object oValue)
        {
        Object oOrig = putInternal(oKey, oValue);
        return oOrig == NO_VALUE ? null : oOrig;
        }

    /**
    * Copies all of the mappings from the specified map to this map.
    * <tt>putAll</tt> is semantically equivalent to:
    * <pre>
    * for (Iterator iter = mapOther.entrySet().iterator(); iter.hasNext(); )
    *     {
    *     Map.Entry entry = (Map.Entry) iter.next();
    *     put(entry.getKey(), entry.getValue());
    *     }
    * </pre>
    *
    * @param mapOther  mappings to be stored in this map
    */
    public void putAll(Map mapOther)
        {
        // If the insert is fairly small, take the naive approach of iterating
        // over the supplied Map's entrySet and inserting each entry.
        //
        // For a large insert, we would expect to have to lock each segment
        // one or more times with the naive approach.  Instead lock all of the
        // segments at once and do the updates/inserts while holding all of
        // the segment-locks.
        int nSizeOther = mapOther.size();
        if (nSizeOther < PUTALL_THRESHOLD)
            {
            // take the naive approach
            for (Iterator iter = mapOther.entrySet().iterator();
                 iter.hasNext();)
                {
                Map.Entry entryOther = (Map.Entry) iter.next();
                putInternal(entryOther.getKey(), entryOther.getValue());
                }
            }
        else
            {
            // Define an action to update or insert.
            // The action will run while all segments are locked.
            EntryAction actionPut = new InsertAction()
                    {
                    public Object invokeFound(Object oKey,
                                              Object oContext,
                                              Entry[] aeBucket,
                                              int nBucket,
                                              Entry entryPrev,
                                              Entry entryCur)
                        {
                        Object oResult = super.invokeFound(oKey,
                                                           oContext,
                                                           aeBucket,
                                                           nBucket,
                                                           entryPrev,
                                                           entryCur);
                        if (oResult == NO_VALUE)
                            {
                            // update
                            entryCur.setValueInternal(oContext);
                            }
                        return oResult;
                        }
                    };

            // lock all of the buckets, and perform the updates/inserts while
            // holding all of the segment locks.  For a big putAll(), this is
            // faster than locking each key individually.
            lockAllBuckets();
            try
                {
                if (nSizeOther > m_cSegmentCapacity * SEGMENT_COUNT)
                    {
                    // grow to at least the size of the user-supplied map
                    grow(nSizeOther);
                    }

                for (Iterator iter = mapOther.entrySet().iterator();
                     iter.hasNext();)
                    {
                    Map.Entry entryOther = (Map.Entry) iter.next();

                    // invoke the put action without additional locking
                    // because we have already locked all buckets
                    invokeOnKey(entryOther.getKey(), entryOther.getValue(),
                                /*fLock*/ false, actionPut);
                    }
                }
            finally
                {
                unlockAllBuckets();
                }
            }
        }

    /**
    * Removes the mapping for this key from this map if present.
    *
    * @param oKey  key whose mapping is to be removed from the map
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key.  A <tt>null</tt> return can
    *         also indicate that the map previously associated <tt>null</tt>
    *         with the specified key
    */
    public Object remove(Object oKey)
        {
        Object oOrig = removeInternal(oKey, getRemoveAction(), /*oContext*/null);
        return oOrig == NO_VALUE ? null : oOrig;
        }

    /**
    * Removes all mappings from this map.
    */
    public void clear()
        {
        // If there are no synthetic entries, just clear and reset the bucket
        // array.  Subclasses that may introduce synthetic entries must
        // override clear() to provide a suitable implementation to account
        // for synthetics.

        lockAllBuckets();
        try
            {
            m_aeBucket = new Entry[DEFAULT_INITIALSIZE];

            // clear the per-segment entry counters
            Segment[] aSegment = m_aSegment;
            for (int i = 0; i < SEGMENT_COUNT; i++)
                {
                aSegment[i].cEntries = 0;
                }
            m_cSegmentCapacity =
                Math.max((int) (DEFAULT_INITIALSIZE * m_flLoadFactor) / SEGMENT_COUNT,
                         MIN_SEGMENT_CAPACITY);
            }
        finally
            {
            unlockAllBuckets();
            }
        }

    /**
    * Returns a set view of the mappings contained in this map.  Each element
    * in the returned set is a <tt>Map.Entry</tt>.  The set is backed by the
    * map, so changes to the map are reflected in the set, and vice-versa.
    * If the map is modified while an iteration over the set is in progress,
    * the results of the iteration are undefined.  The set supports element
    * removal, which removes the corresponding mapping from the map, via the
    * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>,
    * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not support
    * the <tt>add</tt> or <tt>addAll</tt> operations.
    *
    * @return a set view of the mappings contained in this map.
    */
    public Set entrySet()
        {
        // no need to synchronize; it is acceptable that two threads would
        // instantiate an entry set
        EntrySet set = m_setEntries;
        if (set == null)
            {
            m_setEntries = set = instantiateEntrySet();
            }
        return set;
        }

    /**
    * Returns a Set view of the keys contained in this map.  The Set is
    * backed by the map, so changes to the map are reflected in the Set,
    * and vice-versa.  (If the map is modified while an iteration over
    * the Set is in progress, the results of the iteration are undefined.)
    * The Set supports element removal, which removes the corresponding entry
    * from the map, via the Iterator.remove, Set.remove,  removeAll
    * retainAll, and clear operations.  It does not support the add or
    * addAll operations.
    *
    * @return a Set view of the keys contained in this map
    */
    public Set keySet()
        {
        // no need to synchronize; it is acceptable that two threads would
        // instantiate a key set
        KeySet set = m_setKeys;
        if (set == null)
            {
            m_setKeys = set = instantiateKeySet();
            }
        return set;
        }

    /**
    * Returns a collection view of the values contained in this map.  The
    * collection is backed by the map, so changes to the map are reflected in
    * the collection, and vice-versa.  If the map is modified while an
    * iteration over the collection is in progress, the results of the
    * iteration are undefined.  The collection supports element removal,
    * which removes the corresponding mapping from the map, via the
    * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
    * <tt>removeAll</tt>, <tt>retainAll</tt> and <tt>clear</tt> operations.
    * It does not support the add or <tt>addAll</tt> operations.
    *
    * @return a collection view of the values contained in this map
    */
    public Collection values()
        {
        // no need to synchronize; it is acceptable that two threads would
        // instantiate a values collection
        ValuesCollection col = m_colValues;
        if (col == null)
            {
            m_colValues = col = instantiateValuesCollection();
            }
        return col;
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Initialize the EntryAction's for this map.
    */
    protected void initializeActions()
        {
        /* getEntryInternal action */
        setGetEntryAction(instantiateGetEntryAction());

        /* insert action */
        setInsertAction(instantiateInsertAction());

        /* remove action */
        setRemoveAction(instantiateRemoveAction());

        /* containsValue action */
        setContainsValueAction(instantiateContainsValueAction());
        }

    /**
    * Locate an Entry in the hash map based on its key.
    *
    * @param oKey  the key object to search for
    *
    * @return the Entry or null
    */
    protected Entry getEntryInternal(Object oKey)
        {
        return getEntryInternal(oKey, /*fSynthetic*/ false);
        }

    /**
    * Locate an Entry in the hash map based on its key.
    *
    * @param oKey        the key object to search for
    * @param fSynthetic  include synthetic Entry objects representing keys
    *                    that are not contained in the map
    *
    * @return the Entry or <tt>null</tt>
    */
    protected Entry getEntryInternal(Object oKey, boolean fSynthetic)
        {
        Object  oResult;
        Boolean FSynthetic = fSynthetic ? Boolean.TRUE : Boolean.FALSE;
        do
            {
            oResult = invokeOnKey(oKey, FSynthetic,
                                  /*fLock*/ false, getGetEntryAction());
            }
        while (oResult == NO_VALUE);
        return (Entry) oResult;
        }

    /**
    * Associates the specified value with the specified key in this map.  If
    * the map previously contained a mapping for this key, the old value is
    * replaced.
    *
    * @param oKey     key with which the specified value is to be associated
    * @param oValue   value to be associated with the specified key
    *
    * @return previous value associated with specified key, or
    *         <tt>NO_VALUE</tt> if there was no mapping for key.  A
    *         <tt>null</tt> return indicates that the map previously
    *         associated <tt>null</tt> with the specified key
    */
    protected Object putInternal(Object oKey, Object oValue)
        {
        return putInternal(oKey, oValue, /*fOnlyIfAbsent*/false);
        }

    /**
    * Associates the specified value with the specified key in this map.  If
    * the map previously contained a mapping for this key, the old value is
    * replaced.
    *
    * @param oKey           key with which the specified value is to be associated
    * @param oValue         value to be associated with the specified key
    * @param fOnlyIfAbsent  if true, perform a logical insert only; no action
    *                       if the key already exists
    *
    * @return previous value associated with specified key, or
    *         <tt>NO_VALUE</tt> if there was no mapping for key.  A
    *         <tt>null</tt> return indicates that the map previously
    *         associated <tt>null</tt> with the specified key
    */
    protected Object putInternal(Object oKey, Object oValue, boolean fOnlyIfAbsent)
        {
        while (true)
            {
            Entry entry = getEntryInternal(oKey, /*fSynthetic*/ true);

            if (entry == null || entry.isSynthetic())
                {
                // If an entry is not found, or the entry is synthetic, it is a
                // logical insert (and must be done under a segment lock).
                Object oResult = invokeOnKey(oKey, oValue, /*fLock*/ true,
                                             getInsertAction());
                if (oResult == NO_VALUE)
                    {
                    // another thread inserted; try again
                    continue;
                    }

                if (entry == null)
                    {
                    // An Entry was added.  Check to see if the map should be
                    // grown in order to encourage balanced buckets.
                    ensureLoadFactor(getSegmentForKey(oKey));
                    }

                // successfully inserted
                return NO_VALUE;
                }

            // In the case of an update, it is possible that the Entry is
            // being removed or the value is being updated by another thread
            return !fOnlyIfAbsent ? entry.setValueInternal(oValue) : entry.getValue();
            }
        }

    /**
    * Removes the mapping for this key from this map if present.
    *
    * @param oKey          key whose mapping is to be removed from the map
    * @param actionRemove  the EntryAction to apply
    * @param oContext      the context for the remove action
    *
    * @return previous value associated with specified key, or
    *         <tt>NO_VALUE</tt> if there was no mapping for key.  A
    *         <tt>null</tt> return indicates that the map previously
    *         associated <tt>null</tt> with the specified key.
    */
    protected Object removeInternal(Object oKey, EntryAction actionRemove, Object oContext)
        {
        return invokeOnKey(oKey, oContext, /*fLock*/ true, actionRemove);
        }

    /**
    * Apply the specified toArray() action to the entries in the map.  The
    * toArray() action is not applied under any segment lock and is expected
    * to accept a <tt>List</tt> instance as a context.
    *
    * @param  action  the toArray() action
    * @param  a       the array into which the elements of the Collection
    *                 are to be stored, if it is big enough; otherwise, a
    *                 new array of the same runtime type is allocated for
    *                 this purpose
    *
    * @return an array containing the elements returned by the specified
    *         action
    *
    * @throws ArrayStoreException if the runtime type of the specified array
    *         is not a supertype of the runtime type of the elements returned
    *         by the specified action
    */
    protected Object[] toArrayInternal(IterableEntryAction action, Object[] a)
        {
        List    list = new ArrayList();
        Entry[] aeBucket;

        // if there was an intervening resize, apply the action again as
        // Entry's may have been missed or repeated during the resize.
        do
            {
            list.clear();
            aeBucket = getStableBucketArray();
            invokeOnAllKeys(list, /*fLock*/ false, action);
            }
        while (aeBucket != getStableBucketArray());
        return list.toArray(a == null ? new Object[list.size()] : a);
        }

    /**
    * Check whether or not the specified segment is overloaded and if so,
    * grow the bucket array (which suggests with high probability that the
    * per-segment load will decrease).
    *
    * @param segment  the segment to ensure the load-factor for
    */
    protected void ensureLoadFactor(Segment segment)
        {
        // Note: reading the segment size is a dirty read.  The caller is
        // expected to have recently held the segment lock.
        if (segment.cEntries > m_cSegmentCapacity)
            {
            Entry[] aeBucket = m_aeBucket;

            lockAllBuckets();
            try
                {
                // check that some other thread hasn't already grown
                if (m_aeBucket == aeBucket)
                    {
                    grow();
                    }
                }
            finally
                {
                unlockAllBuckets();
                }
            }
        }

    /**
    * Resize the bucket array, rehashing all Entries.
    * <p>
    * Note: caller of this method is expected to hold locks on all segments of
    *       the map while making this call.
    */
    protected void grow()
        {
        Entry[] aeOld = m_aeBucket;
        int     cOld  = aeOld.length;

        // check if there is no more room to grow
        if (cOld >= BIGGEST_MODULO)
            {
            return;
            }

        // calculate growth
        int cNew = (int) Math.min((long) (cOld * (1F + m_flGrowthRate)),
                                  BIGGEST_MODULO);

        grow(cNew);
        }

    /**
    * Resize the bucket array to the specified size, rehashing all Entries.
    * <p>
    * Note: caller of this method is expected to hold locks on all segments of
    *       the map while making this call.
    *
    * @param cNew  the minimum size to attempt to grow to
    */
    protected synchronized void grow(int cNew)
        {
        if (isActiveIterator())
            {
            // don't grow if there are active iterators
            return;
            }

        synchronized (RESIZING)
            {
            // store off the old bucket array
            Entry[] aeOld = m_aeBucket;
            int     cOld  = aeOld.length;

            // use NO_ENTRIES to signify that a resize is taking place
            m_aeBucket = NO_ENTRIES;

            if (cNew <= cOld)
                {
                // very low growth rate or very low initial size.  In either
                // case, ensure that at least some growth happens.
                cNew = cOld + 1;
                }

            // use a prime number at least as big than the new size
            int[] aiModulo = PRIME_MODULO;
            int   cModulos = aiModulo.length;
            for (int i = 0; i < cModulos; ++i)
                {
                int iModulo = aiModulo[i];
                if (iModulo >= cNew)
                    {
                    cNew = iModulo;
                    break;
                    }
                }

            // create a new bucket array; in the case of an OutOfMemoryError
            // be sure to restore the old bucket array
            Entry[] aeNew;
            try
                {
                aeNew = new Entry[cNew];
                }
            catch (OutOfMemoryError e)
                {
                m_aeBucket = aeOld;
                throw e;
                }

            // rehash
            Segment[] aSegment = m_aSegment;
            for (int i = 0; i < cOld; ++i)
                {
                Entry entry       = aeOld[i];
                int   nSegmentOld = getSegmentIndex(i);

                while (entry != null)
                    {
                    // store off the next Entry
                    // (it is going to get hammered otherwise)
                    Entry entryNext = entry.nextEntry(true);

                    // rehash the Entry into the new bucket array
                    Object oKey        = entry.getKey();
                    int    nHash       = oKey == null ? 0 : oKey.hashCode();
                    int    nBucket     = getBucketIndex(nHash, cNew);
                    int    nSegmentNew = getSegmentIndex(nBucket);

                    entry.setNext(aeNew[nBucket]);
                    aeNew[nBucket] = entry;

                    // update the counters
                    --aSegment[nSegmentOld].cEntries;
                    ++aSegment[nSegmentNew].cEntries;

                    // process next Entry in the old list
                    entry = entryNext;
                    }
                }

            // store updated bucket array
            m_cSegmentCapacity =
                Math.max((int) (cNew * m_flLoadFactor) / SEGMENT_COUNT,
                         MIN_SEGMENT_CAPACITY);
            m_aeBucket         = aeNew;
            }
        }

    /**
    * Perform an action on all Entries in the map.  The action is provided as
    * an <tt>EntryAction</tt> and (if the <tt>fLock</tt> is specified) it are
    * invoked while holding the locks for all segments.
    * <p>
    * The semantics of <tt>invokeOnAllKeys</tt> are equivalent to:
    * <pre>
    * for (Iterator iter = entrySet().iterator(); iter.hasNext(); )
    *     {
    *     Entry entry = (Entry) iter.next();
    *     actionEntry.invokeFound(...);
    *     }
    * return oContext;
    * </pre>
    * Except that if <tt>fLock</tt> is specified, it is performed atomically
    * while holding all segment-locks.
    *
    * @param oContext         opaque context for the specified action
    * @param fLock            <tt>true</tt> if all segment-locks should be
    *                         acquired before invoking the specified action
    * @param actionEntry      the action to perform for each entry
    *
    * @return the specified opaque context
    */
    protected Object invokeOnAllKeys(Object oContext, boolean fLock,
                                     IterableEntryAction actionEntry)
        {
        if (fLock)
            {
            lockAllBuckets();
            }

        try
            {
            Entry[] aeBucket = getStableBucketArray();
            int     cBuckets = aeBucket.length;

            for (int iBucket = 0; iBucket < cBuckets; ++iBucket)
                {
                // walk all entries in the bucket
                Entry entry     = aeBucket[iBucket];
                Entry entryPrev = null;

                while (entry != null)
                    {
                    // invoke the action on each Entry
                    Entry entryNext = entry.nextEntry(/*fSynthetic*/ true);

                    actionEntry.invokeFound(entry.getKey(), oContext, aeBucket,
                                            iBucket, entryPrev, entry);
                    if (actionEntry.isComplete(oContext))
                        {
                        return oContext;
                        }

                    // recalculate entryPrev for the next Entry.  Have to
                    // consider that the action may have removed this Entry
                    // from the bucket-list.
                    entryPrev = ((entryPrev == null ?
                                  aeBucket[iBucket] != entry :
                                  entryPrev.nextEntry(true) != entry) ?
                                 entryPrev : entry);
                    entry = entryNext;
                    }
                }
            }
        finally
            {
            if (fLock)
                {
                unlockAllBuckets();
                }
            }
        return oContext;
        }

    /**
    * Perform an action on the specified key.  The action operation is
    * provided as an EntryAction and (if <tt>fLock</tt> is specified), is
    * invoked while holding the appropriate segment lock for the key.
    * <p>
    * The semantics of <tt>invokeOnKey</tt> are equivalent to:
    * <pre>
    * Object oResult;
    * if (containsKey(oKey))
    *     {
    *     oResult = action.invokeFound(...);
    *     }
    * else
    *     {
    *     oResult = action.invokeNotFound(...);
    *     }
    * return oResult;
    * </pre>
    * Except that if <tt>fLock</tt> is specified, it is performed atomically
    * while holding the segment-lock.
    *
    * @param oKey      the key to act on
    * @param oContext  opaque context for the specified action
    * @param fLock     true iff the segment should be locked before invoking
    *                  the specified action
    * @param action    the action to invoke
    *
    * @return the result of performing the action
    */
    protected Object invokeOnKey(Object oKey, Object oContext,
                                 boolean fLock, EntryAction action)
        {
        int nHash = (oKey == null ? 0 : oKey.hashCode());

        while (true)
            {
            Entry[] aeBucket  = getStableBucketArray();
            int     cBuckets  = aeBucket.length;
            int     nBucket   = getBucketIndex(nHash, cBuckets);

            if (fLock)
                {
                lockBucket(nBucket);
                }

            try
                {
                if (aeBucket != m_aeBucket)
                    {
                    continue;
                    }

                // walk the linked list of entries (open hash) in the bucket
                Entry entryCur  = aeBucket[nBucket];
                Entry entryPrev = null;

                while (entryCur != null)
                    {
                    Entry entryNext = entryCur.nextEntry(true);

                    // optimization:  check hash first
                    if (nHash == entryCur.m_nHash &&
                        Base.equals(oKey, entryCur.getKey()))
                        {
                        return action.invokeFound(oKey, oContext, aeBucket,
                                                  nBucket, entryPrev, entryCur);
                        }

                    entryPrev = entryCur;
                    entryCur  = entryNext;
                    }
                return action.invokeNotFound(oKey, oContext, aeBucket, nBucket);
                }
            finally
                {
                if (fLock)
                    {
                    unlockBucket(nBucket);
                    }
                }
            }
        }

    /**
    * Calculate the bucket number for a particular hash code.
    *
    * @param nHash     the hash code
    * @param cBuckets  the number of buckets
    *
    * @return the bucket index for the specified hash code
    */
    protected int getBucketIndex(int nHash, int cBuckets)
        {
        return (int) ((((long) nHash) & 0xFFFFFFFFL) % ((long) cBuckets));
        }

    /**
    * Calculate the segment index for the specified bucket.
    *
    * @param nBucket  the bucket number
    *
    * @return the segment index
    */
    protected int getSegmentIndex(int nBucket)
        {
        return nBucket % SEGMENT_COUNT;
        }

    /**
    * Return the Segment object for the specified key.
    *
    * @param oKey  the key
    *
    * @return the Segment for the specified key
    */
    protected Segment getSegmentForKey(Object oKey)
        {
        Entry[] aeBucket = getStableBucketArray();
        int     cBuckets = aeBucket.length;
        int     nHash    = (oKey == null ? 0 : oKey.hashCode());
        int     nBucket  = getBucketIndex(nHash, cBuckets);
        return m_aSegment[getSegmentIndex(nBucket)];
        }

    /**
    * Get the bucket array, or if a resize is occurring, wait for the resize
    * to complete and return the new bucket array.
    *
    * @return the latest bucket array
    */
    protected Entry[] getStableBucketArray()
        {
        // get the bucket array
        Entry[] aeBucket = m_aeBucket;

        if (aeBucket == NO_ENTRIES)
            {
            // wait for the ongoing resize to complete
            synchronized (RESIZING)
                {
                return m_aeBucket;
                }
            }
        return aeBucket;
        }

    /**
    * Register the activation of an Iterator.
    * <p>
    * Note: The map will not grow while there are any active iterators.
    *
    * @param iter  the activated iterator
    */
    protected synchronized void iteratorActivated(Iterator iter)
        {
        Object oIterActive = m_oIterActive;
        if (oIterActive == null)
            {
            // optimize for single active iterator
            m_oIterActive = new WeakReference(iter);
            }
        else if (oIterActive instanceof WeakReference)
            {
            Object oPrev = ((WeakReference) oIterActive).get();
            if (oPrev == null)
                {
                // previous Iterator was GC'd, replace it
                m_oIterActive = new WeakReference(iter);
                }
            else
                {
                // switch from single to multiple active iterators;
                // WeakHashMap will automatically clean up any GC'd iterators
                Map map = new WeakHashMap();
                m_oIterActive = map;
                map.put(oPrev, null);
                map.put(iter,  null);
                }
            }
        else
            {
            // it's a map, add additional active iterator
            ((Map) oIterActive).put(iter, null);
            }
        }

    /**
    * Release the (formerly-active) Iterator.
    * <p>
    * Note: This method could be used to accelerate the destruction of
    *       unexhausted iterators.
    *
    * @param iter  the iterator to be released
    */
    public synchronized void releaseIterator(Iterator iter)
        {
        Object oIterActive = m_oIterActive;
        if (oIterActive instanceof WeakReference)
            {
            // there is only one active iterator, it must be this one
            if (((WeakReference) oIterActive).get() == iter)
                {
                m_oIterActive = null;
                }
            }
        else
            {
            // remove one of many active iterators
            ((Map) oIterActive).remove(iter);

            // once we go to an Iterator Map we don't switch back; the
            // assumption is that if multiple concurrent iterators are
            // allocated once, they will be allocated again, so avoid
            // thrashing on frequent WeakHashMap creation
            }
        }

    /**
    * Determine if there are any active Iterators, which may mean that they
    * are in the middle of iterating over the Map.
    *
    * @return true iff there is at least one active Iterator
    */
    protected synchronized boolean isActiveIterator()
        {
        Object oIterActive = m_oIterActive;

        if (oIterActive == null)
            {
            return false;
            }
        else if (oIterActive instanceof WeakReference)
            {
            return ((WeakReference) oIterActive).get() != null;
            }
        else
            {
            return !((Map) oIterActive).isEmpty();
            }
        }

    /**
    * Attempt to lock the segment corresponding to the specified bucket.
    *
    * @param nBucket  the bucket index
    *
    * @return true iff the segment was successfully locked
    */
    protected boolean lockBucket(int nBucket)
        {
        return lockSegment(getSegmentIndex(nBucket), true);
        }

    /**
    * Attempt to lock the specified segment.
    *
    * @param nSegment  the segment to lock
    * @param fBlock    should we block on trying to lock the segment
    *
    * @return true iff the segment was successfully locked
    */
    protected boolean lockSegment(int nSegment, boolean fBlock)
        {
        long       maskLock    = 1L << nSegment;
        AtomicLong atomicLocks = m_atomicLocks;

        // optimization: assume nothing is already locked
        if (atomicLocks.compareAndSet(LOCKS_NONE, maskLock))
            {
            // optimistic case worked; we locked the segment
            return true;
            }

        // check to see if everything is locked or a lock-all is pending, or
        // if the desired segment is locked
        for (int cAttempts = 0; true;)
            {
            long lLocks = atomicLocks.get();
            if ((lLocks & (LOCK_ALL_PENDING | maskLock)) != 0L)
                {
                if (fBlock)
                    {
                    // spin a few times before waiting
                    if ((++cAttempts % SEGMENT_LOCK_MAX_SPIN) == 0)
                        {
                        contendForSegment(nSegment);
                        }
                    continue;
                    }
                else
                    {
                    return false;
                    }
                }

            if (atomicLocks.compareAndSet(lLocks, lLocks | maskLock))
                {
                return true;
                }
            }
        }

    /**
    * Unlock the segment corresponding to the specified bucket that was
    * previously locked using the {@link #lockBucket} method.
    *
    * @param nBucket  the bucket to unlock
    */
    protected void unlockBucket(int nBucket)
        {
        unlockSegment(getSegmentIndex(nBucket));
        }

    /**
    * Unlock the specified segment previously locked using the {@link
    * #lockSegment} method.
    *
    * @param nSegment  the segment to unlock
    */
    protected void unlockSegment(int nSegment)
        {
        long       maskLock    = 1L << nSegment;
        AtomicLong atomicLocks = m_atomicLocks;
        long       lLocks      = maskLock;

        // make an optimistic attempt (first loop-iteration) in assuming that
        // nothing other than nSegment is locked
        while (!atomicLocks.compareAndSet(lLocks, lLocks & ~maskLock))
            {
            lLocks = atomicLocks.get();
            }

        // Check to see if there are other threads contending for the segment
        // that we just unlocked that need to be notified
        Segment segment = m_aSegment[nSegment];
        if (segment.fContend)
            {
            synchronized (segment)
                {
                if (segment.fContend)
                    {
                    segment.fContend = false;
                    segment.notifyAll();
                    }
                }
            }
        }

    /**
    * Wait for a segment to be unlocked.
    *
    * @param nSegment  the segment-lock to be waited for
    */
    protected void contendForSegment(int nSegment)
        {
        long    maskLock = 1L << nSegment;
        Segment segment  = m_aSegment[nSegment];

        synchronized (segment)
            {
            // set the contended bit for the segment
            segment.fContend = true;

            // check that the segment is still locked
            if ((m_atomicLocks.get() & maskLock) == 0)
                {
                // The segment was unlocked; try to lock again.
                return;
                }

            // At this point, we know the lock and contend bits are set, so we
            // know that there is a segment-lock holder that should notify us
            // when they unlock it.
            try
                {
                Blocking.wait(segment, 1000);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw Base.ensureRuntimeException(e, "Segment lock interrupted");
                }
            }
        }

    /**
    * Lock everything.  This method will not return until the lock is placed.
    * It must not be called on a thread that could already hold a lock.
    */
    protected void lockAllBuckets()
        {
        int cAttempts = 0;
        while (!lockAllSegments(LOCKS_NONE))
            {
            // wait for (possible) resize to finish
            getStableBucketArray();

            // spin a few times before waiting
            if ((++cAttempts % SEGMENT_LOCK_MAX_SPIN) == 0)
                {
                contendForSegment(LOCK_ALL_PENDING_IDX);
                }
            }
        }

    /**
    * Lock everything, assuming that the segment for the specified bucket has
    * already been locked.
    *
    * @param nBucketAlreadyLocked  the bucket that was already locked.
    *
    * @return <tt>false</tt> if the operation failed because another thread
    *         was also trying to lock everything (indicating potential
    *         deadlock)
    */
    protected boolean lockAllBuckets(int nBucketAlreadyLocked)
        {
        return lockAllSegments(1L << (getSegmentIndex(nBucketAlreadyLocked)));
        }

    /**
    * Unlock everything.
    */
    protected void unlockAllBuckets()
        {
        unlockAllSegments(LOCKS_NONE);
        }

    /**
    * Unlock everything, leaving only the segment for the specified bucket
    * locked.
    *
    * @param nBucketLeaveLocked  the bucket that was already locked
    */
    protected void unlockAllBuckets(int nBucketLeaveLocked)
        {
        unlockAllSegments(1L << (getSegmentIndex(nBucketLeaveLocked)));
        }

    /**
    * Lock all segments except for the specified segments that have already
    * been locked by the calling thread.
    *
    * @param lLocksHeld  the bit-mask representing all segment-locks that the
    *                    calling thread already holds
    *
    * @return false if the operation failed because another thread was also
    *         trying to lock everything (indicating potential deadlock)
    */
    protected boolean lockAllSegments(long lLocksHeld)
        {
        AtomicLong atomicLocks = m_atomicLocks;

        // the passed-in mask must already be locked
        /*
        Base.azzert(lLocksHeld == LOCKS_NONE ||
                    (atomicLocks.getCount() & lLocksHeld) == lLocksHeld);
        */

        // optimistic attempt: lock everything
        if (atomicLocks.compareAndSet(lLocksHeld, LOCKS_ALL))
            {
            // everything else was available and is now locked
            return true;
            }

        // pessimistic attempt: attempt to lock the "intent" lock
        while (true)
            {
            long lLocks = atomicLocks.get();
            if ((lLocks & LOCK_ALL_PENDING) != 0L)
                {
                return false;
                }

            if (atomicLocks.compareAndSet(lLocks, lLocks | LOCK_ALL_PENDING))
                {
                lLocksHeld |= LOCK_ALL_PENDING;
                break;
                }
            }

        // lock all remaining unlocked segments
        int cAttempts  = 0;
        while (lLocksHeld != LOCKS_ALL)
            {
            // find what locks are held in total by this and all other threads
            long lLocks = atomicLocks.get();

            // request all the locks that are not currently held by any thread
            if (atomicLocks.compareAndSet(lLocks, LOCKS_ALL))
                {
                // we just locked all of the available segment-locks; continue
                // until we have locked all of the segment-locks.
                lLocksHeld |= ~lLocks;
                }
            else
                {
                // occasionally stop spinning and yield
                if ((++cAttempts % SEGMENT_LOCK_MAX_SPIN) == 0)
                    {
                    Thread.yield();
                    }
                }
            }

        return true;
        }

    /**
    * Unlock all segments, except the segment-locks indicated by the specified
    * bit-vector.  This method must only be called by a thread if that thread
    * has successfully called {@link #lockAllSegments}.
    *
    * @param lLocksKeep  the segment-locks to keep locked
    */
    protected void unlockAllSegments(long lLocksKeep)
        {
        if (!m_atomicLocks.compareAndSet(LOCKS_ALL, lLocksKeep))
            {
            throw new IllegalStateException();
            }

        // check to see if any threads are contending for segment-locks that
        // we just unlocked (or the lock-all intention bit).
        Segment[] aSegment = m_aSegment;
        for (int i = 0; i < LOCK_COUNT; i++)
            {
            long    maskLock = 1L << i;
            Segment segment  = aSegment[i];

            // if we are not keeping this segment-lock and it is being waited
            // on, we need to notify the waiters
            if ((lLocksKeep & maskLock) == 0 && segment.fContend)
                {
                synchronized (segment)
                    {
                    if (segment.fContend)
                        {
                        segment.fContend = false;
                        segment.notifyAll();
                        }
                    }
                }
            }
        }


    // ----- inner interface: EntryAction -----------------------------------

    /**
    * An EntryAction encapsulates a logical action to be executed in the
    * context of a key (that may or may not exist in the map).  If an Entry
    * exists in the map for the key, <tt>invokeFound</tt> is called; otherwise
    * <tt>invokeNotFound</tt> is called.
    * <p>
    * EntryAction instances are state-aware and are invoked with an opaque
    * context object supplied by the action client.  The meanings of the
    * supplied context and return-values are polymorphic in EntryAction type.
    * Depending on the EntryAction type, it may or may not be invoked while
    * holding the segment lock for the key.
    * <p>
    * An EntryAction should not acquire additional segment-locks.
    */
    protected static interface EntryAction
        {
        /**
        * Invoke some action, holding the segment lock, when a matching Entry
        * is found.
        *
        * @param oKey       the key to which the action is applied
        * @param oContext   opaque context specific to the action
        * @param aeBucket   the bucket array
        * @param nBucket    the index into the bucket array
        * @param entryPrev  the Entry object immediately preceding the
        *                   Entry that was found, or null
        * @param entryCur   the Entry object that was found
        *
        * @return an opaque result value
        */
        public Object invokeFound(Object oKey, Object oContext,
                                  Entry[] aeBucket, int nBucket,
                                  Entry entryPrev, Entry entryCur);

        /**
        * Invoke some action, holding the segment lock, when no matching Entry
        * is found.
        *
        * @param oKey      the key to which the action is applied
        * @param oContext  opaque context specific to the action
        * @param aeBucket  the bucket array
        * @param nBucket   the index into the bucket array
        *
        * @return an opaque result value
        */
        public Object invokeNotFound(Object oKey, Object oContext,
                                     Entry[] aeBucket, int nBucket);
        }


    // ----- inner interface: IterableEntryAction ---------------------------
    /**
    * IterableEntryAction is an EntryAction that is suitable for applying to
    * all keys in a map.
    */
    protected static interface IterableEntryAction
            extends EntryAction
        {
        /**
        * Return true iff further key evaluations for the given context are
        * known to be unnecessary.
        *
        * @param oContext  the action context
        *
        * @return true iff further evaluations are unnecessary
        */
        public boolean isComplete(Object oContext);
        }


    // ----- inner class: EntryActionAdapter --------------------------------

    /**
    * EntryActionAdapter is a convenience class that provides default
    * implementations for the EntryAction and IterableEntryAction interface
    * methods.
    */
    protected static abstract class EntryActionAdapter
            implements EntryAction, IterableEntryAction
        {
        /**
        * {@inheritDoc}
        */
        public Object invokeFound(Object oKey, Object oContext,
                                  Entry[] aeBucket, int nBucket,
                                  Entry entryPrev, Entry entryCur)
            {
            return NO_VALUE;
            }

        /**
        * {@inheritDoc}
        */
        public Object invokeNotFound(Object oKey, Object oContext,
                                     Entry[] aeBucket, int nBucket)
            {
            return NO_VALUE;
            }

        /**
        * {@inheritDoc}
        */
        public boolean isComplete(Object oContext)
            {
            return false;
            }
        }


    // ----- inner class: GetEntryAction ------------------------------------

    /**
    * Factory for GetEntryAction
    *
    * @return a GetEntryAction
    */
    protected GetEntryAction instantiateGetEntryAction()
        {
        return new GetEntryAction();
        }

    /**
    * Action support for getEntryInternal.  The action performs an Entry
    * lookup by key and is not required to run while holding segment locks.
    * <p>
    * The context object for a GetEntryAction is either <tt>Boolean.TRUE</tt>
    * or <tt>Boolean.FALSE</tt> indicating whether or not to return synthetic
    * entries.
    * <p>
    * The result of invoking a GetEntryAction is the (possibly synthetic)
    * Entry corresponding to a given key, <tt>null</tt> if no matching Entry
    * is found, or <tt>NO_VALUE</tt> indicating that a concurrent resize
    * occurred, and the operation must be repeated.
    */
    protected class GetEntryAction
            implements EntryAction
        {
        /**
        * {@inheritDoc}
        */
        public Object invokeFound(Object oKey, Object oContext,
                                  Entry[] aeBucket, int nBucket,
                                  Entry entryPrev, Entry entryCur)
            {
            // An Entry object was found.  If synthetic entries are to be
            // returned, or the found entry is not synthetic, then return it.
            boolean fSynthetic = oContext == Boolean.TRUE;
            return (fSynthetic || !entryCur.isSynthetic()) ? entryCur : null;
            }

        /**
        * {@inheritDoc}
        */
        public Object invokeNotFound(Object oKey, Object oContext,
                                     Entry[] aeBucket, int nBucket)
            {
            // If a resize occurred, the oKey may be in the map and was
            // reshuffled while we were looking for it; we know a resize
            // occurred iff the hash bucket array changed, in which case we
            // must return NO_VALUE so that the lookup is retried.
            return aeBucket == m_aeBucket ? null : NO_VALUE;
            }
        }


    // ----- inner class: InsertAction --------------------------------------

    /**
    * Factory for InsertAction
    *
    * @return an InsertAction
    */
    protected InsertAction instantiateInsertAction()
        {
        return new InsertAction();
        }

    /**
    * Action support for insert.  The action performs locked insert, and is
    * expected to run while holding the segment-lock for the specified key.
    * <p>
    * The context object for an InsertAction is the value object to insert in
    * the map, or <tt>NO_VALUE</tt> to insert a synthetic Entry.
    * <p>
    * The result of invoking an InsertAction is the (possibly synthetic) Entry
    * object that was inserted for the specified key, or <tt>NO_VALUE</tt>
    * indicating that a mapping for the key already exists in the map.
    */
    protected class InsertAction
            implements EntryAction
        {
        /**
        * {@inheritDoc}
        */
        public Object invokeFound(Object oKey, Object oContext,
                                  Entry[] aeBucket, int nBucket,
                                  Entry entryPrev, Entry entryCur)
            {
            if (entryCur.isSynthetic())
                {
                // logical insert
                entryCur.setValueInternal(oContext);

                // return the (logically) newly inserted entry
                return entryCur;
                }

            // An entry was found under put(); return NO_VALUE
            return NO_VALUE;
            }

        /**
        * {@inheritDoc}
        */
        public Object invokeNotFound(Object oKey, Object oContext,
                                     Entry[] aeBucket, int nBucket)
            {
            // No entry object was found under put(); having locked the
            // segment, we are now free to insert a new Entry.
            Object oValue = oContext;
            int    nHash  = (oKey == null ? 0 : oKey.hashCode());

            // instantiate and configure a new Entry
            Entry entry = instantiateEntry(oKey, oValue, nHash);

            // put the Entry in at the front of the list of entries for that
            // bucket
            entry.setNext(aeBucket[nBucket]);
            aeBucket[nBucket] = entry;

            // increment the entry counter for the segment
             ++m_aSegment[getSegmentIndex(nBucket)].cEntries;

            // return the newly created entry
            return entry;
            }
        }


    // ----- inner class: RemoveAction --------------------------------------

    /**
    * Factory for RemoveAction
    *
    * @return a RemoveAction
    */
    protected RemoveAction instantiateRemoveAction()
        {
        return new RemoveAction();
        }

    /**
    * Action support for remove().  The action performs a locked remove, and is
    * expected to run while holding the segment-lock for the specified key.
    * <p>
    * The context object for a RemoveAction is unused.
    * <p>
    * The result of invoking a RemoveAction is the previous value associated
    * with the specified key, or <tt>NO_VALUE</tt> if no mapping for the key
    * exists in the map.  Note that a synthetic Entry does not represent a
    * key-value mapping, so <tt>NO_VALUE</tt> is returned if a matching
    * synthetic Entry is found.
    */
    protected class RemoveAction
            extends EntryActionAdapter
        {
        /**
        * {@inheritDoc}
        */
        public Object invokeFound(Object oKey, Object oContext,
                                  Entry[] aeBucket, int nBucket,
                                  Entry entryPrev, Entry entryCur)
            {
            // Remove the entry from the bucket-list
            Entry entryNext = entryCur.nextEntry(true);
            if (entryPrev == null)
                {
                aeBucket[nBucket] = entryNext;
                }
            else
                {
                entryPrev.setNext(entryNext);
                }

            --m_aSegment[getSegmentIndex(nBucket)].cEntries;
            return entryCur.getValueInternal();
            }
        }


    // ----- inner class: ContainsValueAction -------------------------------

    /**
    * Factory for ContainsValueAction
    *
    * @return a ContainsValueAction
    */
    protected ContainsValueAction instantiateContainsValueAction()
        {
        return new ContainsValueAction();
        }

    /**
    * Action support for containsValue().  The action performs a lookup by
    * value and is not required to run while holding any segment-locks.
    * <p>
    * The context object for a ContainsValueAction is an opaque context
    * created by <tt>instantiateContext</tt>.
    * <p>
    * The result of invoking a ContainsValueAction is <tt>Boolean.TRUE</tt> if
    * the value is found in the map or <tt>Boolean.FALSE</tt> if the value is
    * not found in the map.
    */
    protected static class ContainsValueAction
            extends EntryActionAdapter
        {
        /**
        * {@inheritDoc}
        */
        public Object invokeFound(Object oKey, Object oContext,
                                  Entry[] aeBucket, int nBucket,
                                  Entry entryPrev, Entry entryCur)
            {
            ContainsValueContext context = (ContainsValueContext) oContext;
            if (Base.equals(entryCur.getValue(), context.m_oValue))
                {
                context.m_fFound = true;
                }
            return NO_VALUE;
            }

        /**
        * {@inheritDoc}
        */
        public boolean isComplete(Object oContext)
            {
            return isFound(oContext);
            }

        /**
        * Return true iff the value was found
        *
        * @param oContext  the ContainsValueContext object
        *
        * @return true iff the value was found
        */
        public boolean isFound(Object oContext)
            {
            return ((ContainsValueContext) oContext).m_fFound;
            }

        /**
        * Instantiate a context appropriate for applying ContainsValueAction
        * to lookup oValue in the map.
        *
        * @param oValue  the value to test the existence of
        *
        * @return a context to use with a ContainsValueAction
        */
        public Object instantiateContext(Object oValue)
            {
            ContainsValueContext oContext =  new ContainsValueContext();
            oContext.m_oValue = oValue;
            return oContext;
            }

        /**
        * Context for ContainsValueAction.
        */
        private static class ContainsValueContext
            {
            /**
            * Has the value been found?
            */
            private boolean m_fFound;

            /**
            * The value being searched for.
            */
            private Object  m_oValue;
            }
        }


    // ----- inner class: Segment -------------------------------------------

    /**
    * Segment metadata.
    */
    protected static class Segment
        {
        /**
        * The number of Entry objects (including synthetics) in this segment.
        * <p>
        * Note: On a JSR-133 non-compliant JVM, this value may be stale to
        * some extent.  It is only used as an internal growth statistic, so
        * this is acceptable.  On JSR-133 compliant JVMs, the segment lock
        * under which this value is modified performs an atomic CAS which
        * would guarantee that writes get flushed.
        */
        protected int cEntries;

        /**
        * Are any threads contending to lock this segment?
        */
        protected volatile boolean fContend;
        }


    // ----- inner class: Entry ---------------------------------------------

    /**
    * Factory for Entry.
    *
    * @param oKey    the key
    * @param oValue  the value
    * @param nHash   the hashCode value of the key
    *
    * @return a new instance of the Entry class (or a subclass thereof)
    */
    protected Entry instantiateEntry(Object oKey, Object oValue, int nHash)
        {
        return new Entry(oKey, oValue, nHash);
        }

    /**
    * Return the first non-synthetic Entry object contained by in the
    * specified bucket.
    *
    * @param aeBucket  the array of hash buckets
    * @param nBucket   the bucket index
    *
    * @return the first non-synthetic Entry in the specified bucket or null
    */
    protected static Entry entryFromBucket(Entry[] aeBucket, int nBucket)
        {
        Entry entry = aeBucket[nBucket];
        return (entry != null && entry.isSynthetic()) ?
                entry.nextEntry() : entry;
        }

    /**
    * A map entry (key-value pair).  The <tt>Map.entrySet</tt> method returns
    * a collection-view of the map, whose elements are of this class.
    */
    protected static class Entry
            extends Base
            implements Map.Entry
        {
        /**
        * Construct an Entry object with the specified key, value and hash.
        *
        * @param oKey    key with which the specified value is to be associated
        * @param oValue  value to be associated with the specified key
        * @param nHash   the hashCode for the specified key
        */
        protected Entry(Object oKey, Object oValue, int nHash)
            {
            m_oKey   = oKey;
            m_oValue = oValue;
            m_nHash  = nHash;
            }

        /**
        * Returns the key corresponding to this entry.
        *
        * @return the key corresponding to this entry
        */
        public Object getKey()
            {
            return m_oKey;
            }


        /**
        * Returns the value corresponding to this entry.  If the mapping
        * has been removed from the backing map (by the iterator's
        * <tt>remove</tt> operation), the results of this call are undefined.
        *
        * @return the value corresponding to this entry
        */
        public Object getValue()
            {
            Object oValue = m_oValue;
            return oValue == SegmentedHashMap.NO_VALUE ? null : oValue;
            }


        /**
        * Returns the value corresponding to this entry, or <tt>NO_VALUE</tt>
        * if this Entry is synthetid.  If the mapping has been removed from
        * the backing map (by the iterator's <tt>remove</tt> operation), the
        * results of this call are undefined.
        *
        * @return the value corresponding to this entry, or <tt>NO_VALUE</tt>
        *         if the Entry is synthetic
        */
        protected Object getValueInternal()
            {
            return m_oValue;
            }

        /**
        * Replaces the value corresponding to this entry with the specified
        * value (writes through to the map).  The behavior of this call is
        * undefined if the mapping has already been removed from the map (by
        * the iterator's <tt>remove</tt> operation).
        *
        * @param oValue  new value to be stored in this entry
        *
        * @return old value corresponding to the entry
        */
        public Object setValue(Object oValue)
            {
            Object oPrev = setValueInternal(oValue);
            return oPrev == SegmentedHashMap.NO_VALUE ? null : oPrev;
            }

        /**
        * Replaces the value corresponding to this entry with the specified
        * value (writes through to the map).  The behavior of this call is
        * undefined if the mapping has already been removed from the map.
        *
        * @param oValue  new value to be stored in this entry
        *
        * @return old value corresponding to the entry, or <tt>NO_VALUE</tt>
        *         if the Entry was synthetic
        */
        protected Object setValueInternal(Object oValue)
            {
            Object oPrev = m_oValue;
            m_oValue = oValue;
            return oPrev;
            }

        /**
        * Compares the specified object with this entry for equality.
        * Returns <tt>true</tt> if the given object is also a map entry and
        * the two entries represent the same mapping.  More formally, two
        * entries <tt>e1</tt> and <tt>e2</tt> represent the same mapping
        * if:
        * <pre>
        *     (e1.getKey()==null ?
        *      e2.getKey()==null : e1.getKey().equals(e2.getKey())) &amp;&amp;
        *     (e1.getValue()==null ?
        *      e2.getValue()==null : e1.getValue().equals(e2.getValue()))
        * </pre>
        * This ensures that the <tt>equals</tt> method works properly across
        * different implementations of the <tt>Map.Entry</tt> interface.
        *
        * @param o  object to be compared for equality with this map entry
        *
        * @return <tt>true</tt> if the specified object is equal to this map
        *         entry
        */
        public boolean equals(Object o)
            {
            if (o instanceof Map.Entry)
                {
                Map.Entry that = (Map.Entry) o;

                return this == that ||
                    Base.equals(this.getKey(), that.getKey()) &&
                    Base.equals(this.getValue(), that.getValue());
                }
            return false;
            }

        /**
        * Returns the hash code value for this map entry.  The hash code
        * of a map entry <tt>e</tt> is defined to be:
        * <pre>
        *     (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
        *     (e.getValue()==null ? 0 : e.getValue().hashCode())
        * </pre>
        * This ensures that <tt>e1.equals(e2)</tt> implies that
        * <tt>e1.hashCode()==e2.hashCode()</tt> for any two Entries
        * <tt>e1</tt> and <tt>e2</tt>, as required by the general contract of
        * <tt>Object.hashCode</tt>.
        *
        * @return the hash code value for this map entry
        */
        public int hashCode()
            {
            Object oKey   = m_oKey;
            Object oValue = getValue();
            return (oKey   == null ? 0 : m_nHash) ^
                   (oValue == null ? 0 : oValue.hashCode());
            }

        /**
        * Render the map entry as a String.
        *
        * @return the details about this entry
        */
        public String toString()
            {
            return "key=\"" + getKey() + '\"' + ", value=\"" + getValue() + '\"';
            }

        /**
        * Is this Entry synthetic?
        *
        * @return true iff this Entry is synthetic
        */
        protected boolean isSynthetic()
            {
            return false;
            }

        /**
        * Set the next entry in the linked list (open hash)
        *
        * @param eNext  the next Entry
        */
        protected void setNext(Entry eNext)
            {
            m_eNext = eNext;
            }

        /**
        * Get the next non-synthetic entry in the linked list (open hash)
        *
        * @return the next non-synthetic entry in the linked list
        */
        protected Entry nextEntry()
            {
            return nextEntry(false);
            }

        /**
        * Get the next entry in the linked list (open hash).  If
        * <tt>fSynthetic</tt> is specified, also return synthetic Entry
        * objects.  Synethetic entries are Entry objects logically associated
        * with a given key, but do not represent a key-value mapping in this
        * map.
        *
        * @param fSynthetic  include synthetic Entry objects?
        *
        * @return the next Entry in the linked list
        */
        protected Entry nextEntry(boolean fSynthetic)
            {
            Entry eNext = m_eNext;
            while (eNext != null && !fSynthetic && eNext.isSynthetic())
                {
                eNext = eNext.m_eNext;
                }
            return eNext;
            }

        // ----- data members -----------------------------------------------

        /**
        * The key.  This object reference will not change for the life of
        * the Entry.
        */
        protected final Object m_oKey;

        /**
        * The value. This object reference can change within the life of the
        * Entry.  This field is declared volatile so that get() and put() can
        * proceed without performing any synchronization.
        */
        protected volatile Object m_oValue;

        /**
        * The key's hash code.  This value will not change for the life of the
        * Entry.
        */
        protected final int m_nHash;

        /**
        * The next entry in the linked list (an open hashing implementation).
        * This field is declared volatile so the entry iterator can safely
        * operate without performing any synchronization.
        */
        protected volatile Entry m_eNext;
        }


    // ----- inner class: EntrySet ------------------------------------------

    /**
    * Factory for EntrySet
    *
    * @return a new instance of the EntrySet class (or a subclass thereof)
    */
    protected EntrySet instantiateEntrySet()
        {
        return new EntrySet();
        }

    /**
    * A set of entries backed by this map.
    */
    protected class EntrySet
            extends AbstractSet
        {
        // ----- Set interface ------------------------------------------

        /**
        * Returns an iterator over the elements contained in this collection.
        *
        * @return an iterator over the elements contained in this collection
        */
        public Iterator iterator()
            {
            return SegmentedHashMap.this.isEmpty()
                   ? NullImplementation.getIterator()
                   : instantiateIterator();
            }

        /**
        * Returns the number of elements in this collection.  If the collection
        * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
        * <tt>Integer.MAX_VALUE</tt>.
        *
        * @return the number of elements in this collection
        */
        public int size()
            {
            return SegmentedHashMap.this.size();
            }

        /**
        * Returns <tt>true</tt> if this collection contains the specified
        * element.  More formally, returns <tt>true</tt> if and only if this
        * collection contains at least one element <tt>e</tt> such that
        * <tt>(o==null ? e==null : o.equals(e))</tt>.
        *
        * @param o  object to be checked for containment in this collection
        *
        * @return <tt>true</tt> if this collection contains the specified
        *         element
        */
        public boolean contains(Object o)
            {
            if (o instanceof Map.Entry)
                {
                Map.Entry thatEntry = (Map.Entry) o;
                Map.Entry thisEntry = SegmentedHashMap.this.getEntryInternal(thatEntry.getKey());
                return thisEntry != null && thisEntry.equals(thatEntry);
                }

            return false;
            }

        /**
        * Removes the specified element from this Set of entries if it is
        * present by removing the associated entry from the underlying
        * Map.
        *
        * @param o  object to be removed from this set, if present
        *
        * @return true if the set contained the specified element
        */
        public boolean remove(Object o)
            {
            if (contains(o))
                {
                SegmentedHashMap.this.remove(((Map.Entry) o).getKey());
                return true;
                }
            else
                {
                return false;
                }
            }

        /**
        * Removes all of the elements from this set of Keys by clearing the
        * underlying Map.
        */
        public void clear()
            {
            SegmentedHashMap.this.clear();
            }

        /**
        * Returns an array containing all of the elements in this collection.
        * If the collection makes any guarantees as to what order its elements
        * are returned by its iterator, this method must return the elements
        * in the same order.  The returned array will be "safe" in that no
        * references to it are maintained by the collection.  (In other words,
        * this method must allocate a new array even if the collection is
        * backed by an Array).  The caller is thus free to modify the returned
        * array.
        *
        * @return an array containing all of the elements in this collection
        */
        public Object[] toArray()
            {
            return toArray((Object[]) null);
            }

        /**
        * Returns an array with a runtime type is that of the specified array
        * and that contains all of the elements in this collection.  If the
        * collection fits in the specified array, it is returned therein.
        * Otherwise, a new array is allocated with the runtime type of the
        * specified array and the size of this collection.
        * <p>
        * If the collection fits in the specified array with room to spare
        * (i.e., the array has more elements than the collection), the element
        * in the array immediately following the end of the collection is set
        * to <tt>null</tt>.  This is useful in determining the length of the
        * collection <i>only</i> if the caller knows that the collection does
        * not contain any <tt>null</tt> elements.)
        *
        * @param ao  the array into which the elements of the collection are to
        *            be stored, if it is big enough; otherwise, a new array of
        *            the same runtime type is allocated for this purpose
        *
        * @return an array containing the elements of the collection
        *
        * @throws ArrayStoreException if the runtime type of the specified
        *         array is not a supertype of the runtime type of every
        *         element in this collection
        */
        public Object[] toArray(Object[] ao)
            {
            SegmentedHashMap    map         = SegmentedHashMap.this;
            IterableEntryAction actionEntry = new EntryActionAdapter()
                    {
                    public Object invokeFound(Object oKey, Object oContext,
                                              Entry[] aeBucket, int nBucket,
                                              Entry entryPrev, Entry entryCur)
                        {
                        if (!entryCur.isSynthetic())
                            {
                            ((List) oContext).add(entryCur);
                            }
                        return NO_VALUE;
                        }
                    };
            return map.toArrayInternal(actionEntry, ao);
            }


        // ----- inner class: Entry Set Iterator ------------------------

        /**
        * Factory for EntrySetIterator.
        *
        * @return a new instance of an Iterator over the EntrySet
        */
        protected Iterator instantiateIterator()
            {
            return new EntrySetIterator();
            }

        /**
        * An Iterator over the EntrySet that is backed by the SegmentedHashMap.
        */
        protected class EntrySetIterator
                extends AbstractStableIterator
            {
            // ----- constructors -----------------------------------

            /**
            * Construct an Iterator over the Entries in the SegmentedHashMap.
            */
            protected EntrySetIterator()
                {
                }

            // ----- internal -------------------------------------------

            /**
            * Advance to the next object in the iteration.
            */
            protected void advance()
                {
                if (m_fDeactivated)
                    {
                    // the Iterator has already reached the end on a previous
                    // call to advance()
                    return;
                    }

                Entry[] aeBucket = this.m_aeBucket;
                if (aeBucket == null)
                    {
                    // Activate the iterator upon its first use.  The map will
                    // not grow while there are any active iterators.
                    SegmentedHashMap map = SegmentedHashMap.this;
                    map.iteratorActivated(this);
                    aeBucket = this.m_aeBucket = map.getStableBucketArray();
                    }

                Entry entry    = m_entryPrev;
                int   iBucket  = -1;          // -1 indicates no change
                int   cBuckets = aeBucket.length;
                while (true)
                    {
                    if (entry != null)
                        {
                        // advance within the currrent bucket
                        entry = entry.nextEntry();
                        }

                    // check if the current bucket has been exhausted, and if
                    // it has, then advance to the first non-empty bucket
                    if (entry == null)
                        {
                        iBucket = m_iBucket;
                        do
                            {
                            if (++iBucket >= cBuckets)
                                {
                                // the iterator has been exhausted
                                deactivate();
                                return;
                                }
                            entry = entryFromBucket(aeBucket, iBucket);
                            }
                        while (entry == null);
                        }

                    // update the current bucket index if the iterator
                    // advanced to a new bucket
                    if (iBucket >= 0)
                        {
                        m_iBucket = iBucket;
                        }

                    // remember the entry being iterated next
                    m_entryPrev = entry;

                    // report back the actual entry that exists in the Map
                    // that is being iterated next
                    setNext(entry);
                    return;
                    }
                }

            /**
            * Remove the specified item from the underlying Map.
            *
            * @param oPrev  the previously iterated object to remove
            */
            protected void remove(Object oPrev)
                {
                SegmentedHashMap.this.remove(((Map.Entry) oPrev).getKey());
                }


            // ----- internal ---------------------------------------

            /**
            * Shut down the Iterator. This is done on exhaustion of the
            * contents of the Iterator, or on finalization of the Iterator.
            */
            protected void deactivate()
                {
                if (!m_fDeactivated)
                    {
                    // No more entries to iterate; notify the containing Map
                    // that this Iterator is no longer active.
                    SegmentedHashMap.this.releaseIterator(this);
                    m_fDeactivated = true;

                    // clean up references (no longer needed)
                    m_aeBucket     = null;
                    m_entryPrev    = null;
                    }
                }

            // ----- data members -----------------------------------

            /**
            * Array of buckets in the underlying map.
            */
            private Entry[] m_aeBucket;

            /**
            * Current bucket being iterated.
            */
            private int     m_iBucket = -1;

            /**
            * The most recent Entry object internally iterated.
            */
            private Entry   m_entryPrev;

            /**
            * Set to true when the Iterator is complete.
            */
            private boolean m_fDeactivated;
            }
        }


    // ----- inner class: KeySet --------------------------------------------

    /**
    * Factory for KeySet.
    *
    * @return a new instance of the KeySet class (or subclass thereof)
    */
    protected KeySet instantiateKeySet()
        {
        return new KeySet();
        }

    /**
    * A set of entries backed by this map.
    */
    protected class KeySet
            extends AbstractSet
        {
        // ----- Set interface ------------------------------------------

        /**
        * Obtain an iterator over the keys in the Map.
        *
        * @return an Iterator that provides a live view of the keys in the
        *         underlying Map object
        */
        public Iterator iterator()
            {
            return new Iterator()
                {
                private Iterator m_iter = SegmentedHashMap.this.entrySet().iterator();

                public boolean hasNext()
                    {
                    return m_iter.hasNext();
                    }

                public Object next()
                    {
                    return ((Map.Entry) m_iter.next()).getKey();
                    }

                public void remove()
                    {
                    m_iter.remove();
                    }
                };
            }

        /**
        * Determine the number of keys in the Set.
        *
        * @return the number of keys in the Set, which is the same as the
        *         number of entries in the underlying Map
        */
        public int size()
            {
            return SegmentedHashMap.this.size();
            }

        /**
        * Determine if a particular key is present in the Set.
        *
        * @return true iff the passed key object is in the key Set
        */
        public boolean contains(Object oKey)
            {
            return SegmentedHashMap.this.containsKey(oKey);
            }

        /**
        * Removes the specified element from this Set of keys if it is present
        * by removing the associated entry from the underlying Map.
        *
        * @param o  object to be removed from this set, if present
        *
        * @return true if the set contained the specified element
        */
        public boolean remove(Object o)
            {
            SegmentedHashMap map = SegmentedHashMap.this;
            if (map.containsKey(o))
                {
                map.remove(o);
                return true;
                }
            else
                {
                return false;
                }
            }

        /**
        * Removes all of the elements from this set of Keys by clearing the
        * underlying Map.
        */
        public void clear()
            {
            SegmentedHashMap.this.clear();
            }

        /**
        * Returns an array containing all of the keys in this set.
        *
        * @return an array containing all of the keys in this set
        */
        public Object[] toArray()
            {
            return toArray((Object[]) null);
            }

        /**
        * Returns an array with a runtime type is that of the specified array
        * and that contains all of the keys in this Set.  If the Set fits in
        * the specified array, it is returned therein. Otherwise, a new array
        * is allocated with the runtime type of the specified array and the
        * size of this collection.
        * <p>
        * If the Set fits in the specified array with room to spare (i.e., the
        * array has more elements than the Set), the element in the array
        * immediately following the end of the Set is set to <tt>null</tt>.
        * This is useful in determining the length of the Set <i>only</i> if
        * the caller knows that the Set does not contain any <tt>null</tt>
        * keys.)
        *
        * @param a  the array into which the elements of the Set are to
        *           be stored, if it is big enough; otherwise, a new array
        *           of the same runtime type is allocated for this purpose
        *
        * @return an array containing the elements of the Set
        *
        * @throws ArrayStoreException if the runtime type of the specified
        *         array is not a supertype of the runtime type of every
        *         element in this Set of keys
        */
        public Object[] toArray(Object a[])
            {
            SegmentedHashMap    map         = SegmentedHashMap.this;
            IterableEntryAction actionEntry = new EntryActionAdapter()
                    {
                    public Object invokeFound(Object oKey, Object oContext,
                                              Entry[] aeBucket, int nBucket,
                                              Entry entryPrev, Entry entryCur)
                        {
                        if (!entryCur.isSynthetic())
                            {
                            ((List) oContext).add(entryCur.getKey());
                            }
                        return NO_VALUE;
                        }
                    };
            return map.toArrayInternal(actionEntry, a);
            }
        }


    // ----- inner class: ValuesCollection ----------------------------------

    /**
    * Factory for ValuesCollection.
    *
    * @return a new instance of the ValuesCollection class (or subclass
    *         thereof)
    */
    protected ValuesCollection instantiateValuesCollection()
        {
        return new ValuesCollection();
        }

    /**
    * A collection of values backed by this map.
    */
    protected class ValuesCollection
            extends AbstractCollection
        {
        // ----- Collection interface -----------------------------------

        /**
        * Obtain an iterator over the values in the Map.
        *
        * @return an Iterator that provides a live view of the values in the
        *         underlying Map object
        */
        public Iterator iterator()
            {
            return new Iterator()
                {
                private Iterator m_iter =
                    SegmentedHashMap.this.entrySet().iterator();

                public boolean hasNext()
                    {
                    return m_iter.hasNext();
                    }

                public Object next()
                    {
                    return ((Entry) m_iter.next()).getValue();
                    }

                public void remove()
                    {
                    m_iter.remove();
                    }
                };
            }

        /**
        * Determine the number of values in the Collection.
        *
        * @return the number of values in the Collection, which is the same
        *         as the number of entries in the underlying Map
        */
        public int size()
            {
            return SegmentedHashMap.this.size();
            }

        /**
        * Removes all of the elements from this Collection of values by
        * clearing the underlying Map.
        */
        public void clear()
            {
            SegmentedHashMap.this.clear();
            }

        /**
        * Returns an array containing all of the values in the Collection.
        *
        * @return an array containing all of the values in the Collection
        */
        public Object[] toArray()
            {
            return toArray((Object[]) null);
            }

        /**
        * Returns an array with a runtime type is that of the specified array
        * and that contains all of the values in the Collection.  If the
        * Collection fits in the specified array, it is returned therein.
        * Otherwise, a new array is allocated with the runtime type of the
        * specified array and the size of this collection.
        * <p>
        * If the Collection fits in the specified array with room to spare
        * (i.e., the array has more elements than the Collection), the element
        * in the array immediately following the end of the Collection is set
        * to <tt>null</tt>.  This is useful in determining the length of the
        * Collection <i>only</i> if the caller knows that the Collection does
        * not contain any <tt>null</tt> values.)
        *
        * @param ao  the array into which the elements of the Collection are
        *            to be stored, if it is big enough; otherwise, a new
        *            array of the same runtime type is allocated for this
        *            purpose
        *
        * @return an array containing the elements of the Collection
        *
        * @throws ArrayStoreException if the runtime type of the specified
        *         array is not a supertype of the runtime type of every
        *         element in this Collection of values
        */
        public Object[] toArray(Object ao[])
            {
            SegmentedHashMap    map         = SegmentedHashMap.this;
            IterableEntryAction actionEntry = new EntryActionAdapter()
                    {
                    public Object invokeFound(Object oKey, Object oContext,
                                              Entry[] aeBucket, int nBucket,
                                              Entry entryPrev, Entry entryCur)
                        {
                        if (!entryCur.isSynthetic())
                            {
                            ((List) oContext).add(entryCur.getValue());
                            }
                        return NO_VALUE;
                        }
                    };
            return map.toArrayInternal(actionEntry, ao);
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * When resizing, the entries array is replaced with this special empty
    * array to signify a resize.
    */
    private static final Entry[] NO_ENTRIES = new Entry[0];

    /**
    * A list of possible modulos to use.
    */
    protected static final int[] PRIME_MODULO =
        {
        61,127,197,277,397,457,509,587,641,701,761,827,883,953,1019,1129,
        1279,1427,1543,1733,1951,2143,2371,2671,2927,3253,3539,3907,4211,
        4591,4973,5393,5743,6143,6619,6997,7529,8009,8423,8819,9311,9929,
        10069,11087,12203,13003,14051,15017,16007,17027,18061,19013,20063,
        23011,27011,30011,35023,40009,45007,50021,60013,70001,80021,90001,
        100003,120011,140009,160001,180001,200003,233021,266003,300007,
        350003,400009,450001,500009,550007,600011,650011,700001,800011,
        850009,900001,950009,1000003,1100009,1200007,1300021,1400017,
        1500007,1600033,1700021,1800017,1900009,2000003,2500009,3000017,
        3500017,4000037,4500007,5000011,6000011,7000003,8000009,9000011,
        10000019,12000017,14000029,16000057,18000041,20000003,25000009,
        30000001,35000011,40000003,45000017,50000017,60000011,70000027,
        80000023,90000049,100000007,150000001,200000033,300000007,400000009,
        500000003,600000001,700000001,800000011,900000011,1000000007,
        1100000009,1200000041,1300000003,1400000023,1500000001,1600000009,
        1700000009,1800000011,1900000043,
        2147483647 // Integer.MAX_VALUE is a prime!
        };

    /**
    * Default initial size provides a prime modulo and is large enough that
    * resize is not immediate.  (A hash map probably uses less than 128 bytes
    * initially.)
    */
    public static final int DEFAULT_INITIALSIZE = PRIME_MODULO[0];

    /**
    * Biggest possible modulo.
    */
    protected static final int BIGGEST_MODULO = PRIME_MODULO[PRIME_MODULO.length-1];

    /**
    * The default load factor is 100%, which means that the hash map will not
    * resize until there is (on average) one entry in every bucket. The cost
    * of scanning a linked list in a particular bucket is very low, so there
    * is little reason for having this value below 1.0, and the goal is
    * constant order access, so assuming a perfect hash this will provide the
    * optimal access speed relative to size.
    */
    public static final float DEFAULT_LOADFACTOR = 1.0F;

    /**
    * Using the default growth rate, the bucket array will grow by a factor
    * of four.  The relatively high growth rate helps to ensure less resize
    * operations, an important consideration in a high-concurrency map.
    */
    public static final float DEFAULT_GROWTHRATE = 3.0F;

    /**
    * The minimum segment capacity.
    */
    protected static final int MIN_SEGMENT_CAPACITY = 2;

    /**
    * The number of segments to partition the hash buckets into.  There is a
    * single lock for each segment.  This number is specially chosen as 61 is
    * the largest prime number smaller than 64 (the size of the datatype
    * used to represent the lock).
    */
    protected static final int SEGMENT_COUNT = 61;

    /**
    * The number of segment-locks.  Each segment has its own lock and
    * there is a global "intention" lock.
    */
    protected static final int LOCK_COUNT = SEGMENT_COUNT + 1;

    /**
    * The lock representation used to indicate that no locks are set.
    */
    protected static final long LOCKS_NONE = 0L;

    /**
    * The lock representation used to indicate that all mutexes are locked.
    */
    protected static final long LOCKS_ALL = 0xFFFFFFFFFFFFFFFFL;

    /**
    * The mutex number used to indicate that a lock-all is pending.
    */
    protected static final int LOCK_ALL_PENDING_IDX = 61;

    /**
    * The bit-mask used to indicate that a lock-all is pending.
    */
    protected static final long LOCK_ALL_PENDING = 1L << LOCK_ALL_PENDING_IDX;

    /**
    * Maximum number of times to spin while trying to acquire a segment lock
    * before waiting.
    */
    protected static final int SEGMENT_LOCK_MAX_SPIN = 0xF;

    /**
    * Size threshold used by the putAll operation.
    */
    protected static final int PUTALL_THRESHOLD = (int) (1.5 * SEGMENT_COUNT);

    /**
    * Object to be used as a value representing that the Entry object is
    * "synthetic" and while logically associated with a key, does not
    * represent a key-value mapping in the Map.
    */
    protected static final Object NO_VALUE = new Object();

    /**
     * An empty, immutable SegmentedHashMap instance.
     */
    public static final Map<?, ?> EMPTY = Collections.unmodifiableMap(new SegmentedHashMap(1, DEFAULT_LOADFACTOR, DEFAULT_GROWTHRATE));

    // ----- data members ---------------------------------------------------

    /**
    * When resizing completes, a notification is issued against this object.
    */
    protected final Object RESIZING = new Object();

    /**
    * The "segment-locks". This AtomicCounter is actually used just as an
    * "Atomic Long" value, with each of the first 61 bits being used to
    * represent a segment-lock.  In this case, the number of "buckets" is
    * fixed to 61 (the largest prime smaller than 64).
    */
    protected final AtomicLong m_atomicLocks = new AtomicLong();

    /**
    * An array of the control-structures for the Map's segments.
    */
    protected final Segment[] m_aSegment;

    /**
    * The array of hash buckets.  This field is declared volatile in order to
    * reduce synchronization.
    */
    protected volatile Entry[] m_aeBucket;

    /**
    * The capacity of each segment (the point at which we should resize).
    */
    protected int m_cSegmentCapacity;

    /**
    * The determining factor for the hash map capacity given a certain number
    * of buckets, such that capacity = bucketcount * loadfactor.
    */
    protected final float m_flLoadFactor;

    /**
    * The rate of growth as a fraction of the current number of buckets,
    * 0 &lt; n, such that the hash map grows to bucketcount * (1 + growth-rate).
    */
    protected final float m_flGrowthRate;

    /**
    * The set of entries backed by this map.
    */
    protected EntrySet m_setEntries;

    /**
    * The set of keys backed by this map.
    */
    protected KeySet m_setKeys;

    /**
    * The collection of values backed by this map.
    */
    protected ValuesCollection m_colValues;

    /**
    * A holder for active Iterator(s): either WeakReference(&lt;Iterator&gt;) or
    * WeakHashMap(&lt;Iterator&gt; null)
    */
    protected Object m_oIterActive;

    /**
    * The singleton action for getEntryInternal support.
    */
    protected GetEntryAction m_actionGetEntry;

    /**
    * The singleton action for insert support.
    */
    protected InsertAction m_actionInsert;

    /**
    * The singleton action for remove support.
    */
    protected RemoveAction m_actionRemove;

    /**
    * The singleton action for containsValue support.
    */
    protected ContainsValueAction m_actionContainsValue;
    }
