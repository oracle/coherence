/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import com.oracle.coherence.common.base.Blocking;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ConcurrentKeyLock}.
 *
 * @author Aleks Seovic  2026.04.10
 * @since 26.04
 */
public class ConcurrentKeyLockTest
    {
    @Test
    public void shouldSupportReentrantPerKeyLocking()
        {
        ConcurrentKeyLock<String, String> map = new ConcurrentKeyLock<>();

        assertTrue(map.lock("key", -1L));
        assertTrue(map.lock("key", 0L));
        assertEquals(1, map.getLockEntryCount());

        assertTrue(map.unlock("key"));
        assertEquals(1, map.getLockEntryCount());

        assertTrue(map.unlock("key"));
        assertEquals(0, map.getLockEntryCount());
        }

    @Test
    public void shouldFailUnlockByNonOwner() throws Exception
        {
        ConcurrentKeyLock<String, String> map = new ConcurrentKeyLock<>();

        assertTrue(map.lock("key", -1L));

        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> map.unlock("key"));

        assertFalse(future.get());
        assertTrue(map.unlock("key"));
        assertEquals(0, map.getLockEntryCount());
        }

    @Test
    public void shouldTimeOutAndCleanUpAfterContention() throws Exception
        {
        CountDownLatch                                 latchContend   = new CountDownLatch(1);
        CountDownLatch                                 latchUncontend = new CountDownLatch(1);
        AtomicReference<Thread>                        refContender   = new AtomicReference<>();
        AtomicReference<ConcurrentKeyLock.LockState>   refState       = new AtomicReference<>();
        ConcurrentKeyLock<String, String>              map            = new ConcurrentKeyLock<>(new ConcurrentKeyLock.ContentionObserver<String>()
            {
            @Override
            public void onContend(Thread thread, ConcurrentKeyLock<String, ?>.LockState state)
                {
                refContender.set(thread);
                refState.set(state);
                latchContend.countDown();
                }

            @Override
            public void onUncontend(Thread thread, ConcurrentKeyLock<String, ?>.LockState state)
                {
                latchUncontend.countDown();
                }
            });

        assertTrue(map.lock("key", -1L));

        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> map.lock("key", 100L));

        assertTrue(latchContend.await(5, TimeUnit.SECONDS));
        assertNotNull(refContender.get());
        assertNotNull(refState.get());
        assertSame(Thread.currentThread(), refState.get().getLockHolder());
        assertEquals("key", refState.get().getKey());
        assertTrue(refState.get().isContended());

        assertFalse(future.get());
        assertTrue(latchUncontend.await(5, TimeUnit.SECONDS));

        assertTrue(map.unlock("key"));
        assertEquals(0, map.getLockEntryCount());
        }

    @Test
    public void shouldBlockKeyLockWhileLockAllHeld() throws Exception
        {
        ConcurrentKeyLock<String, String> map = new ConcurrentKeyLock<>();

        assertTrue(map.lock(ConcurrentMap.LOCK_ALL, -1L));

        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> map.lock("key", 100L));

        assertFalse(future.get());
        assertTrue(map.unlock(ConcurrentMap.LOCK_ALL));

        assertTrue(map.lock("key", 0L));
        assertTrue(map.unlock("key"));
        }

    @Test
    public void shouldTimeOutLockAllWhileKeyHeld() throws Exception
        {
        ConcurrentKeyLock<String, String> map = new ConcurrentKeyLock<>();

        assertTrue(map.lock("key", -1L));

        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> map.lock(ConcurrentMap.LOCK_ALL, 100L));

        assertFalse(future.get());

        assertTrue(map.unlock("key"));
        assertTrue(map.lock(ConcurrentMap.LOCK_ALL, 0L));
        assertTrue(map.unlock(ConcurrentMap.LOCK_ALL));
        }

    @Test
    public void shouldNotifyObserverOncePerWait() throws Exception
        {
        AtomicInteger                         cContend   = new AtomicInteger();
        AtomicInteger                         cUncontend = new AtomicInteger();
        CountDownLatch                        latchWait  = new CountDownLatch(1);
        ConcurrentKeyLock<String, String>     map        = new ConcurrentKeyLock<>(new ConcurrentKeyLock.ContentionObserver<String>()
            {
            @Override
            public void onContend(Thread thread, ConcurrentKeyLock<String, ?>.LockState state)
                {
                cContend.incrementAndGet();
                latchWait.countDown();
                }

            @Override
            public void onUncontend(Thread thread, ConcurrentKeyLock<String, ?>.LockState state)
                {
                cUncontend.incrementAndGet();
                }
            });

        assertTrue(map.lock("key", -1L));

        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() ->
            {
            boolean fLocked = map.lock("key", -1L);
            if (fLocked)
                {
                map.unlock("key");
                }
            return fLocked;
            });

        assertTrue(latchWait.await(5, TimeUnit.SECONDS));
        assertEquals(1, cContend.get());

        assertTrue(map.unlock("key"));
        assertTrue(future.get());
        assertEquals(1, cContend.get());
        assertEquals(1, cUncontend.get());
        }

    @Test
    public void shouldAcquireAfterIndefiniteWait() throws Exception
        {
        ConcurrentKeyLock<String, String> map         = new ConcurrentKeyLock<>();
        CountDownLatch                    latchLocked = new CountDownLatch(1);

        assertTrue(map.lock("key", -1L));

        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() ->
            {
            latchLocked.countDown();
            boolean fLocked = map.lock("key", -1L);
            if (fLocked)
                {
                map.unlock("key");
                }
            return fLocked;
            });

        assertTrue(latchLocked.await(5, TimeUnit.SECONDS));
        Blocking.sleep(100L);

        assertTrue(map.unlock("key"));
        assertTrue(future.get());
        assertEquals(0, map.getLockEntryCount());
        }

    @Test
    public void shouldSupportReentrantLockAll()
        {
        ConcurrentKeyLock<String, String> map = new ConcurrentKeyLock<>();

        assertTrue(map.lock(ConcurrentMap.LOCK_ALL, -1L));
        assertTrue(map.lock(ConcurrentMap.LOCK_ALL, 0L));

        assertTrue(map.unlock(ConcurrentMap.LOCK_ALL));
        assertTrue(map.unlock(ConcurrentMap.LOCK_ALL));
        assertFalse(map.unlock(ConcurrentMap.LOCK_ALL));
        }

    @Test
    public void shouldAllowPerKeyLockWhileHoldingLockAll()
        {
        ConcurrentKeyLock<String, String> map = new ConcurrentKeyLock<>();

        assertTrue(map.lock(ConcurrentMap.LOCK_ALL, -1L));
        assertTrue(map.lock("key", 0L));

        assertTrue(map.unlock("key"));
        assertTrue(map.unlock(ConcurrentMap.LOCK_ALL));
        assertEquals(0, map.getLockEntryCount());
        }
    }
