/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.internal.net.topic.impl.paged.model.ContentKey;
import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;

import com.tangosol.util.Binary;
import com.tangosol.util.ObservableMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * A backing map used by a paged topic contents cache.
 *
 * @author Jonathan Knight 2022.08.11
 */
@SuppressWarnings("rawtypes")
public class PagedTopicContentBackingMap
        extends PagedTopicBackingMap
    {
    /**
     * Create a {@link PagedTopicContentBackingMap}.
     *
     * @param map  the wrapped backing map
     */
    public PagedTopicContentBackingMap(ObservableMap map)
        {
        super(map);
        }

    @Override
    public void clear()
        {
        f_mapContent.clear();
        super.clear();
        }

    @Override
    public Object put(Object key, Object value, long cMillis)
        {
        Object oResult = super.put(key, value, cMillis);
        updateContent((Binary) key);
        return oResult;
        }

    @Override
    @SuppressWarnings("unchecked")
    public void putAll(Map map)
        {
        super.putAll(map);
        map.keySet().forEach(k -> updateContent((Binary) k));
        }

    @Override
    public Object remove(Object oKey)
        {
        Object oResult = super.remove(oKey);
        if (oResult != null)
            {
            removeContent((Binary) oKey);
            }
        return oResult;
        }

    @Override
    protected boolean removeBlind(Object oKey)
        {
        boolean fRemoved = super.removeBlind(oKey);
        if (fRemoved)
            {
            removeContent((Binary) oKey);
            }
        return fRemoved;
        }

    @Override
    public boolean remove(Object key, Object value)
        {
        boolean fRemoved = super.remove(key, value);
        if (fRemoved)
            {
            removeContent((Binary) key);
            }
        return fRemoved;
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
    public Map<Integer, Integer> getRemainingMessages(Map<Integer, PagedPosition> mapPosition)
        {
        if (mapPosition == null || mapPosition.isEmpty())
            {
            return Collections.emptyMap();
            }

        Map<Integer, Integer> mapRemain = new HashMap<>();
        for (Map.Entry<Integer, PagedPosition> entry : mapPosition.entrySet())
            {
            SortedSet<PagedPosition> set = f_mapContent.get(entry.getKey());
            if (set != null)
                {
                int cMessage = set.tailSet(entry.getValue()).size();
                mapRemain.put(entry.getKey(), cMessage);
                }
            }
        return mapRemain;
        }

    /**
     * Update the content key index.
     *
     * @param binary  the serialized binary {@link ContentKey}
     */
    public void updateContent(Binary binary)
        {
        ContentKey    key      = ContentKey.fromBinary(binary, true);
        PagedPosition position = new PagedPosition(key.getPage(), key.getElement());
        f_mapContent.compute(key.getChannel(),  (k, set) ->
            {
            if (set == null)
                {
                set = new ConcurrentSkipListSet<>();
                }
            set.add(position);
            return set;
            });
        }

    /**
     * Remove a key from the content key index.
     *
     * @param binary  the serialized binary {@link ContentKey}
     */
    public void removeContent(Binary binary)
        {
        ContentKey    key      = ContentKey.fromBinary(binary, true);
        PagedPosition position = new PagedPosition(key.getPage(), key.getElement());
        f_mapContent.compute(key.getChannel(),  (k, set) ->
            {
            if (set != null)
                {
                set.remove(position);
                }
            return set;
            });
        }

    // ----- data members ---------------------------------------------------

    /**
     * An index of {@link  ContentKey} values by channel.
     */
    private final ConcurrentHashMap<Integer, SortedSet<PagedPosition>> f_mapContent = new ConcurrentHashMap<>();
    }
