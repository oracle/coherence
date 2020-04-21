package com.tangosol.net.internal;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.ServiceInfo;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This test is to ensure that adding and removing caches from multiple threads is safe.
 *
 * @author jk  2020.03.28
 */
public class ScopedCacheReferenceStoreTest
    {
    @Test
    public void shouldBeThreadSafeWhenReleasingCache() throws Exception
        {
        ClassLoader      loaderOne = mock(ClassLoader.class, "One");
        ClassLoader      loaderTwo = mock(ClassLoader.class, "Two");
        NamedCache<?, ?> cacheOne  = mock(NamedCache.class, "One");
        NamedCache<?, ?> cacheTwo  = mock(NamedCache.class, "Two");
        CacheService     service   = mock(CacheService.class);
        ServiceInfo      info      = mock(ServiceInfo.class);
        String           sName     = "Foo";
        CountDownLatch   latchOne  = new CountDownLatch(1);
        CountDownLatch   latchTwo  = new CountDownLatch(1);

        when(service.getInfo()).thenReturn(info);
        when(info.getServiceType()).thenReturn(CacheService.TYPE_DISTRIBUTED);
        when(cacheOne.getCacheService()).thenReturn(service);
        when(cacheOne.getCacheName()).thenReturn(sName);
        when(cacheTwo.getCacheName()).thenReturn(sName);
        when(cacheTwo.getCacheService()).thenAnswer((Answer<CacheService>) i -> {
            // This allows us to block on the call to getCacheService and so control the
            // race condition the the test is checking
            // Release latchOne to signal that we're now blocking
            latchOne.countDown();
            // Block until latchTwo is released
            latchTwo.await(1, TimeUnit.MINUTES);
            return service;
            });

        ScopedCacheReferenceStore store = new ScopedCacheReferenceStore();
        // put the first cache in the store
        Object prevOne = store.putCacheIfAbsent(cacheOne, loaderOne);
        assertThat(prevOne, is(nullValue()));

        // put the second cache in the store - this is done on another thread as it
        // will block until we complete the future
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> store.putCacheIfAbsent(cacheTwo, loaderTwo));

        // wait until the put of cacheTwo is blocking
        latchOne.await(1, TimeUnit.MINUTES);

        // At this point the only cache in the store for name Foo is cacheOne so the internal
        // map of cache by loader will be removed
        boolean fReleasedOne = store.releaseCache(cacheOne);
        assertThat(fReleasedOne, is(true));

        // Release the latch allowing the put of cacheTwo to complete
        latchTwo.countDown();
        Object prevTwo = future.get(1, TimeUnit.MINUTES);
        assertThat(prevTwo, is(nullValue()));

        // Now release cacheTwo, which should succeed.
        boolean fReleasedTwo = store.releaseCache(cacheTwo, loaderTwo);
        assertThat(fReleasedTwo, is(true));
        }

    @Test
    public void shouldBeThreadSafeWhenReleasingCacheAndLoader() throws Exception
        {
        ClassLoader      loaderOne = mock(ClassLoader.class, "One");
        ClassLoader      loaderTwo = mock(ClassLoader.class, "Two");
        NamedCache<?, ?> cacheOne  = mock(NamedCache.class, "One");
        NamedCache<?, ?> cacheTwo  = mock(NamedCache.class, "Two");
        CacheService     service   = mock(CacheService.class);
        ServiceInfo      info      = mock(ServiceInfo.class);
        String           sName     = "Foo";
        CountDownLatch   latchOne  = new CountDownLatch(1);
        CountDownLatch   latchTwo  = new CountDownLatch(1);

        when(service.getInfo()).thenReturn(info);
        when(info.getServiceType()).thenReturn(CacheService.TYPE_DISTRIBUTED);
        when(cacheOne.getCacheService()).thenReturn(service);
        when(cacheOne.getCacheName()).thenReturn(sName);
        when(cacheTwo.getCacheName()).thenReturn(sName);
        when(cacheTwo.getCacheService()).thenAnswer((Answer<CacheService>) i -> {
            // This allows us to block on the call to getCacheService and so control the
            // race condition the the test is checking
            // Release latchOne to signal that we're now blocking
            latchOne.countDown();
            // Block until latchTwo is released
            latchTwo.await(1, TimeUnit.MINUTES);
            return service;
            });

        ScopedCacheReferenceStore store = new ScopedCacheReferenceStore();
        // put the first cache in the store
        Object prevOne = store.putCacheIfAbsent(cacheOne, loaderOne);
        assertThat(prevOne, is(nullValue()));

        // put the second cache in the store - this is done on another thread as it
        // will block until we complete the future
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> store.putCacheIfAbsent(cacheTwo, loaderTwo));

        // wait until the put of cacheTwo is blocking
        latchOne.await(1, TimeUnit.MINUTES);

        // At this point the only cache in the store for name Foo is cacheOne so the internal
        // map of cache by loader will be removed
        boolean fReleasedOne = store.releaseCache(cacheOne, loaderOne);
        assertThat(fReleasedOne, is(true));

        // Release the latch allowing the put of cacheTwo to complete
        latchTwo.countDown();
        Object prevTwo = future.get(1, TimeUnit.MINUTES);
        assertThat(prevTwo, is(nullValue()));

        // Now release cacheTwo, which should succeed.
        boolean fReleasedTwo = store.releaseCache(cacheTwo, loaderTwo);
        assertThat(fReleasedTwo, is(true));
        }


    @Test
    public void shouldBeThreadSafeWhenClearingInactiveCacheRefs() throws Exception
        {
        ClassLoader      loaderOne = mock(ClassLoader.class, "One");
        ClassLoader      loaderTwo = mock(ClassLoader.class, "Two");
        NamedCache<?, ?> cacheOne  = mock(NamedCache.class, "One");
        NamedCache<?, ?> cacheTwo  = mock(NamedCache.class, "Two");
        CacheService     service   = mock(CacheService.class);
        ServiceInfo      info      = mock(ServiceInfo.class);
        String           sName     = "Foo";
        CountDownLatch   latchOne  = new CountDownLatch(1);
        CountDownLatch   latchTwo  = new CountDownLatch(1);
        AtomicBoolean    fReleased = new AtomicBoolean(false);

        when(service.getInfo()).thenReturn(info);
        when(info.getServiceType()).thenReturn(CacheService.TYPE_DISTRIBUTED);
        when(cacheOne.getCacheService()).thenReturn(service);
        when(cacheOne.getCacheName()).thenReturn(sName);
        when(cacheOne.isReleased()).thenAnswer((Answer<Boolean>) invocationOnMock -> fReleased.get());
        when(cacheTwo.getCacheName()).thenReturn(sName);
        when(cacheTwo.getCacheService()).thenAnswer((Answer<CacheService>) i -> {
        // This allows us to block on the call to getCacheService and so control the
        // race condition the the test is checking
        // Release latchOne to signal that we're now blocking
        latchOne.countDown();
        // Block until latchTwo is released
        latchTwo.await(1, TimeUnit.MINUTES);
        return service;
        });

        ScopedCacheReferenceStore store = new ScopedCacheReferenceStore();
        // put the first cache in the store
        Object prevOne = store.putCacheIfAbsent(cacheOne, loaderOne);
        assertThat(prevOne, is(nullValue()));

        // put the second cache in the store - this is done on another thread as it
        // will block until we complete the future
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> store.putCacheIfAbsent(cacheTwo, loaderTwo));

        // wait until the put of cacheTwo is blocking
        latchOne.await(1, TimeUnit.MINUTES);

        // At this point the only cache in the store for name Foo is cacheOne so the internal
        // map of cache by loader will be removed
        fReleased.set(true);
        store.clearInactiveCacheRefs();

        // Release the latch allowing the put of cacheTwo to complete
        latchTwo.countDown();
        Object prevTwo = future.get(1, TimeUnit.MINUTES);
        assertThat(prevTwo, is(nullValue()));

        // Now release cacheTwo, which should succeed.
        boolean fReleasedTwo = store.releaseCache(cacheTwo, loaderTwo);
        assertThat(fReleasedTwo, is(true));
        }
    }
