/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.net.CacheService;
import com.tangosol.net.topic.NamedTopic;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.junit.Assert.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jk 2015.06.22
 */
@SuppressWarnings("unchecked")
public class NamedTopicImplTest
    {
    @Test
    public void shouldNotCreateProducerIfNotActive() throws Exception
        {
        PagedTopicCaches topic = mock(PagedTopicCaches.class);

        when(topic.isActive()).thenReturn(false);

        NamedTopic<String> queue = new PagedTopic<>(topic);

        m_expectedException.expect(IllegalStateException.class);

        queue.createPublisher();
        }

    @Test
    public void shouldDestroyQueueIfCachesActive()
            throws Exception
        {
        PagedTopicCaches topic = mock(PagedTopicCaches.class);

        when(topic.isActive()).thenReturn(true);

        PagedTopic<String> queue = new PagedTopic<>(topic);

        queue.destroy();

        verify(topic).destroy();
        }

    @Test
    public void shouldCloseQueue()
            throws Exception
        {
        PagedTopicCaches topic = mock(PagedTopicCaches.class);

        when(topic.isActive()).thenReturn(true);

        PagedTopic<String> queue = new PagedTopic<>(topic);


        queue.close();

        verify(topic).close();
        }

    @Test
    public void shouldNotCloseInactiveCaches()
            throws Exception
        {
        PagedTopicCaches topic = mock(PagedTopicCaches.class);

        when(topic.isActive()).thenReturn(false);

        PagedTopic<String> queue = new PagedTopic<>(topic);


        queue.close();

        verify(topic, never()).close();
        }

    @Test
    public void shouldCloseQueueUsingRelease()
            throws Exception
        {
        PagedTopicCaches topic = mock(PagedTopicCaches.class);

        when(topic.isActive()).thenReturn(true);

        PagedTopic<String> queue = new PagedTopic<>(topic);

        queue.release();

        verify(topic).close();
        }

    @Test
    public void shouldReturnService()
            throws Exception
        {
        PagedTopicCaches topic = mock(PagedTopicCaches.class);
        CacheService cacheService  = mock(CacheService.class);

        when(topic.isActive()).thenReturn(true);
        when(topic.getCacheService()).thenReturn(cacheService);

        PagedTopic<String> queue = new PagedTopic<>(topic);

        assertThat(queue.getService(), is(sameInstance(cacheService)));
        assertThat(queue.getService(), is(sameInstance(cacheService)));
        }

    @Test
    public void shouldReturnQueueName()
            throws Exception
        {
        PagedTopicCaches topic = mock(PagedTopicCaches.class);

        when(topic.isActive()).thenReturn(true);
        when(topic.getTopicName()).thenReturn("Foo");

        PagedTopic<String> queue = new PagedTopic<>(topic);

        assertThat(queue.getName(), is("Foo"));
        }

    @Test
    public void shouldBeActive()
            throws Exception
        {
        PagedTopicCaches topic = mock(PagedTopicCaches.class);

        when(topic.isActive()).thenReturn(true);

        PagedTopic<String> queue = new PagedTopic<>(topic);

        assertThat(queue.isActive(), is(true));
        }

    @Test
    public void shouldBeInactive()
            throws Exception
        {
        PagedTopicCaches topic = mock(PagedTopicCaches.class);

        when(topic.isActive()).thenReturn(false);

        PagedTopic<String> queue = new PagedTopic<>(topic);

        assertThat(queue.isActive(), is(false));
        }

    @Rule
    public ExpectedException m_expectedException = ExpectedException.none();
    }
