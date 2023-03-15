/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.grpc;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;

import com.tangosol.config.expression.LiteralExpression;

import com.tangosol.internal.net.service.extend.remote.DefaultRemoteCacheServiceDependencies;

import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.internal.util.DaemonPoolDependencies;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.io.SerializerFactory;

import com.tangosol.net.grpc.GrpcChannelDependencies;

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
    }
