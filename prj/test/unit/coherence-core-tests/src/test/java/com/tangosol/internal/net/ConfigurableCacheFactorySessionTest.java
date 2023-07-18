/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import com.oracle.coherence.common.base.Classes;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.ValueTypeAssertion;
import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.EventDispatcherRegistry;
import com.tangosol.net.events.internal.Registry;

import com.tangosol.net.options.WithClassLoader;

import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
        session.activate();
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
        session.activate();
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
        session.activate();
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

    // COH-23438
    @Test
    public void shouldNotThrowOnTopicReleaseAfterCoherenceClose() throws Exception
        {
        ClassLoader              loader   = mock(ClassLoader.class);
        ConfigurableCacheFactory ccf      = mock(ConfigurableCacheFactory.class);
        ResourceRegistry         registry = new SimpleResourceRegistry();
        NamedTopic               topic    = mock(NamedTopic.class);

        when(ccf.getResourceRegistry()).thenReturn(registry);
        when(ccf.ensureTopic(anyString(), any(ClassLoader.class), any(NamedTopic.Option.class))).thenReturn(topic);
        when(topic.isActive()).thenReturn(true);
        when(topic.getName()).thenReturn("foo");

        ConfigurableCacheFactorySession session  = new ConfigurableCacheFactorySession(ccf, loader, "testSession");
        NamedTopic sessionTopic = session.getTopic("foo");
        assertNotNull(sessionTopic);

        verify(ccf).ensureTopic(eq("foo"), same(loader), any(NamedTopic.Option.class));

        session.close();

        // simulate test case that released CCF as part of Coherence.close() and then registered shutdown handler that called sessionTopic.close
        // topic close after Coherence.shutdown throws IllegalStateException when accessing disposed CCF
        when(ccf.getResourceRegistry()).thenThrow(IllegalStateException.class);
        when(topic.isReleased()).thenReturn(true);
        assertTrue(sessionTopic.isReleased());

        sessionTopic.close();
        }

    // inserted in hash map with TypeAssertion as key, must all be equal
    @Test
    public void testTypeAssertion()
        {
        assertEquals(TypeAssertion.withRawTypes(), TypeAssertion.withRawTypes());
        assertEquals(TypeAssertion.withoutTypeChecking(), TypeAssertion.withoutTypeChecking());
        assertEquals(TypeAssertion.withTypes(String.class, String.class), TypeAssertion.withTypes(String.class, String.class));
        }

    // inserted in hash map with ValueTypeAssertion as key, must all be equal
    @Test
    public void testValueTypeAssertion()
        {
        assertEquals(ValueTypeAssertion.withRawTypes(), ValueTypeAssertion.withRawTypes());
        assertEquals(ValueTypeAssertion.withoutTypeChecking(), ValueTypeAssertion.withoutTypeChecking());
        assertEquals(ValueTypeAssertion.withType(String.class), ValueTypeAssertion.withType(String.class));
        }

    @Test
    public void shouldGetSameSessionNamedTopicWithType() throws Exception
        {
        ClassLoader              loader   = mock(ClassLoader.class);
        ConfigurableCacheFactory ccf      = mock(ConfigurableCacheFactory.class);
        ResourceRegistry         registry = new SimpleResourceRegistry();
        NamedTopic               topic    = mock(NamedTopic.class);

        when(ccf.getResourceRegistry()).thenReturn(registry);
        when(ccf.ensureTopic(anyString(), any(ClassLoader.class), any(NamedTopic.Option.class))).thenReturn(topic);
        when(topic.isActive()).thenReturn(true);
        when(topic.getName()).thenReturn("foo");

        ConfigurableCacheFactorySession session  = new ConfigurableCacheFactorySession(ccf, loader, "testSession");
        NamedTopic sessionTopic = session.getTopic("foo", ValueTypeAssertion.withType(String.class));
        assertNotNull(sessionTopic);

        NamedTopic sessionTopic1 = session.getTopic("foo", ValueTypeAssertion.withType(String.class));
        assertTrue(sessionTopic == sessionTopic1);

        sessionTopic.close();
        }

    @Test
    public void shouldGetSameSessionNamedTopicWithoutTypechecking() throws Exception
        {
        ClassLoader              loader   = mock(ClassLoader.class);
        ConfigurableCacheFactory ccf      = mock(ConfigurableCacheFactory.class);
        ResourceRegistry         registry = new SimpleResourceRegistry();
        NamedTopic               topic    = mock(NamedTopic.class);

        when(ccf.getResourceRegistry()).thenReturn(registry);
        when(ccf.ensureTopic(anyString(), any(ClassLoader.class), any(NamedTopic.Option.class))).thenReturn(topic);
        when(topic.isActive()).thenReturn(true);
        when(topic.getName()).thenReturn("foo");

        ConfigurableCacheFactorySession session  = new ConfigurableCacheFactorySession(ccf, loader, "testSession");
        NamedTopic sessionTopic = session.getTopic("foo", ValueTypeAssertion.withoutTypeChecking());
        assertNotNull(sessionTopic);

        NamedTopic sessionTopic1 = session.getTopic("foo", ValueTypeAssertion.withoutTypeChecking());
        assertTrue(sessionTopic == sessionTopic1);

        sessionTopic.close();
        }

    // Default ValueTypeAssertion is withRawTypes
    @Test
    public void shouldGetSameSessionNamedTopic() throws Exception
        {
        ClassLoader              loader   = mock(ClassLoader.class);
        ConfigurableCacheFactory ccf      = mock(ConfigurableCacheFactory.class);
        ResourceRegistry         registry = new SimpleResourceRegistry();
        NamedTopic               topic    = mock(NamedTopic.class);

        when(ccf.getResourceRegistry()).thenReturn(registry);
        when(ccf.ensureTopic(anyString(), any(ClassLoader.class), any(NamedTopic.Option.class))).thenReturn(topic);
        when(topic.isActive()).thenReturn(true);
        when(topic.getName()).thenReturn("foo");

        ConfigurableCacheFactorySession session  = new ConfigurableCacheFactorySession(ccf, loader, "testSession");
        NamedTopic sessionTopic = session.getTopic("foo");
        assertNotNull(sessionTopic);

        NamedTopic sessionTopic1 = session.getTopic("foo");
        assertTrue(sessionTopic == sessionTopic1);

        sessionTopic.close();
        }

    @Test
    public void shouldNotBeSameSessionNamedTopic() throws Exception
        {
        ClassLoader              loader   = mock(ClassLoader.class);
        ConfigurableCacheFactory ccf      = mock(ConfigurableCacheFactory.class);
        ResourceRegistry         registry = new SimpleResourceRegistry();
        NamedTopic               topic    = mock(NamedTopic.class);

        when(ccf.getResourceRegistry()).thenReturn(registry);
        when(ccf.ensureTopic(anyString(), any(ClassLoader.class), any(NamedTopic.Option.class))).thenReturn(topic);
        when(topic.isActive()).thenReturn(true);
        when(topic.getName()).thenReturn("foo");

        ConfigurableCacheFactorySession session  = new ConfigurableCacheFactorySession(ccf, loader, "testSession");
        NamedTopic sessionTopic = session.getTopic("foo", ValueTypeAssertion.withType(String.class));
        assertNotNull(sessionTopic);

        NamedTopic sessionTopic1 = session.getTopic("foo");

        // different value type assertions result in two different SessionNamedTopic instances
        assertTrue(sessionTopic != sessionTopic1);

        sessionTopic.close();
        }

    @Test
    public void shouldUnregisterEventListenerOnSessionClose()
            throws Exception
        {
        ClassLoader              loaderSession       = mock(ClassLoader.class);
        ConfigurableCacheFactory eccf                = mock(ExtensibleConfigurableCacheFactory.class);
        ResourceRegistry         registry            = new SimpleResourceRegistry();
        Registry                 interceptorRegistry = mock(Registry.class);
        String                   interceptorId       = "fooInterceptor";

        registry.registerResource(EventDispatcherRegistry.class, new Registry());
        when(eccf.getResourceRegistry()).thenReturn(registry);
        when(eccf.getInterceptorRegistry()).thenReturn(interceptorRegistry);
        when(interceptorRegistry.registerEventInterceptor(any(EventDispatcherAwareInterceptor.class))).thenReturn(interceptorId);

        ConfigurableCacheFactorySession session = new ConfigurableCacheFactorySession(eccf, loaderSession);
        session.activate();
        verify(interceptorRegistry).registerEventInterceptor(any(EventDispatcherAwareInterceptor.class));

        session.close();
        verify(interceptorRegistry).unregisterEventInterceptor(interceptorId);
        }
    }
