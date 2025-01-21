/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net.extend.message.response;

import com.google.protobuf.Message;

import com.tangosol.coherence.component.net.extend.message.Response;

import com.tangosol.io.Serializer;

import io.grpc.stub.StreamObserver;

/**
 * A gRPC response message.
 *
 * @author Jonathan Knight  2025.01.25
 */
public abstract class GrpcResponse
        extends Response
    {
    /**
     * Default constructor.
     */
    protected GrpcResponse()
        {
        super(null, null, true);
        }

    /**
     * Set the proxy identifier for this message.
     *
     * @param proxyId  the proxy identifier for this message
     */
    public void setProxyId(int proxyId)
        {
        m_nProxyId = proxyId;
        }

    /**
     * Return the proxy identifier for this message.
     *
     * @return the proxy identifier for this message
     */
    public int getProxyId()
        {
        return m_nProxyId;
        }

    /**
     * Set the {@link StreamObserver} to send responses to.
     *
     * @param observer  the {@link StreamObserver} to send responses to
     */
    public void setStreamObserver(StreamObserver<? extends Message> observer)
        {
        m_observer = observer;
        }

    /**
     * Return the {@link StreamObserver} to send responses to.
     *
     * @return the {@link StreamObserver} to send responses to
     */
    public StreamObserver<? extends Message> getStreamObserver()
        {
        return m_observer;
        }

    /**
     * Set the {@link Serializer}.
     *
     * @param serializer  the {@link Serializer}
     */
    public void setSerializer(Serializer serializer)
        {
        m_serializer = serializer;
        }

    /**
     * Return the {@link Serializer}.
     *
     * @return the {@link Serializer}
     */
    public Serializer getSerializer()
        {
        return m_serializer;
        }

    /**
     * Return {@code true} if the {@link StreamObserver} should be
     * completed after returning the response.
     *
     * @return {@code true} if the {@link StreamObserver} should be
     *         completed after returning the response
     */
    public boolean completeStream()
        {
        return true;
        }

    /**
     * Create a protobuf response message.
     *
     * @return a protobuf response message
     */
    public abstract Message getProtoResponse();

    // ----- data members ---------------------------------------------------

    /**
     * The proxy identifier.
     */
    protected int m_nProxyId;

    /**
     * The {@link Serializer}.
     */
    protected Serializer m_serializer;

    /**
     * The {@link StreamObserver} to send responses to.
     */
    protected StreamObserver<? extends Message> m_observer;
    }
