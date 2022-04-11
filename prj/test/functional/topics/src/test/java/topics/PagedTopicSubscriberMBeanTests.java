/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriberMBean;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;
import com.tangosol.net.topic.ManagedSubscriber;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.Filter;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.tangosol.net.topic.Subscriber.completeOnEmpty;
import static com.tangosol.net.topic.Subscriber.inGroup;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
public class PagedTopicSubscriberMBeanTests
    {
    @BeforeClass
    public static void setup()
        {
        Coherence coherence = Coherence.clusterMember().start().join();
        m_session = coherence.getSession();

        m_cluster = CacheFactory.getCluster();
        m_management = m_cluster.getManagement();

        m_proxy = m_management.getMBeanServerProxy();
        }

    @After
    public void cleanup()
        {
        Set<String> set = m_proxy.queryNames(ManagedSubscriber.TYPE_SUBSCRIBER + ",*", AlwaysFilter.INSTANCE());
        set.forEach(m_management::unregister);
        }

    @Test
    public void shouldRegisterAndUnregisterMBean()
        {
        try (NamedTopic<String> topic = m_session.getTopic("test"))
            {
            try (PagedTopicSubscriber<String> subscriber = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("Foo")))
                {
                String sExpected = String.format("Coherence:" + PagedTopicSubscriberMBean.MBEAN_NAME_PATTERN,
                        topic.getName(), "Foo", subscriber.getId());

                Set<String> set = m_proxy.queryNames(ManagedSubscriber.TYPE_SUBSCRIBER + ",*", AlwaysFilter.INSTANCE());
                assertThat(set, is(notNullValue()));
                assertThat(set, contains(sExpected));
                }

            Set<String> set = m_proxy.queryNames(ManagedSubscriber.TYPE_SUBSCRIBER + ",*", AlwaysFilter.INSTANCE());
            assertThat(set, is(notNullValue()));
            assertThat(set.isEmpty(), is(true));
            }
        }

    @Test
    public void shouldRegisterMultipleMBeans() throws Exception
        {
        try (NamedTopic<String> topic = m_session.getTopic("test"))
            {
            try (PagedTopicSubscriber<String> subscriberOne   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("Foo"), completeOnEmpty());
                 PagedTopicSubscriber<String> subscriberTwo   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("Foo"), completeOnEmpty());
                 PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("Bar"), completeOnEmpty());
                 PagedTopicSubscriber<String> subscriberFour  = (PagedTopicSubscriber<String>) topic.createSubscriber(completeOnEmpty());
                 Publisher<String>            publisher       = topic.createPublisher(Publisher.OrderBy.roundRobin()))
                {
                Set<String> set = m_proxy.queryNames(ManagedSubscriber.TYPE_SUBSCRIBER + ",*", AlwaysFilter.INSTANCE());
                assertThat(set, is(notNullValue()));
                assertThat(set.size(), is(4));

                for (int i = 0; i < 1000; i++)
                    {
                    publisher.publish("message-" + i);
                    }

                subscriberOne.receive().get(5, TimeUnit.MINUTES);
                subscriberTwo.receive().get(5, TimeUnit.MINUTES);
                subscriberThree.receive().get(5, TimeUnit.MINUTES);
                subscriberFour.receive().get(5, TimeUnit.MINUTES);

                for (String sName : set)
                    {
                    Map<String, Object> map = m_proxy.getAttributes(sName, AlwaysFilter.INSTANCE());
                    assertThat(map, is(notNullValue()));
                    for (ManagedSubscriber.SubscriberAttribute attribute : ManagedSubscriber.SubscriberAttribute.values())
                        {
                        assertThat(map.containsKey(attribute.name()), is(true));
                        }
                    }
                }
            }
        }

    @Test
    public void shouldGetAttributeBacklog()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);
        long                    nBacklog   = 1234L;

        when(subscriber.getBacklog()).thenReturn(nBacklog);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.Backlog.name());
        assertThat(oValue, is(nBacklog));
        }

    @Test
    public void shouldGetAttributeMaxBacklog()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);
        long                    nBacklog   = 8120L;

        when(subscriber.getMaxBacklog()).thenReturn(nBacklog);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.MaxBacklog.name());
        assertThat(oValue, is(nBacklog));
        }

    @Test
    public void shouldGetAttributeChannelAllocations()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);
        int[]                   aChannel   = {2, 4, 6, 8};

        when(subscriber.getChannels()).thenReturn(aChannel);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.ChannelAllocations.name());
        assertThat(oValue, is(Arrays.toString(aChannel)));
        }

    @Test
    public void shouldGetAttributeNullChannelAllocations()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);

        when(subscriber.getChannels()).thenReturn(null);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.ChannelAllocations.name());
        assertThat(oValue, is("[]"));
        }

    @Test
    public void shouldGetAttributeChannelCount()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);
        int                     cChannel   = m_random.nextInt();

        when(subscriber.getChannelCount()).thenReturn(cChannel);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.ChannelCount.name());
        assertThat(oValue, is(cChannel));
        }

    @Test
    public void shouldGetAttributeDisconnections()
        {
        PagedTopicSubscriber<?> subscriber  = mock(PagedTopicSubscriber.class);
        long                    cDisconnect = m_random.nextLong();

        when(subscriber.getDisconnectCount()).thenReturn(cDisconnect);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.Disconnections.name());
        assertThat(oValue, is(cDisconnect));
        }

    @Test
    public void shouldGetAttributeElements()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);
        long                    cElement   = m_random.nextLong();

        when(subscriber.getElementsPolled()).thenReturn(cElement);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.Elements.name());
        assertThat(oValue, is(cElement));
        }

    @Test
    public void shouldGetAttributeId()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);
        long                    nId        = 12345L;

        when(subscriber.getId()).thenReturn(nId);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.Id.name());
        assertThat(oValue, is(nId));
        }

    @Test
    public void shouldGetAttributeNotifications()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);
        long                    cNotify    = m_random.nextLong();

        when(subscriber.getNotify()).thenReturn(cNotify);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.Notifications.name());
        assertThat(oValue, is(cNotify));
        }

    @Test
    public void shouldGetAttributePolls()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);
        long                    cPolls     = m_random.nextLong();

        when(subscriber.getPolls()).thenReturn(cPolls);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.Polls.name());
        assertThat(oValue, is(cPolls));
        }

    @Test
    public void shouldGetAttributeReceivedCompletions()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);
        long                    count      = m_random.nextLong();

        when(subscriber.getReceived()).thenReturn(count);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.ReceiveCompletions.name());
        assertThat(oValue, is(count));
        }

    @Test
    public void shouldGetAttributeReceivedEmpty()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);
        long                    count      = m_random.nextLong();

        when(subscriber.getReceivedEmpty()).thenReturn(count);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.ReceiveEmpty.name());
        assertThat(oValue, is(count));
        }

    @Test
    public void shouldGetAttributeReceivedErrors()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);
        long                    count      = m_random.nextLong();

        when(subscriber.getReceivedError()).thenReturn(count);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.ReceiveErrors.name());
        assertThat(oValue, is(count));
        }

    @Test
    public void shouldGetAttributeState()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);
        int                     nState     = PagedTopicSubscriber.STATE_CONNECTED;

        when(subscriber.getState()).thenReturn(nState);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.State.name());
        assertThat(oValue, is(nState));
        }

    @Test
    public void shouldGetAttributeStateName()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);
        String                  sState     = PagedTopicSubscriber.STATES[0];

        when(subscriber.getStateName()).thenReturn(sState);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.StateName.name());
        assertThat(oValue, is(sState));
        }

    @Test
    public void shouldGetAttributeFilter()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);
        Filter                  filter     = new EqualsFilter<>("foo", "bar");

        when(subscriber.getFilter()).thenReturn(filter);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.Filter.name());
        assertThat(oValue, is(filter.toString()));
        }

    @Test
    public void shouldGetAttributeConverter()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);
        Function                converter  = x -> x;

        when(subscriber.getConverter()).thenReturn(converter);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.Converter.name());
        assertThat(oValue, is(converter.toString()));
        }

    @Test
    public void shouldGetAttributeSerializer()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);
        Serializer              serializer = new DefaultSerializer();

        when(subscriber.getSerializer()).thenReturn(serializer);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.Serializer.name());
        assertThat(oValue, is(serializer.toString()));
        }

    @Test
    public void shouldGetAttributeCompleteOnEmpty()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);

        when(subscriber.isCompleteOnEmpty()).thenReturn(true);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.CompleteOnEmpty.name());
        assertThat(oValue, is(true));
        }

    @Test
    public void shouldGetAttributeSubscriberGroup()
        {
        PagedTopicSubscriber<?> subscriber = mock(PagedTopicSubscriber.class);
        String                  sGroup     = "Foo";
        SubscriberGroupId       groupId    = new SubscriberGroupId(sGroup);

        String sName  = registerMockMBean(subscriber, groupId);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.SubscriberGroup.name());
        assertThat(oValue, is(sGroup));
        }

    @Test
    public void shouldGetAttributeChannels()
        {
        PagedTopicSubscriber<?>      subscriber = mock(PagedTopicSubscriber.class);
        Subscriber.Channel channel0    = mock(Subscriber.Channel.class);
        Subscriber.Channel channel1    = mock(Subscriber.Channel.class);
        int                          cChannel    = 2;

        when(subscriber.getChannelCount()).thenReturn(cChannel);
        when(subscriber.getChannel(0)).thenReturn(channel0);
        when(subscriber.getChannel(1)).thenReturn(channel1);

        when(channel0.isOwned()).thenReturn(true);
        when(channel1.isOwned()).thenReturn(false);

        String sName  = registerMockMBean(subscriber);
        Object oValue = m_proxy.getAttribute(sName, ManagedSubscriber.SubscriberAttribute.Channels.name());
        assertThat(oValue, is(instanceOf(TabularData.class)));

        TabularData tabularData = (TabularData) oValue;
        assertThat(tabularData.size(), is(cChannel));

        CompositeData row0 = tabularData.get(new Object[]{0});
        assertThat(row0, is(notNullValue()));

        CompositeData row1 = tabularData.get(new Object[]{1});
        assertThat(row1, is(notNullValue()));

        assertThat(row0.get(ManagedSubscriber.ChannelAttribute.Owned.name()), is(true));
        assertThat(row1.get(ManagedSubscriber.ChannelAttribute.Owned.name()), is(false));
        }

    @Test
    public void shouldInvokeHeads() throws Exception
        {
        try (NamedTopic<String> topic = m_session.getTopic("test"))
            {
            try (PagedTopicSubscriber<String> subscriber = (PagedTopicSubscriber<String>) topic.createSubscriber(completeOnEmpty()))
                {
                String sMBeanName = String.format("Coherence:" + PagedTopicSubscriberMBean.MBEAN_NAME_PATTERN,
                        topic.getName(), ManagedSubscriber.ANONYMOUS_GROUP, subscriber.getNotificationId());

                subscriber.receive().get(1, TimeUnit.MINUTES);

                Map<Integer, Position> mapHeads = subscriber.getHeads();

                Object oValue = m_proxy.invoke(sMBeanName, ManagedSubscriber.SubscriberOperation.Heads.name(), NO_PARAMS, NO_SIGNATURE);
                assertThat(oValue, is(instanceOf(TabularData.class)));

                TabularData tabularData = (TabularData) oValue;
                assertThat(tabularData.size(), is(mapHeads.size()));

                for (Map.Entry<Integer, Position> entry : mapHeads.entrySet())
                    {
                    CompositeData compositeData = tabularData.get(new Object[]{entry.getKey()});
                    assertThat(compositeData, is(notNullValue()));
                    assertThat(compositeData.get(ManagedSubscriber.ChannelAttribute.Channel.name()), is(entry.getKey()));
                    assertThat(compositeData.get("Position"), is(String.valueOf(entry.getValue())));
                    }
                }
            }
        }

    @Test
    public void shouldInvokeRemainingMessages() throws Exception
        {
        try (NamedTopic<String> topic = m_session.getTopic("test"))
            {
            try (PagedTopicSubscriber<String> subscriber = (PagedTopicSubscriber<String>) topic.createSubscriber(completeOnEmpty());
                 Publisher<String> publisher = topic.createPublisher(Publisher.OrderBy.roundRobin()))
                {
                String sMBeanName = String.format("Coherence:" + PagedTopicSubscriberMBean.MBEAN_NAME_PATTERN,
                        topic.getName(), ManagedSubscriber.ANONYMOUS_GROUP, subscriber.getNotificationId());

                subscriber.receive().get(1, TimeUnit.MINUTES);

                for (int i = 0; i < 100; i++)
                    {
                    publisher.publish("test").get(1, TimeUnit.MINUTES);
                    }

                int cChannel = topic.getChannelCount();

                Object oValue = m_proxy.invoke(sMBeanName, ManagedSubscriber.SubscriberOperation.RemainingMessages.name(), NO_PARAMS, NO_SIGNATURE);
                assertThat(oValue, is(instanceOf(TabularData.class)));

                TabularData tabularData = (TabularData) oValue;
                assertThat(tabularData.size(), is(cChannel));

                for (int nChannel = 0; nChannel < cChannel; nChannel++)
                    {
                    CompositeData compositeData = tabularData.get(new Object[]{nChannel});
                    assertThat(compositeData, is(notNullValue()));
                    assertThat(compositeData.get(ManagedSubscriber.ChannelAttribute.Channel.name()), is(nChannel));
                    int cExpected = subscriber.getRemainingMessages(nChannel);
                    assertThat(compositeData.get("Count"), is(cExpected));
                    }
                }
            }
        }

    @Test
    public void shouldInvokeDisconnectAndReconnect()
        {
        try (NamedTopic<String> topic = m_session.getTopic("test"))
            {
            try (PagedTopicSubscriber<String> subscriber = (PagedTopicSubscriber<String>) topic.createSubscriber(completeOnEmpty()))
                {
                String sMBeanName = String.format("Coherence:" + PagedTopicSubscriberMBean.MBEAN_NAME_PATTERN,
                        topic.getName(), ManagedSubscriber.ANONYMOUS_GROUP, subscriber.getNotificationId());

                Long cDisconnectBefore = (Long) m_proxy.getAttribute(sMBeanName, ManagedSubscriber.SubscriberAttribute.Disconnections.name());

                Object oValue = m_proxy.invoke(sMBeanName, ManagedSubscriber.SubscriberOperation.Disconnect.name(), NO_PARAMS, NO_SIGNATURE);
                assertThat(oValue, is(nullValue()));

                assertThat(subscriber.isDisconnected(), is(true));

                Long cDisconnectAfter = (Long) m_proxy.getAttribute(sMBeanName, ManagedSubscriber.SubscriberAttribute.Disconnections.name());

                assertThat(cDisconnectAfter - cDisconnectBefore, is(1L));

                oValue = m_proxy.invoke(sMBeanName, ManagedSubscriber.SubscriberOperation.Connect.name(), NO_PARAMS, NO_SIGNATURE);
                assertThat(oValue, is(nullValue()));

                assertThat(subscriber.isDisconnected(), is(false));
                }
            }
        }

    @Test
    public void shouldInvokeNotifyPopulated()
        {
        try (NamedTopic<String> topic = m_session.getTopic("test"))
            {
            try (PagedTopicSubscriber<String> subscriber = (PagedTopicSubscriber<String>) topic.createSubscriber())
                {
                String sMBeanName = String.format("Coherence:" + PagedTopicSubscriberMBean.MBEAN_NAME_PATTERN,
                        topic.getName(), ManagedSubscriber.ANONYMOUS_GROUP, subscriber.getNotificationId());

                subscriber.receive();

                long cChannel = subscriber.getChannelCount();
                Eventually.assertDeferred(subscriber::getPolls, is(cChannel));

                long cPollsBefore = subscriber.getPolls();
                for (int i = 0; i < cChannel; i++)
                    {
                    Subscriber.Channel channel = subscriber.getChannel(i);
                    assertThat(channel, is(notNullValue()));
                    assertThat(channel.isEmpty(), is(true));
                    }

                int    nChannel       = 5;
                long   cNotify        = subscriber.getNotify();
                long   cChannelNotify = ((PagedTopicSubscriber.PagedTopicChannel) subscriber.getChannel(nChannel)).getNotify();

                Object oValue   = m_proxy.invoke(sMBeanName, ManagedSubscriber.SubscriberOperation.NotifyPopulated.name(), new Object[]{nChannel}, new String[]{Integer.class.getName()});

                assertThat(oValue, is(nullValue()));

                Eventually.assertDeferred(subscriber::getNotify, is(greaterThan(cNotify)));
                Eventually.assertDeferred(() -> ((PagedTopicSubscriber.PagedTopicChannel) subscriber.getChannel(nChannel)).getNotify(), is(greaterThan(cChannelNotify)));

                Eventually.assertDeferred(subscriber::getPolls, is(cPollsBefore + 1));
                }
            }
        }

    // ----- helper methods -------------------------------------------------


    private String registerMockMBean(PagedTopicSubscriber<?> subscriber)
        {
        String            sGroup  = "Testing";
        SubscriberGroupId groupId = new SubscriberGroupId(sGroup);
        return registerMockMBean(subscriber, groupId);
        }

    @SuppressWarnings("rawtypes")
    private String registerMockMBean(PagedTopicSubscriber<?> subscriber, SubscriberGroupId id)
        {
        when(subscriber.getSubscriberGroupId()).thenReturn(id);

        NamedTopic topic = mock(NamedTopic.class);
        when(topic.getName()).thenReturn("Test");
        when(subscriber.getNamedTopic()).thenReturn(topic);
        
        PagedTopicSubscriberMBean mBean = new PagedTopicSubscriberMBean(subscriber);
        return mBean.register(m_management);
        }

    // ----- constants ------------------------------------------------------

    private static final Object[] NO_PARAMS = new Object[0];

    private static final String[] NO_SIGNATURE = new String[0];

    // ----- data members ---------------------------------------------------

    private static Cluster m_cluster;

    private static Registry m_management;

    private static Session m_session;

    private static MBeanServerProxy m_proxy;

    private static final Random m_random = new Random(System.currentTimeMillis());
    }
