/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

/**
 * An interface implemented by gRPC proxy services.
 *
 * @author Jonathan Knight  2020.10.15
 */
public interface GrpcProxyService
    {
    /**
     * Obtain the gRPC metrics instance for this service.
     *
     * @return  the gRPC metrics instance for this service
     */
    GrpcProxyMetrics getMetrics();
    }
