/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common;

import com.google.protobuf.Message;
import com.oracle.coherence.common.base.Predicate;
import com.tangosol.internal.net.grpc.RemoteGrpcServiceDependencies;
import com.tangosol.io.Serializer;
import com.tangosol.util.UUID;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;

/**
 * A connection to a Coherence gRPC proxy.
 */
public interface GrpcConnection
    {
    /**
     * Return the underlying gRPC {@link Channel}.
     *
     * @return the underlying gRPC {@link Channel}
     */
    Channel getChannel();

    /**
     * Create a connection to the server.
     */
    void connect();

    /**
     * Return {@code true} if this connection is connected.
     *
     * @return  {@code true} if this connection is connected
     */
    boolean isConnected();

    /**
     * Close the connection.
     *
     */
    void close();

    /**
     * Return the client {@link UUID}.
     *
     * @return the client {@link UUID}
     *
     * @throws IllegalStateException if the connection has not been initialized by
     *                               a prior call to {@link #connect()}
     */
    UUID getUUID();

    /**
     * Return the Coherence version of the proxy this connection is connected to.
     *
     * @return the Coherence version of the proxy this connection is connected to
     *
     * @throws IllegalStateException if the connection has not been initialized by
     *                               a prior call to {@link #connect()}
     */
    String getProxyVersion();

    /**
     * Return the encoded Coherence version of the proxy this connection is connected to.
     *
     * @return the encoded Coherence version of the proxy this connection is connected to
     *
     * @throws IllegalStateException if the connection has not been initialized by
     *                               a prior call to {@link #connect()}
     */
    int getProxyVersionEncoded();

    /**
     * Return the version of the requested protocol that the server supports.
     *
     * @return the version of the requested protocol that the server supports
     *
     * @throws IllegalStateException if the connection has not been initialized by
     *                               a prior call to {@link #connect()}
     */
    int getProtocolVersion();

    /**
     * Synchronously send a message to the server.
     *
     * @param message  the message to send
     * @param <T>      the typeof the response
     *
     * @return the response returned by the server
     */
    <T extends Message> T send(Message message);

    /**
     * Asynchronously send a message to the server.
     *
     * @param message  the message to send
     * @param <T>      the typeof the response
     *
     * @return a {@link CompletableFuture} that will be completed with the response 
     *         returned by the server
     */
    <T extends Message> CompletableFuture<T> poll(Message message);

    /**
     * Asynchronously send a message to the server.
     *
     * @param message   the message to send
     * @param observer  the {@link StreamObserver} to receive the responses
     * @param <T>       the typeof the response
     */
    <T extends Message> void poll(Message message, StreamObserver<T> observer);

    <T extends Message> void addResponseObserver(Listener<T> listener);

    <T extends Message> void removeResponseObserver(Listener<T> listener);

    /**
     * Return the number of heart beat messages sent.
     *
     * @return the number of heart beat messages sent
     */
    long getHeartbeatsSent();

    /**
     * Return the timestamp of the last heartbeat sent.
     *
     * @return the timestamp of the last heartbeat sent
     */
    long getLastHeartbeatTime();

    /**
     * Return the number of heart beat ack responses received.
     *
     * @return the number of heart beat ack responses received
     */
    long getHeartbeatsAcked();

    // ----- inner class: Listener ------------------------------------------

    record Listener<T extends Message>(StreamObserver<T> observer, Predicate<T> predicate)
        {
        }

    // ----- inner interface: DefaultDependencies ---------------------------

    /**
     * The dependencies to use to create a {@link GrpcConnection}.
     */
    interface Dependencies
        {
        /**
         * Return the protocol name.
         *
         * @return the protocol name
         */
        String getProtocolName();
        
        /**
         * Return the protocol version.
         *
         * @return the protocol version
         */
        int getVersion();
        
        /**
         * Return the minimum supported protocol version.
         *
         * @return the minimum supported protocol version
         */
        int getSupportedVersion();
        
        /**
         * Return the gRPC {@link Channel}.
         *
         * @return the gRPC {@link Channel}
         */
        Channel getChannel();

        /**
         * Return the parent gRPC service dependencies.
         *
         * @return the parent gRPC service dependencies
         */
        RemoteGrpcServiceDependencies getServiceDependencies();

        /**
         * Return the serializer to use to serialize Coherence request data.
         *
         * @return the serializer to use to serialize Coherence request data
         */
        Serializer getSerializer();
        }

    // ----- inner class: DefaultDependencies -------------------------------

    /**
     * The default implementation of the dependencies to use 
     * to create a {@link GrpcConnection}.
     */
    class DefaultDependencies
            implements Dependencies
        {
        public DefaultDependencies(String sName, RemoteGrpcServiceDependencies deps, Channel channel, 
                int nVersion, int nSupportedVersion, Serializer serializer)
            {
            this.f_sName             = sName;
            this.f_deps              = deps;
            this.f_channel           = channel;
            this.f_nVersion          = nVersion;
            this.f_nSupportedVersion = nSupportedVersion;
            this.f_serializer        = serializer;
            }

        @Override
        public String getProtocolName()
            {
            return f_sName;
            }

        @Override
        public int getVersion()
            {
            return f_nVersion;
            }

        @Override
        public int getSupportedVersion()
            {
            return f_nSupportedVersion;
            }

        @Override
        public Channel getChannel()
            {
            return f_channel;
            }

        @Override
        public RemoteGrpcServiceDependencies getServiceDependencies()
            {
            return f_deps;
            }

        @Override
        public Serializer getSerializer()
            {
            return f_serializer;
            }

        // ----- data members -----------------------------------------------
                  
        private final String f_sName;
        
        private final RemoteGrpcServiceDependencies f_deps;
        
        private final Channel f_channel;
        
        private final int f_nVersion;
        
        private final int f_nSupportedVersion;
        
        private final Serializer f_serializer;
        }
    }
