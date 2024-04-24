/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.client;

import com.oracle.coherence.grpc.client.common.GrpcChannelConfigurer;
import io.grpc.ManagedChannelBuilder;

public class GrpcChannelConfigurerStub
        implements GrpcChannelConfigurer
    {
    @Override
    public void apply(ManagedChannelBuilder<?> builder)
        {
        }
    }
