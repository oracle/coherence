/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import io.grpc.BindableService;

/**
 * An interface implemented by bindable gRPC proxy services.
 *
 * @author Jonathan Knight  2020.10.15
 */
public interface BindableGrpcProxyService
        extends GrpcProxyService, BindableService
    {
    /**
     * Close this service and clean up any state.
     */
    void close();

    /**
     * Return {@code true} if this service is enabled, otherwise {@code false}.
     *
     * @return {@code true} if this service is enabled, otherwise {@code false}
     */
    default boolean isEnabled()
        {
        return true;
        }
    }
