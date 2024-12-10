/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.tangosol.coherence.component.util.SafeService;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Service;

import java.util.Collection;

/**
 * A {@link RemoteCallable} to obtain the number of connections to a
 * specific Extend proxy service.
 *
 * @author Jonathan Knight  2022.06.06
 * @since 22.06
 */
public class GetExtendConnectionCount
        implements RemoteCallable<Integer>
    {
    /**
     * Create a {@link GetExtendConnectionCount}.
     *
     * @param sProxyServiceName  the name of the Extend proxy service
     */
    public GetExtendConnectionCount(String sProxyServiceName)
        {
        m_sProxyServiceName = sProxyServiceName;
        }

    @Override
    public Integer call() throws Exception
        {
        Cluster cluster = CacheFactory.getCluster();
        Service service = cluster.getService(m_sProxyServiceName);
        if (service instanceof SafeService)
            {
            service = ((SafeService) service).getService();
            }

        if (service instanceof ProxyService)
            {
            Collection<?> colConnection = ((ProxyService) service).getAcceptor().getConnections();
            return colConnection == null ? 0 : colConnection.size();
            }
        else
            {
            throw new IllegalArgumentException("Service " + m_sProxyServiceName + " is not a ProxyService");
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the Extend proxy service.
     */
    private final String m_sProxyServiceName;
    }
