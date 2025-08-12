/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;

import com.tangosol.internal.net.topic.impl.paged.model.Subscription;
import com.tangosol.net.BackingMapManager;

import com.tangosol.net.partition.PartitionSplittingBackingMap;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jonathan Knight 2022.08.11
 */
public class PagedTopicSubscriptionsPartitionedBackingMap
        extends PagedTopicPartitionedBackingMap
    {
    /**
     * Create a {@link PagedTopicSubscriptionsPartitionedBackingMap}.
     *
     * @param bmm    the {@link BackingMapManager} for the cache
     * @param sName  the name of the cache
     */
    public PagedTopicSubscriptionsPartitionedBackingMap(BackingMapManager bmm, String sName)
        {
        super(bmm, sName);
        }

    /**
     * Return a map of rollback positions for a subscriber group and set of channels.
     * <p>
     * A rollback position is the position of the next message after the last commit position.
     * There may not actually be a message at this position.
     *
     * @param groupId    the {@link SubscriberGroupId}
     * @param anChannel  the channels to get the committed positions for
     *
     * @return a map of rollback positions for a subscriber group and set of channels
     */
    @SuppressWarnings("rawtypes")
    public Map<Integer, PagedPosition> getRollbackPosition(SubscriberGroupId groupId, int[] anChannel)
        {
        PartitionSplittingBackingMap.MapArray mapArray    = getMap().getMapArray();
        Map[]                                 backingMaps = mapArray.getBackingMaps();
        Map<Integer, PagedPosition>           mapPosition = new HashMap<>();

        for (Map value : backingMaps)
            {
            PagedTopicSubscriptionsBackingMap backingMap = (PagedTopicSubscriptionsBackingMap) value;
            Map<Integer, PagedPosition>       map        = backingMap.getRollbackPosition(groupId, anChannel);

            for (Map.Entry<Integer, PagedPosition> entry : map.entrySet())
                {
                PagedPosition position = entry.getValue();
                mapPosition.compute(entry.getKey(), (k, v) ->
                    {
                    if (v == null || v.compareTo(position) < 0)
                        {
                        return position;
                        }
                    return v;
                    });
                }
            }
        return mapPosition;
        }
    }
