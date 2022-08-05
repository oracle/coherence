/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * The {@link AbstractPreloadApplication} is an abstract class
 * that can be extended to create a multithreaded preload
 * application.
 */
public class AbstractPreloadApplication
    {
    /**
     * Create a {@link AbstractPreloadApplication}.
     *
     * @param tasks    the preload tasks to run
     * @param timeout  the maximum amount of time to wait for all preload tasks to complete
     */
    public AbstractPreloadApplication(Collection<Runnable> tasks, Duration timeout)
        {
        this.tasks = Objects.requireNonNull(tasks);
        this.timeout = Objects.requireNonNull(timeout);
        }

    public void run() throws InterruptedException
        {
        Logger.info("Starting preloader");

        // Create the Executor that will run the preload tasks
        ExecutorService executor = Executors.newCachedThreadPool();

        // Submit each task to the executor
        for (Runnable task : tasks)
            {
            Logger.info("Submitting preload task " + task);
            executor.submit(new SafeRunnable(task));
            }

        // Stop the executor accepting any more requests
        executor.shutdown();
        // Wait for the executor to complete running the tasks
        Logger.info("Waiting up to " + timeout + " for preload tasks to complete");
        boolean terminated = executor.awaitTermination(timeout.getSeconds(), TimeUnit.SECONDS);
        if (!terminated)
            {
            Logger.err("Preload tasks did not complete within " + timeout);
            executor.shutdownNow();
            }
        }

    // ----- inner class: SafeRunnable --------------------------------------

    /**
     * A {@link Runnable} implementation that wraps another {@link Runnable},
     * which is executed inside a try/catch block.
     */
    protected static class SafeRunnable
            implements Runnable
        {
        /**
         * Create a {@link SafeRunnable}.
         *
         * @param wrapped  the wrapped {@link Runnable}
         *
         * @throws NullPointerException if the wrapped parameter is {@code null}
         */
        protected SafeRunnable(Runnable wrapped)
            {
            this.wrapped = Objects.requireNonNull(wrapped);
            }

        @Override
        public void run()
            {
            Logger.info("Starting preload task: " + wrapped);
            Instant start = Instant.now();
            try
                {
                wrapped.run();
                Logger.info("Completed preload task: " + wrapped + " in "
                                    + Duration.between(start, Instant.now()));
                }
            catch (Throwable t)
                {
                Logger.err("Preload task " + wrapped + " failed execution after "
                                   + Duration.between(start, Instant.now()), t);
                }
            }

        /**
         * The wrapped runnable.
         */
        private final Runnable wrapped;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The preload tasks to execute.
     */
    private final Collection<Runnable> tasks;

    /**
     * The maximum amount of time to wait for the preload tasks to complete.
     */
    private final Duration timeout;
    }
