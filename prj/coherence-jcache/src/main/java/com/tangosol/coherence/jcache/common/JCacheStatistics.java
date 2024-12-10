/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.common;

import javax.cache.management.CacheStatisticsMXBean;

/**
 * Interface for JCache Statistics.
 *
 * JCache cache statistics differed enough from current Coherence cache statistics that
 * just ended up maintaining statistics separately.
 *
 * Examples of differences include put of same value is optimized in Coherence implementation
 * but JCache considers them 2 distinct puts. (could not pass jsr 107 tck with that behavior)
 * Additionally, coherence does not count removals at the time this was written.
 *
 * @author jf  2014.1.29
 * @since Coherence 12.1.3
 */
public interface JCacheStatistics
        extends CacheStatisticsMXBean
    {
    /**
     * add Cache Hits of <tt>count</tt> and compute time in cache hits
     *
     * @param count  number of cache entry lookup hits
     * @param lStartMillis start time in milliseconds for computing time performing
     *                     lookup when there were hits.
     */
    void registerHits(int count, long lStartMillis);

    /**
     * add Cache Misses of <tt>count</tt> and compute time in cache misses
     *
     * @param count number of cache entry lookup misses
     * @param lStartMillis start time of cache entry lookup that resulted in misses
     */
    void registerMisses(int count, long lStartMillis);

    /**
     * add Cache Puts of <tt>count</tt> and compute time in cache puts
     *
     * @param count number of cache entry puts
     * @param lStartMillis start time in milliseconds of put(s) operation
     */
    void registerPuts(long count, long lStartMillis);

    /**
     * Record elapsed time performing puts
     *
     * @param lStartMillis start time in milliseconds of put(s) operation
     */
    void registerPutsCompleted(long lStartMillis);

    /**
     * add Cache Removals of <tt>count</tt> and compute time in cache removals
     *
     * @param count number of cache entry removals
     * @param lStartMillis start time in milliseconds of removal(s) operation
     */
    void registerRemoves(long count, long lStartMillis);

    /**
     * register a Cache Remove
     */
    void registerRemove();

    /**
     * Record elapsed time in milliseconds performing hit(s)
     *
     * @param lStartMillis start time in milliseconds of operation that resulted in a hit(s)
     */
    void registerHitsCompleted(long lStartMillis);

    /**
     * Record elapsed time in milliseconds performing miss(es)
     *
     * @param lStartMillis start time in milliseconds of operation that resulted in miss(es)
     */
    void registerMissesCompleted(long lStartMillis);

    /**
     * Record elapsed time in milliseconds performing removal(s)
     *
     * @param lStartMillis start time in milliseconds of operation that resulted in removal(s)
     */
    void registerRemoveCompleted(long lStartMillis);

    /**
     * Get unique JCacheIdentifier for cache that these statistics are for.
     *
     * @return unique JCacheIdentifier
     */
    JCacheIdentifier getIdentifier();

    /**
     * add {@link JCacheStatistics} <tt>stats</tt> to this instance.
     * @param stats {@link JCacheStatistics} from another data-enabled server
     * @return the addition of <tt>stats</tt> to this instance
     */
    JCacheStatistics add(JCacheStatistics stats);

    /**
     * get time elapsed in milliseconds performing operations resulting in a hit
     *
     * @return duration of operations that resulted in cache entry hit
     */
    long getCacheHitsMillis();

    /**
     * get time elapsed in milliseconds performing operations resulting in a miss
     *
     * @return duration of operations that resulted in cache entry miss
     */
    long getCacheMissesMillis();

    /**
     * get time elapsed in milliseconds performing operations resulting in a put
     *
     * @return duration of operations that resulted in cache entry put
     */
    long getCachePutsMillis();

    /**
     * get time elapsed in milliseconds performing operations resulting in a remove
     *
     * @return duration of operations that resulted in cache entry remove
     */
    long getCacheRemoveMillis();

    // ----- CacheStatisticsMXBean interface --------------------------------

    @Override
    void clear();

    @Override
    long getCacheHits();

    @Override
    float getCacheHitPercentage();

    @Override
    long getCacheMisses();

    @Override
    float getCacheMissPercentage();

    @Override
    long getCacheGets();

    @Override
    long getCachePuts();

    @Override
    long getCacheRemovals();

    @Override
    long getCacheEvictions();

    @Override
    float getAverageGetTime();

    @Override
    float getAveragePutTime();

    @Override
    float getAverageRemoveTime();
    }
