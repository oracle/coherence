/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.tangosol.net.BackingMapManager;

import com.tangosol.net.cache.ConfigurableCacheMap;

import com.tangosol.util.AbstractKeyBasedMap;
import com.tangosol.util.AbstractKeySetBasedMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.abs;
import static java.lang.Math.min;

/**
* The ObservableSplittingBackingCache is an implementation of the
* ConfigurableCacheMap interface that works as an observable backing map
* in a partitioned system. In other words, it acts like a
* {@link com.tangosol.net.cache.LocalCache LocalCache}, but it internally
* partitions its data across any number of caches that implement the
* ConfigurableCacheMap interface. Note that the underlying backing maps must
* implement the ConfigurableCacheMap interface or a runtime exception will
* occur.
*
* @author cp  2009.01.16
*/
public class ObservableSplittingBackingCache
        extends ObservableSplittingBackingMap
        implements ConfigurableCacheMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a ObservableSplittingBackingCache that adds ConfigurableCacheMap
    * functionality to an ObservableSplittingBackingMap.
    *
    * @param bmm    a callback that knows how to create and release the
    *               backing maps that this ObservableSplittingBackingCache is
    *               responsible for
    * @param sName  the cache name for which this backing map exists
    */
    public ObservableSplittingBackingCache(BackingMapManager bmm, String sName)
        {
        super(new CapacityAwareMap(bmm, sName));

        ((CapacityAwareMap) getPartitionSplittingBackingMap()).bind(this);

        initializeConfiguredProperties();
        }


    // ----- PartitionAwareBackingMap methods -------------------------------

    /**
    * {@inheritDoc}
    */
    public void createPartition(int nPid)
        {
        super.createPartition(nPid);
        Map map = super.getPartitionMap(nPid);
        if (map instanceof ConfigurableCacheMap)
            {
            // configure the new cache
            ConfigurableCacheMap cache = (ConfigurableCacheMap) map;

            int nUnitFactor = getPartitionUnitFactor();
            if (nUnitFactor != -1)
                {
                cache.setUnitFactor(nUnitFactor);
                }

            int cExpiryDelayMillis = m_cExpiryDelayMillis;
            if (cExpiryDelayMillis != -1)
                {
                cache.setExpiryDelay(cExpiryDelayMillis);
                }

            EvictionPolicy policy = m_policy;
            if (policy != null)
                {
                cache.setEvictionPolicy(policy);
                }

            UnitCalculator calculator = m_calculator;
            if (calculator != null)
                {
                cache.setUnitCalculator(calculator);
                }

            EvictionApprover approver = m_apprvrEvict;
            if (approver != null)
                {
                cache.setEvictionApprover(approver);
                }

            m_cHighUnitFairShare = calcUnitFairShare(getCalibratedHighUnits());
            m_cLowUnitFairShare  = calcUnitFairShare(getCalibratedLowUnits());

            claimUnused(cache);

            // reset the cache of CCMs
            m_acache = null;
            }
        else
            {
            super.destroyPartition(nPid);
            throw new IllegalStateException("Partition backing map "
                    + (map == null ? "is null" : map.getClass().getName()
                    + " does not implement ConfigurableCacheMap"));
            }
        }

    /**
    * {@inheritDoc}
    */
    public void destroyPartition(int nPid)
        {
        ConfigurableCacheMap mapInner   = (ConfigurableCacheMap) getPartitionSplittingBackingMap().getPartitionMap(nPid);
        int                  cHighUnits = mapInner == null ? 0 : mapInner.getHighUnits();
        super.destroyPartition(nPid);

        m_cHighUnitFairShare = calcUnitFairShare(getCalibratedHighUnits());
        m_cLowUnitFairShare  = calcUnitFairShare(getCalibratedLowUnits());
        if (cHighUnits > 0)
            {
            adjustUnits(cHighUnits);
            }

        // reset the cache of CCMs
        m_acache = null;
        }


    // ----- ConfigurableCacheMap interface ---------------------------------

    /**
    * {@inheritDoc}
    */
    public int getUnits()
        {
        ConfigurableCacheMap[] acache = getPartitionCacheArray();

        // get the actual units by summing up the backing maps
        int cUnits = 0;
        for (ConfigurableCacheMap cache : acache)
            {
            int cPartitionUnits = cache.getUnits();
            if (cPartitionUnits >= 0)
                {
                cUnits = Math.max(0, cUnits) +
                    (int) ((long) cPartitionUnits * getPartitionUnitFactor() / m_nUnitFactor);
                }
            }

        return cUnits;
        }

    /**
    * {@inheritDoc}
    */
    public int getHighUnits()
        {
        return m_cHighUnits;
        }

    /**
    * {@inheritDoc}
    */
    public void setHighUnits(int cMax)
        {
        if (cMax >= 0)
            {
            if (cMax == m_cHighUnits)
                {
                // the caller is requesting a prune
                for (Map map : getPartitionSplittingBackingMap().getMapArray().getBackingMaps())
                    {
                    ConfigurableCacheMap mapPart = (ConfigurableCacheMap) map;
                    mapPart.setHighUnits(mapPart.getHighUnits());
                    }
                }
            else
                {
                int cPrevHighUnits = getCalibratedHighUnits();
                if (m_cHighUnits >= 0)
                    {
                    m_cHighUnitsCalibrated = -1;
                    }

                m_cHighUnits = cMax;
                m_cHighUnitFairShare = calcUnitFairShare(getCalibratedHighUnits());

                // either disseminate the addition or removal of units to all child
                // maps; this method will also set the low units
                adjustUnits(getCalibratedHighUnits() - cPrevHighUnits);
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public int getLowUnits()
        {
        return m_cLowUnits;
        }

    /**
    * {@inheritDoc}
    */
    public void setLowUnits(int cUnits)
        {
        if (cUnits != m_cLowUnits && cUnits >= 0)
            {
            if (m_cLowUnits >= 0)
                {
                m_cLowUnitsCalibrated = -1;
                }

            m_cLowUnits = cUnits;
            m_cLowUnitFairShare = calcUnitFairShare(getCalibratedLowUnits());
            // adjust the low units on all partitioned maps without modification
            // to the high units
            adjustUnits(0);
            }
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
        if (nFactor != m_nUnitFactor && nFactor > 0)
            {
            if (nFactor < MAX_PARTITION_MAP_UNIT_FACTOR)
                {
                // update all the backing maps
                ConfigurableCacheMap[] amap = getPartitionCacheArray();
                for (int i = 0, c = amap.length; i < c; ++i)
                    {
                    amap[i].setUnitFactor(nFactor);
                    }
                }

            m_cHighUnits  = (m_cHighUnits * m_nUnitFactor) / nFactor;
            m_cLowUnits   = (m_cLowUnits  * m_nUnitFactor) / nFactor;
            m_nUnitFactor = nFactor;
            }
        }

    /**
    * {@inheritDoc}
    */
    public EvictionPolicy getEvictionPolicy()
        {
        return m_policy;
        }

    /**
    * {@inheritDoc}
    */
    public void setEvictionPolicy(EvictionPolicy policy)
        {
        if (policy != m_policy)
            {
            ConfigurableCacheMap[] amap = getPartitionCacheArray();
            for (int i = 0, c = amap.length; i < c; ++i)
                {
                amap[i].setEvictionPolicy(policy);
                }

            m_policy = policy;
            }
        }

    /**
    * {@inheritDoc}
    */
    public void evict(Object oKey)
        {
        getPartitionCache(oKey).evict(oKey);
        }

    /**
    * {@inheritDoc}
    */
    public void evictAll(Collection colKeys)
        {
        // note: this is not an optimal implementation
        for (Iterator iter = colKeys.iterator(); iter.hasNext(); )
            {
            evict(iter.next());
            }
        }

    /**
    * {@inheritDoc}
    */
    public void evict()
        {
        // request each of the backing maps to evict
        ConfigurableCacheMap[] amap = getPartitionCacheArray();
        for (int i = 0, c = amap.length; i < c; ++i)
            {
            amap[i].evict();
            }
        }

    /**
    * {@inheritDoc}
    */
    public EvictionApprover getEvictionApprover()
        {
        return m_apprvrEvict;
        }

    /**
    * {@inheritDoc}
    */
    public void setEvictionApprover(EvictionApprover approver)
        {
        if (approver != m_apprvrEvict)
            {
            ConfigurableCacheMap[] amap = getPartitionCacheArray();
            for (int i = 0, c = amap.length; i < c; ++i)
                {
                amap[i].setEvictionApprover(approver);
                }

            m_apprvrEvict = approver;
            }
        }

    /**
    * {@inheritDoc}
    */
    public int getExpiryDelay()
        {
        return m_cExpiryDelayMillis;
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void setExpiryDelay(int cMillis)
        {
        if (cMillis != m_cExpiryDelayMillis)
            {
            // update all the backing maps
            ConfigurableCacheMap[] amap = getPartitionCacheArray();
            for (int i = 0, c = amap.length; i < c; ++i)
                {
                amap[i].setExpiryDelay(cMillis);

                // with the first backing map, verify that it took the value
                // (some maps might not support expiry delay, so their delay
                // would be -1 and they may simply ignore the property set)
                if (i == 0 && amap[i].getExpiryDelay() != cMillis)
                    {
                    return;
                    }
                }

            m_cExpiryDelayMillis = cMillis;
            }
        }

    /**
     * {@inheritDoc}
     */
    public long getNextExpiryTime()
        {
        long ldtNext = Long.MAX_VALUE;

        ConfigurableCacheMap[] amap = getPartitionCacheArray();
        for (ConfigurableCacheMap map : amap)
            {
            long ldt = map.getNextExpiryTime();
            if (ldt > 0)
                {
                ldtNext = Math.min(ldtNext, ldt);
                }
            }

        return ldtNext == Long.MAX_VALUE ? 0 : ldtNext;
        }

    /**
    * {@inheritDoc}
    */
    public ConfigurableCacheMap.Entry getCacheEntry(Object oKey)
        {
        EntrySet.Entry entry = null;

        // first get the backing map for the partition that contains the key
        ConfigurableCacheMap cache = getPartitionCache(oKey);

        // ask the backing map for the entry related to the key (it might
        // not exist)
        ConfigurableCacheMap.Entry entryReal = cache.getCacheEntry(oKey);
        if (entryReal != null)
            {
            // since the real entry exists, create a veneer entry that
            // will pipe the Map.Entry updates through the
            // ObservableSplittingBackingCache but the other work
            // (expiry, touches, etc.) directly to the real entry
            entry = (EntrySet.Entry) ((EntrySet) entrySet())
                    .instantiateEntry(oKey, null);
            entry.setCacheEntry(entryReal);
            }

        return entry;
        }

    /**
    * {@inheritDoc}
    */
    public UnitCalculator getUnitCalculator()
        {
        return m_calculator;
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void setUnitCalculator(UnitCalculator calculator)
        {
        if (calculator != m_calculator)
            {
            ConfigurableCacheMap[] amap = getPartitionCacheArray();
            for (int i = 0, c = amap.length; i < c; ++i)
                {
                amap[i].setUnitCalculator(calculator);
                }

            m_calculator = calculator;
            }
        }

    /**
    * Return the uniform type used by each partition map.
    *
    * @return the type of partition map
    */
    public Class getPartitionMapType()
        {
        return m_clzPartitionMap;
        }


    // ----- ObservableSplittingBackingMap methods --------------------------

    /**
    * {@inheritDoc}
    * <p>
    * This implementation will check if mapPart is under-allocated for high
    * units. If this is the case, demand the entitled unit amount from other
    * maps.
    */
    protected void prepareUpdate(Map mapPartition, Map mapUpdate)
        {
        ConfigurableCacheMap mapPart    = (ConfigurableCacheMap) mapPartition;
        int                  cHighUnits = mapPart.getHighUnits();
        UnitCalculator       unitCalc   = mapPart.getUnitCalculator();

        if (unitCalc != null && cHighUnits < getHighUnitFairShare())
            {
            // the high units value for this map are under-allocated, thus
            // demand more if required
            int cUnits    = mapPart.getUnits();
            int cUnitsNew = 0;
            for (Map.Entry entry : (Set<Map.Entry>) mapUpdate.entrySet())
                {
                // determine whether this store will cause the map to exceed its
                // high unit allocation; to avoid an extra entry lookup we are
                // being aggressive on the unit calculation, always assuming an insert
                cUnitsNew += unitCalc.calculateUnits(entry.getKey(), entry.getValue());
                if (cUnitsNew + cUnits > cHighUnits)
                    {
                    claimAll(mapPart);
                    break;
                    }
                }
            }
        }


    // ----- unit partitioning methods --------------------------------------

    // Internal Notes:
    // Partitioning of units is implemented using a lazy credit based algorithm.
    // Units are repartitioned in an effort to reduce cost to other maps with
    // this map (OSBC) being able to determine whether maps are "overdrawn"
    // based on their current high-unit allocation and the fair-share.
    // The algorithm has two distinct phases with the pivot of each request being
    // the ConfigurableCacheMap in question:
    //     - claimUnused  claim as many unused units from all other partitioned
    //                    maps, i.e. maintaining the constraint
    //                    used-units < high-units && high-units >= fair-share.
    //     - claimAll     a map may demand its fair share of units causing overdrawn
    //                    maps to reduce their high-units to the fair share thus
    //                    being likely to enact eviction. Typically this process
    //                    commences when a put request would cause the map to surpass
    //                    its under-allocated high-units.
    // In addition to these two requests is the ability to adjust the high-units
    // due to more units being made available, either by an adjustment to high
    // or low units or the removal of a partitioned map; see adjustUnits.

    /**
    * Claim as many units as possible from existing maps without causing
    * over-allocated maps to evict.
    *
    * @param mapRequestor  the map requesting its fair share of units
    */
    protected void claimUnused(ConfigurableCacheMap mapRequestor)
        {
        Map[]   aMaps        = getPartitionSplittingBackingMap().getMapArray().getBackingMaps();
        int     cFairHigh    = getHighUnitFairShare();
        int     cFairLow     = getLowUnitFairShare();
        int     cUnits       = aMaps.length > 1 ? 0 : cFairHigh;
        boolean fSetLowUnits = cFairLow >= 0;

        for (int i = 0, c = aMaps.length; i < c; ++i)
           {
           ConfigurableCacheMap mapPart = (ConfigurableCacheMap) aMaps[i];
            if (mapPart == mapRequestor)
                {
                continue;
                }

            int cUnitsCurr = mapPart.getUnits();
            int cUnitsMax  = mapPart.getHighUnits();
            int cUnitsOver = min(cUnitsMax - cUnitsCurr, cUnitsMax - cFairHigh);

            if (cUnitsOver > 0)
                {
                // protect against concurrent setting of high units for the same
                // map; a worker thread may be adjusting the high units the same
                // time as the service thread
                synchronized (mapPart)
                    {
                    cUnitsCurr = mapPart.getUnits();
                    cUnitsMax  = mapPart.getHighUnits();
                    cUnitsOver = min(cUnitsMax - cUnitsCurr, cUnitsMax - cFairHigh);

                    if (cUnitsOver > 0)
                        {
                        mapPart.setHighUnits(cUnitsMax - cUnitsOver);
                        cUnits += cUnitsOver;
                        }
                    }
                }
            // low units must be set after setting the high units
            if (fSetLowUnits)
                {
                mapPart.setLowUnits(cFairLow);
                }
            }

        mapRequestor.setHighUnits(cUnits);
        if (fSetLowUnits)
            {
            mapRequestor.setLowUnits(cFairLow);
            }
        }

    /**
    * Claim the full entitlement of units for mapRequestor. This method
    * should only be invoked if the map has insufficient units, based on a
    * pending put request, however does not occupy its fair share.
    *
    * @param mapRequestor  the map demanding its entitled fair share
    */
    protected void claimAll(ConfigurableCacheMap mapRequestor)
        {
        int cFairHigh      = getHighUnitFairShare();
        int cUnitsMax      = mapRequestor.getHighUnits();
        int cUnitsEntitled = cFairHigh - cUnitsMax;
        int cUnitsClaimed  = 0;

        Map[] aMaps = getPartitionSplittingBackingMap().getMapArray().getBackingMaps();
        for (int i = 0, c = aMaps.length; cUnitsClaimed < cUnitsEntitled && i < c; ++i)
            {
            ConfigurableCacheMap mapPart = (ConfigurableCacheMap) aMaps[i];
            if (mapPart == mapRequestor)
                {
                continue;
                }

            int cUnitsDebit = mapPart.getHighUnits() - cFairHigh;
            if (cUnitsDebit > 0)
                {
                // we must double check due to demand being driven by put requests
                // which can be executed on multiple threads
                synchronized (mapPart)
                    {
                    cUnitsDebit = mapPart.getHighUnits() - cFairHigh;
                    if (cUnitsDebit > 0)
                        {
                        mapPart.setHighUnits(cFairHigh);
                        }
                    }
                cUnitsClaimed += cUnitsDebit;
                }
            }

        if (cUnitsClaimed > 0)
            {
            mapRequestor.setHighUnits(cUnitsMax + cUnitsClaimed);
            }
        }

    /**
    * Adjust the number of units for each map with the pool of units provided.
    * The units provided is a total across all maps with the adjustment per
    * map being made as prescribed below.
    * <p>
    * The provided units may be positive or negative, with the latter suggesting
    * that this number of units should be retrieved, thus decremented, from
    * child maps. If the provided units is positive, maps that are over-allocated
    * may consumer the provided amount. If the provided units is negative
    * maintain a
    * positive delta between the fair share and their current high units may
    * consume the minimum between what is available from the provided units
    * and the determined delta. If the provided units is negative, those maps
    * whose current high unit allocation is more than the fair share will have
    * this delta decremented until the debt (cUnits) is reclaimed.
    *
    * @param cUnits  the number of units to either disseminate (positive unit value)
    *                or retract across the maps
    */
    protected void adjustUnits(int cUnits)
        {
        int cFairHigh = getHighUnitFairShare();
        int cFairLow  = getLowUnitFairShare();
        int nSign     = cUnits < 0 ? -1 : 1;

        Map[] aMaps = getPartitionSplittingBackingMap().getMapArray().getBackingMaps();
        for (int i = 0, c = aMaps.length; i < c; ++i)
            {
            ConfigurableCacheMap mapPart = (ConfigurableCacheMap) aMaps[i];

            // determine the delta between the fair share and the consumers
            // current allocation
            int cMaxUnits   = mapPart.getHighUnits();
            int cUnitsDelta = cFairHigh - cMaxUnits;

            // if adjusting with positive units, only adjust maps that are not
            // "overdrawn" based on the current fair share; in the negative case,
            // only adjust maps that are over the fair share
            if (nSign ==  1 && cUnits > 0 && cUnitsDelta >=0 ||
                nSign == -1 && cUnits < 0 && cUnitsDelta < 0)
                {
                // protect against concurrent setting of high units for the same
                // map; setHighUnits could be called by any thread (mgmt server)
                // or service thread (destroyPartition)
                synchronized (mapPart)
                    {
                    cMaxUnits   = mapPart.getHighUnits();
                    cUnitsDelta = cFairHigh - cMaxUnits;
                    if (nSign ==  1 && cUnits > 0 && cUnitsDelta >= 0 ||
                        nSign == -1 && cUnits < 0 && cUnitsDelta <  0)
                        {
                        int cUnitsAdjust = min(abs(cUnitsDelta), abs(cUnits)) * nSign;
                        mapPart.setHighUnits(cMaxUnits + cUnitsAdjust);
                        cUnits -= cUnitsAdjust;
                        }
                    }
                }
            // changing high units may force an adjustment of low units, so
            // set those as well
            if (cFairLow >= 0)
                {
                mapPart.setLowUnits(cFairLow);
                }
            }
        }


    // ----- internal -------------------------------------------------------

    /**
    * Initialize the configurable properties of this cache-map.
    */
    protected void initializeConfiguredProperties()
        {
        // knowledge of certain CCM configuration parameters is known only to
        // the partition-map BMM and not directly injected into the parent map;
        // create a "dummy" map solely for the purpose of retrieving them
        BackingMapManager bmm   = getBackingMapManager();
        String            sName = getName() + "$synthetic";
        Map               map   = bmm.instantiateBackingMap(sName);

        if (map instanceof ConfigurableCacheMap)
            {
            ConfigurableCacheMap ccm = (ConfigurableCacheMap) map;

            setEvictionPolicy(ccm.getEvictionPolicy());
            setExpiryDelay   (ccm.getExpiryDelay());
            setUnitCalculator(ccm.getUnitCalculator());
            setUnitFactor    (ccm.getUnitFactor());
            setHighUnits     (ccm.getHighUnits());
            setLowUnits      (ccm.getLowUnits());
            }
        else
            {
            throw new IllegalStateException("Partition backing map "
                    + (map == null ? "is null" : map.getClass().getName()
                    + " does not implement ConfigurableCacheMap"));
            }
        m_clzPartitionMap = map.getClass();

        bmm.releaseBackingMap(sName, map);
        }

    /**
    * Obtain the backing cache for a particular key. If the key is not owned
    * by a partition represented by this ObservableSplittingBackingCache,
    * then a runtime exception is thrown.
    *
    * @param oKey  the key of the desired entry
    *
    * @return the backing cache
    */
    protected ConfigurableCacheMap getPartitionCache(Object oKey)
        {
        return (ConfigurableCacheMap) getPartitionSplittingBackingMap().getBackingMap(oKey);
        }

    /**
    * Obtain the array of backing caches.
    *
    * @return an array of all the per-partition backing caches
    */
    protected ConfigurableCacheMap[] getPartitionCacheArray()
        {
        ConfigurableCacheMap[] acache = m_acache;
        if (acache == null)
            {
            Map[] amap  = getPartitionSplittingBackingMap().getMapArray().getBackingMaps();
            int   cMaps = amap.length;

            acache = new ConfigurableCacheMap[cMaps];
            for (int i = 0; i <cMaps; ++i)
                {
                acache[i] = (ConfigurableCacheMap) amap[i];
                }
            m_acache = acache;
            }

        return acache;
        }

    /**
    * Determine the high units adjusted based on the
    * {@link #getPartitionUnitFactor partition unit factor}.
    *
    * @return the limit of the cache size in units adjusted by partition unitFactor
    */
    protected int getCalibratedHighUnits()
        {
        int cUnits = m_cHighUnitsCalibrated;
        if (cUnits > 0)
            {
            return cUnits;
            }

        cUnits = m_cHighUnits;
        if (cUnits < 0)
            {
            return -1;
            }

        return m_cHighUnitsCalibrated = (int) ((long) cUnits * m_nUnitFactor / getPartitionUnitFactor());
        }

    /**
    * Determine the low units adjusted based on the
    * {@link #getPartitionUnitFactor partition unit factor}.
    *
    * @return the number of units adjusted by partition unitFactor that the cache prunes to
    */
    protected int getCalibratedLowUnits()
        {
        int cUnits = m_cLowUnitsCalibrated;
        if (cUnits > 0)
            {
            return cUnits;
            }

        cUnits = m_cLowUnits;
        if (cUnits < 0)
            {
            return -1;
            }

        return m_cLowUnitsCalibrated = (int) ((long) cUnits * m_nUnitFactor / getPartitionUnitFactor());
        }

    /**
    * Determine the unit factor for individual partition maps.
    *
    * @return the unit factor for partition maps
    */
    protected int getPartitionUnitFactor()
        {
        return Math.min(MAX_PARTITION_MAP_UNIT_FACTOR, m_nUnitFactor);
        }

    /**
    * Return the fair share of low-units per partition map.
    *
    * @return the fair share of low-units per partition map
    */
    protected int getLowUnitFairShare()
        {
        return m_cLowUnitFairShare;
        }

    /**
    * Return the fair share of high-units per partition map.
    *
    * @return the fair share of high-units per partition map
    */
    protected int getHighUnitFairShare()
        {
        return m_cHighUnitFairShare;
        }

    /**
    * Return the fair share of units for each child maps. The units provided
    * would typically be either the high or low units allowed for all maps
    * under this map.
    *
    * @param cUnits  the units to distribute across all child maps
    *
    * @return the fair share of units for each child map
    */
    protected int calcUnitFairShare(int cUnits)
        {
        int cConsumers = Math.max(getPartitionSplittingBackingMap().getMapArray().getBackingMaps().length, 1);

        // Note: we do not round up, as was done previously, as the unit allocation
        //       algorithm extracts from the pool of units shared across partition
        //       maps. Consider having 10 units with 3 maps; previously we would
        //       allocate each map 4 units thus put us 2 units over quota {4,4,4},
        //       the current algorithm determines the fair share to be 3 units
        //       each and one map will hold the remainder {3,3,4}; see claimUnused
        return cUnits < 0 ? -1 : cUnits / cConsumers;
        }


    // ----- inner class: EntrySet ------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected Set instantiateEntrySet()
        {
        return new EntrySet();
        }

    /**
    * A set of ConfigurableCacheMap entries backed by this map.
    */
    public class EntrySet
            extends AbstractKeySetBasedMap.EntrySet
        {
        // ----- inner class: Entry -------------------------------------

        /**
        * {@inheritDoc}
        */
        protected Map.Entry instantiateEntry(Object oKey, Object oValue)
            {
            return new Entry(oKey, oValue);
            }

        /**
        * A Cache Entry implementation.
        */
        public class Entry
                extends AbstractKeyBasedMap.EntrySet.Entry
                implements ConfigurableCacheMap.Entry
            {
            /**
            * Construct an Entry.
            *
            * @param oKey    the Entry key
            * @param oValue  the Entry value (optional)
            */
            public Entry(Object oKey, Object oValue)
                {
                super(oKey, oValue);
                }

            /**
            * {@inheritDoc}
            */
            public Object getValue()
                {
                return getCacheEntry().getValue();
                }

            /**
            * {@inheritDoc}
            */
            public void touch()
                {
                getCacheEntry().touch();
                }

            /**
            * {@inheritDoc}
            */
            public int getTouchCount()
                {
                return getCacheEntry().getTouchCount();
                }

            /**
            * {@inheritDoc}
            */
            public long getLastTouchMillis()
                {
                return getCacheEntry().getLastTouchMillis();
                }

            /**
            * {@inheritDoc}
            */
            public long getExpiryMillis()
                {
                return getCacheEntry().getExpiryMillis();
                }

            /**
            * {@inheritDoc}
            */
            public void setExpiryMillis(long lMillis)
                {
                getCacheEntry().setExpiryMillis(lMillis);
                }

            /**
            * {@inheritDoc}
            */
            public int getUnits()
                {
                return getCacheEntry().getUnits();
                }

            /**
            * {@inheritDoc}
            */
            public void setUnits(int cUnits)
                {
                getCacheEntry().setUnits(cUnits);
                }

            /**
            * Configure the backing map cache entry.
            *
            * @param entryBacking  the entry to delegate most of this entry's
            *                      operations to
            */
            protected void setCacheEntry(ConfigurableCacheMap.Entry entryBacking)
                {
                m_entryBacking = entryBacking;
                }

            /**
            * Obtain the actual cache entry from the partition-specific
            * backing map.
            *
            * @return the actual underlying cache entry
            */
            protected ConfigurableCacheMap.Entry getCacheEntry()
                {
                ConfigurableCacheMap.Entry entry = m_entryBacking;
                if (entry == null)
                    {
                    Object oKey = getKey();
                    m_entryBacking = entry = getPartitionCache(oKey).getCacheEntry(oKey);
                    }

                return entry;
                }

            /**
            * The actual cache entry from the partition-specific backing map.
            */
            ConfigurableCacheMap.Entry m_entryBacking;
            }
        }


    // ----- inner class: CapacityAwareMap ----------------------------------

    /**
    * A subclass of PartitionSplittingBackingMap which allows an injected instance
    * of {@link ObservableSplittingBackingMap} to be called immediately before
    * inserting a value(s) in a partition map.
    * <p>
    * This class is intended for internal use only facilitating efficient use
    * of PartitionSplittingBackingMap, by reducing the number of times the partitioned
    * backing map is determined.
    *
    * @see ObservableSplittingBackingCache#prepareUpdate
    */
    protected static class CapacityAwareMap
            extends PartitionSplittingBackingMap
        {
        // ----- constructors -----------------------------------------------

        /**
        * Create a CapacityAwareMap.
        *
        * @param bmm    a BackingMapManager that knows how to create and release
        *               the backing maps that this PartitionSplittingBackingMap is
        *               responsible for
        * @param sName  the cache name for which this backing map exists
        */
        protected CapacityAwareMap(BackingMapManager bmm, String sName)
            {
            super(bmm, sName);
            }

        // ----- PartitionSplittingBackingMap methods -----------------------

        /**
        * {@inheritDoc}
        */
        @Override
        protected Object putInternal(Map mapPart, Object oKey, Object oValue)
            {
            m_mapOuter.prepareUpdate(mapPart, Collections.singletonMap(oKey, oValue));

            return super.putInternal(mapPart, oKey, oValue);
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void putAllInternal(Map mapPart, Map map)
            {
            m_mapOuter.prepareUpdate(mapPart, map);

            super.putAllInternal(mapPart, map);
            }


        // ----- helpers ----------------------------------------------------

        /**
        * Bind to the given {@link ObservableSplittingBackingMap} instance.
        * This instance will have {@link ObservableSplittingBackingCache#prepareUpdate
        * prepareUpdate} invoked immediately prior to an insert to a partition
        * map.
        *
        * @param mapOuter  the map used to call {@link ObservableSplittingBackingCache#prepareUpdate
        *                  prepareUpdate}
        */
        protected void bind(ObservableSplittingBackingCache mapOuter)
            {
            m_mapOuter = mapOuter;
            }

        // ----- data members -----------------------------------------------

        /**
        * The ObservableSplittingBackingMap used to call prepareUpdate.
        */
        protected ObservableSplittingBackingCache m_mapOuter;
        }


    // ----- constants ------------------------------------------------------

    /**
    * The maximum unit factor for partition maps.
    */
    protected static final int MAX_PARTITION_MAP_UNIT_FACTOR = 1024;


    // ----- data members ---------------------------------------------------

    /**
    * High units is the number of units that triggers eviction. The value -1
    * indicates that this cache has not been instructed to override the high
    * units of the underlying caches.
    */
    protected int m_cHighUnits = -1;

    /**
    * Low units is the number of units to evict down to. The value -1
    * indicates that this cache has not been instructed to override the low
    * units of the underlying caches.
    */
    protected int m_cLowUnits = -1;

    /**
    * The high units adjusted based on the {@link #getPartitionUnitFactor partition unit factor}.
    */
    protected int m_cHighUnitsCalibrated = -1;

    /**
    * The low units adjusted based on the {@link #getPartitionUnitFactor partition unit factor}.
    */
    protected int m_cLowUnitsCalibrated = -1;

    /**
    * The unit factor. The value -1 indicates that this cache has not been
    *instructed to override the unit factor of the underlying caches.
    */
    protected int m_nUnitFactor = -1;

    /**
    * The expiry delay. The value -1 indicates that this cache has not been
    * instructed to override the expiry delay of the underlying caches.
    */
    protected int m_cExpiryDelayMillis = -1;

    /**
    * The fair share of high units for each partition map.
    */
    protected int m_cHighUnitFairShare;

    /**
    * The fair share of low units for each partition map.
    */
    protected int m_cLowUnitFairShare;

    /**
    * The eviction policy. The value of null indicates that this cache has
    * not been instructed to override the eviction policy of the underlying
    * caches.
    */
    protected EvictionPolicy m_policy;

    /**
    * The unit calculator. The value of null indicates that this cache has
    * not been instructed to override the unit calculator of the underlying
    * caches.
    */
    protected UnitCalculator m_calculator;

    /**
    * A cached array of the backing ConfigurableCacheMap instances.
    */
    protected ConfigurableCacheMap[] m_acache;

    /**
    * An optional EvictionApprover registered with this cache.
    */
    protected EvictionApprover m_apprvrEvict;

    /*
    * The uniform type used by each partition map.
    */
    protected Class m_clzPartitionMap;
    }
