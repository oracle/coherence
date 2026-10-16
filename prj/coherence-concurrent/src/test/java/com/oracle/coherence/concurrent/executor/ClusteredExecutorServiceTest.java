/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.executor.subscribers.internal.FutureSubscriber;
import com.oracle.coherence.concurrent.executor.util.Caches;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.util.MapEvent;
import com.tangosol.util.function.Remote;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for clustered executor bulk cancellation.
 *
 * @author phf  2026.10.06
 * @since 26.10
 */
public class ClusteredExecutorServiceTest
    {
    @Test
    public void shouldCancelAllUnsubmittedFuturesWhenInvokeAllTimesOut()
            throws Exception
        {
        CacheService service = mock(CacheService.class);
        ClusteredExecutorService executor = new ClusteredExecutorService(service);
        try
            {
            List<Remote.Callable<String>> tasks = Arrays.asList(() -> "first", () -> "second");
            List<Future<String>> futures = executor.invokeAll(tasks, 0, TimeUnit.MILLISECONDS);

            assertEquals(2, futures.size());
            for (Future<String> future : futures)
                {
                assertTrue(future.isDone());
                assertTrue(future.isCancelled());
                assertThrows(CancellationException.class, future::get);
                assertThrows(CancellationException.class, () -> future.get(0, TimeUnit.MILLISECONDS));
                }
            verifyNoInteractions(service);
            }
        finally
            {
            executor.getScheduledExecutorService().shutdownNow();
            }
        }

    @Test
    public void shouldFailInvokeAnyWhenAllCandidatesAreCancelled()
        {
        ClusteredExecutorService executor = spy(new ClusteredExecutorService(mock(CacheService.class)));
        doReturn(orchestration(true)).when(executor).orchestrate(any());
        try
            {
            List<Remote.Callable<String>> tasks = Arrays.asList(() -> "first", () -> "second");
            ExecutionException error = assertThrows(ExecutionException.class, () -> executor.invokeAny(tasks));
            assertTrue(error.getCause() instanceof CancellationException);
            }
        finally
            {
            executor.getScheduledExecutorService().shutdownNow();
            }
        }

    @Test
    public void shouldReturnSuccessfulInvokeAnyResultAfterCancelledCandidate()
            throws Exception
        {
        ClusteredExecutorService executor = spy(new ClusteredExecutorService(mock(CacheService.class)));
        doReturn(orchestration(true), orchestration(false)).when(executor).orchestrate(any());
        try
            {
            List<Remote.Callable<String>> tasks = Arrays.asList(() -> "first", () -> "second");
            assertEquals("result", executor.invokeAny(tasks, 1, TimeUnit.SECONDS));
            }
        finally
            {
            executor.getScheduledExecutorService().shutdownNow();
            }
        }

    @Test
    public void shouldCompleteTimedInvokeAllBeforeQueuedNotifications()
            throws Exception
        {
        assertTerminalInvokeAll(Result.of("finished"), false, true);
        }

    @Test
    public void shouldReconcileCompletedInvokeAllWithoutCacheEvent()
            throws Exception
        {
        assertTerminalInvokeAll(Result.of("finished"), false, false);
        }

    @Test
    public void shouldPreserveInvokeAllFailureWhenCancellationIsRefused()
            throws Exception
        {
        assertTerminalInvokeAll(Result.throwable(new CancellationException("task failure")), false, false);
        }

    @Test
    public void shouldRecognizeAlreadyCancelledInvokeAll()
            throws Exception
        {
        assertTerminalInvokeAll(Result.none(), true, false);
        }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void assertTerminalInvokeAll(Result<String> result, boolean cancelled, boolean deliverCacheEvent)
            throws Exception
        {
        CacheService service = mock(CacheService.class);
        NamedCache tasks = mock(NamedCache.class);
        String taskId = "invoke-all-completion";
        when(service.ensureCache(Caches.TASKS_CACHE_NAME, null)).thenReturn(tasks);
        when(service.ensureCache(Caches.ASSIGNMENTS_CACHE_NAME, null)).thenReturn(mock(NamedCache.class));
        ClusteredTaskManager initial = mock(ClusteredTaskManager.class);
        when(initial.getTaskId()).thenReturn(taskId);
        when(initial.getLastResult()).thenReturn(Result.none());
        ClusteredTaskManager terminal = mock(ClusteredTaskManager.class);
        when(terminal.getTaskId()).thenReturn(taskId);
        when(terminal.getResultVersion()).thenReturn(1);
        when(terminal.getLastResult()).thenReturn(result);
        when(terminal.isCompleted()).thenReturn(!cancelled);
        when(terminal.isCancelled()).thenReturn(cancelled);
        when(tasks.get(taskId)).thenReturn(terminal);
        when(tasks.invoke(eq(taskId), any(ClusteredTaskManager.CancellationProcessor.class)))
                .thenReturn(false);
        DelayedNotificationExecutor executor =
                new DelayedNotificationExecutor(service, tasks, initial, terminal, deliverCacheEvent);
        try
            {
            List<Remote.Callable<String>> callables = Arrays.asList(() -> "finished", () -> "unsubmitted");
            List<Future<String>> futures = executor.invokeAll(callables, 1, TimeUnit.NANOSECONDS);

            assertEquals(2, futures.size());
            for (Future<String> future : futures)
                {
                assertTrue(future.isDone());
                }
            assertTrue(futures.get(1).isCancelled());
            assertThrows(CancellationException.class, () -> futures.get(1).get(0, TimeUnit.MILLISECONDS));
            assertEquals(cancelled, futures.get(0).isCancelled());
            if (cancelled)
                {
                assertThrows(CancellationException.class, () -> futures.get(0).get(0, TimeUnit.MILLISECONDS));
                }
            else if (result.isThrowable())
                {
                ExecutionException failure = assertThrows(ExecutionException.class,
                        () -> futures.get(0).get(0, TimeUnit.MILLISECONDS));
                assertTrue(failure.getCause() instanceof CancellationException);
                }
            else
                {
                assertEquals("finished", futures.get(0).get(0, TimeUnit.MILLISECONDS));
                }

            // delayed callbacks must not change the reconciled terminal outcome
            executor.notifications.drain();
            assertTrue(futures.get(0).isDone());
            assertEquals(cancelled, futures.get(0).isCancelled());
            if (!cancelled && result.isValue())
                {
                assertEquals("finished", futures.get(0).get());
                }
            }
        finally
            {
            executor.getScheduledExecutorService().shutdownNow();
            }
        }

    @SuppressWarnings("unchecked")
    private Task.Orchestration<String> orchestration(boolean cancelled)
        {
        Task.Orchestration<String> orchestration = mock(Task.Orchestration.class, RETURNS_SELF);
        Task.Coordinator<String> coordinator = mock(Task.Coordinator.class);
        Task.Subscription<String> subscription = mock(Task.Subscription.class);
        when(coordinator.isCancelled()).thenReturn(cancelled);
        when(subscription.getCoordinator()).thenReturn(coordinator);
        when(orchestration.submit()).thenReturn(coordinator);
        when(orchestration.subscribe(any())).thenAnswer(invocation ->
            {
            Task.Subscriber<String> subscriber = invocation.getArgument(0);
            subscriber.onSubscribe(subscription);
            if (cancelled)
                {
                subscriber.onError(new InterruptedException("cancelled by coordinator"));
                }
            else
                {
                subscriber.onNext("result");
                subscriber.onComplete();
                }
            return orchestration;
            });
        return orchestration;
        }

    // ----- inner class: DelayedNotificationExecutor -----------------------

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static class DelayedNotificationExecutor
            extends ClusteredExecutorService
        {
        private DelayedNotificationExecutor(CacheService service, NamedCache tasks, ClusteredTaskManager initial,
                ClusteredTaskManager terminal, boolean deliverCacheEvent)
            {
            super(service);
            this.tasks = tasks;
            this.initial = initial;
            this.terminal = terminal;
            this.deliverCacheEvent = deliverCacheEvent;
            }

        @Override
        public void execute(Remote.Runnable command)
            {
            FutureSubscriber<String> subscriber = (FutureSubscriber<String>) command;
            ClusteredTaskCoordinator<String> coordinator =
                    new ClusteredTaskCoordinator<>(getCacheService(), initial, notifications);
            subscriber.setCoordinator(coordinator);
            coordinator.subscribe(subscriber);
            if (deliverCacheEvent)
                {
                coordinator.entryUpdated(new MapEvent(tasks, MapEvent.ENTRY_UPDATED,
                        initial.getTaskId(), initial, terminal));
                }
            }

        private final QueueExecutor notifications = new QueueExecutor();
        private final NamedCache tasks;
        private final ClusteredTaskManager initial;
        private final ClusteredTaskManager terminal;
        private final boolean deliverCacheEvent;
        }

    // ----- inner class: QueueExecutor -------------------------------------

    private static class QueueExecutor
            extends AbstractExecutorService
        {
        @Override
        public void execute(Runnable command)
            {
            queue.add(command);
            }

        @Override
        public void shutdown()
            {
            }

        @Override
        public List<Runnable> shutdownNow()
            {
            return List.of();
            }

        @Override
        public boolean isShutdown()
            {
            return false;
            }

        @Override
        public boolean isTerminated()
            {
            return false;
            }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit)
            {
            return false;
            }

        private void drain()
            {
            while (!queue.isEmpty())
                {
                queue.remove().run();
                }
            }

        private final Queue<Runnable> queue = new ArrayDeque<>();
        }
    }
