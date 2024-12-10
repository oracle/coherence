/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent;

import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;
import com.oracle.coherence.concurrent.internal.SemaphoreStatus;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory methods for local and remote semaphore implementations.
 *
 * @author Vaso Putica  2021.11.30
 * @since 21.12
 */
public class Semaphores
    {
    /**
     * Return a singleton instance of a {@link RemoteSemaphore} with a
     * specified name and number of permits.
     *
     * @param sName   the cluster-wide, unique name of the semaphore
     * @param permits the initial number of permits
     *
     * @return an instance of a {@link RemoteSemaphore} with a specified
     *         name and number of permits
     */
    public static RemoteSemaphore remoteSemaphore(String sName, int permits)
        {
        NamedMap<String, SemaphoreStatus> map = semaphoresMap();
        return f_mapSemaphores.compute(sName, (k, v) ->
            {
            if (v == null)
                {
                int existingInitialPermits = map.invoke(sName, entry ->
                    {
                    if (entry.isPresent())
                        {
                        return entry.getValue().getInitialPermits();
                        }
                    entry.setValue(new SemaphoreStatus(permits));
                    return permits;
                    });
                if (existingInitialPermits != permits)
                    {
                    throw new IllegalArgumentException("The semaphore " + sName + " with a different initial number of permits "
                                                       + existingInitialPermits + " already exists.");
                    }
                return new RemoteSemaphore(k, permits, map);
                }

            if (v.getInitialPermits() != permits)
                {
                throw new IllegalArgumentException("The semaphore " + sName + " with a different initial number of permits "
                                                   + v.getInitialPermits() + " already exists.");
                }
            return v;
            });
        }

    /**
     * Return a singleton instance of a {@link LocalSemaphore} with a
     * specified name and number of permits.
     *
     * @param sName   the process-wide, unique name of the semaphore
     * @param cPermits the initial number of permits
     *
     * @return an instance of a local semaphore with specified name and
     *         number of permits
     */
    public static LocalSemaphore localSemaphore(String sName, int cPermits)
        {
        return f_mapLocalSemaphores.computeIfAbsent(sName, n -> new LocalSemaphore(cPermits));
        }

    /**
     * Clears all {@code semaphores} caches (both local and remote).
     * Used only for testing.
     */
    static void clear()
        {
        f_mapLocalSemaphores.clear();
        f_mapSemaphores.clear();
        semaphoresMap().clear();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return Coherence {@link NamedMap} for the remote semaphores.
     *
     * @return Coherence {@link NamedMap} for the remote semaphores
     */
    public static NamedMap<String, SemaphoreStatus> semaphoresMap()
        {
        return session().getMap("semaphores");
        }

    /**
     * Return Coherence {@link Session} for the Semaphore module.
     *
     * @return Coherence {@link Session} for the Semaphore module
     */
    protected static Session session()
        {
        return Coherence.findSession(SESSION_NAME)
                        .orElseThrow(() ->
                                new IllegalStateException(String.format("The session '%s' has not been initialized", SESSION_NAME)));
        }

    // ----- constants ------------------------------------------------------

    /**
     * The session name.
     */
    public static final String SESSION_NAME = ConcurrentServicesSessionConfiguration.SESSION_NAME;

    /**
     * A process-wide map of semaphores, to avoid creating multiple semaphore
     * instances (and thus sync objects) for the same server-side semaphore.
     */
    private static final Map<String, RemoteSemaphore> f_mapSemaphores = new ConcurrentHashMap<>();

    /**
     * A process-wide cache of named local semaphores.
     */
    private static final Map<String, LocalSemaphore> f_mapLocalSemaphores = new ConcurrentHashMap<>();
    }
