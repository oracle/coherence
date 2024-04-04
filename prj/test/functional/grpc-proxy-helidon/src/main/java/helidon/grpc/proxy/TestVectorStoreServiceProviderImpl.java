/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package helidon.grpc.proxy;

import com.oracle.coherence.ai.grpc.SyncVectorStoreService;

import com.oracle.coherence.ai.grpc.VectorStoreService;

import grpc.proxy.TestVectorStoreServiceProvider;

public class TestVectorStoreServiceProviderImpl
        implements TestVectorStoreServiceProvider
    {
    @Override
    public VectorStoreService getService(VectorStoreService.Dependencies dependencies)
        {
        return new SyncVectorStoreService(dependencies);
        }
    }
