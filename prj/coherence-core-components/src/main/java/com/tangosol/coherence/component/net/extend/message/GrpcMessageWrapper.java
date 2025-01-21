/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net.extend.message;

import com.google.protobuf.Any;

import com.tangosol.coherence.component.net.extend.message.response.GrpcResponse;
import com.tangosol.io.Serializer;

import com.tangosol.net.messaging.Message;

/**
 * A {@link Message} implementation that wraps a protobuf message.
 *
 * @author Jonathan Knight  2025.01.25
 */
public interface GrpcMessageWrapper
        extends Message
    {
    /**
     * Set the wrapped protobuf message.
     *
     * @param any         the wrapped protobuf message
     * @param serializer  the serializer to deserialize binary payloads
     */
    void setProtoMessage(Any any, Serializer serializer);

    /**
     * Return the message response.
     *
     * @return the message response
     */
    GrpcResponse getResponse();
    }
