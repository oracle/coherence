/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.tangosol.internal.util.DaemonPool;
import com.tangosol.internal.util.DaemonPoolDependencies;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Controllable;

import io.helidon.config.Config;

import io.opentracing.Span;

import java.util.Optional;

import java.util.concurrent.Executor;

import java.util.function.Function;

/**
 * An {@link Executor} that uses a {@link DaemonPool} to execute tasks.
 * <p>
 * Instances of {@link DaemonPoolExecutor} are created with a {@link DaemonPool}
 * that is stopped. The executor should be started by calling the {@link #start()}
 * method.
 * <p>
 * Tasks submitted without calling start will be executed immediately on the calling
 * thread.
 *
 * @author Jonathan Knight  2020.06.24
 * @since 20.06
 */
public class DaemonPoolExecutor
        implements Executor, Controllable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link DaemonPoolExecutor}.
     *
     * @param pool  the {@link DaemonPool} to use
     */
    protected DaemonPoolExecutor(DaemonPool pool)
        {
        f_pool = pool;
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
     * @param config the {@link Config} to use
     *
     * @return a {@link DaemonPoolExecutor}
     */
    protected static DaemonPoolExecutor create(Config config)
        {
        return builder(config).build();
        }

    /**
     * Create a {@link Builder}.
     *
     * @return a {@link Builder}
     */
    protected static Builder builder()
        {
        return builder(Config.empty());
        }

    /**
     * Create a {@link Builder}.
     *
     * @param config the {@link Config} to use
     *
     * @return a {@link Builder}
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
        }

    @Override
    public void stop()
        {
        f_pool.stop();
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
            f_config = config;
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
         * Set the supplier function that can create a {@link DaemonPool}
         * from a {@link DaemonPoolDependencies} instance.
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
            return new DaemonPoolExecutor(pool);
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
         * {@link DaemonPool} from a {@link DaemonPoolDependencies} instance.
         */
        private Optional<Function<DaemonPoolDependencies, DaemonPool>> m_optSupplier = Optional.empty();
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
     * The {@link DaemonPool} that
     * will be used to execute tasks.
     */
    protected final DaemonPool f_pool;
    }
