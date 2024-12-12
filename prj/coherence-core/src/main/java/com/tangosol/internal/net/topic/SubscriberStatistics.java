/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

import java.util.Map;

/**
 * Statistics for a subscriber.
 *
 * @author Jonathan Knight  2024.11.26
 */
public interface SubscriberStatistics
    {
    /**
     * Returns the subscriber's identifier.
     *
     * @return the subscriber's identifier
     */
    long getId();

    /**
     * Obtain the name of the subscriber type.
     *
     * @return the name of the subscriber type
     */
    String getTypeName();

    /**
     * Returns the subscriber's notification identifier.
     *
     * @return the subscriber's notification identifier
     */
    int getNotificationId();

    /**
     * Returns the subscriber's optional identifying name.
     *
     * @return the subscriber's unique identifying name
     */
    String getIdentifyingName();

    /**
     * Returns the underlying {@link NamedTopic} that this {@link Subscriber}
     * is subscribed to, which could be of a different generic type to this
     * {@link Subscriber} if the subscriber is using a transformer.
     *
     * @param <T>  the type of the underlying topic
     *
     * @return the underlying {@link NamedTopic} that this {@link Subscriber}
     *         is subscribed to
     */
    <T> NamedTopic<T> getNamedTopic();

    /**
     * Returns this subscriber's group identifier.
     *
     * @return this subscriber's group identifier
     */
    SubscriberGroupId getSubscriberGroupId();

    /**
     * Returns the number of channels in the underlying {@link NamedTopic}.
     * <p>
     * This could be different to the number of channels {@link #getChannels() owned} by this {@link Subscriber}.
     *
     * @return the number of channels in the underlying {@link NamedTopic}
     */
    int getChannelCount();

    /**
     * Determine whether this {@link Subscriber} is active.
     *
     * @return {@code true} if this {@link Subscriber} is active
     */
    boolean isActive();

    /**
     * Returns {@code true} if this subscriber is connected to the topic.
     *
     * @return {@code true} if this subscriber is connected to the topic
     */
    boolean isConnected();

    /**
     * Returns the current set of channels that this {@link Subscriber} owns.
     * <p>
     * Subscribers that are part of a subscriber group own a sub-set of the available channels.
     * A subscriber in a group should normally be assigned ownership of at least one channel. In the case where there
     * are more subscribers in a group that the number of channels configured for a topic, then some
     * subscribers will obviously own zero channels.
     * Anonymous subscribers that are not part of a group are always owners all the available channels.
     *
     * @return the current set of channels that this {@link Subscriber} is the owner of, or an
     *         empty array if this subscriber has not been assigned ownership any channels
     */
    int[] getChannels();

    /**
     * Returns the number of polls of the topic for messages.
     * <p>
     * This is typically larger than the number of messages received due to polling empty pages,
     * empty topics, etc.
     *
     * @return the number of polls of the topic for messages
     */
    long getPolls();

    /**
     * Returns {@code true} if this is an anonymous subscriber,
     * or {@code false} if this subscriber is in a group.
     *
     * @return {@code true} if this is an anonymous subscriber,
     * or {@code false} if this subscriber is in a group
     */
    boolean isAnonymous();

    /**
     * Returns the number of message elements received.
     *
     * @return the number of message elements received
     */
    long getElementsPolled();

    /**
     * Returns the number of times the subscriber has waited on empty channels.
     *
     * @return the number of times the subscriber has waited on empty channels
     */
    long getWaitCount();

    /**
     * Returns the number of notification received that a channel has been populated.
     *
     * @return the number of notification received that a channel has been populated
     */
    long getNotify();

    /**
     * Returns the state of the subscriber.
     *
     * @return the state of the subscriber
     */
    int getState();

    /**
     * Returns the state of the subscriber.
     *
     * @return the state of the subscriber
     */
    String getStateName();

    long getBacklog();

    long getMaxBacklog();

    boolean isCompleteOnEmpty();

    /**
     * Return the optional subscriber group filter.
     *
     * @return the optional subscriber group filter
     */
    Filter<?> getFilter();

    /**
     * Return the optional subscriber group converter.
     *
     * @return the optional subscriber group converter
     */
    ValueExtractor<?,?> getConverter();

    /**
     * Return the count of calls to one of the receive methods.
     *
     * @return the count of calls to one of the receive methods
     */
    long getReceiveRequests();

    /**
     * Return the size of the prefetch queue.
     *
     * @return the size of the prefetch queue
     */
    int getReceiveQueueSize();

    /**
     * Return the number of cancelled receive requests.
     *
     * @return the number of cancelled receive requests
     */
    long getCancelled();

    /**
     * Return the number of completed receive requests.
     *
     * @return the number of completed receive requests
     */
    long getReceived();

    /**
     * Return the mean rate of completed receive requests.
     *
     * @return the mean rate of completed receive requests
     */
    double getReceivedMeanRate();

    /**
     * Return the one-minute rate of completed receive requests.
     *
     * @return the one-minute rate of completed receive requests
     */
    double getReceivedOneMinuteRate();

    /**
     * Return the five-minute rate of completed receive requests.
     *
     * @return the five-minute rate of completed receive requests
     */
    double getReceivedFiveMinuteRate();

    /**
     * Return the fifteen-minute rate of completed receive requests.
     *
     * @return the fifteen-minute rate of completed receive requests
     */
    double getReceivedFifteenMinuteRate();

    /**
     * Return the number of receive requests completed empty.
     * <p>
     * This wil only apply to subscribers using the {@link com.tangosol.net.topic.Subscriber.CompleteOnEmpty}
     * option.
     *
     * @return the number of receive requests completed empty
     */
    long getReceivedEmpty();

    /**
     * Return the number of exceptionally completed receive requests.
     *
     * @return the number of exceptionally completed receive requests
     */
    long getReceivedError();

    /**
     * Return the number of disconnections.
     *
     * @return the number of disconnections
     */
    long getDisconnectCount();

    /**
     * Disconnect this subscriber.
     * <p>
     * This will cause the subscriber to re-initialize itself on re-connection.
     */
    void disconnect();

    /**
     * Ensure that the subscriber is connected.
     */
    void connect();

    /**
     * Returns a {@link Map} of the {@link Position Positions} that are currently the head
     * for each channel owned by this {@link Subscriber}.
     * <p>
     * This result is somewhat transient in situations where the Subscriber has in-flight
     * receive requests, so the heads returned may change just after the method returns.
     *
     * @return the {@link Position Positions} that are currently the heads for each channel owned
     *         by this {@link Subscriber}
     *
     * @throws IllegalStateException if the {@link Subscriber} is closed
     */
    Map<Integer, Position> getHeads();

    /**
     * Returns the specified channel.
     *
     * @param nChannel  the channel to return
     *
     * @return the specified channel
     */
    Subscriber.Channel getChannel(int nChannel);

    /**
     * Notification that one or more channels that were empty now have content.
     *
     * @param nChannel  the non-empty channels
     */
    void notifyChannel(int nChannel);

    /**
     * Returns the number of remaining messages to be read from the topic for this subscriber.
     *
     * @return  the number of remaining messages
     */
    int getRemainingMessages();

    /**
     * Returns the number of remaining messages to be read from the topic channel for this subscriber.
     *
     * @param nChannel  the channel to count remaining messages in
     *
     * @return  the number of remaining messages, or zero if this subscriber does not own the channel
     */
    int getRemainingMessages(int nChannel);
    }
