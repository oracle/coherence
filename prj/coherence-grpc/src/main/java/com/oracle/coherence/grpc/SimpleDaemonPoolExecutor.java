/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.tangosol.internal.util.DaemonPool;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Controllable;

import java.util.concurrent.Executor;

/**
 * An {@link Executor} that uses a {@link DaemonPool} to execute tasks.
 * <p>
 * Instances of {@link SimpleDaemonPoolExecutor} are created with a {@link DaemonPool}
 * that is stopped. The executor should be started by calling the {@link #start()}
 * method.
 * <p>
 * Tasks submitted without calling start will be executed immediately on the calling
 * thread.
 *
 * @author Jonathan Knight  2020.06.24
 * @since 20.06
 */
public class SimpleDaemonPoolExecutor
        implements Executor, Controllable
    {
    // ----- constructors ---------------------------------------------------

    public SimpleDaemonPoolExecutor(String sName)
        {
        this(sName, -1, -1, -1);
        }

    public SimpleDaemonPoolExecutor(String sName, int cThreads, int cThreadsMin, int cThreadsMax)
        {
        DefaultDaemonPoolDependencies dependencies = new DefaultDaemonPoolDependencies();
        dependencies.setThreadCount(1);
        if (sName != null && !sName.isEmpty())
            {
            dependencies.setName(sName);
            }
        if (cThreads >= 0)
            {
            dependencies.setThreadCount(cThreads);
            }
        if (cThreadsMin >= 0)
            {
            dependencies.setThreadCountMin(cThreadsMin);
            }
        if (cThreadsMax >= 0)
            {
            dependencies.setThreadCountMax(cThreadsMax);
            }

        f_pool = Daemons.newDaemonPool(dependencies);
        }

    public SimpleDaemonPoolExecutor(DefaultDaemonPoolDependencies dependencies)
        {
        f_pool = Daemons.newDaemonPool(dependencies);
        }

    /**
     * Create a {@link SimpleDaemonPoolExecutor}.
     *
     * @param pool  the {@link DaemonPool} to use
     */
    public SimpleDaemonPoolExecutor(DaemonPool pool)
        {
        f_pool = pool;
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
    public boolean isStuck()
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

    /**
     * Obtain the underlying {@link DaemonPool}.
     *
     * @return the underlying {@link DaemonPool}
     */
    public DaemonPool getPool()
        {
        return f_pool;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link DaemonPool} that
     * will be used to execute tasks.
     */
    protected final DaemonPool f_pool;
    }
