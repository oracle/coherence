/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.net.Guardian;

/**
 * This interface provides a {@link DaemonPool} with it's external
 * dependencies.
 *
 * @author jh  2014.07.03
 */
public interface DaemonPoolDependencies
    {
    /**
     * Return the optional Guardian used to monitor the daemon threads used
     * by the DaemonPool.
     *
     * @return the optional Guardian
     */
    public Guardian getGuardian();

    /**
     * Return the optional name of the DaemonPool.
     *
     * @return the optional name of the DaemonPool
     */
    public String getName();

    /**
     * Return the initial number of daemon threads used by the DaemonPool.
     *
     * @return the initial number of daemon threads
     */
    public int getThreadCount();

    /**
     * Return the maximum number of daemon threads used by the DaemonPool.
     *
     * @return the maximum number of daemon threads
     */
    public int getThreadCountMax();

    /**
     * Return the minimum number of daemon threads used by the DaemonPool.
     *
     * @return the minimum number of daemon threads
     */
    public int getThreadCountMin();

    /**
     * Return the optional ThreadGroup within which daemon threads for the
     * DaemonPool will be created.
     *
     * @return the optional ThreadGroup for daemon threads
     */
    public ThreadGroup getThreadGroup();

    /**
     * Return the priority of daemon threads created by the DaemonPool.
     *
     * @return the daemon thread priority
     */
    public int getThreadPriority();

    /**
     * Determine if this DaemonPool dynamically changes its thread count to
     * maximize throughput and resource utilization.
     *
     * @return true if the number of daemon threads used by the DaemonPool is
     *         dynamic; false if the number of daemon threads is fixed
     */
    public boolean isDynamic();
    }
