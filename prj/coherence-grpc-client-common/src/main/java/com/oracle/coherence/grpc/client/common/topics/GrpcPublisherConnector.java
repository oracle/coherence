/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common.topics;

import com.google.protobuf.Int32Value;
import com.oracle.coherence.grpc.BinaryHelper;

import com.oracle.coherence.grpc.TopicHelper;

import com.oracle.coherence.grpc.client.common.GrpcConnection;
import com.oracle.coherence.grpc.client.common.SimpleStreamObserver;
import com.oracle.coherence.grpc.messages.topic.v1.PublishRequest;
import com.oracle.coherence.grpc.messages.topic.v1.PublishResult;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequestType;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceResponse;

import com.tangosol.internal.net.topic.BaseRemotePublisher;
import com.tangosol.internal.net.topic.NamedTopicPublisher;
import com.tangosol.internal.net.topic.PublisherChannelConnector;
import com.tangosol.internal.net.topic.PublisherConnector;

import com.tangosol.net.topic.Publisher;

import com.tangosol.util.Binary;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A {@link PublisherConnector} that uses a gRPC connection to
 * send and receive requests and responses.
 *
 * @param <V>  the type of value to publish
 *
 * @author Jonathan Knight  2025.01.25
 */
public class GrpcPublisherConnector<V>
        extends BaseRemotePublisher<V>
        implements PublisherConnector<V>, GrpcConnection.ConnectionListener
    {
    /**
     * Create a {@link GrpcPublisherConnector}.
     *
     * @param connection  the gRPC connection
     * @param nProxyId    the proxy server identifier
     * @param nId         the unique publisher identifier
     * @param cChannel    the default channel count for the topic
     * @param options     the publisher options
     */
    public GrpcPublisherConnector(TopicServiceGrpcConnection connection, int nProxyId,
            long nId, int cChannel, Publisher.Option<? super V>[] options)
        {
        super(nId, cChannel, options);
        f_connection = connection;
        f_nProxyId   = nProxyId;

        StreamObserver<TopicServiceResponse> eventObserver = new SimpleStreamObserver<>(this::onEvent);
        f_listener = new GrpcConnection.Listener<>(eventObserver, r -> r.getProxyId() == nProxyId);
        connection.addResponseObserver(f_listener);
        connection.addConnectionListener(this);
        }

    @Override
    public boolean isActive()
        {
        return f_connection.isConnected();
        }

    @Override
    public void close()
        {
        f_connection.send(0, TopicServiceRequestType.DestroyPublisher, Int32Value.of(f_nProxyId));
        f_connection.close();
        super.close();
        if (f_listener != null)
            {
            f_connection.removeResponseObserver(f_listener);
            }
        f_connection.removeConnectionListener(this);
        }

    @Override
    public void ensureConnected()
        {
        }

    @Override
    public PublisherChannelConnector<V> createChannelConnector(int nChannel)
        {
        return new ChannelConnector(getId(), nChannel);
        }

    @Override
    public void onConnectionEvent(GrpcConnection.ConnectionEvent event)
        {
        if (event.getType() == GrpcConnection.ConnectionEvent.Type.Disconnected)
            {
            dispatchEvent(new NamedTopicPublisher.PublisherEvent(this, NamedTopicPublisher.PublisherEvent.Type.Disconnected));
            }
        }

    // ----- helper methods -------------------------------------------------

    protected void onEvent(TopicServiceResponse response)
        {
        dispatchEvent(TopicHelper.fromProtobufPublisherEvent(this, response));
        }

    // ----- inner class: ChannelConnector ----------------------------------

    /**
     * The publisher channel connector.
     */
    protected class ChannelConnector
            extends BaseChannelConnector
        {
        /**
         * Create a {@link ChannelConnector}.
         *
         * @param nId       the unique identifier for the publisher
         * @param nChannel  the channel identifier for this connector
         */
        public ChannelConnector(long nId, int nChannel)
            {
            super(nId, nChannel);
            }

        @Override
        protected CompletionStage<com.tangosol.internal.net.topic.PublishResult> offerInternal(List<Binary> listBinary, int nNotifyPostFull)
            {
            PublishRequest request = PublishRequest.newBuilder()
                    .setChannel(f_nChannel)
                    .setNotificationIdentifier(nNotifyPostFull)
                    .addAllValues(BinaryHelper.toListOfByteString(listBinary))
                    .build();

            CompletableFuture<TopicServiceResponse> future = f_connection.poll(f_nProxyId, TopicServiceRequestType.Publish, request);
            future.join();
            return future.thenApply(response ->
                {
                PublishResult result = f_connection.unpackMessage(response, PublishResult.class);
                return TopicHelper.fromProtoBufPublishResult(result, getTopicService().getSerializer());
                });
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The gRPC connection to send and receive requests.
     */
    private final TopicServiceGrpcConnection f_connection;

    /**
     * The proxy server identifier.
     */
    private final int f_nProxyId;

    /**
     * The listener for publisher events.
     */
    private final GrpcConnection.Listener<TopicServiceResponse> f_listener;
    }
