/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common.topic;

import com.google.protobuf.Any;

import com.oracle.coherence.grpc.GrpcService;
import com.oracle.coherence.grpc.NamedTopicProtocol;

import com.oracle.coherence.grpc.proxy.common.BaseExtendProxyProtocol;

import com.oracle.coherence.grpc.internal.extend.protocol.GrpcNamedTopicExtendProtocol;
import com.oracle.coherence.grpc.internal.extend.protocol.GrpcTopicServiceExtendProtocol;

import com.tangosol.coherence.component.net.extend.proxy.GrpcExtendProxy;

import com.oracle.coherence.grpc.internal.extend.proxy.serviceProxy.GrpcTopicServiceProxy;

import com.oracle.coherence.grpc.messages.proxy.v1.InitRequest;
import com.oracle.coherence.grpc.messages.topic.v1.ResponseType;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequest;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceResponse;

import com.tangosol.io.Serializer;

import com.tangosol.net.ExtensibleConfigurableCacheFactory;

import com.tangosol.net.messaging.Protocol;

import com.tangosol.util.UUID;

/**
 * The server side {@link NamedTopicProtocol}.
 *
 * @author Jonathan Knight  2025.01.25
 */
public class NamedTopicProxyProtocol
        extends BaseExtendProxyProtocol<TopicServiceRequest, TopicServiceResponse>
        implements NamedTopicProtocol<TopicServiceRequest, TopicServiceResponse>
    {
    @Override
    public Class<TopicServiceRequest> getRequestType()
        {
        return TopicServiceRequest.class;
        }

    @Override
    public Class<TopicServiceResponse> getResponseType()
        {
        return TopicServiceResponse.class;
        }

    @Override
    public Protocol[] getExtendProtocols()
        {
        return new Protocol[]
            {
            GrpcTopicServiceExtendProtocol.getInstance(),
            GrpcNamedTopicExtendProtocol.getInstance()
            };
        }

    @Override
    protected GrpcExtendProxy<TopicServiceResponse> initInternal(GrpcService service, InitRequest request, int nVersion,
            UUID clientUUID)
        {
        String                             sScope     = request.getScope();
        String                             sFormat    = request.getFormat();
        ExtensibleConfigurableCacheFactory eccf       = (ExtensibleConfigurableCacheFactory) service.getCCF(sScope);
        Serializer                         serializer = service.getSerializer(sFormat, eccf.getConfigClassLoader());
        GrpcTopicServiceProxy              proxy      = new GrpcTopicServiceProxy();
        proxy.setCacheFactory(eccf);
        proxy.setSerializer(serializer);
        return proxy;
        }

    @Override
    protected TopicServiceResponse response(int id, Any any)
        {
        return TopicServiceResponse.newBuilder()
                .setProxyId(id)
                .setType(ResponseType.Message)
                .setMessage(any)
                .build();
        }

    @Override
    protected Any getMessage(TopicServiceRequest request)
        {
        return request.getMessage();
        }

    @Override
    protected int getProxyId(TopicServiceRequest request)
        {
        return request.getProxyId();
        }
    }
