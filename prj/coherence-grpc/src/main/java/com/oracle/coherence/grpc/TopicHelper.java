/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;

import com.oracle.coherence.common.base.Exceptions;

import com.oracle.coherence.grpc.messages.common.v1.ErrorMessage;

import com.oracle.coherence.grpc.messages.topic.v1.MapOfChannelAndPosition;
import com.oracle.coherence.grpc.messages.topic.v1.MapOfChannelAndTimestamp;
import com.oracle.coherence.grpc.messages.topic.v1.PublisherEvent;
import com.oracle.coherence.grpc.messages.topic.v1.PublisherEventType;
import com.oracle.coherence.grpc.messages.topic.v1.SubscriberEventType;
import com.oracle.coherence.grpc.messages.topic.v1.TopicPosition;
import com.oracle.coherence.grpc.messages.topic.v1.PublishStatus;
import com.oracle.coherence.grpc.messages.topic.v1.PublishedValueStatus;
import com.oracle.coherence.grpc.messages.topic.v1.EnsureSubscriberResponse;
import com.oracle.coherence.grpc.messages.topic.v1.SubscriberEvent;
import com.oracle.coherence.grpc.messages.topic.v1.TopicElement;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceResponse;

import com.tangosol.coherence.component.net.extend.messageFactory.NamedTopicFactory;

import com.tangosol.internal.net.topic.NamedTopicPublisher;
import com.tangosol.internal.net.topic.PublishResult;
import com.tangosol.internal.net.topic.PublisherConnector;
import com.tangosol.internal.net.topic.SimplePublishResult;
import com.tangosol.internal.net.topic.SimplePublisherStatus;
import com.tangosol.internal.net.topic.SubscriberConnector;

import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import com.tangosol.io.Serializer;

import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.LongArray;
import com.tangosol.util.SimpleLongArray;
import com.tangosol.util.UUID;

import java.time.Instant;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import java.util.stream.IntStream;

/**
 * Common helper methods for gRPC topics.
 *
 * @author Jonathan Knight  2025.01.25
 */
public abstract class TopicHelper
    {
    /**
     * Private constructor for helper class.
     */
    private TopicHelper()
        {
        }

    /**
     * Create a gRPC {@link TopicPosition} from a plain {@link Position}.
     *
     * @param position  the {@link Position} to convert
     *
     * @return  the gRPC {@link TopicPosition}
     */
    public static TopicPosition toProtobufPosition(Position position)
        {
        if (position == null)
            {
            return null;
            }
        if (position instanceof PagedPosition)
            {
            com.oracle.coherence.grpc.messages.topic.v1.PagedPosition.Builder builder
                    = com.oracle.coherence.grpc.messages.topic.v1.PagedPosition.newBuilder();

            builder.setPage(((PagedPosition) position).getPage());
            builder.setOffset(((PagedPosition) position).getOffset());

            return TopicPosition.newBuilder().setPosition(Any.pack(builder.build())).build();
            }
        throw new IllegalArgumentException("unsupported position type: " + position.getClass());
        }

    /**
     * Create a plain {@link Position} from a gRPC {@link TopicPosition}.
     *
     * @param position  the position to convert
     *
     * @return  the plain {@link Position}
     */
    public static Position fromProtobufPosition(TopicPosition position)
        {
        if (position == null || !position.hasPosition())
            {
            return null;
            }

        Any any = position.getPosition();
        try
            {
            if (any.is(com.oracle.coherence.grpc.messages.topic.v1.PagedPosition.class))
                {
                com.oracle.coherence.grpc.messages.topic.v1.PagedPosition pagedPosition
                        = any.unpack(com.oracle.coherence.grpc.messages.topic.v1.PagedPosition.class);

                return new PagedPosition(pagedPosition.getPage(), pagedPosition.getOffset());
                }
            }
        catch (InvalidProtocolBufferException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        throw new IllegalArgumentException("unsupported position typeURL: " + any.getTypeUrl());
        }

    /**
     * Create a protobuf publish result.
     *
     * @param result      the {@link PublishResult}
     * @param serializer  the serializer to use
     *
     * @return the protobuf publish result
     */
    public static com.oracle.coherence.grpc.messages.topic.v1.PublishResult
    toProtobufPublishResult(PublishResult result, Serializer serializer)
        {
        com.oracle.coherence.grpc.messages.topic.v1.PublishResult.Builder builder
                = com.oracle.coherence.grpc.messages.topic.v1.PublishResult.newBuilder();

        if (result != null)
            {
            PublishStatus publishStatus = switch (result.getStatus())
                {
                case Success -> PublishStatus.Success;
                case Retry -> PublishStatus.Success;
                case TopicFull -> PublishStatus.TopicFull;
                };

            int cAccepted = result.getAcceptedCount();

            builder.setChannel(result.getChannelId());
            builder.setStatus(publishStatus);
            builder.setAcceptedCount(cAccepted);
            builder.setRemainingCapacity(result.getRemainingCapacity());

            LongArray<Publisher.Status> aStatuses = result.getPublishStatus();
            LongArray<Throwable>        aErrors   = result.getErrors();

            for (long i = 0; i < cAccepted; i++)
                {
                PublishedValueStatus.Builder statusBuilder = PublishedValueStatus.newBuilder();
                if (aErrors != null && aErrors.exists(i))
                    {
                    Throwable throwable = aErrors.get(i);
                    statusBuilder.setError(ErrorsHelper.createErrorMessage(throwable, serializer));
                    }
                else
                    {
                    Publisher.Status status = aStatuses.get(i);
                    statusBuilder.setPosition(TopicHelper.toProtobufPosition(status.getPosition()));
                    }
                builder.addValueStatus(statusBuilder.build());
                }
            }
        return builder.build();
        }

    /**
     * Create a {@link PublishResult} from a protobuf publish result.
     *
     * @param response    the {@link TopicServiceResponse} containing the result
     * @param serializer  the serializer to use
     *
     * @return a {@link PublishResult} from the protobuf publish result
     */
    public static PublishResult fromProtoBufPublishResult(TopicServiceResponse response, Serializer serializer)
        {
        try
            {
            return fromProtoBufPublishResult(response.getMessage()
                    .unpack(com.oracle.coherence.grpc.messages.topic.v1.PublishResult.class), serializer);
            }
        catch (InvalidProtocolBufferException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Create a {@link PublishResult} from a protobuf publish result.
     *
     * @param result      the protobuf result
     * @param serializer  the serializer to use
     *
     * @return a {@link PublishResult} from the protobuf publish result
     */
    @SuppressWarnings("unchecked")
    public static PublishResult fromProtoBufPublishResult(com.oracle.coherence.grpc.messages.topic.v1.PublishResult result,
            Serializer serializer)
        {
        if (result == null)
            {
            return null;
            }

        int nChannel   = result.getChannel();
        int cAccepted  = result.getAcceptedCount();
        int cRemaining = result.getRemainingCapacity();

        PublishResult.Status status = switch (result.getStatus())
            {
            case Success -> PublishResult.Status.Success;
            case TopicFull -> PublishResult.Status.TopicFull;
            default -> throw new IllegalArgumentException("unrecognized status: " + result.getStatus());
            };

        LongArray<Publisher.Status> aStatus = new SimpleLongArray();
        LongArray<Throwable>        aError  = new SimpleLongArray();

        for (int i = 0; i < cAccepted; i++)
            {
            PublishedValueStatus valueStatus = result.getValueStatus(i);
            if (valueStatus.hasError())
                {
                ErrorMessage error     = valueStatus.getError();
                Throwable    throwable = BinaryHelper.fromByteString(error.getError(), serializer);
                aError.set(i, throwable);
                }
            else
                {
                Position position = fromProtobufPosition(valueStatus.getPosition());
                aStatus.set(i, new SimplePublisherStatus(nChannel, position));
                }
            }
        return new SimplePublishResult(nChannel, cAccepted, aStatus, aError, cRemaining, null, status);
        }

    /**
     * Create a {@link NamedTopicPublisher.PublisherEvent} from a protobuf {@link PublisherEvent}.
     *
     * @param publisher  the publisher that is the event source
     * @param response   the {@link TopicServiceResponse} containing the {@link PublisherEvent} to convert
     *
     * @return a {@link NamedTopicPublisher.PublisherEvent} from the protobuf {@link PublisherEvent}
     */
    public static NamedTopicPublisher.PublisherEvent fromProtobufPublisherEvent(PublisherConnector<?> publisher, TopicServiceResponse response)
        {
        PublisherEvent event = MessageHelper.unpack(response.getMessage(), PublisherEvent.class);
        return fromProtobufPublisherEvent(publisher, event);
        }

    /**
     * Create a {@link NamedTopicPublisher.PublisherEvent} from a protobuf {@link PublisherEvent}.
     *
     * @param publisher  the publisher that is the event source
     * @param event      the {@link PublisherEvent} to convert
     *
     * @return a {@link NamedTopicPublisher.PublisherEvent} from the protobuf {@link PublisherEvent}
     */
    public static NamedTopicPublisher.PublisherEvent fromProtobufPublisherEvent(PublisherConnector<?> publisher, PublisherEvent event)
        {
        int   cChannel  = event.getChannelsCount();
        int[] anChannel = new int[cChannel];
        for (int i = 0; i < cChannel; i++)
            {
            anChannel[i] = event.getChannels(i);
            }

        NamedTopicPublisher.PublisherEvent.Type type = switch (event.getType())
            {
            case PublisherConnected -> NamedTopicPublisher.PublisherEvent.Type.Connected;
            case PublisherDisconnected -> NamedTopicPublisher.PublisherEvent.Type.Disconnected;
            case PublisherChannelsFreed -> NamedTopicPublisher.PublisherEvent.Type.ChannelsFreed;
            case PublisherDestroyed -> NamedTopicPublisher.PublisherEvent.Type.Destroyed;
            case PublisherReleased -> NamedTopicPublisher.PublisherEvent.Type.Released;
            case PublisherEventUnknown, UNRECOGNIZED ->
                    throw new IllegalArgumentException("unknown event type: " + event.getType());
            };

        return new NamedTopicPublisher.PublisherEvent(publisher, type, anChannel);
        }

    /**
     * Create a protobuf {@link PublisherEvent} from a {@link NamedTopicFactory.PublisherEvent}.
     *
     * @param event  the {@link NamedTopicFactory.PublisherEvent} to convert
     *
     * @return a protobuf {@link PublisherEvent} from the {@link NamedTopicFactory.PublisherEvent}
     */
    public static PublisherEvent toProtobufPublisherEvent(NamedTopicFactory.PublisherEvent event)
        {
        NamedTopicPublisher.PublisherEvent.Type eventType = event.getType();

        PublisherEventType type = switch (eventType)
            {
            case Connected -> PublisherEventType.PublisherConnected;
            case Disconnected -> PublisherEventType.PublisherDisconnected;
            case Released -> PublisherEventType.PublisherReleased;
            case Destroyed -> PublisherEventType.PublisherDestroyed;
            case ChannelsFreed -> PublisherEventType.PublisherChannelsFreed;
            };

        PublisherEvent.Builder builder = PublisherEvent.newBuilder()
                .setType(type);

        int[] anChannel = event.getChannels();
        if (anChannel != null && anChannel.length > 0)
            {
            builder.addAllChannels(IntStream.of(event.getChannels()).boxed().toList());
            }

        return builder.build();
        }

    /**
     * Create a protobuf {@link PublisherEvent} from a {@link NamedTopicPublisher.PublisherEvent}.
     *
     * @param event  the {@link NamedTopicPublisher.PublisherEvent} to convert
     *
     * @return a protobuf {@link PublisherEvent} from the {@link NamedTopicPublisher.PublisherEvent}
     */
    public static PublisherEvent toProtobufPublisherEvent(NamedTopicPublisher.PublisherEvent event)
        {
        PublisherEventType type = switch (event.getType())
            {
            case Connected -> PublisherEventType.PublisherConnected;
            case Disconnected -> PublisherEventType.PublisherDisconnected;
            case Released -> PublisherEventType.PublisherReleased;
            case Destroyed -> PublisherEventType.PublisherDestroyed;
            case ChannelsFreed -> PublisherEventType.PublisherChannelsFreed;
            default -> throw new IllegalArgumentException("unknown event type: " + event.getType());
            };

        return PublisherEvent.newBuilder()
                .setType(type)
                .addAllChannels(IntStream.of(event.getChannels()).boxed().toList())
                .build();
        }

    /**
     * Create a {@link SubscriberConnector.SubscriberEvent} from a protobuf {@link SubscriberEvent}.
     *
     * @param connector  the {@link SubscriberConnector} that is the event source
     * @param response   the {@link TopicServiceResponse} containing the event to convert
     *
     * @return a {@link SubscriberConnector.SubscriberEvent} from the protobuf {@link SubscriberEvent}
     */
    public static SubscriberConnector.SubscriberEvent fromProtobufSubscriberEvent(SubscriberConnector<?> connector, TopicServiceResponse response)
        {
        SubscriberEvent event = MessageHelper.unpack(response.getMessage(), SubscriberEvent.class);
        return fromProtobufSubscriberEvent(connector, event);
        }

    /**
     * Create a {@link SubscriberConnector.SubscriberEvent} from a protobuf {@link SubscriberEvent}.
     *
     * @param connector  the {@link SubscriberConnector} that is the event source
     * @param event      the {@link SubscriberEvent} to convert
     *
     * @return a {@link SubscriberConnector.SubscriberEvent} from the protobuf {@link SubscriberEvent}
     */
    public static SubscriberConnector.SubscriberEvent fromProtobufSubscriberEvent(SubscriberConnector<?> connector, SubscriberEvent event)
        {
        SubscriberConnector.SubscriberEvent.Type type = switch (event.getType())
            {
            case SubscriberGroupDestroyed -> SubscriberConnector.SubscriberEvent.Type.GroupDestroyed;
            case SubscriberChannelAllocation -> SubscriberConnector.SubscriberEvent.Type.ChannelAllocation;
            case SubscriberChannelsLost -> SubscriberConnector.SubscriberEvent.Type.ChannelsLost;
            case SubscriberChannelPopulated -> SubscriberConnector.SubscriberEvent.Type.ChannelPopulated;
            case SubscriberChannelHead -> SubscriberConnector.SubscriberEvent.Type.ChannelHead;
            case SubscriberUnsubscribed -> SubscriberConnector.SubscriberEvent.Type.Unsubscribed;
            case SubscriberDestroyed -> SubscriberConnector.SubscriberEvent.Type.Destroyed;
            case SubscriberReleased -> SubscriberConnector.SubscriberEvent.Type.Released;
            case SubscriberDisconnected -> SubscriberConnector.SubscriberEvent.Type.Disconnected;
            default -> throw new IllegalArgumentException("unknown event type: " + event.getType());
            };

        List<Integer>      list       = event.getChannelsList();
        int[]              anChannel  = list.stream().mapToInt(i -> i).toArray();
        SortedSet<Integer> setChannel = new TreeSet<>(list);

        return new SubscriberConnector.SubscriberEvent(connector, type, anChannel, setChannel);
        }

    /**
     * Create a protobuf {@link SubscriberEvent} from a {@link SubscriberConnector.SubscriberEvent}.
     *
     * @param event  the {@link SubscriberConnector.SubscriberEvent} to convert
     *
     * @return a protobuf {@link SubscriberEvent} from the {@link SubscriberConnector.SubscriberEvent}
     */
    public static SubscriberEvent toProtobufSubscriberEvent(SubscriberConnector.SubscriberEvent event)
        {
        return toProtobufSubscriberEvent(event.getType(), event.getAllocatedChannels(), event.getPopulatedChannels());
        }

    /**
     * Create a protobuf {@link SubscriberEvent} from a {@link SubscriberConnector.SubscriberEvent}.
     *
     * @param event  the {@link SubscriberConnector.SubscriberEvent} to convert
     *
     * @return a protobuf {@link SubscriberEvent} from the {@link SubscriberConnector.SubscriberEvent}
     */
    public static SubscriberEvent toProtobufSubscriberEvent(NamedTopicFactory.SubscriberChannelEvent event)
        {
        return toProtobufSubscriberEvent(event.getEventType(), event.getAllocatedChannels(), event.getPopulatedChannels());
        }

    /**
     * Create a protobuf {@link SubscriberEvent} from a {@link SubscriberConnector.SubscriberEvent}.
     *
     * @param eventType             the event type
     * @param setAllocatedChannels  the allocated channels
     * @param anPopulatedChannels   the populated channels
     *
     * @return a protobuf {@link SubscriberEvent} from the {@link SubscriberConnector.SubscriberEvent}
     */
    public static SubscriberEvent toProtobufSubscriberEvent(SubscriberConnector.SubscriberEvent.Type eventType,
            Set<Integer> setAllocatedChannels, int[] anPopulatedChannels)
        {
        SubscriberEventType type = switch (eventType)
            {
            case GroupDestroyed -> SubscriberEventType.SubscriberGroupDestroyed;
            case ChannelAllocation -> SubscriberEventType.SubscriberChannelAllocation;
            case ChannelsLost -> SubscriberEventType.SubscriberChannelsLost;
            case ChannelPopulated -> SubscriberEventType.SubscriberChannelPopulated;
            case ChannelHead -> SubscriberEventType.SubscriberChannelHead;
            case Unsubscribed -> SubscriberEventType.SubscriberUnsubscribed;
            case Destroyed -> SubscriberEventType.SubscriberDestroyed;
            case Released -> SubscriberEventType.SubscriberReleased;
            case Disconnected -> SubscriberEventType.SubscriberDisconnected;
            };

        Collection<Integer> colChannel = setAllocatedChannels;
        if (colChannel == null || colChannel.isEmpty())
            {
            if (anPopulatedChannels == null || anPopulatedChannels.length == 0)
                {
                colChannel = Collections.emptyList();
                }
            else
                {
                colChannel = IntStream.of(anPopulatedChannels).boxed().toList();
                }
            }

        return SubscriberEvent.newBuilder()
                .setType(type)
                .addAllChannels(colChannel)
                .build();
        }

    /**
     * Create a {@link SubscriberId} from a {@link EnsureSubscriberResponse}.
     *
     * @param response  the {@link EnsureSubscriberResponse} to obtain the subscriber identifier from
     *
     * @return a {@link SubscriberId} from the {@link EnsureSubscriberResponse}
     */
    public static SubscriberId fromProtobufSubscriberId(EnsureSubscriberResponse response)
        {
        return fromProtobufSubscriberId(response.getSubscriberId());
        }

    /**
     * Create a {@link SubscriberId} from a protobuf subscriber identifier.
     *
     * @param subscriberId  the protobuf subscriber identifier to convert
     *
     * @return a {@link SubscriberId} from the protobuf subscriber identifier
     */
    public static SubscriberId fromProtobufSubscriberId(com.oracle.coherence.grpc.messages.topic.v1.SubscriberId subscriberId)
        {
        UUID uuid = new UUID(subscriberId.getUuid().toByteArray());
        long id   = subscriberId.getId();
        return new SubscriberId(id, uuid);
        }

    /**
     * Create a protobuf subscriber identifier from a {@link SubscriberId}.
     *
     * @param subscriberId  the {@link SubscriberId} to convert
     *
     * @return a protobuf subscriber identifier from the {@link SubscriberId}
     */
    public static com.oracle.coherence.grpc.messages.topic.v1.SubscriberId toProtobufSubscriberId(SubscriberId subscriberId)
        {
        return com.oracle.coherence.grpc.messages.topic.v1.SubscriberId.newBuilder()
                .setId(subscriberId.getId())
                .setUuid(ByteString.copyFrom(subscriberId.getUID().toByteArray()))
                .build();
        }

    /**
     * Create a {@link SubscriberGroupId} from a {@link EnsureSubscriberResponse}.
     *
     * @param response  the {@link EnsureSubscriberResponse} to obtain the {@link SubscriberGroupId} from
     *
     * @return a {@link SubscriberGroupId} from the {@link EnsureSubscriberResponse}
     */
    public static SubscriberGroupId fromProtobufSubscriberGroupId(EnsureSubscriberResponse response)
        {
        return fromProtobufSubscriberGroupId(response.getGroupId());
        }

    /**
     * Create a {@link SubscriberGroupId} from a protobuf subscriber group identifier.
     *
     * @param groupId  the protobuf subscriber group identifier to convert
     *
     * @return a {@link SubscriberGroupId} from the protobuf subscriber group identifier
     */
    public static SubscriberGroupId fromProtobufSubscriberGroupId(com.oracle.coherence.grpc.messages.topic.v1.SubscriberGroupId groupId)
        {
        return SubscriberGroupId.unsafe(groupId.getName(), groupId.getId());
        }

    /**
     * Create a protobuf subscriber group identifier from a {@link SubscriberGroupId}.
     *
     * @param groupId  the {@link SubscriberGroupId} to convert
     *
     * @return a protobuf subscriber group identifier from the {@link SubscriberGroupId}
     */
    public static com.oracle.coherence.grpc.messages.topic.v1.SubscriberGroupId toProtobufSubscriberGroupId(SubscriberGroupId groupId)
        {
        return com.oracle.coherence.grpc.messages.topic.v1.SubscriberGroupId.newBuilder()
                .setName(groupId.getGroupName())
                .setId(groupId.getMemberId())
                .build();
        }

    /**
     * Convert a map of channel keys to positions into a {@link MapOfChannelAndPosition}.
     *
     * @param map  the map to convert
     *
     * @return the {@link MapOfChannelAndPosition} from the specified map
     */
    public static MapOfChannelAndPosition toProtobufChannelAndPosition(Map<Integer, ? extends Position> map)
        {
        MapOfChannelAndPosition.Builder builder = MapOfChannelAndPosition.newBuilder();

        for (Map.Entry<Integer, ? extends Position> entry : map.entrySet())
            {
            builder.putPositions(entry.getKey(), toProtobufPosition(entry.getValue()));
            }

        return builder.build();
        }

    /**
     * Convert a {@link MapOfChannelAndPosition} to a map of channel keys to {@link Position}.
     *
     * @param col  the {@link MapOfChannelAndPosition} to convert
     *
     * @return  a map of channel keys to {@link Position}
     */
    public static Map<Integer, Position> fromProtobufChannelAndPosition(MapOfChannelAndPosition col)
        {
        Map<Integer, Position> map = new HashMap<>();
        if (col != null)
            {
            for (Map.Entry<Integer, TopicPosition> entry : col.getPositionsMap().entrySet())
                {
                map.put(entry.getKey(), fromProtobufPosition(entry.getValue()));
                }
            }
        return map;
        }

    /**
     * Convert a map of channel keys to {@link Instant} into a {@link MapOfChannelAndTimestamp}.
     *
     * @param map  the map to convert
     *
     * @return the {@link MapOfChannelAndTimestamp} from the specified map
     */
    public static MapOfChannelAndTimestamp toProtobufChannelAndTimestamp(Map<Integer, ? extends Instant> map)
        {
        MapOfChannelAndTimestamp.Builder builder = MapOfChannelAndTimestamp.newBuilder();

        for (Map.Entry<Integer, ? extends Instant> entry : map.entrySet())
            {
            Instant   instant   = entry.getValue();
            Timestamp timestamp = Timestamp.newBuilder()
                    .setSeconds(instant.getEpochSecond())
                    .setNanos(instant.getNano())
                    .build();
            builder.putTimestamps(entry.getKey(), timestamp);
            }

        return builder.build();
        }

    /**
     * Convert a {@link MapOfChannelAndTimestamp} to a map of channel keys to {@link Instant}.
     *
     * @param col  the {@link MapOfChannelAndTimestamp} to convert
     *
     * @return  a map of channel keys to {@link Instant}
     */
    public static Map<Integer, Instant> fromProtobufChannelAndTimestamp(MapOfChannelAndTimestamp col)
        {
        Map<Integer, Instant> map = new HashMap<>();
        if (col != null)
            {
            for (Map.Entry<Integer, Timestamp> entry : col.getTimestampsMap().entrySet())
                {
                Timestamp timestamp = entry.getValue();
                map.put(entry.getKey(), Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()));
                }
            }
        return map;
        }

    /**
     * Convert a {@link Subscriber.Element} into a {@link TopicElement}.
     *
     * @param element  the {@link Subscriber.Element} to convert
     *
     * @return a {@link TopicElement} from the contents of the {@link Subscriber.Element}
     */
    public static TopicElement toProtobufTopicElement(Subscriber.Element<?> element)
        {
        Instant   instant   = element.getTimestamp();
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();

        return TopicElement.newBuilder()
                           .setChannel(element.getChannel())
                           .setTimestamp(timestamp)
                           .setPosition(toProtobufPosition(element.getPosition()))
                           .setValue(BinaryHelper.toByteString(element.getBinaryValue()))
                           .build();
        }
    }
