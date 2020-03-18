/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

/**
 * The LeaseConfig interface provides configuration information for a lease.
 *
 * @author pfm  2011.06.12
 * @since Coherence 12.1.2
 */
public interface LeaseConfig
    {
    /**
     * Return the lease granularity. A value of LEASE_BY_THREAD means that locks are
     * held by a thread that obtained them and can only be released by that thread.
     * A value of LEASE_BY_MEMBER means that locks are held by a cluster node and
     * any thread running on the cluster node that obtained the lock can release it.
     *
     * @return the lease granularity
     */
    public int getLeaseGranularity();

    /**
     * Return the duration of the standard lease in milliseconds. Once a lease
     * has aged past this number of milliseconds, the lock will automatically be released.
     * Set this value to zero to specify a lease that never expires. The purpose
     * of this setting is to avoid deadlocks or blocks caused by stuck threads;
     * the value should be set higher than the longest expected lock duration
     * (e.g. higher than a transaction timeout). It's also recommended to set
     * this value higher then packet-delivery/timeout-milliseconds value.
     *
     * @return the standard lease milliseconds
     */
    public long getStandardLeaseMillis();

    // ----- constants ------------------------------------------------------

    /**
     * Specify the lease granularity where locks are held at a thread level
     * and can only be released by the thread that owns them.
     */
    public static final int LEASE_BY_THREAD = 0;

    /**
     * Specify the lease granularity where locks are held at the member level.
     * Any thread in the member can release a lock obtained by any other thread.
     */
    public static final int LEASE_BY_MEMBER = 1;
    }
