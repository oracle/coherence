/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;

import com.tangosol.coherence.component.net.management.model.localModel.CacheModel;

import com.tangosol.net.CacheService;
import com.tangosol.net.ServiceInfo;

import com.tangosol.util.Base;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests for concurrent cache statistics (Bug 40267900).
 *
 * @author Aleks Seovic  2026.11.06
 * @since 27.01
 */
public class SimpleCacheStatisticsTest
    {
    @Test
    public void shouldNotLoseConcurrentRegistrations()
            throws Exception
        {
        SimpleCacheStatistics stats = new SimpleCacheStatistics();
        runConcurrently(i ->
            {
            for (int j = 0; j < ITERATIONS; j++)
                {
                stats.registerHit();
                stats.registerHit(Long.MAX_VALUE);
                stats.registerHits(3, 0);
                stats.registerMiss();
                stats.registerMiss(Long.MAX_VALUE);
                stats.registerMisses(3, 0);
                stats.registerPut(0);
                stats.registerPuts(3, 0);
                stats.registerCachePrune(0);
                }
            });

        long c = (long) THREADS * ITERATIONS;
        assertEquals(5 * c, stats.getCacheHits());
        assertEquals(5 * c, stats.getCacheMisses());
        assertEquals(10 * c, stats.getTotalGets());
        assertEquals(4 * c, stats.getTotalPuts());
        assertEquals(c, stats.getCachePrunes());
        assertEquals(0, stats.getTotalGetsMillis());
        assertEquals(0, stats.getTotalPutsMillis());
        assertEquals(0, stats.getCachePrunesMillis());
        assertEquals(0.5, stats.getHitProbability(), 0.0);
        }

    @Test
    public void shouldNotLoseConcurrentTimingUpdates()
            throws Exception
        {
        SimpleCacheStatistics stats = new SimpleCacheStatistics();
        long lBefore = Base.getSafeTimeMillis();
        runConcurrently(i ->
            {
            for (int j = 0; j < ITERATIONS; j++)
                {
                stats.registerHit(1);
                stats.registerHits(3, 1);
                stats.registerMiss(1);
                stats.registerMisses(3, 1);
                stats.registerPut(1);
                stats.registerPuts(3, 1);
                stats.registerCachePrune(1);
                stats.registerIncrementalCachePrune(1);
                }
            });
        long lAfter = Base.getSafeTimeMillis();
        long c = (long) THREADS * ITERATIONS;

        // each registration contributes elapsed time once, including bulk calls
        assertTiming(2 * c, lBefore, lAfter, stats.getCacheHitsMillis());
        assertTiming(2 * c, lBefore, lAfter, stats.getCacheMissesMillis());
        assertTiming(2 * c, lBefore, lAfter, stats.getTotalPutsMillis());
        assertTiming(2 * c, lBefore, lAfter, stats.getCachePrunesMillis());
        assertEquals(4 * c, stats.getCacheHits());
        assertEquals(4 * c, stats.getCacheMisses());
        assertEquals(4 * c, stats.getTotalPuts());
        assertEquals(c, stats.getCachePrunes());
        assertEquals(stats.getCacheHitsMillis() + stats.getCacheMissesMillis(), stats.getTotalGetsMillis());
        assertEquals((double) stats.getTotalGetsMillis() / stats.getTotalGets(), stats.getAverageGetMillis(), 0.0);
        assertEquals((double) stats.getCacheHitsMillis() / stats.getCacheHits(), stats.getAverageHitMillis(), 0.0);
        assertEquals((double) stats.getCacheMissesMillis() / stats.getCacheMisses(), stats.getAverageMissMillis(), 0.0);
        assertEquals((double) stats.getTotalPutsMillis() / stats.getTotalPuts(), stats.getAveragePutMillis(), 0.0);
        assertEquals((double) stats.getCachePrunesMillis() / stats.getCachePrunes(), stats.getAveragePruneMillis(), 0.0);
        }

    @Test
    public void shouldCountConcurrentLocalCacheAccess()
            throws Exception
        {
        LocalCache cache = new LocalCache();
        cache.put(1, 1);
        CacheStatistics stats = cache.getCacheStatistics();
        stats.resetHitStatistics();
        runConcurrently(i ->
            {
            for (int j = 0; j < ITERATIONS; j++)
                {
                assertEquals(Integer.valueOf(1), cache.get(1));
                assertEquals(null, cache.get(2));
                }
            });
        assertGets(stats);
        assertManagementTotals(cache);
        }

    @Test
    public void shouldCountConcurrentNearCacheAccess()
            throws Exception
        {
        LocalCache front = new LocalCache();
        LocalCache back  = new LocalCache();
        back.put(1, 1);
        CacheService service = mock(CacheService.class);
        ServiceInfo info = mock(ServiceInfo.class);
        when(service.getInfo()).thenReturn(info);
        when(info.getServiceName()).thenReturn("cache-statistics-test");
        NearCache<Integer, Integer> cache = new NearCache<>(front,
                new WrapperNamedCache<Integer, Integer>(back, "near-statistics", service), CachingMap.LISTEN_NONE);
        cache.get(1);
        CacheStatistics stats = cache.getCacheStatistics();
        stats.resetHitStatistics();
        front.getCacheStatistics().resetHitStatistics();
        back.getCacheStatistics().resetHitStatistics();
        runConcurrently(i ->
            {
            for (int j = 0; j < ITERATIONS; j++)
                {
                assertEquals(Integer.valueOf(1), cache.get(1));
                assertEquals(null, cache.get(2));
                }
            });
        assertGets(stats);
        long c = (long) THREADS * ITERATIONS;
        assertEquals(c, front.getCacheStatistics().getCacheHits());
        assertEquals(2 * c, front.getCacheStatistics().getCacheMisses());
        assertEquals(c, back.getCacheStatistics().getCacheMisses());
        assertManagementTotals(cache);
        }

    @Test
    public void shouldResetAllCountersAndAverages()
            throws Exception
        {
        SimpleCacheStatistics stats = new SimpleCacheStatistics();
        stats.registerHit(1);
        stats.registerMiss(1);
        stats.registerPut(1);
        stats.registerCachePrune(1);
        stats.resetHitStatistics();
        assertEmpty(stats);

        runConcurrently(i ->
            {
            for (int j = 0; j < ITERATIONS; j++)
                {
                stats.registerHit();
                }
            });
        assertEquals((long) THREADS * ITERATIONS, stats.getTotalGets());
        }

    @Test
    public void shouldAllowRegistrationsRacingWithReset()
            throws Exception
        {
        SimpleCacheStatistics stats = new SimpleCacheStatistics();
        runConcurrently(i ->
            {
            for (int j = 0; j < ITERATIONS; j++)
                {
                if (i == 0)
                    {
                    stats.resetHitStatistics();
                    }
                else
                    {
                    stats.registerHit();
                    stats.registerMiss();
                    stats.registerPut(0);
                    stats.registerCachePrune(0);
                    }
                }
            });
        long cMax = (long) (THREADS - 1) * ITERATIONS;
        assertTrue(stats.getCacheHits() >= 0 && stats.getCacheHits() <= cMax);
        assertTrue(stats.getCacheMisses() >= 0 && stats.getCacheMisses() <= cMax);
        assertTrue(stats.getTotalPuts() >= 0 && stats.getTotalPuts() <= cMax);
        assertTrue(stats.getCachePrunes() >= 0 && stats.getCachePrunes() <= cMax);
        stats.resetHitStatistics();
        assertEmpty(stats);
        stats.registerHit();
        assertEquals(1, stats.getTotalGets());
        }

    @Test
    public void shouldPreserveJavaSerialization()
            throws Exception
        {
        assertEquals(4235773659525587475L,
                ObjectStreamClass.lookup(SimpleCacheStatistics.class).getSerialVersionUID());
        SimpleCacheStatistics stats = new SimpleCacheStatistics();
        stats.registerHits(3, 1);
        stats.registerMisses(2, 1);
        stats.registerPuts(4, 1);
        stats.registerCachePrune(1);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes))
            {
            out.writeObject(stats);
            }
        SimpleCacheStatistics copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())))
            {
            copy = (SimpleCacheStatistics) in.readObject();
            }
        assertEquals(stats.getTotalGets(), copy.getTotalGets());
        assertEquals(stats.getTotalGetsMillis(), copy.getTotalGetsMillis());
        assertEquals(stats.getTotalPuts(), copy.getTotalPuts());
        assertEquals(stats.getTotalPutsMillis(), copy.getTotalPutsMillis());
        assertEquals(stats.getCachePrunes(), copy.getCachePrunes());
        assertEquals(stats.getCachePrunesMillis(), copy.getCachePrunesMillis());
        copy.registerHit();
        assertEquals(6, copy.getTotalGets());
        }

    private static void assertGets(CacheStatistics stats)
        {
        long c = (long) THREADS * ITERATIONS;
        assertEquals(c, stats.getCacheHits());
        assertEquals(c, stats.getCacheMisses());
        assertEquals(2 * c, stats.getTotalGets());
        }

    private static void assertManagementTotals(java.util.Map cache)
        {
        CacheModel model = new CacheModel();
        model.set_Map(cache);
        long c = (long) THREADS * ITERATIONS;
        assertEquals(c, model.getCacheHits());
        assertEquals(c, model.getCacheMisses());
        assertEquals(2 * c, model.getTotalGets());
        model.resetStatistics();
        assertEquals(0, model.getTotalGets());
        }

    private static void assertTiming(long cCalls, long lBefore, long lAfter, long cMillis)
        {
        assertTrue("lost timing updates: " + cMillis, cMillis >= cCalls * (lBefore - 1));
        assertTrue("excess timing updates: " + cMillis, cMillis <= cCalls * (lAfter - 1));
        }

    private static void assertEmpty(SimpleCacheStatistics stats)
        {
        assertEquals(0, stats.getCacheHits());
        assertEquals(0, stats.getCacheMisses());
        assertEquals(0, stats.getTotalGets());
        assertEquals(0, stats.getCacheHitsMillis());
        assertEquals(0, stats.getCacheMissesMillis());
        assertEquals(0, stats.getTotalGetsMillis());
        assertEquals(0, stats.getTotalPuts());
        assertEquals(0, stats.getTotalPutsMillis());
        assertEquals(0, stats.getCachePrunes());
        assertEquals(0, stats.getCachePrunesMillis());
        assertEquals(0.0, stats.getAverageGetMillis(), 0.0);
        assertEquals(0.0, stats.getAverageHitMillis(), 0.0);
        assertEquals(0.0, stats.getAverageMissMillis(), 0.0);
        assertEquals(0.0, stats.getAveragePutMillis(), 0.0);
        assertEquals(0.0, stats.getAveragePruneMillis(), 0.0);
        assertEquals(0.0, stats.getHitProbability(), 0.0);
        }

    private static void runConcurrently(IntConsumer action)
            throws Exception
        {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try
            {
            for (int i = 0; i < THREADS; i++)
                {
                final int nThread = i;
                futures.add(executor.submit(() ->
                    {
                    ready.countDown();
                    if (!start.await(30, TimeUnit.SECONDS))
                        {
                        throw new IllegalStateException("start timed out");
                        }
                    action.accept(nThread);
                    return null;
                    }));
                }
            assertTrue("workers did not start", ready.await(30, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> future : futures)
                {
                future.get(30, TimeUnit.SECONDS);
                }
            }
        finally
            {
            start.countDown();
            executor.shutdownNow();
            assertTrue("workers did not stop", executor.awaitTermination(30, TimeUnit.SECONDS));
            }
        }

    private static final int THREADS = 8;
    private static final int ITERATIONS = 25_000;
    }
