/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.internal.extend.message.response;

import com.google.protobuf.Message;

import com.google.protobuf.StringValue;

import com.tangosol.coherence.component.net.extend.message.response.GrpcResponse;

import com.tangosol.io.Serializer;

/**
 * A {@link GrpcResponse} that produces a {@link StringValue}.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class StringValueResponse
        extends BaseProxyResponse
    {
    public StringValueResponse()
        {
        }

    @Override
    public Message getMessage()
        {
        Object o = getResult();
        return o == null ? null : StringValue.of(String.valueOf(o));
        }
    }
