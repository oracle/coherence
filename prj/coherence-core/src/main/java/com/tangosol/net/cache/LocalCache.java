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
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.LiteSet;
import com.tangosol.util.LongArray;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.SimpleEnumerator;
import com.tangosol.util.SparseArray;

import java.lang.reflect.Array;

import java.sql.Time;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
* A LocalCache implementation that supports the JCache API, CacheLoader and
* CacheStore objects.
*
* @since Coherence 2.2
*
* @author cp  2003.05.30
*/
public class LocalCache
        extends SafeHashMap
        implements ConfigurableCacheMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the cache manager.
    */
    public LocalCache()
        {
        this(DEFAULT_UNITS);
        }

    /**
    * Construct the cache manager.
    *
    * @param cUnits         the number of units that the cache manager will
    *                       cache before pruning the cache
    */
    public LocalCache(int cUnits)
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
    public LocalCache(int cUnits, int cExpiryMillis)
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
    public LocalCache(int cUnits, int cExpiryMillis, double dflPruneLevel)
        {
        m_dflPruneLevel = Math.min(Math.max(dflPruneLevel, 0.0), 0.99);
        setHighUnits(cUnits);

        m_cExpiryDelay  = Math.max(cExpiryMillis, 0);
        }

    /**
    * Construct the cache manager.
    *
    * @param cUnits         the number of units that the cache manager will
    *                       cache before pruning the cache
    * @param cExpiryMillis  the number of milliseconds that each cache entry
    *                       lives before being automatically expired
    * @param loader         the CacheLoader or CacheStore to use
    */
    public LocalCache(int cUnits, int cExpiryMillis, CacheLoader loader)
        {
        this(cUnits, cExpiryMillis);

        setCacheLoader(loader);
        }

    // ----- Map interface --------------------------------------------------

    @Override
    public int size()
        {
        // check if the cache needs flushing
        evict();

        return super.size();
        }

    @Override
    public boolean isEmpty()
        {
        // this will call evict()
        return size() == 0;
        }

    @Override
    public boolean containsKey(Object key)
        {
        // check if the cache needs flushing
        tryEvict();

        return getEntryInternal(key) != null;
        }

    @Override
    public Object get(Object oKey)
        {
        Map.Entry entry = getEntry(oKey);
        return (entry == null ? null : entry.getValue());
        }

    @Override
    public ConfigurableCacheMap.Entry getCacheEntry(Object oKey)
        {
        return (ConfigurableCacheMap.Entry) getEntry(oKey);
        }

    @Override
    public Object put(Object oKey, Object oValue)
        {
        return put(oKey, oValue, 0L);
        }

    @Override
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        // check if the cache needs flushing
        tryEvict();

        LocalCache.Entry entry;
        Object oOrig;

        synchronized (this)
            {
            entry = (LocalCache.Entry) getEntryInternal(oKey);
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
                    entry = (LocalCache.Entry) getEntryInternal(oKey);
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

    @Override
    public synchronized void clear()
        {
        // this method is only called as a result of a call from the cache
        // consumer, not from any internal eviction etc.

        // if there is a CacheStore, tell it that all entries are being erased
        CacheStore store = getCacheStore();
        if (store != null)
            {
            store.eraseAll(Collections.unmodifiableCollection(keySet()));
            }

        while (true)
            {
            try
                {
                // notify cache entries of their impending removal
                for (LocalCache.Entry entry : (Set<LocalCache.Entry>) entrySet())
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
    public synchronized Object remove(Object oKey)
        {
        // this method is only called as a result of a call from the cache
        // consumer, not from any internal eviction etc.

        // check for the specified entry; getEntryInternal() will only return an
        // entry if the entry exists and has not expired
        LocalCache.Entry entry = (LocalCache.Entry) getEntryInternal(oKey);
        if (entry == null)
            {
            return null;
            }
        else
            {
            // if there is a CacheStore, tell it that the entry is being
            // erased
            CacheStore store = getCacheStore();
            if (store != null)
                {
                store.erase(oKey);
                }

            // check if the cache needs flushing
            tryEvict();

            entry.discard();
            removeEntryInternal(entry);
            return entry.getValue();
            }
        }

    // ----- ObservableMap methods ------------------------------------------

    @Override
    public synchronized void addMapListener(MapListener listener)
        {
        addMapListener(listener, (Filter) null, false);
        }

    @Override
    public synchronized void removeMapListener(MapListener listener)
        {
        removeMapListener(listener, (Filter) null);
        }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public int getUnits()
        {
        return toExternalUnits(m_cCurUnits, getUnitFactor());
        }

    @Override
    public int getHighUnits()
        {
        return toExternalUnits(m_cMaxUnits, getUnitFactor());
        }

    @Override
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

    @Override
    public int getLowUnits()
        {
        return toExternalUnits(m_cPruneUnits, getUnitFactor());
        }

    @Override
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

    @Override
    public int getUnitFactor()
        {
        return m_nUnitFactor;
        }

    @Override
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

    @Override
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

    @Override
    public synchronized void setEvictionPolicy(ConfigurableCacheMap.EvictionPolicy policy)
        {
        int nType = (policy == null ? EVICTION_POLICY_HYBRID
                                    : EVICTION_POLICY_EXTERNAL);
        configureEviction(nType, policy);
        }

    @Override
    public ConfigurableCacheMap.EvictionApprover getEvictionApprover()
        {
        return m_apprvrEvict;
        }

    @Override
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

    @Override
    public ConfigurableCacheMap.UnitCalculator getUnitCalculator()
        {
        ConfigurableCacheMap.UnitCalculator calculator = m_calculator;

        if (calculator == null)
            {
            calculator = getUnitCalculatorType() == UNIT_CALCULATOR_BINARY
                         ? BinaryMemoryCalculator.INSTANCE
                         : LocalCache.InternalUnitCalculator.INSTANCE;
            }

        return calculator;
        }
    @Override
    public void setUnitCalculator(ConfigurableCacheMap.UnitCalculator calculator)
        {
        int nType = (calculator == null
                     ? UNIT_CALCULATOR_FIXED
                     : UNIT_CALCULATOR_EXTERNAL);
        configureUnitCalculator(nType, calculator);
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

    @Override
    public int getExpiryDelay()
        {
        return m_cExpiryDelay;
        }

    @Override
    public void setExpiryDelay(int cMillis)
        {
        m_cExpiryDelay = Math.max(cMillis, 0);
        }

    @Override
    public long getNextExpiryTime()
        {
        LongArray arrayExpiry = m_arrayExpiry;
        return arrayExpiry.isEmpty() ? 0 : arrayExpiry.getFirstIndex();
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

    @Override
    public void evictAll(Collection colKeys)
        {
        for (Object oKey : colKeys)
            {
            LocalCache.Entry entry = (LocalCache.Entry) getEntryInternal(oKey);
            if (entry != null)
                {
                removeEvicted(entry);
                }
            }
        }

    @Override
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
                if (lCurrent > m_lNextFlush && m_apprvrEvict != ConfigurableCacheMap.EvictionApprover.DISAPPROVER)
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

    // ----- JCache interface -----------------------------------------------

    /**
    * Determine the loader used by this LocalCache, if any.
    *
    * @return the loader used by this LocalCache, or null if none
    */
    public CacheLoader getCacheLoader()
        {
        return m_loader;
        }

    /**
    * Specify the loader used by this LocalCache.
    *
    * @param loader  loader to use, or null
    */
    public synchronized void setCacheLoader(CacheLoader loader)
        {
        if (loader != m_loader)
            {
            // unconfigure the old loader
            m_loader    = null;
            m_store     = null;

            MapListener listener = m_listener;
            if (listener != null)
                {
                removeMapListener(listener);
                m_listener = null;
                }

            // configure with the new loader
            m_loader = loader;
            if (loader instanceof CacheStore)
                {
                m_store    = (CacheStore) loader;
                m_listener = listener = instantiateInternalListener();
                this.addMapListener(listener);
                }
            }
        }

    /**
    * Locate an Entry in the hash map based on its key. If the Entry is not in
    * the cache, load the Entry for the specified key and return it.
    *
    * @param oKey  the key to the desired cached Entry
    *
    * @return the Entry corresponding to the specified key, otherwise null
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

        // Try to load and register Misses only if Cache Loader is configured.
        if (entry == null && getCacheLoader() != null)
            {
            long ldtStart = getCurrentTimeMillis();

            load(oKey);

            // use getEntryInternal() instead of get() to avoid screwing
            // up stats
            entry = (Entry) getEntryInternal(oKey);
            m_stats.registerMisses(0, ldtStart);
            }

        return entry;
        }

    /**
    * Get all the specified keys, if they are in the cache. For each key
    * that is in the cache, that key and its corresponding value will be
    * placed in the map that is returned by this method. The absence of
    * a key in the returned map indicates that it was not in the cache,
    * which may imply (for caches that can load behind the scenes) that
    * the requested data could not be loaded.
    * <p>
    * The result of this method is defined to be semantically the same as
    * the following implementation, without regards to threading issues:
    *
    * <pre><tt>
    * Map map = new AnyMap(); // could be hash map or ...
    * for (Iterator iter = col.iterator(); iter.hasNext(); )
    *     {
    *     Object oKey = iter.next();
    *     Object oVal = get(oKey);
    *     if (oVal != null || containsKey(oKey))
    *         {
    *         map.put(oKey, oVal);
    *         }
    *     }
    * return map;
    * </tt></pre>
    *
    * @param colKeys  a collection of keys that may be in the named cache
    *
    * @return a Map of keys to values for the specified keys passed in
    *         <tt>col</tt>
    */
    public Map getAll(Collection colKeys)
        {
        long ldtStart = 0;

        // first, get all of the requested keys that are already loaded
        // into the map
        Map map    = peekAll(colKeys);
        int cTotal = colKeys.size();
        int cHits  = map.size();
        if (cHits < cTotal)
            {
            // load the remaining keys
            CacheLoader loader = getCacheLoader();
            if (loader != null)
                {
                ldtStart = getCurrentTimeMillis();

                // build a list of the missing keys to load
                Set setRequest = new HashSet(colKeys);
                setRequest.removeAll(map.keySet());

                // load the missing keys
                loadAll(setRequest);

                // whichever ones are now loaded, add their values to the
                // result
                map.putAll(peekAll(setRequest));
                }
            }

        // update stats
        m_stats.registerHits(cHits, ldtStart);
        m_stats.registerMisses(cTotal - cHits, ldtStart);

        return map;
        }

    /**
    * Indicates to the cache that the specified key should be loaded into the
    * cache, if it is not already in the cache. This provides a means to
    * "pre-load" a single entry into the cache using the cache's loader.
    * <p>
    * If a valid entry with the specified key already exists in the cache,
    * or if the cache does not have a loader, then this method has no effect.
    * <p>
    * An implementation may perform the load operation asynchronously.
    *
    * @param oKey  the key to request to be loaded
    */
    public void load(final Object oKey)
        {
        CacheLoader loader = getCacheLoader();
        if (loader != null && getEntryInternal(oKey) == null)
            {
            Object oValue = loader.load(oKey);
            if (oValue != null)
                {
                KeyMask mask = new KeyMask()
                    {
                    public boolean isIgnored(Object oCheckKey)
                        {
                        return equals(oKey, oCheckKey);
                        }
                    };

                setKeyMask(mask);
                try
                    {
                    super.put(oKey, oValue);
                    }
                finally
                    {
                    setKeyMask(null);
                    }
                }
            }
        }

    /**
    * Indicates to the cache that it should load data from its loader to
    * fill the cache; this is sometimes referred to as "pre-loading" or
    * "warming" a cache.
    * <p>
    * The specific set of data that will be loaded is unspecified. The
    * implementation may choose to load all data, some specific subset
    * of the data, or no data. An implementation may require that the
    * loader implement the IterableCacheLoader interface in order for
    * this method to load any data.
    * <p>
    * An implementation may perform the load operation asynchronously.
    */
    public void loadAll()
        {
        CacheLoader loader = getCacheLoader();
        if (loader instanceof IterableCacheLoader)
            {
            Iterator iter = ((IterableCacheLoader) loader).keys();

            int cMaxUnits = getHighUnits();
            if (cMaxUnits > 0 && cMaxUnits < Integer.MAX_VALUE)
                {
                int cTarget  = Math.max(getLowUnits(), (int) (0.9 * cMaxUnits));
                int cCurrent = getUnits();
                while (iter.hasNext() && cCurrent < cTarget)
                    {
                    load(iter.next());

                    int cUnits = getUnits();
                    if (cUnits < cCurrent)
                        {
                        // cache is already starting to prune itself for
                        // some reason; assume that eviction occurred
                        // which is an indication that we've warmed the
                        // cache suitably
                        break;
                        }

                    cCurrent = cUnits;
                    }
                }
            else
                {
                loadAll(new ImmutableArrayList(SimpleEnumerator.toArray(iter)));
                }
            }
        }

    /**
    * Indicates to the cache that the specified keys should be loaded into
    * the cache, if they are not already in the cache. This provides a means
    * to "pre-load" entries into the cache using the cache's loader.
    * <p>
    * The result of this method is defined to be semantically the same as
    * the following implementation:
    *
    * <pre><tt>
    * CacheLoader loader = getCacheLoader();
    * if (loader != null &amp;&amp; !colKeys.isEmpty())
    *     {
    *     Set setRequest = new HashSet(colKeys);
    *     setRequest.removeAll(peekAll(colKeys).keySet());
    *     if (!setRequest.isEmpty())
    *         {
    *         Map map = loader.loadAll(colKeys);
    *         if (!map.isEmpty())
    *             {
    *             putAll(map);
    *             }
    *         }
    *     }
    * </tt></pre>
    *
    * @param colKeys  a collection of keys to request to be loaded
    */
    public void loadAll(Collection colKeys)
        {
        CacheLoader loader = getCacheLoader();
        if (loader != null && !colKeys.isEmpty())
            {
            Set setRequest = new HashSet(colKeys);
            setRequest.removeAll(peekAll(colKeys).keySet());
            if (!setRequest.isEmpty())
                {
                Map map = loader.loadAll(setRequest);
                if (!map.isEmpty())
                    {
                    final Set setKeys = map.keySet();
                    KeyMask mask = new KeyMask()
                        {
                        public boolean isIgnored(Object oCheckKey)
                            {
                            return setKeys.contains(oCheckKey);
                            }
                        };

                    setKeyMask(mask);
                    try
                        {
                        super.putAll(map);
                        }
                    finally
                        {
                        setKeyMask(null);
                        }
                    }
                }
            }
        }

    /**
    * Checks for a valid entry corresponding to the specified key in the
    * cache, and returns the corresponding value if it is. If it is not in
    * the cache, returns null, and does not attempt to load the value using
    * its cache loader.
    *
    * @param oKey  the key to "peek" into the cache for
    *
    * @return the value corresponding to the specified key
    */
    public Object peek(Object oKey)
        {
        // avoid super.get() because it affects statistics
        LocalCache.Entry entry = (LocalCache.Entry) getEntryInternal(oKey);
        return entry == null ? null : entry.getValue();
        }

    /**
    * Checks for a valid entry corresponding to each specified key in the
    * cache, and places the corresponding value in the returned map if it is.
    * For each key that is not in the cache, no entry is placed into the
    * returned map. The cache does not attempt to load any values using
    * its cache loader.
    * <p>
    * The result of this method is defined to be semantically the same as
    * the following implementation, without regards to threading issues:
    *
    * <pre><tt>
    * Map map = new HashMap();
    * for (Iterator iter = colKeys.iterator(); iter.hasNext(); )
    *     {
    *     Object oKey   = iter.next();
    *     Object oValue = peek(oKey);
    *     if (oValue != null || containsKey(oKey))
    *         {
    *         map.put(oKey, oValue);
    *         }
    *     }
    * return map;
    * </tt></pre>
    *
    * @param colKeys  a collection of keys to "peek" into the cache for
    *
    * @return a Map of keys that were found in the cache and their values
    */
    public Map peekAll(Collection colKeys)
        {
        Map map = new HashMap(colKeys.size());
        for (Iterator iter = colKeys.iterator(); iter.hasNext(); )
            {
            Object      oKey  = iter.next();
            LocalCache.Entry entry = (LocalCache.Entry) getEntryInternal(oKey);
            if (entry != null)
                {
                map.put(oKey, entry.getValue());
                }
            }
        return map;
        }

    // ----- inner class: EntrySet ------------------------------------------

    /**
     * Factory pattern.
     *
     * @return a new instance of the EntrySet class (or a subclass thereof)
     */
    protected SafeHashMap.EntrySet instantiateEntrySet()
        {
        return new LocalCache.EntrySet();
        }

    /**
     * A set of entries backed by this map.
     */
    protected class EntrySet
            extends SafeHashMap.EntrySet
        {
        // ----- Set interface ------------------------------------------

        @Override
        public Iterator iterator()
            {
            // optimization
            if (LocalCache.super.isEmpty())
                {
                return NullImplementation.getIterator();
                }

            // complete entry set iterator
            Iterator iter = instantiateIterator();

            // filter to get rid of expired objects
            Filter<LocalCache.Entry> filter = (LocalCache.Entry entry) -> !removeIfExpired(entry);

            return new FilterEnumerator(iter, filter);
            }

        @Override
        public Object[] toArray(Object ao[])
            {
            Object[] aoAll = super.toArray(ao);
            int      cAll  = aoAll.length;

            int ofSrc  = 0;
            int ofDest = 0;
            while (ofSrc < cAll)
                {
                LocalCache.Entry entry = (LocalCache.Entry) aoAll[ofSrc];
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
        return new LocalCache.KeySet();
        }

    /**
     * A set of entries backed by this map.
     */
    protected class KeySet
            extends SafeHashMap.KeySet
        {
        // ----- Set interface ------------------------------------------

        @Override
        public Object[] toArray(Object ao[])
            {
            // build list of non-expired keys
            Object[] aoAll;
            int      cAll = 0;

            // synchronizing prevents add/remove, keeping size() constant
            LocalCache map = LocalCache.this;
            synchronized (map)
                {
                // create the array to store the map keys
                int c = size();
                aoAll = new Object[c];
                if (c > 0)
                    {
                    // walk all buckets
                    SafeHashMap.Entry[] aeBucket = map.m_aeBucket;
                    for (SafeHashMap.Entry e : aeBucket)
                        {
                        // walk all entries in the bucket
                        LocalCache.Entry entry = (LocalCache.Entry) e;
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

        @Override
        public int size()
            {
            // COH-1089: get the size value without causing any eviction
            //           (see SafeHashMap#size)
            return LocalCache.super.size();
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
        return new LocalCache.ValuesCollection();
        }

    /**
     * A collection of values backed by this map.
     */
    protected class ValuesCollection
            extends SafeHashMap.ValuesCollection
        {
        // ----- Collection interface -----------------------------------

        @Override
        public Object[] toArray(Object ao[])
            {
            // build list of non-expired values
            Object[] aoAll;
            int      cAll = 0;

            // synchronizing prevents add/remove, keeping size() constant
            LocalCache map = LocalCache.this;
            synchronized (map)
                {
                // create the array to store the map values
                int c = size();
                aoAll = new Object[c];
                if (c > 0)
                    {
                    // walk all buckets
                    SafeHashMap.Entry[] aeBucket = map.m_aeBucket;
                    for (SafeHashMap.Entry e : aeBucket)
                        {
                        // walk all entries in the bucket
                        LocalCache.Entry entry = (LocalCache.Entry) e;
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

        @Override
        public int size()
            {
            // COH-1089: get the size value without causing any eviction
            //           (see SafeHashMap#size)
            return LocalCache.super.size();
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
     * Returns the CacheStatistics for this cache.
     *
     * @return a CacheStatistics object
     */
    public CacheStatistics getCacheStatistics()
        {
        return m_stats;
        }

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

    // ----- internal -------------------------------------------------------

    /**
     * Attempt to call evict() when no one else is, to avoid contention on
     * opportunistic attempts at evicting.
     */
    protected void tryEvict()
        {
        long lCurrent = getCurrentTimeMillis();

        if (lCurrent > m_lNextFlush &&
            m_apprvrEvict != ConfigurableCacheMap.EvictionApprover.DISAPPROVER &&
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
                else if (policy instanceof LocalCache.InternalEvictionPolicy)
                    {
                    nType  = ((LocalCache.InternalEvictionPolicy) policy).getEvictionType();
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
                else if (calculator == LocalCache.InternalUnitCalculator.INSTANCE)
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
            LocalCache.Entry entry  = (LocalCache.Entry) iter.next();
            int   cUnits = entry.calculateUnits(entry.getValue());

            // update both the entry unit count and total unit count
            entry.setUnits(cUnits);
            }
        }

    @Override
    protected SafeHashMap.Entry getEntryInternal(Object oKey)
        {
        LocalCache.Entry entry = (LocalCache.Entry) super.getEntryInternal(oKey);

        if (entry != null && removeIfExpired(entry))
            {
            entry = null;
            }

        return entry;
        }

    /**
    * Determine the store used by this LocalCache, if any.
    *
    * @return the CacheStore used by this LocalCache, or null if none
    */
    protected CacheStore getCacheStore()
        {
        return m_store;
        }

    /**
    * ThreadLocal: Get the current key mask for the current thread.
    *
    * @return the current key mask
    */
    protected KeyMask getKeyMask()
        {
        KeyMask mask = (KeyMask) m_tloIgnore.get();
        return mask == null ? DEFAULT_KEY_MASK : mask;
        }

    /**
    * ThreadLocal: Set the key mask for the current thread.
    *
    * @param mask  the new key mask, or null to clear the mask
    */
    protected void setKeyMask(KeyMask mask)
        {
        m_tloIgnore.set(mask);
        }

    /**
    * Remove an entry (if it is eligible for eviction) because it has expired.
    *
    * @param entry  the expired cache entry
    *
    * @return true iff the entry was removed
    */
    protected synchronized boolean removeEvicted(LocalCache.Entry entry)
        {
        long    dtExpiry     = entry.getExpiryMillis();
        boolean fExpired     = dtExpiry != 0 && (dtExpiry & ~0xFFL) < getCurrentTimeMillis();
        KeyMask mask         = getKeyMask();
        boolean fPrev        = mask.ensureSynthetic();
        boolean fPrevExpired = fExpired ? mask.ensureExpired() : false;
        try
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
        finally
            {
            mask.setSynthetic(fPrev);
            mask.setExpired(fPrevExpired);
            }
        }

    /**
    * Factory pattern: instantiate a new CacheEvent corresponding
    * to the specified parameters.
    *
    * @return a new instance of the CacheEvent class (or a subclass thereof)
    */
    protected MapEvent instantiateMapEvent(int nId, Object oKey, Object oValueOld, Object oValueNew)
        {
        return new CacheEvent(this,
                              nId,
                              oKey,
                              oValueOld,
                              oValueNew,
                              getKeyMask().isSynthetic(),
                              CacheEvent.TransformationState.TRANSFORMABLE,
                              false,
                              getKeyMask().isExpired());
        }

    /**
     * Remove an entry (if it is eligible for eviction) because it has expired.
     * <p>
     * Note: This method is the same as {@link #removeEvicted(LocalCache.Entry)} and is left
     * for backward compatibility.
     *
     * @param entry            the expired cache entry
     * @param fRemoveInternal  true if the cache entry still needs to be
     *                         removed from the cache
     *
     * @return true iff the entry was removed
     *
     * @deprecated use {@link #removeEvicted(LocalCache.Entry)} instead
     */
    protected boolean removeExpired(LocalCache.Entry entry, boolean fRemoveInternal)
        {
        return removeEvicted(entry);
        }

    /**
     * Remove an entry if it has expired.
     *
     * @param entry  the entry
     *
     * @return true iff the entry was actually removed
     */
    protected boolean removeIfExpired(LocalCache.Entry entry)
        {
        if (entry.isExpired() && m_apprvrEvict != ConfigurableCacheMap.EvictionApprover.DISAPPROVER)
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
        if (m_cCurUnits <= cMax || m_apprvrEvict == ConfigurableCacheMap.EvictionApprover.DISAPPROVER)
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

            long          cTarget   = m_cPruneUnits;
            ArrayList     listEvict = null;
            long          cRemEvict = m_cCurUnits - cTarget;
            boolean       fLRU      = (nType == EVICTION_POLICY_LRU);

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
                SafeHashMap.Entry[] aeBucket = m_aeBucket;
                for (SafeHashMap.Entry e : aeBucket)
                    {
                    LocalCache.Entry entry = (LocalCache.Entry) e;
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
                        cRemEvict -= queueForEviction((LocalCache.Entry) entry, listEvict);
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
                SafeHashMap.Entry[] aeBucket = m_aeBucket;
                for (SafeHashMap.Entry e : aeBucket)
                    {
                    LocalCache.Entry entry = (LocalCache.Entry) e;
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
                    if (o instanceof LocalCache.Entry)
                        {
                        cRemEvict -= queueForEviction((LocalCache.Entry) o, listEvict);
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
                            cRemEvict -= queueForEviction((LocalCache.Entry) entry, listEvict);
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
                SafeHashMap.Entry[] aeBucket = m_aeBucket;
                for (SafeHashMap.Entry e : aeBucket)
                    {
                    LocalCache.Entry entry = (LocalCache.Entry) e;
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
                m_iterEvict = listEvict.iterator();
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
    //private int queueForEviction(Entry entry, Set listEvict)
    private int queueForEviction(LocalCache.Entry entry, List listEvict)
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
        Iterator iterEvict = m_iterEvict;
        if (iterEvict != null)
            {
            // pruning will proceed until the cache is down below the max
            // units, but since there's a cost to this processing, do some
            // arbitrary minimum number of evictions while we're here
            long cMaxUnits   = m_cMaxUnits;
            int  cMinEntries = 60;
            while (iterEvict.hasNext())
                {
                LocalCache.Entry entry = (LocalCache.Entry) getEntryInternal(iterEvict.next());

                iterEvict.remove();
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
     * Determine if the LocalCache has any listeners at all.
     *
     * @return true iff this LocalCache has at least one MapListener
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

    // ----- inner class: InternalListener ----------------------------------

    /**
    * Factory pattern: Instantiate an internal MapListener to listen to this
    * cache and report changes to the CacheStore.
    *
    * @return  a new MapListener instance
    */
    protected MapListener instantiateInternalListener()
        {
        return new InternalListener();
        }

    /**
    * An internal MapListener that listens to this cache and reports
    * changes to the CacheStore.
    */
    protected class InternalListener
            extends Base
            implements MapListener
        {
        /**
        * Invoked when a map entry has been inserted.
        *
        * @param evt  the MapEvent carrying the insert information
        */
        public void entryInserted(MapEvent evt)
            {
            onModify(evt);
            }

        /**
        * Invoked when a map entry has been updated.
        *
        * @param evt  the MapEvent carrying the update information
        */
        public void entryUpdated(MapEvent evt)
            {
            onModify(evt);
            }

        /**
        * Invoked when a map entry has been removed.
        *
        * @param evt  the MapEvent carrying the delete information
        */
        public void entryDeleted(MapEvent evt)
            {
            // deletions are handled by the clear() and remove(Object)
            // methods, and are ignored by the listener, because they
            // include evictions, which may be impossible to differentiate
            // from client-invoked removes and clears
            }

        /**
        * A value modification event (insert or update) has occurred.
        *
        * @param evt  the MapEvent object
        */
        protected void onModify(MapEvent evt)
            {
            if (!getKeyMask().isIgnored(evt.getKey()))
                {
                CacheStore store = getCacheStore();
                if (store != null)
                    {
                    store.store(evt.getKey(), evt.getNewValue());
                    }
                }
            }
        }


    // ----- inner class: KeyMask -------------------------------------------

    /**
    * A class that masks certain changes so that they are not reported back
    * to the CacheStore.
    */
    protected class KeyMask
            extends Base
        {
        /**
        * Check if a key should be ignored.
        *
        * @param oKey  the key that a change event has occurred for
        *
        * @return true if change events for the key should be ignored
        */
        public boolean isIgnored(Object oKey)
            {
            return false;
            }

        /**
        * Check whether or not the currently performed operation is
        * internally initiated.
        *
        * @return true iff the current operation is internal
        */
        public boolean isSynthetic()
            {
            return true;
            }

        /**
        * Specify whether or not the currently performed operation is internally
        * initiated.
        *
        * @param fSynthetic  true iff the the current operation is internal
        */
        public void setSynthetic(boolean fSynthetic)
            {
            }

        /**
        * Check whether or not the currently performed operation has been initiated
        * because the entry expired.
        *
        * @return true iff the entry has expired
        *
        * @since 22.06
        */
        public boolean isExpired()
            {
            return true;
            }

        /**
        * Specify whether or not the currently performed operation concerns an
        * expired entry
        *
        * @param fExpired  true iff the current operation is an expiration one
        *
        * @since 22.06
        */
        public void setExpired(boolean fExpired)
            {
            }

        /**
        * Ensure that the synthetic operation flag is set.
        *
        * @return the previous value of the flag
        */
        public boolean ensureSynthetic()
            {
            boolean f = isSynthetic();
            if (!f)
                {
                setSynthetic(true);
                }
            return f;
            }

        /**
        * Ensure that the expired operation flag is set.
        *
        * @return the previous value of the flag
        *
        * @since 22.06
        */
        public boolean ensureExpired()
            {
            boolean f = isExpired();
            if (!f)
                {
                setExpired(true);
                }
            return f;
            }
        }


    // ----- inner class: Entry ---------------------------------------------

    /**
     * Factory method.  This method exists to allow the LocalCache class to be
     * easily inherited from by allowing the Entry class to be easily
     * sub-classed.
     *
     * @return an instance of Entry that holds the passed cache value
     */
    protected SafeHashMap.Entry instantiateEntry()
        {
        return new LocalCache.Entry();
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

        @Override
        protected void onAdd()
            {
            scheduleExpiry();

            // update units
            int        cNewUnits = calculateUnits(m_oValue);
            LocalCache map       = LocalCache.this;
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

        @Override
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
            int        cNewUnits = calculateUnits(oValue);
            LocalCache map       = LocalCache.this;
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

        @Override
        protected void copyFrom(SafeHashMap.Entry entry)
            {
            LocalCache.Entry entryThat = (LocalCache.Entry) entry;

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
            LocalCache.Entry entryNext = getNext();
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

        @Override
        public void touch()
            {
            ++m_cUses;
            m_dtLastUse = getCurrentTimeMillis();

            ConfigurableCacheMap.EvictionPolicy policy = LocalCache.this.m_policy;
            if (policy != null)
                {
                policy.entryTouched(this);
                }
            }

        @Override
        public long getLastTouchMillis()
            {
            return m_dtLastUse;
            }

        @Override
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

        @Override
        public long getExpiryMillis()
            {
            return m_dtExpiry;
            }

        @Override
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
            int  cDelay   = LocalCache.this.m_cExpiryDelay;
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

                LocalCache map = LocalCache.this;
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
            LocalCache map  = LocalCache.this;
            Object     oKey = getKey();
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

        @Override
        public int getUnits()
            {
            return m_cUnits;
            }

        @Override
        public void setUnits(int cUnits)
            {
            azzert(cUnits >= 0);

            synchronized (LocalCache.this)
                {
                int cOldUnits = m_cUnits;
                if (cOldUnits == -1)
                    {
                    // entry is discarded; avoid exception
                    return;
                    }

                if (cUnits != cOldUnits)
                    {
                    LocalCache.this.adjustUnits(cUnits - cOldUnits);
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
        LocalCache.Entry getNext()
            {
            return (LocalCache.Entry) m_eNext;
            }

        /**
         * Package Private: Specify the next cache entry in the chain of
         * cache entries for a given hash bucket.
         *
         * @param entry  the next cache entry
         */
        void setNext(LocalCache.Entry entry)
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

        @Override
        public void entryTouched(ConfigurableCacheMap.Entry entry)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public void requestEviction(int cMaximum)
            {
            throw new UnsupportedOperationException();
            }

        @Override
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
         * Determine the LocalCache eviction type represented by this
         * InternalEvictionPolicy.
         *
         * @return one of the EVICTION_POLICY_* constants
         */
        public int getEvictionType()
            {
            return m_nType;
            }

        /**
         * The LocalCache eviction type represented by this
         * InternalEvictionPolicy; one of the EVICTION_POLICY_* constants.
         */
        private int m_nType;
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

        @Override
        public int calculateUnits(Object oKey, Object oValue)
            {
            return 1;
            }

        @Override
        public String getName()
            {
            return "Internal-Fixed";
            }

        /**
         * Singleton instance.
         */
        public static final LocalCache.InternalUnitCalculator INSTANCE = new LocalCache.InternalUnitCalculator();
        }

    // ----- constants ------------------------------------------------------

    /**
    * By default, the cache size (in units) is infinite.
    */
    public static final int DEFAULT_UNITS    = Integer.MAX_VALUE;

    /**
    * By default, the cache entries never expire.
    */
    public static final int DEFAULT_EXPIRE   = 0;

    /**
    * The default key mask that ignores nothing.
    */
    protected final KeyMask DEFAULT_KEY_MASK = new KeyMask()
        {
        public boolean isSynthetic()
            {
            return m_fSynthetic;
            }
        public void setSynthetic(boolean fSynthetic)
            {
            m_fSynthetic = fSynthetic;
            }
        public boolean isExpired()
            {
            return m_fExpired;
            }
        public void setExpired(boolean fExpired)
            {
            m_fExpired = fExpired;
            }
        private boolean m_fSynthetic = false;
        private boolean m_fExpired   = false;
        };

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
    public static final LocalCache.EvictionPolicy INSTANCE_HYBRID = new LocalCache.InternalEvictionPolicy(EVICTION_POLICY_HYBRID);

    /**
     * The EvictionPolicy object for the Least Recently Used (LRU) eviction
     * algorithm.
     */
    public static final LocalCache.EvictionPolicy INSTANCE_LRU    = new LocalCache.InternalEvictionPolicy(EVICTION_POLICY_LRU);

    /**
     * The EvictionPolicy object for the Least Frequently Used (LFU) eviction
     * algorithm.
     */
    public static final LocalCache.EvictionPolicy INSTANCE_LFU    = new LocalCache.InternalEvictionPolicy(EVICTION_POLICY_LFU);

    /**
     * The UnitCalculator object that counts each entry as one unit.
     */
    public static final LocalCache.UnitCalculator INSTANCE_FIXED  = LocalCache.InternalUnitCalculator.INSTANCE;

    /**
     * The UnitCalculator object that measures the bytes used by entries. This
     * is intended for caches that manage binary data.
     */
    public static final LocalCache.UnitCalculator INSTANCE_BINARY = BinaryMemoryCalculator.INSTANCE;


    // ----- data members ---------------------------------------------------

    /**
    * The loader used by this cache for misses.
    */
    private CacheLoader m_loader;

    /**
    * The store used by this cache for modifications. If this value is
    * non-null, then it is the same reference as the loader.
    */
    private CacheStore m_store;

    /**
    * The map listener used by this cache to listen to itself in order to
    * pass events to the CacheStore. Only used when there is a CacheStore.
    */
    private MapListener m_listener;

    /**
    * The thread-local object to check for keys that the current thread
    * is supposed to ignore if those keys change. Contains KeyMask objects.
    */
    private ThreadLocal m_tloIgnore = new ThreadLocal();

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
    protected Iterator m_iterEvict;

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
