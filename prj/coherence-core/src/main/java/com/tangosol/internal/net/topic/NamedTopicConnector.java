/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.net.TopicService;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Filter;

import com.tangosol.util.ValueExtractor;

/**
 * A connector used by a {@link NamedTopicView} to connect
 * to remote topic resources.
 *
 * @param <V> the type of the topic values
 *
 * @author Jonathan Knight  2024.11.26
 */
public interface NamedTopicConnector<V>
        extends PublisherConnector.Factory<V>, SubscriberConnector.Factory<V>
    {
    /**
     * Determine whether the underlying topic is active.
     *
     * @return {@code true} if the underlying topic is active
     */
    boolean isActive();

    /**
     * Determine whether the underlying topic is destroyed.
     *
     * @return {@code true} if the underlying topic is destroyed
     */
    boolean isDestroyed();

    /**
     * Determine whether the underlying topic is released.
     *
     * @return {@code true} if the underlying topic is released
     */
    boolean isReleased();

    /**
     * Obtain the count of remaining messages for a subscriber group.
     *
     * @param sSubscriberGroup the name of the subscriber group
     * @param anChannel        the array of channels to obtain counts for
     * @return the total remaining unread messages
     */
    int getRemainingMessages(String sSubscriberGroup, int[] anChannel);

    /**
     * Obtain the {@link TopicService} that manages this topic.
     *
     * @return the {@link TopicService} that manages this topic
     */
    TopicService getTopicService();

    /**
     * Close the underlying topic.
     */
    void close();

    /**
     * Obtain the name of the underlying topic.
     *
     * @return the name of the underlying topic
     */
    String getName();

    /**
     * Destroy the underlying topic.
     */
    void destroy();

    /**
     * Release the underlying topic.
     */
    void release();

    /**
     * Ensure that the specified subscriber group exists for this topic.
     *
     * @param sSubscriberGroup the name of the subscriber group
     * @param filter           the {@link Filter} used to filter messages to be received by subscribers in the group
     * @param extractor        the {@link ValueExtractor} used to convert messages to be received by subscribers in the group
     * @throws IllegalStateException if the subscriber group already exists with a different filter
     *                               or converter extractor
     */
    void ensureSubscriberGroup(String sSubscriberGroup, Filter<?> filter, ValueExtractor<?, ?> extractor);

    /**
     * Destroy the {@link Subscriber.Name named} subscriber group for the associated topic.
     * <p>
     * Releases storage and stops accumulating topic values for destroyed subscriber group.
     * This operation will impact all {@link Subscriber members} of the subscriber group.
     *
     * @param sSubscriberGroup the name of the subscriber group
     */
    void destroySubscriberGroup(String sSubscriberGroup);

    /**
     * Create a {@link PublisherConnector} that can publish values into this {@link NamedTopic}.
     *
     * @param options the {@link Publisher.Option}s controlling the {@link PublisherConnector}
     * @return a {@link PublisherConnector} that can publish values into this {@link NamedTopic}
     */
    PublisherConnector<V> createPublisher(Publisher.Option<? super V>[] options);

    /**
     * Create a {@link SubscriberConnector} to subscribe to this {@link NamedTopic}.
     *
     * @param options the {@link Subscriber.Option}s controlling the {@link SubscriberConnector}
     * @return a {@link SubscriberConnector} to subscribe to this {@link NamedTopic}
     */
    <U> NamedTopicSubscriber<U> createSubscriber(Subscriber.Option<? super V, U>[] options);

    /**
     * Set the instance of the {@link NamedTopicView}.
     *
     * @param namedTopicView the {@link NamedTopicView}
     */
    void setConnectedNamedTopic(NamedTopicView<V> namedTopicView);
    }
