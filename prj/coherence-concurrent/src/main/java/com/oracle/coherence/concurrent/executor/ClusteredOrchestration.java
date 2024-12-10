/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

/**
 * A clustered implementation of a {@link Task.Orchestration}.
 *
 * @param <T>  the type of results produced by a {@link Task}
 *
 * @author phf
 * @since 21.12
 */
public class ClusteredOrchestration<T>
        extends AbstractOrchestration<T, ClusteredExecutorService>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@code ClusteredOrchestration}.
     *
     * @param clusteredExecutorService the {@link ClusteredExecutorService} that this
     *                                 {@code ClusteredOrchestration} will use
     * @param task                     the {@link Task} being orchestrated
     */
    public ClusteredOrchestration(ClusteredExecutorService clusteredExecutorService, Task<T> task)
        {
        super(clusteredExecutorService, task);
        }

    // ----- AbstractOrchestration methods ----------------------------------

    @Override
    public <R> Task.Collectable<T, R> collect(Task.Collector<? super T, ?, R> collector)
        {
        return new ClusteredCollectable<>(this, collector);
        }

    // ----- AbstractOrchestration implementation ---------------------------

    @Override
    protected Task.Collectable<T, T> collect()
        {
        return new ClusteredCollectable<>(this);
        }
    }
