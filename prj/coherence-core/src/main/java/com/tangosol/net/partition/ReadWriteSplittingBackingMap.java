/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.tangosol.net.BackingMapManager;
import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.net.cache.BinaryEntryStore;
import com.tangosol.net.cache.CacheLoader;
import com.tangosol.net.cache.NonBlockingEntryStore;
import com.tangosol.net.cache.ReadWriteBackingMap;

import com.tangosol.util.ObservableMap;

import java.util.Iterator;
import java.util.Map;


/**
* A PartitionAwareBackingMap extension to the ReadWriteBackingMap.
*
* @since Coherence 3.5
* @author cp  2009-01-09
*/
public class ReadWriteSplittingBackingMap
        extends ReadWriteBackingMap
        implements PartitionAwareBackingMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a ReadWriteSplittingBackingMap based on a CacheStore.
    *
    * @param ctxService            the context provided by the CacheService
    *                              which is using this backing map
    * @param mapInternal           the PartitionAwareBackingMap used to store
    *                              the data internally in this backing map;
    *                              it must implement the ObservableMap
    *                              interface
    * @param mapMisses             the Map used to cache CacheStore misses
    *                              (optional)
    * @param loader                the CacheLoader responsible for the
    *                              persistence of the cached data (optional)
    * @param fReadOnly             pass true is the specified loader is in
    *                              fact a CacheStore that needs to be used
    *                              only for read operations; changes to the
    *                              cache will not be persisted
    * @param cWriteBehindSeconds   number of seconds to write if there is a
    *                              CacheStore; zero disables write-behind
    *                              caching, which (combined with !fReadOnly)
    *                              implies write-through
    * @param dflRefreshAheadFactor the interval before an entry expiration
    *                              time (expressed as a percentage of the
    *                              internal cache expiration interval) during
    *                              which an asynchronous load request for the
    *                              entry will be scheduled; zero disables
    *                              refresh-ahead; only applicable when
    *                              the <tt>mapInternal</tt> parameter is an
    *                              instance of
    *                              {@link com.tangosol.net.cache.LocalCache}
    */
    public ReadWriteSplittingBackingMap(
            BackingMapManagerContext ctxService,
            PartitionAwareBackingMap mapInternal,
            Map                      mapMisses,
            CacheLoader              loader,
            boolean                  fReadOnly,
            int                      cWriteBehindSeconds,
            double                   dflRefreshAheadFactor)
        {
        this(ctxService, mapInternal, mapMisses, loader, fReadOnly,
                cWriteBehindSeconds, dflRefreshAheadFactor, RWBM_WB_REMOVE_DEFAULT);
        }

    /**
    * Construct a ReadWriteSplittingBackingMap based on a CacheStore.
    *
    * @param ctxService            the context provided by the CacheService
    *                              which is using this backing map
    * @param mapInternal           the PartitionAwareBackingMap used to store
    *                              the data internally in this backing map;
    *                              it must implement the ObservableMap
    *                              interface
    * @param mapMisses             the Map used to cache CacheStore misses
    *                              (optional)
    * @param loader                the CacheLoader responsible for the
    *                              persistence of the cached data (optional)
    * @param fReadOnly             pass true is the specified loader is in
    *                              fact a CacheStore that needs to be used
    *                              only for read operations; changes to the
    *                              cache will not be persisted
    * @param cWriteBehindSeconds   number of seconds to write if there is a
    *                              CacheStore; zero disables write-behind
    *                              caching, which (combined with !fReadOnly)
    *                              implies write-through
    * @param dflRefreshAheadFactor the interval before an entry expiration
    *                              time (expressed as a percentage of the
    *                              internal cache expiration interval) during
    *                              which an asynchronous load request for the
    *                              entry will be scheduled; zero disables
    *                              refresh-ahead; only applicable when
    *                              the <tt>mapInternal</tt> parameter is an
    *                              instance of
    *                              {@link com.tangosol.net.cache.LocalCache}
    * @param fWriteBehindRemove    pass true if the specified loader is in fact
    *                              a CacheStore that needs to apply write-behind to remove
    *
    * @since 12.2.1.4.18
    */
    public ReadWriteSplittingBackingMap(
            BackingMapManagerContext ctxService,
            PartitionAwareBackingMap mapInternal,
            Map                      mapMisses,
            CacheLoader              loader,
            boolean                  fReadOnly,
            int                      cWriteBehindSeconds,
            double                   dflRefreshAheadFactor,
            boolean                  fWriteBehindRemove)
        {
        super(ctxService, (ObservableMap) mapInternal, mapMisses, loader,
                fReadOnly, cWriteBehindSeconds, dflRefreshAheadFactor, fWriteBehindRemove);
        }

    /**
    * Construct a ReadWriteSplittingBackingMap based on a BinaryEntryStore.
    *
    * @param ctxService            the context provided by the CacheService
    *                              which is using this backing map
    * @param mapInternal           the PartitionAwareBackingMap used to store
    *                              the data internally in this backing map;
    *                              it must implement the ObservableMap
    *                              interface
    * @param mapMisses             the Map used to cache CacheStore misses
    *                              (optional)
    * @param storeBinary                the BinaryEntryStore responsible for the
    *                              persistence of the cached data (optional)
    * @param fReadOnly             pass true is the specified loader is in
    *                              fact a CacheStore that needs to be used
    *                              only for read operations; changes to the
    *                              cache will not be persisted
    * @param cWriteBehindSeconds   number of seconds to write if there is a
    *                              CacheStore; zero disables write-behind
    *                              caching, which (combined with !fReadOnly)
    *                              implies write-through
    * @param dflRefreshAheadFactor the interval before an entry expiration
    *                              time (expressed as a percentage of the
    *                              internal cache expiration interval) during
    *                              which an asynchronous load request for the
    *                              entry will be scheduled; zero disables
    *                              refresh-ahead; only applicable when
    *                              the <tt>mapInternal</tt> parameter is an
    *                              instance of
    *                              {@link com.tangosol.net.cache.LocalCache}
    */
    public ReadWriteSplittingBackingMap(
            BackingMapManagerContext ctxService,
            PartitionAwareBackingMap mapInternal,
            Map                      mapMisses,
            BinaryEntryStore         storeBinary,
            boolean                  fReadOnly,
            int                      cWriteBehindSeconds,
            double                   dflRefreshAheadFactor)
        {
        this(ctxService, mapInternal, mapMisses, storeBinary,
                fReadOnly, cWriteBehindSeconds, dflRefreshAheadFactor, RWBM_WB_REMOVE_DEFAULT);
        }

    /**
    * Construct a ReadWriteSplittingBackingMap based on a BinaryEntryStore.
    *
    * @param ctxService            the context provided by the CacheService
    *                              which is using this backing map
    * @param mapInternal           the PartitionAwareBackingMap used to store
    *                              the data internally in this backing map;
    *                              it must implement the ObservableMap
    *                              interface
    * @param mapMisses             the Map used to cache CacheStore misses
    *                              (optional)
    * @param storeBinary                the BinaryEntryStore responsible for the
    *                              persistence of the cached data (optional)
    * @param fReadOnly             pass true is the specified loader is in
    *                              fact a CacheStore that needs to be used
    *                              only for read operations; changes to the
    *                              cache will not be persisted
    * @param cWriteBehindSeconds   number of seconds to write if there is a
    *                              CacheStore; zero disables write-behind
    *                              caching, which (combined with !fReadOnly)
    *                              implies write-through
    * @param dflRefreshAheadFactor the interval before an entry expiration
    *                              time (expressed as a percentage of the
    *                              internal cache expiration interval) during
    *                              which an asynchronous load request for the
    *                              entry will be scheduled; zero disables
    *                              refresh-ahead; only applicable when
    *                              the <tt>mapInternal</tt> parameter is an
    *                              instance of
    *                              {@link com.tangosol.net.cache.LocalCache}
    * @param fWriteBehindRemove    pass true if the specified loader is in fact
    *                              a CacheStore that needs to apply write-behind to remove
    *
    * @since 12.2.1.4.18
    */
    public ReadWriteSplittingBackingMap(
            BackingMapManagerContext ctxService,
            PartitionAwareBackingMap mapInternal,
            Map                      mapMisses,
            BinaryEntryStore         storeBinary,
            boolean                  fReadOnly,
            int                      cWriteBehindSeconds,
            double                   dflRefreshAheadFactor,
            boolean                  fWriteBehindRemove)
        {
        super(ctxService, (ObservableMap) mapInternal, mapMisses, storeBinary,
                fReadOnly, cWriteBehindSeconds, dflRefreshAheadFactor, fWriteBehindRemove);
        }


    /**
    * Construct a ReadWriteSplittingBackingMap based on a BinaryEntryStore.
    *
    * @param ctxService             the context provided by the CacheService
    *                               which is using this backing map
    * @param mapInternal            the PartitionAwareBackingMap used to store
    *                               the data internally in this backing map;
    *                               it must implement the ObservableMap
    *                               interface
    * @param mapMisses              the Map used to cache CacheStore misses
    *                               (optional)
    * @param storeNonBlockingBinary the NonBlockingEntryStore responsible for
    *                               the persistence of the cached data
    *                               (optional)
    * @param fReadOnly              pass true is the specified loader is in
    *                               fact a CacheStore that needs to be used
    *                               only for read operations; changes to the
    *                               cache will not be persisted
    * @param cWriteBehindSeconds    number of seconds to write if there is a
    *                               CacheStore; zero disables write-behind
    *                               caching, which (combined with !fReadOnly)
    *                               implies write-through
    * @param dflRefreshAheadFactor  the interval before an entry expiration
    *                               time (expressed as a percentage of the
    *                               internal cache expiration interval) during
    *                               which an asynchronous load request for the
    *                               entry will be scheduled; zero disables
    *                               refresh-ahead; only applicable when
    *                               the <tt>mapInternal</tt> parameter is an
    *                               instance of
    *                               {@link com.tangosol.net.cache.LocalCache}
    */
    public ReadWriteSplittingBackingMap(
            BackingMapManagerContext ctxService,
            PartitionAwareBackingMap mapInternal,
            Map                      mapMisses,
            NonBlockingEntryStore    storeNonBlockingBinary,
            boolean                  fReadOnly,
            int                      cWriteBehindSeconds,
            double                   dflRefreshAheadFactor)
        {
            super(ctxService, (ObservableMap) mapInternal, mapMisses, storeNonBlockingBinary,
                    fReadOnly, cWriteBehindSeconds, dflRefreshAheadFactor, RWBM_WB_REMOVE_DEFAULT);
        }

    /**
    * Construct a ReadWriteSplittingBackingMap based on a BinaryEntryStore.
    *
    * @param ctxService             the context provided by the CacheService
    *                               which is using this backing map
    * @param mapInternal            the PartitionAwareBackingMap used to store
    *                               the data internally in this backing map;
    *                               it must implement the ObservableMap
    *                               interface
    * @param mapMisses              the Map used to cache CacheStore misses
    *                               (optional)
    * @param storeNonBlockingBinary the NonBlockingEntryStore responsible for
    *                               the persistence of the cached data
    *                               (optional)
    * @param fReadOnly              pass true is the specified loader is in
    *                               fact a CacheStore that needs to be used
    *                               only for read operations; changes to the
    *                               cache will not be persisted
    * @param cWriteBehindSeconds    number of seconds to write if there is a
    *                               CacheStore; zero disables write-behind
    *                               caching, which (combined with !fReadOnly)
    *                               implies write-through
    * @param dflRefreshAheadFactor  the interval before an entry expiration
    *                               time (expressed as a percentage of the
    *                               internal cache expiration interval) during
    *                               which an asynchronous load request for the
    *                               entry will be scheduled; zero disables
    *                               refresh-ahead; only applicable when
    *                               the <tt>mapInternal</tt> parameter is an
    *                               instance of
    *                               {@link com.tangosol.net.cache.LocalCache}
    * @param fWriteBehindRemove     pass true if the specified loader is in fact
    *                               a CacheStore that needs to apply write-behind to remove
    *
    * @since 12.2.1.4.18
    */
    public ReadWriteSplittingBackingMap(
            BackingMapManagerContext ctxService,
            PartitionAwareBackingMap mapInternal,
            Map                      mapMisses,
            NonBlockingEntryStore    storeNonBlockingBinary,
            boolean                  fReadOnly,
            int                      cWriteBehindSeconds,
            double                   dflRefreshAheadFactor,
            boolean                  fWriteBehindRemove)
        {
            super(ctxService, (ObservableMap) mapInternal, mapMisses, storeNonBlockingBinary,
                    fReadOnly, cWriteBehindSeconds, dflRefreshAheadFactor, fWriteBehindRemove);
        }


    // ----- PartitionAwareBackingMap interface -----------------------------

    /**
    * {@inheritDoc}
    */
    public BackingMapManager getBackingMapManager()
        {
        return getPartitionAwareBackingMap().getBackingMapManager();
        }

    /**
    * {@inheritDoc}
    */
    public String getName()
        {
        return getPartitionAwareBackingMap().getName();
        }

    /**
    * {@inheritDoc}
    */
    public void createPartition(int nPid)
        {
        getPartitionAwareBackingMap().createPartition(nPid);
        }

    /**
    * {@inheritDoc}
    */
    public void destroyPartition(int nPid)
        {
        getPartitionAwareBackingMap().destroyPartition(nPid);

        // discard all of the keys that are in the misses cache for the
        // partition that is being destroyed
        Map mapMisses = getMissesCache();
        if (mapMisses != null)
            {
            BackingMapManagerContext ctx = getContext();
            for (Iterator iter = mapMisses.keySet().iterator(); iter.hasNext(); )
                {
                if (ctx.getKeyPartition(iter.next()) == nPid)
                    {
                    iter.remove();
                    }
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public Map getPartitionMap(int nPid)
        {
        return getPartitionAwareBackingMap().getPartitionMap(nPid);
        }

    /**
    * {@inheritDoc}
    */
    public Map getPartitionMap(PartitionSet partitions)
        {
        return getPartitionAwareBackingMap().getPartitionMap(partitions);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the PartitionAwareBackingMap that this ReadWriteBackingMap
    * uses as its backing map.
    *
    * @return the internal cache as a PartitionAwareBackingMap
    */
    public PartitionAwareBackingMap getPartitionAwareBackingMap()
        {
        return (PartitionAwareBackingMap) getInternalCache();
        }
    }