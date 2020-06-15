/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import java.util.Optional;

import java.util.concurrent.Executor;

import java.util.function.Function;
import java.util.function.Supplier;

import io.helidon.config.Config;

import com.tangosol.internal.util.DaemonPool;
import com.tangosol.internal.util.DaemonPoolDependencies;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.management.AnnotatedStandardMBean;
import com.tangosol.net.management.Registry;

import com.tangosol.net.management.annotation.Description;
import com.tangosol.net.management.annotation.MetricsValue;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Controllable;

import io.opentracing.Span;

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
 * If a tracing {@link Span} is available when tasks are added to this executor then
 * the span will be re-activated when the tasks is run.
 *
 * @author Jonathan Knight  2019.11.19
 * @since 20.06
 */
public class DaemonPoolExecutor
        implements Executor, Controllable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link DaemonPoolExecutor}.
     *
     * @param pool             the {@link DaemonPool} to use
     * @param registrySupplier the supplier to use to obtain a Coherence management {@link Registry}
     */
    protected DaemonPoolExecutor(DaemonPool pool, Supplier<Registry> registrySupplier)
        {
        this.f_pool             = new TracingDaemonPool(pool);
        this.f_registrySupplier = registrySupplier;
        }

    // ----- creation methods -----------------------------------------------

    /**
     * Create a {@link DaemonPoolExecutor}.
     *
     * @return a {@link DaemonPoolExecutor}
     */
    protected static DaemonPoolExecutor create()
        {
        return create(Config.empty());
        }

    /**
     * Create a {@link DaemonPoolExecutor}.
     *
     * @param config the {@link io.helidon.config.Config} to use
     *
     * @return a {@link DaemonPoolExecutor}
     */
    protected static DaemonPoolExecutor create(Config config)
        {
        return builder(config).build();
        }

    /**
     * Create a {@link DaemonPoolExecutor.Builder}.
     *
     * @return a {@link DaemonPoolExecutor.Builder}
     */
    protected static Builder builder()
        {
        return builder(Config.empty());
        }

    /**
     * Create a {@link DaemonPoolExecutor.Builder}.
     *
     * @param config the {@link io.helidon.config.Config} to use
     *
     * @return a {@link DaemonPoolExecutor.Builder}
     */
    protected static Builder builder(Config config)
        {
        return new Builder(config);
        }

    // ----- public methods -------------------------------------------------

    @Override
    public void execute(Runnable command)
        {
        f_pool.add(command);
        }

    @Override
    public void configure(XmlElement xmlElement)
        {
        f_pool.configure(xmlElement);
        }

    @Override
    public void start()
        {
        f_pool.start();
        registerMBean();
        }

    @Override
    public boolean isRunning()
        {
        return f_pool.isRunning();
        }

    /**
     * Return {@code true} if the pool is stuck.
     *
     * @return {@code true} if the pool is stuck
     */
    boolean isStuck()
        {
        return f_pool.isStuck();
        }

    @Override
    public void shutdown()
        {
        f_pool.shutdown();
        unregisterMBean();
        }

    @Override
    public void stop()
        {
        f_pool.stop();
        unregisterMBean();
        }

    @Override
    public ClassLoader getContextClassLoader()
        {
        return f_pool.getContextClassLoader();
        }

    @Override
    public void setContextClassLoader(ClassLoader loader)
        {
        f_pool.setContextClassLoader(loader);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Registers an MBean for this {@code DaemonPoolExecutor}.
     */
    protected void registerMBean()
        {
        try
            {
            Registry registry = f_registrySupplier.get();
            if (registry != null)
                {
                DaemonPoolManagement mBean = new DaemonPoolManagement(f_pool.getDelegate());
                String               globalName = registry.ensureGlobalName(
                        "type=DaemonPool,name=" + f_pool.getDependencies().getName());
                registry.register(globalName, new AnnotatedStandardMBean(mBean, DaemonPoolManagementMBean.class));
                }
            }
        catch (Throwable t)
            {
            CacheFactory.err(t);
            }
        }

    /**
     * Unregister the {@link DaemonPoolExecutor} MBean.
     */
    protected void unregisterMBean()
        {
        Registry registry = f_registrySupplier.get();
        if (registry != null)
            {
            String globalName = registry.ensureGlobalName("type=DaemonPool,name=" + f_pool.getDependencies().getName());
            registry.unregister(globalName);
            }
        }

    // ----- inner class: Builder -------------------------------------------

    /**
     * A builder of {@link DaemonPoolExecutor} instances.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class Builder
            implements io.helidon.common.Builder<DaemonPoolExecutor>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Initializes the {@code Builder} with the provided {@link Config}.
         *
         * @param config  the {@link DaemonPoolExecutor} {@link Config}
         */
        protected Builder(Config config)
            {
            this.f_config = config;
            }

        // ----- public methods ---------------------------------------------

        /**
         * Set the name of the pool.
         *
         * @param name the name of the pool
         *
         * @return this {@link Builder}
         */
        public Builder name(String name)
            {
            this.m_optName = Optional.ofNullable(name);
            return this;
            }

        /**
         * Set the supplier function that can create a {@link com.tangosol.internal.util.DaemonPool}
         * from a {@link com.tangosol.internal.util.DaemonPoolDependencies} instance.
         *
         * @param supplier the supplier function
         *
         * @return this {@link Builder}
         */
        public Builder supplier(Function<DaemonPoolDependencies, DaemonPool> supplier)
            {
            this.m_optSupplier = Optional.ofNullable(supplier);
            return this;
            }

        /**
         * Set the {@link Supplier} that will provide a Coherence management {@link Registry}.
         *
         * @param supplier the {@link Supplier} that will provide a Coherence management {@link Registry}
         *
         * @return this {@link Builder}
         */
        public Builder registry(Supplier<Registry> supplier)
            {
            this.m_registry = supplier;
            return this;
            }

        /**
         * Set the Coherence management {@link Registry}.
         *
         * @param registry the Coherence management {@link Registry}
         *
         * @return this {@link Builder}
         */
        public Builder registry(Registry registry)
            {
            return registry(() -> registry);
            }

        /**
         * Return a new {@link DaemonPoolExecutor} based on the {@link Builder builer's} configuration.
         *
         * @return a new {@link DaemonPoolExecutor} based on the {@link Builder builer's} configuration
         */
        @Override
        public DaemonPoolExecutor build()
            {
            DefaultDaemonPoolDependencies dependencies = new DefaultDaemonPoolDependencies();
            dependencies.setThreadCount(1);
            m_optName.ifPresent(dependencies::setName);
            f_config.get(CONFIG_THREAD_COUNT).asInt().ifPresent(dependencies::setThreadCount);
            f_config.get(CONFIG_THREAD_COUNT_MIN).asInt().ifPresent(dependencies::setThreadCountMin);
            f_config.get(CONFIG_THREAD_COUNT_MAX).asInt().ifPresent(dependencies::setThreadCountMax);

            DaemonPool pool = m_optSupplier.orElse(Daemons::newDaemonPool).apply(dependencies);
            return new DaemonPoolExecutor(pool, m_registry);
            }

        // ----- data members -----------------------------------------------

        /**
         * The configuration for this {@link DaemonPoolExecutor}.
         */
        private final Config f_config;

        /**
         * An {@link Optional optional} name for the {@link DaemonPoolExecutor}.
         */
        private Optional<String> m_optName = Optional.empty();

        /**
         * An {@link Optional optional} supplier  function that can create a
         * {@link com.tangosol.internal.util.DaemonPool} from a
         * {@link com.tangosol.internal.util.DaemonPoolDependencies} instance.
         */
        private Optional<Function<DaemonPoolDependencies, DaemonPool>> m_optSupplier = Optional.empty();

        /**
         * A {@link Supplier} to produce a {@link Registry} for MBean registration.
         */
        private Supplier<Registry> m_registry = () -> CacheFactory.ensureCluster().getManagement();
        }

    // ----- inner interface: DaemonPoolManagementMBean ---------------------

    /**
     * The MBean interface used to register an MBean for the daemon pool.
     * <p>
     * This interface is annotated with Coherence metrics annotations so that the daemon
     * pool MBean will produce metrics for the pool.
     */
    @SuppressWarnings("unused")
    public interface DaemonPoolManagementMBean
        {
        /**
         * Return the number of tasks that have been added to the pool, but not yet scheduled for execution.
         *
         * @return the number of tasks that have been added to the pool, but not yet scheduled for execution
         */
        @MetricsValue("backlog")
        @Description("The number of tasks that have been added to the pool, but not yet scheduled for execution")
        int getBacklog();

        /**
         * Returns the maximum number of daemon threads that the pool can create.
         *
         * @return the maximum number of daemon threads that the pool can create
         */
        @MetricsValue("max_daemon_count")
        @Description("The maximum number of Daemon threads that could exist")
        int getDaemonCountMax();

        /**
         * Set the maximum daemon pool thread count.
         *
         * @param count the maximum daemon pool thread count
         */
        void setDaemonCountMax(int count);

        /**
         * Returns the minimum number of daemon threads that the pool should have.
         *
         * @return the minimum number of daemon threads that the pool should have
         */
        @MetricsValue("min_daemon_count")
        @Description("The minimum number of Daemon threads that should exist")
        int getDaemonCountMin();

        /**
         * Set the minimum daemon pool thread count.
         *
         * @param count the minimum daemon pool thread count
         */
        void setDaemonCountMin(int count);

        /**
         * Return the number of Daemon threads that exist, if the pool has been started,
         * or the number of Daemon threads that will be created, if the pool has not yet been started.
         *
         * @return the number of Daemon threads that exist
         */
        @MetricsValue("daemon_count")
        @Description("The number of Daemon threads that exist")
        int getDaemonCount();

        /**
         * Return the total number of abandoned Daemon threads.
         * <p>
         * Note: this property is purposely not reset when stats are reset.
         *
         * @return the total number of abandoned Daemon threads
         */
        @MetricsValue("abandoned_count")
        @Description("The total number of abandoned Daemon threads")
        int getAbandonedCount();

        /**
         * Return the total number of milliseconds spent by all Daemon threads while executing
         * tasks since the last time the statistics were reset.
         * <p>
         * Note: this value could be greater then the time elapsed since each daemon adds its
         * own processing time when working in parallel.
         *
         * @return the total number of milliseconds spent by all Daemon threads while executing
         *         tasks since the last time the statistics were reset
         */
        long getActiveMillis();

        /**
         * Return the total number of currently executing hung tasks.
         * <p>
         * Note: this property is purposely not reset when stats are reset.
         *
         * @return the total number of currently executing hung tasks
         */
        @MetricsValue("hung_count")
        @Description("The total number of currently executing hung tasks")
        int getHungCount();

        /**
         * Return the longest currently executing hung task duration (in milliseconds).
         * <p>
         * Note: this property is purposely not reset when stats are reset.
         *
         * @return the longest currently executing hung task duration (in milliseconds)
         */
        @MetricsValue("hung_duration")
        @Description("The longest currently executing hung task duration (in milliseconds)")
        long getHungDuration();

        /**
         * Return the last time stats were reset.
         *
         * @return the last time stats were reset
         */
        long getLastResetMillis();

        /**
         * Return the last time the pool was resized.
         *
         * @return the last time the pool was resized
         */
        long getLastResizeMillis();

        /**
         * Return the total number of tasks added to the pool since the last time the
         * statistics were reset.
         *
         * @return the total number of tasks added to the pool since the last time
         *         the statistics were reset
         */
        @MetricsValue("task_added_count")
        @Description("The total number of tasks added to the pool since the last time the statistics were reset")
        long getTaskAddCount();

        /**
         * Return the total number of tasks executed by Daemon threads since the last
         * time the statistics were reset.
         *
         * @return the total number of tasks executed by Daemon threads since the last
         *         time the statistics were reset
         */
        @MetricsValue("task_count")
        @Description("The total number of tasks executed by Daemon threads since"
                     + " the last time the statistics were reset")
        long getTaskCount();

        /**
         * Return the maximum backlog value since the last time the statistics were reset.
         *
         * @return the maximum backlog value since the last time the statistics were reset
         */
        @MetricsValue("max_backlog_count")
        @Description("The maximum backlog value since the last time the statistics were reset")
        int getMaxBacklog();

        /**
         * Return the total number of timed-out tasks since the last time the statistics were reset.
         *
         * @return the total number of timed-out tasks since the last time the statistics were reset
         */
        @MetricsValue("timeout_count")
        @Description("The total number of timed-out tasks since the last time the statistics were reset")
        int getTimeoutCount();

        /**
         * Return the default timeout value for PriorityTasks that don't explicitly specify the
         * execution timeout value.
         *
         * @return the default timeout value for PriorityTasks that don't explicitly specify the
         *         execution timeout value
         */
        long getTaskTimeout();

        /**
         * Reset the MBean statistics.
         */
        void resetStatistics();
        }

    // ----- inner class: DaemonPoolManagement ------------------------------

    /**
     * The daemon pool MBean.
     */
    protected static class DaemonPoolManagement
            implements DaemonPoolManagementMBean
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a new {@code DaemonPoolManagement} instance.
         *
         * @param pool  the managed {@link DaemonPool}
         */
        protected DaemonPoolManagement(DaemonPool pool)
            {
            this.pool = (com.tangosol.coherence.component.util.DaemonPool) pool;
            }

        // ----- DaemonPoolManagement interface -----------------------------

        @Override
        public int getBacklog()
            {
            return pool.getBacklog();
            }

        @Override
        public int getDaemonCountMax()
            {
            return pool.getDaemonCountMax();
            }

        @Override
        public void setDaemonCountMax(int count)
            {
            pool.setDaemonCountMax(count);
            }

        @Override
        public int getDaemonCountMin()
            {
            return pool.getDaemonCountMin();
            }

        @Override
        public void setDaemonCountMin(int count)
            {
            pool.setDaemonCountMin(count);
            }

        @Override
        public int getDaemonCount()
            {
            return pool.getDaemonCount();
            }

        @Override
        public int getAbandonedCount()
            {
            return pool.getStatsAbandonedCount();
            }

        @Override
        public long getActiveMillis()
            {
            return pool.getStatsActiveMillis();
            }

        @Override
        public int getHungCount()
            {
            return pool.getStatsHungCount();
            }

        @Override
        public long getHungDuration()
            {
            return pool.getStatsHungDuration();
            }

        @Override
        public long getLastResetMillis()
            {
            return pool.getStatsLastResetMillis();
            }

        @Override
        public long getLastResizeMillis()
            {
            return pool.getStatsLastResizeMillis();
            }

        @Override
        public long getTaskAddCount()
            {
            return pool.getStatsTaskAddCount().longValue();
            }

        @Override
        public long getTaskCount()
            {
            return pool.getStatsTaskCount();
            }

        @Override
        public int getMaxBacklog()
            {
            return pool.getStatsMaxBacklog();
            }

        @Override
        public int getTimeoutCount()
            {
            return pool.getStatsTimeoutCount();
            }

        @Override
        public long getTaskTimeout()
            {
            return pool.getTaskTimeout();
            }

        @Override
        public void resetStatistics()
            {
            pool.resetStats();
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link DaemonPool} associated with this MBean.
         */
        protected final com.tangosol.coherence.component.util.DaemonPool pool;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The configuration key for thread count.
     */
    static final String CONFIG_THREAD_COUNT = "thread_count";

    /**
     * The configuration key for min thread count.
     */
    static final String CONFIG_THREAD_COUNT_MIN = "thread_count_min";

    /**
     * The configuration key for max thread count.
     */
    static final String CONFIG_THREAD_COUNT_MAX = "thread_count_max";

    // ----- data members ---------------------------------------------------

    /**
     * The {@link com.tangosol.internal.util.DaemonPool} that
     * will be used to execute tasks.
     */
    protected final TracingDaemonPool f_pool;

    /**
     * The supplier to use to obtain a Coherence management {@link Registry}.
     */
    protected final Supplier<Registry> f_registrySupplier;
    }
