/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.internal.extend.message.response;

import com.google.protobuf.Message;

import com.oracle.coherence.grpc.messages.common.v1.CollectionOfStringValues;

import com.tangosol.coherence.component.net.extend.message.response.GrpcResponse;

import com.tangosol.io.Serializer;

/**
 * A {@link GrpcResponse} that produces
 * a {@link CollectionOfStringValues}.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class CollectionOfStringValuesResponse
        extends BaseProxyResponse
    {
    public CollectionOfStringValuesResponse()
        {
        }

    @Override
    @SuppressWarnings("unchecked")
    public Message getMessage()
        {
        Iterable<String> col = (Iterable<String>) getResult();
        if (col == null)
            {
            return CollectionOfStringValues.getDefaultInstance();
            }
        return CollectionOfStringValues.newBuilder()
                .addAllValues(col)
                .build();
        }
    }
