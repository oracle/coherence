/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common.topics;

import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import com.oracle.coherence.common.base.Exceptions;

import com.oracle.coherence.grpc.client.common.GrpcConnection;
import com.oracle.coherence.grpc.client.common.WrapperGrpcConnection;

import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequest;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequestType;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceResponse;

import java.util.concurrent.CompletableFuture;

/**
 * A {@link GrpcConnection} with utility methods for handling
 * topic requests.
 *
 * @author Jonathan Knight  2025.01.25
 */
public class TopicServiceGrpcConnection
        extends WrapperGrpcConnection
    {
    public TopicServiceGrpcConnection(GrpcConnection delegate)
        {
        super(delegate);
        }

    @Override
    public <T extends Message> T send(Message message)
        {
        if (!message.getClass().equals(TopicServiceRequest.class))
            {
            throw new IllegalArgumentException("Message must be an instance of " + TopicServiceRequest.class.getName());
            }
        return super.send(message);
        }

    public TopicServiceResponse send(int nId, TopicServiceRequestType type, Message message)
        {
        TopicServiceRequest.Builder builder = TopicServiceRequest.newBuilder()
                .setProxyId(nId)
                .setType(type);

        if (message != null)
            {
            builder.setMessage(Any.pack(message));
            }
        else
            {
            builder.setMessage(Any.pack(Empty.getDefaultInstance()));
            }

        return super.send(builder.build());
        }

    /**
     * Asynchronously Send a request.
     *
     * @param id    the message destination identifier
     * @param type  the request message type to send
     *
     * @return a {@link CompletableFuture} that will complete with the message response
     */
    public CompletableFuture<TopicServiceResponse> poll(int id, TopicServiceRequestType type)
        {
        return poll(id, type, null);
        }

    /**
     * Asynchronously Send a {@link TopicServiceRequestType}.
     *
     * @param id the topic identifier
     * @param type     the request message type to send
     * @param message  the message to send in the {@link TopicServiceRequest}
     * @return a {@link CompletableFuture} that will complete with the message response
     */
    public CompletableFuture<TopicServiceResponse> poll(int id, TopicServiceRequestType type, Message message)
        {
        TopicServiceRequest.Builder builder = TopicServiceRequest.newBuilder()
                .setProxyId(id)
                .setType(type);

        if (message != null)
            {
            builder.setMessage(Any.pack(message));
            }
        else
            {
            builder.setMessage(Any.pack(Empty.getDefaultInstance()));
            }

        return f_delegate.poll(builder.build());
        }

    /**
     * Unpack a {@link Int32Value} from the {@link Any} value in a response.
     *
     * @param response  the response
     *
     * @return the unpacked {@link Int32Value}
     */
    public Int32Value unpackInteger(TopicServiceResponse response)
        {
        return unpackMessage(response, Int32Value.class);
        }

    /**
     * Unpack a value from the {@link Any} value in a {@link TopicServiceResponse}
     * {@link TopicServiceResponse#getMessage() message field}.
     *
     * @param response the {@link TopicServiceResponse}
     * @param type     they expected type of the message field
     * @return the unpacked value
     */
    public <M extends Message> M unpackMessage(TopicServiceResponse response, Class<M> type)
        {
        Any any = response.getMessage();
        try
            {
            return any.unpack(type);
            }
        catch (InvalidProtocolBufferException e)
            {
            String sMsg = String.format("Failed to unpack protobuf message of type %s expected type %s uri=%s",
                    response.getClass().getSimpleName(), type, any.getTypeUrl());
            throw Exceptions.ensureRuntimeException(e, sMsg);
            }
        }
    }
