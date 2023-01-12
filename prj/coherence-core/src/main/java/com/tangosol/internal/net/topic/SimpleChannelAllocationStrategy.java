/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import com.tangosol.net.Member;

import com.tangosol.util.UUID;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.stream.Collectors;

/**
 * A simple strategy for allocating topic channels to subscribers.
 *
 * @author Jonathan Knight
 * @since 22.06.4
 */
public class SimpleChannelAllocationStrategy
        implements ChannelAllocationStrategy
    {
    @Override
    public SortedMap<Integer, Set<SubscriberId>> cleanup(SortedMap<Long, SubscriberId> mapSubscriber, Set<Member> setMember)
        {
        SortedMap<Integer, Set<SubscriberId>> mapRemoved   = new TreeMap<>();
        Set<UUID>                             setMemberUID = setMember.stream().map(Member::getUuid).collect(Collectors.toSet());
        Set<Integer>                          setMemberID  = setMember.stream().map(Member::getId).collect(Collectors.toSet());

        for (Map.Entry<Long, SubscriberId> entry : mapSubscriber.entrySet())
            {
            SubscriberId subscriberId = entry.getValue();
            UUID         uuid         = subscriberId.getUID();
            int          nMemberId    = subscriberId.getMemberId();
            if (uuid == null && !setMemberID.contains(nMemberId))
                {
                mapRemoved.compute(nMemberId, (key, set) -> ensureSet(nMemberId, subscriberId, set));
                }
            else if (!setMemberUID.contains(uuid))
                {
                mapRemoved.compute(nMemberId, (key, set) -> ensureSet(nMemberId, subscriberId, set));
                }
            }

        mapRemoved.values().stream()
                .flatMap(Set::stream)
                .forEach(id -> mapSubscriber.remove(id.getId()));

        return mapRemoved;
        }

    @Override
    public long[] allocate(SortedMap<Long, SubscriberId> mapSubscriber, int cChannel)
        {
        long[] alChannel = new long[cChannel];

        int cSubscriber = mapSubscriber.size();
        if (cSubscriber == 0)
            {
            // we have no subscribers
            Arrays.fill(alChannel, 0L);
            }
        else if (cSubscriber == 1)
            {
            // there is one subscriber so it gets everything
            Arrays.fill(alChannel, mapSubscriber.values().iterator().next().getId());
            }
        else if (cSubscriber >= cChannel)
            {
            // we have more subscribers than channels (or an equal number)
            // so give one channel to each subscriber starting at the beginning
            // until we run out of channels
            int nChannel = 0;
            for (Map.Entry<Long, SubscriberId> entry : mapSubscriber.entrySet())
                {
                alChannel[nChannel++] = entry.getValue().getId();
                if (nChannel >= cChannel)
                    {
                    break;
                    }
                }
            }
        else
            {
            // we have fewer subscribers than channels
            int nChannel = 0;
            int cAlloc   = cChannel / cSubscriber;  // channels per subscriber, rounded down

            // allocate the required number of channels to the subscriber
            for (Map.Entry<Long, SubscriberId> entry : mapSubscriber.entrySet())
                {
                for (int i = 0; i < cAlloc; i++)
                    {
                    alChannel[nChannel++] = entry.getValue().getId();
                    }
                }

            // assign the remainder round-robin
            if (nChannel < cChannel)
                {
                for (Map.Entry<Long, SubscriberId> entry : mapSubscriber.entrySet())
                    {
                    alChannel[nChannel++] = entry.getValue().getId();
                    if (nChannel >= cChannel)
                        {
                        break;
                        }
                    }
                }
            }

        return alChannel;
        }

    // ----- helper methods -------------------------------------------------

    private Set<SubscriberId> ensureSet(Integer ignored, SubscriberId id, Set<SubscriberId> setId)
        {
        if (setId == null)
            {
            setId = new HashSet<>();
            }
        setId.add(id);
        return setId;
        }
    }
