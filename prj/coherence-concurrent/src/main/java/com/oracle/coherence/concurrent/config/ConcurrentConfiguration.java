/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config;

import com.oracle.coherence.concurrent.executor.ClusteredExecutorService;

import com.oracle.coherence.concurrent.executor.options.CloseExecutor;
import com.oracle.coherence.concurrent.executor.options.Description;
import com.oracle.coherence.concurrent.executor.options.Name;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A simple holder for the parsing result of an {@code coherence-concurrent}
 * configuration artifacts.
 *
 * @author rl  11.20.21
 * @since 21.12
 */
public final class ConcurrentConfiguration
    {
    // ----- constructors ---------------------------------------------------

    /**
     * No instances.
     */
    private ConcurrentConfiguration()
        {
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the {@link List list} of parsed {@link NamedExecutorService}s.
     *
     * @return the {@link List list} of parsed {@link NamedExecutorService}s
     */
    public List<NamedExecutorService> getNamedExecutorServices()
        {
        return Collections.unmodifiableList(new ArrayList<>(f_mapNamedExecutorServices.values()));
        }

    /**
     * Returns the {@link NamedExecutorService} for the given name, if any.
     *
     * @param sName  the name of the {@link NamedExecutorService}
     *
     * @return the {@link NamedExecutorService} for the given name, if any
     *
     * @throws NullPointerException if {@code sName} is {@code null}
     */
    public NamedExecutorService getNamedExecutorService(String sName)
        {
        Objects.requireNonNull(sName, "sName cannot be null");

        return f_mapNamedExecutorServices.get(sName);
        }

    /**
     * Sets the local {@link ClusteredExecutorService} with which {@link NamedExecutorService}
     * will be registered with.
     *
     * @param executorService  the local {@link ClusteredExecutorService} with
     *                         which {@link NamedExecutorService} will be registered with
     *
     * @throws NullPointerException if {@code executorService} is {@code null}
     */
    public synchronized void setExecutorService(ClusteredExecutorService executorService)
        {
        Objects.requireNonNull(executorService, "executorService cannot be null");

        m_executorService = executorService;

        for (NamedExecutorService service : f_mapNamedExecutorServices.values())
            {
            executorService.register(service.getExecutorService(),
                                     Name.of(service.getName()), new CloseExecutor(), Description.of(service.getDescription()));
            }
        }

    /**
     * Reset the ConcurrentConfiguration to it's initial state.
     */
    public synchronized void reset()
        {
        m_executorService = null;
        f_mapNamedExecutorServices.clear();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Add the specified {@link NamedExecutorService} and register it
     * with the local {@link ClusteredExecutorService}.
     *
     * @param service  the {@link NamedExecutorService} to add
     *
     * @throws IllegalArgumentException if an executor has already been registered
     *                                  with the same name
     * @throws NullPointerException     if {@code service} is {@code null}
     */
    public synchronized void addNamedExecutorService(NamedExecutorService service)
        {
        Objects.requireNonNull(service, "service cannot be null");

        String               sName    = service.getName();
        NamedExecutorService existing = f_mapNamedExecutorServices.putIfAbsent(service.getName(), service);

        if (existing != null)
            {
            throw new IllegalArgumentException(
                    String.format("Named executor service already registered to name [%s]", sName));
            }

        // it may be that NamedExecutors are discovered after the executor service has started,
        // when this is the case, register the executors on-the-fly
        if (m_executorService != null)
            {
            m_executorService.register(service.getExecutorService(), Name.of(sName), new CloseExecutor());
            }
        }

    /**
     * Return {@code true} if the specific name has an executor
     * associated with it.
     *
     * @param sName  the name to verify
     *
     * @return {@code true} if the specific name has an executor
     *         associated with it
     *
     * @throws NullPointerException if {@code sName} is null
     */
    public boolean isExecutorNameRegistered(String sName)
        {
        Objects.requireNonNull(sName, "sName cannot be null");

        return f_mapNamedExecutorServices.containsKey(sName);
        }

    /**
     * Return the {@code ConcurrentConfiguration} for this VM.
     *
     * @return the {@code ConcurrentConfiguration} for this VM
     */
    public static ConcurrentConfiguration get()
        {
        return INSTANCE;
        }

    // ----- constants ------------------------------------------------------

    private static final ConcurrentConfiguration INSTANCE = new ConcurrentConfiguration();

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Map map} of {@link NamedExecutorService}s to their given names.
     */
    private final Map<String , NamedExecutorService> f_mapNamedExecutorServices = new HashMap<>();

    /**
     * The local {@link ClusteredExecutorService} to which executors will be
     * registered to.
     */
    private volatile ClusteredExecutorService m_executorService;
    }
