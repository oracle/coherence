/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.grpc;

import com.oracle.coherence.grpc.proxy.common.BindableGrpcProxyService;
import com.oracle.coherence.grpc.proxy.common.BindableServiceFactory;

import com.oracle.coherence.grpc.proxy.common.GrpcServiceDependencies;
import com.tangosol.net.grpc.GrpcDependencies;

import java.util.List;

/**
 * The {@link BindableServiceFactory} to create Vector gRPC services.
 */
public class VectorBindableServiceFactory
        implements BindableServiceFactory
    {
    @Override
    public List<BindableGrpcProxyService> createServices(GrpcServiceDependencies deps)
        {
        VectorStoreService.Dependencies depsVector = new VectorStoreService.DefaultDependencies(deps);
        VectorStoreService              service;

        if (deps.getServerType() == GrpcDependencies.ServerType.Asynchronous)
            {
            // use the Async store on Netty to get off the 
            service = new AsyncVectorStoreService(depsVector);
            }
        else
            {
            service = new SyncVectorStoreService(depsVector);
            }

        return List.of(new VectorStoreServiceGrpcImpl(service));
        }
    }
