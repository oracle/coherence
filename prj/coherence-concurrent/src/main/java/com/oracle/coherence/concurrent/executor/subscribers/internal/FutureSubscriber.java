/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.subscribers.internal;

import com.oracle.coherence.concurrent.executor.Result;
import com.oracle.coherence.concurrent.executor.Task;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link Task.Subscriber} that also implements {@link Future}.
 *
 * @param <T> the result type
 *
 * @author bo, lh
 * @since 21.12
 */
public class FutureSubscriber<T>
        implements Future<T>, Task.Subscriber<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link FutureSubscriber}.
     */
    public FutureSubscriber()
        {
        f_fCompleted   = new AtomicBoolean(false);
        f_fError       = new AtomicBoolean(false);
        m_result       = Result.none();
        f_subscription = new AtomicReference<>();
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Set the coordinator.
     *
     * @param coordinator coordinator
     */
    public void setCoordinator(Task.Coordinator<T> coordinator)
        {
        m_coordinator = coordinator;
        }

    // ----- Subscriber interface -------------------------------------------

    @Override
    public void onComplete()
        {
        f_fCompleted.compareAndSet(false, true);
        f_subscription.set(null);
        synchronized (this)
            {
            notifyAll();
            }
        }

    @Override
    public void onError(Throwable throwable)
        {
        f_fError.compareAndSet(false, true);
        f_subscription.set(null);
        m_result = Result.throwable(throwable);
        synchronized (this)
            {
            notifyAll();
            }
        }

    @Override
    public void onNext(T result)
        {
        m_result = Result.of(result);
        }

    @Override
    public void onSubscribe(Task.Subscription subscription)
        {
        if (!f_subscription.compareAndSet(null, subscription))
            {
            throw new UnsupportedOperationException("FutureSubscriber reuse is not supported.");
            }
        }

    // ----- Future interface -----------------------------------------------

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
        {
        Task.Coordinator<T> coordinator = m_coordinator;

        // coordinator may be null if Task has not yet been submitted
        return !isDone() && coordinator != null && coordinator.cancel(mayInterruptIfRunning);
        }

    @Override
    public boolean isCancelled()
        {
        return m_coordinator.isCancelled();
        }

    @Override
    public boolean isDone()
        {
        return f_fCompleted.get() || f_fError.get();
        }

    @Override
    public T get()
            throws InterruptedException, ExecutionException
        {
        if (!hasResult())
            {
            synchronized (this)
                {
                wait();
                }
            }

        try
            {
            return m_result.get();
            }
        catch (Throwable throwable)
            {
            throw new ExecutionException(throwable);
            }

        }

    @Override
    public T get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException
        {
        if (timeout > 0 && !hasResult())
            {
            synchronized (this)
                {
                wait(unit.toMillis(timeout));
                }

            if (!hasResult())
                {
                throw new TimeoutException("Timed out before the task is completed.");
                }
            }

        try
            {
            return m_result.get();
            }
        catch (Throwable throwable)
            {
            throw new ExecutionException(throwable);
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Determines if the {@link FutureSubscriber} has been completed by a {@link Task.Coordinator}.
     *
     * @return <code>true</code> if the {@link FutureSubscriber} has been completed,
     *         <code>false</code> otherwise
     */
    public boolean getCompleted()
        {
        return f_fCompleted.get();
        }

    /**
     * Determines if the {@link AnyFutureSubscriber} has been completed by a {@link Task.Coordinator}.
     *
     * @return <code>true</code> if the {@link AnyFutureSubscriber} has been completed,
     *         <code>false</code> otherwise
     */
    public boolean hasResult()
        {
        return m_result.isPresent();
        }

    /**
     * Determines if the {@link FutureSubscriber} has been subscribed to a {@link Task.Coordinator}.
     *
     * @return <code>true</code> if the {@link FutureSubscriber} has been subscribed,
     *         <code>false</code> otherwise
     */
    public boolean isSubscribed()
        {
        return f_subscription.get() != null;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Task coordinator.
     */
    protected Task.Coordinator<T> m_coordinator;

    /**
     * Completed.
     */
    protected final AtomicBoolean f_fCompleted;

    /**
     * Error.
     */
    protected final AtomicBoolean f_fError;

    /**
     * The result.
     */
    protected Result<T> m_result;

    /**
     * Subscription.
     */
    private final AtomicReference<Task.Subscription> f_subscription;
    }
