/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.config.unit.Millis;
import com.tangosol.config.annotation.Injectable;

import com.tangosol.net.Service;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.Filter;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.filter.AlwaysFilter;

/**
 * DefaultViewDependencies captures the dependencies required for the {@link
 * com.tangosol.net.internal.ViewCacheService}.
 *
 * @author hr  2019.06.11
 * @since 12.2.1.4
 */
public class DefaultViewDependencies
        extends DefaultServiceDependencies
    {
    // ----- accessor methods -----------------------------------------------

    /**
     * Return a {@link ParameterizedBuilder builder} that can create a {@link
     * Filter} to be used by a {@link ContinuousQueryCache}.
     *
     * @return the builder that creates Filters
     */
    public ParameterizedBuilder<Filter> getFilterBuilder()
        {
        return m_bldrFilter;
        }

    /**
     * Set a {@link ParameterizedBuilder builder} that can create a {@link
     * Filter}.
     *
     * @param bldrFilter  the {@link ParameterizedBuilder builder} that can
     *                    create a {@link Filter}
     */
    @SuppressWarnings("unused")
    @Injectable("view-filter")
    public void setFilterBuilder(ParameterizedBuilder<Filter> bldrFilter)
        {
        m_bldrFilter = bldrFilter;
        }

    /**
     * Return {@link ParameterizedBuilder builder} that can create a {@link
     * ValueExtractor} to be used transform the keys and values of a
     * {@link ContinuousQueryCache}.
     *
     * @return the builder that can create {@link ValueExtractor}s
     */
    public ParameterizedBuilder<ValueExtractor> getTransformerBuilder()
        {
        return m_bldrTransformer;
        }

    /**
     * Set a {@link ParameterizedBuilder builder} that can create a {@link
     * ValueExtractor} to be used transform the keys and values of a
     * {@link ContinuousQueryCache}.
     *
     * @param bldrTransformer  the builder that can create {@link ValueExtractor}s
     */
    @SuppressWarnings("unused")
    @Injectable("transformer")
    public void setTransformerBuilder(ParameterizedBuilder<ValueExtractor> bldrTransformer)
        {
        m_bldrTransformer = bldrTransformer;
        }

    /**
     * Return {@link ParameterizedBuilder builder} that can create a {@link
     * MapListener}s to be used listen to events emanating from the {@link
     * ContinuousQueryCache}.
     *
     * @return a builder that can create a {@link MapListener}s
     */
    public ParameterizedBuilder<MapListener> getListenerBuilder()
            {
            return m_bldrListener;
            }

    /**
     * Set the {@link ParameterizedBuilder builder} that can create a {@link
     * MapListener}s to be used listen to events emanating from the {@link
     * ContinuousQueryCache}.
     *
     * @param bldrListener  a builder that can create a {@link MapListener}s
     */
    @Injectable("listener")
    public void setListenerBuilder(ParameterizedBuilder<MapListener> bldrListener)
        {
        m_bldrListener = bldrListener;
        }

    /**
     * Return the reconnect interval that should be used by the {@link
     * ContinuousQueryCache}.
     *
     * @return the reconnect interval that should be used by the {@link
     *         ContinuousQueryCache}
     *
     * @see {@link ContinuousQueryCache#getReconnectInterval()}.
     */
    public long getReconnectInterval()
        {
        return m_cReconnectMillis;
        }

    /**
     * Set the reconnect interval that should be used by the {@link
     * ContinuousQueryCache}.
     *
     * @param cReconnectMillis  the reconnect interval that should be used
     *                              by the {@link ContinuousQueryCache}
     *
     * @see ContinuousQueryCache#setReconnectInterval(long)
     */
    @SuppressWarnings("unused")
    @Injectable("reconnect-interval")
    public void setReconnectInterval(Millis cReconnectMillis)
        {
        m_cReconnectMillis = cReconnectMillis.get();
        }

    /**
     * Return whether the {@link ContinuousQueryCache} should be in read-only
     * mode.
     *
     * @return whether the ContinuousQueryCache should be in read-only mode
     *
     * @see ContinuousQueryCache#isReadOnly().
     */
    public boolean isReadOnly()
        {
        return m_fReadOnly;
        }

    /**
     * Set whether the {@link ContinuousQueryCache} should be in read-only
     * mode.
     *
     * @param fReadOnly  true if the ContinuousQueryCache should be in read-only
     *                   mode
     *
     * @see ContinuousQueryCache#setReadOnly(boolean).
     */
    @Injectable("read-only")
    public void setReadOnly(boolean fReadOnly)
        {
        m_fReadOnly = fReadOnly;
        }

    /**
     * Return whether the {@link ContinuousQueryCache} should cache values or only keys.
     * The default behavior is to cache values.
     *
     * @return whether the {@link ContinuousQueryCache} should cache keys and values or only keys
     *
     * @see ContinuousQueryCache#isCacheValues()
     *
     * @since 12.2.1.4.11
     */
    public boolean isCacheValues()
        {
        return m_fCacheValues;
        }

    /**
     * Set whether the {@link ContinuousQueryCache} should cache values or only keys.
     *
     * @param fCacheValues  {@code true} if the {@link ContinuousQueryCache} should cache keys and values
     *                      or only keys
     *
     * @see ContinuousQueryCache#setCacheValues(boolean)
     *
     * @since 12.2.1.4.11
     */
    @Injectable("cache-values")
    public void setCacheValues(boolean fCacheValues)
        {
        m_fCacheValues = fCacheValues;
        }

    /**
     * Return the {@link Service} that will back the {@link ContinuousQueryCache}.
     *
     * @return the {@link Service} that will back the ContinuousQueryCache
     */
    public Service getBackService()
        {
        return m_service;
        }

    /**
     * Set the {@link Service} that will back the {@link ContinuousQueryCache}.
     *
     * @param service  the {@link Service} that will back the ContinuousQueryCache
     */
    public void setBackService(Service service)
        {
        m_service = service;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The reconnect interval to pass to the {@link ContinuousQueryCache}.
     */
    protected long m_cReconnectMillis;

    /**
     * The builder that can create the filter to be used by the {@link
     * ContinuousQueryCache}.
     */
    protected ParameterizedBuilder<Filter> m_bldrFilter = (resolver, loader, params) -> AlwaysFilter.INSTANCE;

    /**
     * A builder that can create a {@link ValueExtractor} the {@link
     * ContinuousQueryCache} will use to transform values retrieved from the
     * underlying cache before storing them locally.
     */
    protected ParameterizedBuilder<ValueExtractor> m_bldrTransformer;

    /**
     * A builder that can create a {@link MapListener} to be notified for any
     * changes to the {@link ContinuousQueryCache}.
     */
    protected ParameterizedBuilder<MapListener> m_bldrListener;

    /**
     * Whether the {@link ContinuousQueryCache} should be in read-only mode.
     */
    protected boolean m_fReadOnly;

    /**
     * The service backing the {@link ContinuousQueryCache}.
     */
    protected Service m_service;

    /**
     * Flag controlling the {@link ContinuousQueryCache} storing both keys and values or only keys.
     */
    protected boolean m_fCacheValues = true;
    }
