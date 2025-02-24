/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.internal.extend.messageFactory;

import com.google.protobuf.Any;
import com.google.protobuf.Int32Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;

import com.oracle.coherence.common.base.Exceptions;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.MessageHelper;
import com.oracle.coherence.grpc.TopicHelper;

import com.tangosol.coherence.component.net.extend.message.GrpcMessageWrapper;
import com.tangosol.coherence.component.net.extend.message.Response;

import com.tangosol.coherence.component.net.extend.messageFactory.GrpcMessageFactory;

import com.oracle.coherence.grpc.internal.extend.message.response.ChannelUriResponse;
import com.oracle.coherence.grpc.internal.extend.message.response.CollectionOfStringValuesResponse;
import com.oracle.coherence.grpc.internal.extend.message.response.EmptyResponse;
import com.oracle.coherence.grpc.internal.extend.message.response.Int32ValueResponse;

import com.oracle.coherence.grpc.messages.topic.v1.EnsurePublisherResponse;
import com.oracle.coherence.grpc.messages.topic.v1.EnsureSubscriberResponse;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequest;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceResponse;

import com.tangosol.coherence.component.net.extend.message.response.GrpcResponse;
import com.tangosol.coherence.component.net.extend.messageFactory.TopicServiceFactory;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import com.tangosol.io.Serializer;

import com.tangosol.util.Filter;
import com.tangosol.util.ListMap;
import com.tangosol.util.ValueExtractor;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A {@link TopicServiceFactory} that provides messages
 * that wrap protobuf messages.
 *
 * @author Jonathan Knight  2025.01.25
 */
public class GrpcTopicServiceFactory
        extends TopicServiceFactory
        implements GrpcMessageFactory<TopicServiceRequest, TopicServiceResponse>
    {
    public GrpcTopicServiceFactory()
        {
        }

    static
        {
        __initStatic();
        }

    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new ListMap<>();
        __mapChildren.put("ChannelCountRequest", GrpcChannelCountRequest.class);
        __mapChildren.put("DestroyTopicRequest", GrpcDestroyTopicRequest.class);
        __mapChildren.put("EnsureChannelCountRequest", GrpcEnsureChannelCountRequest.class);
        __mapChildren.put("EnsureTopicRequest", GrpcEnsureTopicRequest.class);
        __mapChildren.put("GetSubscriberGroupRequest", GrpcGetSubscriberGroupsRequest.class);
        __mapChildren.put("EnsurePublisherRequest", GrpcEnsurePublisherRequest.class);
        __mapChildren.put("DestroyPublisherRequest", GrpcDestroyPublisherRequest.class);
        __mapChildren.put("EnsureSubscriberRequest", GrpcEnsureSubscriberRequest.class);
        __mapChildren.put("DestroySubscriberRequest", GrpcDestroySubscriberRequest.class);
        __mapChildren.put("Response", TopicsResponse.class);
        }

    @Override
    protected Map<String, Class<?>> get_ChildClasses()
        {
        return __mapChildren;
        }

    @Override
    @SuppressWarnings("unchecked")
    public <M extends GrpcMessageWrapper> M createRequestMessage(TopicServiceRequest request, Serializer serializer)
        {
        M message = (M) switch (request.getType())
            {
            case EnsureTopic -> createMessage(TYPE_ID_ENSURE_TOPIC);
            case DestroyTopic -> createMessage(TYPE_ID_DESTROY_TOPIC);
            case EnsurePublisher -> createMessage(TYPE_ID_ENSURE_PUBLISHER);
            case DestroyPublisher -> createMessage(TYPE_ID_DESTROY_PUBLISHER);
            case EnsureSubscriber -> createMessage(TYPE_ID_ENSURE_SUBSCRIBER);
            case DestroySubscriber -> createMessage(TYPE_ID_DESTROY_SUBSCRIBER);
            case EnsureChannelCount -> createMessage(TYPE_ID_ENSURE_CHANNEL_COUNT);
            case GetChannelCount -> createMessage(TYPE_ID_CHANNEL_COUNT);
            case GetSubscriberGroups -> createMessage(TYPE_ID_GET_SUBSCRIBER_GROUPS);
            default -> throw new IllegalArgumentException("Unsupported request type: " + request.getType());
            };

        if (request.hasMessage())
            {
            message.setProtoMessage(request.getMessage(), serializer);
            }
        return message;
        }

    @Override
    public TopicServiceResponse createResponse(GrpcResponse response)
        {
        // Must return the populated TopicServiceResponse
        TopicServiceResponse.Builder builder = TopicServiceResponse.newBuilder();
        builder.setProxyId(response.getProxyId());
        builder.setMessage(Any.pack(response.getProtoResponse()));
        return builder.build();
        }

    @Override
    public TopicServiceResponse toProtoMessage(com.tangosol.net.messaging.Message message, int nProxyId)
        {
        if (message instanceof GrpcResponse)
            {
            GrpcResponse response = (GrpcResponse) message;
            Message body = response.getProtoResponse();
            return TopicServiceResponse.newBuilder()
                    .setProxyId(response.getProxyId())
                    .setMessage(Any.pack(body))
                    .build();
            }
        throw new IllegalArgumentException("Unsupported message type: " + message);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Unpack the message field from a request.
     *
     * @param request the request to get the message from
     * @param type    the expected type of the message
     * @param <T>     the expected type of the message
     * @return the unpacked message
     */
    protected <T extends Message> T unpack(TopicServiceRequest request, Class<T> type)
        {
        try
            {
            Any any = request.getMessage();
            return any.unpack(type);
            }
        catch (InvalidProtocolBufferException e)
            {
            throw Exceptions.ensureRuntimeException(e, "Could not unpack message field of type " + type.getName());
            }
        }

    // ----- inner class: GrpcEnsureTopicRequest ----------------------------

    /**
     * The gRPC {@link EnsureTopicRequest} which wraps a protobuf request.
     */
    public static class GrpcEnsureTopicRequest
            extends EnsureTopicRequest
            implements GrpcMessageWrapper
        {
        public GrpcEnsureTopicRequest()
            {
            m_fAutoAccept = true;
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            com.oracle.coherence.grpc.messages.topic.v1.EnsureTopicRequest request
                    = MessageHelper.unpack(any, com.oracle.coherence.grpc.messages.topic.v1.EnsureTopicRequest.class);

            setTopicName(request.getTopic());
            setResponse(new ChannelUriResponse());
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcDestroyTopicRequest ---------------------------

    /**
     * The gRPC {@link DestroyTopicRequest} which wraps a protobuf request.
     */
    public static class GrpcDestroyTopicRequest
            extends DestroyTopicRequest
            implements GrpcMessageWrapper
        {
        public GrpcDestroyTopicRequest()
            {
            setResponse(new EmptyResponse());
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            StringValue request = MessageHelper.unpack(any, StringValue.class);
            setTopicName(request.getValue());
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcEnsurePublisherRequest ------------------------

    /**
     * The gRPC {@link EnsurePublisherRequest} request which wraps a protobuf request.
     */
    public static class GrpcEnsurePublisherRequest
            extends EnsurePublisherRequest
            implements GrpcMessageWrapper
        {
        public GrpcEnsurePublisherRequest()
            {
            m_fAutoAccept = true;
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            com.oracle.coherence.grpc.messages.topic.v1.EnsurePublisherRequest request
                    = MessageHelper.unpack(any, com.oracle.coherence.grpc.messages.topic.v1.EnsurePublisherRequest.class);

            setTopicName(request.getTopic());
            setChannelCount(request.getChannelCount());
            setResponse(new GrpcEnsurePublisherResponse());
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcEnsurePublisherResponse -----------------------

    /**
     * The response for a {@link GrpcEnsurePublisherRequest} request.
     */
    public static class GrpcEnsurePublisherResponse
            extends ChannelUriResponse
        {
        public GrpcEnsurePublisherResponse()
            {
            }

        @Override
        protected String getURIValue()
            {
            Object[] ao = (Object[]) getResult();
            return String.valueOf(ao[0]);
            }

        @Override
        public Message getMessage()
            {
            Object[] aoResponse = (Object[]) getResult();
            URI      uri        = URI.create((String) aoResponse[EnsurePublisherRequest.RESPONSE_ID_URI]);
            int      nProxyId   = Integer.parseInt(uri.getSchemeSpecificPart());

            return EnsurePublisherResponse.newBuilder()
                    .setProxyId(nProxyId)
                    .setPublisherId(((Number) aoResponse[EnsurePublisherRequest.RESPONSE_ID_PUBLISHER_ID]).longValue())
                    .setMaxBatchSize(((Number) aoResponse[EnsurePublisherRequest.RESPONSE_ID_BATCH_SIZE]).longValue())
                    .setChannelCount(((Number) aoResponse[EnsurePublisherRequest.RESPONSE_ID_CHANNEL_COUNT]).intValue())
                    .build();
            }
        }

    // ----- inner class: GrpcDestroyPublisherRequest -----------------------

    /**
     * The gRPC {@link DestroyPublisherRequest} which wraps a protobuf request.
     */
    public static class GrpcDestroyPublisherRequest
            extends DestroyPublisherRequest
            implements GrpcMessageWrapper
        {
        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            Int32Value request = MessageHelper.unpack(any, Int32Value.class);
            setPublisherId(request.getValue());
            setResponse(new EmptyResponse());
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcEnsureSubscriberRequest -----------------------

    /**
     * The gRPC {@link EnsureSubscriberRequest} request which wraps a protobuf request.
     */
    public static class GrpcEnsureSubscriberRequest
            extends EnsureSubscriberRequest
            implements GrpcMessageWrapper
        {
        public GrpcEnsureSubscriberRequest()
            {
            m_fAutoAccept = true;
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {                                                                                       
            com.oracle.coherence.grpc.messages.topic.v1.EnsureSubscriberRequest request
                    = MessageHelper.unpack(any, com.oracle.coherence.grpc.messages.topic.v1.EnsureSubscriberRequest.class);

            setTopicName(request.getTopic());
            setSubscriberGroup(request.getSubscriberGroup());
            setCompleteOnEmpty(request.getCompleteOnEmpty());

            List<Integer> listChannel = request.getChannelsList();
            if (!listChannel.isEmpty())
                {
                setChannels(listChannel.stream().mapToInt(Integer::intValue).toArray());
                }

            if (request.hasFilter())
                {
                Filter<?> filter = BinaryHelper.fromByteString(request.getFilter(), serializer);
                setFilter(filter);
                }
            if (request.hasExtractor())
                {
                ValueExtractor<?, ?> extractor = BinaryHelper.fromByteString(request.getExtractor(), serializer);
                setExtractor(extractor);
                }
            setResponse(new GrpcEnsureSubscriberResponse());
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcEnsureSubscriberResponse ----------------------

    /**
     * The response for a {@link GrpcEnsureSubscriberRequest} request.
     */
    public static class GrpcEnsureSubscriberResponse
            extends ChannelUriResponse
        {
        public GrpcEnsureSubscriberResponse()
            {
            }

        @Override
        protected String getURIValue()
            {
            Object[] ao = (Object[]) getResult();
            return String.valueOf(ao[0]);
            }

        @Override
        public Message getMessage()
            {
            Object[] aoResponse = (Object[]) getResult();
            URI      uri        = URI.create((String) aoResponse[EnsurePublisherRequest.RESPONSE_ID_URI]);
            int      nProxyId   = Integer.parseInt(uri.getSchemeSpecificPart());

            return EnsureSubscriberResponse.newBuilder()
                    .setProxyId(nProxyId)
                    .setSubscriberId(TopicHelper.toProtobufSubscriberId((SubscriberId) aoResponse[1]))
                    .setGroupId(TopicHelper.toProtobufSubscriberGroupId((SubscriberGroupId) aoResponse[2]))
                    .build();
            }
        }

    // ----- inner class: GrpcDestroySubscriberRequest -----------------------

    /**
     * The gRPC {@link GrpcDestroySubscriberRequest} which wraps a protobuf request.
     */
    public static class GrpcDestroySubscriberRequest
            extends DestroySubscriberRequest
            implements GrpcMessageWrapper
        {
        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            Int32Value request = MessageHelper.unpack(any, Int32Value.class);
            setSubscriberId(request.getValue());
            setResponse(new EmptyResponse());
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcChannelCountRequest ---------------------------

    /**
     * The gRPC {@link ChannelCountRequest} which wraps a protobuf request.
     */
    public static class GrpcChannelCountRequest
            extends ChannelCountRequest
            implements GrpcMessageWrapper
        {
        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            StringValue request = MessageHelper.unpack(any, StringValue.class);
            setTopicName(request.getValue());
            setResponse(new Int32ValueResponse());
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcChannelCountRequest ---------------------------

    /**
     * The gRPC {@link EnsureChannelCountRequest} which wraps a protobuf request.
     */
    public static class GrpcEnsureChannelCountRequest
            extends EnsureChannelCountRequest
            implements GrpcMessageWrapper
        {
        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            com.oracle.coherence.grpc.messages.topic.v1.EnsureChannelCountRequest request
                    = MessageHelper.unpack(any, com.oracle.coherence.grpc.messages.topic.v1.EnsureChannelCountRequest.class);

            setTopicName(request.getTopic());
            setChannelCount(request.getChannelCount());
            setRequiredChannels(request.getRequiredCount());
            setResponse(new Int32ValueResponse());
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcChannelCountRequest ---------------------------

    /**
     * The gRPC {@link GetSubscriberGroupsRequest} which wraps a protobuf request.
     */
    public static class GrpcGetSubscriberGroupsRequest
            extends GetSubscriberGroupsRequest
            implements GrpcMessageWrapper
        {
        @Override
        @SuppressWarnings("unchecked")
        protected void onRun(Response response)
            {
            super.onRun(response);
            if (!response.isFailure())
                {
                Collection<SubscriberGroupId> list = (Collection<SubscriberGroupId>) response.getResult();
                response.setResult(list.stream()
                                .filter(SubscriberGroupId::isDurable)
                        .map(SubscriberGroupId::getGroupName).toList());
                }
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            StringValue request = MessageHelper.unpack(any, StringValue.class);
            setTopicName(request.getValue());
            setResponse(new CollectionOfStringValuesResponse());
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- data members ---------------------------------------------------

    private static ListMap<String, Class<?>> __mapChildren;
    }
