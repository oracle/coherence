/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.google.protobuf.Message;

import com.oracle.coherence.grpc.messages.proxy.v1.InitRequest;

import com.tangosol.io.Serializer;

import com.tangosol.util.UUID;

import io.grpc.stub.StreamObserver;

/**
 * A Coherence gRPC message protocol.
 *
 * @param <Req>   the type of the request messages
 * @param <Resp>  the type of the response messages
 */
public interface GrpcServiceProtocol<Req extends Message, Resp extends Message>
    {
    /**
     * Return the priority for this protocol.
     *
     * @return the priority for this protocol
     */
    default int getPriority()
        {
        return PRIORITY_NORMAL;
        }

    /**
     * Return the name of this protocol.
     *
     * @return  the name of this protocol
     */
    String getProtocol();

    /**
     * Return the version of the protocol.
     *
     * @return  the version of the protocol
     */
    int getVersion();

    /**
     * Return the minimum version of the protocol this instance supports.
     *
     * @return  the minimum version of the protocol this instance supports
     */
    int getSupportedVersion();

    /**
     * Return the type of the request messages.
     *
     * @return  the type of the request messages
     */
    Class<Req> getRequestType();

    /**
     * Return the type of the response messages.
     *
     * @return  the type of the response messages
     */
    Class<Resp> getResponseType();

    /**
     * Return the {@link Serializer} to use to serialize response data.
     *
     * @return the {@link Serializer} to use to serialize response data
     */
    Serializer getSerializer();

    /**
     * Initialise this protocol.
     *
     * @param service     the parent {@link GrpcService}
     * @param request     the init request to use to initialise the protocol
     * @param nVersion    the actual version of the protocol to use
     * @param clientUUID  the client {@link UUID}
     * @param observer    the {@link StreamObserver} to send non-request related responses (e.g. events)
     */
    void init(GrpcService service, InitRequest request, int nVersion, UUID clientUUID, StreamObserver<Resp> observer);

    /**
     * Handle a request.
     *
     * @param request   the request to handle
     * @param observer  the {@link StreamObserver} to send the responses to
     */
    void onRequest(Req request, StreamObserver<Resp> observer);

    /**
     * Close this protocol.
     */
    void close();

    /**
     * Handle an error.
     *
     * @param t the exception that was thrown
     */
    default void onError(Throwable t)
        {
        ErrorsHelper.logIfNotCancelled(t);
        close();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The default normal priority.
     */
    int PRIORITY_NORMAL = 0;
    }
