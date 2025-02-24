
/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.RemoteNamedTopic

package com.tangosol.coherence.component.net.extend;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.coherence.component.net.Extend;
import com.tangosol.coherence.component.net.extend.protocol.NamedTopicProtocol;
import com.tangosol.internal.net.topic.SubscriberConnector;
import com.tangosol.internal.net.topic.SubscriberConnector.SubscriberEvent;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.net.TopicService;
import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.Connection;
import com.tangosol.net.messaging.ConnectionException;

import java.util.function.Consumer;

/**
 * A remote topic Extend channel receiver.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class RemoteSubscriberChannel<V>
        extends Extend
        implements Channel.Receiver
    {
    /**
     * Property Channel
     */
    private Channel m_channel;

    /**
     * The name of the topic.
     */
    private String m_sTopicName;

    /**
     * The subscriber identifier.
     */
    private SubscriberId m_subscriberId;

    /**
     * Property TopicService
     */
    private TopicService m_topicService;

    /**
     * The subscriber connected to this connector.
     */
    private RemoteSubscriber<V> m_subscriber;

    // ----- constructors ---------------------------------------------------

    public <T> RemoteSubscriberChannel()
        {
        super(null, null, true);
        }

    @Override
    public void __init()
        {
        // private initialization
        __initPrivate();
        // state initialization: public and protected properties
        // signal the end of the initialization
        set_Constructed(true);
        }

    @Override
    protected void __initPrivate()
        {
        super.__initPrivate();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Send a request.
     *
     * @param id   the type identifier of the message to send
     * @param <R>  the expected result type
     *
     * @return the result of sending the request
     */
    <R> R send(int id)
        {
        return send(id, com.tangosol.net.messaging.Request.class, request -> {});
        }

    /**
     * Send a request.
     *
     * @param id          the type identifier of the message to send
     * @param clz         the type of the request
     * @param configurer  a {@link Consumer} that may configure the request
     * @param <R>         the expected result type
     * @param <M>         the type of the request
     *
     * @return the result of sending the request
     */
    @SuppressWarnings("unchecked")
    <R, M extends com.tangosol.net.messaging.Request> R send(int id, Class<M> clz, Consumer<M> configurer)
        {
        Channel                 channel = ensureChannel();
        Protocol.MessageFactory factory = channel.getMessageFactory();
        M                       request = (M) factory.createMessage(id);
        configurer.accept(request);
        return (R) channel.request(request);
        }

    /**
     * Return a human-readable description of this component.
     *
     * @return a String representation of this component
     */
    @Override
    protected String getDescription()
        {
        return "Subscriber=" + m_subscriberId
                + ", Subscriber=" + m_sTopicName
                + ", Service=" + m_topicService.getInfo().getServiceName();
        }

    @Override
    public void unregisterChannel(Channel channel)
        {
        if (m_channel != null)
            {
            Logger.finer("Disconnected remote subscriber topic=" + m_sTopicName + " id=" + m_subscriber.getSubscriberId());
            m_subscriber.onDisconnected();
            m_channel = null;
            }
        }

    @Override
    public String getName()
        {
        return "Subscriber:" + m_sTopicName;
        }

    @Override
    public Protocol getProtocol()
        {
        return NamedTopicProtocol.getInstance();
        }

    @Override
    public void onChannelClosed(Channel channel)
        {
        Logger.finer("Disconnected remote subscriber due to channel closure topic=" + m_sTopicName + " id=" + m_subscriber.getSubscriberId());
        m_subscriber.onDisconnected();
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

    /**
     * Getter for property Channel.<p>
     */
    public Channel getChannel()
        {
        return m_channel;
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
                    + m_sTopicName + "\" has been " + sCause,
                    connection);
            }

        return channel;
        }

    /**
     * Set the Extend {@link Channel}.
     */
    public void setChannel(Channel channel)
        {
        m_channel = channel;
        }

    public void setSubscriberId(SubscriberId subscriberId)
        {
        m_subscriberId = subscriberId;
        }

    public void setTopicService(TopicService serviceTopic)
        {
        m_topicService = serviceTopic;
        }

    public TopicService getTopicService()
        {
        return m_topicService;
        }

    public void setTopicName(String sTopicName)
        {
        m_sTopicName = sTopicName;
        }

    public RemoteSubscriber<V> getSubscriber()
        {
        return m_subscriber;
        }

    public void setSubscriber(RemoteSubscriber<V> subscriber)
        {
        m_subscriber = subscriber;
        }
    }
