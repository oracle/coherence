/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.Base;

import java.io.Serializable;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;


/**
* Implementation of the CacheStatistics interface intended for use by a cache
* to maintain its statistics.
* <p>
* Registration methods atomically update individual counters, so concurrent
* registrations do not lose updates. Reads of multiple counters, including
* derived statistics, are not an atomic snapshot. Totals are exact once all
* registrations complete, provided statistics are not reset concurrently.
* A reset clears each counter independently; registrations racing with a reset
* can be counted before or after that counter is cleared.
*
* @since Coherence 2.2
* @author cp  2003.06.02
*/
public class SimpleCacheStatistics
        extends Base
        implements CacheStatistics, Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public SimpleCacheStatistics()
        {
        }


    // ----- CacheStatistics interface --------------------------------------

    /**
    * {@inheritDoc}
    */
    public long getTotalGets()
        {
        return m_cCacheHits + m_cCacheMisses;
        }

    /**
    * {@inheritDoc}
    */
    public long getTotalGetsMillis()
        {
        return m_cHitsMillis + m_cMissesMillis;
        }

    /**
    * {@inheritDoc}
    */
    public double getAverageGetMillis()
        {
        double cMillis = m_cHitsMillis + m_cMissesMillis;
        double cGets   = m_cCacheHits  + m_cCacheMisses;
        return cGets == 0.0 ? 0.0 : cMillis / cGets;
        }

    /**
    * {@inheritDoc}
    */
    public long getTotalPuts()
        {
        return m_cCachePuts;
        }

    /**
    * {@inheritDoc}
    */
    public long getTotalPutsMillis()
        {
        return m_cPutsMillis;
        }

    /**
    * {@inheritDoc}
    */
    public double getAveragePutMillis()
        {
        double cMillis = m_cPutsMillis;
        double cPuts   = m_cCachePuts;
        return cPuts == 0.0 ? 0.0 : cMillis / cPuts;
        }

    /**
    * {@inheritDoc}
    */
    public long getCacheHits()
        {
        return m_cCacheHits;
        }

    /**
    * {@inheritDoc}
    */
    public long getCacheHitsMillis()
        {
        return m_cHitsMillis;
        }

    /**
    * {@inheritDoc}
    */
    public double getAverageHitMillis()
        {
        double cMillis = m_cHitsMillis;
        double cGets   = m_cCacheHits;
        return cGets == 0.0 ? 0.0 : cMillis / cGets;
        }

    /**
    * {@inheritDoc}
    */
    public long getCacheMisses()
        {
        return m_cCacheMisses;
        }

    /**
    * {@inheritDoc}
    */
    public long getCacheMissesMillis()
        {
        return m_cMissesMillis;
        }

    /**
    * {@inheritDoc}
    */
    public double getAverageMissMillis()
        {
        double cMillis = m_cMissesMillis;
        double cGets   = m_cCacheMisses;
        return cGets == 0.0 ? 0.0 : cMillis / cGets;
        }

    /**
    * {@inheritDoc}
    */
    public double getHitProbability()
        {
        double cHits   = m_cCacheHits;
        double cTotal  = cHits + m_cCacheMisses;
        return cTotal == 0.0 ? 0.0 : cHits / cTotal;
        }

    /**
    * {@inheritDoc}
    */
    public long getCachePrunes()
        {
        return m_cCachePrunes;
        }

    /**
    * {@inheritDoc}
    */
    public long getCachePrunesMillis()
        {
        return m_cCachePrunesMillis;
        }

    /**
    * Calculate the average number of milliseconds that a prune takes.
    *
    * @return the average number of milliseconds spent per pruning cycle
    */
    public double getAveragePruneMillis()
        {
        double cMillis = m_cCachePrunesMillis;
        double cPrunes = m_cCachePrunes;
        return cPrunes == 0.0 ? 0.0 : cMillis / cPrunes;
        }

    /**
    * {@inheritDoc}
    */
    public void resetHitStatistics()
        {
        HITS.set(this, 0L);
        MISSES.set(this, 0L);
        HIT_MILLIS.set(this, 0L);
        MISS_MILLIS.set(this, 0L);
        PUT_MILLIS.set(this, 0L);
        PUTS.set(this, 0L);
        PRUNES.set(this, 0L);
        PRUNE_MILLIS.set(this, 0L);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * For debugging purposes, format the contents of the
    * SimpleCachingStatistics in a human readable format.
    *
    * @return a String representation of this object
    */
    public String toString()
        {
        StringBuffer sb = new StringBuffer();

        sb.append("CacheStatistics {TotalGets=")
          .append(getTotalGets())
          .append(", TotalGetsMillis=")
          .append(getTotalGetsMillis())
          .append(", AverageGetMillis=")
          .append(toString(getAverageGetMillis(), 5))
          .append(", TotalPuts=")
          .append(getTotalPuts())
          .append(", TotalPutsMillis=")
          .append(getTotalPutsMillis())
          .append(", AveragePutMillis=")
          .append(toString(getAveragePutMillis(), 5))
          .append(", CacheHits=")
          .append(getCacheHits())
          .append(", CacheHitsMillis=")
          .append(getCacheHitsMillis())
          .append(", AverageHitMillis=")
          .append(toString(getAverageHitMillis(), 5))
          .append(", CacheMisses=")
          .append(getCacheMisses())
          .append(", CacheMissesMillis=")
          .append(getCacheMissesMillis())
          .append(", AverageMissMillis=")
          .append(toString(getAverageMissMillis(), 5))
          .append(", HitProbability=")
          .append(toString(getHitProbability(), 5))
          .append(", Prunes=")
          .append(getCachePrunes())
          .append(", PruneMillis=")
          .append(getCachePrunesMillis())
          .append(", AveragePruneMillis=")
          .append(toString(getAveragePruneMillis(), 5))
          .append('}');

        return sb.toString();
        }


    // ----- accessors, mutators and helpers --------------------------------

    /**
    * Register a cache hit (no timing information).
    */
    public void registerHit()
        {
        HITS.incrementAndGet(this);
        }

    /**
    * Register a cache hit.
    *
    * @param lStartMillis  the time when the get operation started
    */
    public void registerHit(long lStartMillis)
        {
        HITS.incrementAndGet(this);
        long lStopMillis = getSafeTimeMillis();
        if (lStopMillis > lStartMillis)
            {
            HIT_MILLIS.addAndGet(this, (lStopMillis - lStartMillis));
            }
        }

    /**
    * Register a multiple cache hit.
    *
    * @param cHits         the number of hits
    * @param lStartMillis  the time when the get operation started
    */
    public void registerHits(int cHits, long lStartMillis)
        {
        HITS.addAndGet(this, cHits);
        if (lStartMillis > 0)
            {
            long lStopMillis = getSafeTimeMillis();
            if (lStopMillis > lStartMillis)
                {
                HIT_MILLIS.addAndGet(this, (lStopMillis - lStartMillis));
                }
            }
        }

    /**
    * Register a cache miss (no timing information).
    */
    public void registerMiss()
        {
        MISSES.incrementAndGet(this);
        }

    /**
    * Register a cache miss.
    *
    * @param lStartMillis  the time when the get operation started
    */
    public void registerMiss(long lStartMillis)
        {
        MISSES.incrementAndGet(this);
        long lStopMillis = getSafeTimeMillis();
        if (lStopMillis > lStartMillis)
            {
            MISS_MILLIS.addAndGet(this, (lStopMillis - lStartMillis));
            }
        }

    /**
    * Register a multiple cache miss.
    *
    * @param cMisses       the number of misses
    * @param lStartMillis  the time when the get operation started
    */
    public void registerMisses(int cMisses, long lStartMillis)
        {
        MISSES.addAndGet(this, cMisses);
        if (lStartMillis > 0)
            {
            long lStopMillis = getSafeTimeMillis();
            if (lStopMillis > lStartMillis)
                {
                MISS_MILLIS.addAndGet(this, (lStopMillis - lStartMillis));
                }
            }
        }

    /**
    * Register a cache put.
    *
    * @param lStartMillis  the time when the put operation started
    */
    public void registerPut(long lStartMillis)
        {
        PUTS.incrementAndGet(this);
        if (lStartMillis > 0)
            {
            long lStopMillis = getSafeTimeMillis();
            if (lStopMillis > lStartMillis)
                {
                PUT_MILLIS.addAndGet(this, (lStopMillis - lStartMillis));
                }
            }
        }

    /**
    * Register a multiple cache put.
    *
    * @param cPuts         the number of puts
    * @param lStartMillis  the time when the put operation started
    */
    public void registerPuts(int cPuts, long lStartMillis)
        {
        PUTS.addAndGet(this, cPuts);
        if (lStartMillis > 0)
            {
            long lStopMillis = getSafeTimeMillis();
            if (lStopMillis > lStartMillis)
                {
                PUT_MILLIS.addAndGet(this, (lStopMillis - lStartMillis));
                }
            }
        }

    /**
    * Register a cache prune.
    *
    * @param lStartMillis  the time when the prune operation started
    */
    public void registerCachePrune(long lStartMillis)
        {
        PRUNES.incrementAndGet(this);
        registerIncrementalCachePrune(lStartMillis);
        }

    /**
    * Register an incremental cache prune, which is to say that the time is
    * accreted but the number of prunes does not increase.
    *
    * @param lStartMillis  the time when the prune operation started
    *
    * @since Coherence 3.5
    */
    public void registerIncrementalCachePrune(long lStartMillis)
        {
        if (lStartMillis > 0)
            {
            long lStopMillis = getSafeTimeMillis();
            if (lStopMillis > lStartMillis)
                {
                PRUNE_MILLIS.addAndGet(this, (lStopMillis - lStartMillis));
                }
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * Preserve the serial form used before atomic counter updates were added.
    */
    private static final long serialVersionUID = 4235773659525587475L;

    /**
    * Atomic updater for {@link #m_cCacheHits}.
    */
    private static final AtomicLongFieldUpdater<SimpleCacheStatistics> HITS =
            AtomicLongFieldUpdater.newUpdater(SimpleCacheStatistics.class, "m_cCacheHits");

    /**
    * Atomic updater for {@link #m_cCacheMisses}.
    */
    private static final AtomicLongFieldUpdater<SimpleCacheStatistics> MISSES =
            AtomicLongFieldUpdater.newUpdater(SimpleCacheStatistics.class, "m_cCacheMisses");

    /**
    * Atomic updater for {@link #m_cHitsMillis}.
    */
    private static final AtomicLongFieldUpdater<SimpleCacheStatistics> HIT_MILLIS =
            AtomicLongFieldUpdater.newUpdater(SimpleCacheStatistics.class, "m_cHitsMillis");

    /**
    * Atomic updater for {@link #m_cMissesMillis}.
    */
    private static final AtomicLongFieldUpdater<SimpleCacheStatistics> MISS_MILLIS =
            AtomicLongFieldUpdater.newUpdater(SimpleCacheStatistics.class, "m_cMissesMillis");

    /**
    * Atomic updater for {@link #m_cCachePuts}.
    */
    private static final AtomicLongFieldUpdater<SimpleCacheStatistics> PUTS =
            AtomicLongFieldUpdater.newUpdater(SimpleCacheStatistics.class, "m_cCachePuts");

    /**
    * Atomic updater for {@link #m_cPutsMillis}.
    */
    private static final AtomicLongFieldUpdater<SimpleCacheStatistics> PUT_MILLIS =
            AtomicLongFieldUpdater.newUpdater(SimpleCacheStatistics.class, "m_cPutsMillis");

    /**
    * Atomic updater for {@link #m_cCachePrunes}.
    */
    private static final AtomicLongFieldUpdater<SimpleCacheStatistics> PRUNES =
            AtomicLongFieldUpdater.newUpdater(SimpleCacheStatistics.class, "m_cCachePrunes");

    /**
    * Atomic updater for {@link #m_cCachePrunesMillis}.
    */
    private static final AtomicLongFieldUpdater<SimpleCacheStatistics> PRUNE_MILLIS =
            AtomicLongFieldUpdater.newUpdater(SimpleCacheStatistics.class, "m_cCachePrunesMillis");


    // ----- data members ---------------------------------------------------

    /**
    * The number of cache hits since the last statistics reset.
    */
    protected volatile long m_cCacheHits;

    /**
    * Total number of milliseconds used for get operations that were hits
    * since the last statistics reset.
    */
    protected volatile long m_cHitsMillis;

    /**
    * The number of cache misses since the last statistics reset.
    */
    protected volatile long m_cCacheMisses;

    /**
    * Total number of milliseconds used for get operations that were misses
    * since the last statistics reset.
    */
    protected volatile long m_cMissesMillis;

    /**
    * Total number of put operations since the last statistics reset.
    */
    protected volatile long m_cCachePuts;

    /**
    * Total number of milliseconds used for put operations since the last
    * statistics reset.
    */
    protected volatile long m_cPutsMillis;

    /**
    * Total number of evictions triggered based on the size of the cache
    * since the last statistics reset.
    */
    protected volatile long m_cCachePrunes;

    /**
    * Total number of milliseconds used for prune operations since the last
    * statistics reset.
    */
    protected volatile long m_cCachePrunesMillis;
    }