/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.partition;

import com.tangosol.net.PartitionedService;

import com.tangosol.net.management.annotation.Description;
import com.tangosol.net.management.annotation.MetricsScope;
import com.tangosol.net.management.annotation.MetricsTag;
import com.tangosol.net.management.annotation.MetricsValue;
import com.tangosol.net.management.annotation.Notification;
import com.tangosol.net.metrics.MBeanMetric;

import java.util.Date;

/**
 * Standard MBean interface that exposes management attributes and operations for
 * a {@link SimpleAssignmentStrategy} used by a {@link PartitionedService}.
 * <p>
 * Each {@link PartitionedService} registers a single instance of this MBean
 * bound to a JMX name of the form:
 * <tt>"Coherence:type=PartitionAssignment,service={ServiceName},responsibility=DistributionCoordinator"</tt>
 * <p>
 * The MBean is attached to a single instance of {@link PartitionAssignmentStrategy} object,
 * which exists on the member that is the distribution coordinator for the service.
 * The associated MBean will not be explicitly unregistered, but its name will be
 * rebound to a new MBean instance if and when a different service member
 * becomes the distribution coordinator.
 *
 * @author op  2012.08.23
 * @since Coherence 12.1.2
 */
@MetricsScope(MBeanMetric.Scope.VENDOR)
@Description("Describes the SimpleAssignmentStrategy and the current state of " +
             "partition distribution for the corresponding PartitionedService.")
@Notification(description = "DistributionCoordinator notifications.",
              types = SimpleStrategyMBean.NOTIFY_LOST)
public interface SimpleStrategyMBean
    {
    // ----- attributes -----------------------------------------------------

    /**
     * Get name of the PartitionAssignmentStrategy in use.
     *
     * @return partitioning strategy name
     */
    @Description("The name of the partition assignment strategy in use.")
    public String getStrategyName();

    /**
     * Get the last time a partition distribution analysis was performed.
     *
     * @return the time of the last analysis
     */
    @Description("The last time a distribution analysis was performed.")
    public Date getLastAnalysisTime();

    /**
     * Get the node id of the ownership distribution coordinator.
     *
     * @return current distribution coordinator node id
     */
    @Description("The member id of the service node that is the ownership " +
                 "distribution coordinator.")
    @MetricsTag("coordinatorId")
    public int getCoordinatorId();

    /**
     * Get the configured number of partitions for the service.
     *
     * @return the partition count
     */
    @Description("The configured number of partitions for the service.")
    public int getPartitionCount();

    /**
     * Get the configured number of backups.
     *
     * @return the backup count
     */
    @Description("The configured number of partition backups to be maintained by the service.")
    public int getBackupCount();

    /**
     * Get the number of storage-enabled nodes running this service.
     *
     * @return the number of storage-enabled nodes
     */
    @Description("The number of storage-enabled nodes running this service.")
    @MetricsValue
    public int getServiceNodeCount();

    /**
     * Get the number of machines that host storage-enabled nodes running this service.
     *
     * @return the number of machines with storage-enabled nodes
     */
    @Description("The number of machines that host storage-enabled nodes running this service.")
    @MetricsValue
    public int getServiceMachineCount();

    /**
     * Get the number of racks that host storage-enabled nodes running this service.
     *
     * @return the number of racks with storage-enabled nodes
     */
    @Description("The number of racks that host storage-enabled nodes running this service.")
    @MetricsValue
    public int getServiceRackCount();

    /**
     * Get the number of sites that host storage-enabled nodes running this service.
     *
     * @return the number of sites with storage-enabled nodes
     */
    @Description("The number of sites that host storage-enabled nodes running this service.")
    @MetricsValue
    public int getServiceSiteCount();

    /**
     * The High Availability status for the service.
     *
     * @return one of the following values: ENDANGERED, NODE-SAFE, MACHINE-SAFE, RACK-SAFE, SITE-SAFE
     */
    @Description("The High Availability status for this service. The values of MACHINE-SAFE, " +
                 "RACK-SAFE and SITE-SAFE mean that all the cluster nodes running on any " +
                 "given machine, rack, or site respectively could be stopped at once without " +
                 "any data loss. The value of NODE-SAFE means that any cluster node could " +
                 "be stopped without data loss. The value of ENDANGERED indicates that " +
                 "abnormal termination of any cluster node that runs this service " +
                 "could cause data loss.")
    public String getHAStatus();

    /**
     * The High Availability status for the service as an integer.
     * <p>
     * Below is an example of how to map a HA Status integer to a string:
     * <pre>
     * HAStatus.values()[getHAStatusCode()].name()
     * </pre>
     * @return integer representation of {@link HAStatus}
     *
     * @since 12.2.1.4.0
     */
    @Description("The High Availability status for this service. The value of 3 (MACHINE_SAFE), " +
        "of 4 (RACK_SAFE) and 5 (SITE_SAFE) mean that all the cluster nodes running on any " +
        "given machine, rack, or site respectively could be stopped at once without " +
        "any data loss. The value of 2 (NODE_SAFE) means that any cluster node could " +
        "be stopped without data loss. The value of 1 (ENDANGERED) indicates that " +
        "abnormal termination of any cluster node that runs this service " +
        "could cause data loss.")
    @MetricsValue
    public int getHAStatusCode();

    /**
     * The High Availability status that this strategy attempts to achieve.
     * Values are the same as for HAStatus attribute.
     *
     * @return the target High Availability status
     */
    @Description("The High Availability status that this strategy attempts to achieve. " +
                 "Values are the same as for the HAStatus attribute.")
    public String getHATarget();

    /**
     * Get the number of backup partitions per storage-enabled service member
     * that this strategy will currently attempt to maintain.
     *
     * @return  the number of backup partitions per storage-enabled member
     */
    @Description("The number of backup partitions per storage-enabled service member " +
                 "that this strategy will currently attempt to maintain.")
    public int getFairShareBackup();

    /**
     * Get the number of primary partitions per storage-enabled service member
     * that this strategy will currently attempt to maintain.
     *
     * @return  the number of primary partitions per storage-enabled member
     */
    @Description("The number of primary partitions per storage-enabled service member " +
                 "that this strategy will currently attempt to maintain.")
    public int getFairSharePrimary();

    /**
     * Get the number of distributions (partition transfers) that remain to be
     * completed before the service achieves the goals set by this strategy.
     *
     * @return the number of remaining partition transfers
     */
    @Description("The number of partition transfers that remain to be completed " +
                 "before the service achieves the goals set by this strategy.")
    @MetricsValue
    public int getRemainingDistributionCount();

    /**
     * Get the average partition storage size.
     *
     * @return the average partition storage size in kilobytes
     */
    @Description("The average partition storage size in kilobytes.")
    @MetricsValue
    public long getAveragePartitionSizeKB();

    /**
     * Get the maximum partition storage size.
     *
     * @return the maximum partition storage size in kilobytes
     */
    @Description("The maximum partition storage size in kilobytes.")
    @MetricsValue
    public long getMaxPartitionSizeKB();

    /**
     * Get the average node storage size.
     *
     * @return the average node storage size in kilobytes
     */
    @Description("The average node storage size in kilobytes.")
    @MetricsValue
    public long getAverageStorageSizeKB();

    /**
     * Get maximum node storage size.
     *
     * @return the maximum node storage size in kilobytes
     */
    @Description("The maximum node storage size in kilobytes.")
    @MetricsValue
    public long getMaxStorageSizeKB();

    /**
     * Get the node id with the maximum storage size.
     *
     * @return the node id with the maximum storage size
     */
    @Description("The node ID with the maximum node storage size.")
    @MetricsValue
    public int getMaxLoadNodeId();

    // ----- operations -----------------------------------------------------

    /**
     * Report partitions that storage-enabled members are waiting to receive or
     * still need to send in order to achieve distribution goal set by the strategy.
     *
     * @param fVerbose  if true, the report includes partition numbers for each scheduled transfer
     *
     * @return a description of scheduled distributions for the service
     */
    @Description("Report partitions that remain to be transferred to achieve the " +
                 "goals set by this strategy. If fVerbose parameter is set to true, " +
                 "the report includes details for each scheduled transfer.")
    public String reportScheduledDistributions(@Description("fVerbose") boolean fVerbose);

    // ----- inner class: HAStatus ------------------------------------------

    /**
     * HAStatus represents the possible values to represent the High Availability Status of the
     * associated PartitionedService. This status represents the strength (and therefore safety)
     * of the weakest partition for the associated PartitionedService.
     *
     * @since 12.2.1.4.0
     */
    public enum HAStatus
        {
        /**
         * A partition is orphaned when there are no storage-enabled member of the
         * service that holds a primary copy of that partition. A PartitionedService
         * that has one or more orphaned partitions is in an orphaned HA state.
         */
        ORPHANED,

        /**
         * Loss of a cluster node that runs a PartitionedService may cause data loss.
         */
        ENDANGERED,

        /**
         * Any cluster member could be stopped without data loss.
         */
        NODE_SAFE,

        /**
         * The cluster nodes running on a machine can be stopped simultaneously without any data loss.
         */
        MACHINE_SAFE,

        /**
         * The cluster nodes running on a rack can be stopped simultaneously without any data loss.
         */
        RACK_SAFE,

        /**
         * The cluster nodes running at a site can be stopped simultaneously without any data loss.
         */
        SITE_SAFE;

        // ----- public methods ---------------------------------------------

        /**
         * Return the integer representation of the HAStatus.
         *
         * @return an integer representation of the HAStatus
         */
        public int getCode()
            {
            // optimization to use ordinal of enumerator for integer constant.
            return ordinal();
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Notification type for a "partitions are lost and needs to be recovered"
     * event.
     */
    public static final String NOTIFY_LOST = "partition.lost";
    }
