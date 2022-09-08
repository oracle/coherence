/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.management;

import com.tangosol.internal.net.topic.impl.paged.PagedTopic;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager;
import com.tangosol.internal.net.topic.impl.paged.statistics.PagedTopicChannelStatistics;
import com.tangosol.internal.net.topic.impl.paged.statistics.PagedTopicStatistics;

/**
 * The MBean model for a channel within a topic.
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public class PagedTopicChannelModel
        implements PublishedMetrics
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a model for a specific channel in a topic.
     *
     * @param pagedTopic  the {@link PagedTopicModel}
     * @param nChannel    the channel
     */
    public PagedTopicChannelModel(PagedTopic<?> pagedTopic, int nChannel)
        {
        PagedTopicBackingMapManager manager    = (PagedTopicBackingMapManager) pagedTopic.getCacheService().getBackingMapManager();
        PagedTopicStatistics        statistics = manager.getStatistics(pagedTopic.getName());

        f_nChannel   = nChannel;
        f_statistics = statistics.getChannelStatistics(f_nChannel);
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
     * Returns the string representation of the tail position in the channel.
     *
     * @return the string representation of the tail position in the channel
     */
    public String getTail()
        {
        return f_statistics.getTail().toString();
        }

    // ----- PublishedMetrics methods ---------------------------------------

    @Override
    public long getPublishedCount()
        {

        return f_statistics.getPublishedCount();
        }

    @Override
    public double getPublishedFifteenMinuteRate()
        {
        return f_statistics.getPublishedFifteenMinuteRate();
        }

    @Override
    public double getPublishedFiveMinuteRate()
        {
        return f_statistics.getPublishedFiveMinuteRate();
        }

    @Override
    public double getPublishedOneMinuteRate()
        {
        return f_statistics.getPublishedOneMinuteRate();
        }

    @Override
    public double getPublishedMeanRate()
        {
        return f_statistics.getPublishedMeanRate();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The channel this model represents
     */
    private final int f_nChannel;

    /**
     * The topic channel statistics.
     */
    private final PagedTopicChannelStatistics f_statistics;
    }
