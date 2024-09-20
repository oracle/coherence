/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package topics;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
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
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.function.Remote;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import topics.callables.CreateSubscriber;
import topics.callables.EnsureTopic;
import topics.callables.GetTopicChannelCount;
import topics.callables.PublishMessages;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TopicChannelCountRecoveryTests
    {
    @Test
    public void shouldRecoverCorrectChannelCount() throws Exception
        {
        String sMethodName       = m_testName.getMethodName();
        String sTopic            = "simple-persistent-topic-test";
        int    cStorageCount     = 3;
        int    cChannelInitial   = 7;
        int    cChannelIncreased = 10;

        // make sure persistence files are not left from a previous test
        File filePersistence = new File("target/store-bdb-active/" + sMethodName);
        if (filePersistence.exists())
            {
            MavenProjectFileUtils.recursiveDelete(filePersistence);
            }

        System.err.println("Starting cluster for " + sMethodName);
        CoherenceCluster cluster = startCluster("initial", true, cStorageCount, "storage");

        Eventually.assertDeferred(cluster::isReady, is(true));
        System.err.println("Cluster for " + sMethodName + " is ready");

        // Get one of the storage members
        CoherenceClusterMember member = cluster.stream().findAny().orElse(null);
        assertThat(member, is(notNullValue()));

        assertThat(member.invoke(new EnsureTopic(sTopic, "test")), is(true));
        assertThat(member.invoke(new GetTopicChannelCount(sTopic)), is(cChannelInitial));

        member.invoke(new PublishMessages(sTopic, 50).withChannelCount(cChannelIncreased));
        assertThat(member.invoke(new GetTopicChannelCount(sTopic)), is(cChannelIncreased));

        // shutdown the storage members
        Logger.info("Stopping storage.");
        cluster.close();

        // Start a storage disabled subscriber
        CoherenceCluster subscribers = startCluster("subscriber", false, 1, "subscriber");
        Eventually.assertDeferred(subscribers::isReady, is(true));
        // Get the subscriber cluster member
        CoherenceClusterMember memberSub = subscribers.stream().findAny().orElse(null);
        assertThat(memberSub, is(notNullValue()));
        CompletableFuture<SubscriberId> future = memberSub.submit(new CreateSubscriber(sTopic, "test"));

        cluster = startCluster("restart", true, cStorageCount, "storage");
        Eventually.assertDeferred(cluster::isReady, is(true));
        System.err.println("Cluster for " + sMethodName + " is ready");

        member = cluster.stream().findAny().orElse(null);
        assertThat(member, is(notNullValue()));
        assertThat(member.invoke(new GetTopicChannelCount(sTopic)), is(cChannelIncreased));

        SubscriberId subscriberId = future.get(5, TimeUnit.MINUTES);
        assertThat(subscriberId, is(notNullValue()));

        // the topic in the subscriber JVM should eventually have the correct channel count
        Eventually.assertDeferred(() -> CreateSubscriber.apply(memberSub, subscriberId, Subscriber::getChannelCount),
                is(cChannelIncreased));

        // call receive on the subscriber so that we ensure it is connected and should be allocated all channels
        CreateSubscriber.apply(memberSub, subscriberId, sub ->
            {
            sub.receive();
            return null;
            });

        // The subscriber should be allocated the correct count of channels
        Eventually.assertDeferred(() -> CreateSubscriber.apply(memberSub, subscriberId, sub ->
                    {
                    int[] anChannel = sub.getChannels();
                    return anChannel == null ? 0 : anChannel.length;
                    }),
                is(cChannelIncreased));

        }


    // ----- helper methods -------------------------------------------------

    private CoherenceCluster startCluster(String suffix, boolean fStorage, int cMember, String sRole)
        {
        CoherenceClusterBuilder builder     = new CoherenceClusterBuilder();
        String                  sMethodName = m_testName.getMethodName();
        OptionsByType           options     = OptionsByType.of(s_options)
                                                .addAll(OperationalOverride.of("common-tangosol-coherence-override.xml"),
                                                        CacheConfig.of("simple-persistence-bdb-cache-config.xml"),
                                                        SystemProperty.of("test.log.level", "9"),
                                                        SystemProperty.of("test.log", "stderr"),
                                                        SystemProperty.of("coherence.distributed.partitioncount", "13"),
                                                        StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()),
                                                        LocalStorage.enabled(fStorage),
                                                        Logging.atMax(),
                                                        JMXManagementMode.ALL,
                                                        DisplayName.of(sRole + '-' + suffix),
                                                        RoleName.of(sRole),
                                                        s_testLogs.builder());

        builder.with(ClusterName.of(sMethodName))
               .include(cMember, CoherenceClusterMember.class, options.asArray());

        return builder.build(LocalPlatform.get());
        }

    // ----- data members ---------------------------------------------------

    @Rule
    public final TestName m_testName = new TestName();

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(TopicsRecoveryTests.class);

    private static final OptionsByType s_options = OptionsByType.of(
            SystemProperty.of("coherence.guard.timeout", 60000),
            CacheConfig.of("simple-persistence-bdb-cache-config.xml"),
            OperationalOverride.of("common-tangosol-coherence-override.xml"),
            SystemProperty.of("coherence.distributed.partitioncount", "13"));
    }
