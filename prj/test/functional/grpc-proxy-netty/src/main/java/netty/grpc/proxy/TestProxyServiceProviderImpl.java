/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package netty.grpc.proxy;

import com.oracle.coherence.grpc.proxy.common.ProxyServiceGrpcImpl;
import com.tangosol.net.grpc.GrpcDependencies;
import grpc.proxy.TestProxyServiceProvider;

public class TestProxyServiceProviderImpl
        implements TestProxyServiceProvider
    {
    @Override
    public ProxyServiceGrpcImpl getProxyService(ProxyServiceGrpcImpl.Dependencies dependencies)
        {
        ProxyServiceGrpcImpl.DefaultDependencies deps = new ProxyServiceGrpcImpl.DefaultDependencies(dependencies)
            {
            @Override
            public GrpcDependencies.ServerType getServerType()
                {
                return GrpcDependencies.ServerType.Asynchronous;
                }
            };
        return new ProxyServiceGrpcImpl(deps);
        }
    }
