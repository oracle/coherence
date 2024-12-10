/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.management;

import com.tangosol.internal.net.management.model.AbstractModel;
import com.tangosol.internal.net.management.model.ModelAttribute;
import com.tangosol.internal.net.management.model.ModelOperation;
import com.tangosol.internal.net.management.model.SimpleModelAttribute;

import com.tangosol.internal.net.management.model.SimpleModelOperation;
import com.tangosol.internal.net.topic.impl.paged.PagedTopic;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.statistics.PagedTopicStatistics;
import com.tangosol.internal.net.topic.impl.paged.statistics.SubscriberGroupStatistics;

import javax.management.DynamicMBean;

import com.tangosol.net.PagedTopicService;

import com.tangosol.util.Filter;
import com.tangosol.util.LongArray;
import com.tangosol.util.SimpleLongArray;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.function.Function;

/**
 * An MBean model for a {@link PagedTopic}
 *
 * @author Jonathan Knight 2022.09.10
 * @since 22.06.4
 */
public class SubscriberGroupModel
        extends AbstractModel<SubscriberGroupModel>
        implements DynamicMBean, PolledMetrics
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link SubscriberGroupModel}.
     *
     * @param pagedTopicStatistics the topic this model represents
     * @param groupId              the {@link SubscriberGroupId id} of the subscriber group
     * @param filter               the filter used to filter messages to be received by subscribers in the group
     * @param fnConvert            the Function used to convert messages to be received by subscribers in the group
     * @param service              the topic service
     */
    public SubscriberGroupModel(PagedTopicStatistics pagedTopicStatistics, SubscriberGroupId groupId, Filter<?> filter,
            Function<?, ?> fnConvert, PagedTopicService service)
        {
        super(MBEAN_DESCRIPTION);
        f_groupId    = groupId;
        f_filter     = filter;
        f_fnConvert  = fnConvert;
        f_statistics = pagedTopicStatistics;
        f_service    = service;

        // create the array of channel models
        int cChannel = service.getChannelCount(pagedTopicStatistics.getTopicName());
        for (int nChannel = 0; nChannel < cChannel; nChannel++)
            {
            f_aChannel.set(nChannel, new SubscriberGroupChannelModel(f_statistics, groupId, nChannel));
            }

        // configure the attributes of the MBean
        addAttribute(ATTRIBUTE_CHANNEL_COUNT);
        addAttribute(ATTRIBUTE_POLLED_COUNT);
        addAttribute(ATTRIBUTE_POLLED_MEAN);
        addAttribute(ATTRIBUTE_POLLED_ONE_MINUTE);
        addAttribute(ATTRIBUTE_POLLED_FIVE_MINUTE);
        addAttribute(ATTRIBUTE_POLLED_FIFTEEN_MINUTE);
        addAttribute(ATTRIBUTE_CHANNEL_TABLE);
        addAttribute(ATTRIBUTE_FILTER);
        addAttribute(ATTRIBUTE_TRANSFORMER);

        // configure the operations of the MBean
        addOperation(OPERATION_DISCONNECT_ALL);
        }

    // ----- PagedTopicModel methods ----------------------------------------

    /**
     * Returns the channel count.
     *
     * @return the channel count
     */
    protected int getChannelCount()
        {
        return f_service.getChannelCount(f_statistics.getTopicName());
        }

    /**
     * Returns the filter for the subscriber group.
     *
     * @return  the filter
     */
    protected String getFilter()
        {
        return valueOrNotApplicable(f_filter);
        }

    /**
     * Returns the transformer for the subscriber group.
     *
     * @return  the transformer
     */
    protected String getTransformer()
        {
        return valueOrNotApplicable(f_fnConvert);
        }

    /**
     * Return the {@link SubscriberGroupChannelModel} for a specific channel.
     * <p>
     * The channel parameter is a zero based index of channels and must be
     * greater than or equal to 0 and less than the channel count.
     *
     * @param nChannel  the channel to obtain the model for
     *
     * @return the {@link SubscriberGroupChannelModel} for the channel
     *
     * @throws IndexOutOfBoundsException if the channel parameter is less than zero
     *         or greater than or equal to the channel count
     */
    protected SubscriberGroupChannelModel getChannelModel(int nChannel)
        {
        SubscriberGroupChannelModel model = f_aChannel.get(nChannel);
        if (model == null)
            {
            f_lock.lock();
            try
                {
                model = f_aChannel.get(nChannel);
                if (model == null)
                    {
                    model = new SubscriberGroupChannelModel(f_statistics, f_groupId, nChannel);
                    f_aChannel.set(nChannel, model);
                    }
                }
            finally
                {
                f_lock.unlock();
                }
            }
        return model;
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
     * Force the subscriber group to disconnect all subscribers.
     */
    protected void disconnectAll(Object[] aoParam)
        {
        new PagedTopicCaches(f_statistics.getTopicName(), f_service, false)
                .disconnectAllSubscribers(f_groupId);
        }

    // ----- helper methods -------------------------------------------------

    protected SubscriberGroupStatistics getStatistics()
        {
        return f_statistics.getSubscriberGroupStatistics(f_groupId);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The MBean's description.
     */
    protected static final String MBEAN_DESCRIPTION = "A Coherence PagedTopic";

    /**
     * The channel count attribute.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_CHANNEL_COUNT =
            SimpleModelAttribute.intBuilder("ChannelCount", SubscriberGroupModel.class)
                    .withDescription("The number of channels in the topic")
                    .withFunction(SubscriberGroupModel::getChannelCount)
                    .metric(false)
                    .build();

    /**
     * The polled count attribute.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_POLLED_COUNT =
            PolledMetrics.ATTRIBUTE_COUNT.asBuilder(SubscriberGroupModel.class)
                    .withFunction(SubscriberGroupModel::getPolledCount)
                    .build();

    /**
     * The polled mean rate attribute.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_POLLED_MEAN =
            PolledMetrics.ATTRIBUTE_MEAN_RATE.asBuilder(SubscriberGroupModel.class)
                    .withFunction(SubscriberGroupModel::getPolledMeanRate)
                    .build();

    /**
     * The polled one-minute rate attribute.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_POLLED_ONE_MINUTE =
            PolledMetrics.ATTRIBUTE_ONE_MINUTE_RATE.asBuilder(SubscriberGroupModel.class)
                    .withFunction(SubscriberGroupModel::getPolledOneMinuteRate)
                    .build();

    /**
     * The polled five-minute rate attribute.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_POLLED_FIVE_MINUTE =
            PolledMetrics.ATTRIBUTE_FIVE_MINUTE_RATE.asBuilder(SubscriberGroupModel.class)
                    .withFunction(SubscriberGroupModel::getPolledFiveMinuteRate)
                    .build();

    /**
     * The polled fifteen-minute rate attribute.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_POLLED_FIFTEEN_MINUTE =
            PolledMetrics.ATTRIBUTE_FIFTEEN_MINUTE_RATE.asBuilder(SubscriberGroupModel.class)
                    .withFunction(SubscriberGroupModel::getPolledFifteenMinuteRate)
                    .build();

    /**
     * The channel attributes table.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_CHANNEL_TABLE
            = new SubscriberGroupChannelTableModel();

    /**
     * The filter attribute.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_FILTER =
            SimpleModelAttribute.stringBuilder("Filter", SubscriberGroupModel.class)
                    .withDescription("The filter")
                    .withFunction(SubscriberGroupModel::getFilter)
                    .build();

    /**
     * The transformer attribute.
     */
    protected static final ModelAttribute<SubscriberGroupModel> ATTRIBUTE_TRANSFORMER =
            SimpleModelAttribute.stringBuilder("Transformer", SubscriberGroupModel.class)
                    .withDescription("The transformer")
                    .withFunction(SubscriberGroupModel::getTransformer)
                    .build();

    /**
     * The subscriber group disconnect all operation.
     */
    protected static final ModelOperation<SubscriberGroupModel> OPERATION_DISCONNECT_ALL =
            SimpleModelOperation.builder("disconnectAll", SubscriberGroupModel.class)
                    .withDescription("Force this subscriber group to disconnect all subscribers.")
                    .withFunction(SubscriberGroupModel::disconnectAll)
                    .build();

    // ----- data members ---------------------------------------------------

    /**
     * The topic statistics.
     */
    private final PagedTopicStatistics f_statistics;

    /**
     * The topic service.
     */
    private final PagedTopicService f_service;

    /**
     * The id of the subscriber group.
     */
    private final SubscriberGroupId f_groupId;

    /**
     * The filter used to filter messages to be received by subscribers in the group.
     */
    private final Filter<?> f_filter;

    /**
     * the Function used to convert messages to be received by subscribers in the group.
     */
    private final Function<?, ?> f_fnConvert;

    /**
     * The channel models.
     */
    @SuppressWarnings("unchecked")
    private final LongArray<SubscriberGroupChannelModel> f_aChannel = new SimpleLongArray();

    /**
     * The lock to use to synchronize access to internal state.
     */
    private final Lock f_lock = new ReentrantLock(true);
    }
