/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.net.Coherence;
import com.tangosol.net.Member;
import com.tangosol.net.Session;
import com.tangosol.net.TopicService;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.util.Filters;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tangosol.net.topic.Subscriber.inGroup;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "unused"})
public class PagedTopicMBeanTests
    {
    @BeforeClass
    @SuppressWarnings("resource")
    public static void setup() throws Exception
        {
        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.ttl", "0");
        System.setProperty(JMXManagementMode.PROPERTY, "all");
        System.setProperty(LocalStorage.PROPERTY, "false");

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
            // MBean should not be on this member (storage disabled)
            String sMBeanNamePrefix = MBeanHelper.getTopicMBeanName(topic);
            String sLocalMBeanName  = s_registry.ensureGlobalName(sMBeanNamePrefix);
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sLocalMBeanName), is(nullValue()));

            // MBean should be on storage enabled members
            String      sQuery   = sMBeanNamePrefix + ",*";
            Set<String> setNames = s_mBeanProxy.queryNames(sMBeanNamePrefix + ",*", Filters.always());
            String      sDomain  = s_registry.getDomainName() + ":";

            for (CoherenceClusterMember clusterMember : s_cluster.getCluster().getAll("Storage"))
                {
                Member member = mock(Member.class);
                when(member.getId()).thenReturn(clusterMember.getLocalMemberId());

                String sMBeanName = sDomain + s_registry.ensureGlobalName(sMBeanNamePrefix, member);
                Eventually.assertDeferred("MBean should be on member " + clusterMember.getName(),
                                          () -> s_mBeanProxy.getMBeanInfo(sMBeanName), is(notNullValue()));
                }

            // MBean should NOT be on storage disabled members
            for (CoherenceClusterMember clusterMember : s_cluster.getCluster().getAll("NonStorage"))
                {
                Member member = mock(Member.class);
                when(member.getId()).thenReturn(clusterMember.getLocalMemberId());

                String sMBeanName = s_registry.ensureGlobalName(sMBeanNamePrefix, member);
                Eventually.assertDeferred("MBean should not be on member " + clusterMember.getName(),
                                          () -> s_mBeanProxy.getMBeanInfo(sMBeanName), is(nullValue()));
                }

            topic.destroy();

            // MBean should not be on this member
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sLocalMBeanName), is(nullValue()));

            // MBean should not be on any other cluster member
            Eventually.assertDeferred(() -> s_mBeanProxy.queryNames(sQuery, Filters.always()), is(emptyIterable()));
            }
        }

    @Test
    public void shouldRegisterAndUnregisterGroups()  throws Exception
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
            // GroupOne MBean should not be on this member
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameOne), is(nullValue()));
            // GroupOne MBean should be on storage members
            assertSubscriberGroupPresent(groupOneId, sTopicName, service);
            // GroupOne MBean should not be on non-storage members
            assertSubscriberGroupNotPresent(groupOneId, sTopicName, service);

            topic.ensureSubscriberGroup(groupTwoId.getGroupName());
            // GroupTwo MBean should not be on this member
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameTwo), is(nullValue()));
            // GroupTwo MBean should be on storage members
            assertSubscriberGroupPresent(groupTwoId, sTopicName, service);
            // GroupTwo MBean should not be on non-storage members
            assertSubscriberGroupNotPresent(groupTwoId, sTopicName, service);
            // GroupOne MBean should not be on this member
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameOne), is(nullValue()));
            // GroupOne MBean should be on storage members
            assertSubscriberGroupPresent(groupOneId, sTopicName, service);
            // GroupOne MBean should not be on non-storage members
            assertSubscriberGroupNotPresent(groupOneId, sTopicName, service);

            topic.destroySubscriberGroup(groupOneId.getGroupName());
            // GroupOne MBean should be gone
            String      sQuery   = sNameOne + ",*";
            Set<String> setNames = s_mBeanProxy.queryNames(sQuery, Filters.always());
            Eventually.assertDeferred(() -> setNames, is(emptyIterable()));
            // GroupTwo MBean should not be on this member
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameTwo), is(nullValue()));
            // GroupTwo MBean should be on storage members
            assertSubscriberGroupPresent(groupTwoId, sTopicName, service);
            // GroupTwo MBean should not be on non-storage members
            assertSubscriberGroupNotPresent(groupTwoId, sTopicName, service);

            topic.destroySubscriberGroup(groupTwoId.getGroupName());
            // GroupOne MBean should be gone
            Eventually.assertDeferred(() -> s_mBeanProxy.queryNames(sNameOne + ",*", Filters.always()), is(emptyIterable()));
            // GroupTwo MBean should be gone
            Eventually.assertDeferred(() -> s_mBeanProxy.queryNames(sNameOne + ",*", Filters.always()), is(emptyIterable()));
            }
        }

    private void assertSubscriberGroupPresent(SubscriberGroupId groupId, String sTopicName, TopicService service)
        {
        String sMBeanNamePrefix = MBeanHelper.getSubscriberGroupMBeanName(groupId, sTopicName, service);
        String sDomain          = s_registry.getDomainName() + ":";

        for (CoherenceClusterMember clusterMember : s_cluster.getCluster().getAll("Storage"))
            {
            Member member = mock(Member.class);
            when(member.getId()).thenReturn(clusterMember.getLocalMemberId());

            String sMBeanName = sDomain + s_registry.ensureGlobalName(sMBeanNamePrefix, member);
            Eventually.assertDeferred("MBean should be on member " + clusterMember.getName(),
                                      () -> s_mBeanProxy.getMBeanInfo(sMBeanName), is(notNullValue()));
            }
        }

    private void assertSubscriberGroupNotPresent(SubscriberGroupId groupId, String sTopicName, TopicService service)
        {
        String sMBeanNamePrefix = MBeanHelper.getSubscriberGroupMBeanName(groupId, sTopicName, service);
        String sDomain          = s_registry.getDomainName() + ":";

        for (CoherenceClusterMember clusterMember : s_cluster.getCluster().getAll("NonStorage"))
            {
            Member member = mock(Member.class);
            when(member.getId()).thenReturn(clusterMember.getLocalMemberId());

            String sMBeanName = sDomain + s_registry.ensureGlobalName(sMBeanNamePrefix, member);
            Eventually.assertDeferred("MBean should not be on member " + clusterMember.getName(),
                                      () -> s_mBeanProxy.getMBeanInfo(sMBeanName), is(nullValue()));
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

            String sNameOne  = MBeanHelper.getSubscriberGroupMBeanName(groupOneId, sTopicName, service);
            String sNameTwo  = MBeanHelper.getSubscriberGroupMBeanName(groupTwoId, sTopicName, service);
            String sQueryOne = sNameOne + ",*";
            String sQueryTwo = sNameTwo + ",*";

            Eventually.assertDeferred(() -> s_mBeanProxy.queryNames(sQueryOne, Filters.always()), is(not(emptyIterable())));
            Eventually.assertDeferred(() -> s_mBeanProxy.queryNames(sQueryTwo, Filters.always()), is(not(emptyIterable())));

            topic.destroy();

            Eventually.assertDeferred(() -> s_mBeanProxy.queryNames(sQueryOne, Filters.always()), is(emptyIterable()));
            Eventually.assertDeferred(() -> s_mBeanProxy.queryNames(sQueryTwo, Filters.always()), is(emptyIterable()));
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

            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameOne), is(notNullValue()));
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameTwo), is(notNullValue()));
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameThree), is(notNullValue()));

            subscriberOne.close();
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameOne), is(nullValue()));
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameTwo), is(notNullValue()));
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameThree), is(notNullValue()));

            subscriberTwo.close();
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameOne), is(nullValue()));
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameTwo), is(nullValue()));
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameThree), is(notNullValue()));

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
                String sPattern = s_registry.ensureGlobalName(MBeanHelper.getSubscriberGroupMBeanName(null, sTopicName, service));
                Eventually.assertDeferred(() -> s_mBeanProxy.queryNames(sPattern, Filters.always()).isEmpty(), is(true));
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

            String sTopicPattern = MBeanHelper.getSubscriberMBeanPattern(topic, false);
            Eventually.assertDeferred(() -> s_mBeanProxy.queryNames(sTopicPattern, Filters.always()).size(), is(3));

            String sSubscriberPattern = MBeanHelper.getSubscriberMBeanPattern(topic, subscriberOne.getSubscriberGroupId(), false);
            Eventually.assertDeferred(() -> s_mBeanProxy.queryNames(sSubscriberPattern, Filters.always()).size(), is(2));

            String sGroupPattern = MBeanHelper.getSubscriberMBeanPattern(topic, subscriberThree.getSubscriberGroupId(), false);
            Eventually.assertDeferred(() -> s_mBeanProxy.queryNames(sGroupPattern, Filters.always()).size(), is(1));

            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameOne), is(notNullValue()));
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameTwo), is(notNullValue()));
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameThree), is(notNullValue()));

            subscriberOne.close();
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameOne), is(nullValue()));
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameTwo), is(notNullValue()));
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameThree), is(notNullValue()));

            subscriberTwo.close();
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameOne),  is(nullValue()));
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameTwo),  is(nullValue()));
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameThree), is(notNullValue()));

            // Subscriber three should be unregistered when topic is closed
            topic.close();
            Eventually.assertDeferred(() -> s_mBeanProxy.getMBeanInfo(sNameThree), is(nullValue()));
            }
        }

    @Test
    public void shouldRegisterGroupsForDurableSubscribers() throws Exception
        {
        String sTopicName = m_testWatcher.getMethodName();
        String sDomain    = s_registry.getDomainName();

        try (NamedTopic<String> topic = s_session.getTopic(sTopicName))
            {
            TopicService service       = topic.getTopicService();
            String       sGroupPattern = sDomain + ":" + MBeanHelper.getSubscriberGroupMBeanName(null, sTopicName, service) + ",*";

            try (PagedTopicSubscriber<String> subscriberOne   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("one"));
                 PagedTopicSubscriber<String> subscriberTwo   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("one"));
                 PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("two")))
                {
                // Group Mbeans should be on each storage member
                Eventually.assertDeferred(() -> s_mBeanProxy.queryNames(sGroupPattern, Filters.always()).size(), is(STORAGE_ENABLED_COUNT * 2));
                }

            // durable groups should still exist after subscribers have closed
            // Group Mbeans should be on each storage member
            Eventually.assertDeferred(() -> s_mBeanProxy.queryNames(sGroupPattern, Filters.always()).size(), is(STORAGE_ENABLED_COUNT * 2));
            }
        }

    // ----- constants ------------------------------------------------------

    public static final String CLUSTER_NAME = "PagedTopicMBeanTests";

    // ----- data members ---------------------------------------------------

    static Coherence s_coherence;

    static Session s_session;

    static Registry s_registry;

    static MBeanServerProxy s_mBeanProxy;

    @Rule(order = 1)
    public TestName m_testWatcher = new TestName();

    public static final int STORAGE_ENABLED_COUNT = 3;

    @ClassRule(order = 1)
    public static final TestLogs s_tetLogs = new TestLogs(PagedTopicMBeanTests.class);

    @ClassRule(order = 2)
    public static final CoherenceClusterResource s_cluster = new CoherenceClusterResource()
            .with(WellKnownAddress.loopback(),
                     JMXManagementMode.ALL,
                     LocalHost.only(),
                     ClusterName.of(CLUSTER_NAME),
                     IPv4Preferred.yes(),
                     s_tetLogs)
            .include(STORAGE_ENABLED_COUNT, CoherenceClusterMember.class,
                     LocalStorage.enabled(),
                     RoleName.of("storage"),
                     DisplayName.of("Storage"))
            .include(3, CoherenceClusterMember.class,
                     LocalStorage.disabled(),
                     RoleName.of("non-storage"),
                     DisplayName.of("NonStorage"));
    }
