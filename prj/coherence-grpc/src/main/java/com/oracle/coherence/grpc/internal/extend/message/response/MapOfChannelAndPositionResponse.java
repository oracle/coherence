/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.internal.extend.message.response;

import com.google.protobuf.Message;

import com.oracle.coherence.grpc.TopicHelper;

import com.tangosol.coherence.component.net.extend.message.response.GrpcResponse;

import com.tangosol.net.topic.Position;

import java.util.Map;

/**
 * A {@link GrpcResponse} that produces a
 * {@link com.oracle.coherence.grpc.messages.topic.v1.MapOfChannelAndPosition}.
 *
 * @author Jonathan Knight  2025.01.25
 */
public class MapOfChannelAndPositionResponse
        extends BaseProxyResponse
    {
    public MapOfChannelAndPositionResponse()
        {
        }

    @Override
    @SuppressWarnings("unchecked")
    public Message getMessage()
        {
        Map<Integer, Position> map = (Map<Integer, Position>) getResult();
        return TopicHelper.toProtobufChannelAndPosition(map);
        }
    }
