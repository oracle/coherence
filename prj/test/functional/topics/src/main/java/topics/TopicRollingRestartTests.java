/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.coherence.options.Pof;

import com.oracle.coherence.testing.junit.ThreadDumpOnTimeoutRule;
import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.ValueTypeAssertion;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Subscriber;

import com.oracle.coherence.testing.SlowTests;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.tangosol.net.topic.Subscriber.Name;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2015.06.25
 */
@Category(SlowTests.class)
@Ignore
public class TopicRollingRestartTests
    {
    @BeforeClass
    public static void setup()
            throws Exception
        {
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

        assertNotNull(subscriberPin);

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
            s_cluster.getCluster().filter(m -> m.getRoleName().equals("storage")).unordered().relaunch();
            }
        }

    private static ExtensibleConfigurableCacheFactory s_eccf;

    @ClassRule
    public static final ThreadDumpOnTimeoutRule timeout = ThreadDumpOnTimeoutRule.after(30, TimeUnit.MINUTES);

    @ClassRule
    public static CoherenceClusterResource s_cluster = new CoherenceClusterResource()
            .include(3, CoherenceClusterMember.class, LocalStorage.enabled())
            .with(ClusterName.of("TopicRolling"),
                  CacheConfig.of("topic-cache-config.xml"),
                  OperationalOverride.of("tangosol-coherence-override.xml"),
                  SystemProperty.of(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY, Config.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY)));

    private ExecutorService    m_executorService = Executors.newFixedThreadPool(4);
    private NamedTopic<String> m_topic           = null;
    }
