/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.SimpleMapEntry;

import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.DoubleSum;
import com.tangosol.util.aggregator.LongMin;

import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.GreaterFilter;

import org.junit.Test;

import static com.tangosol.internal.net.ContinuousAggregationState.Status.DIRTY;
import static com.tangosol.internal.net.ContinuousAggregationState.Status.BUILDING;
import static com.tangosol.internal.net.ContinuousAggregationState.Status.READY;
import static com.tangosol.internal.net.ContinuousAggregationState.Status.STALE_BOUND;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ContinuousAggregationState}.
 *
 * @author Aleks Seovic  2026.09.06
 * @since 26.10
 */
public class ContinuousAggregationStateTest
    {
    @Test
    public void shouldMaintainFilterTransitionsUsingOriginalValue()
        {
        Count<Integer, Long> count = new Count<>();
        count.accumulate(entry(1, 20L, null));

        ContinuousAggregationState state = new ContinuousAggregationState();
        state.completeBuild(count);

        ContinuousAggregationDefinition definition =
                new ContinuousAggregationDefinition(
                        new GreaterFilter<>(IdentityExtractor.INSTANCE(), 10L),
                        new Count<>());

        assertEquals(READY, state.apply(definition, entry(1, 5L, 20L)));
        assertEquals(Integer.valueOf(0), state.getPartialResult());

        assertEquals(READY, state.apply(definition, entry(1, 30L, 5L)));
        assertEquals(Integer.valueOf(1), state.getPartialResult());

        assertEquals(READY, state.apply(definition, entry(1, 40L, 30L)));
        assertEquals(Integer.valueOf(1), state.getPartialResult());
        }

    @Test
    public void shouldUseUpdateOperationForStableFilterMembership()
        {
        TrackingCount count = new TrackingCount();
        count.accumulate(entry(1, 20L, null));

        ContinuousAggregationState state = new ContinuousAggregationState();
        state.completeBuild(count);
        ContinuousAggregationDefinition definition =
                new ContinuousAggregationDefinition(
                        new GreaterFilter<>(IdentityExtractor.INSTANCE(), 10L),
                        new TrackingCount());

        assertEquals(READY, state.apply(definition, entry(1, 30L, 20L)));
        assertEquals(Integer.valueOf(1), state.getPartialResult());
        assertEquals(1, count.m_cUpdates);
        }

    @Test
    public void shouldMarkStateDirtyWhenRetractionRequiresRebuild()
        {
        DoubleSum<Number> sum = new DoubleSum<>(IdentityExtractor.INSTANCE());
        sum.accumulate(entry(1, (Number) Double.POSITIVE_INFINITY, null));
        sum.accumulate(entry(2, (Number) 1.0, null));

        ContinuousAggregationState state = new ContinuousAggregationState();
        state.completeBuild(sum);

        ContinuousAggregationDefinition definition =
                new ContinuousAggregationDefinition(null,
                        new DoubleSum<>(IdentityExtractor.INSTANCE()));

        assertEquals(DIRTY, state.apply(definition,
                entry(1, (Number) 0.0, (Number) Double.POSITIVE_INFINITY)));
        assertFalse(state.isReady());
        }

    @Test
    public void shouldScheduleOnlyOneBuildAtATime()
        {
        ContinuousAggregationState state = new ContinuousAggregationState();

        assertTrue(state.scheduleBuild());
        assertFalse(state.scheduleBuild());

        state.failBuild(null);
        assertTrue(state.scheduleBuild());
        }

    @Test
    public void shouldAllowReplacementValueToRestoreAStaleMinimum()
        {
        LongMin<Number> minimum = new LongMin<>(IdentityExtractor.INSTANCE());
        minimum.accumulate(entry(1, (Number) 1L, null));
        minimum.accumulate(entry(2, (Number) 5L, null));

        ContinuousAggregationState state = new ContinuousAggregationState();
        state.completeBuild(minimum);

        ContinuousAggregationDefinition definition =
                new ContinuousAggregationDefinition(null,
                        new LongMin<>(IdentityExtractor.INSTANCE()));

        assertEquals(READY, state.apply(definition,
                entry(1, (Number) 0L, (Number) 1L)));
        assertEquals(Long.valueOf(0L), state.getPartialResult());

        assertEquals(STALE_BOUND, state.apply(definition,
                entry(1, (Number) 4L, (Number) 0L)));
        assertEquals(Long.valueOf(0L), state.getBound());
        assertFalse(state.scheduleBuild());

        assertEquals(READY, state.apply(definition,
                entry(3, (Number) 0L, null)));
        assertEquals(Long.valueOf(0L), state.getPartialResult());
        }

    @Test
    public void shouldMakeAUsedStaleBoundEligibleForRebuild()
        {
        LongMin<Number> minimum = new LongMin<>(IdentityExtractor.INSTANCE());
        minimum.accumulate(entry(1, (Number) 1L, null));
        minimum.accumulate(entry(2, (Number) 5L, null));

        ContinuousAggregationState state = new ContinuousAggregationState();
        state.completeBuild(minimum);
        ContinuousAggregationDefinition definition =
                new ContinuousAggregationDefinition(null,
                        new LongMin<>(IdentityExtractor.INSTANCE()));

        assertEquals(STALE_BOUND, state.apply(definition,
                entry(1, (Number) 4L, (Number) 1L)));
        state.requireRebuild();
        assertEquals(DIRTY, state.getStatus());
        assertTrue(state.scheduleBuild());
        }

    @Test
    public void shouldRestoreAnExactCheckpointAndRejectInvalidState()
        {
        Count<Integer, Long> count = new Count<>();
        count.accumulate(entry(1, 10L, null));
        count.accumulate(entry(2, 20L, null));

        ContinuousAggregationState original = new ContinuousAggregationState();
        original.completeBuild(count);
        Object snapshot = original.snapshotState();

        ContinuousAggregationDefinition definition =
                new ContinuousAggregationDefinition(null, new Count<>());
        ContinuousAggregationState restored = new ContinuousAggregationState();
        assertTrue(restored.restoreState(definition, snapshot));
        assertEquals(READY, restored.getStatus());
        assertEquals(Integer.valueOf(2), restored.getPartialResult());

        assertEquals(READY, restored.apply(definition, removedEntry(1, 10L)));
        assertEquals(Integer.valueOf(1), restored.getPartialResult());

        assertFalse(restored.restoreState(definition, "not a count"));
        assertEquals(BUILDING, restored.getStatus());
        }

    private static <K, V> SimpleMapEntry<K, V> entry(K key, V value, V original)
        {
        return original == null
               ? new SimpleMapEntry<>(key, value)
               : new SimpleMapEntry<>(key, value, original);
        }

    private static <K, V> SimpleMapEntry<K, V> removedEntry(K key, V original)
        {
        return new SimpleMapEntry<>(key, null, original)
            {
            @Override
            public boolean isPresent()
                {
                return false;
                }
            };
        }

    private static class TrackingCount
            extends Count<Integer, Long>
        {
        @Override
        public InvocableMap.StreamingAggregator<Integer, Long, Integer, Integer> supply()
            {
            return new TrackingCount();
            }

        @Override
        public RetractionResult update(InvocableMap.Entry<? extends Integer, ? extends Long> entry)
            {
            m_cUpdates++;
            return super.update(entry);
            }

        private int m_cUpdates;
        }
    }
