/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.safeNamedTopic;

import com.tangosol.coherence.Component;

import com.tangosol.coherence.component.util.SafeNamedTopic;

import com.tangosol.internal.net.topic.PublisherConnector;
import com.tangosol.internal.net.topic.SubscriberConnector;

import com.tangosol.internal.net.topic.impl.paged.PagedTopic;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicPublisherConnector;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriberConnector;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;

import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

/**
 * A safe wrapper around a paged topic.
 *
 * @author Jonathan Knight  2024.11.26
 */
@SuppressWarnings({"rawtypes"})
public class SafePagedTopic<V>
        extends SafeNamedTopic<V>
    {
    public SafePagedTopic()
        {
        this(null, null, true);
        }

    public SafePagedTopic(String sName, Component compParent, boolean fInit)
        {
        super(sName, compParent, fInit);
        }

    @Override
    public PublisherConnector<V> createPublisherConnector(Publisher.Option<? super V>[] options)
        {
        return new PagedTopicPublisherConnector<>(__m_PagedTopicCaches, getChannelCount(), options);
        }

    @Override
    public <U> SubscriberConnector<U> createSubscriberConnector(Subscriber.Option<? super V, U>[] options)
        {
        return new PagedTopicSubscriberConnector<>(__m_PagedTopicCaches, options);
        }

    @Override
    public boolean isDestroyed()
        {
        return super.isDestroyed() || getPagedTopicCaches().isDestroyed();
        }

    @Override
    public void destroy()
        {
        getPagedTopicCaches().destroy();
        super.destroy();
        }

    @Override
    public boolean isReleased()
        {
        return super.isReleased() || getPagedTopicCaches().isReleased();
        }

    @Override
    public void release()
        {
        getPagedTopicCaches().release();
        super.release();
        }

    @Override
    public void ensureSubscriberGroup(String sGroupName, Filter filter, ValueExtractor extractor)
        {
        if (sGroupName == null)
            {
            throw new IllegalArgumentException("invalid group name");
            }
        PagedTopicCaches pagedTopicCaches = getPagedTopicCaches();
        pagedTopicCaches.ensureSubscriberGroup(sGroupName, filter, extractor);
        }

    @Override
    public void destroySubscriberGroup(String sGroupName)
        {
        if (sGroupName == null)
            {
            throw new IllegalArgumentException("invalid group name");
            }

        PagedTopicCaches pagedTopicCaches = getPagedTopicCaches();
        PagedTopic.destroy(pagedTopicCaches, SubscriberGroupId.withName(sGroupName), 0L);
        }


    public PagedTopicCaches getPagedTopicCaches()
        {
        return __m_PagedTopicCaches;
        }

    public void setPagedTopicCaches(PagedTopicCaches cachesTopic)
        {
        __m_PagedTopicCaches = cachesTopic;
        }

    // ----- data members ---------------------------------------------------

    private PagedTopicCaches __m_PagedTopicCaches;
    }
