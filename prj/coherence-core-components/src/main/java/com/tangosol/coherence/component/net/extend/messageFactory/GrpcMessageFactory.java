/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net.extend.messageFactory;

import com.google.protobuf.Message;

import com.tangosol.coherence.component.net.extend.message.GrpcMessageWrapper;

import com.tangosol.coherence.component.net.extend.message.response.GrpcResponse;

import com.tangosol.io.Serializer;

import com.tangosol.net.messaging.Protocol;

/**
 * A {@link Protocol.MessageFactory} that can also produce messages
 * from Protobuf requests.
 *
 * @author Jonathan Knight  2025.01.25
 */
public interface GrpcMessageFactory<Req extends Message, Resp extends Message>
        extends Protocol.MessageFactory
    {
    /**
     * Create a {@link GrpcMessageWrapper} from a Protobuf {@link Message}.
     *
     * @param request     the {@link Message} to create the {@link GrpcMessageWrapper} from
     * @param serializer  the {@link Serializer} to use to deserialize binary payloads
     * @param <M>         the expected type of the message to return
     *
     * @return a {@link GrpcMessageWrapper} created from a Protobuf {@link Message}
     */
    <M extends GrpcMessageWrapper> M createRequestMessage(Req request, Serializer serializer);

    /**
     * Create a response {@link Message} that will be wrapped in a proxy response
     * before being sent to the response stream observer.
     *
     * @param response  the {@link GrpcResponse} to convert to a {@link Message}
     *
     * @return the response {@link Message}
     */
    Resp createResponse(GrpcResponse response);

    /**
     * Convert a Coherence {@link com.tangosol.net.messaging.Message}
     * into a corresponding Protobuf {@link Message}.
     *
     * @param message   the Coherence message to convert
     * @param nProxyId  the proxy identifier for the message
     *
     * @return the corresponding Protobuf {@link Message}
     */
    Resp toProtoMessage(com.tangosol.net.messaging.Message message, int nProxyId);
    }
