
/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.proxy.NamedTopicProxy

package com.tangosol.coherence.component.net.extend.proxy;

import com.tangosol.coherence.component.net.extend.message.request.NamedTopicRequest;
import com.tangosol.coherence.component.net.extend.message.request.TopicPublisherRequest;

import com.tangosol.coherence.component.net.extend.messageFactory.NamedTopicFactory;

import com.tangosol.coherence.component.net.extend.protocol.NamedTopicProtocol;

import com.tangosol.internal.net.topic.NamedTopicPublisher;
import com.tangosol.internal.net.topic.PublishResult;
import com.tangosol.internal.net.topic.PublisherChannelConnector;
import com.tangosol.internal.net.topic.PublisherConnector;

import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.TopicService;

import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.net.messaging.Message;
import com.tangosol.net.messaging.Protocol;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;

import com.tangosol.net.topic.TopicDependencies;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.function.BiConsumer;

/**
 * A server side proxy for a topic {@link Publisher}.
 *
 * @author Jonathan Knight  2024.11.26
 */
@SuppressWarnings("PatternVariableCanBeUsed")
public class TopicPublisherProxy
        extends com.tangosol.coherence.component.net.extend.Proxy
        implements MemberListener, Channel.Receiver, NamedTopicPublisher.PublisherListener
    {
    /**
     * Property Channel
     */
    private Channel m_channel;

    /**
     * The named topic.
     */
    private transient NamedTopic<?> m_topic;

    /**
     * The converter to use if the client is using a different serializer ot the server.
     */
    private Converter<Binary, Binary> m_converter;

    /**
     * The {@link PublisherConnector} this proxy represents.
     */
    private PublisherConnector<Binary> m_connector;

    /**
     * The list of channel publishers.
     */
    private final Map<Integer, PublisherChannelConnector<Binary>> m_channels = new HashMap<>();

    /**
     * The lock.
     */
    private final Lock f_lock = new ReentrantLock();

    /**
     * Property TransferThreshold
     */
    private long m_nTransferThreshold;

    /**
     * A unique identifier for this proxy.
     */
    private int m_nProxyId;

    public TopicPublisherProxy()
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

    protected void closeChannel()
        {
        Channel channel = getChannel();
        if (channel != null)
            {
            channel.close();
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
            sServiceName = m_connector.getTopicName();
            }
        catch (Throwable t)
            {
            // ignored
            }
        return "NamedTopic=" + (sTopicName == null ? "N/A" : sTopicName)
                + ", Publisher=" + m_connector.getId()
                + ", Service=" + (sServiceName == null ? "N/A" : sServiceName);
        }

    @Override
    public String getName()
        {
        return m_topic.getName() + "#" + m_connector.getId();
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
        return m_nTransferThreshold;
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

    @Override
    public void onMessage(com.tangosol.net.messaging.Message message)
        {
        if (message instanceof NamedTopicRequest)
            {
            NamedTopicRequest request = (NamedTopicRequest) message;
            request.setNamedTopic(m_topic);
            request.setTransferThreshold(getTransferThreshold());
            }
        if (message instanceof TopicPublisherRequest)
            {
            TopicPublisherRequest request = (TopicPublisherRequest) message;
            request.setPublisherConnector(this);
            }
        message.run();
        }

    @Override
    public void registerChannel(Channel channel)
        {
        _assert(getChannel() == null);

        NamedTopic<?> topic = m_topic;
        _assert(topic != null);

        setChannel(channel);

        //noinspection DataFlowIssue
        TopicService service = topic.getTopicService();
        if (service instanceof PagedTopicService &&
                !((PagedTopicService) service).isLocalStorageEnabled())
            {
            service.addMemberListener(this);
            }
        }

    @Override
    public void onEvent(NamedTopicPublisher.PublisherEvent event)
        {
        Channel channel = getChannel();
        Message message = instantiateEventMessage(event);
        if (message != null)
            {
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
                _trace(t, "Error sending MapEvent to " + channel);
                }
            }
        }

    private Message instantiateEventMessage(NamedTopicPublisher.PublisherEvent event)
        {
        NamedTopicPublisher.PublisherEvent.Type type    = event.getType();
        Channel                                 channel = getChannel();

        NamedTopicFactory.PublisherEvent message = (NamedTopicFactory.PublisherEvent)
                channel.getMessageFactory().createMessage(NamedTopicFactory.TYPE_ID_PUBLISHER_EVENT);

        message.setType(type);

        switch (type)
            {
            case Disconnected:
                break;
            case Released:
                break;
            case Destroyed:
                break;
            case ChannelsFreed:
                message.setChannels(event.getChannels());
                return message;
            case Connected:
                break;
            default:
                return null;
            }
        return message;
        }

    /**
     * Setter for property Channel.<p>
     */
    public void setChannel(Channel channel)
        {
        m_channel = channel;
        }

    /**
     * Set the {@link PublisherConnector<Binary>} this proxy represents.
     */
    public void setConnector(PublisherConnector<Binary> connector)
        {
        PublisherConnector<Binary> oldConnector = m_connector;
        if (oldConnector != null)
            {
            oldConnector.removeListener(this);
            }
        m_connector = connector;
        if (connector != null)
            {
            connector.addListener(this);
            }
        }

    /**
     * Return the {@link PublisherConnector}.
     *
     * @return  the {@link PublisherConnector}
     */
    public PublisherConnector<Binary> getConnector()
        {
        return m_connector;
        }

    /**
     * Set the converter to use.
     *
     * @param converter the converter to use
     */
    public void setConverter(Converter<Binary, Binary> converter)
        {
        m_converter = converter;
        }

    /**
     * Return the converter to use.
     *
     * @return the converter to use
     */
    public Converter<Binary, Binary> getConverter()
        {
        return m_converter == null ? bin -> bin : m_converter;
        }

    /**
     * Setter for property TransferThreshold.<p>
     */
    public void setTransferThreshold(long lThreshold)
        {
        m_nTransferThreshold = lThreshold;
        }

    public void setNamedTopic(NamedTopic<?> topic)
        {
        m_topic = topic;
        }

    @Override
    public void unregisterChannel(Channel channel)
        {
        _assert(getChannel() == channel);
        m_connector.close();
        m_connector.removeListener(this);

        setChannel(null);
        }

    /**
     * Return the channel connector, creating a new one if necessary.
     *
     * @param nChannel  the channel to obtain a connector for
     *
     * @return  the connector for the specified channel
     */
    public PublisherChannelConnector<Binary> getChannelConnector(int nChannel)
        {
        PublisherChannelConnector<Binary> connector = m_channels.size() > nChannel
                ? m_channels.get(nChannel) : null;

        if (connector == null)
            {
            f_lock.lock();
            try
                {
                connector = m_channels.computeIfAbsent(nChannel, k -> new ChannelConnector(m_connector.createChannelConnector(nChannel)));
                }
            finally
                {
                f_lock.unlock();
                }
            }
        return connector;
        }

    // ----- inner class: ChannelConnector ----------------------------------

    protected static class ChannelConnector
            implements PublisherChannelConnector<Binary>
        {
        protected final PublisherChannelConnector<Binary> m_channelConnector;

        public ChannelConnector(PublisherChannelConnector<Binary> connector)
            {
            m_channelConnector = connector;
            }

        @Override
        public boolean isActive()
            {
            return m_channelConnector.isActive();
            }

        @Override
        public int getChannel()
            {
            return m_channelConnector.getChannel();
            }

        @Override
        public void close()
            {
            m_channelConnector.close();
            }

        @Override
        public String getTopicName()
            {
            return m_channelConnector.getTopicName();
            }

        @Override
        public void ensureConnected()
            {
            m_channelConnector.ensureConnected();
            }

        @Override
        public CompletionStage<?> initialize()
            {
            return m_channelConnector.initialize();
            }

        @Override
        public void offer(Object oCookie, List<Binary> listBinary, int nNotifyPostFull, BiConsumer<PublishResult, Throwable> handler)
            {
            m_channelConnector.initialize().handle((o, error) ->
                {
                if (error == null)
                    {
                    try
                        {
                        m_channelConnector.offer(o, listBinary, nNotifyPostFull, (result, err) ->
                            {
                            if (err == null && result.getStatus() == PublishResult.Status.Retry)
                                {
                                m_channelConnector.prepareOfferRetry(result.getRetryCookie())
                                        .handle((ignored, throwable) ->
                                            {
                                            handler.accept(result, throwable);
                                            return null;
                                            });
                                }
                            else
                                {
                                handler.accept(result, err);
                                }
                            });
                        }
                    catch (Throwable t)
                        {
                        handler.accept(null, t);
                        }
                    }
                else
                    {
                    handler.accept(null, error);
                    }
                return null;
                });
            }

        @Override
        public CompletionStage<?> prepareOfferRetry(Object oCookie)
            {
            return m_channelConnector.prepareOfferRetry(oCookie);
            }

        @Override
        public TopicDependencies getTopicDependencies()
            {
            return m_channelConnector.getTopicDependencies();
            }

        @Override
        public TopicService getTopicService()
            {
            return m_channelConnector.getTopicService();
            }
        }
    }
