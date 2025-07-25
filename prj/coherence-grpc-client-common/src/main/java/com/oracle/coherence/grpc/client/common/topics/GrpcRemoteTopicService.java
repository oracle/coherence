/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common.topics;

import com.google.protobuf.Any;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.base.Timeout;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.NamedTopicProtocol;

import com.oracle.coherence.grpc.TopicHelper;
import com.oracle.coherence.grpc.client.common.GrpcConnection;
import com.oracle.coherence.grpc.client.common.GrpcRemoteService;

import com.oracle.coherence.grpc.client.common.v1.GrpcConnectionV1;
import com.oracle.coherence.grpc.messages.common.v1.CollectionOfStringValues;

import com.oracle.coherence.grpc.messages.topic.v1.EnsureChannelCountRequest;

import com.oracle.coherence.grpc.messages.topic.v1.EnsurePublisherRequest;
import com.oracle.coherence.grpc.messages.topic.v1.EnsurePublisherResponse;
import com.oracle.coherence.grpc.messages.topic.v1.EnsureSimpleSubscriberRequest;
import com.oracle.coherence.grpc.messages.topic.v1.EnsureSubscriberRequest;
import com.oracle.coherence.grpc.messages.topic.v1.EnsureSubscriberResponse;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequest;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceRequestType;
import com.oracle.coherence.grpc.messages.topic.v1.TopicServiceResponse;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;

import com.tangosol.internal.net.grpc.RemoteGrpcTopicServiceDependencies;

import com.tangosol.internal.net.topic.NamedTopicConnector;
import com.tangosol.internal.net.topic.NamedTopicSubscriber;
import com.tangosol.internal.net.topic.NamedTopicView;

import com.tangosol.internal.net.topic.PublisherConnector;
import com.tangosol.internal.net.topic.SubscriberConnector;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.RequestIncompleteException;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.TopicService;

import com.tangosol.net.events.EventDispatcherRegistry;

import com.tangosol.net.internal.ScopedTopicReferenceStore;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.NamedTopicEvent;
import com.tangosol.net.topic.NamedTopicListener;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.TopicBackingMapManager;

import com.tangosol.util.ResourceRegistry;

import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.Set;

import java.util.concurrent.TimeoutException;
import java.util.function.IntPredicate;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A client {@link TopicService} that uses gRPC to send requests to the proxy.
 *
 * @author Jonathan Knight  2025.01.25
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class GrpcRemoteTopicService
        extends GrpcRemoteService<RemoteGrpcTopicServiceDependencies>
        implements TopicService
    {
    /**
     * Create a {@link GrpcRemoteTopicService}.
     */
    public GrpcRemoteTopicService()
        {
        super(TopicService.TYPE_REMOTE_GRPC);
        }

    @Override
    protected Class<? extends Message> getResponseType()
        {
        return TopicServiceResponse.class;
        }

    @Override
    public TopicBackingMapManager getTopicBackingMapManager()
        {
        return m_backingMapManager;
        }

    @Override
    public void setTopicBackingMapManager(TopicBackingMapManager manager)
        {
        m_backingMapManager = manager;
        }

    @Override
    protected EventDispatcherRegistry getDefaultEventDispatcherRegistry()
        {
        ResourceRegistry registry = m_backingMapManager.getCacheFactory().getResourceRegistry();
        return registry.getResource(EventDispatcherRegistry.class);
        }

    @Override
    public void start()
        {
        super.start();
        // create a connection for service messages
        GrpcConnection grpcConnection = connect(NamedTopicProtocol.PROTOCOL_NAME, NamedTopicProtocol.VERSION,
                NamedTopicProtocol.SUPPORTED_VERSION, GrpcConnectionV1.SERVICE_VERSION);
        m_connection = new TopicServiceGrpcConnection(grpcConnection);
        }

    @Override
    protected void stopInternal()
        {
        for (NamedTopic<?> topic : f_store.getAll())
            {
            topic.removeListener(f_listener);
            try
                {
                topic.release();
                }
            catch (Throwable e)
                {
                Logger.err(e);
                }
            }
        }

    @Override
    public <T> NamedTopic<T> ensureTopic(String sName, ClassLoader ignored)
        {
        ClassLoader loader = getContextClassLoader();
        if (loader == null)
            {
            throw new IllegalStateException("ContextClassLoader is missing");
            }

        NamedTopic<T> topic = f_store.get(sName, loader);
        if (topic == null || !topic.isActive())
            {
            // this is one of the few places that acquiring a distinct lock per topic
            // is beneficial due to the potential cost of createRemoteNamedTopic
            long cWait = getDependencies().getRequestTimeoutMillis();
            if (cWait <= 0)
                {
                cWait = -1;
                }
            if (!f_store.lock(sName, cWait))
                {
                throw new RequestTimeoutException("Failed to get a reference to topic '" +
                    sName + "' after " + cWait + "ms");
                }
            try
                {
                topic = (NamedTopic<T>) f_store.get(sName, loader);
                if (topic == null || !topic.isActive())
                    {
                    topic = ensureNamedTopicClient(sName);
                    f_store.put(topic, loader);
                    }
                }
            finally
                {
                f_store.unlock(sName);
                }
            }

        return topic;
        }

    @Override
    public void releaseTopic(NamedTopic<?> topic)
        {
        GrpcNamedTopicConnector<?> channel = assertTopicType(topic);
        releaseTopic(topic, channel, false);
        }

    @Override
    public void destroyTopic(NamedTopic<?> topic)
        {
        GrpcNamedTopicConnector<?> channel = assertTopicType(topic);
        releaseTopic(topic, channel, true);
        }

    @Override
    public int getChannelCount(String sTopicName)
        {
        StringValue topicName = StringValue.of(sTopicName);
        Int32Value  value     = m_connection.poll(0, TopicServiceRequestType.GetChannelCount, topicName)
                .thenApply(m_connection::unpackInteger)
                .join();

        return value.getValue();
        }

    @Override
    public int ensureChannelCount(String sTopicName, int cRequired, int cChannel)
        {
        EnsureChannelCountRequest request = EnsureChannelCountRequest.newBuilder()
                .setTopic(sTopicName)
                .setRequiredCount(cRequired)
                .setChannelCount(cChannel)
                .build();

        Int32Value value = m_connection.poll(0, TopicServiceRequestType.EnsureChannelCount, request)
                .thenApply(m_connection::unpackInteger)
                .join();

        return value.getValue();
        }

    @Override
    public Set<SubscriberGroupId> getSubscriberGroups(String sTopicName)
        {
        StringValue              topicName = StringValue.of(sTopicName);
        CollectionOfStringValues values    = m_connection.poll(0, TopicServiceRequestType.GetSubscriberGroups, topicName)
                .thenApply(resp -> m_connection.unpackMessage(resp, CollectionOfStringValues.class))
                .join();

        return values.getValuesList().stream()
                .map(SubscriberGroupId::withName)
                .collect(Collectors.toSet());
        }

    @Override
    public Set<String> getTopicNames()
        {
        return f_store.getNames();
        }

    @Override
    public boolean isVersionCompatible(int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
        {
        int nEncoded = ServiceMemberSet.encodeVersion(nMajor, nMinor, nMicro, nPatchSet, nPatch);
        return CacheFactory.VERSION_ENCODED >= nEncoded;
        }

    @Override
    public boolean isVersionCompatible(int nYear, int nMonth, int nPatch)
        {
        int nEncoded = ServiceMemberSet.encodeVersion(nYear, nMonth, nPatch);
        return CacheFactory.VERSION_ENCODED >= nEncoded;
        }

    @Override
    public boolean isVersionCompatible(int nVersion)
        {
        return CacheFactory.VERSION_ENCODED >= nVersion;
        }

    @Override
    public boolean isVersionCompatible(IntPredicate predicate)
        {
        return predicate.test(CacheFactory.VERSION_ENCODED);
        }

    @Override
    public int getMinimumServiceVersion()
        {
        return CacheFactory.VERSION_ENCODED;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Ensure the requested topic
     *
     * @param sName  the name of the topic
     * @param <T>    the type of value in the topic
     *
     * @return the topic
     */
    protected <T> NamedTopic<T> ensureNamedTopicClient(String sName)
        {
        GrpcTopicLifecycleEventDispatcher dispatcher = new GrpcTopicLifecycleEventDispatcher(sName, this);

        Channel channel = m_tracingInterceptor == null
                ? m_channel
                : ClientInterceptors.intercept(m_channel, m_tracingInterceptor);

        GrpcNamedTopicConnector.Dependencies dependencies = createTopicDependencies(sName, channel, dispatcher);

        GrpcConnection grpcConnection = connect(NamedTopicProtocol.PROTOCOL_NAME, NamedTopicProtocol.VERSION,
                NamedTopicProtocol.SUPPORTED_VERSION, GrpcConnectionV1.SERVICE_VERSION);

        TopicServiceGrpcConnection connection = new TopicServiceGrpcConnection(grpcConnection);
        GrpcNamedTopicConnector<T> connector  = new GrpcNamedTopicConnector(dependencies, this, connection);
        NamedTopicView<T>          topic      = new NamedTopicView<>(connector);

        EventDispatcherRegistry dispatcherReg = getEventDispatcherRegistry();

        if (dispatcherReg != null)
            {
            dispatcherReg.registerEventDispatcher(dispatcher);
            }

        topic.addListener(f_listener);

        // We must dispatch the created event async
        m_executor.execute(() -> dispatcher.dispatchTopicCreated(topic));
        return topic;
        }

    /**
     * Create the dependencies for a topic.
     *
     * @param sName       the name of the topic
     * @param channel     the connection to the proxy
     * @param dispatcher  the lifecycle event dispatcher
     *
     * @return the topic dependencies
     */
    private GrpcNamedTopicConnector.Dependencies createTopicDependencies(String sName, Channel channel,
                                                                         GrpcTopicLifecycleEventDispatcher dispatcher)
        {
        RemoteGrpcTopicServiceDependencies dependencies = getDependencies();
        String                             sScopeName   = dependencies.getRemoteScopeName();

        if (sScopeName == null)
            {
            sScopeName = Coherence.DEFAULT_SCOPE;
            }

        GrpcNamedTopicConnector.DefaultDependencies deps
                = new  GrpcNamedTopicConnector.DefaultDependencies(sName, channel, dispatcher);

        deps.setScope(sScopeName);
        deps.setSerializer(m_serializer, m_serializer.getName());
        deps.setExecutor(m_executor);
        deps.setDeferKeyAssociationCheck(dependencies.isDeferKeyAssociationCheck());
        deps.setDeadline(dependencies.getDeadline());
        deps.setHeartbeatMillis(dependencies.getHeartbeatInterval());
        deps.setRequireHeartbeatAck(dependencies.isRequireHeartbeatAck());
        return deps;
        }

    /**
     * Release or destroy a topic managed by this service.
     *
     * @param topic     the topic to release or destroy
     * @param channel   the topic's connection to the proxy
     * @param fDestroy  {@code true} if the topic is being destroyed
     */
    protected void releaseTopic(NamedTopic<?> topic, GrpcNamedTopicConnector<?> channel, boolean fDestroy)
        {
        if (fDestroy)
            {
            channel.destroy();
            }
        else
            {
            channel.release();
            }
        channel.close();
        f_store.release(topic);
        }

    /**
     * Dispatch a destroyed lifecycle event for a topic.
     *
     * @param topic  the topic to dispatch the event for
     */
    protected void dispatchDestroyedLifecycleEvent(NamedTopic<?> topic)
        {
        GrpcNamedTopicConnector<?> channel = unwrapChannel(topic);
        if (channel != null)
            {
            GrpcTopicLifecycleEventDispatcher dispatcher    = (GrpcTopicLifecycleEventDispatcher) channel.getDependencies().getEventDispatcher();
            EventDispatcherRegistry           dispatcherReg = getEventDispatcherRegistry();
            if (dispatcherReg != null)
                {
                dispatcherReg.unregisterEventDispatcher(dispatcher);
                }
            m_executor.execute(() -> dispatcher.dispatchTopicDestroyed(topic));
            }

        }

    /**
     * Assert that a topic is a gRPC topic and return its {@link GrpcNamedTopicConnector}.
     *
     * @param topic  the {@link NamedTopic}
     *
     * @return the {@link GrpcNamedTopicConnector} for the topic
     */
    private GrpcNamedTopicConnector<?> assertTopicType(NamedTopic<?> topic)
        {
        if (!(topic instanceof NamedTopicView<?>))
            {
            throw new IllegalArgumentException("illegal topic: " + topic);
            }
        NamedTopicConnector<?> connector = ((NamedTopicView<?>) topic).getConnector();
        if (!((connector instanceof GrpcNamedTopicConnector<?>)))
            {
            throw new IllegalArgumentException("illegal topic: " + topic);
            }
        return (GrpcNamedTopicConnector<?>) connector;
        }

    /**
     * Unwrap the {@link GrpcNamedTopicConnector} from a {@link NamedTopic}.
     *
     * @param topic  the topic to obtain the {@link GrpcNamedTopicConnector} from
     *
     * @return the {@link GrpcNamedTopicConnector} for the topic or {@code null}
     *         if the topic has no channel
     */
    private GrpcNamedTopicConnector<?> unwrapChannel(NamedTopic<?> topic)
        {
        if (!(topic instanceof NamedTopicView<?>))
            {
            return null;
            }
        NamedTopicConnector<?> connector = ((NamedTopicView<?>) topic).getConnector();
        if (!((connector instanceof GrpcNamedTopicConnector<?>)))
            {
            return null;
            }
        return (GrpcNamedTopicConnector<?>) connector;
        }

    /**
     * Create a {@link TopicServiceGrpcConnection} that can be used to send
     * and receive requests using the {@link NamedTopicProtocol}.
     *
     * @return a new {@link TopicServiceGrpcConnection}
     */
    public TopicServiceGrpcConnection createPublisherConnection()
        {
        GrpcConnection grpcConnection = connect(NamedTopicProtocol.PROTOCOL_NAME,
                NamedTopicProtocol.VERSION,
                NamedTopicProtocol.SUPPORTED_VERSION,
                GrpcConnectionV1.SERVICE_VERSION);

        return new TopicServiceGrpcConnection(grpcConnection);
        }

    /**
     * Create a {@link PublisherConnector}.
     *
     * @param sTopicName  the name of the topic the publisher will publish to
     * @param options     the options used to create the publisher
     * @param <V>         the type of values to be published
     *
     * @return a {@link PublisherConnector}
     */
    public <V> PublisherConnector<V> ensurePublisher(String sTopicName, Publisher.Option<? super V>[] options)
        {
        long nMillis = getDependencies().getRequestTimeoutMillis();
        try (Timeout ignored = Timeout.after(nMillis))
            {
            while (true)
                {
                try
                    {
                    Publisher.OptionSet<V>     optionSet  = Publisher.optionsFrom(options);
                    TopicServiceGrpcConnection connection = createPublisherConnection();

                    EnsurePublisherRequest ensureRequest = EnsurePublisherRequest.newBuilder()
                            .setTopic(sTopicName)
                            .setChannelCount(optionSet.getChannelCount(0))
                            .build();

                    TopicServiceRequest request = TopicServiceRequest.newBuilder()
                            .setType(TopicServiceRequestType.EnsurePublisher)
                            .setMessage(Any.pack(ensureRequest))
                            .build();

                    TopicServiceResponse      response       = connection.send(request);
                    EnsurePublisherResponse   ensureResponse = connection.unpackMessage(response, EnsurePublisherResponse.class);
                    int                       nProxyId       = ensureResponse.getProxyId();
                    long                      nPublisherId   = ensureResponse.getPublisherId();
                    int                       cChannel       = ensureResponse.getChannelCount();
                    GrpcPublisherConnector<V> connector      = new GrpcPublisherConnector<>(connection, nProxyId, nPublisherId, cChannel, options);

                    connector.setTopicName(sTopicName);
                    connector.setMaxBatchSizeBytes(ensureResponse.getMaxBatchSize());
                    connector.setTopicService(this);
                    return connector;
                    }
                catch (Throwable t)
                    {
                    Throwable rootCause = Exceptions.getRootCause(t);
                    if (rootCause instanceof StatusRuntimeException
                            && ((StatusRuntimeException) rootCause).getStatus().getCode() == Status.Code.UNAVAILABLE)
                        {
                        Logger.finer("Caught Status.UNAVAILABLE exception ensuring publisher, will retry");
                        continue;
                        }
                    else if (rootCause instanceof TimeoutException)
                        {
                        Logger.finer("Caught TimeoutException ensuring publisher, will retry");
                        continue;
                        }
                    throw Exceptions.ensureRuntimeException(t);
                    }
                }
            }
        catch (InterruptedException e)
            {
            throw new RequestIncompleteException("Timed out after " + nMillis + " ms attempting to create a publisher", e);
            }
        }

    /**
     * Create a {@link TopicServiceGrpcConnection} that can be used to send
     * and receive requests using the {@link NamedTopicProtocol}.
     *
     * @return a new {@link TopicServiceGrpcConnection}
     */
    public TopicServiceGrpcConnection createSubscriberConnection()
        {
        GrpcConnection grpcConnection = connect(NamedTopicProtocol.PROTOCOL_NAME,
                NamedTopicProtocol.VERSION,
                NamedTopicProtocol.SUPPORTED_VERSION,
                GrpcConnectionV1.SERVICE_VERSION);

        return new TopicServiceGrpcConnection(grpcConnection);
        }

    public <V, U> SubscriberConnector<U> ensureSubscriber(GrpcNamedTopicConnector<?> connector, Subscriber.Option<? super V, U>[] options)
        {
        NamedTopicSubscriber.OptionSet<?, ?> optionSet  = NamedTopicSubscriber.optionsFrom(options);
        boolean                              fSimple    = optionSet.isSimple();
        String                               sTopicName = connector.getName();
        TopicServiceGrpcConnection           connection = createSubscriberConnection();

        TopicServiceResponse response = fSimple
                ? createSimpleSubscriberRequest(sTopicName, optionSet, connection)
                : createSubscriberRequest(sTopicName, optionSet, connection);

        EnsureSubscriberResponse ensureResponse = connection.unpackMessage(response, EnsureSubscriberResponse.class);
        SubscriberId             subscriberId   = TopicHelper.fromProtobufSubscriberId(ensureResponse);
        SubscriberGroupId        groupId        = TopicHelper.fromProtobufSubscriberGroupId(ensureResponse);
        int                      proxyId        = ensureResponse.getProxyId();

        return new GrpcSubscriberConnector<>(connector, proxyId, connection, sTopicName, subscriberId, groupId, fSimple);
        }

    private TopicServiceResponse createSubscriberRequest(String sTopicName,
            NamedTopicSubscriber.OptionSet<?, ?> optionSet, TopicServiceGrpcConnection connection)
        {
        EnsureSubscriberRequest.Builder builder   = EnsureSubscriberRequest.newBuilder();

        optionSet.getSubscriberGroupName().ifPresent(builder::setSubscriberGroup);
        optionSet.getFilter().ifPresent(f -> builder.setFilter(BinaryHelper.toByteString(f, m_serializer)));
        optionSet.getExtractor().ifPresent(e -> builder.setExtractor(BinaryHelper.toByteString(e, m_serializer)));

        int[] anChannel = optionSet.getSubscribeTo();
        if (anChannel != null && anChannel.length > 0)
            {
            builder.addAllChannels(IntStream.of(anChannel).boxed().collect(Collectors.toList()));
            }

        EnsureSubscriberRequest request = builder.setTopic(sTopicName).build();
        return connection.send(0, TopicServiceRequestType.EnsureSubscriber, request);
        }

    private TopicServiceResponse createSimpleSubscriberRequest(String sTopicName,
            NamedTopicSubscriber.OptionSet<?, ?> optionSet, TopicServiceGrpcConnection connection)
        {
        EnsureSimpleSubscriberRequest.Builder builder = EnsureSimpleSubscriberRequest.newBuilder();

        optionSet.getSubscriberGroupName().ifPresent(builder::setSubscriberGroup);
        optionSet.getFilter().ifPresent(f -> builder.setFilter(BinaryHelper.toByteString(f, m_serializer)));
        optionSet.getExtractor().ifPresent(e -> builder.setExtractor(BinaryHelper.toByteString(e, m_serializer)));

        int[] anChannel = optionSet.getSubscribeTo();
        if (anChannel != null && anChannel.length > 0)
            {
            builder.addAllChannels(IntStream.of(anChannel).boxed().collect(Collectors.toList()));
            }

        EnsureSimpleSubscriberRequest request = builder.setTopic(sTopicName).build();
        return connection.send(0, TopicServiceRequestType.EnsureSimpleSubscriber, request);
        }

    // ----- immer class Listener -------------------------------------------

    /**
     * A {@link NamedTopicListener} to detect topics being destroyed.
     */
    protected class Listener
            implements NamedTopicListener
        {
        @Override
        public void onEvent(NamedTopicEvent evt)
            {
            if (evt.getType() == NamedTopicEvent.Type.Destroyed)
                {
                NamedTopic<?> topic = evt.getSource();
                if (f_store.releaseTopic(topic))
                    {
                    dispatchDestroyedLifecycleEvent(topic);
                    }
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The gRPC connection to the proxy.
     */
    private TopicServiceGrpcConnection m_connection;

    /**
     * The {@link TopicBackingMapManager}.
     */
    private TopicBackingMapManager<?, ?> m_backingMapManager;

    /**
     * The store of topics this service manages.
     */
    private final ScopedTopicReferenceStore f_store = new ScopedTopicReferenceStore();

    /**
     * The listener to detect topics being destroyed.
     */
    private final Listener f_listener = new Listener();
    }
