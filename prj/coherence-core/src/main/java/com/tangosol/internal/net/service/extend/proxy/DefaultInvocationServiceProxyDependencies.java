/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.CoherenceMode;

/**
 * The DefaultInvocationServiceProxyDependencies class provides a default implementation of
 * InvocationServiceProxyDependencies.
 *
 * @author pfm 2011.07.25
 * @since Coherence 12.1.2
 */
public class DefaultInvocationServiceProxyDependencies
        extends DefaultServiceProxyDependencies
        implements InvocationServiceProxyDependencies
    {
    /**
     * Construct a DefaultInvocationServiceProxyDependencies object.
     */
    public DefaultInvocationServiceProxyDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultInvocationServiceProxyDependencies object, copying the values from
     * the specified InvocationServiceProxyDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultInvocationServiceProxyDependencies(InvocationServiceProxyDependencies deps)
        {
        super(deps);

        if (deps instanceof DefaultInvocationServiceProxyDependencies)
            {
            DefaultInvocationServiceProxyDependencies depsThat = (DefaultInvocationServiceProxyDependencies) deps;
            m_fOperationalConfigEnabled = depsThat.m_fOperationalConfigEnabled;
            m_sSystemPropertyValue      = depsThat.m_sSystemPropertyValue;
            }
        else if (deps == null)
            {
            setEnabled(!CoherenceMode.isProd());
            applySystemPropertyOverride();
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the invocation-service proxy system property value observed while
     * resolving the enabled flag.
     *
     * @return the property value, or {@code null} if unset
     */
    public String getSystemPropertyValue()
        {
        return m_sSystemPropertyValue;
        }

    /**
     * Return the operational-config enabled value.
     *
     * @return the operational-config value, or {@code null} if unset
     */
    public Boolean getOperationalConfigEnabled()
        {
        return m_fOperationalConfigEnabled;
        }

    /**
     * Record and apply the operational-config enabled value.
     *
     * @param fEnabled  the configured enabled value
     */
    public void setOperationalConfigEnabled(boolean fEnabled)
        {
        m_fOperationalConfigEnabled = fEnabled;
        setEnabled(fEnabled);
        applySystemPropertyOverride();
        }

    /**
     * Apply the system-property override, if present.
     */
    public void applySystemPropertyOverride()
        {
        String sValue = Config.getProperty(PROP_INVOCATION_ENABLED);
        m_sSystemPropertyValue = sValue == null || sValue.isBlank() ? null : sValue;
        if (m_sSystemPropertyValue != null)
            {
            setEnabled(Boolean.parseBoolean(m_sSystemPropertyValue));
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * System property that overrides the invocation-service proxy enabled flag.
     */
    public static final String PROP_INVOCATION_ENABLED = "coherence.invocation.enabled";

    // ----- data members ---------------------------------------------------

    /**
     * The enabled value supplied by operational configuration.
     */
    private Boolean m_fOperationalConfigEnabled;

    /**
     * The system-property value observed while resolving the enabled flag.
     */
    private String m_sSystemPropertyValue;
    }
