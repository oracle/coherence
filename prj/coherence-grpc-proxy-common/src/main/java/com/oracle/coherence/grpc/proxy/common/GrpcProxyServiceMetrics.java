/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

/**
 * A provider of metrics for a gRPC proxy service.
 */
public interface GrpcProxyServiceMetrics
    {
    void addRequestDuration(long nanos);

    /**
     * Add a timing sample to the metrics.
     *
     * @param nanos  the request time in nanos.
     */
    void addMessageDuration(long nanos);

    /**
     * Update the successful request meter.
     */
    void markSuccess();

    /**
     * Update the failed request meter.
     */
    void markError();

    /**
     * Update the messages sent meter.
     */
    void markSent();

    /**
     * Update the messages received meter.
     */
    void markReceived();
    }
