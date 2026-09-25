/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.continuous;

import com.tangosol.coherence.performance.benchmarks.continuous.ContinuousAggregationClusterBenchmark.Bet;
import com.tangosol.coherence.performance.benchmarks.daemonpool.common.BenchmarkProperties;

import com.tangosol.net.Coherence;
import com.tangosol.net.ContinuousAggregator;
import com.tangosol.net.NamedMap;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.Session;

import com.tangosol.net.management.MBeanServerProxy;

import com.tangosol.net.partition.KeyPartitioningStrategy;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.LongMax;
import com.tangosol.util.aggregator.LongMin;

import com.tangosol.util.extractor.ReflectionExtractor;

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
import org.openjdk.jmh.annotations.Warmup;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Single-shot benchmark for continuous aggregation stale-bound queries.
 * <p>
 * Each measured invocation removes the sole partition-local extreme. A
 * dominated stale bound can be ignored because an exact partition result is
 * already better, while a required stale bound forces exactly one partition
 * scan. A different partition is invalidated for every required-bound trial
 * so asynchronous rebuilds cannot turn later trials into ready-state queries.
 *
 * @author Aleks Seovic  2026.09.12
 * @since 26.10
 */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 0)
@Measurement(iterations = 8)
@Fork(3)
public class ContinuousAggregationBoundBenchmark
    {
    /**
     * Query a maintained min or max after invalidating one partition bound.
     *
     * @param state  the benchmark state
     *
     * @return the exact aggregate result
     */
    @Benchmark
    public Object queryAfterInvalidation(BoundState state)
        {
        return state.record(state.m_handle.aggregate());
        }

    // ----- enum: BoundScenario ------------------------------------------

    /**
     * Stale-bound relationship to the remaining exact partition results.
     */
    public enum BoundScenario
        {
        /** The stale bound is dominated and does not require a scan. */
        DOMINATED,

        /** The stale bound can still win and requires a partition scan. */
        REQUIRED
        }

    // ----- enum: Extremum -----------------------------------------------

    /**
     * Bounded aggregator variants exercised by this benchmark.
     */
    public enum Extremum
        {
        MIN,
        MAX;

        /**
         * Create a pristine aggregator definition.
         *
         * @return the aggregator definition
         */
        public InvocableMap.StreamingAggregator<? super Integer, ? super Bet, ?, Long> create()
            {
            return this == MIN
                    ? new LongMin<Bet>(AMOUNT_EXTRACTOR)
                    : new LongMax<Bet>(AMOUNT_EXTRACTOR);
            }

        /**
         * Return the initial amount for an entry.
         */
        public long initialAmount(BoundScenario scenario, int nPartition,
                int cPartitions, boolean fCandidate)
            {
            if (!fCandidate)
                {
                return this == MIN
                        ? 1_000_000_000L + nPartition
                        : -1_000_000_000L - nPartition;
                }

            if (scenario == BoundScenario.REQUIRED)
                {
                return this == MIN ? nPartition : cPartitions - nPartition;
                }

            if (nPartition == 0)
                {
                return this == MIN ? 0L : 1_000_000_000L;
                }
            return this == MIN ? 10_000L + nPartition : 10_000L - nPartition;
            }

        /**
         * Return a value that cannot remain the partition extreme.
         */
        public long replacementAmount()
            {
            return this == MIN ? 2_000_000_000L : -2_000_000_000L;
            }
        }

    // ----- inner class: BoundState --------------------------------------

    /**
     * Distributed state for stale-bound single-shot trials.
     */
    @State(Scope.Benchmark)
    public static class BoundState
        {
        @Param
        public Extremum extremum;

        @Param
        public BoundScenario scenario;

        @Param({"1000000"})
        public int entryCount;

        @Setup(Level.Trial)
        public void setup()
            {
            m_mapProperties = BenchmarkProperties.capture(
                    "coherence.cacheconfig",
                    "coherence.cluster",
                    "coherence.distributed.localstorage",
                    "coherence.localhost",
                    "coherence.log.level",
                    "coherence.member",
                    "coherence.ttl",
                    "coherence.wka");

            setDefault("coherence.cacheconfig",
                    "continuous-aggregation-benchmark-cache-config.xml");
            setDefault("coherence.log.level", "2");
            setDefault("coherence.member", "ca-bound-benchmark-client");
            setDefault("coherence.ttl", "0");

            if (Boolean.getBoolean(
                    ContinuousAggregationClusterBenchmark.PROP_EXTERNAL_CLUSTER))
                {
                requireProperty("coherence.cluster");
                requireProperty("coherence.wka");
                System.setProperty("coherence.distributed.localstorage", "false");
                }
            else
                {
                System.setProperty("coherence.cluster",
                        "ca-bound-benchmark-" + UUID.randomUUID());
                System.setProperty("coherence.distributed.localstorage", "true");
                System.setProperty("coherence.localhost", "127.0.0.1");
                System.setProperty("coherence.wka", "127.0.0.1");
                }

            m_coherence = Coherence.clusterMember().start().join();
            Session session = m_coherence.getSession();
            m_map = session.getMap("ca-benchmark-bound-" + extremum.name().toLowerCase()
                    + '-' + scenario.name().toLowerCase() + '-' + UUID.randomUUID());
            m_map.addIndex(AMOUNT_EXTRACTOR);

            PartitionedService service = (PartitionedService) m_map.getService();
            int cPartitions = service.getPartitionCount();
            m_anCandidateKeys = findCandidateKeys(service, cPartitions);
            int nMaximumCandidate = Arrays.stream(m_anCandidateKeys).max().orElse(-1);
            if (entryCount <= nMaximumCandidate)
                {
                throw new IllegalArgumentException("entryCount must exceed "
                        + nMaximumCandidate + " to cover every partition");
                }

            KeyPartitioningStrategy strategy = service.getKeyPartitioningStrategy();
            Map<Integer, Bet> entries = new HashMap<>(entryCount * 2);
            for (int nKey = 0; nKey < entryCount; nKey++)
                {
                int nPartition = strategy.getKeyPartition(nKey);
                boolean fCandidate = m_anCandidateKeys[nPartition] == nKey;
                long nAmount = extremum.initialAmount(scenario, nPartition,
                        cPartitions, fCandidate);
                entries.put(nKey, value(nKey, nAmount));
                }
            m_map.putAll(entries);
            m_handle = m_map.addAggregator(extremum.create());

            Object expected = m_map.aggregate(extremum.create());
            if (!Objects.deepEquals(expected, m_handle.aggregate()))
                {
                throw new IllegalStateException("initial continuous result mismatch");
                }

            m_nNextPartition = scenario == BoundScenario.DOMINATED ? 1 : 0;
            }

        @Setup(Level.Invocation)
        public void invalidateBound()
            {
            if (m_nNextPartition >= m_anCandidateKeys.length)
                {
                throw new IllegalStateException("exhausted candidate partitions");
                }
            int nPartition = m_nNextPartition++;
            int nKey = m_anCandidateKeys[nPartition];
            m_map.put(nKey, value(nKey, extremum.replacementAmount()));
            m_lFallbackBefore = fallbackCount();
            }

        @TearDown(Level.Invocation)
        public void verifyResult()
            {
            Object expected = m_map.aggregate(extremum.create());
            if (!Objects.deepEquals(expected, m_oLastResult))
                {
                throw new IllegalStateException(scenario + " " + extremum
                        + " stale-bound result does not match on-demand result");
                }

            long cExpected  = scenario == BoundScenario.REQUIRED ? 1L : 0L;
            long cFallbacks = awaitFallbackCount(cExpected);
            if (cFallbacks != cExpected)
                {
                throw new IllegalStateException(scenario + " " + extremum
                        + " expected " + cExpected + " fallback scans but observed "
                        + cFallbacks + "; StorageManager metrics="
                        + continuousAggregationMetrics());
                }
            }

        /**
         * Wait for the remote management snapshot to expose the fallback
         * counter changed by the measured query.
         * <p>
         * The storage operation and result are synchronous, but a
         * storage-disabled client can briefly observe the previous remote
         * MBean snapshot. Waiting is deliberately performed from invocation
         * tear-down, outside the single-shot query measurement.
         */
        private long awaitFallbackCount(long cExpected)
            {
            long lSettle   = System.nanoTime() + METRIC_SETTLE_NANOS;
            long lDeadline = System.nanoTime() + METRIC_TIMEOUT_NANOS;
            long cFallbacks;

            do
                {
                cFallbacks = fallbackCount() - m_lFallbackBefore;
                long lNow = System.nanoTime();
                if (lNow >= lSettle && (cExpected == 0L || cFallbacks >= cExpected))
                    {
                    return cFallbacks;
                    }
                LockSupport.parkNanos(METRIC_POLL_NANOS);
                }
            while (System.nanoTime() < lDeadline);

            return fallbackCount() - m_lFallbackBefore;
            }

        @TearDown(Level.Trial)
        public void tearDown()
            {
            try
                {
                if (m_handle != null && m_map != null)
                    {
                    m_map.removeAggregator(m_handle);
                    }
                if (m_map != null && m_map.isActive())
                    {
                    m_map.destroy();
                    }
                }
            finally
                {
                Coherence.closeAll();
                BenchmarkProperties.restore(m_mapProperties);
                }
            }

        private Object record(Object result)
            {
            m_oLastResult = result;
            return result;
            }

        private long fallbackCount()
            {
            MBeanServerProxy proxy = m_coherence.getManagement().getMBeanServerProxy();
            Set<String> setNames = proxy.queryNames(
                    "Coherence:type=StorageManager,cache=" + m_map.getName() + ",*", null);
            if (setNames.isEmpty())
                {
                throw new IllegalStateException("StorageManager MBeans are not available for "
                        + m_map.getName());
                }

            long cFallbacks = 0L;
            for (String sName : setNames)
                {
                cFallbacks += ((Number) proxy.getAttribute(sName,
                        "ContinuousAggregationFallbackCount")).longValue();
                }
            return cFallbacks;
            }

        private Map<String, Object> continuousAggregationMetrics()
            {
            MBeanServerProxy proxy = m_coherence.getManagement().getMBeanServerProxy();
            Set<String> setNames = proxy.queryNames(
                    "Coherence:type=StorageManager,cache=" + m_map.getName() + ",*", null);
            Map<String, Object> mapMetrics = new HashMap<>();
            String[] asAttributes =
                {
                "ContinuousAggregationRegistrationCount",
                "ContinuousAggregationReadyPartitionCount",
                "ContinuousAggregationBuildingPartitionCount",
                "ContinuousAggregationStalePartitionCount",
                "ContinuousAggregationMutationCount",
                "ContinuousAggregationFallbackCount",
                "ContinuousAggregationRebuildCount",
                "ContinuousAggregationDirtyCount"
                };
            for (String sName : setNames)
                {
                for (String sAttribute : asAttributes)
                    {
                    mapMetrics.put(sName + ':' + sAttribute,
                            proxy.getAttribute(sName, sAttribute));
                    }
                }
            return mapMetrics;
            }

        private static int[] findCandidateKeys(PartitionedService service,
                int cPartitions)
            {
            int[] anKeys = new int[cPartitions];
            Arrays.fill(anKeys, -1);
            KeyPartitioningStrategy strategy = service.getKeyPartitioningStrategy();
            int cFound = 0;
            for (int nKey = 0; cFound < cPartitions; nKey++)
                {
                int nPartition = strategy.getKeyPartition(nKey);
                if (anKeys[nPartition] < 0)
                    {
                    anKeys[nPartition] = nKey;
                    cFound++;
                    }
                }
            return anKeys;
            }

        private static Bet value(int nKey, long nAmount)
            {
            return new Bet(nKey & 1023, nAmount, "market-" + (nKey & 15),
                    1_700_000_000_000L + nKey,
                    1.01d + (nKey % 100) / 100.0d, true);
            }

        private static void setDefault(String sName, String sValue)
            {
            if (System.getProperty(sName) == null)
                {
                System.setProperty(sName, sValue);
                }
            }

        private static void requireProperty(String sName)
            {
            String sValue = System.getProperty(sName);
            if (sValue == null || sValue.isBlank())
                {
                throw new IllegalStateException(sName + " is required for an external cluster");
                }
            }

        private Coherence m_coherence;
        private NamedMap<Integer, Bet> m_map;
        private ContinuousAggregator<Integer, Bet, Long> m_handle;
        private int[] m_anCandidateKeys;
        private int m_nNextPartition;
        private long m_lFallbackBefore;
        private Object m_oLastResult;
        private Map<String, String> m_mapProperties;
        }

    // ----- constants ----------------------------------------------------

    private static final ValueExtractor<Bet, Long> AMOUNT_EXTRACTOR =
            new ReflectionExtractor<>("getAmount");

    /** Minimum delay before trusting a remote management snapshot. */
    private static final long METRIC_SETTLE_NANOS = TimeUnit.SECONDS.toNanos(2L);

    /** Maximum delay for a required fallback to become visible remotely. */
    private static final long METRIC_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5L);

    /**
     * Remote management counter polling interval. RemoteModel retains a newly
     * fetched snapshot while it remains active (128 ms by default), so this
     * leaves enough idle time for that snapshot to be promoted.
     */
    private static final long METRIC_POLL_NANOS = TimeUnit.MILLISECONDS.toNanos(250L);
    }
