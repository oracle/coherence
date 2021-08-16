/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.statusha.fetcher;

import java.util.Arrays;
import java.util.List;

import com.tangosol.net.management.Registry;

/**
 * An abstract implementation of a {@link DataFetcher}.
 *
 * @author tam 2021.08.02
 */
public abstract class AbstractDataFetcher
        implements DataFetcher {

    /**
     * List of valid Status HA values.
     */
    public static final List<String> LIST_STATUS = Arrays.asList("ENDANGERED", "NODE-SAFE", "MACHINE-SAFE", "RACK-SAFE", "SITE-SAFE");

    /**
     * Various MBean attributes to query.
     */
    protected final String ATTR_CLUSTER_NAME = "ClusterName";
    protected final String ATTR_CLUSTER_VERSION = "Version";
    protected final String ATTR_PARTITION_COUNT = "PartitionsAll";
    protected final String ATTR_STORAGE_ENABLED_COUNT = "StorageEnabledCount";
    protected final String ATTR_PARTITIONS_VULNERABLE = "PartitionsVulnerable";
    protected final String ATTR_PARTITIONS_ENDANGERED = "PartitionsEndangered";
    protected final String ATTR_PARTITIONS_UNBALANCED = "PartitionsUnbalanced";
    protected final String ATTR_STATUS_HA = "StatusHA";
    protected final String COHERENCE = "Coherence:";

    /**
     * Cluster name.
     */
    protected String clusterName;

    /**
     * Cluster version.
     */
    protected String clusterVersion;

    /**
     * Service name to monitor or null for all services.
     */
    private final String serviceName;

    /**
     * Constructor for the {@link AbstractDataFetcher}.
     *
     * @param serviceName service name
     */
    public AbstractDataFetcher(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String getClusterName() {
        return clusterName;
    }

    @Override
    public String getClusterVersion() {
        return clusterVersion;
    }

    /**
     * Sets the cluster name.
     *
     * @param clusterName the cluster name
     */
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    /**
     * Sets the cluster version.
     *
     * @param clusterVersion the cluster version
     */
    public void setClusterVersion(String clusterVersion) {
        this.clusterVersion = clusterVersion;
    }

    /**
     * Returns the service name.
     *
     * @return the service name
     */
    protected String getServiceName() {
        return serviceName;
    }
    /**
     * Returns the query for the distribution coordinator.
     *
     * @param serviceName service name
     * @return the query for the distribution coordinator
     */
    protected String getDistributionCoordinatorQuery(String serviceName) {
        return "Coherence:" + Registry.PARTITION_ASSIGNMENT_TYPE + ",responsibility=DistributionCoordinator" +
                (serviceName == null ? ",*" : ",service=" + serviceName);
    }

    /**
     * Return the StatusHA value and 'n/a' if its not yet available.
     *
     * @param statusHA Status HA
     * @return the StatusHA valu
     */
    protected String getSafeStatusHA(String statusHA) {
        return !LIST_STATUS.contains(statusHA) ? "N/A" : statusHA;
    }

    /**
     * Extract the service name from the given object name
     * @param objectName the object name to extract from
     * @return the extraced service name
     */
    protected static String extractService(String objectName) {
        return objectName.replaceAll("^.*type=PartitionAssignment", "")
                .replaceAll(",responsibility=DistributionCoordinator", "")
                .replaceAll(",service=", "");
    }
}
