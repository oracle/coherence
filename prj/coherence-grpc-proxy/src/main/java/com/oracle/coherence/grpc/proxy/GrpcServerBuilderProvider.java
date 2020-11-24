/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.proxy;

import io.grpc.ServerBuilder;

import javax.annotation.Priority;

/**
 * A class that can provide an instance of a {@link io.grpc.ServerBuilder}
 * to be used by the gRPC proxy to create a server.
 * <p>
 * Instances of this class will be discovered using the {@link java.util.ServiceLoader}.
 * If multiple instances ae on the classpath the instance with the the highest priority
 * will be used.
 *
 * @author Jonathan Knight  2020.11.24
 */
public interface GrpcServerBuilderProvider
        extends Comparable<GrpcServerBuilderProvider>
    {
    ServerBuilder<?> getServerBuilder();

    default int getPriority()
        {
        Priority annotation = getClass().getAnnotation(Priority.class);
        return annotation == null ? DEFAULT_PRIORITY : annotation.value();
        }

    @Override
    default int compareTo(GrpcServerBuilderProvider o)
        {
        // order with the highest priority first
        return 0 - Integer.compare(getPriority(), o.getPriority());
        }

    int DEFAULT_PRIORITY = 0;
    }
