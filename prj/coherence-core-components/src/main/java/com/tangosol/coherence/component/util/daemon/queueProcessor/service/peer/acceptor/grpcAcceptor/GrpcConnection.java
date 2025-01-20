/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.grpcAcceptor;

import com.google.protobuf.Message;

import com.tangosol.coherence.component.net.extend.Connection;
import com.tangosol.coherence.component.net.extend.Protocol;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;

import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;

/**
 * A gRPC specific {@link Connection}.
 *
 * @param <Resp>  the type of the response message sent down
 *                the channel to the client
 *
 * @author Jonathan Knight  2025.01.25
 */
@SuppressWarnings("rawtypes")
public class GrpcConnection<Resp extends Message>
        extends Connection
    {
    public GrpcConnection()
        {
        }

    @Override
    protected GrpcChannel<Resp> createChannel()
        {
        GrpcChannel<Resp> channel = new GrpcChannel<>();
        channel.setConnection(this);
        return channel;
        }

    @Override
    @SuppressWarnings({"unchecked"})
    public void setMessageFactoryMap(Map map)
        {
        // we must ensure the map is writeable
        super.setMessageFactoryMap(new HashMap(map));
        }

    /**
     * Add a {@link Peer.Protocol.MessageFactory}.
     *
     * @param clz      they protocol class
     * @param factory  the factory to add
     * @param <P>      the type of the protocol
     */
    @SuppressWarnings("unchecked")
    public  <P extends Protocol> void addMessageFactory(Class<P> clz, Peer.Protocol.MessageFactory factory)
        {
        Map map = getMessageFactoryMap();
        if (map == null)
            {
            map = new HashMap();
            setMessageFactoryMap(map);
            }
        map.put(clz.getSimpleName(), factory);
        }

    public StreamObserver<Resp> getStreamObserver()
        {
        return m_observer;
        }

    public void setStreamObserver(StreamObserver<Resp> observer)
        {
        m_observer = observer;
        }

    // ----- data members ---------------------------------------------------

    private StreamObserver<Resp> m_observer;
    }
