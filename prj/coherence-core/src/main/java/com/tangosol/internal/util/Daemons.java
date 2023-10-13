/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.OperationalContext;
import com.tangosol.util.Base;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * Factory and utility methods for the {@link DaemonPool} classes and
 * interfaces defined in this package.
 *
 * @author jh  2014.06.25
 */
public abstract class Daemons
    {
    // ----- factory methods ------------------------------------------------

    /**
     * Create a new DaemonPool.
     *
     * @param deps  the configuration for the new DaemonPool
     *
     * @return a new, unstarted, fixed-size DaemonPool
     */
    public static DaemonPool newDaemonPool(DaemonPoolDependencies deps)
        {
        try
            {
            DaemonPool pool = (DaemonPool) DAEMON_POOL_CLASS.newInstance();
            pool.setDependencies(deps);

            return pool;
            }
        catch (Throwable e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Returns the singleton common {@link DaemonPool}.
     *
     * @return the singleton common {@link DaemonPool}
     */
    public static DaemonPool commonPool()
        {
        return ((OperationalContext) CacheFactory.getCluster()).getCommonDaemonPool();
        }

    /**
     * Returns a shared, Coherence-managed {@link ForkJoinPool} instance.
     *
     * @return a shared, Coherence-managed {@link ForkJoinPool} instance
     */
    public static ForkJoinPool forkJoinPool()
        {
        if (s_forkJoinPool == null)
            {
            s_forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), ForkJoinPoolWorker::new, null, false);
            }

        return s_forkJoinPool;
        }

    // ----- inner class: ForkJoinPoolWorker --------------------------------

    /**
     * Custom {@link ForkJoinWorkerThread} implementation.
     */
    static class ForkJoinPoolWorker extends ForkJoinWorkerThread
        {
        /**
         * Construct a ForkJoinPoolWorker operating in the given pool.
         *
         * @param pool  the pool this thread works in
         *
         * @throws NullPointerException if pool is null
         */
        ForkJoinPoolWorker(ForkJoinPool pool)
            {
            super(pool);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The class name of the DaemonPool component.
     */
    private static final String DAEMON_POOL =
            "com.tangosol.coherence.component.util.DaemonPool";

    /**
     * The DaemonPool class.
     */
    private static final Class DAEMON_POOL_CLASS;

    private static ForkJoinPool s_forkJoinPool;

    static
        {
        try
            {
            DAEMON_POOL_CLASS = Class.forName(DAEMON_POOL);
            }
        catch (Throwable e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    }
