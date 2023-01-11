/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.management;

import com.tangosol.internal.net.topic.impl.paged.statistics.PagedTopicChannelStatistics;
import com.tangosol.internal.net.topic.impl.paged.statistics.PagedTopicStatistics;

import java.util.function.Supplier;

/**
 * The MBean model for a channel within a topic.
 *
 * @author Jonathan Knight 2022.09.10
 * @since 22.06.4
 */
public class PagedTopicChannelModel
        implements PublishedMetrics
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a model for a specific channel in a topic.
     *
     * @param supplier  the {@link PagedTopicStatistics} supplier
     * @param nChannel  the channel
     */
    public PagedTopicChannelModel(Supplier<PagedTopicStatistics> supplier, int nChannel)
        {
        f_nChannel   = nChannel;
        f_statistics = supplier;
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
        return getStatistics().getTail().toString();
        }

    // ----- PublishedMetrics methods ---------------------------------------

    @Override
    public long getPublishedCount()
        {

        return getStatistics().getPublishedCount();
        }

    @Override
    public double getPublishedFifteenMinuteRate()
        {
        return getStatistics().getPublishedFifteenMinuteRate();
        }

    @Override
    public double getPublishedFiveMinuteRate()
        {
        return getStatistics().getPublishedFiveMinuteRate();
        }

    @Override
    public double getPublishedOneMinuteRate()
        {
        return getStatistics().getPublishedOneMinuteRate();
        }

    @Override
    public double getPublishedMeanRate()
        {
        return getStatistics().getPublishedMeanRate();
        }

    // ----- helper methods -------------------------------------------------
    
    private PagedTopicChannelStatistics getStatistics()
        {
        return f_statistics.get().getChannelStatistics(f_nChannel);
        }
    
    // ----- data members ---------------------------------------------------

    /**
     * The channel this model represents
     */
    private final int f_nChannel;

    /**
     * The {@link PagedTopicStatistics} supplier.
     */
    private final Supplier<PagedTopicStatistics> f_statistics;
    }
