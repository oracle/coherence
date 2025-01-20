/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.grpc;

/**
 * A default implementation of {@link RemoteGrpcTopicServiceDependencies}.
 *
 * @author Jonathan Knight  2025.01.01
 */
public class DefaultRemoteGrpcTopicServiceDependencies
        extends DefaultRemoteGrpcServiceDependencies
        implements RemoteGrpcTopicServiceDependencies
    {
    /**
     * Create a {@link DefaultRemoteGrpcTopicServiceDependencies}.
     */
    public DefaultRemoteGrpcTopicServiceDependencies()
        {
        this(null);
        }

    /**
     * Create a {@link DefaultRemoteGrpcTopicServiceDependencies} by copying
     * the specified {@link RemoteGrpcTopicServiceDependencies}.
     *
     * @param deps  the {@link RemoteGrpcTopicServiceDependencies} to copy
     */
    public DefaultRemoteGrpcTopicServiceDependencies(RemoteGrpcTopicServiceDependencies deps)
        {
        super(deps);
        }
    }
