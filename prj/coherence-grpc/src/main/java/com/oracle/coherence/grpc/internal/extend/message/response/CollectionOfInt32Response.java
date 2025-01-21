/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.internal.extend.message.response;

import com.google.protobuf.Message;

import com.oracle.coherence.grpc.MessageHelper;

import com.tangosol.coherence.component.net.extend.message.response.GrpcResponse;

import com.tangosol.io.Serializer;

/**
 * A {@link GrpcResponse} that produces a
 * {@link com.oracle.coherence.grpc.messages.common.v1.CollectionOfInt32}.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class CollectionOfInt32Response
        extends BaseProxyResponse
    {
    public CollectionOfInt32Response()
        {
        }

    @Override
    @SuppressWarnings("unchecked")
    public Message getMessage()
        {
        Object o = getResult();
        if (o instanceof int[])
            {
            return MessageHelper.toCollectionOfInt32((int[]) o);
            }
        return MessageHelper.toCollectionOfInt32((Iterable<Integer>) o);
        }
    }
