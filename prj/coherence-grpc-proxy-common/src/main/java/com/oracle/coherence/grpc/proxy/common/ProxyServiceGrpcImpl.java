/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import com.oracle.coherence.grpc.messages.proxy.v1.ProxyRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyResponse;
import com.oracle.coherence.grpc.services.proxy.v1.ProxyServiceGrpc;

import com.tangosol.net.grpc.GrpcDependencies;

import io.grpc.ServerServiceDefinition;

import io.grpc.stub.StreamObserver;

/**
 * The implementation of the generated {@link ProxyServiceGrpc.AsyncService}.
 */
public class ProxyServiceGrpcImpl
        extends BaseGrpcServiceImpl
        implements BindableGrpcProxyService, ProxyServiceGrpc.AsyncService
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link ProxyServiceGrpcImpl} with default configuration.
     *
     * @param dependencies  the {@link Dependencies} to use to configure the service
     */
    public ProxyServiceGrpcImpl(Dependencies dependencies)
        {
        super(dependencies, MBEAN_NAME, "GrpcProxy");
        }

    // ----- BindableGrpcProxyService methods -------------------------------

    @Override
    public final ServerServiceDefinition bindService()
        {
        return ProxyServiceGrpc.bindService(this);
        }

    // ----- NamedCacheChannelGrpc.NamedCacheChannelImplBase methods --------
    
    @Override
    public StreamObserver<ProxyRequest> subChannel(StreamObserver<ProxyResponse> observer)
        {
        ProxyServiceChannel         channel = new ProxyServiceChannel(this, observer);
        GrpcDependencies.ServerType type    = getDependencies().getServerType();
        if (type == GrpcDependencies.ServerType.Asynchronous)
            {
            return channel.async(f_executor);
            }
        return channel;
        }

    // ----- inner interface: Dependencies ----------------------------------

    /**
     * The dependencies to configure a {@link ProxyServiceGrpcImpl}.
     */
    public interface Dependencies
            extends BaseGrpcServiceImpl.Dependencies
        {
        }

    // ----- inner class: DefaultDependencies -------------------------------

    /**
     * The default {@link ProxyServiceGrpcImpl.Dependencies} implementation.
     */
    public static class DefaultDependencies
            extends BaseGrpcServiceImpl.DefaultDependencies
            implements Dependencies
        {
        public DefaultDependencies(GrpcDependencies.ServerType serverType)
            {
            super(serverType);
            }

        public DefaultDependencies(GrpcServiceDependencies deps)
            {
            super(deps);
            }

        public DefaultDependencies(Dependencies deps)
            {
            super(deps);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name to use for the management MBean.
     */
    public static final String MBEAN_NAME = "type=GrpcProxy";
    }
