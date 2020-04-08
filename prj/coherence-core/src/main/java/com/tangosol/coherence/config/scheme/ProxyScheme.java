/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.internal.net.service.grid.DefaultProxyServiceDependencies;
import com.tangosol.internal.net.service.grid.DefaultReplicatedCacheDependencies;
import com.tangosol.internal.net.service.grid.LegacyXmlProxyServiceHelper;

import com.tangosol.internal.net.service.grid.ProxyServiceDependencies;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;

/**
 * The {@link ProxyScheme} class builds a Proxy scheme.
 *
 * @author pfm  2011.12.06
 * @since Coherence 12.1.2
 */
public class ProxyScheme
        extends AbstractCachingScheme<ProxyServiceDependencies>
    {
    // ----- constructors  --------------------------------------------------

    /**
     * Constructs a {@link ProxyScheme}.
     */
    public ProxyScheme()
        {
        m_serviceDependencies = new DefaultProxyServiceDependencies();
        }

    // ----- ServiceScheme interface  ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceType()
        {
        return "Proxy";
        }

    // ----- ServiceBuilder interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunningClusterNeeded()
        {
        return true;
        }
    }
