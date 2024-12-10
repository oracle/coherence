/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.net.Guardian;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

/**
 * Simple implementation of the {@link DaemonPoolDependencies} interface.
 *
 * @author jh  2014.07.03
 */
public class DefaultDaemonPoolDependencies
        implements DaemonPoolDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public DefaultDaemonPoolDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultDaemonPoolDependencies object, copying the values
     * from the specified DaemonPoolDependencies object.
     *
     * @param deps  the optional dependencies to copy
     */
    public DefaultDaemonPoolDependencies(DaemonPoolDependencies deps)
        {
        if (deps != null)
            {
            setGuardian(deps.getGuardian());
            setName(deps.getName());
            setThreadCount(deps.getThreadCount());
            setThreadCountMax(deps.getThreadCountMax());
            setThreadCountMin(deps.getThreadCountMin());
            setThreadGroup(deps.getThreadGroup());
            setThreadPriority(deps.getThreadPriority());
            }
        }

    // ----- DaemonPoolDependencies interface -------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Guardian getGuardian()
        {
        return m_guardian;
        }

    /**
     * Configure the optional Guardian used to monitor the daemon threads
     * used by the DaemonPool
     *
     * @param guardian  the optional Guardian
     */
    public void setGuardian(Guardian guardian)
        {
        m_guardian = guardian;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
        {
        return m_sName;
        }

    /**
     * Configure the optional name of the DaemonPool.
     *
     * @param sName  the optional name of the DaemonPool
     */
    public void setName(String sName)
        {
        m_sName = sName;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getThreadCount()
        {
        return m_cThreads;
        }

    /**
     * Configure the initial number of daemon threads used by this DaemonPool.
     *
     * @param cThreads  the initial number of daemon threads
     */
    public void setThreadCount(int cThreads)
        {
        m_cThreads = cThreads;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getThreadCountMax()
        {
        return m_cThreadsMax;
        }

    /**
     * Configure the maximum number of daemon threads used by this DaemonPool.
     *
     * @param cThreads  the maximum number of daemon threads
     */
    public void setThreadCountMax(int cThreads)
        {
        m_cThreadsMax = cThreads;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getThreadCountMin()
        {
        return m_cThreadsMin;
        }

    /**
     * Configure the minimum number of daemon threads used by this DaemonPool.
     *
     * @param cThreads  the minimum number of daemon threads
     */
    public void setThreadCountMin(int cThreads)
        {
        m_cThreadsMin = cThreads;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ThreadGroup getThreadGroup()
        {
        return m_group;
        }

    /**
     * Configure the optional ThreadGroup within which daemon threads for the
     * DaemonPool will be created.
     *
     * @param group  the optional ThreadGroup for daemon threads
     */
    public void setThreadGroup(ThreadGroup group)
        {
        m_group = group;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getThreadPriority()
        {
        return m_nPriority;
        }

    /**
     * Configure the priority of daemon threads created by the DaemonPool.
     *
     * @param nPriority  the priority of daemon threads
     */
    public void setThreadPriority(int nPriority)
        {
        m_nPriority = nPriority;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDynamic()
        {
        return getThreadCountMin() < getThreadCountMax();
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass())
               + "{Guardian="        + getGuardian()
               + ", Name="           + getName()
               + ", ThreadCount="    + getThreadCount()
               + ", ThreadCountMax=" + getThreadCountMax()
               + ", ThreadCountMin=" + getThreadCountMin()
               + ", ThreadGroup="    + getThreadGroup()
               + ", ThreadPriority=" + getThreadPriority()
               + ", Dynamic="        + isDynamic() + "}";
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Validate the dependencies.
     *
     * @return this object
     */
    public DefaultDaemonPoolDependencies validate()
        {
        Base.azzert(getThreadCount() >= 1,
                "ThreadCount cannot be less than 1");
        Base.azzert(getThreadCountMax() >= 1,
                "ThreadCountMax cannot be less than 1");
        Base.azzert(getThreadCountMin() >= 1,
                "ThreadCountMin cannot be less than 1");
        Base.azzert(getThreadCountMax() >= getThreadCountMin(),
                "ThreadCountMax must be greater than or equal to ThreadCountMin");
        Base.azzert(getThreadPriority() >= Thread.MIN_PRIORITY,
                "ThreadPriority cannot be less than " + Thread.MIN_PRIORITY);
        Base.azzert(getThreadPriority() <= Thread.MAX_PRIORITY,
                "ThreadPriority cannot be more than " + Thread.MAX_PRIORITY);
        return this;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The optional Guardian.
     */
    private Guardian m_guardian;

    /**
     * The optional DaemonPool name.
     */
    private String m_sName;

    /**
     * The initial number of daemon threads.
     */
    private int m_cThreads;

    /**
     * The maximum number of daemon threads.
     */
    private int m_cThreadsMax = Integer.MAX_VALUE;

    /**
     * The minimum number of daemon threads
     */
    private int m_cThreadsMin = 1;

    /**
     * The optional ThreadGroup for daemon threads.
     */
    private ThreadGroup m_group;

    /**
     * The priority of daemon threads.
     */
    private int m_nPriority = Thread.NORM_PRIORITY;
    }