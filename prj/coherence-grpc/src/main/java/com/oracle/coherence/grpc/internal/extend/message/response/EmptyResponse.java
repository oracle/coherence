/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.internal.extend.message.response;

import com.google.protobuf.Empty;
import com.google.protobuf.Message;

import com.tangosol.coherence.component.net.extend.message.response.GrpcResponse;

import com.tangosol.io.Serializer;

/**
 * A {@link GrpcResponse} that produces
 * an {@link Empty} instance.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class EmptyResponse
        extends BaseProxyResponse
    {
    public EmptyResponse()
        {
        }

    @Override
    public Message getMessage()
        {
        return Empty.getDefaultInstance();
        }
    }
