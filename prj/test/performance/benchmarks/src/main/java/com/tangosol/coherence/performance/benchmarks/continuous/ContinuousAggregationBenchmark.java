/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.continuous;

import com.tangosol.internal.net.ContinuousAggregationDefinition;
import com.tangosol.internal.net.ContinuousAggregationQuery;
import com.tangosol.internal.net.ContinuousAggregationState;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.SimpleStreamer;

import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.LongSum;

import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.GreaterFilter;

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

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.TimeUnit;

/**
 * Microbenchmarks for the local mechanics of continuous aggregation.
 *
 * @author Aleks Seovic  2026.09.11
 * @since 26.10
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class ContinuousAggregationBenchmark
    {
    // ----- maintenance benchmarks ---------------------------------------

    @Benchmark
    public ContinuousAggregationState.Status maintainCount(MaintenanceState state)
        {
        return state.m_stateCount.apply(state.m_definitionCount, state.nextCountEntry());
        }

    @Benchmark
    public ContinuousAggregationState.Status maintainLongSum(MaintenanceState state)
        {
        return state.m_stateSum.apply(state.m_definitionSum, state.nextSumEntry());
        }

    @Benchmark
    public ContinuousAggregationState.Status maintainFilteredLongSum(MaintenanceState state)
        {
        return state.m_stateFiltered.apply(state.m_definitionFiltered,
                state.nextFilteredEntry());
        }

    @Benchmark
    public InvocableMap.StreamingAggregator.RetractionResult updateCount(
            MaintenanceState state)
        {
        return state.m_countUpdate.update(state.nextCountUpdateEntry());
        }

    @Benchmark
    public InvocableMap.StreamingAggregator.RetractionResult retractAndAccumulateCount(
            MaintenanceState state)
        {
        SimpleMapEntry<Integer, Long> entry = state.nextCountLegacyEntry();
        InvocableMap.StreamingAggregator.RetractionResult result =
                state.m_countLegacy.retract(entry);
        state.m_countLegacy.accumulate(entry);
        return result;
        }

    @Benchmark
    public InvocableMap.StreamingAggregator.RetractionResult updateLongSum(
            MaintenanceState state)
        {
        return state.m_sumUpdate.update(state.nextSumUpdateEntry());
        }

    @Benchmark
    public InvocableMap.StreamingAggregator.RetractionResult retractAndAccumulateLongSum(
            MaintenanceState state)
        {
        SimpleMapEntry<Integer, Long> entry = state.nextSumLegacyEntry();
        InvocableMap.StreamingAggregator.RetractionResult result =
                state.m_sumLegacy.retract(entry);
        state.m_sumLegacy.accumulate(entry);
        return result;
        }

    // ----- rebuild and query-combination benchmarks ---------------------

    @Benchmark
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Long rebuildLongSum(RebuildState state)
        {
        InvocableMap.StreamingAggregator aggregator = state.m_definition.getAggregator().supply();
        aggregator.accumulate(new SimpleStreamer(state.m_entries));
        return (Long) aggregator.getPartialResult();
        }

    @Benchmark
    public Long combineMemberPartials(QueryState state)
        {
        ContinuousAggregationQuery<Long> query = new ContinuousAggregationQuery<>(
                new LongSum<>(IdentityExtractor.INSTANCE()));
        for (Long partial : state.m_partials)
            {
            query.combine(partial);
            }
        return query.finalizeResult();
        }

    // ----- inner class: MaintenanceState --------------------------------

    @State(Scope.Thread)
    public static class MaintenanceState
        {
        @Setup(Level.Trial)
        public void setup()
            {
            Count<Integer, Long> count = new Count<>();
            count.accumulate(new SimpleMapEntry<>(1, 1L));
            m_stateCount.completeBuild(count);

            LongSum<Number> sum = new LongSum<>(IdentityExtractor.INSTANCE());
            sum.accumulate(new SimpleMapEntry<>(1, (Number) 1L));
            m_stateSum.completeBuild(sum);

            LongSum<Number> filtered = new LongSum<>(IdentityExtractor.INSTANCE());
            filtered.accumulate(new SimpleMapEntry<>(1, (Number) 1L));
            m_stateFiltered.completeBuild(filtered);

            m_countUpdate.accumulate(new SimpleMapEntry<>(1, 1L));
            m_countLegacy.accumulate(new SimpleMapEntry<>(1, 1L));
            m_sumUpdate.accumulate(new SimpleMapEntry<>(1, 1L));
            m_sumLegacy.accumulate(new SimpleMapEntry<>(1, 1L));
            }

        @TearDown(Level.Iteration)
        public void verify()
            {
            if (!Integer.valueOf(1).equals(m_stateCount.getPartialResult()))
                {
                throw new IllegalStateException("count maintenance benchmark lost an entry");
                }
            if (!Integer.valueOf(1).equals(m_countUpdate.getPartialResult())
                    || !Integer.valueOf(1).equals(m_countLegacy.getPartialResult()))
                {
                throw new IllegalStateException("count update benchmark lost an entry");
                }
            verifySum(m_stateSum.getPartialResult());
            verifySum(m_stateFiltered.getPartialResult());
            verifySum(m_sumUpdate.getPartialResult());
            verifySum(m_sumLegacy.getPartialResult());
            }

        private void verifySum(Object result)
            {
            if (!Long.valueOf(1L).equals(result) && !Long.valueOf(2L).equals(result))
                {
                throw new IllegalStateException("sum maintenance benchmark produced " + result);
                }
            }

        private SimpleMapEntry<Integer, Long> nextCountEntry()
            {
            return m_aEntry[m_iCount++ & 1];
            }

        private SimpleMapEntry<Integer, Long> nextSumEntry()
            {
            return m_aEntry[m_iSum++ & 1];
            }

        private SimpleMapEntry<Integer, Long> nextFilteredEntry()
            {
            return m_aEntry[m_iFiltered++ & 1];
            }

        private SimpleMapEntry<Integer, Long> nextCountUpdateEntry()
            {
            return m_aEntry[m_iCountUpdate++ & 1];
            }

        private SimpleMapEntry<Integer, Long> nextCountLegacyEntry()
            {
            return m_aEntry[m_iCountLegacy++ & 1];
            }

        private SimpleMapEntry<Integer, Long> nextSumUpdateEntry()
            {
            return m_aEntry[m_iSumUpdate++ & 1];
            }

        private SimpleMapEntry<Integer, Long> nextSumLegacyEntry()
            {
            return m_aEntry[m_iSumLegacy++ & 1];
            }

        private final ContinuousAggregationDefinition m_definitionCount =
                new ContinuousAggregationDefinition(null, new Count<>());

        private final ContinuousAggregationDefinition m_definitionSum =
                new ContinuousAggregationDefinition(null,
                        new LongSum<>(IdentityExtractor.INSTANCE()));

        private final ContinuousAggregationDefinition m_definitionFiltered =
                new ContinuousAggregationDefinition(
                        new GreaterFilter<>(IdentityExtractor.INSTANCE(), 0L),
                        new LongSum<>(IdentityExtractor.INSTANCE()));

        private final ContinuousAggregationState m_stateCount = new ContinuousAggregationState();
        private final ContinuousAggregationState m_stateSum = new ContinuousAggregationState();
        private final ContinuousAggregationState m_stateFiltered = new ContinuousAggregationState();

        private final Count<Integer, Long> m_countUpdate = new Count<>();
        private final Count<Integer, Long> m_countLegacy = new Count<>();
        private final LongSum<Number> m_sumUpdate = new LongSum<>(IdentityExtractor.INSTANCE());
        private final LongSum<Number> m_sumLegacy = new LongSum<>(IdentityExtractor.INSTANCE());

        private final SimpleMapEntry<Integer, Long>[] m_aEntry = new SimpleMapEntry[]
            {
            new SimpleMapEntry<>(1, 2L, 1L),
            new SimpleMapEntry<>(1, 1L, 2L)
            };

        private int m_iCount;
        private int m_iCountUpdate;
        private int m_iCountLegacy;
        private int m_iSum;
        private int m_iSumUpdate;
        private int m_iSumLegacy;
        private int m_iFiltered;
        }

    // ----- inner class: RebuildState ------------------------------------

    @State(Scope.Benchmark)
    public static class RebuildState
        {
        @Param({"1000", "10000"})
        public int entryCount;

        @Setup(Level.Trial)
        public void setup()
            {
            m_entries = new ArrayList<>(entryCount);
            for (int i = 0; i < entryCount; i++)
                {
                m_entries.add(new SimpleMapEntry<>(i, (long) i));
                }
            m_definition = new ContinuousAggregationDefinition(null,
                    new LongSum<>(IdentityExtractor.INSTANCE()));
            }

        private ContinuousAggregationDefinition m_definition;
        private List<InvocableMap.Entry<Integer, Long>> m_entries;
        }

    // ----- inner class: QueryState --------------------------------------

    @State(Scope.Benchmark)
    public static class QueryState
        {
        @Param({"17", "257"})
        public int partitionCount;

        @Setup(Level.Trial)
        public void setup()
            {
            m_partials = new Long[partitionCount];
            for (int i = 0; i < partitionCount; i++)
                {
                m_partials[i] = Long.valueOf(i + 1L);
                }
            }

        private Long[] m_partials;
        }
    }
