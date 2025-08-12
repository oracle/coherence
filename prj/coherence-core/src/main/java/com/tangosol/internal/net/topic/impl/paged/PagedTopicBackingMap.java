/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.net.cache.ConfigurableCacheMap;

import com.tangosol.util.ObservableMap;

import com.tangosol.util.WrapperObservableMap;

import java.util.Collection;

/**
 * A base class for backing map implementation used by
 * paged topic caches.
 *
 * @author Jonathan Knight 2022.08.11
 */
@SuppressWarnings("rawtypes")
public abstract class PagedTopicBackingMap
        extends WrapperObservableMap
        implements ConfigurableCacheMap
    {
    /**
     * Create a {@link PagedTopicBackingMap}.
     *
     * @param map  the backing map to delegate to
     */
    @SuppressWarnings("unchecked")
    protected PagedTopicBackingMap(ObservableMap map)
        {
        super(map);
        if (!(map instanceof ConfigurableCacheMap))
            {
            throw new IllegalArgumentException("map must implement ConfigurableCacheMap");
            }
        }

    /**
     * Return the wrapped {@link ConfigurableCacheMap}.
     *
     * @return the wrapped {@link ConfigurableCacheMap}
     */
    protected ConfigurableCacheMap getConfigurableCacheMap()
        {
        return (ConfigurableCacheMap) getMap();
        }

    @Override
    public int getUnits()
        {
        return getConfigurableCacheMap().getUnits();
        }

    @Override
    public int getHighUnits()
        {
        return getConfigurableCacheMap().getHighUnits();
        }

    @Override
    public void setHighUnits(int cMax)
        {
        getConfigurableCacheMap().setHighUnits(cMax);
        }

    @Override
    public int getLowUnits()
        {
        return getConfigurableCacheMap().getLowUnits();
        }

    @Override
    public void setLowUnits(int cUnits)
        {
        getConfigurableCacheMap().setLowUnits(cUnits);
        }

    @Override
    public int getUnitFactor()
        {
        return getConfigurableCacheMap().getUnitFactor();
        }

    @Override
    public void setUnitFactor(int nFactor)
        {
        getConfigurableCacheMap().setUnitFactor(nFactor);
        }

    @Override
    public void evict(Object oKey)
        {
        getConfigurableCacheMap().evict(oKey);
        }

    @Override
    public void evictAll(Collection colKeys)
        {
        getConfigurableCacheMap().evictAll(colKeys);
        }

    @Override
    public void evict()
        {
        getConfigurableCacheMap().evict();
        }

    @Override
    public EvictionApprover getEvictionApprover()
        {
        return getConfigurableCacheMap().getEvictionApprover();
        }

    @Override
    public void setEvictionApprover(EvictionApprover approver)
        {
        getConfigurableCacheMap().setEvictionApprover(approver);
        }

    @Override
    public int getExpiryDelay()
        {
        return getConfigurableCacheMap().getExpiryDelay();
        }

    @Override
    public void setExpiryDelay(int cMillis)
        {
        getConfigurableCacheMap().setExpiryDelay(cMillis);
        }

    @Override
    public long getNextExpiryTime()
        {
        return getConfigurableCacheMap().getNextExpiryTime();
        }

    @Override
    public ConfigurableCacheMap.Entry getCacheEntry(Object oKey)
        {
        return getConfigurableCacheMap().getCacheEntry(oKey);
        }

    @Override
    public EvictionPolicy getEvictionPolicy()
        {
        return getConfigurableCacheMap().getEvictionPolicy();
        }

    @Override
    public void setEvictionPolicy(EvictionPolicy policy)
        {
        getConfigurableCacheMap().setEvictionPolicy(policy);
        }

    @Override
    public UnitCalculator getUnitCalculator()
        {
        return getConfigurableCacheMap().getUnitCalculator();
        }

    @Override
    public void setUnitCalculator(UnitCalculator calculator)
        {
        getConfigurableCacheMap().setUnitCalculator(calculator);
        }

    @Override
    @SuppressWarnings("unchecked")
    public Object put(Object key, Object value, long cMillis)
        {
        return getConfigurableCacheMap().put(key,value,cMillis);
        }
    }
