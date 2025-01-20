/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import com.google.protobuf.Message;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.component.net.extend.message.GrpcMessageWrapper;

import com.tangosol.coherence.component.net.extend.message.response.GrpcResponse;

import com.tangosol.coherence.component.net.extend.messageFactory.GrpcMessageFactory;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.grpcAcceptor.GrpcChannel;

import com.tangosol.io.Serializer;

import io.grpc.Status;

import io.grpc.stub.StreamObserver;

/**
 * The {@link com.oracle.coherence.grpc.GrpcServiceProtocol} that uses
 * Extend messages.
 *
 * @author Jonathan Knight  2025.01.25
 */
@SuppressWarnings("unchecked")
public abstract class BaseExtendProxyProtocol<Req extends Message, Resp extends Message>
        extends BaseProxyProtocol<Req, Resp>
    {
    @Override
    public void close()
        {
        f_lock.lock();
        try
            {
            m_fClosed = true;
            m_destroyedIds.clear();
            }
        finally
            {
            f_lock.unlock();
            }
        super.close();
        }

    @Override
    protected void onRequestInternal(Req request, StreamObserver<Resp> observer)
        {
        try
            {
            int                           proxyId    = getProxyId(request);
            GrpcChannel<Resp>             channel    = m_serviceProxy.getGrpcChannel();
            Serializer                    serializer = getSerializer();
            GrpcMessageFactory<Req, Resp> factory;
            GrpcMessageWrapper            message;

            if (proxyId == 0)
                {
                factory = (GrpcMessageFactory<Req, Resp>) channel.getMessageFactory();
                message = factory.createRequestMessage(request, serializer);
                message.setChannel(channel);
                }
            else
                {
                GrpcChannel<Resp> subChannel = channel.getSubChannel(proxyId);
                if (subChannel == null)
                    {
                    throw new IllegalArgumentException("Invalid proxy id " + proxyId + " request=" + request);
                    }

                channel = subChannel;
                factory = (GrpcMessageFactory<Req, Resp>) subChannel.getMessageFactory();
                message = factory.createRequestMessage(request, serializer);
                message.setChannel(subChannel);
                }

            GrpcResponse response = message.getResponse();
            response.setProxyId(proxyId);
            response.setStreamObserver(observer);
            response.setSerializer(serializer);
            channel.receive(message);
            }
        catch (Throwable t)
            {
            Logger.err("Failed to process request", t);
            observer.onError(Status.INTERNAL
                    .withDescription("Failed to process request")
                    .withCause(t)
                    .asRuntimeException());
            }
        }

    /**
     * Return the proxy identifier for a request.
     *
     * @param request  the request containing the proxy identifier
     *
     * @return the proxy identifier for the request
     */
    protected abstract int getProxyId(Req request);

    // ----- data members ---------------------------------------------------

    /**
     * A flag indicating whether this proxy protocol is closed.
     */
    protected boolean m_fClosed;    
    }
