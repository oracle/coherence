/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import com.oracle.coherence.grpc.SimpleDaemonPoolExecutor;
import com.tangosol.internal.util.DaemonPool;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;
import com.tangosol.util.Controllable;

import java.util.concurrent.Executor;

/**
 * An {@link Executor} that uses a {@link DaemonPool} to execute tasks.
 * <p>
 * Instances of {@link DaemonPoolExecutor} are created with a {@link DaemonPool}
 * that is stopped. The executor should be started by calling the {@link #start()}
 * method.
 * <p>
 * Tasks submitted without calling start will be executed immediately on the calling
 * thread.
 * <p>
 * If a tracing {@link com.tangosol.internal.tracing.Span} is available
 * when tasks are added to this executor then the span will be re-activated
 * when the tasks are run.
 *
 * @author Jonathan Knight  2019.11.19
 * @since 20.06
 */
public class DaemonPoolExecutor
        extends SimpleDaemonPoolExecutor
        implements Executor, Controllable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link DaemonPoolExecutor}.
     *
     * @param pool             the {@link DaemonPool} to use
     */
    public DaemonPoolExecutor(DaemonPool pool)
        {
        super(TracingDaemonPool.ensureTracingDaemonPool(pool));
        }

    // ----- factory method -------------------------------------------------

    /**
     * Create a {@link DaemonPoolExecutor}.
     *
     * @param deps  the configuration of the {@link DaemonPool}
     *
     * @return a {@link DaemonPoolExecutor}
     */
    public static DaemonPoolExecutor newInstance(DefaultDaemonPoolDependencies deps)
        {
        return new DaemonPoolExecutor(Daemons.newDaemonPool(deps));
        }

    // ----- public methods -------------------------------------------------

    /**
     * Return a {@link DaemonPoolManagement} to manage this executor.
     *
     * @return a {@link DaemonPoolManagement} to manage this executor
     */
    public DaemonPoolManagement getManagement()
        {
        return new DaemonPoolManagement(((TracingDaemonPool) f_pool).getDelegate());
        }

    // ----- inner class: DaemonPoolManagement ------------------------------

    /**
     * Daemon pool metrics and management.
     */
    public static class DaemonPoolManagement
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a new {@code DaemonPoolManagement} instance.
         *
         * @param pool  the managed {@link DaemonPool}
         */
        protected DaemonPoolManagement(DaemonPool pool)
            {
            f_pool = (com.tangosol.coherence.component.util.DaemonPool) pool;
            }

        // ----- DaemonPoolManagement interface -----------------------------

        public int getBacklog()
            {
            return f_pool.getBacklog();
            }

        public int getDaemonCountMax()
            {
            return f_pool.getDaemonCountMax();
            }

        public void setDaemonCountMax(int count)
            {
            f_pool.setDaemonCountMax(count);
            }

        public int getDaemonCountMin()
            {
            return f_pool.getDaemonCountMin();
            }

        public void setDaemonCountMin(int count)
            {
            f_pool.setDaemonCountMin(count);
            }

        public int getDaemonCount()
            {
            return f_pool.getDaemonCount();
            }

        public int getAbandonedCount()
            {
            return f_pool.getStatsAbandonedCount();
            }

        public long getActiveMillis()
            {
            return f_pool.getStatsActiveMillis();
            }

        public int getHungCount()
            {
            return f_pool.getStatsHungCount();
            }

        public long getHungDuration()
            {
            return f_pool.getStatsHungDuration();
            }

        public long getLastResetMillis()
            {
            return f_pool.getStatsLastResetMillis();
            }

        public long getLastResizeMillis()
            {
            return f_pool.getStatsLastResizeMillis();
            }

        public long getTaskAddCount()
            {
            return f_pool.getStatsTaskAddCount().longValue();
            }

        public long getTaskCount()
            {
            return f_pool.getStatsTaskCount();
            }

        public int getMaxBacklog()
            {
            return f_pool.getStatsMaxBacklog();
            }

        public int getTimeoutCount()
            {
            return f_pool.getStatsTimeoutCount();
            }

        public long getTaskTimeout()
            {
            return f_pool.getTaskTimeout();
            }

        @SuppressWarnings("unused")
        public void resetStatistics()
            {
            f_pool.resetStats();
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link DaemonPool} associated with this MBean.
         */
        protected final com.tangosol.coherence.component.util.DaemonPool f_pool;
        }
    }
