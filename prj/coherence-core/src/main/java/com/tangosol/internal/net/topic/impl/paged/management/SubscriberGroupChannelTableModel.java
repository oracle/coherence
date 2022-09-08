/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.management;

import com.tangosol.internal.net.management.model.ModelAttribute;
import com.tangosol.internal.net.management.model.SimpleModelAttribute;
import com.tangosol.internal.net.management.model.TabularModel;

/**
 * A tabular MBean model for the channels within a subscriber group in a
 * {@link com.tangosol.internal.net.topic.impl.paged.PagedTopic}
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public class SubscriberGroupChannelTableModel
        extends TabularModel<SubscriberGroupChannelModel, SubscriberGroupModel>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link SubscriberGroupChannelTableModel}.
     */
    public SubscriberGroupChannelTableModel()
        {
        super("Channels", "Channel statistics",
              ATTRIBUTE_CHANNEL.getName(), getAttributes(),
              SubscriberGroupModel::getChannelCount, SubscriberGroupModel::getChannelModel);
        }

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings("unchecked")
    private static ModelAttribute<SubscriberGroupChannelModel>[] getAttributes()
        {
        return new ModelAttribute[]
                {
                ATTRIBUTE_CHANNEL, ATTRIBUTE_POLLED_COUNT, ATTRIBUTE_POLLED_MEAN,
                ATTRIBUTE_POLLED_ONE_MINUTE, ATTRIBUTE_POLLED_FIVE_MINUTE,
                ATTRIBUTE_POLLED_FIFTEEN_MINUTE, ATTRIBUTE_HEAD
                };
        }

    // ----- constants ------------------------------------------------------

    /**
     * The channel attribute.
     */
    protected static final ModelAttribute<SubscriberGroupChannelModel> ATTRIBUTE_CHANNEL =
            SimpleModelAttribute.intBuilder("Channel", SubscriberGroupChannelModel.class)
                    .withDescription("The topic channel")
                    .withFunction(SubscriberGroupChannelModel::getChannel)
                    .metricTag(true)
                    .build();

    /**
     * The channel attribute.
     */
    protected static final ModelAttribute<SubscriberGroupChannelModel> ATTRIBUTE_HEAD =
            SimpleModelAttribute.stringBuilder("Head", SubscriberGroupChannelModel.class)
                    .withDescription("The head position in the channel")
                    .withFunction(SubscriberGroupChannelModel::getHead)
                    .build();

    /**
     * The polled count attribute.
     */
    protected static final ModelAttribute<SubscriberGroupChannelModel> ATTRIBUTE_POLLED_COUNT =
            PolledMetrics.ATTRIBUTE_COUNT.asBuilder(SubscriberGroupChannelModel.class)
                    .withFunction(SubscriberGroupChannelModel::getPolledCount)
                    .build();

    /**
     * The polled mean rate attribute.
     */
    protected static final ModelAttribute<SubscriberGroupChannelModel> ATTRIBUTE_POLLED_MEAN =
            PolledMetrics.ATTRIBUTE_MEAN_RATE.asBuilder(SubscriberGroupChannelModel.class)
                    .withFunction(SubscriberGroupChannelModel::getPolledMeanRate)
                    .build();

    /**
     * The polled one-minute rate attribute.
     */
    protected static final ModelAttribute<SubscriberGroupChannelModel> ATTRIBUTE_POLLED_ONE_MINUTE =
            PolledMetrics.ATTRIBUTE_ONE_MINUTE_RATE.asBuilder(SubscriberGroupChannelModel.class)
                    .withFunction(SubscriberGroupChannelModel::getPolledOneMinuteRate)
                    .build();

    /**
     * The polled five-minute rate attribute.
     */
    protected static final ModelAttribute<SubscriberGroupChannelModel> ATTRIBUTE_POLLED_FIVE_MINUTE =
            PolledMetrics.ATTRIBUTE_FIVE_MINUTE_RATE.asBuilder(SubscriberGroupChannelModel.class)
                    .withFunction(SubscriberGroupChannelModel::getPolledFiveMinuteRate)
                    .build();

    /**
     * The polled fifteen-minute rate attribute.
     */
    protected static final ModelAttribute<SubscriberGroupChannelModel> ATTRIBUTE_POLLED_FIFTEEN_MINUTE =
            PolledMetrics.ATTRIBUTE_FIFTEEN_MINUTE_RATE.asBuilder(SubscriberGroupChannelModel.class)
                    .withFunction(SubscriberGroupChannelModel::getPolledFifteenMinuteRate)
                    .build();
    }
