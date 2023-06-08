/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.application.Context;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.net.grpc.GrpcAcceptorController;

/**
 * The dependencies for a gRPC proxy service's acceptor.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public interface GrpcAcceptorDependencies
        extends AcceptorDependencies
    {
    /**
     * Return the {@link SocketProviderBuilder} that may be used by the gRPC server.
     *
     * @return the {@link SocketProviderBuilder}
     */
    SocketProviderBuilder getSocketProviderBuilder();

    /**
     * Return the local address string.
     *
     * @return the local address string
     */
    String getLocalAddress();

    /**
     * Return the local port.
     *
     * @return the local port
     */
    int getLocalPort();

    /**
     * Returns the name of the in-process gRPC server.
     *
     * @return the name of the in-process gRPC server
     */
    String getInProcessName();

    /**
     * Returns the gRPC server controller.
     *
     * @return the gRPC server controller
     */
    GrpcAcceptorController getController();

    /**
     * Return the max page size for the Channelz service.
     *
     * @return the max page size for the Channelz service
     */
    int getChannelzPageSize();

    /**
     * Return the optional application {@link Context}.
     *
     * @return the optional application {@link Context}
     */
    Context getContext();
    }
