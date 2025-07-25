/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.internal.extend.messageFactory;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;

import com.google.protobuf.util.Timestamps;

import com.oracle.coherence.common.base.Exceptions;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.ErrorsHelper;
import com.oracle.coherence.grpc.MessageHelper;
import com.oracle.coherence.grpc.TopicHelper;

import com.oracle.coherence.grpc.messages.topic.v1.SimpleReceiveRequest;
import com.tangosol.coherence.component.net.extend.message.GrpcMessageWrapper;
import com.tangosol.coherence.component.net.extend.message.Response;
import com.tangosol.coherence.component.net.extend.messageFactory.GrpcMessageFactory;

import com.oracle.coherence.grpc.internal.extend.message.response.BoolValueResponse;
import com.oracle.coherence.grpc.internal.extend.message.response.CollectionOfInt32Response;
import com.oracle.coherence.grpc.internal.extend.message.response.EmptyResponse;
import com.oracle.coherence.grpc.internal.extend.message.response.Int32ValueResponse;
import com.oracle.coherence.grpc.internal.extend.message.response.MapOfChannelAndPositionResponse;

import com.oracle.coherence.grpc.messages.common.v1.CollectionOfInt32;

import com.oracle.coherence.grpc.messages.topic.v1.ChannelAndPosition;
import com.oracle.coherence.grpc.messages.topic.v1.CommitResponse;
import com.oracle.coherence.grpc.messages.topic.v1.CommitResponseStatus;
import com.oracle.coherence.grpc.messages.topic.v1.InitializeSubscriptionResponse;
import com.oracle.coherence.grpc.messages.topic.v1.NamedTopicEvent;
import com.oracle.coherence.grpc.messages.topic.v1.ReceiveResponse;
import com.oracle.coherence.grpc.messages.topic.v1.ReceiveStatus;
import com.oracle.coherence.grpc.messages.topic.v1.ResponseType;
import com.oracle.coherence.grpc.messages.topic.v1.SeekedPositions;
import com.oracle.coherence.grpc.messages.topic.v1.TopicEventType;
import com.oracle.coherence.grpc.messages.topic.v1.TopicPosition;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequest;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceResponse;

import com.tangosol.coherence.component.net.extend.message.response.GrpcResponse;
import com.tangosol.coherence.component.net.extend.messageFactory.NamedTopicFactory;

import com.tangosol.internal.net.topic.PublishResult;
import com.tangosol.internal.net.topic.SeekResult;

import com.tangosol.internal.net.topic.SimpleReceiveResult;

import com.tangosol.io.Serializer;

import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Binary;
import com.tangosol.util.Filter;
import com.tangosol.util.ListMap;
import com.tangosol.util.ValueExtractor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A {@link NamedTopicFactory} that provides messages
 * that wrap protobuf messages.
 *
 * @author Jonathan Knight  2025.01.25
 */
public class GrpcNamedTopicFactory
        extends NamedTopicFactory
        implements GrpcMessageFactory<TopicServiceRequest, TopicServiceResponse>
    {
    public GrpcNamedTopicFactory()
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
        __mapChildren.put("GetRemainingMessagesRequest", GrpcGetRemainingMessagesRequest.class);
        __mapChildren.put("EnsureSubscriberGroupRequest", GrpcEnsureSubscriberGroupRequest.class);
        __mapChildren.put("DestroySubscriberGroupRequest", GrpcDestroySubscriberGroupRequest.class);
        __mapChildren.put("PublishRequest", GrpcPublishRequest.class);
        __mapChildren.put("InitializeSubscriptionRequest", GrpcInitializeSubscriptionRequest.class);
        __mapChildren.put("EnsureSubscriptionRequest", GrpcEnsureSubscriptionRequest.class);
        __mapChildren.put("GetOwnedChannelsRequest", GrpcGetOwnedChannelsRequest.class);
        __mapChildren.put("ReceiveRequest", GrpcReceiveRequest.class);
        __mapChildren.put("SimpleReceiveRequest", GrpcSimpleReceiveRequest.class);
        __mapChildren.put("PeekRequest", GrpcPeekRequest.class);
        __mapChildren.put("CommitRequest", GrpcCommitRequest.class);
        __mapChildren.put("IsCommitedRequest", GrpcIsCommitedRequest.class);
        __mapChildren.put("GetLastCommitedRequest", GrpcGetLastCommitedRequest.class);
        __mapChildren.put("GetHeadsRequest", GrpcGetHeadsRequest.class);
        __mapChildren.put("GetTailsRequest", GrpcGetTailsRequest.class);
        __mapChildren.put("SeekRequest", GrpcSeekRequest.class);
        __mapChildren.put("HeartbeatRequest", GrpcHeartbeatRequest.class);

        __mapChildren.put("Response", TopicsResponse.class);
        __mapChildren.put("PublisherEvent", PublisherEvent.class);
        __mapChildren.put("GetSubscription", GetSubscriptionRequest.class);
        __mapChildren.put("SubscriberEvent", SubscriberChannelEvent.class);
        __mapChildren.put("DestroyEvent", DestroyEvent.class);
        }

    @Override
    protected Map<String, Class<?>> get_ChildClasses()
        {
        return __mapChildren;
        }

    @Override
    @SuppressWarnings({"unchecked"})
    public <M extends GrpcMessageWrapper> M createRequestMessage(TopicServiceRequest request, Serializer serializer)
        {
        M message = (M) switch (request.getType())
            {
            case EnsureSubscriberGroup -> createMessage(TYPE_ID_ENSURE_SUBSCRIBER_GROUP);
            case DestroySubscriberGroup -> createMessage(TYPE_ID_DESTROY_SUBSCRIBER_GROUP);
            case GetRemainingMessages -> createMessage(TYPE_ID_REMAINING_MESSAGES);
            case GetTails -> createMessage(TYPE_ID_GET_TAILS);
            case Publish -> createMessage(TYPE_ID_PUBLISH);
            case InitializeSubscription -> createMessage(TYPE_ID_INITIALIZE_SUBSCRIPTION);
            case EnsureSubscription -> createMessage(TYPE_ID_ENSURE_SUBSCRIPTION);
            case GetSubscriberHeads -> createMessage(TYPE_ID_GET_HEADS);
            case GetLastCommited -> createMessage(TYPE_ID_GET_LAST_COMMITTED);
            case GetOwnedChannels -> createMessage(TYPE_ID_GET_OWNED_CHANNELS);
            case SubscriberHeartbeat -> createMessage(TYPE_ID_HEARTBEAT);
            case IsPositionCommitted -> createMessage(TYPE_ID_IS_COMMITTED);
            case PeekAtPosition -> createMessage(TYPE_ID_PEEK);
            case Receive -> createMessage(TYPE_ID_RECEIVE);
            case SimpleReceive -> createMessage(TYPE_ID_SIMPLE_RECEIVE);
            case SeekSubscriber -> createMessage(TYPE_ID_SEEK);
            case CommitPosition -> createMessage(TYPE_ID_COMMIT);
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
        Message protoResponse = response.getProtoResponse();

        // Must return the populated TopicServiceResponse
        TopicServiceResponse.Builder builder = TopicServiceResponse.newBuilder()
                .setProxyId(response.getProxyId())
                .setMessage(Any.pack(protoResponse));

        return builder.build();
        }

    @Override
    public TopicServiceResponse toProtoMessage(com.tangosol.net.messaging.Message message, int nProxyId)
        {
        int nTypeId = message.getTypeId();

        ResponseType responseType;
        if (nTypeId == TYPE_ID_PUBLISHER_EVENT
                || nTypeId == TYPE_ID_SUBSCRIBER_EVENT
                || nTypeId == TYPE_ID_DESTROY_EVENT)
            {
            responseType = ResponseType.Event;

            TopicServiceResponse.Builder builder = TopicServiceResponse.newBuilder()
                    .setProxyId(nProxyId)
                    .setType(responseType);

            Message payload = switch (nTypeId)
                {
                case TYPE_ID_PUBLISHER_EVENT -> TopicHelper.toProtobufPublisherEvent((PublisherEvent) message);
                case TYPE_ID_DESTROY_EVENT -> NamedTopicEvent.newBuilder().setType(TopicEventType.TopicDestroyed).build();
                case TYPE_ID_SUBSCRIBER_EVENT -> TopicHelper.toProtobufSubscriberEvent((SubscriberChannelEvent) message);
                default -> throw new IllegalArgumentException("Unsupported message type: " + message);
                };

            builder.setMessage(Any.pack(payload));
            return builder.build();
            }

        return null;
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

    // ----- inner class: GrpcGetRemainingMessagesRequest -------------------

    /**
     * The gRPC {@link GrpcGetRemainingMessagesRequest} which wraps a protobuf request.
     */
    public static class GrpcGetRemainingMessagesRequest
            extends GetRemainingMessagesRequest
            implements GrpcMessageWrapper
        {
        public GrpcGetRemainingMessagesRequest()
            {
            setResponse(new Int32ValueResponse());
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            com.oracle.coherence.grpc.messages.topic.v1.GetRemainingMessagesRequest request = MessageHelper.unpack(any, com.oracle.coherence.grpc.messages.topic.v1.GetRemainingMessagesRequest.class);
            setSubscriberGroup(request.getSubscriberGroup());
            List<Integer> list = request.getChannelsList();
            setChannels(list.stream().mapToInt(i -> i).toArray());
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcEnsureSubscriberGroupRequest ------------------

    /**
     * The gRPC {@link GrpcEnsureSubscriberGroupRequest} which wraps a protobuf request.
     */
    public static class GrpcEnsureSubscriberGroupRequest
            extends EnsureSubscriberGroupRequest
            implements GrpcMessageWrapper
        {
        public GrpcEnsureSubscriberGroupRequest()
            {
            setResponse(new EmptyResponse());
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            com.oracle.coherence.grpc.messages.topic.v1.EnsureSubscriberGroupRequest request = MessageHelper.unpack(any, com.oracle.coherence.grpc.messages.topic.v1.EnsureSubscriberGroupRequest.class);
            setSubscriberGroup(request.getSubscriberGroup());
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
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcDestroySubscriberGroupRequest -----------------

    /**
     * The gRPC {@link GrpcDestroySubscriberGroupRequest} which wraps a protobuf request.
     */
    public static class GrpcDestroySubscriberGroupRequest
            extends DestroySubscriberGroupRequest
            implements GrpcMessageWrapper
        {
        public GrpcDestroySubscriberGroupRequest()
            {
            setResponse(new EmptyResponse());
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            StringValue sGroup = MessageHelper.unpack(any, StringValue.class);
            setSubscriberGroup(sGroup.getValue());
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcPublishRequest --------------------------------

    /**
     * The gRPC {@link GrpcPublishRequest} which wraps a protobuf request.
     */
    public static class GrpcPublishRequest
            extends PublishRequest
            implements GrpcMessageWrapper
        {
        public GrpcPublishRequest()
            {
            setResponse(new GrpcPublishResponse());
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            com.oracle.coherence.grpc.messages.topic.v1.PublishRequest request = MessageHelper.unpack(any, com.oracle.coherence.grpc.messages.topic.v1.PublishRequest.class);
            setChannel(request.getChannel());
            setBinaries(BinaryHelper.toListOfBinary(request.getValuesList()));

            if (request.hasNotificationIdentifier())
                {
                setNotify(request.getNotificationIdentifier());
                }
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcPublishResponseRequest ------------------------

    /**
     * The response for a {@link GrpcPublishRequest} request.
     */
    public static class GrpcPublishResponse
            extends GrpcResponse
        {
        public GrpcPublishResponse()
            {
            }

        @Override
        public Message getProtoResponse()
            {
            PublishResult result = (PublishResult) getResult();
            return TopicHelper.toProtobufPublishResult(result, m_serializer);
            }


        }

    // ----- inner class: GrpcInitializeSubscriptionRequest -----------------

    /**
     * The gRPC {@link GrpcEnsureSubscriptionRequest} which wraps a protobuf request.
     */
    public static class GrpcInitializeSubscriptionRequest
            extends InitializeSubscriptionRequest
            implements GrpcMessageWrapper
        {
        public GrpcInitializeSubscriptionRequest()
            {
            setResponse(new GrpcInitializeSubscriptionResponse());
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            com.oracle.coherence.grpc.messages.topic.v1.InitializeSubscriptionRequest request = MessageHelper.unpack(any, com.oracle.coherence.grpc.messages.topic.v1.InitializeSubscriptionRequest.class);
            setDisconnected(request.getDisconnected());
            setReconnect(request.getReconnect());
            setForceReconnect(request.getForceReconnect());
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcInitializeSubscriptionResponseRequest ---------

    /**
     * The response for a {@link GrpcInitializeSubscriptionRequest} request.
     */
    public static class GrpcInitializeSubscriptionResponse
            extends GrpcResponse
        {
        public GrpcInitializeSubscriptionResponse()
            {
            }

        @Override
        public Message getProtoResponse()
            {
            Object[]  aoResult   = (Object[]) getResult();
            Timestamp timestamp  = Timestamps.fromMillis((Long) aoResult[1]);
            Object[]  aoPosition = (Object[]) aoResult[2];

            List<TopicPosition> heads = Arrays.stream(aoPosition)
                    .map(o -> TopicHelper.toProtobufPosition((Position) o))
                    .toList();

            return InitializeSubscriptionResponse.newBuilder()
                    .setSubscriptionId((Long) aoResult[0])
                    .setTimestamp(timestamp)
                    .addAllHeads(heads)
                    .build();
            }
        }

    // ----- inner class: GrpcEnsureSubscriptionRequest ---------------------

    /**
     * The gRPC {@link GrpcEnsureSubscriptionRequest} which wraps a protobuf request.
     */
    public static class GrpcEnsureSubscriptionRequest
            extends EnsureSubscriptionRequest
            implements GrpcMessageWrapper
        {
        public GrpcEnsureSubscriptionRequest()
            {
            setResponse(new BoolValueResponse());
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            com.oracle.coherence.grpc.messages.topic.v1.EnsureSubscriptionRequest request = MessageHelper.unpack(any, com.oracle.coherence.grpc.messages.topic.v1.EnsureSubscriptionRequest.class);
            setSubscriptionId(request.getSubscriptionId());
            setForceReconnect(request.getForceReconnect());
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcGetOwnedChannelsRequest -----------------------

    /**
     * The gRPC {@link GrpcGetOwnedChannelsRequest} which wraps a protobuf request.
     */
    public static class GrpcGetOwnedChannelsRequest
            extends GetOwnedChannelsRequest
            implements GrpcMessageWrapper
        {
        public GrpcGetOwnedChannelsRequest()
            {
            setResponse(new CollectionOfInt32Response());
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcReceiveRequest --------------------------------

    /**
     * The gRPC {@link GrpcReceiveRequest} which wraps a protobuf request.
     */
    public static class GrpcReceiveRequest
            extends ReceiveRequest
            implements GrpcMessageWrapper
        {
        public GrpcReceiveRequest()
            {
            setResponse(new GrpcReceiveResponse());
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            com.oracle.coherence.grpc.messages.topic.v1.ReceiveRequest request = MessageHelper.unpack(any, com.oracle.coherence.grpc.messages.topic.v1.ReceiveRequest.class);
            setTopicChannel(request.getChannel());
            setMaxElements(request.getMaxMessages());
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcReceiveRequest --------------------------------

    /**
     * The gRPC {@link GrpcSimpleReceiveRequest} which wraps a protobuf request.
     */
    public static class GrpcSimpleReceiveRequest
            extends SimpleReceiveRequest
            implements GrpcMessageWrapper
        {
        public GrpcSimpleReceiveRequest()
            {
            setResponse(new GrpcReceiveResponse());
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            com.oracle.coherence.grpc.messages.topic.v1.SimpleReceiveRequest request = MessageHelper.unpack(any, com.oracle.coherence.grpc.messages.topic.v1.SimpleReceiveRequest.class);
            int                  cMaxMessages = request.getMaxMessages();
            setMaxElements(Math.max(1, cMaxMessages));
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcReceiveResponseRequest ------------------------

    /**
     * The response for a {@link GrpcReceiveRequest} request.
     */
    public static class GrpcReceiveResponse
            extends GrpcResponse
        {
        public GrpcReceiveResponse()
            {
            }

        @Override
        public Message getProtoResponse()
            {
            Object oResult = getResult();
            if (oResult instanceof Throwable)
                {
                return ErrorsHelper.createErrorMessage((Throwable) oResult, m_serializer);
                }

            SimpleReceiveResult result = (SimpleReceiveResult) oResult;

            ReceiveStatus status = switch (result.getStatus())
                {
                case Success -> ReceiveStatus.ReceiveSuccess;
                case Exhausted -> ReceiveStatus.ChannelExhausted;
                case NotAllocatedChannel -> ReceiveStatus.ChannelNotAllocatedChannel;
                case UnknownSubscriber -> ReceiveStatus.UnknownSubscriber;
                };

            ReceiveResponse.Builder builder = ReceiveResponse.newBuilder()
                    .setHeadPosition(TopicHelper.toProtobufPosition(result.getHead()))
                    .setRemainingValues(result.getRemainingElementCount())
                    .setStatus(status)
                    .setChannel(result.getChannel());

            for (Binary binary : result.getElements())
                {
                builder.addValues(BinaryHelper.toByteString(binary));
                }

            return builder.build();
            }
        }

    // ----- inner class: GrpcPeekRequest -----------------------------------

    /**
     * The gRPC {@link GrpcPeekRequest} which wraps a protobuf request.
     */
    public static class GrpcPeekRequest
            extends PeekRequest
            implements GrpcMessageWrapper
        {
        public GrpcPeekRequest()
            {
            setResponse(new GrpcPeekResponse());
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            ChannelAndPosition cp = MessageHelper.unpack(any, ChannelAndPosition.class);
            setChannel(cp.getChannel());
            setPosition(TopicHelper.fromProtobufPosition(cp.getPosition()));
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcPeekResponse ----------------------------------

    /**
     * The response for a {@link GrpcPeekRequest} request.
     */
    public static class GrpcPeekResponse
            extends GrpcResponse
        {
        public GrpcPeekResponse()
            {
            }

        @Override
        public Message getProtoResponse()
            {
            Subscriber.Element<?> element = (Subscriber.Element<?>) getResult();
            return TopicHelper.toProtobufTopicElement(element);
            }
        }

    // ----- inner class: GrpcCommitRequest ---------------------------------

    /**
     * The gRPC {@link GrpcCommitRequest} which wraps a protobuf request.
     */
    public static class GrpcCommitRequest
            extends CommitRequest
            implements GrpcMessageWrapper
        {
        public GrpcCommitRequest()
            {
            setResponse(new GrpcCommitResponse());
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            ChannelAndPosition cp = MessageHelper.unpack(any, ChannelAndPosition.class);
            setChannel(cp.getChannel());
            setPosition(TopicHelper.fromProtobufPosition(cp.getPosition()));
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcCommitResponse --------------------------------

    /**
     * The response for a {@link GrpcCommitRequest} request.
     */
    public static class GrpcCommitResponse
            extends GrpcResponse
        {
        public GrpcCommitResponse()
            {
            }

        @Override
        public Message getProtoResponse()
            {
            Object[]                aoResult = (Object[]) getResult();
            TopicPosition           head     = TopicHelper.toProtobufPosition((Position) aoResult[CommitRequest.RESPONSE_ID_HEAD]);
            Subscriber.CommitResult result   = (Subscriber.CommitResult) aoResult[CommitRequest.RESPONSE_ID_RESULT];

            CommitResponseStatus status = switch (result.getStatus())
                {
                case Committed -> CommitResponseStatus.Committed;
                case AlreadyCommitted -> CommitResponseStatus.AlreadyCommitted;
                case Rejected -> CommitResponseStatus.Rejected;
                case Unowned -> CommitResponseStatus.Unowned;
                case NothingToCommit -> CommitResponseStatus.NothingToCommit;
                };

            CommitResponse.Builder builder = CommitResponse.newBuilder()
                .setHead(head)
                .setStatus(status);

            result.getChannel().ifPresent(builder::setChannel);
            result.getError().ifPresent(err -> builder.setError(ErrorsHelper.createErrorMessage(err, m_serializer)));
            result.getPosition().ifPresent(pos -> builder.setPosition(TopicHelper.toProtobufPosition(pos)));
            return builder.build();
            }
        }

    // ----- inner class: GrpcIsCommitedRequest -----------------------------

    /**
     * The gRPC {@link GrpcIsCommitedRequest} which wraps a protobuf request.
     */
    public static class GrpcIsCommitedRequest
            extends IsCommitedRequest
            implements GrpcMessageWrapper
        {
        public GrpcIsCommitedRequest()
            {
            setResponse(new BoolValueResponse());
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            ChannelAndPosition cp = MessageHelper.unpack(any, ChannelAndPosition.class);
            setChannel(cp.getChannel());
            setPosition(TopicHelper.fromProtobufPosition(cp.getPosition()));
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcGetLastCommitedRequest ------------------------

    /**
     * The gRPC {@link GrpcGetLastCommitedRequest} which wraps a protobuf request.
     */
    public static class GrpcGetLastCommitedRequest
            extends GetLastCommitedRequest
            implements GrpcMessageWrapper
        {
        public GrpcGetLastCommitedRequest()
            {
            setResponse(new MapOfChannelAndPositionResponse());
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcGetHeadsRequest -------------------------------

    /**
     * The gRPC {@link GrpcGetTailsRequest} which wraps a protobuf request.
     */
    public static class GrpcGetHeadsRequest
            extends GetHeadsRequest
            implements GrpcMessageWrapper
        {
        public GrpcGetHeadsRequest()
            {
            setResponse(new MapOfChannelAndPositionResponse());
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            CollectionOfInt32 col = MessageHelper.unpack(any, CollectionOfInt32.class);
            setChannels(MessageHelper.toIntArray(col));
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcGetTailsRequest -------------------------------

    /**
     * The gRPC {@link GrpcGetTailsRequest} which wraps a protobuf request.
     */
    public static class GrpcGetTailsRequest
            extends GetTailsRequest
            implements GrpcMessageWrapper
        {
        public GrpcGetTailsRequest()
            {
            setResponse(new MapOfChannelAndPositionResponse());
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcSeekRequest -----------------------------------

    /**
     * The gRPC {@link SeekRequest} which wraps a protobuf request.
     */
    public static class GrpcSeekRequest
            extends SeekRequest
            implements GrpcMessageWrapper
        {
        public GrpcSeekRequest()
            {
            setResponse(new SeekResponse());
            }

        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            com.oracle.coherence.grpc.messages.topic.v1.SeekRequest request = MessageHelper.unpack(any, com.oracle.coherence.grpc.messages.topic.v1.SeekRequest.class);
            if (request.hasByPosition())
                {
                setPositions(TopicHelper.fromProtobufChannelAndPosition(request.getByPosition()));
                }
            else
                {
                setTimestamps(TopicHelper.fromProtobufChannelAndTimestamp(request.getByTimestamp()));
                }
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: SeekResponse -------------------------------------

    /**
     * The response from a {@link GrpcSeekRequest} request.
     */
    public static class SeekResponse
        extends GrpcResponse
        {
        @Override
        @SuppressWarnings("unchecked")
        public Message getProtoResponse()
            {
            com.oracle.coherence.grpc.messages.topic.v1.SeekResponse.Builder builder
                    = com.oracle.coherence.grpc.messages.topic.v1.SeekResponse.newBuilder();

            Map<Integer, SeekResult> map = (Map<Integer, SeekResult>) getResult();
            for (Map.Entry<Integer, SeekResult> entry : map.entrySet())
                {
                SeekResult      result    = entry.getValue();
                SeekedPositions.Builder positionBuilder = SeekedPositions.newBuilder();
                TopicPosition           head            = TopicHelper.toProtobufPosition(result.getHead());
                TopicPosition           seeked          = TopicHelper.toProtobufPosition(result.getSeekPosition());

                if (head != null)
                    {
                    positionBuilder.setHead(head);
                    }

                if (seeked != null)
                    {
                    positionBuilder.setSeekedTo(seeked);
                    }

                builder.putPositions(entry.getKey(), positionBuilder.build());
                }

            return builder.build();
            }
        }

    // ----- inner class: GrpcHeartbeatRequest ------------------------------

    /**
     * The gRPC {@link HeartbeatRequest} which wraps a protobuf request.
     */
    public static class GrpcHeartbeatRequest
            extends HeartbeatRequest
            implements GrpcMessageWrapper
        {
        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            boolean fAsync = false;
            if (any != null)
                {
                BoolValue b = MessageHelper.unpack(any, BoolValue.class);
                fAsync = b != null && b.getValue();
                }

            setAsync(fAsync);
            setResponse(new EmptyResponse());
            }

        @Override
        public GrpcResponse getResponse()
            {
            return (GrpcResponse) super.getResponse();
            }
        }

    // ----- inner class: GrpcHeartbeatRequest ------------------------------

    /**
     * The gRPC {@link PublisherEvent} which wraps a protobuf request.
     */
    public static class GrpcPublisherEvent
            extends PublisherEvent
            implements GrpcMessageWrapper
        {
        @Override
        public void setProtoMessage(Any any, Serializer serializer)
            {
            }

        @Override
        public GrpcResponse getResponse()
            {
            throw new UnsupportedOperationException();
            }
        }

    // ----- data members ---------------------------------------------------

    private static ListMap<String, Class<?>> __mapChildren;
    }
