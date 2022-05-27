/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.persistence.SnapshotArchiverFactory;

/**
 * PersistenceDependencies encapsulates the persistence-related configuration
 * for a PartitionedService.
 *
 * @since  Coherence 12.1.3
 * @author rhl 2013.07.25
 */
public interface PersistenceDependencies
    {
    /**
     * Return the PersistenceEnvironmentBuilder used by the partitioned service
     * to create a PersistenceEnvironment for automatically storing partition data
     * and creating persistent snapshots, or null if one hasn't been configured.
     * The partitioned service will also use the manager to recover partitioned
     * data during cluster restart.
     *
     * @return the PersistenceEnvironmentBuilder used by the partitioned service
     */
    public ParameterizedBuilder getPersistenceEnvironmentBuilder();

    /**
     * Return the SnapshotArchiverFactory used by the partitioned service
     * to create SnapshotArchivers to archive, retrieve, or purge persistent
     * snapshots, or null if one hasn't been configured.
     *
     * @return the SnapshotArchiverFactory used by the partitioned service
     */
    public SnapshotArchiverFactory getArchiverFactory();

    /**
     * Return the failure mode (one of the FAILURE_* constants).
     *
     * @return the failure mode
     */
    public int getFailureMode();

    /**
     * Return the persistence mode (one of the MODE_* constants).
     *
     * @return the persistence mode
     */
    public String getPersistenceMode();

    /**
     * Return true if persistence tasks should be written asynchronously from
     * the original mutating request.
     *
     * @return true if persistence should be written asynchronously
     */
    public default boolean isAsync()
        {
        return MODE_ACTIVE_ASYNC.equals(getPersistenceMode());
        }

    // ----- constants ------------------------------------------------------

    /**
     * Stop the service if a failure is encountered while writing to or recovering
     * from the active persistence.
     */
    public static final int FAILURE_STOP_SERVICE = 0;

    /**
     * Disable persistence but allow the service to continue running if a failure
     * is encountered while writing to or recovering from the active persistence.
     */
    public static final int FAILURE_STOP_PERSISTENCE = 1;

    /**
     * Persistence Mode: active.
     */
    public static final String MODE_ACTIVE        = "active";

    /**
     * Persistence Mode: active-backup.
     */
    public static final String MODE_ACTIVE_BACKUP = "active-backup";

    /**
     * Persistence Mode: active-async.
     */
    public static final String MODE_ACTIVE_ASYNC  = "active-async";

    /**
     * Persistence Mode: on-demand.
     */
    public static final String MODE_ON_DEMAND     = "on-demand";
    }
