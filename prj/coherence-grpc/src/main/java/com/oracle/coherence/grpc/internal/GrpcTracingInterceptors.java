/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.internal;

import com.tangosol.internal.tracing.LegacyXmlTracingHelper;
import com.tangosol.internal.tracing.TracingHelper;
import com.tangosol.internal.tracing.TracingShim;
import com.tangosol.net.CacheFactory;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;

import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;

import java.util.ServiceLoader;

/**
 * Utility class to obtain a tracing implementation-specific gRPC
 * tracing interceptor.
 * <p></p>
 * NOTE: This is an internal API and is subject to change without notice.
 *
 * @author rl 9.27.2023
 * @since 24.03
 */
public final class GrpcTracingInterceptors
    {
    // ----- constructors ---------------------------------------------------

    /**
     * No instances.
     */
    private GrpcTracingInterceptors()
        {
        }

    // ----- api ------------------------------------------------------------

    /**
     * Return the {@link ClientInterceptor} for the supported
     * tracing runtime.
     *
     * @return a {@link ClientInterceptor} for the supported
     *         tracing runtime
     */
    public static ClientInterceptor getClientInterceptor()
        {
        return LOADED == null || !TracingHelper.isEnabled() ? null : LOADED.createClientInterceptor();
        }

    /**
     * Return the {@link ServerInterceptor} for the supported
     * tracing runtime.
     *
     * @return a {@link ServerInterceptor} for the supported
     *         tracing runtime
     */
    public static ServerInterceptor getServerInterceptor()
        {
        return LOADED == null || !TracingHelper.isEnabled() ? null : LOADED.createServerInterceptor();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Load the {@link GrpcTracingImplementation} via the
     * {@link ServiceLoader}.
     *
     * @return the first {@code non-null} {@link GrpcTracingImplementation}
     *         or {@code null}, if no suitable gRPC tracing support is found
     */
    private static GrpcTracingImplementation load()
        {
        ServiceLoader<GrpcTracingImplementationLoader> loader =
                ServiceLoader.load(GrpcTracingImplementationLoader.class,
                                   Base.getContextClassLoader());

        for (GrpcTracingImplementationLoader l : loader)
            {
            GrpcTracingImplementation impl = l.getGrpcTracingImplementation();
            if (impl != null)
                {
                return impl;
                }
            }

        return null;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The gRPC tracing implementation.
     */
    private static final GrpcTracingImplementation LOADED;

    static
        {
        LOADED = load();
        }
    }
