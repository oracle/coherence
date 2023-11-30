/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.FilterEnumerator;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.concurrent.atomic.AtomicReferenceArray;

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
* The cache can optionally be flushed on a periodic basis by setting
* the FlushDelay property or scheduling a specific flush time by setting
* the FlushTime property.
* <p>
* Cache hit statistics can be obtained from the CacheHits, CacheMisses,
* HitProbability, KeyHitProbability and CompositeHitProbability read-only
* properties. The statistics can be reset by invoking resetHitStatistics.
* The statistics are automatically reset when the cache is cleared (the clear
* method).
* <p>
* The OldOldCache implements the ObservableMap interface, meaning it provides
* event notifications to any interested listener for each insert, update and
* delete, including those that occur when the cache is pruned or entries
* are automatically expired.
* <p>
* This implementation is designed to support extension through inheritence.
* When overriding the inner Entry class, the OldOldCache.instantiateEntry factory
* method must be overridden to instantiate the correct Entry sub-class. To
* override the one-unit-per-entry default behavior, extend the inner Entry
* class and override the calculateUnits method.
*
* @author cp  2001.04.19
* @author cp  2005.05.18  moved to com.tangosol.net.cache package
*
* @deprecated As of release 3.1, replaced by {@link LocalCache}
*/
public class OldOldCache
        extends SafeHashMap
        implements ObservableMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the cache manager.
    */
    public OldOldCache()
        {
        this(DEFAULT_UNITS);
        }

    /**
    * Construct the cache manager.
    *
    * @param cUnits  the number of units that the cache manager will cache
    *                before pruning the cache
    */
    public OldOldCache(int cUnits)
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
    public OldOldCache(int cUnits, int cExpiryMillis)
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
    public OldOldCache(int cUnits, int cExpiryMillis, double dflPruneLevel)
        {
        m_cMaxUnits     = cUnits;
        m_dflPruneLevel = dflPruneLevel = Math.min(Math.max(dflPruneLevel, 0.0), 0.99);
        m_cPruneUnits   = (int) (dflPruneLevel * cUnits);
        m_cExpiryDelay  = Math.max(cExpiryMillis, 0);
        scheduleFlush();
        }


    // ----- Map interface --------------------------------------------------

    /**
    * Returns the number of key-value mappings in this map.
    *
    * @return the number of key-value mappings in this map
    */
    public int size()
        {
        // check if the cache needs flushing
        checkFlush();

        return super.size();
        }

    /**
    * Returns <tt>true</tt> if the cache contains the specified key.
    *
    * @param key key whose presence in this map is to be tested
    *
    * @return <tt>true</tt> if this map contains a mapping for the specified
    *         key
    */
    public boolean containsKey(Object key)
        {
        // check if the cache needs flushing
        checkFlush();

        return getEntryInternal(key) != null;
        }

    /**
    * If the specified item is in the cache, return it.
    *
    * @param oKey  the key to the desired cached item
    *
    * @return the desired Object if it is in the cache, otherwise null
    */
    public Object get(Object oKey)
        {
        Map.Entry entry = getEntry(oKey);
        return (entry == null ? null : entry.getValue());
        }

    /**
    * Locate an Entry in the hash map based on its key.
    *
    * @param oKey  the key object to search for
    *
    * @return the Entry or null
    */
    public SafeHashMap.Entry getEntry(Object oKey)
        {
        // check if the cache needs flushing
        checkFlush();

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
    * Store a value in the cache.
    *
    * @param oKey    the key with which to associate the cache value
    * @param oValue  the value to cache
    *
    * @return the value that was cached associated with that key, or null if
    *         no value was cached associated with that key
    */
    public Object put(Object oKey, Object oValue)
        {
        return put(oKey, oValue, 0L);
        }

    /**
    * Store a value in the cache that will be automatically evicted according
    * to the specified expiration value.
    *
    * @param oKey     the key with which to associate the cache value
    * @param oValue   the value to cache
    * @param cMillis  the number of milliseconds until the entry will expire;
    *                 pass zero to use the cache's default ExpiryDelay settings;
    *                 pass -1 to indicate that the entry should never expire
    *
    * @return the value that was cached associated with that key, or null if
    *         no value was cached associated with that key
    *
    * @since Coherence 2.3
    */
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        // check if the cache needs flushing
        checkFlush();

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
                        Base.getSafeTimeMillis() + cMillis : 0L);
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
    * Remove an entry from the cache.
    *
    * @param oKey  the key of a cached value
    *
    * @return the value that was cached associated with that key, or null if
    *         no value was cached associated with that key
    */
    public Object remove(Object oKey)
        {
        // check if the cache needs flushing
        checkFlush();

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
    * Remove everything from the cache.
    */
    public void clear()
        {
        // push next flush out to avoid attempts at multiple simultaneous
        // flushes
        deferFlush();

        synchronized (this)
            {
            while (true)
                {
                try
                    {
                    // notify cache entries of their impending removal
                    for (Iterator iter = entrySet().iterator(); iter.hasNext(); )
                        {
                        ((Entry) iter.next()).discard();
                        }

                    // verify that the cache maintains its data correctly
                   if (m_cCurUnits != 0)
                        {
                        // soft assertion
                        Base.out("Invalid unit count after clear: " + m_cCurUnits);
                        m_cCurUnits = 0;
                        }
                    break;
                    }
                catch (ConcurrentModificationException e)
                    {
                    }
                }

            // reset the cache storage
            super.clear();

            // reset hit/miss stats
            resetHitStatistics();

            // schedule next flush
            scheduleFlush();
            }
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
    public synchronized void addMapListener(MapListener listener, Object oKey, boolean fLite)
        {
        Base.azzert(listener != null);

        MapListenerSupport support = m_listenerSupport;
        if (support == null)
            {
            support = m_listenerSupport = new MapListenerSupport();
            }

        support.addListener(listener, oKey, fLite);
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void removeMapListener(MapListener listener, Object oKey)
        {
        Base.azzert(listener != null);

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

    /**
    * {@inheritDoc}
    */
    public synchronized void addMapListener(MapListener listener, Filter filter, boolean fLite)
        {
        Base.azzert(listener != null);

        MapListenerSupport support = m_listenerSupport;
        if (support == null)
            {
            support = m_listenerSupport = new MapListenerSupport();
            }

        support.addListener(listener, filter, fLite);
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void removeMapListener(MapListener listener, Filter filter)
        {
        Base.azzert(listener != null);

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


    // ----- JCache methods -------------------------------------------------

    /**
    * Evict a specified key from the cache, as if it had expired from the
    * cache. If the key is not in the cache, then the method has no effect.
    *
    * @param oKey  the key to evict from the cache
    */
    public void evict(Object oKey)
        {
        Entry entry = (Entry) getEntryInternal(oKey);
        if (entry != null)
            {
            removeExpired(entry, true);
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
        for (Iterator iter = colKeys.iterator(); iter.hasNext(); )
            {
            Object oKey = iter.next();
            Entry entry = (Entry) getEntryInternal(oKey);
            if (entry != null)
                {
                removeExpired(entry, true);
                }
            }
        }

    /**
    * Evict all entries from the cache that are no longer valid, and
    * potentially prune the cache size if the cache is size-limited
    * and its size is above the caching low water mark.
    */
    public synchronized void evict()
        {
        // push next flush out to avoid attempts at multiple simultaneous
        // flushes
        deferFlush();

        // walk all buckets
        AtomicReferenceArray aeBucket = m_aeBucket;
        int                  cBuckets = aeBucket.length();
        for (int iBucket = 0; iBucket < cBuckets; ++iBucket)
            {
            // walk all entries in the bucket
            Entry entry = (Entry) aeBucket.get(iBucket);
            while (entry != null)
                {
                if (entry.isExpired())
                    {
                    removeExpired(entry, true);
                    }
                entry = entry.getNext();
                }
            }

        // schedule next flush
        scheduleFlush();
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
            if (OldOldCache.this.isEmpty())
                {
                return NullImplementation.getIterator();
                }

            // complete entry set iterator
            Iterator iter = instantiateIterator();

            // filter to get rid of expired objects
            Filter filter = new Filter()
                {
                public boolean evaluate(Object o)
                    {
                    Entry   entry    = (Entry) o;
                    boolean fExpired = entry.isExpired();
                    if (fExpired)
                        {
                        OldOldCache.this.removeExpired(entry, true);
                        }
                    return !fExpired;
                    }
                };

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
                else if (entry.isExpired())
                    {
                    removeExpired(entry, true);
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
            OldOldCache map = OldOldCache.this;
            synchronized (map)
                {
                // create the array to store the map keys
                int c = map.size();
                aoAll = new Object[c];
                if (c > 0)
                    {
                    // walk all buckets
                    AtomicReferenceArray aeBucket = map.m_aeBucket;
                    int                  cBuckets = aeBucket.length();
                    for (int iBucket = 0; iBucket < cBuckets; ++iBucket)
                        {
                        // walk all entries in the bucket
                        Entry entry = (Entry) aeBucket.get(iBucket);
                        while (entry != null)
                            {
                            if (entry.isExpired())
                                {
                                removeExpired(entry, true);
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
            OldOldCache map = OldOldCache.this;
            synchronized (map)
                {
                // create the array to store the map values
                int c = map.size();
                aoAll = new Object[c];
                if (c > 0)
                    {
                    // walk all buckets
                    AtomicReferenceArray aeBucket = map.m_aeBucket;
                    int                  cBuckets = aeBucket.length();
                    for (int iBucket = 0; iBucket < cBuckets; ++iBucket)
                        {
                        // walk all entries in the bucket
                        Entry entry = (Entry) aeBucket.get(iBucket);
                        while (entry != null)
                            {
                            if (entry.isExpired())
                                {
                                removeExpired(entry, true);
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
                StringBuffer sb = new StringBuffer("Cache {\n");
                int i = 0;
                for (Iterator iter = entrySet().iterator(); iter.hasNext(); )
                    {
                    sb.append('[')
                      .append(i++)
                      .append("]: ")
                      .append(iter.next())
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
    * Determine the number of units that the cache currently stores.
    *
    * @return the current size of the cache in units
    */
    public int getUnits()
        {
        return m_cCurUnits;
        }

    /**
    * Determine the limit of the cache size in units. The cache will prune
    * itself automatically once it reaches its maximum unit level. This is
    * often referred to as the "high water mark" of the cache.
    *
    * @return the limit of the cache size in units
    */
    public int getHighUnits()
        {
        return m_cMaxUnits;
        }

    /**
    * Update the maximum size of the cache in units. This is often referred
    * to as the "high water mark" of the cache.
    *
    * @param cMax  the new maximum size of the cache, in units
    */
    public synchronized void setHighUnits(int cMax)
        {
        if (cMax < 0)
            {
            throw new IllegalArgumentException("high units out of bounds");
            }

        boolean fShrink = cMax < m_cMaxUnits;

        m_cMaxUnits = cMax;

        // ensure that low units are in range
        setLowUnits((int) (m_dflPruneLevel * cMax));

        if (fShrink)
            {
            checkSize();
            }
        }

    /**
    * Determine the point to which the cache will shrink when it prunes.
    * This is often referred to as a "low water mark" of the cache.
    *
    * @return the number of units that the cache prunes to
    */
    public int getLowUnits()
        {
        return m_cPruneUnits;
        }

    /**
    * Specify the point to which the cache will shrink when it prunes.
    * This is often referred to as a "low water mark" of the cache.
    *
    * @param cUnits  the number of units that the cache prunes to
    */
    public synchronized void setLowUnits(int cUnits)
        {
        if (cUnits < 0)
            {
            throw new IllegalArgumentException("low units out of bounds");
            }

        if (cUnits >= m_cMaxUnits)
            {
            cUnits = (int) (m_dflPruneLevel * m_cMaxUnits);
            }

        m_cPruneUnits = cUnits;
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
    * Determine the current external eviction policy, if any.
    *
    * @return the external eviction policy, if one has been provided
    */
    public EvictionPolicy getEvictionPolicy()
        {
        return m_policy;
        }

    /**
    * Set the external eviction policy, and change the eviction type to
    * EVICTION_POLICY_EXTERNAL. If null is passed, clear the external
    * eviction policy, and use the default internal policy.
    *
    * @param policy  an external eviction policy, or null to use the default
    *                policy
    */
    public synchronized void setEvictionPolicy(EvictionPolicy policy)
        {
        int nType = (policy == null ? EVICTION_POLICY_HYBRID
                                    : EVICTION_POLICY_EXTERNAL);
        configureEviction(nType, policy);
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
    * Determine the current external unit calculator, if any.
    *
    * @return the external unit calculator, if one has been provided
    */
    public UnitCalculator getUnitCalculator()
        {
        return m_calculator;
        }

    /**
    * Set the external unit calculator, and change the unit calculator type to
    * UNIT_CALCULATOR_EXTERNAL. If null is passed, clear the external
    * unit calculator, and use the default unit calculator.
    *
    * @param calculator  an external unit calculator, or null to use the default
    *                    unit calculator
    */
    public void setUnitCalculator(UnitCalculator calculator)
        {
        int nType = (calculator == null ? UNIT_CALCULATOR_FIXED
                                        : UNIT_CALCULATOR_EXTERNAL);
        configureUnitCalculator(nType, calculator);
        }

    /**
    * Determine the "time to live" for each individual cache entry.
    *
    * @return the number of milliseconds that a cache entry value will live,
    *         or zero if cache entries are never automatically expired
    */
    public int getExpiryDelay()
        {
        return m_cExpiryDelay;
        }

    /**
    * Specify the "time to live" for cache entries. This does not affect
    * the already-scheduled expiry of existing entries.
    *
    * @param cMillis  the number of milliseconds that cache entries will
    *                 live, or zero to disable automatic expiry
    */
    public void setExpiryDelay(int cMillis)
        {
        m_cExpiryDelay = Math.max(cMillis, 0);
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
    */
    public long getFlushTime()
        {
        return m_lNextFlush;
        }

    /**
    * Specify the date/time at which the next cache flush is to occur.
    * Note that the date/time may be Long.MAX_VALUE, which implies that a
    * flush will never occur. A time in the past or at the present will
    * cause an immediate flush.
    *
    * @param lMillis  the date/time value, in milliseconds, when the cache
    *                 should next automatically flush
    */
    public void setFlushTime(long lMillis)
        {
        m_lNextFlush = lMillis;
        checkFlush();
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
    * Determine the rough probability (0 <= p <= 1) that any particular
    * {@link #get} invocation will be satisfied by an existing entry in
    * the cache, based on the statistics collected since the last reset
    * of the cache statistics.
    *
    * @return the cache hit probability (0 <= p <= 1)
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
    protected synchronized void configureEviction(int nType, EvictionPolicy policy)
        {
        switch (nType)
            {
            case EVICTION_POLICY_HYBRID:
            case EVICTION_POLICY_LRU:
            case EVICTION_POLICY_LFU:
                break;

            case EVICTION_POLICY_EXTERNAL:
                if (policy == null)
                    {
                    throw new IllegalStateException("an attempt was made to"
                        + " set eviction type to EVICTION_POLICY_EXTERNAL"
                        + " without providing an external EvictionPolicy");
                    }
                break;

            default:
                throw new IllegalArgumentException("unknown eviction type: " + nType);
            }

        EvictionPolicy policyPrev = m_policy;
        if (policyPrev instanceof MapListener)
            {
            removeMapListener((MapListener) policyPrev);
            }

        m_nEvictionType = nType;
        m_policy        = policy;

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
    protected synchronized void configureUnitCalculator(int nType, UnitCalculator calculator)
        {
        switch (nType)
            {
            case UNIT_CALCULATOR_FIXED:
            case UNIT_CALCULATOR_BINARY:
                if (nType == m_nCalculatorType)
                    {
                    // nothing to do
                    return;
                    }
                break;

            case UNIT_CALCULATOR_EXTERNAL:
                if (calculator == null)
                    {
                    throw new IllegalStateException("an attempt was made to"
                        + " set the unit calculator type to"
                        + " UNIT_CALCULATOR_EXTERNAL"
                        + " without providing an external UnitCalculator");
                    }
                else if (UNIT_CALCULATOR_EXTERNAL == m_nCalculatorType &&
                        Base.equals(calculator, m_calculator))
                    {
                    // nothing to do
                    return;
                    }
                break;

            default:
                throw new IllegalArgumentException("unknown unit calculator type: "
                        + nType);
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
    * expired, it is removed from the hash map.
    * <p/>
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

        if (entry != null && entry.isExpired())
            {
            removeExpired(entry, true);
            entry = null;
            }

        return entry;
        }

    /**
    * Remove an entry because it has expired.
    *
    * @param entry            the expired cache entry
    * @param fRemoveInternal  true if the cache entry still needs to be
    *                         removed from the cache
    */
    protected synchronized void removeExpired(Entry entry, boolean fRemoveInternal)
        {
        entry.discard();
        if (fRemoveInternal)
            {
            super.removeEntryInternal(entry);
            }
        }

    /**
    * Adjust current size.
    */
    protected synchronized void adjustUnits(int cDelta)
        {
        m_cCurUnits += cDelta;
        }

    /**
    * Check if the cache is too big, and if it is prune it by discarding the
    * lowest priority cache entries.
    */
    protected void checkSize()
        {
        // check if pruning is required
        if (m_cCurUnits > m_cMaxUnits)
            {
            synchronized (this)
                {
                // recheck so that only one thread prunes
                if (m_cCurUnits > m_cMaxUnits)
                    {
                    prune();
                    }
                }
            }
        }

    /**
    * Prune the cache by discarding the lowest priority cache entries.
    */
    protected synchronized void prune()
        {
        long   ldtStart = Base.getSafeTimeMillis();
        int cCur = getUnits();
        int cMin = getLowUnits();
        if (cCur < cMin)
            {
            return;
            }

        int nType = getEvictionType();
        switch (nType)
            {
            default:
            case EVICTION_POLICY_HYBRID:
                {
                // sum the entries' units per priority
                int[] acUnits;
                while (true)
                    {
                    try
                        {
                        acUnits = new int[11];
                        for (Iterator iter = entrySet().iterator(); iter.hasNext(); )
                            {
                            Entry entry = (Entry) iter.next();
                            int cUnits = entry.getUnits();
                            try
                                {
                                acUnits[entry.getPriority()] += cUnits;
                                }
                            catch (IndexOutOfBoundsException e)
                                {
                                acUnits[Math.max(0, Math.min(entry.getPriority(), 10))] += cUnits;
                                }
                            }
                        break;
                        }
                    catch (ConcurrentModificationException e)
                        {
                        }
                    }

                int cTotal         = 0;
                int nPrunePriority = 0;
                while (nPrunePriority <= 10)
                    {
                    cTotal += acUnits[nPrunePriority];
                    if (cTotal > cMin)
                        {
                        break;
                        }
                    ++nPrunePriority;
                    }

                // build a list of entries to discard
                Entry entryDiscardHead = null;
                Entry entryDiscardTail = null;

                // determine the number at the cut-off priority that must be pruned
                int cAdditional = Math.max(0, cTotal - cMin);

                while (cCur > cMin)
                    {
                    try
                        {
                        for (Iterator iter = entrySet().iterator(); iter.hasNext() && cCur > cMin; )
                            {
                            Entry entry = (Entry) iter.next();
                            int nPriority = entry.getPriority();
                            if (nPriority >= nPrunePriority)
                                {
                                int cUnits = entry.getUnits();
                                if (nPriority == nPrunePriority)
                                    {
                                    if (cAdditional <= 0)
                                        {
                                        continue;
                                        }
                                    cAdditional -= cUnits;
                                    }
                                cCur -= cUnits;

                                // remove the entry from the map
                                super.removeEntryInternal(entry);

                                // link the entry into the list of deferred
                                // removals, but without changing its "next"
                                // reference because the iterator that we are
                                // using here is counting on that "next" ref
                                if (entryDiscardHead == null)
                                    {
                                    entryDiscardHead = entry;
                                    }
                                else
                                    {
                                    entryDiscardTail.setNext(entry);
                                    }
                                entryDiscardTail = entry;
                                }
                            }

                        // seal the end of the linked list of entries to discard
                        if (entryDiscardTail != null)
                            {
                            entryDiscardTail.setNext(null);
                            }
                        break;
                        }
                    catch (ConcurrentModificationException e)
                        {
                        }
                    }

                // process the list of deferred removals
                for (Entry entryDiscard = entryDiscardHead; entryDiscard != null; )
                    {
                    // unlink it altogether
                    Entry entryNext = entryDiscard.getNext();
                    entryDiscard.setNext(null);

                    // discard it
                    removeExpired(entryDiscard, false);

                    entryDiscard = entryNext;
                    }
                }
                break;

            case EVICTION_POLICY_LRU:
            case EVICTION_POLICY_LFU:
                {
                boolean     fLRU  = (nType == EVICTION_POLICY_LRU);
                SparseArray array;
                while (true)
                    {
                    try
                        {
                        array = new SparseArray();
                        for (Iterator iter = entrySet().iterator(); iter.hasNext(); )
                            {
                            Entry  entry  = (Entry) iter.next();
                            long   lOrder = fLRU ? entry.getLastTouchMillis()
                                                 : entry.getTouchCount();
                            Object oPrev  = array.set(lOrder, entry);
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
                                    list.add((Entry) oPrev);
                                    }
                                list.add(entry);
                                array.set(lOrder, list);
                                }
                            }
                        break;
                        }
                    catch (ConcurrentModificationException e)
                        {
                        }
                    }

                for (Iterator iter = array.iterator();
                     getUnits() > cMin && iter.hasNext(); )
                    {
                    Object o = iter.next();
                    if (o instanceof Entry)
                        {
                        Entry entry = (Entry) o;
                        removeExpired(entry, true);
                        }
                    else
                        {
                        List list = (List) o;
                        for (Iterator iterList = list.iterator();
                             getUnits() > cMin && iterList.hasNext(); )
                            {
                            Entry entry = (Entry) iterList.next();
                            removeExpired(entry, true);
                            }
                        }
                    }
                }
                break;

            case EVICTION_POLICY_EXTERNAL:
                getEvictionPolicy().requestEviction(cMin);
                break;
            }
        m_stats.registerCachePrune(ldtStart);
        }

    /**
    * Check if the cache is timed out, and clear if it is.
    */
    protected void checkFlush()
        {
        // check if flushing is required
        if (Base.getSafeTimeMillis() > m_lNextFlush)
            {
            synchronized (this)
                {
                // recheck so that only one thread flushes
                if (Base.getSafeTimeMillis() > m_lNextFlush)
                    {
                    evict();
                    }
                }
            }
        }

    /**
    * Defer the next flush by scheduling it for infinity and beyond.
    */
    protected void deferFlush()
        {
        // push next flush out to avoid attempts at multiple simultaneous
        // flushes
        m_lNextFlush = Long.MAX_VALUE;
        }

    /**
    * Schedule the next flush.
    */
    protected void scheduleFlush()
        {
        // int cDelayMillis = m_cFlushDelay;
        m_lNextFlush = Long.MAX_VALUE ;
        }

    /**
    * Factory pattern: instantiate a new MapEvent corresponding
    * to the specified parameters.
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
    * Determine if the OverflowMap has any listeners at all.
    *
    * @return true iff this OverflowMap has at least one MapListener
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
    * @param evt   a CacheEvent object
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


    // ----- inner class: Entry ---------------------------------------------

    /**
    * Factory method.  This method exists to allow the OldOldCache class to be
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
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct the cacheable entry that holds the cached value.
        */
        public Entry()
            {
            m_dtLastUse = m_dtCreated = getSafeTimeMillis();
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
            OldOldCache map       = OldOldCache.this;
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
            if (map.hasListeners())
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
            OldOldCache map       = OldOldCache.this;
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
            long dt = getSafeTimeMillis();

            // calculate "age" - how old is the statement
            long cMillisAge = dt - m_dtCreated;

            // calculate "recentness" - when was the statement last used
            long cMillisDormant = dt - m_dtLastUse;

            // calculate "frequency" - how often is the statement used
            int    cUses   = m_cUses;
            double dflRate = (cMillisAge == 0 ? 1 : cMillisAge)
                           / (cUses == 0 ? 1 : cUses);

            // combine the measurements into a priority
            double dflRateScore    = Math.log(Math.max(dflRate       , 100.0) / 100.0 );
            double dflDormantScore = Math.log(Math.max(cMillisDormant, 1000L) / 1000.0);
            int    nPriority       = (int) (dflRateScore + dflDormantScore) / 2;

            return Math.max(0, Math.min(10, nPriority));
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
            m_dtLastUse = getSafeTimeMillis();

            EvictionPolicy policy = OldOldCache.this.getEvictionPolicy();
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
            m_dtExpiry = lMillis;
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
            return dtExpiry != 0 && dtExpiry < getSafeTimeMillis();
            }

        /**
        * Reschedule the cache entry expiration.
        */
        protected void scheduleExpiry()
            {
            long dtExpiry = 0L;
            int  cDelay   = OldOldCache.this.m_cExpiryDelay;
            if (cDelay > 0)
                {
                dtExpiry = getSafeTimeMillis() + cDelay;
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
                OldOldCache map = OldOldCache.this;
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
        * <p/>
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
            OldOldCache map  = OldOldCache.this;
            Object   oKey = getKey();
            switch (map.getUnitCalculatorType())
                {
                case UNIT_CALCULATOR_BINARY:
                    return BinaryMemoryCalculator.INSTANCE.calculateUnits(oKey, oValue);

                case UNIT_CALCULATOR_EXTERNAL:
                    return map.getUnitCalculator().calculateUnits(oKey, oValue);

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

            synchronized (OldOldCache.this)
                {
                int cOldUnits = m_cUnits;
                if (cOldUnits == -1)
                    {
                    // entry is discarded; avoid exception
                    return;
                    }

                if (cUnits != cOldUnits)
                    {
                    OldOldCache.this.adjustUnits(cUnits - cOldUnits);
                    m_cUnits = cUnits;
                    }
                }
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
               + ", expiry=" + (dtExpiry == 0 ? "none" : new Time(dtExpiry) + (isExpired() ? " (expired)" : ""))
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
        {
        /**
        * This method is called by the cache to indicate that an entry has
        * been touched.
        *
        * @param entry  the Cache Entry that has been touched
        */
        public void entryTouched(OldOldCache.Entry entry);

        /**
        * This method is called by the cache when the cache requires the
        * eviction policy to evict entries.
        *
        * @param cMaximum  the maximum number of units that should remain
        *                  in the cache when the eviction is complete
        */
        public void requestEviction(int cMaximum);
        }


    // ----- interface: UnitCalculator --------------------------------------

    /**
    * A unit calculator is an object that can calculate the cost of caching
    * an object.
    */
    public interface UnitCalculator
        {
        /**
        * Calculate a cache cost for the specified cache entry key and value.
        *
        * @param oKey    the cache key to evaluate for unit cost
        * @param oValue  the cache value to evaluate for unit cost
        *
        * @return an integer value 0 or greater, with a larger value
        *         signifying a higher cost
        */
        public int calculateUnits(Object oKey, Object oValue);
        }


    // ----- constants ------------------------------------------------------

    /**
    * By default, the cache size (in units).
    */
    public static final int    DEFAULT_UNITS   = 1000;

    /**
    * By default, the cache entries expire after one hour.
    */
    public static final int    DEFAULT_EXPIRE  = 3600000;

    /**
    * By default, expired cache entries are flushed on a minute interval.
    */
    public static final int    DEFAULT_FLUSH   = 60000;

    /**
    * By default, when the cache prunes, it reduces its entries by 25%,
    * meaning it retains 75% (.75) of its entries.
    */
    public static final double DEFAULT_PRUNE   = 0.75;

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
    * Specifies the default unit calculator that weighs all entries equally as 1.
    */
    public static final int    UNIT_CALCULATOR_FIXED    = 0;

    /**
    * Specifies a unit calculator that assigns an object a weight equal to the
    * number of bytes of memory required to cache the object.
    *
    * @see BinaryMemoryCalculator
    */
    public static final int    UNIT_CALCULATOR_BINARY   = 1;

    /**
    * Specifies a external (custom) unit calculator implementation.
    */
    public static final int    UNIT_CALCULATOR_EXTERNAL = 2;


    // ----- data members ---------------------------------------------------

    /**
    * The current number of units in the cache. A unit is an undefined means
    * of measuring cached values, and must be 0 or positive. The particular
    * Entry implementation being used defines the meaning of unit.
    */
    protected int  m_cCurUnits;

    /**
    * The number of units to allow the cache to grow to before pruning.
    */
    protected int  m_cMaxUnits;

    /**
    * The percentage of the total number of units that will remain after the
    * cache manager prunes the cache (i.e. this is the "low water mark"
    * value); this value is in the range 0.0 to 1.0.
    */
    protected double m_dflPruneLevel;

    /**
    * The number of units to prune the cache down to.
    */
    protected int  m_cPruneUnits;

    /**
    * The number of milliseconds that a value will live in the cache.
    * Zero indicates no timeout.
    */
    protected int m_cExpiryDelay;

    /**
    * The time (ie System.currentTimeMillis) at which the next full cache
    * flush should occur.
    */
    protected volatile long m_lNextFlush;

    /**
    * The CacheStatistics object maintained by this cache.
    */
    protected SimpleCacheStatistics m_stats = new SimpleCacheStatistics();

    /**
    * The MapListenerSupport object.
    */
    protected MapListenerSupport m_listenerSupport;

    /**
    * The type of eviction policy employed by the cache; one of the
    * EVICTION_POLICY_* enumerated values.
    */
    protected int m_nEvictionType;

    /**
    * The eviction policy; for eviction type EVICTION_POLICY_EXTERNAL.
    */
    protected EvictionPolicy m_policy;

    /**
    * The type of unit calculator employed by the cache; one of the
    * UNIT_CALCULATOR_* enumerated values.
    */
    protected int m_nCalculatorType;

    /**
    * The external unit calculator.
    */
    protected UnitCalculator m_calculator;
    }
