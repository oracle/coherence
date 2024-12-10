/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.internal.net.topic.ChannelAllocationStrategy;
import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import com.tangosol.util.UUID;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import java.util.stream.Collectors;

/**
 * A config map utility class for a {@link com.tangosol.net.PagedTopicService}.
 *
 * @author Jonathan Knight
 * @since 22.06.4
 */
public abstract class PagedTopicConfigMap
    {
    /**
     * Return a {@link Set} of topic names known to this config map.
     *
     * @return a {@link Set} of topic names known to this config map
     */
    public static Set<String> getTopicNames(Map<?, ?> configMap)
        {
        return configMap.keySet().stream()
                .filter(key -> key instanceof String)
                .map(String.class::cast)
                .collect(Collectors.toSet());
        }

    /**
     * Return a {@link Set} of {@link SubscriberGroupId subscriber group identifiers}
     * for a specific topic known to this config map.
     *
     * @param sTopicName  the name of the topic to get the groups for
     *
     * @return a {@link Set} of {@link SubscriberGroupId subscriber group identifiers}
     *         for a specific topic known to this config map
     */
    public static Set<SubscriberGroupId> getSubscriberGroups(Map<?, ?> configMap, String sTopicName)
        {
        return configMap.keySet().stream()
                .filter(v -> v instanceof PagedTopicSubscription.Key)
                .map(PagedTopicSubscription.Key.class::cast)
                .filter(key -> key.getTopicName().equals(sTopicName))
                .map(PagedTopicSubscription.Key::getGroupId)
                .collect(Collectors.toSet());
        }

    /**
     * Return a {@link Set} of {@link SubscriberId subscriber identifiers} for subscribers
     * that sre subscribed to a specific subscriber group for a specific topic known to
     * this config map.
     *
     * @param sTopicName  the name of the topic to get the groups for
     *
     * @return a {@link Set} of {@link SubscriberId subscriber identifiers} for subscribers
     *         that sre subscribed to a specific subscriber group for a specific topic known to
     *         this config map
     */
    public static Set<SubscriberId> getSubscribers(Map<?, ?> configMap, String sTopicName, SubscriberGroupId groupId)
        {
        PagedTopicSubscription subscription = getSubscription(configMap, sTopicName, groupId);
        if (subscription != null)
            {
            return subscription.getSubscriberIds();
            }
        return Collections.emptySet();
        }

    /**
     * Return an {@link Iterable} of {@link PagedTopicSubscription subscriptions}
     * known to this config map.
     *
     * @return an {@link Iterable} of {@link PagedTopicSubscription subscriptions}
     *         known to this config map
     */
    public static Iterable<PagedTopicSubscription> getSubscriptions(Map<?, ?> configMap)
        {
        return configMap.values().stream()
                .filter(v -> v instanceof PagedTopicSubscription)
                .map(PagedTopicSubscription.class::cast)
                .collect(Collectors.toList());
        }

    /**
     * Update this config map with the specified {@link PagedTopicSubscription subscription}.
     *
     * @param subscription  the {@link PagedTopicSubscription subscription} to update
     */
    public static void updateSubscription(Map<Object, Object> configMap, PagedTopicSubscription subscription)
        {
        configMap.put(subscription.getKey(), subscription);
        }

    /**
     * Return a {@link PagedTopicSubscription subscription}.
     *
     * @param lSubscriptionId  the identifier of the subscription
     *
     * @return the {@link PagedTopicSubscription subscription} or {@code null}
     *         if no {@link PagedTopicSubscription subscription} exists
     *         for the specified identifier
     */
    public static PagedTopicSubscription getSubscription(Map<?, ?> configMap, long lSubscriptionId)
        {
        return configMap.values().stream()
                .filter(PagedTopicSubscription.class::isInstance)
                .map(PagedTopicSubscription.class::cast)
                .filter(s -> s.getSubscriptionId() == lSubscriptionId)
                .findFirst()
                .orElse(null);
        }

    /**
     * Return a {@link PagedTopicSubscription subscription}.
     *
     * @param sTopicName  the name of the topic
     * @param groupId     the subscriber group identifier
     *
     * @return the {@link PagedTopicSubscription subscription} or {@code null}
     *         if no {@link PagedTopicSubscription subscription} exists
     *         for the specified topic and subscriber group identifier
     */
    public static PagedTopicSubscription getSubscription(Map<?, ?> configMap, String sTopicName, SubscriberGroupId groupId)
        {
        return (PagedTopicSubscription) configMap.get(new PagedTopicSubscription.Key(sTopicName, groupId));
        }

    /**
     * Return a {@link PagedTopicSubscription subscription} identifier.
     *
     * @param sTopicName  the name of the topic
     * @param groupId     the subscriber group identifier
     *
     * @return the {@link PagedTopicSubscription subscription} identifier or
     *         zero if no {@link PagedTopicSubscription subscription} exists
     *         for the specified topic and subscriber group identifier
     */
    public static long getSubscriptionId(Map<?, ?> configMap, String sTopicName, SubscriberGroupId groupId)
        {
        PagedTopicSubscription subscription = getSubscription(configMap, sTopicName, groupId);
        return subscription == null ? 0 : subscription.getSubscriptionId();
        }

    /**
     * Remove a subscription from this config map.
     *
     * @param key  the {@link PagedTopicSubscription.Key key} of the subscription to remove
     */
    public static void removeSubscription(Map<Object, Object> configMap, PagedTopicSubscription.Key key)
        {
        configMap.computeIfPresent(key, (k, v) -> null);
        }

    /**
     * Returns {@code true} if the specified subscription exists with the
     * specified subscriber subscribed to it.
     * 
     * @param lSubscriptionId  the subscription identifier
     * @param subscriberId     the subscriber identifier
     *
     * @return  {@code true} if the specified subscription exists with the
     *          specified subscriber subscribed to it
     */
    public static boolean hasSubscription(Map<?, ?> configMap, long lSubscriptionId, SubscriberId subscriberId)
        {
        PagedTopicSubscription subscription = getSubscription(configMap, lSubscriptionId);
        if (subscription != null)
            {
            return subscriberId == null
                    || subscription.hasSubscriber(subscriberId);
            }
        return false;
        }

    /**
     * Returns {@code true} if the specified topic has any registered subscriptions.
     *
     * @param configMap  the config map for the service
     * @param sTopic     the name of the topic
     *
     * @return {@code true} if the specified topic has any registered subscriptions
     */
    public static boolean hasSubscriptions(Map<?, ?> configMap, String sTopic)
        {
        return configMap.keySet().stream()
                .filter(key -> key instanceof PagedTopicSubscription.Key)
                .map(PagedTopicSubscription.Key.class::cast)
                .anyMatch(key -> key.getTopicName().equals(sTopic));
        }

    /**
     * Returns the count of subscriptions for the specified topic.
     *
     * @param configMap  the config map for the service
     * @param sTopic     the name of the topic
     *
     * @return the count of subscriptions for the specified topic
     */
    public static long getSubscriptionCount(Map<?, ?> configMap, String sTopic)
        {
        return configMap.keySet().stream()
                .filter(key -> key instanceof PagedTopicSubscription.Key)
                .map(PagedTopicSubscription.Key.class::cast)
                .filter(key -> key.getTopicName().equals(sTopic))
                .count();
        }

    /**
     * Update the channel count and allocations for all subscriptions for a topic.
     *
     * @param configMap  the config map to update
     * @param sTopic     the name of the topic
     * @param cChannel   the new channel count
     * @param strategy   the channel allocation strategy
     */
    @SuppressWarnings("unchecked")
    public static void setChannelCount(Map<?, ?> configMap, String sTopic, int cChannel, ChannelAllocationStrategy strategy)
        {
        Set<PagedTopicSubscription.Key> setKey = configMap.keySet().stream()
                .filter(key -> key instanceof PagedTopicSubscription.Key)
                .map(PagedTopicSubscription.Key.class::cast)
                .filter(key -> key.getTopicName().equals(sTopic))
                .collect(Collectors.toSet());

        Map<PagedTopicSubscription.Key, PagedTopicSubscription> mapSub = (Map<PagedTopicSubscription.Key, PagedTopicSubscription>) configMap;
        for (PagedTopicSubscription.Key key : setKey)
            {
            PagedTopicSubscription subscription = (PagedTopicSubscription) configMap.get(key);
            if (subscription.getChannelCount() < cChannel)
                {
                Logger.config("Updating channel count for subscription " + key.getGroupName() + " in topic " + sTopic + " to " + cChannel);
                subscription.updateChannelAllocations(strategy, cChannel);
                mapSub.put(key, subscription);
                }
            }
        }

    /**
     * Remove configurations for the specified topic name.
     *
     * @param sTopicName  the name of the topic to remove
     */
    public static void removeTopic(Map<?, ?> configMap, String sTopicName)
        {
        // remove the topic entry
        configMap.remove(sTopicName);
        // remove all the subscriptions for the topic
        Set<PagedTopicSubscription.Key> setKeys   = configMap.keySet().stream()
                .filter(v -> v instanceof PagedTopicSubscription.Key)
                .map(PagedTopicSubscription.Key.class::cast)
                .filter(key -> key.getTopicName().equals(sTopicName))
                .collect(Collectors.toSet());
        setKeys.forEach(configMap::remove);
        }

    public static Map<String, Map<SubscriptionAndGroup, Set<SubscriberId>>> getDepartedSubscriptions(Map<?, ?> configMap, Set<UUID> setMember)
        {
        Map<String, Map<SubscriptionAndGroup, Set<SubscriberId>>> mapDeparted = new HashMap<>();
        for (Map.Entry<?, ?> entry : configMap.entrySet())
            {
            Object oKey = entry.getKey();
            if (oKey instanceof PagedTopicSubscription.Key)
                {
                PagedTopicSubscription subscription = (PagedTopicSubscription) entry.getValue();
                Set<SubscriberId>      setDeparted  = subscription.getDepartedSubscribers(setMember);
                if (!setDeparted.isEmpty())
                    {
                    String                                       sTopicName = subscription.getKey().getTopicName();
                    Map<SubscriptionAndGroup, Set<SubscriberId>> map        = mapDeparted.get(sTopicName);
                    if (map == null)
                        {
                        map = new HashMap<>();
                        }
                    SubscriptionAndGroup sg = new SubscriptionAndGroup(subscription.getSubscriptionId(), subscription.getSubscriberGroupId());
                    map.put(sg, setDeparted);
                    mapDeparted.put(sTopicName, map);
                    }
                }
            }
        return mapDeparted;
        }

    // ----- inner class SubscriptionInfo -----------------------------------

    public static class SubscriptionAndGroup
        {
        public SubscriptionAndGroup(long lSubscription, SubscriberGroupId groupId)
            {
            f_lSubscription = lSubscription;
            f_groupId       = groupId;
            }

        public long getSubscriptionId()
            {
            return f_lSubscription;
            }

        public SubscriberGroupId getSubscriberGroupId()
            {
            return f_groupId;
            }

        @Override
        public boolean equals(Object o)
            {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SubscriptionAndGroup that = (SubscriptionAndGroup) o;
            return f_lSubscription == that.f_lSubscription && Objects.equals(f_groupId, that.f_groupId);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(f_lSubscription, f_groupId);
            }

        // ----- data members -----------------------------------------------

        private final long f_lSubscription;

        private final SubscriberGroupId f_groupId;
        }

    }
