/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * A factory that can produce {@link BindableGrpcProxyService} instances.
 * <p>
 * Implementations of {@link BindableServiceFactory} are discovered at runtime using
 * the Java service loader and will be deployed to the Coherence gRPC proxy.
 */
public interface BindableServiceFactory
    {
    List<BindableGrpcProxyService> createServices(GrpcServiceDependencies depsService);

    /**
     * Discover all the {@link BindableServiceFactory} instances and return all
     * the BindableGrpcProxyService instances created by the factories.
     *
     * @param depsService  the {@link GrpcServiceDependencies} to use to create services
     *
     * @return the BindableGrpcProxyService instances created by the discovered
     *         {@link BindableServiceFactory} instances
     */
    static List<BindableGrpcProxyService> discoverServices(GrpcServiceDependencies depsService)
        {
        ServiceLoader<BindableServiceFactory> loader      = ServiceLoader.load(BindableServiceFactory.class);
        List<BindableGrpcProxyService>        listService = new ArrayList<>();
        for (BindableServiceFactory factory : loader)
            {
            List<BindableGrpcProxyService> list = factory.createServices(depsService);
            if (list != null)
                {
                listService.addAll(list);
                }
            }
        return listService;
        }
    }
