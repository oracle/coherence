/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.grpc;

import com.tangosol.config.annotation.Injectable;

/**
 * A default implementation of {@link RemoteGrpcCacheServiceDependencies}.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class DefaultRemoteGrpcCacheServiceDependencies
        extends DefaultRemoteGrpcServiceDependencies
        implements RemoteGrpcCacheServiceDependencies
    {
    /**
     * Create a {@link DefaultRemoteGrpcCacheServiceDependencies}.
     */
    public DefaultRemoteGrpcCacheServiceDependencies()
        {
        this(null);
        }

    /**
     * Create a {@link DefaultRemoteGrpcCacheServiceDependencies} by copying
     * the specified {@link RemoteGrpcCacheServiceDependencies}.
     *
     * @param deps  the {@link RemoteGrpcCacheServiceDependencies} to copy
     */
    public DefaultRemoteGrpcCacheServiceDependencies(RemoteGrpcCacheServiceDependencies deps)
        {
        super(deps);
        }

    @Override
    public long getEventsHeartbeat()
        {
        return m_nEventsHeartbeat;
        }

    /**
     * Set the frequency in millis that heartbeats should be sent by the
     * proxy to the client bidirectional events channel.
     * <p/>
     * If the frequency is set to zero or less, then no heartbeats will be sent.
     *
     * @param nEventsHeartbeat the heartbeat frequency in millis
     */
    @Injectable("event-heartbeat-millis")
    public void setEventsHeartbeat(long nEventsHeartbeat)
        {
        m_nEventsHeartbeat = Math.max(NO_EVENTS_HEARTBEAT, nEventsHeartbeat);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The frequency in millis that heartbeats should be sent by the
     * proxy to the client bidirectional events channel
     */
    private long m_nEventsHeartbeat = NO_EVENTS_HEARTBEAT;
    }
