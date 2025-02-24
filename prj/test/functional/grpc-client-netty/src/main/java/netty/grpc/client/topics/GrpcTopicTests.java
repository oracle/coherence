/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package netty.grpc.client.topics;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.grpc.client.common.topics.GrpcSubscriberConnector;
import com.tangosol.coherence.component.util.safeNamedTopic.SafeSubscriberConnector;
import com.tangosol.coherence.config.Config;
import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.internal.net.topic.NamedTopicSubscriber;
import com.tangosol.internal.net.topic.ReceiveResult;
import com.tangosol.internal.net.topic.SubscriberConnector;
import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.Invocable;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.Binary;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import topics.AbstractNamedTopicTests;
import topics.NamedTopicTests;
import topics.TopicPublisher;
import topics.TopicSubscriber;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class GrpcTopicTests
        extends AbstractNamedTopicTests
    {
    public GrpcTopicTests(String sSerializer)
        {
        super(sSerializer);
        }

    @BeforeClass
    public static void setupClass()
        {
        System.setProperty(LocalStorage.PROPERTY, "false");
        }

    @Before
    public void logStart()
        {
        String sMsg = ">>>>> Starting test: " + m_testWatcher.getMethodName();
        for (CoherenceClusterMember member : cluster.getCluster())
            {
            member.submit(() ->
                {
                System.err.println(sMsg);
                System.err.flush();
                return null;
                }).join();
            }
        }

    @After
    public void logEnd()
        {
        String sMsg = ">>>>> Finished test: " + m_testWatcher.getMethodName();
        for (CoherenceClusterMember member : cluster.getCluster())
            {
            member.submit(() ->
                {
                System.err.println(sMsg);
                System.err.flush();
                return null;
                }).join();
            }
        }

    // ----- test lifecycle methods -----------------------------------------

    @Parameterized.Parameters(name = "serializer={0}")
    public static Collection<Object[]> parameters()
        {
        return Arrays.asList(new Object[][]
            {
            {"pof"}, {"java"}
            });
        }

    @BeforeClass
    public static void setup() throws Exception
        {
        System.setProperty("coherence.topic.publisher.close.timeout", "2s");
        System.setProperty("com.oracle.coherence.common.internal.util.HeapDump.Bug-27585336-tmb-migration", "true");

        String sHost = LocalPlatform.get().getLoopbackAddress().getHostAddress();
        System.setProperty("coherence.localhost", sHost);
        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.cacheconfig", "grpc-topics-client-cache-config.xml");
        }

    // ----- test methods ---------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReceiveSpecifiedNumberOfMessages() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic();
        int                nCount = 10;

        try (Publisher<String>            publisher  = topic.createPublisher(Publisher.OrderBy.id(0));
             NamedTopicSubscriber<String> subscriber = (NamedTopicSubscriber<String>) topic.createSubscriber())
            {
            SubscriberConnector<String> connector = subscriber.getConnector();
            if (connector instanceof SafeSubscriberConnector<String>)
                {
                connector = ((SafeSubscriberConnector<String>) connector).ensureRunningConnector();
                }

            GrpcSubscriberConnector<String> grpcConnector = (GrpcSubscriberConnector<String>) connector;

            publisher.publish("Message 0").get(1, TimeUnit.MINUTES);

            Subscriber.Element<String> element = subscriber.receive().get(1, TimeUnit.MINUTES);
            assertThat(element, is(notNullValue()));
            assertThat(element.getValue(), is("Message 0"));

            int nChannel = element.getChannel();

            for (int i = 0; i < nCount; i++)
                {
                publisher.publish("Message " + i).get(1, TimeUnit.MINUTES);
                }

            Queue<Binary> queue = new LinkedList<>();

            SubscriberConnector.ReceiveHandler handler = (lVersion, result, error, continuation) ->
                {
                queue.addAll(result.getElements());
                if (continuation != null)
                    {
                    continuation.onContinue();
                    }
                };

            grpcConnector.receive(subscriber, nChannel, null, 0L, 2, handler).get(1, TimeUnit.MINUTES);
            assertThat(queue.size(), is(2));

            grpcConnector.receive(subscriber, nChannel, null, 0L, 1, handler).get(1, TimeUnit.MINUTES);
            assertThat(queue.size(), is(3));
            }

        }

    @Test
    public void shouldDestroy() throws Exception
        {
        String             sTopicName = ensureTopicName(m_sSerializer + "-topic-foo");
        NamedTopic<String> topic;
        try
            {
            topic = getSession().getTopic(sTopicName);
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }

        try
            {
            topic.createSubscriber(Subscriber.inGroup("foo"));

            String sMsg = ">>>>> About to destroy subscriber group";
            for (CoherenceClusterMember member : cluster.getCluster())
                {
                member.submit(() ->
                    {
                    System.err.println(sMsg);
                    System.err.flush();
                    return null;
                    }).join();
                }

            topic.destroySubscriberGroup("foo");
            }
        catch (Throwable t)
            {
            throw new RuntimeException(t);
            }
        }

    // ----- helper methods -------------------------------------------------

    @Override
    protected Session getSession()
        {
        return new ConfigurableCacheFactorySession(getECCF(), Classes.getContextClassLoader());
        }

    protected ExtensibleConfigurableCacheFactory getECCF()
        {
        return (ExtensibleConfigurableCacheFactory) cluster
            .createSession(SessionBuilders.extendClient("grpc-topics-client-cache-config.xml"));
        }

    @Override
    protected void runInCluster(RemoteRunnable runnable)
        {
        cluster.getCluster().forEach((member) -> member.submit(runnable));
        }

    @Override
    @SuppressWarnings("unchecked")
    protected <R> R runOnServer(Invocable invocable)
        {
        InvocationService service = (InvocationService) getECCF().ensureService("RemoteInvocation");
        Map<Member, R>    map     = service.query(invocable, null);
        return map.isEmpty() ? null : map.values()
                .stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        }

    @Override
    protected int getStorageMemberCount()
        {
        return STORAGE_MEMBER_COUNT;
        }

    @Override
    protected String getCoherenceCacheConfig()
        {
        return CACHE_CONFIG_FILE;
        }

    // ----- data members ---------------------------------------------------

    public static final int STORAGE_MEMBER_COUNT = 2;

    public static final String CLUSTER_NAME = "RemoteTopicTests";

    public static final String CACHE_CONFIG_FILE = "topic-cache-config.xml";

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(NamedTopicTests.class);

    @ClassRule
    public static CoherenceClusterResource cluster =
            new CoherenceClusterResource()
                    .with(ClusterName.of(CLUSTER_NAME),
                            Logging.at(9),
                            CacheConfig.of(CACHE_CONFIG_FILE),
                            LocalHost.only(),
                            WellKnownAddress.of("127.0.0.1"),
                            JMXManagementMode.ALL,
                            IPv4Preferred.yes(),
                            SystemProperty.of("coherence.topic.publisher.close.timeout", "2s"),
                            SystemProperty.of("coherence.management.remote", "true"),
                            SystemProperty.of("coherence.management.refresh.expiry", "1ms"),
                            SystemProperty.of("com.oracle.coherence.common.internal.util.HeapDump.Bug-27585336-tmb-migration", "true"),
                            SystemProperty.of(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY,
                            Config.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY)))
                    .include(STORAGE_MEMBER_COUNT,
                             CoherenceClusterMember.class,
                             DisplayName.of("storage"),
                             RoleName.of("storage"),
                             s_testLogs.builder());
    }
