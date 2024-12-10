/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.atomic.AtomicEnum;

import com.oracle.coherence.concurrent.executor.internal.ExecutorTrace;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * A service for tracking, composing and executing {@link ComposableContinuation}s.
 *
 * @param <T>  a type
 *
 * @author Brian Oliver
 * @since 21.12
 */
public class ContinuationService<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link ContinuationService} using the specified {@link ThreadFactory}
     * to provide {@link Thread}s for executing {@link ComposableContinuation}s.
     *
     * @param threadFactory  the {@link ThreadFactory}
     */
    public ContinuationService(ThreadFactory threadFactory)
        {
        // establish the map of pending continuations
        f_mapPendingContinuations = new ConcurrentHashMap<>();

        // establish the continuation executor services
        // (a prime number to allow perfect hashing of continuations)
        f_aContinuationServices = new ExecutorService[7];

        for (int i = 0; i < f_aContinuationServices.length; i++)
            {
            f_aContinuationServices[i] = Executors.newSingleThreadExecutor(threadFactory);
            }

        // we're immediately running
        f_state = AtomicEnum.of(State.RUNNING);
        }

    // ----- public methods -------------------------------------------------

    /**
     * Schedule a {@link ComposableContinuation} to be asynchronously executed for the
     * specified object.
     *
     * @param continuation  the {@link ComposableContinuation}
     * @param object        the object
     *
     * @return {@code true} if the {@link ComposableContinuation} was accepted
     *         {@code false} if the {@link ComposableContinuation} was rejected
     *         (due to the service being unavailable or the continuation being
     *         invalid ie: {@code null})
     */
    public boolean submit(ComposableContinuation continuation, final T object)
        {
        ExecutorTrace.entering(ContinuationService.class, "submit", continuation, object);

        boolean fResult = false;

        if (f_state.get() == State.RUNNING)
            {
            if (continuation != null)
                {
                Map<T, ComposableContinuation> mapPendingContinuations = f_mapPendingContinuations;
                ExecutorService[]              aExecutorServices       = f_aContinuationServices;

                synchronized (mapPendingContinuations)
                    {
                    ComposableContinuation existing = mapPendingContinuations.get(object);
                    ComposableContinuation composed = existing == null
                                                          ? continuation
                                                          : existing.compose(continuation);

                    ExecutorTrace.log(() -> String.format("Composing existing [%s] with provided [%s]; result [%s]",
                                                          existing, continuation, composed));
                    // only submit non-null composed continuations
                    if (composed != null)
                        {
                        mapPendingContinuations.put(object, composed);

                        // we schedule a Runnable to execute the continuation for the first non-null continuation
                        if (existing == null)
                            {
                            // determine the Executor to use based on the object hashcode
                            int index = Math.abs(object.hashCode()) % aExecutorServices.length;

                            // ensure we have a valid index (as abs may yield a negative!)
                            if (index < 0)
                                {
                                index = 0;
                                }

                            int             nIdx    = index;
                            ExecutorService service = aExecutorServices[nIdx];

                            ExecutorTrace.log(() -> String.format("Submitting continuation to executor [%s] at index [%s]", service, nIdx));

                            aExecutorServices[nIdx].submit(new ContinuationRunnable(object, nIdx));
                            }
                        }

                    fResult = true;
                    }
                }
            }

            ExecutorTrace.exiting(ContinuationService.class, "submit", fResult);

            return fResult;
        }

    /**
     * Abandons any {@link ComposableContinuation}s for the specified object.
     *
     * @param object  the object
     */
    public void abandon(T object)
        {
        synchronized (object)
            {
            f_mapPendingContinuations.remove(object);
            }
        }

    /**
     * Initiates an orderly shutdown in which previously submitted
     * {@link ComposableContinuation}s are allowed to execute, but no new
     * {@link ComposableContinuation}s may be executed.
     * <p>
     * This method does not wait for previously submitted tasks to complete
     * execution. Use awaitTermination to do that.
     */
    public void shutdown()
        {
        if (f_state.compareAndSet(State.RUNNING, State.SHUTDOWN))
            {
            for (ExecutorService executorService : f_aContinuationServices)
                {
                try
                    {
                    executorService.shutdown();
                    }
                catch (Throwable t)
                    {
                    // TODO:
                    }
                }
            }
        }

    /**
     * Attempts the shutdown of the {@link ContinuationService} immediately, abandoning
     * any submitted {@link ComposableContinuation}s and interrupting those that
     * are running.
     * <p>
     * This method does not wait for previously submitted tasks to complete execution.
     * Use awaitTermination to do that.
     */
    public void shutdownNow()
        {
        if (f_state.compareAndSet(State.RUNNING, State.SHUTDOWN))
            {
            for (ExecutorService executorService : f_aContinuationServices)
                {
                try
                    {
                    executorService.shutdownNow();
                    }
                catch (Throwable t)
                    {
                    // TODO:
                    }
                }
            }
        }

    // ----- enum: State ----------------------------------------------------

    /**
     * The possible states of the {@link ContinuationService}.
     */
    private enum State
        {
        /**
         * Running state.
         */
        RUNNING,

        /**
         * Shutdown state.
         */
        SHUTDOWN
        }

    // ----- inner class: ContinuationRunnable ------------------------------

    /**
     * The {@link Runnable} that will execute a {@link ComposableContinuation} for an
     * {@link Object} using a specified {@link ExecutorService} index.
     */
    public class ContinuationRunnable
            implements Runnable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link ContinuationRunnable}.
         *
         * @param object        the {@link Object}
         * @param serviceIndex  the service index
         */
        public ContinuationRunnable(T object, int serviceIndex)
            {
            f_object        = object;
            f_nServiceIndex = serviceIndex;
            }

        // ----- Runnable interface -----------------------------------------

        @Override
        public void run()
            {
            if (ExecutorTrace.isEnabled())
                {
                ExecutorTrace.entering(ContinuationRunnable.class,
                                       "run", f_object, Integer.toHexString(f_object.hashCode()), f_nServiceIndex);
                }

            Map<T, ComposableContinuation> mapPendingContinuations = f_mapPendingContinuations;
            T                              key                     = f_object;
            ComposableContinuation         continuation;

            synchronized (mapPendingContinuations)
                {
                // remove the current continuation
                continuation = mapPendingContinuations.remove(key);
                }

            if (continuation == null)
                {
                Logger.fine(() -> String.format(
                        "ComposableContinuation for [%s] has been removed (ignoring request)", key));
                }
            else
                {
                try
                    {
                    ExecutorTrace.log(() -> String.format("Executing continuation [%s] for [%s]", continuation, key));

                    // attempt to execute the continuation
                    continuation.proceed(null);
                    }
                catch (Throwable t)
                    {
                    // failed to execute the continuation!
                    Logger.warn(() -> String.format("Failed to execute continuation [%s] for [%s]",
                                                    continuation, key));
                    Logger.warn("ComposableContinuation encountered", t);
                    }
                }

            ExecutorTrace.exiting(ContinuationRunnable.class, "run");
            }

        // ----- data members -----------------------------------------------

        /**
         * The object.
         */
        protected final T f_object;

        /**
         * The service index.
         */
        protected final int f_nServiceIndex;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The map of pending {@link ComposableContinuation}s by object.
     */
    protected final ConcurrentHashMap<T, ComposableContinuation> f_mapPendingContinuations;

    /**
     * The {@link ExecutorService}s for executing {@link ComposableContinuation}s.
     */
    protected final ExecutorService[] f_aContinuationServices;

    /**
     * The current {@link State} of the {@link ContinuationService}.
     */
    protected final AtomicEnum<State> f_state;
    }
