/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.grpc;

import com.tangosol.config.expression.Expression;
import com.tangosol.internal.net.service.extend.remote.RemoteCacheServiceDependencies;
import com.tangosol.internal.util.DaemonPoolDependencies;
import com.tangosol.net.grpc.GrpcChannelDependencies;

/**
 * The RemoteGrpcCacheServiceDependencies interface provides a gRPC
 * RemoteCacheService with its external dependencies.
 *
 * @author Jonathan Knight  2022.02.02
 * @since 23.03
 */
public interface RemoteGrpcServiceDependencies
        extends RemoteCacheServiceDependencies
    {
    /**
     * Return the name of the scope configured for this service.
     *
     * @return the name of the scope configured for this service
     */
    String getScopeName();

    /**
     * Return the name of the scope on the remote to connect to on the cluster.
     *
     * @return the name of the scope on the remote to connect to on the cluster
     */
    String getRemoteScopeName();

    /**
     * Return the ChannelProvider builder.
     *
     * @return the ChannelProvider builder
     */
    GrpcChannelDependencies getChannelDependencies();

    /**
     * Return the {@link Expression} that will produce the flag to
     * determine whether client tracing is enabled.
     *
     * @return the {@link Expression} that will produce the flag to
     *         determine whether client tracing is enabled
     *
     * @deprecated
     */
    @Deprecated
    Expression<Boolean> isTracingEnabled();

    /**
     * Return the {@link DaemonPoolDependencies}.
     *
     * @return the {@link DaemonPoolDependencies}
     */
    DaemonPoolDependencies getDaemonPoolDependencies();

    /**
     * Returns the deadline to use for gRPC requests.
     *
     * @return the deadline to use for gRPC requests
     */
    long getDeadline();

    /**
     * Returns the frequency in millis that heartbeats should be sent by the
     * proxy to the client bidirectional channel.
     *
     * @return the frequency in millis that heartbeats should be sent by the
     *         proxy to the client bidirectional channel
     */
    long getHeartbeatInterval();

    /**
     * Return the flag to determine whether heart beat messages should require an
     * ack response from the server.
     *
     * @return  that is {@code true} if heart beat messages should require an
     *          ack response from the server
     */
    boolean isRequireHeartbeatAck();

    /**
     * The default heartbeat frequency value representing no heartbeats to be sent.
     */
    long NO_EVENTS_HEARTBEAT = 0L;
    }
