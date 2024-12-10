/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.statistics;

import com.tangosol.internal.net.metrics.Meter;
import com.tangosol.internal.net.topic.impl.paged.management.PolledMetrics;
import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import com.tangosol.util.LongArray;
import com.tangosol.util.SimpleLongArray;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The class holding statistics for a subscriber group in a
 * {@link com.tangosol.internal.net.topic.impl.paged.PagedTopic}.
 * <p>
 * Statistics are only for the local member.
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public class SubscriberGroupStatistics
        implements PolledMetrics
    {
    /**
     * Create a {@link SubscriberGroupStatistics}.
     *
     * @param cChannel  the current channel count
     */
    public SubscriberGroupStatistics(int cChannel)
        {
        for (int i = 0; i < cChannel; i++)
            {
            m_aChannelStats.set(i, new SubscriberGroupChannelStatistics(i));
            }
        }

    /**
     * Return the {@link SubscriberGroupChannelStatistics statistics} for a channel.
     *
     * @param nChannel  the channel to get statistics for
     *
     * @return the {@link SubscriberGroupChannelStatistics statistics} for the channel
     */
    public SubscriberGroupChannelStatistics getChannelStatistics(int nChannel)
        {
        SubscriberGroupChannelStatistics statistics = m_aChannelStats.get(nChannel);
        if (statistics == null)
            {
            f_lock.lock();
            try
                {
                statistics = new SubscriberGroupChannelStatistics(nChannel);
                m_aChannelStats.set(nChannel, statistics);
                }
            finally
                {
                f_lock.unlock();
                }
            }
        return statistics;
        }

    /**
     * Update the polled messages metric.
     *
     * @param nChannel  the channel polled from
     * @param cMessage  the number of messages polled
     * @param head      the new head for the channel
     */
    public void onPolled(int nChannel, long cMessage, PagedPosition head)
        {
        f_metricPolled.mark(cMessage);
        getChannelStatistics(nChannel).onPolled(cMessage, head);
        }

    /**
     * Update the committed messages metric.
     *
     * @param nChannel           the channel polled from
     * @param committedPosition  the committed position
     */
    public void onCommitted(int nChannel, PagedPosition committedPosition)
        {
        getChannelStatistics(nChannel).onCommitted(committedPosition);
        }

    /**
     * Set the owning subscriber.
     *
     * @param nChannel           the channel
     * @param nOwningSubscriber  the owning subscriber
     */
    public void setOwningSubscriber(int nChannel, SubscriberId nOwningSubscriber)
        {
        getChannelStatistics(nChannel).setOwningSubscriber(nOwningSubscriber);
        }

    @Override
    public long getPolledCount()
        {
        return f_metricPolled.getCount();
        }

    @Override
    public double getPolledFifteenMinuteRate()
        {
        return f_metricPolled.getFifteenMinuteRate();
        }

    @Override
    public double getPolledFiveMinuteRate()
        {
        return f_metricPolled.getFiveMinuteRate();
        }

    @Override
    public double getPolledOneMinuteRate()
        {
        return f_metricPolled.getOneMinuteRate();
        }

    @Override
    public double getPolledMeanRate()
        {
        return f_metricPolled.getMeanRate();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The channel statistics.
     */
    @SuppressWarnings("unchecked")
    private final LongArray<SubscriberGroupChannelStatistics> m_aChannelStats = new SimpleLongArray();

    /**
     * The lock to use to synchronize access to internal state.
     */
    private final Lock f_lock = new ReentrantLock(true);

    /**
     * The polled messages metric.
     */
    private final Meter f_metricPolled = new Meter();
    }
