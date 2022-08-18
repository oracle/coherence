/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.caffeine;

import com.google.common.collect.Iterables;

import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.cache.CacheEvent.TransformationState;
import com.tangosol.net.cache.ConfigurableCacheMap;
import com.tangosol.net.cache.ConfigurableCacheMap.UnitCalculator;
import com.tangosol.net.cache.OldCache;

import com.tangosol.util.Base;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;

import java.time.Duration;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static com.tangosol.util.MapEvent.ENTRY_DELETED;
import static com.tangosol.util.MapEvent.ENTRY_INSERTED;
import static com.tangosol.util.MapEvent.ENTRY_UPDATED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
public final class CaffeineCacheTest
    {
    @ParameterizedTest
    @MethodSource("caches")
    public void mapListener(ConfigurableCacheMap cache)
        {
        var created = ArgumentCaptor.forClass(MapEvent.class);
        var updated = ArgumentCaptor.forClass(MapEvent.class);
        var deleted = ArgumentCaptor.forClass(MapEvent.class);
        var listener = Mockito.mock(MapListener.class);
        cache.addMapListener(listener);

        cache.put(1, 2);
        verify(listener).entryInserted(created.capture());
        checkMapEvent(created.getValue(), new CacheEvent<>(cache, ENTRY_INSERTED, 1, null, 2, false));

        cache.put(1, 3);
        verify(listener).entryUpdated(updated.capture());
        checkMapEvent(updated.getValue(), new CacheEvent<>(cache, ENTRY_UPDATED, 1, 2, 3, false));

        cache.replace(1, 4);
        verify(listener, times(2)).entryUpdated(updated.capture());
        checkMapEvent(Iterables.getLast(updated.getAllValues()),
                      new CacheEvent<>(cache, ENTRY_UPDATED, 1, 3, 4, false));

        cache.remove(1);
        verify(listener).entryDeleted(deleted.capture());
        checkMapEvent(deleted.getValue(), new CacheEvent<>(cache, ENTRY_DELETED, 1, 4, null, false));
        }

    @ParameterizedTest
    @MethodSource("caches")
    public void smallUnits(ConfigurableCacheMap cache)
        {
        cache.setUnitCalculator(new FixedCalculator(10));
        cache.setHighUnits(100);
        cache.setUnitFactor(2);

        cache.put(1, 2);
        assertThat(cache.getUnits(), is(5));

        cache.setUnitCalculator(new FixedCalculator(20));
        assertThat(cache.getCacheEntry(1).getUnits(), is(20));
        assertThat(cache.getUnits(), is(10));
        }

    @ParameterizedTest
    @MethodSource("caches")
    public void largeUnits(ConfigurableCacheMap cache)
        {
        int cSize = 4;
        int nUnitFactor = 8;
        int cMaximum = Integer.MAX_VALUE;

        cache.setHighUnits(cMaximum);
        cache.setUnitFactor(nUnitFactor);
        cache.setUnitCalculator(new FixedCalculator(cMaximum));
        for (int i = 0; i < cSize; i++)
            {
            cache.put(i, i);
            assertThat(cache.getCacheEntry(i).getUnits(), is(cMaximum));
            }

        int cExpectedUnits = (int) ((((long) cSize * cMaximum) + nUnitFactor - 1) / nUnitFactor);
        assertThat(cache.getUnits(), is(cExpectedUnits));
        }

    @ParameterizedTest
    @MethodSource("caches")
    public void statistics(ConfigurableCacheMap cache)
        {
        var stats = (cache instanceof CaffeineCache)
                    ? ((CaffeineCache) cache).getCacheStatistics()
                    : ((OldCache) cache).getCacheStatistics();
        assertThat(stats.getCacheMisses(), is(0L));
        assertThat(stats.getTotalPuts(), is(0L));
        assertThat(stats.getCacheHits(), is(0L));

        cache.get(1);
        assertThat(stats.getCacheMisses(), is(1L));

        cache.put(1, 2);
        assertThat(stats.getTotalPuts(), is(1L));

        cache.get(1);
        assertThat(stats.getCacheHits(), is(1L));
        assertThat(stats.getHitProbability(), is(0.5));
        }

    @ParameterizedTest
    @MethodSource("caches")
    public void eviction(ConfigurableCacheMap cache)
        {
        var evicted = ArgumentCaptor.forClass(MapEvent.class);
        var listener = Mockito.mock(MapListener.class);
        cache.addMapListener(listener);

        int inserts = 1_000;
        int maximumSize = 100;
        cache.setLowUnits(maximumSize);
        cache.setHighUnits(maximumSize);
        for (int i = 0; i < 1_000; i++)
            {
            cache.put(i, i);
            }

        assertThat(cache.size(), lessThanOrEqualTo(maximumSize));
        verify(listener, atLeast(inserts - maximumSize)).entryDeleted(evicted.capture());
        for (var event : evicted.getAllValues())
            {
            if (event instanceof CacheEvent)
                {
                assertThat(((CacheEvent) event).isSynthetic(), is(true));
                assertThat(((CacheEvent) event).isExpired(), is(false));
                }
            assertThat(event.getId(), is(ENTRY_DELETED));
            }
        }

    @ParameterizedTest
    @MethodSource("caches")
    public void expiration(ConfigurableCacheMap cache)
        {
        var expired = ArgumentCaptor.forClass(MapEvent.class);
        var listener = Mockito.mock(MapListener.class);
        cache.addMapListener(listener);

        cache.setExpiryDelay(1_000);
        assertThat(cache.getExpiryDelay(), is(1_000));

        cache.put(1, 2);

        // Caffeine has ~1s (2^30 ns) maximum delay
        sleepUninterruptibly(Duration.ofMillis(1_100));
        assertThat(cache.containsKey(1), is(false));

        cache.evict();
        verify(listener).entryDeleted(expired.capture());
        checkMapEvent(expired.getValue(), new CacheEvent<>(cache, ENTRY_DELETED,
                                                           1, 2, null, true, TransformationState.TRANSFORMABLE, false, true));
        }

    @ParameterizedTest
    @MethodSource("caches")
    public void evict(ConfigurableCacheMap cache)
        {
        var expired = ArgumentCaptor.forClass(MapEvent.class);
        var listener = Mockito.mock(MapListener.class);
        cache.addMapListener(listener);

        cache.put(1, 2);
        cache.evict(1);
        assertThat(cache.containsKey(1), is(false));

        if (cache instanceof CaffeineCache)
            {
            // Caffeine has ~1s (2^30 ns) maximum delay
            sleepUninterruptibly(Duration.ofMillis(1_100));
            cache.evict();
            }

        verify(listener).entryDeleted(expired.capture());
        checkMapEvent(expired.getValue(), new CacheEvent<>(cache, ENTRY_DELETED,
                                                           1, 2, null, true, TransformationState.TRANSFORMABLE, false, true));
        }

    @ParameterizedTest
    @MethodSource("caches")
    public void put_expires(ConfigurableCacheMap cache)
        {
        var expired = ArgumentCaptor.forClass(MapEvent.class);
        var listener = Mockito.mock(MapListener.class);
        cache.addMapListener(listener);

        cache.setExpiryDelay(5_000);
        cache.put(1, 2, 1_000);

        // Caffeine has ~1s (2^30 ns) maximum delay
        sleepUninterruptibly(Duration.ofMillis(1_100));
        assertThat(cache.containsKey(1), is(false));

        cache.evict();
        verify(listener).entryDeleted(expired.capture());
        checkMapEvent(expired.getValue(), new CacheEvent<>(cache, ENTRY_DELETED,
                                                           1, 2, null, true, TransformationState.TRANSFORMABLE, false, true));
        }

    @ParameterizedTest
    @MethodSource("caches")
    public void nextExpiryTime(ConfigurableCacheMap cache)
        {
        assertThat(cache.getNextExpiryTime(), is(0L));
        cache.setExpiryDelay(5_000);

        cache.put(1, 1);
        cache.put(2, 2, 8_000);
        assertThat(cache.getNextExpiryTime() - Base.getSafeTimeMillis(), lessThanOrEqualTo(5_000L));

        cache.put(3, 3, 3_000);
        assertThat(cache.getNextExpiryTime() - Base.getSafeTimeMillis(), lessThanOrEqualTo(3_000L));
        }

    public static List<Arguments> caches()
        {
        return List.of(
                arguments(named("OldCache", new OldCache())),
                arguments(named("Caffeine", new CaffeineCache())));
        }

    private static void checkMapEvent(MapEvent<?, ?> actual, CacheEvent<?, ?> expected)
        {
        assertThat(actual.getNewEntry(), is(expected.getNewEntry()));
        assertThat(actual.getOldValue(), is(expected.getOldValue()));
        assertThat(actual.getId(), is(expected.getId()));
        if (actual instanceof CacheEvent)
            {
            assertThat(((CacheEvent) actual).isSynthetic(), is(expected.isSynthetic()));
            assertThat(((CacheEvent) actual).isExpired(), is(expected.isExpired()));
            }
        }

    private static final class FixedCalculator
            implements UnitCalculator
        {
        private final int f_nWeight;

        FixedCalculator(int nWeight)
            {
            this.f_nWeight = nWeight;
            }

        @Override
        public String getName()
            {
            return null;
            }

        @Override
        public int calculateUnits(Object oKey, Object oValue)
            {
            return f_nWeight;
            }
        }
    }
