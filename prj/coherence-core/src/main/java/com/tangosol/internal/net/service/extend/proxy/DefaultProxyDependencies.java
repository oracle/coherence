/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.internal.net.service.DefaultServiceDependencies;

import com.tangosol.net.ProxyService;

import com.tangosol.util.ClassHelper;

/**
 * The DefaultProxyDependencies class provides a default implementation of ProxyDependencies.
 *
 * @author pfm 2011.07.25
 * @since Coherence 12.1.2
 */
public class DefaultProxyDependencies
        extends DefaultServiceDependencies
        implements ProxyDependencies
    {
    /**
     * Construct a DefaultProxyDependencies object.
     */
    public DefaultProxyDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultProxyDependencies object, copying the values from the specified
     * ProxyDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultProxyDependencies(ProxyDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_fEnabled = deps.isEnabled();
            }
        }

    // ----- DefaultProxyDependencies methods -------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled()
        {
        return m_fEnabled;
        }

    /**
     * Set the enabled flag to enable or disable the proxy.
     *
     * @param fEnabled  the enabled flag
     */
    @Injectable("enabled")
    public void setEnabled(boolean fEnabled)
        {
        m_fEnabled = fEnabled;
        }

    /**
     * Validate the supplied dependencies.
     *
     * @throws IllegalArgumentException if the dependencies are not valid
     *
     * @return this object
     */
    public DefaultProxyDependencies validate()
        {
        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) + "{Enabled=" + isEnabled() + "}";
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The enabled flag.
     */
    private boolean m_fEnabled = true;
    }
