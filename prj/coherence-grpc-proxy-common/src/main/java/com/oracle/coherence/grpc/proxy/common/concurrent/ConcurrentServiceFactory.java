/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common.concurrent;


import com.oracle.coherence.concurrent.config.ConcurrentConfiguration;
import com.oracle.coherence.grpc.proxy.common.BindableGrpcProxyService;
import com.oracle.coherence.grpc.proxy.common.BindableServiceFactory;
import com.oracle.coherence.grpc.proxy.common.GrpcServiceDependencies;

import java.util.List;

public class ConcurrentServiceFactory
        implements BindableServiceFactory
    {
    @Override
    public List<BindableGrpcProxyService> createServices(GrpcServiceDependencies depsService)
        {
        try
            {
            ConcurrentConfiguration.get();
            return List.of();
            }
        catch (Throwable e)
            {
            // Coherence concurrent is not available
            return List.of();
            }
        }
    }
