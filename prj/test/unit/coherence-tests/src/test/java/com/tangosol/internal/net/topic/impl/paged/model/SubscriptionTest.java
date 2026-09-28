/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObjectSerializer;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.net.Member;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.Convert;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.UUID;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.filter.AlwaysFilter;

import org.junit.Test;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
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
        Convert<String, Integer> convert      = Subscriber.Convert.using(new ReflectionExtractor<>("length"));
        SubscriberId             subscriberId = new SubscriberId(1234L, null);

        subscription.setSubscriptionHead(20);
        subscription.setPage(10);
        subscription.setPosition(1010);
        subscription.setFilter(new AlwaysFilter<>());
        subscription.setConverter(convert.getExtractor());
        subscription.setLastPolledSubscriber(subscriberId);

        Binary       binary = ExternalizableHelper.toBinary(subscription, serializer);
        Subscription result = ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result.getSubscriptionHead(), is(subscription.getSubscriptionHead()));
        assertThat(result.getPosition(), is(subscription.getPosition()));
        assertThat(result.getFilter().equals(subscription.getFilter()), is(true));
        assertThat(result.getConverter(), is(notNullValue()));
        assertThat(result.getLastPolledSubscriber(), is(subscriberId));
        assertThat(result.getDataVersion(), is(subscription.getImplVersion()));
        assertThat(result.getFutureData(), is(nullValue()));
        assertThat(result.toString(), is(notNullValue()));

        Binary       binaryResult = ExternalizableHelper.toBinary(result, serializer);
        Subscription stableResult = ExternalizableHelper.fromBinary(binaryResult, serializer);
        Binary       binaryStable = ExternalizableHelper.toBinary(stableResult, serializer);

        assertThat(binaryStable.length(), is(binaryResult.length()));
        }

    @Test
    public void shouldReadVersionFourWithoutLastPolledSubscriber()
        {
        SimplePofContext contextLegacy = createContext(LegacySubscriptionWithoutLastPolled.class);
        SimplePofContext contextCurrent = createContext(Subscription.class);

        Binary       binary = ExternalizableHelper.toBinary(new LegacySubscriptionWithoutLastPolled(), contextLegacy);
        Subscription result = ExternalizableHelper.fromBinary(binary, contextCurrent);

        assertThat(result.getDataVersion(), is(4));
        assertThat(result.getLastPolledSubscriber(), is(nullValue()));
        assertThat(result.getFutureData(), is(nullValue()));
        }

    @Test
    public void shouldCompactDuplicateLastPolledSubscriberData()
        {
        SimplePofContext  contextLegacy = createContext(LegacySubscription.class);
        SimplePofContext  contextCurrent = createContext(Subscription.class);
        LegacySubscription subscription = new LegacySubscription();
        SubscriberId       subscriberId = new SubscriberId(1L, null);

        subscription.setLastPolledSubscriber(subscriberId);
        Binary binaryInitial = ExternalizableHelper.toBinary(subscription, contextLegacy);
        Binary binaryGrown   = binaryInitial;

        for (int i = 2; i <= 5; i++)
            {
            subscription = ExternalizableHelper.fromBinary(binaryGrown, contextLegacy);
            subscriberId = new SubscriberId(i, null);
            subscription.setLastPolledSubscriber(subscriberId);
            binaryGrown = ExternalizableHelper.toBinary(subscription, contextLegacy);
            }

        assertThat(binaryGrown.length(), is(greaterThan(binaryInitial.length())));

        Subscription result          = ExternalizableHelper.fromBinary(binaryGrown, contextCurrent);
        Binary       binaryCompacted = ExternalizableHelper.toBinary(result, contextCurrent);

        assertThat(result.getLastPolledSubscriber(), is(subscriberId));
        assertThat(result.getFutureData(), is(nullValue()));
        assertThat(binaryCompacted.length(), is(lessThan(binaryGrown.length())));

        Subscription resultCompacted = ExternalizableHelper.fromBinary(binaryCompacted, contextCurrent);
        Binary       binaryStable    = ExternalizableHelper.toBinary(resultCompacted, contextCurrent);

        assertThat(binaryStable.length(), is(binaryCompacted.length()));
        }

    @Test
    public void shouldPreserveFutureData()
        {
        SimplePofContext  contextFuture  = createContext(FutureSubscription.class);
        SimplePofContext  contextCurrent = createContext(Subscription.class);
        FutureSubscription subscription  = new FutureSubscription();

        subscription.setFutureValue("future");

        Binary       binary = ExternalizableHelper.toBinary(subscription, contextFuture);
        Subscription result = ExternalizableHelper.fromBinary(binary, contextCurrent);

        assertThat(result.getDataVersion(), is(6));
        assertThat(result.getFutureData(), is(notNullValue()));

        Binary             binaryResult = ExternalizableHelper.toBinary(result, contextCurrent);
        FutureSubscription restored     = ExternalizableHelper.fromBinary(binaryResult, contextFuture);

        assertThat(restored.getFutureValue(), is("future"));
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
        long         nId          = SubscriberId.createId(19L, nMember);
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
        long         nId1          = SubscriberId.createId(19L, nMember1);
        SubscriberId subscriberId1 = new SubscriberId(nId1, member1.getUuid());
        long         nId2          = SubscriberId.createId(76L, nMember2);
        SubscriberId subscriberId2 = new SubscriberId(nId2, member2.getUuid());
        long         nId3          = SubscriberId.createId(66L, nMember1);
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
        long         nId1          = SubscriberId.createId(19L, nMember);
        SubscriberId subscriberId1 = new SubscriberId(nId1, member.getUuid());
        long         nId2          = SubscriberId.createId(66L, nMember);
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
            long         nId           = SubscriberId.createId(id, nMember);
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
            long         nId           = SubscriberId.createId(id, nMember);
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
                long         nId          = SubscriberId.createId(s, nMember);
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
                long         nId          = SubscriberId.createId(s, nMember);
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T> SimplePofContext createContext(Class<T> clz)
        {
        SimplePofContext context = new SimplePofContext();
        context.registerUserType(0, clz, new PortableObjectSerializer(0));
        context.registerUserType(1, SubscriberId.class, new PortableObjectSerializer(1));
        context.registerUserType(2, PagedPosition.class, new PortableObjectSerializer(2));
        return context;
        }

    public static class LegacySubscription
            extends Subscription
        {
        @Override
        public int getImplVersion()
            {
            return 4;
            }

        @Override
        public Binary getFutureData()
            {
            return m_binFuture;
            }

        @Override
        public void setFutureData(Binary binFuture)
            {
            m_binFuture = binFuture;
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            in.readLong(0);
            in.readLong(1);
            in.readInt(2);
            in.readObject(3);
            in.readObject(4);

            if (getDataVersion() >= 2)
                {
                in.readLong(5);
                in.readCollection(6, new ArrayList<>());
                in.readLongArray(7);
                in.readObject(8);
                in.readObject(9);
                }

            if (getDataVersion() >= 3)
                {
                in.readObject(10);
                in.readMap(11, new TreeMap<>());
                }

            if (getDataVersion() >= 4)
                {
                in.readInt(12);
                }
            }

        private Binary m_binFuture;
        }

    public static class LegacySubscriptionWithoutLastPolled
            extends LegacySubscription
        {
        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeLong(0, Page.NULL_PAGE);
            out.writeLong(1, Page.NULL_PAGE);
            out.writeInt(2, 0);
            out.writeObject(3, null);
            out.writeObject(4, null);
            out.writeLong(5, 0L);
            out.writeObject(6, null);
            out.writeLongArray(7, (long[]) null);
            out.writeObject(8, PagedPosition.NULL_POSITION);
            out.writeObject(9, PagedPosition.NULL_POSITION);
            out.writeObject(10, null);
            out.writeMap(11, null);
            out.writeInt(12, 0);
            }
        }

    public static class FutureSubscription
            extends Subscription
        {
        @Override
        public int getImplVersion()
            {
            return 6;
            }

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            super.readExternal(in);
            m_sFuture = in.readString(14);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            super.writeExternal(out);
            out.writeString(14, m_sFuture);
            }

        public String getFutureValue()
            {
            return m_sFuture;
            }

        public void setFutureValue(String sFuture)
            {
            m_sFuture = sFuture;
            }

        private String m_sFuture;
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
