/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.oracle.coherence.common.base.Blocking;

import com.tangosol.application.ContainerHelper;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.tracing.Scope;
import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.SpanContext;
import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.license.CoherenceCommunityEdition;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.Guardian;
import com.tangosol.net.Guardian.GuardContext;
import com.tangosol.net.GuardSupport;

import com.tangosol.util.AbstractKeyBasedMap;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.Daemon;
import com.tangosol.util.EntrySetMap;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InflatableList;
import com.tangosol.util.LongArray;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.RecyclingLinkedList;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.SafeHashSet;
import com.tangosol.util.SegmentedConcurrentMap;
import com.tangosol.util.SimpleEnumerator;
import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.SparseArray;
import com.tangosol.util.SubSet;
import com.tangosol.util.WrapperException;

import java.lang.reflect.Array;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
* Backing Map implementation that provides a size-limited cache of a
* persistent store and supports configurable write-behind and refresh-
* ahead caching.
*
* This implementation is not intended to support null keys or null
* values.
*
* @author cp 2002.11.25
* @author jh 2005.02.08
*/
@SuppressWarnings("unchecked")
public class ReadWriteBackingMap
        extends AbstractMap
        implements CacheMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a ReadWriteBackingMap based on a CacheLoader (CacheStore).
    *
    * @param ctxService   the context provided by the CacheService
    *                     which is using this backing map
    * @param mapInternal  the ObservableMap used to store the data
    *                     internally in this backing map
    * @param mapMisses    the Map used to cache CacheLoader misses (optional)
    * @param loader       the CacheLoader responsible for the persistence of
    *                     the cached data (optional)
    */
    public ReadWriteBackingMap(BackingMapManagerContext ctxService,
            ObservableMap mapInternal, Map mapMisses, CacheLoader loader)
        {
        init(ctxService, mapInternal, mapMisses, loader, null, null, true, 0, 0.0, RWBM_WB_REMOVE_DEFAULT);
        }

    /**
    * Construct a ReadWriteBackingMap based on a CacheLoader (CacheStore).
    *
    * @param ctxService             the context provided by the CacheService
    *                               which is using this backing map
    * @param mapInternal            the ObservableMap used to store the data
    *                               internally in this backing map
    * @param mapMisses              the Map used to cache CacheStore misses
    *                               (optional)
    * @param loader                 the CacheLoader responsible for the
    *                               persistence of the cached data (optional)
    * @param fReadOnly              pass true is the specified loader is in fact
    *                               a CacheStore that needs to be used only for
    *                               read operations; changes to the cache will
    *                               not be persisted
    * @param cWriteBehindSeconds    number of seconds to write if there is a
    *                               CacheStore; zero disables write-behind
    *                               caching, which (combined with !fReadOnly)
    *                               implies write-through
    * @param dflRefreshAheadFactor  the interval before an entry expiration time
    *                               (expressed as a percentage of the internal
    *                               cache expiration interval) during which an
    *                               asynchronous load request for the
    *                               entry will be scheduled; zero disables
    *                               refresh-ahead; only applicable when
    *                               the <tt>mapInternal</tt> parameter is an
    *                               instance of {@link ConfigurableCacheMap}
    */
    public ReadWriteBackingMap(BackingMapManagerContext ctxService, ObservableMap mapInternal,
            Map mapMisses, CacheLoader loader, boolean fReadOnly, int cWriteBehindSeconds,
            double dflRefreshAheadFactor)
        {
        init(ctxService, mapInternal, mapMisses, loader, null, null, fReadOnly,
             cWriteBehindSeconds, dflRefreshAheadFactor, RWBM_WB_REMOVE_DEFAULT);
        }

    /**
     * Construct a ReadWriteBackingMap based on a CacheLoader (CacheStore).
     *
     * @param ctxService             the context provided by the CacheService
     *                               which is using this backing map
     * @param mapInternal            the ObservableMap used to store the data
     *                               internally in this backing map
     * @param mapMisses              the Map used to cache CacheStore misses
     *                               (optional)
     * @param loader                 the CacheLoader responsible for the
     *                               persistence of the cached data (optional)
     * @param fReadOnly              pass true is the specified loader is in fact
     *                               a CacheStore that needs to be used only for
     *                               read operations; changes to the cache will
     *                               not be persisted
     * @param cWriteBehindSeconds    number of seconds to write if there is a
     *                               CacheStore; zero disables write-behind
     *                               caching, which (combined with !fReadOnly)
     *                               implies write-through
     * @param dflRefreshAheadFactor  the interval before an entry expiration time
     *                               (expressed as a percentage of the internal
     *                               cache expiration interval) during which an
     *                               asynchronous load request for the
     *                               entry will be scheduled; zero disables
     *                               refresh-ahead; only applicable when
     *                               the <tt>mapInternal</tt> parameter is an
     *                               instance of {@link ConfigurableCacheMap}
     * @param fWriteBehindRemove     pass true if the specified loader is in fact a
     *                               CacheStore that needs to apply write-behind to remove
     *
     * @since 12.2.1.4.18
     */
    public ReadWriteBackingMap(BackingMapManagerContext ctxService, ObservableMap mapInternal,
                               Map mapMisses, CacheLoader loader, boolean fReadOnly, int cWriteBehindSeconds,
                               double dflRefreshAheadFactor, boolean fWriteBehindRemove)
        {
        init(ctxService, mapInternal, mapMisses, loader, null, null, fReadOnly,
                cWriteBehindSeconds, dflRefreshAheadFactor, fWriteBehindRemove);
        }

    /**
    * Construct a ReadWriteBackingMap based on a BinaryEntryStore.
    *
    * @param ctxService             the context provided by the CacheService
    *                               which is using this backing map
    * @param mapInternal            the ObservableMap used to store the data
    *                               internally in this backing map
    * @param mapMisses              the Map used to cache CacheStore misses
    *                               (optional)
    * @param storeBinary            the BinaryEntryStore responsible for the
    *                               persistence of the cached data (optional)
    * @param fReadOnly              pass true is the specified loader is in fact
    *                               a CacheStore that needs to be used only for
    *                               read operations; changes to the cache will
    *                               not be persisted
    * @param cWriteBehindSeconds    number of seconds to write if there is a
    *                               CacheStore; zero disables write-behind
    *                               caching, which (combined with !fReadOnly)
    *                               implies write-through
    * @param dflRefreshAheadFactor  the interval before an entry expiration time
    *                               (expressed as a percentage of the internal
    *                               cache expiration interval) during which an
    *                               asynchronous load request for the
    *                               entry will be scheduled; zero disables
    *                               refresh-ahead; only applicable when
    *                               the <tt>mapInternal</tt> parameter is an
    *                               instance of {@link ConfigurableCacheMap}
    * @since Coherence 3.6
    */
    public ReadWriteBackingMap(BackingMapManagerContext ctxService, ObservableMap mapInternal,
                               Map mapMisses, BinaryEntryStore storeBinary, boolean fReadOnly, int cWriteBehindSeconds,
                               double dflRefreshAheadFactor)
        {
        this(ctxService, mapInternal, mapMisses, storeBinary, fReadOnly,
             cWriteBehindSeconds, dflRefreshAheadFactor, RWBM_WB_REMOVE_DEFAULT);
        }

    /**
    * Construct a ReadWriteBackingMap based on a BinaryEntryStore.
    *
    * @param ctxService             the context provided by the CacheService
    *                               which is using this backing map
    * @param mapInternal            the ObservableMap used to store the data
    *                               internally in this backing map
    * @param mapMisses              the Map used to cache CacheStore misses
    *                               (optional)
    * @param storeBinary            the BinaryEntryStore responsible for the
    *                               persistence of the cached data (optional)
    * @param fReadOnly              pass true is the specified loader is in fact
    *                               a CacheStore that needs to be used only for
    *                               read operations; changes to the cache will
    *                               not be persisted
    * @param cWriteBehindSeconds    number of seconds to write if there is a
    *                               CacheStore; zero disables write-behind
    *                               caching, which (combined with !fReadOnly)
    *                               implies write-through
    * @param dflRefreshAheadFactor  the interval before an entry expiration time
    *                               (expressed as a percentage of the internal
    *                               cache expiration interval) during which an
    *                               asynchronous load request for the
    *                               entry will be scheduled; zero disables
    *                               refresh-ahead; only applicable when
    *                               the <tt>mapInternal</tt> parameter is an
    *                               instance of {@link ConfigurableCacheMap}
    * @param fWriteBehindRemove     pass true if the specified loader is in fact
    *                               a CacheStore that needs to apply write-behind to remove
    *
    * @since 12.2.1.4.18
    */
    public ReadWriteBackingMap(BackingMapManagerContext ctxService, ObservableMap mapInternal,
            Map mapMisses, BinaryEntryStore storeBinary, boolean fReadOnly, int cWriteBehindSeconds,
            double dflRefreshAheadFactor, boolean fWriteBehindRemove)
        {
        init(ctxService, mapInternal, mapMisses, null, storeBinary, null, fReadOnly,
             cWriteBehindSeconds, dflRefreshAheadFactor, fWriteBehindRemove);
        }

    /**
    * Construct a ReadWriteBackingMap based on a NonBlockingEntryStore.
    *
    * @param ctxService             the context provided by the CacheService
    *                               which is using this backing map
    * @param mapInternal            the ObservableMap used to store the data
    *                               internally in this backing map
    * @param mapMisses              the Map used to cache CacheStore misses
    *                               (optional)
    * @param storeBinary            the NonBlockingEntryStore responsible for the
    *                               persistence of the cached data (optional)
    * @param fReadOnly              pass true is the specified loader is in fact
    *                               a CacheStore that needs to be used only for
    *                               read operations; changes to the cache will
    *                               not be persisted
    * @param cWriteBehindSeconds    number of seconds to write if there is a
    *                               CacheStore; zero disables write-behind
    *                               caching, which (combined with !fReadOnly)
    *                               implies write-through
    * @param dflRefreshAheadFactor  the interval before an entry expiration time
    *                               (expressed as a percentage of the internal
    *                               cache expiration interval) during which an
    *                               asynchronous load request for the
    *                               entry will be scheduled; zero disables
    *                               refresh-ahead; only applicable when
    *                               the <tt>mapInternal</tt> parameter is an
    *                               instance of {@link ConfigurableCacheMap}
    * @since Coherence 21.06
    */
    public ReadWriteBackingMap(BackingMapManagerContext ctxService, ObservableMap mapInternal,
           Map mapMisses, NonBlockingEntryStore storeBinary, boolean fReadOnly, int cWriteBehindSeconds,
           double dflRefreshAheadFactor)
        {
        init(ctxService, mapInternal, mapMisses, null, null, storeBinary, fReadOnly,
             cWriteBehindSeconds, dflRefreshAheadFactor, RWBM_WB_REMOVE_DEFAULT);
        }

    /**
    * Construct a ReadWriteBackingMap based on a NonBlockingEntryStore.
    *
    * @param ctxService             the context provided by the CacheService
    *                               which is using this backing map
    * @param mapInternal            the ObservableMap used to store the data
    *                               internally in this backing map
    * @param mapMisses              the Map used to cache CacheStore misses
    *                               (optional)
    * @param storeBinary            the NonBlockingEntryStore responsible for the
    *                               persistence of the cached data (optional)
    * @param fReadOnly              pass true is the specified loader is in fact
    *                               a CacheStore that needs to be used only for
    *                               read operations; changes to the cache will
    *                               not be persisted
    * @param cWriteBehindSeconds    number of seconds to write if there is a
    *                               CacheStore; zero disables write-behind
    *                               caching, which (combined with !fReadOnly)
    *                               implies write-through
    * @param dflRefreshAheadFactor  the interval before an entry expiration time
    *                               (expressed as a percentage of the internal
    *                               cache expiration interval) during which an
    *                               asynchronous load request for the
    *                               entry will be scheduled; zero disables
    *                               refresh-ahead; only applicable when
    *                               the <tt>mapInternal</tt> parameter is an
    *                               instance of {@link ConfigurableCacheMap}
    * @param fWriteBehindRemove     pass true if the specified loader is in fact
    *                               a CacheStore that needs to apply write-behind to remove
    *
    * @since 12.2.1.4.18
    */
    public ReadWriteBackingMap(BackingMapManagerContext ctxService, ObservableMap mapInternal,
           Map mapMisses, NonBlockingEntryStore storeBinary, boolean fReadOnly, int cWriteBehindSeconds,
           double dflRefreshAheadFactor, boolean fWriteBehindRemove)
        {
        init(ctxService, mapInternal, mapMisses, null, null, storeBinary, fReadOnly,
                cWriteBehindSeconds, dflRefreshAheadFactor, fWriteBehindRemove);
        }

    /**
    * Initialize the ReadWriteBackingMap.
    *
    * @param ctxService            the context provided by the CacheService
    *                              which is using this backing map
    * @param mapInternal           the ObservableMap used to store the data
    *                              internally in this backing map
    * @param mapMisses             the Map used to cache CacheStore misses
    *                              (optional)
    * @param loader                the object responsible for the persistence
    *                              of the cached data (optional)
    * @param storeBinary           the BinaryEntryStore to wrap
    *                              exclusive with storeNonBlocking)
    * @param storeNonBlocking      the NonBlockingEntryStore to wrap
    *                              (mutually exclusive with storeBinary)
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
    *                              instance of {@link ConfigurableCacheMap}
    * @param fWriteBehindRemove    pass true if the specified loader is in fact
    *                              a CacheStore that needs to apply write-behind to remove
    */
    private void init(BackingMapManagerContext ctxService, ObservableMap mapInternal,
            Map mapMisses, CacheLoader loader, BinaryEntryStore storeBinary, NonBlockingEntryStore storeNonBlocking,
            boolean fReadOnly, int cWriteBehindSeconds, double dflRefreshAheadFactor,
            boolean fWriteBehindRemove)
        {
        m_ctxService = ctxService;
        m_setPendingRemoves = null;

        configureInternalCache(mapInternal);

        if (loader != null || storeBinary != null || storeNonBlocking != null)
            {
            boolean fWBRemove = cWriteBehindSeconds > 0 ? fWriteBehindRemove : false;

            // the misses map is only applicable when there is a valid store
            m_mapMisses = mapMisses;

            if (loader == null)
                {
                configureCacheStore(storeBinary == null
                        ? instantiateCacheStoreWrapper(storeNonBlocking)
                        : instantiateCacheStoreWrapper(storeBinary),
                    fReadOnly, fWBRemove);
                }
            else if (loader instanceof CacheStore)
                {
                configureCacheStore(
                    instantiateCacheStoreWrapper((CacheStore) loader), fReadOnly, fWBRemove);
                }
            else
                {
                configureCacheStore(
                    instantiateCacheStoreWrapper(
                        instantiateCacheLoaderCacheStore(loader)), true, RWBM_WB_REMOVE_DEFAULT);
                }

            // configure the optional write-behind queue and daemon
            configureWriteThread(cWriteBehindSeconds);

            // configure the optional refresh-ahead queue and daemon
            configureReadThread(dflRefreshAheadFactor);
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the context information provided by the CacheService.
    *
    * @return the CacheService's BackingMapManagerContext object that it
    *         provided to the BackingMapManager that created this backing
    *         map
    */
    public BackingMapManagerContext getContext()
        {
        return m_ctxService;
        }

    /**
    * Return the CacheService.
    *
    * @return the CacheService
    */
    public CacheService getCacheService()
        {
        return getContext().getCacheService();
        }

    /**
    * Determine if exceptions caught during synchronous CacheStore operations
    * are rethrown to the calling thread; if false, exceptions are logged.
    *
    * @return true if CacheStore exceptions are rethrown to the calling thread
    */
    public boolean isRethrowExceptions()
        {
        return m_fRethrowExceptions;
        }

    /**
    * Set the value of the flag that determines if exceptions caught during
    * synchronous CacheStore operations are rethrown to the calling thread; if
    * false, exceptions are logged.
    *
    * @param fRethrow  true to indicate that exceptions should be rethrown
    */
    public void setRethrowExceptions(boolean fRethrow)
        {
        m_fRethrowExceptions = fRethrow;
        }

    /**
    * Return the refresh-ahead factor.
    * <p>
    * The refresh-ahead factor is used to calculate the "soft-expiration" time
    * for cache entries. Soft-expiration is the point in time prior to the
    * actual expiration after which any access request for an entry will
    * schedule an asynchronous load request for the entry.
    * <p>
    * The value of this property is expressed as a percentage of the internal
    * cache expiration interval. If zero, refresh-ahead scheduling is
    * disabled.
    *
    * @return the refresh-ahead factor
    */
    public double getRefreshAheadFactor()
        {
        return m_dflRefreshAheadFactor;
        }

    /**
    * Set the refresh-ahead factor, expressed as a percentage of the internal
    * cache expiration interval. Valid values are doubles in the interval
    * [0.0, 1.0].
    * <p>
    * This method has no effect if refresh-ahead is disabled.
    *
    * @param dflRefreshAheadFactor  the new refresh-ahead factor
    *
    * @see #getRefreshAheadFactor
    */
    public void setRefreshAheadFactor(double dflRefreshAheadFactor)
        {
        if (isRefreshAhead())
            {
            if (dflRefreshAheadFactor >= 0.0 && dflRefreshAheadFactor <= 1.0)
                {
                m_dflRefreshAheadFactor = dflRefreshAheadFactor;
                }
            else
                {
                throw new IllegalArgumentException("Invalid refresh-ahead factor: "
                              + dflRefreshAheadFactor);
                }
            }
        }

    /**
    * Determine if the backing map should send data changes through the
    * CacheStore, or should just keep them in memory.
    *
    * @return false to send changes to CacheStore (a read-write cache), or
    *         true to just keep them in memory (a read-only cache)
    */
    public boolean isReadOnly()
        {
        return m_fReadOnly;
        }

    /**
    * Determine if the backing map preemptively reads soon-to-be expired entries
    * on a refresh-ahead thread.
    *
    * @return true if refresh-ahead is enabled
    */
    public boolean isRefreshAhead()
        {
        return getCacheStore() != null && getReadQueue() != null;
        }

    /**
    * Get the maximum size of the write-behind batch.
    *
    * @return the maximum number of entries in the write-behind batch
    */
    public int getWriteMaxBatchSize()
        {
        return m_cWriteMaxBatchSize;
        }

    /**
    * Set the maximum size of a batch. The size is used to reduce the size
    * of the write-behind batches and the amount of [scratch] memory used to
    * keep de-serialized entries passed to the storeAll operations.
    * <p>
    * This method has no effect if write-behind is disabled.
    *
    * @param cWriteMaxBatchSize  the maximum batch size
    */
    public void setWriteMaxBatchSize(int cWriteMaxBatchSize)
        {
        if (cWriteMaxBatchSize <= 0)
            {
            throw new IllegalArgumentException(
                    "Invalid batch size: " + cWriteMaxBatchSize);
            }
        m_cWriteMaxBatchSize = cWriteMaxBatchSize;
        }

    /**
    * Return the write-batch factor.
    * <p>
    * The write-batch factor is used to calculate the "soft-ripe" time for
    * write-behind queue entries. A queue entry is considered to be "ripe"
    * for a write operation if it has been in the write-behind queue for no
    * less than the write-behind interval. The "soft-ripe" time is the point
    * in time prior to the actual ripe time after which an entry will be
    * included in a batched asynchronous write operation to the CacheStore
    * (along with all other ripe and soft-ripe entries). In other words, a
    * soft-ripe entry is an entry that has been in the write-behind queue
    * for at least the following duration:
    * <pre>
    * D' = (1.0 - F)*D</pre>
    * where:
    * <pre>
    * D = write-behind delay
    * F = write-batch factor</pre>
    * Conceptually, the write-behind thread uses the following logic when
    * performing a batched update:
    * <ol>
    * <li>The thread waits for a queued entry to become ripe.</li>
    * <li>When an entry becomes ripe, the thread dequeues all ripe and
    *     soft-ripe entries in the queue.</li>
    * <li>The thread then writes all ripe and soft-ripe entries either via
    *     {@link CacheStore#store store()} (if there is only the single ripe
    *     entry) or {@link CacheStore#storeAll storeAll()} (if there are
    *     multiple ripe/soft-ripe entries).</li>
    * <li>The thread then repeats (1).</li>
    * </ol>
    * <p>
    * This property is only applicable if asynchronous writes are enabled and
    * the CacheStore implements the {@link CacheStore#storeAll storeAll()}
    * method.
    * <p>
    * The value of this property is expressed as a percentage of the
    * {@link #getWriteBehindSeconds write-behind} interval. Valid values are
    * doubles in the interval [0.0, 1.0].
    *
    * @return the write-batch factor
    */
    public double getWriteBatchFactor()
        {
        return m_dflWriteBatchFactor;
        }

    /**
    * Set the write-batch factor, expressed as a percentage of the
    * write-behind interval. Valid values are doubles in the interval
    * [0.0, 1.0].
    * <p>
    * This method has no effect if write-behind is disabled.
    *
    * @param dflWriteBatchFactor  the new write-batch factor
    *
    * @see #getWriteBatchFactor
    */
    public void setWriteBatchFactor(double dflWriteBatchFactor)
        {
        if (isWriteBehind())
            {
            if (dflWriteBatchFactor >= 0.0 && dflWriteBatchFactor <= 1.0)
                {
                m_dflWriteBatchFactor = dflWriteBatchFactor;
                }
            else
                {
                throw new IllegalArgumentException("Invalid write-batch factor: "
                              + dflWriteBatchFactor);
                }
            }
        }

    /**
    * Determine if the backing map writes changes on a write-behind thread
    * through the CacheStore.
    *
    * @return true implies changes are queued to be written asynchronously
    */
    public boolean isWriteBehind()
        {
        return !isReadOnly() && getCacheStore() != null && getWriteQueue() != null;
        }

    /**
    * Return the number of seconds between write-behind writes to the
    * CacheStore or <tt>0</tt> if write-behind is not enabled.
    *
    * @return the number of seconds between write-behind writes
    */
    public int getWriteBehindSeconds()
        {
        long cMillis = getWriteBehindMillis();
        return cMillis == 0 ? 0 : Math.max(1, (int) (cMillis / 1000));
        }

    /**
    * Set the number of seconds between write-behind writes to the CacheStore.
    * <p>
    * This method has not effect if write-behind is not enabled.
    *
    * @param cSecs  the new write-behind delay in seconds
    */
    public void setWriteBehindSeconds(int cSecs)
        {
        setWriteBehindMillis(1000L * cSecs);
        }

    /**
    * Return the number of milliseconds between write-behind writes to the
    * CacheStore or <tt>0</tt> if write-behind is not enabled.
    *
    * @return the number of milliseconds between write-behind writes
    *
    * @since Coherence 3.4
    */
    public long getWriteBehindMillis()
        {
        return m_cWriteBehindMillis;
        }

    /**
    * Set the number of milliseconds between write-behind writes to the CacheStore.
    * <p>
    * This method has not effect if write-behind is not enabled.
    *
    * @param cMillis  the new write-behind delay in milliseconds
    *
    * @since Coherence 3.4
    */
    public void setWriteBehindMillis(long cMillis)
        {
        if (isWriteBehind())
            {
            if (cMillis > 0)
                {
                ConfigurableCacheMap cache = getInternalConfigurableCache();
                if (cache != null)
                    {
                    // make sure that the internal cache expiry is greater or
                    // equal to the write-behind delay
                    int  cExpiryMillis = cache.getExpiryDelay();
                    if (cExpiryMillis > 0 && cExpiryMillis < cMillis)
                        {
                        StringBuilder sb = new StringBuilder()
                          .append("ReadWriteBackingMap internal cache expiry of ")
                          .append(cExpiryMillis)
                          .append(" milliseconds is less than the write-delay of ")
                          .append(cMillis)
                          .append(" milliseconds; ");

                        cMillis = cExpiryMillis;
                        sb.append("decreasing the write-delay to ")
                          .append(cMillis)
                          .append(" milliseconds.");

                        Base.log(sb.toString());
                        }
                    }

                m_cWriteBehindMillis = cMillis;
                getWriteQueue().setDelayMillis(cMillis);
                }
            else
                {
                throw new IllegalArgumentException("Invalid write-behind delay: "
                              + cMillis);
                }
            }
        }

    /**
    * Return the maximum size of the write-behind queue for which failed
    * CacheStore write operations are requeued or <tt>0</tt> if write-behind
    * requeueing is disabled.
    *
    * @return the write-behind requeue threshold
    */
    public int getWriteRequeueThreshold()
        {
        return m_cWriteRequeueThreshold;
        }

    /**
    * Set the maximum size of the write-behind queue for which failed CacheStore
    * write operations are requeued.
    * <p>
    * This method has not effect if write-behind is not enabled.
    *
    * @param cThreshold  the new write-behind requeue threshold
    */
    public void setWriteRequeueThreshold(int cThreshold)
        {
        if (isWriteBehind())
            {
            if (cThreshold >= 0)
                {
                m_cWriteRequeueThreshold = cThreshold;
                }
            else
                {
                throw new IllegalArgumentException("Invalid write requeue threshold: "
                              + cThreshold);
                }
            }
        }

    /**
    * Determine if the backing map writes changes immediately through the
    * CacheStore.
    *
    * @return true implies that changes to the backing map are written
    *         synchronously to the CacheStore
    */
    public boolean isWriteThrough()
        {
        return !isReadOnly() && getCacheStore() != null && getWriteQueue() == null;
        }

    /**
    * Return the timeout used for CacheStore operations, or 0 if no timeout is
    * specified.
    *
    * @return the CacheStore timeout
    */
    public long getCacheStoreTimeoutMillis()
        {
        return m_cStoreTimeoutMillis;
        }

    /**
    * Set the timeout used for CacheStore operations.  A value of 0 indicates
    * to use the default guardian timeout of the associated service.
    *
    * @param cStoreTimeoutMillis  the CacheStore timeout, or 0 for the default
    *                             guardian timeout
    */
    public void setCacheStoreTimeoutMillis(long cStoreTimeoutMillis)
        {
        m_cStoreTimeoutMillis = cStoreTimeoutMillis;

        CacheService service = getContext().getCacheService();
        if (service instanceof Guardian)
            {
            ReadThread  daemonRead  = getReadThread();
            WriteThread daemonWrite = getWriteThread();
            if (daemonRead != null)
                {
                daemonRead.setGuardPolicy((Guardian) service,
                                          cStoreTimeoutMillis, GUARD_RECOVERY);
                daemonRead.m_fRefreshContext = true;
                }
            if (daemonWrite != null)
                {
                daemonWrite.setGuardPolicy((Guardian) service,
                                           cStoreTimeoutMillis, GUARD_RECOVERY);
                daemonWrite.m_fRefreshContext = true;
                }
            }
        }

    /**
    * Set the cache name for ReadThread and WriteThread if not already set.
    *
    * @param sCacheName  the name of the cache
    */
    public void setCacheName(String sCacheName)
        {
        if (sCacheName != null && sCacheName.trim().length() > 0)
            {
            updateThreadName(getReadThread(), sCacheName);
            updateThreadName(getWriteThread(), sCacheName);
            }
        }

    /**
    * Return the size of the write-behind queue if configured, or the
    * number of pending writes of the non-blocking store, if configured.
    * Return -1 if neither.
    *
    * @return number of pending writes
    */
    public int getPendingWrites()
        {
        if (isWriteBehind())
            {
            return getWriteQueue().size();
            }

        StoreWrapper store = getCacheStore();
        if (store != null && !store.isBlocking())
            {
            return (int) store.f_cPendingAsyncStoreOps.get();
            }

        return -1;
        }

    /**
    * Determine if the backing map should apply write-behind delay to
    * remove from CacheStore.
    *
    * @return true to delay the remove from CacheStore (a read-write cache),
    *         or false to remove from CacheStore immediately
    *
    * @since 12.2.1.4.18
    */
    public boolean isWriteBehindRemove()
        {
        return m_fWBRemove;
        }

    // ----- Map interface --------------------------------------------------

    /**
    * Remove everything from the Map.
    */
    public void clear()
        {
        for (Iterator iter = entrySet().iterator(); iter.hasNext(); )
            {
            iter.next();
            iter.remove();
            }
        }

    /**
    * Returns <tt>true</tt> if this map contains a mapping for the specified
    * key.
    *
    * @param oKey  the key to test for
    *
    * @return <tt>true</tt> if this map contains a mapping for the specified
    *         key, <tt>false</tt> otherwise.
    */
    public boolean containsKey(Object oKey)
        {
        if (isWriteBehindRemove() && getPendingRemoves().contains(oKey))
            {
            return false;
            }

        return getInternalCache().containsKey(oKey);
        }

    /**
    * Returns <tt>true</tt> if this CachingMap maps one or more keys to the
    * specified value.
    *
    * @param oValue  the value to test for
    *
    * @return <tt>true</tt> if this CachingMap maps one or more keys to the
    *         specified value, <tt>false</tt> otherwise
    */
    public boolean containsValue(Object oValue)
        {
        return getInternalCache().containsValue(oValue);
        }

    /**
    * Returns the value to which this map maps the specified key.
    *
    * @param oKey  the key object
    *
    * @return the value to which this map maps the specified key,
    *         or null if the map contains no mapping for this key
    */
    public Object get(Object oKey)
        {
        ConcurrentMap mapControl = getControlMap();
        Map           mapMisses  = getMissesCache();

        mapControl.lock(oKey, -1L);
        try
            {
            // check the misses cache
            if (mapMisses != null && mapMisses.containsKey(oKey))
                {
                return null;
                }

            // check the pending removes key set
            if (isWriteBehindRemove() && getPendingRemoves().contains(oKey))
                {
                return null;
                }

            Object oValue = getFromInternalCache(oKey);

            // if the value wasn't found in the in-memory cache; if it's owned
            // ("get" is not caused by a re-distribution), load the value from
            // the CacheStore and cache the value in the in-memory cache
            if (oValue == null && getContext().isKeyOwned(oKey))
                {
                StoreWrapper store = getCacheStore();
                if (store != null)
                    {
                    // load the data from the CacheStore
                    Entry entry = store.load(oKey);

                    oValue = entry == null ? null : entry.getBinaryValue();
                    putToInternalCache(oKey, oValue, extractExpiry(entry));
                    }
                }

            return oValue;
            }
        finally
            {
            mapControl.unlock(oKey);
            }
        }

    /**
    * Associates the specified value with the specified key in this map.
    *
    * @param oKey    key with which the specified value is to be associated
    * @param oValue  value to be associated with the specified key
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key.  A <tt>null</tt> return can
    *         also indicate that the map previously associated <tt>null</tt>
    *         with the specified key, if the implementation supports
    *         <tt>null</tt> values.
    */
    public Object put(Object oKey, Object oValue)
        {
        return putInternal(oKey, oValue, 0L);
        }

    /**
    * Removes the mapping for this key from this map if present.
    * Expensive: updates both the underlying cache and the local cache.
    *
    * @param oKey  key whose mapping is to be removed from the map
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key.  A <tt>null</tt> return can
    *         also indicate that the map previously associated <tt>null</tt>
    *         with the specified key, if the implementation supports
    *         <tt>null</tt> values.
    */
    public Object remove(Object oKey)
        {
        return removeInternal(oKey, false);
        }

    /**
     * Associates the specified values with the respective keys in this map.
     *
     * Be aware that the keys will be locked in the order they are returned from
     * iterating over the map passed in and unlocked at the end of the method.
     * This method is called internally within Coherence and the keys will have
     * been locked at the Service level already, so concurrent calls to this method
     * with the same keys will not be an issue.
     * If this method is somehow called directly by application code, which is not
     * recommended, then it is advisable to pass in a sorted map that sorts the keys
     * by their natural ordering.
     *
     * @param map  keys and values which are to be associated in this map
     */
    @SuppressWarnings("unchecked")
    @Override
    public void putAll(Map map)
        {
        StoreWrapper store = getCacheStore();

        if (map.size() == 1 || getWriteQueue() != null ||
            store == null || !store.isStoreAllSupported() || isReadOnly())
            {
            super.putAll(map);
            return;
            }

        ConcurrentMap mapControl   = getControlMap();
        try
            {
            Map        mapMisses   = getMissesCache();
            Map        mapInternal = getInternalCache();
            Set<Entry> setEntries  = new LinkedHashSet<>(map.size());

            BackingMapManagerContext ctx = getContext();
            for (Map.Entry entry : (Set<Map.Entry>) map.entrySet())
                {
                Object oKey = entry.getKey();

                mapControl.lock(oKey, -1L);

                // clear the key from the misses cache
                if (mapMisses != null)
                    {
                    mapMisses.remove(oKey);
                    }

                if (isWriteBehindRemove())
                    {
                    getPendingRemoves().remove(oKey);
                    }

                cancelOutstandingReads(oKey);

                // if key is owned this is a regular put as opposed to a put due to fail-over
                if (ctx.isKeyOwned(oKey))
                    {
                    setEntries.add(instantiateEntry(oKey, entry.getValue(), mapInternal.get(oKey), 0L));
                    }
                }

            if (!setEntries.isEmpty())
                {
                SubSet     setEntriesFailed = new SubSet(setEntries);
                Set<Entry> setSuccess       = null;
                try
                    {
                    store.storeAll(setEntriesFailed);
                    setSuccess = setEntries;
                    }
                catch (Throwable e)
                    {
                    setSuccess = setEntriesFailed.getRemoved();
                    throw Base.ensureRuntimeException(e);
                    }
                finally
                    {
                    for (Entry entry : setSuccess)
                        {
                        long   cMillis  = 0L;
                        Binary binValue = entry.getBinaryValue();
                        if (entry.isChanged())
                            {
                            binValue = entry.getChangedBinaryValue();

                            // due to technical reasons (event handling), synchronous
                            // removal (binValue == null) is not currently supported
                            // instead, we schedule the entry for almost
                            // instant expiry
                            cMillis = binValue == null ? 1L : extractExpiry(entry);
                            }

                        putToInternalMap(entry.getBinaryKey(), binValue, cMillis);
                        }
                    }
                }
            }
        finally
            {
            for (Object oKey : map.keySet())
                {
                mapControl.unlock(oKey);
                }
            }
        }

    /**
     * Put the specified key in internal format and value in
     * internal format into the internal backing map.
     * If the cExpiry parameter is greater than the default expiry
     * value CacheMap.EXPIRY_DEFAULT and the internal map is not an
     * instance of {@link CacheMap} then an exception will be thrown.
     *
     * @param binKey    the key in internal format
     * @param binValue  the value in internal format; null if the value should be
     *                  cached as "missing"
     * @param cExpiry   the cache entry expiry value
     *
     * @return any previous value tht was mapped to the key.
     *
     * @throws UnsupportedOperationException if the value of cExpiry is
     *         greater than CacheMap.EXPIRY_DEFAULT and the internal map
     *         is not an instance of {@link CacheMap}.
     */
    protected Object putToInternalMap(Object binKey, Object binValue, long cExpiry)
        {
        Map mapInternal = getInternalCache();
        if (mapInternal instanceof CacheMap)
            {
            return ((CacheMap) mapInternal).put(binKey, binValue, cExpiry);
            }
        else if (cExpiry <= CacheMap.EXPIRY_DEFAULT)
            {
            return mapInternal.put(binKey, binValue);
            }

        throw new UnsupportedOperationException(
                "Class \"" + mapInternal.getClass().getName() +
                "\" does not implement CacheMap interface");
        }

    /**
    * Implementation of the remove() API.
    *
    * @param oKey    key whose mapping is to be removed from the map
    * @param fBlind  if true, the return value will be ignored
    *
    * @return previous value associated with specified key, or null
    */
    protected Object removeInternal(Object oKey, boolean fBlind)
        {
        ConcurrentMap mapControl  = getControlMap();
        Map           mapMisses   = getMissesCache();

        mapControl.lock(oKey, -1L);
        try
            {
            // clear the key from the misses cache
            if (mapMisses != null)
                {
                mapMisses.remove(oKey);
                }

            cancelOutstandingReads(oKey);

            // there are three possibilities:
            // (1) read-only: remove in memory only; no CacheStore ops
            // (2) write-through: immediate erase through CacheStore
            // (3) write-behind: queued remove to CacheStore or failover

            // the remove is a potential CacheStore operation even if there is
            // no entry in the internal cache except if it's caused by the
            // CacheService transferring the entry from this backing map;
            // make sure it is owned by this node before delegating to the store
            Object       oValue  = getCachedOrPending(oKey);
            WriteQueue   queue   = getWriteQueue();
            StoreWrapper store   = getCacheStore();
            boolean      fQueued = false;

            if (store != null)
                {
                boolean fOwned = getContext().isKeyOwned(oKey);

                // check if the value needs to be loaded
                if (!fBlind && oValue == null && fOwned)
                    {
                    Entry entry = store.load(oKey);
                    oValue = entry == null ? null : entry.getBinaryValue();

                    // if there is nothing to remove, then return immediately
                    if (oValue == null)
                        {
                        return null;
                        }
                    }

                // remove from the store only if is a read/write store
                if (!isReadOnly())
                    {
                    removeFromWriteQueue(oKey);
                    if (fOwned)
                        {
                        if (isWriteBehindRemove() && queue != null)
                            {
                            // set the value to BIN_ERASE_PENDING
                            queue.add(instantiateEntry(oKey, BIN_ERASE_PENDING, oValue, 0L), 0L);
                            getInternalCache().put(oKey, BIN_ERASE_PENDING);
                            getPendingRemoves().add(oKey);
                            fQueued = true;
                            }
                        else
                            {
                            store.erase(instantiateEntry(oKey, null, oValue));
                            }
                        }
                    }
                }

            if (!fQueued)
                {
                // the remove from the internal cache comes last
                getInternalCache().remove(oKey);
                }

            return oValue;
            }
        finally
            {
            mapControl.unlock(oKey);
            }
        }

    /**
    * Returns the number of key-value mappings in this map.  If the
    * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
    * <tt>Integer.MAX_VALUE</tt>.
    *
    * @return the number of key-value mappings in this map
    */
    public int size()
        {
        int cPendingRemoves = isWriteBehindRemove() ? getPendingRemoves().size() : 0;
        return getInternalCache().size() - cPendingRemoves;
        }

    /**
    * Returns an set view of the mappings contained in this map.
    *
    * @return a set view of the mappings contained in this map
    */
    public Set entrySet()
        {
        EntrySet set = m_entryset;
        if (set == null)
            {
            m_entryset = set = instantiateEntrySet();
            }
        return set;
        }

    /**
    * Returns an set view of the keys contained in this map.
    *
    * @return a set view of the keys contained in this map
    */
    public Set keySet()
        {
        KeySet set = m_keyset;
        if (set == null)
            {
            m_keyset = set = instantiateKeySet();
            }
        return set;
        }

    /**
    * Returns a collection view of the values contained in this map.
    *
    * @return a collection view of the values contained in this map
    */
    public Collection values()
        {
        ValuesCollection values = m_values;
        if (values == null)
            {
            m_values = values = instantiateValuesCollection();
            }
        return values;
        }


    // ----- CacheMap interface ---------------------------------------------

    /**
    * Associates the specified value with the specified key in this map.
    *
    * @param oKey     key with which the specified value is to be associated
    * @param oValue   value to be associated with the specified key
    * @param cMillis  the number of milliseconds until the entry will expire;
    *                 pass zero to use the cache's default ExpiryDelay settings;
    *                 pass -1 to indicate that the entry should never expire
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key
    */
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        return putInternal(oKey, oValue, cMillis);
        }

    /**
    * Retrieve values for all the specified keys.
    *
    * @param colKeys  a collection of keys that may be in the named cache
    *
    * @return a Map of keys to values for the specified keys passed in
    *         <tt>colKeys</tt>
    */
    public Map getAll(Collection colKeys)
        {
        ConcurrentMap            mapControl = getControlMap();
        BackingMapManagerContext ctx        = getContext();

        if (!(colKeys instanceof SortedSet))
            {
            // assume "natural" sort; all keys must be Comparable
            colKeys = new TreeSet(colKeys);
            }

        // to simplify the code, we lock all keys (that could potentially
        // be loaded) within the "try-finally" block, relying on the
        // "forgiving" behavior of the unlock API that would just ignore
        // the keys that were not locked

        try
            {
            Map mapMisses = getMissesCache();
            Map mapResult = new HashMap();
            Set setLoad   = new HashSet();

            for (Object oKey : colKeys)
                {
                // we assume that unlike "get", the "getAll" is never called during
                // re-distribution; technically speaking we should not even need to
                // make the "isKeyOwned" check, but let's play it safe
                if (ctx.isKeyOwned(oKey))
                    {
                    mapControl.lock(oKey, -1L);
                    }
                else
                    {
                    throw new IllegalStateException("Key is not owned: " + oKey);
                    }

                if (mapMisses != null && mapMisses.containsKey(oKey))
                    {
                    // known to be missing; skip
                    continue;
                    }

                if (isWriteBehindRemove() && getPendingRemoves().contains(oKey))
                    {
                    // known to be pending remove; skip
                    continue;
                    }

                Object oValue = getFromInternalCache(oKey);
                if (oValue == null)
                    {
                    // add the key to the set of keys that should be loaded
                    setLoad.add(oKey);
                    }
                else
                    {
                    // the entry is found in the internal cache
                    mapResult.put(oKey, oValue);
                    }
                }

            if (setLoad.isEmpty())
                {
                // everything is found
                return mapResult;
                }

            StoreWrapper store = getCacheStore();
            if (store != null)
                {
                Set setLoaded = store.loadAll(setLoad);
                Set setMissed = new SubSet(setLoad);

                // iterate over the loaded entries and insert them
                for (Object oEntry : setLoaded)
                    {
                    Entry entry   = (Entry) oEntry;
                    Binary binKey = entry.getBinaryKey();

                    putToInternalCache(entry);
                    // merge the loaded keys with the entries already found
                    mapResult.put(binKey, entry.getBinaryValue());

                    setMissed.remove(binKey);
                    }

                // need to update the "misses" for the keys that could not be loaded
                for (Object oKey : setMissed)
                    {
                    putToInternalCache(oKey, null, CacheMap.EXPIRY_DEFAULT);
                    }
                }
            return mapResult;
            }
        finally
            {
            for (Object oKey : colKeys)
                {
                mapControl.unlock(oKey);
                }
            }
        }

    // ----- internal -------------------------------------------------------

    /**
    *  Remove the collection of keys from this ReadWriteBackingMap.
    *  <p>
    *  This method will ensure the configured CacheStore.eraseAll method is
    *  called with all owned keys in addition to removing the relevant entries
    *  from the internal Map.
    *
    * @param colKeys  a collection of keys to remove, that may be in the map
    *
    * @return true if any of the provided keys were successfully removed from
    *         this map
    */
    protected boolean removeAll(Collection colKeys)
        {
        if (isWriteBehindRemove())
            {
            for (Object oKey : colKeys)
                {
                remove(oKey);
                }

            return true;
            }

        StoreWrapper  store            = getCacheStore();
        ConcurrentMap mapControl       = getControlMap();
        Map           mapInternal      = getInternalCache();
        Collection    colKeysProcessed = colKeys;

        try
            {
            Map        mapMisses   = getMissesCache();
            Set<Entry> setEntries  = new HashSet<>(colKeys.size());
            boolean    fReadOnly   = isReadOnly();

            BackingMapManagerContext ctx = getContext();
            for (Object oKey : colKeys)
                {
                mapControl.lock(oKey, -1L);

                // clear the key from the misses cache
                if (mapMisses != null)
                    {
                    mapMisses.remove(oKey);
                    }

                cancelOutstandingReads(oKey);

                if (store != null)
                    {
                    // whether entry exists or not, erase/eraseAll
                    // needs to be called: fetch existing value or use null
                    Object oValue = getCachedOrPending(oKey);

                    boolean fOwned = ctx.isKeyOwned(oKey);

                    // remove from the store only if is a read/write store
                    if (!fReadOnly)
                        {
                        removeFromWriteQueue(oKey);

                        if (fOwned)
                            {
                            setEntries.add(instantiateEntry(oKey, null, oValue, 0L));
                            }
                        }
                    }
                }

            if (!setEntries.isEmpty())
                {
                SubSet<Entry> setEntriesFailed = new SubSet<>(setEntries);
                Set<Entry>    setSuccess       = null;
                try
                    {
                    if (store != null)
                        {
                        store.eraseAll(setEntriesFailed);
                        }
                    setSuccess = setEntries;
                    }
                catch (Throwable e)
                    {
                    setSuccess = setEntriesFailed.getRemoved();
                    throw Base.ensureRuntimeException(e);
                    }
                finally
                    {
                    colKeysProcessed = ConverterCollections.getCollection(
                            setSuccess, Entry::getBinaryKey, NullImplementation.getConverter());
                    }
                }

            return !colKeysProcessed.isEmpty();
            }
        finally
            {
            for (Object binKey : colKeysProcessed)
                {
                mapInternal.remove(binKey);
                }
            for (Object oKey : colKeys)
                {
                mapControl.unlock(oKey);
                }
            }
        }

    /**
    * Add the key and value pair to the internal cache in such a way that the
    * resulting map event would be marked as "synthetic".
    *
    * @param oKey  the key in internal format
    * @param oVal  the value in internal format; null if the value should be
    *              cached as "missing"
    */
    protected void putToInternalCache(Object oKey, Object oVal)
        {
        putToInternalCache(oKey, oVal, CacheMap.EXPIRY_DEFAULT);
        }

    /**
    * Add the key and value pair to the internal cache in such a way that the
    * resulting map event would be marked as "synthetic".
    *
    * @param entry  cache entry
    */
    protected void putToInternalCache(Entry entry)
        {
        Binary oKey = entry.getBinaryKey();
        Binary oVal = entry.getBinaryValue();

        putToInternalCache(oKey, oVal, extractExpiry(entry));
        }

    /**
    * Add the key and value pair to the internal cache in such a way that the
    * resulting map event would be marked as "synthetic".
    *
    * @param oKey     the key in internal format
    * @param oVal     the value in internal format; null if the value should be
    *                 cached as "missing"
    * @param cMillis  the cache entry expiry value
    */
    protected void putToInternalCache(Object oKey, Object oVal, long cMillis)
        {
        Map mapInternal = getInternalCache();
        if (oVal == null)
            {
            Map mapMisses = getMissesCache();
            if (mapMisses != null)
                {
                mapMisses.put(oKey, oKey);
                }
            }
        else
            {
            Map mapSynthetic = getSyntheticEventsMap();

            mapSynthetic.put(oKey, oKey);
            try
                {
                if (cMillis != CacheMap.EXPIRY_DEFAULT && mapInternal instanceof CacheMap)
                    {
                    ((CacheMap) mapInternal).put(oKey, oVal, cMillis);
                    }
                else
                    {
                    mapInternal.put(oKey, oVal);
                    }
                }
            finally
                {
                mapSynthetic.remove(oKey);
                }
            }
        }

    /**
    * Cancel any outstanding asynchronous reads for a key.
    *
    * @param oKey  the key in internal format
    */
    protected void cancelOutstandingReads(Object oKey)
        {
        if (isRefreshAhead() && !isReadOnly())
            {
            Map mapControl = getControlMap();

            getReadQueue().remove(oKey);
            ReadLatch latch = (ReadLatch) mapControl.get(oKey);
            if (latch != null)
                {
                latch.cancel();
                mapControl.remove(oKey);
                }
            }
        }

    /**
    * Get the value for a given key. If the entry is present in the
    * internal cache and refresh-ahead is configured, check if a reload
    * operation needs to be scheduled. If the entry is missing, check for a
    * potentially pending refresh-ahead operation and potentially pending
    * write-behind.
    *
    * @param oKey  the key in internal format
    *
    * @return the value or null if the value is not found
    */
    protected Object getFromInternalCache(Object oKey)
        {
        if (isRefreshAhead())
            {
            ConfigurableCacheMap       cache      = getInternalConfigurableCache();
            ConfigurableCacheMap.Entry entry      = cache.getCacheEntry(oKey);
            Map                        mapControl = getControlMap();

            if (entry == null)
                {
                if (!getContext().isKeyOwned(oKey))
                    {
                    // not owned (quite likely re-distribution); skip
                    return null;
                    }

                // check to see if the value for the given key is currently
                // being loaded by the refresh-ahead thread
                ReadLatch latch = (ReadLatch) mapControl.get(oKey);
                if (latch == null)
                    {
                    // remove the key from the queue to prevent double loads
                    getReadQueue().remove(oKey);
                    }
                else
                    {
                    try
                        {
                        synchronized (latch)
                            {
                            // wait for the load operation to complete,
                            // if necessary
                            while (!latch.isComplete())
                                {
                                Blocking.wait(latch);
                                }
                            }
                        }
                    catch (InterruptedException e)
                        {
                        Thread.currentThread().interrupt();
                        }
                    finally
                        {
                        // remove the latch from the control map
                        mapControl.remove(oKey);
                        }

                    // this method may rethrow an exception thrown by the
                    // asynchronous load operation
                    Object oVal = latch.getValue();

                    putToInternalCache(oKey, oVal);

                    return oVal;
                    }
                }
            else
                {
                // if the entry is ripe for an asynchronous load and the
                // refresh-ahead thread is not currently loading the key,
                // add the key to the refresh-ahead queue
                long lExpiryMillis = entry.getExpiryMillis();
                if (lExpiryMillis != 0)
                    {
                    long lInterval = (long) (cache.getExpiryDelay()
                                        * getRefreshAheadFactor());
                    if (Base.getSafeTimeMillis() >= lExpiryMillis - lInterval)
                        {
                        ReadLatch latch = (ReadLatch) mapControl.get(oKey);
                        if (latch == null)
                            {
                            getReadQueue().add(oKey);
                            }
                        }
                    }
                return entry.getValue();
                }
            }
        return getCachedOrPending(oKey);
        }

    /**
    * Get a value from the internal cache in a way that respects a potentially
    * pending write-behind operation.
    *
    * @param oKey  the key
    *
    * @return  the corresponding value
    */
    protected Object getCachedOrPending(Object oKey)
        {
        Object oValue = getInternalCache().get(oKey);
        if (oValue == null)
            {
            WriteQueue queue = getWriteQueue();
            if (queue != null)
                {
                // check for a rare case when an entry has been evicted
                // at the exact time when it ripened and is being processed
                // by the WriteBehind thread
                // (see COH-1234)
                oValue = queue.checkPending(oKey);
                }
            }
        return oValue;
        }

    /**
    * An actual implementation for the extended put() method.
    *
    * @param oKey     key with which the specified value is to be associated
    * @param oValue   value to be associated with the specified key
    * @param cMillis  the number of milliseconds until the entry will expire
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key.
    */
    protected Object putInternal(Object oKey, Object oValue, long cMillis)
        {
        ConcurrentMap mapControl  = getControlMap();
        Map           mapMisses   = getMissesCache();
        Map           mapInternal = getInternalCache();
        Set           setRemoves  = getPendingRemoves();

        mapControl.lock(oKey, -1L);
        try
            {
            // clear the key from the misses cache
            if (mapMisses != null)
                {
                mapMisses.remove(oKey);
                }

            if (isWriteBehindRemove() && !setRemoves.isEmpty())
                {
                setRemoves.remove(oKey);
                }

            cancelOutstandingReads(oKey);

            BackingMapManagerContext ctx = getContext();

            // there are three possibilities:
            // (1) read-only: keep it in memory; no CacheStore ops
            // (2) write-through: immediate write to CacheStore
            // (3) write-behind: queued write to CacheStore or failover
            StoreWrapper store = getCacheStore();
            WriteQueue   queue = getWriteQueue();
            if (store != null && !isReadOnly())
                {
                // the "owned" flag indicates whether or not this put is a
                // regular (client driven) operation or an effect of a failover
                boolean fOwned = ctx.isKeyOwned(oKey);

                if (queue == null)
                    {
                    boolean fDecorated = !fOwned && !store.isBlocking() &&
                                 ExternalizableHelper.isDecorated((Binary) oValue,
                                    BackingMapManagerContext.DECO_STORE);
                    if (fOwned || fDecorated)
                        {
                        if (!store.isBlocking() && !fDecorated)
                            {
                            // non-blocking entry store;
                            // decorate the entry with a "store deferred" flag
                            oValue = ExternalizableHelper.decorate((Binary) oValue,
                                BackingMapManagerContext.DECO_STORE, BIN_STORE_PENDING);
                            }

                        Entry entry =
                            instantiateEntry(oKey, oValue, mapInternal.get(oKey), cMillis);

                        store.store(entry, false);

                        if (entry.isChanged())
                            {
                            oValue = entry.getChangedBinaryValue();

                            // due to technical reasons (event handling), synchronous
                            // removal (oValue == null) is not currently supported
                            // instead, we schedule the entry for almost
                            // instant expiry
                            cMillis = oValue == null ? 1L : extractExpiry(entry);
                            }
                        }
                    }
                else
                    {
                    if (fOwned)
                        {
                        // regular operation;
                        // decorate the entry with a "store deferred" flag
                        oValue = ExternalizableHelper.decorate((Binary) oValue,
                            BackingMapManagerContext.DECO_STORE, BIN_STORE_PENDING);
                        }
                    else
                        {
                        // failover; check if the value has already been stored
                        if (!ExternalizableHelper.isDecorated((Binary) oValue,
                                BackingMapManagerContext.DECO_STORE))
                            {
                            // absence of the decoration means that it has
                            // already been stored; no requeueing is necessary
                            queue = null;
                            }
                        else
                            {
                            // if we ever need to add statistics regarding a
                            // number of items that were re-queued due to a
                            // failover redistribution
                            }
                        }
                    }
                }

            // update the in-memory cache and queue if necessary
            if (queue != null)
                {
                queue.add(
                    instantiateEntry(oKey, oValue, mapInternal.get(oKey), cMillis), 0L);
                }

            return putToInternalMap(oKey, oValue, cMillis);
            }
        finally
            {
            mapControl.unlock(oKey);
            }
        }

    /**
    * Wait for notification on the specified object for no longer than
    * the specified wait time.
    * <p>
    * Note: the caller must hold synchronization on the object being waited
    * on.
    *
    * @param o         the object to wait for notification on
    * @param cMillis   the maximum time in milliseconds to wait;
    *                  pass 0 for forever
    *
    * @return true iff notification was received, the timeout has
    *         passed, or the thread was spuriously wakened;
    *         false if this thread was interrupted
    */
    protected boolean waitFor(Object o, long cMillis)
        {
        try
            {
            Blocking.wait(o, cMillis);
            }
        catch (InterruptedException e)
            {
            if (isActive())
                {
                throw Base.ensureRuntimeException(e);
                }
            return false;
            }
        return true;
        }

    /**
    * Issue a service guardian "heartbeat" for the current thread.
    */
    protected void heartbeat()
        {
        long cMillis = getCacheStoreTimeoutMillis();

        if (cMillis == 0)
            {
            GuardSupport.heartbeat();
            }
        else
            {
            GuardSupport.heartbeat(cMillis);
            }
        }

    /**
    * Return the expiration value for the given entry.
    *
    * @param entry  the entry
    *
    * @return the expiration value
    */
    protected long extractExpiry(Entry entry)
        {
        return entry == null ? CacheMap.EXPIRY_DEFAULT : entry.getExpiry();
        }

    /**
    * Append the provided name to the Daemon's thread name if not already appended.
    *
    * @param daemon  the Daemon to be modified
    * @param sName   the name to append to the Daemon's thread name
    */
    protected void updateThreadName(Daemon daemon, String sName)
        {
        if (daemon != null)
            {
            Thread thread    = daemon.getThread();
            String sNamePrev = thread.getName();
                   sName     = ":" + sName;

            if (!sNamePrev.endsWith(sName))
                {
                thread.setName(sNamePrev + sName);
                }
            }
        }

    // ----- inner class: EntrySet ------------------------------------------

    /**
    * Factory pattern: instantiate an entry set for this backing map.
    *
    * @return a new EntrySet object
    */
    protected EntrySet instantiateEntrySet()
        {
        return new EntrySet();
        }

    /**
    * A set of entries corresponding to this backing map.
    *
    * @author cp 2002.10.22
    */
    protected class EntrySet
            extends AbstractSet
        {
        // ----- Set interface ------------------------------------------

        /**
        * Returns an iterator over the elements contained in this collection.
        *
        * @return an iterator over the elements contained in this collection
        */
        public Iterator iterator()
            {
            if (ReadWriteBackingMap.this.isEmpty())
                {
                return NullImplementation.getIterator();
                }

            return new SimpleEnumerator(getInternalCache().keySet().toArray())
                {
                public Object next()
                    {
                    m_entryPrev = instantiateEntry(super.next());
                    return m_entryPrev;
                    }

                public void remove()
                    {
                    if (m_entryPrev == null)
                        {
                        throw new IllegalStateException();
                        }
                    else
                        {
                        ReadWriteBackingMap.this.
                            removeInternal(m_entryPrev.getKey(), true);
                        m_entryPrev = null;
                        }
                    }

                private Map.Entry m_entryPrev;
                };
            }

        /**
        * Returns the number of elements in this collection.  If the collection
        * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
        * <tt>Integer.MAX_VALUE</tt>.
        *
        * @return the number of elements in this collection
        */
        public int size()
            {
            int cPendingRemoves = getPendingRemoves() == null ? 0 : getPendingRemoves().size();
            return ReadWriteBackingMap.this.size() - cPendingRemoves;
            }

        /**
        * Returns <tt>true</tt> if this collection contains the specified
        * element.  More formally, returns <tt>true</tt> if and only if this
        * collection contains at least one element <tt>e</tt> such that
        * <tt>(o==null ? e==null : o.equals(e))</tt>.
        *
        * @param o  object to be checked for containment in this collection
        *
        * @return <tt>true</tt> if this collection contains the specified element
        */
        public boolean contains(Object o)
            {
            if (o instanceof Map.Entry)
                {
                Map.Entry entry = (Map.Entry) o;
                Object    oKey  = entry.getKey();

                ReadWriteBackingMap map = ReadWriteBackingMap.this;
                return  map.containsKey(oKey)
                    &&  Base.equals(entry.getValue(), map.get(oKey))
                    &&  map.containsKey(oKey); // verify not evicted
                }

            return false;
            }

        /**
        * Removes the specified element from this Set of entries if it is
        * present by removing the associated entry from the underlying
        * Map.
        *
        * @param o  object to be removed from this set, if present
        *
        * @return true if the set contained the specified element
        */
        public boolean remove(Object o)
            {
            ReadWriteBackingMap map     = ReadWriteBackingMap.this;
            Object              oKey    = ((Map.Entry) o).getKey();
            boolean             fExists = map.containsKey(oKey);

            // whether or not the entry exists; store.erase() should be called
            // in the same way as by the remove's behavior
            map.removeInternal(oKey, true);
            return fExists;
            }

        /**
        * Removes all of the elements from this set of Keys by clearing the
        * underlying Map.
        */
        public void clear()
            {
            ReadWriteBackingMap.this.clear();
            }

        /**
        * Returns an array containing all of the elements in this collection.  If
        * the collection makes any guarantees as to what order its elements are
        * returned by its iterator, this method must return the elements in the
        * same order. The returned array will be "safe" in that no references to
        * it are maintained by the collection.  (In other words, this method must
        * allocate a new array even if the collection is backed by an Array).
        * The caller is thus free to modify the returned array.
        *
        * @return an array containing all of the elements in this collection
        */
        public Object[] toArray()
            {
            return toArray((Object[]) null);
            }

        /**
        * Returns an array with a runtime type is that of the specified array and
        * that contains all of the elements in this collection.  If the
        * collection fits in the specified array, it is returned therein.
        * Otherwise, a new array is allocated with the runtime type of the
        * specified array and the size of this collection.
        *
        * If the collection fits in the specified array with room to spare (i.e.,
        * the array has more elements than the collection), the element in the
        * array immediately following the end of the collection is set to
        * <tt>null</tt>.  This is useful in determining the length of the
        * collection <i>only</i> if the caller knows that the collection does
        * not contain any <tt>null</tt> elements.)
        *
        * @param ao  the array into which the elements of the collection are
        *            to be stored, if it is big enough; otherwise, a new
        *            array of the same runtime type is allocated for this
        *            purpose
        *
        * @return an array containing the elements of the collection
        *
        * @throws ArrayStoreException if the runtime type of the specified array
        *         is not a supertype of the runtime type of every element in this
        *         collection
        */
        public Object[] toArray(Object[] ao)
            {
            Object[] aoKey = getInternalCache().keySet().toArray();
            int      cKeys = aoKey.length;

            // create the array to store the map contents
            if (ao == null)
                {
                // implied Object[] type, see toArray()
                ao = new Object[cKeys];
                }
            else if (ao.length < cKeys)
                {
                // if it is not big enough, a new array of the same runtime
                // type is allocated
                ao = (Object[]) Array.newInstance(ao.getClass().getComponentType(), cKeys);
                }
            else if (ao.length > cKeys)
                {
                // if the collection fits in the specified array with room to
                // spare, the element in the array immediately following the
                // end of the collection is set to null
                ao[cKeys] = null;
                }

            for (int i = 0; i < cKeys; ++i)
                {
                ao[i] = instantiateEntry(aoKey[i]);
                }

            return ao;
            }

        /**
        * Factory pattern: instantiate a Map.Entry object for the specified
        * key.
        *
        * @param oKey  the key
        *
        * @return a Map.Entry for the specified key
        */
        protected Map.Entry instantiateEntry(Object oKey)
            {
            return new SimpleMapEntry(oKey)
                {
                public Object getValue()
                    {
                    return ReadWriteBackingMap.this.get(getKey());
                    }

                public Object setValue(Object oValue)
                    {
                    return ReadWriteBackingMap.this.put(getKey(), oValue);
                    }
                };
            }
        }


    // ----- inner class: KeySet --------------------------------------------

    /**
    * Factory pattern: instantiate a key set for this backing map.
    *
    * @return a new KeySet object
    */
    protected KeySet instantiateKeySet()
        {
        return new KeySet();
        }

    /**
    * A set of entries backed by this backing map.
    */
    protected class KeySet
            extends AbstractSet
        {
        // ----- Set interface ------------------------------------------

        /**
        * Returns an iterator over the elements contained in this collection.
        *
        * @return an iterator over the elements contained in this collection
        */
        public Iterator iterator()
            {
            return new Iterator()
                {
                public boolean hasNext()
                    {
                    return m_iter.hasNext();
                    }

                public Object next()
                    {
                    Object oKey = m_iter.next();
                    m_oKeyPrev = oKey;
                    return oKey;
                    }

                public void remove()
                    {
                    if (m_oKeyPrev == null)
                        {
                        throw new IllegalStateException();
                        }
                    else
                        {
                        ReadWriteBackingMap.this.removeInternal(m_oKeyPrev, true);
                        m_oKeyPrev = null;
                        }
                    }

                private Iterator m_iter = getInternalCache().keySet().iterator();
                private Object m_oKeyPrev;
                };
            }

        /**
        * Determine the number of keys in the Set.
        *
        * @return the number of keys in the Set, which is the same as the
        *         number of entries in the underlying Map
        */
        public int size()
            {
            int cPendinRemoves = isWriteBehindRemove() ? getPendingRemoves().size() : 0;
            return ReadWriteBackingMap.this.getInternalCache().keySet().size() - cPendinRemoves;
            }

        /**
        * Determine if a particular key is present in the Set.
        *
        * @return true iff the passed key object is in the key Set
        */
        public boolean contains(Object oKey)
            {
            return ReadWriteBackingMap.this.containsKey(oKey);
            }

        /**
        * Removes the specified element from this Set of keys if it is
        * present by removing the associated entry from the underlying
        * Map.
        *
        * @param o  object to be removed from this set, if present
        *
        * @return true if the set contained the specified element
        */
        public boolean remove(Object o)
            {
            ReadWriteBackingMap map     = ReadWriteBackingMap.this;
            boolean             fExists = map.containsKey(o);

            // whether or not the entry exists; store.erase() should be called
            // in the same way as by the remove's behavior
            map.removeInternal(o, true);
            return fExists;
            }

        /**
        * Removes the provided collection from this Set of keys by removing
        * the associated entries from the underlying Map.
        *
        * @param colKeys  objects to be removed from this set, if present
        *
        * @return true if the Map was modified as a result of this call
        */
        public boolean removeAll(Collection colKeys)
            {
            return ReadWriteBackingMap.this.removeAll(colKeys);
            }

        /**
        * Removes all of the elements from this set of Keys by clearing the
        * underlying Map.
        */
        public void clear()
            {
            ReadWriteBackingMap.this.clear();
            }

        /**
        * Returns an array containing all of the keys in this set.
        *
        * @return an array containing all of the keys in this set
        */
        public Object[] toArray()
            {
            return getInternalCache().keySet().toArray();
            }

        /**
        * Returns an array with a runtime type is that of the specified array
        * and that contains all of the keys in this Set.  If the Set fits
        * in the specified array, it is returned therein. Otherwise, a new
        * array is allocated with the runtime type of the specified array
        * and the size of this collection.
        * <p>
        * If the Set fits in the specified array with room to spare (i.e.,
        * the array has more elements than the Set), the element in the
        * array immediately following the end of the Set is set to
        * <tt>null</tt>.  This is useful in determining the length of the
        * Set <i>only</i> if the caller knows that the Set does
        * not contain any <tt>null</tt> keys.)
        *
        * @param  ao  the array into which the elements of the Set are to
        *             be stored, if it is big enough; otherwise, a new array
        *             of the same runtime type is allocated for this purpose
        *
        * @return an array containing the elements of the Set
        *
        * @throws ArrayStoreException if the runtime type of the specified
        *         array is not a supertype of the runtime type of every
        *         element in this Set of keys
        */
        public Object[] toArray(Object ao[])
            {
            return getInternalCache().keySet().toArray(ao);
            }
        }


    // ----- inner class: ValuesCollection ----------------------------------

    /**
    * Factory pattern.
    *
    * @return a new instance of the ValuesCollection class (or subclass
    *         thereof)
    */
    protected ValuesCollection instantiateValuesCollection()
        {
        return new ValuesCollection();
        }

    /**
    * A collection of values backed by this map.
    */
    protected class ValuesCollection
            extends AbstractCollection
        {
        // ----- Collection interface -----------------------------------

        /**
        * Obtain an iterator over the values in the Map.
        *
        * @return an Iterator that provides a live view of the values in the
        *         underlying Map object
        */
        public Iterator iterator()
            {
            return new Iterator()
                {
                public boolean hasNext()
                    {
                    return m_iter.hasNext();
                    }

                public Object next()
                    {
                    return ReadWriteBackingMap.this.get(m_iter.next());
                    }

                public void remove()
                    {
                    m_iter.remove();
                    }

                private Iterator m_iter =
                    ReadWriteBackingMap.this.keySet().iterator();
                };
            }

        /**
        * Determine the number of values in the Collection.
        *
        * @return the number of values in the Collection, which is the same
        *         as the number of entries in the underlying Map
        */
        public int size()
            {
            int cPendinRemoves = isWriteBehindRemove() ? getPendingRemoves().size() : 0;
            return ReadWriteBackingMap.this.size() - cPendinRemoves;
            }

        /**
        * Removes all of the elements from this Collection of values by
        * clearing the underlying Map.
        */
        public void clear()
            {
            ReadWriteBackingMap.this.clear();
            }

        /**
        * Returns an array containing all of the keys in this collection.
        *
        * @return an array containing all of the keys in this collection
        */
        public Object[] toArray()
            {
            return getInternalCache().values().toArray();
            }

        /**
        * Returns an array with a runtime type is that of the specified array
        * and that contains all of the keys in this Collection.  If the
        * Collection fits in the specified array, it is returned therein.
        * Otherwise, a new array is allocated with the runtime type of the
        * specified array and the size of this collection.
        * <p>
        * If the Collection fits in the specified array with room to spare
        * (i.e., the array has more elements than the Collection), the
        * element in the array immediately following the end of the
        * Collection is set to <tt>null</tt>.  This is useful in determining
        * the length of the Collection <i>only</i> if the caller knows that
        * the Collection does not contain any <tt>null</tt> elements.)
        *
        * @param  ao  the array into which the elements of the Collection are
        *             to be stored, if it is big enough; otherwise, a new
        *             array of the same runtime type is allocated for this
        *             purpose
        *
        * @return an array containing the elements of the Collection
        *
        * @throws ArrayStoreException if the runtime type of the specified
        *         array is not a supertype of the runtime type of every
        *         element in this Collection of values
        */
        public Object[] toArray(Object ao[])
            {
            return getInternalCache().values().toArray(ao);
            }
        }


    // ----- ObservableMap interface ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public void addMapListener(MapListener listener)
        {
        addMapListener(listener, null, false);
        }

    /**
    * {@inheritDoc}
    */
    public void removeMapListener(MapListener listener)
        {
        removeMapListener(listener, null);
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


    // ----- Object methods -------------------------------------------------

    /**
    * Compares the specified object with this map for equality.  Returns
    * <tt>true</tt> if the given object is also a map and the two maps
    * represent the same mappings.
    *
    * @param o  object to be compared for equality with this map
    *
    * @return <tt>true</tt> if the specified object is equal to this map
    */
    public boolean equals(Object o)
        {
        return o == this || o instanceof Map && getInternalCache().equals(o);
        }

    /**
    * Returns the hash code value for this map.
    *
    * @return the hash code value for this map
    */
    public int hashCode()
        {
        return getInternalCache().hashCode();
        }

    /**
    * For debugging purposes, format the contents of the Map in a human-
    * readable format.
    *
    * @return a String representation of this ReadWriteBackingMap
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) +
            '{' + getInternalCache() + '}';
        }


    // ----- internal cache -------------------------------------------------

    /**
    * Get the representative of the "in-memory" storage for this backing map.
    *
    * @return the ObservableMap object (never null) that this backing map
    *         uses to store entries
    */
    public ObservableMap getInternalCache()
        {
        return m_mapInternal;
        }

    /**
    * Get the map that provides internal storage for this backing map. If the
    * internal storage is a ConfigurableCacheMap, then this accessor returns the
    * same reference as {@link #getInternalCache}; otherwise it returns null.
    * The refresh-ahead implementation relies on the internal storage
    * providing the ConfigurableCacheMap interface, so this method will
    * always return a non-null value if refresh-ahead is enabled.
    *
    * @return the cache for this backing map or null if the internal map is
    *         not an implementation of the ConfigurableCacheMap interface
    */
    protected ConfigurableCacheMap getInternalConfigurableCache()
        {
        Map mapInternal = m_mapInternal;
        return mapInternal instanceof ConfigurableCacheMap
                ? (ConfigurableCacheMap) mapInternal : null;
        }

    /**
    * Configure the internal cache that this backing map uses to store its
    * "in-memory" data.
    *
    * @param mapInternal  the internal map
    */
    protected void configureInternalCache(ObservableMap mapInternal)
        {
        m_mapInternal      = mapInternal;
        m_mapControl       = instantiateControlMap();
        m_listenerInternal = instantiateInternalListener();
        mapInternal.addMapListener(getInternalListener());
        }

    /**
    * Get the optional map used to cache CacheLoader (or CacheStore) misses.
    *
    * @return the Map that this backing map uses to cache CacheLoader (or
    *         CacheStore) misses or null if misses are not cached
    */
    public Map getMissesCache()
        {
        return m_mapMisses;
        }

    /**
     * Get the pending removes key set for the CacheStore used by this
     * backing map.
     *
     * @return the key set of pending removes for the CacheStore
     *
     * @since 12.2.1.4.18
     */
    public Set getPendingRemoves()
        {
        return m_setPendingRemoves;
        }

    /**
    * Get the concurrency control map for this backing map.
    *
    * @return the ObservableMap object (never null) that this backing map
    *         uses to store entries
    */
    public ConcurrentMap getControlMap()
        {
        return m_mapControl;
        }

    /**
    * Factory pattern: Create the concurrency control map for this backing map.
    *
    * @return a new concurrency control map
    */
    protected ConcurrentMap instantiateControlMap()
        {
        return new SegmentedConcurrentMap();
        }

    /**
    * Get the map of keys for which the events should be marked as
    * synthetic (internal).
    *
    * @return the map of keys to mark events as internal
    */
    protected Map getSyntheticEventsMap()
        {
        Map map = m_mapSyntheticEvents;
        if (map == null)
            {
            synchronized (this)
                {
                map = m_mapSyntheticEvents;
                if (map == null)
                    {
                    map = m_mapSyntheticEvents = new SafeHashMap();
                    }
                }
            }
        return map;
        }


    // ----- inner class: InternalMapListener -------------------------------

    /**
    * Obtain the MapListener that listens to the internal cache and routes
    * those events to anyone listening to this ReadWriteBackingMap, creating
    * such a listener if one does not already exist.
    *
    * @return a routing MapListener
    */
    protected MapListener getInternalListener()
        {
        return m_listenerInternal;
        }

    /**
    * Factory pattern: Create a MapListener that listens to the internal
    * cache and routes those events to anyone listening to this
    * ReadWriteBackingMap.
    *
    * @return a new routing MapListener
    */
    protected MapListener instantiateInternalListener()
        {
        return new InternalMapListener();
        }

    /**
    * A MapListener implementation that listens to the internal cache and
    * routes those events to anyone listening to this ReadWriteBackingMap.
    *
    * @author cp 2002.10.22
    */
    protected class InternalMapListener
            extends Base
            implements MapListener
        {
        /**
        * Invoked when a map entry has been inserted.
        */
        public void entryInserted(MapEvent evt)
            {
            // notify any listeners listening to this backing map
            dispatch(evt);
            }

        /**
        * Invoked when a map entry has been updated.
        */
        public void entryUpdated(MapEvent evt)
            {
            // notify any listeners listening to this backing map
            dispatch(evt);
            }

        /**
        * Invoked when a map entry has been removed.
        */
        public void entryDeleted(MapEvent evt)
            {
            if (isWriteBehind())
                {
                // most commonly, the installed eviction approver would not allow
                // eviction of not-yet-persisted entries; however in a very rare
                // case when the internal map is not configurable, or the approver
                // has been changed outside of our control we may need to ensure
                // that the corresponding entry has been persisted
                ConfigurableCacheMap                  mapInternal      = getInternalConfigurableCache();
                ConfigurableCacheMap.EvictionApprover approver         = mapInternal == null ? null : mapInternal.getEvictionApprover();
                boolean                               fApproverChanged = mapInternal != null &&
                        approver != ConfigurableCacheMap.EvictionApprover.DISAPPROVER &&
                        approver != f_writeBehindDisapprover;

                if (mapInternal == null || fApproverChanged)
                    {
                    if (fApproverChanged)
                        {
                        err(String.format("The internal map of a ReadWriteBackingMap has an " +
                            "unexpected EvictionApprover(type=%s); custom maps should accept " +
                            "and use the supplied approver.", (approver == null ? null : approver.getClass().getName())));
                        }

                    ConcurrentMap mapControl = getControlMap();
                    Object        oKey       = evt.getKey();
                    if (mapControl.lock(oKey, 500L))
                        {
                        try
                            {
                            if (getContext().isKeyOwned(oKey))
                                {
                                processDeletedEntry(oKey, evt.getOldValue());
                                }
                            }
                        finally
                            {
                            mapControl.unlock(oKey);
                            }
                        }
                    else
                        {
                        Object oValueOld = evt.getOldValue();
                        Object oValue    = getInternalCache().put(oKey, oValueOld);
                        if (oValue != null && !equals(oValue, oValueOld))
                            {
                            String sCulprit = getCacheStore() == null ?
                                    "backing map" : "cache store";

                            err("Due to an exceptionally long " + sCulprit +
                                " operation an eviction event cannot be processed" +
                                " in order. Canceling the eviction: " + evt);
                            }
                        return;
                        }
                    }
                }

            // notify any listeners listening to this backing map
            dispatch(evt);
            }

        // ----- helpers ----------------------------------------------------

        /**
        * Process an entry that is about to be removed from the internal cache.
        * This method is called after the entry has been successfully locked, but
        * before any listeners are notified.
        * <p>
        * If the entry is queued to be inserted or updated, then that must occur
        * (be persisted) <b>before</b> we notify any listeners that it has been
        * removed from the internal cache, otherwise (for example) if this server
        * dies and it has the only copy of the pending update then the update
        * will be lost!
        *
        * @param oKey       the key
        * @param oValueOld  the old value
        */
        protected void processDeletedEntry(Object oKey, Object oValueOld)
            {
            StoreWrapper store = getCacheStore();
            if (store != null && !isReadOnly())
                {
                Entry entry = removeFromWriteQueue(oKey);
                if (entry != null)
                    {
                    // the store operation may throw an exception if the
                    // RWBM is configured to do so; to preserve the
                    // behavior of the RWBM prior to COH-125, catch the
                    // exception and log a message
                    try
                        {
                        store.store(entry, false);
                        }
                    catch (WrapperException e)
                        {
                        log(e);
                        }
                    }
                }
            }

        /**
        * Dispatch the event to the corresponding listeners.
        *
        * @param evt  the MapEvent object
        */
        protected void dispatch(final MapEvent evt)
            {
            MapListenerSupport support = m_listenerSupport;
            if (support != null)
                {
                Object  oKey       = evt.getKey();
                boolean fSynthetic =
                    (evt instanceof CacheEvent && ((CacheEvent)evt).isSynthetic()) ||
                    getSyntheticEventsMap().containsKey(oKey);

                MapEvent evtNew = new CacheEvent(ReadWriteBackingMap.this, evt.getId(),
                                                 oKey, null, null, fSynthetic,
                                                 CacheEvent.TransformationState.TRANSFORMABLE, false,
                                                 (evt instanceof CacheEvent && ((CacheEvent) evt).isExpired()))
                    {
                    public Object getOldValue()
                        {
                        return evt.getOldValue();
                        }
                    public Object getNewValue()
                        {
                        return evt.getNewValue();
                        }
                    };
                support.fireEvent(evtNew, true);
                }
            }
        }


    // ----- life cycle -----------------------------------------------------

    /**
    * Release the backing map when it is no longer being used.
    */
    public void release()
        {
        if (isActive())
            {
            try
                {
                getInternalCache().removeMapListener(getInternalListener());
                }
            catch (Exception e)
                {
                Base.err("An exception occurred while removing an internal"
                        + " listener during release:");
                Base.err(e);
                }

            if (isRefreshAhead())
                {
                terminateReadThread();
                }

            if (isWriteBehind())
                {
                terminateWriteThread();
                }

            m_store   = null;
            m_fActive = false;
            }
        }

    /**
    * Determine if the backing map is still active.
    *
    * @return true if the backing map is still active
    */
    public boolean isActive()
        {
        return m_fActive;
        }


    // ----- inner class: ReadLatch -----------------------------------------

    /**
    * Factory pattern: Instantiate a new read latch the given key.
    *
    * @param oKey  the key
    *
    * @return the read latch
    */
    protected ReadLatch instantiateReadLatch(Object oKey)
        {
        return new ReadLatch(oKey);
        }

    /**
    * A synchronization construct used to coordinate asynchronous loads by the
    * refresh-ahead thread with cache accesses by other threads.
    * <p>
    * The refresh-ahead thread places a new <tt>ReadLatch</tt> in the control
    * map before performing a load operation on the cache store. The presence of
    * the latch signals to a thread executing the {@link ReadWriteBackingMap#get}
    * method that an asynchronous load is in progress. This thread can then
    * wait on the latch to get the results of the asynchronous load. This thread
    * is then responsible for removing the latch from the control map.
    * <p>
    * Additionally, a thread performing a {@link ReadWriteBackingMap#put} or
    * {@link ReadWriteBackingMap#remove} operation can cancel an ongoing
    * asynchronous load using the latch. This thread is also responsible for
    * removing the latch from the control map.
    *
    * @author jh 2005.02.11
    */
    protected static class ReadLatch
        {
        // ----- constructors -------------------------------------------

        /**
        * Create a new <tt>ReadLatch</tt> for the specified key.
        *
        * @param oKey  the key that is being loaded by the refresh-ahead thread
        */
        protected ReadLatch(Object oKey)
            {
            m_oKey = oKey;
            }


        // ----- latch operations ---------------------------------------

        /**
        * Cancel the load operation. This method has no effect if the operation
        * has already been completed or canceled.
        */
        public synchronized void cancel()
            {
            cancel(null);
            }

        /**
        * Cancel the load operation due to an exception. This method has no
        * effect if the operation has already been completed or canceled.
        *
        * @param t  the exception responsible for cancelling the load
        */
        public synchronized void cancel(Throwable t)
            {
            if (!m_fCanceled && !m_fComplete)
                {
                m_oValue    = null;
                m_throwable = t;
                m_fCanceled = true;
                m_fComplete = true;
                notifyAll();
                }
            }

        /**
        * Complete the load operation. The specified value and entry is the
        * result of the load operation. This method has no effect if the
        * operation has already been completed or canceled.
        *
        * @param oValue  the result of the load operation
        */
        public synchronized void complete(Object oValue)
            {
            if (!m_fCanceled && !m_fComplete)
                {
                m_oValue    = oValue;
                m_fComplete = true;
                notifyAll();
                }
            }


        // ----- accessors ----------------------------------------------

        /**
        * Return <tt>true</tt> if the load operation is complete. The results
        * of the load operation can be retrieved using the {@link #getValue}
        * method.
        *
        * @return <tt>true</tt> if the load operation is complete
        */
        public boolean isComplete()
            {
            return m_fComplete;
            }

        /**
        * Return <tt>true</tt> if the load operation has been canceled.
        *
        * @return <tt>true</tt> if the load operation has been canceled
        */
        public boolean isCanceled()
            {
            return m_fCanceled;
            }

        /**
        * Return the key that is being loaded by the refresh-ahead thread.
        *
        * @return the key that is being loaded
        */
        public Object getKey()
            {
            return m_oKey;
            }

        /**
        * Return the result of the load operation.
        * <p>
        * Note: this method should not be called by the refresh-ahead daemon
        *       thread
        *
        * @return the result of the load operation
        */
        public synchronized Object getValue()
            {
            Throwable throwable = m_throwable;
            if (throwable != null && m_fCanceled)
                {
                throw Base.ensureRuntimeException(throwable);
                }
            return m_oValue;
            }

        // ----- data members -------------------------------------------

        /**
        * Flag that indicates whether or not the load operation has completed.
        */
        private volatile boolean m_fComplete;

        /**
        * Flag that indicates whether or not the load operation has been
        * canceled.
        */
        private volatile boolean m_fCanceled;

        /**
        * The key that is being loaded by the refresh-ahead thread.
        */
        private Object m_oKey;

        /**
        * The result of the load operation.
        */
        private Object m_oValue;

        /**
        * A Throwable associated with a canceled operation.
        */
        private Throwable m_throwable;
        }


    // ----- inner class: ReadQueue (refresh-ahead queue) -------------------

    /**
    * Get the queue of keys that are yet to be read.
    *
    * @return the refresh-ahead queue object
    */
    public ReadQueue getReadQueue()
        {
        return m_queueRead;
        }

    /**
    * Factory pattern: Instantiate a new <tt>ReadQueue</tt> object.
    *
    * @return a new <tt>ReadQueue</tt> object
    */
    protected ReadQueue instantiateReadQueue()
        {
        return new ReadQueue();
        }

    /**
    * A queue of keys that should be read from the underlying
    * <tt>CacheStore</tt>.
    *
    * @author jh 2005.02.08
    */
    public class ReadQueue
            extends CoherenceCommunityEdition
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a <tt>ReadQueue</tt>.
        */
        protected ReadQueue()
            {
            }

        // ----- Queue API ----------------------------------------------

        /**
        * Add a key to the queue. This method has no effect if the key is
        * already queued.
        *
        * @param oKey  the key object
        *
        * @return true if the key was added to the queue; false otherwise
        */
        public synchronized boolean add(Object oKey)
            {
            Map map = getKeyMap();
            if (map.get(oKey) == null)
                {
                map.put(oKey, oKey);

                List    list      = getKeyList();
                boolean fWasEmpty = list.isEmpty();
                list.add(oKey);
                if (fWasEmpty)
                    {
                    this.notify();  // @see peek()
                    }

                return true;
                }
            return false;
            }

        /**
        * Wait for a key to be added to the queue and return it without
        * removing it from the queue.
        *
        * @return the next item in the queue (it will only return null when the
        *         backing map is no longer active)
        */
        public Object peek()
            {
            return peek(-1);
            }

        /**
        * Wait for a key (up to the specified wait time) to be added to the
        * queue and return it without removing it from the queue, or null if
        * the specified wait time has passed).
        *
        * @param cMillis  the number of ms to wait for a key in the queue; pass
        *                 -1 to wait indefinitely or 0 for no wait
        *
        * @return the next item in the queue, or null if the wait time has passed
        *         or if the backing map is no longer active
        */
        public synchronized Object peek(long cMillis)
            {
            List list = getKeyList();
            while (true)
                {
                if (!isActive())
                    {
                    return null;
                    }

                if (!list.isEmpty())
                    {
                    return list.get(0);
                    }
                else if (cMillis == 0L)
                    {
                    return null;
                    }

                // cap the wait time so that during shutdown the
                // thread will eventually recheck the active flag
                long cWait = (cMillis < 0L || cMillis > 0xFFL) ? 0xFFL : cMillis;
                waitFor(this, cWait);

                if (cMillis > 0L)
                    {
                    // if we run out of time, set cMillis to 0 so we
                    // check the list one last time before giving up
                    cMillis = Math.max(0L, cMillis - cWait);
                    }
                }
            }

        /**
        * Remove a key from the queue if the key is in the queue.
        *
        * @param oKey the key object
        *
        * @return true if the key was removed from the queue; false otherwise
        */
        public synchronized boolean remove(Object oKey)
            {
            if (getKeyMap().remove(oKey) != null)
                {
                getKeyList().remove(oKey);
                return true;
                }
            return false;
            }

        /**
        * Select the next key from the refresh-ahead queue that is a candidate
        * for an asynchronous load. A key is a candidate if it can be locked
        * "quickly".
        * <p>
        * This method performs the selection process by iterating through the
        * refresh-ahead queue starting with the first key in the queue. If the
        * queue is empty, this method will block until a key is added to the
        * queue. A key is skipped if it cannot be locked within the specified
        * wait time.
        * <p>
        * If a candidate key is found, a new <tt>ReadLatch</tt> for the key is
        * placed in the control map and returned; otherwise, <tt>null</tt> is
        * returned.
        *
        * @param cWaitMillis the maximum amount of time (in milliseconds) to
        *                    wait to select a key and acquire a latch on it;
        *                    pass -1 to wait indefinitely
        *
        * @return a <tt>ReadLatch</tt> for the selected key or <tt>null</tt> if
        *         a candidate key was not found and latched within the specified
        *         time
        */
        protected ReadLatch select(long cWaitMillis)
            {
            List          listKeys   = getKeyList();
            ConcurrentMap mapControl = getControlMap();
            Object        oKey;

            if (cWaitMillis == -1L)
                {
                oKey = peek(-1);
                }
            else
                {
                long ldtStart = Base.getSafeTimeMillis();
                oKey = peek(cWaitMillis);
                cWaitMillis -= Math.max(0L, Base.getSafeTimeMillis() - ldtStart);
                }
            if (oKey == null)
                {
                // couldn't find an entry within the wait time
                return null;
                }

            do
                {
                long cWaitLatch = 0L;
                int  index      = 0;
                while (oKey != null)
                    {
                    boolean fRemoved = false;

                    // make sure the key can be locked within the specified time
                    if (mapControl.lock(oKey, cWaitLatch))
                        {
                        try
                            {
                            // make sure the key is still in the queue; it could
                            // have been removed by a call to get(), put(), or
                            // remove() before we had a chance to lock the key
                            if (remove(oKey))
                                {
                                ReadLatch latch = instantiateReadLatch(oKey);
                                mapControl.put(oKey, latch);
                                return latch;
                                }
                            else
                                {
                                fRemoved = true;
                                }
                            }
                        finally
                            {
                            mapControl.unlock(oKey);
                            }
                        }

                    // adjust the wait time
                    cWaitMillis -= cWaitLatch;
                    if (cWaitMillis < 0)
                        {
                        // ran out of time
                        break;
                        }

                    // get the next key in the queue
                    try
                        {
                        oKey = listKeys.get(fRemoved ? index : ++index);
                        }
                    catch (IndexOutOfBoundsException e)
                        {
                        break;
                        }
                    cWaitLatch += 10L;
                    }
                }
            while (cWaitMillis != 0L);

            return null;
            }

        /**
        * Remove all keys from the queue.
        */
        public synchronized void clear()
            {
            getKeyMap().clear();
            getKeyList().clear();
            }

        /**
        * Return the length of the queue.
        *
        * @return the length of the queue
        */
        public int size()
            {
            return getKeyList().size();
            }

        /**
        * For debugging purposes, present the queue in human-readable format.
        *
        * @return a <tt>String</tt> representation of this object
        */
        public String toString()
            {
            return "ReadQueue: " + getKeyList();
            }

        // ----- internal -----------------------------------------------

        /**
        * Return a list of keys in the queue.
        *
        * @return a list of keys in the queue
        */
        protected List getKeyList()
            {
            return m_listQueued;
            }

        /**
        * Return a map of keys in the queue.
        * <p>
        * Note: The map returned from this method is not thread-safe; therefore,
        * a lock on this <tt>ReadQueue</tt> must be obtained before accessing
        * the map
        *
        * @return a map of keys in the queue
        */
        protected Map<Object, Span> getKeyMap()
            {
            return m_mapQueuedReads;
            }

        // ----- data members -------------------------------------------

        /**
        * The queue (ordered list) of keys.
        */
        private List m_listQueued     = new RecyclingLinkedList();

        /**
        * Map of key objects in the queue; allows quick lookup to determine if
        * a key is or is not already queued.
        */
        private Map  m_mapQueuedReads = new HashMap();
        }


    // ----- inner class: WriteQueue (write-behind queue) -------------------

    /**
    * Get the queue of entries that are yet to be written.
    *
    * @return the write-behind queue object
    */
    public WriteQueue getWriteQueue()
        {
        return m_queueWrite;
        }

    /**
    * Flush the write-behind queue, writing everything immediately.
    */
    public void flush()
        {
        WriteQueue   queue = getWriteQueue();
        StoreWrapper store = getCacheStore();
        if (queue != null && store != null && !isReadOnly())
            {
            // prevent the WriteThread from being shutdown, which invalidates
            // the queue; forces the shutdown to wait until the flush is completed
            synchronized (m_daemonWrite)
                {
                if (queue == getWriteQueue())
                    {
                    flush(queue, store);
                    }
                }
            }
        }

    /**
    * Flush the write-behind queue, writing everything immediately.
    *
    * @param queue  the write-behind queue to flush
    * @param store  the CacheStore to flush to
    */
    protected void flush(WriteQueue queue, StoreWrapper store)
        {
        Base.azzert(queue != null && store != null);

        ConcurrentMap            mapControl = getControlMap();
        BackingMapManagerContext ctx        = getContext();

        // try to LOCK_ALL; if successful then we can flush using storeAll();
        // otherwise store entries individually
        boolean fBatch   = mapControl.lock(ConcurrentMap.LOCK_ALL, 10L);
        Set     setBatch = fBatch ? new LinkedHashSet() : null;

        try
            {
            for (Entry entry = queue.removeImmediate();
                    entry != null && isActive(); entry = queue.removeImmediate())
                {
                Binary binKey = entry.getBinaryKey();
                if (ctx.isKeyOwned(binKey))
                    {
                    if (fBatch)
                        {
                        setBatch.add(entry);
                        }
                    else
                        {
                        mapControl.lock(binKey, -1L);
                        try
                            {
                            // issue the CacheStore operation; the store operation may
                            // throw an exception if the RWBM is configured to do so; to
                            // preserve the behavior of the RWBM prior to COH-125, catch
                            // the exception and log a message
                            store.store(entry, true);
                            }
                        catch (WrapperException e)
                            {
                            Base.err(e);
                            }
                        finally
                            {
                            mapControl.unlock(binKey);
                            }
                        }
                    }
                }

            if (fBatch)
                {
                try
                    {
                    store.storeAll(setBatch);
                    }
                catch (WrapperException e)
                    {
                    Base.err(e);
                    }
                }
            }
        finally
            {
            if (fBatch)
                {
                mapControl.unlock(ConcurrentMap.LOCK_ALL);
                }
            }

        // COH-10078: If the write thread is still in the process of completing a
        //            store/storeAll operation, we need to wait util it finishes.
        synchronized (queue)
            {
            while (!queue.getPendingMap().isEmpty())
                {
                queue.setWaitingOnPending(true);
                waitFor(queue, 0xFFL);
                }
            }
        }

    /**
    * Remove the specified entry from the WriteQueue.
    *
    * @param binKey  the key
    *
    * @return the currently queued entry (could be NO_VALUE marker or null)
    */
    protected Entry removeFromWriteQueue(Object binKey)
        {
        WriteQueue queue = getWriteQueue();
        return queue == null ? null : queue.remove(binKey);
        }


    // ----- queue entry --------------------------------------------

    /**
    * Factory pattern: instantiate a queue entry.
    *
    * @param oKey        the key for the new entry
    * @param oValue      the entry's value; could be null representing a
    *                    non-existing or removed entry
    * @param oValueOrig  the entry's original value; could be null representing
    *                    a non-existing entry
    *
    * @return a new Entry
    */
    protected Entry instantiateEntry(Object oKey, Object oValue, Object oValueOrig)
        {
        return instantiateEntry(oKey, oValue, oValueOrig, CacheMap.EXPIRY_DEFAULT);
        }

    /**
    * Factory pattern: instantiate a queue entry.
    *
    * @param oKey        the key for the new entry
    * @param oValue      the entry's value; could be null representing a
    *                    non-existing or removed entry
    * @param oValueOrig  the entry's original value; could be null representing
    *                    a non-existing entry
    * @param cExpiry     the expiry delay, or {@link CacheMap#EXPIRY_NEVER} or
    *                    {@link CacheMap#EXPIRY_DEFAULT}
    *
    * @return a new Entry
    */
    protected Entry instantiateEntry(Object oKey, Object oValue, Object oValueOrig, long cExpiry)
        {
        return new Entry((Binary) oKey, (Binary) oValue, (Binary) oValueOrig, cExpiry, getContext());
        }

    /**
    * A queue entry that is scheduled to come out of the front of the
    * queue no earlier than some specific point in time.
    *
    * @since Coherence 3.6
    */
    public class Entry
            extends BackingMapBinaryEntry
        {
        /**
        * Construct an Entry with just a key.
        *
        * @param binKey        the Binary key
        * @param binValue      the Binary value; could be null representing a
        *                      non-existing or removed entry
        * @param binValueOrig  an original Binary value; could be null
        *                      representing a non-existing entry
        * @param cExpiry       the expiry delay
        * @param ctx           the BackingMapManagerContext
        */
        public Entry(Binary binKey, Binary binValue, Binary binValueOrig,
                     long cExpiry, BackingMapManagerContext ctx)
            {
            super(binKey, binValue, binValueOrig, cExpiry, ctx);
            f_spanParent = TracingHelper.getActiveSpan();
            }

        // ----- BinaryEntry methods --------------------------------------

        /**
        * {@inheritDoc}
        */
        public Object setValue(Object oValue)
            {
            if (m_fTrackChanges)
                {
                m_binChangedValue = (Binary) getContext().
                        getValueToInternalConverter().convert(oValue);
                return null; // ignore the "old value" contract
                }
            else
                {
                return super.setValue(oValue);
                }
            }

        /**
        * {@inheritDoc}
        */
        public void updateBinaryValue(Binary binValue)
            {
            Span entrySpan = getParentSpan();
            if (!TracingHelper.isNoop(entrySpan))
                {
                SpanContext   entryContext   = entrySpan.getContext();
                Span          currentSpan    = TracingHelper.getActiveSpan();

                if (currentSpan != null)
                    {
                    //noinspection ConstantConditions
                    SpanContext currentContext = currentSpan.getContext();
                    StringBuilder sb = new StringBuilder(128);
                    sb.append("Span")
                            .append("[trace-id=")
                            .append(entryContext.getTraceId())
                            .append(", span-id=")
                            .append(entryContext.getSpanId())
                            .append("] associated with this queued entry has been updated by this operation")
                            .append("[trace-id=")
                            .append(currentContext.getTraceId())
                            .append(", span-id=")
                            .append(currentContext.getSpanId())
                            .append("] prior to flush.");
                    currentSpan.log(sb.toString());
                    }
                }

            if (m_fTrackChanges)
                {
                // null indicates the remove operation
                m_binChangedValue = binValue == null ? REMOVED : binValue;
                }
            else
                {
                super.updateBinaryValue(binValue);
                }
            }

        /**
        * {@inheritDoc}
        */
        public ObservableMap getBackingMap()
            {
            return ReadWriteBackingMap.this;
            }

        /**
        * {@inheritDoc}
        */
        public void expire(long cMillis)
            {
            if (m_fTrackChanges && cMillis != getExpiry())
                {
                m_fExpiryChanged = true;
                }
            super.expire(cMillis);
            }

        // ----- customization --------------------------------------------

        /**
        * Determine when the entry becomes ripe to be persisted.
        *
        * @return the time at which the entry becomes ripe
        */
        public long getRipeMillis()
            {
            return m_ldtRipeMillis;
            }

        /**
        * Specify the times when the entry has to be persisted. This
        * property is immutable once set.
        *
        * @param ldtMillis  the time when the entry becomes ripe
        */
        protected void setRipeMillis(long ldtMillis)
            {
            m_ldtRipeMillis = ldtMillis;
            }

        /**
        * Specifies whether or not the underlying value has been changed during
        * BinaryEntryStore operations.
        *
        * @return true iff during the BinaryEntryStore operations any of the
        *         value mutating methods were called (including remove)
        */
        public boolean isChanged()
            {
            return m_binChangedValue != null || m_fExpiryChanged;
            }

        /**
        * Return the Binary value changed by the BinaryEntryStore.
        *
        * @return the changed Binary value, or null if the entry was removed
        */
        public Binary getChangedBinaryValue()
            {
            Binary binValue = m_binChangedValue;

            if (isChanged() && binValue == null)
                {
                // only the expiry has changed
                return getBinaryValue();
                }
            return binValue == REMOVED ? null : binValue;
            }

        /**
        * Start tracking changes by the BinaryEntryStore.
        */
        protected void startTracking()
            {
            m_fTrackChanges = true;
            }

        /**
        * Stop tracking changes by the BinaryEntryStore.
        */
        protected void stopTracking()
            {
            m_fTrackChanges = false;
            }

        /**
        * Return the associated parent Span if any.
        *
        * @return the parent span or null
        */
        protected Span getParentSpan()
            {
            return f_spanParent;
            }


        // ----- fields ---------------------------------------------------

        /**
        * Time when the entry has to be persisted.
        */
        private long m_ldtRipeMillis;

        /**
        * Indicates that the value change tracking is on.
        */
        private boolean m_fTrackChanges;

        /**
        * Holds the binary value changed by the BinaryEntryStore.
        */
        private Binary m_binChangedValue;

        /**
        * Indicates that the expiry value has been changed.
        */
        private boolean m_fExpiryChanged;

        /**
        * The parent tracing span.
        */
        protected final Span f_spanParent;
        }

    /**
    * Factory pattern: Instantiate a new WriteQueue object.
    *
    * @return a new WriteQueue object
    */
    protected WriteQueue instantiateWriteQueue()
        {
        return new WriteQueue();
        }

    /**
    * A queue that only releases entries after a configurable period of time.
    *
    * @author cp 2002.10.22
    */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public class WriteQueue
            extends CoherenceCommunityEdition
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a WriteQueue that holds write-behind entries.
        */
        protected WriteQueue()
            {
            }


        // ----- accessors ----------------------------------------------

        /**
        * @return the number of seconds that an entry added to the queue will
        *         sit in the queue before being removable ("ripe")
        */
        public int getDelaySeconds()
            {
            long cMillis = getDelayMillis();
            return cMillis == 0 ? 0 : Math.max(1, (int) (cMillis / 1000)) ;
            }

        /**
        * Specify the number of seconds that an entry added to the queue will
        * sit in the queue before being removable ("ripe").
        *
        * @param cSeconds  the number of seconds to wait before allowing an
        *                  entry to be removed from the queue
        */
        public void setDelaySeconds(int cSeconds)
            {
            setDelayMillis(1000L * cSeconds);
            }

        /**
        * @return the number of milliseconds that an entry added to the queue
        *         will sit in the queue before being removable ("ripe")
        *
        * @since Coherence 3.4
        */
        public long getDelayMillis()
            {
            return m_cDelayMillis;
            }

        /**
        * Specify the number of milliseconds that an entry added to the queue
        * will sit in the queue before being removable ("ripe").
        *
        * @param cMillis  the number of milliseconds to wait before allowing an
        *                  entry to be removed from the queue
        *
        * @since Coherence 3.4
        */
        public synchronized void setDelayMillis(long cMillis)
            {
            m_cDelayMillis = Math.max(1, cMillis);
            this.notify();  // @see remove()
            }


        // ----- Queue API ----------------------------------------------

        /**
        * Add an entry to the queue. By specifying a cDelay that is greater
        * than 0, persisting of this entry will be delayed with the specified
        * number of milliseconds. Note that the delay will always be adjusted
        * to make sure that this entry ripes no earlier than any existing ones.
        *
        * @param entryNew  the entry to insert
        * @param cDelay    the number of milliseconds until the entry is
        *                  considered ripe
        *
        * @return an old entry for the same key, if such an entry existed at
        *         the time this method was invoked; null otherwise
        */
        protected synchronized Entry add(Entry entryNew, long cDelay)
            {
            Map    map    = getEntryMap();
            Binary binKey = entryNew.getBinaryKey();
            Entry  entry  = (Entry) map.get(binKey);

            if (entry == null)
                {
                cDelay = Math.max(getDelayMillis(), cDelay);

                LongArray arrayRipe   = getRipeArray();
                boolean   fWasEmpty   = arrayRipe.isEmpty();
                long      ldtNow      = getSafeTimeMillis();
                long      ldtRipe     = (ldtNow + cDelay) & ~0xFFL;
                long      ldtSoftRipe = ldtNow + (long) (cDelay * (1 - getWriteBatchFactor()));

                if (!fWasEmpty)
                    {
                    long ldtFirst = arrayRipe.getFirstIndex();
                    long ldtLast  = arrayRipe.getLastIndex();
                    if (ldtSoftRipe <= ldtFirst)
                        {
                        ldtRipe = ldtFirst;
                        }
                    else if (ldtLast > ldtRipe)
                        {
                        // due to requeueing, there are entries beyond the
                        // standard "ripe" time; move the insert point there
                        int cThreshold = getWriteRequeueThreshold();
                        int cSize      = size();
                        if (cThreshold > 0  && cSize % cThreshold == 0)
                            {
                            log("Due to requeuing after store failures, the queue size reached "
                                + cSize + " entries and runs behind the schedule by "
                                + (ldtLast - ldtRipe) + " ms.");
                            }
                        ldtRipe = ldtLast;
                        }
                    }

                List listKeys;
                if (arrayRipe.exists(ldtRipe))
                    {
                    listKeys = (List) arrayRipe.get(ldtRipe);
                    }
                else
                    {
                    arrayRipe.set(ldtRipe, listKeys = new InflatableList());
                    }

                entryNew.setRipeMillis(ldtRipe);
                map.put(binKey, entryNew);
                listKeys.add(binKey);
                
                if (fWasEmpty)
                    {
                    this.notify(); // @see remove()
                    }
                return entryNew;
                }
            else
                {
                entry.updateBinaryValue(entryNew.getBinaryValue());
                entry.expire(entryNew.getExpiry());
                return entry;
                }
            }

        /**
        * Remove a key from the queue if the key is in the queue. This method
        * is used, for example, if an item has actually been removed from
        * the backing map.
        *
        * @param binKey  the key object
        *
        * @return the corresponding entry in the queue or null if the
        *         specified key was not in the queue
        */
        protected synchronized Entry remove(Object binKey)
            {
            // 1. ensure any outstanding updates have been completed prior to
            //    processing the remove such that the following order is visible
            //    (store.store, store.erase)
            // 2. allow synthetic removes to return immediately
            if (getContext().isKeyOwned(binKey) &&
                Thread.currentThread() != ReadWriteBackingMap.this.getWriteThread().getThread())
                {
                while (getPendingMap().containsKey(binKey))
                    {
                    // If re-queue is enabled, it would be done as part of the current
                    // store operation.
                    setWaitingOnPending(true);
                    waitFor(this, 100L);
                    }
                }

            Entry entry = (Entry) getEntryMap().remove(binKey);
            if (entry != null)
                {
                LongArray arrayRipe = getRipeArray();
                long      lIndex    = entry.getRipeMillis();
                List      listKeys  = (List) arrayRipe.get(lIndex);
                if (listKeys != null)
                    {
                    listKeys.remove(binKey);
                    if (listKeys.isEmpty())
                        {
                        arrayRipe.remove(lIndex);
                        }
                    }
                }
            return entry;
            }

        /**
        * Remove the first key from the queue immediately.
        *
        * @return the first entry in the queue or null if the queue is empty
        */
        protected synchronized Entry removeImmediate()
            {
            if (!isActive())
                {
                return null;
                }

            LongArray arrayRipe = getRipeArray();
            if (!arrayRipe.isEmpty())
                {
                long lIndex   = arrayRipe.getFirstIndex();
                List listKeys = (List) arrayRipe.get(lIndex);
                if (!listKeys.isEmpty())
                    {
                    Object oKey = listKeys.remove(0);

                    if (listKeys.isEmpty())
                        {
                        arrayRipe.remove(lIndex);
                        }
                    return (Entry) getEntryMap().remove(oKey);
                    }
                }
            return null;
            }

        /**
        * Wait for item in the queue to ripen (to be ready to be removed), and
        * when there is one and it is ripe, then remove and return it.
        *
        * @return the next item in the queue (it will only return null when the
        *         backing map is no longer active)
        */
        public Entry remove()
            {
            return remove(-1L);
            }

        /**
        * Wait for the next item in the queue to ripen (to be ready to be
        * removed), and remove and return it, or return null if the specified
        * wait time has passed.
        *
        * @param cMillis  the number of ms to wait for an entry to ripen; pass
        *                 -1 to wait indefinitely or 0 for no wait
        *
        * @return the next item in the queue, or null if the wait time has passed
        *         or if the backing map is no longer active
        */
        public synchronized Entry remove(long cMillis)
            {
            LongArray arrayRipe = getRipeArray();

            while (true)
                {
                if (!isActive())
                    {
                    return null;
                    }

                long ldtNow      = getSafeTimeMillis();
                long lIndex      = arrayRipe.getFirstIndex();
                long ldtSoftRipe = lIndex - (long) (getDelayMillis() * getWriteBatchFactor());

                if (lIndex > 0 && (ldtSoftRipe <= ldtNow || m_fFlush))
                    {
                    List listKeys = (List) arrayRipe.get(lIndex);
                    if (!listKeys.isEmpty())
                        {
                        Object oKey  = listKeys.remove(0);
                        Entry  entry = (Entry) getEntryMap().remove(oKey);

                        if (listKeys.isEmpty())
                            {
                            arrayRipe.remove(lIndex);
                            }
                        getPendingMap().put(oKey, entry);
                        return entry;
                        }
                    else
                        {
                        arrayRipe.remove(lIndex);
                        continue;
                        }
                    }

                m_fFlush = false;

                if (cMillis == 0L)
                    {
                    return null;
                    }

                // cap the wait time so that during shutdown the
                // thread will eventually re-check the active flag
                long cWait = (cMillis <= 0L || cMillis > 0xFFL) ? 0xFFL : cMillis;

                if (lIndex > ldtNow)
                    {
                    // adjust the max wait time to maturity time for the next entry
                    cWait = Math.min(cWait, lIndex - ldtNow);
                    }

                waitFor(this, cWait);

                if (cMillis > 0L)
                    {
                    // if we run out of time, set cMillis to 0 so we
                    // check the list one last time before giving up
                    cMillis = Math.max(0L, cMillis - cWait);
                    }
                }
            }

        /**
        * Check for a ripe or soft-ripe item in the queue, and if there is
        * one, return it; otherwise, return null.
        * <p>
        * Unlike the {@link #remove} method, this method will also remove
        * soft-ripe items as calculated using the write-batch factor. A
        * soft-ripe item is an item that has been in the write-behind queue
        * for at least the following duration:
        * <pre>
        * D' = (1.0 - F)*D</pre>
        * where:
        * <pre>
        * D = write-behind delay
        * F = write-batch factor</pre>
        *
        * @return the next item in the queue if one is ripe or soft-ripe and
        *         if the backing map is active, otherwise null
        */
        public synchronized Entry removeNoWait()
            {
            if (isActive())
                {
                LongArray arrayRipe = getRipeArray();
                if (arrayRipe.isEmpty())
                    {
                    m_fFlush = false;
                    }
                else
                    {
                    long ldtNow = getSafeTimeMillis();
                    long lIndex = arrayRipe.getFirstIndex();
                    long ldtSoftRipe = lIndex - (long) (getDelayMillis() * getWriteBatchFactor());

                    if (m_fFlush || ldtSoftRipe <= ldtNow)
                        {
                        List listKeys = (List) arrayRipe.get(lIndex);
                        if (!listKeys.isEmpty())
                            {
                            Object binKey = listKeys.remove(0);
                            Entry  entry  = (Entry) getEntryMap().remove(binKey);

                            if (listKeys.isEmpty())
                                {
                                arrayRipe.remove(lIndex);
                                }
                            getPendingMap().put(binKey, entry);
                            return entry;
                            }
                        }
                    }
                }
            return null;
            }

        /**
        * @return the length of the queue
        */
        public int size()
            {
            return getEntryMap().size();
            }

        /**
        * @return true if and only if the queue contains no items
        */
        public boolean isEmpty()
            {
            return getEntryMap().isEmpty();
            }

        /**
         * Return true iff all contents have been persisted to the underlying store.
         *
         * @return true iff all contents have been persisted to the underlying store
         */
        public synchronized boolean isFlushed()
            {
            return getEntryMap().isEmpty() && getPendingMap().isEmpty();
            }

        /**
         * Asynchronous flush, i.e. effectively mark all currently queue'd entries as ripe.
         */
        public synchronized void flush()
            {
            m_fFlush = true;
            notifyAll();
            }

        /**
        * @param binKey  the key to look for
        *
        * @return true if and only if the queue contains the specified key
        */
        public boolean containsKey(Object binKey)
            {
            return getEntryMap().containsKey(binKey);
            }

        /**
        * Check for an item known to the WriteBehind queue.
        *
        * @param binKey  the key object to look for
        *
        * @return a value from the queue or a pending map
        */
        public synchronized Object checkPending(Object binKey)
            {
            // check for the specified key in one of the two locations:
            // the Queue or PendingMap (see COH-1234)
            Entry entry = (Entry) getEntryMap().get(binKey);
            if (entry == null)
                {
                entry = (Entry) getPendingMap().get(binKey);
                }

            return entry == null ? null : entry.getBinaryValue();
            }

        /**
        * Clear the map of pending entries. Notify all threads that may be
        * waiting for pending store operations to complete.
        */
        public synchronized void clearPending()
            {
            if (isWaitingOnPending())
                {
                notifyAll();
                setWaitingOnPending(false);
                }
            getPendingMap().clear();
            }

        /**
         * Move the ripe time for the queued entry up to accelerate the store
         * operation.
         *
         * @param binKey  the binary key
         *
         * @return true if the entry has already been stored
         */
        public synchronized boolean accelerateEntryRipe(Binary binKey)
            {
            Entry entry = (Entry) getEntryMap().get(binKey);
            if (entry == null)
                {
                // allow the entry to be evicted if the following holds:
                //   1. it is not waiting to be written
                //   2. it is not actively being written
                //   3. it is actively being written but that process initiated
                //      the expiry (@see replace)

                entry = (Entry) getPendingMap().get(binKey);
                return entry == null || extractExpiry(entry) == 1L;
                }

            long ldtNow  = Base.getSafeTimeMillis();
            long ldtRipe = entry.getRipeMillis();
            long ldtRipeThreshold = ldtNow + ACCELERATE_MIN;

            if (ldtRipe >= ldtRipeThreshold)
                {
                LongArray arrayRipe = getRipeArray();

                // remove from original index
                List listKeys = (List) arrayRipe.get(ldtRipe);

                listKeys.remove(binKey);
                if (listKeys.isEmpty())
                    {
                    arrayRipe.remove(ldtRipe);
                    }

                // batch with first index sits within ACCELERATE_MIN interval,
                // otherwise make it ripe in 10 milliseconds
                long ldtFirst = arrayRipe.getFirstIndex();
                if (ldtFirst >= 0 && ldtFirst < ldtRipeThreshold)
                    {
                    listKeys = (List) arrayRipe.get(ldtFirst);
                    listKeys.add(binKey);
                    entry.setRipeMillis(ldtFirst);
                    }
                else
                    {
                    ldtRipe = ldtNow + 10L;
                    arrayRipe.set(ldtRipe, listKeys = new InflatableList());
                    listKeys.add(binKey);
                    entry.setRipeMillis(ldtRipe);
                    }

                // mark it as "almost expired"
                entry.expire(1);
                }

            return false;
            }


        // ----- internal -----------------------------------------------

        /**
        * Return a map of items queued to be written.
        *
        * @return the map of items queued to be written
        */
        protected Map getEntryMap()
            {
            return m_mapQueuedWrites;
            }

        /**
        * Return a {@link LongArray} indexed by ripe time (when entries become
        * eligible to be written), and associated to a list of binary keys.
        *
        * @return a LongArray of items in the queue
        */
        protected LongArray getRipeArray()
            {
            return m_arrQueue;
            }

        /**
        * Obtain a map of entries removed from the queue, but not yet persisted
        * to the underlying datastore. The returned map is not thread safe, so
        * all access to its content must be synchronized by the caller.
        *
        * @return the map of items not yet persisted to the underlying datastore
        */
        protected Map getPendingMap()
            {
            return m_mapPending;
            }

        /**
        * Check whether any threads are waiting for the store operation
        * to complete.
        *
        * @return  true iff there is at least one waiting thread
        */
        protected boolean isWaitingOnPending()
            {
            return m_fWaitingOnPending;
            }

        /**
        * Set the flag indicating whether any threads are waiting for the
        * pending store operation to complete.
        *
        * @param fPending  the boolean value to set
        */
        protected void setWaitingOnPending(boolean fPending)
            {
            m_fWaitingOnPending = fPending;
            }

        // ----- data members -------------------------------------------

        /**
        * Map of key to Entry objects in the queue; allows quick lookup to
        * determine if something is or is not already queued, and actually
        * holds the value and when-added time information.
        */
        private final Map       m_mapQueuedWrites = new SafeHashMap();

        /**
        * The queue (ordered list) of entry keys where the index indicates when
        * an entry is ripe. The entry itself is a List of items that ripens
        * at the same time.
        */
        private final LongArray m_arrQueue       = new SparseArray();

        /**
        * Map of entries removed from the queue, but not yet persisted to the
        * underlying datastore.
        */
        private final Map       m_mapPending     = new HashMap();

        /**
        * Number of milliseconds to delay before allowing items to be removed off
        * the front of the queue; default to one minute.
        */
        private long      m_cDelayMillis   = 60 * 1000;

        /**
         * Entries that will ripe within this interval won't be accelerated.
         */
        private static final long ACCELERATE_MIN = 0xFFL;

        /**
        * Flag indicating whether there are threads waiting for the store
        * operation to complete.
        */
        private boolean m_fWaitingOnPending;

        /**
        * True iff an async flush has been requested.
        */
        private boolean m_fFlush;
        }


    // ----- inner class: ReadThread (refresh-ahead thread) -----------------

    /**
    * Get the refresh-ahead thread.
    *
    * @return the refresh-ahead thread or null if refresh-ahead is not
    *         enabled
    */
    protected ReadThread getReadThread()
        {
        return m_daemonRead;
        }

    /**
    * Set up the optional refresh-ahead thread and queue that this backing map
    * will use.
    * <p>
    * This method has no effect if the given refresh-ahead factor is zero or
    * the cache returned by {@link #getInternalConfigurableCache()} is null or
    * non-expiring.
    *
    * @param dflRefreshAheadFactor  the refresh-ahead factor expressed as a
    *                               percentage of the internal cache expiry
    */
    protected void configureReadThread(double dflRefreshAheadFactor)
        {
        Base.azzert(dflRefreshAheadFactor >= 0.0 && dflRefreshAheadFactor <= 1.0,
                "Invalid refresh-ahead factor: " + dflRefreshAheadFactor);

        ConfigurableCacheMap cache = getInternalConfigurableCache();
        if (dflRefreshAheadFactor > 0.0 && cache != null)
            {
            int cExpiryMillis = cache.getExpiryDelay();
            if (cExpiryMillis == 0)
                {
                dflRefreshAheadFactor = 0.0;
                }
            else if (isWriteBehind())
                {
                // make sure that the refresh-ahead delay is greater or equal
                // to the write-behind delay
                int cExpiry     = cExpiryMillis / 1000;
                int cReadDelay  = (int) (cExpiry * (1.0 - dflRefreshAheadFactor));
                int cWriteDelay = getWriteBehindSeconds();
                if (cReadDelay < cWriteDelay)
                    {
                    cReadDelay = (cExpiry + cWriteDelay) / 2;

                    StringBuilder sb = new StringBuilder()
                      .append("ReadWriteBackingMap refresh-ahead factor of ")
                      .append(dflRefreshAheadFactor)
                      .append(" is too aggressive for the write-delay of ")
                      .append(cWriteDelay)
                      .append(" seconds; ");

                    dflRefreshAheadFactor = cExpiry == 0
                            ? 0.0 : 1.0 - ((double) cReadDelay / cExpiry);
                    if (dflRefreshAheadFactor > 0.0)
                        {
                        sb.append("reducing the factor to ")
                          .append(dflRefreshAheadFactor)
                          .append('.');
                        }
                    else
                        {
                        sb.append("disabling refresh-ahead.");
                        }

                    Base.log(sb.toString());
                    }
                }

            if (dflRefreshAheadFactor > 0.0)
                {
                m_dflRefreshAheadFactor = dflRefreshAheadFactor;
                m_queueRead             = instantiateReadQueue();
                m_daemonRead            = instantiateReadThread();
                m_daemonRead.start();
                }
            }
        }

    /**
    * Factory pattern: Instantiate the refresh-ahead thread.
    *
    * @return a new refresh-ahead thread
    */
    protected ReadThread instantiateReadThread()
        {
        return new ReadThread();
        }

    /**
    * Terminate the refresh-ahead thread.
    */
    protected void terminateReadThread()
        {
        if (isActive())
            {
            // thread is exiting; make sure it does not appear
            // that the map is still refresh-ahead or read-dirty
            ReadThread daemon = m_daemonRead;
            ReadQueue  queue  = m_queueRead;
            m_daemonRead = null;
            m_queueRead  = null;

            if (daemon != null)
                {
                daemon.stop();
                }

            if (queue != null)
                {
                queue.clear();
                }
            }
        }

    /**
    * A thread that removes keys from a {@link ReadQueue}, reads the value
    * for the key from the underlying <tt>CacheStore</tt>, and caches the
    * value in the internal <tt>ReadWriteBackingMap</tt> cache.
    *
    * @author jh 2005.02.08
    */
    public class ReadThread
            extends Daemon
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        */
        public ReadThread()
            {
            super("ReadThread:"
                    + getCacheStore()
                    + (getCacheService() == null
                       ? ""
                       : (":" + getCacheService().getInfo().getServiceName())),
                     Thread.NORM_PRIORITY, false);

            m_fRefreshContext = false;
            }

        // ----- Daemon methods -----------------------------------------

        /**
        * The daemon's implementation method.
        */
        public void run()
            {
            ContainerHelper.initializeThreadContext(getCacheService());

            ConcurrentMap mapControl  = getControlMap();
            ReadQueue     queue       = getReadQueue();
            long          cWaitMillis = getMaxWaitMillis(0xFFL);

            try
                {
                while (isActive() && !isStopping())
                    {
                    if (m_fRefreshContext)
                        {
                        GuardSupport.setThreadContext(getContext());
                        m_fRefreshContext = false;
                        }

                    StoreWrapper store = getCacheStore();

                    if (store != null)
                        {
                        // heartbeat before waiting.
                        heartbeat();

                        // find the next candidate key for an asynchronous
                        // load in the queue and place a latch in the
                        // control map under the key; the latch serves two
                        // purposes:
                        //
                        // (1) the presence of a latch in the control map
                        //     signals to other threads that the key is
                        //     currently being loaded by the refresh-ahead
                        //     thread;
                        // (2) other threads can wait on the latch for the
                        //     result of the load operation
                        ReadLatch latch = queue.select(cWaitMillis);
                        if (latch == null)
                            {
                            // couldn't find and/or latch a key in time
                            continue;
                            }

                        Object    oKey      = latch.getKey();
                        Entry     entry     = null;
                        Throwable exception = null;

                        // load the key and store the new value in one of
                        // two ways:
                        //
                        // (1) if the control map can be quickly locked, the
                        //     refresh thread can directly cache the new value
                        //     in the internal cache, as long as the load
                        //     operation hasn't been canceled;
                        // (2) otherwise, a thread is either waiting for the
                        //     result of the load operation or is going to
                        //     cancel the operation (but not both), so call
                        //     complete() on the latch
                        try
                            {
                            // avoid loading from a store if the entry is not
                            // owned anymore; this is a simple optimization,
                            // since this check must be performed again after
                            // the lock is acquired
                            if (ReadWriteBackingMap.this.getContext().isKeyOwned(oKey))
                                {
                                entry = store.load(oKey);
                                }
                            }
                        catch (Throwable e)
                            {
                            exception = e;
                            }

                        // try a quick lock and double-check that the load
                        // operation wasn't canceled between the time the load
                        // latch was placed in the control map and the load
                        // operation completed; also, since the load was done
                        // asynchronously, check to see if the key is still
                        // owned by this member
                        Object oValue = entry == null ? null : entry.getBinaryValue();
                        if (mapControl.lock(oKey, 0))
                            {
                            try
                                {
                                // synchronization is not necessary here since
                                // this thread owns the key
                                if (exception == null && !latch.isCanceled() &&
                                    ReadWriteBackingMap.this.getContext().isKeyOwned(oKey))
                                    {
                                    putToInternalCache(oKey, oValue, extractExpiry(entry));
                                    }
                                }
                            finally
                                {
                                mapControl.remove(oKey);
                                mapControl.unlock(oKey);
                                }
                            }
                        else
                            {
                            // since we could not lock, notify the lock owner
                            // that the current load operation has either
                            // completed or been canceled due to an exception
                            if (exception == null)
                                {
                                latch.complete(oValue);
                                }
                            else
                                {
                                latch.cancel(exception);
                                }
                            mapControl.remove(oKey);
                            }
                        }
                    }
                }
            finally
                {
                terminateReadThread();
                }
            }

        /**
        * {@inheritDoc}
        */
        public void terminate()
            {
            // As of Coherence 3.6, recovery will attempt to interrupt the
            // thread but we don't terminate the cache service (or the thread)
            // due to refresh-ahead timeout.

            err("The refresh-ahead thread timed out.  This could be indicative of " +
                "an extremely slow-running or hung CacheStore call, or deadlock.");
            GuardSupport.logStackTraces();

            // Re-register the thread with the guardian; this will ensure that
            // the thread remains guarded, and periodically re-log the error msg
            // and stack traces.
            setGuardPolicy((Guardian) ReadWriteBackingMap.this.getContext().getCacheService(),
                           getCacheStoreTimeoutMillis(), GUARD_RECOVERY);
            }

        /**
        * {@inheritDoc}
        */
        protected void setGuardPolicy(Guardian guardian, long cTimeoutMillis, float flPctRecover)
            {
            // Note: needed to provide access visibility to the outer class
            super.setGuardPolicy(guardian, cTimeoutMillis, flPctRecover);
            }

        // ----- data fields ---------------------------------------------

        /**
        * Field used to tell the {@link ReadThread} to refresh its {@link GuardContext}.
        */
        protected volatile boolean m_fRefreshContext;
        }


    // ----- inner class: WriteThread (write-behind thread) -----------------

    /**
    * Get the write-behind thread.
    *
    * @return the write-behind thread or null if there is no CacheStore to
    *         write to
    */
    protected WriteThread getWriteThread()
        {
        return m_daemonWrite;
        }

    /**
    * Set up the optional write-behind thread and queue that this backing map
    * will use.
    * <p>
    * This method has no effect if the given write-behind delay is zero or
    * {@link #isReadOnly()} returns true.
    *
    * @param cWriteBehindSeconds  write-behind delay
    */
    protected void configureWriteThread(int cWriteBehindSeconds)
        {
        if (cWriteBehindSeconds > 0 && !isReadOnly())
            {
            m_queueWrite  = instantiateWriteQueue();
            m_daemonWrite = instantiateWriteThread();
            m_daemonWrite.start();

            if (isWriteBehindRemove())
                {
                m_setPendingRemoves = new SafeHashSet();
                }

            setWriteBehindSeconds(cWriteBehindSeconds);

            ConfigurableCacheMap mapInternal = getInternalConfigurableCache();
            if (mapInternal != null)
                {
                // COH-6163: Prevent eviction of entries that are pending to be stored
                mapInternal.setEvictionApprover(f_writeBehindDisapprover);
                }
            }
        }

    /**
    * Factory pattern: Instantiate the write-behind thread.
    *
    * @return a new write-behind thread
    */
    protected WriteThread instantiateWriteThread()
        {
        return new WriteThread();
        }

    /**
    * Terminate the write-behind thread.
    */
    protected void terminateWriteThread()
        {
        if (isActive())
            {
            StoreWrapper store = getCacheStore();
            WriteQueue   queue = getWriteQueue();
            if (store != null && queue != null)
                {
                // no operation on a queue is allowed while we are flushing
                // (see put, remove, flush)
                try
                    {
                    // thread is exiting; make sure it does not appear
                    // that the map is still write-behind
                    WriteThread daemon = m_daemonWrite;
                    synchronized (daemon)
                        {
                        m_daemonWrite = null;
                        m_queueWrite  = null;

                        flush(queue, store);

                        if (daemon != null)
                            {
                            daemon.stop();
                            }
                        }
                    }
                catch (Exception e)
                    {
                    Base.err("An exception occurred while flushing the write-behind queue"
                            + " while terminating the write-behind thread:");
                    Base.err(e);
                    Base.err("(The write-behind thread is exiting.)");
                    }
                }
            }
        }

    /**
    * This is the write-behind thread that pulls things from the write-behind
    * queue and writes them to the CacheStore that the backing map uses.
    *
    * @author cp 2002.10.22
    */
    public class WriteThread
            extends Daemon
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        */
        public WriteThread()
            {
            super("WriteBehindThread:"
                    + getCacheStore()
                    + (getCacheService() == null
                       ? ""
                       : (":" + getCacheService().getInfo().getServiceName())),
                     Thread.NORM_PRIORITY, false);

            m_fRefreshContext = false;
            }

        // ----- Daemon methods -----------------------------------------

        /**
        * The daemon's implementation method.
        */
        public void run()
            {
            CacheService service = getCacheService();
            ClassLoader  loader  = service.getContextClassLoader();
            if (loader != null)
                {
                setThreadContextClassLoader(loader);
                }

            ContainerHelper.initializeThreadContext(service);
            try
                {
                while (isActive() && !isStopping())
                    {
                    WriteQueue   queue = getWriteQueue();
                    StoreWrapper store = getCacheStore();
                    long         cWait = getMaxWaitMillis(0xFFL);

                    if (m_fRefreshContext)
                        {
                        GuardSupport.setThreadContext(getContext());
                        m_fRefreshContext = false;
                        }

                    // prevent NPE on release()
                    if (queue == null || store == null)
                        {
                        continue;
                        }

                    try
                        {
                        // issue a heartbeat before blocking on the write queue
                        heartbeat();

                        Entry entry = queue.remove(cWait);
                        if (entry == null)
                            {
                            continue;
                            }

                        // populate a set of ripe and soft-ripe entries
                        boolean    fStoreAll   = store.isStoreAllSupported();
                        boolean    fEraseAll   = store.isEraseAllSupported();
                        int        cEntries    = 0;
                        int        cMaxEntries = getWriteMaxBatchSize();
                        Set<Entry> setStore    = fStoreAll ? new LinkedHashSet<>(cMaxEntries, 0.75f) : null;
                        Set<Entry> setErase    = fEraseAll ? new LinkedHashSet<>(cMaxEntries, 0.75f) : null;

                        while (entry != null)
                            {
                            boolean fRemove = equals(entry.getBinaryValue(), BIN_ERASE_PENDING);

                            if (fRemove)
                                {
                                if (fEraseAll)
                                    {
                                    setErase.add(entry);
                                    }
                                else
                                    {
                                    store.erase(entry);
                                    }
                                }
                            else
                                {
                                if (fStoreAll)
                                    {
                                    setStore.add(entry);
                                    }
                                else
                                    {
                                    store.store(entry, true);
                                    }
                                }

                            if (++cEntries >= cMaxEntries)
                                {
                                break;
                                }

                            // remove next ripe or soft-ripe entry, if available
                            entry = queue.removeNoWait();
                            }

                        if (fEraseAll && !setErase.isEmpty())
                            {
                            if (setErase.size() == 1)
                                {
                                store.erase(setErase.iterator().next());
                                }
                            else
                                {
                                store.eraseAll(setErase);
                                }
                            }

                        if (fStoreAll && !setStore.isEmpty())
                            {
                            if (setStore.size() == 1)
                                {
                                store.store(setStore.iterator().next(), true);
                                }
                            else
                                {
                                store.storeAll(setStore);
                                }
                            }
                        }
                    catch (Throwable e)
                        {
                        // don't want to allow an exception to kill the
                        // write-behind thread
                        err("An exception occurred on the write-behind thread");
                        err(e);
                        err("(The exception will be ignored. " +
                                "The write-behind thread will continue.)");

                        // clear the interrupted flag. If the CacheStore
                        // implementation (e.g. JDBC driver) caught the
                        // interrupt and reset the interrupted flag, and we
                        // intend to continue executing on the write-behind
                        // thread, we need to ensure that the interrupted flag
                        // is cleared before continuing
                        Thread.interrupted();
                        }
                    finally
                        {
                        queue.clearPending();
                        }
                    }
                }
            finally
                {
                terminateWriteThread();
                }
            }

        /**
        * {@inheritDoc}
        */
        public void terminate()
            {
            // As of Coherence 3.6, recovery will attempt to interrupt the
            // thread but we don't terminate the cache service (or the thread)
            // due to write-behind timeout.

            err("The write-behind thread timed out.  This could be indicative of " +
                "an extremely slow-running or hung CacheStore call, or deadlock.");
            GuardSupport.logStackTraces();

            // Re-register the thread with the guardian; this will ensure that
            // the thread remains guarded, and periodically re-log the error msg
            // and stack traces.
            setGuardPolicy((Guardian) ReadWriteBackingMap.this.getContext().getCacheService(),
                           getCacheStoreTimeoutMillis(), GUARD_RECOVERY);
            }

        /**
        * {@inheritDoc}
        */
        protected void setGuardPolicy(Guardian guardian, long cTimeoutMillis, float flPctRecover)
            {
            // Note: needed to provide access visibility to the outer class
            super.setGuardPolicy(guardian, cTimeoutMillis, flPctRecover);
            }

        // ----- data fields ---------------------------------------------

        /**
        * Field used to tell the {@link WriteThread} to refresh its {@link GuardContext}.
        */
        protected volatile boolean m_fRefreshContext;
        }


    // ----- CacheStore accessor and configuration --------------------------

    /**
    * Get the representative of the "persistent" storage for this backing
    * map.
    *
    * @return the cache store wrapper object that this backing map uses for
    *         persistence or null if there is no persistent store behind
    *         this backing map
    */
    public StoreWrapper getCacheStore()
        {
        return m_store;
        }

    /**
    * Set up the StoreWrapper that this backing map will use.
    *
    * @param store      the StoreWrapper that this backing map will
    *                   delegate persistence responsibilities to
    * @param fReadOnly  pass true to prevent the usage of the cache store
    *                   write operations
    * @param fWBRemove  pass true to apply write-behind to remove
    */
    protected void configureCacheStore(StoreWrapper store, boolean fReadOnly, boolean fWBRemove)
        {
        Base.azzert(store != null && m_store == null);
        m_fReadOnly = fReadOnly;
        m_store     = store;
        m_fWBRemove = fWBRemove;
        }


    // ----- inner class: StoreWrapper ----------------------------------

    /**
    * Factory pattern: Instantiate a StoreWrapper wrapper around the
    * passed CacheStore. (Supports CacheStore extension by delegation
    * pattern.)
    *
    * @param store  the CacheStore to wrap
    *
    * @return the StoreWrapper wrapper that can supplement and override the
    *         operations of the supplied CacheStore
    */
    protected StoreWrapper instantiateCacheStoreWrapper(CacheStore store)
        {
        return new CacheStoreWrapper(store);
        }

    /**
    * Factory pattern: Instantiate a StoreWrapper wrapper around the
    * passed BinaryEntryStore. (Supports BinaryEntryStore extension by
    * delegation pattern.)
    *
    * @param store  the BinaryEntryStore to wrap
    *
    * @return the StoreWrapper wrapper that can supplement and override the
    *         operations of the supplied BinaryEntryStore
    */
    protected StoreWrapper instantiateCacheStoreWrapper(BinaryEntryStore store)
        {
        return new BinaryEntryStoreWrapper(store);
        }

    /**
    * Factory pattern: Instantiate a StoreWrapper wrapper around the
    * passed NonBlockingEntryStore. (Supports NonBlockingEntryStore extension
    * by delegation pattern.)
    *
    * @param store  the NonBlockingEntryStore to wrap
    *
    * @return the StoreWrapper wrapper that can supplement and override the
    *         operations of the supplied BinaryEntryStore
    */
    protected StoreWrapper instantiateCacheStoreWrapper(NonBlockingEntryStore store)
        {
        return new NonBlockingEntryStoreWrapper(store);
        }

    /**
    * Abstract wrapper around a cache store to allow operations to be
    * overridden and extended.
    *
    * @author cp 2002.06.04
    * @author tb 2011.01.11
    */
    public abstract class StoreWrapper
            extends Base
        {
        // ----- Bundling support ---------------------------------------

        /**
        * Configure the bundler for the "load" operations. If the bundler does
        * not exist and bundling is enabled, it will be instantiated.
        *
        * @param cBundleThreshold  the bundle size threshold; pass zero to
        *                          disable "load" operation bundling
        *
        * @return the "load" bundler or null if bundling is disabled
        */
        public synchronized AbstractBundler ensureLoadBundler(int cBundleThreshold)
            {
            if (cBundleThreshold > 0)
                {
                AbstractBundler bundler = m_loadBundler;
                if (bundler == null)
                    {
                    m_loadBundler = bundler = instantiateLoadBundler();
                    }
                bundler.setSizeThreshold(cBundleThreshold);
                return bundler;
                }
            else
                {
                return m_loadBundler = null;
                }
            }

        /**
        * Configure the bundler for the "store" operations. If the bundler
        * does not exist and bundling is enabled, it will be instantiated.
        *
        * @param cBundleThreshold  the bundle size threshold; pass zero to
        *                          disable "store" operation bundling
        *
        * @return the "store" bundler or null if bundling is disabled
        */
        public synchronized AbstractBundler ensureStoreBundler(int cBundleThreshold)
            {
            if (cBundleThreshold > 0)
                {
                AbstractBundler bundler = m_storeBundler;
                if (bundler == null)
                    {
                    m_storeBundler = bundler = instantiateStoreBundler();
                    }
                bundler.setSizeThreshold(cBundleThreshold);
                return bundler;
                }
            else
                {
                return m_storeBundler = null;
                }
            }

        /**
        * Configure the bundler for the "erase" operations. If the bundler
        * does not exist and bundling is enabled, it will be instantiated.
        *
        * @param cBundleThreshold  the bundle size threshold; pass zero to
        *                          disable "erase" operation bundling
        *
        * @return the "erase" bundler or null if bundling is disabled
        */
        public synchronized AbstractBundler ensureEraseBundler(int cBundleThreshold)
            {
            if (cBundleThreshold > 0)
                {
                AbstractBundler bundler = m_eraseBundler;
                if (bundler == null)
                    {
                    m_eraseBundler = bundler = instantiateEraseBundler();
                    }
                bundler.setSizeThreshold(cBundleThreshold);
                return bundler;
                }
            else
                {
                return m_eraseBundler = null;
                }
            }

        // ----- JMX support --------------------------------------------

        /**
        * Determine the number of load() operations.
        *
        * @return the number of load() operations
        */
        public long getLoadOps()
            {
            return f_cLoadOps.get();
            }

        /**
        * Determine the number of load() failures.
        *
        * @return the number of load() failures
        */
        public long getLoadFailures()
            {
            return f_cLoadFailures.get();
            }

        /**
        * Determine the cumulative time spent on load() operations.
        *
        * @return the cumulative time spent on load() operations
        */
        public long getLoadMillis()
            {
            return f_cLoadMillis.get();
            }

        /**
        * Determine the number of store() operations.
        *
        * @return the number of store() operations
        */
        public long getStoreOps()
            {
            return f_cStoreOps.get();
            }

        /**
        * Determine the number of store() failures.
        *
        * @return the number of store() failures
        */
        public long getStoreFailures()
            {
            return f_cStoreFailures.get();
            }

        /**
        * Determine the cumulative time spent on store() operations.
        *
        * @return the cumulative time spent on store() operations
        */
        public long getStoreMillis()
            {
            return f_cStoreMillis.get();
            }

        /**
        * Determine the number of erase() operations.
        *
        * @return the number of erase() operations
        */
        public long getEraseOps()
            {
            return m_cEraseOps;
            }

        /**
        * Determine the number of erase() failures.
        *
        * @return the number of erase() failures
        */
        public long getEraseFailures()
            {
            return m_cEraseFailures;
            }

        /**
        * Determine the cumulative time spent on erase() operations.
        *
        * @return the cumulative time spent on erase() operations
        */
        public long getEraseMillis()
            {
            return m_cEraseMillis;
            }

        /**
        * Determine the average number of entries stored per store() operation.
        *
        * @return the average number of entries stored per store() operation
        */
        public long getAverageBatchSize()
            {
            long cOps = getStoreOps();
            return cOps > 0L ? f_cStoreEntries.get() / cOps : 0L;
            }

        /**
        * Determine the average time spent per load() operation.
        *
        * @return the average time spent per load() operation
        */
        public long getAverageLoadMillis()
            {
            long cOps = getLoadOps();
            return cOps > 0L ? f_cLoadMillis.get() / cOps : 0L;
            }

        /**
        * Determine the average time spent per store() operation.
        *
        * @return the average time spent per store() operation
        */
        public long getAverageStoreMillis()
            {
            long cOps = getStoreOps();
            return cOps > 0L ? f_cStoreMillis.get() / cOps : 0L;
            }

        /**
        * Determine the average time spent per erase() operation.
        *
        * @return the average time spent per erase() operation
        */
        public long getAverageEraseMillis()
            {
            long cOps = m_cEraseOps;
            return cOps > 0L ? m_cEraseMillis / cOps : 0L;
            }

        /**
        * Determine the number of pending non-blocking store operations.
        *
        * @return the number of pending non-blocking store operations
        */
        public long getPendingAsyncStoreOps()
            {
            return f_cPendingAsyncStoreOps.get();
            }

        /**
        * Reset the CacheStore statistics.
        */
        public void resetStatistics()
            {
            f_cLoadOps.set(0L);
            f_cLoadFailures.set(0L);
            f_cLoadMillis.set(0L);
            f_cStoreOps.set(0L);
            f_cStoreEntries.set(0L);
            f_cStoreFailures.set(0L);
            f_cStoreMillis.set(0L);
            f_cPendingAsyncStoreOps.set(0L);

            m_cEraseOps      = 0L;
            m_cEraseFailures = 0L;
            m_cEraseMillis   = 0L;
            }

        // ----- accessors ----------------------------------------------

        /**
        * Obtain the bundler for the "load" operations.
        *
        * @return the "load" bundler
        */
        public AbstractBundler getLoadBundler()
            {
            return m_loadBundler;
            }

        /**
        * Obtain the bundler for the "store" operations.
        *
        * @return the "store" bundler
        */
        public AbstractBundler getStoreBundler()
            {
            return m_storeBundler;
            }

        /**
        * Obtain the bundler for the "erase" operations.
        *
        * @return the "erase" bundler
        */
        public AbstractBundler getEraseBundler()
            {
            return m_eraseBundler;
            }

        /**
        * Determine if the wrapped store supports store() operations.
        *
        * @return true if the wrapped store supports store() operations
        */
        public boolean isStoreSupported()
            {
            return m_fStoreSupported;
            }

        /**
        * Set the flag that determines whether or not the wrapped store
        * supports store() operations.
        *
        * @param fSupported  the new value of the flag
        */
        public void setStoreSupported(boolean fSupported)
            {
            m_fStoreSupported = fSupported;
            }

        /**
        * Determine if the wrapped store supports storeAll() operations.
        *
        * @return true if the wrapped store supports storeAll() operations
        */
        public boolean isStoreAllSupported()
            {
            return m_fStoreAllSupported;
            }

        /**
        * Set the flag that determines whether or not the wrapped store
        * supports storeAll() operations.
        *
        * @param fSupported  the new value of the flag
        */
        public void setStoreAllSupported(boolean fSupported)
            {
            m_fStoreAllSupported = fSupported;
            }

        /**
        * Determine if the wrapped store supports erase() operations.
        *
        * @return true if the wrapped store supports erase() operations
        */
        public boolean isEraseSupported()
            {
            return m_fEraseSupported;
            }

        /**
        * Set the flag that determines whether or not the wrapped store
        * supports erase() operations.
        *
        * @param fSupported  the new value of the flag
        */
        public void setEraseSupported(boolean fSupported)
            {
            m_fEraseSupported = fSupported;
            }

        /**
        * Determine if the wrapped store supports eraseAll() operations.
        *
        * @return true if the wrapped store supports eraseAll() operations
        */
        public boolean isEraseAllSupported()
            {
            return m_fEraseAllSupported;
            }

        /**
        * Set the flag that determines whether or not the wrapped store
        * supports eraseAll() operations.
        *
        * @param fSupported  the new value of the flag
        */
        public void setEraseAllSupported(boolean fSupported)
            {
            m_fEraseAllSupported = fSupported;
            }

        /**
        * Determine if the wrapped store implements blocking operations.
        *
        * @return true if the operations are blocking
        */
        public boolean isBlocking()
            {
            return true;
            }

        // ----- pseudo CacheStore interface ----------------------------

        /**
        * Return the entry associated with the specified key, or null if the
        * key does not have an associated value in the underlying store.
        * <p>
        * Same as {@link CacheLoader#load}, but the key and the value
        * are in the internal format.
        *
        * @param binKey  binary key whose associated entry is to be returned
        *
        * @return the entry associated with the specified binary key, or
        *         <tt>null</tt> if no value is available for that key
        */
        protected Entry load(Object binKey)
            {
            // issue a heartbeat before I/O
            ReadWriteBackingMap.this.heartbeat();

            long lStart = getSafeTimeMillis();
            try
                {
                return loadInternal(binKey);
                }
            finally
                {
                f_cLoadOps.incrementAndGet();
                long lElapsed = getSafeTimeMillis() - lStart;
                if (lElapsed != 0L)
                    {
                    f_cLoadMillis.addAndGet(lElapsed);
                    }
                }
            }

        /**
        * Return the entry set associated with the specified keys in the
        * passed collection. If a key does not have an associated value in
        * the underlying store, then the return set will not have an entry
        * for that key.
        * <p>
        * Same as {@link CacheLoader#loadAll}, but the keys are in the
        * internal format.
        *
        * @param setBinKey  a set of keys to load
        *
        * @return a Set of entries for the specified keys
        */
        protected Set loadAll(Set setBinKey)
            {
            // issue a heartbeat before I/O
            ReadWriteBackingMap.this.heartbeat();

            long lStart = getSafeTimeMillis();
            try
                {
                return loadAllInternal(setBinKey);
                }
            finally
                {
                f_cLoadOps.incrementAndGet();
                long lElapsed = getSafeTimeMillis() - lStart;
                if (lElapsed != 0L)
                    {
                    f_cLoadMillis.addAndGet(lElapsed);
                    }
                }
            }

        /**
        * Store the specified entry in the underlying store.
        *
        * @param binEntry      the entry
        * @param fAllowChange  if true, any changes made to the entry by the
        *                      store operation should be applied to the
        *                      internal cache; otherwise they will be
        *                      dealt with by the caller
        */
        protected void store(Entry binEntry, boolean fAllowChange)
            {
            // issue a heartbeat before I/O
            ReadWriteBackingMap.this.heartbeat();

            long    lStart   = getSafeTimeMillis();
            boolean fSuccess = true;
            try
                {
                storeInternal(binEntry);
                }
            catch (RuntimeException e)
                {
                fSuccess = false;
                f_cStoreFailures.incrementAndGet();
                onStoreFailure(binEntry, e);
                }
            finally
                {
                // defer to onNext() for non-blocking stores
                if (isBlocking())
                    {
                    f_cStoreOps.incrementAndGet();
                    f_cStoreEntries.incrementAndGet();
                    binEntry.stopTracking();
                    long lElapsed = getSafeTimeMillis() - lStart;
                    if (lElapsed != 0L)
                        {
                        f_cStoreMillis.addAndGet(lElapsed);
                        }
                    }
                else
                    {
                    f_cPendingAsyncStoreOps.incrementAndGet();
                    }
                }

            boolean fAsynch = getWriteQueue() != null;

            // We need to replace the internal value in two cases:
            // 1) if the store() operation changed the entry that is not evicted;
            // 2) if the write-behind succeeded.
            // Note, that if the asynchronous store operation failed, we don't
            // undecorate; as a result, if the requeueing is off, failed-to-store
            // entries will remain decorated and may be attempted to be stored
            // again upon future re-distributions
            if (fAllowChange && (binEntry.isChanged() || fAsynch && fSuccess))
                {
                replace(binEntry);
                }
            }

        /**
        * Store the entries in the specified set in the underlying store.
        *
        * @param setBinEntries  the set of binary entries
        */
        protected void storeAll(Set setBinEntries)
            {
            int cEntries = setBinEntries.size();
            if (cEntries == 0)
                {
                return;
                }

            // issue a heartbeat before I/O
            ReadWriteBackingMap.this.heartbeat();

            boolean fAsynch = getWriteQueue() != null;
            Set     setAll  = setBinEntries;
            if (fAsynch)
                {
                // hold the entries since the storeAll may remove them from
                // the passed in map upon successful DB operation
                setAll = new HashSet(setBinEntries);
                }

            long    lStart   = getSafeTimeMillis();
            boolean fSuccess = true;
            try
                {
                storeAllInternal(setBinEntries);
                }
            catch (RuntimeException e)
                {
                fSuccess = false;
                f_cStoreFailures.incrementAndGet();
                onStoreAllFailure(setBinEntries, e, isBlocking());
                }
            finally
                {
                if (isBlocking())
                    {
                    f_cStoreOps.incrementAndGet();
                    f_cStoreEntries.addAndGet(cEntries);
                    long lElapsed = getSafeTimeMillis() - lStart;
                    if (lElapsed != 0L)
                        {
                        f_cStoreMillis.addAndGet(lElapsed);
                        }
                    }
                else
                    {
                    f_cPendingAsyncStoreOps.incrementAndGet();
                    }
                }

            for (Object o : setAll)
                {
                Entry entry = (Entry) o;

                entry.stopTracking();

                // if something failed, according to our convention entries that
                // stored successfully are removed from the setBinEntries
                if (entry.isChanged() ||
                    fAsynch && (fSuccess || !setBinEntries.contains(entry)))
                    {
                    replace(entry);
                    }
                }
            }

        /**
        * Remove the specified entry from the underlying store.
        *
        * @param binEntry  the entry
        */
        protected void erase(Entry binEntry)
            {
            // issue a heartbeat before I/O
            ReadWriteBackingMap.this.heartbeat();

            long lStart = getSafeTimeMillis();
            try
                {
                eraseInternal(binEntry);

                if (getWriteQueue() != null && isWriteBehindRemove())
                    {
                    Binary binKey = binEntry.getBinaryKey();
                    getInternalCache().remove(binKey);
                    getPendingRemoves().remove(binKey);
                    }
                }
            catch (RuntimeException e)
                {
                ++m_cEraseFailures;
                onEraseFailure(binEntry, e);
                }
            finally
                {
                ++m_cEraseOps;
                long lElapsed = getSafeTimeMillis() - lStart;
                if (lElapsed != 0L)
                    {
                    m_cEraseMillis += lElapsed;
                    }
                }
            }

        /**
        * Remove the specified entries from the underlying store.
        *
        * @param setBinEntries  the set of entries
        */
        protected void eraseAll(Set setBinEntries)
            {
            // issue a heartbeat before I/O
            ReadWriteBackingMap.this.heartbeat();

            Set     setAll  = setBinEntries;
            boolean fAsynch = getWriteQueue() != null && isWriteBehindRemove();

            if (fAsynch)
                {
                // hold the entries since the eraseAll may remove them from
                // the passed in map upon successful DB operation
                setAll = new HashSet(setBinEntries);
                }

            long lStart = getSafeTimeMillis();
            try
                {
                eraseAllInternal(setBinEntries);
                if (fAsynch)
                    {
                    for (ReadWriteBackingMap.Entry entry : (Set<ReadWriteBackingMap.Entry>) setAll)
                        {
                        getInternalCache().remove(entry.getBinaryKey());
                        getPendingRemoves().remove(entry.getBinaryKey());
                        }
                    }
                }
            catch (RuntimeException e)
                {
                ++m_cEraseFailures;
                onEraseAllFailure(setBinEntries, e);
                }
            finally
                {
                ++m_cEraseOps;
                long lElapsed = getSafeTimeMillis() - lStart;
                if (lElapsed != 0L)
                    {
                    m_cEraseMillis += lElapsed;
                    }
                }

            if (fAsynch && !setBinEntries.isEmpty())
                {
                for (Object o : setAll)
                    {
                    Entry entry = (Entry) o;

                    // if something failed, according to our convention entries that
                    // erase successfully are removed from the setBinEntries.
                    if (!setBinEntries.contains(entry))
                        {
                        getInternalCache().remove(entry.getBinaryKey());
                        getPendingRemoves().remove(entry.getBinaryKey());
                        }
                    }
                }
            }

        /**
        * Replace the value in the internal cache for the specified entry.
        * <p>
        * For write-behind, we should replace (and undecorate) the value *only*
        * if the internal cache still holds the value it contained when the
        * entry was de-queued for store operations.
        * <p>
        * Note: for write-through RWBM, this method is only called while the
        * entry is locked
        *
        * @param entry  the entry that holds the binary value to replace
        */
        protected void replace(Entry entry)
            {
            Map           mapInternal = getInternalCache();
            ConcurrentMap mapControl  = getControlMap();
            Binary        binKey      = entry.getBinaryKey();
            boolean       fSync       = getWriteQueue() == null;

            // Ensure that the key is owned and the entry is locked before doing
            // the replace operation.  Write-through or non-blocking operations
            // are guaranteed to have the entry locked and partition pinned.
            //
            // Note: there exists a small race condition here between the isKeyOwned()
            //       check and the put to the backing-map, where it is possible for
            //       service thread to start transferring the partition out.
            //       (see COH-6606, COH-6626)
            if (fSync ||
                (getContext().isKeyOwned(binKey) && mapControl.lock(binKey, 50)))
                {
                try
                    {
                    Binary binValue = entry.getBinaryValue();
                    if (Base.equals(binValue, mapInternal.get(binKey)))
                        {
                        if (entry.isChanged())
                            {
                            // the store operation changed the value; replace
                            // the existing value with the changed value
                            // Note: they would have to re-decorate the
                            // custom expiry by themselves if necessary
                            binValue = entry.getChangedBinaryValue();
                            }
                        // undecorate the persistence flag
                        //
                        // Note: Persistence decoration should not result in sending map
                        //       listener events.
                        //       See PartitionedCache.ResourceCoordinator.processEvent
                        binValue = ExternalizableHelper.undecorate(
                            binValue, BackingMapManagerContext.DECO_STORE);

                        ConfigurableCacheMap mapCCM  = getInternalConfigurableCache();
                        long                 cExpire = extractExpiry(entry);

                        // TTL of 1 ms is a "synthetic" one used by the accelerateEntryRipe
                        // method; either have it result in a remove or an evict

                        if (binValue == null || cExpire == 1L && mapCCM == null)
                            {
                            mapInternal.remove(binKey);
                            }
                        else if (cExpire == 1L)
                            {
                            mapCCM.evict(binKey);
                            }
                        else if (mapInternal instanceof CacheMap)
                            {
                            ((CacheMap) mapInternal).put(binKey, binValue, cExpire);
                            }
                        else
                            {
                            mapInternal.put(binKey, binValue);
                            }
                        }
                    }
                catch (RuntimeException e)
                    {
                    // there is a chance that while the write-behind store
                    // operation was in progress, the partition has been
                    // moved. In this case, for a PartitionAwareBackingMap
                    // the "put" would fail. We should treat it in exactly
                    // the same way as if the value is not there by doing
                    // nothing
                    }
                finally
                    {
                    if (!fSync)
                        {
                        mapControl.unlock(binKey);
                        }
                    }
                }
            else
                {
                // Either we failed to lock the entry (indicating that another
                // [service/worker] thread is working on it) or we no longer own
                // the partition; don't replace the value.  If this method is called
                // to replace the "STORE" decoration, the only effect would be a
                // potentially repetitive store() call on failover. If the entry was
                // changed by the BinaryEntryStore, all we promised was the "best effort"
                }
            }

        // ----- error handling -----------------------------------------

        /**
        * Logs a store load() failure. This method is intended to be
        * overwritten if a particular store can fail and the backing
        * map must take action based on it.
        *
        * @param oKeyReal  the key
        * @param e         the exception
        */
        protected void onLoadFailure(Object oKeyReal, Exception e)
            {
            onLoadFailure(oKeyReal, e, /*fThrow*/true);
            }

        /**
        * Logs a store load() failure. This method is intended to be
        * overwritten if a particular store can fail and the backing
        * map must take action based on it.
        *
        * @param oKeyReal  the key
        * @param e         the exception
        * @param fThrow    the caller can bubble up exceptions
        */
        protected void onLoadFailure(Object oKeyReal, Exception e, boolean fThrow)
            {
            if (isRethrowExceptions() && fThrow)
                {
                throw ensureRuntimeException(e, "Failed to load key=\""
                        + oKeyReal + "\"");
                }
            else
                {
                err("Failed to load key=\"" + oKeyReal + "\":");
                err(e);
                }
            }

        /**
        * Logs a store loadAll() failure. This method is intended to be
        * overwritten if a particular store can fail and the backing
        * map must take action based on it.
        *
        * @param colKeys  colKeys a collection of keys in external form to load
        * @param e        the exception
        */
        protected void onLoadAllFailure(Collection colKeys, Exception e)
            {
            onLoadAllFailure(colKeys, e, /*fThrow*/true);
            }

        /**
        * Logs a store loadAll() failure. This method is intended to be
        * overwritten if a particular store can fail and the backing
        * map must take action based on it.
        *
        * @param colKeys  colKeys a collection of keys in external form to load
        * @param e        the exception
        * @param fThrow   the caller can bubble up exceptions
        */
        protected void onLoadAllFailure(Collection colKeys, Exception e, boolean fThrow)
            {
            if (isRethrowExceptions() && fThrow)
                {
                throw ensureRuntimeException(e, "Failed to load keys=\""
                        + colKeys + "\"");
                }
            else
                {
                err("Failed to load keys=\"" + colKeys + "\":");
                err(e);
                }
            }

        /**
        * Logs a store store() failure. This method is intended to be
        * overwritten if a particular store can fail and the backing
        * map must take action based on it.
        *
        * @param entry  the entry
        * @param e      the exception
        */
        protected void onStoreFailure(Entry entry, Exception e)
            {
            onStoreFailure(entry, e, /*fThrow*/true);
            }

        /**
        * Logs a store store() failure. This method is intended to be
        * overwritten if a particular store can fail and the backing
        * map must take action based on it.
        *
        * @param entry   the entry
        * @param e       the exception
        * @param fThrow  whether the store mode supports exceptions
        */
        protected void onStoreFailure(Entry entry, Exception e, boolean fThrow)
            {
            WriteQueue  queue       = getWriteQueue();
            WriteThread writeThread = getWriteThread();
            int         cThreshold  = getWriteRequeueThreshold();

            if (e instanceof UnsupportedOperationException)
                {
                if (isStoreSupported())
                    {
                    setStoreSupported(false);
                    reportUnsupported("store");
                    }

                // force an unconditional requeue of the entry
                cThreshold = Integer.MAX_VALUE;
                }

            String sMsg = "Failed to store key=\"" + entry.getKey() + "\"";
            if (queue == null || Thread.currentThread() != writeThread.getThread())
                {
                // if write-behind is disabled or the store operation was
                // synchronous (i.e. not performed by the write-behind thread)
                // or the store is non blocking,
                // either log or rethrow the exception
                if (isRethrowExceptions() && fThrow)
                    {
                    throw ensureRuntimeException(e, sMsg);
                    }
                else
                    {
                    err(sMsg);
                    err(e);
                    }
                }
            else
                {
                // this is a write-behind map; log and requeue, if necessary
                err(sMsg);
                err(e);

                if (cThreshold != 0)
                    {
                    requeue(queue, cThreshold, entry);
                    }
                }
            }

        /**
        * Logs a store storeAll() failure. This method is intended to be
        * overwritten if a particular store can fail and the backing
        * map must take action based on it.
        *
        * @param setBinEntries  set of {@link Entry Entries}
        * @param e              the exception
        */
        protected void onStoreAllFailure(Set setBinEntries, Exception e)
            {
            onStoreAllFailure(setBinEntries, e, /*fThrow*/true);
            }

        /**
        * Logs a store storeAll() failure. This method is intended to be
        * overwritten if a particular store can fail and the backing
        * map must take action based on it.
        *
        * @param setBinEntries  set of {@link Entry Entries}
        * @param e              the exception
        * @param fThrow         whether the store mode supports exceptions
        */
        protected void onStoreAllFailure(Set setBinEntries, Exception e, boolean fThrow)
            {
            WriteQueue  queue       = getWriteQueue();
            WriteThread writeThread = getWriteThread();
            int         cThreshold  = getWriteRequeueThreshold();

            if (e instanceof UnsupportedOperationException)
                {
                if (isStoreAllSupported())
                    {
                    setStoreAllSupported(false);
                    reportUnsupported("storeAll");
                    }

                // force an unconditional requeue of all entries
                cThreshold = Integer.MAX_VALUE;
                }

            String sMsg = formatKeys(setBinEntries, "Failed to store");
            if (queue == null || Thread.currentThread() != writeThread.getThread())
                {
                // if write-behind is disabled or the storeAll operation was
                // synchronous (i.e. not performed by the write-behind thread)
                // or the store is non blocking,
                // either log or rethrow the exception
                if (isRethrowExceptions() && fThrow)
                    {
                    throw ensureRuntimeException(e, sMsg);
                    }
                else
                    {
                    err(sMsg);
                    err(e);
                    }
                }
            else
                {
                // this is a write-behind map; log and requeue, if necessary
                if (isStoreAllSupported())
                    {
                    err(sMsg);
                    err(e);
                    }

                if (cThreshold != 0)
                    {
                    for (Object o : setBinEntries)
                        {
                        requeue(queue, cThreshold, (Entry) o);
                        }
                    }
                }
            }

        /**
        * Requeue the specified entry.
        * <p>
        * Note: Subclasses could override this method and perform some type of
        * the "last recovery attempt" operation if the super.requeue(...) call
        * returns <tt>false</tt>.
        * Note 2: Starting with Coherence 3.6 a positive threshold value
        * ensures that entries are never dropped. The signature of the method
        * did not change to maintain backward compatibility.
        *
        * @param queue       the queue (never null)
        * @param cThreshold  the queue size threshold
        * @param entry       the entry to requeue
        *
        * @return starting with Coherence 3.6 this method always returns true
        */
        protected boolean requeue(WriteQueue queue, int cThreshold, Entry entry)
            {
            synchronized (queue)
                {
                BackingMapManagerContext ctx = getContext();

                Binary binKey = entry.getBinaryKey();

                // only requeue if there is no entry for this key;
                // NO_VALUE marker or a new value make the requeue unnecessary;
                // Note: while the store operation was in progress, the
                // partition could have been moved
                if (!queue.containsKey(binKey) && ctx.isKeyOwned(binKey))
                    {
                    long ldtDelay = calculateRequeueDelay(queue);
                    queue.add(entry, ldtDelay);
                    }
                }
            return true;
            }

        /**
        * Calculate the requeue delay after a store operation failed. The
        * default implementations delays the entry by at least a minute or
        * by the two times the write-behind delay.
        *
        * @param queue  the write-behind queue
        *
        * @return the number of milliseconds until another attempt should be
        *         made to persist the specified value
        */
        protected long calculateRequeueDelay(WriteQueue queue)
            {
            return Math.max(getWriteBehindMillis() * 2, MIN_REQUEUE_DELAY);
            }

        /**
        * Logs a store erase() failure. This method is intended to be
        * overwritten if a particular store can fail and the backing
        * map must take action based on it.
        *
        * @param entry  the entry
        * @param e      the exception
        */
        protected void onEraseFailure(Entry entry, Exception e)
            {
            // allow the store to avoid erasing
            if (e instanceof UnsupportedOperationException)
                {
                if (isEraseSupported())
                    {
                    setEraseSupported(false);
                    reportUnsupported("erase");
                    }
                }
            else
                {
                String sMsg = "Failed to erase key=\"" + entry.getKey() + "\"";
                if (isWriteBehindRemove())
                    {
                    WriteQueue  queue      = getWriteQueue();
                    int         cThreshold = getWriteRequeueThreshold();

                    // this is a write-behind map; log and requeue, if necessary
                    err(sMsg);
                    err(e);

                    if (queue != null && cThreshold != 0)
                        {
                        requeue(queue, cThreshold, entry);
                        }
                    }
                else
                    {
                    if (isRethrowExceptions())
                        {
                        throw ensureRuntimeException(e, sMsg);
                        }
                    else
                        {
                        err(sMsg);
                        err(e);
                        }
                    }
                }
            }

        /**
         * Logs a store erase() failure. This method is intended to be
         * overwritten if a particular store can fail and the backing
         * map must take action based on it.
         *
         * @param oKeyReal  the key
         * @param e         the exception
         */
        protected void onEraseFailure(Object oKeyReal, Exception e)
            {
            // allow the store to avoid erasing
            if (e instanceof UnsupportedOperationException)
                {
                if (isEraseSupported())
                    {
                    setEraseSupported(false);
                    reportUnsupported("erase");
                    }
                }
            else
                {
                String sMsg = "Failed to erase key=\"" + oKeyReal + "\"";
                if (isRethrowExceptions())
                    {
                    throw ensureRuntimeException(e, sMsg);
                    }
                else
                    {
                    err(sMsg);
                    err(e);
                    }
                }
            }

        /**
        * Logs a store eraseAll() failure. This method is intended to be
        * overwritten if a particular store can fail and the backing
        * map must take action based on it.
        *
        * @param setBinEntries  set of {@link Entry Entries}
        * @param e              the exception
        */
        protected void onEraseAllFailure(Set setBinEntries, Exception e)
            {
            // allow the store to avoid erasing
            if (e instanceof UnsupportedOperationException)
                {
                if (isEraseAllSupported())
                    {
                    setEraseAllSupported(false);
                    reportUnsupported("eraseAll");
                    }
                }
            else
                {
                String sMsg = formatKeys(setBinEntries, "Failed to eraseAll");
                if (isWriteBehindRemove())
                    {
                    WriteQueue  queue      = getWriteQueue();
                    int         cThreshold = getWriteRequeueThreshold();

                    // this is a write-behind map; log and requeue, if necessary
                    err(sMsg);
                    err(e);

                    if (queue != null && cThreshold != 0)
                        {
                        for (Object o : setBinEntries)
                            {
                            requeue(queue, cThreshold, (Entry) o);
                            }
                        }
                    }
                else
                    {
                    if (isRethrowExceptions())
                        {
                        throw ensureRuntimeException(e, sMsg);
                        }
                    else
                        {
                        err(sMsg);
                        err(e);
                        }
                    }
                }
            }

        /**
        * Log the info about an unsupported operation.
        *
        * @param sOp  the unsupported operation
        */
        protected void reportUnsupported(String sOp)
            {
            log("The cache store \"" + getStore() + "\" does not support the "
                 + sOp + " operation.");
            }

        /**
        * Generate a log message containing the keys from the specified set
        * of entries.
        *
        * @param setBinEntries  set of {@link Entry Entries}
        * @param sHeader        message header
        *
        * @return the formatted message
        */
        protected String formatKeys(Set setBinEntries, String sHeader)
            {
            StringBuilder sb = new StringBuilder();
            sb.append(sHeader)
              .append(" keys=\"");
            for (Object o : setBinEntries)
                {
                sb.append(((Entry) o).getKey())
                  .append(", ");
                }
            sb.append('"');

            return sb.toString();
            }

        // ----- subclassing support ------------------------------------

        /**
        * Return the cache store object to which this wrapper delegates.
        *
        * @return the cache store object to which this wrapper delegates
        */
        public abstract Object getStore();

        /**
        * Create the bundler for the load operations.
        *
        * @return the "load" bundler
        */
        protected abstract AbstractBundler instantiateLoadBundler();

        /**
        * Create the bundler for the store operations.
        *
        * @return the "store" bundler
        */
        protected abstract AbstractBundler instantiateStoreBundler();

        /**
        * Create the bundler for the erase operations.
        *
        * @return the "erase" bundler
        */
        protected abstract AbstractBundler instantiateEraseBundler();

        /**
        * Load the entry associated with the specified key from the underlying
        * store.
        *
        * @param binKey  binary key whose associated value is to be loaded
        *
        * @return the entry associated with the specified key, or
        *         <tt>null</tt> if no value is available for that key
        */
        protected abstract Entry loadInternal(Object binKey);

        /**
        * Load the entries associated with each of the specified binary keys
        * from the underlying store.
        *
        * @param setBinKey  a set of binary keys to load
        *
        * @return a Set of entries for the specified keys
        */
        protected abstract Set loadAllInternal(Set setBinKey);

        /**
        * Store the specified entry in the underlying store.
        *
        * @param binEntry  the entry to be stored
        */
        protected abstract void storeInternal(Entry binEntry);

        /**
        * Store the entries in the specified set in the underlying store.
        *
        * @param setBinEntries  the set of entries to be stored
        */
        protected abstract void storeAllInternal(Set setBinEntries);

        /**
        * Remove the specified entry from the underlying store.
        *
        * @param binEntry  the entry to be removed from the store
        */
        protected abstract void eraseInternal(Entry binEntry);

        /**
        * Remove the specified entries from the underlying store.
        *
        * @param setBinEntries  the set entries to be removed from the store
        */
        protected abstract void eraseAllInternal(Set setBinEntries);

        // ----- data members -------------------------------------------

        /**
        * The number of Load operations.
        */
        protected final AtomicLong f_cLoadOps              = new AtomicLong();

        /**
        * The number of Load failures.
        */
        protected final AtomicLong f_cLoadFailures         = new AtomicLong();

        /**
        * The cumulative time spent on Load operations.
        */
        protected final AtomicLong f_cLoadMillis           = new AtomicLong();

        /**
        * The number of Store operations.
        */
        protected final AtomicLong f_cStoreOps             = new AtomicLong();

        /**
        * The total number of entries written in Store operations.
        */
        protected final AtomicLong f_cStoreEntries         = new AtomicLong();

        /**
        * The number of Store failures.
        */
        protected final AtomicLong f_cStoreFailures        = new AtomicLong();

        /**
        * The cumulative time spent on Store operations.
        */
        protected final AtomicLong f_cStoreMillis          = new AtomicLong();

        /**
        * The number of pending non-blocking store operations.
        */
        protected final AtomicLong f_cPendingAsyncStoreOps = new AtomicLong();

        /**
        * The number of Erase operations.
        */
        protected volatile long m_cEraseOps;

        /**
        * The number of Erase failures.
        */
        protected volatile long m_cEraseFailures;

        /**
        * The cumulative time spent on Erase operations.
        */
        protected volatile long m_cEraseMillis;

        /**
        * Flag that determines whether or not Store operations are supported by
        * the wrapped store.
        */
        protected boolean m_fStoreSupported    = true;

        /**
        * Flag that determines whether or not StoreAll operations are supported
        * by the wrapped store.
        */
        protected boolean m_fStoreAllSupported = true;

        /**
        * Flag that determines whether or not Erase operations are supported by
        * the wrapped store.
        */
        protected boolean m_fEraseSupported    = true;

        /**
        * Flag that determines whether or not EraseAll operations are supported
        * by the wrapped store.
        */
        protected boolean m_fEraseAllSupported = true;

        /**
        * The bundler for load() operations.
        */
        protected AbstractBundler m_loadBundler;

        /**
        * The bundler for store() operations.
        */
        protected AbstractBundler m_storeBundler;

        /**
        * The bundler for erase() operations.
        */
        protected AbstractBundler m_eraseBundler;
        }


    // ----- inner class: CacheStoreWrapper ---------------------------------

    /**
    * A wrapper around the original CacheStore to allow operations to be
    * overridden and extended.
    *
    * @author cp 2002.06.04
    */
    public class CacheStoreWrapper
            extends StoreWrapper
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a CacheStoreWrapper.
        *
        * @param store  the CacheStore to wrap
        */
        public CacheStoreWrapper(CacheStore store)
            {
            azzert(store != null);
            m_store = store;
            }

        // ----- StoreWrapper -------------------------------------------

        /**
        * {@inheritDoc}
        */
        public AbstractBundler instantiateLoadBundler()
            {
            return new AbstractKeyBundler()
                {
                /**
                * A pass through the underlying loadAll operation.
                */
                protected Map bundle(Collection colKeys)
                    {
                    return getCacheStore().loadAll(colKeys);
                    }

                /**
                * A pass through the underlying load operation.
                */
                protected Object unbundle(Object oKey)
                    {
                    return getCacheStore().load(oKey);
                    }
                };
            }

        /**
        * {@inheritDoc}
        */
        public AbstractBundler instantiateStoreBundler()
            {
            return new AbstractEntryBundler()
                {
                /**
                * A pass through the underlying storeAll() operation.
                */
                protected void bundle(Map map)
                    {
                    getCacheStore().storeAll(map);
                    }
                };
            }

        /**
        * {@inheritDoc}
        */
        public AbstractBundler instantiateEraseBundler()
            {
            return new AbstractKeyBundler()
                {
                /**
                * A pass through the underlying eraseAll() operation.
                */
                protected Map bundle(Collection colKeys)
                    {
                    getCacheStore().eraseAll(colKeys);
                    return NullImplementation.getMap();
                    }

                /**
                * A pass through the underlying remove() operation.
                */
                protected Object unbundle(Object oKey)
                    {
                    getCacheStore().erase(oKey);
                    return null;
                    }
                };
            }

        /**
        * {@inheritDoc}
        */
        protected Entry loadInternal(Object binKey)
            {
            BackingMapManagerContext ctx  = getContext();
            Object                   oKey = ctx.getKeyFromInternalConverter().convert(binKey);
            Span                     span = newSpan("load").startSpan();

            try (@SuppressWarnings("unused") Scope scope = TracingHelper.getTracer().withSpan(span))
                {
                AbstractKeyBundler bundler = (AbstractKeyBundler)m_loadBundler;

                Object oValueReal = bundler == null ?
                        getCacheStore().load(oKey) : bundler.process(oKey);

                return oValueReal == null ? null :
                       instantiateEntry(binKey,
                            ctx.getValueToInternalConverter().convert(oValueReal), null);
                }
            catch (RuntimeException e)
                {
                // if it is desirable at this point for the load to truly
                // fail, then the onLoadFailure method should throw an
                // exception
                f_cLoadFailures.incrementAndGet();
                try
                    {
                    onLoadFailure(oKey, e);
                    }
                catch (RuntimeException re)
                    {
                    TracingHelper.augmentSpanWithErrorDetails(span, true, re);
                    throw re;
                    }
                return null;
                }
            finally
                {
                span.end();
                }
            }

        /**
        * {@inheritDoc}
        */
        protected Set loadAllInternal(Set setBinKey)
            {
            BackingMapManagerContext ctx         = getContext();
            Collection               colKeysReal =
                    ConverterCollections.getCollection(setBinKey,
                                                       ctx.getKeyFromInternalConverter(),
                                                       ctx.getKeyToInternalConverter());
            Span                     span        = newSpan("loadAll")
                    .withMetadata("entryCount", colKeysReal.size())
                    .startSpan();

            try (@SuppressWarnings("unused") Scope scope = TracingHelper.getTracer().withSpan(span))
                {
                AbstractKeyBundler bundler = (AbstractKeyBundler) m_loadBundler;

                Map map = bundler == null ?
                    getCacheStore().loadAll(colKeysReal) :
                    bundler.processAll(colKeysReal);

                Set setReturn = new HashSet(map.size());
                for (Object oEntry : map.entrySet())
                    {
                    Map.Entry entry = (Map.Entry) oEntry;

                    setReturn.add(instantiateEntry(
                        ctx.getKeyToInternalConverter().convert(entry.getKey()),
                        ctx.getValueToInternalConverter().convert(entry.getValue()), null));
                    }
                return setReturn;
                }
            catch (RuntimeException e)
                {
                // if it is desirable at this point for the load to truly
                // fail, then the onLoadFailure method should throw an
                // exception
                f_cLoadFailures.incrementAndGet();
                try
                    {
                    onLoadFailure(colKeysReal, e);
                    }
                catch (RuntimeException re)
                    {
                    TracingHelper.augmentSpanWithErrorDetails(span, true, re);
                    throw re;
                    }
                return Collections.EMPTY_SET;
                }
            finally
                {
                span.end();
                }
            }

        /**
        * {@inheritDoc}
        */
        protected void storeInternal(Entry binEntry)
            {
            azzert(!isReadOnly());

            Object oKey    = binEntry.getKey();
            Object oValue  = binEntry.getValue();

            AbstractEntryBundler bundler = (AbstractEntryBundler) m_storeBundler;
            Span                 span    = newSpan("store", binEntry).startSpan();

            try (@SuppressWarnings("unused") Scope scope = TracingHelper.getTracer().withSpan(span))
                {
                if (bundler == null)
                    {
                    getCacheStore().store(oKey, oValue);
                    }
                else
                    {
                    bundler.process(oKey, oValue);
                    }
                }
            catch (RuntimeException e)
                {
                TracingHelper.augmentSpanWithErrorDetails(span, true, e);
                throw e;
                }
            finally
                {
                span.end();
                }
            }

        /**
        * {@inheritDoc}
        */
        protected void storeAllInternal(Set setBinEntries)
            {
            azzert(!isReadOnly());

            Map mapEntries = new EntrySetMap(setBinEntries);

            AbstractEntryBundler bundler = (AbstractEntryBundler) m_storeBundler;
            Span                 span    = newSpan("storeAll", setBinEntries)
                    .withMetadata("entryCount", setBinEntries.size())
                    .startSpan();

            try (@SuppressWarnings("unused") Scope scope = TracingHelper.getTracer().withSpan(span))
                {
                if (bundler == null)
                    {
                    getCacheStore().storeAll(mapEntries);
                    }
                else
                    {
                    bundler.processAll(mapEntries);
                    }
                }
            catch (RuntimeException e)
                {
                TracingHelper.augmentSpanWithErrorDetails(span, true, e);
                throw e;
                }
            finally
                {
                span.end();
                }
            }

        /**
        * {@inheritDoc}
        */
        protected void eraseInternal(Entry binEntry)
            {
            azzert(!isReadOnly());

            Object oKey = binEntry.getKey();

            AbstractKeyBundler bundler = (AbstractKeyBundler) m_eraseBundler;
            Span                  span = newSpan("erase", binEntry).startSpan();

            try (@SuppressWarnings("unused") Scope scope = TracingHelper.getTracer().withSpan(span))
                {
                if (bundler == null)
                    {
                    getCacheStore().erase(oKey);
                    }
                else
                    {
                    bundler.process(oKey);
                    }
                }
            catch (RuntimeException e)
                {
                TracingHelper.augmentSpanWithErrorDetails(span, true, e);
                throw e;
                }
            finally
                {
                span.end();
                }
            }

        /**
        * {@inheritDoc}
        */
        protected void eraseAllInternal(Set setBinEntries)
            {
            azzert(!isReadOnly());

            Collection colKeys = new EntrySetMap(setBinEntries).keySet();

            AbstractKeyBundler bundler = (AbstractKeyBundler) m_eraseBundler;
            Span                  span = newSpan("eraseAll", setBinEntries)
                    .withMetadata("entryCount", setBinEntries.size())
                    .startSpan();

            try (@SuppressWarnings("unused") Scope scope = TracingHelper.getTracer().withSpan(span))
                {
                if (bundler == null)
                    {
                    getCacheStore().eraseAll(colKeys);
                    }
                else
                    {
                    bundler.processAll(colKeys);
                    }
                }
            catch (RuntimeException e)
                {
                TracingHelper.augmentSpanWithErrorDetails(span, true, e);
                throw e;
                }
            finally
                {
                span.end();
                }
            }

        /**
        * {@inheritDoc}
        */
        public Object getStore()
            {
            return getCacheStore();
            }

        // ----- accessors ----------------------------------------------

        /**
        * The wrapped CacheStore.
        *
        * @return the underlying CacheStore this CacheStoreWrapper wraps
        */
        public CacheStore getCacheStore()
            {
            return m_store;
            }

        // ----- helpers ------------------------------------------------

        /**
         * Return a {@link Span.Builder} for the specified operation.
         *
         * @param sOperation  the operation name
         *
         * @return the {@link Span.Builder}
         */
        protected Span.Builder newSpan(String sOperation)
            {
            return TracingHelper.newSpan("cachestore." + sOperation)
                    .withMetadata(Span.Type.COMPONENT.key(), "ReadWriteBackingMap");
            }

        /**
         * Return a {@link Span.Builder} for a given operation on an entry.
         *
         * @param sOperation  the operation name
         * @param entry       the entry
         *
         * @return the {@link Span.Builder}
         */
        protected Span.Builder newSpan(String sOperation, Entry entry)
            {
            Span.Builder builder = newSpan(sOperation);
            Span         span    = entry.getParentSpan();

            return span == TracingHelper.getActiveSpan()
                    ? builder
                    : builder.withAssociation(Span.Association.FOLLOWS_FROM.key(), span.getContext());
            }

        /**
         * Return a {@link Span.Builder} for a given operation on a set of entries.
         *
         * @param sOperation  the operation name
         * @param setEntries  the set of entries
         *
         * @return the {@link Span.Builder}
         */
        protected Span.Builder newSpan(String sOperation, Set<Entry> setEntries)
            {
            Span.Builder builder  = newSpan(sOperation);
            Span         spanLast = TracingHelper.getActiveSpan();

            if (spanLast != null)
                {
                for (Entry entry : setEntries)
                    {
                    Span span = entry.getParentSpan();
                    if (span != null && span != spanLast)
                        {
                        builder = builder.withAssociation(Span.Association.FOLLOWS_FROM.key(), span.getContext());
                        spanLast = span;
                        }
                    }
                }

            return builder;
            }

        // ----- Object overrides ---------------------------------------

        /**
        * Return a String representation of the CacheStoreWrapper object
        * that will be used as a part of the write-behind thread name.
        *
        * @return a String representation of the CacheStoreWrapper object
        */
        public String toString()
            {
            Class<?> clzStore = m_store.getClass();
            // if the cache store is CDI bean, clzStore will be a Weld proxy,
            // so we need to find the first parent class that isn't synthetic
            while (clzStore.isSynthetic())
                {
                clzStore = clzStore.getSuperclass();
                }
            return "Store=" + clzStore.getName();
            }

        // ----- data members -------------------------------------------

        /**
        * The wrapped CacheStore.
        */
        private CacheStore m_store;
        }

    // ----- inner class: NonBlockingEntryStoreWrapper ----------------------

    /**
    * A wrapper around the original NonBlockingEntryStore to allow operations
    * to be overridden and extended.
    */
    public class NonBlockingEntryStoreWrapper
            extends StoreWrapper
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a NonBlockingEntryStoreWrapper.
        *
        * @param store  the NonBlockingEntryStore to wrap
        */
        public NonBlockingEntryStoreWrapper(NonBlockingEntryStore store)
            {
            azzert(store != null);

            f_storeNonBlocking = store;
            }

        // ----- StoreWrapper -------------------------------------------

        /**
        * {@inheritDoc}
        */
        public AbstractBundler instantiateLoadBundler()
            {
            return new AbstractBinaryEntryBundler()
                {
                /**
                * A pass through to the underlying loadAll operation.
                */
                protected void bundle(Set setEntries)
                    {
                    for (Iterator iter = setEntries.iterator(); iter.hasNext(); )
                        {
                        Entry entry = (Entry) iter.next();

                        // remove from read queue if refresh ahead is configured
                        if (isRefreshAhead())
                            {
                            getReadQueue().remove(entry.getBinaryKey());
                            }
                        }

                    LoadOperationObserver observer = new LoadOperationObserver(setEntries.size());

                    getNonBlockingEntryStore().loadAll(setEntries, observer);

                    // block until non-blocking operation completes
                    observer.waitForCompleted();
                    }

                /**
                * A pass through to the underlying load operation.
                */
                protected void unbundle(BinaryEntry binEntry)
                    {
                    // remove from read queue if refresh ahead is configured
                    if (isRefreshAhead())
                        {
                        getReadQueue().remove(binEntry.getBinaryKey());
                        }

                    LoadOperationObserver observer = new LoadOperationObserver();

                    getNonBlockingEntryStore().load(binEntry, observer);

                    // block until non-blocking operation completes
                    observer.waitForCompleted();
                    }
                };
            }

        /**
        * {@inheritDoc}
        */
        public AbstractBundler instantiateStoreBundler()
            {
            return new AbstractBinaryEntryBundler()
                {
                /**
                * A pass through to the underlying storeAll operation.
                */
                protected void bundle(Set setEntries)
                    {
                    getNonBlockingEntryStore().storeAll(setEntries, new StoreOperationObserver(setEntries));
                    }

                /**
                * A pass through to the underlying store operation.
                */
                protected void unbundle(BinaryEntry binEntry)
                    {
                    getNonBlockingEntryStore().store(binEntry, new StoreOperationObserver());
                    }
                };
            }

        /**
        * {@inheritDoc}
        */
        public AbstractBundler instantiateEraseBundler()
            {
            return new AbstractBinaryEntryBundler()
                {
                /**
                * A pass through to the underlying eraseAll operation.
                */
                protected void bundle(Set setEntries)
                    {
                    getNonBlockingEntryStore().eraseAll(setEntries);
                    }

                /**
                * A pass through to the underlying erase operation.
                */
                protected void unbundle(BinaryEntry binEntry)
                    {
                    getNonBlockingEntryStore().erase(binEntry);
                    }
                };
            }

        /**
        * {@inheritDoc}
        */
        protected Entry loadInternal(Object binKey)
            {
            Entry binEntry = instantiateEntry(binKey, null, null);

            try
                {
                AbstractBinaryEntryBundler bundler =
                        (AbstractBinaryEntryBundler) m_loadBundler;

                if (bundler == null)
                    {
                    // assume that the caller has the key locked in the control map
                    // block the current thread until LoadObserver.(onNext | onError) is called

                    // remove from read queue if refresh ahead is configured
                    if (isRefreshAhead())
                        {
                        getReadQueue().remove(binKey);
                        }

                    LoadOperationObserver observer = new LoadOperationObserver();

                    getNonBlockingEntryStore().load(binEntry, observer);

                    observer.waitForCompleted();

                    putToInternalCache((Entry) binEntry);
                    }
                else
                    {
                    bundler.process(binEntry);
                    }
                return binEntry;
                }
            catch (RuntimeException e)
                {
                // if it is desirable at this point for the load to truly
                // fail, then the onLoadFailure method should throw an
                // exception
                f_cLoadFailures.incrementAndGet();
                // load is synchronous unlike other NonBlocking calls
                onLoadFailure(binKey, e);
                return null;
                }
            }

        /**
        * {@inheritDoc}
        */
        protected Set loadAllInternal(Set setBinKey)
            {
            Set setEntries = new HashSet(setBinKey.size());

            for (Object oKey : setBinKey)
                {
                setEntries.add(instantiateEntry(oKey, null, null));

                // remove from read queue if refresh ahead is configured
                if (isRefreshAhead())
                    {
                    getReadQueue().remove(oKey);
                    }
                }

            try
                {
                AbstractBinaryEntryBundler bundler =
                        (AbstractBinaryEntryBundler) m_loadBundler;

                if (bundler == null)
                    {
                    LoadOperationObserver observer = new LoadOperationObserver(setBinKey.size());
                    getNonBlockingEntryStore().loadAll(setEntries, observer);

                    // wait for all entries to come back or err out
                    observer.waitForCompleted();

                    return observer.getProcessedEntries();
                    }
                else
                    {
                    bundler.processAll(setEntries);

                    return setEntries;
                    }
                }
            catch (RuntimeException e)
                {
                // if it is desirable at this point for the load to truly
                // fail, then the onLoadAllFailure method should throw an
                // exception
                f_cLoadFailures.incrementAndGet();
                // loadAll is synchronous unlike other NonBlocking calls
                onLoadAllFailure(setBinKey, e);
                return Collections.EMPTY_SET;
                }
            }

        /**
        * {@inheritDoc}
        */
        protected void storeInternal(Entry binEntry)
            {
            azzert(!isReadOnly());

            binEntry.startTracking();

            AbstractBinaryEntryBundler bundler =
                    (AbstractBinaryEntryBundler) m_storeBundler;

            if (bundler == null)
                {
                getNonBlockingEntryStore().store(binEntry, new StoreOperationObserver());
                }
            else
                {
                bundler.process(binEntry);
                }
            }

        /**
        * {@inheritDoc}
        */
        protected void storeAllInternal(Set setBinEntries)
            {
            azzert(!isReadOnly());

            for (Object o : setBinEntries)
                {
                ((Entry) o).startTracking();
                }
            StoreOperationObserver observer = new StoreOperationObserver(setBinEntries);

            AbstractBinaryEntryBundler bundler =
                    (AbstractBinaryEntryBundler) m_storeBundler;

            if (bundler == null)
                {
                getNonBlockingEntryStore().storeAll(setBinEntries, observer);
                }
            else
                {
                bundler.processAll(setBinEntries);
                }
            }

        /**
        * {@inheritDoc}
        */
        protected void eraseInternal(Entry binEntry)
            {
            azzert(!isReadOnly());

            AbstractBinaryEntryBundler bundler =
                    (AbstractBinaryEntryBundler) m_eraseBundler;

            if (bundler == null)
                {
                getNonBlockingEntryStore().erase(binEntry);
                }
            else
                {
                bundler.process(binEntry);
                }
            }

        /**
        * {@inheritDoc}
        */
        protected void eraseAllInternal(Set setBinEntries)
            {
            azzert(!isReadOnly());

            AbstractBinaryEntryBundler bundler =
                    (AbstractBinaryEntryBundler) m_eraseBundler;

            if (bundler == null)
                {
                getNonBlockingEntryStore().eraseAll(setBinEntries);
                }
            else
                {
                bundler.processAll(setBinEntries);
                }
            }

        /**
        * {@inheritDoc}
        */
        public Object getStore()
            {
            return getNonBlockingEntryStore();
            }

        // ----- accessors --------------------------------------------------

        /**
        * The wrapped NonBlockingEntryStore.
        *
        * @return the underlying NonBlockingEntryStore this CacheStoreWrapper wraps
        */
        public NonBlockingEntryStore getNonBlockingEntryStore()
            {
            return f_storeNonBlocking;
            }

        // ----- Object overrides -------------------------------------------

        /**
        * Return a String representation of the NonBlockingEntryStoreWrapper
        * object that will be used as a part of the write-behind thread name.
        *
        * @return a String representation of the CacheStoreWrapper object
        */
        public String toString()
            {
            Class<?> clzStore = f_storeNonBlocking.getClass();
            // if the cache store is CDI bean, clzStore will be a Weld proxy,
            // so we need to find the first parent class that isn't synthetic
            while (clzStore.isSynthetic())
                {
                clzStore = clzStore.getSuperclass();
                }
            return "NonBlockingStore=" + clzStore.getName();
            }

        /**
        * Determine if the wrapped store implements blocking operations.
        *
        * @return true if the operations are blocking
        */
        public boolean isBlocking()
            {
            return false;
            }

        // ----- inner class: LoadOperationObserver ---------------------

        /**
        * Callbacks invoked by NonBlockingEntryStore implementation to handle
        * the result of load operations.
        */
        public class LoadOperationObserver
                implements StoreObserver
            {
            // ----- constructors -------------------------------------------

            /**
            * Default constructor
            */
            public LoadOperationObserver()
                {
                this(null, 1);
                }

            /**
            * Constructor for loadAll() code path.
            *
            * @param cEntries  countdown gate for synchronization
            */
            public LoadOperationObserver(int cEntries)
                {
                this(new SafeHashSet(), cEntries);
                }

            /**
             * Private constructor.
             *
             * @param setProcessed  set that will contain successfully processed
             *                      entries
             * @param cEntries      countdown gate for synchronization
             */
            private LoadOperationObserver(Set setProcessed, int cEntries)
                {
                f_setProcessedEntries = setProcessed;
                f_setEntriesInError   = new SafeHashSet();
                f_counterEntries      = new AtomicInteger(cEntries);
                }

            /**
            * {@inheritDoc}
            */
            public void onNext(BinaryEntry binEntry)
                {
                completeSync(binEntry, null);

                if (f_setProcessedEntries != null)
                    {
                    f_setProcessedEntries.add(binEntry);
                    }
                }

            /**
            * {@inheritDoc}
            */
            public void onError(BinaryEntry binEntry, Exception exception)
                {
                setThrowable(exception);

                completeSync(binEntry, exception);

                f_setEntriesInError.add(binEntry);
                }

            /**
             * {@inheritDoc}
             */
            public void onComplete()
                {
                f_counterEntries.set(-1);
                synchronized (f_counterEntries)
                    {
                    f_counterEntries.notify();
                    }
                }

            // ----- helpers ------------------------------------------------

            /**
            * Common code with onNext/onError for synchronization processing.
            *
            * @param binEntry   entry being handled
            * @param exception  exception to be relayed, if set
            */
            private void completeSync(BinaryEntry binEntry, Exception exception)
                {
                // the thread that called store.load or store.loadAll owns the
                // lock for this key and would have installed a countdown gate

                // prevent from calling sync for the same entry more than once
                if (f_setProcessedEntries != null &&
                    f_setProcessedEntries.contains(binEntry) ||
                    f_setEntriesInError.contains(binEntry))
                    {
                    return;
                    }

                if (f_counterEntries.get() == -1)
                    {
                    throw new IllegalStateException("A method was called on an already closed StoreObserver");
                    }

                if (f_counterEntries.decrementAndGet() == 0)
                    {
                    onComplete();
                    }
                }

            /**
            * Synchronize loadInternal or loadAllInternal with responses.
            *
            * @throws RuntimeException upon interruption
            */
            protected void waitForCompleted()
                {
                try
                    {
                    synchronized (f_counterEntries)
                        {
                        while (f_counterEntries.get() > 0)
                            {
                            Blocking.wait(f_counterEntries);
                            }
                        }
                    }
                catch (InterruptedException ie)
                    {
                    // load/loadAll failed, not much else to do...
                    throw Base.ensureRuntimeException(ie);
                    }

                // rethrow exception if there is one and no entry was processed
                if (f_setProcessedEntries != null &&
                    f_setProcessedEntries.size() == 0 &&
                    getThrowable() != null)
                    {
                    throw Base.ensureRuntimeException(getThrowable());
                    }
                }

            // ----- accessors ----------------------------------------------

            /**
            * Set of entries successfully processed in loadAll().
            *
            * @return set of good entries
            */
            protected Set getProcessedEntries()
                {
                return f_setProcessedEntries;
                }

            /**
            * Exception to be returned to caller.
            *
            * @return the exception
            */
            private Throwable getThrowable()
                {
                return m_error;
                }

            /**
            * Sets the exception to return to the caller.
            *
            * @param error  exception being set
            */
            private void setThrowable(Throwable error)
                {
                m_error = error;
                }

            // ----- data members -------------------------------------------

            /**
            * A count for the number of entries being stored.
            * Note: this is used as a semaphore for loading threads.
            */
            private final AtomicInteger f_counterEntries;

            /**
            * Holds the set of entries that were successfully processed.
            */
            private final Set f_setProcessedEntries;

            /**
            * Holds the set of entries that were in error.
            */
            private final Set f_setEntriesInError;

            /**
            * Exception encountered during load.
            */
            private Throwable m_error;
            }

        // ----- inner class: StoreOperationObserver --------------------

        /**
        * Callbacks invoked by NonBlockingEntryStore implementer to handle
        * the result of store operations.
        */
        public class StoreOperationObserver
                implements StoreObserver
            {
            // ----- constructors -------------------------------------------

            /**
            * Default constructor.
            */
            public StoreOperationObserver()
                {
                this(null);
                }

            /**
            * Constructor with initial set of entries to be stored.
            *
            * @param setEntries  the entries that need storing
            */
            public StoreOperationObserver(Set setEntries)
                {
                f_ldtStartTime   = getSafeTimeMillis();
                f_setUnProcessed = new SafeHashSet();
                if (setEntries != null)
                    {
                    f_setUnProcessed.addAll(setEntries);
                    }
                }

            /**
            * {@inheritDoc}
            */
            public void onNext(BinaryEntry binEntry)
                {
                Entry         entry      = (Entry) binEntry;
                ConcurrentMap mapControl = getControlMap();
                Object        binKey     = binEntry.getBinaryKey();

                f_counterProcessed.incrementAndGet();

                if (getContext().isKeyOwned(binKey) && mapControl.lock(binKey, 100L))
                    {
                    try
                        {
                        replace(entry);
                        getSetUnProcessed().remove(binEntry);
                        }
                    finally
                        {
                        entry.stopTracking();

                        mapControl.unlock(binKey);
                        }
                    }
                }

            /**
            * {@inheritDoc}
            */
            public void onError(BinaryEntry binEntry, Exception exception)
                {
                Entry         entry      = (Entry) binEntry;
                ConcurrentMap mapControl = getControlMap();
                Object        binKey     = binEntry.getBinaryKey();

                f_counterProcessed.incrementAndGet();
                f_cStoreFailures.incrementAndGet();
                onStoreFailure(entry, exception, false);

                if (getContext().isKeyOwned(binKey) && mapControl.lock(binKey, 100L))
                    {
                    try
                        {
                        replace(entry);
                        getSetUnProcessed().remove(binEntry);
                        }
                    finally
                        {
                        mapControl.unlock(binKey);
                        }
                    }
                }

            /**
             * {@inheritDoc}
             */
            public void onComplete()
                {
                // go through entries not processed by onNext/onError
                ConcurrentMap mapControl = getControlMap();

                // no entry was processed, onNext | onError were never called
                // treat as error
                if (f_counterProcessed.get() == 0)
                    {
                    f_cStoreFailures.incrementAndGet();
                    }

                f_cStoreOps.incrementAndGet();
                f_cStoreEntries.addAndGet(f_counterProcessed.get());

                long lElapsed = getSafeTimeMillis() - f_ldtStartTime;
                if (lElapsed != 0L)
                    {
                    f_cStoreMillis.addAndGet(lElapsed);
                    }

                f_cPendingAsyncStoreOps.decrementAndGet();

                // process (remove decoration for) any entries for which the non-blocking store
                // did not notify the StoreObserver.(onNext | onError)
                for (Iterator iter = getSetUnProcessed().iterator(); iter.hasNext(); )
                    {
                    Entry  entry  = (Entry) iter.next();
                    Object binKey = entry.getBinaryKey();
                    if (getContext().isKeyOwned(binKey) && mapControl.lock(binKey, 100L))
                        {
                        try
                            {
                            replace(entry);
                            }
                        finally
                            {
                            entry.stopTracking();

                            mapControl.unlock(binKey);
                            }
                        }
                    }
                }

            // ----- accessors ----------------------------------------------

            /**
            * Provides the set of entries resulting in errors.
            *
            * @return entries in error
            */
            private Set getSetUnProcessed()
                {
                return f_setUnProcessed;
                }

            // ----- data members -------------------------------------------

            /**
            * Start time for operation duration calculation.
            */
            private final long f_ldtStartTime;

            /**
            * Counter of entries seen in onNext and onError.
            */
            private final AtomicInteger f_counterProcessed = new AtomicInteger();

            /**
            * Set of entries in error.
            */
            private final Set f_setUnProcessed;
            }

        // ----- data members -------------------------------------------

        /**
        * The wrapped NonBlockingEntryStore.
        */
        private NonBlockingEntryStore f_storeNonBlocking;
        }

    // ----- inner class: BinaryEntryStoreWrapper ---------------------------

    /**
    * A wrapper around the original BinaryEntryStore to allow operations to be
    * overridden and extended.
    *
    * @author tb 2011.01.11
    */
    public class BinaryEntryStoreWrapper
            extends StoreWrapper
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a BinaryEntryStoreWrapper.
        *
        * @param store  the BinaryEntryStore to wrap
        */
        public BinaryEntryStoreWrapper(BinaryEntryStore store)
            {
            azzert(store != null);
            m_storeBinary = store;
            }

        // ----- StoreWrapper -------------------------------------------

        /**
        * {@inheritDoc}
        */
        public AbstractBundler instantiateLoadBundler()
            {
            return new AbstractBinaryEntryBundler()
                {
                /**
                * A pass through the underlying loadAll operation.
                */
                protected void bundle(Set setEntries)
                    {
                    getBinaryEntryStore().loadAll(setEntries);
                    }

                /**
                * A pass through the underlying load operation.
                */
                protected void unbundle(BinaryEntry binEntry)
                    {
                    getBinaryEntryStore().load(binEntry);
                    }
                };
            }

        /**
        * {@inheritDoc}
        */
        public AbstractBundler instantiateStoreBundler()
            {
            return new AbstractBinaryEntryBundler()
                {
                /**
                * A pass through the underlying storeAll operation.
                */
                protected void bundle(Set setEntries)
                    {
                    getBinaryEntryStore().storeAll(setEntries);
                    }

                /**
                * A pass through the underlying store operation.
                */
                protected void unbundle(BinaryEntry binEntry)
                    {
                    getBinaryEntryStore().store(binEntry);
                    }
                };
            }

        /**
        * {@inheritDoc}
        */
        public AbstractBundler instantiateEraseBundler()
            {
            return new AbstractBinaryEntryBundler()
                {
                /**
                * A pass through the underlying eraseAll operation.
                */
                protected void bundle(Set setEntries)
                    {
                    getBinaryEntryStore().eraseAll(setEntries);
                    }

                /**
                * A pass through the underlying erase operation.
                */
                protected void unbundle(BinaryEntry binEntry)
                    {
                    getBinaryEntryStore().erase(binEntry);
                    }
                };
            }

        /**
        * {@inheritDoc}
        */
        protected Entry loadInternal(Object binKey)
            {
            Entry binEntry = instantiateEntry(binKey, null, null);
            try
                {
                AbstractBinaryEntryBundler bundler =
                        (AbstractBinaryEntryBundler) m_loadBundler;
                if (bundler == null)
                    {
                    getBinaryEntryStore().load(binEntry);
                    }
                else
                    {
                    bundler.process(binEntry);
                    }
                return binEntry;
                }
            catch (RuntimeException e)
                {
                // if it is desirable at this point for the load to truly
                // fail, then the onLoadFailure method should throw an
                // exception
                f_cLoadFailures.incrementAndGet();
                onLoadFailure(binKey, e);
                return null;
                }
            }

        /**
        * {@inheritDoc}
        */
        protected Set loadAllInternal(Set setBinKey)
            {
            Set setEntry = new HashSet(setBinKey.size());
            for (Object oKey : setBinKey)
                {
                setEntry.add(instantiateEntry(oKey, null, null));
                }

            try
                {
                AbstractBinaryEntryBundler bundler =
                        (AbstractBinaryEntryBundler) m_loadBundler;
                if (bundler == null)
                    {
                    getBinaryEntryStore().loadAll(setEntry);
                    }
                else
                    {
                    bundler.processAll(setEntry);
                    }
                return setEntry;
                }
            catch (RuntimeException e)
                {
                // if it is desirable at this point for the load to truly
                // fail, then the onLoadFailure method should throw an
                // exception
                f_cLoadFailures.incrementAndGet();
                onLoadAllFailure(setBinKey, e);
                return Collections.EMPTY_SET;
                }
            }

        /**
        * {@inheritDoc}
        */
        protected void storeInternal(Entry binEntry)
            {
            azzert(!isReadOnly());

            binEntry.startTracking();

            AbstractBinaryEntryBundler bundler =
                    (AbstractBinaryEntryBundler) m_storeBundler;
            if (bundler == null)
                {
                getBinaryEntryStore().store(binEntry);
                }
            else
                {
                bundler.process(binEntry);
                }
            }

        /**
        * {@inheritDoc}
        */
        protected void storeAllInternal(Set setBinEntries)
            {
            azzert(!isReadOnly());

            for (Object o : setBinEntries)
                {
                ((Entry) o).startTracking();
                }

            AbstractBinaryEntryBundler bundler =
                    (AbstractBinaryEntryBundler) m_storeBundler;
            if (bundler == null)
                {
                getBinaryEntryStore().storeAll(setBinEntries);
                }
            else
                {
                bundler.processAll(setBinEntries);
                }
            }

        /**
        * {@inheritDoc}
        */
        protected void eraseInternal(Entry binEntry)
            {
            azzert(!isReadOnly());

            AbstractBinaryEntryBundler bundler =
                    (AbstractBinaryEntryBundler) m_eraseBundler;
            if (bundler == null)
                {
                getBinaryEntryStore().erase(binEntry);
                }
            else
                {
                bundler.process(binEntry);
                }
            }

        /**
        * {@inheritDoc}
        */
        protected void eraseAllInternal(Set setBinEntries)
            {
            azzert(!isReadOnly());

            AbstractBinaryEntryBundler bundler =
                    (AbstractBinaryEntryBundler) m_eraseBundler;
            if (bundler == null)
                {
                getBinaryEntryStore().eraseAll(setBinEntries);
                }
            else
                {
                bundler.processAll(setBinEntries);
                }
            }

        /**
        * {@inheritDoc}
        */
        public Object getStore()
            {
            return getBinaryEntryStore();
            }

        // ----- accessors ----------------------------------------------

        /**
        * The wrapped BinaryEntryStore.
        *
        * @return the underlying BinaryEntryStore this CacheStoreWrapper wraps
        */
        public BinaryEntryStore getBinaryEntryStore()
            {
            return m_storeBinary;
            }

        // ----- Object overrides ---------------------------------------

        /**
        * Return a String representation of the BinaryEntryStoreWrapper object
        * that will be used as a part of the write-behind thread name.
        *
        * @return a String representation of the CacheStoreWrapper object
        */
        public String toString()
            {
            Class<?> clzStore = m_storeBinary.getClass();
            // if the cache store is CDI bean, clzStore will be a Weld proxy,
            // so we need to find the first parent class that isn't synthetic
            while (clzStore.isSynthetic())
                {
                clzStore = clzStore.getSuperclass();
                }
            return "BinaryStore=" + clzStore.getName();
            }

        // ----- data members -------------------------------------------

        /**
        * The wrapped BinaryEntryStore.
        */
        private BinaryEntryStore m_storeBinary;
        }


    // ----- inner class: CacheLoaderCacheStore -----------------------------

    /**
    * Factory pattern: Instantiate a CacheLoaderCacheStore wrapper around a
    * passed CacheLoader.
    *
    * @param loader  the CacheLoader to wrap; never null
    *
    * @return a CacheStore instance
    */
    protected CacheStore instantiateCacheLoaderCacheStore(CacheLoader loader)
        {
        return new CacheLoaderCacheStore(loader);
        }

    /**
    * A CacheStore wrapped around a CacheLoader.
    *
    * @author cp 2002.06.04
    */
    public static class CacheLoaderCacheStore
            extends AbstractCacheStore
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a CacheLoaderCacheStore.
        *
        * @param loader  the CacheLoader to wrap
        */
        public CacheLoaderCacheStore(CacheLoader loader)
            {
            azzert(loader != null);
            m_loader = loader;
            }

        // ----- CacheStore interface -----------------------------------

        /**
        * {@inheritDoc}
        */
        public Object load(Object oKey)
            {
            return getCacheLoader().load(oKey);
            }

        /**
        * {@inheritDoc}
        */
        public Map loadAll(Collection colKeys)
            {
            return getCacheLoader().loadAll(colKeys);
            }

        // ----- accessors ----------------------------------------------

        /**
        * The wrapped CacheLoader.
        *
        * @return the underlying CacheLoader that this CacheStore wraps
        */
        protected CacheLoader getCacheLoader()
            {
            return m_loader;
            }

        // ----- data members -------------------------------------------

        /**
        * The wrapped CacheLoader.
        */
        private CacheLoader m_loader;
        }


    // ----- inner class: EvictingBackupMap ---------------------------------

    /**
    * A Map implementation used for a backup map that evicts all data that
    * has been successfully written.
    *
    * @since Coherence 3.4
    */
    public static class EvictingBackupMap
            extends AbstractKeyBasedMap
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        */
        public EvictingBackupMap()
            {
            }

        // ----- Map methods --------------------------------------------

        /**
        * {@inheritDoc}
        */
        public Object get(Object oKey)
            {
            return m_map.get(oKey);
            }

        /**
        * {@inheritDoc}
        */
        public Object put(Object oKey, Object oValue)
            {
            Binary binDeco = ExternalizableHelper.getDecoration((Binary) oValue,
                    BackingMapManagerContext.DECO_STORE);

            return equals(binDeco, BIN_STORE_PENDING)
                   ? m_map.put(oKey, oValue)
                   : m_map.remove(oKey);
            }

        /**
        * {@inheritDoc}
        */
        public Object remove(Object oKey)
            {
            return m_map.remove(oKey);
            }

        // ----- AbstractKeyBasedMap methods ----------------------------

        /**
        * {@inheritDoc}
        */
        protected Iterator iterateKeys()
            {
            return m_map.keySet().iterator();
            }

        // ----- data members -------------------------------------------

        /**
        * The actual storage for the backup Map.
        */
        private Map m_map = new SafeHashMap();
        }


    // ----- constants ------------------------------------------------------

    /**
    * Marker object used by {@link Entry} to indicate {@link Entry#remove(boolean)
    * remove} was called on the Entry.
    */
    protected static final Binary REMOVED = new Binary();

    /**
    * The binary form of the decoration indicating that the CacheStore
    * on the primary member has not yet written the value.
    */
    protected static final Binary BIN_STORE_PENDING =
            ExternalizableHelper.toBinary(Boolean.FALSE);

    /**
    * The recovery threshold to use for guarded execution of
    * write-behind/refresh-ahead threads.
    */
    protected static final float GUARD_RECOVERY = 0.9F;

    /**
    * The minimum "retry after requeue" time interval. Default value is 60 sec and
    * can be overridden by the system property:
    * <pre>
    * coherence.rwbm.requeue.delay
    * </pre>
    */
    public static final long MIN_REQUEUE_DELAY = Config.getLong("coherence.rwbm.requeue.delay", 60000L);

    /**
     * Binary representation of a decorated null for write-behind remove.
     *
     * @since 12.2.1.4.18
     */
    public static final Binary BIN_ERASE_PENDING = ExternalizableHelper.decorate(ExternalizableHelper.toBinary(null),
            BackingMapManagerContext.DECO_STORE, BIN_STORE_PENDING);

    /**
     * A Boolean system property to control whether write behind remove is enabled.
     *
     * @since 12.1.4.18
     */
    public static final String PROP_WB_REMOVE_DEFAULT = "coherence.rwbm.writebehind.remove.default";

    /**
     * The default write behind remove behavior.  This behavior will be used
     * if write behind remove has not been specified in the cache configuration
     * for a RWBM instance.  The default value is false and can be overridden
     * by the system property:
     * <pre>
     * coherence.rwbm.writebehind.remove.default
     * </pre>
     *
     * @since 12.2.1.4.18
     */
    public static final boolean RWBM_WB_REMOVE_DEFAULT = Config.getBoolean(PROP_WB_REMOVE_DEFAULT, false);

    // ----- data fields ----------------------------------------------------

    /**
    * An EvicitonApprover used when write behind is enabled to disapprove the
    * eviction of entries pending to be written to the cache store.
    */
    private final ConfigurableCacheMap.EvictionApprover f_writeBehindDisapprover = new ConfigurableCacheMap.EvictionApprover()
        {
        public boolean isEvictable(
                ConfigurableCacheMap.Entry cacheEntry)
            {
            return m_queueWrite.accelerateEntryRipe((Binary) cacheEntry.getKey());
            }
        };

    /**
    * The context information provided by the CacheService.
    */
    private BackingMapManagerContext m_ctxService;

    /**
    * True until the map is released.
    */
    private boolean          m_fActive = true;

    /**
    * The representative of the "in-memory" storage for this backing map.
    */
    private ObservableMap    m_mapInternal;

    /**
    * The Map used to cache CacheLoader (or CacheStore) misses.
    */
    private Map              m_mapMisses;

    /**
     * The Set used to keep track of CacheStore pending removes.
     *
     * @since 12.2.1.4.18
     */
    private Set              m_setPendingRemoves;

    /**
    * The concurrency control map for this backing map.
    */
    private ConcurrentMap    m_mapControl;

    /**
    * The MapListener that this backing map uses to listen to the internal
    * "in-memory" storage for this backing map.
    */
    private MapListener      m_listenerInternal;

    /**
    * The set of keys for which the events should be marked as synthetic
    * (internal).
    */
    private Map              m_mapSyntheticEvents;

    /**
    * The optional representative of the "persistent" storage for this
    * backing map.
    */
    private StoreWrapper     m_store;

    /**
    * Specifies a read-write cache if false, which will send changes to the
    * CacheStore, or a read-only cache if true, which will just keep changes
    * in memory.
    */
    private boolean          m_fReadOnly;

    /**
    * The queue of entries to be read asynchronously. Null if refresh-ahead
    * is not enabled.
    */
    private ReadQueue        m_queueRead;

    /**
    * The thread responsible for refresh-ahead processing. Null if refresh-
    * ahead is not enabled.
    */
    private ReadThread       m_daemonRead;

    /**
    * The queue of entries that have not yet been delegated to the
    * CacheStore. Null if write-behind is not enabled.
    */
    private WriteQueue       m_queueWrite;

    /**
    * The thread responsible for write-behind processing. Null if
    * write-behind is not enabled.
    */
    private WriteThread      m_daemonWrite;

    /**
    * MapListenerSupport object.
    */
    protected MapListenerSupport m_listenerSupport;

    /**
    * The EntrySet for this backing map.
    */
    private EntrySet         m_entryset;

    /**
    * The KeySet for this backing map.
    */
    private KeySet           m_keyset;

    /**
    * The ValuesCollection for this backing map.
    */
    private ValuesCollection m_values;

    /**
    * The write-behind delay (in milliseconds)
    */
    private long             m_cWriteBehindMillis;

    /**
    * The timeout to use for cache store operations.
    */
    private long             m_cStoreTimeoutMillis;

    /**
    * The refresh-ahead factor, 0 if refresh-ahead is disabled
    */
    private volatile double  m_dflRefreshAheadFactor;

    /**
    * Flag that indicates whether exceptions caught during synchronous
    * CacheStore operations are rethrown to the calling thread or not;
    * if false, exceptions are logged.
    */
    private volatile boolean m_fRethrowExceptions;

    /**
    * The write-batch factor.
    */
    private volatile double  m_dflWriteBatchFactor;

    /**
    * The maximum size of the write-behind queue for which failed CacheStore
    * write operations are requeued; if -1, write-behind requeueing is disabled.
    */
    private volatile int     m_cWriteRequeueThreshold;

    /**
    * Controls the maximum size of a storeAll batch.
    */
    private int              m_cWriteMaxBatchSize = 128;

    /**
     * Specifies whether the CacheStore will perform write-behind remove
     * operations. This property only applies to write-behind CacheStores.
     *
     * @since 12.2.1.4.18
     */
    private boolean          m_fWBRemove;
    }
