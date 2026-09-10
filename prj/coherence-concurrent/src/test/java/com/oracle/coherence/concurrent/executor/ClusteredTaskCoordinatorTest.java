/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.executor.subscribers.internal.FutureSubscriber;
import com.oracle.coherence.concurrent.executor.util.Caches;

import com.tangosol.net.CacheService;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;
import com.tangosol.net.NamedCache;
import com.tangosol.net.ServiceInfo;

import java.util.Collections;
import java.util.List;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for task-state reconciliation after remote cache-service reconnects.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ClusteredTaskCoordinatorTest
    {
    @Test
    public void shouldRecoverMissedCompletionWhenRemoteMemberLeaves()
            throws Exception
        {
        TestContext context = createContext();
        ClusteredTaskManager completed = manager(1, Result.of("result"), true);
        when(context.tasks.get(TASK_ID)).thenReturn(completed);

        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        subscriber.setCoordinator(context.coordinator);
        context.coordinator.subscribe(subscriber);
        clearInvocations(context.tasks);

        context.listener.memberLeft(mock(MemberEvent.class));

        assertEquals("result", subscriber.get(1, TimeUnit.SECONDS));
        InOrder ordered = inOrder(context.tasks);
        ordered.verify(context.tasks).addMapListener(context.coordinator, TASK_ID, false);
        ordered.verify(context.tasks).get(TASK_ID);
        }

    @Test
    public void shouldFailRecoveredFutureWhenTaskNoLongerExists()
        {
        TestContext context = createContext();
        when(context.tasks.get(TASK_ID)).thenReturn(null);

        FutureSubscriber<String> subscriber = new FutureSubscriber<>();
        subscriber.setCoordinator(context.coordinator);
        context.coordinator.subscribe(subscriber);
        clearInvocations(context.tasks);

        context.listener.memberJoined(mock(MemberEvent.class));

        ExecutionException error = assertThrows(ExecutionException.class,
                () -> subscriber.get(1, TimeUnit.SECONDS));
        assertTrue(error.getCause() instanceof IllegalStateException);
        }

    private TestContext createContext()
        {
        CacheService service = mock(CacheService.class);
        NamedCache   tasks   = mock(NamedCache.class);
        ServiceInfo  info    = mock(ServiceInfo.class);

        when(service.ensureCache(Caches.TASKS_CACHE_NAME, null)).thenReturn(tasks);
        when(service.getInfo()).thenReturn(info);
        when(info.getServiceType()).thenReturn(CacheService.TYPE_REMOTE);

        ClusteredTaskManager initial = manager(0, Result.none(), false);
        ClusteredTaskCoordinator<String> coordinator =
                new ClusteredTaskCoordinator<>(service, initial, new DirectExecutorService());

        ArgumentCaptor<MemberListener> captor = ArgumentCaptor.forClass(MemberListener.class);
        verify(service).addMemberListener(captor.capture());

        return new TestContext(tasks, coordinator, captor.getValue());
        }

    private ClusteredTaskManager manager(int nVersion, Result<?> result, boolean fCompleted)
        {
        ClusteredTaskManager manager = mock(ClusteredTaskManager.class);
        when(manager.getTaskId()).thenReturn(TASK_ID);
        when(manager.getResultVersion()).thenReturn(nVersion);
        when(manager.getLastResult()).thenReturn(result);
        when(manager.isCompleted()).thenReturn(fCompleted);
        return manager;
        }

    // ----- inner class: DirectExecutorService ---------------------------

    private static class DirectExecutorService
            extends AbstractExecutorService
        {
        @Override
        public void shutdown()
            {
            m_fShutdown = true;
            }

        @Override
        public List<Runnable> shutdownNow()
            {
            m_fShutdown = true;
            return Collections.emptyList();
            }

        @Override
        public boolean isShutdown()
            {
            return m_fShutdown;
            }

        @Override
        public boolean isTerminated()
            {
            return m_fShutdown;
            }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit)
            {
            return m_fShutdown;
            }

        @Override
        public void execute(Runnable command)
            {
            command.run();
            }

        private boolean m_fShutdown;
        }

    // ----- inner class: TestContext -------------------------------------

    private static class TestContext
        {
        private TestContext(NamedCache tasks, ClusteredTaskCoordinator<String> coordinator,
                MemberListener listener)
            {
            this.tasks       = tasks;
            this.coordinator = coordinator;
            this.listener    = listener;
            }

        private final NamedCache tasks;

        private final ClusteredTaskCoordinator<String> coordinator;

        private final MemberListener listener;
        }

    private static final String TASK_ID = "task";
    }
