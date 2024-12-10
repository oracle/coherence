/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.coherence.config.unit.Seconds;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Filter;

import com.tangosol.io.ClassLoaderAware;
import com.tangosol.util.ValueExtractor;

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
        implements NamedTopic<V>, ClassLoaderAware
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
        f_pagedTopicCaches = pagedTopicCaches;
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

    // ----- NamedTopic methods ---------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public <U> Subscriber<U> createSubscriber(Subscriber.Option<? super V, U>... options)
        {
        ensureActive();
        f_pagedTopicCaches.ensureConnected();
        return new PagedTopicSubscriber<>(this, f_pagedTopicCaches, options);
        }

    @Override
    public void ensureSubscriberGroup(String sName, Filter<?> filter, ValueExtractor<?, ?> extractor)
        {
        f_pagedTopicCaches.ensureSubscriberGroup(sName, filter, extractor);
        }

    @Override
    public void destroySubscriberGroup(String sGroupName)
        {
        if (sGroupName == null)
            {
            throw new IllegalArgumentException("invalid group name");
            }
        PagedTopicSubscriber.destroy(f_pagedTopicCaches, SubscriberGroupId.withName(sGroupName), 0L);
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

        return new PagedTopicPublisher<>(this, f_pagedTopicCaches, options);
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
        return getClass().getSimpleName() + "(name=" + f_pagedTopicCaches.getTopicName() + ")";
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Ensure that this {@link PagedTopic} is active.
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

    // ----- constants ------------------------------------------------------

    /**
     * The topic should have the default number of channels.
     */
    public static final int DEFAULT_CHANNEL_COUNT = 0;

    /**
     * The default capacity of pages when using the default binary calculator (1MB).
     */
    public static final long DEFAULT_PAGE_CAPACITY_BYTES = 1024*1024;

    /**
     * The default subscriber timeout.
     */
    public static final Seconds DEFAULT_SUBSCRIBER_TIMEOUT_SECONDS = new Seconds(300);

    /**
     * The default reconnect timeout.
     */
    public static final Seconds DEFAULT_RECONNECT_TIMEOUT_SECONDS = new Seconds(300);

    /**
     * The default reconnect retry.
     */
    public static final Seconds DEFAULT_RECONNECT_RETRY_SECONDS = new Seconds(5);

    /**
     * The default reconnect wait.
     */
    public static final Seconds DEFAULT_RECONNECT_WAIT_SECONDS = new Seconds(10);

    // ----- data members ---------------------------------------------------

    /**
     * The {@link PagedTopicCaches} instance managing the caches containing this topic's data.
     */
    private final PagedTopicCaches f_pagedTopicCaches;
    }
