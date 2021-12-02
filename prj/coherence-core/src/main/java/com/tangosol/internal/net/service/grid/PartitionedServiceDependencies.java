/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.PartitionAssignmentStrategyBuilder;

import com.tangosol.net.partition.KeyAssociator;
import com.tangosol.net.partition.KeyPartitioningStrategy;
import com.tangosol.net.partition.PartitionListener;

import java.util.List;

/**
 * The PartitionedServiceDependencies interface provides a PartitionedService with its
 * external dependencies.
 *
 * @author pfm  2011.05.12
 * @since Coherence 12.1.2
 */
public interface PartitionedServiceDependencies
        extends GridDependencies
    {
    /**
     * Return the aggressiveness factor.
     *
     * NOTE: UNDOCUMENTED
     *
     * @return the aggressiveness factor
     */
    public int getDistributionAggressiveness();

   /**
     * Return true if distribution is synchronized.
     *
     * NOTE: UNDOCUMENTED
     *
     * @return true if distribution is synchronized
     */
    public boolean isDistributionSynchronized();

    /**
     * Return the KeyAssociator which provides key associations on behalf of a set of keys.
     * The information provided by a KeyAssociator will be used to place all associated keys
     * into the same partition.
     *
     * @return the KeyAssociator
     */
    public KeyAssociator getKeyAssociator();

    /**
     * Return the KeyPartitioningStrategy which is a pluggable strategy for assigning keys
     * to specific partitions.
     *
     * @return the KeyPartitioningStrategy
     */
    public KeyPartitioningStrategy getKeyPartitioningStrategy();

    /**
    * Return true if this member is capable of local ownership of partitions.
    * A storage enabled node may start with ownership enabled but in certain
    * cases, ownership will be disable.
    *
    * @return true if ownership is enabled
    */
    public boolean isOwnershipCapable();

    /**
     * Return true if this member is configured to do async backup.
     *
     * @return true if async backup is enabled
     */
    public boolean isAsyncBackupEnabled();

    /**
     * Return zero if this member is configured to do async backup, or a duration
     * between scheduled backups. If null, async or scheduled backups are disabled.
     *
     * @return Duration of scheduled backups, zero if async backups are to be enabled
     */
    public Duration getAsyncBackupInterval();

    /**
     * Return the PartitionAssignmentStrategyBuilder is a pluggable strategy used by a
     * PartitionedService to manage partition distribution.
     *
     * @return the PartitionAssignmentStrategyBuilder
     */
    public PartitionAssignmentStrategyBuilder getPartitionAssignmentStrategyBuilder();

    /**
     * Return the PartitionListeners which listen for PartitionEvents.
     *
     * @return the PartitionListeners
     */
    public List<ParameterizedBuilder<PartitionListener>> getPartitionListenerBuilders();

    /**
     * Return the number of members of the partitioned cache service that hold the backup
     * data for each unit of storage in the cache.
     *
     * @return the backup count
     */
    public int getPreferredBackupCount();

    /**
     * Return the preferred number of partitions that a partitioned (distributed) cache will
     * be "chopped up" into.  Each member running the partitioned cache service that has the
     * local-storage option set to true will manage a "fair" (balanced) number of partitions.
     * The first member of the cluster determines the partition count used at runtime.
     *
     * @return the partition count
     */
    public int getPreferredPartitionCount();

    /**
     * Return the transfer threshold for the primary buckets distribution in bytes. When a
     * new node joins the partitioned cache service or when a member of the service leaves,
     * the remaining nodes perform a task of bucket ownership re-distribution. During this
     * process, the existing data gets re-balanced along with the ownership information. This
     * parameter indicates a preferred message size for data transfer communications. Setting
     * this value lower will make the distribution process take longer, but will reduce network
     * bandwidth utilization during this activity.
     *
     * @return the transfer threshold
     */
    public int getTransferThreshold();


    /**
     * Return the PersistenceDependencies encapsulating the persistence-related
     * configuration of the partitioned service only if {@see #isOwnershipCapable()}
     * returns true.
     *
     * @return the PersistenceDependencies used by the partitioned service
     */
    public PersistenceDependencies getPersistenceDependencies();
    }
