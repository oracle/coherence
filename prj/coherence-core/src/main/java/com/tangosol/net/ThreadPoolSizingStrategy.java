/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

/**
 * A ThreadPoolSizingStrategy is a pluggable strategy used by a
 * Coherence service thread pool to manage its size.
 * <p>
 * The ThreadPoolSizingStrategy is initialized during the starting of a Service.
 * The strategy will periodically analyze the threads usage of the thread pool
 * and make adjustments to the size of its thread pool.
 * Strategies may be stateful (e.g. some strategies may formulate
 * recommendations based on trends over accumulated statistics).
 *
 * @author lh 2011.12.21
 *
 * @since Coherence 12.1.2
 */
public interface ThreadPoolSizingStrategy
    {
    /**
     * Initialize the ThreadPoolSizingStrategy and bind it to the specified manager.
     *
     * @param mgr  the ThreadPoolManager this strategy will be bound to
     */
    public void init(ThreadPoolManager mgr);

    /**
     * Analyze the thread usage of the thread pool. If adjustment of the
     * thread pool size is needed, return the target pool size;
     * otherwise, -1 when no adjustment is necessary. The strategy can influence
     * the frequency with which it is analyzed by specifying the desired time
     * interval for the next adjustment. The caller of the strategy is
     * responsible setting the target size of its thread pool.
     * <p>
     * The mutation of the statistics information exposed by the Coherence
     * thread pool for the duration of this method call should have minimum
     * impact on the target thread pool size.
     *
     * @return the target thread pool size of the thread pool or -1 if no
     *         adjustment is necessary
     */
    public int adjustPoolSize();

    /**
     * Return a human-readable description of the state of the
     * ThreadPoolSizingStrategy.
     *
     * @return a human-readable description
     */
    public String getDescription();
    }
