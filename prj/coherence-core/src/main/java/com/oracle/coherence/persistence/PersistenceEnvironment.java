/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.persistence;

/**
 * A PersistenceEnvironment is responsible for managing a singleton
 * PersistenceManager and provides facilities for creating, opening, and
 * deleting persistent copies or "snapshots" of a PersistenceManager.
 *
 * @param <R>  the type of a raw, environment specific object representation
 *
 * @author jh/rl/hr  2013.05.09
 */
public interface PersistenceEnvironment<R>
    {
    /**
     * Open and return a singleton PersistenceManager to store backup data.
     *
     * @return the singleton backup PersistenceManager or null if an active
     *         PersistenceManager has not been configured
     */
    public default PersistenceManager<R> openBackup()
        {
        return null;
        }

    /**
     * Open and return a singleton PersistenceManager to store MapEvents.
     *
     * @return the singleton store of MapEvents or null if durable events
     *         is not enabled
     */
    public default PersistenceManager<R> openEvents()
        {
        return null;
        }

    /**
     * Open and return the singleton active PersistenceManager.
     *
     * @return the singleton active PersistenceManager or null if an active
     *         PersistenceManager has not been configured
     */
    public PersistenceManager<R> openActive();

    /**
     * Open a PersistenceManager used to access the snapshot with the
     * specified identifier.
     *
     * @param sSnapshot  the snapshot identifier
     *
     * @throws IllegalArgumentException if a snapshot with the given
     *         identifier does not exist
     *
     * @return a PersistenceManager representing the snapshot
     */
    public PersistenceManager<R> openSnapshot(String sSnapshot);

    /**
     * Create a PersistenceManager used to manage the snapshot with the
     * specified identifier.
     *
     * @param sSnapshot  the snapshot identifier
     *
     * @return a PersistenceManager representing the snapshot
     */
    public default PersistenceManager<R> createSnapshot(String sSnapshot)
        {
        return createSnapshot(sSnapshot, null);
        }

    /**
     * Create a PersistenceManager used to manage the snapshot with the
     * specified identifier.
     *
     * @param sSnapshot  the snapshot identifier
     * @param manager    the optional PersistenceManager to create a snapshot
     *                   of; if null, an empty snapshot will be created
     *
     * @return a PersistenceManager representing the snapshot
     */
    public PersistenceManager<R> createSnapshot(String sSnapshot, PersistenceManager<R> manager);

    /**
     * Remove the persistent artifacts associated with the snapshot with the
     * specified identifier.
     *
     * @param sSnapshot  the snapshot identifier
     *
     * @return true if the snapshot was successfully deleted, false otherwise
     */
    public boolean removeSnapshot(String sSnapshot);

    /**
     * Return the identifiers of the snapshots known to this environment.
     *
     * @return a list of the known snapshot identifiers
     */
    public String[] listSnapshots();

    /**
     * Release all resources held by this environment. Note that the behavior
     * of all other methods on this environment is undefined after this
     * method is called.
     */
    public void release();
    }
