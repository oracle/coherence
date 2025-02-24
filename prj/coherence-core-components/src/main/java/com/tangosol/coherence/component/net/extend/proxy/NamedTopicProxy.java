
/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.proxy.NamedTopicProxy

package com.tangosol.coherence.component.net.extend.proxy;

import com.tangosol.coherence.component.net.extend.message.request.NamedTopicRequest;

import com.tangosol.coherence.component.net.extend.message.request.TopicServiceRequest;
import com.tangosol.coherence.component.net.extend.messageFactory.NamedTopicFactory;
import com.tangosol.coherence.component.net.extend.protocol.NamedTopicProtocol;

import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.TopicService;

import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.net.messaging.Message;
import com.tangosol.net.messaging.Protocol;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.NamedTopicEvent;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.SynchronousListener;

/**
 * A server side Extend proxy for a {@link NamedTopic}.
 *
 * @author Jonathan Knight  2024.11.26
 */
@SuppressWarnings({"rawtypes", "unchecked", "PatternVariableCanBeUsed"})
public class NamedTopicProxy
        extends com.tangosol.coherence.component.net.extend.Proxy
        implements MemberListener,
        Channel.Receiver, NamedTopic, com.tangosol.net.topic.NamedTopicListener, SynchronousListener
    {
    /**
     * Property Channel
     */
    private Channel m_channel;

    /**
     * Property NamedTopic
     */
    private NamedTopic m_topic;

    /**
     * Property TransferThreshold
     */
    private long m_nTransferThreshold;

    /**
     * A unique identifier for this proxy.
     */
    private int m_nProxyId;

    public NamedTopicProxy()
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

    @Override
    public void destroySubscriberGroup(String Param_1)
        {
        getNamedTopic().destroySubscriberGroup(Param_1);
        }

    @Override
    public void ensureSubscriberGroup(String sName)
        {
        getNamedTopic().ensureSubscriberGroup(sName);
        }

    @Override
    @SuppressWarnings("unchecked")
    public void ensureSubscriberGroup(String Param_1, com.tangosol.util.Filter Param_2, com.tangosol.util.ValueExtractor Param_3)
        {
        getNamedTopic().ensureSubscriberGroup(Param_1, Param_2, Param_3);
        }

    @Override
    public int getChannelCount()
        {
        return getNamedTopic().getChannelCount();
        }

    @Override
    public int getRemainingMessages(String sSubscriberGroup)
        {
        return getNamedTopic().getRemainingMessages(sSubscriberGroup);
        }

    @Override
    public int getRemainingMessages(String Param_1, int[] Param_2)
        {
        return getNamedTopic().getRemainingMessages(Param_1, Param_2);
        }

    @Override
    public java.util.Set<String> getSubscriberGroups()
        {
        return getNamedTopic().getSubscriberGroups();
        }

    @Override
    public TopicService getTopicService()
        {
        return getNamedTopic().getTopicService();
        }

    @Override
    public void addListener(com.tangosol.net.topic.NamedTopicListener listener)
        {
        getNamedTopic().addListener(listener);
        }

    @Override
    public void close()
        {
        // must close the NamedTopic via the TopicServiceProxy
        throw new UnsupportedOperationException();
        }

    protected void closeChannel()
        {
        Channel channel = getChannel();
        if (channel != null)
            {
            channel.close();
            }
        }

    @Override
    public Publisher createPublisher(Publisher.Option[] options)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public Subscriber createSubscriber(Subscriber.Option[] options)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public void destroy()
        {
        // must destroy the NamedTopic via the TopicServiceProxy
        throw new UnsupportedOperationException();
        }

    /**
     * Getter for property Channel.<p>
     */
    public Channel getChannel()
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
            sTopicName   = getName();
            sServiceName = getTopicService().getInfo().getServiceName();
            }
        catch (Throwable t)
            {
            // ignored
            }
        return "NamedTopic=" + (sTopicName == null ? "N/A" : sTopicName)
                + ", Service=" + (sServiceName == null ? "N/A" : sServiceName);
        }

    @Override
    public String getName()
        {
        return m_topic.getName();
        }

    /**
     * Getter for property NamedTopic.<p>
     */
    public NamedTopic getNamedTopic()
        {
        return m_topic;
        }

    @Override
    public Protocol getProtocol()
        {
        return NamedTopicProtocol.getInstance();
        }

    @Override
    public com.tangosol.net.Service getService()
        {
        return null;
        }

    /**
     * Getter for property TransferThreshold.<p>
     */
    public long getTransferThreshold()
        {
        return m_nTransferThreshold;
        }

    @Override
    public boolean isActive()
        {
        return false;
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
        Channel channel = getChannel();

        if (channel != null)
            {
            // we only add a member listener if the service is a PagedTopicService
            PagedTopicService service = (PagedTopicService) evt.getService();
            // avoid iterating the member set (getOwnershipSenior()) if partition 0 has an assignment
            if (service.getPartitionOwner(0) == null && service.getOwnershipSenior() == null)
                {
                Protocol.MessageFactory factory = channel.getMessageFactory();
// ToDo this should send a message to indicate all subscriber channels lost
//                    com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.NoStorageMembers message = (com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.NoStorageMembers) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory.NoStorageMembers.TYPE_ID);
//                    channel.send(message);
                }
            }
        }

    protected void onDeactivation()
        {
        if (getTopicService().isRunning())
            {
            closeChannel();
            }
        }

    @Override
    public void onEvent(com.tangosol.net.topic.NamedTopicEvent event)
        {
        Channel channel = getChannel();
        if (channel != null)
            {
            Message[] aMessages = instantiateEventMessages(event);
            try
                {
                for (Message message : aMessages)
                    {
                    channel.send(message);
                    }
                }
            catch (ConnectionException e)
                {
                // the Channel is most likely closing or has been closed
                }
            catch (Throwable t)
                {
                _trace(t, "Error sending topic event to " + channel);
                }
            }
        if (event.getType() == NamedTopicEvent.Type.Destroyed)
            {
            onDeactivation();
            }
        }

    private Message[] instantiateEventMessages(NamedTopicEvent event)
        {
        // we only have a destroy event which is handled by the channel closing
        if (event.getType() == NamedTopicEvent.Type.Destroyed)
            {
            Channel                 channel = getChannel();
            Protocol.MessageFactory factory = channel.getMessageFactory();
            Message                 message = factory.createMessage(NamedTopicFactory.TYPE_ID_DESTROY_EVENT);
            return new Message[]{message};
            }
        return new Message[0];
        }

    @Override
    public void onMessage(com.tangosol.net.messaging.Message message)
        {
        if (message instanceof TopicServiceRequest)
            {
            TopicServiceRequest request = (TopicServiceRequest) message;
            request.setTopicName(m_topic.getName());
            request.setTopicService(m_topic.getTopicService());
            request.setTransferThreshold(getTransferThreshold());
            }
        if (message instanceof NamedTopicRequest)
            {
            NamedTopicRequest request = (NamedTopicRequest) message;
            request.setNamedTopic(m_topic);
            request.setTransferThreshold(getTransferThreshold());
            }
        message.run();
        }

    @Override
    public void registerChannel(Channel channel)
        {
        _assert(getChannel() == null);

        NamedTopic topic = getNamedTopic();
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
    public void release()
        {
        // must release the NamedTopic via the TopicServiceProxy
        throw new UnsupportedOperationException();
        }

    public void removeListener(com.tangosol.net.topic.NamedTopicListener listener)
        {
        throw new UnsupportedOperationException();
        }

    /**
     * Setter for property Channel.<p>
     */
    public void setChannel(Channel channel)
        {
        m_channel = channel;
        }

    /**
     * Setter for property NamedTopic.<p>
     */
    public void setNamedTopic(NamedTopic<?> topic)
        {
        m_topic = topic;
        }

    /**
     * Setter for property TransferThreshold.<p>
     */
    public void setTransferThreshold(long lThreshold)
        {
        m_nTransferThreshold = lThreshold;
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

    @Override
    public void unregisterChannel(Channel channel)
        {
        _assert(getChannel() == channel);
        NamedTopic topic = getNamedTopic();
        _assert(topic != null);
        //noinspection DataFlowIssue
        topic.removeListener(this);
        setChannel(null);
        }
    }
