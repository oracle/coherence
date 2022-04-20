/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.rwbm;

import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.net.cache.BinaryEntryStore;
import com.tangosol.net.cache.CacheLoader;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.NonBlockingEntryStore;
import com.tangosol.net.cache.ReadWriteBackingMap;

import com.tangosol.util.Base;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ObservableMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom extension of the ReadWriteBackingMap allowing to simulate eviction
 * "on-demand".
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class EvictingRWBM
        extends ReadWriteBackingMap
    {
    public EvictingRWBM(BackingMapManagerContext ctxService, ObservableMap mapInternal, Map mapMisses,
                        NonBlockingEntryStore binaryStore, boolean fReadOnly, int cWriteBehindSeconds,
                        double dflRefreshAheadFactor)
        {
        super(ctxService, mapInternal, mapMisses, binaryStore, fReadOnly, cWriteBehindSeconds, dflRefreshAheadFactor);
        }

    public EvictingRWBM(BackingMapManagerContext ctxService, ObservableMap mapInternal, Map mapMisses,
                        BinaryEntryStore binaryStore, boolean fReadOnly, int cWriteBehindSeconds,
                        double dflRefreshAheadFactor)
        {
        super(ctxService, mapInternal, mapMisses, binaryStore, fReadOnly, cWriteBehindSeconds, dflRefreshAheadFactor);
        }

    public EvictingRWBM(BackingMapManagerContext ctxService, ObservableMap mapInternal, Map mapMisses,
                        CacheLoader loader, boolean fReadOnly, int cWriteBehindSeconds, double dflRefreshAheadFactor)
        {
        super(ctxService, mapInternal, mapMisses, loader, fReadOnly, cWriteBehindSeconds, dflRefreshAheadFactor);
        }

    public boolean containsKey(Object oKey)
        {
        checkEvict(oKey, true);

        return super.containsKey(oKey);
        }

    public Object get(Object oKey)
        {
        checkEvict(oKey, true);

        return super.get(oKey);
        }

    public void putAll(Map map)
        {
        checkEvictMap(map, true);

        try
            {
            super.putAll(map);
            }
        finally
            {
            checkEvictMap(map, false);
            }
        }

    protected Object putInternal(Object oKey, Object oValue, long cMillis)
        {
        checkEvict(oKey, true);

        try
            {
            return super.putInternal(oKey, oValue, cMillis);
            }
        finally
            {
            checkEvict(oKey, false);
            }
        }

    public Object removeInternal(Object oKey, boolean fBlind)
        {
        checkEvict(oKey, true);

        return super.removeInternal(oKey, fBlind);
        }

    public LocalCache getLocalCache()
        {
        return (LocalCache) super.getInternalCache();
        }

    private void checkEvict(Object oKey, boolean fBefore)
        {
        Map     mapEvict = (Map) m_tloEvict.get();
        Boolean FEvict   = (Boolean) mapEvict.get(oKey);

        if (Base.equals(FEvict, Boolean.valueOf(fBefore)))
            {
            getLocalCache().evict(oKey);
            mapEvict.remove(oKey);
            }
        }

    private void checkEvictMap(Map<Object, Object> map, boolean fBefore)
        {
        for (Map.Entry entry : map.entrySet())
            {
            checkEvict(entry.getKey(), fBefore);
            }
        }

    public void forceEvict(InvocableMap.Entry entry, boolean fBefore)
        {
        BinaryEntry binEntry = (BinaryEntry) entry;

        ((Map) m_tloEvict.get()).put(binEntry.getBinaryKey(), Boolean.valueOf(fBefore));
        }

    protected final ThreadLocal m_tloEvict = new ThreadLocal()
        {
        @Override
        protected Object initialValue()
            {
            return new HashMap();
            }
        };
    }
