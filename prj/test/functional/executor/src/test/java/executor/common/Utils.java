/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package executor.common;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.ClusteredAssignment;
import com.oracle.coherence.concurrent.executor.ClusteredExecutorInfo;
import com.oracle.coherence.concurrent.executor.ClusteredProperties;
import com.oracle.coherence.concurrent.executor.ClusteredTaskManager;

import com.tangosol.net.NamedCache;

import java.util.Objects;

/**
 * Testing utilities.
 *
 * @author rlubke 2022.4.1
 * @since 14.1.1.2.0
 */
public final class Utils
    {
    /**
     * Dump cache states for the specified caches.
     *
     * @param executorInfoCache  the {@link ClusteredExecutorInfo} cache
     * @param assignmentCache    the {@link ClusteredAssignment} cache
     * @param taskManagerCache   the {@link ClusteredTaskManager} cache
     * @param propertiesCache    the {@link ClusteredProperties} cache
     *
     * @throws NullPointerException if any of the arguments are {@code null}
     */
    public static void dumpExecutorCacheStates(NamedCache<?, ?> executorInfoCache,
                                               NamedCache<?, ?> assignmentCache,
                                               NamedCache<?, ?> taskManagerCache,
                                               NamedCache<?, ?> propertiesCache)
        {
        Objects.requireNonNull(executorInfoCache);
        Objects.requireNonNull(assignmentCache);
        Objects.requireNonNull(taskManagerCache);
        Objects.requireNonNull(propertiesCache);

        final StringBuilder builder = new StringBuilder(1024);

        builder.append("\n\n### Dumping Cache States ...\n");
        builder.append("=== Executors [count=").append(executorInfoCache.size()).append("] ===\n");
        executorInfoCache.entrySet().forEach(o -> builder.append('\t').append(o).append('\n'));

        builder.append("\n=== Assignments [count=").append(assignmentCache.size()).append("] ===\n");
        assignmentCache.entrySet().forEach(o -> builder.append('\t').append(o).append('\n'));

        builder.append("\n=== Tasks [count=").append(taskManagerCache.size()).append("] ===\n");
        taskManagerCache.entrySet().forEach(o -> builder.append('\t').append(o).append('\n'));

        builder.append("\n=== Properties [count=").append(propertiesCache.size()).append("] ===\n");
        propertiesCache.entrySet().forEach(o -> builder.append('\t').append(o).append('\n'));
        builder.append("\n\n");

        System.out.print(builder);
        }

    /**
     * Runs the provided assertion {@link Runnable} upon which, if a failure occurs, the {@code onFailure}
     * {@link Runnable} will be invoked.  In all cases, the error raised by the assertion logic will
     * be re-thrown.
     *
     * @param assertion  the assertion logic to run
     * @param onFailure  the failure logic to run if the assertion logic raises an error
     *
     * @throws NullPointerException if any of the arguments are {@code null}
     */
    public static void assertWithFailureAction(Runnable assertion, Runnable onFailure)
        {
        Objects.requireNonNull(assertion);
        Objects.requireNonNull(onFailure);

        try
            {
            assertion.run();
            }
        catch (Throwable t)
            {
            try
                {
                onFailure.run();
                }
            catch (Throwable tInner)
                {
                Logger.err("Unexpected error invoking onFailure action", tInner);
                }
            throw t;
            }
        }
    }
