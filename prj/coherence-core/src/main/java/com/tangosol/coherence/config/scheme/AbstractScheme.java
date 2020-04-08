/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.config.annotation.Injectable;

/**
 * The {@link AbstractScheme} is the base implementation of a {@link Scheme}.
 * The setters annotated with @Injectable are automatically called by CODI
 * during document processing.  Non-annotated setters are typically called
 * by the CODI element processors.
 *
 * @author pfm  2011.12.28
 * @since Coherence 12.1.2
 */
public abstract class AbstractScheme
        implements Scheme
    {
    // ----- Scheme interface -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSchemeName()
        {
        String sSchemeName = m_sSchemeName;

        if (sSchemeName == null)
            {
            m_sSchemeName = sSchemeName = "";
            }

        return sSchemeName;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAnonymous()
        {
        return m_sSchemeName == null || m_sSchemeName.isEmpty();
        }

    // ----- AbstractScheme methods -----------------------------------------

    /**
     * Set the scheme name, trimming the name of starting and ending
     * whitespace if necessary.
     *
     * @param sName  the scheme name
     */
    @Injectable
    public void setSchemeName(String sName)
        {
        m_sSchemeName = sName == null ? "" : sName.trim();
        }

    // ----- internal -------------------------------------------------------

    /**
     * Validate the properties.
     */
    protected void validate()
        {
        }

    // ----- data members ---------------------------------------------------

    /**
     * The scheme name.
     */
    private String m_sSchemeName;
    }
