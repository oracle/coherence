/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;
import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.Convert;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.filter.AlwaysFilter;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author jf 2019.11.20
 */
public class SubscriptionTest
    {
    @Test
    public void shouldSerializeUsingPof()
        throws Exception
        {
        ConfigurablePofContext   serializer   = new ConfigurablePofContext("coherence-pof-config.xml");
        Subscription             subscription = new Subscription();
        Convert<String, Integer> convert      = Subscriber.Convert.using(Integer::parseInt);

        subscription.setSubscriptionHead(20);
        subscription.setPage(10);
        subscription.setPosition(1010);
        subscription.setFilter(new AlwaysFilter());
        subscription.setConverter(convert.getFunction());

        Binary       binary = ExternalizableHelper.toBinary(subscription, serializer);
        Subscription result = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result.getSubscriptionHead(), is(subscription.getSubscriptionHead()));
        assertThat(result.getPosition(), is(subscription.getPosition()));
        assertThat(result.getFilter().equals(subscription.getFilter()), is(true));
        assertNotNull(result.getConverter());
        assertThat(result.getDataVersion(), is(subscription.getImplVersion()));
        assertNotNull(result.toString());
        }

    @Test
    public void shouldNotHaveChannelAllocations()
        {
        Subscription subscription = new Subscription();
        MatcherAssert.assertThat(subscription.getOwningSubscriber(), is(0L));
        MatcherAssert.assertThat(subscription.getSubscribers(), is(emptyIterable()));
        }

    @Test
    public void shouldAllocateOneSubscriberToAllChannels()
        {
        int          cChannel     = 17;
        int          nMember      = 1;
        long         nId          = PagedTopicSubscriber.createId(19L, nMember);
        Subscription subscription = new Subscription();
        subscription.addSubscriber(nId, cChannel, Collections.singleton(nMember));
        MatcherAssert.assertThat(subscription.getSubscribers(), containsInAnyOrder(nId));

        for (int i = 0; i < cChannel; i++)
            {
            MatcherAssert.assertThat(subscription.getChannelOwner(i), is(nId));
            }
        }

    @Test
    public void shouldRemoveDeadSubscriberOnAllocate()
        {
        int          cChannel     = 17;
        int          nMember1     = 1;
        int          nMember2     = 2;
        long         nId1         = PagedTopicSubscriber.createId(19L, nMember1);
        long         nId2         = PagedTopicSubscriber.createId(76L, nMember2);
        long         nId3         = PagedTopicSubscriber.createId(66L, nMember1);
        Subscription subscription = new Subscription();
        Set<Integer> setMember    = new HashSet<>(Arrays.asList(nMember1, nMember2));

        subscription.addSubscriber(nId1, cChannel, setMember);
        subscription.addSubscriber(nId2, cChannel, setMember);
        MatcherAssert.assertThat(subscription.getSubscribers(), containsInAnyOrder(nId1, nId2));

        setMember.remove(nMember2);
        subscription.addSubscriber(nId3, cChannel, setMember);
        MatcherAssert.assertThat(subscription.getSubscribers(), containsInAnyOrder(nId1, nId3));
        }

    @Test
    public void shouldAllocateTwoSubscriberToAllChannels()
        {
        int          cChannel     = 17;
        int          nMember      = 1;
        long         nId1         = PagedTopicSubscriber.createId(19L, nMember);
        long         nId2         = PagedTopicSubscriber.createId(66L, nMember);
        Subscription subscription = new Subscription();
        subscription.addSubscriber(nId1, cChannel, Collections.singleton(nMember));
        subscription.addSubscriber(nId2, cChannel, Collections.singleton(nMember));
        MatcherAssert.assertThat(subscription.getSubscribers(), containsInAnyOrder(nId1, nId2));

        for (int i = 0; i < cChannel; i++)
            {
            MatcherAssert.assertThat(subscription.getChannelOwner(i), anyOf(is(nId1), is(nId2)));
            }
        }

    @Test
    public void shouldAllocateSameNumberOfSubscribersAsChannels()
        {
        int          cChannel      = 17;
        int          nMember       = 1;
        Subscription subscription  = new Subscription();
        Set<Long>    setSubscriber = new TreeSet<>();
        long[]       alExpected    = new long[cChannel];
        int          n             = 0;

        for (long id = 1; id <= cChannel; id++)
            {
            long nId = PagedTopicSubscriber.createId(id, nMember);
            subscription.addSubscriber(nId, cChannel, Collections.singleton(nMember));
            setSubscriber.add(nId);
            alExpected[n++] = nId;
            }

        MatcherAssert.assertThat(subscription.getSubscribers(), is(setSubscriber));
        MatcherAssert.assertThat(subscription.getChannelAllocations(), is(alExpected));
        }

    @Test
    public void shouldAllocateMoreSubscriberThanChannels()
        {
        int          cChannel      = 17;
        int          nMember       = 1;
        Subscription subscription  = new Subscription();
        Set<Long>    setSubscriber = new TreeSet<>();
        long[]       alExpected    = new long[cChannel];
        int          n             = 0;

        for (long s = 1; s <= cChannel * 2; s++)
            {
            long nId = PagedTopicSubscriber.createId(s, nMember);
            subscription.addSubscriber(nId, cChannel, Collections.singleton(nMember));
            setSubscriber.add(nId);
            if (n < cChannel)
                {
                alExpected[n++] = nId;
                }
            }

        MatcherAssert.assertThat(subscription.getSubscribers(), is(setSubscriber));
        MatcherAssert.assertThat(subscription.getChannelAllocations(), is(alExpected));
        }


    /**
     * This is one of the most important tests!!!
     * We MUST have consistent allocations across {@link Subscription} instances
     * because allocations are done across the cluster with no shared state.
     */
    @Test
    public void shouldCreateConsistentAllocationForMultipleIterations()
        {
        int                cChannel    = 17;
        int                nMember     = 1;
        List<Subscription> list        = new ArrayList<>();
        long               nIdStart    = 100;
        long               cSubscriber = nIdStart + (cChannel * 2); // create more subscribers than channels

        // create 2000 subscribers, so we will compare 2000 allocations for matches
        for (int i = 0; i < 2000; i++)
            {
            list.add(new Subscription());
            }

        for (long s = nIdStart; s < cSubscriber; s++)
            {
            // Allocate a new subscriber identifier to each subscription in turn
            for (Subscription subscription : list)
                {
                long nId = PagedTopicSubscriber.createId(s, nMember);
                subscription.addSubscriber(nId, cChannel, Collections.singleton(nMember));
                }
            // assert that all the subscriptions have the same allocation as the first one
            Subscription subscriptionZero = list.get(0);
            for (Subscription subscription : list)
                {
                MatcherAssert.assertThat(subscription.getChannelAllocations(), is(subscriptionZero.getChannelAllocations()));
                }
            }
        }

    /**
     * This is one of the most important tests!!!
     * We MUST have consistent allocations across {@link Subscription} instances
     * because allocations are done across the cluster with no shared state.
     */
    @Test
    public void shouldCreateConsistentAllocationForSameSubscribersInRandomOrders()
        {
        int                cChannel         = 17;
        int                nMember          = 1;
        List<Subscription> listSubscription = new ArrayList<>();
        List<Long>         listSubscriber   = new ArrayList<>();
        long               nIdStart         = 100;
        long               cSubscriber      = nIdStart + (cChannel * 2);

        for (long i = nIdStart; i < cSubscriber; i++)
            {
            listSubscriber.add(i);
            }

        // create 2000 subscribers, so we will compare 2000 allocations for matches
        for (int i = 0; i < 2000; i++)
            {
            listSubscription.add(new Subscription());
            }

        // For each subscription add the same list of subscribers but in a random order
        for (Subscription subscription : listSubscription)
            {
            Base.randomize(listSubscriber);
            for (long s : listSubscriber)
                {
                // Allocate a new subscriber identifier to each subscription in turn
                long nId = PagedTopicSubscriber.createId(s, nMember);
                subscription.addSubscriber(nId, cChannel, Collections.singleton(nMember));
                }
            }

        // assert that all the subscriptions have the same allocation as the first one
        Subscription subscriptionZero = listSubscription.get(0);
        for (Subscription subscription : listSubscription)
            {
            MatcherAssert.assertThat(subscription.getChannelAllocations(), is(subscriptionZero.getChannelAllocations()));
            }
        }
    }
