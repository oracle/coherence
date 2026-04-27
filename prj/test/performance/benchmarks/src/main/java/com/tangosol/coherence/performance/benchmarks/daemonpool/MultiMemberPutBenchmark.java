/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.daemonpool;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Backup write workload for daemon-pool multi-member measurements.
 *
 * @author Aleks Seovic  2026.04.25
 * @since 26.04
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgsAppend = "-Djdk.tracePinnedThreads=full")
@Threads(16)
public class MultiMemberPutBenchmark
    {
    @Benchmark
    public int putValue(BenchmarkState state)
        {
        Integer nPrevious = state.getCache().put(state.nextKey(), state.nextValue());
        return nPrevious == null ? -1 : nPrevious;
        }

    @Benchmark
    public int putAllValues(BenchmarkState state)
        {
        Map<Integer, Integer> mapBatch = state.nextPutAllBatch();
        state.getCache().putAll(mapBatch);
        return mapBatch.size();
        }

    @State(Scope.Benchmark)
    public static class BenchmarkState
            extends MultiMemberBenchmarkState
        {
        @Param({"64"})
        public int putAllBatchSize;

        @Setup(Level.Trial)
        public void setup()
                throws Exception
            {
            setupCluster(CACHE_NAME, false);
            }

        @TearDown(Level.Trial)
        public void tearDown()
                throws Exception
            {
            tearDownCluster();
            }

        Map<Integer, Integer> nextPutAllBatch()
            {
            int                   cBatch   = Math.max(1, putAllBatchSize);
            int                   cEntries = Math.max(1, cacheSize);
            int                   nStart   = ThreadLocalRandom.current().nextInt(cEntries);
            Map<Integer, Integer> mapBatch = new HashMap<>(cBatch);

            for (int i = 0; i < cBatch; i++)
                {
                mapBatch.put((nStart + i) % cEntries, nextValue());
                }

            return mapBatch;
            }
        }

    private static final String CACHE_NAME = "benchmark-backup";
    }
