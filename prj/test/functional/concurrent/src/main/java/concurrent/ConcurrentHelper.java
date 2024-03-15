/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.base.Exceptions;

import com.oracle.coherence.concurrent.Latches;
import com.oracle.coherence.concurrent.Semaphores;

import com.oracle.coherence.concurrent.atomic.Atomics;

import com.oracle.coherence.concurrent.internal.LatchCounter;

import com.tangosol.net.NamedMap;

import java.lang.reflect.Method;

import org.hamcrest.core.Is;

/**
 * Utilities to access protected methods in Coherence Concurrent.
 */
public class ConcurrentHelper
    {
    /**
     * Clear all local atomics.
     */
    public static void resetAtomics()
        {
        try
            {
            Method method = Atomics.class.getDeclaredMethod("reset");
            method.setAccessible(true);
            method.invoke(null);
            }
        catch (Throwable e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Clear all local semaphores.
     */
    public static void clearSemaphores()
        {
        try
            {
            Method method = Semaphores.class.getDeclaredMethod("clear");
            method.setAccessible(true);
            method.invoke(null);
            }
        catch (Throwable e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Return Coherence {@link NamedMap} containing the countdown latches state.
     *
     * @return Coherence {@link NamedMap} containing the countdown latches state
     */
    public static NamedMap<String, LatchCounter> latchesMap()
        {
        return LatchesWrapper.latchesMap();
        }

    /**
     * A wrapper around {@link Latches} to expose protected methods.
     */
    public static class LatchesWrapper
        extends Latches
        {
        public static NamedMap<String, LatchCounter> latchesMap()
            {
            return Latches.latchesMap();
            }
        }

    /**
     * Ensure the concurrent service is available throughout the cluster.
     *
     * @param cluster  the {@link CoherenceCluster}
     */
    public static void ensureConcurrentServiceRunning(CoherenceCluster cluster)
        {
        cluster.stream()
                .forEach(member ->
                                 Eventually.assertDeferred(
                                         () -> member.isServiceRunning("$SYS:Concurrent"), Is.is(true)));
        }
    }
