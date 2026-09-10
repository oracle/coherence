/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.subscribers.internal;

import java.time.Duration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link FutureSubscriber} completion waits.
 */
public class FutureSubscriberTest
    {
    @AfterEach
    public void cleanup()
        {
        if (m_executor != null)
            {
            m_executor.shutdownNow();
            }
        }

    @Test
    public void shouldWaitForTaskCompletionAfterReceivingResult()
            throws Exception
        {
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        subscriber.onNext("result");

        Future<String> future = executor().submit(() ->
            {
            return subscriber.get();
            });
        Thread.sleep(50L);

        assertFalse(future.isDone());
        subscriber.onComplete();
        assertEquals("result", future.get(1, TimeUnit.SECONDS));
        }

    @Test
    public void shouldIgnoreSpuriousNotificationWhileWaiting()
            throws Exception
        {
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        Future<String> future = executor().submit(() -> subscriber.get(1, TimeUnit.SECONDS));

        synchronized (subscriber)
            {
            subscriber.notifyAll();
            }

        Thread.sleep(50L);
        assertFalse(future.isDone());

        subscriber.onNext("result");
        subscriber.onComplete();
        assertEquals("result", future.get(1, TimeUnit.SECONDS));
        }

    @Test
    public void shouldHonorTimeoutAfterSpuriousNotification()
            throws Exception
        {
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        CountDownLatch ready = new CountDownLatch(1);

        Future<Long> future = executor().submit(() ->
            {
            long started = System.nanoTime();
            ready.countDown();
            assertThrows(TimeoutException.class,
                    () -> subscriber.get(200, TimeUnit.MILLISECONDS));
            return Duration.ofNanos(System.nanoTime() - started).toMillis();
            });

        assertTrue(ready.await(1, TimeUnit.SECONDS));
        Thread.sleep(50L);
        synchronized (subscriber)
            {
            subscriber.notifyAll();
            }

        assertTrue(future.get(1, TimeUnit.SECONDS) >= 150L);
        }

    @Test
    public void shouldCompleteExceptionally()
        {
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        IllegalStateException expected = new IllegalStateException("expected");

        subscriber.onError(expected);

        ExecutionException error = assertThrows(ExecutionException.class, subscriber::get);
        assertEquals(expected, error.getCause());
        }

    private ExecutorService executor()
        {
        if (m_executor == null)
            {
            m_executor = Executors.newSingleThreadExecutor();
            }
        return m_executor;
        }

    private ExecutorService m_executor;
    }
