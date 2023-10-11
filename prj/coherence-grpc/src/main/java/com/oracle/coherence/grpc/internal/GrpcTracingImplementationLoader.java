/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.internal;

/**
 * Implementations represent a tracing implementation-specific gRPC
 * interceptors.
 * <p></p>
 * NOTE: This is an internal API and is subject to change without notice.
 *
 * @author rl 9.27.2023
 * @since 24.03
 */
public interface GrpcTracingImplementationLoader
    {
    /**
     * Return a {@link GrpcTracingImplementation} if the appropriate
     * dependencies are available, otherwise returns {@code false}
     *
     * @return a {@link GrpcTracingImplementation} if the appropriate
     *         dependencies are available, otherwise returns {@code false}
     */
    GrpcTracingImplementation getGrpcTracingImplementation();
    }
