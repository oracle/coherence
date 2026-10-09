/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.subscribers.internal;

import com.oracle.coherence.concurrent.executor.Task;

import java.time.Duration;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FutureSubscriber} completion waits.
 *
 * @author Aleks Seovic  2026.09.02
 * @since 26.10
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

    @Test
    @SuppressWarnings("unchecked")
    public void shouldCompleteCancellationBeforeCoordinatorNotification()
        {
        Task.Coordinator<String> coordinator = mock(Task.Coordinator.class);
        when(coordinator.cancel(true)).thenReturn(true);
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        subscriber.setCoordinator(coordinator);

        // withhold the asynchronous coordinator notification until after get
        assertTrue(subscriber.cancel(true));
        assertThrows(CancellationException.class, () -> subscriber.get(0, TimeUnit.MILLISECONDS));
        assertTrue(subscriber.isDone());
        assertTrue(subscriber.isCancelled());
        assertThrows(CancellationException.class, subscriber::get);

        subscriber.onError(new InterruptedException("delayed cancellation"));
        subscriber.onNext("late result");
        subscriber.onComplete();
        assertThrows(CancellationException.class, subscriber::get);
        assertFalse(subscriber.cancel(true));
        verify(coordinator).cancel(true);
        }

    @Test
    public void shouldCancelBeforeSubmission()
        {
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();

        assertFalse(subscriber.isCancelled());
        assertTrue(subscriber.cancel(false));
        assertTrue(subscriber.isDone());
        assertTrue(subscriber.isCancelled());
        assertFalse(subscriber.cancel(true));
        assertThrows(CancellationException.class, subscriber::get);
        assertThrows(CancellationException.class, () -> subscriber.get(0, TimeUnit.MILLISECONDS));
        }

    @Test
    public void shouldRemainPendingWhenCoordinatorRefusesCancellation()
            throws Exception
        {
        Task.Coordinator<String> coordinator = coordinator();
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        subscriber.setCoordinator(coordinator);

        assertFalse(subscriber.cancel(false));
        assertFalse(subscriber.isDone());
        assertFalse(subscriber.isCancelled());
        assertThrows(TimeoutException.class, () -> subscriber.get(0, TimeUnit.MILLISECONDS));

        subscriber.onNext("result");
        subscriber.onComplete();
        assertEquals("result", subscriber.get());
        verify(coordinator).cancel(false);
        }

    @Test
    public void shouldPreserveCompletedResult()
            throws Exception
        {
        Task.Coordinator<String> coordinator = coordinator();
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        subscriber.setCoordinator(coordinator);
        subscriber.onNext("result");
        subscriber.onComplete();

        assertFalse(subscriber.cancel(true));
        subscriber.onError(new IllegalStateException("late error"));
        subscriber.onNext("late result");
        assertEquals("result", subscriber.get(0, TimeUnit.MILLISECONDS));
        assertFalse(subscriber.isCancelled());
        verifyNoInteractions(coordinator);
        }

    @Test
    public void shouldPreserveTaskFailure()
        {
        Task.Coordinator<String> coordinator = coordinator();
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        subscriber.setCoordinator(coordinator);
        CancellationException failure = new CancellationException("thrown by the task");
        subscriber.onError(failure);

        assertFalse(subscriber.cancel(true));
        subscriber.onNext("late result");
        subscriber.onComplete();
        assertFalse(subscriber.isCancelled());
        assertEquals(failure, assertThrows(ExecutionException.class, subscriber::get).getCause());
        }

    @Test
    public void shouldReportCoordinatorCancellation()
        {
        Task.Coordinator<String> coordinator = coordinator();
        when(coordinator.isCancelled()).thenReturn(true);
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        subscriber.setCoordinator(coordinator);

        subscriber.onError(new InterruptedException("cancelled remotely"));

        assertTrue(subscriber.isCancelled());
        assertTrue(subscriber.isDone());
        assertFalse(subscriber.getCompleted());
        assertThrows(CancellationException.class, subscriber::get);
        assertThrows(CancellationException.class, () -> subscriber.get(0, TimeUnit.MILLISECONDS));
        }

    @Test
    public void shouldHandleCancellationNotificationBeforeCancelReturns()
        {
        Task.Coordinator<String> coordinator = coordinator();
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        subscriber.setCoordinator(coordinator);
        when(coordinator.cancel(true)).thenAnswer(invocation ->
            {
            when(coordinator.isCancelled()).thenReturn(true);
            subscriber.onError(new InterruptedException("cancelled"));
            return true;
            });

        assertTrue(subscriber.cancel(true));
        assertTrue(subscriber.isDone());
        assertThrows(CancellationException.class, subscriber::get);
        }

    @Test
    public void shouldKeepCompletionWhenCancellationDecisionArrivesLater()
            throws Exception
        {
        Task.Coordinator<String> coordinator = coordinator();
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        subscriber.setCoordinator(coordinator);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(coordinator.cancel(true)).thenAnswer(invocation ->
            {
            entered.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return true;
            });

        Future<Boolean> cancellation = executor().submit(() -> subscriber.cancel(true));
        try
            {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            subscriber.onNext("result");
            subscriber.onComplete();
            assertEquals("result", subscriber.get(0, TimeUnit.MILLISECONDS));
            }
        finally
            {
            release.countDown();
            }
        assertFalse(cancellation.get(5, TimeUnit.SECONDS));
        assertFalse(subscriber.isCancelled());
        assertEquals("result", subscriber.get());
        }

    @Test
    public void shouldWakeTimedAndUntimedGetOnCancellation()
            throws Exception
        {
        CountDownLatch ready = new CountDownLatch(2);
        Thread caller = Thread.currentThread();
        FutureSubscriber<String> subscriber = new FutureSubscriber<>()
            {
            @Override
            public boolean isDone()
                {
                boolean done = super.isDone();
                if (!done && Thread.currentThread() != caller)
                    {
                    // get checks this under the subscriber monitor immediately before waiting
                    ready.countDown();
                    }
                return done;
                }
            };
        m_executor = Executors.newFixedThreadPool(2);
        Future<?> untimed = m_executor.submit(() ->
            {
            assertThrows(CancellationException.class, subscriber::get);
            });
        Future<?> timed = m_executor.submit(() ->
            {
            assertThrows(CancellationException.class, () -> subscriber.get(30, TimeUnit.SECONDS));
            });

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        assertTrue(subscriber.cancel(true));
        untimed.get(5, TimeUnit.SECONDS);
        timed.get(5, TimeUnit.SECONDS);
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldCancelUsingSubscriptionCoordinator()
        {
        Task.Coordinator<String> coordinator = coordinator();
        Task.Subscription<String> subscription = mock(Task.Subscription.class);
        when(subscription.getCoordinator()).thenReturn(coordinator);
        when(coordinator.cancel(false)).thenReturn(true);
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        subscriber.onSubscribe(subscription);

        assertTrue(subscriber.cancel(false));
        assertTrue(subscriber.isCancelled());
        assertFalse(subscriber.isSubscribed());
        assertThrows(CancellationException.class, subscriber::get);
        verify(coordinator).cancel(false);
        }

    @Test
    public void shouldRejectSubscriptionAfterCancellation()
        {
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        Task.Subscription<String> subscription = mock(Task.Subscription.class);

        assertTrue(subscriber.cancel(false));
        assertThrows(UnsupportedOperationException.class, () -> subscriber.onSubscribe(subscription));
        assertFalse(subscriber.isSubscribed());
        verifyNoInteractions(subscription);
        }

    @Test
    public void shouldRejectCoordinatorAfterLocalCancellation()
        {
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        Task.Coordinator<String> coordinator = coordinator();

        assertTrue(subscriber.cancel(false));
        assertThrows(UnsupportedOperationException.class, () -> subscriber.setCoordinator(coordinator));
        verifyNoInteractions(coordinator);
        }

    @Test
    public void shouldAllowSameCoordinatorAfterSubscriptionCompletes()
            throws Exception
        {
        Task.Coordinator<String> coordinator = coordinator();
        Task.Subscription<String> subscription = mock(Task.Subscription.class);
        when(subscription.getCoordinator()).thenReturn(coordinator);
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        subscriber.onSubscribe(subscription);
        subscriber.onNext("result");
        subscriber.onComplete();

        // submission can return its coordinator after callbacks have already finished
        subscriber.setCoordinator(coordinator);
        assertEquals("result", subscriber.get(0, TimeUnit.MILLISECONDS));
        verifyNoInteractions(coordinator);
        }

    @Test
    public void shouldUseSubscriptionPublishedBeforeCancellationDecision()
            throws Exception
        {
        Task.Coordinator<String> coordinator = coordinator();
        Task.Subscription<String> subscription = mock(Task.Subscription.class);
        when(subscription.getCoordinator()).thenReturn(coordinator);
        when(coordinator.cancel(false)).thenReturn(true);
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        AtomicReference<Boolean> cancelled = new AtomicReference<>();
        Thread thread = new Thread(() -> cancelled.set(subscriber.cancel(false)), "future-cancel-publication");

        try
            {
            synchronized (subscriber)
                {
                thread.start();
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (thread.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline)
                    {
                    Thread.onSpinWait();
                    }
                assertEquals(Thread.State.BLOCKED, thread.getState());

                // publish while cancellation is blocked on the subscriber monitor
                subscriber.onSubscribe(subscription);
                }
            thread.join(TimeUnit.SECONDS.toMillis(5));
            assertFalse(thread.isAlive());
            assertEquals(Boolean.TRUE, cancelled.get());
            assertTrue(subscriber.isCancelled());
            assertFalse(subscriber.isSubscribed());
            verify(coordinator).cancel(false);
            }
        finally
            {
            thread.interrupt();
            thread.join(TimeUnit.SECONDS.toMillis(5));
            }
        }

    @Test
    public void shouldHandleConcurrentCancellationRequests()
            throws Exception
        {
        Task.Coordinator<String> coordinator = coordinator();
        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        subscriber.setCoordinator(coordinator);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger attempts = new AtomicInteger();
        when(coordinator.cancel(true)).thenAnswer(invocation ->
            {
            if (attempts.incrementAndGet() == 1)
                {
                entered.countDown();
                assertTrue(release.await(5, TimeUnit.SECONDS));
                return true;
                }
            return false;
            });

        Future<Boolean> first = executor().submit(() -> subscriber.cancel(true));
        try
            {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertFalse(subscriber.cancel(true));
            }
        finally
            {
            release.countDown();
            }
        assertTrue(first.get(5, TimeUnit.SECONDS));
        assertTrue(subscriber.isDone());
        assertThrows(CancellationException.class, subscriber::get);
        }

    @SuppressWarnings("unchecked")
    private Task.Coordinator<String> coordinator()
        {
        return mock(Task.Coordinator.class);
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
