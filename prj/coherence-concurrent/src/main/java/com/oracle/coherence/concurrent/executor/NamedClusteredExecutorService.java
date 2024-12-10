/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;

import com.oracle.coherence.concurrent.executor.function.Predicates;

import com.oracle.coherence.concurrent.executor.options.Name;

import com.oracle.coherence.concurrent.executor.util.Caches;

import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;

import com.tangosol.net.Session;

import com.tangosol.net.ViewBuilder;
import com.tangosol.util.Extractors;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;

import com.tangosol.util.function.Remote;

import java.util.List;

import java.util.Objects;

import java.util.concurrent.RejectedExecutionException;

/**
 * A {@link ClusteredExecutorService} that dispatches to executors
 * associated with a logical name.
 *
 * @author rl  11.20.21
 * @since 21.12
 */
public class NamedClusteredExecutorService
        extends ClusteredExecutorService
    {
    // ----- constructors -----------------------------------------------

    /**
     * Constructs a {@code NamedClusteredExecutorService} for the given
     * {@link Name}.
     *
     * @param name  the executor service name
     */
    @SuppressWarnings("unchecked")
    public NamedClusteredExecutorService(Name name)
        {
        super(session());

        f_name = name;

        Filter<?>         filter  = Filters.equal(Extractors.extract("getExecutorName()"), f_name.getName());
        ViewBuilder<?, ?> builder = Caches.executors(session()).view();

        m_viewNamed = builder.keys().filter(filter).build();
        }

    // ----- ClusteredExecutorService methods ---------------------------

    @Override
    public <T> Task.Orchestration<T> orchestrate(Task<T> task)
        {
        return new NamedOrchestration<>(this, f_name, Objects.requireNonNull(task));
        }

    @Override
    public void execute(Remote.Runnable command)
        {
        if (m_viewNamed.isEmpty())
            {
            throw new RejectedExecutionException(String.format("No RemoteExecutor service available by name [%s]", f_name));
            }
        super.execute(command);
        }

    @Override
    public void shutdown()
        {
        release();

        super.shutdown();
        }

    @Override
    public List<Runnable> shutdownNow()
        {
        release();

        return super.shutdownNow();
        }

    @Override
    protected void init(CacheService cacheService)
        {
        m_cacheService = cacheService;
        }

    /**
     * Return the {@link Name} of this {@link NamedClusteredExecutorService}.
     *
     * @return the {@link Name} of this {@link NamedClusteredExecutorService}
     */
    public Name getName()
        {
        return f_name;
        }

    // ----- inner class: NamedOrchestration --------------------------------

    /**
     * {@link ClusteredOrchestration} extension that orchestrates only to
     * executors associated with the given {@link Name name}.
     *
     * @param <T>  the type of result produced by the {@link Task}
     */
    protected static class NamedOrchestration<T>
            extends ClusteredOrchestration<T>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Creates a NamedOrchestration.
         *
         * @param clusteredExecutorService the {@link ClusteredExecutorService}
         * @param name                     the executor {@link Name}
         * @param task                     the {@link Task} to execute
         */
        public NamedOrchestration(ClusteredExecutorService clusteredExecutorService, Name name, Task<T> task)
            {
            super(clusteredExecutorService, task);
            m_strategyBuilder.m_predicate = Predicates.has(name);
            }

        /**
         * As this orchestration installs a filter as part of its required
         * function, any user predicates will be <em>and</em>'d together.
         * @param predicate  the {@link TaskExecutorService.ExecutorInfo} predicate
         *
         * @return this orchestration
         */
        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public Task.Orchestration<T> filter(Remote.Predicate<? super ExecutorInfo> predicate)
            {
            Remote.Predicate<?> predExisting = m_strategyBuilder.m_predicate;
            if (predExisting != null)
                {
                m_strategyBuilder.m_predicate = predExisting.and((Remote.Predicate) predicate);
                }
            return this;
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the Coherence {@link Session session} for the {@code coherence-concurrent}
     * module.
     *
     * @return the Coherence {@link Session session} for the {@code coherence-concurrent}
     *         module
     *
     * @throws IllegalStateException if no session is found; most likely means
     *                               this API was called without initializing
     *                               Coherence using the Bootstrap API
     */
    protected static Session session()
        {
        String sSessionName = ConcurrentServicesSessionConfiguration.SESSION_NAME;
        return Coherence.findSession(sSessionName)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("The session '%s' has not been initialized", sSessionName)));
        }

    /**
     * Releases resources associated with this {@code NamedExecutorService}.
     */
    protected void release()
        {
        if (m_viewNamed != null)
            {
            m_viewNamed.release();
            m_viewNamed = null;
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The executor's logical name.
     */
    protected final Name f_name;

    /**
     * Local view of executors matching this executor's name.
     * If the map is empty, it means there is no registered executors
     * for the given name causing attempted executions to be rejected.
     */
    protected NamedCache<?, ?> m_viewNamed;
    }
