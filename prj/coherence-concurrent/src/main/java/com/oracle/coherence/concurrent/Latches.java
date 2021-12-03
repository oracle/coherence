/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent;

import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;

import com.oracle.coherence.concurrent.internal.LatchCounter;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Factory methods for various distributed countdown latch implementations.
 *
 * @author as, lh  2021.11.17
 * @since 21.12
 */
public class Latches
    {
    /**
     * Return a singleton instance of a {@link DistributedCountDownLatch}
     * with a specified name and count.
     *
     * @param sName  the cluster-wide, unique name of the countdown latch
     * @param count  the initial count of the countdown latch
     *
     * @return an instance of a {@link DistributedCountDownLatch} with
     *         a specified name and count
     */
    public static DistributedCountDownLatch countDownLatch(String sName, int count)
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

                return new DistributedCountDownLatch(k, count, map);
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
     * Return a singleton instance of a local {@link CountDownLatch}
     * with a specified name. If the named latch does not exist, return null.
     *
     * @param sName  the process-wide, unique name of the latch
     *
     * @return an instance of a local countdown latch with a specified name
     *         or null if one does not exist
     */
    public static CountDownLatch localCountDownLatch(String sName)
        {
        return f_mapLatchLocal.get(sName);
        }

    /**
     * Return a singleton instance of a local {@link CountDownLatch}
     * with a specified name and count.  If one is already exist, then it
     * is overridden.
     *
     * @param sName  the process-wide, unique name of the latch
     * @param count  the latch count
     *
     * @return an instance of a local countdown latch with a specified name
     */
    public static CountDownLatch localCountDownLatch(String sName, int count)
        {
        return  f_mapLatchLocal.compute(sName, (k, v) ->
            {
            return new CountDownLatch(count);
            });
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
    private static final Map<String, CountDownLatch> f_mapLatchLocal = new ConcurrentHashMap<>();

    /**
     * A process-wide map of distributed countdown latches, to avoid creating multiple
     * countdown latch instances (and thus sync objects) for the same
     * server-side latch.
     */
    private static final Map<String, DistributedCountDownLatch> f_mapLatch = new ConcurrentHashMap<>();
    }