/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.oracle.coherence.grpc.client.common.AbstractGrpcChannelFactory;
import com.oracle.coherence.grpc.client.common.GrpcChannelFactory;
import com.oracle.coherence.grpc.client.common.GrpcRemoteService;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.internal.net.grpc.RemoteGrpcServiceDependencies;
import com.tangosol.net.grpc.GrpcChannelDependencies;
import com.tangosol.net.messaging.ConnectionException;
import io.grpc.Channel;
import io.grpc.ChannelCredentials;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolverRegistry;

/**
 * A default implementation of {@link GrpcChannelFactory}.
 */
public class NettyGrpcChannelFactory
        extends AbstractGrpcChannelFactory
        implements GrpcChannelFactory
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link NettyGrpcChannelFactory}
     */
    public NettyGrpcChannelFactory()
        {
        NameResolverRegistry.getDefaultRegistry().register(this);
        }

    // ----- GrpcChannelFactory methods -------------------------------------

    @Override
    public int getPriority()
        {
        return GrpcChannelFactory.PRIORITY_NORMAL;
        }

    @Override
    public Channel getChannel(GrpcRemoteService<?> service)
        {
        try
            {
            RemoteGrpcServiceDependencies depsService = service.getDependencies();
            GrpcChannelDependencies       depsChannel = depsService.getChannelDependencies();
            ManagedChannelBuilder<?>      builder     = (ManagedChannelBuilder<?>) depsChannel.getChannelProvider()
                                                                    .orElse(createManagedChannelBuilder(service));

            return builder.build();
            }
        catch (Exception e)
            {
            throw new ConnectionException("Failed to create gRPC channel for service " + service.getServiceName(), e);
            }
        }

    @Override
    protected ChannelCredentials createChannelCredentials(SocketProviderBuilder builder)
        {
        return CredentialsHelper.createChannelCredentials(builder);
        }
    }
