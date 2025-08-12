/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import com.tangosol.internal.net.topic.impl.paged.statistics.PagedTopicStatistics;

import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

import java.util.Set;

/**
 * A {@link TopicService} which provides globally ordered topics.
 */
public interface PagedTopicService
        extends TopicService, DistributedCacheService
    {
    /**
     * Ensure the specified subscriber is created in a subscription.
     *
     * @param sTopicName    the name of the topic
     * @param groupId       the {@link SubscriberGroupId id} of the subscriber group
     * @param subscriberId  the {@link Subscriber.Id}
     * @param filter        the {@link Filter} to use to filter messages sent to subscribers
     * @param extractor     the {@link ValueExtractor} to use to convert messages sent to subscribers
     *
     * @return  the unique identifier of the subscriber group the {@link Subscriber}
     *          is subscribed to, of {@code -1} if the cluster is not version
     *          compatible and cannot create subscriptions
     */
    long ensureSubscription(String sTopicName, SubscriberGroupId groupId, Subscriber.Id subscriberId,
                            Filter<?> filter, ValueExtractor<?, ?> extractor);

    /**
     * Ensure the specified subscriber is subscribed to a subscription.
     *
     * @param sTopicName     the name of the topic
     * @param lSubscription  the unique id of the subscriber group
     * @param subscriberId   the {@link Subscriber.Id}
     */
    void ensureSubscription(String sTopicName, long lSubscription, Subscriber.Id subscriberId);

    /**
     * Ensure the specified subscriber is subscribed to a subscription.
     *
     * @param sTopicName       the name of the topic
     * @param lSubscription    the unique id of the subscriber group
     * @param subscriberId     the {@link Subscriber.Id}
     * @param fForceReconnect  force a reconnection even if the subscriber is known to the service
     */
    void ensureSubscription(String sTopicName, long lSubscription, Subscriber.Id subscriberId, boolean fForceReconnect);

    /**
     * Remove an existing subscriber from a subscriber group.
     *
     * @param lSubscriptionId  the subscription identifier
     * @param subscriberId     the {@link Subscriber.Id}
     */
    void destroySubscription(long lSubscriptionId, Subscriber.Id subscriberId);

    /**
     * Remove an existing subscriber group.
     *
     * @param lSubscriptionId  the subscription identifier
     */
    void destroySubscription(long lSubscriptionId);

    /**
     * Determine whether a subscriber group exists.
     *
     * @param lSubscriptionId  the id of the subscription to return
     *
     * @return  {@code true} if the group exists
     */
    boolean hasSubscription(long lSubscriptionId);

    /**
     * Determine whether a subscriber group has been destroyed.
     *
     * @param lSubscriptionId  the unique identifier of the subscriber group
     *
     * @return  {@code true} if the group has been destroyed
     */
    boolean isSubscriptionDestroyed(long lSubscriptionId);

    /**
     * Return a {@link PagedTopicSubscription}.
     *
     * @param lSubscriptionId  the id of the subscription to return
     *
     * @return the specified {@link PagedTopicSubscription} or {@code null}
     *         if the subscription does not exist.
     */
    PagedTopicSubscription getSubscription(long lSubscriptionId);

    /**
     * Return the subscription id for a subscriber group.
     *
     * @param sTopicName  the name of the topic
     * @param groupId     the {@link SubscriberGroupId identifier} for the subscriber group
     *
     * @return the subscription id for a subscriber group or zero
     *         if the subscription does not exist.
     */
    long getSubscriptionId(String sTopicName, SubscriberGroupId groupId);

    /**
     * Returns the {@link PagedTopicStatistics} for a topic.
     *
     * @param sTopicName  the name of the topic
     *
     * @return the {@link PagedTopicStatistics} for the topic or {@code null}
     *         if no statistics exist for the topic
     */
    PagedTopicStatistics getTopicStatistics(String sTopicName);

    /**
     * Returns the {@link SubscriberId subscriber ids} known to this service and
     * subscribed to a subscriber group for a topic
     * .
     *
     * @return the {@link SubscriberId subscriber ids} known to this service and
     *         subscribed to a subscriber group for a topic
     */
    Set<SubscriberId> getSubscribers(String sTopicName, SubscriberGroupId groupId);

    /**
     * Returns {@code true} if the specified topic has subscribers or subscriber groups.
     *
     * @param sTopicName the name of the topic
     *
     * @return {@code true} if the specified topic has subscribers or subscriber groups
     */
    boolean hasSubscribers(String sTopicName);

    /**
     * Returns the count of subscriptions for the specified topic.
     *
     * @param sTopicName the name of the topic
     *
     * @return the count of subscriptions for the specified topic
     */
    long getSubscriptionCount(String sTopicName);

    /**
     * Add a listener that will be notified when changes are made to topic subscriptions.
     *
     * @param listener  the listener to add
     */
    void addSubscriptionListener(PagedTopicSubscription.Listener listener);

    /**
     * Remove a listener that was being notified when changes are made to topic subscriptions.
     *
     * @param listener  the listener to remove
     */
    void removeSubscriptionListener(PagedTopicSubscription.Listener listener);

    /**
     * Return the number messages remaining in the topic after the last committed message.
     * <p>
     * This value is a count of messages remaining to be polled after the last committed message.
     * If a subscriber has received multiple messages but not committed any of them then the
     * count of remaining messages will remain unchanged.
     * <p>
     * This value is transitive and could already have changed by another in-flight commit request
     * immediately after this value is returned.
     * <p>
     * Note, getting the total remaining messages count is a cluster wide operation as the value is stored
     * by each member.
     *
     * @param sTopic             the name of the topic
     * @param subscriberGroupId  the subscriber group
     * @param anChannel          the channels to get the remaining message count from,
     *                           or specify no channels to return the count for all channels
     *
     * @return the number of unread messages
     */
    int getRemainingMessages(String sTopic, SubscriberGroupId subscriberGroupId, int... anChannel);

    /**
     * Return the minimum topics API version supported by the cluster members.
     *
     * @return  the minimum topics API version supported by cluster members
     */
    int getCurrentClusterTopicsApiVersion();

    /**
     * Topic API version zero
     * 14.1.1.2206.2 and below
     * 22.09.*
     */
    int TOPIC_API_v0 = 0;

    /**
     * Topic API version one
     * 14.1.1.2206.3 - 14.1.1.2206.5
     * 23.03.0 - 22.03.1
     */
    int TOPIC_API_v1 = 1;

    /**
     * Topic API version two
     * 14.1.1.2206.6 -> and above
     * 23.03.2 and above
     */
    int TOPIC_API_v2 = 2;

    /**
     * Topic API version three
     * 15.1.1.0.0 -> and above
     * patch compatible 14.1.2.0.4
     * patch compatible 14.1.1.2206.14
     */
    int TOPIC_API_v3 = 3;
    }
