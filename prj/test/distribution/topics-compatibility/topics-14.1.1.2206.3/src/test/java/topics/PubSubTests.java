/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.common.util.Threads;
import com.tangosol.net.topic.Subscriber;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tangosol.net.topic.Subscriber.inGroup;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings({"unchecked", "unused"})
public class PubSubTests
        extends BaseTopicsTests
    {
    @Test
    public void shouldUsePreviousPubAndSubWithCurrentCluster() throws Exception
        {
        shouldPubAndSub(Version.Current, Version.Previous);
        }

    @Test
    public void shouldUseCurrentPubAndSubWithPreviousCluster() throws Exception
        {
        shouldPubAndSub(Version.Previous, Version.Current);
        }

    @Test
    public void shouldUseCurrentPubAndSubOnMixedClusterWithPreviousSenior() throws Exception
        {
        try (ClosableCluster clusterSenior = createRunningCluster(createClusterName(), Version.Previous);
             ClosableCluster clusterOther  = createRunningCluster(createClusterName(), Version.Current))
            {
            shouldPubAndSub(clusterSenior, Version.Previous, Version.Current);
            }
        }

    @Test
    public void shouldUseCurrentPubAndSubOnMixedClusterWithCurrentSenior() throws Exception
        {
        try (ClosableCluster clusterSenior = createRunningCluster(createClusterName(), Version.Current);
             ClosableCluster clusterOther  = createRunningCluster(createClusterName(), Version.Previous))
            {
            shouldPubAndSub(clusterSenior, Version.Previous, Version.Current);
            }
        }

    @Test
    public void shouldUsePreviousPubAndSubOnMixedClusterWithPreviousSenior() throws Exception
        {
        try (ClosableCluster clusterSenior = createRunningCluster(createClusterName(), Version.Previous);
             ClosableCluster clusterOther  = createRunningCluster(createClusterName(), Version.Current))
            {
            shouldPubAndSub(clusterSenior, Version.Previous, Version.Previous);
            }
        }

    @Test
    public void shouldUsePreviousPubAndSubOnMixedClusterWithCurrentSenior() throws Exception
        {
        try (ClosableCluster clusterSenior = createRunningCluster(createClusterName(), Version.Current);
             ClosableCluster clusterOther  = createRunningCluster(createClusterName(), Version.Previous))
            {
            shouldPubAndSub(clusterSenior, Version.Previous, Version.Previous);
            }
        }

    @Test
    public void shouldUsePreviousPubAndMultipleSubsWithCurrentCluster() throws Exception
        {
        shouldPubWithMultipleSubs(Version.Current, Version.Previous);
        }

    @Test
    public void shouldUseCurrentPubAndMultipleSubsWithPreviousCluster() throws Exception
        {
        shouldPubWithMultipleSubs(Version.Previous, Version.Current);
        }

    @Test
    public void shouldUsePreviousPubAndMultipleSubsMixedClusterWithCurrentSenior() throws Exception
        {
        try (ClosableCluster clusterSenior = createRunningCluster(createClusterName(), Version.Current);
             ClosableCluster clusterOther  = createRunningCluster(createClusterName(), Version.Previous))
            {
            shouldPubWithMultipleSubs(clusterSenior, Version.Current, Version.Previous);
            }
        }

    @Test
    public void shouldUsePreviousPubAndMultipleSubsMixedClusterWithPreviousSenior() throws Exception
        {
        try (ClosableCluster clusterSenior = createRunningCluster(createClusterName(), Version.Previous);
             ClosableCluster clusterOther  = createRunningCluster(createClusterName(), Version.Current))
            {
            shouldPubWithMultipleSubs(clusterSenior, Version.Current, Version.Previous);
            }
        }

    @Test
    public void shouldUseCurrentPubAndMultipleSubsMixedClusterWithCurrentSenior() throws Exception
        {
        try (ClosableCluster clusterSenior = createRunningCluster(createClusterName(), Version.Current);
             ClosableCluster clusterOther  = createRunningCluster(createClusterName(), Version.Previous))
            {
            shouldPubWithMultipleSubs(clusterSenior, Version.Previous, Version.Current);
            }
        }

    @Test
    public void shouldUseCurrentPubAndMultipleSubsMixedClusterWithPreviousSenior() throws Exception
        {
        try (ClosableCluster clusterSenior = createRunningCluster(createClusterName(), Version.Previous);
             ClosableCluster clusterOther  = createRunningCluster(createClusterName(), Version.Current))
            {
            shouldPubWithMultipleSubs(clusterSenior, Version.Previous, Version.Current);
            }
        }

    @Test
    public void shouldUsePreviousPubAndMultipleSubsAndReallocationWithCurrentCluster() throws Exception
        {
        shouldPubWithMultipleSubsAndReallocation(Version.Current, Version.Previous);
        }

    @Test
    public void shouldUseCurrentPubAndMultipleSubsAndReallocationWithPreviousCluster() throws Exception
        {
        shouldPubWithMultipleSubsAndReallocation(Version.Previous, Version.Current);
        }

    @Test
    public void shouldUsePreviousPubAndMultipleSubsAndReallocationMixedClusterWithCurrentSenior() throws Exception
        {
        try (ClosableCluster clusterSenior = createRunningCluster(createClusterName(), Version.Current);
             ClosableCluster clusterOther  = createRunningCluster(createClusterName(), Version.Previous))
            {
            shouldPubWithMultipleSubsAndReallocation(clusterSenior, Version.Current, Version.Previous);
            }
        }

    @Test
    public void shouldUsePreviousPubAndMultipleSubsAndReallocationMixedClusterWithPreviousSenior() throws Exception
        {
        try (ClosableCluster clusterSenior = createRunningCluster(createClusterName(), Version.Previous);
             ClosableCluster clusterOther  = createRunningCluster(createClusterName(), Version.Current))
            {
            shouldPubWithMultipleSubsAndReallocation(clusterSenior, Version.Current, Version.Previous);
            }
        }

    @Test
    public void shouldUseCurrentPubAndMultipleSubsAndReallocationMixedClusterWithCurrentSenior() throws Exception
        {
        try (ClosableCluster clusterSenior = createRunningCluster(createClusterName(), Version.Current);
             ClosableCluster clusterOther  = createRunningCluster(createClusterName(), Version.Previous))
            {
            shouldPubWithMultipleSubsAndReallocation(clusterSenior, Version.Previous, Version.Current);
            }
        }

    @Test
    public void shouldUseCurrentPubAndMultipleSubsAndReallocationMixedClusterWithPreviousSenior() throws Exception
        {
        try (ClosableCluster clusterSenior = createRunningCluster(createClusterName(), Version.Previous);
             ClosableCluster clusterOther  = createRunningCluster(createClusterName(), Version.Current))
            {
            shouldPubWithMultipleSubsAndReallocation(clusterSenior, Version.Previous, Version.Current);
            }
        }

    // ----- helper methods -------------------------------------------------

    protected void shouldPubAndSub(Version clusterVersion, Version pubSubVersion) throws Exception
        {
        try (ClosableCluster cluster = createRunningCluster(createClusterName(), clusterVersion))
            {
            shouldPubAndSub(cluster, clusterVersion, pubSubVersion);
            }
        }

    protected void shouldPubAndSub(ClosableCluster cluster, Version clusterVersion, Version pubSubVersion) throws Exception
        {
        try (CoherenceClusterMember memberPub = cluster.startPublisher("PublisherOne", pubSubVersion.getClassPath());
             CoherenceClusterMember memberSub = cluster.startSubscriber("SubscriberOne", pubSubVersion.getClassPath()))
            {
            Eventually.assertDeferred(memberPub::isCoherenceRunning, is(true), Timeout.after(5, TimeUnit.MINUTES));
            Eventually.assertDeferred(memberSub::isCoherenceRunning, is(true), Timeout.after(5, TimeUnit.MINUTES));

            String sTopicName = "test-topic";
            String sGroupName = "test-group";

            TopicSubscriber.createSubscriber(memberSub, "Sub1", sTopicName, inGroup(sGroupName), Subscriber.CompleteOnEmpty.enabled());
            TopicPublisher.createPublisher(memberPub, "Pub1", sTopicName);

            CompletableFuture<Set<String>> futurePublish = TopicPublisher.publish(memberPub, "Pub1", 1000);
            Set<String>                    setPublish    = futurePublish.get(5, TimeUnit.MINUTES);
            CompletableFuture<Set<String>> futureReceive = TopicSubscriber.receive(memberSub, "Sub1");
            Set<String>                    setReceive    = futureReceive.get(5, TimeUnit.MINUTES);

            assertThat(setPublish.isEmpty(), is(false));
            assertThat(setReceive.isEmpty(), is(false));
            assertThat(setReceive.size(), is(setPublish.size()));
            }
        }

    protected void shouldPubWithMultipleSubs(Version clusterVersion, Version pubSubVersion) throws Exception
        {
        try (ClosableCluster cluster = createRunningCluster(createClusterName(), clusterVersion))
            {
            shouldPubWithMultipleSubs(cluster, clusterVersion, pubSubVersion);
            }
        }

    protected void shouldPubWithMultipleSubs(ClosableCluster cluster, Version clusterVersion, Version pubSubVersion) throws Exception
        {
        try (CoherenceClusterMember memberPub = cluster.startPublisher("PublisherOne", pubSubVersion.getClassPath());
             CoherenceClusterMember memberSub1 = cluster.startSubscriber("SubscriberOne", pubSubVersion.getClassPath());
             CoherenceClusterMember memberSub2 = cluster.startSubscriber("SubscriberTwo", pubSubVersion.getClassPath()))
            {
            Eventually.assertDeferred(memberPub::isCoherenceRunning, is(true), Timeout.after(5, TimeUnit.MINUTES));
            Eventually.assertDeferred(memberSub1::isCoherenceRunning, is(true), Timeout.after(5, TimeUnit.MINUTES));
            Eventually.assertDeferred(memberSub2::isCoherenceRunning, is(true), Timeout.after(5, TimeUnit.MINUTES));

            String sTopicName = "test-topic";
            String sGroupName = "test-group";

            TopicSubscriber.createSubscriber(memberSub1, "Sub1.1", sTopicName, inGroup(sGroupName), Subscriber.CompleteOnEmpty.enabled());
            TopicSubscriber.createSubscriber(memberSub1, "Sub1.2", sTopicName, inGroup(sGroupName), Subscriber.CompleteOnEmpty.enabled());
            TopicSubscriber.createSubscriber(memberSub2, "Sub2.1", sTopicName, inGroup(sGroupName), Subscriber.CompleteOnEmpty.enabled());
            TopicSubscriber.createSubscriber(memberSub2, "Sub2.2", sTopicName, inGroup(sGroupName), Subscriber.CompleteOnEmpty.enabled());
            TopicPublisher.createPublisher(memberPub, "Pub1", sTopicName);

            CompletableFuture<Set<String>> futurePublish  = TopicPublisher.publish(memberPub, "Pub1", 1000);
            Set<String>                    setPublish     = futurePublish.get(5, TimeUnit.MINUTES);
            CompletableFuture<Set<String>> futureReceive1 = TopicSubscriber.receive(memberSub1, "Sub1.1");
            CompletableFuture<Set<String>> futureReceive2 = TopicSubscriber.receive(memberSub1, "Sub1.2");
            CompletableFuture<Set<String>> futureReceive3 = TopicSubscriber.receive(memberSub2, "Sub2.1");
            CompletableFuture<Set<String>> futureReceive4 = TopicSubscriber.receive(memberSub2, "Sub2.2");
            Set<String>                    setReceive1    = futureReceive1.get(5, TimeUnit.MINUTES);
            Set<String>                    setReceive2    = futureReceive2.get(5, TimeUnit.MINUTES);
            Set<String>                    setReceive3    = futureReceive3.get(5, TimeUnit.MINUTES);
            Set<String>                    setReceive4    = futureReceive4.get(5, TimeUnit.MINUTES);

            Set<String> setReceive = new HashSet<>();
            setReceive.addAll(setReceive1);
            setReceive.addAll(setReceive2);
            setReceive.addAll(setReceive3);
            setReceive.addAll(setReceive4);

            assertThat(setPublish.isEmpty(), is(false));
            assertThat(setReceive.isEmpty(), is(false));
            assertThat(setReceive.size(), is(setPublish.size()));
            }
        }

    protected void shouldPubWithMultipleSubsAndReallocation(Version clusterVersion, Version pubSubVersion) throws Exception
        {
        try (ClosableCluster cluster = createRunningCluster(createClusterName(), clusterVersion))
            {
            shouldPubWithMultipleSubsAndReallocation(cluster, clusterVersion, pubSubVersion);
            }
        }

    protected void shouldPubWithMultipleSubsAndReallocation(ClosableCluster cluster, Version clusterVersion, Version pubSubVersion) throws Exception
        {
        try (CoherenceClusterMember memberPub  = cluster.startPublisher("PublisherOne", pubSubVersion.getClassPath());
             CoherenceClusterMember memberSub1 = cluster.startSubscriber("SubscriberOne", pubSubVersion.getClassPath());
             CoherenceClusterMember memberSub2 = cluster.startSubscriber("SubscriberTwo", pubSubVersion.getClassPath()))
            {
            Eventually.assertDeferred(memberPub::isCoherenceRunning, is(true), Timeout.after(5, TimeUnit.MINUTES));
            Eventually.assertDeferred(memberSub1::isCoherenceRunning, is(true), Timeout.after(5, TimeUnit.MINUTES));
            Eventually.assertDeferred(memberSub2::isCoherenceRunning, is(true), Timeout.after(5, TimeUnit.MINUTES));

            cluster.log(">>>> Starting shouldPubWithMultipleSubsAndReallocation");

            String sTopicName = "test-topic";
            String sGroupName = "test-group";

            cluster.log(">>>> In shouldPubWithMultipleSubsAndReallocation - creating subscribers");

            try
                {
                TopicSubscriber.createSubscriber(memberSub1, "Sub1.1", sTopicName, inGroup(sGroupName), Subscriber.CompleteOnEmpty.enabled());
                TopicSubscriber.createSubscriber(memberSub1, "Sub1.2", sTopicName, inGroup(sGroupName), Subscriber.CompleteOnEmpty.enabled());
                TopicSubscriber.createSubscriber(memberSub2, "Sub2.1", sTopicName, inGroup(sGroupName), Subscriber.CompleteOnEmpty.enabled());
                TopicSubscriber.createSubscriber(memberSub2, "Sub2.2", sTopicName, inGroup(sGroupName), Subscriber.CompleteOnEmpty.enabled());

                cluster.log(">>>> In shouldPubWithMultipleSubsAndReallocation - creating publisher");
                TopicPublisher.createPublisher(memberPub, "Pub1", sTopicName);

                cluster.log(">>>> In shouldPubWithMultipleSubsAndReallocation - starting publisher");
                CompletableFuture<Set<String>> futurePublish  = TopicPublisher.publish(memberPub, "Pub1", 1700);
                Set<String>                    setPublish     = futurePublish.get(5, TimeUnit.MINUTES);
                cluster.log(">>>> In shouldPubWithMultipleSubsAndReallocation - finished publishing " + setPublish.size() + " messages");

                cluster.log(">>>> In shouldPubWithMultipleSubsAndReallocation - starting subscribers");
                CompletableFuture<Set<String>> futureReceive1 = TopicSubscriber.receive(memberSub1, "Sub1.1", 10);
                CompletableFuture<Set<String>> futureReceive2 = TopicSubscriber.receive(memberSub1, "Sub1.2", 10);
                CompletableFuture<Set<String>> futureReceive3 = TopicSubscriber.receive(memberSub2, "Sub2.1", 10);
                CompletableFuture<Set<String>> futureReceive4 = TopicSubscriber.receive(memberSub2, "Sub2.2", 10);
                Set<String>                    setReceive1    = futureReceive1.get(5, TimeUnit.MINUTES);
                Set<String>                    setReceive2    = futureReceive2.get(5, TimeUnit.MINUTES);
                Set<String>                    setReceive3    = futureReceive3.get(5, TimeUnit.MINUTES);
                Set<String>                    setReceive4    = futureReceive4.get(5, TimeUnit.MINUTES);
                cluster.log(">>>> In shouldPubWithMultipleSubsAndReallocation - finished subscribers - 1.1 = " + setReceive1.size());
                cluster.log(">>>> In shouldPubWithMultipleSubsAndReallocation - finished subscribers - 1.2 = " + setReceive2.size());
                cluster.log(">>>> In shouldPubWithMultipleSubsAndReallocation - finished subscribers - 2.1 = " + setReceive3.size());
                cluster.log(">>>> In shouldPubWithMultipleSubsAndReallocation - finished subscribers - 2.2 = " + setReceive4.size());

                Set<String> setReceive = new HashSet<>();
                setReceive.addAll(setReceive1);
                setReceive.addAll(setReceive2);
                setReceive.addAll(setReceive3);
                setReceive.addAll(setReceive4);

                cluster.log(">>>> In shouldPubWithMultipleSubsAndReallocation - closing subscribers");
                TopicSubscriber.closeSubscriber(memberSub1, "Sub1.1");
                TopicSubscriber.closeSubscriber(memberSub2, "Sub2.1");
                cluster.log(">>>> In shouldPubWithMultipleSubsAndReallocation - closed subscribers");

                Eventually.assertDeferred(() -> TopicSubscriber.getChannelCount(memberSub1, "Sub1.2")
                        + TopicSubscriber.getChannelCount(memberSub2, "Sub2.2"), is(17), Timeout.after(5, TimeUnit.MINUTES));

                cluster.log(">>>> In shouldPubWithMultipleSubsAndReallocation - subscriber have all channels");

                futureReceive2 = TopicSubscriber.receive(memberSub1, "Sub1.2");
                futureReceive4 = TopicSubscriber.receive(memberSub2, "Sub2.2");

                cluster.log(">>>> In shouldPubWithMultipleSubsAndReallocation - received all messages");

                Set<String> setReceive5    = futureReceive2.get(5, TimeUnit.MINUTES);
                Set<String> setReceive6    = futureReceive4.get(5, TimeUnit.MINUTES);

                cluster.log(">>>> In shouldPubWithMultipleSubsAndReallocation - received all messages - 1.2 = " + setReceive5.size());
                cluster.log(">>>> In shouldPubWithMultipleSubsAndReallocation - received all messages - 2.2 = " + setReceive6.size());

                setReceive = new HashSet<>();
                setReceive.addAll(setReceive1);
                setReceive.addAll(setReceive2);
                setReceive.addAll(setReceive3);
                setReceive.addAll(setReceive4);
                setReceive.addAll(setReceive5);
                setReceive.addAll(setReceive6);

                assertThat(setPublish.isEmpty(), is(false));
                assertThat(setReceive.isEmpty(), is(false));
                assertThat(setReceive.size(), is(setPublish.size()));

                cluster.log(">>>> Finished shouldPubWithMultipleSubsAndReallocation");
                }
            catch (TimeoutException e)
                {
                cluster.threadDump();
                System.err.println("Test timed out: ");
                System.err.println(Threads.getThreadDump(true));
                fail("Test failed with exception: " + e.getMessage());
                }
            }
        }

    public static String createClusterName()
        {
        return PubSubTests.class.getSimpleName() + "-" + m_cCluster.incrementAndGet();
        }

    // ----- data members ---------------------------------------------------

    private static final AtomicInteger m_cCluster = new AtomicInteger();
    }
