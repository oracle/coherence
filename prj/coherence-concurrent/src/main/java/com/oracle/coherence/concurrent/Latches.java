/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent;

import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;

import com.oracle.coherence.concurrent.internal.LatchCounter;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory methods for various distributed countdown latch implementations.
 *
 * @author as, lh  2021.11.17
 * @since 21.12
 */
public class Latches
    {
    /**
     * Return a singleton instance of a {@link RemoteCountDownLatch}
     * with a specified name and count.
     *
     * @param sName  the cluster-wide, unique name of the countdown latch
     * @param count  the initial count of the countdown latch
     *
     * @return an instance of a {@link RemoteCountDownLatch} with
     *         a specified name and count
     */
    public static RemoteCountDownLatch remoteCountDownLatch(String sName, int count)
        {
        NamedMap<String, LatchCounter> map = latchesMap();

        return  f_mapLatch.compute(sName, (k, v) ->
            {
            if (v == null)
                {
                long existingInitialCount = map.invoke(sName, entry ->
                    {
                    if (entry.isPresent())
                        {
                        return entry.getValue().getInitialCount();
                        }

                    entry.setValue(new LatchCounter(count));
                    return 0L;
                    });

                if (existingInitialCount > 0 && existingInitialCount != count)
                    {
                    throw new IllegalArgumentException("The latch " + sName + " with a different initial count "
                                + existingInitialCount + " already exists.");
                    }

                return new RemoteCountDownLatch(k, count, map);
                }

            if (v.getInitialCount() != count)
                {
                throw new IllegalArgumentException("The latch " + sName + " with a different initial count "
                        + v.getInitialCount() + " already exists.");
                }

            return v;
            });
        }

    /**
     * Return a singleton instance of a {@link LocalCountDownLatch}
     * with a specified name and initial count.
     * <p>
     * The specified latch count is only relevant during the initial latch creation,
     * and is ignored if the latch already exists in the current process. That means
     * that the returned latch instance could have a different count from the one
     * requested.
     *
     * @param sName  the process-wide, unique name of the latch
     * @param count  the initial latch count; ignored if the latch already exists
     *
     * @return an instance of a local countdown latch with a specified name
     */
    public static LocalCountDownLatch localCountDownLatch(String sName, int count)
        {
        return f_mapLatchLocal.computeIfAbsent(sName, k -> new LocalCountDownLatch(count));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Remove the named countdown latch.
     *
     * @param sName  the name of the latch
     */
    protected static void removeCountDownLatch(String sName)
        {
        f_mapLatch.remove(sName);
        }

    /**
     * Return Coherence {@link Session} for the Latches module.
     *
     * @return Coherence {@link Session} for the Latches module
     */
    protected static Session session()
        {
        return Coherence.findSession(SESSION_NAME)
                        .orElseThrow(() ->
                                new IllegalStateException(String.format("The session '%s' has not been initialized", SESSION_NAME)));
        }

    /**
     * Return Coherence {@link NamedMap} containing the countdown latches state.
     *
     * @return Coherence {@link NamedMap} containing the countdown latches state
     */
    protected static NamedMap<String, LatchCounter> latchesMap()
        {
        return session().getMap("latches-countdown");
        }

    // ----- constants ------------------------------------------------------

    /**
     * The session name.
     */
    public static final String SESSION_NAME = ConcurrentServicesSessionConfiguration.SESSION_NAME;

    /**
     * A process-wide map of named local countdown latches.
     */
    private static final Map<String, LocalCountDownLatch> f_mapLatchLocal = new ConcurrentHashMap<>();

    /**
     * A process-wide map of distributed countdown latches, to avoid creating multiple
     * countdown latch instances (and thus sync objects) for the same
     * server-side latch.
     */
    private static final Map<String, RemoteCountDownLatch> f_mapLatch = new ConcurrentHashMap<>();
    }