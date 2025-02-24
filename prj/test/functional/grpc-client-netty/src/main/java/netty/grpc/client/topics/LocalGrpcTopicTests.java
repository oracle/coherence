/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package netty.grpc.client.topics;

import com.tangosol.net.events.topics.TopicLifecycleEvent;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.NamedTopicEvent;
import com.tangosol.net.topic.Publisher;
import org.junit.Test;
import topics.callables.DestroyTopic;
import topics.remote.EnsureTopic;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

/**
 * Tests for remote gRPC topics.
 * <p>
 * This test runs the cluster member and proxy in the test JVM.
 * The client is then run by Bedrock as a separate process.
 * This makes it easier to debug the proxy side when the test runs.
 */
@SuppressWarnings({"resource", "unchecked"})
public class LocalGrpcTopicTests
        extends BaseLocalGrpcTests
    {
    public LocalGrpcTopicTests()
        {
        super("pof");
        }

    @Test
    public void shouldEnsureTopic() throws Exception
        {
        String      sName    = getTopicName();
        EnsureTopic callable = new EnsureTopic(sName);

        assertThat(getLifecycleEvents(sName), is(empty()));

        boolean fSuccess = s_client.submit(callable).get(1, TimeUnit.MINUTES);
        assertThat(fSuccess, is(true));

        List<TopicLifecycleEvent> events = getLifecycleEvents(sName);
        assertThat(events.size(), is(1));
        TopicLifecycleEvent event = events.get(0);
        assertThat(event.getTopicName(), is(sName));
        assertThat(event.getType(), is(TopicLifecycleEvent.Type.CREATED));
        }

    @Test
    public void shouldDestroyTopic() throws Exception
        {
        String        sName      = getTopicName();
        NamedTopic<?> topic      = s_ccf.ensureTopic(sName);
        AtomicBoolean fDestroyed = new AtomicBoolean(false);

        assertThat(topic.isActive(), is(true));
        assertThat(topic.isDestroyed(), is(false));

        topic.addListener(evt ->
            {
            if (evt.getType() == NamedTopicEvent.Type.Destroyed)
                {
                fDestroyed.set(true);
                }
            });

        DestroyTopic callable = new DestroyTopic(sName);
        boolean      fSuccess = s_client.submit(callable).get(1, TimeUnit.MINUTES);
        assertThat(fSuccess, is(true));
        assertThat(topic.isActive(), is(false));
        assertThat(topic.isDestroyed(), is(true));
        assertThat(fDestroyed.get(), is(true));

        // Get topic again should get a new topic
        NamedTopic<?> topicTwo = s_ccf.ensureTopic(sName);
        assertThat(topicTwo.isActive(), is(true));
        assertThat(topicTwo.isDestroyed(), is(false));
        }

    @Test
    public void shouldReleaseTopic() throws Exception
        {
        String        sName = getTopicName();
        NamedTopic<?> topic = s_ccf.ensureTopic(sName);

        assertThat(topic.isActive(), is(true));
        assertThat(topic.isReleased(), is(false));

        EnsureTopic callable = new EnsureTopic(sName, t ->
            {
            assertThat(t.isActive(), is(true));
            assertThat(t.isReleased(), is(false));
            t.release();
            assertThat(t.isReleased(), is(true));
            return true;
            });

        boolean fSuccess = s_client.submit(callable).get(1, TimeUnit.MINUTES);
        assertThat(fSuccess, is(true));
        assertThat(topic.isActive(), is(true));
        assertThat(topic.isReleased(), is(false));
        assertThat(topic.isDestroyed(), is(false));
        }

    @Test
    public void shouldGetChannelCount() throws Exception
        {
        String        sName    = getTopicName();
        NamedTopic<?> topic    = s_ccf.ensureTopic(sName);
        int           cChannel = topic.getTopicService().ensureChannelCount(sName, 25, 25);
        assertThat(cChannel, is(25));

        EnsureTopic callable = new EnsureTopic(sName, t ->
            {
            assertThat(t.getChannelCount(), is(25));
            return true;
            });

        boolean fSuccess = s_client.submit(callable).get(1, TimeUnit.MINUTES);
        assertThat(fSuccess, is(true));
        }

    @Test
    public void shouldGetRemainingMessages() throws Exception
        {
        String             sGroup = "foo";
        String             sName  = getTopicName();
        NamedTopic<String> topic  = s_ccf.ensureTopic(sName);

        topic.ensureSubscriberGroup(sGroup);
        int cChannel = topic.getChannelCount();
        int cTotal   = cChannel * 2;
        try (Publisher<String> publisher = topic.createPublisher(Publisher.OrderBy.roundRobin()))
            {
            // publish two messages per channel
            for (int i = 0; i < cTotal; i++)
                {
                publisher.publish("Message-" + i).get(1, TimeUnit.MINUTES);
                }
            }

        EnsureTopic callable = new EnsureTopic(sName, t ->
            {
            assertThat(t.getRemainingMessages(sGroup), is(cTotal));
            assertThat(t.getRemainingMessages(sGroup, 1, 2), is(4));
            return true;
            });

        boolean fSuccess = s_client.submit(callable).get(1, TimeUnit.MINUTES);
        assertThat(fSuccess, is(true));
        }

    @Test
    public void shouldGetSubscriberGroups() throws Exception
        {
        String             sName = getTopicName();
        NamedTopic<String> topic = s_ccf.ensureTopic(sName);

        topic.ensureSubscriberGroup("foo");
        topic.ensureSubscriberGroup("bar");

        EnsureTopic callable = new EnsureTopic(sName, t ->
            {
            assertThat(t.getSubscriberGroups(), is(Set.of("foo", "bar")));
            return true;
            });

        boolean fSuccess = s_client.submit(callable).get(1, TimeUnit.MINUTES);
        assertThat(fSuccess, is(true));
        }

    @Test
    public void shouldEnsureSubscriberGroups() throws Exception
        {
        String             sName = getTopicName();
        NamedTopic<String> topic = s_ccf.ensureTopic(sName);

        EnsureTopic callable = new EnsureTopic(sName, t ->
            {
            t.ensureSubscriberGroup("foo");
            t.ensureSubscriberGroup("bar");
            return true;
            });

        boolean fSuccess = s_client.submit(callable).get(1, TimeUnit.MINUTES);
        assertThat(fSuccess, is(true));
        assertThat(topic.getSubscriberGroups(), is(Set.of("foo", "bar")));
        }

    @Test
    public void shouldDestroySubscriberGroups() throws Exception
        {
        String             sName = getTopicName();
        NamedTopic<String> topic = s_ccf.ensureTopic(sName);

        topic.ensureSubscriberGroup("foo");
        topic.ensureSubscriberGroup("bar");

        EnsureTopic callable = new EnsureTopic(sName, t ->
            {
            t.destroySubscriberGroup("foo");
            return true;
            });

        boolean fSuccess = s_client.submit(callable).get(1, TimeUnit.MINUTES);
        assertThat(fSuccess, is(true));
        assertThat(topic.getSubscriberGroups(), is(Set.of("bar")));
        }
    }
