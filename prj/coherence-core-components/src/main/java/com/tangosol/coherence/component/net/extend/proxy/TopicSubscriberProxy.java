
/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.proxy.NamedTopicProxy

package com.tangosol.coherence.component.net.extend.proxy;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.coherence.component.net.extend.message.request.NamedTopicRequest;
import com.tangosol.coherence.component.net.extend.message.request.TopicSubscriberRequest;

import com.tangosol.coherence.component.net.extend.messageFactory.NamedTopicFactory;

import com.tangosol.coherence.component.net.extend.protocol.NamedTopicProtocol;

import com.tangosol.internal.net.topic.SubscriberConnector;
import com.tangosol.internal.net.topic.SubscriberConnector.SubscriberEvent;

import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.TopicService;

import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.net.messaging.Protocol;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.NamedTopicEvent;
import com.tangosol.net.topic.NamedTopicListener;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Binary;

/**
 * A server side proxy for a topic {@link Subscriber}.
 *
 * @author Jonathan Knight  2024.11.26
 */
@SuppressWarnings({"rawtypes", "PatternVariableCanBeUsed"})
public class TopicSubscriberProxy
        extends com.tangosol.coherence.component.net.extend.Proxy
        implements MemberListener, Channel.Receiver, NamedTopicListener, SubscriberConnector.SubscriberListener
    {
    /**
     * Property Channel
     */
    private com.tangosol.net.messaging.Channel m_channel;

    /**
     * The named topic.
     */
    private transient NamedTopic m_topic;

    /**
     * The proxied subscriber;
     */
    private SubscriberConnector.ConnectedSubscriber<Binary> m_subscriber;

    /**
     * Property TransferThreshold
     */
    private long m_transferThreshold;

    /**
     * A unique identifier for this proxy.
     */
    private int m_nProxyId;

    /**
     * A flag indicating that this subscriber proxy is using a simple subscriber API.
     */
    private boolean m_fSimple;

    // ----- constructors ---------------------------------------------------

    public TopicSubscriberProxy()
        {
        super(null, null, false);
        __init();
        }

    @Override
    public void __init()
        {
        // private initialization
        __initPrivate();

        // state initialization: public and protected properties
        try
            {
            setEnabled(true);
            setTransferThreshold(524288L);
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

    // ----- proxy methods --------------------------------------------------

    protected void closeChannel()
        {
        com.tangosol.net.messaging.Channel channel = getChannel();
        if (channel != null)
            {
            channel.close();
            }
        }

    /**
     * Getter for property Channel.<p>
     */
    public com.tangosol.net.messaging.Channel getChannel()
        {
        return m_channel;
        }

    /**
     * Return a human-readable description of this component.
     *
     * @return a String representation of this component
     */
    @Override
    protected String getDescription()
        {
        String sTopicName   = null;
        String sServiceName = null;

        try
            {
            sTopicName   = m_topic.getName();
            sServiceName = m_topic.getTopicService().getInfo().getServiceName();
            }
        catch (Throwable t)
            {
            // ignored
            }
        return "NamedTopic=" + (sTopicName == null ? "N/A" : sTopicName)
                + ", Subscriber=" + m_subscriber.getSubscriberId()
                + ", Service=" + (sServiceName == null ? "N/A" : sServiceName);
        }

    @Override
    public String getName()
        {
        return m_topic.getName() + "#" + m_subscriber.getSubscriberId();
        }

    @Override
    public Protocol getProtocol()
        {
        return NamedTopicProtocol.getInstance();
        }

    /**
     * Getter for property TransferThreshold.<p>
     */
    public long getTransferThreshold()
        {
        return m_transferThreshold;
        }

    @Override
    public void memberJoined(MemberEvent evt)
        {
        }

    @Override
    public void memberLeaving(MemberEvent evt)
        {
        }

    @Override
    public void memberLeft(MemberEvent evt)
        {
        com.tangosol.net.messaging.Channel channel = getChannel();
        if (channel != null)
            {
            // we only add a member listener if the service is a PagedTopicService
            PagedTopicService service = (PagedTopicService) evt.getService();
            // avoid iterating the member set (getOwnershipSenior()) if partition 0 has an assignment
            if (service.getPartitionOwner(0) == null && service.getOwnershipSenior() == null)
                {
                onEvent(new SubscriberEvent(m_subscriber.getConnector(), SubscriberEvent.Type.ChannelAllocation));
                }
            }
        }

    protected void onDeactivation()
        {
        NamedTopic topic = m_topic;
        if (topic != null)
            {
            TopicService service = m_topic.getTopicService();
            if (service.isRunning())
                {
                closeChannel();
                }
            }
        }

    @Override
    public void onEvent(NamedTopicEvent event)
        {
        if (event.getType() == NamedTopicEvent.Type.Destroyed)
            {
            onDeactivation();
            }
        }

    @Override
    public void onMessage(com.tangosol.net.messaging.Message message)
        {
        if (message instanceof NamedTopicRequest)
            {
            NamedTopicRequest request = (NamedTopicRequest) message;
            request.setNamedTopic(m_topic);
            request.setTransferThreshold(getTransferThreshold());
            }
        if (message instanceof TopicSubscriberRequest)
            {
            TopicSubscriberRequest request = (TopicSubscriberRequest) message;
            request.setSubscriber(m_subscriber);
            }
        message.run();
        }

    @Override
    public void registerChannel(com.tangosol.net.messaging.Channel channel)
        {
        _assert(getChannel() == null);

        NamedTopic topic = m_topic;
        _assert(topic != null);

        setChannel(channel);

        //noinspection DataFlowIssue
        topic.addListener(this);

        TopicService service = topic.getTopicService();
        if (service instanceof PagedTopicService &&
                !((PagedTopicService) service).isLocalStorageEnabled())
            {
            service.addMemberListener(this);
            }
        }

    @Override
    public void onEvent(SubscriberEvent evt)
        {
        Channel channel = getChannel();
        if (channel != null)
            {
            NamedTopicFactory.SubscriberChannelEvent message = (NamedTopicFactory.SubscriberChannelEvent)
                    channel.getMessageFactory().createMessage(NamedTopicFactory.TYPE_ID_SUBSCRIBER_EVENT);

            message.setType(evt.getType().ordinal());
            message.setPopulatedChannels(evt.getPopulatedChannels());
            message.setAllocatedChannels(evt.getAllocatedChannels());

            try
                {
                channel.send(message);
                }
            catch (ConnectionException e)
                {
                // the Channel is most likely closing or has been closed
                }
            catch (Throwable t)
                {
                _trace(t, "Error sending SubscriberEvent to " + channel);
                }
            }
        }

    /**
     * Setter for property Channel.<p>
     */
    public void setChannel(com.tangosol.net.messaging.Channel channel)
        {
        m_channel = channel;
        }

    /**
     * Return this proxy's identifier.
     *
     * @return this proxy's identifier
     */
    public int getProxyId()
        {
        return m_nProxyId;
        }

    /**
     * Set this proxy's identifier.
     *
     * @param nProxyId  this proxy's identifier
     */
    public void setProxyId(int nProxyId)
        {
        m_nProxyId = nProxyId;
        }

    /**
     * Set the proxied subscriber.
     *
     * @param subscriber the proxied subscriber
     */
    public void setSubscriber(SubscriberConnector.ConnectedSubscriber<Binary> subscriber)
        {
        if (m_subscriber != null)
            {
            m_subscriber.getConnector().removeListener(this);
            }
        m_subscriber = subscriber;
        if (m_subscriber != null)
            {
            m_subscriber.getConnector().addListener(this);
            }
        }

    /**
     * Return the proxied subscriber.
     *
     * @return the proxied subscriber
     */
    public SubscriberConnector.ConnectedSubscriber<Binary> getSubscriber()
        {
        return m_subscriber;
        }

    /**
     * Set the {@link NamedTopic} this proxy is subscribing to.
     *
     * @param topic  the {@link NamedTopic} this proxy is subscribing to
     */
    public void setNamedTopic(NamedTopic topic)
        {
        m_topic = topic;
        }

    /**
     * Setter for property TransferThreshold.<p>
     */
    public void setTransferThreshold(long lThreshold)
        {
        m_transferThreshold = lThreshold;
        }

    /**
     * Return {@code true} if this proxy is using the simple subscriber API.
     *
     * @return {@code true} if this proxy is using the simple subscriber API
     */
    public boolean isSimple()
        {
        return m_fSimple;
        }

    /**
     * Set the flag to indicate that this proxy is using the simple subscriber API.
     *
     * @param fSimple  {@code true} if this proxy is using the simple subscriber API
     */
    public void setSimple(boolean fSimple)
        {
        m_fSimple = fSimple;
        }

    @Override
    public void unregisterChannel(com.tangosol.net.messaging.Channel channel)
        {
        if (getChannel() != channel)
            {
            System.err.println();
            }
        _assert(getChannel() == channel);
        NamedTopic topic = m_topic;
        if (topic != null)
            {
            topic.removeListener(this);
            m_topic = null;
            }
        SubscriberConnector.ConnectedSubscriber<Binary> subscriber = m_subscriber;
        if (subscriber != null)
            {
            Logger.fine("Remote channel closed, closing remote subscriber " + subscriber.getSubscriberId());
            subscriber.close();
            m_subscriber = null;
            }
        setChannel(null);
        }
    }
