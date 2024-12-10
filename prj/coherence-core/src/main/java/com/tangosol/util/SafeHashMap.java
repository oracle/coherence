/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.collections.AbstractStableIterator;

import com.tangosol.internal.util.Primes;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
* An implementation of java.util.Map that is synchronized, but minimally so.
* This class is for use in situation where high concurrency is required, but
* so is data integrity.
* <p>
* All additions and removals are synchronized on the map, so to temporarily
* prevent changes to the map contents, synchronize on the map object.
*
* @author cp 1999.04.27
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class SafeHashMap<K, V>
        extends AbstractMap<K, V>
        implements Cloneable, Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a thread-safe hash map using the default settings.
    */
    public SafeHashMap()
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
    *                         bucketCount = bucketCount * (1 + growthRate)
    */
    public SafeHashMap(int cInitialBuckets, float flLoadFactor, float flGrowthRate)
        {
        if (cInitialBuckets <= 0)
            {
            throw new IllegalArgumentException("SafeHashMap:  "
                + "Initial number of buckets must be greater than zero.");
            }
        if (flLoadFactor <= 0)
            {
            throw new IllegalArgumentException("SafeHashMap:  "
                + "Load factor must be greater than zero.");
            }
        if (flGrowthRate <= 0)
            {
            throw new IllegalArgumentException("SafeHashMap:  "
                + "Growth rate must be greater than zero.");
            }

        // initialize the hash map data structure
        m_aeBucket     = new AtomicReferenceArray<>(cInitialBuckets);
        m_cCapacity    = (int) (cInitialBuckets * flLoadFactor);
        m_flLoadFactor = flLoadFactor;
        m_flGrowthRate = flGrowthRate;
    }


    // ----- Map interface --------------------------------------------------

    /**
    * Returns the number of key-value mappings in this map.  If the
    * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
    * <tt>Integer.MAX_VALUE</tt>.
    * <p>
    * This method is not synchronized; it returns the size at the moment
    * that the method is invoked.  To ensure that the size does not change
    * from the returned value, the caller must synchronize on the map before
    * calling the size method.
    *
    * @return the number of key-value mappings in this map
    */
    public int size()
        {
        return m_cEntries;
        }

    /**
    * Returns <tt>true</tt> if this map contains no key-value mappings.
    * <p>
    * This method is not synchronized; it returns the state of the map at
    * the moment that the method is invoked.  To ensure that the size does
    * not change, the caller must synchronize on the map before calling the
    * method.
    *
    * @return <tt>true</tt> if this map contains no key-value mappings
    */
    public boolean isEmpty()
        {
        return m_cEntries == 0;
        }

    /**
    * Returns <tt>true</tt> if this map contains a mapping for the specified
    * key.
    * <p>
    * This method is not synchronized; it returns true if the map contains
    * the key at the moment that the method is invoked.  To ensure that the
    * key is still in (or is still not in) the table when the method returns,
    * the caller must synchronize on the map before calling the method.
    *
    * @param key key whose presence in this map is to be tested
    *
    * @return <tt>true</tt> if this map contains a mapping for the specified
    *         key
    */
    public boolean containsKey(Object key)
        {
        return getEntryInternal(key) != null;
        }

    /**
    * Returns the value to which this map maps the specified key.  Returns
    * <tt>null</tt> if the map contains no mapping for this key.  A return
    * value of <tt>null</tt> does not <i>necessarily</i> indicate that the
    * map contains no mapping for the key; it's also possible that the map
    * explicitly maps the key to <tt>null</tt>.  The <tt>containsKey</tt>
    * operation may be used to distinguish these two cases.
    *
    * @param key key whose associated value is to be returned
    *
    * @return the value to which this map maps the specified key, or
    *         <tt>null</tt> if the map contains no mapping for this key
    */
    public V get(Object key)
        {
        Entry<K, V> entry = getEntryInternal(key);
        return (entry == null ? null : entry.getValue());
        }

    /**
    * Locate an Entry in the hash map based on its key.
    *
    * @param key  the key object to search for
    *
    * @return the Entry or null
    */
    public Entry<K, V> getEntry(Object key)
        {
        return getEntryInternal(key);
        }

    /**
    * Associates the specified value with the specified key in this map.
    * If the map previously contained a mapping for this key, the old
    * value is replaced.
    * <p>
    * This method is not synchronized; it only synchronizes internally if
    * it has to add a new Entry.  To ensure that the value does not change
    * (or the Entry is not removed) before this method returns, the caller
    * must synchronize on the map before calling this method.
    *
    * @param key key with which the specified value is to be associated
    *
    * @param value value to be associated with the specified key
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *          if there was no mapping for key.  A <tt>null</tt> return can
    *          also indicate that the map previously associated <tt>null</tt>
    *          with the specified key, if the implementation supports
    *          <tt>null</tt> values
    */
    public V put(K key, V value)
        {
        Entry<K, V> entry = getEntryInternal(key);
        nonexistent: if (entry == null)
            {
            // calculate hash code for the key
            int nHash = (key == null ? 0 : key.hashCode());

            // instantiate and configure a new Entry
            entry = instantiateEntry(key, value, nHash);

            // synchronize the addition of the new Entry
            // note that it is possible that an Entry with the same key
            // has been added by another thread
            synchronized (this)
                {
                // get the array of buckets
                AtomicReferenceArray<Entry> aeBucket = m_aeBucket;
                int                         cBuckets = aeBucket.length();

                // hash to a particular bucket
                int    nBucket  = getBucketIndex(nHash, cBuckets);
                Entry  entryCur = aeBucket.get(nBucket);

                // walk the linked list of entries (open hash) in the bucket
                // to verify the Entry has not already been added
                while (entryCur != null)
                    {
                    // optimization:  check hash first
                    if (nHash == entryCur.m_nHash &&
                        (key == null ? entryCur.m_oKey == null
                                    : key.equals(entryCur.m_oKey)))
                        {
                        // found the entry; it is no longer non-existent
                        entry = entryCur;
                        break nonexistent;
                        }

                    entryCur = entryCur.m_eNext;
                    }

                // put the Entry in at the front of the list of entries
                // for that bucket
                aeBucket.accumulateAndGet(nBucket, entry, (oldHead, newHead) ->
                    {
                    newHead.m_eNext = oldHead;
                    return newHead;
                    });

                if (++m_cEntries > m_cCapacity)
                    {
                    grow();
                    }
                }

            // note: supports subclasses that implement ObservableMap
            entry.onAdd();

            return null;
            }

        // note that it is possible that the Entry is being removed
        // by another thread or that the value is being updated by
        // another thread because this is not synchronized
        return entry.setValue(value);
        }

    /**
    * Resize the bucket array, rehashing all Entries.
    */
    protected synchronized void grow()
        {
        // store off the old bucket array
        AtomicReferenceArray<Entry> aeOld = m_aeBucket;
        int                         cOld  = aeOld.length();

        // check if there is no more room to grow
        if (cOld >= BIGGEST_MODULO)
            {
            return;
            }

        // use a 0-length array to signify that a resize is taking place
        m_aeBucket = NO_ENTRIES;

        // calculate growth
        int cNew = (int) Math.min((long) (cOld * (1F + m_flGrowthRate)), BIGGEST_MODULO);
        if (cNew <= cOld)
            {
            // very low growth rate or very low initial size (stupid!)
            cNew = cOld + 1;
            }

        // use a prime bigger than the new size
        cNew = Primes.next(cNew);

        // create a new bucket array; in the case of an OutOfMemoryError
        // be sure to restore the old bucket array
        AtomicReferenceArray<Entry> aeNew;
        try
            {
            aeNew = new AtomicReferenceArray<>(cNew);
            }
        catch (OutOfMemoryError e)
            {
            m_aeBucket = aeOld;
            throw e;
            }

        // if there are Iterators active, they are iterating over an array of
        // buckets which is about to be completely whacked; to make sure that
        // the Iterators can recover from the bucket array getting whacked,
        // the resize will create a clone of each entry in the old bucket
        // array and put those clones into the old bucket array in the same
        // order that the original entries were found, thus allowing the
        // Iterators to find their place again without missing or repeating
        // any data, other than the potential for missing data added after
        // the iteration began (which is always possible anyways)
        boolean fRetain = isActiveIterator();

        // rehash
        for (int i = 0; i < cOld; ++i)
            {
            Entry entry       = aeOld.get(i);
            Entry entryRetain = null;
            while (entry != null)
                {
                // store off the next Entry
                // (it is going to get hammered otherwise)
                Entry entryNext = entry.m_eNext;

                // rehash the Entry into the new bucket array
                int nBucket = getBucketIndex(entry.m_nHash, cNew);
                aeNew.accumulateAndGet(nBucket, entry, (oldHead, newHead) ->
                    {
                    newHead.m_eNext = oldHead;
                    return newHead;
                    });

                // clone each entry if Iterators are active (since they will
                // need the entries in the same order to avoid having to
                // throw a CME in most cases)
                if (fRetain)
                    {
                    Entry entryCopy = (Entry) entry.clone();
                    if (entryRetain == null)
                        {
                        aeOld.set(i, entryCopy);
                        }
                    else
                        {
                        entryRetain.m_eNext = entryCopy;
                        }
                    entryRetain = entryCopy;
                    }

                // process next Entry in the old list
                entry = entryNext;
                }
            }

        // store updated bucket array
        m_cCapacity = (int) (cNew * m_flLoadFactor);
        m_aeBucket  = aeNew;

        // notify threads that are waiting for the resize to complete
        synchronized (RESIZING)
            {
            RESIZING.notifyAll();
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
    *         with the specified key, if the implementation supports
    *         <tt>null</tt> values
    */
    public synchronized V remove(Object oKey)
        {
        // get the array of buckets
        AtomicReferenceArray<Entry> aeBucket = m_aeBucket;
        int                         cBuckets = aeBucket.length();

        // hash to a particular bucket
        int         nHash    = (oKey == null ? 0 : oKey.hashCode());
        int         nBucket  = getBucketIndex(nHash, cBuckets);
        Entry<K, V> entryCur = aeBucket.get(nBucket);

        // walk the linked list of entries (open hash) in the bucket
        // to verify the Entry has not already been added
        Entry entryPrev = null;
        while (entryCur != null)
            {
            // optimization:  check hash first
            if (nHash == entryCur.m_nHash &&
                (oKey == null ? entryCur.m_oKey == null
                              : oKey.equals(entryCur.m_oKey)))
                {
                // remove the current Entry from the list
                if (entryPrev == null)
                    {
                    aeBucket.set(nBucket, entryCur.m_eNext);
                    }
                else
                    {
                    entryPrev.m_eNext = entryCur.m_eNext;
                    }
                --m_cEntries;
                return entryCur.getValue();
                }

            entryPrev = entryCur;
            entryCur  = entryCur.m_eNext;
            }

        return null;
        }

    /**
    * Removes all mappings from this map.
    */
    public synchronized void clear()
        {
        m_aeBucket  = new AtomicReferenceArray<>(DEFAULT_INITIALSIZE);
        m_cEntries  = 0;
        m_cCapacity = (int) (DEFAULT_INITIALSIZE * m_flLoadFactor);
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
    public Set<Map.Entry<K,V>> entrySet()
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
    * addAll operations.<p>
    *
    * @return a Set view of the keys contained in this map
    */
    public Set<K> keySet()
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
    public Collection<V> values()
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


    // ----- Map interface (default methods) --------------------------------

    @Override
    public synchronized boolean remove(Object key, Object value)
        {
        return super.remove(key, value);
        }

    @Override
    public synchronized boolean replace(K key, V oldValue, V newValue)
        {
        return super.replace(key, oldValue, newValue);
        }

    @Override
    public synchronized V replace(K key, V newValue)
        {
        return super.replace(key, newValue);
        }

    @Override
    public synchronized V putIfAbsent(K key, V value)
        {
        return super.putIfAbsent(key, value);
        }

    @Override
    public synchronized void replaceAll(BiFunction<? super K, ? super V, ? extends V> function)
        {
        super.replaceAll(function);
        }

    @Override
    public synchronized V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
        {
        return super.computeIfAbsent(key, mappingFunction);
        }

    @Override
    public synchronized V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        return super.computeIfPresent(key, remappingFunction);
        }

    @Override
    public synchronized V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        return super.compute(key, remappingFunction);
        }

    @Override
    public synchronized V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)
        {
        return super.merge(key, value, remappingFunction);
        }

    // ----- Cloneable interface --------------------------------------------

    /**
    * Create a clone of the SafeHashMap. Synchronization is necessary to
    * prevent resizing (or to wait for resizing to complete), and also
    * prevents other changes to the SafeHashMap while the deep clone is
    * occurring.
    *
    * @return a clone of the SafeHashMap
    */
    public synchronized Object clone()
        {
        try
            {
            SafeHashMap that = (SafeHashMap) super.clone();

            // only have to deep-clone the bucket array and the map entries
            AtomicReferenceArray<Entry> aeBucket = new AtomicReferenceArray<>(m_aeBucket.length());
            int                         cBuckets = aeBucket.length();
            for (int i = 0; i < cBuckets; ++i)
                {
                Entry entryThis = m_aeBucket.get(i);
                if (entryThis != null)
                    {
                    aeBucket.set(i, that.cloneEntryList(entryThis));
                    }
                }
            that.m_aeBucket    = aeBucket;
            that.m_setEntries  = null;
            that.m_setKeys     = null;
            that.m_colValues   = null;
            that.m_oIterActive = null;

            return that;
            }
        catch (CloneNotSupportedException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Clone an entire linked list of entries.
    * <p>
    * This method must be called on the map that will contain the clones, to
    * allow the map to be the parent of the entries if the entries are not
    * static inner classes.
    *
    * @param entryThat  the entry that is the head of the list to clone
    *
    * @return null if the entry is null, otherwise an entry that is a clone
    *         of the passed entry, and a linked list of clones for each entry
    *         in the linked list that was passed
    */
    protected Entry cloneEntryList(Entry entryThat)
        {
        if (entryThat == null)
            {
            return null;
            }

        // clone the head of the chain
        Entry entryThis = instantiateEntry();
        entryThis.copyFrom(entryThat);

        // clone the rest of the chain
        Entry entryPrevThis = entryThis;
        Entry entryNextThat = entryThat.m_eNext;
        while (entryNextThat != null)
            {
            // clone the entry
            Entry entryNextThis = instantiateEntry();
            entryNextThis.copyFrom(entryNextThat);

            // link it in
            entryPrevThis.m_eNext = entryNextThis;

            // advance
            entryPrevThis = entryNextThis;
            entryNextThat = entryNextThat.m_eNext;
            }

        return entryThis;
        }


    // ----- Serializable interface -----------------------------------------

    /**
    * Write this object to an ObjectOutputStream.
    *
    * @param out  the ObjectOutputStream to write this object to
    *
    * @throws IOException  thrown if an exception occurs writing this object
    */
    private synchronized void writeObject(ObjectOutputStream out)
            throws IOException
        {
        // store stats first
        AtomicReferenceArray<Entry> aeBucket = m_aeBucket;
        int                         cBuckets = aeBucket.length();

        out.writeInt(cBuckets);
        out.writeInt(m_cCapacity);
        out.writeFloat(m_flLoadFactor);
        out.writeFloat(m_flGrowthRate);

        // store entries second
        int cEntries = m_cEntries;
        int cCheck   = 0;
        out.writeInt(cEntries);
        for (int iBucket = 0; iBucket < cBuckets; ++iBucket)
            {
            Entry entry = aeBucket.get(iBucket);
            while (entry != null)
                {
                out.writeObject(entry.m_oKey);
                out.writeObject(entry.m_oValue);

                entry = entry.m_eNext;
                ++cCheck;
                }
            }

        if (cCheck != cEntries)
            {
            throw new IOException("expected to write " + cEntries
                + " objects but actually wrote " + cCheck);
            }
        }

    /**
    * Read this object from an ObjectInputStream.
    *
    * @param in  the ObjectInputStream to read this object from
    *
    * @throws IOException  if an exception occurs reading this object
    * @throws ClassNotFoundException  if the class for an object being
    *         read cannot be found
    */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
        RESIZING = new Object();

        // read map stats
        int  cBuckets    = in.readInt();
        int  cCapacity   = in.readInt();
        float flLoadFactor = in.readFloat();
        float flGrowthRate = in.readFloat();
        int  cEntries    = in.readInt();

        if (cBuckets <= ExternalizableHelper.CHUNK_THRESHOLD)
            {
            m_cCapacity   = cCapacity;
            m_flLoadFactor = flLoadFactor;
            m_flGrowthRate = flGrowthRate;
            m_cEntries    = cEntries;

            // JEP-290 - ensure we can allocate this array
            ExternalizableHelper.validateLoadArray(SafeHashMap.Entry[].class, cBuckets, in);

            AtomicReferenceArray<Entry> aeBucket = m_aeBucket = new AtomicReferenceArray<>(cBuckets);

            // read entries
            for (int i = 0; i < cEntries; ++i)
                {
                K   oKey    = (K) in.readObject();
                V   oValue  = (V) in.readObject();
                int nHash   = (oKey == null ? 0 : oKey.hashCode());
                int nBucket = getBucketIndex(nHash, cBuckets);

                Entry<K, V> entry = instantiateEntry(oKey, oValue, nHash);

                aeBucket.accumulateAndGet(nBucket, entry, (oldHead, newHead) ->
                    {
                    newHead.m_eNext = oldHead;
                    return newHead;
                    });
                }
            }
        else
            {
            // if the cBuckets exceeds the threshold, consider the
            // deserialized map parameters are invalid, and grow
            // the structures using defaults by calling put.
            for (int i = 0; i < cEntries; ++i)
                {
                put((K) in.readObject(), (V) in.readObject());
                }
            }
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Locate an Entry in the hash map based on its key.
    * <p>
    * Unlike the {@link #getEntry} method, there must be no side-effects of
    * calling this method.
    *
    * @param oKey  the key object to search for
    *
    * @return the Entry or null
    */
    protected Entry<K, V> getEntryInternal(Object oKey)
        {
        // calculate hash code for the oKey
        int nHash = (oKey == null ? 0 : oKey.hashCode());

        while (true)
            {
            // get the bucket array
            AtomicReferenceArray<Entry> aeBucket = getStableBucketArray();
            int                         cBuckets = aeBucket.length();

            // hash to a particular bucket
            int   nBucket = getBucketIndex(nHash, cBuckets);
            Entry entry   = aeBucket.get(nBucket);

            // walk the linked list of entries (open hash) in the bucket
            while (entry != null)
                {
                // optimization:  check hash first
                if (nHash == entry.m_nHash &&
                    (oKey == null ? entry.m_oKey == null
                                 : oKey.equals(entry.m_oKey)))
                    {
                    // COH-1542: check for resize before returning Entry
                    break;
                    }

                entry = entry.m_eNext;
                }

            // if a resize occurred, the bucket array may have been reshuffled
            // while we were searching it; we know a resize occurred iff the
            // hash bucket array changed
            if (aeBucket == m_aeBucket)
                {
                // no resize occurred
                return entry;
                }
            }
        }

    /**
    * Removes the passed Entry from the map.
    * <p>
    * <b>Note:</b> Unlike calling the "remove" method, which is overridden at
    * subclasses, calling this method directly does not generate any events.
    *
    * @param entry  the entry to remove from this map
    */
    protected synchronized void removeEntryInternal(Entry<K, V> entry)
        {
        if (entry == null)
            {
            throw new IllegalArgumentException("entry is null");
            }

        // get the array of buckets
        AtomicReferenceArray<Entry> aeBucket = m_aeBucket;
        int                         cBuckets = aeBucket.length();

        // hash to a particular bucket
        int    nHash    = entry.m_nHash;
        int    nBucket  = getBucketIndex(nHash, cBuckets);

        // check the head
        Entry entryHead = aeBucket.get(nBucket);
        if (entry == entryHead)
            {
            aeBucket.set(nBucket, entry.m_eNext);
            }
        else
            {
            // walk the linked list of entries (open hash) in the bucket
            Entry entryPrev = entryHead;
            while (true)
                {
                if (entryPrev == null)
                    {
                    // another thread has already removed the entry
                    return;
                    }

                Entry entryCur = entryPrev.m_eNext;
                if (entry == entryCur)
                    {
                    entryPrev.m_eNext = entryCur.m_eNext;
                    break;
                    }

                entryPrev = entryCur;
                }
            }

        --m_cEntries;
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
    * Get the bucket array, or if a resize is occurring, wait for the resize
    * to complete and return the new bucket array.
    *
    * @return the latest bucket array
    */
    protected AtomicReferenceArray<Entry> getStableBucketArray()
        {
        // get the bucket array
        AtomicReferenceArray<Entry> aeBucket = m_aeBucket;

        // wait for any ongoing resize to complete
        while (aeBucket.length() == 0)
            {
            synchronized (RESIZING)
                {
                // now that we have the lock, verify that it is
                // still necessary to wait
                if (m_aeBucket.length() == 0)
                    {
                    try
                        {
                        // limit the wait, so grow() can fail w/out notifying
                        Blocking.wait(RESIZING, 1000);
                        }
                    catch (InterruptedException e)
                        {
                        Thread.currentThread().interrupt();
                        throw Base.ensureRuntimeException(e);
                        }
                    }
                }

            aeBucket = m_aeBucket;
            }

        return aeBucket;
        }

    /**
    * Register the activation of an Iterator.
    *
    * @param iter the activated iterator
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
    * Unregister the (formerly active) Iterator.
    *
    * @param iter the deactivated iterator
    */
    protected synchronized void iteratorDeactivated(Iterator iter)
        {
        Object oIterActive = m_oIterActive;
        if (oIterActive instanceof WeakReference)
            {
            // there is only one active iterator, it must be this one
            assert ((WeakReference) oIterActive).get() == iter;
            m_oIterActive = null;
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


    // ----- inner class: Entry ---------------------------------------------

    /**
    * Factory pattern: instantiate initialized Entry object.
    *
    * @param oKey    the key
    * @param oValue  the value
    * @param iHash   the hash value
    *
    * @return a new instance of the Entry class (or a subclass thereof)
    */
    protected Entry<K, V> instantiateEntry(K oKey, V oValue, int iHash)
        {
        Entry<K, V> entry = instantiateEntry();

        entry.m_oKey   = oKey;
        entry.m_oValue = oValue;
        entry.m_nHash  = iHash;

        return entry;
        }

    /**
    * Factory pattern: instantiate an un-initialized Entry object.
    *
    * @return a new instance of the Entry class (or a subclass thereof)
    */
    protected Entry<K, V> instantiateEntry()
        {
        return new Entry<>();
        }

    /**
    * A map entry (key-value pair).  The <tt>Map.entrySet</tt> method returns
    * a collection-view of the map, whose elements are of this class.
    */
    protected static class Entry<K, V>
            extends Base
            implements Map.Entry<K, V>, Cloneable, Serializable
        {
        /**
        * Returns the key corresponding to this entry.
        *
        * @return the key corresponding to this entry.
        */
        public K getKey()
            {
            return m_oKey;
            }

        /**
        * Returns the value corresponding to this entry.  If the mapping
        * has been removed from the backing map (by the iterator's
        * <tt>remove</tt> operation), the results of this call are undefined.
        *
        * @return the value corresponding to this entry.
        */
        public V getValue()
            {
            return m_oValue;
            }

        /**
        * Replaces the value corresponding to this entry with the specified
        * value (optional operation).  (Writes through to the map.)  The
        * behavior of this call is undefined if the mapping has already been
        * removed from the map (by the iterator's <tt>remove</tt> operation).
        *
        * @param value new value to be stored in this entry
        *
        * @return old value corresponding to the entry
        */
        public V setValue(V value)
            {
            V oPrev = m_oValue;
            m_oValue = value;
            return oPrev;
            }

        /**
        * Compares the specified object with this entry for equality.
        * Returns <tt>true</tt> if the given object is also a map entry and
        * the two entries represent the same mapping.  More formally, two
        * entries <tt>e1</tt> and <tt>e2</tt> represent the same mapping
        * if<pre>
        *     (e1.getKey()==null ?
        *      e2.getKey()==null : e1.getKey().equals(e2.getKey()))  &amp;&amp;
        *     (e1.getValue()==null ?
        *      e2.getValue()==null : e1.getValue().equals(e2.getValue()))
        * </pre>
        * This ensures that the <tt>equals</tt> method works properly across
        * different implementations of the <tt>Map.Entry</tt> interface.
        *
        * @param o object to be compared for equality with this map entry
        *
        * @return <tt>true</tt> if the specified object is equal to this map
        *         entry
        */
        public boolean equals(Object o)
            {
            if (o instanceof Map.Entry)
                {
                Map.Entry that = (Map.Entry) o;
                if (this == that)
                    {
                    return true;
                    }

                Object oThisKey   = this.m_oKey;
                Object oThatKey   = that.getKey();
                Object oThisValue = this.m_oValue;
                Object oThatValue = that.getValue();

                return (oThisKey   == null ? oThatKey   == null
                                           : oThisKey.equals(oThatKey))
                    && (oThisValue == null ? oThatValue == null
                                           : oThisValue.equals(oThatValue));
                }

            return false;
            }

        /**
        * Returns the hash code value for this map entry.  The hash code
        * of a map entry <tt>e</tt> is defined to be: <pre>
        *     (e.getKey()==null   ? 0 : e.getKey().hashCode()) ^
        *     (e.getValue()==null ? 0 : e.getValue().hashCode())
        * </pre>
        * This ensures that <tt>e1.equals(e2)</tt> implies that
        * <tt>e1.hashCode()==e2.hashCode()</tt> for any two Entries
        * <tt>e1</tt> and <tt>e2</tt>, as required by the general
        * contract of <tt>Object.hashCode</tt>.
        *
        * @return the hash code value for this map entry.
        */
        public int hashCode()
            {
            Object oKey   = m_oKey;
            Object oValue = m_oValue;
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
        * Clone the Entry. This will not work for non-static inner children
        * being cloned for purposes of cloning a containing Map; instead use
        * {@link #copyFrom copyFrom}.
        *
        * @return a Clone of this entry
        */
        public Object clone()
            {
            try
                {
                Entry<K, V> that = (Entry<K, V>) super.clone();
                that.m_eNext = null; // clone just this entry, not the chain
                return that;
                }
            catch (CloneNotSupportedException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        /**
        * Copy this Entry's information from another Entry. Sub-classes must
        * implement this method if they add any additional fields.
        *
        * @param entry  the entry to copy from
        */
        protected void copyFrom(Entry<K, V> entry)
            {
            m_oKey   = entry.m_oKey;
            m_oValue = entry.m_oValue;
            m_nHash  = entry.m_nHash;
            }

        /**
        * This method is invoked when the containing Map has actually
        * added this Entry to itself.
        */
        protected void onAdd()
            {
            }

        // ----- data members -------------------------------------------

        /**
        * The key.  This object reference will not change for the life of
        * the Entry.
        */
        protected K m_oKey;

        /**
        * The value. This object reference can change within the life of the
        * Entry.  This field is declared volatile so that get() and put() can
        * proceed without performing any synchronization.
        */
        protected volatile V m_oValue;

        /**
        * The key's hash code.  This value will not change for the life of
        * the Entry.
        */
        protected int m_nHash;

        /**
        * The next entry in the linked list (an open hashing implementation).
        * This field is declared volatile so the entry iterator can safely
        * operate without performing any synchronization.
        */
        protected volatile Entry<K, V> m_eNext;
        }


    // ----- inner class: EntrySet ------------------------------------------

    /**
    * Factory pattern.
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
            extends AbstractSet<Map.Entry<K, V>>
            implements Serializable
        {
        // ----- Set interface ------------------------------------------

        /**
        * Returns an iterator over the elements contained in this collection.
        *
        * @return an iterator over the elements contained in this collection.
        */
        public Iterator<Map.Entry<K, V>> iterator()
            {
            return SafeHashMap.this.isEmpty()
                   ? NullImplementation.getIterator()
                   : instantiateIterator();
            }

        /**
        * Returns the number of elements in this collection.  If the collection
        * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
        * <tt>Integer.MAX_VALUE</tt>.
        *
        * @return the number of elements in this collection.
        */
        public int size()
            {
            return SafeHashMap.this.size();
            }

        /**
        * Returns <tt>true</tt> if this collection contains the specified
        * element.  More formally, returns <tt>true</tt> if and only if this
        * collection contains at least one element <tt>e</tt> such that
        * <tt>(o==null ? e==null : o.equals(e))</tt>.<p>
        *
        * @param o object to be checked for containment in this collection
        *
        * @return <tt>true</tt> if this collection contains the specified element
        */
        public boolean contains(Object o)
            {
            if (o instanceof Map.Entry)
                {
                Map.Entry thatEntry = (Map.Entry) o;
                Map.Entry thisEntry = SafeHashMap.this.getEntryInternal(thatEntry.getKey());
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
            SafeHashMap map = SafeHashMap.this;
            synchronized (map)
                {
                if (contains(o))
                    {
                    map.remove(((Map.Entry) o).getKey());
                    return true;
                    }
                else
                    {
                    return false;
                    }
                }
            }

        /**
        * Removes all of the elements from this set of Keys by clearing the
        * underlying Map.
        */
        public void clear()
            {
            SafeHashMap.this.clear();
            }

        /**
        * Returns an array containing all of the elements in this collection.  If
        * the collection makes any guarantees as to what order its elements are
        * returned by its iterator, this method must return the elements in the
        * same order.  The returned array will be "safe" in that no references to
        * it are maintained by the collection.  (In other words, this method must
        * allocate a new array even if the collection is backed by an Array).
        * The caller is thus free to modify the returned array.<p>
        *
        * @return an array containing all of the elements in this collection
        */
        public Object[] toArray()
            {
            return toArray((Object[]) null);
            }

        /**
        * Returns an array with a runtime type is that of the specified array and
        * that contains all of the elements in this collection.  If the
        * collection fits in the specified array, it is returned there = in.
        * Otherwise, a new array is allocated with the runtime type of the
        * specified array and the size of this collection.<p>
        *
        * If the collection fits in the specified array with room to spare (i.e.,
        * the array has more elements than the collection), the element in the
        * array immediately following the end of the collection is set to
        * <tt>null</tt>.  This is useful in determining the length of the
        * collection <i>only</i> if the caller knows that the collection does
        * not contain any <tt>null</tt> elements.)<p>
        *
        * @param  a  the array into which the elements of the collection are to
        *         be stored, if it is big enough; otherwise, a new array of the
        *         same runtime type is allocated for this purpose
        *
        * @return an array containing the elements of the collection
        *
        * @throws ArrayStoreException if the runtime type of the specified array
        *         is not a supertype of the runtime type of every element in this
        *         collection
        */
        public <T> T[] toArray(T a[])
            {
            // synchronizing prevents add/remove, keeping size() constant
            SafeHashMap map = SafeHashMap.this;
            synchronized (map)
                {
                // create the array to store the map contents
                int c = map.size();
                if (a == null)
                    {
                    // implied Object[] type, see toArray()
                    a = (T[]) new Object[c];
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

                // walk all buckets
                AtomicReferenceArray<Entry> aeBucket = map.m_aeBucket;
                int                         cBuckets = aeBucket.length();
                for (int iBucket = 0, i = 0; iBucket < cBuckets; ++iBucket)
                    {
                    // walk all entries in the bucket
                    Entry entry = aeBucket.get(iBucket);
                    while (entry != null)
                        {
                        a[i++] = (T) entry;
                        entry  = entry.m_eNext;
                        }
                    }
                }

            return a;
            }


        // ----- inner class: Entry Set Iterator ------------------------

        /**
        * Factory pattern.
        *
        * @return a new instance of an Iterator over the EntrySet
        */
        protected Iterator instantiateIterator()
            {
            return new EntrySetIterator();
            }

        /**
        * An Iterator over the EntrySet that is backed by the SafeHashMap.
        */
        protected class EntrySetIterator
                extends AbstractStableIterator
            {
            // ----- constructors -----------------------------------

            /**
            * Construct an Iterator over the Entries in the SafeHashMap.
            * Special care must be taken to handle the condition in which the
            * SafeHashMap is currently resizing.
            */
            protected EntrySetIterator()
                {
                }

            // ----- internal -------------------------------------------

            /**
            * Advance to the next object.
            */
            protected void advance()
                {
                if (m_fDeactivated)
                    {
                    // the Iterator has already reached the end on a previous
                    // call to advance()
                    return;
                    }

                AtomicReferenceArray<Entry> aeBucket = this.m_aeBucket;
                if (aeBucket == null)
                    {
                    SafeHashMap map = SafeHashMap.this;
                    map.iteratorActivated(this);
                    aeBucket = this.m_aeBucket = map.getStableBucketArray();
                    }

                Entry   entry    = m_entryPrev;
                int     iBucket  = -1;          // -1 indicates no change
                boolean fResized = m_fResized;  // resize previously detected
                while (true)
                    {
                    if (entry != null)
                        {
                        // advance within the current bucket
                        entry = entry.m_eNext;
                        }

                    // check if the current bucket has been exhausted, and if
                    // it has, then advance to the first non-empty bucket
                    if (entry == null)
                        {
                        iBucket = m_iBucket;
                        int cBuckets = aeBucket.length();
                        do
                            {
                            if (++iBucket >= cBuckets)
                                {
                                // a resize could have occurred to cause this
                                if (!fResized && aeBucket != SafeHashMap.this.m_aeBucket)
                                    {
                                    // at this point, a resize has just been
                                    // detected; the handling for the resize
                                    // is below
                                    break;
                                    }

                                deactivate();
                                return;
                                }

                            entry = aeBucket.get(iBucket);
                            }
                        while (entry == null);
                        }

                    // check for a resize having occurred since the iterator
                    // was created
                    if (!fResized && aeBucket != SafeHashMap.this.m_aeBucket)
                        {
                        m_fResized = true;

                        // if there is a previously-iterated entry, the
                        // Iterator has to back up and find that same entry
                        // in the cloned list of entries in the bucket; that
                        // cloned list is used to maintain a nearly CME-free
                        // view of the Map contents after the resize has
                        // occurred.
                        if (m_entryPrev != null)
                            {
                            // wait for the resize to complete (so that the
                            // necessary clones of each entry will have been
                            // created)
                            SafeHashMap.this.getStableBucketArray();

                            // find the same entry
                            Object oKey = m_entryPrev.m_oKey;
                            entry = aeBucket.get(m_iBucket);
                            while (entry != null && entry.m_oKey != oKey)
                                {
                                entry = entry.m_eNext;
                                }
                            if (entry == null)
                                {
                                // previous has been removed, thus we don't
                                // know where to pick up the iteration and
                                // have to revert to a CME
                                deactivate();
                                throw new ConcurrentModificationException();
                                }
                            m_entryPrev = entry;
                            }

                        // since a resize has occurred, the Iterator has to
                        // start again from the last-iterated entry to find
                        // the next entry, because the entry that this
                        // Iterator had previously is a "real" entry which
                        // is in the new "current" bucket array for the Map,
                        // while the entries being iterated (post-resize) are
                        // just copies of the entries created to maintain the
                        // pre-resize iteration order
                        advance();
                        return;
                        }

                    // after a resize occurs, the entries being iterated
                    // over are no longer the "real" entries; they are simply
                    // place-holders for purpose of maintaining the order of
                    // the iterator; if this has occurred, find the real
                    // entry and make it visible from the Iterator
                    Entry entryVisible = fResized
                                       ? getEntryInternal(entry.getKey())
                                       : entry;

                    // update the current bucket index if the iterator
                    // advanced to a new bucket
                    if (iBucket >= 0)
                        {
                        m_iBucket = iBucket;
                        }


                    // after a resize, the entry could have been removed, and
                    // that would not have shown up in the pre-resize entries
                    // that this iterator is going over so check for a remove
                    if (entryVisible != null)
                        {
                        // remember the entry being iterated next; if a
                        // resize has occurred, this is a copy of the
                        // actual entry, maintained by the Iterator for
                        // purposes of providing a nearly CME-free
                        // iteration
                        m_entryPrev = entry;

                        // report back the actual entry that exists in the
                        // Map that is being iterated next
                        setNext(entryVisible);
                        return;
                        }
                    }
                }

            /**
            * Remove the specified item.
            * <p>
            * This is an optional operation. If the Iterator supports element
            * removal, then it should implement this method, which is delegated to by
            * the {@link #remove()} method.
            *
            * @param oPrev  the previously iterated object that should be removed
            */
            protected void remove(Object oPrev)
                {
                SafeHashMap.this.remove(((Map.Entry) oPrev).getKey());
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
                    // no more entries to iterate; notify the
                    // containing Map that this Iterator is no
                    // longer active against a particular version
                    // of the bucket array
                    SafeHashMap.this.iteratorDeactivated(this);
                    m_fDeactivated = true;

                    // clean up references (no longer needed)
                    m_aeBucket     = null;
                    m_entryPrev    = null;
                    }
                }

            // ----- data members -----------------------------------

            /**
            * Array of buckets in the hash map. This is a purposeful copy
            * of the hash map's reference to its buckets in order to detect
            * that a resize has occurred.
            */
            private AtomicReferenceArray<Entry> m_aeBucket;

            /**
            * Current bucket being iterated.
            */
            private int     m_iBucket = -1;

            /**
            * The most recent Entry object internally iterated. This is not
            * necessarily the same Entry object that was reported to the
            * stable iterator (via the setNext() method), since when a
            * resize occurs, the entries that are being iterated over
            * internally are the "old" Entry objects (pre-resize) while the
            * entries being returned from the Iterator are the "new" Entry
            * objects (post-resize).
            */
            private Entry   m_entryPrev;

            /**
            * Set to true when a resize has been detected.
            */
            private boolean m_fResized;

            /**
            * Set to true when the Iterator is complete.
            */
            private boolean m_fDeactivated;
            }
        }


    // ----- inner class: KeySet --------------------------------------------

    /**
    * Factory pattern.
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
            extends AbstractSet<K>
            implements Serializable
        {
        // ----- Set interface ------------------------------------------

        /**
        * Obtain an iterator over the keys in the Map.
        *
        * @return an Iterator that provides a live view of the keys in the
        *         underlying Map object
        */
        public Iterator<K> iterator()
            {
            return new Iterator()
                {
                private Iterator<Map.Entry<K, V>> m_iter = SafeHashMap.this.entrySet().iterator();

                public boolean hasNext()
                    {
                    return m_iter.hasNext();
                    }

                public K next()
                    {
                    return m_iter.next().getKey();
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
            return SafeHashMap.this.size();
            }

        /**
        * Determine if a particular key is present in the Set.
        *
        * @return true iff the passed key object is in the key Set
        */
        public boolean contains(Object oKey)
            {
            return SafeHashMap.this.containsKey(oKey);
            }

        /**
        * Removes the specified element from this Set of keys if it is
        * present by removing the associated entry from the underlying
        * Map.
        *
        * @param o  object to be removed from this set, if present
        *
        * @return true if the set contained the specified element
        */
        public boolean remove(Object o)
            {
            SafeHashMap map = SafeHashMap.this;
            synchronized (map)
                {
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
            }

        /**
        * Removes all the specified elements from this Set of keys
        * also removing any associated entry from the underlying
        * Map.
        *
        * @param c  the collection of keys to be removed from this set
        *
        * @return true if the key set was modified
        */
        @Override
        public boolean removeAll(Collection<?> c)
            {
            Objects.requireNonNull(c);
            boolean modified = false;
            for (Object e : c)
                {
                modified |= remove(e);
                }
            return modified;
            }

        /**
        * Removes all of the elements from this set of Keys by clearing the
        * underlying Map.
        */
        public void clear()
            {
            SafeHashMap.this.clear();
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
        * and that contains all of the keys in this Set.  If the Set fits
        * in the specified array, it is returned there = in. Otherwise, a new
        * array is allocated with the runtime type of the specified array
        * and the size of this collection.<p>
        *
        * If the Set fits in the specified array with room to spare (i.e.,
        * the array has more elements than the Set), the element in the
        * array immediately following the end of the Set is set to
        * <tt>null</tt>.  This is useful in determining the length of the
        * Set <i>only</i> if the caller knows that the Set does
        * not contain any <tt>null</tt> keys.)<p>
        *
        * @param  a  the array into which the elements of the Set are to
        *         be stored, if it is big enough; otherwise, a new array
        *         of the same runtime type is allocated for this purpose
        *
        * @return an array containing the elements of the Set
        *
        * @throws ArrayStoreException if the runtime type of the specified
        *         array is not a supertype of the runtime type of every
        *         element in this Set of keys
        */
        public <T> T[] toArray(T a[])
            {
            // synchronizing prevents add/remove, keeping size() constant
            SafeHashMap map = SafeHashMap.this;
            synchronized (map)
                {
                // create the array to store the map contents
                int c = map.size();
                if (a == null)
                    {
                    // implied Object[] type, see toArray()
                    a = (T[]) new Object[c];
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

                // walk all buckets
                AtomicReferenceArray<Entry> aeBucket = map.m_aeBucket;
                int                         cBuckets = aeBucket.length();
                for (int iBucket = 0, i = 0; iBucket < cBuckets; ++iBucket)
                    {
                    // walk all entries in the bucket
                    Entry entry = aeBucket.get(iBucket);
                    while (entry != null)
                        {
                        a[i++] = (T) entry.m_oKey;
                        entry  = entry.m_eNext;
                        }
                    }
                }

            return a;
            }
        }


    // ----- inner class: ValuesCollection ----------------------------------

    /**
    * Factory pattern.
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
            extends AbstractCollection<V>
            implements Serializable
        {
        // ----- Collection interface -----------------------------------

        /**
        * Obtain an iterator over the values in the Map.
        *
        * @return an Iterator that provides a live view of the values in the
        *         underlying Map object
        */
        public Iterator<V> iterator()
            {
            return new Iterator()
                {
                private Iterator<Map.Entry<K, V>> m_iter = SafeHashMap.this.entrySet().iterator();

                public boolean hasNext()
                    {
                    return m_iter.hasNext();
                    }

                public V next()
                    {
                    return m_iter.next().getValue();
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
            return SafeHashMap.this.size();
            }

        /**
        * Removes all of the elements from this Collection of values by
        * clearing the underlying Map.
        */
        public void clear()
            {
            SafeHashMap.this.clear();
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
        * Collection fits in the specified array, it is returned there = in.
        * Otherwise, a new array is allocated with the runtime type of the
        * specified array and the size of this collection.<p>
        *
        * If the Collection fits in the specified array with room to spare
        * (i.e., the array has more elements than the Collection), the
        * element in the array immediately following the end of the
        * Collection is set to <tt>null</tt>.  This is useful in determining
        * the length of the Collection <i>only</i> if the caller knows that
        * the Collection does not contain any <tt>null</tt> values.)<p>
        *
        * @param  a  the array into which the elements of the Collection are
        *         to be stored, if it is big enough; otherwise, a new
        *         array of the same runtime type is allocated for this
        *         purpose
        *
        * @return an array containing the elements of the Collection
        *
        * @throws ArrayStoreException if the runtime type of the specified
        *         array is not a supertype of the runtime type of every
        *         element in this Collection of values
        */
        public <T> T[] toArray(T a[])
            {
            // synchronizing prevents add/remove, keeping size() constant
            SafeHashMap map = SafeHashMap.this;
            synchronized (map)
                {
                // create the array to store the map contents
                int c = map.size();
                if (a == null)
                    {
                    // implied Object[] type, see toArray()
                    a = (T[]) new Object[c];
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

                // walk all buckets
                AtomicReferenceArray<Entry> aeBucket = map.m_aeBucket;
                int                         cBuckets = aeBucket.length();
                for (int iBucket = 0, i = 0; iBucket < cBuckets; ++iBucket)
                    {
                    // walk all entries in the bucket
                    Entry entry = aeBucket.get(iBucket);
                    while (entry != null)
                        {
                        a[i++] = (T) entry.m_oValue;
                        entry  = entry.m_eNext;
                        }
                    }
                }

            return a;
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * When resizing, the entries array is replaced with an empty array to
    * signify a resize.
    */
    private static final AtomicReferenceArray<Entry> NO_ENTRIES = new AtomicReferenceArray<>(0);

    /**
    * Default initial size provides a prime modulo and is large enough that
    * resize is not immediate.  (A hash map probably uses less than 128 bytes
    * initially.)
    */
    public static final int DEFAULT_INITIALSIZE = 17;

    /**
    * Biggest possible modulo.
    */
    protected static final int BIGGEST_MODULO = Integer.MAX_VALUE; // yes it's prime

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


    // ----- data members ---------------------------------------------------

    /**
    * When resizing completes, a notification is issued against this object.
    * <p>
    * Due to custom serialization this field cannot be marked as "final",
    * but must be treated as such.
    */
    protected Object RESIZING = new Object();

    /**
    * The number of entries stored in the hash map, 0 &lt;= n.  This field is
    * declared volatile to avoid synchronization for the size() operation.
    */
    protected volatile int m_cEntries;

    /**
    * The array of hash buckets.  This field is declared volatile in order to
    * reduce synchronization.
    */
    protected volatile AtomicReferenceArray<SafeHashMap.Entry> m_aeBucket;

    /**
    * The capacity of the hash map (the point at which it must resize), 1 &lt;= n.
    */
    protected int m_cCapacity;

    /**
    * The determining factor for the hash map capacity given a certain number
    * of buckets, such that capacity = bucketCount * loadFactor.
    */
    protected float m_flLoadFactor;

    /**
    * The rate of growth as a fraction of the current number of buckets,
    * 0 &lt; n, such that the hash map grows to bucketCount * (1 + growthRate)
    */
    protected float m_flGrowthRate;

    /**
    * The set of entries backed by this map.
    */
    protected transient EntrySet m_setEntries;

    /**
    * The set of keys backed by this map.
    */
    protected transient KeySet m_setKeys;

    /**
    * The collection of values backed by this map.
    */
    protected transient ValuesCollection m_colValues;

    /**
    * A holder for active Iterator(s): either WeakReference(&lt;Iterator&gt;) or
    * WeakHashMap(&lt;Iterator&gt;, null)
    */
    protected transient Object m_oIterActive;
    }
