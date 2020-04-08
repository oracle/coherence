/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.util.Base;

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
