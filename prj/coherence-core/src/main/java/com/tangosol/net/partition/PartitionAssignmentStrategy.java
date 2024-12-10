/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.tangosol.net.Member;

import java.util.Map;


/**
 * A PartitionAssignmentStrategy is a pluggable strategy used by a
 * PartitionedService to manage partition distribution.
 * <p>
 * The PartitionAssignmentStrategy is initialized when a PartitionedService
 * member becomes the distribution coordinator.  The service will periodically
 * ask the strategy to analyze the distribution, and to make distribution
 * recommendations to the {@link DistributionManager}.  Strategies may be
 * stateful (e.g. some strategies may formulate recommendations based on trends
 * over accumulated statistics).
 *
 * @author rhl 2010.10.22
 * @since  Coherence 3.7
 */
public interface PartitionAssignmentStrategy
    {
    /**
     * Initialize the PartitionAssignmentStrategy and bind it to the specified
     * DistributionManager. This method is called only on the distribution
     * coordinator, prior its first distribution {@link #analyzeDistribution analysis}.
     *
     * @param manager  the DistributionManager that this strategy is bound to
     */
    public void init(DistributionManager manager);

    /**
     * Analyze and suggest the assignment of orphaned partitions (partitions
     * without an active primary or backup owner) subject to the specified
     * constraints.  For each partition, the supplied constraints specify the
     * set of members which are eligible to be assigned the ownership.
     * <p>
     * The strategy must provide suggestions for all orphaned partitions which
     * are consistent with the supplied constraints. Failure to provide a
     * complete set of valid suggestions may result in the loss of partition data.
     *
     * @param mapConstraints  the map of assignment constraints associating
     *                        members with the set of partitions that they could
     *                        be assigned ownership of
     *
     * @since Coherence 12.1.2
     */
    public void analyzeOrphans(Map<Member, PartitionSet> mapConstraints);

    /**
     * Analyze the distribution and return the desired time interval before the
     * next distribution analysis. This method may or may not make distribution
     * suggestions through the distribution manager.  The strategy can influence
     * (but not guarantee) the frequency with which it is analyzed by returning
     * the desired interval before the next call, or -1 if the strategy has no
     * preference and will rely on the PartitionedService to decide.
     * <p>
     * As a result of failover, partitions may become 'endangered', meaning that
     * the necessary number of backups do not exist.  Failure to suggest a
     * distribution recovery plan for those partitions may result in the
     * partition remaining in the endangered state.  Additionally,
     * ownership-enabled service members that are in the process of shutting down
     * will wait until all owned partitions are transferred out.  Failure to
     * suggest a distribution plan may delay the exit of these leaving members.
     * <p>
     * The statistics and ownership information exposed by the
     * DistributionManager will not mutate for the duration of this method call.
     *
     * @return the time interval before the next desired analysis, or -1
     */
    public long analyzeDistribution();

    /**
     * Return a human-readable description of the state of the partition
     * assignment.
     *
     * @return a human-readable description
     */
    public String getDescription();
    }
