/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.grpcAcceptor;

import com.google.protobuf.Message;

import com.tangosol.coherence.component.net.extend.Channel;
import com.tangosol.coherence.component.net.extend.Connection;

import com.tangosol.coherence.component.net.extend.message.response.GrpcResponse;

import com.tangosol.coherence.component.net.extend.messageFactory.GrpcMessageFactory;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor;

import io.grpc.stub.StreamObserver;

import java.util.Optional;

/**
 * An implementation of a {@link Channel} used by gRPC proxies.
 *
 * @param <Resp>  the type of the response message sent down
 *                the channel to the client
 *
 * @author Jonathan Knight  2025.01.25
 */
public class GrpcChannel<Resp extends Message>
        extends Channel
    {
    public GrpcChannel()
        {
        }

    @Override
    @SuppressWarnings("unchecked")
    public GrpcConnection<Resp> getConnection()
        {
        return (GrpcConnection<Resp>) super.getConnection();
        }

    @Override
    public void setConnection(com.tangosol.net.messaging.Connection connection)
        {
        super.setConnection(connection);
        }

    /**
     * Accept requests on the specified sub-channel.
     *
     * @param nChannelId  the channel identifier
     */
    public void acceptChannelRequest(int nChannelId)
        {
        Connection connection = getConnection();
        connection.acceptChannelRequest(nChannelId, null, null);
        }

    /**
     * Add a {@link com.tangosol.coherence.component.net.extend.MessageFactory}.
     *
     * @param clz      they protocol class
     * @param factory  the factory to add
     * @param <P>      the type of the protocol
     */
    @SuppressWarnings("unchecked")
    protected <P extends Peer.Protocol> void addMessageFactory(Class<P> clz, Acceptor.MessageFactory factory)
        {
        getConnection().getMessageFactoryMap().put(clz.getSimpleName(), factory);
        }

    /**
     * Obtain a sub-channel.
     *
     * @param nChannelId  the channel identifier
     *
     * @return the sub-channel or an empty {@link Optional} if there
     *         is no sub-channel with the specified identifier
     */
    @SuppressWarnings("unchecked")
    public GrpcChannel<Resp> getSubChannel(int nChannelId)
        {
        return (GrpcChannel<Resp>) getConnection().getChannel(nChannelId);
        }

    @Override
    @SuppressWarnings("unchecked")
    public void post(com.tangosol.net.messaging.Message message)
        {
        if (message instanceof GrpcResponse)
            {
            GrpcResponse         response = (GrpcResponse) message;
            StreamObserver<Resp> observer = (StreamObserver<Resp>) response.getStreamObserver();
            if (response.isFailure())
                {
                Throwable error = (Throwable) response.getResult();
                observer.onError(error);
                }
            else
                {
                GrpcMessageFactory<?, Resp> factory       = (GrpcMessageFactory<?, Resp>) getMessageFactory();
                Resp                        protoResponse = factory.createResponse(response);
                observer.onNext(protoResponse);
                if (response.completeStream())
                    {
                    observer.onCompleted();
                    }
                }
            }
        else
            {
            int nId = getId();
            if (nId != 0)
                {
                GrpcMessageFactory<?, Resp> factory      = (GrpcMessageFactory<?, Resp>) getMessageFactory();
                Resp                        protoMessage = factory.toProtoMessage(message, nId);
                if (protoMessage != null)
                    {
                    getConnection().getStreamObserver().onNext(protoMessage);
                    }
                }
            else
                {
                super.post(message);
                }
            }
        }
    }
