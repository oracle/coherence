/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder.storemanager;

import com.tangosol.coherence.config.builder.BuilderCustomization;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.expression.ParameterResolver;


/**
 * The AbstractStoreManagerBuilder class builds an instance of a
 * BinaryStoreManager.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public abstract class AbstractStoreManagerBuilder<T>
        implements BinaryStoreManagerBuilder, BuilderCustomization<T>
    {
    // ----- BuilderCustomization methods -----------------------------------

    /**
     * {@inheritDoc}
     */
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

    // ----- internal -------------------------------------------------------

    /**
     * Validate the builder.
     *
     * @param resolver  the ParameterResolver
     */
    protected void validate(ParameterResolver resolver)
        {
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link ParameterizedBuilder} used to build a custom store manager.
     */
    private ParameterizedBuilder<T> m_bldrCustom;
    }
