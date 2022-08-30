/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.helidon.grpc.proxy;

import com.oracle.coherence.grpc.proxy.BindableGrpcProxyService;
import com.oracle.coherence.grpc.proxy.DefaultGrpcAcceptorController;
import com.oracle.coherence.grpc.proxy.GrpcMetricsInterceptor;
import com.oracle.coherence.grpc.proxy.GrpcServerController;
import com.tangosol.coherence.config.Config;

import com.tangosol.net.grpc.GrpcDependencies;

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
        if (!Config.getBoolean(GrpcDependencies.PROP_ENABLED, true))
            {
            GrpcRouting.Builder routing = context.routing();
            // we have to disable the default gRPC start-up
            GrpcServerController.INSTANCE.setEnabled(false);

            for (BindableGrpcProxyService service : DefaultGrpcAcceptorController.createGrpcServices())
                {
                GrpcMetricsInterceptor interceptor = new GrpcMetricsInterceptor(service.getMetrics());
                routing.register(service, rules -> rules.intercept(interceptor));
                }
            context.whenStarted().thenRun(GrpcServerController.INSTANCE::markStarted);
            }
        }
    }
