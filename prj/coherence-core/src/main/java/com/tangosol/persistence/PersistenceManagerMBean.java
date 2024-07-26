/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.tangosol.net.management.Registry;

import com.tangosol.net.management.annotation.Description;
import com.tangosol.net.management.annotation.Notification;

/**
 * Standard MBean interface that exposes management attributes and operations
 * relating to a PartitionedService configured with on-demand or active persistence.
 * <p>
 * Each persistence-enabled PartitionedService registers a single instance of
 * this MBean bound to a JMX name of the form:
 * <tt>"Coherence:type=Persistence,service={ServiceName},responsibility=PersistenceCoordinator"</tt>
 *
 * @author rhl 2013.05.07
 * @since Coherence 12.2.1
 */
@Notification(description = "PersistenceManager notifications.",
    types = {
            PersistenceManagerMBean.CREATE_SNAPSHOT_BEGIN,
            PersistenceManagerMBean.CREATE_SNAPSHOT_END,
            PersistenceManagerMBean.RECOVER_SNAPSHOT_BEGIN,
            PersistenceManagerMBean.RECOVER_SNAPSHOT_END,
            PersistenceManagerMBean.REMOVE_SNAPSHOT_BEGIN,
            PersistenceManagerMBean.REMOVE_SNAPSHOT_END,
            PersistenceManagerMBean.ARCHIVE_SNAPSHOT_BEGIN,
            PersistenceManagerMBean.ARCHIVE_SNAPSHOT_END,
            PersistenceManagerMBean.RETRIEVE_ARCHIVED_SNAPSHOT_BEGIN,
            PersistenceManagerMBean.RETRIEVE_ARCHIVED_SNAPSHOT_END,
            PersistenceManagerMBean.REMOVE_ARCHIVED_SNAPSHOT_BEGIN,
            PersistenceManagerMBean.REMOVE_ARCHIVED_SNAPSHOT_END,
            PersistenceManagerMBean.RECOVER_BEGIN,
            PersistenceManagerMBean.RECOVER_END,
            PersistenceManagerMBean.RECOVER_DISALLOWED,
            })
public interface PersistenceManagerMBean
    {
    // ----- attributes -----------------------------------------------------

    /**
     * Return the node id of the persistence coordinator.
     *
     * @return current persistence coordinator node id
     */
    @Description("The member id of the service node that is the persistence coordinator.")
    public int getCoordinatorId();

    /**
     * Return the status of the current operation issued by the persistence coordinator.
     *
     * @return the status of the current operation issued by the persistence coordinator
     */
    @Description("The status of the current operation issued by the persistence coordinator.")
    public String getOperationStatus();

    /**
     * Specifies whether or not the persistence coordinator is idle.
     *
     * @return true if the persistence coordinator is idle
     */
    @Description("Specifies whether or not the persistence coordinator is idle.")
    public boolean isIdle();

    /**
     * Return a list of snapshots that are available for recovery.
     *
     * @return a list of snapshots that are available for recovery
     */
    @Description("The list of snapshot identifiers that are available to recover from.")
    public String[] getSnapshots();

    // ----- snapshot operations --------------------------------------------

    /**
     * Create a snapshot of the service with the specified name.
     *
     * @param sName  the snapshot name to create
     */
    @Description("Asynchronously create a snapshot of the service with the specified name. " +
                 "Subscribe to JMX notifications to see the status of the operation.")
    public void createSnapshot(@Description("sName") String sName);

    /**
     * Recover the snapshot that was previously created with the specified name.
     * <p>
     * Note: the entire service is recovered from persisted state therefore
     *       transient caches are reset.
     *
     * @param sName  the snapshot name to recover
     */
    @Description("Asynchronously recover the entire service from a snapshot with the specified name. " +
                 "Subscribe to JMX notifications to see the status of the operation.")
    public void recoverSnapshot(@Description("sName") String sName);

    /**
     * Remove the snapshot that was previously created with the specified name.
     *
     * @param sName  the snapshot name to remove
     */
    @Description("Asynchronously remove the snapshot of the service with the specified name. " +
                 "Subscribe to JMX notifications to see the status of the operation.")
    public void removeSnapshot(@Description("sName") String sName);

    // ----- archive operations ---------------------------------------------

    /**
     * Archive the snapshot to a centralized location.
     *
     * @param sName  the snapshot name to archive
     */
    @Description("Asynchronously archive the snapshot to a centralized location. " +
                 "Subscribe to JMX notifications to see the status of the operation.")
    public void archiveSnapshot(@Description("sName") String sName);

    /**
     * Retrieve the archived snapshot from a centralized location.
     *
     * @param sName  the snapshot name to archive
     */
    @Description("Asynchronously retrieve the archived snapshot from a centralized location. " +
                 "Subscribe to JMX notifications to see the status of the operation.")
    public void retrieveArchivedSnapshot(@Description("sName") String sName);

    /**
     * Purge the archived snapshot from a centralized location.
     *
     * @param sName  the snapshot name to purge
     */
    @Description("Remove the archived snapshot from a centralized location.")
    public void removeArchivedSnapshot(@Description("sName") String sName);

    /**
     * Proceed with recovery despite the dynamic quorum policy objections.
     */
    @Description("Proceed with recovery despite the dynamic quorum policy objections. " +
                 "This may lead to the partial or full data loss of the corresponding cache service.")
    public void forceRecovery();

    /**
     * Return a list of archived snapshots that the configured archiver knows about.
     * If none exist, an empty {@link String}[] will be returned.
     *
     * @return a {@link String}[] of known archived snapshots
     */
    @Description("Return a list of archived snapshots for the service.")
    public String[] listArchivedSnapshots();

    /**
     * Return a list of stores for a given archived snapshot.
     *
     * @param sName  the snapshot name to list stores for
     *
     * @return a {@link String}[] of known stores for the archived snapshot
     */
    @Description("Return a list of stores for the archived snapshot with " +
                 "the specified name.")
    public String[] listArchivedSnapshotStores(@Description("sName") String sName);


    // ----- constants ------------------------------------------------------

    /**
     * String representation of the responsibility of PersistenceCoordinator.
     */
    public static final String PERSISTENCE_COORDINATOR = "PersistenceCoordinator";

    /**
     * Notification type for the start of snapshot creation.
     */
    public static final String CREATE_SNAPSHOT_BEGIN  = "create.snapshot.begin";

    /**
     * Notification type for the end of snapshot creation.
     */
    public static final String CREATE_SNAPSHOT_END    = "create.snapshot.end";

    /**
     * Notification type for the start of recovering from a snapshot.
     */
    public static final String RECOVER_SNAPSHOT_BEGIN = "recover.snapshot.begin";

    /**
     * Notification type for the end of recovering from a snapshot.
     */
    public static final String RECOVER_SNAPSHOT_END   = "recover.snapshot.end";

    /**
     * Notification type for the start of removing a snapshot.
     */
    public static final String REMOVE_SNAPSHOT_BEGIN  = "remove.snapshot.begin";

    /**
     * Notification type for the end of removing a snapshot.
     */
    public static final String REMOVE_SNAPSHOT_END    = "remove.snapshot.end";

    /**
     * Notification type for the start of archiving a snapshot.
     */
    public static final String ARCHIVE_SNAPSHOT_BEGIN = "archive.snapshot.begin";

    /**
     * Notification type for the end of archiving a snapshot.
     */
    public static final String ARCHIVE_SNAPSHOT_END = "archive.snapshot.end";

    /**
     * Notification type for the start of retrieving an archived snapshot.
     */
    public static final String RETRIEVE_ARCHIVED_SNAPSHOT_BEGIN = "retrieve.archived.snapshot.begin";

    /**
     * Notification type for the end of retrieving an archiving snapshot.
     */
    public static final String RETRIEVE_ARCHIVED_SNAPSHOT_END = "retrieve.archived.snapshot.end";

    /**
     * Notification type for the start of purging (removing) an archived snapshot.
     */
    public static final String REMOVE_ARCHIVED_SNAPSHOT_BEGIN = "remove.archived.snapshot.begin";

    /**
     * Notification type for the end of purging (removing) an archived snapshot.
     */
    public static final String REMOVE_ARCHIVED_SNAPSHOT_END = "remove.archived.snapshot.end";

    /**
     * Notification type for the start of recovery.
     */
    public static final String RECOVER_BEGIN = "recover.begin";

    /**
     * Notification type for the end of recovery.
     */
    public static final String RECOVER_END = "recover.end";

    /**
     * Notification type for the recovery being disallowed by the quorum.
     */
    public static final String RECOVER_DISALLOWED = "recover.disallowed";
    }