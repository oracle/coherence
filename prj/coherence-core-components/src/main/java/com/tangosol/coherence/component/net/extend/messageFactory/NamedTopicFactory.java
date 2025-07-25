
/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.messageFactory.NamedTopicFactory

package com.tangosol.coherence.component.net.extend.messageFactory;

import com.tangosol.coherence.Component;

import com.tangosol.coherence.component.net.extend.Message;
import com.tangosol.coherence.component.net.extend.MessageFactory;
import com.tangosol.coherence.component.net.extend.RemotePublisher;
import com.tangosol.coherence.component.net.extend.RemoteSubscriber;
import com.tangosol.coherence.component.net.extend.RemoteSubscriberChannel;

import com.tangosol.coherence.component.net.extend.message.Response;
import com.tangosol.coherence.component.net.extend.message.request.NamedTopicRequest;
import com.tangosol.coherence.component.net.extend.message.request.TopicPublisherRequest;
import com.tangosol.coherence.component.net.extend.message.request.TopicSubscriberRequest;

import com.tangosol.coherence.component.net.extend.proxy.TopicPublisherProxy;

import com.tangosol.coherence.component.net.extend.proxy.TopicSubscriberProxy;
import com.tangosol.internal.net.topic.NamedTopicPublisher;
import com.tangosol.internal.net.topic.NamedTopicSubscriber;
import com.tangosol.internal.net.topic.PublishResult;
import com.tangosol.internal.net.topic.PublisherChannelConnector;
import com.tangosol.internal.net.topic.ReceiveResult;
import com.tangosol.internal.net.topic.SeekResult;
import com.tangosol.internal.net.topic.SimpleReceiveResult;
import com.tangosol.internal.net.topic.SubscriberConnector;
import com.tangosol.internal.net.topic.SubscriberConnector.ConnectedSubscriber;
import com.tangosol.internal.net.topic.SubscriberConnector.SubscriberEvent;
import com.tangosol.internal.net.topic.TopicSubscription;

import com.tangosol.internal.net.topic.impl.paged.agent.PollProcessor;

import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.messaging.Channel;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Binary;
import com.tangosol.util.Filter;
import com.tangosol.util.ListMap;
import com.tangosol.util.UUID;
import com.tangosol.util.ValueExtractor;

import java.io.IOException;

import java.time.Instant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

/**
 * The {@link MessageFactory} to create messages for the {@link NamedTopic}
 * Extend protocol.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class NamedTopicFactory
        extends BaseTopicMessageFactory
    {
    private static ListMap<String, Class<?>> __mapChildren;

    public static final int TYPE_ID_REMAINING_MESSAGES = 1;
    public static final int TYPE_ID_ENSURE_SUBSCRIBER_GROUP = 2;
    public static final int TYPE_ID_DESTROY_SUBSCRIBER_GROUP = 3;
    public static final int TYPE_ID_DESTROY_EVENT = 4;
    public static final int TYPE_ID_PUBLISHER_EVENT = 6;
    public static final int TYPE_ID_PUBLISH = 7;
    public static final int TYPE_ID_INITIALIZE_SUBSCRIPTION = 10;
    public static final int TYPE_ID_ENSURE_SUBSCRIPTION = 11;
    public static final int TYPE_ID_GET_SUBSCRIPTION = 12;
    public static final int TYPE_ID_GET_OWNED_CHANNELS = 13;
    public static final int TYPE_ID_RECEIVE = 14;
    public static final int TYPE_ID_PEEK = 16;
    public static final int TYPE_ID_COMMIT = 17;
    public static final int TYPE_ID_IS_COMMITTED = 19;
    public static final int TYPE_ID_GET_LAST_COMMITTED = 20;
    public static final int TYPE_ID_GET_HEADS = 21;
    public static final int TYPE_ID_GET_TAILS = 22;
    public static final int TYPE_ID_SEEK = 23;
    public static final int TYPE_ID_HEARTBEAT = 24;
    public static final int TYPE_ID_SUBSCRIBER_EVENT = 25;
    public static final int TYPE_ID_SIMPLE_RECEIVE = 26;

    static
        {
        __initStatic();
        }

    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new ListMap<>();
        __mapChildren.put("Response", TopicsResponse.class);
        __mapChildren.put("GetRemainingMessagesRequest", GetRemainingMessagesRequest.class);
        __mapChildren.put("EnsureSubscriberGroupRequest", EnsureSubscriberGroupRequest.class);
        __mapChildren.put("DestroySubscriberGroupRequest", DestroySubscriberGroupRequest.class);
        __mapChildren.put("PublisherEvent", PublisherEvent.class);
        __mapChildren.put("PublishRequest", PublishRequest.class);
        __mapChildren.put("InitializeSubscriptionRequest", InitializeSubscriptionRequest.class);
        __mapChildren.put("EnsureSubscriptionRequest", EnsureSubscriptionRequest.class);
        __mapChildren.put("GetSubscriptionRequest", GetSubscriptionRequest.class);
        __mapChildren.put("GetOwnedChannelsRequest", GetOwnedChannelsRequest.class);
        __mapChildren.put("ReceiveRequest", ReceiveRequest.class);
        __mapChildren.put("SimpleReceiveRequest", SimpleReceiveRequest.class);
        __mapChildren.put("PeekRequest", PeekRequest.class);
        __mapChildren.put("CommitRequest", CommitRequest.class);
        __mapChildren.put("IsCommitedRequest", IsCommitedRequest.class);
        __mapChildren.put("GetLastCommitedRequest", GetLastCommitedRequest.class);
        __mapChildren.put("GetHeadsRequest", GetHeadsRequest.class);
        __mapChildren.put("GetTailsRequest", GetTailsRequest.class);
        __mapChildren.put("SeekRequest", SeekRequest.class);
        __mapChildren.put("HeartbeatRequest", HeartbeatRequest.class);
        __mapChildren.put("SubscriberEvent", SubscriberChannelEvent.class);
        __mapChildren.put("DestroyEvent", DestroyEvent.class);
        }

    public NamedTopicFactory()
        {
        super(null, null, true);
        }

    @Override
    protected Map<String, Class<?>> get_ChildClasses()
        {
        return __mapChildren;
        }

    // ----- inner class: GetRemainingMessagesRequest -----------------------

    /**
     * A message to return the number of messages remaining in a topic
     * for a subscriber group..
     */
    public static class GetRemainingMessagesRequest
            extends NamedTopicRequest
        {
        private String m_sSubscriberGroup;

        private int[] m_anChannel;

        public GetRemainingMessagesRequest()
            {
            super(null, null, true);
            }

        public GetRemainingMessagesRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_REMAINING_MESSAGES;
            }

        @Override
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            NamedTopic<?> topic = getNamedTopic();
            _assert(topic != null);
            _assert(m_sSubscriberGroup != null);
            int count;
            if (m_anChannel == null)
                {
                //noinspection DataFlowIssue
                count = topic.getRemainingMessages(m_sSubscriberGroup);
                }
            else
                {
                //noinspection DataFlowIssue
                count = topic.getRemainingMessages(m_sSubscriberGroup, m_anChannel);
                }
            response.setResult(count);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            super.readExternal(in);
            m_sSubscriberGroup = in.readString(10);
            m_anChannel        = in.readIntArray(11);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeString(10, m_sSubscriberGroup);
            out.writeIntArray(11, m_anChannel);
            }

        public void setSubscriberGroup(String sSubscriberGroup)
            {
            m_sSubscriberGroup = sSubscriberGroup;
            }

        public void setChannels(int[] anChannel)
            {
            m_anChannel = anChannel;
            }
        }

    // ----- inner class: GetRemainingMessagesRequest -----------------------

    /**
     * A message to ensure a subscriber group.
     */
    public static class EnsureSubscriberGroupRequest
            extends NamedTopicRequest
        {
        private String m_sSubscriberGroup;

        private Filter<?> m_filter;

        private ValueExtractor<?, ?> m_extractor;

        public EnsureSubscriberGroupRequest()
            {
            super(null, null, true);
            }

        public EnsureSubscriberGroupRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_ENSURE_SUBSCRIBER_GROUP;
            }

        @Override
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            NamedTopic<?> topic = getNamedTopic();
            _assert(topic != null);
            //noinspection DataFlowIssue
            topic.ensureSubscriberGroup(m_sSubscriberGroup, m_filter, m_extractor);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            super.readExternal(in);
            m_sSubscriberGroup = in.readString(10);
            m_filter           = in.readObject(11);
            m_extractor        = in.readObject(12);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeString(10, m_sSubscriberGroup);
            out.writeObject(11, m_filter);
            out.writeObject(12, m_extractor);
            }

        public void setSubscriberGroup(String sSubscriberGroup)
            {
            m_sSubscriberGroup = sSubscriberGroup;
            }

        public void setFilter(Filter<?> filter)
            {
            m_filter = filter;
            }

        public void setExtractor(ValueExtractor<?, ?> extractor)
            {
            m_extractor = extractor;
            }
        }

    // ----- inner class: GetRemainingMessagesRequest -----------------------

    /**
     * A message to destroy a subscriber group.
     */
    public static class DestroySubscriberGroupRequest
            extends NamedTopicRequest
        {
        private String m_sSubscriberGroup;

        public DestroySubscriberGroupRequest()
            {
            super(null, null, true);
            }

        public DestroySubscriberGroupRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_DESTROY_SUBSCRIBER_GROUP;
            }

        @Override
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            NamedTopic<?> topic = getNamedTopic();
            _assert(topic != null);
            _assert(m_sSubscriberGroup != null);
            //noinspection DataFlowIssue
            topic.destroySubscriberGroup(m_sSubscriberGroup);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            super.readExternal(in);
            m_sSubscriberGroup = in.readString(10);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeString(10, m_sSubscriberGroup);
            }

        public void setSubscriberGroup(String sSubscriberGroup)
            {
            m_sSubscriberGroup = sSubscriberGroup;
            }
        }

    // ----- inner class: SubscriberIdOptionRequest -------------------------

    /**
     * A {@link Subscriber.Option} to set the subscriber identifier.
     */
    @SuppressWarnings("rawtypes")
    protected static class SubscriberIdOption
            implements NamedTopicSubscriber.WithSubscriberId
        {
        public SubscriberIdOption(int nId, UUID uuid)
            {
            m_nId  = nId;
            m_uuid = uuid;
            }

        @Override
        public SubscriberId getId(int nNotificationId)
            {
            return new SubscriberId(nNotificationId, m_nId, m_uuid);
            }

        // ----- data members -----------------------------------------------

        private final int m_nId;

        private final UUID m_uuid;
        }

    // ----- inner class: PublishRequest ------------------------------------

    /**
     * A message to publish values to a topic.
     */
    public static class PublishRequest
            extends TopicPublisherRequest
        {
        /**
         * The channel to publish to.
         */
        protected int m_nChannel;

        /**
         * The list of serialized binary values to publish.
         */
        protected List<Binary> m_listBinary;

        /**
         * The notification identifier.
         */
        protected int m_nNotify;

        public PublishRequest()
            {
            super(null, null, true);
            }

        public PublishRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_PUBLISH;
            }

        @Override
        @SuppressWarnings("DataFlowIssue")
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            TopicPublisherProxy proxy = getPublisherConnector();
            _assert(proxy != null);

            PublisherChannelConnector<Binary> connector = proxy.getChannelConnector(m_nChannel);
            _assert(connector != null);

            CompletableFuture<PublishResult> future = new CompletableFuture<>();
            connector.offer(null, m_listBinary, m_nNotify, (result, errOffer) ->
                {
                if (errOffer != null)
                    {
                    future.completeExceptionally(errOffer);
                    }
                else
                    {
                    future.complete(result);
                    }
                });
            PublishResult result = future.join();
            response.setResult(result);
            }

        public void setChannel(int nChannel)
            {
            m_nChannel = nChannel;
            }

        public void setBinaries(List<Binary> list)
            {
            m_listBinary = list;
            }

        public void setNotify(int nNotify)
            {
            m_nNotify = nNotify;
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_nChannel   = in.readInt(10);
            m_listBinary = in.readCollection(11, new ArrayList<>());
            m_nNotify    = in.readInt(12);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeInt(10, m_nChannel);
            out.writeCollection(11, m_listBinary, Binary.class);
            out.writeInt(12, m_nNotify);
            }
        }

    // ----- inner class: PublisherEventRequest -----------------------------

    /**
     * A message to dispatch a {@link NamedTopicPublisher.PublisherEvent}.
     */
    public static class PublisherEvent
            extends Message
        {
        /**
         * The event type.
         */
        private NamedTopicPublisher.PublisherEvent.Type m_nType;

        /**
         * The event channels.
         */
        private int[] m_anChannel;

        // ----- constructors -----------------------------------------------

        public PublisherEvent()
            {
            this(null, null, true);
            }

        public PublisherEvent(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            if (fInit)
                {
                __init();
                }
            }

        // ----- accessors ----------------------------------------------------

        /**
         * Return the message type.
         * @return the message type
         */
        public NamedTopicPublisher.PublisherEvent.Type getType()
            {
            return m_nType;
            }

        /**
         * Set the event type.
         *
         * @param nType the event type
         */
        public void setType(NamedTopicPublisher.PublisherEvent.Type nType)
            {
            m_nType = nType;
            }

        /**
         * Obtain the channels this event applies to.
         *
         * @return the channels this event applies to
         */
        public int[] getChannels()
            {
            return m_anChannel;
            }

        /**
         * Set the event channels
         *
         * @param anChannel  the event channels
         */
        public void setChannels(int[] anChannel)
            {
            m_anChannel = anChannel;
            }

        // ----- Message methods --------------------------------------------

        @Override
        public int getTypeId()
            {
            return TYPE_ID_PUBLISHER_EVENT;
            }

        @Override
        public void __init()
            {
            // private initialization
            __initPrivate();
            // signal the end of the initialization
            set_Constructed(true);
            }

        @Override
        protected void __initPrivate()
            {
            super.__initPrivate();
            }

        /**
         * Return a human-readable description of this component.
         *
         * @return a String representation of this component
         */
        @Override
        protected String getDescription()
            {
            if (m_anChannel == null)
                {
                return super.getDescription()
                        + ", Type="    + m_nType
                        + ", Channels=<none>";
                }
            return super.getDescription()
                    + ", Type="    + m_nType
                    + ", Channels=" + Arrays.toString(m_anChannel);
            }

        @Override
        public void run()
            {
            Channel                                 channel   = getChannel();
            RemotePublisher<?>                      publisher = (RemotePublisher<?>) channel.getReceiver();
            NamedTopicPublisher.PublisherEvent.Type type      = getType();
            int[]                                   anChannel = getChannels();

            if (anChannel == null)
                {
                publisher.dispatchEvent(type);
                }
            else
                {
                publisher.dispatchEvent(type, anChannel);
                }
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_nType     = in.readObject(10);
            m_anChannel = in.readIntArray(20);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeObject(10, m_nType);
            out.writeIntArray(20, m_anChannel);
            }
        }

    // ----- inner class: InitializeSubscriptionRequest ---------------------

    /**
     * A message to initialize a subscription for a subscriber.
     */
    public static class InitializeSubscriptionRequest
            extends TopicSubscriberRequest
        {
        private boolean m_fForceReconnect;

        private boolean m_fReconnect;

        private boolean m_fDisconnected;

        public InitializeSubscriptionRequest()
            {
            this(null, null, true);
            }

        public InitializeSubscriptionRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        public void setForceReconnect(boolean fForceReconnect)
            {
            m_fForceReconnect = fForceReconnect;
            }

        public void setReconnect(boolean fReconnect)
            {
            m_fReconnect = fReconnect;
            }

        public void setDisconnected(boolean fDisconnected)
            {
            m_fDisconnected = fDisconnected;
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_INITIALIZE_SUBSCRIPTION;
            }

        @Override
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            try
                {
                TopicSubscriberProxy proxy = (TopicSubscriberProxy) getChannel().getReceiver();
                if (proxy.isSimple())
                    {
                    throw new UnsupportedOperationException("this method is not supported for a simple subscriber");
                    }

                ConnectedSubscriber<Binary> subscriber = getSubscriber();

                Position[] aHead = subscriber.getConnector()
                        .initialize(subscriber, m_fForceReconnect, m_fReconnect, m_fDisconnected);

                long       lSubscriptionId = subscriber.getSubscriptionId();
                long       nTimestamp      = subscriber.getConnectionTimestamp();
                response.setResult(new Object[]{lSubscriptionId, nTimestamp, aHead});
                }
            catch (UnsupportedOperationException e)
                {
                response.setFailure(true);
                response.setResult(e);
                }
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            super.readExternal(in);
            m_fForceReconnect = in.readBoolean(10);
            m_fReconnect      = in.readBoolean(11);
            m_fDisconnected   = in.readBoolean(12);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            super.writeExternal(out);
            out.writeBoolean(10, m_fForceReconnect);
            out.writeBoolean(11, m_fReconnect);
            out.writeBoolean(12, m_fDisconnected);
            }
        }

    // ----- inner class: EnsureSubscriptionRequest -------------------------

    /**
     * A message to ensure a subscription for a subscriber.
     */
    public static class EnsureSubscriptionRequest
            extends TopicSubscriberRequest
        {
        private boolean m_fForceReconnect;

        private long m_subscriptionId;

        public EnsureSubscriptionRequest()
            {
            super(null, null, true);
            }

        public EnsureSubscriptionRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        public void setForceReconnect(boolean fForceReconnect)
            {
            m_fForceReconnect = fForceReconnect;
            }

        public void setSubscriptionId(long subscriptionId)
            {
            m_subscriptionId = subscriptionId;
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_ENSURE_SUBSCRIPTION;
            }

        @Override
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            try
                {
                TopicSubscriberProxy proxy = (TopicSubscriberProxy) getChannel().getReceiver();
                if (proxy.isSimple())
                    {
                    throw new UnsupportedOperationException("this method is not supported for a simple subscriber");
                    }

                ConnectedSubscriber<Binary> subscriber = getSubscriber();
                boolean f = subscriber.getConnector()
                        .ensureSubscription(subscriber, m_subscriptionId, m_fForceReconnect);
                response.setResult(f);
                }
            catch (Exception e)
                {
                response.setFailure(true);
                response.setResult(e);
                }
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            super.readExternal(in);
            m_fForceReconnect = in.readBoolean(10);
            m_subscriptionId  = in.readLong(11);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeBoolean(10, m_fForceReconnect);
            out.writeLong(11, m_subscriptionId);
            }
        }

    // ----- inner class: GetSubscriptionRequest ----------------------------

    /**
     * A message to obtain a subscription.
     */
    public static class GetSubscriptionRequest
            extends TopicSubscriberRequest
        {
        private long m_lSubscriptionId;

        public GetSubscriptionRequest()
            {
            super(null, null, true);
            }

        public GetSubscriptionRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        public void setSubscriptionId(long id)
            {
            m_lSubscriptionId = id;
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_GET_SUBSCRIPTION;
            }

        @Override
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            try
                {
                TopicSubscriberProxy proxy = (TopicSubscriberProxy) getChannel().getReceiver();
                if (proxy.isSimple())
                    {
                    throw new UnsupportedOperationException("this method is not supported for a simple subscriber");
                    }

                ConnectedSubscriber<Binary> subscriber = getSubscriber();
                _assert(subscriber != null);
                //noinspection DataFlowIssue
                TopicSubscription subscription = subscriber.getConnector().getSubscription(subscriber, m_lSubscriptionId);
                response.setResult(subscription);
                }
            catch (UnsupportedOperationException e)
                {
                response.setFailure(true);
                response.setResult(e);
                }
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            super.readExternal(in);
            m_lSubscriptionId = in.readLong(10);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeLong(10, m_lSubscriptionId);
            }
        }

    // ----- inner class: GetOwnedChannelsRequest --------------------------

    /**
     * A subscriber request to obtain the channels owned by a subscriber.
     */
    public static class GetOwnedChannelsRequest
            extends TopicSubscriberRequest
        {
        public GetOwnedChannelsRequest()
            {
            super(null, null, true);
            }

        public GetOwnedChannelsRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_GET_OWNED_CHANNELS;
            }

        @Override
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            ConnectedSubscriber<Binary> subscriber = getSubscriber();
            _assert(subscriber != null);
            //noinspection DataFlowIssue
            SortedSet<Integer> channels = subscriber.getConnector().getOwnedChannels(subscriber);
            response.setResult(channels);
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            }
        }

    // ----- inner class: ReceiveRequest ------------------------------------

    /**
     * A subscriber request to receive messages from a topic.
     */
    public static class ReceiveRequest
            extends TopicSubscriberRequest
        {
        private int m_nChannel;

        private int m_cMaxElements;

        public ReceiveRequest()
            {
            this(null, null, true);
            }

        public ReceiveRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        public void setTopicChannel(int nChannel)
            {
            m_nChannel = nChannel;
            }

        public int getTopicChannel()
            {
            return m_nChannel;
            }

        public void setMaxElements(int cMaxElements)
            {
            m_cMaxElements = cMaxElements;
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_RECEIVE;
            }

        @Override
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            TopicSubscriberProxy proxy = (TopicSubscriberProxy) getChannel().getReceiver();
            ConnectedSubscriber<Binary> subscriber = getSubscriber();

            if (proxy.isSimple())
                {
                throw new UnsupportedOperationException("Channel specific receive requests are not supported" +
                        "for simple subscribers");
                }
            subscriber.receive(m_nChannel, m_cMaxElements, new Handler(response)).join();
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_nChannel     = in.readInt(10);
            m_cMaxElements = in.readInt(11);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeInt(10, m_nChannel);
            out.writeInt(11, m_cMaxElements);
            }

        // ----- inner class: Handler ---------------------------------------

        /**
         * A subscriber receive method response handler.
         */
        protected class Handler
                implements SubscriberConnector.ReceiveHandler
            {
            public Handler(com.tangosol.coherence.component.net.extend.message.Response response)
                {
                f_response = response;
                }

            @Override
            public void onReceive(long lVersion, ReceiveResult result, Throwable error, SubscriberConnector.Continuation continuation)
                {
                try
                    {
                    Channel channel = getChannel();
                    _assert(channel != null);

                    ConnectedSubscriber<Binary> subscriber = getSubscriber();
                    Position head = subscriber.getChannelHead(m_nChannel);

                    if (result instanceof PollProcessor.Result)
                        {
                        result = ((PollProcessor.Result) result).toSimpleResult(head);
                        }
                    else
                        {
                        result = new SimpleReceiveResult(result.getElements(),
                                channel.getId(),
                                result.getRemainingElementCount(),
                                result.getStatus(),
                                head);
                        }

                    continuation.onContinue();

                    if (error == null)
                        {
                        f_response.setResult(result);
                        }
                    else
                        {
                        f_response.setFailure(true);
                        f_response.setResult(error);
                        }
                    }
                catch (Throwable t)
                    {
                    f_response.setFailure(true);
                    f_response.setResult(t);
                    }
                }

            // ----- data members -------------------------------------------

            com.tangosol.coherence.component.net.extend.message.Response f_response;
            }
        }

    // ----- inner class: ReceiveRequest ------------------------------------

    /**
     * A subscriber request to receive messages from a topic.
     */
    public static class SimpleReceiveRequest
            extends TopicSubscriberRequest
        {
        /**
         * The maximum number of elements to return.
         */
        private int m_cMaxElements;

        public SimpleReceiveRequest()
            {
            this(null, null, true);
            }

        public SimpleReceiveRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        public void setMaxElements(int cMaxElements)
            {
            m_cMaxElements = cMaxElements;
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_SIMPLE_RECEIVE;
            }

        @Override
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            TopicSubscriberProxy proxy = (TopicSubscriberProxy) getChannel().getReceiver();
            ConnectedSubscriber<Binary> subscriber = getSubscriber();

            if (proxy.isSimple())
                {
                subscriber.receive(m_cMaxElements)
                        .handle((list, err) ->
                            {
                            if (err == null)
                                {
                                Queue<Binary> elements = new LinkedList<>();
                                int nChannel = 0;
                                for (Subscriber.Element<?> element : list)
                                    {
                                    nChannel = element.getChannel();
                                    elements.add(((Subscriber.BinaryElement<?>) element).getRawBinary());
                                    }
                                response.setResult(new SimpleReceiveResult(elements, nChannel,
                                        0, ReceiveResult.Status.Success, PagedPosition.EMPTY_POSITION));
                                }
                            else
                                {
                                response.setResult(err);
                                response.setFailure(true);
                                }
                            return null;
                            })
                        .join();
                }
            else
                {
                throw new UnsupportedOperationException("Simple receive requests are only supported for simple subscribers");
                }
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_cMaxElements = in.readInt(11);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeInt(11, m_cMaxElements);
            }
        }

    // ----- inner class: PeekRequest ---------------------------------------

    /**
     * A subscriber request to peek at a message in a topic.
     */
    public static class PeekRequest
            extends TopicSubscriberRequest
        {
        private int m_nChannel;

        private Position m_position;

        public PeekRequest()
            {
            super(null, null, true);
            }

        public PeekRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        public void setChannel(int nChannel)
            {
            m_nChannel = nChannel;
            }

        public void setPosition(Position position)
            {
            m_position = position;
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_PEEK;
            }

        @Override
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            ConnectedSubscriber<Binary> subscriber = getSubscriber();
            _assert(subscriber != null);
            //noinspection DataFlowIssue
            Subscriber.Element<Binary> element = subscriber.getConnector().peek(m_nChannel, m_position);
            response.setResult(element);
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_nChannel = in.readInt(10);
            m_position = in.readObject(11);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeObject(10, m_nChannel);
            out.writeObject(11, m_position);
            }
        }

    // ----- inner class: CommitRequest -------------------------------------

    /**
     * A subscriber request to commit a position in a channel.
     */
    public static class CommitRequest
            extends TopicSubscriberRequest
        {
        /**
         * The identifier in the response map of the {@link Subscriber.CommitResult}.
         */
        public static final int RESPONSE_ID_RESULT = 0;
        /**
         * The identifier in the response map of the head position.
         */
        public static final int RESPONSE_ID_HEAD = 1;

        /**
         * The channel to commit.
         */
        private int m_nChannel;

        /**
         * The position in the channel to commit.
         */
        private Position m_position;

        /**
         * Default constructor.
         */
        public CommitRequest()
            {
            this(null, null, true);
            }

        /**
         * TDE Component constructor.
         */
        public CommitRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        /**
         * Set the channel to commit.
         *
         * @param nChannel  the channel to commit
         */
        public void setChannel(int nChannel)
            {
            m_nChannel = nChannel;
            }

        /**
         * Set the position in the channel to commit.
         *
         * @param position  the position in the channel to commit
         */
        public void setPosition(Position position)
            {
            m_position = position;
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_COMMIT;
            }

        @Override
        protected void onRun(Response response)
            {
            ConnectedSubscriber<Binary> subscriber = getSubscriber();

            Subscriber.CommitResult result  = subscriber.commitAsync(m_nChannel, m_position).join();
            Position                head    = subscriber.getChannelHead(m_nChannel);
            Object[]                oResult = new Object[2];

            oResult[RESPONSE_ID_RESULT] = result;
            oResult[RESPONSE_ID_HEAD]   = head;
            response.setResult(oResult);
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_nChannel = in.readInt(10);
            m_position = in.readObject(11);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeObject(10, m_nChannel);
            out.writeObject(11, m_position);
            }
        }

    // ----- inner class: IsCommitedRequest ---------------------------------

    /**
     * A subscriber request to determine whether a position in a channel
     * has been committed.
     */
    public static class IsCommitedRequest
            extends TopicSubscriberRequest
        {
        private int m_nChannel;

        private Position m_position;

        public IsCommitedRequest()
            {
            this(null, null, true);
            }

        public IsCommitedRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        public void setChannel(int nChannel)
            {
            m_nChannel = nChannel;
            }

        public void setPosition(Position position)
            {
            m_position = position;
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_IS_COMMITTED;
            }

        @Override
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            ConnectedSubscriber<Binary> subscriber = getSubscriber();
            _assert(subscriber != null);
            //noinspection DataFlowIssue
            SubscriberGroupId groupId    = subscriber.getSubscriberGroupId();
            boolean           fCommitted = subscriber.getConnector().isCommitted(groupId, m_nChannel, m_position);
            response.setResult(fCommitted);
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_nChannel = in.readInt(10);
            m_position = in.readObject(11);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeObject(10, m_nChannel);
            out.writeObject(11, m_position);
            }
        }

    // ----- inner class: GetLastCommitedRequest ----------------------------

    /**
     * A subscriber request to obtain the last committed position.
     */
    public static class GetLastCommitedRequest
            extends TopicSubscriberRequest
        {
        public GetLastCommitedRequest()
            {
            this(null, null, true);
            }

        public GetLastCommitedRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_GET_LAST_COMMITTED;
            }

        @Override
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            ConnectedSubscriber<Binary> subscriber = getSubscriber();
            _assert(subscriber != null);
            //noinspection DataFlowIssue
            SubscriberGroupId      groupId = subscriber.getSubscriberGroupId();
            Map<Integer, Position> map     = subscriber.getConnector().getLastCommittedInGroup(groupId);
            response.setResult(map);
            }
        }

    // ----- inner class: GetHeadsRequest -----------------------------------

    /**
     * A subscriber request to obtain the current head positions.
     */
    public static class GetHeadsRequest
            extends TopicSubscriberRequest
        {
        private int[] m_anChannel;

        public GetHeadsRequest()
            {
            this(null, null, true);
            }

        public GetHeadsRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        public void setChannels(int[] anChannel)
            {
            m_anChannel = anChannel;
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_GET_HEADS;
            }

        @Override
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            ConnectedSubscriber<Binary> subscriber = getSubscriber();
            _assert(subscriber != null);
            //noinspection DataFlowIssue
            Map<Integer, Position> map = subscriber.getConnector().getTopicHeads(m_anChannel);
            response.setResult(map);
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_anChannel = in.readIntArray(10);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeIntArray(10, m_anChannel);
            }
        }

    // ----- inner class: GetTailsRequest -----------------------------------

    /**
     * A subscriber request to obtain the current head positions.
     */
    public static class GetTailsRequest
            extends TopicSubscriberRequest
        {
        public GetTailsRequest()
            {
            super(null, null, true);
            }

        public GetTailsRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_GET_TAILS;
            }

        @Override
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            ConnectedSubscriber<Binary> subscriber = getSubscriber();
            _assert(subscriber != null);
            //noinspection DataFlowIssue
            Map<Integer, Position> map = subscriber.getConnector().getTopicTails();
            response.setResult(map);
            }
        }

    // ----- inner class: SeekRequest ---------------------------------------

    /**
     * A subscriber request to move the head positions.
     */
    public static class SeekRequest
            extends TopicSubscriberRequest
        {
        private Map<Integer, Position> m_mapPosition;

        private Map<Integer, Instant> m_mapTimestamp;

        public SeekRequest()
            {
            this(null, null, true);
            }

        public SeekRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        public void setPositions(Map<Integer, Position> mapPosition)
            {
            m_mapPosition = mapPosition;
            }

        public void setTimestamps(Map<Integer, Instant> mapTimestamp)
            {
            m_mapTimestamp = mapTimestamp;
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_SEEK;
            }

        @Override
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            ConnectedSubscriber<Binary> subscriber = getSubscriber();
            SubscriberConnector<Binary>                      connector  = subscriber.getConnector();

            Map<Integer, SeekResult> mapResult;
            if (m_mapPosition != null && !m_mapPosition.isEmpty())
                {
                mapResult = connector.seekToPosition(subscriber, m_mapPosition);
                }
            else if (m_mapTimestamp != null && !m_mapTimestamp.isEmpty())
                {
                mapResult = connector.seekToTimestamp(subscriber, m_mapTimestamp);
                }
            else
                {
                throw new IllegalArgumentException("Neither the seek position nor timestamp have been set");
                }
            response.setResultAsEntrySet(mapResult.entrySet());
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_mapPosition  = in.readMap(11, new HashMap<>());
            m_mapTimestamp = in.readMap(12, new HashMap<>());
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeMap(11, m_mapPosition);
            out.writeMap(12, m_mapTimestamp, Integer.class, Instant.class);
            }
        }

    // ----- inner class: HeartbeatRequest ----------------------------------

    /**
     * A subscriber request to send a heartbeat.
     */
    public static class HeartbeatRequest
            extends TopicSubscriberRequest
        {
        private boolean m_fAsync;

        public HeartbeatRequest()
            {
            super(null, null, true);
            }

        public HeartbeatRequest(String sName, Component compParent, boolean fInit)
            {
            super(sName, compParent, fInit);
            }

        public void setAsync(boolean f)
            {
            m_fAsync = f;
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_HEARTBEAT;
            }

        @Override
        protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
            {
            ConnectedSubscriber<Binary> subscriber = getSubscriber();
            _assert(subscriber != null);
            //noinspection DataFlowIssue
            subscriber.getConnector().heartbeat(subscriber, m_fAsync);
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_fAsync = in.readBoolean(10);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeBoolean(10, m_fAsync);
            }
        }

    // ----- inner class: SubscriberChannelEvent ----------------------------

    /**
     * A message representing a subscriber channel event.
     */
    public static class SubscriberChannelEvent
            extends Message
        {
        /**
         * The event type.
         */
        private int m_nType;

        /**
         * The event channels.
         */
        private int[] m_anChannel;

        /**
         * The event channels.
         */
        private SortedSet<Integer> m_setChannel;

        // ----- constructors -----------------------------------------------

        public SubscriberChannelEvent()
            {
            this(null, null, true);
            }

        public SubscriberChannelEvent(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            if (fInit)
                {
                __init();
                }
            }

        // ----- accessors ----------------------------------------------------

        /**
         * Return the message type.
         * @return the message type
         */
        public int getType()
            {
            return m_nType;
            }

        /**
         * Set the event type.
         *
         * @param nType the event type
         */
        public void setType(int nType)
            {
            m_nType = nType;
            }

        /**
         * Return the event type.
         *
         * @return the event type
         */
        public SubscriberEvent.Type getEventType()
            {
            return SubscriberEvent.Type.values()[m_nType];
            }

        /**
         * Obtain the channels this event applies to.
         *
         * @return the channels this event applies to
         */
        public int[] getPopulatedChannels()
            {
            return m_anChannel;
            }

        /**
         * Set the event channels
         *
         * @param anChannel  the event channels
         */
        public void setPopulatedChannels(int[] anChannel)
            {
            m_anChannel = anChannel;
            }

        /**
         * Obtain the channels this event applies to.
         *
         * @return the channels this event applies to
         */
        public SortedSet<Integer> getAllocatedChannels()
            {
            return m_setChannel;
            }

        /**
         * Set the event channels
         *
         * @param set  the event channels
         */
        public void setAllocatedChannels(SortedSet<Integer> set)
            {
            m_setChannel = set;
            }

        // ----- Message methods --------------------------------------------

        @Override
        public int getTypeId()
            {
            return TYPE_ID_SUBSCRIBER_EVENT;
            }

        @Override
        public void __init()
            {
            // private initialization
            __initPrivate();
            // signal the end of the initialization
            set_Constructed(true);
            }

        @Override
        protected void __initPrivate()
            {
            super.__initPrivate();
            }

        /**
         * Return a human-readable description of this component.
         *
         * @return a String representation of this component
         */
        @Override
        protected String getDescription()
            {
            return super.getDescription()
                    + ", Type="    + m_nType
                    + ", PopulatedChannels=" + Arrays.toString(m_anChannel)
                    + ", AllocatedChannels=" + m_setChannel;
            }

        @Override
        public void run()
            {
            Channel                    channel = getChannel();
            RemoteSubscriberChannel<?> subscriberChannel = (RemoteSubscriberChannel<?>) channel.getReceiver();
            RemoteSubscriber<?>        subscriber        = subscriberChannel.getSubscriber();
            SubscriberEvent            event             = createEvent();
            subscriber.dispatchEvent(event);
            }

        /**
         * Create a {@link SubscriberEvent}.
         *
         * @return a {@link SubscriberEvent}
         */
        public SubscriberEvent createEvent()
            {
            Channel                    channel           = getChannel();
            RemoteSubscriberChannel<?> subscriberChannel = (RemoteSubscriberChannel<?>) channel.getReceiver();
            RemoteSubscriber<?>        subscriber        = subscriberChannel.getSubscriber();
            SubscriberEvent.Type       type              = SubscriberEvent.Type.values()[m_nType];
            return new SubscriberEvent(subscriber, type, m_anChannel, m_setChannel);
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_nType      = in.readInt(10);
            m_anChannel  = in.readIntArray(11);
            m_setChannel = in.readCollection(12, new TreeSet<>());
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeInt(10, m_nType);
            out.writeIntArray(11, m_anChannel);
            out.writeCollection(12, m_setChannel, Integer.class);
            }
        }

    // ----- inner class: DestroyEvent ------------------------------------------

    /**
     * An event message to signal destruction of a NamedTopic.
     */
    public static class DestroyEvent
            extends Message
        {
        /**
         * Property ScopeName
         * <p>
         * The scope name of the topic service.
         */
        private String __m_ScopeName;

        /**
         * Property TopicName
         * <p>
         * The name of the topic that has been destroyed.
         */
        private String __m_TopicName;

        public DestroyEvent()
            {
            this(null, null, true);
            }

        public DestroyEvent(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);

            if (fInit)
                {
                __init();
                }
            }

        @Override
        protected String getDescription()
            {
            return super.getDescription()
                    + " scope=" + getScopeName()
                    + " topic=" + getTopicName();
            }

        /**
         * Getter for property ScopeName.<p>
         * The scope name of the topic service.
         */
        public String getScopeName()
            {
            return __m_ScopeName;
            }

        /**
         * Getter for property TopicName.<p>
         * The name of the topic that has been destroyed.
         */
        public String getTopicName()
            {
            return __m_TopicName;
            }

        @Override
        public int getTypeId()
            {
            return TYPE_ID_DESTROY_EVENT;
            }

        @Override
        public boolean isExecuteInOrder()
            {
            return true;
            }

        @Override
        public void run()
            {
            throw new UnsupportedOperationException("Implement this method!");
            }

        /**
         * Setter for property ScopeName.<p>
         * The scope name of the topic service.
         */
        public void setScopeName(String sName)
            {
            __m_ScopeName = sName;
            }

        /**
         * Setter for property TopicName.<p>
         * The name of the topic that has been destroyed.
         */
        public void setTopicName(String sName)
            {
            __m_TopicName = sName;
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            setScopeName(in.readString(0));
            setTopicName(in.readString(1));
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);

            out.writeString(0, getScopeName());
            out.writeString(1, getTopicName());
            }
        }
    }
