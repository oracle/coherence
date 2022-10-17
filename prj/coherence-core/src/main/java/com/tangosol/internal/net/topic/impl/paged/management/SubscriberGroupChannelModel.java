/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.management;

import com.tangosol.internal.net.topic.impl.paged.statistics.PagedTopicStatistics;
import com.tangosol.internal.net.topic.impl.paged.statistics.SubscriberGroupChannelStatistics;

import java.util.Date;
import java.util.Objects;

/**
 * The MBean model for a channel within a topic subscriber group.
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public class SubscriberGroupChannelModel
        implements PolledMetrics
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a model for a specific channel in a topic.
     *
     * @param statistics  the {@link SubscriberGroupChannelStatistics}
     * @param sGroupName  the subscriber group name
     * @param nChannel    the channel
     */
    public SubscriberGroupChannelModel(PagedTopicStatistics statistics, String sGroupName, int nChannel)
        {
        f_statistics = statistics;
        f_sGroupName = sGroupName;
        f_nChannel   = nChannel;
        }

    /**
     * Returns the channel this model represents.
     *
     * @return the channel this model represents
     */
    public int getChannel()
        {
        return f_nChannel;
        }

    /**
     * Returns the string representation of the head position in the channel.
     *
     * @return the string representation of the head position in the channel
     */
    public String getHead()
        {
        return getStatistics().getHead().toString();
        }

    // ----- PolledMetrics methods ---------------------------------------

    @Override
    public long getPolledCount()
        {

        return getStatistics().getPolledCount();
        }

    @Override
    public double getPolledFifteenMinuteRate()
        {
        return getStatistics().getPolledFifteenMinuteRate();
        }

    @Override
    public double getPolledFiveMinuteRate()
        {
        return getStatistics().getPolledFiveMinuteRate();
        }

    @Override
    public double getPolledOneMinuteRate()
        {
        return getStatistics().getPolledOneMinuteRate();
        }

    @Override
    public double getPolledMeanRate()
        {
        return getStatistics().getPolledMeanRate();
        }

    /**
     * Return the owning subscriber.
     *
     * @return the owning subscriber
     */
    public String getOwningSubscriber()
        {
        return Objects.toString(getStatistics().getOwningSubscriber(), "");
        }

    /**
     * Return the last committed position.
     *
     * @return the last committed position
     */
    public String getLastCommittedPosition()
        {
        return Objects.toString(getStatistics().getLastCommittedPosition(), "");
        }

    /**
     * Return the last committed timestamp.
     *
     * @return the last committed timestamp
     */
    public String getLastCommittedTimestamp()
        {
        long ts = getStatistics().getLastCommittedTimestamp();
        return ts == 0
               ? ""
               : new Date(ts).toString();
        }

    /**
     * Return the last polled timestamp.
     *
     * @return the last polled timestamp
     */
    public String getLastPolledTimestamp()
        {
        long ts = getStatistics().getLastPolledTimestamp();
        return ts == 0
               ? ""
               : new Date(ts).toString();
        }

    /**
     * Return the remaining unpolled messages count.
     *
     * @return the remaining unpolled messages count
     */
    public long getRemainingUnpolledMessages()
        {
        return f_statistics.getChannelStatistics(f_nChannel).getPublishedCount() - getPolledCount();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Returns the {@link SubscriberGroupChannelStatistics} for this model.
     *
     * @return the {@link SubscriberGroupChannelStatistics} for this model
     */
    private SubscriberGroupChannelStatistics getStatistics()
        {
        return f_statistics.getSubscriberGroupStatistics(f_sGroupName).getChannelStatistics(f_nChannel);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The subscriber group channel statistics.
     */
    private final PagedTopicStatistics f_statistics;

    /**
     * The subscriber group name.
     */
    private final String f_sGroupName;

    /**
     * The channel this model represents
     */
    private final int f_nChannel;
    }
