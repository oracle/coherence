/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.tangosol.coherence.config.Config;

import io.helidon.grpc.server.GrpcRouting;

import io.helidon.microprofile.grpc.server.spi.GrpcMpContext;
import io.helidon.microprofile.grpc.server.spi.GrpcMpExtension;

/**
 * A Helidon Microprofile gRPC extension that will deploy the
 * gRPC proxy services to the helidon gRPC server and disable
 * the internal Coherence gRPC server.
 *
 * @author Jonathan Knight  2020.10.08
 */
public class HelidonGrpcMpExtension
        implements GrpcMpExtension
    {
    // ----- constructors ---------------------------------------------------

    /**
     * This class will be loaded via the ServiceLoader so must
     * have a public no-arg constructor.
     */
    public HelidonGrpcMpExtension()
        {
        }

    // ----- GrpcMpExtension methods ---------------------------------------

    @Override
    public void configure(GrpcMpContext context)
        {
        if (!Config.getBoolean(GrpcServerController.PROP_ENABLED, true))
            {
            GrpcRouting.Builder routing = context.routing();
            GrpcServerController.INSTANCE.setEnabled(false);

            for (BindableGrpcProxyService service : GrpcServerController.INSTANCE.createGrpcServices())
                {
                GrpcMetricsInterceptor interceptor = new GrpcMetricsInterceptor(service.getMetrics());
                routing.register(service, rules -> rules.intercept(interceptor));
                }
            context.whenStarted().thenRun(GrpcServerController.INSTANCE::markStarted);
            }
        }
    }
