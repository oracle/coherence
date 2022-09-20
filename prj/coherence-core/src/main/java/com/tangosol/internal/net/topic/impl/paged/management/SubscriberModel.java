/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.management;

import com.oracle.coherence.common.base.Exceptions;

import com.tangosol.internal.net.management.model.AbstractModel;
import com.tangosol.internal.net.management.model.ModelAttribute;
import com.tangosol.internal.net.management.model.ModelOperation;
import com.tangosol.internal.net.management.model.SimpleModelAttribute;
import com.tangosol.internal.net.management.model.SimpleModelOperation;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;

import com.tangosol.io.Serializer;

import com.tangosol.net.Cluster;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Filter;

import javax.management.DynamicMBean;

import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;

import java.util.Arrays;
import java.util.Map;

import java.util.function.Function;

/**
 * A topic subscriber MBean model.
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public class SubscriberModel
        extends AbstractModel<SubscriberModel>
        implements DynamicMBean
    {
    /**
     * Create a {@link SubscriberModel}.
     *
     * @param subscriber  the {@link PagedTopicSubscriber} the model represents
     */
    public SubscriberModel(PagedTopicSubscriber<?> subscriber)
        {
        super(MBEAN_DESCRIPTION);
        f_subscriber = subscriber;

        // configure the attributes of the MBean (ordering does not matter)
        addAttribute(ATTRIBUTE_BACKLOG);
        addAttribute(ATTRIBUTE_CHANNEL_ALLOCATIONS);
        addAttribute(ATTRIBUTE_CHANNEL_COUNT);
        addAttribute(ATTRIBUTE_CHANNELS);
        addAttribute(ATTRIBUTE_COMPLETE_ON_EMPTY);
        addAttribute(ATTRIBUTE_CONVERTER);
        addAttribute(ATTRIBUTE_DISCONNECTIONS);
        addAttribute(ATTRIBUTE_ELEMENTS);
        addAttribute(ATTRIBUTE_FILTER);
        addAttribute(ATTRIBUTE_ID);
        addAttribute(ATTRIBUTE_MAX_BACKLOG);
        addAttribute(ATTRIBUTE_MEMBER);
        addAttribute(ATTRIBUTE_NOTIFICATIONS);
        addAttribute(ATTRIBUTE_POLLS);
        addAttribute(ATTRIBUTE_RECEIVE_COMPLETIONS);
        addAttribute(ATTRIBUTE_RECEIVE_EMPTY);
        addAttribute(ATTRIBUTE_RECEIVE_ERRORS);
        addAttribute(ATTRIBUTE_SERIALIZER);
        addAttribute(ATTRIBUTE_STATE);
        addAttribute(ATTRIBUTE_STATE_NAME);
        addAttribute(ATTRIBUTE_SUBSCRIBER_GROUP);
        addAttribute(ATTRIBUTE_TYPE);
        addAttribute(ATTRIBUTE_WAITS);

        // configure the operations of the MBean
        addOperation(OPERATION_CONNECT);
        addOperation(OPERATION_DISCONNECT);
        addOperation(OPERATION_HEADS);
        addOperation(OPERATION_NOTIFY);
        addOperation(OPERATION_REMAINING);
        }

    // ----- SubscriberModel methods ----------------------------------------

    /**
     * Returns the subscriber type, typically the simple class name.
     *
     * @return the subscriber type, typically the simple class name
     */
    protected String getSubscriberType()
        {
        return f_subscriber.getClass().getSimpleName();
        }

    /**
     * Return the subscriber identifier.
     *
     * @return the subscriber identifier
     */
    protected long getId()
        {
        return f_subscriber.getId();
        }

    /**
     * Return the member the subscriber is running on.
     *
     * @return the member the subscriber is running on
     */
    protected String getMember()
        {
        NamedTopic<?> topic = f_subscriber.getNamedTopic();
        Cluster cluster = topic.getService().getCluster();
        return String.valueOf(cluster.getLocalMember());
        }

    /**
     * Return the group the subscriber belongs to, if part of a group.
     *
     * @return the group the subscriber belongs to, if part of a group
     */
    protected String getSubscriberGroup()
        {
        SubscriberGroupId groupId = f_subscriber.getSubscriberGroupId();
        return groupId != null ? groupId.getGroupName() : null;
        }

    /**
     * Return the number of channels the topic has.
     *
     * @return the number of channels the topic has
     */
    protected int getChannelCount()
        {
        return f_subscriber.getChannelCount();
        }

    /**
     * Return the list of channels owned by this subscriber as a String.
     *
     * @return the list of channels owned by this subscriber as a String
     */
    protected String getChannels()
        {
        int[] aChannel = f_subscriber.getChannels();
        return aChannel == null ? "[]" : Arrays.toString(aChannel);
        }

    /**
     * Return the number of times the subscriber has polled for messages.
     *
     * @return the number of times the subscriber has polled for messages
     */
    protected long getPolls()
        {
        return f_subscriber.getPolls();
        }

    /**
     * Return the number of message elements received.
     *
     * @return the number of message elements received
     */
    protected long getElementsPolled()
        {
        return f_subscriber.getElementsPolled();
        }

    /**
     * Return the number of times the subscriber has had to wait on an empty topic.
     *
     * @return the number of times the subscriber has had to wait on an empty topic
     */
    protected long getWaits()
        {
        return f_subscriber.getWaitCount();
        }

    /**
     * Return the number of channel populated notifications received.
     *
     * @return the number of channel populated notifications received
     */
    protected long getNotifications()
        {
        return f_subscriber.getNotify();
        }

    /**
     * Return the subscriber's state.
     *
     * @return the subscriber's state
     */
    protected int getState()
        {
        return f_subscriber.getState();
        }

    /**
     * Return the subscriber's state, as a String.
     *
     * @return the subscriber's state, as a String
     */
    protected String getStateName()
        {
        return f_subscriber.getStateName();
        }

    /**
     * Return the count of receive requests not yet complete.
     *
     * @return the count of receive requests not yet complete
     */
    protected long getBacklog()
        {
        return f_subscriber.getBacklog();
        }

    /**
     * Return the maximum allowed backlog of receive requests not yet complete.
     *
     * @return the maximum allowed backlog of receive requests not yet complete
     */
    protected long getMaxBacklog()
        {
        return f_subscriber.getMaxBacklog();
        }

    /**
     * Return {@code true} of the subscriber completes receive futures if empty.
     *
     * @return {@code true} of the subscriber completes receive futures if empty
     */
    protected boolean isCompleteOnEmpty()
        {
        return f_subscriber.isCompleteOnEmpty();
        }

    /**
     * Return the optional subscriber {@link Filter}.
     *
     * @return the optional subscriber {@link Filter}
     */
    protected String getFilter()
        {
        Filter<?> filter = f_subscriber.getFilter();
        return filter == null ? "n/a" : String.valueOf(filter);
        }

    /**
     * Return the optional subscriber converter.
     *
     * @return the optional subscriber converter
     */
    protected String getConverter()
        {
        Function<?, ?> function = f_subscriber.getConverter();
        return function == null ? "n/a" : String.valueOf(function);
        }

    /**
     * Return the subscriber {@link Serializer}.
     *
     * @return the subscriber {@link Serializer}
     */
    protected String getSerializer()
        {
        return String.valueOf(f_subscriber.getSerializer());
        }

    /**
     * Return the count of receive requests completed.
     *
     * @return the count of receive requests completed
     */
    protected long getReceivedCount()
        {
        return f_subscriber.getReceived();
        }

    /**
     * Return the count of receive requests completed with a {@code null} message.
     *
     * @return the count of receive requests completed with a {@code null} message
     */
    protected long getReceivedEmptyCount()
        {
        return f_subscriber.getReceivedEmpty();
        }

    /**
     * Return the count of receive requests completed with an error.
     *
     * @return the count of receive requests completed with an error
     */
    protected long getErrorCount()
        {
        return f_subscriber.getReceivedError();
        }

    /**
     * Return the number of times the subscriber has disconnected.
     *
     * @return the number of times the subscriber has disconnected
     */
    protected long getDisconnectCount()
        {
        return f_subscriber.getDisconnectCount();
        }

    /**
     * Force the subscriber to disconnect.
     */
    protected void disconnect(Object[] aoParam)
        {
        f_subscriber.disconnect();
        }

    /**
     * Ensure the subscriber is connected.
     */
    protected void connect(Object[] aoParam)
        {
        f_subscriber.connect();
        }

    /**
     * Return the current topic heads as seen by this subscriber.
     *
     * @return the current topic heads as seen by this subscriber
     */
    protected Map<Integer, Position> getHeads(Object[] ignored)
        {
        return f_subscriber.getHeads();
        }

    /**
     * Return the specified channel.
     *
     * @param nChannel the channel to obtain
     * @return the specified channel
     */
    protected Subscriber.Channel getChannel(int nChannel)
        {
        return f_subscriber.getChannel(nChannel);
        }

    /**
     * Notify the subscriber that the specified channel has been populated.
     *
     * @param nChannel the channel identifier
     */
    protected void notifyChannel(int nChannel)
        {
        f_subscriber.notifyChannel(nChannel);
        }

    /**
     * Return the remaining messages in the specified channel, or if the channel is
     * less than zero, return the count of all remaining messages for channels owned
     * by this subscriber.
     *
     * @param nChannel the channel to count remaining messages in, or -1 for all owned channels
     *
     * @return the remaining messages in the specified channel, or if the channel is
     *         less than zero, return the count of all remaining messages for channels owned
     *         by this subscriber
     */
    protected int getRemainingMessages(int nChannel)
        {
        if (nChannel < 0)
            {
            return f_subscriber.getRemainingMessages();
            }
        else
            {
            return f_subscriber.getRemainingMessages(nChannel);
            }
        }

    /**
     * Returns a {@link TabularData table} of the subscribers heads.
     *
     * @return a {@link TabularData table} of the subscribers heads
     */
    protected TabularData getHeadsTable(Object[] ignored)
        {
        try
            {
            Map<Integer, Position> mapHead = f_subscriber.getHeads();
            return CHANNEL_HEADS_TABLE.getTabularData(mapHead);
            }
        catch (Throwable e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Returns a {@link TabularData table} of the remaining messages by channel.
     *
     * @return a {@link TabularData table} of the remaining messages by channel
     */
    protected TabularData getRemainingMessagesTable(Object[] ignored)
        {
        try
            {
            return REMAINING_MESSAGES_TABLE.getTabularData(this);
            }
        catch (Throwable e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    protected void invokeNotifyChannel(Object[] aoParam)
        {
        if (aoParam == null || aoParam.length != 1 || !(aoParam[0] instanceof Integer))
            {
            throw new IllegalArgumentException("An integer channel identifier must be supplied");
            }

        int nChannel = (Integer) aoParam[0];
        if (nChannel < 0 || nChannel >= getChannelCount())
            {
            throw new IllegalArgumentException("An integer channel identifier must be supplied in the range 0.." + getChannelCount());
            }

        notifyChannel(nChannel);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The MBean's description.
     */
    protected static final String MBEAN_DESCRIPTION = "A Coherence PagedTopic Subscriber";
                                                 
    /**
     * The channel count attribute.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_CHANNEL_COUNT =
            SimpleModelAttribute.intBuilder("ChannelCount", SubscriberModel.class)
                    .withDescription("The number of channels in the topic")
                    .withFunction(SubscriberModel::getChannelCount)
                    .build();

    /**
     * The subscriber id attribute.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_ID =
            SimpleModelAttribute.longBuilder("Id", SubscriberModel.class)
                    .withDescription("The subscriber's identifier")
                    .withFunction(SubscriberModel::getId)
                    .build();

    /**
     * The subscriber type attribute.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_TYPE =
                SimpleModelAttribute.stringBuilder("Type", SubscriberModel.class)
                        .withDescription("The type of this subscriber")
                        .withFunction(SubscriberModel::getSubscriberType)
                        .build();

    /**
     * The channel allocation attribute.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_CHANNEL_ALLOCATIONS =
                    SimpleModelAttribute.stringBuilder("ChannelAllocations", SubscriberModel.class)
                            .withDescription("The subscriber's allocated channels")
                            .withFunction(SubscriberModel::getChannels)
                            .build();

    /**
     * The subscriber group attribute.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_SUBSCRIBER_GROUP =
                    SimpleModelAttribute.stringBuilder("SubscriberGroup", SubscriberModel.class)
                            .withDescription("The subscriber group the subscriber belongs to")
                            .withFunction(SubscriberModel::getSubscriberGroup)
                            .build();

    /**
     * The number of polls attribute.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_POLLS  =
                SimpleModelAttribute.longBuilder("Polls", SubscriberModel.class)
                        .withDescription("The total number of polls for messages")
                        .withFunction(SubscriberModel::getPolls)
                        .build();
    /**
     * The number of received elements attribute.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_ELEMENTS =
                SimpleModelAttribute.longBuilder("ReceivedCount", SubscriberModel.class)
                        .withDescription("The number of elements received")
                        .withFunction(SubscriberModel::getElementsPolled)
                        .build();

    /**
     * The number of completed received requests.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_RECEIVE_COMPLETIONS  =
                SimpleModelAttribute.longBuilder("ReceiveCompletions", SubscriberModel.class)
                        .withDescription("The number completed receive requests")
                        .withFunction(SubscriberModel::getReceivedCount)
                        .build();

    /**
     * The number of received requests completed in error.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_RECEIVE_ERRORS  =
                SimpleModelAttribute.longBuilder("ReceiveErrors", SubscriberModel.class)
                        .withDescription("The number exceptionally completed receive requests")
                        .withFunction(SubscriberModel::getErrorCount)
                        .build();

    /**
     * The number of empty received requests.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_RECEIVE_EMPTY  =
                SimpleModelAttribute.longBuilder("ReceiveEmpty", SubscriberModel.class)
                        .withDescription("The number empty receive requests")
                        .withFunction(SubscriberModel::getReceivedEmptyCount)
                        .build();

    /**
     * The number of times the subscriber has waited on empty channels.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_WAITS  =
                SimpleModelAttribute.longBuilder("Waits", SubscriberModel.class)
                        .withDescription("The number of waits on an empty channel")
                        .withFunction(SubscriberModel::getWaits)
                        .build();

    /**
     * The number of times the subscriber has been notified on non-empty channels.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_NOTIFICATIONS  =
                SimpleModelAttribute.longBuilder("Notifications", SubscriberModel.class)
                        .withDescription("The number of channel notifications received")
                        .withFunction(SubscriberModel::getNotifications)
                        .build();

    /**
     * The subscriber state attribute.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_STATE =
                SimpleModelAttribute.longBuilder("State", SubscriberModel.class)
                        .withDescription("The state of the subscriber")
                        .withFunction(SubscriberModel::getState)
                        .build();

    /**
     * The subscriber state name attribute.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_STATE_NAME =
                    SimpleModelAttribute.stringBuilder("StateName", SubscriberModel.class)
                        .withDescription("The state of the subscriber as a string")
                        .withFunction(SubscriberModel::getStateName)
                        .build();

    /**
     * The backlog of receive requests attribute.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_BACKLOG  =
                SimpleModelAttribute.longBuilder("Backlog", SubscriberModel.class)
                        .withDescription("The number of outstanding receive requests")
                        .withFunction(SubscriberModel::getBacklog)
                        .build();

    /**
     * The maximum allowed backlog of receive requests attribute.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_MAX_BACKLOG =
            SimpleModelAttribute.longBuilder("MaxBacklog", SubscriberModel.class)
                        .withDescription("The maximum number of outstanding receive requests allowed before flow control blocks receive calls")
                        .withFunction(SubscriberModel::getMaxBacklog)
                        .build();

    /**
     * The number of times the subscriber has been disconnected.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_DISCONNECTIONS  =
                SimpleModelAttribute.longBuilder("Disconnections", SubscriberModel.class)
                        .withDescription("The number of times this subscriber has disconnected")
                        .withFunction(SubscriberModel::getDisconnectCount)
                        .build();

    /**
     * The filter the subscriber is using.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_FILTER =
                    SimpleModelAttribute.stringBuilder("Filter", SubscriberModel.class)
                            .withDescription("The optional filter being used to filter messages")
                            .withFunction(SubscriberModel::getFilter)
                            .build();

    /**
     * The converter the subscriber is using.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_CONVERTER =
                    SimpleModelAttribute.stringBuilder("Converter", SubscriberModel.class)
                            .withDescription("The optional converter being used to transform messages")
                            .withFunction(SubscriberModel::getConverter)
                            .build();

    /**
     * The serializer the subscriber is using.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_SERIALIZER =
                    SimpleModelAttribute.stringBuilder("Serializer", SubscriberModel.class)
                            .withDescription("The serializer used to deserialize messages")
                            .withFunction(SubscriberModel::getSerializer)
                            .build();

    /**
     * The complete-on-empty flag.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_COMPLETE_ON_EMPTY =
                    SimpleModelAttribute.booleanBuilder("CompleteOnEmpty", SubscriberModel.class)
                            .withDescription("A flag indicating whether the subscriber completes receive requests with a null message when the topic is empty")
                            .withFunction(SubscriberModel::isCompleteOnEmpty)
                            .build();

    /**
     * The member owning the subscriber.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_MEMBER =
                    SimpleModelAttribute.stringBuilder("Member", SubscriberModel.class)
                            .withDescription("The cluster member owning this subscriber")
                            .withFunction(SubscriberModel::getMember)
                            .build();

    /**
     * The subscriber channel information table.
     */
    protected static final ModelAttribute<SubscriberModel> ATTRIBUTE_CHANNELS = new SubscriberChannelTableModel();

    /**
     * The channel heads table.
     * <p>
     * This model is used to return the tabular data for the {@link #OPERATION_HEADS} operation
     */
    protected static final ChannelPositionTableModel CHANNEL_HEADS_TABLE = new ChannelPositionTableModel();

    /**
     * The remaining messages by channel table model.
     * <p>
     * This model is used to return the tabular data for the {@link #OPERATION_REMAINING} operation
     */
    protected static final ChannelCountsTableModel<SubscriberModel> REMAINING_MESSAGES_TABLE =
            new ChannelCountsTableModel<>(SubscriberModel::getChannelCount, SubscriberModel::getRemainingMessages);

    /**
     * The subscriber disconnect operation.
     */
    protected static final ModelOperation<SubscriberModel> OPERATION_DISCONNECT =
            SimpleModelOperation.builder("Disconnect", SubscriberModel.class)
                    .withDescription("Force this subscriber to disconnect and reset itself")
                    .withFunction(SubscriberModel::disconnect)
                    .build();

    /**
     * The subscriber dconnect operation.
     */
    protected static final ModelOperation<SubscriberModel> OPERATION_CONNECT =
            SimpleModelOperation.builder("Connect", SubscriberModel.class)
                    .withDescription("Ensure this subscriber to connected")
                    .withFunction(SubscriberModel::connect)
                    .build();

    /**
     * The get channel heads operation.
     */
    protected static final ModelOperation<SubscriberModel> OPERATION_HEADS =
            SimpleModelOperation.builder("Heads", SubscriberModel.class)
                    .withDescription("Retrieve the current head positions for each channel")
                    .withFunction(SubscriberModel::getHeadsTable)
                    .returning(CHANNEL_HEADS_TABLE.getType())
                    .build();

    /**
     * The get remaining messages operation.
     */
    protected static final ModelOperation<SubscriberModel> OPERATION_REMAINING =
            SimpleModelOperation.builder("RemainingMessages", SubscriberModel.class)
                    .withDescription("Retrieve the count of remaining messages for each channel")
                    .withFunction(SubscriberModel::getRemainingMessagesTable)
                    .returning(REMAINING_MESSAGES_TABLE.getType())
                    .build();


    /**
     * The force notification operation.
     */
    protected static final ModelOperation<SubscriberModel> OPERATION_NOTIFY =
            SimpleModelOperation.builder("NotifyPopulated", SubscriberModel.class)
                    .withDescription("Send a channel populated notification to this subscriber")
                    .withParameter("Channel", "The channel identifier", SimpleType.INTEGER)
                    .withFunction(SubscriberModel::invokeNotifyChannel)
                    .build();

    // ----- data members ---------------------------------------------------

    /**
     * The {@link PagedTopicSubscriber} this MBean represents.
     */
    private final PagedTopicSubscriber<?> f_subscriber;
    }