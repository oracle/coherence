/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.junit.CoherenceBuilder;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.callables.IsCoherenceRunning;
import com.oracle.bedrock.runtime.coherence.callables.IsServiceRunning;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.features.JmxFeature;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.MavenProjectFileUtils;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.collections.ConcurrentHashMap;
import com.oracle.coherence.common.util.Threads;
import com.oracle.coherence.testing.junit.ThreadDumpOnTimeoutRule;
import com.tangosol.internal.net.topic.NamedTopicPublisher;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.ExternalizableHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import topics.callables.GetTopicServiceName;

import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tangosol.internal.net.topic.NamedTopicSubscriber.withIdentifyingName;
import static com.tangosol.net.topic.Subscriber.Name.inGroup;
import static com.tangosol.net.topic.Subscriber.completeOnEmpty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;

/**
 * This test verifies that topic publishers and subscribers recover from
 * a total loss of storage. In this case storage has active persistence
 * enabled and is removed via a clean shutdown (as would happen in k8s
 * using the operator).
 */
@SuppressWarnings({"CallToPrintStackTrace", "resource"})
public abstract class AbstractTopicsStorageRecoveryTests
    {
    @Before
    public void setupTest()
        {
        String sMethodName = m_testName.getMethodName();
        System.err.println(">>>> Entering setupTest() " + sMethodName);

        String clientCacheConfig = getClientConfig();

        System.setProperty(ClusterName.PROPERTY, sMethodName);
        System.setProperty(LocalStorage.PROPERTY, "false");
        System.setProperty(WellKnownAddress.PROPERTY, "127.0.0.1");
        System.setProperty("test.log.level", "9");
        System.setProperty("test.log", "stderr");
        System.setProperty(OperationalOverride.PROPERTY, "common-tangosol-coherence-override.xml");
        System.setProperty("coherence.distributed.partitioncount", "13");
        System.setProperty(CacheConfig.PROPERTY, clientCacheConfig);

        // make sure persistence files are not left from a previous test
        File filePersistence = new File("target/store-bdb-active/" + sMethodName);
        if (filePersistence.exists())
            {
            MavenProjectFileUtils.recursiveDelete(filePersistence);
            }

        System.err.println("Starting cluster for " + sMethodName);
        s_storageCluster = startCluster("initial");

        Eventually.assertDeferred(s_storageCluster::isReady, is(true));
        System.err.println("Cluster for " + sMethodName + " is ready");
        OptionsByType options = OptionsByType.of();
        options.add(RoleName.of("client"));
        options.add(LocalStorage.disabled());
        options.add(CacheConfig.of(clientCacheConfig));

        System.err.println("Starting local Coherence instance for " + sMethodName);

        CoherenceBuilder builder = getCoherenceBuilder();

        s_coherence = builder.build(LocalPlatform.get(), s_storageCluster, options);
        s_session   = s_coherence.getSession();
        System.err.println("Started local Coherence instance for " + sMethodName);

        assertCoherenceReady(s_coherence);

        s_count.incrementAndGet();
        System.err.println(">>>> Exiting setupTest() " + sMethodName);
        }

    protected abstract String getClientConfig();

    protected abstract CoherenceBuilder getCoherenceBuilder();

    protected abstract void assertCoherenceReady(Coherence coherence);

    @After
    public void cleanupTest()
        {
        String sMethodName = m_testName.getMethodName();
        System.err.println(">>>> Entering cleanupTest() " + sMethodName);
        closeSafely(m_topic);
        closeSafely(s_storageCluster);
        s_storageCluster = null;
        closeSafely(s_coherence);
        s_coherence = null;
        shutdown();
        System.err.println(">>>> Exiting cleanupTest() " + sMethodName);
        }

    private void shutdown()
        {
        Coherence.closeAll();
        CacheFactory.shutdown();
        }

    private void closeSafely(AutoCloseable closeable)
        {
        if (closeable != null)
            {
            try
                {
                closeable.close();
                }
            catch (Exception e)
                {
                // ignored
                }
            }
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldRecoverAfterCleanStorageRestart() throws Exception
        {
        NamedTopic<Message> topic  = ensureTopic("test");
        String              sGroup = "group-one";

        // create a subscriber group so that published messages are not lost before the subscriber subscribes
        topic.ensureSubscriberGroup(sGroup);

        String sServiceName = s_storageCluster.getAny().invoke(new GetTopicServiceName(topic.getName()));

        Publisher<Message> publisher = topic.createPublisher(Publisher.OrderBy.roundRobin(),
                Publisher.OnFailure.Continue, NamedTopicPublisher.ChannelCount.of(10));
        try
            {
            AtomicBoolean fPublish    = new AtomicBoolean(true);
            AtomicBoolean fSubscribe  = new AtomicBoolean(true);
            AtomicBoolean fPublishing = new AtomicBoolean(false);
            AtomicBoolean fPublished  = new AtomicBoolean(false);
            AtomicBoolean fSubscribed = new AtomicBoolean(false);
            AtomicInteger cPublished  = new AtomicInteger(0);
            AtomicInteger cReceived   = new AtomicInteger(0);
            int           cPubMax     = 101;

            Map<Message, Subscriber.Element<Message>> mapReceived = new ConcurrentHashMap<>();
            Map<Message, Publisher.Status> mapPublished = new ConcurrentHashMap<>();

            // A runnable to do the publishing
            Runnable runPublisher = () ->
                {
                try
                    {
                    for (int i = 0; i < cPubMax && fPublish.get(); i++)
                        {
                        Message message = new Message(i, "Message-" + i);
                        publisher.publish(message)
                                .handle((status, err) ->
                                        {
                                        if (err != null)
                                            {
                                            err.printStackTrace();
                                            }
                                        else
                                            {
                                            cPublished.incrementAndGet();
                                            mapPublished.put(message, status);
                                            }
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
                    Logger.err("Error in publish loop");
                    Logger.err(t);
                    }
                fPublished.set(true);
                };

            // start the publisher thread
            Thread threadPublish = new Thread(runPublisher, "Test-Publisher");
            threadPublish.start();

            // wait until a few messages have been published
            Eventually.assertDeferred(fPublishing::get, is(true));

            // start the subscriber runnable
            Runnable runSubscriber = () ->
                {
                try
                    {
                    int cSubscriber = 20;
                    for (int i = 0; i <= cSubscriber && fSubscribe.get(); i++)
                        {
                        String sName = "sub-" + i;

                        if (i == cSubscriber)
                            {
                            Logger.info("Waiting to for publisher to complete before creating subscriber " + sName);
                            Eventually.assertDeferred(fPublished::get, is(true));
                            }

                        boolean fPublisherFinished = fPublished.get();
                        Subscriber.Option<Message, Message> optComplete = fPublisherFinished
                                ? completeOnEmpty() : Subscriber.Option.nullOption();

                        Logger.info("Creating subscriber " + sName + " fPublisherFinished=" + fPublisherFinished + " published=" + cPublished.get() + " received=" + cReceived.get());
                        try (Subscriber<Message> subscriber =  topic.createSubscriber(inGroup(sGroup),
                                optComplete, withIdentifyingName(sName)))
                            {
                            int nMax = (i == cSubscriber ? cPubMax : 5);
                            Logger.info("Created subscriber " + sName + " " + subscriber);
                            for (int j = 0; j < nMax; j++)
                                {
                                CompletableFuture<Subscriber.Element<Message>> future = null;
                                try
                                    {
                                    future = subscriber.receive();
                                    Subscriber.Element<Message> element = future.get(10, TimeUnit.SECONDS);
                                    if (element != null)
                                        {
                                        mapReceived.put(element.getValue(), element);
                                        element.commit();
                                        cReceived.incrementAndGet();
                                        if (i >= 5)
                                            {
                                            fSubscribed.set(true);
                                            }
                                        }
                                    }
                                catch (Throwable t)
                                    {
                                    if (future != null && !future.isDone())
                                        {
                                        if (!future.cancel(true))
                                            {
                                            // the future has actually completed
                                            if (future.isDone() && !future.isCompletedExceptionally())
                                                {
                                                Subscriber.Element<Message> element = future.get();
                                                if (element != null)
                                                    {
                                                    mapReceived.put(element.getValue(), element);
                                                    element.commit();
                                                    cReceived.incrementAndGet();
                                                    if (i >= 5)
                                                        {
                                                        fSubscribed.set(true);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    if (!(t instanceof TimeoutException))
                                        {
                                        t.printStackTrace();
                                        }
                                    }
                                }
                            Logger.info("Closing subscriber " + sName + " " + subscriber);
                            }
                        catch (Throwable t)
                            {
                            Logger.info("Exiting subscriber " + sName + " " + t);
                            }
                        Logger.info("Closed subscriber " + sName);
                        }
                    }
                catch (Throwable t)
                    {
                    Logger.info("Exiting subscriber thread with error " + t);
                    }
                Logger.info("Exiting subscriber thread.");
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
            Logger.info(">>>>  Suspending service " + sServiceName + " published=" + cPublished.get());
            suspendService(sServiceName);
            Logger.info(">>>> Suspended service " + sServiceName + " published=" + cPublished.get());

            // shutdown the storage members
            restartCluster();

            IsServiceRunning   isRunning    = new IsServiceRunning(sServiceName);
            IsCoherenceRunning isCohRunning = new IsCoherenceRunning();
            for (CoherenceClusterMember m : s_storageCluster)
                {
                Eventually.assertDeferred(() -> m.invoke(isCohRunning), is(true));
                Eventually.assertDeferred(() -> m.invoke(isRunning), is(true));
                }
            Logger.info(">>>> Restarted service " + sServiceName + " on all members");

            // The cache service should still be suspended so resume it via a storage member like the operator would
            Logger.info(">>>> Resuming service " + sServiceName + " published=" + cPublished.get());
            resumeService(sServiceName);
            Logger.info(">>>> Resumed service " + sServiceName + " published=" + cPublished.get());
            Logger.info(">>>> Awake. published=" + cPublished.get());

            // wait for the publisher and subscriber to finish
            threadPublish.join(600000);
            threadSubscribe.join(120000);
            // we should have received at least as many as published (could be more if a commit did not succeed
            // during fail-over and the messages was re-received, but that is ok)
            int count = mapReceived.size();
            Logger.info(">>>> Test complete: published=" + cPublished.get() + " received=" + cReceived.get() + " map=" + count);
            if (count != cPublished.get())
                {
                TreeSet<Message> setKey = new TreeSet<>(mapPublished.keySet());
                for (Message message : setKey)
                    {
                    System.err.println(mapPublished.get(message) + " " + mapReceived.get(message));
                    }
                }
            assertThat(count, greaterThanOrEqualTo(cPublished.get()));
            }
        finally
            {
            CoherenceClusterMember member = s_storageCluster.stream().findAny().orElse(null);
            assertThat(member, is(notNullValue()));
            resumeService(sServiceName);
            CompletableFuture.runAsync(publisher::close).get(1, TimeUnit.MINUTES);
            }
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldRecoverWaitingSubscriberAfterCleanStorageRestart() throws Exception
        {
        NamedTopic<Message> topic       = ensureTopic("test-three");
        String              sGroup       = "group-one";
        String              sServiceName = s_storageCluster.getAny().invoke(new GetTopicServiceName(topic.getName()));

        // create a subscriber group so that published messages are not lost before the subscriber subscribes
        topic.ensureSubscriberGroup(sGroup);

        try (Subscriber<Message> subscriberOne   = topic.createSubscriber(inGroup(sGroup));
             Subscriber<Message> subscriberTwo   = topic.createSubscriber(inGroup(sGroup));
             Subscriber<Message> subscriberThree = topic.createSubscriber(inGroup(sGroup)))
            {
            // topic is empty, futures will not complete yet...
            CompletableFuture<Subscriber.Element<Message>> futureOne = subscriberOne.receive();
            CompletableFuture<Subscriber.Element<Message>> futureTwo = subscriberTwo.receive();

            // Get one of the storage members
            CoherenceClusterMember member = s_storageCluster.stream().findAny().orElse(null);
            assertThat(member, is(notNullValue()));

            // wait some time to ensure the subscribers are waiting on empty topics
            Thread.sleep(10000);
            
            // Suspend the services - we do this via the storage member like the Operator would
            Logger.info(">>>>  Suspending service " + sServiceName);
            suspendService(sServiceName);
            Logger.info(">>>> Suspended service " + sServiceName);

            // futures should not be completed
            assertThat(futureOne.isDone(), is(false));
            assertThat(futureTwo.isDone(), is(false));
            
            // shutdown the storage members
            restartCluster();

            IsServiceRunning   isRunning    = new IsServiceRunning(sServiceName);
            IsCoherenceRunning isCohRunning = new IsCoherenceRunning();
            for (CoherenceClusterMember m : s_storageCluster)
                {
                Eventually.assertDeferred(() -> m.invoke(isCohRunning), is(true));
                Eventually.assertDeferred(() -> m.invoke(isRunning), is(true));
                }
            Logger.info(">>>> Restarted service " + sServiceName + " on all members");

            // futures should not be completed
            assertThat(futureOne.isDone(), is(false));
            assertThat(futureTwo.isDone(), is(false));

            // The topics cache service should still be suspended so resume it via a storage member like the operator would
            Logger.info(">>>> Resuming service " + sServiceName);
            resumeService(sServiceName);
            Logger.info(">>>> Resumed service " + sServiceName);

            // futures should not be completed
            assertThat(futureOne.isDone(), is(false));
            assertThat(futureTwo.isDone(), is(false));

            // subscriber from third subscriber
            CompletableFuture<Subscriber.Element<Message>> futureThree = subscriberThree.receive();

            int cChannel;
            try (Publisher<Message> publisher = topic.createPublisher(Publisher.OrderBy.roundRobin()))
                {
                // publish to every channel to ensure the subscriber receive a message
                cChannel = publisher.getChannelCount();
                Logger.info(">>>> Publishing " + cChannel + " messages");
                for (int i = 0; i < cChannel; i++)
                    {
                    Message message = new Message(i, "Message-" + i);
                    publisher.publish(message).get(1, TimeUnit.MINUTES);
                    Logger.info(">>>> Published " + message);
                    }
                }

            Eventually.assertDeferred(futureOne::isDone, is(true));
            Eventually.assertDeferred(futureTwo::isDone, is(true));
            Eventually.assertDeferred(futureThree::isDone, is(true));

            assertThat(futureOne.isCompletedExceptionally(), is(false));
            assertThat(futureTwo.isCompletedExceptionally(), is(false));
            assertThat(futureThree.isCompletedExceptionally(), is(false));

            List<Integer> listChannel = new ArrayList<>();
            Arrays.stream(subscriberOne.getChannels()).forEach(listChannel::add);
            Arrays.stream(subscriberTwo.getChannels()).forEach(listChannel::add);
            Arrays.stream(subscriberThree.getChannels()).forEach(listChannel::add);
            assertThat(listChannel.size(), is(cChannel));
            assertThat(listChannel.stream().distinct().count(), is((long) cChannel));
            }
        }

    // ----- helper methods -------------------------------------------------

    protected void suspendService(String sServiceName) throws Exception
        {
        CoherenceCluster       cluster    = s_storageCluster;
        CoherenceClusterMember member     = cluster.getAny();
        JmxFeature             jmxFeature = member.get(JmxFeature.class);
        assertThat(jmxFeature, is(notNullValue()));

        MBeanServerConnection connection = jmxFeature.getDeferredJMXConnector().get().getMBeanServerConnection();
        ObjectName            objectName = new ObjectName("Coherence:type=Cluster");
        MBeanInfo             info       = connection.getMBeanInfo(objectName);
        assertThat(info, is(notNullValue()));

        connection.invoke(objectName, "suspendService", new Object[]{sServiceName}, new String[]{"java.lang.String"});
        }

    protected void resumeService(String sServiceName) throws Exception
        {
        CoherenceCluster       cluster    = s_storageCluster;
        CoherenceClusterMember member     = cluster.getAny();
        JmxFeature             jmxFeature = member.get(JmxFeature.class);
        assertThat(jmxFeature, is(notNullValue()));

        MBeanServerConnection connection = jmxFeature.getDeferredJMXConnector().get().getMBeanServerConnection();
        ObjectName            objectName = new ObjectName("Coherence:type=Cluster");
        MBeanInfo             info       = connection.getMBeanInfo(objectName);
        assertThat(info, is(notNullValue()));

        connection.invoke(objectName, "resumeService", new Object[]{sServiceName}, new String[]{"java.lang.String"});
        }


    protected void restartCluster()
        {
        Logger.info(">>>> Stopping storage.");
        s_storageCluster.close();
        Logger.info(">>>> Restarting storage.");
        s_storageCluster = startCluster("restarted");
        Logger.info(">>>> Restarted storage.");
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
            int cTopic = f_cTopic.getAndIncrement();
            m_topic = s_session.getTopic(getCacheName("simple-persistent-topic-" + sPrefix + "-" + cTopic));
            }
        return (NamedTopic<V>) m_topic;
        }

    private String getCacheName(String sPrefix)
        {
        return sPrefix + "-" + s_count.get();
        }

    private CoherenceCluster startCluster(String suffix)
        {
        CoherenceClusterBuilder builder     = new CoherenceClusterBuilder();
        String                  sMethodName = m_testName.getMethodName();
        OptionsByType           options     = OptionsByType.of()
                                                .addAll(CacheConfig.of("simple-persistence-bdb-cache-config.xml"),
                                                        LocalStorage.enabled(),
                                                        StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()),
                                                        Logging.atMax(),
                                                        JMXManagementMode.ALL,
                                                        DisplayName.of(sMethodName + '-' + suffix),
                                                        JmxFeature.enabled(),
                                                        s_testLogs.builder());

        builder.with(ClusterName.of(sMethodName),
                    SystemProperty.of("coherence.guard.timeout", 60000),
                    CacheConfig.of("simple-persistence-bdb-cache-config.xml"),
//                    OperationalOverride.of("common-tangosol-coherence-override.xml"),
                    Logging.atMax(),
                    SystemProperty.of("coherence.distributed.partitioncount", 13),
                    OperationalOverride.of("common-tangosol-coherence-override.xml"),
                    SystemProperty.of("test.log.level", 9),
                    SystemProperty.of("test.log", "stderr"),
                    WellKnownAddress.loopback())
               .include(2, CoherenceClusterMember.class, options.asArray());

        return builder.build(LocalPlatform.get());
        }

    // ----- inner class: Message -------------------------------------------

    public static class Message
            implements ExternalizableLite, PortableObject, Comparable<Message>
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
        public void readExternal(PofReader in) throws IOException
            {
            m_id = in.readInt(0);
            m_sValue = in.readString(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeInt(0, m_id);
            out.writeString(1, m_sValue);
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
        public int compareTo(Message o)
            {
            int n = Integer.compare(m_id, o.m_id);
            if (n == 0)
                {
                n = m_sValue.compareTo(o.m_sValue);
                }
            return n;
            }

        @Override
        public String toString()
            {
            return "Message(" +
                    "id=" + m_id +
                    ", value='" + m_sValue + '\'' +
                    ')';
            }

        @Override
        public boolean equals(Object o)
            {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Message message = (Message) o;
            return m_id == message.m_id && Objects.equals(m_sValue, message.m_sValue);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(m_id, m_sValue);
            }

        private int m_id;

        private String m_sValue;
        }

    // ----- constants ------------------------------------------------------

    private static final AtomicInteger s_count = new AtomicInteger();

    // ----- data members ---------------------------------------------------

    @Rule
    public final TestName m_testName = new TestName();

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(TopicsRecoveryTests.class);

    protected static volatile CoherenceCluster s_storageCluster;

    /**
     * A JUnit rule that will cause the test to fail if it runs too long.
     * A thread dump will be generated on failure.
     */
    @ClassRule
    public static final ThreadDumpOnTimeoutRule timeout = ThreadDumpOnTimeoutRule.after(30, TimeUnit.MINUTES, () ->
            s_storageCluster.forEach(member -> member.invoke(() ->
                                                 {
                                                 System.err.println(Threads.getThreadDump(true));
                                                 return null;
                                                 })));

    private static Coherence s_coherence;

    private static Session s_session;

    private final AtomicInteger f_cTopic = new AtomicInteger(0);

    private NamedTopic<?> m_topic;
    }
