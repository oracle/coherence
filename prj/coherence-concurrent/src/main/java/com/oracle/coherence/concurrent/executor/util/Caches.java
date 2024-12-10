/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor.util;

import com.oracle.coherence.concurrent.executor.ClusteredAssignment;
import com.oracle.coherence.concurrent.executor.ClusteredExecutorInfo;
import com.oracle.coherence.concurrent.executor.ClusteredProperties;
import com.oracle.coherence.concurrent.executor.ClusteredTaskManager;

import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import java.util.Objects;

/**
 * Utility class for easy access to the various caches used by the
 * {@code Executor} service.
 *
 * @since 22.06
 * @author rl  2022.5.25
 */
@SuppressWarnings("rawtypes")
public final class Caches
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Instances aren't allowed.
     */
    private Caches()
        {
        }

    // ----- public methods -------------------------------------------------

    /**
     * Obtains the cache used to store {@link ClusteredTaskManager} instances.
     *
     * @param service  the {@link CacheService} to use to obtain a cache reference
     *
     * @return the cache used to store {@link ClusteredTaskManager} instances
     *
     * @throws NullPointerException if {@code service} is {@code null}
     */
    public static NamedCache tasks(CacheService service)
        {
        return getCache(service, TASKS_CACHE_NAME);
        }

    /**
     * Obtains the cache used to store {@link ClusteredAssignment} instances.
     *
     * @param service  the {@link CacheService} to use to obtain a cache reference
     *
     * @return the cache used to store {@link ClusteredAssignment} instances
     *
     * @throws NullPointerException if {@code service} is {@code null}
     */
    public static NamedCache assignments(CacheService service)
        {
        return getCache(service, ASSIGNMENTS_CACHE_NAME);
        }

    /**
     * Obtains the cache used to store {@link ClusteredExecutorInfo} instances.
     *
     * @param service  the {@link CacheService} to use to obtain a cache reference
     *
     * @return the cache used to store {@link ClusteredExecutorInfo} instances
     *
     * @throws NullPointerException if {@code service} is {@code null}
     */
    public static NamedCache executors(CacheService service)
        {
        return getCache(service, EXECUTORS_CACHE_NAME);
        }

    /**
     * Obtains the cache used to store {@link ClusteredProperties} instances.
     *
     * @param service  the {@link CacheService} to use to obtain a cache reference
     *
     * @return the cache used to store {@link ClusteredProperties} instances
     *
     * @throws NullPointerException if {@code service} is {@code null}
     */
    public static NamedCache properties(CacheService service)
        {
        return getCache(service, PROPERTIES_CACHE_NAME);
        }

    // ----------------------------------------------------------------------

    /**
     * Obtains the cache used to store {@link ClusteredTaskManager} instances.
     *
     * @param session  the {@link Session} to use to obtain a cache reference
     *
     * @return the cache used to store {@link ClusteredTaskManager} instances
     *
     * @throws NullPointerException if {@code session} is {@code null}
     */
    public static NamedCache tasks(Session session)
        {
        return getCache(session, TASKS_CACHE_NAME);
        }

    /**
     * Obtains the cache used to store {@link ClusteredAssignment} instances.
     *
     * @param session  the {@link Session} to use to obtain a cache reference
     *
     * @return the cache used to store {@link ClusteredAssignment} instances
     *
     * @throws NullPointerException if {@code session} is {@code null}
     */
    public static NamedCache assignments(Session session)
        {
        return getCache(session, ASSIGNMENTS_CACHE_NAME);
        }

    /**
     * Obtains the cache used to store {@link ClusteredExecutorInfo} instances.
     *
     * @param session  the {@link Session} to use to obtain a cache reference
     *
     * @return the cache used to store {@link ClusteredExecutorInfo} instances
     *
     * @throws NullPointerException if {@code session} is {@code null}
     */
    public static NamedCache executors(Session session)
        {
        return getCache(session, EXECUTORS_CACHE_NAME);
        }

    /**
     * Obtains the cache used to store {@link ClusteredProperties} instances.
     *
     * @param session  the {@link Session} to use to obtain a cache reference
     *
     * @return the cache used to store {@link ClusteredProperties} instances
     *
     * @throws NullPointerException if {@code session} is {@code null}
     */
    public static NamedCache properties(Session session)
        {
        return getCache(session, PROPERTIES_CACHE_NAME);
        }

    // ----------------------------------------------------------------------

    /**
     * Obtains the cache used to store {@link ClusteredTaskManager} instances.
     *
     * @param factory  the {@link ConfigurableCacheFactory} to use to obtain a cache reference
     *
     * @return the cache used to store {@link ClusteredTaskManager} instances
     *
     * @throws NullPointerException if {@code factory} is {@code null}
     */
    public static NamedCache tasks(ConfigurableCacheFactory factory)
        {
        return getCache(factory, TASKS_CACHE_NAME);
        }

    /**
     * Obtains the cache used to store {@link ClusteredAssignment} instances.
     *
     * @param factory  the {@link ConfigurableCacheFactory} to use to obtain a cache reference
     *
     * @return the cache used to store {@link ClusteredAssignment} instances
     *
     * @throws NullPointerException if {@code factory} is {@code null}
     */
    public static NamedCache assignments(ConfigurableCacheFactory factory)
        {
        return getCache(factory, ASSIGNMENTS_CACHE_NAME);
        }

    /**
     * Obtains the cache used to store {@link ClusteredExecutorInfo} instances.
     *
     * @param factory  the {@link ConfigurableCacheFactory} to use to obtain a cache reference
     *
     * @return the cache used to store {@link ClusteredAssignment} instances
     *
     * @throws NullPointerException if {@code factory} is {@code null}
     */
    public static NamedCache executors(ConfigurableCacheFactory factory)
        {
        return getCache(factory, EXECUTORS_CACHE_NAME);
        }

    /**
     * Obtains the cache used to store {@link ClusteredProperties} instances.
     *
     * @param factory  the {@link ConfigurableCacheFactory} to use to obtain a cache reference
     *
     * @return the cache used to store {@link ClusteredProperties} instances
     *
     * @throws NullPointerException if {@code factory} is {@code null}
     */
    public static NamedCache properties(ConfigurableCacheFactory factory)
        {
        return getCache(factory, PROPERTIES_CACHE_NAME);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtains a reference to the desired {@link NamedCache} using the
     * provided {@link CacheService}.
     *
     * @param service     the {@link CacheService} to use to obtain a cache reference
     * @param sCacheName  the cache name
     *
     * @return the {@link NamedCache}
     *
     * @throws NullPointerException if either {@code service} or {@code sCacheName}
     *                              is {@code null}
     */
    private static NamedCache getCache(CacheService service, String sCacheName)
        {
        Objects.requireNonNull(service);

        return service.ensureCache(sCacheName, null);
        }

    /**
     * Obtains a reference to the desired {@link NamedCache} using the
     * provided {@link Session}.
     *
     * @param session     the {@link Session} to use to obtain a cache reference
     * @param sCacheName  the cache name
     *
     * @return the {@link NamedCache}
     *
     * @throws NullPointerException if either {@code session} or {@code sCacheName}
     *                              is {@code null}
     */
    private static NamedCache getCache(Session session, String sCacheName)
        {
        Objects.requireNonNull(session);

        return session.getCache(sCacheName);
        }

    /**
     * Obtains a reference to the desired {@link NamedCache} using the
     * provided {@link ConfigurableCacheFactory}.
     *
     * @param factory     the {@link ConfigurableCacheFactory} to use to obtain a cache reference
     * @param sCacheName  the cache name
     *
     * @return the {@link NamedCache}
     *
     * @throws NullPointerException if either {@code factory} or {@code sCacheName}
     *                              is {@code null}
     */
    private static NamedCache getCache(ConfigurableCacheFactory factory, String sCacheName)
        {
        Objects.requireNonNull(factory);

        return factory.ensureCache(sCacheName, null);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The {@link NamedCache} in which {@link ClusteredTaskManager} instances will be placed.
     */
    public static String TASKS_CACHE_NAME = "executor-tasks";

    /**
     * The {@link NamedCache} in which {@link ClusteredAssignment} instances will be placed.
     */
    public static String ASSIGNMENTS_CACHE_NAME = "executor-assignments";

    /**
     * The {@link NamedCache} in which {@link ClusteredExecutorInfo} instances will be placed.
     */
    public static final String EXECUTORS_CACHE_NAME = "executor-executors";

    /**
     * The {@link NamedCache} in which {@link ClusteredProperties} instances will be placed.
     */
    public static String PROPERTIES_CACHE_NAME = "executor-properties";
    }
