/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.topic;

import com.tangosol.internal.util.Primes;

/**
 * The dependencies for a topic.
 *
 * @author Jonathan Knight 2002.09.10
 * @since 23.03
 */
public interface TopicDependencies
    {
    /**
     * Returns the number of channels in the topic, or
     * {@link com.tangosol.internal.net.topic.impl.paged.PagedTopic#DEFAULT_CHANNEL_COUNT}
     * to indicate that the topic uses the default number of channels.
     *
     * @return the number of channels in the topic
     */
    int getChannelCount();

    /**
     * Compute the channel count based on the supplied partition count.
     *
     * @param cPartitions the partition count
     *
     * @return the channel count based on the supplied partition count
     */
    static int computeChannelCount(int cPartitions)
        {
        return Math.min(cPartitions, Primes.next((int) Math.sqrt(cPartitions)));
        }

    /**
     * Obtain the expiry delay to apply to elements in ths topic.
     *
     * @return  the expiry delay to apply to elements in ths topic
     */
    long getElementExpiryMillis();

    /**
     * Return the maximum size of a batch.
     *
     * @return the max batch size
     */
    long getMaxBatchSizeBytes();

    /**
     * Returns {@code true} if this topic retains messages after they have been committed
     * or {@code false} if messages are removed after all known subscribers have committed
     * them.
     *
     * @return {@code true} if this topic retains messages after they have been committed
     *         or {@code false} if messages are removed after all known subscribers have
     *         committed them
     */
    boolean isRetainConsumed();

    /**
     * Returns number of milliseconds within which a subscriber must issue a heartbeat or
     * be forcefully considered closed.
     *
     * @return number of milliseconds within which a subscriber must issue a heartbeat
     */
    long getSubscriberTimeoutMillis();

    /**
     * Returns the timeout that a subscriber will use when waiting for its first allocation of channels.
     *
     * @return the timeout that a subscriber will use when waiting for its first allocation of channels
     */
    long getNotificationTimeout();

    /**
     * Returns {@code true} if the topic allows commits of a position in a channel to be
     * made by subscribers that do not own the channel.
     *
     * @return {@code true} if the topic allows commits of a position in a channel to be
     *         made by subscribers that do not own the channel
     */
    boolean isAllowUnownedCommits();

    /**
     * Returns {@code true} if the topic only allows commits of a position in a channel to be
     * made by subscribers that own the channel.
     *
     * @return {@code true} if the topic only allows commits of a position in a channel to be
     *         made by subscribers that own the channel
     */
    boolean isOnlyOwnedCommits();

    /**
     * Return the calculator used to calculate element sizes.
     *
     * @return the calculator used to calculate element sizes
     */
    NamedTopic.ElementCalculator getElementCalculator();

    /**
     * Returns the maximum amount of time publishers and subscribers will
     * attempt to reconnect after being disconnected.
     *
     * @return the maximum amount of time publishers and subscribers will
     *         attempt to reconnect after being disconnected
     */
    long getReconnectTimeoutMillis();

    /**
     * Return the amount of time publishers and subscribers will wait between
     * attempts to reconnect after being disconnected.
     *
     * @return the maximum amount of time publishers and subscribers will
     *         wait between attempts to reconnect after being disconnected
     */
    long getReconnectRetryMillis();

    /**
     * Return the amount of time publishers and subscribers will wait before attempting
     * to reconnect after being disconnected.
     *
     * @return the maximum amount of time publishers and subscribers will
     *         wait before attempting to reconnect after being disconnected
     */
    long getReconnectWaitMillis();
    }
