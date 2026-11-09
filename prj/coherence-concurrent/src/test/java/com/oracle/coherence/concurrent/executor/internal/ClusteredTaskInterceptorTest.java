/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor.internal;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;

import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.internal.ServiceDispatcher;
import com.tangosol.net.events.internal.StorageDispatcher;
import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.PartitionedCacheDispatcher;

import com.tangosol.util.SimpleResourceRegistry;

import org.junit.jupiter.api.Test;

import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests task interceptor initialization during service recovery.
 *
 * @author phf  2026.10.19
 * @since 26.10
 */
public class ClusteredTaskInterceptorTest
    {
    @Test
    public void shouldConstructWithoutLookingUpService()
        {
        try (MockedStatic<CacheFactory> cacheFactory = mockStatic(CacheFactory.class))
            {
            ClusteredTaskInterceptor interceptor = new ClusteredTaskInterceptor("Concurrent");
            try
                {
                assertNull(interceptor.m_sequences);
                assertNull(interceptor.f_atomicPartsPending.get());
                cacheFactory.verifyNoInteractions();
                }
            finally
                {
                interceptor.f_executorService.shutdownNow();
                }
            }
        }

    @Test
    public void shouldInitializeBeforeRegisteringForCacheEvents()
        {
        ClusteredTaskInterceptor interceptor = new ClusteredTaskInterceptor("Concurrent");
        try (MockedStatic<CacheFactory> cacheFactory = mockStatic(CacheFactory.class))
            {
            PartitionedCacheDispatcher dispatcher = mock(PartitionedCacheDispatcher.class);
            DistributedCacheService service = service();
            when(dispatcher.getCacheService()).thenReturn(service);
            doAnswer(invocation ->
                {
                assertEquals(257, interceptor.m_sequences.length());
                assertEquals(257, interceptor.f_atomicPartsPending.get().getPartitionCount());
                assertTrue(interceptor.f_atomicPartsPending.get().isEmpty());
                assertEquals(0L, interceptor.getSequence(256));
                return null;
                }).when(dispatcher).addEventInterceptor("task", interceptor);

            interceptor.introduceEventDispatcher("task", dispatcher);
            verify(dispatcher).addEventInterceptor("task", interceptor);
            cacheFactory.verifyNoInteractions();
            }
        finally
            {
            interceptor.f_executorService.shutdownNow();
            }
        }

    @Test
    public void shouldSubscribeToBothDispatchersWithoutResettingState()
        {
        ClusteredTaskInterceptor interceptor = new ClusteredTaskInterceptor("Concurrent");
        try
            {
            DistributedCacheService service = service();
            ServiceDispatcher serviceDispatcher = new ServiceDispatcher(service);
            BackingMapContext context = mock(BackingMapContext.class);
            BackingMapManagerContext managerContext = mock(BackingMapManagerContext.class);
            when(context.getCacheName()).thenReturn("executor-tasks");
            when(context.getManagerContext()).thenReturn(managerContext);
            when(managerContext.getCacheService()).thenReturn(service);
            StorageDispatcher cacheDispatcher = new StorageDispatcher(context);

            interceptor.introduceEventDispatcher("task", serviceDispatcher);
            assertEquals(1L, interceptor.getNextSequence(256));
            interceptor.addPending(7);
            interceptor.introduceEventDispatcher("task", cacheDispatcher);

            assertTrue(serviceDispatcher.isSubscribed(TransactionEvent.Type.COMMITTING));
            assertTrue(serviceDispatcher.isSubscribed(TransferEvent.Type.RECOVERED));
            assertTrue(cacheDispatcher.isSubscribed(EntryEvent.Type.INSERTING));
            assertEquals(1L, interceptor.getSequence(256));
            assertTrue(interceptor.f_atomicPartsPending.get().contains(7));
            }
        finally
            {
            interceptor.f_executorService.shutdownNow();
            }
        }

    @Test
    public void shouldRetainPartitionStateWhenServiceRestarts()
        {
        ClusteredTaskInterceptor interceptor = new ClusteredTaskInterceptor("Concurrent");
        try
            {
            interceptor.introduceEventDispatcher("task", new ServiceDispatcher(service()));
            assertEquals(1L, interceptor.getNextSequence(256));
            interceptor.addPending(7);

            ServiceDispatcher restarted = new ServiceDispatcher(service());
            interceptor.introduceEventDispatcher("task", restarted);

            assertTrue(restarted.isSubscribed(TransferEvent.Type.RECOVERED));
            assertEquals(1L, interceptor.getSequence(256));
            assertTrue(interceptor.f_atomicPartsPending.get().contains(7));
            }
        finally
            {
            interceptor.f_executorService.shutdownNow();
            }
        }

    @Test
    public void shouldIgnoreUnrelatedDispatchers()
        {
        ClusteredTaskInterceptor interceptor = new ClusteredTaskInterceptor("Concurrent");
        try
            {
            EventDispatcher dispatcher = mock(EventDispatcher.class);
            interceptor.introduceEventDispatcher("task", dispatcher);
            verifyNoInteractions(dispatcher);
            assertNull(interceptor.m_sequences);
            }
        finally
            {
            interceptor.f_executorService.shutdownNow();
            }
        }

    @Test
    public void shouldLookUpCurrentServiceForDeferredWork()
        {
        ClusteredTaskInterceptor interceptor = new ClusteredTaskInterceptor("Concurrent");
        try (MockedStatic<CacheFactory> cacheFactory = mockStatic(CacheFactory.class))
            {
            Cluster cluster = mock(Cluster.class);
            DistributedCacheService first = mock(DistributedCacheService.class);
            DistributedCacheService restarted = mock(DistributedCacheService.class);
            cacheFactory.when(CacheFactory::getCluster).thenReturn(cluster);
            when(cluster.getService("Concurrent")).thenReturn(first, restarted);

            assertSame(first, interceptor.getCacheService());
            assertSame(restarted, interceptor.getCacheService());
            }
        finally
            {
            interceptor.f_executorService.shutdownNow();
            }
        }

    private static DistributedCacheService service()
        {
        DistributedCacheService service = mock(DistributedCacheService.class);
        BackingMapManager manager = mock(BackingMapManager.class);
        ConfigurableCacheFactory factory = mock(ConfigurableCacheFactory.class);
        when(service.getPartitionCount()).thenReturn(257);
        when(service.getBackingMapManager()).thenReturn(manager);
        when(manager.getCacheFactory()).thenReturn(factory);
        when(factory.getResourceRegistry()).thenReturn(new SimpleResourceRegistry());
        return service;
        }
    }
