/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;
import com.tangosol.net.TopicService;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.util.Filters;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tangosol.net.topic.Subscriber.inGroup;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings({"unchecked", "unused"})
public class PagedTopicMBeanTests
    {
    @BeforeClass
    @SuppressWarnings("resource")
    public static void setup() throws Exception
        {
        System.setProperty("coherence.cluster", "PagedTopicMBeanTests");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.ttl", "0");

        s_coherence  = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);
        s_session    = s_coherence.getSession();
        s_registry   = s_coherence.getCluster().getManagement();
        s_mBeanProxy = s_registry.getMBeanServerProxy();
        }

    @AfterClass
    public static void cleanup()
        {
        Coherence.closeAll();
        }

    @Test
    public void shouldRegisterAndUnregisterTopicMBean()
        {
        String sTopicName = m_testWatcher.getMethodName();

        try (NamedTopic<String> topic = s_session.getTopic(sTopicName))
            {
            String sMBeanName = s_registry.ensureGlobalName(MBeanHelper.getTopicMBeanName(topic));
            assertThat(s_mBeanProxy.getMBeanInfo(sMBeanName), is(notNullValue()));

            topic.destroy();
            assertThat(s_mBeanProxy.getMBeanInfo(sMBeanName), is(nullValue()));
            }
        }

    @Test
    public void shouldRegisterAndUnregisterGroups()
        {
        SubscriberGroupId  groupOneId = SubscriberGroupId.withName("group-one");
        SubscriberGroupId  groupTwoId = SubscriberGroupId.withName("group-two");
        String             sTopicName = m_testWatcher.getMethodName();

        try (NamedTopic<String> topic = s_session.getTopic(sTopicName))
            {
            TopicService service  = topic.getTopicService();
            String       sNameOne = s_registry.ensureGlobalName(MBeanHelper.getSubscriberGroupMBeanName(groupOneId, sTopicName, service));
            String       sNameTwo = s_registry.ensureGlobalName(MBeanHelper.getSubscriberGroupMBeanName(groupTwoId, sTopicName, service));

            topic.ensureSubscriberGroup(groupOneId.getGroupName());
            assertThat(s_mBeanProxy.getMBeanInfo(sNameOne), is(notNullValue()));

            topic.ensureSubscriberGroup(groupTwoId.getGroupName());
            assertThat(s_mBeanProxy.getMBeanInfo(sNameOne), is(notNullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameTwo), is(notNullValue()));

            topic.destroySubscriberGroup(groupOneId.getGroupName());
            assertThat(s_mBeanProxy.getMBeanInfo(sNameOne), is(nullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameTwo), is(notNullValue()));

            topic.destroySubscriberGroup(groupTwoId.getGroupName());
            assertThat(s_mBeanProxy.getMBeanInfo(sNameTwo), is(nullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameOne), is(nullValue()));
            }
        }

    @Test
    public void shouldUnregisterGroupsWhenTopicClosed()
        {
        SubscriberGroupId groupOneId  = SubscriberGroupId.withName("group-one");
        SubscriberGroupId groupTwoId  = SubscriberGroupId.withName("group-two");
        String            sTopicName = m_testWatcher.getMethodName();

        try (NamedTopic<String> topic = s_session.getTopic(sTopicName))
            {
            TopicService service = topic.getTopicService();

            topic.ensureSubscriberGroup(groupOneId.getGroupName());
            topic.ensureSubscriberGroup(groupTwoId.getGroupName());

            String sNameOne = s_registry.ensureGlobalName(MBeanHelper.getSubscriberGroupMBeanName(groupOneId, sTopicName, service));
            String sNameTwo = s_registry.ensureGlobalName(MBeanHelper.getSubscriberGroupMBeanName(groupTwoId, sTopicName, service));

            assertThat(s_mBeanProxy.getMBeanInfo(sNameOne), is(notNullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameTwo), is(notNullValue()));

            topic.destroy();
            assertThat(s_mBeanProxy.getMBeanInfo(sNameOne), is(nullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameTwo), is(nullValue()));
            }
        }

    @Test
    public void shouldRegisterAndUnregisterAnonymousSubscribers()
        {
        String sTopicName = m_testWatcher.getMethodName();
        String sNameOne;
        String sNameTwo;
        String sNameThree;

        try (NamedTopic<String>           topic           = s_session.getTopic(sTopicName);
             PagedTopicSubscriber<String> subscriberOne   = (PagedTopicSubscriber<String>) topic.createSubscriber();
             PagedTopicSubscriber<String> subscriberTwo   = (PagedTopicSubscriber<String>) topic.createSubscriber();
             PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>) topic.createSubscriber())
            {
            sNameOne   = MBeanHelper.getSubscriberMBeanName(subscriberOne);
            sNameTwo   = MBeanHelper.getSubscriberMBeanName(subscriberTwo);
            sNameThree = MBeanHelper.getSubscriberMBeanName(subscriberThree);

            assertThat(s_mBeanProxy.getMBeanInfo(sNameOne), is(notNullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameTwo), is(notNullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameThree), is(notNullValue()));

            subscriberOne.close();
            assertThat(s_mBeanProxy.getMBeanInfo(sNameOne), is(nullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameTwo), is(notNullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameThree), is(notNullValue()));

            subscriberTwo.close();
            assertThat(s_mBeanProxy.getMBeanInfo(sNameOne), is(nullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameTwo), is(nullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameThree), is(notNullValue()));

            // Subscriber three should be unregistered when topic is closed
            topic.close();
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameThree), is(nullValue()));
            }
        }

    @Test
    public void shouldNotRegisterGroupsForAnonymousSubscribers()
        {
        String sTopicName = m_testWatcher.getMethodName();
        try (NamedTopic<String> topic = s_session.getTopic(sTopicName))
            {
            TopicService service = topic.getTopicService();
            try (PagedTopicSubscriber<String> ignored = (PagedTopicSubscriber<String>) topic.createSubscriber())
                {
                String      sPattern = s_registry.ensureGlobalName(MBeanHelper.getSubscriberGroupMBeanName(null, sTopicName, service));
                Set<String> setNames = s_mBeanProxy.queryNames(sPattern, Filters.always());

                assertThat(setNames.isEmpty(), is(true));
                }
            }
        }

    @Test
    public void shouldRegisterAndUnregisterDurableSubscribers()
        {
        String sTopicName = m_testWatcher.getMethodName();

        try (NamedTopic<String>           topic           = s_session.getTopic(sTopicName);
             PagedTopicSubscriber<String> subscriberOne   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("one"));
             PagedTopicSubscriber<String> subscriberTwo   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("one"));
             PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("two")))
            {
            String sNameOne   = MBeanHelper.getSubscriberMBeanName(subscriberOne);
            String sNameTwo   = MBeanHelper.getSubscriberMBeanName(subscriberTwo);
            String sNameThree = MBeanHelper.getSubscriberMBeanName(subscriberThree);

            Set<String> setNames;

            setNames = s_mBeanProxy.queryNames(MBeanHelper.getSubscriberMBeanPattern(topic, false), Filters.always());
            assertThat(setNames.size(), is(3));

            setNames = s_mBeanProxy.queryNames(MBeanHelper.getSubscriberMBeanPattern(topic, subscriberOne.getSubscriberGroupId(), false), Filters.always());
            assertThat(setNames.size(), is(2));

            setNames = s_mBeanProxy.queryNames(MBeanHelper.getSubscriberMBeanPattern(topic, subscriberThree.getSubscriberGroupId(), false), Filters.always());
            assertThat(setNames.size(), is(1));

            assertThat(s_mBeanProxy.getMBeanInfo(sNameOne), is(notNullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameTwo), is(notNullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameThree), is(notNullValue()));

            subscriberOne.close();
            assertThat(s_mBeanProxy.getMBeanInfo(sNameOne), is(nullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameTwo), is(notNullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameThree), is(notNullValue()));

            subscriberTwo.close();
            assertThat(s_mBeanProxy.getMBeanInfo(sNameOne),  is(nullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameTwo),  is(nullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameThree), is(notNullValue()));

            // Subscriber three should be unregistered when topic is closed
            topic.close();
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameThree), is(nullValue()));
            }
        }

    @Test
    public void shouldRegisterGroupsForDurableSubscribers()
        {
        String sTopicName = m_testWatcher.getMethodName();
        String sNameOne;
        String sNameTwo;

        try (NamedTopic<String> topic = s_session.getTopic(sTopicName))
            {
            try (PagedTopicSubscriber<String> subscriberOne   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("one"));
                 PagedTopicSubscriber<String> subscriberTwo   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("one"));
                 PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("two")))
                {
                TopicService service       = topic.getTopicService();
                String       sGroupPattern = s_registry.ensureGlobalName(MBeanHelper.getSubscriberGroupMBeanName(null, sTopicName, service));
                Set<String>  setNames      = s_mBeanProxy.queryNames(sGroupPattern, Filters.always());

                assertThat(setNames.size(), is(2));

                sNameOne = s_registry.ensureGlobalName(MBeanHelper.getSubscriberGroupMBeanName(subscriberOne.getSubscriberGroupId(), sTopicName, service));
                sNameTwo = s_registry.ensureGlobalName(MBeanHelper.getSubscriberGroupMBeanName(subscriberThree.getSubscriberGroupId(), sTopicName, service));

                assertThat(s_mBeanProxy.getMBeanInfo(sNameOne), is(notNullValue()));
                assertThat(s_mBeanProxy.getMBeanInfo(sNameTwo), is(notNullValue()));
                }

            // durable groups should still exist after subscribers have closed
            assertThat(s_mBeanProxy.getMBeanInfo(sNameOne), is(notNullValue()));
            assertThat(s_mBeanProxy.getMBeanInfo(sNameTwo), is(notNullValue()));
            }
        // durable groups should not exist after the topic has closed
        assertThat(s_mBeanProxy.getMBeanInfo(sNameOne), is(nullValue()));
        assertThat(s_mBeanProxy.getMBeanInfo(sNameTwo), is(nullValue()));
        }

    // ----- data members ---------------------------------------------------

    static Coherence s_coherence;

    static Session s_session;

    static Registry s_registry;

    static MBeanServerProxy s_mBeanProxy;

    @Rule(order = 1)
    public TestName m_testWatcher = new TestName();
    }
