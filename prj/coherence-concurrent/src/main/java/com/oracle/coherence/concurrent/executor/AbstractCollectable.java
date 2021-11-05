/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.tangosol.util.function.Remote.Predicate;

import java.util.LinkedHashSet;

/**
 * A base implementation of a {@link Task.Collectable} and {@link Task.Completable}.
 *
 * @param <T>  the type of the {@link Task}
 * @param <R>  the type of the collected result
 * @param <O>  the type of {@link Task.Orchestration} on which the {@link Task.Collectable} is based
 *
 * @author bo
 * @since 21.12
 */
public abstract class AbstractCollectable<T, R, O extends AbstractOrchestration<T, ?>>
        implements Task.Collectable<T, R>, Task.Completable<T, R>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link Task.Collectable} based on an {@link Task.Orchestration}.
     *
     * @param orchestration  the {@link Task.Orchestration}
     * @param collector      the {@link Task.Collector} of results
     */
    public AbstractCollectable(O orchestration,
                               Task.Collector<? super T, ?, R> collector)
        {
        m_orchestration       = orchestration;
        m_collector           = collector;

        // establish default orchestration properties
        m_setSubscribers = new LinkedHashSet<>();
        }

    // ----- AbstractCollectable methods ------------------------------------

    @Override
    public Task.Completable<T, R> until(Predicate<? super R> predicate)
        {
        m_completionPredicate = predicate;

        return this;
        }

    @Override
    public Task.Completable<T, R> andThen(Task.CompletionRunnable<? super R> runnable)
        {
        m_completionRunnable = runnable;

        return this;
        }

    @Override
    public Task.Collectable<T, R> subscribe(Task.Subscriber<? super R> subscriber)
        {
        m_setSubscribers.add(subscriber);

        return this;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Task.Orchestration} on which the {@link Task.Collectable} is based.
     */
    protected O m_orchestration;

    /**
     * The {@link Task.Collector} for the {@link Task}.
     */
    protected Task.Collector<? super T, ?, R> m_collector;

    /**
     * The optional {@link Predicate} to indicate if collection is completed.
     * <code>null</code> when not required (for continuous collection)
     */
    protected Predicate<? super R> m_completionPredicate;

    /**
     * The {@link Task.Subscriber}s to register when creating the {@link Task} orchestration with the {@link
     * TaskExecutorService}.
     */
    protected LinkedHashSet<Task.Subscriber<? super R>> m_setSubscribers;

    /**
     * The optional {@link Task.CompletionRunnable} to call after the task is completed.
     * <code>null</code> when not required (to call a Runnable)
     */
    protected Task.CompletionRunnable<? super R> m_completionRunnable;
    }
