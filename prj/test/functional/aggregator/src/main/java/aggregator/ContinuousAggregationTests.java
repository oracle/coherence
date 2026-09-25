/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package aggregator;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.net.ContinuousAggregator;
import com.tangosol.net.NamedMap;

import com.tangosol.util.CompositeKey;
import com.tangosol.util.Extractors;

import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.CompositeAggregator;
import com.tangosol.util.aggregator.DistinctValues;
import com.tangosol.util.aggregator.GroupAggregator;
import com.tangosol.util.aggregator.LongMax;
import com.tangosol.util.aggregator.LongMin;
import com.tangosol.util.aggregator.LongSum;
import com.tangosol.util.aggregator.TopNAggregator;

import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.KeyAssociatedFilter;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for continuously maintained distributed aggregations.
 *
 * @author Aleks Seovic  2026.09.06
 * @since 26.10
 */
public class ContinuousAggregationTests
        extends AbstractFunctionalTest
    {
    @BeforeClass
    public static void startup()
        {
        startCacheServer("ContinuousAggregationTests-1", "aggregator");
        startCacheServer("ContinuousAggregationTests-2", "aggregator");
        }

    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("ContinuousAggregationTests-1");
        stopCacheServer("ContinuousAggregationTests-2");
        }

    @Test
    public void shouldMaintainAndQueryPartitionResultsAcrossMembers()
        {
        NamedMap<Integer, Long> map = getNamedCache("dist-continuous-aggregation");
        waitForBalanced(map.getService());
        map.clear();

        Map<Integer, Long> entries = new HashMap<>();
        for (int i = 0; i < 100; i++)
            {
            entries.put(i, (long) i);
            }
        map.putAll(entries);

        ContinuousAggregator<Integer, Long, Integer> count = map.addAggregator(new Count<>());
        ContinuousAggregator<Integer, Long, Long> sum = map.addAggregator(
                new LongSum<>(IdentityExtractor.INSTANCE()));
        ContinuousAggregator<Integer, Long, Integer> filtered = map.addAggregator(
                new GreaterFilter<>(IdentityExtractor.INSTANCE(), 50L),
                new Count<>());

        assertEquals(Integer.valueOf(100), count.aggregate());
        assertEquals(Long.valueOf(4950L), sum.aggregate());
        assertEquals(Integer.valueOf(49), filtered.aggregate());

        map.put(0, 1000L);
        map.remove(1);
        map.put(100, 75L);

        assertEquals(Integer.valueOf(100), count.aggregate());
        assertEquals(Long.valueOf(6024L), sum.aggregate());
        assertEquals(Integer.valueOf(51), filtered.aggregate());

        // Equal definitions are idempotent and resolve to the same server-side
        // registration even though each call returns a map-bound handle.
        ContinuousAggregator<Integer, Long, Integer> duplicate = map.addAggregator(new Count<>());
        assertEquals(Integer.valueOf(100), duplicate.aggregate());

        map.removeAggregator(filtered);
        map.removeAggregator(sum);
        map.removeAggregator(count);
        }

    @Test
    public void shouldMaintainAndQueryOnlyKeyAssociatedPartitions()
        {
        NamedMap<CompositeKey<String, Integer>, Long> map =
                getNamedCache("dist-continuous-aggregation-affinity");
        waitForBalanced(map.getService());
        map.clear();

        CompositeKey<String, Integer> keyOne   = new CompositeKey<>("customer-1", 1);
        CompositeKey<String, Integer> keyTwo   = new CompositeKey<>("customer-1", 2);
        CompositeKey<String, Integer> keyOther = new CompositeKey<>("customer-2", 1);
        map.put(keyOne, 10L);
        map.put(keyTwo, 20L);
        map.put(keyOther, 100L);

        KeyAssociatedFilter<Long> filterOne = new KeyAssociatedFilter<>(
                new GreaterFilter<>(IdentityExtractor.INSTANCE(), 10L), "customer-1");
        KeyAssociatedFilter<Long> filterOther = new KeyAssociatedFilter<>(
                new GreaterFilter<>(IdentityExtractor.INSTANCE(), 10L), "customer-2");
        ContinuousAggregator<CompositeKey<String, Integer>, Long, Integer> countOne =
                map.addAggregator(filterOne, new Count<>());
        ContinuousAggregator<CompositeKey<String, Integer>, Long, Integer> countOther =
                map.addAggregator(filterOther, new Count<>());

        try
            {
            assertEquals(map.aggregate(filterOne, new Count<>()), countOne.aggregate());
            assertEquals(Integer.valueOf(1), countOne.aggregate());
            assertEquals(Integer.valueOf(1), countOther.aggregate());

            // Mutations in another affinity group must not affect this
            // registration, even when the wrapped filter matches.
            map.put(new CompositeKey<>("customer-2", 2), 200L);
            assertEquals(Integer.valueOf(1), countOne.aggregate());
            assertEquals(Integer.valueOf(2), countOther.aggregate());

            map.put(keyOne, 30L);
            map.remove(keyTwo);
            assertEquals(map.aggregate(filterOne, new Count<>()), countOne.aggregate());
            assertEquals(Integer.valueOf(1), countOne.aggregate());
            }
        finally
            {
            map.removeAggregator(countOther);
            map.removeAggregator(countOne);
            }
        }

    @Test
    public void shouldInvokeAgainstFinalizedPartitionResults()
        {
        NamedMap<CompositeKey<String, Integer>, Long> map =
                getNamedCache("dist-continuous-aggregation-invoke");
        waitForBalanced(map.getService());
        map.clear();

        CompositeKey<String, Integer> bettorOneBetOne =
                new CompositeKey<>("bettor-1", 1);
        CompositeKey<String, Integer> bettorOneBetTwo =
                new CompositeKey<>("bettor-1", 2);
        CompositeKey<String, Integer> bettorTwoBetOne =
                new CompositeKey<>("bettor-2", 1);
        CompositeKey<String, Integer> bettorOneAnchor =
                new CompositeKey<>("bettor-1", -1);
        CompositeKey<String, Integer> bettorTwoAnchor =
                new CompositeKey<>("bettor-2", -1);

        map.put(bettorOneBetOne, 10L);
        map.put(bettorOneBetTwo, 20L);
        map.put(bettorTwoBetOne, 100L);

        GroupAggregator<CompositeKey<String, Integer>, Long, Long, String, Long> sumByBettor =
                GroupAggregator.<CompositeKey<String, Integer>, Long, Long, String, Long>createInstance(
                        Extractors.<Long, String>key("getPrimaryKey"),
                        new LongSum<>(IdentityExtractor.INSTANCE()),
                        null);
        ContinuousAggregator<CompositeKey<String, Integer>, Long, Map<String, Long>> continuous =
                map.addAggregator(sumByBettor);
        try
            {
            assertEquals(Long.valueOf(30L),
                    continuous.invoke(bettorOneAnchor,
                            partial -> partial.get("bettor-1")));

            Map<CompositeKey<String, Integer>, Long> results = continuous.invokeAll(
                    Arrays.asList(bettorOneAnchor, bettorTwoAnchor),
                    (key, partial) -> partial.get(key.getPrimaryKey()));
            assertEquals(Long.valueOf(30L), results.get(bettorOneAnchor));
            assertEquals(Long.valueOf(100L), results.get(bettorTwoAnchor));

            Map<String, Long> partitionResult = map.invoke(bettorOneAnchor,
                    entry -> entry.asBinaryEntry()
                            .getContinuousAggregationResult(sumByBettor));
            assertEquals(Long.valueOf(30L), partitionResult.get("bettor-1"));

            map.put(bettorOneBetTwo, 25L);
            assertEquals(Long.valueOf(35L),
                    continuous.invoke(bettorOneAnchor,
                            partial -> partial.get("bettor-1")));
            }
        finally
            {
            map.removeAggregator(continuous);
            }
        }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void shouldMaintainCompositeAndDifficultAggregators()
        {
        NamedMap<Integer, Long> map = getNamedCache("dist-continuous-aggregation-phase-4");
        waitForBalanced(map.getService());
        map.clear();

        Map<Integer, Long> entries = new HashMap<>();
        for (int i = 0; i < 100; i++)
            {
            entries.put(i, (long) (i % 5));
            }
        map.putAll(entries);

        ContinuousAggregator<Integer, Long, Long> minimum = map.addAggregator(
                new LongMin<>(IdentityExtractor.INSTANCE()));
        ContinuousAggregator<Integer, Long, Long> maximum = map.addAggregator(
                new LongMax<>(IdentityExtractor.INSTANCE()));
        ContinuousAggregator<Integer, Long, Collection<Long>> distinct = map.addAggregator(
                new DistinctValues<>(IdentityExtractor.INSTANCE()));
        ContinuousAggregator<Integer, Long, Map<Long, Integer>> grouped = map.addAggregator(
                GroupAggregator.createInstance(IdentityExtractor.INSTANCE(), new Count<>()));
        ContinuousAggregator<Integer, Long, List> composite = map.addAggregator(
                new CompositeAggregator(new com.tangosol.util.InvocableMap.EntryAggregator[] {
                        new Count<>(), new LongSum<>(IdentityExtractor.INSTANCE())}));
        ContinuousAggregator<Integer, Long, Long[]> top = map.addAggregator(
                new TopNAggregator<>(IdentityExtractor.INSTANCE(), null, 3));

        assertEquals(Long.valueOf(0L), minimum.aggregate());
        assertEquals(Long.valueOf(4L), maximum.aggregate());
        assertEquals(new HashSet<>(Arrays.asList(0L, 1L, 2L, 3L, 4L)),
                new HashSet<>(distinct.aggregate()));
        assertEquals(Integer.valueOf(20), grouped.aggregate().get(0L));
        assertEquals(Arrays.asList(100, 200L), composite.aggregate());
        assertArrayEquals(new Long[] {4L, 4L, 4L}, top.aggregate());

        for (int i = 0; i < 100; i += 5)
            {
            map.remove(i);
            }
        map.put(1, 10L);

        assertEquals(Long.valueOf(1L), minimum.aggregate());
        assertEquals(Long.valueOf(10L), maximum.aggregate());
        assertEquals(new HashSet<>(Arrays.asList(1L, 2L, 3L, 4L, 10L)),
                new HashSet<>(distinct.aggregate()));

        Map<Long, Integer> mapGrouped = grouped.aggregate();
        assertEquals(Integer.valueOf(19), mapGrouped.get(1L));
        assertEquals(Integer.valueOf(20), mapGrouped.get(2L));
        assertEquals(Integer.valueOf(1), mapGrouped.get(10L));
        assertEquals(Arrays.asList(80, 209L), composite.aggregate());
        assertArrayEquals(new Long[] {10L, 4L, 4L}, top.aggregate());

        map.put(100, 0L);
        assertEquals(Long.valueOf(0L), minimum.aggregate());

        map.removeAggregator(top);
        map.removeAggregator(composite);
        map.removeAggregator(grouped);
        map.removeAggregator(distinct);
        map.removeAggregator(maximum);
        map.removeAggregator(minimum);
        }

    @Test
    public void shouldRemainExactAcrossTransferAndBackupPromotion()
        {
        NamedMap<Integer, Long> map = getNamedCache("dist-continuous-aggregation-phase-5");
        waitForBalanced(map.getService());
        map.clear();

        Map<Integer, Long> entries = new HashMap<>();
        for (int i = 0; i < 1000; i++)
            {
            entries.put(i, (long) i);
            }
        map.putAll(entries);

        ContinuousAggregator<Integer, Long, Integer> count = map.addAggregator(new Count<>());
        ContinuousAggregator<Integer, Long, Long> sum = map.addAggregator(
                new LongSum<>(IdentityExtractor.INSTANCE()));
        ContinuousAggregator<Integer, Long, Collection<Long>> distinct = map.addAggregator(
                new DistinctValues<>(IdentityExtractor.INSTANCE()));

        String  sMember  = "ContinuousAggregationTests-phase-5";
        boolean fRunning = false;
        try
            {
            assertEquals(Integer.valueOf(1000), count.aggregate());
            assertEquals(Long.valueOf(499500L), sum.aggregate());

            startCacheServer(sMember, "aggregator");
            fRunning = true;
            waitForBalanced(map.getService());

            // Joining the third member causes planned primary transfers. The
            // exact maintenance checkpoints must remain usable afterwards.
            assertEquals(Integer.valueOf(1000), count.aggregate());
            assertEquals(Long.valueOf(499500L), sum.aggregate());
            assertEquals(1000, distinct.aggregate().size());

            map.put(1000, 1000L);
            assertEquals(Integer.valueOf(1001), count.aggregate());
            assertEquals(Long.valueOf(500500L), sum.aggregate());

            // An ungraceful stop promotes backup partitions. Those partitions
            // have no maintained backup state and must rebuild or use the
            // normal exact fallback scan until their rebuild is complete.
            stopCacheServer(sMember, false);
            fRunning = false;
            waitForBalanced(map.getService());

            assertEquals(Integer.valueOf(1001), count.aggregate());
            assertEquals(Long.valueOf(500500L), sum.aggregate());
            assertEquals(1001, distinct.aggregate().size());

            map.remove(0);
            map.put(1000, 2000L);
            assertEquals(Integer.valueOf(1000), count.aggregate());
            assertEquals(Long.valueOf(501500L), sum.aggregate());
            }
        finally
            {
            if (fRunning)
                {
                stopCacheServer(sMember, false);
                waitForBalanced(map.getService());
                }
            map.removeAggregator(distinct);
            map.removeAggregator(sum);
            map.removeAggregator(count);
            }
        }

    @Test
    public void shouldRetainDefinitionsAcrossClearAndTruncate()
        {
        NamedMap<Integer, Long> map = getNamedCache("dist-continuous-aggregation-truncate");
        waitForBalanced(map.getService());
        map.clear();
        map.put(1, 10L);
        map.put(2, 20L);

        ContinuousAggregator<Integer, Long, Integer> count = map.addAggregator(new Count<>());
        ContinuousAggregator<Integer, Long, Long> sum = map.addAggregator(
                new LongSum<>(IdentityExtractor.INSTANCE()));

        assertEquals(Integer.valueOf(2), count.aggregate());
        assertEquals(Long.valueOf(30L), sum.aggregate());

        map.clear();
        assertEquals(Integer.valueOf(0), count.aggregate());
        assertNull(sum.aggregate());

        map.put(3, 30L);
        map.truncate();
        assertEquals(Integer.valueOf(0), count.aggregate());
        assertNull(sum.aggregate());

        map.put(4, 40L);
        assertEquals(Integer.valueOf(1), count.aggregate());
        assertEquals(Long.valueOf(40L), sum.aggregate());

        map.removeAggregator(sum);
        map.removeAggregator(count);
        }

    @Test
    public void shouldMatchOnDemandAggregationAfterRandomMutations()
        {
        NamedMap<Integer, Long> map = getNamedCache("dist-continuous-aggregation-random");
        waitForBalanced(map.getService());
        map.clear();

        GreaterFilter<Long, Long> filter =
                new GreaterFilter<>(IdentityExtractor.INSTANCE(), 10L);
        ContinuousAggregator<Integer, Long, Integer> count = map.addAggregator(new Count<>());
        ContinuousAggregator<Integer, Long, Long> sum = map.addAggregator(
                new LongSum<>(IdentityExtractor.INSTANCE()));
        ContinuousAggregator<Integer, Long, Long> minimum = map.addAggregator(
                new LongMin<>(IdentityExtractor.INSTANCE()));
        ContinuousAggregator<Integer, Long, Long> maximum = map.addAggregator(
                new LongMax<>(IdentityExtractor.INSTANCE()));
        ContinuousAggregator<Integer, Long, Integer> filtered = map.addAggregator(filter, new Count<>());

        Random random = new Random(12978543L);
        try
            {
            for (int i = 0; i < 500; i++)
                {
                int key = random.nextInt(80);
                if (random.nextInt(4) == 0)
                    {
                    map.remove(key);
                    }
                else
                    {
                    map.put(key, (long) random.nextInt(40));
                    }

                if (i % 17 == 0)
                    {
                    assertEquals(map.aggregate(new Count<>()), count.aggregate());
                    assertEquals(map.aggregate(new LongSum<>(IdentityExtractor.INSTANCE())),
                            sum.aggregate());
                    assertEquals(map.aggregate(new LongMin<>(IdentityExtractor.INSTANCE())),
                            minimum.aggregate());
                    assertEquals(map.aggregate(new LongMax<>(IdentityExtractor.INSTANCE())),
                            maximum.aggregate());
                    assertEquals(map.aggregate(filter, new Count<>()), filtered.aggregate());
                    }
                }
            }
        finally
            {
            map.removeAggregator(filtered);
            map.removeAggregator(maximum);
            map.removeAggregator(minimum);
            map.removeAggregator(sum);
            map.removeAggregator(count);
            }
        }

    @Test
    public void shouldRemainUsableDuringConcurrentMutationAndRegistration()
            throws Exception
        {
        NamedMap<Integer, Long> map = getNamedCache("dist-continuous-aggregation-concurrent");
        waitForBalanced(map.getService());
        map.clear();
        for (int i = 0; i < 200; i++)
            {
            map.put(i, (long) i);
            }

        ContinuousAggregator<Integer, Long, Integer> count = map.addAggregator(new Count<>());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try
            {
            Future<?> writer = executor.submit(() ->
                {
                start.await();
                for (int i = 0; i < 1000; i++)
                    {
                    int key = i % 250;
                    if ((i & 7) == 0)
                        {
                        map.remove(key);
                        }
                    else
                        {
                        map.put(key, (long) i);
                        }
                    }
                return null;
                });
            Future<?> registrar = executor.submit(() ->
                {
                start.await();
                for (int i = 0; i < 25; i++)
                    {
                    ContinuousAggregator<Integer, Long, Long> transientSum = map.addAggregator(
                            new LongSum<>(IdentityExtractor.INSTANCE()));
                    assertTrue(transientSum.aggregate() != null);
                    map.removeAggregator(transientSum);
                    }
                return null;
                });

            start.countDown();
            while (!writer.isDone() || !registrar.isDone())
                {
                assertTrue(count.aggregate() >= 0);
                }
            writer.get(30, TimeUnit.SECONDS);
            registrar.get(30, TimeUnit.SECONDS);
            assertEquals(map.aggregate(new Count<>()), count.aggregate());
            }
        finally
            {
            executor.shutdownNow();
            map.removeAggregator(count);
            }
        }

    @Test
    public void shouldInvalidateHandleWhenCacheIsDestroyed()
        {
        NamedMap<Integer, Long> map = getNamedCache("dist-continuous-aggregation-destroy");
        waitForBalanced(map.getService());
        map.clear();
        ContinuousAggregator<Integer, Long, Integer> count = map.addAggregator(new Count<>());
        assertEquals(Integer.valueOf(0), count.aggregate());

        map.destroy();
        assertThrows(RuntimeException.class, count::aggregate);
        }
    }
