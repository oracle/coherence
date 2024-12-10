/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

/**
 * A clustered implementation of a {@link Task.Collectable} and {@link Task.Completable}.
 *
 * @param <T>  the type of the task
 * @param <R>  the type of the collected result
 *
 * @author bo
 * @since 21.12
 */
class ClusteredCollectable<T, R>
        extends AbstractCollectable<T, R, ClusteredOrchestration<T>>
    {
    /**
     * Constructs a {@link ClusteredCollectable}.
     *
     * @param orchestration  the {@link Task.Orchestration}
     * @param collector      the {@link Task.Collector}
     */
    ClusteredCollectable(ClusteredOrchestration<T> orchestration, Task.Collector<? super T, ?, R> collector)
        {
        super(orchestration, collector);
        }

    /**
     * Constructs a {@link ClusteredCollectable} with no {@link Task.Collector}.
     *
     * @param orchestration  the {@link Task.Orchestration}
     */
    ClusteredCollectable(ClusteredOrchestration<T> orchestration)
        {
        super(orchestration, null);
        }

    @Override
    public Task.Coordinator<R> submit()
        {
        return m_orchestration.getTaskExecutorService().submit(
                m_orchestration.getTask(),
                m_orchestration.getTaskId(),
                m_orchestration.getAssignmentStrategy(),
                m_orchestration.getOptionsByType(),
                m_orchestration.getProperties(),
                m_collector,
                m_completionPredicate,
                m_completionRunnable,
                m_orchestration.getRetainDuration(),
                m_setSubscribers.iterator());
        }
    }
