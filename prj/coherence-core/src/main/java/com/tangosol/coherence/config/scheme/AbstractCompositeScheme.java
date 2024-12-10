/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.util.Base;

/**
 * The {@link AbstractCompositeScheme} manages a scheme that is used to
 * build a composite cache consisting of a front map and a back cache/map.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public abstract class AbstractCompositeScheme<T>
        extends AbstractLocalCachingScheme<T>
    {
    // ----- AbstractCompositeScheme methods --------------------------------

    /**
     * Return the front scheme.
     *
     * @return the front scheme
     */
    public CachingScheme getFrontScheme()
        {
        return m_schemeFront;
        }

    /**
     * Set the front scheme.
     *
     * @param scheme  the front scheme
     */
    public void setFrontScheme(CachingScheme scheme)
        {
        m_schemeFront = scheme;
        }

    /**
     * Return the back scheme.
     *
     * @return the back scheme
     */
    public CachingScheme getBackScheme()
        {
        return m_schemeBack;
        }

    /**
     * Set the back scheme.
     *
     * @param scheme  the back scheme
     */
    public void setBackScheme(CachingScheme scheme)
        {
        m_schemeBack = scheme;
        }

    // ----- internal -------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate(ParameterResolver resolver)
        {
        super.validate(resolver);

        Base.checkNotNull(getBackScheme(), "BackScheme");
        Base.checkNotNull(getFrontScheme(), "FrontScheme");
        }

    // ----- data members ---------------------------------------------------

    /**
     * The front scheme.
     */
    private CachingScheme m_schemeFront;

    /**
     * The back scheme.
     */
    private CachingScheme m_schemeBack;
    }
