/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net;

import com.tangosol.net.ContinuousAggregator;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.function.Remote;

import java.util.Collection;
import java.util.Map;

import java.util.Objects;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default map-bound {@link ContinuousAggregator} implementation.
 *
 * @param <K>  the type of the map entry keys
 * @param <V>  the type of the map entry values
 * @param <R>  the type of the final aggregation result
 *
 * @author Aleks Seovic  2026.09.06
 * @since 26.10
 */
public final class DefaultContinuousAggregator<K, V, R>
        implements ContinuousAggregator<K, V, R>
    {
    /**
     * Register a definition and create its map-bound handle.
     *
     * @param map         the map that owns the registration
     * @param filter      the filter selecting entries to aggregate
     * @param aggregator  the streaming aggregator definition
     *
     * @return a handle for the active registration
     */
    public static <K, V, R> ContinuousAggregator<K, V, R> create(
            InvocableMap<K, V> map,
            Filter<?> filter,
            InvocableMap.StreamingAggregator<? super K, ? super V, ?, R> aggregator)
        {
        Objects.requireNonNull(map, "map cannot be null");

        ContinuousAggregationDefinition definition =
                new ContinuousAggregationDefinition(filter, aggregator);

        if (!(map instanceof ContinuousAggregationSupport))
            {
            throw new UnsupportedOperationException("continuous aggregation is not supported by "
                    + map.getClass().getName());
            }

        ContinuousAggregationSupport support = (ContinuousAggregationSupport) map;
        definition = Objects.requireNonNull(
                support.prepareContinuousAggregation(definition),
                "prepared continuous aggregation definition cannot be null");
        support.registerContinuousAggregation(definition);

        return new DefaultContinuousAggregator<>(map, support, definition);
        }

    /**
     * Remove a handle from its owning map.
     *
     * @param map         the map requesting removal
     * @param aggregator  the handle to remove
     */
    public static void remove(InvocableMap<?, ?> map, ContinuousAggregator<?, ?, ?> aggregator)
        {
        Objects.requireNonNull(map, "map cannot be null");
        Objects.requireNonNull(aggregator, "aggregator cannot be null");

        if (!(aggregator instanceof DefaultContinuousAggregator))
            {
            throw new IllegalArgumentException("continuous aggregator was not created by this map");
            }

        ((DefaultContinuousAggregator<?, ?, ?>) aggregator).removeFrom(map);
        }

    /**
     * Construct a map-bound continuous aggregation handle.
     */
    private DefaultContinuousAggregator(InvocableMap<K, V>              map,
                                        ContinuousAggregationSupport    support,
                                        ContinuousAggregationDefinition definition)
        {
        f_map        = map;
        f_support    = support;
        f_definition = definition;
        }

    @Override
    @SuppressWarnings("unchecked")
    public R aggregate()
        {
        if (!f_active.get())
            {
            throw new IllegalStateException("continuous aggregation registration has been removed");
            }

        return (R) f_support.aggregateContinuousAggregation(f_definition);
        }

    @Override
    public <T> T invoke(K key, Remote.Function<? super R, ? extends T> function)
        {
        ensureActive();
        Objects.requireNonNull(function, "function cannot be null");

        return f_map.invoke(key,
                new ContinuousAggregationProcessor<>(f_definition, function));
        }

    @Override
    public <T> Map<K, T> invokeAll(Collection<? extends K> keys,
            Remote.BiFunction<? super K, ? super R, ? extends T> function)
        {
        ensureActive();
        Objects.requireNonNull(keys, "keys cannot be null");
        Objects.requireNonNull(function, "function cannot be null");

        return f_map.invokeAll(keys,
                new ContinuousAggregationProcessor<>(f_definition, function));
        }

    /**
     * Ensure this handle still represents an active registration.
     */
    private void ensureActive()
        {
        if (!f_active.get())
            {
            throw new IllegalStateException("continuous aggregation registration has been removed");
            }
        }

    /**
     * Remove this handle from the specified map.
     */
    private void removeFrom(InvocableMap<?, ?> map)
        {
        if (f_map != map)
            {
            throw new IllegalArgumentException("continuous aggregator belongs to a different map");
            }

        if (f_active.compareAndSet(true, false))
            {
            try
                {
                f_support.removeContinuousAggregation(f_definition);
                }
            catch (RuntimeException | Error e)
                {
                f_active.set(true);
                throw e;
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The map that owns this handle.
     */
    private final InvocableMap<K, V> f_map;

    /**
     * The service implementation backing this handle.
     */
    private final ContinuousAggregationSupport f_support;

    /**
     * The semantic registration definition.
     */
    private final ContinuousAggregationDefinition f_definition;

    /**
     * Whether this local handle remains active.
     */
    private final AtomicBoolean f_active = new AtomicBoolean(true);
    }
