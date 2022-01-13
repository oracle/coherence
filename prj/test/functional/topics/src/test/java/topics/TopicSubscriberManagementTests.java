/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.java.options.ClassName;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberInfo;

import com.tangosol.net.Coherence;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Session;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.ChannelOwnershipListeners;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.tangosol.net.topic.Subscriber.ChannelOwnershipListeners.withListener;
import static com.tangosol.net.topic.Subscriber.Name.inGroup;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.number.OrderingComparison.greaterThan;

@SuppressWarnings("unchecked")
public class TopicSubscriberManagementTests
    {
    @BeforeClass
    public static void setup()
        {
        System.setProperty(LocalStorage.PROPERTY, "true");
        System.setProperty(Logging.PROPERTY_LEVEL, "9");

        s_coherence = Coherence.clusterMember();
        s_coherence.start().join();
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

    @Test
    public void shouldGetChannelAllocations()
        {
        NamedTopic<String>      topic         = s_session.getTopic(f_testName.getMethodName());
        DistributedCacheService service       = (DistributedCacheService) topic.getService();
        PagedTopicCaches        caches        = new PagedTopicCaches(topic.getName(), service);
        String                  sGroupOne     = "group-one";
        String                  sGroupTwo     = "group-two";
        OwnershipListener       listenerOne   = new OwnershipListener();
        OwnershipListener       listenerTwo   = new OwnershipListener();
        OwnershipListener       listenerThree = new OwnershipListener();

        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerOne));
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerTwo));
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
            NamedTopic<String>      topic         = s_session.getTopic(f_testName.getMethodName());
            DistributedCacheService service       = (DistributedCacheService) topic.getService();
            PagedTopicCaches        caches        = new PagedTopicCaches(topic.getName(), service);
            String                  sGroupOne     = "group-one";
            String                  sGroupTwo     = "group-two";
            OwnershipListener       listenerOne   = new OwnershipListener();
            OwnershipListener       listenerTwo   = new OwnershipListener();
            OwnershipListener       listenerThree = new OwnershipListener();

            try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerOne));
                 PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerTwo));
                 PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupTwo), withListener(listenerThree)))
                {
                Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(greaterThan(0)));
                Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(greaterThan(0)));
                Eventually.assertDeferred(() -> subscriberThree.getChannels().length, is(greaterThan(0)));

                Eventually.assertDeferred(() -> caches.Subscribers.size(), is(3));

                Set<SubscriberInfo.Key> setSubscriberGroupOne = caches.getSubscribers(sGroupOne);
                Set<SubscriberInfo.Key> setSubscriberGroupTwo = caches.getSubscribers(sGroupTwo);

                assertThat(setSubscriberGroupOne, containsInAnyOrder(subscriberOne.getKey(), subscriberTwo.getKey()));
                assertThat(setSubscriberGroupTwo, containsInAnyOrder(subscriberThree.getKey()));

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
                caches.disconnectSubscriber(subscriberOne.getKey());

                Eventually.assertDeferred(futureLostOne::isDone, is(true));
                assertThat(futureLostTwo.isDone(), is(false));
                assertThat(futureLostThree.isDone(), is(false));

                // all channels should eventually be allocated to subscriberTwo
                Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(topic.getChannelCount()));
                Map<Long, Set<Integer>> mapAllocation = caches.getChannelAllocations(sGroupOne);
                assertThat(mapAllocation.size(), is(1));
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
        public void shouldDisconnectSingleSubscriberByGroupAndId() throws Exception
            {
            NamedTopic<String>      topic         = s_session.getTopic(f_testName.getMethodName());
            DistributedCacheService service       = (DistributedCacheService) topic.getService();
            PagedTopicCaches        caches        = new PagedTopicCaches(topic.getName(), service);
            String                  sGroupOne     = "group-one";
            String                  sGroupTwo     = "group-two";
            OwnershipListener       listenerOne   = new OwnershipListener();
            OwnershipListener       listenerTwo   = new OwnershipListener();
            OwnershipListener       listenerThree = new OwnershipListener();

            try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerOne));
                 PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerTwo));
                 PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupTwo), withListener(listenerThree)))
                {
                Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(greaterThan(0)));
                Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(greaterThan(0)));
                Eventually.assertDeferred(() -> subscriberThree.getChannels().length, is(greaterThan(0)));

                Eventually.assertDeferred(() -> caches.Subscribers.size(), is(3));

                Set<SubscriberInfo.Key> setSubscriberGroupOne = caches.getSubscribers(sGroupOne);
                Set<SubscriberInfo.Key> setSubscriberGroupTwo = caches.getSubscribers(sGroupTwo);

                assertThat(setSubscriberGroupOne, containsInAnyOrder(subscriberOne.getKey(), subscriberTwo.getKey()));
                assertThat(setSubscriberGroupTwo, containsInAnyOrder(subscriberThree.getKey()));

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
                caches.disconnectSubscriber(sGroupOne, subscriberOne.getId());

                Eventually.assertDeferred(futureLostOne::isDone, is(true));
                assertThat(futureLostTwo.isDone(), is(false));
                assertThat(futureLostThree.isDone(), is(false));

                // all channels should eventually be allocated to subscriberTwo
                Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(topic.getChannelCount()));
                Map<Long, Set<Integer>> mapAllocation = caches.getChannelAllocations(sGroupOne);
                assertThat(mapAllocation.size(), is(1));
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
            NamedTopic<String>      topic         = s_session.getTopic(f_testName.getMethodName());
            DistributedCacheService service       = (DistributedCacheService) topic.getService();
            PagedTopicCaches        caches        = new PagedTopicCaches(topic.getName(), service);
            String                  sGroupOne     = "group-one";
            String                  sGroupTwo     = "group-two";
            OwnershipListener       listenerOne   = new OwnershipListener();
            OwnershipListener       listenerTwo   = new OwnershipListener();
            OwnershipListener       listenerThree = new OwnershipListener();

            try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerOne));
                 PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerTwo));
                 PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupTwo), withListener(listenerThree)))
                {
                Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(greaterThan(0)));
                Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(greaterThan(0)));
                Eventually.assertDeferred(() -> subscriberThree.getChannels().length, is(greaterThan(0)));

                Eventually.assertDeferred(() -> caches.Subscribers.size(), is(3));

                Set<SubscriberInfo.Key> setSubscriberGroupOne = caches.getSubscribers(sGroupOne);
                Set<SubscriberInfo.Key> setSubscriberGroupTwo = caches.getSubscribers(sGroupTwo);

                assertThat(setSubscriberGroupOne, containsInAnyOrder(subscriberOne.getKey(), subscriberTwo.getKey()));
                assertThat(setSubscriberGroupTwo, containsInAnyOrder(subscriberThree.getKey()));

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
        public void shouldDisconnectAllSubscribers() throws Exception
            {
            NamedTopic<String>      topic         = s_session.getTopic(f_testName.getMethodName());
            DistributedCacheService service       = (DistributedCacheService) topic.getService();
            PagedTopicCaches        caches        = new PagedTopicCaches(topic.getName(), service);
            String                  sGroupOne     = "group-one";
            String                  sGroupTwo     = "group-two";
            OwnershipListener       listenerOne   = new OwnershipListener();
            OwnershipListener       listenerTwo   = new OwnershipListener();
            OwnershipListener       listenerThree = new OwnershipListener();

            try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerOne));
                 PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerTwo));
                 PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupTwo), withListener(listenerThree)))
                {
                Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(greaterThan(0)));
                Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(greaterThan(0)));
                Eventually.assertDeferred(() -> subscriberThree.getChannels().length, is(greaterThan(0)));

                Eventually.assertDeferred(() -> caches.Subscribers.size(), is(3));

                Set<SubscriberInfo.Key> setSubscriberGroupOne = caches.getSubscribers(sGroupOne);
                Set<SubscriberInfo.Key> setSubscriberGroupTwo = caches.getSubscribers(sGroupTwo);

                assertThat(setSubscriberGroupOne, containsInAnyOrder(subscriberOne.getKey(), subscriberTwo.getKey()));
                assertThat(setSubscriberGroupTwo, containsInAnyOrder(subscriberThree.getKey()));

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

                // disconnect ALL subscribers for topic
                caches.disconnectAllSubscribers();

                Eventually.assertDeferred(futureLostOne::isDone, is(true));
                Eventually.assertDeferred(futureLostTwo::isDone, is(true));
                Eventually.assertDeferred(futureLostThree::isDone, is(true));

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
    public void shouldDisconnectSubscribersOnSpecificMember()
        {
        NamedTopic<String>      topic       = s_session.getTopic(f_testName.getMethodName());
        DistributedCacheService service     = (DistributedCacheService) topic.getService();
        PagedTopicCaches        caches      = new PagedTopicCaches(topic.getName(), service);
        String                  sGroupOne   = "group-one";
        String                  sGroupTwo   = "group-two";
        OwnershipListener       listenerOne = new OwnershipListener();
        OwnershipListener       listenerTwo = new OwnershipListener();
        LocalPlatform           platform    = LocalPlatform.get();

        try (CoherenceClusterMember member    = platform.launch(CoherenceClusterMember.class,
                                                                 LocalStorage.disabled(),
                                                                 ClassName.of(Coherence.class),
                                                                 s_testLogs.builder()))
            {
            Eventually.assertDeferred(() -> member.invoke(new IsCoherenceRunning()), is(true));

            try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupOne), withListener(listenerOne));
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
        @Override
        public synchronized void onChannelsAssigned(Set<Integer> setAssigned)
            {
            f_listOnAssigned.forEach(c -> c.accept(setAssigned));
            }

        @Override
        public synchronized void onChannelsRevoked(Set<Integer> setRevoked)
            {
            f_listOnRevoked.forEach(c -> c.accept(setRevoked));
            }

        @Override
        public synchronized void onChannelsLost(Set<Integer> setLost)
            {
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

        private final List<Consumer<Set<Integer>>> f_listOnAssigned = new ArrayList<>();

        private final List<Consumer<Set<Integer>>> f_listOnRevoked = new ArrayList<>();

        private final List<Consumer<Set<Integer>>> f_listOnLost = new ArrayList<>();
        }

    // ----- data members ---------------------------------------------------

    @Rule
    public TestName f_testName = new TestName();

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(TopicsRecoveryTests.class);

    private static Coherence s_coherence;

    private static Session s_session;
    }
