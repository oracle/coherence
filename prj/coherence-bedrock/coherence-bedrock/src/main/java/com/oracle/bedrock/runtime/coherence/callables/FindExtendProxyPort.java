/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.coherence.common.net.InetSocketAddress32;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Service;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * A {@link RemoteCallable} that returns the port that the Extend proxy is listening on.
 */
public class FindExtendProxyPort
        implements RemoteCallable<Integer>
    {
    /**
     * Create an instance of {@link FindExtendProxyPort} to find the default proxy port.
     */
    public FindExtendProxyPort()
        {
        this("Proxy");
        }

    /**
     * Create an instance of {@link FindExtendProxyPort} to find the
     * port for the Extend proxy with the specified service name.
     *
     * @param sServiceName  the service name
     */
    public FindExtendProxyPort(String sServiceName)
        {
        m_sServiceName = sServiceName;
        }

    @Override
    public Integer call()
        {
        return findPort(m_sServiceName);
        }

    // ----- helper methods -------------------------------------------------

    private int findPort(String sServiceName)
        {
        Cluster      cluster = CacheFactory.getCluster();
        Service      service = cluster.getService(sServiceName);
        if (service instanceof SafeService)
            {
            service = ((SafeService) service).getService();
            }

        if (service instanceof ProxyService)
            {
            ProxyService  proxyService = (ProxyService) service;
            TcpAcceptor   acceptor     = (TcpAcceptor) proxyService.getAcceptor();
            SocketAddress address      = acceptor.getLocalAddress();
            if (address instanceof InetSocketAddress)
                {
                return ((InetSocketAddress) address).getPort();
                }
            if (address instanceof InetSocketAddress32)
                {
                return ((InetSocketAddress32) address).getPort();
                }
            }
        return -1;
        }

    /**
     * Find the locally running Extend proxy port.
     *
     * @return the local Extend proxy port
     */
    public static int local()
        {
        return local("Proxy");
        }

    /**
     * Find the locally running Extend proxy port.
     *
     * @param sServiceName  the service name
     *
     * @return the local Extend proxy port
     */
    public static int local(String sServiceName)
        {
        return INSTANCE.findPort(sServiceName);
        }

    // ----- constants ------------------------------------------------------

    /**
     * A singleton instance of {@link FindExtendProxyPort} to find the default proxy port.
     */
    public static final FindExtendProxyPort INSTANCE = new FindExtendProxyPort();

    // ----- data members ---------------------------------------------------

    /**
     * The proxy service name.
     */
    private final String m_sServiceName;
    }
