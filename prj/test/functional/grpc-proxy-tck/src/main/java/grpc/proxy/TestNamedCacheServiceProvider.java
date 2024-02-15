/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy;

import com.oracle.coherence.grpc.proxy.common.BaseNamedCacheServiceImpl;
import com.oracle.coherence.grpc.proxy.common.NamedCacheService;

import java.util.Optional;
import java.util.ServiceLoader;

public interface TestNamedCacheServiceProvider
    {
    NamedCacheService getService(NamedCacheService.Dependencies dependencies);

    default BaseNamedCacheServiceImpl getBaseService(NamedCacheService.Dependencies dependencies)
        {
        return (BaseNamedCacheServiceImpl) getService(dependencies);
        }

    static Optional<TestNamedCacheServiceProvider> getProvider()
        {
        ServiceLoader<TestNamedCacheServiceProvider> loader = ServiceLoader.load(TestNamedCacheServiceProvider.class);
        return loader.findFirst();
        }
    }
