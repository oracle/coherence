/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package netty.grpc.proxy;


import com.oracle.coherence.grpc.proxy.NettyNamedCacheService;
import com.oracle.coherence.grpc.proxy.common.v0.NamedCacheService;
import grpc.proxy.TestNamedCacheServiceProvider;

public class TestNamedCacheServiceProviderImpl
        implements TestNamedCacheServiceProvider
    {
    @Override
    public NamedCacheService getService(NamedCacheService.Dependencies dependencies)
        {
        return NettyNamedCacheService.newInstance(dependencies);
        }
    }
