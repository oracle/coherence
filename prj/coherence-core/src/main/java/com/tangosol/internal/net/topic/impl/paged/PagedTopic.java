/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.Service;

import com.tangosol.net.partition.PartitionSet;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Base;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.PartitionedFilter;

import com.tangosol.io.ClassLoaderAware;

import com.tangosol.internal.net.topic.impl.paged.model.Subscription.Key;

import java.util.Set;
import java.util.stream.Collectors;

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

    // ----- NamedTopic methods ---------------------------------------

    @Override
    public <U> Subscriber<U> createSubscriber(Subscriber.Option<? super V, U>... options)
        {
        ensureActive();

        return new PagedTopicSubscriber<>(f_pagedTopicCaches, options);
        }

    @Override
    public void destroySubscriberGroup(String sGroupName)
        {
        if (sGroupName == null)
            {
            throw new IllegalArgumentException("invalid group name");
            }

        PagedTopicSubscriber.destroy(f_pagedTopicCaches, SubscriberGroupId.withName(sGroupName));
        }

    @Override
    public Set<String> getSubscriberGroups()
        {
        int cParts = ((PartitionedService) f_pagedTopicCaches.Subscriptions.getCacheService()).getPartitionCount();
        PartitionSet setPart = new PartitionSet(cParts);
        setPart.add(Base.getRandom().nextInt(cParts));
        Set<Key> setSubs = f_pagedTopicCaches.Subscriptions
                .keySet(new PartitionedFilter<>(AlwaysFilter.INSTANCE(), setPart));

        return setSubs.stream()
                .filter((key) -> key.getGroupId().getMemberTimestamp() == 0)
                .map((key) -> key.getGroupId().getGroupName())
                .collect(Collectors.toSet());
        }

    @Override
    public Publisher<V> createPublisher(Publisher.Option<? super V>... options)
        {
        ensureActive();

        return new PagedTopicPublisher<>(f_pagedTopicCaches, options);
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
        return f_pagedTopicCaches.getCacheService();
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
            f_pagedTopicCaches.close();
            }
        }

    @Override
    public boolean isActive()
        {
        return f_pagedTopicCaches.isActive();
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

        return !(f_pagedTopicCaches != null
                 ? !f_pagedTopicCaches.equals(that.f_pagedTopicCaches)
                 : that.f_pagedTopicCaches != null);

        }

    @Override
    public int hashCode()
        {
        return f_pagedTopicCaches != null
               ? f_pagedTopicCaches.hashCode()
               : 0;
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

    // ----- data members ---------------------------------------------------

    /**
     * The {@link PagedTopicCaches} instance managing the caches containing this topic's data.
     */
    private final PagedTopicCaches f_pagedTopicCaches;
    }
