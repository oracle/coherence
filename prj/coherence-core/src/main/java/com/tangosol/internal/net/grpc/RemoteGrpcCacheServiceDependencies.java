/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.grpc;

/**
 * The RemoteGrpcCacheServiceDependencies interface provides a gRPC
 * RemoteCacheService with its external dependencies.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public interface RemoteGrpcCacheServiceDependencies
        extends RemoteGrpcServiceDependencies
    {
    /**
     * Returns the frequency in millis that heartbeats should be sent by the
     * proxy to the client bidirectional events channel.
     *
     * @return the frequency in millis that heartbeats should be sent by the
     *         proxy to the client bidirectional events channel
     */
    long getEventsHeartbeat();

    /**
     * The default heartbeat frequency value representing no heartbeats to be sent.
     */
    long NO_EVENTS_HEARTBEAT = 0L;
    }
