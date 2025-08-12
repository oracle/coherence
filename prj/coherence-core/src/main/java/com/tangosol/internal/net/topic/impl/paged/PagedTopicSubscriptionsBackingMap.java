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

import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.util.Converter;
import com.tangosol.util.ObservableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A backing map for a paged topic subscriptions cache.
 *
 * @author Jonathan Knight 2022.08.11
 */
@SuppressWarnings("rawtypes")
public class PagedTopicSubscriptionsBackingMap
        extends PagedTopicBackingMap
    {
    /**
     * Create a {@link PagedTopicSubscriptionsBackingMap}.
     *
     * @param map      the wrapped backing map
     * @param context  the {@link BackingMapManagerContext} for the cache
     */
    public PagedTopicSubscriptionsBackingMap(ObservableMap map, BackingMapManagerContext context)
        {
        super(map);
        f_convKey   = context.getKeyFromInternalConverter();
        f_convValue = context.getValueFromInternalConverter();
        }

    @Override
    public void clear()
        {
        f_mapRollback.clear();
        super.clear();
        }

    @Override
    @SuppressWarnings("unchecked")
    public Object put(Object key, Object value, long cMillis)
        {
        Object oResult = super.put(key, value, cMillis);
        updateSubscription((Subscription.Key) f_convKey.convert(key),
                (Subscription) f_convValue.convert(value));
        return oResult;
        }

    @Override
    @SuppressWarnings("unchecked")
    public void putAll(Map map)
        {
        super.putAll(map);
        Set<Map.Entry> set = map.entrySet();
        for (Map.Entry<?, ?> entry : set)
            {
            updateSubscription((Subscription.Key) f_convKey.convert(entry.getKey()),
                    (Subscription) f_convValue.convert(entry.getValue()));
            }
        }

    @Override
    @SuppressWarnings("unchecked")
    protected boolean removeBlind(Object oKey)
        {
        boolean fRemoved =  super.removeBlind(oKey);
        if (fRemoved)
            {
            removeSubscription((Subscription.Key) f_convKey.convert(oKey));
            }
        return fRemoved;
        }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object key, Object value)
        {
        boolean fRemoved = super.remove(key, value);
        if (fRemoved)
            {
            removeSubscription((Subscription.Key) f_convKey.convert(key));
            }
        return fRemoved;
        }

    @Override
    @SuppressWarnings("unchecked")
    public Object remove(Object oKey)
        {
        Object oResult = super.remove(oKey);
        if (oResult != null)
            {
            removeSubscription((Subscription.Key) f_convKey.convert(oKey));
            }
        return oResult;
        }

    /**
     * Update the committed position index.
     *
     * @param key           the {@link Subscription.Key} to use to update the index
     * @param subscription  the {@link Subscription} containing the committed position to update
     */
    protected void updateSubscription(Subscription.Key key, Subscription subscription)
        {
        SubscriberGroupId groupId  = key.getGroupId();
        Integer           nChannel = key.getChannelId();
        if (subscription != null)
            {
            // A rollback position is the position of the next message after the last commit position.
            // There may not actually be a message at this position.
            PagedPosition rollbackPosition = subscription.getCommittedPosition().next();
            f_mapRollback.compute(groupId, (k, currentMap) ->
                {
                if (currentMap == null)
                    {
                    currentMap = new ConcurrentHashMap<>();
                    }
                currentMap.compute(nChannel, (k1, currentPosition) ->
                    {
                    if (currentPosition == null)
                        {
                        return rollbackPosition;
                        }
                    return currentPosition.compareTo(rollbackPosition) <= 0 ? rollbackPosition : currentPosition;
                    });
                return currentMap;
                });
            }
        }

    /**
     * Remove a committed position from the index.
     *
     * @param key  the {@link Subscription.Key} to use to update the index
     */
    protected void removeSubscription(Subscription.Key key)
        {
        SubscriberGroupId groupId  = key.getGroupId();
        Integer           nChannel = key.getChannelId();
        f_mapRollback.compute(groupId, (k, map) ->
            {
            if (map != null)
                {
                if (map.remove(nChannel) != null)
                    {
                    if (map.isEmpty())
                        {
                        return null;
                        }
                    }
                }
            return map;
            });
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
    public Map<Integer, PagedPosition> getRollbackPosition(SubscriberGroupId groupId, int[] anChannel)
        {
        Map<Integer, PagedPosition> mapResult = new HashMap<>();
        Map<Integer, PagedPosition> mapGroup  = f_mapRollback.get(groupId);
        if (mapGroup != null)
            {
            if (anChannel == null || anChannel.length == 0)
                {
                mapResult.putAll(mapGroup);
                }
            else
                {
                for (int c : anChannel)
                    {
                    mapResult.put(c, mapGroup.getOrDefault(c, PagedPosition.NULL_POSITION));
                    }
                }
            }
        return mapResult;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Converter} to use to convert {@link com.tangosol.util.Binary} keys to their object form
     */
    private final Converter f_convKey;

    /**
     * The {@link Converter} to use to convert {@link com.tangosol.util.Binary} values to their object form
     */
    private final Converter f_convValue;

    /**
     * Am index of {@link SubscriberGroupId} to a map of channel to rollback position.
     * <p>
     * A rollback position is the position of the next message after the last commit position.
     * There may not actually be a message at this position.
     */
    private final ConcurrentHashMap<SubscriberGroupId, Map<Integer, PagedPosition>> f_mapRollback = new ConcurrentHashMap<>();
    }
