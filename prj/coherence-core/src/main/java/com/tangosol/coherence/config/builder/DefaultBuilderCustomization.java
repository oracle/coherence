/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;


/**
 * The {@link DefaultBuilderCustomization} class is the default implementation
 * of {@link BuilderCustomization}.
 *
 * @author pfm  2012.01.06
 * @since Coherence 12.1.2
 */
public class DefaultBuilderCustomization<T>
        implements BuilderCustomization<T>
    {
    // ----- BuilderCustomization interface ---------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterizedBuilder<T> getCustomBuilder()
        {
        return m_bldr;
        }

    /**
     * {@inheritDoc}
     */
    public void setCustomBuilder(ParameterizedBuilder<T> bldr)
        {
        m_bldr = bldr;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link ParameterizedBuilder} used to build the custom instance.
     */
    private ParameterizedBuilder<T> m_bldr;
    }
