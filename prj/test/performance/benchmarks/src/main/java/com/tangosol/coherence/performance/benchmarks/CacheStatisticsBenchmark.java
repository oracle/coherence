/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks;

import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.SimpleCacheStatistics;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Compare atomic cache statistics with the former unsynchronized increments.
 * The legacy implementation is a performance baseline and can lose updates.
 *
 * @author Aleks Seovic  2026.11.06
 * @since 27.01
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class CacheStatisticsBenchmark
    {
    @Param({"legacy", "atomic"})
    public String implementation;

    @Setup
    public void setup()
        {
        stats = implementation.equals("legacy") ? new LegacyStatistics() : new SimpleCacheStatistics();
        cache = new BenchmarkCache(stats);
        cache.put(1, 1);
        }

    @Benchmark
    public void registerHit()
        {
        stats.registerHit();
        }

    @Benchmark
    public Object localCacheGet()
        {
        return cache.get(1);
        }

    /**
     * Install the selected counter implementation in the cache under test.
     */
    private static class BenchmarkCache
            extends LocalCache
        {
        private BenchmarkCache(SimpleCacheStatistics stats)
            {
            m_stats = stats;
            }
        }

    /**
     * Reproduce the former hit registration for performance comparisons.
     */
    private static class LegacyStatistics
            extends SimpleCacheStatistics
        {
        @Override
        public void registerHit()
            {
            ++m_cCacheHits;
            }
        }

    private SimpleCacheStatistics stats;
    private LocalCache cache;
    }
