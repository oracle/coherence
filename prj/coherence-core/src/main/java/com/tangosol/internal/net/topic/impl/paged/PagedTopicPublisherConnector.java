/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.util.Options;

import com.tangosol.internal.net.topic.NamedTopicPublisher;
import com.tangosol.internal.net.topic.NamedTopicPublisher.PublisherEvent;
import com.tangosol.internal.net.topic.NamedTopicDeactivationListener;
import com.tangosol.internal.net.topic.PublisherChannelConnector;
import com.tangosol.internal.net.topic.PublisherConnector;

import com.tangosol.internal.net.topic.impl.paged.model.NotificationKey;

import com.tangosol.net.Cluster;
import com.tangosol.net.TopicService;

import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.TopicDependencies;

import com.tangosol.util.Filter;
import com.tangosol.util.Listeners;
import com.tangosol.util.MapListener;

import com.tangosol.util.filter.InKeySetFilter;

import com.tangosol.util.listener.SimpleMapListener;

/**
 * A {@link PublisherConnector} to connect a channel specific publisher
 * to an underlying paged topic.
 *
 * @param <V>  the type of element published to the topic
 *
 * @author Jonathan Knight  2024.11.26
 */
@SuppressWarnings("rawtypes")
public class PagedTopicPublisherConnector<V>
        implements PublisherConnector<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PagedTopicPublisherConnector}.
     *
     * @param caches    the {@link PagedTopicCaches} for the underlying paged topic
     * @param cChannel  the number of channels this publisher publishes to
     * @param opts      the options to use to configure the publisher
     */
    public PagedTopicPublisherConnector(PagedTopicCaches caches, int cChannel, Publisher.Option<? super V>[] opts)
        {
        Publisher.OptionSet<V> options = Publisher.optionsFrom(opts);

        f_nId                        = options.getId().orElse(createId(caches.getService()));
        f_nNotifyPostFull            = options.getNotifyPostFull(this);
        f_cChannel                   = options.getChannelCount(cChannel);
        m_caches                     = caches;
        f_sTopicName                 = caches.getTopicName();
        f_service                    = caches.getService();
        f_dependencies               = caches.getDependencies();
        f_filterListenerNotification = new InKeySetFilter<>(/*filter*/ null, m_caches.getPartitionNotifierSet(f_nNotifyPostFull));

        m_caches.addListener(new CachesListener());

        f_listenerNotification = new SimpleMapListener<NotificationKey, int[]>()
                            .addDeleteHandler(evt -> onNotification(evt.getOldValue()))
                            .synchronous();

        if (f_nNotifyPostFull != 0)
            {
            m_caches.Notifications.addMapListener(f_listenerNotification, f_filterListenerNotification, /*fLite*/ false);
            }
        }

    @Override
    public boolean isActive()
        {
        return m_caches.isActive();
        }

    @Override
    public boolean isDestroyed()
        {
        return m_caches.isDestroyed();
        }

    @Override
    public boolean isReleased()
        {
        return m_caches.isReleased();
        }

    @Override
    public long getId()
        {
        return f_nId;
        }

    @Override
    public void close()
        {
        fClosed = true;
        if (f_nNotifyPostFull != 0)
            {
            // unregister the publisher listener in each partition
            PagedTopicCaches caches = m_caches;
            if (caches.Notifications.isActive())
                {
                caches.Notifications.removeMapListener(f_listenerNotification, f_filterListenerNotification);
                }
            }
        m_caches = null;
        dispatch(PublisherEvent.Type.Destroyed);
        }

    @Override
    public String getTopicName()
        {
        return f_sTopicName;
        }

    @Override
    public TopicService getTopicService()
        {
        return f_service;
        }

    @Override
    public TopicDependencies getTopicDependencies()
        {
        return f_dependencies;
        }

    @Override
    public int getChannelCount()
        {
        return f_cChannel;
        }

    @Override
    public void ensureConnected()
        {
        if (fClosed)
            {
            throw new IllegalStateException("Connector is closed");
            }
        m_caches.ensureConnected();
        }

    @Override
    public void addListener(NamedTopicPublisher.PublisherListener listener)
        {
        f_listeners.add(listener);
        }

    @Override
    public void removeListener(NamedTopicPublisher.PublisherListener listener)
        {
        f_listeners.remove(listener);
        }

    @Override
    public PublisherChannelConnector<V> createChannelConnector(int nChannel)
        {
        return new PagedTopicChannelPublisherConnector<>(getId(), nChannel, f_cChannel, m_caches, f_nNotifyPostFull);
        }

    @Override
    public long getMaxBatchSizeBytes()
        {
        return getTopicDependencies().getMaxBatchSizeBytes();
        }

    private void onNotification(int[] anChannel)
        {
        NamedTopicPublisher.PublisherEvent event = new PublisherEvent(this, PublisherEvent.Type.ChannelsFreed, anChannel);
        event.dispatch(f_listeners);
        }

    /**
     * Dispatch a {@link PublisherEvent}.
     *
     * @param type  the type of event.
     */
    protected void dispatch(PublisherEvent.Type type)
        {
        NamedTopicPublisher.PublisherEvent event = new PublisherEvent(PagedTopicPublisherConnector.this, type);
        event.dispatch(f_listeners);
        }

    // ----- inner class: CachesListener ------------------------------------

    protected class CachesListener
            implements NamedTopicDeactivationListener
        {
        @Override
        public void onDisconnect()
            {
            dispatch(PublisherEvent.Type.Disconnected);
            }

        @Override
        public void onConnect()
            {
            dispatch(PublisherEvent.Type.Connected);
            }

        @Override
        public void onDestroy()
            {
            dispatch(PublisherEvent.Type.Destroyed);
            }

        @Override
        public void onRelease()
            {
            dispatch(PublisherEvent.Type.Released);
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create a publisher identifier.
     *
     * @return a publisher identifier
     */
    protected long createId(TopicService service)
        {
        Cluster cluster   = service.getCluster();
        long    nMemberId = cluster.getLocalMember().getId();
        long    nNotificationId = System.identityHashCode(this);
        return (nMemberId << 32) | (nNotificationId & 0xFFFFFFFFL);
        }

    // ----- data members ---------------------------------------------------

    /**
     * A unique identifier for this publisher.
     */
    private final long f_nId;

    /**
     * The name of the topic.
     */
    private final String f_sTopicName;

    /**
     * The topic service.
     */
    private final TopicService f_service;

    /**
     * The topic dependencies.
     */
    private final TopicDependencies f_dependencies;

    /**
     * The topic channel count.
     */
    private final int f_cChannel;

    /**
     * The {@link PagedTopicCaches} to use to invoke cache operations.
     */
    private PagedTopicCaches m_caches;

    /**
     * A flag indicating whether this publisher is closed.
     */
    private boolean fClosed;

    /**
     * Filter used with f_listenerNotification.
     */
    private final Filter<int[]> f_filterListenerNotification;

    /**
     * The post full notifier.
     */
    private final int f_nNotifyPostFull;

    /**
     * The listener used to notify this publisher that previously full topics now have more space.
     */
    private final MapListener<NotificationKey, int[]> f_listenerNotification;

    /**
     * The listener list.
     */
    private final Listeners f_listeners = new Listeners();
    }
