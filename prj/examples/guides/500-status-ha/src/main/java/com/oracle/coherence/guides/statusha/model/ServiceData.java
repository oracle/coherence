/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.statusha.model;

/**
 * A data structure that represents StatusHA details about a service.
 *
 * @author tam 2021.08.02
 */
public class ServiceData {
    // #tag::attributes[]
    /**
     * The service name.
     */
    private final String serviceName;

    /**
     * The number of storage-enabled members for this service.
     */
    private final int storageCount;

    /**
     * The StatusHA value for the service.
     */
    private final String statusHA;

    /**
     * The total number of partitions in this service.
     */
    private final int partitionCount;

    /**
     * The number of partitions that are vulnerable, e.g. backed up on the same machine.
     */
    private final int partitionsVulnerable;

    /**
     * The number of partitions yet to be balanced.
     */
    private final int partitionsUnbalanced;

    /**
     * The number of partitions that do not have a backup.
     */
    private final int partitionsEndangered;
    // #end::attributes[]

    /**
     * Construct a {@link ServiceData} instance.
     *
     * @param serviceName          service name
     * @param storageCount         number of storage-enabled members for this service
     * @param statusHA             StatusHA value for the service
     * @param partitionCount       total number of partitions in this service
     * @param partitionsVulnerable number of partitions that are vulnerable, e.g. backed up on the same machine.
     * @param partitionsUnbalanced number of partitions yet to be balanced
     * @param partitionsEndangered number of partitions that do not have a backup
     */
    public ServiceData(String serviceName, int storageCount, String statusHA, int partitionCount,
                       int partitionsVulnerable, int partitionsUnbalanced, int partitionsEndangered) {
        this.serviceName = serviceName;
        this.storageCount = storageCount;
        this.statusHA = statusHA;
        this.partitionCount = partitionCount;
        this.partitionsVulnerable = partitionsVulnerable;
        this.partitionsUnbalanced = partitionsUnbalanced;
        this.partitionsEndangered = partitionsEndangered;
    }

    /**
     * Returns the service name.
     *
     * @return the service name
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Returns the number of storage-enabled members for this service.
     *
     * @return the number of storage-enabled members for this service
     */
    public int getStorageCount() {
        return storageCount;
    }

    /**
     * Returns the StatusHA value for the service.
     *
     * @return StatusHA value for the service
     */
    public String getHAStatus() {
        return statusHA;
    }

    /**
     * Returns the total number of partitions in this service.
     *
     * @return the total number of partitions in this service
     */
    public int getPartitionCount() {
        return partitionCount;
    }

    /**
     * Returns the number of partitions that are vulnerable, e.g. backed up on the same machine.
     *
     * @return number of partitions that are vulnerable, e.g. backed up on the same machine
     */
    public int getPartitionsVulnerable() {
        return partitionsVulnerable;
    }

    /**
     * Returns the number of partitions yet to be balanced.
     *
     * @return the number of partitions yet to be balanced
     */
    public int getPartitionsUnbalanced() {
        return partitionsUnbalanced;
    }

    /**
     * Returns the number of partitions that do not have a backup.
     *
     * @return the number of partitions that do not have a backup
     */
    public int getPartitionsEndangered() {
        return partitionsEndangered;
    }

    /**
     * Return a status value indicating how safe the service is.
     *
     * @return a status value indicating how safe the service i
     */
    public String getStatus() {
        return statusHA.equals("ENDANGERED") ? "StatusHA is ENDANGERED"
                : partitionsEndangered > 0 ? partitionsEndangered + " partitions are endangered"
                : partitionsUnbalanced > 0 ? partitionsUnbalanced + " partitions are unbalanced"
                : partitionsVulnerable > 0 ? partitionsVulnerable + " partitions are vulnerable"
                : "Safe";
    }
}
