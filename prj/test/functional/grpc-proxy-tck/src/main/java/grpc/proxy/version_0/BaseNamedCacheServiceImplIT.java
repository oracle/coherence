/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.version_0;

import com.oracle.coherence.grpc.proxy.common.v0.NamedCacheService;

/**
 * A base class for named cache gRPC service integration tests.
 */
public abstract class BaseNamedCacheServiceImplIT
        extends BaseVersionZeroGrpcIT
    {
    // ----- helper methods -------------------------------------------------

    /**
     * Create an instance of the {@link NamedCacheService} to use for testing.
     *
     * @return an instance of the {@link NamedCacheService} to use for testing
     */
    protected NamedCacheService createService()
        {
        NamedCacheService service = m_service;
        if (service == null)
            {
            service = m_service = createCacheService();
            }
        return service;
        }

    // ----- data members ---------------------------------------------------

    private NamedCacheService m_service;
    }
