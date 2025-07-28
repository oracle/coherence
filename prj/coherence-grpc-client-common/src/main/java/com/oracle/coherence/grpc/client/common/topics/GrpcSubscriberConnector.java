/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common.topics;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;

import com.google.protobuf.util.Timestamps;

import com.oracle.coherence.common.base.Exceptions;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.ErrorsHelper;
import com.oracle.coherence.grpc.MessageHelper;
import com.oracle.coherence.grpc.TopicHelper;

import com.oracle.coherence.grpc.client.common.GrpcConnection;
import com.oracle.coherence.grpc.client.common.SimpleStreamObserver;
import com.oracle.coherence.grpc.messages.common.v1.CollectionOfInt32;

import com.oracle.coherence.grpc.messages.topic.v1.ChannelAndPosition;


import com.oracle.coherence.grpc.messages.topic.v1.CommitResponse;
import com.oracle.coherence.grpc.messages.topic.v1.EnsureSubscriptionRequest;
import com.oracle.coherence.grpc.messages.topic.v1.InitializeSubscriptionRequest;
import com.oracle.coherence.grpc.messages.topic.v1.InitializeSubscriptionResponse;
import com.oracle.coherence.grpc.messages.topic.v1.MapOfChannelAndPosition;
import com.oracle.coherence.grpc.messages.topic.v1.ReceiveRequest;
import com.oracle.coherence.grpc.messages.topic.v1.ReceiveResponse;
import com.oracle.coherence.grpc.messages.topic.v1.SeekRequest;
import com.oracle.coherence.grpc.messages.topic.v1.SeekResponse;
import com.oracle.coherence.grpc.messages.topic.v1.SeekedPositions;
import com.oracle.coherence.grpc.messages.topic.v1.SimpleReceiveRequest;
import com.oracle.coherence.grpc.messages.topic.v1.SimpleReceiveResponse;
import com.oracle.coherence.grpc.messages.topic.v1.TopicElement;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequestType;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceResponse;

import com.tangosol.internal.net.topic.BaseRemoteSubscriber;
import com.tangosol.internal.net.topic.ReceiveResult;
import com.tangosol.internal.net.topic.SeekResult;
import com.tangosol.internal.net.topic.SimpleReceiveResult;
import com.tangosol.internal.net.topic.TopicSubscription;

import com.tangosol.internal.net.topic.impl.paged.agent.SeekProcessor;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import com.tangosol.io.Serializer;

import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.TopicDependencies;

import com.tangosol.util.Binary;
import io.grpc.stub.StreamObserver;

import java.time.Instant;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;

import java.util.concurrent.CompletableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * A gRPC remote subscriber connector.
 *
 * @param <V>  the type of elements received from the topic.
 *
 * @author Jonathan Knight  2025.01.25
 */
public class GrpcSubscriberConnector<V>
        extends BaseRemoteSubscriber<V>
        implements GrpcConnection.ConnectionListener
    {
    /**
     * Create a {@link GrpcSubscriberConnector}.
     *
     * @param connector    the parent {@link GrpcNamedTopicConnector}
     * @param proxyId      the gRPC server proxy identifier
     * @param connection   the connection to use to send and receive messages
     * @param sTopicName   the topic name
     * @param subscriberId the subscriber identifier
     * @param groupId      the subscriber group identifier
     * @param fSimple      {@code true} if the subscriber is using the simple API
     */
    public GrpcSubscriberConnector(GrpcNamedTopicConnector<?> connector, int proxyId,
                                   TopicServiceGrpcConnection connection, String sTopicName, SubscriberId subscriberId,
                                   SubscriberGroupId groupId, boolean fSimple)
        {
        super(sTopicName, subscriberId, groupId, fSimple);
        f_connector  = connector;
        f_nProxyId   = proxyId;
        f_connection = connection;
        f_serializer = connector.getTopicService().getSerializer();

        StreamObserver<TopicServiceResponse> eventObserver = new SimpleStreamObserver<>(this::onEvent);
        f_listener = new GrpcConnection.Listener<>(eventObserver, r -> r.getProxyId() == proxyId);
        connection.addResponseObserver(f_listener);
        connection.addConnectionListener(this);
        }

    @Override
    public void postConstruct(ConnectedSubscriber<V> subscriber)
        {
        m_subscriber = subscriber;
        }

    @Override
    public boolean isActive()
        {
        return f_connection.isConnected();
        }

    @Override
    public void ensureConnected()
        {
        }

    @Override
    public void close()
        {
        f_connection.removeConnectionListener(this);
        if (f_listener != null)
            {
            f_connection.removeResponseObserver(f_listener);
            }
        }

    @Override
    public Position[] initialize(ConnectedSubscriber<V> subscriber, boolean fForceReconnect, boolean fReconnect, boolean fDisconnected)
        {
        if (f_fSimple)
            {
            throw new UnsupportedOperationException("this method is not supported for a simple subscriber");
            }

        InitializeSubscriptionRequest request = InitializeSubscriptionRequest.newBuilder()
                .setDisconnected(fDisconnected)
                .setForceReconnect(fForceReconnect)
                .setReconnect(fReconnect)
                .build();

        InitializeSubscriptionResponse response = send(TopicServiceRequestType.InitializeSubscription, request, InitializeSubscriptionResponse.class);

        m_subscriptionId      = response.getSubscriptionId();
        m_connectionTimestamp = Timestamps.toMillis(response.getTimestamp());
        return response.getHeadsList()
                .stream()
                .map(TopicHelper::fromProtobufPosition)
                .toArray(Position[]::new);
        }

    @Override
    public boolean ensureSubscription(ConnectedSubscriber<V> subscriber, long subscriptionId, boolean fForceReconnect)
        {
        if (f_fSimple)
            {
            throw new UnsupportedOperationException("this method is not supported for a simple subscriber");
            }

        EnsureSubscriptionRequest request = EnsureSubscriptionRequest.newBuilder()
                .setSubscriptionId(subscriptionId)
                .setForceReconnect(fForceReconnect)
                .build();

        BoolValue value = send(TopicServiceRequestType.EnsureSubscription, request, BoolValue.class);
        return value != null && value.getValue();
        }

    @Override
    protected void sendHeartbeat(boolean fAsync)
        {
        if (fAsync)
            {
            poll(TopicServiceRequestType.SubscriberHeartbeat, BoolValue.of(fAsync));
            }
        else
            {
            send(TopicServiceRequestType.SubscriberHeartbeat, BoolValue.of(fAsync));
            }
        }

    @Override
    protected SimpleReceiveResult receiveInternal(int nChannel, Position headPosition, long lVersion, int cMaxElements)
        {
        if (f_fSimple)
            {
            throw new UnsupportedOperationException("Method should not be called when using the simple API");
            }
        return receiveInternal(nChannel, cMaxElements);
        }

    public SimpleReceiveResult receive(int cMaxElements)
        {
        if (!f_fSimple)
            {
            throw new UnsupportedOperationException("Method should not be called when not using the simple API");
            }
        return receiveInternal(-1, cMaxElements);
        }

    protected SimpleReceiveResult receiveInternal(int nChannel, int cMaxElements)
        {
        CompletableFuture<SimpleReceiveResult> future;
        if (f_fSimple)
            {
            SimpleReceiveRequest request = SimpleReceiveRequest.newBuilder()
                    .setMaxMessages(cMaxElements)
                    .build();

            future = poll(TopicServiceRequestType.SimpleReceive, request, SimpleReceiveResponse.class)
                            .thenApply(this::onSimpleReceiveResponse);
            }
        else
            {
            ReceiveRequest request = ReceiveRequest.newBuilder()
                    .setChannel(nChannel)
                    .setMaxMessages(cMaxElements)
                    .build();

            future = poll(TopicServiceRequestType.Receive, request, ReceiveResponse.class)
                            .thenApply(this::onReceiveResponse);
            }

        long cMillis = getRequestTimeoutMillis();
        try
            {
            return future.get(cMillis, TimeUnit.MILLISECONDS);
            }
        catch (ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        catch (InterruptedException | TimeoutException e)
            {
            future.cancel(true);
            throw new RequestTimeoutException(e);
            }
        }

    protected void onEvent(TopicServiceResponse response)
        {
        dispatchEvent(TopicHelper.fromProtobufSubscriberEvent(this, response));
        }

    protected SimpleReceiveResult onReceiveResponse(ReceiveResponse response)
        {
        Position head = TopicHelper.fromProtobufPosition(response.getHeadPosition());

        ReceiveResult.Status status = switch (response.getStatus())
            {
            case ReceiveSuccess -> ReceiveResult.Status.Success;
            case ChannelExhausted -> ReceiveResult.Status.Exhausted;
            case ChannelNotAllocatedChannel -> ReceiveResult.Status.NotAllocatedChannel;
            case UnknownSubscriber -> ReceiveResult.Status.UnknownSubscriber;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown subscriber status: " + response.getStatus());
            };

        Queue<Binary> elements = response.getValuesList()
                .stream()
                .map(BinaryHelper::toBinary)
                .collect(Collectors.toCollection(LinkedList::new));

        return new SimpleReceiveResult(elements, response.getRemainingValues(), status, head);
        }

    protected SimpleReceiveResult onSimpleReceiveResponse(SimpleReceiveResponse response)
        {
        ReceiveResult.Status status = switch (response.getStatus())
            {
            case ReceiveSuccess -> ReceiveResult.Status.Success;
            case ChannelExhausted -> ReceiveResult.Status.Exhausted;
            case ChannelNotAllocatedChannel -> ReceiveResult.Status.NotAllocatedChannel;
            case UnknownSubscriber -> ReceiveResult.Status.UnknownSubscriber;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown subscriber status: " + response.getStatus());
            };

        List<TopicElement> elements = response.getValuesList();

        Queue<Binary> binaries = elements.stream()
                .map(TopicHelper::fromProtobufTopicElement)
                .collect(Collectors.toCollection(LinkedList::new));

        return new SimpleReceiveResult(binaries, 0, status);
        }

    @Override
    protected void commitInternal(int nChannel, Position position, CommitHandler handler)
        {
        ChannelAndPosition request = ChannelAndPosition.newBuilder()
                .setChannel(nChannel)
                .setPosition(TopicHelper.toProtobufPosition(position))
                .build();

        CommitResponse response = send(TopicServiceRequestType.CommitPosition, request, CommitResponse.class);
        Position       head     = TopicHelper.fromProtobufPosition(response.getPosition());

        Subscriber.CommitResultStatus status = switch (response.getStatus())
            {
            case Committed -> Subscriber.CommitResultStatus.Committed;
            case AlreadyCommitted -> Subscriber.CommitResultStatus.AlreadyCommitted;
            case Rejected -> Subscriber.CommitResultStatus.Rejected;
            case Unowned -> Subscriber.CommitResultStatus.Unowned;
            case NothingToCommit -> Subscriber.CommitResultStatus.NothingToCommit;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown subscriber status: " + response.getStatus());
            };

        Throwable error = null;
        if (response.hasError())
            {
            error = ErrorsHelper.createException(response.getError(), f_serializer);
            }

        Subscriber.CommitResult result = new Subscriber.CommitResult(nChannel, position, status, error);
        handler.committed(result, head);
        }

    @Override
    public Subscriber.Element<V> peek(int nChannel, Position position)
        {
        ChannelAndPosition request = ChannelAndPosition.newBuilder()
                .setChannel(nChannel)
                .setPosition(TopicHelper.toProtobufPosition(position))
                .build();

        CompletableFuture<Subscriber.Element<V>> future = poll(TopicServiceRequestType.PeekAtPosition, request, TopicElement.class)
                .thenApply(this::createElement);

        long cMillis = getRequestTimeoutMillis();
        try
            {
            return future.get(cMillis, TimeUnit.MILLISECONDS);
            }
        catch (ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        catch (InterruptedException | TimeoutException e)
            {
            future.cancel(true);
            throw new RequestTimeoutException(e);
            }
        }

    /**
     * Create a {@link Subscriber.Element} from a protobuf {@link TopicElement}.
     *
     * @param element the protobuf {@link TopicElement}
     * @return a {@link Subscriber.Element}
     */
    protected Subscriber.Element<V> createElement(TopicElement element)
        {
        return m_subscriber.createElement(BinaryHelper.toBinary(element.getValue()), element.getChannel());
        }

    @Override
    public int getRemainingMessages(SubscriberGroupId groupId, int[] anChannel)
        {
        return f_connector.getRemainingMessages(groupId.getGroupName(), anChannel);
        }

    @Override
    public boolean isCommitted(SubscriberGroupId groupId, int nChannel, Position position)
        {
        ChannelAndPosition request = ChannelAndPosition.newBuilder()
                .setChannel(nChannel)
                .setPosition(TopicHelper.toProtobufPosition(position))
                .build();

        CompletableFuture<Boolean> future = poll(TopicServiceRequestType.IsPositionCommitted, request, BoolValue.class)
                .thenApply(value -> value != null && value.getValue());

        long cMillis = getRequestTimeoutMillis();
        try
            {
            return future.get(cMillis, TimeUnit.MILLISECONDS);
            }
        catch (ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        catch (InterruptedException | TimeoutException e)
            {
            future.cancel(true);
            throw new RequestTimeoutException(e);
            }
        }

    @Override
    public TopicSubscription getSubscription(ConnectedSubscriber<V> subscriber, long id)
        {
        // should not be called from the gRPC client
        throw new UnsupportedOperationException();
        }

    @Override
    public SortedSet<Integer> getOwnedChannels(ConnectedSubscriber<V> subscriber)
        {
        CompletableFuture<TreeSet<Integer>> future = poll(TopicServiceRequestType.GetOwnedChannels, CollectionOfInt32.class)
                .thenApply(col -> new TreeSet<>(col.getValuesList()));

        long cMillis = getRequestTimeoutMillis();
        try
            {
            return future.get(cMillis, TimeUnit.MILLISECONDS);
            }
        catch (ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        catch (InterruptedException | TimeoutException e)
            {
            future.cancel(true);
            throw new RequestTimeoutException(e);
            }
        }

    @Override
    public Map<Integer, Position> getTopicHeads(int[] anChannel)
        {
        CollectionOfInt32 col = MessageHelper.toCollectionOfInt32(anChannel);
        CompletableFuture<Map<Integer, Position>> future = poll(TopicServiceRequestType.GetSubscriberHeads, col, MapOfChannelAndPosition.class)
                .thenApply(TopicHelper::fromProtobufChannelAndPosition);

        long cMillis = getRequestTimeoutMillis();
        try
            {
            return future.get(cMillis, TimeUnit.MILLISECONDS);
            }
        catch (ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        catch (InterruptedException | TimeoutException e)
            {
            future.cancel(true);
            throw new RequestTimeoutException(e);
            }
        }

    @Override
    public Map<Integer, Position> getTopicTails()
        {
        CompletableFuture<Map<Integer, Position>> future = poll(TopicServiceRequestType.GetTails, MapOfChannelAndPosition.class)
                .thenApply(TopicHelper::fromProtobufChannelAndPosition);

        long cMillis = getRequestTimeoutMillis();
        try
            {
            return future.get(cMillis, TimeUnit.MILLISECONDS);
            }
        catch (ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        catch (InterruptedException | TimeoutException e)
            {
            future.cancel(true);
            throw new RequestTimeoutException(e);
            }
        }

    @Override
    public Map<Integer, Position> getLastCommittedInGroup(SubscriberGroupId groupId)
        {
        CompletableFuture<Map<Integer, Position>> future = poll(TopicServiceRequestType.GetLastCommited, MapOfChannelAndPosition.class)
                .thenApply(TopicHelper::fromProtobufChannelAndPosition);

        long cMillis = getRequestTimeoutMillis();
        try
            {
            return future.get(cMillis, TimeUnit.MILLISECONDS);
            }
        catch (ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        catch (InterruptedException | TimeoutException e)
            {
            future.cancel(true);
            throw new RequestTimeoutException(e);
            }
        }

    @Override
    public Map<Integer, SeekResult> seekToPosition(ConnectedSubscriber<V> subscriber, Map<Integer, Position> map)
        {
        SeekRequest request = SeekRequest.newBuilder()
                .setByPosition(TopicHelper.toProtobufChannelAndPosition(map))
                .build();

        CompletableFuture<Map<Integer, SeekResult>> future = poll(TopicServiceRequestType.SeekSubscriber, request, SeekResponse.class)
                .thenApply(this::handleSeekResponse);

        long cMillis = getRequestTimeoutMillis();
        try
            {
            return future.get(cMillis, TimeUnit.MILLISECONDS);
            }
        catch (ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        catch (InterruptedException | TimeoutException e)
            {
            future.cancel(true);
            throw new RequestTimeoutException(e);
            }
        }

    @Override
    public Map<Integer, SeekResult> seekToTimestamp(ConnectedSubscriber<V> subscriber, Map<Integer, Instant> map)
        {
        SeekRequest request = SeekRequest.newBuilder()
                .setByTimestamp(TopicHelper.toProtobufChannelAndTimestamp(map))
                .build();

        CompletableFuture<Map<Integer, SeekResult>> future = poll(TopicServiceRequestType.SeekSubscriber, request, SeekResponse.class)
                .thenApply(this::handleSeekResponse);

        long cMillis = getRequestTimeoutMillis();
        try
            {
            return future.get(cMillis, TimeUnit.MILLISECONDS);
            }
        catch (ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        catch (InterruptedException | TimeoutException e)
            {
            future.cancel(true);
            throw new RequestTimeoutException(e);
            }
        }

    protected Map<Integer, SeekResult> handleSeekResponse(SeekResponse response)
        {
        Map<Integer, SeekResult>      map       = new HashMap<>();
        Map<Integer, SeekedPositions> seekedMap = response.getPositionsMap();
        for (Map.Entry<Integer, SeekedPositions> entry : seekedMap.entrySet())
            {
            SeekedPositions pos    = entry.getValue();
            Position        head   = TopicHelper.fromProtobufPosition(pos.getHead());
            Position        seeked = TopicHelper.fromProtobufPosition(pos.getSeekedTo());
            map.put(entry.getKey(), new SeekProcessor.Result(head, seeked));
            }
        return map;
        }

    @Override
    public void closeSubscription(ConnectedSubscriber<V> subscriber, boolean fDestroyed)
        {
        if (f_connection.isConnected())
            {
            f_connection.send(0, TopicServiceRequestType.DestroySubscriber, Int32Value.of(f_nProxyId));
            f_connection.close();
            }
        }

    @Override
    public TopicDependencies getTopicDependencies()
        {
        return f_connector.getTopicService()
                .getTopicBackingMapManager()
                .getTopicDependencies(f_sTopicName);
        }

    @Override
    public void onConnectionEvent(GrpcConnection.ConnectionEvent event)
        {
        if (event.getType() == GrpcConnection.ConnectionEvent.Type.Disconnected)
            {
            dispatchEvent(new SubscriberEvent(this, SubscriberEvent.Type.Disconnected));
            }
        }

    // ----- helper methods -------------------------------------------------

    protected long getRequestTimeoutMillis()
        {
        return f_connector.getTopicService().getDependencies().getRequestTimeoutMillis();
        }

    protected CompletableFuture<TopicServiceResponse> poll(TopicServiceRequestType type)
        {
        return f_connection.poll(f_nProxyId, type);
        }

    protected CompletableFuture<TopicServiceResponse> poll(TopicServiceRequestType type, Message message)
        {
        return f_connection.poll(f_nProxyId, type, message);
        }

    protected <M extends Message> CompletableFuture<M> poll(TopicServiceRequestType requestType, Class<M> resultType)
        {
        return f_connection.poll(f_nProxyId, requestType)
                .thenApply(m -> f_connection.unpackMessage(m, resultType));
        }

    protected <M extends Message> CompletableFuture<M> poll(TopicServiceRequestType requestType, Message message, Class<M> resultType)
        {
        return f_connection.poll(f_nProxyId, requestType, message)
                .thenApply(m -> f_connection.unpackMessage(m, resultType));
        }

    protected TopicServiceResponse send(TopicServiceRequestType requestType, Message message)
        {
        CompletableFuture<TopicServiceResponse> future  = poll(requestType, message);
        long                                    cMillis = getRequestTimeoutMillis();
        try
            {
            return future.get(cMillis, TimeUnit.MILLISECONDS);
            }
        catch (ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        catch (InterruptedException | TimeoutException e)
            {
            future.cancel(true);
            throw new RequestTimeoutException(e);
            }
        }

    protected <M extends Message> M send(TopicServiceRequestType requestType, Message message, Class<M> resultType)
        {
        CompletableFuture<M> future  = poll(requestType, message, resultType);
        long                 cMillis = getRequestTimeoutMillis();
        try
            {
            return future.get(cMillis, TimeUnit.MILLISECONDS);
            }
        catch (ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        catch (InterruptedException | TimeoutException e)
            {
            future.cancel(true);
            throw new RequestTimeoutException(e);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The parent {@link GrpcNamedTopicConnector}.
     */
    private final GrpcNamedTopicConnector<?> f_connector;

    /**
     * The gRPC connection to use to send and receive messages.
     */
    private final TopicServiceGrpcConnection f_connection;

    private final GrpcConnection.Listener<TopicServiceResponse> f_listener;

    /**
     * The gRPC subscriber identifier;
     */
    private final int f_nProxyId;

    /**
     * The topic service serializer.
     */
    private final Serializer f_serializer;

    /**
     * The subscriber using this connector.
     */
    private ConnectedSubscriber<V> m_subscriber;
    }
