/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.net.Member;

import java.util.Set;
import java.util.SortedMap;

/**
 * A strategy for allocating topic channels to subscribers.
 *
 * @author Jonathan Knight
 * @since 22.06.4
 */
public interface ChannelAllocationStrategy
    {
    /**
     * Remove any subscribers for members that are not in the member set.
     *
     * @param mapSubscriber  the map of subscribers
     * @param setMember      the current service member set
     *
     * @return a map of removed subscribers
     */
    SortedMap<Integer, Set<SubscriberId>> cleanup(SortedMap<Long, SubscriberId> mapSubscriber, Set<Member> setMember);

    /**
     * Allocate subscribers to channels.
     *
     * @param mapSubscriber  the sorted set of subscriber id long value to {@link SubscriberId}
     * @param cChannel       the number of channels
     *
     * @return  the new channel allocations
     */
    long[] allocate(SortedMap<Long, SubscriberId> mapSubscriber, int cChannel);

    /**
     * Remove any dead subscribers from the subscriber map and allocate
     * the remaining subscribers to channels.
     *
     * @param mapSubscriber  the sorted set of subscriber id long value to {@link SubscriberId}
     * @param cChannel       the number of channels
     * @param setMember      the current service member set
     *
     * @return  the new channel allocations
     */
    default long[] allocate(SortedMap<Long, SubscriberId> mapSubscriber, int cChannel, Set<Member> setMember)
        {
        cleanup(mapSubscriber, setMember);
        return allocate(mapSubscriber, cChannel);
        }
    }
