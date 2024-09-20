/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.common.util.Duration.Magnitude;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.unit.Millis;
import com.tangosol.coherence.config.unit.Seconds;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.io.ClassLoaderAware;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.BinaryEntryStore;
import com.tangosol.net.cache.CacheLoader;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.NonBlockingEntryStore;
import com.tangosol.net.cache.ReadWriteBackingMap;
import com.tangosol.net.partition.PartitionAwareBackingMap;
import com.tangosol.net.partition.ReadWriteSplittingBackingMap;

import com.tangosol.util.Base;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ObservableMap;

import java.util.Map;

import static com.tangosol.net.cache.ReadWriteBackingMap.RWBM_WB_REMOVE_DEFAULT;

/**
 * The {@link RemoteCacheScheme} is responsible for creating a fully
 * configured ReadWriteBackingMap.  The  setters are annotated so that CODI
 * can automatically configure the builder  After the builder is configured,
 * the realize method can be called to create either a custom ReadWriteBackingMap
 * or the internal Coherence ReadWriteBackingMap.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public class ReadWriteBackingMapScheme
        extends AbstractLocalCachingScheme<ReadWriteBackingMap>
    {
    // ----- MapBuilder interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadWriteBackingMap realizeMap(ParameterResolver resolver, Dependencies dependencies)
        {
        validate(resolver);

        ReadWriteBackingMap                       rwbm            = null;
        ClassLoader                               loader          = dependencies.getClassLoader();
        CacheStoreScheme                          bldrCacheStore  = getCacheStoreScheme();
        MapBuilder                                bldrInternalMap = getInternalScheme();
        LocalScheme                               bldrMissCache   = getMissCacheScheme();
        ObservableMap                             mapInternal     = getInternalMap();
        BackingMapManagerContext                  contextBmm      = dependencies.getBackingMapManagerContext();
        ParameterizedBuilder<ReadWriteBackingMap> bldrCustom      = getCustomBuilder();
        boolean                                   fReadOnly       = isReadOnly(resolver);
        boolean                                   fSplitting      = mapInternal instanceof PartitionAwareBackingMap;
        boolean                                   fWBRemove       = isWriteBehindRemove(resolver);

        // create the internal map if it hasn't been set already.  For example,
        // for partitioned backing maps, the BackingMapManager will set the
        // internal map.
        if (mapInternal == null)
            {
            mapInternal = bldrInternalMap == null
                          ? null : (ObservableMap) bldrInternalMap.realizeMap(resolver, dependencies);
            }

        // create the miss cache
        LocalCache mapMisses = bldrMissCache == null
                               ? null : (LocalCache) bldrMissCache.realizeMap(resolver, dependencies);

        // create the cache store
        Object store = bldrCacheStore == null ? null : bldrCacheStore.realize(resolver, dependencies);

        // init the binary store variable
        BinaryEntryStore      storeBinary            = null;
        NonBlockingEntryStore storeNonBlockingBinary = null;

        if (store instanceof BinaryEntryStore || store instanceof NonBlockingEntryStore)
            {
            // If the store implements the BinaryEntryStore interface, use it.
            // The only exception from that rule is the SCHEME_REMOTE_CACHE case,
            // (which always returns the SafeNamedCache), that was de-optimized
            // due to the Serializers incompatibility
            if (!(store instanceof NamedCache && store instanceof ClassLoaderAware
                && ((ClassLoaderAware) store).getContextClassLoader() != NullImplementation.getClassLoader()))
                {
                if (store instanceof BinaryEntryStore)
                    {
                    storeBinary = (BinaryEntryStore) store;
                    }
                else
                    {
                    storeNonBlockingBinary = (NonBlockingEntryStore) store;
                    }
                }
            }

        // get the write behind delay; if the "write-delay" element exists, try
        // to parse it; otherwise, parse the "write-delay-seconds" element
        long cWriteBehindMillis = getWriteDelay(resolver).as(Magnitude.MILLI);

        if (cWriteBehindMillis == 0)
            {
            cWriteBehindMillis = 1000L * getWriteDelaySeconds(resolver);
            }

        int    cWriteBehindSec       = cWriteBehindMillis == 0 ? 0 : Math.max(1, (int) (cWriteBehindMillis / 1000));

        double dflRefreshAheadFactor = getRefreshAheadFactor(resolver);

        // create the internal ReadWriteBackingMap or a custom map.
        if (bldrCustom == null)
            {
            if (storeBinary == null && storeNonBlockingBinary == null)
                {
                CacheLoader storeObject = (CacheLoader) store;

                rwbm = fSplitting
                       ? instantiateReadWriteSplittingBackingMap(contextBmm, (PartitionAwareBackingMap) mapInternal,
                             mapMisses, storeObject, fReadOnly, cWriteBehindSec, dflRefreshAheadFactor, fWBRemove)
                       : instantiateReadWriteBackingMap(contextBmm, mapInternal, mapMisses, storeObject, fReadOnly,
                             cWriteBehindSec, dflRefreshAheadFactor, fWBRemove);
                }
            else if (storeNonBlockingBinary != null)
                {
                if (cWriteBehindSec != 0)
                    {
                    Base.log("Write-behind configured with a non-blocking store implementation. Disabling write-behind.");
                    cWriteBehindSec = 0;
                    }

                rwbm = fSplitting
                        ? instantiateReadWriteSplittingBackingMap(contextBmm, (PartitionAwareBackingMap) mapInternal,
                             mapMisses, storeNonBlockingBinary, fReadOnly, cWriteBehindSec, dflRefreshAheadFactor, fWBRemove)
                        : instantiateReadWriteBackingMap(contextBmm, mapInternal, mapMisses, storeNonBlockingBinary, fReadOnly,
                             cWriteBehindSec, dflRefreshAheadFactor, fWBRemove);
               }
            else
                {
                rwbm = fSplitting
                       ? instantiateReadWriteSplittingBackingMap(contextBmm, (PartitionAwareBackingMap) mapInternal,
                             mapMisses, storeBinary, fReadOnly, cWriteBehindSec, dflRefreshAheadFactor, fWBRemove)
                       : instantiateReadWriteBackingMap(contextBmm, mapInternal, mapMisses, storeBinary, fReadOnly,
                             cWriteBehindSec, dflRefreshAheadFactor, fWBRemove);
                }
            }
        else
            {
            ParameterList listArgs = new ResolvableParameterList();

            listArgs.add(new Parameter("contextBmm", contextBmm));
            listArgs.add(new Parameter("mapInternal", mapInternal));
            listArgs.add(new Parameter("mapMisses", mapMisses));
            listArgs.add(new Parameter("storeBinary", storeBinary == null ? store : storeBinary));
            listArgs.add(new Parameter("readOnly", Boolean.valueOf(fReadOnly)));
            listArgs.add(new Parameter("writeBehindSec", Integer.valueOf(cWriteBehindSec)));
            listArgs.add(new Parameter("refreshAheadFactory", Double.valueOf(dflRefreshAheadFactor)));
            if (fWBRemove)
                {
                listArgs.add(new Parameter("writeBehindRemove", Boolean.valueOf(fWBRemove)));
                }
            rwbm = bldrCustom.realize(resolver, loader, listArgs);
            }

        // Read/Write Threads will have the cache name appended to the thread name
        rwbm.setCacheName(dependencies.getCacheName());
        rwbm.setRethrowExceptions(isRollbackCacheStoreFailures(resolver));
        rwbm.setWriteBatchFactor(getWriteBatchFactor(resolver));
        rwbm.setWriteRequeueThreshold(getWriteRequeueThreshold(resolver));
        rwbm.setWriteMaxBatchSize(getWriteMaxBatchSize(resolver));

        if (cWriteBehindMillis != 1000L * cWriteBehindSec)
            {
            rwbm.setWriteBehindMillis(cWriteBehindMillis);
            }

        rwbm.setCacheStoreTimeoutMillis(getCacheStoreTimeout(resolver).as(Magnitude.MILLI));

        BundleManager managerBundle = bldrCacheStore == null ? null : bldrCacheStore.getBundleManager();

        if (managerBundle != null)
            {
            managerBundle.ensureBundles(resolver, rwbm.getCacheStore());
            }

        return rwbm;
        }

    // ----- ObservableCachingScheme interface ------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void establishMapListeners(Map map, ParameterResolver resolver, Dependencies dependencies)
        {
        super.establishMapListeners(map, resolver, dependencies);

        if (getInternalScheme() instanceof ObservableCachingScheme && map instanceof ReadWriteBackingMap)
            {
            ((ObservableCachingScheme) getInternalScheme())
                .establishMapListeners(((ReadWriteBackingMap) map).getInternalCache(), resolver, dependencies);
            }

        }

    // ----- ReadWriteBackingMapScheme methods ------------------------------

    /**
     * Return the {@link CacheStoreScheme} used to create a CacheStore
     * or CacheLoader.
     *
     * @return the builder
     */
    public CacheStoreScheme getCacheStoreScheme()
        {
        return m_schemeCacheStore;
        }

    /**
     * Set the {@link CacheStoreScheme} builder.
     *
     * @param  bldr  the builder
     */
    @Injectable("cachestore-scheme")
    public void setCacheStoreScheme(CacheStoreScheme bldr)
        {
        m_schemeCacheStore = bldr;
        }

    /**
     * Return the timeout interval to use for CacheStore read and write
     * operations. If a CacheStore operation times out, the executing thread
     * is interrupted and may ultimately lead to the termination of the cache
     * service. Timeouts of asynchronous CacheStore operations (for example,
     * refresh-ahead, write-behind) do not result in service termination.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the timeout
     */
    public Millis getCacheStoreTimeout(ParameterResolver resolver)
        {
        return m_exprCacheStoreTimeout.evaluate(resolver);
        }

    /**
     * Set the timeout interval to use for CacheStore/CacheLoader read and
     * write operations.
     *
     * @param expr  the timeout interval expression
     */
    @Injectable("cachestore-timeout")
    public void setCacheStoreTimeout(Expression<Millis> expr)
        {
        m_exprCacheStoreTimeout = expr;
        }

    /**
     * Return the scheme which the specifies the map used to cache entries.
     *
     * @return the scheme for the internal map
     */
    public CachingScheme getInternalScheme()
        {
        return m_schemeInternal;
        }

    /**
     * Set the internal scheme.
     *
     * @param  scheme  the internal scheme
     */
    @Injectable("internal-cache-scheme")
    public void setInternalScheme(CachingScheme scheme)
        {
        m_schemeInternal = scheme;
        }

    /**
     * Return the internal map which is set by the backing map manager when
     * the partitioned flag is true.  Otherwise the map will be null.
     *
     * @return the internal map
     */
    public ObservableMap getInternalMap()
        {
        return m_mapInternal;
        }

    /**
     * Set the internal map.
     *
     * @param map  the internal map
     */
    public void setInternalMap(ObservableMap map)
        {
        m_mapInternal = map;
        }

    /**
     * Return the {@link Scheme} for the cache used to maintain information on cache
     * misses.  The miss-cache is used track keys which were not found in the
     * cache store.  The knowledge that a key is not in the cache store allows
     * some operations to perform faster, as they can avoid querying the potentially
     * slow cache store.  A size-limited scheme may be used to control how many
     * misses are cached.  If unspecified no cache-miss data is maintained.
     *
     * @return the miss cache scheme
     */
    public LocalScheme getMissCacheScheme()
        {
        return m_schemeMissCache;
        }

    /**
     * Set the miss cache {@link Scheme}.
     *
     * @param scheme  the miss cache scheme
     */
    @Injectable("miss-cache-scheme")
    public void setMissCacheScheme(LocalScheme scheme)
        {
        m_schemeMissCache = scheme;
        }

    /**
     * Returns true if the cache is read only. A read-only cache loads data
     * from cache store for read operations and does not perform any writing
     * to the cache store when the cache is updated.
     *
     * @param resolver  the ParameterResolver
     *
     * @return true if the cache is read only
     */
    public boolean isReadOnly(ParameterResolver resolver)
        {
        return m_exprReadOnly.evaluate(resolver);
        }

    /**
     * Set the read-only flag.
     *
     * @param expr  true if the cache is read-only
     */
    @Injectable
    public void setReadOnly(Expression<Boolean> expr)
        {
        m_exprReadOnly = expr;
        }

    /**
     * Return refresh-ahead-factor used to calculate the "soft-expiration"
     * time for cache entries.  Soft-expiration is the point in time before
     * the actual expiration after which any access request for an entry
     * schedules an asynchronous load request for the entry. This attribute
     * is only applicable if the internal cache is a LocalCache, with a
     * non-zero expiry delay.  The value is expressed as a percentage of the
     * internal LocalCache expiration interval. If zero, refresh-ahead scheduling
     * is disabled. If 1.0, then any get operation immediately triggers an
     * asynchronous reload. Legal values are nonnegative doubles less than
     * or equal to 1.0.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the refresh-ahead factor
     */
    public double getRefreshAheadFactor(ParameterResolver resolver)
        {
        return m_exprRefreshAheadFactor.evaluate(resolver);
        }

    /**
     * Set the refresh ahead factor.
     *
     * @param expr  the refresh ahead factor
     */
    @Injectable
    public void setRefreshAheadFactor(Expression<Double> expr)
        {
        m_exprRefreshAheadFactor = expr;
        }

    /**
     * Return true if exceptions caught during synchronous cachestore operations
     * are rethrown to the calling thread (possibly over the network to a remote
     * member).  Legal values are true or false. If the value of this element is
     * false, an exception caught during a synchronous cachestore operation is
     * logged locally and the internal cache is updated. If the value is true,
     * the exception is rethrown to the calling thread and the internal cache is
     * not changed. If the operation was called within a transactional context,
     * this would have the effect of rolling back the current transaction.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the rollback cachestore failures flag
     */
    public boolean isRollbackCacheStoreFailures(ParameterResolver resolver)
        {
        return m_exprfRollbackCacheStoreFailures.evaluate(resolver);
        }

    /**
     * Set the flag to indicate that cache store failures should be rolled back.
     *
     * @param expr  true if failures should be rolled back
     */
    @Injectable("rollback-cachestore-failures")
    public void setRollbackCacheStoreFailures(Expression<Boolean> expr)
        {
        m_exprfRollbackCacheStoreFailures = expr;
        }

    /**
     * Return the write-batch-factor element is used to calculate the "soft-ripe"
     * time for write-behind queue entries. A queue entry is considered to be
     * "ripe" for a write operation if it has been in the write-behind queue
     * for no less than the write-delay interval. The "soft-ripe" time is the
     * point in time before the actual ripe time after which an entry is included
     * in a batched asynchronous write operation to the CacheStore (along with
     * all other ripe and soft-ripe entries).
     *
     * @param resolver  the ParameterResolver
     *
     * @return the write batch factor
     */
    public double getWriteBatchFactor(ParameterResolver resolver)
        {
        return m_exprWriteBatchFactor.evaluate(resolver);
        }

    /**
     * Set the write batch factor.
     *
     * @param expr  the write batch factor
     */
    @Injectable
    public void setWriteBatchFactor(Expression<Double> expr)
        {
        m_exprWriteBatchFactor = expr;
        }

    /**
     * Return the time interval to defer asynchronous writes to the cache store
     * for a write-behind queue.  If zero, synchronous writes to the cache store
     * (without queuing) take place, otherwise the writes are asynchronous and
     * deferred by specified time interval after the last update to the value
     * in the cache.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the write behind delay
     */
    public Seconds getWriteDelay(ParameterResolver resolver)
        {
        return m_exprWriteDelay.evaluate(resolver);
        }

    /**
     * Set the write behind delay.
     *
     * @param expr  the write behind delay
     */
    @Injectable
    public void setWriteDelay(Expression<Seconds> expr)
        {
        m_exprWriteDelay = expr;
        }

    /**
     * Return the write behind delay in seconds.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the write behind delay in seconds
     */
    public int getWriteDelaySeconds(ParameterResolver resolver)
        {
        return m_exprWriteDelaySeconds.evaluate(resolver);
        }

    /**
     * Set the write behind delay seconds.
     *
     * @param expr  the write behind delay in seconds
     */
    @Injectable
    public void setWriteDelaySeconds(Expression<Integer> expr)
        {
        m_exprWriteDelaySeconds = expr;
        }

    /**
     * Return the maximum number of entries to write in a single storeAll
     * operation.  Valid values are positive integers or zero. The default
     * value is 128 entries.  This value has no effect if write behind is disabled.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the write maximum batch size
     */
    public int getWriteMaxBatchSize(ParameterResolver resolver)
        {
        return m_exprWriteMaxBatchSize.evaluate(resolver);
        }

    /**
     * Set the write max batch size.
     *
     * @param expr  the write max batch size
     */
    @Injectable
    public void setWriteMaxBatchSize(Expression<Integer> expr)
        {
        m_exprWriteMaxBatchSize = expr;
        }

    /**
     * Return the size of the write-behind queue at which additional actions
     * could be taken.  If zero, write-behind re-queuing is disabled. Otherwise,
     * this value controls the frequency of the corresponding log messages.
     * For example, a value of 100 produces a log message every time the size
     * of the write queue is a multiple of 100. Legal values are positive
     * integers or zero.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the write re-queue threshold
     */
    public int getWriteRequeueThreshold(ParameterResolver resolver)
        {
        return m_exprWriteRequeueThreshold.evaluate(resolver);
        }

    /**
     * Set the size of the write-behind queue at which additional actions
     * could be taken.
     *
     * @param expr  the write re-queue threshold
     */
    @Injectable
    public void setWriteRequeueThreshold(Expression<Integer> expr)
        {
        m_exprWriteRequeueThreshold = expr;
        }

    /**
     * Returns true if the write-behind applies to remove.
     *
     * @param resolver  the ParameterResolver
     *
     * @return true if the write-behind applies to remove.
     *
     * @since 12.2.1.4.18
     */
    public boolean isWriteBehindRemove(ParameterResolver resolver)
        {
        return m_exprWriteBehindRemove.evaluate(resolver);
        }

    /**
     * Set the write-behind-remove flag.
     *
     * @param expr  true if the write-behind applies to remove.
     *
     * @since 12.2.1.4.18
     */
    @Injectable
    public void setWriteBehindRemove(Expression<Boolean> expr)
        {
        m_exprWriteBehindRemove = expr;
        }

    // ----- internal -------------------------------------------------------

    /**
     * Construct a ReadWriteBackingMap using the specified parameters.
     * <p>
     * This method exposes a corresponding ReadWriteBackingMap
     * {@link ReadWriteBackingMap#ReadWriteBackingMap(BackingMapManagerContext,
     * ObservableMap, Map, CacheLoader, boolean, int, double, boolean) constructor}
     * and is provided for the express purpose of allowing its override.
     *
     * @param context               the context provided by the CacheService
     *                              which is using this backing map
     * @param mapInternal           the ObservableMap used to store the data
     *                              internally in this backing map
     * @param mapMisses             the Map used to cache CacheStore misses
     *                              (optional)
     * @param store                 the object responsible for the persistence
     *                              of the cached data (optional)
     * @param fReadOnly             pass true is the specified loader is in fact
     *                              a CacheStore that needs to be used only for
     *                              read operations; changes to the cache will
     *                              not be persisted
     * @param cWriteBehindSeconds   number of seconds to write if there is a
     *                              CacheStore; zero disables write-behind
     *                              caching, which (combined with !fReadOnly)
     *                              implies write-through
     * @param dflRefreshAheadFactor the interval before an entry expiration time
     *                              (expressed as a percentage of the internal
     *                              cache expiration interval) during which an
     *                              asynchronous load request for the
     *                              entry will be scheduled; zero disables
     *                              refresh-ahead; only applicable when
     *                              the <tt>mapInternal</tt> parameter is an
     *                              instance of {@link com.tangosol.net.cache.ConfigurableCacheMap}
     * @param fWriteBehindRemove    pass true if the specified loader is in fact a
     *                              CacheStore that needs to apply write-behind to remove
     *
     * @return the instantiated {@link ReadWriteBackingMap}
     */
    protected ReadWriteBackingMap instantiateReadWriteBackingMap(BackingMapManagerContext context,
        ObservableMap mapInternal, Map mapMisses, CacheLoader store, boolean fReadOnly, int cWriteBehindSeconds,
        double dflRefreshAheadFactor, boolean fWriteBehindRemove)
        {
        return new ReadWriteBackingMap(context, mapInternal, mapMisses, store, fReadOnly, cWriteBehindSeconds,
                                       dflRefreshAheadFactor, fWriteBehindRemove);
        }

    /**
     * Construct a ReadWriteBackingMap using the specified parameters.
     * <p>
     * This method exposes a corresponding ReadWriteBackingMap
     * {@link ReadWriteBackingMap#ReadWriteBackingMap(BackingMapManagerContext,
     * ObservableMap, Map, BinaryEntryStore, boolean, int, double, boolean) constructor}
     * and is provided for the express purpose of allowing its override.
     *
     * @param context               the context provided by the CacheService
     *                              which is using this backing map
     * @param mapInternal           the ObservableMap used to store the data
     *                              internally in this backing map
     * @param mapMisses             the Map used to cache CacheStore misses
     *                              (optional)
     * @param storeBinary           the BinaryEntryStore responsible for the
     *                              persistence of the cached data (optional)
     * @param fReadOnly             pass true is the specified loader is in fact
     *                              a CacheStore that needs to be used only for
     *                              read operations; changes to the cache will
     *                              not be persisted
     * @param cWriteBehindSeconds   number of seconds to write if there is a
     *                              CacheStore; zero disables write-behind
     *                              caching, which (combined with !fReadOnly)
     *                              implies write-through
     * @param dflRefreshAheadFactor the interval before an entry expiration time
     *                              (expressed as a percentage of the internal
     *                              cache expiration interval) during which an
     *                              asynchronous load request for the
     *                              entry will be scheduled; zero disables
     *                              refresh-ahead; only applicable when
     *                              the <tt>mapInternal</tt> parameter is an
     *                              instance of {@link com.tangosol.net.cache.ConfigurableCacheMap}
     * @param fWriteBehindRemove    pass true if the specified loader is in fact a
     *                              CacheStore that needs to apply write-behind to remove
     *
     * @return the instantiated {@link ReadWriteBackingMap}
     */
    protected ReadWriteBackingMap instantiateReadWriteBackingMap(BackingMapManagerContext context,
        ObservableMap mapInternal, Map mapMisses, BinaryEntryStore storeBinary, boolean fReadOnly,
        int cWriteBehindSeconds, double dflRefreshAheadFactor, boolean fWriteBehindRemove)
        {
        return new ReadWriteBackingMap(context, mapInternal, mapMisses, storeBinary, fReadOnly, cWriteBehindSeconds,
                                       dflRefreshAheadFactor, fWriteBehindRemove);
        }

    /**
     * Construct a ReadWriteBackingMap using the specified parameters.
     * <p>
     * This method exposes a corresponding ReadWriteBackingMap
     * {@link ReadWriteBackingMap#ReadWriteBackingMap(BackingMapManagerContext,
     * ObservableMap, Map, BinaryEntryStore, boolean, int, double, boolean) constructor}
     * and is provided for the express purpose of allowing its override.
     *
     * @param context               the context provided by the CacheService
     *                              which is using this backing map
     * @param mapInternal           the ObservableMap used to store the data
     *                              internally in this backing map
     * @param mapMisses             the Map used to cache CacheStore misses
     *                              (optional)
     * @param storeBinary           the NonBlockingEntryStore responsible for the
     *                              persistence of the cached data (optional)
     * @param fReadOnly             pass true is the specified loader is in fact
     *                              a CacheStore that needs to be used only for
     *                              read operations; changes to the cache will
     *                              not be persisted
     * @param cWriteBehindSeconds   number of seconds to write if there is a
     *                              CacheStore; zero disables write-behind
     *                              caching, which (combined with !fReadOnly)
     *                              implies write-through
     * @param dflRefreshAheadFactor the interval before an entry expiration time
     *                              (expressed as a percentage of the internal
     *                              cache expiration interval) during which an
     *                              asynchronous load request for the
     *                              entry will be scheduled; zero disables
     *                              refresh-ahead; only applicable when
     *                              the <tt>mapInternal</tt> parameter is an
     *                              instance of {@link com.tangosol.net.cache.ConfigurableCacheMap}
     * @param fWriteBehindRemove    pass true if the specified loader is in fact a
     *                              CacheStore that needs to apply write-behind to remove
     *
     * @return the instantiated {@link ReadWriteBackingMap}
     */
    protected ReadWriteBackingMap instantiateReadWriteBackingMap(BackingMapManagerContext context,
        ObservableMap mapInternal, Map mapMisses, NonBlockingEntryStore storeBinary, boolean fReadOnly,
        int cWriteBehindSeconds, double dflRefreshAheadFactor, boolean fWriteBehindRemove)
        {
        return new ReadWriteBackingMap(context, mapInternal, mapMisses, storeBinary, fReadOnly, cWriteBehindSeconds,
                dflRefreshAheadFactor, fWriteBehindRemove);
        }

    /**
     * Construct a ReadWriteSplittingBackingMap using the specified parameters.
     * <p>
     * This method exposes a corresponding ReadWriteSplittingBackingMap
     * {@link ReadWriteSplittingBackingMap#ReadWriteSplittingBackingMap(BackingMapManagerContext,
     * PartitionAwareBackingMap, Map, CacheLoader, boolean, int, double, boolean) constructor}
     * and is provided for the express purpose of allowing its override.
     *
     * @param context               the context provided by the CacheService
     *                              which is using this backing map
     * @param mapInternal           the ObservableMap used to store the data
     *                              internally in this backing map
     * @param mapMisses             the Map used to cache CacheStore misses
     *                              (optional)
     * @param store                 the object responsible for the persistence
     *                              of the cached data (optional)
     * @param fReadOnly             pass true is the specified loader is in fact
     *                              a CacheStore that needs to be used only for
     *                              read operations; changes to the cache will
     *                              not be persisted
     * @param cWriteBehindSeconds   number of seconds to write if there is a
     *                              CacheStore; zero disables write-behind
     *                              caching, which (combined with !fReadOnly)
     *                              implies write-through
     * @param dflRefreshAheadFactor the interval before an entry expiration time
     *                              (expressed as a percentage of the internal
     *                              cache expiration interval) during which an
     *                              asynchronous load request for the
     *                              entry will be scheduled; zero disables
     *                              refresh-ahead; only applicable when
     *                              the <tt>mapInternal</tt> parameter is an
     *                              instance of {@link com.tangosol.net.cache.ConfigurableCacheMap}
     * @param fWriteBehindRemove    pass true if the specified loader is in fact a
     *                              CacheStore that needs to apply write-behind to remove
     *
     * @return the instantiated {@link ReadWriteSplittingBackingMap}
     */
    protected ReadWriteSplittingBackingMap instantiateReadWriteSplittingBackingMap(BackingMapManagerContext context,
        PartitionAwareBackingMap mapInternal, Map mapMisses, CacheLoader store, boolean fReadOnly,
        int cWriteBehindSeconds, double dflRefreshAheadFactor, boolean fWriteBehindRemove)
        {
        return new ReadWriteSplittingBackingMap(context, mapInternal, mapMisses, store, fReadOnly, cWriteBehindSeconds,
            dflRefreshAheadFactor, fWriteBehindRemove);
        }

    /**
     * Construct a ReadWriteSplittingBackingMap using the specified parameters.
     * <p>
     * This method exposes a corresponding ReadWriteSplittingBackingMap
     * {@link ReadWriteSplittingBackingMap#ReadWriteSplittingBackingMap(BackingMapManagerContext,
     * PartitionAwareBackingMap, Map, BinaryEntryStore, boolean, int, double, boolean) constructor}
     * and is provided for the express purpose of allowing its override.
     *
     * @param context               the context provided by the CacheService
     *                              which is using this backing map
     * @param mapInternal           the ObservableMap used to store the data
     *                              internally in this backing map
     * @param mapMisses             the Map used to cache CacheStore misses
     *                              (optional)
     * @param storeBinary           the BinaryEntryStore responsible for the
     *                              persistence of the cached data (optional)
     * @param fReadOnly             pass true is the specified loader is in fact
     *                              a CacheStore that needs to be used only for
     *                              read operations; changes to the cache will
     *                              not be persisted
     * @param cWriteBehindSeconds   number of seconds to write if there is a
     *                              CacheStore; zero disables write-behind
     *                              caching, which (combined with !fReadOnly)
     *                              implies write-through
     * @param dflRefreshAheadFactor the interval before an entry expiration time
     *                              (expressed as a percentage of the internal
     *                              cache expiration interval) during which an
     *                              asynchronous load request for the
     *                              entry will be scheduled; zero disables
     *                              refresh-ahead; only applicable when
     *                              the <tt>mapInternal</tt> parameter is an
     *                              instance of {@link com.tangosol.net.cache.ConfigurableCacheMap}
     * @param fWriteBehindRemove    pass true if the specified loader is in fact a
     *                              CacheStore that needs to apply write-behind to remove
     *
     * @return the instantiated {@link ReadWriteSplittingBackingMap}
     */
    protected ReadWriteSplittingBackingMap instantiateReadWriteSplittingBackingMap(BackingMapManagerContext context,
        PartitionAwareBackingMap mapInternal, Map mapMisses, BinaryEntryStore storeBinary, boolean fReadOnly,
        int cWriteBehindSeconds, double dflRefreshAheadFactor, boolean fWriteBehindRemove)
        {
        return new ReadWriteSplittingBackingMap(context, mapInternal, mapMisses, storeBinary, fReadOnly,
            cWriteBehindSeconds, dflRefreshAheadFactor, fWriteBehindRemove);
        }

    /**
     * Construct a ReadWriteSplittingBackingMap using the specified parameters.
     * <p>
     * This method exposes a corresponding ReadWriteSplittingBackingMap
     * {@link ReadWriteSplittingBackingMap#ReadWriteSplittingBackingMap(BackingMapManagerContext,
     * PartitionAwareBackingMap, Map, BinaryEntryStore, boolean, int, double, boolean) constructor}
     * and is provided for the express purpose of allowing its override.
     *
     * @param context               the context provided by the CacheService
     *                              which is using this backing map
     * @param mapInternal           the ObservableMap used to store the data
     *                              internally in this backing map
     * @param mapMisses             the Map used to cache CacheStore misses
     *                              (optional)
     * @param storeBinary           the NonBlockingEntryStore responsible for the
     *                              persistence of the cached data (optional)
     * @param fReadOnly             pass true is the specified loader is in fact
     *                              a CacheStore that needs to be used only for
     *                              read operations; changes to the cache will
     *                              not be persisted
     * @param cWriteBehindSeconds   number of seconds to write if there is a
     *                              CacheStore; zero disables write-behind
     *                              caching, which (combined with !fReadOnly)
     *                              implies write-through
     * @param dflRefreshAheadFactor the interval before an entry expiration time
     *                              (expressed as a percentage of the internal
     *                              cache expiration interval) during which an
     *                              asynchronous load request for the
     *                              entry will be scheduled; zero disables
     *                              refresh-ahead; only applicable when
     *                              the <tt>mapInternal</tt> parameter is an
     *                              instance of {@link com.tangosol.net.cache.ConfigurableCacheMap}
     * @param fWriteBehindRemove    pass true if the specified loader is in fact a
     *                              CacheStore that needs to apply write-behind to remove
     *
     * @return the instantiated {@link ReadWriteSplittingBackingMap}
     */
    protected ReadWriteSplittingBackingMap instantiateReadWriteSplittingBackingMap(BackingMapManagerContext context,
        PartitionAwareBackingMap mapInternal, Map mapMisses, NonBlockingEntryStore storeBinary, boolean fReadOnly,
        int cWriteBehindSeconds, double dflRefreshAheadFactor, boolean fWriteBehindRemove)
        {
        return new ReadWriteSplittingBackingMap(context, mapInternal, mapMisses, storeBinary, fReadOnly,
                cWriteBehindSeconds, dflRefreshAheadFactor, fWriteBehindRemove);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate(ParameterResolver resolver)
        {
        super.validate(resolver);

        Base.checkNotNull(getInternalScheme(), "Internal scheme");
        }

    // ----- data members ---------------------------------------------------

    /**
     * The CacheStore or CacheLoader scheme.
     */
    private CacheStoreScheme m_schemeCacheStore;

    /**
     * The internal scheme.
     */
    private CachingScheme m_schemeInternal;

    /**
     * The miss cache scheme.
     */
    private LocalScheme m_schemeMissCache;

    /**
     * The CacheStore or CacheLoader timeout.
     */
    private Expression<Millis> m_exprCacheStoreTimeout = new LiteralExpression<Millis>(new Millis("0"));

    /**
     * The flag that specifies if the cache is read-only.
     */
    private Expression<Boolean> m_exprReadOnly = new LiteralExpression<Boolean>(Boolean.FALSE);

    /**
     * The refresh ahead factor.
     */
    private Expression<Double> m_exprRefreshAheadFactor = new LiteralExpression<Double>(0.0);

    /**
     * The rollback CacheStore failures flag.
     */
    private Expression<Boolean> m_exprfRollbackCacheStoreFailures = new LiteralExpression<Boolean>(Boolean.TRUE);

    /**
     * The write batch factor.
     */
    private Expression<Double> m_exprWriteBatchFactor = new LiteralExpression<Double>(0.0);

    /**
     * The write-delay value.
     */
    private Expression<Seconds> m_exprWriteDelay = new LiteralExpression<Seconds>(new Seconds("0"));

    /**
     * The write-delay-seconds value.
     */
    private Expression<Integer> m_exprWriteDelaySeconds = new LiteralExpression<Integer>(Integer.valueOf(0));

    /**
     * The write maximum batch size.
     */
    private Expression<Integer> m_exprWriteMaxBatchSize = new LiteralExpression<Integer>(Integer.valueOf(128));

    /**
     * The write re-queue threshold.
     */
    private Expression<Integer> m_exprWriteRequeueThreshold = new LiteralExpression<Integer>(Integer.valueOf(0));

    /**
     * The flag that specifies if the write-behind applies to remove.
     *
     * @since 12.2.1.4.18
     */
    private Expression<Boolean> m_exprWriteBehindRemove = new LiteralExpression<>(RWBM_WB_REMOVE_DEFAULT);

    /**
     * The internal map.
     */
    private ObservableMap m_mapInternal;
    }
