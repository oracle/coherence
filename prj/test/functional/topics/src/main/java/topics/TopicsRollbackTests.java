/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package topics;

import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings({"unchecked", "resource"})
public class TopicsRollbackTests
    {
    @BeforeClass
    public static void setup()
        {
        System.setProperty(LocalStorage.PROPERTY, "true");
        System.setProperty(Logging.PROPERTY_LEVEL, "7");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.cluster", "TopicsRollbackTests");
        }

    @Test
    public void shouldRollbackOnFailover() throws Exception
        {
        Coherence coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);
        Session   session   = coherence.getSession();
        String    sTopic    = "test-topic";
        String    sGroup    = "test-group";
        int       cMessage  = 300;

        NamedTopic<String> topic = session.getTopic(sTopic);
        topic.ensureSubscriberGroup(sGroup);

        try (Publisher<String> publisher = topic.createPublisher(Publisher.OrderBy.roundRobin()))
            {
            for (int i = 0; i < cMessage; i++)
                {
                publisher.publish("message-" + i).get(5, TimeUnit.MINUTES);
                }
            }

        CoherenceClusterMember member0 = startSubscriber(0);
        CoherenceClusterMember member1 = startSubscriber(1);
        CoherenceClusterMember member2 = startSubscriber(2);

        // Receive 10 messages on each member and commit them
        List<String> list0_1 = member0.invoke(new SubscriberRunner(sTopic, sGroup, 10, true));
        List<String> list1_1 = member1.invoke(new SubscriberRunner(sTopic, sGroup, 10, true));
        List<String> list2_1 = member2.invoke(new SubscriberRunner(sTopic, sGroup, 10, true));

        // Receive 10 more messages on member 1 WITHOUT committing
        member1.invoke(new SubscriberRunner(sTopic, sGroup, 10, false));
        // kill member 1
        member1.close();
        member1.waitFor(Timeout.after(5, TimeUnit.MINUTES));

        // Receive 10 more messages on member 2 WITHOUT committing
        member2.invoke(new SubscriberRunner(sTopic, sGroup, 10, false));
        // kill member 2
        member2.close();
        member2.waitFor(Timeout.after(5, TimeUnit.MINUTES));

        // ensure the remaining subscriber owns all channels
        Eventually.assertDeferred(() -> member0.invoke(new GetSubscriberChannels()), is(17));
        // Receive everything from the remaining member
        List<String> list0_2 = member0.invoke(new SubscriberRunner(sTopic, sGroup, cMessage, false));

        Set<String> setMessages = new HashSet<>();
        setMessages.addAll(list0_1);
        setMessages.addAll(list1_1);
        setMessages.addAll(list2_1);
        setMessages.addAll(list0_2);
        assertThat(setMessages.size(), is(cMessage));
        }

    private CoherenceClusterMember startSubscriber(int id)
        {
        LocalPlatform platform = LocalPlatform.get();
        CoherenceClusterMember member = platform.launch(CoherenceClusterMember.class,
                DisplayName.of("Subscriber-" + id),
                ClusterName.of("TopicsRollbackTests"),
                RoleName.of("subscriber"),
                LocalStorage.disabled(),
                Logging.atFinest(),
                LocalHost.only(),
                IPv4Preferred.autoDetect(),
                WellKnownAddress.loopback(),
                m_logs);
        Eventually.assertDeferred(member::isCoherenceRunning, is(true));
        return member;
        }

    // ----- inner class: SubscriberRunner ----------------------------------

    public static class SubscriberRunner
            implements RemoteCallable<List<String>>
        {
        public SubscriberRunner(String sTopic, String sGroup, int cMessage, boolean fCommit)
            {
            m_sTopic   = sTopic;
            m_sGroup   = sGroup;
            m_cMessage = cMessage;
            m_fCommit  = fCommit;
            }

        @Override
        @SuppressWarnings("resource")
        public List<String> call() throws Exception
            {
            List<String>       list       = new ArrayList<>();
            Subscriber<String> subscriber = ensureSubscriber(m_sTopic, m_sGroup);
            for (int i = 0; i < m_cMessage; i++)
                {
                Subscriber.Element<String> element = subscriber.receive().get(5, TimeUnit.MINUTES);
                if (element == null)
                    {
                    break;
                    }
                list.add(element.getValue());
                if (m_fCommit)
                    {
                    element.commit();
                    }
                }
            return list;
            }

        static Subscriber<String> ensureSubscriber(String sTopic, String sGroup)
            {
            Subscriber<String> subscriber = m_subscriber;
            if (subscriber == null)
                {
                Session session = Coherence.getInstance().getSession();
                subscriber = m_subscriber = session.createSubscriber(sTopic,
                        Subscriber.inGroup(sGroup), Subscriber.completeOnEmpty());
                }
            return subscriber;
            }

        protected static Subscriber<String> m_subscriber;

        private final String m_sTopic;

        private final String m_sGroup;

        private final int m_cMessage;

        private boolean m_fCommit;
        }

    // ----- inner class: GetSubscriberChannels -----------------------------

    public static class GetSubscriberChannels
            implements RemoteCallable<Integer>
        {
        @Override
        public Integer call() throws Exception
            {
            Subscriber<?> subscriber = SubscriberRunner.m_subscriber;
            if (subscriber == null)
                {
                return 0;
                }
            return subscriber.getChannels().length;
            }
        }


    // ----- data members ---------------------------------------------------

    @ClassRule
    public static final TestLogs m_logs = new TestLogs(TopicsRollbackTests.class);
    }
