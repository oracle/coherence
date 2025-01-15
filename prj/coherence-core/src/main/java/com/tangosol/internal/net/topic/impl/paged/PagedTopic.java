/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.internal.net.topic.NamedTopicSubscriber;
import com.tangosol.internal.net.topic.NamedTopicPublisher;
import com.tangosol.internal.net.topic.PublisherConnector;
import com.tangosol.internal.net.topic.SubscriberConnector;
import com.tangosol.internal.net.topic.impl.paged.agent.DestroySubscriptionProcessor;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;

import com.tangosol.internal.net.topic.impl.paged.model.Subscription;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.Service;

import com.tangosol.net.events.EventDispatcherRegistry;
import com.tangosol.net.events.internal.TopicDispatcher;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.NamedTopicEvent;
import com.tangosol.net.topic.NamedTopicListener;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Filter;

import com.tangosol.io.ClassLoaderAware;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.Listeners;
import com.tangosol.util.ValueExtractor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * An implementation of a {@link NamedTopic} which provides global ordering.
 * <p>
 * This implementation uses a number of underlying {@link NamedCache} instances
 * to hold the data for this {@link NamedTopic}.
 *
 * @param <V>  the data type of the elements stored in the topic
 *
 * @author jk 2015.06.03
 * @since Coherence 14.1.1
 */
public class PagedTopic<V>
        implements NamedTopic<V>,
                   ClassLoaderAware,
                   PublisherConnector.Factory<V>,
                   SubscriberConnector.Factory<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a NamedTopicImpl that will use the specified {@link PagedTopicCaches}
     * to hold the topic data and the specified {@link Publisher} to
     * populate the topic.
     *
     * @param pagedTopicCaches  the {@link PagedTopicCaches} holding the underlying topic data
     */
    public PagedTopic(PagedTopicCaches pagedTopicCaches)
        {
        f_listeners        = new Listeners();
        f_sTopicName       = pagedTopicCaches.getTopicName();
        f_pagedTopicCaches = pagedTopicCaches;
        f_pagedTopicCaches.addListener(new TopicListener(f_listeners));
        f_dispatcher       = new TopicDispatcher(f_sTopicName, pagedTopicCaches.getService());
        }

    // ----- PagedTopic methods ---------------------------------------

    /**
     * Return the {@link PagedTopicDependencies} for this topic.
     *
     * @return the {@link PagedTopicDependencies} for this topic
     */
    public PagedTopicDependencies getDependencies()
        {
        return f_pagedTopicCaches.getDependencies();
        }

    /**
     * Return the {@link TopicDispatcher event dispatcher}.
     *
     * @return the {@link TopicDispatcher event dispatcher}
     */
    public TopicDispatcher getEventDispatcher()
        {
        return f_dispatcher;
        }

    // ----- NamedTopic methods ---------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public <U> Subscriber<U> createSubscriber(Subscriber.Option<? super V, U>... options)
        {
        ensureActive();
        PagedTopicSubscriberConnector<U> connector = createSubscriberConnector(options);
        return new NamedTopicSubscriber<>(this, connector, options);
        }

    @Override
    public <U> PagedTopicSubscriberConnector<U> createSubscriberConnector(Subscriber.Option<? super V, U>[] options)
        {
        f_pagedTopicCaches.ensureConnected();
        return new PagedTopicSubscriberConnector<>(f_pagedTopicCaches, options);
        }

    @Override
    public void ensureSubscriberGroup(String sName, Filter<?> filter, ValueExtractor<?, ?> extractor)
        {
        if (sName == null)
            {
            throw new IllegalArgumentException("invalid group name");
            }
        f_pagedTopicCaches.ensureSubscriberGroup(sName, filter, extractor);
        }

    @Override
    public void destroySubscriberGroup(String sGroupName)
        {
        if (sGroupName == null)
            {
            throw new IllegalArgumentException("invalid group name");
            }
        PagedTopic.destroy(f_pagedTopicCaches, SubscriberGroupId.withName(sGroupName), 0L);
        }

    @Override
    public Set<String> getSubscriberGroups()
        {
        return f_pagedTopicCaches.getSubscriberGroups();
        }

    @Override
    @SuppressWarnings("unchecked")
    public Publisher<V> createPublisher(Publisher.Option<? super V>... options)
        {
        ensureActive();
        PagedTopicPublisherConnector<V> connector = createPublisherConnector(options);
        return new NamedTopicPublisher<>(this, connector, options);
        }

    @Override
    public PagedTopicPublisherConnector<V> createPublisherConnector(Publisher.Option<? super V>[] options)
        {
        int cChannel = getChannelCount();
        return new PagedTopicPublisherConnector<>(f_pagedTopicCaches, cChannel, options);
        }

    @Override
    public void destroy()
        {
        f_pagedTopicCaches.destroy();
        }

    @Override
    public Service getService()
        {
        return getCacheService();
        }

    public CacheService getCacheService()
        {
        return f_pagedTopicCaches.getService();
        }

    @Override
    public String getName()
        {
        return f_sTopicName;
        }

    @Override
    public void release()
        {
        f_pagedTopicCaches.release();
        }

    @Override
    public boolean isActive()
        {
        return f_pagedTopicCaches.isActive();
        }

    @Override
    public boolean isDestroyed()
        {
        return f_pagedTopicCaches.isDestroyed();
        }

    @Override
    public boolean isReleased()
        {
        return f_pagedTopicCaches.isReleased();
        }

    @Override
    public int getChannelCount()
        {
        return f_pagedTopicCaches.getChannelCount();
        }

    @Override
    public int getRemainingMessages(String sSubscriberGroup, int... anChannel)
        {
        return f_pagedTopicCaches.getRemainingMessages(SubscriberGroupId.withName(sSubscriberGroup), anChannel);
        }

    @Override
    public void addListener(NamedTopicListener listener)
        {
        f_listeners.add(listener);
        }

    @Override
    public void removeListener(NamedTopicListener listener)
        {
        f_listeners.remove(listener);
        }

    // ----- ClassLoaderAware methods ---------------------------------------

    @Override
    public ClassLoader getContextClassLoader()
        {
        return f_pagedTopicCaches.getContextClassLoader();
        }

    @Override
    public void setContextClassLoader(ClassLoader classLoader)
        {
        throw new UnsupportedOperationException();
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        PagedTopic<?> that = (PagedTopic<?>) o;
        return Objects.equals(f_pagedTopicCaches, that.f_pagedTopicCaches);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_pagedTopicCaches);
        }

    @Override
    public String toString()
        {
        return getClass().getSimpleName() + "(name=" + f_sTopicName + ")";
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Destroy a subscriber group.
     *
     * @param caches   the associated caches
     * @param groupId  the group to destroy
     */
    public static void destroy(PagedTopicCaches caches, SubscriberGroupId groupId, long lSubscriptionId)
        {
        PagedTopicService service = caches.getService();
        if (lSubscriptionId == 0 && !groupId.isAnonymous())
            {
            lSubscriptionId = service.getSubscriptionId(caches.getTopicName(), groupId);
            }

        service.destroySubscription(lSubscriptionId);

        if (caches.isActive() && caches.Subscriptions.isActive())
            {
            int                   cParts      = service.getPartitionCount();
            Set<Subscription.Key> setSubParts = new HashSet<>(cParts);

            for (int i = 0; i < cParts; ++i)
                {
                // channel 0 will propagate the operation to all other channels
                setSubParts.add(new Subscription.Key(i, /*nChannel*/ 0, groupId));
                }


            DestroySubscriptionProcessor processor = new DestroySubscriptionProcessor(lSubscriptionId);
            InvocableMapHelper.invokeAllAsync(caches.Subscriptions, setSubParts,
                                              (key) -> caches.getUnitOfOrder(key.getPartitionId()),
                                              processor)
                    .join();
            }
        }

    /**
     * Ensure that this {@link NamedTopic} is active.
     *
     * @throws IllegalStateException if not active
     */
    protected void ensureActive()
        {
        if (!f_pagedTopicCaches.isActive())
            {
            throw new IllegalStateException("This topic is no longer active");
            }
        }

    // ----- inner class: TopicListener -------------------------------------

    private class TopicListener
            implements PagedTopicCaches.Listener
        {
        public TopicListener(Listeners listeners)
            {
            m_listeners = listeners;
            }

        @Override
        public void onDestroy()
            {
            NamedTopicEvent event = new NamedTopicEvent(PagedTopic.this, NamedTopicEvent.Type.Destroyed);
            event.dispatch(m_listeners);
            }

        @Override
        public void onDisconnect()
            {
            }

        @Override
        public void onConnect()
            {
            }

        @Override
        public void onRelease()
            {
            }

        // ----- data members ---------------------------------------------------

        private final Listeners m_listeners;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of this topic.
     */
    private final String f_sTopicName;

    /**
     * The {@link PagedTopicCaches} instance managing the caches containing this topic's data.
     */
    private final PagedTopicCaches f_pagedTopicCaches;

    /**
     * The listeners registered with this topic.
     */
    private final Listeners f_listeners;

    /**
     * The {@link TopicDispatcher} to dispatch lifecycle events.
     */
    private final TopicDispatcher f_dispatcher;
    }
