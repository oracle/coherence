/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net.extend.proxy;

import com.google.protobuf.Message;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.grpcAcceptor.GrpcChannel;

import com.tangosol.net.messaging.Channel;

/**
 * A gRPC extend proxy.
 *
 * @author Jonathan Knight  2025.01.25
 */
public interface GrpcExtendProxy<Resp extends Message>
        extends Channel.Receiver
    {
    /**
     * Return the {@link GrpcChannel} used by this proxy.
     *
     * @return the {@link GrpcChannel} used by this proxy
     */
    @SuppressWarnings("unchecked")
    default GrpcChannel<Resp> getGrpcChannel()
        {
        return (GrpcChannel<Resp>) getChannel();
        }

    /**
     * Return the {@link Channel} used by this proxy.
     *
     * @return the {@link Channel} used by this proxy
     */
    Channel getChannel();
    }
