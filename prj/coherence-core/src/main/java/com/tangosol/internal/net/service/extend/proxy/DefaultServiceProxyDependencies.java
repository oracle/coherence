/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.net.Service;
import com.tangosol.run.xml.XmlElement;

/**
 * The DefaultServiceProxyDependencies class provides a default implementation of
 * ServiceProxyDependencies.
 *
 * @author pfm 2011.07.25
 * @since Coherence 12.1.2
 */
public class DefaultServiceProxyDependencies
        extends DefaultProxyDependencies
        implements ServiceProxyDependencies
    {
    /**
     * Construct a DefaultServiceProxyDependencies object.
     */
    public DefaultServiceProxyDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultServiceProxyDependencies object, copying the values from the
     * specified ServiceProxyDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultServiceProxyDependencies(ServiceProxyDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_serviceClassConfig = deps.getServiceClassConfig();
            m_bldrCustomService  = deps.getServiceBuilder();
            }
        }

    // ----- DefaultServiceProxyDependencies methods ------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public XmlElement getServiceClassConfig()
        {
        return m_serviceClassConfig;
        }

    /**
     * Set the XML that specifies the class configuration for the pluggable service.
     *
     * @param xml  the XML specifying the ServiceClass class-name and init-params
     *
     * @return this object
     */
    @Deprecated
    public DefaultServiceProxyDependencies setServiceClassConfig(XmlElement xml)
        {
        m_serviceClassConfig = xml;
        return this;
        }

    /**
     * Set the optional custom Service builder that is used by the proxy service.
     *
     * @param bldr  theService builder used by the proxy.
     *
     * @return this object
     */
    public DefaultServiceProxyDependencies setServiceBuilder(ParameterizedBuilder<? extends Service> bldr)
        {
        m_bldrCustomService = bldr;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterizedBuilder<? extends Service> getServiceBuilder()
        {
        return m_bldrCustomService;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultServiceProxyDependencies validate()
        {
        super.validate();

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return super.toString()
               + ", ServiceClassConfig=" + getServiceClassConfig()
               + "}";
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The pluggable XML configuration that specifies a Service implementation.
     */
    private XmlElement m_serviceClassConfig = null;

    /**
     * The custom Service builder.
     */
    private ParameterizedBuilder<? extends Service> m_bldrCustomService;
    }
