/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;

import com.oracle.coherence.concurrent.executor.function.Predicates;

import com.oracle.coherence.concurrent.executor.options.Name;

import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import com.tangosol.util.Extractors;
import com.tangosol.util.Filters;
import com.tangosol.util.Processors;
import com.tangosol.util.ResourceRegistry;

import com.tangosol.util.function.Remote;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO.
 *
 * @author rl  8.10.2021
 * @since 21.12
 */
public final class RemoteExecutors
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Instances not allowed.
     */
    private RemoteExecutors()
        {
        }

    // ---- API -------------------------------------------------------------

    /**
     * Creates a thread pool that reuses a fixed number of threads
     * operating off a shared unbounded queue.  At any point, at most
     * {@code nThreads} threads will be active processing tasks.
     * If additional tasks are submitted when all threads are active,
     * they will wait in the queue until a thread is available.
     * If any thread terminates due to a failure during execution
     * prior to shutdown, a new one will take its place if needed to
     * execute subsequent tasks.  The threads in the pool will exist
     * until it is explicitly {@link ExecutorService#shutdown shutdown}.
     *
     * @param sName     the logical name of the {@link RemoteExecutorService}
     * @param nThreads  the number of threads in the pool
     *
     * @return the newly created {@link ExecutorService}
     *
     * @throws IllegalArgumentException if {@code nThreads <= 0} or if
     *                                  {@code sName} is zero-length
     * @throws NullPointerException     if {@code sName} is null
     */
    public static RemoteExecutorService newFixedThreadPool(String sName, int nThreads)
        {
        if (nThreads <= 0)
            {
            throw new IllegalStateException("thread count must be greater than zero");
            }
        validateName(sName);

        Remote.Supplier<ExecutorService> supplier =
                () -> java.util.concurrent.Executors.newFixedThreadPool(nThreads, getThreadFactory(sName));

        return createRemoteExecutorService(Name.of(sName), supplier);
        }

    /**
     * Creates a thread pool that reuses a fixed number of threads
     * operating off a shared unbounded queue, using the provided
     * ThreadFactory to create new threads when needed.  At any point,
     * at most {@code nThreads} threads will be active processing
     * tasks.  If additional tasks are submitted when all threads are
     * active, they will wait in the queue until a thread is
     * available.  If any thread terminates due to a failure during
     * execution prior to shutdown, a new one will take its place if
     * needed to execute subsequent tasks.  The threads in the pool will
     * exist until it is explicitly {@link ExecutorService#shutdown
     * shutdown}.
     *
     * @param sName          the logical name of the {@link RemoteExecutorService}
     * @param nThreads       the number of threads in the pool
     * @param threadFactory  the factory to use when creating new threads
     *
     * @return the newly created {@link ExecutorService}
     *
     * @throws IllegalArgumentException if {@code nThreads <= 0} or if
     *                                  {@code sName} is zero-length
     * @throws NullPointerException     if threadFactory is {@code null}
     */
    public static RemoteExecutorService newFixedThreadPool(String sName, int nThreads, Remote.ThreadFactory threadFactory)
        {
        Objects.requireNonNull(threadFactory);
        if (nThreads <= 0)
            {
            throw new IllegalStateException("thread count must be greater than zero");
            }
        validateName(sName);

        Remote.Supplier<ExecutorService> supplier =
                () -> java.util.concurrent.Executors.newFixedThreadPool(nThreads, threadFactory);

        return createRemoteExecutorService(Name.of(sName), supplier);
        }

    /**
     * Creates an Executor that uses a single worker thread operating
     * off an unbounded queue. (Note however that if this single
     * thread terminates due to a failure during execution prior to
     * shutdown, a new one will take its place if needed to execute
     * subsequent tasks.)  Tasks are guaranteed to execute
     * sequentially, and no more than one task will be active at any
     * given time.
     *
     * @param sName  the logical name of the {@link RemoteExecutorService}
     *
     * @return the newly created single-threaded {@link RemoteExecutorService}
     *
     * @throws IllegalArgumentException if {@code sName} is zero-length
     * @throws NullPointerException     if {@code sName} is null
     */
    public static RemoteExecutorService newSingleThreadExecutor(String sName)
        {
        validateName(sName);

        Remote.Supplier<ExecutorService> supplier =
                () -> java.util.concurrent.Executors.newSingleThreadExecutor(getThreadFactory(sName));

        return createRemoteExecutorService(Name.of(sName), supplier);
        }

    /**
     * Creates an Executor that uses a single worker thread operating
     * off an unbounded queue, and uses the provided ThreadFactory to
     * create a new thread when needed.
     *
     * @param sName          the logical name of the {@link RemoteExecutorService}
     * @param threadFactory  the factory to use when creating new threads
     *
     * @return the newly created single-threaded Executor
     *
     * @throws IllegalArgumentException if {@code sName} is zero-length
     * @throws NullPointerException     if either {@code threadFactory}
     *                                  or {@code sName} is {@code null}
     */
    public static RemoteExecutorService newSingleThreadExecutor(String sName, Remote.ThreadFactory threadFactory)
        {
        Objects.requireNonNull(threadFactory);
        validateName(sName);

        Remote.Supplier<ExecutorService> supplier =
                () -> java.util.concurrent.Executors.newSingleThreadExecutor(threadFactory);

        return createRemoteExecutorService(Name.of(sName), supplier);
        }

    /**
     * Creates a thread pool that creates new threads as needed, but
     * will reuse previously constructed threads when they are
     * available.  These pools will typically improve the performance
     * of programs that execute many short-lived asynchronous tasks.
     * Calls to {@code execute} will reuse previously constructed
     * threads if available. If no existing thread is available, a new
     * thread will be created and added to the pool. Threads that have
     * not been used for sixty seconds are terminated and removed from
     * the cache. Thus, a pool that remains idle for long enough will
     * not consume any resources.
     *
     * TODO
     * Note that pools with similar
     * properties but different details (for example, timeout parameters)
     * may be created using ThreadPoolExecutor constructors.
     *
     * @param sName  the logical name of the {@link RemoteExecutorService}
     *
     * @throws IllegalArgumentException if {@code sName} is zero-length
     * @throws NullPointerException     if {@code sName} is null
     *
     * @return the newly created thread pool
     */
    public static RemoteExecutorService newCachedThreadPool(String sName)
        {
        validateName(sName);

        Remote.Supplier<ExecutorService> supplier =
                () -> java.util.concurrent.Executors.newCachedThreadPool(getThreadFactory(sName));

        return createRemoteExecutorService(Name.of(sName), supplier);
        }

    /**
     * Creates a thread pool that creates new threads as needed, but
     * will reuse previously constructed threads when they are
     * available, and uses the provided
     * ThreadFactory to create new threads when needed.
     *
     * @param sName          the logical name of the {@link RemoteExecutorService}
     * @param threadFactory  the factory to use when creating new threads
     *
     * @return the newly created {@link ExecutorService}
     *
     * @throws IllegalArgumentException if {@code sName} is zero-length
     * @throws NullPointerException     if either {@code threadFactory} or
     *                                  {@code sName} is {@code null}
     */
    public static RemoteExecutorService newCachedThreadPool(String sName, Remote.ThreadFactory threadFactory)
        {
        Objects.requireNonNull(threadFactory);
        validateName(sName);

        Remote.Supplier<ExecutorService> supplier =
                () -> java.util.concurrent.Executors.newCachedThreadPool(threadFactory);

        return createRemoteExecutorService(Name.of(sName), supplier);
        }

    /**
     * Creates a work-stealing thread pool using the number of
     * {@linkplain Runtime#availableProcessors available processors}
     * as its target parallelism level.
     *
     * @return the newly created {@link ExecutorService}
     *
     * @throws IllegalArgumentException if {@code sName} is zero-length
     * @throws NullPointerException     if {@code sName} is {@code null}
     *
     * @see #newWorkStealingPool(String, int)
     */
    public static RemoteExecutorService newWorkStealingPool(String sName)
        {
        validateName(sName);

        Remote.Supplier<ExecutorService> supplier = java.util.concurrent.Executors::newWorkStealingPool;

        return createRemoteExecutorService(Name.of(sName), supplier);
        }

    /**
     * Creates a thread pool that maintains enough threads to support
     * the given parallelism level, and may use multiple queues to
     * reduce contention. The parallelism level corresponds to the
     * maximum number of threads actively engaged in, or available to
     * engage in, task processing. The actual number of threads may
     * grow and shrink dynamically. A work-stealing pool makes no
     * guarantees about the order in which submitted tasks are
     * executed.
     *
     * @param nParallelism  the targeted parallelism level
     *
     * @return the newly created {@link ExecutorService}
     *
     * @throws IllegalArgumentException if {@code sName} is zero-length or
     *                                  if {@code nParallelism <= 0}
     * @throws NullPointerException     if {@code sName} is {@code null}
     */
    public static RemoteExecutorService newWorkStealingPool(String sName, int nParallelism)
        {
        if (nParallelism <= 0)
            {
            throw new IllegalArgumentException("nParallelism must be greater than zero");
            }
        validateName(sName);

        Remote.Supplier<ExecutorService> supplier =
                () -> java.util.concurrent.Executors.newWorkStealingPool(nParallelism);

        return createRemoteExecutorService(Name.of(sName), supplier);
        }

    // ----- inner class: DefaultNamedThreadFactory

    private static class DefaultNamedThreadFactory implements Remote.ThreadFactory
        {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        private final ThreadGroup group;
        private final String namePrefix;

        DefaultNamedThreadFactory(String sName)
            {
            group = Thread.currentThread().getThreadGroup();
            namePrefix = sName + "-pool-" +
                         poolNumber.getAndIncrement() +
                         "-thread-";
            }

        public Thread newThread(Runnable r)
            {
            Thread t = new Thread(group, r,
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            t.setDaemon(true);
            return t;
            }
        }

    // ----- helper methods -------------------------------------------------

    private static RemoteExecutorService createRemoteExecutorService(Name name, Remote.Supplier<ExecutorService> supplier)
        {
        ClusteredExecutorService s = new NamedClusteredExecutorService(name);
        s.register(supplier.get(), supplier, name);
        return s;
        }

    private static Remote.ThreadFactory getThreadFactory(String sName)
        {
        return new DefaultNamedThreadFactory(sName);
        }

    private static void validateName(String sName)
        {
        Objects.requireNonNull(sName, "RemoteExecutorService must have a name");

        final String sNameLocal = sName.trim();
        if (sNameLocal.isEmpty())
            {
            throw new IllegalStateException("RemoteExecutorService name cannot be zero-length");
            }
        }

    /**
     * Returns the locally registered {@link TaskExecutorService}, if any.
     *
     * @return the locally registered {@link TaskExecutorService} or
     *         {@code null} if not registered
     *
     * @throws IllegalStateException if no session is found; most likely means
     *                               this API was called without initializing
     *                               Coherence using the Bootstrap API
     */
    static TaskExecutorService getLocalExecutorService()
        {
        Session          session  = session();
        ResourceRegistry registry = session.getResourceRegistry();

        return registry.getResource(ClusteredExecutorService.class, ClusteredExecutorService.class.getSimpleName());
        }

    static Set<Name> getKnownExecutorNames()
        {
        return Collections.unmodifiableSet(getExecutorNameMap().keySet());
        }

    @SuppressWarnings("unchecked")
    static ConcurrentHashMap<Name, Name> getExecutorNameMap()
        {
        ResourceRegistry registry = session().getResourceRegistry();
        ConcurrentHashMap<Name, Name> localExecutorNames =
                registry.getResource(ConcurrentHashMap.class, "KnownExecutors");
        if (localExecutorNames == null)
            {
            synchronized (registry)
                {
                localExecutorNames = registry.getResource(ConcurrentHashMap.class, "KnownExecutors");
                if (localExecutorNames == null)
                    {
                    localExecutorNames = new ConcurrentHashMap<>();
                    registry.registerResource(ConcurrentHashMap.class, "KnownExecutors", localExecutorNames);
                    }
                }
            }
        return localExecutorNames;
        }

    static boolean registerExecutorName(TaskExecutorService.Registration.Option name)
        {
        if (name instanceof Name)
            {
            Name nameLocal = (Name) name;
            return getExecutorNameMap().putIfAbsent(nameLocal, nameLocal) == null;
            }
        return false;
        }

    /**
     * Return the Coherence {@link Session session} for the {@code RemoteExecutors}
     * module.
     *
     * @return the Coherence {@link Session session} for the {@code RemoteExecutors}
     *         module
     *
     * @throws IllegalStateException if no session is found; most likely means
     *                               this API was called without initializing
     *                               Coherence using the Bootstrap API
     */
    static Session session()
        {
        return Coherence.findSession(SESSION_NAME)
                .orElseThrow(() -> new IllegalStateException(
                                             String.format("The session '%s' has not been initialized", SESSION_NAME)));
        }

    // ----- inner class: NamedExecutorServiceDispatcher --------------------

    /**
     * TODO
     */
    protected static class NamedClusteredExecutorService
            extends ClusteredExecutorService
        {
        // ----- constructors -----------------------------------------------

        public NamedClusteredExecutorService(Name name)
            {
            super(RemoteExecutors.session());
            f_name = name;
            }

        // ----- ClusteredExecutorService methods ---------------------------

        public <T> Task.Orchestration<T> orchestrate(Task<T> task)
            {
            return new NamedOrchestration<>(this, f_name, task);
            }

        protected void init(CacheService cacheService)
            {
            m_cacheService = cacheService;
            }

        public void shutdown()
            {
            super.shutdown();

            NamedCache<String, ClusteredExecutorInfo> registrations = m_cacheService.ensureCache(ClusteredExecutorInfo.CACHE_NAME, null);
            registrations.invokeAll(Processors.remove(Filters.equal(Extractors.extract("getOption", Name.class, null), f_name)));
            }

        // ----- data members -----------------------------------------------

        protected final Name f_name;
        }

    // ----- inner class: NamedOrchestration --------------------------------

    protected static class NamedOrchestration<T>
            extends ClusteredOrchestration<T>
        {
        public NamedOrchestration(ClusteredExecutorService clusteredExecutorService, Name name, Task<T> task)
            {
            super(clusteredExecutorService, task);
            filter(Predicates.has(name));
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The Coherence Executor {@link Session session} name.
     */
    public static final String SESSION_NAME = ConcurrentServicesSessionConfiguration.SESSION_NAME;

    /**
     * System property that may be set to override the default executor configuration
     * with the configuration specified by the value of the property.
     */
    public static final String EXECUTOR_CONFIG_OVERRIDE = "coherence.executor.config.override";
    }
