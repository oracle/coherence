/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.proxy;

import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

import javax.annotation.Priority;

/**
 * A class that can provide an instance of a {@link io.grpc.ServerBuilder}
 * to be used by the gRPC proxy to create a server, and an instance of a
 * {@link InProcessServerBuilder} to build an in-process gRPC server.
 * <p>
 * Instances of this class will be discovered using the {@link java.util.ServiceLoader}.
 * If multiple instances are on the classpath the instance with the the highest priority
 * will be used. If multiple discovered instances share the highest priority the instance
 * used will be undetermined.
 *
 * @author Jonathan Knight  2020.11.24
 * @since 20.12
 */
public interface GrpcServerBuilderProvider
        extends Comparable<GrpcServerBuilderProvider>
    {
    /**
     * Returns a {@link ServerBuilder} that may be used to build
     * a gRPC server.
     *
     * @param nPort  the default port to bind to
     *
     * @return a {@link ServerBuilder} that may be used to build
     *         a gRPC server
     */
    ServerBuilder<?> getServerBuilder(int nPort);

    /**
     * Returns a {@link InProcessServerBuilder} that may be used to build
     * an in-process gRPC server.
     *
     * @param sName  the default name for the in-process server
     *
     * @return a {@link InProcessServerBuilder} that may be used to build
     *         an in-process gRPC server
     */
    InProcessServerBuilder getInProcessServerBuilder(String sName);

    /**
     * Obtain the priority of this {@link GrpcServerBuilderProvider}.
     * <p>
     * If multiple {@link GrpcServerBuilderProvider} instances are discovered
     * by the {@link java.util.ServiceLoader} then the one with the highest
     * priority will be used. If multiple instances have the same highest
     * priority then the instance used is undetermined.
     *
     * @return the priority of this {@link GrpcServerBuilderProvider}
     */
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

    /**
     * The default priority for providers without a specific priority.
     */
    int DEFAULT_PRIORITY = 0;

    /**
     * The default {@link GrpcServerBuilderProvider}.
     */
    GrpcServerBuilderProvider INSTANCE = new GrpcServerBuilderProvider()
        {
        @Override
        public ServerBuilder<?> getServerBuilder(int nPort)
            {
            return ServerBuilder.forPort(nPort);
            }

        @Override
        public InProcessServerBuilder getInProcessServerBuilder(String sName)
            {
            return InProcessServerBuilder.forName(sName);
            }
        };
    }
