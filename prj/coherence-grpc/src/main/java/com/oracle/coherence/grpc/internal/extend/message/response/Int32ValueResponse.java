/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.internal.extend.message.response;

import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;

import com.tangosol.coherence.component.net.extend.message.response.GrpcResponse;

import com.tangosol.io.Serializer;

/**
 * A {@link GrpcResponse} that produces a {@link Int32Value}.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class Int32ValueResponse
        extends BaseProxyResponse
    {
    public Int32ValueResponse()
        {
        }

    @Override
    public Message getMessage()
        {
        Number n = (Number) getResult();
        return n == null ? null : Int32Value.of(n.intValue());
        }
    }
