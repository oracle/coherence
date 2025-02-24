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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

            if (nMemberId != 0) // zero member id is an Extend client so we cannot do anything here
                {
                if (uuid == null && !setMemberID.contains(nMemberId))
                    {
                    mapRemoved.compute(nMemberId, (key, set) -> ensureSet(nMemberId, subscriberId, set));
                    }
                else if (!setMemberUID.contains(uuid))
                    {
                    mapRemoved.compute(nMemberId, (key, set) -> ensureSet(nMemberId, subscriberId, set));
                    }
                }
            }

        mapRemoved.values().stream()
                .flatMap(Set::stream)
                .forEach(id -> mapSubscriber.remove(id.getId()));

        return mapRemoved;
        }

    @Override
    public long[] allocate(SortedMap<Long, SubscriberId> mapSubscriber, Map<SubscriberId, int[]> mapChannel, int cChannel)
        {
        long[]             alChannel      = new long[cChannel];
        boolean[]          afAllocated    = new boolean[cChannel];
        int                cAllocated     = 0;
        List<SubscriberId> listSubscriber = new ArrayList<>();

        // initialize all to zero
        Arrays.fill(alChannel, 0L);
        Arrays.fill(afAllocated, false);

        // add subscriber to list in order
        mapSubscriber.forEach((k, v) -> listSubscriber.add(v));

        // do any manual allocations first
        Iterator<SubscriberId> iterator = listSubscriber.iterator();
        while (iterator.hasNext())
            {
            SubscriberId subscriberId = iterator.next();
            int[]        anChannel    = mapChannel.get(subscriberId);
            if (anChannel != null && anChannel.length != 0)
                {
                iterator.remove();
                long id = subscriberId.getId();
                for (int c : anChannel)
                    {
                    if (c >= 0 && c < cChannel)
                        {
                        alChannel[c]   = id;
                        afAllocated[c] = true;
                        cAllocated++;
                        }
                    }
                }
            }

        int cSubscriber  = listSubscriber.size();
        int cUnallocated = cChannel - cAllocated;
        if (cUnallocated <= 0 || cSubscriber == 0)
            {
            // we manually allocated everything, or we have no subscribers left, so we're done
            return alChannel;
            }

        if (cSubscriber == 1)
            {
            // there is one subscriber so it gets everything unallocated
            SubscriberId subscriberId = listSubscriber.get(0);
            long         id           = subscriberId.getId();
            for (int c = 0; c < cChannel; c++)
                {
                if (!afAllocated[c])
                    {
                    alChannel[c] = id;
                    }
                }
            }
        else if (cSubscriber >= cUnallocated)
            {
            // we have more subscribers than unallocated channels (or an equal number)
            // so give one channel to each subscriber starting at the beginning
            // until we run out of channels
            int nChannel = 0;
            for (SubscriberId subscriberId : listSubscriber)
                {
                while (nChannel < cChannel && afAllocated[nChannel])
                    {
                    nChannel++;
                    }
                if (nChannel < cChannel)
                    {
                    alChannel[nChannel++] = subscriberId.getId();
                    if (nChannel >= cChannel)
                        {
                        break;
                        }
                    }
                }
            }
        else
            {
            // we have fewer subscribers than unallocated channels
            int nChannel = 0;
            int cAlloc   = cUnallocated / cSubscriber;  // channels per subscriber, rounded down

            // allocate the required number of channels to the subscriber
            for (SubscriberId subscriberId : listSubscriber)
                {
                long nId = subscriberId.getId();
                for (int i = 0; i < cAlloc; i++)
                    {
                    while (nChannel < cChannel && afAllocated[nChannel])
                        {
                        nChannel++;
                        }
                    if (nChannel < cChannel)
                        {
                        afAllocated[nChannel] = true;
                        alChannel[nChannel++] = nId;
                        cAllocated++;
                        }
                    }
                }

            // assign the remainder round-robin
            if (cAllocated < cChannel)
                {
                for (SubscriberId subscriberId : listSubscriber)
                    {
                    long nId = subscriberId.getId();
                    while (nChannel < cChannel && afAllocated[nChannel])
                        {
                        nChannel++;
                        }
                    if (nChannel < cChannel)
                        {
                        alChannel[nChannel++] = nId;
                        if (nChannel >= cChannel)
                            {
                            break;
                            }
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
