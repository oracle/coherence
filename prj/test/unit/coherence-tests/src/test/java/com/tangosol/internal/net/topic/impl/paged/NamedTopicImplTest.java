/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.net.CacheService;
import com.tangosol.net.topic.NamedTopic;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jk 2015.06.22
 */
public class NamedTopicImplTest
    {
    @Test
    public void shouldNotCreateProducerIfNotActive()
        {
        PagedTopicCaches caches = mock(PagedTopicCaches.class);

        when(caches.isActive()).thenReturn(false);

        NamedTopic<String> topic = new PagedTopic<>(caches);

        assertThrows(IllegalStateException.class, topic::createPublisher);
        }

    @Test
    public void shouldDestroyQueueIfCachesActive()
        {
        PagedTopicCaches caches = mock(PagedTopicCaches.class);

        when(caches.isActive()).thenReturn(true);

        PagedTopic<String> topic = new PagedTopic<>(caches);

        topic.destroy();
        verify(caches).destroy();
        }

    @Test
    public void shouldCloseQueue()
        {
        PagedTopicCaches caches = mock(PagedTopicCaches.class);

        when(caches.isActive()).thenReturn(true);

        PagedTopic<String> topic = new PagedTopic<>(caches);

        topic.close();
        verify(caches).release();
        }

    @Test
    public void shouldNotCloseInactiveCaches()
        {
        PagedTopicCaches caches = mock(PagedTopicCaches.class);

        when(caches.isActive()).thenReturn(false);

        PagedTopic<String> topic = new PagedTopic<>(caches);

        topic.close();
        verify(caches, never()).release();
        }

    @Test
    public void shouldCloseQueueUsingRelease()
        {
        PagedTopicCaches caches = mock(PagedTopicCaches.class);

        when(caches.isActive()).thenReturn(true);

        PagedTopic<String> topic = new PagedTopic<>(caches);

        topic.release();
        verify(caches).release();
        }

    @Test
    public void shouldReturnService()
        {
        PagedTopicCaches caches = mock(PagedTopicCaches.class);
        CacheService cacheService  = mock(CacheService.class);

        when(caches.isActive()).thenReturn(true);
        when(caches.getCacheService()).thenReturn(cacheService);

        PagedTopic<String> topic = new PagedTopic<>(caches);

        assertThat(topic.getService(), is(sameInstance(cacheService)));
        assertThat(topic.getService(), is(sameInstance(cacheService)));
        }

    @Test
    public void shouldReturnQueueName()
        {
        PagedTopicCaches topic = mock(PagedTopicCaches.class);

        when(topic.isActive()).thenReturn(true);
        when(topic.getTopicName()).thenReturn("Foo");

        PagedTopic<String> queue = new PagedTopic<>(topic);

        assertThat(queue.getName(), is("Foo"));
        }

    @Test
    public void shouldBeActive()
        {
        PagedTopicCaches topic = mock(PagedTopicCaches.class);

        when(topic.isActive()).thenReturn(true);

        PagedTopic<String> queue = new PagedTopic<>(topic);

        assertThat(queue.isActive(), is(true));
        }

    @Test
    public void shouldBeInactive()
        {
        PagedTopicCaches topic = mock(PagedTopicCaches.class);

        when(topic.isActive()).thenReturn(false);

        PagedTopic<String> queue = new PagedTopic<>(topic);

        assertThat(queue.isActive(), is(false));
        }
    }
