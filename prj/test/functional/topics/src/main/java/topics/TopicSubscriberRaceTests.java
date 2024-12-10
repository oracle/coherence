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
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;
import com.tangosol.internal.net.topic.impl.paged.agent.CloseSubscriptionProcessor;
import com.tangosol.internal.net.topic.impl.paged.model.Page;
import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.Session;
import com.tangosol.net.partition.PartitionSet;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.Binary;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.PartitionedFilter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber.withIdentifyingName;
import static com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber.withNotificationId;
import static com.tangosol.net.topic.Subscriber.completeOnEmpty;
import static com.tangosol.net.topic.Subscriber.inGroup;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class TopicSubscriberRaceTests
    {
    @BeforeClass
    public static void setup()
        {
        System.setProperty("coherence.distributed.partitioncount", "17");
        m_session = m_cluster.buildSession(SessionBuilders.storageDisabledMember());
        }

    @AfterClass
    public static void cleanup() throws Exception
        {
        if (m_session != null)
            {
            m_session.close();
            }
        }

    @Test
    @SuppressWarnings({"unchecked", "resource", "unused"})
    public void shouldNotSkipMessages() throws Exception
        {
        String             sTopicName = m_testWatcher.getMethodName();
        String             sGroup     = "test";
        NamedTopic<String> topic      = m_session.getTopic(sTopicName);
        long               cPublished = 0;

        // ensure the subscriber group exists
        topic.ensureSubscriberGroup(sGroup);

        // publish messages to channels 4 and 5
        try (Publisher<String> publisherOne = topic.createPublisher(Publisher.OrderBy.id(4));
             Publisher<String> publisherTwo = topic.createPublisher(Publisher.OrderBy.id(5)))
            {
            for (int i = 0; i < 5; i++)
                {
                publisherOne.publish("message-4" + i).get(1, TimeUnit.MINUTES);
                publisherTwo.publish("message-5" + i).get(1, TimeUnit.MINUTES);
                cPublished += 2;
                }
            }

        // We use some hard coded subscriber notification ids, so we can influence the channel allocations
        int[] anChannel = new int[]{64448927, 1430821611, 282253707, 1709181172, 1264892729, 1285671258,
                                    366450594, 1389964259, 902398731, 1172520457, 1657767738};


        ChannelListener channelAllocationListener = new ChannelListener();

        // Create the first five subscribers
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>)
                topic.createSubscriber(withIdentifyingName("one"), inGroup(sGroup), completeOnEmpty(), withNotificationId(anChannel[1]));
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>)
                             topic.createSubscriber(withIdentifyingName("two"), inGroup(sGroup), completeOnEmpty(), withNotificationId(anChannel[2]));
             PagedTopicSubscriber<String> subscriberThree = (PagedTopicSubscriber<String>)
                             topic.createSubscriber(withIdentifyingName("three"), inGroup(sGroup), completeOnEmpty(), withNotificationId(anChannel[3]));
             PagedTopicSubscriber<String> subscriberFour = (PagedTopicSubscriber<String>)
                             topic.createSubscriber(Subscriber.withListener(channelAllocationListener), withIdentifyingName("four"), inGroup(sGroup), completeOnEmpty(), withNotificationId(anChannel[4]));
             PagedTopicSubscriber<String> subscriberFive = (PagedTopicSubscriber<String>)
                             topic.createSubscriber(withIdentifyingName("five"), inGroup(sGroup), completeOnEmpty(), withNotificationId(anChannel[5])))
            {
            List<Subscriber.Element<String>> listElement = new ArrayList<>();

            // Receive an element from subscriber four - this will actually fetch back the whole page locally to the subscriber
            Subscriber.Element<String> element  = subscriberFour.receive().get(1, TimeUnit.MINUTES);
            listElement.add(element);

            // The message should be from channel 4
            assertThat(element, is(notNullValue()));
            assertThat(element.getChannel(), is(4));

            PagedTopicService        service  = (PagedTopicService) topic.getTopicService();
            BackingMapManagerContext context  = service.getBackingMapManager().getContext();
            int                      nChannel = element.getChannel();
            PagedPosition            position = (PagedPosition) element.getPosition();
            Page.Key                 key      = new Page.Key(element.getChannel(), position.getPage());
            Binary                   binKey   = (Binary) context.getKeyToInternalConverter().convert(key);
            int                      nPart    = context.getKeyPartition(binKey);

            // Close subscriber 1, causing a channel reallocation
            CompletableFuture<Void> channelFuture = channelAllocationListener.reset();
            subscriberOne.close();

            // wait for subscriber four to be notified of channel re-allocation
            channelFuture.get(1, TimeUnit.MINUTES);
            // subscriber four should still have ownership of channel 4
            assertThat(subscriberFour.isOwner(4), is(true));

            System.err.println();
            System.err.println(subscriberFour.getIdentifyingName() + " " + subscriberFour.getSubscriberId() + " channels: " + Arrays.toString(subscriberFour.getChannels()));

            // reset the channel allocation listener
            channelFuture = channelAllocationListener.reset();

            // create a new subscriber, which will cause channel re-allocation
            try (PagedTopicSubscriber<String> subscriberSix = (PagedTopicSubscriber<String>)
                    topic.createSubscriber(withIdentifyingName("six"), inGroup(sGroup), completeOnEmpty(), withNotificationId(anChannel[7])))
                {
                // wait for subscriber four to be notified of channel re-allocation
                channelFuture.get(1, TimeUnit.MINUTES);
                // subscriber four should still have ownership of channel 4
                assertThat(subscriberFour.isOwner(4), is(true));

                String                                     sCache         = PagedTopicCaches.Names.SUBSCRIPTIONS.cacheNameForTopicName(sTopicName);
                NamedCache<Subscription.Key, Subscription> cache          = service.ensureCache(sCache, null);
                CloseSubscriptionProcessor                 closeProcessor = new CloseSubscriptionProcessor(subscriberTwo.getSubscriberId());
                PartitionSet                               parts          = new PartitionSet(service.getPartitionCount()).fill();

                // reset the channel allocation listener
                channelFuture = channelAllocationListener.reset();

                // Close on partitions except the partition owning the previously received page
                // This causes the ownership of channel 4 to be lost by subscriber four on all partitions except
                // the partition owning the previously received page
                parts.remove(nPart);
                PartitionedFilter<?> filter = new PartitionedFilter<>(AlwaysFilter.INSTANCE(), parts);
                cache.invokeAll(filter, closeProcessor);

                // wait to ensure subscriber four is not notified of any channel re-allocation
                try
                    {
                    channelFuture.get(10, TimeUnit.SECONDS);
                    fail("Subscriber should not have had any allocation changes");
                    }
                catch (TimeoutException e)
                    {
                    // expected
                    }
                // subscriber four should own channel 4
                assertThat(subscriberFour.isOwner(4), is(true));

                // Receive another message using subscriber four.
                // This will clean out the pre-fetch cache of messages in un-owned channels
                element = subscriberFour.receive().get(1, TimeUnit.MINUTES);
                listElement.add(element);

                // reset the channel allocation listener
                channelFuture = channelAllocationListener.reset();

                // create a new subscriber, which will cause channel re-allocation
                try (PagedTopicSubscriber<String> subscriberSeven = (PagedTopicSubscriber<String>)
                                     topic.createSubscriber(withIdentifyingName("seven"), inGroup(sGroup), completeOnEmpty(), withNotificationId(anChannel[6])))
                    {
                    // wait for subscriber four to be notified of channel re-allocation
                    channelFuture.get(1, TimeUnit.MINUTES);
                    // subscriber four should have regained ownership of channel 4
                    assertThat(subscriberFour.isOwner(4), is(true));

                    // Close subscriber two for the partition owning the original received page
                    // Channel ownership will now be consistent across the cluster
                    cache.invoke(new Subscription.Key(nPart, nChannel, subscriberFour.getSubscriberGroupId()), closeProcessor);

                    // Receive all the elements from the topic using subscriber four
                    element = subscriberFour.receive().get(1, TimeUnit.MINUTES);
                    while (element != null)
                        {
                        listElement.add(element);
                        element = subscriberFour.receive().get(1, TimeUnit.MINUTES);
                        }

                    // all the published messages should have been received
                    long cMessage = listElement.stream()
                                            .filter(Objects::nonNull)
                                            .map(Subscriber.Element::getValue)
                                            .distinct()
                                            .count();

                    assertThat(cMessage, is(cPublished));
                    }
                }
            }
        }

    // ----- inner class: ChannelListener -----------------------------------

    private static class ChannelListener
            implements Subscriber.ChannelOwnershipListener
        {
        public ChannelListener()
            {
            reset();
            }

        public CompletableFuture<Void> reset()
            {
            m_future = new CompletableFuture<>();
            return m_future;
            }

        @Override
        public void onChannelsAssigned(Set<Integer> setAssigned)
            {
            m_future.complete(null);
            }

        @Override
        public void onChannelsRevoked(Set<Integer> setRevoked)
            {
            }

        @Override
        public void onChannelsLost(Set<Integer> setLost)
            {
            }

        private CompletableFuture<Void> m_future;
        }

    // ----- data members ---------------------------------------------------

    public static Session m_session;

    public static final int STORAGE_MEMBER_COUNT = 3;

    public static final String CLUSTER_NAME = "TopicSubscriberRaceTests";

    public static final String CACHE_CONFIG_FILE = "topics-channel-config.xml";

    @ClassRule(order = 0)
    public static TestLogs m_testLogs = new TestLogs(NamedTopicTests.class);

    @Rule(order = 1)
    public TestName m_testWatcher = new TestName();

    @ClassRule(order = 2)
    public static CoherenceClusterResource m_cluster =
            new CoherenceClusterResource()
                    .with(ClusterName.of(CLUSTER_NAME),
                          SystemProperty.of("coherence.distributed.partitioncount", "17"),
                          Logging.atFinest(),
                          CacheConfig.of(CACHE_CONFIG_FILE),
                          LocalHost.only(),
                          WellKnownAddress.loopback(),
                          IPv4Preferred.yes())
                    .include(STORAGE_MEMBER_COUNT,
                             CoherenceClusterMember.class,
                             DisplayName.of("Storage"),
                             RoleName.of("storage"),
                             JmxFeature.enabled(),
                             m_testLogs.builder());
    }
