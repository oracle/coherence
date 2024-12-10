/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.Base;
import com.tangosol.util.BitHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.FilterEnumerator;
import com.tangosol.util.LiteSet;
import com.tangosol.util.LongArray;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.SparseArray;

import java.lang.reflect.Array;

import java.sql.Time;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReferenceArray;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
* A generic cache manager.
* <p>
* The implementation is thread safe and uses a combination of
* Most Recently Used (MRU) and Most Frequently Used (MFU) caching
* strategies.
* <p>
* The cache is size-limited, which means that once it reaches its maximum
* size ("high-water mark") it prunes itself (to its "low-water mark"). The
* cache high- and low-water-marks are measured in terms of "units", and each
* cached item by default uses one unit. All of the cache constructors, except
* for the default constructor, require the maximum number of units to be
* passed in. To change the number of units that each cache entry uses, either
* set the Units property of the cache entry, or extend the Cache
* implementation so that the inner Entry class calculates its own unit size.
* To determine the current, high-water and low-water sizes of the cache, use
* the cache object's Units, HighUnits and LowUnits properties. The HighUnits
* and LowUnits properties can be changed, even after the cache is in use.
* To specify the LowUnits value as a percentage when constructing the cache,
* use the extended constructor taking the percentage-prune-level.
* <p>
* Each cached entry expires after one hour by default. To alter this
* behavior, use a constructor that takes the expiry-millis; for example, an
* expiry-millis value of 10000 will expire entries after 10 seconds. The
* ExpiryDelay property can also be set once the cache is in use, but it
* will not affect the expiry of previously cached items.
* <p>
* Cache hit statistics can be obtained from the CacheHits, CacheMisses,
* HitProbability, KeyHitProbability and CompositeHitProbability read-only
* properties. The statistics can be reset by invoking resetHitStatistics.
* The statistics are automatically reset when the cache is cleared (the clear
* method).
* <p>
* The OldCache implements the ObservableMap interface, meaning it provides
* event notifications to any interested listener for each insert, update and
* delete, including those that occur when the cache is pruned or entries
* are automatically expired.
* <p>
* This implementation is designed to support extension through inheritance.
* When overriding the inner Entry class, the OldCache.instantiateEntry
* factory method must be overridden to instantiate the correct Entry
* sub-class. To override the one-unit-per-entry default behavior, extend the
* inner Entry class and override the calculateUnits method.
*
* @author cp  2001.04.19
* @author cp  2005.05.18  moved to com.tangosol.net.cache package
*
* @deprecated As of Coherence 3.1, use {@link LocalCache} instead
*/
public class OldCache
        extends SafeHashMap
        implements ObservableMap, ConfigurableCacheMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the cache manager.
    */
    public OldCache()
        {
        this(DEFAULT_UNITS);
        }

    /**
    * Construct the cache manager.
    *
    * @param cUnits  the number of units that the cache manager will cache
    *                before pruning the cache
    */
    public OldCache(int cUnits)
        {
        this(cUnits, DEFAULT_EXPIRE);
        }

    /**
    * Construct the cache manager.
    *
    * @param cUnits         the number of units that the cache manager will
    *                       cache before pruning the cache
    * @param cExpiryMillis  the number of milliseconds that each cache entry
    *                       lives before being automatically expired
    */
    public OldCache(int cUnits, int cExpiryMillis)
        {
        this(cUnits, cExpiryMillis, DEFAULT_PRUNE);
        }

    /**
    * Construct the cache manager.
    *
    * @param cUnits         the number of units that the cache manager will
    *                       cache before pruning the cache
    * @param cExpiryMillis  the number of milliseconds that each cache entry
    *                       lives before being automatically expired
    * @param dflPruneLevel  the percentage of the total number of units that
    *                       will remain after the cache manager prunes the
    *                       cache (i.e. this is the "low water mark" value);
    *                       this value is in the range 0.0 to 1.0
    */
    public OldCache(int cUnits, int cExpiryMillis, double dflPruneLevel)
        {
        m_dflPruneLevel = Math.min(Math.max(dflPruneLevel, 0.0), 0.99);
        setHighUnits(cUnits);

        m_cExpiryDelay  = Math.max(cExpiryMillis, 0);
        }


    // ----- Map interface --------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public int size()
        {
        // check if the cache needs flushing
        evict();

        return super.size();
        }

    /**
    * {@inheritDoc}
    */
    public boolean isEmpty()
        {
        // this will call evict()
        return size() == 0;
        }

    /**
    * {@inheritDoc}
    */
    public boolean containsKey(Object key)
        {
        // check if the cache needs flushing
        tryEvict();

        return getEntryInternal(key) != null;
        }

    /**
    * {@inheritDoc}
    */
    public Object get(Object oKey)
        {
        Map.Entry entry = getEntry(oKey);
        return (entry == null ? null : entry.getValue());
        }

    /**
    * {@inheritDoc}
    */
    public SafeHashMap.Entry getEntry(Object oKey)
        {
        // check if the cache needs flushing
        tryEvict();

        Entry entry = (Entry) getEntryInternal(oKey);
        if (entry == null)
            {
            m_stats.registerMiss();
            }
        else
            {
            m_stats.registerHit();
            entry.touch();
            }

        return entry;
        }

    /**
    * {@inheritDoc}
    */
    public ConfigurableCacheMap.Entry getCacheEntry(Object oKey)
        {
        return (ConfigurableCacheMap.Entry) getEntry(oKey);
        }

    /**
    * {@inheritDoc}
    */
    public Object put(Object oKey, Object oValue)
        {
        return put(oKey, oValue, 0L);
        }

    /**
    * {@inheritDoc}
    */
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        // check if the cache needs flushing
        tryEvict();

        Entry  entry;
        Object oOrig;

        synchronized (this)
            {
            entry = (Entry) getEntryInternal(oKey);
            if (entry == null)
                {
                // new cache entry
                oOrig = super.put(oKey, oValue);
                }
            else
                {
                // cache entry already exists
                entry.touch();

                oOrig = entry.setValue(oValue);
                }

            if (cMillis != 0L)
                {
                if (entry == null)
                    {
                    entry = (Entry) getEntryInternal(oKey);
                    }
                if (entry != null)
                    {
                    entry.setExpiryMillis(cMillis > 0L ?
                            getCurrentTimeMillis() + cMillis : 0L);

                    }
                }

            // check the cache size (COH-467, COH-480)
            if (m_cCurUnits > m_cMaxUnits)
                {
                prune();

                // could have evicted the item we just inserted/updated
                if (getEntryInternal(oKey) == null)
                    {
                    oOrig = null;
                    }
                }
            }

        m_stats.registerPut(0L);
        return oOrig;
        }

    /**
    * {@inheritDoc}
    */
    public Object remove(Object oKey)
        {
        // check if the cache needs flushing
        tryEvict();

        synchronized (this)
            {
            // determine if the key is in the cache
            Entry entry = (Entry) getEntryInternal(oKey);
            if (entry == null)
                {
                return null;
                }
            else
                {
                entry.discard();
                removeEntryInternal(entry);
                return entry.getValue();
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void clear()
        {
        while (true)
            {
            try
                {
                // notify cache entries of their impending removal
                for (Entry entry : (Set<Entry>) entrySet())
                    {
                    entry.discard();
                    }

                // verify that the cache maintains its data correctly
                if (m_cCurUnits != 0L)
                    {
                    // soft assertion
                    Base.err("Invalid LocalCache unit count after clear: " + m_cCurUnits);
                    m_cCurUnits = 0L;
                    }

                if (!m_arrayExpiry.isEmpty())
                    {
                    // soft assertion
                    Base.err("LocalCache still contained " + m_arrayExpiry.getSize()
                            + " expiry items after clear.");
                    m_arrayExpiry.clear();
                    }

                break;
                }
            catch (ConcurrentModificationException e)
                {
                }
            }

        // reset the cache storage
        super.clear();

        // discard any pending evictions
        m_iterEvict = null;

        // reset hit/miss stats
        resetHitStatistics();
        }


    // ----- ObservableMap methods ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public synchronized void addMapListener(MapListener listener)
        {
        addMapListener(listener, (Filter) null, false);
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void removeMapListener(MapListener listener)
        {
        removeMapListener(listener, (Filter) null);
        }

    /**
    * {@inheritDoc}
    */
    public void addMapListener(MapListener listener, Object oKey, boolean fLite)
        {
        Base.azzert(listener != null);

        f_listenerLock.lock();
        try
            {
            MapListenerSupport support = m_listenerSupport;
            if (support == null)
                {
                support = m_listenerSupport = new MapListenerSupport();
                }

            support.addListener(listener, oKey, fLite);
            }
        finally
            {
            f_listenerLock.unlock();
            }
        }

    /**
    * {@inheritDoc}
    */
    public void removeMapListener(MapListener listener, Object oKey)
        {
        Base.azzert(listener != null);

        f_listenerLock.lock();
        try
            {
            MapListenerSupport support = m_listenerSupport;
            if (support != null)
                {
                support.removeListener(listener, oKey);
                if (support.isEmpty())
                    {
                    m_listenerSupport = null;
                    }
                }
            }
        finally
            {
            f_listenerLock.unlock();
            }
        }

    /**
    * {@inheritDoc}
    */
    public void addMapListener(MapListener listener, Filter filter, boolean fLite)
        {
        Base.azzert(listener != null);

        f_listenerLock.lock();
        try
            {
            MapListenerSupport support = m_listenerSupport;
            if (support == null)
                {
                support = m_listenerSupport = new MapListenerSupport();
                }

            support.addListener(listener, filter, fLite);
            }
        finally
            {
            f_listenerLock.unlock();
            }
        }

    /**
    * {@inheritDoc}
    */
    public void removeMapListener(MapListener listener, Filter filter)
        {
        Base.azzert(listener != null);

        f_listenerLock.lock();
        try
            {
            MapListenerSupport support = m_listenerSupport;
            if (support != null)
                {
                support.removeListener(listener, filter);
                if (support.isEmpty())
                    {
                    m_listenerSupport = null;
                    }
                }
            }
        finally
            {
            f_listenerLock.unlock();
            }
        }


    // ----- ConfigurableCacheMap interface ---------------------------------

    /**
    * {@inheritDoc}
    */
    public Map getAll(Collection colKeys)
        {
        Map map = new HashMap();
        for (Object oKey : colKeys)
            {
            Entry entry = (Entry) getEntry(oKey);
            if (entry != null)
                {
                map.put(oKey, entry.getValue());
                }
            }
        return map;
        }

    /**
    * Evict a specified key from the cache, as if it had expired from the
    * cache. If the key is not in the cache or the entry is not eligible
    * for eviction, then this method has no effect.
    *
    * @param oKey  the key to evict from the cache
    */
    public void evict(Object oKey)
        {
        Entry entry = (Entry) getEntryInternal(oKey);
        if (entry != null)
            {
            removeEvicted(entry);
            }
        }

    /**
    * Evict the specified keys from the cache, as if they had each expired
    * from the cache.
    * <p>
    * The result of this method is defined to be semantically the same as
    * the following implementation:
    *
    * <tt>
    * for (Iterator iter = colKeys.iterator(); iter.hasNext(); )
    *     {
    *     Object oKey = iter.next();
    *     evict(oKey);
    *     }
    * </tt>
    *
    * @param colKeys  a collection of keys to evict from the cache
    */
    public void evictAll(Collection colKeys)
        {
        for (Object oKey : colKeys)
            {
            Entry entry = (Entry) getEntryInternal(oKey);
            if (entry != null)
                {
                removeEvicted(entry);
                }
            }
        }

    /**
    * Attempt to call evict() when no one else is, to avoid contention on
    * opportunistic attempts at evicting.
    */
    public void tryEvict()
        {
        long lCurrent = getCurrentTimeMillis();

        if (lCurrent > m_lNextFlush &&
            m_apprvrEvict != EvictionApprover.DISAPPROVER &&
            f_evictLock.getAndSet(false))
            {
            try
                {
                // only one thread calling tryEvict() is going to contend inside evict()
                evict();
                }
            finally
                {
                f_evictLock.lazySet(true);
                }
            }
        }

    /**
    * Evict all entries from the cache that are no longer valid, and
    * potentially prune the cache size if the cache is size-limited
    * and its size is above the caching low water mark.
    */
    public void evict()
        {
        // check if flushing has been done recently
        long lCurrent = getCurrentTimeMillis();
        if (lCurrent > m_lNextFlush)
            {
            // protect against other threads attempting to evict() at the
            // same time
            synchronized (this)
                {
                if (lCurrent > m_lNextFlush && m_apprvrEvict != EvictionApprover.DISAPPROVER)
                    {
                    // protect against _this_ thread attempting to evict()
                    // at the same time (e.g. recursively as the side-effect
                    // of an event)
                    m_lNextFlush = Long.MAX_VALUE;

                    try
                        {
                        Set       setEvict    = null;
                        LongArray arrayExpiry = m_arrayExpiry;

                        if (!arrayExpiry.isEmpty() && lCurrent > arrayExpiry.getFirstIndex())
                            {
                            for (LongArray.Iterator iterKeySets = arrayExpiry.iterator();
                                    iterKeySets.hasNext(); )
                                {
                                Set setKeys = (Set) iterKeySets.next();
                                if (setKeys != null && lCurrent > iterKeySets.getIndex())
                                    {
                                    iterKeySets.remove();

                                    if (setEvict == null)
                                        {
                                        setEvict = setKeys;
                                        }
                                    else
                                        {
                                        setEvict.addAll(setKeys);
                                        }
                                    }
                                else
                                    {
                                    break;
                                    }
                                }
                            }
                        if (setEvict != null)
                            {
                            evictAll(setEvict);
                            }
                        }
                    finally
                        {
                        // don't allow another flush for a quarter second
                        // (the expiry has a quarter-second granularity; see
                        // setExpiryMillis)
                        m_lNextFlush = getCurrentTimeMillis() + 0x100L;
                        }
                    }
                }
            }
        }

    /**
    * Returns the CacheStatistics for this cache.
    *
    * @return a CacheStatistics object
    */
    public CacheStatistics getCacheStatistics()
        {
        return m_stats;
        }


    // ----- inner class: EntrySet ------------------------------------------

    /**
    * Factory pattern.
    *
    * @return a new instance of the EntrySet class (or a subclass thereof)
    */
    protected SafeHashMap.EntrySet instantiateEntrySet()
        {
        return new EntrySet();
        }

    /**
    * A set of entries backed by this map.
    */
    protected class EntrySet
            extends SafeHashMap.EntrySet
        {
        // ----- Set interface ------------------------------------------

        /**
        * Returns an iterator over the elements contained in this collection.
        *
        * @return an iterator over the elements contained in this collection.
        */
        public Iterator iterator()
            {
            // optimization
            if (OldCache.super.isEmpty())
                {
                return NullImplementation.getIterator();
                }

            // complete entry set iterator
            Iterator iter = instantiateIterator();

            // filter to get rid of expired objects
            Filter<Entry> filter = (Entry entry) -> !removeIfExpired(entry);

            return new FilterEnumerator(iter, filter);
            }

        /**
        * Returns an array with a runtime type is that of the specified array and
        * that contains all of the elements in this collection.  If the
        * collection fits in the specified array, it is returned therein.
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
        * @param  ao  the array into which the elements of the collection are to
        * 	     be stored, if it is big enough; otherwise, a new array of the
        * 	     same runtime type is allocated for this purpose
        *
        * @return an array containing the elements of the collection
        *
        * @throws ArrayStoreException if the runtime type of the specified array
        *         is not a supertype of the runtime type of every element in this
        *         collection
        */
        public Object[] toArray(Object ao[])
            {
            Object[] aoAll = super.toArray(ao);
            int      cAll  = aoAll.length;

            int ofSrc  = 0;
            int ofDest = 0;
            while (ofSrc < cAll)
                {
                Entry entry = (Entry) aoAll[ofSrc];
                if (entry == null)
                    {
                    // this happens when ao is passed in and is larger than
                    // the number of entries
                    break;
                    }
                else if (removeIfExpired(entry))
                    {
                    //no-op
                    }
                else
                    {
                    if (ofSrc > ofDest)
                        {
                        aoAll[ofDest] = aoAll[ofSrc];
                        }
                    ++ofDest;
                    }
                ++ofSrc;
                }

            if (ofSrc == ofDest)
                {
                // no entries expired; return the original array
                return aoAll;
                }

            if (ao == aoAll)
                {
                // this is the same array as was passed in; per the toArray
                // contract, null the element past the end of the non-expired
                // entries (since we removed at least one entry) and return it
                ao[ofDest] = null;
                return ao;
                }

            // resize has to occur because we've removed some of the
            // entries because they were expired
            if (ao == null)
                {
                ao = new Object[ofDest];
                }
            else
                {
                ao = (Object[]) Array.newInstance(ao.getClass().getComponentType(), ofDest);
                }

            System.arraycopy(aoAll, 0, ao, 0, ofDest);
            return ao;
            }
        }


    // ----- inner class: KeySet --------------------------------------------

    /**
    * Factory pattern.
    *
    * @return a new instance of the KeySet class (or subclass thereof)
    */
    protected SafeHashMap.KeySet instantiateKeySet()
        {
        return new KeySet();
        }

    /**
    * A set of entries backed by this map.
    */
    protected class KeySet
            extends SafeHashMap.KeySet
        {
        // ----- Set interface ------------------------------------------

        /**
        * Returns an array with a runtime type is that of the specified array and
        * that contains all of the elements in this collection.  If the
        * collection fits in the specified array, it is returned therein.
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
        * @param  ao  the array into which the elements of the collection are to
        * 	     be stored, if it is big enough; otherwise, a new array of the
        * 	     same runtime type is allocated for this purpose
        *
        * @return an array containing the elements of the collection
        *
        * @throws ArrayStoreException if the runtime type of the specified array
        *         is not a supertype of the runtime type of every element in this
        *         collection
        */
        public Object[] toArray(Object ao[])
            {
            // build list of non-expired keys
            Object[] aoAll;
            int      cAll = 0;

            // synchronizing prevents add/remove, keeping size() constant
            OldCache map = OldCache.this;
            synchronized (map)
                {
                // create the array to store the map keys
                int c = size();
                aoAll = new Object[c];
                if (c > 0)
                    {
                    // walk all buckets
                    AtomicReferenceArray aeBucket = map.m_aeBucket;
                    for (int i = 0; i < aeBucket.length(); i++)
                        {
                        // walk all entries in the bucket
                        Entry entry = (Entry) aeBucket.get(i);
                        while (entry != null)
                            {
                            if (removeIfExpired(entry))
                                {
                                //no-op
                                }
                            else
                                {
                                aoAll[cAll++] = entry.getKey();
                                }
                            entry = entry.getNext();
                            }
                        }
                    }
                }

            // if no entries had expired, just return the "work" array
            if (ao == null && cAll == aoAll.length)
                {
                return aoAll;
                }

            // allocate the necessary array (or stick the null in at the
            // right place) per the Map spec
            if (ao == null)
                {
                ao = new Object[cAll];
                }
            else if (ao.length < cAll)
                {
                ao = (Object[]) Array.newInstance(ao.getClass().getComponentType(), cAll);
                }
            else if (ao.length > cAll)
                {
                ao[cAll] = null;
                }

            // copy the data into the array to return and return it
            if (cAll > 0)
                {
                System.arraycopy(aoAll, 0, ao, 0, cAll);
                }
            return ao;
            }

        /**
        * {@inheritDoc}
        */
        public int size()
            {
            // COH-1089: get the size value without causing any eviction
            //           (see SafeHashMap#size)
            return OldCache.super.size();
            }
        }

    // ----- inner class: ValuesCollection ----------------------------------

    /**
    * Factory pattern.
    *
    * @return a new instance of the ValuesCollection class (or subclass
    *         thereof)
    */
    protected SafeHashMap.ValuesCollection instantiateValuesCollection()
        {
        return new ValuesCollection();
        }

    /**
    * A collection of values backed by this map.
    */
    protected class ValuesCollection
            extends SafeHashMap.ValuesCollection
        {
        // ----- Collection interface -----------------------------------

        /**
        * Returns an array with a runtime type is that of the specified array and
        * that contains all of the elements in this collection.  If the
        * collection fits in the specified array, it is returned therein.
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
        * @param  ao  the array into which the elements of the collection are to
        * 	     be stored, if it is big enough; otherwise, a new array of the
        * 	     same runtime type is allocated for this purpose
        *
        * @return an array containing the elements of the collection
        *
        * @throws ArrayStoreException if the runtime type of the specified array
        *         is not a supertype of the runtime type of every element in this
        *         collection
        */
        public Object[] toArray(Object ao[])
            {
            // build list of non-expired values
            Object[] aoAll;
            int      cAll = 0;

            // synchronizing prevents add/remove, keeping size() constant
            OldCache map = OldCache.this;
            synchronized (map)
                {
                // create the array to store the map values
                int c = size();
                aoAll = new Object[c];
                if (c > 0)
                    {
                    // walk all buckets
                    AtomicReferenceArray aeBucket = map.m_aeBucket;
                    for (int i = 0; i < aeBucket.length(); i++)
                        {
                        // walk all entries in the bucket
                        Entry entry = (Entry) aeBucket.get(i);
                        while (entry != null)
                            {
                            if (removeIfExpired(entry))
                                {
                                //no-op
                                }
                            else
                                {
                                aoAll[cAll++] = entry.getValue();
                                }
                            entry = entry.getNext();
                            }
                        }
                    }
                }

            // if no entries had expired, just return the "work" array
            if (ao == null && cAll == aoAll.length)
                {
                return aoAll;
                }

            // allocate the necessary array (or stick the null in at the
            // right place) per the Map spec
            if (ao == null)
                {
                ao = new Object[cAll];
                }
            else if (ao.length < cAll)
                {
                ao = (Object[]) Array.newInstance(ao.getClass().getComponentType(), cAll);
                }
            else if (ao.length > cAll)
                {
                ao[cAll] = null;
                }

            // copy the data into the array to return and return it
            if (cAll > 0)
                {
                System.arraycopy(aoAll, 0, ao, 0, cAll);
                }
            return ao;
            }

        /**
         * {@inheritDoc}
         */
        public int size()
            {
            // COH-1089: get the size value without causing any eviction
            //           (see SafeHashMap#size)
            return OldCache.super.size();
            }
        }

    // ----- Object methods -------------------------------------------------

    /**
    * For debugging purposes, format the contents of the cache as a String.
    *
    * @return a String representation of the cache contents
    */
    public synchronized String toString()
        {
        while (true)
            {
            try
                {
                StringBuilder sb = new StringBuilder("Cache {\n");
                int i = 0;
                for (Object entry : entrySet())
                    {
                    sb.append('[')
                      .append(i++)
                      .append("]: ")
                      .append(entry)
                      .append('\n');
                    }
                sb.append('}');
                return sb.toString();
                }
            catch (ConcurrentModificationException e)
                {
                }
            }
        }


    // ----- Cache management methods ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public int getUnits()
        {
        return toExternalUnits(m_cCurUnits, getUnitFactor());
        }

    /**
    * {@inheritDoc}
    */
    public int getHighUnits()
        {
        return toExternalUnits(m_cMaxUnits, getUnitFactor());
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void setHighUnits(int cMax)
        {
        long cUnits = toInternalUnits(cMax, getUnitFactor());

        m_cMaxUnits   = cUnits;
        m_cPruneUnits = cUnits == Long.MAX_VALUE ? cUnits : (long) (m_dflPruneLevel * cUnits);

        if (m_cCurUnits > cUnits)
            {
            prune();
            }
        }

    /**
    * {@inheritDoc}
    */
    public int getLowUnits()
        {
        return toExternalUnits(m_cPruneUnits, getUnitFactor());
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void setLowUnits(int cMin)
        {
        long cUnits = toInternalUnits(cMin, getUnitFactor());
        long cMax   = m_cMaxUnits;
        if (cUnits >= cMax)
            {
            cUnits = (long) (m_dflPruneLevel * cMax);
            }
        else if (cMax == Long.MAX_VALUE)
            {
            // no max indicates no min
            cUnits = cMax;
            }

        m_cPruneUnits = cUnits;
        }

    /**
    * {@inheritDoc}
    */
    public int getUnitFactor()
        {
        return m_nUnitFactor;
        }

    /**
    * {@inheritDoc}
    */
    public void setUnitFactor(int nFactor)
        {
        if (nFactor == m_nUnitFactor)
            {
            return;
            }

        if (nFactor < 1)
            {
            throw new IllegalArgumentException("unit factor must be >= 1");
            }

        if (m_cCurUnits > 0)
            {
            throw new IllegalStateException(
                    "unit factor cannot be set after the cache has been populated");
            }

        // only adjust the max units if there was no unit factor set previously
        if (m_nUnitFactor == 1 && m_cMaxUnits != Long.MAX_VALUE)
            {
            m_cMaxUnits   *= nFactor;
            m_cPruneUnits *= nFactor;
            }

        m_nUnitFactor = nFactor;
        }

    /**
    * Convert from an external 32-bit unit value to an internal 64-bit unit
    * value using the configured units factor.
    *
    * @param cUnits   an external 32-bit units value
    * @param nFactor  the unit factor
    *
    * @return an internal 64-bit units value
    */
    protected static long toInternalUnits(int cUnits, int nFactor)
        {
        return cUnits <= 0 || cUnits == Integer.MAX_VALUE
               ? Long.MAX_VALUE
               : ((long) cUnits) * nFactor;
        }

    /**
    * Convert from an internal 64-bit unit value to an external 32-bit unit
    * value using the configured units factor.
    *
    * @param cUnits   an internal 64-bit units value
    * @param nFactor  the unit factor
    *
    * @return an external 32-bit units value
    */
    protected static int toExternalUnits(long cUnits, int nFactor)
        {
        if (cUnits == 0L || cUnits == Long.MAX_VALUE)
            {
            return 0;
            }

        if (nFactor > 1)
            {
            cUnits = (cUnits + nFactor - 1) / nFactor;
            }

        return cUnits > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) cUnits;
        }

    /**
    * Determine the current eviction type.
    *
    * @return one of the EVICTION_POLICY_* enumerated values
    */
    public int getEvictionType()
        {
        return m_nEvictionType;
        }

    /**
    * Specify the eviction type for the cache. The type can only be
    * set to an external policy if an EvictionPolicy object has been
    * provided.
    *
    * @param nType  one of the EVICTION_POLICY_* enumerated values
    */
    public synchronized void setEvictionType(int nType)
        {
        configureEviction(nType, null);
        }

    /**
    * {@inheritDoc}
    */
    public ConfigurableCacheMap.EvictionPolicy getEvictionPolicy()
        {
        ConfigurableCacheMap.EvictionPolicy policy = m_policy;

        if (policy == null)
            {
            switch (getEvictionType())
                {
                default:
                case EVICTION_POLICY_HYBRID:
                    policy = INSTANCE_HYBRID;
                    break;
                case EVICTION_POLICY_LRU:
                    policy = INSTANCE_LRU;
                    break;
                case EVICTION_POLICY_LFU:
                    policy = INSTANCE_LFU;
                    break;
                }
            }

        return policy;
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void setEvictionPolicy(ConfigurableCacheMap.EvictionPolicy policy)
        {
        int nType = (policy == null ? EVICTION_POLICY_HYBRID
                                    : EVICTION_POLICY_EXTERNAL);
        configureEviction(nType, policy);
        }

    /**
    * {@inheritDoc}
    */
    public ConfigurableCacheMap.EvictionApprover getEvictionApprover()
        {
        return m_apprvrEvict;
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void setEvictionApprover(ConfigurableCacheMap.EvictionApprover approver)
        {
        m_apprvrEvict = approver;
        }

    /**
    * Determine the current unit calculator type.
    *
    * @return one of the UNIT_CALCULATOR_* enumerated values
    */
    public int getUnitCalculatorType()
        {
        return m_nCalculatorType;
        }

    /**
    * Specify the unit calculator type for the cache. The type can only be
    * set to an external unit calculator if a UnitCalculator object has been
    * provided.
    *
    * @param nType  one of the UNIT_CALCULATOR_* enumerated values
    */
    public void setUnitCalculatorType(int nType)
        {
        configureUnitCalculator(nType, null);
        }

    /**
    * {@inheritDoc}
    */
    public ConfigurableCacheMap.UnitCalculator getUnitCalculator()
        {
        ConfigurableCacheMap.UnitCalculator calculator = m_calculator;

        if (calculator == null)
            {
            calculator = getUnitCalculatorType() == UNIT_CALCULATOR_BINARY
                    ? BinaryMemoryCalculator.INSTANCE
                    : InternalUnitCalculator.INSTANCE;
            }

        return calculator;
        }

    /**
    * {@inheritDoc}
    */
    public void setUnitCalculator(ConfigurableCacheMap.UnitCalculator calculator)
        {
        int nType = (calculator == null
                ? UNIT_CALCULATOR_FIXED
                : UNIT_CALCULATOR_EXTERNAL);
        configureUnitCalculator(nType, calculator);
        }

    /**
    * {@inheritDoc}
    */
    public int getExpiryDelay()
        {
        return m_cExpiryDelay;
        }

    /**
    * {@inheritDoc}
    */
    public void setExpiryDelay(int cMillis)
        {
        m_cExpiryDelay = Math.max(cMillis, 0);
        }

    /**
     * {@inheritDoc}
     */
    public long getNextExpiryTime()
        {
        LongArray arrayExpiry = m_arrayExpiry;
        return arrayExpiry.isEmpty() ? 0 : arrayExpiry.getFirstIndex();
        }

    /**
    * Determine the date/time at which the next cache flush is scheduled.
    * Note that the date/time may be Long.MAX_VALUE, which implies that a
    * flush will never occur. Also note that the cache may internally adjust
    * the flush time to prevent a flush from occurring during certain
    * processing as a means to raise concurrency.
    *
    * @return the date/time value, in milliseconds, when the cache will next
    *         automatically flush
    *
    * @deprecated as of Coherence 3.5
    */
    public long getFlushTime()
        {
        return 0L;
        }

    /**
    * Specify the date/time at which the next cache flush is to occur.
    * Note that the date/time may be Long.MAX_VALUE, which implies that a
    * flush will never occur. A time in the past or at the present will
    * cause an immediate flush.
    *
    * @param lMillis  the date/time value, in milliseconds, when the cache
    *                 should next automatically flush
    *
    * @deprecated as of Coherence 3.5
    */
    public void setFlushTime(long lMillis)
        {
        // no-op
        }

    /**
    * Determine if incremental eviction is enabled. (Incremental eviction is
    * not supported for custom eviction policies.)
    *
    * @return true if eviction is incremental; false if it is done in bulk
    *
    * @since Coherence 3.5
    */
    public boolean isIncrementalEviction()
        {
        return m_fIncrementalEvict;
        }

    /**
    * Specify whether incremental eviction is enabled.
    *
    * @param fIncrementalEvict  pass true to enable incremental eviction;
    *                           false to disable incremental eviction
    *
    * @since Coherence 3.5
    */
    public synchronized void setIncrementalEviction(boolean fIncrementalEvict)
        {
        m_fIncrementalEvict = fIncrementalEvict;
        if (!fIncrementalEvict)
            {
            m_iterEvict = null;
            }
        }


    // ----- statistics -----------------------------------------------------

    /**
    * Determine the rough number of cache hits since the cache statistics
    * were last reset.
    *
    * @return the number of {@link #get} calls that have been served by
    *         existing cache entries
    */
    public long getCacheHits()
        {
        return m_stats.getCacheHits();
        }

    /**
    * Determine the rough number of cache misses since the cache statistics
    * were last reset.
    *
    * @return the number of {@link #get} calls that failed to find an
    *         existing cache entry because the requested key was not in the
    *         cache
    */
    public long getCacheMisses()
        {
        return m_stats.getCacheMisses();
        }

    /**
    * Determine the rough probability (0 &lt;= p &lt;= 1) that any particular
    * {@link #get} invocation will be satisfied by an existing entry in
    * the cache, based on the statistics collected since the last reset
    * of the cache statistics.
    *
    * @return the cache hit probability (0 &lt;= p &lt;= 1)
    */
    public double getHitProbability()
        {
        return m_stats.getHitProbability();
        }

    /**
    * Reset the cache statistics.
    */
    public void resetHitStatistics()
        {
        m_stats.resetHitStatistics();
        }


    // ----- internal methods -----------------------------------------------

    /**
    * Configure the eviction type and policy.
    *
    * @param nType   one of the EVICTION_POLICY_* enumerated values
    * @param policy  an external eviction policy, or null
    */
    protected synchronized void configureEviction(
            int nType, ConfigurableCacheMap.EvictionPolicy policy)
        {
        switch (nType)
            {
            case EVICTION_POLICY_HYBRID:
            case EVICTION_POLICY_LRU:
            case EVICTION_POLICY_LFU:
                policy = null;
                break;

            case EVICTION_POLICY_EXTERNAL:
                if (policy == null)
                    {
                    // just use the default
                    nType = EVICTION_POLICY_HYBRID;
                    }
                else if (policy instanceof InternalEvictionPolicy)
                    {
                    nType  = ((InternalEvictionPolicy) policy).getEvictionType();
                    policy = null;
                    }
                break;

            default:
                throw new IllegalArgumentException("unknown eviction type: " + nType);
            }

        ConfigurableCacheMap.EvictionPolicy policyPrev = m_policy;
        if (policyPrev instanceof MapListener)
            {
            removeMapListener((MapListener) policyPrev);
            }

        m_nEvictionType = nType;
        m_policy        = policy;
        m_iterEvict     = null;

        if (policy instanceof MapListener)
            {
            addMapListener((MapListener) policy);
            }
        }

    /**
    * Configure the unit calculator type and implementation.
    *
    * @param nType       one of the UNIT_CALCULATOR_* enumerated values
    * @param calculator  an external unit calculator, or null
    */
    protected synchronized void configureUnitCalculator(
            int nType, ConfigurableCacheMap.UnitCalculator calculator)
        {
        switch (nType)
            {
            case UNIT_CALCULATOR_EXTERNAL:
                if (calculator == null)
                    {
                    // just use the default
                    nType = UNIT_CALCULATOR_FIXED;
                    }
                else if (calculator == InternalUnitCalculator.INSTANCE)
                    {
                    nType      = UNIT_CALCULATOR_FIXED;
                    calculator = null;
                    }
                else if (calculator == BinaryMemoryCalculator.INSTANCE)
                    {
                    nType      = UNIT_CALCULATOR_BINARY;
                    calculator = null;
                    }
                else if (UNIT_CALCULATOR_EXTERNAL == m_nCalculatorType &&
                         Base.equals(calculator, m_calculator))
                    {
                    // nothing to do
                    return;
                    }
                else
                    {
                    break;
                    }
                // fall through

            case UNIT_CALCULATOR_FIXED:
            case UNIT_CALCULATOR_BINARY:
                if (nType == m_nCalculatorType)
                    {
                    // nothing to do
                    return;
                    }
                break;

            default:
                throw new IllegalArgumentException(
                        "unknown unit calculator type: " + nType);
            }

        m_nCalculatorType = nType;
        m_calculator      = calculator;

        // recalculate unit costs
        for (Iterator iter = entrySet().iterator(); iter.hasNext(); )
            {
            Entry entry  = (Entry) iter.next();
            int   cUnits = entry.calculateUnits(entry.getValue());

            // update both the entry unit count and total unit count
            entry.setUnits(cUnits);
            }
        }

    /**
    * Locate an Entry in the hash map based on its key. If the Entry has
    * expired and is eligible for eviction, it is removed from the hash map.
    * <p>
    * Unlike the {@link #getEntry} method, this method does not flush the cache
    * (if necessary) or update cache statistics.
    *
    * @param oKey the key object to search for
    *
    * @return the Entry or null if the entry is not found in the hash map or
    *         has expired
    */
    protected SafeHashMap.Entry getEntryInternal(Object oKey)
        {
        Entry entry = (Entry) super.getEntryInternal(oKey);

        if (entry != null && removeIfExpired(entry))
            {
            entry = null;
            }

        return entry;
        }

    /**
    * Remove an entry (if it is eligible for eviction) because it has expired.
    * <p>
    * Note: This method is the same as {@link #removeEvicted(Entry)} and is left
    * for backward compatibility.
    *
    * @param entry            the expired cache entry
    * @param fRemoveInternal  true if the cache entry still needs to be
    *                         removed from the cache
    *
    * @return true iff the entry was removed
    *
    * @deprecated use {@link #removeEvicted(Entry)} instead
    */
    protected boolean removeExpired(OldCache.Entry entry, boolean fRemoveInternal)
        {
        return removeEvicted(entry);
        }

    /**
    * Remove an entry (if it is eligible for eviction) because it has expired.
    *
    * @param entry  the expired cache entry
    *
    * @return true iff the entry was removed
    */
    protected synchronized boolean removeEvicted(Entry entry)
        {
        ConfigurableCacheMap.EvictionApprover appr = m_apprvrEvict;
        if (appr == null || appr.isEvictable(entry))
            {
            entry.discard();
            removeEntryInternal(entry);
            return true;
            }
        else
            {
            return false;
            }
        }

    /**
    * Remove an entry if it has expired.
    *
    * @param entry  the entry
    *
    * @return true iff the entry was actually removed
    */
    protected boolean removeIfExpired(Entry entry)
        {
        if (entry.isExpired() && m_apprvrEvict != EvictionApprover.DISAPPROVER)
            {
            synchronized (this)
                {
                if (entry.isExpired())
                    {
                    return removeEvicted(entry);
                    }
                }
            }
        return false;
        }

    /**
    * Adjust current size.
    *
    * @param cDelta  the delta units to adjust to
    */
    protected synchronized void adjustUnits(int cDelta)
        {
        m_cCurUnits += cDelta;
        }

    /**
    * Prune the cache by discarding the lowest priority cache entries.
    */
    protected synchronized void prune()
        {
        long cMax = m_cMaxUnits;
        if (m_cCurUnits <= cMax || m_apprvrEvict == EvictionApprover.DISAPPROVER)
            {
            return;
            }

        // COH-764: prioritize reclaiming of expired entries

        // Note: it is deliberate that prune() calls evict() and *not* vice-versa
        //       as prune may call into a custom EvictionPolicy that could legally
        //       call a 'read' method on this map resulting in a call to evict (stack overflow)
        evict();
        if (m_cCurUnits < cMax)
            {
            return;
            }

        // start a new eviction cycle
        long ldtStart = getCurrentTimeMillis();
        int  nType    = getEvictionType();
        if (nType == EVICTION_POLICY_EXTERNAL)
            {
            getEvictionPolicy().requestEviction(getLowUnits());
            }
        else
            {
            // first attempt to continue a previous incremental eviction
            if (m_iterEvict != null)
                {
                pruneIncremental();
                if (m_cCurUnits < cMax)
                    {
                    m_stats.registerIncrementalCachePrune(ldtStart);
                    return;
                    }
                }

            long      cTarget   = m_cPruneUnits;
            ArrayList listEvict = null;
            long      cRemEvict = m_cCurUnits - cTarget;
            boolean   fLRU      = (nType == EVICTION_POLICY_LRU);

            // if eviction is being deferred, create a list of items to evict
            //  (attempting to pre-size the list in such a way that it will
            // hold the necessary amount of items without resizing)
            if (isIncrementalEviction())
                {
                double dflEstPct = Math.max(0.01, Math.min(1.0,
                        1.0 - (((double) cTarget) / (cMax + 1L)) + .005));
                listEvict = new ArrayList((int) (dflEstPct * super.size()) + 10);
                }

            switch (nType)
                {
                default:
                case EVICTION_POLICY_HYBRID:
                    {
                    int         cLists = 11;
                    ArrayList[] alist = new ArrayList[cLists];
                    int         cGuess = super.size() >>> 4;
                    for (int i = 0; i < cLists; ++i)
                        {
                        alist[i] = new ArrayList(cGuess);
                        }

                    // calculate a rough average number of touches that each
                    // entry should expect to have
                    CacheStatistics stats = getCacheStatistics();
                    m_cAvgTouch = (int) ((stats.getTotalPuts() + stats.getTotalGets())
                                / ((super.size() + 1L) * (stats.getCachePrunes() + 1L)));

                    // sort the entries by their priorities to be retained
                    AtomicReferenceArray aeBucket = m_aeBucket;
                    for (int i = 0; i < aeBucket.length(); i++)
                        {
                        Entry entry = (Entry) aeBucket.get(i);
                        while (entry != null)
                            {
                            alist[entry.getPriority()].add(entry);
                            entry = entry.getNext();
                            }
                        }

                    // build a list of the items to evict incrementally,
                    // from the lowest (10) priority to the highest (0)
                    // until the cache will drop to its low units
                    ForEachPriority: for (int i = cLists - 1; i >= 0; --i)
                        {
                        for (Object entry : alist[i])
                            {
                            cRemEvict -= queueForEviction((Entry) entry, listEvict);
                            if (cRemEvict <= 0L)
                                {
                                break ForEachPriority;
                                }
                            }
                        }
                    }
                    break;

                case EVICTION_POLICY_LRU:
                case EVICTION_POLICY_LFU:
                    {
                    SparseArray array = new SparseArray();

                    // sort the entries by their recentness / frequentness of use
                    AtomicReferenceArray aeBucket = m_aeBucket;
                    for (int i = 0; i < aeBucket.length(); i++)
                        {
                        Entry entry = (Entry) aeBucket.get(i);
                        while (entry != null)
                            {
                            long lOrder = fLRU ? entry.getLastTouchMillis()
                                    : entry.getTouchCount();
                            Object oPrev = array.set(lOrder, entry);
                            if (oPrev != null)
                                {
                                // oops, more than one entry with the same order;
                                // make a list of entries
                                List list;
                                if (oPrev instanceof List)
                                    {
                                    list = (List) oPrev;
                                    }
                                else
                                    {
                                    list = new ArrayList();
                                    list.add(oPrev);
                                    }
                                list.add(entry);
                                array.set(lOrder, list);
                                }
                            entry = entry.getNext();
                            }
                        }

                    // evict from the least to the most frequently / recently
                    // used until the cache has dropped below its low units
                    ForEachEntry:
                    for (Object o : array)
                        {
                        if (o instanceof Entry)
                            {
                            cRemEvict -= queueForEviction((Entry) o, listEvict);
                            if (cRemEvict <= 0L)
                                {
                                break;
                                }
                            }
                        else
                            {
                            List list = (List) o;
                            for (Object entry : list)
                                {
                                cRemEvict -= queueForEviction((Entry) entry, listEvict);
                                if (cRemEvict <= 0L)
                                    {
                                    break ForEachEntry;
                                    }
                                }
                            }
                        }
                    }
                    break;
                }

            if (!fLRU)
                {
                // reset touch counts
                AtomicReferenceArray aeBucket = m_aeBucket;
                for (int i = 0; i < aeBucket.length(); i++)
                    {
                    Entry entry = (Entry) aeBucket.get(i);
                    while (entry != null)
                        {
                        entry.resetTouchCount();
                        entry = entry.getNext();
                        }
                    }
                }

            // store off the list of pending evictions
            if (listEvict != null)
                {
                m_iterEvict = listEvict.listIterator();
                }

            // make a first pass at the pending evictions
            pruneIncremental();
            }

        m_stats.registerCachePrune(ldtStart);
        m_lLastPrune = getCurrentTimeMillis();
        }

    /**
    * When determining items to evict, they are either evicted immediately or
    * their eviction is deferred. This method is responsible for handling
    * both of those cases as items are selected for eviction.
    *
    * @param entry      the entry to evict
    * @param listEvict  the list to defer to if deferring, otherwise null
    *
    * @return the number of units queued for eviction (or evicted)
    *
    * @since Coherence 3.5
    */
    private int queueForEviction(Entry entry, List listEvict)
        {
        int cUnits = entry.getUnits();

        if (listEvict == null)
            {
            // try to evict now, if we don't succeed,
            // then zero units were queued
            if (!removeEvicted(entry))
                {
                cUnits = 0;
                }
            }
        else
            {
            // defer eviction
            entry.setEvictable(true);
            listEvict.add(entry.getKey());
            }

        return cUnits;
        }

    /**
    * Incrementally evict some entries that were previously selected for
    * eviction.
    *
    * @since Coherence 3.5
    */
    private void pruneIncremental()
        {
        ListIterator iterEvict = m_iterEvict;
        if (iterEvict != null)
            {
            // pruning will proceed until the cache is down below the max
            // units, but since there's a cost to this processing, do some
            // arbitrary minimum number of evictions while we're here
            long cMaxUnits   = m_cMaxUnits;
            int  cMinEntries = 60;
            while (iterEvict.hasNext())
                {
                Entry entry = (Entry) getEntryInternal(iterEvict.next());

                iterEvict.set(null); // COH-27922 - release the reference to the entry so that it can be garbage collected
                if (entry != null && entry.isEvictable() &&
                    removeEvicted(entry) &&
                    --cMinEntries <= 0 && m_cCurUnits < cMaxUnits)
                    {
                    return;
                    }
                }

            m_iterEvict = null;
            }
        }

    /**
    * Check if any entries in the cache have expired, and evict them if they
    * have.
    *
    * @deprecated as of Coherence 3.5, use {@link #evict()}
    */
    protected void checkFlush()
        {
        evict();
        }

    /**
    * Factory pattern: instantiate a new MapEvent corresponding
    * to the specified parameters.
    *
    * @param nId         the event Id
    * @param oKey        the key
    * @param oValueOld   the old value
    * @param  oValueNew  the new value
    *
    * @return a new instance of the MapEvent class (or a subclass thereof)
    */
    protected MapEvent instantiateMapEvent(
            int nId, Object oKey, Object oValueOld, Object oValueNew)
        {
        return new MapEvent(this, nId, oKey, oValueOld, oValueNew);
        }


    // ----- event dispatching ----------------------------------------------

    /**
    * Accessor for the MapListenerSupport for sub-classes.
    *
    * @return the MapListenerSupport, or null if there are no listeners
    */
    protected MapListenerSupport getMapListenerSupport()
        {
        return m_listenerSupport;
        }

    /**
    * Determine if the OldCache has any listeners at all.
    *
    * @return true iff this OldCache has at least one MapListener
    */
    protected boolean hasListeners()
        {
        // m_listenerSupport defaults to null, and it is reset to null when
        // the last listener unregisters
        return m_listenerSupport != null;
        }

    /**
    * Dispatch the passed event.
    *
    * @param evt  a CacheEvent object
    */
    protected void dispatchEvent(MapEvent evt)
        {
        MapListenerSupport listenerSupport = getMapListenerSupport();
        if (listenerSupport != null)
            {
            // the events can only be generated while the current thread
            // holds the monitor on this map
            synchronized (this)
                {
                listenerSupport.fireEvent(evt, false);
                }
            }
        }

    /**
    * Return the current {@link Base#getSafeTimeMillis() safe time} or
    * {@link Base#getLastSafeTimeMillis last safe time}
    * depending on the optimization flag.
    *
    * @return the current time
    */
    public long getCurrentTimeMillis()
        {
        return m_fOptimizeGetTime ?
            Base.getLastSafeTimeMillis() : Base.getSafeTimeMillis();
        }

    /**
    * Specify whether or not this cache is used in the environment,
    * where the {@link Base#getSafeTimeMillis()} is used very frequently and
    * as a result, the {@link Base#getLastSafeTimeMillis} could be used
    * without sacrificing the clock precision. By default, the optimization
    * is off.
    *
    * @param fOptimize  pass true to turn the "last safe time" optimization on
    */
    public void setOptimizeGetTime(boolean fOptimize)
        {
        m_fOptimizeGetTime = fOptimize;
        }

    // ----- inner class: Entry ---------------------------------------------

    /**
    * Factory method.  This method exists to allow the OldCache class to be
    * easily inherited from by allowing the Entry class to be easily
    * sub-classed.
    *
    * @return an instance of Entry that holds the passed cache value
    */
    protected SafeHashMap.Entry instantiateEntry()
        {
        return new Entry();
        }

    /**
    * A holder for a cached value.
    *
    * @author cp  2001.04.19
    */
    public class Entry
            extends SafeHashMap.Entry
            implements ConfigurableCacheMap.Entry
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct the cacheable entry that holds the cached value.
        */
        public Entry()
            {
            m_dtLastUse = m_dtCreated = getCurrentTimeMillis();
            }

        /**
        * This method is invoked when the containing Map has actually
        * added this Entry to itself.
        */
        protected void onAdd()
            {
            scheduleExpiry();

            // update units
            int      cNewUnits = calculateUnits(m_oValue);
            OldCache map       = OldCache.this;
            synchronized (map)
                {
                int cOldUnits = m_cUnits;
                if (cOldUnits == -1)
                    {
                    // entry is discarded; avoid exception
                    return;
                    }

                if (cNewUnits != cOldUnits)
                    {
                    map.adjustUnits(cNewUnits - cOldUnits);
                    m_cUnits = cNewUnits;
                    }
                }

            // issue add notification
            MapListenerSupport support = map.getMapListenerSupport();
            if (support != null && !support.isEmpty())
                {
                map.dispatchEvent(map.instantiateMapEvent(
                    MapEvent.ENTRY_INSERTED, getKey(), null, getValue()));
                }
            }

        // ----- Map.Entry interface ------------------------------------

        /**
        * Update the cached value.
        *
        * @param oValue  the new value to cache
        *
        * @return the old cache value
        */
        public Object setValue(Object oValue)
            {
            // optimization - verify that the entry is still valid
            if (m_cUnits == -1)
                {
                // entry is discarded; avoid exception
                super.setValue(oValue);
                return null;
                }

            // perform the entry update
            Object oPrev;
            int      cNewUnits = calculateUnits(oValue);
            OldCache map       = OldCache.this;
            synchronized (map)
                {
                int cOldUnits = m_cUnits;
                if (cOldUnits == -1)
                    {
                    // entry is discarded; avoid repetitive events
                    super.setValue(oValue);
                    return null;
                    }

                if (cNewUnits != cOldUnits)
                    {
                    map.adjustUnits(cNewUnits - cOldUnits);
                    m_cUnits = cNewUnits;
                    }

                oPrev = super.setValue(oValue);

                // if previously queued for eviction, interpret the
                // modification as being an indicator that it should not be
                // evicted
                setEvictable(false);
                }

            scheduleExpiry();

            // issue update notification
            if (map.hasListeners())
                {
                map.dispatchEvent(map.instantiateMapEvent(
                    MapEvent.ENTRY_UPDATED, getKey(), oPrev, oValue));
                }

            return oPrev;
            }

        // ----- SafeHashMap.Entry methods ------------------------------

        /**
        * {@inheritDoc}
        */
        protected void copyFrom(SafeHashMap.Entry entry)
            {
            Entry entryThat = (Entry) entry;

            super.copyFrom(entry);

            m_dtCreated = entryThat.m_dtCreated;
            m_dtLastUse = entryThat.m_dtLastUse;
            m_dtExpiry  = entryThat.m_dtExpiry;
            m_cUses     = entryThat.m_cUses;
            m_cUnits    = entryThat.m_cUnits;
            }

        // ----- Cache Entry methods ------------------------------------

        /**
        * Calculate a cache priority.
        *
        * @return a value between 0 and 10, 0 being the highest priority
        */
        public int getPriority()
            {
            // calculate an LRU score - how recently was the entry used?
            long dtPrune   = m_lLastPrune;
            long dtTouch   = m_dtLastUse;
            int  nScoreLRU = 0;
            if (dtTouch > dtPrune)
                {
                // measure recentness against the window of time since the
                // last prune
                long   dtCurrent      = getCurrentTimeMillis();
                long   cMillisDormant = dtCurrent - dtTouch;
                long   cMillisWindow  = dtCurrent - dtPrune;
                double dflPct = (cMillisWindow - cMillisDormant) / (1.0 + cMillisWindow);
                nScoreLRU = 1 + BitHelper.indexOfMSB((int) ((dflPct * dflPct * 64)));
                }

            // calculate "frequency" - how often has the entry been used?
            int  cUses     = m_cUses;
            int  nScoreLFU = 0;
            if (cUses > 0)
                {
                nScoreLFU = 1;
                int cAvg = m_cAvgTouch;
                if (cUses > cAvg)
                    {
                    ++nScoreLFU;
                    }

                int cAdj = (cUses << 1) - cAvg;
                if (cAdj > 0)
                    {
                    nScoreLFU += 1 + Math.min(4,
                        BitHelper.indexOfMSB((int) ((cAdj << 3) / (1.0 + cAvg))));
                    }
                }

            // use comparison to another entry as a bonus score
            Entry entryNext = getNext();
            if (entryNext != null)
                {
                if (dtTouch > entryNext.m_dtLastUse)
                    {
                    ++nScoreLRU;
                    }
                if (cUses > entryNext.m_cUses)
                    {
                    ++nScoreLFU;
                    }
                }

            return Math.max(0, 10 - nScoreLRU - nScoreLFU);
            }

        /**
        * Determine when the cache entry was created.
        *
        * @return the date/time value, in millis, when the entry was created
        */
        public long getCreatedMillis()
            {
            return m_dtCreated;
            }

        /**
        * Called each time the entry is accessed or modified.
        */
        public void touch()
            {
            ++m_cUses;
            m_dtLastUse = getCurrentTimeMillis();

            ConfigurableCacheMap.EvictionPolicy policy = OldCache.this.m_policy;
            if (policy != null)
                {
                policy.entryTouched(this);
                }
            }

        /**
        * Determine when the cache entry was last touched.
        *
        * @return the date/time value, in millis, when the entry was most
        *         recently touched
        */
        public long getLastTouchMillis()
            {
            return m_dtLastUse;
            }

        /**
        * Determine the number of times that the cache entry has been touched.
        *
        * @return the number of times that the cache entry has been touched
        */
        public int getTouchCount()
            {
            return m_cUses;
            }

        /**
        * Reset the number of times that the cache entry has been touched.
        * The touch count does not get reset to zero, but rather to a
        * fraction of its former self; this prevents long lived items from
        * gaining an unassailable advantage in the eviction process.
        *
        * @since Coherence 3.5
        */
        protected void resetTouchCount()
            {
            int cUses = m_cUses;
            if (cUses > 0)
                {
                m_cUses = Math.max(1, cUses >>> 4);
                }
            }

        /**
        * Determine when the cache entry will expire, if ever.
        *
        * @return the date/time value, in millis, when the entry will (or
        *         did) expire; zero indicates no expiry
        */
        public long getExpiryMillis()
            {
            return m_dtExpiry;
            }

        /**
        * Specify when the cache entry will expire, or disable expiry. Note
        * that if the cache is configured for automatic expiry, each
        * subsequent update to this cache entry will reschedule the expiry
        * time.
        *
        * @param lMillis  pass the date/time value, in millis, for when the
        *        entry will expire, or pass zero to disable automatic expiry
        */
        public void setExpiryMillis(long lMillis)
            {
            if (lMillis != 0L || m_dtExpiry != 0L)
                {
                registerExpiry(lMillis);
                m_dtExpiry = lMillis;
                }
            }

        /**
        * Register (or unregister or replace the registration of) this entry for
        * expiry.
        *
        * @param lMillis  the date/time value for when the entry will expire;
        *                 0 is passed to indicate that the entry needs to be
        *                 removed from the items queued for expiry
        */
        protected synchronized void registerExpiry(long lMillis)
            {
            LongArray arrayExpiry = m_arrayExpiry;
            boolean   fWasEmpty   = arrayExpiry.isEmpty();

            // dequeue previous expiry
            long lMillisOld = m_dtExpiry;
            if (lMillisOld > 0L)
                {
                // resolution is 1/4 second (to more efficiently lump
                // keys into sets)
                lMillisOld &= ~0xFFL;
                Set setKeys = (Set) arrayExpiry.get(lMillisOld);
                if (setKeys != null)
                    {
                    setKeys.remove(getKey());
                    if (setKeys.isEmpty())
                        {
                        arrayExpiry.remove(lMillisOld);
                        }
                    }
                }

            // enqueue new expiry
            if (lMillis > 0L)
                {
                lMillis &= ~0xFFL;
                Set setKeys = (Set) arrayExpiry.get(lMillis);
                if (setKeys == null)
                    {
                    setKeys = new LiteSet();
                    arrayExpiry.set(lMillis, setKeys);
                    }
                setKeys.add(getKey());

                // the "next flush" is set to "never" (max long) to avoid
                // any attempts to flush; now that something is scheduled
                // to expire, make sure that flushes are enabled
                if (fWasEmpty && m_lNextFlush == Long.MAX_VALUE)
                    {
                    m_lNextFlush = 0L;
                    }
                }
            }

        /**
        * Determine if the cache entry has expired.
        *
        * @return true if the cache entry was subject to automatic expiry and
        *         the current time is greater than the entry's expiry time
        */
        public boolean isExpired()
            {
            long dtExpiry = m_dtExpiry;
            return dtExpiry != 0 && dtExpiry < getCurrentTimeMillis();
            }

        /**
        * Reschedule the cache entry expiration.
        */
        protected void scheduleExpiry()
            {
            long dtExpiry = 0L;
            int  cDelay   = OldCache.this.m_cExpiryDelay;
            if (cDelay > 0)
                {
                dtExpiry = getCurrentTimeMillis() + cDelay;
                }
            setExpiryMillis(dtExpiry);
            }

        /**
        * Called to inform the Entry that it is no longer used.
        */
        protected void discard()
            {
            if (!isDiscarded())
                {
                if (m_dtExpiry > 0L)
                    {
                    // remove this entry from the expiry queue
                    registerExpiry(0L);
                    }

                OldCache map = OldCache.this;
                synchronized (map)
                    {
                    int cUnits = m_cUnits;
                    if (cUnits == -1)
                        {
                        // entry is discarded; avoid repetitive events
                        return;
                        }

                    if (cUnits > 0)
                        {
                        map.adjustUnits(-cUnits);
                        }

                    m_cUnits = -1;
                    }

                // issue remove notification
                if (map.hasListeners())
                    {
                    map.dispatchEvent(map.instantiateMapEvent(
                        MapEvent.ENTRY_DELETED, getKey(), getValue(), null));
                    }
                }
            }

        /**
        * Determine if this entry has already been discarded from the cache.
        *
        * @return true if this entry has been discarded
        */
        protected boolean isDiscarded()
            {
            return m_cUnits == -1;
            }

        /**
        * Calculate a cache cost for the specified object.
        * <p>
        * The default implementation uses the unit calculator type of the
        * containing cache.
        *
        * @param oValue  the cache value to evaluate for unit cost
        *
        * @return an integer value 0 or greater, with a larger value
        *         signifying a higher cost
        */
        protected int calculateUnits(Object oValue)
            {
            OldCache map  = OldCache.this;
            Object   oKey = getKey();
            switch (map.getUnitCalculatorType())
                {
                case UNIT_CALCULATOR_BINARY:
                    return BinaryMemoryCalculator.INSTANCE.calculateUnits(oKey, oValue);

                case UNIT_CALCULATOR_EXTERNAL:
                    return map.m_calculator.calculateUnits(oKey, oValue);

                case UNIT_CALCULATOR_FIXED:
                default:
                    return 1;
                }
            }

        /**
        * Determine the number of cache units used by this Entry.
        *
        * @return an integer value 0 or greater, with a larger value
        *         signifying a higher cost; -1 implies that the Entry
        *         has been discarded
        */
        public int getUnits()
            {
            return m_cUnits;
            }

        /**
        * Specify the number of cache units used by this Entry.
        *
        * @param cUnits  an integer value 0 or greater, with a larger value
        *                signifying a higher cost
        */
        public void setUnits(int cUnits)
            {
            azzert(cUnits >= 0);

            synchronized (OldCache.this)
                {
                int cOldUnits = m_cUnits;
                if (cOldUnits == -1)
                    {
                    // entry is discarded; avoid exception
                    return;
                    }

                if (cUnits != cOldUnits)
                    {
                    OldCache.this.adjustUnits(cUnits - cOldUnits);
                    m_cUnits = cUnits;
                    }
                }
            }

        /**
        * Determine if this entry has been marked as being evictable.
        *
        * @return true if this entry is evictable
        *
        * @since Coherence 3.5
        */
        protected boolean isEvictable()
            {
            return m_fEvictable;
            }

        /**
        * Specify that this entry is evictable or not.
        *
        * @param fEvict  true to specify that this entry is evictable, such
        *                as when it is selected for deferred eviction, and
        *                false to specify that it is no longer evictable
        *
        * @since Coherence 3.5
        */
        protected void setEvictable(boolean fEvict)
            {
            m_fEvictable = fEvict;
            }


        // ----- Object methods -----------------------------------------

        /**
        * Render the cache entry as a String.
        *
        * @return the details about this Entry
        */
        public String toString()
            {
            long dtExpiry = getExpiryMillis();

            return super.toString()
                + ", priority=" + getPriority()
                + ", created=" + new Time(getCreatedMillis())
                + ", last-use=" + new Time(getLastTouchMillis())
                + ", expiry=" + (dtExpiry == 0 ? "none"
                    : new Time(dtExpiry) + (isExpired() ? " (expired)" : ""))
                + ", use-count=" + getTouchCount()
                + ", units=" + getUnits();
            }

        // ----- internal -----------------------------------------------

        /**
        * Package Private: Obtain the next cache entry in the chain of
        * cache entries for a given hash bucket.
        *
        * @return the next cache entry in the hash bucket
        */
        Entry getNext()
            {
            return (Entry) m_eNext;
            }

        /**
        * Package Private: Specify the next cache entry in the chain of
        * cache entries for a given hash bucket.
        *
        * @param entry  the next cache entry
        */
        void setNext(Entry entry)
            {
            m_eNext = entry;
            }

        // ----- data members -------------------------------------------

        /**
        * The time at which this Entry was created.
        */
        private volatile long   m_dtCreated;

        /**
        * The time at which this Entry was last accessed.
        */
        private volatile long   m_dtLastUse;

        /**
        * The time at which this Entry will (or did) expire.
        */
        private volatile long   m_dtExpiry;

        /**
        * The number of times that this Entry has been accessed.
        */
        private int    m_cUses;

        /**
        * The number of units for the Entry.
        */
        private int    m_cUnits;

        /**
        * This specifies whether or not this entry has been selected For
        * deferred eviction.
        */
        private boolean m_fEvictable;
        }


    // ----- interface: EvictionPolicy --------------------------------------

    /**
    * An eviction policy is an object that the cache provides with access
    * information, and when requested, the eviction policy selects and
    * evicts entries from the cache. If the eviction policy needs to be
    * aware of changes to the cache, it must implement the MapListener
    * interface; if it does, it will automatically be registered to receive
    * MapEvents.
    */
    public interface EvictionPolicy
            extends ConfigurableCacheMap.EvictionPolicy
        {
        }


    // ----- inner class: InternalEvictionPolicy ----------------------------

    /**
    * The InternalEvictionPolicy represents a pluggable eviction policy for
    * the non-pluggable built-in (internal) eviction policies supported by
    * this cache implementation.
    */
    public static class InternalEvictionPolicy
            implements EvictionPolicy
        {
        /**
        * Constructor.
        *
        * @param nType  the internal eviction type as defined by the
        *               EVICTION_POLICY_* constants
        */
        InternalEvictionPolicy(int nType)
            {
            m_nType = nType;
            }

        /**
        * {@inheritDoc}
        */
        public void entryTouched(ConfigurableCacheMap.Entry entry)
            {
            throw new UnsupportedOperationException();
            }

        /**
        * {@inheritDoc}
        */
        public void requestEviction(int cMaximum)
            {
            throw new UnsupportedOperationException();
            }

        /**
        * {@inheritDoc}
        */
        public String getName()
            {
            switch (m_nType)
                {
                case EVICTION_POLICY_HYBRID:
                    return "Internal-Hybrid";

                case EVICTION_POLICY_LRU:
                    return "Internal-LRU";

                case EVICTION_POLICY_LFU:
                    return "Internal-LFU";

                default:
                    throw new IllegalStateException();
                }
            }

        /**
        * Determine the OldCache eviction type represented by this
        * InternalEvictionPolicy.
        *
        * @return one of the EVICTION_POLICY_* constants
        */
        public int getEvictionType()
            {
            return m_nType;
            }

        /**
        * The OldCache eviction type represented by this
        * InternalEvictionPolicy; one of the EVICTION_POLICY_* constants.
        */
        private int m_nType;
        }


    // ----- interface: UnitCalculator --------------------------------------

    /**
    * A unit calculator is an object that can calculate the cost of caching
    * an object.
    */
    public interface UnitCalculator
            extends ConfigurableCacheMap.UnitCalculator
        {
        }


    // ----- inner class: InternalUnitCalculator ----------------------------

    /**
    * The InternalUnitCalculator represents a pluggable UnitCalculator for
    * the non-pluggable built-in (internal) UnitCalculator implementation
    * provided by this cache implementation.
    */
    public static class InternalUnitCalculator
            implements UnitCalculator
        {
        /**
        * Default constructor.
        */
        private InternalUnitCalculator()
            {
            }

        /**
        * {@inheritDoc}
        */
        public int calculateUnits(Object oKey, Object oValue)
            {
            return 1;
            }

        /**
        * {@inheritDoc}
        */
        public String getName()
            {
            return "Internal-Fixed";
            }

        /**
        * Singleton instance.
        */
        public static final InternalUnitCalculator INSTANCE = new InternalUnitCalculator();
        }


    // ----- constants ------------------------------------------------------

    /**
    * By default, the cache size (in units).
    */
    public static final int    DEFAULT_UNITS            = 1000;

    /**
    * By default, the cache entries expire after one hour.
    */
    public static final int    DEFAULT_EXPIRE           = 3600000;

    /**
    * By default, expired cache entries are flushed on a minute interval.
    *
    * @deprecated as of Coherence 3.5
    */
    public static final int    DEFAULT_FLUSH            = 60000;

    /**
    * By default, when the cache prunes, it reduces its entries to this
    * percentage.
    */
    public static final double DEFAULT_PRUNE            = 0.80;

    /**
    * By default, the cache prunes based on a hybrid LRU+LFU algorithm.
    */
    public static final int    EVICTION_POLICY_HYBRID   = 0;

    /**
    * The cache can prune based on a pure Least Recently Used (LRU)
    * algorithm.
    */
    public static final int    EVICTION_POLICY_LRU      = 1;

    /**
    * The cache can prune based on a pure Least Frequently Used (LFU)
    * algorithm.
    */
    public static final int    EVICTION_POLICY_LFU      = 2;

    /**
    * The cache can prune using an external eviction policy.
    */
    public static final int    EVICTION_POLICY_EXTERNAL = 3;

    /**
    * Specifies the default unit calculator that weighs all entries equally
    * as 1.
    */
    public static final int    UNIT_CALCULATOR_FIXED    = 0;

    /**
    * Specifies a unit calculator that assigns an object a weight equal to
    * the number of bytes of memory required to cache the object.
    *
    * @see BinaryMemoryCalculator
    */
    public static final int    UNIT_CALCULATOR_BINARY   = 1;

    /**
    * Specifies a external (custom) unit calculator implementation.
    */
    public static final int    UNIT_CALCULATOR_EXTERNAL = 2;


    // ----- "pluggable" eviction policies and unit calculators -------------

    /**
    * The EvictionPolicy object for the Hybrid eviction algorithm.
    */
    public static final EvictionPolicy INSTANCE_HYBRID = new InternalEvictionPolicy(EVICTION_POLICY_HYBRID);

    /**
    * The EvictionPolicy object for the Least Recently Used (LRU) eviction
    * algorithm.
    */
    public static final EvictionPolicy INSTANCE_LRU    = new InternalEvictionPolicy(EVICTION_POLICY_LRU);

    /**
    * The EvictionPolicy object for the Least Frequently Used (LFU) eviction
    * algorithm.
    */
    public static final EvictionPolicy INSTANCE_LFU    = new InternalEvictionPolicy(EVICTION_POLICY_LFU);

    /**
    * The UnitCalculator object that counts each entry as one unit.
    */
    public static final UnitCalculator INSTANCE_FIXED  = InternalUnitCalculator.INSTANCE;

    /**
    * The UnitCalculator object that measures the bytes used by entries. This
    * is intended for caches that manage binary data.
    */
    public static final UnitCalculator INSTANCE_BINARY = BinaryMemoryCalculator.INSTANCE;


    // ----- data members ---------------------------------------------------

    /**
    * The current number of units in the cache. A unit is an undefined means
    * of measuring cached values, and must be 0 or positive. The particular
    * Entry implementation being used defines the meaning of unit.
    */
    protected volatile long m_cCurUnits;

    /**
    * The number of units to allow the cache to grow to before pruning.
    */
    protected long m_cMaxUnits;

    /**
    * The percentage of the total number of units that will remain after the
    * cache manager prunes the cache (i.e. this is the "low water mark"
    * value); this value is in the range 0.0 to 1.0.
    */
    protected double m_dflPruneLevel;

    /**
    * The number of units to prune the cache down to.
    */
    protected long m_cPruneUnits;

    /**
    * The unit factor.
    */
    protected int m_nUnitFactor = 1;

    /**
    * The number of milliseconds that a value will live in the cache.
    * Zero indicates no timeout.
    */
    protected int m_cExpiryDelay;

    /**
    * The time before which a expired-entries flush will not be performed.
    */
    protected volatile long m_lNextFlush = Long.MAX_VALUE;

    /**
    * The CacheStatistics object maintained by this cache.
    */
    protected SimpleCacheStatistics m_stats = new SimpleCacheStatistics();

    /**
    * The MapListenerSupport object.
    */
    protected MapListenerSupport m_listenerSupport;

    /**
     * A lock for access to {@link #m_listenerSupport}.
     */
    private final Lock f_listenerLock = new ReentrantLock();

    /**
    * The type of eviction policy employed by the cache; one of the
    * EVICTION_POLICY_* enumerated values.
    */
    protected int m_nEvictionType = EVICTION_POLICY_HYBRID;

    /**
    * The eviction policy; for eviction type EVICTION_POLICY_EXTERNAL.
    */
    protected ConfigurableCacheMap.EvictionPolicy m_policy;

    /**
    * The type of unit calculator employed by the cache; one of the
    * UNIT_CALCULATOR_* enumerated values.
    */
    protected int m_nCalculatorType;

    /**
    * The external unit calculator.
    */
    protected ConfigurableCacheMap.UnitCalculator m_calculator;

    /**
    * Array of set of keys, indexed by the time of expiry.
    * @since Coherence 3.5
    */
    protected LongArray m_arrayExpiry = new SparseArray();

    /**
    * The last time that a prune was run. This value is used by the hybrid
    * eviction policy.
    * @since Coherence 3.5
    */
    protected long m_lLastPrune = getCurrentTimeMillis();

    /**
    * For a prune cycle, this value is the average number of touches that an
    * entry should have. This value is used by the hybrid eviction policy.
    * @since Coherence 3.5
    */
    protected int m_cAvgTouch;

    /**
    * For deferred eviction, iterator of entries to evict. If null, then
    * there are no entries with deferred eviction.
    * @since Coherence 3.5
    */
    protected ListIterator m_iterEvict;

    /**
    * Specifies whether or not this cache will incrementally evict.
    */
    protected boolean m_fIncrementalEvict = true;

    /**
    * The EvictionApprover.
    */
    protected volatile ConfigurableCacheMap.EvictionApprover m_apprvrEvict;

    /**
    * Specifies whether or not this cache is used in the environment,
    * where the {@link Base#getSafeTimeMillis()} is used very frequently and
    * as a result, the {@link Base#getLastSafeTimeMillis} could be used
    * without sacrificing the clock precision. By default, the optimization
    * is off.
    */
    protected  boolean m_fOptimizeGetTime;

    /**
    * Lock for limiting concurrent eviction, allows for relaxing contention
    * when eviction is done opportunistically.
    */
    private final AtomicBoolean f_evictLock = new AtomicBoolean(true);
    }
