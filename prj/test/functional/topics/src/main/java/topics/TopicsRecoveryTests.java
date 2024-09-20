/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.options.LaunchLogging;
import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;

import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.common.base.NonBlocking;

import com.oracle.coherence.common.util.Threads;
import com.oracle.coherence.testing.junit.ThreadDumpOnTimeoutRule;
import com.tangosol.coherence.component.util.safeService.SafeCacheService;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicDependencies;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.Service;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.TaskDaemon;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tangosol.net.topic.Subscriber.Name.inGroup;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import static org.junit.Assert.fail;

public class TopicsRecoveryTests
    {
    @BeforeClass
    public static void setup() throws Exception
        {
        String sAddress = LocalPlatform.get().getLoopbackAddress().getHostAddress();

        System.setProperty(Logging.PROPERTY_LEVEL, "8");
        System.setProperty(CacheConfig.PROPERTY, CACHE_CONFIG);
        System.setProperty(LocalStorage.PROPERTY, "false");
        System.setProperty("coherence.wka", sAddress);
        System.setProperty("coherence.localhost", sAddress);
        System.setProperty("test.unicast.address", sAddress);
        System.setProperty("test.unicast.port", "0");
        System.setProperty("coherence.ttl", "0");
        }

    @AfterClass
    public static void cleanup()
        {
        Coherence.closeAll();
        }

    @Before
    public void setupTest() throws Exception
        {
        m_nClusterPort = LocalPlatform.get().getAvailablePorts().next();

        System.setProperty("test.multicast.port", String.valueOf(m_nClusterPort));
        m_sClusterName = "TopicsRecoveryTests-" + m_cCluster.getAndIncrement();
        System.setProperty("coherence.cluster", m_sClusterName);

        CoherenceConfiguration config = CoherenceConfiguration.builder()
                .withSession(SessionConfiguration.defaultSession())
                .build();

        s_coherence = Coherence.clusterMember(config);
        s_coherence.start().get(5, TimeUnit.MINUTES);
        s_session = s_coherence.getSession();
        s_count.incrementAndGet();
        }

    @After
    public void cleanupTest()
        {
        Coherence.closeAll();
        CacheFactory.shutdown();
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldPublishAfterServiceRestart() throws Exception
        {
        try
            {
            NamedTopic<Message>         topic        = ensureTopic("binary-test");
            LocalPlatform               platform     = LocalPlatform.get();
            DistributedCacheService     service      = (DistributedCacheService) topic.getService();
            PagedTopicBackingMapManager mgr          = (PagedTopicBackingMapManager) service.getBackingMapManager();
            PagedTopicDependencies      dependencies = mgr.getTopicDependencies(topic.getName());
            int                         cbPage       = dependencies.getPageCapacity();
            int                         cMsgPerPage  = 10;
            int                         cMsgTotal    = cMsgPerPage * service.getPartitionCount() * 3; // lots of pages over all partitions
            int                         cbMessage    = cbPage / cMsgPerPage;
            String                      sMsg         = Base.getRandomString(cbMessage, cbMessage, true);


            try (CoherenceClusterMember member = platform.launch(CoherenceClusterMember.class,
                    ClusterName.of(m_sClusterName),
                    WellKnownAddress.loopback(),
                    LocalHost.loopback(),
                    SystemProperty.of("test.multicast.port", m_nClusterPort),
                    LocalStorage.enabled(),
                    CacheConfig.of(CACHE_CONFIG),
                    IPv4Preferred.autoDetect(),
                    s_testLogs.builder(),
                    DisplayName.of(m_testName.getMethodName())))
                {
                Eventually.assertDeferred(() -> CacheFactory.getCluster().getMemberSet().size(), is(2));
                Eventually.assertDeferred(() -> isTopicServiceRunning(member), is(true));

                // ensure messages are retained by creating a subscriber group
                topic.ensureSubscriberGroup("test");

                try (Publisher<Message> publisher = topic.createPublisher())
                    {
                    int i = 0;
                    System.err.println("Publishing " + (cMsgTotal / 2) + " messages of " + cbMessage + " bytes");
                    for (; i < cMsgTotal / 2; i++)
                        {
                        publisher.publish(new Message(i, sMsg)).get(30, TimeUnit.SECONDS);
                        }
                    System.err.println("Flushing Publisher");
                    publisher.flush().get(5, TimeUnit.MINUTES);

                    restartService(topic);

                    System.err.println("Publishing remaining " + (cMsgTotal - i) + " messages of " + cbMessage + " bytes");
                    for (; i < cMsgTotal; i++)
                        {
                        publisher.publish(new Message(i, sMsg)).get(30, TimeUnit.SECONDS);
                        }
                    System.err.println("Flushing Publisher");
                    publisher.flush().get(5, TimeUnit.MINUTES);

                    // assert that all the expected messages exist in the topic
                    try (Subscriber<Message> subscriber = topic.createSubscriber(inGroup("test")))
                        {
                        for (int m = 0; m < cMsgTotal; m++)
                            {
                            Subscriber.Element<Message> element = subscriber.receive().get(1, TimeUnit.MINUTES);
                            assertThat(element, is(notNullValue()));
                            Message message = element.getValue();
                            assertThat(message, is(notNullValue()));
                            assertThat(message.m_id, is(m));
                            }
                        }
                    }
                }
            }
        catch (TimeoutException e)
            {
            System.err.println("Test timed out: ");
            System.err.println(Threads.getThreadDump(true));
            fail("Test failed with exception: " + e.getMessage());
            }
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldPublishAfterServiceRestartWhenThrottled() throws Exception
        {
        try
            {
            NamedTopic<Message> topic        = ensureTopic("limited-binary-test");
            LocalPlatform       platform     = LocalPlatform.get();
            String              sMsg         = "foo";

            try (CoherenceClusterMember member = platform.launch(CoherenceClusterMember.class,
                    ClusterName.of(m_sClusterName),
                    WellKnownAddress.loopback(),
                    LocalHost.loopback(),
                    LocalStorage.enabled(),
                    SystemProperty.of("test.multicast.port", m_nClusterPort),
                    CacheConfig.of(CACHE_CONFIG),
                    IPv4Preferred.autoDetect(),
                    s_testLogs.builder(),
                    DisplayName.of(m_testName.getMethodName())))
                {
                Eventually.assertDeferred(() -> CacheFactory.getCluster().getMemberSet().size(), is(2));
                Eventually.assertDeferred(() -> isTopicServiceRunning(member), is(true));

                // ensure messages are retained by creating a subscriber group
                topic.ensureSubscriberGroup("test");

                try (Publisher<Message> publisher = topic.createPublisher())
                    {
                    // We need to use a non-blocking try block here otherwise the publish call
                    // would block forever when we're backlogged.
                    try (@SuppressWarnings("unused") NonBlocking nb = new NonBlocking())
                        {
                        // Publish 75 messages - we'll be backlogged very quickly well before 75
                        // Once backlogged the service will be restarted, then we'll read the messages
                        // so becoming un-backlogged and be able to publish more.
                        int cBlocked = 0;
                        for (int i = 0; i < 75 && cBlocked < 2; i++)
                            {
                            publisher.publish(new Message(i, sMsg));
                            try
                                {
                                publisher.flush().get(5, TimeUnit.SECONDS);
                                }
                            catch (TimeoutException _ignored)
                                {
                                cBlocked++;
                                // we're throttled so bounce the cache service...
                                restartService(topic);

                                try (Subscriber<Message> subscriber = topic.createSubscriber(inGroup("test"), Subscriber.CompleteOnEmpty.enabled()))
                                    {
                                    // read everything
                                    Subscriber.Element<Message> element  = subscriber.receive().get(1, TimeUnit.MINUTES);
                                    if (element != null)
                                        {
                                        int      nChannel = element.getChannel();
                                        Position position = null;
                                        while(element != null)
                                            {
                                            position = element.getPosition();
                                            element  = subscriber.receive().get(1, TimeUnit.MINUTES);
                                            }
                                        if (nChannel >= 0)
                                            {
                                            subscriber.commit(nChannel, position);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        catch (TimeoutException e)
            {
            System.err.println("Test timed out: ");
            System.err.println(Threads.getThreadDump(true));
            fail("Test failed with exception: " + e.getMessage());
            }
        }

    @Test
    public void shouldSubscribeWithAnonymousSubscriberAfterServiceRestart() throws Exception
        {
        try
            {
            NamedTopic<Message>         topic        = ensureTopic("binary-test");
            LocalPlatform               platform     = LocalPlatform.get();
            DistributedCacheService     service      = (DistributedCacheService) topic.getService();
            PagedTopicBackingMapManager mgr          = (PagedTopicBackingMapManager) service.getBackingMapManager();
            PagedTopicDependencies      dependencies = mgr.getTopicDependencies(topic.getName());
            int                         cbPage       = dependencies.getPageCapacity();
            int                         cMsgPerPage  = 10;
            int                         cMsgTotal    = cMsgPerPage * service.getPartitionCount() * 3; // lots of pages over all partitions
            int                         cbMessage    = cbPage / cMsgPerPage;
            String                      sMsg         = Base.getRandomString(cbMessage, cbMessage, true);

            try (CoherenceClusterMember member = platform.launch(CoherenceClusterMember.class,
                    ClusterName.of(m_sClusterName),
                    WellKnownAddress.loopback(),
                    LocalHost.loopback(),
                    LocalStorage.enabled(),
                    SystemProperty.of("test.multicast.port", m_nClusterPort),
                    CacheConfig.of(CACHE_CONFIG),
                    IPv4Preferred.autoDetect(),
                    s_testLogs.builder(),
                    LaunchLogging.disabled(),
                    DisplayName.of(m_testName.getMethodName())))
                {
                Eventually.assertDeferred(() -> CacheFactory.getCluster().getMemberSet().size(), is(2));
                Eventually.assertDeferred(() -> isTopicServiceRunning(member), is(true));

                try (Publisher<Message> publisher = topic.createPublisher();
                     Subscriber<Message> subscriber = topic.createSubscriber())
                    {
                    System.err.println("Publishing " + cMsgTotal + " messages of " + cbMessage + " bytes");
                    for (int i = 0; i < cMsgTotal; i++)
                        {
                        publisher.publish(new Message(i, sMsg));
                        }
                    System.err.println("Flushing Publisher");
                    publisher.flush().get(5, TimeUnit.MINUTES);

                    System.err.println("Subscriber receiving " + (cMsgTotal / 2) + " messages of " + cbMessage + " bytes");
                    Subscriber.Element<Message> element = null;
                    int m = 0;
                    for ( ; m < cMsgTotal / 2; m++)
                        {
                        element = subscriber.receive().get(1, TimeUnit.MINUTES);
                        assertThat(element, is(notNullValue()));
                        Message message = element.getValue();
                        assertThat(message, is(notNullValue()));
                        assertThat(message.m_id, is(m));
                        }
                    assertThat(element, is(notNullValue()));
                    System.err.println("Subscriber committing element at " + element.getPosition());
                    assertThat(element.commit().isSuccess(), is(true));

                    // read some more and do not commit - restart should be from the commit
                    System.err.println("Subscriber receiving a further " + (cMsgTotal / 10) + " messages of " + cbMessage + " bytes");
                    for (int i = 0; i < cMsgTotal / 10; i++)
                        {
                        element = subscriber.receive().get(1, TimeUnit.MINUTES);
                        assertThat(element, is(notNullValue()));
                        }

                    restartService(topic);

                    assertThat(((PagedTopicSubscriber<Message>) subscriber).getState(), is(PagedTopicSubscriber.STATE_DISCONNECTED));

                    System.err.println("Subscriber receiving remaining " + (cMsgTotal - m) + " messages of " + cbMessage + " bytes");
                    for ( ; m < cMsgTotal; m++)
                        {
                        element = subscriber.receive().get(1, TimeUnit.MINUTES);
                        assertThat(element, is(notNullValue()));
                        Message message = element.getValue();
                        assertThat(message, is(notNullValue()));
                        assertThat(message.m_id, is(m));
                        }
                    }
                }
            }
        catch (TimeoutException e)
            {
            System.err.println("Test timed out: ");
            System.err.println(Threads.getThreadDump(true));
            fail("Test failed with exception: " + e.getMessage());
            }
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldSubscribeWithGroupSubscriberAfterServiceRestart() throws Exception
        {
        try
            {
            NamedTopic<Message>         topic        = ensureTopic("binary-test");
            LocalPlatform               platform     = LocalPlatform.get();
            DistributedCacheService     service      = (DistributedCacheService) topic.getService();
            PagedTopicBackingMapManager mgr          = (PagedTopicBackingMapManager) service.getBackingMapManager();
            PagedTopicDependencies      dependencies = mgr.getTopicDependencies(topic.getName());
            int                         cbPage       = dependencies.getPageCapacity();
            int                         cMsgPerPage  = 10;
            int                         cMsgTotal    = cMsgPerPage * service.getPartitionCount() * 3; // lots of pages over all partitions
            int                         cbMessage    = cbPage / cMsgPerPage;
            String                      sMsg         = Base.getRandomString(cbMessage, cbMessage, true);

            try (CoherenceClusterMember member = platform.launch(CoherenceClusterMember.class,
                    ClusterName.of(m_sClusterName),
                    WellKnownAddress.loopback(),
                    LocalHost.loopback(),
                    SystemProperty.of("test.multicast.port", m_nClusterPort),
                    LocalStorage.enabled(),
                    CacheConfig.of(CACHE_CONFIG),
                    s_testLogs.builder(),
                    LaunchLogging.disabled(),
                    DisplayName.of(m_testName.getMethodName())))
                {
                Eventually.assertDeferred(() -> CacheFactory.getCluster().getMemberSet().size(), is(2));
                Eventually.assertDeferred(() -> isTopicServiceRunning(member), is(true));

                try (Publisher<Message>  publisher  = topic.createPublisher();
                     Subscriber<Message> subscriber = topic.createSubscriber(inGroup("test")))
                    {
                    System.err.println("Publishing " + cMsgTotal + " messages of " + cbMessage + " bytes");
                    for (int i = 0; i < cMsgTotal; i++)
                        {
                        publisher.publish(new Message(i, sMsg));
                        }
                    System.err.println("Flushing Publisher");
                    publisher.flush().get(5, TimeUnit.MINUTES);

                    System.err.println("Subscriber receiving " + (cMsgTotal / 2) + " messages of " + cbMessage + " bytes");
                    Subscriber.Element<Message> element = null;
                    int m = 0;
                    for ( ; m < cMsgTotal / 2; m++)
                        {
                        element = subscriber.receive().get(1, TimeUnit.MINUTES);
                        assertThat(element, is(notNullValue()));
                        Message message = element.getValue();
                        assertThat(message, is(notNullValue()));
                        assertThat(message.m_id, is(m));
                        }
                    assertThat(element, is(notNullValue()));
                    System.err.println("Subscriber committing element at " + element.getPosition() + " value=" + element.getValue());
                    assertThat(element.commit().isSuccess(), is(true));

                    // read some more and do not commit - restart should be from the commit
                    System.err.println("Subscriber receiving a further " + (cMsgTotal / 10) + " messages of " + cbMessage + " bytes");
                    for (int i = 0; i < cMsgTotal / 10; i++)
                        {
                        element = subscriber.receive().get(1, TimeUnit.MINUTES);
                        assertThat(element, is(notNullValue()));
                        }

                    restartService(topic);

                    assertThat(((PagedTopicSubscriber<Message>) subscriber).getState(), is(PagedTopicSubscriber.STATE_DISCONNECTED));

                    System.err.println("Subscriber receiving remaining " + (cMsgTotal - m) + " messages of " + cbMessage + " bytes");
                    for ( ; m < cMsgTotal; m++)
                        {
                        element = subscriber.receive().get(1, TimeUnit.MINUTES);
                        assertThat(element, is(notNullValue()));
                        Message message = element.getValue();
                        assertThat(message, is(notNullValue()));
                        assertThat(message.m_id, is(m));
                        }
                    }
                }
            }
        catch (TimeoutException e)
            {
            System.err.println("Test timed out: ");
            System.err.println(Threads.getThreadDump(true));
            fail("Test failed with exception: " + e.getMessage());
            }
        }

    @Test
    @SuppressWarnings("unchecked")
    @Ignore("Intermittent due to async EP seeming to get result from a different invocation!")
    public void shouldRecoverFromServiceRestartDuringSubscription() throws Exception
        {
        try
            {
            NamedTopic<Message> topic    = ensureTopic("binary-test");
            LocalPlatform       platform = LocalPlatform.get();
            TaskDaemon          daemon   = new TaskDaemon("test-daemon");

            try (CoherenceClusterMember member = platform.launch(CoherenceClusterMember.class,
                    ClusterName.of(m_sClusterName),
                    WellKnownAddress.loopback(),
                    LocalHost.loopback(),
                    LocalStorage.enabled(),
                    SystemProperty.of("test.multicast.port", m_nClusterPort),
                    CacheConfig.of(CACHE_CONFIG),
                    LaunchLogging.disabled(),
                    s_testLogs.builder(),
                    DisplayName.of(m_testName.getMethodName())))
                {
                Eventually.assertDeferred(() -> CacheFactory.getCluster().getMemberSet().size(), is(2));
                Eventually.assertDeferred(() -> isTopicServiceRunning(member), is(true));

                try (Subscriber<Message> subscriber = topic.createSubscriber(Subscriber.CompleteOnEmpty.enabled()))
                    {
                    int cMessage;

                    try (Publisher<Message> publisher = topic.createPublisher())
                        {
                        PublishTask task = new PublishTask(publisher, 3);
                        daemon.executeTask(task);
                        cMessage = task.onCompletion().get(5, TimeUnit.MINUTES);
                        }

                    SubscribeTask task = new SubscribeTask(subscriber, cMessage);
                    daemon.executeTask(task);
                    // wait until the subscriber in receiving...
                    Eventually.assertDeferred(task::getDistinctReceived, is(greaterThan(100)));

                    restartService(topic);

                    Integer cReceived = task.onCompletion().get(2, TimeUnit.MINUTES);
                    assertThat(cReceived, is(cMessage));
                    }
                }
            }
        catch (TimeoutException e)
            {
            System.err.println("Test timed out: ");
            System.err.println(Threads.getThreadDump(true));
            fail("Test failed with exception: " + e.getMessage());
            }
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldRecoverFromServiceRestartDuringPublishing() throws Exception
        {
        try
            {
            NamedTopic<Message> topic      = ensureTopic("binary-test");
            LocalPlatform       platform   = LocalPlatform.get();
            TaskDaemon          daemon     = new TaskDaemon("test-daemon");

            try (CoherenceClusterMember member = platform.launch(CoherenceClusterMember.class,
                        ClusterName.of(m_sClusterName),
                        WellKnownAddress.loopback(),
                        LocalHost.loopback(),
                        LocalStorage.enabled(),
                        SystemProperty.of("test.multicast.port", m_nClusterPort),
                        CacheConfig.of(CACHE_CONFIG),
                        LaunchLogging.disabled(),
                        s_testLogs.builder(),
                        DisplayName.of(m_testName.getMethodName())))
                {
                Eventually.assertDeferred(() -> CacheFactory.getCluster().getMemberSet().size(), is(2));
                Eventually.assertDeferred(() -> isTopicServiceRunning(member), is(true));

                // create a subscriber group to retain messages
                topic.ensureSubscriberGroup("test");

                System.err.println("Starting test");
                try (Publisher<Message> publisher = topic.createPublisher(Publisher.OrderBy.id(0), Publisher.OnFailure.Continue))
                    {
                    PublishTask task = new PublishTask(publisher, 5);
                    daemon.executeTask(task);
                    // wait until the publisher is publishing...
                    Eventually.assertDeferred(task::getPublishedCount, is(greaterThan(1000)));

                    restartService(topic);

                    // Wait for the publisher task to complete, ignoring errors.
                    // Most of the time we see a CancellationException as a request was in-flight
                    // when the service stopped, but occasionally there is no in-flight request,
                    // and we see the publisher complete successfully
                    task.onCompletion()
                            .handle((count, err) -> null)
                            .get(5, TimeUnit.MINUTES);

                    assertThat(publisher.isActive(), is(true));

                    System.err.println("Publishing task published " + task.getPublishedCount() + " messages");
                    System.err.println("Publishing final message");

                    Boolean fSuccess = publisher.publish(new Message(9999, "foo"))
                            .handle((s, err) ->
                                    {
                                    if (err != null)
                                        {
                                        err.printStackTrace();
                                        Throwable cause = err.getCause();
                                        while (cause != null)
                                            {
                                            err = cause;
                                            cause = err.getCause();
                                            }
                                        System.err.println("Final publish failed " + err.getMessage());
                                        err.printStackTrace();
                                        return false;
                                        }
                                    else
                                        {
                                        System.err.println("Final publish success " + s);
                                        return true;
                                        }
                                    })
                            .get(1, TimeUnit.MINUTES);

                    publisher.flush().get(5, TimeUnit.MINUTES);

                    assertThat(fSuccess, is(true));
                    }
                }
            }
        catch (TimeoutException e)
            {
            System.err.println("Test timed out: ");
            System.err.println(Threads.getThreadDump(true));
            fail("Test failed with exception: " + e.getMessage());
            }
        }

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings("unchecked")
    private <V> NamedTopic<V> ensureTopic(String sPrefix)
        {
        if (m_topic == null)
            {
            m_topic = s_session.getTopic(getCacheName(sPrefix));
            }
        return (NamedTopic<V>) m_topic;
        }

    private boolean isTopicServiceRunning(CoherenceClusterMember member)
        {
        return member.isServiceRunning(TOPIC_SERVICE);
        }

    private String getCacheName(String sPrefix)
        {
        return sPrefix + "-" + s_count.get();
        }

    private void restartService(NamedTopic<?> topic)
        {
        Service service = topic.getService();
        System.err.println("Stopping topics cache service " + service.getInfo().getServiceName());

        Service serviceFinal = service instanceof SafeCacheService
                ? ((SafeCacheService) service).getRunningCacheService()
                : service;

        serviceFinal.stop();
        // wait for DCS to restart the service
        Eventually.assertDeferred(service::isRunning, is(true));

        System.err.println("Restarted topics cache service " + service.getInfo().getServiceName());
        }

    // ----- inner class: Message -------------------------------------------

    public static class Message
            implements ExternalizableLite
        {
        public Message()
            {
            }

        public Message(int id, String sValue)
            {
            m_id     = id;
            m_sValue = sValue;
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_id     = in.readInt();
            m_sValue = ExternalizableHelper.readUTF(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            out.writeInt(m_id);
            ExternalizableHelper.writeUTF(out, m_sValue);
            }

        @Override
        public String toString()
            {
            return "Message(" +
                    "id=" + m_id +
                    ", value='" + m_sValue + '\'' +
                    ')';
            }

        private int m_id;

        private String m_sValue;
        }

    // ----- inner class: PublishTask ---------------------------------------

    static class PublishTask
            implements Runnable
        {
        public PublishTask(Publisher<Message> publisher, int cPagesPerPartition)
            {
            m_publisher          = publisher;
            m_cPagesPerPartition = cPagesPerPartition;
            m_future             = new CompletableFuture<>();
            }

        @Override
        @SuppressWarnings("resource")
        public void run()
            {
            try
                {
                NamedTopic<Message>     topic        = m_publisher.getNamedTopic();
                PagedTopicCaches        caches       = new PagedTopicCaches(topic.getName(), (PagedTopicService) topic.getService(), false);
                PagedTopicDependencies  dependencies = caches.getDependencies();
                int                     cbPage       = dependencies.getPageCapacity();
                int                     cMsgPerPage  = 10;
                int                     cMsgTotal    = cMsgPerPage * caches.getPartitionCount() * m_cPagesPerPartition;
                int                     cbMessage    = cbPage / cMsgPerPage;
                int                     nOnePercent  = cMsgTotal / 100;
                String                  sValue       = Base.getRandomString(cbMessage, cbMessage, true);

                System.err.println("Publishing " + cMsgTotal + " messages of " + cbMessage + " bytes");

                for (int i = 0; i < cMsgTotal; i++)
                    {
                    int nMessage = i;
                    m_publisher.publish(new Message(i, sValue))
                            .handle((status, err) -> m_cPublished =  nMessage + 1);

                    if (i % nOnePercent == 0)
                        {
                        System.err.print(".");
                        System.err.flush();
                        }
                    }
                
                System.err.println("Flushing Publisher");
                m_publisher.flush()
                        .handle((_void, err) -> null)
                        .get(5, TimeUnit.MINUTES); // ensure we publish everything within 5 minutes so the test does not hang with deadlocks if there are bugs anywhere
                m_future.complete(cMsgTotal);
                System.err.println("Publisher Completed");
                }
            catch (Throwable e)
                {
                System.err.println("Publisher failed " + e);
                e.printStackTrace();
                m_future.completeExceptionally(e);
                }
            }

        public CompletableFuture<Integer> onCompletion()
            {
            return m_future;
            }

        public int getPublishedCount()
            {
            return m_cPublished;
            }

        // ----- data members -----------------------------------------------

        private final Publisher<Message> m_publisher;

        private final int m_cPagesPerPartition;

        private final CompletableFuture<Integer> m_future;

        private volatile int m_cPublished = 0;
        }

    // ----- inner class: SubscribeTask -------------------------------------

    static class SubscribeTask
            implements Runnable
        {
        public SubscribeTask(Subscriber<Message> subscriber, int cMessage)
            {
            this(subscriber, cMessage, -1);
            }

        public SubscribeTask(Subscriber<Message> subscriber, int cMessage, int cCommit)
            {
            m_subscriber = subscriber;
            m_cMessage   = cMessage;
            m_cCommit    = Math.max(1, cCommit);
            m_future     = new CompletableFuture<>();
            }

        @Override
        public void run()
            {
            try
                {
                System.err.println("Subscriber receiving " + m_cMessage + " messages");
                int          nLastId        = -1;
                Set<Integer> setKeys        = new HashSet<>();
                int          cTotalReceived = 0;
                int          nOnePercent    = m_cMessage / 100;
                while(true)
                    {
                    Subscriber.Element<Message> element = m_subscriber.receive().get(2, TimeUnit.MINUTES);
                    if (element == null)
                        {
                        break;
                        }

                    if (cTotalReceived % m_cCommit == 0)
                        {
                        element.commitAsync().get(1, TimeUnit.MINUTES);
                        }

                    if (cTotalReceived % nOnePercent == 0)
                        {
                        System.err.print(".");
                        System.err.flush();
                        }

                    cTotalReceived++;
                    m_cReceived = cTotalReceived;
                    Message message = element.getValue();
                    if (setKeys.add(message.m_id))
                        {
                        if (message.m_id - nLastId > 1)
                            {
                            // we've missed a message!
                            fail("Expected message with id " + (nLastId + 1) + " but received " + message);
                            }
                        nLastId = message.m_id;
                        }
                    m_cReceivedDistinct = setKeys.size();
                    }

                m_cReceivedDistinct = setKeys.size();

                System.err.println();
                System.err.println("Subscriber completed - received " + cTotalReceived + " messages, "
                                           + (cTotalReceived - m_cReceivedDistinct) + " duplicates");

                m_future.complete(m_cReceivedDistinct);
                }
            catch (Throwable e)
                {
                m_future.completeExceptionally(e);
                }
            }

        public CompletableFuture<Integer> onCompletion()
            {
            return m_future;
            }

        public int getDistinctReceived()
            {
            return m_cReceivedDistinct;
            }

        public int getReceived()
            {
            return m_cReceived;
            }

        // ----- data members -----------------------------------------------

        private final Subscriber<Message> m_subscriber;

        private final int m_cMessage;

        private final int m_cCommit;

        private volatile int m_cReceivedDistinct = 0;

        private volatile int m_cReceived = 0;

        private final CompletableFuture<Integer> m_future;
        }

    // ----- constants ------------------------------------------------------

    public static final String CACHE_CONFIG = "topics-small-page-config.xml";

    public static final String TOPIC_SERVICE = "PartitionedTopic";

    private static final AtomicInteger s_count = new AtomicInteger();

    // ----- data members ---------------------------------------------------

    @ClassRule
    public static final ThreadDumpOnTimeoutRule timeout = ThreadDumpOnTimeoutRule.after(30, TimeUnit.MINUTES);

    @Rule
    public final TestLogs s_testLogs = new TestLogs(TopicsRecoveryTests.class);

    @Rule
    public final TestName m_testName = new TestName();

    private NamedTopic<?> m_topic;

    private static Coherence s_coherence;

    private static Session s_session;

    private final AtomicInteger m_cCluster = new AtomicInteger();

    public static int m_nClusterPort;

    private String m_sClusterName;
    }
