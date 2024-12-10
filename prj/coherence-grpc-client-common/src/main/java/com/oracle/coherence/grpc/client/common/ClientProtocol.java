/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common;

/**
 * An interface implemented by classes that provide the actual gRPC
 * request and response layer. Different implementations of this
 * interface support different versions of the gRPC cache API
 * protocol.
 */
public interface ClientProtocol
    {
    /**
     * Return the protocol version.
     *
     * @return the protocol version
     */
    int getVersion();

    /**
     * Close this client.
     */
    void close();

    /**
     * Return {@code true} if this client protocol is active.
     *
     * @return {@code true} if this client protocol is active
     */
    boolean isActive();

    /**
     * Return the {@link GrpcConnection} being used.
     *
     * @return the {@link GrpcConnection} being used
     */
    GrpcConnection getConnection();
    }
