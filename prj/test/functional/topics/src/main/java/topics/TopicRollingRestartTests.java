/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.testing.junit.ThreadDumpOnTimeoutRule;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.ValueTypeAssertion;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.oracle.coherence.testing.SlowTests;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.tangosol.net.topic.Subscriber.Name;
import static com.tangosol.net.topic.Subscriber.inGroup;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jk 2015.06.25
 */
@SuppressWarnings("unchecked")
@Category(SlowTests.class)
@Ignore
public class TopicRollingRestartTests
    {
    @BeforeClass
    public static void setup()
            throws Exception
        {
        System.setProperty(LocalStorage.PROPERTY, "false");
        s_eccf = (ExtensibleConfigurableCacheFactory) s_cluster.createSession(SessionBuilders.storageDisabledMember());
        }

    @After
    public void cleanup()
            throws Exception
        {
        if (m_topic != null)
            {
            m_topic.destroy();
            m_topic = null;
            }
        }

    @Test
    public void shouldPopulateTopicWhileRollingRestartInProgress()
            throws Exception
        {
        NamedTopic<String> topic           = ensureTopic("pof-test-p");
        int                nCount          = 10000;
        String             sPrefix         = "Element-";
        TopicPublisher     publisher       = new TopicPublisher(topic, sPrefix, nCount, true);
        RollingRestart     restart         = new RollingRestart();
        Subscriber         subscriberPin   = topic.createSubscriber();
        Future<?>          futurePublisher = m_executorService.submit(publisher);
        Future<?>          futureRestart   = m_executorService.submit(restart);

        assertThat(subscriberPin, is(notNullValue()));

        futureRestart.get();
        futurePublisher.get();

        assertThat(publisher.getPublished(), is(nCount));

        TopicAssertions.assertPublishedOrder(topic, nCount, sPrefix);
        }

    @Test
    public void shouldConsumeTopicWhileRollingRestartInProgress()
            throws Exception
        {
        NamedTopic<String> topic       = ensureTopic("pof-test-c");
        int                nCount      = 10000;
        String             sPrefix     = "Element-";
        TopicPublisher     publisher   = new TopicPublisher(topic, sPrefix, nCount, true);
        TopicSubscriber    subscriber1 = new TopicSubscriber(topic.createSubscriber(Name.of("Foo")), sPrefix, nCount, 3, TimeUnit.MINUTES, true);
        TopicSubscriber    subscriber2 = new TopicSubscriber(topic.createSubscriber(Name.of("Bar")), sPrefix, nCount, 3, TimeUnit.MINUTES, true);
        RollingRestart     restart     = new RollingRestart();

        Future<?>          futurePublisher = m_executorService.submit(publisher);

        futurePublisher.get();
        assertThat(publisher.getPublished(), is(nCount));

        try
            {
            TopicAssertions.assertPublishedOrder(topic, nCount, sPrefix);
            }
        catch (Throwable t)
            {
            System.out.println("Ignoring assertion ");
            t.printStackTrace();
            }

        Future<?> futureSubscriber1 = m_executorService.submit(subscriber1);
        Future<?> futureSubscriber2 = m_executorService.submit(subscriber2);
        Future<?> futureRestart   = m_executorService.submit(restart);

        futureRestart.get();
        futureSubscriber1.get();
        futureSubscriber2.get();

        assertThat(subscriber1.getConsumedCount(), is(nCount));
        assertThat(subscriber2.getConsumedCount(), is(nCount));
        }

    @Test
    public void shouldPopulateAndConsumeWhileRollingRestartInProgress()
            throws Exception
        {
        NamedTopic<String> topic = ensureTopic("pof-test-p-and-c");
        int                nCount      = 10000;
        String             sPrefix     = "Element-";
        TopicPublisher     publisher   = new TopicPublisher(topic, sPrefix, nCount, true);
        TopicSubscriber    subscriber1 = new TopicSubscriber(topic.createSubscriber(Name.of("Foo")), sPrefix, nCount, 15, TimeUnit.MINUTES, false);
        TopicSubscriber    subscriber2 = new TopicSubscriber(topic.createSubscriber(Name.of("Bar")), sPrefix, nCount, 15, TimeUnit.MINUTES, false);
        RollingRestart     restart     = new RollingRestart();

        Future<?> futurePublisher   = m_executorService.submit(publisher);
        Future<?> futureSubscriber1 = m_executorService.submit(subscriber1);
        Future<?> futureSubscriber2 = m_executorService.submit(subscriber2);
        Future<?> futureRestart     = m_executorService.submit(restart);

        futureRestart.get();
        futurePublisher.get();
        futureSubscriber1.get();
        futureSubscriber2.get();

        assertThat(publisher.getPublished(), is(nCount));
        assertThat(subscriber1.getConsumedCount(), is(nCount));
        assertThat(subscriber1.getConsumedCount(), is(nCount));
        }

    @Test
    @SuppressWarnings("resource")
    public void shouldHaveCorrectRemainingMessagesAfterRestart() throws Exception
        {
        NamedTopic<String> topic = ensureTopic("pof-test-p-and-c");
        int                nCount      = 10000;
        String             sPrefix     = "Element-";
        RollingRestart     restart     = new RollingRestart();

        try (Publisher<String> publisher = topic.createPublisher(Publisher.OrderBy.roundRobin());
            Subscriber<String> subscriber1 = topic.createSubscriber(inGroup("One"));
            Subscriber<String> subscriber2 = topic.createSubscriber(inGroup("One"));
            Subscriber<String> subscriber3 = topic.createSubscriber(inGroup("Two"));
            Subscriber<String> subscriber4 = topic.createSubscriber(inGroup("Two")))
            {
            Map<Integer, Integer> mapPublished = new HashMap<>();

            for (int i = 0; i < nCount; i++)
                {
                Publisher.Status status = publisher.publish(sPrefix + i).get(30, TimeUnit.SECONDS);
                mapPublished.compute(status.getChannel(), (k, v) -> v == null ? 1 : v + 1);
                }


            int cChannel = topic.getChannelCount();
            int countStart1 = subscriber1.getRemainingMessages();
            int countStart2 = subscriber2.getRemainingMessages();
            int countStart3 = subscriber3.getRemainingMessages();
            int countStart4 = subscriber4.getRemainingMessages();

            assertThat(countStart1 + countStart2, is(nCount));
            assertThat(countStart3 + countStart4, is(nCount));

            int cReceive1 = 10;//s_random.nextInt(100) + 100;
            int cReceive2 = 10;//s_random.nextInt(100) + 100;
            int cReceive3 = 10;//s_random.nextInt(100) + 100;
            int cReceive4 = 10;//s_random.nextInt(100) + 100;

            for (int i = 0; i < cReceive1; i++)
                {
                Subscriber.Element<String> element = subscriber1.receive().get(1, TimeUnit.MINUTES);
                Subscriber.CommitResult    result  = element.commit();
                assertThat(result.getStatus(), is(Subscriber.CommitResultStatus.Committed));
                System.out.println("Committed " + result.getChannel() + " " + result.getPosition());
                }

            for (int i = 0; i < cReceive2; i++)
                {
                Subscriber.Element<String> element = subscriber2.receive().get(1, TimeUnit.MINUTES);
                element.commit();
                }

            for (int i = 0; i < cReceive3; i++)
                {
                Subscriber.Element<String> element = subscriber3.receive().get(1, TimeUnit.MINUTES);
                element.commit();
                }

            for (int i = 0; i < cReceive4; i++)
                {
                Subscriber.Element<String> element = subscriber4.receive().get(1, TimeUnit.MINUTES);
                element.commit();
                }

            int countAfter1 = subscriber1.getRemainingMessages();
            int countAfter2 = subscriber2.getRemainingMessages();
            int countAfter3 = subscriber3.getRemainingMessages();
            int countAfter4 = subscriber4.getRemainingMessages();

            assertThat(countAfter1 + cReceive1, is(countStart1));
            assertThat(countAfter2 + cReceive2, is(countStart2));
            assertThat(countAfter3 + cReceive3, is(countStart3));
            assertThat(countAfter4 + cReceive4, is(countStart4));

            s_cluster.getCluster().filter(m -> m.getRoleName()
                    .equals(STORAGE_ROLE))
                    .unordered()
                    .relaunch(DisplayName.of("StorageRestarted"));


            Eventually.assertDeferred(subscriber1::getRemainingMessages, is(countAfter1));
            Eventually.assertDeferred(subscriber2::getRemainingMessages, is(countAfter2));
            Eventually.assertDeferred(subscriber3::getRemainingMessages, is(countAfter3));
            Eventually.assertDeferred(subscriber4::getRemainingMessages, is(countAfter4));
            }
        }


    public void safeGet(Future<?> future)
        {
        try
            {
            future.get();
            }
        catch (Exception e)
            {
            e.printStackTrace();
            }
        }

    protected synchronized NamedTopic<String> ensureTopic(String sTopicName)
        {
        if (m_topic == null)
            {
            m_topic = s_eccf.ensureTopic(sTopicName, ValueTypeAssertion.withType(String.class));
            }

        return m_topic;
        }

    public class RollingRestart
            implements Runnable
        {
        @Override
        public void run()
            {
            s_cluster.getCluster().filter(m -> m.getRoleName()
                    .equals(STORAGE_ROLE))
                    .unordered()
                    .relaunch(DisplayName.of("StorageRestarted"));
            }
        }

    private static final Random s_random = new Random();

    private static ExtensibleConfigurableCacheFactory s_eccf;

    @ClassRule(order = 0)
    public static final ThreadDumpOnTimeoutRule timeout = ThreadDumpOnTimeoutRule.after(30, TimeUnit.MINUTES);

    @ClassRule(order = 1)
    public static TestLogs m_logs = new TestLogs(TopicRollingRestartTests.class);

    public static final String STORAGE_ROLE = "storage";

    @ClassRule(order = 2)
    public static CoherenceClusterResource s_cluster = new CoherenceClusterResource()
            .include(3, CoherenceClusterMember.class, LocalStorage.enabled())
            .with(ClusterName.of("TopicRolling"),
                    CacheConfig.of("topic-cache-config.xml"),
                    OperationalOverride.of("tangosol-coherence-override.xml"),
                    DisplayName.of("Storage"),
                    RoleName.of(STORAGE_ROLE),
                    m_logs,
                    SystemProperty.of(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY, Config.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY)));

    private ExecutorService    m_executorService = Executors.newFixedThreadPool(4);
    private NamedTopic<String> m_topic           = null;
    }
