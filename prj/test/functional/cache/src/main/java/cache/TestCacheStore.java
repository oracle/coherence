/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package cache;

import com.oracle.coherence.common.collections.ConcurrentHashMap;
import com.oracle.coherence.common.collections.ConcurrentLinkedQueue;

import com.tangosol.net.cache.CacheStore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * {@link CacheStore} implementation for testing with AsyncNamedcache
 * putAll operation.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class TestCacheStore<K, V>
        implements CacheStore<K, V>
    {
    @Override
    public void store(K key, V value)
        {
        m_mapPut.put(key, value);
        }

    @Override
    public void erase(K key)
        {
        m_queueErase.offer(key);
        }

    @Override
    public V load(Object key)
        {
        m_queueGet.offer(key);
        return (V) m_mapData.get(key);
        }

    public static void clear()
        {
        m_queueGet.clear();
        m_queueErase.clear();
        m_mapPut.clear();
        m_mapData.clear();
        }

    public static void put(Object key, Object value)
        {
        m_mapData.put(key, value);
        }

    public static void putAll(Map map)
        {
        m_mapData.putAll(map);
        }

    public static Map getStores()
        {
        return m_mapPut;
        }

    public static Set getLoadsAsSet()
        {
        return new HashSet(getLoads());
        }

    public static Queue getLoads()
        {
        return m_queueGet;
        }

    public static Set getErasesAsSet()
        {
        return new HashSet(getErases());
        }

    public static Queue getErases()
        {
        return m_queueErase;
        }

    // ----- data members ---------------------------------------------------

    private static final Queue m_queueGet = new ConcurrentLinkedQueue<>();

    private static final Queue m_queueErase = new ConcurrentLinkedQueue<>();

    private static final Map m_mapPut = new ConcurrentHashMap<>();

    private static final Map m_mapData = new HashMap();
    }
