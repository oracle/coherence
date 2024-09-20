/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.oracle.coherence.grpc.proxy.common.BindableGrpcProxyService;
import com.oracle.coherence.grpc.proxy.common.BindableServiceFactory;
import com.oracle.coherence.grpc.proxy.common.GrpcServiceDependencies;
import com.oracle.coherence.grpc.proxy.common.v0.NamedCacheService;
import com.oracle.coherence.grpc.proxy.common.v0.NamedCacheServiceGrpcImpl;

import com.oracle.coherence.grpc.proxy.common.ProxyServiceGrpcImpl;
import com.tangosol.net.grpc.GrpcDependencies;

import java.util.List;

/**
 * The Netty {@link BindableServiceFactory}.
 */
public class NettyBindableServiceFactory
        implements BindableServiceFactory
    {
    @Override
    public List<BindableGrpcProxyService> createServices(GrpcServiceDependencies depsService)
        {
        if (depsService.getServerType() == GrpcDependencies.ServerType.Asynchronous)
            {
            NamedCacheService.DefaultDependencies depsCache = new NamedCacheService.DefaultDependencies(depsService);
            ProxyServiceGrpcImpl.Dependencies     depsProxy = new ProxyServiceGrpcImpl.DefaultDependencies(depsService);

            return List.of(new NamedCacheServiceGrpcImpl(NettyNamedCacheService.newInstance(depsCache)),
                    new ProxyServiceGrpcImpl(depsProxy));
            }
        return List.of();
        }
    }
