/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.config.expression.ParameterResolver;

/**
 * A {@link ParameterizedBuilder} that resolves a named resource from the
 * {@link com.tangosol.net.Cluster cluster's} {@link com.tangosol.util.ResourceRegistry}.
 *
 * @param <T>  the type of the named resource
 */
public class NamedResourceBuilder<T>
        implements ParameterizedBuilder<T>
    {
    // ----- constructors ---------------------------------------------------

    public NamedResourceBuilder(ParameterizedBuilder<T> delegate, String sName)
        {
        m_delegate = delegate;
        m_sName    = sName;
        }

    // ----- accessors ------------------------------------------------------

    public ParameterizedBuilder<T> getDelegate()
        {
        return m_delegate;
        }

    public String getName()
        {
        return m_sName;
        }

    // ----- ParameterizedBuilder methods -----------------------------------

    @Override
    public T realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        return m_delegate.realize(resolver, loader, listParameters);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link ParameterizedBuilder} this builder delegates to.
     */
    private final ParameterizedBuilder<T> m_delegate;

    /**
     * The name of the resource.
     */
    private final String m_sName;
    }
