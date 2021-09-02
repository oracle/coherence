/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.util.Base;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsArrayContainingInAnyOrder.arrayContainingInAnyOrder;

public class SubscriptionTest
    {
    @Test
    public void shouldNotHaveChannelAllocations()
        {
        Subscription subscription = new Subscription();
        assertThat(subscription.getOwningSubscriber(), is(0L));
        assertThat(subscription.getSubscribers(), is(emptyIterable()));
        }

    @Test
    public void shouldAllocateOneSubscriberToAllChannels()
        {
        int          cChannel     = 17;
        Subscription subscription = new Subscription();
        subscription.addSubscriber(19L, cChannel);
        assertThat(subscription.getSubscribers(), containsInAnyOrder(19L));

        for (int i = 0; i < cChannel; i++)
            {
            assertThat(subscription.getChannelOwner(i), is(19L));
            }
        }

    @Test
    public void shouldAllocateTwoSubscriberToAllChannels()
        {
        int          cChannel     = 17;
        Subscription subscription = new Subscription();
        subscription.addSubscriber(19L, cChannel);
        subscription.addSubscriber(66L, cChannel);
        assertThat(subscription.getSubscribers(), containsInAnyOrder(19L, 66L));

        for (int i = 0; i < cChannel; i++)
            {
            assertThat(subscription.getChannelOwner(i), anyOf(is(19L), is(66L)));
            }
        }

    @Test
    public void shouldAllocateSameNumberOfSubscribersAsChannels()
        {
        int          cChannel      = 17;
        Subscription subscription  = new Subscription();
        Set<Long>    setSubscriber = new TreeSet<>();
        long[]       alExpected    = new long[cChannel];
        int          n             = 0;

        for (long s = 1; s <= cChannel; s++)
            {
            subscription.addSubscriber(s, cChannel);
            setSubscriber.add(s);
            alExpected[n++] = s;
            }

        assertThat(subscription.getSubscribers(), is(setSubscriber));
        assertThat(subscription.getChannelAllocations(), is(alExpected));
        }

    @Test
    public void shouldAllocateMoreSubscriberThanChannels()
        {
        int          cChannel      = 17;
        Subscription subscription  = new Subscription();
        Set<Long>    setSubscriber = new TreeSet<>();
        long[]       alExpected    = new long[cChannel];
        int          n             = 0;

        for (long s = 1; s <= cChannel * 2; s++)
            {
            subscription.addSubscriber(s, cChannel);
            setSubscriber.add(s);
            if (n < cChannel)
                {
                alExpected[n++] = s;
                }
            }

        assertThat(subscription.getSubscribers(), is(setSubscriber));
        assertThat(subscription.getChannelAllocations(), is(alExpected));
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
                subscription.addSubscriber(s, cChannel);
                }
            // assert that all the subscriptions have the same allocation as the first one
            Subscription subscriptionZero = list.get(0);
            for (Subscription subscription : list)
                {
                assertThat(subscription.getChannelAllocations(), is(subscriptionZero.getChannelAllocations()));
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
                subscription.addSubscriber(s, cChannel);
                }
            }

        // assert that all the subscriptions have the same allocation as the first one
        Subscription subscriptionZero = listSubscription.get(0);
        for (Subscription subscription : listSubscription)
            {
            assertThat(subscription.getChannelAllocations(), is(subscriptionZero.getChannelAllocations()));
            }
        }
    }
