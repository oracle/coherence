/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.net.Member;
import com.tangosol.util.UUID;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SimpleChannelAllocationStrategyTest
    {
    @Test
    public void shouldAllocateOneSubscriberToAllChannels()
        {
        int          cChannel     = 17;
        int          nMember      = 1;
        Member       member       = mockMember(nMember);
        long         nId          = SubscriberId.createId(19L, nMember);
        SubscriberId subscriberId = new SubscriberId(nId, member.getUuid());

        SimpleChannelAllocationStrategy strategy = new SimpleChannelAllocationStrategy();

        long[] anAllocation = strategy.allocate(sortedMap(Map.of(1L, subscriberId)), Map.of(), cChannel, Set.of(member));
        assertThat(anAllocation, is(notNullValue()));
        assertThat(anAllocation.length, is(cChannel));

        for (int i = 0; i < cChannel; i++)
            {
            assertThat(anAllocation[i], is(nId));
            }
        }

    @Test
    public void shouldAllocateOneSubscriberWithEmptyManualAllocation()
        {
        int          cChannel     = 17;
        int          nMember      = 1;
        Member       member       = mockMember(nMember);
        long         nId          = SubscriberId.createId(19L, nMember);
        SubscriberId subscriberId = new SubscriberId(nId, member.getUuid());
        int[]        anManual     = new int[0];

        SimpleChannelAllocationStrategy strategy = new SimpleChannelAllocationStrategy();

        long[] anAllocation = strategy.allocate(sortedMap(Map.of(1L, subscriberId)), Map.of(subscriberId, anManual), cChannel, Set.of(member));
        assertThat(anAllocation, is(notNullValue()));
        assertThat(anAllocation.length, is(cChannel));

        for (int i = 0; i < cChannel; i++)
            {
            assertThat(anAllocation[i], is(nId));
            }
        }

    @Test
    public void shouldAllocateOneSubscriberWithOneManualAllocation()
        {
        int          cChannel     = 17;
        int          nMember      = 1;
        Member       member       = mockMember(nMember);
        long         nId          = SubscriberId.createId(19L, nMember);
        SubscriberId subscriberId = new SubscriberId(nId, member.getUuid());
        int[]        anManual     = new int[]{5};

        SimpleChannelAllocationStrategy strategy = new SimpleChannelAllocationStrategy();

        long[] anAllocation = strategy.allocate(sortedMap(Map.of(1L, subscriberId)), Map.of(subscriberId, anManual), cChannel, Set.of(member));
        assertThat(anAllocation, is(notNullValue()));
        assertThat(anAllocation.length, is(cChannel));
        assertThat(anAllocation, is(new long[]{0L, 0L, 0L, 0L, 0L, nId, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L}));
        }

    @Test
    public void shouldAllocateOneSubscriberWithMultipleManualAllocation()
        {
        int          cChannel     = 17;
        int          nMember      = 1;
        Member       member       = mockMember(nMember);
        long         nId          = SubscriberId.createId(19L, nMember);
        SubscriberId subscriberId = new SubscriberId(nId, member.getUuid());
        int[]        anManual     = new int[]{2, 4, 5, 10};

        SimpleChannelAllocationStrategy strategy = new SimpleChannelAllocationStrategy();

        long[] anAllocation = strategy.allocate(sortedMap(Map.of(1L, subscriberId)), Map.of(subscriberId, anManual), cChannel, Set.of(member));
        assertThat(anAllocation, is(notNullValue()));
        assertThat(anAllocation.length, is(cChannel));
        assertThat(anAllocation, is(new long[]{0L, 0L, nId, 0L, nId, nId, 0L, 0L, 0L, 0L, nId, 0L, 0L, 0L, 0L, 0L, 0L}));
        }

    @Test
    public void shouldAllocateOneSubscriberWithOneInvalidManualAllocation()
        {
        int          cChannel     = 17;
        int          nMember      = 1;
        Member       member       = mockMember(nMember);
        long         nId          = SubscriberId.createId(19L, nMember);
        SubscriberId subscriberId = new SubscriberId(nId, member.getUuid());
        int[]        anManual     = new int[]{25};

        SimpleChannelAllocationStrategy strategy = new SimpleChannelAllocationStrategy();

        long[] anAllocation = strategy.allocate(sortedMap(Map.of(1L, subscriberId)), Map.of(subscriberId, anManual), cChannel, Set.of(member));
        assertThat(anAllocation, is(notNullValue()));
        assertThat(anAllocation.length, is(cChannel));
        assertThat(anAllocation, is(new long[]{0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L}));
        }

    @Test
    public void shouldAllocateOneSubscriberWithMultipleInvalidManualAllocation()
        {
        int          cChannel     = 17;
        int          nMember      = 1;
        Member       member       = mockMember(nMember);
        long         nId          = SubscriberId.createId(19L, nMember);
        SubscriberId subscriberId = new SubscriberId(nId, member.getUuid());
        int[]        anManual     = new int[]{-1, 25, 35};

        SimpleChannelAllocationStrategy strategy = new SimpleChannelAllocationStrategy();

        long[] anAllocation = strategy.allocate(sortedMap(Map.of(1L, subscriberId)), Map.of(subscriberId, anManual), cChannel, Set.of(member));
        assertThat(anAllocation, is(notNullValue()));
        assertThat(anAllocation.length, is(cChannel));
        assertThat(anAllocation, is(new long[]{0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L}));
        }

    @Test
    public void shouldAllocateOneSubscriberWithMultipleValidAndInvalidManualAllocation()
        {
        int          cChannel     = 17;
        int          nMember      = 1;
        Member       member       = mockMember(nMember);
        long         nId          = SubscriberId.createId(19L, nMember);
        SubscriberId subscriberId = new SubscriberId(nId, member.getUuid());
        int[]        anManual     = new int[]{-1, 25, 35, 2, 4, 5, 10};

        SimpleChannelAllocationStrategy strategy = new SimpleChannelAllocationStrategy();

        long[] anAllocation = strategy.allocate(sortedMap(Map.of(1L, subscriberId)), Map.of(subscriberId, anManual), cChannel, Set.of(member));
        assertThat(anAllocation, is(notNullValue()));
        assertThat(anAllocation.length, is(cChannel));
        assertThat(anAllocation, is(new long[]{0L, 0L, nId, 0L, nId, nId, 0L, 0L, 0L, 0L, nId, 0L, 0L, 0L, 0L, 0L, 0L}));
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
        Set<Member>  setMember     = new HashSet<>(List.of(member1));

        SortedMap<Long, SubscriberId>   mapSubscriber = sortedMap(Map.of(nId1, subscriberId1, nId2, subscriberId2, nId3, subscriberId3));
        SimpleChannelAllocationStrategy strategy      = new SimpleChannelAllocationStrategy();

        long[] anAllocation = strategy.allocate(mapSubscriber, Map.of(), cChannel, setMember);
        assertThat(anAllocation, is(notNullValue()));
        assertThat(anAllocation.length, is(cChannel));

        boolean fSeen1 = false;
        boolean fSeen3 = false;
        for (int i = 0; i < cChannel; i++)
            {
            fSeen1 = fSeen1 || anAllocation[i] == nId1;
            fSeen3 = fSeen3 || anAllocation[i] == nId3;
            assertThat(anAllocation[i], is(anyOf(is(nId1), is(nId3))));
            }
        assertThat(fSeen1, is(true));
        assertThat(fSeen3, is(true));
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
        Set<Member>  setMember     = new HashSet<>(List.of(member));

        SortedMap<Long, SubscriberId>   mapSubscriber = sortedMap(Map.of(nId1, subscriberId1, nId2, subscriberId2));
        SimpleChannelAllocationStrategy strategy      = new SimpleChannelAllocationStrategy();

        long[] anAllocation = strategy.allocate(mapSubscriber, Map.of(), cChannel, setMember);
        assertThat(anAllocation, is(notNullValue()));
        assertThat(anAllocation.length, is(cChannel));

        boolean fSeen1 = false;
        boolean fSeen2 = false;
        for (int i = 0; i < cChannel; i++)
            {
            fSeen1 = fSeen1 || anAllocation[i] == nId1;
            fSeen2 = fSeen2 || anAllocation[i] == nId2;
            assertThat(anAllocation[i], is(anyOf(is(nId1), is(nId2))));
            }
        assertThat(fSeen1, is(true));
        assertThat(fSeen2, is(true));
        }

    @Test
    public void shouldAllocateTwoSubscribersOneWithManualAllocations()
        {
        int          cChannel      = 17;
        int          nMember       = 1;
        Member       member        = mockMember(nMember);
        long         nId1          = SubscriberId.createId(19L, nMember);
        SubscriberId subscriberId1 = new SubscriberId(nId1, member.getUuid());
        long         nId2          = SubscriberId.createId(66L, nMember);
        SubscriberId subscriberId2 = new SubscriberId(nId2, member.getUuid());
        Set<Member>  setMember     = new HashSet<>(List.of(member));

        SortedMap<Long, SubscriberId> mapSubscriber = sortedMap(Map.of(nId1, subscriberId1, nId2, subscriberId2));
        Map<SubscriberId, int[]>      mapAllocation = Map.of(subscriberId1, new int[]{2, 9, 15});

        SimpleChannelAllocationStrategy strategy = new SimpleChannelAllocationStrategy();

        long[] anAllocation = strategy.allocate(mapSubscriber, mapAllocation, cChannel, setMember);
        assertThat(anAllocation, is(notNullValue()));
        assertThat(anAllocation.length, is(cChannel));
        assertThat(anAllocation, is(new long[]{nId2, nId2, nId1, nId2, nId2, nId2, nId2, nId2, nId2, nId1, nId2, nId2, nId2, nId2, nId2, nId1, nId2}));
        }

    @Test
    public void shouldAllocateTwoSubscribersBothWithManualAllocations()
        {
        int          cChannel      = 17;
        int          nMember       = 1;
        Member       member        = mockMember(nMember);
        long         nId1          = SubscriberId.createId(19L, nMember);
        SubscriberId subscriberId1 = new SubscriberId(nId1, member.getUuid());
        long         nId2          = SubscriberId.createId(66L, nMember);
        SubscriberId subscriberId2 = new SubscriberId(nId2, member.getUuid());
        Set<Member>  setMember     = new HashSet<>(List.of(member));

        SortedMap<Long, SubscriberId> mapSubscriber = sortedMap(Map.of(nId1, subscriberId1, nId2, subscriberId2));
        Map<SubscriberId, int[]>      mapAllocation = Map.of(subscriberId1, new int[]{2, 9, 15}, subscriberId2, new int[]{4, 7, 12});

        SimpleChannelAllocationStrategy strategy = new SimpleChannelAllocationStrategy();

        long[] anAllocation = strategy.allocate(mapSubscriber, mapAllocation, cChannel, setMember);
        assertThat(anAllocation, is(notNullValue()));
        assertThat(anAllocation.length, is(cChannel));
        assertThat(anAllocation, is(new long[]{0L, 0L, nId1, 0L, nId2, 0L, 0L, nId2, 0L, nId1, 0L, 0L, nId2, 0L, 0L, nId1, 0L}));
        }

    @Test
    public void shouldAllocateTwoSubscribersWithDuplicateManualAllocations()
        {
        int          cChannel      = 17;
        int          nMember       = 1;
        Member       member        = mockMember(nMember);
        long         nId1          = SubscriberId.createId(19L, nMember);
        SubscriberId subscriberId1 = new SubscriberId(nId1, member.getUuid());
        long         nId2          = SubscriberId.createId(66L, nMember);
        SubscriberId subscriberId2 = new SubscriberId(nId2, member.getUuid());
        Set<Member>  setMember     = new HashSet<>(List.of(member));

        SortedMap<Long, SubscriberId> mapSubscriber = sortedMap(Map.of(nId1, subscriberId1, nId2, subscriberId2));
        Map<SubscriberId, int[]>      mapAllocation = Map.of(subscriberId1, new int[]{0, 1, 2, 9, 15}, subscriberId2, new int[]{0, 1, 4, 7, 12});

        SimpleChannelAllocationStrategy strategy = new SimpleChannelAllocationStrategy();

        long[] anAllocation = strategy.allocate(mapSubscriber, mapAllocation, cChannel, setMember);
        assertThat(anAllocation, is(notNullValue()));
        assertThat(anAllocation.length, is(cChannel));
        long nMaxId = Math.max(nId1, nId2);
        // max id wins for duplicates
        assertThat(anAllocation, is(new long[]{nMaxId, nMaxId, nId1, 0L, nId2, 0L, 0L, nId2, 0L, nId1, 0L, 0L, nId2, 0L, 0L, nId1, 0L}));
        }

    @Test
    public void shouldAllocateMultipleSubscribersWithAndWithoutManualAllocations()
        {
        int          cChannel      = 17;
        int          nMember       = 1;
        Member       member        = mockMember(nMember);
        long         nId1          = SubscriberId.createId(19L, nMember);
        SubscriberId subscriberId1 = new SubscriberId(nId1, member.getUuid());
        long         nId2          = SubscriberId.createId(66L, nMember);
        SubscriberId subscriberId2 = new SubscriberId(nId2, member.getUuid());
        long         nId3          = SubscriberId.createId(77L, nMember);
        SubscriberId subscriberId3 = new SubscriberId(nId3, member.getUuid());
        long         nId4          = SubscriberId.createId(88L, nMember);
        SubscriberId subscriberId4 = new SubscriberId(nId4, member.getUuid());
        Set<Member>  setMember     = new HashSet<>(List.of(member));

        SortedMap<Long, SubscriberId> mapSubscriber = sortedMap(Map.of(nId1, subscriberId1, nId2, subscriberId2, nId3, subscriberId3, nId4, subscriberId4));
        Map<SubscriberId, int[]>      mapAllocation = Map.of(subscriberId1, new int[]{2, 9, 15}, subscriberId2, new int[]{4, 7, 12});

        SimpleChannelAllocationStrategy strategy = new SimpleChannelAllocationStrategy();

        long[] anAllocation = strategy.allocate(mapSubscriber, mapAllocation, cChannel, setMember);
        assertThat(anAllocation, is(notNullValue()));
        assertThat(anAllocation.length, is(cChannel));

        assertThat(anAllocation[0], anyOf(is(nId3), is(nId4)));
        assertThat(anAllocation[1], anyOf(is(nId3), is(nId4)));
        assertThat(anAllocation[2], is(nId1));
        assertThat(anAllocation[3], anyOf(is(nId3), is(nId4)));
        assertThat(anAllocation[4], is(nId2));
        assertThat(anAllocation[5], anyOf(is(nId3), is(nId4)));
        assertThat(anAllocation[6], anyOf(is(nId3), is(nId4)));
        assertThat(anAllocation[7], is(nId2));
        assertThat(anAllocation[8], anyOf(is(nId3), is(nId4)));
        assertThat(anAllocation[9], is(nId1));
        assertThat(anAllocation[10], anyOf(is(nId3), is(nId4)));
        assertThat(anAllocation[11], anyOf(is(nId3), is(nId4)));
        assertThat(anAllocation[12], is(nId2));
        assertThat(anAllocation[13], anyOf(is(nId3), is(nId4)));
        assertThat(anAllocation[14], anyOf(is(nId3), is(nId4)));
        assertThat(anAllocation[15], is(nId1));
        assertThat(anAllocation[16], anyOf(is(nId3), is(nId4)));
        }

    @Test
    public void shouldAllocateMultipleSubscribersWhereAllChannelsAreAllocatedBySomeSubscribers()
        {
        int          cChannel      = 17;
        int          nMember       = 1;
        Member       member        = mockMember(nMember);
        long         nId1          = SubscriberId.createId(19L, nMember);
        SubscriberId subscriberId1 = new SubscriberId(nId1, member.getUuid());
        long         nId2          = SubscriberId.createId(66L, nMember);
        SubscriberId subscriberId2 = new SubscriberId(nId2, member.getUuid());
        long         nId3          = SubscriberId.createId(77L, nMember);
        SubscriberId subscriberId3 = new SubscriberId(nId3, member.getUuid());
        long         nId4          = SubscriberId.createId(88L, nMember);
        SubscriberId subscriberId4 = new SubscriberId(nId4, member.getUuid());
        Set<Member>  setMember     = new HashSet<>(List.of(member));

        SortedMap<Long, SubscriberId> mapSubscriber = sortedMap(Map.of(nId1, subscriberId1, nId2, subscriberId2, nId3, subscriberId3, nId4, subscriberId4));
        Map<SubscriberId, int[]>      mapAllocation =
                Map.of(subscriberId1, new int[]{0, 1, 2, 4, 6, 8, 12},
                       subscriberId2, new int[]{3, 5, 7, 9, 10, 11, 13, 14, 15, 16});

        SimpleChannelAllocationStrategy strategy = new SimpleChannelAllocationStrategy();

        long[] anAllocation = strategy.allocate(mapSubscriber, mapAllocation, cChannel, setMember);
        assertThat(anAllocation, is(notNullValue()));
        assertThat(anAllocation.length, is(cChannel));

        assertThat(anAllocation[0], is(nId1));
        assertThat(anAllocation[1], is(nId1));
        assertThat(anAllocation[2], is(nId1));
        assertThat(anAllocation[3], is(nId2));
        assertThat(anAllocation[4], is(nId1));
        assertThat(anAllocation[5], is(nId2));
        assertThat(anAllocation[6], is(nId1));
        assertThat(anAllocation[7], is(nId2));
        assertThat(anAllocation[8], is(nId1));
        assertThat(anAllocation[9], is(nId2));
        assertThat(anAllocation[10], is(nId2));
        assertThat(anAllocation[11], is(nId2));
        assertThat(anAllocation[12], is(nId1));
        assertThat(anAllocation[13], is(nId2));
        assertThat(anAllocation[14], is(nId2));
        assertThat(anAllocation[15], is(nId2));
        assertThat(anAllocation[16], is(nId2));
        }

    @Test
    public void shouldAllocateSameNumberOfSubscribersAsChannels()
        {
        int    cChannel = 17;
        int    nMember  = 1;
        Member member   = mockMember(nMember);

        SortedMap<Long, SubscriberId> mapSubscriber = new TreeMap<>();
        for (long id = 1; id <= cChannel; id++)
            {
            long         nId           = SubscriberId.createId(id, nMember);
            SubscriberId subscriberId  = new SubscriberId(nId, member.getUuid());
            mapSubscriber.put(id, subscriberId);
            }

        SimpleChannelAllocationStrategy strategy = new SimpleChannelAllocationStrategy();

        long[] anAllocation = strategy.allocate(mapSubscriber, Map.of(), cChannel, Set.of(member));
        assertThat(anAllocation, is(notNullValue()));
        assertThat(anAllocation.length, is(cChannel));

        long cAllocation = Arrays.stream(anAllocation).distinct().count();
        assertThat(cAllocation, is((long) mapSubscriber.size()));
        }

    @Test
    public void shouldAllocateMoreSubscriberThanChannels()
        {
        int    cChannel = 17;
        int    nMember  = 1;
        Member member   = mockMember(nMember);

        SortedMap<Long, SubscriberId> mapSubscriber = new TreeMap<>();
        for (long id = 1; id <= cChannel * 2; id++)
            {
            long         nId           = SubscriberId.createId(id, nMember);
            SubscriberId subscriberId  = new SubscriberId(nId, member.getUuid());
            mapSubscriber.put(id, subscriberId);
            }

        SimpleChannelAllocationStrategy strategy = new SimpleChannelAllocationStrategy();

        long[] anAllocation = strategy.allocate(mapSubscriber, Map.of(), cChannel, Set.of(member));
        assertThat(anAllocation, is(notNullValue()));
        assertThat(anAllocation.length, is(cChannel));

        long cAllocation = Arrays.stream(anAllocation).distinct().count();
        assertThat(cAllocation, is((long) cChannel));
        }

    @Test
    public void shouldAllocateMoreSubscriberThanChannelsWhereSomeHaveManualAllocations()
        {
        int          cChannel      = 17;
        int          nMember       = 1;
        Member       member        = mockMember(nMember);
        long         nId1          = SubscriberId.createId(1019L, nMember);
        SubscriberId subscriberId1 = new SubscriberId(nId1, member.getUuid());
        long         nId2          = SubscriberId.createId(1066L, nMember);
        SubscriberId subscriberId2 = new SubscriberId(nId2, member.getUuid());

        SortedMap<Long, SubscriberId> mapSubscriber = new TreeMap<>();
        mapSubscriber.put(nId1, subscriberId1);
        mapSubscriber.put(nId2, subscriberId2);
        for (long id = 1; id <= cChannel * 2; id++)
            {
            long         nId           = SubscriberId.createId(id, nMember);
            SubscriberId subscriberId  = new SubscriberId(nId, member.getUuid());
            mapSubscriber.put(id, subscriberId);
            }

        Map<SubscriberId, int[]> mapAllocation = Map.of(subscriberId1, new int[]{2, 9, 15}, subscriberId2, new int[]{4, 7, 12});

        SimpleChannelAllocationStrategy strategy = new SimpleChannelAllocationStrategy();

        long[] anAllocation = strategy.allocate(mapSubscriber, mapAllocation, cChannel, Set.of(member));
        assertThat(anAllocation, is(notNullValue()));
        assertThat(anAllocation.length, is(cChannel));

        assertThat(anAllocation[2], is(nId1));
        assertThat(anAllocation[9], is(nId1));
        assertThat(anAllocation[15], is(nId1));
        assertThat(anAllocation[4], is(nId2));
        assertThat(anAllocation[7], is(nId2));
        assertThat(anAllocation[12], is(nId2));

        long cAllocation = Arrays.stream(anAllocation).distinct().count();
        assertThat(cAllocation, is((long) (cChannel - 4)));
        }


    /**
     * This is one of the most important tests!!!
     * We MUST have consistent allocations across {@link SimpleChannelAllocationStrategy} instances
     * because allocations are done across the cluster with no shared state.
     */
    @Test
    public void shouldCreateConsistentAllocationForMultipleIterations()
        {
        int    cChannel = 17;
        int    nMember  = 1;
        Member member   = mockMember(nMember);

        // create 2000 subscribers, so we will compare 2000 allocations for matches
        List<SimpleChannelAllocationStrategy> listStrategy = new ArrayList<>();
        for (int i = 0; i < 2000; i++)
            {
            listStrategy.add(new SimpleChannelAllocationStrategy());
            }

        SortedMap<Long, SubscriberId> mapSubscriber = new TreeMap<>();
        for (long id = 1; id <= cChannel * 2; id++)
            {
            long         nId           = SubscriberId.createId(id, nMember);
            SubscriberId subscriberId  = new SubscriberId(nId, member.getUuid());
            mapSubscriber.put(id, subscriberId);
            }

        // assert that all the subscriptions have the same allocation as the first one
        SimpleChannelAllocationStrategy strategyZero = listStrategy.get(0);
        long[] anAllocationZero = strategyZero.allocate(mapSubscriber, Map.of(), cChannel, Set.of(member));
        assertThat(anAllocationZero, is(notNullValue()));
        assertThat(anAllocationZero.length, is(cChannel));

        for (SimpleChannelAllocationStrategy strategy : listStrategy)
            {
            long[] anAllocation = strategy.allocate(mapSubscriber, Map.of(), cChannel, Set.of(member));
            assertThat(anAllocation, is(notNullValue()));
            assertThat(anAllocation.length, is(cChannel));
            assertThat(anAllocation, is(anAllocationZero));
            }
        }

    // ----- helper methods -------------------------------------------------

    static SortedMap<Long, SubscriberId> sortedMap(Map<Long, SubscriberId> map)
        {
        return new TreeMap<>(map);
        }

    private Member mockMember(int nId)
        {
        Member member = mock(Member.class);
        UUID   uuid   = new UUID();
        when(member.getId()).thenReturn(nId);
        when(member.getUuid()).thenReturn(uuid);
        return member;
        }
    }
