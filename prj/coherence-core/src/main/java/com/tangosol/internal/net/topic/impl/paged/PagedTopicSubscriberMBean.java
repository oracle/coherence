/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;

import com.tangosol.io.Serializer;
import com.tangosol.net.Cluster;
import com.tangosol.net.management.Registry;
import com.tangosol.net.topic.ManagedSubscriber;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.Filter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

/**
 * An MBean for a {@link PagedTopicSubscriber}.
 *
 * @author Jonathan Knight 2022.03.22
 * @since 21.12.4
 */
public class PagedTopicSubscriberMBean
        extends ManagedSubscriber
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a subscriber MBean.
     *
     * @param subscriber  the {@link PagedTopicSubscriber} this MBean represents.
     */
    public PagedTopicSubscriberMBean(PagedTopicSubscriber<?> subscriber)
        {
        f_subscriber = subscriber;
        }

    // ----- PagedTopicSubscriberMBean methods ------------------------------

    /**
     * Register this MBean and return the name it was registered with.
     *
     * @param registry  the {@link Registry} to register this MBean in
     *
     * @return  the name ths MBean was registered with
     */
    public String register(Registry registry)
        {
        String sGroup;
        long   nId;
        if (f_subscriber.isAnonymous())
            {
            sGroup = ANONYMOUS_GROUP;
            nId    = f_subscriber.getNotificationId();
            }
        else
            {
            sGroup = f_subscriber.getSubscriberGroupId().getGroupName();
            nId    = f_subscriber.getId();
            }

        String sName = String.format(MBEAN_NAME_PATTERN, f_subscriber.getNamedTopic().getName(), sGroup, nId);
        registry.register(sName, this);
        return sName;
        }

    // ----- ManagedSubscriber methods --------------------------------------

    @Override
    protected long getBacklog()
        {
        return f_subscriber.getBacklog();
        }

    @Override
    protected long getMaxBacklog()
        {
        return f_subscriber.getMaxBacklog();
        }

    @Override
    protected boolean isCompleteOnEmpty()
        {
        return f_subscriber.isCompleteOnEmpty();
        }

    @Override
    protected Filter<?> getFilter()
        {
        return f_subscriber.getFilter();
        }

    @Override
    protected Function<?, ?> getConverter()
        {
        return f_subscriber.getConverter();
        }

    @Override
    protected Serializer getSerializer()
        {
        return f_subscriber.getSerializer();
        }

    @Override
    protected int getChannelCount()
        {
        return f_subscriber.getChannelCount();
        }

    @Override
    protected String getChannels()
        {
        int[] aChannel = f_subscriber.getChannels();
        return aChannel == null ? "[]" : Arrays.toString(aChannel);
        }

    @Override
    protected long getDisconnectCount()
        {
        return f_subscriber.getDisconnectCount();
        }

    @Override
    protected long getElementsPolled()
        {
        return f_subscriber.getElementsPolled();
        }

    @Override
    protected long getErrorCount()
        {
        return f_subscriber.getReceivedError();
        }

    @Override
    protected long getId()
        {
        return f_subscriber.getId();
        }

    @Override
    protected String getMember()
        {
        NamedTopic<?> topic = f_subscriber.getNamedTopic();
        Cluster cluster = topic.getService().getCluster();
        return String.valueOf(cluster.getLocalMember());
        }

    @Override
    protected long getNotifications()
        {
        return f_subscriber.getNotify();
        }

    @Override
    protected long getPolls()
        {
        return f_subscriber.getPolls();
        }

    @Override
    protected long getReceivedCount()
        {
        return f_subscriber.getReceived();
        }

    @Override
    protected long getReceivedEmptyCount()
        {
        return f_subscriber.getReceivedEmpty();
        }

    @Override
    protected int getState()
        {
        return f_subscriber.getState();
        }

    @Override
    protected String getStateName()
        {
        return f_subscriber.getStateName();
        }

    @Override
    protected String getSubscriberGroup()
        {
        SubscriberGroupId groupId = f_subscriber.getSubscriberGroupId();
        return groupId != null ? groupId.getGroupName() : null;
        }

    @Override
    protected String getSubscriberType()
        {
        return f_subscriber.getClass().getSimpleName();
        }

    @Override
    protected long getWaits()
        {
        return f_subscriber.getWaitCount();
        }

    @Override
    protected void disconnect()
        {
        f_subscriber.disconnect();
        }

    @Override
    protected void connect()
        {
        f_subscriber.ensureActive();
        f_subscriber.ensureConnected();
        }

    @Override
    protected Map<Integer, Position> getHeads()
        {
        return f_subscriber.getHeads();
        }

    @Override
    protected Subscriber.Channel getChannel(int nChannel)
        {
        return f_subscriber.getChannel(nChannel);
        }

    @Override
    protected void notifyChannel(int nChannel)
        {
        f_subscriber.onChannelPopulatedNotification(new int[]{nChannel});
        }

    @Override
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

    // ----- data members ---------------------------------------------------

    /**
     * The {@link PagedTopicSubscriber} this MBean represents.
     */
    private final PagedTopicSubscriber<?> f_subscriber;
    }
