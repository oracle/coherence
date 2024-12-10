/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.internal;

import com.tangosol.internal.tracing.TracingHelper;

import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;

import io.opentracing.contrib.grpc.TracingClientInterceptor;
import io.opentracing.contrib.grpc.TracingServerInterceptor;

/**
 * TODO: docs
 *
 * @author rl 9.27.2023
 * @since 24.03
 */
public class OpenTracingGrpcTracingImplementationLoader
        extends AbstractGrpcTracingImplementationLoader
    {
    // ----- AbstractGrpcTracingInterceptorLoader interface -----------------

    @Override
    protected String getSearchClassName()
        {
        return CLASS_NAME;
        }

    @Override
    public GrpcTracingImplementation newInstance()
        {
        return new OpenTracingGrpcInterceptors();
        }

    // ----- inner class: OpenTracingGrpcHelper -----------------------------

    /**
     * The {@link GrpcTracingImplementation} for {@code OpenTracing}.
     */
    static class OpenTracingGrpcInterceptors
            implements GrpcTracingImplementation
        {
        // ----- GrpcTracingHelper interface --------------------------------

        @Override
        public ClientInterceptor createClientInterceptor()
            {
            return TracingClientInterceptor.newBuilder()
                    .withTracer(TracingHelper.getTracer().underlying())
                    .build();
            }

        public ServerInterceptor createServerInterceptor()
            {
            return TracingServerInterceptor.newBuilder()
                    .withTracer(TracingHelper.getTracer().underlying())
                    .build();
            }
        }
    // ----- constants ------------------------------------------------------

    /**
     * The class name that indicates the {@code OpenTracing} {@code gRPC }
     * tracing implementation is present on the classpath.
     */
    private static final String CLASS_NAME = "io.opentracing.contrib.grpc.TracingClientInterceptor";
    }
