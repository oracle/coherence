/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches.Names;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Converter;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author jk 2015.06.23
 */
@SuppressWarnings("rawtypes")
public class PagedTopicTest
    {
    @Test
    public void shouldEnsurePartitionedQueue()
        {
        BinaryEntry              entry         = mock(BinaryEntry.class);
        BackingMapContext        ctxBackingMap = mock(BackingMapContext.class);
        BackingMapManagerContext ctxManager    = mock(BackingMapManagerContext.class);

        when(entry.getContext()).thenReturn(ctxManager);
        when(entry.getBackingMapContext()).thenReturn(ctxBackingMap);
        when(ctxBackingMap.getManagerContext()).thenReturn(ctxManager);
        when(ctxBackingMap.getCacheName()).thenReturn(Names.PAGES.cacheNameForTopicName("Foo"));

        PagedTopicPartition queue = PagedTopicPartition.ensureTopic(entry);

        assertThat(queue, is(notNullValue()));
        assertThat(queue.f_ctxManager, is(sameInstance(ctxManager)));
        assertThat(queue.f_sName, is("Foo"));
        }

    @Test
    public void shouldGetKeyToInternalConverter()
        {
        BackingMapManagerContext ctxManager = mock(BackingMapManagerContext.class);
        Converter                converter  = mock(Converter.class);

        when(ctxManager.getKeyToInternalConverter()).thenReturn(converter);

        PagedTopicPartition queue = new PagedTopicPartition(ctxManager, "Foo", 0);

        assertThat(queue.getKeyToInternalConverter(), is(sameInstance(converter)));
        }

    @Test
    public void shouldGetBackingMapContext()
        {
        BackingMapManagerContext ctxManager    = mock(BackingMapManagerContext.class);
        BackingMapContext        ctxBackingMap = mock(BackingMapContext.class);
        String                   sQueueName    = "Foo";
        String                   sCacheName    = PagedTopicCaches.Names.PAGES.cacheNameForTopicName(sQueueName);

        when(ctxManager.getBackingMapContext(sCacheName)).thenReturn(ctxBackingMap);

        PagedTopicPartition queue = new PagedTopicPartition(ctxManager, "Foo", 0);

        assertThat(queue.getBackingMapContext(PagedTopicCaches.Names.PAGES), is(sameInstance(ctxBackingMap)));
        }

    @Test
    public void shouldGetBackingMapEntry()
        {
        BackingMapManagerContext ctxManager    = mock(BackingMapManagerContext.class);
        BackingMapContext        ctxBackingMap = mock(BackingMapContext.class);
        BinaryEntry              entry         = mock(BinaryEntry.class);
        Binary                   binKey        = new Binary(new byte[] {0, 1, 2});
        String                   sQueueName    = "Foo";
        String                   sCacheName    = PagedTopicCaches.Names.PAGES.cacheNameForTopicName(sQueueName);

        when(ctxManager.getBackingMapContext(sCacheName)).thenReturn(ctxBackingMap);
        when(ctxBackingMap.getBackingMapEntry(binKey)).thenReturn(entry);

        PagedTopicPartition queue = new PagedTopicPartition(ctxManager, "Foo", 0);

        assertThat(queue.enlistBackingMapEntry(PagedTopicCaches.Names.PAGES, binKey), is(sameInstance(entry)));
        }

    @Test
    public void shouldGetQueueConfiguration()
        {
        BackingMapManagerContext ctxManager    = mock(BackingMapManagerContext.class);
        CacheService             cacheService  = mock(CacheService.class);
        ResourceRegistry         registry      = new SimpleResourceRegistry();
        PagedTopic.Dependencies  configuration = new Configuration();

        when(ctxManager.getCacheService()).thenReturn(cacheService);
        when(cacheService.getResourceRegistry()).thenReturn(registry);

        registry.registerResource(PagedTopic.Dependencies.class, "Foo", configuration);

        PagedTopicPartition queue = new PagedTopicPartition(ctxManager, "Foo", 0);

        assertThat(queue.getDependencies(), is(sameInstance(configuration)));
        }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetContextClassLoaderThrowsUnsupportedException()
        {
        CacheService cacheService = mock(CacheService.class);

        PagedTopic<?> topic = new PagedTopic<>(new PagedTopicCaches("topic", cacheService));
        topic.setContextClassLoader(Thread.currentThread().getContextClassLoader());
        }

    @Test(expected = IllegalArgumentException.class)
    public void testDestroySubscriberGroupWithNullParam()
        {
        CacheService cacheService = mock(CacheService.class);

        PagedTopic topic = new PagedTopic(new PagedTopicCaches("topic", cacheService));
        topic.destroySubscriberGroup(null);
        }

    @Test
    public void testEquality()
        {
        CacheService cacheService = mock(CacheService.class);

        PagedTopic topic1 = new PagedTopic(new PagedTopicCaches("topic", cacheService));
        PagedTopic topic2 = new PagedTopic(new PagedTopicCaches("different-topic", cacheService));
        PagedTopic topic3 = new PagedTopic(new PagedTopicCaches("topic", cacheService));
        PagedTopic topic4 = new PagedTopic(null);
        PagedTopic topic5 = new PagedTopic(null);

        assertThat(topic1, is(topic1));
        assertThat(topic1, is(topic3));
        assertThat(topic1.hashCode(), is(topic3.hashCode()));
        assertThat(topic4, is(topic5));
        assertThat(topic4.hashCode(), is(topic5.hashCode()));

        assertThat(topic1, is(notNullValue()));
        assertThat(topic1, is(not(topic2)));
        assertThat(topic4, is(not(topic1)));
        assertThat(topic1, is(not(topic4)));

        assertThat(topic1.toString(), is(notNullValue()));
        }
    }
