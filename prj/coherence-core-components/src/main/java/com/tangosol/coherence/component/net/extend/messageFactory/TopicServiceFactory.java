
/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.messageFactory.TopicServiceFactory

package com.tangosol.coherence.component.net.extend.messageFactory;

import com.tangosol.coherence.component.net.extend.MessageFactory;

import com.tangosol.coherence.component.net.extend.message.Response;
import com.tangosol.coherence.component.net.extend.message.request.TopicServiceRequest;

import com.tangosol.coherence.component.net.extend.protocol.NamedTopicProtocol;

import com.tangosol.coherence.component.net.extend.proxy.NamedTopicProxy;
import com.tangosol.coherence.component.net.extend.proxy.TopicPublisherProxy;
import com.tangosol.coherence.component.net.extend.proxy.TopicSubscriberProxy;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;

import com.tangosol.internal.net.topic.NamedTopicPublisher;
import com.tangosol.internal.net.topic.PublisherConnector;
import com.tangosol.internal.net.topic.SubscriberConnector.ConnectedSubscriber;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.Member;
import com.tangosol.net.Service;
import com.tangosol.net.TopicService;

import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.Connection;
import com.tangosol.net.messaging.ConnectionManager;

import com.tangosol.net.topic.NamedTopic;

import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.Binary;
import com.tangosol.util.Filter;
import com.tangosol.util.ListMap;
import com.tangosol.util.ValueExtractor;

import java.io.IOException;

import java.net.URI;

import java.util.Map;

/**
 * The {@link MessageFactory} to create messages for the
 * topic service Extend protocol.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class TopicServiceFactory
        extends BaseTopicMessageFactory
    {
    public static final int TYPE_ID_CHANNEL_COUNT = 1;
    public static final int TYPE_ID_DESTROY_TOPIC = 3;
    public static final int TYPE_ID_ENSURE_CHANNEL_COUNT = 4;
    public static final int TYPE_ID_ENSURE_TOPIC          = 5;
    public static final int TYPE_ID_GET_SUBSCRIBER_GROUPS = 6;
    public static final int TYPE_ID_ENSURE_PUBLISHER      = 8;
    public static final int TYPE_ID_ENSURE_SUBSCRIBER = 9;
    public static final int TYPE_ID_DESTROY_PUBLISHER = 10;
    public static final int TYPE_ID_DESTROY_SUBSCRIBER = 11;

    private static ListMap<String, Class<?>> __mapChildren;

    static
        {
        __initStatic();
        }

    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new ListMap<>();
        __mapChildren.put("ChannelCountRequest", ChannelCountRequest.class);
        __mapChildren.put("DestroyTopicRequest", DestroyTopicRequest.class);
        __mapChildren.put("EnsureChannelCountRequest", EnsureChannelCountRequest.class);
        __mapChildren.put("EnsureTopicRequest", EnsureTopicRequest.class);
        __mapChildren.put("GetSubscriberGroupsRequest", GetSubscriberGroupsRequest.class);
        __mapChildren.put("EnsurePublisherRequest", EnsurePublisherRequest.class);
        __mapChildren.put("EnsureSubscriberRequest", EnsureSubscriberRequest.class);
        __mapChildren.put("Response", TopicsResponse.class);
        }

    public TopicServiceFactory()
        {
        super(null, null, true);
        }

    @Override
    protected Map<String, Class<?>> get_ChildClasses()
        {
        return __mapChildren;
        }

    // ----- inner class: ChannelCountRequest -------------------------------

    /**
     * A request to obtain the channel count for a topic.
     */
    public static class ChannelCountRequest
            extends TopicServiceRequest
        {
        public ChannelCountRequest()
            {
            super(null, null, true);
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_CHANNEL_COUNT;
            }

        @Override
        protected void onRun(Response response)
            {
            TopicService service = getTopicService();
            _assert(service != null);
            //noinspection DataFlowIssue
            int cChannel = service.getChannelCount(getTopicName());
            response.setResult(cChannel);
            }
        }

    // ----- inner class: DestroyTopicRequest -------------------------------

    /**
     * A request to destroy a topic.
     */
    public static class DestroyTopicRequest
            extends TopicServiceRequest
        {
        public DestroyTopicRequest()
            {
            super(null, null, true);
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_DESTROY_TOPIC;
            }

        @Override
        protected void onRun(Response response)
            {
            TopicService       service = getTopicService();
            NamedTopic<Object> topic   = service.ensureTopic(getTopicName(), null);
            //service.destroyTopic(topic);
            topic.destroy();
            response.setResult(Boolean.TRUE);
            }
        }

    // ----- EnsureChannelCountRequest --------------------------------------

    /**
     * A request to ensure a topic has a minimum number of channels.
     */
    public static class EnsureChannelCountRequest
            extends TopicServiceRequest
        {
        /**
         * Property ChannelCount
         */
        private int __m_ChannelCount;

        /**
         * Property RequiredChannels
         */
        private int __m_RequiredChannels;

        public EnsureChannelCountRequest()
            {
            super(null, null, true);
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_ENSURE_CHANNEL_COUNT;
            }

        @Override
        protected void onRun(Response response)
            {
            TopicService service  = getTopicService();
            int          cChannel = service.ensureChannelCount(getTopicName(), __m_RequiredChannels, __m_ChannelCount);
            response.setResult(cChannel);
            }

        /**
         * Setter for property ChannelCount.<p>
         */
        public void setChannelCount(int nCount)
            {
            __m_ChannelCount = nCount;
            }

        /**
         * Setter for property RequiredChannels.<p>
         */
        public void setRequiredChannels(int nChannels)
            {
            __m_RequiredChannels = nChannels;
            }

        /**
         * Getter for property ChannelCount.<p>
         */
        public int getChannelCount()
            {
            return __m_ChannelCount;
            }

        /**
         * Getter for property RequiredChannels.<p>
         */
        public int getRequiredChannels()
            {
            return __m_RequiredChannels;
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);

            setRequiredChannels(in.readInt(2));
            setChannelCount(in.readInt(3));
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);

            out.writeInt(2, getRequiredChannels());
            out.writeInt(3, getChannelCount());
            }
        }

    // ----- EnsureTopicRequest ---------------------------------------------

    /**
     * A request that creates a new channel.
     */
    public static abstract class NewChannelRequest
            extends TopicServiceRequest
        {
        protected NewChannelRequest()
            {
            super(null, null, true);
            }

        protected URI createChannel(Channel.Receiver receiver)
            {
            Channel    channel    = getChannel();
            Connection connection = channel.getConnection();
            URI        uri        = connection.createChannel(NamedTopicProtocol.getInstance(), null, receiver);
            if (m_fAutoAccept)
                {
                connection.acceptChannel(uri, null, receiver, null);
                }
            return uri;
            }

        // ----- data members -----------------------------------------------

        /**
         * A flag that is {@code true} to indicate the new channel should be immediately accepted.
         * This is not normal Extend behaviour and is used by gRPC to accept channels.
         */
        protected boolean m_fAutoAccept;
        }

    // ----- EnsureTopicRequest ---------------------------------------------

    /**
     * The ensure topic request.
     */
    public static class EnsureTopicRequest
            extends NewChannelRequest
        {
        public EnsureTopicRequest()
            {
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_ENSURE_TOPIC;
            }

        /**
         * Called when the Request is run.
         *
         * @param response the Response that should be populated with the
         *                 result of running the Request
         */
        @Override
        protected void onRun(Response response)
            {
            TopicService  service = getTopicService();
            String        sName   = getTopicName();

            if (sName == null || sName.isEmpty())
                {
                throw new IllegalArgumentException("The topic name cannot be null or blank");
                }

            NamedTopic<?>   topic = service.ensureTopic(sName, null);
            NamedTopicProxy proxy = new NamedTopicProxy();
            proxy.setNamedTopic(topic);
            proxy.setTransferThreshold(getTransferThreshold());

            URI uri = createChannel(proxy);

            ConnectionManager manager = getChannel().getConnection().getConnectionManager();
            if (manager instanceof Peer)
                {
                Service parentService = ((Peer) manager).getParentService();
                if (parentService instanceof ProxyService)
                    {
                    proxy.setDaemonPool(((ProxyService) parentService).getDaemonPool());
                    }
                }
            response.setResult(String.valueOf(uri));
            }
        }

    // ----- inner class: EnsurePublisherRequest ----------------------------

    /**
     * A request to ensure a publisher.
     */
    @SuppressWarnings({"unchecked"})
    public static class EnsurePublisherRequest
            extends NewChannelRequest
        {
        /**
         * The position of the URI in the response array.
         */
        public static final int RESPONSE_ID_URI = 0;

        /**
         * The position of the publisher identifier in the response array.
         */
        public static final int RESPONSE_ID_PUBLISHER_ID = 1;

        /**
         * The position of the batch size in the response array.
         */
        public static final int RESPONSE_ID_BATCH_SIZE = 2;

        /**
         * The position of the channel count in the response array.
         */
        public static final int RESPONSE_ID_CHANNEL_COUNT = 3;

        /**
         * The publisher's channel count.
         */
        private int m_cChannel;

        public EnsurePublisherRequest()
            {
            }

        public void setChannelCount(int cChannel)
            {
            m_cChannel = cChannel;
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_ENSURE_PUBLISHER;
            }

        @Override
        protected void onRun(Response response)
            {
            TopicService  service = getTopicService();
            String        sName   = getTopicName();

            if (sName == null || sName.isEmpty())
                {
                throw new IllegalArgumentException("The topic name cannot be null or blank");
                }

            NamedTopic<?>              topic     = service.ensureTopic(sName, null);
            TopicPublisherProxy        proxy     = createPublisherProxy(topic);
            PublisherConnector<Binary> connector = proxy.getConnector();

            URI uri = createChannel(proxy);

            ConnectionManager manager = getChannel().getConnection().getConnectionManager();
            if (manager instanceof Peer)
                {
                Service parentService = ((Peer) manager).getParentService();
                if (parentService instanceof ProxyService)
                    {
                    proxy.setDaemonPool(((ProxyService) parentService).getDaemonPool());
                    }
                }

            Object[] aoResponse = new Object[4];
            aoResponse[RESPONSE_ID_URI]           = String.valueOf(uri);
            aoResponse[RESPONSE_ID_PUBLISHER_ID]  = connector.getId();
            aoResponse[RESPONSE_ID_BATCH_SIZE]    = connector.getMaxBatchSizeBytes();
            aoResponse[RESPONSE_ID_CHANNEL_COUNT] = connector.getChannelCount();

            response.setResult(aoResponse);
            }

        /**
         * Create a {@link TopicPublisherProxy}.
         *
         * @return a {@link TopicPublisherProxy}
         */
        @SuppressWarnings("rawtypes")
        protected TopicPublisherProxy createPublisherProxy(NamedTopic<?> topic)
            {
            if (topic instanceof PublisherConnector.Factory)
                {
                Publisher.Option[] options;
                if (m_cChannel <= 0)
                    {
                    options = new Publisher.Option[0];
                    }
                else
                    {
                    options = new Publisher.Option[]{NamedTopicPublisher.ChannelCount.of(m_cChannel)};
                    }

                PublisherConnector<Binary> connector = ((PublisherConnector.Factory<?>) topic)
                        .createPublisherConnector(options);

                TopicPublisherProxy proxy = new TopicPublisherProxy();
                proxy.setNamedTopic(topic);
                proxy.setConnector(connector);
                proxy.setTransferThreshold(getTransferThreshold());

                return proxy;
                }
            throw new IllegalArgumentException("The topic is not an instance of "
                    + PublisherConnector.Factory.class.getName());
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            super.readExternal(in);
            m_cChannel = in.readInt(10);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            super.writeExternal(out);
            out.writeInt(10, m_cChannel);
            }
        }

    // ----- inner class: DestroyPublisherRequest ---------------------------

    /**
     * A request to destroy a publisher.
     */
    public static class DestroyPublisherRequest
            extends TopicServiceRequest
        {
        /**
         * The identifier for the publisher to destroy.
         */
        private int m_nPublisherId;

        public DestroyPublisherRequest()
            {
            super(null, null, true);
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_DESTROY_PUBLISHER;
            }

        /**
         * Set the identifier for the publisher to destroy.
         *
         * @param nPublisherId  the identifier for the publisher to destroy
         *
         * @throws IllegalArgumentException if the identifier specified is negative or zero.
         */
        public void setPublisherId(int nPublisherId)
            {
            if (nPublisherId <= 0)
                {
                throw new IllegalArgumentException("The publisher identifier must be non-zero and positive");
                }
            m_nPublisherId = nPublisherId;
            }

        @Override
        protected void onRun(Response response)
            {
            if (m_nPublisherId <= 0)
                {
                throw new IllegalArgumentException("The publisher identifier must be non-zero and positive");
                }
            Channel channel = getChannel().getConnection().getChannel(m_nPublisherId);
            if (channel != null)
                {
                channel.close();
                }
            }
        }

    // ----- inner class: EnsureSubscriberRequest ---------------------------

    /**
     * A request to ensure a subscriber.
     */
    @SuppressWarnings({"unchecked"})
    public static class EnsureSubscriberRequest
            extends NewChannelRequest
        {
        /**
         * The position of the URI in the response array.
         */
        public static final int RESPONSE_ID_URI = 0;

        /**
         * The position of the {@link com.tangosol.internal.net.topic.impl.paged.model.SubscriberId}
         * in the response array.
         */
        public static final int RESPONSE_ID_SUBSCRIBER_ID = 1;

        /**
         * The position of the {@link com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId}
         * in the response array.
         */
        public static final int RESPONSE_ID_GROUP_ID = 2;

        /**
         * An optional {@link Filter} to filter messages.
         */
        private Filter<?> m_filter;

        /**
         * An optional {@link ValueExtractor} to convert messages.
         */
        private ValueExtractor<?, ?> m_extractor;

        /**
         * {@code true} if the subscriber should return from a receive request
         * immediately if the topic is empty
         */
        private boolean m_fCompleteOnEmpty;

        /**
         * The subscriber group identifier.
         */
        private String m_sSubscriberGroup;

        /**
         * The channels this subscriber wants to subscribe to.
         */
        private int[] m_anChannel;

        // ----- constructors ---------------------------------------------------

        public EnsureSubscriberRequest()
            {
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_ENSURE_SUBSCRIBER;
            }

        @Override
        protected void onRun(Response response)
            {
            TopicService  service = getTopicService();
            String        sName   = getTopicName();

            if (sName == null || sName.isEmpty())
                {
                throw new IllegalArgumentException("The topic name cannot be null or blank");
                }

            NamedTopic<?>               topic      = service.ensureTopic(sName, null);
            TopicSubscriberProxy        proxy      = createTopicSubscriberProxy(topic);
            ConnectedSubscriber<Binary> subscriber = proxy.getSubscriber();

            URI uri = createChannel(proxy);

            ConnectionManager manager = getChannel().getConnection().getConnectionManager();
            if (manager instanceof Peer)
                {
                Service parentService = ((Peer) manager).getParentService();
                if (parentService instanceof ProxyService)
                    {
                    proxy.setDaemonPool(((ProxyService) parentService).getDaemonPool());
                    }
                }

            Object[] aoResponse = new Object[3];
            aoResponse[RESPONSE_ID_URI]           = String.valueOf(uri);
            aoResponse[RESPONSE_ID_SUBSCRIBER_ID] = subscriber.getSubscriberId();
            aoResponse[RESPONSE_ID_GROUP_ID]      = subscriber.getSubscriberGroupId();

            response.setResult(aoResponse);
            }

        @SuppressWarnings("rawtypes")
        public TopicSubscriberProxy createTopicSubscriberProxy(NamedTopic<?> topic)
            {
            Channel    channel    = getChannel();
            Connection connection = channel.getConnection();
            Member     member     = topic.getTopicService().getCluster().getLocalMember();

            Subscriber.Option[] options = new Subscriber.Option[]
                    {
                    new NamedTopicFactory.SubscriberIdOption(member.getId(), member.getUuid()),
                    m_sSubscriberGroup != null && !m_sSubscriberGroup.isEmpty()
                            ? Subscriber.inGroup(m_sSubscriberGroup)
                            : Subscriber.Option.nullOption(),
                    Subscriber.withFilter(m_filter),
                    Subscriber.withConverter(m_extractor),
                    m_fCompleteOnEmpty ? Subscriber.CompleteOnEmpty.enabled() : Subscriber.Option.nullOption(),
                    Subscriber.subscribeTo(m_anChannel),
                    };

            ConnectedSubscriber<Binary> subscriber
                    = (ConnectedSubscriber<Binary>) topic.createSubscriber(options);

            TopicSubscriberProxy proxy = new TopicSubscriberProxy();
            proxy.setSubscriber(subscriber);
            proxy.setNamedTopic(topic);
            proxy.setTransferThreshold(getTransferThreshold());

            ConnectionManager manager = connection.getConnectionManager();
            if (manager instanceof Peer)
                {
                Service parentService = ((Peer) manager).getParentService();
                if (parentService instanceof ProxyService)
                    {
                    proxy.setDaemonPool(((ProxyService) parentService).getDaemonPool());
                    }
                }

            return proxy;
            }

        public void setFilter(Filter<?> filter)
            {
            m_filter = filter;
            }

        public void setExtractor(ValueExtractor<?,?> extractor)
            {
            m_extractor = extractor;
            }

        public void setCompleteOnEmpty(boolean fCompleteOnEmpty)
            {
            m_fCompleteOnEmpty = fCompleteOnEmpty;
            }

        public void setSubscriberGroup(String sSubscriberGroup)
            {
            m_sSubscriberGroup = sSubscriberGroup;
            }

        public void setChannels(int[] anChannel)
            {
            m_anChannel = anChannel;
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            super.readExternal(in);
            m_filter           = in.readObject(10);
            m_extractor        = in.readObject(11);
            m_fCompleteOnEmpty = in.readBoolean(12);
            m_sSubscriberGroup = in.readString(13);
            m_anChannel        = in.readIntArray(14);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeObject(10, m_filter);
            out.writeObject(11, m_extractor);
            out.writeBoolean(12, m_fCompleteOnEmpty);
            out.writeString(13, m_sSubscriberGroup);
            out.writeIntArray(14, m_anChannel);
            }
        }

    // ----- inner class: DestroySubscriberRequest --------------------------

    /**
     * A request to destroy a subscriber.
     */
    public static class DestroySubscriberRequest
            extends TopicServiceRequest
        {
        /**
         * The identifier for the subscriber to destroy.
         */
        private int m_nSubscriberId;

        public DestroySubscriberRequest()
            {
            super(null, null, true);
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_DESTROY_SUBSCRIBER;
            }

        /**
         * Get the identifier for the subscriber to destroy.
         *
         * @return the identifier for the subscriber to destroy
         */
        public int getSubscriberId()
            {
            return m_nSubscriberId;
            }

        /**
         * Set the identifier for the subscriber to destroy.
         *
         * @param nSubscriberId  the identifier for the subscriber to destroy
         *
         * @throws IllegalArgumentException if the identifier specified is negative or zero.
         */
        public void setSubscriberId(int nSubscriberId)
            {
            if (nSubscriberId <= 0)
                {
                throw new IllegalArgumentException("The subscriber identifier must be non-zero and positive");
                }
            m_nSubscriberId = nSubscriberId;
            }

        @Override
        protected void onRun(Response response)
            {
            if (m_nSubscriberId <= 0)
                {
                throw new IllegalArgumentException("The subscriber identifier must be non-zero and positive");
                }
            Channel channel = getChannel().getConnection().getChannel(m_nSubscriberId);
            if (channel != null)
                {
                Channel.Receiver receiver = channel.getReceiver();
                if (receiver instanceof TopicSubscriberProxy)
                    {
                    TopicSubscriberProxy        proxy      = (TopicSubscriberProxy) receiver;
                    ConnectedSubscriber<Binary> subscriber = proxy.getSubscriber();
                    subscriber.close();
                    }
                channel.close();
                }
            }
        }

    // ----- GetSubscriberGroupsRequest -------------------------------------

    /**
     * A request to get a subscriber group.
     */
    public static class GetSubscriberGroupsRequest
            extends TopicServiceRequest
        {
        public GetSubscriberGroupsRequest()
            {
            super(null, null, true);
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_GET_SUBSCRIBER_GROUPS;
            }

        /**
         * Called when the Request is run.
         *
         * @param response the Response that should be populated with the
         *                 result of running the Request
         */
        @Override
        protected void onRun(Response response)
            {
            TopicService service = getTopicService();
            _assert(service != null);
            //noinspection DataFlowIssue
            response.setResult(service.getSubscriberGroups(getTopicName()));
            }
        }
    }
