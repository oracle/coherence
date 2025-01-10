/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: Component.net.extend.remoteService.RemoteTopicService

package com.tangosol.coherence.component.net.extend.remoteService;

import com.tangosol.coherence.Component;
import com.tangosol.coherence.component.net.extend.Protocol;
import com.tangosol.coherence.component.net.extend.RemoteNamedTopic;
import com.tangosol.coherence.component.net.extend.RemotePublisher;
import com.tangosol.coherence.component.net.extend.RemoteService;
import com.tangosol.coherence.component.net.extend.RemoteSubscriber;
import com.tangosol.coherence.component.net.extend.RemoteSubscriberChannel;
import com.tangosol.coherence.component.net.extend.messageFactory.TopicServiceFactory;
import com.tangosol.coherence.component.net.extend.messageFactory.TopicServiceFactory.EnsurePublisherRequest;
import com.tangosol.coherence.component.net.extend.messageFactory.TopicServiceFactory.EnsureSubscriberRequest;
import com.tangosol.coherence.component.net.extend.protocol.NamedTopicProtocol;
import com.tangosol.coherence.component.net.extend.protocol.TopicServiceProtocol;
import com.tangosol.internal.net.service.extend.remote.DefaultRemoteServiceDependencies;
import com.tangosol.internal.net.service.extend.remote.DefaultRemoteTopicServiceDependencies;
import com.tangosol.internal.net.service.extend.remote.LegacyXmlRemoteTopicServiceHelper;
import com.tangosol.internal.net.service.extend.remote.RemoteServiceDependencies;
import com.tangosol.internal.net.service.extend.remote.RemoteTopicServiceDependencies;
import com.tangosol.internal.net.topic.NamedTopicSubscriber;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.TopicService;
import com.tangosol.net.events.EventDispatcherRegistry;
import com.tangosol.net.events.internal.TopicDispatcher;
import com.tangosol.net.internal.ScopedTopicReferenceStore;
import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.Connection;
import com.tangosol.net.messaging.ConnectionEvent;
import com.tangosol.net.messaging.ConnectionInitiator;
import com.tangosol.net.security.SecurityHelper;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.TopicBackingMapManager;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Listeners;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;
import com.tangosol.util.WrapperException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * Service implementation that allows a JVM to use a remote clustered Service
 * without having to join the Cluster.
 *
 * @see com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService
 */
@SuppressWarnings({"rawtypes", "unchecked", "PatternVariableCanBeUsed", "deprecation"})
public class RemoteTopicService
        extends RemoteService
        implements TopicService
    {
    /**
     * Property ScopedTopicStore
     */
    private ScopedTopicReferenceStore __m_ScopedTopicStore;

    private TopicBackingMapManager m_topicBackingMapManager;

    public RemoteTopicService()
        {
        this(null, null, true);
        }

    public RemoteTopicService(String sName, Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        if (fInit)
            {
            __init();
            }
        }

    @Override
    public void __init()
        {
        // private initialization
        __initPrivate();

        // state initialization: public and protected properties
        try
            {
            setMemberListeners(new Listeners());
            setResourceRegistry(new SimpleResourceRegistry());
            setScopedTopicStore(new ScopedTopicReferenceStore());
            setServiceListeners(new Listeners());
            setServiceVersion("1");
            }
        catch (Exception e)
            {
            // re-throw as a runtime exception
            throw new WrapperException(e);
            }

        // signal the end of the initialization
        set_Constructed(true);
        }

    @Override
    protected void __initPrivate()
        {
        super.__initPrivate();
        }

    //++ getter for static property _Instance

    /**
     * Getter for property _Instance.<p>
     * Auto generated
     */
    public static Component get_Instance()
        {
        return new RemoteTopicService();
        }

    /**
     * Getter for property _CLASS.<p>
     * Property with auto-generated accessor that returns the Class object for a
     * given component.
     */
    public static Class get_CLASS()
        {
        return RemoteTopicService.class;
        }

    /**
     * Create a new Default dependencies object by cloning the input
     * dependencies.  Each class or component that uses dependencies implements
     * a Default dependencies class which provides the clone functionality.
     * The dependency injection design pattern requires every component in the
     * component hierarchy to implement clone.
     *
     * @return DefaultRemoteServiceDependencies  the cloned dependencies
     */
    @Override
    protected DefaultRemoteServiceDependencies cloneDependencies(RemoteServiceDependencies deps)
        {
        return new DefaultRemoteTopicServiceDependencies((RemoteTopicServiceDependencies) deps);
        }

    @Override
    public void connectionClosed(ConnectionEvent evt)
        {
        releaseTopics();
        super.connectionClosed(evt);
        }

    @Override
    public void connectionError(ConnectionEvent evt)
        {
        releaseTopics();
        super.connectionError(evt);
        }


    @Override
    public void destroyTopic(NamedTopic topic)
        {
        if (!(topic instanceof RemoteNamedTopic))
            {
            throw new IllegalArgumentException("illegal topic: " + topic);
            }
        RemoteNamedTopic remoteTopic = (RemoteNamedTopic) topic;
        getScopedTopicStore().releaseTopic(remoteTopic);
        destroyRemoteNamedTopic(remoteTopic);
        }

    // Declared at the super level

    /**
     * The configure() implementation method. This method must only be called by
     * a thread that has synchronized on this RemoteService.
     *
     * @param xml the XmlElement containing the new configuration for this
     *            RemoteService
     */
    @Override
    protected void doConfigure(XmlElement xml)
        {
        setDependencies(LegacyXmlRemoteTopicServiceHelper.fromXml(xml,
                new DefaultRemoteTopicServiceDependencies(), getOperationalContext(),
                getContextClassLoader()));
        }

    // Declared at the super level

    /**
     * The shutdown() implementation method. This method must only be called by
     * a thread that has synchronized on this RemoteService.
     */
    @Override
    protected void doShutdown()
        {
        super.doShutdown();

        getScopedTopicStore().clear();
        }

    @Override
    public int ensureChannelCount(String sName, int cChannel)
        {
        return ensureChannelCount(sName, cChannel, cChannel);
        }

    @Override
    public int ensureChannelCount(String sTopic, int cRequired, int cChannel)
        {
        Channel                                       channel = ensureChannel();
        Protocol.MessageFactory                       factory = channel.getMessageFactory();
        TopicServiceFactory.EnsureChannelCountRequest request = (TopicServiceFactory.EnsureChannelCountRequest) factory.createMessage(TopicServiceFactory.TYPE_ID_ENSURE_CHANNEL_COUNT);

        request.setTopicName(sTopic);
        request.setRequiredChannels(cRequired);
        request.setChannelCount(cChannel);

        return (Integer) channel.request(request);
        }

    @Override
    public NamedTopic ensureTopic(String sName, ClassLoader loader)
        {
        if (sName == null || sName.isEmpty())
            {
            sName = "Default";
            }

        if (loader == null)
            {
            loader = getContextClassLoader();
            _assert(loader != null, "ContextClassLoader is missing");
            }

        ScopedTopicReferenceStore store = getScopedTopicStore();
        RemoteNamedTopic          topic = (RemoteNamedTopic) store.getTopic(sName, loader);

        if (topic == null || !topic.isActive())
            {
            // this is one of the few places that acquiring a distinct lock per topic
            // is beneficial due to the potential cost of createRemoteNamedTopic
            long cWait = getDependencies().getRequestTimeoutMillis();
            if (cWait <= 0)
                {
                cWait = -1;
                }
            if (!store.lock(sName, cWait))
                {
                throw new RequestTimeoutException("Failed to get a reference to topic '" +
                        sName + "' after " + cWait + "ms");
                }
            try
                {
                topic = (RemoteNamedTopic) store.getTopic(sName, loader);
                if (topic == null || !topic.isActive())
                    {
                    topic = createRemoteNamedTopic(sName, loader);
                    store.putTopic(topic, loader);
                    }
                }
            finally
                {
                store.unlock(sName);
                }
            }
        return topic;
        }

    @Override
    public int getChannelCount(String sTopic)
        {
        Channel                                 channel = ensureChannel();
        Protocol.MessageFactory                 factory = channel.getMessageFactory();
        TopicServiceFactory.ChannelCountRequest request = (TopicServiceFactory.ChannelCountRequest) factory.createMessage(TopicServiceFactory.TYPE_ID_CHANNEL_COUNT);

        request.setTopicName(sTopic);

        return (Integer) channel.request(request);
        }

    /**
     * Getter for property ScopedTopicStore.<p>
     */
    public ScopedTopicReferenceStore getScopedTopicStore()
        {
        return __m_ScopedTopicStore;
        }

    @Override
    public String getServiceType()
        {
        return TopicService.TYPE_REMOTE;
        }

    @Override
    public Set<SubscriberGroupId> getSubscriberGroups(String sTopicName)
        {
        Channel                                       channel = ensureChannel();
        Protocol.MessageFactory                        factory = channel.getMessageFactory();
        TopicServiceFactory.GetSubscriberGroupsRequest request = (TopicServiceFactory.GetSubscriberGroupsRequest) factory.createMessage(TopicServiceFactory.TYPE_ID_GET_SUBSCRIBER_GROUPS);

        request.setTopicName(sTopicName);
        return new HashSet((Collection) channel.request(request));
        }

    @Override
    public TopicBackingMapManager getTopicBackingMapManager()
        {
        return m_topicBackingMapManager;
        }

    @Override
    public Set getTopicNames()
        {
        return Collections.emptySet();
        }

    @Override
    protected void onDependencies(RemoteServiceDependencies deps)
        {
        super.onDependencies(deps);
        // register all Protocols
        ConnectionInitiator initiator = getInitiator();
        initiator.registerProtocol(TopicServiceProtocol.getInstance());
        initiator.registerProtocol(NamedTopicProtocol.getInstance());
        }

    /**
     * Open a Channel to the remote ProxyService.
     */
    @Override
    protected Channel openChannel()
        {
        lookupProxyServiceAddress();

        Connection connection = getInitiator().ensureConnection();
        return connection.openChannel(TopicServiceProtocol.getInstance(),
                "TopicServiceProxy",
                null,
                null,
                SecurityHelper.getCurrentSubject());
        }

    @Override
    public void releaseTopic(NamedTopic topic)
        {
        if (!(topic instanceof RemoteNamedTopic))
            {
            throw new IllegalArgumentException("illegal topic: " + topic);
            }
        RemoteNamedTopic remoteTopic = (RemoteNamedTopic) topic;
        getScopedTopicStore().releaseTopic(remoteTopic);
        releaseRemoteNamedTopic(remoteTopic);
        }

    @Override
    public void setTopicBackingMapManager(TopicBackingMapManager topicBackingMapManager)
        {
        m_topicBackingMapManager = topicBackingMapManager;
        }

    protected <V> RemoteNamedTopic<V> createRemoteNamedTopic(String sName, ClassLoader loader)
        {
        Channel                                channel    = ensureChannel();
        Connection                             connection = channel.getConnection();
        Protocol.MessageFactory                factory    = channel.getMessageFactory();
        RemoteNamedTopic<V>                    topic      = new RemoteNamedTopic<>();
        Subject                                subject    = SecurityHelper.getCurrentSubject();
        TopicServiceFactory.EnsureTopicRequest request    = (TopicServiceFactory.EnsureTopicRequest) factory.createMessage(TopicServiceFactory.TYPE_ID_ENSURE_TOPIC);

        request.setTopicName(sName);

        URI uri;
        try
            {
            uri = new URI((String) channel.request(request));
            }
        catch (URISyntaxException e)
            {
            throw ensureRuntimeException(e, "error instantiating URI");
            }

        topic.setTopicName(sName);
        topic.setTopicService(this);
        topic.setEventDispatcher(ensureEventDispatcher());

        TopicDispatcher dispatcher = new TopicDispatcher(sName, this);
        topic.setTopicLifecycleEventDispatcher(dispatcher);

        ResourceRegistry        resourceRegistry   = getTopicBackingMapManager().getCacheFactory().getResourceRegistry();
        EventDispatcherRegistry dispatcherRegistry = resourceRegistry.getResource(EventDispatcherRegistry.class);
        dispatcherRegistry.registerEventDispatcher(dispatcher);

        connection.acceptChannel(uri, loader, topic, subject);

        dispatcher.dispatchTopicCreated(topic);
        return topic;
        }

    protected void destroyRemoteNamedTopic(RemoteNamedTopic topic)
        {
        releaseRemoteNamedTopic(topic);

        Channel                                 channel = ensureChannel();
        Protocol.MessageFactory                 factory = channel.getMessageFactory();
        TopicServiceFactory.DestroyTopicRequest request = (TopicServiceFactory.DestroyTopicRequest) factory.createMessage(TopicServiceFactory.TYPE_ID_DESTROY_TOPIC);

        request.setTopicName(topic.getTopicName());
        channel.request(request);

        TopicDispatcher dispatcher = topic.getTopicLifecycleEventDispatcher();
        dispatcher.dispatchTopicDestroyed(topic);

        ResourceRegistry        resourceRegistry   = getTopicBackingMapManager().getCacheFactory().getResourceRegistry();
        EventDispatcherRegistry dispatcherRegistry = resourceRegistry.getResource(EventDispatcherRegistry.class);
        dispatcherRegistry.unregisterEventDispatcher(dispatcher);
        }

    protected void releaseRemoteNamedTopic(RemoteNamedTopic topic)
        {
        try
            {
            // when this is called due to certain connection error, e.g. ping
            // timeout, the channel could be null and closed.
            Channel channel = topic.getChannel();
            if (channel != null)
                {
                channel.close();
                }
            }
        catch (RuntimeException e)
            {
            // ignored
            }
        }

    protected void releaseTopics()
        {
        ScopedTopicReferenceStore store = getScopedTopicStore();
        for (Object o : store.getAllTopics())
            {
            RemoteNamedTopic topic = (RemoteNamedTopic) o;
            releaseRemoteNamedTopic(topic);
            }

        store.clear();
        }

    public <V> RemotePublisher<V> createPublisherConnector(String sTopicName, Publisher.Option[] options)
        {
        Channel                 channel    = ensureChannel();
        Connection              connection = channel.getConnection();
        Protocol.MessageFactory factory    = channel.getMessageFactory();
        Subject                 subject    = SecurityHelper.getCurrentSubject();
        Publisher.OptionSet<V>  optionSet  = Publisher.optionsFrom(options);
        EnsurePublisherRequest  request    = (EnsurePublisherRequest) factory.createMessage(TopicServiceFactory.TYPE_ID_ENSURE_PUBLISHER);

        request.setTopicName(sTopicName);
        request.setChannelCount(optionSet.getChannelCount(0));

        Object[] aoResponse = (Object[]) channel.request(request);

        _assert(aoResponse != null);
        //noinspection DataFlowIssue
        _assert(aoResponse.length >= 3);

        URI uri;
        try
            {
            uri = new URI(String.valueOf(aoResponse[EnsurePublisherRequest.RESPONSE_ID_URI]));
            }
        catch (URISyntaxException e)
            {
            throw ensureRuntimeException(e, "error instantiating URI");
            }

        long nId;
        try
            {
            nId = ((Number) aoResponse[EnsurePublisherRequest.RESPONSE_ID_PUBLISHER_ID]).longValue();
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, "error parsing publisher id");
            }

        long nMaxBatch;
        try
            {
            nMaxBatch = ((Number) aoResponse[2]).longValue();
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, "error parsing max batch size");
            }

        int cChannel;
        try
            {
            cChannel = ((Number) aoResponse[EnsurePublisherRequest.RESPONSE_ID_CHANNEL_COUNT]).intValue();
            }
        catch (Exception e)
            {
            cChannel = getChannelCount(sTopicName);
            }

        RemotePublisher<V> connector = new RemotePublisher<>(nId, cChannel, options);
        connector.setTopicName(sTopicName);
        connector.setTopicService(this);
        connector.setMaxBatchSizeBytes(nMaxBatch);

        connection.acceptChannel(uri, getContextClassLoader(), connector, subject);
        return connector;
        }

    public <U> RemoteSubscriber<U> createSubscriberConnector(String sTopicName, Subscriber.Option[] options)
        {
        Channel                 channel    = ensureChannel();
        Connection              connection = channel.getConnection();
        Protocol.MessageFactory factory    = channel.getMessageFactory();
        Subject                 subject = SecurityHelper.getCurrentSubject();
        EnsureSubscriberRequest request = (EnsureSubscriberRequest) factory.createMessage(TopicServiceFactory.TYPE_ID_ENSURE_SUBSCRIBER);

        NamedTopicSubscriber.OptionSet<?, ?> optionSet = NamedTopicSubscriber.optionsFrom(options);

        optionSet.getSubscriberGroupName().ifPresent(request::setSubscriberGroup);
        optionSet.getFilter().ifPresent(request::setFilter);
        optionSet.getExtractor().ifPresent(request::setExtractor);

        request.setTopicName(sTopicName);
        request.setCompleteOnEmpty(optionSet.isCompleteOnEmpty());

        Object[] aoResponse = (Object[]) channel.request(request);

        _assert(aoResponse != null);
        //noinspection DataFlowIssue
        _assert(aoResponse.length >= 3);

        URI uri;
        try
            {
            uri = new URI(String.valueOf(aoResponse[0]));
            }
        catch (URISyntaxException e)
            {
            throw ensureRuntimeException(e, "error instantiating URI");
            }

        SubscriberId subscriberId;
        try
            {
            subscriberId = (SubscriberId) aoResponse[1];
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, "error parsing subscriber id");
            }

        SubscriberGroupId groupId;
        try
            {
            groupId = (SubscriberGroupId) aoResponse[2];
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, "error parsing subscriber group id");
            }

        RemoteSubscriberChannel<U> subscriberChannel = new RemoteSubscriberChannel<>();
        subscriberChannel.setSubscriberId(subscriberId);
        subscriberChannel.setTopicName(sTopicName);
        subscriberChannel.setTopicService(this);
        subscriberChannel.setChannel(channel);

        connection.acceptChannel(uri, getContextClassLoader(), subscriberChannel, subject);

        return new RemoteSubscriber<>(sTopicName, subscriberChannel, subscriberId, groupId);
        }

    /**
     * Setter for property ScopedTopicStore.<p>
     */
    public void setScopedTopicStore(ScopedTopicReferenceStore storeTopic)
        {
        __m_ScopedTopicStore = storeTopic;
        }
    }
