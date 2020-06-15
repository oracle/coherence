/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.internal.net.service.grid.DefaultInvocationServiceDependencies;
import com.tangosol.internal.net.service.grid.DefaultReplicatedCacheDependencies;
import com.tangosol.internal.net.service.grid.InvocationServiceDependencies;
import com.tangosol.internal.net.service.grid.LegacyXmlGridHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.InvocationService;
import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;

/**
 * The {@link InvocationScheme} class builds an Invocation service.
 *
 * @author pfm  2011.12.06
 * @since Coherence 12.1.2
 */
public class InvocationScheme
        extends AbstractServiceScheme<InvocationServiceDependencies>
    {
    // ----- constructors  --------------------------------------------------

    /**
     * Constructs a {@link InvocationScheme}.
     */
    public InvocationScheme()
        {
        m_serviceDependencies = new DefaultInvocationServiceDependencies();
        }

    // ----- ServiceScheme interface  ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceType()
        {
        return InvocationService.TYPE_DEFAULT;
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
