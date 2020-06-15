/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.Filter;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.filter.AlwaysFilter;

import java.util.function.Supplier;

/**
 * The {@link ViewBuilder} provides a means to {@link #build()} a {@code view}
 * ({@link ContinuousQueryCache}) using a fluent pattern / style.
 *
 * @param <K>        the type of the cache entry keys
 * @param <V_BACK>   the type of the entry values in the back cache that is used
 *                   as the source for this {@code view}
 * @param <V_FRONT>  the type of the entry values in this {@code view}, which
 *                   will be the same as {@code V_BACK}, unless a {@code transformer} is specified
 *                   when creating this {@code view}
 *
 * @see ContinuousQueryCache
 *
 * @author rl 5.22.19
 * @since 12.2.1.4
 */
public class ViewBuilder<K, V_BACK, V_FRONT>
        extends MapViewBuilder<K, V_BACK, V_FRONT>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct a new {@link ViewBuilder} for the provided {@link NamedCache}.
     *
     * @param cache  the {@link NamedCache} from which the view will be created
     */
    public ViewBuilder(NamedCache<K, V_BACK> cache)
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
    public ViewBuilder(Supplier<NamedCache<K, V_BACK>> supplierNamedCache)
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
    public ViewBuilder<K, V_BACK, V_FRONT> filter(Filter filter)
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
    public ViewBuilder<K, V_BACK, V_FRONT> listener(MapListener<? super K, ? super V_FRONT> listener)
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
    public ViewBuilder<K, V_BACK, V_FRONT> map(ValueExtractor<? super V_BACK, ? extends V_FRONT> mapper)
        {
        m_mapper = mapper;
        return this;
        }

    /**
     * The resulting {@code view} will only cache keys.
     *
     * NOTE: this is mutually exclusive with {@link #values()}.
     *
     * @return this {@link ViewBuilder}
     */
    public ViewBuilder<K, V_BACK, V_FRONT> keys()
        {
        m_fCacheValues = false;
        return this;
        }

    /**
     * The resulting {@code view} with cache both keys and values.
     *
     * NOTE: this is mutually exclusive with {@link #keys()}.
     *
     * @return this {@link ViewBuilder}
     */
    public ViewBuilder<K, V_BACK, V_FRONT> values()
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
    public ViewBuilder<K, V_BACK, V_FRONT> withClassLoader(ClassLoader loader)
        {
        m_loader = loader;
        return this;
        }

    /**
     * Construct a {@code view} of the {@link NamedCache} provided to this builder.
     *
     * @return the {@code view} of the {@link NamedCache} provided to this builder
     */
    public NamedCache<K, V_FRONT> build()
        {
        return (NamedCache<K, V_FRONT>) super.build();
        }
    }
