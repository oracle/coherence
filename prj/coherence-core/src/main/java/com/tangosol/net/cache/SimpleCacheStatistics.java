/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.Base;

import java.io.Serializable;


/**
* Implementation of the CacheStatistics interface intended for use by a cache
* to maintain its statistics.
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
        m_cCacheHits         = 0L;
        m_cCacheMisses       = 0L;
        m_cHitsMillis        = 0L;
        m_cMissesMillis      = 0L;
        m_cPutsMillis        = 0L;
        m_cCachePuts         = 0L;
        m_cCachePrunes       = 0L;
        m_cCachePrunesMillis = 0L;
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
        ++m_cCacheHits;
        }

    /**
    * Register a cache hit.
    *
    * @param lStartMillis  the time when the get operation started
    */
    public void registerHit(long lStartMillis)
        {
        m_cCacheHits++;
        long lStopMillis = getSafeTimeMillis();
        if (lStopMillis > lStartMillis)
            {
            m_cHitsMillis += (lStopMillis - lStartMillis);
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
        m_cCacheHits += cHits;
        if (lStartMillis > 0)
            {
            long lStopMillis = getSafeTimeMillis();
            if (lStopMillis > lStartMillis)
                {
                m_cHitsMillis += (lStopMillis - lStartMillis);
                }
            }
        }

    /**
    * Register a cache miss (no timing information).
    */
    public void registerMiss()
        {
        ++m_cCacheMisses;
        }

    /**
    * Register a cache miss.
    *
    * @param lStartMillis  the time when the get operation started
    */
    public void registerMiss(long lStartMillis)
        {
        m_cCacheMisses++;
        long lStopMillis = getSafeTimeMillis();
        if (lStopMillis > lStartMillis)
            {
            m_cMissesMillis += (lStopMillis - lStartMillis);
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
        m_cCacheMisses += cMisses;
        if (lStartMillis > 0)
            {
            long lStopMillis = getSafeTimeMillis();
            if (lStopMillis > lStartMillis)
                {
                m_cMissesMillis += (lStopMillis - lStartMillis);
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
        m_cCachePuts++;
        if (lStartMillis > 0)
            {
            long lStopMillis = getSafeTimeMillis();
            if (lStopMillis > lStartMillis)
                {
                m_cPutsMillis += (lStopMillis - lStartMillis);
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
        m_cCachePuts += cPuts;
        if (lStartMillis > 0)
            {
            long lStopMillis = getSafeTimeMillis();
            if (lStopMillis > lStartMillis)
                {
                m_cPutsMillis += (lStopMillis - lStartMillis);
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
        m_cCachePrunes++;
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
                m_cCachePrunesMillis += (lStopMillis - lStartMillis);
                }
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The rough (ie unsynchronized) number of calls that could be answered
    * from the front or the back and were answered by data in the front map.
    */
    protected volatile long m_cCacheHits;

    /**
    * Total number of milliseconds used for get operations that were hits
    * since the last statistics reset.
    */
    protected volatile long m_cHitsMillis;

    /**
    * The rough (ie unsynchronized) number of calls that could be answered
    * from the front or the back and were answered by data in the back map.
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