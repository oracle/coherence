/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.persistence;

/**
 * PersistenceTools provides an ability to submit operations under the context
 * of a {@link PersistenceManager}, hence a reference to PersistenceTools is
 * retrieved from {@link PersistenceManager#getPersistenceTools()}. The PersistenceManager
 * typically will refer to offline stores such as a snapshot, archived snapshot,
 * or an unlinked active store (disconnected from running cache server).
 * <p>
 * The intent of this interface is to describe some offline operations that can
 * be performed against {@link PersistentStore}s to validate their integrity
 * and accumulate offline information. One of the primary benefits is to verify
 * recovery will be successful. There are other operations that can be beneficial,
 * including retrieving statistics, correction and compaction.
 *
 * @since 12.2.1
 * @author hr/tam  2014.10.11
 */
public interface PersistenceTools
    {
    /**
     * Return summary information regarding the available {@link PersistentStore}s
     * under the context of the {@link PersistenceManager} (snapshot or archived
     * snapshot).
     *
     * @return summary information about the specific snapshot
     */
    public OfflinePersistenceInfo getPersistenceInfo();

    /**
     * Validate the available {@link PersistentStore}s under the context of the
     * associated {@link PersistenceManager} (snapshot or archived snapshot).
     *
     * @throws RuntimeException if the snapshot is invalid
     */
    public void validate();

    /**
     * Return a {@link PersistenceStatistics} object representing the available
     * {@link PersistentStore}s under the context of the associated {@link
     * PersistenceManager} (snapshot or archived snapshot).
     *
     * @return a PersistenceStatistics object representing a snapshot
     */
    public PersistenceStatistics getStatistics();
    }