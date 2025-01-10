
/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.RemoteNamedTopic

package com.tangosol.coherence.component.net.extend;

import com.tangosol.coherence.Component;

import com.tangosol.coherence.component.net.Extend;

import com.tangosol.coherence.component.net.extend.messageFactory.NamedTopicFactory;

import com.tangosol.coherence.component.net.extend.protocol.NamedTopicProtocol;

import com.tangosol.coherence.component.net.extend.remoteService.RemoteTopicService;

import com.tangosol.coherence.component.util.daemon.QueueProcessor;

import com.tangosol.internal.net.topic.NamedTopicPublisher;
import com.tangosol.internal.net.topic.NamedTopicSubscriber;

import com.tangosol.internal.net.topic.PublisherConnector;
import com.tangosol.internal.net.topic.SubscriberConnector;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;

import com.tangosol.net.events.internal.TopicDispatcher;

import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.Connection;
import com.tangosol.net.messaging.ConnectionException;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.NamedTopicEvent;
import com.tangosol.net.topic.NamedTopicListener;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Filter;
import com.tangosol.util.Listeners;
import com.tangosol.util.ValueExtractor;

import java.util.Set;

import java.util.stream.Collectors;

/**
 * An Extend {@link Channel.Receiver} for a remote topic.
 *
 * @author Jonathan Knight  2024.11.26
 */
@SuppressWarnings("rawtypes")
public class RemoteNamedTopic<V>
        extends Extend
        implements Channel.Receiver, NamedTopic<V>, SubscriberConnector.Factory<V>, PublisherConnector.Factory<V>
    {
    /**
     * Property Channel
     */
    private Channel __m_Channel;

    /**
     * Property DeactivationListeners
     */
    private Listeners __m_DeactivationListeners;

    /**
     * Property EventDispatcher
     */
    private QueueProcessor __m_EventDispatcher;

    /**
     * Property Listeners
     */
    private Listeners __m_Listeners;

    /**
     * Property Released
     * <p>
     * A flag indicating whether this topic has been released.
     */
    private boolean __m_Released;

    /**
     * Property TopicName
     */
    private String __m_TopicName;

    /**
     * Property TopicService
     */
    private RemoteTopicService __m_TopicService;

    /**
     * The topic lifecycle dispatcher.
     */
    private TopicDispatcher __m_TopicLifecycleEventDispatcher;

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public RemoteNamedTopic()
        {
        this(null, null, true);
        }

    /**
     * Initializing constructor.
     */
    public RemoteNamedTopic(String sName, Component compParent, boolean fInit)
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
            setListeners(new com.tangosol.util.Listeners());
            }
        catch (Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }

        // signal the end of the initialization
        set_Constructed(true);
        }

    @Override
    protected void __initPrivate()
        {
        super.__initPrivate();
        }

    @Override
    public void addListener(NamedTopicListener listener)
        {
        getListeners().add(listener);
        }

    @Override
    public RemotePublisher<V> createPublisherConnector(Publisher.Option<? super V>[] options)
        {
        RemoteTopicService service = (RemoteTopicService) getService();
        return service.createPublisherConnector(getTopicName(), options);
        }

    @Override
    @SuppressWarnings("unchecked")
    public Publisher<V> createPublisher(Publisher.Option<? super V>[] options)
        {
        RemotePublisher connector = createPublisherConnector(options);
        return new NamedTopicPublisher<>(this, connector, options);
        }

    @Override
    public <U> RemoteSubscriber<U> createSubscriberConnector(Subscriber.Option<? super V, U>[] options)
        {
        RemoteTopicService service = (RemoteTopicService) getService();
        return service.createSubscriberConnector(getTopicName(), options);
        }

    @Override
    public final <U> Subscriber<U> createSubscriber(Subscriber.Option<? super V, U>[] options)
        {
        RemoteSubscriber<U> connector = createSubscriberConnector(options);
        return new NamedTopicSubscriber<>(this, connector, options);
        }

    @Override
    public void destroy()
        {
        getTopicService().destroyTopic(this);
        }

    @Override
    public void destroySubscriberGroup(String sGroupName)
        {
        Channel channel = ensureChannel();
        NamedTopicFactory.DestroySubscriberGroupRequest request = (NamedTopicFactory.DestroySubscriberGroupRequest)
                channel.getMessageFactory().createMessage(NamedTopicFactory.TYPE_ID_DESTROY_SUBSCRIBER_GROUP);

        request.setSubscriberGroup(sGroupName);
        channel.request(request);
        }

    @Override
    public void ensureSubscriberGroup(String sGroupName, Filter filter, ValueExtractor extractor)
        {
        Channel channel = ensureChannel();
        NamedTopicFactory.EnsureSubscriberGroupRequest request = (NamedTopicFactory.EnsureSubscriberGroupRequest)
                channel.getMessageFactory().createMessage(NamedTopicFactory.TYPE_ID_ENSURE_SUBSCRIBER_GROUP);

        request.setSubscriberGroup(sGroupName);
        request.setFilter(filter);
        request.setExtractor(extractor);
        channel.request(request);
        }

    /**
     * Getter for property Channel.<p>
     */
    public Channel getChannel()
        {
        return __m_Channel;
        }

    /**
     * Return the Channel used by this remote topic. If the Channel is null
     * or is not open, this method throws an exception.
     *
     * @return a Channel that can be used to exchange Messages with the
     * remote ProxyService
     */
    protected Channel ensureChannel()
        {
        Channel channel = getChannel();
        if (channel == null || !channel.isOpen())
            {
            String     sCause     = "released";
            Connection connection = null;

            if (channel != null)
                {
                connection = channel.getConnection();
                if (connection == null || !connection.isOpen())
                    {
                    sCause = "closed";
                    }
                }

            throw new ConnectionException("NamedTopic \""
                    + getTopicName() + "\" has been " + sCause,
                    connection);
            }

        return channel;
        }

    @Override
    public int getChannelCount()
        {
        return getTopicService().getChannelCount(getTopicName());
        }

    /**
     * Getter for property DeactivationListeners.<p>
     */
    public Listeners getDeactivationListeners()
        {
        return __m_DeactivationListeners;
        }

    /**
     * Return a human-readable description of this component.
     *
     * @return a String representation of this component
     */
    @Override
    protected String getDescription()
        {
        return "NamedTopic=" + getTopicName()
                + ", Service=" + getTopicService().getInfo().getServiceName();
        }

    /**
     * Getter for property EventDispatcher.<p>
     */
    public QueueProcessor getEventDispatcher()
        {
        return __m_EventDispatcher;
        }

    /**
     * Getter for property Listeners.<p>
     */
    public Listeners getListeners()
        {
        return __m_Listeners;
        }

    @Override
    public String getName()
        {
        return getTopicName();
        }

    @Override
    public Protocol getProtocol()
        {
        return NamedTopicProtocol.getInstance();
        }

    @Override
    public int getRemainingMessages(String sSubscriberGroup, int[] anChannel)
        {
        Channel channel = ensureChannel();
        NamedTopicFactory.GetRemainingMessagesRequest request = (NamedTopicFactory.GetRemainingMessagesRequest)
                channel.getMessageFactory().createMessage(NamedTopicFactory.TYPE_ID_REMAINING_MESSAGES);

        request.setSubscriberGroup(sSubscriberGroup);
        request.setChannels(anChannel);

        return (Integer) channel.request(request);
        }

    @Override
    public com.tangosol.net.Service getService()
        {
        return getTopicService();
        }

    @Override
    public Set<String> getSubscriberGroups()
        {
        return getTopicService().getSubscriberGroups(getTopicName())
                .stream()
                .filter(SubscriberGroupId::isDurable)
                .map(SubscriberGroupId::getGroupName)
                .collect(Collectors.toSet());
        }

    /**
     * Getter for property TopicName.<p>
     */
    public String getTopicName()
        {
        return __m_TopicName;
        }

    /**
     * Getter for property TopicService.<p>
     */
    @Override
    public RemoteTopicService getTopicService()
        {
        return __m_TopicService;
        }

    @Override
    public boolean isActive()
        {
        Channel channel = getChannel();
        return channel != null && channel.isOpen();
        }

    /**
     * Getter for property Destroyed.<p>
     * A flag indicating whether this topic has been destroyed.
     */
    @Override
    public boolean isDestroyed()
        {
        Channel    channel    = getChannel();
        Connection connection = channel == null ? null : channel.getConnection();

        if (channel == null || connection == null)
            {
            // unknown if destroyed or not.
            return false;
            }
        else
            {
            // infer destroyed when channel is closed and connection is open.
            return !channel.isOpen() && connection.isOpen();
            }
        }

    @Override
    public void onChannelClosed(Channel channel)
        {
        Listeners listeners = getListeners();
        if (!listeners.isEmpty())
            {
            NamedTopicEvent evt = new NamedTopicEvent(this, NamedTopicEvent.Type.Destroyed);
            evt.dispatch(listeners);
            }
        }

    @Override
    public void onMessage(com.tangosol.net.messaging.Message message)
        {
        message.run();
        }

    @Override
    public void registerChannel(Channel channel)
        {
        setChannel(channel);
        }

    @Override
    public boolean isReleased()
        {
        return __m_Released;
        }

    @Override
    public void release()
        {
        setReleased(true);
        getTopicService().releaseTopic(this);
        }

    @Override
    public void removeListener(NamedTopicListener listener)
        {
        getListeners().add(listener);
        }

    @Override
    public void unregisterChannel(Channel channel)
        {
        setChannel(null);
        }

    /**
     * Setter for property Channel.<p>
     */
    public void setChannel(Channel channel)
        {
        __m_Channel = channel;
        }

    /**
     * Setter for property DeactivationListeners.<p>
     */
    public void setDeactivationListeners(Listeners listenersDeactivation)
        {
        __m_DeactivationListeners = listenersDeactivation;
        }

    /**
     * Setter for property EventDispatcher.<p>
     */
    public void setEventDispatcher(QueueProcessor processorDispatcher)
        {
        __m_EventDispatcher = processorDispatcher;
        }

    /**
     * Setter for property Listeners.<p>
     */
    public void setListeners(Listeners listeners)
        {
        __m_Listeners = listeners;
        }

    /**
     * Setter for property Released.<p>
     * A flag indicating whether this topic has been released.
     */
    public void setReleased(boolean fReleased)
        {
        __m_Released = fReleased;
        }

    /**
     * Setter for property TopicName.<p>
     */
    public void setTopicName(String sName)
        {
        __m_TopicName = sName;
        }

    /**
     * Setter for property TopicService.<p>
     */
    public void setTopicService(RemoteTopicService serviceTopic)
        {
        __m_TopicService = serviceTopic;
        }

    public TopicDispatcher getTopicLifecycleEventDispatcher()
        {
        return __m_TopicLifecycleEventDispatcher;
        }

    public void setTopicLifecycleEventDispatcher(TopicDispatcher dispatcher)
        {
        __m_TopicLifecycleEventDispatcher = dispatcher;
        }
    }
