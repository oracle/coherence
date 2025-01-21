/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common;

import com.google.protobuf.Message;
import com.tangosol.util.UUID;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;

/**
 * A {@link GrpcConnection} that delegates to a wrapped {@link GrpcConnection}.
 *
 * @author Jonathan Knight  2025.01.25
 */
public class WrapperGrpcConnection
        implements GrpcConnection
    {
    /**
     * Create a {@link WrapperGrpcConnection}.
     *
     * @param delegate  the {@link GrpcConnection} to delegate to
     */
    public WrapperGrpcConnection(GrpcConnection delegate)
        {
        f_delegate = delegate;
        }

    @Override
    public Channel getChannel()
        {
        return f_delegate.getChannel();
        }

    @Override
    public void connect()
        {
        f_delegate.connect();
        }

    @Override
    public boolean isConnected()
        {
        return f_delegate.isConnected();
        }

    @Override
    public void close()
        {
        f_delegate.close();
        }

    @Override
    public UUID getUUID()
        {
        return f_delegate.getUUID();
        }

    @Override
    public String getProxyVersion()
        {
        return f_delegate.getProxyVersion();
        }

    @Override
    public int getProxyVersionEncoded()
        {
        return f_delegate.getProxyVersionEncoded();
        }

    @Override
    public int getProtocolVersion()
        {
        return f_delegate.getProtocolVersion();
        }

    @Override
    public <T extends Message> T send(Message message)
        {
        return f_delegate.send(message);
        }

    @Override
    public <T extends Message> CompletableFuture<T> poll(Message message)
        {
        return f_delegate.poll(message);
        }

    @Override
    public <T extends Message> void poll(Message message, StreamObserver<T> observer)
        {
        f_delegate.poll(message, observer);
        }

    @Override
    public <T extends Message> void addResponseObserver(Listener<T> listener)
        {
        f_delegate.addResponseObserver(listener);
        }

    @Override
    public <T extends Message> void removeResponseObserver(Listener<T> listener)
        {
        f_delegate.removeResponseObserver(listener);
        }

    @Override
    public long getHeartbeatsSent()
        {
        return f_delegate.getHeartbeatsSent();
        }

    @Override
    public long getLastHeartbeatTime()
        {
        return f_delegate.getLastHeartbeatTime();
        }

    @Override
    public long getHeartbeatsAcked()
        {
        return f_delegate.getHeartbeatsAcked();
        }

    @Override
    public void addConnectionListener(ConnectionListener listener)
        {
        f_delegate.addConnectionListener(listener);
        }

    @Override
    public void removeConnectionListener(ConnectionListener listener)
        {
        f_delegate.removeConnectionListener(listener);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The wrapped {@link GrpcConnection}.
     */
    protected final GrpcConnection f_delegate;
    }
