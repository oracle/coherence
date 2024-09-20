/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.internal;

import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;

import io.opentelemetry.api.GlobalOpenTelemetry;

import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;

/**
 * A {@link GrpcTracingImplementationLoader} implementation for
 * {@code OpenTelemetry}.
 *
 * @author rl 9.27.2023
 * @since 24.03
 */
public class OpenTelemetryGrpcImplementationLoader
        extends AbstractGrpcTracingImplementationLoader
    {
    // ----- AbstractGrpcTracingInterceptorsLoader interface ----------------

    @Override
    protected String getSearchClassName()
        {
        return CLASS_NAME;
        }

    @Override
    public GrpcTracingImplementation newInstance()
        {
        return new OpenTelemetryGrpcTracingImplementation();
        }

    // ----- inner class: OpenTelemetryGrpcHelper ---------------------------

    /**
     * The {@link GrpcTracingImplementation} for {@code OpenTelemetry}.
     */
    static class OpenTelemetryGrpcTracingImplementation
            implements GrpcTracingImplementation
        {
        // ----- GrpcTracingHelper interface --------------------------------

        @Override
        public ClientInterceptor createClientInterceptor()
            {
            return GrpcTelemetry.create(GlobalOpenTelemetry.get()).newClientInterceptor();
            }

        @Override
        public ServerInterceptor createServerInterceptor()
            {
            return GrpcTelemetry.create(GlobalOpenTelemetry.get()).newServerInterceptor();
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The class name that indicates the {@code OpenTelemetry} {@code gRPC }
     * tracing implementation is present on the classpath.
     */
    private static final String CLASS_NAME = "io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry";
    }
