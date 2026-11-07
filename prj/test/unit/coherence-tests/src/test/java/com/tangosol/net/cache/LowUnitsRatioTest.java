/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;

import com.oracle.coherence.testing.SystemPropertyResource;

import com.tangosol.coherence.config.unit.LowUnits;
import com.tangosol.coherence.config.unit.Units;

import com.tangosol.io.BinaryStore;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.partition.ObservableSplittingBackingCache;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Verifies low-watermark configuration, eviction and partitioned resizing.
 *
 * @author Aleks Seovic  2026.11.06
 * @since 27.01
 */
public class LowUnitsRatioTest
    {
    @Test
    public void shouldParsePercentagesAndPreserveAbsoluteUnits()
        {
        assertEquals(0.9, new LowUnits("90%").getRatio(), 0.0);
        assertEquals(0.905, new LowUnits("90.5%").getRatio(), 0.0);
        assertEquals(0.001, new LowUnits("0.1%").getRatio(), 0.0);
        assertEquals(123, new LowUnits("123").getUnitCount());
        assertEquals(104857600, new LowUnits("100M").getUnitCount());
        assertEquals(-1.0, new LowUnits("0").getRatio(), 0.0);
        assertThrows(IllegalArgumentException.class, () -> new Units("90%"));
        }

    @Test
    public void shouldRejectMalformedPercentages()
        {
        for (String s : new String[] {"0%", "100%", "101%", "-1%", "+90%", "NaN%",
                "Infinity%", "9e1%", "90%junk", "90%%", ".5%", "90.%"})
            {
            assertThrows(s, IllegalArgumentException.class, () -> new LowUnits(s));
            }
        }

    @Test
    public void shouldUseExistingDefaultWhenPropertyIsUnset()
        {
        String previous = System.clearProperty(PROPERTY);
        try
            {
            assertEquals(0.80, LocalCache.getDefaultPruneLevel(), 0.0);
            assertEquals(800, new LocalCache(1000, 0).getLowUnits());
            }
        finally
            {
            if (previous != null)
                {
                System.setProperty(PROPERTY, previous);
                }
            }
        }

    @Test
    public void shouldReadDefaultForNewCachesOnly()
        {
        try (var ignored = new SystemPropertyResource(PROPERTY, "0.90"))
            {
            var cache = new LocalCache(1000, 0);
            assertEquals(900, cache.getLowUnits());
            var loader = mock(CacheLoader.class);
            var loaded = new LocalCache(1000, 0, loader);
            assertSame(loader, loaded.getCacheLoader());
            assertEquals(900, loaded.getLowUnits());
            loaded.setHighUnits(2000);
            assertEquals(1800, loaded.getLowUnits());
            System.setProperty(PROPERTY, "0.60");
            cache.setHighUnits(2000);
            assertEquals(1800, cache.getLowUnits());
            assertEquals(600, new LocalCache(1000, 0).getLowUnits());
            assertEquals(700, new OldCache(1000, 0, 0.7).getLowUnits());
            }
        }

    @Test
    public void shouldIgnoreInvalidDefaultRatios()
        {
        for (String s : new String[] {"0", "1", "-1", "1.1", "NaN", "Infinity", "invalid", "90%"})
            {
            try (var ignored = new SystemPropertyResource(PROPERTY, s))
                {
                assertEquals(s, 800, new LocalCache(1000, 0).getLowUnits());
                }
            }
        }

    @Test
    public void shouldRetainRatioAcrossResizeAndPrune()
        {
        var cache = new LocalCache(100, 0);
        cache.setLowUnitsRatio(0.9);
        cache.setEvictionType(LocalCache.EVICTION_POLICY_LRU);
        for (int i = 0; i <= 100; i++)
            {
            cache.put(i, i);
            }
        assertEquals(90, cache.size());
        cache.setHighUnits(200);
        assertEquals(180, cache.getLowUnits());
        cache.setHighUnits(200);
        assertEquals(180, cache.getLowUnits());
        cache.setLowUnits(123);
        assertEquals(123, cache.getLowUnits());
        assertTrue(cache.getLowUnitsRatio() < 0.0);
        }

    @Test
    public void shouldHandleUnitFactorsTinyAndUnlimitedCaches()
        {
        var cache = new LocalCache(100, 0);
        cache.setUnitFactor(1024);
        cache.setLowUnitsRatio(0.9);
        assertEquals(90, cache.getLowUnits());
        cache.setHighUnits(1);
        assertEquals(0, cache.getLowUnits());
        cache.setHighUnits(0);
        assertEquals(0, cache.getHighUnits());
        assertEquals(0, cache.getLowUnits());
        var tiny = new LocalCache(1, 0);
        tiny.setLowUnitsRatio(0.9);
        assertEquals(0, tiny.getLowUnits());
        }

    @Test
    public void shouldValidateRatioApi()
        {
        var cache = new LocalCache(100, 0);
        for (double d : new double[] {0, -1, 1, Double.NaN, Double.POSITIVE_INFINITY})
            {
            assertThrows(IllegalArgumentException.class, () -> cache.setLowUnitsRatio(d));
            }
        cache.setLowUnitsRatio(0.999);
        assertEquals(99, cache.getLowUnits());
        }

    @Test
    public void shouldApplyDefaultAndExplicitRatioToSerializationCache()
        {
        try (var ignored = new SystemPropertyResource(PROPERTY, "0.90"))
            {
            var store = mock(BinaryStore.class);
            when(store.keys()).thenReturn(java.util.Collections.emptyIterator());
            var cache = new SerializationCache(store, 1000);
            assertEquals(900, cache.getLowUnits());
            cache.setLowUnitsRatio(0.6);
            cache.setHighUnits(2000);
            assertEquals(1200, cache.getLowUnits());
            cache.setLowUnits(321);
            assertEquals(321, cache.getLowUnits());
            assertTrue(cache.getLowUnitsRatio() < 0);
            }
        }

    @Test
    public void shouldPreserveRatioAndFairSharesAcrossPartitionChanges()
        {
        var cache = partitionedCache(0.9);
        assertEquals(900, cache.getLowUnits());
        cache.createPartition(0);
        cache.createPartition(1);
        assertEquals(0.9, cache.getLowUnitsRatio(), 0.0);
        cache.setHighUnits(2000);
        assertEquals(1800, cache.getLowUnits());
        assertEquals(900, ((ConfigurableCacheMap) cache.getPartitionMap(0)).getLowUnits());
        assertEquals(900, ((ConfigurableCacheMap) cache.getPartitionMap(1)).getLowUnits());
        cache.setHighUnits(2000);
        assertEquals(900, ((ConfigurableCacheMap) cache.getPartitionMap(0)).getLowUnits());
        cache.destroyPartition(1);
        assertEquals(1800, ((ConfigurableCacheMap) cache.getPartitionMap(0)).getLowUnits());
        cache.setLowUnits(500);
        cache.setHighUnits(3000);
        assertEquals(500, cache.getLowUnits());
        assertTrue(cache.getLowUnitsRatio() < 0);
        }

    @Test
    public void shouldPropagateRatioEvenWhenRoundedThresholdDoesNotChange()
        {
        var cache = partitionedCache(0.9);
        cache.createPartition(0);
        cache.setLowUnitsRatio(0.9001);
        var partition = (ConfigurableCacheMap) cache.getPartitionMap(0);
        partition.setHighUnits(10000);
        assertEquals(9001, partition.getLowUnits());
        // restore the allocation before changing the aggregate capacity
        partition.setHighUnits(1000);
        cache.setHighUnits(0);
        assertEquals(0, cache.getLowUnits());
        assertEquals(0, partition.getLowUnits());
        }

    @Test
    public void shouldDelegateRatiosThroughTopicBackingMaps()
        {
        var local = new LocalCache(1000, 0);
        var wrapper = new TopicMap(local);
        wrapper.setLowUnitsRatio(0.9);
        wrapper.setHighUnits(2000);
        assertEquals(0.9, wrapper.getLowUnitsRatio(), 0.0);
        assertEquals(1800, wrapper.getLowUnits());
        }

    private static class TopicMap
            extends com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMap
        {
        TopicMap(LocalCache cache)
            {
            super(cache);
            }
        }

    private ObservableSplittingBackingCache partitionedCache(double dflRatio)
        {
        var manager = mock(BackingMapManager.class);
        var context = mock(BackingMapManagerContext.class);
        var service = mock(PartitionedService.class, withSettings().extraInterfaces(CacheService.class));
        when(manager.getContext()).thenReturn(context);
        when(context.getCacheService()).thenReturn((CacheService) service);
        when(service.getPartitionCount()).thenReturn(257);
        when(manager.instantiateBackingMap(anyString())).thenAnswer(invocation ->
            {
            var map = new LocalCache(1000, 0);
            map.setLowUnitsRatio(dflRatio);
            return map;
            });
        return new ObservableSplittingBackingCache(manager, "ratio-test");
        }

    private static final String PROPERTY = "coherence.local.cache.default.prune";
    }
