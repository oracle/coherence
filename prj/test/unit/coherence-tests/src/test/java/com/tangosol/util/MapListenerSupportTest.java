/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import com.tangosol.net.cache.CacheEvent.TransformationState;

import com.tangosol.util.filter.InKeySetFilter;
import com.tangosol.util.filter.MapEventFilter;
import com.tangosol.util.listener.SimpleMapListener;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Regression tests for key-set listener selection with matching filter metadata.
 *
 * @author phf  2026.10.21
 * @since 26.10
 */
public class MapListenerSupportTest
    {
    @Test
    public void shouldSelectKeySetListenerWithWholeCacheFilter()
        {
        assertMatchingFilter(null);
        }

    @Test
    public void shouldSelectKeySetListenerWithEventFilter()
        {
        assertMatchingFilter(new MapEventFilter(MapEventFilter.E_ALL));
        }

    @Test
    public void shouldSelectKeySetListenerWithEmptyFilterList()
        {
        MapListenerSupport support = new MapListenerSupport();
        MapListener        target  = new SimpleMapListener<>();
        support.addListener(target, keyFilter(1), false);

        assertListeners(support.collectListeners(event(1, new Filter[0])), target);
        assertListeners(support.collectListeners(event(2, new Filter[0])));
        }

    @Test
    public void shouldSelectKeySetListenerWithoutFilterMetadata()
        {
        MapListenerSupport support = new MapListenerSupport();
        MapListener        target  = new SimpleMapListener<>();
        support.addListener(target, keyFilter(1), false);

        assertListeners(support.collectListeners(new MapEvent(new ObservableHashMap(),
                MapEvent.ENTRY_INSERTED, 1, null, "new")), target);
        }

    @Test
    public void shouldUseConvertedEventKey()
        {
        MapListenerSupport support = new MapListenerSupport();
        MapListener        target  = new SimpleMapListener<>();
        MapListener        other   = new SimpleMapListener<>();
        support.addListener(target, keyFilter(1), false);
        support.addListener(other, (Filter) null, false);

        MapEvent converted = MapListenerSupport.convertEvent(event("1", new Filter[] {null}),
                new ObservableHashMap(), key -> Integer.valueOf((String) key), value -> value);

        assertListeners(support.collectListeners(converted), target, other);
        }

    @Test
    public void shouldExcludeKeySetListenerFromTransformedEvent()
        {
        MapListenerSupport support = new MapListenerSupport();
        MapListener        target  = new SimpleMapListener<>();
        MapListener        other   = new SimpleMapListener<>();
        support.addListener(target, keyFilter(1), false);
        support.addListener(other, (Filter) null, false);

        MapEvent transformed = new MapListenerSupport.FilterEvent(new ObservableHashMap(),
                MapEvent.ENTRY_INSERTED, 1, null, "transformed", false,
                TransformationState.TRANSFORMED, new Filter[] {null});

        assertListeners(support.collectListeners(transformed), other);
        assertListeners(support.collectListeners(MapListenerSupport.convertEvent(transformed,
                new ObservableHashMap(), null, null)), other);
        }

    @Test
    public void shouldRetainServerFilterListWithoutReevaluatingOtherFilters()
        {
        MapListenerSupport support = new MapListenerSupport();
        MapListener        target  = new SimpleMapListener<>();
        MapListener        other   = new SimpleMapListener<>();
        Filter             filter  = value -> { throw new AssertionError("filter reevaluated"); };
        Filter[]           filters = {filter};
        support.addListener(target, keyFilter(1), false);
        support.addListener(other, filter, false);

        Listeners selected = support.collectListeners(event(1, filters));
        assertListeners(selected, target, other);
        assertSame(filters, selected.getFilters());
        }

    @Test
    public void shouldNotSelectUnmatchedOrdinaryFilter()
        {
        MapListenerSupport support = new MapListenerSupport();
        MapListener        target  = new SimpleMapListener<>();
        MapListener        other   = new SimpleMapListener<>();
        Filter             filter  = value -> { throw new AssertionError("unmatched filter reevaluated"); };
        support.addListener(target, keyFilter(1), false);
        support.addListener(other, filter, false);

        assertListeners(support.collectListeners(event(1, new Filter[0])), target);
        }

    @Test
    public void shouldDeduplicateOverlappingKeySetAndFilterSubscriptions()
        {
        MapListenerSupport support = new MapListenerSupport();
        MapListener        target  = new SimpleMapListener<>();
        MapListener        second  = new SimpleMapListener<>();
        Filter             keys    = keyFilter(1, 2);
        support.addListener(target, keys, false);
        support.addListener(target, keyFilter(1, 3), false);
        support.addListener(target, Integer.valueOf(1), false);
        support.addListener(target, (Filter) null, false);
        support.addListener(second, keyFilter(2), false);

        assertListeners(support.collectListeners(event(1, new Filter[] {null, keys})), target);
        assertListeners(support.collectListeners(event(2, new Filter[] {null})), target, second);
        }

    @Test
    public void shouldRemoveAndReregisterKeySetListener()
        {
        MapListenerSupport support = new MapListenerSupport();
        MapListener        target  = new SimpleMapListener<>();
        MapListener        other   = new SimpleMapListener<>();
        Filter             keys    = keyFilter(1);
        support.addListener(target, keys, false);
        support.addListener(other, (Filter) null, false);
        assertKeySetFilters(support, keys);
        assertListeners(support.collectListeners(event(1, new Filter[] {null})), target, other);

        support.removeListener(target, keys);
        assertKeySetFilters(support);
        assertListeners(support.collectListeners(event(1, new Filter[] {null})), other);
        support.addListener(target, keys, false);
        assertKeySetFilters(support, keys);
        assertListeners(support.collectListeners(event(1, new Filter[] {null})), target, other);
        support.removeListener(other, (Filter) null);
        assertListeners(support.collectListeners(event(1, new Filter[0])), target);

        support.removeListener(target, keys);
        Filter replacement = keyFilter(2);
        support.addListener(target, replacement, false);
        assertKeySetFilters(support, replacement);
        assertListeners(support.collectListeners(event(1, new Filter[0])));
        assertListeners(support.collectListeners(event(2, new Filter[0])), target);
        }

    @Test
    public void shouldRetainKeySetSubscriptionUntilLastListenerIsRemoved()
        {
        MapListenerSupport support   = new MapListenerSupport();
        MapListener        first     = new SimpleMapListener<>();
        MapListener        second    = new SimpleMapListener<>();
        MapListener        other     = new SimpleMapListener<>();
        Filter             keys      = keyFilter(1);
        Filter             otherKeys = keyFilter(2);
        support.addListener(first, keys, false);
        support.addListener(second, keys, true);
        support.addListener(other, otherKeys, false);
        support.addListener(other, (Filter) null, false);
        assertKeySetFilters(support, keys, otherKeys);
        support.removeListener(first, keys);
        assertKeySetFilters(support, keys, otherKeys);
        assertListeners(support.collectListeners(event(1, new Filter[0])), second);
        support.removeListener(second, keys);
        assertKeySetFilters(support, otherKeys);
        assertListeners(support.collectListeners(event(1, new Filter[0])));
        assertListeners(support.collectListeners(event(2, new Filter[0])), other);
        support.removeListener(other, otherKeys);
        assertKeySetFilters(support);
        assertListeners(support.collectListeners(event(2, new Filter[] {null})), other);
        }

    @Test
    public void shouldClearKeySetSubscriptions()
        {
        MapListenerSupport support = new MapListenerSupport();
        MapListener        target  = new SimpleMapListener<>();
        Filter             first   = keyFilter(1);
        Filter             second  = keyFilter(2);
        support.addListener(target, first, false);
        support.addListener(target, second, true);
        support.addListener(target, (Filter) null, false);
        assertKeySetFilters(support, first, second);
        support.clear();
        assertKeySetFilters(support);
        support.addListener(new SimpleMapListener<>(), new MapEventFilter(MapEventFilter.E_ALL), false);
        assertListeners(support.collectListeners(event(1, new Filter[0])));
        Filter replacement = keyFilter(3);
        support.addListener(target, replacement, false);
        assertKeySetFilters(support, replacement);
        assertListeners(support.collectListeners(event(1, new Filter[0])));
        assertListeners(support.collectListeners(event(3, new Filter[0])), target);
        }

    @Test
    public void shouldNotRetainRemovedKeySetFilters()
        {
        MapListenerSupport support = new MapListenerSupport();
        MapListener        target  = new SimpleMapListener<>();
        MapListener        other   = new SimpleMapListener<>();
        support.addListener(other, (Filter) null, false);

        for (int i = 0; i < 100; i++)
            {
            Filter keys = keyFilter(i);
            support.addListener(target, keys, false);
            assertKeySetFilters(support, keys);
            support.removeListener(target, keys);
            assertKeySetFilters(support);
            }

        assertListeners(support.collectListeners(event(1, new Filter[] {null})), other);
        }

    @Test
    public void shouldPreservePrimingDispatchRules()
        {
        MapListenerSupport support  = new MapListenerSupport();
        AtomicInteger      received = new AtomicInteger();
        MapListener        target   = new SimpleMapListener<>().addEventHandler(evt -> received.incrementAndGet());
        MapListener        priming  = new MapListenerSupport.WrapperPrimingListener(target);
        support.addListener(target, keyFilter(1), false);
        support.addListener(priming, keyFilter(1, 2), false);

        support.fireEvent(new MapListenerSupport.FilterEvent(new ObservableHashMap(),
                MapEvent.ENTRY_UPDATED, 1, null, "prime", true,
                TransformationState.NON_TRANSFORMABLE, true, new Filter[0]), true);

        assertEquals(1, received.get());
        }

    @Test
    public void shouldPreserveEventPartitionAndVersion()
        {
        MapListenerSupport support = new MapListenerSupport();
        AtomicInteger      received = new AtomicInteger();
        support.addListener(new SimpleMapListener<>().addEventHandler(evt ->
            {
            assertEquals(7, evt.getPartition());
            assertEquals(123L, evt.getVersion());
            received.incrementAndGet();
            }), keyFilter(1), false);

        support.fireEvent(event(1, new Filter[0]).with(7, 123L), true);
        assertEquals(1, received.get());
        }

    private void assertMatchingFilter(Filter filter)
        {
        for (boolean fFirst : new boolean[] {false, true})
            {
            for (boolean fLite : new boolean[] {false, true})
                {
                MapListenerSupport support = new MapListenerSupport();
                MapListener        target  = new SimpleMapListener<>();
                MapListener        other   = new SimpleMapListener<>();
                if (fFirst)
                    {
                    support.addListener(target, keyFilter(1, 2), fLite);
                    }
                support.addListener(other, filter, fLite);
                if (!fFirst)
                    {
                    support.addListener(target, keyFilter(1, 2), fLite);
                    }

                for (int id : new int[] {MapEvent.ENTRY_INSERTED, MapEvent.ENTRY_UPDATED, MapEvent.ENTRY_DELETED})
                    {
                    MapEvent event = new MapListenerSupport.FilterEvent(new ObservableHashMap(),
                            id, 1, "old", "new", false, TransformationState.NON_TRANSFORMABLE,
                            new Filter[] {filter});
                    assertListeners(support.collectListeners(event), target, other);
                    }
                assertListeners(support.collectListeners(event(3, new Filter[] {filter})), other);
                }
            }
        }

    private static void assertKeySetFilters(MapListenerSupport support, Filter... expected)
        {
        synchronized (support)
            {
            Set<Filter> actual = support.m_setKeySetFilters;
            assertEquals("tracked key-set subscriptions", new HashSet<>(Arrays.asList(expected)),
                    actual == null ? Collections.emptySet() : actual);
            }
        }

    private static Filter keyFilter(Integer... keys)
        {
        return new InKeySetFilter<>(null, new HashSet<>(Arrays.asList(keys)));
        }

    private static MapEvent event(Object key, Filter[] filters)
        {
        return new MapListenerSupport.FilterEvent(new ObservableHashMap(), MapEvent.ENTRY_INSERTED,
                key, null, "new", false, TransformationState.NON_TRANSFORMABLE, filters);
        }

    private static void assertListeners(Listeners actual, MapListener... expected)
        {
        Set<?> selected = new HashSet<>(Arrays.asList(actual.listeners()));
        assertEquals(expected.length, actual.getListenerCount());
        assertEquals(new HashSet<>(Arrays.asList(expected)), selected);
        }
    }
