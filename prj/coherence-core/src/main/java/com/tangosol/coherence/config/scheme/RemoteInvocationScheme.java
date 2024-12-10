/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.internal.net.service.extend.remote.DefaultRemoteInvocationServiceDependencies;
import com.tangosol.internal.net.service.extend.remote.RemoteInvocationServiceDependencies;

import com.tangosol.net.Cluster;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Service;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

/**
 * The {@link RemoteInvocationScheme} class builds a remote invocation service.
 *
 * @author pfm  2011.12.06
 * @since Coherence 12.1.2
 */
public class RemoteInvocationScheme
        extends AbstractServiceScheme<RemoteInvocationServiceDependencies>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link RemoteCacheScheme}.
     */
    public RemoteInvocationScheme()
        {
        m_serviceDependencies = new DefaultRemoteInvocationServiceDependencies();
        }

    // ----- ServiceScheme interface  ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServiceType()
        {
        return InvocationService.TYPE_REMOTE;
        }

    // ----- ServiceBuilder interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRunningClusterNeeded()
        {
        return false;
        }

    /**
     * {@inheritDoc}
     */
    public Service realizeService(ParameterResolver resolver, ClassLoader loader, Cluster cluster)
        {
        Service service = super.realizeService(resolver, loader, cluster);

        injectScopeNameIntoService(service);

        return service;
        }
    }
