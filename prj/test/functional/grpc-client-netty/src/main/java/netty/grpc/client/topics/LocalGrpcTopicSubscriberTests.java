/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package netty.grpc.client.topics;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Subscriber;
import org.junit.Test;
import topics.callables.UsingTopic;
import topics.remote.EnsureTopic;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.tangosol.net.topic.Subscriber.inGroup;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

public class LocalGrpcTopicSubscriberTests
        extends BaseLocalGrpcTests
    {
    public LocalGrpcTopicSubscriberTests()
        {
        super("pof");
        }

    @Test
    public void shouldEnsureSubscribers() throws Exception
        {
        String sName = getTopicName();

        EnsureTopic callable = new EnsureTopic(sName, t ->
            {
            System.err.println("**** Entered shouldEnsureSubscriber test");
            int           cChannel      = t.getChannelCount();
            Subscriber<?> subscriberOne = t.createSubscriber();
            assertThat(subscriberOne, is(notNullValue()));
            assertThat(subscriberOne.getSubscriberId(), is(not(nullValue())));
            Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(cChannel));
            Subscriber<?> subscriberTwo = t.createSubscriber();
            assertThat(subscriberTwo, is(notNullValue()));
            assertThat(subscriberTwo.getSubscriberId(), is(not(nullValue())));
            assertThat(subscriberOne.getSubscriberId(), is(not(subscriberTwo.getSubscriberId())));
            Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(cChannel));
            System.err.println("**** Exiting shouldEnsureSubscriber test");
            return true;
            });

        boolean fSuccess = s_client.submit(callable).get(1, TimeUnit.MINUTES);
        assertThat(fSuccess, is(true));
        }

    @Test
    public void shouldEnsureSubscribersInGroup() throws Exception
        {
        String sName = getTopicName();

        EnsureTopic callable = new EnsureTopic(sName, t ->
            {
            int           cChannel      = t.getChannelCount();
            String        sGroup        = "test-group";
            Subscriber<?> subscriberOne = t.createSubscriber(inGroup(sGroup));
            assertThat(subscriberOne, is(notNullValue()));
            assertThat(subscriberOne.getSubscriberId(), is(not(nullValue())));

            Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(cChannel));

            Subscriber<?> subscriberTwo = t.createSubscriber(inGroup(sGroup));
            assertThat(subscriberTwo, is(notNullValue()));
            assertThat(subscriberTwo.getSubscriberId(), is(not(nullValue())));
            assertThat(subscriberOne.getSubscriberId(), is(not(subscriberTwo.getSubscriberId())));
            Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(not(0)));
            Eventually.assertDeferred(() ->
                {
                int cOne = subscriberOne.getChannels().length;
                int cTwo = subscriberTwo.getChannels().length;
                return cOne + cTwo;
                }, is(cChannel));

            return true;
            });

        boolean fSuccess = s_client.submit(callable).get(1, TimeUnit.MINUTES);
        assertThat(fSuccess, is(true));
        }

    @Test
    public void shouldEnsureAndCloseSubscriber() throws Exception
        {
        String sName = getTopicName();

        EnsureTopic callable = new EnsureTopic(sName, t ->
            {
            int           cChannel      = t.getChannelCount();
            String        sGroup        = "test-group";
            Subscriber<?> subscriberOne = t.createSubscriber(inGroup(sGroup));
            assertThat(subscriberOne, is(notNullValue()));
            assertThat(subscriberOne.getSubscriberId(), is(not(nullValue())));

            Eventually.assertDeferred(() -> subscriberOne.getChannels().length, is(cChannel));

            Subscriber<?> subscriberTwo = t.createSubscriber(inGroup(sGroup));
            assertThat(subscriberTwo, is(notNullValue()));
            assertThat(subscriberTwo.getSubscriberId(), is(not(nullValue())));
            assertThat(subscriberOne.getSubscriberId(), is(not(subscriberTwo.getSubscriberId())));
            Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(not(0)));

            Eventually.assertDeferred(() ->
                {
                int cOne = subscriberOne.getChannels().length;
                int cTwo = subscriberTwo.getChannels().length;
                return cOne + cTwo;
                }, is(cChannel));

            subscriberOne.close();
            Eventually.assertDeferred(() -> subscriberTwo.getChannels().length, is(cChannel));

            return true;
            });

        boolean fSuccess = s_client.submit(callable).get(1, TimeUnit.MINUTES);
        assertThat(fSuccess, is(true));
        }

    @Test
    public void shouldCleanUpWhenSubscriberClosed() throws Exception
        {
        String            sName   = getTopicName();
        String            sGroup  = "test";
        NamedTopic<?>     topic   = s_ccf.ensureTopic(sName);
        PagedTopicService service = (PagedTopicService) topic.getTopicService();
        PagedTopicCaches  caches  = new PagedTopicCaches(sName, service, false);

        assertThat(caches.getSubscribers(sGroup), is(emptyIterable()));

        UsingTopic<SubscriberId> createSubscriber = new UsingTopic<>(sName, t ->
            {
            Subscriber<?> subscriber = t.createSubscriber(inGroup(sGroup));
            SUBSCRIBER_MAP.put(subscriber.getSubscriberId(), subscriber);
            assertThat(subscriber, is(notNullValue()));
            assertThat(subscriber.getSubscriberId(), is(not(nullValue())));
            return subscriber.getSubscriberId();
            });

        SubscriberId idOne = s_client.submit(createSubscriber).get(1, TimeUnit.MINUTES);
        assertThat(idOne, is(notNullValue()));

        Set<SubscriberId> subscribers = caches.getSubscribers(sGroup);
        assertThat(subscribers, containsInAnyOrder(idOne));

        SubscriberId idTwo = s_client.submit(createSubscriber).get(1, TimeUnit.MINUTES);
        assertThat(idTwo, is(notNullValue()));

        subscribers = caches.getSubscribers(sGroup);
        assertThat(subscribers, containsInAnyOrder(idOne, idTwo));

        UsingTopic<Boolean> closeSubscriber = new UsingTopic<>(sName, t ->
            {
            Subscriber<?> subscriber = SUBSCRIBER_MAP.get(idOne);
            assertThat(subscriber, is(notNullValue()));
            subscriber.close();
            return true;
            });

        boolean fSuccess = s_client.submit(closeSubscriber).get(1, TimeUnit.MINUTES);
        assertThat(fSuccess, is(true));

        Eventually.assertDeferred(() -> caches.getChannelAllocations(sGroup).size(), is(1));
        assertThat(caches.getSubscribers(sGroup), containsInAnyOrder(idTwo));
        }

    // ----- data members ---------------------------------------------------

    protected static final Map<SubscriberId, Subscriber<?>> SUBSCRIBER_MAP = new ConcurrentHashMap<>();
    }
