/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.java.options.ClassName;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.util.Threads;
import com.oracle.coherence.testing.junit.ThreadDumpOnTimeoutRule;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.Session;

import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber.withIdentifyingName;
import static com.tangosol.net.topic.Subscriber.ChannelOwnershipListeners.withListener;
import static com.tangosol.net.topic.Subscriber.Name.inGroup;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.fail;

@SuppressWarnings({"unchecked", "resource"})
public class TopicSubscriberManagementTests
    {
    @BeforeClass
    public static void setup()
        {
        System.setProperty(WellKnownAddress.PROPERTY, "127.0.0.1");
        System.setProperty(LocalHost.PROPERTY, "127.0.0.1");
        System.setProperty(LocalStorage.PROPERTY, "true");
        System.setProperty(Logging.PROPERTY_LEVEL, "9");
        System.setProperty(PagedTopicPartition.PROP_PUBLISHER_NOTIFICATION_EXPIRY_MILLIS, "10s");

        SessionConfiguration sessionConfiguration = SessionConfiguration.builder()
                .withParameter("coherence.profile", "thin")
                .withParameter("coherence.topic.reconnect.wait", "15s")
                .build();

        CoherenceConfiguration configuration = CoherenceConfiguration.builder()
                .withSession(sessionConfiguration)
                .build();

        s_coherence = Coherence.clusterMember(configuration)
                .start()
                .join();
        
        s_session = s_coherence.getSession();
        }

    @AfterClass
    public static void cleanup()
        {
        if (s_coherence != null)
            {
            s_coherence.close();
            }
        }

    @Before
    public void start()
        {
        Logger.info(">>>> Starting test " + f_testName.getMethodName());
        }

    @After
    public void after()
        {
        Logger.info(">>>> Finished test " + f_testName.getMethodName());
        }

    @Test
    public void shouldGetChannelAllocations()
        {
        NamedTopic<String> topic         = s_session.getTopic(f_testName.getMethodName());
        PagedTopicService  service       = (PagedTopicService) topic.getService();
        String             sGroupOne     = "group-one";
        String             sGroupTwo     = "group-two";
        OwnershipListener  listenerOne   = new OwnershipListener("one");
        OwnershipListener  listenerTwo   = new OwnershipListener("two");
        OwnershipListener  listenerThree = new OwnershipListener("three");

        try (PagedTopicCaches             caches          = new PagedTopicCaches(topic.getName(), service);
             PagedTopicSubscriber<String> subscriberOne   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerOne));
             PagedTopicSubscriber<String> subscriberTwo   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerTwo));
             PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupTwo), withListener(listenerThree)))
            {
            Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(greaterThan(0)));
            Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(greaterThan(0)));
            Eventually.assertDeferred(() -> subscriberThree.getChannels().length, is(greaterThan(0)));

            Eventually.assertDeferred(() -> caches.Subscribers.size(), is(3));

            Map<Long, Set<Integer>> mapOne = caches.getChannelAllocations(sGroupOne);
            assertThat(mapOne.size(), is(2));
            assertThat(mapOne.get(subscriberOne.getId()), is(subscriberOne.getChannelSet()));
            assertThat(mapOne.get(subscriberTwo.getId()), is(subscriberTwo.getChannelSet()));

            Map<Long, Set<Integer>> mapTwo = caches.getChannelAllocations(sGroupTwo);
            assertThat(mapTwo.size(), is(1));
            assertThat(mapTwo.get(subscriberThree.getId()), is(subscriberThree.getChannelSet()));

            caches.printChannelAllocations(sGroupOne, System.err);
            }
        }

    @Test
    public void shouldDisconnectSingleSubscriberByKey() throws Exception
        {
        System.err.println(">>>>> Entering shouldDisconnectSingleSubscriberByKey");
        NamedTopic<String> topic         = s_session.getTopic(f_testName.getMethodName());
        PagedTopicService  service       = (PagedTopicService) topic.getService();
        String             sGroupOne     = "group-one";
        String             sGroupTwo     = "group-two";
        OwnershipListener  listenerOne   = new OwnershipListener("one");
        OwnershipListener  listenerTwo   = new OwnershipListener("two");
        OwnershipListener  listenerThree = new OwnershipListener("three");

        System.err.println(">>>>> In shouldDisconnectSingleSubscriberByKey - creating publisher and subscribers");
        try (PagedTopicCaches             caches          = new PagedTopicCaches(topic.getName(), service,false);
             PagedTopicSubscriber<String> subscriberOne   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerOne));
             PagedTopicSubscriber<String> subscriberTwo   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerTwo));
             PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupTwo), withListener(listenerThree)))
            {
            System.err.println(">>>>> In shouldDisconnectSingleSubscriberByKey - awaiting subscriber channel allocation");
            Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(greaterThan(0)));
            Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(greaterThan(0)));
            Eventually.assertDeferred(() -> subscriberThree.getChannels().length, is(greaterThan(0)));

            System.err.println(">>>>> In shouldDisconnectSingleSubscriberByKey - awaiting three subscribers");
            Eventually.assertDeferred(() -> caches.Subscribers.size(), is(3));
            Eventually.assertDeferred(() -> caches.Subscribers.keySet().size(), is(3));

            System.err.println(">>>>> In shouldDisconnectSingleSubscriberByKey - Subscribers " + caches.Subscribers.keySet());

            Set<SubscriberId> setSubscriberGroupOne = caches.getSubscribers(sGroupOne);
            Set<SubscriberId> setSubscriberGroupTwo = caches.getSubscribers(sGroupTwo);

            System.err.println(">>>>> In shouldDisconnectSingleSubscriberByKey - Subscribers groupOne " + setSubscriberGroupOne);
            System.err.println(">>>>> In shouldDisconnectSingleSubscriberByKey - Subscribers groupTwo " + setSubscriberGroupTwo);

            assertThat(setSubscriberGroupOne, containsInAnyOrder(subscriberOne.getSubscriberId(), subscriberTwo.getSubscriberId()));
            assertThat(setSubscriberGroupTwo, containsInAnyOrder(subscriberThree.getSubscriberId()));

            int cNotification = caches.Notifications.size();
            assertThat(cNotification, is(0));

            // receive - should not complete as topic is empty
            CompletableFuture<Subscriber.Element<String>> futureOne = subscriberOne.receive();
            CompletableFuture<Subscriber.Element<String>> futureTwo = subscriberTwo.receive();
            CompletableFuture<Subscriber.Element<String>> futureThree = subscriberThree.receive();

            // wait for the notifications to appear, so we know the subscribers are waiting
            // there will be one notification per channel, per subscriber group
            Eventually.assertDeferred(() -> caches.Notifications.size(), is(topic.getChannelCount() * 2));

            CompletableFuture<Set<Integer>> futureLostOne   = listenerOne.awaitLost();
            CompletableFuture<Set<Integer>> futureLostTwo   = listenerTwo.awaitLost();
            CompletableFuture<Set<Integer>> futureLostThree = listenerThree.awaitLost();

            // disconnect subscriberOne *only* using its key
            System.err.println(">>>>> In shouldDisconnectSingleSubscriberByKey - Disconnecting subscriber " + subscriberOne.getSubscriberId());
            caches.disconnectSubscriber(subscriberOne.getSubscriberGroupId(), subscriberOne.getSubscriberId());

            Eventually.assertDeferred(futureLostOne::isDone, is(true));
            assertThat(futureLostTwo.isDone(), is(false));
            assertThat(futureLostThree.isDone(), is(false));

            // all channels should eventually be allocated to subscriberTwo
            Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(topic.getChannelCount()));
            final AtomicReference<Map<Long, Set<Integer>>> ref = new AtomicReference<>();
            Eventually.assertDeferred(() ->
                {
                Map<Long, Set<Integer>> map = caches.getChannelAllocations(sGroupOne);
                ref.set(map);
                return map.size();
                }, is(1));

            assertThat(ref.get().get(subscriberTwo.getId()), is(subscriberTwo.getChannelSet()));

            // receive futures should still be waiting
            assertThat(futureOne.isDone(), is(false));
            assertThat(futureTwo.isDone(), is(false));
            assertThat(futureThree.isDone(), is(false));

            // publish messages
            try (Publisher<String> publisher = topic.createPublisher(Publisher.OrderBy.roundRobin()))
                {
                for (int i = 0; i < publisher.getChannelCount(); i++)
                    {
                    String sMessage = "foo bar " + i;
                    publisher.publish(sMessage).get(5, TimeUnit.MINUTES);
                    }
                }

            // The subscribers should reconnect and receive the message
            Subscriber.Element<String> element;

            element = futureOne.get(5, TimeUnit.MINUTES);
            assertThat(element.getValue(), startsWith("foo bar"));

            element = futureTwo.get(5, TimeUnit.MINUTES);
            assertThat(element.getValue(), startsWith("foo bar"));

            element = futureThree.get(5, TimeUnit.MINUTES);
            assertThat(element.getValue(), startsWith("foo bar"));
            }
        }

    @Test
    public void shouldDisconnectSingleSubscriberByGroupAndId() throws Exception
        {
        NamedTopic<String> topic         = s_session.getTopic(f_testName.getMethodName());
        PagedTopicService  service       = (PagedTopicService) topic.getService();
        String             sGroupOne     = "group-one";
        String             sGroupTwo     = "group-two";
        OwnershipListener  listenerOne   = new OwnershipListener("one");
        OwnershipListener  listenerTwo   = new OwnershipListener("two");
        OwnershipListener  listenerThree = new OwnershipListener("three");

        try (PagedTopicCaches             caches          = new PagedTopicCaches(topic.getName(), service);
             PagedTopicSubscriber<String> subscriberOne   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerOne));
             PagedTopicSubscriber<String> subscriberTwo   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerTwo));
             PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupTwo), withListener(listenerThree)))
            {
            Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(greaterThan(0)));
            Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(greaterThan(0)));
            Eventually.assertDeferred(() -> subscriberThree.getChannels().length, is(greaterThan(0)));

            Eventually.assertDeferred(() -> caches.Subscribers.size(), is(3));
            Eventually.assertDeferred(() -> caches.Subscribers.keySet().size(), is(3));

            Set<SubscriberId> setSubscriberGroupOne = caches.getSubscribers(sGroupOne);
            Set<SubscriberId> setSubscriberGroupTwo = caches.getSubscribers(sGroupTwo);

            assertThat(setSubscriberGroupOne, containsInAnyOrder(subscriberOne.getSubscriberId(), subscriberTwo.getSubscriberId()));
            assertThat(setSubscriberGroupTwo, containsInAnyOrder(subscriberThree.getSubscriberId()));

            int cNotification = caches.Notifications.size();
            assertThat(cNotification, is(0));

            // receive - should not complete as topic is empty
            CompletableFuture<Subscriber.Element<String>> futureOne = subscriberOne.receive();
            CompletableFuture<Subscriber.Element<String>> futureTwo = subscriberTwo.receive();
            CompletableFuture<Subscriber.Element<String>> futureThree = subscriberThree.receive();

            // wait for the notifications to appear, so we know the subscribers are waiting
            // there will be one notification per channel, per subscriber group
            Eventually.assertDeferred(() -> caches.Notifications.size(), is(topic.getChannelCount() * 2));

            CompletableFuture<Set<Integer>> futureLostOne   = listenerOne.awaitLost();
            CompletableFuture<Set<Integer>> futureLostTwo   = listenerTwo.awaitLost();
            CompletableFuture<Set<Integer>> futureLostThree = listenerThree.awaitLost();

            // disconnect subscriberOne *only* using the subscriber group name and subscriber id
            caches.disconnectSubscriber(subscriberOne.getSubscriberGroupId(), subscriberOne.getSubscriberId());

            Eventually.assertDeferred(futureLostOne::isDone, is(true));
            assertThat(futureLostTwo.isDone(), is(false));
            assertThat(futureLostThree.isDone(), is(false));

            // all channels should eventually be allocated to subscriberTwo
            Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(topic.getChannelCount()));
            Eventually.assertDeferred(() -> caches.getChannelAllocations(sGroupOne).size(), is(1));
            Map<Long, Set<Integer>> mapAllocation = caches.getChannelAllocations(sGroupOne);
            assertThat(mapAllocation.get(subscriberTwo.getId()), is(subscriberTwo.getChannelSet()));

            // receive futures should still be waiting
            assertThat(futureOne.isDone(), is(false));
            assertThat(futureTwo.isDone(), is(false));
            assertThat(futureThree.isDone(), is(false));

            // publish messages
            try (Publisher<String> publisher = topic.createPublisher(Publisher.OrderBy.roundRobin()))
                {
                for (int i = 0; i < publisher.getChannelCount(); i++)
                    {
                    String sMessage = "foo bar " + i;
                    publisher.publish(sMessage).get(5, TimeUnit.MINUTES);
                    }
                }

            // The subscribers should reconnect and receive the message
            Subscriber.Element<String> element;

            element = futureOne.get(5, TimeUnit.MINUTES);
            assertThat(element.getValue(), startsWith("foo bar"));

            element = futureTwo.get(5, TimeUnit.MINUTES);
            assertThat(element.getValue(), startsWith("foo bar"));

            element = futureThree.get(5, TimeUnit.MINUTES);
            assertThat(element.getValue(), startsWith("foo bar"));
            }
        }

    @Test
    public void shouldDisconnectAllSubscribersInGroup() throws Exception
        {
        NamedTopic<String> topic         = s_session.getTopic(f_testName.getMethodName());
        PagedTopicService  service       = (PagedTopicService) topic.getService();
        String             sGroupOne     = "group-one";
        String             sGroupTwo     = "group-two";
        OwnershipListener  listenerOne   = new OwnershipListener("one");
        OwnershipListener  listenerTwo   = new OwnershipListener("two");
        OwnershipListener  listenerThree = new OwnershipListener("three");

        try (PagedTopicCaches             caches          = new PagedTopicCaches(topic.getName(), service);
             PagedTopicSubscriber<String> subscriberOne   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerOne));
             PagedTopicSubscriber<String> subscriberTwo   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerTwo));
             PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupTwo), withListener(listenerThree)))
            {
            Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(greaterThan(0)));
            Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(greaterThan(0)));
            Eventually.assertDeferred(() -> subscriberThree.getChannels().length, is(greaterThan(0)));

            Eventually.assertDeferred(() -> caches.Subscribers.size(), is(3));

            Set<SubscriberId> setSubscriberGroupOne = caches.getSubscribers(sGroupOne);
            Set<SubscriberId> setSubscriberGroupTwo = caches.getSubscribers(sGroupTwo);

            assertThat(setSubscriberGroupOne, containsInAnyOrder(subscriberOne.getSubscriberId(), subscriberTwo.getSubscriberId()));
            assertThat(setSubscriberGroupTwo, containsInAnyOrder(subscriberThree.getSubscriberId()));

            int cNotification = caches.Notifications.size();
            assertThat(cNotification, is(0));

            // receive - should not complete as topic is empty
            CompletableFuture<Subscriber.Element<String>> futureOne   = subscriberOne.receive();
            CompletableFuture<Subscriber.Element<String>> futureTwo   = subscriberTwo.receive();
            CompletableFuture<Subscriber.Element<String>> futureThree = subscriberThree.receive();

            // wait for the notifications to appear, so we know the subscribers are waiting
            // there will be one notification per channel, per subscriber group
            Eventually.assertDeferred(() -> caches.Notifications.size(), is(topic.getChannelCount() * 2));

            CompletableFuture<Set<Integer>> futureLostOne   = listenerOne.awaitLost();
            CompletableFuture<Set<Integer>> futureLostTwo   = listenerTwo.awaitLost();
            CompletableFuture<Set<Integer>> futureLostThree = listenerThree.awaitLost();

            // disconnect ALL subscribers for group one
            long[] alDisconnected = caches.disconnectAllSubscribers(sGroupOne);
            assertThat(alDisconnected.length, is(2));

            assertThat(caches.Subscribers.get(subscriberThree.getKey()), is(notNullValue()));

            Eventually.assertDeferred(futureLostOne::isDone, is(true));
            Eventually.assertDeferred(futureLostTwo::isDone, is(true));
            assertThat(futureLostThree.isDone(), is(false));

            // receive futures should still be waiting
            assertThat(futureOne.isDone(), is(false));
            assertThat(futureTwo.isDone(), is(false));
            assertThat(futureThree.isDone(), is(false));

            // publish messages
            try (Publisher<String> publisher = topic.createPublisher(Publisher.OrderBy.roundRobin()))
                {
                for (int i = 0; i < publisher.getChannelCount(); i++)
                    {
                    String sMessage = "foo bar " + i;
                    publisher.publish(sMessage).get(5, TimeUnit.MINUTES);
                    }
                }

            // The subscribers should reconnect and receive the message
            Subscriber.Element<String> element;

            element = futureOne.get(5, TimeUnit.MINUTES);
            assertThat(element.getValue(), startsWith("foo bar"));

            element = futureTwo.get(5, TimeUnit.MINUTES);
            assertThat(element.getValue(), startsWith("foo bar"));

            element = futureThree.get(5, TimeUnit.MINUTES);
            assertThat(element.getValue(), startsWith("foo bar"));
            }
        }

    @Test
    public void shouldDisconnectAllSubscribersAndReconnectOnPublishNotification() throws Exception
        {
        NamedTopic<String> topic         = s_session.getTopic(f_testName.getMethodName());
        PagedTopicService  service       = (PagedTopicService) topic.getService();
        String             sGroupOne     = "group-one";
        String             sGroupTwo     = "group-two";
        OwnershipListener  listenerOne   = new OwnershipListener("one");
        OwnershipListener  listenerTwo   = new OwnershipListener("two");
        OwnershipListener  listenerThree = new OwnershipListener("three");

        SubscriberStateListener stateListenerOne   = new SubscriberStateListener();
        SubscriberStateListener stateListenerTwo   = new SubscriberStateListener();
        SubscriberStateListener stateListenerThree = new SubscriberStateListener();

        Logger.info(">>>> In " + f_testName.getMethodName() + ": creating subscribers");

        try (PagedTopicCaches             caches          = new PagedTopicCaches(topic.getName(), service);
             PagedTopicSubscriber<String> subscriberOne   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerOne), withIdentifyingName("one"));
             PagedTopicSubscriber<String> subscriberTwo   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerTwo), withIdentifyingName("two"));
             PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupTwo), withListener(listenerThree), withIdentifyingName("three")))
            {
            Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(greaterThan(0)));
            Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(greaterThan(0)));
            Eventually.assertDeferred(() -> subscriberThree.getChannels().length, is(greaterThan(0)));

            subscriberOne.addStateListener(stateListenerOne);
            subscriberTwo.addStateListener(stateListenerTwo);
            subscriberThree.addStateListener(stateListenerThree);

            Eventually.assertDeferred(() -> caches.Subscribers.size(), is(3));

            Set<SubscriberId> setSubscriberGroupOne = caches.getSubscribers(sGroupOne);
            Set<SubscriberId> setSubscriberGroupTwo = caches.getSubscribers(sGroupTwo);

            assertThat(setSubscriberGroupOne, containsInAnyOrder(subscriberOne.getSubscriberId(), subscriberTwo.getSubscriberId()));
            assertThat(setSubscriberGroupTwo, containsInAnyOrder(subscriberThree.getSubscriberId()));

            int cNotification = caches.Notifications.size();
            assertThat(cNotification, is(0));

            Logger.info(">>>> In " + f_testName.getMethodName() + ": Calling receive()");

            // receive - should not complete as topic is empty
            CompletableFuture<Subscriber.Element<String>> futureOne = subscriberOne.receive()
                    .handle((element, err) ->
                            {
                            Logger.info(">>>> In " + f_testName.getMethodName() + ": futureOne completed element=" + element + " error=" + err);
                            return element;
                            });
            CompletableFuture<Subscriber.Element<String>> futureTwo = subscriberTwo.receive()
                    .handle((element, err) ->
                            {
                            Logger.info(">>>> In " + f_testName.getMethodName() + ": futureTwo completed element=" + element + " error=" + err);
                            return element;
                            });
            CompletableFuture<Subscriber.Element<String>> futureThree = subscriberThree.receive()
                    .handle((element, err) ->
                            {
                            Logger.info(">>>> In " + f_testName.getMethodName() + ": futureThree completed element=" + element + " error=" + err);
                            return element;
                            });

            Logger.info(">>>> In " + f_testName.getMethodName() + ": Waiting for notification insertions");

            // wait for the notifications to appear, so we know the subscribers are waiting
            // there will be one notification per channel, per subscriber group
            Eventually.assertDeferred(() -> caches.Notifications.size(), is(topic.getChannelCount() * 2));

            CompletableFuture<Set<Integer>> futureLostOne   = listenerOne.awaitLost();
            CompletableFuture<Set<Integer>> futureLostTwo   = listenerTwo.awaitLost();
            CompletableFuture<Set<Integer>> futureLostThree = listenerThree.awaitLost();

            Logger.info(">>>> In " + f_testName.getMethodName() + ": Subscriber One: " + subscriberOne);
            Logger.info(">>>> In " + f_testName.getMethodName() + ": Subscriber Two: " + subscriberTwo);
            Logger.info(">>>> In " + f_testName.getMethodName() + ": Subscriber Three: " + subscriberThree);

            Logger.info(">>>> In " + f_testName.getMethodName() + ": Disconnecting subscribers");

            stateListenerOne.reset();
            stateListenerTwo.reset();
            stateListenerThree.reset();

            // disconnect ALL subscribers for topic
            caches.disconnectAllSubscribers();

            stateListenerOne.awaitDisconnected(1, TimeUnit.MINUTES);
            stateListenerTwo.awaitDisconnected(1, TimeUnit.MINUTES);
            stateListenerThree.awaitDisconnected(1, TimeUnit.MINUTES);

            Eventually.assertDeferred(futureLostOne::isDone, is(true));
            Eventually.assertDeferred(futureLostTwo::isDone, is(true));
            Eventually.assertDeferred(futureLostThree::isDone, is(true));

            Logger.info(">>>> In " + f_testName.getMethodName() + ": Subscriber One: " + subscriberOne);
            Logger.info(">>>> In " + f_testName.getMethodName() + ": Subscriber Two: " + subscriberTwo);
            Logger.info(">>>> In " + f_testName.getMethodName() + ": Subscriber Three: " + subscriberThree);

            Logger.info(">>>> In " + f_testName.getMethodName() + ": Disconnected subscribers - received channel lost events");

            // receive futures should still be waiting
            assertThat(futureOne.isDone(), is(false));
            assertThat(futureTwo.isDone(), is(false));
            assertThat(futureThree.isDone(), is(false));

            Logger.info(">>>> In " + f_testName.getMethodName() + ": Publishing messages");

            // publish messages
            try (Publisher<String> publisher = topic.createPublisher(Publisher.OrderBy.roundRobin()))
                {
                for (int i = 0; i < publisher.getChannelCount(); i++)
                    {
                    String sMessage = "foo bar " + i;
                    publisher.publish(sMessage).get(5, TimeUnit.MINUTES);
                    }
                }

            stateListenerOne.awaitConnected(1, TimeUnit.MINUTES);
            stateListenerTwo.awaitConnected(1, TimeUnit.MINUTES);
            stateListenerThree.awaitConnected(1, TimeUnit.MINUTES);

            Logger.info(">>>> In " + f_testName.getMethodName() + ": Waiting for subscriber receives");

            // The subscribers should reconnect and receive the message
            Subscriber.Element<String> element;

            element = futureOne.get(5, TimeUnit.MINUTES);
            assertThat(element.getValue(), startsWith("foo bar"));
            Logger.info(">>>> In " + f_testName.getMethodName() + ": Subscriber 1 received");

            element = futureTwo.get(5, TimeUnit.MINUTES);
            assertThat(element.getValue(), startsWith("foo bar"));
            Logger.info(">>>> In " + f_testName.getMethodName() + ": Subscriber 2 received");

            element = futureThree.get(5, TimeUnit.MINUTES);
            assertThat(element.getValue(), startsWith("foo bar"));
            Logger.info(">>>> In " + f_testName.getMethodName() + ": Subscriber 3 received");
            }
        catch (TimeoutException e)
            {
            System.err.println("Test timed out: ");
            System.err.println(Threads.getThreadDump(true));
            fail("Test failed with exception: " + e.getMessage());
            }
        }

    @Test
    public void shouldDisconnectAndAutoReconnectSubscribersWithPendingReceives()
        {
        NamedTopic<String> topic         = s_session.getTopic(f_testName.getMethodName());
        PagedTopicService  service       = (PagedTopicService) topic.getService();
        String             sGroupOne     = "group-one";
        String             sGroupTwo     = "group-two";
        OwnershipListener  listenerOne   = new OwnershipListener("one");
        OwnershipListener  listenerTwo   = new OwnershipListener("two");
        OwnershipListener  listenerThree = new OwnershipListener("three");

        Logger.info(">>>> In " + f_testName.getMethodName() + ": creating subscribers");

        try (PagedTopicCaches             caches          = new PagedTopicCaches(topic.getName(), service);
             PagedTopicSubscriber<String> subscriberOne   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerOne));
             PagedTopicSubscriber<String> subscriberTwo   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerTwo));
             PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupTwo), withListener(listenerThree)))
            {
            Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(greaterThan(0)));
            Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(greaterThan(0)));
            Eventually.assertDeferred(() -> subscriberThree.getChannels().length, is(greaterThan(0)));

            Eventually.assertDeferred(() -> caches.Subscribers.size(), is(3));

            Set<SubscriberId> setSubscriberGroupOne = caches.getSubscribers(sGroupOne);
            Set<SubscriberId> setSubscriberGroupTwo = caches.getSubscribers(sGroupTwo);

            assertThat(setSubscriberGroupOne, containsInAnyOrder(subscriberOne.getSubscriberId(), subscriberTwo.getSubscriberId()));
            assertThat(setSubscriberGroupTwo, containsInAnyOrder(subscriberThree.getSubscriberId()));

            int cNotification = caches.Notifications.size();
            assertThat(cNotification, is(0));

            Logger.info(">>>> In " + f_testName.getMethodName() + ": Calling receive()");

            // receive - should not complete as topic is empty
            CompletableFuture<Subscriber.Element<String>> futureOne = subscriberOne.receive();
            CompletableFuture<Subscriber.Element<String>> futureTwo = subscriberTwo.receive();
            CompletableFuture<Subscriber.Element<String>> futureThree = subscriberThree.receive();

            Logger.info(">>>> In " + f_testName.getMethodName() + ": Waiting for notification insertions");

            // wait for the notifications to appear, so we know the subscribers are waiting
            // there will be one notification per channel, per subscriber group
            Eventually.assertDeferred(() -> caches.Notifications.size(), is(topic.getChannelCount() * 2));

            CompletableFuture<Set<Integer>> futureLostOne   = listenerOne.awaitLost();
            CompletableFuture<Set<Integer>> futureLostTwo   = listenerTwo.awaitLost();
            CompletableFuture<Set<Integer>> futureLostThree = listenerThree.awaitLost();

            Logger.info(">>>> In " + f_testName.getMethodName() + ": Disconnecting subscribers");
            caches.disconnectAllSubscribers();

            Eventually.assertDeferred(futureLostOne::isDone, is(true));
            Eventually.assertDeferred(futureLostTwo::isDone, is(true));
            Eventually.assertDeferred(futureLostThree::isDone, is(true));

            Logger.info(">>>> In " + f_testName.getMethodName() + ": Disconnected subscribers - received channel lost events");

            // receive futures should still be waiting
            assertThat(futureOne.isDone(), is(false));
            assertThat(futureTwo.isDone(), is(false));
            assertThat(futureThree.isDone(), is(false));

            Logger.info(">>>> In " + f_testName.getMethodName() + ": Waiting for subscriber auto-reconnects");
            Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(greaterThan(0)));
            Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(greaterThan(0)));
            Eventually.assertDeferred(() -> subscriberThree.getChannels().length, is(greaterThan(0)));
            }
        }

    @Test
    public void shouldDisconnectAndNotReconnectInactiveSubscribers()
        {
        NamedTopic<String> topic         = s_session.getTopic(f_testName.getMethodName());
        PagedTopicService  service       = (PagedTopicService) topic.getService();
        String             sGroupOne     = "group-one";
        String             sGroupTwo     = "group-two";
        OwnershipListener  listenerOne   = new OwnershipListener("one");
        OwnershipListener  listenerTwo   = new OwnershipListener("two");
        OwnershipListener  listenerThree = new OwnershipListener("three");

        Logger.info(">>>> In " + f_testName.getMethodName() + ": creating subscribers");

        try (PagedTopicCaches             caches          = new PagedTopicCaches(topic.getName(), service);
             PagedTopicSubscriber<String> subscriberOne   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerOne));
             PagedTopicSubscriber<String> subscriberTwo   = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerTwo));
             PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupTwo), withListener(listenerThree)))
            {
            Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(greaterThan(0)));
            Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(greaterThan(0)));
            Eventually.assertDeferred(() -> subscriberThree.getChannels().length, is(greaterThan(0)));

            Eventually.assertDeferred(() -> caches.Subscribers.size(), is(3));

            Set<SubscriberId> setSubscriberGroupOne = caches.getSubscribers(sGroupOne);
            Set<SubscriberId> setSubscriberGroupTwo = caches.getSubscribers(sGroupTwo);

            assertThat(setSubscriberGroupOne, containsInAnyOrder(subscriberOne.getSubscriberId(), subscriberTwo.getSubscriberId()));
            assertThat(setSubscriberGroupTwo, containsInAnyOrder(subscriberThree.getSubscriberId()));

            CompletableFuture<Set<Integer>> futureLostOne   = listenerOne.awaitLost();
            CompletableFuture<Set<Integer>> futureLostTwo   = listenerTwo.awaitLost();
            CompletableFuture<Set<Integer>> futureLostThree = listenerThree.awaitLost();

            int cBeforeOne   = subscriberOne.getAutoReconnectTaskCount();
            int cBeforeTwo   = subscriberTwo.getAutoReconnectTaskCount();
            int cBeforeThree = subscriberThree.getAutoReconnectTaskCount();

            Logger.info(">>>> In " + f_testName.getMethodName() + ": Disconnecting subscribers");
            caches.disconnectAllSubscribers();

            Eventually.assertDeferred(futureLostOne::isDone, is(true));
            Eventually.assertDeferred(futureLostTwo::isDone, is(true));
            Eventually.assertDeferred(futureLostThree::isDone, is(true));

            Logger.info(">>>> In " + f_testName.getMethodName() + ": Disconnected subscribers - received channel lost events");

            Logger.info(">>>> In " + f_testName.getMethodName() + ": Waiting for subscriber auto-reconnects");

            Eventually.assertDeferred(subscriberOne::getAutoReconnectTaskCount, is(greaterThan(cBeforeOne)));
            Eventually.assertDeferred(subscriberTwo::getAutoReconnectTaskCount, is(greaterThan(cBeforeTwo)));
            Eventually.assertDeferred(subscriberThree::getAutoReconnectTaskCount, is(greaterThan(cBeforeThree)));

            assertThat(subscriberOne.isDisconnected(), is(true));
            assertThat(subscriberTwo.isDisconnected(), is(true));
            assertThat(subscriberThree.isDisconnected(), is(true));
            }
        }

    @Test
    public void shouldDisconnectSubscribersOnSpecificMember()
        {
        NamedTopic<String> topic       = s_session.getTopic(f_testName.getMethodName());
        PagedTopicService  service     = (PagedTopicService) topic.getService();
        String             sGroupOne   = "group-one";
        String             sGroupTwo   = "group-two";
        OwnershipListener  listenerOne   = new OwnershipListener("one");
        OwnershipListener  listenerTwo   = new OwnershipListener("two");
        LocalPlatform      platform    = LocalPlatform.get();

        try (CoherenceClusterMember member    = platform.launch(CoherenceClusterMember.class,
                                                                 WellKnownAddress.of("127.0.0.1"),
                                                                 LocalHost.of("127.0.0.1"),
                                                                 LocalStorage.disabled(),
                                                                 ClassName.of(Coherence.class),
                                                                 s_testLogs))
            {
            Eventually.assertDeferred(() -> member.invoke(new IsCoherenceRunning()), is(true));

            try (PagedTopicCaches             caches        = new PagedTopicCaches(topic.getName(), service);
                 PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerOne));
                 PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupTwo), withListener(listenerTwo)))
                {
                Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(greaterThan(0)));
                Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(greaterThan(0)));

                long nIdRemoteOne   = member.invoke(new CreateSubscriber(topic.getName(), sGroupOne));
                long nIdRemoteTwo   = member.invoke(new CreateSubscriber(topic.getName(), sGroupOne));
                long nIdRemoteThree = member.invoke(new CreateSubscriber(topic.getName(), sGroupTwo));

                Eventually.assertDeferred(() -> caches.Subscribers.size(), is(5));

                long[] alSubscriberId = caches.disconnectAllSubscribers(sGroupOne, member.getLocalMemberId());
                assertThat(alSubscriberId.length, is(2));

                Eventually.assertDeferred(() -> member.invoke(new IsSubscriberDisconnected(nIdRemoteOne)), is(true));
                Eventually.assertDeferred(() -> member.invoke(new IsSubscriberDisconnected(nIdRemoteTwo)), is(true));
                Eventually.assertDeferred(() -> member.invoke(new IsSubscriberDisconnected(nIdRemoteThree)), is(false));
                assertThat(subscriberOne.isDisconnected(), is(false));
                assertThat(subscriberTwo.isDisconnected(), is(false));
                }
            }
        }

    // ----- inner remote callable: CreateSubscriber ------------------------

    /**
     * A {@link RemoteCallable} to create some more subscribers on a remote member.
     */
    public static class CreateSubscriber
            implements RemoteCallable<Long>
        {
        public CreateSubscriber(String sTopic, String sGroup)
            {
            f_sTopic = sTopic;
            f_sGroup = sGroup;
            }

        @Override
        public Long call()
            {
            Session                 session    = Coherence.getInstance().getSession();
            NamedTopic<?>           topic      = session.getTopic(f_sTopic);
            PagedTopicSubscriber<?> subscriber = (PagedTopicSubscriber<?>) topic.createSubscriber(inGroup(f_sGroup));
            long                    nId        = subscriber.getId();
            s_mapSubscriber.put(nId, subscriber);

            Eventually.assertDeferred(() -> subscriber.getChannels().length, is(greaterThan(0)));

            return nId;
            }

        // ----- constants --------------------------------------------------

        public static final Map<Long, PagedTopicSubscriber<?>> s_mapSubscriber = new HashMap<>();

        // ----- data members ---------------------------------------------------

        private final String f_sTopic;

        private final String f_sGroup;
        }

    // ----- inner remote callable: IsSubscriberDisconnected ----------------

    /**
     * A {@link RemoteCallable} to create some more subscribers on a remote member.
     */
    public static class IsSubscriberDisconnected
            implements RemoteCallable<Boolean>
        {
        public IsSubscriberDisconnected(long nId)
            {
            f_nId = nId;
            }

        @Override
        public Boolean call()
            {
            PagedTopicSubscriber<?> subscriber = CreateSubscriber.s_mapSubscriber.get(f_nId);
            return subscriber.isDisconnected();
            }

        // ----- data members ---------------------------------------------------

        private final long f_nId;
        }

    // ----- inner remote callable: IsCoherenceRunning ----------------------

    /**
     * A {@link RemoteCallable} to create some more subscribers on a remote member.
     */
    public static class IsCoherenceRunning
            implements RemoteCallable<Boolean>
        {
        @Override
        public Boolean call() throws Exception
            {
            Coherence coherence = Coherence.getInstance();
            if (coherence == null)
                {
                return false;
                }
            coherence.whenStarted().get(5, TimeUnit.MINUTES);
            return coherence.isStarted();
            }
        }

    // ----- inner remote callable: IsCoherenceRunning ----------------------

    /**
     * A {@link Subscriber.ChannelOwnershipListener} to monitor ownership changes in tests.
     */
    public static class OwnershipListener
            implements Subscriber.ChannelOwnershipListener
        {
        public OwnershipListener(String sName)
            {
            f_sName = sName;
            }

        @Override
        public synchronized void onChannelsAssigned(Set<Integer> setAssigned)
            {
            Logger.info(">>>> OwnershipListener: onChannelsAssigned " + f_sName + " " + setAssigned);
            f_listOnAssigned.forEach(c -> c.accept(setAssigned));
            }

        @Override
        public synchronized void onChannelsRevoked(Set<Integer> setRevoked)
            {
            Logger.info(">>>> OwnershipListener: onChannelsRevoked " + f_sName + " " + setRevoked);
            f_listOnRevoked.forEach(c -> c.accept(setRevoked));
            }

        @Override
        public synchronized void onChannelsLost(Set<Integer> setLost)
            {
            Logger.info(">>>> OwnershipListener: onChannelsLost " + f_sName + " " + setLost);
            f_listOnLost.forEach(c -> c.accept(setLost));
            }

        public synchronized CompletableFuture<Set<Integer>> awaitAssigned()
            {
            CompletableFuture<Set<Integer>> future = new CompletableFuture<>();
            whenAssigned(future::complete);
            return future;
            }

        public synchronized OwnershipListener whenAssigned(Consumer<Set<Integer>> consumer)
            {
            f_listOnAssigned.add(Objects.requireNonNull(consumer));
            return this;
            }

        public synchronized CompletableFuture<Set<Integer>> awaitRevoked()
            {
            CompletableFuture<Set<Integer>> future = new CompletableFuture<>();
            whenRevoked(future::complete);
            return future;
            }

        public synchronized OwnershipListener whenRevoked(Consumer<Set<Integer>> consumer)
            {
            f_listOnRevoked.add(Objects.requireNonNull(consumer));
            return this;
            }

        public synchronized CompletableFuture<Set<Integer>> awaitLost()
            {
            CompletableFuture<Set<Integer>> future = new CompletableFuture<>();
            whenLost(future::complete);
            return future;
            }

        public synchronized OwnershipListener whenLost(Consumer<Set<Integer>> consumer)
            {
            f_listOnLost.add(Objects.requireNonNull(consumer));
            return this;
            }

        public synchronized void clear()
            {
            f_listOnAssigned.clear();
            f_listOnRevoked.clear();
            f_listOnLost.clear();
            }

        private final String f_sName;

        private final List<Consumer<Set<Integer>>> f_listOnAssigned = new ArrayList<>();

        private final List<Consumer<Set<Integer>>> f_listOnRevoked = new ArrayList<>();

        private final List<Consumer<Set<Integer>>> f_listOnLost = new ArrayList<>();
        }

    // ----- inner class: SubscriberStateListener ---------------------------

    public static class SubscriberStateListener
            implements PagedTopicSubscriber.StateListener
        {
        @Override
        public void onStateChange(PagedTopicSubscriber<?> subscriber, int nNewState, int nPrevState)
            {
            f_lock.lock();
            try
                {
                Logger.info("Subscriber state changed: from " + PagedTopicSubscriber.getStateName(nPrevState)
                                    + " to " + PagedTopicSubscriber.getStateName(nNewState) + " " + subscriber);
                m_listState.add(nNewState);
                if (nNewState == PagedTopicSubscriber.STATE_CONNECTED)
                    {
                    m_stateConnectedLatch.countDown();
                    }
                else if (nNewState == PagedTopicSubscriber.STATE_DISCONNECTED)
                    {
                    m_stateDisconnectedLatch.countDown();
                    }
                }
            finally
                {
                f_lock.unlock();
                }
            }

        List<Integer> getStates()
            {
            return Collections.unmodifiableList(m_listState);
            }

        void awaitConnected(int nTimeout, TimeUnit units) throws InterruptedException
            {
            m_stateConnectedLatch.await(nTimeout, units);
            }

        void awaitDisconnected(int nTimeout, TimeUnit units) throws InterruptedException
            {
            m_stateDisconnectedLatch.await(nTimeout, units);
            }

        public void reset()
            {
            f_lock.lock();
            try
                {
                m_listState.clear();
                m_stateConnectedLatch = new CountDownLatch(1);
                m_stateDisconnectedLatch = new CountDownLatch(1);
                }
            finally
                {
                f_lock.unlock();
                }
            }

        private final List<Integer> m_listState = new ArrayList<>();

        private CountDownLatch m_stateConnectedLatch = new CountDownLatch(1);

        private CountDownLatch m_stateDisconnectedLatch = new CountDownLatch(1);

        private Lock f_lock = new ReentrantLock();
        }

    // ----- data members ---------------------------------------------------

    @ClassRule
    public static final ThreadDumpOnTimeoutRule timeout = ThreadDumpOnTimeoutRule.after(30, TimeUnit.MINUTES);

    @Rule
    public final TestName f_testName = new TestName();

    @ClassRule
    public static final TestLogs s_testLogs = new TestLogs(TopicsRecoveryTests.class);

    private static Coherence s_coherence;

    private static Session s_session;
    }
