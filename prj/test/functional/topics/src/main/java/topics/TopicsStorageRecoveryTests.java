/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.OptionsByType;

import com.oracle.bedrock.junit.CoherenceBuilder;

import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.callables.IsServiceRunning;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.coherence.options.RoleName;

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

import com.tangosol.internal.net.topic.impl.paged.PagedTopicPublisher;
import com.tangosol.io.ExternalizableLite;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Coherence;
import com.tangosol.net.DistributedCacheService;

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
import topics.callables.ResumeService;
import topics.callables.SuspendService;

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

import static com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber.withIdentifyingName;
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
@SuppressWarnings("CallToPrintStackTrace")
public class TopicsStorageRecoveryTests
    {
    @Before
    public void setupTest()
        {
        String sMethodName = m_testName.getMethodName();
        System.err.println(">>>> Entering setupTest() " + sMethodName);
        shutdown();

        System.setProperty(ClusterName.PROPERTY, sMethodName);
        System.setProperty(LocalStorage.PROPERTY, "false");
        System.setProperty("test.log.level", "9");
        System.setProperty("test.log", "stderr");
        System.setProperty(OperationalOverride.PROPERTY, "common-tangosol-coherence-override.xml");
        System.setProperty(CacheConfig.PROPERTY, "simple-persistence-bdb-cache-config.xml");

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

        OptionsByType options = OptionsByType.of(s_options);
        options.add(ClusterName.of(sMethodName));
        options.add(RoleName.of("client"));
        options.add(LocalStorage.disabled());

        System.err.println("Starting local Coherence instance for " + sMethodName);
        s_coherence = CoherenceBuilder.clusterMember().build(LocalPlatform.get(), s_storageCluster, options);
        s_session   = s_coherence.getSession();
        System.err.println("Started local Coherence instance for " + sMethodName);

        Cluster cluster = CacheFactory.ensureCluster();
        Eventually.assertDeferred(cluster::isRunning, is(true));
        System.err.println("Waiting for local Coherence instance to join cluster " + sMethodName);
        Eventually.assertDeferred(() -> cluster.getMemberSet().size(), is(3), Timeout.after(5, TimeUnit.MINUTES));

        s_count.incrementAndGet();
        System.err.println(">>>> Exiting setupTest() " + sMethodName);
        }

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
        NamedTopic<Message>     topic        = ensureTopic("test");
        String                  sGroup       = "group-one";
        DistributedCacheService service      = (DistributedCacheService) topic.getService();
        Cluster                 cluster      = service.getCluster();
        String                  sServiceName = service.getInfo().getServiceName();

        // the test should not be storage enabled
        assertThat(service.isLocalStorageEnabled(), is(false));

        // create a subscriber group so that published messages are not lost before the subscriber subscribes
        topic.ensureSubscriberGroup(sGroup);

        try (Publisher<Message> publisher = topic.createPublisher(Publisher.OrderBy.roundRobin(), PagedTopicPublisher.ChannelCount.of(10)))
            {
            AtomicBoolean fPublish    = new AtomicBoolean(true);
            AtomicBoolean fSubscribe  = new AtomicBoolean(true);
            AtomicBoolean fPublishing = new AtomicBoolean(false);
            AtomicBoolean fPublished  = new AtomicBoolean(false);
            AtomicBoolean fSubscribed = new AtomicBoolean(false);
            AtomicInteger cPublished  = new AtomicInteger(0);
            AtomicInteger cReceived   = new AtomicInteger(0);

            Map<Message, Subscriber.Element<Message>> mapReceived = new ConcurrentHashMap<>();
            Map<Message, Publisher.Status> mapPublished = new ConcurrentHashMap<>();

            // A runnable to do the publishing
            Runnable runPublisher = () ->
                {
                try
                    {
                    for (int i = 0; i < 100 && fPublish.get(); i++)
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
                    Logger.err("Error in publish loop", t);
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
                int cSubscriber = 20;
                for (int i = 0; i <= cSubscriber && fSubscribe.get(); i++)
                    {
                    String sName = "sub-" + i;

                    if (i == cSubscriber)
                        {
                        System.err.println("Waiting to for publisher to complete before creating subscriber " + sName);
                        Eventually.assertDeferred(fPublished::get, is(true));
                        }

                    boolean fPublisherFinished = fPublished.get();
                    Subscriber.Option<Message, Message> optComplete = fPublisherFinished
                            ? completeOnEmpty() : Subscriber.Option.nullOption();

                    System.err.println("Creating subscriber " + sName + " fPublisherFinished=" + fPublisherFinished + " published=" + cPublished.get() + " received=" + cReceived.get());
                    try (Subscriber<Message> subscriber =  topic.createSubscriber(inGroup(sGroup),
                            optComplete, withIdentifyingName(sName)))
                        {
                        System.err.println("Created subscriber " + sName + " " + subscriber);
                        for (int j = 0; j < 5; j++)
                            {
                            CompletableFuture<Subscriber.Element<Message>> future = null;
                            try
                                {
                                future = subscriber.receive();
                                Subscriber.Element<Message> element = future.get(1, TimeUnit.MINUTES);
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
                                    future.cancel(true);
                                    }
                                if (!(t instanceof TimeoutException))
                                    {
                                    t.printStackTrace();
                                    }
                                }
                            }
                        System.err.println("Closing subscriber " + sName + " " + subscriber);
                        }
                    System.err.println("Closed subscriber " + sName);
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
            Boolean fSuspended = member.invoke(new SuspendService(sServiceName));
            assertThat(fSuspended, is(true));
            Logger.info("Suspended service " + sServiceName + " published=" + cPublished.get());

            // shutdown the storage members
            Logger.info("Stopping storage. published=" + cPublished.get());
            s_storageCluster.close();

            // we should eventually have a single cluster member (which is this test JVM)
            Eventually.assertDeferred(() -> cluster.getMemberSet().size(), is(1));
            Logger.info("Stopped storage. published=" + cPublished.get());

            // restart the storage cluster
            Logger.info("Restarting storage. published=" + cPublished.get());
            s_storageCluster = startCluster("restarted");

            // we should eventually have three cluster members
            Eventually.assertDeferred(() -> cluster.getMemberSet().size(), is(3));
            Logger.info("Restarted storage. published=" + cPublished.get());

            IsServiceRunning isRunning = new IsServiceRunning(sServiceName);
            for (CoherenceClusterMember m : s_storageCluster)
                {
                Eventually.assertDeferred(() -> m.invoke(isRunning), is(true));
                }
            Logger.info("Restarted service " + sServiceName + " on all members");

            // The cache service should still be suspended so resume it via a storage member like the operator would
            Logger.info("Resuming service " + sServiceName + " published=" + cPublished.get());
            member = s_storageCluster.stream().findAny().orElse(null);
            assertThat(member, is(notNullValue()));
            Boolean fResumed = member.invoke(new ResumeService(sServiceName));
            assertThat(fResumed, is(true));
            Logger.info("Resumed service " + sServiceName + " published=" + cPublished.get());
            Logger.info("Awake. published=" + cPublished.get());

            // wait for the publisher and subscriber to finish
            threadPublish.join(600000);
            threadSubscribe.join(120000);
            // we should have received at least as many as published (could be more if a commit did not succeed
            // during fail-over and the messages was re-received, but that is ok)
            int count = mapReceived.size();
            Logger.info("Test complete: published=" + cPublished.get() + " received=" + cReceived.get() + " map=" + count);
            if (count != cPublished.get())
                {
                TreeSet<Message> setKey = new TreeSet<>(mapPublished.keySet());
                for (Message message : setKey)
                    {
                    System.err.println(mapPublished.get(message) + " " + mapReceived.get(message));
                    }
                }
            assertThat(count, is(cPublished.get()));
            }
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldRecoverAfterStorageRestart() throws Exception
        {
        NamedTopic<Message>     topic        = ensureTopic("test-two");
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
                try (Subscriber<Message> subscriber = topic.createSubscriber(inGroup(sGroup)))
                    {
                    while (fSubscribe.get())
                        {
                        try
                            {
                            Subscriber.Element<Message> element = subscriber.receive().get(30, TimeUnit.SECONDS);
                            element.commit();
                            int c = cReceived.incrementAndGet();
                            if (c >= 5)
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
                };

            // start the subscriber thread
            Thread threadSubscribe = new Thread(runSubscriber, "Test-Subscriber");
            threadSubscribe.start();

            // wait until we have received some messages
            Eventually.assertDeferred(fSubscribed::get, is(true));

            // Get one of the storage members
            CoherenceClusterMember member = s_storageCluster.stream().findAny().orElse(null);
            assertThat(member, is(notNullValue()));

            // shutdown the storage members
            Logger.info("Stopping storage. published=" + cPublished.get());
            s_storageCluster.close();

            // we should eventually have a single cluster member (which is this test JVM)
            Eventually.assertDeferred(() -> cluster.getMemberSet().size(), is(1));
            Logger.info("Stopped storage. published=" + cPublished.get());

            // restart the storage cluster
            Logger.info("Restarting storage. published=" + cPublished.get());
            s_storageCluster = startCluster("restarted");

            // we should eventually have three cluster members
            Eventually.assertDeferred(() -> cluster.getMemberSet().size(), is(3));
            Logger.info("Restarted storage. published=" + cPublished.get());

            IsServiceRunning isRunning = new IsServiceRunning(sServiceName);
            for (CoherenceClusterMember m : s_storageCluster)
                {
                Eventually.assertDeferred(() -> m.invoke(isRunning), is(true));
                }
            Logger.info("Restarted service " + sServiceName + " on all members");

            // wait for the publisher and subscriber to finish
            threadPublish.join(600000);

            // should eventually have received all the messages
            Eventually.assertDeferred(cReceived::get, is(greaterThanOrEqualTo(cPublished.get())));

            // stop the subscriber
            fSubscribe.set(false);
            threadSubscribe.join(120000);

            // we should have received at least as many as published (could be more if a commit did not succeed
            // during fail-over and the messages was re-received, but that is ok)
            Logger.info("Test complete: published=" + cPublished.get() + " received=" + cReceived.get());
            }
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldRecoverWaitingSubscriberAfterCleanStorageRestart() throws Exception
        {
        NamedTopic<Message>     topic        = ensureTopic("test-three");
        String                  sGroup       = "group-one";
        DistributedCacheService service      = (DistributedCacheService) topic.getService();
        Cluster                 cluster      = service.getCluster();
        String                  sServiceName = service.getInfo().getServiceName();

        // the test should not be storage enabled
        assertThat(service.isLocalStorageEnabled(), is(false));

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

            // Suspend the services - we do this via the storage member like the Operator would
            Logger.info(" Suspending service " + sServiceName);
            Boolean fSuspended = member.invoke(new SuspendService(sServiceName));
            assertThat(fSuspended, is(true));
            Logger.info("Suspended service " + sServiceName);

            // futures should not be completed
            assertThat(futureOne.isDone(), is(false));
            assertThat(futureTwo.isDone(), is(false));
            
            // shutdown the storage members
            Logger.info("Stopping storage.");
            s_storageCluster.close();

            // we should eventually have a single cluster member (which is this test JVM)
            Eventually.assertDeferred(() -> cluster.getMemberSet().size(), is(1));
            Logger.info("Stopped storage.");

            // futures should not be completed
            assertThat(futureOne.isDone(), is(false));
            assertThat(futureTwo.isDone(), is(false));

            // restart the storage cluster
            Logger.info("Restarting storage.");
            s_storageCluster = startCluster("restarted");

            // we should eventually have three cluster members
            Eventually.assertDeferred(() -> cluster.getMemberSet().size(), is(3));
            Logger.info("Restarted storage.");

            IsServiceRunning isRunning = new IsServiceRunning(sServiceName);
            for (CoherenceClusterMember m : s_storageCluster)
                {
                Eventually.assertDeferred(() -> m.invoke(isRunning), is(true));
                }
            Logger.info("Restarted service " + sServiceName + " on all members");

            // futures should not be completed
            assertThat(futureOne.isDone(), is(false));
            assertThat(futureTwo.isDone(), is(false));

            // The topics cache service should still be suspended so resume it via a storage member like the operator would
            Logger.info("Resuming service " + sServiceName);
            member = s_storageCluster.stream().findAny().orElse(null);
            assertThat(member, is(notNullValue()));
            Boolean fResumed = member.invoke(new ResumeService(sServiceName));
            assertThat(fResumed, is(true));
            Logger.info("Resumed service " + sServiceName);

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
                Logger.info("Publishing " + cChannel + " messages");
                for (int i = 0; i < cChannel; i++)
                    {
                    Message message = new Message(i, "Message-" + i);
                    publisher.publish(message).get(1, TimeUnit.MINUTES);
                    Logger.info("Published " + message);
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

    @Test
//    @Ignore("skipped until a solution to non-clean shutdown with active persistence is discovered")
    @SuppressWarnings("unchecked")
    public void shouldRecoverWaitingSubscriberAfterStorageRestart() throws Exception
        {
        NamedTopic<Message>     topic        = ensureTopic("test-four");
        String                  sGroup       = "group-one";
        DistributedCacheService service      = (DistributedCacheService) topic.getService();
        Cluster                 cluster      = service.getCluster();
        String                  sServiceName = service.getInfo().getServiceName();

        // the test should not be storage enabled
        assertThat(service.isLocalStorageEnabled(), is(false));

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

            // shutdown the storage members
            Logger.info("Stopping storage.");
            s_storageCluster.close();

            // we should eventually have a single cluster member (which is this test JVM)
            Eventually.assertDeferred(() -> cluster.getMemberSet().size(), is(1));
            Logger.info("Stopped storage.");

            // futures should not be completed
            assertThat(futureOne.isDone(), is(false));
            assertThat(futureTwo.isDone(), is(false));

            // restart the storage cluster
            Logger.info("Restarting storage.");
            s_storageCluster = startCluster("restarted");

            // we should eventually have three cluster members
            Eventually.assertDeferred(() -> cluster.getMemberSet().size(), is(3));
            Logger.info("Restarted storage.");

            IsServiceRunning isRunning = new IsServiceRunning(sServiceName);
            for (CoherenceClusterMember m : s_storageCluster)
                {
                Eventually.assertDeferred(() -> m.invoke(isRunning), is(true));
                }
            Logger.info("Restarted service " + sServiceName + " on all members");

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
                Logger.info("Publishing " + cChannel + " messages");
                for (int i = 0; i < cChannel; i++)
                    {
                    Message message = new Message(i, "Message-" + i);
                    publisher.publish(message).get(1, TimeUnit.MINUTES);
                    Logger.info("Published " + message);
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
        OptionsByType           options     = OptionsByType.of(s_options)
                                                .addAll(OperationalOverride.of("common-tangosol-coherence-override.xml"),
                                                        CacheConfig.of("simple-persistence-bdb-cache-config.xml"),
                                                        SystemProperty.of("test.log.level", "9"),
                                                        SystemProperty.of("test.log", "stderr"),
                                                        SystemProperty.of("coherence.distributed.partitioncount", "13"),
                                                        LocalStorage.enabled(),
                                                        StabilityPredicate.none(),
                                                        Logging.atMax(),
                                                        JMXManagementMode.ALL,
                                                        DisplayName.of(sMethodName + '-' + suffix),
                                                        s_testLogs.builder());

        builder.with(ClusterName.of(sMethodName))
               .include(2, CoherenceClusterMember.class, options.asArray());

        return builder.build(LocalPlatform.get());
        }

    // ----- inner class: Message -------------------------------------------

    public static class Message
            implements ExternalizableLite, Comparable<Message>
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

    private static CoherenceCluster s_storageCluster;

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

    private static final OptionsByType s_options = OptionsByType.of(
            SystemProperty.of("coherence.guard.timeout", 60000),
            CacheConfig.of("simple-persistence-bdb-cache-config.xml"),
            OperationalOverride.of("common-tangosol-coherence-override.xml"),
            SystemProperty.of("coherence.distributed.partitioncount", "13"));


    private static Coherence s_coherence;

    private static Session s_session;

    private final AtomicInteger f_cTopic = new AtomicInteger(0);

    private NamedTopic<?> m_topic;
    }
