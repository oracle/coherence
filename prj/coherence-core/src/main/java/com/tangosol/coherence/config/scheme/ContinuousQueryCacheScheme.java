/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

/**
 * This scheme is internally used to provide the {@link ParameterizedBuilder} that constructs the {@code view-filter}
 * for the {@link ViewScheme}.
 *
 * @author rlubke
 * @since 12.2.1.4
 */
public class ContinuousQueryCacheScheme
        extends AbstractLocalCachingScheme
    {
    // ----- accessor methods -------------------------------------------------

    /**
     * Set the {@link ParameterizedBuilder} used to construct the {@link Filter} to be used by the
     * {@link ContinuousQueryCache}.
     *
     * @param filterBuilder  the {@link ParameterizedBuilder} used to construct the {@link ValueExtractor} to
     *                       be used as a transformer by the {@link ContinuousQueryCache}
     */
    @SuppressWarnings("unused")
    @Injectable("view-filter")
    public void setFilterBuilder(ParameterizedBuilder<Filter> filterBuilder)
        {
        m_filterBuilder = filterBuilder;
        }

    /**
     * Return the {@link ParameterizedBuilder} used to construct the {@link Filter} to be used by the
     * {@link ContinuousQueryCache}.
     *
     * @return the {@link ParameterizedBuilder} used to construct the {@link Filter} to be used by the
     *         {@link ContinuousQueryCache}
     */
    public ParameterizedBuilder<Filter> getFilterBuilder()
        {
        return m_filterBuilder;
        }

    /**
     * Set the {@link ParameterizedBuilder} used to construct the {@link ValueExtractor} to be used as a transformer
     * by the {@link ContinuousQueryCache}.
     *
     * @param transformerBuilder  the {@link ParameterizedBuilder} used to construct the {@link ValueExtractor} to
     *                            be used as a transformer by the {@link ContinuousQueryCache}
     */
    @SuppressWarnings("unused")
    @Injectable("transformer")
    public void setTransformerBuilder(ParameterizedBuilder<ValueExtractor> transformerBuilder)
        {
        m_transformerBuilder = transformerBuilder;
        }

    /**
     * Return the {@link ParameterizedBuilder} used to construct the {@link ValueExtractor} to be used as a
     * transformer by the {@link ContinuousQueryCache}.
     *
     * @return the {@link ParameterizedBuilder} used to construct the {@link ValueExtractor} to be used as a
     *         transformer by the {@link ContinuousQueryCache}.
     */
    public ParameterizedBuilder<ValueExtractor> getTransformerBuilder()
        {
        return m_transformerBuilder;
        }

    /**
     * See {@link ContinuousQueryCache#setReconnectInterval(long)}.
     *
     * @param ldtReconnectInterval  reconnect interval in milliseconds
     */
    @SuppressWarnings("unused")
    @Injectable("reconnect-interval")
    public void setReconnectInterval(long ldtReconnectInterval)
        {
        m_cReconnectMillis = ldtReconnectInterval;
        }

    /**
     * See {@link ContinuousQueryCache#getReconnectInterval()}.
     *
     * @return reconnect interval in milliseconds
     */
    public long getReconnectInterval()
        {
        return m_cReconnectMillis;
        }

    /**
     * See {@link ContinuousQueryCache#setReadOnly(boolean)}.
     *
     * @param fReadOnly  pass true to prohibit clients from making
     *                   modifications to this cache
     */
    @Injectable("read-only")
    public void setReadOnly(boolean fReadOnly)
        {
        m_fReadOnly = fReadOnly;
        }

    /**
     * See {@link ContinuousQueryCache#isReadOnly()}.
     *
     * @return true if this ContinuousQueryCache has been configured as
     *         read-only
     */
    public boolean isReadOnly()
        {
        return m_fReadOnly;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The reconnect interval to pass to the {@link ContinuousQueryCache}.
     */
    protected long m_cReconnectMillis;

    /**
     * The {@link ParameterizedBuilder} used to construct the {@link ValueExtractor} the {@link ContinuousQueryCache}
     * will use to transform values retrieved from the underlying cache before storing them locally.
     */
    private ParameterizedBuilder<ValueExtractor> m_transformerBuilder;

    /**
     * The {@link ParameterizedBuilder} used to construct the filter to be used by the
     * {@link ContinuousQueryCache}.
     */
    private ParameterizedBuilder<Filter> m_filterBuilder;

    /**
     * The read-only flag to pass to the {@link ContinuousQueryCache}.
     */
    protected boolean m_fReadOnly;
    }
