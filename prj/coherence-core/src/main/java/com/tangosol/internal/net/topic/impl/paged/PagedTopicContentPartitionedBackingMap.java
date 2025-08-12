/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;

import com.tangosol.net.BackingMapManager;

import com.tangosol.net.partition.PartitionSplittingBackingMap.MapArray;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A partitioned backing map used by a paged topic contents cache.
 *
 * @author Jonathan Knight 2022.08.11
 */
public class PagedTopicContentPartitionedBackingMap
        extends PagedTopicPartitionedBackingMap
    {
    /**
     * Create a {@link PagedTopicContentPartitionedBackingMap}
     *
     * @param bmm    the {@link BackingMapManager} for the cache
     * @param sName  the name of the cache
     */
    public PagedTopicContentPartitionedBackingMap(BackingMapManager bmm, String sName)
        {
        super(bmm, sName);
        }

    /**
     * Return the count of remaining messages.
     * <p>
     * For each channel and position specified this method will return a
     * count of the remaining messages in that channel.
     *
     * @param mapPosition  the map of channels and positions to count messages from
     *
     * @return a map of channel and the count of remaining messages for that channel
     */
    @SuppressWarnings("rawtypes")
    public Map<Integer, Integer> getRemainingMessages(Map<Integer, PagedPosition> mapPosition)
        {
        if (mapPosition == null || mapPosition.isEmpty())
            {
            return Collections.emptyMap();
            }

        MapArray              mapArray    = getMap().getMapArray();
        Map[]                 backingMaps = mapArray.getBackingMaps();
        Map<Integer, Integer> mapRemain   = new HashMap<>();

        for (Map value : backingMaps)
            {
            PagedTopicContentBackingMap backingMap = (PagedTopicContentBackingMap) value;
            Map<Integer, Integer>       map        = backingMap.getRemainingMessages(mapPosition);

            for (Map.Entry<Integer, Integer> entry : map.entrySet())
                {
                mapRemain.compute(entry.getKey(), (k, v) ->
                        v == null ? entry.getValue() : v + entry.getValue());
                }
            }
        return mapRemain;
        }
    }
