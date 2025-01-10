
/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.RemoteNamedTopic

package com.tangosol.coherence.component.net.extend;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.component.net.extend.messageFactory.NamedTopicFactory;
import com.tangosol.coherence.component.net.extend.protocol.NamedTopicProtocol;

import com.tangosol.internal.net.topic.BaseRemotePublisher;
import com.tangosol.internal.net.topic.NamedTopicPublisher.PublisherEvent;
import com.tangosol.internal.net.topic.PublishResult;
import com.tangosol.internal.net.topic.PublisherChannelConnector;
import com.tangosol.internal.net.topic.PublisherConnector;

import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.Connection;
import com.tangosol.net.messaging.ConnectionException;

import com.tangosol.net.topic.Publisher;

import com.tangosol.util.Binary;

import java.util.List;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Base component for all Coherence*Extend implementation components.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class RemotePublisher<V>
        extends BaseRemotePublisher<V>
        implements Channel.Receiver, PublisherConnector<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link RemotePublisher}.
     *
     * @param nId       the unique publisher identifier
     * @param cChannel  the default channel count for the topic
     * @param options   the publisher options
     */
    public RemotePublisher(long nId, int cChannel, Publisher.Option<? super V>[] options)
        {
        super(nId, cChannel, options);
        }

    // ----- ConnectedPublisher.Connector methods ---------------------------

    @Override
    public boolean isActive()
        {
        Channel channel = m_channel;
        return channel != null && channel.isOpen();
        }

    @Override
    public void close()
        {
        super.close();
        try
            {
            // when this is called due to certain connection error, e.g. ping
            // timeout, the channel could be null and closed.
            Channel channel = getChannel();
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

    @Override
    public void ensureConnected()
        {
        ensureChannel();
        }

    @Override
    public PublisherChannelConnector<V> createChannelConnector(int nChannel)
        {
        return new ChannelConnector(getId(), nChannel);
        }

    @Override
    public void unregisterChannel(Channel channel)
        {
        if (m_channel != null)
            {
            Logger.finer("Disconnected remote publisher topic=" + getTopicName() + " id=" + getId());
            dispatchEvent(PublisherEvent.Type.Disconnected);
            m_channel = null;
            }
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
    public void onChannelClosed(Channel channel)
        {
        dispatchEvent(PublisherEvent.Type.Disconnected);
        }

    @Override
    public void onMessage(com.tangosol.net.messaging.Message message)
        {
        message.run();
        }

    @Override
    public void registerChannel(Channel channel)
        {
        m_channel = channel;
        dispatchEvent(PublisherEvent.Type.Connected);
        }

    // ----- helper methods -------------------------------------------------

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
        Channel channel = m_channel;
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

    /**
     * Setter for property Channel.<p>
     */
    public void setChannel(Channel channel)
        {
        m_channel = channel;
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
         * @param nId       the unique identifier for this publisher
         * @param nChannel  the channel identifier for this connector
         */
        public ChannelConnector(long nId, int nChannel)
            {
            super(nId, nChannel);
            }

        @Override
        public boolean isActive()
            {
            return RemotePublisher.this.isActive();
            }

        @Override
        protected CompletionStage<PublishResult> offerInternal(List<Binary> listBinary, int nNotifyPostFull)
            {
            try
                {
                Channel                          channel = ensureChannel();
                Protocol.MessageFactory          factory = channel.getMessageFactory();
                NamedTopicFactory.PublishRequest request = (NamedTopicFactory.PublishRequest) factory.createMessage(NamedTopicFactory.TYPE_ID_PUBLISH);

                request.setChannel(f_nChannel);
                request.setBinaries(listBinary);
                request.setNotify(nNotifyPostFull);

                PublishResult result = (PublishResult) channel.request(request);
                return CompletableFuture.completedFuture(result);
                }
            catch (Throwable t)
                {
                if (t instanceof ConnectionException)
                    {
                    Channel channel = m_channel;
                    if (channel != null)
                        {
                        m_channel = null;
                        Logger.finer("Closing remote publisher channel due to connection exception " + m_channel);
                        // The proxy disconnected, close the channel
                        dispatchEvent(PublisherEvent.Type.Disconnected);
                        channel.close();
                        }
                    }
                return CompletableFuture.failedFuture(t);
                }
            }
        }


    // ----- data members ---------------------------------------------------

    /**
     * The {@link Channel} to use to send and receive requests and messages.
     */
    private Channel m_channel;
    }
