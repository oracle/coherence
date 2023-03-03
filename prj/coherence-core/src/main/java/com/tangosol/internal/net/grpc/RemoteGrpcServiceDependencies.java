/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
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
     */
    Expression<Boolean> isTracingEnabled();

    /**
     * Return the {@link DaemonPoolDependencies}.
     *
     * @return the {@link DaemonPoolDependencies}
     */
    DaemonPoolDependencies getDaemonPoolDependencies();
    }
