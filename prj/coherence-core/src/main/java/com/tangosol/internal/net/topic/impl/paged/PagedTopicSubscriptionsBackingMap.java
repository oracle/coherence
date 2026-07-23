/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.internal.net.topic.impl.paged.model.Subscription;
import com.tangosol.internal.util.security.RemoteInstallGate;

import com.tangosol.io.SerializationRole;

import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.util.Converter;
import com.tangosol.util.Filter;
import com.tangosol.util.ObservableMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A backing-map wrapper for paged topic subscriptions.
 *
 * @author Aleks Seovic  2026.05.15
 * @since 26.04
 */
@SuppressWarnings({"rawtypes", "unchecked"})
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
        f_convValue = context.getValueFromInternalConverter();
        }

    @Override
    public synchronized void clear()
        {
        f_setReplayDedup.clear();
        super.clear();
        }

    @Override
    public Object put(Object key, Object value, long cMillis)
        {
        enforceReplay(resolveSubscription(value), f_setReplayDedup);
        return super.put(key, value, cMillis);
        }

    @Override
    public void putAll(Map map)
        {
        Set<Map.Entry>             set         = map.entrySet();
        Map<Object, Subscription>  mapResolved = new LinkedHashMap<>();

        for (Map.Entry entry : set)
            {
            mapResolved.put(entry.getKey(), resolveSubscription(entry.getValue()));
            }

        for (Subscription subscription : mapResolved.values())
            {
            enforceReplay(subscription, f_setReplayDedup);
            }

        super.putAll(map);
        }

    private Subscription resolveSubscription(Object value)
        {
        if (value == null || value instanceof Subscription)
            {
            return (Subscription) value;
            }
        return (Subscription) f_convValue.convert(value);
        }

    private void enforceReplay(Subscription subscription, Set<String> setDedup)
        {
        if (subscription == null || SerializationRole.current() != SerializationRole.PERSISTENCE)
            {
            return;
            }

        Filter       filter    = subscription.getFilter();
        Function     converter = subscription.getConverter();
        RemoteInstallGate.enforceTopicSubscriberReplay(filter, converter, SerializationRole.PERSISTENCE, null,
                setDedup);
        }

    private final Converter f_convValue;

    private final Set<String> f_setReplayDedup = ConcurrentHashMap.newKeySet();
    }
