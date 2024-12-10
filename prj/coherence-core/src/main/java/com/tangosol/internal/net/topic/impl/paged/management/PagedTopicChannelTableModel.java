/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.management;

import com.tangosol.internal.net.management.model.ModelAttribute;
import com.tangosol.internal.net.management.model.SimpleModelAttribute;
import com.tangosol.internal.net.management.model.TabularModel;

/**
 * A tabular MBean model for the channels within a
 * {@link com.tangosol.internal.net.topic.impl.paged.PagedTopic}
 *
 * @author Jonathan Knight 2022.09.10
 * @since 22.06.4
 */
public class PagedTopicChannelTableModel
        extends TabularModel<PagedTopicChannelModel, PagedTopicModel>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PagedTopicChannelTableModel}.
     */
    public PagedTopicChannelTableModel()
        {
        super(TABLE_NAME, TABLE_DESCRIPTION, ATTRIBUTE_CHANNEL.getName(), getAttributes(),
              PagedTopicModel::getChannelCount, PagedTopicModel::getChannelModel);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create the array of {@link ModelAttribute attributes} for this model.
     *
     * @return the array of {@link ModelAttribute attributes} for this model
     */
    @SuppressWarnings("unchecked")
    private static ModelAttribute<PagedTopicChannelModel>[] getAttributes()
        {
        // ordering of attributes in the array does not matter
        return new ModelAttribute[]
                {
                ATTRIBUTE_CHANNEL,
                ATTRIBUTE_PUBLISHED_COUNT,
                ATTRIBUTE_PUBLISHED_MEAN,
                ATTRIBUTE_PUBLISHED_ONE_MINUTE,
                ATTRIBUTE_PUBLISHED_FIVE_MINUTE,
                ATTRIBUTE_PUBLISHED_FIFTEEN_MINUTE,
                ATTRIBUTE_TAIL
                };
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of this attribute.
     */
    public static final String TABLE_NAME = "Channels";

    /**
     * The description of this attribute.
     */
    public static final String TABLE_DESCRIPTION = "Channel statistics.";

    /**
     * The channel attribute.
     */
    protected static final ModelAttribute<PagedTopicChannelModel> ATTRIBUTE_CHANNEL =
            SimpleModelAttribute.intBuilder("Channel", PagedTopicChannelModel.class)
                    .withDescription("The topic channel.")
                    .withFunction(PagedTopicChannelModel::getChannel)
                    .metricTag(true)
                    .build();

    /**
     * The channel attribute.
     */
    protected static final ModelAttribute<PagedTopicChannelModel> ATTRIBUTE_TAIL =
            SimpleModelAttribute.stringBuilder("Tail", PagedTopicChannelModel.class)
                    .withDescription("The tail position in the channel.")
                    .withFunction(PagedTopicChannelModel::getTail)
                    .build();

    /**
     * The published count attribute.
     */
    protected static final ModelAttribute<PagedTopicChannelModel> ATTRIBUTE_PUBLISHED_COUNT =
            PublishedMetrics.ATTRIBUTE_COUNT.asBuilder(PagedTopicChannelModel.class)
                    .withFunction(PagedTopicChannelModel::getPublishedCount)
                    .build();

    /**
     * The published mean rate attribute.
     */
    protected static final ModelAttribute<PagedTopicChannelModel> ATTRIBUTE_PUBLISHED_MEAN =
            PublishedMetrics.ATTRIBUTE_MEAN_RATE.asBuilder(PagedTopicChannelModel.class)
                    .withFunction(PagedTopicChannelModel::getPublishedMeanRate)
                    .build();

    /**
     * The published one-minute rate attribute.
     */
    protected static final ModelAttribute<PagedTopicChannelModel> ATTRIBUTE_PUBLISHED_ONE_MINUTE =
            PublishedMetrics.ATTRIBUTE_ONE_MINUTE_RATE.asBuilder(PagedTopicChannelModel.class)
                    .withFunction(PagedTopicChannelModel::getPublishedOneMinuteRate)
                    .build();

    /**
     * The published five-minute rate attribute.
     */
    protected static final ModelAttribute<PagedTopicChannelModel> ATTRIBUTE_PUBLISHED_FIVE_MINUTE =
            PublishedMetrics.ATTRIBUTE_FIVE_MINUTE_RATE.asBuilder(PagedTopicChannelModel.class)
                    .withFunction(PagedTopicChannelModel::getPublishedFiveMinuteRate)
                    .build();

    /**
     * The published fifteen-minute rate attribute.
     */
    protected static final ModelAttribute<PagedTopicChannelModel> ATTRIBUTE_PUBLISHED_FIFTEEN_MINUTE =
            PublishedMetrics.ATTRIBUTE_FIFTEEN_MINUTE_RATE.asBuilder(PagedTopicChannelModel.class)
                    .withFunction(PagedTopicChannelModel::getPublishedFifteenMinuteRate)
                    .build();
    }
