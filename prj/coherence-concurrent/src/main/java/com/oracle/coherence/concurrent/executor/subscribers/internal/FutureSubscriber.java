/*
 * Copyright (c) 2016, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.subscribers.internal;

import com.oracle.coherence.concurrent.executor.ClusteredTaskCoordinator;
import com.oracle.coherence.concurrent.executor.Result;
import com.oracle.coherence.concurrent.executor.Task;

import java.util.concurrent.CancellationException;
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
        f_fCancelled   = new AtomicBoolean(false);
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
        synchronized (this)
            {
            if (isDone() && m_coordinator != coordinator)
                {
                throw new UnsupportedOperationException("FutureSubscriber reuse is not supported.");
                }
            m_coordinator = coordinator;
            }
        }

    // ----- Subscriber interface -------------------------------------------

    @Override
    public void onComplete()
        {
        synchronized (this)
            {
            if (!isDone())
                {
                f_fCompleted.set(true);
                f_subscription.set(null);
                notifyAll();
                }
            }
        }

    @Override
    public void onError(Throwable throwable)
        {
        synchronized (this)
            {
            if (!isDone())
                {
                Task.Coordinator<?> coordinator = getTaskCoordinator();
                if (coordinator != null && coordinator.isCancelled())
                    {
                    f_fCancelled.set(true);
                    }
                else
                    {
                    m_result = Result.throwable(throwable);
                    f_fError.set(true);
                    }
                f_subscription.set(null);
                notifyAll();
                }
            }
        }

    @Override
    public void onNext(T result)
        {
        synchronized (this)
            {
            if (!isDone())
                {
                m_result = Result.of(result);
                }
            }
        }

    @Override
    @SuppressWarnings("unchecked")
    public void onSubscribe(Task.Subscription subscription)
        {
        synchronized (this)
            {
            if (isDone() || !f_subscription.compareAndSet(null, subscription))
                {
                throw new UnsupportedOperationException("FutureSubscriber reuse is not supported.");
                }
            if (m_coordinator == null)
                {
                m_coordinator = (Task.Coordinator<T>) subscription.getCoordinator();
                }
            }
        }

    // ----- Future interface -----------------------------------------------

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
        {
        Task.Coordinator<?> coordinator;
        synchronized (this)
            {
            if (isDone())
                {
                return false;
                }
            coordinator = getTaskCoordinator();
            if (coordinator == null)
                {
                // serialize local cancellation with coordinator and subscription publication
                f_fCancelled.set(true);
                f_subscription.set(null);
                notifyAll();
                return true;
                }
            }

        // do not hold the subscriber monitor across a coordinator request
        if (!coordinator.cancel(mayInterruptIfRunning))
            {
            reconcileTerminalResult(coordinator);
            return false;
            }

        synchronized (this)
            {
            if (!isDone())
                {
                f_fCancelled.set(true);
                f_subscription.set(null);
                notifyAll();
                }
            return f_fCancelled.get();
            }
        }

    @Override
    public boolean isCancelled()
        {
        return f_fCancelled.get();
        }

    @Override
    public boolean isDone()
        {
        return f_fCompleted.get() || f_fError.get() || f_fCancelled.get();
        }

    @Override
    public T get()
            throws InterruptedException, ExecutionException
        {
        synchronized (this)
            {
            while (!isDone())
                {
                wait();
                }
            }

        return getResult();
        }

    @Override
    public T get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException
        {
        long cNanos = unit.toNanos(timeout);
        long ldtEnd = System.nanoTime() + cNanos;

        synchronized (this)
            {
            while (!isDone())
                {
                if (cNanos <= 0)
                    {
                    throw new TimeoutException("Timed out before the task is completed.");
                    }
                TimeUnit.NANOSECONDS.timedWait(this, cNanos);
                cNanos = ldtEnd - System.nanoTime();
                }
            }

        return getResult();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Reconcile a terminal result when the clustered task has already finished
     * or been cancelled, but subscriber callbacks have not yet been delivered.
     *
     * @param coordinator  the coordinator that refused cancellation
     */
    @SuppressWarnings("unchecked")
    private void reconcileTerminalResult(Task.Coordinator<?> coordinator)
        {
        Result<?> result = coordinator instanceof ClusteredTaskCoordinator
                ? ((ClusteredTaskCoordinator<?>) coordinator).getTerminalResult()
                : null;
        boolean cancelled = coordinator.isCancelled();

        synchronized (this)
            {
            if (!isDone())
                {
                if (cancelled)
                    {
                    f_fCancelled.set(true);
                    }
                else if (result != null && result.isPresent())
                    {
                    m_result = (Result<T>) result;
                    if (result.isThrowable())
                        {
                        f_fError.set(true);
                        }
                    else
                        {
                        f_fCompleted.set(true);
                        }
                    }

                if (isDone())
                    {
                    f_subscription.set(null);
                    notifyAll();
                    }
                }
            }
        }

    /**
     * Obtain the explicitly assigned coordinator or the current subscription's
     * coordinator when this future was subscribed directly.
     *
     * @return the coordinator, or {@code null} before submission
     */
    private Task.Coordinator<?> getTaskCoordinator()
        {
        Task.Coordinator<?> coordinator = m_coordinator;
        Task.Subscription   subscription = f_subscription.get();
        return coordinator == null && subscription != null
                ? subscription.getCoordinator()
                : coordinator;
        }

    /**
     * Return the terminal result, reporting cancellation directly.
     *
     * @return the result
     *
     * @throws CancellationException if this future was cancelled
     * @throws ExecutionException    if the task failed
     */
    protected T getResult()
            throws ExecutionException
        {
        if (isCancelled())
            {
            throw new CancellationException("Task has been cancelled.");
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
    protected volatile Task.Coordinator<T> m_coordinator;

    /**
     * Completed.
     */
    protected final AtomicBoolean f_fCompleted;

    /**
     * Error.
     */
    protected final AtomicBoolean f_fError;

    /**
     * Cancelled.
     */
    protected final AtomicBoolean f_fCancelled;

    /**
     * The result.
     */
    protected volatile Result<T> m_result;

    /**
     * Subscription.
     */
    private final AtomicReference<Task.Subscription> f_subscription;
    }
