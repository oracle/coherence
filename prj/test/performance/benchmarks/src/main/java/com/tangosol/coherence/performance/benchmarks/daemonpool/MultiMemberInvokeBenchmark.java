/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.daemonpool;

import com.tangosol.coherence.performance.benchmarks.daemonpool.common.NoOpProcessor;
import com.tangosol.coherence.performance.benchmarks.daemonpool.common.SleepingProcessor;

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

import java.util.concurrent.TimeUnit;

/**
 * Invoke workloads for daemon-pool multi-member measurements.
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
public class MultiMemberInvokeBenchmark
    {
    @Benchmark
    public int invokeNoOpProcessor(BenchmarkState state)
        {
        return state.getCache().invoke(state.nextKey(), NoOpProcessor.INSTANCE);
        }

    @Benchmark
    public int invokeSleepingProcessor(BenchmarkState state)
        {
        return state.getCache().invoke(state.nextKey(), state.getSleepingProcessor());
        }

    @State(Scope.Benchmark)
    public static class BenchmarkState
            extends MultiMemberBenchmarkState
        {
        @Param({"100"})
        public int processorSleepMicros;

        @Setup(Level.Trial)
        public void setup()
                throws Exception
            {
            m_processorSleep = new SleepingProcessor(processorSleepMicros);
            setupCluster(CACHE_NAME, false);
            }

        @TearDown(Level.Trial)
        public void tearDown()
                throws Exception
            {
            tearDownCluster();
            }

        SleepingProcessor getSleepingProcessor()
            {
            return m_processorSleep;
            }

        private SleepingProcessor m_processorSleep;
        }

    private static final String CACHE_NAME = "benchmark-backup";
    }
