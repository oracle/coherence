/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.component.util.safeService.SafeCacheService;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.AsyncNamedMap;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Regression tests for async cache recovery after a failed restart.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SafeAsyncNamedCacheTest
    {
    @Before
    public void setup()
        {
        m_serviceSafe = mock(SafeCacheService.class);
        m_service     = mock(CacheService.class);
        m_cache       = mock(NamedCache.class);
        m_delegate    = mock(AsyncNamedCache.class);
        m_processor   = mock(InvocableMap.EntryProcessor.class);
        m_options     = new AsyncNamedMap.Option[] {AsyncNamedMap.OrderBy.none()};

        when(m_serviceSafe.isRunning()).thenReturn(true);
        when(m_serviceSafe.getRunningService()).thenReturn(m_service);
        when(m_cache.isActive()).thenReturn(true);
        when(m_cache.async(m_options)).thenReturn(m_delegate);
        when(m_delegate.getNamedCache()).thenReturn(m_cache);

        m_cacheSafe = new SafeNamedCache();
        m_cacheSafe.setCacheName(CACHE_NAME);
        m_cacheSafe.setClassLoader(getClass().getClassLoader());
        m_cacheSafe.setSafeCacheService(m_serviceSafe);
        m_cacheSafe.setInternalNamedCache(m_cache);
        m_cacheSafe.setStarted(true);

        m_cacheAsync = new SafeAsyncNamedCache();
        m_cacheAsync.setCacheName(CACHE_NAME);
        m_cacheAsync.setSafeNamedCache(m_cacheSafe);
        m_cacheAsync.setSafeCacheService(m_serviceSafe);
        m_cacheAsync.setOptions(m_options);
        m_cacheAsync.setInternalNamedCache(m_delegate);
        m_cacheAsync.setStarted(true);
        }

    @Test
    public void shouldPreserveRestartFailureOnRepeatedInvocations()
        {
        IllegalStateException failure = new IllegalStateException("SafeService was explicitly stopped");
        when(m_serviceSafe.isRunning()).thenReturn(false);
        when(m_serviceSafe.getRunningService()).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> m_cacheAsync.invoke("key", m_processor)));
        assertNull(m_cacheAsync.getInternalNamedCache());
        assertNull(m_cacheSafe.getInternalNamedCache());

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> m_cacheAsync.invokeAll(Collections.singleton("key"), m_processor)));
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> m_cacheAsync.invoke("key", m_processor)));
        verify(m_serviceSafe, times(3)).getRunningService();
        }

    @Test
    public void shouldRecoverAfterFailedRestart()
        {
        IllegalStateException failure = new IllegalStateException("restart failed");
        when(m_serviceSafe.isRunning()).thenReturn(false);
        when(m_serviceSafe.getRunningService()).thenThrow(failure).thenReturn(m_service);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> m_cacheAsync.invoke("key", m_processor)));
        assertNull(m_cacheAsync.getInternalNamedCache());

        NamedCache        cacheNew    = mock(NamedCache.class);
        AsyncNamedCache   delegateNew = mock(AsyncNamedCache.class);
        CompletableFuture result      = CompletableFuture.completedFuture("recovered");
        when(m_serviceSafe.isRunning()).thenReturn(true);
        when(m_service.ensureCache(CACHE_NAME, getClass().getClassLoader())).thenReturn(cacheNew);
        when(cacheNew.isActive()).thenReturn(true);
        when(cacheNew.async(m_options)).thenReturn(delegateNew);
        when(delegateNew.getNamedCache()).thenReturn(cacheNew);
        when(delegateNew.invoke("key", m_processor)).thenReturn(result);

        assertSame(result, m_cacheAsync.invoke("key", m_processor));
        assertSame(cacheNew, m_cacheSafe.getInternalNamedCache());
        assertSame(delegateNew, m_cacheAsync.getInternalNamedCache());
        assertSame(delegateNew, m_cacheAsync.ensureRunningNamedCache());
        verify(cacheNew).async(m_options);
        verify(m_service).ensureCache(CACHE_NAME, getClass().getClassLoader());
        }

    @Test
    public void shouldRebuildMissingAsyncDelegate()
        {
        m_cacheAsync.setInternalNamedCache(null);

        assertSame(m_delegate, m_cacheAsync.ensureRunningNamedCache());
        assertSame(m_delegate, m_cacheAsync.getInternalNamedCache());
        verify(m_cache).async(m_options);
        verifyNoInteractions(m_service);
        }

    @Test
    public void shouldRejectReleasedCacheWithExistingDelegate()
        {
        assertRejectedCache("released", false);
        }

    @Test
    public void shouldRejectReleasedCacheWithMissingDelegate()
        {
        assertRejectedCache("released", true);
        }

    @Test
    public void shouldRejectDestroyedCacheWithExistingDelegate()
        {
        assertRejectedCache("destroyed", false);
        }

    @Test
    public void shouldRejectDestroyedCacheWithMissingDelegate()
        {
        assertRejectedCache("destroyed", true);
        }

    // ----- helpers --------------------------------------------------------

    private void assertRejectedCache(String sReason, boolean fMissingDelegate)
        {
        if ("destroyed".equals(sReason))
            {
            m_cacheSafe.destroy();
            }
        else
            {
            m_cacheSafe.release();
            }

        when(m_cache.isActive()).thenReturn(false);
        if (fMissingDelegate)
            {
            m_cacheAsync.setInternalNamedCache(null);
            }

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                m_cacheAsync::ensureRunningNamedCache);
        assertEquals("SafeAsyncNamedCache was explicitly " + sReason, failure.getMessage());
        verifyNoInteractions(m_service);
        }

    // ----- data members ---------------------------------------------------

    private static final String CACHE_NAME = "safe-async-restart";

    private SafeCacheService m_serviceSafe;

    private CacheService m_service;

    private NamedCache m_cache;

    private AsyncNamedCache m_delegate;

    private InvocableMap.EntryProcessor m_processor;

    private AsyncNamedMap.Option[] m_options;

    private SafeNamedCache m_cacheSafe;

    private SafeAsyncNamedCache m_cacheAsync;
    }
