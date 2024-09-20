/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


/**
* An interface for exposing Cache statistics.
*
* @since Coherence 2.2
* @author cp  2003.05.26
*/
public interface CacheStatistics
    {
    /**
    * Determine the total number of <tt>get()</tt> operations since the cache
    * statistics were last reset.
    *
    * @return the total number of <tt>get()</tt> operations
    */
    public long getTotalGets();

    /**
    * Determine the total number of milliseconds spent on <tt>get()</tt>
    * operations since the cache statistics were last reset.
    *
    * @return the total number of milliseconds processing <tt>get()</tt>
    *         operations
    */
    public long getTotalGetsMillis();

    /**
    * Determine the average number of milliseconds per <tt>get()</tt>
    * invocation since the cache statistics were last reset.
    *
    * @return the average number of milliseconds per <tt>get()</tt> operation
    */
    public double getAverageGetMillis();

    /**
    * Determine the total number of <tt>put()</tt> operations since the cache
    * statistics were last reset.
    *
    * @return the total number of <tt>put()</tt> operations
    */
    public long getTotalPuts();

    /**
    * Determine the total number of milliseconds spent on <tt>put()</tt>
    * operations since the cache statistics were last reset.
    *
    * @return the total number of milliseconds processing <tt>put()</tt>
    *         operations
    */
    public long getTotalPutsMillis();

    /**
    * Determine the average number of milliseconds per <tt>put()</tt>
    * invocation since the cache statistics were last reset.
    *
    * @return the average number of milliseconds per <tt>put()</tt> operation
    */
    public double getAveragePutMillis();

    /**
    * Determine the rough number of cache hits since the cache statistics
    * were last reset.
    * <p>
    * A cache hit is a read operation invocation (e.g. <tt>get()</tt>) for
    * which an entry exists in this map.
    *
    * @return the number of <tt>get()</tt> calls that have been served by
    *         existing cache entries
    */
    public long getCacheHits();

    /**
    * Determine the total number of milliseconds (since the last statistics
    * reset) for the <tt>get()</tt> operations for which an entry existed in
    * this map.
    *
    * @return the total number of milliseconds for the <tt>get()</tt>
    *         operations that were hits
    */
    public long getCacheHitsMillis();

    /**
    * Determine the average number of milliseconds per <tt>get()</tt>
    * invocation that is a hit.
    *
    * @return the average number of milliseconds per cache hit
    */
    public double getAverageHitMillis();

    /**
    * Determine the rough number of cache misses since the cache statistics
    * were last reset.
    * <p>
    * A cache miss is a <tt>get()</tt> invocation that does not have an entry
    * in this map.
    *
    * @return the number of <tt>get()</tt> calls that failed to find an
    *         existing cache entry because the requested key was not in the
    *         cache
    */
    public long getCacheMisses();

    /**
    * Determine the total number of milliseconds (since the last statistics
    * reset) for the <tt>get()</tt> operations for which no entry existed in
    * this map.
    *
    * @return the total number of milliseconds (since the last statistics
    *         reset) for the <tt>get()</tt> operations that were misses
    */
    public long getCacheMissesMillis();

    /**
    * Determine the average number of milliseconds per <tt>get()</tt>
    * invocation that is a miss.
    *
    * @return the average number of milliseconds per cache miss
    */
    public double getAverageMissMillis();

    /**
    * Determine the rough probability (0 &lt;= p &lt;= 1) that the next
    * invocation will be a hit, based on the statistics collected since the
    * last reset of the cache statistics.
    *
    * @return the cache hit probability (0 &lt;= p &lt;= 1)
    */
    public double getHitProbability();

    /**
    * Determine the rough number of cache pruning cycles since the cache
    * statistics were last reset.
    * <p>
    * For the LocalCache implementation, this refers to the number of times
    * that the <tt>prune()</tt> method is executed.
    *
    * @return the total number of cache pruning cycles (since the last
    *         statistics reset)
    */
    public long getCachePrunes();

    /**
    * Determine the total number of milliseconds (since the last statistics
    * reset) spent on cache pruning.
    * <p>
    * For the LocalCache implementation, this refers to the time spent in
    * the <tt>prune()</tt> method.
    *
    * @return the total number of milliseconds (since the last statistics
    *         reset) for cache pruning operations
    */
    public long getCachePrunesMillis();

    /**
    * Reset all of the cache statistics.
    * <p>
    * Note that the method name implies that only the hit statistics are
    * cleared, which is not the case; all of the statistics are cleared.
    */
    public void resetHitStatistics();
    }
