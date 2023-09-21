/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.Filter;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.comparator.SafeComparator;
import com.tangosol.util.filter.AlwaysFilter;

import java.util.Comparator;
import java.util.function.Supplier;

/**
 * The {@link ViewBuilder} provides a means to {@link #build()} a {@code view}
 * ({@link ContinuousQueryCache}) using a fluent pattern / style.
 *
 * @param <K>  the type of the cache entry keys
 * @param <V>  the type of the entry values in the back cache that is used
 *             as the source for this {@code view}
 *
 * @see ContinuousQueryCache
 *
 * @author rl 5.22.19
 * @since 12.2.1.4
 */
public class ViewBuilder<K, V>
        extends MapViewBuilder<K, V>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct a new {@link ViewBuilder} for the provided {@link NamedCache}.
     *
     * @param cache  the {@link NamedCache} from which the view will be created
     */
    public ViewBuilder(NamedCache<K, V> cache)
        {
        this(() -> cache);
        }

    /**
     * Construct a new {@link ViewBuilder} for the provided {@link NamedCache}.
     * The {@link Supplier} should return a new {@link NamedCache} instance upon
     * each invocation.
     *
     * @param supplierNamedCache  the {@link Supplier} returning a {@link NamedCache}
     *                            from which the view will be created
     */
    public ViewBuilder(Supplier<NamedCache<K, V>> supplierNamedCache)
        {
        super(supplierNamedCache);
        }

    // ----- builder interface ----------------------------------------------

    /**
     * The {@link Filter} that will be used to define the entries maintained in this view.
     * If no {@link Filter} is specified, {@link AlwaysFilter#INSTANCE} will be used.
     *
     * @param filter  the {@link Filter} that will be used to query the
     *                underlying {@link NamedCache}
     *
     * @return this {@link ViewBuilder}
     */
    public ViewBuilder<K, V> filter(Filter<?> filter)
        {
        m_filter = filter;
        return this;
        }

    /**
     * The {@link MapListener} that will receive all events, including those that
     * result from the initial population of the {@code view}.
     *
     * @param listener  the {@link MapListener} that will receive all the events from
     *                  the {@code view}, including those corresponding to its initial
     *                  population.
     *
     * @return this {@link ViewBuilder}
     */
    public ViewBuilder<K, V> listener(MapListener<? super K, ? super V> listener)
        {
        m_listener = listener;
        return this;
        }

    /**
     * The {@link ValueExtractor} that this {@code view} will use to transform the results from
     * the underlying cache prior to storing them locally.
     *
     * @param mapper  the {@link ValueExtractor} that will be used to
     *                transform values retrieved from the underlying cache
     *                before storing them locally; if specified, this
     *                {@code view} will become {@code read-only}
     *
     * @return this {@link ViewBuilder}
     */
    @SuppressWarnings("unchecked")
    public <U> ViewBuilder<K, U> map(ValueExtractor<? super V, ? extends U> mapper)
        {
        m_mapper = mapper;
        return (ViewBuilder<K, U>) this;
        }

    /**
     * Ensure that the view is sorted  based on the natural order of
     * the values, which must implement {@link Comparable} interface.
     *
     * @return this {@link MapViewBuilder}
     */
    public ViewBuilder<K, V> sorted()
        {
        return sorted(null);
        }

    /**
     * Ensure that the view is sorted using specified {@link Comparator}.
     *
     * @param comparator  the {@link Comparator} that will be used to sort the
     *                    entries in this view; if {@code null}, the entries will
     *                    be sorted based on the natural order of the values, which
     *                    must implement {@link Comparable} interface
     *
     * @return this {@link MapViewBuilder}
     */
    public ViewBuilder<K, V> sorted(Comparator<? super V> comparator)
        {
        m_comparator = SafeComparator.ensureSafe(comparator);
        return this;
        }

    /**
     * The resulting {@code view} will only cache keys.
     * <p></p>
     * NOTE: this is mutually exclusive with {@link #values()}.
     *
     * @return this {@link ViewBuilder}
     */
    public ViewBuilder<K, V> keys()
        {
        m_fCacheValues = false;
        return this;
        }

    /**
     * The resulting {@code view} with cache both keys and values.
     * <p></p>
     * NOTE: this is mutually exclusive with {@link #keys()}.
     *
     * @return this {@link ViewBuilder}
     */
    public ViewBuilder<K, V> values()
        {
        m_fCacheValues = true;
        return this;
        }

    /**
     * The optional {@link ClassLoader} to use when performing serialization/de-serialization operations.
     *
     * @param loader  the {@link ClassLoader}
     *
     * @return this {@link ViewBuilder}
     */
    public ViewBuilder<K, V> withClassLoader(ClassLoader loader)
        {
        m_loader = loader;
        return this;
        }

    /**
     * Construct a {@code view} of the {@link NamedCache} provided to this builder.
     *
     * @return the {@code view} of the {@link NamedCache} provided to this builder
     */
    public NamedCache<K, V> build()
        {
        return (NamedCache<K, V>) super.build();
        }
    }
