/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.base.Timeout;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Base;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class TopicsRestartTests
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
    @SuppressWarnings("unchecked")
    public void shouldRemoveAnonymousSubscribersFromDepartedMembers() throws Exception
        {
        String             sTopicName = "test-topic";
        NamedTopic<String> topic      = s_session.getTopic(sTopicName);
        PagedTopicCaches   caches     = new PagedTopicCaches(topic.getName(), (PagedTopicService) topic.getService());
        LocalPlatform      platform   = LocalPlatform.get();

        // start a storage disabled member
        try (CoherenceClusterMember member = platform.launch(CoherenceClusterMember.class, LocalStorage.disabled(), DisplayName.of("client")))
            {
            Eventually.assertDeferred(() -> CacheFactory.getCluster().getMemberSet().size(), is(2));
            Eventually.assertDeferred(() -> member.isServiceRunning("PartitionedTopic"), is(true));

            // should have zero subscriptions
            assertThat(caches.Subscriptions.isEmpty(), is(true));

            // Remotely invoke creation of a subscriber on the storage disabled member we just started
            member.invoke(() ->
                    {
                    try
                        {
                        Logger.info("Creating subscriber for topic " + sTopicName);
                        Session                    session    = Session.create();
                        NamedTopic<String>         namedTopic = session.getTopic(sTopicName);
                        Subscriber<String>         subscriber = namedTopic.createSubscriber(Subscriber.CompleteOnEmpty.enabled());
                        Subscriber.Element<String> element    = subscriber.receive().get(1, TimeUnit.MINUTES);
                        Logger.info("Created subscriber for topic " + sTopicName + " received " + element);
                        return true;
                        }
                    catch (Throwable e)
                        {
                        e.printStackTrace();
                        return false;
                        }
                    });

            // exiting the try block will stop the cluster member
            }

        // should now have an anonymous subscription
        // should have some subscriptions in the cache (one per channel per partition)
        assertThat(caches.Subscriptions.isEmpty(), is(false));

        // The anonymous subscriber created in the dead member will never come back and needs to be cleaned up
        // We can trigger cleanup by publishing messages, eventually as we hit all of the partitions
        // the subscriptions will be removed (or the loop below will timeout after 5 minutes!)
        try (Publisher<String> publisher = topic.createPublisher();
             @SuppressWarnings("unused") Timeout timeout = Timeout.after(5, TimeUnit.MINUTES))
            {
            String sMsg = Base.getRandomString(100, 100, true);
            while (!caches.Subscriptions.isEmpty())
                {
                publisher.publish(sMsg);
                }
            }

        // now there should be zero subscriptions left
        assertThat(caches.Subscriptions.isEmpty(), is(true));
        }

    // ----- data members ---------------------------------------------------

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(TopicsRestartTests.class);

    private static Coherence s_coherence;

    private static Session s_session;
    }
