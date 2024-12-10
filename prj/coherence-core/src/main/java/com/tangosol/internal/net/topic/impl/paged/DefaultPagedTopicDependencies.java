/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.util.Duration;
import com.tangosol.internal.util.Primes;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.topic.BinaryElementCalculator;
import com.tangosol.net.topic.NamedTopic;

/**
 * A default implementation of {@link PagedTopicDependencies}.
 *
 * @author Jonathan Knight 2002.09.10
 * @since 23.03
 */
public class DefaultPagedTopicDependencies
        implements PagedTopicDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link DefaultPagedTopicDependencies}.
     *
     * @param cPartition  the partition count of the underlying service
     */
    public DefaultPagedTopicDependencies(int cPartition)
        {
        m_cPartition = cPartition;
        }

    /**
     * A copy constructor to create a {@link DefaultPagedTopicDependencies}
     * from another {@link PagedTopicDependencies} instance.
     *
     * @param deps  the {@link PagedTopicDependencies} to copy
     */
    public DefaultPagedTopicDependencies(PagedTopicDependencies deps)
        {
        this(0);
        setAllowUnownedCommits(deps.isAllowUnownedCommits());
        setChannelCount(deps.getConfiguredChannelCount());
        setElementCalculator(deps.getElementCalculator());
        setElementExpiryMillis(deps.getElementExpiryMillis());
        setMaxBatchSizeBytes(deps.getMaxBatchSizeBytes());
        setPageCapacity(deps.getPageCapacity());
        setReconnectRetryMillis(deps.getReconnectRetryMillis());
        setReconnectTimeoutMillis(deps.getReconnectTimeoutMillis());
        setReconnectWaitMillis(deps.getReconnectWaitMillis());
        setRetainConsumed(deps.isRetainConsumed());
        setServerCapacity(deps.getServerCapacity());
        setSubscriberTimeoutMillis(deps.getSubscriberTimeoutMillis());
        }

    /**
     * Returns the number of channels in the topic, or {@link PagedTopic#DEFAULT_CHANNEL_COUNT}
     * to indicate that the topic uses the default number of channels.
     *
     * @return the number of channels in the topic
     */
    @Override
    public int getConfiguredChannelCount()
        {
        int cChannel = m_cChannel;
        if (cChannel == PagedTopic.DEFAULT_CHANNEL_COUNT)
            {
            cChannel = m_cChannel = computeChannelCount(m_cPartition);
            }
        return cChannel;
        }

    /**
     * Compute the channel count based on the supplied partition count.
     *
     * @param cPartitions the partition count
     *
     * @return the channel count based on the supplied partition count
     */
    public static int computeChannelCount(int cPartitions)
        {
        return Math.min(cPartitions, Primes.next((int) Math.sqrt(cPartitions)));
        }

    /**
     * Set the number of channels in the topic.
     * <p>
     * Valid channel counts are positive integers where zero represents the default channel count.
     * Setting the channel count small reduces the capacity for concurrency, for example, publishers
     * would have more contention as they would all publish to a much smaller number of channels; and
     * subscribers in a group read from a channel, so only having one channel means a group could have
     * no more than one subscriber (although there could be any number of groups).
     * Setting the value too large would have an impact on memory and storage use due to the number of
     * data structures that are duplicated on a per-channel basis. This is similar to the pros and cons
     * of different partition counts in Coherence itself.
     * <p>
     * Ideally, for better distribution, the channel count should be a prime. The default channel count
     * is based on the partition count configured for the underlying cache service and will be the next
     * largest prime above the square root of the partition count. For the Coherence default partition
     * count of 257, this gives a default channel count of 17.
     *
     * @param nChannel  the number of channels in the topic
     *
     * @throws IllegalArgumentException if the channel count parameter is not a positive integer
     *                                  or zero (the {@link PagedTopic#DEFAULT_CHANNEL_COUNT} value).
     */
    public void setChannelCount(int nChannel)
        {
        if (nChannel >= 0)
            {
            m_cChannel = nChannel;
            }
        else
            {
            throw new IllegalArgumentException("Invalid channel count, valid values are positive non-zero integers");
            }
        }

    /**
     * Obtain the page capacity in bytes.
     *
     * @return the capacity
     */
    @Override
    public int getPageCapacity()
        {
        return m_cbPageCapacity;
        }

    /**
     * Set the page capacity in bytes.
     *
     * @param cbPageCapacity  the capacity
     */
    public void setPageCapacity(int cbPageCapacity)
        {
        m_cbPageCapacity = cbPageCapacity;
        }

    /**
     * Get maximum capacity for a server.
     *
     * @return return the capacity or zero if unlimited.
     */
    @Override
    public long getServerCapacity()
        {
        return m_cbServerCapacity;
        }

    /**
     * Set the maximum capacity for a sever, or zero for unlimited.
     *
     * @param cbServer the maximum capacity for a server.
     */
    public void setServerCapacity(long cbServer)
        {
        m_cbServerCapacity = cbServer;
        }

    /**
     * Obtain the expiry delay to apply to elements in ths topic.
     *
     * @return  the expiry delay to apply to elements in ths topic
     */
    @Override
    public long getElementExpiryMillis()
        {
        return m_cMillisExpiry;
        }

    /**
     * Set the expiry delay to apply to elements in ths topic.
     *
     * @param cMillisExpiry  the expiry delay to apply to elements in ths topic
     */
    public void setElementExpiryMillis(long cMillisExpiry)
        {
        m_cMillisExpiry = cMillisExpiry;
        }

    /**
     * Return the maximum size of a batch.
     *
     * @return the max batch size
     */
    @Override
    public long getMaxBatchSizeBytes()
        {
        return m_cbMaxBatch;
        }

    /**
     * Specify the maximum size of a batch.
     *
     * @param cb  the max batch size
     */
    public void setMaxBatchSizeBytes(long cb)
        {
        m_cbMaxBatch = cb;
        }

    @Override
    public boolean isRetainConsumed()
        {
        return m_fRetainConsumed;
        }

    /**
     * Set the flag indicating whether the topic retains consumed messages.
     *
     * @param fRetainElements  {@code true} to retain consumed messages
     */
    public void setRetainConsumed(boolean fRetainElements)
        {
        m_fRetainConsumed = fRetainElements;
        }

    /**
     * Returns number of milliseconds within which a subscriber must issue a heartbeat or
     * be forcefully considered closed.
     *
     * @return number of milliseconds within which a subscriber must issue a heartbeat
     */
    @Override
    public long getSubscriberTimeoutMillis()
        {
        return m_cSubscriberTimeoutMillis;
        }

    /**
     * Set the number of milliseconds within which a subscriber must issue a heartbeat or
     * be forcefully considered closed.
     * <p>
     * An timeout time of less than zero will never time out.
     * <p>
     * The minimum allowed timeout time is one second.
     *
     * @param cMillis  the number of milliseconds within which a subscriber must issue a heartbeat
     */
    public void setSubscriberTimeoutMillis(long cMillis)
        {
        m_cSubscriberTimeoutMillis = cMillis <= 0 ? LocalCache.EXPIRY_NEVER : Math.max(1000L, cMillis);
        }

    /**
     * Returns the timeout that a subscriber will use when waiting for its first allocation of channels.
     *
     * @return the timeout that a subscriber will use when waiting for its first allocation of channels
     */
    @Override
    public long getNotificationTimeout()
        {
        if (m_cSubscriberTimeoutMillis == LocalCache.EXPIRY_NEVER)
            {
            return LocalCache.EXPIRY_NEVER;
            }
        else
            {
            return m_cSubscriberTimeoutMillis / 2;
            }
        }

    /**
     * Returns {@code true} if the topic allows commits of a position in a channel to be
     * made by subscribers that do not own the channel.
     *
     * @return {@code true} if the topic allows commits of a position in a channel to be
     *         made by subscribers that do not own the channel
     */
    @Override
    public boolean isAllowUnownedCommits()
        {
        return m_fAllowUnownedCommits;
        }

    /**
     * Returns {@code true} if the topic only allows commits of a position in a channel to be
     * made by subscribers that own the channel.
     *
     * @return {@code true} if the topic only allows commits of a position in a channel to be
     *         made by subscribers that own the channel
     */
    @Override
    public boolean isOnlyOwnedCommits()
        {
        return !m_fAllowUnownedCommits;
        }

    /**
     * Set the flag indicating whether the topic allows commits of a position in a channel to be
     * made by subscribers that do not own the channel.
     * <p>
     * Setting this flag to {@code true} would typically be to allow for subscribers that have just
     * had a channel allocation revoked due to subscriber redistribution to still commit the
     * positions of elements they have just processed. This is on the understanding that the
     * subscriber that the channels have been reallocated to could have also processed the message
     * and be about to (or already have) committed.
     * <p>
     * The default value is {@code false} to disallow commits of unowned channels as this assumes
     * that a revoked channel has been reallocated to a different subscriber and that subscriber
     * will then read from the last commit and reprocess and commit any uncommitted messages.
     *
     * @param f  {@code true} if the topic should allow commits of a position in a channel to be
     *           made by subscribers that do not own the channel, or {@code false} to disallow
     *           commits to be made by subscribes that do now own the channel
     */
    public void setAllowUnownedCommits(boolean f)
        {
        m_fAllowUnownedCommits = f;
        }

    @Override
    public NamedTopic.ElementCalculator getElementCalculator()
        {
        return m_calculator;
        }

    /**
     * Set the calculator used to calculate element sizes.
     *
     * @param calculator  the calculator used to calculate element sizes
     */
    public void setElementCalculator(NamedTopic.ElementCalculator calculator)
        {
        m_calculator = calculator == null ? BinaryElementCalculator.INSTANCE : calculator;
        }

    @Override
    public long getReconnectTimeoutMillis()
        {
        return m_cReconnectTimeoutMillis;
        }

    /**
     * Set the maximum amount of time publishers and subscribers will
     * attempt to reconnect after being disconnected.
     *
     * @param cMillis  the maximum amount of time publishers and subscribers will
     *                 attempt to reconnect after being disconnected
     */
    public void setReconnectTimeoutMillis(long cMillis)
        {
        m_cReconnectTimeoutMillis = cMillis <= 0 ? 0 : Math.max(1000L, cMillis);
        }

    @Override
    public long getReconnectRetryMillis()
        {
        return m_cReconnectRetryMillis;
        }

    /**
     * Set the amount of time publishers and subscribers will wait between
     * attempts to reconnect after being disconnected.
     *
     * @param cMillis  the maximum amount of time publishers and subscribers will
     *                 wait between attempts to reconnect after being disconnected
     */
    public void setReconnectRetryMillis(long cMillis)
        {
        m_cReconnectRetryMillis = cMillis <= 0 ? 0 : Math.max(1000L, cMillis);
        }

    @Override
    public long getReconnectWaitMillis()
        {
        return m_cReconnectWaitMillis;
        }

    /**
     * Set the amount of time publishers and subscribers will wait before
     * attempting to reconnect after being disconnected.
     *
     * @param cMillis  the maximum amount of time publishers and subscribers will
     *                 wait before attempting to reconnect after being disconnected
     */
    public void setReconnectWaitMillis(long cMillis)
        {
        m_cReconnectWaitMillis = cMillis <= 0 ? 1000L : Math.max(1000L, cMillis);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "PageTopicScheme Configuration: Page=" + m_cbPageCapacity + "b, " +
                "CacheServerMaxStorage=" + m_cbServerCapacity + "," +
                "Expiry=" + m_cMillisExpiry + "ms, " +
                "MaxBatch=" + m_cbMaxBatch + "b, " +
                "RetainConsumed=" + m_fRetainConsumed + ", " +
                "ElementCalculator=" + m_calculator.getName() + ", " +
                "SubscriberTimeout=" + m_cSubscriberTimeoutMillis + "ms " +
                "ReconnectWait=" + m_cReconnectWaitMillis + "ms " +
                "ReconnectTimeout=" + m_cReconnectTimeoutMillis + "ms " +
                "ReconnectRetry=" + m_cReconnectRetryMillis + "ms " +
                "AllowUnownedCommits=" + m_fAllowUnownedCommits;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The partition count on the underlying cache service.
     */
    private final int m_cPartition;

    /**
     * The number of channels in this topic.
     */
    private int m_cChannel;

    /**
     * The maximum number of elements that pages
     * in this topic can contain.
     */
    private int m_cbPageCapacity = 0;

    /**
     * The maximum storage usage per cache server
     */
    private long m_cbServerCapacity = Long.MAX_VALUE;

    /**
     * The expiry time for elements offered to the topic
     */
    private long m_cMillisExpiry = LocalCache.DEFAULT_EXPIRE;

    /**
     * The target maximum batch size.
     */
    private long m_cbMaxBatch;

    /**
     * Flag indicating whether this topic retains elements after they have been
     * read by subscribers.
     */
    private boolean m_fRetainConsumed;

    /**
     * The number of milliseconds within which a subscriber must issue a heartbeat or
     * be forcefully considered closed.
     */
    private long m_cSubscriberTimeoutMillis = PagedTopic.DEFAULT_SUBSCRIBER_TIMEOUT_SECONDS.as(Duration.Magnitude.MILLI);

    /**
     * A flag that when {@code true}, indicates that the topic allows positions to be committed in channels
     * that are not owned by the subscriber performing the commit.
     * <p>
     * This would typically be set to {@code true} to allow subscribers that have just lost ownership of a
     * channel to still commit what they have processed.
     */
    private boolean m_fAllowUnownedCommits;

    /**
     * The unit calculator used to calculate topic element sizes.
     */
    private NamedTopic.ElementCalculator m_calculator = BinaryElementCalculator.INSTANCE;

    /**
     * The maximum amount of time that publishers and subscribers will attempt to reconnect.
     */
    private long m_cReconnectTimeoutMillis = PagedTopic.DEFAULT_RECONNECT_TIMEOUT_SECONDS.as(Duration.Magnitude.MILLI);

    /**
     * The amount of time that publishers and subscribers will wait between attempts to reconnect.
     */
    private long m_cReconnectRetryMillis = PagedTopic.DEFAULT_RECONNECT_RETRY_SECONDS.as(Duration.Magnitude.MILLI);

    /**
     * The amount of time that publishers and subscribers will wait before attempting to reconnect.
     */
    private long m_cReconnectWaitMillis = PagedTopic.DEFAULT_RECONNECT_WAIT_SECONDS.as(Duration.Magnitude.MILLI);
    }
