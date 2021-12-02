/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.config.ConcurrentConfiguration;
import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;

import com.oracle.coherence.concurrent.executor.management.ExecutorMBean;

import com.oracle.coherence.concurrent.executor.options.Name;

import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;

import com.tangosol.net.management.AnnotatedStandardEmitterMBean;
import com.tangosol.net.management.Registry;

import com.tangosol.util.WrapperException;

import java.util.Objects;

import java.util.concurrent.ConcurrentHashMap;

import javax.management.NotCompliantMBeanException;

/**
 * Executor service utility class.
 *
 * @author rl  11.20.21
 * @since 21.12
 */
public final class ExecutorsHelper
    {
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
    static Session session()
        {
        return Coherence.findSession(SESSION_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("The session '%s' has not been initialized", SESSION_NAME)));
        }

    /**
     * Obtain a RemoteExecutor for the specified {@code name}.  If no registered
     * executor exists, this method will return {@code null}.
     *
     * @param sName  the executor name
     *
     * @return the {@link RemoteExecutor}, if registered, or {@code null} if
     *         not
     *
     * @throws NullPointerException if {@code sName} is {@code null}
     */
    static RemoteExecutor ensureRemoteExecutor(String sName)
        {
        Objects.requireNonNull(sName);

        if (ConcurrentConfiguration.get().isExecutorNameRegistered(sName))
            {
            return f_mapLiveExecutors.computeIfAbsent(sName, s -> new NamedClusteredExecutorService(Name.of(sName)));
            }
        return null;
        }

    /**
     * Registers the provided MBean for the specified executor.
     *
     * @param service  the cache service
     * @param mbean    the mbean to register
     * @param sName    the executor name
     *
     * @throws NullPointerException if any of {@code service}, {@code mbean},
     *                              or {@code sName} is {@code null}
     */
    static void registerExecutorMBean(CacheService service, ExecutorMBean mbean, String sName)
        {
        Objects.requireNonNull(service, "service cannot be null");
        Objects.requireNonNull(mbean,   "mbean cannot be null");
        Objects.requireNonNull(sName,   "sName cannot be null");

        Cluster  cluster  = service.getCluster();
        Registry registry = cluster.getManagement();

        if (registry != null)
            {
            String sMbeanName = getExecutorServiceMBeanName(registry, sName);

            try
                {
                registry.register(sMbeanName, new AnnotatedStandardEmitterMBean(mbean, ExecutorMBean.class));
                }
            catch (NotCompliantMBeanException e)
                {
                throw new WrapperException(e);
                }
            }
        }

    /**
     * Unregisters the MBean for the specified executor.
     *
     * @param service  the cache service
     * @param sName    the executor name
     *
     * @throws NullPointerException if either {@code service} or
     *                             {@code sName} is {@code null}
     */
    static void unregisterExecutiveServiceMBean(CacheService service, String sName)
        {
        Objects.requireNonNull(service, "service cannot be null");
        Objects.requireNonNull(sName,   "sName cannot be null");

        Registry registry = service.getCluster().getManagement();

        if (registry != null)
            {
            String sMBeanName = getExecutorServiceMBeanName(registry, sName);

            registry.unregister(sMBeanName);
            }
        }

    /**
     * Get the MBean name for the {@code named} executor.
     *
     * @param registry  the management registry
     * @param sName     the executor name
     *
     * @return the MBean name for the {@code named} executor
     *
     * @throws NullPointerException if either {@code registry} or
     *                             {@code sName} is {@code null}
     */
    static String getExecutorServiceMBeanName(Registry registry, String sName)
        {
        Objects.requireNonNull(registry, "registry cannot be null");
        Objects.requireNonNull(sName,    "sName cannot be null");

        return registry.ensureGlobalName(EXECUTOR_TYPE + EXECUTOR_NAME + sName);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The Coherence Executor {@link Session session} name.
     */
    public static final String SESSION_NAME = ConcurrentServicesSessionConfiguration.SESSION_NAME;

    /**
     * String representing the "type" part of <code>ObjectName</code> for the ExecutorMBean.
     */
    public static final String EXECUTOR_TYPE = "type=Executor";

    /**
     * The executor name.
     */
    private static final String EXECUTOR_NAME = ",name=";

    // ----- data members ---------------------------------------------------

    /**
     * Current executors in use, keyed by name.
     */
    private static final ConcurrentHashMap<String, NamedClusteredExecutorService> f_mapLiveExecutors =
            new ConcurrentHashMap<>();
    }
