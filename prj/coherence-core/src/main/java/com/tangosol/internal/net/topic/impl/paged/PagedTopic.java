/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.coherence.config.unit.Seconds;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberInfo;
import com.tangosol.internal.util.Primes;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Filter;

import com.tangosol.io.ClassLoaderAware;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * An implementation of a {@link NamedTopic} which provides global ordering.
 * <p>
 * This implementation uses a number of underlying {@link NamedCache} instances
 * to hold the data for this {@link NamedTopic}.
 *
 * @param <V>  the data type of the elements stored in the topic
 *
 * @author jk 2015.06.03
 * @since Coherence 14.1.1
 */
public class PagedTopic<V>
        implements NamedTopic<V>, ClassLoaderAware
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a NamedTopicImpl that will use the specified {@link PagedTopicCaches}
     * to hold the topic data and the specified {@link Publisher} to
     * populate the topic.
     *
     * @param pagedTopicCaches  the {@link PagedTopicCaches} holding the underlying topic data
     */
    public PagedTopic(PagedTopicCaches pagedTopicCaches)
        {
        f_pagedTopicCaches = pagedTopicCaches;
        }

    // ----- PagedTopic methods ---------------------------------------

    public Dependencies getDependencies()
        {
        return getService().getResourceRegistry().getResource(Dependencies.class, getName());
        }

    // ----- NamedTopic methods ---------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public <U> Subscriber<U> createSubscriber(Subscriber.Option<? super V, U>... options)
        {
        ensureActive();
        f_pagedTopicCaches.ensureConnected();
        return new PagedTopicSubscriber<>(this, f_pagedTopicCaches, options);
        }

    @Override
    public void ensureSubscriberGroup(String sName, Filter<?> filter, Function<?, ?> fnConverter)
        {
        f_pagedTopicCaches.ensureSubscriberGroup(sName, filter, fnConverter);
        }

    @Override
    public void destroySubscriberGroup(String sGroupName)
        {
        if (sGroupName == null)
            {
            throw new IllegalArgumentException("invalid group name");
            }

        PagedTopicSubscriber.destroy(f_pagedTopicCaches, SubscriberGroupId.withName(sGroupName));
        }

    @Override
    public Set<String> getSubscriberGroups()
        {
        return f_pagedTopicCaches.getSubscriberGroups();
        }

    @Override
    @SuppressWarnings("unchecked")
    public Publisher<V> createPublisher(Publisher.Option<? super V>... options)
        {
        ensureActive();

        return new PagedTopicPublisher<>(this, f_pagedTopicCaches, options);
        }

    @Override
    public void destroy()
        {
        f_pagedTopicCaches.destroy();
        }

    @Override
    public Service getService()
        {
        return getCacheService();
        }

    public CacheService getCacheService()
        {
        return f_pagedTopicCaches.getCacheService();
        }

    @Override
    public String getName()
        {
        return f_pagedTopicCaches.getTopicName();
        }

    @Override
    public void release()
        {
        if (f_pagedTopicCaches.isActive())
            {
            f_pagedTopicCaches.release();
            }
        }

    @Override
    public boolean isActive()
        {
        return f_pagedTopicCaches.isActive();
        }

    @Override
    public boolean isDestroyed()
        {
        return f_pagedTopicCaches.isDestroyed();
        }

    @Override
    public boolean isReleased()
        {
        return f_pagedTopicCaches.isReleased();
        }

    @Override
    public int getChannelCount()
        {
        return f_pagedTopicCaches.getChannelCount();
        }

    @Override
    public int getRemainingMessages(String sSubscriberGroup, int... anChannel)
        {
        return f_pagedTopicCaches.getRemainingMessages(SubscriberGroupId.withName(sSubscriberGroup), anChannel);
        }

    // ----- ClassLoaderAware methods ---------------------------------------

    @Override
    public ClassLoader getContextClassLoader()
        {
        return f_pagedTopicCaches.getContextClassLoader();
        }

    @Override
    public void setContextClassLoader(ClassLoader classLoader)
        {
        throw new UnsupportedOperationException();
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        PagedTopic<?> that = (PagedTopic<?>) o;
        return Objects.equals(f_pagedTopicCaches, that.f_pagedTopicCaches);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_pagedTopicCaches);
        }

    @Override
    public String toString()
        {
        return getClass().getSimpleName() + "(name=" + f_pagedTopicCaches.getTopicName() + ")";
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Returns the identifiers for all the subscribers belonging to a subscriber group.
     * <p>
     * There is no guarantee that all of the subscribers are actually still active. If a subscriber
     * process exits without closing the subscriber, the identifier remains in the cache until it
     * is timed-out.
     *
     * @param sSubscriberGroup  the subscriber group name to get subscribers for
     *
     * @return the identifiers for all the subscribers belonging to a subscriber group
     */
    public Set<SubscriberInfo.Key> getSubscribers(String sSubscriberGroup)
        {
        return f_pagedTopicCaches.getSubscribers(sSubscriberGroup);
        }

    /**
     * Ensure that this {@link PagedTopic} is active.
     *
     * @throws IllegalStateException if not active
     */
    protected void ensureActive()
        {
        if (!f_pagedTopicCaches.isActive())
            {
            throw new IllegalStateException("This topic is no longer active");
            }
        }

    // ----- inner interface: Dependencies ----------------------------------

    public interface Dependencies
        {
        /**
         * Returns the number of channels in the topic, or {@link #DEFAULT_CHANNEL_COUNT}
         * to indicate that the topic uses the default number of channels.
         *
         * @param cPartition  the topic service partition count used to compute the
         *                    default channel count
         *
         * @return the number of channels in the topic
         */
        int getChannelCount(int cPartition);

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
         * Obtain the page capacity in bytes.
         *
         * @return the capacity
         */
        int getPageCapacity();

        /**
         * Get maximum capacity for a server.
         *
         * @return return the capacity or zero if unlimited.
         */
        long getServerCapacity();

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
        }

    // ----- constants ------------------------------------------------------

    /**
     * The topic should have the default number of channels.
     */
    public static final int DEFAULT_CHANNEL_COUNT = 0;

    /**
     * The default capacity of pages when using the default binary calculator (1MB).
     */
    public static final long DEFAULT_PAGE_CAPACITY_BYTES = 1024*1024;

    /**
     * The default subscriber timeout.
     */
    public static final Seconds DEFAULT_SUBSCRIBER_TIMEOUT_SECONDS = new Seconds(300);

    /**
     * The default reconnect timeout.
     */
    public static final Seconds DEFAULT_RECONNECT_TIMEOUT_SECONDS = new Seconds(300);

    /**
     * The default reconnect retry.
     */
    public static final Seconds DEFAULT_RECONNECT_RETRY_SECONDS = new Seconds(5);

    // ----- data members ---------------------------------------------------

    /**
     * The {@link PagedTopicCaches} instance managing the caches containing this topic's data.
     */
    private final PagedTopicCaches f_pagedTopicCaches;
    }
