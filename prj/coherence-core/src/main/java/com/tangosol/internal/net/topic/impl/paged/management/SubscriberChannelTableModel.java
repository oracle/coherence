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
import com.tangosol.net.topic.Subscriber;

/**
 * A table model representing topic subscriber channel statistics.
 *
 * @author Jonathan Knight 2022.09.10
 * @since 22.06.4
 */
public class SubscriberChannelTableModel
        extends TabularModel<Subscriber.Channel, SubscriberModel>
    {
    /**
     * Create a {@link SubscriberChannelTableModel}.
     */
    public SubscriberChannelTableModel()
        {
        super("Channels", "The subscriber's channel details.",
              ATTRIBUTE_CHANNEL.getName(), getAttributes(),
              SubscriberModel::getChannelCount, SubscriberModel::getChannel);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Returns the attributes for the MBean.
     *
     * @return the attributes for the MBean
     */
    @SuppressWarnings("unchecked")
    private static ModelAttribute<Subscriber.Channel>[] getAttributes()
        {
        // ordering of attributes does not matter
        return new ModelAttribute[] {
                ATTRIBUTE_CHANNEL, 
                ATTRIBUTE_EMPTY, 
                ATTRIBUTE_HEAD,
                ATTRIBUTE_LAST_COMMIT,
                ATTRIBUTE_COMMIT_COUNT,
                ATTRIBUTE_LAST_RECEIVED,
                ATTRIBUTE_RECEIVE_COUNT,
                ATTRIBUTE_LAST_POLLED,
                ATTRIBUTE_OWNED,
                ATTRIBUTE_OWNED_CODE,
                ATTRIBUTE_LAST_POLLED_TIMESTAMP,
                ATTRIBUTE_POLL_COUNT,
                ATTRIBUTE_FIRST_POLLED,
                ATTRIBUTE_FIRST_POLLED_TIMESTAMP,
                ATTRIBUTE_RECEIVE_COMPLETIONS,
                ATTRIBUTE_RECEIVE_COMPLETIONS_MEAN,
                ATTRIBUTE_RECEIVE_COMPLETIONS_ONE,
                ATTRIBUTE_RECEIVE_COMPLETIONS_FIVE,
                ATTRIBUTE_RECEIVE_COMPLETIONS_FIFTEEN
                };
        }

    // ----- constants ------------------------------------------------------

    /**
     * The channel identifier attribute.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_CHANNEL =
            SimpleModelAttribute.intBuilder("Channel", Subscriber.Channel.class)
                    .withDescription("The number of channels in the topic.")
                    .withFunction(Subscriber.Channel::getId)
                    .metricTag(true)
                    .build();

    /**
     * The empty channel attribute.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_EMPTY =
            SimpleModelAttribute.booleanBuilder("Empty", Subscriber.Channel.class)
                    .withDescription("A flag indicating whether the channel is empty.")
                    .withFunction(Subscriber.Channel::isEmpty)
                    .build();

    /**
     * The channel head position.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_HEAD =
            SimpleModelAttribute.stringBuilder("Head", Subscriber.Channel.class)
                    .withDescription("The position this subscriber knows as the head for the channel.")
                    .withFunction(c -> String.valueOf(c.getHead()))
                    .build();

    /**
     * The position of the last element received from the channel.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_LAST_RECEIVED =
            SimpleModelAttribute.stringBuilder("LastReceived", Subscriber.Channel.class)
                    .withDescription("The last position received by this subscriber since it was last assigned ownership of this channel.")
                    .withFunction(c -> String.valueOf(c.getLastReceived()))
                    .build();

    /**
     * The number of completed receive requests.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_RECEIVE_COUNT =
            SimpleModelAttribute.longBuilder("ReceivedCount", Subscriber.Channel.class)
                    .withDescription("The number of receive requests completed from this channel.")
                    .withFunction(Subscriber.Channel::getReceiveCount)
                    .metric(true)
                    .build();

    /**
     * The number elements polled from the channel.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_POLL_COUNT =
            SimpleModelAttribute.longBuilder("PolledCount", Subscriber.Channel.class)
                    .withDescription("The number of elements in the channel polled by this subscriber.")
                    .withFunction(Subscriber.Channel::getPolls)
                    .metric(true)
                    .build();

    /**
     * The position of the first element polled from the channel.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_FIRST_POLLED =
            SimpleModelAttribute.stringBuilder("FirstPolled", Subscriber.Channel.class)
                    .withDescription("The first position in the channel polled by this subscriber.")
                    .withFunction(c -> String.valueOf(c.getFirstPolled()))
                    .build();

    /**
     * The timestamp of the first element polled from the channel.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_FIRST_POLLED_TIMESTAMP =
            SimpleModelAttribute.longBuilder("FirstPolledTimestamp", Subscriber.Channel.class)
                    .withDescription("The first position in the channel polled by this subscriber.")
                    .withFunction(Subscriber.Channel::getFirstPolledTimestamp)
                    .build();

    /**
     * The position of the last element polled from the channel.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_LAST_POLLED =
            SimpleModelAttribute.stringBuilder("LastPolled", Subscriber.Channel.class)
                    .withDescription("The last position polled by this subscriber since it was last assigned ownership of this channel.")
                    .withFunction(c -> String.valueOf(c.getLastPolled()))
                    .build();

    /**
     * The timestamp of the last element polled from the channel.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_LAST_POLLED_TIMESTAMP =
            SimpleModelAttribute.longBuilder("LastPolledTimestamp", Subscriber.Channel.class)
                    .withDescription("The timestamp of the first entry in the channel polled by this subscriber.")
                    .withFunction(Subscriber.Channel::getLastPolledTimestamp)
                    .build();

    /**
     * The number of completed commit requests.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_COMMIT_COUNT =
            SimpleModelAttribute.longBuilder("CommittedCount", Subscriber.Channel.class)
                    .withDescription("The number of commit requests completed from this channel.")
                    .withFunction(Subscriber.Channel::getCommitCount)
                    .metric(true)
                    .build();

    /**
     * The last committed position in the channel.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_LAST_COMMIT =
            SimpleModelAttribute.stringBuilder("LastCommit", Subscriber.Channel.class)
                    .withDescription("The last position successfully committed by this subscriber since it was last assigned ownership of this channel.")
                    .withFunction(c -> String.valueOf(c.getLastCommit()))
                    .build();

    /**
     * An attribute indicating whether the subscriber owns this channel.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_OWNED =
            SimpleModelAttribute.booleanBuilder("Owned", Subscriber.Channel.class)
                    .withDescription("A flag indicating whether the channel is owned by this subscriber.")
                    .withFunction(Subscriber.Channel::isOwned)
                    .build();

    /**
     * An integer attribute indicating whether the subscriber owns this channel.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_OWNED_CODE =
            SimpleModelAttribute.intBuilder("OwnedCode", Subscriber.Channel.class)
                    .withDescription("An integer indicating whether the channel is owned by this subscriber (1 indicates true and 0 indicates false).")
                    .withFunction(Subscriber.Channel::getOwnedCode)
                    .metric("OwnedCode")
                    .build();

    /**
     * The number of completed received requests.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_RECEIVE_COMPLETIONS  =
                SimpleModelAttribute.longBuilder("ReceiveCompletionsCount", Subscriber.Channel.class)
                        .withDescription("The number completed receive requests.")
                        .withFunction(Subscriber.Channel::getReceived)
                        .metric("ReceiveCompletionsCount")
                        .build();

    /**
     * The number of completed received requests in one-minute.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_RECEIVE_COMPLETIONS_MEAN  =
            SimpleModelAttribute.doubleBuilder("ReceiveCompletionsMeanRate", Subscriber.Channel.class)
                    .withDescription("The completed receive requests, mean rate.")
                    .withFunction(Subscriber.Channel::getReceivedOneMinuteRate)
                    .metric("ReceiveCompletions")
                    .withMetricLabels("rate", RATE_MEAN)
                    .build();

    /**
     * The number of completed received requests in one-minute.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_RECEIVE_COMPLETIONS_ONE  =
            SimpleModelAttribute.doubleBuilder("ReceiveCompletionsOneMinuteRate", Subscriber.Channel.class)
                    .withDescription("The completed receive requests, one-minute rate.")
                    .withFunction(Subscriber.Channel::getReceivedOneMinuteRate)
                    .metric("ReceiveCompletions")
                    .withMetricLabels("rate", RATE_1MIN)
                    .build();

    /**
     * The number of completed received requests in one-minute.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_RECEIVE_COMPLETIONS_FIVE  =
                SimpleModelAttribute.doubleBuilder("ReceiveCompletionsFiveMinuteRate", Subscriber.Channel.class)
                        .withDescription("The completed receive requests, five-minute rate.")
                        .withFunction(Subscriber.Channel::getReceivedFiveMinuteRate)
                        .metric("ReceiveCompletions")
                        .withMetricLabels("rate", RATE_5MIN)
                        .build();

    /**
     * The number of completed received requests in one-minute.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_RECEIVE_COMPLETIONS_FIFTEEN  =
                SimpleModelAttribute.doubleBuilder("ReceiveCompletionsFifteenMinuteRate", Subscriber.Channel.class)
                        .withDescription("The completed receive requests, fifteen-minute rate.")
                        .withFunction(Subscriber.Channel::getReceivedFifteenMinuteRate)
                        .metric("ReceiveCompletions")
                        .withMetricLabels("rate", RATE_15MIN)
                        .build();
    }
