/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.statistics;

import com.tangosol.internal.net.metrics.Meter;
import com.tangosol.internal.net.topic.impl.paged.management.PolledMetrics;
import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;

import com.tangosol.util.Base;

import java.util.Objects;

/**
 * The class holding statistics for a channel in a subscriber group in a
 * {@link com.tangosol.internal.net.topic.impl.paged.PagedTopic}.
 * <p>
 * Statistics are only for the local member.
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public class SubscriberGroupChannelStatistics
        implements PolledMetrics
    {
    /**
     * Create a {@link SubscriberGroupChannelStatistics}.
     *
     * @param nChannel  the channel these statistics represent
     */
    public SubscriberGroupChannelStatistics(int nChannel)
        {
        f_nChannel = nChannel;
        }

    /**
     * Returns the channel these statistics represent.
     *
     * @return the channel these statistics represent
     */
    public int getChannel()
        {
        return f_nChannel;
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

    /**
     * Update the polled messages metric.
     *
     * @param cMessage  the number of messages polled
     * @param head      the new channel head
     */
    public void onPolled(long cMessage, PagedPosition head)
        {
        f_metricPolled.mark(cMessage);
        setHead(head);
        }

    /**
     * Return the current channel head.
     *
     * @return  the current channel head
     */
    public PagedPosition getHead()
        {
        return m_head;
        }

    /**
     * Set the head position for the channel.
     *
     * @param head the head position
     */
    public void setHead(PagedPosition head)
        {
        if (Objects.requireNonNull(head).compareTo(m_head) > 0)
            {
            m_head = head;
            m_lastPolledTimestamp = Base.getSafeTimeMillis();
            }
        }

    /**
     * Return the owning subscriber.
     *
     * @return the owning subscriber
     */
    public SubscriberId getOwningSubscriber()
        {
        return m_owningSubscriber;
        }

    /**
     * Set the owning subscriber for the channel.
     *
     * @param nOwningSubscriber the owning subscriber
     */
    public void setOwningSubscriber(SubscriberId nOwningSubscriber)
        {
        m_owningSubscriber = nOwningSubscriber;
        }

    /**
     * Update the committed position.
     *
     * @param committedPosition  the new committed position
     */
    public void onCommitted(PagedPosition committedPosition)
        {
        m_committedPosition = committedPosition;
        m_lastCommittedTimestamp = Base.getSafeTimeMillis();
        }

    /**
     * Return the last committed position.
     *
     * @return  the last committed position
     */
    public PagedPosition getLastCommittedPosition()
        {
        return m_committedPosition;
        }

    /**
     * Return the last committed timestamp.
     *
     * @return the last committed timestamp
     */
    public long getLastCommittedTimestamp()
        {
        return m_lastCommittedTimestamp;
        }

    /**
     * Return the last polled timestamp.
     *
     * @return the last polled timestamp
     */
    public long getLastPolledTimestamp()
        {
        return m_lastPolledTimestamp;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The channel these statistics represent
     */
    private final int f_nChannel;

    /**
     * The polled messages metric.
     */
    private final Meter f_metricPolled = new Meter();

    /**
     * The current head position.
     */
    private PagedPosition m_head = PagedPosition.NULL_POSITION;

    /**
     * The owning subscriber.
     */
    private SubscriberId m_owningSubscriber;

    /**
     * The last committed position.
     */
    private PagedPosition m_committedPosition;

    /**
     * The last committed timestamp.
     */
    private long m_lastCommittedTimestamp;

    /**
     * The last polled timestamp.
     */
    private long m_lastPolledTimestamp;
    }
