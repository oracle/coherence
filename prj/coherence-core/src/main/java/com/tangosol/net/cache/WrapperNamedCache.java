/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.QueryMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.WrapperConcurrentMap;

import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
* A simple implementation of NamedCache interface built as a
* wrapper around any Map implementation.
*
* @param <K>  the type of the cache entry keys
* @param <V>  the type of the cache entry values
*
* @author cp 2003.05.19
*/
public class WrapperNamedCache<K, V>
        extends WrapperConcurrentMap<K, V>
        implements NamedCache<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a NamedCache wrapper for an empty HashMap.
    *
    * @param sName  the cache name
    */
    public WrapperNamedCache(String sName)
        {
        this(new HashMap<>(), sName);
        }

    /**
    * Construct a NamedCache wrapper based on the specified map.
    * <p>
    * <b>Note:</b> it is assumed that while the WrapperNamedCache exists,
    * there is no direct manipulation with the content of the wrapped map.
    *
    * @param map    the Map that will be wrapped by this WrapperNamedCache
    * @param sName  the cache name
    */
    public WrapperNamedCache(Map<K, V> map, String sName)
        {
        this(map, sName, null);
        }

    /**
    * Construct a NamedCache wrapper based on the specified map.
    * <p>
    * <b>Note:</b> it is assumed that while the WrapperNamedCache exists,
    * there is no direct manipulation with the content of the wrapped map.
    *
    * @param map      the Map that will be wrapped by this WrapperNamedCache
    * @param sName    the cache name (could be null if the map is a NamedCache)
    * @param service  the cache service this NamedCache is a part of
    *                 (ignored if the map is a NamedCache)
    */
    public WrapperNamedCache(Map<K, V> map, String sName, CacheService service)
        {
        super(map, false, -1L);

        m_sName   = map instanceof NamedCache && sName == null ?
                    ((NamedCache) map).getCacheName()    : sName;
        m_service = map instanceof NamedCache ?
                    ((NamedCache) map).getCacheService() : service;
        }


    // ----- NamedCache interface -------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String getCacheName()
        {
        return m_sName;
        }

    /**
    * {@inheritDoc}
    */
    public CacheService getCacheService()
        {
        CacheService service = m_service;
        if (service == null)
            {
            Map map = getMap();
            if (map instanceof NamedCache)
                {
                service = ((NamedCache) map).getCacheService();
                }
            }
        return service;
        }

    /**
    * {@inheritDoc}
    */
    public boolean isActive()
        {
        Map map = getMap();
        return !(map instanceof NamedCache) || ((NamedCache) map).isActive();
        }

    @Override
    public boolean isReady()
        {
        Map map = getMap();
        return !(map instanceof NamedMap) || ((NamedMap) map).isActive();
        }

    /**
    * {@inheritDoc}
    */
    public void release()
        {
        Map map = getMap();
        if (map instanceof NamedCache)
            {
            ((NamedCache) map).release();
            }
        else
            {
            // no-op
            }
        }

    /**
    * {@inheritDoc}
    */
    public void destroy()
        {
        Map map = getMap();
        if (map instanceof NamedCache)
            {
            ((NamedCache) map).destroy();
            }
        else
            {
            // no-op
            }
        }

    /**
     * {@inheritDoc}
     */
    public void truncate()
        {
        Map map = getMap();
        if (map instanceof NamedCache)
            {
            ((NamedCache) map).truncate();
            }
        else
            {
            clear();
            }
        }

    /**
     * {@inheritDoc}
     */
    public boolean isDestroyed()
        {
        Map map = getMap();
        return map instanceof NamedCache && ((NamedCache) map).isDestroyed();
        }

    /**
     * {@inheritDoc}
     */
    public boolean isReleased()
        {
        Map map = getMap();
        return map instanceof NamedCache && ((NamedCache) map).isReleased();
        }

    // ----- CacheMap interface ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public Map<K, V> getAll(Collection<? extends K> colKeys)
        {
        Map<K, V> map = getMap();
        if (map instanceof CacheMap)
            {
            long ldtStart  = getSafeTimeMillis();
            Map<K, V> mapResult = ((CacheMap<K, V>) map).getAll(colKeys);

            if (isCollectStats())
                {
                int cHits = mapResult.size();
                if (cHits > 0)
                    {
                    m_stats.registerHits(cHits, ldtStart);
                    }
                int cMisses = colKeys.size() - cHits;
                if (cMisses > 0)
                    {
                    m_stats.registerMisses(cMisses, ldtStart);
                    }
                }
            return mapResult;
            }
        else
            {
            Map<K, V> mapResult = new HashMap<>(colKeys.size());
            for (K key : colKeys)
                {
                V value = get(key);
                if (value != null || containsKey(key))
                    {
                    mapResult.put(key, value);
                    }
                }
            return mapResult;
            }
        }

    /**
    * {@inheritDoc}
    */
    public V put(K oKey, V oValue, long cMillis)
        {
        V oOrig;

        Map<K, V> mapInner = getMap();
        if (mapInner instanceof CacheMap)
            {
            // for an understanding of this locking-related code, see
            // WrapperConcurrentMap#put
            boolean fForceLock = isLockingEnforced();
            if (!fForceLock || lock(oKey, getWaitMillis()))
                {
                try
                    {
                    boolean fStats   = isCollectStats();
                    long    ldtStart = fStats ? getSafeTimeMillis() : 0L;

                    // for an understanding of this event-related code, see
                    // WrapperObservableMap#put
                    if (isEventFabricator())
                        {
                        int nEvent = mapInner.containsKey(oKey)
                                     ? MapEvent.ENTRY_UPDATED
                                     : MapEvent.ENTRY_INSERTED;
                        oOrig = ((CacheMap<K, V>) mapInner).put(oKey, oValue, cMillis);
                        dispatchEvent(new CacheEvent<>(this, nEvent, oKey,
                                                       oOrig, oValue, false));
                        }
                    else
                        {
                        oOrig = ((CacheMap<K, V>) mapInner).put(oKey, oValue, cMillis);
                        }

                    if (fStats)
                        {
                        // update statistics
                        m_stats.registerPut(ldtStart);
                        }
                    }
                finally
                    {
                    if (fForceLock)
                        {
                        unlock(oKey);
                        }
                    }
                }
            else
                {
                throw new ConcurrentModificationException(
                        "(thread=" + Thread.currentThread() + ") "
                        + getLockDescription(oKey));
                }
            }
        else if (cMillis <= 0)
            {
            oOrig = super.put(oKey, oValue);
            }
        else
            {
            throw new UnsupportedOperationException();
            }

        return oOrig;
        }


    // ----- QueryMap interface ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Set<K> keySet(Filter filter)
        {
        Map<K, V> map = getMap();
        return map instanceof QueryMap ?
            ((QueryMap<K, V>) map).keySet(filter) :
            InvocableMapHelper.query(map, filter, false, false, null);
        }

    /**
    * {@inheritDoc}
    */
    public Set<Map.Entry<K, V>> entrySet(Filter filter)
        {
        Map<K, V> map = getMap();
        return map instanceof QueryMap ?
            ((QueryMap<K, V>) map).entrySet(filter) :
            InvocableMapHelper.query(map, filter, true, false, null);
        }

    /**
    * {@inheritDoc}
    */
    public Set<Map.Entry<K, V>> entrySet(Filter filter, Comparator comparator)
        {
        Map<K, V> map = getMap();
        return map instanceof QueryMap ?
            ((QueryMap<K, V>) map).entrySet(filter, comparator) :
            InvocableMapHelper.query(map, filter, true, true, comparator);
        }

    /**
    * {@inheritDoc}
    */
    public <T, E> void addIndex(ValueExtractor<? super T, ? extends E> extractor, boolean fOrdered, Comparator<? super E> comparator)
        {
        Map<K, V> map = getMap();
        if (map instanceof QueryMap)
            {
            ((QueryMap<K, V>) map).addIndex(extractor, fOrdered, comparator);
            }
        }

    /**
    * {@inheritDoc}
    */
    public <T, E> void removeIndex(ValueExtractor<? super T, ? extends E> extractor)
        {
        Map<K, V> map = getMap();
        if (map instanceof QueryMap)
            {
            ((QueryMap<K, V>) map).removeIndex(extractor);
            }
        }


    // ----- InvocableMap interface -----------------------------------------

    /**
    * {@inheritDoc}
    */
    public <R> R invoke(K key, EntryProcessor<K, V, R> agent)
        {
        Map<K, V> map = getMap();
        if (map instanceof InvocableMap)
            {
            return ((InvocableMap<K, V>) map).invoke(key, agent);
            }
        else
            {
            return InvocableMapHelper.invokeLocked(this,
                                                   InvocableMapHelper.makeEntry(this, key), agent);
            }
        }

    /**
    * {@inheritDoc}
    */
    public <R> Map<K, R> invokeAll(Collection<? extends K> collKeys, EntryProcessor<K, V, R> agent)
        {
        Map<K, V> map = getMap();
        if (map instanceof InvocableMap)
            {
            return ((InvocableMap<K, V>) map).invokeAll(collKeys, agent);
            }
        else
            {
            return InvocableMapHelper.invokeAllLocked(this,
                                                      InvocableMapHelper.makeEntrySet(this, collKeys, false), agent);
            }
        }

    /**
    * {@inheritDoc}
    */
    public <R> Map<K, R> invokeAll(Filter filter, EntryProcessor<K, V, R> agent)
        {
        Map<K, V> map = getMap();
        if (map instanceof InvocableMap)
            {
            return ((InvocableMap<K, V>) map).invokeAll(filter, agent);
            }
        else
            {
            return invokeAll(keySet(filter), agent);
            }
        }

    /**
    * {@inheritDoc}
    */
    public <R> R aggregate(Collection<? extends K> collKeys, EntryAggregator<? super K, ? super V, R> agent)
        {
        Map<K, V> map = getMap();
        if (map instanceof InvocableMap)
            {
            return ((InvocableMap<K, V>) map).aggregate(collKeys, agent);
            }
        else
            {
            return agent.aggregate(InvocableMapHelper.makeEntrySet(this, collKeys, true));
            }
        }

    /**
    * {@inheritDoc}
    */
    public <R> R aggregate(Filter filter, EntryAggregator<? super K, ? super V, R> agent)
        {
        Map<K, V> map = getMap();
        if (map instanceof InvocableMap)
            {
            return ((InvocableMap<K, V>) map).aggregate(filter, agent);
            }
        else
            {
            return aggregate(keySet(filter), agent);
            }
        }


    // ----- data fields ----------------------------------------------------

    /**
    * The name of the cache.
    */
    protected String m_sName;

    /**
    * The CacheService this NamedCache is a part of.
    */
    protected CacheService m_service;
    }
