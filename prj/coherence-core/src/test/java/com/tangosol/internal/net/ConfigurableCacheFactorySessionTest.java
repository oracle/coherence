/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import com.oracle.coherence.common.base.Classes;

import com.tangosol.net.Coherence;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.net.options.WithClassLoader;

import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight  2020.10.26
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ConfigurableCacheFactorySessionTest
    {
    @Test
    public void shouldGetCacheWithSessionClassLoader()
        {
        ClassLoader              loader   = mock(ClassLoader.class);
        ConfigurableCacheFactory ccf      = mock(ConfigurableCacheFactory.class);
        ResourceRegistry         registry = new SimpleResourceRegistry();
        NamedCache               cache    = mock(NamedCache.class);

        when(ccf.getResourceRegistry()).thenReturn(registry);
        when(ccf.ensureTypedCache(anyString(), any(ClassLoader.class), any(TypeAssertion.class))).thenReturn(cache);

        ConfigurableCacheFactorySession session  = new ConfigurableCacheFactorySession(ccf, loader);
        session.activate(Coherence.Mode.Client);
        session.getCache("foo");

        verify(ccf).ensureTypedCache(eq("foo"), same(loader), any(TypeAssertion.class));
        }

    @Test
    public void shouldGetCacheWithOptionClassLoader()
        {
        ClassLoader              loaderSession = mock(ClassLoader.class);
        ClassLoader              loaderOption  = mock(ClassLoader.class);
        ConfigurableCacheFactory ccf           = mock(ConfigurableCacheFactory.class);
        ResourceRegistry         registry      = new SimpleResourceRegistry();
        NamedCache               cache         = mock(NamedCache.class);

        when(ccf.getResourceRegistry()).thenReturn(registry);
        when(ccf.ensureTypedCache(anyString(), any(ClassLoader.class), any(TypeAssertion.class))).thenReturn(cache);

        ConfigurableCacheFactorySession session       = new ConfigurableCacheFactorySession(ccf, loaderSession);
        session.activate(Coherence.Mode.Client);
        session.getCache("foo", WithClassLoader.using(loaderOption));

        verify(ccf).ensureTypedCache(eq("foo"), same(loaderOption), any(TypeAssertion.class));
        }

    @Test
    public void shouldGetCacheWithContextClassLoader()
        {
        ClassLoader              loaderSession = mock(ClassLoader.class);
        ConfigurableCacheFactory ccf           = mock(ConfigurableCacheFactory.class);
        ResourceRegistry         registry      = new SimpleResourceRegistry();
        NamedCache               cache         = mock(NamedCache.class);

        when(ccf.getResourceRegistry()).thenReturn(registry);
        when(ccf.ensureTypedCache(anyString(), any(ClassLoader.class), any(TypeAssertion.class))).thenReturn(cache);

        ConfigurableCacheFactorySession session = new ConfigurableCacheFactorySession(ccf, loaderSession);
        session.activate(Coherence.Mode.Client);
        ClassLoader loaderCtx = Classes.getContextClassLoader();
        session.getCache("foo", WithClassLoader.autoDetect());

        verify(ccf).ensureTypedCache(eq("foo"), same(loaderCtx), any(TypeAssertion.class));
        }

    @Test
    public void shouldGetTopicWithSessionClassLoader()
        {
        ClassLoader              loader   = mock(ClassLoader.class);
        ConfigurableCacheFactory ccf      = mock(ConfigurableCacheFactory.class);
        ResourceRegistry         registry = new SimpleResourceRegistry();
        NamedTopic               topic    = mock(NamedTopic.class);

        when(ccf.getResourceRegistry()).thenReturn(registry);
        when(ccf.ensureTopic(anyString(), any(ClassLoader.class), any(NamedTopic.Option.class))).thenReturn(topic);

        ConfigurableCacheFactorySession session  = new ConfigurableCacheFactorySession(ccf, loader);
        session.getTopic("foo");

        verify(ccf).ensureTopic(eq("foo"), same(loader), any(NamedTopic.Option.class));
        }

    @Test
    public void shouldGetTopicWithOptionClassLoader()
        {
        ClassLoader              loaderSession = mock(ClassLoader.class);
        ClassLoader              loaderOption  = mock(ClassLoader.class);
        ConfigurableCacheFactory ccf           = mock(ConfigurableCacheFactory.class);
        ResourceRegistry         registry      = new SimpleResourceRegistry();
        NamedTopic               topic         = mock(NamedTopic.class);

        when(ccf.getResourceRegistry()).thenReturn(registry);
        when(ccf.ensureTopic(anyString(), any(ClassLoader.class), any(NamedTopic.Option.class))).thenReturn(topic);

        ConfigurableCacheFactorySession session       = new ConfigurableCacheFactorySession(ccf, loaderSession);
        session.getTopic("foo", WithClassLoader.using(loaderOption));

        verify(ccf).ensureTopic(eq("foo"), same(loaderOption), any(NamedTopic.Option.class));
        }

    @Test
    public void shouldGetTopicWithContextClassLoader()
        {
        ClassLoader              loaderSession = mock(ClassLoader.class);
        ConfigurableCacheFactory ccf           = mock(ConfigurableCacheFactory.class);
        ResourceRegistry         registry      = new SimpleResourceRegistry();
        NamedTopic               topic         = mock(NamedTopic.class);

        when(ccf.getResourceRegistry()).thenReturn(registry);
        when(ccf.ensureTopic(anyString(), any(ClassLoader.class), any(NamedTopic.Option.class))).thenReturn(topic);

        ConfigurableCacheFactorySession session   = new ConfigurableCacheFactorySession(ccf, loaderSession);
        ClassLoader                     loaderCtx = Classes.getContextClassLoader();
        session.getTopic("foo", WithClassLoader.autoDetect());

        verify(ccf).ensureTopic(eq("foo"), same(loaderCtx), any(NamedTopic.Option.class));
        }
    }
