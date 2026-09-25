/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;

import com.tangosol.internal.net.ContinuousAggregationBound;
import com.tangosol.internal.util.aggregator.BigDecimalSerializationWrapper;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.SimpleMapEntry;

import com.tangosol.util.extractor.IdentityExtractor;

import org.junit.Test;

import java.math.BigDecimal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import static com.tangosol.util.InvocableMap.StreamingAggregator.RetractionResult.REBUILD_REQUIRED;
import static com.tangosol.util.InvocableMap.StreamingAggregator.RetractionResult.STALE_BOUND;
import static com.tangosol.util.InvocableMap.StreamingAggregator.RetractionResult.UPDATED;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for continuously maintained streaming aggregators.
 */
public class ContinuousAggregatorTest
    {
    @Test
    public void shouldAdvertiseContinuousSupportOnlyWhenImplemented()
        {
        assertTrue(new Count<>().isContinuous());
        assertTrue(new LongSum<>(IdentityExtractor.INSTANCE()).isContinuous());
        assertTrue(new DoubleSum<>(IdentityExtractor.INSTANCE()).isContinuous());
        assertTrue(new DoubleAverage<>(IdentityExtractor.INSTANCE()).isContinuous());
        assertTrue(new BigDecimalSum<>(IdentityExtractor.INSTANCE()).isContinuous());
        assertTrue(new BigDecimalAverage<>(IdentityExtractor.INSTANCE()).isContinuous());
        assertTrue(new ReducerAggregator<>(IdentityExtractor.INSTANCE()).isContinuous());
        assertTrue(new LongMin<>(IdentityExtractor.INSTANCE()).isContinuous());
        assertTrue(new LongMax<>(IdentityExtractor.INSTANCE()).isContinuous());
        assertTrue(new DoubleMin<>(IdentityExtractor.INSTANCE()).isContinuous());
        assertTrue(new DoubleMax<>(IdentityExtractor.INSTANCE()).isContinuous());
        assertTrue(new BigDecimalMin<>(IdentityExtractor.INSTANCE()).isContinuous());
        assertTrue(new BigDecimalMax<>(IdentityExtractor.INSTANCE()).isContinuous());
        assertTrue(new ComparableMin<>(IdentityExtractor.INSTANCE()).isContinuous());
        assertTrue(new ComparableMax<>(IdentityExtractor.INSTANCE()).isContinuous());
        assertTrue(new DistinctValues<>(IdentityExtractor.INSTANCE()).isContinuous());
        assertTrue(new TopNAggregator<>(IdentityExtractor.INSTANCE(), null, 3).isContinuous());

        GroupAggregator group = GroupAggregator.createInstance(
                IdentityExtractor.INSTANCE(), new Count<>());
        assertTrue(group.isContinuous());

        CompositeAggregator composite = new CompositeAggregator(
                new InvocableMap.EntryAggregator[] {new Count<>(),
                        new LongSum<>(IdentityExtractor.INSTANCE())});
        assertTrue(composite.isContinuous());

        assertFalse(GroupAggregator.createInstance(IdentityExtractor.INSTANCE(),
                new PriorityAggregator<>(new Count<>())).isContinuous());
        assertFalse(new CompositeAggregator(new InvocableMap.EntryAggregator[] {
                new Count<>(), new PriorityAggregator<>(new Count<>())}).isContinuous());

        PriorityAggregator<Integer, Integer, Integer, Integer> priority =
                new PriorityAggregator<>(new Count<>());
        assertFalse(priority.isContinuous());
        assertFalse(priority.isStateCheckpointable());

        assertTrue(new Count<>().isStateCheckpointable());
        assertTrue(group.isStateCheckpointable());
        assertTrue(composite.isStateCheckpointable());
        }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void shouldCheckpointAndRestoreMaintenanceState()
        {
        Count<Integer, Integer> count = new Count<>();
        count.accumulate(entry(1, 10));
        count.accumulate(entry(2, 20));
        Count<Integer, Integer> countRestored = restore(count);
        assertEquals(UPDATED, countRestored.retract(update(1, 10, null)));
        assertEquals(Integer.valueOf(1), countRestored.getPartialResult());

        DistinctValues<Integer, String, String, String> distinct =
                new DistinctValues<>(IdentityExtractor.INSTANCE());
        distinct.accumulate(entry(1, "one"));
        distinct.accumulate(entry(2, "one"));
        distinct.accumulate(entry(3, "two"));
        DistinctValues distinctRestored = restore(distinct);
        assertEquals(UPDATED, distinctRestored.retract(update(1, "one", null)));
        assertTrue(((Collection) distinctRestored.getPartialResult()).contains("one"));
        assertEquals(UPDATED, distinctRestored.retract(update(2, "one", null)));
        assertFalse(((Collection) distinctRestored.getPartialResult()).contains("one"));

        LongMin<Number> minimum = new LongMin<>(IdentityExtractor.INSTANCE());
        minimum.accumulate(entry(1, (Number) 1L));
        minimum.accumulate(entry(2, (Number) 1L));
        minimum.accumulate(entry(3, (Number) 5L));
        LongMin minimumRestored = restore(minimum);
        assertEquals(UPDATED, minimumRestored.retract(update(1, 1L, null)));
        assertEquals(STALE_BOUND, minimumRestored.retract(update(2, 1L, null)));

        ReducerAggregator<Integer, Integer, Integer, Integer> reducer =
                new ReducerAggregator<>(IdentityExtractor.INSTANCE());
        reducer.accumulate(entry(1, 10));
        reducer.accumulate(entry(2, 20));
        ReducerAggregator reducerRestored = restore(reducer);
        assertEquals(UPDATED, reducerRestored.retract(update(1, 10, null)));
        assertEquals(Map.of(2, 20), reducerRestored.getPartialResult());

        TopNAggregator<Integer, Integer, Integer, Integer> top =
                new TopNAggregator<>(IdentityExtractor.INSTANCE(), null, 2);
        top.accumulate(entry(1, 1));
        top.accumulate(entry(2, 2));
        top.accumulate(entry(3, 3));
        TopNAggregator topRestored = restore(top);
        assertEquals(REBUILD_REQUIRED, topRestored.retract(update(3, 3, null)));
        }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void shouldCheckpointAndRestoreNestedAggregators()
        {
        GroupAggregator<Integer, Long, Long, Long, Integer> group =
                GroupAggregator.createInstance(IdentityExtractor.INSTANCE(), new Count<>());
        group.accumulate(entry(1, 10L));
        group.accumulate(entry(2, 10L));
        group.accumulate(entry(3, 20L));

        GroupAggregator groupRestored = restore(group);
        assertEquals(UPDATED, groupRestored.retract(update(1, 10L, null)));
        assertEquals(Map.of(10L, 1, 20L, 1), finalizePartial(groupRestored));

        CompositeAggregator<Integer, Number> composite = new CompositeAggregator<>(
                new InvocableMap.EntryAggregator[] {new Count<>(),
                        new LongSum<>(IdentityExtractor.INSTANCE())});
        composite.accumulate(entry(1, (Number) 2L));
        composite.accumulate(entry(2, (Number) 3L));

        CompositeAggregator compositeRestored = restore(composite);
        SimpleMapEntry<Integer, Number> update = update(1, (Number) 2L, (Number) 7L);
        assertEquals(UPDATED, compositeRestored.retract(update));
        compositeRestored.accumulate(update);
        assertEquals(Arrays.asList(2, 10L), finalizePartial(compositeRestored));
        }

    @Test
    public void shouldMaintainCount()
        {
        Count<Integer, Integer> count = new Count<>();

        count.accumulate(entry(1, 10));
        count.accumulate(entry(2, 20));

        assertEquals(Integer.valueOf(2), count.getPartialResult());
        assertEquals(Integer.valueOf(2), count.getPartialResult());

        SimpleMapEntry<Integer, Integer> update = update(1, 10, 11);
        assertEquals(UPDATED, count.retract(update));
        count.accumulate(update);

        assertEquals(UPDATED, count.retract(update(2, 20, null)));
        assertEquals(Integer.valueOf(1), count.getPartialResult());
        assertEquals(Integer.valueOf(1), finalizePartial(count));

        assertEquals(UPDATED, count.retract(entry(3, 30)));
        assertEquals(Integer.valueOf(1), count.getPartialResult());
        }

    @Test
    public void shouldOptimizeStableUpdates()
        {
        Count<Integer, Number> count = new Count<>();
        count.accumulate(entry(1, (Number) 10L));
        assertEquals(UPDATED, count.update(update(1, (Number) 10L, (Number) 11L)));
        assertEquals(Integer.valueOf(1), count.getPartialResult());

        LongSum<Number> sum = new LongSum<>(IdentityExtractor.INSTANCE());
        sum.accumulate(entry(1, (Number) 10L));
        assertEquals(UPDATED, sum.update(update(1, (Number) 10L, (Number) 10L)));
        assertEquals(Long.valueOf(10L), sum.getPartialResult());
        assertEquals(UPDATED, sum.update(update(1, (Number) 10L, (Number) 7L)));
        assertEquals(Long.valueOf(7L), sum.getPartialResult());

        LongMin<Number> minimum = new LongMin<>(IdentityExtractor.INSTANCE());
        minimum.accumulate(entry(1, (Number) 1L));
        assertEquals(UPDATED, minimum.update(update(1, (Number) 1L, (Number) 1L)));
        assertEquals(UPDATED, minimum.getMaintenanceStatus());
        assertEquals(Long.valueOf(1L), minimum.getPartialResult());
        }

    @Test
    public void shouldMaintainLongSumUsingOriginalValues()
        {
        LongSum<Number> sum = new LongSum<>(IdentityExtractor.INSTANCE());

        sum.accumulate(entry(1, (Number) 10L));
        sum.accumulate(entry(2, (Number) null));
        sum.accumulate(entry(3, (Number) Long.valueOf(-4L)));

        assertEquals(Long.valueOf(6), sum.getPartialResult());
        assertEquals(Long.valueOf(6), sum.getPartialResult());

        SimpleMapEntry<Integer, Number> update = update(1, 10L, 7L);
        assertEquals(UPDATED, sum.retract(update));
        sum.accumulate(update);
        assertEquals(Long.valueOf(3), finalizePartial(sum));

        assertEquals(UPDATED, sum.retract(update(3, -4L, null)));
        assertEquals(Long.valueOf(7), finalizePartial(sum));

        SimpleMapEntry<Integer, Number> fromNull = update(2, null, 5L);
        assertEquals(UPDATED, sum.retract(fromNull));
        sum.accumulate(fromNull);
        assertEquals(Long.valueOf(12), finalizePartial(sum));

        InvocableMap.StreamingAggregator<Object, Object, Object, Long> supplied = sum.supply();
        assertNull(supplied.getPartialResult());
        }

    @Test
    public void shouldMaintainDoubleSumAndAverage()
        {
        DoubleSum<Number> sum = new DoubleSum<>(IdentityExtractor.INSTANCE());
        sum.accumulate(entry(1, (Number) 2.5));
        sum.accumulate(entry(2, (Number) 1.5));

        SimpleMapEntry<Integer, Number> update = update(1, 2.5, 6.5);
        assertEquals(UPDATED, sum.retract(update));
        sum.accumulate(update);
        assertEquals(8.0, (Double) finalizePartial(sum), 0.0);

        assertEquals(UPDATED, sum.retract(update(2, 1.5, null)));
        assertEquals(6.5, (Double) finalizePartial(sum), 0.0);

        DoubleAverage<Number> average = new DoubleAverage<>(IdentityExtractor.INSTANCE());
        average.accumulate(entry(1, (Number) 2.0));
        average.accumulate(entry(2, (Number) 4.0));

        byte[] abPartialOne = (byte[]) average.getPartialResult();
        byte[] abPartialTwo = (byte[]) average.getPartialResult();
        assertNotSame(abPartialOne, abPartialTwo);

        update = update(1, 2.0, 8.0);
        assertEquals(UPDATED, average.retract(update));
        average.accumulate(update);
        assertEquals(6.0, (Double) finalizePartial(average), 0.0);

        assertEquals(UPDATED, average.retract(update(2, 4.0, null)));
        assertEquals(8.0, (Double) finalizePartial(average), 0.0);
        }

    @Test
    public void shouldRequireRebuildWhenRetractingNonFiniteDouble()
        {
        DoubleSum<Number> sum = new DoubleSum<>(IdentityExtractor.INSTANCE());
        sum.accumulate(entry(1, (Number) Double.POSITIVE_INFINITY));
        sum.accumulate(entry(2, (Number) 1.0));

        assertEquals(REBUILD_REQUIRED,
                sum.retract(update(1, Double.POSITIVE_INFINITY, null)));

        DoubleAverage<Number> average = new DoubleAverage<>(IdentityExtractor.INSTANCE());
        average.accumulate(entry(1, (Number) Double.NaN));
        average.accumulate(entry(2, (Number) 1.0));

        assertEquals(REBUILD_REQUIRED, average.retract(update(1, Double.NaN, null)));
        }

    @Test
    public void shouldMaintainBigDecimalSumAndAverage()
        {
        BigDecimalSum<Number> sum = new BigDecimalSum<>(IdentityExtractor.INSTANCE());
        sum.accumulate(entry(1, (Number) new BigDecimal("2.50")));
        sum.accumulate(entry(2, (Number) new BigDecimal("3.50")));

        BigDecimalSerializationWrapper partialOne =
                (BigDecimalSerializationWrapper) sum.getPartialResult();
        BigDecimalSerializationWrapper partialTwo =
                (BigDecimalSerializationWrapper) sum.getPartialResult();
        assertNotSame(partialOne, partialTwo);
        assertBigDecimalEquals("6.00", partialOne.getBigDecimal());
        assertBigDecimalEquals("6.00", partialTwo.getBigDecimal());

        SimpleMapEntry<Integer, Number> update =
                update(1, new BigDecimal("2.50"), new BigDecimal("4.50"));
        assertEquals(UPDATED, sum.retract(update));
        sum.accumulate(update);
        assertBigDecimalEquals("8.00", (BigDecimal) finalizePartial(sum));

        BigDecimalAverage<Number> average =
                new BigDecimalAverage<>(IdentityExtractor.INSTANCE());
        average.accumulate(entry(1, (Number) new BigDecimal("2.0")));
        average.accumulate(entry(2, (Number) new BigDecimal("4.0")));

        update = update(1, new BigDecimal("2.0"), new BigDecimal("8.0"));
        assertEquals(UPDATED, average.retract(update));
        average.accumulate(update);
        assertBigDecimalEquals("6.0", (BigDecimal) finalizePartial(average));

        assertEquals(UPDATED,
                average.retract(update(2, new BigDecimal("4.0"), null)));
        assertBigDecimalEquals("8.0", (BigDecimal) finalizePartial(average));
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldMaintainReducerAndReturnDetachedPartials()
        {
        ReducerAggregator<Integer, Integer, Integer, Integer> reducer =
                new ReducerAggregator<>(IdentityExtractor.INSTANCE());
        reducer.accumulate(entry(1, 10));
        reducer.accumulate(entry(2, 20));

        Map<Integer, Integer> partial = (Map<Integer, Integer>) reducer.getPartialResult();
        partial.remove(1);
        partial.put(3, 30);

        Map<Integer, Integer> expected = new HashMap<>();
        expected.put(1, 10);
        expected.put(2, 20);
        assertEquals(expected, reducer.getPartialResult());

        SimpleMapEntry<Integer, Integer> update = update(1, 10, 11);
        assertEquals(UPDATED, reducer.update(update));
        assertEquals(UPDATED, reducer.retract(update(2, 20, null)));

        expected.clear();
        expected.put(1, 11);
        assertEquals(expected, finalizePartial(reducer));

        assertEquals(REBUILD_REQUIRED, reducer.retract(update(9, 90, null)));
        }

    @Test
    public void shouldMaintainMinimumAsRecoverableBound()
        {
        LongMin<Number> minimum = new LongMin<>(IdentityExtractor.INSTANCE());
        minimum.accumulate(entry(1, (Number) 1L));
        minimum.accumulate(entry(2, (Number) 1L));
        minimum.accumulate(entry(3, (Number) 5L));

        assertEquals(UPDATED, minimum.retract(update(1, (Number) 1L, null)));
        assertEquals(Long.valueOf(1), minimum.getPartialResult());

        assertEquals(STALE_BOUND, minimum.retract(update(2, (Number) 1L, null)));
        assertEquals(STALE_BOUND, minimum.getMaintenanceStatus());
        assertEquals(Long.valueOf(1), minimum.getPartialResult());

        ContinuousAggregationBound bound = minimum;
        assertFalse(bound.isContinuousAggregationBoundDominated(1L, 3L));
        assertTrue(bound.isContinuousAggregationBoundDominated(1L, 0L));

        minimum.accumulate(entry(4, (Number) 3L));
        assertEquals(STALE_BOUND, minimum.getMaintenanceStatus());

        minimum.accumulate(entry(5, (Number) 1L));
        assertEquals(UPDATED, minimum.getMaintenanceStatus());
        assertEquals(Long.valueOf(1), minimum.getPartialResult());

        assertEquals(STALE_BOUND, minimum.retract(update(5, (Number) 1L, null)));
        minimum.accumulate(entry(6, (Number) 0L));
        assertEquals(UPDATED, minimum.getMaintenanceStatus());
        assertEquals(Long.valueOf(0), minimum.getPartialResult());
        }

    @Test
    public void shouldMaintainAllExtremumFamilies()
        {
        ComparableMax<Number, Number> comparableMax =
                new ComparableMax<>(IdentityExtractor.INSTANCE(), Comparator.comparingLong(Number::longValue));
        comparableMax.accumulate(entry(1, (Number) 4L));
        comparableMax.accumulate(entry(2, (Number) 9L));
        assertEquals(STALE_BOUND,
                comparableMax.retract(update(2, (Number) 9L, null)));

        DoubleMax<Number> doubleMax = new DoubleMax<>(IdentityExtractor.INSTANCE());
        doubleMax.accumulate(entry(1, (Number) Double.valueOf(-10.0)));
        doubleMax.accumulate(entry(2, (Number) Double.valueOf(-4.0)));
        assertEquals(Double.valueOf(-4.0), doubleMax.getPartialResult());
        assertEquals(STALE_BOUND,
                doubleMax.retract(update(2, (Number) Double.valueOf(-4.0), null)));

        BigDecimalMin<Number> decimalMin =
                new BigDecimalMin<>(IdentityExtractor.INSTANCE());
        decimalMin.accumulate(entry(1, (Number) new BigDecimal("2.0")));
        decimalMin.accumulate(entry(2, (Number) new BigDecimal("5.0")));
        assertEquals(STALE_BOUND, decimalMin.retract(
                update(1, (Number) new BigDecimal("2.00"), null)));
        decimalMin.accumulate(entry(3, (Number) new BigDecimal("1.0")));
        assertEquals(UPDATED, decimalMin.getMaintenanceStatus());
        assertBigDecimalEquals("1.0", (BigDecimal) finalizePartial(decimalMin));
        }

    @Test
    public void shouldMaintainDistinctValueFrequencies()
        {
        DistinctValues<Integer, String, String, String> distinct =
                new DistinctValues<>(IdentityExtractor.INSTANCE());
        distinct.accumulate(entry(1, "one"));
        distinct.accumulate(entry(2, "one"));
        distinct.accumulate(entry(3, "two"));

        Collection<String> partial = (Collection<String>) distinct.getPartialResult();
        partial.clear();
        assertEquals(2, ((Collection<?>) distinct.getPartialResult()).size());

        assertEquals(UPDATED, distinct.retract(update(1, "one", null)));
        assertTrue(((Collection<?>) distinct.getPartialResult()).contains("one"));
        assertEquals(UPDATED, distinct.retract(update(2, "one", null)));
        assertEquals(1, ((Collection<?>) distinct.getPartialResult()).size());
        assertTrue(((Collection<?>) distinct.getPartialResult()).contains("two"));
        }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void shouldMaintainGroupAndCompositeDelegates()
        {
        GroupAggregator<Integer, Long, Long, Long, Integer> group =
                GroupAggregator.createInstance(IdentityExtractor.INSTANCE(), new Count<>());
        group.accumulate(entry(1, 10L));
        group.accumulate(entry(2, 10L));
        group.accumulate(entry(3, 20L));
        assertEquals(UPDATED, group.update(update(1, 10L, 20L)));

        Map<Long, Integer> grouped = (Map<Long, Integer>) finalizePartial(group);
        Map<Long, Integer> expected = new HashMap<>();
        expected.put(10L, 1);
        expected.put(20L, 2);
        assertEquals(expected, grouped);

        CompositeAggregator<Integer, Number> composite = new CompositeAggregator<>(
                new InvocableMap.EntryAggregator[] {new Count<>(),
                        new LongSum<>(IdentityExtractor.INSTANCE())});
        composite.accumulate(entry(1, (Number) 2L));
        composite.accumulate(entry(2, (Number) 3L));
        SimpleMapEntry<Integer, Number> update = update(1, (Number) 2L, (Number) 7L);
        assertEquals(UPDATED, composite.update(update));
        assertEquals(Arrays.asList(2, 10L), finalizePartial(composite));
        }

    @Test
    public void shouldRebuildTopNOnlyWhenARetainedValueIsRemoved()
        {
        TopNAggregator<Integer, Integer, Integer, Integer> top =
                new TopNAggregator<>(IdentityExtractor.INSTANCE(), null, 2);
        top.accumulate(entry(1, 1));
        top.accumulate(entry(2, 2));
        top.accumulate(entry(3, 3));
        top.accumulate(entry(4, 4));

        TopNAggregator.PartialResult<Integer> partial = top.getPartialResult();
        partial.clear();
        assertEquals(2, top.getPartialResult().size());

        assertEquals(UPDATED, top.retract(update(1, 1, null)));
        assertEquals(REBUILD_REQUIRED, top.retract(update(4, 4, null)));
        }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object finalizePartial(InvocableMap.StreamingAggregator aggregator)
        {
        InvocableMap.StreamingAggregator combiner = aggregator.supply();
        combiner.combine(aggregator.getPartialResult());
        return combiner.finalizeResult();
        }

    @SuppressWarnings("unchecked")
    private static <A extends InvocableMap.StreamingAggregator> A restore(A aggregator)
        {
        A restored = (A) aggregator.supply();
        restored.restoreState(aggregator.snapshotState());
        return restored;
        }

    private static void assertBigDecimalEquals(String expected, BigDecimal actual)
        {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
        }

    private static <K, V> SimpleMapEntry<K, V> entry(K key, V value)
        {
        return new SimpleMapEntry<>(key, value);
        }

    private static <K, V> SimpleMapEntry<K, V> update(K key, V original, V value)
        {
        return new SimpleMapEntry<>(key, value, original);
        }
    }
