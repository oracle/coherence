/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.management;

import com.tangosol.internal.net.management.model.AbstractModel;
import com.tangosol.internal.net.management.model.ModelAttribute;
import com.tangosol.internal.net.management.model.SimpleModelAttribute;

import com.tangosol.internal.net.topic.impl.paged.PagedTopic;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager;

import com.tangosol.internal.net.topic.impl.paged.statistics.PagedTopicStatistics;

import javax.management.DynamicMBean;

/**
 * An MBean model for a {@link PagedTopic}
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public class PagedTopicModel
        extends AbstractModel<PagedTopicModel>
        implements DynamicMBean, PublishedMetrics
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PagedTopicModel}.
     *
     * @param pagedTopic  the topic this model represents
     */
    public PagedTopicModel(PagedTopic<?> pagedTopic)
        {
        super(MBEAN_DESCRIPTION);
        f_pagedTopic = pagedTopic;
        f_cChannel   = pagedTopic.getChannelCount();
        f_statistics = ((PagedTopicBackingMapManager) pagedTopic.getCacheService().getBackingMapManager())
                .getStatistics(pagedTopic.getName());

        // create the array of channel models
        f_aChannel = new PagedTopicChannelModel[f_cChannel];
        for (int nChannel = 0; nChannel < f_cChannel; nChannel++)
            {
            f_aChannel[nChannel] = new PagedTopicChannelModel(pagedTopic, nChannel);
            }

        // configure the attributes of the MBean (ordering does not matter)
        addAttribute(ATTRIBUTE_CHANNEL_COUNT);
        addAttribute(ATTRIBUTE_PAGE_CAPACITY);
        addAttribute(ATTRIBUTE_PUBLISHED_COUNT);
        addAttribute(ATTRIBUTE_PUBLISHED_MEAN);
        addAttribute(ATTRIBUTE_PUBLISHED_ONE_MINUTE);
        addAttribute(ATTRIBUTE_PUBLISHED_FIVE_MINUTE);
        addAttribute(ATTRIBUTE_PUBLISHED_FIFTEEN_MINUTE);
        addAttribute(ATTRIBUTE_CHANNEL_TABLE);
        }

    // ----- PagedTopicModel methods ----------------------------------------

    /**
     * Return the channel count for the topic.
     *
     * @return the channel count for the topic
     */
    protected int getChannelCount()
        {
        return f_cChannel;
        }

    /**
     * Return the capacity of a page in the topic.
     *
     * @return the capacity of a page in the topic
     */
    public int getPageCapacity()
        {
        return f_pagedTopic.getDependencies().getPageCapacity();
        }

    /**
     * Return the {@link PagedTopicChannelModel} for a specific channel.
     * <p>
     * The channel parameter is a zero based index of channels and must be
     * greater than or equal to 0 and less than the channel count.
     *
     * @param nChannel  the channel to obtain the model for
     *
     * @return the {@link PagedTopicChannelModel} for the channel
     *
     * @throws IndexOutOfBoundsException if the channel parameter is less than zero
     *         or greater than or equal to the channel count
     */
    protected PagedTopicChannelModel getChannelModel(int nChannel)
        {
        return f_aChannel[nChannel];
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

    // ----- constants ------------------------------------------------------

    /**
     * The MBean's description.
     */
    protected static final String MBEAN_DESCRIPTION = "A Coherence PagedTopic";

    /**
     * The channel count attribute.
     */
    protected static final ModelAttribute<PagedTopicModel> ATTRIBUTE_CHANNEL_COUNT =
            SimpleModelAttribute.intBuilder("ChannelCount", PagedTopicModel.class)
                    .withDescription("The number of channels in the topic")
                    .withFunction(PagedTopicModel::getChannelCount)
                    .metric(false)
                    .build();

    /**
     * The page capacity attribute.
     */
    protected static final ModelAttribute<PagedTopicModel> ATTRIBUTE_PAGE_CAPACITY =
            SimpleModelAttribute.intBuilder("PageCapacity", PagedTopicModel.class)
                    .withDescription("The capacity of a page")
                    .withFunction(PagedTopicModel::getPageCapacity)
                    .build();

    /**
     * The published count attribute.
     */
    protected static final ModelAttribute<PagedTopicModel> ATTRIBUTE_PUBLISHED_COUNT =
            PublishedMetrics.ATTRIBUTE_COUNT.asBuilder(PagedTopicModel.class)
                    .withFunction(PagedTopicModel::getPublishedCount)
                    .build();

    /**
     * The published mean rate attribute.
     */
    protected static final ModelAttribute<PagedTopicModel> ATTRIBUTE_PUBLISHED_MEAN =
            PublishedMetrics.ATTRIBUTE_MEAN_RATE.asBuilder(PagedTopicModel.class)
                    .withFunction(PagedTopicModel::getPublishedMeanRate)
                    .build();

    /**
     * The published one-minute rate attribute.
     */
    protected static final ModelAttribute<PagedTopicModel> ATTRIBUTE_PUBLISHED_ONE_MINUTE =
            PublishedMetrics.ATTRIBUTE_ONE_MINUTE_RATE.asBuilder(PagedTopicModel.class)
                    .withFunction(PagedTopicModel::getPublishedOneMinuteRate)
                    .build();

    /**
     * The published five-minute rate attribute.
     */
    protected static final ModelAttribute<PagedTopicModel> ATTRIBUTE_PUBLISHED_FIVE_MINUTE =
            PublishedMetrics.ATTRIBUTE_FIVE_MINUTE_RATE.asBuilder(PagedTopicModel.class)
                    .withFunction(PagedTopicModel::getPublishedFiveMinuteRate)
                    .build();

    /**
     * The published fifteen-minute rate attribute.
     */
    protected static final ModelAttribute<PagedTopicModel> ATTRIBUTE_PUBLISHED_FIFTEEN_MINUTE =
            PublishedMetrics.ATTRIBUTE_FIFTEEN_MINUTE_RATE.asBuilder(PagedTopicModel.class)
                    .withFunction(PagedTopicModel::getPublishedFifteenMinuteRate)
                    .build();

    /**
     * The channel attributes table.
     */
    protected static final ModelAttribute<PagedTopicModel> ATTRIBUTE_CHANNEL_TABLE = new PagedTopicChannelTableModel();

    // ----- data members ---------------------------------------------------

    /**
     * The paged topic represented by this MBean.
     */
    private final PagedTopic<?> f_pagedTopic;

    /**
     * The topic statistics.
     */
    private final PagedTopicStatistics f_statistics;

    /**
     * The channel count;
     */
    private final int f_cChannel;

    /**
     * The channel models.
     */
    private final PagedTopicChannelModel[] f_aChannel;
    }
