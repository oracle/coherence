/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.tangosol.io.Serializer;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jk 2015.06.19
 */
public class PagedTopicCachesTest
    {
    @Test
    public void shouldNotAllowNullName() throws Exception
        {
        m_expectedException.expect(IllegalArgumentException.class);
        m_expectedException.expectMessage(containsString("The name argument cannot be null or empty String"));

        String       sName        = null;
        CacheService cacheService = mock(CacheService.class);

        new PagedTopicCaches(sName, cacheService);
        }

    @Test
    public void shouldNotAllowEmptyName() throws Exception
        {
        m_expectedException.expect(IllegalArgumentException.class);
        m_expectedException.expectMessage(containsString("The name argument cannot be null or empty String"));

        String       sName        = "";
        CacheService cacheService = mock(CacheService.class);

        new PagedTopicCaches(sName, cacheService);
        }

    @Test
    public void shouldNotAllowNullCacheService() throws Exception
        {
        m_expectedException.expect(IllegalArgumentException.class);
        m_expectedException.expectMessage(containsString("The cacheService argument cannot be null"));

        String       sName        = "Foo";
        CacheService cacheService = null;

        new PagedTopicCaches(sName, cacheService);
        }

    @Test
    public void shouldHaveCorrectName() throws Exception
        {
        String           sName            = "Foo";
        CacheService     cacheService     = mock(CacheService.class);
        PagedTopicCaches pagedTopicCaches = new PagedTopicCaches(sName, cacheService);

        assertThat(pagedTopicCaches.getTopicName(), is(sName));
        }

    @Test
    public void shouldHaveCorrectCacheService() throws Exception
        {
        String       sName        = "Foo";
        CacheService cacheService = mock(CacheService.class);

        PagedTopicCaches pagedTopicCaches = new PagedTopicCaches(sName, cacheService);

        assertThat(pagedTopicCaches.getCacheService(), is(Matchers.sameInstance(cacheService)));
        }

    @Test
    public void shouldBeEqualWithEqualNames() throws Exception
        {
        String       sName        = "Foo";
        CacheService cacheService = mock(CacheService.class);

        PagedTopicCaches pagedTopicCaches1 = new PagedTopicCaches(sName, cacheService);
        PagedTopicCaches pagedTopicCaches2 = new PagedTopicCaches(sName, cacheService);

        assertThat(pagedTopicCaches1.equals(pagedTopicCaches2), is(true));
        }

    @Test
    public void shouldBeNotEqualWithNotEqualNames() throws Exception
        {
        CacheService cacheService = mock(CacheService.class);

        PagedTopicCaches pagedTopicCaches1 = new PagedTopicCaches("Foo", cacheService);
        PagedTopicCaches pagedTopicCaches2 = new PagedTopicCaches("Bar", cacheService);

        assertThat(pagedTopicCaches1.equals(pagedTopicCaches2), is(false));
        }

    @Test
    public void shouldReturnCacheServiceSerializer() throws Exception
        {
        CacheService cacheService = mock(CacheService.class);
        ClassLoader  loader       = mock(ClassLoader.class);
        Serializer   serializer   = mock(Serializer.class);

        when(cacheService.getContextClassLoader()).thenReturn(loader);
        when(cacheService.getSerializer()).thenReturn(serializer);

        PagedTopicCaches pagedTopicCaches = new PagedTopicCaches("Foo", cacheService);

        assertThat(pagedTopicCaches.getSerializer(), is(sameInstance(serializer)));
        }


    @Test
    public void shouldDestroyCaches() throws Exception
        {
        String                 sQueueName   = "Foo";
        CacheService           cacheService = mock(CacheService.class);
        ClassLoader            loader       = mock(ClassLoader.class);
        Map<String,NamedCache> mapCaches    = mockCachesForQueue(sQueueName);

        when(cacheService.getContextClassLoader()).thenReturn(loader);
        when(cacheService.ensureCache(anyString(), same(loader)))
                .thenAnswer(invocation ->
                            {
                            String sCacheName = String.valueOf(invocation.getArguments()[0]);
                            return mapCaches.get(sCacheName);
                            });

        PagedTopicCaches pagedTopicCaches = new PagedTopicCaches("Foo", cacheService);

        pagedTopicCaches.destroy();

        assertThat(pagedTopicCaches.f_setCaches, is(nullValue()));

        for (NamedCache cache : mapCaches.values())
            {
            verify(cache).destroy();
            }
        }

    @Test
    public void shouldReleaseCaches() throws Exception
        {
        String                 sQueueName   = "Foo";
        CacheService           cacheService = mock(CacheService.class);
        ClassLoader            loader       = mock(ClassLoader.class);
        Map<String,NamedCache> mapCaches    = mockCachesForQueue(sQueueName);

        when(cacheService.getContextClassLoader()).thenReturn(loader);
        when(cacheService.ensureCache(anyString(), same(loader)))
                .thenAnswer(invocation ->
                            {
                            String sCacheName = String.valueOf(invocation.getArguments()[0]);
                            return mapCaches.get(sCacheName);
                            });

        PagedTopicCaches pagedTopicCaches = new PagedTopicCaches("Foo", cacheService);

        pagedTopicCaches.close();

        assertThat(pagedTopicCaches.f_setCaches, is(nullValue()));

        for (NamedCache cache : mapCaches.values())
            {
            verify(cache).release();
            }
        }

    @Test
    public void shouldBeActiveIfDPageCacheIsActive() throws Exception
        {
        String                 sQueueName   = "Foo";
        CacheService           cacheService = mock(CacheService.class);
        ClassLoader            loader       = mock(ClassLoader.class);
        Map<String,NamedCache> mapCaches    = mockCachesForQueue(sQueueName);

        when(mapCaches.get(PagedTopicCaches.Names.PAGES.cacheNameForTopicName(sQueueName)).isActive()).thenReturn(true);

        when(cacheService.getContextClassLoader()).thenReturn(loader);
        when(cacheService.ensureCache(anyString(), same(loader)))
                .thenAnswer(invocation ->
                            {
                            String sCacheName = String.valueOf(invocation.getArguments()[0]);
                            return mapCaches.get(sCacheName);
                            });

        PagedTopicCaches pagedTopicCaches = new PagedTopicCaches("Foo", cacheService);

        assertThat(pagedTopicCaches.isActive(), is(true));
        }

    @Test
    public void shouldBeInactiveIfPageCacheIsInctive() throws Exception
        {
        String                 sQueueName   = "Foo";
        CacheService           cacheService = mock(CacheService.class);
        ClassLoader            loader       = mock(ClassLoader.class);
        Map<String,NamedCache> mapCaches    = mockCachesForQueue(sQueueName);

        when(mapCaches.get(PagedTopicCaches.Names.PAGES.cacheNameForTopicName(sQueueName)).isActive()).thenReturn(false);
        when(cacheService.getContextClassLoader()).thenReturn(loader);
        when(cacheService.ensureCache(anyString(), same(loader)))
                .thenAnswer(invocation ->
                            {
                            String sCacheName = String.valueOf(invocation.getArguments()[0]);
                            return mapCaches.get(sCacheName);
                            });

        PagedTopicCaches pagedTopicCaches = new PagedTopicCaches("Foo", cacheService);

        assertThat(pagedTopicCaches.isActive(), is(false));
        }


    protected Map<String,NamedCache> mockCachesForQueue(String sQueueName)
        {
        Map<String,NamedCache> mapCaches    = new HashMap<>();

        for (PagedTopicCaches.Names name : PagedTopicCaches.Names.values())
            {
            String sName = name.cacheNameForTopicName(sQueueName);
            mapCaches.put(sName, mock(NamedCache.class, sName));
            }

        return mapCaches;
        }

    @Rule
    public ExpectedException m_expectedException = ExpectedException.none();
    }
