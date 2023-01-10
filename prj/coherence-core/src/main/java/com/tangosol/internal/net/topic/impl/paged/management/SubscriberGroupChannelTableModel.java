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
 * A tabular MBean model for the channels within a subscriber group in a
 * {@link com.tangosol.internal.net.topic.impl.paged.PagedTopic}
 *
 * @author Jonathan Knight 2022.09.10
 * @since 22.06.4
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
                ATTRIBUTE_POLLED_FIFTEEN_MINUTE, ATTRIBUTE_HEAD,
                ATTRIBUTE_OWNING_SUBSCRIBER_ID, ATTRIBUTE_OWNING_SUBSCRIBER_MEMBER_ID,
                ATTRIBUTE_OWNING_SUBSCRIBER_MEMBER_UUID, ATTRIBUTE_OWNING_SUBSCRIBER_NOTIFICATION_ID,
                ATTRIBUTE_LAST_COMMITTED_POSITION,
                ATTRIBUTE_LAST_COMMITTED_TIMESTAMP, ATTRIBUTE_LAST_POLLED_TIMESTAMP,
                ATTRIBUTE_REMAINING_UNPOLLED_MESSAGES
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

    /**
     * The owning subscriber attribute.
     */
    protected static final ModelAttribute<SubscriberGroupChannelModel> ATTRIBUTE_OWNING_SUBSCRIBER_ID =
            SimpleModelAttribute.longBuilder("OwningSubscriberId", SubscriberGroupChannelModel.class)
                    .withDescription("The Owning Subscriber ID")
                    .withFunction(SubscriberGroupChannelModel::getOwningSubscriber)
                    .metric("OwningSubscriberId")
                    .build();

    /**
     * The owning subscriber member id attribute.
     */
    protected static final ModelAttribute<SubscriberGroupChannelModel> ATTRIBUTE_OWNING_SUBSCRIBER_NOTIFICATION_ID =
            SimpleModelAttribute.intBuilder("OwningSubscriberMemberNotificationId", SubscriberGroupChannelModel.class)
                    .withDescription("The Owning Subscriber Notification ID")
                    .withFunction(SubscriberGroupChannelModel::getOwningSubscriberNotificationId)
                    .build();

    /**
     * The owning subscriber member id attribute.
     */
    protected static final ModelAttribute<SubscriberGroupChannelModel> ATTRIBUTE_OWNING_SUBSCRIBER_MEMBER_ID =
            SimpleModelAttribute.intBuilder("OwningSubscriberMemberId", SubscriberGroupChannelModel.class)
                    .withDescription("The Owning Subscriber Member ID")
                    .withFunction(SubscriberGroupChannelModel::getOwningSubscriberMemberId)
                    .build();

    /**
     * The owning subscriber member uuid attribute.
     */
    protected static final ModelAttribute<SubscriberGroupChannelModel> ATTRIBUTE_OWNING_SUBSCRIBER_MEMBER_UUID =
            SimpleModelAttribute.stringBuilder("OwningSubscriberMemberUuid", SubscriberGroupChannelModel.class)
                    .withDescription("The Owning Subscriber Member UUID")
                    .withFunction(SubscriberGroupChannelModel::getOwningSubscriberMemberUuid)
                    .build();

    /**
     * The last committed position attribute.
     */
    protected static final ModelAttribute<SubscriberGroupChannelModel> ATTRIBUTE_LAST_COMMITTED_POSITION =
            SimpleModelAttribute.stringBuilder("LastCommittedPosition", SubscriberGroupChannelModel.class)
                    .withDescription("The Last Committed Position")
                    .withFunction(SubscriberGroupChannelModel::getLastCommittedPosition)
                    .build();

    /**
     * The last committed timestamp attribute.
     */
    protected static final ModelAttribute<SubscriberGroupChannelModel> ATTRIBUTE_LAST_COMMITTED_TIMESTAMP =
            SimpleModelAttribute.stringBuilder("LastCommittedTimestamp", SubscriberGroupChannelModel.class)
                    .withDescription("The Last Committed Timestamp")
                    .withFunction(SubscriberGroupChannelModel::getLastCommittedTimestamp)
                    .build();

    /**
     * The last polled timestamp attribute.
     */
    protected static final ModelAttribute<SubscriberGroupChannelModel> ATTRIBUTE_LAST_POLLED_TIMESTAMP =
            SimpleModelAttribute.stringBuilder("LastPolledTimestamp", SubscriberGroupChannelModel.class)
                    .withDescription("The Last Polled Timestamp")
                    .withFunction(SubscriberGroupChannelModel::getLastPolledTimestamp)
                    .build();

    /**
     * The remaining unpolled messages timestamp attribute.
     */
    protected static final ModelAttribute<SubscriberGroupChannelModel> ATTRIBUTE_REMAINING_UNPOLLED_MESSAGES =
            SimpleModelAttribute.longBuilder("RemainingUnpolledMessages", SubscriberGroupChannelModel.class)
                    .withDescription("The Remaining Unpolled Messages")
                    .withFunction(SubscriberGroupChannelModel::getRemainingUnpolledMessages)
                    .metric(true)
                    .build();
    }
