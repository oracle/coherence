/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.OptionsByType;

import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.coherence.options.RoleName;

import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.StabilityPredicate;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.ExternalizableHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tangosol.net.topic.Subscriber.Name.inGroup;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;

/**
 * This test verifies that topic publishers and subscribers recover from
 * a total loss of storage. In this case storage has active persistence
 * enabled and is removed via a clean shutdown (as would happen in k8s
 * using the operator).
 */
public class TopicsStorageRecoveryTests
    {
    @BeforeClass
    public static void setup()
        {
        System.setProperty(LocalStorage.PROPERTY, "false");
        System.setProperty(Logging.PROPERTY, "9");
        System.setProperty(OperationalOverride.PROPERTY, "common-tangosol-coherence-override.xml");
        System.setProperty(CacheConfig.PROPERTY, "simple-persistence-bdb-cache-config.xml");

        s_storageCluster = startCluster("initial");

        OptionsByType options = OptionsByType.of(s_options);
        options.add(RoleName.of("client"));
        options.add(LocalStorage.disabled());

        s_ccf = SessionBuilders.storageDisabledMember().build(LocalPlatform.get(), s_storageCluster, options);

        Cluster cluster = CacheFactory.ensureCluster();
        Eventually.assertDeferred(cluster::isRunning, is(true));
        Eventually.assertDeferred(() -> cluster.getMemberSet().size(), is(3));
        }

    @Before
    public void setupTest()
        {
        s_count.incrementAndGet();
        }

    @After
    public void cleanupTest()
        {
        if (m_topic != null)
            {
            try
                {
                m_topic.close();
                }
            catch (Exception e)
                {
                // ignored
                }
            }
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldRecover() throws Exception
        {
        NamedTopic<Message>     topic        = ensureTopic("test");
        String                  sGroup       = "group-one";
        DistributedCacheService service      = (DistributedCacheService) topic.getService();
        Cluster                 cluster      = service.getCluster();
        String                  sServiceName = service.getInfo().getServiceName();

        // the test should not be storage enabled
        assertThat(service.isLocalStorageEnabled(), is(false));

        // create a subscriber group so that published messages are not lost before the subscriber subscribes
        topic.ensureSubscriberGroup(sGroup);

        try (Publisher<Message> publisher = topic.createPublisher())
            {
            AtomicBoolean fPublish    = new AtomicBoolean(true);
            AtomicBoolean fSubscribe  = new AtomicBoolean(true);
            AtomicBoolean fPublishing = new AtomicBoolean(false);
            AtomicBoolean fSubscribed = new AtomicBoolean(false);
            AtomicInteger cPublished  = new AtomicInteger(0);
            AtomicInteger cReceived   = new AtomicInteger(0);

            // A runnable to do the publishing
            Runnable runPublisher = () ->
                {
                try
                    {
                    for (int i = 0; i < 50 && fPublish.get(); i++)
                        {
                        publisher.publish(new Message(i, "Message-" + i))
                                .handle((v, err) ->
                                        {
                                        if (err != null)
                                            {
                                            err.printStackTrace();
                                            }
                                        cPublished.incrementAndGet();
                                        return null;
                                        });
                        if (i > 5)
                            {
                            fPublishing.set(true);
                            }
                        pause();
                        }
                    }
                catch (Throwable t)
                    {
                    Logger.err("Error in publish loop", t);
                    }
                };

            // start the publisher thread
            Thread threadPublish = new Thread(runPublisher, "Test-Publisher");
            threadPublish.start();

            // wait until a few messages have been published
            Eventually.assertDeferred(fPublishing::get, is(true));

            // start the subscriber runnable
            Runnable runSubscriber = () ->
                {
                int cMessage = 0;
                for (int i = 0; i < 10 && fSubscribe.get(); i++)
                    {
                    try (Subscriber<Message> subscriber =  topic.createSubscriber(inGroup(sGroup)))
                        {
                        for (int j = 0; j < 5; j++)
                            {
                            try
                                {
                                Subscriber.Element<Message> element = subscriber.receive().join();
                                element.commit();
                                cMessage++;
                                cReceived.incrementAndGet();
                                if (i >= 5)
                                    {
                                    fSubscribed.set(true);
                                    }
                                }
                            catch (Throwable t)
                                {
                                t.printStackTrace();
                                }
                            }
                        }
                    }
                };

            // start the subscriber thread
            Thread threadSubscribe = new Thread(runSubscriber, "Test-Subscriber");
            threadSubscribe.start();

            // wait until we have received some messages
            Eventually.assertDeferred(fSubscribed::get, is(true));

            // Get one of the storage members
            CoherenceClusterMember member = s_storageCluster.stream().findAny().orElse(null);
            assertThat(member, is(notNullValue()));

            // Suspend the services - we do this via the storage member like the Operator would
            Logger.info(" Suspending service " + sServiceName + " published=" + cPublished.get());
            Boolean fSuspended = member.invoke(() -> suspend(sServiceName));
            assertThat(fSuspended, is(true));
            Logger.info("Suspended service " + sServiceName + " published=" + cPublished.get());

            // shutdown the storage members
            Logger.info("Stopping storage. published=" + cPublished.get());
            s_storageCluster.close();

            // we should eventually have a single cluster member (which is this test JVM)
            Eventually.assertDeferred(() -> cluster.getMemberSet().size(), is(1));
            Logger.info("Stopped storage. published=" + cPublished.get());

            // restart the storage cluster
            System.err.println("Restarting storage. published=" + cPublished.get());
            s_storageCluster = startCluster("restarted");

            // we should eventually have three cluster members
            Eventually.assertDeferred(() -> cluster.getMemberSet().size(), is(3));
            System.err.println("Restarted storage. published=" + cPublished.get());

            // The cache service should still be suspended so resume it via a storage member like the operator would
            Logger.info("Resuming service " + sServiceName + " published=" + cPublished.get());
            member = s_storageCluster.stream().findAny().orElse(null);
            assertThat(member, is(notNullValue()));
            Boolean fResumed = member.invoke(() -> resume(sServiceName));
            assertThat(fResumed, is(true));
            Logger.info("Resumed service " + sServiceName + " published=" + cPublished.get());
            Logger.info("Awake. published=" + cPublished.get());

            // wait for the publisher and subscriber to finish
            threadPublish.join(600000);
            threadSubscribe.join(600000);
            // we should have received at least as many as published (could be more if a commit did not succeed
            // during fail-over, but that is ok)
            assertThat(cReceived.get(), is(lessThanOrEqualTo(cPublished.get())));
            }
        }

    // ----- helper methods -------------------------------------------------

    static Boolean suspend(String sName)
        {
        Logger.info("Suspending service " + sName);
        CacheFactory.ensureCluster().suspendService(sName);
        Logger.info("Suspended service " + sName);
        return true;
        }

    static Boolean resume(String sName)
        {
        Logger.info("Resuming service " + sName);
        CacheFactory.ensureCluster().resumeService(sName);
        Logger.info("Resumed service " + sName);
        return true;
        }

    private void pause()
        {
        try
            {
            Thread.sleep(100);
            }
        catch (InterruptedException e)
            {
            // ignored
            }
        }

    @SuppressWarnings("unchecked")
    private <V> NamedTopic<V> ensureTopic(String sPrefix)
        {
        if (m_topic == null)
            {
            m_topic = s_ccf.ensureTopic(getCacheName("simple-persistent-topic-" + sPrefix));
            }
        return (NamedTopic<V>) m_topic;
        }

    private String getCacheName(String sPrefix)
        {
        return sPrefix + "-" + s_count.get();
        }

    private static CoherenceCluster startCluster(String suffix)
        {
        CoherenceClusterBuilder builder = new CoherenceClusterBuilder();
        OptionsByType options = OptionsByType.of(s_options)
                        .addAll(LocalStorage.enabled(),
                                StabilityPredicate.none(),
                                Logging.at(9),
                                DisplayName.of("storage-" + suffix),
                                s_testLogs.builder());

        builder.include(2, CoherenceClusterMember.class, options.asArray());

        return builder.build(LocalPlatform.get());
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

    // ----- constants ------------------------------------------------------

    private static final AtomicInteger s_count = new AtomicInteger();

    // ----- data members ---------------------------------------------------

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(TopicsRecoveryTests.class);

    private static final OptionsByType s_options = OptionsByType.of(
            SystemProperty.of("coherence.guard.timeout", 60000),
            CacheConfig.of("simple-persistence-bdb-cache-config.xml"),
            OperationalOverride.of("common-tangosol-coherence-override.xml"),
            ClusterName.of(TopicsStorageRecoveryTests.class.getSimpleName())
    );

    private static CoherenceCluster s_storageCluster;

    private static ConfigurableCacheFactory s_ccf;

    private NamedTopic<?> m_topic;
    }
