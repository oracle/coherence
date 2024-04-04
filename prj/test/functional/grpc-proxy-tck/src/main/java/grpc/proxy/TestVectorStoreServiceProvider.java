/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy;

import com.oracle.coherence.ai.grpc.VectorStoreService;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * A factory to provide instances of a test {@link VectorStoreService}.
 */
public interface TestVectorStoreServiceProvider
    {
    /**
     * Return the {@link VectorStoreService} to be tested.
     *
     * @param dependencies  the {@link VectorStoreService.Dependencies} to use to create the service
     *
     * @return the {@link VectorStoreService} to be tested
     */
    VectorStoreService getService(VectorStoreService.Dependencies dependencies);

    /**
     * Discover and return a {@link TestVectorStoreServiceProvider} using the
     * Java {@link ServiceLoader}.
     *
     * @return a discovered a {@link TestVectorStoreServiceProvider} or an empty optional
     *         if no {@link TestVectorStoreServiceProvider} is present.
     */
    static Optional<TestVectorStoreServiceProvider> getProvider()
        {
        ServiceLoader<TestVectorStoreServiceProvider> loader = ServiceLoader.load(TestVectorStoreServiceProvider.class);
        return loader.findFirst();
        }
    }
