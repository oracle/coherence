/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.helidon;

import com.oracle.coherence.grpc.proxy.common.BindableGrpcProxyService;
import com.oracle.coherence.grpc.proxy.common.BindableServiceFactory;
import com.oracle.coherence.grpc.proxy.common.GrpcServiceDependencies;
import com.oracle.coherence.grpc.proxy.common.ProxyServiceGrpcImpl;
import com.oracle.coherence.grpc.proxy.common.v0.NamedCacheService;
import com.oracle.coherence.grpc.proxy.common.v0.NamedCacheServiceGrpcImpl;

import com.tangosol.net.grpc.GrpcDependencies;

import java.util.List;

/**
 * The Helidon {@link BindableServiceFactory}.
 */
public class HelidonBindableServiceFactory
        implements BindableServiceFactory
    {
    @Override
    public List<BindableGrpcProxyService> createServices(GrpcServiceDependencies depsService)
        {
        if (depsService.getServerType() == GrpcDependencies.ServerType.Synchronous)
            {
            ProxyServiceGrpcImpl.Dependencies     depsProxy = new ProxyServiceGrpcImpl.DefaultDependencies(depsService);
            NamedCacheService.DefaultDependencies depsCache = new NamedCacheService.DefaultDependencies(depsService);
            depsCache.setExecutor(Runnable::run);

            return List.of(new NamedCacheServiceGrpcImpl(HelidonNamedCacheService.newInstance(depsCache)),
                    new ProxyServiceGrpcImpl(depsProxy));
            }
        return List.of();
        }
    }
