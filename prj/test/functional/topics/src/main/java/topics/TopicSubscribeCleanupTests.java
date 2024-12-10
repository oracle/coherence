/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.bedrock.runtime.java.options.ClassName;

import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberInfo;
import com.tangosol.net.Coherence;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.Session;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Subscriber;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.concurrent.TimeUnit;

import static com.tangosol.net.topic.Subscriber.Name.inGroup;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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
    public static void setup() throws Exception
        {
        System.setProperty(LocalStorage.PROPERTY, "true");
        System.setProperty(Logging.PROPERTY_LEVEL, "9");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.cluster", "TopicSubscribeCleanupTests");

        s_coherence = Coherence.clusterMember();
        s_coherence.start().get(5, TimeUnit.MINUTES);
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
    public void shouldRemoveSubscriberInfoOnClose()
        {
        NamedTopic<String> topic   = s_session.getTopic(f_testName.getMethodName());
        PagedTopicService  service = (PagedTopicService) topic.getService();
        PagedTopicCaches   caches  = new PagedTopicCaches(topic.getName(), service);

        assertThat(caches.Subscribers.isEmpty(), is(true));

        try (PagedTopicSubscriber<String> subscriber = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("group-one")))
            {
            SubscriberInfo info = caches.Subscribers.get(subscriber.getKey());
            assertThat(info, is(notNullValue()));

            subscriber.close();

            assertThat(caches.Subscribers.isEmpty(), is(true));
            }
        }

    @Test
    public void shouldCloseSubscribersOnMemberDeparture() throws Exception
        {
        LocalPlatform platform = LocalPlatform.get();
        try (CoherenceClusterMember member = platform.launch(CoherenceClusterMember.class,
                                                        LocalStorage.enabled(),
                                                        WellKnownAddress.loopback(),
                                                        LocalHost.only(),
                                                        ClassName.of(Coherence.class),
                                                        ClusterName.of("TopicSubscribeCleanupTests"),
                                                        IPv4Preferred.no(),
                                                        Logging.atMax(),
                                                        s_testLogs.builder()))
            {
            NamedTopic<String> topic   = s_session.getTopic(f_testName.getMethodName());
            PagedTopicService  service = (PagedTopicService) topic.getService();
            Eventually.assertDeferred(() -> service.getOwnershipEnabledMembers().size(), is(2));

            PagedTopicCaches caches = new PagedTopicCaches(topic.getName(), service);

            topic.ensureSubscriberGroup("group-one");
            topic.ensureSubscriberGroup("group-two");
            int cChannel = topic.getChannelCount();
            try (Subscriber<String> subscriberOne = topic.createSubscriber(inGroup("group-one")))
                {
                // call receive so we reconnect if disconnected
                subscriberOne.receive();
                
                // there is currently one subscribe that should have all channels
                Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(cChannel));
                Eventually.assertDeferred(() -> caches.Subscribers.size(), is(1));

                System.err.println("***** Subscriber has all channels");
                System.err.println("***** " + subscriberOne);

                System.err.println("***** Creating subscribers on member " + member.getLocalMemberId());
                // create some more subscribers on the remote member
                int cExistingSubscriber = caches.Subscribers.size();
                int cNewSubscriber      = member.invoke(new CreateSubscriber(topic.getName()));
                int cSubscriber         = cExistingSubscriber + cNewSubscriber;

                // there should eventually be the correct number of subscribers
                // and the local subscriber should not own all the channels
                Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(not(cChannel)));
                Eventually.assertDeferred(() -> caches.Subscribers.size(), is(cSubscriber));

                Thread.sleep(5000);
                System.err.println("***** Subscriber channels reallocated");
                System.err.println("***** " + subscriberOne);

                System.err.println("***** Closing member " + member.getLocalMemberId());
                // close the remote member, which should trigger subscriber clean-up
                member.close();

                // Eventually there will just be the local subscriber owning all channels
                System.err.println("Waiting for only one subscriber");
                Eventually.assertDeferred(() -> caches.Subscribers.size(), is(1));
                System.err.println("Waiting for subscriber to have all channels");
                System.err.println("----------------");
                System.err.println(subscriberOne);
                System.err.println("----------------");
                Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(cChannel));
                System.err.println("Done!!!");
                }
            }
        }

    /**
     * A {@link RemoteCallable} to create some more subscribers on a remote member.
     */
    public static class CreateSubscriber
            implements RemoteCallable<Integer>
        {
        public CreateSubscriber(String sTopicName)
            {
            this.f_sTopicName = sTopicName;
            }

        @Override
        public Integer call()
            {
            System.err.println("***** Creating subscribers on topic " + f_sTopicName);
            Session            session = Coherence.getInstance().getSession();
            NamedTopic<String> topic   = session.getTopic(f_sTopicName);
            topic.createSubscriber(inGroup("group-one"));
            System.err.println("***** Created first subscriber on topic " + f_sTopicName + " group-one");
            topic.createSubscriber(inGroup("group-one"));
            System.err.println("***** Created second subscriber on topic " + f_sTopicName + " group-one");
            topic.createSubscriber(inGroup("group-two"));
            System.err.println("***** Created first subscriber on topic " + f_sTopicName + " group-two");
            topic.createSubscriber(inGroup("group-two"));
            System.err.println("***** Created second subscriber on topic " + f_sTopicName + " group-two");
            return 4;
            }

        private final String f_sTopicName;
        }

    // ----- data members ---------------------------------------------------

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(TopicsRecoveryTests.class);

    @Rule
    public final TestName f_testName = new TestName();

    private static Coherence s_coherence;

    private static Session s_session;
    }
