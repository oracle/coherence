/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.executor.function.Predicates;

import com.oracle.coherence.concurrent.executor.options.Name;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Extractors;
import com.tangosol.util.Filters;
import com.tangosol.util.Processors;

/**
 * A {@link ClusteredExecutorService} that dispatches to executors
 * associated with a logical name.
 *
 * @author rl  11.20.21
 * @since 21.12
 */
class NamedClusteredExecutorService
        extends ClusteredExecutorService
    {
    // ----- constructors -----------------------------------------------

    /**
     * Constructs a {@code NamedClusteredExecutorService} for the given
     * {@link Name}.
     *
     * @param name  the executor service name
     */
    public NamedClusteredExecutorService(Name name)
        {
        super(ExecutorsHelper.session());
        f_name = name;
        }

    // ----- ClusteredExecutorService methods ---------------------------

    @Override
    public <T> Task.Orchestration<T> orchestrate(Task<T> task)
        {
        return new NamedOrchestration<>(this, f_name, task);
        }

    @Override
    protected void init(CacheService cacheService)
        {
        m_cacheService = cacheService;
        }

    @SuppressWarnings("unchecked")
    @Override
    public void shutdown()
        {
        super.shutdown();

        NamedCache<String, ClusteredExecutorInfo> registrations = m_cacheService.ensureCache(ClusteredExecutorInfo.CACHE_NAME, null);
        registrations.invokeAll(Processors.remove(Filters.equal(Extractors.extract("getOption", Name.class, null), f_name)));
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
        public NamedOrchestration(ClusteredExecutorService clusteredExecutorService, Name name, Task<T> task)
            {
            super(clusteredExecutorService, task);
            filter(Predicates.has(name));
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The executor's logical name.
     */
    protected final Name f_name;
    }
