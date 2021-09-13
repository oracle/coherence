/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.bedrock.runtime.java.options.ClassName;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;

import com.tangosol.net.Coherence;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Session;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Subscriber;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static com.tangosol.net.topic.Subscriber.Name.inGroup;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

/**
 * Testing that topic subscribers are cleaned up if the member they are running
 * on leaves the cluster.
 *
 * @author Jonathan Knight  2021.09.09
 */
@SuppressWarnings("unchecked")
public class TopicSubscribeCleanupTests
    {
    @BeforeClass
    public static void setup()
        {
        System.setProperty(LocalStorage.PROPERTY, "true");

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
    public void shouldCloseSubscribersOnMemberDeparture()
        {
        LocalPlatform platform = LocalPlatform.get();
        try (CoherenceClusterMember member = platform.launch(CoherenceClusterMember.class,
                                                        LocalStorage.enabled(),
                                                        ClassName.of(Coherence.class),
                                                        s_testLogs.builder()))
            {
            NamedTopic<String> topic = s_session.getTopic("test");
            DistributedCacheService service = (DistributedCacheService) topic.getService();
            Eventually.assertDeferred(() -> service.getOwnershipEnabledMembers().size(), is(2));

            PagedTopicCaches caches = new PagedTopicCaches(topic.getName(), service);

            topic.ensureSubscriberGroup("group-one");
            topic.ensureSubscriberGroup("group-two");
            int cChannel = topic.getChannelCount();
            try (Subscriber<String> subscriber = topic.createSubscriber(inGroup("group-one")))
                {
                // there is currently one subscribe that should have all channels
                Eventually.assertDeferred(() -> subscriber.getChannels().length, is(cChannel));
                Eventually.assertDeferred(() -> caches.Subscribers.size(), is(1));

                // create some more subscribers on the remote member
                int cNewSubscriber = member.invoke(new CreateSubscriber());

                // there should eventually be the correct number of subscribers
                // and the local subscriber shoul dnot own all the channels
                Eventually.assertDeferred(() -> subscriber.getChannels().length, is(not(cChannel)));
                Eventually.assertDeferred(() -> caches.Subscribers.size(), is(1 + cNewSubscriber));

                // close the remote member, which should trigger subscriber clean-up
                member.close();

                // Eventually there will just be the local subscriber owning all channels
                Eventually.assertDeferred(() -> caches.Subscribers.size(), is(1));
                Eventually.assertDeferred(() -> subscriber.getChannels().length, is(cChannel));
                }
            }
        }

    /**
     * A {@link RemoteCallable} to create some more subscribers on a remote member.
     */
    public static class CreateSubscriber
            implements RemoteCallable<Integer>
        {
        @Override
        public Integer call()
            {
            Session            session = Coherence.getInstance().getSession();
            NamedTopic<String> topic   = session.getTopic("test");
            topic.createSubscriber(inGroup("group-one"));
            topic.createSubscriber(inGroup("group-one"));
            topic.createSubscriber(inGroup("group-two"));
            topic.createSubscriber(inGroup("group-two"));
            return 4;
            }
        }

    // ----- data members ---------------------------------------------------

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(TopicsRecoveryTests.class);

    private static Coherence s_coherence;

    private static Session s_session;
    }
