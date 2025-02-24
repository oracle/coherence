/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common.topics;

import com.google.protobuf.Any;
import com.google.protobuf.StringValue;

import com.oracle.coherence.grpc.MessageHelper;
import com.oracle.coherence.grpc.client.common.BaseClientChannel;
import com.oracle.coherence.grpc.client.common.BaseGrpcClient;
import com.oracle.coherence.grpc.client.common.ClientProtocol;
import com.oracle.coherence.grpc.client.common.GrpcConnection;

import com.oracle.coherence.grpc.client.common.SimpleStreamObserver;
import com.oracle.coherence.grpc.messages.topic.v1.EnsureSubscriberGroupRequest;
import com.oracle.coherence.grpc.messages.topic.v1.EnsureTopicRequest;
import com.oracle.coherence.grpc.messages.topic.v1.GetRemainingMessagesRequest;

import com.oracle.coherence.grpc.messages.topic.v1.NamedTopicEvent;
import com.oracle.coherence.grpc.messages.topic.v1.TopicEventType;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequest;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequestType;

import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceResponse;
import com.tangosol.internal.net.topic.NamedTopicConnector;
import com.tangosol.internal.net.topic.NamedTopicSubscriber;
import com.tangosol.internal.net.topic.NamedTopicView;
import com.tangosol.internal.net.topic.PublisherConnector;
import com.tangosol.internal.net.topic.SubscriberConnector;

import com.tangosol.net.TopicService;

import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

import io.grpc.Channel;
import io.grpc.stub.StreamObserver;

import static com.tangosol.internal.net.grpc.RemoteGrpcServiceDependencies.NO_EVENTS_HEARTBEAT;

/**
 * A {@link ClientProtocol} implementation for the topic API.
 *
 * @author Jonathan Knight  2025.01.25
 */
public class GrpcNamedTopicConnector<V>
        extends BaseClientChannel<GrpcNamedTopicConnector.Dependencies, TopicServiceGrpcConnection>
        implements ClientProtocol, NamedTopicConnector<V>
    {
    /**
     * Creates an {@link GrpcNamedTopicConnector} from the specified
     * {@link Dependencies}.
     *
     * @param dependencies  the {@link Dependencies} to configure this
     *                      {@link GrpcNamedTopicConnector}
     * @param service       the owning {@link GrpcRemoteTopicService}
     * @param connection    the {@link GrpcConnection}
     */
    public GrpcNamedTopicConnector(Dependencies dependencies, GrpcRemoteTopicService service, TopicServiceGrpcConnection connection)
        {
        super(dependencies, connection);
        f_topicService = service;
        f_sName        = dependencies.getName();

        EnsureTopicRequest ensureRequest = EnsureTopicRequest.newBuilder()
                .setTopic(f_sName)
                .build();

        TopicServiceRequest request = TopicServiceRequest.newBuilder()
                .setType(TopicServiceRequestType.EnsureTopic)
                .setMessage(Any.pack(ensureRequest))
                .build();

        TopicServiceResponse response = connection.send(request);
        f_nTopicId = response.getProxyId();

        StreamObserver<TopicServiceResponse> eventObserver = new SimpleStreamObserver<>(this::onEvent);
        f_listener = new GrpcConnection.Listener<>(eventObserver, r -> r.getProxyId() == f_nTopicId);
        connection.addResponseObserver(f_listener);
        }

    // ----- NamedTopicView.Connector methods -------------------------------

    @Override
    public boolean isDestroyed()
        {
        return m_fDestroyed;
        }

    @Override
    public boolean isReleased()
        {
        return m_fReleased;
        }

    @Override
    public int getRemainingMessages(String sSubscriberGroup, int[] anChannel)
        {
        GetRemainingMessagesRequest.Builder builder = GetRemainingMessagesRequest.newBuilder()
                .setSubscriberGroup(sSubscriberGroup);

        if (anChannel != null)
            {
            for (int nChannel : anChannel)
                {
                builder.addChannels(nChannel);
                }
            }

        return f_connection.poll(f_nTopicId, TopicServiceRequestType.GetRemainingMessages, builder.build())
                .thenApply(f_connection::unpackInteger)
                .join()
                .getValue();
        }

    @Override
    public TopicService getTopicService()
        {
        return f_topicService;
        }

    @Override
    public String getName()
        {
        return f_sName;
        }

    @Override
    public void destroy()
        {
        release(true);
        }

    @Override
    public void release()
        {
        release(false);
        }

    @Override
    public void ensureSubscriberGroup(String sSubscriberGroup, Filter<?> filter, ValueExtractor<?, ?> extractor)
        {
        EnsureSubscriberGroupRequest.Builder builder = EnsureSubscriberGroupRequest.newBuilder()
                .setSubscriberGroup(sSubscriberGroup);

        if (filter != null)
            {
            builder.setFilter(toByteString(filter));
            }

        if (extractor != null)
            {
            builder.setExtractor(toByteString(filter));
            }

        f_connection.poll(f_nTopicId, TopicServiceRequestType.EnsureSubscriberGroup, builder.build()).join();
        }

    @Override
    public void destroySubscriberGroup(String sSubscriberGroup)
        {
        StringValue value = StringValue.newBuilder().setValue(sSubscriberGroup).build();
        f_connection.poll(f_nTopicId, TopicServiceRequestType.DestroySubscriberGroup, value).join();
        }

    @Override
    public PublisherConnector<V> createPublisher(Publisher.Option<? super V>[] options)
        {
        return createPublisherConnector(options);
        }

    @Override
    public <U> NamedTopicSubscriber<U> createSubscriber(Subscriber.Option<? super V, U>[] options)
        {
        SubscriberConnector<U> connector = createSubscriberConnector(options);
        return new NamedTopicSubscriber<>(m_topicView, connector, options);
        }

    @Override
    public void setConnectedNamedTopic(NamedTopicView<V> namedTopicView)
        {
        m_topicView = namedTopicView;
        }

    @Override
    public PublisherConnector<V> createPublisherConnector(Publisher.Option<? super V>[] options)
        {
        return f_topicService.ensurePublisher(f_sName, options);
        }

    @Override
    public <U> SubscriberConnector<U> createSubscriberConnector(Subscriber.Option<? super V, U>[] options)
        {
        return f_topicService.ensureSubscriber(this, options);
        }

    // ----- helper methods -------------------------------------------------

    protected void onEvent(TopicServiceResponse response)
        {
        NamedTopicView<?> view  = m_topicView;
        if (view != null)
            {
            NamedTopicEvent   event = MessageHelper.unpack(response.getMessage(), NamedTopicEvent.class);
            if (event != null && event.getType() == TopicEventType.TopicDestroyed)
                {
                view.dispatchEvent(com.tangosol.net.topic.NamedTopicEvent.Type.Destroyed);
                }
            }
        }

    protected void release(boolean fDestroy)
        {
        if (fDestroy)
            {
            m_fDestroyed = true;
            f_connection.send(0, TopicServiceRequestType.DestroyTopic, StringValue.of(f_sName));
            }
        else
            {
            m_fReleased = true;
            }
        f_connection.close();
        }

    // ----- Dependencies ---------------------------------------------------

    /**
     * The dependencies used to create an {@link GrpcNamedTopicConnector}.
     */
    public interface Dependencies
            extends BaseGrpcClient.Dependencies
        {
        /**
         * Returns the frequency in millis that heartbeats should be sent by the
         * proxy to the client bidirectional channel.
         *
         * @return the frequency in millis that heartbeats should be sent by the
         *         proxy to the client bidirectional channel
         */
        long getHeartbeatMillis();

        /**
         * Return the flag to determine whether heart beat messages should require an
         * ack response from the server.
         *
         * @return  that is {@code true} if heart beat messages should require an
         *          ack response from the server
         */
        boolean isRequireHeartbeatAck();
        }

    // ----- DefaultDependencies ----------------------------------------

    /**
     * The dependencies used to create an {@link GrpcNamedTopicConnector}.
     */
    public static class DefaultDependencies
            extends BaseGrpcClient.DefaultDependencies
            implements Dependencies
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link DefaultDependencies}.
         *
         * @param sName       the name of the underlying topic
         * @param channel     the gRPC {@link Channel} to use
         * @param dispatcher  the lifecycle event dispatcher
         */
        public DefaultDependencies(String sName, Channel channel, GrpcTopicLifecycleEventDispatcher dispatcher)
            {
            super(sName, channel, dispatcher);
            }

        // ----- Dependencies methods ---------------------------------------

        @Override
        public long getHeartbeatMillis()
            {
            return m_nEventsHeartbeat;
            }

        @Override
        public boolean isRequireHeartbeatAck()
            {
            return m_fRequireHeartbeatAck;
            }

        // ----- setters ----------------------------------------------------

        /**
         * Set the frequency in millis that heartbeats should be sent by the
         * proxy to the client bidirectional channel.
         * <p/>
         * If the frequency is set to zero or less, then no heartbeats will be sent.
         *
         * @param nEventsHeartbeat the heartbeat frequency in millis
         */
        public void setHeartbeatMillis(long nEventsHeartbeat)
            {
            m_nEventsHeartbeat = Math.max(NO_EVENTS_HEARTBEAT, nEventsHeartbeat);
            }

        /**
         * Set the flag to indicate whether heart beat messages require an
         * ack response from the server.
         *
         * @param fRequireHeartbeatAck  {@code true} to require an ack response
         */
        public void setRequireHeartbeatAck(boolean fRequireHeartbeatAck)
            {
            m_fRequireHeartbeatAck = fRequireHeartbeatAck;
            }

        // ----- data members -----------------------------------------------

        /**
         * The frequency in millis that heartbeats should be sent by the
         * proxy to the client bidirectional channel
         */
        private long m_nEventsHeartbeat = NO_EVENTS_HEARTBEAT;

        /**
         * {@code true} if heartbeat messages should be acknowledged.
         */
        private boolean m_fRequireHeartbeatAck;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of this topic.
     */
    protected final String f_sName;

    /**
     * The owning {@link GrpcRemoteTopicService}.
     */
    protected final GrpcRemoteTopicService f_topicService;

    /**
     * The topic identifier for this protocol.
     */
    private final int f_nTopicId;

    /**
     * The {@link NamedTopicView} this connector connects to.
     */
    protected NamedTopicView<V> m_topicView;

    /**
     * A flag indicating whether this {@link GrpcNamedTopicConnector} is destroyed.
     */
    private boolean m_fDestroyed;

    /**
     * A flag indicating whether this {@link GrpcNamedTopicConnector} is destroyed.
     */
    private boolean m_fReleased;

    /**
     * The listener for topic events.
     */
    private final GrpcConnection.Listener<TopicServiceResponse> f_listener;
    }
