/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.coherence.component.net.extend.connection.TcpConnection;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Member;
import com.tangosol.net.Service;
import com.tangosol.net.messaging.Connection;
import com.tangosol.net.messaging.ConnectionAcceptor;
import com.tangosol.util.UUID;

import java.util.Collection;

/**
 * A {@link RemoteCallable} to determine whether a specific Extend client
 * is connected to an Extend proxy service.
 *
 * @author Jonathan Knight  2022.06.06
 * @since 22.06
 */
public class HasExtendConnection
        implements RemoteCallable<Boolean>
    {
    /**
     * Create a {@link HasExtendConnection}.
     *
     * @param sProxyServiceName the name of the Extend proxy service
     * @param uuid              the UUID of the client
     */
    public HasExtendConnection(String sProxyServiceName, UUID uuid)
        {
        m_sProxyServiceName = sProxyServiceName;
        m_uuid              = uuid;
        }

    @Override
    @SuppressWarnings("unchecked")
    public Boolean call() throws Exception
        {
        Cluster cluster = CacheFactory.getCluster();
        Service service = cluster.getService(m_sProxyServiceName);
        if (service instanceof SafeService)
            {
            service = ((SafeService) service).getService();
            }

        if (service instanceof ProxyService)
            {
            ConnectionAcceptor acceptor = ((ProxyService) service).getAcceptor();
            Collection<Connection> colConnection = acceptor.getConnections();
            for (Connection connection : colConnection)
                {
                Member member = ((TcpConnection) connection).getMember();
                if (member.getUuid().equals(m_uuid))
                    {
                    return true;
                    }
                }
            return false;
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

    /**
     * The {@link UUID} of the Extend client.
     */
    private final UUID m_uuid;
    }
