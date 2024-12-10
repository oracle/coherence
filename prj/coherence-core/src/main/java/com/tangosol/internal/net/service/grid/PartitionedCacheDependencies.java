/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.tangosol.io.DeltaCompressor;

/**
 * The PartitionedCacheDependencies interface provides a PartitionedCache with
 * its external dependencies.
 *
 * @author pfm  2011.05.12
 * @since Coherence 12.1.2
 */
public interface PartitionedCacheDependencies
        extends PartitionedServiceDependencies, LeaseConfig
    {
    /**
     * Return  the number of members of the partitioned cache service that will hold the
     * backup data for each unit of storage in the cache that does not require write-behind,
     * that is, data that is not vulnerable to being lost even if the entire cluster were
     * shut down.
     *
     * @return the backup count after write-behind
     */
    public int getBackupCountAfterWriteBehind();

    /**
     * Return the DeltaCompressor.
     *
     * NOTE: UNDOCUMENTED
     *
     * @return the DeltaCompressor
     */
    public DeltaCompressor getDeltaCompressor();

    /**
     * Return true if strict partitioning is used.
     *
     * NOTE: UNDOCUMENTED
     *
     * @return true if strict partitioning is used
     */
    public boolean isStrictPartitioning();
    }
