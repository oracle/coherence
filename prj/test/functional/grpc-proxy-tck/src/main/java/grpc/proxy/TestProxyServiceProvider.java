/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy;

import com.oracle.coherence.grpc.proxy.common.ProxyServiceGrpcImpl;

import java.util.Optional;
import java.util.ServiceLoader;

public interface TestProxyServiceProvider
    {
    ProxyServiceGrpcImpl getProxyService(ProxyServiceGrpcImpl.Dependencies dependencies);

    static Optional<TestProxyServiceProvider> getProvider()
        {
        ServiceLoader<TestProxyServiceProvider> loader = ServiceLoader.load(TestProxyServiceProvider.class);
        return loader.findFirst();
        }
    }
