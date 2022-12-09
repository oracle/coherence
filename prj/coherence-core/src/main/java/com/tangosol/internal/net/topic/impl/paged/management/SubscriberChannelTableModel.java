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
import com.tangosol.net.topic.Subscriber;

/**
 * A table model representing topic subscriber channel statistics.
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public class SubscriberChannelTableModel
        extends TabularModel<Subscriber.Channel, SubscriberModel>
    {
    /**
     * Create a {@link SubscriberChannelTableModel}.
     */
    public SubscriberChannelTableModel()
        {
        super("Channels", "The subscriber's channel details",
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
                ATTRIBUTE_LAST_RECEIVED,
                ATTRIBUTE_LAST_POLLED,
                ATTRIBUTE_OWNED,
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
                    .withDescription("The number of channels in the topic")
                    .withFunction(Subscriber.Channel::getId)
                    .build();

    /**
     * The empty channel attribute.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_EMPTY =
            SimpleModelAttribute.booleanBuilder("Empty", Subscriber.Channel.class)
                    .withDescription("A flag indicating whether the channel is empty")
                    .withFunction(Subscriber.Channel::isEmpty)
                    .build();

    /**
     * The channel head position.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_HEAD =
            SimpleModelAttribute.stringBuilder("Head", Subscriber.Channel.class)
                    .withDescription("The position this subscriber knows as the head for the channel")
                    .withFunction(c -> String.valueOf(c.getHead()))
                    .build();

    /**
     * The position of the last element received from the channel.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_LAST_RECEIVED =
            SimpleModelAttribute.stringBuilder("LastReceived", Subscriber.Channel.class)
                    .withDescription("The last position received by this subscriber since it was last assigned ownership of this channel")
                    .withFunction(c -> String.valueOf(c.getLastReceived()))
                    .build();

    /**
     * The position of the last element polled from the channel.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_LAST_POLLED =
            SimpleModelAttribute.stringBuilder("LastPolled", Subscriber.Channel.class)
                    .withDescription("The last position polled by this subscriber since it was last assigned ownership of this channel")
                    .withFunction(c -> String.valueOf(c.getLastPolled()))
                    .build();

    /**
     * The last committed position in the channel.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_LAST_COMMIT =
            SimpleModelAttribute.stringBuilder("LastCommit", Subscriber.Channel.class)
                    .withDescription("The last position successfully committed by this subscriber since it was last assigned ownership of this channel")
                    .withFunction(c -> String.valueOf(c.getLastCommit()))
                    .build();

    /**
     * An attribute indicating whether the subscriber owns this channel.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_OWNED =
            SimpleModelAttribute.booleanBuilder("Owned", Subscriber.Channel.class)
                    .withDescription("A flag indicating whether the channel is owned by this subscriber")
                    .withFunction(Subscriber.Channel::isOwned)
                    .build();

    /**
     * The number of completed received requests.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_RECEIVE_COMPLETIONS  =
                SimpleModelAttribute.longBuilder("ReceiveCompletionsCount", Subscriber.Channel.class)
                        .withDescription("The number completed receive requests")
                        .withFunction(Subscriber.Channel::getReceived)
                        .metric("ReceiveCompletionsCount")
                        .build();

    /**
     * The number of completed received requests in one-minute.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_RECEIVE_COMPLETIONS_MEAN  =
            SimpleModelAttribute.doubleBuilder("ReceiveCompletionsMeanRate", Subscriber.Channel.class)
                    .withDescription("The completed receive requests, mean rate")
                    .withFunction(Subscriber.Channel::getReceivedOneMinuteRate)
                    .metric("ReceiveCompletions")
                    .withMetricLabels("rate", "mean")
                    .build();

    /**
     * The number of completed received requests in one-minute.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_RECEIVE_COMPLETIONS_ONE  =
            SimpleModelAttribute.doubleBuilder("ReceiveCompletionsOneMinuteRate", Subscriber.Channel.class)
                    .withDescription("The completed receive requests, one-minute rate")
                    .withFunction(Subscriber.Channel::getReceivedOneMinuteRate)
                    .metric("ReceiveCompletions")
                    .withMetricLabels("rate", "1-min")
                    .build();

    /**
     * The number of completed received requests in one-minute.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_RECEIVE_COMPLETIONS_FIVE  =
                SimpleModelAttribute.doubleBuilder("ReceiveCompletionsFiveMinuteRate", Subscriber.Channel.class)
                        .withDescription("The completed receive requests, five-minute rate")
                        .withFunction(Subscriber.Channel::getReceivedFiveMinuteRate)
                        .metric("ReceiveCompletions")
                        .withMetricLabels("rate", "5-min")
                        .build();

    /**
     * The number of completed received requests in one-minute.
     */
    protected static final ModelAttribute<Subscriber.Channel> ATTRIBUTE_RECEIVE_COMPLETIONS_FIFTEEN  =
                SimpleModelAttribute.doubleBuilder("ReceiveCompletionsFifteenMinuteRate", Subscriber.Channel.class)
                        .withDescription("The completed receive requests, fifteen-minute rate")
                        .withFunction(Subscriber.Channel::getReceivedFifteenMinuteRate)
                        .metric("ReceiveCompletions")
                        .withMetricLabels("rate", "15-min")
                        .build();
    }
