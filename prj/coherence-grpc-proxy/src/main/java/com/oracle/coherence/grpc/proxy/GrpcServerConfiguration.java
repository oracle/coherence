/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.proxy;

import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

/**
 * A class that can configure gRPC server builders prior to them being used to start the servers.
 * <p>
 * Implementations can be automatically discovered via Java's SPI mechanism.
 * For automatic discovery, the implementation must have a zero-argument constructor and include
 * a resource named {@code META-INF/services/com.oracle.coherence.grpc.proxy.GrpcServerConfiguration}
 * in their JAR. The file's contents should be the implementation's class name.
 *
 * @author Jonathan Knight  2020.09.24
 */
public interface GrpcServerConfiguration
    {
    /**
     * Configure the server builders.
     * <p>
     * The build method must not be called on either builder.
     *
     * @param serverBuilder           the {@link ServerBuilder} to configure
     * @param inProcessServerBuilder  the {@link InProcessServerBuilder} to configure
     */
    void configure(ServerBuilder<?> serverBuilder, InProcessServerBuilder inProcessServerBuilder);
    }
