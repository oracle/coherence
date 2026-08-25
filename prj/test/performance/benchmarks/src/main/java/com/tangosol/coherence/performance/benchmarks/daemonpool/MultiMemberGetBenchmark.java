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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Read workloads for Phase 4 daemon-pool multi-member measurements.
 *
 * @author Aleks Seovic  2026.04.30
 * @since 26.04
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgsAppend = "-Djdk.tracePinnedThreads=full")
@Threads(16)
public class MultiMemberGetBenchmark
    {
    @Benchmark
    public int getValue(BenchmarkState state)
        {
        Integer nValue = state.getCache().get(state.nextGetKey());
        return nValue == null ? -1 : nValue;
        }

    @Benchmark
    public int getAllValues(BenchmarkState state)
        {
        Map<Integer, Integer> mapResult = state.getCache().getAll(state.nextGetAllKeys());
        return mapResult.size();
        }

    @State(Scope.Benchmark)
    public static class BenchmarkState
            extends MultiMemberBenchmarkState
        {
        @Param({"plain"})
        public String cacheShape;

        @Param({"16"})
        public int batchSize;

        @Setup(Level.Trial)
        public void setup()
                throws Exception
            {
            setupCluster(cacheName(), false);
            }

        @TearDown(Level.Trial)
        public void tearDown()
                throws Exception
            {
            tearDownCluster();
            }

        int nextGetKey()
            {
            if ("SINGLE_HOT".equalsIgnoreCase(keyDistribution))
                {
                return 0;
                }

            return nextKey();
            }

        Collection<Integer> nextGetAllKeys()
            {
            int cBatch = Math.max(1, batchSize);
            if ("HOT_SUBSET".equalsIgnoreCase(keyDistribution))
                {
                Collection<Integer> colKeys = new ArrayList<>(cBatch);
                for (int i = 0; i < cBatch; i++)
                    {
                    colKeys.add(ThreadLocalRandom.current().nextInt(HOT_SUBSET_SIZE));
                    }
                return colKeys;
                }

            Set<Integer> setKeys  = new LinkedHashSet<>(cBatch);
            int          cEntries = Math.max(1, cacheSize);
            while (setKeys.size() < cBatch)
                {
                setKeys.add(ThreadLocalRandom.current().nextInt(cEntries));
                }
            return setKeys;
            }

        private String cacheName()
            {
            if ("rwbm".equalsIgnoreCase(cacheShape))
                {
                return CACHE_NAME_RWBM;
                }
            if ("sliding".equalsIgnoreCase(cacheShape)
                    || "sliding-expiry".equalsIgnoreCase(cacheShape))
                {
                return CACHE_NAME_SLIDING;
                }
            return CACHE_NAME_PLAIN;
            }

        private static final int HOT_SUBSET_SIZE = 16;
        }

    private static final String CACHE_NAME_PLAIN   = "benchmark-get-plain";
    private static final String CACHE_NAME_RWBM    = "benchmark-get-rwbm";
    private static final String CACHE_NAME_SLIDING = "benchmark-get-sliding";
    }
