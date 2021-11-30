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

import java.util.concurrent.ConcurrentHashMap;

import javax.management.NotCompliantMBeanException;

/**
 * TODO
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

    static RemoteExecutor ensureRemoteExecutor(String sName)
        {
        if (ConcurrentConfiguration.get().isExecutorNameRegistered(sName))
            {
            return f_mapLiveExecutors.computeIfAbsent(sName, s -> new NamedClusteredExecutorService(Name.of(sName)));
            }
        return null;
        }

    static void registerExecutorMBean(CacheService service, ExecutorMBean mbean, ClusteredExecutorInfo info)
        {
        Cluster           cluster       = service.getCluster();
        Registry          registry      = cluster.getManagement();
        String            sExecutorName = info.getExecutorName();

        if (registry != null)
            {
            String sName = getExecutorServiceMBeanName(registry, sExecutorName);

            try
                {
                registry.register(sName, new AnnotatedStandardEmitterMBean(mbean, ExecutorMBean.class));
                }
            catch (NotCompliantMBeanException e)
                {
                throw new WrapperException(e);
                }
            }
        }

    static void unregisterExecutiveServiceMBean(CacheService service, String sName)
        {
        Registry registry = service.getCluster().getManagement();

        if (registry != null)
            {
            String sMBeanName = getExecutorServiceMBeanName(registry, sName);

            registry.unregister(sMBeanName);
            }
        }

    private static String getExecutorServiceMBeanName(Registry registry, String sName)
        {
        return registry.ensureGlobalName(EXECUTOR_TYPE + EXECUTOR_NAME + sName);
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

    /**
     * String representing the "type" part of <code>ObjectName</code> for the ExecutorMBean.
     */
    public static final String EXECUTOR_TYPE = "type=Executor";

    private static final String EXECUTOR_NAME = ",name=";

    // ----- data members ---------------------------------------------------

    private static final ConcurrentHashMap<String, NamedClusteredExecutorService> f_mapLiveExecutors =
            new ConcurrentHashMap<>();
    }
