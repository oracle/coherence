/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import io.helidon.microprofile.grpc.server.spi.GrpcMpContext;
import io.helidon.microprofile.grpc.server.spi.GrpcMpExtension;

/**
 * A {@link GrpcMpExtension} that will only be executed in an environment where
 * there is also a Helidon gRPC MP server. This ensures that the server is
 * running before the {@link CoherenceClientExtension} starts for uses-cases
 * where the client needs to connect to the same server.
 *
 * @author Jonathan Knight  2020.06.23
 * @since 20.06
 */
public class ClientGrpcMpExtension
        implements GrpcMpExtension
    {
    @Override
    public void configure(GrpcMpContext context)
        {
        context.whenStarted().thenRun(CoherenceClientExtension::setStarted);
        }
    }
