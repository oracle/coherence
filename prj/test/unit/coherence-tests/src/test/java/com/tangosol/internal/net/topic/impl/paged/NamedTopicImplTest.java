/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.internal.net.topic.NamedTopicView;
import com.tangosol.net.PagedTopicService;
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
@SuppressWarnings("resource")
public class NamedTopicImplTest
    {
    @Test
    public void shouldNotCreateProducerIfNotActive()
        {
        PagedTopicCaches caches = mock(PagedTopicCaches.class);

        when(caches.isActive()).thenReturn(false);

        NamedTopic<String> topic = new NamedTopicView<>(new PagedTopicConnector<>(caches));

        assertThrows(IllegalStateException.class, topic::createPublisher);
        }

    @Test
    public void shouldDestroyQueueIfCachesActive()
        {
        PagedTopicCaches caches = mock(PagedTopicCaches.class);

        when(caches.isActive()).thenReturn(true);

        NamedTopic<String> topic = new NamedTopicView<>(new PagedTopicConnector<>(caches));

        topic.destroy();
        verify(caches).destroy();
        }

    @Test
    public void shouldCloseQueue()
        {
        PagedTopicCaches caches = mock(PagedTopicCaches.class);

        when(caches.isActive()).thenReturn(true);

        NamedTopic<String> topic = new NamedTopicView<>(new PagedTopicConnector<>(caches));

        topic.close();
        verify(caches).release();
        }

    @Test
    public void shouldNotCloseInactiveCaches()
        {
        PagedTopicCaches caches = mock(PagedTopicCaches.class);

        when(caches.isActive()).thenReturn(false);

        NamedTopic<String> topic = new NamedTopicView<>(new PagedTopicConnector<>(caches));

        topic.close();
        verify(caches, never()).release();
        }

    @Test
    public void shouldCloseQueueUsingRelease()
        {
        PagedTopicCaches caches = mock(PagedTopicCaches.class);

        when(caches.isActive()).thenReturn(true);

        NamedTopic<String> topic = new NamedTopicView<>(new PagedTopicConnector<>(caches));

        topic.release();
        verify(caches).release();
        }

    @Test
    public void shouldReturnService()
        {
        PagedTopicCaches  caches  = mock(PagedTopicCaches.class);
        PagedTopicService service = mock(PagedTopicService.class);

        when(caches.isActive()).thenReturn(true);
        when(caches.getService()).thenReturn(service);

        NamedTopic<String> topic = new NamedTopicView<>(new PagedTopicConnector<>(caches));

        assertThat(topic.getService(), is(sameInstance(service)));
        assertThat(topic.getService(), is(sameInstance(service)));
        }

    @Test
    public void shouldReturnQueueName()
        {
        PagedTopicCaches caches = mock(PagedTopicCaches.class);

        when(caches.isActive()).thenReturn(true);
        when(caches.getTopicName()).thenReturn("Foo");

        NamedTopic<String> queue = new NamedTopicView<>(new PagedTopicConnector<>(caches));

        assertThat(queue.getName(), is("Foo"));
        }

    @Test
    public void shouldBeActive()
        {
        PagedTopicCaches caches = mock(PagedTopicCaches.class);

        when(caches.isActive()).thenReturn(true);

        NamedTopic<String> queue = new NamedTopicView<>(new PagedTopicConnector<>(caches));

        assertThat(queue.isActive(), is(true));
        }

    @Test
    public void shouldBeInactive()
        {
        PagedTopicCaches caches = mock(PagedTopicCaches.class);

        when(caches.isActive()).thenReturn(false);

        NamedTopic<String> queue = new NamedTopicView<>(new PagedTopicConnector<>(caches));

        assertThat(queue.isActive(), is(false));
        }
    }
