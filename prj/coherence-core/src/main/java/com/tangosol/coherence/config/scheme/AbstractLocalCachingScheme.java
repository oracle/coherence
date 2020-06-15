/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.BuilderCustomization;
import com.tangosol.coherence.config.builder.NamedEventInterceptorBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.net.CacheService;

import java.util.Collections;
import java.util.List;

/**
 * The {@link AbstractLocalCachingScheme} is provides common functionality
 * for local caching schemes, including local-scheme, external-scheme, etc.
 *
 * @author pfm  2011.12.28
 * @since Coherence 12.1.2
 */
public abstract class AbstractLocalCachingScheme<T>
        extends AbstractCachingScheme
        implements BuilderCustomization<T>
    {
    // ----- ServiceScheme interface  ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceType()
        {
        return CacheService.TYPE_LOCAL;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NamedEventInterceptorBuilder> getEventInterceptorBuilders()
        {
        return Collections.EMPTY_LIST;
        }

    // ----- ServiceBuilder interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunningClusterNeeded()
        {
        return false;
        }

    // ----- BuilderCustomization interface ---------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterizedBuilder<T> getCustomBuilder()
        {
        return m_bldrCustom;
        }

    /**
     * {@inheritDoc}
     */
    public void setCustomBuilder(ParameterizedBuilder<T> bldr)
        {
        m_bldrCustom = bldr;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link ParameterizedBuilder} used to build the custom instance.
     */
    private ParameterizedBuilder<T> m_bldrCustom;
    }
