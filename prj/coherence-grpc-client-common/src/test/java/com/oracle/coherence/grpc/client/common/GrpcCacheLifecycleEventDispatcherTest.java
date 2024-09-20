/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;

import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link GrpcCacheLifecycleEventDispatcher}.
 *
 * @since 23.03
 */
public class GrpcCacheLifecycleEventDispatcherTest
    {
    @Test
    @SuppressWarnings({"unchecked", "resource"})
    public void shouldGetSessionNameWhenNoServiceAvailable()
        {
        AsyncNamedCacheClient<String, String> async    = mock(AsyncNamedCacheClient.class);
        GrpcRemoteCacheService                service  = mock(GrpcRemoteCacheService.class);
        BackingMapManager                     bmm      = mock(BackingMapManager.class);
        ExtensibleConfigurableCacheFactory    eccf     = mock(ExtensibleConfigurableCacheFactory.class);
        ResourceRegistry                      registry = new SimpleResourceRegistry();

        registry.registerResource(String.class, ConfigurableCacheFactorySession.SESSION_NAME, "foo");

        when(service.getBackingMapManager()).thenReturn(bmm);
        when(bmm.getCacheFactory()).thenReturn(eccf);
        when(eccf.getResourceRegistry()).thenReturn(registry);

        GrpcCacheLifecycleEventDispatcher dispatcher =
                new GrpcCacheLifecycleEventDispatcher("test", service);
        GrpcCacheLifecycleEventDispatcher.GrpcCacheLifecycleEvent event =
                new GrpcCacheLifecycleEventDispatcher.GrpcCacheLifecycleEvent(
                        dispatcher, CacheLifecycleEvent.Type.CREATED, async.getNamedCache());

        // this call should fail if the issue persists
        assertThat(event.getSessionName(), is("foo"));
        }
    }
