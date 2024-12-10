/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

/**
* A ThreadPoolManager manages the thread pool information for a Coherence
* service thread pool.  The thread pool relies on a pluggable {@link
* ThreadPoolSizingStrategy} to analyze the current thread usage
* and adjust its size accordingly.
* <p>
* The ThreadPoolManager provides a consistent and stable view of DaemonPool
* state for the duration of the call to {@link
* ThreadPoolSizingStrategy#adjustPoolSize}.
*
* @author lh 2011.12.21
*
* @since Coherence 12.1.2
*/
public interface ThreadPoolManager
    {
    /**
    * Return the minimum size of the thread pool.
    *
    * @return the minimum size of the thread pool
    */
    public int getMinPoolSize();

    /**
    * Return the maximum size of the thread pool.
    *
    * @return the maximum size of the thread pool
    */
    public int getMaxPoolSize();

    /**
    * Return the current size of the thread pool.
    *
    * @return the current size of the thread pool
    */
    public int getPoolSize();

    /**
    * Return the backlog of the thread pool.
    *
    * @return the backlog of the thread pool
    */
    public int getBacklog();

    /**
    * Return the idle thread count of the thread pool.
    *
    * @return the idle thread count of the thread pool
    */
    public int getIdleThreadCount();

    /**
    * Return the time interval for dynamically decreasing the size of the thread pool.
    *
    * @return the time interval for dynamically decreasing the size of the thread pool
    */
    public long getThreadDecreaseInterval();

    /**
    * Return the time interval for dynamically increasing the size of the thread pool.
    *
    * @return the time interval for dynamically increasing the size of the thread pool
    */
    public long getThreadIncreaseInterval();
    }
