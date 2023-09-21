/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.internal.util.OrderedView;
import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.comparator.SafeComparator;
import com.tangosol.util.filter.AlwaysFilter;

import java.util.Comparator;
import java.util.function.Supplier;

/**
 * The {@link ViewBuilder} provides a means to {@link #build()} a {@code map view}
 * using a fluent pattern / style.
 *
 * @param <K>  the type of the map entry keys
 * @param <V>  the type of the entry values in the backing map that is used
 *             as the source for this {@code view}
 *
 * @author Aleks Seovic  2020.06.06
 *
 * @since 20.06
 */
public class MapViewBuilder<K, V>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct a new {@link MapViewBuilder} for the provided {@link NamedMap}.
     *
     * @param map  the {@link NamedMap} from which the view will be created
     */
    MapViewBuilder(NamedMap<K, V> map)
        {
        this(() -> (NamedCache<K, V>) map);
        }

    /**
     * Construct a new {@link ViewBuilder} for the provided {@link Supplier}.
     *
     * @param supplierNamedCache  the {@link Supplier} returning a {@link NamedCache}
     *                            from which the view will be created
     */
    protected MapViewBuilder(Supplier<NamedCache<K, V>> supplierNamedCache)
        {
        f_supplierNamedCache = supplierNamedCache;
        }

    // ----- builder interface ----------------------------------------------

    /**
     * The {@link Filter} that will be used to define the entries maintained in this view.
     * If no {@link Filter} is specified, {@link AlwaysFilter#INSTANCE} will be used.
     *
     * @param filter  the {@link Filter} that will be used to query the
     *                underlying {@link NamedMap}
     *
     * @return this {@link MapViewBuilder}
     */
    public MapViewBuilder<K, V> filter(Filter<?> filter)
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
     * @return this {@link MapViewBuilder}
     */
    public MapViewBuilder<K, V> listener(MapListener<? super K, ? super V> listener)
        {
        m_listener = listener;
        return this;
        }

    /**
     * The {@link ValueExtractor} that this {@code view} will use to transform the results from
     * the underlying map prior to storing them locally.
     *
     * @param mapper  the {@link ValueExtractor} that will be used to
     *                transform values retrieved from the underlying map
     *                before storing them locally; if specified, this
     *                {@code view} will become {@code read-only}
     * @param <U>     the type of the extracted value
     *
     * @return this {@link MapViewBuilder}
     */
    @SuppressWarnings("unchecked")
    public <U> MapViewBuilder<K, U> map(ValueExtractor<? super V, ? extends U> mapper)
        {
        m_mapper = mapper;
        return (MapViewBuilder<K, U>) this;
        }

    /**
     * Ensure that the view is sorted  based on the natural order of
     * the values, which must implement {@link Comparable} interface.
     *
     * @return this {@link MapViewBuilder}
     */
    public MapViewBuilder<K, V> sorted()
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
    public MapViewBuilder<K, V> sorted(Comparator<? super V> comparator)
        {
        m_comparator = comparator == null ? SafeComparator.INSTANCE() : comparator;
        return this;
        }

    /**
     * The resulting {@code view} will only map keys.
     * <p></p>
     * NOTE: this is mutually exclusive with {@link #values()}.
     *
     * @return this {@link MapViewBuilder}
     */
    public MapViewBuilder<K, V> keys()
        {
        m_fCacheValues = false;
        return this;
        }

    /**
     * The resulting {@code view} with both map keys and values.
     * <p></p>
     * NOTE: this is mutually exclusive with {@link #keys()}, and the default.
     *
     * @return this {@link MapViewBuilder}
     */
    public MapViewBuilder<K, V> values()
        {
        m_fCacheValues = true;
        return this;
        }

    /**
     * The optional {@link ClassLoader} to use when performing serialization/de-serialization operations.
     *
     * @param loader  the {@link ClassLoader}
     *
     * @return this {@link MapViewBuilder}
     */
    public MapViewBuilder<K, V> withClassLoader(ClassLoader loader)
        {
        m_loader = loader;
        return this;
        }

    /**
     * Construct a {@code view} of the {@link NamedMap} provided to this builder.
     *
     * @return the {@code view} of the {@link NamedMap} provided to this builder
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public NamedMap<K, V> build()
        {
        Filter<?>        filter = m_filter;
        ClassLoader      loader = m_loader;
        NamedCache<K, V> view   = new ContinuousQueryCache(f_supplierNamedCache,
                                                           filter == null ? AlwaysFilter.INSTANCE : filter,
                                                           m_fCacheValues, m_listener, m_mapper,
                                                           loader == null ? Base.getContextClassLoader(this) : loader);

        return m_comparator == null ? view : new OrderedView<>(view, m_comparator);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Supplier} returning a {@link NamedCache} from which the
     * view will be created.
     */
    protected final Supplier<NamedCache<K, V>> f_supplierNamedCache;

    /**
     * The {@link Filter} that will be used to define the entries maintained
     * in this view.
     */
    protected Filter<?> m_filter;

    /**
     * The {@link MapListener} that will receive all the events from
     * the {@code view}, including those corresponding to its initial
     * population.
     */
    protected MapListener<? super K, ? super V> m_listener;

    /**
     * The {@link ValueExtractor} that will be used to transform values
     * retrieved from the underlying map before storing them locally; if
     * specified, this {@code view} will become {@code read-only}.
     */
    protected ValueExtractor<? super V, ?> m_mapper;

    /**
     * The {@link Comparator} to use when creating a sorted view.
     */
    protected Comparator<? super V> m_comparator;

    /**
     * Flag controlling if the {@code view} will store both keys and values
     * or only keys. {@code true} by default.
     */
    protected boolean m_fCacheValues = true;

    /**
     * The View's {@link ClassLoader}.
     */
    protected ClassLoader m_loader;
    }
