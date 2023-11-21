/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.common.base.Continuation;
import com.oracle.coherence.common.base.Notifier;
import com.oracle.coherence.common.base.SingleWaiterMultiNotifier;
import com.oracle.coherence.common.base.Timeout;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.internal.util.Daemons;
import com.tangosol.net.FlowControl;

import com.tangosol.util.aggregator.AbstractAsynchronousAggregator;
import com.tangosol.util.aggregator.AsynchronousAggregator;

import com.tangosol.util.processor.AbstractAsynchronousProcessor;
import com.tangosol.util.processor.AsynchronousProcessor;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.function.Supplier;

/**
 * Base class for asynchronous operations that provides a simple implementation
 * of the {@link Future} interface. It is assumed that subclasses at some point
 * will either call {@link #complete(Supplier)} passing the result supplier
 * when completed successfully or call {@link #completeExceptionally} passing the
 * failure reason.
 *
 * @param <T>  the type of the result
 *
 * @author gg/mf 2012.12.21
 * @author gg/bb 2015.04.06
 */
public abstract class AsynchronousAgent<T>
        implements FlowControl, Future<T>
    {
    /**
     * Construct the agent.
     *
     * @param iOrderId  a unit-of-order id associated with this agent. Ordering
     *                  semantics of operations based on this id are defined
     *                  by subclasses
     */
    protected AsynchronousAgent(int iOrderId)
        {
        this(iOrderId, null);
        }

    /**
     * Construct the agent.
     *
     * @param iOrderId  a unit-of-order id associated with this agent. Ordering
     *                  semantics of operations based on this id are defined
     *                  by subclasses
     * @param executor  an optional {@link Executor} to complete the future on,
     *                  if not provided the {@link Daemons#commonPool()} is used
     */
    protected AsynchronousAgent(int iOrderId, Executor executor)
        {
        m_iOrderId = iOrderId;
        f_executor = executor == null ? Daemons.commonPool() : executor;
        }

    // ----- FlowControl support --------------------------------------------

    /**
     * Bind this agent with the specified underlying FlowControl object. This
     * method is to be used only internally by the service.
     *
     * @param control the underlying FlowControl
     */
    public void bind(FlowControl control)
        {
        m_control = control;
        }

    @Override
    public void flush()
        {
        FlowControl control = m_control;
        if (control == null)
            {
            throw new IllegalStateException();
            }
        control.flush();
        }

    @Override
    public long drainBacklog(long cMillis)
        {
        FlowControl control = m_control;
        if (control == null)
            {
            throw new IllegalStateException();
            }
        return control.drainBacklog(cMillis);
        }

    @Override
    public boolean checkBacklog(Continuation<Void> continueNormal)
        {
        FlowControl control = m_control;
        if (control == null)
            {
            throw new IllegalStateException();
            }
        return control.checkBacklog(continueNormal);
        }

    // ----- Future support -------------------------------------------------

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
        {
        return completeExceptionally(new CancellationException());
        }

    @Override
    public boolean isCancelled()
        {
        CompletableFuture<T> future;
        if (m_fCompleted && (future = m_future) != null)
            {
            return future.isCancelled();
            }

        return false;
        }

    @Override
    public boolean isDone()
        {
        return m_fCompleted;
        }

    @Override
    public T get()
            throws InterruptedException, ExecutionException
        {
        try
            {
            return get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            }
        catch (TimeoutException e)
            {
            // will never happen
            throw new IllegalStateException();
            }
        }

    @Override
    public T get(long cTimeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException
        {
        long ldtTimeout = Base.getLastSafeTimeMillis() + unit.toMillis(cTimeout);

        try (Timeout t = Timeout.after(cTimeout, unit))
            {
            while (!m_fCompleted)
                {
                f_notifier.await();
                }

            return getCompletableFuture().get();
            }
        catch (InterruptedException e)
            {
            if (Base.getSafeTimeMillis() >= ldtTimeout)
                {
                throw new TimeoutException("Elapsed " + new Duration(unit.toNanos(cTimeout)));
                }
            else
                {
                throw e;
                }
            }
        }

     // ----- Subclasses support ---------------------------------------------

    /**
     * Return a unit-of-order id associated with this agent. By default,
     * the unit-of-order id is assigned to the calling thread's hashCode.
     * <p>
     * Note 1: the ordering guarantee is respected between {@link
     * AsynchronousAggregator}s and {@link AsynchronousProcessor}s with the same
     * unit-of-order id;
     * <br>
     * Note 2: there is no ordering guarantee between asynchronous and synchronous
     * operations.
     *
     * @return the order id
     *
     * @see AbstractAsynchronousAggregator#getUnitOfOrderId()
     * @see AbstractAsynchronousProcessor#getUnitOfOrderId()
     */
    public int getUnitOfOrderId()
        {
        return m_iOrderId;
        }

    /**
     * Should be called if the operation completed successfully.
     *
     * @param supplier the supplier of the result of the asynchronous execution
     *
     * @return {@code true} if agent could be marked to complete with the
     *         given Supplier.
     */
    protected synchronized boolean complete(Supplier<T> supplier)
        {
        if (!m_fCompleted)
            {
            if (supplier == null)
                {
                throw new IllegalArgumentException("No supplier");
                }

            CompletableFuture<T> future = m_future;
            if (future == null || !future.isDone())
                {
                if (future == null)
                    {
                    // getCompletableFuture hasn't been called yet
                    m_supplier   = supplier;
                    m_fCompleted = true;
                    f_notifier.signal();
                    }
                else
                    {
                    future.completeAsync(supplier, f_executor)
                            .whenComplete((r, e) ->
                                {
                                m_fCompleted = true;
                                f_notifier.signal();
                                });
                    }

                return true;
                }
            }

        return false;
        }

    /**
     * Should be called if the operation failed for any reason.
     *
     * @param eReason  the reason of failure
     *
     * @return {@code true} if agent could be marked to complete with the
     *         given exception.
     */
    protected synchronized boolean completeExceptionally(Throwable eReason)
        {
        if (!m_fCompleted)
            {
            if (eReason == null)
                {
                throw new IllegalArgumentException("No reason");
                }

            CompletableFuture<T> future = m_future;
            if (future == null || !future.isDone())
                {
                if (future == null)
                    {
                    future = m_future = new CompletableFuture<>();
                    }

                future.completeExceptionally(eReason);

                m_fCompleted = true;
                f_notifier.signal();
                return true;
                }
            }

        return false;
        }

    /**
     * Helper method that calls {@link #get} and re-throws checked exceptions
     * as a RuntimeException.
     *
     * @return  the result value
     */
    public T getResult()
        {
        try
            {
            return get();
            }
        catch (ExecutionException e)
            {
            throw Base.ensureRuntimeException(e.getCause());
            }
        catch (InterruptedException e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * Helper method that returns an exception (if completed exceptionally).
     *
     * @return  the exception or null if the operation completed successfully
     */
    public Throwable getException()
        {
        if (isCompletedExceptionally())
            {
            try
                {
                get();
                }
            catch (ExecutionException e)
                {
                return e.getCause();
                }
            catch (InterruptedException e)
                {
                return e;
                }
            }
        return null;
        }

    /**
     * Helper method to check if the operation failed.
     *
     * @return  true if the operation failed
     */
    public boolean isCompletedExceptionally()
        {
        CompletableFuture<T> future;
        if (m_fCompleted && (future = m_future) != null)
            {
            return future.isCompletedExceptionally();
            }

        return false;
        }

    /**
     * Get the CompletableFuture.
     *
     * @return  CompletableFuture
     */
    public synchronized CompletableFuture<T> getCompletableFuture()
        {
        CompletableFuture<T> future = m_future;
        if (future == null)
            {
            future = m_future = new CompletableFuture<>();
            if (m_fCompleted)
                {
                assert m_supplier != null;

                future.completeAsync(m_supplier, f_executor)
                        .whenComplete((r, e) -> f_notifier.signal());
                }
            }

        return future;
        }

    // ----- data fields ----------------------------------------------------

    /**
     * The underlying FlowControl; could be null if the "automatic flow control"
     * is turned on.
     */
    protected FlowControl m_control;

    /**
     * A unit-of-order id associated with this agent.
     */
    protected final int m_iOrderId;

    /**
     * Indicates that the operation has completed.
     */
    private volatile boolean m_fCompleted;

    /**
     * Supplier of the final result of operation (if successful).
     */
    private Supplier<T> m_supplier;

    /**
     * CompletableFuture tied to the agent.
     */
    private CompletableFuture<T> m_future;

    /**
     * Notification handler.
     */
    private final Notifier f_notifier = new SingleWaiterMultiNotifier();

    /**
     * The {@link Executor} to complete the future on.
     */
    private final Executor f_executor;
    }
