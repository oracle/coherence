/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.tangosol.net.Member;
import com.tangosol.net.PartitionedService;

import java.util.Map;
import java.util.Set;


/**
 * A DistributionManager coordinates the distribution of partitions for a
 * partitioned service.  The DistributionManager relies on a pluggable {@link
 * PartitionAssignmentStrategy} to analyze the current state of the partition
 * usage, and formulate a desired distribution plan.  The DistributionManager
 * is responsible for gathering and providing to the strategy the state
 * necessary to make distribution decisions, as well as collecting and enacting
 * the formulated distribution recommendations.
 * <p>
 * The DistributionManager provides a consistent and stable view of
 * Service-related state for the duration of the call to {@link
 * PartitionAssignmentStrategy#analyzeDistribution}.
 *
 * @author rhl 2010.10.22
 * @since  Coherence 3.7
 */
public interface DistributionManager
    {
    /**
     * Return the partitioned service for which this DistributionManager
     * coordinates the distribution.
     *
     * @return the partitioned service
     */
    public PartitionedService getService();

    /**
     * Return the ownership-enabled service member with the specified
     * mini-id, or null if the member does not exist.
     *
     * @param nMemberId  the mini-id
     *
     * @return the ownership-enabled service member with the specified
     *         mini-id, or null
     */
    public Member getMember(int nMemberId);

    /**
     * Return the set of ownership-enabled members in the partitioned service.
     *
     * @return the set of ownership-enabled members
     */
    public Set<Member> getOwnershipMembers();

    /**
     * Return the set of ownership-enabled members in the partitioned
     * service that are in the process of leaving.
     *
     * @return the set of ownership-enabled members that are leaving
     */
    public Set<Member> getOwnershipLeavingMembers();

    /**
     * Return the set of partitions for which the specified member owns the
     * specified storage index (replica).
     *
     * @param member  the member to determine the ownership for
     * @param iStore  the storage index (zero for primary)
     *
     * @return the set of partitions
     *
     * @throws IllegalArgumentException if the backup number is non-positive or
     *         greater than the backup count for this partitioned service
     */
    public PartitionSet getOwnedPartitions(Member member, int iStore);

    /**
     * Return the Ownership information for the specified partition.
     *
     * @param nPartition  the partition to return the ownership for
     *
     * @return Ownership information for the specified partition
     *
     * @throws IllegalArgumentException if the partition number is negative or
     *         greater than the partition count for this partitioned service
     */
    public Ownership getPartitionOwnership(int nPartition);

    /**
     * Suggest a distribution change for the specified set of partitions,
     * indicating either primary or backup transfer (or both).  The desired
     * transfer is specified as the ownership that would result from enacting the
     * transfer.
     * <p>
     * Note: this method does not in any way enforce or guarantee that the
     *       specified transfers are performed; it merely provides advice
     *
     * @param parts      the set of partitions to perform distribution on
     * @param ownership  the desired ownership
     */
    public void suggest(PartitionSet parts, Ownership ownership);

    /**
     * Return a set of partitions from the previous suggestions that were
     * ignored or rejected.
     *
     * @return partitions that were suggested for an ownership change, but
     *         those changes were rejected by the service
     *
     * @since Coherence 12.2.1
     */
    public PartitionSet getIgnoredAdvice();

    /**
     * Schedule the next distribution analysis in the specified time interval.
     * <p>
     * This method may influence, but does not guarantee the time of the next
     * distribution analysis. This method may be called on any thread but has
     * an effect only if this member is currently the distribution coordinator.
     *
     * @param cMillis  the delay (in ms) before the next analysis (must be &gt; 0)
     *
     * @since Coherence 12.1.2
     */
    public void scheduleNextAnalysis(long cMillis);

    /**
     * Return the sampling period at which partition statistics are collected.
     *
     * @return the sampling period in milliseconds
     */
    public long getSamplingPeriod();

    /**
     * Return an array of the most recently available partition statistics,
     * indexed by partition-id.
     *
     * @return an array of the most recently available partition statistics
     */
    public PartitionStatistics[] getPartitionStats();
    }
