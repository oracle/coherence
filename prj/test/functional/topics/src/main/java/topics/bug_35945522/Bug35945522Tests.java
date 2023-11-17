/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics.bug_35945522;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.options.LaunchLogging;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.concurrent.runnable.ThreadDump;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.HeapSize;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import org.junit.ClassRule;
import org.junit.Test;
import topics.NamedTopicTests;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;

/**
 * This test is related to BUG 35945522.
 * </p>
 * Specifically, the bug related to a QA test where 17 subscriber JVMs were started,
 * one for each channel. A publisher was started that published, round-robin, to
 * each channel. After a short time, a subscriber was killed. The issue was that
 * after reallocation of channels, some of the subscribers stopped receiving any
 * messages, even though they had channel allocations (which had changed).
 */
@SuppressWarnings("resource")
public class Bug35945522Tests
    {
    @Test
    public void shouldContinueToReceive() throws Exception
        {
        Eventually.assertDeferred(this::getChannelCount, is(not(-1)));
        int cChannel = getChannelCount();
        assertThat(cChannel, is(not(-1)));

        CoherenceClusterBuilder builder = new CoherenceClusterBuilder()
                .with(cluster.getCommonOptions())
                .include(cChannel, CoherenceClusterMember.class,
                        ClassName.of(SubscriberMain.class),
                        LocalStorage.disabled(),
                        DisplayName.of("subscriber"),
                        RoleName.of("subscriber"))
                .include(1, CoherenceClusterMember.class,
                        ClassName.of(PublisherMain.class),
                        LocalStorage.disabled(),
                        DisplayName.of("publisher"),
                        RoleName.of("publisher"));

        CoherenceCluster pubSub = builder.build();

        CoherenceClusterMember publisher = pubSub.stream()
                                                    .filter(m -> m.getName()
                                                    .startsWith("publisher"))
                                                    .findFirst()
                                                    .orElse(null);

        assertThat(publisher, is(notNullValue()));

        TreeMap<Long, CoherenceClusterMember> mapSub = new TreeMap<>();

        // assert member is subscribed - it has a subscriber id
        for (CoherenceClusterMember member : pubSub.getAll("subscriber"))
            {
            Eventually.assertDeferred(() -> member.invoke(SubscriberMain.GET_SUBSCRIBER_ID), is(notNullValue()));
            mapSub.put(member.invoke(SubscriberMain.GET_SUBSCRIBER_ID), member);
            }

        // assert each subscriber has one channel
        for (CoherenceClusterMember member : mapSub.values())
            {
            Eventually.assertDeferred(() -> member.invoke(SubscriberMain.GET_CHANNEL_COUNT), is(1));
            }

        // publish 2 messages to each channel
        System.err.println("***** Publishing first message");
        assertThat(publisher.invoke(new PublisherMain.Publish()), is(cChannel));
        System.err.println("***** Publishing second message");
        assertThat(publisher.invoke(new PublisherMain.Publish()), is(cChannel));

        // wait for the subscribers to receive them
        for (CoherenceClusterMember member : mapSub.values())
            {
            System.err.println("***** Checking member " + member.getName() + " for two messages");
            try
                {
                Eventually.assertDeferred(() -> member.invoke(SubscriberMain.GET_RECEIVED_COUNT), is(2));
                }
            catch (AssertionError e)
                {
                member.submit(ThreadDump.toStdErr()).get(5, TimeUnit.MINUTES);
                throw e;
                }
            }

        // kill a subscriber from the middle of the set of subscribers
        Map.Entry<Long, CoherenceClusterMember> entryKill = mapSub.entrySet().stream()
                                        .skip(10)
                                        .findFirst()
                                        .orElse(null);

        assertThat(entryKill, is(notNullValue()));
        entryKill.getValue().close();

        mapSub.remove(entryKill.getKey());

        // publish 2 more messages to each channel
        assertThat(publisher.invoke(new PublisherMain.Publish()), is(cChannel));
        assertThat(publisher.invoke(new PublisherMain.Publish()), is(cChannel));

        // wait for the remaining subscribers to receive more messages
        for (CoherenceClusterMember member : mapSub.values())
            {
            Eventually.assertDeferred(() -> member.invoke(SubscriberMain.GET_RECEIVED_COUNT), is(greaterThan(2)));
            }
        }

    private int getChannelCount()
        {
        return cluster.getCluster()
                .findAny()
                .map(member -> member.invoke(new PublisherMain.GetChannelCount()))
                .orElse(-1);
        }

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(NamedTopicTests.class);

    @ClassRule
    public static CoherenceClusterResource cluster =
            new CoherenceClusterResource()
                    .with(ClusterName.of("Bug35945522Tests"),
                            HeapSize.of(64, HeapSize.Units.MB, 512, HeapSize.Units.MB),
                            Logging.atMax(),
                            LocalHost.only(),
                            WellKnownAddress.loopback(),
                            IPv4Preferred.yes(),
                            s_testLogs,
                            LaunchLogging.disabled(),
                            StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()))
                    .include(3,
                             CoherenceClusterMember.class,
                             DisplayName.of("storage"),
                             RoleName.of("storage"));
    }
