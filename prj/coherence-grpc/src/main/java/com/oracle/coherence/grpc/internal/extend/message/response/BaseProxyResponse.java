/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.internal.extend.message.response;

import com.google.protobuf.Message;
import com.tangosol.coherence.component.net.extend.message.response.GrpcResponse;

public abstract class BaseProxyResponse
        extends GrpcResponse
    {
    @Override
    public Message getProtoResponse()
        {
        return getMessage();
        }

    protected abstract Message getMessage();

    }
