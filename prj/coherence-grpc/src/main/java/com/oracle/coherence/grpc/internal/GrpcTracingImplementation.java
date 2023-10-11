/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.internal;

import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;

/**
 * Abstraction to provide tracing implementation-specific client and
 * server {@code gRPC} interceptors.
 * <p></p>
 * NOTE: This is an internal API and is subject to change without notice.
 *
 * @author rl 9.27.2023
 * @since 24.03
 */
public interface GrpcTracingImplementation
    {
    // ----- api ------------------------------------------------------------

    /**
     * Create a new tracing {@link ClientInterceptor}.
     *
     * @return a new tracing {@link ClientInterceptor}
     */
    ClientInterceptor createClientInterceptor();

    /**
     * Create a new tracing {@link ServerInterceptor}.
     *
     * @return a new tracing {@link ServerInterceptor}
     */
    ServerInterceptor createServerInterceptor();
    }
