/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.internal.net.service.grid.ProxyServiceDependencies;
import com.tangosol.internal.net.service.peer.acceptor.GrpcAcceptorDependencies;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Coherence;
import com.tangosol.net.ProxyService;
import com.tangosol.net.grpc.GrpcAcceptorController;
import com.tangosol.net.grpc.GrpcDependencies;

/**
 * A {@link RemoteCallable} that returns the port that the gRPC proxy is listening on.
 */
public class FindGrpcProxyPort
        implements RemoteCallable<Integer>
    {
    /**
     * Create an instance of {@link FindGrpcProxyPort} to find the default proxy port.
     */
    public FindGrpcProxyPort()
        {
        this(Coherence.DEFAULT_SCOPE);
        }

    /**
     * Create an instance of {@link FindGrpcProxyPort} to find the
     * port for the gRPC proxy with the specified scope prefix.
     *
     * @param sScope  the scope prefix
     */
    public FindGrpcProxyPort(String sScope)
        {
        m_sScope = sScope;
        }

    @Override
    public Integer call()
        {
        return findPort(m_sScope);
        }

    // ----- helper methods -------------------------------------------------

    private int findPort(String sScope)
        {
        Cluster      cluster = CacheFactory.getCluster();
        ProxyService service = (ProxyService) cluster.getService(sScope + GrpcDependencies.SCOPED_PROXY_SERVICE_NAME);
        if (service == null)
            {
            return -1;
            }
        ProxyServiceDependencies depsService  = (ProxyServiceDependencies) service.getDependencies();
        GrpcAcceptorDependencies depsAcceptor = (GrpcAcceptorDependencies) depsService.getAcceptorDependencies();
        GrpcAcceptorController   controller   = depsAcceptor.getController();
        return controller.getLocalPort();
        }

    /**
     * Find the locally running gRPC proxy port.
     *
     * @return the local gRPC proxy port
     */
    public static int local()
        {
        return local(Coherence.DEFAULT_SCOPE);
        }

    /**
     * Find the locally running gRPC proxy port.
     *
     * @param sScope  the scope prefix
     *
     * @return the local gRPC proxy port
     */
    public static int local(String sScope)
        {
        return INSTANCE.findPort(sScope);
        }

    // ----- constants ------------------------------------------------------

    /**
     * A singleton instance of {@link FindGrpcProxyPort} to find the default proxy port.
     */
    public static final FindGrpcProxyPort INSTANCE = new FindGrpcProxyPort();

    // ----- data members ---------------------------------------------------

    /**
     * The scope prefix to use.
     */
    private final String m_sScope;
    }
