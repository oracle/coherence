/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common;

import io.grpc.ManagedChannelBuilder;

/**
 * A class that can apply additional configuration to a
 * {@link ManagedChannelBuilder} before it is used to
 * build a {@link io.grpc.ManagedChannel}.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public interface GrpcChannelConfigurer
    {
    void apply(ManagedChannelBuilder<?> builder);
    }
