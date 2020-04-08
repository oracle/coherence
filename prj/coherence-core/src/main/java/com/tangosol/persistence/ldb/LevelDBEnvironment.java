package com.tangosol.persistence.ldb;

import com.oracle.coherence.persistence.PersistenceException;
import com.oracle.coherence.persistence.PersistenceManager;

import com.tangosol.io.ReadBuffer;

import com.tangosol.persistence.AbstractPersistenceEnvironment;
import com.tangosol.persistence.AbstractPersistenceManager;

import java.io.File;
import java.io.IOException;

/**
 * PersistenceEnvironment implementation that uses LevelDB.
 *
 * @author jh  2013.05.21
 */
public class LevelDBEnvironment
        extends AbstractPersistenceEnvironment
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Create a new LevelDBEnvironment that manages a singleton
     * LevelDBManager with the specified data directory and that creates,
     * opens, and deletes snapshots under the specified snapshot directory.
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
    public LevelDBEnvironment(File fileActive, File fileSnapshot, File fileTrash)
            throws IOException
        {
        super(fileActive, fileSnapshot, fileTrash);
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
            return new LevelDBManager(getPersistenceActiveDirectory(),
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
            return new LevelDBManager(fileSnapshot, null, sSnapshot);
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
        if (manager != null && !(manager instanceof LevelDBManager))
            {
            throw new IllegalArgumentException("incompatible persistence manager type: "
                    + manager.getClass());
            }

        // create a new snapshot
        LevelDBManager snapshot = (LevelDBManager) openSnapshotInternal(
                fileSnapshot, sSnapshot);
        if (manager instanceof LevelDBManager)
            {
            try
                {
                ((LevelDBManager) manager).createSnapshot(fileSnapshot);
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
