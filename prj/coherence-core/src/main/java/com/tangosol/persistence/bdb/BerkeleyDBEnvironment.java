/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence.bdb;

import com.oracle.coherence.persistence.PersistenceException;
import com.oracle.coherence.persistence.PersistenceManager;

import com.tangosol.io.ReadBuffer;

import com.tangosol.persistence.AbstractPersistenceEnvironment;
import com.tangosol.persistence.AbstractPersistenceManager;

import java.io.File;
import java.io.IOException;

/**
 * PersistenceEnvironment implementation that uses BerkeleyDB.
 *
 * @author jh  2013.05.21
 */
public class BerkeleyDBEnvironment
        extends AbstractPersistenceEnvironment
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Create a new BerkeleyDBEnvironment that manages a singleton
     * BerkeleyDBManager with the specified directories:
     * <ol>
     *     <li>data - active persistence</li>
     *     <li>snapshot - location for snapshots</li>
     *     <li>trash - optional location trashed stores</li>
     * </ol>
     *
     * @param fileActive    the data directory of the singleton active
     *                      manager or null if an active manager shouldn't
     *                      be maintained by this environment
     * @param fileSnapshot  the snapshot directory
     * @param fileTrash     an optional trash directory used for "safe"
     *                      deletes
     *
     * @throws IOException if the data directory could not be created
     *
     * @throws IllegalArgumentException if the data, snapshot, and trash
     *         directories are not unique
     */
    public BerkeleyDBEnvironment(File fileActive, File fileSnapshot, File fileTrash)
            throws IOException
        {
        this(fileActive, fileSnapshot, fileTrash, null);
        }

    /**
     * Create a new BerkeleyDBEnvironment that manages a singleton
     * BerkeleyDBManager with the specified directories:
     * <ol>
     *     <li>data - active persistence</li>
     *     <li>snapshot - location for snapshots</li>
     *     <li>trash - optional location trashed stores</li>
     *     <li>events - optional location for event storage</li>
     * </ol>
     *
     * @param fileActive    the data directory of the singleton active
     *                      manager or null if an active manager shouldn't
     *                      be maintained by this environment
     * @param fileSnapshot  the snapshot directory
     * @param fileTrash     an optional trash directory used for "safe"
     *                      deletes
     * @param fileEvents    an optional events directory used for to store
     *                      map events
     *
     * @throws IOException if the data directory could not be created
     *
     * @throws IllegalArgumentException if the data, snapshot, and trash
     *         directories are not unique
     */
    public BerkeleyDBEnvironment(File fileActive, File fileSnapshot, File fileTrash, File fileEvents)
            throws IOException
        {
        this(fileActive, null, fileEvents, fileSnapshot, fileTrash);
        }

    /**
     * Create a new BerkeleyDBEnvironment that manages a singleton
     * BerkeleyDBManager with the specified directories:
     * <ol>
     *     <li>data - active persistence</li>
     *     <li>backup - optional location for backup storage</li>
     *     <li>events - optional location for event storage</li>
     *     <li>snapshot - location for snapshots</li>
     *     <li>trash - optional location trashed stores</li>
     * </ol>
     *
     * @param fileActive   the data directory of the singleton active manager or
     *                     null if an active manager shouldn't be maintained by
     *                     this environment
     * @param fileBackup   an optional backup directory used to store backup map
     *                     data
     * @param fileEvents   an optional events directory used to store map
     *                     events
     * @param fileSnapshot the snapshot directory
     * @param fileTrash    an optional trash directory used for "safe" deletes
     * @throws IOException              if the data directory could not be
     *                                  created
     * @throws IllegalArgumentException if the data, snapshot, and trash
     *                                  directories are not unique
     */
    public BerkeleyDBEnvironment(File fileActive, File fileBackup, File fileEvents, File fileSnapshot, File fileTrash)
            throws IOException
        {
        super(fileActive, fileBackup, fileEvents, fileSnapshot, fileTrash);
        }

    // ----- AbstractPersistenceEnvironment methods -------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractPersistenceManager openActiveInternal()
        {
        try
            {
            return new BerkeleyDBManager(getPersistenceActiveDirectory(),
                    getPersistenceTrashDirectory(), null);
            }
        catch (IOException e)
            {
            throw ensurePersistenceException(e);
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractPersistenceManager openBackupInternal()
        {
        try
            {
            return new BerkeleyDBManager(getPersistenceBackupDirectory(),
                    getPersistenceTrashDirectory(), null);
            }
        catch (IOException e)
            {
            throw ensurePersistenceException(e);
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractPersistenceManager openEventsInternal()
        {
        try
            {
            return new BerkeleyDBManager(getPersistenceEventsDirectory(),
                    getPersistenceTrashDirectory(), null);
            }
        catch (IOException e)
            {
            throw ensurePersistenceException(e);
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractPersistenceManager openSnapshotInternal(File fileSnapshot,
            String sSnapshot)
        {
        try
            {
            return new BerkeleyDBManager(fileSnapshot, null, sSnapshot);
            }
        catch (IOException e)
            {
            throw ensurePersistenceException(e);
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractPersistenceManager createSnapshotInternal(final File fileSnapshot,
            String sSnapshot, PersistenceManager<ReadBuffer> manager)
        {
        if (manager != null && !(manager instanceof BerkeleyDBManager))
            {
            throw new IllegalArgumentException("incompatible persistence manager type: "
                    + manager.getClass());
            }

        // create a new snapshot
        BerkeleyDBManager snapshot = (BerkeleyDBManager) openSnapshotInternal(
                fileSnapshot, sSnapshot);
        if (manager instanceof BerkeleyDBManager)
            {
            try
                {
                ((BerkeleyDBManager) manager).createSnapshot(fileSnapshot);
                }
            catch (PersistenceException e)
                {
                snapshot.release();
                throw e;
                }
            }

        return snapshot;
        }
    }