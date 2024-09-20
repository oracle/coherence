/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common.v0;

import com.google.protobuf.Message;
import com.oracle.coherence.grpc.client.common.GrpcConnection;
import com.tangosol.util.UUID;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;

/**
 * The legacy version zero implementation of a {@link GrpcConnection}.
 */
public class GrpcConnectionV0
        implements GrpcConnection
    {
    /**
     * Create a version zero {@link GrpcConnectionV0}.
     *
     * @param channel  the underlying gRPC {@link Channel}
     */
    public GrpcConnectionV0(Channel channel)
        {
        f_channel = channel;
        }

    @Override
    public Channel getChannel()
        {
        return f_channel;
        }

    @Override
    public void connect()
        {
        }

    @Override
    public boolean isConnected()
        {
        return true;
        }

    @Override
    public void close()
        {
        }

    @Override
    public UUID getUUID()
        {
        return null;
        }

    @Override
    public String getProxyVersion()
        {
        return "Unknown";
        }

    @Override
    public int getProxyVersionEncoded()
        {
        return 0;
        }

    @Override
    public int getProtocolVersion()
        {
        return 0;
        }

    @Override
    public <T extends Message> T send(Message message)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public <T extends Message> CompletableFuture<T> poll(Message message)
        {
        return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }

    @Override
    public <T extends Message> void poll(Message message, StreamObserver<T> observer)
        {
        observer.onError(new UnsupportedOperationException());
        }

    @Override
    public <T extends Message> void addResponseObserver(Listener<T> listener)
        {
        }

    @Override
    public <T extends Message> void removeResponseObserver(Listener<T> listener)
        {
        }

    @Override
    public long getHeartbeatsSent()
        {
        return 0L;
        }

    @Override
    public long getLastHeartbeatTime()
        {
        return 0L;
        }

    @Override
    public long getHeartbeatsAcked()
        {
        return 0L;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying gRPC {@link Channel}.
     */
    private final Channel f_channel;
    }
