/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.common.base.Classes;
import com.tangosol.coherence.config.Config;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.Session;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Subscriber;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author jk 2015.05.28
 */
@RunWith(Parameterized.class)
@SuppressWarnings("unchecked")
public class NamedTopicTests
        extends AbstractNamedTopicTests
    {
    // ----- constructors ---------------------------------------------------

    public NamedTopicTests(String sSerializer)
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

    @Parameterized.Parameters(name = "serializer={0} extend={1}")
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
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldDestroy() throws Exception
        {
        String             sTopicName = ensureTopicName(m_sSerializer + "-topic-foo");
        NamedTopic<String> topic      = getSession().getTopic(sTopicName);

        Subscriber<String> foo = topic.createSubscriber(Subscriber.inGroup("foo"));

        topic.createPublisher().publish("value-1").get(1, TimeUnit.MINUTES);

        CompletableFuture<Subscriber.Element<String>> future1 = foo.receive();
        CompletableFuture<Subscriber.Element<String>> future2 = foo.receive();

        Thread.sleep(5000);

        topic.destroySubscriberGroup("foo");

        future1.handle((v, err) ->
            {
            if (err != null)
                {
                err.printStackTrace();
                }

            return null;
            });

        future2.handle((v, err) ->
            {
            if (err != null)
                {
                err.printStackTrace();
                }

            return null;
            });

       // Thread.sleep(60000);
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
            .createSession(SessionBuilders.storageDisabledMember());
        }

    @Override
    protected void runInCluster(RemoteRunnable runnable)
        {
        cluster.getCluster().forEach((member) -> member.submit(runnable));
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

    // ----- constants ------------------------------------------------------

    public static final int STORAGE_MEMBER_COUNT = 2;

    public static final String CACHE_CONFIG_FILE = "topic-cache-config.xml";

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(NamedTopicTests.class);

    @ClassRule
    public static CoherenceClusterResource cluster =
            new CoherenceClusterResource()
                    .with(ClusterName.of("TopicTests"),
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
                             DisplayName.of("NamedTopicTests"),
                             RoleName.of("storage"),
                             s_testLogs.builder());
    }
