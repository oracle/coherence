/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.oracle.coherence.persistence.PersistenceEnvironment;
import com.oracle.coherence.persistence.PersistenceException;

import com.oracle.datagrid.persistence.PersistenceTools;

import com.tangosol.io.ReadBuffer;

/**
 * An interface for archiving and restoring snapshots.
 *
 * @since 12.2.1
 * @author jh/tam  2014.03.05
 */
public interface SnapshotArchiver
    {
    /**
     * Return the identifiers of the archived snapshots known to this archiver.
     *
     * @return a list of the known archived snapshot identifiers
     */
    public String[] list();

    /**
     * Return a {@link Snapshot} which represents the archived snapshot with
     * the given identifier.
     *
     * @param sSnapshot  the identifier of the archived snapshot
     *
     * @return the {@link Snapshot}
     *
     * @throws IllegalArgumentException if an archived snapshot with the
     *         given identifier does not exist
     */
    public Snapshot get(String sSnapshot);

    /**
     * Remove the archived snapshot with the specified identifier.
     *
     * @param sSnapshot  the identifier of the archived snapshot
     *
     * @return true if the snapshot was successfully deleted, false otherwise
     */
    public boolean remove(String sSnapshot);

    /**
     * Archive the specified snapshot.
     *
     * @param snapshot  the snapshot to archive
     * @param env       the PersistenceEnvironment used to read the snapshot
     *
     * @throws PersistenceException if an error occurred while reading the
     *         snapshot
     *
     * @throws IllegalArgumentException if a snapshot represented by the
     *         given {@link Snapshot} does not exist
     */
    public void archive(Snapshot snapshot, PersistenceEnvironment<ReadBuffer> env);

    /**
     * Retrieve the specified archived snapshot.
     *
     * @param snapshot  the snapshot to retrieve
     * @param env       the PersistenceEnvironment used to write the snapshot
     *
     * @throws PersistenceException if an error occurred while writing the
     *         snapshot
     *
     * @throws IllegalArgumentException if an archived snapshot represented
     *         by the given {@link Snapshot} does not exist
     */
    public void retrieve(Snapshot snapshot, PersistenceEnvironment<ReadBuffer> env);

    /**
     * Return an instance of {@link PersistenceTools} allowing offline operations
     * to be performed against the associated PersistenceManager and appropriate
     * {@link com.oracle.coherence.persistence.PersistentStore}.
     *
     * @param sSnapshot  the snapshot to return tools for
     *
     * @return a PersistenceTools implementation
     */
    public PersistenceTools getPersistenceTools(String sSnapshot);
    }
