/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.internal.net.topic.NamedTopicConnector;
import com.tangosol.internal.net.topic.NamedTopicSubscriber;
import com.tangosol.internal.net.topic.NamedTopicView;
import com.tangosol.internal.net.topic.PublisherConnector;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.NamedTopicEvent;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

import java.util.Objects;

/**
 * An implementation of a {@link NamedTopic} which provides global ordering.
 * <p>
 * This implementation uses a number of underlying {@link NamedCache} instances
 * to hold the data for this {@link NamedTopic}.
 *
 * @param <V>  the data type of the elements stored in the topic
 *
 * @author Jonathan Knight  2024.11.26
 */
public class PagedTopicConnector<V>
        implements NamedTopicConnector<V>, PublisherConnector.Factory<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a NamedTopicImpl that will use the specified {@link PagedTopicCaches}
     * to hold the topic data and the specified {@link Publisher} to
     * populate the topic.
     *
     * @param pagedTopicCaches  the {@link PagedTopicCaches} holding the underlying topic data
     */
    public PagedTopicConnector(PagedTopicCaches pagedTopicCaches)
        {
        f_pagedTopicCaches = pagedTopicCaches;
        pagedTopicCaches.addListener(new TopicListener());
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

    public PagedTopicCaches getPagedTopicCaches()
        {
        return f_pagedTopicCaches;
        }

    @Override
    public void setConnectedNamedTopic(NamedTopicView<V> topic)
        {
        m_topic = topic;
        }

    @Override
    public PagedTopicService getTopicService()
        {
        return f_pagedTopicCaches.getService();
        }

    @Override
    public void close()
        {
        release();
        }

    @Override
    public void destroy()
        {
        f_pagedTopicCaches.destroy();
        }

    @Override
    public String getName()
        {
        return f_pagedTopicCaches.getTopicName();
        }

    @Override
    public void release()
        {
        if (f_pagedTopicCaches.isActive())
            {
            f_pagedTopicCaches.release();
            }
        }

    @Override
    public boolean isActive()
        {
        return f_pagedTopicCaches.isActive();
        }

    @Override
    public boolean isDestroyed()
        {
        return m_fDestroyed || f_pagedTopicCaches.isDestroyed();
        }

    @Override
    public boolean isReleased()
        {
        return m_fReleased || f_pagedTopicCaches.isReleased();
        }

    @Override
    public int getRemainingMessages(String sSubscriberGroup, int... anChannel)
        {
        return f_pagedTopicCaches.getRemainingMessages(SubscriberGroupId.withName(sSubscriberGroup), anChannel);
        }

    @Override
    public void ensureSubscriberGroup(String sSubscriberGroup, Filter<?> filter, ValueExtractor<?, ?> extractor)
        {
        if (sSubscriberGroup == null)
            {
            throw new IllegalArgumentException("invalid group name");
            }
        f_pagedTopicCaches.ensureSubscriberGroup(sSubscriberGroup, filter, extractor);
        }

    @Override
    public void destroySubscriberGroup(String sSubscriberGroup)
        {
        if (sSubscriberGroup == null)
            {
            throw new IllegalArgumentException("invalid group name");
            }
        PagedTopic.destroy(f_pagedTopicCaches, SubscriberGroupId.withName(sSubscriberGroup), 0L);
        }

    @Override
    public PublisherConnector<V> createPublisher(Publisher.Option<? super V>[] options)
        {
        ensureActive();
        int cChannel = f_pagedTopicCaches.getChannelCount();
        return new PagedTopicPublisherConnector<>(f_pagedTopicCaches, cChannel, options);
        }

    @Override
    public <U> NamedTopicSubscriber<U> createSubscriber(Subscriber.Option<? super V, U>[] options)
        {
        ensureActive();
        PagedTopicSubscriberConnector<U> connector = createSubscriberConnector(options);
        return new NamedTopicSubscriber<>(m_topic, connector, options);
        }

    @Override
    public PublisherConnector<V> createPublisherConnector(Publisher.Option<? super V>[] options)
        {
        return createPublisher(options);
        }

    @Override
    public <U> PagedTopicSubscriberConnector<U> createSubscriberConnector(Subscriber.Option<? super V, U>[] options)
        {
        return new PagedTopicSubscriberConnector<U>(f_pagedTopicCaches, options);
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
        PagedTopicConnector<?> that = (PagedTopicConnector<?>) o;
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
        return getClass().getSimpleName() + "(name=" + f_pagedTopicCaches.getTopicName() + ")";
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Ensure that this {@link PagedTopicConnector} is active.
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
        @Override
        public void onDestroy()
            {
            m_fDestroyed = true;
            m_topic.dispatchEvent(NamedTopicEvent.Type.Destroyed);
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
            m_fReleased = true;
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link NamedTopicView}.
     */
    private NamedTopicView<V> m_topic;

    /**
     * The {@link PagedTopicCaches} instance managing the caches containing this topic's data.
     */
    private final PagedTopicCaches f_pagedTopicCaches;

    /**
     * A flag indicating whether the underlying topic has been destroyed.
     */
    private boolean m_fDestroyed;

    /**
     * A flag indicating whether the underlying topic has been released.
     */
    private boolean m_fReleased;
    }
