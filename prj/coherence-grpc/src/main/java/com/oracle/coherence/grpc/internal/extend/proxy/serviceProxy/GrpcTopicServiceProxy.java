/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.internal.extend.proxy.serviceProxy;

import com.oracle.coherence.grpc.internal.extend.protocol.GrpcTopicServiceExtendProtocol;

import com.tangosol.coherence.component.net.extend.proxy.GrpcExtendProxy;

import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceResponse;

import com.tangosol.coherence.component.net.extend.protocol.TopicServiceProtocol;

import com.tangosol.coherence.component.net.extend.proxy.serviceProxy.TopicServiceProxy;

/**
 * A gRPC {@link TopicServiceProxy}.
 *
 * @author Jonathan Knight  2025.01.25
 */
public class GrpcTopicServiceProxy
        extends TopicServiceProxy
        implements GrpcExtendProxy<TopicServiceResponse>
    {
    @Override
    public TopicServiceProtocol getProtocol()
        {
        return GrpcTopicServiceExtendProtocol.getInstance();
        }
    }
