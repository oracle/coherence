/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.continuous;

import com.tangosol.coherence.performance.benchmarks.daemonpool.common.BenchmarkProperties;

import com.tangosol.net.Coherence;
import com.tangosol.net.ContinuousAggregator;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.BigDecimalAverage;
import com.tangosol.util.aggregator.BigDecimalMax;
import com.tangosol.util.aggregator.BigDecimalMin;
import com.tangosol.util.aggregator.BigDecimalSum;
import com.tangosol.util.aggregator.ComparableMax;
import com.tangosol.util.aggregator.ComparableMin;
import com.tangosol.util.aggregator.CompositeAggregator;
import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.DistinctValues;
import com.tangosol.util.aggregator.DoubleAverage;
import com.tangosol.util.aggregator.DoubleMax;
import com.tangosol.util.aggregator.DoubleMin;
import com.tangosol.util.aggregator.DoubleSum;
import com.tangosol.util.aggregator.GroupAggregator;
import com.tangosol.util.aggregator.LongMax;
import com.tangosol.util.aggregator.LongMin;
import com.tangosol.util.aggregator.LongSum;
import com.tangosol.util.aggregator.ReducerAggregator;
import com.tangosol.util.aggregator.TopNAggregator;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.EqualsFilter;

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

import org.openjdk.jmh.infra.BenchmarkParams;

import java.io.Serializable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import java.util.concurrent.TimeUnit;

/**
 * Distributed benchmarks for continuous aggregation mutation, query, and
 * rebuild latency.
 * <p>
 * By default, the benchmark starts a single storage-enabled member for local
 * smoke testing. Set {@value #PROP_EXTERNAL_CLUSTER} to {@code true}, together
 * with {@code coherence.cluster} and {@code coherence.wka}, to run as a
 * storage-disabled peer against an external cluster over the network.
 *
 * @author Aleks Seovic  2026.09.11
 * @since 26.10
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ContinuousAggregationClusterBenchmark
    {
    @Benchmark
    public Bet putWithoutContinuousAggregation(ClusterState state)
        {
        return state.m_mapBaseline.put(0, state.nextBaselineValue());
        }

    @Benchmark
    public Bet putWithContinuousAggregation(ClusterState state)
        {
        return state.m_mapContinuous.put(0, state.nextContinuousValue());
        }

    @Benchmark
    public Object queryContinuousAggregation(ClusterState state)
        {
        return state.m_handle.aggregate();
        }

    @Benchmark
    public Object queryOnDemandAggregation(ClusterState state)
        {
        return state.aggregateOnDemand();
        }

    @Benchmark
    public Object registerBuildAndQuery(ClusterState state)
        {
        ContinuousAggregator<Integer, Bet, ?> aggregator =
                state.addAggregator(state.m_mapRebuild);
        try
            {
            return aggregator.aggregate();
            }
        finally
            {
            state.m_mapRebuild.removeAggregator(aggregator);
            }
        }

    // ----- enum: FilterMode ---------------------------------------------

    /**
     * Filter shapes exercised by the network benchmark.
     */
    public enum FilterMode
        {
        /**
         * Aggregate all entries through the no-filter API overload.
         */
        NONE,

        /**
         * Select the open entries, which comprise half of the initial data.
         */
        HALF;

        /**
         * Create the filter for this mode.
         *
         * @return the filter, or {@code null} for the no-filter overload
         */
        public Filter<?> create()
            {
            return this == NONE
                    ? null
                    : new EqualsFilter<Bet, Boolean>(OPEN_EXTRACTOR, Boolean.TRUE);
            }
        }

    // ----- enum: IndexMode ----------------------------------------------

    /**
     * Index shapes exercised by the entity-valued network benchmark.
     */
    public enum IndexMode
        {
        /**
         * Do not install indexes.
         */
        NONE,

        /**
         * Index the filter and aggregation extractors used by the workload.
         */
        EXTRACTORS;

        /**
         * Install this mode's indexes on the specified map.
         *
         * @param map             the map to configure
         * @param filterMode      the filter mode
         * @param aggregatorType  the aggregator type
         */
        public void configure(NamedMap<Integer, Bet> map, FilterMode filterMode,
                AggregatorType aggregatorType)
            {
            if (this == NONE)
                {
                return;
                }

            if (filterMode != FilterMode.NONE)
                {
                map.addIndex(OPEN_EXTRACTOR);
                }
            if (aggregatorType != AggregatorType.COUNT)
                {
                map.addIndex(AMOUNT_EXTRACTOR);
                }
            if (aggregatorType == AggregatorType.GROUP_LONG_SUM)
                {
                map.addIndex(BETTOR_ID_EXTRACTOR);
                }
            }
        }

    /**
     * Set to {@code true} to run as a storage-disabled peer against an
     * externally managed cluster.
     */
    public static final String PROP_EXTERNAL_CLUSTER =
            "coherence.continuousaggregation.benchmark.external";

    // ----- enum: AggregatorType -----------------------------------------

    /**
     * Continuously supported built-in aggregator variants exercised by the
     * network benchmark.
     */
    public enum AggregatorType
        {
        COUNT,
        LONG_SUM,
        DOUBLE_SUM,
        BIG_DECIMAL_SUM,
        DOUBLE_AVERAGE,
        BIG_DECIMAL_AVERAGE,
        LONG_MIN,
        LONG_MAX,
        DOUBLE_MIN,
        DOUBLE_MAX,
        BIG_DECIMAL_MIN,
        BIG_DECIMAL_MAX,
        COMPARABLE_MIN,
        COMPARABLE_MAX,
        DISTINCT_VALUES,
        REDUCER,
        TOP_N,
        GROUP_LONG_SUM,
        COMPOSITE;

        /**
         * Create a pristine aggregator definition for this variant.
         *
         * @return the aggregator definition
         */
        public InvocableMap.StreamingAggregator<? super Integer, ? super Bet, ?, ?> create()
            {
            switch (this)
                {
                case COUNT:
                    return new Count<Integer, Bet>();
                case LONG_SUM:
                    return new LongSum<Bet>(AMOUNT_EXTRACTOR);
                case DOUBLE_SUM:
                    return new DoubleSum<Bet>(AMOUNT_EXTRACTOR);
                case BIG_DECIMAL_SUM:
                    return new BigDecimalSum<Bet>(AMOUNT_EXTRACTOR);
                case DOUBLE_AVERAGE:
                    return new DoubleAverage<Bet>(AMOUNT_EXTRACTOR);
                case BIG_DECIMAL_AVERAGE:
                    return new BigDecimalAverage<Bet>(AMOUNT_EXTRACTOR);
                case LONG_MIN:
                    return new LongMin<Bet>(AMOUNT_EXTRACTOR);
                case LONG_MAX:
                    return new LongMax<Bet>(AMOUNT_EXTRACTOR);
                case DOUBLE_MIN:
                    return new DoubleMin<Bet>(AMOUNT_EXTRACTOR);
                case DOUBLE_MAX:
                    return new DoubleMax<Bet>(AMOUNT_EXTRACTOR);
                case BIG_DECIMAL_MIN:
                    return new BigDecimalMin<Bet>(AMOUNT_EXTRACTOR);
                case BIG_DECIMAL_MAX:
                    return new BigDecimalMax<Bet>(AMOUNT_EXTRACTOR);
                case COMPARABLE_MIN:
                    return new ComparableMin<Bet, Long>(AMOUNT_EXTRACTOR);
                case COMPARABLE_MAX:
                    return new ComparableMax<Bet, Long>(AMOUNT_EXTRACTOR);
                case DISTINCT_VALUES:
                    return new DistinctValues<Integer, Bet, Bet, Long>(AMOUNT_EXTRACTOR);
                case REDUCER:
                    return new ReducerAggregator<Integer, Bet, Bet, Long>(AMOUNT_EXTRACTOR);
                case TOP_N:
                    return new TopNAggregator<Integer, Bet, Bet, Long>(
                            AMOUNT_EXTRACTOR, null, 100);
                case GROUP_LONG_SUM:
                    return GroupAggregator
                            .<Integer, Bet, Bet, Long, Long>createInstance(
                                    BETTOR_ID_EXTRACTOR,
                                    new LongSum<Bet>(AMOUNT_EXTRACTOR), null);
                case COMPOSITE:
                    return new CompositeAggregator<Integer, Bet>(
                            new InvocableMap.EntryAggregator[]
                                {
                                new Count<Integer, Bet>(),
                                new LongSum<Bet>(AMOUNT_EXTRACTOR),
                                new LongMin<Bet>(AMOUNT_EXTRACTOR),
                                new LongMax<Bet>(AMOUNT_EXTRACTOR)
                                });
                default:
                    throw new IllegalStateException("unknown aggregator type " + this);
                }
            }
        }

    // ----- inner class: ClusterState ------------------------------------

    @State(Scope.Benchmark)
    public static class ClusterState
        {
        @Param
        public AggregatorType aggregatorType;

        @Param({"NONE"})
        public FilterMode filterMode;

        @Param({"NONE"})
        public IndexMode indexMode;

        @Param({"10000"})
        public int entryCount;

        @Param({"1024"})
        public int valueCardinality;

        @Setup(Level.Trial)
        public void setup(BenchmarkParams benchmarkParams)
            {
            if (entryCount < 0)
                {
                throw new IllegalArgumentException("entryCount must not be negative");
                }
            if (valueCardinality <= 0)
                {
                throw new IllegalArgumentException("valueCardinality must be positive");
                }
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
            setDefault("coherence.member", "ca-benchmark-client");
            setDefault("coherence.ttl", "0");

            if (Boolean.getBoolean(PROP_EXTERNAL_CLUSTER))
                {
                requireProperty("coherence.cluster");
                requireProperty("coherence.wka");
                System.setProperty("coherence.distributed.localstorage", "false");
                }
            else
                {
                System.setProperty("coherence.cluster",
                        "ca-cluster-benchmark-" + UUID.randomUUID());
                System.setProperty("coherence.distributed.localstorage", "true");
                System.setProperty("coherence.localhost", "127.0.0.1");
                System.setProperty("coherence.wka", "127.0.0.1");
                }

            m_coherence = Coherence.clusterMember().start().join();
            Session session = m_coherence.getSession();
            String sSuffix = aggregatorType.name().toLowerCase() + '-'
                    + filterMode.name().toLowerCase() + '-'
                    + indexMode.name().toLowerCase() + '-' + UUID.randomUUID();

            m_filter = filterMode.create();

            Map<Integer, Bet> entries = new HashMap<>(entryCount * 2);
            for (int i = 0; i < entryCount; i++)
                {
                entries.put(i, Bet.forEntry(i, valueCardinality));
                }

            String sBenchmark = benchmarkParams.getBenchmark();
            if (sBenchmark.endsWith(".putWithoutContinuousAggregation"))
                {
                m_mapBaseline = session.getMap("ca-benchmark-baseline-" + sSuffix);
                indexMode.configure(m_mapBaseline, filterMode, aggregatorType);
                m_mapBaseline.putAll(entries);
                }
            else if (sBenchmark.endsWith(".registerBuildAndQuery"))
                {
                m_mapRebuild = session.getMap("ca-benchmark-rebuild-" + sSuffix);
                indexMode.configure(m_mapRebuild, filterMode, aggregatorType);
                m_mapRebuild.putAll(entries);
                }
            else
                {
                m_mapContinuous = session.getMap("ca-benchmark-continuous-" + sSuffix);
                indexMode.configure(m_mapContinuous, filterMode, aggregatorType);
                m_mapContinuous.putAll(entries);
                m_handle = addAggregator(m_mapContinuous);
                }

            verifyContinuousResult();
            }

        @TearDown(Level.Iteration)
        public void verifyContinuousResult()
            {
            if (m_handle == null)
                {
                return;
                }

            Object expected = aggregateOnDemand();
            Object actual   = m_handle.aggregate();
            if (!Objects.deepEquals(expected, actual))
                {
                throw new IllegalStateException(filterMode + " " + aggregatorType
                        + " continuous result does not match on-demand result");
                }
            }

        private Object aggregateOnDemand()
            {
            InvocableMap.StreamingAggregator<? super Integer, ? super Bet, ?, ?> aggregator =
                    aggregatorType.create();
            return m_filter == null
                    ? m_mapContinuous.aggregate(aggregator)
                    : m_mapContinuous.aggregate(m_filter, aggregator);
            }

        private ContinuousAggregator<Integer, Bet, ?> addAggregator(
                NamedMap<Integer, Bet> map)
            {
            InvocableMap.StreamingAggregator<? super Integer, ? super Bet, ?, ?> aggregator =
                    aggregatorType.create();
            return m_filter == null
                    ? map.addAggregator(aggregator)
                    : map.addAggregator(m_filter, aggregator);
            }

        private Bet nextBaselineValue()
            {
            return nextMutationValue(++m_lBaselineValue);
            }

        private Bet nextContinuousValue()
            {
            return nextMutationValue(++m_lContinuousValue);
            }

        private Bet nextMutationValue(long sequence)
            {
            return Bet.forMutation(sequence, sequence % valueCardinality,
                    (sequence & 1L) == 0L);
            }

        @TearDown(Level.Trial)
        public void tearDown()
            {
            try
                {
                if (m_handle != null && m_mapContinuous != null)
                    {
                    m_mapContinuous.removeAggregator(m_handle);
                    }
                destroy(m_mapRebuild);
                destroy(m_mapContinuous);
                destroy(m_mapBaseline);
                }
            finally
                {
                Coherence.closeAll();
                BenchmarkProperties.restore(m_mapProperties);
                }
            }

        private void destroy(NamedMap<?, ?> map)
            {
            if (map != null && map.isActive())
                {
                map.destroy();
                }
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
                throw new IllegalStateException(sName
                        + " is required when " + PROP_EXTERNAL_CLUSTER + " is true");
                }
            }

        private Coherence m_coherence;
        private NamedMap<Integer, Bet> m_mapBaseline;
        private NamedMap<Integer, Bet> m_mapContinuous;
        private NamedMap<Integer, Bet> m_mapRebuild;
        private ContinuousAggregator<Integer, Bet, ?> m_handle;
        private Filter<?> m_filter;
        private Map<String, String> m_mapProperties;
        private long m_lBaselineValue;
        private long m_lContinuousValue;
        }

    // ----- inner class: Bet ---------------------------------------------

    /**
     * Entity value used to include realistic Java deserialization and
     * extractor costs in the distributed benchmark.
     */
    public static class Bet
            implements Serializable
        {
        /**
         * Construct a benchmark value.
         *
         * @param bettorId  the bettor identifier
         * @param amount    the bet amount
         * @param market    the market identifier
         * @param timestamp the event timestamp
         * @param odds      the decimal odds
         * @param open      whether the bet is open
         */
        public Bet(long bettorId, long amount, String market, long timestamp,
                double odds, boolean open)
            {
            m_bettorId = bettorId;
            m_amount = amount;
            m_market = market;
            m_timestamp = timestamp;
            m_odds = odds;
            m_open = open;
            }

        /**
         * Create a deterministic initial value.
         *
         * @param key               the entry key
         * @param valueCardinality  the configured cardinality
         *
         * @return the value
         */
        public static Bet forEntry(int key, int valueCardinality)
            {
            return new Bet(key % valueCardinality, key % valueCardinality,
                    "market-" + (key & 15), 1_700_000_000_000L + key,
                    1.01d + (key % 100) / 100.0d, (key & 1) == 0);
            }

        /**
         * Create a deterministic mutation value.
         *
         * @param sequence  the mutation sequence
         * @param amount    the amount
         * @param open      whether the bet is open
         *
         * @return the value
         */
        public static Bet forMutation(long sequence, long amount, boolean open)
            {
            return new Bet((sequence >>> 1) & 1L, amount, "market-0",
                    1_800_000_000_000L + sequence,
                    1.5d + (sequence & 1L) / 10.0d, open);
            }

        public Long getBettorId()
            {
            return m_bettorId;
            }

        public Long getAmount()
            {
            return m_amount;
            }

        public String getMarket()
            {
            return m_market;
            }

        public long getTimestamp()
            {
            return m_timestamp;
            }

        public double getOdds()
            {
            return m_odds;
            }

        public boolean isOpen()
            {
            return m_open;
            }

        private static final long serialVersionUID = 1L;

        private final Long m_bettorId;
        private final Long m_amount;
        private final String m_market;
        private final long m_timestamp;
        private final double m_odds;
        private final boolean m_open;
        }

    // ----- constants ----------------------------------------------------

    private static final ValueExtractor<Bet, Long> AMOUNT_EXTRACTOR =
            new ReflectionExtractor<>("getAmount");

    private static final ValueExtractor<Bet, Long> BETTOR_ID_EXTRACTOR =
            new ReflectionExtractor<>("getBettorId");

    private static final ValueExtractor<Bet, Boolean> OPEN_EXTRACTOR =
            new ReflectionExtractor<>("isOpen");
    }
