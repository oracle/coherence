/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
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

import com.oracle.coherence.concurrent.locks.Locks;

import com.tangosol.net.NamedMap;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.Map;

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
     * Clear all local and remote latches.
     */
    public static void clearLatches()
        {
        try
            {
            clearMap(Latches.class, "f_mapLatchLocal");
            clearMap(Latches.class, "f_mapLatch");
            latchesMap().clear();
            }
        catch (Throwable e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Clear all local and remote locks.
     */
    public static void clearLocks()
        {
        try
            {
            clearMap(Locks.class, "f_mapExclusiveLocal");
            clearMap(Locks.class, "f_mapReadWriteLocal");
            clearMap(Locks.class, "f_mapExclusive");
            clearMap(Locks.class, "f_mapReadWrite");
            Locks.exclusiveLocksMap().clear();
            Locks.readWriteLocksMap().clear();
            }
        catch (Throwable e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Clear the named static map field.
     *
     * @param clz         the type declaring the field
     * @param sFieldName  the field name
     *
     * @throws Exception if the map cannot be cleared
     */
    private static void clearMap(Class<?> clz, String sFieldName)
            throws Exception
        {
        Field field = clz.getDeclaredField(sFieldName);
        field.setAccessible(true);
        ((Map<?, ?>) field.get(null)).clear();
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
