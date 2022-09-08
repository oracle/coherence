/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.statistics;

import com.tangosol.internal.net.metrics.Histogram;
import com.tangosol.internal.net.metrics.Meter;
import com.tangosol.internal.net.metrics.Snapshot;
import com.tangosol.internal.net.topic.impl.paged.management.PublishedMetrics;
import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;

import java.util.Objects;

/**
 * A class holding statistics for a channel in a
 * {@link com.tangosol.internal.net.topic.impl.paged.PagedTopic}.
 * <p>
 * Statistics are only for the local member.
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public class PagedTopicChannelStatistics
        implements PublishedMetrics
    {
    /**
     * Create a {@link PagedTopicChannelStatistics}.
     *
     * @param nChannel  the channel these statistics represent
     */
    public PagedTopicChannelStatistics(int nChannel)
        {
        f_nChannel = nChannel;
        }

    /**
     * Return the channel these statistics represent.
     *
     * @return the channel these statistics represent
     */
    public int getChannel()
        {
        return f_nChannel;
        }

    @Override
    public long getPublishedCount()
        {
        return f_metricPublished.getCount();
        }

    @Override
    public double getPublishedFifteenMinuteRate()
        {
        return f_metricPublished.getFifteenMinuteRate();
        }

    @Override
    public double getPublishedFiveMinuteRate()
        {
        return f_metricPublished.getFiveMinuteRate();
        }

    @Override
    public double getPublishedOneMinuteRate()
        {
        return f_metricPublished.getOneMinuteRate();
        }

    @Override
    public double getPublishedMeanRate()
        {
        return f_metricPublished.getMeanRate();
        }

    /**
     * Update the published messages metric.
     *
     * @param cMessage  the number of messages published
     * @param tail      the new tail position
     */
    public void onPublished(long cMessage, PagedPosition tail)
        {
        f_metricPublished.mark(cMessage);
        setTail(tail);
        }

    /**
     * Returns the current tail for the channel
     *
     * @return the current tail for the channel
     */
    public PagedPosition getTail()
        {
        return m_tail;
        }

    /**
     * Set the tail position.
     *
     * @param tail the new tail position
     */
    public void setTail(PagedPosition tail)
        {
        if (Objects.requireNonNull(tail).compareTo(m_tail) > 0)
            {
            m_tail = tail;
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The channel these statistics represent.
     */
    private final int f_nChannel;

    /**
     * The published messages metric.
     */
    private final Meter f_metricPublished = new Meter();

    /**
     * The tail position in the channel.
     */
    private PagedPosition m_tail = PagedPosition.NULL_POSITION;
    }
