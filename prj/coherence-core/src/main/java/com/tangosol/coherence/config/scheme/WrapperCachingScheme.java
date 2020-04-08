/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.NamedEventInterceptorBuilder;
import com.tangosol.coherence.config.builder.ServiceBuilder;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.BackingMapManager;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import java.util.List;
import java.util.Map;

/**
 * A simple implementation of the {@link CachingScheme} interface
 * built as a wrapper around another CachingScheme implementation.
 *
 * @author jk 2015.05.29
 * @since Coherence 14.1.1
 */
public class WrapperCachingScheme
        implements CachingScheme
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link WrapperCachingScheme} that wraps a specified {@link CachingScheme}.
     *
     * @param innerScheme  the {@link CachingScheme} being wrapped
     */
    public WrapperCachingScheme(CachingScheme innerScheme)
        {
        f_innerScheme = innerScheme;
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Obtain the wrapped {@link CachingScheme}.
     * 
     * @return  the wrapped {@link CachingScheme}
     */
    public CachingScheme getCachingScheme()
        {
        return f_innerScheme;
        }

    // ----- CachingScheme methods ------------------------------------------

    @Override
    public BackingMapManager realizeBackingMapManager(ConfigurableCacheFactory ccf)
        {
        return f_innerScheme.realizeBackingMapManager(ccf);
        }

    @Override
    public Map realizeMap(ParameterResolver resolver, Dependencies dependencies)
        {
        return f_innerScheme.realizeMap(resolver, dependencies);
        }

    @Override
    public NamedCache realizeCache(ParameterResolver resolver, Dependencies dependencies)
        {
        return f_innerScheme.realizeCache(resolver, dependencies);
        }

    @Override
    public boolean isAutoStart()
        {
        return f_innerScheme.isAutoStart();
        }

    @Override
    public String getServiceName()
        {
        return f_innerScheme.getServiceName();
        }

    @Override
    public String getScopedServiceName()
        {
        return f_innerScheme.getScopedServiceName();
        }

    @Override
    public String getServiceType()
        {
        return f_innerScheme.getServiceType();
        }

    @Override
    public ServiceBuilder getServiceBuilder()
        {
        return f_innerScheme.getServiceBuilder();
        }

    @Override
    public List<NamedEventInterceptorBuilder> getEventInterceptorBuilders()
        {
        return f_innerScheme.getEventInterceptorBuilders();
        }

    @Override
    public String getSchemeName()
        {
        return f_innerScheme.getSchemeName();
        }

    @Override
    public boolean isAnonymous()
        {
        return f_innerScheme.isAnonymous();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The wrapped {@link CachingScheme}.
     */
    private final CachingScheme f_innerScheme;
    }
