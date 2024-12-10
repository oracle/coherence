/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;
import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.Member;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.Convert;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UUID;
import com.tangosol.util.filter.AlwaysFilter;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author jf 2019.11.20
 */
public class SubscriptionTest
    {
    @Test
    public void shouldSerializeUsingPof()
        {
        ConfigurablePofContext   serializer   = new ConfigurablePofContext("coherence-pof-config.xml");
        Subscription             subscription = new Subscription();
        Convert<String, Integer> convert      = Subscriber.Convert.using(Integer::parseInt);

        subscription.setSubscriptionHead(20);
        subscription.setPage(10);
        subscription.setPosition(1010);
        subscription.setFilter(new AlwaysFilter<>());
        subscription.setConverter(convert.getExtractor());

        Binary       binary = ExternalizableHelper.toBinary(subscription, serializer);
        Subscription result = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result.getSubscriptionHead(), is(subscription.getSubscriptionHead()));
        assertThat(result.getPosition(), is(subscription.getPosition()));
        assertThat(result.getFilter().equals(subscription.getFilter()), is(true));
        assertThat(result.getConverter(), is(notNullValue()));
        assertThat(result.getDataVersion(), is(subscription.getImplVersion()));
        assertThat(result.toString(), is(notNullValue()));
        }

    @Test
    public void shouldNotHaveChannelAllocations()
        {
        Subscription subscription = new Subscription();
        assertThat(subscription.getOwningSubscriber(), is(nullValue()));
        assertThat(subscription.getSubscribers(), is(emptyIterable()));
        }

    @Test
    public void shouldAllocateOneSubscriberToAllChannels()
        {
        int          cChannel     = 17;
        int          nMember      = 1;
        Member       member       = mockMember(nMember);
        long         nId          = PagedTopicSubscriber.createId(19L, nMember);
        SubscriberId subscriberId = new SubscriberId(nId, member.getUuid());
        Subscription subscription = new Subscription();
        subscription.addSubscriber(subscriberId, cChannel, Collections.singleton(member));
        assertThat(subscription.getSubscribers(), containsInAnyOrder(subscriberId));

        for (int i = 0; i < cChannel; i++)
            {
            assertThat(subscription.getChannelOwner(i), is(subscriberId));
            }
        }

    @Test
    public void shouldRemoveDeadSubscriberOnAllocate()
        {
        int          cChannel      = 17;
        int          nMember1      = 1;
        Member       member1       = mockMember(nMember1);
        int          nMember2      = 2;
        Member       member2       = mockMember(nMember2);
        long         nId1          = PagedTopicSubscriber.createId(19L, nMember1);
        SubscriberId subscriberId1 = new SubscriberId(nId1, member1.getUuid());
        long         nId2          = PagedTopicSubscriber.createId(76L, nMember2);
        SubscriberId subscriberId2 = new SubscriberId(nId2, member2.getUuid());
        long         nId3          = PagedTopicSubscriber.createId(66L, nMember1);
        SubscriberId subscriberId3 = new SubscriberId(nId3, member1.getUuid());
        Subscription subscription  = new Subscription();
        Set<Member>  setMember     = new HashSet<>(List.of(member1, member2));

        subscription.addSubscriber(subscriberId1, cChannel, setMember);
        subscription.addSubscriber(subscriberId2, cChannel, setMember);
        assertThat(subscription.getSubscribers(), containsInAnyOrder(subscriberId1, subscriberId2));

        setMember.remove(member2);
        subscription.addSubscriber(subscriberId3, cChannel, setMember);
        assertThat(subscription.getSubscribers(), containsInAnyOrder(subscriberId1, subscriberId3));
        }

    @Test
    public void shouldAllocateTwoSubscriberToAllChannels()
        {
        int          cChannel      = 17;
        int          nMember       = 1;
        Member       member        = mockMember(nMember);
        long         nId1          = PagedTopicSubscriber.createId(19L, nMember);
        SubscriberId subscriberId1 = new SubscriberId(nId1, member.getUuid());
        long         nId2          = PagedTopicSubscriber.createId(66L, nMember);
        SubscriberId subscriberId2 = new SubscriberId(nId2, member.getUuid());
        Subscription subscription  = new Subscription();

        subscription.addSubscriber(subscriberId1, cChannel, Collections.singleton(member));
        subscription.addSubscriber(subscriberId2, cChannel, Collections.singleton(member));
        assertThat(subscription.getSubscribers(), containsInAnyOrder(subscriberId1, subscriberId2));

        for (int i = 0; i < cChannel; i++)
            {
            assertThat(subscription.getChannelOwner(i), anyOf(is(subscriberId1), is(subscriberId2)));
            }
        }

    @Test
    public void shouldAllocateSameNumberOfSubscribersAsChannels()
        {
        int               cChannel      = 17;
        int               nMember       = 1;
        Member            member        = mockMember(nMember);
        Subscription      subscription  = new Subscription();
        Set<SubscriberId> setSubscriber = new TreeSet<>();
        long[]            alExpected    = new long[cChannel];
        int               n             = 0;

        for (long id = 1; id <= cChannel; id++)
            {
            long         nId           = PagedTopicSubscriber.createId(id, nMember);
            SubscriberId subscriberId  = new SubscriberId(nId, member.getUuid());
            subscription.addSubscriber(subscriberId, cChannel, Collections.singleton(member));
            setSubscriber.add(subscriberId);
            alExpected[n++] = nId;
            }

        assertThat(subscription.getSubscribers(), is(setSubscriber));
        assertThat(subscription.getChannelAllocations(), is(alExpected));
        }

    @Test
    public void shouldAllocateMoreSubscriberThanChannels()
        {
        int               cChannel      = 17;
        int               nMember       = 1;
        Member            member        = mockMember(nMember);
        Subscription      subscription  = new Subscription();
        Set<SubscriberId> setSubscriber = new TreeSet<>();
        long[]            alExpected    = new long[cChannel];
        int               n             = 0;

        for (long id = 1; id <= cChannel * 2; id++)
            {
            long         nId           = PagedTopicSubscriber.createId(id, nMember);
            SubscriberId subscriberId  = new SubscriberId(nId, member.getUuid());
            subscription.addSubscriber(subscriberId, cChannel, Collections.singleton(member));
            setSubscriber.add(subscriberId);
            if (n < cChannel)
                {
                alExpected[n++] = nId;
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
        int                nMember     = 1;
        Member             member      = mockMember(nMember);
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
                long         nId          = PagedTopicSubscriber.createId(s, nMember);
                SubscriberId subscriberId = new SubscriberId(nId, member.getUuid());
                subscription.addSubscriber(subscriberId, cChannel, Collections.singleton(member));
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
        int                nMember          = 1;
        Member             member           = mockMember(nMember);
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
                long         nId          = PagedTopicSubscriber.createId(s, nMember);
                SubscriberId subscriberId = new SubscriberId(nId, member.getUuid());
                subscription.addSubscriber(subscriberId, cChannel, Collections.singleton(member));
                }
            }

        // assert that all the subscriptions have the same allocation as the first one
        Subscription subscriptionZero = listSubscription.get(0);
        for (Subscription subscription : listSubscription)
            {
            assertThat(subscription.getChannelAllocations(), is(subscriptionZero.getChannelAllocations()));
            }
        }

    private Member mockMember(int nId)
        {
        Member member = mock(Member.class);
        UUID uuid     = new UUID();
        when(member.getId()).thenReturn(nId);
        when(member.getUuid()).thenReturn(uuid);
        return member;
        }
    }
