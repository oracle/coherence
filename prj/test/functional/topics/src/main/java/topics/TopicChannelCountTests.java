/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;

import com.oracle.bedrock.runtime.java.features.JmxFeature;

import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.testing.junit.ThreadDumpOnTimeoutRule;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicPublisher;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;

import com.tangosol.internal.net.topic.impl.paged.management.SubscriberModel;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Session;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TestName;

import topics.callables.GetChannelsWithMessages;
import topics.callables.PublishMessages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.concurrent.atomic.AtomicInteger;

import static com.tangosol.net.topic.Subscriber.completeOnEmpty;
import static com.tangosol.net.topic.Subscriber.inGroup;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;

import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings({"resource", "unchecked"})
public class TopicChannelCountTests
    {
    @After
    public void cleanup()
        {
        Cluster cluster = CacheFactory.getCluster();
        CacheFactory.shutdown();
        Eventually.assertDeferred(cluster::isRunning, is(false));
        }

    @Test
    public void shouldPublishWhenPublisherHasFewerChannels() throws Exception
        {
        String sTopicName = m_testWatcher.getMethodName();
        int    cChannel   = 5;

        try (Session session = m_cluster.buildSession(SessionBuilders
                .storageDisabledMember(SystemProperty.of(PROP_CHANNELS, cChannel))))
            {
            NamedTopic<String> topic = session.getTopic(sTopicName);
            topic.ensureSubscriberGroup(sTopicName);

            int cActual = topic.getChannelCount();

            Set<Integer> setChannel = new HashSet<>();
            try (Publisher<String> publisher = topic.createPublisher(Publisher.OrderBy.roundRobin()))
                {
                for (int i = 0; i < 100; i++)
                    {
                    Publisher.Status status = publisher.publish("message-" + i).get(1, TimeUnit.MINUTES);
                    assertThat(status, is(notNullValue()));
                    setChannel.add(status.getChannel());
                    }
                }
            // should have published to all channels based on the publisher's channel count
            assertThat(setChannel.size(), is(cActual));

            CoherenceClusterMember member = m_cluster.getCluster()
                    .findAny()
                    .orElseThrow(() -> new AssertionError("Could not find cluster member"));

            Set<Integer> setActualChannel = member.invoke(new GetChannelsWithMessages(sTopicName));
            assertThat(setActualChannel.size(), is(cActual));

            for (int i = 0; i < cActual; i++)
                {
                assertThat(setChannel.contains(i), is(true));
                assertThat(setActualChannel.contains(i), is(true));
                }
            }
        }

    @Test
    public void shouldPublishWhenPublisherConfiguredWithMoreChannels() throws Exception
        {
        String sTopicName = m_testWatcher.getMethodName();
        String sGroupName = "test-group";
        int    cChannel   = 34;

        try (Session session = m_cluster.buildSession(SessionBuilders.storageDisabledMember()))
            {
            NamedTopic<Integer> topic = session.getTopic(sTopicName);
            topic.ensureSubscriberGroup(sGroupName);

            try (Subscriber<Integer> subscriberOne = topic.createSubscriber(inGroup(sGroupName), Subscriber.CompleteOnEmpty.enabled());
                 Subscriber<Integer> subscriberTwo = topic.createSubscriber(inGroup(sGroupName), Subscriber.CompleteOnEmpty.enabled()))
                {
                try (Publisher<Integer> publisher = topic.createPublisher(PagedTopicPublisher.ChannelCount.of(cChannel), Publisher.OrderBy.roundRobin()))
                    {
                    CompletableFuture<Publisher.Status>[] aFuture = new CompletableFuture[cChannel];
                    for (int i = 0; i < cChannel; i++)
                        {
                        aFuture[i] = publisher.publish(i);
                        }

                    for (int i = 0; i < cChannel; i++)
                        {
                        Eventually.assertDeferred(aFuture[i]::isDone, is(true));
                        // the future should have completed normally
                        aFuture[i].get();
                        }

                    CoherenceClusterMember member = m_cluster.getCluster()
                            .findAny()
                            .orElseThrow(() -> new AssertionError("Could not find cluster member"));

                    // should have pages in 34 channels
                    Set<Integer> setActualChannel = member.invoke(new GetChannelsWithMessages(sTopicName));
                    assertThat(setActualChannel.size(), is(cChannel));

                    Set<Integer> setMessage = new HashSet<>();

                    Subscriber.Element<Integer> element = subscriberOne.receive().get(1, TimeUnit.MINUTES);
                    while (element != null)
                        {
                        setMessage.add(element.getValue());
                        element = subscriberOne.receive().get(1, TimeUnit.MINUTES);
                        }

                    element = subscriberTwo.receive().get(1, TimeUnit.MINUTES);
                    while (element != null)
                        {
                        setMessage.add(element.getValue());
                        element = subscriberTwo.receive().get(1, TimeUnit.MINUTES);
                        }

                    // should have received from all channels based on the publisher's channel count
                    assertThat(setMessage.size(), is(cChannel));
                    }
                }
            }
        }

    @Test
    public void shouldPublishWhenPublisherCreatedWithMoreChannels() throws Exception
        {
        String sTopicName = m_testWatcher.getMethodName();
        int    cChannel   = 34;

        try (Session session = m_cluster.buildSession(SessionBuilders.storageDisabledMember()))
            {
            NamedTopic<Integer> topic = session.getTopic(sTopicName);
            topic.ensureSubscriberGroup(sTopicName);

            // member is configured with 17 channels, publisher created with 34
            try (Publisher<Integer> publisher = topic.createPublisher(PagedTopicPublisher.ChannelCount.of(cChannel),
                                                                      Publisher.OrderBy.value(n -> n)))
                {
                CompletableFuture<Publisher.Status>[] aFuture = new CompletableFuture[cChannel];
                for (int i = 0; i < cChannel; i++)
                    {
                    aFuture[i] = publisher.publish(i);
                    }

                for (int i = 0; i < cChannel; i++)
                    {
                    Eventually.assertDeferred(aFuture[i]::isDone, is(true));
                    // we're effectively asserting the future completed normally
                    aFuture[i].get();
                    }

                CoherenceClusterMember member = m_cluster.getCluster()
                        .findAny()
                        .orElseThrow(() -> new AssertionError("Could not find cluster member"));

                Set<Integer> setActualChannel = member.invoke(new GetChannelsWithMessages(sTopicName));
                assertThat(setActualChannel.size(), is(cChannel));

                Set<Integer> setMessage = new HashSet<>();
                int cReceived = 0;
                try (Subscriber<Integer> subscriber = topic.createSubscriber(inGroup("test"), Subscriber.CompleteOnEmpty.enabled()))
                    {
                    Subscriber.Element<Integer> element = subscriber.receive().get(1, TimeUnit.MINUTES);
                    while (element != null)
                        {
                        setMessage.add(element.getValue());
                        cReceived++;
                        element = subscriber.receive().get(1, TimeUnit.MINUTES);
                        }
                    }

                // should have received from all channels based on the publisher's channel count
                assertThat(cReceived, is(cChannel));
                assertThat(setMessage.size(), is(cChannel));
                }
            }
        }

    @Test
    public void shouldSubscribeWhenSubscriberHasMoreChannels() throws Exception
        {
        CoherenceClusterMember member = m_cluster.getCluster()
                .findAny()
                .orElseThrow(() -> new AssertionError("Could not find cluster member"));

        String sTopicName = m_testWatcher.getMethodName();
        String sGroupName = "test";
        int    cChannel   = 34;
        int    cMessage   = 1000;

        try (Session session = m_cluster.buildSession(SessionBuilders
                .storageDisabledMember(SystemProperty.of(PROP_CHANNELS, cChannel))))
            {
            NamedTopic<String> topic = session.getTopic(sTopicName);
            topic.ensureSubscriberGroup(sGroupName);

            member.invoke(new PublishMessages(sTopicName, cMessage));

            Set<String> setMessage = new HashSet<>();
            int cReceived = 0;
            try (Subscriber<String> subscriber = topic.createSubscriber(inGroup(sGroupName), Subscriber.CompleteOnEmpty.enabled()))
                {
                Subscriber.Element<String> element = subscriber.receive().get(1, TimeUnit.MINUTES);
                while (element != null)
                    {
                    setMessage.add(element.getValue());
                    cReceived++;
                    element = subscriber.receive().get(1, TimeUnit.MINUTES);
                    }
                }

            // should have published to all channels based on the publisher's channel count
            assertThat(cReceived, is(cMessage));
            assertThat(setMessage.size(), is(cMessage));
            }
        }

    @Test
    public void shouldSubscribeWhenSubscriberHasFewerChannels() throws Exception
        {
        CoherenceClusterMember member = m_cluster.getCluster()
                .findAny()
                .orElseThrow(() -> new AssertionError("Could not find cluster member"));

        String sTopicName = m_testWatcher.getMethodName();
        String sGroupName = "test";
        int    cChannel   = 5;
        int    cMessage   = 1000;

        try (Session session = m_cluster.buildSession(SessionBuilders
                .storageDisabledMember(SystemProperty.of(PROP_CHANNELS, cChannel))))
            {
            NamedTopic<String> topic = session.getTopic(sTopicName);
            topic.ensureSubscriberGroup(sGroupName);

            member.invoke(new PublishMessages(sTopicName, cMessage));

            Set<String> setMessage = new HashSet<>();
            int cReceived = 0;
            try (Subscriber<String> subscriber = topic.createSubscriber(inGroup(sGroupName), Subscriber.CompleteOnEmpty.enabled()))
                {
                Subscriber.Element<String> element = subscriber.receive().get(1, TimeUnit.MINUTES);
                while (element != null)
                    {
                    setMessage.add(element.getValue());
                    cReceived++;
                    element = subscriber.receive().get(1, TimeUnit.MINUTES);
                    }
                }

            // should have published to all channels based on the publisher's channel count
            assertThat(cReceived, is(cMessage));
            assertThat(setMessage.size(), is(cMessage));
            }
        }

    @Test
    @Ignore("Skipped until Bug 34767222 is fixed")
    public void shouldIncreaseChannelCountWhileActive() throws Exception
        {
        String sTopicName = m_testWatcher.getMethodName();

        try (Session session = m_cluster.buildSession(SessionBuilders.storageDisabledMember()))
            {
            ExecutorService executorSub = Executors.newFixedThreadPool(5, new ThreadFactory()
                {
                @Override
                public Thread newThread(Runnable r)
                    {
                    return new Thread(r, "SubscriberTask-" + m_cSubscriber.getAndIncrement());
                    }
                });

            ExecutorService executorPub = Executors.newFixedThreadPool(5, new ThreadFactory()
                {
                @Override
                public Thread newThread(Runnable r)
                    {
                    return new Thread(r, "PublisherTask-" + m_cPublisher.getAndIncrement());
                    }
                });

            NamedTopic<String>   topic       = session.getTopic(sTopicName);
            List<SubscriberTask> subscribers = new ArrayList<>();
            List<PublisherTask>  publishers  = new ArrayList<>();
            Queue<Integer>       queueId     = new LinkedList<>();

            for (int i = 0; i < 10; i++)
                {
                Integer nId = queueId.poll();
                if (nId == null)
                    {
                    subscribers.add(new SubscriberTask(topic, i));
                    }
                else
                    {
                    subscribers.add(new SubscriberTask(topic, i, nId));
                    }
                }

            int cChannel = 17;
            int nId      = 0;
            for (int i = 0; i < 5; i++)
                {
                publishers.add(new PublisherTask(topic, nId++, cChannel));
                }

            for (int i = 0; i < 5; i++)
                {
                Thread.sleep(2000);
                cChannel = cChannel + (i * 3);
                publishers.add(new PublisherTask(topic, nId++, cChannel));
                }

            subscribers.forEach(executorSub::submit);
            publishers.forEach(executorPub::submit);

            executorPub.shutdown();
            executorSub.shutdown();
            executorPub.awaitTermination(5, TimeUnit.MINUTES);
            executorSub.awaitTermination(5, TimeUnit.MINUTES);

            Map<PositionAndChannel, String> mapPublished = new HashMap<>();
            Map<PositionAndChannel, String> mapReceived  = new HashMap<>();

            for (PublisherTask publisherTask : publishers)
                {
                Eventually.assertDeferred(publisherTask.m_future::isDone, is(true));
                publisherTask.m_future.get();
                mapPublished.putAll(publisherTask.m_mapMessage);
                }

            for (SubscriberTask subscriberTask : subscribers)
                {
                Eventually.assertDeferred(subscriberTask.m_future::isDone, is(true));
                subscriberTask.m_future.get();
                mapReceived.putAll(subscriberTask.m_mapMessage);
                }

            FinalSubscriberTask finalSubscriberTask = new FinalSubscriberTask(topic, subscribers.size());
            subscribers.add(finalSubscriberTask);
            finalSubscriberTask.run();
            mapReceived.putAll(finalSubscriberTask.m_mapMessage);

            int[] anPublished = mapPublished.keySet().stream().mapToInt(k -> k.m_cChannel).distinct().sorted().toArray();
            int[] anReceived = mapReceived.keySet().stream().mapToInt(k -> k.m_cChannel).distinct().sorted().toArray();

            Set<PositionAndChannel> setMissing = new HashSet<>(mapPublished.keySet());
            setMissing.removeAll(mapReceived.keySet());

            Logger.info("Published to  : " + Arrays.toString(anPublished) + " of " + cChannel);
            Logger.info("Received from : " + Arrays.toString(anReceived) + " of " + cChannel);
            Logger.info("Missing       : ");

            if (setMissing.size() > 0)
                {
                // The tes will fail in later assertions below, so we dump out some debug information
                Set<Integer> setChannel = new HashSet<>();
                for (PositionAndChannel p : setMissing)
                    {
                    setChannel.add(p.m_cChannel);
                    Logger.info(p.toString());
                    }
                for (int nChannel : setChannel)
                    {
                    for (SubscriberTask subscriberTask : subscribers)
                        {
                        Position position = subscriberTask.m_mapFirst.get(nChannel);
                        if (position != null)
                            {
                            Logger.info("Channel " + nChannel + " Subscriber " + subscriberTask.m_nId
                                    + " first=" + position + " last=" + subscriberTask.m_mapLast.get(nChannel));
                            }
                        }
                    }
                for (SubscriberTask subscriber : subscribers)
                    {
                    subscriber.dumpStats();
                    }
                }

            assertThat(mapReceived.size(), is(mapPublished.size()));
            assertThat(mapReceived, is(mapPublished));

            long cChannelPublished = mapPublished.keySet().stream()
                    .mapToInt(k -> k.m_cChannel)
                    .distinct()
                    .count();

            long cChannelReceived = mapReceived.keySet().stream()
                    .mapToInt(k -> k.m_cChannel)
                    .distinct()
                    .count();

            assertThat(cChannelReceived, is(cChannelPublished));
            }
        }

    @Test
    public void shouldIncreaseChannelCountWhileActiveSubscriber() throws Exception
        {
        String sTopicName = m_testWatcher.getMethodName();

        try (Session session = m_cluster.buildSession(SessionBuilders.storageDisabledMember()))
            {
            NamedTopic<String> topic       = session.getTopic(sTopicName);
            int                nChannelOne = 1;
            int                nChannelTwo = 21;

            try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("test-group"), completeOnEmpty(), PagedTopicSubscriber.withIdentifyingName("one"));
                 Publisher<String>  publisherOne  = topic.createPublisher(PagedTopicPublisher.ChannelCount.of(17), Publisher.OrderBy.id(nChannelOne));
                 Publisher<String>  publisherTwo  = topic.createPublisher(PagedTopicPublisher.ChannelCount.of(34), Publisher.OrderBy.id(nChannelTwo)))
                {
                List<String> listMessage = new ArrayList<>();

                for (int i = 0; i < 10; i++)
                    {
                    publisherOne.publish("message-1-" + i).get(1, TimeUnit.MINUTES);
                    }

                Subscriber.Element<String> element = subscriberOne.receive().get(1, TimeUnit.MINUTES);
                String sValue = element.getValue() + " from " + subscriberOne;
                listMessage.add(sValue);
                System.err.println("Received: " + sValue);
                element.commit();

                System.err.println("Publishing from Publisher Two: " + publisherTwo);
                publisherTwo.publish("message-2-0").get(1, TimeUnit.MINUTES);

                System.err.println("Creating Additional Subscribers");
                PagedTopicSubscriber<String> subscriberTwo   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("test-group"), completeOnEmpty(), PagedTopicSubscriber.withIdentifyingName("two"));
                PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("test-group"), completeOnEmpty(), PagedTopicSubscriber.withIdentifyingName("three"));

                System.err.println("Receive from Subscriber Two");
                element = subscriberTwo.receive().get(1, TimeUnit.MINUTES);
                if (element != null)
                    {
                    sValue = element.getValue() + " from " + subscriberTwo;
                    listMessage.add(sValue);
                    System.err.println("Received: " + sValue);
                    element.commit();
                    }

                System.err.println("Receive from Subscriber Three");
                element = subscriberThree.receive().get(1, TimeUnit.MINUTES);
                if (element != null)
                    {
                    sValue = element.getValue() + " from " + subscriberThree;
                    listMessage.add(sValue);
                    System.err.println("Received: " + sValue);
                    element.commit();
                    }

                System.err.println("Subscribers:");
                System.err.println(subscriberOne);
                System.err.println(subscriberTwo);
                System.err.println(subscriberThree);

                long cNotifyTwo   = subscriberTwo.getNotify();
                long cNotifyThree = subscriberThree.getNotify();

                System.err.println("Publishing more messages from publisher two");
                for (int i = 1; i < 10; i++)
                    {
                    publisherTwo.publish("message-2-" + i).get(1, TimeUnit.MINUTES);
                    }

                // wait for whichever subscriber owns channel "nChannelTwo" to be notified of additional messages

                //noinspection StatementWithEmptyBody
                if (subscriberOne.isOwner(nChannelTwo))
                    {
                    // nothing to do as subscriber one will not be waiting
                    }
                else if (subscriberTwo.isOwner(nChannelTwo))
                    {
                    System.err.println("Waiting for Subscriber two to be notified");
                    Eventually.assertDeferred(subscriberTwo::getNotify, is(greaterThan(cNotifyTwo)));
                    }
                else if (subscriberThree.isOwner(nChannelTwo))
                    {
                    System.err.println("Waiting for Subscriber three to be notified");
                    Eventually.assertDeferred(subscriberThree::getNotify, is(greaterThan(cNotifyThree)));
                    }

                for (PagedTopicSubscriber<String> subscriber : Arrays.asList(subscriberOne, subscriberTwo, subscriberThree))
                    {
                    System.err.println(">>> Using " + subscriber);
                    subscriber.printChannels(System.err);
                    subscriber.printPreFetchCache(System.err);

                    element = subscriber.receive().get(1, TimeUnit.MINUTES);
                    while (element != null)
                        {
                        sValue = element.getValue() + " from " + subscriber;
                        listMessage.add(sValue);
                        System.err.println("Received: " + sValue);
                        element.commit();
                        element = subscriber.receive().get(1, TimeUnit.MINUTES);
                        }
                    }

                System.err.println(">>>> Received messages:");
                listMessage.forEach(System.err::println);
                assertThat(listMessage.size(), is(greaterThanOrEqualTo(20)));
                }
            }
        }

    static class SubscriberTask
        implements Runnable
        {
        public SubscriberTask(NamedTopic<String> topic, int nId)
            {
            this(topic, nId, -1);
            }

        public SubscriberTask(NamedTopic<String> topic, int nId, int nNotificationId)
            {
            m_topic           = topic;
            m_nId             = nId;
            m_nNotificationId = nNotificationId;
            }

        public Map<PositionAndChannel, String> getMessages()
            {
            return m_mapMessage;
            }

        protected Subscriber<String> createSubscriber()
            {
            if (m_nNotificationId == -1)
                {
                return m_topic.createSubscriber(inGroup("test"),
                                                PagedTopicSubscriber.withIdentifyingName(String.valueOf(m_nId)),
                                                Subscriber.CompleteOnEmpty.enabled());
                }

            return m_topic.createSubscriber(inGroup("test"),
                    PagedTopicSubscriber.withIdentifyingName(String.valueOf(m_nId)),
                    PagedTopicSubscriber.withNotificationId(m_nNotificationId),
                    Subscriber.CompleteOnEmpty.enabled());
            }

        @Override
        public void run()
            {
            try (Subscriber<String> subscriber = m_subscriber = createSubscriber())
                {
                Logger.info("SubscriberTask starting: " + subscriber);
                try
                    {
                    long nStart = System.currentTimeMillis();
                    long nNow   = nStart;
                    long nEnd   = nStart + 2000 + ((long) m_nId * 1000);
                    while (nNow < nEnd)
                        {
                        CompletableFuture<Subscriber.Element<String>> future = subscriber.receive();
                        Subscriber.Element<String> element = future.get(1, TimeUnit.MINUTES);
                        if (element != null)
                            {
                            int      nChannel = element.getChannel();
                            Position position = element.getPosition();
                            m_mapFirst.putIfAbsent(nChannel, position);
                            m_mapLast.put(nChannel, position);
                            m_mapMessage.put(new PositionAndChannel(nChannel, position), element.getValue());
                            element.commit();
                            }
                        nNow = System.currentTimeMillis();
                        }
                    m_future.complete(null);
                    Logger.info("SubscriberTask " + m_nId + " completed (" + m_mapMessage.size() + ") subscriber=" + subscriber);
                    }
                catch (TimeoutException e)
                    {
                    Logger.err("Subscriber " + m_nId + " timed-out  subscriber=" + subscriber);
                    m_future.complete(null);
                    }
                }
            catch (Throwable t)
                {
                m_future.completeExceptionally(t);
                Logger.info("SubscriberTask " + m_nId + " completed exceptionally (" + m_mapMessage.size() + ") " + t.getMessage());
                }
            }

        public void dumpStats()
            {
            if (m_subscriber != null)
                {
                PagedTopicSubscriber<?> pagedTopicSubscriber = (PagedTopicSubscriber<?>) m_subscriber;
                System.err.println("--------------------------------------------------");
                System.err.println("Attributes for subscriber: id=" + pagedTopicSubscriber.getId()
                        + " name=" + pagedTopicSubscriber.getIdentifyingName());
                SubscriberModel model = new SubscriberModel((PagedTopicSubscriber<?>) m_subscriber);
                model.dumpAttributes(System.err);
                System.err.println("--------------------------------------------------");
                }
            }

        protected final NamedTopic<String> m_topic;

        protected Subscriber<String> m_subscriber;

        protected final int m_nId;

        protected final int m_nNotificationId;

        protected final Map<PositionAndChannel, String> m_mapMessage = new HashMap<>();

        protected final Map<Integer, Position> m_mapFirst = new HashMap<>();

        protected final Map<Integer, Position> m_mapLast = new HashMap<>();

        protected final CompletableFuture<Void> m_future = new CompletableFuture<>();
        }

    static class FinalSubscriberTask
        extends SubscriberTask
        {
        public FinalSubscriberTask(NamedTopic<String> topic, int nId)
            {
            super(topic, nId);
            }

        public FinalSubscriberTask(NamedTopic<String> topic, int nId, int nNotificationId)
            {
            super(topic, nId, nNotificationId);
            }

        @Override
        public void run()
            {
            try (Subscriber<String> subscriber = m_subscriber = createSubscriber())
                {
                Logger.info("FinalSubscriberTask starting: " + subscriber);
                CompletableFuture<Subscriber.Element<String>> future = subscriber.receive();
                Subscriber.Element<String> element = future.get(1, TimeUnit.MINUTES);
                while (element != null)
                    {
                    m_mapMessage.put(new PositionAndChannel(element.getChannel(), element.getPosition()), element.getValue());
                    //element.commit();
                    element = subscriber.receive().get(1, TimeUnit.MINUTES);
                    }
                m_future.complete(null);
                Logger.info("FinalSubscriberTask " + m_nId + " completed (" + m_mapMessage.size() + ") subscriber=" + subscriber);
                }
            catch (Throwable t)
                {
                m_future.completeExceptionally(t);
                }
            }
        }

    static class PublisherTask
        implements Runnable
        {
        public PublisherTask(NamedTopic<String> topic, int nId, int cChannel)
            {
            m_topic    = topic;
            m_nId      = nId;
            m_cChannel = cChannel;
            }

        public Map<PositionAndChannel, String> getMessages()
            {
            return m_mapMessage;
            }

        @Override
        public void run()
            {
            Logger.info("PublisherTask " + m_nId + " starting");
            try (Publisher<String> publisher = m_topic.createPublisher(Publisher.OrderBy.roundRobin(),
                                                                       PagedTopicPublisher.ChannelCount.of(m_cChannel)))
                {
                long nStart   = System.currentTimeMillis();
                long nNow     = nStart;
                long nEnd     = nStart + 2000 + ((long) m_nId * 1000);
                int  cMessage = 0;

                while (nNow < nEnd)
                    {
                    for (int i = 0; i < m_cChannel; i++)
                        {
                        String sMessage = "message-" + m_nId + "-" + cMessage++;
                        CompletableFuture<Publisher.Status> future = publisher.publish(sMessage);
                        Publisher.Status status = future.get(1, TimeUnit.MINUTES);
                        m_mapMessage.put(new PositionAndChannel(status.getChannel(), status.getPosition()), sMessage);
                        }
                    nNow = System.currentTimeMillis();
                    }
                m_future.complete(null);
                }
            catch (Throwable t)
                {
                m_future.completeExceptionally(t);
                }
            Logger.info("PublisherTask " + m_nId + " completed (" + m_mapMessage.size() + ")");
            }

        private final NamedTopic<String> m_topic;

        private final int m_nId;

        private final int m_cChannel;

        private final Map<PositionAndChannel, String> m_mapMessage = new HashMap<>();

        private final CompletableFuture<Void> m_future = new CompletableFuture<>();
        }

    static class PositionAndChannel
            implements Comparable<PositionAndChannel>
        {
        public PositionAndChannel(int cChannel, Position position)
            {
            m_cChannel = cChannel;
            m_position = position;
            }

        @Override
        public int compareTo(PositionAndChannel other)
            {
            int n = Integer.compare(m_cChannel, other.m_cChannel);
            return n == 0 ? m_position.compareTo(other.m_position) : n;
            }

        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o == null || getClass() != o.getClass())
                {
                return false;
                }
            PositionAndChannel that = (PositionAndChannel) o;
            return m_cChannel == that.m_cChannel && Objects.equals(m_position, that.m_position);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(m_cChannel, m_position);
            }

        @Override
        public String toString()
            {
            return "[channel=" + m_cChannel + ":" + m_position + ']';
            }

        private final int m_cChannel;

        private final Position m_position;
        }

    // ----- data members ---------------------------------------------------

    @ClassRule
    public static final ThreadDumpOnTimeoutRule timeout = ThreadDumpOnTimeoutRule.after(30, TimeUnit.MINUTES);

    public static final String PROP_CHANNELS = "coherence.channel.count";

    public static final int STORAGE_MEMBER_COUNT = 3;

    public static final int STORAGE_CHANNEL_COUNT = 17;

    public static final String CACHE_CONFIG_FILE = "topics-channel-config.xml";

    @Rule(order = 0)
    public TestLogs m_testLogs = new TestLogs(NamedTopicTests.class);

    @Rule(order = 1)
    public TestName m_testWatcher = new TestName();

    @Rule(order = 2)
    public CoherenceClusterResource m_cluster =
            new CoherenceClusterResource()
                    .with(ClusterName.of(m_testWatcher.getMethodName()),
                          Logging.atMax(),
                          CacheConfig.of(CACHE_CONFIG_FILE),
                          LocalHost.only(),
                          WellKnownAddress.loopback(),
                          IPv4Preferred.yes())
                    .include(STORAGE_MEMBER_COUNT,
                             CoherenceClusterMember.class,
                             SystemProperty.of(PROP_CHANNELS, STORAGE_CHANNEL_COUNT),
                             DisplayName.of("Storage"),
                             RoleName.of("storage"),
                             JmxFeature.enabled(),
                             m_testLogs.builder());

    static final AtomicInteger m_cPublisher = new AtomicInteger();

    static final AtomicInteger m_cSubscriber = new AtomicInteger();
    }
