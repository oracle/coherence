/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.net.Guardian;
import com.tangosol.util.Controllable;

import java.util.concurrent.Executor;

/**
 * A DaemonPool processes queued operations on one or more daemon threads.
 *
 * @author jh  2014.06.25
 */
public interface DaemonPool
        extends Controllable, Executor
    {

    // ----- DaemonPool interface -------------------------------------------

    /**
     * Adds a Runnable task to the DaemonPool.
     *
     * @param task  the Runnable task to execute (call the <tt>run()</tt>
     *              method of) on one of the daemon threads
     */
    public void add(Runnable task);

    /**
     * Schedules the specified Runnable task for execution by the DaemonPool
     * after the specified delay.
     *
     * @param task     task to be scheduled
     * @param cMillis  delay in milliseconds before task is to be executed by
     *                 this DaemonPool
     */
    public void schedule(Runnable task, long cMillis);

    // ----- accessors ------------------------------------------------------

    /**
     * Return the external dependencies of this DaemonPool.
     *
     * @return the external dependencies
     */
    public DaemonPoolDependencies getDependencies();

    /**
     * Configure the external dependencies of this DaemonPool.
     *
     * @param deps  the external dependencies
     *
     * @throws IllegalStateException if the DaemonPool is running
     */
    public void setDependencies(DaemonPoolDependencies deps);

    /**
     * Return a {@link Guardian} that can be registered with.
     *
     * @return a Guardian that can be registered with
     */
    public default Guardian getGuardian()
        {
        DaemonPoolDependencies deps = getDependencies();
        return deps == null ? null : deps.getGuardian();
        }

    /**
     * Determine if this DaemonPool has not made progress since the last time
     * this method was called.
     *
     * @return true iff this DaemonPool has not made progress since the
     *         last time this method was called
     */
    public boolean isStuck();

    @Override
    default void execute(Runnable task)
        {
        this.add(task);
        }

    /**
     * The name used to register the common pool builder in the cluster registry.
     */
    String COMMON_POOL_BUILDER_NAME = "common-daemon-pool";
    }
