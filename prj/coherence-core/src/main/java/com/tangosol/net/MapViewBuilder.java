/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.filter.AlwaysFilter;

import java.util.function.Supplier;

/**
 * The {@link ViewBuilder} provides a means to {@link #build()} a {@code map view}
 * using a fluent pattern / style.
 *
 * @param <K>        the type of the map entry keys
 * @param <V_BACK>   the type of the entry values in the backing map that is used
 *                   as the source for this {@code view}
 * @param <V_FRONT>  the type of the entry values in this {@code view}, which
 *                   will be the same as {@code V_BACK}, unless a {@code transformer} is specified
 *                   when creating this {@code view}
 *
 * @author Aleks Seovic  2020.06.06
 *
 * @since 14.1.2
 */
public class MapViewBuilder<K, V_BACK, V_FRONT>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct a new {@link MapViewBuilder} for the provided {@link NamedMap}.
     *
     * @param map  the {@link NamedMap} from which the view will be created
     */
    MapViewBuilder(NamedMap<K, V_BACK> map)
        {
        this(() -> (NamedCache<K, V_BACK>) map);
        }

    /**
     * Construct a new {@link ViewBuilder} for the provided {@link Supplier}.
     *
     * @param supplierNamedCache  the {@link Supplier} returning a {@link NamedCache}
     *                            from which the view will be created
     */
    protected MapViewBuilder(Supplier<NamedCache<K, V_BACK>> supplierNamedCache)
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
    public MapViewBuilder<K, V_BACK, V_FRONT> filter(Filter filter)
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
    public MapViewBuilder<K, V_BACK, V_FRONT> listener(MapListener<? super K, ? super V_FRONT> listener)
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
     *
     * @return this {@link MapViewBuilder}
     */
    public MapViewBuilder<K, V_BACK, V_FRONT> map(ValueExtractor<? super V_BACK, ? extends V_FRONT> mapper)
        {
        m_mapper = mapper;
        return this;
        }

    /**
     * The resulting {@code view} will only map keys.
     *
     * NOTE: this is mutually exclusive with {@link #values()}.
     *
     * @return this {@link MapViewBuilder}
     */
    public MapViewBuilder<K, V_BACK, V_FRONT> keys()
        {
        m_fCacheValues = false;
        return this;
        }

    /**
     * The resulting {@code view} with both map keys and values.
     *
     * NOTE: this is mutually exclusive with {@link #keys()}, and the default.
     *
     * @return this {@link MapViewBuilder}
     */
    public MapViewBuilder<K, V_BACK, V_FRONT> values()
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
    public MapViewBuilder<K, V_BACK, V_FRONT> withClassLoader(ClassLoader loader)
        {
        m_loader = loader;
        return this;
        }

    /**
     * Construct a {@code view} of the {@link NamedMap} provided to this builder.
     *
     * @return the {@code view} of the {@link NamedMap} provided to this builder
     */
    public NamedMap<K, V_FRONT> build()
        {
        Filter      filter = m_filter;
        ClassLoader loader = m_loader;
        return new ContinuousQueryCache<>(f_supplierNamedCache,
                                          filter == null ? AlwaysFilter.INSTANCE : filter,
                                          m_fCacheValues,
                                          m_listener,
                                          m_mapper,
                                          loader == null ? Base.getContextClassLoader(this) : loader);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Supplier} returning a {@link NamedCache} from which the
     * view will be created.
     */
    protected final Supplier<NamedCache<K, V_BACK>> f_supplierNamedCache;

    /**
     * The {@link Filter} that will be used to define the entries maintained
     * in this view.
     */
    protected Filter m_filter;

    /**
     * The {@link MapListener} that will receive all the events from
     * the {@code view}, including those corresponding to its initial
     * population.
     */
    protected MapListener<? super K, ? super V_FRONT> m_listener;

    /**
     * The {@link ValueExtractor} that will be used to transform values
     * retrieved from the underlying map before storing them locally; if
     * specified, this {@code view} will become {@code read-only}.
     */
    protected ValueExtractor<? super V_BACK, ? extends V_FRONT> m_mapper;

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
